/*
 * Copyright 2017-2022 Tim Segall
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

import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.cobber.fta.core.RegExpSplitter;
import com.cobber.fta.dates.DateTimeParserResult.Token;

class FormatterToken {
	private final Token type;
	private final int count;
	private final int high;
	private final char value;

	public Token getType() {
		return type;
	}

	public int getCount() {
		return count;
	}

	public int getHigh() {
		return high;
	}

	public char getValue() {
		return value;
	}

	FormatterToken(final Token type) {
		this.type = type;
		this.count = 0;
		this.high = 0;
		this.value = '\0';
	}

	FormatterToken(final Token type, final int count) {
		this.type = type;
		this.count = count;
		this.high = 0;
		this.value = '\0';
	}

	FormatterToken(final Token type, final int low, final int high) {
		this.type = type;
		this.count = low;
		this.high = high;
		this.value = '\0';
	}

	FormatterToken(final Token type, final char value) {
		this.type = type;
		this.count = 0;
		this.high = 0;
		this.value = value;
	}

	public static List<FormatterToken> tokenize(final String formatString) {
		final ArrayList<FormatterToken> ret = new ArrayList<>();

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

			case 'k':
				if (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					i++;
					ret.add(new FormatterToken(Token.CLOCK24_2));
				}
				else
					ret.add(new FormatterToken(Token.CLOCK24_1_OR_2));
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

			case 'O':
				int countO = 1;
				while (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					countO++;
					i++;
				}
				ret.add(new FormatterToken(Token.LOCALIZED_TIMEZONE_OFFSET, countO));
				break;

			case 'p':
				if (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					i++;
					ret.add(new FormatterToken(Token.PAD_2));
				}
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
				int count = 1;
				int high = 1;
				if (i + 1 < formatLength) {
					final char next = formatString.charAt(i + 1);
					if (next == 'S') {
						while (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
							count++;
							i++;
						}
						high = count;
					}
					else if (next == '{') {
						final RegExpSplitter facts = RegExpSplitter.newInstance(formatString.substring(i + 1));
						if (facts == null)
							return null;
						i += facts.getLength();
						count = facts.getMin();
						high = facts.getMax();
					}
				}
				ret.add(new FormatterToken(Token.FRACTION, count, high));
				break;

			case 'x':
			case 'X':
				int countX = 1;
				while (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					countX++;
					i++;
				}
				ret.add(new FormatterToken(ch == 'x' ? Token.TIMEZONE_OFFSET : Token.TIMEZONE_OFFSET_ZERO, countX));
				break;

			case 'z':
				ret.add(new FormatterToken(Token.TIMEZONE_NAME));
				break;

			case 'Z':
				int countZ = 1;
				while (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					countZ++;
					i++;
				}
				ret.add(new FormatterToken(Token.TIMEZONE_OFFSET_Z, countZ));
				break;

			case '\'':
				// Quotes in format strings are either '' which indicates a single quote or 'xyz' which indicates the constant string "xyz"
				if (i + 1 == formatLength)
					throw new DateTimeParseException("Unterminated quote in format String", formatString, i);
				if (formatString.charAt(i + 1) == '\'')
					ret.add(new FormatterToken(Token.CONSTANT_CHAR, '\''));
				else {
					ret.add(new FormatterToken(Token.QUOTE));
					do {
						ret.add(new FormatterToken(Token.CONSTANT_CHAR, formatString.charAt(++i)));
					} while (i + 1 < formatLength && formatString.charAt(i + 1) != '\'');
					ret.add(new FormatterToken(Token.QUOTE));
				}
				if (i + 1 == formatLength)
					throw new DateTimeParseException("Unterminated quote in format String", formatString, i);
				i++;
				break;

			case 'a':
				ret.add(new FormatterToken(Token.AMPM));
				break;

			case 'G':
				int countG = 1;
				while (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					countG++;
					i++;
				}
				ret.add(new FormatterToken(Token.ERA, countG));
				break;

			default:
				// Strictly speaking (according to the spec) it is good practice to enclose random characters in quotes.
				// However, it is is not required - so if we do not recognize the character just assume it is a constant character.
				ret.add(new FormatterToken(Token.CONSTANT_CHAR, ch));
			}
		}

		return ret;
	}
}
