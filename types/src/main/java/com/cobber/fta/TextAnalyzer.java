/*
 * Copyright 2017-2026 Tim Segall
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cobber.fta;

import static com.cobber.fta.dates.DateTimeParserResult.FRACTION_INDEX;
import static com.cobber.fta.dates.DateTimeParserResult.HOUR_INDEX;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.LoggerFactory;

import com.cobber.fta.core.FTAMergeException;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.FTAUnsupportedLocaleException;
import com.cobber.fta.core.InternalErrorException;
import com.cobber.fta.core.PatternFG;
import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.core.TraceException;
import com.cobber.fta.core.Utils;
import com.cobber.fta.dates.DateTimeParser;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;
import com.cobber.fta.dates.DateTimeParserResult;
import com.cobber.fta.dates.LocaleInfo;
import com.cobber.fta.token.TokenStream;
import com.cobber.fta.token.TokenStreams;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Analyze Text data to determine type information and other key metrics
 * associated with a text stream. A key objective of the analysis is that it
 * should be sufficiently fast to be in-line (i.e. as the data is input from
 * some source it should be possible to stream the data through this class
 * without undue performance degradation).
 *
 * <p>
 * Typical usage is:
 * </p>
 *
 * <pre>
 * {@code
 * 		TextAnalyzer analysis = new TextAnalyzer("Age");
 *
 * 		analysis.train("12");
 * 		analysis.train("62");
 * 		analysis.train("21");
 * 		analysis.train("37");
 * 		...
 *
 * 		TextAnalysisResult result = analysis.getResult();
 * }
 * </pre>
 */
public class TextAnalyzer {
	/** Plugin Threshold for detection - by default this is unset and sensible defaults are used. */
	private int pluginThreshold = -1;

	private Locale locale = Locale.getDefault();

	/** We need to see at least this many samples (all unique) before we will claim this is a possible key. */
	private static final int MIN_SAMPLES_FOR_KEY = 1000;

	private AnalysisConfig analysisConfig = new AnalysisConfig();

	/** We are prepared to recognize any set of this size as an enum (and give a suitable regular expression). */
	private static final int MAX_ENUM_SIZE = 40;

	protected static final int REFLECTION_SAMPLES = 30;
	private int reflectionSamples = REFLECTION_SAMPLES;

	/** Context provided to this analysis - contains name, DateResolutionMode, ... */
	private AnalyzerContext context;

	private NumericInfo ni;

	private NumberFormat longFormatter;
	private NumberFormat doubleFormatter;
	private final FiniteMap outliersSmashed = new FiniteMap();
	private List<String> raw; // 0245-11-98
	// 0: d{4}-d{2}-d{2} 1: d{+}-d{+}-d{+} 2: d{+}-d{+}-d{+}
	// 0: d{4} 1: d{+} 2: [-]d{+}
	// input "hello world" 0: a{5} a{5} 1: a{+} a{+} 2: a{+}

	private TokenStreams tokenStreams;

	private final Random random = new Random(271828);

	Trace traceConfig;

	private int internalErrors;

	private static final String insufficient = "Insufficient digits in input (";

	private Collator collator;

	private String localizedYes;
	private String localizedNo;

	private boolean cacheCheck;

	private Correlation correlation;

	private static final Object pluginDefinitionsLock = new Object();
	private static volatile List<PluginDefinition> pluginDefinitions;

	private boolean nullTextAsNull;

	/** Shared state passed to pipeline helper classes after initialize(). */
	AnalysisContext ac;

	/** Handles per-type value validation and statistics tracking on every training sample. */
	private TypeTracker typeTracker;

	/** Builds escalation data during the detect window and determines the stream's base/semantic type. */
	private TypeDeterminer typeDeterminer;

	/** Handles type-finalization logic: backouts, finite/regexp/datetime checking, per-type finalizers. */
	private ResultFinalizer resultFinalizer;

	/** Enumeration that defines all on/off features for parsers. */
	public enum Feature {
		/**
		 * Allow recognition of "AM" and "PM" in dates that would otherwise be rejected based on the recognition of the
		 * locale-specific AM/PM indicators. Feature is enabled by default.
		 */
		ALLOW_ENGLISH_AMPM,
		/** Feature that determines whether to collect statistics or not. Feature is enabled by default. */
		COLLECT_STATISTICS,
		/** Feature that indicates whether to enable the built-in Semantic Types. Feature is enabled by default. */
		DEFAULT_SEMANTIC_TYPES,
		/** Indicate whether we should track distributions (Quantiles/Histograms). Feature is enabled by default. */
		DISTRIBUTIONS,
		/** Feature that indicates whether to attempt to detect the stream format (HTML, XML, JSON, BASE64, OTHER). Feature is disabled by default. */
		FORMAT_DETECTION,
		/** Indicate whether we should qualify the size of the RegExp. Feature is enabled by default. */
		LENGTH_QUALIFIER,
		/**
		 * Indicate whether we should Month Abbreviations (without punctuation), for instance the locale for Canada
		 * uses 'AUG.' for the month abbreviation, and similarly for the AM/PM string which are defined as A.M and P.M.
		 * Feature is enabled by default.
		 */
		NO_ABBREVIATION_PUNCTUATION,
		/** Indicate whether we should treat "NULL" (and similar) as Null values. Feature is enabled by default. */
		NULL_TEXT_AS_NULL,
		/** Feature that if enabled returns a double if we see a set of integers followed by some doubles call it a double. Feature is enabled by default. */
		NUMERIC_WIDENING
	}

	/**
	 * Method for changing state of an on/off feature for this TextAnalyzer.
	 * @param feature The feature to be set.
	 * @param state The new state of the feature.
	 */
	public void configure(final Feature feature, final boolean state) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot adjust feature '" + feature.toString() + "' once training has started");

