/*
 * Copyright 2017-2020 Tim Segall
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
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
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
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

import com.cobber.fta.DateTimeParser.DateResolutionMode;

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

	/** The default value for the number of samples to collect before making a type determination. */
	public static final int DETECT_WINDOW_DEFAULT = 20;
	private int detectWindow = DETECT_WINDOW_DEFAULT;

	/** Should we collect statistics (min, max, sum) as we parse the data stream. */
	private boolean collectStatistics = true;

	/** Internal-only debugging flag. */
	private int debug = 0;

	/** Should we enable Default Logical Type detection. */
	private boolean enableDefaultLogicalTypes = true;

	/** Should we attempt to qualifier the size of the returned RexExp. */
	private boolean lengthQualifier = true;

	/** Plugin Threshold for detection - by default this is unset and sensible defaults are used. */
	private int pluginThreshold = -1;

	/** The default value for the detection threshold. */
	public static final int DETECTION_THRESHOLD_DEFAULT = 95;
	private int threshold = DETECTION_THRESHOLD_DEFAULT;

	/** Enable Numeric widening - i.e. if we see lots of integers then some doubles call it a double. */
	private boolean numericWidening = true;

	private Locale locale = Locale.getDefault();

	/** The default value for the maximum Cardinality tracked. */
	public static final int MAX_CARDINALITY_DEFAULT = 12000;
	private int maxCardinality = MAX_CARDINALITY_DEFAULT;

	/** We need to see at least this many samples (all unique) before we will claim this is a possible key. */
	private static final int MIN_SAMPLES_FOR_KEY = 1000;

	/** The default value for the maximum # of outliers tracked. */
	public static final int MAX_OUTLIERS_DEFAULT = 50;
	private int maxOutliers = MAX_OUTLIERS_DEFAULT;

	/** The maximum number of shapes tracked. */
	public static final int MAX_SHAPES_DEFAULT = 400;
	private int maxShapes = MAX_SHAPES_DEFAULT;

	/** We are prepared to recognize any set of this size as an enum (and give a suitable regular expression). */
	private static final int MAX_ENUM_SIZE = 40;

	protected static final int REFLECTION_SAMPLES = 30;
	private int reflectionSamples = REFLECTION_SAMPLES;

	private String dataStreamName;
	private DateResolutionMode resolutionMode = DateResolutionMode.None;
	private char decimalSeparator;
	private char utilizedDecimalSeparator = '.';
	private NumberFormat longFormatter;
	private NumberFormat doubleFormatter;
	private char groupingSeparator;
	private char minusSign;
	private char negativePrefix;
	private boolean hasNegativePrefix;
	private char negativeSuffix;
	private boolean hasNegativeSuffix;
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

	private Shapes shapes = new Shapes(MAX_SHAPES_DEFAULT);

	class Escalation {
		StringBuilder[] level;

		@Override
		public int hashCode() {
			return level[0].toString().hashCode() + 7 * level[1].toString().hashCode() + 11 * level[2].toString().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Escalation other = (Escalation) obj;
			if (!Arrays.equals(level, other.level))
				return false;
			return true;
		}

		Escalation() {
			level = new StringBuilder[3];
		}
	}

	private ArrayList<Escalation> levels;
	List<Map<String, Integer>> frequencies = new ArrayList<>();

	private long matchCount;
	private PatternInfo matchPatternInfo;

	private boolean trainingStarted;
	private boolean initialized;

	private boolean multiline = false;
	private boolean leadingWhiteSpace = false;
	private boolean trailingWhiteSpace = false;

	private double minDouble = Double.MAX_VALUE;
	private double maxDouble = -Double.MAX_VALUE;
	private BigDecimal sumBD = BigDecimal.ZERO;
	private TopBottomK<Double, Double> tbDouble = new TopBottomK<>();

	private long minLong = Long.MAX_VALUE;
	private long minLongNonZero = Long.MAX_VALUE;
	private long maxLong = Long.MIN_VALUE;
	private BigInteger sumBI = BigInteger.ZERO;
	private TopBottomK<Long, Long> tbLong = new TopBottomK<>();

	private String minString;
	private String maxString;
	private TopBottomK<String, String> tbString = new TopBottomK<>();

	private String minOutlierString;
	private String maxOutlierString;

	private String minBoolean;
	private String maxBoolean;

	private LocalTime minLocalTime;
	private LocalTime maxLocalTime;
	private TopBottomK<LocalTime, LocalTime> tbLocalTime = new TopBottomK<>();

	private LocalDate minLocalDate;
	private LocalDate maxLocalDate;
	private TopBottomK<LocalDate, ChronoLocalDate> tbLocalDate = new TopBottomK<>();

	private LocalDateTime minLocalDateTime;
	private LocalDateTime maxLocalDateTime;
	private TopBottomK<LocalDateTime, ChronoLocalDateTime<?>> tbLocalDateTime = new TopBottomK<>();

	private ZonedDateTime minZonedDateTime;
	private ZonedDateTime maxZonedDateTime;
	private TopBottomK<ZonedDateTime, ChronoZonedDateTime<?>> tbZonedDateTime = new TopBottomK<>();

	private OffsetDateTime minOffsetDateTime;
	private OffsetDateTime maxOffsetDateTime;
	private TopBottomK<OffsetDateTime, OffsetDateTime> tbOffsetDateTime = new TopBottomK<>();

	// The minimum length (not trimmed)
	private int minRawLength = Integer.MAX_VALUE;
	// The maximum length (not trimmed)
	private int maxRawLength = Integer.MIN_VALUE;

	// The minimum length (not trimmed) - but must be non-Blank
	private int minRawNonBlankLength = Integer.MAX_VALUE;
	// The maximum length (not trimmed) - but must be non-Blank
	private int maxRawNonBlankLength = Integer.MIN_VALUE;

	private int minTrimmedLength = Integer.MAX_VALUE;
	private int maxTrimmedLength = Integer.MIN_VALUE;

	private int minTrimmedOutlierLength = Integer.MAX_VALUE;
	private int maxTrimmedOutlierLength = Integer.MIN_VALUE;

	private int possibleDateTime;
	private long totalLeadingZeros;
	private long groupingSeparators;

	private ArrayList<LogicalTypeInfinite> infiniteTypes = new ArrayList<>();
	private ArrayList<LogicalTypeFinite> finiteTypes = new ArrayList<>();
	private ArrayList<LogicalTypeRegExp> regExpTypes = new ArrayList<>();
	private int[] candidateCounts;

	private KnownPatterns knownPatterns = new KnownPatterns();

	private DateTimeParser dateTimeParser;

	private Plugins plugins = new Plugins();

	/**
	 * Construct a Text Analyzer for the named data stream.  Note: The resolution mode will be 'None'.
	 *
	 * @param name The name of the data stream (e.g. the column of the CSV file)
	 */
	public TextAnalyzer(final String name) {
		this(name, DateResolutionMode.None);
	}

	/**
	 * Construct an anonymous Text Analyzer for a data stream.  Note: The resolution mode will be 'None'.
	 */
	public TextAnalyzer() {
		this(null, DateResolutionMode.None);
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
		this.dataStreamName = name == null ? "anonymous" : name;
		this.resolutionMode = resolutionMode;
	}

	/**
	 * Get the name of the Data Stream.
	 *
	 * @return The name of the Data Stream.
	 */
	public String getStreamName() {
		return dataStreamName;
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
		this.collectStatistics = collectStatistics;
		return ret;
	}

	/**
	 * Internal Only.  Enable internal debugging.
	 *
	 * @param debug The debug level.
	 */
	public void setDebug(int debug) {
		this.debug = debug;
	}

	/**
	 * Indicates whether to collect statistics or not.
	 *
	 * @return Whether Statistics collection is enabled.
	 */
	public boolean getCollectStatistics() {
		return collectStatistics;
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
		this.enableDefaultLogicalTypes = logicalTypeDetection;
		return ret;
	}

	/**
	 * Indicates whether to enable default Logical Type processing or not.
	 *
	 * @return Whether default Logical Type processing collection is enabled.
	 */
	public boolean getDefaultLogicalTypes() {
		return enableDefaultLogicalTypes;
	}

	/**
	 * The percentage when we declare success 0 - 100.
	 * Typically this should not be adjusted, if you want to run in Strict mode then set this to 100.
	 * @param threshold The new threshold for detection.
	 */
	public void setThreshold(int threshold) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot adjust Threshold once training has started");

		if (threshold <= 0 || threshold > 100)
			throw new IllegalArgumentException("Threshold must be between 0 and 100");

		this.threshold = threshold;
	}

	/**
	 * Get the current detection Threshold.
	 *
	 * @return The current threshold.
	 */
	public int getThreshold() {
		return threshold;
	}

	/**
	 * The percentage when we declare success 0 - 100 for Logical Type plugins.
	 * Typically this should not be adjusted, if you want to run in Strict mode then set this to 100.
	 * @param threshold The new threshold used for detection.
	 */
	public void setPluginThreshold(int threshold) {
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
	 * If true enable Numeric widening - i.e. if we see lots of integers then some doubles call it a double.
	 * @param numericWidening The new value for numericWidening.
	 */
	public void setNumericWidening(boolean numericWidening) {
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
	 * @param locale The new Locale used to determine separators in numbers, date processing, default plugins, etc.
	 * Note: There is no support for Locales that do not use the Gregorian Calendar.
	 */
	public void setLocale(Locale locale) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot adjust Locale once training has started");

		this.locale = locale;
	}

    /**
     * Set the size of the Detect Window (i.e. number of samples) to collect before attempting to determine the
     * type. Note: It is not possible to change the Sample Size once training
     * has started.
     *
     * @param detectWindow
     *            The number of samples to collect
     * @return The previous value of this parameter.
	*/
	public int setDetectWindow(final int detectWindow) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot change size of detect window once training has started");
		if (detectWindow < DETECT_WINDOW_DEFAULT)
			throw new IllegalArgumentException("Cannot set detect window size below " + DETECT_WINDOW_DEFAULT);

		final int ret = detectWindow;
		this.detectWindow = detectWindow;

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
		return detectWindow;
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
	 * Note:
	 *  - The Cardinality must be larger than the Cardinality of the largest Finite Logical type.
	 *  - It is not possible to change the cardinality once training has started.
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

		final int ret = maxCardinality;
		maxCardinality = newCardinality;
		return ret;
	}

	/**
	 * Get the maximum cardinality that will be tracked. See
	 * {@link #setMaxCardinality(int) setMaxCardinality()} method.
	 *
	 * @return The maximum cardinality.
	 */
	public int getMaxCardinality() {
		return maxCardinality;
	}

	/**
	 * Set the maximum number of outliers that will be tracked. Note: It is not
	 * possible to change the outlier count once training has started.
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

		final int ret = maxOutliers;
		maxOutliers = newMaxOutliers;
		return ret;
	}

	/**
	 * Get the maximum number of outliers that will be tracked. See
	 * {@link #setMaxOutliers(int) setMaxOutliers()} method.
	 *
	 * @return The maximum cardinality.
	 */
	public int getMaxOutliers() {
		return maxOutliers;
	}

	/**
	 * Indicate whether we should qualify the size of the RegExp.
	 * For example "\d{3,6}" vs. "\d+"
	 * Note: This only impacts simple Numerics/Alphas/AlphaNumerics.
	 *
	 * @param newLengthQualifier The new value.

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

	String getRegExp(KnownPatterns.ID id) {
		return knownPatterns.getRegExp(id);
	}

	// Track basic facts for the field - called for all input
	void trackLengthAndShape(final String input, long count) {
		// We always want to track basic facts for the field
		final int length = input.length();

		if (length != 0 && length < minRawLength)
			minRawLength = length;
		if (length > maxRawLength)
			maxRawLength = length;

		String trimmed = input.trim();
		if (trimmed.length() != 0) {
			if (length != 0 && length < minRawNonBlankLength)
				minRawNonBlankLength = length;
			if (length > maxRawNonBlankLength)
				maxRawNonBlankLength = length;

			shapes.track(trimmed, count);
		}
	}

	private boolean trackLong(final String rawInput, PatternInfo patternInfo, final boolean register) {
		final String input = rawInput.trim();

		// Track String facts - just in case we end up backing out.
		if (minString == null || minString.compareTo(input) > 0)
			minString = input;

		if (maxString == null || maxString.compareTo(input) < 0)
			maxString = input;

		long l;

		// Interpret the String as a long, first attempt uses parseLong which is fast (although not localized), if that fails,
		// then try using a NumberFormatter which will cope with grouping separators (e.g. 1,000).
		int digits = input.length();

		try {
			if (patternInfo.id == KnownPatterns.ID.ID_SIGNED_LONG_TRAILING) {
				digits = input.length();
				if (digits >= 2 && input.charAt(digits - 1) == '-') {
					l = -Long.parseLong(input.substring(0, digits - 1));
					digits--;
				}
				else
					l = Long.parseLong(input);
			}
			else {
				l = Long.parseLong(input);
				digits = input.length();
				char ch = input.charAt(0);
				if (ch == '-' || ch == '+')
					digits--;
			}
		} catch (NumberFormatException e) {
			ParsePosition pos = new ParsePosition(0);
			Number n = longFormatter.parse(input, pos);
			if (n == null || input.length() != pos.getIndex())
				return false;
			l = n.longValue();
			if (input.indexOf(groupingSeparator) != -1)
				groupingSeparators++;
			digits = input.length();
			char ch = input.charAt(0);
			if (hasNegativePrefix && (ch == '-' || ch == '+' || ch == negativePrefix))
				digits--;
			if (l < 0 && hasNegativeSuffix)
				digits--;
		}

		if (register) {
			if (input.charAt(0) == '0' && digits != 1)
				totalLeadingZeros++;

			if (digits < minTrimmedLength)
				minTrimmedLength = digits;
			if (digits > maxTrimmedLength)
				maxTrimmedLength = digits;

			if (l != 0 && l < minLongNonZero)
				minLongNonZero = l;

			if (l < minLong)
				minLong = l;

			if (l > maxLong)
				maxLong = l;

			if (collectStatistics) {
				sumBI = sumBI.add(BigInteger.valueOf(l));
				tbLong.observe(l);
			}
		}

		if (patternInfo.isLogicalType()) {
			// If it is a registered Infinite Logical Type then validate it
			LogicalType logical = plugins.getRegistered(patternInfo.typeQualifier);
			if (PatternInfo.Type.LONG.equals(logical.getBaseType()))
				return logical.isValid(input);
		}

		return true;
	}

	private boolean trackBoolean(final String input) {
		final String trimmedLower = input.trim().toLowerCase(locale);

		final boolean isTrue = "true".equals(trimmedLower) || "yes".equals(trimmedLower) || "y".equals(trimmedLower);
		final boolean isFalse = !isTrue && ("false".equals(trimmedLower) || "no".equals(trimmedLower) || "n".equals(trimmedLower));

		if (isTrue) {
			if (minBoolean == null)
				minBoolean = trimmedLower;
			if (maxBoolean == null || "false".equals(maxBoolean) || "no".equals(maxBoolean) || "n".equals(maxBoolean))
				maxBoolean = trimmedLower;
		} else if (isFalse) {
			if (maxBoolean == null)
				maxBoolean = trimmedLower;
			if (minBoolean == null || "true".equals(minBoolean) || "yes".equals(maxBoolean) || "y".equals(maxBoolean))
				minBoolean = trimmedLower;
		}

		return isTrue || isFalse;
	}

	private boolean trackString(final String rawInput, PatternInfo patternInfo, final boolean register) {
		if (register && debug >= 2 && rawInput.length() > 0 && rawInput.charAt(0) == '¶' && rawInput.equals("¶ xyzzy ¶"))
			throw new NullPointerException("¶ xyzzy ¶");
		if (patternInfo.typeQualifier == null) {
			String trimmed = rawInput.trim();
			for (int i = 0; i < trimmed.length(); i++) {
				if (patternInfo.isAlphabetic() && !Character.isAlphabetic(trimmed.charAt(i)))
					return false;
				if (patternInfo.isAlphanumeric() && !Character.isLetterOrDigit((trimmed.charAt(i))))
					return false;
			}
		}
		else if (patternInfo.isLogicalType) {
			// If it is a registered Infinite Logical Type then validate it
			LogicalType logical = plugins.getRegistered(patternInfo.typeQualifier);
			if (PatternInfo.Type.STRING.equals(logical.getBaseType()) && !logical.isValid(rawInput))
				return false;
		}

		return updateStats(rawInput);
	}

	private boolean updateStats(final String cleaned) {
		final int len = cleaned.trim().length();

		if (matchPatternInfo.minLength != -1 && len < matchPatternInfo.minLength)
			return false;
		if (matchPatternInfo.maxLength != -1 && len > matchPatternInfo.maxLength)
			return false;

		if (minString == null || minString.compareTo(cleaned) > 0)
			minString = cleaned;

		if (maxString == null || maxString.compareTo(cleaned) < 0)
			maxString = cleaned;

		if (len < minTrimmedLength)
			minTrimmedLength = len;
		if (len > maxTrimmedLength)
			maxTrimmedLength = len;

		if (collectStatistics)
			tbString.observe(cleaned);

		return true;
	}

	private boolean trackDouble(final String rawInput, PatternInfo patternInfo, final boolean register) {
		final String input = rawInput.trim();
		double d;

		try {
			if (patternInfo.id == KnownPatterns.ID.ID_SIGNED_DOUBLE_TRAILING) {
				int digits = input.length();
				if (digits >= 2 && input.charAt(digits - 1) == '-')
					d = -Double.parseDouble(input.substring(0, digits - 1));
				else
					d = Double.parseDouble(input);
			}
			else
				d = Double.parseDouble(input);
		} catch (NumberFormatException e) {
			ParsePosition pos = new ParsePosition(0);
			Number n = doubleFormatter.parse(input, pos);
			if (n == null || input.length() != pos.getIndex())
				return false;
			d = n.doubleValue();
			if (input.indexOf(groupingSeparator) != -1)
				groupingSeparators++;
			if (decimalSeparator != '.' && utilizedDecimalSeparator != decimalSeparator && input.indexOf(decimalSeparator) != -1)
				utilizedDecimalSeparator = decimalSeparator;
		}

		if (patternInfo.isLogicalType()) {
			// If it is a registered Infinite Logical Type then validate it
			LogicalType logical = plugins.getRegistered(patternInfo.typeQualifier);
			if (PatternInfo.Type.DOUBLE.equals(logical.getBaseType()) && !logical.isValid(input))
				return false;
		}

		// If it is NaN/Infinity then we are all done
		if (Double.isNaN(d) || Double.isInfinite(d))
			return true;

		if (register && collectStatistics) {
			if (d < minDouble)
				minDouble = d;

			if (d > maxDouble)
				maxDouble = d;

			sumBD = sumBD.add(BigDecimal.valueOf(d));
			tbDouble.observe(d);
		}

		return true;
	}

	/*
	 * Validate (and track) the date/time/datetime input.
	 * This routine is called for every date/time/datetime we see in the input, so performance is critical.
	 */
	private boolean trackDateTime(final String input, PatternInfo patternInfo, final boolean register) {
		String dateFormat = patternInfo.format;

		// Retrieve the (likely cached) DateTimeParserResult for the supplied dateFormat
		final DateTimeParserResult result = DateTimeParserResult.asResult(dateFormat, resolutionMode, locale);
		if (result == null)
			throw new InternalErrorException("NULL result for " + dateFormat);

		DateTimeFormatter formatter = DateTimeParser.ofPattern(result.getFormatString(), locale);

		final String trimmed = input.trim();

		// If we are not collecting statistics we can use the parse on DateTimeParserResult which is
		// significantly faster than the parse on LocalTime/LocalDate/LocalDateTime/...
		switch (result.getType()) {
		case LOCALTIME:
			if (register && collectStatistics) {
				final LocalTime localTime = LocalTime.parse(trimmed, formatter);
				if (minLocalTime == null || localTime.compareTo(minLocalTime) < 0)
					minLocalTime = localTime;
				if (maxLocalTime == null || localTime.compareTo(maxLocalTime) > 0)
					maxLocalTime = localTime;
				tbLocalTime.observe(localTime);
			}
			else
				result.parse(trimmed);
			break;

		case LOCALDATE:
			if (register && collectStatistics) {
				final LocalDate localDate = LocalDate.parse(trimmed, formatter);
				if (minLocalDate == null || localDate.compareTo(minLocalDate) < 0)
					minLocalDate = localDate;
				if (maxLocalDate == null || localDate.compareTo(maxLocalDate) > 0)
					maxLocalDate = localDate;
				tbLocalDate.observe(localDate);
			}
			else
				result.parse(trimmed);
			break;

		case LOCALDATETIME:
			if (register && collectStatistics) {
				final LocalDateTime localDateTime = LocalDateTime.parse(trimmed, formatter);
				if (minLocalDateTime == null || localDateTime.compareTo(minLocalDateTime) < 0)
					minLocalDateTime = localDateTime;
				if (maxLocalDateTime == null || localDateTime.compareTo(maxLocalDateTime) > 0)
					maxLocalDateTime = localDateTime;
				tbLocalDateTime.observe(localDateTime);
			}
			else
				result.parse(trimmed);
			break;

		case ZONEDDATETIME:
			if (register && collectStatistics) {
				final ZonedDateTime zonedDateTime = ZonedDateTime.parse(trimmed, formatter);
				if (minZonedDateTime == null || zonedDateTime.compareTo(minZonedDateTime) < 0)
					minZonedDateTime = zonedDateTime;
				if (maxZonedDateTime == null || zonedDateTime.compareTo(maxZonedDateTime) > 0)
					maxZonedDateTime = zonedDateTime;
				tbZonedDateTime.observe(zonedDateTime);
			}
			else
				result.parse(trimmed);
			break;

		case OFFSETDATETIME:
			if (register && collectStatistics) {
				final OffsetDateTime offsetDateTime = OffsetDateTime.parse(trimmed, formatter);
				if (minOffsetDateTime == null || offsetDateTime.compareTo(minOffsetDateTime) < 0)
					minOffsetDateTime = offsetDateTime;
				if (maxOffsetDateTime == null || offsetDateTime.compareTo(maxOffsetDateTime) > 0)
					maxOffsetDateTime = offsetDateTime;
				tbOffsetDateTime.observe(offsetDateTime);
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
	 *
	 * @param locale The Locale used for analysis, the will impact both the set of plugins registered as well as the behavior of the individual plugins
	 *
	 * Note: If the locale is null it will default to the Default locale.
	 */
	public void registerDefaultPlugins(Locale locale) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/plugins.json")))) {
			plugins.registerPlugins(reader, dataStreamName, locale);
		} catch (Exception e) {
			throw new IllegalArgumentException("Internal error: Issues with plugins file: " + e.getMessage(), e);
		}
	}

	private void initialize() {
		Calendar cal = Calendar.getInstance(locale);
		if (!(cal instanceof GregorianCalendar))
			throw new IllegalArgumentException("No support for locales that do not use the Gregorian Calendar");

		if (!NumberFormat.getNumberInstance(locale).format(0).matches("\\d"))
			throw new IllegalArgumentException("No support for locales that do not use Arabic numerals");

		raw = new ArrayList<>(detectWindow);
		levels = new ArrayList<>(detectWindow);

		// If enabled, load the default set of plugins for Logical Type detection
		if (enableDefaultLogicalTypes)
			registerDefaultPlugins(locale);

		for (LogicalType logical : plugins.getRegisteredLogicalTypes()) {

			if ((logical instanceof LogicalTypeFinite) && ((LogicalTypeFinite)logical).getSize() + 10 > getMaxCardinality())
				throw new IllegalArgumentException("Internal error: Max Cardinality: " + getMaxCardinality() + " is insufficient to support plugin: " + logical.getQualifier());

			if (logical instanceof LogicalTypeInfinite)
				infiniteTypes.add((LogicalTypeInfinite)logical);
			else if (logical instanceof LogicalTypeFinite)
				finiteTypes.add((LogicalTypeFinite)logical);
			else
				regExpTypes.add((LogicalTypeRegExp)logical);

			if (pluginThreshold != -1)
				logical.setThreshold(pluginThreshold);
		}

		Collections.sort(infiniteTypes);
		Collections.sort(finiteTypes);
		Collections.sort(regExpTypes);

		candidateCounts = new int[infiniteTypes.size()];

		longFormatter = NumberFormat.getIntegerInstance(locale);
		doubleFormatter = NumberFormat.getInstance(locale);

		DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);
		decimalSeparator = formatSymbols.getDecimalSeparator();
		groupingSeparator = formatSymbols.getGroupingSeparator();
		minusSign = formatSymbols.getMinusSign();
		NumberFormat simple = NumberFormat.getNumberInstance(locale);
		if (simple instanceof DecimalFormat) {
			String signFacts = ((DecimalFormat) simple).getNegativePrefix();
			if (signFacts.length() > 1)
				throw new IllegalArgumentException("No support for locales with multi-character sign prefixes");
			hasNegativePrefix = !signFacts.isEmpty();
			if (hasNegativePrefix)
				negativePrefix = signFacts.charAt(0);
			signFacts = ((DecimalFormat) simple).getNegativeSuffix();
			if (signFacts.length() > 1)
				throw new IllegalArgumentException("No support for locales with multi-character sign suffixes");
			hasNegativeSuffix = !signFacts.isEmpty();
			if (hasNegativeSuffix)
				negativeSuffix = signFacts.charAt(0);
		}
		else {
			String signFacts = String.valueOf(formatSymbols.getMinusSign());
			hasNegativePrefix = true;
			negativePrefix = signFacts.charAt(0);
			hasNegativeSuffix = false;
		}

		knownPatterns.initialize(locale);

		// If Resolution mode is auto then set DayFirst or MonthFirst based on the Locale
		if (resolutionMode == DateResolutionMode.Auto) {
			DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, locale);
			String pattern = ((SimpleDateFormat)df).toPattern();
			int dayIndex = pattern.indexOf('d');
			int monthIndex = pattern.indexOf('M');
			if (dayIndex == -1 || monthIndex == -1)
				throw new IllegalArgumentException("Failed to determine DateResolutionMode for this locale");
			// We assume that if Day is before Month, then Day is also before Year!
			resolutionMode = dayIndex < monthIndex ? DateResolutionMode.DayFirst : DateResolutionMode.MonthFirst;
		}

		dateTimeParser = new DateTimeParser(resolutionMode, locale);

		initialized = true;
	}

	StringBuilder[]
	determineNumericPattern(SignStatus signStatus, int numericDecimalSeparators, int possibleExponentSeen) {
		StringBuilder[] result = new StringBuilder[2];

		if (signStatus == SignStatus.TRAILING_MINUS) {
			result[0] = result[1] = new StringBuilder(numericDecimalSeparators == 1 ? knownPatterns.PATTERN_SIGNED_DOUBLE_TRAILING : knownPatterns.PATTERN_SIGNED_LONG_TRAILING);
			return result;
		}

		boolean numericSigned = signStatus == SignStatus.LOCALE_STANDARD || signStatus == SignStatus.LEADING_SIGN;

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
		Observation(String observed, long count, long used) {
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
	 */
	public void trainBulk(Map<String, Long> observed) {
		// Sort so we have the most frequent first
		observed = Utils.sortByValue(observed);
		Observation[] facts = new Observation[observed.size()];
		int i = 0;
		long total = 0;

		// Setup the array of observations and calculate the total number of observations
		for (Map.Entry<String, Long> entry : observed.entrySet()) {
			facts[i++] = new Observation(entry.getKey(), entry.getValue(), 0);
			total += entry.getValue();
		}

		// Each element in the array has the probability that an observation is in this location or an earlier one
		long running = 0;
		for (int f = 0; f < facts.length; f++) {
			running += facts[f].count;
			facts[f].percentage = (double)running/total;
		}

		Random choose = new Random(271828);

		// First send in a random set of samples until we are trained
		boolean trained = false;
		for (int j = 0; j < total && !trained; j++) {
			double index = choose.nextDouble();
			for (int f = 0; f < facts.length; f++) {
				if (index < facts[f].percentage && facts[f].used < facts[f].count) {
					if (train(facts[f].observed))
						trained = true;
					facts[f].used++;
					break;
				}
			}
		}

		// Now send in the rest of the samples in bulk
		for (int f = 0; f < facts.length; f++) {
			long remaining = facts[f].count - facts[f].used;
			if (remaining != 0)
				trainBulkCore(facts[f].observed, remaining);
		}
	}

	private void trainBulkCore(final String rawInput, long count) {
		sampleCount += count;

		if (rawInput == null) {
			nullCount += count;
			return;
		}

		final String trimmed = rawInput.trim();

		final int length = trimmed.length();

		if (length == 0) {
			blankCount += count;
			trackLengthAndShape(rawInput, count);
			return;
		}

		// This next try/catch is unnecessary in theory, if there are zero bugs then it will never trip,
		// if there happens to be an issue then we will lose this training event.
		try {
			trainCore(rawInput, trimmed, length, count);
		}
		catch (RuntimeException e) {
			if (debug != 0) {
				System.err.println("Internal error: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Train is the core streaming entry point used to supply input to the Text Analyzer.
	 *
	 * @param rawInput
	 *            The raw input as a String
	 * @return A boolean indicating if the resultant type is currently known.
	 */
	public boolean train(final String rawInput) {
		// Initialize if we have not already done so
		if (!initialized) {
			initialize();
			trainingStarted = true;
		}

		sampleCount++;

		PatternInfo.Type matchType = matchPatternInfo != null ? matchPatternInfo.type : null;

		if (rawInput == null) {
			nullCount++;
			return matchType != null;
		}

		final String trimmed = rawInput.trim();

		final int length = trimmed.length();

		if (length == 0) {
			blankCount++;
			trackLengthAndShape(rawInput, 1);
			return matchType != null;
		}

		// This next try/catch is unnecessary in theory, if there are zero bugs then it will never trip,
		// if there happens to be an issue then we will lose this training event.
		boolean result;
		try {
			result = trainCore(rawInput, trimmed, length, 1);
		}
		catch (RuntimeException e) {
			if (debug != 0) {
				System.err.println("Internal error: " + e.getMessage());
				e.printStackTrace();
			}
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

	private boolean trainCore(final String rawInput, String trimmed, final int length, long count) {

		trackResult(rawInput, true, count);

		// If we have determined a type, no need to further train
		if (matchPatternInfo != null && matchPatternInfo.type != null)
			return true;

		raw.add(rawInput);

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
			char ch = trimmed.charAt(i);

			// Track counts and last occurrence for simple characters
			if (ch <= 127) {
				charCounts[ch]++;
				lastIndex[ch] = i;
			}

			if ((ch == minusSign || ch == '+') && i == 0)
				numericSigned = SignStatus.LEADING_SIGN;
			else if (!hasNegativeSuffix && numericSigned == SignStatus.NONE && ch == '-' && i == stopLooking - 1) {
				numericSigned = SignStatus.TRAILING_MINUS;
			} else if (Character.isDigit(ch)) {
				l0.append('d');
				digitsSeen++;
			} else if (ch == decimalSeparator) {
				l0.append('D');
				numericDecimalSeparators++;
			} else if (ch == groupingSeparator) {
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
					if (ch != minusSign && ch != '+')
						couldBeNumeric = false;
				}
				else
					couldBeNumeric = false;
			}
		}

		if (couldBeNumeric && possibleExponentSeen != -1) {
			int exponentLength = stopLooking - possibleExponentSeen - 1;
			if (exponentLength >= 5)
				couldBeNumeric = false;
			else {
				int exponentSize = Integer.parseInt(trimmed.substring(possibleExponentSeen + 1, stopLooking));
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
				if (ch == last) {
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
		Escalation escalation = new Escalation();
		escalation.level[0] = compressedl0;

		if (dateTimeParser.determineFormatString(trimmed) != null)
			possibleDateTime++;

		// Check to see if this input is one of our registered Infinite Logical Types
		int c = 0;
		for (LogicalTypeInfinite logical : infiniteTypes) {
			try {
				if (logical.isCandidate(trimmed, compressedl0, charCounts, lastIndex))
					candidateCounts[c]++;
			}
			catch (Exception e) {
				System.err.printf("Plugin: %s, issue: %s%n", logical.getQualifier(), e.getMessage());
			}
			c++;
		}

		// Create the level 1 and 2
		if (digitsSeen > 0 && couldBeNumeric && numericDecimalSeparators <= 1) {
			StringBuilder[] result = determineNumericPattern(numericSigned, numericDecimalSeparators, possibleExponentSeen);
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

		levels.add(escalation);

		return matchPatternInfo != null && matchPatternInfo.type != null;
	}

	private Map.Entry<String, Integer> getBest(final int levelIndex) {
		Map<String, Integer> frequency = frequencies.get(levelIndex);

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

		if (bestPattern != null && secondBestPattern != null && PatternInfo.Type.STRING.equals(bestPattern.type)) {
			// Promote anything to STRING
			best = new AbstractMap.SimpleEntry<>(best.getKey(),
					best.getValue() + secondBest.getValue());
		}

		return best;
	}

	private void collapse() {
		// Map from Escalation hash to count of occurrences
		final Map<Integer, Integer> observedFrequency = new HashMap<>();
		// Map from Escalation hash to Escalation
		Map<Integer, Escalation> observedSet = new HashMap<>();

		// Calculate the frequency of every element
		for (final Escalation e : levels) {
			int hash = e.hashCode();
			final Integer seen = observedFrequency.get(hash);
			if (seen == null) {
				observedFrequency.put(hash, 1);
				observedSet.put(hash, e);
			} else {
				observedFrequency.put(hash, seen + 1);
			}
		}

		for (int i = 0; i < 3; i++) {
			Map<String, Integer> keyFrequency = new HashMap<>();
			for (Map.Entry<Integer, Integer> entry : observedFrequency.entrySet()) {
				Escalation escalation = observedSet.get(entry.getKey());
				String key = escalation.level[i].toString();
				final Integer seen = keyFrequency.get(key);
				if (seen == null)
					keyFrequency.put(key, entry.getValue());
				else
					keyFrequency.put(key, seen + entry.getValue());
			}

			// If it makes sense rewrite our sample data switching numeric matches to alphanumeric matches
			if (keyFrequency.size() > 1) {
				final Set<String> keys = new HashSet<>(keyFrequency.keySet());
				for (String oldKey : keys) {
					String newKey = oldKey.replace(KnownPatterns.PATTERN_NUMERIC, KnownPatterns.PATTERN_ALPHANUMERIC);
					if (!newKey.equals(oldKey) && keys.contains(newKey)) {
						int oldCount = keyFrequency.remove(oldKey);
						int currentCount = keyFrequency.get(newKey);
						keyFrequency.put(newKey, currentCount + oldCount);
					}
				}
			}
			frequencies.add(Utils.sortByValue(keyFrequency));
		}
	}

	/**
	 * This is the core routine for determining the type of the field. It is
	 * responsible for setting: - matchPattern - matchPatternInfo - matchCount -
	 * type
	 */
	private void determineType() {
		// If we have fewer than 6 samples do not even pretend
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
				for (LogicalTypeRegExp logical : regExpTypes) {
					if (logical.getRegExp().equals(level0pattern)) {
						level0patternInfo = new PatternInfo(null, logical.getRegExp(), logical.getBaseType(), logical.getQualifier(), true, -1, -1, null, null);
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
			// - we have different keys but same type (signed vs. not-signed)
			// - we have different keys, two numeric types and an improvement of at least 5%
			// - we have different keys, different types and an improvement of at least 10%
			if (level2 != null &&
					((matchPatternInfo == null && level2patternInfo != null)
					|| (best.getKey().equals(level2pattern) && level2value > best.getValue())
					|| (!best.getKey().equals(level2pattern) && level2patternInfo != null
							&& matchPatternInfo.type.equals(level2patternInfo.type)
							&& level2.getValue() > best.getValue())
					|| (!best.getKey().equals(level2pattern) && level2patternInfo != null
							&& matchPatternInfo.isNumeric()
							&& level2patternInfo.isNumeric()
							&& (double)level2.getValue() >= 1.05 * best.getValue())
					|| (!best.getKey().equals(level2pattern)
							&& (double)level2.getValue() >= 1.10 * best.getValue()))) {
				matchPatternInfo = level2patternInfo;
			}

			if (possibleDateTime != 0 && possibleDateTime + 1 >= raw.size()) {
				final DateTimeParser det = new DateTimeParser(resolutionMode, locale);
				for (final String sample : raw)
					det.train(sample);

				final DateTimeParserResult result = det.getResult();
				final String formatString = result.getFormatString();
				matchPatternInfo = new PatternInfo(null, result.getRegExp(), result.getType(), formatString, false, -1, -1, null, formatString);
			}

			// Check to see if it might be one of the known Infinite Logical Types
			int i = 0;
			double bestConfidence = 0.0;
			for (LogicalTypeInfinite logical : infiniteTypes) {
				if ((double)candidateCounts[i]/raw.size() >= logical.getThreshold()/100.0) {
					int count = 0;
					PatternInfo candidate = new PatternInfo(null, logical.getRegExp(), logical.getBaseType(), logical.getQualifier(), true, -1, -1, null, null);
					for (final String sample : raw) {
						if (PatternInfo.Type.STRING.equals(logical.getBaseType())) {
							if (trackString(sample, candidate, false))
								count++;
						}
						else if (PatternInfo.Type.LONG.equals(logical.getBaseType())) {
							if (trackLong(sample, candidate, false))
								count++;
						}
						else if (PatternInfo.Type.DOUBLE.equals(logical.getBaseType())) {
							if (trackDouble(sample, candidate, false))
								count++;
						} if (candidate.isDateType()) {
							if (trackDateTime(sample, candidate, false))
								count++;
						}
					}

					// If a reasonable number look genuine then we are convinced
					double currentConfidence = logical.getConfidence(count, raw.size(), dataStreamName);
					if (currentConfidence > bestConfidence && currentConfidence >= logical.getThreshold()/100.0) {
						matchPatternInfo = candidate;
						bestConfidence = currentConfidence;
					}
				}
				i++;
			}

			for (final String sample : raw)
				trackResult(sample, false, 1);
		}
	}

	private void addValid(final String input, long count) {
		final Long seen = cardinality.get(input);
		if (seen == null) {
			if (cardinality.size() < maxCardinality)
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

		String smashed = Smashed.smash(input);
		Long seen = outliersSmashed.get(smashed);
		if (seen == null) {
			if (outliersSmashed.size() < maxOutliers)
				outliersSmashed.put(smashed, 1L);
		} else {
			outliersSmashed.put(smashed, seen + 1);
		}

		seen = outliers.get(input);
		if (seen == null) {
			if (outliers.size() < maxOutliers)
				outliers.put(input, 1L);
		} else {
			outliers.put(input, seen + 1);
		}

		return outliers.size();
	}

	class OutlierAnalysis {
		int alphas = 0;
		int digits = 0;
		int spaces = 0;
		int other = 0;
		int doubles = 0;
		int nonAlphaNumeric = 0;
		boolean negative = false;
		boolean exponent = false;

		private boolean isDouble(final String input) {
			try {
				Double.parseDouble(input.trim());
			} catch (NumberFormatException e) {
				ParsePosition pos = new ParsePosition(0);
				Number n = doubleFormatter.parse(input, pos);
				if (n == null || input.length() != pos.getIndex())
					return false;
			}
			return true;
		}

		OutlierAnalysis(Map<String, Long> outliers,  PatternInfo current) {
			// Sweep the current outliers
			for (final Map.Entry<String, Long> entry : outliers.entrySet()) {
				String key = entry.getKey();
				Long value = entry.getValue();
				if (PatternInfo.Type.LONG.equals(current.type) && isDouble(key)) {
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
				int len = key.length();
				for (int i = 0; i < len; i++) {
					Character c = key.charAt(i);
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

	private boolean conditionalBackoutToPattern(final long realSamples, PatternInfo current) {
		OutlierAnalysis analysis = new OutlierAnalysis(outliers, current);

		int badCharacters = current.isAlphabetic() ? analysis.digits : analysis.alphas;
		// If we are currently Alphabetic and the only errors are digits then convert to AlphaNumeric
		if (badCharacters != 0 && analysis.spaces == 0 && analysis.other == 0 && current.isAlphabetic()) {
			if (outliers.size() == maxOutliers || analysis.digits > .01 * realSamples) {
				backoutToPatternID(realSamples, KnownPatterns.ID.ID_ALPHANUMERIC_VARIABLE);
				return true;
			}
		}
		// If we are currently Numeric and the only errors are alpha then convert to AlphaNumeric
		else if (badCharacters != 0 && analysis.spaces == 0 && analysis.other == 0 && PatternInfo.Type.LONG.equals(current.type)) {
			if (outliers.size() == maxOutliers || analysis.alphas > .01 * realSamples) {
				backoutToPattern(realSamples, KnownPatterns.PATTERN_ALPHANUMERIC_VARIABLE);
				return true;
			}
		}
		// If we are currently Numeric and the only errors are doubles then convert to double
		else if (outliers.size() == analysis.doubles && PatternInfo.Type.LONG.equals(current.type)) {
			KnownPatterns.ID id;
			if (analysis.exponent)
				id = analysis.negative ? KnownPatterns.ID.ID_SIGNED_DOUBLE_WITH_EXPONENT : KnownPatterns.ID.ID_DOUBLE_WITH_EXPONENT;
			else
				id = analysis.negative ? KnownPatterns.ID.ID_SIGNED_DOUBLE : KnownPatterns.ID.ID_DOUBLE;
			backoutToPatternID(realSamples, id);
			return true;
		}
		else if ((realSamples > reflectionSamples && outliers.size() == maxOutliers)
					|| (badCharacters + analysis.nonAlphaNumeric) > .01 * realSamples) {
				backoutToPattern(realSamples, KnownPatterns.PATTERN_ANY_VARIABLE);
				return true;
		}

		return false;
	}

	private void backoutToPattern(final long realSamples, String newPattern) {
		PatternInfo newPatternInfo = knownPatterns.getByRegExp(newPattern);

		// If it is not one of our known types then construct a suitable PatternInfo
		if (newPatternInfo == null)
			newPatternInfo = new PatternInfo(null, newPattern, PatternInfo.Type.STRING, null, false, -1, -1, null, null);

		backoutToPatternInfo(realSamples, newPatternInfo);
	}

	private void backoutToPatternID(final long realSamples, KnownPatterns.ID patternID) {
		backoutToPatternInfo(realSamples, knownPatterns.getByID(patternID));
	}

	private void backoutToString(final long realSamples) {
		matchCount = realSamples;

		// All outliers are now part of the cardinality set and there are now no outliers
		cardinality.putAll(outliers);

		RegExpGenerator gen = new RegExpGenerator(true, MAX_ENUM_SIZE, locale);
		for (String s : cardinality.keySet())
			gen.train(s);

		String newPattern = gen.getResult();
		PatternInfo newPatternInfo = knownPatterns.getByRegExp(newPattern);

		// If it is not one of our known types then construct a suitable PatternInfo
		if (newPatternInfo == null)
			newPatternInfo = new PatternInfo(null, newPattern, PatternInfo.Type.STRING, null, false, -1, -1, null, null);

		matchPatternInfo = newPatternInfo;

		for (String s : cardinality.keySet())
			trackString(s, newPatternInfo, false);

		outliers.clear();
		outliersSmashed.clear();
	}

	private void backoutToPatternInfo(final long realSamples, PatternInfo newPatternInfo) {
		matchCount = realSamples;
		matchPatternInfo = newPatternInfo;

		// All outliers are now part of the cardinality set and there are now no outliers
		cardinality.putAll(outliers);

		// Need to update stats to reflect any outliers we previously ignored
		if (matchPatternInfo.type.equals(PatternInfo.Type.STRING)) {
			for (String s : cardinality.keySet())
				trackString(s, newPatternInfo, false);
		}
		else if (matchPatternInfo.type.equals(PatternInfo.Type.DOUBLE)) {
			minDouble = minLong;
			maxDouble = maxLong;
			sumBD = new BigDecimal(sumBI);
			for (final Map.Entry<String, Long> entry : outliers.entrySet()) {
				for (int i = 0; i < entry.getValue(); i++)
					trackDouble(entry.getKey(), matchPatternInfo, true);
			}
		}

		outliers.clear();
		outliersSmashed.clear();
	}

	/**
	 * Backout from a mistaken logical type whose base type was long
	 * @param logical The Logical type we are backing out from
	 * @param realSamples The number of real samples we have seen.
	 */
	private void backoutLogicalLongType(LogicalType logical, final long realSamples) {
		int otherLongs = 0;

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
				if (cardinality.size() < maxCardinality)
					cardinality.put(entry.getKey(), entry.getValue());
				outliers.remove(entry.getKey(), entry.getValue());
				otherLongs += entry.getValue();
			}
		}

		matchCount += otherLongs;
		if ((double) matchCount / realSamples > threshold/100.0)
			matchPatternInfo = knownPatterns.getByID(KnownPatterns.ID.ID_LONG);
		else
			backoutToPatternID(realSamples, KnownPatterns.ID.ID_ANY_VARIABLE);
	}

	// Track basic facts for the field - called for any Valid input
	private void trackTrimmedLengthAndWhiteSpace(final String input) {
		final int length = input.length();
		final int trimmedLength = input.trim().length();

		// Determine if there is leading or trailing White space (if not done previously)
		if (trimmedLength != 0) {
			if (!leadingWhiteSpace)
				leadingWhiteSpace = input.charAt(0) == ' ' || input.charAt(0) == '\t';
			if (!trailingWhiteSpace)
				trailingWhiteSpace = input.charAt(length - 1) == ' ' || input.charAt(length - 1) == '\t';
		}

		if (trimmedLength < minRawLength && trimmedLength < minTrimmedLength)
			minTrimmedLength = trimmedLength;
		if (trimmedLength > maxRawLength && trimmedLength > maxTrimmedLength)
			maxTrimmedLength = trimmedLength;

		// Determine if this is a multi-line field (if not already decided)
		if (!multiline)
			multiline = input.indexOf('\n') != -1 || input.indexOf('\r') != -1;
	}

	/**
	 * Track the supplied raw input, once we have enough samples attempt to determine the type.
	 * @param input The raw input string
	 */
	private void trackResult(final String input, boolean fromTraining, long count) {

		if (fromTraining)
			trackLengthAndShape(input, count);

		// If the detect window cache is full and we have not determined a type compute one
		if ((matchPatternInfo == null || matchPatternInfo.type == null) && sampleCount - (nullCount + blankCount) > detectWindow)
			determineType();

		if (matchPatternInfo == null || matchPatternInfo.type == null)
			return;

		final long realSamples = sampleCount - (nullCount + blankCount);
		boolean valid = false;

		switch (matchPatternInfo.type) {
		case BOOLEAN:
			if (trackBoolean(input)) {
				matchCount += count;
				addValid(input, count);
				valid = true;
			}
			break;

		case LONG:
			if (trackLong(input, matchPatternInfo, true)) {
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
			if (trackString(input, matchPatternInfo, true)) {
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
				DateTimeParserResult result = DateTimeParserResult.asResult(matchPatternInfo.format, resolutionMode, locale);
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
						final String insufficient = "Insufficient digits in input (";
						char ditch = '?';
						int find = e.getMessage().indexOf(insufficient);
						if (find != -1)
							ditch = e.getMessage().charAt(insufficient.length());

						String newFormatString = null;
						if (ditch != '?') {
							int offset = matchPatternInfo.format.indexOf(ditch);

							// S is s special case (unlike H, H, M, d) and is *NOT* handled by the default DateTimeFormatter.ofPattern
							if (ditch == 'S')
								newFormatString = Utils.replaceAt(matchPatternInfo.format, offset, result.timeFieldLengths[3],
										"S{1," + result.timeFieldLengths[3] + "}");
							else
								newFormatString = new StringBuffer(matchPatternInfo.format).deleteCharAt(offset).toString();

							updated = true;
						}
						else if (e.getMessage().equals("Expecting end of input, extraneous input found, last token (FRACTION)")) {
							int offset = matchPatternInfo.format.indexOf('S');
							int oldLength = result.timeFieldLengths[3];
							result.timeFieldLengths[3] = oldLength + 1;
							newFormatString = Utils.replaceAt(matchPatternInfo.format, offset, oldLength,
									"S{1," + result.timeFieldLengths[3] + "}");
							updated = true;
						}
						else if (e.getMessage().equals("Invalid value for hours: 24 (expected 0-23)")) {
							int offset = matchPatternInfo.format.indexOf('H');
							newFormatString = Utils.replaceAt(matchPatternInfo.format, offset, result.timeFieldLengths[0],
									result.timeFieldLengths[0] == 1 ? "k" : "kk");
							updated = true;
						}

						if (!updated)
							break;

						result = DateTimeParserResult.asResult(newFormatString, resolutionMode, locale);
						matchPatternInfo = new PatternInfo(null, result.getRegExp(), matchPatternInfo.type, newFormatString, false, -1, -1, null, newFormatString);
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
			trackTrimmedLengthAndWhiteSpace(input);
		else {
			outlier(input);
			if (!matchPatternInfo.isDateType() && outliers.size() == maxOutliers) {
				if (matchPatternInfo.isLogicalType()) {
					// Do we need to back out from any of our Infinite type determinations
					LogicalType logical = plugins.getRegistered(matchPatternInfo.typeQualifier);
					if (logical.isValidSet(dataStreamName, matchCount, realSamples, null, cardinality, outliers) != null)
						if (PatternInfo.Type.LONG.equals(matchPatternInfo.type) && matchPatternInfo.typeQualifier != null)
							backoutLogicalLongType(logical, realSamples);
						else if (PatternInfo.Type.STRING.equals(matchPatternInfo.type) && matchPatternInfo.typeQualifier != null)
							backoutToPattern(realSamples, KnownPatterns.PATTERN_ANY_VARIABLE);
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
	private FiniteMatchResult checkFiniteSet(Map<String, Long> cardinalityUpper, Map<String, Long> outliers, LogicalTypeFinite logical) {
		final long realSamples = sampleCount - (nullCount + blankCount);
		long missCount = 0;				// count of number of misses

		final Map<String, Long> newOutliers = new HashMap<>();
		final Map<String, Long> addMatches = new HashMap<>();
		final Map<String, Long> minusMatches = new HashMap<>();
		double missThreshold = 1.0 - logical.getThreshold()/100.0;
		long validCount = 0;

		for (final Map.Entry<String, Long> entry : outliers.entrySet()) {
			String upper = entry.getKey().toUpperCase(Locale.ENGLISH);
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
		if ((double) missCount / realSamples <= missThreshold) {
			for (final Map.Entry<String, Long> entry : cardinalityUpper.entrySet()) {
				if (logical.isValid(entry.getKey()))
					validCount += entry.getValue();
				else {
					missCount += entry.getValue();
					minusMatches.put(entry.getKey(), entry.getValue());
					newOutliers.put(entry.getKey(), entry.getValue());
				}
			}
		}

		final Map<String, Long> newCardinality = new HashMap<>(cardinalityUpper);
		newCardinality.putAll(addMatches);
		for (final String elt : minusMatches.keySet())
			newCardinality.remove(elt);

		if (logical.isValidSet(dataStreamName, validCount, realSamples, null, newCardinality, newOutliers) != null)
			return new FiniteMatchResult();

		return new FiniteMatchResult(logical, logical.getConfidence(validCount, realSamples, dataStreamName), validCount, newOutliers, newCardinality);
	}

	private String lengthQualifier(int min, int max) {
		if (!lengthQualifier)
			return min > 0 ? "+" : ".";

		return RegExpSplitter.qualify(min, max);
	}


	/**
	 * Given a Regular Expression with an unbound Integer freeze it with the low and high size.
	 * For example, given something like \d+, convert to \d{4,9}.
	 * @return If possible an updated String, if not found then the original string.
	 */
	private String freezeNumeric(String input) {
		StringBuilder result = new StringBuilder(input);
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
			result.replace(idx, idx + 1, lengthQualifier(minTrimmedLength, maxTrimmedLength)).toString();
	}

	private StringFacts calculateFacts() {
		StringFacts ret = new StringFacts();

		// We know the type - so calculate a minimum and maximum value
		switch (matchPatternInfo.type) {
		case BOOLEAN:
			ret.minValue = String.valueOf(minBoolean);
			ret.maxValue = String.valueOf(maxBoolean);
			break;

		case LONG:
			ret.minValue = String.valueOf(minLong);
			ret.maxValue = String.valueOf(maxLong);
			ret.sum = sumBI.toString();
			ret.bottomK = tbLong.bottomKasString();
			ret.topK = tbLong.topKasString();
			break;

		case DOUBLE:
			NumberFormat formatter = NumberFormat.getInstance(locale);
			formatter.setMaximumFractionDigits(12);
			formatter.setMinimumFractionDigits(1);
			formatter.setGroupingUsed(false);

			ret.minValue = formatter.format(minDouble);
			ret.maxValue = formatter.format(maxDouble);
			ret.sum = sumBD.toString();
			ret.bottomK = tbDouble.bottomKasString();
			ret.topK = tbDouble.topKasString();
			break;

		case STRING:
			if ("NULL".equals(matchPatternInfo.typeQualifier)) {
				minRawLength = maxRawLength = 0;
			} else if ("BLANK".equals(matchPatternInfo.typeQualifier)) {
				// If all the fields are blank (i.e. a variable number of spaces) - then we have not saved any of the raw input, so we
				// need to synthesize the min and max value, as well as the minRawlength if not set.
				if (minRawLength == Integer.MAX_VALUE)
					minRawLength = 0;
				final StringBuilder s = new StringBuilder(maxRawLength);
				for (int i = 0; i < maxRawLength; i++) {
					if (i == minRawLength)
						ret.minValue = s.toString();
					s.append(' ');
				}
				ret.maxValue = s.toString();
				if (minRawLength == maxRawLength)
					ret.minValue = ret.maxValue;
			}
			else {
				ret.minValue = minString;
				ret.maxValue = maxString;
				ret.bottomK = tbString.bottomKasString();
				ret.topK = tbString.topKasString();
			}
			break;

		case LOCALDATE:
			if (collectStatistics) {
				DateTimeFormatter dtf = DateTimeParser.ofPattern(matchPatternInfo.format, locale);

				ret.minValue = minLocalDate == null ? null : minLocalDate.format(dtf);
				ret.maxValue = maxLocalDate == null ? null : maxLocalDate.format(dtf);
				ret.bottomK = tbLocalDate.bottomKasString();
				ret.topK = tbLocalDate.topKasString();
			}
			break;

		case LOCALTIME:
			if (collectStatistics) {
				DateTimeFormatter dtf = DateTimeParser.ofPattern(matchPatternInfo.format, locale);

				ret.minValue = minLocalTime == null ? null : minLocalTime.format(dtf);
				ret.maxValue = maxLocalTime == null ? null : maxLocalTime.format(dtf);
				ret.bottomK = tbLocalTime.bottomKasString();
				ret.topK = tbLocalTime.topKasString();
			}
			break;

		case LOCALDATETIME:
			if (collectStatistics) {
				DateTimeFormatter dtf = DateTimeParser.ofPattern(matchPatternInfo.format, locale);

				ret.minValue = minLocalDateTime == null ? null : minLocalDateTime.format(dtf);
				ret.maxValue = maxLocalDateTime == null ? null : maxLocalDateTime.format(dtf);
				ret.bottomK = tbLocalDateTime.bottomKasString();
				ret.topK = tbLocalDateTime.topKasString();
			}
			break;

		case ZONEDDATETIME:
			if (collectStatistics) {
				DateTimeFormatter dtf = DateTimeParser.ofPattern(matchPatternInfo.format, locale);

				ret.minValue = minZonedDateTime.format(dtf);
				ret.maxValue = maxZonedDateTime.format(dtf);
				ret.bottomK = tbZonedDateTime.bottomKasString();
				ret.topK = tbZonedDateTime.topKasString();
			}
			break;

		case OFFSETDATETIME:
			if (collectStatistics) {
				DateTimeFormatter dtf = DateTimeParser.ofPattern(matchPatternInfo.format, locale);

				ret.minValue = minOffsetDateTime.format(dtf);
				ret.maxValue = maxOffsetDateTime.format(dtf);
				ret.bottomK = tbOffsetDateTime.bottomKasString();
				ret.topK = tbOffsetDateTime.topKasString();
			}
			break;
		}

		return ret;
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

		FiniteMatchResult(LogicalTypeFinite logical, double score, long validCount, Map<String, Long> newOutliers, Map<String, Long> newCardinality) {
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

	private LogicalTypeFinite matchFiniteTypes(Map<String, Long> cardinalityUpper, int minKeyLength, int maxKeyLength) {
		FiniteMatchResult bestResult = null;
		double bestScore = -1.0;

		for (LogicalTypeFinite logical : finiteTypes)
			if ((!logical.isClosed() || cardinalityUpper.size() <= logical.getSize() + 2 + logical.getSize()/20)) {
				FiniteMatchResult result = checkFiniteSet(cardinalityUpper, outliers, logical);
				if (result.matched() && result.score > bestScore) {
					bestResult = result;
					bestScore = result.score;
				}
			}

		if (bestResult == null)
			return null;

		outliers = bestResult.newOutliers;
		cardinality = bestResult.newCardinality;
		matchCount = bestResult.validCount;
		matchPatternInfo = new PatternInfo(null, bestResult.logical.getRegExp(), PatternInfo.Type.STRING, bestResult.logical.getQualifier(), true, -1, -1, null, null);

		return bestResult.logical;
	}

	/**
	 * Determine the result of the training complete to date. Typically invoked
	 * after all training is complete, but may be invoked at any stage.
	 *
	 * @return A TextAnalysisResult with the analysis of any training completed.
	 */
	public TextAnalysisResult getResult() {
		// Normally we will initialize as a consequence of the first call to train() but just in case no training happens!
		if (!initialized)
			initialize();

		// If we have not already determined the type, now we need to
		if (matchPatternInfo == null)
			determineType();

		// Compute our confidence
		final long realSamples = sampleCount - (nullCount + blankCount);
		double confidence = 0;

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

		// Do we need to back out from any of our Logical type determinations.  Most of the time this backs out of
		// Infinite type determinations (since we have not yet declared it to be a Finite type).  However it is possible
		// that this is a subsequent call to getResult()!!
		if (matchPatternInfo.isLogicalType()) {
			LogicalType logical = plugins.getRegistered(matchPatternInfo.typeQualifier);

			// Update our Regular Expression - since it may have changed based on all the data observed
			matchPatternInfo.regexp = logical.getRegExp();
			confidence = logical.getConfidence(matchCount, realSamples, dataStreamName);

			String newPattern;
			if ((newPattern = logical.isValidSet(dataStreamName, matchCount, realSamples, null, cardinality, outliers)) != null) {
				if (PatternInfo.Type.STRING.equals(logical.getBaseType())) {
					backoutToPattern(realSamples, newPattern);
					confidence = (double) matchCount / realSamples;
				}
				else if (PatternInfo.Type.LONG.equals(logical.getBaseType())) {
					backoutLogicalLongType(logical, realSamples);
					confidence = (double) matchCount / realSamples;
				}
			}
		}

		Map<String, Long> cardinalityUpper = new HashMap<>();

		if (KnownPatterns.ID.ID_LONG == matchPatternInfo.id || KnownPatterns.ID.ID_SIGNED_LONG == matchPatternInfo.id) {
			if (KnownPatterns.ID.ID_LONG == matchPatternInfo.id && matchPatternInfo.typeQualifier == null && minLong < 0)
				matchPatternInfo = knownPatterns.getByID(KnownPatterns.ID.ID_SIGNED_LONG);

			// Sometimes a Long is not a Long but it is really a date
			if (groupingSeparators == 0 && minLongNonZero > 19000101 && maxLong < 20410101 &&
					DateTimeParser.plausibleDateCore(false, (int)minLongNonZero%100, ((int)minLongNonZero/100)%100, (int)minLongNonZero/10000, 4)  &&
					DateTimeParser.plausibleDateCore(false, (int)maxLong%100, ((int)maxLong/100)%100, (int)maxLong/10000, 4)  &&
					((realSamples >= reflectionSamples && cardinality.size() > 10) || dataStreamName.toLowerCase(locale).contains("date"))) {
				matchPatternInfo = new PatternInfo(null, (minLongNonZero == minLong || shapes.size() == 1) ? "\\d{8}" : "0|\\d{8}", PatternInfo.Type.LOCALDATE, "yyyyMMdd", false, 8, 8, null, "yyyyMMdd");
				DateTimeFormatter dtf = DateTimeParser.ofPattern(matchPatternInfo.format, locale);
				minLocalDate = LocalDate.parse(String.valueOf(minLongNonZero), dtf);
				maxLocalDate = LocalDate.parse(String.valueOf(maxLong), dtf);

				// If we are collecting statistics - we need to generate the topK and bottomK
				if (collectStatistics)
					for (String s : cardinality.keySet())
						if (!Utils.allZeroes(s))
							trackDateTime(s, matchPatternInfo, true);
			} else if (groupingSeparators == 0 && minLongNonZero > 1800 && maxLong < 2041 &&
					((realSamples >= reflectionSamples && cardinality.size() > 10) || dataStreamName.toLowerCase(locale).contains("year") || dataStreamName.toLowerCase(locale).contains("date"))) {
				matchPatternInfo = new PatternInfo(null, minLongNonZero == minLong ? "\\d{4}" : "0|\\d{4}", PatternInfo.Type.LOCALDATE, "yyyy", false, 4, 4, null, "yyyy");
				minLocalDate = LocalDate.of((int)minLongNonZero, 1, 1);
				maxLocalDate = LocalDate.of((int)maxLong, 1, 1);

				// If we are collecting statistics - we need to generate the topK and bottomK
				if (collectStatistics)
					for (String s : cardinality.keySet())
						if (!"0".equals(s))
							trackDateTime(s, matchPatternInfo, true);
			} else if (cardinality.size() == 2 && minLong == 0 && maxLong == 1) {
				// boolean by any other name
				minBoolean = "0";
				maxBoolean = "1";
				matchPatternInfo = knownPatterns.getByID(KnownPatterns.ID.ID_BOOLEAN_ONE_ZERO);
			} else {
				if (groupingSeparators != 0)
					matchPatternInfo = knownPatterns.grouping(matchPatternInfo.regexp);

				matchPatternInfo = new PatternInfo(matchPatternInfo);
				matchPatternInfo.regexp = freezeNumeric(matchPatternInfo.regexp);

				for (LogicalTypeRegExp logical : regExpTypes) {
					if (PatternInfo.Type.LONG.equals(logical.getBaseType()) &&
							logical.isMatch(matchPatternInfo.regexp) &&
							logical.isValidSet(dataStreamName, matchCount, realSamples, calculateFacts(), cardinality, outliers) == null) {
						matchPatternInfo = new PatternInfo(null, logical.getRegExp(), logical.getBaseType(), logical.getQualifier(), true, -1, -1, null, null);
						confidence = logical.getConfidence(matchCount, realSamples, dataStreamName);
						break;
					}
				}

				if (!matchPatternInfo.isLogicalType() && realSamples >= detectWindow &&
						(confidence < threshold/100.0 ||
								(numericWidening && outliers.size() != 0 && (new OutlierAnalysis(outliers, matchPatternInfo)).doubles == outliers.size()))) {
					// We thought it was an integer field, but on reflection it does not feel like it
					conditionalBackoutToPattern(realSamples, matchPatternInfo);
					confidence = (double) matchCount / realSamples;
				}
			}
		} else if (PatternInfo.Type.BOOLEAN.equals(matchPatternInfo.type) && matchPatternInfo.id == KnownPatterns.ID.ID_BOOLEAN_Y_N && cardinality.size() == 1) {
			backoutToString(realSamples);
			confidence = (double) matchCount / realSamples;
		} else if (PatternInfo.Type.DOUBLE.equals(matchPatternInfo.type) && !matchPatternInfo.isLogicalType()) {
			if (minDouble < 0.0)
				matchPatternInfo = knownPatterns.negation(matchPatternInfo.regexp);

			if (groupingSeparators != 0)
				matchPatternInfo = knownPatterns.grouping(matchPatternInfo.regexp);

			for (LogicalTypeRegExp logical : regExpTypes) {
				if (PatternInfo.Type.DOUBLE.equals(logical.getBaseType()) &&
						logical.isMatch(matchPatternInfo.regexp) &&
						logical.isValidSet(dataStreamName, matchCount, realSamples, calculateFacts(), cardinality, outliers) == null) {
					matchPatternInfo = new PatternInfo(null, logical.getRegExp(), logical.getBaseType(), logical.getQualifier(), true, -1, -1, null, null);
					confidence = logical.getConfidence(matchCount, realSamples, dataStreamName);
					break;
				}
			}
		} else if (PatternInfo.Type.STRING.equals(matchPatternInfo.type) && !matchPatternInfo.isLogicalType()) {
			// Build Cardinality map ignoring case (and white space)
			int minKeyLength = Integer.MAX_VALUE;
			int maxKeyLength = 0;
			for (final Map.Entry<String, Long> entry : cardinality.entrySet()) {
				String key = entry.getKey().toUpperCase(locale).trim();
				int keyLength = key.length();
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

			LogicalTypeFinite logical = matchFiniteTypes(cardinalityUpper, minKeyLength, maxKeyLength);
			if (logical != null)
				confidence = logical.getConfidence(matchCount, realSamples, dataStreamName);

			// Fixup any likely enums
			if (matchPatternInfo.typeQualifier == null && cardinalityUpper.size() < MAX_ENUM_SIZE && outliers.size() != 0 && outliers.size() < 10) {
				boolean updated = false;

				Set<String> killSet = new HashSet<>();

				// Sort the outliers so that we consider the most frequent first
				outliers = Utils.sortByValue(outliers);

				// Iterate through the outliers adding them to the core cardinality set if we think they are reasonable.
				for (final Map.Entry<String, Long> entry : outliers.entrySet()) {
					String key = entry.getKey();
					String keyUpper = key.toUpperCase(locale).trim();
					String validChars = " _-";
					boolean skip = false;

					// We are wary of outliers that only have one instance, do an extra check that the characters in the
					// outlier exist in the real set.
					if (entry.getValue() == 1) {
						// Build the universe of valid characters
						for (String existing : cardinalityUpper.keySet()) {
							for (int i = 0; i < existing.length(); i++) {
								char ch = existing.charAt(i);
								if (!Character.isAlphabetic(ch) && !Character.isDigit(ch))
									if (validChars.indexOf(ch) == -1)
										validChars += ch;
							}
						}
						for (int i = 0; i < keyUpper.length(); i++) {
							char ch = keyUpper.charAt(i);
							if (!Character.isAlphabetic(ch) && !Character.isDigit(ch) && validChars.indexOf(ch) == -1) {
								skip = true;
								break;
							}
						}
					}
					else
						skip = false;

					if (!skip) {
						Long value = cardinalityUpper.get(keyUpper);
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
					Map<String, Long> remainingOutliers = new HashMap<>();
					remainingOutliers.putAll(outliers);
					for (String elt : killSet)
						remainingOutliers.remove(elt);

					backoutToPattern(realSamples, KnownPatterns.PATTERN_ANY_VARIABLE);
					confidence = (double) matchCount / realSamples;
					outliers = remainingOutliers;
				}
			}

			// Need to evaluate if we got the type wrong
			if (matchPatternInfo.typeQualifier == null && outliers.size() != 0 && matchPatternInfo.isAlphabetic() && realSamples >= reflectionSamples) {
				conditionalBackoutToPattern(realSamples, matchPatternInfo);
				confidence = (double) matchCount / realSamples;

				// Rebuild the cardinalityUpper Map
				cardinalityUpper.clear();
				for (final Map.Entry<String, Long> entry : cardinality.entrySet()) {
					String key = entry.getKey().toUpperCase(locale).trim();
					final Long seen = cardinalityUpper.get(key);
					if (seen == null) {
						cardinalityUpper.put(key, entry.getValue());
					} else
						cardinalityUpper.put(key, seen + entry.getValue());
				}
			}
		}

		if (PatternInfo.Type.STRING.equals(matchPatternInfo.type) && matchPatternInfo.typeQualifier == null) {
			boolean updated = false;
			// We would really like to say something better than it is a String!

			// Flip to a Regular Expression based on Shape analysis if possible
			String newRegExp = shapes.getRegExp(realSamples);
			if (newRegExp != null) {
				matchPatternInfo = new PatternInfo(null, newRegExp, PatternInfo.Type.STRING, matchPatternInfo.typeQualifier, false, minTrimmedLength,
						maxTrimmedLength, null, null);
			}

			for (LogicalTypeRegExp logical : regExpTypes) {
				if (PatternInfo.Type.STRING.equals(logical.getBaseType()) &&
						logical.isMatch(matchPatternInfo.regexp) &&
						logical.isValidSet(dataStreamName, matchCount, realSamples, calculateFacts(), cardinality, outliers) == null) {
					matchPatternInfo = new PatternInfo(null, logical.getRegExp(), logical.getBaseType(), logical.getQualifier(), true, -1, -1, null, null);
					confidence = logical.getConfidence(matchCount, realSamples, dataStreamName);
					updated = true;
					break;
				}
			}

			long interestingSamples = sampleCount - (nullCount + blankCount);

			// Try a nice discrete enum
			if (!updated && cardinalityUpper.size() > 1 && cardinalityUpper.size() <= MAX_ENUM_SIZE && (interestingSamples > reflectionSamples || interestingSamples / cardinalityUpper.size() >= 3)) {
				// Rip through the enum doing some basic sanity checks
				RegExpGenerator gen = new RegExpGenerator(true, MAX_ENUM_SIZE, locale);
				boolean fail = false;
				int excessiveDigits = 0;

				for (String elt : cardinalityUpper.keySet()) {
					int length = elt.length();
					// Give up if any one of the strings is too long
					if (length > 40) {
						fail = true;
						break;
					}
					int digits = 0;
					for (int i = 0; i < length; i++) {
						char ch = elt.charAt(i);
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
						Map<String, Long> sorted = Utils.sortByValue(cardinalityUpper);
						for (Map.Entry<String, Long> elt : sorted.entrySet()) {
							if (elt.getValue() < 3 && elt.getKey().length() > 3 && Utils.distanceLevenshtein(elt.getKey(), cardinalityUpper.keySet()) <= 1) {
								cardinalityUpper.remove(elt.getKey());
								outliers.put(elt.getKey(), elt.getValue());
								matchCount -= elt.getValue();
								confidence = (double) matchCount / realSamples;
							}
						}

						// Regenerate the enum without the outliers removed
						gen = new RegExpGenerator(true, MAX_ENUM_SIZE, locale);
						for (String elt : cardinalityUpper.keySet())
							gen.train(elt);
					}

					matchPatternInfo = new PatternInfo(null, gen.getResult(), PatternInfo.Type.STRING, matchPatternInfo.typeQualifier, false, minTrimmedLength,
							maxTrimmedLength, null, null);
					updated = true;
				}
			}

			// Check to see whether the most common shape matches our regExp and test to see if this valid
			if (!updated && shapes.size() > 1) {
				Map.Entry<String, Long> bestShape = shapes.getBest();

				String regExp = Smashed.smashedAsRegExp(bestShape.getKey().trim());
				for (LogicalTypeRegExp logical : regExpTypes) {
					if (PatternInfo.Type.STRING.equals(logical.getBaseType()) &&
							logical.isMatch(regExp) &&
							logical.isValidSet(dataStreamName, bestShape.getValue(), realSamples, calculateFacts(), cardinality, outliers) == null) {
						matchPatternInfo = new PatternInfo(null, regExp, logical.getBaseType(), logical.getQualifier(), true, -1, -1, null, null);
						matchCount = bestShape.getValue();
						confidence = logical.getConfidence(matchCount, realSamples, dataStreamName);
						updated = true;
						break;
					}
				}
			}

			// Qualify Alpha or Alnum with a min and max length
			if (!updated && (KnownPatterns.PATTERN_ALPHA_VARIABLE.equals(matchPatternInfo.regexp) || KnownPatterns.PATTERN_ALPHANUMERIC_VARIABLE.equals(matchPatternInfo.regexp))) {
				String newPattern = matchPatternInfo.regexp;
				newPattern = newPattern.substring(0, newPattern.length() - 1) + lengthQualifier(minTrimmedLength, maxTrimmedLength);
				matchPatternInfo = new PatternInfo(null, newPattern, PatternInfo.Type.STRING, matchPatternInfo.typeQualifier, false, minTrimmedLength,
						maxTrimmedLength, null, null);
				updated = true;
			}

			// Qualify random string with a min and max length
			if (!updated && KnownPatterns.PATTERN_ANY_VARIABLE.equals(matchPatternInfo.regexp)) {
				String newPattern = KnownPatterns.freezeANY(minTrimmedLength, maxTrimmedLength, minRawNonBlankLength, maxRawNonBlankLength, leadingWhiteSpace, trailingWhiteSpace, multiline);
				matchPatternInfo = new PatternInfo(null, newPattern, PatternInfo.Type.STRING, matchPatternInfo.typeQualifier, false, minRawLength,
						maxRawLength, null, null);
				updated = true;
			}
		}

		StringFacts facts = calculateFacts();

		// Attempt to identify keys?
		boolean key = false;
		if (sampleCount > MIN_SAMPLES_FOR_KEY && maxCardinality >= MIN_SAMPLES_FOR_KEY / 2 &&
				(cardinality.size() == maxCardinality || cardinality.size() == sampleCount) &&
				blankCount == 0 && nullCount == 0 &&
				((matchPatternInfo.typeQualifier != null && matchPatternInfo.typeQualifier.equals("GUID")) ||
				(matchPatternInfo.typeQualifier == null &&
				((PatternInfo.Type.STRING.equals(matchPatternInfo.type) && minRawLength == maxRawLength && minRawLength < 32)
						|| PatternInfo.Type.LONG.equals(matchPatternInfo.type))))) {
			key = true;

			if (cardinality.size() == maxCardinality)
				// Might be a key but only iff every element in the cardinality
				// set only has a count of 1
				for (final Map.Entry<String, Long> entry : cardinality.entrySet()) {
					if (entry.getValue() != 1) {
						key = false;
						break;
					}
				}
		}

		return new TextAnalysisResult(dataStreamName, matchCount, matchPatternInfo, leadingWhiteSpace,
				trailingWhiteSpace, multiline, sampleCount, nullCount, blankCount, totalLeadingZeros, confidence, facts,
				minRawLength, maxRawLength, utilizedDecimalSeparator, resolutionMode,
				cardinality, maxCardinality, outliers, maxOutliers, shapes.getShapes(), maxShapes, key, collectStatistics);
	}

	/**
	 * Access the training set - this will typically be the first {@link #DETECT_WINDOW_DEFAULT} records.
	 *
	 * @return A List of the raw input strings.
	 */
	public List<String>getTrainingSet() {
		return raw;
	}
}
