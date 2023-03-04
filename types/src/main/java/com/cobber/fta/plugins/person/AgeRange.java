/* Copyright 2017-2022 Tim Segall
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
package com.cobber.fta.plugins.person;

import java.util.List;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.Keywords;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.Utils;
import com.cobber.fta.core.WordProcessor;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect Age Range (Person).
 * 
 * Handle:
 *  Aged 18-44 years
 * 	10 - 20 years
 * 	10 to 20 years
 *  10-20
 *  10-20 years
 *  85 years and over
 *  85+
 *  &lt;1
 */
public class AgeRange extends LogicalTypeInfinite {
	/** The Regular Expression for this Semantic type. */
	public static final String LONG_REGEXP = "\\d{1,3}";
	public static final String DOUBLE_REGEXP = "\\d{1,3}\\.\\d+";

	private final int MAX_AGE = 120;
	private final Keywords keywords = new Keywords();

	private WordProcessor wordProcessor = new WordProcessor().withBreakChars(" \u00A0-");
	
	/**
	 * Construct an Age Range plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public AgeRange(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		keywords.initialize(locale);

		return true;
	}

	@Override
	public String nextRandom() {
		final int low = random.nextInt(15);
		return String.valueOf(low * 5) + " - " + String.valueOf((low + 1) * 5);
	}

	@Override
	public String getRegExp() {
		return ".*";
	}

	@Override
	public boolean isRegExpComplete() {
		return false;
	}
	
	private boolean isYear(final String yearWord) {
		return keywords.match(yearWord, "YEARS", Keywords.MatchStyle.EQUALS) >= 90;
	}

	private boolean validate(final String trimmed) {
		final List<String> words = wordProcessor.asWords(Utils.cleanse(trimmed));
		final int wordCount = words.size();

		if (wordCount == 0)
			return false;
		String lowAge = words.get(0);
		int initialOffset = 1;
		if (!Utils.isNumeric(lowAge)) {
			// Maybe we can just skip the first word
			if (wordCount <= 2)
				return false;
			lowAge = words.get(1);
			if (!Utils.isNumeric(lowAge))
				return false;
			initialOffset++;
		}

		if (lowAge.length() > 3 || Integer.parseInt(lowAge) > MAX_AGE)
			return false;

		int rangeEnd = -1;
		int yearIndex = -1;
		for (int i = initialOffset; i < wordCount; i++) {
			final String word = words.get(i); 
			if (Utils.isNumeric(word)) {
				if (rangeEnd != -1)
					return false;
				if (word.length() > 3 || Integer.parseInt(word) > MAX_AGE)
					return false;
				rangeEnd = i;
			}
			else if (isYear(word)) {
				yearIndex = i;
			}
		}
		
		if (rangeEnd != -1) {
			if (wordCount == rangeEnd + 1)
				return true;
			if (rangeEnd + 2 == wordCount && yearIndex != -1)
				return true;
		}
		
		return yearIndex != -1 || trimmed.charAt(0) == '<' || trimmed.charAt(trimmed.length() - 1) == '+';
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		return validate(input.trim());
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return validate(trimmed);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		return getConfidence(matchCount, realSamples, context) >= getThreshold() / 100.0 ?  PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}
}
