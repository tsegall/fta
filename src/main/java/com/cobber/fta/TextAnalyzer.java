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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.DateTimeParser.DateResolutionMode;
import com.cobber.fta.plugins.LogicalTypeCAProvince;
import com.cobber.fta.plugins.LogicalTypeCountry;
import com.cobber.fta.plugins.LogicalTypeEmail;
import com.cobber.fta.plugins.LogicalTypeGender;
import com.cobber.fta.plugins.LogicalTypeMonthAbbr;
import com.cobber.fta.plugins.LogicalTypeURL;
import com.cobber.fta.plugins.LogicalTypeUSState;
import com.cobber.fta.plugins.LogicalTypeUSZip5;

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
	public static final int SAMPLE_DEFAULT = 20;
	private int samples = SAMPLE_DEFAULT;

	/** Should we collect statistics (min, max, sum) as we parse the data stream. */
	private boolean collectStatistics = true;

	/** Should we enable Default Logical Type detection. */
	private boolean logicalTypeDetection = true;

	/** Threshold for detection - by default this is unset and sensible defaults are used. */
	private int threshold = -1;

	/** The default value for the maximum Cardinality tracked. */
	public static final int MAX_CARDINALITY_DEFAULT = 500;
	private int maxCardinality = MAX_CARDINALITY_DEFAULT;

	private static final int MIN_SAMPLES_FOR_KEY = 1000;

	/** The default value for the maximum # of outliers tracked. */
	public static final int MAX_OUTLIERS_DEFAULT = 50;
	private int maxOutliers = MAX_OUTLIERS_DEFAULT;

	private static final int REFLECTION_SAMPLES = 30;
	private int reflectionSamples = REFLECTION_SAMPLES;

	private String dataStreamName;
	private DateResolutionMode resolutionMode = DateResolutionMode.None;
	private char decimalSeparator;
	private char monetaryDecimalSeparator;
	private char groupingSeparator;
	private char minusSign;
	private long sampleCount;
	private long nullCount;
	private long blankCount;
	private Map<String, Integer> cardinality = new HashMap<String, Integer>();
	private final Map<String, Integer> outliers = new HashMap<String, Integer>();
	private List<String> raw; // 0245-11-98
	// 0: d{4}-d{2}-d{2} 1: d{+}-d{+}-d{+} 2: d{+}-d{+}-d{+}
	// 0: d{4} 1: d{+} 2: [-]d{+}
	// input "hello world" 0: a{5} a{5} 1: a{+} a{+} 2: a{+}
	private List<StringBuilder>[] levels = new ArrayList[3];

	private long matchCount;
	private PatternInfo matchPatternInfo;

	private boolean trainingStarted;

	private boolean leadingWhiteSpace = false;
	private boolean trailingWhiteSpace = false;

	private double minDouble = Double.MAX_VALUE;
	private double maxDouble = -Double.MAX_VALUE;
	private BigDecimal sumBD = BigDecimal.ZERO;

	private long minLong = Long.MAX_VALUE;
	private long maxLong = Long.MIN_VALUE;
	private BigInteger sumBI = BigInteger.ZERO;

	private String minString;
	private String maxString;

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

	private int minRawLength = Integer.MAX_VALUE;
	private int maxRawLength = Integer.MIN_VALUE;

	private int minTrimmedLength = Integer.MAX_VALUE;
	private int maxTrimmedLength = Integer.MIN_VALUE;

	private int possibleDateTime;
	private long totalLeadingZeros;

	private static Map<String, PatternInfo> patternInfo;
	private static Map<String, PatternInfo> typeInfo;
	private static Map<String, String> promotion;

	private Set<String> registered = new HashSet<String>();
	private ArrayList<LogicalTypeInfinite> infiniteTypes = new ArrayList<LogicalTypeInfinite>();
	private ArrayList<LogicalTypeFinite> finiteTypes = new ArrayList<LogicalTypeFinite>();

	public static final String PATTERN_ANY = ".";
	public static final String PATTERN_ANY_VARIABLE = ".+";
	public static final String PATTERN_ALPHA = "\\p{Alpha}";
	public static final String PATTERN_ALPHA_VARIABLE = PATTERN_ALPHA + "+";
	public static final String PATTERN_ALPHA_2 = PATTERN_ALPHA + "{2}";
	public static final String PATTERN_ALPHA_3 = PATTERN_ALPHA + "{3}";

	public static final String PATTERN_ALPHANUMERIC = "\\p{Alnum}";
	public static final String PATTERN_ALPHANUMERIC_VARIABLE = PATTERN_ALPHANUMERIC + "+";
	public static final String PATTERN_ALPHANUMERIC_2 = PATTERN_ALPHANUMERIC + "{2}";
	public static final String PATTERN_ALPHANUMERIC_3 = PATTERN_ALPHANUMERIC + "{3}";

	public static final String PATTERN_BOOLEAN = "(?i)(true|false)";
	public static final String PATTERN_YESNO = "(?i)(yes|no)";

	public static final String PATTERN_LONG = "\\d+";
	public static final String PATTERN_SIGNED_LONG = "-?\\d+";
	public static final String PATTERN_DOUBLE = PATTERN_LONG + "|" + "(\\d+)?\\.\\d+";
	public static final String PATTERN_SIGNED_DOUBLE = PATTERN_SIGNED_LONG + "|" + "-?(\\d+)?\\.\\d+";
	public static final String PATTERN_DOUBLE_WITH_EXPONENT = PATTERN_LONG + "|" + "(\\d+)?\\.\\d+(?:[eE]([-+]?\\d+))?";
	public static final String PATTERN_SIGNED_DOUBLE_WITH_EXPONENT = PATTERN_SIGNED_LONG + "|" + "-?(\\d+)?\\.\\d+(?:[eE]([-+]?\\d+))?";

	private final Map<String, DateTimeFormatter> formatterCache = new HashMap<String, DateTimeFormatter>();

	private static void addPattern(final Map<String, PatternInfo> map, final boolean patternIsKey, final String regexp, final PatternInfo.Type type,
			final String typeQualifier, final int minLength, final int maxLength, final String generalPattern, final String format) {
		map.put(patternIsKey ? regexp : (type.toString() + "." + typeQualifier), new PatternInfo(regexp, type, typeQualifier, minLength, maxLength, generalPattern, format));
	}

	static {
		patternInfo = new HashMap<String, PatternInfo>();
		typeInfo = new HashMap<String, PatternInfo>();
		promotion = new HashMap<String, String>();

		addPattern(patternInfo, true, PATTERN_BOOLEAN, PatternInfo.Type.BOOLEAN, null, 4, 5, null, "");
		addPattern(patternInfo, true, PATTERN_YESNO, PatternInfo.Type.BOOLEAN, null, 2, 3, null, "");
		addPattern(patternInfo, true, "[0|1]", PatternInfo.Type.BOOLEAN, null, -1, -1, null, null);

		addPattern(patternInfo, true, PATTERN_ANY_VARIABLE, PatternInfo.Type.STRING, null, 1, -1, null, "");
		addPattern(patternInfo, true, PATTERN_ALPHA_VARIABLE, PatternInfo.Type.STRING, null, 1, -1, null, "");
		addPattern(patternInfo, true, PATTERN_ALPHANUMERIC_VARIABLE, PatternInfo.Type.STRING, null, 1, -1, null, "");
		addPattern(patternInfo, true, PATTERN_ALPHA_2, PatternInfo.Type.STRING, null, 2, 2, null, "");
		addPattern(patternInfo, true, PATTERN_ALPHA_3, PatternInfo.Type.STRING, null, 3, 3, null, "");
		addPattern(patternInfo, true, PATTERN_ALPHANUMERIC_2, PatternInfo.Type.STRING, null, 2, 2, null, "");
		addPattern(patternInfo, true, PATTERN_ALPHANUMERIC_3, PatternInfo.Type.STRING, null, 3, 3, null, "");

		addPattern(patternInfo, true, PATTERN_LONG, PatternInfo.Type.LONG, null, 1, -1, null, "");
		addPattern(patternInfo, true, PATTERN_SIGNED_LONG, PatternInfo.Type.LONG, "SIGNED", 1, -1, null, "");
		addPattern(patternInfo, true, PATTERN_DOUBLE, PatternInfo.Type.DOUBLE, null, -1, -1, null, "");
		addPattern(patternInfo, true, PATTERN_SIGNED_DOUBLE, PatternInfo.Type.DOUBLE, "SIGNED", -1, -1, null, "");
		addPattern(patternInfo, true, PATTERN_DOUBLE_WITH_EXPONENT, PatternInfo.Type.DOUBLE, null, -1, -1, null, "");
		addPattern(patternInfo, true, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PatternInfo.Type.DOUBLE, "SIGNED", -1, -1, null, "");

		// Logical Types
		addPattern(typeInfo, false, "[NULL]", PatternInfo.Type.STRING, "NULL", -1, -1, null, null);
		addPattern(typeInfo, false, "\\p{javaWhitespace}*", PatternInfo.Type.STRING, "BLANKORNULL", -1, -1, null, null);
		addPattern(typeInfo, false, "\\p{javaWhitespace}*", PatternInfo.Type.STRING, "BLANK", -1, -1, null, null);

		promotion.put(PATTERN_LONG + "---" + PATTERN_SIGNED_LONG, PATTERN_SIGNED_LONG);
		promotion.put(PATTERN_LONG + "---" + PATTERN_DOUBLE, PATTERN_DOUBLE);
		promotion.put(PATTERN_LONG + "---" + PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE);
		promotion.put(PATTERN_LONG + "---" + PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_LONG + "---" + PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);

		promotion.put(PATTERN_SIGNED_LONG + "---" + PATTERN_LONG, PATTERN_SIGNED_LONG);
		promotion.put(PATTERN_SIGNED_LONG + "---" + PATTERN_DOUBLE, PATTERN_DOUBLE);
		promotion.put(PATTERN_SIGNED_LONG + "---" + PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE);
		promotion.put(PATTERN_SIGNED_LONG + "---" + PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_SIGNED_LONG + "---" + PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);

		promotion.put(PATTERN_DOUBLE + "---" + PATTERN_LONG, PATTERN_DOUBLE);
		promotion.put(PATTERN_DOUBLE + "---" + PATTERN_SIGNED_LONG, PATTERN_DOUBLE);
		promotion.put(PATTERN_DOUBLE + "---" + PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE);
		promotion.put(PATTERN_DOUBLE + "---" + PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_DOUBLE + "---" + PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);

		promotion.put(PATTERN_SIGNED_DOUBLE + "---" + PATTERN_LONG, PATTERN_SIGNED_DOUBLE);
		promotion.put(PATTERN_SIGNED_DOUBLE + "---" + PATTERN_SIGNED_LONG, PATTERN_SIGNED_DOUBLE);
		promotion.put(PATTERN_SIGNED_DOUBLE + "---" + PATTERN_DOUBLE, PATTERN_SIGNED_DOUBLE);
		promotion.put(PATTERN_SIGNED_DOUBLE + "---" + PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_SIGNED_DOUBLE + "---" + PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);

		promotion.put(PATTERN_DOUBLE_WITH_EXPONENT + "---" + PATTERN_LONG, PATTERN_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_DOUBLE_WITH_EXPONENT + "---" + PATTERN_SIGNED_LONG, PATTERN_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_DOUBLE_WITH_EXPONENT + "---" + PATTERN_DOUBLE, PATTERN_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_DOUBLE_WITH_EXPONENT + "---" + PATTERN_SIGNED_DOUBLE, PATTERN_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_DOUBLE_WITH_EXPONENT + "---" + PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);

		promotion.put(PATTERN_SIGNED_DOUBLE_WITH_EXPONENT + "---" + PATTERN_LONG, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_SIGNED_DOUBLE_WITH_EXPONENT + "---" + PATTERN_SIGNED_LONG, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_SIGNED_DOUBLE_WITH_EXPONENT + "---" + PATTERN_DOUBLE, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_SIGNED_DOUBLE_WITH_EXPONENT + "---" + PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_SIGNED_DOUBLE_WITH_EXPONENT + "---" + PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
	}

	/**
	 * Construct a Text Analyzer for the named data stream with the supplied DateResolutionMode.
	 *
	 * @param name The name of the data stream (e.g. the column of the CSV file)
	 * @param resolutionMode Determines what to do when the Date field is ambiguous (i.e. we cannot determine which
	 *   of the fields is the day or the month.  If resolutionMode is DayFirst, then assume day is first, if resolutionMode is
	 *   MonthFirst then assume month is first, if it is None then the pattern returned may have '?' in to represent
	 *   this ambiguity.
	 * @throws IOException
	 *             If an internal error occurred.
	 */
	public TextAnalyzer(final String name, final DateResolutionMode resolutionMode) throws IOException {
		this.dataStreamName = name;
		this.resolutionMode = resolutionMode;
		final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
		decimalSeparator = formatSymbols.getDecimalSeparator();
		monetaryDecimalSeparator = formatSymbols.getMonetaryDecimalSeparator();
		groupingSeparator = formatSymbols.getGroupingSeparator();
		minusSign = formatSymbols.getMinusSign();
	}

	/**
	 * Register a new Logical Type processor.
	 * See {@link LogicalTypeFinite} or {@link LogicalTypeInfinite}
	 *
	 * @param newLogicalType The class representing the new Logical Type
	 * @return Success if the new Logical type was successfully registered.
	 */
	public boolean registerLogicalType(Class<? extends LogicalType> newLogicalType) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot register logical types once training has started");

		Constructor<?> ctor;
		LogicalType object;

		try {
			ctor = newLogicalType.getConstructor();
			object = (LogicalType)ctor.newInstance();

			if (registered.contains(object.getQualifier()))
				throw new IllegalArgumentException("Logical type: " + object.getQualifier() + " already registered.");
			registered.add(object.getQualifier());

			if (object instanceof LogicalTypeInfinite)
				infiniteTypes.add((LogicalTypeInfinite)object);
			else
				finiteTypes.add((LogicalTypeFinite)object);
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			return false;
		}
		return true;
	}

	/**
	 * Construct a Text Analyzer for the named data stream.  Note: The resolution mode will be 'None'.
	 *
	 * @param name The name of the data stream (e.g. the column of the CSV file)
	 *
	 * @throws IOException
	 *             If an internal error occurred.
	 */
	public TextAnalyzer(final String name) throws IOException {
		this(name, DateResolutionMode.None);
	}

	/**
	 * Construct an anonymous Text Analyzer for a data stream.  Note: The resolution mode will be 'None'.
	 *
	 * @throws IOException
	 *             If an internal error occurred.
	 */
	public TextAnalyzer() throws IOException {
		this("anonymous", DateResolutionMode.None);
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
		this.logicalTypeDetection = logicalTypeDetection;
		return ret;
	}

	/**
	 * Indicates whether to enable default Logical Type processing or not.
	 *
	 * @return Whether default Logical Type processing collection is enabled.
	 */
	public boolean getDefaultLogicalTypes() {
		return logicalTypeDetection;
	}

	/**
	 * The percentage when we declare success 0 - 100.
	 * Typically this should not be adjusted, if you want to run in Strict mode then set this to 100.
	 * @param threshold The new threshold used for detection.
	 */
	public void setPluginThreshold(int threshold) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot adjust Threshold once training has started");

		if (threshold < 0 || threshold > 100)
			throw new IllegalArgumentException("Threshold must be between 0 and 100ƒ");

		this.threshold = threshold;
	}

	/**
	 * Get the current detection Threshold.
	 * If not set, this will return -1, this means that each plugin is using a default threshold and doing something sensible.
	 *
	 * @return The current threshold.
	 */
	public int getPluginThreshold() {
		return threshold;
	}

    /**
     * Set the number of Samples to collect before attempting to determine the
     * type. Note: It is not possible to change the Sample Size once training
     * has started.
     * Indicate whether to collect statistics or not.
     *
     * @param samples
     *            The number of samples to collect
     * @return The previous value of this parameter.
	*/
	public int setSampleSize(final int samples) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot change sample size once training has started");
		if (samples < SAMPLE_DEFAULT)
			throw new IllegalArgumentException("Cannot set sample size below " + SAMPLE_DEFAULT);

		final int ret = samples;
		this.samples = samples;

		// Never want the Sample Size to be greater than the Reflection point
		if (samples >= reflectionSamples)
			reflectionSamples = samples + 1;
		return ret;
	}

	/**
	 * Get the number of Samples used to collect before attempting to determine
	 * the type.
	 *
	 * @return The current size of the sample window.
	 */
	public int getSampleSize() {
		return samples;
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
	 * Set the maximum cardinality that will be tracked. Note: It is not
	 * possible to change the cardinality once training has started.
	 *
	 * @param newCardinality
	 *            The maximum Cardinality that will be tracked (0 implies no
	 *            tracking)
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

	private boolean trackLong(final String rawInput, PatternInfo patternInfo, final boolean register) {
		final String input = rawInput.trim();

		// Track String facts - just in case we end up backing out.
		if (minString == null || minString.compareTo(input) > 0) {
			minString = input;
		}
		if (maxString == null || maxString.compareTo(input) < 0) {
			maxString = input;
		}

		long l;

		try {
			l = Long.parseLong(input);
		} catch (NumberFormatException e) {
			return false;
		}

		if (register) {
			if (input.charAt(0) == '0')
				totalLeadingZeros++;

			if (collectStatistics) {
				if (l < minLong) {
					minLong = l;
				}
				if (l > maxLong) {
					maxLong = l;
				}
				final int digits = l < 0 ? input.length() - 1 : input.length();
				if (digits < minTrimmedLength)
					minTrimmedLength = digits;
				if (digits > maxTrimmedLength)
					maxTrimmedLength = digits;

				sumBI = sumBI.add(BigInteger.valueOf(l));
			}
		}

		if (patternInfo.typeQualifier != null) {
			// If it is a registered Infinite Logical Type then validate it
			for (LogicalType logical : infiniteTypes) {
				if (PatternInfo.Type.LONG.equals(logical.getBaseType()) && logical.getQualifier().equals(patternInfo.typeQualifier))
					return logical.isValid(input);
			}
		}

		return true;
	}

	private boolean trackBoolean(final String input) {
		final String trimmedLower = input.trim().toLowerCase(Locale.ROOT);

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

	private boolean trackString(final String input, PatternInfo patternInfo, final boolean register) {
		String cleaned = input;

		if (patternInfo.typeQualifier == null) {
			if (patternInfo.isAlphabetic() && !cleaned.trim().chars().allMatch(Character::isAlphabetic))
				return false;
		}
		else {
			// If it is a registered Infinite Logical Type then validate it
			for (LogicalType logical : infiniteTypes) {
				if (PatternInfo.Type.STRING.equals(logical.getBaseType()) && logical.getQualifier().equals(patternInfo.typeQualifier)) {
					if (!logical.isValid(input))
						return false;
					break;
				}
			}
		}

		return updateStats(cleaned);
	}

	private boolean updateStats(final String cleaned) {
		final int len = cleaned.trim().length();

		if (matchPatternInfo.minLength != -1 && len < matchPatternInfo.minLength)
			return false;
		if (matchPatternInfo.maxLength != -1 && len > matchPatternInfo.maxLength)
			return false;

		if (minString == null || minString.compareTo(cleaned) > 0) {
			minString = cleaned;
		}
		if (maxString == null || maxString.compareTo(cleaned) < 0) {
			maxString = cleaned;
		}

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
			return false;
		}
		return true;
	}

	private boolean trackDouble(final String input, PatternInfo patternInfo, final boolean register) {
		double d;

		try {
			d = Double.parseDouble(input.trim());
		} catch (NumberFormatException e) {
			return false;
		}

		if (patternInfo.typeQualifier != null) {
			// If it is a registered Infinite Logical Type then validate it
			for (LogicalType logical : infiniteTypes) {
				if (PatternInfo.Type.DOUBLE.equals(logical.getBaseType()) && logical.getQualifier().equals(patternInfo.typeQualifier)) {
					if (!logical.isValid(input))
						return false;
					break;
				}
			}
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

	private void trackDateTime(final String dateFormat, final String input) throws DateTimeParseException {
		final DateTimeParserResult result = DateTimeParserResult.asResult(dateFormat, resolutionMode);
		if (result == null) {
			throw new InternalErrorException("NULL result for " + dateFormat);
		}

		// Grab the cached Formatter
		final String formatString = result.getFormatString();
		DateTimeFormatter formatter = formatterCache.get(formatString);
		if (formatter == null) {
			formatter = DateTimeFormatter.ofPattern(formatString);
			formatterCache.put(formatString, formatter);
		}

		final String trimmed = input.trim();

		switch (result.getType()) {
		case LOCALTIME:
			final LocalTime localTime = LocalTime.parse(trimmed, formatter);
			if (collectStatistics) {
				if (minLocalTime == null || localTime.compareTo(minLocalTime) < 0)
					minLocalTime = localTime;
				if (maxLocalTime == null || localTime.compareTo(maxLocalTime) > 0)
					maxLocalTime = localTime;
			}
			break;

		case LOCALDATE:
			final LocalDate localDate = LocalDate.parse(trimmed, formatter);
			if (collectStatistics) {
				if (minLocalDate == null || localDate.compareTo(minLocalDate) < 0)
					minLocalDate = localDate;
				if (maxLocalDate == null || localDate.compareTo(maxLocalDate) > 0)
					maxLocalDate = localDate;
			}
			break;

		case LOCALDATETIME:
			final LocalDateTime localDateTime = LocalDateTime.parse(trimmed, formatter);
			if (collectStatistics) {
				if (minLocalDateTime == null || localDateTime.compareTo(minLocalDateTime) < 0)
					minLocalDateTime = localDateTime;
				if (maxLocalDateTime == null || localDateTime.compareTo(maxLocalDateTime) > 0)
					maxLocalDateTime = localDateTime;
			}
			break;

		case ZONEDDATETIME:
			final ZonedDateTime zonedDataTime = ZonedDateTime.parse(trimmed, formatter);
			if (collectStatistics) {
				if (minZonedDateTime == null || zonedDataTime.compareTo(minZonedDateTime) < 0)
					minZonedDateTime = zonedDataTime;
				if (maxZonedDateTime == null || zonedDataTime.compareTo(maxZonedDateTime) > 0)
					maxZonedDateTime = zonedDataTime;
			}
			break;

		case OFFSETDATETIME:
			final OffsetDateTime offsetDateTime = OffsetDateTime.parse(trimmed, formatter);
			if (collectStatistics) {
				if (minOffsetDateTime == null || offsetDateTime.compareTo(minOffsetDateTime) < 0)
					minOffsetDateTime = offsetDateTime;
				if (maxOffsetDateTime == null || offsetDateTime.compareTo(maxOffsetDateTime) > 0)
					maxOffsetDateTime = offsetDateTime;
			}
			break;

		default:
			throw new InternalErrorException("Expected Date/Time type.");
		}
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
		if (!trainingStarted) {
			raw = new ArrayList<String>(samples);
			levels[0] = new ArrayList<StringBuilder>(samples);
			levels[1] = new ArrayList<StringBuilder>(samples);
			levels[2] = new ArrayList<StringBuilder>(samples);

			if (logicalTypeDetection) {
				// Infinite ...
				registerLogicalType(LogicalTypeURL.class);
				registerLogicalType(LogicalTypeEmail.class);
				registerLogicalType(LogicalTypeUSZip5.class);
				// Finite - Variable length ...
				registerLogicalType(LogicalTypeGender.class);
				registerLogicalType(LogicalTypeCountry.class);
				// Finite - Fixed length ...
				registerLogicalType(LogicalTypeMonthAbbr.class);
				registerLogicalType(LogicalTypeCAProvince.class);
				registerLogicalType(LogicalTypeUSState.class);
			}

			// Run the initializers for the Logical Types
			for (LogicalType logical : infiniteTypes)
				logical.initialize();
			for (LogicalType logical : finiteTypes)
				logical.initialize();

			if (threshold != -1) {
				// Set the threshold for all Logical Types
				for (LogicalType logical : infiniteTypes)
					logical.setThreshold(threshold);
				for (LogicalType logical : finiteTypes)
					logical.setThreshold(threshold);
			}

			trainingStarted = true;
		}

		sampleCount++;

		PatternInfo.Type matchType = matchPatternInfo != null ? matchPatternInfo.type : null;

		if (rawInput == null) {
			nullCount++;
			return matchType != null;
		}

		final String input = rawInput.trim();

		final int length = input.length();

		if (length == 0) {
			blankCount++;
			trackLength(rawInput);
			return matchType != null;
		}

		trackResult(rawInput);

		// If we have determined a type, no need to further train
		if (matchPatternInfo != null && matchPatternInfo.type != null)
			return true;

		raw.add(rawInput);

		final StringBuilder l0 = new StringBuilder(length);

		// Walk the string
		boolean numericSigned = false;
		int numericDecimalSeparators = 0;
		boolean couldBeNumeric = true;
		int possibleExponentSeen = -1;
		int digitsSeen = 0;
		int alphasSeen = 0;
		int[] charCounts = new int[128];
		int[] lastIndex = new int[128];

		for (int i = 0; i < length; i++) {
			char ch = input.charAt(i);

			// Track counts and last occurrence for simple characters
			if (ch <= 127) {
				charCounts[ch]++;
				lastIndex[ch] = i;
			}

			if (i == 0 && ch == minusSign) {
				numericSigned = true;
			} else if (Character.isDigit(ch)) {
				l0.append('d');
				digitsSeen++;
			} else if (ch == decimalSeparator) {
				l0.append('D');
				numericDecimalSeparators++;
			} else if (ch == groupingSeparator) {
				l0.append('G');
			} else if (Character.isAlphabetic(ch)) {
				l0.append('a');
				alphasSeen++;
				if (couldBeNumeric && (ch == 'e' || ch == 'E')) {
					if (possibleExponentSeen != -1 || i < 3 || i + 1 >= length)
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

		final StringBuilder compressedl0 = new StringBuilder(length);
		if (alphasSeen != 0 && digitsSeen != 0 && alphasSeen + digitsSeen == length) {
			compressedl0.append(PATTERN_ALPHANUMERIC).append('{').append(String.valueOf(length)).append('}');

		} else if ("true".equalsIgnoreCase(input) || "false".equalsIgnoreCase(input)) {
			compressedl0.append(PATTERN_BOOLEAN);
		} else if ("yes".equalsIgnoreCase(input) || "no".equalsIgnoreCase(input)) {
			compressedl0.append(PATTERN_YESNO);
		} else {
			// Walk the new level0 to create the new level1
			final String l0withSentinel = l0.toString() + "|";
			char last = l0withSentinel.charAt(0);
			int repetitions = 1;
			for (int i = 1; i < l0withSentinel.length(); i++) {
				final char ch = l0withSentinel.charAt(i);
				if (ch == last) {
					repetitions++;
				} else {
					if (last == 'd' || last == 'a') {
						compressedl0.append(last == 'd' ? "\\d" : PATTERN_ALPHA);
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

		if (DateTimeParser.determineFormatString(input, resolutionMode) != null)
			possibleDateTime++;

		// Check to see if this input is one of our registered Logical Types
		for (LogicalTypeInfinite logical : infiniteTypes)
			logical.isCandidate(input, compressedl0, charCounts, lastIndex);

		// Create the level 1 and 2
		if (digitsSeen > 0 && couldBeNumeric && numericDecimalSeparators <= 1) {
			StringBuilder l1 = null;
			StringBuilder l2 = null;
			if (numericDecimalSeparators == 1) {
				if (possibleExponentSeen == -1) {
					l1 = new StringBuilder(numericSigned ? PATTERN_SIGNED_DOUBLE : PATTERN_DOUBLE);
					l2 = new StringBuilder(PATTERN_SIGNED_DOUBLE);
				}
				else {
					l1 = new StringBuilder(numericSigned ? PATTERN_SIGNED_DOUBLE_WITH_EXPONENT : PATTERN_DOUBLE_WITH_EXPONENT);
					l2 = new StringBuilder(PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
				}
			}
			else {
				l1 = new StringBuilder(numericSigned ? PATTERN_SIGNED_LONG : PATTERN_LONG);
				l2 = new StringBuilder(PATTERN_SIGNED_LONG);
			}
			levels[1].add(l1);
			levels[2].add(l2);
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
			final PatternInfo found = patternInfo.get(compressedl0.toString());
			if (found != null && found.generalPattern != null) {
				levels[1].add(new StringBuilder(found.generalPattern));
				levels[2].add(new StringBuilder(collapsed));
			} else {
				levels[1].add(new StringBuilder(collapsed));
				levels[2].add(new StringBuilder(PATTERN_ANY_VARIABLE));
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
				bestPattern = patternInfo.get(best.getKey());
			}
			else if (secondBest == null) {
				secondBest = entry;
				secondBestPattern = patternInfo.get(secondBest.getKey());
				if (levelIndex != 0 && bestPattern != null && secondBestPattern != null &&
						bestPattern.isNumeric() && secondBestPattern.isNumeric()) {
					newKey = promotion.get(bestPattern.regexp + "---" + secondBestPattern.regexp);
					best = new AbstractMap.SimpleEntry<String, Integer>(newKey, best.getValue() + secondBest.getValue());
				}
			}
			else if (thirdBest == null) {
				thirdBest = entry;
				thirdBestPattern = patternInfo.get(thirdBest.getKey());
				if (levelIndex != 0 && bestPattern != null && secondBestPattern != null && thirdBestPattern != null &&
						bestPattern.isNumeric() && secondBestPattern.isNumeric() && thirdBestPattern.isNumeric()) {
					newKey = promotion.get(newKey + "---" + thirdBestPattern.regexp);
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
			matchPatternInfo = patternInfo.get(PATTERN_ANY_VARIABLE);
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
			level0patternInfo = patternInfo.get(level0pattern);
		}
		if (level1 != null) {
			level1pattern = level1.getKey();
			level1value = level1.getValue();
			level1patternInfo = patternInfo.get(level1pattern);
		}
		if (level2 != null) {
			level2pattern = level2.getKey();
			level2value = level2.getValue();
			level2patternInfo = patternInfo.get(level2pattern);
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
			// - we have different keys, different types but an improvement of
			// at least 10%
			if (level2 != null && (matchPatternInfo == null
					|| (best.getKey().equals(level2pattern) && level2value > best.getValue())
					|| (!best.getKey().equals(level2pattern) && level2patternInfo != null
							&& matchPatternInfo.type.equals(level2patternInfo.type)
							&& level2.getValue() > best.getValue())
					|| (!best.getKey().equals(level2pattern) && level2.getValue() > best.getValue() + samples / 10))) {
				best = level2;
				matchPatternInfo = level2patternInfo;
			}

			if (possibleDateTime == raw.size()) {
				final DateTimeParser det = new DateTimeParser(resolutionMode);
				for (final String sample : raw)
					det.train(sample);

				final DateTimeParserResult result = det.getResult();
				final String formatString = result.getFormatString();
				matchPatternInfo = new PatternInfo(result.getRegExp(), result.getType(), formatString, -1, -1, null,
						formatString);
			}

			// If it is a registered Infinite Logical Type then validate it
			for (LogicalTypeInfinite logical : infiniteTypes) {
				if (logical.getCandidateCount() == raw.size()) {
					int count = 0;
					PatternInfo candidate = new PatternInfo(logical.getRegexp(), logical.getBaseType(), logical.getQualifier(), -1, -1, null, null);
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
			}

			for (final String sample : raw)
				trackResult(sample);

			matchCount = best.getValue();
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

	private int outlier(final String input) {
		final Integer seen = outliers.get(input);
		if (seen == null) {
			if (outliers.size() < maxOutliers)
				outliers.put(input, 1);
		} else {
			outliers.put(input, seen + 1);
		}

		return outliers.size();
	}

	private boolean conditionalBackoutToPattern(final long realSamples, PatternInfo current) {
		int alphas = 0;
		int digits = 0;
		int spaces = 0;
		int other = 0;
		int doubles = 0;
		boolean negative = false;
		boolean exponent = false;

		// Sweep the current outliers
		for (final Map.Entry<String, Integer> entry : outliers.entrySet()) {
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
			for (int c : key.codePoints().toArray()) {
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
		}

		int badCharacters = current.isAlphabetic() ? digits : alphas;
		// If we are currently Alphabetic and the only errors are digits then convert to AlphaNumeric
		if (badCharacters != 0 && spaces == 0 && other == 0 && current.isAlphabetic()) {
			if (outliers.size() == maxOutliers || digits > .01 * realSamples) {
				backoutToPattern(realSamples, current.regexp.replace("Alpha", "Alnum"));
				return true;
			}
		}
		// If we are currently Numeric and the only errors are alpha then convert to AlphaNumeric
		else if (badCharacters != 0 && spaces == 0 && other == 0 && PatternInfo.Type.LONG.equals(current.type)) {
			if (outliers.size() == maxOutliers || alphas > .01 * realSamples) {
				backoutToPattern(realSamples, "\\p{Alnum}" + Utils.lengthQualifier(minRawLength, maxRawLength));
				return true;
			}
		}
		// If we are currently Numeric and the only errors are doubles then convert to double
		else if (outliers.size() == doubles && PatternInfo.Type.LONG.equals(current.type)) {
			backoutToPattern(realSamples, doublePattern(negative, exponent));
			return true;
		}
		else {
			if (outliers.size() == maxOutliers || (badCharacters + spaces + other) > .01 * realSamples) {
				backoutToPattern(realSamples, PATTERN_ANY_VARIABLE);
				return true;
			}
		}

		return false;
	}

	private String doublePattern(boolean negative, boolean exponent) {
		if (exponent)
			return negative ? PATTERN_SIGNED_DOUBLE_WITH_EXPONENT : PATTERN_DOUBLE_WITH_EXPONENT;

		return negative ? PATTERN_SIGNED_DOUBLE : PATTERN_DOUBLE;
	}

	private void backoutToPattern(final long realSamples, String newPattern) {
		matchCount = realSamples;
		matchPatternInfo = patternInfo.get(newPattern);

		// If it is not one of our known types then construct a suitable PatternInfo
		if (matchPatternInfo == null)
			matchPatternInfo = new PatternInfo(newPattern, PatternInfo.Type.STRING, null, -1, -1, null, null);

		// All outliers are now part of the cardinality set and there are now no outliers
		cardinality.putAll(outliers);

		// Need to update stats to reflect any outliers we previously ignored
		if (matchPatternInfo.type.equals(PatternInfo.Type.STRING))
			for (final String key : outliers.keySet())
				updateStats(key);
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
	}

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

		if ((double) matchCount / realSamples > 0.9)
			matchPatternInfo = patternInfo.get(PATTERN_LONG);
		else
			backoutToPattern(realSamples, PATTERN_ANY_VARIABLE);
	}

	private void trackLength(final String input) {
		// We always want to track basic facts for the field
		final int length = input.length();

		if (length != 0 && length < minRawLength)
			minRawLength = length;
		if (length > maxRawLength)
			maxRawLength = length;
	}

	private void trackTrimmedLengthAndWhiteSpace(final String input) {
		// We always want to track basic facts for the field
		final int length = input.length();

		// Determine if there is leading or trailing White space (if not done previously)
		if (length != 0 && (!leadingWhiteSpace || !trailingWhiteSpace)) {
			leadingWhiteSpace = Character.isSpaceChar(input.charAt(0));
			if (length >= 2 && !trailingWhiteSpace) {
				boolean maybe = Character.isSpaceChar(input.charAt(length - 1));
				if (maybe) {
					int i = length - 2;
					while (i >= 0) {
						if (!Character.isSpaceChar(input.charAt(i))) {
							trailingWhiteSpace = true;
							break;
						}
						i--;
					}
				}
			}
		}

		final int trimmedLength = input.trim().length();

		if (trimmedLength < minRawLength)
			minRawLength = trimmedLength;
		if (trimmedLength > maxRawLength)
			maxRawLength = trimmedLength;
	}

	/**
	 * Track the supplied raw input, once we have enough samples attempt to determine the type.
	 * @param input The raw input string
	 */
	private void trackResult(final String input) {

		trackLength(input);

		// If the cache is full and we have not determined a type compute one
		if ((matchPatternInfo == null || matchPatternInfo.type == null) && sampleCount - (nullCount + blankCount) > samples)
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
				DateTimeParserResult result = DateTimeParserResult.asResult(matchPatternInfo.format, resolutionMode);
				try {
					result.parse(input);
				}
				catch (DateTimeParseException e) {
					if ("Insufficient digits in input (d)".equals(e.getMessage()) || "Insufficient digits in input (M)".equals(e.getMessage())) {
						try {
							final String formatString = new StringBuffer(matchPatternInfo.format).deleteCharAt(e.getErrorIndex()).toString();
							result = DateTimeParserResult.asResult(formatString, resolutionMode);
							matchPatternInfo = new PatternInfo(result.getRegExp(), matchPatternInfo.type, formatString, -1, -1, null,
									formatString);

							trackDateTime(matchPatternInfo.format, input);
							matchCount++;
							addValid(input);
							valid = true;
						}
						catch (DateTimeParseException e2) {
							// Ignore and record as outlier below
						}
					}
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
					for (LogicalType logical : infiniteTypes) {
						if (logical.getQualifier().equals(matchPatternInfo.typeQualifier) && "US_ZIP5".equals(matchPatternInfo.typeQualifier))
							backoutLogicalLongType(logical, realSamples);
						else if (PatternInfo.Type.STRING.equals(matchPatternInfo.type) && matchPatternInfo.typeQualifier != null)
							backoutToPattern(realSamples, PATTERN_ANY_VARIABLE);
					}
				}
				else if (PatternInfo.Type.STRING.equals(matchPatternInfo.type) && matchPatternInfo.isAlphabetic()) {
					// Need to evaluate if we got this wrong
					conditionalBackoutToPattern(realSamples, matchPatternInfo);
				}
			}
		}
	}

	/**
	 * Parse a String regexp to determine length.
	 *
	 * @param input
	 *            String input that must be either a variable length string
	 *            (\\p{Alpha}+) or fixed length, e.g. \\p{Alpha}{3}
	 * @return The length of the input string or -1 if length is variable
	 */
	private int determineLength(final String input) {
		final int len = input.length();
		if (len > 0 && (input.charAt(len - 1) == '+' || input.charAt(len - 1) == '*') || input.indexOf(',') != -1)
			return -1;
		final int lengthInformation = input.lastIndexOf('{');
		if (lengthInformation == -1)
			return -1;
		final String lengthString = input.substring(lengthInformation + 1, len - 1);
		return Integer.parseInt(lengthString);
	}

	/**
	 * Determine if the current dataset reflects a logical type (of uniform length).
	 * @param logical The Logical type we are testing
	 * @return True if we believe that this data set is defined by the provided set
	 */
	private boolean checkUniformLengthSet(LogicalTypeFinite logical) {
		final long realSamples = sampleCount - (nullCount + blankCount);
		long misses = 0;					// count of number of groups that are misses
		long missCount = 0;				// count of number of misses

		// Sweep the current outliers and check they are part of the set
		for (final Map.Entry<String, Integer> entry : outliers.entrySet()) {
			misses++;
			missCount += entry.getValue();
			// Break out early if we know we are going to fail
			if ((double) missCount / realSamples > .05)
				return false;
		}

		// Sweep the balance and check they are part of the set
		long validCount = 0;
		final Map<String, Integer> newOutliers = new HashMap<String, Integer>();
		if ((double) missCount / realSamples <= .05) {
			for (final Map.Entry<String, Integer> entry : cardinality.entrySet()) {
				if (logical.isValid(entry.getKey().trim().toUpperCase(Locale.ROOT)))
					validCount += entry.getValue();
				else {
					misses++;
					missCount += entry.getValue();
					newOutliers.put(entry.getKey(), entry.getValue());
					// Break out early if we know we are going to fail
					if (newOutliers.size() > 1 && (double) missCount / realSamples > .05)
						return false;
				}
			}
		}

		// To declare success we need fewer than 5% failures by count and additionally fewer than 4 groups
		if (((double) missCount / realSamples > .05 || misses >= 4))
				return false;

		matchCount = validCount;
		matchPatternInfo = new PatternInfo(logical.getRegexp(), PatternInfo.Type.STRING, logical.getQualifier(), -1, -1, null, null);
		outliers.putAll(newOutliers);
		cardinality.keySet().removeAll(newOutliers.keySet());

		return true;
	}

	/**
	 * Determine if the current dataset reflects a logical type (of variable length).
	 * @param cardinalityUpper The cardinality set but reduced to ignore case
	 * @param logical The Logical type we are testing
	 * @return True if we believe that this data set is defined by the provided by this Logical Type
	 */
	private boolean checkVariableLengthSet(Map<String, Integer> cardinalityUpper, LogicalTypeFinite logical) { 
		final long realSamples = sampleCount - (nullCount + blankCount);
		final Map<String, Integer> newOutliers = new HashMap<String, Integer>();
		long validCount = 0;
		long misses = 0;				// count of number of groups that are misses
		long missCount = 0;				// count of number of misses

		// Sweep the balance and check they are part of the set
		for (final Map.Entry<String, Integer> entry : cardinalityUpper.entrySet()) {
			if (logical.isValid(entry.getKey()))
				validCount += entry.getValue();
			else {
				misses++;
				missCount += entry.getValue();
				// Break out early if we know we are going to fail
				// To declare success we need fewer than 40% failures by count and also a limited number of misses by group
				if ((double) missCount / realSamples > .4 || misses > (long)Math.sqrt(logical.getSize()))
					return false;
				newOutliers.put(entry.getKey(), entry.getValue());
			}
		}

		// To declare success we need fewer than 40% failures by count and also a limited number of misses by group
		if ((double) missCount / realSamples > .4 || misses > (long)Math.sqrt(logical.getSize()))
			return false;

		outliers.putAll(newOutliers);
		cardinalityUpper.keySet().removeAll(newOutliers.keySet());
		matchCount = validCount;
		matchPatternInfo = new PatternInfo(logical.getRegexp(), PatternInfo.Type.STRING, logical.getQualifier(), -1, -1, null, null);
		cardinality = cardinalityUpper;
		return true;
	}

	/**
	 * Determine the result of the training complete to date. Typically invoked
	 * after all training is complete, but may be invoked at any stage.
	 *
	 * @return A TextAnalysisResult with the analysis of any training completed.
	 */
	public TextAnalysisResult getResult() {
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
				matchPatternInfo = typeInfo.get(PatternInfo.Type.STRING.toString() + "." + "NULL");
			else if (blankCount == sampleCount)
				matchPatternInfo = typeInfo.get(PatternInfo.Type.STRING.toString() + "." + "BLANK");
			else
				matchPatternInfo = typeInfo.get(PatternInfo.Type.STRING.toString() + "." + "BLANKORNULL");
			matchCount = sampleCount;
			confidence = sampleCount >= 10 ? 1.0 : 0.0;
		}
		else {
			confidence = (double) matchCount / realSamples;
		}

		// Do we need to back out from any of our Infinite type determinations
		if (matchPatternInfo.typeQualifier != null)
			for (LogicalType logical : infiniteTypes) {
				String newPattern;
				if (matchPatternInfo.typeQualifier.equals(logical.getQualifier()) &&
						(newPattern = logical.shouldBackout(matchCount, realSamples, cardinality, outliers)) != null) {
					if (PatternInfo.Type.STRING.equals(logical.getBaseType()))
						backoutToPattern(realSamples, newPattern);
					else if (PatternInfo.Type.LONG.equals(logical.getBaseType()))
						backoutLogicalLongType(logical, realSamples);
					confidence = (double) matchCount / realSamples;
				}
			}

		if (PATTERN_LONG.equals(matchPatternInfo.regexp)) {
			if (matchPatternInfo.typeQualifier == null && minLong < 0)
				matchPatternInfo = patternInfo.get(PATTERN_SIGNED_LONG);

			if (minLong > 19000101 && maxLong < 20400101 &&
					((realSamples >= reflectionSamples && cardinality.size() > 10) || dataStreamName.toLowerCase(Locale.ROOT).contains("date"))) {
				matchPatternInfo = new PatternInfo("\\d{8}", PatternInfo.Type.LOCALDATE, "yyyyMMdd", 8, 8, null, "yyyyMMdd");
				DateTimeFormatter dtf = DateTimeFormatter.ofPattern(matchPatternInfo.format);
				minLocalDate = LocalDate.parse(String.valueOf(minLong), dtf);
				maxLocalDate = LocalDate.parse(String.valueOf(maxLong), dtf);
			} else if (minLong > 1800 && maxLong < 2030 &&
					((realSamples >= reflectionSamples && cardinality.size() > 10) || dataStreamName.toLowerCase(Locale.ROOT).contains("year") || dataStreamName.toLowerCase(Locale.ROOT).contains("date"))) {
				matchPatternInfo = new PatternInfo("\\d{4}", PatternInfo.Type.LOCALDATE, "yyyy", 4, 4, null, "yyyy");
				minLocalDate = LocalDate.of((int)minLong, 1, 1);
				maxLocalDate = LocalDate.of((int)maxLong, 1, 1);
			} else if (cardinality.size() == 2 && minLong == 0 && maxLong == 1) {
				// boolean by any other name
				minBoolean = "0";
				maxBoolean = "1";
				matchPatternInfo = patternInfo.get("[0|1]");
			} else {
				String newPattern = matchPatternInfo.typeQualifier != null ? "-?\\d{" : "\\d{";
				newPattern += minTrimmedLength;
				if (minTrimmedLength != maxTrimmedLength)
					newPattern += "," + maxTrimmedLength;
				newPattern += "}";
				matchPatternInfo = new PatternInfo(newPattern, matchPatternInfo.type, matchPatternInfo.typeQualifier, -1, -1, null, null);

				if (realSamples >= reflectionSamples && confidence < 0.96) {
					// We thought it was an integer field, but on reflection it does not feel like it
					conditionalBackoutToPattern(realSamples, matchPatternInfo);
					confidence = 1.0;
				}
			}
		} else if (PATTERN_DOUBLE.equals(matchPatternInfo.regexp)) {
			if (matchPatternInfo.typeQualifier == null && minDouble < 0.0)
				matchPatternInfo = patternInfo.get(PATTERN_SIGNED_DOUBLE);
		} else if (PatternInfo.Type.STRING.equals(matchPatternInfo.type)) {
			final int length = determineLength(matchPatternInfo.regexp);
			// We thought it was a fixed length string, but on reflection it does not feel like it
			if (length != -1 && realSamples >= reflectionSamples && (double) matchCount / realSamples < 0.95) {
				backoutToPattern(realSamples, PATTERN_ANY_VARIABLE);
				confidence = (double) matchCount / realSamples;
			}

			// Build Cardinality map ignoring case (and white space)
			int minKeyLength = Integer.MAX_VALUE;
			int maxKeyLength = 0;
			Map<String, Integer> cardinalityUpper = new HashMap<String, Integer>();
			for (final Map.Entry<String, Integer> entry : cardinality.entrySet()) {
				String key = entry.getKey().toUpperCase(Locale.ROOT).trim();
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
						if (!typeIdentified && realSamples >= reflectionSamples && cardinalityUpper.size() > 1
						&& cardinalityUpper.size() <= logical.getSize() + 2) {
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
						if (!typeIdentified && realSamples >= reflectionSamples && cardinalityUpper.size() > 1
						&& cardinalityUpper.size() <= logical.getSize() + 1) {
							typeIdentified = checkVariableLengthSet(cardinalityUpper, logical);
							if (typeIdentified) {
								confidence = (double) matchCount / realSamples;
								break;
							}
						}
				}
			}

			// Do we need to back out from any of our Finite type determinations
			if (matchPatternInfo.typeQualifier != null)
				for (LogicalTypeFinite logical : finiteTypes) {
					String newPattern;
					if (matchPatternInfo.typeQualifier.equals(logical.getQualifier()) &&
							(newPattern = logical.shouldBackout(matchCount, realSamples, cardinalityUpper, outliers)) != null) {
						if (PatternInfo.Type.STRING.equals(logical.getBaseType()))
							backoutToPattern(realSamples, newPattern);
						else if (PatternInfo.Type.LONG.equals(logical.getBaseType()))
							backoutLogicalLongType(logical, realSamples);
						confidence = (double) matchCount / realSamples;
						typeIdentified = false;
						break;
					}
				}

			// Need to evaluate if we got the type wrong
			if (!typeIdentified && PatternInfo.Type.STRING.equals(matchPatternInfo.type) && matchPatternInfo.isAlphabetic()) {
				conditionalBackoutToPattern(realSamples, matchPatternInfo);
				confidence = (double) matchCount / realSamples;
			}

			// Qualify Alpha or Alnum with a min and max length
			if (!typeIdentified && (PATTERN_ALPHA_VARIABLE.equals(matchPatternInfo.regexp) || PATTERN_ALPHANUMERIC_VARIABLE.equals(matchPatternInfo.regexp))) {
				String newPattern = matchPatternInfo.regexp;
				newPattern = newPattern.substring(0, newPattern.length() - 1) + "{" + minTrimmedLength;
				if (minTrimmedLength != maxTrimmedLength)
					newPattern += "," + maxTrimmedLength;
				newPattern += "}";
				matchPatternInfo = new PatternInfo(newPattern, PatternInfo.Type.STRING, matchPatternInfo.typeQualifier, minTrimmedLength, maxTrimmedLength, null,
						null);
			}

			// Qualify random string with a min and max length
			if (!typeIdentified && PATTERN_ANY_VARIABLE.equals(matchPatternInfo.regexp)) {
				String newPattern = PATTERN_ANY + Utils.lengthQualifier(minRawLength, maxRawLength);
				matchPatternInfo = new PatternInfo(newPattern, PatternInfo.Type.STRING, matchPatternInfo.typeQualifier, minRawLength, maxRawLength, null,
						null);
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
			minValue = String.valueOf(minDouble);
			maxValue = String.valueOf(maxDouble);
			sum = sumBD.toString();
			break;

		case STRING:
			if ("NULL".equals(matchPatternInfo.typeQualifier)) {
				minRawLength = maxRawLength = 0;
			} else if ("BLANK".equals(matchPatternInfo.typeQualifier)) {
				// If all the fields are blank - then we have not saved any of the raw input, so we
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
				minValue = minLocalDate.format(DateTimeFormatter.ofPattern(matchPatternInfo.format));
				maxValue = maxLocalDate.format(DateTimeFormatter.ofPattern(matchPatternInfo.format));
			}
			break;

		case LOCALTIME:
			if (collectStatistics) {
				minValue = minLocalTime.format(DateTimeFormatter.ofPattern(matchPatternInfo.format));
				maxValue = maxLocalTime.format(DateTimeFormatter.ofPattern(matchPatternInfo.format));
			}
			break;

		case LOCALDATETIME:
			if (collectStatistics) {
				minValue = minLocalDateTime == null ? null : minLocalDateTime.format(DateTimeFormatter.ofPattern(matchPatternInfo.format));
				maxValue = maxLocalDateTime == null ? null : maxLocalDateTime.format(DateTimeFormatter.ofPattern(matchPatternInfo.format));
			}
			break;

		case ZONEDDATETIME:
			if (collectStatistics) {
				minValue = minZonedDateTime.format(DateTimeFormatter.ofPattern(matchPatternInfo.format));
				maxValue = maxZonedDateTime.format(DateTimeFormatter.ofPattern(matchPatternInfo.format));
			}
			break;

		case OFFSETDATETIME:
			if (collectStatistics) {
				minValue = minOffsetDateTime.format(DateTimeFormatter.ofPattern(matchPatternInfo.format));
				maxValue = maxOffsetDateTime.format(DateTimeFormatter.ofPattern(matchPatternInfo.format));
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
		return new TextAnalysisResult(dataStreamName, matchCount, matchPatternInfo, leadingWhiteSpace, trailingWhiteSpace, sampleCount,
				nullCount, blankCount, totalLeadingZeros, confidence, minValue, maxValue, minRawLength, maxRawLength, sum,
				cardinality, outliers, key);
	}

	/**
	 * Access the training set - this will typically be the first {@link #SAMPLE_DEFAULT} records.
	 *
	 * @return A List of the raw input strings.
	 */
	public List<String>getTrainingSet() {
		return raw;
	}
}
