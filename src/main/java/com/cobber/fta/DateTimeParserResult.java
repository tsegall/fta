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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.cobber.fta.DateTimeParser.DateResolutionMode;

/**
 * DateTimeParserResult is the result of a {@link DateTimeParser} analysis.
 */
public class DateTimeParserResult {
	public int timeElements = -1;
	public int dateElements = -1;
	public Boolean timeFirst;
	public Character dateTimeSeparator;
	public int yearOffset = -1;
	public int monthOffset = -1;
	public int dayOffset = -1;
	public int[] dateFieldLengths = new int[] {-1, -1, -1};
	public int[] dateFieldOffsets = new int[] {-1, -1, -1};
	public int[] timeFieldLengths = new int[] {-1, -1, -1, -1};
	public int[] timeFieldOffsets = new int[] {-1, -1, -1, -1};
	public String timeZone = "";
	public Character dateSeparator;
	public String formatString;
	public Boolean amPmIndicator;

	private DateResolutionMode resolutionMode = DateResolutionMode.None;
	private Locale locale;
	private int hourLength = -1;

	static Map<String, DateTimeParserResult> dtpCache = new ConcurrentHashMap<String, DateTimeParserResult>();

	DateTimeParserResult(final String formatString, final DateResolutionMode resolutionMode, Locale locale, final int timeElements,
			final int[] timeFieldLengths, final int[] timeFieldOffsets, final int hourLength, final int dateElements, final int[] dateFieldLengths,
			final int[] dateFieldOffsets, final Boolean timeFirst, final Character dateTimeSeparator, final int yearOffset, final	int monthOffset,
			final int dayOffset, final Character dateSeparator, final String timeZone, final Boolean amPmIndicator) {
		this.formatString = formatString;
		this.resolutionMode = resolutionMode;
		this.locale = locale;
		this.timeElements = timeElements;
		this.timeFieldLengths = timeFieldLengths;
		this.timeFieldOffsets = timeFieldOffsets;
		this.hourLength = hourLength;
		this.dateElements = dateElements;
		this.dateFieldLengths = dateFieldLengths;
		this.dateFieldOffsets = dateFieldOffsets;
		this.timeFirst = timeFirst;
		this.dateTimeSeparator = dateTimeSeparator;
		this.dayOffset = dayOffset;
		this.monthOffset = monthOffset;
		this.yearOffset = yearOffset;
		this.dateSeparator = dateSeparator;
		this.timeZone = timeZone;
		this.amPmIndicator = amPmIndicator;
	}

	public static DateTimeParserResult newInstance(final DateTimeParserResult r) {
		return new DateTimeParserResult(r.formatString, r.resolutionMode, r.locale,
				r.timeElements,
				r.timeFieldLengths != null ? Arrays.copyOf(r.timeFieldLengths, r.timeFieldLengths.length) : null,
				r.timeFieldOffsets != null ? Arrays.copyOf(r.timeFieldOffsets, r.timeFieldOffsets.length) : null, r.hourLength,
				r.dateElements,
				r.dateFieldLengths != null ? Arrays.copyOf(r.dateFieldLengths, r.dateFieldLengths.length) : null,
				r.dateFieldOffsets != null ? Arrays.copyOf(r.dateFieldOffsets, r.dateFieldOffsets.length) : null, r.timeFirst, r.dateTimeSeparator, r.yearOffset, r.monthOffset, r.dayOffset, r.dateSeparator, r.timeZone, r.amPmIndicator);
	}

	enum Token {
		CONSTANT_CHAR, DAYS_1_OR_2, DAYS_2, DAY_OF_WEEK, DAY_OF_WEEK_ABBR, DIGITS_1_OR_2, MONTHS_1_OR_2,
		MONTHS_2, HOURS12_1_OR_2, HOURS12_2, HOURS24_1_OR_2, HOURS24_2, MINS_2, SECS_2, FRACTION,
		DIGITS_2, YEARS_2, YEARS_4, MONTH, MONTH_ABBR, TIMEZONE, TIMEZONE_OFFSET, AMPM
	}

