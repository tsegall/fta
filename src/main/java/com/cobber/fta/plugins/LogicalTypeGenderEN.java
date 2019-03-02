package com.cobber.fta.plugins;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.LogicalTypeFinite;
import com.cobber.fta.RegExpGenerator;

/**
 * Plugin to detect Gender. (English-language only).
 */
public class LogicalTypeGenderEN extends LogicalTypeFinite {
	private static Set<String> members = new HashSet<String>();
	private String happyRegex = "\\p{Alpha}+";
	private Locale locale;

	static {
		members.add("FEMALE");
		members.add("MALE");
	}

	@Override
	public boolean initialize(Locale locale) {
		super.initialize(locale);

		this.locale = locale;
		threshold = 95;

		return true;
	}

	@Override
	public Set<String> getMembers() {
		return members;
	}

	@Override
	public String getQualifier() {
		return "GENDER_EN";
	}

	@Override
	public String getRegExp() {
		return 	happyRegex;
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, Map<String, Integer> cardinality, Map<String, Integer> outliers) {
		// Feel like this should be a little more inclusive in this day and age but not sure what set to use!!
		if (outliers.size() > 1)
			return "\\p{IsAlphabetic}+";

		// If we have seen both Male & Female and no more than one outlier then we are feeling pretty good unless we are in Strict mode (e.g. 100%)
		if ((threshold != 100 && outliers.size() <= 1) || (double)matchCount / realSamples >= getThreshold()/100.0) {
			RegExpGenerator re = new RegExpGenerator(true, 3, locale);
			for (String element : members)
				re.train(element);
			if (!outliers.isEmpty())
				re.train(outliers.keySet().iterator().next());
			happyRegex = re.getResult();
			return null;
		}

		return "\\p{IsAlphabetic}+";
	}
}
