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
package com.cobber.fta.plugins.address;

import java.util.List;
import java.util.Locale;
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
import com.cobber.fta.core.WordProcessor;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect a Street Name (no number or marker). (English-language only).
 */
public class AddressStreetNameBareEN extends LogicalTypeInfinite {
	private boolean multiline;
	private SingletonSet addressMarkersRef;
	private Set<String> addressMarkers;
	private final WordProcessor wordProcessor = new WordProcessor().withAdditionalBreakChars("-#").withAdditionalKillChars("'");

	/**
	 * Construct a plugin to detect a Street Name based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public AddressStreetNameBareEN(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		return AddressCommon.SAMPLE_STREETS[getRandom().nextInt(AddressCommon.SAMPLE_STREETS.length)];
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		addressMarkersRef = new SingletonSet(new Content("resource", "/reference/en_street_markers.csv"));
		addressMarkers = addressMarkersRef.getMembers();

		return true;
	}

	@Override
	public String getRegExp() {
		return multiline ? "(?s).+" : ".+";
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		final String inputUpper = input.trim().toUpperCase(Locale.ENGLISH);

		return validation(inputUpper, detectMode, count);
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		final int headerConfidence = getHeaderConfidence(context);
		double confidence = (double)matchCount/realSamples;
		if (headerConfidence >= 99) {
			if (context.isNextSemanticType("STREET_MARKER_EN"))
				confidence = Math.min(confidence + 0.20, 1.0);
			return confidence;
		}
		if (headerConfidence >= 60 && context.isNextSemanticType("STREET_MARKER_EN")) {
			if (context.isPreviousSemanticType("DIRECTION", "STREET_NUMBER"))
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

		final String firstWord = words.get(0);
		if (wordCount == 1 && ("GREEN".equals(firstWord) || "PARK".equals(firstWord) || "BROADWAY".equals(firstWord)))
			return true;

		if (wordCount == 2 && ("VIEW".equals(words.get(1)) || "HILL".equals(words.get(1))))
			return true;

		if (wordCount > 3 || (wordCount == 1 && addressMarkers.contains(firstWord)))
			return false;

		for (int i = wordCount - 1; i >= 1; i--)
			if (addressMarkers.contains(words.get(i)))
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
