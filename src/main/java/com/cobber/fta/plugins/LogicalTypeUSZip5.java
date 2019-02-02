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

public class LogicalTypeUSZip5 extends LogicalTypeInfinite {
	public final static String REGEXP = "\\d{5}";
	private static Set<String> zips = new HashSet<String>();

	@Override
	public boolean isCandidate(String input, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		return compressed.length() == 5 && compressed.toString().equals("\\d{5}");
	}

	@Override
	public synchronized boolean initialize(Locale locale) {
		threshold = 95;

		// Only set up the Static Data once
		if (zips.isEmpty()) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/us_zips.csv")))){
				String line = null;

				while ((line = reader.readLine()) != null) {
					zips.add(line);
				}
			} catch (IOException e) {
				throw new IllegalArgumentException("Internal error: Issues with US Zip database");
			}
		}

		return true;
	}

	@Override
	public String getQualifier() {
		return "US_ZIP5";
	}

	@Override
	public String getRegExp() {
		return REGEXP;
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
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, Map<String, Integer> cardinality, Map<String, Integer> outliers) {
		boolean zipName = dataStreamName != null && dataStreamName.toUpperCase().contains("ZIP");
		return (cardinality.size() < 5 && !zipName) || (double)matchCount/realSamples < getThreshold()/100.0 ? REGEXP : null;
	}
}
