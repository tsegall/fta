package com.cobber.fta.plugins;

import java.text.DateFormatSymbols;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.LogicalTypeFinite;
import com.cobber.fta.Utils;

public class LogicalTypeMonthAbbr extends LogicalTypeFinite {
	private static HashMap<String, Set<String>> monthAbbrs = new HashMap<String, Set<String>>();
	private static HashMap<String, String> regExps = new HashMap<String, String>();
	String ISO3Language;

	@Override
	public  synchronized boolean initialize(Locale locale) {
		ISO3Language = locale.getISO3Language();

		threshold = 95;

		if (monthAbbrs.get(ISO3Language) != null) {
			super.initialize(locale);
			return true;
		}

		boolean alphabetic = false;
		boolean alphabeticWithPunctuation = false;

		HashSet<String> thisLanguage = new HashSet<String>();
		final String[] shortMonths = new DateFormatSymbols(locale).getShortMonths();
		for (int i = 0; i < 12; i++) {
			String thisMonthAbbr = shortMonths[i].toUpperCase(locale);
			thisLanguage.add(thisMonthAbbr);
			if (thisMonthAbbr.matches("\\p{Alpha}*"))
				;
			else if (thisMonthAbbr.matches("\\p{IsAlphabetic}*"))
				alphabetic = true;
			else if (thisMonthAbbr.matches("[\\p{IsAlphabetic}\\.]*"))
				alphabeticWithPunctuation = true;
		}
		monthAbbrs.put(ISO3Language, thisLanguage);

		super.initialize(locale);

		String baseRE = alphabeticWithPunctuation ? "[\\p{IsAlphabetic}\\.]" :
				(alphabetic ? "\\p{IsAlphabetic}" : "\\p{Alpha}");
		regExps.put(ISO3Language, baseRE + Utils.regExpLength(getMinLength(), getMaxLength()));

		return true;
	}

	@Override
	public Set<String> getMembers() {
		return monthAbbrs.get(ISO3Language);
	}

	@Override
	public String getQualifier() {
		return "MONTHABBR";
	}

	@Override
	public String getRegexp() {
		return regExps.get(ISO3Language);
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, Map<String, Integer> cardinality, Map<String, Integer> outliers) {
		if (outliers.size() > 1)
			return regExps.get(ISO3Language);

		return (double)matchCount / realSamples >= getThreshold()/100.0 ? null : regExps.get(ISO3Language);
	}
}
