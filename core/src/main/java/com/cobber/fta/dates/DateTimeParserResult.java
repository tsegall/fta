/*
 * Copyright 2017-2023 Tim Segall
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
package com.cobber.fta.dates;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.InternalErrorException;
import com.cobber.fta.core.MinMax;
import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.core.RegExpSplitter;
import com.cobber.fta.core.Utils;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;

/**
 * DateTimeParserResult is the result of a {@link DateTimeParser} analysis.
 */
public class DateTimeParserResult {
	public final static int HOUR_INDEX = 0;
	public final static int FRACTION_INDEX = 3;

	public int timeElements = -1;
	public int dateElements = -1;
	public Boolean timeFirst;
	public Character dateTimeSeparator;
	public int yearOffset = -1;
	public int monthOffset = -1;
	public int dayOffset = -1;
	public int[] dateFieldLengths = {-1, -1, -1};
	public int[] dateFieldOffsets = {-1, -1, -1};
	public int[] dateFieldPad = {0, 0, 0};
	public MinMax[] timeFieldLengths = {new MinMax(), new MinMax(), new MinMax(), new MinMax()};
	public int[] timeFieldOffsets = {-1, -1, -1, -1};
	public int[] timeFieldPad = {0, 0, 0, 0};
	public String timeZone = "";
	public Character dateSeparator;
	private String formatString;
	public Boolean amPmIndicator;
	public TokenList tokenized;

	private final DateResolutionMode resolutionMode;
	private final DateTimeParserConfig config;
	private final Locale locale;
	private final LocaleInfo localeInfo;
	private final int hourLength;

	private static Map<String, DateTimeParserResult> dtpCache = new ConcurrentHashMap<>();

	DateTimeParserResult(final String formatString, final DateResolutionMode resolutionMode, final DateTimeParserConfig config, final int timeElements,
			final MinMax[] timeFieldLengths, final int[] timeFieldOffsets, final int[] timeFieldPad, final int hourLength, final int dateElements, final int[] dateFieldLengths,
			final int[] dateFieldOffsets, final int[] dateFieldPad, final Boolean timeFirst, final Character dateTimeSeparator, final int yearOffset, final	int monthOffset,
			final int dayOffset, final Character dateSeparator, final String timeZone, final Boolean amPmIndicator, final TokenList tokenized) {
		this.formatString = formatString;
		this.resolutionMode = resolutionMode;
		this.config = config;
		this.locale = config.getLocale();
		this.localeInfo = LocaleInfo.getInstance(config.getLocaleInfoConfig());
		this.timeElements = timeElements;
		this.timeFieldLengths = timeFieldLengths;
		this.timeFieldOffsets = timeFieldOffsets;
		this.timeFieldPad = timeFieldPad;
		this.hourLength = hourLength;
		this.dateElements = dateElements;
		this.dateFieldLengths = dateFieldLengths;
		this.dateFieldOffsets = dateFieldOffsets;
		this.dateFieldPad = dateFieldPad;
		this.timeFirst = timeFirst;
		this.dateTimeSeparator = dateTimeSeparator;
		this.dayOffset = dayOffset;
		this.monthOffset = monthOffset;
		this.yearOffset = yearOffset;
		this.dateSeparator = dateSeparator;
		this.timeZone = timeZone;
		this.amPmIndicator = amPmIndicator;
		this.tokenized = tokenized;
	}

	public static DateTimeParserResult newInstance(final DateTimeParserResult r) {
		MinMax[] timeFieldLengthsClone = null;
		if (r.timeFieldLengths != null) {
			timeFieldLengthsClone = new MinMax[r.timeFieldLengths.length];
			for (int i = 0; i < r.timeFieldLengths.length; i++) {
				timeFieldLengthsClone[i] = new MinMax(r.timeFieldLengths[i]);
			}
		}

		return new DateTimeParserResult(r.formatString, r.resolutionMode, r.config,
				r.timeElements,
				timeFieldLengthsClone,
				r.timeFieldOffsets != null ? Arrays.copyOf(r.timeFieldOffsets, r.timeFieldOffsets.length) : null,
				r.timeFieldPad != null ? Arrays.copyOf(r.timeFieldPad, r.timeFieldPad.length) : null,
				r.hourLength, r.dateElements,
				r.dateFieldLengths != null ? Arrays.copyOf(r.dateFieldLengths, r.dateFieldLengths.length) : null,
				r.dateFieldOffsets != null ? Arrays.copyOf(r.dateFieldOffsets, r.dateFieldOffsets.length) : null,
				r.dateFieldPad != null ? Arrays.copyOf(r.dateFieldPad, r.dateFieldPad.length) : null,
				r.timeFirst, r.dateTimeSeparator, r.yearOffset, r.monthOffset, r.dayOffset, r.dateSeparator, r.timeZone,
				r.amPmIndicator, r.tokenized);

	}

