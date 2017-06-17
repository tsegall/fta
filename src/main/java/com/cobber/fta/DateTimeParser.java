package com.cobber.fta;

import java.text.DateFormatSymbols;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class DateTimeParser {

	static class SimpleDateMatcher {
		String matcher;
		String format;
		int monthOffset;

		SimpleDateMatcher(String matcher, String format, int monthOffset) {
			this.matcher = matcher;
			this.format = format;
			this.monthOffset = monthOffset;
		}
	}
	static HashSet<String> monthAbbr = new HashSet<String>();
	static HashMap<String, SimpleDateMatcher> simpleDateMatcher = new HashMap<String, SimpleDateMatcher>();

	static {
		// Setup the Monthly abbreviations
		String[] shortMonths = new DateFormatSymbols().getShortMonths();
		for (String shortMonth : shortMonths) {
			monthAbbr.add(shortMonth.toUpperCase(Locale.ROOT));
		}

		simpleDateMatcher.put("þþþþ þþ þþ", new SimpleDateMatcher("þþþþ þþ þþ", "yyyy MM dd", -1));
		simpleDateMatcher.put("þþþþ þ þþ", new SimpleDateMatcher("þþþþ þ þþ", "yyyy M dd", -1));
		simpleDateMatcher.put("þþþþ þþ þ", new SimpleDateMatcher("þþþþ þþ þ", "yyyy MM d", -1));
		simpleDateMatcher.put("þþþþ þ þ", new SimpleDateMatcher("þþþþ þ þ", "yyyy M d", -1));

		simpleDateMatcher.put("þþ 999 þþþþ", new SimpleDateMatcher("þþ 999 þþþþ", "dd MMM yyyy", 3));
		simpleDateMatcher.put("þ 999 þþþþ", new SimpleDateMatcher("þ 999 þþþþ", "d MMM yyyy", 2));
		simpleDateMatcher.put("þþ-999-þþþþ", new SimpleDateMatcher("þþ-999-þþþþ", "dd-MMM-yyyy", 3));
		simpleDateMatcher.put("þ-999-þþþþ", new SimpleDateMatcher("þ-999-þþþþ", "d-MMM-yyyy", 2));

		simpleDateMatcher.put("þþ 999 þþ", new SimpleDateMatcher("þþ 999 þþ", "dd MMM yy", 3));
		simpleDateMatcher.put("þ 999 þþ", new SimpleDateMatcher("þ 999 þþ", "d MMM yy", 2));
		simpleDateMatcher.put("þþ-999-þþ", new SimpleDateMatcher("þþ-999-þþ", "dd-MMM-yy", 3));
		simpleDateMatcher.put("þ-999-þþ", new SimpleDateMatcher("þ-999-þþ", "d-MMM-yy", 2));
	}

	static boolean isValidMonthAbbreviation(String month) {
		return monthAbbr.contains(month.toUpperCase(Locale.ROOT));
	}

	Map<String, Integer> results = new HashMap<String, Integer>();
	int sampleCount = 0;
	int nullCount = 0;
	int blankCount = 0;
	int invalidCount = 0;

	/**
	 * Train is the core entry point used to supply input to the DateTimeParser.
	 * @param input The String representing a date with possible surrounding whitespace.
	 * @return A String representing the DateTime detected (Using DateTimeFormatter Patterns) or null if no match.
	 */
	public String train(String input) {
		if (input == null) {
			nullCount++;
			return null;
		}

		String trimmed = input.trim();
		if (input.length() == 0) {
			blankCount++;
			return null;
		}

		String ret = determineFormatString(trimmed);
		if (ret == null) {
			invalidCount++;
			return null;
		}
		Integer seen = results.get(ret);
		if (seen == null)
			results.put(ret, 1);
		else
			results.put(ret, seen + 1);

		return ret;
	}

	private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		return map.entrySet().stream().sorted(Map.Entry.comparingByValue())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	/**
	 * Determine the result of the training complete to date. Typically invoked
	 * after all training is complete, but may be invoked at any stage.
	 * @return A DateTimeParserResult with the analysis of any training completed.
	 */
	public DateTimeParserResult getResult() {
		int timeElements = -1;
		int hourLength = -1;
		int dateElements = -1;
		int[] dateFieldLengths = new int[] {-1,-1,-1};
		Boolean timeFirst = null;
		int dayOffset = -1;
		int monthOffset = -1;
		int yearOffset = -1;
		Character dateSeparator = null;
		Character dateTimeSeparator = null;
		String timeZone = null;
		int located = 0;

		// Sort the results of our training by value so that we consider the most frequent first
		Map<String, Integer> byValue = sortByValue(results);

		// Iterate through all the results of our training merging them to produce our best guess
		for (Map.Entry<String, Integer> entry : byValue.entrySet()) {
			DateTimeParserResult result = DateTimeParserResult.asResult(entry.getKey());
			if (result == null)
				System.err.println("NOT FOUND - input: '" + entry.getKey() + "'");
			if (timeElements == -1)
				timeElements = result.timeElements;
			if (hourLength == -1 || result.hourLength == 1 && hourLength == 2)
				hourLength = result.hourLength;
			if (timeFirst == null)
				timeFirst = result.timeFirst;
			if (dateElements == -1)
				dateElements = result.dateElements;
			if (result.dateFieldLengths != null) {
				for (int i = 0; i < result.dateFieldLengths.length; i++) {
					if (dateFieldLengths[i] == -1)
						dateFieldLengths[i] = result.dateFieldLengths[i];
					else
						if (result.dateFieldLengths[i] < dateFieldLengths[i])
							dateFieldLengths[i] = result.dateFieldLengths[i];
				}
			}
			if (dayOffset == -1 && result.dayOffset != -1) {
				dayOffset = result.dayOffset;
				located++;
			}
			if (monthOffset == -1 && result.monthOffset != -1) {
				monthOffset = result.monthOffset;
				located++;
			}
			if (yearOffset == -1 && result.yearOffset != -1) {
				yearOffset = result.yearOffset;
				located++;
			}
			if (dateSeparator == null)
				dateSeparator = result.dateSeparator;
			if (dateTimeSeparator == null)
				dateTimeSeparator = result.dateTimeSeparator;
			if (timeZone == null)
				timeZone = result.timeZone;
		}

		// If we have matched two of the date fields, then by elimination the remaining unidentified field is known
		if (located == 2) {
			if (dayOffset == -1)
				dayOffset = 3 - monthOffset - yearOffset;
			else if (monthOffset == -1)
				monthOffset = 3 - dayOffset - yearOffset;
			else if (yearOffset == -1)
				yearOffset = 3 - dayOffset - monthOffset;
		}

		if (timeZone == null)
			timeZone = "";

		return new DateTimeParserResult(null, timeElements, hourLength, dateElements, dateFieldLengths, timeFirst,
				dateTimeSeparator, yearOffset, monthOffset, dayOffset, dateSeparator, timeZone);
	}

	static String retDigits(int digitCount, char patternChar) {
		String ret = String.valueOf(patternChar);
		return digitCount == 1 ? ret : ret + patternChar;
	}

	/**
	 * Determine a FormatString from an input string that may represent a Date, Time or DateTime.
	 * @param input The String representing a date with trimmed of whitespace
	 * @return A String representing the DateTime detected (Using DateTimeFormatter Patterns) or null if no match.
	 */
	public static String determineFormatString(String input) {
		int len = input.length();

		// Fail fast if we can
		if (len < 4 || !Character.isDigit(input.charAt(0)))
			return null;

		// Cope with simple dates of the form '21 May 2017' or '9-Sep-2018'
		if (input.indexOf('þ') != -1)
			return null;
		String templated = input.replaceAll("[0-9]", "þ");
		templated = templated.replaceAll("[a-zA-Z]", "9");

		SimpleDateMatcher matcher = simpleDateMatcher.get(templated);
		if (matcher != null) {
			if (!DateTimeParser.isValidMonthAbbreviation(input.substring(matcher.monthOffset, matcher.monthOffset + 3)))
				return null;

			return matcher.format;
		}

		// Fail fast if we can
		if (!templated.contains("þ:þþ") && !templated.contains("þ/þþ") && !templated.contains("þ-þþ"))
			return null;

		int digits = 0;
		int value = 0;
		int[] dateValue = new int[3];
		int[] dateDigits = new int[3];
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
		boolean ISO8601 = false;

		for (int i = 0; i < len && timeZone.length() == 0; i++) {
			char ch = input.charAt(i);
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
				timeFirst = dateComponent == 0;
				timeSeen = true;
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
				break;

			case '-':
				if (ISO8601) {
					// Expecting DD:DD
					if (!"þþ:þþ".equals(templated.substring(len - 5, len)))
						return null;
					timeZone = "x";
					break;
				}
				// FALL THROUGH

			case '/':
				dateSeen = true;
				if (dateComponent == 2)
					return null;

				dateValue[dateComponent] = value;
				dateDigits[dateComponent] = digits;
				if (dateComponent == 0) {
					dateSeparator = ch;
					fourDigitYear = digits == 4;
					yearInDateFirst = fourDigitYear || value > 31;
					if (!yearInDateFirst && digits != 1 && digits != 2)
						return null;
				} else if (dateComponent == 1) {
					if (ch != dateSeparator)
						return null;
					if (digits != 1 && digits != 2)
						return null;
				} else if (dateComponent == 2)
					return null;
				dateComponent++;
				digits = 0;
				value = 0;
				break;

			case 'T':
				// ISO 8601
				if (!dateSeen || dateClosed || digits != 2 || dateSeparator != '-' || !fourDigitYear || !yearInDateFirst)
					return null;
				ISO8601 = true;
				dateValue[dateComponent] = value;
				dateDigits[dateComponent] = digits;
				dateClosed = true;
				digits = 0;
				value = 0;
				break;

			case ' ':
				if (timeSeen && !timeClosed) {
					if (digits != 2)
						return null;
					timeClosed = true;
				}
				if (dateSeen && !dateClosed) {
					if (digits != 2 && (yearInDateFirst == false && digits != 4))
						return null;
					fourDigitYear = digits == 4;
					dateValue[dateComponent] = value;
					dateDigits[dateComponent] = digits;
					dateClosed = true;
				}
				digits = 0;
				value = 0;
				if (timeSeen && dateSeen)
					timeZone = " z";
				break;

			case '+':
				if (!ISO8601)
					return null;

				// Expecting DD:DD
				if (!"þþ:þþ".equals(templated.substring(len - 5, len)))
					return null;
				timeZone = "x";
				break;

			default:
				return null;
			}
		}

		if (!dateSeen && !timeSeen)
			return null;

		if (dateSeen && !dateClosed) {
			// Need to close out the date
			if (digits != 2 && (yearInDateFirst == false && digits != 4))
				return null;
			fourDigitYear = digits == 4;
			dateValue[dateComponent] = value;
			dateDigits[dateComponent] = digits;
		}
		if (timeSeen && !timeClosed) {
			// Need to close out the time
			if (digits != 2)
				return null;
		}

		String timeAnswer = null;
		if (timeComponent != 0) {
			String hours = hourLength == 1 ? "H" : "HH";
			timeAnswer = hours + (timeComponent == 1 ? ":mm" : ":mm:ss");
			if (dateComponent == 0)
				return timeAnswer;
		}

		String dateAnswer = null;

		// Do we have any date components?
		if (dateComponent != 0) {
			// If we don't have two date components then it is invalid
			if (dateComponent == 1)
				return null;
			if (yearInDateFirst)
				dateAnswer = "yyyy" + dateSeparator + retDigits(dateDigits[1], 'M') + dateSeparator + retDigits(dateDigits[2], 'd');
			else {
				if (fourDigitYear) {
					// Year is the last field - attempt to determine which is the month
					if (dateValue[0] > 12)
						dateAnswer = "dd" + dateSeparator + retDigits(dateDigits[1], 'M') + dateSeparator + "yyyy";
					else if (dateValue[1] > 12)
						dateAnswer = retDigits(dateDigits[0], 'M') + dateSeparator + "dd" + dateSeparator + "yyyy";
					else
						dateAnswer = retDigits(dateDigits[0], 'X') + dateSeparator + retDigits(dateDigits[1], 'X') + dateSeparator + "yyyy";
				} else {
					// If year is the first field - then assume yy/MM/dd
					if (dateValue[0] > 31)
						dateAnswer = "yy" + dateSeparator + retDigits(dateDigits[1], 'M') + dateSeparator + retDigits(dateDigits[2], 'd');
					else if (dateValue[2] > 31) {
						// Year is the last field - attempt to determine which is the month
						if (dateValue[0] > 12)
							dateAnswer = "dd" + dateSeparator + retDigits(dateDigits[1], 'M') + dateSeparator + "yy";
						else if (dateValue[1] > 12)
							dateAnswer = retDigits(dateDigits[0], 'M') + dateSeparator + "dd" + dateSeparator + "yy";
						else
							dateAnswer = retDigits(dateDigits[0], 'X') + dateSeparator + retDigits(dateDigits[1], 'X') + dateSeparator + "yy";
					} else if (dateValue[1] > 12)
						dateAnswer = retDigits(dateDigits[0], 'M') + dateSeparator + "dd" + dateSeparator + "yy";
					else
						dateAnswer = retDigits(dateDigits[0], 'X') + dateSeparator + retDigits(dateDigits[1], 'X') + dateSeparator + retDigits(dateDigits[2], 'X');
				}
			}
			if (timeComponent == 0)
				return dateAnswer + timeZone;
		}

		if (timeFirst)
			return timeAnswer + (ISO8601 ? "T" : " ") + dateAnswer + timeZone;
		else
			return dateAnswer + (ISO8601 ? "T" : " ") + timeAnswer + timeZone;
	}
}
