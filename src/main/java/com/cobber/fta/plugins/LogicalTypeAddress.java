package com.cobber.fta.plugins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PatternInfo;
import com.cobber.fta.PatternInfo.Type;
import com.cobber.fta.TextAnalyzer;

public class LogicalTypeAddress extends LogicalTypeInfinite {
	private static Set<String> addressMarkers = new HashSet<String>();

	@Override
	public boolean initialize() {
		threshold = 90;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/address_markers.csv")))){
			String line = null;

			while ((line = reader.readLine()) != null) {
				addressMarkers.add(line);
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("Internal error: Issues with Address database");
		}

		return true;
	}

	@Override
	public String getQualifier() {
		return "ADDRESS_EN";
	}

	@Override
	public String getRegexp() {
		return ".+";
	}

	@Override
	public Type getBaseType() {
		return PatternInfo.Type.STRING;
	}

	@Override
	public boolean isValid(String input) {
		int spaceIndex = input.lastIndexOf(' ');
		return spaceIndex != -1 && addressMarkers.contains(input.substring(spaceIndex + 1).toUpperCase(Locale.ROOT));
	}

	@Override
	public boolean isCandidate(String input, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		int spaceIndex = lastIndex[' '];
		return spaceIndex != -1 && addressMarkers.contains(input.substring(spaceIndex + 1, input.length()).toUpperCase(Locale.ROOT));
	}

	@Override
	public String shouldBackout(long matchCount, long realSamples, Map<String, Integer> cardinality, Map<String, Integer> outliers) {
		return (double)matchCount/realSamples >= getThreshold()/100.0 ? null : ".+";
	}
}
