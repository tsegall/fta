package com.cobber.fta.examples;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.LogicalTypeFinite;
import com.cobber.fta.StringFacts;

public class PluginColor extends LogicalTypeFinite {
	public final static String SEMANTIC_TYPE = "COLOR.TEXT_EN";
	private static Set<String> members = new HashSet<String>();
	private static String colors[] = new String[] {
			"RED",  "GREEN", "BLUE", "PINK", "BLACK", "WHITE", "ORANGE", "PURPLE",
			"GREY", "GREEN", "YELLOW", "MAUVE", "CREAM", "BROWN", "SILVER", "GOLD",
			"PEACH", "OLIVE", "LEMON", "LILAC", "BEIGE", "AMBER", "BURGUNDY"
	};

	static {
		members.addAll(Arrays.asList(colors));
	}

	@Override
	public Set<String> getMembers() {
		return members;
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public String getRegExp() {
		return ".+";
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, StringFacts stringFacts,
			Map<String, Integer> cardinality, Map<String, Integer> outliers) {
		if (outliers.size() > 3)
			return ".+";

		if ((double)matchCount / realSamples >= getThreshold()/100.0)
			return null;

		return ".+";
	}
}
