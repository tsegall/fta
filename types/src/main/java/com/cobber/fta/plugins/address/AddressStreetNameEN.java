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

import java.util.HashSet;
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
import com.cobber.fta.core.Utils;
import com.cobber.fta.core.WordProcessor;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect a Street Name (no number!). (English-language only).
 * Note: This will also detect a Street Intersection.
 */
public class AddressStreetNameEN extends LogicalTypeInfinite {
	private boolean multiline;
	private Set<String> addressMarkers;
	private Set<String> markersSeen;
	private final WordProcessor wordProcessor = new WordProcessor().withAdditionalBreakChars("-#").withAdditionalKillChars("'");

	/**
	 * Construct a plugin to detect a Street Name based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public AddressStreetNameEN(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		final String simpleAddressMarkers[] = { "Street", "St", "Road", "Rd", "Rd.", "Avenue", "Ave", "Terrace", "Drive" };

		return AddressCommon.SAMPLE_STREETS[getRandom().nextInt(AddressCommon.SAMPLE_STREETS.length)] + ' ' + simpleAddressMarkers[getRandom().nextInt(simpleAddressMarkers.length)];
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		addressMarkers = new SingletonSet(new Content("resource", "/reference/en_street_markers.csv")).getMembers();
		markersSeen = new HashSet<>();

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
		final List<String> words = wordProcessor.asWords(trimmedUpper);
		final int wordCount = words.size();

		if (wordCount == 0)
			return false;

		if (wordCount <= 2 && "BROADWAY".equals(words.get(0)))
			return true;

		if (wordCount < 2 || trimmedUpper.indexOf(' ') == -1)
			return false;

		int extrasPresent = 0;
		for (int i = wordCount - 1; i >= 0; i--)
			if (AddressCommon.isModifier(words.get(i), i == wordCount - 1))
				extrasPresent += 2;
			else if (AddressCommon.isDirection(words.get(i)))
				extrasPresent++;

		final boolean intersection = trimmedUpper.contains("/") || trimmedUpper.contains("&") || trimmedUpper.contains(" AND ");
		if (!intersection && wordCount > 4 + extrasPresent || wordCount > 8 + extrasPresent)
			return false;

		final String first = words.get(0);
		final String second = words.get(1);

		// Check for something like 'Interstate 23'
		if (AddressCommon.isInitialMarker(first) && Utils.isNumeric(second))
			return true;

		// Check for something like 'W 13th'
		if (AddressCommon.isDirection(first) && AddressCommon.numericStreetName(second))
			return true;

		final boolean isAddressNumber = AddressCommon.isAddressNumber(first);

		// If the first word looks like a number then the next better be an address marker
		if (isAddressNumber && !addressMarkers.contains(second))
			return false;

		// These commonly appear at the front - e.g. 'Avenue of the Americas'
		if (AddressCommon.isInitialMarker(first) || (AddressCommon.isDirection(first) && AddressCommon.isInitialMarker(second)))
			return true;

		// If there is no modifier (e.g. Flat, Suite, Building, ... ) then one of the last two words should be an Address Marker
		if (!intersection && extrasPresent == 0) {
			if (wordCount >= 3)
				return addressMarkers.contains(words.get(wordCount - 1)) || addressMarkers.contains(words.get(wordCount - 2));
			else
				return addressMarkers.contains(words.get(wordCount - 1));
		}

		int markers = 0;
		for (int i = 0; i < wordCount; i++) {
			final String word = words.get(i);
			if (addressMarkers.contains(word)) {
				markersSeen.add(word);
				markers++;
			}
		}

		return intersection ? markers >= 2 : markers >= 1;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		final String rubbish = "{}[]=+";
		for (int i = 0; i < rubbish.length(); i++)
			if (charCounts[rubbish.charAt(i)] != 0)
				return false;

		return validation(trimmed.toUpperCase(Locale.ENGLISH), true);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		// Don't declare success if the header does not look good and we only have a few sample OR the uniqueness of the set is extremely low
		if (getHeaderConfidence(context) <= 85 && realSamples < 10 ||
				getHeaderConfidence(context) <= 85 && markersSeen.size() < 2 ||
				getHeaderConfidence(context) < 99 && cardinality.size() * 100 / realSamples < 1)
			return PluginAnalysis.SIMPLE_NOT_OK;
		return getConfidence(matchCount, realSamples, context) >= getThreshold()/100.0 ? PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		final int headerConfidence = getHeaderConfidence(context);
		double confidence = (double)matchCount/realSamples;

		// Boost based on how much we like the header
		if (headerConfidence >= 99)
			confidence = Math.min(confidence + 0.20, 1.0);
		else if (headerConfidence >= 90)
			confidence = Math.min(confidence + Math.min((1.0 - confidence)/2, 0.15), 1.0);

		return confidence;
	}
}
