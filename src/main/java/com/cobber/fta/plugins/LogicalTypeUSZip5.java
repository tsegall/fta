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
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.StringFacts;
import com.cobber.fta.TextAnalyzer;

/**
 * Plugin to detect valid US Zip codes.
 * Note: we used an Infinite :-) Logical Type since the domains is so large.
 */
public class LogicalTypeUSZip5 extends LogicalTypeInfinite {
	public final static String SEMANTIC_TYPE = "POSTAL_CODE.ZIP5_US";
	public static final String REGEXP_CONSTANT = "\\d{5}";
	public static final String REGEXP_VARIABLE = "\\d{3,5}";
	private static Set<String> zips = new HashSet<String>();
	private static String[] zipArray;
	private int minLength = 5;

	public LogicalTypeUSZip5(PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(String trimmed, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		return compressed.length() == 5 && compressed.toString().equals("\\d{5}");
	}

	@Override
	public synchronized boolean initialize(Locale locale) {
		super.initialize(locale);

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
	public String nextRandom() {
		if (zipArray == null)
			zipArray = zips.toArray(new String[zips.size()]);
		return zipArray[random.nextInt(zips.size())];
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public String getRegExp() {
		return minLength == 5 ? REGEXP_CONSTANT : REGEXP_VARIABLE;
	}

	@Override
	public Type getBaseType() {
		return PatternInfo.Type.LONG;
	}

	@Override
	public boolean isValid(String input) {
		int len = input.length();

		if (len > 5 || len < 3)
			return false;

		if (len < 5) {
			input = (len == 3 ? "00" : "0") + input;
			minLength = 3;
		}

		return zips.contains(input);
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, StringFacts stringFacts, Map<String, Long> cardinality, Map<String, Long> outliers) {
		String upperDataStreamName = dataStreamName.toUpperCase();
		boolean zipName = dataStreamName != null &&
				(upperDataStreamName.contains("ZIP") || upperDataStreamName.contains("POSTALCODE") || upperDataStreamName.contains("POSTCODE"));
		return (cardinality.size() < 5 && !zipName) || (double)matchCount/realSamples < getThreshold()/100.0 ? getRegExp() : null;
	}
}
