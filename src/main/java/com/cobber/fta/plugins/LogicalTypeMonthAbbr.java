/*
 * Copyright 2017-2020 Tim Segall
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cobber.fta.plugins;

import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.LogicalTypeFinite;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.TypeFacts;
import com.cobber.fta.dates.LocaleInfo;

/**
 * Plugin to detect Month Abbreviations.
 */
public class LogicalTypeMonthAbbr extends LogicalTypeFinite {
	private Set<String> months = null;
	private String[] monthsArray = null;

	public LogicalTypeMonthAbbr(PluginDefinition plugin) throws FileNotFoundException {
		super(plugin);
	}

	@Override
	public boolean initialize(Locale locale) {
		super.initialize(locale);

		threshold = 95;

		months = LocaleInfo.getShortMonths(locale).keySet();

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
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, TypeFacts facts, Map<String, Long> cardinality, Map<String, Long> outliers) {
		if (outliers.size() > 1)
			return LocaleInfo.getShortMonthsRegExp(locale);

		return (double)matchCount / realSamples >= getThreshold()/100.0 ? null : LocaleInfo.getShortMonthsRegExp(locale);
	}
}
