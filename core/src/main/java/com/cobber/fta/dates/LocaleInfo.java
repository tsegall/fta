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

import java.text.DateFormatSymbols;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.core.Utils;

/*
 * Set of operations on the current Locale.
 */
public class LocaleInfo {
	private Map<String, Integer> months = new HashMap<>();
	private Map<String, Integer> monthsDefault = new HashMap<>();
	private Map<String, Integer> shortMonths = new HashMap<>();
	private String[] shortMonthsArray;

	private Set<String> weekdays = new HashSet<>();
	private Set<String> shortWeekdays = new HashSet<>();
	private String monthsRegExp;
	private Boolean areMonthsAlphabetic;
	private String shortMonthsRegExp;
	private Integer shortMonthsLength;
	private Set<String> ampmStrings = new HashSet<>();
	private String ampmRegExp;

	private String weekdaysRegExp;
	private Boolean areWeekdaysAlphabetic;
	private String shortWeekdaysRegExp;
	private Boolean areShortWeekdaysAlphabetic;

	private String unsupportedReason;

	private final LocaleInfoConfig localeInfoConfig;

	private static Map<String, LocaleInfo> cache = new ConcurrentHashMap<>();
	private static Set<String> amPmNonLocalized = new HashSet<>(Arrays.asList("AM", "PM"));

	public static LocaleInfo getInstance(final LocaleInfoConfig localeInfoConfig) {
		final String cacheKey = localeInfoConfig.getCacheKey();

		// Check to see if we are already in the cache
		if (cache.get(cacheKey) != null)
			return cache.get(cacheKey);

		final LocaleInfo ret = new LocaleInfo(localeInfoConfig);

		ret.initialize(localeInfoConfig.getLocale());

		final LocaleInfo oldValue = cache.putIfAbsent(cacheKey, ret);

		return oldValue == null ? ret : oldValue;

	}

	/**
	 * Check that this locale is supported.
	 * @param locale Locale we are interested in
	 * @return String Return reason if not supported, otherwise null.
	 */
	public static String isSupported(final Locale locale) {
		// We do not really care whether the other arguments to getInstance are true or false, it does not
		// change whether the locals is supported.
		return getInstance(new LocaleInfoConfig(locale, true, true)).unsupportedReason;
	}

	private LocaleInfo(final LocaleInfoConfig localeInfoConfig) {
		this.localeInfoConfig = localeInfoConfig;
	}

	public Locale getLocale() {
		return localeInfoConfig.getLocale();
	}

	private static class LengthComparator implements Comparator<String> {
		@Override
		public int compare(final String str1, final String str2) {
			final int ret = str2.length() - str1.length();
			if (ret != 0)
				return ret;
			return str1.compareTo(str2);
		}
	}

