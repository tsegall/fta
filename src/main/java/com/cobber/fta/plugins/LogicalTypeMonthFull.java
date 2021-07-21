/*
 * Copyright 2017-2021 Tim Segall
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
import com.cobber.fta.Shapes;
import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.FactsTypeBased;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.dates.LocaleInfo;

/**
 * Plugin to detect full Month Names.
 */
public class LogicalTypeMonthFull extends LogicalTypeFinite {
	private Set<String> months;
	private String[] monthsArray;

	public LogicalTypeMonthFull(final PluginDefinition plugin) throws FileNotFoundException {
		super(plugin);
	}

	@Override
	public boolean initialize(final Locale locale) throws FTAPluginException {
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
	public String isValidSet(final String dataStreamName, final long matchCount, final long realSamples, String currentRegExp, final FactsTypeBased facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final Shapes shapes, AnalysisConfig analysisConfig) {
		if (outliers.size() > 1)
			return LocaleInfo.getMonthsRegExp(locale);

		return (double)matchCount / realSamples >= getThreshold()/100.0 ? null : LocaleInfo.getMonthsRegExp(locale);
	}
}

