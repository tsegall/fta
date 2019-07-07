package com.cobber.fta.plugins;

import java.util.Locale;
import java.util.Map;

import com.cobber.fta.LogicalTypeCode;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PatternInfo;
import com.cobber.fta.PatternInfo.Type;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.StringFacts;

/**
 * Plugin to detect 'Last Name, First Name'.
 */
public class LogicalTypeNameFirstLast extends LogicalTypeInfinite {
	public final static String SEMANTIC_TYPE = "NAME.FIRST_LAST";
	public final static String REGEXP = "[- \\p{isAlphabetic}]+ [- \\\\p{isAlphabetic}]+";
	private static LogicalTypeCode logicalFirst;
	private static LogicalTypeCode logicalLast;

	public LogicalTypeNameFirstLast(PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(Locale locale) {
		super.initialize(locale);

		PluginDefinition pluginFirst = new PluginDefinition("NAME.FIRST", "com.cobber.fta.plugins.LogicalTypeFirstName");
		logicalFirst = LogicalTypeCode.newInstance(pluginFirst, Locale.getDefault());
		PluginDefinition pluginLast = new PluginDefinition("NAME.LAST", "com.cobber.fta.plugins.LogicalTypeLastName");
		logicalLast = LogicalTypeCode.newInstance(pluginLast, Locale.getDefault());

		threshold = 95;

		return true;
	}

	@Override
	public String nextRandom() {
		return logicalFirst.nextRandom() + " " + logicalLast.nextRandom();
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
		String trimmed = input.trim();
		int lastSpace = trimmed.lastIndexOf(' ');
		if (lastSpace == -1)
			return false;

		boolean processingLast = false;
		int len = trimmed.length();
		int dashes = 0;
		int apostrophe = 0;
		int alphas = 0;
		for (int i = 0; i < len; i++) {
			if (i == lastSpace) {
				processingLast = true;
				alphas = 0;
				dashes = 0;
				continue;
			}
			char ch = trimmed.charAt(i);
			if (Character.isAlphabetic(ch)) {
				alphas++;
				continue;
			}
			if (ch == '\'') {
				apostrophe++;
				if (processingLast && apostrophe == 1)
					continue;
				return false;
			}
			if (ch == '-') {
				dashes++;
				if (dashes == 2)
					return false;
				continue;
			}
			if (ch == '.' && alphas == 1)
				continue;

			return false;
		}

		return logicalFirst.isValid(trimmed.substring(0, lastSpace)) || logicalLast.isValid(trimmed.substring(lastSpace + 1));
	}

	@Override
	public boolean isCandidate(String trimmed, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		return trimmed.length() >= 5 && trimmed.length() <= 30 && (charCounts[' '] == 1 || charCounts[' '] == 2);
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, StringFacts stringFacts,
			Map<String, Long> cardinality, Map<String, Long> outliers) {
		return (double) matchCount / realSamples >= getThreshold() / 100.0 ? null : ".+";
	}
}

