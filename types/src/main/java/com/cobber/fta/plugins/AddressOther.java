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

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect an Address line (non-English).
 */
public class AddressOther extends LogicalTypeInfinite {
	private boolean multiline;
	private String language;

	/**
	 * Construct a plugin to detect an Address based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public AddressOther(final PluginDefinition plugin) {
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

		language = locale.getLanguage().toUpperCase(Locale.ROOT);

		return true;
	}

	@Override
	public String getSemanticType() {
		return defn.semanticType.replace("<LANGUAGE>", language);
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
		// Calculate the length in Code Points so we do not penalize non-ASCII characters
		final int length = input.codePointCount(0, input.length());

		// Attempt to fail fast
		if (length > 100 || length < 5)
			return false;

		return input.indexOf(' ') != -1;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		// Track whether this is a multi-line field or not
		if (!multiline)
			multiline = trimmed.indexOf('\n') != -1 || trimmed.indexOf('\r') != -1;

		return charCounts[' '] != 0;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		return getConfidence(matchCount, realSamples, context) >= getThreshold()/100.0 ? PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		final int headerConfidence = getHeaderConfidence(context.getStreamName());
		double confidence = (double)matchCount/realSamples;

		// We really want to see a great header
		if (headerConfidence < 99)
			return 0;

		return	Math.min(confidence + Math.min((1.0 - confidence)/2, 0.30), 1.0);
	}
}
