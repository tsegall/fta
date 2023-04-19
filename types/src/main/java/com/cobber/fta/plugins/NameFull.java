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
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.LogicalTypeFiniteSimple;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.WordProcessor;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect an unstructured full name.
 */
public class NameFull extends LogicalTypeInfinite {
	/** The Regular Expression for this Semantic type. */
	private static final String REGEXP = "\\p{IsAlphabetic}[- \\p{IsAlphabetic}]*, ?[- \\p{IsAlphabetic}]+";
	private static final String BACKOUT = ".+";
	private LogicalTypeFiniteSimple logicalFirst;
	private LogicalTypeFiniteSimple logicalLast;
	private LogicalTypeFiniteSimple logicalSuffix;
	private LogicalTypeFiniteSimple logicalHonorific;
	private static final int MAX_FIRST_NAMES = 100;
	private static final int MAX_LAST_NAMES = 100;
	private Set<String> lastNames;
	private Set<String> firstNames;
	private WordProcessor wordProcessor = new WordProcessor().withAdditionalBreakChars("-");

	/**
	 * Construct a plugin to detect Last name followed by First name based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public NameFull(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		logicalFirst = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByQualifier("NAME.FIRST"), analysisConfig);
		logicalLast = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByQualifier("NAME.LAST"), analysisConfig);
		logicalSuffix = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByQualifier("NAME.SUFFIX"), analysisConfig);
		logicalHonorific = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByQualifier("HONORIFIC_EN"), analysisConfig);

		firstNames = new HashSet<>();
		lastNames = new HashSet<>();

		return true;
	}

	@Override
	public String nextRandom() {
		return logicalLast.nextRandom() + ", " + logicalFirst.nextRandom();
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public boolean isRegExpComplete() {
		return true;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		final String inputUpper = input.trim().toUpperCase(Locale.ENGLISH);

		return validation(inputUpper, detectMode, count);
	}

	private boolean validation(final String trimmedUpper, final boolean detectMode, final long count) {
		final List<String> words = wordProcessor.asWords(trimmedUpper);
		final int wordCount = words.size();

		if (words.size() < 2)
			return false;

		boolean initialSeen = false;
		boolean honorificSeen = false;
		boolean suffixSeen = false;
		int nameCount = 0;

		for (int i = 0; i < wordCount; i++) {
			final String word = words.get(i);
			if (!initialSeen && word.length() == 1 && Character.isAlphabetic(word.charAt(0))) {
				initialSeen = true;
				continue;
			}
			if (!suffixSeen && logicalSuffix.isValid(word, detectMode, -1)) {
				suffixSeen = true;
				continue;
			}
			if (!honorificSeen && logicalHonorific.isValid(word, detectMode, -1)) {
				honorificSeen = true;
				continue;
			}
			if (logicalFirst.isValid(word, detectMode, -1) || logicalLast.isValid(word, detectMode, -1))
				nameCount++;
		}

		if ((honorificSeen || suffixSeen) && nameCount != 0)
			return true;

		if (nameCount >= 2)
			return true;

		return false;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return validation(trimmed.toUpperCase(Locale.ENGLISH), true, 0);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		int minCardinality = 8;
		int minSamples = 10;
		if (getHeaderConfidence(context.getStreamName()) != 0) {
			minCardinality = 3;
			minSamples = 3;
		}

		if (cardinality.size() < minCardinality)
			return new PluginAnalysis(BACKOUT);

		if (realSamples < minSamples)
			return new PluginAnalysis(BACKOUT);

		// Reject if there is not a reasonable spread of last or first names
		if (getHeaderConfidence(context.getStreamName()) <= 0 &&
				((lastNames.size() < MAX_LAST_NAMES && (double)lastNames.size()/matchCount < .2) ||
				(firstNames.size() < MAX_FIRST_NAMES && (double)firstNames.size()/matchCount < .2)))
			return new PluginAnalysis(BACKOUT);

		if (getConfidence(matchCount, realSamples, context) >= getThreshold()/100.0)
			return PluginAnalysis.OK;

		return new PluginAnalysis(BACKOUT);
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		final double is = (double)matchCount/realSamples;
		if (matchCount == realSamples || getHeaderConfidence(context.getStreamName()) <= 0)
			return is;

		return is + (1.0 - is)/2;
	}
}