		analysisConfig.configure(feature, state);
	}

	/**
	 * Method for checking whether given TextAnalyzer feature is enabled.
	 * @param feature The feature to be tested.
	 * @return Whether the identified feature is enabled.
	 */
	public boolean isEnabled(final Feature feature) {
		return analysisConfig.isEnabled(feature);
	}

	private boolean trainingStarted;
	private boolean initialized;

	Facts facts;

	private final List<LogicalTypeInfinite> infiniteTypes = new ArrayList<>();
	private final List<LogicalTypeFinite> finiteTypes = new ArrayList<>();
	private final List<LogicalTypeRegExp> regExpTypes = new ArrayList<>();
	private int[] candidateCounts;
	private int[] candidateCountsRE;

	private final KnownTypes knownTypes = new KnownTypes();
	private Keywords keywords;

	private DateTimeParser dateTimeParser;

	private final ObjectMapper mapper = new ObjectMapper();

	private final Plugins plugins = new Plugins(mapper);

	/**
	 * Construct a Text Analyzer using the supplied context.
	 *
	 * @param context The context used to interpret the stream.
	 */
	public TextAnalyzer(final AnalyzerContext context) {
		this.context = context;
		this.facts = new Facts();
	}

	/**
	 * Construct a Text Analyzer for the named data stream.
	 * <p>Note: The DateResolutionMode mode will be 'None'.
	 *
	 * @param name The name of the data stream (e.g. the column of the CSV file)
	 */
	public TextAnalyzer(final String name) {
		this(new AnalyzerContext(name, DateResolutionMode.None, null,  new String[] { name }));
	}

	/**
	 * Construct an anonymous Text Analyzer for a data stream.
	 * <p>Note: The DateResolutionMode mode will be 'None'.
	 */
	public TextAnalyzer() {
		this(new AnalyzerContext(null, DateResolutionMode.None, null, null));
	}

	/**
	 * Construct a Text Analyzer for the named data stream with the supplied DateResolutionMode.
	 *
	 * @param name The name of the data stream (e.g. the column of the CSV file)
	 * @param resolutionMode Determines what to do when the Date field is ambiguous (i.e. we cannot determine which
	 *   of the fields is the day or the month.  If resolutionMode is DayFirst, then assume day is first, if resolutionMode is
	 *   MonthFirst then assume month is first, if it is Auto then choose either DayFirst or MonthFirst based on the locale, if it
	 *   is None then the pattern returned will have '?' in to represent any ambiguity present.
	 */
	public TextAnalyzer(final String name, final DateResolutionMode resolutionMode) {
		this(new AnalyzerContext(name, resolutionMode, null, null));
	}

	public TextAnalyzer duplicate() throws FTAPluginException {
		final TextAnalyzer ret = new TextAnalyzer(getContext());

		ret.setExternalFacts(getFacts().external);
		ret.setConfig(new AnalysisConfig(getConfig()));
		ret.getPlugins().registerPluginListWithPrecedence(getPlugins().getUserDefinedPlugins(), getConfig());

		return ret;
	}

	/**
	 * Get the name of the Data Stream.
	 *
	 * @return The name of the Data Stream.
	 */
	public String getStreamName() {
		return context.getStreamName();
	}

	/**
	 * Get the context supplied to the TextAnalyzer.
	 *
	 * @return The AnalyzerContext of the TextAnalyzer.
	 */
	public AnalyzerContext getContext() {
		return context;
	}

	/**
	 * Set the context supplied to the TextAnalyzer.
	 *
	 * @param context The Context for this analysis.
	 */
	protected void  setContext(final AnalyzerContext context) {
		this.context = context;
	}

	/**
	 * Get the configuration associated with this TextAnalyzer.
	 *
	 * @return The AnalysisConfig  of the TextAnalyzer.
	 */
	public AnalysisConfig getConfig() {
		return analysisConfig;
	}

	/**
	 * Set the configuration associated with this TextAnalyzer.
	 * Note: Internal only.
	 * @param analysisConfig The replacement AnalysisConfig
	 */
	protected void setConfig(final AnalysisConfig analysisConfig) {
		this.analysisConfig = analysisConfig;

		if (analysisConfig.getLocaleTag() != null)
			setLocale(Locale.forLanguageTag(analysisConfig.getLocaleTag()));

		setDebug(analysisConfig.getDebug());
	}

	/**
	 * Internal Only.  Enable internal debugging.
	 *
	 * @param debug The debug level.
	 */
	public void setDebug(final int debug) {
		analysisConfig.setDebug(debug);
	}

	/**
	 * Set tracing options.
	 *
	 * General form of options is &lt;attribute1&gt;=&lt;value1&gt;,&lt;attribute2&gt;=&lt;value2&gt; ...
	 * Supported attributes are:
	 * <ul>
	 * <li>enabled=true/false,
	 * <li>stream=&lt;name of stream&gt; (defaults to all)
	 * <li>directory=&lt;directory for trace file&gt; (defaults to java.io.tmpdir)
	 * <li>samples=&lt;# samples to trace&gt; (defaults to 1000)
	 * </ul>
	 *
	 * @param traceOptions The trace options.
	 */
	public void setTrace(final String traceOptions) {
		if (traceOptions == null || traceOptions.isEmpty())
			throw new TraceException("Argument to setTrace must be non-null");
		analysisConfig.setTraceOptions(traceOptions);
	}

	/**
	 * The threshold (0-100) used to determine if a data stream is of a particular base type.  For example, if the data stream has 100 samples
	 * and we see 97 valid doubles and 3 malformed values like '3.456e', 'e05', and '-' then provided the threshold is below 97 this stream
	 * will be detected as base type 'DOUBLE'.
	 * Typically this should not be adjusted, if you want to run in Strict mode then set this to 100.
	 * @param threshold The new threshold for base type detection.
	 */
	public void setThreshold(final int threshold) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot adjust Threshold once training has started");

		if (threshold <= 0 || threshold > 100)
			throw new IllegalArgumentException("Threshold must be between 0 and 100");

		analysisConfig.setThreshold(threshold);
	}

	/**
	 * Get the current base type detection Threshold.
	 *
	 * @return The current base type threshold.
	 */
	public int getThreshold() {
		return analysisConfig.getThreshold();
	}

	/**
	 * The percentage when we declare success 0 - 100 for Semantic Type plugins.
	 * Typically this should not be adjusted, if you want to run in Strict mode then set this to 100.
	 * @param threshold The new threshold used for detection.
	 */
	public void setPluginThreshold(final int threshold) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot adjust Plugin Threshold once training has started");

		if (threshold <= 0 || threshold > 100)
			throw new IllegalArgumentException("Plugin Threshold must be between 0 and 100");

		this.pluginThreshold = threshold;
	}

	/**
	 * Get the current detection Threshold for Semantic Type plugins.
	 * If not set, this will return -1, this means that each plugin is using a default threshold and doing something sensible!
	 *
	 * @return The current threshold.
	 */
	public int getPluginThreshold() {
		return pluginThreshold;
	}

	/**
	 * Override the default Locale.
	 * <p>Note: There is no support for Locales that do not use the Gregorian Calendar.
	 *
	 * @param locale The new Locale used to determine separators in numbers, date processing, default plugins, etc.
	 */
	public void setLocale(final Locale locale) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot adjust Locale once training has started");

		this.locale = locale;
		analysisConfig.setLocaleTag(locale.toLanguageTag());
	}

    /**
     * Set the size of the Detect Window (that is, number of samples) to collect before attempting to determine the
     * type.
	 * Default is {@link AnalysisConfig#DETECT_WINDOW_DEFAULT}.
     * <p> Note: It is not possible to change the Sample Size once training has started.
     *
     * @param detectWindow
     *            The number of samples to collect
     * @return The previous value of this parameter.
	*/
	public int setDetectWindow(final int detectWindow) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot change size of detect window once training has started");
		if (detectWindow < AnalysisConfig.DETECT_WINDOW_DEFAULT)
			throw new IllegalArgumentException("Cannot set detect window size below " + AnalysisConfig.DETECT_WINDOW_DEFAULT);

		final int ret = detectWindow;
		analysisConfig.setDetectWindow(detectWindow);

		// Never want the Detect Window to be greater than the Reflection point
		if (detectWindow >= reflectionSamples)
			reflectionSamples = detectWindow + 1;
		return ret;
	}

	/**
	 * Get the size of the Detect Window (i.e number of Samples used to collect before attempting to determine
	 * the type.
	 *
	 * @return The current size of the Detect Window.
	 */
	public int getDetectWindow() {
		return analysisConfig.getDetectWindow();
	}

	/**
	 * Get the number of Samples required before we will 'reflect' on the analysis and
	 * potentially change determination.
	 *
	 * @return The current size of the reflection window.
	 */
	public int getReflectionSampleSize() {
		return reflectionSamples;
	}

	/**
	 * Set the maximum cardinality that will be tracked.
	 * Default is {@link AnalysisConfig#MAX_CARDINALITY_DEFAULT}.
	 * <p>
	 * Note:
	 * <ul>
	 * <li>The Cardinality must be larger than the Cardinality of the largest Finite Semantic type
	 * (if Semantic Type detection is enabled - see {@link #configure(Feature, boolean)}).
	 * <li>
	 * <li>It is not possible to change the cardinality once training has started.
	 * </ul>
	 *
	 * @param newCardinality
	 *            The maximum Cardinality that will be tracked (0 implies no tracking)
	 * @return The previous value of this parameter.
	 */
	public int setMaxCardinality(final int newCardinality) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot change maxCardinality once training has started");
		if (newCardinality < 0)
			throw new IllegalArgumentException("Invalid value for maxCardinality " + newCardinality);

		return analysisConfig.setMaxCardinality(newCardinality);
	}

	/**
	 * Get the maximum cardinality that will be tracked. See
	 * {@link #setMaxCardinality(int) setMaxCardinality()} method.
	 *
	 * @return The maximum cardinality.
	 */
	public int getMaxCardinality() {
		return analysisConfig.getMaxCardinality();
	}

	/**
	 * Set the maximum number of outliers that will be tracked.
	 * Default is {@link AnalysisConfig#MAX_OUTLIERS_DEFAULT}.
	 * <p>Note: It is not possible to change the outlier count once training has started.
	 *
	 * @param newMaxOutliers
	 *            The maximum number of outliers that will be tracked (0 implies
	 *            no tracking)
	 * @return The previous value of this parameter.
	 */
	public int setMaxOutliers(final int newMaxOutliers) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot change outlier count once training has started");
		if (newMaxOutliers < 0)
			throw new IllegalArgumentException("Invalid value for outlier count " + newMaxOutliers);

		return analysisConfig.setMaxOutliers(newMaxOutliers);
	}

	/**
	 * Get the maximum number of outliers that will be tracked. See
	 * {@link #setMaxOutliers(int) setMaxOutliers()} method.
	 *
	 * @return The maximum number of outliers to track.
	 */
	public int getMaxOutliers() {
		return analysisConfig.getMaxOutliers();
	}

	/**
	 * Set the maximum number of invalid entries that will be tracked.
	 * Default is {@link AnalysisConfig#MAX_INVALID_DEFAULT}.
	 * <p>Note: It is not possible to change the invalid count once training has started.
	 *
	 * @param newMaxInvalids
	 *            The maximum number of invalid entries that will be tracked (0 implies no tracking)
	 * @return The previous value of this parameter.
	 */
	public int setMaxInvalids(final int newMaxInvalids) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot change invalid count once training has started");
		if (newMaxInvalids < 0)
			throw new IllegalArgumentException("Invalid value for invalid count " + newMaxInvalids);

		return analysisConfig.setMaxInvalids(newMaxInvalids);
	}

	/**
	 * Get the maximum number of shapes that will be tracked. See
	 * {@link #setMaxShapes(int) setMaxShapes()} method.
	 *
	 * @return The maximum number of shapes to track.
	 */
	public int getMaxShapes() {
		return analysisConfig.getMaxShapes();
	}

	/**
	 * Set the maximum number of shapes that will be tracked.
	 * Default is {@link AnalysisConfig#MAX_SHAPES_DEFAULT}.
	 * <p>Note: It is not possible to change this value once training has started.
	 *
	 * @param newMaxShapes
	 *            The maximum number of shapes that will be tracked (0 implies no tracking)
	 * @return The previous value of this parameter.
	 */
	public int setMaxShapes(final int newMaxShapes) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot change maximum shapes once training has started");
		if (newMaxShapes < 0)
			throw new IllegalArgumentException("Invalid value for maximum shapes " + newMaxShapes);

		return analysisConfig.setMaxShapes(newMaxShapes);
	}

	/**
	 * Get the number of top/bottom values tracked. See
	 * {@link #setTopBottomK(int) setTopBottomK()} method.
	 *
	 * @return The number of top/bottom values tracked.
	 */
	public int getTopBottomK() {
		return analysisConfig.getTopBottomK();
	}

	/**
	 * Set the number of top/bottom values tracked.
	 * Default is {@link AnalysisConfig#TOP_BOTTOM_K}.
	 * <p>Note: It is not possible to change this value once training has started.
	 *
	 * @param newTopBottomK
	 *            The number of top/bottom values tracked
	 * @return The previous value of this parameter.
	 */
	public int setTopBottomK(final int newTopBottomK) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot change the number of top/bottom values tracked once training has started");
		if (newTopBottomK < 0)
			throw new IllegalArgumentException("Invalid value for the number of top/bottom values tracked " + newTopBottomK);

		return analysisConfig.setTopBottomK(newTopBottomK);
	}

	/**
	 * Get the maximum number of invalid entries that will be tracked. See
	 * {@link #setMaxInvalids(int) setMaxInvalids()} method.
	 *
	 * @return The maximum number of invalid entries to track.
	 */
	public int getMaxInvalids() {
		return analysisConfig.getMaxInvalids();
	}

	/**
	 * Set the Key Confidence - typically used where there is an external source that indicated definitively that this is a key.
	 * @param keyConfidence The new keyConfidence
	 */
	public void setKeyConfidence(final double keyConfidence) {
		facts.external.keyConfidence = keyConfidence;
		facts.keyConfidence = keyConfidence;
	}

	/**
	 * Set the Uniqueness - typically used where there is an external source that has visibility into the entire data set and
	 * 'knows' the uniqueness of the set as a whole.
	 * @param uniqueness The new Uniqueness
	 */
	public void setUniqueness(final double uniqueness) {
		facts.external.uniqueness = uniqueness;
		facts.uniqueness = uniqueness;
	}

	/**
	 * Set the Distinct Count - commonly used where there is an external source that has visibility into the entire data set and
	 * 'knows' the distinct count of the set as a whole. If determined by FTA it will typically indicate that the distinct count
	 * is less than the maximum cardinality being tracked.
	 * @param distinctCount The new Distinct Count
	 */
	public void setDistinctCount(final long distinctCount) {
		facts.distinctCount = distinctCount;
	}

	/**
	 * Set the total number of elements in the Data Stream.
	 * Only used when there is an external source that has visibility into the entire data stream.
	 * @param totalCount The total number of elements, as opposed to the number sampled.
	 */
	public void setTotalCount(final long totalCount) {
		facts.external.totalCount = totalCount;
	}

	/**
	 * Set the count of all null elements in the entire data stream.
	 * Only used when there is an external source that has visibility into the entire data stream.
	 * @param totalNullCount The total number of null elements, as opposed to the number of nulls in the sample set.
	 */
	public void setTotalNullCount(final long totalNullCount) {
		facts.external.totalNullCount = totalNullCount;
	}

	/**
	 * Set the count of all blank elements in the entire data stream.
	 * Only used when there is an external source that has visibility into the entire data stream.
	 * @param totalBlankCount The total number of blank elements, as opposed to the number of blanks in the sample set.
	 */
	public void setTotalBlankCount(final long totalBlankCount) {
		facts.external.totalBlankCount = totalBlankCount;
	}

	/**
	 * Set the count of all invalid elements in the entire data stream.
	 * Only used when there is an external source that has visibility into the entire data stream.
	 * @param totalInvalidCount The total number of invalid elements, as opposed to the number of invalids in the sample set.
	 */
	public void setTotalInvalidCount(final long totalInvalidCount) {
		facts.external.totalInvalidCount = totalInvalidCount;
	}

	/**
	 * Set the count of all elements that match the detected type in the entire data stream.
	 * Only used when there is an external source that has visibility into the entire data stream.
	 * @param totalMatchCount The total number of matching elements, as opposed to the number of matches in the sample set.
	 */
	public void setTotalMatchCount(final long totalMatchCount) {
		facts.external.totalMatchCount = totalMatchCount;
	}

	/**
	 * Set the mean for Numeric types (Long, Double) across the entire data stream.
	 * Only used when there is an external source that has visibility into the entire data stream.
	 * @param totalMean The mean of all elements in the data stream, as opposed to the mean of the sampled set.
	 */
	public void setTotalMean(final Double totalMean) {
		facts.external.totalMean = totalMean;
	}

	/**
	 * Get the Standard Deviation for Numeric types (Long, Double) across the entire data stream (if known).
	 * Only used when there is an external source that has visibility into the entire data stream.
	 * @param totalStandardDeviation The Standard Deviation of all elements in the data stream, as opposed to the Standard Deviation of the sampled set.
	 */
	public void setTotalStandardDeviation(final Double totalStandardDeviation) {
		facts.external.totalStandardDeviation = totalStandardDeviation;
	}

	/**
	 * Set the minimum value for Numeric, Boolean and String types across the entire data stream.
	 * Only used when there is an external source that has visibility into the entire data stream.
	 * @param totalMinValue The minimum value of all elements in the data stream, as opposed to the minimum of the sampled set.
	 */
	public void setTotalMinValue(final String totalMinValue) {
		facts.external.totalMinValue = totalMinValue;
	}

	/**
	 * Set the maximum value for Numeric, Boolean and String across the entire data stream.
	 * Only used when there is an external source that has visibility into the entire data stream.
	 * @param totalMaxValue The maximum value of all elements in the data stream, as opposed to the maximum of the sampled set.
	 */
	public void setTotalMaxValue(final String totalMaxValue) {
		facts.external.totalMaxValue = totalMaxValue;
	}

	/**
	 * Set the minimum length for Numeric, Boolean and String across the entire data stream.
	 * Only used when there is an external source that has visibility into the entire data stream.
	 * Note: For String and Boolean types this length includes any whitespace.
	 * @param totalMinLength The minimum length of all elements in the data stream, as opposed to the minimum length of the sampled set.
	 */
	public void setTotalMinLength(final int totalMinLength) {
		facts.external.totalMinLength = totalMinLength;
	}

	/**
	 * Set the maximum length for Numeric, Boolean and String across the entire data stream.
	 * Only used when there is an external source that has visibility into the entire data stream.
	 * Note: For String and Boolean types this length includes any whitespace.
	 * @param totalMaxLength The maximum length of all elements in the data stream, as opposed to the maximum length of the sampled set.
	 */
	public void setTotalMaxLength(final int totalMaxLength) {
		facts.external.totalMaxLength = totalMaxLength;
	}

	/**
	 * Sets the maximum input length for sampling.
	 * Default is {@link AnalysisConfig#MAX_INPUT_LENGTH_DEFAULT}.
	 * @param maxInputLength The maximum length of samples, any samples longer than this will be truncated to this length.
	 *
	 * @return The previous value of this parameter.
	 */
	public int setMaxInputLength(final int maxInputLength) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot change maxInputLength once training has started");
		if (maxInputLength < AnalysisConfig.MAX_INPUT_LENGTH_MINIMUM)
			throw new IllegalArgumentException("Invalid value for maxInputLength (must be >= " + AnalysisConfig.MAX_INPUT_LENGTH_MINIMUM + ")");
		return analysisConfig.setMaxInputLength(maxInputLength);
	}

	/**
	 * Gets the relative-error guarantee for quantiles.
	 * @return The relative-error guarantee for quantiles (relevant only if cardinality &gt; maxCardinality).
	 */
	public double getQuantileRelativeAccuracy() {
		return analysisConfig.getQuantileRelativeAccuracy();
	}

	/**
	 * Sets the relative-error guarantee for quantiles.
	 * Default is {@link AnalysisConfig#QUANTILE_RELATIVE_ACCURACY_DEFAULT}.
	 * @param quantileRelativeAccuracy The relative-error guarantee desired for quantile determination, note smaller values require more memory!
	 *
	 * @return The previous value of this parameter.
	 */
	public double setQuantileRelativeAccuracy(final double quantileRelativeAccuracy) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot change quantileRelativeAccuracy once training has started");
		if (quantileRelativeAccuracy < 0.0 || quantileRelativeAccuracy > 1.0)
			throw new IllegalArgumentException("Invalid value for quantileRelativeAccuracy (must be <= 1.0)");
		return analysisConfig.setQuantileRelativeAccuracy(quantileRelativeAccuracy);
	}

	/**
	 * Gets the number of bins to use for the underlying approximation used to hold the Histogram once maxCardinality is exceeded.
	 * @return The number of underlying bins used for the approximation (relevant only if cardinality &gt; maxCardinality).
	 */
	public int getHistogramBins() {
		return analysisConfig.getHistogramBins();
	}

	/**
	 * Sets the number of bins to use for the underlying approximation used to hold the Histogram once maxCardinality is exceeded.
	 * Default is {@link AnalysisConfig#HISTOGRAM_BINS_DEFAULT}.
	 * @param histogramBins the number of bins to use for the underlying approximation, note larger values require more memory, and significantly impact performance!
	 *
	 * @return The previous value of this parameter.
	 */
	public int setHistogramBins(final int histogramBins) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot change the number of Histogram bins once training has started");
		if (histogramBins < 100 || histogramBins > 10_000)
			throw new IllegalArgumentException("Invalid value for Histogram bin (must be between 100 and 10,000)");
		return analysisConfig.setHistogramBins(histogramBins);
	}

	/**
	 * Return the full path to the trace file, or null if no tracing configured.
	 * Note: This will only be valid (i.e. non-null) after the first invocation of train() or trainBulk().
	 * @return The Path to the trace file.
	 */
	public String getTraceFilePath() {
		return traceConfig == null ? null : traceConfig.getFilename();
	}

	/**
	 * Gets the current maximum input length for sampling.
	 * @return The current maximum length before an input sample is truncated.
	 */
	public int getMaxInputLength() {
		return analysisConfig.getMaxInputLength();
	}

	/**
	 * Gets a 'magic' TextAnalyzer based on a locale that can be used to inspect the current set of plugins.
	 * @param locale The Locale used for the TextAnalyzer.
	 * @return A TextAnalyzer.
	 */
	public static TextAnalyzer getDefaultAnalysis(final Locale locale) {
		// Create an Analyzer to retrieve the Semantic Types (magically will be all - since passed in '*')
		final TextAnalyzer analysis = new TextAnalyzer("*");
		if (locale != null)
			analysis.setLocale(locale);

		// Load the default set of plugins for Semantic Type detection (normally done by a call to train())
		analysis.registerDefaultPlugins(analysis.getConfig());

		return  analysis;
	}

	protected String getRegExp(final KnownTypes.ID id) {
		return knownTypes.getByID(id).getRegExp();
	}

	// Track basic facts for the field - called for all input
	private void trackLengthAndShape(final String input, final String trimmed, final long count) {
		typeTracker.trackLengthAndShape(input, trimmed, count);
	}

	private boolean trackLong(final String trimmed, final TypeInfo typeInfo, final boolean register, final long count) {
		return typeTracker.trackLong(trimmed, typeInfo, register, count);
	}

	private boolean trackBoolean(final String trimmed) {
		return typeTracker.trackBoolean(trimmed);
	}

	private boolean trackString(final String rawInput, final String trimmed, final TypeInfo typeInfo, final boolean register, final long count) {
		return typeTracker.trackString(rawInput, trimmed, typeInfo, register, count);
	}

	private void updateStats(final String cleaned) {
		typeTracker.updateStats(cleaned);
	}

	private boolean trackDouble(final String rawInput, final TypeInfo typeInfo, final boolean register, final long count) {
		return typeTracker.trackDouble(rawInput, typeInfo, register, count);
	}

	/*
	 * Validate (and track) the date/time/datetime input.
	 * This routine is called for every date/time/datetime we see in the input, so performance is critical.
	 */
	private boolean trackDateTime(final String input, final TypeInfo typeInfo, final boolean register, final long count) {
		return typeTracker.trackDateTime(input, typeInfo, register, count);
	}

	public Plugins getPlugins() {
		return plugins;
	}

	/**
	 * Register the default set of plugins for Semantic Type detection.
	 *
	 * @param analysisConfig The Analysis configuration used for this analysis.
	 * Note: The Locale (on the configuration)  will impact both the set of plugins registered as well as the behavior of the individual plugins
	 */
	public void registerDefaultPlugins(final AnalysisConfig analysisConfig) {
		synchronized (pluginDefinitionsLock) {
			if (pluginDefinitions == null)
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/plugins.json"), StandardCharsets.UTF_8))) {
					pluginDefinitions = mapper.readValue(reader, new TypeReference<List<PluginDefinition>>(){});
				} catch (Exception e) {
					throw new IllegalArgumentException("Internal error: Issues with plugins file: " + e.getMessage(), e);
				}
		}

		try {
			plugins.registerPluginsInternal(pluginDefinitions, context.getStreamName(), analysisConfig);
		} catch (Exception e) {
			throw new IllegalArgumentException("Internal error: Issues with plugins file: " + e.getMessage(), e);
		}
	}

	/**
	 * Retrieve the Plugin Definition associated with this Semantic Type name.
	 *
	 * Note: Unlike the similar function in PluginDefinition this one accesses the current instance and any edits
	 * will impact the current Analyzer.
	 *
	 * @param semanticTypeName The name for this Semantic Type
	 * @return The Plugin Definition associated with the supplied name.
	 */
	public PluginDefinition findByName(final String semanticTypeName) {
		if (!initialized) {
			try {
				initialize();
			} catch (FTAPluginException|FTAUnsupportedLocaleException e) {
				return null;
			}
		}

		final LogicalType logicalType = getPlugins().getRegistered(semanticTypeName);
		return logicalType == null ? null : logicalType.getPluginDefinition();
	}

	void initialize() throws FTAPluginException, FTAUnsupportedLocaleException {
		memoryDebug("initialize.entry");

		facts.initialize(getTopBottomK());

		mapper.registerModule(new JavaTimeModule());
		mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

		if (LocaleInfo.isSupported(locale) != null)
			throw new FTAUnsupportedLocaleException(LocaleInfo.isSupported(locale));

		collator = Collator.getInstance(locale);
		collator.setStrength(Collator.PRIMARY);

		raw = new ArrayList<>(analysisConfig.getDetectWindow());

		tokenStreams = new TokenStreams(getMaxShapes());

		// If enabled, load the default set of plugins for Semantic Type detection
		if (analysisConfig.isEnabled(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES))
			registerDefaultPlugins(analysisConfig);

		for (final LogicalType logical : plugins.getRegisteredSemanticTypes()) {

			if ((logical instanceof LogicalTypeFinite) && ((LogicalTypeFinite)logical).getSize() + 10 > getMaxCardinality())
				throw new FTAPluginException("Internal error: Max Cardinality: " + getMaxCardinality() + " is insufficient to support plugin: " + logical.getSemanticType());

			// Check to see if this plugin requires a mandatory hotword (and it is present)
			if (logical.getPluginDefinition().isMandatoryHeaderUnsatisfied(analysisConfig.getLocale(), context))
				continue;

			if (logical instanceof LogicalTypeInfinite)
				infiniteTypes.add((LogicalTypeInfinite)logical);
			else if (logical instanceof LogicalTypeFinite)
				finiteTypes.add((LogicalTypeFinite)logical);
			else
				regExpTypes.add((LogicalTypeRegExp)logical);

			if (pluginThreshold != -1)
				logical.setThreshold(pluginThreshold);
		}

		// Sort each of the plugins based on their priority (lower is referenced first)
		Collections.sort(infiniteTypes);
		Collections.sort(finiteTypes);
		Collections.sort(regExpTypes);

		candidateCounts = new int[infiniteTypes.size()];
		candidateCountsRE = new int[regExpTypes.size()];

		longFormatter = NumberFormat.getIntegerInstance(locale);
		doubleFormatter = NumberFormat.getInstance(locale);

		ni = new NumericInfo(locale);

		knownTypes.initialize(locale);

		keywords = Keywords.getInstance(locale);

		nullTextAsNull = analysisConfig.isEnabled(TextAnalyzer.Feature.NULL_TEXT_AS_NULL);
		keywords.get("NO_DATA");

		if (knownTypes.getByID(KnownTypes.ID.ID_BOOLEAN_YES_NO_LOCALIZED) != null) {
			localizedYes = keywords.get("YES");
			localizedNo = keywords.get("NO");
		}

		// If Resolution mode is auto then set DayFirst or MonthFirst based on the Locale
		if (context.getDateResolutionMode() == DateResolutionMode.Auto) {
			final DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, locale);
			final String pattern = ((SimpleDateFormat)df).toPattern();
			final int dayIndex = pattern.indexOf('d');
			final int monthIndex = pattern.indexOf('M');
			if (dayIndex == -1 || monthIndex == -1)
				throw new FTAUnsupportedLocaleException("Failed to determine DateResolutionMode for this locale");
			// We assume that if Day is before Month, then Day is also before Year!
			context.setDateResolutionMode(dayIndex < monthIndex ? DateResolutionMode.DayFirst : DateResolutionMode.MonthFirst);
		}

		dateTimeParser = new DateTimeParser()
				.withDateResolutionMode(context.getDateResolutionMode())
				.withLocale(locale)
				.withNumericMode(false)
				.withNoAbbreviationPunctuation(analysisConfig.isEnabled(TextAnalyzer.Feature.NO_ABBREVIATION_PUNCTUATION))
				.withEnglishAMPM(analysisConfig.isEnabled(TextAnalyzer.Feature.ALLOW_ENGLISH_AMPM));

		// Now that we have initialized these facts cannot change, so set them on the Facts object
		this.facts.setConfig(analysisConfig);

		outliersSmashed.setMaxCapacity(analysisConfig.getMaxOutliers());

