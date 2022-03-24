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

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.SingletonSet;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
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
	public boolean initialize(final Locale locale) throws FTAPluginException {
		super.initialize(locale);

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
	public boolean isValid(final String input) {
		final int length = input.length();

		// Attempt to fail fast
		if (length > 60)
			return false;

		final String inputUpper = input.trim().toUpperCase(Locale.ENGLISH);

		// Simple case first - last 'word is something we recognize
		final int spaceIndex = inputUpper.trim().lastIndexOf(' ');
		if (spaceIndex != -1 && addressMarkers.contains(inputUpper.substring(spaceIndex + 1)))
			return true;

		if (inputUpper.startsWith("PO BOX"))
			return true;

		// Accept something of the form, initial digit followed by an address marker word (e.g. Road, Street, etc).
		if (!Character.isDigit(inputUpper.charAt(0)))
			return false;

		final String[] words = inputUpper.replace(",", "").split(" ");
		if (words.length < 3)
			return false;

		for (int i = 1; i < words.length  - 1; i++) {
			if (addressMarkers.contains(words[i]))
				return true;
		}

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

		if (inputUpper.startsWith("PO BOX"))
			return true;

		if (!Character.isDigit(inputUpper.charAt(0)) || charCounts[' '] < 2)
			return false;

		final String[] words = inputUpper.replace(",", "").split(" ");
		for (int i = 1; i < words.length  - 1; i++) {
			if (addressMarkers.contains(words[i]))
				return true;
		}

		return false;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		return getConfidence(matchCount, realSamples, context.getStreamName()) >= getThreshold()/100.0 ? PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final String dataStreamName) {
		double confidence = (double)matchCount/realSamples;
		// Boost by up to 5% if we like the header
		if (getHeaderConfidence(dataStreamName) != 0)
			confidence = Math.min(confidence + Math.min((1.0 - confidence)/2, 0.05), 1.0);

		return confidence;
	}
}
