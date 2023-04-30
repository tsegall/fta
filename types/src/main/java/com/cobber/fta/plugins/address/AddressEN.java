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
package com.cobber.fta.plugins.address;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

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
import com.cobber.fta.core.WordProcessor;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect an Address line. (English-language only).
 */
public class AddressEN extends LogicalTypeInfinite {
	private boolean multiline;
	private SingletonSet addressMarkersRef;
	private Set<String> addressMarkers;
	private String country;
	private Pattern poBox;
	private WordProcessor wordProcessor;

	/**
	 * Construct a plugin to detect an Address based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public AddressEN(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		final String[] streets = {
				"Main",  "Lakeside", "Pennsylvania", "Hill", "Croydon", "Buchanan", "Riverside", "Flushing",
				"Jefferson", "Randolph", "North Point", "Massachusetts", "Meadow", "Central", "Lincoln", "Eight Mile",
				"4th", "Flower", "High", "3rd", "12th", "D", "Piedmont", "Chaton", "Kenwood", "Sycamore Lake",
				"Euclid", "Cedarstone", "Carriage", "Isaacs Creek", "Happy Hollow", "Armory", "Bryan", "Charack",
				"Atha", "Bassel", "Overlook", "Chatham", "Melville", "Stone", "Dawson", "Pringle", "Federation",
				"Winifred", "Pratt", "Hillview", "Rosemont", "Romines Mill", "School House", "Candlelight"
		};
		final String simpleAddressMarkers[] = { "Street", "St", "Road", "Rd", "Rd.", "Avenue", "Ave", "Terrace", "Drive" };

		return String.valueOf(1 + random.nextInt(999)) + ' ' + streets[random.nextInt(streets.length)] + ' ' + simpleAddressMarkers[random.nextInt(simpleAddressMarkers.length)];
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		addressMarkersRef = new SingletonSet("resource", "/reference/en_street_markers.csv");
		addressMarkers = addressMarkersRef.getMembers();

		country = locale.getCountry().toUpperCase(Locale.ROOT);

		poBox = Pattern.compile(AddressCommon.POBOX);

		wordProcessor = new WordProcessor().withAdditionalBreakChars("-#").withAdditionalKillChars("'");
		// Australia commonly uses unit/number in the address so allow '/' to be part of words
		if ("AU".equals(country))
			wordProcessor = wordProcessor.withAdditionalWordChars("/");

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

		final int length = inputUpper.length();

		// Attempt to fail fast
		if (length > 60 || length < 5)
			return false;

		return validation(inputUpper, detectMode);
	}

	private boolean validation(final String trimmedUpper, final boolean detectMode) {
		final List<String> words = wordProcessor.asWords(trimmedUpper);
		final int wordCount = words.size();
		int addressMarkerIndex = -1;
		int directionIndex = -1;
		int modifierIndex = -1;
		int blockIndex = -1;
		int start = 0;
		int firstSpace;

		if (wordCount < 2 || (firstSpace = trimmedUpper.indexOf(' ')) == -1)
			return false;

		String initialWord = words.get(0);

		// Handle an initial Unit #
		if ("#".equals(initialWord) && Utils.isNumeric(words.get(1))) {
			if (wordCount < 3)
				return false;
			modifierIndex = 0;
			start = 2;
			initialWord = words.get(2);
		}
		final boolean initialNumeric = AddressCommon.isAddressNumber(initialWord);

		if (start == 0 && words.size() == 2 && initialNumeric && "BROADWAY".equals(words.get(1)))
			return true;

		// If we have a P.O. Box then we are all good
		if (trimmedUpper.charAt(0) == 'P' && poBox.matcher(trimmedUpper).find())
			return true;

		if (wordCount < 3 || trimmedUpper.lastIndexOf(' ') == firstSpace)
			return false;

		// If don't start with a number and it is not a modifier like Apartment, Suite, ... then we are done
		if (start == 0 && !AddressCommon.isModifier(initialWord, false) && !addressMarkers.contains(initialWord) && !initialNumeric)
			return false;

		for (int i = start; i < wordCount; i++) {
			final String word = words.get(i);
			if ((addressMarkers.contains(word) && i >= 2) || (AddressCommon.isInitialMarker(word) && i >= 1)) {
				if (addressMarkerIndex == -1)
					addressMarkerIndex = i;
			}
			else if (AddressCommon.isDirection(word)) {
				if (directionIndex == -1)
					directionIndex = i;
			}
			else if (AddressCommon.isModifier(word, i == wordCount - 1))
				modifierIndex = i;
			else if ("BLOCK".equals(word))
				blockIndex = i;
			else if ("BOX".equals(word) && i + 1 < wordCount && Character.isDigit(words.get(i).charAt(0)))
				return true;
		}

		if (modifierIndex == 0 && addressMarkerIndex != -1 && addressMarkerIndex - 2 > 0 && Utils.isNumeric(words.get(addressMarkerIndex - 2)))
			return true;

		if (initialNumeric && (addressMarkerIndex != -1 || modifierIndex != -1 || blockIndex != -1))
			return true;

		// If it looks like '1280 SE XXXXX' declare it good
		if (initialNumeric && directionIndex == 1)
			return true;

		return false;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return validation(trimmed.toUpperCase(Locale.ENGLISH), true);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		return getConfidence(matchCount, realSamples, context) >= getThreshold()/100.0 ? PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		final String dataStreamName = context.getStreamName();

		// We really don't want to classify Line 2/Line 3 of an address as a Line 1
		if (dataStreamName.length() > 1) {
			final char lastChar = dataStreamName.charAt(dataStreamName.length() - 1);
			if (Character.isDigit(lastChar) && lastChar != '1')
				return 0.0;
		}

		final String[] semanticTypes = context.getSemanticTypes();
		final int current = context.getStreamIndex();
		final boolean previousSemanticTypeInfoAvailable = semanticTypes != null && current >= 1;

		if (context.isPreviousSemanticType("STREET_ADDRESS_EN", "STREET_ADDRESS2_EN", "STREET_ADDRESS3_EN") ||
				// If we have all the headers then we can check if this one is less likely to be the primary address field than the previous one
				(!previousSemanticTypeInfoAvailable && (current >= 1 && getHeaderConfidence(context.getCompositeStreamNames()[current - 1]) > getHeaderConfidence(dataStreamName))))
			return 0.0;

		final int headerConfidence = getHeaderConfidence(dataStreamName);
		double confidence = (double)matchCount/realSamples;

		// Does the next field have a Semantic Type that indicates it is a Street Address 2
		if (confidence > 0.1 && context.isNextSemanticType("STREET_ADDRESS2_EN")) {
			// If this header is the same as the next but with a 2 switched for a '1' then we are extremely confident
			final String nextStreamName = context.getCompositeStreamNames()[current + 1];

			final int index = nextStreamName.indexOf('2');
			if (index != -1 && dataStreamName.equals(nextStreamName.substring(0, index) + '1' + nextStreamName.substring(index + 1)))
				return 0.95;
		}

		// Boost based on how much we like the header
		if (headerConfidence >= 99)
			confidence = Math.min(confidence + 0.20, 1.0);
		else if (headerConfidence >= 90)
			confidence = Math.min(confidence + 0.10, 1.0);
		else if (headerConfidence >= 85)
			confidence = Math.min(confidence + 0.05, 1.0);

		return confidence;
	}
}
