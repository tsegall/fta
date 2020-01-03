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

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * Analyze String data to determine whether input represents a date or datetime.
 *
 * <p>
 * Typical usage is:
 * </p>
 * <pre>
 * {@code
 *		DateTimeParser dtp = new DateTimeParser(false);
 *
 *		dtp.train("2/7/2012 06:24:47");
 *		dtp.train("2/7/2012 09:44:04");
 *		dtp.train("2/7/2012 06:21:38");
 *		dtp.train("1/7/2012 23:16:14");
 *		dtp.train("19/7/2012 17:49:53");
 *
 *		DateTimeParserResult result = dtp.getResult();
 *      // Expect "d/M/yyyy HH:mm:ss");
 *		System.err.println(result.getFormatString());
 * }
 * </pre>
 */
public class DateTimeParser {
	/** When we have ambiguity - should we prefer to conclude day first, month first or unspecified. */
	public enum DateResolutionMode {
		/** Result returned may have unbound elements, for example ??/??/yyyy. */
		None,
		/** In order to resolve ambiguity the day will be assumed to be first. */
		DayFirst,
		/** In order to resolve ambiguity the month will be assumed to be first. */
		MonthFirst,
		/** Auto will choose DayFirst or MonthFirst based on the Locale. */
		Auto
	}

	private DateResolutionMode resolutionMode = DateResolutionMode.None;
	private Locale locale;

