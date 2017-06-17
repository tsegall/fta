/*
 * Copyright 2017 Tim Segall
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
import java.text.DateFormatSymbols;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeParseException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

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

	/**
	 * The default value for the number of samples to collect before making a
	 * type determination.
	 */
	public static final int SAMPLE_DEFAULT = 20;
	private int samples = SAMPLE_DEFAULT;

	/** The default value for the Maximum Cardinality tracked. */
	public static final int MAX_CARDINALITY_DEFAULT = 500;
	private int maxCardinality = MAX_CARDINALITY_DEFAULT;

	private final int MIN_SAMPLES_FOR_KEY = 1000;

	/** The default value for the Maximum # of outliers tracked. */
	public static final int MAX_OUTLIERS_DEFAULT = 50;
	private int maxOutliers = MAX_OUTLIERS_DEFAULT;

	private static final int REFLECTION_SAMPLES = 50;

	String name;
	Boolean dayFirst = null;
	DecimalFormatSymbols formatSymbols;
	char decimalSeparator;
	char monetaryDecimalSeparator;
	char groupingSeparator;
	char minusSign;
	int sampleCount;
	int nullCount;
	int blankCount;
	Map<String, Integer> cardinality = new HashMap<String, Integer>();
	Map<String, Integer> outliers = new HashMap<String, Integer>();
	ArrayList<String> raw; // 0245-11-98
	// 0: d{4}-d{2}-d{2} 1: d{+}-d{+}-d{+} 2: d{+}-d{+}-d{+}
	// 0: d{4} 1: d{+} 2: [-]d{+}
	// input "hello world" 0: a{5} a{5} 1: a{+} a{+} 2: a{+}
	ArrayList<StringBuilder>[] levels = new ArrayList[3];

	String matchType;
	int matchCount;
	String matchPattern;
	PatternInfo matchPatternInfo;

	boolean trainingStarted;

	double minDouble = Double.MAX_VALUE;
	double maxDouble = -Double.MAX_VALUE;
	BigDecimal sumBD = new BigDecimal(0);

	long minLong = Long.MAX_VALUE;
	long maxLong = Long.MIN_VALUE;
	BigInteger sumBI = new BigInteger("0");

	String minString = null;
	String maxString = null;

	Boolean minBoolean = null;
	Boolean maxBoolean = null;

	String minValue = null;
	String maxValue = null;
	String sum = null;

	int minRawLength = Integer.MAX_VALUE;
	int maxRawLength = Integer.MIN_VALUE;

	int minTrimmedLength = Integer.MAX_VALUE;
	int maxTrimmedLength = Integer.MIN_VALUE;

	int stringLength = -1;

	int possibleDateTime = 0;
	int totalLongs = 0;
	int totalLeadingZeros = 0;
	int possibleEmails = 0;
	int possibleZips = 0;

	Map.Entry<String, Integer> lastCompute;

	static HashMap<String, PatternInfo> patternInfo = null;
	HashSet<String> zips = new HashSet<String>();
	HashSet<String> usStates = new HashSet<String>();
	HashSet<String> caProvinces = new HashSet<String>();
	HashSet<String> countries = new HashSet<String>();
	HashSet<String> monthAbbr = new HashSet<String>();

	static boolean dataLoaded = false;

	void addPattern(String pattern, String type, int minLength, int maxLength, String generalPattern, String format,
			String typeQualifier) {
		patternInfo.put(pattern,
				new PatternInfo(pattern, type, minLength, maxLength, generalPattern, format, typeQualifier));
	}

	void initPatternInfo() {
		patternInfo = new HashMap<String, PatternInfo>();

		addPattern("(?i)true|false", "Boolean", 4, 5, null, "", null);
		addPattern("\\a{2}", "String", 2, 2, null, "", null);
		addPattern("\\a{3}", "String", 3, 3, null, "", null);
		addPattern("\\a{+}", "String", 1, -1, null, "", null);
		addPattern("\\d{+}", "Long", 1, -1, null, "", null);
		addPattern("[-]\\d{+}", "Long", 1, -1, null, "", "Signed");
		addPattern("\\d{*}D\\d{+}", "Double", -1, -1, null, "", null);
		addPattern("[-]\\d{*}D\\d{+}", "Double", -1, -1, null, "", "Signed");
		addPattern("^[ ]*$", "[BLANK]", -1, -1, null, null, null);
		addPattern("[NULL]", "[NULL]", -1, -1, null, null, null);
		addPattern("[ZIP]", "Long", -1, -1, null, null, "Zip");
		addPattern("[0|1]", "Boolean", -1, -1, null, null, null);
		addPattern("[NA_STATE]", "String", -1, -1, null, null, "NA_STATE");
		addPattern("[US_STATE]", "String", -1, -1, null, null, "US_STATE");
		addPattern("[CA_PROVINCE]", "String", -1, -1, null, null, "CA_PROVINCE");
		addPattern("[COUNTRY]", "String", -1, -1, null, null, "COUNTRY");
		addPattern("[MONTHABBR]", "String", -1, -1, null, null, "MONTHABBR");
	}

	/**
	 * Construct a Text Analyzer for the named data stream.
	 *
	 * @param name
	 *            The name of the data stream (e.g. the column of the CSV file)
	 * @throws IOException
	 *             If an internal error occurred.
	 */
	public TextAnalyzer(String name, Boolean dayFirst) throws IOException {
		if (patternInfo == null) {
			initPatternInfo();
		}

		if (!dataLoaded) {
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/us_zips.csv")))) {
				String line = null;
				while ((line = reader.readLine()) != null) {
					zips.add(line);
				}
			}

			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/us_states.csv")))) {
				String line = null;
				while ((line = reader.readLine()) != null) {
					usStates.add(line);
				}
			}

			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/ca_provinces.csv")))) {
				String line = null;
				while ((line = reader.readLine()) != null) {
					caProvinces.add(line);
				}
			}

			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/countries.csv")))) {
				String line = null;
				while ((line = reader.readLine()) != null) {
					countries.add(line);
				}
			}

			// Setup the Monthly abbreviations
			String[] shortMonths = new DateFormatSymbols().getShortMonths();
			for (String shortMonth : shortMonths) {
				monthAbbr.add(shortMonth.toUpperCase(Locale.ROOT));
			}
		}

		this.name = name;
		this.dayFirst = dayFirst;
		formatSymbols = new DecimalFormatSymbols();
		decimalSeparator = formatSymbols.getDecimalSeparator();
		monetaryDecimalSeparator = formatSymbols.getMonetaryDecimalSeparator();
		groupingSeparator = formatSymbols.getGroupingSeparator();
		minusSign = formatSymbols.getMinusSign();
	}

	/**
	 * Construct an anonymous Text Analyzer for a data stream.
	 *
	 * @throws IOException
	 *             If an internal error occurred.
	 */
	public TextAnalyzer() throws IOException {
		this("anonymous", null);
	}

	/**
	 * Set the number of Samples to collect before attempting to determine the
	 * type. Note: It is not possible to change the Sample Size once training
	 * has started.
	 *
	 * @param samples
	 *            The number of samples to collect
	 * @return The previous value of this parameter.
	 */
	public int setSampleSize(int samples) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot change sample size once training has started");
		if (samples < SAMPLE_DEFAULT)
			throw new IllegalArgumentException("Cannot set sample size below " + SAMPLE_DEFAULT);

		int ret = samples;
		this.samples = samples;
		return ret;
	}

	/**
	 * Get the number of Samples used to collect before attempting to determine
	 * the type.
	 *
	 * @return The maximum cardinality.
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
	public int setMaxCardinality(int newCardinality) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot change maxCardinality once training has started");
		if (newCardinality < 0)
			throw new IllegalArgumentException("Invalid value for maxCardinality " + newCardinality);

		int ret = maxCardinality;
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
	public int setMaxOutliers(int newMaxOutliers) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot change outlier count once training has started");
		if (newMaxOutliers < 0)
			throw new IllegalArgumentException("Invalid value for outlier count " + newMaxOutliers);

		int ret = maxOutliers;
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

	private boolean trackLong(String rawInput, boolean register) {
		long l;
		String input = rawInput.trim();

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

		if (l < minLong) {
			minLong = l;
		}
		if (l > maxLong) {
			maxLong = l;
		}
		int digits = l < 0 ? input.length() - 1 : input.length();
		if (digits < minTrimmedLength)
			minTrimmedLength = digits;
		if (digits > maxTrimmedLength)
			maxTrimmedLength = digits;

		sumBI = sumBI.add(BigInteger.valueOf(l));

		if ("Zip".equals(matchPatternInfo.typeQualifier)) {
			return zips.contains(rawInput);
		}

		return true;
	}

	private boolean trackBoolean(String input) {
		String trimmed = input.trim();

		boolean isTrue = "true".equalsIgnoreCase(trimmed);
		boolean isFalse = !isTrue && "false".equalsIgnoreCase(trimmed);

		if (isTrue) {
			if (minBoolean == null)
				minBoolean = true;
			if (maxBoolean == null || maxBoolean == false)
				maxBoolean = true;
		} else if (isFalse) {
			if (maxBoolean == null)
				maxBoolean = false;
			if (minBoolean == null || minBoolean == true)
				minBoolean = false;
		}

		return isTrue || isFalse;
	}

	private boolean trackString(String input, boolean register) {
		if ("Email".equals(matchPatternInfo.typeQualifier)) {
			// Address lists commonly have ;'s as separators as opposed to the
			// ','
			if (input.indexOf(';') != -1)
				input = input.replaceAll(";", ",");
			try {
				InternetAddress[] emails = InternetAddress.parse(input);
				return emails.length != 0;
			} catch (AddressException e) {
				return false;
			}
		}

		int len = input.length();
		if (matchPatternInfo.minLength != -1 && len < matchPatternInfo.minLength)
			return false;
		if (matchPatternInfo.maxLength != -1 && len > matchPatternInfo.maxLength)
			return false;

		if (minString == null || minString.compareTo(input) > 0) {
			minString = input;
		}
		if (maxString == null || maxString.compareTo(input) < 0) {
			maxString = input;
		}

		return true;
	}

	private boolean trackDouble(String input) {
		double d;

		try {
			d = Double.parseDouble(input.trim());
		} catch (NumberFormatException e) {
			return false;
		}

		// If it is NaN/Infinity then we are all done
		if (Double.isNaN(d) || Double.isInfinite(d))
			return true;

		if (d < minDouble) {
			minDouble = d;
		}
		if (d > maxDouble) {
			maxDouble = d;
		}

		sumBD = sumBD.add(BigDecimal.valueOf(d));

		return true;
	}

	private void trackDateTime(String dateFormat, String input) throws DateTimeParseException {
		String trimmed = input.trim();

		DateTimeParserResult result = DateTimeParserResult.asResult(dateFormat);
		if (result == null)
			System.err.printf("NULL result for '%s'\n", dateFormat);

		result.parse(trimmed);
	}

	/**
	 * Train is the core entry point used to supply input to the Text Analyzer.
	 *
	 * @param rawInput
	 *            The raw input as a String
	 * @return A boolean indicating if the resultant type is currently known.
	 */
	public boolean train(String rawInput) {
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

		String input = rawInput.trim();

		if (input.length() == 0) {
			blankCount++;
			return matchType != null;
		}

		int length = input.length();

		trackResult(rawInput);

		// If we have determined a type, no need to further train
		if (matchType != null)
			return true;

		raw.add(rawInput);

		StringBuilder l0 = new StringBuilder(length);

		// Walk the string
		boolean numericSigned = false;
		int numericDecimalSeparators = 0;
		boolean notNumericOnly = false;
		int digitsSeen = 0;
		int commas = 0;
		int semicolons = 0;
		int atSigns = 0;
		for (int i = 0; i < length; i++) {
			char ch = input.charAt(i);
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

		if (DateTimeParser.determineFormatString(input) != null)
			possibleDateTime++;
		if (atSigns - 1 == commas || atSigns - 1 == semicolons)
			possibleEmails++;
		if (length == 5 && digitsSeen == 5)
			possibleZips++;

		StringBuilder compressedl0 = new StringBuilder(length);
		if ("true".equalsIgnoreCase(input) || "false".equalsIgnoreCase(input)) {
			compressedl0.append("(?i)true|false");
		} else {
			// Walk the new level0 to create the new level1
			String l0withSentinel = l0.toString() + "|";
			char last = l0withSentinel.charAt(0);
			int repetitions = 1;
			for (int i = 1; i < l0withSentinel.length(); i++) {
				char ch = l0withSentinel.charAt(i);
				if (ch == last) {
					repetitions++;
				} else {
					if (last == 'd' || last == 'a') {
						compressedl0.append('\\').append(last);
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

		// Create the level 1 and 2
		if (digitsSeen > 0 && notNumericOnly == false && numericDecimalSeparators <= 1) {
			StringBuilder l1 = new StringBuilder();
			StringBuilder l2 = new StringBuilder().append('[').append(minusSign).append(']');
			if (numericSigned)
				l1.append('[').append(minusSign).append(']');
			if (numericDecimalSeparators == 1) {
				l1.append("\\d{*}D");
				l2.append("\\d{*}D");
			}
			l1.append("\\d{+}");
			l2.append("\\d{+}");
			levels[1].add(l1);
			levels[2].add(l2);
		} else {
			// Fast version of replaceAll("\\{\\d*\\}", "{+}")
			StringBuilder collapsed = new StringBuilder(compressedl0);
			for (int i = 0; i < collapsed.length(); i++) {
				if (collapsed.charAt(i) == '{' && Character.isDigit(collapsed.charAt(i + 1))) {
					int start = ++i;
					while (collapsed.charAt(++i) != '}')
						/* EMPTY */;
					if (start + 1 == i)
						collapsed.setCharAt(start, '+');
					else
						collapsed.replace(start, i, "+");
				}
			}

			// Level 1 is the collapsed version e.g. convert d{4}-d{2}-d{2] to
			// d{+}-d{+}-d{+}
			PatternInfo found = patternInfo.get(compressedl0.toString());
			if (found != null && found.generalPattern != null) {
				levels[1].add(new StringBuilder(found.generalPattern));
				levels[2].add(new StringBuilder(collapsed));
			} else {
				levels[1].add(new StringBuilder(collapsed));
				levels[2].add(new StringBuilder("\\a{+}"));
			}

		}

		return matchType != null;
	}

	private Map.Entry<String, Integer> getBest(int levelIndex) {
		ArrayList<StringBuilder> level = levels[levelIndex];
		if (level.isEmpty())
			return null;

		Map<String, Integer> map = new HashMap<String, Integer>();

		// Calculate the frequency of every element
		for (StringBuilder s : level) {
			String key = s.toString();
			Integer seen = map.get(key);
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
		for (Map.Entry<String, Integer> entry : map.entrySet()) {
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

		String bestKey = best.getKey();
		String secondBestKey;
		if (secondBest != null) {
			secondBestKey = secondBest.getKey();

			PatternInfo bestPattern = patternInfo.get(bestKey);
			PatternInfo secondBestPattern = patternInfo.get(secondBestKey);
			if (bestPattern != null && secondBestPattern != null) {
				if (bestPattern.isNumeric() && secondBestPattern.isNumeric()) {
					if (!bestPattern.type.equals(secondBestPattern.type)) {
						// Promote Long to Double
						String newKey = "Double".equals(bestPattern.type) ? best.getKey() : secondBest.getKey();
						best = new AbstractMap.SimpleEntry<String, Integer>(newKey,
								best.getValue() + secondBest.getValue());
					}
				} else if ("String".equals(bestPattern.type)) {
					// Promote anything to "String"
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
			matchPattern = "\\a{+}";
			matchPatternInfo = patternInfo.get(matchPattern);
			matchType = matchPatternInfo.type;
			return;
		}

		int level0value = 0, level1value = 0, level2value = 0;
		String level0pattern = null, level1pattern = null, level2pattern = null;
		PatternInfo level0patternInfo = null, level1patternInfo = null, level2patternInfo = null;
		String pattern = null;
		Map.Entry<String, Integer> level0 = getBest(0);
		Map.Entry<String, Integer> level1 = getBest(1);
		Map.Entry<String, Integer> level2 = getBest(2);
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

			if (matchType == null) {
				if (matchPatternInfo != null) {
					matchType = matchPatternInfo.type;
				}

				if (matchType != null) {
					if (possibleDateTime == raw.size()) {
						DateTimeParser det = new DateTimeParser();
						for (String sample : raw)
							det.train(sample);

						DateTimeParserResult result = det.getResult();
						result.forceResolve(dayFirst);
						String formatString = result.getFormatString();
						matchPatternInfo = new PatternInfo("\\a{+}", result.getType(), -1, -1, null, formatString,
								formatString);
						matchType = matchPatternInfo.type;
					}
					// Do we have a set of possible emails?
					if (possibleEmails == raw.size()) {
						PatternInfo save = matchPatternInfo;
						matchPatternInfo = new PatternInfo(matchPattern, "String", -1, -1, null, null, "Email");
						int emails = 0;
						for (String sample : raw)
							if (trackString(sample, false))
								emails++;
						// if at least 90% of them looked like a genuine email
						// then stay with email, otherwise back out to simple
						// String
						if (emails < .9 * raw.size())
							matchPatternInfo = save;
					}

					// Do we have a set of possible zip codes?
					if (possibleZips == raw.size()) {
						PatternInfo save = matchPatternInfo;
						pattern = "[ZIP]";
						matchPatternInfo = patternInfo.get(pattern);

						int zipCount = 0;
						for (String sample : raw)
							if (trackLong(sample, false))
								zipCount++;
						// if at least 90% of them looked like a genuine zip
						// then stay with zip, otherwise back out to simple Long
						if (zipCount < .9 * raw.size()) {
							matchPatternInfo = save;
							pattern = save.pattern;
						}
						matchType = matchPatternInfo.type;
					}

					for (String sample : raw)
						trackResult(sample);

					matchCount = best.getValue();
					matchPattern = pattern;
				}
			}
		}
	}

	private void addValid(String input) {
		Integer seen = cardinality.get(input);
		if (seen == null) {
			if (cardinality.size() < maxCardinality)
				cardinality.put(input, 1);
		} else
			cardinality.put(input, seen + 1);
	}

	private void outlier(String input) {
		Integer seen = outliers.get(input);
		if (seen == null) {
			if (outliers.size() < maxOutliers)
				outliers.put(input, 1);
		} else {
			outliers.put(input, seen + 1);
		}
	}

	private void trackResult(String input) {
		// We always want to track basic facts for the field
		int length = input.length();

		if (length != 0 && length < minRawLength)
			minRawLength = length;
		if (length > maxRawLength)
			maxRawLength = length;

		// If the cache is full and we have not determined a type compute one
		if (matchType == null && sampleCount - (nullCount + blankCount) > samples) {
			determineType();
		}

		if (matchType == null) {
			return;
		}

		switch (matchType) {
		case "Boolean":
			if (trackBoolean(input)) {
				matchCount++;
				addValid(input);
				return;
			}
			outlier(input);
			break;

		case "Long":
			if (trackLong(input, true)) {
				matchCount++;
				addValid(input);
				return;
			}
			outlier(input);
			break;

		case "Double":
			if (trackDouble(input)) {
				matchCount++;
				addValid(input);
				return;
			}
			outlier(input);
			break;

		case "String":
			if (trackString(input, true)) {
				matchCount++;
				addValid(input);
				return;
			}
			outlier(input);
			break;

		case "Date":
		case "Time":
		case "DateTime":
			try {
				trackDateTime(matchPatternInfo.format, input);
				matchCount++;
				addValid(input);
				return;
			}
			catch (DateTimeParseException e) {
				if ("Insufficient digits in input (d)".equals(e.getMessage()) || "Insufficient digits in input (M)".equals(e.getMessage())) {
					try {
						String formatString = new StringBuffer(matchPatternInfo.format).deleteCharAt(e.getErrorIndex()).toString();
						matchPatternInfo = new PatternInfo("\\a{+}", matchPatternInfo.type, -1, -1, null, formatString,
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
			outlier(input);
			break;
		}
	}

	/**
	 * Parse a String regexp to determine length.
	 *
	 * @param input
	 *            String input that must be either a variable length string
	 *            (\a{+}) or fixed length, e.g. \a{3}
	 * @return The length of the input string or -1 if length is variable
	 */
	private int determineLength(String input) {
		String lengthInformation = input.substring(3, input.length() - 1);
		return Character.isDigit(lengthInformation.charAt(0)) ? Integer.parseInt(lengthInformation) : -1;
	}

	/**
	 * Determine if the current dataset reflects a logical type (of uniform length) as defined by the provided set.
	 * @param uniformSet The set of items with a uniform length that reflect this logical type
	 * @param successPattern The logical type that this set represents
	 * @return True if we believe that this data set is defined by the provided set
	 */
	private boolean checkUniformLengthSet(HashSet<String> uniformSet, String successPattern) {
		int realSamples = sampleCount - (nullCount + blankCount);
		int validCount = 0;
		int misses = 0;					// count of number of groups that are misses
		int missCount = 0;				// count of number of misses

		// Sweep the current outliers and check they are part of the set
		for (Map.Entry<String, Integer> entry : outliers.entrySet()) {
			misses++;
			missCount += entry.getValue();
			// It makes sense to break out early if we know we are going to fail
			if ((double) missCount / realSamples > .05)
				return false;
		}

		// Sweep the balance and check they are part of the set
		Map<String, Integer> newOutliers = new HashMap<String, Integer>();
		if ((double) missCount / realSamples <= .05) {
			for (Map.Entry<String, Integer> entry : cardinality.entrySet()) {
				if (uniformSet.contains(entry.getKey().toUpperCase(Locale.ROOT)))
					validCount += entry.getValue();
				else {
					misses++;
					missCount += entry.getValue();
					newOutliers.put(entry.getKey(), entry.getValue());
					// It makes sense to break out early if we know we are going to fail
					if ((double) missCount / realSamples > .05)
						return false;
				}
			}
		}

		// To declare success we need fewer than 5% failures by count and additionally fewer than 4 groups
		if ((double) missCount / realSamples > .05 || misses >= 4)
			return false;

		matchPattern = successPattern;
		matchCount = validCount;
		matchPatternInfo = patternInfo.get(matchPattern);
		outliers.putAll(newOutliers);
		cardinality.keySet().removeAll(newOutliers.keySet());

		return true;
	}

	/**
	 * Determine if the current dataset reflects a logical type (of variable length) as defined by the provided set.
	 * @param variableSet The set of items that reflect this logical type
	 * @param successPattern The logical type that this set represents
	 * @return True if we believe that this data set is defined by the provided set
	 */
	private boolean checkVariableLengthSet(HashSet<String> variableSet, String successPattern) {
		int realSamples = sampleCount - (nullCount + blankCount);
		int validCount = 0;
		int misses = 0;					// count of number of groups that are misses
		int missCount = 0;				// count of number of misses

		// Sweep the balance and check they are part of the set
		for (Map.Entry<String, Integer> entry : cardinality.entrySet()) {
			if (variableSet.contains(entry.getKey().toUpperCase(Locale.ROOT)))
				validCount += entry.getValue();
			else {
				misses++;
				missCount += entry.getValue();
				// It makes sense to break out early if we know we are going to fail
				if ((double) missCount / realSamples > .05)
					return false;
			}
		}

		// To declare success we need fewer than 5% failures by count and additionally fewer than 4 groups
		if ((double) missCount / realSamples > .05 || misses >= 4)
			return false;

		matchPattern = successPattern;
		matchCount = validCount;
		matchPatternInfo = patternInfo.get(matchPattern);
		return true;
	}

	/**
	 * Determine the result of the training complete to date. Typically invoked
	 * after all training is complete, but may be invoked at any stage.
	 *
	 * @return A TextAnalysisResult with the analysis of any training completed.
	 */
	public TextAnalysisResult getResult() {
		// If we have not already determined the type, now we need to
		if (matchType == null) {
			determineType();
		}

		// Compute our confidence
		double confidence = 0;
		int realSamples = sampleCount - (nullCount + blankCount);

		if (blankCount == sampleCount || nullCount == sampleCount) {
			matchPattern = blankCount == sampleCount ? "^[ ]*$" : "[NULL]";
			matchPatternInfo = patternInfo.get(matchPattern);
			matchCount = sampleCount;
			confidence = sampleCount >= 10 ? 1.0 : 0.0;
		} else {
			confidence = (double) matchCount / realSamples;
		}

		if ("[ZIP]".equals(matchPattern)) {
			// We thought it was a Zip, but on reflection it does not feel like
			// it
			if ((realSamples > REFLECTION_SAMPLES && confidence < 0.9) || cardinality.size() < 5) {
				if (totalLongs > .95 * realSamples) {
					matchPattern = "\\d{+}";
					matchCount = totalLongs;
				} else {
					matchPattern = "\\a{+}";
					matchCount = realSamples;
				}
				confidence = (double) matchCount / realSamples;
				matchPatternInfo = patternInfo.get(matchPattern);
			}
		}

		if ("\\d{+}".equals(matchPattern) && minLong > 1700 && maxLong < 2030) {
			matchPatternInfo = new PatternInfo("\\d{4}", "Date", -1, -1, null, "yyyy", "yyyy");
		} else if ("String".equals(matchPatternInfo.type)) {
			int length = determineLength(matchPattern);
			// We thought it was a fixed length string, but on reflection it does not feel like it
			if (length != -1 && realSamples > REFLECTION_SAMPLES && (double) matchCount / realSamples < 0.95) {
				matchPattern = "\\a{+}";
				matchPatternInfo = patternInfo.get(matchPattern);
				matchCount = realSamples;

				// All outliers are now part of the cardinality set and there are now no outliers
				cardinality.putAll(outliers);
				outliers.clear();

				confidence = (double) matchCount / realSamples;
			}
			if ("\\a{+}".equals(matchPattern)) {
				matchPattern = "\\a{" + minRawLength;
				if (minRawLength != maxRawLength)
					matchPattern += "," + maxRawLength;
				matchPattern += "}";
				matchPatternInfo = new PatternInfo(matchPattern, "String", minRawLength, maxRawLength, null, null,
						matchPatternInfo.typeQualifier);
			}

			boolean typeIdentified = false;
			if (realSamples > REFLECTION_SAMPLES && cardinality.size() > 1 && "\\a{3}".equals(matchPattern)
					&& cardinality.size() <= monthAbbr.size() + 2) {
				typeIdentified = checkUniformLengthSet(monthAbbr, "[MONTHABBR]");
			}

			if (!typeIdentified && realSamples > REFLECTION_SAMPLES && cardinality.size() > 1
					&& cardinality.size() <= countries.size()) {
				typeIdentified = checkVariableLengthSet(countries, "[COUNTRY]");
			}

			if (!typeIdentified && realSamples > REFLECTION_SAMPLES && "\\a{2}".equals(matchPattern)
					&& cardinality.size() < usStates.size() + caProvinces.size() + 5
					&& (name.toLowerCase().contains("state") || name.toLowerCase().contains("province")
							|| cardinality.size() > 5)) {
				int usStateCount = 0;
				int caProvinceCount = 0;
				int misses = 0;
				for (Map.Entry<String, Integer> entry : cardinality.entrySet()) {
					if (usStates.contains(entry.getKey().toUpperCase(Locale.ROOT)))
						usStateCount += entry.getValue();
					else if (caProvinces.contains(entry.getKey().toUpperCase(Locale.ROOT)))
						caProvinceCount += entry.getValue();
					else
						misses++;
				}

				if (misses < 3) {
					if (usStateCount != 0 && caProvinceCount != 0) {
						matchPattern = "[NA_STATE]";
						matchCount = usStateCount + caProvinceCount;
					} else if (usStateCount != 0) {
						matchPattern = "[US_STATE]";
						matchCount = usStateCount;
					} else if (caProvinceCount != 0) {
						matchPattern = "[CA_PROVINCE]";
						matchCount = caProvinceCount;
					}
					confidence = (double) matchCount / realSamples;
					matchPatternInfo = patternInfo.get(matchPattern);
				}
			}
		} else if ("\\d{+}".equals(matchPattern)) {
			if (cardinality.size() == 2 && minLong == 0 && maxLong == 1) {
				// boolean by any other name
				matchPattern = "[0|1]";
				minBoolean = false;
				maxBoolean = true;
				matchType = "Boolean";
				matchPatternInfo = patternInfo.get(matchPattern);
			} else {
				// We thought it was an integer field, but on reflection it does
				// not feel like it
				if (realSamples > REFLECTION_SAMPLES && confidence < 0.9) {
					matchPattern = "\\a{" + minRawLength;
					matchType = "String";
					matchCount = realSamples;
					confidence = 1.0;
					if (minRawLength != maxRawLength)
						matchPattern += "," + maxRawLength;
					matchPattern += "}";
					matchPatternInfo = new PatternInfo(matchPattern, "String", -1, -1, null, null, null);

					// All outliers are now part of the cardinality set and
					// there are no outliers
					cardinality.putAll(outliers);
					outliers.clear();
				} else {
					matchPattern = "\\d{" + minTrimmedLength;
					if (minTrimmedLength != maxTrimmedLength)
						matchPattern += "," + maxTrimmedLength;
					matchPattern += "}";
					matchPatternInfo = new PatternInfo(matchPattern, matchType, -1, -1, null, null, null);
				}
			}
		}

		boolean key = false;

		if (matchType != null) {
			switch (matchType) {
			case "Boolean":
				minValue = String.valueOf(minBoolean);
				maxValue = String.valueOf(maxBoolean);
				break;

			case "Long":
				minValue = String.valueOf(minLong);
				maxValue = String.valueOf(maxLong);
				sum = sumBI.toString();
				break;

			case "Double":
				minValue = String.valueOf(minDouble);
				maxValue = String.valueOf(maxDouble);
				sum = sumBD.toString();
				break;

			case "String":
				minValue = minString;
				maxValue = maxString;
				break;
			}

			// Attempt to identify keys?
			if (sampleCount > MIN_SAMPLES_FOR_KEY && maxCardinality >= MIN_SAMPLES_FOR_KEY / 2
					&& cardinality.size() >= maxCardinality && blankCount == 0 && nullCount == 0
					&& matchPatternInfo.typeQualifier == null
					&& (("String".equals(matchType) && minRawLength == maxRawLength && minRawLength < 32)
							|| "Long".equals(matchType))) {
				key = true;
				// Might be a key but only iff every element in the cardinality
				// set only has a count of 1
				for (Map.Entry<String, Integer> entry : cardinality.entrySet()) {
					if (entry.getValue() != 1) {
						key = false;
						break;
					}
				}
			}
		}

		return new TextAnalysisResult(matchCount, matchPatternInfo, sampleCount, nullCount, blankCount,
				totalLeadingZeros, confidence, minValue, maxValue, minRawLength, maxRawLength, sum, cardinality, outliers, key);
	}
}
