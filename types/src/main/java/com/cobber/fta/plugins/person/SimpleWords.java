/* Copyright 2017-2023 Tim Segall
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.core.WordProcessor;
import com.cobber.fta.token.TokenStreams;

/**
* Plugin to detect any Semantic Type defined by the presence of a set of words.
*/
public abstract class SimpleWords extends LogicalTypeInfinite {
	private Set<String> keywordsHash;
	private final WordProcessor wordProcessor = new WordProcessor().withAdditionalBreakChars("_=");
	private String regExp = "[-\\p{IsAlphabetic} /]+";
	private final Set<String> rejected = new HashSet<>();

	protected abstract String[] getWords();
	protected int getMaxWords() {
		return 3;
	}

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
		return getWords()[getRandom().nextInt(getWords().length)];
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

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		if (keywordsHash.contains(input.toUpperCase(locale)))
			return true;

		final List<String> words = wordProcessor.asWords(input);

		// We don't want to mistake whole paragraphs with key words - so bail if too many words
		if (words.size() > getMaxWords())
			return false;

		for (final String word : words)
			// Good if any of the words is in the list of happy words
			if (keywordsHash.contains(word.toUpperCase(locale)))
				return true;

		return false;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		if (!isValid(trimmed, true, -1)) {
			rejected.add(trimmed);
			return false;
		}

		return true;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		double confidence = (double)matchCount/realSamples;
		if (getHeaderConfidence(context.getStreamName()) >= 95)
			confidence = Math.min(confidence + 0.20, 1.0);

		if (confidence >= getThreshold() / 100.0)
			return confidence;

		return confidence > .7 && rejected.size() <= 1 ? getThreshold() / 100.0 : 0;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
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
