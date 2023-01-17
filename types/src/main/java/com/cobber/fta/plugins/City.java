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

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.SingletonSet;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.Utils;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect City.
 */
public class City extends LogicalTypeInfinite {
	private boolean randomInitialized;
	private SingletonSet samples;
	private int maxLength = 0;

	/**
	 * Construct a plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public City(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		return true;
	}

	/*
	 * There are two options for generating random samples for 'regex' plugins.  Either you provide a list of samples via the content and then
	 * these are selected at from random or you use Xeger to generate samples based on the regular expression returned.
	 * We use samples for things like 'CITY' where we prefer to see 'Paris', 'London' etc as opposed to 'aqrzx' which is what would be generated
	 * if we simply use the regular expression.
	 */
	@Override
	public String nextRandom() {
		if (!randomInitialized) {
			// Check to see if we have been provided with a set of samples
			if (defn.content != null && samples == null)
				samples = new SingletonSet(defn.contentType, defn.content);
			randomInitialized = true;
		}

		return samples != null ? samples.getRandom(random) : null;
	}


	@Override
	public String getRegExp() {
		return "\\p{IsAlphabetic}[-' \\.\\p{IsAlphabetic}\\d]*";
	}

	@Override
	public boolean isRegExpComplete() {
		return true;
	}

	// <city>, state
	// <city>
	// <city_1] <city_2>
	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		final String trimmedUpper = Utils.cleanse(input.toUpperCase(locale).trim());
		final int len = trimmedUpper.length();
		if (len > maxLength)
			maxLength = len;

		int spaces = 0;
		int commas = 0;
		int dashes = 0;
		int quotes = 0;
		int periods = 0;
		int chars = 0;
		for (int i = 0; i < len; i++) {
			char ch = trimmedUpper.charAt(i);
			if (Character.isAlphabetic(ch))
				chars++;
			else if (ch == ',') {
				if (commas != 0)
					return false;
				else
					commas++;
			}
			else if (ch == ' ') {
				if (spaces > 2)
					return false;
				else
					spaces++;
			}
			else if (ch == '-') {
				if (dashes != 0)
					return false;
				else
					dashes++;
			}
			else if (ch == '\'') {
				if (quotes != 0)
					return false;
				else
					quotes++;
			}
			else if (ch == '.') {
				if (periods != 0)
					return false;
				else
					periods++;
			}
			else
				return false;
		}

		return chars > 1;
	}

	@Override
	public boolean isCandidate(String trimmed, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		return isValid(trimmed, true, 0);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		final int headerConfidence = getHeaderConfidence(context.getStreamName());

		if (headerConfidence <= 0 || maxLength <= 3)
			return PluginAnalysis.SIMPLE_NOT_OK;

		double confidence = (double)matchCount/realSamples;
		if (headerConfidence >= 99)
			return PluginAnalysis.OK;

		// Boost based on how much we like the header
		if (headerConfidence >= 95)
			confidence = Math.min(confidence + 0.20, 1.0);
		else if (headerConfidence >= 75)
			confidence = Math.min(confidence + 0.10, 1.0);

		return confidence >= getThreshold() / 100.0 ?  PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}
}
