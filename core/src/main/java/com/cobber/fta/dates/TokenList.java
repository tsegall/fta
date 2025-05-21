/*
 * Copyright 2017-2025 Tim Segall
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.cobber.fta.core.RegExpSplitter;
import com.cobber.fta.core.Utils;
import com.cobber.fta.dates.DateTimeParserResult.Token;
import com.cobber.fta.dates.FormatterToken.DateField;

public class TokenList implements Iterable<FormatterToken> {

	private static Map<String, TokenList> tokenListCache = new ConcurrentHashMap<>();

	private List<FormatterToken> tokens;

	private TokenList() {
	}

	private TokenList(final TokenList other) {
		tokens = new ArrayList<>(other.tokens);
	}

	public static TokenList getTokenList(final String formatString) {
		TokenList ret = tokenListCache.get(formatString);
		if (ret != null)
			return ret;

		ret = new TokenList();

		final List<FormatterToken> tokens = new ArrayList<>();

		final int formatLength = formatString.length();
		int fieldWidth = -1;

		for (int i = 0; i < formatLength; i++) {
			final char ch = formatString.charAt(i);
			final int newTokenOffset = i;
			switch (ch) {
			case 'E':
				if (i + 1 < formatLength && formatString.charAt(i + 1) == 'E') {
					i++;
					if (i + 1 < formatLength && formatString.charAt(i + 1) == 'E') {
						i++;
						if (i + 1 < formatLength && formatString.charAt(i + 1) == 'E') {
							i++;
							tokens.add(new FormatterToken(Token.DAY_OF_WEEK).withOffset(newTokenOffset));
						}
						else
							tokens.add(new FormatterToken(Token.DAY_OF_WEEK_ABBR).withOffset(newTokenOffset));
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
							tokens.add(new FormatterToken(Token.MONTH).withOffset(newTokenOffset));
						}
						else
							tokens.add(new FormatterToken(Token.MONTH_ABBR).withOffset(newTokenOffset));
					}
					else
						tokens.add(new FormatterToken(Token.MONTHS, 2).withOffset(newTokenOffset));
				} else
					tokens.add(new FormatterToken(Token.MONTHS, 1).withOffset(newTokenOffset));
				break;

			case 'd':
				if (i + 1 < formatLength && formatString.charAt(i + 1) == 'd') {
					i++;
					tokens.add(new FormatterToken(Token.DAYS, 2).withOffset(newTokenOffset));
				}
				else
					tokens.add(new FormatterToken(Token.DAYS, 1).withOffset(newTokenOffset).withFieldWidth(fieldWidth));
				fieldWidth = -1;
				break;

			case 'h':
				if (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					i++;
					tokens.add(new FormatterToken(Token.HOURS12, 2).withOffset(newTokenOffset));
				}
				else
					tokens.add(new FormatterToken(Token.HOURS12, 1).withOffset(newTokenOffset).withFieldWidth(fieldWidth));
				fieldWidth = -1;
				break;

			case 'H':
				if (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					i++;
					tokens.add(new FormatterToken(Token.HOURS24, 2).withOffset(newTokenOffset));
				}
				else
					tokens.add(new FormatterToken(Token.HOURS24, 1).withOffset(newTokenOffset).withFieldWidth(fieldWidth));
				fieldWidth = -1;
				break;

			case 'k':
				if (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					i++;
					tokens.add(new FormatterToken(Token.CLOCK24, 2).withOffset(newTokenOffset));
				}
				else
					tokens.add(new FormatterToken(Token.CLOCK24, 1).withOffset(newTokenOffset).withFieldWidth(fieldWidth));
				fieldWidth = -1;
				break;

			case '?':
				if (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					i++;
					tokens.add(new FormatterToken(Token.DIGITS, 2).withOffset(newTokenOffset));
				}
				else
					tokens.add(new FormatterToken(Token.DIGITS, 1).withOffset(newTokenOffset).withFieldWidth(fieldWidth));
				fieldWidth = -1;
				break;

			case 'm':
			case 's':
				tokens.add(new FormatterToken(ch == 'm' ? Token.MINS : Token.SECS).withOffset(newTokenOffset));
				i++;
				break;

			case 'O':
				int countO = 1;
				while (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					countO++;
					i++;
				}
				tokens.add(new FormatterToken(Token.LOCALIZED_TIMEZONE_OFFSET, countO).withOffset(newTokenOffset));
				break;

			case 'p':
				if (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					i++;
					fieldWidth = 2;
				}
				break;

			case 'y':
				i++;
				if (i + 1 < formatLength && formatString.charAt(i + 1) == 'y') {
					tokens.add(new FormatterToken(Token.YEARS_4, 4).withOffset(newTokenOffset));
					i += 2;
				} else
					tokens.add(new FormatterToken(Token.YEARS_2, 2).withOffset(newTokenOffset));
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
				tokens.add(new FormatterToken(Token.FRACTION, count, high).withOffset(newTokenOffset));
				break;

			case 'x':
			case 'X':
				int countX = 1;
				while (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					countX++;
					i++;
				}
				tokens.add(new FormatterToken(ch == 'x' ? Token.TIMEZONE_OFFSET : Token.TIMEZONE_OFFSET_ZERO, countX).withOffset(newTokenOffset));
				break;

			case 'z':
				tokens.add(new FormatterToken(Token.TIMEZONE_NAME).withOffset(newTokenOffset));
				break;

			case 'Z':
				int countZ = 1;
				while (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					countZ++;
					i++;
				}
				tokens.add(new FormatterToken(Token.TIMEZONE_OFFSET_Z, countZ).withOffset(newTokenOffset));
				break;

			case '\'':
				// Quotes in format strings are either '' which indicates a single quote or 'xyz' which indicates the constant string "xyz"
				if (i + 1 == formatLength)
					throw new DateTimeParseException("Unterminated quote in format String", formatString, i);
				if (formatString.charAt(i + 1) == '\'')
					tokens.add(new FormatterToken(Token.CONSTANT_CHAR, '\'').withOffset(newTokenOffset));
				else {
					tokens.add(new FormatterToken(Token.QUOTE).withOffset(newTokenOffset));
					do {
						tokens.add(new FormatterToken(Token.CONSTANT_CHAR, formatString.charAt(++i)).withOffset(i));
					} while (i + 1 < formatLength && formatString.charAt(i + 1) != '\'');
					tokens.add(new FormatterToken(Token.QUOTE).withOffset(i));
				}
				if (i + 1 == formatLength)
					throw new DateTimeParseException("Unterminated quote in format String", formatString, i);
				i++;
				break;

			case 'a':
				tokens.add(new FormatterToken(Token.AMPM).withOffset(newTokenOffset));
				break;

			case 'P':
				tokens.add(new FormatterToken(Token.AMPM_NL).withOffset(newTokenOffset));
				break;

			case 'G':
				int countG = 1;
				while (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					countG++;
					i++;
				}
				tokens.add(new FormatterToken(Token.ERA, countG).withOffset(newTokenOffset));
				break;

			default:
				// Strictly speaking (according to the spec) it is good practice to enclose random characters in quotes.
				// However, it is is not required - so if we do not recognize the character just assume it is a constant character.
				tokens.add(new FormatterToken(Token.CONSTANT_CHAR, ch).withOffset(newTokenOffset));
			}
		}

		ret.tokens = tokens;

		tokenListCache.put(formatString, ret);

		return ret;
	}

	public String getFormatString() {
		final StringBuilder ret = new StringBuilder();
		for (final FormatterToken token : tokens) {
			final Token nextToken = token.getType();

			switch (nextToken) {
			case CLOCK24:
			case DAYS:
			case HOURS12:
			case HOURS24:
			case MONTHS:
				ret.append(token.getRepresentation());
				break;
			case AMPM:
			case AMPM_NL:
			case DAY_OF_WEEK:
			case DAY_OF_WEEK_ABBR:
			case MINS:
			case MONTH:
			case MONTH_ABBR:
			case SECS:
			case TIMEZONE_NAME:
			case YEARS_2:
			case YEARS_4:
			case QUOTE:
				ret.append(nextToken.getRepresentation());
				break;
			case FRACTION:
				if (token.getHigh() != token.getCount())
					ret.append(nextToken.getRepresentation()).append('{').append(token.getCount()).append(',').append(token.getHigh()).append('}');
				else
					ret.append(Utils.repeat(nextToken.getRepresentation().charAt(0), token.getCount()));
				break;
			case LOCALIZED_TIMEZONE_OFFSET:
			case TIMEZONE_OFFSET:
			case TIMEZONE_OFFSET_ZERO:
				ret.append(Utils.repeat(nextToken.getRepresentation().charAt(0), token.getCount()));
				break;
			case CONSTANT_CHAR:
				ret.append(token.getValue());
				if (token.getValue() == '\'')
					ret.append(token.getValue());
				break;
			case DIGITS:
				ret.append(Utils.repeat('?', token.getCount()));
				break;
			default:
				break;
			}
		}

		return ret.toString();
	}

	public FormatterToken findByType(final Token type) {
		for (final FormatterToken token : tokens)
			if (type == token.getType())
				return token;
		return null;
	}

	public int findIndexByDateField(final DateField fieldType) {
		int unbound = 0;
		for (int i = 0; i < tokens.size(); i++) {
			final Token token = tokens.get(i).getType();
			if (token == Token.DIGITS)
				unbound++;

			switch (fieldType) {
			case Day:
				if (token == Token.DAYS)
					return i;
				break;
			case Month:
				if (token == Token.MONTHS)
					return i;
				break;
			case Year:
				if (token == Token.YEARS_2 || token == Token.YEARS_4)
					return i;
				break;
			case Hour:
				if (token == Token.HOURS12 || token == Token.HOURS24 || token == Token.CLOCK24)
					return i;
				break;
			case Minute:
				if (token == Token.MINS)
					return i;
				break;
			case Second:
				if (token == Token.SECS)
					return i;
				break;
			case Fraction:
				if (token == Token.FRACTION)
					return i;
				break;
			case Unbound1:
				if (unbound == 1)
					return i;
				break;
			case Unbound2:
				if (unbound == 2)
					return i;
				break;
			case Unbound3:
				if (unbound == 3)
					return i;
				break;
			}
		}
		return -1;
	}

	public FormatterToken findByDateField(final DateField fieldType) {
		final int index = findIndexByDateField(fieldType);
		return index == -1 ? null : tokens.get(index);
	}

	public int findIndexByOffset(final int offset) {
		for (int i = 0; i < tokens.size(); i++)
			if (tokens.get(i).getOffset() == offset)
				return i;

		return -1;
	}

	public FormatterToken findByOffset(final int offset) {
		final int index = findIndexByOffset(offset);
		return index == -1 ? null : tokens.get(index);
	}

	public FormatterToken get(final int index) {
		return tokens.get(index);
	}

	public TokenList update(final int index, final FormatterToken token) {
//		System.err.printf("Setting at index: %d, %s%n", index, token.getRepresentation());
		final TokenList newTokenList = new TokenList(this);
		newTokenList.tokens.set(index, token);
		return getTokenList(newTokenList.getFormatString());
	}

	class TokenIterator<F> implements Iterator<FormatterToken> {
		Iterator<FormatterToken> iter;

	    public TokenIterator(final List<FormatterToken> list) {
	    	iter = list.iterator();
	    }

	    @Override
		public boolean hasNext() {
	        return iter.hasNext();
	    }

	    @Override
		public FormatterToken next() {
	    	return iter.next();
	    }

	    @Override
		public void remove() {
	        throw new UnsupportedOperationException();
	    }
	}

	@Override
	public Iterator<FormatterToken> iterator() {
		return new TokenIterator<>(tokens);
	}
}