	private void initialize(final Locale locale) {
		final Calendar rawCal = Calendar.getInstance(locale);
		if (!(rawCal instanceof GregorianCalendar)) {
			unsupportedReason = "No support for locales that do not use the Gregorian Calendar";
			return;
		}

		if (!NumberFormat.getNumberInstance(locale).format(0).matches("\\d")) {
			unsupportedReason = "No support for locales that do not use Arabic numerals";
			return;
		}

		final GregorianCalendar cal = (GregorianCalendar) rawCal;

		final int actualMonths = cal.getActualMaximum(Calendar.MONTH);

		final DateFormatSymbols dfs = DateFormatSymbols.getInstance(locale);

		// Setup the Months
		final String[] longMonths = dfs.getMonths();
		final Map<String, Integer> monthsLocale = new TreeMap<>(new LengthComparator());
		final Map<String, Integer> monthsDefaultLocale = new TreeMap<>(new LengthComparator());
		RegExpGenerator generator = new RegExpGenerator();

		boolean isAllAlphabetic = true;
		for (int i = 0; i <= actualMonths; i++) {
			final String month = Normalizer.normalize(longMonths[i].toUpperCase(locale), Normalizer.Form.NFC);
			if (isAllAlphabetic && !month.chars().allMatch(Character::isAlphabetic))
				isAllAlphabetic = false;
			monthsLocale.put(month, i + 1);
			monthsDefaultLocale.put(longMonths[i], i + 1);
			generator.train(longMonths[i]);
		}
		months = monthsLocale;
		monthsDefault = monthsDefaultLocale;
		monthsRegExp = generator.getResult();
		areMonthsAlphabetic = isAllAlphabetic;

		// Setup the Monthly abbreviations, in Java some countries (e.g. AU, CA) have the short months defined with a
		// period after them, for example 'AUG.' - if useStandardAbbreviations is set just using the US definition of 'truth'
		shortMonthsArray = dfs.getShortMonths();
		final NavigableMap<String, Integer> shortMonthsLocale = new TreeMap<>(new LengthComparator());
		generator = new RegExpGenerator();

		boolean useShortMonths = true;
		for (int i = 0; i <= actualMonths; i++) {
			final int len = shortMonthsArray[i].length();
			if (localeInfoConfig.isNoAbbreviationPunctuation() && len != 0 && shortMonthsArray[i].charAt(len - 1) == '.')
				shortMonthsArray[i] = shortMonthsArray[i].substring(0, len - 1);
			final String shortMonth = shortMonthsArray[i].toUpperCase(locale);
			shortMonthsLocale.put(shortMonth, i + 1);
			// Check that the length of the upper case month abbreviation is the same as the length of the month abbreviation in Title Case
			// This is not the case with Language 'el', Month Μαΐ.
			if (shortMonthsArray[i].length() != shortMonth.length())
				useShortMonths = false;
			if (Utils.isNumeric(shortMonthsArray[i]))
				useShortMonths = false;
			generator.train(shortMonthsArray[i]);
		}
		if (useShortMonths) {
			shortMonths = shortMonthsLocale;
			shortMonthsRegExp = generator.getResult();
			final int len = shortMonthsLocale.firstKey().length();
			shortMonthsLength = len == shortMonthsLocale.lastKey().length() ? len : -1;
		}
		else {
			shortMonths = new TreeMap<>(new LengthComparator());
			shortMonthsLength =  -1;
		}

		final Set<String> amPmStrings = new TreeSet<>(Arrays.asList(dfs.getAmPmStrings()));

		// Setup the AM/PM strings
		final Set<String> ampmStringsLocale = new LinkedHashSet<>();
		String ampmRegExpLocale = "";
		for (final String s : amPmStrings) {
			if (ampmRegExpLocale.length() != 0)
				ampmRegExpLocale += "|";
			ampmRegExpLocale += s;
			ampmStringsLocale.add(s.toUpperCase(locale));
		}
		ampmStrings = ampmStringsLocale;
		ampmRegExp = "(?i)(" + ampmRegExpLocale + ")";

		// Setup the Week Day strings
		final Set<String> weekdaysLocale = new TreeSet<>(new LengthComparator());
		generator = new RegExpGenerator();
		isAllAlphabetic = true;
		for (final String week : dfs.getWeekdays()) {
			if (week.isEmpty())
				continue;
			if (isAllAlphabetic && !week.chars().allMatch(Character::isAlphabetic))
				isAllAlphabetic = false;
			final String weekUpper = week.toUpperCase(locale);
			weekdaysLocale.add(weekUpper);
			generator.train(week);
			generator.train(weekUpper);
		}
		weekdays = weekdaysLocale;
		weekdaysRegExp = generator.getResult();
		areWeekdaysAlphabetic = isAllAlphabetic;

		// Setup the Short Week Day strings
		final Set<String> shortWeekdaysLocale = new TreeSet<>(new LengthComparator());
		generator = new RegExpGenerator();
		isAllAlphabetic = true;
		for (String shortWeek : dfs.getShortWeekdays()) {
			if (shortWeek.isEmpty())
				continue;
			// In Java some countries (e.g. CA) have the short days defined with a period after them,
			// for example 'SUN.' - if useStandardAbbreviations is set just using the US definition of 'truth'
			if (localeInfoConfig.isNoAbbreviationPunctuation()) {
				final int len = shortWeek.length();
				if (len != 0 && shortWeek.charAt(len - 1) == '.')
					shortWeek = shortWeek.substring(0, len - 1);
			}
			if (isAllAlphabetic && !shortWeek.chars().allMatch(Character::isAlphabetic))
				isAllAlphabetic = false;
			final String weekUpper = shortWeek.toUpperCase(locale);
			shortWeekdaysLocale.add(weekUpper);
			generator.train(shortWeek);
			generator.train(weekUpper);
		}
		shortWeekdays = shortWeekdaysLocale;
		shortWeekdaysRegExp = generator.getResult();
		areShortWeekdaysAlphabetic = isAllAlphabetic;
	}

	/**
	 * Retrieve the Map of Month name to month index for this Locale
	 * @return Map of Month name to month index for this Locale
	 */
	public Map<String, Integer> getMonths() {
		return months;
	}

