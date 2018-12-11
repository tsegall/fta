package com.cobber.fta.plugins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PatternInfo;
import com.cobber.fta.PatternInfo.Type;
import com.cobber.fta.TextAnalyzer;

public class LogicalTypeAddress extends LogicalTypeInfinite {
	private static Set<String> addressMarkers = new HashSet<String>();

	@Override
	public boolean initialize() {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/address_markers.csv")))){
			String line = null;

			while ((line = reader.readLine()) != null) {
				addressMarkers.add(line);
			}
		} catch (IOException e) {
			return false;
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
	public double getSampleThreshold() {
		return 0.9;
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
		boolean result = spaceIndex != -1 && addressMarkers.contains(input.substring(spaceIndex + 1, input.length()).toUpperCase(Locale.ROOT));
		if (result)
			candidateCount++;
		return result;
	}
}