//		correlation = new Correlation(locale);
//		correlation.initialize();

		ac = new AnalysisContext();
		ac.facts = facts;
		ac.analysisConfig = analysisConfig;
		ac.analyzerContext = context;
		ac.knownTypes = knownTypes;
		ac.keywords = keywords;
		ac.plugins = plugins;
		ac.dateTimeParser = dateTimeParser;
		ac.tokenStreams = tokenStreams;
		ac.ni = ni;
		ac.longFormatter = longFormatter;
		ac.doubleFormatter = doubleFormatter;
		ac.outliersSmashed = outliersSmashed;
		ac.raw = raw;
		ac.locale = locale;
		ac.collator = collator;
		ac.localizedYes = localizedYes;
		ac.localizedNo = localizedNo;
		ac.reflectionSamples = reflectionSamples;
		ac.pluginThreshold = pluginThreshold;
		ac.infiniteTypes = infiniteTypes;
		ac.finiteTypes = finiteTypes;
		ac.regExpTypes = regExpTypes;
		ac.candidateCounts = candidateCounts;
		ac.candidateCountsRE = candidateCountsRE;
		ac.nullTextAsNull = nullTextAsNull;
		ac.internalErrors = internalErrors;

		typeTracker = new TypeTracker(ac);
		typeDeterminer = new TypeDeterminer(ac, typeTracker);
		resultFinalizer = new ResultFinalizer(ac, typeTracker);

		initialized = true;
		memoryDebug("initialize.exit");
	}

	void initializeTrace() {
		// If no trace options already set then pick them up from the environment (if set)
		if (analysisConfig.getTraceOptions() == null) {
			final String ftaTrace = System.getenv("FTA_TRACE");
			if (ftaTrace != null && !ftaTrace.isEmpty())
				analysisConfig.setTraceOptions(ftaTrace);
		}

		if (analysisConfig.getTraceOptions() != null)
			traceConfig = new Trace(analysisConfig.getTraceOptions(), context,  analysisConfig);
	}

	class Observation {
		String observed;
		long count;
		long used;
		double percentage;
		Observation(final String observed, final long count, final long used) {
			this.observed = observed;
			this.count = count;
			this.used = used;
		}
	}

	/**
	 * TrainBulk is the core bulk entry point used to supply input to the Text Analyzer.
	 * This routine is commonly used to support training using the results aggregated from a
	 * database query.
	 *
	 * @param input A Map containing the observed items and the corresponding count
	 * @throws FTAPluginException Thrown when a registered plugin has detected an issue
	 * @throws FTAUnsupportedLocaleException Thrown when a requested locale is not supported
	 */
	public void trainBulk(final Map<String, Long> input) throws FTAPluginException, FTAUnsupportedLocaleException {
		// Initialize if we have not already done so
		if (!initialized) {
			analysisConfig.setTrainingMode(AnalysisConfig.TrainingMode.BULK);
			initializeTrace();
			initialize();
			trainingStarted = true;
		}

		// Sort so we have the most frequent first
		final Map<String, Long> observed = Utils.sortByValue(input);

		// Strip out the uninteresting entries (i.e. nulls and blanks)
		final Map<String, Long> uninteresting = new HashMap<>();
		final Iterator<Entry<String, Long>> it = observed.entrySet().iterator();
		while (it.hasNext()) {
			final Entry<String, Long> entry = it.next();
			if (isNullEquivalent(entry.getKey()) || entry.getKey().isBlank()) {
				uninteresting.put(entry.getKey(), entry.getValue());
				it.remove();
			}
		 }

		final Observation[] facts = new Observation[observed.size()];
		int i = 0;
		long total = 0;

		// Setup the array of observations and calculate the total number of observations
		for (final Map.Entry<String, Long> entry : observed.entrySet()) {
			facts[i++] = new Observation(entry.getKey(), entry.getValue(), 0);
			total += entry.getValue();
		}

		// Each element in the array has the probability that an observation is in this location or an earlier one
		long running = 0;
		for (final Observation fact : facts) {
			running += fact.count;
			fact.percentage = (double)running/total;
		}

		// First send in a random set of samples until we are trained
		boolean trained = false;
		for (int j = 0; j < total && !trained; j++) {
			final double index = random.nextDouble();
			for (final Observation fact : facts) {
				if (index < fact.percentage && fact.used < fact.count) {
					if (train(fact.observed))
						trained = true;
					fact.used++;
					break;
				}
			}
		}

		final Map<String, Long> bulkObservations = new HashMap<>();

		// Now send in the balance of the interesting samples in bulk
		for (final Observation fact : facts) {
			final long remaining = fact.count - fact.used;
			if (remaining != 0) {
				bulkObservations.put(fact.observed, remaining);
				trainBulkCore(fact.observed, remaining);
			}
		}

		// We need to close out the random samples we sent in above and record the bulk observations
		if (traceConfig != null) {
			traceConfig.persistSamples();
			traceConfig.recordBulk(bulkObservations);
		}

		// Now send in the uninteresting elements
		for (final Entry<String, Long> entry : uninteresting.entrySet())
			trainBulkCore(entry.getKey(), entry.getValue());
	}

	private void trainBulkCore(final String rawInput, final long count) {
		facts.sampleCount += count;

		if (rawInput == null || isNullEquivalent(rawInput)) {
			facts.nullCount += count;
			return;
		}

		final String trimmed = rawInput.trim();

		final int length = trimmed.length();

		if (length == 0) {
			facts.blankCount += count;
			typeTracker.trackLengthAndShape(rawInput, trimmed, count);
			return;
		}

		// This next try/catch is unnecessary in theory, if there are zero bugs then it will never trip,
		// if there happens to be an issue then we will lose this training event.
		try {
			trainCore(rawInput, trimmed, count);
		}
		catch (RuntimeException e) {
			internalErrors++;
			if (analysisConfig.getDebug() != 0)
				throw new InternalErrorException(e.getMessage(), e);
		}
	}

	static final int CACHE_SIZE = 10;
	FiniteMap cache = new FiniteMap(CACHE_SIZE);

	void emptyCache() {
		final TypeInfo typeInfo = facts.getMatchTypeInfo();
		LogicalType logical = null;
		String regExp = null;
		if (typeInfo != null)
			if (typeInfo.isSemanticType())
				logical = plugins.getRegistered(typeInfo.getSemanticType());
			else
				regExp = typeInfo.getRegExp();

		final Map<String, Long> invalid = new HashMap<>();

		// Process the valid entries first
		for (final Map.Entry<String, Long> entry : cache.entrySet()) {
			final String key = entry.getKey();
			if (key != null && ((logical != null && logical.isValid(key)) || (regExp != null && key.matches(regExp))))
				trainBulkCore(entry.getKey(), entry.getValue());
			else
				invalid.put(key, entry.getValue());
		}

		// Now process the invalid entries
		for (final Map.Entry<String, Long> entry : invalid.entrySet())
			trainBulkCore(entry.getKey(), entry.getValue());

		cache.clear();
	}

	/**
	 * Train is the streaming entry point used to supply input to the Text Analyzer.
	 *
	 * @param rawInput
	 *            The raw input as a String
	 * @return A boolean indicating if the resultant type is currently known.
	 * @throws FTAPluginException Thrown when a registered plugin has detected an issue
	 * @throws FTAUnsupportedLocaleException Thrown when a requested locale is not supported
	 */
	public boolean train(final String rawInput) throws FTAPluginException, FTAUnsupportedLocaleException {
		// Initialize if we have not already done so
		if (!initialized) {
			analysisConfig.setTrainingMode(AnalysisConfig.TrainingMode.SIMPLE);
			initializeTrace();
			initialize();
			handleForce();
			trainingStarted = true;
		}

		// If we have a large number of repetitive samples, then cache them to speed up the analysis
		if (facts.sampleCount > 100 && facts.getMatchTypeInfo() != null && facts.cardinality.size() < 2 * CACHE_SIZE) {
			final boolean added = cache.mergeIfSpace(rawInput, 1L, Long::sum);
			if (added)
				return facts.getMatchTypeInfo().getBaseType() != null;
			else {
				emptyCache();
			}
		}

		if (traceConfig != null)
			traceConfig.recordSample(rawInput, facts.sampleCount);

		facts.sampleCount++;

		final FTAType matchType = facts.getMatchTypeInfo() != null ? facts.getMatchTypeInfo().getBaseType() : null;

		if (isNullEquivalent(rawInput)) {
			facts.nullCount++;
			return matchType != null;
		}

		final String trimmed = rawInput.trim();

		final int length = trimmed.length();

		if (length == 0) {
			facts.blankCount++;
			trackLengthAndShape(rawInput, trimmed, 1);
			return matchType != null;
		}

		// This next try/catch is unnecessary in theory, if there are zero bugs then it will never trip,
		// if there happens to be an issue then we will lose this training event.
		final boolean result;
		try {
			result = trainCore(rawInput, trimmed, 1);
		}
		catch (RuntimeException e) {
			internalErrors++;
			if (analysisConfig.getDebug() != 0)
				throw new InternalErrorException(e.getMessage(), e);
			return false;
		}
		return result;
	}

	private static volatile PatternFG patternFG = null;
	/**
	 * Check if the input is equivalent to null based on the configuration, this includes the set of words which are considered null.
	 * @param input Input string to check for null equivalency.
	 * @return A boolean indicating if the input is equivalent to null.
	 */
	public boolean isNullEquivalent(final String input) {
		if (input == null)
			return true;

		if (!nullTextAsNull)
			return false;

		if (patternFG == null) {
			synchronized (pluginDefinitionsLock) {
				if (patternFG == null)
					patternFG = PatternFG.compile(keywords.get("NO_DATA"));
			}
		}

		return patternFG.matcher(input);
	}

	private int reported = 0;
	private boolean trainCore(final String rawInput, final String trimmed, final long count) {
		if (reported%100_000 == 0)
			memoryDebug("trainCore.100K");
		reported++;

		trackResult(rawInput, trimmed, true, count);

		// If we have determined a type, no need to further train
		if (facts.getMatchTypeInfo() != null && facts.getMatchTypeInfo().getBaseType() != null)
			return true;

		typeDeterminer.buildEscalation(rawInput, trimmed, count);

		return facts.getMatchTypeInfo() != null && facts.getMatchTypeInfo().getBaseType() != null;
	}

	final AllocationTracker tracker = new AllocationTracker();
	private void memoryDebug(final String where) {
		if (analysisConfig.getDebug() >= 3)
			debug("Memory, location: {}, initialization - Allocated: {}, Free memory: {}", where, tracker.getAllocated(), Runtime.getRuntime().freeMemory());
	}

	void debug(final String format, final Object... arguments) {
		if (analysisConfig.getDebug() >= 2)
			LoggerFactory.getLogger("com.cobber.fta").debug(format, arguments);
	}

	void ctxdebug(final String area, final String format, final Object... arguments) {
		if (analysisConfig.getDebug() >= 2) {
			final StringBuilder formatWithContext = new StringBuilder().append(area).append(" ({}, {}, {}) -- ").append(format);
			final Object[] newArguments = new Object[3 + arguments.length];
			int newIndex = 0;
			newArguments[newIndex++] = context.getStreamName();
			newArguments[newIndex++] = analysisConfig.getTrainingMode();
			newArguments[newIndex++] = context.isNested();
			for (final Object arg : arguments)
				newArguments[newIndex++] = arg;
			LoggerFactory.getLogger("com.cobber.fta").debug(formatWithContext.toString(), newArguments);
		}
	}

	private boolean handleForce() {
		return typeDeterminer.handleForce();
	}

	/**
	 * This is the core routine for determining the type of the field. It is
	 * responsible for setting matchTypeInfo.
	 */
	void determineType() {
		// typeDeterminer is null when called before initialize() (e.g. serialize()/merge() on an untrained instance).
		// In that case sampleCount is 0 so there is nothing to determine; leave matchTypeInfo null.
		if (typeDeterminer == null)
			return;
		if (typeDeterminer.determineType())
			raw.forEach((value) -> trackResult(value, value.trim(), false, 1));
	}

	private void addValid(final String input, final long count) {
		typeTracker.addValid(input, count);
	}

	private void addOutlier(final String input, final long count) {
		typeTracker.addOutlier(input, count);
	}

	private void addInvalid(final Map.Entry<String, Long> entry) {
		typeTracker.addInvalid(entry);
	}

	private boolean hasGroupingSeparator(final String input, final char groupingSeparator, final char decimalSeparator) {
		return typeTracker.hasGroupingSeparator(input, groupingSeparator, decimalSeparator);
	}

	private boolean conditionalBackoutToPattern(final long realSamples, final TypeInfo current) {
		return resultFinalizer.conditionalBackoutToPattern(realSamples, current);
	}

	/**
	 * Track the supplied raw input, once we have enough samples attempt to determine the type.
	 * @param rawInput The raw input string
	 * @param trimmed The trimmed version of the raw input string
	 * @param fromTraining True if this is a real sample
	 * @param count The number of occurrences of this input
	 */
	private void trackResult(final String rawInput, final String trimmed, final boolean fromTraining, final long count) {
		if (fromTraining)
			typeTracker.trackLengthAndShape(rawInput, trimmed, count);

		// If the detect window cache is full and we have not determined a type compute one
		if ((facts.getMatchTypeInfo() == null || facts.getMatchTypeInfo().getBaseType() == null) && facts.sampleCount - (facts.nullCount + facts.blankCount) > analysisConfig.getDetectWindow())
			determineType();

		if (facts.getMatchTypeInfo() == null || facts.getMatchTypeInfo().getBaseType() == null)
			return;

		final long realSamples = facts.sampleCount - (facts.nullCount + facts.blankCount);
		boolean valid = false;

		final String input = rawInput.length() > getMaxInputLength() ? rawInput.substring(0, getMaxInputLength()) : rawInput;

		switch (facts.getMatchTypeInfo().getBaseType()) {
		case BOOLEAN:
			if (trackBoolean(trimmed))
				valid = true;
			break;

		case LONG:
			if (trackLong(trimmed, facts.getMatchTypeInfo(), true, count))
				valid = true;
			break;

		case DOUBLE:
			if (trackDouble(input, facts.getMatchTypeInfo(), true, count))
				valid = true;
			break;

		case STRING:
			if (trackString(input, trimmed, facts.getMatchTypeInfo(), true, count))
				valid = true;
			break;

		case LOCALDATE:
		case LOCALTIME:
		case LOCALDATETIME:
		case OFFSETDATETIME:
		case ZONEDDATETIME:
			try {
				if (trackDateTime(input, facts.getMatchTypeInfo(), true, count))
					valid = true;
			}
			catch (DateTimeParseException reale) {
				// The real parse threw an Exception, this does not give us enough facts to usefully determine if there are any
				// improvements to our assumptions we could make to do better, so re-parse and handle our more nuanced exception
				DateTimeParserResult result = DateTimeParserResult.asResult(facts.getMatchTypeInfo().format, context.getDateResolutionMode(), dateTimeParser.getConfig());
				String newFormatString = null;
				TypeInfo newTypeInfo = facts.getMatchTypeInfo();
				boolean success = false;
				do {
					try {
						result.parse(input);
						success = true;
					}
					catch (DateTimeParseException e) {
						boolean updated = false;

						// If the parse exception is of the form 'Insufficient digits in input (M)' or similar
						// then worth updating our pattern and retrying.
						char ditch = '?';
						final int find = e.getMessage().indexOf(insufficient);
						if (find != -1)
							ditch = e.getMessage().charAt(insufficient.length());

						if (ditch != '?') {
							final int offset = newTypeInfo.format.indexOf(ditch);

							// S is a special case (unlike H, H, M, d) and is *NOT* handled by the default DateTimeFormatter.ofPattern
							if (ditch == 'S') {
								// The input is shorter than we expected so set the minimum length to 1 and then update
								final int len = result.timeFieldLengths[FRACTION_INDEX].getPatternLength();
								result.timeFieldLengths[FRACTION_INDEX].setMin(1);
								newFormatString = Utils.replaceAt(newTypeInfo.format, offset, len,
										result.timeFieldLengths[FRACTION_INDEX].getPattern('S'));
							}
							else
								newFormatString = new StringBuffer(newTypeInfo.format).deleteCharAt(offset).toString();

							updated = true;
						}
						else if (e.getMessage().equals("Expecting end of input, extraneous input found, last token (FRACTION)")) {
							final int offset = newTypeInfo.format.indexOf('S');
							// If we have extra input and the existing length is not maxed out then widen and try again
							if (result.timeFieldLengths[FRACTION_INDEX].getMax() < 9) {
								final int oldLength = result.timeFieldLengths[FRACTION_INDEX].getPatternLength();
								result.timeFieldLengths[FRACTION_INDEX].set(result.timeFieldLengths[FRACTION_INDEX].getMin(),
										result.timeFieldLengths[FRACTION_INDEX].getMax() + 1);
								newFormatString = Utils.replaceAt(newTypeInfo.format, offset, oldLength,
										result.timeFieldLengths[FRACTION_INDEX].getPattern('S'));
								updated = true;
							}
						}
						else if (e.getMessage().equals("Invalid value for hours: 24 (expected 0-23)")) {
							final int offset = newTypeInfo.format.indexOf('H');
							newFormatString = Utils.replaceAt(newTypeInfo.format, offset, result.timeFieldLengths[HOUR_INDEX].getMin(),
									result.timeFieldLengths[HOUR_INDEX].getMin() == 1 ? "k" : "kk");
							updated = true;
						}

						if (!updated)
							break;

						result = DateTimeParserResult.asResult(newFormatString, context.getDateResolutionMode(), dateTimeParser.getConfig());
						newTypeInfo = new TypeInfo(null, result.getRegExp(), facts.getMatchTypeInfo().getBaseType(), newFormatString, false, newFormatString);
					}
				} while (!success);

				// If we succeeded in coming up with something better then make it so!
				if (success && newFormatString != null)
					facts.setMatchTypeInfo(newTypeInfo);

				try {
					trackDateTime(input, facts.getMatchTypeInfo(), true, count);
					valid = true;
				}
				catch (DateTimeParseException eIgnore) {
					// Ignore and record as outlier below
				}
			}
			break;
		}

		if (valid) {
			facts.matchCount += count;
			addValid(input, count);
			facts.trackTrimmedLengthAndWhiteSpace(rawInput, trimmed, count);
		}
		else {
			addOutlier(input, count);

			if (facts.getMatchTypeInfo().isSemanticType()) {
				if (facts.outliers.size() == analysisConfig.getMaxOutliers() && !facts.getMatchTypeInfo().isForce()) {
					// Do we need to back out from any of our Infinite type determinations
					final LogicalType logical = plugins.getRegistered(facts.getMatchTypeInfo().getSemanticType());
					final PluginAnalysis pluginAnalysis = logical.analyzeSet(context, facts.matchCount, realSamples, facts.getMatchTypeInfo().getRegExp(), facts.calculateFacts(), facts.cardinality, facts.outliers, tokenStreams, analysisConfig);
					if (!pluginAnalysis.isValid())
						backout(logical, realSamples, pluginAnalysis);
				}
			}
			else if (facts.outliers.size() == analysisConfig.getMaxOutliers() || (realSamples > reflectionSamples * 3 && (double)facts.matchCount / realSamples < .5)) {
				// Need to evaluate if we got this wrong
				conditionalBackoutToPattern(realSamples, facts.getMatchTypeInfo());
			}
		}

		// So before we blow the cache (either Shapes or Cardinality) we should look for RegExp matches ONCE!
		if (!cacheCheck && (tokenStreams.isFull() || facts.cardinality.size() + 1 == analysisConfig.getMaxCardinality())) {
			checkRegExpTypes(facts.getMatchTypeInfo().getBaseType());
			cacheCheck = true;
		}
	}

	private void backout(final LogicalType logical, final long realSamples, final PluginAnalysis pluginAnalysis) {
		resultFinalizer.backout(logical, realSamples, pluginAnalysis);
	}

	private String lengthQualifier(final int min, final int max) {
		return resultFinalizer.lengthQualifier(min, max);
	}

	/**
	 * Calculate the Levenshtein distance of the source string from the 'closest' string from the provided universe.
	 * @param source The source string to test.
	 * @param universe The universe of strings to test for distance
	 * @return The Levenshtein distance from the best match.
	 */
	protected static int distanceLevenshtein(final String source, final Set<String> universe) {
		final LevenshteinDistance distance = new LevenshteinDistance(null);

		Integer best = Integer.MAX_VALUE;
		for (final String test : universe) {
			if (test.equals(source))
				continue;
			final Integer current = distance.apply(source, test);
			if (current < best)
				best = current;
		}

		return best;
	}

	protected TextAnalysisResult reAnalyze(final Map<String, Long> details) throws FTAPluginException, FTAUnsupportedLocaleException {
		final TextAnalyzer analysisBulk = duplicate();
		analysisBulk.getContext().setNested();

		analysisBulk.trainBulk(details);
		return analysisBulk.getResult();
	}

	/**
	 * Determine the result of the training complete to date. Typically invoked
	 * after all training is complete, but may be invoked at any stage.
	 *
	 * @return A TextAnalysisResult with the analysis of any training completed.
	 * @throws FTAPluginException Thrown when a registered plugin has detected an issue
	 * @throws FTAUnsupportedLocaleException Thrown when a requested locale is not supported
	 */
	public TextAnalysisResult getResult() throws FTAPluginException, FTAUnsupportedLocaleException {
		memoryDebug("getResult.entry");
		// Normally we will initialize as a consequence of the first call to train() but just in case no training happens!
		if (!initialized) {
			initializeTrace();
			initialize();
		}

		emptyCache();

		// If we have not already determined the type, now we need to
		if (facts.getMatchTypeInfo() == null)
			determineType();

		// Compute our confidence
		final long realSamples = facts.sampleCount - (facts.nullCount + facts.blankCount);

		// Check to see if we are all blanks or all nulls
		if (facts.blankCount == facts.sampleCount || facts.nullCount == facts.sampleCount || facts.blankCount + facts.nullCount == facts.sampleCount) {
			if (facts.nullCount == facts.sampleCount)
				facts.setMatchTypeInfo(knownTypes.getByID(KnownTypes.ID.ID_NULL));
			else if (facts.blankCount == facts.sampleCount)
				facts.setMatchTypeInfo(knownTypes.getByID(KnownTypes.ID.ID_BLANK));
			else
				facts.setMatchTypeInfo(knownTypes.getByID(KnownTypes.ID.ID_BLANKORNULL));
			facts.confidence = facts.sampleCount >= 10 ? 1.0 : 0.0;
		}
		else {
			facts.confidence = (double) facts.matchCount / realSamples;
		}

		boolean backedOutRegExp = false;

		// Do we need to back out from any of our Semantic type determinations.  Most of the time this backs out of
		// Infinite type determinations (since we have not yet declared it to be a Finite type).  However it is possible
		// that this is a subsequent call to getResult()!!
		final long outlierCount = facts.outliers.values().stream().mapToLong(l-> l).sum();
		if (facts.getMatchTypeInfo().isSemanticType() && !facts.getMatchTypeInfo().isForce()) {
			final LogicalType logical = plugins.getRegistered(facts.getMatchTypeInfo().getSemanticType());

			final PluginAnalysis pluginAnalysis = logical.analyzeSet(context, facts.matchCount, realSamples, facts.getMatchTypeInfo().getRegExp(), facts.calculateFacts(), facts.cardinality, facts.outliers, tokenStreams, analysisConfig);
			if (!pluginAnalysis.isValid()) {
				if (logical.acceptsBaseType(FTAType.STRING) || logical.acceptsBaseType(FTAType.LONG) || logical.acceptsBaseType(FTAType.DOUBLE)) {
					backout(logical, realSamples, pluginAnalysis);
					if (logical instanceof LogicalTypeRegExp)
						backedOutRegExp = true;
				}
			}
			else {
				// Update our Regular Expression - since it may have changed based on all the data observed
				facts.getMatchTypeInfo().setRegExp(logical.getRegExp());
				facts.matchCount += outlierCount - facts.outliers.values().stream().mapToLong(l-> l).sum();
				facts.confidence = logical.getConfidence(facts.matchCount, realSamples, context);
			}
		}

		final FiniteMap cardinalityUpper = new FiniteMap(facts.cardinality);
		final FTAType currentType = facts.getMatchTypeInfo().getBaseType();

		if (FTAType.LONG.equals(currentType))
			finalizeLong(realSamples);
		else if (FTAType.BOOLEAN.equals(currentType))
			finalizeBoolean(realSamples);
		else if (FTAType.DOUBLE.equals(currentType) && !facts.getMatchTypeInfo().isSemanticType())
			finalizeDouble(realSamples);
		else if (FTAType.STRING.equals(currentType))
			finalizeString(realSamples, cardinalityUpper);

		if (FTAType.STRING.equals(facts.getMatchTypeInfo().getBaseType())) {
			if (facts.getMatchTypeInfo().isSemanticType()) {
				final LogicalType logical = plugins.getRegistered(facts.getMatchTypeInfo().getSemanticType());
				boolean recalcConfidence = false;

				// Sweep the outliers - flipping them to invalid if they do not pass the relaxed isValid definition
				for (final Map.Entry<String, Long> entry : facts.outliers.entrySet()) {
					// Split the outliers to either invalid entries or valid entries
					if (logical.isValid(entry.getKey(), false, entry.getValue())) {
						addValid(entry.getKey(), entry.getValue());
						facts.matchCount += entry.getValue();
						recalcConfidence = true;
					}
					else
						addInvalid(entry);
				}

				if (recalcConfidence)
					facts.confidence = logical.getConfidence(facts.matchCount, realSamples, context);
				facts.outliers.clear();
			}
			else {
				// We would really like to say something better than it is a String!
				boolean updated = false;

				// If we are currently matching everything then flip to a better Regular Expression based on Stream analysis if possible
				if (facts.matchCount == realSamples && !tokenStreams.isAnyShape()) {
					final String newRegExp = tokenStreams.getRegExp(false);
					if (newRegExp != null) {
						facts.setMatchTypeInfo(new TypeInfo(null, newRegExp, FTAType.STRING, null, false, null));
						ctxdebug("Type determination", "updated based on Stream analysis {}", facts.getMatchTypeInfo());
					}
				}

				if (!backedOutRegExp)
					updated = checkRegExpTypes(FTAType.STRING);

				final long interestingSamples = facts.sampleCount - (facts.nullCount + facts.blankCount);

				// Try a nice discrete enum
				if (!updated && cardinalityUpper.size() > 1 && cardinalityUpper.size() <= MAX_ENUM_SIZE && (interestingSamples > reflectionSamples || interestingSamples / cardinalityUpper.size() >= 3)) {
					// Rip through the enum doing some basic sanity checks
					RegExpGenerator gen = new RegExpGenerator(MAX_ENUM_SIZE, locale);
					boolean fail = false;
					int excessiveDigits = 0;

					for (final String elt : cardinalityUpper.keySet()) {
						final int length = elt.length();
						// Give up if any one of the strings is too long
						if (length > 40) {
							fail = true;
							break;
						}
						int digits = 0;
						for (int i = 0; i < length; i++) {
							final char ch = elt.charAt(i);
							// Give up if we have some non-expected character
							if (!Character.isAlphabetic(ch) && !Character.isDigit(ch) &&
									ch != '-' && ch != '_' && ch != ' ' && ch != ';' && ch != '.' && ch != ',' && ch != '/' && ch != '(' && ch != ')') {
								fail = true;
								break;
							}

							// Record how many of the elements have 3 or more digits
							if (Character.isDigit(ch)) {
								digits++;
								if (digits == 3)
									excessiveDigits++;
							}
						}

						if (fail)
							break;
						gen.train(elt);
					}

					// If we did not find any reason to reject, output it as an enum
					if (excessiveDigits != cardinalityUpper.size() && !fail) {
						// If we have a significant # of samples and a small number of non-numerics with distinct values attempt to remove outliers
						if (interestingSamples > 1000 && cardinalityUpper.size() < 20 && !gen.isDigit()) {
							final Map<String, Long> sorted = Utils.sortByValue(cardinalityUpper);
							for (final Map.Entry<String, Long> elt : sorted.entrySet()) {
								if (elt.getValue() < 3 && elt.getKey().length() > 3 && distanceLevenshtein(elt.getKey(), cardinalityUpper.keySet()) <= 1) {
									cardinalityUpper.remove(elt.getKey());
									facts.cardinality.entrySet()
									  .removeIf(entry -> entry.getKey().equalsIgnoreCase(elt.getKey()));
									facts.outliers.put(elt.getKey(), elt.getValue());
									facts.matchCount -= elt.getValue();
									facts.confidence = (double) facts.matchCount / realSamples;
								}
							}

							// Regenerate the enum without the outliers removed
							gen = new RegExpGenerator(MAX_ENUM_SIZE, locale);
							for (final String elt : cardinalityUpper.keySet())
								gen.train(elt);
						}

						facts.setMatchTypeInfo(new TypeInfo(null, gen.getResult(), FTAType.STRING, facts.getMatchTypeInfo().typeModifier, false, null));
						updated = true;

						// Now we have mapped to an enum we need to check again if this should be matched to a Semantic type
						for (final LogicalTypeRegExp logical : regExpTypes) {
							if (logical.acceptsBaseType(FTAType.STRING) &&
									logical.isMatch(facts.getMatchTypeInfo().getRegExp()) &&
									logical.analyzeSet(context, facts.matchCount, realSamples, facts.getMatchTypeInfo().getRegExp(), facts.calculateFacts(), facts.cardinality, facts.outliers, tokenStreams, analysisConfig).isValid()) {
								facts.setMatchTypeInfo(new TypeInfo(logical.getRegExp(), logical.getBaseType(), logical.getSemanticType(), facts.getMatchTypeInfo()));
								facts.confidence = logical.getConfidence(facts.matchCount, realSamples, context);
								break;
							}
						}
					}
				}

				// Check to see whether the most common shape matches our regExp and test to see if this valid
				if (!updated && tokenStreams.size() > 1) {
					final TokenStream best = tokenStreams.getBest();
					final String regExp = best.getRegExp(false);
					for (final LogicalTypeRegExp logical : regExpTypes) {
						if (logical.acceptsBaseType(FTAType.STRING) &&
								logical.isMatch(regExp) &&
								logical.analyzeSet(context, best.getOccurrences(), realSamples, facts.getMatchTypeInfo().getRegExp(), facts.calculateFacts(), facts.cardinality, facts.outliers, tokenStreams, analysisConfig).isValid()) {
							facts.setMatchTypeInfo(new TypeInfo(regExp, logical.getBaseType(), logical.getSemanticType(), facts.getMatchTypeInfo()));
							facts.matchCount = best.getOccurrences();
							facts.confidence = logical.getConfidence(facts.matchCount, realSamples, context);
							updated = true;
							break;
						}
					}
				}

				// Qualify Alpha or Alnum with a min and max length
				if (!updated && (KnownTypes.PATTERN_ALPHA_VARIABLE.equals(facts.getMatchTypeInfo().getRegExp()) || KnownTypes.PATTERN_ALPHANUMERIC_VARIABLE.equals(facts.getMatchTypeInfo().getRegExp()))) {
					String newPattern = facts.getMatchTypeInfo().getRegExp();
					newPattern = newPattern.substring(0, newPattern.length() - 1) + lengthQualifier(facts.minTrimmedLength, facts.maxTrimmedLength);
					facts.setMatchTypeInfo(new TypeInfo(null, newPattern, FTAType.STRING, facts.getMatchTypeInfo().typeModifier, false, null));
					updated = true;
				}

				// Qualify random string with a min and max length
				if (!updated && KnownTypes.PATTERN_ANY_VARIABLE.equals(facts.getMatchTypeInfo().getRegExp())) {
					final String newPattern = KnownTypes.freezeANY(facts.minTrimmedLength, facts.maxTrimmedLength, facts.minRawNonBlankLength, facts.maxRawNonBlankLength, facts.leadingWhiteSpace, facts.trailingWhiteSpace, facts.multiline);
					facts.setMatchTypeInfo(new TypeInfo(null, newPattern, FTAType.STRING, facts.getMatchTypeInfo().typeModifier, false, null));
					updated = true;
				}
			}
		}

		checkDateTimeTypes(facts.getMatchTypeInfo().getBaseType());
		checkRegExpTypes(facts.getMatchTypeInfo().getBaseType());

		// Only attempt to do key identification if we have not already been told the answer
		if (facts.keyConfidence == null) {
			// Attempt to identify keys?
			facts.keyConfidence = 0.0;
			if (facts.sampleCount > MIN_SAMPLES_FOR_KEY && analysisConfig.getMaxCardinality() >= MIN_SAMPLES_FOR_KEY / 2 &&
					(facts.cardinality.size() == analysisConfig.getMaxCardinality() || facts.cardinality.size() == facts.sampleCount) &&
					facts.blankCount == 0 && facts.nullCount == 0 &&
					((facts.getMatchTypeInfo().isSemanticType() && "GUID".equals(facts.getMatchTypeInfo().getSemanticType())) ||
					(facts.getMatchTypeInfo().typeModifier == null &&
					((FTAType.STRING.equals(facts.getMatchTypeInfo().getBaseType()) && facts.minRawLength == facts.maxRawLength && facts.minRawLength < 32)
							|| FTAType.LONG.equals(facts.getMatchTypeInfo().getBaseType()))))) {
				facts.keyConfidence = 0.9;

				if (facts.cardinality.size() == analysisConfig.getMaxCardinality())
					// Might be a key but only iff every element in the cardinality
					// set only has a count of 1
					for (final Map.Entry<String, Long> entry : facts.cardinality.entrySet()) {
						if (entry.getValue() != 1) {
							facts.keyConfidence = 0.0;
							break;
						}
					}
			}
		}

		// Only attempt to set uniqueness if we have not already been told the answer
		if (facts.uniqueness == null) {
			if (facts.cardinality.isEmpty())
				facts.uniqueness = 0.0;
			// Can only generate uniqueness if we have not overflowed Max Cardinality
			else if (facts.cardinality.size() < analysisConfig.getMaxCardinality()) {
				int uniques = 0;
				for (final Map.Entry<String, Long> entry : facts.cardinality.entrySet()) {
					if (entry.getValue() == 1)
						uniques++;
				}
				facts.uniqueness = (double)uniques/facts.cardinality.size();
			}
			else if (FTAType.LONG.equals(facts.getMatchTypeInfo().getBaseType()) && (facts.monotonicIncreasing || facts.monotonicDecreasing)) {
				facts.uniqueness = 1.0;
			}
			else
				// -1 indicates we have no perspective on the uniqueness of this field
				facts.uniqueness = -1.0;
		}

		// Only attempt to set distinct count if we have not already been told the answer
		if (facts.distinctCount == null) {
			if (facts.cardinality.size() < analysisConfig.getMaxCardinality())
				facts.distinctCount = (long)facts.cardinality.size();
			else if (FTAType.LONG.equals(facts.getMatchTypeInfo().getBaseType()) && (facts.monotonicIncreasing || facts.monotonicDecreasing))
				facts.distinctCount = facts.matchCount;
			else
				facts.distinctCount = -1L;
		}

		if (isEnabled(Feature.FORMAT_DETECTION))
			facts.streamFormat = Utils.determineStreamFormat(mapper, facts.cardinality);

		TextAnalysisResult result = null;
		// If we have not detected a Semantic Type but the header looks really good, then try excluding the
		// most popular non-valid entry in the hope that it is something like 'NA', 'XX', etc.
		if (FTAType.STRING.equals(facts.getMatchTypeInfo().getBaseType()) && !facts.getMatchTypeInfo().isSemanticType() && !getContext().isNested() && pluginThreshold != 100 && facts.cardinality.size() >= 4) {
			for (final LogicalType logical : plugins.getRegisteredSemanticTypes()) {
				final Map<String, Long> details = facts.synthesizeBulk();
				long worst = (facts.sampleCount - (facts.nullCount + facts.blankCount)) / 20;
				Map.Entry<String, Long> worstEntry = null;
				if (logical.getHeaderConfidence(context) >= 90) {
					for (final Map.Entry<String, Long> entry : details.entrySet()) {
						if (isInteresting(entry.getKey()) && !logical.isValid(entry.getKey()) && entry.getValue() > worst) {
							worstEntry = entry;
							worst = entry.getValue();
						}
					}
					if (worstEntry != null) {
						details.remove(worstEntry.getKey());
						final TextAnalysisResult newResult = reAnalyze(details);
						if (newResult.isSemanticType() && newResult.getSemanticType().equals(logical.getSemanticType())) {
							newResult.getFacts().invalid.put(worstEntry.getKey(), worstEntry.getValue());
							if (!getFacts().outliers.isEmpty()) {
								newResult.getFacts().invalid.putAll(getFacts().outliers);
								newResult.getFacts().sampleCount += getFacts().outliers.values().stream().mapToLong(l-> l).sum();
								for (final Map.Entry<String, Long> entry : getFacts().outliers.entrySet())
									newResult.getFacts().lengths[Math.min(entry.getKey().length(), facts.lengths.length - 1)] += entry.getValue();
							}
							newResult.getFacts().sampleCount += worstEntry.getValue();
							newResult.getFacts().confidence -= 0.05;
							newResult.getFacts().getMatchTypeInfo().setBaseType(FTAType.STRING);
							newResult.getFacts().lengths[Math.min(worstEntry.getKey().length(), facts.lengths.length - 1)] += worstEntry.getValue();
							result = newResult;
							ctxdebug("Type determination", "was STRING, post exclusion analyis ({}, {}), matchTypeInfo {} -> {} ",
									worstEntry.getKey(), worstEntry.getValue(), facts.matchTypeInfo, newResult.getFacts().getMatchTypeInfo());
							break;
						}
					}
				}
			}
		}

		// If we have not detected a Semantic Type - then attempt to exclude outliers and re-analyze
		if (analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS) &&
				analysisConfig.isEnabled(TextAnalyzer.Feature.DISTRIBUTIONS) &&
				FTAType.LONG.equals(facts.getMatchTypeInfo().getBaseType()) &&
				!facts.getMatchTypeInfo().isSemanticType() &&
				facts.cardinality.size() != analysisConfig.getMaxCardinality() &&
				!getContext().isNested() && pluginThreshold != 100 && facts.matchCount >= 20) {
			final Map<String, Long> details = facts.synthesizeBulk();
			final Map<String, Long> outliers = new HashMap<>();
			final Histogram.Entry[] buckets = facts.calculateFacts().getHistogram().getHistogram(10);
			final StringConverter stringConverter = facts.getStringConverter();

			// Associate each bucket with a cluster - so that we can do the outlier detection
			Histogram.tagClusters(buckets);

			// Identify any outliers based on 'density-based clustering' (using the Histograms to generate the clusters)
			for (final Map.Entry<String, Long> entry : details.entrySet()) {
				if (Utils.isNumeric(entry.getKey())) {
					final double value = stringConverter.toDouble(entry.getKey());
					final Histogram.Entry bucket = Histogram.getBucket(buckets, value);
					// Scratch any cluster with less than 2% of the samples
					if (bucket.getClusterPercent() < .02)
						outliers.put(entry.getKey(), entry.getValue());
				}
			}

			// If we identified any outliers then re-analyze using the 'cleaned' set and see if we have success
			if (!outliers.isEmpty()) {
				details.keySet().removeAll(outliers.keySet());
				final TextAnalysisResult newResult = reAnalyze(details);

				final FTAType newType = newResult.getFacts().getMatchTypeInfo().getBaseType();

				if (newResult.isSemanticType() || newType.isDateOrTimeType()) {
					// We found a new Semantic Type so add the old invalids & outliers to the current invalids and update the sample count
					for (final Map.Entry<String, Long> entry : outliers.entrySet()) {
						newResult.getFacts().outliers.mergeIfSpace(entry.getKey(), entry.getValue(), Long::sum);
						newResult.getFacts().lengths[Math.min(entry.getKey().length(), facts.lengths.length - 1)] += entry.getValue();
					}
					newResult.getFacts().sampleCount += outliers.values().stream().mapToLong(l-> l).sum();
					for (final Map.Entry<String, Long> entry : getFacts().invalid.entrySet()) {
						newResult.getFacts().invalid.mergeIfSpace(entry.getKey(), entry.getValue(), Long::sum);
						newResult.getFacts().lengths[Math.min(entry.getKey().length(), facts.lengths.length - 1)] += entry.getValue();
					}
					newResult.getFacts().sampleCount += getFacts().invalid.values().stream().mapToLong(l-> l).sum();
					newResult.getFacts().confidence -= 0.05;
					result = newResult;
					ctxdebug("Type determination", "was LONG, post outlier analyis, matchTypeInfo - {}", newResult.getFacts().getMatchTypeInfo());
				}
			}
		}

		// If we have not detected a Semantic Type and we have a double masquerading as a Long then re-analyze with a long set
		if (analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS) &&
				FTAType.DOUBLE.equals(facts.getMatchTypeInfo().getBaseType()) &&
				!facts.getMatchTypeInfo().isSemanticType() &&
				facts.allZeroes &&
				!getContext().isNested() && pluginThreshold != 100 && facts.matchCount >= 20) {
			final Map<String, Long> doubleDetails = facts.synthesizeBulk();
			final Map<String, Long> details = new HashMap<>();
			final StringConverter stringConverter = facts.getStringConverter();

			for (final Map.Entry<String, Long> entry : doubleDetails.entrySet())
				if (entry.getKey() == null || entry.getKey().isBlank())
					details.put(entry.getKey(), entry.getValue());
				else
					details.put(String.valueOf(((Double)stringConverter.getValue(entry.getKey())).longValue()), entry.getValue());

			final TextAnalysisResult newResult = reAnalyze(details);

			final FTAType newType = newResult.getFacts().getMatchTypeInfo().getBaseType();

			if (newResult.isSemanticType()) {
				facts.getMatchTypeInfo().setSemanticType(newResult.getSemanticType());
				ctxdebug("Type determination", "was DOUBLE, post LONG conversion, matchTypeInfo - {}", newResult.getFacts().getMatchTypeInfo());
			}
			else if (FTAType.LOCALDATE.equals(newType)) {
				final TypeInfo interimTypeInfo = newResult.getFacts().getMatchTypeInfo();
				final String trailingZeroes = "." + Utils.repeat('0',  facts.zeroesLength);
				final String updatedModifier = interimTypeInfo.typeModifier + "'" + trailingZeroes + "'";
				final int updatedLength = interimTypeInfo.typeModifier.length() + trailingZeroes.length();
				final TypeInfo newTypeInfo = new TypeInfo(null, newResult.getFacts().getMatchTypeInfo().getRegExp() + "\\Q" + trailingZeroes + "\\E", FTAType.LOCALDATE, updatedModifier, false, updatedModifier);
				final DateTimeFormatter interimFormatter = dateTimeParser.ofPattern(interimTypeInfo.format);
				switchToDate(newTypeInfo,
						LocalDate.parse(String.valueOf(Double.valueOf(facts.minDoubleNonZero).longValue()), interimFormatter),
						LocalDate.parse(String.valueOf(Double.valueOf(facts.maxDouble).longValue()), interimFormatter));
				ctxdebug("Type determination", "was DOUBLE, post LONG conversion, matchTypeInfo - {}", newResult.getFacts().getMatchTypeInfo());
			}
		}

		if (analysisConfig.isEnabled(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES)) {
			final PluginDefinition pluginDefinition = PluginDefinition.findByName("IDENTIFIER");
			final LogicalType identifier = LogicalTypeFactory.newInstance(pluginDefinition, analysisConfig);

			if (!facts.getMatchTypeInfo().isSemanticType() && !getContext().isNested() &&
					((facts.external.keyConfidence != null && facts.external.keyConfidence == 1.0) ||
					(facts.uniqueness == 1.0 && facts.matchCount >= 20 &&
						identifier.analyzeSet(context, facts.matchCount, realSamples, facts.getMatchTypeInfo().getRegExp(), facts.calculateFacts(), facts.cardinality, facts.outliers, tokenStreams, analysisConfig).isValid()))) {
				facts.getMatchTypeInfo().setRegExp(facts.getRegExp());
				facts.getMatchTypeInfo().setSemanticType(identifier.getSemanticType());
				// If the keyConfidence was not set externally and we have concluded we have an IDENTIFIER set the keyConfidence to reflect this
				if (facts.external.keyConfidence == null) {
					facts.confidence = facts.external.totalCount == facts.sampleCount ? 1.0 : identifier.getConfidence(facts.matchCount, realSamples, context);
					facts.keyConfidence = facts.confidence;
				}
				else
					facts.confidence = 1.0;

				ctxdebug("Type determination", "post Uniqueness analyis, matchTypeInfo - {}", facts.getMatchTypeInfo());
			}
		}

		// If we are in SIMPLE mode (i.e. not Bulk) and we have not detected a Semantic Type - try replaying accumulated set in Bulk mode,
		// this has the potential to pick up entries where the first <n> (by default 20 are misleading).
		if (FTAType.STRING.equals(facts.getMatchTypeInfo().getBaseType()) && !facts.getMatchTypeInfo().isSemanticType() && analysisConfig.getTrainingMode() == AnalysisConfig.TrainingMode.SIMPLE && pluginThreshold != 100) {
			final TextAnalysisResult bulkResult = reAnalyze(facts.synthesizeBulk());
			if (bulkResult.isSemanticType() || bulkResult.getType() != facts.getMatchTypeInfo().getBaseType()) {
				if (!getFacts().outliers.isEmpty()) {
					bulkResult.getFacts().invalid.putAll(getFacts().outliers);
					bulkResult.getFacts().sampleCount += getFacts().outliers.values().stream().mapToLong(l-> l).sum();
				}
				bulkResult.getFacts().confidence -= 0.05;
				result = bulkResult;
				ctxdebug("Type determination", "was STRING, post Bulk analyis, matchTypeInfo - {}", bulkResult.getFacts().getMatchTypeInfo());
			}
		}

		if (result == null)
			result = new TextAnalysisResult(context.getStreamName(), facts.calculateFacts(), context.getDateResolutionMode(), analysisConfig, tokenStreams);

		if (traceConfig != null) {
			traceConfig.recordResult(result, internalErrors);
			traceConfig.tag("getResult", getFacts().getSampleCount());
		}

		return result;
	}

	private boolean isInteresting(final String input) {
		return input != null && !input.isBlank();
	}

	private boolean checkDateTimeTypes(final FTAType type) {
		return resultFinalizer.checkDateTimeTypes(type);
	}

	boolean checkRegExpTypes(final FTAType type) {
		return resultFinalizer.checkRegExpTypes(type);
	}

	private void switchToDate(final TypeInfo newTypeInfo, final LocalDate newMin, final LocalDate newMax) {
		resultFinalizer.switchToDate(newTypeInfo, newMin, newMax);
	}

	// Called to finalize a LONG type determination when NOT a Semantic type
	void finalizeLong(final long realSamples) {
		resultFinalizer.finalizeLong(realSamples);
	}

	protected void killInvalidDates() {
		resultFinalizer.killInvalidDates();
	}

	private void finalizeBoolean(final long realSamples) {
		resultFinalizer.finalizeBoolean(realSamples);
	}

	// Called to finalize a DOUBLE type determination when NOT a Semantic type
	private void finalizeDouble(final long realSamples) {
		resultFinalizer.finalizeDouble(realSamples);
	}

	private void finalizeString(final long realSamples, final FiniteMap cardinalityUpper) {
		resultFinalizer.finalizeString(realSamples, cardinalityUpper);
	}

	/**
	 * Access the training set - this will typically be the first {@link AnalysisConfig#DETECT_WINDOW_DEFAULT} records.
	 *
	 * @return A List of the raw input strings.
	 */
	public List<String>getTrainingSet() {
		return raw;
	}

	/**
	 * Serialize a TextAnalyzer - commonly used in concert with {@link #deserialize(String)} and {@link #merge(TextAnalyzer, TextAnalyzer)}
	 * to merge TextAnalyzers run on separate shards into a single TextAnalyzer and hence a single TextAnalysisResult.
	 * @return A Serialized version of this TextAnalyzer which can be hydrated via deserialize().
	 * @throws FTAPluginException Thrown when a registered plugin has detected an issue
	 * @throws FTAUnsupportedLocaleException Thrown when a requested locale is not supported
	 */
	public String serialize() throws FTAPluginException, FTAUnsupportedLocaleException {
		return AnalyzerSerializer.serialize(this);
	}

	/**
	 * Create a new TextAnalyzer from a serialized representation - used in concert with {@link #serialize()} and {@link #merge(TextAnalyzer, TextAnalyzer)}
	 * to merge TextAnalyzers run on separate shards into a single TextAnalyzer and hence a single TextAnalysisResult.
	 * @param serialized The serialized form of a TextAnalyzer.
	 * @return A new TextAnalyzer which can be merged with another TextAnalyzer to product a single result.
	 * @throws FTAMergeException When we fail to de-serialize the provided String.
	 * @throws FTAUnsupportedLocaleException Thrown when a requested locale is not supported
	 * @throws FTAPluginException Thrown when a registered plugin has detected an issue
	 */
	public static TextAnalyzer deserialize(final String serialized) throws FTAMergeException, FTAPluginException, FTAUnsupportedLocaleException {
		return AnalyzerSerializer.deserialize(serialized);
	}

	/**
	 * Create a new TextAnalyzer which is the result of merging two separate TextAnalyzers.
	 * This is typically used to merge TextAnalyzers run on separate shards into a single TextAnalyzer and hence a single TextAnalysisResult.
	 * See also {@link #serialize() and @link #deserialize(String)}.
	 * @param first The first TextAnalyzer
	 * @param second The second TextAnalyzer
	 * @return A new TextAnalyzer which is a merge of the two arguments.
	 * @throws FTAMergeException If the AnalysisConfig for both TextAnalyzers are not identical
	 * @throws FTAUnsupportedLocaleException Thrown when a requested locale is not supported
	 * @throws FTAPluginException Thrown when a registered plugin has detected an issue
	 */
	public static TextAnalyzer merge(final TextAnalyzer first, final TextAnalyzer second) throws FTAMergeException, FTAPluginException, FTAUnsupportedLocaleException  {
		return AnalyzerSerializer.merge(first, second);
	}

	protected Facts getFacts() {
		return facts;
	}

	protected void setExternalFacts(final Facts.ExternalFacts externalFacts) {
		facts.external = externalFacts;
	}

	@Override
	public boolean equals(final Object obj) {
		return equals(obj, 0.0);
	}

	public boolean equals(final Object obj, final double epsilon) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final TextAnalyzer other = (TextAnalyzer) obj;
		return Objects.equals(analysisConfig, other.analysisConfig) && Objects.equals(context, other.context)
				&& facts.equals(other.facts, epsilon);
	}
}
