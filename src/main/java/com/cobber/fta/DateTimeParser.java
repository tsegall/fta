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
		None, DayFirst, MonthFirst
	}

	private DateResolutionMode resolutionMode = DateResolutionMode.None;
	private Locale locale;

	public static Set<String> timeZones = new HashSet<String>();


	static {
		// Cache the set of available Time Zones
		Collections.addAll(timeZones, TimeZone.getAvailableIDs());
		// Add the non-real Time Zones (that people use)
		timeZones.addAll(Arrays.asList(new String[] {"EDT", "CDT", "MDT", "PDT"}));

	}

	public static int shortMonthOffset(final String month, Locale locale) {
		final Integer offset = LocaleInfo.getShortMonths(locale).get(month.toUpperCase(locale));
		return offset == null ? -1 : offset;
	}

	public static int monthOffset(final String month, Locale locale) {
		final Integer offset = LocaleInfo.getMonths(locale).get(month.toUpperCase(locale));
		return offset == null ? -1 : offset;
	}

	private final Map<String, Integer> results = new HashMap<String, Integer>();
	private int sampleCount;
	private int nullCount;
	private int blankCount;
	private int invalidCount;

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
		final String ret = determineFormatString(trimmed, DateResolutionMode.None, locale);
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

		// If there is only one result then it must be correct :-)
		if (results.size() == 1)
			return DateTimeParserResult.asResult(results.keySet().iterator().next(), resolutionMode, locale);

		DateTimeParserResult answerResult = null;

		// Sort the results of our training by value so that we consider the most frequent first
		final HashMap<String, Integer> byValue = sortByValue(results);

		// Iterate through all the results of our training, merging them to produce our best guess
		StringBuffer answerBuffer = null;
		for (final Map.Entry<String, Integer> entry : byValue.entrySet()) {
			final String key = entry.getKey();
			final DateTimeParserResult result = DateTimeParserResult.asResult(key, resolutionMode, locale);

			// First entry
			if (answerBuffer == null) {
				answerBuffer = new StringBuffer(key);
				answerResult = result;
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

				for (int i = 0; i < result.dateFieldLengths.length; i++) {
					if (answerResult.dateFieldLengths[i] == -1 && result.dateFieldLengths[i] != -1)
						answerResult.dateFieldLengths[i] = result.dateFieldLengths[i];
					else if (answerResult.dateFieldLengths[i] != result.dateFieldLengths[i] && (result.dateFieldLengths[i] == 1 || result.dateFieldLengths[i] == 4)) {
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

		// If we are supposed to be fully bound and still have some ambiguities then fix them based on the mode
		if (answerResult.dateElements != -1 && resolutionMode != DateResolutionMode.None && (answerResult.dayOffset == -1 || answerResult.monthOffset == -1 || answerResult.yearOffset == -1)) {
			if (answerResult.monthOffset != -1) {
				int start = answerResult.dateFieldOffsets[0];
				answerBuffer.replace(start, start + answerResult.dateFieldLengths[0], longString('d').substring(0, answerResult.dateFieldLengths[0]));
				start = answerResult.dateFieldOffsets[2];
				answerBuffer.replace(start, start + answerResult.dateFieldLengths[2], longString('y').substring(0, answerResult.dateFieldLengths[2]));
			}
			else {
					final char firstField = resolutionMode == DateResolutionMode.DayFirst ? 'd' : 'M';
					final char secondField = resolutionMode == DateResolutionMode.DayFirst ? 'M' : 'd';
					int start = answerResult.dateFieldOffsets[0];
					answerBuffer.replace(start, start + answerResult.dateFieldLengths[0], longString(firstField).substring(0, answerResult.dateFieldLengths[0]));
					start = answerResult.dateFieldOffsets[1];
					answerBuffer.replace(start, start + answerResult.dateFieldLengths[1], longString(secondField).substring(0, answerResult.dateFieldLengths[1]));
			}
		}

		if (answerResult.timeZone == null)
			answerResult.timeZone = "";

		answerResult.formatString = answerBuffer.toString();
		return DateTimeParserResult.newInstance(answerResult);
	}

	private static String retDigits(final int digitCount, final char patternChar) {
		final String ret = String.valueOf(patternChar);
		if (digitCount == 1)
			return ret;
		if (digitCount == 2)
			return ret + patternChar;
		return ret + patternChar + patternChar + patternChar;
	}

	private static boolean plausibleDate(final int[] dateValues, final int[] dateDigits, final int[] fieldOffsets) {
		final int year = dateValues[fieldOffsets[2]];
		if (year == 0 && dateDigits[fieldOffsets[2]] == 4)
			return false;
		final int month = dateValues[fieldOffsets[1]];
		if (month == 0 || month > 12)
			return false;
		final int day = dateValues[fieldOffsets[0]];
		final int monthDays[] = {-1, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
		if (day == 0 || day > monthDays[month])
			return false;
		return true;
	}

	private static String dateFormat(final int[] dateDigits, final char dateSeparator, final DateResolutionMode resolutionMode, final boolean yearKnown) {
		switch (resolutionMode) {
		case None:
			return retDigits(dateDigits[0], '?') + dateSeparator + retDigits(dateDigits[1], '?') + dateSeparator + retDigits(dateDigits[2], yearKnown ? 'y' : '?');
		case DayFirst:
			return retDigits(dateDigits[0], 'd') + dateSeparator + retDigits(dateDigits[1], 'M') + dateSeparator + retDigits(dateDigits[2], 'y');
		case MonthFirst:
			return retDigits(dateDigits[0], 'M') + dateSeparator + retDigits(dateDigits[1], 'd') + dateSeparator + retDigits(dateDigits[2], 'y');
		}

		return null;
	}

	/**
	 * Determine a FormatString from an input string that may represent a Date, Time,
	 * DateTime, OffsetDateTime or a ZonedDateTime.
	 * @param input The String representing a date with optional leading/trailing whitespace
	 * @param resolutionMode When we have ambiguity - should we prefer to conclude day first, month first or unspecified
	 * @param locale The Locale used to interpret the input
	 * @return A String representing the DateTime detected (Using DateTimeFormatter Patterns) or null if no match.
	 */
	public static String determineFormatString(final String input, final DateResolutionMode resolutionMode, Locale locale) {
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
		if (matcher.isKnown()) {
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

		// Fail fast if we can
		if (!matcher.getCompressed().contains(":d{") && !matcher.getCompressed().contains("/d{") && !matcher.getCompressed().contains("-d{"))
			return null;

		// Fail fast if we can
		if (!Character.isDigit(trimmed.charAt(0)))
			return null;

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
		boolean expectingAlphaTimeZone = false;

		int lastCh = '¶';
		for (int i = 0; i < len && timeZone.length() == 0; i++) {
			final char ch = trimmed.charAt(i);

			// Two spaces in a row is always bad news
			if (lastCh == ' ' && ch == ' ')
				return null;
			lastCh = ch;

			if (expectingAlphaTimeZone) {
				final String currentTimeZone = trimmed.substring(i, len);
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
				if (!iso8601)
					return null;

				// FALL THROUGH

			case '-':
				if (iso8601 || (dateSeen && dateClosed && timeSeen && timeComponent == 2)) {
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
				if ((dateSeen && !dateClosed) || (timeSeen && timeClosed) || timeComponent != 2 || digits != 2)
					return null;
				timeValue[timeComponent] = value;
				timeDigits[timeComponent] = digits;
				timeComponent++;
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
				if (timeSeen && dateSeen)
					expectingAlphaTimeZone = true;
				break;

			default:
				if (!Character.isAlphabetic(ch))
					return null;

				String rest = input.substring(i).toUpperCase(locale);
				boolean ampmDetected = false;
				for (String s : LocaleInfo.getAMPMStrings(locale)) {
					if (rest.startsWith(s)) {
						amPmIndicator = trimmed.charAt(i - 1) == ' ' ? " a" : "a";
						i += s.length();
						ampmDetected = true;
						break;
					}
				}

				if (ampmDetected) {
					// Eat the space after if it exists
					if (i + 1 < len && trimmed.charAt(i + 1) == ' ')
						i++;
				}
				else if (ch == 'T') {
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
				if (timeValue[0] > 23)
					return null;
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
						dateAnswer = dateFormat(dateDigits, dateSeparator, resolutionMode, true);
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
							dateAnswer = dateFormat(dateDigits, dateSeparator, resolutionMode, true);
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
							dateAnswer = dateFormat(dateDigits, dateSeparator, resolutionMode, true);
					} else if (dateValue[1] > 12) {
						if (!plausibleDate(dateValue, dateDigits, new int[] {1,0,2}))
							return null;
						dateAnswer = retDigits(dateDigits[0], 'M') + dateSeparator + "dd" + dateSeparator + "yy";
					} else if (dateValue[0] > 12 && dateValue[2] > 12) {
						dateAnswer = "??" + dateSeparator + retDigits(dateDigits[1], 'M') + dateSeparator + "??";
					} else
						dateAnswer = dateFormat(dateDigits, dateSeparator, resolutionMode, false);
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
}
