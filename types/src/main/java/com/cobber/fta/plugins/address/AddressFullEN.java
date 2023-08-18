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

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Content;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.LogicalTypeFiniteSimple;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.SingletonSet;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.Utils;
import com.cobber.fta.core.WordOffset;
import com.cobber.fta.core.WordProcessor;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect an Address line. (English-language only).
 */
public class AddressFullEN extends LogicalTypeInfinite {
	private boolean multiline;
	private Set<String> addressMarkers;
	private Set<String> statesWithSpaces;

	private LogicalTypeInfinite logicalPostCode;
	private LogicalTypeInfinite logicalZipPlus;
	private LogicalTypeFiniteSimple logicalState;
	private LogicalTypeFiniteSimple logicalCountry;
	private Pattern poBox;
	private String country;
	private final WordProcessor wordProcessor = new WordProcessor().withAdditionalBreakChars("-#").withAdditionalKillChars("'");

	/**
	 * Construct a plugin to detect an Address based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public AddressFullEN(final PluginDefinition plugin) {
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

		final StringBuilder b = new StringBuilder();
		return b.append(1 + getRandom().nextInt(999))
				.append(' ')
				.append(streets[getRandom().nextInt(streets.length)])
				.append(' ')
				.append(simpleAddressMarkers[getRandom().nextInt(simpleAddressMarkers.length)])
				.append(", ")
				.append(logicalState.nextRandom())
				.append(' ')
				.append(logicalPostCode.nextRandom()).toString();
	}

	private int getPostCodeIndex(final List<WordOffset> words) {
		int ret = -1;

		for (int i = 1; i < words.size(); i++) {
			final String word = words.get(i).word;
			if ("US".equals(country)) {
				if (word.length() >= 5) {
					// Always prefer a 5 digit zip if we see one
					if (logicalPostCode.isValid(word, true, -1))
						ret = i;
					else if (logicalZipPlus.isValid(word, true, -1) && ret == -1)
						ret = i;
				}
			}
			else if ("CA".equals(country)) {
				if (word.length() == 6 && logicalPostCode.isValid(word, true, -1) ||
						(word.length() == 3 && Utils.isSimpleAlpha(word.charAt(0)) && Utils.isSimpleNumeric(word.charAt(1)) && Utils.isSimpleAlpha(word.charAt(2)) &&
						i + 1 < words.size() && logicalPostCode.isValid(word + " " + words.get(i + 1).word, true, -1)))
					ret = i;
			}
			else if ("AU".equals(country)) {
				if (word.length() == 4 && logicalPostCode.isValid(word, true, -1))
					ret = i;
			}

		}
		return ret;
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		country = locale.getCountry().toUpperCase(Locale.ROOT);

		poBox = Pattern.compile(AddressCommon.POBOX);

		if ("US".equals(country)) {
			logicalPostCode = (LogicalTypeInfinite) LogicalTypeFactory.newInstance(PluginDefinition.findByName("POSTAL_CODE.ZIP5_US"), analysisConfig);
			logicalZipPlus = (LogicalTypeInfinite) LogicalTypeFactory.newInstance(PluginDefinition.findByName("POSTAL_CODE.ZIP5_PLUS4_US"), analysisConfig);
			logicalState = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByName("STATE_PROVINCE.STATE_NAME_US"), analysisConfig);
		}
		else if ("CA".equals(country)) {
			logicalPostCode = (LogicalTypeInfinite) LogicalTypeFactory.newInstance(PluginDefinition.findByName("POSTAL_CODE.POSTAL_CODE_CA"), analysisConfig);
			logicalState = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByName("STATE_PROVINCE.PROVINCE_NAME_CA"), analysisConfig);
		}
		else if ("AU".equals(country)) {
			logicalPostCode = (LogicalTypeInfinite) LogicalTypeFactory.newInstance(PluginDefinition.findByName("POSTAL_CODE.POSTAL_CODE_AU"), analysisConfig);
			logicalState = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByName("STATE_PROVINCE.STATE_NAME_AU"), analysisConfig);
		}

		logicalCountry = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByName("COUNTRY.TEXT_EN"), analysisConfig);

		addressMarkers = new SingletonSet(new Content("resource", "/reference/en_street_markers.csv")).getMembers();

		statesWithSpaces = new HashSet<>();
		for (final String state : logicalState.getMembers())
			if (state.indexOf(' ') != -1)
				statesWithSpaces.add(state);

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
		return validation(input.trim());
	}

	public boolean validation(final String input) {
		String upper = input;
		boolean geoCoded = false;

		// A number of addresses end with a geo-coded location of the form (LAT, LONG) - if so remove it.
		final int len = input.length();
		if (len > 10 && input.charAt(len - 1) == ')') {
			final int start = input.lastIndexOf('(');
			if (start != -1)
				upper = input.substring(0, start);
			geoCoded = true;
		}
		upper = upper.toUpperCase(Locale.ENGLISH);

		final List<WordOffset> words = wordProcessor.asWordOffsets(upper);
		final int wordCount = words.size();

		if (wordCount < 4 || wordCount > 12)
			return false;

		int score = 1;
		int stateIndex = -1;
		int countryIndex = -1;
		int addressMarkerIndex = -1;
		int hintIndex = -1;
		int directionIndex = -1;
		boolean poBoxFound = false;

		final int postCodeIndex = getPostCodeIndex(words);

		if (poBox.matcher(upper).find())
			poBoxFound = true;

		// Decide where to start - skip any words that are non-numeric, when we find the first numeric back up one (if possible)
		int start = -1;
		for (int i = 0; i < wordCount; i++) {
			final String word = words.get(i).word;
			if ((word.length() <= 5 && Utils.isNumeric(word)) || AddressCommon.isAddressNumber(word)) {
				start = i == 0 ? 0 : i - 1;
				break;
			}
		}
		if (start == -1)
			return false;

		for (int i = start; i < wordCount; i++) {
			final String word = words.get(i).word;
			if (i != start && addressMarkerIndex == -1 && addressMarkers.contains(word)) {
				score++;
				addressMarkerIndex = i;
				// Skip a Direction if it exists
				if (i + 1 < wordCount && AddressCommon.isDirection(words.get(i + 1).word))
					i++;
			}
			else if (directionIndex == -1 && AddressCommon.isDirection(word))
				directionIndex = i;
			else if (logicalState.isValid(word, true, -1))
				stateIndex = i;
			else if (logicalCountry.isValid(word, true, -1))
				countryIndex = i;
			else if (AddressCommon.isModifier(input, i == wordCount - 1))
				hintIndex = i;
			else if (!poBoxFound && "BOX".equals(word))
				poBoxFound = true;
		}

		if (!poBoxFound && !AddressCommon.isAddressNumber(words.get(0).word) &&
				(addressMarkerIndex == -1 || addressMarkerIndex < 2 || !AddressCommon.isAddressNumber(words.get(addressMarkerIndex - 2).word)))
			return false;

		// Unfortunately some States have spaces in them so if we have not found a state but and it is worth searching (look harder)
		if (stateIndex == -1 && score >= 2) {
			for (final String state : statesWithSpaces) {
				int offset = -1;
				if ((offset = upper.indexOf(state)) != -1) {
					for (int i = 0; i < wordCount; i++) {
						if (words.get(i).offset == offset) {
							stateIndex = i;
							break;
						}
					}
				}
				if (stateIndex != -1)
					break;
			}
		}

		if (postCodeIndex >= wordCount - 3)
			score++;
		if (poBoxFound)
			score++;
		if (hintIndex >= wordCount - 3)
			score++;
		if (stateIndex >= wordCount - 3)
			score++;
		if (countryIndex >= wordCount - 3)
			score++;
		if (directionIndex != -1 && directionIndex < wordCount - 2)
			score++;
		if (geoCoded)
			score++;

		// If no State and no PostCode then it is not a FULL address
		if (stateIndex == -1 && postCodeIndex == -1)
			return false;

		if (score >= 4)
			return true;

		if (score >= 2) {
			// Do we have a PO BOX and a State?
			if (poBoxFound && stateIndex != -1)
				return true;
			// Are the State and Zip are next to each other?
			if (postCodeIndex != -1 && Math.abs(stateIndex - postCodeIndex) == 1)
				return true;
			// Is the State after the addressMarker?
			if (addressMarkerIndex != -1 && stateIndex != -1 && (stateIndex > addressMarkerIndex || stateIndex == wordCount - 1))
				return true;
			// Is the ZIP after the addressMarker?
			if (addressMarkerIndex != -1 && postCodeIndex != -1 && postCodeIndex > addressMarkerIndex)
				return true;
		}

		return false;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		if (charCounts[' '] < 2)
			return false;
		return validation(trimmed);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		return getConfidence(matchCount, realSamples, context) >= getThreshold()/100.0 ? PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		final String dataStreamName = context.getStreamName();

		final int headerConfidence = getHeaderConfidence(dataStreamName);
		double confidence = (double)matchCount/realSamples;

		// Boost based on how much we like the header
		if (headerConfidence >= 99)
			confidence = Math.min(confidence + 0.20, 1.0);
		else if (headerConfidence >= 90)
			confidence = Math.min(confidence + 0.10, 1.0);

		return confidence;
	}
}
