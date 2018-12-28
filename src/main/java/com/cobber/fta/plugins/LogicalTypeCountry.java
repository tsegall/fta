package com.cobber.fta.plugins;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.Map;

import com.cobber.fta.LogicalTypeFiniteSimple;

public class LogicalTypeCountry extends LogicalTypeFiniteSimple {
	public LogicalTypeCountry() throws FileNotFoundException {
		super("COUNTRY_EN", ".+", ".+}",
				new InputStreamReader(LogicalTypeCAProvince.class.getResourceAsStream("/reference/countries.csv")), 95);
	}

	@Override
	public String shouldBackout(long matchCount, long realSamples, Map<String, Integer> cardinality, Map<String, Integer> outliers) {
		if (outliers.size() > Math.sqrt(getMembers().size()))
			return ".+";

		return (double)matchCount / realSamples >= getThreshold()/100.0 ? null : ".+";
	}
}
