/*
 * Copyright 2017-2022 Tim Segall
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

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.FactsTypeBased;
import com.cobber.fta.LogicalTypeFinite;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.dates.LocaleInfo;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect Day of Week Abbreviations.
 */
public class DOYAbbr extends LogicalTypeFinite {
	private Set<String> days;
	private String[] daysArray;

	public DOYAbbr(final PluginDefinition plugin) throws FileNotFoundException {
		super(plugin);
	}

	@Override
	public boolean initialize(final Locale locale) throws FTAPluginException {
		super.initialize(locale);

		days = LocaleInfo.getShortWeekdays(locale);

		return true;
	}

	@Override
	public String nextRandom() {
		if (daysArray == null)
			daysArray = days.toArray(new String[0]);

		return daysArray[random.nextInt(days.size())];
	}

	@Override
	public Set<String> getMembers() {
		return LocaleInfo.getShortWeekdays(locale);
	}

	@Override
	public String getQualifier() {
		return "DAY.ABBR_" + locale.toLanguageTag();
	}

	@Override
	public String getRegExp() {
		return LocaleInfo.getShortWeekdaysRegExp(locale);
	}

	@Override
	public String isValidSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final FactsTypeBased facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		if (outliers.size() > 1)
			return LocaleInfo.getShortWeekdaysRegExp(locale);

		return (double)matchCount / realSamples >= getThreshold()/100.0 ? null : LocaleInfo.getShortWeekdaysRegExp(locale);
	}
}
