/*
 * Copyright 2017-2021 Tim Segall
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
import com.cobber.fta.FactsTypeBased;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.Shapes;
import com.cobber.fta.SingletonSet;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;

/**
 * Plugin to detect Job Titles. (English-language only).
 */
public class LogicalTypeJobTitleEN extends LogicalTypeInfinite {
	public static final String SEMANTIC_TYPE = "JOB_TITLE_EN";
	private static final String BACKOUT = ".+";
	private SingletonSet titleStartersRef;
	private Set<String> titleStarters;
	private SingletonSet titleHotWordsRef;
	private Set<String> titleHotWords;

	public LogicalTypeJobTitleEN(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		final String[] examples = new String[] {
				"RED",  "GREEN", "BLUE", "PINK", "BLACK", "WHITE", "ORANGE", "PURPLE",
				"GREY", "GREEN", "YELLOW", "MAUVE", "CREAM", "BROWN", "SILVER", "GOLD",
				"PEACH", "OLIVE", "LEMON", "LILAC", "BEIGE", "AMBER", "BURGUNDY"
		};

		return examples[random.nextInt(examples.length)];
	}

	@Override
	public boolean initialize(final Locale locale) throws FTAPluginException {
		super.initialize(locale);

		threshold = 75;

		titleStartersRef = new SingletonSet("resource", "/reference/en_title_starters.csv");
		titleStarters = titleStartersRef.getMembers();
		titleHotWordsRef = new SingletonSet("resource", "/reference/en_title_hotwords.csv");
		titleHotWords = titleHotWordsRef.getMembers();

		return true;
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public String getRegExp() {
		return ".+";
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public boolean isValid(final String input) {
		final int length = input.length();

		// Attempt to fail fast
		if (length > 80 || length < 2)
			return false;

		final String inputUpper = input.trim().toUpperCase(Locale.ENGLISH);
		final String[] words = inputUpper.split("[-/; ,]");

		String firstWord = words[0];
		int firstWordLength = firstWord.length();
		if (firstWordLength < 2)
			return false;

		if (titleStarters.contains(firstWord))
			return true;

		for (String word : words)
			if (titleHotWords.contains(word))
				return true;

		return false;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return isValid(trimmed);
	}

	@Override
	public String isValidSet(final String dataStreamName, final long matchCount, final long realSamples, String currentRegExp, final FactsTypeBased facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final Shapes shapes, AnalysisConfig analysisConfig) {
		int minCardinality = 10;
		int minSamples = 20;
		if (getHeaderConfidence(dataStreamName) != 0) {
			minCardinality = 5;
			minSamples = 5;
		}

		if (cardinality.size() < minCardinality)
			return BACKOUT;

		if (realSamples < minSamples)
			return BACKOUT;

		return (double)matchCount/realSamples >= getThreshold()/100.0 ? null : ".+";
	}
}