	/**
	 * Determine whether a string input matches this DateTimeParserResult.
	 * @param input The string to validate (stripped of whitespace.
	 * @return A boolean indicating if the input is valid.
	 */
	public boolean isValid8(final String input) {
		final DateTimeFormatter formatter = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(getFormatString()).toFormatter(locale);

		try {
			if (PatternInfo.Type.LOCALTIME.equals(getType()))
				LocalTime.parse(input, formatter);
			else if (PatternInfo.Type.LOCALDATE.equals(getType()))
				LocalDate.parse(input, formatter);
			else if (PatternInfo.Type.LOCALDATETIME.equals(getType()))
				LocalDateTime.parse(input, formatter);
			else if (PatternInfo.Type.ZONEDDATETIME.equals(getType()))
				ZonedDateTime.parse(input, formatter);
			else
				OffsetDateTime.parse(input, formatter);
			return true;
		}
		catch (DateTimeParseException exc) {
			return false;
		}
	}

	/**
	 * Determine whether a string input matches this DateTimeParserResult.
	 * @param input The string to validate (stripped of whitespace.
	 * @return A boolean indicating if the input is valid.
	 */
	public boolean isValid(final String input) {
		try {
			parse(input);
			return true;
		}
		catch (DateTimeParseException exc) {
			return false;
		}
	}

	/**
	 * Given an input string in SimpleDateTimeFormat convert to a DateTimeParserResult
	 * @param formatString A DateTimeString using DateTimeFormatter patterns
	 * @param resolutionMode 	When we have ambiguity - should we prefer to conclude day first, month first or unspecified
	 * @param locale Locale the input string is in
	 * @return The corresponding DateTimeParserResult
	 */
	public static DateTimeParserResult asResult(final String formatString, final DateResolutionMode resolutionMode, Locale locale) {
		final String key = resolutionMode.name() + '#' + locale + '#' + formatString;
		DateTimeParserResult ret = dtpCache.get(key);
		if (ret != null)
			return newInstance(ret);

		int dayOffset = -1;
		int dayLength = -1;
		int monthOffset = -1;
		int monthLength = -1;
		int yearOffset = -1;
		int yearLength = -1;
		int hourLength = -1;
		int dateElements = 0;
		int timeElements = 0;
		int[] dateFieldLengths = new int[] {-1, -1, -1};
		int[] dateFieldOffsets = new int[] {-1, -1, -1};
		int[] timeFieldLengths = new int[] {-1, -1, -1, -1};
		int[] timeFieldOffsets = new int[] {-1, -1, -1, -1};
		String timeZone = "";
		Boolean timeFirst = null;
		Character dateSeparator = null;
		Character dateTimeSeparator = ' ';
		Boolean fullyBound = true;
		Boolean amPmIndicator = null;

		final int formatLength = formatString.length();

		for (int i = 0; i < formatLength; i++) {
			final char ch = formatString.charAt(i);
			switch (ch) {
			case '?':
				dateFieldOffsets[dateElements] = i;
				++dateElements;
				if (i + 1 < formatLength && formatString.charAt(i + 1) == '?') {
					i++;
					dateFieldLengths[dateElements - 1] = 2;
				}
				else {
					dateFieldLengths[dateElements - 1] = 1;
				}
				if (dateElements == 1)
					dateSeparator = formatString.charAt(i + 1);
				fullyBound = false;
				break;

			case 'M':
				dateFieldOffsets[dateElements] = i;
				monthOffset = dateElements++;
				if (i + 1 < formatLength && formatString.charAt(i + 1) == 'M') {
					i++;
					if (i + 1 < formatLength && formatString.charAt(i + 1) == 'M') {
						i++;
						if (i + 1 < formatLength && formatString.charAt(i + 1) == 'M') {
							i++;
							monthLength = 4;
						}
						else
							monthLength = 3;
					}
					else
						monthLength = 2;
				} else
					monthLength = 1;
				dateFieldLengths[dateElements - 1] = monthLength;

				if (dateElements == 1)
					dateSeparator = formatString.charAt(i + 1);
				break;

			case 'd':
				dateFieldOffsets[dateElements] = i;
				dayOffset = dateElements++;
				if (i + 1 < formatLength && formatString.charAt(i + 1) == 'd') {
					i++;
					dayLength = 2;
				}
				else
					dayLength = 1;
				dateFieldLengths[dateElements - 1] = dayLength;
				if (dateElements == 1)
					dateSeparator = formatString.charAt(i + 1);
				break;

			case 'h':
			case 'H':
				timeFieldOffsets[timeElements] = i;
				timeFirst = dateElements == 0;
				timeElements++;
				if (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					i++;
					hourLength = 2;
				}
				else
					hourLength = 1;
				timeFieldLengths[timeElements - 1] = hourLength;
				break;

			case 'm':
			case 's':
				timeFieldOffsets[timeElements] = i;
				timeFieldLengths[timeElements] = 2;
				timeElements++;
				if (i + 1 >= formatLength || formatString.charAt(i + 1) != ch)
					return null;
				i++;
				break;

			case 'S':
				timeFieldOffsets[timeElements] = i;
				int fractions = 1;
				while (i + 1 < formatLength && formatString.charAt(i + 1) == 'S') {
					i++;
					fractions++;
				}
				timeFieldLengths[timeElements] = fractions;
				timeElements++;
				break;

			case 'y':
				dateFieldOffsets[dateElements] = i;
				yearOffset = dateElements++;
				i++;
				if (i + 1 < formatLength && formatString.charAt(i + 1) == 'y') {
					yearLength = 4;
					i += 2;
				} else
					yearLength = 2;
				dateFieldLengths[dateElements - 1] = yearLength;
				if (dateElements == 1)
					dateSeparator = formatString.charAt(i + 1);
				break;

			case 'x':
				timeZone = "x";
				while (i + 1 < formatLength && formatString.charAt(i + 1) == 'x') {
					timeZone += "x";
					i++;
				}
				break;

			case 'z':
				timeZone = " z";
				break;

			case 'T':
				dateTimeSeparator = 'T';
				break;

			case 'a':
				amPmIndicator = true;
				break;

			default:
				// FIX ME
			}
		}

		if (dateElements == 0)
			dateElements = -1;
		if (timeElements == 0)
			timeElements = -1;

		// Add to cache
		ret = new DateTimeParserResult(fullyBound ? formatString : null, resolutionMode, locale, timeElements, timeFieldLengths, timeFieldOffsets, hourLength, dateElements,
				dateFieldLengths, dateFieldOffsets, timeFirst, dateTimeSeparator, yearOffset, monthOffset, dayOffset, dateSeparator, timeZone, amPmIndicator);
		dtpCache.put(key, ret);

		return newInstance(ret);
	}

