/*
 * Copyright 2017-2020 Tim Segall
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

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/*
 * Helper class used to cache Month information across multiple languages.
 */
public class LocaleInfo {
	private static Map<String, Map<String, Integer>> months = new HashMap<>();
	private static Map<String, Map<String, Integer>> monthsDefault = new HashMap<>();
	private static Map<String, Map<String, Integer>> shortMonths = new HashMap<>();
	private static Map<String, Set<String>> weekdays = new HashMap<>();
	private static Map<String, Set<String>> shortWeekdays = new HashMap<>();
	private static Map<String, String> monthsRegExp = new HashMap<>();
	private static Map<String, Boolean> monthsAlphabetic = new HashMap<>();
	private static Map<String, String> shortMonthsRegExp = new HashMap<>();
	private static Map<String, Integer> shortMonthsLength = new HashMap<>();
	private static Map<String, Set<String>> ampmStrings = new HashMap<>();
	private static Map<String, String> ampmRegExp = new HashMap<>();

	private static Map<String, String> weekdaysRegExp = new HashMap<>();
	private static Map<String, Boolean> weekdaysAlphabetic	= new HashMap<>();
	private static Map<String, String> shortWeekdaysRegExp = new HashMap<>();
	private static Map<String, Boolean> shortWeekdaysAlphabetic	= new HashMap<>();

	private static LocaleInfo localeInfo = new LocaleInfo();

	class LengthComparator implements Comparator<String> {
		@Override
		public int compare(String str1, String str2) {
			int ret = str2.length() - str1.length();
			if (ret != 0)
				return ret;
			return str1.compareTo(str2);
		}
	}

	private static synchronized void cacheLocaleInfo(Locale locale) {
		final String languageTag = locale.toLanguageTag();

		// Check to see if we are already in the cache
		if (months.get(languageTag) != null)
			return;

		final GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance(locale);
		final int actualMonths = cal.getActualMaximum(Calendar.MONTH);

		DateFormatSymbols dfs = new DateFormatSymbols(locale);


		// Setup the Months
		final String[] longMonths = dfs.getMonths();
		Map<String, Integer> monthsLocale = new TreeMap<>(localeInfo.new LengthComparator());
		Map<String, Integer> monthsDefaultLocale = new TreeMap<>(localeInfo.new LengthComparator());
		RegExpGenerator generator = new RegExpGenerator();

		boolean isAllAlphabetic = true;
		for (int i = 0; i <= actualMonths; i++) {
			final String month = longMonths[i].toUpperCase(locale);
			if (isAllAlphabetic && !month.chars().allMatch(Character::isAlphabetic))
				isAllAlphabetic = false;
			monthsLocale.put(month, i + 1);
			monthsDefaultLocale.put(longMonths[i], i + 1);
			generator.train(longMonths[i]);
		}
		months.put(languageTag, monthsLocale);
		monthsDefault.put(languageTag, monthsDefaultLocale);
		monthsRegExp.put(languageTag, generator.getResult());
		monthsAlphabetic.put(languageTag, isAllAlphabetic);

		// Setup the Monthly abbreviations
		final String[] m = dfs.getShortMonths();
		TreeMap<String, Integer> shortMonthsLocale = new TreeMap<>(localeInfo.new LengthComparator());
		generator = new RegExpGenerator();

		for (int i = 0; i <= actualMonths; i++) {
			final String shortMonth = m[i].toUpperCase(locale);
			shortMonthsLocale.put(shortMonth, i + 1);
			generator.train(m[i]);
		}
		shortMonths.put(languageTag, shortMonthsLocale);
		shortMonthsRegExp.put(languageTag, generator.getResult());
		int len = shortMonthsLocale.firstKey().length();
		shortMonthsLength.put(languageTag, len == shortMonthsLocale.lastKey().length() ? len : -1);

		// Setup the AM/PM strings
		Set<String> ampmStringsLocale = new LinkedHashSet<>();
		String ampmRegExpLocale = "";
		for (String s : dfs.getAmPmStrings()) {
			if (ampmRegExpLocale.length() != 0)
				ampmRegExpLocale += "|";
			ampmRegExpLocale += s;
			ampmStringsLocale.add(s.toUpperCase(locale));
		}
		ampmStrings.put(languageTag, ampmStringsLocale);
		ampmRegExp.put(languageTag, "(?i)(" + ampmRegExpLocale + ")");

		// Setup the Week Day strings
		Set<String> weekdaysLocale = new TreeSet<>(localeInfo.new LengthComparator());
		generator = new RegExpGenerator();
		isAllAlphabetic = true;
		for (String week : dfs.getWeekdays()) {
			if (week.isEmpty())
				continue;
			if (isAllAlphabetic && !week.chars().allMatch(Character::isAlphabetic))
				isAllAlphabetic = false;
			final String weekUpper = week.toUpperCase(locale);
			weekdaysLocale.add(weekUpper);
			generator.train(week);
			generator.train(weekUpper);
		}
		weekdays.put(languageTag, weekdaysLocale);
		weekdaysRegExp.put(languageTag, generator.getResult());
		weekdaysAlphabetic.put(languageTag, isAllAlphabetic);

		// Setup the Short Week Day strings
		Set<String> shortWeekdaysLocale = new TreeSet<>(localeInfo.new LengthComparator());
		generator = new RegExpGenerator();
		isAllAlphabetic = true;
		for (String shortWeek : dfs.getShortWeekdays()) {
			if (shortWeek.isEmpty())
				continue;
			if (isAllAlphabetic && !shortWeek.chars().allMatch(Character::isAlphabetic))
				isAllAlphabetic = false;
			final String weekUpper = shortWeek.toUpperCase(locale);
			shortWeekdaysLocale.add(weekUpper);
			generator.train(shortWeek);
			generator.train(weekUpper);
		}
		shortWeekdays.put(languageTag, shortWeekdaysLocale);
		shortWeekdaysRegExp.put(languageTag, generator.getResult());
		shortWeekdaysAlphabetic.put(languageTag, isAllAlphabetic);
	}

