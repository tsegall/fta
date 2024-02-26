/*
 * Copyright 2017-2024 Tim Segall
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 *  under 10
 *  over 65
 *  CDC PUF (Public Use Files) AGES - AGEALL, AGE017, AGE1839, AGE4064, AGE6584, AGE85PLUS
 *
 */
public class AgeRange extends LogicalTypeInfinite {
	private static final int MAX_AGE = 120;
	private final String symbols = "<>+≤≥";
	private final static Set<String> agesPUF = new HashSet<>();
	private Keywords keywords;

	static {
		agesPUF.add("AGE011");
		agesPUF.add("AGE017");
		agesPUF.add("AGE1217");
		agesPUF.add("AGE1824");
		agesPUF.add("AGE1834");
		agesPUF.add("AGE1839");
		agesPUF.add("AGE18PLUS");
		agesPUF.add("AGE2529");
		agesPUF.add("AGE3034");
		agesPUF.add("AGE3539");
		agesPUF.add("AGE4044");
		agesPUF.add("AGE4064");
		agesPUF.add("AGE4549");
		agesPUF.add("AGE5054");
		agesPUF.add("AGE5559");
		agesPUF.add("AGE6064");
		agesPUF.add("AGE6569");
		agesPUF.add("AGE6579");
		agesPUF.add("AGE6584");
		agesPUF.add("AGE65PLUS");
		agesPUF.add("AGE7074");
		agesPUF.add("AGE7579");
		agesPUF.add("AGE8084");
		agesPUF.add("AGE80PLUS");
		agesPUF.add("AGE85PLUS");
		agesPUF.add("AGEALL");
	}

	private final WordProcessor wordProcessor = new WordProcessor().withBreakChars(" \u00A0-");

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

		keywords = Keywords.getInstance(analysisConfig.getLocale());

		return true;
	}

	@Override
	public String nextRandom() {
		final int low = getRandom().nextInt(15);
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

	private boolean isYear(final String word) {
		return keywords.match(word, "YEARS") >= 90;
	}

	private boolean isDateQualifier(final String word) {
		return keywords.match(word, "MONTHS") >= 90 ||
				keywords.match(word, "WEEKS") >= 90;
	}

	private boolean isAgeModifier(final String modifier) {
		final int len = modifier.length();
		if (len == 1 && symbols.indexOf(modifier.charAt(0)) != -1)
			return true;

		if (keywords.match(modifier, "UNDER") >= 90 ||
				keywords.match(modifier, "OVER") >= 90)
			return true;

		return false;
	}

	/*
	 * Approximate BNF.
	 *
	 *  [<IgnoreWord>] [<AgeModifier>] <Age> [<YearsMarker>]
	 *  [<IgnoreWord>] <Age> [<AgeModifier>] [<YearsMarker>]
	 *  [<IgnoreWord>] <Age> <AnyWord> <Age> [<YearsMarker>]
	 *  <CDCWord>
	 *
	 *  <AgeModifier> ::= '<' | '>' | '+' | '≤' | '≥' | localized('over') | localized('under')
	 *  <IgnoreWord> ::=  AnyWord
	 *  <CDCWord> ::= "AGEALL" | "AGE017" | "AGE1839" | "AGE4064" | "AGE6584" | "AGE85PLUS"
	 */
	private boolean validate(final String trimmed) {
		if (trimmed.length() > 40)
			return false;

		// Handle CDCWord
		if (agesPUF.contains(trimmed))
			return true;

		final List<String> words = wordProcessor.asWords(Utils.cleanse(trimmed));
		final int wordCount = words.size();

		if (wordCount == 0)
			return false;

		int rangeStartIndex = -1;
		int rangeEndIndex = -1;
		int rangeStart = -1;
		int rangeEnd = -1;
		int ageModifier = -1;
		int yearIndex = -1;
		int dateQualifierIndex = -1;

		for (int i = 0; i < wordCount; i++) {
			final String word = words.get(i);
			if (Utils.isNumeric(word)) {
				// If it is numeric but not a plausible AGE call it a day
				if (word.length() >= 4)
					return false;
				final int value = Integer.parseInt(word);
				if (value > MAX_AGE)
					return false;
				if (rangeStartIndex == -1) {
					rangeStartIndex = i;
					rangeStart = value;
					continue;
				}
				if (rangeEndIndex == -1) {
					rangeEndIndex = i;
					rangeEnd = value;
					continue;
				}
				return false;
			}
			else if (isAgeModifier(word))
				ageModifier = i;
			else if (isYear(word))
				yearIndex = i;
			else if (isDateQualifier(word))
				dateQualifierIndex = i;
		}

		// No <Age>'s found so we are done
		if (rangeStartIndex == -1 && rangeEndIndex == -1)
			return false;

		// We expect some text/symbol to qualify the single <Age>
		if (rangeEndIndex == -1) {
			if (ageModifier != -1)
				return true;

			return yearIndex != -1 && yearIndex > rangeStartIndex;
		}
		else if (dateQualifierIndex == -1 && rangeStart >= rangeEnd)
			return false;

		return true;
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
