package com.cobber.fta.plugins;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.LocaleInfo;
import com.cobber.fta.LogicalTypeFinite;

public class LogicalTypeMonthAbbr extends LogicalTypeFinite {
	Locale locale;

	@Override
	public  synchronized boolean initialize(Locale locale) {
		this.locale = locale;

		threshold = 95;

		super.initialize(locale);

		return true;
	}

	@Override
	public Set<String> getMembers() {
		return LocaleInfo.getShortMonths(locale).keySet();
	}

	@Override
	public String getQualifier() {
		return "MONTHABBR";
	}

	@Override
	public String getRegexp() {
		return LocaleInfo.getShortMonthsRegExp(locale);
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, Map<String, Integer> cardinality, Map<String, Integer> outliers) {
		if (outliers.size() > 1)
			return LocaleInfo.getShortMonthsRegExp(locale);

		return (double)matchCount / realSamples >= getThreshold()/100.0 ? null : LocaleInfo.getShortMonthsRegExp(locale);
	}
}
