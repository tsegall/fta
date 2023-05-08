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
import com.cobber.fta.Content;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.SingletonSet;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect NAICS codes.
 */
public class IndustryNAICS extends LogicalTypeInfinite {
	private SingletonSet codesRef;
	private Set<String> codes;
	private int minLength = Integer.MAX_VALUE;

	/**
	 * Construct plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public IndustryNAICS(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		return codesRef.getRandom(random);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		codesRef = new SingletonSet(new Content("resource", "/reference/naics_code.csv"));
		codes = codesRef.getMembers();

		return true;
	}

	@Override
	public String getRegExp() {
		return minLength == 6 ? "\\d{6}" : "\\d{2,6}";
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		final String trimmed = input.trim();
		if (!validate(trimmed, detectMode, count))
			return false;

		if (trimmed.length() < minLength)
			minLength = trimmed.length();
		return true;
	}

	private boolean validate(final String input, final boolean detectMode, final long count) {
		final int len = input.length();

		// Attempt to fail fast
		if (len > 6 || len < 2)
			return false;

		if (codes.contains(input))
			return true;

		if (len < 6)
			return false;

		// NAICS codes are often stored padded with zeroes
		if (input.endsWith("0000"))
			return codes.contains(input.substring(0, 2));
		if (input.endsWith("000"))
			return codes.contains(input.substring(0, 3));
		if (input.endsWith("00"))
			return codes.contains(input.substring(0, 4));
		if (input.endsWith("0"))
			return codes.contains(input.substring(0, 5));

		return false;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return isValid(trimmed, true, 0);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		if (getConfidence(matchCount, realSamples, context) == 1.0)
			return PluginAnalysis.OK;

		int minCardinality = 10;
		int minSamples = 20;
		if (getHeaderConfidence(context.getStreamName()) > 0) {
			minCardinality = 5;
			minSamples = 5;
		}

		if (cardinality.size() < minCardinality)
			return PluginAnalysis.SIMPLE_NOT_OK;

		if (realSamples < minSamples)
			return PluginAnalysis.SIMPLE_NOT_OK;

		if ((double)matchCount/realSamples >= getThreshold()/100.0)
			return PluginAnalysis.OK;

		return PluginAnalysis.SIMPLE_NOT_OK;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		final double confidence = (double)matchCount/realSamples;

		if (confidence > 0.75 && getHeaderConfidence(context.getStreamName()) >= 99)
			return 1.0;
		else if (getHeaderConfidence(context.getStreamName()) >= 90)
			return Math.min(1.2 * confidence, 1.0);

		return confidence;
	}
}
