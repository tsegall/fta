package com.cobber.fta;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DateTimeParserResult is the result of a {@link DateTimeParser} analysis.
 */
public class DateTimeParserResult {
	String format = null;
	int timeElements = -1;
	int hourLength = -1;
	int dateElements = -1;
	Boolean timeFirst = null;
	Character dateTimeSeparator = null;
	int yearOffset = -1;
	int yearLength = -1;
	int monthOffset = -1;
	int monthLength = -1;
	int dayOffset = -1;
	int dayLength = -1;
	int[] dateFieldLengths = new int[] {-1, -1, -1};
	int[] timeFieldLengths = new int[] {-1, -1, -1, -1};
	String timeZone = "";
	Character dateSeparator = null;
	String formatString = null;
	Boolean dayFirst = null;

	static Map<String, DateTimeParserResult> options = new ConcurrentHashMap<String, DateTimeParserResult>();

	DateTimeParserResult(String formatString, Boolean dayFirst, int timeElements, int[] timeFieldLengths, int hourLength,
			int dateElements, int[] dateFieldLengths, Boolean timeFirst, Character dateTimeSeparator, int yearOffset,
			int monthOffset, int dayOffset, Character dateSeparator, String timeZone) {
		this.formatString = formatString;
		this.dayFirst = dayFirst;
		this.timeElements = timeElements;
		this.timeFieldLengths = timeFieldLengths;
		this.hourLength = hourLength;
		this.dateElements = dateElements;
		this.dateFieldLengths = dateFieldLengths;
		this.timeFirst = timeFirst;
		this.dateTimeSeparator = dateTimeSeparator;
		this.dayOffset = dayOffset;
		if (dayOffset != -1)
			this.dayLength = dateFieldLengths[dayOffset];
		this.monthOffset = monthOffset;
		if (monthOffset != -1)
			this.monthLength = dateFieldLengths[monthOffset];
		this.yearOffset = yearOffset;
		if (yearOffset != -1)
			this.yearLength = dateFieldLengths[yearOffset];
		this.dateSeparator = dateSeparator;
		this.timeZone = timeZone;
	}

	private enum Token {
		CONSTANT_CHAR, DAYS_1_OR_2, DAYS_2, DIGITS_1_OR_2, MONTHS_1_OR_2, MONTHS_2,
		HOURS_1_OR_2, HOURS_2, MINS_2, SECS_2, FRACTION,
		DIGITS_2, YEARS_2, YEARS_4, MONTH, MONTH_ABBR, TIMEZONE, TIMEZONE_OFFSET
	}

	private class FormatterToken {
		Token type;
		int count;
		char value;

		FormatterToken(Token type) {
			this.type = type;
		}

		FormatterToken(Token type, int count) {
			this.type = type;
			this.count = count;
		}

		FormatterToken(Token type, char value) {
			this.type = type;
			this.value = value;
		}
	}

	/**
	 * Determine whether a string input matches this DateTimeParserResult.
	 * @param input The string to validate (stripped of whitespace.
	 * @return A boolean indicating if the input is valid.
	 */
	public boolean isValid8(String input) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(getFormatString());

