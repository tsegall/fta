package com.cobber.fta.plugins;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.LogicalTypeFinite;

public class LogicalTypeGender extends LogicalTypeFinite {
	private static Set<String> members = new HashSet<String>();

	static {
		members.add("FEMALE");
		members.add("MALE");
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
		return "GENDER_EN";
	}

	@Override
	public String getRegexp() {
		return 	"\\p{Alpha}+";
	}

	@Override
	public String shouldBackout(long matchCount, long realSamples, Map<String, Integer> cardinality, Map<String, Integer> outliers) {
		// Feel like this should be a little more inclusive in this day and age but not sure what setß to use!! 
		if (outliers.size() > 1)
			return "\\p{Alpha}+";

		// If we have seen both Male & Female and no more than one outlier then we are feeling pretty good unless we are in Strict mode (e.g. 100%)
		if (threshold != 100 && cardinality.size() == 3)
			return null;
		return (double)matchCount / realSamples >= getThreshold()/100.0 ? null : "\\p{Alpha}+";
	}
}
