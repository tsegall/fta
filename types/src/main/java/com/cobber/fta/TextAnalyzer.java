/*
 * Copyright 2017-2022 Tim Segall
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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.core.RegExpSplitter;
import com.cobber.fta.core.TraceException;
import com.cobber.fta.core.Utils;
import com.cobber.fta.dates.DateTimeParser;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;
import com.cobber.fta.dates.DateTimeParserConfig;
import com.cobber.fta.dates.DateTimeParserResult;
import com.cobber.fta.dates.LocaleInfo;
import com.cobber.fta.token.Token;
import com.cobber.fta.token.TokenStream;
import com.cobber.fta.token.TokenStreams;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
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

	private char localeDecimalSeparator;
	private char localeGroupingSeparator;
	private char localeMinusSign;
	private NumberFormat longFormatter;
	private NumberFormat doubleFormatter;
	private char negativePrefix;
	private boolean hasNegativePrefix;
	private char negativeSuffix;
	private boolean hasNegativeSuffix;
	private final FiniteMap outliersSmashed = new FiniteMap();
	private List<String> raw; // 0245-11-98
	// 0: d{4}-d{2}-d{2} 1: d{+}-d{+}-d{+} 2: d{+}-d{+}-d{+}
	// 0: d{4} 1: d{+} 2: [-]d{+}
	// input "hello world" 0: a{5} a{5} 1: a{+} a{+} 2: a{+}

	private final TokenStreams tokenStreams = new TokenStreams(AnalysisConfig.MAX_SHAPES_DEFAULT);

	private final Random random = new Random(271828);

	private Trace traceConfig;

	private int internalErrors;

	private final static String insufficient = "Insufficient digits in input (";

	private Collator collator;

	private String localizedYes;
	private String localizedNo;

	private boolean cacheCheck;

	private Correlation correlation;

	/** Enumeration that defines all on/off features for parsers. */
	public enum Feature {
		/** Feature that determines whether to collect statistics or not. Feature is enabled by default. */
		COLLECT_STATISTICS,
		/** Feature that indicates whether to enable the built-in Semantic Types. Feature is enabled by default. */
		DEFAULT_SEMANTIC_TYPES,
		/** Indicate whether we should track distributions (Quantiles/Histograms). Feature is enabled by default. */
		DISTRIBUTIONS,
		/** Feature that indicates whether to attempt to detect the stream format (HTML, XML, JSON, BASE64, OTHER). Feature is disabled by default. */
		FORMAT_DETECTION,
		/** Indicate whether we should generate Legacy JSON (compatible with FTA 11.X). */
		LEGACY_JSON,
		/** Indicate whether we should qualify the size of the RegExp. Feature is enabled by default. */
		LENGTH_QUALIFIER,
		/**
		 * Indicate whether we should Month Abbreviations (without punctuation), for instance the locale for Canada
		 * uses 'AUG.' for the month abbreviation, and similarly for the AM/PM string which are defined as A.M and P.M.
		 * Feature is enabled by default.
		 */
		NO_ABBREVIATION_PUNCTUATION,
		/** Indicate whether we should treat "NULL" (and similar) as Null values. Feature is enabled by default. */
		NULL_AS_TEXT,
		/** Feature that if enabled return a double if we see a set of integers followed by some doubles call it a double. Feature is enabled by default. */
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
	private final Keywords keywords = new Keywords();

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
		this(new AnalyzerContext(name, DateResolutionMode.None, null, null));
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
	 * The percentage when we declare success 0 - 100.
	 * Typically this should not be adjusted, if you want to run in Strict mode then set this to 100.
	 * @param threshold The new threshold for detection.
	 */
	public void setThreshold(final int threshold) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot adjust Threshold once training has started");

		if (threshold <= 0 || threshold > 100)
			throw new IllegalArgumentException("Threshold must be between 0 and 100");

		analysisConfig.setThreshold(threshold);
	}

	/**
	 * Get the current detection Threshold.
	 *
	 * @return The current threshold.
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
		facts.keyConfidence = keyConfidence;
	}

	/**
	 * Set the Uniqueness - typically used where there is an external source that has visibility into the entire data set and
	 * 'knows' the uniqueness of the set as a whole.
	 * @param uniqueness The new Uniqueness
	 */
	public void setUniqueness(final double uniqueness) {
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
		facts.totalCount = totalCount;
	}

	/**
	 * Set the count of all null elements in the entire data stream.
	 * Only used when there is an external source that has visibility into the entire data stream.
	 * @param totalNullCount The total number of null elements, as opposed to the number of nulls in the sample set.
	 */
	public void setTotalNullCount(final long totalNullCount) {
		facts.totalNullCount = totalNullCount;
	}

	/**
	 * Set the count of all blank elements in the entire data stream.
	 * Only used when there is an external source that has visibility into the entire data stream.
	 * @param totalBlankCount The total number of blank elements, as opposed to the number of blanks in the sample set.
	 */
	public void setTotalBlankCount(final long totalBlankCount) {
		facts.totalBlankCount = totalBlankCount;
	}

	/**
	 * Set the mean for Numeric types (Long, Double) across the entire data stream.
	 * Only used when there is an external source that has visibility into the entire data stream.
	 * @param totalMean The mean of all elements in the data stream, as opposed to the mean of the sampled set.
	 */
	public void setTotalMean(final Double totalMean) {
		facts.totalMean = totalMean;
	}

	/**
	 * Get the Standard Deviation for Numeric types (Long, Double) across the entire data stream (if known).
	 * Only used when there is an external source that has visibility into the entire data stream.
	 * @param totalStandardDeviation The Standard Deviation of all elements in the data stream, as opposed to the Standard Deviation of the sampled set.
	 */
	public void setTotalStandardDeviation(final Double totalStandardDeviation) {
		facts.totalStandardDeviation = totalStandardDeviation;
	}

	/**
	 * Set the minimum value for Numeric, Boolean and String types across the entire data stream.
	 * Only used when there is an external source that has visibility into the entire data stream.
	 * @param totalMinValue The minimum value of all elements in the data stream, as opposed to the minimum of the sampled set.
	 */
	public void setTotalMinValue(final String totalMinValue) {
		facts.totalMinValue = totalMinValue;
	}

	/**
	 * Set the maximum value for Numeric, Boolean and String across the entire data stream.
	 * Only used when there is an external source that has visibility into the entire data stream.
	 * @param totalMaxValue The manimum value of all elements in the data stream, as opposed to the manimum of the sampled set.
	 */
	public void setTotalMaxValue(final String totalMaxValue) {
		facts.totalMaxValue = totalMaxValue;
	}

	/**
	 * Set the minimum length for Numeric, Boolean and String across the entire data stream.
	 * Only used when there is an external source that has visibility into the entire data stream.
	 * Note: For String and Boolean types this length includes any whitespace.
	 * @param totalMinLength The minimum length of all elements in the data stream, as opposed to the minimum length of the sampled set.
	 */
	public void setTotalMinLength(final int totalMinLength) {
		facts.totalMinLength = totalMinLength;
	}

	/**
	 * Set the maximum length for Numeric, Boolean and String across the entire data stream.
	 * Only used when there is an external source that has visibility into the entire data stream.
	 * Note: For String and Boolean types this length includes any whitespace.
	 * @param totalMaxLength The manimum length of all elements in the data stream, as opposed to the manimum length of the sampled set.
	 */
	public void setTotalMaxLength(final int totalMaxLength) {
		facts.totalMaxLength = totalMaxLength;
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
	 * @param histogramBins the number of bins to use for the underlying approximation, note larger values require more memory!
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

	protected String getRegExp(final KnownTypes.ID id) {
		return knownTypes.getRegExp(id);
	}

	// Track basic facts for the field - called for all input
	private void trackLengthAndShape(final String input, final String trimmed, final long count) {
		// We always want to track basic facts for the field
		final int length = input.length();

		if (length != 0 && length < facts.minRawLength)
			facts.minRawLength = length;
		if (length > facts.maxRawLength)
			facts.maxRawLength = length;

		if (trimmed.length() != 0) {
			if (length != 0 && length < facts.minRawNonBlankLength)
				facts.minRawNonBlankLength = length;
			if (length > facts.maxRawNonBlankLength)
				facts.maxRawNonBlankLength = length;

			tokenStreams.track(trimmed, count);
		}
	}

	private boolean trackLong(final String trimmed, final TypeInfo typeInfo, final boolean register, final long count) {
		// Track String facts - just in case we end up backing out.
		if (facts.minString == null || facts.minString.compareTo(trimmed) > 0)
			facts.minString = trimmed;

		if (facts.maxString == null || facts.maxString.compareTo(trimmed) < 0)
			facts.maxString = trimmed;

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
			if (trimmed.indexOf(localeGroupingSeparator) != -1) {
				facts.groupingSeparators++;
				if (!facts.getMatchTypeInfo().isSemanticType() && !facts.getMatchTypeInfo().hasGrouping()) {
					facts.setMatchTypeInfo(knownTypes.grouping(facts.getMatchTypeInfo().regexp));
					debug("Type determination - now with grouping {}", facts.getMatchTypeInfo());
				}
			}
			digits = trimmed.length();
			if (hasNegativePrefix && (firstCh == '-' || firstCh == '+' || firstCh == negativePrefix))
				digits--;
			if (l < 0 && hasNegativeSuffix)
				digits--;
		}

		if (typeInfo.isSemanticType()) {
			// If it is a registered Infinite Semantic Type then validate it
			final LogicalType logical = plugins.getRegistered(typeInfo.semanticType);
			if (logical.acceptsBaseType(FTAType.LONG) && !logical.isValid(trimmed, true))
				return false;
		}


		if (register) {
			if (firstCh == '0' && digits != 1)
				facts.leadingZeroCount++;

			if (digits < facts.minTrimmedLengthNumeric)
				facts.minTrimmedLengthNumeric = digits;
			if (digits > facts.maxTrimmedLengthNumeric)
				facts.maxTrimmedLengthNumeric = digits;

			if (l != 0 && l < facts.minLongNonZero)
				facts.minLongNonZero = l;

			if (l < facts.minLong)
				facts.minLong = l;
			else
				facts.monotonicDecreasing = false;

			if (l > facts.maxLong)
				facts.maxLong = l;
			else
				facts.monotonicIncreasing = false;

			if (analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS)) {
				// Avoids any work if the existing mean is the same as the input
				if (l != facts.mean) {
					// Calculate the mean & standard deviation using Welford's algorithm (weighted)
					double oldMean = facts.mean;
					long newCount = facts.matchCount + count;
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
				(localizedYes != null && localizedYes.equals(trimmedLower));
		final boolean isFalse = !isTrue && ("false".equals(trimmedLower) || "no".equals(trimmedLower) || "n".equals(trimmedLower)) ||
				(localizedNo != null && localizedNo.equals(trimmedLower));

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

	private boolean trackString(final String rawInput, final String trimmed, final TypeInfo typeInfo, final boolean register) {
		if (register && analysisConfig.getDebug() >= 2 && rawInput.length() > 0 && rawInput.charAt(0) == '¶' && "¶ xyzzy ¶".equals(rawInput))
			throw new NullPointerException("¶ xyzzy ¶");
		if (!typeInfo.isSemanticType()) {
			for (int i = 0; i < trimmed.length(); i++) {
				if (typeInfo.isAlphabetic() && !Character.isAlphabetic(trimmed.charAt(i)))
					return false;
				if (typeInfo.isAlphanumeric() && !Character.isLetterOrDigit((trimmed.charAt(i))))
					return false;
			}
		}
		else if (typeInfo.isSemanticType()) {
			// If it is a registered Infinite Semantic Type then validate it
			final LogicalType logical = plugins.getRegistered(typeInfo.semanticType);
			if (logical.acceptsBaseType(FTAType.STRING) && !logical.isValid(rawInput, true))
				return false;
		}

		return updateStats(rawInput);
	}

	private boolean updateStats(final String cleaned) {
		final int len = cleaned.trim().length();

		if (facts.getMatchTypeInfo().getMinLength() != -1 && len < facts.getMatchTypeInfo().getMinLength())
			return false;
		if (facts.getMatchTypeInfo().getMaxLength() != -1 && len > facts.getMatchTypeInfo().getMaxLength())
			return false;

		if (facts.minString == null || facts.minString.compareTo(cleaned) > 0)
			facts.minString = cleaned;

		if (facts.maxString == null || facts.maxString.compareTo(cleaned) < 0)
			facts.maxString = cleaned;

		if (len < facts.minTrimmedLength)
			facts.minTrimmedLength = len;
		if (len > facts.maxTrimmedLength)
			facts.maxTrimmedLength = len;

		if (analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS))
			facts.tbString.observe(cleaned);

		return true;
	}

	private boolean trackDouble(final String rawInput, final TypeInfo typeInfo, final boolean register, final long count) {
		final String input = rawInput.trim();
		double d;

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
		} catch (NumberFormatException e) {
			// If we think we are a Non Localized number then no point in using the locale-aware parsing
			if (typeInfo.isNonLocalized())
				return false;
			// Failed to parse using the naive parseDouble, so use the locale-sensitive Numberformat.parse
			final ParsePosition pos = new ParsePosition(0);
			final Number n = doubleFormatter.parse(input, pos);
			if (n == null || input.length() != pos.getIndex())
				return false;
			d = n.doubleValue();
			if (input.indexOf(localeGroupingSeparator) != -1)
				facts.groupingSeparators++;
			// Make sure to track the decimal separator being used for doubles
			if (localeDecimalSeparator != '.' && facts.decimalSeparator != localeDecimalSeparator && input.indexOf(localeDecimalSeparator) != -1)
				facts.decimalSeparator = localeDecimalSeparator;
		}

		// If it is NaN/Infinity then we are all done
		if (Double.isNaN(d) || Double.isInfinite(d))
			return false;

		if (register && analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS)) {
			if (d < facts.minDouble)
				facts.minDouble = d;

			if (d > facts.maxDouble)
				facts.maxDouble = d;

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
		}

		if (typeInfo.isSemanticType()) {
			// If it is a registered Infinite Semantic Type then validate it
			final LogicalType logical = plugins.getRegistered(typeInfo.semanticType);
			if (logical.acceptsBaseType(FTAType.DOUBLE) && !logical.isValid(input, true))
				return false;
		}

		return true;
	}

	/*
	 * Validate (and track) the date/time/datetime input.
	 * This routine is called for every date/time/datetime we see in the input, so performance is critical.
	 */
	private boolean trackDateTime(final String input, final TypeInfo typeInfo, final boolean register) {
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
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/plugins.json"), StandardCharsets.UTF_8))) {
			plugins.registerPluginsInternal(reader, context.getStreamName(), analysisConfig);
		} catch (Exception e) {
			throw new IllegalArgumentException("Internal error: Issues with plugins file: " + e.getMessage(), e);
		}
	}

	private void initialize() throws FTAPluginException, FTAUnsupportedLocaleException {
		mapper.registerModule(new JavaTimeModule());
		mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

		if (LocaleInfo.isSupported(locale) != null)
			throw new FTAUnsupportedLocaleException(LocaleInfo.isSupported(locale));

		collator = Collator.getInstance(locale);
		collator.setStrength(Collator.PRIMARY);

		raw = new ArrayList<>(analysisConfig.getDetectWindow());
		detectWindowEscalations = new ArrayList<>(analysisConfig.getDetectWindow());

		// If enabled, load the default set of plugins for Semantic Type detection
		if (analysisConfig.isEnabled(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES))
			registerDefaultPlugins(analysisConfig);

		for (final LogicalType logical : plugins.getRegisteredLogicalTypes()) {

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

		final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);
		localeDecimalSeparator = formatSymbols.getDecimalSeparator();
		localeGroupingSeparator = formatSymbols.getGroupingSeparator();
		localeMinusSign = formatSymbols.getMinusSign();
		final NumberFormat simple = NumberFormat.getNumberInstance(locale);
		if (simple instanceof DecimalFormat) {
			String signFacts = ((DecimalFormat) simple).getNegativePrefix();
			// Ignore the LEFT_TO_RIGHT_MARK if it exists
			if (!signFacts.isEmpty() && signFacts.charAt(0) == KnownTypes.LEFT_TO_RIGHT_MARK)
				signFacts = signFacts.substring(1);
			if (signFacts.length() > 1)
				throw new FTAUnsupportedLocaleException("No support for locales with multi-character sign prefixes");
			hasNegativePrefix = !signFacts.isEmpty();
			if (hasNegativePrefix)
				negativePrefix = signFacts.charAt(0);
			signFacts = ((DecimalFormat) simple).getNegativeSuffix();
			if (signFacts.length() > 1)
				throw new FTAUnsupportedLocaleException("No support for locales with multi-character sign suffixes");
			hasNegativeSuffix = !signFacts.isEmpty();
			if (hasNegativeSuffix)
				negativeSuffix = signFacts.charAt(0);
		}
		else {
			final String signFacts = String.valueOf(formatSymbols.getMinusSign());
			hasNegativePrefix = true;
			negativePrefix = signFacts.charAt(0);
			hasNegativeSuffix = false;
		}

		knownTypes.initialize(locale);

		keywords.initialize(locale);

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
				.withNoAbbreviationPunctuation(analysisConfig.isEnabled(TextAnalyzer.Feature.NO_ABBREVIATION_PUNCTUATION));

		// If no trace options already set then pick them up from the environment (if set)
		if (analysisConfig.getTraceOptions() == null) {
			final String ftaTrace = System.getenv("FTA_TRACE");
			if (ftaTrace != null && !ftaTrace.isEmpty())
				analysisConfig.setTraceOptions(ftaTrace);
		}

		if (analysisConfig.getTraceOptions() != null)
			traceConfig = new Trace(analysisConfig.getTraceOptions(), context,  analysisConfig);

		// Now that we have initialized these facts cannot change, so set them on the Facts object
		this.facts.setConfig(analysisConfig);

		outliersSmashed.setMaxCapacity(analysisConfig.getMaxOutliers());

		correlation = new Correlation(locale);
		correlation.initialize();

		initialized = true;
	}

	private StringBuilder[]
	determineNumericPattern(final SignStatus signStatus, final int numericDecimalSeparators, final int possibleExponentSeen, final boolean nonLocalizedDouble) {
		StringBuilder[] result = new StringBuilder[2];

		if (signStatus == SignStatus.TRAILING_MINUS) {
			result[0] = result[1] = new StringBuilder(numericDecimalSeparators == 1 ? knownTypes.PATTERN_SIGNED_DOUBLE_TRAILING : knownTypes.PATTERN_SIGNED_LONG_TRAILING);
			return result;
		}

		final boolean numericSigned = signStatus == SignStatus.LOCALE_STANDARD || signStatus == SignStatus.LEADING_SIGN;

		if (numericDecimalSeparators == 1) {
			if (possibleExponentSeen == -1) {
				if (nonLocalizedDouble) {
					result[0] = new StringBuilder(numericSigned ? knownTypes.PATTERN_SIGNED_DOUBLE_NL : knownTypes.PATTERN_DOUBLE_NL);
					result[1] = new StringBuilder(knownTypes.PATTERN_SIGNED_DOUBLE_NL);
				}
				else {
					result[0] = new StringBuilder(numericSigned ? knownTypes.PATTERN_SIGNED_DOUBLE : knownTypes.PATTERN_DOUBLE);
					result[1] = new StringBuilder(knownTypes.PATTERN_SIGNED_DOUBLE);
				}
			}
			else {
				result[0] = new StringBuilder(numericSigned ? knownTypes.PATTERN_SIGNED_DOUBLE_WITH_EXPONENT : knownTypes.PATTERN_DOUBLE_WITH_EXPONENT);
				result[1] = new StringBuilder(knownTypes.PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
			}
		}
		else {
			if (possibleExponentSeen == -1) {
				result[0] = new StringBuilder(numericSigned ? knownTypes.PATTERN_SIGNED_LONG : knownTypes.PATTERN_LONG);
				result[1] = new StringBuilder(knownTypes.PATTERN_SIGNED_LONG);
			}
			else {
				result[0] = new StringBuilder(numericSigned ? knownTypes.PATTERN_SIGNED_DOUBLE_WITH_EXPONENT : knownTypes.PATTERN_DOUBLE_WITH_EXPONENT);
				result[1] = new StringBuilder(knownTypes.PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
			}
		}

		return result;
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
	 * @param observed
	 *            A Map containing the observed items and the corresponding count
	 * @throws FTAPluginException Thrown when a registered plugin has detected an issue
	 * @throws FTAUnsupportedLocaleException Thrown when a requested locale is not supported
	 */
	public void trainBulk(Map<String, Long> observed) throws FTAPluginException, FTAUnsupportedLocaleException {
		// Initialize if we have not already done so
		if (!initialized) {
			initialize();
			trainingStarted = true;
		}

		// Sort so we have the most frequent first
		observed = Utils.sortByValue(observed);
		Observation[] facts = new Observation[observed.size()];
		int i = 0;
		long total = 0;

		if (traceConfig != null)
			traceConfig.recordBulk(observed);

		// Setup the array of observations and calculate the total number of observations
		for (final Map.Entry<String, Long> entry : observed.entrySet()) {
			facts[i++] = new Observation(entry.getKey(), entry.getValue(), 0);
			total += entry.getValue();
		}

		// Each element in the array has the probability that an observation is in this location or an earlier one
		long running = 0;
		for (int f = 0; f < facts.length; f++) {
			running += facts[f].count;
			facts[f].percentage = (double)running/total;
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

		// Now send in the rest of the samples in bulk
		for (final Observation fact : facts) {
			final long remaining = fact.count - fact.used;
			if (remaining != 0)
				trainBulkCore(fact.observed, remaining);
		}
	}

	private void trainBulkCore(final String rawInput, final long count) {
		facts.sampleCount += count;

		if (rawInput == null) {
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
			initialize();
			trainingStarted = true;
		}

		if (traceConfig != null)
			traceConfig.recordSample(rawInput, facts.sampleCount);

		facts.sampleCount++;

		final FTAType matchType = facts.getMatchTypeInfo() != null ? facts.getMatchTypeInfo().getBaseType() : null;

		if (rawInput == null || (analysisConfig.isEnabled(TextAnalyzer.Feature.NULL_AS_TEXT) && keywords.match(rawInput, "NO_DATA", Keywords.MatchStyle.EQUALS) == 100)) {
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

	enum SignStatus {
		NONE,
		LOCALE_STANDARD,
		LEADING_SIGN,
		TRAILING_MINUS
	}

	private boolean trainCore(final String rawInput, final String trimmed, final long count) {

		trackResult(rawInput, trimmed, true, count);

		// If we have determined a type, no need to further train
		if (facts.getMatchTypeInfo() != null && facts.getMatchTypeInfo().getBaseType() != null)
			return true;

		raw.add(rawInput);

		final int length = trimmed.length();

		final StringBuilder l0 = new StringBuilder(length);

		// Walk the string
		SignStatus numericSigned = SignStatus.NONE;
		int numericDecimalSeparators = 0;
		int numericGroupingSeparators = 0;
		boolean nonLocalizedDouble = false;
		boolean couldBeNumeric = true;
		boolean leadingZero = false;
		int possibleExponentSeen = -1;
		int digitsSeen = 0;
		int alphasSeen = 0;
		int[] charCounts = new int[128];
		int[] lastIndex = new int[128];
		int startLooking = 0;
		int stopLooking = length;

		int matchesRequired = 0;
		int matches = 0;
		if (hasNegativePrefix) {
			matchesRequired++;
			if (negativePrefix == trimmed.charAt(0)) {
				matches++;
				startLooking = 1;
			}
		}
		if (hasNegativeSuffix) {
			matchesRequired++;
			if (negativeSuffix == trimmed.charAt(length - 1)) {
				matches++;
				stopLooking = length - 1;
			}
		}
		if (matches == matchesRequired && matches > 0)
			numericSigned = SignStatus.LOCALE_STANDARD;

		int periodOffset = -1;
		int periods = 0;
		for (int i = startLooking; i < stopLooking; i++) {
			final char ch = trimmed.charAt(i);

			// Track counts and last occurrence for simple characters
			if (ch <= 127) {
				charCounts[ch]++;
				lastIndex[ch] = i;
			}

			if ((ch == localeMinusSign || ch == '+') && i == 0)
				numericSigned = SignStatus.LEADING_SIGN;
			else if (!hasNegativeSuffix && numericSigned == SignStatus.NONE && ch == '-' && i == stopLooking - 1) {
				numericSigned = SignStatus.TRAILING_MINUS;
			} else if (Character.isDigit(ch)) {
				l0.append('d');
				if (digitsSeen == 0 && ch == '0')
					leadingZero = true;
				digitsSeen++;
			} else if (ch == localeDecimalSeparator) {
				l0.append('D');
				numericDecimalSeparators++;
				if (numericDecimalSeparators > 1)
					couldBeNumeric = false;
			} else if (leadingZero == false && ch == localeGroupingSeparator && i + 3 < stopLooking) {
				l0.append('G');
				numericGroupingSeparators++;
			} else if (Character.isAlphabetic(ch)) {
				l0.append('a');
				alphasSeen++;
				if (couldBeNumeric && (ch == 'e' || ch == 'E')) {
					if (possibleExponentSeen != -1 || i < 1 || i + 1 >= length)
						couldBeNumeric = false;
					else
						possibleExponentSeen = i;
				}
				else
					couldBeNumeric = false;
			} else {
				l0.append(ch);
				if (ch == '.') {
					periodOffset = i;
					periods++;
				}
				// If the last character was an exponentiation symbol then this better be a sign if it is going to be numeric
				if (possibleExponentSeen != -1 && possibleExponentSeen == i - 1) {
					if (ch != localeMinusSign && ch != '+')
						couldBeNumeric = false;
				}
				else
					couldBeNumeric = false;
			}
		}

		// Handle doubles stored in non-localized for (e.g. latitude is often stored with a '.')
		// This case handles the case where the grouping separator is not a '.'
		if (alphasSeen == 0 && periodOffset != -1 && periods == 1 && numericDecimalSeparators == 0 && numericGroupingSeparators == 0) {
			couldBeNumeric = true;
			for (int i = 0; i < l0.length(); i++) {
				if (l0.charAt(i) == '.') {
					l0.replace(i, i + 1, "D");
					numericDecimalSeparators++;
					nonLocalizedDouble = true;
					break;
				}
			}
		}

		if (couldBeNumeric && possibleExponentSeen != -1) {
			final int exponentLength = stopLooking - possibleExponentSeen - 1;
			if (exponentLength >= 5)
				couldBeNumeric = false;
			else {
				int offset = possibleExponentSeen + 1;
				// parseInt cannot cope with UTF-8 minus sign, which is used in some locales, so just skip sign
				final char ch = trimmed.charAt(possibleExponentSeen + 1);
				if (ch == localeMinusSign || ch == '-' || ch == '+')
					offset++;
				final int exponentSize = Integer.parseInt(trimmed.substring(offset, stopLooking));
				if (exponentSize > 308)
					couldBeNumeric = false;
			}
		}

		// Handle doubles stored in non-localized for (e.g. latitude is often stored with a '.')
		// This case handles the case where the grouping separator is a '.'
		if (couldBeNumeric && numericGroupingSeparators == 1 && numericDecimalSeparators == 0 && localeGroupingSeparator == '.' &&
				(digitsSeen - 1) / 3 > numericGroupingSeparators) {
			for (int i = 0; i < l0.length(); i++) {
				if (l0.charAt(i) == 'G') {
					l0.replace(i, i + 1, "D");
					numericGroupingSeparators--;
					numericDecimalSeparators++;
					nonLocalizedDouble = true;
					break;
				}
			}
		}


		final StringBuilder compressedl0 = new StringBuilder(length);
		if (alphasSeen != 0 && digitsSeen != 0 && alphasSeen + digitsSeen == length) {
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
			String l0withSentinel = l0.toString() + "|";
			// Walk the new level0 to create the new level1
			if (couldBeNumeric && numericGroupingSeparators > 0)
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
				if ((facts.getMatchTypeInfo() == null || logical.acceptsBaseType(facts.getMatchTypeInfo().getBaseType())) && logical.isCandidate(trimmed, compressedl0, charCounts, lastIndex))
					candidateCounts[c]++;
			}
			catch (Exception e) {
				LoggerFactory.getLogger("com.cobber.fta").error("Plugin: {}, issue: {}.", logical.getSemanticType(), e.getMessage());
			}
			c++;
		}

		// Create the level 1 and 2
		if (digitsSeen > 0 && couldBeNumeric && numericDecimalSeparators <= 1) {
			final StringBuilder[] result = determineNumericPattern(numericSigned, numericDecimalSeparators, possibleExponentSeen, nonLocalizedDouble);
			escalation.level[1] = result[0];
			escalation.level[2] = result[1];
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
			final TypeInfo found = knownTypes.getByRegExp(compressedl0.toString());
			if (found != null && found.generalPattern != null) {
				escalation.level[1] = new StringBuilder(found.generalPattern);
				escalation.level[2] = new StringBuilder(collapsed);
			} else {
				escalation.level[1] = new StringBuilder(collapsed);
				escalation.level[2] = new StringBuilder(KnownTypes.PATTERN_ANY_VARIABLE);
			}
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
					newKey = knownTypes.numericPromotion(bestPattern.regexp, secondBestPattern.regexp);
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
					newKey = knownTypes.numericPromotion(newKey != null ? newKey : bestPattern.regexp, thirdBestPattern.regexp);
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

		// Calculate the frequency of every element
		for (final Escalation e : detectWindowEscalations) {
			final int hash = e.hashCode();
			final Integer seen = observedFrequency.get(hash);
			if (seen == null) {
				observedFrequency.put(hash, 1);
				observedSet.put(hash, e);
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

	void debug(final String format, final Object... arguments) {
		if (analysisConfig.getDebug() >= 2)
			LoggerFactory.getLogger("com.cobber.fta").debug(format, arguments);
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
					facts.setMatchTypeInfo(new TypeInfo(null, result.getRegExp(), result.getType(), formatString, false, -1, -1, null, formatString));
				}
				catch (RuntimeException e) {
				    debug("Internal error: {}", e);
				}
			}

			debug("Type determination - initial, matchTypeInfo - {}", facts.getMatchTypeInfo());

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
							if (trackString(sample, sample.trim(),  candidate, false))
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
							if (trackDateTime(sample, candidate, false))
								count++;
							break;
						}
					}

					// If a reasonable number look genuine then we are convinced
					final double currentConfidence = logical.getConfidence(count, raw.size(), context);
					if (currentConfidence > bestConfidence && currentConfidence >= logical.getThreshold()/100.0) {
						facts.setMatchTypeInfo(candidate);
						bestConfidence = currentConfidence;
						debug("Type determination - infinite type, confidence: {}, matchTypeInfo - {}", currentConfidence, facts.getMatchTypeInfo());
					}
				}
				i++;
			}

			// Try a regExp match nice and early - we can always back out
			for (final LogicalTypeRegExp logical : regExpTypes) {
				if (logical.acceptsBaseType(facts.getMatchTypeInfo().getBaseType()) &&
						logical.isMatch(facts.getMatchTypeInfo().regexp)) {
					facts.setMatchTypeInfo(new TypeInfo(logical.getRegExp(), logical.getBaseType(), logical.getSemanticType(), facts.getMatchTypeInfo()));
					debug("Type determination - was '{}', matchTypeInfo - {}", facts.getMatchTypeInfo().getBaseType(), facts.getMatchTypeInfo());
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

	private void addInvalid(final String input, final long count) {
		facts.invalid.mergeIfSpace(input, count, Long::sum);
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

		private boolean isDouble(final String input) {
			try {
				Double.parseDouble(input.trim());
			} catch (NumberFormatException e) {
				final ParsePosition pos = new ParsePosition(0);
				final Number n = doubleFormatter.parse(input, pos);
				if (n == null || input.length() != pos.getIndex())
					return false;
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
				id = analysis.negative ? KnownTypes.ID.ID_SIGNED_DOUBLE_WITH_EXPONENT : KnownTypes.ID.ID_DOUBLE_WITH_EXPONENT;
			else
				id = analysis.negative ? KnownTypes.ID.ID_SIGNED_DOUBLE : KnownTypes.ID.ID_DOUBLE;
			if (current.hasGrouping())
				id = knownTypes.grouping(knownTypes.getByID(id).regexp).id;
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

		for (final String s : facts.cardinality.keySet())
			trackString(s, s.trim(), newTypeInfo, false);

		facts.outliers.clear();
		outliersSmashed.clear();
		debug("Type determination - backing out, matchTypeInfo - {}", facts.getMatchTypeInfo());
	}

	private void backoutToTypeInfo(final long realSamples, final TypeInfo newTypeInfo) {
		facts.matchCount = realSamples;
		facts.setMatchTypeInfo(newTypeInfo);

		// All outliers are now part of the cardinality set and there are now no outliers
		facts.cardinality.putAll(facts.outliers);

		// Need to update stats to reflect any outliers we previously ignored
		if (facts.getMatchTypeInfo().getBaseType().equals(FTAType.STRING)) {
			for (final String s : facts.cardinality.keySet())
				trackString(s, s.trim(), newTypeInfo, false);
		}
		else if (facts.getMatchTypeInfo().getBaseType().equals(FTAType.DOUBLE)) {
			facts.minDouble = facts.minLong;
			facts.maxDouble = facts.maxLong;
			for (final Map.Entry<String, Long> entry : facts.outliers.entrySet())
				trackDouble(entry.getKey(), facts.getMatchTypeInfo(), true, entry.getValue());
		}

		facts.outliers.clear();
		outliersSmashed.clear();
		debug("Type determination - backing out, matchTypeInfo - {}", facts.getMatchTypeInfo());
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

		debug("Type determination - backing out double, matchTypeInfo - {}", facts.getMatchTypeInfo());
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
		for (final Map.Entry<String, Long> entry : longOutliers.entrySet())
			addValid(entry.getKey(), entry.getValue());

		if ((double) facts.matchCount / realSamples > analysisConfig.getThreshold()/100.0) {
			facts.setMatchTypeInfo(knownTypes.getByID(KnownTypes.ID.ID_LONG));
		}
		else if ((double)(facts.matchCount + otherDoubles) / realSamples > analysisConfig.getThreshold()/100.0) {
			facts.setMatchTypeInfo(knownTypes.getByID(KnownTypes.ID.ID_DOUBLE));
			facts.outliers.entrySet().removeAll(doubleOutliers.entrySet());
			for (final Map.Entry<String, Long> entry : doubleOutliers.entrySet())
				addValid(entry.getKey(), entry.getValue());

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
			if (trackString(input, trimmed, facts.getMatchTypeInfo(), true))
				valid = true;
			break;

		case LOCALDATE:
		case LOCALTIME:
		case LOCALDATETIME:
		case OFFSETDATETIME:
		case ZONEDDATETIME:
			try {
				trackDateTime(input, facts.getMatchTypeInfo(), true);
				valid = true;
			}
			catch (DateTimeParseException reale) {
				// The real parse threw an Exception, this does not give us enough facts to usefully determine if there are any
				// improvements to our assumptions we could make to do better, so re-parse and handle our more nuanced exception
				DateTimeParserResult result = DateTimeParserResult.asResult(facts.getMatchTypeInfo().format, context.getDateResolutionMode(), new DateTimeParserConfig(locale));
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

						String newFormatString = null;
						if (ditch != '?') {
							final int offset = facts.getMatchTypeInfo().format.indexOf(ditch);

							// S is a special case (unlike H, H, M, d) and is *NOT* handled by the default DateTimeFormatter.ofPattern
							if (ditch == 'S') {
								// The input is shorter than we expected so set the minimum length to 1 and then update
								final int len = result.timeFieldLengths[FRACTION_INDEX].getPatternLength();
								result.timeFieldLengths[FRACTION_INDEX].setMin(1);
								newFormatString = Utils.replaceAt(facts.getMatchTypeInfo().format, offset, len,
										result.timeFieldLengths[FRACTION_INDEX].getPattern('S'));
							}
							else
								newFormatString = new StringBuffer(facts.getMatchTypeInfo().format).deleteCharAt(offset).toString();

							updated = true;
						}
						else if (e.getMessage().equals("Expecting end of input, extraneous input found, last token (FRACTION)")) {
							final int offset = facts.getMatchTypeInfo().format.indexOf('S');
							final int oldLength = result.timeFieldLengths[FRACTION_INDEX].getPatternLength();
							result.timeFieldLengths[FRACTION_INDEX].set(result.timeFieldLengths[FRACTION_INDEX].getMin(),
									result.timeFieldLengths[FRACTION_INDEX].getMax() + 1);
							newFormatString = Utils.replaceAt(facts.getMatchTypeInfo().format, offset, oldLength,
									result.timeFieldLengths[FRACTION_INDEX].getPattern('S'));
							updated = true;
						}
						else if (e.getMessage().equals("Invalid value for hours: 24 (expected 0-23)")) {
							final int offset = facts.getMatchTypeInfo().format.indexOf('H');
							newFormatString = Utils.replaceAt(facts.getMatchTypeInfo().format, offset, result.timeFieldLengths[HOUR_INDEX].getMin(),
									result.timeFieldLengths[HOUR_INDEX].getMin() == 1 ? "k" : "kk");
							updated = true;
						}

						if (!updated)
							break;

						result = DateTimeParserResult.asResult(newFormatString, context.getDateResolutionMode(), new DateTimeParserConfig(locale));
						facts.setMatchTypeInfo(new TypeInfo(null, result.getRegExp(), facts.getMatchTypeInfo().getBaseType(), newFormatString, false, -1, -1, null, newFormatString));
					}
				} while (!success);

				try {
					trackDateTime(input, facts.getMatchTypeInfo(), true);
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
			facts.trackTrimmedLengthAndWhiteSpace(rawInput, trimmed);
		}
		else {
			addOutlier(input, count);

			// So we are about to blow the outliers - so now would be a good time to validate that the Semantic Type we have declared is actually correct!
			if (!facts.getMatchTypeInfo().isDateType() && facts.outliers.size() == analysisConfig.getMaxOutliers()) {
				if (facts.getMatchTypeInfo().isSemanticType()) {
					// Do we need to back out from any of our Infinite type determinations
					final LogicalType logical = plugins.getRegistered(facts.getMatchTypeInfo().semanticType);
					final PluginAnalysis pluginAnalysis = logical.analyzeSet(context, facts.matchCount, realSamples, facts.getMatchTypeInfo().regexp, facts.calculateFacts(), facts.cardinality, facts.outliers, tokenStreams, analysisConfig);
					if (!pluginAnalysis.isValid())
						backout(logical, realSamples, pluginAnalysis);
				}
				else {

					// Need to evaluate if we got this wrong
					conditionalBackoutToPattern(realSamples, facts.getMatchTypeInfo());
				}
			}
		}

		// So before we blow the cache (either Shapes or Cardinality) we should look for RegExp matches ONCE!
		if (!cacheCheck && (tokenStreams.isFull() || facts.cardinality.size() + 1 == analysisConfig.getMaxCardinality())) {
			checkRegExpTypes();
			cacheCheck = true;
		}
	}

	void backout(final LogicalType logical, final long realSamples, final PluginAnalysis pluginAnalysis) {
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
			if (logical.isValid(upper, true)) {
				validCount += entry.getValue();
				addMatches.put(upper, entry.getValue());
			}
			else {
				missCount += entry.getValue();
				newOutliers.put(entry.getKey(), entry.getValue());
			}
		}

		// Sweep the balance and check they are part of the set
		if ((double) missCount / realSamples > missThreshold)
			return new FiniteMatchResult();

		final Map<String, Long> minusMatches = new HashMap<>();

		final Set<String> ignorable = logical.getIgnorable();

		for (final Map.Entry<String, Long> entry : cardinalityUpper.entrySet()) {
			if (ignorable != null && ignorable.contains(entry.getKey())) {
				realSamples -= entry.getValue();
				minusMatches.put(entry.getKey(), entry.getValue());
			}
			else if (logical.isValid(entry.getKey(), true))
				validCount += entry.getValue();
			else {
				missCount += entry.getValue();
				minusMatches.put(entry.getKey(), entry.getValue());
				newOutliers.put(entry.getKey(), entry.getValue());
			}
		}

		final FiniteMap newCardinality = new FiniteMap(cardinalityUpper.getMaxCapacity());
		newCardinality.putAll(cardinalityUpper);
		newCardinality.putAll(addMatches);
		for (final String elt : minusMatches.keySet())
			newCardinality.remove(elt);

		if (!logical.analyzeSet(context, validCount, realSamples, facts.getMatchTypeInfo().regexp, facts.calculateFacts(), newCardinality, newOutliers, tokenStreams, analysisConfig).isValid())
			return new FiniteMatchResult();

		return new FiniteMatchResult(logical, logical.getConfidence(validCount, realSamples, context), validCount, newOutliers, newCardinality);
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
		double score;
		FiniteMap newOutliers;
		FiniteMap newCardinality;
		long validCount;
		boolean isMatch;

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

	private LogicalTypeFinite matchFiniteTypes(final FTAType type, final FiniteMap cardinalityUpper, final double scoreToBeat) {
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

				// Choose the best score
				if (result.score > bestScore ||
						// If bestResult is null then this finite match has matched an incoming score to beat,
						// we prefer finite matches to infinite matches if scores are equal
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
		facts.setMatchTypeInfo(new TypeInfo(bestResult.logical.getRegExp(), FTAType.STRING, bestResult.logical.getSemanticType(), facts.getMatchTypeInfo()));

		debug("Type determination - new matchTypeInfo - {}", facts.getMatchTypeInfo());

		return bestResult.logical;
	}

	/**
	 * Calculate the Levenshtein distance of the source string from the 'closest' string from the provided universe.
	 * @param source The source string to test.
	 * @param universe The universe of strings to test for distance
	 * @return The Levenshtein distance from the best match.
	 */
	protected static int distanceLevenshtein(final String source, final Set<String> universe) {
		final LevenshteinDistance distance = new LevenshteinDistance();

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

	/**
	 * Determine the result of the training complete to date. Typically invoked
	 * after all training is complete, but may be invoked at any stage.
	 *
	 * @return A TextAnalysisResult with the analysis of any training completed.
	 * @throws FTAPluginException Thrown when a registered plugin has detected an issue
	 * @throws FTAUnsupportedLocaleException Thrown when a requested locale is not supported
	 */
	public TextAnalysisResult getResult() throws FTAPluginException, FTAUnsupportedLocaleException {
		// Normally we will initialize as a consequence of the first call to train() but just in case no training happens!
		if (!initialized)
			initialize();

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
		if (facts.getMatchTypeInfo().isSemanticType()) {
			final LogicalType logical = plugins.getRegistered(facts.getMatchTypeInfo().semanticType);

			final PluginAnalysis pluginAnalysis = logical.analyzeSet(context, facts.matchCount, realSamples, facts.getMatchTypeInfo().regexp, facts.calculateFacts(), facts.cardinality, facts.outliers, tokenStreams, analysisConfig);
			if (!pluginAnalysis.isValid()) {
				if (logical.acceptsBaseType(FTAType.STRING) || logical.acceptsBaseType(FTAType.LONG) || logical.acceptsBaseType(FTAType.DOUBLE)) {
					backout(logical, realSamples, pluginAnalysis);
					if (logical instanceof LogicalTypeRegExp)
						backedOutRegExp = true;
				}
			}
			else {
				// Update our Regular Expression - since it may have changed based on all the data observed
				facts.getMatchTypeInfo().regexp = logical.getRegExp();
				facts.confidence = logical.getConfidence(facts.matchCount, realSamples, context);
			}
		}

		final FiniteMap cardinalityUpper = new FiniteMap(facts.cardinality.getMaxCapacity());

		if (FTAType.LONG.equals(facts.getMatchTypeInfo().getBaseType()))
			finalizeLong(realSamples);
		else if (FTAType.BOOLEAN.equals(facts.getMatchTypeInfo().getBaseType()) && facts.getMatchTypeInfo().id == KnownTypes.ID.ID_BOOLEAN_Y_N && facts.cardinality.size() == 1)
			finalizeBoolean(realSamples);
		else if (FTAType.DOUBLE.equals(facts.getMatchTypeInfo().getBaseType()) && !facts.getMatchTypeInfo().isSemanticType())
			finalizeDouble(realSamples);
		else if (FTAType.STRING.equals(facts.getMatchTypeInfo().getBaseType())) {
			// Build Cardinality map ignoring case (and white space)
			for (final Map.Entry<String, Long> entry : facts.cardinality.entrySet()) {
				final String key = entry.getKey().toUpperCase(locale).trim();
				cardinalityUpper.merge(key, entry.getValue(), Long::sum);
			}
			// Sort the results so that we consider the most frequent first (we will hopefully fail faster)
			cardinalityUpper.sortByValue();

			final double scoreToBeat = facts.getMatchTypeInfo().isSemanticType() ? facts.confidence : -1.0;

			// We may have a Semantic Type already identified but see if there is a better Finite Semantic type
			final LogicalTypeFinite logical = matchFiniteTypes(FTAType.STRING, cardinalityUpper, scoreToBeat);
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
						// Build the universe of valid characters
						for (final String existing : cardinalityUpper.keySet()) {
							for (int i = 0; i < existing.length(); i++) {
								final char ch = existing.charAt(i);
								if (!Character.isAlphabetic(ch) && !Character.isDigit(ch))
									if (validChars.indexOf(ch) == -1)
										validChars += ch;
							}
						}
						for (int i = 0; i < keyUpper.length(); i++) {
							final char ch = keyUpper.charAt(i);
							if (!Character.isAlphabetic(ch) && !Character.isDigit(ch) && validChars.indexOf(ch) == -1) {
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

					backoutToPattern(realSamples, KnownTypes.PATTERN_ANY_VARIABLE);
					facts.confidence = (double) facts.matchCount / realSamples;
					facts.outliers = remainingOutliers;
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

		if (FTAType.STRING.equals(facts.getMatchTypeInfo().getBaseType()) && facts.getMatchTypeInfo().isSemanticType()) {
			final LogicalType logical = plugins.getRegistered(facts.getMatchTypeInfo().semanticType);
			boolean recalcConfidence = false;

			// Sweep the outliers - flipping them to invalid if they do not pass the relaxed isValid definition
			for (final Map.Entry<String, Long> entry : facts.outliers.entrySet()) {
				// Split the outliers to either invalid entries or valid entries
				if (logical.isValid(entry.getKey(), false)) {
					addValid(entry.getKey(), entry.getValue());
					facts.matchCount += entry.getValue();
					recalcConfidence = true;
				}
				else
					addInvalid(entry.getKey(), entry.getValue());
			}

			if (recalcConfidence)
				facts.confidence = logical.getConfidence(facts.matchCount, realSamples, context);
			facts.outliers.clear();
		}

		// We would really like to say something better than it is a String!
		if (FTAType.STRING.equals(facts.getMatchTypeInfo().getBaseType()) && !facts.getMatchTypeInfo().isSemanticType()) {
			boolean updated = false;

			// If we are currently matching everything then flip to a better Regular Expression based on Stream analysis if possible
			if (facts.matchCount == realSamples && !tokenStreams.isAnyShape()) {
				final String newRegExp = tokenStreams.getRegExp(false);
				if (newRegExp != null) {
					facts.setMatchTypeInfo(new TypeInfo(null, newRegExp, FTAType.STRING, null, false, facts.minTrimmedLength,
							facts.maxTrimmedLength, null, null));
					debug("Type determination - updated based on Stream analysis {}", facts.getMatchTypeInfo());
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

					facts.setMatchTypeInfo(new TypeInfo(null, gen.getResult(), FTAType.STRING, facts.getMatchTypeInfo().typeModifier, false, facts.minTrimmedLength,
							facts.maxTrimmedLength, null, null));
					updated = true;

					// Now we have mapped to an enum we need to check again if this should be matched to a Semantic type
					for (final LogicalTypeRegExp logical : regExpTypes) {
						if (logical.acceptsBaseType(FTAType.STRING) &&
								logical.isMatch(facts.getMatchTypeInfo().regexp) &&
								logical.analyzeSet(context, facts.matchCount, realSamples, facts.getMatchTypeInfo().regexp, facts.calculateFacts(), facts.cardinality, facts.outliers, tokenStreams, analysisConfig).isValid()) {
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
							logical.analyzeSet(context, best.getOccurrences(), realSamples, facts.getMatchTypeInfo().regexp, facts.calculateFacts(), facts.cardinality, facts.outliers, tokenStreams, analysisConfig).isValid()) {
						facts.setMatchTypeInfo(new TypeInfo(regExp, logical.getBaseType(), logical.getSemanticType(), facts.getMatchTypeInfo()));
						facts.matchCount = best.getOccurrences();
						facts.confidence = logical.getConfidence(facts.matchCount, realSamples, context);
						updated = true;
						break;
					}
				}
			}

			// Qualify Alpha or Alnum with a min and max length
			if (!updated && (KnownTypes.PATTERN_ALPHA_VARIABLE.equals(facts.getMatchTypeInfo().regexp) || KnownTypes.PATTERN_ALPHANUMERIC_VARIABLE.equals(facts.getMatchTypeInfo().regexp))) {
				String newPattern = facts.getMatchTypeInfo().regexp;
				newPattern = newPattern.substring(0, newPattern.length() - 1) + lengthQualifier(facts.minTrimmedLength, facts.maxTrimmedLength);
				facts.setMatchTypeInfo(new TypeInfo(null, newPattern, FTAType.STRING, facts.getMatchTypeInfo().typeModifier, false, facts.minTrimmedLength,
						facts.maxTrimmedLength, null, null));
				updated = true;
			}

			// Qualify random string with a min and max length
			if (!updated && KnownTypes.PATTERN_ANY_VARIABLE.equals(facts.getMatchTypeInfo().regexp)) {
				final String newPattern = KnownTypes.freezeANY(facts.minTrimmedLength, facts.maxTrimmedLength, facts.minRawNonBlankLength, facts.maxRawNonBlankLength, facts.leadingWhiteSpace, facts.trailingWhiteSpace, facts.multiline);
				facts.setMatchTypeInfo(new TypeInfo(null, newPattern, FTAType.STRING, facts.getMatchTypeInfo().typeModifier, false, facts.minRawLength,
						facts.maxRawLength, null, null));
				updated = true;
			}
		}

		// Only attempt to do key identification if we have not already been told the answer
		if (facts.keyConfidence == null) {
			// Attempt to identify keys?
			facts.keyConfidence = 0.0;
			if (facts.sampleCount > MIN_SAMPLES_FOR_KEY && analysisConfig.getMaxCardinality() >= MIN_SAMPLES_FOR_KEY / 2 &&
					(facts.cardinality.size() == analysisConfig.getMaxCardinality() || facts.cardinality.size() == facts.sampleCount) &&
					facts.blankCount == 0 && facts.nullCount == 0 &&
					((facts.getMatchTypeInfo().isSemanticType() && facts.getMatchTypeInfo().semanticType.equals("GUID")) ||
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

		final TextAnalysisResult result = new TextAnalysisResult(context.getStreamName(),
				facts.calculateFacts(), context.getDateResolutionMode(), analysisConfig, tokenStreams);

		if (traceConfig != null)
			traceConfig.recordResult(result, internalErrors);

		return result;
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
					if (((newMatchCount = tokenStreams.matches(re, logical.getThreshold())) != 0) &&
							logical.analyzeSet(context, facts.matchCount, realSamples, facts.getMatchTypeInfo().regexp, facts.calculateFacts(), facts.cardinality, facts.outliers, tokenStreams, analysisConfig).isValid()) {
						logical.setMatchEntry(entry);
						facts.setMatchTypeInfo(new TypeInfo(logical.getRegExp(), logical.getBaseType(), logical.getSemanticType(), facts.getMatchTypeInfo()));
						facts.matchCount = newMatchCount;
						debug("Type determination - updated to Regular Expression Semantic type {}", facts.getMatchTypeInfo());
						facts.confidence = logical.getConfidence(facts.matchCount, realSamples, context);
						return true;
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
			if (Integer.valueOf(s.trim()) != 0)
				try {
					trackDateTime(s, facts.getMatchTypeInfo(), true);
				}
				catch (DateTimeException e) {
					// Swallow - any we lost are no good so will not be in the top/bottom set!
				}
	}

	// Called to finalize a LONG type determination when NOT a Semantic type
	void finalizeLong(final long realSamples) {
		if (KnownTypes.ID.ID_LONG == facts.getMatchTypeInfo().id && facts.getMatchTypeInfo().typeModifier == null && facts.minLong < 0) {
			facts.setMatchTypeInfo(knownTypes.negation(facts.getMatchTypeInfo().regexp));
			debug("Type determination - now with sign {}", facts.getMatchTypeInfo());
		}

		// Sometimes a Long is not a Long but it is really a date
		if (facts.groupingSeparators == 0 && facts.minLongNonZero != Long.MAX_VALUE && facts.minLongNonZero > EARLY_LONG_YYYYMMDD && facts.maxLong < LATE_LONG_YYYYMMDD &&
				DateTimeParser.plausibleDateLong(facts.minLongNonZero, 4) && DateTimeParser.plausibleDateLong(facts.maxLong, 4) &&
				((realSamples >= reflectionSamples && facts.cardinality.size() > 10) || context.getStreamName().toLowerCase(locale).contains("date"))) {
			facts.setMatchTypeInfo(new TypeInfo(null, "\\d{8}", FTAType.LOCALDATE, "yyyyMMdd", false, 8, 8, null, "yyyyMMdd"));
			facts.killZeroes();
			final DateTimeFormatter dtf = dateTimeParser.ofPattern(facts.getMatchTypeInfo().format);
			facts.minLocalDate = LocalDate.parse(String.valueOf(facts.minLongNonZero), dtf);
			facts.maxLocalDate = LocalDate.parse(String.valueOf(facts.maxLong), dtf);

			// If we are collecting statistics - we need to generate the topK and bottomK
			if (analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS))
				generateTopBottom();
		} else if (facts.groupingSeparators == 0 && facts.minLongNonZero != Long.MAX_VALUE && facts.minLongNonZero > DateTimeParser.EARLY_LONG_YYYY && facts.maxLong <= DateTimeParser.LATE_LONG_YYYY &&
				((realSamples >= reflectionSamples && facts.cardinality.size() > 10) ||
						keywords.match(context.getStreamName(), "YEAR", Keywords.MatchStyle.CONTAINS) >= 90 ||
						keywords.match(context.getStreamName(), "DATE", Keywords.MatchStyle.CONTAINS) >= 90)) {
			facts.setMatchTypeInfo(new TypeInfo(null, "\\d{4}", FTAType.LOCALDATE, "yyyy", false, 4, 4, null, "yyyy"));
			facts.killZeroes();
			facts.minLocalDate = LocalDate.of((int)facts.minLongNonZero, 1, 1);
			facts.maxLocalDate = LocalDate.of((int)facts.maxLong, 1, 1);

			// If we are collecting statistics - we need to generate the topK and bottomK
			if (analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS))
				generateTopBottom();
		} else if (facts.cardinality.size() == 2 && facts.minLong == 0 && facts.maxLong == 1) {
			// boolean by any other name
			facts.minBoolean = "0";
			facts.maxBoolean = "1";
			facts.setMatchTypeInfo(knownTypes.getByID(KnownTypes.ID.ID_BOOLEAN_ONE_ZERO));
		} else {
			if (!facts.getMatchTypeInfo().isSemanticType() && facts.groupingSeparators != 0 && !facts.getMatchTypeInfo().hasGrouping()) {
				facts.setMatchTypeInfo(knownTypes.grouping(facts.getMatchTypeInfo().regexp));
				debug("Type determination - now with grouping {}", facts.getMatchTypeInfo());
			}

			if (!facts.getMatchTypeInfo().isSemanticType()) {
				// Create a new TypeInfo - we don't want to change a predefined one!
				facts.setMatchTypeInfo(new TypeInfo(facts.getMatchTypeInfo()));
				facts.getMatchTypeInfo().regexp = freezeNumeric(facts.getMatchTypeInfo().regexp);
			}

			final double scoreToBeat = facts.getMatchTypeInfo().isSemanticType() ? facts.confidence : -1.0;

			// We may have a Semantic Type already identified but see if there is a better Finite Semantic type
			final LogicalTypeFinite logicalFinite = matchFiniteTypes(FTAType.LONG, facts.cardinality, scoreToBeat);
			if (logicalFinite != null)
				facts.confidence = logicalFinite.getConfidence(facts.matchCount, realSamples, context);

			if (!facts.getMatchTypeInfo().isSemanticType())
				for (final LogicalTypeRegExp logical : regExpTypes) {
					if (logical.acceptsBaseType(FTAType.LONG) &&
							logical.isMatch(facts.getMatchTypeInfo().regexp) &&
							logical.analyzeSet(context, facts.matchCount, realSamples, facts.getMatchTypeInfo().regexp, facts.calculateFacts(), facts.cardinality, facts.outliers, tokenStreams, analysisConfig).isValid()) {
						facts.setMatchTypeInfo(new TypeInfo(logical.getRegExp(), logical.getBaseType(), logical.getSemanticType(), facts.getMatchTypeInfo()));
						facts.confidence = logical.getConfidence(facts.matchCount, realSamples, context);
						debug("Type determination - was LONG, matchTypeInfo - {}", facts.getMatchTypeInfo());
						break;
					}
				}

			if (!facts.getMatchTypeInfo().isSemanticType() && realSamples >= analysisConfig.getDetectWindow() &&
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
	}

	private void finalizeBoolean(final long realSamples) {
		backoutToString(realSamples);
		facts.confidence = (double) facts.matchCount / realSamples;
	}

	// Called to finalize a DOUBLE type determination when NOT a Semantic type
	private void finalizeDouble(final long realSamples) {
		if (facts.minDouble < 0.0) {
			facts.setMatchTypeInfo(knownTypes.negation(facts.getMatchTypeInfo().regexp));
			debug("Type determination - now with sign {}", facts.getMatchTypeInfo());
		}

		if (facts.groupingSeparators != 0 && !facts.getMatchTypeInfo().hasGrouping()) {
			facts.setMatchTypeInfo(knownTypes.grouping(facts.getMatchTypeInfo().regexp));
			debug("Type determination - now with grouping {}", facts.getMatchTypeInfo());
		}

		for (final LogicalTypeRegExp logical : regExpTypes) {
			if (logical.acceptsBaseType(FTAType.DOUBLE) &&
					logical.isMatch(facts.getMatchTypeInfo().regexp) &&
					logical.analyzeSet(context, facts.matchCount, realSamples, facts.getMatchTypeInfo().regexp, facts.calculateFacts(), facts.cardinality, facts.outliers, tokenStreams, analysisConfig).isValid()) {
				facts.setMatchTypeInfo(new TypeInfo(logical.getRegExp(), logical.getBaseType(), logical.getSemanticType(), facts.getMatchTypeInfo()));
				facts.confidence = logical.getConfidence(facts.matchCount, realSamples, context);
				break;
			}
		}

		// All outliers are actually invalid
		if (!facts.outliers.isEmpty()) {
			facts.invalid.putAll(facts.outliers);
			facts.outliers.clear();
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
		// If we have not already determined the type - we need to force the issue
		if (facts.getMatchTypeInfo() == null)
			determineType();

		final TextAnalyzerWrapper wrapper = new TextAnalyzerWrapper(analysisConfig, context, facts.calculateFacts());
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		try {
			return mapper.writeValueAsString(mapper.convertValue(wrapper, JsonNode.class));
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
		final ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		TextAnalyzer ret = null;

		try {
			final TextAnalyzerWrapper wrapper = mapper.readValue(serialized, TextAnalyzerWrapper.class);
			ret = new TextAnalyzer(wrapper.analyzerContext);
			ret.setConfig(wrapper.analysisConfig);

			ret.facts = wrapper.facts;
			ret.facts.setConfig(wrapper.analysisConfig);
			ret.facts.hydrate();
			ret.initialize();

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
		TextAnalyzer ret = new TextAnalyzer(first.context);

		if (!first.analysisConfig.equals(second.analysisConfig))
			throw new FTAMergeException("The AnalysisConfig for both TextAnalyzers must be identical.");

		ret.setConfig(first.analysisConfig);

		// Train using all the non-null/non-blank elements
		final Map<String, Long>merged = new HashMap<>();

		// Prime the merged set with the first set (real, outliers, and invalid which are non-overlapping)
		final Facts firstFacts = first.facts.calculateFacts();
		merged.putAll(firstFacts.cardinality);
		merged.putAll(firstFacts.outliers);
		merged.putAll(firstFacts.invalid);
		// Preserve the top and bottom values - even if they were not captured in the cardinality set
		addToMap(merged, firstFacts.topK, first);
		addToMap(merged, firstFacts.bottomK, first);

		// Merge in the second set
		final Facts secondFacts = second.facts.calculateFacts();
		for (final Map.Entry<String, Long>entry : secondFacts.cardinality.entrySet()) {
			final Long seen = firstFacts.cardinality.get(entry.getKey());
			if (seen == null) {
				merged.put(entry.getKey(), entry.getValue());
			}
			else
				merged.put(entry.getKey(), seen + entry.getValue());
		}
		for (final Map.Entry<String, Long>entry : secondFacts.outliers.entrySet()) {
			final Long seen = firstFacts.outliers.get(entry.getKey());
			if (seen == null) {
				merged.put(entry.getKey(), entry.getValue());
			}
			else
				merged.put(entry.getKey(), seen + entry.getValue());
		}
		for (final Map.Entry<String, Long>entry : secondFacts.invalid.entrySet()) {
			final Long seen = firstFacts.invalid.get(entry.getKey());
			if (seen == null) {
				merged.put(entry.getKey(), entry.getValue());
			}
			else
				merged.put(entry.getKey(), seen + entry.getValue());
		}
		// Preserve the top and bottom values - even if they were not captured in the cardinality set
		addToMap(merged, secondFacts.topK, second);
		addToMap(merged, secondFacts.bottomK, second);
		ret.trainBulk(merged);

		ret.facts.nullCount = firstFacts.nullCount + secondFacts.nullCount;
		ret.facts.blankCount = firstFacts.blankCount + secondFacts.blankCount;
		ret.facts.sampleCount += ret.facts.nullCount + ret.facts.blankCount;
		if (firstFacts.totalCount != -1 && secondFacts.totalCount != -1)
			ret.facts.totalCount = firstFacts.totalCount + secondFacts.totalCount;

		// Set the min/maxRawLength just in case a blank field is the longest/shortest
		ret.facts.minRawLength = Math.min(first.facts.minRawLength, second.facts.minRawLength);
		ret.facts.maxRawLength = Math.max(first.facts.maxRawLength, second.facts.maxRawLength);

		// So if both sets are unique in their own right and the sets are non-overlapping then the merged set is unique
		if (firstFacts.getMatchTypeInfo() != null && firstFacts.uniqueness != null && firstFacts.uniqueness == 1.0 &&
				secondFacts.uniqueness != null && secondFacts.uniqueness == 1.0 &&
				nonOverlappingRegions(firstFacts, secondFacts, ret.analysisConfig))
			ret.facts.uniqueness = 1.0;

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
//			if (ret.facts.outliers.size() != 0)
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
		        ret.facts.variance = ((first.facts.matchCount - 1)*first.facts.variance + (second.facts.matchCount - 1)*second.facts.variance)/(first.facts.matchCount+second.facts.matchCount-2);
		        ret.facts.currentM2 = ret.facts.variance * ret.facts.matchCount;
			}
		}

		return ret;
	}


	private static boolean nonOverlappingRegions(final Facts firstFacts, final Facts secondFacts, final AnalysisConfig analysisConfig) {
		StringConverter stringConverter = new StringConverter(firstFacts.getMatchTypeInfo().getBaseType(), new TypeFormatter(firstFacts.getMatchTypeInfo(), analysisConfig));
		if (stringConverter.toDouble(firstFacts.getMinValue()) == stringConverter.toDouble(secondFacts.getMinValue()))
			return false;

		if (stringConverter.toDouble(firstFacts.getMinValue()) < stringConverter.toDouble(secondFacts.getMinValue()))
			return stringConverter.toDouble(firstFacts.getMaxValue()) < stringConverter.toDouble(secondFacts.getMinValue());

		return stringConverter.toDouble(secondFacts.getMaxValue()) < stringConverter.toDouble(firstFacts.getMinValue());
	}

	protected Facts getFacts() {
		return facts;
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

		for (final String e : extremes) {
			if (e == null)
				return;
			// If we already have it in the merged set then we are done
			if (merged.get(e) != null)
				continue;
			final Object extreme = analyzer.facts.getStringConverter().getValue(e);
			if (extreme == null)
				continue;
			boolean found = false;
			for (final String m : merged.keySet()) {
				// Check for equality of value not of format - e.g. "00" will equal "0" once both are converted to Longs
				final Object mValue = analyzer.facts.getStringConverter().getValue(m);
				if (mValue != null && mValue.equals(extreme)) {
					found = true;
					break;
				}
			}
			if (!found)
				merged.put(e, 1L);
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