	/**
	 * Retrieve the Map of Month name to month index for this Locale
	 * @param locale Locale we are interested in
	 * @return Map of Month name to month index for this Locale
	 */
	public static Map<String, Integer> getMonths(Locale locale) {
		cacheLocaleInfo(locale);
		return months.get(locale.toLanguageTag());
	}

	/**
	 * Retrieve the Map of Month name to month index for this Locale
	 * @param locale Locale we are interested in
	 * @return Map of Month name to month index for this Locale
	 */
	public static Map<String, Integer> getMonthsDefault(Locale locale) {
		cacheLocaleInfo(locale);
		return monthsDefault.get(locale.toLanguageTag());
	}

	/**
	 * Retrieve a Regular Expression for months in this Locale
	 * @param locale Locale we are interested in
	 * @return Regular Expression for months in this locale
	 */
	public static String getMonthsRegExp(Locale locale) {
		cacheLocaleInfo(locale);
		return monthsRegExp.get(locale.toLanguageTag());
	}

	public static int shortMonthOffset(final String month, Locale locale) {
		final Integer offset = LocaleInfo.getShortMonths(locale).get(month.toUpperCase(locale));
		return offset == null ? -1 : offset;
	}

	public static int monthOffset(final String month, Locale locale) {
		final Integer offset = LocaleInfo.getMonths(locale).get(month.toUpperCase(locale));
		return offset == null ? -1 : offset;
	}

	/**
	 * Are months all Alphabetic?
	 * @param locale Locale we are interested in
	 * @return True if all month strings are all Alphabetic
	 */
	public static boolean areMonthsAlphabetic(Locale locale) {
		cacheLocaleInfo(locale);
		return monthsAlphabetic.get(locale.toLanguageTag());
	}

