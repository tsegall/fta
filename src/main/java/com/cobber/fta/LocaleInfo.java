package com.cobber.fta;

import java.text.DateFormatSymbols;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/*
 * Helper class used to cache Month information across multiple languages.
 */
public class LocaleInfo {
	private static HashMap<String, HashMap<String, Integer>> months = new HashMap<String, HashMap<String, Integer>>();
	private static HashMap<String, HashMap<String, Integer>> monthAbbr = new HashMap<String, HashMap<String, Integer>>();
	private static Map<String, String> monthRegExp = new HashMap<String, String>();
	private static Map<String, String> monthAbbrRegExp = new HashMap<String, String>();

	private static synchronized void cacheLocaleInfo(Locale locale) {
		final String language = locale.getISO3Language();

		// Check to see if we are already in the cache
		if (months.get(language) != null)
			return;

		final GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance(locale);
		final int actualMonths = cal.getActualMaximum(GregorianCalendar.MONTH);

		// Setup the Months
		final String[] longMonths = new DateFormatSymbols(locale).getMonths();
		HashMap<String, Integer> monthsLocale = new HashMap<String, Integer>();
		CharacterClass characterClass = new CharacterClass();

		for (int i = 0; i <= actualMonths; i++) {
			final String month = longMonths[i].toUpperCase(locale);
			monthsLocale.put(month, i + 1);
			characterClass.train(month);
		}
		months.put(language, monthsLocale);
		monthRegExp.put(language, characterClass.getResult());

		// Setup the Monthly abbreviations
		final String[] shortMonths = new DateFormatSymbols(locale).getShortMonths();
		HashMap<String, Integer> monthAbbrLocale = new HashMap<String, Integer>();
		characterClass = new CharacterClass();

		for (int i = 0; i <= actualMonths; i++) {
			final String monthAbbr = shortMonths[i].toUpperCase(locale);
			monthAbbrLocale.put(monthAbbr, i + 1);
			characterClass.train(monthAbbr);
		}
		monthAbbr.put(language, monthAbbrLocale);
		monthAbbrRegExp.put(language, characterClass.getResult());
	}

	/**
	 * Retrieve the Map of month abbreviation name to month index for this Locale
	 * @param locale Locale we are interested in
	 * @return Map of month abbreviation name to month index for this Locale
	 */
	public static HashMap<String, Integer> getMonthAbbrs(Locale locale) {
		cacheLocaleInfo(locale);
		return monthAbbr.get(locale.getISO3Language());
	}

	/**
	 * Retrieve the Map of Month name to month index for this Locale
	 * @param locale Locale we are interested in
	 * @return Map of Month name to month index for this Locale
	 */
	public static HashMap<String, Integer> getMonths(Locale locale) {
		cacheLocaleInfo(locale);
		return months.get(locale.getISO3Language());
	}

	/**
	 * Retrieve a Regular Expression for months in this Locale
	 * @param locale Locale we are interested in
	 * @return Regular Expression for months in this locale
	 */
	public static String getMonthRegExp(Locale locale) {
		cacheLocaleInfo(locale);
		return monthRegExp.get(locale.getISO3Language());
	}

	/**
	 * Retrieve a Regular Expression for month abbreviations in this Locale
	 * @param locale Locale we are interested in
	 * @return Regular Expression for month abbreviations in this locale
	 */
	public static String getMonthAbbrRegExp(Locale locale) {
		cacheLocaleInfo(locale);
		return monthAbbrRegExp.get(locale.getISO3Language());
	}
}