	public static Set<String> timeZones = new HashSet<String>();
	private static final int monthDays[] = {-1, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

	static {
		// Cache the set of available Time Zones
		Collections.addAll(timeZones, TimeZone.getAvailableIDs());
		// Add the non-real Time Zones (that people use)
		timeZones.addAll(Arrays.asList(new String[] {
				"ACDT", "ACST", "ACT", "ACWDT", "ACWST", "ADS", "ADST", "ADT", "AEDT", "AEST", "AET", "AFST", "AFT", "AKDT", "AKST", "ALMST",
				"ALMT", "AMDT", "AMST", "AMT", "ANAST", "ANAT", "AQTST", "AQTT", "ARST", "ART", "AST", "AT", "AWDT", "AWST", "AZODT", "AZOST",
				"AZOT", "AZST", "AZT", "BDST", "BDT", "BNST", "BNT", "BOST", "BOT", "BRST", "BST", "BT", "BTST", "BTT", "CAST", "CAT", "CCST",
				"CCT", "CDST", "CDT", "CEDT", "CEST", "CET", "CHADT", "CHAST", "CHODST", "CHODT", "CHOST", "CHOT", "CHUST", "CHUT", "CIDST",
				"CIST", "CIT", "CKHST", "CKT", "CLDT", "CLST", "CLT", "COST", "COT", "CST", "CT", "CVST", "CVT", "CXST", "CXT", "ChDT", "ChST",
				"DAVST", "DAVT", "DDUST", "DDUT", "EADT", "EASST", "EAST", "EAT", "ECST", "ECT", "EDST", "EDT", "EEDT", "EEST", "EET", "EFATE",
				"EGST", "EGT", "EIST", "EST", "ET", "FET", "FJDT", "FJST", "FJT", "FKDT", "FKST", "FKT", "FNST", "FNT", "GALST", "GALT", "GAMST",
				"GAMT", "GDT", "GEST", "GET", "GFST", "GFT", "GHST", "GILST", "GILT", "GMT", "GST", "GT", "GYST", "GYT", "HAA", "HAC", "HADT",
				"HAE", "HAP", "HAR", "HAST", "HAT", "HDT", "HKST", "HKT", "HLV", "HNA", "HNC", "HNE", "HNP", "HNR", "HNT", "HOVDST", "HOVDT",
				"HOVST", "HOVT", "HST", "ICST", "ICT", "IDT", "IOST", "IOT", "IRDT", "IRKST", "IRKT", "IRST", "IST", "IT", "JDT", "JST", "KDT",
				"KGST", "KGT", "KIT", "KOSST", "KOST", "KRAST", "KRAT", "KST", "KT", "KUYT", "LHDT", "LHST", "LINST", "LINT", "MAGST", "MAGT",
				"MARST", "MART", "MAWST", "MAWT", "MCK", "MDST", "MDT", "MEST", "MESZ", "MEZ", "MHST", "MHT", "MIDT", "MMST", "MMT", "MSD", "MSK",
				"MST", "MT", "MUST", "MUT", "MVST", "MVT", "MYST", "MYT", "NACDT", "NACST", "NAEDT", "NAEST", "NAMDT", "NAMST", "NAPDT", "NAPST",
				"NCST", "NCT", "NDT", "NFST", "NFT", "NOVST", "NOVT", "NPST", "NPT", "NRST", "NRT", "NST", "NUST", "NUT", "NZDT", "NZST", "OESZ",
				"OEZ", "OMSST", "OMST", "ORAST", "ORAT", "PDST", "PDT", "PEST", "PET", "PETST", "PETT", "PGST", "PGT", "PHOST", "PHOT", "PHST",
				"PHT", "PKST", "PKT", "PMDT", "PMST", "PONST", "PONT", "PST", "PT", "PWST", "PWT", "PYST", "PYT", "Pacific", "QYZST", "QYZT",
				"REST", "RET", "ROTST", "ROTT", "SAKST", "SAKT", "SAMST", "SAMT", "SAST", "SBST", "SBT", "SCST", "SCT", "SDT", "SGST", "SGT",
				"SREDT", "SRET", "SRST", "SRT", "SST", "ST", "SYOST", "SYOT", "TAHST", "TAHT", "TFST", "TFT", "TJST", "TJT", "TKST", "TKT", "TLST",
				"TLT", "TMST", "TMT", "TOST", "TOT", "TRT", "TVST", "TVT", "ULAST", "ULAT", "UTC", "UYST", "UYT", "UZST", "UZT", "VEST", "VET",
				"VLAST", "VLAT", "VOSST", "VOST", "VUST", "VUT", "WAKST", "WAKT", "WARST", "WAST", "WAT", "WDT", "WEDT", "WEST", "WESZ", "WET",
				"WEZ", "WFST", "WFT", "WGST", "WGT", "WIB", "WIST", "WIT", "WITA", "WSDT", "WST", "WT", "XJDT", "YAKST", "YAKT", "YAPT", "YEKST",
				"YEKT" }));
	}

	private final Map<String, Integer> results = new HashMap<String, Integer>();
	private int sampleCount;
	private int nullCount;
	private int blankCount;
	private int invalidCount;

	// lenient allows dates of the form 00/00/00 etc to be viewed as valid for the purpose of Format detection
	private boolean lenient = true;

	DateTimeParser(final DateResolutionMode resolutionMode) {
		this(resolutionMode, Locale.getDefault());
	}

	DateTimeParser() {
		this(DateResolutionMode.None, Locale.getDefault());
	}

	DateTimeParser(final DateResolutionMode resolutionMode, Locale locale) {
		this.resolutionMode = resolutionMode;
		this.locale = locale;
	}

	private static final Map<String, DateTimeFormatter> formatterCache = new HashMap<>();

	/**
	 * Given an input string with a DateTimeFormatter pattern return a suitable DateTimeFormatter.
	 * This is very similar to DateTimeFormatter.ofPattern(), however, there are a set of key differences:
	 *  - This will cache the Formatters
	 *  - It supports a slightly extended syntax, the following are supported:
	 *    - Year only - "yyyy"
	 *    - S{min,max} to reflect a variable number of digits in a fractional seconds component
	 *    - Year month only - "MM/YYYY" or "MM-YYYY" or "YYYY/MM" or "YYYY-MM"
	 *  - The formatter returned is always case-insensitive
	 *
	 * @param formatString A DateTimeString using DateTimeFormatter patterns
	 * @param locale Locale the input string is in
	 * @return The corresponding DateTimeFormatter (note - this will be a case-insensitive parser).
	 */
	public static DateTimeFormatter ofPattern(final String formatString, Locale locale) {
		DateTimeFormatter formatter = formatterCache.get(locale.toLanguageTag() + "---" + formatString);

		if (formatter != null)
			return formatter;

		int fractionOffset = formatString.indexOf("S{");
		if (fractionOffset != -1)
			formatter = new DateTimeFormatterBuilder()
			.appendPattern(formatString.substring(0, fractionOffset))
			.appendFraction(ChronoField.MICRO_OF_SECOND, 1, 3, false)
			.toFormatter(locale);
		else if ("yyyy".equals(formatString))
            // The default formatter with "yyyy" will not default the month/day, make it so!
            formatter = new DateTimeFormatterBuilder()
            .appendPattern("yyyy")
            .parseCaseInsensitive()
            .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
            .toFormatter(locale);
		else if ("MM/yyyy".equals(formatString) || "MM-yyyy".equals(formatString) || "yyyy/MM".equals(formatString) || "yyyy-MM".equals(formatString))
			formatter = new DateTimeFormatterBuilder()
			.parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
			.append(DateTimeFormatter.ofPattern(formatString))
			.toFormatter(locale);
		else
			formatter = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(formatString).toFormatter(locale);

		formatterCache.put(locale.toLanguageTag() + "---" + formatString, formatter);

		return formatter;
	}

	/**
	 * Given an input string with a DateTimeFormatter pattern return a suitable DateTimeFormatter.
	 *
	 * @see #ofPattern(java.lang.String, Locale) for more detail
	 *
	 * @param formatString A DateTimeString using DateTimeFormatter patterns
	 * @return The corresponding DateTimeFormatter (note - this will be a case-insensitive parser).
	 */
	public static DateTimeFormatter ofPattern(final String formatString) {
		return ofPattern(formatString, Locale.getDefault());
	}

	/**
	 * Train is the core entry point used to supply input to the DateTimeParser.
	 * @param input The String representing a date with possible surrounding whitespace.
	 * @return A String representing the DateTime detected (Using DateTimeFormatter Patterns) or null if no match.
	 */
	public String train(final String input) {
		sampleCount++;

		if (input == null) {
			nullCount++;
			return null;
		}

		if (input.length() == 0) {
			blankCount++;
			return null;
		}

		final String trimmed = input.trim();
		final String ret = determineFormatString(trimmed, DateResolutionMode.None);
		if (ret == null) {
			invalidCount++;
			return null;
		}
		final Integer seen = results.get(ret);
		if (seen == null)
			results.put(ret, 1);
		else
			results.put(ret, seen + 1);

		return ret;
	}

	public static <K, V extends Comparable<? super V>> HashMap<K, V> sortByValue(final Map<K, V> map) {
		return map.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	private static String longString(final char c) {
		return "" + c + c + c + c;
	}

	/**
	 * Determine the result of the training complete to date. Typically invoked
	 * after all training is complete, but may be invoked at any stage.
	 * @return A DateTimeParserResult with the analysis of any training completed, or null if no answer.
	 */
	public DateTimeParserResult getResult() {
		// If we have no good samples, call it a day
		if (sampleCount == nullCount + blankCount + invalidCount)
			return null;

		DateTimeParserResult answerResult = null;
		StringBuffer answerBuffer = null;

		// If there is only one result then it must be correct :-)
		if (results.size() == 1) {
			answerResult = DateTimeParserResult.asResult(results.keySet().iterator().next(), resolutionMode, locale);
			// If we are fully bound then we are done!
			if (!answerResult.isDateUnbound() || resolutionMode == DateResolutionMode.None)
				return answerResult;
			answerResult = DateTimeParserResult.newInstance(answerResult);
			answerBuffer = new StringBuffer(answerResult.getFormatString());
		}
		else {

			// Sort the results of our training by value so that we consider the most frequent first
			final HashMap<String, Integer> byValue = sortByValue(results);

			// Iterate through all the results of our training, merging them to produce our best guess
			for (final Map.Entry<String, Integer> entry : byValue.entrySet()) {
				final String key = entry.getKey();
				final DateTimeParserResult result = DateTimeParserResult.asResult(key, resolutionMode, locale);

				// First entry
				if (answerBuffer == null) {
					answerBuffer = new StringBuffer(key);
					answerResult = DateTimeParserResult.newInstance(result);
					continue;
				}

				// Process any time-related information
				if (result.timeElements != -1) {
					if (answerResult.timeElements == -1)
						answerResult.timeElements = result.timeElements;
					if (answerResult.timeFirst == null)
						answerResult.timeFirst = result.timeFirst;
					if (answerResult.timeZone == null)
						answerResult.timeZone = result.timeZone;
					if (answerResult.amPmIndicator == null)
						answerResult.amPmIndicator = result.amPmIndicator;

					// If we were H (0-23) and we have a k (1-24) then assume k
					char was = answerBuffer.charAt(answerResult.timeFieldOffsets[0]);
					char is = result.getFormatString().charAt(result.timeFieldOffsets[0]);
					if (was == 'H' && is == 'k') {
						final int start = answerResult.timeFieldOffsets[0];
						final int len = answerResult.timeFieldLengths[0];
						answerBuffer.replace(start, start + len, longString('k').substring(0, len));
					}

					if (result.timeFieldLengths != null) {
						// Adjust the Hours, Minutes, or Seconds fields if the length is shorter (but not the fractions of Seconds)
						for (int i = 0; i < result.timeFieldLengths.length - 1; i++) {
							if (answerResult.timeFieldLengths[i] == -1)
								answerResult.timeFieldLengths[i] = result.timeFieldLengths[i];
							else
								if (result.timeFieldLengths[i] < answerResult.timeFieldLengths[i]) {
									final int start = answerResult.timeFieldOffsets[i];
									final int len = answerResult.timeFieldLengths[i];
									answerResult.timeFieldLengths[i] = result.timeFieldLengths[i];
									answerBuffer.replace(start, start + len, longString(answerBuffer.charAt(start)).substring(0, result.timeFieldLengths[i]));
								}
						}
					}
				}

				// Process any date-related information
				if (result.dateElements != -1) {
					if (answerResult.dayOffset == -1 && result.dayOffset != -1) {
						// We did not know where the day was and now do
						answerResult.dayOffset = result.dayOffset;
						final int start = answerResult.dateFieldOffsets[answerResult.dayOffset];
						final int len = answerResult.dateFieldLengths[answerResult.dayOffset];
						answerBuffer.replace(start, start + len, longString('d').substring(0, len));
					}
					if (answerResult.monthOffset == -1 && result.monthOffset != -1) {
						// We did not know where the month was and now do
						answerResult.monthOffset = result.monthOffset;
						final int start = answerResult.dateFieldOffsets[answerResult.monthOffset];
						final int len = answerResult.dateFieldLengths[answerResult.monthOffset];
						answerBuffer.replace(start, start + len, longString('M').substring(0, len));
					}
					if (answerResult.yearOffset == -1 && result.yearOffset != -1) {
						// We did not know where the year was and now do
						answerResult.yearOffset = result.yearOffset;
						final int start = answerResult.dateFieldOffsets[answerResult.yearOffset];
						final int len = answerResult.dateFieldLengths[answerResult.yearOffset];
						answerBuffer.replace(start, start + len, longString('y').substring(0, len));
					}

					if (answerResult.dateElements == -1)
						answerResult.dateElements = result.dateElements;
					if (answerResult.dateSeparator == null)
						answerResult.dateSeparator = result.dateSeparator;
					if (answerResult.dateTimeSeparator == null)
						answerResult.dateTimeSeparator = result.dateTimeSeparator;

					// If they result we are looking at has the same format as the current answer then merge lengths
					if (answerResult.yearOffset == result.yearOffset && answerResult.monthOffset == result.monthOffset)
						for (int i = 0; i < result.dateFieldLengths.length; i++) {
							if (answerResult.dateFieldLengths[i] == -1 && result.dateFieldLengths[i] != -1)
								answerResult.dateFieldLengths[i] = result.dateFieldLengths[i];
							else if (i != answerResult.yearOffset && answerResult.dateFieldLengths[i] != result.dateFieldLengths[i] && (result.dateFieldLengths[i] == 1 || result.dateFieldLengths[i] == 4)) {
								// Merge two date lengths:
								//  - d and dd -> d
								//  - M and MM -> M
								//  - MMM and MMMM -> MMMM
								final int start = answerResult.dateFieldOffsets[i];
								final int len = answerResult.dateFieldLengths[i];
								final int delta = answerResult.dateFieldLengths[i] - result.dateFieldLengths[i];
								for (int j = i + 1; j < result.dateFieldLengths.length; j++) {
									 answerResult.dateFieldOffsets[j] -= delta;
								}
								answerResult.dateFieldLengths[i] = result.dateFieldLengths[i];
								answerBuffer.replace(start, start + len, longString(answerBuffer.charAt(start)).substring(0, result.dateFieldLengths[i]));
							}
						}
				}
			}
		}

		// If we are supposed to be fully bound and still have some ambiguities then fix them based on the mode
		if (answerResult.isDateUnbound() && resolutionMode != DateResolutionMode.None)
			if (answerResult.monthOffset != -1) {
				int start = answerResult.dateFieldOffsets[0];
				answerBuffer.replace(start, start + answerResult.dateFieldLengths[0], longString('d').substring(0, answerResult.dateFieldLengths[0]));
				start = answerResult.dateFieldOffsets[2];
				answerBuffer.replace(start, start + answerResult.dateFieldLengths[2], longString('y').substring(0, answerResult.dateFieldLengths[2]));
			}
			else {
					final char firstField = resolutionMode == DateResolutionMode.DayFirst ? 'd' : 'M';
					final char secondField = resolutionMode == DateResolutionMode.DayFirst ? 'M' : 'd';
					int idx = answerResult.yearOffset == 0 ? 1 : 0;
					int start = answerResult.dateFieldOffsets[idx];
					answerBuffer.replace(start, start + answerResult.dateFieldLengths[idx], longString(firstField).substring(0, answerResult.dateFieldLengths[idx]));
					start = answerResult.dateFieldOffsets[idx + 1];
					answerBuffer.replace(start, start + answerResult.dateFieldLengths[idx + 1], longString(secondField).substring(0, answerResult.dateFieldLengths[idx + 1]));
			}

		if (answerResult.timeZone == null)
			answerResult.timeZone = "";

		answerResult.updateFormatString(answerBuffer.toString());
		return answerResult;
	}

	private static String retDigits(final int digitCount, final char patternChar) {
		final String ret = String.valueOf(patternChar);
		if (digitCount == 1)
			return ret;
		if (digitCount == 2)
			return ret + patternChar;
		return ret + patternChar + patternChar + patternChar;
	}

	private boolean plausibleDate(final int[] dateValues, final int[] dateDigits, final int[] fieldOffsets) {
		return plausibleDateCore(lenient, dateValues[fieldOffsets[0]], dateValues[fieldOffsets[1]],
				dateValues[fieldOffsets[2]], dateDigits[fieldOffsets[2]]);
	}

	public static boolean plausibleDateCore(boolean lenient, final int day, final int month, final int year, final int yearLength) {
		if (lenient && year == 0 && month == 0 && day == 0)
			return true;

		if (year == 0 && yearLength == 4)
			return false;
		if (month == 0 || month > 12)
			return false;
		if (day == 0 || day > monthDays[month])
			return false;
		return true;
	}

	private static String dateFormat(final int[] dateDigits, final char dateSeparator, final DateResolutionMode resolutionMode, final boolean yearKnown, boolean yearFirst) {
		if (yearFirst)
			switch (resolutionMode) {
			case None:
				return retDigits(dateDigits[0], yearKnown ? 'y' : '?')  + dateSeparator + retDigits(dateDigits[1], '?') + dateSeparator + retDigits(dateDigits[2], '?');
			case DayFirst:
				return  retDigits(dateDigits[0], 'y') + dateSeparator + retDigits(dateDigits[1], 'd') + dateSeparator + retDigits(dateDigits[2], 'M');
			case MonthFirst:
				return retDigits(dateDigits[0], 'y') + dateSeparator + retDigits(dateDigits[1], 'M') + dateSeparator + retDigits(dateDigits[2], 'd');
			default:
				throw new InternalErrorException("Internal error: unexpected resolutionMode: " + resolutionMode);
			}
		else
			switch (resolutionMode) {
			case None:
				return retDigits(dateDigits[0], '?') + dateSeparator + retDigits(dateDigits[1], '?') + dateSeparator + retDigits(dateDigits[2], yearKnown ? 'y' : '?');
			case DayFirst:
				return retDigits(dateDigits[0], 'd') + dateSeparator + retDigits(dateDigits[1], 'M') + dateSeparator + retDigits(dateDigits[2], 'y');
			case MonthFirst:
				return retDigits(dateDigits[0], 'M') + dateSeparator + retDigits(dateDigits[1], 'd') + dateSeparator + retDigits(dateDigits[2], 'y');
			default:
				throw new InternalErrorException("Internal error: unexpected resolutionMode: " + resolutionMode);
			}
	}

	/**
	 * Determine a FormatString from an input string that may represent a Date, Time,
	 * DateTime, OffsetDateTime or a ZonedDateTime.
	 * @param input The String representing a date with optional leading/trailing whitespace
	 * The resolution mode will default to the mode provided when the DateTimeParser was initially constructed.
	 * @return A String representing the DateTime detected (Using DateTimeFormatter Patterns) or null if no match.
	 */
	public String determineFormatString(final String input) {
		return determineFormatString(input,  resolutionMode);
	}

	/**
	 * Determine a FormatString from an input string that may represent a Date, Time,
	 * DateTime, OffsetDateTime or a ZonedDateTime.
	 * @param input The String representing a date with optional leading/trailing whitespace
	 * @param resolutionMode When we have ambiguity - should we prefer to conclude day first, month first or unspecified
	 * @return A String representing the DateTime detected (Using DateTimeFormatter Patterns) or null if no match.
	 */
	public String determineFormatString(final String input, final DateResolutionMode resolutionMode) {
		int len = input.length();

		// Remove leading spaces
		int start = 0;
		while (start < len && input.charAt(start) == ' ')
			start++;

		// Remove trailing spaces
		while (len >= 1 && input.charAt(len - 1) == ' ')
			len--;

		len -= start;

		final String trimmed = input.substring(start, len + start);

		// Fail fast if we can
		if (len < 4 || len > 50 || (!Character.isDigit(trimmed.charAt(0)) && !Character.isAlphabetic(trimmed.charAt(0))))
			return null;

		if (trimmed.indexOf('¶') != -1)
			return null;

		final SimpleDateMatcher matcher = new SimpleDateMatcher(trimmed, locale);

		// Initial pass is a simple match against a set of known patterns
		if (passOne(matcher) != null)
			return matcher.getFormat();

		// Fail fast if we can
		if (matcher.getComponentCount() < 2 || !Character.isDigit(trimmed.charAt(0)))
			return null;

		// Second pass is an attempt to 'parse' the provided input string and derive a format
		String attempt = passTwo(trimmed, resolutionMode);

		if (attempt != null)
			return attempt;

		// Third and final pass is brute force by elimination
		return passThree(trimmed, matcher, resolutionMode);
	}

	/**
	 * This is the first simple pass where we test against a set of predefined simple options.
	 *
	 * @param matcher The previously computed matcher which provides both the compressed form of the input as well as a component count
	 * @return a DateTimeFormatter pattern.
	 */
	String passOne(SimpleDateMatcher matcher) {
		if (!matcher.isKnown())
			return null;

		int[] dateValue = new int[] {-1, -1, -1};

		if (!matcher.parse())
			return null;

		dateValue[0] = matcher.getDayOfMonth();
		dateValue[1] = matcher.getMonthValue();
		dateValue[2] = matcher.getYear();

		if (!plausibleDate(dateValue, new int[] {matcher.getDayLength(), matcher.getMonthLength(), matcher.getYearLength()}, new int[] {0,1,2}))
			return null;

		return matcher.getFormat();
	}

	/**
	 * This is core 'intuitive' pass where we hunt in some logical fashion for something that looks like a date/time.
	 *
	 * @param trimmed The input we are scouring for a date/datetime/time
	 * @param resolutionMode When we have ambiguity - should we prefer to conclude day first, month first or unspecified
	 * @return a DateTimeFormatter pattern.
	 */
	String passTwo(String trimmed, final DateResolutionMode resolutionMode) {
		int len = trimmed.length();

		int digits = 0;
		int value = 0;
		int[] dateValue = new int[3];
		int[] dateDigits = new int[3];
		int[] timeValue = new int[4];
		int[] timeDigits = new int[4];
		char dateSeparator = '_';
		int dateComponent = 0;
		int timeComponent = 0;
		int hourLength = -1;
		boolean timeFirst = false;
		boolean yearInDateFirst = false;
		boolean fourDigitYear = false;
		boolean timeSeen = false;
		boolean timeClosed = false;
		boolean dateSeen = false;
		boolean dateClosed = false;
		String timeZone = "";
		String amPmIndicator = "";
		boolean iso8601 = false;

		int lastCh = '¶';
		for (int i = 0; i < len && timeZone.length() == 0; i++) {
			final char ch = trimmed.charAt(i);

			// Two spaces in a row is always bad news
			if (lastCh == ' ' && ch == ' ')
				return null;
			lastCh = ch;

			switch (ch) {
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				value = value * 10 + ch - '0';
				digits++;
				if (digits > 4)
					return null;
				break;

			case ':':
				if ((dateSeen && !dateClosed) || (timeSeen && timeClosed) || timeComponent == 3)
					return null;

				timeFirst = dateComponent == 0;
				timeSeen = true;
				timeValue[timeComponent] = value;
				timeDigits[timeComponent] = digits;

				if (timeComponent == 0) {
					if (digits != 1 && digits != 2)
						return null;
					hourLength = digits;
				}
				if (timeComponent == 1 && digits != 2)
					return null;
				if (timeComponent == 2)
					return null;
				timeComponent++;
				digits = 0;
				value = 0;
				break;

			case '+':
				// FALL THROUGH

			case '-':
				if (dateSeen && dateClosed && timeSeen && timeComponent >= 2) {
					int minutesOffset = Integer.MIN_VALUE;
					int secondsOffset = Integer.MIN_VALUE;

					i++;

					final String offset = SimpleDateMatcher.compress(trimmed.substring(i, len), locale);

					// Expecting DD:DD:DD or DDDDDD or DD:DD or DDDD or DD
					if (i + 8 <= len && "d{2}:d{2}:d{2}".equals(offset)) {
						timeZone = "xxxxx";
						minutesOffset = 3;
						secondsOffset = 6;
					}
					else if (i + 6 <= len && "d{6}".equals(offset)) {
						timeZone = "xxxx";
						minutesOffset = 2;
						secondsOffset = 4;
					}
					else if (i + 5 <= len && "d{2}:d{2}".equals(offset)) {
						timeZone = "xxx";
						minutesOffset = 3;
					}
					else if (i + 4 <= len && "d{4}".equals(offset)) {
						timeZone = "xx";
					}
					else if (i + 2 <= len && "d{2}".equals(offset)) {
						timeZone = "x";
					}
					else
						return null;

					// Validate the hours
					int hours = Utils.getValue(trimmed, i, 2, 2);
					if (hours != Integer.MIN_VALUE && hours > 18)
						return null;

					// Validate the minutes
					if (minutesOffset != Integer.MIN_VALUE) {
						final int minutes = Utils.getValue(trimmed, i + minutesOffset, 2, 2);
						if (minutes != Integer.MIN_VALUE && minutes > 59)
							return null;
					}

					// Validate the seconds
					if (secondsOffset != Integer.MIN_VALUE) {
						final int seconds = Utils.getValue(trimmed, i + secondsOffset, 2, 2);
						if (seconds != Integer.MIN_VALUE && seconds > 59)
							return null;
					}
					break;
				}
				// FALL THROUGH

			case '/':
				if (timeSeen && !timeClosed)
					return null;
				if (dateComponent == 2)
					return null;

				dateSeen = true;
				dateValue[dateComponent] = value;
				dateDigits[dateComponent] = digits;
				if (dateComponent == 0) {
					dateSeparator = ch;
					fourDigitYear = digits == 4;
					yearInDateFirst = fourDigitYear || (digits == 2 && value > 31);
					if (!yearInDateFirst && digits != 1 && digits != 2)
						return null;
				} else if (dateComponent == 1) {
					if (ch != dateSeparator)
						return null;
					if (digits != 1 && digits != 2)
						return null;
				}

				dateComponent++;
				digits = 0;
				value = 0;
				break;

			case '.':
				// If are not processing the time component
				if ((!timeSeen || timeClosed)) {
					if (dateComponent == 2)
						return null;

					// Expecting a 'dotted' date - e.g. 9.12.2008
					dateSeen = true;
					dateValue[dateComponent] = value;
					dateDigits[dateComponent] = digits;
					if (dateComponent == 0) {
						dateSeparator = ch;
						fourDigitYear = digits == 4;
						yearInDateFirst = fourDigitYear || (digits == 2 && value > 31);
						if (!yearInDateFirst && digits != 1 && digits != 2)
							return null;
					} else if (dateComponent == 1) {
						if (ch != dateSeparator)
							return null;
						if (digits != 1 && digits != 2)
							return null;
					}

					dateComponent++;
				}
				else {
					if ((dateSeen && !dateClosed) || (timeSeen && timeClosed) || timeComponent != 2 || digits != 2)
						return null;
					timeValue[timeComponent] = value;
					timeDigits[timeComponent] = digits;
					timeComponent++;
				}
				digits = 0;
				value = 0;
				break;

			case ' ':
				if (!dateSeen && !timeSeen)
					return null;
				if (timeSeen && !timeClosed) {
					if (digits != 2)
						return null;
					timeValue[timeComponent] = value;
					timeDigits[timeComponent] = digits;
					timeClosed = true;
				}
				else if (dateSeen && !dateClosed) {
					if (dateComponent != 2)
						return null;
					if (!(digits == 2 || (yearInDateFirst == false && digits == 4)))
						return null;
					if (!fourDigitYear)
						fourDigitYear = digits == 4;
					dateValue[dateComponent] = value;
					dateDigits[dateComponent] = digits;
					dateClosed = true;
				}
				else
					return null;
				digits = 0;
				value = 0;
				break;

			default:
				if (!Character.isAlphabetic(ch))
					return null;

				if (timeSeen) {
					String rest = trimmed.substring(i).toUpperCase(locale);
					boolean ampmDetected = false;
					for (String s : LocaleInfo.getAMPMStrings(locale)) {
						if (rest.startsWith(s)) {
							amPmIndicator = trimmed.charAt(i - 1) == ' ' ? " a" : "a";
							i += s.length();
							ampmDetected = true;
						}
					}

					if (ampmDetected) {
						// Eat the space after if it exists
						if (i + 1 < len && trimmed.charAt(i + 1) == ' ')
							i++;
					}
					else {
						if (ch == 'Z')
							timeZone = "X";
						else {
							final String currentTimeZone = trimmed.substring(i, len);
							if (!DateTimeParser.timeZones.contains(currentTimeZone))
								return null;
							timeZone = " z";
						}
					}
				}
				else {
					if (ch == 'T') {
						// ISO 8601
						if (timeSeen)
							return null;
						if (!dateSeen || dateClosed || digits != 2 || dateSeparator != '-' || !fourDigitYear || !yearInDateFirst)
							return null;
						iso8601 = true;
						dateValue[dateComponent] = value;
						dateDigits[dateComponent] = digits;
						dateClosed = true;
						digits = 0;
						value = 0;
					}
					else
						return null;
				}
				break;
			}
		}

		if (!dateSeen && !timeSeen)
			return null;

		if (dateSeen && !dateClosed) {
			// Need to close out the date
			if (yearInDateFirst) {
				if (digits != 2)
					return null;
			}
			else {
				if (digits != 2 && digits != 4)
					return null;
			}
			fourDigitYear = digits == 4;
			if (dateComponent != 2)
				return null;
			dateValue[dateComponent] = value;
			dateDigits[dateComponent] = digits;
			digits = 0;
		}
		if (timeSeen && !timeClosed) {
			// Need to close out the time
			if ((timeComponent != 3 && digits != 2) || (timeComponent == 3 && (digits == 0 || digits > 9)))
				return null;
			timeValue[timeComponent] = value;
			timeDigits[timeComponent] = digits;
			digits = 0;
		}

		if (digits != 0)
			return null;

		if (iso8601 && timeComponent == 0)
			return null;

		String timeAnswer = null;
		if (timeComponent != 0) {
			if (timeValue[1] > 59 || (timeComponent >= 2 && timeValue[2] > 59))
				return null;
			String hours;
			if (amPmIndicator.length() != 0) {
				if (timeValue[0] > 12)
					return null;
				hours = hourLength == 1 ? "h" : "hh";
			}
			else {
				if (timeValue[0] > 24)
					return null;
				if (timeValue[0] == 24)
					hours = hourLength == 1 ? "k" : "kk";
				else
					hours = hourLength == 1 ? "H" : "HH";
			}
			timeAnswer = hours + ":mm";
			if (timeComponent >= 2)
				timeAnswer += ":ss";
			if (timeComponent == 3)
				timeAnswer += "." + "SSSSSSSSS".substring(0, timeDigits[3]);
			if (dateComponent == 0)
				return timeAnswer + amPmIndicator;
		}

		String dateAnswer = null;

		// Do we have any date components?
		if (dateComponent != 0) {
			// If we don't have two date components then it is invalid
			if (dateComponent == 1)
				return null;
			boolean freePass = lenient && dateValue[0] == 0 && dateValue[1] == 0 && dateValue[2] == 0;
			if ((!freePass && dateValue[1] == 0) || dateValue[1] > 31)
				return null;
			if (yearInDateFirst) {
				if (iso8601 || dateValue[2] > 12) {
					if (!plausibleDate(dateValue, dateDigits, new int[] {2,1,0}))
						return null;
					dateAnswer = retDigits(dateDigits[0], 'y') + dateSeparator + retDigits(dateDigits[1], 'M') + dateSeparator + retDigits(dateDigits[2], 'd');
				}
				else if (dateValue[1] > 12) {
					if (!plausibleDate(dateValue, dateDigits, new int[] {1,2,0}))
						return null;
					dateAnswer = retDigits(dateDigits[0], 'y') + dateSeparator + retDigits(dateDigits[1], 'd') + dateSeparator + retDigits(dateDigits[2], 'M');
				}
				else
					dateAnswer = dateFormat(dateDigits, dateSeparator, resolutionMode, true, true);
			}
			else {
				if (fourDigitYear) {
					// Year is the last field - attempt to determine which is the month
					if (dateValue[0] > 12) {
						if (!plausibleDate(dateValue, dateDigits, new int[] {0,1,2}))
							return null;
						dateAnswer = "dd" + dateSeparator + retDigits(dateDigits[1], 'M') + dateSeparator + "yyyy";
					}
					else if (dateValue[1] > 12) {
						if (!plausibleDate(dateValue, dateDigits, new int[] {1,0,2}))
							return null;
						dateAnswer = retDigits(dateDigits[0], 'M') + dateSeparator + "dd" + dateSeparator + "yyyy";
					}
					else
						dateAnswer = dateFormat(dateDigits, dateSeparator, resolutionMode, true, false);
				} else {
					// If the first group of digits is of length 1, then it is either d/MM/yy or M/dd/yy
					if (dateDigits[0] == 1) {
						if (!freePass && dateValue[0] == 0)
							return null;
						if (dateValue[1] > 12) {
							if (!plausibleDate(dateValue, dateDigits, new int[] {1,0,2}))
								return null;
							dateAnswer = retDigits(dateDigits[0], 'M') + dateSeparator + "dd" + dateSeparator + "yy";
						}
						else
							dateAnswer = dateFormat(dateDigits, dateSeparator, resolutionMode, true, false);
					}
					// If year is the first field - then assume yy/MM/dd
					else if (dateValue[0] > 31)
						dateAnswer = "yy" + dateSeparator + retDigits(dateDigits[1], 'M') + dateSeparator + retDigits(dateDigits[2], 'd');
					else if (dateValue[2] > 31) {
						// Year is the last field - attempt to determine which is the month
						if (dateValue[0] > 12) {
							if (!plausibleDate(dateValue, dateDigits, new int[] {0,1,2}))
								return null;
							dateAnswer = "dd" + dateSeparator + retDigits(dateDigits[1], 'M') + dateSeparator + "yy";
						}
						else if (dateValue[1] > 12) {
							if (!plausibleDate(dateValue, dateDigits, new int[] {1,0,2}))
								return null;
							dateAnswer = retDigits(dateDigits[0], 'M') + dateSeparator + "dd" + dateSeparator + "yy";
						}
						else
							dateAnswer = dateFormat(dateDigits, dateSeparator, resolutionMode, true, false);
					} else if (dateValue[1] > 12) {
						if (!plausibleDate(dateValue, dateDigits, new int[] {1,0,2}))
							return null;
						dateAnswer = retDigits(dateDigits[0], 'M') + dateSeparator + "dd" + dateSeparator + "yy";
					} else if (dateValue[0] > 12 && dateValue[2] > 12) {
						dateAnswer = "??" + dateSeparator + retDigits(dateDigits[1], 'M') + dateSeparator + "??";
					} else
						dateAnswer = dateFormat(dateDigits, dateSeparator, resolutionMode, false, false);
				}
			}
			if (timeComponent == 0)
				return dateAnswer + timeZone;
		}

		if (timeFirst)
			return timeAnswer + amPmIndicator + (iso8601 ? "'T'" : " ") + dateAnswer + timeZone;
		else
			return dateAnswer + (iso8601 ? "'T'" : " ") + timeAnswer + amPmIndicator + timeZone;
	}

	/**
	 * This is our last attempt to construct a date pattern from the input.
	 *
	 * We use a brute force approach to repeatedly remove 'known' good patterns until we end up with a fully qualified pattern.
	 * This technique has the advantage that it is relatively forgiving of input with strange characters used to separate the components.
	 * For example, this will recognize input like the following:
	 * 	2017-10-12 16:45:30,403 or 2015-12-03:16:03:50 or 01APR2019
	 *
	 * @param trimmed The input we are scouring for a date/datetime/time
	 * @param matcher The previously computed matcher which provides both the compressed form of the input as well as a component count
	 * @param resolutionMode When we have ambiguity - should we prefer to conclude day first, month first or unspecified
	 * @return a DateTimeFormatter pattern.
	 */
	String passThree(String trimmed, SimpleDateMatcher matcher, final DateResolutionMode resolutionMode) {
		String compressed = matcher.getCompressed();
		int components = matcher.getComponentCount();

		if (components > 6) {
			if (compressed.indexOf("d{3}") == -1)
				return null;
			compressed = Utils.replaceFirst(compressed, "d{3}", "SSS");
			components--;
		}

		if (components >= 3 && compressed.indexOf("d{2}:d{2}:d{2}") != -1) {
			compressed = Utils.replaceFirst(compressed, "d{2}:d{2}:d{2}", "HH:mm:ss");
			components -= 3;
		}

		if (components > 3)
			return null;

		if (components == 2) {
			// We only support MM/yyyy, MM-yyyy, yyyy/MM, and yyyy-MM
			if (compressed.length() != 9)
				return null;
			int year = compressed.indexOf("d{4}");
			if (year == -1)
				return null;
			int month = compressed.indexOf("d{2}");
			if (month == -1)
				return null;

			char separator = compressed.charAt(4);
			if (separator != '/' && separator != '-')
				return null;

			if (year == 5 && month == 0)
				compressed = "MM" + separator + "yyyy";
			else if (year == 0 && month == 5)
				compressed = "yyyy" + separator + "MM";
			else
				return null;
		}
		else {
			int alreadyResolved = 0;
			int yearIndex = compressed.indexOf("d{4}");
			if (yearIndex != -1) {
				compressed = Utils.replaceFirst(compressed, "d{4}", "yyyy");
				components--;
				alreadyResolved++;
			}

			int monthIndex = compressed.indexOf("MMM");
			if (monthIndex != -1) {
				components--;
				alreadyResolved++;
			}

			// At this point we need at most two unresolved components
			if (alreadyResolved == 0 || components + alreadyResolved != 3)
				return null;

			if (components == 1) {
				if (compressed.indexOf("d{2}") == -1)
					return null;
				compressed = Utils.replaceFirst(compressed, "d{2}", "dd");
			}
			else {
				int first = compressed.indexOf("d{2}");
				int second = compressed.indexOf("d{2}", first + 4);

				if (first == -1 || second == -1)
					return null;

				if (monthIndex != -1)
					compressed = Utils.replaceFirst(Utils.replaceFirst(compressed, "d{2}", "dd"), "d{2}", "yy");
				else
					if (first > yearIndex)
						compressed = Utils.replaceFirst(Utils.replaceFirst(compressed, "d{2}", "MM"), "d{2}", "dd");
					else {
						int firstValue = Utils.getValue(trimmed, first, 2, 2);
						int secondValue = Utils.getValue(trimmed, second, 2, 2);

						if (firstValue > 12)
							compressed = Utils.replaceFirst(Utils.replaceFirst(compressed, "d{2}", "dd"), "d{2}", "MM");
						else if (secondValue > 12)
							compressed = Utils.replaceFirst(Utils.replaceFirst(compressed, "d{2}", "MM"), "d{2}", "dd");
						else
							if (resolutionMode == DateResolutionMode.DayFirst)
								compressed = Utils.replaceFirst(Utils.replaceFirst(compressed, "d{2}", "dd"), "d{2}", "MM");
							else
								compressed = Utils.replaceFirst(Utils.replaceFirst(compressed, "d{2}", "MM"), "d{2}", "dd");
					}

			}
		}

		// So we think we have nailed it - but it only counts if it happily passes a validity check

		DateTimeParserResult dtp = DateTimeParserResult.asResult(compressed, resolutionMode, locale);

		return dtp.isValid(trimmed) ? compressed : null;
	}
}
