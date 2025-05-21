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
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.Utils;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect Identifiers.
 *
 * This plugin is SPECIAL it is never used inline - it is only used after all other analysis has been completed
 *
 */
public class Identifier extends LogicalTypeInfinite {
	private final int THRESHOLD_MONOTONIC = 50;
	private final int THRESHOLD_UNIQUENESS_TEST = 50;

	/** The Regular Expression for this Semantic type. */
	private static final String REGEXP = ".+";
	private int nextID = 0;

	/**
	 * Construct a plugin to detect Identifiers based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public Identifier(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		return true;
	}

	@Override
	public String nextRandom() {
		return String.valueOf(++nextID);
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		return input.length() < 40;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		// This plugin is SPECIAL it is never used inline - it is only used after all other analysis has been completed
		return false;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		if (facts.uniqueness == null || facts.uniqueness != 1.0 || facts.sampleCount != facts.matchCount)
			return PluginAnalysis.SIMPLE_NOT_OK;

		if (getHeaderConfidence(context.getStreamName()) >= 85)
			return PluginAnalysis.OK;

		// If the type is LONG and we have a reasonable number of samples then we can do further analysis
		if (FTAType.LONG.equals(facts.getMatchTypeInfo().getBaseType())) {
			// We know whether it is monotonic increasing or decreasing - if so declare it good
			if (facts.matchCount >= THRESHOLD_MONOTONIC && (facts.monotonicDecreasing || facts.monotonicIncreasing)
					&& facts.getMinLong() >= 0 && facts.getMaxLong() - facts.getMinLong() == facts.matchCount - 1)
				return PluginAnalysis.OK;
			// Given the size of the Sample Space and the number of samples we can calculate the likelihood that is is unique
			if (facts.matchCount >= THRESHOLD_UNIQUENESS_TEST && Utils.uniquenessProbability((int)(facts.getMaxLong() - facts.getMinLong()), (int)facts.matchCount) > .99)
				return PluginAnalysis.OK;
		}

		return PluginAnalysis.SIMPLE_NOT_OK;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		if (matchCount == 0)
			return 0;

		final int headerConfidence = getHeaderConfidence(context.getStreamName());
		if (headerConfidence >= 85)
			return (double)headerConfidence/100;

		if (realSamples <= 500)
			return .95;
		if (realSamples <= 1000)
			return .97;
		return .99;
	}
}
