/*
 * Copyright 2017-2023 Tim Segall
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

import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeFinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect full Month Names.
 */
public class MonthFull extends LogicalTypeFinite {
	private Set<String> months;
	private String[] monthsArray;

	/**
	 * Construct a plugin to detect Month full names based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public MonthFull(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		months = localeInfo.getMonths().keySet();

		return true;
	}

	@Override
	public String nextRandom() {
		if (monthsArray == null)
			monthsArray = months.toArray(new String[0]);

		return monthsArray[getRandom().nextInt(months.size())];
	}

	@Override
	public Set<String> getMembers() {
		return localeInfo.getMonths().keySet();
	}

	@Override
	public String getSemanticType() {
		return "MONTH.FULL_" + locale.toLanguageTag();
	}

	@Override
	public String getRegExp() {
		return localeInfo.getMonthsRegExp();
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		if (outliers.size() > 1)
			return new PluginAnalysis(localeInfo.getMonthsRegExp());

		if ((double)matchCount / realSamples >= getThreshold()/100.0)
			return PluginAnalysis.OK;

		return new PluginAnalysis(localeInfo.getMonthsRegExp());
	}
}