	public static FormatterToken[] tokenize(String formatString) {
		final List<FormatterToken> ret = new ArrayList<FormatterToken>();
		int upto = 0;

		final int formatLength = formatString.length();

		for (int i = 0; i < formatLength; i++) {
			final char ch = formatString.charAt(i);
			switch (ch) {
			case 'E':
				if (i + 1 < formatLength && formatString.charAt(i + 1) == 'E') {
					i++;
					if (i + 1 < formatLength && formatString.charAt(i + 1) == 'E') {
						i++;
						if (i + 1 < formatLength && formatString.charAt(i + 1) == 'E') {
							i++;
							ret.add(new FormatterToken(Token.DAY_OF_WEEK));
						}
						else
							ret.add(new FormatterToken(Token.DAY_OF_WEEK_ABBR));
					}
				}
				break;

			case 'M':
				if (i + 1 < formatLength && formatString.charAt(i + 1) == 'M') {
					i++;
					if (i + 1 < formatLength && formatString.charAt(i + 1) == 'M') {
						i++;
						if (i + 1 < formatLength && formatString.charAt(i + 1) == 'M') {
							i++;
							ret.add(new FormatterToken(Token.MONTH));
						}
						else
							ret.add(new FormatterToken(Token.MONTH_ABBR));
					}
					else
						ret.add(new FormatterToken(Token.MONTHS_2));
				} else
					ret.add(new FormatterToken(Token.MONTHS_1_OR_2));
				break;

			case 'd':
				if (i + 1 < formatLength && formatString.charAt(i + 1) == 'd') {
					i++;
					ret.add(new FormatterToken(Token.DAYS_2));
				}
				else
					ret.add(new FormatterToken(Token.DAYS_1_OR_2));

				break;

			case 'h':
				if (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					i++;
					ret.add(new FormatterToken(Token.HOURS12_2));
				}
				else
					ret.add(new FormatterToken(Token.HOURS12_1_OR_2));
				break;

			case 'H':
				if (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					i++;
					ret.add(new FormatterToken(Token.HOURS24_2));
				}
				else
					ret.add(new FormatterToken(Token.HOURS24_1_OR_2));
				break;

			case '?':
				if (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					i++;
					ret.add(new FormatterToken(Token.DIGITS_2));
				}
				else
					ret.add(new FormatterToken(Token.DIGITS_1_OR_2));
				break;

			case 'm':
			case 's':
				ret.add(new FormatterToken(ch == 'm' ? Token.MINS_2 : Token.SECS_2));
				i++;
				break;

			case 'y':
				i++;
				if (i + 1 < formatLength && formatString.charAt(i + 1) == 'y') {
					ret.add(new FormatterToken(Token.YEARS_4));
					i += 2;
				} else
					ret.add(new FormatterToken(Token.YEARS_2));
				break;

			case 'S':
			case 'x':
				int nextCount = 1;
				while (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					nextCount++;
					i++;
				}
				ret.add(new FormatterToken(ch == 'x' ? Token.TIMEZONE_OFFSET : Token.FRACTION, nextCount));
				break;

			case 'z':
				ret.add(new FormatterToken(Token.TIMEZONE));
				break;

			case '\'':
				i++;
				ret.add(new FormatterToken(Token.CONSTANT_CHAR, formatString.charAt(i)));
				if (i + 1 >= formatLength || formatString.charAt(i + 1) != '\'') {
					throw new DateTimeParseException("Unterminated quote in format String", formatString, upto);
				}
				i++;
				break;

			case 'a':
				ret.add(new FormatterToken(Token.AMPM));
				break;

			default:
				ret.add(new FormatterToken(Token.CONSTANT_CHAR, ch));
			}
		}

		return ret.toArray(new FormatterToken[ret.size()]);
	}

