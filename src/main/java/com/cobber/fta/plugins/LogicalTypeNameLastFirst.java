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
 * Plugin to detect '&lt;Last Name&gt;, &lt;First Name&gt;'.
 */
public class LogicalTypeNameLastFirst extends LogicalTypeInfinite {
	public final static String SEMANTIC_TYPE = "NAME.LAST_FIRST";
	public final static String REGEXP = "[- \\p{isAlphabetic}]+, ?[- \\\\p{isAlphabetic}]+";
	private static LogicalTypeCode logicalFirst;
	private static LogicalTypeCode logicalLast;

	public LogicalTypeNameLastFirst(PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(Locale locale) {
		super.initialize(locale);

		threshold = 95;

		return true;
	}

	@Override
	public String nextRandom() {
		if (logicalFirst == null) {
			PluginDefinition pluginFirst = new PluginDefinition("NAME.FIRST", "com.cobber.fta.plugins.LogicalTypeFirstName");
			logicalFirst = LogicalTypeCode.newInstance(pluginFirst, Locale.getDefault());
			PluginDefinition pluginLast = new PluginDefinition("NAME.LAST", "com.cobber.fta.plugins.LogicalTypeLastName");
			logicalLast = LogicalTypeCode.newInstance(pluginLast, Locale.getDefault());
		}

		return logicalLast.nextRandom() + ", " + logicalFirst.nextRandom();
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
		int comma = trimmed.indexOf(',');
		if (comma == -1 || comma == 0 || comma == trimmed.length() - 1)
			return false;

		boolean processingLast = true;
		int len = trimmed.length();
		int spaces = 0;
		int dashes = 0;
		int apostrophe = 0;
		int alphas = 0;
		for (int i = 0; i < len; i++) {
			if (i == comma) {
				processingLast = false;
				alphas = 0;
				spaces = 0;
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
			if (ch == ' ') {
				alphas = 0;
				if (i != comma + 1)
					spaces++;
				if (spaces == 2)
					return false;
				continue;
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

		return true;
	}

	@Override
	public boolean isCandidate(String trimmed, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		return trimmed.length() >= 5 && trimmed.length() <= 30 && charCounts[','] == 1;
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, StringFacts stringFacts,
			Map<String, Long> cardinality, Map<String, Long> outliers) {
		return getConfidence(matchCount, realSamples, dataStreamName) >= getThreshold()/100.0 ? null : ".+";
	}

	@Override
	public double getConfidence(long matchCount, long realSamples, String dataStreamName) {
		double is = (double)matchCount/realSamples;
		if (matchCount != realSamples && getHeaderConfidence(dataStreamName) != 0)
			return is + (1.0 - is)/2;
		else
			return is;
	}
}

