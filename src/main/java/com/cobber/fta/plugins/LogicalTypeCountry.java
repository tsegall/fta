package com.cobber.fta.plugins;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.LogicalTypeFiniteSimple;

public class LogicalTypeCountry extends LogicalTypeFiniteSimple {
	private static Set<String> members = new HashSet<String>();

	public LogicalTypeCountry() throws FileNotFoundException {
		super("COUNTRY_EN", ".+", ".+}",
				new InputStreamReader(LogicalTypeCAProvince.class.getResourceAsStream("/reference/countries.csv")), 95);
	}

	@Override
	public Set<String> getMembers() {
		return members;
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, Map<String, Integer> cardinality, Map<String, Integer> outliers) {
		if (outliers.size() > Math.sqrt(getMembers().size()))
			return ".+";

		return (double)matchCount / realSamples >= getThreshold()/100.0 ? null : ".+";
	}
}
