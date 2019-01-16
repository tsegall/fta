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
	private static HashMap<String, HashMap<String, SimpleDateMatcher>> knownMatchers = new HashMap<String, HashMap<String, SimpleDateMatcher>>();

	private static HashMap<String, SimpleDateMatcher> getSimpleDataMatchers(Locale locale) {
		final String language = locale.getISO3Language();

		// Check to see if we are already in the cache
		if (knownMatchers.get(language) != null)
			return knownMatchers.get(language);

		Set<SimpleDateMatcher> matchers = new HashSet<SimpleDateMatcher>();

		matchers.add(new SimpleDateMatcher("d{4} d{2} d{2}", "yyyy MM dd", new int[] {8, 2, 5, 2, 0, 4}));

		matchers.add(new SimpleDateMatcher("d{4} d{2} d{2}", "yyyy MM dd", new int[] {8, 2, 5, 2, 0, 4}));
		matchers.add(new SimpleDateMatcher("d{4} d d{2}", "yyyy M dd", new int[] {7, 2, 5, 1, 0, 4}));
		matchers.add(new SimpleDateMatcher("d{4} d{2} d", "yyyy MM d", new int[] {8, 1, 5, 2, 0, 4}));
		matchers.add(new SimpleDateMatcher("d{4} d d", "yyyy M d", new int[] {7, 1, 5, 1, 0, 4}));

		matchers.add(new SimpleDateMatcher("d{2} a{3} d{4}", "dd MMM yyyy", new int[] {0, 2, 3, -2, -4, 4}));
		matchers.add(new SimpleDateMatcher("d a{3} d{4}", "d MMM yyyy", new int[] {0, 1, 2, -2, -4, 4}));
		matchers.add(new SimpleDateMatcher("d{2}-a{3}-d{4}", "dd-MMM-yyyy", new int[] {0, 2, 3, -2, -4, 4}));
		matchers.add(new SimpleDateMatcher("d-a{3}-d{4}", "d-MMM-yyyy", new int[] {0, 1, 2, -2, -4, 4}));
		matchers.add(new SimpleDateMatcher("d{2}/a{3}/d{4}", "dd/MMM/yyyy", new int[] {0, 2, 3, -2, -4, 4}));
		matchers.add(new SimpleDateMatcher("d/a{3}/d{4}", "d/MMM/yyyy", new int[] {0, 1, 2, -2, -4, 4}));

		matchers.add(new SimpleDateMatcher("d{2} a{4} d{4}", "dd MMMM yyyy", new int[] {0, 2, 3, -1, -4, 4}));
		matchers.add(new SimpleDateMatcher("d a{4} d{4}", "d MMMM yyyy", new int[] {0, 1, 2, -1, -4, 4}));
		matchers.add(new SimpleDateMatcher("d{2}-a{4}-d{4}", "dd-MMMM-yyyy", new int[] {0, 2, 3, -1, -4, 4}));
		matchers.add(new SimpleDateMatcher("d-a{4}-d{4}", "d-MMMM-yyyy", new int[] {0, 1, 2, -1, -4, 4}));
		matchers.add(new SimpleDateMatcher("d{2}/a{4}/d{4}", "dd/MMMM/yyyy", new int[] {0, 2, 3, -1, -4, 4}));
		matchers.add(new SimpleDateMatcher("d/a{4}/d{4}", "d/MMMM/yyyy", new int[] {0, 1, 2, -1, -4, 4}));

		matchers.add(new SimpleDateMatcher("d{2} a{3} d{2}", "dd MMM yy", new int[] {0, 2, 3, -2, -2, 2}));
		matchers.add(new SimpleDateMatcher("d a{3} d{2}", "d MMM yy", new int[] {0, 1, 2, -2, -2, 2}));
		matchers.add(new SimpleDateMatcher("d{2}-a{3}-d{2}", "dd-MMM-yy", new int[] {0, 2, 3, -2, -2, 2}));
		matchers.add(new SimpleDateMatcher("d-a{3}-d{2}", "d-MMM-yy", new int[] {0, 1, 2, -2, -2, 2}));
		matchers.add(new SimpleDateMatcher("d{2}/a{3}/d{2}", "dd/MMM/yy", new int[] {0, 2, 3, -2, -2, 2}));
		matchers.add(new SimpleDateMatcher("d/a{3}/d{2}", "d/MMM/yy", new int[] {0, 1, 2, -2, -2, 2}));

		matchers.add(new SimpleDateMatcher("a{3} d{2}, d{4}", "MMM dd',' yyyy", new int[] {4, 2, 0, -2, -4, 4}));
		matchers.add(new SimpleDateMatcher("a{3} d, d{4}", "MMM d',' yyyy", new int[] {4, 1, 0, -2, -4, 4}));
		matchers.add(new SimpleDateMatcher("a{3} d d{4}", "MMM dd yyyy", new int[] {4, 2, 0, -2, -4, 4}));
		matchers.add(new SimpleDateMatcher("a{3} d d{4}", "MMM d yyyy", new int[] {4, 1, 0, -2, -4, 4}));
		matchers.add(new SimpleDateMatcher("a{3}-d{2}-d{4}", "MMM-dd-yyyy", new int[] {4, 2, 0, -2, -4, 4}));
		matchers.add(new SimpleDateMatcher("a{3}-d-d{4}", "MMM-d-yyyy", new int[] {4, 1, 0, -2, -4, 4}));

		matchers.add(new SimpleDateMatcher("a{4} d{2}, d{4}", "MMMM dd',' yyyy", new int[] {-8, 2, 0, -1, -4, 4}));
		matchers.add(new SimpleDateMatcher("a{4} d, d{4}", "MMMM d',' yyyy", new int[] {-7, 1, 0, -1, -4, 4}));
		matchers.add(new SimpleDateMatcher("a{4} d{2} d{4}", "MMMM dd yyyy", new int[] {-7, 2, 0, -1, -4, 4}));
		matchers.add(new SimpleDateMatcher("a{4} d d{4}", "MMMM d yyyy", new int[] {-6, 1, 0, -1, -4, 4}));
		matchers.add(new SimpleDateMatcher("a{4}-d{2}-d{4}", "MMMM-dd-yyyy", new int[] {-7, 2, 0, -1, -4, 4}));
		matchers.add(new SimpleDateMatcher("a{4}-d-d{4}", "MMMM-d-yyyy", new int[] {-6, 1, 0, -1, -4, 4}));

		matchers.add(new SimpleDateMatcher("d{8}Td{6}Z", "yyyyMMdd'T'HHmmss'Z'", new int[] {6, 2, 4, 2, 0, 4}));
		matchers.add(new SimpleDateMatcher("d{8}Td{6}", "yyyyMMdd'T'HHmmss", new int[] {6, 2, 4, 2, 0, 4}));
		matchers.add(new SimpleDateMatcher("d{8}Td{6}+d{4}", "yyyyMMdd'T'HHmmssx", new int[] {6, 2, 4, 2, 0, 4}));
		matchers.add(new SimpleDateMatcher("d{8}Td{6}-d{4}", "yyyyMMdd'T'HHmmssx", new int[] {6, 2, 4, 2, 0, 4}));
		matchers.add(new SimpleDateMatcher("d{8}Td{6}.d{3}+d{4}", "yyyyMMdd'T'HHmmss.SSSx", new int[] {6, 2, 4, 2, 0, 4}));
		matchers.add(new SimpleDateMatcher("d{8}Td{6}.d{3}-d{4}", "yyyyMMdd'T'HHmmss.SSSx", new int[] {6, 2, 4, 2, 0, 4}));

		matchers.add(new SimpleDateMatcher("d{2}/a{3}/d{2} d:d{2} P", "dd/MMM/yy h:mm a", new int[] {0, 2, 3, -2, 7, 2}));
		matchers.add(new SimpleDateMatcher("d{2}/a{3}/d{2} d{2}:d{2} P", "dd/MMM/yy hh:mm a", new int[] {0, 2, 3, -2, 7, 2}));

		matchers.add(new SimpleDateMatcher("a{3} a{3} d{2} d{2}:d{2}:d{2} a{3} d{4}", "EEE MMM dd HH:mm:ss z yyyy", new int[] {8, 2, 4, -2, 24, 4}));

		HashMap<String, SimpleDateMatcher> localeMatcher = new HashMap<String, SimpleDateMatcher>();
		for (SimpleDateMatcher sdm : matchers) {
			localeMatcher.put(sdm.getMatcher(), sdm);
		}

		knownMatchers.put(language, localeMatcher);

		return localeMatcher;
	}

	private final String matcher;
	private final String format;
	private int dayOffset;
	private final int dayLength;
	private int monthOffset;
	private final int monthLength;
	private int yearOffset;
	private final int yearLength;

	SimpleDateMatcher(final String matcher, final String format, final int[] dateFacts) {
		this.matcher = matcher;
		this.format = format;
		this.dayOffset = dateFacts[0];
		this.dayLength = dateFacts[1];
		this.monthOffset = dateFacts[2];
		this.monthLength = dateFacts[3];
		this.yearOffset = dateFacts[4];
		this.yearLength = dateFacts[5];
	}

	String getMatcher() {
		return matcher;
	}

	public int getMonthOffset() {
		return monthOffset;
	}

	public int getMonthLength() {
		return monthLength;
	}

	public int getDayOffset() {
		return dayOffset;
	}

	public int getYearOffset() {
		return yearOffset;
	}

	public int getYearLength() {
		return yearLength;
	}

	public String getFormat() {
		return format;
	}

	public int getDayLength() {
		return dayLength;
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

		if (input.endsWith("AM") || input.endsWith("PM")) {
			len -= 2;
			amIndicator = true;
		}

		for (String monthAbbr : LocaleInfo.getMonthAbbrs(locale).keySet())
			if (input.contains(monthAbbr)) {
				input = input.replaceFirst(monthAbbr, "aaa");
				len -= monthAbbr.length() - 3;
				break;
			}

		for (String month : LocaleInfo.getMonths(locale).keySet())
			if (input.contains(month)) {
				input = input.replaceFirst(month, "aaaa");
				len -= month.length() - 4;
				break;
			}

		for (int i = 0; i < len; i++) {
			final char ch = input.charAt(i);
			if (Character.isDigit(ch)) {
				if (Character.isDigit(lastCh))
					count++;
				else {
					if (count != 0) {
						result.append("{" + count + "}");
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
					if ((i+1 == len || !Character.isAlphabetic(input.charAt(i+1))) && (ch == 'T' || ch == 'Z'))
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

	/**
	 * Retrieve the SimpleDateMatcher for this compressed pattern.
	 * @param pattern The compressed pattern as generated by {@link #compress(String, Locale) compress()} method.
	 * @param locale The Locale we are currently processing
	 * @return The SimpleDateMatcher found or null if no match.
	 */
	public static SimpleDateMatcher get(String pattern, Locale locale) {
		return getSimpleDataMatchers(locale).get(pattern);
	}
}
