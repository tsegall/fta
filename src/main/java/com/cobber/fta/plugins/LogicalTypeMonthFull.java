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
 * Plugin to detect full Month Names.
 */
public class LogicalTypeMonthFull extends LogicalTypeFinite {
	private Set<String> months = null;
	private String[] monthsArray = null;

	public LogicalTypeMonthFull(PluginDefinition plugin) throws FileNotFoundException {
		super(plugin);
	}

	@Override
	public boolean initialize(Locale locale) {
		super.initialize(locale);

		threshold = 95;

		months = LocaleInfo.getMonths(locale).keySet();

		return true;
	}

	@Override
	public String nextRandom() {
		if (monthsArray == null)
			monthsArray = months.toArray(new String[months.size()]);

		return monthsArray[random.nextInt(months.size())];
	}

	@Override
	public Set<String> getMembers() {
		return LocaleInfo.getMonths(locale).keySet();
	}

	@Override
	public String getQualifier() {
		return "MONTH.FULL_" + locale.toLanguageTag();
	}

	@Override
	public String getRegExp() {
		return LocaleInfo.getMonthsRegExp(locale);
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, StringFacts stringFacts, Map<String, Long> cardinality, Map<String, Long> outliers) {
		if (outliers.size() > 1)
			return LocaleInfo.getMonthsRegExp(locale);

		return (double)matchCount / realSamples >= getThreshold()/100.0 ? null : LocaleInfo.getMonthsRegExp(locale);
	}
}
