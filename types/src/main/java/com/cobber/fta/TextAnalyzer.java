/*
 * Copyright 2017-2025 Tim Segall
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

import com.cobber.fta.TextAnalyzer.Escalation;
import com.cobber.fta.TextAnalyzer.Observation;
import com.cobber.fta.TextAnalyzer.OutlierAnalysis;
import com.cobber.fta.TextAnalyzer.SignStatus;
import com.cobber.fta.core.FTAMergeException;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.FTAUnsupportedLocaleException;
import com.cobber.fta.core.InternalErrorException;
import com.cobber.fta.core.PatternFG;
import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.core.RegExpSplitter;
import com.cobber.fta.core.TraceException;
import com.cobber.fta.core.Utils;
import com.cobber.fta.dates.DateTimeParser;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;
import com.cobber.fta.dates.DateTimeParserResult;
import com.cobber.fta.dates.LocaleInfo;
import com.cobber.fta.token.Token;
import com.cobber.fta.token.TokenStream;
import com.cobber.fta.token.TokenStreams;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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

	private Trace traceConfig;

	private int internalErrors;

	private final static String insufficient = "Insufficient digits in input (";

	private Collator collator;

	private String localizedYes;
	private String localizedNo;

	private boolean cacheCheck;

	private Correlation correlation;

	private static List<PluginDefinition> pluginDefinitions = new ArrayList<>();

	private static boolean nullTextAsNull;

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

	/**
	 * An Escalation contains three regExps in order of increasing genericity.  So for example the following 3 regExps:
	 *
	 * - [\p{IsAlphabetic}\d]{10}
	 * - [\p{IsAlphabetic}\d]+
	 * - .+
	 *
	 * would all describe "A43BCHK12L".
	 */
	class Escalation {
		StringBuilder[] level;
		TypeInfo typeInfo;

		@Override
		public int hashCode() {
			return level[0].toString().hashCode() + 7 * level[1].toString().hashCode() + 11 * level[2].toString().hashCode();
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final Escalation other = (Escalation) obj;

			return Arrays.equals(level, other.level);
		}

		Escalation() {
			level = new StringBuilder[3];
		}
	}

	// Maintain a list of the Escalations for the first Detect Window samples
	private List<Escalation> detectWindowEscalations;

	// Maintain a list (corresponding to the levels) of the keys and their frequencies
	List<Map<String, Integer>> frequencies = new ArrayList<>();

	private boolean trainingStarted;
	private boolean initialized;

	private Facts facts;

	private int possibleDateTime;

	private final List<LogicalTypeInfinite> infiniteTypes = new ArrayList<>();
	private final List<LogicalTypeFinite> finiteTypes = new ArrayList<>();
	private final List<LogicalTypeRegExp> regExpTypes = new ArrayList<>();
	private int[] candidateCounts;

	private final KnownTypes knownTypes = new KnownTypes();
	private Keywords keywords;

	private DateTimeParser dateTimeParser;

	private final ObjectMapper mapper = new ObjectMapper();

	private final Plugins plugins = new Plugins(mapper);

	private static final ObjectMapper serializationMapper = new ObjectMapper().registerModule(new JavaTimeModule()).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

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
		ret.getPlugins().registerPluginList(getPlugins().getUserDefinedPlugins(), getStreamName(), getConfig());

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
		// We always want to track basic facts for the field
		final int length = input.length();

		if (length < facts.minRawLength)
			facts.minRawLength = length;
		if (length > facts.maxRawLength)
			facts.maxRawLength = length;

		final int trimmedLength = trimmed.length();
		facts.lengths[Math.min(trimmedLength, facts.lengths.length - 1)] += count;


		if (trimmedLength != 0) {
			if (length != 0 && length < facts.minRawNonBlankLength)
				facts.minRawNonBlankLength = length;
			if (length > facts.maxRawNonBlankLength)
				facts.maxRawNonBlankLength = length;

			tokenStreams.track(trimmed, count);
		}
	}

	private boolean trackLong(final String trimmed, final TypeInfo typeInfo, final boolean register, final long count) {
		// Track String facts - just in case we end up backing out.
		if (facts.getMinString() == null || facts.getMinString().compareTo(trimmed) > 0)
			facts.setMinString(trimmed);

		if (facts.getMaxString() == null || facts.getMaxString().compareTo(trimmed) < 0)
			facts.setMaxString(trimmed);

		long l;

		// Interpret the String as a long, first attempt uses parseLong which is fast (although not localized), if that fails,
		// then try using a NumberFormatter which will cope with grouping separators (e.g. 1,000).
		int digits = trimmed.length();
		final char firstCh = trimmed.charAt(0);

		try {
			if (typeInfo.isTrailingMinus()) {
				if (digits >= 2 && trimmed.charAt(digits - 1) == '-') {
					l = -Long.parseLong(trimmed.substring(0, digits - 1));
					digits--;
				}
				else
					l = Long.parseLong(trimmed);
			}
			else {
				l = Long.parseLong(trimmed);
				if (firstCh == '-' || firstCh == '+')
					digits--;
			}
		} catch (NumberFormatException e) {
			final ParsePosition pos = new ParsePosition(0);
			final String cleaned = firstCh == '+' ? trimmed.substring(1) : trimmed;
			final Number n = longFormatter.parse(cleaned, pos);
			if (n == null || cleaned.length() != pos.getIndex())
				return false;
			l = n.longValue();
			if (trimmed.indexOf(ni.groupingSeparator) != -1) {
				facts.groupingSeparators++;
				if (!facts.getMatchTypeInfo().isSemanticType() && !facts.getMatchTypeInfo().hasGrouping()) {
					facts.setMatchTypeInfo(knownTypes.grouping(facts.getMatchTypeInfo().getRegExp()));
					ctxdebug("Type determination", "now with grouping {}", facts.getMatchTypeInfo());
				}
			}
			digits = trimmed.length();
			if (ni.hasNegativePrefix && (firstCh == '-' || firstCh == '+' || firstCh == ni.negativePrefix))
				digits--;
			if (l < 0 && ni.hasNegativeSuffix)
				digits--;
		}

		if (typeInfo.isSemanticType()) {
			// If it is a registered Infinite Semantic Type then validate it
			final LogicalType logical = plugins.getRegistered(typeInfo.getSemanticType());
			if (logical.acceptsBaseType(FTAType.LONG) && !logical.isValid(trimmed, false, count))
				return false;
		}

		if (register) {
			if (firstCh == '0' && digits != 1)
				facts.leadingZeroCount++;

			if (digits < facts.minTrimmedLengthNumeric)
				facts.minTrimmedLengthNumeric = digits;
			if (digits > facts.maxTrimmedLengthNumeric)
				facts.maxTrimmedLengthNumeric = digits;

			if (l != 0 && l < facts.getMinLongNonZero())
				facts.setMinLongNonZero(l);

			if (l < facts.getMinLong())
				facts.setMinLong(l);
			else
				facts.monotonicDecreasing = false;

			if (l > facts.getMaxLong())
				facts.setMaxLong(l);
			else
				facts.monotonicIncreasing = false;

			if (analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS)) {
				// Avoids any work if the existing mean is the same as the input
				if (l != facts.mean) {
					// Calculate the mean & standard deviation using Welford's algorithm (weighted)
					final double oldMean = facts.mean;
					final long newCount = facts.matchCount + count;
					facts.mean += (count * (l - oldMean)) / newCount;
					facts.currentM2 += count * ((l - facts.mean) * (l - oldMean));
				}

				facts.tbLong.observe(l);
			}
		}

		return true;
	}

	private boolean trackBoolean(final String trimmed) {
		final String trimmedLower = trimmed.toLowerCase(locale);

		final boolean isTrue = "true".equals(trimmedLower) || "yes".equals(trimmedLower) || "y".equals(trimmedLower) ||
				(localizedYes != null && collator.compare(trimmed, localizedYes) == 0);
		final boolean isFalse = !isTrue && ("false".equals(trimmedLower) || "no".equals(trimmedLower) || "n".equals(trimmedLower)) ||
				(localizedNo != null && collator.compare(trimmed, localizedNo) == 0);

		if (isTrue) {
			if (facts.minBoolean == null)
				facts.minBoolean = trimmedLower;
			if (facts.maxBoolean == null || "false".equals(facts.maxBoolean) || "no".equals(facts.maxBoolean) || "n".equals(facts.maxBoolean) || (localizedNo != null && localizedNo.equals(facts.maxBoolean)))
				facts.maxBoolean = trimmedLower;
		} else if (isFalse) {
			if (facts.maxBoolean == null)
				facts.maxBoolean = trimmedLower;
			if (facts.minBoolean == null || "true".equals(facts.minBoolean) || "yes".equals(facts.maxBoolean) || "y".equals(facts.maxBoolean) || (localizedYes != null && localizedYes.equals(facts.maxBoolean)))
				facts.minBoolean = trimmedLower;
		}

		return isTrue || isFalse;
	}

	private boolean trackString(final String rawInput, final String trimmed, final TypeInfo typeInfo, final boolean register, final long count) {
		if (register && analysisConfig.getDebug() >= 2 && rawInput.length() > 0 && rawInput.charAt(0) == '¶' && "¶ xyzzy ¶".equals(rawInput))
			throw new NullPointerException("¶ xyzzy ¶");

		if (typeInfo.isSemanticType()) {
			// If it is a registered Infinite Semantic Type then validate it
			final LogicalType logical = plugins.getRegistered(typeInfo.getSemanticType());
			if (logical.acceptsBaseType(FTAType.STRING) && !logical.isValid(rawInput, false, count))
				return false;
		}
		else {
			for (int i = 0; i < trimmed.length(); i++) {
				if (typeInfo.isAlphabetic() && !Character.isAlphabetic(trimmed.charAt(i)))
					return false;
				if (typeInfo.isAlphanumeric() && !Character.isLetterOrDigit((trimmed.charAt(i))))
					return false;
			}
		}

		updateStats(rawInput);

		return true;
	}

	private void updateStats(final String cleaned) {
		final int len = cleaned.trim().length();

		if (facts.getMinString() == null || facts.getMinString().compareTo(cleaned) > 0)
			facts.setMinString(cleaned);

		if (facts.getMaxString() == null || facts.getMaxString().compareTo(cleaned) < 0)
			facts.setMaxString(cleaned);

		if (len < facts.minTrimmedLength)
			facts.minTrimmedLength = len;
		if (len > facts.maxTrimmedLength)
			facts.maxTrimmedLength = len;

		if (analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS))
			facts.tbString.observe(cleaned);
	}

	private boolean trackDouble(final String rawInput, final TypeInfo typeInfo, final boolean register, final long count) {
		final String input = rawInput.trim();
		double d = 0.0;
		boolean converted = false;

		if (facts.decimalSeparator == '.')
			try {
				// parseDouble is not locale sensitive, but it is fast!
				if (typeInfo.isTrailingMinus()) {
					final int digits = input.length();
					if (digits >= 2 && input.charAt(digits - 1) == '-')
						d = -Double.parseDouble(input.substring(0, digits - 1));
					else
						d = Double.parseDouble(input);
				}
				else
					d = Double.parseDouble(input);
				converted = true;
			} catch (NumberFormatException | StringIndexOutOfBoundsException e) {
				// IGNORE - note converted will be false
			}

		if (!converted) {
			// If we think we are a Non Localized number then no point in using the locale-aware parsing
			if (typeInfo.isNonLocalized())
				return false;
			Double dd;
			try {
				// Failed to parse using the naive parseDouble, so use the locale-sensitive Numberformat.parse
				dd = Utils.parseDouble(input, doubleFormatter);
			} catch (NumberFormatException e) {
				return false;
			}
			if (dd == null)
				return false;
			d = dd;
			if (input.indexOf(ni.groupingSeparator) != -1) {
				if (!hasGroupingSeparator(input, ni.groupingSeparator, ni.decimalSeparator))
					return false;
				facts.groupingSeparators++;
			}
			// Make sure to track the decimal separator being used for doubles
			if (ni.decimalSeparator != '.' && facts.decimalSeparator != ni.decimalSeparator && input.indexOf(ni.decimalSeparator) != -1)
				facts.decimalSeparator = ni.decimalSeparator;
		}

		// If it is NaN/Infinity then we are all done
		if (Double.isNaN(d) || Double.isInfinite(d))
			return false;

		if (register && analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS)) {
			if (d < facts.minDouble)
				facts.minDouble = d;

			if (d > facts.maxDouble)
				facts.maxDouble = d;

			if (d != 0.0 && d < facts.minDoubleNonZero)
				facts.minDoubleNonZero = d;

			// This test avoids the loop if the existing mean is the same as the input
			if (d != facts.mean)
				for (int i = 0; i < count; i++) {
					// Calculate the mean & standard deviation using Welford's algorithm
					final double delta = d - facts.mean;
					// matchCount is one low - because we do not 'count' the record until we return from this routine indicating valid
					facts.mean += delta / (facts.matchCount + i + 1);
					facts.currentM2 += delta * (d - facts.mean);
				}

			facts.tbDouble.observe(d);

			// Track whether we have ever seen a double with a non-zero fractional component
			if (facts.allZeroes) {
				final int separatorIndex = input.indexOf(facts.decimalSeparator);
				if (separatorIndex != -1 && !Utils.allZeroes(input.substring(separatorIndex + 1)))
					facts.allZeroes = false;
				else {
					if (facts.zeroesLength == -1)
						facts.zeroesLength = input.length() - separatorIndex - 1;
					else if (facts.zeroesLength != input.length() - separatorIndex - 1)
						facts.allZeroes = false;
				}
			}
		}

		if (typeInfo.isSemanticType()) {
			// If it is a registered Infinite Semantic Type then validate it
			final LogicalType logical = plugins.getRegistered(typeInfo.getSemanticType());
			if (logical.acceptsBaseType(FTAType.DOUBLE) && !logical.isValid(input, false, count))
				return false;
		}

		return true;
	}

	/*
	 * Validate (and track) the date/time/datetime input.
	 * This routine is called for every date/time/datetime we see in the input, so performance is critical.
	 */
	private boolean trackDateTime(final String input, final TypeInfo typeInfo, final boolean register, final long count) {
		final String dateFormat = typeInfo.format;

		// Retrieve the (likely cached) DateTimeParserResult for the supplied dateFormat
		final DateTimeParserResult result = DateTimeParserResult.asResult(dateFormat, context.getDateResolutionMode(), dateTimeParser.getConfig());
		if (result == null)
			throw new InternalErrorException("NULL result for " + dateFormat);

		final DateTimeFormatter formatter = dateTimeParser.ofPattern(result.getFormatString());

		final String trimmed = input.trim();

		// If we are not collecting statistics we can use the parse on DateTimeParserResult which is
		// significantly faster than the parse on LocalTime/LocalDate/LocalDateTime/...
		switch (result.getType()) {
		case LOCALTIME:
			if (register && analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS)) {
				final LocalTime localTime = LocalTime.parse(trimmed, formatter);
				if (facts.minLocalTime == null || localTime.compareTo(facts.minLocalTime) < 0)
					facts.minLocalTime = localTime;
				if (facts.maxLocalTime == null || localTime.compareTo(facts.maxLocalTime) > 0)
					facts.maxLocalTime = localTime;
				facts.tbLocalTime.observe(localTime);
			}
			else
				result.parse(trimmed);
			break;

		case LOCALDATE:
			if (register && analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS)) {
				final LocalDate localDate = LocalDate.parse(trimmed, formatter);
				if (facts.minLocalDate == null || localDate.compareTo(facts.minLocalDate) < 0)
					facts.minLocalDate = localDate;
				if (facts.maxLocalDate == null || localDate.compareTo(facts.maxLocalDate) > 0)
					facts.maxLocalDate = localDate;
				facts.tbLocalDate.observe(localDate);
			}
			else
				result.parse(trimmed);
			break;

		case LOCALDATETIME:
			if (register && analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS)) {
				final LocalDateTime localDateTime = LocalDateTime.parse(trimmed, formatter);
				if (facts.minLocalDateTime == null || localDateTime.compareTo(facts.minLocalDateTime) < 0)
					facts.minLocalDateTime = localDateTime;
				if (facts.maxLocalDateTime == null || localDateTime.compareTo(facts.maxLocalDateTime) > 0)
					facts.maxLocalDateTime = localDateTime;
				facts.tbLocalDateTime.observe(localDateTime);
			}
			else
				result.parse(trimmed);
			break;

		case ZONEDDATETIME:
			if (register && analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS)) {
				final ZonedDateTime zonedDateTime = ZonedDateTime.parse(trimmed, formatter);
				if (facts.minZonedDateTime == null || zonedDateTime.compareTo(facts.minZonedDateTime) < 0)
					facts.minZonedDateTime = zonedDateTime;
				if (facts.maxZonedDateTime == null || zonedDateTime.compareTo(facts.maxZonedDateTime) > 0)
					facts.maxZonedDateTime = zonedDateTime;
				facts.tbZonedDateTime.observe(zonedDateTime);
			}
			else
				result.parse(trimmed);
			break;

		case OFFSETDATETIME:
			if (register && analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS)) {
				final OffsetDateTime offsetDateTime = OffsetDateTime.parse(trimmed, formatter);
				if (facts.minOffsetDateTime == null || offsetDateTime.compareTo(facts.minOffsetDateTime) < 0)
					facts.minOffsetDateTime = offsetDateTime;
				if (facts.maxOffsetDateTime == null || offsetDateTime.compareTo(facts.maxOffsetDateTime) > 0)
					facts.maxOffsetDateTime = offsetDateTime;
				facts.tbOffsetDateTime.observe(offsetDateTime);
			}
			else
				result.parse(trimmed);
			break;

		default:
			throw new InternalErrorException("Expected Date/Time type.");
		}

		if (typeInfo.isSemanticType()) {
			// If it is a registered Infinite Semantic Type then validate it
			final LogicalType logical = plugins.getRegistered(typeInfo.getSemanticType());
			if (logical.acceptsBaseType(result.getType()) && !logical.isValid(input, false, count))
				return false;
		}

		return true;
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
		synchronized (pluginDefinitions) {
			if (pluginDefinitions.isEmpty()) {
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
	}

	private void initialize() throws FTAPluginException, FTAUnsupportedLocaleException {
		memoryDebug("initialize.entry");

		facts.initialize(getTopBottomK());

		mapper.registerModule(new JavaTimeModule());
		mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

		if (LocaleInfo.isSupported(locale) != null)
			throw new FTAUnsupportedLocaleException(LocaleInfo.isSupported(locale));

		collator = Collator.getInstance(locale);
		collator.setStrength(Collator.PRIMARY);

		raw = new ArrayList<>(analysisConfig.getDetectWindow());
		detectWindowEscalations = new ArrayList<>(analysisConfig.getDetectWindow());

		tokenStreams = new TokenStreams(getMaxShapes());

		// If enabled, load the default set of plugins for Semantic Type detection
		if (analysisConfig.isEnabled(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES))
			registerDefaultPlugins(analysisConfig);

		for (final LogicalType logical : plugins.getRegisteredSemanticTypes()) {

			if ((logical instanceof LogicalTypeFinite) && ((LogicalTypeFinite)logical).getSize() + 10 > getMaxCardinality())
				throw new FTAPluginException("Internal error: Max Cardinality: " + getMaxCardinality() + " is insufficient to support plugin: " + logical.getSemanticType());

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

		initialized = true;
		memoryDebug("initialize.exit");
	}

	private void initializeTrace() {
		// If no trace options already set then pick them up from the environment (if set)
		if (analysisConfig.getTraceOptions() == null) {
			final String ftaTrace = System.getenv("FTA_TRACE");
			if (ftaTrace != null && !ftaTrace.isEmpty())
				analysisConfig.setTraceOptions(ftaTrace);
		}

		if (analysisConfig.getTraceOptions() != null)
			traceConfig = new Trace(analysisConfig.getTraceOptions(), context,  analysisConfig);
	}

	private void updateNumericPattern(final Escalation escalation, final SignStatus signStatus, final int numericDecimalSeparators, final int possibleExponentSeen, final boolean nonLocalizedDouble) {
		if (signStatus == SignStatus.TRAILING_MINUS)
			escalation.typeInfo = knownTypes.getByID(numericDecimalSeparators == 1  ? KnownTypes.ID.ID_SIGNED_DOUBLE_TRAILING : KnownTypes.ID.ID_SIGNED_LONG_TRAILING);
		else {
			final boolean numericSigned = signStatus == SignStatus.LOCALE_STANDARD || signStatus == SignStatus.LEADING_SIGN;

			if (numericDecimalSeparators == 1) {
				if (possibleExponentSeen == -1) {
					if (nonLocalizedDouble)
						escalation.typeInfo = knownTypes.getByID(numericSigned ? KnownTypes.ID.ID_SIGNED_DOUBLE_NL : KnownTypes.ID.ID_DOUBLE_NL);
					else
						escalation.typeInfo = knownTypes.getByID(numericSigned ? KnownTypes.ID.ID_SIGNED_DOUBLE : KnownTypes.ID.ID_DOUBLE);
				}
				else {
					escalation.typeInfo = knownTypes.getByID(numericSigned ? KnownTypes.ID.ID_SIGNED_DOUBLE_WITH_EXPONENT : KnownTypes.ID.ID_DOUBLE_WITH_EXPONENT);
				}
			}
			else {
				if (possibleExponentSeen == -1)
					escalation.typeInfo = knownTypes.getByID(numericSigned ? KnownTypes.ID.ID_SIGNED_LONG : KnownTypes.ID.ID_LONG);
				else
					escalation.typeInfo = knownTypes.getByID(numericSigned ? KnownTypes.ID.ID_SIGNED_DOUBLE_WITH_EXPONENT : KnownTypes.ID.ID_DOUBLE_WITH_EXPONENT);
			}
		}

		escalation.level[1] = new StringBuilder(escalation.typeInfo.getRegExp());
		escalation.level[2] = new StringBuilder(knownTypes.negation(escalation.typeInfo.getRegExp()).getRegExp());
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
			trackLengthAndShape(rawInput, trimmed, count);
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

	final static int CACHE_SIZE = 10;
	FiniteMap cache = new FiniteMap(CACHE_SIZE);

	private void emptyCache() {
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
		for (Map.Entry<String, Long> entry : cache.entrySet()) {
			final String key = entry.getKey();
			if (key != null && ((logical != null && logical.isValid(key)) || (regExp != null && key.matches(regExp))))
				trainBulkCore(entry.getKey(), entry.getValue());
			else
				invalid.put(key, entry.getValue());
		}

		// Now process the invalid entries
		for (Map.Entry<String, Long> entry : invalid.entrySet())
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
		boolean result;
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

	private static PatternFG patternFG = null;
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

		if (patternFG == null)
			patternFG = PatternFG.compile(keywords.get("NO_DATA"));

		return patternFG.matcher(input);
	}

	enum SignStatus {
		NONE,
		LOCALE_STANDARD,
		LEADING_SIGN,
		TRAILING_MINUS
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

		for (int i = 0; i < count; i++)
			raw.add(rawInput);

		final int length = trimmed.length();

		// Analyze the input to determine a set of attributes including whether it is numeric
		final NumericResult nr = Numeric.analyze(trimmed, length, ni);

		final StringBuilder compressedl0 = new StringBuilder(length);
		if (nr.alphasSeen != 0 && nr.digitsSeen != 0 && nr.alphasSeen + nr.digitsSeen == length) {
			compressedl0.append(KnownTypes.PATTERN_ALPHANUMERIC).append('{').append(String.valueOf(length)).append('}');
		} else if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
			compressedl0.append(KnownTypes.PATTERN_BOOLEAN_TRUE_FALSE);
		} else if (localizedYes != null && (collator.compare(trimmed, localizedYes) == 0 || collator.compare(trimmed, localizedNo) == 0)) {
			compressedl0.append(KnownTypes.PATTERN_BOOLEAN_YES_NO_LOCALIZED);
		} else if ("yes".equalsIgnoreCase(trimmed) || "no".equalsIgnoreCase(trimmed)) {
			compressedl0.append(KnownTypes.PATTERN_BOOLEAN_YES_NO);
		} else if ("y".equalsIgnoreCase(trimmed) || "n".equalsIgnoreCase(trimmed)) {
			compressedl0.append(KnownTypes.PATTERN_BOOLEAN_Y_N);
		} else {
			String l0withSentinel = nr.l0.toString() + "|";
			// Walk the new level0 to create the new level1
			if (nr.couldBeNumeric && nr.numericGroupingSeparators > 0)
				l0withSentinel = l0withSentinel.replace("G", "");
			char last = l0withSentinel.charAt(0);
			int repetitions = 1;
			for (int i = 1; i < l0withSentinel.length(); i++) {
				final char ch = l0withSentinel.charAt(i);
				if (ch == last && i + 1 != l0withSentinel.length()) {
					repetitions++;
				} else {
					if (last == 'd' || last == 'a') {
						compressedl0.append(last == 'd' ? KnownTypes.PATTERN_NUMERIC : KnownTypes.PATTERN_ALPHA);
						compressedl0.append('{').append(String.valueOf(repetitions)).append('}');
					} else {
						for (int j = 0; j < repetitions; j++) {
							compressedl0.append(last);
						}
					}
					last = ch;
					repetitions = 1;
				}
			}
		}
		final Escalation escalation = new Escalation();
		escalation.level[0] = compressedl0;

		if (dateTimeParser.determineFormatString(trimmed) != null)
			possibleDateTime++;

		// Check to see if this input is one of our registered Infinite Semantic Types
		int c = 0;
		for (final LogicalTypeInfinite logical : infiniteTypes) {
			try {
				if ((facts.getMatchTypeInfo() == null || logical.acceptsBaseType(facts.getMatchTypeInfo().getBaseType())) && logical.isCandidate(trimmed, compressedl0, nr.charCounts, nr.lastIndex))
					candidateCounts[c]++;
			}
			catch (Exception e) {
				LoggerFactory.getLogger("com.cobber.fta").error("Plugin: {}, issue: {}.", logical.getSemanticType(), e.getMessage());
			}
			c++;
		}

		// Create the level 1 and 2
		if (nr.digitsSeen > 0 && nr.couldBeNumeric && nr.numericDecimalSeparators <= 1) {
			updateNumericPattern(escalation, nr.numericSigned, nr.numericDecimalSeparators, nr.possibleExponentSeen, nr.nonLocalizedDouble);
		} else {
			// Fast version of replaceAll("\\{\\d*\\}", "+"), e.g. replace \d{5} with \d+
			final StringBuilder collapsed = new StringBuilder(compressedl0);
			for (int i = 0; i < collapsed.length(); i++) {
				if (collapsed.charAt(i) == '{' && i + 1 < collapsed.length() && Character.isDigit(collapsed.charAt(i + 1))) {
					final int start = i++;
					while (collapsed.charAt(++i) != '}')
						/* EMPTY */;
					collapsed.replace(start, i + 1, "+");
				}
			}

			// Level 1 is the collapsed version e.g. convert \d{4}-\d{2}-\d{2] to
			// \d+-\d+-\d+
			escalation.level[1] = new StringBuilder(collapsed);
			escalation.level[2] = new StringBuilder(KnownTypes.PATTERN_ANY_VARIABLE);
		}

		detectWindowEscalations.add(escalation);

		return facts.getMatchTypeInfo() != null && facts.getMatchTypeInfo().getBaseType() != null;
	}

	private Map.Entry<String, Integer> getBest(final int levelIndex) {
		final Map<String, Integer> frequency = frequencies.get(levelIndex);

		// Grab the best and the second best based on frequency
		Map.Entry<String, Integer> best = null;
		Map.Entry<String, Integer> secondBest = null;
		Map.Entry<String, Integer> thirdBest = null;
		TypeInfo bestPattern = null;
		TypeInfo secondBestPattern = null;
		TypeInfo thirdBestPattern = null;
		String newKey = null;

		// Handle numeric promotion
		for (final Map.Entry<String, Integer> entry : frequency.entrySet()) {

			if (best == null) {
				best = entry;
				bestPattern = knownTypes.getByRegExp(best.getKey());
			}
			else if (secondBest == null) {
				secondBest = entry;
				secondBestPattern = knownTypes.getByRegExp(secondBest.getKey());
				if (levelIndex != 0 && bestPattern != null && secondBestPattern != null &&
						bestPattern.isNumeric() && secondBestPattern.isNumeric()) {
					newKey = knownTypes.numericPromotion(bestPattern.getRegExp(), secondBestPattern.getRegExp());
					if (newKey != null) {
						best = new AbstractMap.SimpleEntry<>(newKey, best.getValue() + secondBest.getValue());
						bestPattern = knownTypes.getByRegExp(best.getKey());
					}
				}
			}
			else if (thirdBest == null) {
				thirdBest = entry;
				thirdBestPattern = knownTypes.getByRegExp(thirdBest.getKey());
				if (levelIndex != 0 && bestPattern != null && thirdBestPattern != null &&
						bestPattern.isNumeric() && thirdBestPattern.isNumeric()) {
					newKey = knownTypes.numericPromotion(newKey != null ? newKey : bestPattern.getRegExp(), thirdBestPattern.getRegExp());
					if (newKey != null) {
						best = new AbstractMap.SimpleEntry<>(newKey, best.getValue() + thirdBest.getValue());
						bestPattern = knownTypes.getByRegExp(best.getKey());
					}
				}
			}
		}

		// Promote almost anything to STRING, DOUBLES are pretty clear (unlike LONGS) so do not promote these
		if (bestPattern != null && secondBestPattern != null && !FTAType.DOUBLE.equals(bestPattern.getBaseType()))
			if (FTAType.STRING.equals(bestPattern.getBaseType()))
				best = new AbstractMap.SimpleEntry<>(best.getKey(), best.getValue() + secondBest.getValue());
			else if (secondBestPattern.id == KnownTypes.ID.ID_ANY_VARIABLE)
				best = new AbstractMap.SimpleEntry<>(secondBest.getKey(), best.getValue() + secondBest.getValue());

		return best;
	}

	/*
	 * collapse() will attempt to coalesce samples to be more generic so we stay at a lower level rather than
	 * ending up with '.+' which is not very informative.  So for example, with the following pair:
	 * [\p{IsAlphabetic}{10}, \p{IsAlphabetic}+, .+]
	 * [[\p{IsAlphabetic}\d]{10}, [\p{IsAlphabetic}\d]+, .+]
	 * The first element will be 'promoted' to the second.
	 */
	private void collapse() {
		// Map from Escalation hash to count of occurrences
		final Map<Integer, Integer> observedFrequency = new HashMap<>();
		// Map from Escalation hash to Escalation
		final Map<Integer, Escalation> observedSet = new HashMap<>();

		int longCount = 0;
		int doubleCount = 0;
		int totalCount = 0;
		Escalation eDouble = null;
		for (final Escalation e : detectWindowEscalations) {
			totalCount++;
			if (e.typeInfo != null)
				switch (e.typeInfo.getBaseType()) {
					case LONG:
						longCount++;
						break;
					case DOUBLE:
						eDouble = e;
						doubleCount++;
						break;
					default:
						break;
				}
		}

		// If we have a full complement of numerics which is a mix of longs and doubles - then we want to switch to doubles
		final boolean switchToDouble = longCount != 0 && doubleCount != 0 && longCount + doubleCount == totalCount;

		// Calculate the frequency of every element
		for (final Escalation e : detectWindowEscalations) {
			// If we are switching to doubles and this entry is a long then just replace it with a random double entry
			final Escalation eObserved = switchToDouble && e.typeInfo.getBaseType() == FTAType.LONG ? eDouble : e;
			final int hash = eObserved.hashCode();
			final Integer seen = observedFrequency.get(hash);
			if (seen == null) {
				observedFrequency.put(hash, 1);
				observedSet.put(hash, eObserved);
			} else {
				observedFrequency.put(hash, seen + 1);
			}
		}

		for (int i = 0; i < 3; i++) {
			final Map<String, Integer> keyFrequency = new HashMap<>();
			for (final Map.Entry<Integer, Integer> entry : observedFrequency.entrySet()) {
				final Escalation escalation = observedSet.get(entry.getKey());
				keyFrequency.merge(escalation.level[i].toString(), entry.getValue(), Integer::sum);
			}

			// If it makes sense rewrite our sample data switching numeric/alpha matches to alphanumeric matches
			if (keyFrequency.size() > 1) {
				final Set<String> keys = new HashSet<>(keyFrequency.keySet());
				for (final String oldKey : keys) {
					String newKey = oldKey.replace(KnownTypes.PATTERN_NUMERIC, KnownTypes.PATTERN_ALPHANUMERIC);
					if (!newKey.equals(oldKey) && keys.contains(newKey)) {
						final int oldCount = keyFrequency.remove(oldKey);
						final int currentCount = keyFrequency.get(newKey);
						keyFrequency.put(newKey, currentCount + oldCount);
					} else {
						newKey = oldKey.replace(KnownTypes.PATTERN_ALPHA, KnownTypes.PATTERN_ALPHANUMERIC);
						if (!newKey.equals(oldKey) && keys.contains(newKey)) {
							final int oldCount = keyFrequency.remove(oldKey);
							final int currentCount = keyFrequency.get(newKey);
							keyFrequency.put(newKey, currentCount + oldCount);
						}
					}
				}
			}
			frequencies.add(Utils.sortByValue(keyFrequency));
		}
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
		final String[] semanticTypes = context.getSemanticTypes();
		if (semanticTypes == null)
			return false;

		final String semanticType = semanticTypes[context.getStreamIndex()];

		if (isInteresting(semanticType)) {
			final PluginDefinition pluginDefinition = PluginDefinition.findByName(semanticType);
			if (pluginDefinition == null) {
				debug("ERROR: Failed to locate plugin named '{}'", semanticType);
				return false;
			}

			LogicalType logical = null;
			try {
				logical = LogicalTypeFactory.newInstance(pluginDefinition, analysisConfig);
			} catch (FTAPluginException e) {
				debug("ERROR: Failed to instantiate plugin named '{}', error: {}", semanticType, e.getMessage());
				return false;
			}
			final TypeInfo answer = new TypeInfo(logical.getRegExp(), logical.getBaseType(), logical.getSemanticType(), facts.getMatchTypeInfo());
			answer.setForce(true);

			facts.setMatchTypeInfo(answer);
			ctxdebug("Type determination", "infinite type, confidence: FORCED, matchTypeInfo - {}", facts.getMatchTypeInfo());

			return true;
		}

		return false;
	}

	/**
	 * This is the core routine for determining the type of the field. It is
	 * responsible for setting matchTypeInfo.
	 */
	private void determineType() {
		if (facts.sampleCount == 0) {
			facts.setMatchTypeInfo(knownTypes.getByRegExp(KnownTypes.PATTERN_ANY_VARIABLE));
			return;
		}

		collapse();

		int level0value = 0;
		int level1value = 0;
		int level2value = 0;
		String level0pattern = null;
		String level1pattern = null;
		String level2pattern = null;
		TypeInfo level0typeInfo = null;
		TypeInfo level1typeInfo = null;
		TypeInfo level2typeInfo = null;
		final Map.Entry<String, Integer> level0 = getBest(0);
		final Map.Entry<String, Integer> level1 = getBest(1);
		final Map.Entry<String, Integer> level2 = getBest(2);
		Map.Entry<String, Integer> best = level0;

		if (level0 != null) {
			level0pattern = level0.getKey();
			level0value = level0.getValue();
			level0typeInfo = knownTypes.getByRegExp(level0pattern);

			if (level0typeInfo == null) {
				for (final LogicalTypeRegExp logical : regExpTypes) {
					// Check that the samples match the pattern we are looking for and the pattern returned.
					// For example to be an IDENTITY.EIN_US you need to match
					//    "regExpsToMatch": [ "\\d{2}-\\d{7}" ],
					// as well as
					//    "regExpReturned": "(0[1-6]|1[0-6]|2[0-7]|3[0-9]|4[0-8]|5[0-9]|6[0-8]|7[1-7]|8[0-8]|9[01234589])-\\d{7}",
					if (logical.isMatch(level0pattern) &&  tokenStreams.matches(logical.getRegExp(), logical.getThreshold()) != 0) {
						level0typeInfo = new TypeInfo(logical.getRegExp(), logical.getBaseType(), logical.getSemanticType(), facts.getMatchTypeInfo());
						break;
					}
				}
			}
		}
		if (level1 != null) {
			level1pattern = level1.getKey();
			level1value = level1.getValue();
			level1typeInfo = knownTypes.getByRegExp(level1pattern);
		}
		if (level2 != null) {
			level2pattern = level2.getKey();
			level2value = level2.getValue();
			level2typeInfo = knownTypes.getByRegExp(level2pattern);
		}

		if (best != null) {
			facts.setMatchTypeInfo(level0typeInfo);

			// Take any 'reasonable' (80%) level 1 with something we recognize or a better count
			if (level1 != null && (double)level1value/raw.size() >= 0.8 && (level0typeInfo == null || level1value > level0value)) {
				best = level1;
				facts.setMatchTypeInfo(level1typeInfo);
			}

			// Take any level 2 if
			// - we have something we recognize (and we had nothing)
			// - we have the same key but a better count
			// - we have different keys but same type (signed versus not-signed)
			// - we have different keys, two numeric types and an improvement of at least 5%
			// - we have different keys, different types and an improvement of at least 10% and we are below the threshold
			if (level2 != null &&
					((facts.getMatchTypeInfo() == null && level2typeInfo != null)
					|| (best.getKey().equals(level2pattern) && level2value > best.getValue())
					|| (!best.getKey().equals(level2pattern) && level2typeInfo != null
							&& facts.getMatchTypeInfo().getBaseType().equals(level2typeInfo.getBaseType())
							&& level2value > best.getValue())
					|| (!best.getKey().equals(level2pattern) && level2typeInfo != null
							&& facts.getMatchTypeInfo().isNumeric()
							&& level2typeInfo.isNumeric()
							&& level2value >= 1.05 * best.getValue())
					|| (!best.getKey().equals(level2pattern) && (double)best.getValue()/raw.size() < (double)analysisConfig.getThreshold()/100
							&& level2value >= 1.10 * best.getValue()))) {
				facts.setMatchTypeInfo(level2typeInfo);

				// If we are really unhappy with this determination then just back out to String
				if ((double)level2value/raw.size() < 0.8)
					facts.setMatchTypeInfo(knownTypes.getByID(KnownTypes.ID.ID_ANY_VARIABLE));

			}

			if (possibleDateTime != 0 && possibleDateTime + 1 >= raw.size()) {

				// This next try/catch is unnecessary in theory, if there are zero bugs then it will never trip,
				// if there happens to be an issue then we swallow it and will not detect the date/datetime.
				try {
					for (final String sample : raw)
						dateTimeParser.train(sample);

					final DateTimeParserResult result = dateTimeParser.getResult();
					final String formatString = result.getFormatString();
					facts.setMatchTypeInfo(new TypeInfo(null, result.getRegExp(), result.getType(), formatString, false, formatString));
				}
				catch (RuntimeException e) {
				    debug("Internal error: {}", e);
				}
			}

			ctxdebug("Type determination", "initial, matchTypeInfo - {}", facts.getMatchTypeInfo());

			// Check to see if it might be one of the known Infinite Semantic Types
			int i = 0;
			double bestConfidence = 0.0;
			for (final LogicalTypeInfinite logical : infiniteTypes) {
				if (logical.acceptsBaseType(facts.getMatchTypeInfo().getBaseType()) && logical.getConfidence(candidateCounts[i], raw.size(), context)  >= logical.getThreshold()/100.0) {
					int count = 0;
					final TypeInfo candidate = new TypeInfo(logical.getRegExp(), logical.getBaseType(), logical.getSemanticType(), facts.getMatchTypeInfo());
					for (final String sample : raw) {
						switch (logical.getBaseType()) {
						case STRING:
							if (trackString(sample, sample.trim(),  candidate, false, 1))
								count++;
							break;
						case LONG:
							if (trackLong(sample.trim(), candidate, false, 1))
								count++;
							break;
						case DOUBLE:
							if (trackDouble(sample, candidate, false, 1))
								count++;
							break;
						default:
							break;
						}
					}

					// If a reasonable number look genuine then we are convinced
					final double currentConfidence = logical.getConfidence(count, raw.size(), context);
					if (currentConfidence > bestConfidence && currentConfidence >= logical.getThreshold()/100.0) {
						facts.setMatchTypeInfo(candidate);
						bestConfidence = currentConfidence;
						ctxdebug("Type determination", "infinite type, confidence: {}, matchTypeInfo - {}", currentConfidence, facts.getMatchTypeInfo());
					}
				}
				i++;
			}

			// Try a regExp match nice and early - we can always back out
			for (final LogicalTypeRegExp logical : regExpTypes) {
				if (logical.acceptsBaseType(facts.getMatchTypeInfo().getBaseType()) &&
						logical.isMatch(facts.getMatchTypeInfo().getRegExp())) {
					facts.setMatchTypeInfo(new TypeInfo(logical.getRegExp(), logical.getBaseType(), logical.getSemanticType(), facts.getMatchTypeInfo()));
					ctxdebug("Type determination", "was '{}', matchTypeInfo - {}", facts.getMatchTypeInfo().getBaseType(), facts.getMatchTypeInfo());
					break;
				}
			}

			raw.forEach((value) -> trackResult(value, value.trim(), false, 1));
		}
	}

	private void addValid(final String input, final long count) {
		final boolean added = facts.cardinality.mergeIfSpace(input, count, Long::sum);
		// If Cardinality blown track remaining set in a Sketch
		if (!added && analysisConfig.isEnabled(TextAnalyzer.Feature.DISTRIBUTIONS) && !facts.getMatchTypeInfo().getBaseType().equals(FTAType.STRING)) {
			facts.getSketch().accept(input, count);
			if (facts.getCardinalityOverflow() == null)
				facts.createHistogramOverflow(new StringConverter(facts.getMatchTypeInfo().getBaseType(), new TypeFormatter(facts.getMatchTypeInfo(), analysisConfig)));
			facts.getCardinalityOverflow().accept(input, count);
		}
	}

	private void addOutlier(final String input, final long count) {
		final String cleaned = input.trim();
		final int trimmedLength = cleaned.length();

		if (trimmedLength < facts.minTrimmedOutlierLength)
			facts.minTrimmedOutlierLength = trimmedLength;
		if (trimmedLength > facts.maxTrimmedOutlierLength)
			facts.maxTrimmedOutlierLength = trimmedLength;

		if (facts.minOutlierString == null || facts.minOutlierString.compareTo(cleaned) > 0)
			facts.minOutlierString = cleaned;

		if (facts.maxOutlierString == null || facts.maxOutlierString.compareTo(cleaned) < 0)
			facts.maxOutlierString = cleaned;

		outliersSmashed.mergeIfSpace(Token.generateKey(input), count, Long::sum);

		facts.outliers.mergeIfSpace(input, count, Long::sum);
	}

	private void addInvalid(final Map.Entry<String, Long> entry) {
		facts.invalid.mergeIfSpace(entry.getKey(), entry.getValue(), Long::sum);
	}

	private boolean hasGroupingSeparator(final String input, final char groupingSeparator, final char decimalSeparator) {
		int digitsLength = 0;
		boolean decimalSeparatorSeen = false;
		boolean groupingSeparatorSeen = false;
		boolean exponentSeen = false;

		for (int i = 0; i < input.length(); i++) {
			final char ch = input.charAt(i);
			if (Character.isDigit(ch))
				digitsLength++;
			else if (ch == groupingSeparator) {
				if (decimalSeparatorSeen || digitsLength > 3 || (groupingSeparatorSeen && digitsLength != 3))
					return false;
				digitsLength = 0;
				groupingSeparatorSeen = true;
			}
			else if (ch == decimalSeparator) {
				if (decimalSeparatorSeen || digitsLength > 3 || (groupingSeparatorSeen && digitsLength != 3))
					return false;
				digitsLength = 0;
				decimalSeparatorSeen = true;
			}
			else if (ch == 'e' || ch == 'E')
				exponentSeen = true;
		}

		return groupingSeparatorSeen && (decimalSeparatorSeen || exponentSeen || digitsLength == 0 || digitsLength == 3);
	}

	class OutlierAnalysis {
		long alphas;
		long digits;
		long spaces;
		long other;
		int doubles;
		long nonAlphaNumeric;
		boolean negative;
		boolean exponent;
		boolean grouping;

		private boolean isDouble(final String input) {
			try {
				Double.parseDouble(input.trim());
			} catch (NumberFormatException e) {
				final ParsePosition pos = new ParsePosition(0);
				final Number n = doubleFormatter.parse(input, pos);
				if (n == null || input.length() != pos.getIndex())
					return false;
				if (hasGroupingSeparator(input, ni.groupingSeparator, ni.decimalSeparator))
					grouping = true;
			}
			return true;
		}

		OutlierAnalysis(final Map<String, Long> outliers,  final TypeInfo current) {
			// Sweep the current outliers
			for (final Map.Entry<String, Long> entry : outliers.entrySet()) {
				final String key = entry.getKey();
				final Long value = entry.getValue();
				if (FTAType.LONG.equals(current.getBaseType()) && isDouble(key)) {
					doubles++;
					if (!negative)
						negative = key.charAt(0) == '-';
					if (!exponent)
						exponent = key.indexOf('e') != -1 || key.indexOf('E') != -1;
				}
				boolean foundAlpha = false;
				boolean foundDigit = false;
				boolean foundSpace = false;
				boolean foundOther = false;
				final int len = key.length();
				for (int i = 0; i < len; i++) {
					final Character c = key.charAt(i);
					if (Character.isAlphabetic(c))
						foundAlpha = true;
				    else if (Character.isDigit(c))
						foundDigit = true;
				    else if (Character.isWhitespace(c))
						foundSpace = true;
				    else
						foundOther = true;
				}
				if (foundAlpha)
					alphas += value;
				if (foundDigit)
					digits += value;
				if (foundSpace)
					spaces += value;
				if (foundOther)
					other += value;
				if (foundSpace || foundOther)
					nonAlphaNumeric += value;
			}
		}
	}

	private boolean conditionalBackoutToPattern(final long realSamples, final TypeInfo current) {
		final OutlierAnalysis analysis = new OutlierAnalysis(facts.outliers, current);

		final long badCharacters = current.isAlphabetic() ? analysis.digits : analysis.alphas;
		// If we are currently Alphabetic and the only errors are digits then convert to AlphaNumeric
		if (badCharacters != 0 && analysis.spaces == 0 && analysis.other == 0 && current.isAlphabetic()) {
			if (facts.outliers.size() == analysisConfig.getMaxOutliers() || analysis.digits > .01 * realSamples) {
				backoutToPatternID(realSamples, KnownTypes.ID.ID_ALPHANUMERIC_VARIABLE);
				return true;
			}
		}
		// If we are currently Numeric and the only errors are alpha then convert to AlphaNumeric
		else if (badCharacters != 0 && analysis.spaces == 0 && analysis.other == 0 && FTAType.LONG.equals(current.getBaseType())) {
			if (facts.outliers.size() == analysisConfig.getMaxOutliers() || analysis.alphas > .01 * realSamples) {
				backoutToPattern(realSamples, KnownTypes.PATTERN_ALPHANUMERIC_VARIABLE);
				return true;
			}
		}
		// If we are currently Numeric and the only errors are doubles then convert to double
		else if (!facts.getMatchTypeInfo().isSemanticType() && facts.outliers.size() == analysis.doubles && FTAType.LONG.equals(current.getBaseType())) {
			KnownTypes.ID id;
			if (analysis.exponent)
				id = (current.isSigned() || analysis.negative) ? KnownTypes.ID.ID_SIGNED_DOUBLE_WITH_EXPONENT : KnownTypes.ID.ID_DOUBLE_WITH_EXPONENT;
			else
				id = (current.isSigned() || analysis.negative) ? KnownTypes.ID.ID_SIGNED_DOUBLE : KnownTypes.ID.ID_DOUBLE;
			if (current.hasGrouping() || analysis.grouping)
				id = knownTypes.grouping(knownTypes.getByID(id).getRegExp()).id;
			backoutToPatternID(realSamples, id);
			return true;
		}
		else if ((realSamples > reflectionSamples && facts.outliers.size() == analysisConfig.getMaxOutliers())
					|| (badCharacters + analysis.nonAlphaNumeric) > .01 * realSamples) {
				backoutToPattern(realSamples, KnownTypes.PATTERN_ANY_VARIABLE);
				return true;
		}

		return false;
	}

	private void backoutToPattern(final long realSamples, final String newPattern) {
		TypeInfo newTypeInfo = knownTypes.getByRegExp(newPattern);

		// If it is not one of our known types then construct a suitable TypeInfo
		if (newTypeInfo == null)
			newTypeInfo = new TypeInfo(null, newPattern, FTAType.STRING, null, 0);

		backoutToTypeInfo(realSamples, newTypeInfo);
	}

	private void backoutToPatternID(final long realSamples, final KnownTypes.ID patternID) {
		backoutToTypeInfo(realSamples, knownTypes.getByID(patternID));
	}

	private void backoutToString(final long realSamples) {
		facts.matchCount = realSamples;

		// All outliers are now part of the cardinality set and there are now no outliers
		facts.cardinality.putAll(facts.outliers);

		final RegExpGenerator gen = new RegExpGenerator(MAX_ENUM_SIZE, locale);
		for (final String s : facts.cardinality.keySet())
			gen.train(s);

		final String newPattern = gen.getResult();
		TypeInfo newTypeInfo = knownTypes.getByRegExp(newPattern);

		// If it is not one of our known types then construct a suitable TypeInfo
		if (newTypeInfo == null)
			newTypeInfo = new TypeInfo(null, newPattern, FTAType.STRING, null, 0);

		facts.setMatchTypeInfo(newTypeInfo);

		for (final Entry<String, Long> entry : facts.cardinality.entrySet())
			trackString(entry.getKey(), entry.getKey().trim(), newTypeInfo, false, entry.getValue());

		facts.outliers.clear();
		outliersSmashed.clear();
		ctxdebug("Type determination", "backing out, matchTypeInfo - {}", facts.getMatchTypeInfo());
	}

	private void backoutToTypeInfo(final long realSamples, final TypeInfo newTypeInfo) {
		facts.matchCount = realSamples;
		facts.setMatchTypeInfo(newTypeInfo);

		// All outliers are now part of the cardinality set and there are now no outliers
		for (final Map.Entry<String, Long> entry : facts.outliers.entrySet())
			facts.cardinality.merge(entry.getKey(), entry.getValue(), Long::sum);

		// Need to update stats to reflect any outliers we previously ignored
		if (facts.getMatchTypeInfo().getBaseType().equals(FTAType.STRING)) {
			for (final Map.Entry<String, Long> entry : facts.cardinality.entrySet())
				trackString(entry.getKey(), entry.getKey().trim(), newTypeInfo, false, entry.getValue());
		}
		else if (facts.getMatchTypeInfo().getBaseType().equals(FTAType.DOUBLE)) {
			facts.minDouble = facts.getMinLong();
			facts.maxDouble = facts.getMaxLong();
			for (final Map.Entry<String, Long> entry : facts.outliers.entrySet())
				trackDouble(entry.getKey(), facts.getMatchTypeInfo(), true, entry.getValue());
		}

		facts.outliers.clear();
		outliersSmashed.clear();
		ctxdebug("Type determination", "backing out, matchTypeInfo - {}", facts.getMatchTypeInfo());
	}

	/**
	 * Backout from a mistaken Semantic type whose base type was DOUBLE
	 * @param logical The Semantic type we are backing out from
	 * @param realSamples The number of real samples we have seen.
	 */
	private void backoutLogicalDoubleType(final LogicalType logical, final long realSamples) {
		long otherDoubles = 0;

		final Map<String, Long> doubleOutliers = new HashMap<>();

		// Back out to a Double with the same Modifiers we already have (e.g., SIGNED, NON_LOCALIZED, ...
		facts.setMatchTypeInfo(knownTypes.getByTypeAndModifier(FTAType.DOUBLE, facts.getMatchTypeInfo().typeModifierFlags));

		// Sweep the current outliers and check they are part of the set
		for (final Map.Entry<String, Long> entry : facts.outliers.entrySet()) {
			try {
				Double.parseDouble(entry.getKey());
				otherDoubles += entry.getValue();
				doubleOutliers.put(entry.getKey(), entry.getValue());
			} catch (NumberFormatException el) {
				// Swallow
			}
		}

		// Move the doubles from the outlier set to the cardinality set
		facts.matchCount += otherDoubles;
		facts.outliers.entrySet().removeAll(doubleOutliers.entrySet());
		for (final Map.Entry<String, Long> entry : doubleOutliers.entrySet())
			addValid(entry.getKey(), entry.getValue());

		ctxdebug("Type determination", "backing out double, matchTypeInfo - {}", facts.getMatchTypeInfo());
	}

	/**
	 * Backout from a mistaken Semantic type whose base type was LONG
	 * @param logical The Semantic type we are backing out from
	 * @param realSamples The number of real samples we have seen.
	 */
	private void backoutLogicalLongType(final LogicalType logical, final long realSamples) {
		long otherLongs = 0;
		long otherDoubles = 0;

		final Map<String, Long> longOutliers = new HashMap<>();
		final Map<String, Long> doubleOutliers = new HashMap<>();

		// Sweep the current outliers and check they are part of the set
		for (final Map.Entry<String, Long> entry : facts.outliers.entrySet()) {
			try {
				Long.parseLong(entry.getKey());
				otherLongs += entry.getValue();
				longOutliers.put(entry.getKey(), entry.getValue());
			} catch (NumberFormatException el) {
				try {
					Double.parseDouble(entry.getKey());
					otherDoubles += entry.getValue();
					doubleOutliers.put(entry.getKey(), entry.getValue());
				}
				catch (NumberFormatException ed) {
					// Swallow
				}
			}
		}

		// Move the longs from the outlier set to the cardinality set
		facts.matchCount += otherLongs;
		facts.outliers.entrySet().removeAll(longOutliers.entrySet());

		// So if all the values observed to date have been monotic increasing (or decreasing) then we should preserve this fact
		final boolean saveMonitonicIncreasing = facts.monotonicIncreasing;
		final boolean saveMonitonicDecreasing = facts.monotonicDecreasing;
		for (final Map.Entry<String, Long> entry : longOutliers.entrySet()) {
			trackLong(entry.getKey().trim(), knownTypes.getByID(KnownTypes.ID.ID_LONG), true, entry.getValue());
			addValid(entry.getKey(), entry.getValue());
		}
		facts.monotonicIncreasing = saveMonitonicIncreasing;
		facts.monotonicDecreasing = saveMonitonicDecreasing;

		if ((double) facts.matchCount / realSamples > analysisConfig.getThreshold()/100.0) {
			facts.setMatchTypeInfo(knownTypes.getByID(KnownTypes.ID.ID_LONG));
			ctxdebug("Type determination", "backing out long, matchTypeInfo - {}", facts.getMatchTypeInfo());
		}
		else if ((double)(facts.matchCount + otherDoubles) / realSamples > analysisConfig.getThreshold()/100.0) {
			facts.setMatchTypeInfo(knownTypes.getByID(KnownTypes.ID.ID_DOUBLE));
			facts.outliers.entrySet().removeAll(doubleOutliers.entrySet());
			for (final Map.Entry<String, Long> entry : doubleOutliers.entrySet())
				addValid(entry.getKey(), entry.getValue());
			facts.matchCount += otherDoubles;

			// Recalculate the world since we now 'know' it is a double
	        facts.mean = 0.0;
	        facts.variance = 0.0;
	        facts.currentM2 = 0.0;

	        facts.cardinality.forEach((k, v) -> trackDouble(k, facts.getMatchTypeInfo(), true, v));
		}
		else
			backoutToPatternID(realSamples, KnownTypes.ID.ID_ANY_VARIABLE);
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
			trackLengthAndShape(rawInput, trimmed, count);

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
				trackDateTime(input, facts.getMatchTypeInfo(), true, count);
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
			checkRegExpTypes();
			cacheCheck = true;
		}
	}

	private void backout(final LogicalType logical, final long realSamples, final PluginAnalysis pluginAnalysis) {
		if (FTAType.STRING.equals(facts.getMatchTypeInfo().getBaseType()))
			backoutToPattern(realSamples, pluginAnalysis.getNewPattern());
		else if (FTAType.LONG.equals(facts.getMatchTypeInfo().getBaseType()))
			backoutLogicalLongType(logical, realSamples);
		else
			backoutLogicalDoubleType(logical, realSamples);

		facts.confidence = (double) facts.matchCount / realSamples;
	}

	/**
	 * Determine if the current dataset reflects a Semantic type.
	 * @param logical The Semantic type we are testing
	 * @return A MatchResult that indicates the quality of the match against the provided data
	 */
	private FiniteMatchResult checkFiniteSet(final FiniteMap cardinalityUpper, final FiniteMap outliers, final LogicalTypeFinite logical) {
		long realSamples = facts.sampleCount - (facts.nullCount + facts.blankCount);
		long missCount = 0;				// count of number of misses

		final FiniteMap newOutliers = new FiniteMap(outliers.getMaxCapacity());
		final Map<String, Long> addMatches = new HashMap<>();
		final double missThreshold = 1.0 - logical.getThreshold()/100.0;
		long validCount = 0;

		for (final Map.Entry<String, Long> entry : outliers.entrySet()) {
			final String upper = entry.getKey().toUpperCase(Locale.ENGLISH);
			if (logical.isValid(upper, true, entry.getValue())) {
				validCount += entry.getValue();
				addMatches.merge(upper, entry.getValue(), Long::sum);
			}
			else {
				missCount += entry.getValue();
				newOutliers.merge(entry.getKey(), entry.getValue(), Long::sum);
			}
		}

		// Sweep the balance and check they are part of the set
		if ((double) missCount / realSamples > missThreshold)
			return new FiniteMatchResult();

		final Map<String, Long> minusMatches = new HashMap<>();

		final Set<String> ignorable = logical.getIgnorable();

		long missEntries = 0;
		Map.Entry<String, Long> missEntry = null;

		for (final Map.Entry<String, Long> entry : cardinalityUpper.entrySet()) {
			if (ignorable != null && ignorable.contains(entry.getKey())) {
				realSamples -= entry.getValue();
				minusMatches.put(entry.getKey(), entry.getValue());
				newOutliers.put(entry.getKey(), entry.getValue());
			}
			else if (logical.isValid(entry.getKey(), true, entry.getValue()))
				validCount += entry.getValue();
			else {
				missEntries++;
				if (missEntry == null || entry.getValue() > missEntry.getValue())
					missEntry = entry;
				minusMatches.put(entry.getKey(), entry.getValue());
				newOutliers.put(entry.getKey(), entry.getValue());
			}
		}

		final FiniteMap newCardinality = new FiniteMap(cardinalityUpper.getMaxCapacity());
		newCardinality.putAll(cardinalityUpper);
		newCardinality.putAll(addMatches);
		for (final String elt : minusMatches.keySet())
			newCardinality.remove(elt);

		final long outlierCount = newOutliers.values().stream().mapToLong(l-> l).sum();
		if (logical.analyzeSet(context, validCount, realSamples, facts.getMatchTypeInfo().getRegExp(), facts.calculateFacts(), newCardinality, newOutliers, tokenStreams, analysisConfig).isValid()) {
			validCount += outlierCount - newOutliers.values().stream().mapToLong(l-> l).sum();
			return new FiniteMatchResult(logical, logical.getConfidence(validCount, realSamples, context), validCount, newOutliers, newCardinality);
		}

		// If the number of misses is less than 10% then remove the worst offender since it will often be something
		// silly like All, Other, N/A, ...
		if (missEntries != 0 && (double)missEntries/cardinalityUpper.size() < .1 && logical.getHeaderConfidence(context.getStreamName()) >= 90) {
			realSamples -= missEntry.getValue();
			if (logical.analyzeSet(context, validCount, realSamples, facts.getMatchTypeInfo().getRegExp(), facts.calculateFacts(), newCardinality, newOutliers, tokenStreams, analysisConfig).isValid())
				return new FiniteMatchResult(logical, logical.getConfidence(validCount, realSamples, context), validCount, newOutliers, newCardinality);
		}

		return new FiniteMatchResult();
	}

	private String lengthQualifier(final int min, final int max) {
		if (!isEnabled(Feature.LENGTH_QUALIFIER))
			return min > 0 ? "+" : ".";

		return RegExpSplitter.qualify(min, max);
	}

	/**
	 * Given a Regular Expression with an unbound Integer freeze it with the low and high size.
	 * For example, given something like \d+, convert to \d{4,9}.
	 * @return If possible an updated String, if not found then the original string.
	 */
	private String freezeNumeric(final String input) {
		final StringBuilder result = new StringBuilder(input);
		boolean characterClass = false;
		boolean numericStarted = false;
		int idx = 0;

		while (idx < result.length()) {
			char ch = result.charAt(idx);
			if (ch == '\\') {
				ch = result.charAt(++idx);
				if (ch == 'd')
					numericStarted = true;
			}
			else if (ch == '[')
				characterClass = true;
			else if (ch == ']')
				characterClass = false;
			else if (ch == '+') {
				if (numericStarted && !characterClass) {
					break;
				}
			}
			idx++;
		}

		return idx == result.length() ? input :
			result.replace(idx, idx + 1, lengthQualifier(facts.minTrimmedLengthNumeric, facts.maxTrimmedLengthNumeric)).toString();
	}


	private class FiniteMatchResult {
		LogicalTypeFinite logical;
		final double score;
		FiniteMap newOutliers;
		FiniteMap newCardinality;
		long validCount;
		final boolean isMatch;

		boolean matched() {
			return isMatch;
		}

		FiniteMatchResult(final LogicalTypeFinite logical, final double score, final long validCount, final FiniteMap newOutliers, final FiniteMap newCardinality) {
			this.logical = logical;
			this.score = score;
			this.validCount = validCount;
			this.newOutliers = newOutliers;
			this.newCardinality = newCardinality;
			this.isMatch = true;
		}

		FiniteMatchResult() {
			score = Double.MIN_VALUE;
			this.isMatch = false;
		}
	}

	private LogicalTypeFinite matchFiniteTypes(final FTAType type, final FiniteMap cardinalityUpper) {
		double scoreToBeat;

		LogicalType priorLogical = null;
		// We may have a Semantic Type already identified but see if there is a better Finite Semantic type
		if (facts.getMatchTypeInfo().isSemanticType()) {
			priorLogical = plugins.getRegistered(facts.getMatchTypeInfo().getSemanticType());
			scoreToBeat = facts.confidence;
		}
		else
			scoreToBeat = -1.0;

		FiniteMatchResult bestResult = null;
		double bestScore = scoreToBeat;

		for (final LogicalTypeFinite logical : finiteTypes) {
            if (!logical.acceptsBaseType(type))
                continue;

            // Either we need to be an open set or the cardinality should be reasonable (relative to the size of the set)
			if ((!logical.isClosed() || cardinalityUpper.size() <= logical.getSize() + 2 + logical.getSize()/20)) {
				final FiniteMatchResult result = checkFiniteSet(cardinalityUpper, facts.outliers, logical);

				if (!result.matched() || result.score < bestScore)
					continue;

				// We prefer finite matches to infinite matches only if header or priority is better
				if (bestResult == null && priorLogical != null && result.score <= bestScore &&
						logical.getHeaderConfidence(context.getStreamName()) <= priorLogical.getHeaderConfidence(context.getStreamName()) &&
						logical.getPluginDefinition().priority <= priorLogical.getPluginDefinition().priority)
					continue;

				// Choose the best score
				if (result.score > bestScore ||
						// If bestResult is null then this finite match has matched an incoming score to beat
						bestResult == null ||
						// If two scores the same then prefer the one with the higher header confidence
						logical.getHeaderConfidence(context.getStreamName()) > bestResult.logical.getHeaderConfidence(context.getStreamName()) ||
						// If two scores the same then prefer the logical with the highest priority
						(logical.getHeaderConfidence(context.getStreamName()) == bestResult.logical.getHeaderConfidence(context.getStreamName()) &&
						logical.getPriority() < bestResult.logical.getPriority())) {
					bestResult = result;
					bestScore = result.score;
				}
			}
		}

		if (bestResult == null)
			return null;

		facts.outliers = bestResult.newOutliers;
		facts.cardinality = bestResult.newCardinality;
		facts.matchCount = bestResult.validCount;
		facts.setMatchTypeInfo(new TypeInfo(bestResult.logical.getRegExp(), bestResult.logical.getBaseType(), bestResult.logical.getSemanticType(), facts.getMatchTypeInfo()));

		ctxdebug("Type determination", "new matchTypeInfo - {}", facts.getMatchTypeInfo());

		return bestResult.logical;
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

	private final static int EARLY_LONG_YYYYMMDD = 19000101;
	private final static int LATE_LONG_YYYYMMDD = 20510101;

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

		final FiniteMap cardinalityUpper = new FiniteMap(facts.cardinality.getMaxCapacity());
		final FTAType currentType = facts.getMatchTypeInfo().getBaseType();

		if (FTAType.LONG.equals(currentType))
			finalizeLong(realSamples);
		else if (FTAType.BOOLEAN.equals(currentType))
			finalizeBoolean(realSamples);
		else if (FTAType.DOUBLE.equals(currentType) && !facts.getMatchTypeInfo().isSemanticType())
			finalizeDouble(realSamples);
		else if (FTAType.STRING.equals(currentType))
			finalizeString(realSamples, cardinalityUpper);

		// Check Date/Time types for a Semantic Type
		// NOTE: finalizeLong() above may have switched a long to a date - hence this is not an else!
		if (facts.getMatchTypeInfo().getBaseType().isDateOrTimeType() && !facts.getMatchTypeInfo().isSemanticType()) {
			for (final LogicalTypeInfinite logical : infiniteTypes) {
				if (logical.acceptsBaseType(facts.getMatchTypeInfo().getBaseType()) && logical.analyzeSet(context, facts.matchCount, realSamples, facts.getMatchTypeInfo().getRegExp(), facts.calculateFacts(), facts.cardinality, facts.outliers, tokenStreams, analysisConfig).isValid()) {
					facts.getMatchTypeInfo().setSemanticType(logical.getSemanticType());
					ctxdebug("Type determination", "infinite type, matchTypeInfo - {}", facts.getMatchTypeInfo());
				}
			}
		}

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
					updated = checkRegExpTypes();

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
				if (logical.getHeaderConfidence(getContext().getStreamName()) >= 90) {
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
								newResult.getFacts().sampleCount += getFacts().outliers.values().stream().mapToLong(l-> l).sum();;
							}
							newResult.getFacts().sampleCount += worstEntry.getValue();
							newResult.getFacts().confidence -= 0.05;
							newResult.getFacts().getMatchTypeInfo().setBaseType(FTAType.STRING);
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
					for (final Map.Entry<String, Long> entry : outliers.entrySet())
						newResult.getFacts().outliers.mergeIfSpace(entry.getKey(), entry.getValue(), Long::sum);
					newResult.getFacts().sampleCount += outliers.values().stream().mapToLong(l-> l).sum();
					for (final Map.Entry<String, Long> entry : getFacts().invalid.entrySet())
						newResult.getFacts().invalid.mergeIfSpace(entry.getKey(), entry.getValue(), Long::sum);
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

		// If we are in SIMPLE mode (i.e. not Bulk) and we have not detected a Semantic Type - try replaying accumulated set in Bulk mode,
		// this has the potential to pick up entries where the first <n> (by default 20 are misleading).
		if (FTAType.STRING.equals(facts.getMatchTypeInfo().getBaseType()) && !facts.getMatchTypeInfo().isSemanticType() && analysisConfig.getTrainingMode() == AnalysisConfig.TrainingMode.SIMPLE && pluginThreshold != 100) {
			final TextAnalysisResult bulkResult = reAnalyze(facts.synthesizeBulk());
			if (bulkResult.isSemanticType() || bulkResult.getType() != facts.getMatchTypeInfo().getBaseType()) {
				if (!getFacts().outliers.isEmpty()) {
					bulkResult.getFacts().invalid.putAll(getFacts().outliers);
					bulkResult.getFacts().sampleCount += getFacts().outliers.values().stream().mapToLong(l-> l).sum();;
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
		return input != null && input.trim().length() != 0;
	}

	private boolean checkRegExpTypes() {
		final long realSamples = facts.sampleCount - (facts.nullCount + facts.blankCount);

		for (final LogicalTypeRegExp logical : regExpTypes) {
			// Check to see if either
			// the Regular Expression we have matches the Semantic types, or
			// the Regular Expression for the Semantic types matches all the data we have observed
			if (logical.acceptsBaseType(FTAType.STRING)) {
				for (final PluginMatchEntry entry : logical.getMatchEntries()) {
					long newMatchCount = facts.matchCount;
					final String re = entry.getRegExpReturned();
					if (((newMatchCount = tokenStreams.matches(re, logical.getThreshold())) != 0)) {
						// Build the new Cardinality and Invalid maps - based on the RE
						final FiniteMap newCardinality = new FiniteMap(facts.cardinality.getMaxCapacity());
						final FiniteMap newInvalids = new FiniteMap(facts.outliers.getMaxCapacity());
						for (final Map.Entry<String, Long> current : facts.cardinality.entrySet()) {
							if (current.getKey().trim().matches(re))
								newCardinality.put(current.getKey(), current.getValue());
							else
								newInvalids.put(current.getKey(), current.getValue());
						}
						for (final Map.Entry<String, Long> current : facts.outliers.entrySet()) {
							if (current.getKey().trim().matches(re))
								newCardinality.put(current.getKey(), current.getValue());
							else
								newInvalids.put(current.getKey(), current.getValue());
						}

						// Based on the new Cardinality/Outliers do we think this is a match?
						if (logical.analyzeSet(context, facts.matchCount, realSamples, facts.getMatchTypeInfo().getRegExp(), facts.calculateFacts(), newCardinality, newInvalids, tokenStreams, analysisConfig).isValid()) {
							logical.setMatchEntry(entry);
							facts.setMatchTypeInfo(new TypeInfo(logical.getRegExp(), logical.getBaseType(), logical.getSemanticType(), facts.getMatchTypeInfo()));
							facts.matchCount = newMatchCount;
							facts.cardinality = newCardinality;
							facts.invalid = newInvalids;
							facts.outliers.clear();
							ctxdebug("Type determination", "updated to Regular Expression Semantic type {}", facts.getMatchTypeInfo());
							facts.confidence = logical.getConfidence(facts.matchCount, realSamples, context);
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	/*
	 * Synthesize the topK/bottomK by running the cardinality set.
	 */
	private void generateTopBottom() {
		for (final String s : facts.cardinality.keySet())
			try {
				trackDateTime(s, facts.getMatchTypeInfo(), true, 1);
			}
			catch (DateTimeException e) {
				// Swallow - any we lost are no good so will not be in the top/bottom set!
			}
	}

	private boolean plausibleYear(final long realSamples) {
		return (facts.getMinLongNonZero() > DateTimeParser.RECENT_EARLY_LONG_YYYY && facts.getMaxLong() <= DateTimeParser.LATE_LONG_YYYY &&
				realSamples >= reflectionSamples && facts.cardinality.size() > 10) ||
				(facts.getMinLongNonZero() >= DateTimeParser.EARLY_LONG_YYYY && facts.getMaxLong() <= DateTimeParser.LATE_LONG_YYYY &&
				(keywords.match(context.getStreamName(), "YEAR") >= 90 ||
					keywords.match(context.getStreamName(), "DATE") >= 90 ||
					keywords.match(context.getStreamName(), "PERIOD") >= 90));
	}

	private void switchToDate(final TypeInfo newTypeInfo, final LocalDate newMin, final LocalDate newMax) {
		facts.setMatchTypeInfo(newTypeInfo);
		facts.minLocalDate = newMin;
		facts.maxLocalDate = newMax;

		killInvalidDates();

		// If we are collecting statistics - we need to generate the topK and bottomK
		if (analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS))
			generateTopBottom();
	}

	boolean isReallyDate(final long realSamples) {
		if (facts.groupingSeparators != 0 || facts.getMinLongNonZero() == Long.MAX_VALUE)
			return false;

		if (facts.getMinLongNonZero() > EARLY_LONG_YYYYMMDD && facts.getMaxLong() < LATE_LONG_YYYYMMDD &&
				DateTimeParser.plausibleDateLong(facts.getMinLongNonZero(), 4) && DateTimeParser.plausibleDateLong(facts.getMaxLong(), 4) &&
				((realSamples >= reflectionSamples && facts.cardinality.size() > 10) || keywords.match(context.getStreamName(), "DATE") >= 90)) {
			// Sometimes a Long is not a Long but it is really a date (yyyyMMdd)
			final TypeInfo newTypeInfo = new TypeInfo(null, "\\d{8}", FTAType.LOCALDATE, "yyyyMMdd", false, "yyyyMMdd");
			final DateTimeFormatter dtf = dateTimeParser.ofPattern(newTypeInfo.format);
			switchToDate(newTypeInfo, LocalDate.parse(String.valueOf(facts.getMinLongNonZero()), dtf), LocalDate.parse(String.valueOf(facts.getMaxLong()), dtf));
			return true;
		}

		if (facts.getMinLongNonZero() > EARLY_LONG_YYYYMMDD/100 && facts.getMaxLong() < LATE_LONG_YYYYMMDD/100 &&
				DateTimeParser.plausibleDateLong(facts.getMinLongNonZero() * 100 + 1, 4) && DateTimeParser.plausibleDateLong(facts.getMaxLong() * 100 + 1, 4) &&
				((realSamples >= reflectionSamples && facts.cardinality.size() > 10) || keywords.match(context.getStreamName(), "PERIOD") >= 90)) {
			// Sometimes a Long is not a Long but it is really a date (yyyyMM)
			final TypeInfo newTypeInfo = new TypeInfo(null, "\\d{6}", FTAType.LOCALDATE, "yyyyMM", false, "yyyyMM");
			final DateTimeFormatter dtf = dateTimeParser.ofPattern(newTypeInfo.format);
			switchToDate(newTypeInfo, LocalDate.parse(String.valueOf(facts.getMinLongNonZero()), dtf), LocalDate.parse(String.valueOf(facts.getMaxLong()), dtf));
			return true;
		}

		if (facts.groupingSeparators == 0 && facts.getMinLongNonZero() != Long.MAX_VALUE && plausibleYear(realSamples)) {
			// Sometimes a Long is not a Long but it is really a date (yyyy)
			final TypeInfo newTypeInfo = new TypeInfo(null, "\\d{4}", FTAType.LOCALDATE, "yyyy", false, "yyyy");
			switchToDate(newTypeInfo, LocalDate.of((int)facts.getMinLongNonZero(), 1, 1), LocalDate.of((int)facts.getMaxLong(), 1, 1));
			return true;
		}

		return false;
	}

	// Called to finalize a LONG type determination when NOT a Semantic type
	void finalizeLong(final long realSamples) {
		if (KnownTypes.ID.ID_LONG == facts.getMatchTypeInfo().id && facts.getMatchTypeInfo().typeModifier == null && facts.getMinLong() < 0) {
			facts.setMatchTypeInfo(knownTypes.negation(facts.getMatchTypeInfo().getRegExp()));
			ctxdebug("Type determination", "now with sign {}", facts.getMatchTypeInfo());
		}

		if (isReallyDate(realSamples))
			return;

		if (facts.cardinality.size() == 2 && facts.getMinLong() == 0 && facts.getMaxLong() == 1) {
			// boolean by any other name
			facts.minBoolean = "0";
			facts.maxBoolean = "1";
			facts.setMatchTypeInfo(knownTypes.getByID(KnownTypes.ID.ID_BOOLEAN_ONE_ZERO));
			return;
		}

		if (!facts.getMatchTypeInfo().isSemanticType() && facts.groupingSeparators != 0 && !facts.getMatchTypeInfo().hasGrouping()) {
			facts.setMatchTypeInfo(knownTypes.grouping(facts.getMatchTypeInfo().getRegExp()));
			ctxdebug("Type determination", "now with grouping {}", facts.getMatchTypeInfo());
		}

		if (!facts.getMatchTypeInfo().isSemanticType()) {
			// Create a new TypeInfo - we don't want to change a predefined one!
			facts.setMatchTypeInfo(new TypeInfo(facts.getMatchTypeInfo()));
			facts.getMatchTypeInfo().setRegExp(freezeNumeric(facts.getMatchTypeInfo().getRegExp()));
		}

		// We may have a Semantic Type already identified but see if there is a better Finite Semantic type
		final LogicalTypeFinite logicalFinite = matchFiniteTypes(FTAType.LONG, facts.cardinality);
		if (logicalFinite != null)
			facts.confidence = logicalFinite.getConfidence(facts.matchCount, realSamples, context);

		if (!facts.getMatchTypeInfo().isSemanticType())
			for (final LogicalTypeRegExp logical : regExpTypes) {
				if (logical.acceptsBaseType(FTAType.LONG) &&
						logical.isMatch(facts.getMatchTypeInfo().getRegExp()) &&
						logical.analyzeSet(context, facts.matchCount, realSamples, facts.getMatchTypeInfo().getRegExp(), facts.calculateFacts(), facts.cardinality, facts.outliers, tokenStreams, analysisConfig).isValid()) {
					facts.setMatchTypeInfo(new TypeInfo(logical.getRegExp(), logical.getBaseType(), logical.getSemanticType(), facts.getMatchTypeInfo()));
					facts.confidence = logical.getConfidence(facts.matchCount, realSamples, context);
					ctxdebug("Type determination", "was LONG, matchTypeInfo - {}", facts.getMatchTypeInfo());
					break;
				}
			}

		// Do we want to back out to a DOUBLE?  Only do this if we have seen a reasonable number of samples and have
		// not blown out the maximum cardinality.
		if (!facts.getMatchTypeInfo().isSemanticType() && realSamples >= analysisConfig.getDetectWindow() &&
				facts.getCardinalityOverflow() == null &&
				(facts.confidence < analysisConfig.getThreshold()/100.0 ||
						(analysisConfig.isEnabled(TextAnalyzer.Feature.NUMERIC_WIDENING) && !facts.outliers.isEmpty() && (new OutlierAnalysis(facts.outliers, facts.getMatchTypeInfo())).doubles == facts.outliers.size()))) {
			// We thought it was an integer field, but on reflection it does not feel like it
			conditionalBackoutToPattern(realSamples, facts.getMatchTypeInfo());
			facts.confidence = (double) facts.matchCount / realSamples;
		}

		// If it is a Semantic type then the outliers are invalid, if it is not a Semantic type then it is garbage and so is also invalid
		if (!facts.outliers.isEmpty()) {
			facts.invalid.putAll(facts.outliers);
			facts.outliers.clear();
		}
	}

	protected void killInvalidDates() {
		final Iterator<Entry<String, Long>> it = facts.cardinality.entrySet().iterator();

		while (it.hasNext()) {
			final Entry<String, Long> entry = it.next();
			boolean kill = false;
			try {
				trackDateTime(entry.getKey().trim(), facts.getMatchTypeInfo(), false, 1);
			}
			catch (DateTimeException e) {
				kill = true;
			}

			if (kill) {
				facts.invalid.put(entry.getKey(), entry.getValue());
				facts.matchCount -= entry.getValue();
				it.remove();
			}
		 }
	}

	private void finalizeBoolean(final long realSamples) {
		if ((facts.cardinality.size() == 1 && facts.getMatchTypeInfo().id == KnownTypes.ID.ID_BOOLEAN_Y_N)
				|| (facts.confidence < .98 && facts.outliers.size() >= 2)) {
			backoutToString(realSamples);
			facts.confidence = (double) facts.matchCount / realSamples;
		}
	}

	// Called to finalize a DOUBLE type determination when NOT a Semantic type
	private void finalizeDouble(final long realSamples) {
		if (facts.minDouble < 0.0) {
			facts.setMatchTypeInfo(knownTypes.negation(facts.getMatchTypeInfo().getRegExp()));
			ctxdebug("Type determination", "now with sign {}", facts.getMatchTypeInfo());
		}

		if (facts.groupingSeparators != 0 && !facts.getMatchTypeInfo().hasGrouping()) {
			facts.setMatchTypeInfo(knownTypes.grouping(facts.getMatchTypeInfo().getRegExp()));
			ctxdebug("Type determination", "now with grouping {}", facts.getMatchTypeInfo());
		}

		for (final LogicalTypeRegExp logical : regExpTypes) {
			if (logical.acceptsBaseType(FTAType.DOUBLE) &&
					logical.isMatch(facts.getMatchTypeInfo().getRegExp()) &&
					logical.analyzeSet(context, facts.matchCount, realSamples, facts.getMatchTypeInfo().getRegExp(), facts.calculateFacts(), facts.cardinality, facts.outliers, tokenStreams, analysisConfig).isValid()) {
				facts.setMatchTypeInfo(new TypeInfo(logical.getRegExp(), logical.getBaseType(), logical.getSemanticType(), facts.getMatchTypeInfo()));
				facts.confidence = logical.getConfidence(facts.matchCount, realSamples, context);
				break;
			}
		}

		if (!facts.getMatchTypeInfo().isSemanticType() && realSamples >= analysisConfig.getDetectWindow() &&
				facts.outliers.size() > (realSamples >= 100 ? 2 : 1) &&
				(facts.confidence < analysisConfig.getThreshold()/100.0 ||
						(analysisConfig.isEnabled(TextAnalyzer.Feature.NUMERIC_WIDENING) && !facts.outliers.isEmpty() && (new OutlierAnalysis(facts.outliers, facts.getMatchTypeInfo())).doubles == facts.outliers.size()))) {
			// We thought it was an double field, but on reflection it does not feel like it
			conditionalBackoutToPattern(realSamples, facts.getMatchTypeInfo());
			facts.confidence = (double) facts.matchCount / realSamples;
		}

		// All outliers are actually invalid
		if (!facts.outliers.isEmpty()) {
			facts.invalid.putAll(facts.outliers);
			facts.outliers.clear();
		}
	}

	private void finalizeString(final long realSamples, final FiniteMap cardinalityUpper) {
		// Build Cardinality map ignoring case (and white space)
		for (final Map.Entry<String, Long> entry : facts.cardinality.entrySet()) {
			final String key = entry.getKey().toUpperCase(locale).trim();
			cardinalityUpper.merge(key, entry.getValue(), Long::sum);
		}
		// Sort the results so that we consider the most frequent first (we will hopefully fail faster)
		cardinalityUpper.sortByValue();

		// We may have a Semantic Type already identified but see if there is a better Finite Semantic type
		final LogicalTypeFinite logical = matchFiniteTypes(FTAType.STRING, cardinalityUpper);
		if (logical != null)
			facts.confidence = logical.getConfidence(facts.matchCount, realSamples, context);

		// Fixup any likely enums
		if (!facts.getMatchTypeInfo().isSemanticType() && cardinalityUpper.size() < MAX_ENUM_SIZE && !facts.outliers.isEmpty() && facts.outliers.size() < 10) {
			boolean updated = false;

			final Set<String> killSet = new HashSet<>();

			// Sort the outliers so that we consider the most frequent first
			facts.outliers.sortByValue();

			// Iterate through the outliers adding them to the core cardinality set if we think they are reasonable.
			for (final Map.Entry<String, Long> entry : facts.outliers.entrySet()) {
				final String key = entry.getKey();
				final String keyUpper = key.toUpperCase(locale).trim();
				String validChars = " _-";
				boolean skip = false;

				// We are wary of outliers that only have one instance, do an extra check that the characters in the
				// outlier exist in the real set.
				if (entry.getValue() == 1) {
					boolean onlyAlphas = true;
					boolean onlyNumeric = true;
					// Build the universe of valid characters
					for (final String existing : cardinalityUpper.keySet()) {
						for (int i = 0; i < existing.length(); i++) {
							final char ch = existing.charAt(i);
							if (onlyAlphas && !Character.isAlphabetic(ch))
								onlyAlphas = false;
							if (onlyNumeric && !Character.isDigit(ch))
								onlyNumeric = false;
							if (!Character.isAlphabetic(ch) && !Character.isDigit(ch))
								if (validChars.indexOf(ch) == -1)
									validChars += ch;
						}
					}
					for (int i = 0; i < keyUpper.length(); i++) {
						final char ch = keyUpper.charAt(i);
						if ((onlyAlphas && !Character.isAlphabetic(ch)) || (onlyNumeric && !Character.isDigit(ch)) ||
								(!Character.isAlphabetic(ch) && !Character.isDigit(ch) && validChars.indexOf(ch) == -1)) {
							skip = true;
							break;
						}
					}
				}
				else
					skip = false;

				if (!skip) {
					cardinalityUpper.merge(keyUpper, entry.getValue(), Long::sum);
					killSet.add(key);
					updated = true;
				}
			}

			// If we updated the set then we need to remove the outliers we OK'd and
			// also update the pattern to reflect the looser definition
			if (updated) {
				final FiniteMap remainingOutliers = new FiniteMap(facts.outliers.getMaxCapacity());
				remainingOutliers.putAll(facts.outliers);
				for (final String elt : killSet)
					remainingOutliers.remove(elt);

				// This resets the Cardinality set to include all outliers
				backoutToPattern(realSamples, KnownTypes.PATTERN_ANY_VARIABLE);
				// Fix the outliers
				facts.outliers = remainingOutliers;
				// Fix the cardinality set
				for (final String elt : facts.outliers.keySet())
					facts.cardinality.remove(elt);
				facts.matchCount -= remainingOutliers.values().stream().mapToLong(l-> l).sum();
				facts.confidence = (double) facts.matchCount / realSamples;
			}
		}

		// Need to evaluate if we got the type wrong
		if (!facts.getMatchTypeInfo().isSemanticType() && !facts.outliers.isEmpty() && facts.getMatchTypeInfo().isAlphabetic() && realSamples >= reflectionSamples) {
			conditionalBackoutToPattern(realSamples, facts.getMatchTypeInfo());
			facts.confidence = (double) facts.matchCount / realSamples;

			// Rebuild the cardinalityUpper Map
			cardinalityUpper.clear();
			for (final Map.Entry<String, Long> entry : facts.cardinality.entrySet())
				cardinalityUpper.merge(entry.getKey().toUpperCase(locale).trim(), entry.getValue(), Long::sum);
		}
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
		if (analysisConfig.getTraceOptions() != null && traceConfig == null)
			initializeTrace();

		emptyCache();

		// If we have not already determined the type - we need to force the issue
		if (facts.getMatchTypeInfo() == null)
			determineType();

		final TextAnalyzerWrapper wrapper = new TextAnalyzerWrapper(analysisConfig, context, facts.calculateFacts());

		// We are serializing the analyzer (assume it will not be used again - so persist the samples)
		if (traceConfig != null) {
			traceConfig.persistSamples();
			traceConfig.tag("serialize", facts.sampleCount);
		}

		try {
			return serializationMapper.writeValueAsString(serializationMapper.convertValue(wrapper, JsonNode.class));
		} catch (IOException e) {
			throw new InternalErrorException("Cannot output JSON for the Analysis", e);
		}
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
		TextAnalyzer ret = null;

		try {
			final TextAnalyzerWrapper wrapper = serializationMapper.readValue(serialized, TextAnalyzerWrapper.class);
			ret = new TextAnalyzer(wrapper.analyzerContext);
			ret.setConfig(wrapper.analysisConfig);

			ret.facts = wrapper.facts;
			ret.facts.setConfig(wrapper.analysisConfig);
			ret.initializeTrace();
			ret.initialize();
			ret.facts.hydrate();

			if (ret.traceConfig != null)
				ret.traceConfig.tag("deserialize", ret.facts.sampleCount);

			return ret;
		} catch (JsonProcessingException e) {
			throw new FTAMergeException("Issue deserializing supplied JSON.", e);
		}
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
		first.emptyCache();
		second.emptyCache();
		final TextAnalyzer ret = new TextAnalyzer(first.context);

		// We are merging two analyzers (assume they will not be used again - so persist the samples)
		if (first.traceConfig != null) {
			first.traceConfig.persistSamples();
			first.traceConfig.tag("merge.left", first.getFacts().getSampleCount());
		}
		if (second.traceConfig != null) {
			second.traceConfig.persistSamples();
			second.traceConfig.tag("merge.right", first.getFacts().getSampleCount());
		}

		if (!first.analysisConfig.equals(second.analysisConfig))
			throw new FTAMergeException("The AnalysisConfig for both TextAnalyzers must be identical.");

		// If we have not already determined the type - we need to force the issue
		if (first.facts.getMatchTypeInfo() == null)
			first.determineType();
		if (second.facts.getMatchTypeInfo() == null)
			second.determineType();

		ret.setConfig(first.analysisConfig);

		// Train using all the non-null/non-blank elements
		final Map<String, Long>merged = new HashMap<>();

		// Prime the merged set with the first set (real, outliers, and invalid which are non-overlapping)
		final Facts firstFacts = first.facts.calculateFacts();
		merged.putAll(firstFacts.cardinality);
		merged.putAll(firstFacts.outliers);
		merged.putAll(firstFacts.invalid);
		// Preserve the top and bottom values - even if they were not captured in the cardinality set
		if (firstFacts.cardinality.size() >= first.getMaxCardinality()) {
			addToMap(merged, firstFacts.topK, first);
			addToMap(merged, firstFacts.bottomK, first);
		}

		// Merge in the second set
		final Facts secondFacts = second.facts.calculateFacts();
		for (final Map.Entry<String, Long>entry : secondFacts.cardinality.entrySet()) {
			final Long seen = merged.get(entry.getKey());
			if (seen == null) {
				merged.put(entry.getKey(), entry.getValue());
			}
			else
				merged.put(entry.getKey(), seen + entry.getValue());
		}
		for (final Map.Entry<String, Long>entry : secondFacts.outliers.entrySet()) {
			final Long seen = merged.get(entry.getKey());
			if (seen == null) {
				merged.put(entry.getKey(), entry.getValue());
			}
			else
				merged.put(entry.getKey(), seen + entry.getValue());
		}
		for (final Map.Entry<String, Long>entry : secondFacts.invalid.entrySet()) {
			final Long seen = merged.get(entry.getKey());
			if (seen == null) {
				merged.put(entry.getKey(), entry.getValue());
			}
			else
				merged.put(entry.getKey(), seen + entry.getValue());
		}
		// Preserve the top and bottom values - even if they were not captured in the cardinality set
		if (secondFacts.cardinality.size() >= second.getMaxCardinality()) {
			addToMap(merged, secondFacts.topK, second);
			addToMap(merged, secondFacts.bottomK, second);
		}
		ret.trainBulk(merged);

		ret.facts.nullCount = firstFacts.nullCount + secondFacts.nullCount;
		ret.facts.blankCount = firstFacts.blankCount + secondFacts.blankCount;
		ret.facts.sampleCount += ret.facts.nullCount + ret.facts.blankCount;

		if (firstFacts.external.totalCount != -1 && secondFacts.external.totalCount != -1)
			ret.facts.external.totalCount = firstFacts.external.totalCount + secondFacts.external.totalCount;
		if (firstFacts.external.totalNullCount != -1 && secondFacts.external.totalNullCount != -1)
			ret.facts.external.totalNullCount = firstFacts.external.totalNullCount + secondFacts.external.totalNullCount;
		if (firstFacts.external.totalBlankCount != -1 && secondFacts.external.totalBlankCount != -1)
			ret.facts.external.totalBlankCount = firstFacts.external.totalBlankCount + secondFacts.external.totalBlankCount;
		if (firstFacts.external.totalBlankCount != -1 && secondFacts.external.totalBlankCount != -1)
			ret.facts.external.totalBlankCount = firstFacts.external.totalBlankCount + secondFacts.external.totalBlankCount;
		if (firstFacts.external.totalMinLength != -1 && secondFacts.external.totalMinLength != -1)
			ret.facts.external.totalMinLength = Math.min(firstFacts.external.totalMinLength, secondFacts.external.totalMinLength);
		if (firstFacts.external.totalMaxLength != -1 && secondFacts.external.totalMaxLength != -1)
			ret.facts.external.totalMaxLength = Math.max(firstFacts.external.totalMaxLength, secondFacts.external.totalMaxLength);
		if (firstFacts.external.totalMinValue != null && secondFacts.external.totalMinValue != null) {
			final CommonComparator<?> comparator = new CommonComparator<>(firstFacts.getStringConverter());
			if (comparator.compare(firstFacts.external.totalMinValue, secondFacts.external.totalMinValue) < 0)
				ret.facts.external.totalMinValue = firstFacts.external.totalMinValue;
			else
				ret.facts.external.totalMinValue = secondFacts.external.totalMinValue;
		}
		if (firstFacts.external.totalMaxValue != null && secondFacts.external.totalMaxValue != null) {
			final CommonComparator<?> comparator = new CommonComparator<>(firstFacts.getStringConverter());
			if (comparator.compare(firstFacts.external.totalMaxValue, secondFacts.external.totalMaxValue) > 0)
				ret.facts.external.totalMaxValue = firstFacts.external.totalMaxValue;
			else
				ret.facts.external.totalMaxValue = secondFacts.external.totalMaxValue;
		}
		// Unfortunately nothing we can do for totalMean/totalStandardDeviation when we are merging
		// as we do not have the requisite data.

		// Set the min/maxRawLength just in case a blank field is the longest/shortest
		ret.facts.minRawLength = Math.min(first.facts.minRawLength, second.facts.minRawLength);
		ret.facts.maxRawLength = Math.max(first.facts.maxRawLength, second.facts.maxRawLength);

		// Lengths are true representations - so just overwrite with truth
		System.arraycopy(firstFacts.lengths, 0, ret.facts.lengths, 0, firstFacts.lengths.length);
		for (int i = 0; i < ret.facts.lengths.length; i++)
			ret.facts.lengths[i] += secondFacts.lengths[i];

		// So if both sets are unique in their own right and the sets are non-overlapping then the merged set is unique
		if (firstFacts.getMatchTypeInfo() != null && nonOverlappingRegions(firstFacts, secondFacts, ret.analysisConfig)) {
			if (firstFacts.uniqueness != null && firstFacts.uniqueness == 1.0 && secondFacts.uniqueness != null && secondFacts.uniqueness == 1.0 )
				ret.facts.uniqueness = 1.0;
			if (firstFacts.monotonicIncreasing && secondFacts.monotonicIncreasing)
				ret.facts.monotonicIncreasing = true;
			else if (firstFacts.monotonicDecreasing && secondFacts.monotonicDecreasing)
				ret.facts.monotonicDecreasing = true;
		}

		// Check to see if we have exceeded the cardinality on the the first, second, or the merge.
		// If so the samples we have seen do not reflect the entirety of the input so we need to
		// we need to calculate a set of attributes.
		if (ret.facts.cardinality.size() == ret.analysisConfig.getMaxCardinality() ||
				firstFacts.cardinality.size() == first.analysisConfig.getMaxCardinality() ||
				secondFacts.cardinality.size() == second.analysisConfig.getMaxCardinality()) {

			ret.facts.minRawNonBlankLength = Math.min(first.facts.minRawNonBlankLength, second.facts.minRawNonBlankLength);
			ret.facts.maxRawNonBlankLength = Math.max(first.facts.maxRawNonBlankLength, second.facts.maxRawNonBlankLength);

			ret.facts.minTrimmedLength = Math.min(first.facts.minTrimmedLength, second.facts.minTrimmedLength);
			ret.facts.maxTrimmedLength = Math.max(first.facts.maxTrimmedLength, second.facts.maxTrimmedLength);

			ret.facts.minTrimmedLengthNumeric = Math.min(first.facts.minTrimmedLengthNumeric, second.facts.minTrimmedLengthNumeric);
			ret.facts.maxTrimmedLengthNumeric = Math.max(first.facts.maxTrimmedLengthNumeric, second.facts.maxTrimmedLengthNumeric);

			ret.facts.minTrimmedOutlierLength = Math.min(first.facts.minTrimmedOutlierLength, second.facts.minTrimmedOutlierLength);
			ret.facts.maxTrimmedOutlierLength = Math.max(first.facts.maxTrimmedOutlierLength, second.facts.maxTrimmedOutlierLength);

			// In order to calculate the matchCount without having seen all the samples we need both the total
			// number of samples as well as a valid count of the outliers
			// TODO: If we do this we end up with matchCount > samples!
//			if (ret.facts.outliers.size() == ret.analysisConfig.getMaxOutliers() ||
//					firstFacts.outliers.size() == first.analysisConfig.getMaxOutliers() ||
//					secondFacts.outliers.size() == second.analysisConfig.getMaxOutliers())
//				throw new FTAMergeException("Outlier cardinality overflow!!");
//			if (ret.facts.totalCount == -1)
//				throw new FTAMergeException("Total count required on both Analyses to be merged!!");
//
//			long outliers = 0;
//			if (!ret.facts.outliers.isEmpty())
//				for (final long value : ret.facts.outliers.values())
//					outliers += value;
//			ret.facts.matchCount = ret.facts.totalCount - ret.facts.nullCount - ret.facts.blankCount - outliers;

			ret.facts.leadingWhiteSpace = first.facts.leadingWhiteSpace || second.facts.leadingWhiteSpace;
			ret.facts.trailingWhiteSpace = first.facts.trailingWhiteSpace || second.facts.trailingWhiteSpace;
			ret.facts.multiline = first.facts.multiline || second.facts.multiline;

			// When we did the trainBulk above with the new set, max cardinality entries landed in the Cardinality set and potentially
			// some overflow was captured in the cardinalityOverflow - merge in the overflow from the first and second set.
			if (ret.facts.cardinalityOverflow != null || firstFacts.cardinalityOverflow != null || secondFacts.cardinalityOverflow != null) {
				if (firstFacts.cardinalityOverflow != null)
					ret.facts.cardinalityOverflow = ret.facts.cardinalityOverflow == null ? firstFacts.cardinalityOverflow : ret.facts.cardinalityOverflow.merge(firstFacts.cardinalityOverflow);
				if (secondFacts.cardinalityOverflow != null)
					ret.facts.cardinalityOverflow = ret.facts.cardinalityOverflow == null ? secondFacts.cardinalityOverflow : ret.facts.cardinalityOverflow.merge(secondFacts.cardinalityOverflow);
			}

			// If we are numeric then we need to synthesize the mean and variance
			if (ret.facts.getMatchTypeInfo() != null && ret.facts.getMatchTypeInfo().isNumeric()) {
				ret.facts.mean = (first.facts.mean*first.facts.matchCount + second.facts.mean*second.facts.matchCount)/(first.facts.matchCount + second.facts.matchCount);
				if (first.facts.variance == null)
					ret.facts.variance = second.facts.variance;
				else if (second.facts.variance == null)
					ret.facts.variance = first.facts.variance;
				else
					ret.facts.variance = ((first.facts.matchCount - 1)*first.facts.variance + (second.facts.matchCount - 1)*second.facts.variance)/(first.facts.matchCount+second.facts.matchCount-2);
				ret.facts.currentM2 = ret.facts.variance * ret.facts.matchCount;
			}
		}

		return ret;
	}

	/*
	 * Used when merging to preserve uniqueness/monotonicIncreasing/monotonicDecreasing.  We can preserve these facts
	 * iff they have the same FTAType (e.g. Long/Double/Date) and they are comparable using a double as a proxy (see StringConverter).
	 */
	private static boolean nonOverlappingRegions(final Facts firstFacts, final Facts secondFacts, final AnalysisConfig analysisConfig) {
		final TypeInfo firstInfo = firstFacts.getMatchTypeInfo();
		final TypeInfo secondInfo = secondFacts.getMatchTypeInfo();

		if (firstInfo == null || secondInfo == null)
			return false;

		final FTAType firstBaseType = firstInfo.getBaseType();
		final FTAType secondBaseType = secondInfo.getBaseType();

		if (firstBaseType != secondBaseType || !Objects.equals(firstInfo.typeModifier, secondInfo.typeModifier))
			return false;

		if (!firstBaseType.isNumeric() && !firstBaseType.isDateOrTimeType())
			return false;

		if (firstBaseType.isDateOrTimeType() && !firstInfo.format.equals(secondInfo.format))
			return false;

		final String firstMin = firstFacts.getMinValue();
		final String secondMin = secondFacts.getMinValue();
		if (firstMin == null || secondMin == null)
			return false;

		final StringConverter stringConverter = new StringConverter(firstFacts.getMatchTypeInfo().getBaseType(), new TypeFormatter(firstFacts.getMatchTypeInfo(), analysisConfig));
		if (stringConverter.toDouble(firstMin) == stringConverter.toDouble(secondMin))
			return false;

		if (stringConverter.toDouble(firstMin) < stringConverter.toDouble(secondMin))
			return stringConverter.toDouble(firstFacts.getMaxValue()) < stringConverter.toDouble(secondMin);

		return stringConverter.toDouble(secondFacts.getMaxValue()) < stringConverter.toDouble(firstMin);
	}

	protected Facts getFacts() {
		return facts;
	}

	protected void setExternalFacts(final Facts.ExternalFacts externalFacts) {
		facts.external = externalFacts;
	}

	/*
	 * AddToMap is used to add the bottomK and topK to the Map we are going to use to train.  Doing this ensures that
	 * the merged result will at least have the same bottomK/topK as it should have even if these were not captured in the
	 * cardinality set.
	 * The challenge here is that the bottomK/topK values are normalized e.g. if the user supplies 00, 2, 4, 6, ...
	 * then the bottomK will be 0,2,4,6 so 0 will not appear in the cardinality set but will appear in the bottomK set.
	 * Note: this routine is not fast if the extremes are not in the cardinality set, but it is only used when we are merging two analyses.
	 */
	private static void addToMap(final Map<String, Long>merged, final Set<String> extremes, final TextAnalyzer analyzer) {
		if (extremes == null)
			return;

		Map<Object, String> missing = new HashMap<>();
		for (final String e : extremes) {
			if (e == null)
				return;
			// If we already have it in the merged set then we are done
			if (merged.get(e.toUpperCase(analyzer.getConfig().getLocale())) != null)
				continue;
			final Object extreme = analyzer.facts.getStringConverter().getValue(e);
			if (extreme == null)
				continue;
			missing.put(extreme, e);
		}

		// If we failed to find any of the extreme values then do a single pass through the existing set to see if any
		// are present in their normalized form, if so remove them, if not then add them to the set.
		if (missing.size() != 0) {
			for (final String m : merged.keySet()) {
				// Check for equality of value not of format - e.g. "00" will equal "0" once both are converted to Longs
				final Object mValue = analyzer.facts.getStringConverter().getValue(m);
				if (mValue != null && missing.keySet().contains(mValue))
					missing.remove(mValue);
			}
			for (final String missed : missing.values())
				merged.put(missed, 1L);
		}
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
