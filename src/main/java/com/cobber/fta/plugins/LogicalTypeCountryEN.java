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

	public LogicalTypeCountryEN(PluginDefinition plugin) throws FileNotFoundException {
		super(plugin, REGEXP, "\\p{IsAlphabetic}{2}", 95);
		setReader(new InputStreamReader(LogicalTypeCountryEN.class.getResourceAsStream("/reference/countries.csv")));
	}

	@Override
	public Set<String> getMembers() {
		return members;
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, StringFacts stringFacts, Map<String, Long> cardinality, Map<String, Long> outliers) {
		if (matchCount < 50 && outliers.size() > Math.sqrt(getMembers().size()))
			return REGEXP;

		int headerConfidence = getHeaderConfidence(dataStreamName);

		if (headerConfidence == 0 && (realSamples < 10 || cardinality.size() == 1))
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
