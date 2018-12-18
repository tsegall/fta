package com.cobber.fta.plugins;

import java.text.DateFormatSymbols;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.LogicalTypeFinite;

public class LogicalTypeMonthAbbr extends LogicalTypeFinite {
	private static Set<String> members;

	static {
		members = new HashSet<String>();	

		// Setup the Monthly abbreviations
		final String[] shortMonths = new DateFormatSymbols().getShortMonths();
		for (int i = 0; i < 12; i++) {
			members.add(shortMonths[i].toUpperCase(Locale.ROOT));
		}
	}

	@Override
	public boolean initialize() {
		super.initialize();

		threshold = 95;

		return true;
	}

	@Override
	public Set<String> getMembers() {
		return members;
	}

	@Override
	public String getQualifier() {
		return "MONTHABBR";
	}
	
	@Override
	public String getRegexp() {
		return "\\p{Alpha}{3}";
	}

	@Override
	public String shouldBackout(long matchCount, long realsamples, Map<String, Integer> cardinality, Map<String, Integer> outliers) {
		if (outliers.size() > 1)
			return "\\p{Alpha}{3}";

		return (double)matchCount / realsamples >= getThreshold()/100.0 ? null : "\\p{Alpha}{3}";
	}
}
