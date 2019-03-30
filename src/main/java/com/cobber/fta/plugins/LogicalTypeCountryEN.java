package com.cobber.fta.plugins;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.LogicalTypeFiniteSimple;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.StringFacts;

/**
 * Plugin to detect Country names. (English-language only).
 */
public class LogicalTypeCountryEN extends LogicalTypeFiniteSimple {
	public final static String SEMANTIC_TYPE = "COUNTRY.TEXT_EN";
	private static Set<String> members = new HashSet<String>();
	private static String[] membersArray = null;
	final static String REGEXP = ".+";
	final static String hotWord = "country";

	public LogicalTypeCountryEN(PluginDefinition plugin) throws FileNotFoundException {
		super(plugin.qualifier, plugin.hotWords, REGEXP,
				"\\p{IsAlphabetic}{2}", new InputStreamReader(LogicalTypeCAProvince.class.getResourceAsStream("/reference/countries.csv")), 95);
	}

	@Override
	public Set<String> getMembers() {
		return members;
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, StringFacts stringFacts, Map<String, Long> cardinality, Map<String, Long> outliers) {
		if (outliers.size() > Math.sqrt(getMembers().size()))
			return REGEXP;

		if (!dataStreamName.toLowerCase(locale).contains(hotWord) && (realSamples < 10 || cardinality.size() == 1))
			return REGEXP;

		return (double)matchCount / realSamples >= getThreshold()/100.0 ? null : REGEXP;
	}

	@Override
	public String[] getMemberArray() {
		if (membersArray == null)
			membersArray = members.toArray(new String[members.size()]);
		return membersArray;
	}
}