	/**
	 * Retrieve the Map of Month name to month index for this Locale
	 * @return Map of Month name to month index for this Locale
	 */
	public Map<String, Integer> getMonthsDefault() {
		return monthsDefault;
	}

	/**
	 * Retrieve a Regular Expression for months in this Locale
	 * @return Regular Expression for months in this locale
	 */
	public String getMonthsRegExp() {
		return monthsRegExp;
	}

	public int shortMonthOffset(final String month) {
		final Integer offset = getShortMonths().get(month.toUpperCase(localeInfoConfig.getLocale()));
		return offset == null ? -1 : offset;
	}

	public int monthOffset(final String month) {
		final Integer offset = getMonths().get(month.toUpperCase(localeInfoConfig.getLocale()));
		return offset == null ? -1 : offset;
	}

	/**
	 * Are months all Alphabetic?
	 * @return True if all month strings are all Alphabetic
	 */
	public boolean areMonthsAlphabetic() {
		return areMonthsAlphabetic;
	}

	/**
	 * Check that the input starts with a valid month (case insensitive) and if so
	 * return the offset of the end of match.
	 * @param input The input string
	 * @return Offset of the end of the matched input, or -1 if no match
	 */
	public int skipValidMonth(String input) {
		final int inputLength = input.length();
		final boolean allAlphabetic = areMonthsAlphabetic;
		int upto = 0;

		if (allAlphabetic) {
			while (upto < inputLength && Character.isAlphabetic(input.charAt(upto)))
				upto++;
			final String month = input.substring(0, upto);
			if (monthOffset(month) == -1)
				return -1;
			return upto;
		}

		// In some locales the length of a month.toLowerCase().length != month.toUpperCase().length
		// So try matching first with the name as default, then the Upper case version
		boolean found = false;
		for (final String monthName : getMonthsDefault().keySet()) {
			if (input.startsWith(monthName)) {
				found = true;
				upto += monthName.length();
				break;
			}
		}
		if (!found) {
			input = input.toUpperCase(localeInfoConfig.getLocale());
			for (final String monthName : getMonths().keySet()) {
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
	 * @return Map of month abbreviation name to month index for this Locale
	 */
	public Map<String, Integer> getShortMonths() {
		return shortMonths;
	}

	/**
	 * Retrieve the array of month abbreviations, similar to DateFormatSymbols.getShortMonths() BUT
	 * the abbreviations may have been removed, based on the value of noAbbreviationPunctuation.
	 * @return Array of month abbreviations for this locale.
	 */
	public String[] getShortMonthsArray() {
		return shortMonthsArray;
	}

	/**
	 * Check that the input starts with a valid month abbreviation (case insensitive)
	 * and if so return the matched month abbreviation.
	 * @param input The input string
	 * @return The matched month abbreviation, or null if no match
	 */
	public String findValidMonthAbbr(final String input) {
		final int inputLength = input.length();
		// Get the length of Month Abbreviations in this locale
		final int abbrLength = getShortMonthsLength();

		if (abbrLength != -1) {
			// If it is constant length it is easy
			if (abbrLength > inputLength)
				return null;
			final String monthAbbreviation = input.substring(0, abbrLength);
			if (shortMonthOffset(monthAbbreviation) == -1)
				return null;
			return monthAbbreviation;
		}

		for (final String monthAbbr : getShortMonths().keySet()) {
			if (input.toUpperCase(localeInfoConfig.getLocale()).startsWith(monthAbbr)) {
				return monthAbbr;
			}
		}

		return null;
	}

	/**
	 * Retrieve a Regular Expression for month abbreviations in this Locale
	 * @return Regular Expression for month abbreviations in this locale
	 */
	public String getShortMonthsRegExp() {
		return shortMonthsRegExp;
	}

	/**
	 * Retrieve the length of the Short Months
	 * @return The length of the Short Months abbreviation (or -1 if not consistent)
	 */
	public int getShortMonthsLength() {
		return shortMonthsLength;
	}

	/**
	 * Retrieve the Set containing the week days for this Locale
	 * @return Set containing week days for this Locale
	 */
	public Set<String> getWeekdays() {
		return weekdays;
	}

	/**
	 * Retrieve a Regular Expression for a week day in this Locale
	 * @return Regular Expression for week day in this locale
	 */
	public String getWeekdaysRegExp() {
		return weekdaysRegExp;
	}

	/**
	 * Retrieve the Set containing the week day abbreviations for this Locale
	 * @return Set containing week day abbreviations for this Locale
	 */
	public Set<String> getShortWeekdays() {
		return shortWeekdays;
	}

	/**
	 * Are Week Days all Alphabetic?
	 * @return True if all Week Days strings are all Alphabetic
	 */
	public boolean areDayOfWeekAlphabetic() {
		return areWeekdaysAlphabetic;
	}

	/**
	 * Are Short Week Days all Alphabetic?
	 * @return True if all Short Week Days strings are all Alphabetic
	 */
	public boolean areDayOfWeekAbbrAlphabetic() {
		return areShortWeekdaysAlphabetic;
	}

	/**
	 * Retrieve a Regular Expression for week day abbreviations in this Locale
	 * @return Regular Expression for week day abbreviations in this locale
	 */
	public String getShortWeekdaysRegExp() {
		return shortWeekdaysRegExp;
	}

	public boolean validDayOfWeek(final String dayOfWeek) {
		return getWeekdays().contains(dayOfWeek.toUpperCase(localeInfoConfig.getLocale()));
	}

	public boolean validDayOfWeekAbbr(final String dayOfWeekAbbr) {
		return getShortWeekdays().contains(dayOfWeekAbbr.toUpperCase(localeInfoConfig.getLocale()));
	}

	/**
	 * Check that the input starts with a valid week day (case insensitive)
	 * and if so return the offset of the end of match.
	 * @param input The input string
	 * @return Offset of the end of the matched input, or -1 if no match
	 */
	public int skipValidDayOfWeek(String input) {
		final int inputLength = input.length();
		final boolean allAlphabetic = areWeekdaysAlphabetic;
		int upto = 0;

		if (allAlphabetic) {
			while (upto < inputLength && Character.isAlphabetic(input.charAt(upto)))
				upto++;
			final String dayOfWeek = input.substring(0, upto);
			return validDayOfWeek(dayOfWeek) ? upto : -1;
		}

		boolean found = false;
		input = input.toUpperCase(localeInfoConfig.getLocale());
		for (final String dayOfWeek : getWeekdays()) {
			if (input.startsWith(dayOfWeek)) {
				found = true;
				upto += dayOfWeek.length();
				break;
			}
		}

		return found ? upto : -1;
	}

	/**
	 * Check that the input starts with a valid week day abbreviation (case insensitive)
	 * and if so return the offset of the end of match.
	 * @param input The input string
	 * @return Offset of the end of the matched input, or -1 if no match
	 */
	public int skipValidDayOfWeekAbbr(String input) {
		final int inputLength = input.length();
		final boolean allAlphabetic = areShortWeekdaysAlphabetic;
		int upto = 0;

		if (allAlphabetic) {
			while (upto < inputLength && Character.isAlphabetic(input.charAt(upto)))
				upto++;
			final String dayOfWeekAbbr = input.substring(0, upto);
			return validDayOfWeekAbbr(dayOfWeekAbbr) ? upto : -1;
		}

		boolean found = false;
		input = input.toUpperCase(localeInfoConfig.getLocale());
		for (final String dayOfWeekAbbr : getShortWeekdays()) {
			if (input.startsWith(dayOfWeekAbbr)) {
				found = true;
				upto += dayOfWeekAbbr.length();
				break;
			}
		}

		return found ? upto : -1;
	}

	/**
	 * Retrieve the Set containing the AM/PM strings for this Locale
	 * @return Set containing AM/PM strings for this Locale
	 */
	public Set<String> getAMPMStrings() {
		return ampmStrings;
	}

	/**
	 * Retrieve the non-localized Set containing "AM" and "PM"
	 * @return Set containing "AM" and "PM"
	 */
	public Set<String> getAMPMStringsNonLocalized() {
		return amPmNonLocalized;
	}

	/**
	 * Retrieve a Regular Expression for AM/PM strings in this Locale
	 * @return Regular Expression for AM/PM strings in this locale
	 */
	public String getAMPMRegExp() {
		return ampmRegExp;
	}

	/**
	 * Check that the input starts with a valid AM/PM string (case insensitive)
	 * and if so return the offset of the end of match.
	 * @param input The input string
	 * @param localized Is the input string localized
	 * @return Offset of the end of the matched input, or -1 if no match
	 */
	public int skipValidAMPM(final String input, final boolean localized) {
		int upto = 0;
		final Set<String> indicators = localized ? getAMPMStrings() : getAMPMStringsNonLocalized();

		for (final String ampmIndicator : indicators) {
			if (input.substring(upto).toUpperCase(localeInfoConfig.getLocale()).startsWith(ampmIndicator)) {
				upto += ampmIndicator.length();
				return upto;
			}
		}

		return -1;
	}
}
