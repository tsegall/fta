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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormatSymbols;
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

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.cobber.fta.DateTimeParser.DateResolutionMode;

/**
 * Analyze Text data to determine type information and other key metrics
 * associated with a text stream. A key objective of the analysis is that it
 * should be sufficiently fast to be in-line (e.g. as the data is input from
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

	/** The default value for the maximum Cardinality tracked. */
	public static final int MAX_CARDINALITY_DEFAULT = 500;
	private int maxCardinality = MAX_CARDINALITY_DEFAULT;

	private static final int MIN_SAMPLES_FOR_KEY = 1000;

	/** The default value for the maximum # of outliers tracked. */
	public static final int MAX_OUTLIERS_DEFAULT = 50;
	private int maxOutliers = MAX_OUTLIERS_DEFAULT;

	private static final int REFLECTION_SAMPLES = 50;

	private String name;
	private DateResolutionMode resolutionMode = DateResolutionMode.None;
	private char decimalSeparator;
	private char monetaryDecimalSeparator;
	private char groupingSeparator;
	private char minusSign;
	private long sampleCount;
	private long nullCount;
	private long blankCount;
	private final Map<String, Integer> cardinality = new HashMap<String, Integer>();
	private final Map<String, Integer> outliers = new HashMap<String, Integer>();
	private List<String> raw; // 0245-11-98
	// 0: d{4}-d{2}-d{2} 1: d{+}-d{+}-d{+} 2: d{+}-d{+}-d{+}
	// 0: d{4} 1: d{+} 2: [-]d{+}
	// input "hello world" 0: a{5} a{5} 1: a{+} a{+} 2: a{+}
	private List<StringBuilder>[] levels = new ArrayList[3];

	private PatternInfo.Type matchType;
	private long matchCount;
	private String matchPattern;
	private PatternInfo matchPatternInfo;

	private boolean trainingStarted;

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
	private long totalLongs;
	private long totalLeadingZeros;
	private int possibleEmails;
	private int possibleZips;
	private int possibleURLs;

	private static Map<String, PatternInfo> patternInfo;
	private static Map<String, PatternInfo> typeInfo;
	private static Set<String> zips = new HashSet<String>();
	private static Set<String> usStates = new HashSet<String>();
	private static Set<String> caProvinces = new HashSet<String>();
	private static Set<String> countries = new HashSet<String>();
	private static Set<String> monthAbbr = new HashSet<String>();

	public static final String PATTERN_ALPHA = "\\p{Alpha}+";
	public static final String PATTERN_LONG = "\\d+";
	public static final String PATTERN_SIGNED_LONG = "-?\\d+";
	public static final String PATTERN_DOUBLE = "\\.\\d+|\\d+(\\.\\d+)?";
	public static final String PATTERN_SIGNED_DOUBLE = "-?\\.\\d+|-?\\d+(\\.\\d+)?";

	private final Map<String, DateTimeFormatter> formatterCache = new HashMap<String, DateTimeFormatter>();

	private static void addPattern(final Map<String, PatternInfo> map, final boolean patternIsKey, final String regexp, final PatternInfo.Type type,
			final String typeQualifier, final int minLength, final int maxLength, final String generalPattern, final String format) {
		map.put(patternIsKey ? regexp : (type.toString() + "." + typeQualifier), new PatternInfo(regexp, type, typeQualifier, minLength, maxLength, generalPattern, format));
	}

	static {
		patternInfo = new HashMap<String, PatternInfo>();
		typeInfo = new HashMap<String, PatternInfo>();

		addPattern(patternInfo, true, "(?i)true|false", PatternInfo.Type.BOOLEAN, null, 4, 5, null, "");
		addPattern(patternInfo, true, "(?i)yes|no", PatternInfo.Type.BOOLEAN, null, 2, 3, null, "");
		addPattern(patternInfo, true, "[0|1]", PatternInfo.Type.BOOLEAN, null, -1, -1, null, null);

		addPattern(patternInfo, true, "\\p{Alpha}{2}", PatternInfo.Type.STRING, null, 2, 2, null, "");
		addPattern(patternInfo, true, "\\p{Alpha}{3}", PatternInfo.Type.STRING, null, 3, 3, null, "");
		addPattern(patternInfo, true, PATTERN_ALPHA, PatternInfo.Type.STRING, null, 1, -1, null, "");

		addPattern(patternInfo, true, PATTERN_LONG, PatternInfo.Type.LONG, null, 1, -1, null, "");
		addPattern(patternInfo, true, PATTERN_SIGNED_LONG, PatternInfo.Type.LONG, "SIGNED", 1, -1, null, "");
		addPattern(patternInfo, true, PATTERN_DOUBLE, PatternInfo.Type.DOUBLE, null, -1, -1, null, "");
		addPattern(patternInfo, true, PATTERN_SIGNED_DOUBLE, PatternInfo.Type.DOUBLE, "SIGNED", -1, -1, null, "");

		// Logical Types
		addPattern(typeInfo, false, "[NULL]", PatternInfo.Type.STRING, "NULL", -1, -1, null, null);
		addPattern(typeInfo, false, "[BLANKORNULL]", PatternInfo.Type.STRING, "BLANKORNULL", -1, -1, null, null);
		addPattern(typeInfo, false, "[ ]*", PatternInfo.Type.STRING, "BLANK", -1, -1, null, null);
		addPattern(typeInfo, false, "\\d{5}", PatternInfo.Type.LONG, "ZIP", -1, -1, null, null);
		addPattern(typeInfo, false, "\\p{Alpha}{2}", PatternInfo.Type.STRING, "NA_STATE", -1, -1, null, null);
		addPattern(typeInfo, false, "\\p{Alpha}{2}", PatternInfo.Type.STRING, "US_STATE", -1, -1, null, null);
		addPattern(typeInfo, false, "\\p{Alpha}{2}", PatternInfo.Type.STRING, "CA_PROVINCE", -1, -1, null, null);
		addPattern(typeInfo, false, ".+", PatternInfo.Type.STRING, "COUNTRY", -1, -1, null, null);
		addPattern(typeInfo, false, "\\p{Alpha}{3}", PatternInfo.Type.STRING, "MONTHABBR", -1, -1, null, null);

		try {
			BufferedReader reader = null;

			reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/us_zips.csv")));
			String line = null;
			while ((line = reader.readLine()) != null) {
				zips.add(line);
			}

			reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/us_states.csv")));
			while ((line = reader.readLine()) != null) {
				usStates.add(line);
			}

			reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/ca_provinces.csv")));
			while ((line = reader.readLine()) != null) {
				caProvinces.add(line);
			}

			reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/countries.csv")));
			while ((line = reader.readLine()) != null) {
				countries.add(line);
			}

			// Setup the Monthly abbreviations
			final String[] shortMonths = new DateFormatSymbols().getShortMonths();
			for (final String shortMonth : shortMonths) {
				monthAbbr.add(shortMonth.toUpperCase(Locale.ROOT));
			}
		}
		catch (IOException e) {
			throw new InternalErrorException("Failed to initialize", e);
		}
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
		this.name = name;
		this.resolutionMode = resolutionMode;
		final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
		decimalSeparator = formatSymbols.getDecimalSeparator();
		monetaryDecimalSeparator = formatSymbols.getMonetaryDecimalSeparator();
		groupingSeparator = formatSymbols.getGroupingSeparator();
		minusSign = formatSymbols.getMinusSign();
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
	 * Indicate whether to collect statistics or not.
	 *
	 * @return Whether Statistics collection is enabled.
	 */
	public boolean getCollectStatistics() {
		return collectStatistics;
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

	private boolean trackLong(final String rawInput, final boolean register) {
		final String input = rawInput.trim();
		long l;

		try {
			l = Long.parseLong(input);
		} catch (NumberFormatException e) {
			return false;
		}

		if (register) {
			totalLongs++;
			if (input.charAt(0) == '0')
				totalLeadingZeros++;
		}

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

		if ("ZIP".equals(matchPatternInfo.typeQualifier)) {
			return zips.contains(rawInput);
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

	private boolean trackString(final String input, final boolean register) {
		String cleaned = input;

		if ("EMAIL".equals(matchPatternInfo.typeQualifier)) {
			// Address lists commonly have ;'s as separators as opposed to the
			// ','
			if (cleaned.indexOf(';') != -1)
				cleaned = cleaned.replaceAll(";", ",");
			try {
				return InternetAddress.parse(cleaned).length != 0;
			} catch (AddressException e) {
				return false;
			}
		} else if ("URL".equals(matchPatternInfo.typeQualifier)) {
			try {
				final URL url = new URL(cleaned);
				url.toURI();
				return true;
			} catch (MalformedURLException | URISyntaxException exception) {
				return false;
			}
		}

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

		return true;
	}

	private boolean trackDouble(final String input) {
		double d;

		try {
			d = Double.parseDouble(input.trim());
		} catch (NumberFormatException e) {
			return false;
		}

		// If it is NaN/Infinity then we are all done
		if (Double.isNaN(d) || Double.isInfinite(d))
			return true;

		if (collectStatistics) {
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
			trainingStarted = true;
			raw = new ArrayList<String>(samples);
			levels[0] = new ArrayList<StringBuilder>(samples);
			levels[1] = new ArrayList<StringBuilder>(samples);
			levels[2] = new ArrayList<StringBuilder>(samples);
		}

		sampleCount++;

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
		if (matchType != null)
			return true;

		raw.add(rawInput);

		final StringBuilder l0 = new StringBuilder(length);

		// Walk the string
		boolean numericSigned = false;
		int numericDecimalSeparators = 0;
		boolean notNumericOnly = false;
		int digitsSeen = 0;
		int commas = 0;
		int semicolons = 0;
		int atSigns = 0;
		for (int i = 0; i < length; i++) {
			final char ch = input.charAt(i);
			if (i == 0 && ch == minusSign) {
				numericSigned = true;
			} else if (Character.isDigit(ch)) {
				l0.append('d');
				digitsSeen++;
			} else if (ch == decimalSeparator) {
				l0.append('D');
				numericDecimalSeparators++;
				if (decimalSeparator == ',')
					commas++;
			} else if (ch == groupingSeparator) {
				l0.append('G');
				if (groupingSeparator == ',')
					commas++;
			} else if (Character.isAlphabetic(ch)) {
				l0.append('a');
				notNumericOnly = true;
			} else {
				if (ch == '@')
					atSigns++;
				else if (ch == ',')
					commas++;
				else if (ch == ';')
					semicolons++;
				l0.append(ch);
				notNumericOnly = true;
			}
		}

		final StringBuilder compressedl0 = new StringBuilder(length);
		if ("true".equalsIgnoreCase(input) || "false".equalsIgnoreCase(input)) {
			compressedl0.append("(?i)true|false");
		} else if ("yes".equalsIgnoreCase(input) || "no".equalsIgnoreCase(input)) {
			compressedl0.append("(?i)yes|no");
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
						compressedl0.append(last == 'd' ? "\\d" : "\\p{Alpha}");
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
		if (atSigns - 1 == commas || atSigns - 1 == semicolons)
			possibleEmails++;
		if (length == 5 && digitsSeen == 5)
			possibleZips++;
		if (input.indexOf("://") != -1)
			possibleURLs++;

		// Create the level 1 and 2
		if (digitsSeen > 0 && notNumericOnly == false && numericDecimalSeparators <= 1) {
			StringBuilder l1 = null;
			StringBuilder l2 = null;
			if (numericDecimalSeparators == 1) {
				l1 = new StringBuilder(numericSigned ? PATTERN_SIGNED_DOUBLE : PATTERN_DOUBLE);
				l2 = new StringBuilder(PATTERN_SIGNED_DOUBLE);
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
				levels[2].add(new StringBuilder(PATTERN_ALPHA));
			}

		}

		return matchType != null;
	}

	private Map.Entry<String, Integer> getBest(final int levelIndex) {
		final List<StringBuilder> level = levels[levelIndex];
		if (level.isEmpty())
			return null;

		final Map<String, Integer> map = new HashMap<String, Integer>();

		// Calculate the frequency of every element
		for (final StringBuilder s : level) {
			final String key = s.toString();
			final Integer seen = map.get(key);
			if (seen == null) {
				map.put(key, 1);
			} else {
				map.put(key, seen + 1);
			}
		}

		// Grab the best and the second best based on frequency
		int bestCount = 0;
		int secondBestCount = 0;
		Map.Entry<String, Integer> best = null;
		Map.Entry<String, Integer> secondBest = null;
		for (final Map.Entry<String, Integer> entry : map.entrySet()) {
			if (entry.getValue() > bestCount) {
				secondBest = best;
				secondBestCount = bestCount;
				best = entry;
				bestCount = entry.getValue();
			} else if (entry.getValue() > secondBestCount) {
				secondBest = entry;
				secondBestCount = entry.getValue();
			}
		}

		final String bestKey = best.getKey();
		String secondBestKey;
		if (secondBest != null) {
			secondBestKey = secondBest.getKey();

			final PatternInfo bestPattern = patternInfo.get(bestKey);
			final PatternInfo secondBestPattern = patternInfo.get(secondBestKey);
			if (bestPattern != null && secondBestPattern != null) {
				if (bestPattern.isNumeric() && secondBestPattern.isNumeric()) {
					if (!bestPattern.type.equals(secondBestPattern.type)) {
						// Promote Long to Double
						final String newKey = PatternInfo.Type.DOUBLE.equals(bestPattern.type) ? best.getKey() : secondBest.getKey();
						best = new AbstractMap.SimpleEntry<String, Integer>(newKey,
								best.getValue() + secondBest.getValue());
					}
				} else if (PatternInfo.Type.STRING.equals(bestPattern.type)) {
					// Promote anything to STRING
					best = new AbstractMap.SimpleEntry<String, Integer>(bestKey,
							best.getValue() + secondBest.getValue());
				}
			}
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
			matchPattern = PATTERN_ALPHA;
			matchPatternInfo = patternInfo.get(matchPattern);
			matchType = matchPatternInfo.type;
			return;
		}

		int level0value = 0, level1value = 0, level2value = 0;
		String level0pattern = null, level1pattern = null, level2pattern = null;
		PatternInfo level0patternInfo = null, level1patternInfo = null, level2patternInfo = null;
		String pattern = null;
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
			pattern = level0pattern;
			matchPatternInfo = level0patternInfo;

			// Take any level 1 with something we recognize or a better count
			if (level1 != null && (level0patternInfo == null || level1value > level0value)) {
				best = level1;
				pattern = level1pattern;
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
				pattern = level2pattern;
				matchPatternInfo = level2patternInfo;
			}

			matchType = matchPatternInfo.type;

			if (possibleDateTime == raw.size()) {
				final DateTimeParser det = new DateTimeParser(resolutionMode);
				for (final String sample : raw)
					det.train(sample);

				final DateTimeParserResult result = det.getResult();
				final String formatString = result.getFormatString();
				matchPatternInfo = new PatternInfo(result.getRegExp(), result.getType(), formatString, -1, -1, null,
						formatString);
				matchType = matchPatternInfo.type;
				pattern = matchPatternInfo.regexp;
			}

			// Do we have a set of possible emails?
			if (possibleEmails == raw.size()) {
				final PatternInfo save = matchPatternInfo;
				matchPatternInfo = new PatternInfo(matchPattern, PatternInfo.Type.STRING, "EMAIL", -1, -1, null, null);
				int emails = 0;
				for (final String sample : raw) {
					if (trackString(sample, false))
						emails++;
				}
				// if at least 90% of them looked like a genuine email then
				// stay with email, otherwise back out to simple String
				if (emails < .9 * raw.size())
					matchPatternInfo = save;
			}

			// Do we have a set of possible URLs?
			if (possibleURLs == raw.size()) {
				final PatternInfo save = matchPatternInfo;
				matchPatternInfo = new PatternInfo(matchPattern, PatternInfo.Type.STRING, "URL", -1, -1, null, null);
				int countURLs = 0;
				for (final String sample : raw)
					if (trackString(sample, false))
						countURLs++;
				// if at least 90% of them looked like a genuine URL then
				// stay with URL, otherwise back out to simple String
				if (countURLs < .9 * raw.size())
					matchPatternInfo = save;
			}

			// Do we have a set of possible zip codes?
			if (possibleZips == raw.size()) {
				final PatternInfo save = matchPatternInfo;
				matchPatternInfo = typeInfo.get(PatternInfo.Type.LONG.toString() + "." + "ZIP");
				pattern = matchPatternInfo.regexp;

				int zipCount = 0;
				for (final String sample : raw)
					if (trackLong(sample, false))
						zipCount++;
				// if at least 90% of them looked like a genuine zip
				// then stay with zip, otherwise back out to simple Long
				if (zipCount < .9 * raw.size()) {
					matchPatternInfo = save;
					pattern = save.regexp;
				}
				matchType = matchPatternInfo.type;
			}

			for (final String sample : raw)
				trackResult(sample);

			matchCount = best.getValue();
			matchPattern = pattern;
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

	private void outlier(final String input) {
		final Integer seen = outliers.get(input);
		if (seen == null) {
			if (outliers.size() < maxOutliers)
				outliers.put(input, 1);
		} else {
			outliers.put(input, seen + 1);
		}
	}

	private void backoutToString(final long realSamples) {
		matchPattern = PATTERN_ALPHA;
		matchCount = realSamples;
		matchPatternInfo = patternInfo.get(matchPattern);

		// All outliers are now part of the cardinality set and there are now no outliers
		cardinality.putAll(outliers);
		outliers.clear();
	}

	private void backoutZip(final long realSamples) {
		if (totalLongs > .95 * realSamples) {
			matchPattern = PATTERN_LONG;
			matchCount = totalLongs;

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
				}
			}

			matchPatternInfo = patternInfo.get(matchPattern);
		} else {
			backoutToString(realSamples);
		}
	}

	private void trackLength(final String input) {
		// We always want to track basic facts for the field
		final int length = input.length();

		if (length != 0 && length < minRawLength)
			minRawLength = length;
		if (length > maxRawLength)
			maxRawLength = length;
	}

	/**
	 * Track the supplied raw input, once we have enough samples attempt to determine the type.
	 * @param input The raw input string
	 */
	private void trackResult(final String input) {

		trackLength(input);

		// If the cache is full and we have not determined a type compute one
		if (matchType == null && sampleCount - (nullCount + blankCount) > samples) {
			determineType();
		}

		if (matchType == null) {
			return;
		}

		final long realSamples = sampleCount - (nullCount + blankCount);

		switch (matchType) {
		case BOOLEAN:
			if (trackBoolean(input)) {
				matchCount++;
				addValid(input);
				return;
			}
			outlier(input);
			break;

		case LONG:
			if (trackLong(input, true)) {
				matchCount++;
				addValid(input);
				return;
			}
			outlier(input);

			// Do a sanity check once we have at least REFLECTION_SAMPLES
			if (realSamples == REFLECTION_SAMPLES && (double) matchCount / realSamples < 0.9 && "ZIP".equals(matchPatternInfo.typeQualifier))
				backoutZip(realSamples);
			break;

		case DOUBLE:
			if (trackDouble(input)) {
				matchCount++;
				addValid(input);
				return;
			}
			outlier(input);
			break;

		case STRING:
			if (trackString(input, true)) {
				matchCount++;
				addValid(input);
				return;
			}
			outlier(input);

			if (realSamples == REFLECTION_SAMPLES && (double) matchCount / realSamples < 0.95 &&
					("URL".equals(matchPatternInfo.typeQualifier) || "EMAIL".equals(matchPatternInfo.typeQualifier)))
				backoutToString(realSamples);
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
				return;
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
							return;
						}
						catch (DateTimeParseException e2) {
							// Ignore and record as outlier below
						}
					}
				}
			}
			outlier(input);
			break;
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
	 * Determine if the current dataset reflects a logical type (of uniform length) as defined by the provided set.
	 * @param uniformSet The set of items with a uniform length that reflect this logical type
	 * @param type The type that along with the qualifier identifies the logical type that this set represents
	 * @param qualifier The qualifier that along with the type identifies the logical type that this set represents
	 * @return True if we believe that this data set is defined by the provided set
	 */
	private boolean checkUniformLengthSet(final Set<String> uniformSet, final PatternInfo.Type type, final String qualifier) {
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
				if (uniformSet.contains(entry.getKey().toUpperCase(Locale.ROOT)))
					validCount += entry.getValue();
				else {
					misses++;
					missCount += entry.getValue();
					newOutliers.put(entry.getKey(), entry.getValue());
					// Break out early if we know we are going to fail
					if ((double) missCount / realSamples > .05)
						return false;
				}
			}
		}

		// To declare success we need fewer than 5% failures by count and additionally fewer than 4 groups
		if ((double) missCount / realSamples > .05 || misses >= 4)
			return false;

		matchCount = validCount;
		matchPatternInfo = typeInfo.get(type.toString() + "." + qualifier);
		matchPattern = matchPatternInfo.regexp;
		outliers.putAll(newOutliers);
		cardinality.keySet().removeAll(newOutliers.keySet());

		return true;
	}

	/**
	 * Determine if the current dataset reflects a logical type (of variable length) as defined by the provided set.
	 * @param variableSet The set of items that reflect this logical type
	 * @param type The type that along with the qualifier identifies the logical type that this set represents
	 * @param qualifier The qualifier that along with the type identifies the logical type that this set represents
	 * @return True if we believe that this data set is defined by the provided set
	 */
	private boolean checkVariableLengthSet(final Set<String> variableSet, final PatternInfo.Type type, final String qualifier) {
		final long realSamples = sampleCount - (nullCount + blankCount);
		final Map<String, Integer> newOutliers = new HashMap<String, Integer>();
		long validCount = 0;
		long misses = 0;				// count of number of groups that are misses
		long missCount = 0;				// count of number of misses

		// Sweep the balance and check they are part of the set
		for (final Map.Entry<String, Integer> entry : cardinality.entrySet()) {
			if (variableSet.contains(entry.getKey().trim().toUpperCase(Locale.ROOT)))
				validCount += entry.getValue();
			else {
				misses++;
				missCount += entry.getValue();
				// Break out early if we know we are going to fail
				if ((double) missCount / realSamples > .05)
					return false;
				newOutliers.put(entry.getKey(), entry.getValue());
			}
		}

		// To declare success we need fewer than 5% failures by count and additionally fewer than 4 groups
		if ((double) missCount / realSamples > .05 || misses >= 4)
			return false;

		outliers.putAll(newOutliers);
		cardinality.keySet().removeAll(newOutliers.keySet());
		matchCount = validCount;
		matchPatternInfo = typeInfo.get(type.toString() + "." + qualifier);
		matchPattern = matchPatternInfo.regexp;
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
		if (matchType == null) {
			determineType();
		}

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
			matchPattern = matchPatternInfo.regexp;
			matchType = matchPatternInfo.type;
			matchCount = sampleCount;
			confidence = sampleCount >= 10 ? 1.0 : 0.0;
		}
		else {
			confidence = (double) matchCount / realSamples;
		}

		// Do a sanity check - we need a minimum number to declare it a ZIP
		if ("ZIP".equals(matchPatternInfo.typeQualifier) && ((realSamples > REFLECTION_SAMPLES && confidence < 0.9) || cardinality.size() < 5)) {
			backoutZip(realSamples);
			confidence = (double) matchCount / realSamples;
		}

		if (PATTERN_LONG.equals(matchPattern) && minLong > 1700 && maxLong < 2030) {
			matchPatternInfo = new PatternInfo("\\d{4}", PatternInfo.Type.LOCALDATE, "yyyy", -1, -1, null, "yyyy");
		} else if (PatternInfo.Type.STRING.equals(matchPatternInfo.type)) {
			final int length = determineLength(matchPattern);
			// We thought it was a fixed length string, but on reflection it does not feel like it
			if (length != -1 && realSamples > REFLECTION_SAMPLES && (double) matchCount / realSamples < 0.95) {
				backoutToString(realSamples);
				confidence = (double) matchCount / realSamples;
			}

			boolean typeIdentified = false;
			if (realSamples > REFLECTION_SAMPLES && cardinality.size() > 1 && "\\p{Alpha}{3}".equals(matchPattern)
					&& cardinality.size() <= monthAbbr.size() + 2) {
				typeIdentified = checkUniformLengthSet(monthAbbr, PatternInfo.Type.STRING, "MONTHABBR");
			}

			if (!typeIdentified && realSamples > REFLECTION_SAMPLES && cardinality.size() > 1
					&& cardinality.size() <= countries.size()) {
				typeIdentified = checkVariableLengthSet(countries, PatternInfo.Type.STRING, "COUNTRY");
			}

			if (!typeIdentified && realSamples > REFLECTION_SAMPLES && "\\p{Alpha}{2}".equals(matchPattern)
					&& cardinality.size() < usStates.size() + caProvinces.size() + 5
					&& (name.toLowerCase(Locale.ROOT).contains("state") || name.toLowerCase(Locale.ROOT).contains("province")
							|| cardinality.size() > 5)) {
				int usStateCount = 0;
				int caProvinceCount = 0;
				int misses = 0;
				final Map<String, Integer> newOutliers = new HashMap<String, Integer>();

				for (final Map.Entry<String, Integer> entry : cardinality.entrySet()) {
					if (usStates.contains(entry.getKey().trim().toUpperCase(Locale.ROOT)))
						usStateCount += entry.getValue();
					else if (caProvinces.contains(entry.getKey().trim().toUpperCase(Locale.ROOT)))
						caProvinceCount += entry.getValue();
					else {
						misses++;
						newOutliers.put(entry.getKey(), entry.getValue());
					}
				}

				if (misses < 3) {
					String accessor = null;
					if (usStateCount != 0 && caProvinceCount != 0) {
						accessor = "NA_STATE";
						matchCount = usStateCount + caProvinceCount;
					} else if (usStateCount != 0) {
						accessor = "US_STATE";
						matchCount = usStateCount;
					} else if (caProvinceCount != 0) {
						accessor = "CA_PROVINCE";
						matchCount = caProvinceCount;
					}
					confidence = (double) matchCount / realSamples;
					outliers.putAll(newOutliers);
					cardinality.keySet().removeAll(newOutliers.keySet());
					matchPatternInfo = typeInfo.get(PatternInfo.Type.STRING.toString() + "." + accessor);
					matchPattern = matchPatternInfo.regexp;
				}
			}

			if (!typeIdentified && PATTERN_ALPHA.equals(matchPattern)) {
				matchPattern = ".{" + minRawLength;
				if (minRawLength != maxRawLength)
					matchPattern += "," + maxRawLength;
				matchPattern += "}";
				matchPatternInfo = new PatternInfo(matchPattern, PatternInfo.Type.STRING, matchPatternInfo.typeQualifier, minRawLength, maxRawLength, null,
						null);
			}

		} else if (PATTERN_LONG.equals(matchPattern)) {
			if (cardinality.size() == 2 && minLong == 0 && maxLong == 1) {
				// boolean by any other name
				matchPattern = "[0|1]";
				minBoolean = "0";
				maxBoolean = "1";
				matchType = PatternInfo.Type.BOOLEAN;
				matchPatternInfo = patternInfo.get(matchPattern);
			} else {
				// We thought it was an integer field, but on reflection it does not feel like it
				if (realSamples > REFLECTION_SAMPLES && confidence < 0.9) {
					matchPattern = ".{" + minRawLength;
					matchType = PatternInfo.Type.STRING;
					matchCount = realSamples;
					confidence = 1.0;
					if (minRawLength != maxRawLength)
						matchPattern += "," + maxRawLength;
					matchPattern += "}";
					matchPatternInfo = new PatternInfo(matchPattern, PatternInfo.Type.STRING, null, -1, -1, null, null);

					// All outliers are now part of the cardinality set and
					// there are no outliers
					cardinality.putAll(outliers);
					outliers.clear();
				} else {
					matchPattern = "\\d{" + minTrimmedLength;
					if (minTrimmedLength != maxTrimmedLength)
						matchPattern += "," + maxTrimmedLength;
					matchPattern += "}";
					matchPatternInfo = new PatternInfo(matchPattern, matchType, null, -1, -1, null, null);
				}
			}
		}

		boolean key = false;

		switch (matchType) {
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
		if (sampleCount > MIN_SAMPLES_FOR_KEY && maxCardinality >= MIN_SAMPLES_FOR_KEY / 2
				&& cardinality.size() >= maxCardinality && blankCount == 0 && nullCount == 0
				&& matchPatternInfo.typeQualifier == null
				&& ((PatternInfo.Type.STRING.equals(matchType) && minRawLength == maxRawLength && minRawLength < 32)
						|| PatternInfo.Type.LONG.equals(matchType))) {
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

		return new TextAnalysisResult(name, matchCount, matchPatternInfo, sampleCount, nullCount, blankCount,
				totalLeadingZeros, confidence, minValue, maxValue, minRawLength, maxRawLength, sum, cardinality, outliers, key);
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