	DateTimeParserResult updateStart() {
		MinMax[] timeFieldLengthsClone = null;
		if (this.timeFieldLengths != null) {
			timeFieldLengthsClone = new MinMax[this.timeFieldLengths.length];
			for (int i = 0; i < this.timeFieldLengths.length; i++) {
				timeFieldLengthsClone[i] = new MinMax(this.timeFieldLengths[i]);
			}
		}

		return new DateTimeParserResult(this.formatString, this.resolutionMode, this.config, this.timeElements,
				timeFieldLengthsClone,
				this.timeFieldOffsets != null ? Arrays.copyOf(this.timeFieldOffsets, this.timeFieldOffsets.length) : null,
				this.timeFieldPad != null ? Arrays.copyOf(this.timeFieldPad, this.timeFieldPad.length) : null,
				this.hourLength, this.dateElements,
				this.dateFieldLengths != null ? Arrays.copyOf(this.dateFieldLengths, this.dateFieldLengths.length) : null,
				this.dateFieldOffsets != null ? Arrays.copyOf(this.dateFieldOffsets, this.dateFieldOffsets.length) : null,
				this.dateFieldPad != null ? Arrays.copyOf(this.dateFieldPad, this.dateFieldPad.length) : null,
				this.timeFirst, this.dateTimeSeparator, this.yearOffset, this.monthOffset, this.dayOffset,
				this.dateSeparator, this.timeZone, this.amPmIndicator, this.tokenized);
	}

	DateTimeParserResult updateEnd() {
		return asResult(tokenized.getFormatString(), config.resolutionMode, config);
	}

	enum Token {
		AMPM("a"), AMPM_NL("P"),
		CLOCK24("k"),
		CONSTANT_CHAR,
		QUOTE("'"),
		DAYS("d"), DAY_OF_WEEK("EEEE"), DAY_OF_WEEK_ABBR("EEE"),
		DIGITS("?"),
		ERA("G"),
		MONTHS("M"), MONTH("MMMM"), MONTH_ABBR("MMM"),
		HOURS12("h"), HOURS24("H"), MINS("mm"), SECS("ss"), FRACTION("S"),
		YEARS_2("yy"), YEARS_4("yyyy"),
		TIMEZONE_NAME("z"), TIMEZONE_OFFSET_Z("Z"),
		LOCALIZED_TIMEZONE_OFFSET("O"),
		TIMEZONE_OFFSET("x"), TIMEZONE_OFFSET_ZERO("X");

		public final String rep;

		Token() {
			rep = null;
		}

		Token(final String rep) {
			this.rep = rep;
		}

		public String getRepresentation() {
			return rep;
		}
	}

	public boolean isDateBound() {
		return dayOffset != -1 && monthOffset != -1 && yearOffset != -1;
	}

	protected boolean isDateUnbound() {
		// If there is not a date then it is cannot be unbound
		if (dateElements == -1)
			return false;

		int bound = 0;
		if (dayOffset != -1)
			bound++;
		if (monthOffset != -1)
			bound++;
		if (yearOffset != -1)
			bound++;
		return bound < dateElements;
	}

	/**
	 * Determine whether a string input matches this DateTimeParserResult.
	 * @param input The string to validate (stripped of whitespace).
	 * @return A boolean indicating if the input is valid.
	 */
	public boolean isValid8(final String input) {
		final DateTimeFormatter dtf = new DateTimeParser().withLocale(locale).ofPattern(getFormatString());

		try {
			if (FTAType.LOCALTIME.equals(getType()))
				LocalTime.parse(input, dtf);
			else if (FTAType.LOCALDATE.equals(getType()))
				LocalDate.parse(input, dtf);
			else if (FTAType.LOCALDATETIME.equals(getType()))
				LocalDateTime.parse(input, dtf);
			else if (FTAType.ZONEDDATETIME.equals(getType()))
				ZonedDateTime.parse(input, dtf);
			else
				OffsetDateTime.parse(input, dtf);
			return true;
		}
		catch (DateTimeParseException exc) {
			return false;
		}
	}

