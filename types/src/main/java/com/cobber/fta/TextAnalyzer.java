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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
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
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.cobber.fta.dates.DateTimeParserResult;
import com.cobber.fta.dates.LocaleInfo;
import com.cobber.fta.token.Token;
import com.cobber.fta.token.TokenStream;
import com.cobber.fta.token.TokenStreams;

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
	/** Should we attempt to qualifier the size of the returned RexExp. */
	private boolean lengthQualifier = true;

	/** Plugin Threshold for detection - by default this is unset and sensible defaults are used. */
	private int pluginThreshold = -1;

	/** Enable Numeric widening - i.e. if we see lots of integers then some doubles call it a double. */
	private boolean numericWidening = true;

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
	private long totalCount = -1;
	private long sampleCount;
	private long nullCount;
	private long blankCount;
	private Map<String, Long> cardinality = new HashMap<>();
	private Map<String, Long> outliers = new HashMap<>();
	private final Map<String, Long> outliersSmashed = new HashMap<>();
	private List<String> raw; // 0245-11-98
	// 0: d{4}-d{2}-d{2} 1: d{+}-d{+}-d{+} 2: d{+}-d{+}-d{+}
	// 0: d{4} 1: d{+} 2: [-]d{+}
	// input "hello world" 0: a{5} a{5} 1: a{+} a{+} 2: a{+}

	private final TokenStreams tokenStreams = new TokenStreams(AnalysisConfig.MAX_SHAPES_DEFAULT);

	private final Random random = new Random(271828);

	private Trace traceConfig;

	private int internalErrors;

	private final static String insufficient = "Insufficient digits in input (";

	private Logger logger = null;

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

	private long matchCount;
	private PatternInfo matchPatternInfo;

	private boolean trainingStarted;
	private boolean initialized;

	private Facts facts;

	private long minLongNonZero = Long.MAX_VALUE;
	private boolean monotonicIncreasing = true;
	private boolean monotonicDecreasing = true;

	private String minOutlierString;
	private String maxOutlierString;

	// The minimum length (not trimmed) - but must be non-Blank
	private int minRawNonBlankLength = Integer.MAX_VALUE;
	// The maximum length (not trimmed) - but must be non-Blank
	private int maxRawNonBlankLength = Integer.MIN_VALUE;

	private int minTrimmedLength = Integer.MAX_VALUE;
	private int maxTrimmedLength = Integer.MIN_VALUE;

	private int minTrimmedLengthNumeric = Integer.MAX_VALUE;
	private int maxTrimmedLengthNumeric = Integer.MIN_VALUE;

	private int minTrimmedOutlierLength = Integer.MAX_VALUE;
	private int maxTrimmedOutlierLength = Integer.MIN_VALUE;

	private int possibleDateTime;
	private long groupingSeparators;

	private final List<LogicalTypeInfinite> infiniteTypes = new ArrayList<>();
	private final List<LogicalTypeFinite> finiteTypes = new ArrayList<>();
	private final List<LogicalTypeRegExp> regExpTypes = new ArrayList<>();
	private int[] candidateCounts;

	private final KnownPatterns knownPatterns = new KnownPatterns();

	private DateTimeParser dateTimeParser;

	private final Plugins plugins = new Plugins();

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
	 * Indicate whether to collect statistics or not.
	 *
     * @param collectStatistics
     *            A boolean indicating the desired state
	 * @return The previous value of this parameter.
	 */
	public boolean setCollectStatistics(final boolean collectStatistics) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot adjust statistics collection once training has started");

		final boolean ret = collectStatistics;
		analysisConfig.setCollectStatistics(collectStatistics);
		return ret;
	}

	/**
	 * Internal Only.  Enable internal debugging.
	 *
	 * @param debug The debug level.
	 */
	public void setDebug(final int debug) {
		analysisConfig.debug = debug;
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
		analysisConfig.traceOptions = traceOptions;
	}

	/**
	 * Indicates whether to collect statistics or not.
	 *
	 * @return Whether Statistics collection is enabled.
	 */
	public boolean getCollectStatistics() {
		return analysisConfig.isCollectStatistics();
	}

	/**
	 * Indicate whether to enable default Logical Type processing.
	 *
	 * @param logicalTypeDetection
	 *            A boolean indicating the desired state
	 * @return The previous value of this parameter.
	 */
	public boolean setDefaultLogicalTypes(final boolean logicalTypeDetection) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot adjust Logical Type detection once training has started");

		final boolean ret = logicalTypeDetection;
		analysisConfig.setEnableDefaultLogicalTypes(logicalTypeDetection);
		return ret;
	}

	/**
	 * Indicates whether to enable default Logical Type processing or not.
	 *
	 * @return Whether default Logical Type processing collection is enabled.
	 */
	public boolean getDefaultLogicalTypes() {
		return analysisConfig.isEnableDefaultLogicalTypes();
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
	 * The percentage when we declare success 0 - 100 for Logical Type plugins.
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
	 * Get the current detection Threshold for Logical Type plugins.
	 * If not set, this will return -1, this means that each plugin is using a default threshold and doing something sensible!
	 *
	 * @return The current threshold.
	 */
	public int getPluginThreshold() {
		return pluginThreshold;
	}

	/**
	 * If true enable Numeric widening - that is, if we see lots of integers then some doubles call it a double.
	 * <p>Note: Defaults to true.
	 * @param numericWidening The new value for numericWidening.
	 */
	public void setNumericWidening(final boolean numericWidening) {
		this.numericWidening = numericWidening;
	}

	/**
	 * Get the current value for numeric widening.
	 *
	 * @return The current value.
	 */
	public boolean getNumericWidening() {
		return numericWidening;
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
	 * <li>The Cardinality must be larger than the Cardinality of the largest Finite Logical type.
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
	 * @return The maximum cardinality.
	 */
	public int getMaxOutliers() {
		return analysisConfig.getMaxOutliers();
	}

	/**
	 * Indicate whether we should qualify the size of the RegExp.
	 * For example "\d{3,6}" vs. "\d+"
	 * <p>Note: This only impacts simple Numerics/Alphas/AlphaNumerics.
	 *
	 * @param newLengthQualifier The new value.
	 *
	 * @return The previous value of this parameter.
	 */
	public boolean setLengthQualifier(final boolean newLengthQualifier) {

		final boolean ret = this.lengthQualifier;
		lengthQualifier = newLengthQualifier;
		return ret;
	}

	/**
	 * Indicates whether the size of the RegExp pattern is being defined.
	 *
	 * @return True if lengths are being qualified.
	 */
	public boolean getLengthQualifier() {
		return lengthQualifier;
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
	 * Set the total number of elements in the Data Stream (if known).
	 * @param totalCount The total number of elements, as opposed to the number sampled.
	 */
	public void setTotalCount(final long totalCount) {
		this.totalCount = totalCount;
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
		if (maxInputLength < AnalysisConfig.MAX_INPUT_LENGTH_DEFAULT)
			throw new IllegalArgumentException("Invalid value for maxInputLength (must be >= " + AnalysisConfig.MAX_INPUT_LENGTH_DEFAULT + ")");

		return analysisConfig.setMaxInputLength(maxInputLength);
	}

	/**
	 * Gets the current maximum input length for sampling.
	 * @return The current maximum length before an input sample is truncated.
	 */
	public int getMaxInputLength() {
		return analysisConfig.getMaxInputLength();
	}

	String getRegExp(final KnownPatterns.ID id) {
		return knownPatterns.getRegExp(id);
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
			if (length != 0 && length < minRawNonBlankLength)
				minRawNonBlankLength = length;
			if (length > maxRawNonBlankLength)
				maxRawNonBlankLength = length;

			tokenStreams.track(trimmed, count);
		}
	}

	private boolean trackLong(final String trimmed, final PatternInfo patternInfo, final boolean register) {
		// Track String facts - just in case we end up backing out.
		if (facts.minString == null || facts.minString.compareTo(trimmed) > 0)
			facts.minString = trimmed;

		if (facts.maxString == null || facts.maxString.compareTo(trimmed) < 0)
			facts.maxString = trimmed;

		long l;

		// Interpret the String as a long, first attempt uses parseLong which is fast (although not localized), if that fails,
		// then try using a NumberFormatter which will cope with grouping separators (e.g. 1,000).
		int digits = trimmed.length();

		try {
			if (patternInfo.id == KnownPatterns.ID.ID_SIGNED_LONG_TRAILING) {
				if (digits >= 2 && trimmed.charAt(digits - 1) == '-') {
					l = -Long.parseLong(trimmed.substring(0, digits - 1));
					digits--;
				}
				else
					l = Long.parseLong(trimmed);
			}
			else {
				l = Long.parseLong(trimmed);
				final char ch = trimmed.charAt(0);
				if (ch == '-' || ch == '+')
					digits--;
			}
		} catch (NumberFormatException e) {
			final ParsePosition pos = new ParsePosition(0);
			final Number n = longFormatter.parse(trimmed, pos);
			if (n == null || trimmed.length() != pos.getIndex())
				return false;
			l = n.longValue();
			if (trimmed.indexOf(localeGroupingSeparator) != -1)
				groupingSeparators++;
			digits = trimmed.length();
			final char ch = trimmed.charAt(0);
			if (hasNegativePrefix && (ch == '-' || ch == '+' || ch == negativePrefix))
				digits--;
			if (l < 0 && hasNegativeSuffix)
				digits--;
		}

		if (register) {
			if (trimmed.charAt(0) == '0' && digits != 1)
				facts.leadingZeroCount++;

			if (digits < minTrimmedLengthNumeric)
				minTrimmedLengthNumeric = digits;
			if (digits > maxTrimmedLengthNumeric)
				maxTrimmedLengthNumeric = digits;

			if (l != 0 && l < minLongNonZero)
				minLongNonZero = l;

			if (l < facts.minLong)
				facts.minLong = l;
			else
				monotonicDecreasing = false;

			if (l > facts.maxLong)
				facts.maxLong = l;
			else
				monotonicIncreasing = false;

			if (analysisConfig.isCollectStatistics()) {
				// Calculate the mean & standard deviation using Welford's algorithm
				final double delta = l - facts.currentMean;
				// matchCount is one low - because we do not 'count' the record until we return from this routine indicating valid
				facts.currentMean += delta / (matchCount + 1);
				facts.currentM2 += delta * (l - facts.currentMean);

				facts.tbLong.observe(l);
			}
		}

		if (patternInfo.isLogicalType()) {
			// If it is a registered Infinite Logical Type then validate it
			final LogicalType logical = plugins.getRegistered(patternInfo.typeQualifier);
			if (logical.acceptsBaseType(FTAType.LONG))
				return logical.isValid(trimmed);
		}

		return true;
	}

	private boolean trackBoolean(final String trimmed) {
		final String trimmedLower = trimmed.toLowerCase(locale);

		final boolean isTrue = "true".equals(trimmedLower) || "yes".equals(trimmedLower) || "y".equals(trimmedLower);
		final boolean isFalse = !isTrue && ("false".equals(trimmedLower) || "no".equals(trimmedLower) || "n".equals(trimmedLower));

		if (isTrue) {
			if (facts.minBoolean == null)
				facts.minBoolean = trimmedLower;
			if (facts.maxBoolean == null || "false".equals(facts.maxBoolean) || "no".equals(facts.maxBoolean) || "n".equals(facts.maxBoolean))
				facts.maxBoolean = trimmedLower;
		} else if (isFalse) {
			if (facts.maxBoolean == null)
				facts.maxBoolean = trimmedLower;
			if (facts.minBoolean == null || "true".equals(facts.minBoolean) || "yes".equals(facts.maxBoolean) || "y".equals(facts.maxBoolean))
				facts.minBoolean = trimmedLower;
		}

		return isTrue || isFalse;
	}

	private boolean trackString(final String rawInput, final String trimmed, final PatternInfo patternInfo, final boolean register) {
		if (register && analysisConfig.debug >= 2 && rawInput.length() > 0 && rawInput.charAt(0) == '¶' && "¶ xyzzy ¶".equals(rawInput))
			throw new NullPointerException("¶ xyzzy ¶");
		if (patternInfo.typeQualifier == null) {
			for (int i = 0; i < trimmed.length(); i++) {
				if (patternInfo.isAlphabetic() && !Character.isAlphabetic(trimmed.charAt(i)))
					return false;
				if (patternInfo.isAlphanumeric() && !Character.isLetterOrDigit((trimmed.charAt(i))))
					return false;
			}
		}
		else if (patternInfo.isLogicalType()) {
			// If it is a registered Infinite Logical Type then validate it
			final LogicalType logical = plugins.getRegistered(patternInfo.typeQualifier);
			if (logical.acceptsBaseType(FTAType.STRING) && !logical.isValid(rawInput))
				return false;
		}

		return updateStats(rawInput);
	}

	private boolean updateStats(final String cleaned) {
		final int len = cleaned.trim().length();

		if (matchPatternInfo.getMinLength() != -1 && len < matchPatternInfo.getMinLength())
			return false;
		if (matchPatternInfo.getMaxLength() != -1 && len > matchPatternInfo.getMaxLength())
			return false;

		if (facts.minString == null || facts.minString.compareTo(cleaned) > 0)
			facts.minString = cleaned;

		if (facts.maxString == null || facts.maxString.compareTo(cleaned) < 0)
			facts.maxString = cleaned;

		if (len < minTrimmedLength)
			minTrimmedLength = len;
		if (len > maxTrimmedLength)
			maxTrimmedLength = len;

		if (analysisConfig.isCollectStatistics())
			facts.tbString.observe(cleaned);

		return true;
	}

	private boolean trackDouble(final String rawInput, final PatternInfo patternInfo, final boolean register) {
		final String input = rawInput.trim();
		double d;

		try {
			// parseDouble is not locale sensitive, but it is fast!
			if (patternInfo.id == KnownPatterns.ID.ID_SIGNED_DOUBLE_TRAILING) {
				final int digits = input.length();
				if (digits >= 2 && input.charAt(digits - 1) == '-')
					d = -Double.parseDouble(input.substring(0, digits - 1));
				else
					d = Double.parseDouble(input);
			}
			else
				d = Double.parseDouble(input);
		} catch (NumberFormatException e) {
			// Failed to parse using the naive parseDouble, so use the locale-sensitive Numberformat.parse
			final ParsePosition pos = new ParsePosition(0);
			final Number n = doubleFormatter.parse(input, pos);
			if (n == null || input.length() != pos.getIndex())
				return false;
			d = n.doubleValue();
			if (input.indexOf(localeGroupingSeparator) != -1)
				groupingSeparators++;
			// Make sure to track the decimal separator being used for doubles
			if (localeDecimalSeparator != '.' && facts.decimalSeparator != localeDecimalSeparator && input.indexOf(localeDecimalSeparator) != -1)
				facts.decimalSeparator = localeDecimalSeparator;
		}

		if (patternInfo.isLogicalType()) {
			// If it is a registered Infinite Logical Type then validate it
			final LogicalType logical = plugins.getRegistered(patternInfo.typeQualifier);
			if (logical.acceptsBaseType(FTAType.DOUBLE) && !logical.isValid(input))
				return false;
		}

		// If it is NaN/Infinity then we are all done
		if (Double.isNaN(d) || Double.isInfinite(d))
			return true;

		if (register && analysisConfig.isCollectStatistics()) {
			if (d < facts.minDouble)
				facts.minDouble = d;

			if (d > facts.maxDouble)
				facts.maxDouble = d;

			// Calculate the mean & standard deviation using Welford's algorithm
			final double delta = d - facts.currentMean;
			// matchCount is one low - because we do not 'count' the record until we return from this routine indicating valid
			facts.currentMean += delta / (matchCount + 1);
			facts.currentM2 += delta * (d - facts.currentMean);

			facts.tbDouble.observe(d);
		}

		return true;
	}

	/*
	 * Validate (and track) the date/time/datetime input.
	 * This routine is called for every date/time/datetime we see in the input, so performance is critical.
	 */
	private boolean trackDateTime(final String input, final PatternInfo patternInfo, final boolean register) {
		final String dateFormat = patternInfo.format;

		// Retrieve the (likely cached) DateTimeParserResult for the supplied dateFormat
		final DateTimeParserResult result = DateTimeParserResult.asResult(dateFormat, context.getDateResolutionMode(), locale);
		if (result == null)
			throw new InternalErrorException("NULL result for " + dateFormat);

		final DateTimeFormatter formatter = DateTimeParser.ofPattern(result.getFormatString(), locale);

		final String trimmed = input.trim();

		// If we are not collecting statistics we can use the parse on DateTimeParserResult which is
		// significantly faster than the parse on LocalTime/LocalDate/LocalDateTime/...
		switch (result.getType()) {
		case LOCALTIME:
			if (register && analysisConfig.isCollectStatistics()) {
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
			if (register && analysisConfig.isCollectStatistics()) {
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
			if (register && analysisConfig.isCollectStatistics()) {
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
			if (register && analysisConfig.isCollectStatistics()) {
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
			if (register && analysisConfig.isCollectStatistics()) {
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
	 * Register the default set of plugins for Logical Type detection.
	 * <p>Note: If the locale is null it will default to the Default locale.
	 *
	 * @param locale The Locale used for analysis, the will impact both the set of plugins registered as well as the behavior of the individual plugins
	 *
	 */
	public void registerDefaultPlugins(final Locale locale) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/plugins.json"), StandardCharsets.UTF_8))) {
			plugins.registerPluginsInternal(reader, context.getStreamName(), locale);
		} catch (Exception e) {
			throw new IllegalArgumentException("Internal error: Issues with plugins file: " + e.getMessage(), e);
		}
	}

	private void initialize() throws FTAPluginException, FTAUnsupportedLocaleException {
		final Calendar cal = Calendar.getInstance(locale);
		if (!(cal instanceof GregorianCalendar))
			throw new FTAUnsupportedLocaleException("No support for locales that do not use the Gregorian Calendar");

		if (!NumberFormat.getNumberInstance(locale).format(0).matches("\\d"))
			throw new FTAUnsupportedLocaleException("No support for locales that do not use Arabic numerals");

		if (LocaleInfo.isSupported(locale) != null)
			throw new FTAUnsupportedLocaleException(LocaleInfo.isSupported(locale));

		raw = new ArrayList<>(analysisConfig.getDetectWindow());
		detectWindowEscalations = new ArrayList<>(analysisConfig.getDetectWindow());

		// If enabled, load the default set of plugins for Logical Type detection
		if (analysisConfig.isEnableDefaultLogicalTypes())
			registerDefaultPlugins(locale);

		for (final LogicalType logical : plugins.getRegisteredLogicalTypes()) {

			if ((logical instanceof LogicalTypeFinite) && ((LogicalTypeFinite)logical).getSize() + 10 > getMaxCardinality())
				throw new FTAPluginException("Internal error: Max Cardinality: " + getMaxCardinality() + " is insufficient to support plugin: " + logical.getQualifier());

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
			if (!signFacts.isEmpty() && signFacts.charAt(0) == KnownPatterns.LEFT_TO_RIGHT_MARK)
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

		knownPatterns.initialize(locale);

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

		dateTimeParser = new DateTimeParser(context.getDateResolutionMode(), locale);

		// If no trace options already set then pick them up from the environment (if set)
		if (analysisConfig.traceOptions == null) {
			final String ftaTrace = System.getenv("FTA_TRACE");
			if (ftaTrace != null && !ftaTrace.isEmpty())
				analysisConfig.traceOptions = ftaTrace;
		}

		if (analysisConfig.traceOptions != null)
			traceConfig = new Trace(analysisConfig.traceOptions, context,  analysisConfig);

		// Now that we have initialized these facts cannot change, so set them on the Facts object
		this.facts.setCollectStatistics(analysisConfig.isCollectStatistics());
		this.facts.setLocale(this.locale);

		initialized = true;
	}

	StringBuilder[]
	determineNumericPattern(final SignStatus signStatus, final int numericDecimalSeparators, final int possibleExponentSeen) {
		StringBuilder[] result = new StringBuilder[2];

		if (signStatus == SignStatus.TRAILING_MINUS) {
			result[0] = result[1] = new StringBuilder(numericDecimalSeparators == 1 ? knownPatterns.PATTERN_SIGNED_DOUBLE_TRAILING : knownPatterns.PATTERN_SIGNED_LONG_TRAILING);
			return result;
		}

		final boolean numericSigned = signStatus == SignStatus.LOCALE_STANDARD || signStatus == SignStatus.LEADING_SIGN;

		if (numericDecimalSeparators == 1) {
			if (possibleExponentSeen == -1) {
				result[0] = new StringBuilder(numericSigned ? knownPatterns.PATTERN_SIGNED_DOUBLE : knownPatterns.PATTERN_DOUBLE);
				result[1] = new StringBuilder(knownPatterns.PATTERN_SIGNED_DOUBLE);
			}
			else {
				result[0] = new StringBuilder(numericSigned ? knownPatterns.PATTERN_SIGNED_DOUBLE_WITH_EXPONENT : knownPatterns.PATTERN_DOUBLE_WITH_EXPONENT);
				result[1] = new StringBuilder(knownPatterns.PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
			}
		}
		else {
			if (possibleExponentSeen == -1) {
				result[0] = new StringBuilder(numericSigned ? knownPatterns.PATTERN_SIGNED_LONG : knownPatterns.PATTERN_LONG);
				result[1] = new StringBuilder(knownPatterns.PATTERN_SIGNED_LONG);
			}
			else {
				result[0] = new StringBuilder(numericSigned ? knownPatterns.PATTERN_SIGNED_DOUBLE_WITH_EXPONENT : knownPatterns.PATTERN_DOUBLE_WITH_EXPONENT);
				result[1] = new StringBuilder(knownPatterns.PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
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
		sampleCount += count;

		if (rawInput == null) {
			nullCount += count;
			return;
		}

		final String trimmed = rawInput.trim();

		final int length = trimmed.length();

		if (length == 0) {
			blankCount += count;
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
			if (analysisConfig.debug != 0)
				throw new InternalErrorException(e.getMessage());
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
			traceConfig.recordSample(rawInput, sampleCount);

		sampleCount++;

		final FTAType matchType = matchPatternInfo != null ? matchPatternInfo.getBaseType() : null;

		if (rawInput == null) {
			nullCount++;
			return matchType != null;
		}

		final String trimmed = rawInput.trim();

		final int length = trimmed.length();

		if (length == 0) {
			blankCount++;
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
			if (analysisConfig.debug != 0)
				throw new InternalErrorException(e.getMessage());
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
		if (matchPatternInfo != null && matchPatternInfo.getBaseType() != null)
			return true;

		raw.add(rawInput);

		final int length = trimmed.length();

		final StringBuilder l0 = new StringBuilder(length);

		// Walk the string
		SignStatus numericSigned = SignStatus.NONE;
		int numericDecimalSeparators = 0;
		int numericGroupingSeparators = 0;
		boolean couldBeNumeric = true;
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
				digitsSeen++;
			} else if (ch == localeDecimalSeparator) {
				l0.append('D');
				numericDecimalSeparators++;
			} else if (ch == localeGroupingSeparator) {
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
				// If the last character was an exponentiation symbol then this better be a sign if it is going to be numeric
				if (possibleExponentSeen != -1 && possibleExponentSeen == i - 1) {
					if (ch != localeMinusSign && ch != '+')
						couldBeNumeric = false;
				}
				else
					couldBeNumeric = false;
			}
		}

		if (couldBeNumeric && possibleExponentSeen != -1) {
			final int exponentLength = stopLooking - possibleExponentSeen - 1;
			if (exponentLength >= 5)
				couldBeNumeric = false;
			else {
				final int exponentSize = Integer.parseInt(trimmed.substring(possibleExponentSeen + 1, stopLooking));
				if (Math.abs(exponentSize) > 308)
					couldBeNumeric = false;
			}
		}

		final StringBuilder compressedl0 = new StringBuilder(length);
		if (alphasSeen != 0 && digitsSeen != 0 && alphasSeen + digitsSeen == length) {
			compressedl0.append(KnownPatterns.PATTERN_ALPHANUMERIC).append('{').append(String.valueOf(length)).append('}');
		} else if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
			compressedl0.append(KnownPatterns.PATTERN_BOOLEAN_TRUE_FALSE);
		} else if ("yes".equalsIgnoreCase(trimmed) || "no".equalsIgnoreCase(trimmed)) {
			compressedl0.append(KnownPatterns.PATTERN_BOOLEAN_YES_NO);
		} else if ("y".equalsIgnoreCase(trimmed) || "n".equalsIgnoreCase(trimmed)) {
			compressedl0.append(KnownPatterns.PATTERN_BOOLEAN_Y_N);
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
						compressedl0.append(last == 'd' ? "\\d" : KnownPatterns.PATTERN_ALPHA);
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

		// Check to see if this input is one of our registered Infinite Logical Types
		int c = 0;
		for (final LogicalTypeInfinite logical : infiniteTypes) {
			try {
				if ((matchPatternInfo == null || logical.acceptsBaseType(matchPatternInfo.getBaseType())) && logical.isCandidate(trimmed, compressedl0, charCounts, lastIndex))
					candidateCounts[c]++;
			}
			catch (Exception e) {
				logger.error("Plugin: %s, issue: %s.", logical.getQualifier(), e.getMessage());
			}
			c++;
		}

		// Create the level 1 and 2
		if (digitsSeen > 0 && couldBeNumeric && numericDecimalSeparators <= 1) {
			final StringBuilder[] result = determineNumericPattern(numericSigned, numericDecimalSeparators, possibleExponentSeen);
			escalation.level[1] = result[0];
			escalation.level[2] = result[1];
		} else {
			// Fast version of replaceAll("\\{\\d*\\}", "+"), e.g. replace \d{5} with \d+
			final StringBuilder collapsed = new StringBuilder(compressedl0);
			for (int i = 0; i < collapsed.length(); i++) {
				if (collapsed.charAt(i) == '{' && Character.isDigit(collapsed.charAt(i + 1))) {
					final int start = i++;
					while (collapsed.charAt(++i) != '}')
						/* EMPTY */;
					collapsed.replace(start, i + 1, "+");
				}
			}

			// Level 1 is the collapsed version e.g. convert \d{4}-\d{2}-\d{2] to
			// \d+-\d+-\d+
			final PatternInfo found = knownPatterns.getByRegExp(compressedl0.toString());
			if (found != null && found.generalPattern != null) {
				escalation.level[1] = new StringBuilder(found.generalPattern);
				escalation.level[2] = new StringBuilder(collapsed);
			} else {
				escalation.level[1] = new StringBuilder(collapsed);
				escalation.level[2] = new StringBuilder(KnownPatterns.PATTERN_ANY_VARIABLE);
			}
		}

		detectWindowEscalations.add(escalation);

		return matchPatternInfo != null && matchPatternInfo.getBaseType() != null;
	}

	private Map.Entry<String, Integer> getBest(final int levelIndex) {
		final Map<String, Integer> frequency = frequencies.get(levelIndex);

		// Grab the best and the second best based on frequency
		Map.Entry<String, Integer> best = null;
		Map.Entry<String, Integer> secondBest = null;
		Map.Entry<String, Integer> thirdBest = null;
		PatternInfo bestPattern = null;
		PatternInfo secondBestPattern = null;
		PatternInfo thirdBestPattern = null;
		String newKey = null;

		// Handle numeric promotion
		for (final Map.Entry<String, Integer> entry : frequency.entrySet()) {

			if (best == null) {
				best = entry;
				bestPattern = knownPatterns.getByRegExp(best.getKey());
			}
			else if (secondBest == null) {
				secondBest = entry;
				secondBestPattern = knownPatterns.getByRegExp(secondBest.getKey());
				if (levelIndex != 0 && bestPattern != null && secondBestPattern != null &&
						bestPattern.isNumeric() && secondBestPattern.isNumeric()) {
					newKey = knownPatterns.numericPromotion(bestPattern.regexp, secondBestPattern.regexp);
					if (newKey != null) {
						best = new AbstractMap.SimpleEntry<>(newKey, best.getValue() + secondBest.getValue());
						bestPattern = knownPatterns.getByRegExp(best.getKey());
					}
				}
			}
			else if (thirdBest == null) {
				thirdBest = entry;
				thirdBestPattern = knownPatterns.getByRegExp(thirdBest.getKey());
				if (levelIndex != 0 && bestPattern != null && thirdBestPattern != null &&
						bestPattern.isNumeric() && thirdBestPattern.isNumeric()) {
					newKey = knownPatterns.numericPromotion(newKey != null ? newKey : bestPattern.regexp, thirdBestPattern.regexp);
					if (newKey != null) {
						best = new AbstractMap.SimpleEntry<>(newKey, best.getValue() + thirdBest.getValue());
						bestPattern = knownPatterns.getByRegExp(best.getKey());
					}
				}
			}
		}

		// Promote almost anything to STRING, DOUBLES are pretty clear (unlike LONGS) so do not promote these
		if (bestPattern != null && secondBestPattern != null && !FTAType.DOUBLE.equals(bestPattern.getBaseType()))
			if (FTAType.STRING.equals(bestPattern.getBaseType()))
				best = new AbstractMap.SimpleEntry<>(best.getKey(), best.getValue() + secondBest.getValue());
			else if (secondBestPattern.id == KnownPatterns.ID.ID_ANY_VARIABLE)
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
				final String key = escalation.level[i].toString();
				final Integer seen = keyFrequency.get(key);
				if (seen == null)
					keyFrequency.put(key, entry.getValue());
				else
					keyFrequency.put(key, seen + entry.getValue());
			}

			// If it makes sense rewrite our sample data switching numeric/alpha matches to alphanumeric matches
			if (keyFrequency.size() > 1) {
				final Set<String> keys = new HashSet<>(keyFrequency.keySet());
				for (final String oldKey : keys) {
					String newKey = oldKey.replace(KnownPatterns.PATTERN_NUMERIC, KnownPatterns.PATTERN_ALPHANUMERIC);
					if (!newKey.equals(oldKey) && keys.contains(newKey)) {
						final int oldCount = keyFrequency.remove(oldKey);
						final int currentCount = keyFrequency.get(newKey);
						keyFrequency.put(newKey, currentCount + oldCount);
					} else {
						newKey = oldKey.replace(KnownPatterns.PATTERN_ALPHA, KnownPatterns.PATTERN_ALPHANUMERIC);
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
		if (analysisConfig.debug >= 2) {
			if (logger == null)
				logger = LoggerFactory.getLogger("fta");
			logger.debug(format, arguments);
		}
	}

	/**
	 * This is the core routine for determining the type of the field. It is
	 * responsible for setting matchPatternInfo.
	 */
	private void determineType() {
		if (sampleCount == 0) {
			matchPatternInfo = knownPatterns.getByRegExp(KnownPatterns.PATTERN_ANY_VARIABLE);
			return;
		}

		collapse();

		int level0value = 0;
		int level1value = 0;
		int level2value = 0;
		String level0pattern = null;
		String level1pattern = null;
		String level2pattern = null;
		PatternInfo level0patternInfo = null;
		PatternInfo level1patternInfo = null;
		PatternInfo level2patternInfo = null;
		final Map.Entry<String, Integer> level0 = getBest(0);
		final Map.Entry<String, Integer> level1 = getBest(1);
		final Map.Entry<String, Integer> level2 = getBest(2);
		Map.Entry<String, Integer> best = level0;

		if (level0 != null) {
			level0pattern = level0.getKey();
			level0value = level0.getValue();
			level0patternInfo = knownPatterns.getByRegExp(level0pattern);

			if (level0patternInfo == null) {
				for (final LogicalTypeRegExp logical : regExpTypes) {
					if (logical.isMatch(level0pattern)) {
						level0patternInfo = new PatternInfo(null, logical.getRegExp(), logical.getBaseType(), logical.getQualifier(), true, false, -1, -1, null, null);
						break;
					}
				}
			}
		}
		if (level1 != null) {
			level1pattern = level1.getKey();
			level1value = level1.getValue();
			level1patternInfo = knownPatterns.getByRegExp(level1pattern);
		}
		if (level2 != null) {
			level2pattern = level2.getKey();
			level2value = level2.getValue();
			level2patternInfo = knownPatterns.getByRegExp(level2pattern);
		}

		if (best != null) {
			matchPatternInfo = level0patternInfo;

			// Take any 'reasonable' (80%) level 1 with something we recognize or a better count
			if (level1 != null && (double)level1value/raw.size() >= 0.8 && (level0patternInfo == null || level1value > level0value)) {
				best = level1;
				matchPatternInfo = level1patternInfo;
			}

			// Take any level 2 if
			// - we have something we recognize (and we had nothing)
			// - we have the same key but a better count
			// - we have different keys but same type (signed versus not-signed)
			// - we have different keys, two numeric types and an improvement of at least 5%
			// - we have different keys, different types and an improvement of at least 10% and we are below the threshold
			if (level2 != null &&
					((matchPatternInfo == null && level2patternInfo != null)
					|| (best.getKey().equals(level2pattern) && level2value > best.getValue())
					|| (!best.getKey().equals(level2pattern) && level2patternInfo != null
							&& matchPatternInfo.getBaseType().equals(level2patternInfo.getBaseType())
							&& level2value > best.getValue())
					|| (!best.getKey().equals(level2pattern) && level2patternInfo != null
							&& matchPatternInfo.isNumeric()
							&& level2patternInfo.isNumeric()
							&& level2value >= 1.05 * best.getValue())
					|| (!best.getKey().equals(level2pattern) && (double)best.getValue()/raw.size() < (double)analysisConfig.getThreshold()/100
							&& level2value >= 1.10 * best.getValue()))) {
				matchPatternInfo = level2patternInfo;
			}

			if (possibleDateTime != 0 && possibleDateTime + 1 >= raw.size()) {

				// This next try/catch is unnecessary in theory, if there are zero bugs then it will never trip,
				// if there happens to be an issue then we swallow it and will not detect the date/datetime.
				try {
					final DateTimeParser det = new DateTimeParser(context.getDateResolutionMode(), locale);
					for (final String sample : raw)
						det.train(sample);

					final DateTimeParserResult result = det.getResult();
					final String formatString = result.getFormatString();
					matchPatternInfo = new PatternInfo(null, result.getRegExp(), result.getType(), formatString, false, false, -1, -1, null, formatString);
				}
				catch (RuntimeException e) {
				    debug("Internal error: {}", e);
				}
			}

			debug("Type determination - initial, matchPatternInfo - {}", matchPatternInfo);

			// Check to see if it might be one of the known Infinite Logical Types
			int i = 0;
			double bestConfidence = 0.0;
			for (final LogicalTypeInfinite logical : infiniteTypes) {
				if (logical.acceptsBaseType(matchPatternInfo.getBaseType()) && logical.getConfidence(candidateCounts[i], raw.size(), context.getStreamName())  >= logical.getThreshold()/100.0) {
					int count = 0;
					final PatternInfo candidate = new PatternInfo(null, logical.getRegExp(), logical.getBaseType(), logical.getQualifier(), true, false, -1, -1, null, null);
					for (final String sample : raw) {
						if (logical.acceptsBaseType(FTAType.STRING)) {
							if (trackString(sample, sample.trim(),  candidate, false))
								count++;
						}
						else if (logical.acceptsBaseType(FTAType.LONG)) {
							if (trackLong(sample.trim(), candidate, false))
								count++;
						}
						else if (logical.acceptsBaseType(FTAType.DOUBLE)) {
							if (trackDouble(sample, candidate, false))
								count++;
						}
						else if (candidate.isDateType()) {
							if (trackDateTime(sample, candidate, false))
								count++;
						}
					}

					// If a reasonable number look genuine then we are convinced
					final double currentConfidence = logical.getConfidence(count, raw.size(), context.getStreamName());
					if (currentConfidence > bestConfidence && currentConfidence >= logical.getThreshold()/100.0) {
						matchPatternInfo = candidate;
						bestConfidence = currentConfidence;
						debug("Type determination - infinite type, matchPatternInfo - {}", matchPatternInfo);
					}
				}
				i++;
			}

			// Try a regExp match nice and early - we can always back out
			for (final LogicalTypeRegExp logical : regExpTypes) {
				if (logical.acceptsBaseType(matchPatternInfo.getBaseType()) &&
						logical.isMatch(matchPatternInfo.regexp)) {
					matchPatternInfo = new PatternInfo(null, logical.getRegExp(), logical.getBaseType(), logical.getQualifier(), true, false, -1, -1, null, null);
					debug("Type determination - was '{}', matchPatternInfo - {}", matchPatternInfo.getBaseType(), matchPatternInfo);
					break;
				}
			}

			for (final String sample : raw)
				trackResult(sample, sample.trim(), false, 1);
		}
	}

	private void addValid(final String input, final long count) {
		final Long seen = cardinality.get(input);
		if (seen == null) {
			if (cardinality.size() < analysisConfig.getMaxCardinality())
				cardinality.put(input, count);
		}
		else
			cardinality.put(input, seen + count);
	}

	private int outlier(final String input) {
		final String cleaned = input.trim();
		final int trimmedLength = cleaned.length();

		if (trimmedLength < minTrimmedOutlierLength)
			minTrimmedOutlierLength = trimmedLength;
		if (trimmedLength > maxTrimmedOutlierLength)
			maxTrimmedOutlierLength = trimmedLength;

		if (minOutlierString == null || minOutlierString.compareTo(cleaned) > 0)
			minOutlierString = cleaned;

		if (maxOutlierString == null || maxOutlierString.compareTo(cleaned) < 0)
			maxOutlierString = cleaned;

		final String smashed = Token.generateKey(input);
		Long seen = outliersSmashed.get(smashed);
		if (seen == null) {
			if (outliersSmashed.size() < analysisConfig.getMaxOutliers())
				outliersSmashed.put(smashed, 1L);
		} else {
			outliersSmashed.put(smashed, seen + 1);
		}

		seen = outliers.get(input);
		if (seen == null) {
			if (outliers.size() < analysisConfig.getMaxOutliers())
				outliers.put(input, 1L);
		} else {
			outliers.put(input, seen + 1);
		}

		return outliers.size();
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

		OutlierAnalysis(final Map<String, Long> outliers,  final PatternInfo current) {
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

	private boolean conditionalBackoutToPattern(final long realSamples, final PatternInfo current) {
		final OutlierAnalysis analysis = new OutlierAnalysis(outliers, current);

		final long badCharacters = current.isAlphabetic() ? analysis.digits : analysis.alphas;
		// If we are currently Alphabetic and the only errors are digits then convert to AlphaNumeric
		if (badCharacters != 0 && analysis.spaces == 0 && analysis.other == 0 && current.isAlphabetic()) {
			if (outliers.size() == analysisConfig.getMaxOutliers() || analysis.digits > .01 * realSamples) {
				backoutToPatternID(realSamples, KnownPatterns.ID.ID_ALPHANUMERIC_VARIABLE);
				return true;
			}
		}
		// If we are currently Numeric and the only errors are alpha then convert to AlphaNumeric
		else if (badCharacters != 0 && analysis.spaces == 0 && analysis.other == 0 && FTAType.LONG.equals(current.getBaseType())) {
			if (outliers.size() == analysisConfig.getMaxOutliers() || analysis.alphas > .01 * realSamples) {
				backoutToPattern(realSamples, KnownPatterns.PATTERN_ALPHANUMERIC_VARIABLE);
				return true;
			}
		}
		// If we are currently Numeric and the only errors are doubles then convert to double
		else if (outliers.size() == analysis.doubles && FTAType.LONG.equals(current.getBaseType())) {
			KnownPatterns.ID id;
			if (analysis.exponent)
				id = analysis.negative ? KnownPatterns.ID.ID_SIGNED_DOUBLE_WITH_EXPONENT : KnownPatterns.ID.ID_DOUBLE_WITH_EXPONENT;
			else
				id = analysis.negative ? KnownPatterns.ID.ID_SIGNED_DOUBLE : KnownPatterns.ID.ID_DOUBLE;
			backoutToPatternID(realSamples, id);
			return true;
		}
		else if ((realSamples > reflectionSamples && outliers.size() == analysisConfig.getMaxOutliers())
					|| (badCharacters + analysis.nonAlphaNumeric) > .01 * realSamples) {
				backoutToPattern(realSamples, KnownPatterns.PATTERN_ANY_VARIABLE);
				return true;
		}

		return false;
	}

	private void backoutToPattern(final long realSamples, final String newPattern) {
		PatternInfo newPatternInfo = knownPatterns.getByRegExp(newPattern);

		// If it is not one of our known types then construct a suitable PatternInfo
		if (newPatternInfo == null)
			newPatternInfo = new PatternInfo(null, newPattern, FTAType.STRING, null, false, false, -1, -1, null, null);

		backoutToPatternInfo(realSamples, newPatternInfo);
	}

	private void backoutToPatternID(final long realSamples, final KnownPatterns.ID patternID) {
		backoutToPatternInfo(realSamples, knownPatterns.getByID(patternID));
	}

	private void backoutToString(final long realSamples) {
		matchCount = realSamples;

		// All outliers are now part of the cardinality set and there are now no outliers
		cardinality.putAll(outliers);

		final RegExpGenerator gen = new RegExpGenerator(MAX_ENUM_SIZE, locale);
		for (final String s : cardinality.keySet())
			gen.train(s);

		final String newPattern = gen.getResult();
		PatternInfo newPatternInfo = knownPatterns.getByRegExp(newPattern);

		// If it is not one of our known types then construct a suitable PatternInfo
		if (newPatternInfo == null)
			newPatternInfo = new PatternInfo(null, newPattern, FTAType.STRING, null, false, false, -1, -1, null, null);

		matchPatternInfo = newPatternInfo;

		for (final String s : cardinality.keySet())
			trackString(s, s.trim(), newPatternInfo, false);

		outliers.clear();
		outliersSmashed.clear();
	}

	private void backoutToPatternInfo(final long realSamples, final PatternInfo newPatternInfo) {
		matchCount = realSamples;
		matchPatternInfo = newPatternInfo;

		// All outliers are now part of the cardinality set and there are now no outliers
		cardinality.putAll(outliers);

		// Need to update stats to reflect any outliers we previously ignored
		if (matchPatternInfo.getBaseType().equals(FTAType.STRING)) {
			for (final String s : cardinality.keySet())
				trackString(s, s.trim(), newPatternInfo, false);
		}
		else if (matchPatternInfo.getBaseType().equals(FTAType.DOUBLE)) {
			facts.minDouble = facts.minLong;
			facts.maxDouble = facts.maxLong;
			for (final Map.Entry<String, Long> entry : outliers.entrySet()) {
				for (int i = 0; i < entry.getValue(); i++)
					trackDouble(entry.getKey(), matchPatternInfo, true);
			}
		}

		outliers.clear();
		outliersSmashed.clear();
		debug("Type determination - backing out, matchPatternInfo - {}", matchPatternInfo);
	}

	/**
	 * Backout from a mistaken logical type whose base type was long
	 * @param logical The Logical type we are backing out from
	 * @param realSamples The number of real samples we have seen.
	 */
	private void backoutLogicalLongType(final LogicalType logical, final long realSamples) {
		long otherLongs = 0;

		final Map<String, Long> outliersCopy = new HashMap<>(outliers);

		// Sweep the current outliers and check they are part of the set
		for (final Map.Entry<String, Long> entry : outliersCopy.entrySet()) {
			boolean isLong = true;
			try {
				Long.parseLong(entry.getKey());
			} catch (NumberFormatException e) {
				isLong = false;
			}

			if (isLong) {
				if (cardinality.size() < analysisConfig.getMaxCardinality())
					cardinality.put(entry.getKey(), entry.getValue());
				outliers.remove(entry.getKey(), entry.getValue());
				otherLongs += entry.getValue();
			}
		}

		matchCount += otherLongs;
		if ((double) matchCount / realSamples > analysisConfig.getThreshold()/100.0)
			matchPatternInfo = knownPatterns.getByID(KnownPatterns.ID.ID_LONG);
		else
			backoutToPatternID(realSamples, KnownPatterns.ID.ID_ANY_VARIABLE);
	}

	// Track basic facts for the field - called for any Valid input
	private void trackTrimmedLengthAndWhiteSpace(final String input, final String trimmed) {
		final int trimmedLength = trimmed.length();

		// Determine if there is leading or trailing White space (if not done previously)
		if (trimmedLength != 0) {
			if (!facts.leadingWhiteSpace)
				facts.leadingWhiteSpace = input.charAt(0) == ' ' || input.charAt(0) == '\t';
			if (!facts.trailingWhiteSpace) {
				final int length = input.length();
				facts.trailingWhiteSpace = input.charAt(length - 1) == ' ' || input.charAt(length - 1) == '\t';
			}
		}

		if (trimmedLength < minTrimmedLength)
			minTrimmedLength = trimmedLength;
		if (trimmedLength > maxTrimmedLength)
			maxTrimmedLength = trimmedLength;

		// Determine if this is a multi-line field (if not already decided)
		if (!facts.multiline)
			facts.multiline = input.indexOf('\n') != -1 || input.indexOf('\r') != -1;
	}

	/**
	 * Track the supplied raw input, once we have enough samples attempt to determine the type.
	 * @param input The raw input string
	 */
	private void trackResult(final String rawInput, final String trimmed, final boolean fromTraining, final long count) {
		if (fromTraining)
			trackLengthAndShape(rawInput, trimmed, count);

		// If the detect window cache is full and we have not determined a type compute one
		if ((matchPatternInfo == null || matchPatternInfo.getBaseType() == null) && sampleCount - (nullCount + blankCount) > analysisConfig.getDetectWindow())
			determineType();

		if (matchPatternInfo == null || matchPatternInfo.getBaseType() == null)
			return;

		final long realSamples = sampleCount - (nullCount + blankCount);
		boolean valid = false;

		final String input = rawInput.length() > getMaxInputLength() ? rawInput.substring(0, getMaxInputLength()) : rawInput;

		switch (matchPatternInfo.getBaseType()) {
		case BOOLEAN:
			if (trackBoolean(trimmed)) {
				matchCount += count;
				addValid(input, count);
				valid = true;
			}
			break;

		case LONG:
			if (trackLong(trimmed, matchPatternInfo, true)) {
				matchCount += count;
				addValid(input, count);
				valid = true;
			}
			break;

		case DOUBLE:
			if (trackDouble(input, matchPatternInfo, true)) {
				matchCount += count;
				addValid(input, count);
				valid = true;
			}
			break;

		case STRING:
			if (trackString(input, trimmed, matchPatternInfo, true)) {
				matchCount += count;
				addValid(input, count);
				valid = true;
			}
			break;

		case LOCALDATE:
		case LOCALTIME:
		case LOCALDATETIME:
		case OFFSETDATETIME:
		case ZONEDDATETIME:
			try {
				trackDateTime(input, matchPatternInfo, true);
				matchCount += count;
				addValid(input, count);
				valid = true;
			}
			catch (DateTimeParseException reale) {
				// The real parse threw an Exception, this does not give us enough facts to usefully determine if there are any
				// improvements to our assumptions we could make to do better, so re-parse and handle our more nuanced exception
				DateTimeParserResult result = DateTimeParserResult.asResult(matchPatternInfo.format, context.getDateResolutionMode(), locale);
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
							final int offset = matchPatternInfo.format.indexOf(ditch);

							// S is a special case (unlike H, H, M, d) and is *NOT* handled by the default DateTimeFormatter.ofPattern
							if (ditch == 'S') {
								// The input is shorter than we expected so set the minimum length to 1 and then update
								final int len = result.timeFieldLengths[FRACTION_INDEX].getPatternLength();
								result.timeFieldLengths[FRACTION_INDEX].setMin(1);
								newFormatString = Utils.replaceAt(matchPatternInfo.format, offset, len,
										result.timeFieldLengths[FRACTION_INDEX].getPattern('S'));
							}
							else
								newFormatString = new StringBuffer(matchPatternInfo.format).deleteCharAt(offset).toString();

							updated = true;
						}
						else if (e.getMessage().equals("Expecting end of input, extraneous input found, last token (FRACTION)")) {
							final int offset = matchPatternInfo.format.indexOf('S');
							final int oldLength = result.timeFieldLengths[FRACTION_INDEX].getPatternLength();
							result.timeFieldLengths[FRACTION_INDEX].set(result.timeFieldLengths[FRACTION_INDEX].getMin(),
									result.timeFieldLengths[FRACTION_INDEX].getMax() + 1);
							newFormatString = Utils.replaceAt(matchPatternInfo.format, offset, oldLength,
									result.timeFieldLengths[FRACTION_INDEX].getPattern('S'));
							updated = true;
						}
						else if (e.getMessage().equals("Invalid value for hours: 24 (expected 0-23)")) {
							final int offset = matchPatternInfo.format.indexOf('H');
							newFormatString = Utils.replaceAt(matchPatternInfo.format, offset, result.timeFieldLengths[HOUR_INDEX].getMin(),
									result.timeFieldLengths[HOUR_INDEX].getMin() == 1 ? "k" : "kk");
							updated = true;
						}

						if (!updated)
							break;

						result = DateTimeParserResult.asResult(newFormatString, context.getDateResolutionMode(), locale);
						matchPatternInfo = new PatternInfo(null, result.getRegExp(), matchPatternInfo.getBaseType(), newFormatString, false, false, -1, -1, null, newFormatString);
					}
				} while (!success);

				try {
					trackDateTime(input, matchPatternInfo, true);
					matchCount += count;
					addValid(input, count);
					valid = true;
				}
				catch (DateTimeParseException eIgnore) {
					// Ignore and record as outlier below
				}
			}
			break;
		}

		if (valid)
			trackTrimmedLengthAndWhiteSpace(rawInput, trimmed);
		else {
			outlier(input);
			if (!matchPatternInfo.isDateType() && outliers.size() == analysisConfig.getMaxOutliers()) {
				if (matchPatternInfo.isLogicalType()) {
					// Do we need to back out from any of our Infinite type determinations
					final LogicalType logical = plugins.getRegistered(matchPatternInfo.typeQualifier);
					final String newPattern = logical.isValidSet(context, matchCount, realSamples, matchPatternInfo.regexp, null, cardinality, outliers, tokenStreams, analysisConfig);
					if (newPattern != null)
						if (FTAType.LONG.equals(matchPatternInfo.getBaseType()) && matchPatternInfo.typeQualifier != null)
							backoutLogicalLongType(logical, realSamples);
						else if (FTAType.STRING.equals(matchPatternInfo.getBaseType()) && matchPatternInfo.typeQualifier != null)
							backoutToPattern(realSamples, newPattern);
				}
				else {

					// Need to evaluate if we got this wrong
					conditionalBackoutToPattern(realSamples, matchPatternInfo);
				}
			}
		}
	}

	/**
	 * Determine if the current dataset reflects a logical type.
	 * @param logical The Logical type we are testing
	 * @return A MatchResult that indicates the quality of the match against the provided data
	 */
	private FiniteMatchResult checkFiniteSet(final Map<String, Long> cardinalityUpper, final Map<String, Long> outliers, final LogicalTypeFinite logical) {
		final long realSamples = sampleCount - (nullCount + blankCount);
		long missCount = 0;				// count of number of misses

		final Map<String, Long> newOutliers = new HashMap<>();
		final Map<String, Long> addMatches = new HashMap<>();
		final Map<String, Long> minusMatches = new HashMap<>();
		final double missThreshold = 1.0 - logical.getThreshold()/100.0;
		long validCount = 0;

		for (final Map.Entry<String, Long> entry : outliers.entrySet()) {
			final String upper = entry.getKey().toUpperCase(Locale.ENGLISH);
			if (logical.isValid(upper)) {
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

		for (final Map.Entry<String, Long> entry : cardinalityUpper.entrySet()) {
			if (logical.isValid(entry.getKey()))
				validCount += entry.getValue();
			else {
				missCount += entry.getValue();
				minusMatches.put(entry.getKey(), entry.getValue());
				newOutliers.put(entry.getKey(), entry.getValue());
			}
		}

		final Map<String, Long> newCardinality = new HashMap<>(cardinalityUpper);
		newCardinality.putAll(addMatches);
		for (final String elt : minusMatches.keySet())
			newCardinality.remove(elt);

		if (logical.isValidSet(context, validCount, realSamples, matchPatternInfo.regexp, null, newCardinality, newOutliers, tokenStreams, analysisConfig) != null)
			return new FiniteMatchResult();

		return new FiniteMatchResult(logical, logical.getConfidence(validCount, realSamples, context.getStreamName()), validCount, newOutliers, newCardinality);
	}

	private String lengthQualifier(final int min, final int max) {
		if (!lengthQualifier)
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
			result.replace(idx, idx + 1, lengthQualifier(minTrimmedLengthNumeric, maxTrimmedLengthNumeric)).toString();
	}


	private class FiniteMatchResult {
		LogicalTypeFinite logical;
		double score;
		Map<String, Long> newOutliers;
		Map<String, Long> newCardinality;
		long validCount;
		boolean isMatch;

		boolean matched() {
			return isMatch;
		}

		FiniteMatchResult(final LogicalTypeFinite logical, final double score, final long validCount, final Map<String, Long> newOutliers, final Map<String, Long> newCardinality) {
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

	private LogicalTypeFinite matchFiniteTypes(final Map<String, Long> cardinalityUpper, final int minKeyLength, final int maxKeyLength) {
		FiniteMatchResult bestResult = null;
		double bestScore = -1.0;

		for (final LogicalTypeFinite logical : finiteTypes)
			if ((!logical.isClosed() || cardinalityUpper.size() <= logical.getSize() + 2 + logical.getSize()/20)) {
				final FiniteMatchResult result = checkFiniteSet(cardinalityUpper, outliers, logical);

				if (!result.matched() || result.score < bestScore)
					continue;

				// Choose the best score
				if (result.score > bestScore ||
						// If two scores the same then prefer the one with the higher header confidence
						logical.getHeaderConfidence(context.getStreamName()) > bestResult.logical.getHeaderConfidence(context.getStreamName()) ||
						// If two scores the same then prefer the logical with the highest priority
						(logical.getHeaderConfidence(context.getStreamName()) == bestResult.logical.getHeaderConfidence(context.getStreamName()) &&
						logical.getPriority() < bestResult.logical.getPriority())) {
					bestResult = result;
					bestScore = result.score;
				}
			}

		if (bestResult == null)
			return null;

		outliers = bestResult.newOutliers;
		cardinality = bestResult.newCardinality;
		matchCount = bestResult.validCount;
		matchPatternInfo = new PatternInfo(null, bestResult.logical.getRegExp(), FTAType.STRING, bestResult.logical.getQualifier(), true, false, -1, -1, null, null);

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
	private final static int LATE_LONG_YYYYMMDD = 20410101;

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
		if (matchPatternInfo == null)
			determineType();

		// Compute our confidence
		final long realSamples = sampleCount - (nullCount + blankCount);
		double confidence;

		// Check to see if we are all blanks or all nulls
		if (blankCount == sampleCount || nullCount == sampleCount || blankCount + nullCount == sampleCount) {
			if (nullCount == sampleCount)
				matchPatternInfo = knownPatterns.getByID(KnownPatterns.ID.ID_NULL);
			else if (blankCount == sampleCount)
				matchPatternInfo = knownPatterns.getByID(KnownPatterns.ID.ID_BLANK);
			else
				matchPatternInfo = knownPatterns.getByID(KnownPatterns.ID.ID_BLANKORNULL);
			confidence = sampleCount >= 10 ? 1.0 : 0.0;
		}
		else {
			confidence = (double) matchCount / realSamples;
		}

		boolean backedOutRegExp = false;

		// Do we need to back out from any of our Logical type determinations.  Most of the time this backs out of
		// Infinite type determinations (since we have not yet declared it to be a Finite type).  However it is possible
		// that this is a subsequent call to getResult()!!
		if (matchPatternInfo.isLogicalType()) {
			final LogicalType logical = plugins.getRegistered(matchPatternInfo.typeQualifier);

			String newPattern;
			if ((newPattern = logical.isValidSet(context, matchCount, realSamples, matchPatternInfo.regexp, null, cardinality, outliers, tokenStreams, analysisConfig)) != null) {
				if (logical.acceptsBaseType(FTAType.STRING) || logical.acceptsBaseType(FTAType.LONG)) {
					if (logical.acceptsBaseType(FTAType.STRING))
						backoutToPattern(realSamples, newPattern);
					else
						backoutLogicalLongType(logical, realSamples);
					confidence = (double) matchCount / realSamples;
					if (logical instanceof LogicalTypeRegExp)
						backedOutRegExp = true;
				}
			}
			else {
				// Update our Regular Expression - since it may have changed based on all the data observed
				matchPatternInfo.regexp = logical.getRegExp();
				confidence = logical.getConfidence(matchCount, realSamples, context.getStreamName());
			}
		}

		Map<String, Long> cardinalityUpper = new HashMap<>();

		if (KnownPatterns.ID.ID_LONG == matchPatternInfo.id || KnownPatterns.ID.ID_SIGNED_LONG == matchPatternInfo.id) {
			if (KnownPatterns.ID.ID_LONG == matchPatternInfo.id && matchPatternInfo.typeQualifier == null && facts.minLong < 0)
				matchPatternInfo = knownPatterns.getByID(KnownPatterns.ID.ID_SIGNED_LONG);

			// Sometimes a Long is not a Long but it is really a date
			if (groupingSeparators == 0 && minLongNonZero != Long.MAX_VALUE && minLongNonZero > EARLY_LONG_YYYYMMDD && facts.maxLong < LATE_LONG_YYYYMMDD &&
					DateTimeParser.plausibleDateCore(false, (int)minLongNonZero%100, ((int)minLongNonZero/100)%100, (int)minLongNonZero/10000, 4)  &&
					DateTimeParser.plausibleDateCore(false, (int)facts.maxLong%100, ((int)facts.maxLong/100)%100, (int)facts.maxLong/10000, 4)  &&
					((realSamples >= reflectionSamples && cardinality.size() > 10) || context.getStreamName().toLowerCase(locale).contains("date"))) {
				matchPatternInfo = new PatternInfo(null, (minLongNonZero == facts.minLong || tokenStreams.size() == 1) ? "\\d{8}" : "0|\\d{8}", FTAType.LOCALDATE, "yyyyMMdd", false, false, 8, 8, null, "yyyyMMdd");
				final DateTimeFormatter dtf = DateTimeParser.ofPattern(matchPatternInfo.format, locale);
				facts.minLocalDate = LocalDate.parse(String.valueOf(minLongNonZero), dtf);
				facts.maxLocalDate = LocalDate.parse(String.valueOf(facts.maxLong), dtf);

				// If we are collecting statistics - we need to generate the topK and bottomK
				if (analysisConfig.isCollectStatistics())
					for (final String s : cardinality.keySet())
						if (!Utils.allZeroes(s))
							trackDateTime(s, matchPatternInfo, true);
			} else if (groupingSeparators == 0 && minLongNonZero != Long.MAX_VALUE && minLongNonZero > 1800 && facts.maxLong < 2041 &&
					((realSamples >= reflectionSamples && cardinality.size() > 10) || context.getStreamName().toLowerCase(locale).contains("year") || context.getStreamName().toLowerCase(locale).contains("date"))) {
				matchPatternInfo = new PatternInfo(null, minLongNonZero == facts.minLong ? "\\d{4}" : "0+|\\d{4}", FTAType.LOCALDATE, "yyyy", false, false, 4, 4, null, "yyyy");
				facts.minLocalDate = LocalDate.of((int)minLongNonZero, 1, 1);
				facts.maxLocalDate = LocalDate.of((int)facts.maxLong, 1, 1);

				// If we are collecting statistics - we need to generate the topK and bottomK
				if (analysisConfig.isCollectStatistics())
					for (final String s : cardinality.keySet())
						if (Integer.valueOf(s) != 0)
							trackDateTime(s, matchPatternInfo, true);
			} else if (cardinality.size() == 2 && facts.minLong == 0 && facts.maxLong == 1) {
				// boolean by any other name
				facts.minBoolean = "0";
				facts.maxBoolean = "1";
				matchPatternInfo = knownPatterns.getByID(KnownPatterns.ID.ID_BOOLEAN_ONE_ZERO);
			} else {
				if (groupingSeparators != 0)
					matchPatternInfo = knownPatterns.grouping(matchPatternInfo.regexp);

				matchPatternInfo = new PatternInfo(matchPatternInfo);
				matchPatternInfo.regexp = freezeNumeric(matchPatternInfo.regexp);

				for (final LogicalTypeRegExp logical : regExpTypes) {
					if (logical.acceptsBaseType(FTAType.LONG) &&
							logical.isMatch(matchPatternInfo.regexp) &&
							logical.isValidSet(context, matchCount, realSamples, matchPatternInfo.regexp, facts.calculateFacts(matchPatternInfo, matchCount), cardinality, outliers, tokenStreams, analysisConfig) == null) {
						matchPatternInfo = new PatternInfo(null, logical.getRegExp(), logical.getBaseType(), logical.getQualifier(), true, false, -1, -1, null, null);
						confidence = logical.getConfidence(matchCount, realSamples, context.getStreamName());
						debug("Type determination - was LONG, matchPatternInfo - {}", matchPatternInfo);
						break;
					}
				}

				if (!matchPatternInfo.isLogicalType() && realSamples >= analysisConfig.getDetectWindow() &&
						(confidence < analysisConfig.getThreshold()/100.0 ||
								(numericWidening && !outliers.isEmpty() && (new OutlierAnalysis(outliers, matchPatternInfo)).doubles == outliers.size()))) {
					// We thought it was an integer field, but on reflection it does not feel like it
					conditionalBackoutToPattern(realSamples, matchPatternInfo);
					confidence = (double) matchCount / realSamples;
				}
			}
		} else if (FTAType.BOOLEAN.equals(matchPatternInfo.getBaseType()) && matchPatternInfo.id == KnownPatterns.ID.ID_BOOLEAN_Y_N && cardinality.size() == 1) {
			backoutToString(realSamples);
			confidence = (double) matchCount / realSamples;
		} else if (FTAType.DOUBLE.equals(matchPatternInfo.getBaseType()) && !matchPatternInfo.isLogicalType()) {
			if (facts.minDouble < 0.0)
				matchPatternInfo = knownPatterns.negation(matchPatternInfo.regexp);

			if (groupingSeparators != 0)
				matchPatternInfo = knownPatterns.grouping(matchPatternInfo.regexp);

			for (final LogicalTypeRegExp logical : regExpTypes) {
				if (logical.acceptsBaseType(FTAType.DOUBLE) &&
						logical.isMatch(matchPatternInfo.regexp) &&
						logical.isValidSet(context, matchCount, realSamples, matchPatternInfo.regexp, facts.calculateFacts(matchPatternInfo, matchCount), cardinality, outliers, tokenStreams, analysisConfig) == null) {
					matchPatternInfo = new PatternInfo(null, logical.getRegExp(), logical.getBaseType(), logical.getQualifier(), true, false, -1, -1, null, null);
					confidence = logical.getConfidence(matchCount, realSamples, context.getStreamName());
					break;
				}
			}
		} else if (FTAType.STRING.equals(matchPatternInfo.getBaseType()) && !matchPatternInfo.isLogicalType()) {
			// Build Cardinality map ignoring case (and white space)
			int minKeyLength = Integer.MAX_VALUE;
			int maxKeyLength = 0;
			for (final Map.Entry<String, Long> entry : cardinality.entrySet()) {
				final String key = entry.getKey().toUpperCase(locale).trim();
				final int keyLength = key.length();
				if (keyLength < minKeyLength)
					minKeyLength = keyLength;
				if (keyLength > maxKeyLength)
					maxKeyLength = keyLength;
				final Long seen = cardinalityUpper.get(key);
				if (seen == null) {
					cardinalityUpper.put(key, entry.getValue());
				} else
					cardinalityUpper.put(key, seen + entry.getValue());
			}
			// Sort the results so that we consider the most frequent first (we will hopefully fail faster)
			cardinalityUpper = Utils.sortByValue(cardinalityUpper);

			final LogicalTypeFinite logical = matchFiniteTypes(cardinalityUpper, minKeyLength, maxKeyLength);
			if (logical != null)
				confidence = logical.getConfidence(matchCount, realSamples, context.getStreamName());

			// Fixup any likely enums
			if (matchPatternInfo.typeQualifier == null && cardinalityUpper.size() < MAX_ENUM_SIZE && !outliers.isEmpty() && outliers.size() < 10) {
				boolean updated = false;

				final Set<String> killSet = new HashSet<>();

				// Sort the outliers so that we consider the most frequent first
				outliers = Utils.sortByValue(outliers);

				// Iterate through the outliers adding them to the core cardinality set if we think they are reasonable.
				for (final Map.Entry<String, Long> entry : outliers.entrySet()) {
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
						final Long value = cardinalityUpper.get(keyUpper);
						if (value == null)
							cardinalityUpper.put(keyUpper, entry.getValue());
						else
							cardinalityUpper.put(keyUpper, value + entry.getValue());
						killSet.add(key);
						updated = true;
					}
				}

				// If we updated the set then we need to remove the outliers we OK'd and
				// also update the pattern to reflect the looser definition
				if (updated) {
					final Map<String, Long> remainingOutliers = new HashMap<>();
					remainingOutliers.putAll(outliers);
					for (final String elt : killSet)
						remainingOutliers.remove(elt);

					backoutToPattern(realSamples, KnownPatterns.PATTERN_ANY_VARIABLE);
					confidence = (double) matchCount / realSamples;
					outliers = remainingOutliers;
				}
			}

			// Need to evaluate if we got the type wrong
			if (matchPatternInfo.typeQualifier == null && !outliers.isEmpty() && matchPatternInfo.isAlphabetic() && realSamples >= reflectionSamples) {
				conditionalBackoutToPattern(realSamples, matchPatternInfo);
				confidence = (double) matchCount / realSamples;

				// Rebuild the cardinalityUpper Map
				cardinalityUpper.clear();
				for (final Map.Entry<String, Long> entry : cardinality.entrySet()) {
					final String key = entry.getKey().toUpperCase(locale).trim();
					final Long seen = cardinalityUpper.get(key);
					if (seen == null) {
						cardinalityUpper.put(key, entry.getValue());
					} else
						cardinalityUpper.put(key, seen + entry.getValue());
				}
			}
		}

		// We would really like to say something better than it is a String!
		if (FTAType.STRING.equals(matchPatternInfo.getBaseType()) && matchPatternInfo.typeQualifier == null) {
			boolean updated = false;

			// If we are currently matching everything then flip to a better Regular Expression based on Stream analysis if possible
			if (matchCount == realSamples && !tokenStreams.isAnyShape()) {
				final String newRegExp = tokenStreams.getRegExp(false);
				if (newRegExp != null) {
					matchPatternInfo = new PatternInfo(null, newRegExp, FTAType.STRING, matchPatternInfo.typeQualifier, false, false,
							minTrimmedLength, maxTrimmedLength, null, null);
				}
			}

			if (!backedOutRegExp)
				for (final LogicalTypeRegExp logical : regExpTypes) {
					if (logical.acceptsBaseType(FTAType.STRING) &&
							(logical.isMatch(matchPatternInfo.regexp) || tokenStreams.matches(logical.getRegExp())) &&
							logical.isValidSet(context, matchCount, realSamples, matchPatternInfo.regexp, facts.calculateFacts(matchPatternInfo, matchCount), cardinality, outliers, tokenStreams, analysisConfig) == null) {
						matchPatternInfo = new PatternInfo(null, logical.getRegExp(), logical.getBaseType(), logical.getQualifier(), true, false, -1, -1, null, null);
						confidence = logical.getConfidence(matchCount, realSamples, context.getStreamName());
						updated = true;
						break;
					}
				}

			final long interestingSamples = sampleCount - (nullCount + blankCount);

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
								cardinality.entrySet()
								  .removeIf(entry -> entry.getKey().equalsIgnoreCase(elt.getKey()));
								outliers.put(elt.getKey(), elt.getValue());
								matchCount -= elt.getValue();
								confidence = (double) matchCount / realSamples;
							}
						}

						// Regenerate the enum without the outliers removed
						gen = new RegExpGenerator(MAX_ENUM_SIZE, locale);
						for (final String elt : cardinalityUpper.keySet())
							gen.train(elt);
					}

					matchPatternInfo = new PatternInfo(null, gen.getResult(), FTAType.STRING, matchPatternInfo.typeQualifier, false, false,
							minTrimmedLength, maxTrimmedLength, null, null);
					updated = true;

					// Now we have mapped to an enum we need to check again if this should be matched to a logical type
					for (final LogicalTypeRegExp logical : regExpTypes) {
						if (logical.acceptsBaseType(FTAType.STRING) &&
								logical.isMatch(matchPatternInfo.regexp) &&
								logical.isValidSet(context, matchCount, realSamples, matchPatternInfo.regexp, facts.calculateFacts(matchPatternInfo, matchCount), cardinality, outliers, tokenStreams, analysisConfig) == null) {
							matchPatternInfo = new PatternInfo(null, logical.getRegExp(), logical.getBaseType(), logical.getQualifier(), true, false, -1, -1, null, null);
							confidence = logical.getConfidence(matchCount, realSamples, context.getStreamName());
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
							logical.isValidSet(context, best.getOccurrences(), realSamples, matchPatternInfo.regexp, facts.calculateFacts(matchPatternInfo, matchCount), cardinality, outliers, tokenStreams, analysisConfig) == null) {
						matchPatternInfo = new PatternInfo(null, regExp, logical.getBaseType(), logical.getQualifier(), true, false, -1, -1, null, null);
						matchCount = best.getOccurrences();
						confidence = logical.getConfidence(matchCount, realSamples, context.getStreamName());
						updated = true;
						break;
					}
				}
			}

			// Qualify Alpha or Alnum with a min and max length
			if (!updated && (KnownPatterns.PATTERN_ALPHA_VARIABLE.equals(matchPatternInfo.regexp) || KnownPatterns.PATTERN_ALPHANUMERIC_VARIABLE.equals(matchPatternInfo.regexp))) {
				String newPattern = matchPatternInfo.regexp;
				newPattern = newPattern.substring(0, newPattern.length() - 1) + lengthQualifier(minTrimmedLength, maxTrimmedLength);
				matchPatternInfo = new PatternInfo(null, newPattern, FTAType.STRING, matchPatternInfo.typeQualifier, false, false,
						minTrimmedLength, maxTrimmedLength, null, null);
				updated = true;
			}

			// Qualify random string with a min and max length
			if (!updated && KnownPatterns.PATTERN_ANY_VARIABLE.equals(matchPatternInfo.regexp)) {
				final String newPattern = KnownPatterns.freezeANY(minTrimmedLength, maxTrimmedLength, minRawNonBlankLength, maxRawNonBlankLength, facts.leadingWhiteSpace, facts.trailingWhiteSpace, facts.multiline);
				matchPatternInfo = new PatternInfo(null, newPattern, FTAType.STRING, matchPatternInfo.typeQualifier, false, false,
						facts.minRawLength, facts.maxRawLength, null, null);
				updated = true;
			}
		}

		// Only attempt to do key identification if we have not already been told the answer
		if (facts.keyConfidence == null) {
			// Attempt to identify keys?
			facts.keyConfidence = 0.0;
			if (sampleCount > MIN_SAMPLES_FOR_KEY && analysisConfig.getMaxCardinality() >= MIN_SAMPLES_FOR_KEY / 2 &&
					(cardinality.size() == analysisConfig.getMaxCardinality() || cardinality.size() == sampleCount) &&
					blankCount == 0 && nullCount == 0 &&
					((matchPatternInfo.typeQualifier != null && matchPatternInfo.typeQualifier.equals("GUID")) ||
					(matchPatternInfo.typeQualifier == null &&
					((FTAType.STRING.equals(matchPatternInfo.getBaseType()) && facts.minRawLength == facts.maxRawLength && facts.minRawLength < 32)
							|| FTAType.LONG.equals(matchPatternInfo.getBaseType()))))) {
				facts.keyConfidence = 0.9;

				if (cardinality.size() == analysisConfig.getMaxCardinality())
					// Might be a key but only iff every element in the cardinality
					// set only has a count of 1
					for (final Map.Entry<String, Long> entry : cardinality.entrySet()) {
						if (entry.getValue() != 1) {
							facts.keyConfidence = 0.0;
							break;
						}
					}
			}
		}

		// Only attempt to do uniqueness if we have not already been told the answer
		if (facts.uniqueness == null) {
			if (cardinality.isEmpty())
				facts.uniqueness = 0.0;
			// Can only generate uniqueness if we have not overflowed Max Cardinality
			else if (cardinality.size() < analysisConfig.getMaxCardinality()) {
				int uniques = 0;
				for (final Map.Entry<String, Long> entry : cardinality.entrySet()) {
					if (entry.getValue() == 1)
						uniques++;
				}
				facts.uniqueness = (double)uniques/cardinality.size();
			}
			else if (FTAType.LONG.equals(matchPatternInfo.getBaseType()) && (monotonicIncreasing || monotonicDecreasing)) {
				facts.uniqueness = 1.0;
			}
			else
				// -1 indicates we have no perspective on the uniqueness of this field
				facts.uniqueness = -1.0;
		}

		final TextAnalysisResult result = new TextAnalysisResult(context.getStreamName(), locale,
				matchCount, totalCount, sampleCount, nullCount, blankCount,
				matchPatternInfo, facts.calculateFacts(matchPatternInfo, matchCount), confidence, context.getDateResolutionMode(),
				analysisConfig, cardinality, outliers, tokenStreams);

		if (traceConfig != null)
			traceConfig.recordResult(result, internalErrors);

		return result;
	}

	/**
	 * Access the training set - this will typically be the first {@link AnalysisConfig#DETECT_WINDOW_DEFAULT} records.
	 *
	 * @return A List of the raw input strings.
	 */
	public List<String>getTrainingSet() {
		return raw;
	}
}
