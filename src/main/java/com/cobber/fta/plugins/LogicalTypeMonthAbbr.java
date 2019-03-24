package com.cobber.fta.plugins;

import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.LocaleInfo;
import com.cobber.fta.LogicalTypeFinite;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.StringFacts;

/**
 * Plugin to detect Month Abbreviations.
 */
public class LogicalTypeMonthAbbr extends LogicalTypeFinite {
	private static String[] months = null;

	public LogicalTypeMonthAbbr(PluginDefinition plugin) throws FileNotFoundException {
		super(plugin);
	}

	@Override
	public  synchronized boolean initialize(Locale locale) {
		super.initialize(locale);

		threshold = 95;

		months = getMembers().toArray(new String[getMembers().size()]);

		return true;
	}

	@Override
	public String nextRandom() {
		return months[random.nextInt(getMembers().size())];
	}

	@Override
	public Set<String> getMembers() {
		return LocaleInfo.getShortMonths(locale).keySet();
	}

	@Override
	public String getQualifier() {
		return "MONTH.ABBR_" + locale.toLanguageTag();
	}

	@Override
	public String getRegExp() {
		return LocaleInfo.getShortMonthsRegExp(locale);
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, StringFacts stringFacts, Map<String, Integer> cardinality, Map<String, Integer> outliers) {
		if (outliers.size() > 1)
			return LocaleInfo.getShortMonthsRegExp(locale);

		return (double)matchCount / realSamples >= getThreshold()/100.0 ? null : LocaleInfo.getShortMonthsRegExp(locale);
	}
}
