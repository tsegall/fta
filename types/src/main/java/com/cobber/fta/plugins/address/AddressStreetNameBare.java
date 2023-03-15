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
package com.cobber.fta.plugins.address;

import java.util.List;
import java.util.Locale;

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
import com.cobber.fta.core.WordProcessor;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect a Street Name (no number or marker). (Non-English).
 */
public class AddressStreetNameBare extends LogicalTypeInfinite {
	private static final String SEMANTIC_TYPE = "STREET_NAME_BARE_";
	private String language;
	private WordProcessor wordProcessor = new WordProcessor().withAdditionalBreakChars("-#").withAdditionalKillChars("'");

	/**
	 * Construct a plugin to detect a Street Name based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public AddressStreetNameBare(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		return AddressCommon.sampleStreets[random.nextInt(AddressCommon.sampleStreets.length)];
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		language = locale.getLanguage().toUpperCase(Locale.ROOT);

		return true;
	}

	@Override
	public String getRegExp() {
		return ".+";
	}

	@Override
	public String getSemanticType() {
		return SEMANTIC_TYPE + language;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		final String inputUpper = input.trim().toUpperCase(locale);

		return validation(inputUpper, detectMode, count);
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		final String dataStreamName = context.getStreamName();

		final int headerConfidence = getHeaderConfidence(dataStreamName);
		double confidence = (double)matchCount/realSamples;
		if (headerConfidence >= 99) {
			if (context.isNextSemanticType("STREET_NUMBER"))
				confidence = Math.min(confidence + 0.20, 1.0);
			return confidence;
		}

		return 0.0;
	}

	private boolean validation(final String trimmedUpper, final boolean detectMode, final long count) {
		final List<String> words = wordProcessor.asWords(trimmedUpper);
		final int wordCount = words.size();

		if (wordCount == 0)
			return false;

		for (int i = wordCount - 1; i >= 0; i--)
			if (Utils.isNumeric(words.get(i)))
				return false;

		return true;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return validation(trimmed.toUpperCase(Locale.ENGLISH), true, 0);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		if (getConfidence(matchCount, realSamples, context) < getThreshold()/100.0)
			return PluginAnalysis.SIMPLE_NOT_OK;

		return PluginAnalysis.OK;
	}
}
