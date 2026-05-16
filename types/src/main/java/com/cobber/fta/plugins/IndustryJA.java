/*
 * Copyright 2017-2026 Tim Segall
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
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect Industry Names (Japanese Language).
 */
public class IndustryJA extends LogicalTypeInfinite {
	public static final String REGEXP = ".+";

	// 業 covers most JSIC categories; 産業 and 業界 cover compound/informal forms
	private static final String INDUSTRY_SUFFIXES = "業";
	private static final String[] INDUSTRY_COMPOUND_SUFFIXES = { "産業", "業界", "業種" };

	private SingletonSet industriesRef;
	private Set<String> industries;

	public IndustryJA(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		return industriesRef.getRandom(getRandom());
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);
		industriesRef = new SingletonSet(new Content("resource", "/reference/ja_industries.csv"));
		industries = industriesRef.getMembers();
		return true;
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		final String trimmed = input.trim();
		final int len = trimmed.length();

		if (len < 2 || len > 30)
			return false;

		if (industries.contains(trimmed))
			return true;

		// Single-kanji suffix 業 covers manufacturing, retail, agriculture, finance, etc.
		if (INDUSTRY_SUFFIXES.indexOf(trimmed.charAt(len - 1)) >= 0)
			return true;

		// Multi-character suffixes: 産業, 業界, 業種
		for (final String suffix : INDUSTRY_COMPOUND_SUFFIXES)
			if (trimmed.endsWith(suffix))
				return true;

		return false;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return isValid(trimmed, true, 0);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples,
			final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers,
			final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		if (getHeaderConfidence(context) >= 99)
			return PluginAnalysis.OK;

		if (getHeaderConfidence(context) <= 0 || cardinality.size() < 5 || realSamples < 5)
			return PluginAnalysis.SIMPLE_NOT_OK;

		if (getConfidence(matchCount, realSamples, context) >= getThreshold() / 100.0)
			return PluginAnalysis.OK;

		return PluginAnalysis.SIMPLE_NOT_OK;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		final double confidence = (double) matchCount / realSamples;

		if (getHeaderConfidence(context) >= 99)
			return Math.min(confidence + 0.20, 1.0);

		return confidence;
	}
}