	@SuppressWarnings("incomplete-switch")
	void validateTokenValue(final Token token, final int value, final String input, final int upto) {
		switch (token) {
		case HOURS12_2:
			if (value == 0 || value > 12)
				throw new DateTimeParseException("Invalid value for hours (expected 1-12)", input, upto);
			break;

		case HOURS24_2:
			if (value > 24)
				throw new DateTimeParseException("Invalid value for hours (expected 0-23)", input, upto);
			break;

		case MINS_2:
			if (value > 59)
				throw new DateTimeParseException("Invalid value for minutes (expected 0-59)", input, upto);
			break;

		case SECS_2:
			if (value > 59)
				throw new DateTimeParseException("Invalid value for seconds (expected 0-59)", input, upto);
			break;
		}
	}

	/**
	 * Determine whether a string input matches this DateTimeParserResult.
	 * @param input The string to validate (stripped of whitespace).
	 * if non-zero then this is the offset where the parse failed
	 */
	public void parse(final String input) throws DateTimeParseException {
		final int inputLength = input.length();
		int upto = 0;

		if (formatString == null)
			formatString = getFormatString();

		for (final FormatterToken token : tokenize(formatString)) {
			final Token nextToken = token.getType();

			char inputChar;
			int value = 0;
			int start;

			switch (nextToken) {
			case DAY_OF_WEEK:
			case DAY_OF_WEEK_ABBR:
				start = upto;
				while (upto < inputLength && Character.isAlphabetic(input.charAt(upto)))
					upto++;
				break;

			case MONTH:
				start = upto;
				while (upto < inputLength && Character.isAlphabetic(input.charAt(upto)))
					upto++;
				final String month = input.substring(start, upto);
				if (DateTimeParser.monthOffset(month, locale) == -1)
					throw new DateTimeParseException("Month invalid", input, start);
				break;

			case MONTH_ABBR:
				if (upto + 3 > inputLength)
					throw new DateTimeParseException("Month Abbreviation not complete", input, upto);
				final String monthAbbreviation = input.substring(upto, upto + 3);
				if (DateTimeParser.shortMonthOffset(monthAbbreviation, locale) == -1)
					throw new DateTimeParseException("Month Abbreviation invalid", input, upto);
				upto += 3;
				break;

			case HOURS12_1_OR_2:
			case HOURS24_1_OR_2:
			case DIGITS_1_OR_2:
				if (upto == inputLength)
					throw new DateTimeParseException("Expecting digit, end of input", input, upto);
				if (!Character.isDigit(input.charAt(upto)))
					throw new DateTimeParseException("Expecting digit", input, upto);
				upto++;
				if (upto != inputLength && Character.isDigit(input.charAt(upto)))
					upto++;
				break;

			case DAYS_2:
			case MONTHS_2:
			case DAYS_1_OR_2:
			case MONTHS_1_OR_2:
				if (upto == inputLength)
					throw new DateTimeParseException("Expecting digit, end of input", input, upto);
				inputChar = input.charAt(upto);
				if (!Character.isDigit(inputChar))
					throw new DateTimeParseException("Expecting digit", input, upto);
				value = inputChar - '0';
				upto++;
				if (nextToken == Token.DAYS_2 && upto < inputLength && dateSeparator == input.charAt(upto))
					throw new DateTimeParseException("Insufficient digits in input (d)", input, upto);
				if (nextToken == Token.MONTHS_2 && upto < inputLength && dateSeparator == input.charAt(upto))
					throw new DateTimeParseException("Insufficient digits in input (M)", input, upto);

				if ((nextToken == Token.DAYS_2 || nextToken == Token.MONTHS_2) && (upto == inputLength || !Character.isDigit(input.charAt(upto))))
					throw new DateTimeParseException("Expecting digit", input, upto);
				if (upto < inputLength && Character.isDigit(input.charAt(upto))) {
					value = 10 * value + (input.charAt(upto) - '0');
					final int limit = (nextToken == Token.DAYS_1_OR_2  || nextToken == Token.DAYS_2) ? 31 : 12;
					if (value > limit)
						throw new DateTimeParseException("Value too large for day/month", input, upto);
					upto++;
				}
				if (value == 0)
					throw new DateTimeParseException("0 value illegal for day/month", input, upto);
				break;

			case FRACTION:
				for (int i = 0; i < token.getCount(); i++) {
					if (upto == inputLength)
						throw new DateTimeParseException("Expecting digit, end of input", input, upto);
					if (!Character.isDigit(input.charAt(upto)))
						throw new DateTimeParseException("Expecting digit", input, upto);
					upto++;
				}
				break;

			case HOURS12_2:
			case HOURS24_2:
			case MINS_2:
			case SECS_2:
			case YEARS_2:
			case DIGITS_2:
				if (upto == inputLength)
					throw new DateTimeParseException("Expecting digit, end of input", input, upto);
				inputChar = input.charAt(upto);
				if (!Character.isDigit(inputChar))
					throw new DateTimeParseException("Expecting digit", input, upto);
				value = inputChar - '0';
				upto++;
				if (upto == inputLength)
					throw new DateTimeParseException("Expecting digit, end of input", input, upto);
				inputChar = input.charAt(upto);
				if (!Character.isDigit(inputChar))
					throw new DateTimeParseException("Expecting digit", input, upto);
				value = 10 * value + (inputChar - '0');
				upto++;
				validateTokenValue(nextToken, value, input, upto - 2);
				break;

			case YEARS_4:
				if (upto + 4 > inputLength)
					throw new DateTimeParseException("Expecting digit, end of input", input, upto);
				for (int j = 0; j < 4; j++) {
					if (!Character.isDigit(input.charAt(upto)))
						throw new DateTimeParseException("Expecting digit", input, upto);
					upto++;
				}
				break;

			case CONSTANT_CHAR:
				if (upto == inputLength)
					throw new DateTimeParseException("Expecting constant char, end of input", input, upto);
				if (input.charAt(upto) != token.getValue())
					throw new DateTimeParseException("Expecting constant char", input, upto);
				upto++;
				break;

			case AMPM:
				if (upto + 1 >= inputLength)
					throw new DateTimeParseException("Expecting am/pm indicator, end of input", input, upto);
				inputChar = input.charAt(upto);
				if (inputChar != 'a' && inputChar != 'A' && inputChar != 'p' && inputChar != 'P')
					throw new DateTimeParseException("Expecting am/pm indicator", input, upto);
				upto++;
				inputChar = input.charAt(upto);
				if (inputChar != 'm' && inputChar != 'M')
					throw new DateTimeParseException("Expecting am/pm indicator", input, upto);
				upto++;
				break;

			case TIMEZONE:
				start = upto;
				while (upto < inputLength && input.charAt(upto) != ' ')
					upto++;
				final String currentTimeZone = input.substring(start, upto);
				if (!DateTimeParser.timeZones.contains(currentTimeZone))
					throw new DateTimeParseException("Expecting time zone - bad time zone: " + currentTimeZone, input, upto);
				break;

			case TIMEZONE_OFFSET:
				if (token.getCount() < 1 || token.getCount() > 5)
					throw new DateTimeParseException("Invalid time zone offset", input, upto);

				final int[] timeZoneLength = { -1, 2, 4, 5, 6, 8 };
				final int len = timeZoneLength[token.getCount()];

				if (upto + len >= inputLength)
					throw new DateTimeParseException("Expecting time zone offset, end of input", input, upto);

				final String[] timeZonePattern = { null, "¶¶", "¶¶¶¶", "¶¶:¶¶", "¶¶¶¶¶¶", "¶¶:¶¶:¶¶" };
				final String pattern = timeZonePattern[token.getCount()];
				final char direction = input.charAt(upto);
				final String offset = input.substring(upto + 1, upto + 1 + len).replaceAll("[0-9]", "¶");
				if ((direction != '-' && direction != '+') || !pattern.equals(offset))
					throw new DateTimeParseException("Expecting time zone offset, bad time zone offset", input, upto);

				// Validate hour offset
				final int hour = Utils.getValue(input, upto + 1, 2, 2);
				if (hour > 18)
					throw new DateTimeParseException("Expecting time zone offset, invalid hour offset", input, upto + 1);

				final int[] minuteOffset = { -1, -1, 2, 3, 2, 3 };
				// Validate minute offset (if necessary)
				if (minuteOffset[token.getCount()] != -1) {
					final int minute = Utils.getValue(input, upto + 1 + minuteOffset[token.getCount()], 2, 2);
					if (minute > 59)
						throw new DateTimeParseException("Expecting time zone offset, invalid minute offset", input, upto + 1 + minuteOffset[token.getCount()]);
				}

				final int[] secondOffset = { -1, -1, -1, -1, 4, 6 };
				// Validate second offset (if necessary)
				if (secondOffset[token.getCount()] != -1) {
					final int second = Utils.getValue(input, upto + 1 + secondOffset[token.getCount()], 2, 2);
					if (second > 59)
						throw new DateTimeParseException("Expecting time zone offset, invalid second offset", input, upto + 1 + secondOffset[token.getCount()]);
				}
				upto += len + 1;

				// One letter outputs just the hour, such as '+01', unless the minute is non-zero in which case the minute is also output, such as '+0130'.
				if (token.getCount() == 1 && upto + 2 == inputLength)
					upto += 2;
				break;
			}
		}

		if (upto != inputLength)
			throw new DateTimeParseException("Expecting end of input, extraneous input found", input, upto);
	}