	private boolean checkYear(final Temporal t) {
		final int year = t.get(ChronoField.YEAR);
		return year >= DateTimeParser.EARLY_LONG_YYYY && year <= DateTimeParser.LATE_LONG_YYYY;
	}

	/**
	 * Determine whether a string input matches the supplied DateTimeFormatter.
	 * @param input The string to validate (stripped of whitespace).
	 * @param dtf The DateTimeFormatter used to validate the input.
	 * @return A boolean indicating if the input is valid.
	 */
	public boolean isPlausible(final String input, final DateTimeFormatter dtf) {
		try {
			if (FTAType.LOCALTIME.equals(getType())) {
				LocalTime.parse(input, dtf);
				return true;
			}
			else if (FTAType.LOCALDATE.equals(getType()))
				return checkYear(LocalDate.parse(input, dtf));
			else if (FTAType.LOCALDATETIME.equals(getType()))
				return checkYear(LocalDateTime.parse(input, dtf));
			else if (FTAType.ZONEDDATETIME.equals(getType()))
				return checkYear(ZonedDateTime.parse(input, dtf));
			else
				return checkYear(OffsetDateTime.parse(input, dtf));
		}
		catch (DateTimeParseException exc) {
			return false;
		}
	}

	/**
	 * Return the locale that we have decided on.
	 * @return The locale that the input is in.
	 */
	public Locale getLocale() {
		return locale;
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
	 * Given an input string with a DateTimeFormatter pattern convert to a DateTimeParserResult
	 * @param formatString A DateTimeString using DateTimeFormatter patterns
	 * @param resolutionMode 	When we have ambiguity - should we prefer to conclude day first, month first or unspecified
	 * @param config DateTimeParserConfig (including the locale the input string is in)
	 * @return The corresponding DateTimeParserResult
	 */
	public static DateTimeParserResult asResult(final String formatString, final DateResolutionMode resolutionMode, final DateTimeParserConfig config) {
		final String key = resolutionMode.name() + '#' + config.getLocale() + '#' + formatString + '#' + config.noAbbreviationPunctuation;
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
		final int[] dateFieldLengths = {-1, -1, -1};
		final int[] dateFieldOffsets = {-1, -1, -1};
		final int[] dateFieldPad = {0, 0, 0};
		final MinMax[] timeFieldLengths = {new MinMax(), new MinMax(), new MinMax(), new MinMax()};
		final int[] timeFieldOffsets = {-1, -1, -1, -1};
		final int[] timeFieldPad = {0, 0, 0, 0};
		String timeZone = "";
		Boolean timeFirst = null;
		Character dateSeparator = null;
		Character dateTimeSeparator = ' ';
		boolean fullyBound = true;
		Boolean amPmIndicator = null;
		int padLength = 0;

		final int formatLength = formatString.length();

		for (int i = 0; i < formatLength; i++) {
			final char ch = formatString.charAt(i);
			switch (ch) {
			case '?':
				if (dateElements == dateFieldOffsets.length)
					return null;
				dateFieldOffsets[dateElements] = i;
				dateFieldPad[dateElements] = padLength;
				padLength = 0;
				++dateElements;
				if (i + 1 < formatLength && formatString.charAt(i + 1) == '?') {
					i++;
					dateFieldLengths[dateElements - 1] = 2;
				}
				else {
					dateFieldLengths[dateElements - 1] = 1;
				}

				if (dateElements == 1 && i + 1 < formatLength) {
					final char nextCh = formatString.charAt(i + 1);
					if (!Character.isAlphabetic(nextCh))
						dateSeparator = nextCh;
				}
				fullyBound = false;
				break;

			case 'M':
				if (dateElements == dateFieldOffsets.length)
					return null;
				dateFieldOffsets[dateElements] = i;
				dateFieldPad[dateElements] = padLength;
				padLength = 0;
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

				if (dateElements == 1 && i + 1 < formatLength) {
					final char nextCh = formatString.charAt(i + 1);
					if (!Character.isAlphabetic(nextCh))
						dateSeparator = nextCh;
				}
				break;

			case 'd':
				if (dateElements == dateFieldOffsets.length)
					return null;
				dateFieldOffsets[dateElements] = i;
				dateFieldPad[dateElements] = padLength;
				padLength = 0;
				dayOffset = dateElements++;
				if (i + 1 < formatLength && formatString.charAt(i + 1) == 'd') {
					i++;
					dayLength = 2;
				}
				else
					dayLength = 1;
				dateFieldLengths[dateElements - 1] = dayLength;

				if (dateElements == 1 && i + 1 < formatLength) {
					final char nextCh = formatString.charAt(i + 1);
					if (!Character.isAlphabetic(nextCh))
						dateSeparator = nextCh;
				}
				break;

			case 'k':
			case 'h':
			case 'H':
				if (timeElements == timeFieldOffsets.length)
					return null;
				timeFieldOffsets[timeElements] = i;
				timeFieldPad[timeElements] = padLength;
				padLength = 0;
				timeFirst = dateElements == 0;
				if (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					i++;
					hourLength = 2;
					timeFieldLengths[timeElements].set(2, 2);
				}
				else {
					hourLength = 1;
					timeFieldLengths[timeElements].set(1, 2);
				}
				timeElements++;
				break;

			case 'm':
			case 's':
				if (timeElements == timeFieldOffsets.length)
					return null;
				timeFieldOffsets[timeElements] = i;
				timeFieldLengths[timeElements].set(2);
				timeFieldPad[timeElements] = padLength;
				padLength = 0;
				timeElements++;
				if (i + 1 >= formatLength || formatString.charAt(i + 1) != ch)
					return null;
				i++;
				break;

			case 'S':
				if (timeElements == timeFieldOffsets.length)
					return null;
				timeFieldOffsets[timeElements] = i;
				timeFieldPad[timeElements] = padLength;
				padLength = 0;
				if (i + 1 < formatLength) {
					final char next = formatString.charAt(i + 1);
					if (next == 'S') {
						int fractions = 1;
						while (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
							fractions++;
							i++;
						}
						timeFieldLengths[timeElements].set(fractions);
					}
					else if (next == '{') {
						final RegExpSplitter facts = RegExpSplitter.newInstance(formatString.substring(i + 1));
						if (facts == null)
							return null;
						i += facts.getLength();
						timeFieldLengths[timeElements].set(facts.getMin(), facts.getMax());
					}
					else
						timeFieldLengths[timeElements].set(1);
				}
				else
					timeFieldLengths[timeElements].set(1);
				timeElements++;
				break;

			case 'y':
				if (dateElements == dateFieldOffsets.length)
					return null;
				dateFieldOffsets[dateElements] = i;
				dateFieldPad[dateElements] = padLength;
				padLength = 0;
				yearOffset = dateElements++;
				i++;
				if (i + 1 < formatLength && formatString.charAt(i + 1) == 'y') {
					yearLength = 4;
					i += 2;
				} else
					yearLength = 2;
				dateFieldLengths[dateElements - 1] = yearLength;

				if (dateElements == 1 && i + 1 < formatLength) {
					final char nextCh = formatString.charAt(i + 1);
					if (!Character.isAlphabetic(nextCh))
						dateSeparator = nextCh;
				}
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

			case 'p':
				padLength = 1;
				if (i + 1 < formatLength && formatString.charAt(i + 1) == 'p') {
					i++;
					padLength++;
				}
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
		ret = new DateTimeParserResult(fullyBound ? formatString : null, resolutionMode, config, timeElements, timeFieldLengths, timeFieldOffsets, timeFieldPad,
				hourLength, dateElements, dateFieldLengths, dateFieldOffsets, dateFieldPad, timeFirst, dateTimeSeparator,
				yearOffset, monthOffset, dayOffset, dateSeparator, timeZone, amPmIndicator, TokenList.getTokenList(formatString));
		dtpCache.put(key, ret);

		return newInstance(ret);
	}

	@SuppressWarnings("incomplete-switch")
	private void validateTokenValue(final Token token, final int value, final String input, final int upto) {
		switch (token) {
		case CLOCK24:
			if (value == 0 || value > 24)
				throw new DateTimeParseException("Invalid value for hours (expected 1-24)", input, upto);
			break;

		case HOURS12:
			if (value == 0 || value > 12)
				throw new DateTimeParseException("Invalid value for hours (expected 1-12)", input, upto);
			break;

		case HOURS24:
			if (value == 24)
				throw new DateTimeParseException("Invalid value for hours: 24 (expected 0-23)", input, upto);
			else if (value > 24)
				throw new DateTimeParseException("Invalid value for hours (expected 0-23)", input, upto);
			break;

		case MINS:
			if (value > 59)
				throw new DateTimeParseException("Invalid value for minutes (expected 0-59)", input, upto);
			break;

		case SECS:
			if (value > 59)
				throw new DateTimeParseException("Invalid value for seconds (expected 0-59)", input, upto);
			break;
		}
	}

	/**
	 * Determine whether a string input matches this DateTimeParserResult.
	 * @param input The string to validate (stripped of whitespace).
	 * @throws DateTimeParseException If the input does not match the DateTimeParserResult
	 *
	 * Note: This routine is akin to the parse() methods on LocalDateTime/ZonedDateTime etc. but runs faster
	 * and does not return an instance of the LocalDateTime/ZonedDateTime etc.
	 */
	public void parse(final String input) {
		final int inputLength = input.length();
		int upto = 0;

		if (formatString == null)
			formatString = getFormatString();

		if (tokenized == null)
			tokenized = TokenList.getTokenList(formatString);

		Token nextToken = null;
		for (final FormatterToken token : tokenized) {
			nextToken = token.getType();

			char inputChar;
			int value = 0;
			int start;

			switch (nextToken) {
			case DAY_OF_WEEK:
				start = upto;
				final int dayOfWeekOffset = localeInfo.skipValidDayOfWeek(input.substring(upto));
				if (dayOfWeekOffset == -1)
					throw new DateTimeParseException("Day of Week invalid", input, start);
				upto += dayOfWeekOffset;
				break;

			case DAY_OF_WEEK_ABBR:
				start = upto;
				final int dayOfWeekAbbrOffset = localeInfo.skipValidDayOfWeekAbbr(input.substring(upto));
				if (dayOfWeekAbbrOffset == -1)
					throw new DateTimeParseException("Day of Week Abbreviation invalid", input, start);
				upto += dayOfWeekAbbrOffset;
				break;

			case MONTH:
				start = upto;
				final int monthOffset = localeInfo.skipValidMonth(input.substring(upto));
				if (monthOffset == -1)
					throw new DateTimeParseException("Month invalid", input, start);
				upto += monthOffset;
				break;

			case MONTH_ABBR:
				start = upto;
				final String monthAbbr = localeInfo.findValidMonthAbbr(input.substring(upto));
				if (monthAbbr == null)
					throw new DateTimeParseException("Month Abbreviation invalid", input, start);
				upto += monthAbbr.length();
				break;

			case CLOCK24:
			case HOURS12:
			case HOURS24:
				if (upto == inputLength)
					throw new DateTimeParseException("Expecting digit, end of input", input, upto);
				inputChar = input.charAt(upto);
				if (token.getCount() == 1) {
					if (token.getFieldWidth() != -1 && inputChar == ' ' && upto < inputLength) {
						inputChar = input.charAt(++upto);
					}
					if (!Character.isDigit(inputChar))
						throw new DateTimeParseException("Expecting digit", input, upto);
					value = inputChar - '0';
					upto++;
					if (upto != inputLength && Character.isDigit(input.charAt(upto))) {
						value = 10 * value + (input.charAt(upto) - '0');
						upto++;
						if (nextToken != Token.DIGITS)
							validateTokenValue(nextToken, value, input, upto - 2);
					}
				}
				else {
					if (!Character.isDigit(inputChar))
						throw new DateTimeParseException("Expecting digit", input, upto);
					value = inputChar - '0';
					upto++;
					if (upto == inputLength)
						throw new DateTimeParseException("Expecting digit, end of input", input, upto);
					inputChar = input.charAt(upto);
					if (nextToken == Token.HOURS12 && token.getCount() == 2 && upto < inputLength &&
							input.charAt(upto) == ':')
						throw new DateTimeParseException("Insufficient digits in input (h)", input, upto);
					else if (nextToken == Token.HOURS24 && token.getCount() == 2 && upto < inputLength &&
							input.charAt(upto) == ':')
						throw new DateTimeParseException("Insufficient digits in input (H)", input, upto);
					if (!Character.isDigit(inputChar))
						throw new DateTimeParseException("Expecting digit", input, upto);
					value = 10 * value + (inputChar - '0');
					upto++;
					validateTokenValue(nextToken, value, input, upto - 2);
				}
				break;

			case DAYS:
			case MONTHS:
				if (upto == inputLength)
					throw new DateTimeParseException("Expecting digit, end of input", input, upto);
				inputChar = input.charAt(upto);
				if (token.getFieldWidth() != -1 && inputChar == ' ' && upto < inputLength)
					inputChar = input.charAt(++upto);
				if (!Character.isDigit(inputChar))
					throw new DateTimeParseException("Expecting digit", input, upto);
				value = inputChar - '0';
				upto++;
				if (nextToken == Token.DAYS && token.getCount() == 2 && upto < inputLength &&
						dateSeparator != null && dateSeparator == input.charAt(upto))
					throw new DateTimeParseException("Insufficient digits in input (d)", input, upto);
				else if (nextToken == Token.MONTHS && token.getCount() == 2 && upto < inputLength &&
						dateSeparator != null && dateSeparator == input.charAt(upto))
					throw new DateTimeParseException("Insufficient digits in input (M)", input, upto);

				if ((nextToken == Token.DAYS || nextToken == Token.MONTHS) && token.getCount() == 2 && (upto == inputLength || !Character.isDigit(input.charAt(upto))))
					throw new DateTimeParseException("Expecting digit", input, upto);
				if (upto < inputLength && Character.isDigit(input.charAt(upto))) {
					value = 10 * value + (input.charAt(upto) - '0');
					final int limit = (nextToken == Token.DAYS) ? 31 : 12;
					if (value > limit)
						throw new DateTimeParseException("Value too large for day/month", input, upto);
					upto++;
				}
				if (value == 0)
					throw new DateTimeParseException("0 value illegal for day/month", input, upto);
				break;

			case FRACTION:
				if (upto == inputLength)
					throw new DateTimeParseException("Expecting digit, end of input", input, upto);
				for (int i = 0; i < token.getHigh(); i++) {
					if (upto == inputLength) {
						if (i < token.getCount())
							throw new DateTimeParseException("Insufficient digits in input (S)", input, upto);
						else
							break;
					}
					if (!Character.isDigit(input.charAt(upto))) {
						if (i < token.getCount())
							throw new DateTimeParseException("Insufficient digits in input (S)", input, upto);
						else
							break;
					}
					upto++;
				}
				break;

			case MINS:
			case SECS:
			case YEARS_2:
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
				boolean allZeroes = true;
				for (int j = 0; j < 4; j++) {
					final char ch = input.charAt(upto);
					if (ch != '0')
						allZeroes = false;
					if (!Character.isDigit(ch))
						throw new DateTimeParseException("Expecting digit", input, upto);
					upto++;
				}
				if (allZeroes)
					throw new DateTimeParseException("Invalid value for YearOfEra: 0000", input, upto);
				break;

			case QUOTE:
				// Quote's are used to encapsulate a constant string - so we just ignore them on parsing
				// and process the embedded sequence of CONSTANT_CHAR's that follow until the next QUOTE.
				break;

			case CONSTANT_CHAR:
				if (upto == inputLength)
					throw new DateTimeParseException("Expecting constant char, end of input", input, upto);
				if (input.charAt(upto) != token.getValue())
					throw new DateTimeParseException("Expecting constant char", input, upto);
				upto++;
				break;

			case AMPM:
			case AMPM_NL:
				start = upto;
				final int ampmOffset = localeInfo.skipValidAMPM(input.substring(upto), nextToken == Token.AMPM);
				if (ampmOffset == -1)
					throw new DateTimeParseException("Expecting am/pm indicator", input, start);
				upto += ampmOffset;
				break;

			case LOCALIZED_TIMEZONE_OFFSET:
				// 'O' - expecting H[:MM[:SS]]
				// 'OOOO' - expecting HH:MM[:SS]
				if (token.getCount() != 1 && token.getCount() != 4)
					throw new DateTimeParseException("Invalid localized time zone offset", input, upto);
				start = upto;
				while (upto < inputLength && (input.charAt(upto) != '+' && input.charAt(upto) != '-'))
					upto++;
				final String timeZoneLTO = input.substring(start, upto);
				if (!DateTimeParser.timeZones.contains(timeZoneLTO))
					throw new DateTimeParseException("Expecting time zone - bad time zone: " + timeZoneLTO, input, upto);

				if (upto == inputLength || (input.charAt(upto) != '+' && input.charAt(upto) != '-'))
					throw new DateTimeParseException("Expecting offset - bad time zone offset: " + timeZoneLTO, input, upto);

				upto++;
				final int hourlength = upto == inputLength || !Character.isDigit(input.charAt(upto)) ? 1 : 2;
				if (hourlength == 1 && token.getCount() == 4)
					throw new DateTimeParseException("Expecting time zone offset, expected two digit hour", input, upto);
				final int hours = Utils.getValue(input, upto, hourlength, 2);
				if (hours > 18)
					throw new DateTimeParseException("Expecting time zone offset, invalid hour offset", input, upto);

				upto += hourLength;

				if (upto == inputLength) {
					if (token.getCount() == 4)
						throw new DateTimeParseException("Expecting time zone offset, expected minutes", input, upto + 1);
				}
				else {
					if (input.charAt(upto) != ':') {
						if (token.getCount() == 4)
							throw new DateTimeParseException("Expecting time zone offset, expected minutes", input, upto + 1);
					}
					else {
						upto++;
						final int minutes = Utils.getValue(input, upto, 2, 2);
						if (minutes > 59)
							throw new DateTimeParseException("Expecting time zone offset, invalid minute offset", input, upto);
						upto += 2;
						if (upto != inputLength && input.charAt(upto) == ':') {
							upto++;
							final int seconds = Utils.getValue(input, upto, 2, 2);
							if (seconds > 59)
								throw new DateTimeParseException("Expecting time zone offset, invalid seconds offset", input, upto);
							upto += 2;
						}
					}
				}
				break;

			case TIMEZONE_NAME:
				start = upto;
				while (upto < inputLength && input.charAt(upto) != ' ')
					upto++;
				final String timeZoneT = input.substring(start, upto);
				if (!DateTimeParser.timeZones.contains(timeZoneT))
					throw new DateTimeParseException("Expecting time zone - bad time zone: " + timeZoneT, input, upto);
				break;

			case TIMEZONE_OFFSET_ZERO:
				if (upto >= inputLength)
					throw new DateTimeParseException("Expecting time zone offset, end of input", input, upto);
				if (input.charAt(upto) == 'Z') {
					upto++;
					break;
				}
				// FALL THROUGH

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
			throw new DateTimeParseException("Expecting end of input, extraneous input found, last token (" + nextToken + ")", input, upto);
	}

	/**
	 * Return the detected type of this input.
	 * @return The detected type of this input, will be either "LocalDate", "LocalTime",
	 *  "LocalDateTime", "ZonedDateTime" or "OffsetDateTime".
	 */
	public FTAType getType() {
		if (tokenized == null)
			throw new InternalErrorException("Invoked getType() with no tokenized value");


		for (final FormatterToken t : tokenized) {
			if (t.getType().equals(Token.TIMEZONE_OFFSET) || t.getType().equals(Token.TIMEZONE_OFFSET_ZERO) ||
					t.getType().equals(Token.LOCALIZED_TIMEZONE_OFFSET) || t.getType().equals(Token.TIMEZONE_OFFSET_Z))
				return FTAType.OFFSETDATETIME;
			if (t.getType().equals(Token.TIMEZONE_NAME))
				return FTAType.ZONEDDATETIME;
		}

		if (timeElements == -1)
			return FTAType.LOCALDATE;
		if (dateElements == -1)
			return FTAType.LOCALTIME;

		return FTAType.LOCALDATETIME;
	}

	private void addDigits(final StringBuilder b, final int digitsMin, int digitsMax, int padding) {
		if (padding > 0) {
			digitsMax -= 1;
			padding = 0;
			b.append("[ \\d]\\d");
		}
		else
			b.append("\\d");

		b.append(RegExpSplitter.qualify(digitsMin, digitsMax));
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
		int padding = 0;
		final String x = "[-+][0-9]{2}([0-9]{2})?";
		final String xx = "[-+][0-9]{4}";
		final String xxx = "[-+][0-9]{2}:[0-9]{2}";
		final String xxxx = "[-+][0-9]{4}([0-9]{2})?";
		final String xxxxx = "[-+][0-9]{2}:[0-9]{2}(:[0-9]{2})?";

		if (formatString == null)
			formatString = getFormatString();

		for (final FormatterToken token : tokenized) {
			if (token.getType() == Token.CONSTANT_CHAR || token.getType() == Token.QUOTE ||
					token.getType() == Token.MONTH || token.getType() == Token.MONTH_ABBR ||
					token.getType() == Token.DAY_OF_WEEK || token.getType() == Token.DAY_OF_WEEK_ABBR ||
					token.getType() == Token.AMPM ||  token.getType() == Token.AMPM_NL ||
					token.getType() == Token.TIMEZONE_NAME ||
					token.getType() == Token.TIMEZONE_OFFSET || token.getType() == Token.TIMEZONE_OFFSET_ZERO ||
					token.getType() == Token.LOCALIZED_TIMEZONE_OFFSET) {
				if (digitsMin != 0) {
					addDigits(ret, digitsMin, digitsMax, padding);
					digitsMin = digitsMax = padding = 0;
				}
				switch (token.getType()) {
				case QUOTE:
					// Quote's are used to encapsulate constant strings e.g. we will see
					// QUOTE, CONSTANT_CHAR('T'), QUOTE in the case of an ISO 8601 date ('T') in this case
					// the Regular Expression is only concerned with the T and we can ignore the quotes.
					break;

				case CONSTANT_CHAR:
					ret.append(RegExpGenerator.slosh(token.getValue()));
					break;

				case MONTH:
					ret.append(localeInfo.getMonthsRegExp());
					break;

				case DAY_OF_WEEK:
					ret.append(localeInfo.getWeekdaysRegExp());
					break;

				case DAY_OF_WEEK_ABBR:
					ret.append(localeInfo.getShortWeekdaysRegExp());
					break;

				case MONTH_ABBR:
					ret.append(localeInfo.getShortMonthsRegExp());
					break;

				case AMPM:
					ret.append(localeInfo.getAMPMRegExp());
					break;

				case AMPM_NL:
					ret.append("(AM|PM)");
					break;

				case TIMEZONE_NAME:
					ret.append(".*");
					break;

//					Offset X and x: This formats the offset based on the number of pattern letters.
//				    One letter outputs just the hour, such as '+01', unless the minute is non-zero in which case the minute is also output, such as '+0130'.
//					Two letters outputs the hour and minute, without a colon, such as '+0130'.
//					Three letters outputs the hour and minute, with a colon, such as '+01:30'.
//					Four letters outputs the hour and minute and optional second, without a colon, such as '+013015'.
//					Five letters outputs the hour and minute and optional second, with a colon, such as '+01:30:15'.
//					Six or more letters throws IllegalArgumentException. Pattern letter 'X' (upper case) will output 'Z' when the offset to be output would be zero, whereas pattern letter 'x' (lower case) will output '+00', '+0000', or '+00:00'.
				case TIMEZONE_OFFSET_ZERO:
					switch (token.getCount()) {
					case 1:
						ret.append('(').append(x).append("|Z)");
						break;

					case 2:
						ret.append('(').append(xx).append("|Z)");
						break;

					case 3:
						ret.append('(').append(xxx).append("|Z)");
						break;

					case 4:
						ret.append('(').append(xxxx).append("|Z)");
						break;

					case 5:
						ret.append('(').append(xxxxx).append("|Z)");
						break;
					}
					break;

				case TIMEZONE_OFFSET:
					switch (token.getCount()) {
					case 1:
						ret.append(x);
						break;

					case 2:
						ret.append(xx);
						break;

					case 3:
						ret.append(xxx);
						break;

					case 4:
						ret.append(xxxx);
						break;

					case 5:
						ret.append(xxxxx);
						break;
					}
					break;
				case LOCALIZED_TIMEZONE_OFFSET:
					ret.append("\\p{IsAlphabetic}+[+-]");
					if (token.getCount() == 1) {
						// 'O' - expecting H[:MM[:SS]]
						ret.append("\\d{1,2}(:\\d{2}(:\\d{2})?)?");
					}
					else {
						// 'OOOO' - expecting HH:MM[:SS]
						ret.append("\\d{2}:\\d{2}(:\\d{2})?");
					}
					break;
				}
			}
			else {
				switch (token.getType()) {
				case CLOCK24:
				case DIGITS:
				case DAYS:
				case HOURS12:
				case HOURS24:
				case MONTHS:
					digitsMin += token.getCount();
					// FIXME should not really be 2 should be max
					digitsMax += 2;
					padding = token.getFieldWidth();
					break;

				case YEARS_2:
				case MINS:
				case SECS:
					digitsMin += 2;
					digitsMax += 2;
					break;

				case YEARS_4:
					digitsMin += 4;
					digitsMax += 4;
					break;

				case FRACTION:
					digitsMin += token.getCount();
					digitsMax += token.getHigh();
					break;
				}
			}
		}

		if (digitsMin != 0)
			addDigits(ret, digitsMin, digitsMax, padding);

		return ret.toString();
	}

	/**
	 * Return a DateTimeFormatter representation of the DateTimeParserResult.
	 * @return A String representation using DateTimeFormatter semantics.
	 */
	public String getFormatString() {
		return tokenized.getFormatString();
	}
}