		try {
			if (PatternInfo.Type.TIME.equals(getType()))
				LocalTime.parse(input, formatter);
			else if (PatternInfo.Type.DATE.equals(getType()))
				LocalDate.parse(input, formatter);
			else if (PatternInfo.Type.DATETIME.equals(getType()))
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
	public boolean isValid(String input) {
		try {
			parse(input);
			return true;
		}
		catch (DateTimeParseException exc) {
			return false;
		}
	}

	static String stringify(Boolean bool) {
		return bool == null ? "null" : bool.toString();
	}

	/**
	 * Given an input string in SimpleDateTimeFormat convert to a DateTimeParserResult
	 * @param formatString A DateTimeString using DateTimeFormatter patterns
	 * @param dayFirst 	When we have ambiguity - should we prefer to conclude day first, month first or unspecified
	 * @return The corresponding DateTimeParserResult
	 */
	public static DateTimeParserResult asResult(String formatString, Boolean dayFirst) {
		String key = stringify(dayFirst) + '#' + formatString;
		DateTimeParserResult ret = options.get(key);
//		if (ret != null)
//			System.err.printf("Looked for '%s' and found result with formatString = '%s'\n", key, ret.formatString);
		if (ret != null)
			return ret;

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
		int[] timeFieldLengths = new int[] {-1, -1, -1, -1};
		String timeZone = "";
		Boolean timeFirst = null;
		Character dateSeparator = null;
		Character dateTimeSeparator = ' ';
		Boolean fullyBound = true;

		int formatLength = formatString.length();

		for (int i = 0; i < formatLength; i++) {
			char ch = formatString.charAt(i);
			switch (ch) {
			case '?':
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

			case 'H':
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
				timeFieldLengths[timeElements] = 2;
				timeElements++;
				if (i + 1 >= formatLength || formatString.charAt(i + 1) != ch)
					return null;
				i++;
				break;

			case 'S':
				int fractions = 0;
				while (i + 1 < formatLength && formatString.charAt(i + 1) == 'S') {
					i++;
					fractions++;
				}
				timeFieldLengths[timeElements] = fractions;
				timeElements++;
				break;

			case 'y':
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

			default:
				// FIX ME
			}
		}

		if (dateElements == 0)
			dateElements = -1;
		if (timeElements == 0)
			timeElements = -1;

		// Add to cache
		ret  = new DateTimeParserResult(fullyBound ? formatString : null, dayFirst, timeElements, timeFieldLengths, hourLength, dateElements, dateFieldLengths,
				timeFirst, dateTimeSeparator, yearOffset, monthOffset, dayOffset, dateSeparator, timeZone);
		options.put(key, ret);

		return ret;
	}

	private FormatterToken[] tokenize() {
		if (formatString == null)
			formatString = getFormatString();

		ArrayList<FormatterToken> ret = new ArrayList<FormatterToken>();
		int upto = 0;

		int formatLength = formatString.length();

		for (int i = 0; i < formatLength; i++) {
			char ch = formatString.charAt(i);
			switch (ch) {
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

			case 'H':
			case '?':
				if (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					i++;
					ret.add(new FormatterToken(ch == 'H' ? Token.HOURS_2 : Token.DIGITS_2));
				}
				else
					ret.add(new FormatterToken(ch == 'H' ? Token.HOURS_1_OR_2 : Token.DIGITS_1_OR_2));
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

			default:
				ret.add(new FormatterToken(Token.CONSTANT_CHAR, ch));
			}
		}

		return ret.toArray(new FormatterToken[ret.size()]);
	}


	void validateTokenValue(Token token, int value, String input, int upto) {
		switch (token) {
		case HOURS_2:
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
	public void parse(String input) throws DateTimeParseException {
		int upto = 0;
		int inputLength = input.length();

		if (formatString == null)
			formatString = getFormatString();

		for (FormatterToken token : tokenize()) {
			Token nextToken = token.type;

			char inputChar;
			int value = 0;

			switch (nextToken) {
			case MONTH:
				int start = upto;
				while (upto < inputLength && Character.isAlphabetic(input.charAt(upto)))
					upto++;
				String month = input.substring(start, upto);
				if (DateTimeParser.monthOffset(month) == -1)
					throw new DateTimeParseException("Month invalid", input, start);
				break;

			case MONTH_ABBR:
				if (upto + 3 > inputLength)
					throw new DateTimeParseException("Month Abbreviation not complete", input, upto);
				String monthAbbreviation = input.substring(upto, upto + 3);
				if (DateTimeParser.monthAbbreviationOffset(monthAbbreviation) == -1)
					throw new DateTimeParseException("Month Abbreviation invalid", input, upto);
				upto += 3;
				break;

			case HOURS_1_OR_2:
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
					int limit = (nextToken == Token.DAYS_1_OR_2  || nextToken == Token.DAYS_2) ? 31 : 12;
					if (value > limit)
						throw new DateTimeParseException("Value too large for day/month", input, upto);
					upto++;
				}
				if (value == 0)
					throw new DateTimeParseException("0 value illegal for day/month", input, upto);
				break;

			case FRACTION:
				for (int i = 0; i < token.count; i++) {
					if (upto == inputLength)
						throw new DateTimeParseException("Expecting digit, end of input", input, upto);
					if (!Character.isDigit(input.charAt(upto)))
						throw new DateTimeParseException("Expecting digit", input, upto);
					upto++;
				}
				break;

			case HOURS_2:
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
				if (input.charAt(upto) != token.value)
					throw new DateTimeParseException("Expecting constant char", input, upto);
				upto++;
				break;

			case TIMEZONE:
				String currentTimeZone = input.substring(upto, inputLength);
				if (!DateTimeParser.timeZones.contains(currentTimeZone))
					throw new DateTimeParseException("Expecting time zone - bad time zone: " + currentTimeZone, input, upto);
				upto = inputLength;
				break;

			case TIMEZONE_OFFSET:
				final int[] timeZoneLength = { -1, 2, 4, 5, 6, 8 };
				final String[] timeZonePattern = { null, "þþ", "þþþþ", "þþ:þþ", "þþþþþþ", "þþ:þþ:þþ" };
				final int[] minuteOffset = { -1, -1, 2, 3, 2, 3 };
				final int[] secondOffset = { -1, -1, -1, -1, 4, 6 };

				if (token.count < 1 || token.count > 5)
					throw new DateTimeParseException("Invalid time zone offset", input, upto);
				int len = timeZoneLength[token.count];
				String pattern = timeZonePattern[token.count];

				if (upto + len >= inputLength)
					throw new DateTimeParseException("Expecting time zone offset, end of input", input, upto);
				char direction = input.charAt(upto);
				String offset = input.substring(upto + 1, upto + 1 + len).replaceAll("[0-9]", "þ");
				if ((direction != '-' && direction != '+') || !pattern.equals(offset))
					throw new DateTimeParseException("Expecting time zone offset, bad time zone offset", input, upto);

				// Validate hour offset
				int hour = DateTimeParser.getValue(input, upto + 1, 2);
				if (hour > 18)
					throw new DateTimeParseException("Expecting time zone offset, invalid hour offset", input, upto + 1);

				// Validate minute offset (if necessary)
				if (minuteOffset[token.count] != -1) {
					int minute = DateTimeParser.getValue(input, upto + 1 + minuteOffset[token.count], 2);
					if (minute > 59)
						throw new DateTimeParseException("Expecting time zone offset, invalid minute offset", input, upto + 1 + minuteOffset[token.count]);
				}

				// Validate second offset (if necessary)
				if (secondOffset[token.count] != -1) {
					int second = DateTimeParser.getValue(input, upto + 1 + secondOffset[token.count], 2);
					if (second > 59)
						throw new DateTimeParseException("Expecting time zone offset, invalid second offset", input, upto + 1 + secondOffset[token.count]);
				}
				upto += len + 1;

				// One letter outputs just the hour, such as '+01', unless the minute is non-zero in which case the minute is also output, such as '+0130'.
				if (token.count == 1 && upto + 2 == inputLength)
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
			return PatternInfo.Type.DATE;
		if (dateElements == -1)
			return PatternInfo.Type.TIME;
		if (timeZone == null || timeZone.length() == 0)
			return PatternInfo.Type.DATETIME;
		return timeZone.indexOf('z') == -1 ? PatternInfo.Type.OFFSETDATETIME : PatternInfo.Type.ZONEDDATETIME;
	}

	private String asDate(char[] fieldChars) {
		StringBuilder ret = new StringBuilder();
		for (int f = 0; f < fieldChars.length; f++) {
			for (int i = 0; i < dateFieldLengths[f]; i++) {
				ret.append(fieldChars[f]);
			}
			if (f + 1 < fieldChars.length)
				ret.append(dateSeparator);
		}

		return ret.toString();
	}

	private String digitsRegExp(int digitsMin, int digitsMax) {
		StringBuilder ret = new StringBuilder();

		ret.append("\\d{");
		ret.append(digitsMin);
		if (digitsMax != digitsMin) {
			ret.append(',');
			ret.append(digitsMax);
		}
		ret.append('}');

		return ret.toString();
	}

	/**
	 * Return the Regular Expression that matches this Date/Time object. All valid inputs should match this
	 * Regular Expression, however, not all inputs that match this RE are necessarily valid.  For example,
	 * 28/13/2017 will match the RE (\d{2}/\d{2}/\d{4}) however this is not a valid date with pattern dd/MM/yyyy.
	 * @return The Regular Expression that mirrors this Date/Time object.
	 **/
	public String getRegExp() {
		StringBuilder ret = new StringBuilder();
		int digitsMin = 0;
		int digitsMax = 0;

		for (FormatterToken token : tokenize()) {
			if (token.type == Token.CONSTANT_CHAR || token.type == Token.MONTH || token.type == Token.MONTH_ABBR ||
					token.type == Token.TIMEZONE || token.type == Token.TIMEZONE_OFFSET) {
				if (digitsMin != 0) {
					ret.append(digitsRegExp(digitsMin, digitsMax));
					digitsMin = digitsMax = 0;
				}
				switch (token.type) {
				case CONSTANT_CHAR:
					// Cope with the Character being a '.' which needs to be sloshed as an RE
					if (token.value == '.')
						ret.append('\\');
					ret.append(token.value);
					break;

				case MONTH:
					ret.append(DateTimeParser.monthPattern);
					break;

				case MONTH_ABBR:
					ret.append("\\p{Alpha}{3}");
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
					switch (token.count) {
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
				switch (token.type) {
				case DAYS_1_OR_2:
				case DIGITS_1_OR_2:
				case MONTHS_1_OR_2:
				case HOURS_1_OR_2:
					digitsMin += 1;
					digitsMax += 2;
					break;

				case DAYS_2:
				case MONTHS_2:
				case YEARS_2:
				case HOURS_2:
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
					digitsMin += token.count;
					digitsMax += token.count;
					break;
				}
			}
		}

		if (digitsMin != 0)
			ret.append(digitsRegExp(digitsMin, digitsMax));

		return ret.toString();
	}

	/**
	 * Return a DateTimeFormatter representation of the DateTimeParserResult.
	 * @return A String representation using DateTimeFormatter semantics.
	 */
	public String getFormatString() {
		if (formatString != null)
			return formatString;

		String hours = hourLength == 1 ? "H" : "HH";
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
		String dateAnswer = "";
		if (dateElements != 0) {
			if (yearOffset == -1) {
				if (dayOffset != -1)
					dateAnswer = asDate(new char[] {'M', 'd', 'y'});
				else
					if (dayFirst != null)
						if (dayFirst)
							dateAnswer = asDate(new char[] {'d', 'M', 'y'});
						else
							dateAnswer = asDate(new char[] {'M', 'd', 'y'});
					else
						dateAnswer = asDate(new char[] {'?', '?', '?'});

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
					if (dayFirst != null)
						if (dayFirst)
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

		String separator = dateTimeSeparator == ' ' ? " " : "'T'";
		formatString = (timeFirst != null && timeFirst) ? timeAnswer + separator + dateAnswer + timeZone
				: dateAnswer + separator + timeAnswer + timeZone;

		return formatString;
	}
}
