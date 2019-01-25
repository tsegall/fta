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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Helper class to manage several of the fixed length date inputs.
 * Used to match from an internal normalized form to a Java DateTimeFormatter.
 *
 * Vector returned is:
 *   DayOffset DayLength MonthOffset MonthLength YearOffset YearLength
 *   Offsets can be positive from the start of the string, or negative from the end of the string
 *   Lengths can be positive which reflects true length or negative field separated
 *
 * Note: Currently the set of string supported does not depend on the Locale supplied.  Need to survey
 * additional languages to identify whether this is necessary or not.
 */
public class SimpleDateMatcher {

	private static Map<String, Map<String, SimpleFacts>> knownFacts = new HashMap<>();

	private static Map<String, SimpleFacts> getSimpleDataFacts(Locale locale) {
		final String languageTag = locale.toLanguageTag();

		// Check to see if we are already in the cache
		if (knownFacts.get(languageTag) != null)
			return knownFacts.get(languageTag);

		Set<SimpleFacts> matchers = new HashSet<>();

		matchers.add(new SimpleFacts("d{4} d{2} d{2}", "yyyy MM dd", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("d{4} d d{2}", "yyyy M dd", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("d{4} d{2} d", "yyyy MM d", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("d{4} d d", "yyyy M d", PatternInfo.Type.LOCALDATE));

		matchers.add(new SimpleFacts("d{2} a{3} d{4}", "dd MMM yyyy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("d a{3} d{4}", "d MMM yyyy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("d{2}-a{3}-d{4}", "dd-MMM-yyyy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("d-a{3}-d{4}", "d-MMM-yyyy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("d{2}/a{3}/d{4}", "dd/MMM/yyyy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("d/a{3}/d{4}", "d/MMM/yyyy", PatternInfo.Type.LOCALDATE));

		matchers.add(new SimpleFacts("d{2} a{4} d{4}", "dd MMMM yyyy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("d a{4} d{4}", "d MMMM yyyy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("d{2}-a{4}-d{4}", "dd-MMMM-yyyy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("d-a{4}-d{4}", "d-MMMM-yyyy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("d{2}/a{4}/d{4}", "dd/MMMM/yyyy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("d/a{4}/d{4}", "d/MMMM/yyyy", PatternInfo.Type.LOCALDATE));

		matchers.add(new SimpleFacts("d{2} a{3} d{2}", "dd MMM yy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("d a{3} d{2}", "d MMM yy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("d{2}-a{3}-d{2}", "dd-MMM-yy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("d-a{3}-d{2}", "d-MMM-yy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("d{2}/a{3}/d{2}", "dd/MMM/yy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("d/a{3}/d{2}", "d/MMM/yy", PatternInfo.Type.LOCALDATE));

		matchers.add(new SimpleFacts("a{3} d{2}, d{4}", "MMM dd',' yyyy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("a{3} d, d{4}", "MMM d',' yyyy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("a{3} d{2} d{4}", "MMM dd yyyy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("a{3} d d{4}", "MMM d yyyy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("a{3}-d{2}-d{4}", "MMM-dd-yyyy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("a{3}-d-d{4}", "MMM-d-yyyy", PatternInfo.Type.LOCALDATE));

		matchers.add(new SimpleFacts("a{4} d{2}, d{4}", "MMMM dd',' yyyy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("a{4} d, d{4}", "MMMM d',' yyyy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("a{4} d{2} d{4}", "MMMM dd yyyy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("a{4} d d{4}", "MMMM d yyyy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("a{4}-d{2}-d{4}", "MMMM-dd-yyyy", PatternInfo.Type.LOCALDATE));
		matchers.add(new SimpleFacts("a{4}-d-d{4}", "MMMM-d-yyyy", PatternInfo.Type.LOCALDATE));

		matchers.add(new SimpleFacts("d{8}Td{6}Z", "yyyyMMdd'T'HHmmss'Z'", PatternInfo.Type.LOCALDATETIME));
		matchers.add(new SimpleFacts("d{8}Td{6}", "yyyyMMdd'T'HHmmss", PatternInfo.Type.LOCALDATETIME));
		matchers.add(new SimpleFacts("d{8}Td{6}+d{4}", "yyyyMMdd'T'HHmmssxx", PatternInfo.Type.OFFSETDATETIME));
		matchers.add(new SimpleFacts("d{8}Td{6}-d{4}", "yyyyMMdd'T'HHmmssxx", PatternInfo.Type.OFFSETDATETIME));
		matchers.add(new SimpleFacts("d{8}Td{6}.d{3}+d{4}", "yyyyMMdd'T'HHmmss.SSSxx", PatternInfo.Type.OFFSETDATETIME));
		matchers.add(new SimpleFacts("d{8}Td{6}.d{3}-d{4}", "yyyyMMdd'T'HHmmss.SSSxx", PatternInfo.Type.OFFSETDATETIME));

		matchers.add(new SimpleFacts("d{2}/a{3}/d{2} d:d{2} P", "dd/MMM/yy h:mm a", PatternInfo.Type.LOCALDATETIME));
		matchers.add(new SimpleFacts("d{2}/a{3}/d{2} d{2}:d{2} P", "dd/MMM/yy hh:mm a", PatternInfo.Type.LOCALDATETIME));

		matchers.add(new SimpleFacts("a{3} a{3} d{2} d{2}:d{2}:d{2} a{3} d{4}", "EEE MMM dd HH:mm:ss z yyyy", PatternInfo.Type.ZONEDDATETIME));

		HashMap<String, SimpleFacts> localeMatcher = new HashMap<>();
		for (SimpleFacts sdm : matchers) {
			localeMatcher.put(sdm.getMatcher(), sdm);
			localeMatcher.put(sdm.getFormat(), sdm);
		}

		knownFacts.put(languageTag, localeMatcher);

		return localeMatcher;
	}

	public static PatternInfo.Type getType(String pattern, Locale locale) {
		Map<String, SimpleFacts> sfMap = getSimpleDataFacts(locale);
		if (sfMap == null)
			return null;

		SimpleFacts sf = sfMap.get(pattern);
		if (sf == null)
			return null;

		return sf.type;
	}

	private static String replaceString(String input, int len, String target, String replacement, boolean wordBoundary) {
		int found = input.indexOf(target);
		if (found == -1)
			return null;

		char ch;
		if (found != 0) {
			ch = input.charAt(found - 1);
			if (Character.isAlphabetic(ch) || (wordBoundary && ch != ' '))
				return null;
		}
		if (found + target.length() < len) {
			ch = input.charAt(found + target.length());
			if (Character.isAlphabetic(ch) || (wordBoundary && ch != ' '))
				return null;
		}

		return input.replaceFirst(target, replacement);
	}

	/**
	 * 'Compress' the input string (which we think represents a date) so that it can be matched in the list of predefined formats.
	 * Mapping:
	 * <ul>
	 * <li>Trailing AM/PM replaced with P</li>
	 * <li>Strings of digits replaced by d{n} for n &gt; 1, or d for n = 1</li>
	 * <li>Strings of alphas replaced by a{4} for n &gt; 4, a{n} for n &gt; 1 and n &lt; 4, or a for n = 1</li>
	 * </ul>
	 * @param input The input string to be matched
	 * @param locale The Locale the date String is in
	 * @return The compressed representation
	 */
	public static String compress(String input, Locale locale) {
		final StringBuilder result = new StringBuilder();
		int len = input.length();
		char lastCh = '=';
		int count = 0;
		boolean amIndicator = false;

		input = input.toUpperCase(locale);

		for (String s : LocaleInfo.getAMPMStrings(locale)) {
			if (input.endsWith(s)) {
				input = input.substring(0, input.lastIndexOf(s));
				len = input.length();
				amIndicator = true;
				break;
			}
		}

		String replaced = null;
		for (String shortMonth : LocaleInfo.getShortMonths(locale).keySet()) {
			replaced = replaceString(input, len, shortMonth, "aaa", false);
			if (replaced != null) {
				input = replaced;
				len = input.length();
				break;
			}
		}

		if (replaced == null)
			for (String month : LocaleInfo.getMonths(locale).keySet()) {
				replaced = replaceString(input, len, month, "aaaa", false);
				if (replaced != null) {
					input = replaced;
					len = input.length();
					break;
				}
			}

		for (String weekday : LocaleInfo.getShortWeekdays(locale)) {
			replaced = replaceString(input, len, weekday, "aaa", true);
			if (replaced != null) {
				input = replaced;
				len = input.length();
				break;
			}
		}

		for (int i = 0; i < len; i++) {
			final char ch = input.charAt(i);
			if (Character.isDigit(ch)) {
				if (Character.isDigit(lastCh))
					count++;
				else {
					if (count != 0) {
						result.append("{" + String.valueOf(count + 1) + "}");
						count = 0;
					}
					result.append('d');
					lastCh = ch;
				}
			}
			else if (Character.isAlphabetic(ch)) {
				if (Character.isAlphabetic(lastCh)) {
					if (count < 3)
						count++;
				}
				else {
					if (count != 0) {
						result.append("{" + String.valueOf(count + 1) + "}");
						count = 0;
					}
					// If we have a standalone 'T' or 'Z' then preserve it
					if ((i + 1 == len || (i + 1 < len && !Character.isAlphabetic(input.charAt(i+1)))) && (ch == 'T' || ch == 'Z'))
						result.append(ch);
					else
						result.append('a');
					lastCh = ch;
				}
			}
			else {
				if (count != 0) {
					result.append("{" + String.valueOf(count + 1) + "}");
					count = 0;
				}
				result.append(ch);
				lastCh = ch;
			}
		}
		if (count != 0) {
			result.append("{" + String.valueOf(count + 1) + "}");
			count = 0;
		}

		if (amIndicator)
			result.append('P');

		return result.toString();
	}

	private SimpleFacts simpleFacts;
	private String input;
	private String compressed;
	private int componentCount;
	private Locale locale;
	private int dayOfMonth = -1;
	private int dayLength = -1;
	private int monthValue = -1;
	private int monthLength = -1;
	private int year = -1;
	private int yearLength = -1;

	public PatternInfo.Type getType() {
		return simpleFacts.type;
	}

	public String getFormat() {
		return simpleFacts.getFormat();
	}

	public String getCompressed() {
		return compressed;
	}

	public int getComponentCount() {
		return componentCount;
	}

	public int getDayOfMonth() {
		return dayOfMonth;
	}

	public int getMonthValue() {
		return monthValue;
	}

	public int getYear() {
		return year;
	}

	public int getDayLength() {
		return dayLength;
	}

	public int getMonthLength() {
		return monthLength;
	}

	public int getYearLength() {
		return yearLength;
	}

	public boolean isKnown() {
		return simpleFacts != null;
	}

	public boolean parse() {

		StringBuilder eating = new StringBuilder(input.toUpperCase(locale));
		boolean found = false;

		for (final FormatterToken token : DateTimeParserResult.tokenize(getFormat())) {
			switch (token.getType()) {
			case CONSTANT_CHAR:
				if (eating.length() == 0 || eating.charAt(0) != token.getValue())
					return false;
				eating.deleteCharAt(0);
				break;

			case MONTH:
				found = false;
				for (String month : LocaleInfo.getMonths(locale).keySet())
					if (eating.indexOf(month) == 0) {
						monthValue = DateTimeParser.monthOffset(month, locale);
						eating.delete(0, month.length());
						found = true;
						break;
					}
				if (!found)
					return false;
				break;

			case DAY_OF_WEEK:
				// Not implemented
				return false;

			case DAY_OF_WEEK_ABBR:
				found = false;
				for (String weekday : LocaleInfo.getShortWeekdays(locale)) {
					if (eating.indexOf(weekday) == 0) {
						eating.delete(0, weekday.length());
						found = true;
						break;
					}
				}
				if (!found)
					return false;
				break;

			case MONTH_ABBR:
				found = false;
				for (String shortMonth : LocaleInfo.getShortMonths(locale).keySet())
					if (eating.indexOf(shortMonth) == 0) {
						monthValue = DateTimeParser.shortMonthOffset(shortMonth, locale);
						eating.delete(0, shortMonth.length());
						found = true;
						break;
					}
				if (!found)
					return false;
				break;

			case AMPM:
				found = false;
				for (String s : LocaleInfo.getAMPMStrings(locale)) {
					if (eating.indexOf(s) == 0) {
						eating.delete(0, s.length());
						found = true;
						break;
					}
				}
				if (!found)
					return false;
				break;

			case TIMEZONE:
				while (eating.length() > 0 && eating.charAt(0) != ' ')
					eating.deleteCharAt(0);
				break;

			case TIMEZONE_OFFSET:
	//			Offset X and x: This formats the offset based on the number of pattern letters.
	//		    One letter outputs just the hour, such as '+01', unless the minute is non-zero in which case the minute is also output, such as '+0130'.
	//			Two letters outputs the hour and minute, without a colon, such as '+0130'.
	//			Three letters outputs the hour and minute, with a colon, such as '+01:30'.
	//			Four letters outputs the hour and minute and optional second, without a colon, such as '+013015'.
	//			Five letters outputs the hour and minute and optional second, with a colon, such as '+01:30:15'.
	//			Six or more letters throws IllegalArgumentException. Pattern letter 'X' (upper case) will output 'Z' when the offset to be output would be zero, whereas pattern letter 'x' (lower case) will output '+00', '+0000', or '+00:00'.
				switch (token.getCount()) {
				case 1:
					// +DD[DD]
					if (eating.length() < 3)
						return false;
					eating.delete(0, 3);
					if (eating.length() >= 2 && Character.isDigit(eating.charAt(0)))
						eating.delete(0, 2);
					break;

				case 2:
					// +DDDD
					if (eating.length() < 5)
						return false;
					eating.delete(0, 5);
					break;

				case 3:
					// +DD:DD
					if (eating.length() < 6)
						return false;
					eating.delete(0, 6);
					break;

				case 4:
					// +DDDD[DD]
					if (eating.length() < 5)
						return false;
					eating.delete(0, 5);
					if (eating.length() >= 2 && Character.isDigit(eating.charAt(0)))
						eating.delete(0, 2);
					break;

				case 5:
					// +DD:DD[:DD]
					if (eating.length() < 6)
						return false;
					eating.delete(0, 6);
					if (eating.length() >= 3 && eating.charAt(0) == ':')
						eating.delete(0, 3);
					break;
				}
				break;

			case YEARS_4:
				if (eating.length() < 4)
					return false;
				year = Utils.getValue(eating.toString(), 0, 4, 4);
				yearLength = 4;
				eating.delete(0, 4);
				break;

			case YEARS_2:
				if (eating.length() < 2)
					return false;
				year = Utils.getValue(eating.toString(), 0, 2, 2);
				yearLength = 2;
				eating.delete(0, 2);
				break;

			case DAYS_1_OR_2:
				if (eating.length() == 0)
					return false;
				if (eating.length() == 1 || !Character.isDigit(eating.charAt(1))) {
					dayOfMonth = Utils.getValue(eating.toString(), 0, 1, 1);
					dayLength = 1;
					eating.delete(0, 1);
					break;
				}
				// FALL THROUGH
			case DAYS_2:
				if (eating.length() < 2)
					return false;
				dayOfMonth = Utils.getValue(eating.toString(), 0, 2, 2);
				dayLength = 2;
				eating.delete(0, 2);
				break;

			case MONTHS_1_OR_2:
				if (eating.length() == 0)
					return false;
				if (eating.length() == 1 || !Character.isDigit(eating.charAt(1))) {
					monthValue = Utils.getValue(eating.toString(), 0, 1, 1);
					monthLength = 1;
					eating.delete(0, 1);
					break;
				}
				// FALL THROUGH
			case MONTHS_2:
				if (eating.length() < 2)
					return false;
				monthValue = Utils.getValue(eating.toString(), 0, 2, 2);
				monthLength = 2;
				eating.delete(0, 2);
				break;

			case DIGITS_1_OR_2:
			case HOURS12_1_OR_2:
			case HOURS24_1_OR_2:
				if (eating.length() == 0)
					return false;
				if (eating.length() == 1 || !Character.isDigit(eating.charAt(1)))
					eating.delete(0, 1);
				else
					eating.delete(0, 2);
				break;

			case HOURS12_2:
			case HOURS24_2:
			case MINS_2:
			case SECS_2:
			case DIGITS_2:
				if (eating.length() < 2)
					return false;
				eating.delete(0, 2);
				break;

			case FRACTION:
				if (eating.length() < token.getCount())
					return false;
				eating.delete(0, token.getCount());
				break;
			}
		}

		return eating.length() == 0 ? true : false;
	}

	private int countComponents(String compressed) {
		int len = compressed.length();
		int result = 0;

		for (int i = 0; i < len; i++) {
			char ch = compressed.charAt(i);
			if (ch == 'a' || ch == 'd') {
				result++;
				if (i + 1 == len)
					return result;
				i++;
				ch = compressed.charAt(i);
				if (ch == '{') {
					i++;
					do {
						i++;
					} while (compressed.charAt(i) != '}');
				}
			}
		}
		return result;
	}

	/**
	 * Construct the SimpleDateMatcher for this input (if possible).
	 * @param input The input to be parsed
	 * @param locale The Locale we are currently processing
	 */
	public SimpleDateMatcher(String input, Locale locale) {
		this.input = input;
		this.compressed = compress(input, locale);
		this.componentCount = countComponents(compressed);
		this.locale = locale;
		simpleFacts = getSimpleDataFacts(locale).get(compressed);
	}
}
