package com.cobber.fta.plugins;

import java.util.Locale;
import java.util.Map;

import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PatternInfo;
import com.cobber.fta.PatternInfo.Type;

/**
 * Plugin to detect GUIDs.
 */
public class LogicalTypeGUID extends LogicalTypeInfinite {
	public final static String SEMANTIC_TYPE = "GUID";
	public final static String REGEXP = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";

	@Override
	public boolean initialize(Locale locale) {
		threshold = 99;

		return true;
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public Type getBaseType() {
		return PatternInfo.Type.STRING;
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public boolean isRegExpComplete() {
		return true;
	}

	@Override
	public boolean isValid(String input) {
		int len = input.length();
		if (len != 36)
			return false;

		if (input.charAt(8) != '-' || input.charAt(13) != '-' || input.charAt(18) != '-' || input.charAt(23) != '-')
			return false;

		for (int i = 0; i < len; i++) {
			if (i == 8 || i == 13 || i == 18 || i == 23)
				continue;
			char ch = input.charAt(i);
			if ((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F'))
				continue;
			return false;
		}

		return true;
	}

	@Override
	public boolean isCandidate(String trimmed, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		return trimmed.length() == 36 && charCounts['-'] == 4;
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, Map<String, Integer> cardinality,
			Map<String, Integer> outliers) {
		return (double) matchCount / realSamples >= getThreshold() / 100.0 ? null : ".+";
	}
}
