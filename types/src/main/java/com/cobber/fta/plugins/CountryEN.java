/*
 * Copyright 2017-2025 Tim Segall
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

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeFiniteSimpleExternal;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect Country names. (English-language only).
 */
public class CountryEN extends LogicalTypeFiniteSimpleExternal {
	/**
	 * Construct a plugin to detect Country names based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public CountryEN(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		if (matchCount < 50 && outliers.size() > Math.sqrt(getMembers().size()))
			return new PluginAnalysis(defn.backout);

		final int headerConfidence = getHeaderConfidence(context.getStreamName());

		if (headerConfidence <= 0 && (realSamples < 10 || cardinality.size() == 1))
			return new PluginAnalysis(defn.backout);

		if ((double)matchCount / realSamples >= getThreshold()/100.0)
			return PluginAnalysis.OK;

		return new PluginAnalysis(defn.backout);
	}
}
