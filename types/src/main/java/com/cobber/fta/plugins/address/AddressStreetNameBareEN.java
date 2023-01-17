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
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.Utils;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect a Street Name (no number or marker). (English-language only).
 */
public class AddressStreetNameBareEN extends LogicalTypeInfinite {
	private boolean multiline;
	private SingletonSet addressMarkersRef;
	private Set<String> addressMarkers;

	/**
	 * Construct a plugin to detect a Street Name based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public AddressStreetNameBareEN(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		return AddressCommon.sampleStreets[random.nextInt(AddressCommon.sampleStreets.length)];
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		addressMarkersRef = new SingletonSet("resource", "/reference/en_street_markers.csv");
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

		return validation(inputUpper, detectMode);
	}

	private boolean validation(final String trimmedUpper, final boolean detectMode) {
		final List<String> words = Utils.asWords(trimmedUpper, "-#");
		final int wordCount = words.size();

		if (words.size() > 3)
			return false;

		for (int i = wordCount - 1; i >= 0; i--)
			if (addressMarkers.contains(words.get(i)))
				return false;

		return true;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return validation(trimmed.toUpperCase(Locale.ENGLISH), true);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		return getConfidence(matchCount, realSamples, context) >= getThreshold()/100.0 ? PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}
}
