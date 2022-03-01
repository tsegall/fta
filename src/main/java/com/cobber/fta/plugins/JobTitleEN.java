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
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.SingletonSet;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect Job Titles. (English-language only).
 */
public class JobTitleEN extends LogicalTypeInfinite {
	public static final String SEMANTIC_TYPE = "JOB_TITLE_EN";
	private static final String BACKOUT = ".+";
	private SingletonSet titleStartersRef;
	private Set<String> titleStarters;
	private SingletonSet titleHotWordsRef;
	private Set<String> titleHotWords;

	public JobTitleEN(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		final String[] examples = {
				"CTO",  "CEO", "PROJECT MANAGER", "PROGRAM MANAGER", "VP", "VICE PRESIDENT", "COO", "CMO",
				"DIRECTOR", "MARKETING MANAGER", "EVP", "SVP", "SALES ENGINEER", "SENIOR ENGINEER", "CISO",
				"CHIEF TECHNOLOGY OFFICER", "CHIEF MARKETING OFFICER",
				"CHIEF INFORMATION SECURITY OFFICER", "CHIEF EXECUTIVE OFFICER",
		};

		return examples[random.nextInt(examples.length)];
	}

	@Override
	public boolean initialize(final Locale locale) throws FTAPluginException {
		super.initialize(locale);

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

		if (words.length == 0)
			return false;

		final String firstWord = words[0];
		final int firstWordLength = firstWord.length();
		if (firstWordLength < 2)
			return false;

		if (titleStarters.contains(firstWord))
			return true;

		for (final String word : words)
			if (titleHotWords.contains(word))
				return true;

		return false;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return isValid(trimmed);
	}

	@Override
	public String isValidSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		int minCardinality = 10;
		int minSamples = 20;
		if (getHeaderConfidence(context.getStreamName()) != 0) {
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