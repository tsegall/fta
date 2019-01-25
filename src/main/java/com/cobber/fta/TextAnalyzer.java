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
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
	public static final int SAMPLE_DEFAULT = 20;
	private int samples = SAMPLE_DEFAULT;

	/** Should we collect statistics (min, max, sum) as we parse the data stream. */
	private boolean collectStatistics = true;

	/** Should we enable Default Logical Type detection. */
	private boolean logicalTypeDetection = true;

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

	private static final int REFLECTION_SAMPLES = 30;
	private int reflectionSamples = REFLECTION_SAMPLES;

	private String dataStreamName;
	private DateResolutionMode resolutionMode = DateResolutionMode.None;
	private char decimalSeparator;
	private char monetaryDecimalSeparator;
	private NumberFormat longFormatter;
	private NumberFormat doubleFormatter;
	private char groupingSeparator;
	private char minusSign;
	private long sampleCount;
	private long nullCount;
	private long blankCount;
	private Map<String, Integer> cardinality = new HashMap<String, Integer>();
	private final Map<String, Integer> outliers = new HashMap<String, Integer>();
	private final Map<String, Integer> outliersCompressed = new HashMap<String, Integer>();
	private List<String> raw; // 0245-11-98
	// 0: d{4}-d{2}-d{2} 1: d{+}-d{+}-d{+} 2: d{+}-d{+}-d{+}
	// 0: d{4} 1: d{+} 2: [-]d{+}
	// input "hello world" 0: a{5} a{5} 1: a{+} a{+} 2: a{+}
	private List<StringBuilder>[] levels = new ArrayList[3];

	private long matchCount;
	private PatternInfo matchPatternInfo;

	private boolean trainingStarted;
	private boolean initialized;

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

	// The minimum length (inclusive of spaces)
	private int minRawLength = Integer.MAX_VALUE;
	// The maximum length (inclusive of spaces)
	private int maxRawLength = Integer.MIN_VALUE;

	private int minTrimmedLength = Integer.MAX_VALUE;
	private int maxTrimmedLength = Integer.MIN_VALUE;

	private int minTrimmedOutlierLength = Integer.MAX_VALUE;
	private int maxTrimmedOutlierLength = Integer.MIN_VALUE;

	private int possibleDateTime;
	private long totalLeadingZeros;
	private long groupingSeparators;

	private Map<String, LogicalType> registered = new HashMap<String, LogicalType>();
	private ArrayList<LogicalTypeInfinite> infiniteTypes = new ArrayList<LogicalTypeInfinite>();
	private int[] candidateCounts;
	private ArrayList<LogicalTypeFinite> finiteTypes = new ArrayList<LogicalTypeFinite>();

	private KnownPatterns knownPatterns = new KnownPatterns();

	private DateTimeParser dateTimeParser;


	/**
	 * Construct a Text Analyzer for the named data stream with the supplied DateResolutionMode.
	 *
	 * @param name The name of the data stream (e.g. the column of the CSV file)
	 * @param resolutionMode Determines what to do when the Date field is ambiguous (i.e. we cannot determine which
	 *   of the fields is the day or the month.  If resolutionMode is DayFirst, then assume day is first, if resolutionMode is
	 *   MonthFirst then assume month is first, if it is None then the pattern returned may have '?' in to represent
	 *   this ambiguity.
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
		LogicalType object;

		try {
			newLogicalType = Class.forName(className);
			ctor = newLogicalType.getConstructor();
			object = (LogicalType)ctor.newInstance();

			if (!(object instanceof LogicalType))
				throw new IllegalArgumentException("Logical type: " + className + " does not appear to be a Logical Type.");

			if (registered.containsKey(object.getQualifier()))
				throw new IllegalArgumentException("Logical type: " + object.getQualifier() + " already registered.");

			registered.put(object.getQualifier(), object);

			if (object instanceof LogicalTypeInfinite)
				infiniteTypes.add((LogicalTypeInfinite)object);
			else
				finiteTypes.add((LogicalTypeFinite)object);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
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
     * @param samples
     *            The number of samples to collect
     * @return The previous value of this parameter.
	*/
	public int setDetectWindow(final int samples) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot change sample size once training has started");
		if (samples < SAMPLE_DEFAULT)
			throw new IllegalArgumentException("Cannot set sample size below " + SAMPLE_DEFAULT);

		final int ret = samples;
		this.samples = samples;

		// Never want the Detect Window to be greater than the Reflection point
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

		// Interpret the String as a long, first attempt uses parseLong which is fast, if that fails, then try
		// using a NumberFormatter which will cope with grouping separators (e.g. 1,000).
		try {
			l = Long.parseLong(input);
		} catch (NumberFormatException e) {
			ParsePosition pos = new ParsePosition(0);
			Number n = longFormatter.parse(input, pos);
			if (n == null || input.length() != pos.getIndex())
				return false;
			l = n.longValue();
			if (input.indexOf(groupingSeparator) != -1)
				groupingSeparators++;
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
		if (patternInfo.typeQualifier == null) {
			if (patternInfo.isAlphabetic() && !rawInput.trim().chars().allMatch(Character::isAlphabetic))
				return false;
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

	private void trackDateTime(final String dateFormat, final String input) throws DateTimeParseException {
		final DateTimeParserResult result = DateTimeParserResult.asResult(dateFormat, resolutionMode, locale);
		if (result == null) {
			throw new InternalErrorException("NULL result for " + dateFormat);
		}

		DateTimeFormatter formatter = LocaleInfo.getFormatter(result.getFormatString(), locale);

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

	private void initialize() {
		Calendar cal = GregorianCalendar.getInstance(locale);
		if (!(cal instanceof GregorianCalendar))
			throw new IllegalArgumentException("No support for locales that do not use the Gregorian Calendar");

		if (!NumberFormat.getNumberInstance(locale).format(0).matches("\\d"))
			throw new IllegalArgumentException("No support for locales that do not use Arabic numerals");

		raw = new ArrayList<String>(samples);
		levels[0] = new ArrayList<StringBuilder>(samples);
		levels[1] = new ArrayList<StringBuilder>(samples);
		levels[2] = new ArrayList<StringBuilder>(samples);

		if (logicalTypeDetection) {
			// Load the default set of plugins for Logical Type detection
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/plugins.txt")))){
				String line = null;

				while ((line = reader.readLine()) != null) {
					if (line.trim().length() == 0 || line.charAt(0) == '#')
						continue;
					registerLogicalType(line);
				}
			} catch (IOException e) {
				throw new IllegalArgumentException("Internal error: Issues with plugins file");
			}
		}

		candidateCounts = new int[infiniteTypes.size()];

		// Run the initializers for the Logical Types
		for (LogicalType logical : infiniteTypes)
			logical.initialize(locale);
		for (LogicalTypeFinite logical : finiteTypes) {
			logical.initialize(locale);
			if (logical.getSize() + 10 > getMaxCardinality())
				throw new IllegalArgumentException("Internal error: Max Cardinality: " + getMaxCardinality() + " is insufficient to support plugin: " + logical.getQualifier());
		}

		if (pluginThreshold != -1) {
			// Set the threshold for all Logical Types
			for (LogicalType logical : infiniteTypes)
				logical.setThreshold(pluginThreshold);
			for (LogicalType logical : finiteTypes)
				logical.setThreshold(pluginThreshold);
		}

		longFormatter = NumberFormat.getIntegerInstance(locale);
		doubleFormatter = NumberFormat.getInstance(locale);

		DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);
		decimalSeparator = formatSymbols.getDecimalSeparator();
		monetaryDecimalSeparator = formatSymbols.getMonetaryDecimalSeparator();
		groupingSeparator = formatSymbols.getGroupingSeparator();
		minusSign = formatSymbols.getMinusSign();

		knownPatterns.initialize(locale);

		dateTimeParser = new DateTimeParser(resolutionMode, locale);

		initialized = true;
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

		final String input = rawInput.trim();

		final int length = input.length();

		if (length == 0) {
			blankCount++;
			trackLengthAndShape(rawInput);
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
		int numericGroupingSeparators = 0;
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
				numericGroupingSeparators++;
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
			compressedl0.append(KnownPatterns.PATTERN_ALPHANUMERIC).append('{').append(String.valueOf(length)).append('}');
		} else if ("true".equalsIgnoreCase(input) || "false".equalsIgnoreCase(input)) {
			compressedl0.append(KnownPatterns.PATTERN_BOOLEAN_TRUE_FALSE);
		} else if ("yes".equalsIgnoreCase(input) || "no".equalsIgnoreCase(input)) {
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

		if (dateTimeParser.determineFormatString(input) != null)
			possibleDateTime++;

		// Check to see if this input is one of our registered Infinite Logical Types
		int c = 0;
		for (LogicalTypeInfinite logical : infiniteTypes) {
			if (logical.isCandidate(input, compressedl0, charCounts, lastIndex))
				candidateCounts[c]++;
			c++;
		}

		// Create the level 1 and 2
		if (digitsSeen > 0 && couldBeNumeric && numericDecimalSeparators <= 1) {
			StringBuilder l1 = null;
			StringBuilder l2 = null;
			if (numericDecimalSeparators == 1) {
				if (possibleExponentSeen == -1) {
					l1 = new StringBuilder(numericSigned ? knownPatterns.PATTERN_SIGNED_DOUBLE : knownPatterns.PATTERN_DOUBLE);
					l2 = new StringBuilder(knownPatterns.PATTERN_SIGNED_DOUBLE);
				}
				else {
					l1 = new StringBuilder(numericSigned ? knownPatterns.PATTERN_SIGNED_DOUBLE_WITH_EXPONENT : knownPatterns.PATTERN_DOUBLE_WITH_EXPONENT);
					l2 = new StringBuilder(knownPatterns.PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
				}
			}
			else {
				l1 = new StringBuilder(numericSigned ? knownPatterns.PATTERN_SIGNED_LONG : knownPatterns.PATTERN_LONG);
				l2 = new StringBuilder(knownPatterns.PATTERN_SIGNED_LONG);
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
				if (levelIndex != 0 && bestPattern != null && secondBestPattern != null && thirdBestPattern != null &&
						bestPattern.isNumeric() && secondBestPattern.isNumeric() && thirdBestPattern.isNumeric()) {
					newKey = knownPatterns.numericPromotion(newKey, thirdBestPattern.regexp);
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
					PatternInfo candidate = new PatternInfo(null, logical.getRegexp(), logical.getBaseType(), logical.getQualifier(), true, -1, -1, null, null);
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

	private String compress(final String input) {
		StringBuilder b = new StringBuilder();

		int len = input.length();
		if (len > 30)
			return ".+";
		for (int i = 0; i < len; i++) {
			char ch = input.charAt(i);
			if (Character.isDigit(ch))
				b.append('1');
			else if (Character.isAlphabetic(ch))
				b.append('a');
			else
				b.append(ch);
		}

		return b.toString();
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

		String compressed = compress(input);
		Integer seen = outliersCompressed.get(compressed);
		if (seen == null) {
			if (outliersCompressed.size() < maxOutliers)
				outliersCompressed.put(compressed, 1);
		} else {
			outliersCompressed.put(compressed, seen + 1);
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
		Map<String, Integer> outlierMap = useCompressed ? outliersCompressed : outliers;

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
		outliersCompressed.clear();
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
	private String shape = null;
	private void trackLengthAndShape(final String input) {
		// We always want to track basic facts for the field
		final int length = input.length();

		if (length != 0 && length < minRawLength)
			minRawLength = length;
		if (length > maxRawLength)
			maxRawLength = length;

		if (uniformShape) {
			String inputShape = compress(input);
			if (shape == null)
				shape = inputShape;
			else if (!shape.contentEquals(inputShape))
				uniformShape = false;
		}
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
			minTrimmedLength = trimmedLength;
		if (trimmedLength > maxRawLength)
			maxTrimmedLength = trimmedLength;
	}

	/**
	 * Track the supplied raw input, once we have enough samples attempt to determine the type.
	 * @param input The raw input string
	 */
	private void trackResult(final String input) {

		trackLengthAndShape(input);

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
				DateTimeParserResult result = DateTimeParserResult.asResult(matchPatternInfo.format, resolutionMode, locale);
				try {
					result.parse(input);
				}
				catch (DateTimeParseException e) {
					char ditch = '_';
					if ("Insufficient digits in input (d)".equals(e.getMessage()))
						ditch = 'd';
					else if ("Insufficient digits in input (M)".equals(e.getMessage()))
						ditch = 'M';
					if (ditch != '_') {
						try {

							int offset = matchPatternInfo.format.indexOf(ditch);
							final String newFormatString = new StringBuffer(matchPatternInfo.format).deleteCharAt(offset).toString();
							result = DateTimeParserResult.asResult(newFormatString, resolutionMode, locale);
							matchPatternInfo = new PatternInfo(null, result.getRegExp(), matchPatternInfo.type, newFormatString, false, -1, -1, null, newFormatString);

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
					LogicalType logical = registered.get(matchPatternInfo.typeQualifier);
					if (logical != null && logical.isValidSet(dataStreamName, matchCount, realSamples, cardinality, outliers) != null)
						if (logical.getQualifier().equals(matchPatternInfo.typeQualifier) && "US_ZIP5".equals(matchPatternInfo.typeQualifier))
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

		// Check how many outliers we have
		for (final Map.Entry<String, Integer> entry : outliers.entrySet()) {
			misses++;
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
					misses++;
					missCount += entry.getValue();
					newOutliers.put(entry.getKey(), entry.getValue());
					// Break out early if we know we are going to fail
					if (newOutliers.size() > 1 && (double)missCount / realSamples > missThreshold)
						return false;
				}
			}
		}

		// To declare success we need fewer than threshold failures by count and additionally fewer than 4 groups
		if (((double)missCount / realSamples > missThreshold || misses >= 4))
			return false;

		matchCount = validCount;
		matchPatternInfo = new PatternInfo(null, logical.getRegexp(), PatternInfo.Type.STRING, logical.getQualifier(), true, -1, -1, null, null);
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
				// Break out early if we know we are going to fail
				if (logical.isValidSet(dataStreamName, realSamples - missCount, realSamples, cardinalityUpper, newOutliers) != null)
					return false;
			}
		}

		if (logical.isValidSet(dataStreamName, realSamples - missCount, realSamples, cardinalityUpper, newOutliers) != null)
			return false;

		outliers.putAll(newOutliers);
		cardinalityUpper.keySet().removeAll(newOutliers.keySet());
		matchCount = validCount;
		matchPatternInfo = new PatternInfo(null, logical.getRegexp(), PatternInfo.Type.STRING, logical.getQualifier(), true, -1, -1, null, null);
		cardinality = cardinalityUpper;
		return true;
	}

	private String lengthQualifier(int min, int max) {
		if (!lengthQualifier)
			return min > 0 ? "+" : ".";

		return Utils.regExpLength(min, max);
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
		PatternInfo save = matchPatternInfo;

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
			matchCount = sampleCount;
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

		if (KnownPatterns.ID.ID_LONG == matchPatternInfo.id || KnownPatterns.ID.ID_SIGNED_LONG == matchPatternInfo.id) {
			if (KnownPatterns.ID.ID_LONG == matchPatternInfo.id && matchPatternInfo.typeQualifier == null && minLong < 0)
				matchPatternInfo = knownPatterns.getByID(KnownPatterns.ID.ID_SIGNED_LONG);

			if (groupingSeparators == 0 && minLong > 19000101 && maxLong < 20400101 &&
					((realSamples >= reflectionSamples && cardinality.size() > 10) || dataStreamName.toLowerCase(locale).contains("date"))) {
				matchPatternInfo = new PatternInfo(null, "\\d{8}", PatternInfo.Type.LOCALDATE, "yyyyMMdd", false, 8, 8, null, "yyyyMMdd");
				DateTimeFormatter dtf = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(matchPatternInfo.format).toFormatter(locale);
				minLocalDate = LocalDate.parse(String.valueOf(minLong), dtf);
				maxLocalDate = LocalDate.parse(String.valueOf(maxLong), dtf);
			} else if (groupingSeparators == 0 && minLong > 1800 && maxLong < 2030 &&
					((realSamples >= reflectionSamples && cardinality.size() > 10) || dataStreamName.toLowerCase(locale).contains("year") || dataStreamName.toLowerCase(locale).contains("date"))) {
				matchPatternInfo = new PatternInfo(null, "\\d{4}", PatternInfo.Type.LOCALDATE, "yyyy", false, 4, 4, null, "yyyy");
				minLocalDate = LocalDate.of((int)minLong, 1, 1);
				maxLocalDate = LocalDate.of((int)maxLong, 1, 1);
			} else if (cardinality.size() == 2 && minLong == 0 && maxLong == 1) {
				// boolean by any other name
				minBoolean = "0";
				maxBoolean = "1";
				matchPatternInfo = knownPatterns.getByID(KnownPatterns.ID.ID_BOOLEAN_ONE_ZERO);
			} else {
				if (groupingSeparators != 0)
					if (matchPatternInfo.id == KnownPatterns.ID.ID_LONG)
						matchPatternInfo = knownPatterns.getByID(KnownPatterns.ID.ID_LONG_GROUPING);
					else
						matchPatternInfo = knownPatterns.getByID(KnownPatterns.ID.ID_SIGNED_LONG_GROUPING);

				matchPatternInfo = new PatternInfo(matchPatternInfo);
				matchPatternInfo.regexp = matchPatternInfo.regexp.replace("+", lengthQualifier(minTrimmedLength, maxTrimmedLength));

				if (realSamples >= reflectionSamples && confidence < threshold/100.0) {
					// We thought it was an integer field, but on reflection it does not feel like it
					conditionalBackoutToPattern(realSamples, matchPatternInfo, false);
					confidence = (double) matchCount / realSamples;
				}
			}
		} else if (PatternInfo.Type.DOUBLE.equals(matchPatternInfo.type)) {
			if (matchPatternInfo.typeQualifier == null && minDouble < 0.0)
				matchPatternInfo = knownPatterns.getByID(KnownPatterns.ID.ID_SIGNED_DOUBLE);

			if (groupingSeparators != 0)
				if (matchPatternInfo.id == KnownPatterns.ID.ID_DOUBLE)
					matchPatternInfo = knownPatterns.getByID(KnownPatterns.ID.ID_DOUBLE_GROUPING);
				else
					matchPatternInfo = knownPatterns.getByID(KnownPatterns.ID.ID_SIGNED_DOUBLE_GROUPING);
		} else if (PatternInfo.Type.STRING.equals(matchPatternInfo.type)) {
			final int length = determineLength(matchPatternInfo.regexp);
			// We thought it was a fixed length string, but on reflection it does not feel like it
			if (length != -1 && realSamples >= reflectionSamples && (double) matchCount / realSamples < 0.95) {
				backoutToPattern(realSamples, KnownPatterns.PATTERN_ANY_VARIABLE);
				confidence = (double) matchCount / realSamples;
			}

			// Build Cardinality map ignoring case (and white space)
			int minKeyLength = Integer.MAX_VALUE;
			int maxKeyLength = 0;
			Map<String, Integer> cardinalityUpper = new HashMap<String, Integer>();
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

			// Fixup any likely enums
			if (matchPatternInfo.typeQualifier == null && cardinalityUpper.size() < 100 && outliersCompressed.size() != 0) {
				boolean fail = false;
				int count = 0;

				for (final Map.Entry<String, Integer> entry : outliersCompressed.entrySet()) {
					String key = entry.getKey();
					count += entry.getValue();
					int len = key.length();
					for (int i = 0; i < len; i++) {
						char c = key.charAt(i);
						if (c != '1' && c != 'a' && c != '-' && c != '_') {
							fail = true;
							break;
						}
					}
				}
				if ((double)count/matchCount > 0.01 && !fail) {
					backoutToPattern(realSamples, KnownPatterns.PATTERN_ANY_VARIABLE);
					confidence = (double) matchCount / realSamples;
				}
			}

			// Need to evaluate if we got the type wrong
			if (matchPatternInfo.typeQualifier == null && matchPatternInfo.isAlphabetic() && realSamples >= reflectionSamples) {
				conditionalBackoutToPattern(realSamples, matchPatternInfo, false);
				confidence = (double) matchCount / realSamples;
			}
		}

		if (PatternInfo.Type.STRING.equals(matchPatternInfo.type) && matchPatternInfo.typeQualifier == null) {
			// Qualify Alpha or Alnum with a min and max length
			if ((KnownPatterns.PATTERN_ALPHA_VARIABLE.equals(matchPatternInfo.regexp) || KnownPatterns.PATTERN_ALPHANUMERIC_VARIABLE.equals(matchPatternInfo.regexp))) {
				String newPattern = matchPatternInfo.regexp;
				newPattern = newPattern.substring(0, newPattern.length() - 1) + lengthQualifier(minTrimmedLength, maxTrimmedLength);
				matchPatternInfo = new PatternInfo(null, newPattern, PatternInfo.Type.STRING, matchPatternInfo.typeQualifier, false, minTrimmedLength,
						maxTrimmedLength, null, null);
			}

			// Qualify random string with a min and max length
			if (KnownPatterns.PATTERN_ANY_VARIABLE.equals(matchPatternInfo.regexp)) {
				String newPattern = null;
				if (uniformShape && cardinality.size() > 1)
					newPattern = shapeToRegExp(shape);
				if (newPattern == null)
					newPattern = KnownPatterns.PATTERN_ANY + lengthQualifier(minTrimmedLength, maxTrimmedLength);
				matchPatternInfo = new PatternInfo(null, newPattern, PatternInfo.Type.STRING, matchPatternInfo.typeQualifier, false, minRawLength,
						maxRawLength, null, null);
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
				DateTimeFormatter dtf = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(matchPatternInfo.format).toFormatter(locale);

				minValue = minLocalDate == null ? null : minLocalDate.format(dtf);
				maxValue = maxLocalDate == null ? null : maxLocalDate.format(dtf);
			}
			break;

		case LOCALTIME:
			if (collectStatistics) {
				DateTimeFormatter dtf = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(matchPatternInfo.format).toFormatter(locale);

				minValue = minLocalTime == null ? null : minLocalTime.format(dtf);
				maxValue = maxLocalTime == null ? null : maxLocalTime.format(dtf);
			}
			break;

		case LOCALDATETIME:
			if (collectStatistics) {
				DateTimeFormatter dtf = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(matchPatternInfo.format).toFormatter(locale);

				minValue = minLocalDateTime == null ? null : minLocalDateTime.format(dtf);
				maxValue = maxLocalDateTime == null ? null : maxLocalDateTime.format(dtf);
			}
			break;

		case ZONEDDATETIME:
			if (collectStatistics) {
				DateTimeFormatter dtf = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(matchPatternInfo.format).toFormatter(locale);

				minValue = minZonedDateTime.format(dtf);
				maxValue = maxZonedDateTime.format(dtf);
			}
			break;

		case OFFSETDATETIME:
			if (collectStatistics) {
				DateTimeFormatter dtf = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(matchPatternInfo.format).toFormatter(locale);

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

		TextAnalysisResult result = new TextAnalysisResult(dataStreamName, matchCount, matchPatternInfo, leadingWhiteSpace, trailingWhiteSpace, sampleCount,
				nullCount, blankCount, totalLeadingZeros, confidence, minValue, maxValue, minRawLength, maxRawLength, sum,
				cardinality, outliers, key);

		matchPatternInfo = save;

		return result;
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
