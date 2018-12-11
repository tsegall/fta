package com.cobber.fta.plugins;

import java.text.DateFormatSymbols;
import java.util.HashSet;
import java.util.Locale;
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
	public double getSampleThreshold() {
		// TODO Auto-generated method stub
		return 0;
	}
}
