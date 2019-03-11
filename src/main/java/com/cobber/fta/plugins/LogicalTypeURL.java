package com.cobber.fta.plugins;

import java.util.Locale;
import java.util.Map;

import org.apache.commons.validator.routines.UrlValidator;

import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PatternInfo;
import com.cobber.fta.StringFacts;

/**
 * Plugin to detect URLs.
 */
public class LogicalTypeURL extends LogicalTypeInfinite {
	public final static String SEMANTIC_TYPE = "URL";
	public final static String REGEXP_PROTOCOL = "(https?|ftp|file)";
	public final static String REGEXP_RESOURCE = "[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
	static UrlValidator validator = null;
	static {
		validator = UrlValidator.getInstance();
	}
	int[] protocol = new int[2];

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
		if (protocol[0] != 0)
			return protocol[1] != 0 ? REGEXP_PROTOCOL + "?" + REGEXP_RESOURCE : REGEXP_PROTOCOL + REGEXP_RESOURCE;

		return REGEXP_RESOURCE;
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
		int index = 0;
		if (input.indexOf("://") == -1) {
			input = "http://" + input;
			index = 1;
		}

		boolean ret = validator.isValid(input);
		if (ret)
			protocol[index]++;

		return ret;
	}

	@Override
	public boolean isCandidate(String trimmed, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		// Quickly rule out rubbish
		if (charCounts[' '] != 0)
				return false;

		// Does it have a protocol?
		if (charCounts[':'] != 0 && compressed.indexOf("://") != -1)
			return true;

		return validator.isValid("http://" + trimmed);
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, StringFacts stringFacts, Map<String, Integer> cardinality, Map<String, Integer> outliers) {
		return (double)matchCount/realSamples >= getThreshold()/100.0 ? null : ".+";
	}
}
