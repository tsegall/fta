package com.cobber.fta.plugins;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;
import java.util.Map;

import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PatternInfo;

/**
 * Plugin to detect URLs.
 */
public class LogicalTypeURL extends LogicalTypeInfinite {
	public final static String SEMANTIC_TYPE = "URL";
	public final static String REGEXP = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";

	@Override
	public boolean initialize(Locale locale) {
		threshold = 95;

		return true;
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public String getRegExp() {
		return 	"^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
	}

	@Override
	public boolean isRegExpComplete() {
		return true;
	}

	@Override
	public PatternInfo.Type getBaseType() {
		return PatternInfo.Type.STRING;
	}

	@Override
	public boolean isValid(String input) {
		try {
			final URL url = new URL(input);
			url.toURI();
			return true;
		} catch (MalformedURLException | URISyntaxException exception) {
			return false;
		}
	}

	@Override
	public boolean isCandidate(String input, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		return charCounts[':'] != 0 && compressed.indexOf("://") != -1;
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, Map<String, Integer> cardinality, Map<String, Integer> outliers) {
		return (double)matchCount/realSamples >= getThreshold()/100.0 ? null : ".+";
	}
}
