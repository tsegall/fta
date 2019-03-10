/*
 * Copyright 2017-2018 Tim Segall
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.DateTimeParser.DateResolutionMode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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

	/** Threshold for detection - by default this is set to 95%. */
	private int threshold = 95;

	private Locale locale = Locale.getDefault();

	/** The default value for the maximum Cardinality tracked. */
	public static final int MAX_CARDINALITY_DEFAULT = 500;
	private int maxCardinality = MAX_CARDINALITY_DEFAULT;

	private static final int MIN_SAMPLES_FOR_KEY = 1000;

	/** The default value for the maximum # of outliers tracked. */
	public static final int MAX_OUTLIERS_DEFAULT = 50;
	private int maxOutliers = MAX_OUTLIERS_DEFAULT;

	/** We are prepared to recognize any set of this size as an enum (and give a suitable regular expression). */
	private final int MAX_ENUM_SIZE = 40;

	private static final int REFLECTION_SAMPLES = 30;
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
	private Map<String, Integer> cardinality = new HashMap<String, Integer>();
	private Map<String, Integer> outliers = new HashMap<String, Integer>();
	private final Map<String, Integer> outliersSmashed = new HashMap<String, Integer>();
	private List<String> raw; // 0245-11-98
	// 0: d{4}-d{2}-d{2} 1: d{+}-d{+}-d{+} 2: d{+}-d{+}-d{+}
	// 0: d{4} 1: d{+} 2: [-]d{+}
	// input "hello world" 0: a{5} a{5} 1: a{+} a{+} 2: a{+}
	private List<StringBuilder>[] levels = new ArrayList[3];

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

	private long minLong = Long.MAX_VALUE;
	private long minLongNonZero = Long.MAX_VALUE;
	private long maxLong = Long.MIN_VALUE;
	private BigInteger sumBI = BigInteger.ZERO;

	private String minString;
	private String maxString;

	private String minOutlierString;
	private String maxOutlierString;

	private String minBoolean;
	private String maxBoolean;

	private LocalTime minLocalTime;
	private LocalTime maxLocalTime;

	private LocalDate minLocalDate;
	private LocalDate maxLocalDate;

	private LocalDateTime minLocalDateTime;
	private LocalDateTime maxLocalDateTime;

	private ZonedDateTime minZonedDateTime;
	private ZonedDateTime maxZonedDateTime;

	private OffsetDateTime minOffsetDateTime;
	private OffsetDateTime maxOffsetDateTime;

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

	private Map<String, LogicalType> registered = new HashMap<String, LogicalType>();
	private ArrayList<LogicalTypeInfinite> infiniteTypes = new ArrayList<LogicalTypeInfinite>();
	private ArrayList<LogicalTypeFinite> finiteTypes = new ArrayList<LogicalTypeFinite>();
	private ArrayList<LogicalTypeRegExp> regExpTypes = new ArrayList<LogicalTypeRegExp>();
	private int[] candidateCounts;

	private KnownPatterns knownPatterns = new KnownPatterns();

	private DateTimeParser dateTimeParser;

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
		this("anonymous", DateResolutionMode.None);
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
		this.dataStreamName = name;
		this.resolutionMode = resolutionMode;
	}

	/**
	 * Register a new Logical Type processor.
	 * See {@link LogicalTypeFinite} or {@link LogicalTypeInfinite}
	 *
	 * @param className The name of the class for the new Logical Type
	 * @return Success if the new Logical type was successfully registered.
	 */
	public boolean registerLogicalType(String className) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot register logical types once training has started");

		Class<?> newLogicalType;
		Constructor<?> ctor;
		LogicalType logical;

		try {
			newLogicalType = Class.forName(className);
			ctor = newLogicalType.getConstructor();
			logical = (LogicalType)ctor.newInstance();

			if (!(logical instanceof LogicalType))
				throw new IllegalArgumentException("Logical type: " + className + " does not appear to be a Logical Type.");

			logical.initialize(locale);

			if ((logical instanceof LogicalTypeFinite) && ((LogicalTypeFinite)logical).getSize() + 10 > getMaxCardinality())
				throw new IllegalArgumentException("Internal error: Max Cardinality: " + getMaxCardinality() + " is insufficient to support plugin: " + logical.getQualifier());

			if (registered.containsKey(logical.getQualifier()))
				throw new IllegalArgumentException("Logical type: " + logical.getQualifier() + " already registered.");

			registered.put(logical.getQualifier(), logical);

			if (logical instanceof LogicalTypeInfinite)
				infiniteTypes.add((LogicalTypeInfinite)logical);
			else
				finiteTypes.add((LogicalTypeFinite)logical);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			return false;
		}
		return true;
	}

	/**
	 * Register a new Logical Type processor of type LogicalTypeRegExp. See {@link LogicalTypeRegExp}
	 *
	 * @param qualifier The name of the Logical Type
	 * @param hotWords Any words that should improve confidence if observed in the data stream name
	 * @param regExp The RegExp that reflects this logical type (note: this should be the one generated by FTA)
	 * @param baseType The underlying base Type of this Logical Type, e.g. LONG, STRING, ...
	 * @param threshold The threshold required to report this field as of this Logical Type
	 * @return Success if the new Logical type was successfully registered.
	 */
	public boolean registerLogicalTypeRegExp(String qualifier, String[] hotWords, String regExp, int threshold, PatternInfo.Type baseType) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot register logical types once training has started");

		LogicalTypeRegExp logical = new LogicalTypeRegExp(qualifier, hotWords, regExp, threshold, baseType);

		try {
			if (registered.containsKey(logical.getQualifier()))
				throw new IllegalArgumentException("Logical type: " + logical.getQualifier() + " already registered.");

			logical.initialize(locale);

			registered.put(logical.getQualifier(), logical);

			regExpTypes.add(logical);
		} catch (SecurityException | IllegalArgumentException e) {
			return false;
		}
		return true;
	}

	/**
	 * Return the set of registered Logical Types.
	 * @return A Collection of the currently registered Logical Types.
	 */
	public Collection<LogicalType> getRegisteredLogicalTypes() {
		return new HashSet<LogicalType>(registered.values());
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

		if (threshold < 0 || threshold > 100)
			throw new IllegalArgumentException("Threshold must be between 0 and 100ƒ");

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

		if (threshold < 0 || threshold > 100)
			throw new IllegalArgumentException("Plugin Threshold must be between 0 and 100ƒ");

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
	 * @return The current size of the sample window.
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
			l = Long.parseLong(input);
			digits = input.length();
			char ch = input.charAt(0);
			if (ch == '-' || ch == '+')
				digits--;
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

			if (collectStatistics)
				sumBI = sumBI.add(BigInteger.valueOf(l));
		}

		if (patternInfo.isLogicalType()) {
			// If it is a registered Infinite Logical Type then validate it
			LogicalType logical = registered.get(patternInfo.typeQualifier);
			if (PatternInfo.Type.LONG.equals(logical.getBaseType()))
				return logical.isValid(input);
		}

		return true;
	}

	private boolean trackBoolean(final String input) {
		final String trimmedLower = input.trim().toLowerCase(locale);

		final boolean isTrue = "true".equals(trimmedLower) || "yes".equals(trimmedLower);
		final boolean isFalse = !isTrue && ("false".equals(trimmedLower) || "no".equals(trimmedLower));

		if (isTrue) {
			if (minBoolean == null)
				minBoolean = trimmedLower;
			if (maxBoolean == null || "false".equals(maxBoolean) || "no".equals(maxBoolean))
				maxBoolean = trimmedLower;
		} else if (isFalse) {
			if (maxBoolean == null)
				maxBoolean = trimmedLower;
			if (minBoolean == null || "true".equals(minBoolean) || "yes".equals(maxBoolean))
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
			LogicalType logical = registered.get(patternInfo.typeQualifier);
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

		return true;
	}

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

	private boolean trackDouble(final String rawInput, PatternInfo patternInfo, final boolean register) {
		final String input = rawInput.trim();
		double d;

		try {
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
			LogicalType logical = registered.get(patternInfo.typeQualifier);
			if (PatternInfo.Type.DOUBLE.equals(logical.getBaseType()) && !logical.isValid(input))
				return false;
		}

		// If it is NaN/Infinity then we are all done
		if (Double.isNaN(d) || Double.isInfinite(d))
			return true;

		if (register && collectStatistics) {
			if (d < minDouble) {
				minDouble = d;
			}
			if (d > maxDouble) {
				maxDouble = d;
			}

			sumBD = sumBD.add(BigDecimal.valueOf(d));
		}

		return true;
	}

	/*
	 * Validate (and track) the date/time/datetime inoput.
	 * This routine is called for every date/time/datetime we see in the input, so performance is critical.
	 */
	private void trackDateTime(final String dateFormat, final String input) throws DateTimeParseException {
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
			if (collectStatistics) {
				final LocalTime localTime = LocalTime.parse(trimmed, formatter);
				if (minLocalTime == null || localTime.compareTo(minLocalTime) < 0)
					minLocalTime = localTime;
				if (maxLocalTime == null || localTime.compareTo(maxLocalTime) > 0)
					maxLocalTime = localTime;
			}
			else
				result.parse(trimmed);
			break;

		case LOCALDATE:
			if (collectStatistics) {
				final LocalDate localDate = LocalDate.parse(trimmed, formatter);
				if (minLocalDate == null || localDate.compareTo(minLocalDate) < 0)
					minLocalDate = localDate;
				if (maxLocalDate == null || localDate.compareTo(maxLocalDate) > 0)
					maxLocalDate = localDate;
			}
			else
				result.parse(trimmed);
			break;

		case LOCALDATETIME:
			if (collectStatistics) {
				final LocalDateTime localDateTime = LocalDateTime.parse(trimmed, formatter);
				if (minLocalDateTime == null || localDateTime.compareTo(minLocalDateTime) < 0)
					minLocalDateTime = localDateTime;
				if (maxLocalDateTime == null || localDateTime.compareTo(maxLocalDateTime) > 0)
					maxLocalDateTime = localDateTime;
			}
			else
				result.parse(trimmed);
			break;

		case ZONEDDATETIME:
			if (collectStatistics) {
				final ZonedDateTime zonedDataTime = ZonedDateTime.parse(trimmed, formatter);
				if (minZonedDateTime == null || zonedDataTime.compareTo(minZonedDateTime) < 0)
					minZonedDateTime = zonedDataTime;
				if (maxZonedDateTime == null || zonedDataTime.compareTo(maxZonedDateTime) > 0)
					maxZonedDateTime = zonedDataTime;
			}
			else
				result.parse(trimmed);
			break;

		case OFFSETDATETIME:
			if (collectStatistics) {
				final OffsetDateTime offsetDateTime = OffsetDateTime.parse(trimmed, formatter);
				if (minOffsetDateTime == null || offsetDateTime.compareTo(minOffsetDateTime) < 0)
					minOffsetDateTime = offsetDateTime;
				if (maxOffsetDateTime == null || offsetDateTime.compareTo(maxOffsetDateTime) > 0)
					maxOffsetDateTime = offsetDateTime;
			}
			else
				result.parse(trimmed);
			break;

		default:
			throw new InternalErrorException("Expected Date/Time type.");
		}
	}

	private void initialize() {
		Calendar cal = GregorianCalendar.getInstance(locale);
		if (!(cal instanceof GregorianCalendar))
			throw new IllegalArgumentException("No support for locales that do not use the Gregorian Calendar");

		if (!NumberFormat.getNumberInstance(locale).format(0).matches("\\d"))
			throw new IllegalArgumentException("No support for locales that do not use Arabic numerals");

		raw = new ArrayList<String>(detectWindow);
		levels[0] = new ArrayList<StringBuilder>(detectWindow);
		levels[1] = new ArrayList<StringBuilder>(detectWindow);
		levels[2] = new ArrayList<StringBuilder>(detectWindow);

		if (enableDefaultLogicalTypes) {
			// Load the default set of plugins for Logical Type detection
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/plugins.json")))){

				ObjectMapper mapper = new ObjectMapper();
				List<PluginDefinition> plugins = mapper.readValue(reader, new TypeReference<List<PluginDefinition>>(){});
				String languageTag = locale.toLanguageTag();
				String language = locale.getLanguage();

				// Only register plugins that are valid for this locale
				for (PluginDefinition plugin : plugins) {
					boolean register = false;
					if (plugin.locale.length != 0) {
						for (String validLocale : plugin.locale) {
							if (validLocale.indexOf('-') != -1) {
								if (validLocale.equals(languageTag)) {
									register = true;
									break;
								}
							}
							else if (validLocale.equals(language)) {
								register = true;
								break;
							}
						}
					}
					else
						register = true;

					if (register)
						registerLogicalType(plugin.clazz);
				}
			} catch (IOException e) {
				throw new IllegalArgumentException("Internal error: Issues with plugins file");
			}
		}

		candidateCounts = new int[infiniteTypes.size()];

		if (pluginThreshold != -1) {
			// Set the threshold for all Logical Types
			for (LogicalType logical : infiniteTypes)
				logical.setThreshold(pluginThreshold);
			for (LogicalType logical : finiteTypes)
				logical.setThreshold(pluginThreshold);
			for (LogicalType logical : regExpTypes)
				logical.setThreshold(pluginThreshold);
		}

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
	determineNumericPattern(boolean numericSigned, int numericDecimalSeparators, int possibleExponentSeen) {
		StringBuilder[] result = new StringBuilder[2];

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

	/**
	 * Train is the core entry point used to supply input to the Text Analyzer.
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
			trackLengthAndShape(rawInput);
			return matchType != null;
		}

		// This next try/catch is unnecessary in theory, if there are zero bugs then it will never trip,
		// if there happens to be an issue then we will lose this training event.
		boolean result;
		try {
			result = trainCore(rawInput, trimmed, length);
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

	public boolean trainCore(final String rawInput, String trimmed, final int length) {

		trackResult(rawInput);

		// If we have determined a type, no need to further train
		if (matchPatternInfo != null && matchPatternInfo.type != null)
			return true;

		raw.add(rawInput);

		final StringBuilder l0 = new StringBuilder(length);

		// Walk the string
		boolean numericSigned = false;
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
			numericSigned = true;

		for (int i = startLooking; i < stopLooking; i++) {
			char ch = trimmed.charAt(i);

			// Track counts and last occurrence for simple characters
			if (ch <= 127) {
				charCounts[ch]++;
				lastIndex[ch] = i;
			}

			if ((ch == minusSign || ch == '+') && i == 0) {
				numericSigned = true;
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

		if (couldBeNumeric && possibleExponentSeen != -1 &&
			stopLooking - possibleExponentSeen - 1 > 3)
			couldBeNumeric = false;

		final StringBuilder compressedl0 = new StringBuilder(length);
		if (alphasSeen != 0 && digitsSeen != 0 && alphasSeen + digitsSeen == length) {
			compressedl0.append(KnownPatterns.PATTERN_ALPHANUMERIC).append('{').append(String.valueOf(length)).append('}');
		} else if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
			compressedl0.append(KnownPatterns.PATTERN_BOOLEAN_TRUE_FALSE);
		} else if ("yes".equalsIgnoreCase(trimmed) || "no".equalsIgnoreCase(trimmed)) {
			compressedl0.append(KnownPatterns.PATTERN_BOOLEAN_YES_NO);
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
		levels[0].add(compressedl0);

		if (dateTimeParser.determineFormatString(trimmed) != null)
			possibleDateTime++;

		// Check to see if this input is one of our registered Infinite Logical Types
		int c = 0;
		for (LogicalTypeInfinite logical : infiniteTypes) {
			if (logical.isCandidate(trimmed, compressedl0, charCounts, lastIndex))
				candidateCounts[c]++;
			c++;
		}

		// Create the level 1 and 2
		if (digitsSeen > 0 && couldBeNumeric && numericDecimalSeparators <= 1) {
			StringBuilder[] result = determineNumericPattern(numericSigned, numericDecimalSeparators, possibleExponentSeen);
			levels[1].add(result[0]);
			levels[2].add(result[1]);
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
				levels[1].add(new StringBuilder(found.generalPattern));
				levels[2].add(new StringBuilder(collapsed));
			} else {
				levels[1].add(new StringBuilder(collapsed));
				levels[2].add(new StringBuilder(KnownPatterns.PATTERN_ANY_VARIABLE));
			}
		}

		return matchPatternInfo != null && matchPatternInfo.type != null;
	}

	private Map.Entry<String, Integer> getBest(final int levelIndex) {
		final List<StringBuilder> level = levels[levelIndex];
		if (level.isEmpty())
			return null;

		Map<String, Integer> frequency = new HashMap<String, Integer>();

		// Calculate the frequency of every element
		for (final StringBuilder s : level) {
			final String key = s.toString();
			final Integer seen = frequency.get(key);
			if (seen == null) {
				frequency.put(key, 1);
			} else {
				frequency.put(key, seen + 1);
			}
		}

		// Sort the results
		frequency = Utils.sortByValue(frequency);

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
					best = new AbstractMap.SimpleEntry<String, Integer>(newKey, best.getValue() + secondBest.getValue());
				}
			}
			else if (thirdBest == null) {
				thirdBest = entry;
				thirdBestPattern = knownPatterns.getByRegExp(thirdBest.getKey());
				if (levelIndex != 0 && bestPattern != null && thirdBestPattern != null &&
						bestPattern.isNumeric() && thirdBestPattern.isNumeric()) {
					newKey = knownPatterns.numericPromotion(newKey != null ? newKey : bestPattern.regexp, thirdBestPattern.regexp);
					best = new AbstractMap.SimpleEntry<String, Integer>(newKey, best.getValue() + thirdBest.getValue());
				}
			}
		}

		if (bestPattern != null && secondBestPattern != null && PatternInfo.Type.STRING.equals(bestPattern.type)) {
			// Promote anything to STRING
			best = new AbstractMap.SimpleEntry<String, Integer>(best.getKey(),
					best.getValue() + secondBest.getValue());
		}

		return best;
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

		int level0value = 0, level1value = 0, level2value = 0;
		String level0pattern = null, level1pattern = null, level2pattern = null;
		PatternInfo level0patternInfo = null, level1patternInfo = null, level2patternInfo = null;
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

			// Take any level 1 with something we recognize or a better count
			if (level1 != null && (level0patternInfo == null || level1value > level0value)) {
				best = level1;
				matchPatternInfo = level1patternInfo;
			}

			// Take a level 2 if
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
				best = level2;
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
						}
					}

					// If a reasonable number look genuine then we are convinced
					if (count >= logical.getThreshold()/100.0 * raw.size())
						matchPatternInfo = candidate;
				}
				i++;
			}

			for (final String sample : raw)
				trackResult(sample);
		}
	}

	private void addValid(final String input) {
		final Integer seen = cardinality.get(input);
		if (seen == null) {
			if (cardinality.size() < maxCardinality)
				cardinality.put(input, 1);
		} else
			cardinality.put(input, seen + 1);
	}

	private String shapeToRegExp(final String shape) {

		if (shape.equals(".+"))
			return null;

		StringBuilder result = new StringBuilder();
		final String shapeWithSentinel = shape.toString() + "|";
		char last = shapeWithSentinel.charAt(0);
		int repetitions = 1;
		for (int i = 1; i < shapeWithSentinel.length(); i++) {
			final char ch = shapeWithSentinel.charAt(i);
			if (ch == last) {
				repetitions++;
			} else {
				if (last == '1' || last == 'a') {
					result.append(last == '1' ? "\\d" : KnownPatterns.PATTERN_ALPHA);
					result.append('{').append(String.valueOf(repetitions)).append('}');
				} else {
					for (int j = 0; j < repetitions; j++) {
						if (last == '+' || last == '.' || last == '*' || last == '(' || last == ')' ||
								last == '{' || last == '}' || last == '[' || last == ']' || last == '^' || last == '$')
							result.append('\\');
						result.append(last);
					}
				}
				last = ch;
				repetitions = 1;
			}
		}

		return result.toString();
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

		String smashed = RegExpGenerator.smash(input);
		Integer seen = outliersSmashed.get(smashed);
		if (seen == null) {
			if (outliersSmashed.size() < maxOutliers)
				outliersSmashed.put(smashed, 1);
		} else {
			outliersSmashed.put(smashed, seen + 1);
		}

		seen = outliers.get(input);
		if (seen == null) {
			if (outliers.size() < maxOutliers)
				outliers.put(input, 1);
		} else {
			outliers.put(input, seen + 1);
		}

		return outliers.size();
	}

	private boolean conditionalBackoutToPattern(final long realSamples, PatternInfo current, boolean useCompressed) {
		int alphas = 0;
		int digits = 0;
		int spaces = 0;
		int other = 0;
		int doubles = 0;
		int nonAlphaNumeric = 0;
		boolean negative = false;
		boolean exponent = false;
		Map<String, Integer> outlierMap = useCompressed ? outliersSmashed : outliers;

		// Sweep the current outliers
		for (final Map.Entry<String, Integer> entry : outlierMap.entrySet()) {
			String key = entry.getKey();
			Integer value = entry.getValue();
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

		int badCharacters = current.isAlphabetic() ? digits : alphas;
		// If we are currently Alphabetic and the only errors are digits then convert to AlphaNumeric
		if (badCharacters != 0 && spaces == 0 && other == 0 && current.isAlphabetic()) {
			if (outlierMap.size() == maxOutliers || digits > .01 * realSamples) {
				backoutToPatternID(realSamples, KnownPatterns.ID.ID_ALPHANUMERIC_VARIABLE);
				return true;
			}
		}
		// If we are currently Numeric and the only errors are alpha then convert to AlphaNumeric
		else if (badCharacters != 0 && spaces == 0 && other == 0 && PatternInfo.Type.LONG.equals(current.type)) {
			if (outlierMap.size() == maxOutliers || alphas > .01 * realSamples) {
				backoutToPattern(realSamples, KnownPatterns.PATTERN_ALPHANUMERIC_VARIABLE);
				return true;
			}
		}
		// If we are currently Numeric and the only errors are doubles then convert to double
		else if (outlierMap.size() == doubles && PatternInfo.Type.LONG.equals(current.type)) {
			KnownPatterns.ID id;
			if (exponent)
				id = negative ? KnownPatterns.ID.ID_SIGNED_DOUBLE_WITH_EXPONENT : KnownPatterns.ID.ID_DOUBLE_WITH_EXPONENT;
			else
				id = negative ? KnownPatterns.ID.ID_SIGNED_DOUBLE : KnownPatterns.ID.ID_DOUBLE;
			backoutToPatternID(realSamples, id);
			return true;
		}
		else if ((realSamples > reflectionSamples && outlierMap.size() == maxOutliers)
					|| (badCharacters + nonAlphaNumeric) > .01 * realSamples) {
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

	private void backoutToPatternInfo(final long realSamples, PatternInfo newPatternInfo) {
		matchCount = realSamples;
		matchPatternInfo = newPatternInfo;

		// All outliers are now part of the cardinality set and there are now no outliers
		cardinality.putAll(outliers);

		// Need to update stats to reflect any outliers we previously ignored
		if (matchPatternInfo.type.equals(PatternInfo.Type.STRING)) {
			if (minString == null || minString.compareTo(minOutlierString) > 0)
				minString = minOutlierString;

			if (maxString == null || maxString.compareTo(maxOutlierString) < 0)
				maxString = maxOutlierString;

			if (minTrimmedLength > minTrimmedOutlierLength)
				minTrimmedLength = minTrimmedOutlierLength;
			if (maxTrimmedOutlierLength > maxTrimmedLength)
				maxTrimmedLength = maxTrimmedOutlierLength;
		}
		else if (matchPatternInfo.type.equals(PatternInfo.Type.DOUBLE)) {
			minDouble = minLong;
			maxDouble = maxLong;
			sumBD = new BigDecimal(sumBI);
			for (final Map.Entry<String, Integer> entry : outliers.entrySet()) {
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

		final Map<String, Integer> outliersCopy = new HashMap<String, Integer>(outliers);

		// Sweep the current outliers and check they are part of the set
		for (final Map.Entry<String, Integer> entry : outliersCopy.entrySet()) {
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

	private boolean uniformShape = true;
	private Map<String, Integer> shapes = new HashMap<>();
	// Track basic facts for the field - called for all input
	private void trackLengthAndShape(final String input) {
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

			if (uniformShape) {
				String inputShape = RegExpGenerator.smash(trimmed);
				if (inputShape.equals(".+"))
					uniformShape = false;
				else {
					Integer seen = shapes.get(inputShape);
					if (seen == null)
						if (shapes.size() < 4)
							shapes.put(inputShape, 1);
						else
							uniformShape = false;
					else
						shapes.put(inputShape, seen + 1);
				}
			}
		}
	}

	// Track basic facts for the field - called for any Valid input
	private void trackTrimmedLengthAndWhiteSpace(final String input) {
		final int length = input.length();
		final int trimmedLength = input.trim().length();

		// Determine if there is leading or trailing White space (if not done previously)
		if (trimmedLength != 0) {
			if (!leadingWhiteSpace)
				leadingWhiteSpace = Character.isSpaceChar(input.charAt(0));
			if (!trailingWhiteSpace)
				trailingWhiteSpace = Character.isSpaceChar(input.charAt(length - 1));
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
	private void trackResult(final String input) {

		trackLengthAndShape(input);

		// If the cache is full and we have not determined a type compute one
		if ((matchPatternInfo == null || matchPatternInfo.type == null) && sampleCount - (nullCount + blankCount) > detectWindow)
			determineType();

		if (matchPatternInfo == null || matchPatternInfo.type == null)
			return;

		final long realSamples = sampleCount - (nullCount + blankCount);
		boolean valid = false;

		switch (matchPatternInfo.type) {
		case BOOLEAN:
			if (trackBoolean(input)) {
				matchCount++;
				addValid(input);
				valid = true;
			}
			break;

		case LONG:
			if (trackLong(input, matchPatternInfo, true)) {
				matchCount++;
				addValid(input);
				valid = true;
			}
			break;

		case DOUBLE:
			if (trackDouble(input, matchPatternInfo, true)) {
				matchCount++;
				addValid(input);
				valid = true;
			}
			break;

		case STRING:
			if (trackString(input, matchPatternInfo, true)) {
				matchCount++;
				addValid(input);
				valid = true;
			}
			break;

		case LOCALDATE:
		case LOCALTIME:
		case LOCALDATETIME:
		case OFFSETDATETIME:
		case ZONEDDATETIME:
			try {
				trackDateTime(matchPatternInfo.format, input);
				matchCount++;
				addValid(input);
				valid = true;
			}
			catch (DateTimeParseException reale) {
				// The real parse threw an Exception, this does not give us enough facts to usefully determine if there is any
				// improvements to our assumptions we could make to do better, so re-parse and handle our more nuanced exception
				DateTimeParserResult result = DateTimeParserResult.asResult(matchPatternInfo.format, resolutionMode, locale);
				boolean success = false;
				do {
					try {
						result.parse(input);
						success = true;
					}
					catch (DateTimeParseException e) {
						// If the parse exception is of the form 'Insufficient digits in input (M)' or similar
						// then worth updating our pattern and retrying.
						final String insufficient = "Insufficient digits in input (";
						char ditch = '?';
						int find = e.getMessage().indexOf(insufficient);
						if (find != -1)
							ditch = e.getMessage().charAt(insufficient.length());

						if (ditch == '?')
							break;

						int offset = matchPatternInfo.format.indexOf(ditch);
						String newFormatString;

						// S is s special case (unlike H, H, M, d) and is *NOT* handled by the default DateTimeFormatter.ofPattern
						if (ditch == 'S')
							newFormatString = Utils.replaceFirst(matchPatternInfo.format, "SSS", "S{1,3}");
						else
							newFormatString = new StringBuffer(matchPatternInfo.format).deleteCharAt(offset).toString();

						result = DateTimeParserResult.asResult(newFormatString, resolutionMode, locale);
						matchPatternInfo = new PatternInfo(null, result.getRegExp(), matchPatternInfo.type, newFormatString, false, -1, -1, null, newFormatString);
					}
				} while (!success);

				try {
					trackDateTime(matchPatternInfo.format, input);
					matchCount++;
					addValid(input);
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
			if (outliers.size() == maxOutliers) {
				if (matchPatternInfo.typeQualifier != null) {
					// Do we need to back out from any of our Infinite type determinations
					LogicalType logical = registered.get(matchPatternInfo.typeQualifier);
					if (logical != null && logical.isValidSet(dataStreamName, matchCount, realSamples, cardinality, outliers) != null)
						if (PatternInfo.Type.LONG.equals(matchPatternInfo.type) && matchPatternInfo.typeQualifier != null)
							backoutLogicalLongType(logical, realSamples);
						else if (PatternInfo.Type.STRING.equals(matchPatternInfo.type) && matchPatternInfo.typeQualifier != null)
							backoutToPattern(realSamples, KnownPatterns.PATTERN_ANY_VARIABLE);
				}
				else {
					// Need to evaluate if we got this wrong
					conditionalBackoutToPattern(realSamples, matchPatternInfo, true);
				}
			}
		}
	}

	/**
	 * Determine if the current dataset reflects a logical type (of uniform length).
	 * @param logical The Logical type we are testing
	 * @return True if we believe that this data set is defined by the provided set
	 */
	private boolean checkUniformLengthSet(LogicalTypeFinite logical) {
		final long realSamples = sampleCount - (nullCount + blankCount);
		long missCount = 0;				// count of number of misses

		// Check how many outliers we have
		for (final Map.Entry<String, Integer> entry : outliers.entrySet()) {
			missCount += entry.getValue();
			// Break out early if we know we are going to fail
			if ((double) missCount / realSamples > .05)
				return false;
		}

		// Sweep the balance and check they are part of the set
		long validCount = 0;
		double missThreshold = 1.0 - logical.getThreshold()/100.0;
		final Map<String, Integer> newOutliers = new HashMap<String, Integer>();
		if ((double) missCount / realSamples <= missThreshold) {
			for (final Map.Entry<String, Integer> entry : cardinality.entrySet()) {
				if (logical.isValid(entry.getKey().trim().toUpperCase(locale)))
					validCount += entry.getValue();
				else {
					missCount += entry.getValue();
					newOutliers.put(entry.getKey(), entry.getValue());
				}
			}
		}

		if (logical.isValidSet(dataStreamName, realSamples - missCount, realSamples, cardinality, newOutliers) != null)
			return false;

		matchCount = validCount;
		matchPatternInfo = new PatternInfo(null, logical.getRegExp(), PatternInfo.Type.STRING, logical.getQualifier(), true, -1, -1, null, null);
		outliers.putAll(newOutliers);
		cardinality.keySet().removeAll(newOutliers.keySet());

		return true;
	}

	/**
	 * Determine if the current data set reflects a logical type (of variable length).
	 * @param cardinalityUpper The cardinality set but reduced to ignore case
	 * @param logical The Logical type we are testing
	 * @return True if we believe that this data set is defined by the provided by this Logical Type
	 */
	private boolean checkVariableLengthSet(Map<String, Integer> cardinalityUpper, LogicalTypeFinite logical) {
		final long realSamples = sampleCount - (nullCount + blankCount);
		final Map<String, Integer> newOutliers = new HashMap<String, Integer>();
		long validCount = 0;
		long missCount = 0;				// count of number of misses

		// Sweep the balance and check they are part of the set
		for (final Map.Entry<String, Integer> entry : cardinalityUpper.entrySet()) {
			if (logical.isValid(entry.getKey()))
				validCount += entry.getValue();
			else {
				missCount += entry.getValue();
				newOutliers.put(entry.getKey(), entry.getValue());
			}
		}

		if (logical.isValidSet(dataStreamName, realSamples - missCount, realSamples, cardinalityUpper, newOutliers) != null)
			return false;

		outliers.putAll(newOutliers);
		cardinalityUpper.keySet().removeAll(newOutliers.keySet());
		matchCount = validCount;
		matchPatternInfo = new PatternInfo(null, logical.getRegExp(), PatternInfo.Type.STRING, logical.getQualifier(), true, -1, -1, null, null);
		cardinality = cardinalityUpper;
		return true;
	}

	private String lengthQualifier(int min, int max) {
		if (!lengthQualifier)
			return min > 0 ? "+" : ".";

		return Utils.regExpLength(min, max);
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
			} else if (ch == '[')
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

		String minValue = null;
		String maxValue = null;
		String sum = null;

		// If we have not already determined the type, now we need to
		if (matchPatternInfo == null || matchPatternInfo.type == null)
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
			LogicalType logical = registered.get(matchPatternInfo.typeQualifier);
			String newPattern;
			if (logical != null && (newPattern = logical.isValidSet(dataStreamName, matchCount, realSamples, cardinality, outliers)) != null) {
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

		Map<String, Integer> cardinalityUpper = new HashMap<String, Integer>();

		if (KnownPatterns.ID.ID_LONG == matchPatternInfo.id || KnownPatterns.ID.ID_SIGNED_LONG == matchPatternInfo.id) {
			if (KnownPatterns.ID.ID_LONG == matchPatternInfo.id && matchPatternInfo.typeQualifier == null && minLong < 0)
				matchPatternInfo = knownPatterns.getByID(KnownPatterns.ID.ID_SIGNED_LONG);

			if (groupingSeparators == 0 && minLongNonZero > 19000101 && maxLong < 20410101 &&
					DateTimeParser.plausibleDateCore(false, (int)minLongNonZero%100, ((int)minLongNonZero/100)%100, (int)minLongNonZero/10000, 4)  &&
					DateTimeParser.plausibleDateCore(false, (int)maxLong%100, ((int)maxLong/100)%100, (int)maxLong/10000, 4)  &&
					((realSamples >= reflectionSamples && cardinality.size() > 10) || dataStreamName.toLowerCase(locale).contains("date"))) {
				matchPatternInfo = new PatternInfo(null, minLongNonZero == minLong ? "\\d{8}" : "0|\\d{8}", PatternInfo.Type.LOCALDATE, "yyyyMMdd", false, 8, 8, null, "yyyyMMdd");
				DateTimeFormatter dtf = DateTimeParser.ofPattern(matchPatternInfo.format, locale);
				minLocalDate = LocalDate.parse(String.valueOf(minLongNonZero), dtf);
				maxLocalDate = LocalDate.parse(String.valueOf(maxLong), dtf);
			} else if (groupingSeparators == 0 && minLongNonZero > 1800 && maxLong < 2041 &&
					((realSamples >= reflectionSamples && cardinality.size() > 10) || dataStreamName.toLowerCase(locale).contains("year") || dataStreamName.toLowerCase(locale).contains("date"))) {
				matchPatternInfo = new PatternInfo(null, minLongNonZero == minLong ? "\\d{4}" : "0|\\d{4}", PatternInfo.Type.LOCALDATE, "yyyy", false, 4, 4, null, "yyyy");
				minLocalDate = LocalDate.of((int)minLongNonZero, 1, 1);
				maxLocalDate = LocalDate.of((int)maxLong, 1, 1);
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

				if (realSamples >= reflectionSamples && confidence < threshold/100.0) {
					// We thought it was an integer field, but on reflection it does not feel like it
					conditionalBackoutToPattern(realSamples, matchPatternInfo, false);
					confidence = (double) matchCount / realSamples;
				}
			}
		} else if (PatternInfo.Type.DOUBLE.equals(matchPatternInfo.type) && !matchPatternInfo.isLogicalType()) {
			if (minDouble < 0.0)
				matchPatternInfo = knownPatterns.negation(matchPatternInfo.regexp);

			if (groupingSeparators != 0)
				matchPatternInfo = knownPatterns.grouping(matchPatternInfo.regexp);
		} else if (PatternInfo.Type.STRING.equals(matchPatternInfo.type) && !matchPatternInfo.isLogicalType()) {
			// Build Cardinality map ignoring case (and white space)
			int minKeyLength = Integer.MAX_VALUE;
			int maxKeyLength = 0;
			for (final Map.Entry<String, Integer> entry : cardinality.entrySet()) {
				String key = entry.getKey().toUpperCase(locale).trim();
				int keyLength = key.length();
				if (keyLength < minKeyLength)
					minKeyLength = keyLength;
				if (keyLength > maxKeyLength)
					maxKeyLength = keyLength;
				final Integer seen = cardinalityUpper.get(key);
				if (seen == null) {
					cardinalityUpper.put(key, entry.getValue());
				} else
					cardinalityUpper.put(key, seen + entry.getValue());
			}
			// Sort the results so that we consider the most frequent first (we will hopefully fail faster)
			cardinalityUpper = Utils.sortByValue(cardinalityUpper);

			boolean typeIdentified = false;

			if (minKeyLength == maxKeyLength) {
				// Hunt for a fixed length Logical Type
				for (LogicalTypeFinite logical : finiteTypes) {
					if (minKeyLength == logical.getMinLength() && logical.getMinLength() == logical.getMaxLength())
						if (!typeIdentified && cardinalityUpper.size() <= logical.getSize() + 2) {
							typeIdentified = checkUniformLengthSet(logical);
							if (typeIdentified) {
								confidence = (double) matchCount / realSamples;
								break;
							}
						}
				}
			}
			else {
				// Hunt for a variable length Logical Type
				for (LogicalTypeFinite logical : finiteTypes) {
					if (logical.getMinLength() != logical.getMaxLength())
						if (!typeIdentified && cardinalityUpper.size() <= logical.getSize() + 1) {
							typeIdentified = checkVariableLengthSet(cardinalityUpper, logical);
							if (typeIdentified) {
								confidence = (double) matchCount / realSamples;
								break;
							}
						}
				}
			}

			// Fixup any likely enums
			if (matchPatternInfo.typeQualifier == null && cardinalityUpper.size() < MAX_ENUM_SIZE && outliers.size() != 0 && outliers.size() < 10) {
				boolean updated = false;

				Set<String> killSet = new HashSet<>();

				// Sort the outliers so that we consider the most frequent first
				outliers = Utils.sortByValue(outliers);

				// Iterate through the outliers adding them to the core cardinality set if we think they are reasonable.
				for (final Map.Entry<String, Integer> entry : outliers.entrySet()) {
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
						Integer value = cardinalityUpper.get(keyUpper);
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
					Map<String, Integer> remainingOutliers = new HashMap<String, Integer>();
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
				conditionalBackoutToPattern(realSamples, matchPatternInfo, false);
				confidence = (double) matchCount / realSamples;

				// Rebuild the cardinalityUpper Map
				cardinalityUpper.clear();
				for (final Map.Entry<String, Integer> entry : cardinality.entrySet()) {
					String key = entry.getKey().toUpperCase(locale).trim();
					final Integer seen = cardinalityUpper.get(key);
					if (seen == null) {
						cardinalityUpper.put(key, entry.getValue());
					} else
						cardinalityUpper.put(key, seen + entry.getValue());
				}
			}
		}

		if (PatternInfo.Type.STRING.equals(matchPatternInfo.type) && matchPatternInfo.typeQualifier == null) {

			// We would really like to say something better than it is a String!

			boolean updated = false;
			long interestingSamples = sampleCount - (nullCount + blankCount);

			// First try a nice discrete enum
			if ((interestingSamples > reflectionSamples || interestingSamples / cardinalityUpper.size() >= 3) && cardinalityUpper.size() > 1 && cardinalityUpper.size() <= MAX_ENUM_SIZE) {
				// Rip through the enum doing some basic sanity checks
				RegExpGenerator gen = new RegExpGenerator(true, MAX_ENUM_SIZE, locale);
				boolean fail = false;
				int excessiveDigits = 0;
				for (String elt : cardinalityUpper.keySet()) {
					int length = elt.length();
					// Give up if any one of the string is too long
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
					matchPatternInfo = new PatternInfo(null, gen.getResult(), PatternInfo.Type.STRING, matchPatternInfo.typeQualifier, false, minTrimmedLength,
							maxTrimmedLength, null, null);
					updated = true;
				}
			}

			/*
			if (!updated && cardinality.size() == 1 && interestingSamples > reflectionSamples) {
				matchPatternInfo = new PatternInfo(null, RegExpGenerator.slosh(cardinality.keySet().iterator().next().trim()), PatternInfo.Type.STRING, matchPatternInfo.typeQualifier, false, minTrimmedLength,
						maxTrimmedLength, null, null);
				updated = true;
			}
			*/

			String singleUniformShape = null;
			if (!updated && uniformShape) {
				if (shapes.size() == 1)
					singleUniformShape = shapes.keySet().iterator().next();
				else {
					if (shapes.size() == 2 && realSamples > 100) {
						Iterator<Map.Entry<String, Integer>> iter = shapes.entrySet().iterator();
						Map.Entry<String, Integer> firstShape = iter.next();
						Map.Entry<String, Integer> secondShape = iter.next();

						if (firstShape.getValue() > realSamples * 15/100 && secondShape.getValue() > realSamples * 15/100) {
							String firstRE = RegExpGenerator.smashedAsRegExp(firstShape.getKey());
							String secondRE = RegExpGenerator.smashedAsRegExp(secondShape.getKey());
							matchPatternInfo = new PatternInfo(null, RegExpGenerator.merge(firstRE, secondRE), PatternInfo.Type.STRING, matchPatternInfo.typeQualifier, false, minTrimmedLength,
									maxTrimmedLength, null, null);
							updated = true;
						}
/*						for (Map.Entry<String, Integer> shape : shapes.entrySet()) {
							if (shape.getValue() < realSamples/15)
								interesting = false;
						}
						if (interesting) {
							System.err.println("Multishape: ");
							for (Map.Entry<String, Integer> shape : shapes.entrySet()) {
								System.err.printf("%s: %d\n", shape.getKey(), shape.getValue());
							}
						}
						*/
					}
				}
			}

			if (!updated && singleUniformShape != null && interestingSamples > reflectionSamples) {
				matchPatternInfo = new PatternInfo(null, RegExpGenerator.smashedAsRegExp(singleUniformShape.trim()), PatternInfo.Type.STRING, matchPatternInfo.typeQualifier, false, minTrimmedLength,
						maxTrimmedLength, null, null);
				updated = true;
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
				String newPattern = null;
				if (singleUniformShape != null && cardinality.size() > 1)
					newPattern = shapeToRegExp(singleUniformShape);
				if (newPattern == null)
					newPattern = KnownPatterns.freezeANY(minTrimmedLength, maxTrimmedLength, minRawNonBlankLength, maxRawNonBlankLength, leadingWhiteSpace, trailingWhiteSpace, multiline);
				matchPatternInfo = new PatternInfo(null, newPattern, PatternInfo.Type.STRING, matchPatternInfo.typeQualifier, false, minRawLength,
						maxRawLength, null, null);
				updated = true;
			}
		}

		// We know the type - so calculate a minimum and maximum value
		switch (matchPatternInfo.type) {
		case BOOLEAN:
			minValue = String.valueOf(minBoolean);
			maxValue = String.valueOf(maxBoolean);
			break;

		case LONG:
			minValue = String.valueOf(minLong);
			maxValue = String.valueOf(maxLong);
			sum = sumBI.toString();
			break;

		case DOUBLE:
			NumberFormat formatter = NumberFormat.getInstance(locale);
			formatter.setMaximumFractionDigits(12);
			formatter.setMinimumFractionDigits(1);
			formatter.setGroupingUsed(false);

			minValue = formatter.format(minDouble);
			maxValue = formatter.format(maxDouble);
			sum = sumBD.toString();
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
						minValue = new String(s.toString());
					s.append(' ');
				}
				maxValue = s.toString();
				if (minRawLength == maxRawLength)
					minValue = maxValue;
			}
			else {
				minValue = minString;
				maxValue = maxString;
			}
			break;

		case LOCALDATE:
			if (collectStatistics) {
				DateTimeFormatter dtf = DateTimeParser.ofPattern(matchPatternInfo.format, locale);

				minValue = minLocalDate == null ? null : minLocalDate.format(dtf);
				maxValue = maxLocalDate == null ? null : maxLocalDate.format(dtf);
			}
			break;

		case LOCALTIME:
			if (collectStatistics) {
				DateTimeFormatter dtf = DateTimeParser.ofPattern(matchPatternInfo.format, locale);

				minValue = minLocalTime == null ? null : minLocalTime.format(dtf);
				maxValue = maxLocalTime == null ? null : maxLocalTime.format(dtf);
			}
			break;

		case LOCALDATETIME:
			if (collectStatistics) {
				DateTimeFormatter dtf = DateTimeParser.ofPattern(matchPatternInfo.format, locale);

				minValue = minLocalDateTime == null ? null : minLocalDateTime.format(dtf);
				maxValue = maxLocalDateTime == null ? null : maxLocalDateTime.format(dtf);
			}
			break;

		case ZONEDDATETIME:
			if (collectStatistics) {
				DateTimeFormatter dtf = DateTimeParser.ofPattern(matchPatternInfo.format, locale);

				minValue = minZonedDateTime.format(dtf);
				maxValue = maxZonedDateTime.format(dtf);
			}
			break;

		case OFFSETDATETIME:
			if (collectStatistics) {
				DateTimeFormatter dtf = DateTimeParser.ofPattern(matchPatternInfo.format, locale);

				minValue = minOffsetDateTime.format(dtf);
				maxValue = maxOffsetDateTime.format(dtf);
			}
			break;
		}

		// Attempt to identify keys?
		boolean key = false;
		if (sampleCount > MIN_SAMPLES_FOR_KEY && maxCardinality >= MIN_SAMPLES_FOR_KEY / 2
				&& cardinality.size() >= maxCardinality && blankCount == 0 && nullCount == 0
				&& matchPatternInfo.typeQualifier == null
				&& ((PatternInfo.Type.STRING.equals(matchPatternInfo.type) && minRawLength == maxRawLength && minRawLength < 32)
						|| PatternInfo.Type.LONG.equals(matchPatternInfo.type))) {
			key = true;
			// Might be a key but only iff every element in the cardinality
			// set only has a count of 1
			for (final Map.Entry<String, Integer> entry : cardinality.entrySet()) {
				if (entry.getValue() != 1) {
					key = false;
					break;
				}
			}
		}

		TextAnalysisResult result = new TextAnalysisResult(dataStreamName, matchCount, matchPatternInfo, leadingWhiteSpace,
				trailingWhiteSpace, multiline, sampleCount, nullCount, blankCount, totalLeadingZeros, confidence, minValue,
				maxValue, minRawLength, maxRawLength, sum, utilizedDecimalSeparator, resolutionMode, cardinality, outliers, key, collectStatistics);

		return result;
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
