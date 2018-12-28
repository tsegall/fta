package com.cobber.fta.plugins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PatternInfo;
import com.cobber.fta.PatternInfo.Type;
import com.cobber.fta.TextAnalyzer;

public class LogicalTypeUSZip5 extends LogicalTypeInfinite {
	private static Set<String> zips = new HashSet<String>();

	@Override
	public boolean isCandidate(String input, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		return compressed.length() == 5 && compressed.toString().equals("\\d{5}");
	}

	@Override
	public boolean initialize() {
		threshold = 95;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/us_zips.csv")))){
			String line = null;

			while ((line = reader.readLine()) != null) {
				zips.add(line);
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("Internal error: Issues with US Zip database");
		}

		return true;
	}

	@Override
	public String getQualifier() {
		return "US_ZIP5";
	}

	@Override
	public String getRegexp() {
		return "\\d{5}";
	}

	@Override
	public Type getBaseType() {
		return PatternInfo.Type.LONG;
	}

	@Override
	public boolean isValid(String input) {
		return zips.contains(input);
	}

	@Override
	public String shouldBackout(long matchCount, long realSamples, Map<String, Integer> cardinality, Map<String, Integer> outliers) {
		return cardinality.size() < 5 || (double)matchCount/realSamples < getThreshold()/100.0 ? "\\d{5}" : null;
	}
}