	/**
	 * Return the detected type of this input.
	 * @return The detected type of this input, will be either "Date", "Time",
	 *  "DateTime", "ZonedDateTime" or "OffsetDateTime".
	 */
	public PatternInfo.Type getType() {
		if (timeElements == -1)
			return PatternInfo.Type.LOCALDATE;
		if (dateElements == -1)
			return PatternInfo.Type.LOCALTIME;
		if (timeZone == null || timeZone.length() == 0)
			return PatternInfo.Type.LOCALDATETIME;
		return timeZone.indexOf('z') == -1 ? PatternInfo.Type.OFFSETDATETIME : PatternInfo.Type.ZONEDDATETIME;
	}

	private String asDate(final char[] fieldChars) {
		final StringBuilder ret = new StringBuilder();
		for (int f = 0; f < fieldChars.length; f++) {
			for (int i = 0; i < dateFieldLengths[f]; i++) {
				ret.append(fieldChars[f]);
			}
			if (f + 1 < fieldChars.length)
				ret.append(dateSeparator);
		}

		return ret.toString();
	}

	/**
	 * Return the Regular Expression that matches this Date/Time object. All valid inputs should match this
	 * Regular Expression, however, not all inputs that match this RE are necessarily valid.  For example,
	 * 28/13/2017 will match the RE (\d{2}/\d{2}/\d{4}) however this is not a valid date with pattern dd/MM/yyyy.
	 * @return The Regular Expression that mirrors this Date/Time object.
	 **/
	@SuppressWarnings("incomplete-switch")
	public String getRegExp() {
		final StringBuilder ret = new StringBuilder(40);
		int digitsMin = 0;
		int digitsMax = 0;

		if (formatString == null)
			formatString = getFormatString();

		for (final FormatterToken token : tokenize(formatString)) {
			if (token.getType() == Token.CONSTANT_CHAR || token.getType() == Token.MONTH ||
					token.getType() == Token.MONTH_ABBR || token.getType() == Token.DAY_OF_WEEK_ABBR ||
					token.getType() == Token.AMPM || token.getType() == Token.TIMEZONE ||
					token.getType() == Token.TIMEZONE_OFFSET) {
				if (digitsMin != 0) {
					ret.append("\\d").append(Utils.regExpLength(digitsMin, digitsMax));
					digitsMin = digitsMax = 0;
				}
				switch (token.getType()) {
				case CONSTANT_CHAR:
					// Cope with the Character being a '.' which needs to be sloshed as an RE
					if (token.getValue() == '.')
						ret.append('\\');
					ret.append(token.getValue());
					break;

				case MONTH:
					ret.append(LocaleInfo.getMonthsRegExp(locale));
					break;

				case DAY_OF_WEEK_ABBR:
					ret.append(LocaleInfo.getShortWeekdaysRegExp(locale));
					break;

				case MONTH_ABBR:
					ret.append(LocaleInfo.getShortMonthsRegExp(locale));
					break;

				case AMPM:
					ret.append(LocaleInfo.getAMPMRegExp(locale));
					break;

				case TIMEZONE:
					ret.append(".*");
					break;

				case TIMEZONE_OFFSET:
//					Offset X and x: This formats the offset based on the number of pattern letters.
//				    One letter outputs just the hour, such as '+01', unless the minute is non-zero in which case the minute is also output, such as '+0130'.
//					Two letters outputs the hour and minute, without a colon, such as '+0130'.
//					Three letters outputs the hour and minute, with a colon, such as '+01:30'.
//					Four letters outputs the hour and minute and optional second, without a colon, such as '+013015'.
//					Five letters outputs the hour and minute and optional second, with a colon, such as '+01:30:15'.
//					Six or more letters throws IllegalArgumentException. Pattern letter 'X' (upper case) will output 'Z' when the offset to be output would be zero, whereas pattern letter 'x' (lower case) will output '+00', '+0000', or '+00:00'.
					switch (token.getCount()) {
					case 1:
						ret.append("[-+][0-9]{2}([0-9]{2})?");
						break;

					case 2:
						ret.append("[-+][0-9]{4}");
						break;

					case 3:
						ret.append("[-+][0-9]{2}:[0-9]{2}");
						break;

					case 4:
						ret.append("[-+][0-9]{4}([0-9]{2})?");
						break;

					case 5:
						ret.append("[-+][0-9]{2}:[0-9]{2}(:[0-9]{2})?");
						break;
					}
					break;
				}
			}
			else {
				switch (token.getType()) {
				case DAYS_1_OR_2:
				case DIGITS_1_OR_2:
				case MONTHS_1_OR_2:
				case HOURS12_1_OR_2:
				case HOURS24_1_OR_2:
					digitsMin += 1;
					digitsMax += 2;
					break;

				case DAYS_2:
				case MONTHS_2:
				case YEARS_2:
				case HOURS12_2:
				case HOURS24_2:
				case MINS_2:
				case SECS_2:
				case DIGITS_2:
					digitsMin += 2;
					digitsMax += 2;
					break;

				case YEARS_4:
					digitsMin += 4;
					digitsMax += 4;
					break;

				case FRACTION:
					digitsMin += token.getCount();
					digitsMax += token.getCount();
					break;
				}
			}
		}

		if (digitsMin != 0)
			ret.append("\\d").append(Utils.regExpLength(digitsMin, digitsMax));

		return ret.toString();
	}

