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
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect an Address line. (English-language only).
 */
public class AddressEN extends LogicalTypeInfinite {
	/** The Semantic type for this Plugin. */
	public static final String SEMANTIC_TYPE = "STREET_ADDRESS_EN";

	private boolean multiline;
	private SingletonSet addressMarkersRef;
	private Set<String> addressMarkers;
	private String country;

	private Pattern poBox;

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

		return true;
	}

	@Override
	public String getSemanticType() {
		return SEMANTIC_TYPE;
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
	public boolean isValid(final String input, final boolean detectMode) {
		final String inputUpper = input.trim().toUpperCase(Locale.ENGLISH);

		final int length = inputUpper.length();

		// Attempt to fail fast
		if (length > 60 || length < 5)
			return false;

		return validation(inputUpper, detectMode);
	}

	private boolean validation(final String trimmedUpper, final boolean detectMode) {
		// Australia commonly uses unit/number in the address so allow '/' to be part of words
		final List<String> words = Utils.asWords(trimmedUpper, "AU".equals(country) ? "/-#" : "-#");
		final int wordCount = words.size();

		if (wordCount < 2)
			return false;

		final String firstWord = words.get(0);
		if ("BOX".equals(firstWord))
			return true;

		boolean initialNumeric = AddressCommon.isAddressNumber(firstWord);

		if (words.size() == 2 && initialNumeric && "BROADWAY".equals(words.get(1)))
			return true;

		if (trimmedUpper.charAt(0) == 'P' && poBox.matcher(trimmedUpper).find())
			return true;

		if (wordCount < 3)
			return false;

		int addressMarkerIndex = -1;
		int score = 0;

		if (!AddressCommon.isModifier(firstWord, false) && !addressMarkers.contains(firstWord) && !initialNumeric)
			return false;

		// Only get credit if the number starts with something non-zero in detect mode (despite the fact that they do occur)
		if (initialNumeric && (!detectMode || words.get(0).charAt(0) != '0'))
			score++;

		for (int i = 0; i < wordCount; i++) {
			String word = words.get(i);
			if (addressMarkers.contains(word)) {
				if (addressMarkerIndex == -1)
					score++;
				addressMarkerIndex = i;
			}
			else if (AddressCommon.isDirection(word) || AddressCommon.isModifier(word, i == wordCount - 1))
				score++;
		}

		if (score >= 2)
			return true;

		// If it looks like '1280 SE XXXXX' declare it good
		if (initialNumeric && AddressCommon.isDirection(words.get(1)))
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

		// If we have all the headers then we can check if this one is less likely to be the primary address field than the previous one
		int current = context.getStreamIndex();
		if (current >= 1 && getHeaderConfidence(context.getCompositeStreamNames()[current - 1]) > getHeaderConfidence(dataStreamName))
			return 0.0;

		final int headerConfidence = getHeaderConfidence(dataStreamName);
		double confidence = (double)matchCount/realSamples;

		final String[] semanticTypes = context.getSemanticTypes();
		final boolean semanticTypeInfoAvailable = semanticTypes != null && context.getStreamIndex() != -1 && context.getStreamIndex() != semanticTypes.length - 1;

		// Does the next field have a Semantic Type that indicates it is a Street Address 2
		if (semanticTypeInfoAvailable && confidence > 0.1 && "STREET_ADDRESS2_EN".equals(semanticTypes[context.getStreamIndex() + 1])) {
			// If this header is the same as the next but with a 2 switched for a '1' then we are extremely confident
			final String nextStreamName = context.getCompositeStreamNames()[current + 1];

			int index = nextStreamName.indexOf('2');
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