	/**
	 * Check that the input starts with a valid month (case insensitive) and if so
	 * return the offset of the end of match.
	 * @param input The input string
	 * @param locale Locale we are interested in
	 * @return Offset of the end of the matched input, or -1 if no match
	 */
	public static int skipValidMonth(String input, Locale locale) {
		int upto = 0;
		int inputLength = input.length();
		boolean allAlphabetic = LocaleInfo.areMonthsAlphabetic(locale);

		if (allAlphabetic) {
			while (upto < inputLength && Character.isAlphabetic(input.charAt(upto)))
				upto++;
			final String month = input.substring(0, upto);
			if (monthOffset(month, locale) == -1)
				return -1;
			return upto;
		}

		// In some locales the length of a month.toLowerCase().length != month.toUpperCase().length
		// So try matching first with the name as default, then the Upper case version
		boolean found = false;
		for (String monthName : LocaleInfo.getMonthsDefault(locale).keySet()) {
			if (input.startsWith(monthName)) {
				found = true;
				upto += monthName.length();
				break;
			}
		}
		if (!found) {
			input = input.toUpperCase(locale);
			for (String monthName : LocaleInfo.getMonths(locale).keySet()) {
				if (input.startsWith(monthName)) {
					found = true;
					upto += monthName.length();
					break;
				}
			}
		}

		return found ? upto : -1;
	}

	/**
	 * Retrieve the Map of month abbreviation name to month index for this Locale
	 * @param locale Locale we are interested in
	 * @return Map of month abbreviation name to month index for this Locale
	 */
	public static Map<String, Integer> getShortMonths(Locale locale) {
		cacheLocaleInfo(locale);
		return shortMonths.get(locale.toLanguageTag());
	}

	/**
	 * Check that the input starts with a valid month abbreviation (case insensitive)
	 * and if so return the offset of the end of match.
	 * @param input The input string
	 * @param locale Locale we are interested in
	 * @return Offset of the end of the matched input, or -1 if no match
	 */
	public static int skipValidMonthAbbr(String input, Locale locale) {
		int upto = 0;
		int inputLength = input.length();
		// Get the length of Month Abbreviations in this locale
		int abbrLength = LocaleInfo.getShortMonthsLength(locale);
		if (abbrLength != -1) {
			// If it is constant length it is easy
			if (upto + abbrLength > inputLength)
				return -1;
			final String monthAbbreviation = input.substring(upto, upto + abbrLength);
			if (LocaleInfo.shortMonthOffset(monthAbbreviation, locale) == -1)
				return -1;
			upto += abbrLength;
			return upto;
		}

		boolean found = false;
		for (String monthAbbr : LocaleInfo.getShortMonths(locale).keySet()) {
			if (input.substring(upto).toUpperCase(locale).startsWith(monthAbbr)) {
				found = true;
				upto += monthAbbr.length();
				break;
			}
		}
		return found ? upto : -1;
	}

	/**
	 * Retrieve a Regular Expression for month abbreviations in this Locale
	 * @param locale Locale we are interested in
	 * @return Regular Expression for month abbreviations in this locale
	 */
	public static String getShortMonthsRegExp(Locale locale) {
		cacheLocaleInfo(locale);
		return shortMonthsRegExp.get(locale.toLanguageTag());
	}

	/**
	 * Retrieve the length of the Short Months
	 * @param locale Locale we are interested in
	 * @return The length of the Short Months abbreviation (or -1 if not consistent)
	 */
	public static int getShortMonthsLength(Locale locale) {
		cacheLocaleInfo(locale);
		return shortMonthsLength.get(locale.toLanguageTag());
	}

	/**
	 * Retrieve the Set containing the week days for this Locale
	 * @param locale Locale we are interested in
	 * @return Set containing week days for this Locale
	 */
	public static Set<String> getWeekdays(Locale locale) {
		cacheLocaleInfo(locale);
		return weekdays.get(locale.toLanguageTag());
	}

