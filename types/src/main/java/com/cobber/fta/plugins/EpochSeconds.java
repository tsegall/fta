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
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.Utils;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect an Epoch time in seconds.
 *
 */
public class EpochSeconds extends LogicalTypeInfinite {
	private long minObservation = Long.MAX_VALUE;
	private long maxObservation = Long.MIN_VALUE;
	private static final long YEAR_2000_SECONDS = 946_684_800L;

	/**
	 * Construct a plugin to detect an Epoch time in seconds based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public EpochSeconds(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return validate(trimmed);
	}

	private boolean validate(final String trimmed) {

		if (trimmed.length() != 10 || !Utils.isNumeric(trimmed))
			return false;

		final long observation = Long.parseLong(trimmed);
		if (observation * 1000 > System.currentTimeMillis())
			return false;

		if (observation > maxObservation)
			maxObservation = observation;
		if (observation < minObservation)
			minObservation = observation;

		return true;
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		return true;
	}

	@Override
	public String nextRandom() {
		return String.valueOf(System.currentTimeMillis() / 1000 - getRandom().nextInt(26 * 60 * 60));
	}

	@Override
	public String getRegExp() {
		return "\\d{10}";
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		return validate(input.trim());
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams,
			AnalysisConfig analysisConfig) {
		// If we are not sure about the header and it less than YEAR 2000 or > now() call it a day
		if (getHeaderConfidence(context) < 99 && (cardinality.size() < 10 || minObservation < YEAR_2000_SECONDS || maxObservation > System.currentTimeMillis() / 1000))
			return PluginAnalysis.SIMPLE_NOT_OK;

		return getConfidence(matchCount, realSamples, context) >= getThreshold()/100.0 ? PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}
}
