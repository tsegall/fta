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

import java.util.HashSet;
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
 * Plugin to detect an Address line. (English-language only).
 */
public class AddressEN extends LogicalTypeInfinite {
	/** The Semantic type for this Plugin. */
	public static final String SEMANTIC_TYPE = "STREET_ADDRESS_EN";

	private boolean multiline;
	private SingletonSet addressMarkersRef;
	private Set<String> addressMarkers;

	private static HashSet<String> directions = new HashSet<>();

	static {
		directions.add("N");
		directions.add("NE");
		directions.add("NW");
		directions.add("S");
		directions.add("SE");
		directions.add("SW");
		directions.add("E");
		directions.add("W");
	}

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

		return true;
	}

	@Override
	public String getQualifier() {
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
	public boolean isValid(final String input, boolean detectMode) {
		final int length = input.length();

		// Attempt to fail fast
		if (length > 60 || length < 5)
			return false;

		final String inputUpper = input.trim().toUpperCase(Locale.ENGLISH);

		// Simple case first - last 'word is something we recognize
		final int spaceIndex = inputUpper.trim().lastIndexOf(' ');
		if (spaceIndex != -1 && addressMarkers.contains(inputUpper.substring(spaceIndex + 1)))
			return true;

		if (inputUpper.contains("BOX") || inputUpper.contains("SUITE") || inputUpper.contains("FLOOR"))
			return true;

		// Accept something of the form, initial digit followed by an address marker word (e.g. Road, Street, etc).
		if (inputUpper.length() < 5 || !Character.isDigit(inputUpper.charAt(0)))
			return false;

		return validation(inputUpper);
	}

	private boolean validation(final String input) {
		final String[] words = input.replaceAll("[,\\.]", " ").split(" ");
		if (words.length < 3)
			return false;

		for (int i = 1; i <= words.length - 1; i++) {
			if (addressMarkers.contains(words[i]))
				return true;
		}

		// If it looks like '1280 SE XXXXX' declare it good
		if (Utils.isNumeric(words[0]) && directions.contains(words[1]))
				return true;

		return false;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		final String inputUpper = trimmed.toUpperCase(Locale.ENGLISH);
		final int spaceIndex = lastIndex[' '];

		// Track whether this is a multi-line field or not
		if (!multiline)
			multiline = trimmed.indexOf('\n') != -1 || trimmed.indexOf('\r') != -1;

		if (spaceIndex != -1 && addressMarkers.contains(inputUpper.substring(spaceIndex + 1, inputUpper.length())))
			return true;

		if (inputUpper.contains("BOX") || inputUpper.contains("SUITE"))
			return true;

		if (!Character.isDigit(inputUpper.charAt(0)) || charCounts[' '] < 2)
			return false;

		return validation(inputUpper);
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

		// Boost based on how much we like the header
		if (headerConfidence >= 99)
			confidence = Math.min(confidence + 0.20, 1.0);
		else if (headerConfidence >= 90)
			confidence = Math.min(confidence + Math.min((1.0 - confidence)/2, 0.15), 1.0);

		return confidence;
	}
}