	/**
	 * Retrieve the Set containing the week day abbreviations for this Locale
	 * @param locale Locale we are interested in
	 * @return Set containing week day abbreviations for this Locale
	 */
	public static Set<String> getShortWeekdays(Locale locale) {
		cacheLocaleInfo(locale);
		return shortWeekdays.get(locale.toLanguageTag());
	}

	/**
	 * Are Short Week Days all Alphabetic?
	 * @param locale Locale we are interested in
	 * @return True if all Short Week Days strings are all Alphabetic
	 */
	public static boolean areDayOfWeekAbbrAlphabetic(Locale locale) {
		cacheLocaleInfo(locale);
		return shortWeekdaysAlphabetic.get(locale.toLanguageTag());
	}

	/**
	 * Retrieve a Regular Expression for week day abbreviations in this Locale
	 * @param locale Locale we are interested in
	 * @return Regular Expression for week day abbreviations in this locale
	 */
	public static String getShortWeekdaysRegExp(Locale locale) {
		cacheLocaleInfo(locale);
		return shortWeekdaysRegExp.get(locale.toLanguageTag());
	}

	public static boolean validDayOfWeekAbbr(final String dayOfWeekAbbr, Locale locale) {
		return LocaleInfo.getShortWeekdays(locale).contains(dayOfWeekAbbr.toUpperCase(locale));
	}

	/**
	 * Check that the input starts with a valid week day abbreviation (case insensitive)
	 * and if so return the offset of the end of match.
	 * @param input The input string
	 * @param locale Locale we are interested in
	 * @return Offset of the end of the matched input, or -1 if no match
	 */
	public static int skipValidDayOfWeekAbbr(String input, Locale locale) {
		int upto = 0;
		int inputLength = input.length();
		boolean allAlphabetic = LocaleInfo.areDayOfWeekAbbrAlphabetic(locale);

		if (allAlphabetic) {
			while (upto < inputLength && Character.isAlphabetic(input.charAt(upto)))
				upto++;
			final String dayOfWeekAbbr = input.substring(0, upto);
			return validDayOfWeekAbbr(dayOfWeekAbbr, locale) ? upto : -1;
		}

		boolean found = false;
		input = input.toUpperCase(locale);
		for (String dayOfWeekAbbr : LocaleInfo.getShortWeekdays(locale)) {
			if (input.startsWith(dayOfWeekAbbr)) {
				found = true;
				upto += dayOfWeekAbbr.length();
				break;
			}
		}

		return found ? upto : -1;
	}

	/**
	 * Retrieve the Set containing the for this Locale
	 * @param locale Locale we are interested in
	 * @return Set containing AM/PM strings  for this Locale
	 */
	public static Set<String> getAMPMStrings(Locale locale) {
		cacheLocaleInfo(locale);
		return ampmStrings.get(locale.toLanguageTag());
	}

	/**
	 * Retrieve a Regular Expression for AM/PM strings in this Locale
	 * @param locale Locale we are interested in
	 * @return Regular Expression for AM/PM strings in this locale
	 */
	public static String getAMPMRegExp(Locale locale) {
		cacheLocaleInfo(locale);
		return ampmRegExp.get(locale.toLanguageTag());
	}

	/**
	 * Check that the input starts with a valid AM/PM string (case insensitive)
	 * and if so return the offset of the end of match.
	 * @param input The input string
	 * @param locale Locale we are interested in
	 * @return Offset of the end of the matched input, or -1 if no match
	 */
	public static int skipValidAMPM(String input, Locale locale) {
		int upto = 0;

		for (String ampmIndicator : LocaleInfo.getAMPMStrings(locale)) {
			if (input.substring(upto).toUpperCase(locale).startsWith(ampmIndicator)) {
				upto += ampmIndicator.length();
				return upto;
			}
		}

		return -1;
	}
}
