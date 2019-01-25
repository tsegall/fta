package com.cobber.fta;

import java.text.DateFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
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
	private static Map<String, Map<String, Integer>> shortMonths = new HashMap<>();
	private static Map<String, Set<String>> shortWeekdays = new HashMap<>();
	private static Map<String, String> monthsRegExp = new HashMap<>();
	private static Map<String, String> shortMonthsRegExp = new HashMap<>();
	private static Map<String, Set<String>> ampmStrings = new HashMap<>();
	private static Map<String, String> ampmRegExp = new HashMap<>();
	private static Map<String, String> shortWeekdaysRegExp = new HashMap<>();
	private static LocaleInfo localeInfo = new LocaleInfo();
	private static final Map<String, DateTimeFormatter> formatterCache = new HashMap<>();

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
		final int actualMonths = cal.getActualMaximum(GregorianCalendar.MONTH);

		DateFormatSymbols dfs = new DateFormatSymbols(locale);


		// Setup the Months
		final String[] longMonths = dfs.getMonths();
		Map<String, Integer> monthsLocale = new TreeMap<>(localeInfo.new LengthComparator());
		RegExpGenerator generator = new RegExpGenerator();

		for (int i = 0; i <= actualMonths; i++) {
			final String month = longMonths[i].toUpperCase(locale);
			monthsLocale.put(month, i + 1);
			generator.train(month);
			generator.train(longMonths[i]);
		}
		months.put(languageTag, monthsLocale);
		monthsRegExp.put(languageTag, generator.getResult());

		// Setup the Monthly abbreviations
		final String[] m = dfs.getShortMonths();
		Map<String, Integer> shortMonthsLocale = new TreeMap<>(localeInfo.new LengthComparator());
		generator = new RegExpGenerator();

		for (int i = 0; i <= actualMonths; i++) {
			final String shortMonth = m[i].toUpperCase(locale);
			shortMonthsLocale.put(shortMonth, i + 1);
			generator.train(shortMonth);
			generator.train(m[i]);
		}
		shortMonths.put(languageTag, shortMonthsLocale);
		shortMonthsRegExp.put(languageTag, generator.getResult());

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

		// Setup the Short Week Day strings
		Set<String> shortWeekdaysLocale = new TreeSet<>(localeInfo.new LengthComparator());
		generator = new RegExpGenerator();
		for (String week : dfs.getShortWeekdays()) {
			if (week.isEmpty())
				continue;
			final String weekUpper = week.toUpperCase(locale);
			shortWeekdaysLocale.add(weekUpper);
			generator.train(week);
			generator.train(weekUpper);
		}
		shortWeekdays.put(languageTag, shortWeekdaysLocale);
		shortWeekdaysRegExp.put(languageTag, generator.getResult());
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
	 * Retrieve a Regular Expression for months in this Locale
	 * @param locale Locale we are interested in
	 * @return Regular Expression for months in this locale
	 */
	public static String getMonthsRegExp(Locale locale) {
		cacheLocaleInfo(locale);
		return monthsRegExp.get(locale.toLanguageTag());
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
	 * Retrieve a Regular Expression for month abbreviations in this Locale
	 * @param locale Locale we are interested in
	 * @return Regular Expression for month abbreviations in this locale
	 */
	public static String getShortMonthsRegExp(Locale locale) {
		cacheLocaleInfo(locale);
		return shortMonthsRegExp.get(locale.toLanguageTag());
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
	 * Retrieve a Regular Expression for week day abbreviations in this Locale
	 * @param locale Locale we are interested in
	 * @return Regular Expression for week day abbreviations in this locale
	 */
	public static String getShortWeekdaysRegExp(Locale locale) {
		cacheLocaleInfo(locale);
		return shortWeekdaysRegExp.get(locale.toLanguageTag());
	}

	public static Set<String> getAMPMStrings(Locale locale) {
		cacheLocaleInfo(locale);
		return ampmStrings.get(locale.toLanguageTag());
	}

	public static String getAMPMRegExp(Locale locale) {
		cacheLocaleInfo(locale);
		return ampmRegExp.get(locale.toLanguageTag());
	}

	/*
	 * Get a DateTimeFormatter suitable for the supplied formatString and Locale.
	 */
	public static DateTimeFormatter getFormatter(String formatString, Locale locale) {
		DateTimeFormatter formatter = formatterCache.get(locale.toLanguageTag() + "---" + formatString);

		if (formatter != null)
			return formatter;

		formatter = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(formatString).toFormatter(locale);
		formatterCache.put(locale.toLanguageTag() + "---" + formatString, formatter);

		return formatter;
	}

}
