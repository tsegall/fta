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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.token.TokenStreams;

/**
* Plugin to detect any Logical Type defined by the presence of a set of words.
*/
public abstract class SimpleWords extends LogicalTypeInfinite {
	private Set<String> keywordsHash;

	abstract String[] getWords();

	private String regExp = "[-\\p{IsAlphabetic} /]+";
	private final Set<String> rejected = new HashSet<>();

	/**
	 * Construct an Race plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public SimpleWords(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		keywordsHash = new HashSet<>();
		keywordsHash.addAll(Arrays.asList(getWords()));

		return true;
	}

	@Override
	public String nextRandom() {
		return getWords()[random.nextInt(getWords().length)];
	}

	@Override
	public String getQualifier() {
		return defn.qualifier;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public String getRegExp() {
		return regExp;
	}

	@Override
	public boolean isRegExpComplete() {
		return false;
	}

	private static List<String> splitIntoWords(final String input) {
		final ArrayList<String> ret = new ArrayList<>();

		int start = -1;
		for (int i = 0; i < input.length(); i++) {
			final char ch = input.charAt(i);
			if (Character.isLetter(ch)) {
				if (start == -1)
					start = i;
			}
			else {
				if (start != -1) {
					ret.add(input.substring(start, i));
					start = -1;
				}
			}
		}

		if (start != -1)
			ret.add(input.substring(start, input.length()));

		return ret;
	}

	@Override
	public boolean isValid(final String input) {
		if (keywordsHash.contains(input.toUpperCase(locale)))
			return true;

		final List<String> words = splitIntoWords(input);

		for (final String word : words)
			// Good if any of the words is in the list of happy words
			if (keywordsHash.contains(word.toUpperCase(locale)))
				return true;

		return false;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		if (!isValid(trimmed)) {
			rejected.add(trimmed);
			return false;
		}

		return true;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		final double confidence = (double)matchCount/realSamples;
		if (confidence >= getThreshold() / 100.0)
			return confidence;

		return confidence > .7 && rejected.size() <= 1 ? getThreshold() / 100.0 : 0;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		if (getConfidence(matchCount, realSamples, context) < getThreshold() / 100.0)
			return PluginAnalysis.SIMPLE_NOT_OK;

		// Generate the RE using all the elements in the set, we assume the outliers are also good just not detected
		// as so by the raceWords list above.
		final RegExpGenerator re = new RegExpGenerator(cardinality.size() + outliers.size(), locale);
		cardinality.putAll(outliers);
		outliers.clear();
		for (final String item : cardinality.keySet())
			re.train(item);
		regExp = re.getResult();

		return PluginAnalysis.OK;
	}
}