	/**
	 * Return a DateTimeFormatter representation of the DateTimeParserResult.
	 * @return A String representation using DateTimeFormatter semantics.
	 */
	public String getFormatString() {
		if (formatString != null)
			return formatString;

		String hours = (amPmIndicator != null && amPmIndicator) ? "h" : "H";
		if (hourLength == 2)
			hours += hours;
		String timeAnswer = null;
		switch (timeElements) {
		case 0:
			timeAnswer = "";
			break;
		case 1:
			timeAnswer = hours;
			break;
		case 2:
			timeAnswer = hours + ":mm";
			break;
		case 3:
			timeAnswer = hours + ":mm:ss" ;
			break;
		case 4:
			timeAnswer = hours + ":mm:ss" + ".S";
			break;
		}
		if (amPmIndicator != null && amPmIndicator)
			timeAnswer += " a";

		String dateAnswer = "";
		if (dateElements != 0) {
			if (yearOffset == -1) {
				if (dayOffset != -1) {
					// The day must be 1, since if it were 0 the year would be known as d/y/m is not valid
					dateAnswer = asDate(new char[] {'M', 'd', 'y'});
				}
				else {
					// yearOffset == -1 && dayOffset == -1
					if (resolutionMode != DateResolutionMode.None)
						if (resolutionMode == DateResolutionMode.DayFirst || monthOffset == 1)
							dateAnswer = asDate(new char[] {'d', 'M', 'y'});
						else
							dateAnswer = asDate(new char[] {'M', 'd', 'y'});
					else {
						dateAnswer = asDate(new char[] {'?', monthOffset == 1 ? 'M' : '?', '?'});
					}
				}
			}
			else if (yearOffset == 0) {
				if (dayOffset != -1) {
					if (dayOffset == 1)
						dateAnswer = asDate(new char[] {'y', 'd', 'M'});
					else
						dateAnswer = asDate(new char[] {'y', 'M', 'd'});
				} else
					dateAnswer += asDate(new char[] {'y', '?', '?'});
			}
			else if (yearOffset == 2) {
				if (dayOffset != -1) {
					if (dayOffset == 0)
						dateAnswer = asDate(new char[] {'d', 'M', 'y'});
					else
						dateAnswer = asDate(new char[] {'M', 'd', 'y'});
				} else {
					if (resolutionMode != DateResolutionMode.None)
						if (resolutionMode == DateResolutionMode.DayFirst)
							dateAnswer = asDate(new char[] {'d', 'M', 'y'});
						else
							dateAnswer = asDate(new char[] {'M', 'd', 'y'});
					else
						dateAnswer = asDate(new char[] {'?', '?', 'y'});
				}
			}
		}

		if (timeElements == -1)
			return dateAnswer + timeZone;
		if (dateElements == -1)
			return timeAnswer;

		final String separator = dateTimeSeparator == ' ' ? " " : "'T'";
		formatString = (timeFirst != null && timeFirst) ? timeAnswer + separator + dateAnswer + timeZone
				: dateAnswer + separator + timeAnswer + timeZone;

		return formatString;
	}
}
