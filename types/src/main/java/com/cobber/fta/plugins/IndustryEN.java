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

import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.SingletonSet;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.WordProcessor;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect Industry Names. (English-language only).
 */
public class IndustryEN extends LogicalTypeInfinite {
	/** The Regular Expression for this Semantic type. */
	public static final String REGEXP = ".+";

	private SingletonSet hotWordsRef;
	private Set<String> hotWords;
	private SingletonSet industriesRef;
	private Set<String> industries;

	private WordProcessor wordProcessor = new WordProcessor();

	/**
	 * Construct a Industry plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public IndustryEN(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		return industriesRef.getRandom(random);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		hotWordsRef = new SingletonSet("resource", "/reference/en_industry_hotwords.csv");
		hotWords = hotWordsRef.getMembers();

		industriesRef = new SingletonSet("resource", "/reference/en_industries.csv");
		industries = industriesRef.getMembers();

		return true;
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		final String trimmedUpper = input.trim().toUpperCase(Locale.ENGLISH);

		if (industries.contains(trimmedUpper))
			return true;

		final int length = input.length();

		// Attempt to fail fast
		if (length > 200 || length < 2)
			return false;

		final List<String> words = wordProcessor.asWords(trimmedUpper);

		if (words.size() > 10)
			return false;

		for (final String word : words)
			if (hotWords.contains(word))
				return true;

		return false;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return isValid(trimmed, true, 0);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		if (getHeaderConfidence(context.getStreamName()) >= 99)
			return PluginAnalysis.OK;

		final int minCardinality = 5;
		final int minSamples = 5;

		if (getHeaderConfidence(context.getStreamName()) <= 0 || cardinality.size() < minCardinality || realSamples < minSamples)
			return PluginAnalysis.SIMPLE_NOT_OK;

		if (getConfidence(matchCount, realSamples, context) >= getThreshold()/100.0)
			return PluginAnalysis.OK;

		return PluginAnalysis.SIMPLE_NOT_OK;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		final double confidence = (double)matchCount/realSamples;

		if (getHeaderConfidence(context.getStreamName()) >= 99)
			return Math.min(confidence + 0.20, 1.0);

		return confidence;
	}

}
