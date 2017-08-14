package com.cobber.fta;

import java.text.DateFormatSymbols;
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
	// When we have ambiguity - should we prefer to conclude day first, month first or unspecified
	Boolean dayFirst = null;

	static class SimpleDateMatcher {
		String matcher;
		String format;
		int dayOffset;
		int dayLength;
		int monthOffset;
		int monthLength;
		int yearOffset;
		int yearLength;

		SimpleDateMatcher(String matcher, String format, int dayOffset, int dayLength, int monthOffset, int monthLength, int yearOffset, int yearLength) {
			this.matcher = matcher;
			this.format = format;
			this.dayOffset = dayOffset;
			this.dayLength = dayLength;
			this.monthOffset = monthOffset;
			this.monthLength = monthLength;
			this.yearOffset = yearOffset;
			this.yearLength = yearLength;
		}
	}
	static Map<String, Integer> monthAbbr = new HashMap<String, Integer>();
	static Set<String> timeZones = new HashSet<String>();
	static Map<String, SimpleDateMatcher> simpleDateMatcher = new HashMap<String, SimpleDateMatcher>();

	static {
		// Setup the Monthly abbreviations
		String[] shortMonths = new DateFormatSymbols().getShortMonths();
		for (int i = 0; i < shortMonths.length; i++) {
			monthAbbr.put(shortMonths[i].toUpperCase(Locale.ROOT), i + 1);
		}

		// Cache the set of available Time Zones
		Collections.addAll(timeZones, TimeZone.getAvailableIDs());

		simpleDateMatcher.put("þþþþ þþ þþ", new SimpleDateMatcher("þþþþ þþ þþ", "yyyy MM dd", 8, 2, 5, 2, 0, 4));
		simpleDateMatcher.put("þþþþ þ þþ", new SimpleDateMatcher("þþþþ þ þþ", "yyyy M dd", 7, 2, 5, 1, 0, 4));
		simpleDateMatcher.put("þþþþ þþ þ", new SimpleDateMatcher("þþþþ þþ þ", "yyyy MM d", 8, 1, 5, 2, 0, 4));
		simpleDateMatcher.put("þþþþ þ þ", new SimpleDateMatcher("þþþþ þ þ", "yyyy M d", 7, 1, 5, 1, 0, 4));

		simpleDateMatcher.put("þþ 999 þþþþ", new SimpleDateMatcher("þþ 999 þþþþ", "dd MMM yyyy", 0, 2, 3, 3, 7, 4));
		simpleDateMatcher.put("þ 999 þþþþ", new SimpleDateMatcher("þ 999 þþþþ", "d MMM yyyy", 0, 1, 2, 3, 6, 4));
		simpleDateMatcher.put("þþ-999-þþþþ", new SimpleDateMatcher("þþ-999-þþþþ", "dd-MMM-yyyy", 0, 2, 3, 3, 7, 4));
		simpleDateMatcher.put("þ-999-þþþþ", new SimpleDateMatcher("þ-999-þþþþ", "d-MMM-yyyy", 0, 1, 2, 3, 6, 4));

		simpleDateMatcher.put("þþ 999 þþ", new SimpleDateMatcher("þþ 999 þþ", "dd MMM yy", 0, 2, 3, 3, 7, 2));
		simpleDateMatcher.put("þ 999 þþ", new SimpleDateMatcher("þ 999 þþ", "d MMM yy", 0, 1, 2, 3, 6, 2));
		simpleDateMatcher.put("þþ-999-þþ", new SimpleDateMatcher("þþ-999-þþ", "dd-MMM-yy", 0, 2, 3, 3, 7, 2));
		simpleDateMatcher.put("þ-999-þþ", new SimpleDateMatcher("þ-999-þþ", "d-MMM-yy", 0, 1, 2, 3, 6, 2));

		simpleDateMatcher.put("999 þþ, þþþþ", new SimpleDateMatcher("999 þþ, þþþþ", "MMM dd',' yyyy", 4, 2, 0, 3, 8, 4));
		simpleDateMatcher.put("999 þ, þþþþ", new SimpleDateMatcher("999 þ, þþþþ", "MMM d',' yyyy", 4, 1, 0, 3, 7, 4));
		simpleDateMatcher.put("999 þþ þþþþ", new SimpleDateMatcher("999 þ þþþþ", "MMM dd yyyy", 4, 2, 0, 3, 7, 4));
		simpleDateMatcher.put("999 þ þþþþ", new SimpleDateMatcher("999 þ þþþþ", "MMM d yyyy", 4, 1, 0, 3, 6, 4));
		simpleDateMatcher.put("999-þþ-þþþþ", new SimpleDateMatcher("999-þþ-þþþþ", "MMM-dd-yyyy", 4, 2, 0, 3, 7, 4));
		simpleDateMatcher.put("999-þ-þþþþ", new SimpleDateMatcher("999-þ-þþþþ", "MMM-d-yyyy", 4, 1, 0, 3, 6, 4));
	}

	static int monthAbbreviationOffset(String month) {
		Integer offset = monthAbbr.get(month.toUpperCase(Locale.ROOT));
		return offset == null ? -1 : offset;
	}

	Map<String, Integer> results = new HashMap<String, Integer>();
	int sampleCount = 0;
	int nullCount = 0;
	int blankCount = 0;
	int invalidCount = 0;

	DateTimeParser(Boolean dayFirst) {
		this.dayFirst = dayFirst;
	}

	DateTimeParser() {
		this(null);
	}

	/**
	 * Train is the core entry point used to supply input to the DateTimeParser.
	 * @param input The String representing a date with possible surrounding whitespace.
	 * @return A String representing the DateTime detected (Using DateTimeFormatter Patterns) or null if no match.
	 */
	public String train(String input) {
		sampleCount++;

		if (input == null) {
			nullCount++;
			return null;
		}

		String trimmed = input.trim();
		if (input.length() == 0) {
			blankCount++;
			return null;
		}

		String ret = determineFormatString(trimmed, null);
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

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		return map.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	/**
	 * Determine the result of the training complete to date. Typically invoked
	 * after all training is complete, but may be invoked at any stage.
	 * @return A DateTimeParserResult with the analysis of any training completed.
	 */
	public DateTimeParserResult getResult() {
		// If we have no good samples, call it a day
		if (sampleCount == nullCount + blankCount + invalidCount)
			return null;

		// If there is only one result then it must be correct :-)
		if (results.size() == 1)
			return DateTimeParserResult.asResult(results.keySet().iterator().next(), dayFirst);

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

		// Sort the results of our training by value so that we consider the most frequent first
		Map<String, Integer> byValue = sortByValue(results);

		// Iterate through all the results of our training, merging them to produce our best guess
		for (Map.Entry<String, Integer> entry : byValue.entrySet()) {
			DateTimeParserResult result = DateTimeParserResult.asResult(entry.getKey(), dayFirst);
			if (result == null) {
				System.err.println("NOT FOUND - input: '" + entry.getKey() + "'");
				continue;
			}
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
			}
			if (monthOffset == -1 && result.monthOffset != -1) {
				monthOffset = result.monthOffset;
			}
			if (yearOffset == -1 && result.yearOffset != -1) {
				yearOffset = result.yearOffset;
			}
			if (dateSeparator == null)
				dateSeparator = result.dateSeparator;
			if (dateTimeSeparator == null)
				dateTimeSeparator = result.dateTimeSeparator;
			if (timeZone == null)
				timeZone = result.timeZone;
		}

		if (timeZone == null)
			timeZone = "";

		return new DateTimeParserResult(null, dayFirst, timeElements, hourLength, dateElements, dateFieldLengths,
				timeFirst, dateTimeSeparator, yearOffset, monthOffset, dayOffset, dateSeparator, timeZone);
	}

	static String retDigits(int digitCount, char patternChar) {
		String ret = String.valueOf(patternChar);
		if (digitCount == 1)
			return ret;
		if (digitCount == 2)
			return ret + patternChar;
		return ret + patternChar + patternChar + patternChar;
	}

	/**
	 * Give a String as input with an offset and length return the integer at that position.
	 * @param input String to extract integer from
	 * @param offset Integer offset that marks the start
	 * @param length Integer length of integer to be extracted.
	 * @return An integer value from the supplied String.
	 */
	static int getValue(String input, int offset, int length) {
		return Integer.valueOf(input.substring(offset, offset + length));
	}

	static boolean plausibleDate(int[] dateValues, int[] dateDigits, int[] fieldOffsets) {
		int monthDays[] = {-1, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 30};
		int year = dateValues[fieldOffsets[2]];
		if (year == 0 && dateDigits[fieldOffsets[2]] == 4)
			return false;
		int month = dateValues[fieldOffsets[1]];
		if (month == 0 || month > 12)
			return false;
		int day = dateValues[fieldOffsets[0]];
		if (day == 0 || day > monthDays[month])
			return false;
		return true;
	}

	private static String dateFormat(int[] dateDigits, char dateSeparator, Boolean dayFirst, boolean yearKnown) {
		if (dayFirst == null)
			return retDigits(dateDigits[0], '?') + dateSeparator + retDigits(dateDigits[1], '?') + dateSeparator + retDigits(dateDigits[2], yearKnown ? 'y' : '?');

		if (dayFirst)
			return retDigits(dateDigits[0], 'd') + dateSeparator + retDigits(dateDigits[1], 'M') + dateSeparator + retDigits(dateDigits[2], 'y');

		return retDigits(dateDigits[0], 'M') + dateSeparator + retDigits(dateDigits[1], 'd') + dateSeparator + retDigits(dateDigits[2], 'y');
	}


	/**
	 * Determine a FormatString from an input string that may represent a Date, Time,
	 * DateTime, OffsetDateTime or a ZonedDateTime.
	 * @param input The String representing a date with optional leading/trailing whitespace
	 * @param dayFirst TODO
	 * @return A String representing the DateTime detected (Using DateTimeFormatter Patterns) or null if no match.
	 */
	public static String determineFormatString(String input, Boolean dayFirst) {
		int len = input.length();

		// Remove leading spaces
		int start = 0;
		while (start < len && input.charAt(start) == ' ')
			start++;

		// Remove trailing spaces
		while (len >= 1 && input.charAt(len - 1) == ' ')
			len--;

		len -= start;

		String trimmed = input.substring(start, len + start);

		// Fail fast if we can
		if (len < 4 || (!Character.isDigit(trimmed.charAt(0)) && !Character.isAlphabetic(trimmed.charAt(0))))
			return null;

		// Cope with simple dates of the form '21 May 2017' or '9-Sep-2018'
		if (trimmed.indexOf('þ') != -1)
			return null;
		String templated = trimmed.replaceAll("[0-9]", "þ");
		templated = templated.replaceAll("[a-zA-Z]", "9");

		SimpleDateMatcher matcher = simpleDateMatcher.get(templated);
		if (matcher != null) {
			int[] dateValue = new int[] {-1, -1, -1};
			if (matcher.monthLength == 3) {
				dateValue[1] = DateTimeParser.monthAbbreviationOffset(trimmed.substring(matcher.monthOffset, matcher.monthOffset + matcher.monthLength));
				if (dateValue[1] == -1)
					return null;
			}
			else {
				dateValue[1] = getValue(trimmed, matcher.monthOffset, matcher.monthLength);
			}
			dateValue[0] = getValue(trimmed, matcher.dayOffset, matcher.dayLength);
			dateValue[2] = getValue(trimmed, matcher.yearOffset, matcher.yearLength);

			if (!plausibleDate(dateValue, new int[] {matcher.dayLength, matcher.monthLength, matcher.yearLength}, new int[] {0,1,2}))
				return null;

			return matcher.format;
		}

		// Fail fast if we can
		if (!templated.contains("þ:þþ") && !templated.contains("þ/þþ") && !templated.contains("þ-þþ"))
			return null;

		// Fail fast if we can
		if (!Character.isDigit(trimmed.charAt(0)))
			return null;

		int digits = 0;
		int value = 0;
		int[] dateValue = new int[3];
		int[] timeValue = new int[3];
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
		boolean expectingAlphaTimeZone = false;

		int lastCh = 'þ';
		for (int i = 0; i < len && timeZone.length() == 0; i++) {
			char ch = trimmed.charAt(i);

			// Two spaces in a row is always bad news
			if (lastCh == ' ' && ch == ' ')
				return null;
			lastCh = ch;

			if (expectingAlphaTimeZone) {
				String currentTimeZone = trimmed.substring(i, len);
				if (!DateTimeParser.timeZones.contains(currentTimeZone))
					return null;
				timeZone = " z";
				continue;
			}

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
				if (dateSeen && !dateClosed || timeSeen && timeClosed)
					return null;

				timeFirst = dateComponent == 0;
				timeSeen = true;
				timeValue[timeComponent] = value;

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
				if (!ISO8601)
					return null;

				// FALL THROUGH

			case '-':
				if (ISO8601 || (dateSeen && dateClosed && timeSeen && timeComponent == 2)) {
					int hours = Integer.MIN_VALUE;
					int minutesOffset = Integer.MIN_VALUE;
					int secondsOffset = Integer.MIN_VALUE;

					i++;

					String offset = templated.substring(i, len);

					// Expecting DD:DD:DD or DDDDDD or DD:DD or DDDD or DD
					if (i + 8 <= len && "þþ:þþ:þþ".equals(offset)) {
						timeZone = "xxxxx";
						minutesOffset = 3;
						secondsOffset = 6;
					}
					else if (i + 6 <= len && "þþþþþþ".equals(offset)) {
						timeZone = "xxxx";
						minutesOffset = 2;
						secondsOffset = 4;
					}
					else if (i + 5 <= len && "þþ:þþ".equals(offset)) {
						timeZone = "xxx";
						minutesOffset = 3;
					}
					else if (i + 4 <= len && "þþþþ".equals(offset)) {
						timeZone = "xx";
					}
					else if (i + 2 <= len && "þþ".equals(offset)) {
						timeZone = "x";
					}
					else
						return null;

					// Validate the hours
					hours = getValue(trimmed, i, 2);
					if (hours != Integer.MIN_VALUE && hours > 18)
						return null;

					// Validate the minutes
					if (minutesOffset != Integer.MIN_VALUE) {
						int minutes = getValue(trimmed, i + minutesOffset, 2);
						if (minutes != Integer.MIN_VALUE && minutes > 59)
							return null;
					}

					// Validate the seconds
					if (secondsOffset != Integer.MIN_VALUE) {
						int seconds = getValue(trimmed, i + secondsOffset, 2);
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

			case 'T':
				// ISO 8601
				if (timeSeen)
					return null;
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
				if (!dateSeen && !timeSeen)
					return null;
				if (timeSeen && !timeClosed) {
					if (digits != 2)
						return null;
					timeValue[timeComponent] = value;
					timeClosed = true;
				}
				else if (dateSeen && !dateClosed) {
					if (dateComponent != 2)
						return null;
					if (!((digits == 2) || (yearInDateFirst == false && digits == 4)))
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
				if (timeSeen && dateSeen)
					expectingAlphaTimeZone = true;
				break;

			default:
				return null;
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
			dateValue[dateComponent] = value;
			dateDigits[dateComponent] = digits;
			digits = 0;
		}
		if (timeSeen && !timeClosed) {
			// Need to close out the time
			if (digits != 2)
				return null;
			timeValue[timeComponent] = value;
			digits = 0;
		}

		if (digits != 0)
			return null;

		if (ISO8601 && timeComponent == 0)
			return null;

		String timeAnswer = null;
		if (timeComponent != 0) {
			if (timeValue[0] > 23 || timeValue[1] > 59 || (timeComponent == 2 && timeValue[2] > 59))
				return null;
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
			if (dateValue[1] == 0 || dateValue[1] > 31)
				return null;
			if (yearInDateFirst) {
				if (!plausibleDate(dateValue, dateDigits, new int[] {2,1,0}))
					return null;
				dateAnswer = retDigits(dateDigits[0], 'y') + dateSeparator + retDigits(dateDigits[1], 'M') + dateSeparator + retDigits(dateDigits[2], 'd');
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
						dateAnswer = dateFormat(dateDigits, dateSeparator, dayFirst, true);
				} else {
					// If the first group of digits is of length 1, then it is either d/MM/yy or M/dd/yy
					if (dateDigits[0] == 1) {
						if (dateValue[0] == 0)
							return null;
						if (dateValue[1] > 12) {
							if (!plausibleDate(dateValue, dateDigits, new int[] {1,0,2}))
								return null;
							dateAnswer = retDigits(dateDigits[0], 'M') + dateSeparator + "dd" + dateSeparator + "yy";
						}
						else
							dateAnswer = dateFormat(dateDigits, dateSeparator, dayFirst, true);
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
							dateAnswer = dateFormat(dateDigits, dateSeparator, dayFirst, true);
					} else if (dateValue[1] > 12) {
						if (!plausibleDate(dateValue, dateDigits, new int[] {1,0,2}))
							return null;
						dateAnswer = retDigits(dateDigits[0], 'M') + dateSeparator + "dd" + dateSeparator + "yy";
					}
					else
						dateAnswer = dateFormat(dateDigits, dateSeparator, dayFirst, false);
				}
			}
			if (timeComponent == 0)
				return dateAnswer + timeZone;
		}

		if (timeFirst)
			return timeAnswer + (ISO8601 ? "'T'" : " ") + dateAnswer + timeZone;
		else
			return dateAnswer + (ISO8601 ? "'T'" : " ") + timeAnswer + timeZone;
	}
}