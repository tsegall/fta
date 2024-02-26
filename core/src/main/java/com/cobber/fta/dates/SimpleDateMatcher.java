/*
 * Copyright 2017-2024 Tim Segall
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.Utils;
import com.cobber.fta.token.Token;

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

	private static Map<String, SimpleFacts> knownFacts;

	private final String input;
	private final String compressed;
	private final int componentCount;
	private final LocaleInfo localeInfo;
	private int dayOfMonth = -1;
	private int dayLength = -1;
	private int monthValue = -1;
	private int monthLength = -1;
	private int year = -1;
	private int yearLength = -1;

	public static Map<String, SimpleFacts> getSimpleDataFacts() {

		if (knownFacts != null)
			return knownFacts;

		final Set<SimpleFacts> matchers = new HashSet<>();

		matchers.add(new SimpleFacts("d{4} d{2} d{2}", "yyyy MM dd", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("d{4} d d{2}", "yyyy M dd", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("d{4} d{2} d", "yyyy MM d", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("d{4} d d", "yyyy M d", FTAType.LOCALDATE));

		matchers.add(new SimpleFacts("d{2} MMM d{4}", "dd MMM yyyy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("d MMM d{4}", "d MMM yyyy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("d{2}-MMM-d{4}", "dd-MMM-yyyy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("d-MMM-d{4}", "d-MMM-yyyy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("d{2}/MMM/d{4}", "dd/MMM/yyyy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("d/MMM/d{4}", "d/MMM/yyyy", FTAType.LOCALDATE));

		matchers.add(new SimpleFacts("d{2} MMMM d{4}", "dd MMMM yyyy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("d MMMM d{4}", "d MMMM yyyy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("d{2}-MMMM-d{4}", "dd-MMMM-yyyy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("d-MMMM-d{4}", "d-MMMM-yyyy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("d{2}/MMMM/d{4}", "dd/MMMM/yyyy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("d/MMMM/d{4}", "d/MMMM/yyyy", FTAType.LOCALDATE));

		matchers.add(new SimpleFacts("d{2} MMM d{2}", "dd MMM yy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("d MMM d{2}", "d MMM yy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("d{2}-MMM-d{2}", "dd-MMM-yy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("d-MMM-d{2}", "d-MMM-yy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("d{2}/MMM/d{2}", "dd/MMM/yy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("d/MMM/d{2}", "d/MMM/yy", FTAType.LOCALDATE));

		matchers.add(new SimpleFacts("MMM d{2}, d{4}", "MMM dd, yyyy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("MMM d, d{4}", "MMM d, yyyy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("MMM d{2} d{4}", "MMM dd yyyy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("MMM d d{4}", "MMM d yyyy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("MMM-d{2}-d{4}", "MMM-dd-yyyy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("MMM-d-d{4}", "MMM-d-yyyy", FTAType.LOCALDATE));

		matchers.add(new SimpleFacts("MMMM d{2}, d{4}", "MMMM dd, yyyy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("MMMM d, d{4}", "MMMM d, yyyy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("MMMM d{2} d{4}", "MMMM dd yyyy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("MMMM d d{4}", "MMMM d yyyy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("MMMM-d{2}-d{4}", "MMMM-dd-yyyy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("MMMM-d-d{4}", "MMMM-d-yyyy", FTAType.LOCALDATE));

		matchers.add(new SimpleFacts("MMM d d{4} d{2}:d{2}:d{2} P", "MMM d yyyy hh:mm:ss a", FTAType.LOCALDATETIME));
		matchers.add(new SimpleFacts("MMM d{2} d{4} d{2}:d{2}:d{2} P", "MMM dd yyyy hh:mm:ss a", FTAType.LOCALDATETIME));
		matchers.add(new SimpleFacts("MMMM d d{4} d{2}:d{2}:d{2} P", "MMMM d yyyy hh:mm:ss a", FTAType.LOCALDATETIME));
		matchers.add(new SimpleFacts("MMMM d{2} d{4} d{2}:d{2}:d{2} P", "MMMM dd yyyy hh:mm:ss a", FTAType.LOCALDATETIME));

		matchers.add(new SimpleFacts("d{8}Td{2}Z", "yyyyMMdd'T'HH'Z'", FTAType.LOCALDATETIME));
		matchers.add(new SimpleFacts("d{8}Td{2}", "yyyyMMdd'T'HH", FTAType.LOCALDATETIME));
		matchers.add(new SimpleFacts("d{4}-d{2}-d{2}Td{2}", "yyyy-MM-dd'T'HH", FTAType.LOCALDATETIME));
		matchers.add(new SimpleFacts("d{8}Td{4}Z", "yyyyMMdd'T'HHmm'Z'", FTAType.LOCALDATETIME));
		matchers.add(new SimpleFacts("d{8}Td{4}", "yyyyMMdd'T'HHmm", FTAType.LOCALDATETIME));
		matchers.add(new SimpleFacts("d{4}-d{2}-d{2}Td{2}:d{2}", "yyyy-MM-dd'T'HH:mm", FTAType.LOCALDATETIME));
		matchers.add(new SimpleFacts("d{8}Td{6}Z", "yyyyMMdd'T'HHmmss'Z'", FTAType.LOCALDATETIME));
		matchers.add(new SimpleFacts("d{8}Td{6}", "yyyyMMdd'T'HHmmss", FTAType.LOCALDATETIME));
		matchers.add(new SimpleFacts("d{4}-d{2}-d{2}Td{2}:d{2}:d{2}", "yyyy-MM-dd'T'HH:mm:ss", FTAType.LOCALDATETIME));
		matchers.add(new SimpleFacts("d{8}Td{7}Z", "yyyyMMdd'T'HHmmssS'Z'", FTAType.LOCALDATETIME));
		matchers.add(new SimpleFacts("d{8}Td{7}", "yyyyMMdd'T'HHmmssS", FTAType.LOCALDATETIME));
		matchers.add(new SimpleFacts("d{8}Td{6}+d{4}", "yyyyMMdd'T'HHmmssxx", FTAType.OFFSETDATETIME));
		matchers.add(new SimpleFacts("d{8}Td{6}-d{4}", "yyyyMMdd'T'HHmmssxx", FTAType.OFFSETDATETIME));
		matchers.add(new SimpleFacts("d{8}Td{6}.d{3}+d{4}", "yyyyMMdd'T'HHmmss.SSSxx", FTAType.OFFSETDATETIME));
		matchers.add(new SimpleFacts("d{8}Td{6}.d{3}-d{4}", "yyyyMMdd'T'HHmmss.SSSxx", FTAType.OFFSETDATETIME));

		matchers.add(new SimpleFacts("d{2}/MMM/d{2} d:d{2} P", "dd/MMM/yy h:mm a", FTAType.LOCALDATETIME));
		matchers.add(new SimpleFacts("d{2}/MMM/d{2} d{2}:d{2} P", "dd/MMM/yy hh:mm a", FTAType.LOCALDATETIME));

		matchers.add(new SimpleFacts("EEEE, MMM, d{2}, d{4}", "EEEE, MMM, dd, yyyy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("EEEE, MMMM, d{2}, d{4}", "EEEE, MMMM, dd, yyyy", FTAType.LOCALDATE));
		matchers.add(new SimpleFacts("EEE MMM d{2} d{2}:d{2}:d{2} z d{4}", "EEE MMM dd HH:mm:ss z yyyy", FTAType.ZONEDDATETIME));
		matchers.add(new SimpleFacts("EEE MMM  d d{2}:d{2}:d{2} z d{4}", "EEE MMM ppd HH:mm:ss z yyyy", FTAType.ZONEDDATETIME));
		matchers.add(new SimpleFacts("EEE MMM d{2} d{2}:d{2}:d{2} O d{4}", "EEE MMM dd HH:mm:ss O yyyy", FTAType.OFFSETDATETIME));
		matchers.add(new SimpleFacts("EEE MMM d{2} d{2}:d{2}:d{2} OOOO d{4}", "EEE MMM dd HH:mm:ss OOOO yyyy", FTAType.OFFSETDATETIME));
		matchers.add(new SimpleFacts("EEE d{2} MMM d{4} d{2}:d{2}:d{2} +d{4}", "EEE dd MMM yyyy HH:mm:ss x", FTAType.OFFSETDATETIME));

		final Map<String, SimpleFacts> knownFacts = new HashMap<>();
		for (final SimpleFacts sdm : matchers) {
			knownFacts.put(sdm.getMatcher(), sdm);
			knownFacts.put(sdm.getFormat(), sdm);
		}

		return knownFacts;
	}

	public static FTAType getType(final String pattern) {
		final Map<String, SimpleFacts> sfMap = getSimpleDataFacts();
		if (sfMap == null)
			return null;

		final SimpleFacts sf = sfMap.get(pattern);
		if (sf == null)
			return null;

		return sf.getType();
	}

	/**
	 * Replace an isolated occurrence of the 'target' string with the 'replacement' string.
	 */
	private static String replaceString(final String input, final int len, final String target, final String replacement) {
		int startOffset = 0;
		int found;

		while ((found = input.indexOf(target, startOffset)) != -1) {
			startOffset = found + 1;
			char ch;
			if (found != 0) {
				ch = input.charAt(found - 1);
				if (Character.isAlphabetic(ch))
					continue;
			}
			if (found + target.length() < len) {
				ch = input.charAt(found + target.length());
				if (Character.isAlphabetic(ch))
					continue;
			}

			return input.substring(0, found) + replacement + input.substring(found + target.length());
		}

		return null;
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
	 * @param localeInfo The details for the Locale the date String is in
	 * @return The compressed representation
	 */
	public static String compress(String input, final LocaleInfo localeInfo) {
		final StringBuilder result = new StringBuilder();
		int len = input.length();
		char lastCh = '=';
		int count = 0;
		boolean amIndicator = false;

		input = input.toUpperCase(localeInfo.getLocale());

		for (final String s : localeInfo.getAMPMStrings()) {
			if (input.endsWith(s)) {
				input = input.substring(0, input.lastIndexOf(s));
				len = input.length();
				amIndicator = true;
				break;
			}
		}

		String replaced;

		boolean monthReplaced = false;
		// We want to replace the maximal string possible whether that be the short or long version of the month
		// If they are equal then assume it is the short month, it will be promoted later if we end up
		// with enough training samples to make a determination
		for (final String month : localeInfo.getMonths().keySet()) {
			if (input.indexOf(month) != -1) {
				for (final String shortMonth : localeInfo.getShortMonths().keySet()) {
					if (input.indexOf(shortMonth) != -1) {
						if (shortMonth.length() >= month.length())
							replaced = replaceString(input, len, shortMonth, "MMM");
						else
							replaced = replaceString(input, len, month, "MMMM");
						// We found a month string but it may be an embedded string in which case we did not replace it
						if (replaced != null) {
							monthReplaced = true;
							input = replaced;
							len = input.length();
						}
						break;
					}
				}
				// We found the month and no corresponding abbreviation so use the month
				if (!monthReplaced) {
					replaced = replaceString(input, len, month, "MMMM");
					if (replaced != null) {
						monthReplaced = true;
						input = replaced;
						len = input.length();
					}
				}
				if (monthReplaced)
					break;
			}
		}
		// We did not find the month so try a month abbreviation
		if (!monthReplaced)
			for (final String shortMonth : localeInfo.getShortMonths().keySet()) {
				replaced = replaceString(input, len, shortMonth, "MMM");
				if (replaced != null) {
					input = replaced;
					len = input.length();
					break;
				}
			}

		// Some locales have weekday abbreviations that overlap with month abbreviations, given this and our very limited
		// support for EEE, we will only replace weekday abbreviations if at the start of the string and it is long enough
		// to be of the form 'EEE MMM d{2} d{2}:d{2}:d{2} z d{4}'
		for (final String weekday : localeInfo.getShortWeekdays()) {
			if (len >= 22 && input.startsWith(weekday)) {
				replaced = replaceString(input, len, weekday, "EEE");
				if (replaced != null) {
					input = replaced;
					len = input.length();
					break;
				}
			}
		}

		// Some locales (e.g. br_FR Breton) have weekdays that overlap with months.
		for (final String weekday : localeInfo.getWeekdays()) {
			if (input.startsWith(weekday)) {
				replaced = replaceString(input, len, weekday, "EEEE");
				if (replaced != null) {
					input = replaced;
					len = input.length();
					break;
				}
			}
		}

		final String[] words = input.split(" ");
		for (final String word : words) {
			if (DateTimeParser.timeZones.contains(word)) {
				input = replaceString(input, len, word, "z");
				len = input.length();
				break;
			}
			else {
				// Look for a localized zone-offset (O), something of the form GMT+8 or GMT+08:00 or GMT+08:00:00
				if (word.startsWith("GMT+") || word.startsWith("GMT-")) {
					final String balance = word.substring(4);
					final int offsetLength = balance.length();
					if (offsetLength == 0)
						break;
					final String smashed = Token.generateKey(balance);
					if ("9".equals(smashed) || "99".equals(smashed) || "9:99".equals(smashed)) {
						input = replaceString(input, len, word, "O");
						len = input.length();
						break;
					}
					else if ("99:99".equals(smashed) || "99:99:99".equals(smashed)) {
						input = replaceString(input, len, word, "OOOO");
						len = input.length();
						break;
					}
				}
			}
		}

		for (int i = 0; i < len; i++) {
			final char ch = input.charAt(i);
			if (Character.isDigit(ch)) {
				if (Character.isDigit(lastCh))
					count++;
				else {
					if (count != 0) {
						result.append('{').append(count + 1).append('}');
						count = 0;
					}
					result.append('d');
					lastCh = ch;
				}
			}
			else if (Character.isAlphabetic(ch)) {
				if (!Character.isAlphabetic(lastCh)) {
					if (count != 0) {
						result.append('{').append(count + 1).append('}');
						count = 0;
					}
					lastCh = ch;
				}
				result.append(ch);
			}
			else {
				if (count != 0) {
					result.append('{').append(count + 1).append('}');
					count = 0;
				}
				result.append(ch);
				lastCh = ch;
			}
		}

		if (count != 0)
			result.append('{').append(count + 1).append('}');

		if (amIndicator)
			result.append('P');

		return result.toString();
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

	public boolean parse(final String format) {

		final StringBuilder eating = new StringBuilder(input.toUpperCase(localeInfo.getLocale()));
		boolean found;

		for (final FormatterToken token : TokenList.getTokenList(format)) {
			switch (token.getType()) {
			case QUOTE:
				break;

			case CONSTANT_CHAR:
				if (eating.length() == 0 || eating.charAt(0) != token.getValue())
					return false;
				eating.deleteCharAt(0);
				break;

			case MONTH:
				found = false;
				for (final String month : localeInfo.getMonths().keySet())
					if (eating.indexOf(month) == 0) {
						monthValue = localeInfo.monthOffset(month);
						eating.delete(0, month.length());
						found = true;
						break;
					}
				if (!found)
					return false;
				break;

			case DAY_OF_WEEK:
				found = false;
				for (final String weekday : localeInfo.getWeekdays()) {
					if (eating.indexOf(weekday) == 0) {
						eating.delete(0, weekday.length());
						found = true;
						break;
					}
				}
				if (!found)
					return false;
				break;

			case DAY_OF_WEEK_ABBR:
				found = false;
				for (final String weekday : localeInfo.getShortWeekdays()) {
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
				for (final String shortMonth : localeInfo.getShortMonths().keySet())
					if (eating.indexOf(shortMonth) == 0) {
						monthValue = localeInfo.shortMonthOffset(shortMonth);
						eating.delete(0, shortMonth.length());
						found = true;
						break;
					}
				if (!found)
					return false;
				break;

			case AMPM:
			case AMPM_NL:
				final Set<String> indicators = token.getType() == DateTimeParserResult.Token.AMPM ? localeInfo.getAMPMStrings() : localeInfo.getAMPMStringsNonLocalized();
				found = false;
				for (final String s : indicators) {
					if (eating.indexOf(s) == 0) {
						eating.delete(0, s.length());
						found = true;
						break;
					}
				}
				if (!found)
					return false;
				break;

			case TIMEZONE_NAME:
			case LOCALIZED_TIMEZONE_OFFSET:
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

			case DAYS:
				if (eating.length() < token.getCount())
					return false;
				if (token.getCount() == 1) {
					if (eating.length() == 1 || !Character.isDigit(eating.charAt(1))) {
						dayOfMonth = Utils.getValue(eating.toString(), 0, 1, 1);
						dayLength = 1;
						eating.delete(0, 1);
						break;
					}
				}

				dayOfMonth = Utils.getValue(eating.toString(), 0, 2, 2);
				dayLength = 2;
				eating.delete(0, 2);
				break;

			case MONTHS:
				if (eating.length() < token.getCount())
					return false;
				if (token.getCount() == 1) {
					if (eating.length() == 1 || !Character.isDigit(eating.charAt(1))) {
						monthValue = Utils.getValue(eating.toString(), 0, 1, 1);
						monthLength = 1;
						eating.delete(0, 1);
						break;
					}
				}

				monthValue = Utils.getValue(eating.toString(), 0, 2, 2);
				monthLength = 2;
				eating.delete(0, 2);
				break;

			case CLOCK24:
			case DIGITS:
			case HOURS12:
			case HOURS24:
				if (eating.length() < token.getCount())
					return false;
				if (token.getCount() == 1) {
					if (eating.length() == 1 || !Character.isDigit(eating.charAt(1)))
						eating.delete(0, 1);
					else
						eating.delete(0, 2);
				}
				else
					eating.delete(0, 2);
				break;

			case MINS:
			case SECS:
				if (eating.length() < 2)
					return false;
				eating.delete(0, 2);
				break;

			case FRACTION:
				if (eating.length() < token.getCount())
					return false;
				eating.delete(0, token.getCount());
				for (int j = 1; eating.length() != 0 && Character.isDigit(eating.charAt(0)) && j < token.getHigh(); j++)
					eating.delete(0, 1);
				break;
			}
		}

		return eating.length() == 0;
	}

	private int countComponents(final String compressed) {
		final int len = compressed.length();
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
						if (i == len)
							return 0;
					} while (compressed.charAt(i) != '}');
				}
			}
		}

		if (compressed.indexOf("MMM") != -1)
			return result + 1;
		return result;
	}

	/**
	 * Construct the SimpleDateMatcher for this input (if possible).
	 * @param input The input to be parsed
	 * @param localeInfo The details for the Locale we are currently processing
	 */
	public SimpleDateMatcher(final String input, final LocaleInfo localeInfo) {
		this.input = input;
		this.compressed = compress(input, localeInfo);
		this.componentCount = countComponents(compressed);
		this.localeInfo = localeInfo;
	}
}
