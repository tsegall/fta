/*
 * Copyright 2017-2026 Tim Segall
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import com.cobber.fta.core.Utils;
import com.cobber.fta.token.TokenStreams;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

/**
 * Plugin to detect '&lt;Last Name&gt;, &lt;First Name&gt;' (Western) or '&lt;姓&gt;[ ]&lt;名&gt;' (Japanese).
 */
public class NameLastFirst extends LogicalTypeInfinite {
	/** The Regular Expression for this Semantic type. */
	private static final String REGEXP = "\\p{IsAlphabetic}[- \\p{IsAlphabetic}]*, ?[- \\p{IsAlphabetic}]+";
	/** Regex covering Hiragana, Katakana, and CJK Unified/Extension-A Ideographs (plus repetition mark 々) with optional space separator. */
	private static final String REGEXP_JA = "[぀-ヿ一-鿿㐀-䶿々]+ ?[぀-ヿ一-鿿㐀-䶿々]+";
	private static final String BACKOUT = ".+";
	private LogicalTypeFiniteSimple logicalFirst;
	private LogicalTypeFiniteSimple logicalLast;
	private static final int MAX_FIRST_NAMES = 100;
	private static final int MAX_LAST_NAMES = 100;
	private Set<String> lastNames;
	private Set<String> firstNames;

	// Japanese support via bloom filters - only populated when locale language is "ja"
	private BloomFilter<CharSequence> jaLastFilter;
	private BloomFilter<CharSequence> jaFirstFilter;
	private static volatile List<String> jaExamples;

	/**
	 * Construct a plugin to detect Last name followed by First name based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public NameLastFirst(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		logicalFirst = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByName("NAME.FIRST"), analysisConfig);
		logicalLast = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByName("NAME.LAST"), analysisConfig);

		firstNames = new HashSet<>();
		lastNames = new HashSet<>();

		if ("ja".equals(locale.getLanguage())) {
			try (InputStream lastStream = NameLastFirst.class.getResourceAsStream("/reference/ja_lastnames.bf");
				 InputStream firstStream = NameLastFirst.class.getResourceAsStream("/reference/ja_firstnames.bf")) {
				if (lastStream == null)
					throw new FTAPluginException("Failed to locate Japanese last name bloom filter");
				if (firstStream == null)
					throw new FTAPluginException("Failed to locate Japanese first name bloom filter");
				jaLastFilter = BloomFilter.readFrom(lastStream, Funnels.stringFunnel(StandardCharsets.UTF_8));
				jaFirstFilter = BloomFilter.readFrom(firstStream, Funnels.stringFunnel(StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new FTAPluginException("Failed to load Japanese name bloom filters", e);
			}
		}

		return true;
	}

	private boolean isJapanese() {
		return jaLastFilter != null;
	}

	private static boolean isJapaneseChars(final String s) {
		if (s.isEmpty())
			return false;
		for (int i = 0; i < s.length(); i++)
			if (!Japanese.isJapaneseChar(s.charAt(i)))
				return false;
		return true;
	}

	@Override
	public String nextRandom() {
		if (!isJapanese())
			return logicalLast.nextRandom() + ", " + logicalFirst.nextRandom();

		if (jaExamples == null) {
			synchronized (NameLastFirst.class) {
				if (jaExamples == null) {
					final List<String> lastList = new ArrayList<>();
					final List<String> firstList = new ArrayList<>();
					try (InputStream stream = NameLastFirst.class.getResourceAsStream("/reference/ja_lastnames_s.csv");
						 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
						String line;
						while ((line = reader.readLine()) != null)
							lastList.add(line);
					} catch (IOException e) {
						throw new IllegalArgumentException("Failed to load Japanese last name samples", e);
					}
					try (InputStream stream = NameLastFirst.class.getResourceAsStream("/reference/ja_firstnames_s.csv");
						 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
						String line;
						while ((line = reader.readLine()) != null)
							firstList.add(line);
					} catch (IOException e) {
						throw new IllegalArgumentException("Failed to load Japanese first name samples", e);
					}
					final List<String> combined = new ArrayList<>();
					final int size = Math.min(lastList.size(), firstList.size());
					for (int i = 0; i < size; i++)
						combined.add(lastList.get(i) + " " + firstList.get(i));
					jaExamples = combined;
				}
			}
		}
		return jaExamples.get(getRandom().nextInt(jaExamples.size()));
	}

	@Override
	public String getRegExp() {
		return isJapanese() ? REGEXP_JA : REGEXP;
	}

	@Override
	public boolean isRegExpComplete() {
		return true;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		if (isJapanese())
			return isValidJapanese(input.trim(), detectMode);

		final String trimmed = Utils.cleanse(input.trim());
		final int comma = trimmed.indexOf(',');
		if (comma == -1 || comma == 0 || comma == trimmed.length() - 1)
			return false;

		boolean processingLast = true;
		final int len = trimmed.length();
		int spaces = 0;
		int dashes = 0;
		int periods = 0;
		int apostrophe = 0;
		int alphas = 0;
		int end = len;
		for (int i = 0; i < len; i++) {
			if (i == comma) {
				processingLast = false;
				alphas = 0;
				spaces = 0;
				dashes = 0;
				continue;
			}
			final char ch = trimmed.charAt(i);
			if (Character.isAlphabetic(ch)) {
				alphas++;
				continue;
			}
			if (ch == '\'') {
				apostrophe++;
				if (processingLast && apostrophe == 1)
					continue;
				return false;
			}
			if (ch == ' ') {
				alphas = 0;
				if (i != comma + 1 && !(i == comma + 2 && trimmed.charAt(i - 1) == ' '))
					spaces++;
				if (spaces == 2)
					return false;
				continue;
			}
			if (ch == '-') {
				dashes++;
				if (dashes == 2)
					return false;
				continue;
			}
			if (ch == '.') {
				periods++;
				if (periods == alphas)
					continue;
			}

			// If the last character is a comma or period then just ignore it
			if (i + 1 == len) {
				if (ch == ',' || ch == '.') {
					end = len - 1;
					break;
				}
			}

			return false;
		}

		String firstName = trimmed.substring(comma + 1, end).trim();
		final int middleName = firstName.indexOf(' ');
		if (middleName != -1)
			firstName = firstName.substring(0, middleName);
		final String lastName = trimmed.substring(0, comma).trim();

		if (firstNames.size() < MAX_FIRST_NAMES)
			firstNames.add(firstName);
		if (lastNames.size() < MAX_LAST_NAMES)
			lastNames.add(lastName);

		// So if we only have a few names insist it is found, otherwise use the isValid() test
		if (firstNames.size() < 10 ? logicalFirst.isMember(firstName) : logicalFirst.isValid(firstName, detectMode, -1))
			return true;

		return lastNames.size() < 10 ? logicalLast.isMember(lastName) : logicalLast.isValid(lastName, detectMode, -1);
	}

	private boolean isValidJapanese(final String trimmed, final boolean detectMode) {
		final int space = trimmed.indexOf(' ');
		if (space > 0 && space < trimmed.length() - 1) {
			// Space-separated form: 田中 太郎
			final String last = trimmed.substring(0, space);
			final String first = trimmed.substring(space + 1);
			if (!isJapaneseChars(last) || !isJapaneseChars(first))
				return false;
			if (lastNames.size() < MAX_LAST_NAMES)
				lastNames.add(last);
			if (firstNames.size() < MAX_FIRST_NAMES)
				firstNames.add(first);
			return jaLastFilter.mightContain(last) && jaFirstFilter.mightContain(first);
		}

		// Concatenated form: 田中太郎 — try splits at positions 1-3 (typical surname length)
		if (!isJapaneseChars(trimmed))
			return false;
		final int len = trimmed.length();
		for (int split = 1; split <= Math.min(3, len - 1); split++) {
			final String last = trimmed.substring(0, split);
			final String first = trimmed.substring(split);
			if (jaLastFilter.mightContain(last) && jaFirstFilter.mightContain(first)) {
				if (lastNames.size() < MAX_LAST_NAMES)
					lastNames.add(last);
				if (firstNames.size() < MAX_FIRST_NAMES)
					firstNames.add(first);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		if (isJapanese())
			return trimmed.length() >= 2 && trimmed.length() <= 8 && Japanese.isJapaneseChar(trimmed.charAt(0));
		return trimmed.length() >= 5 && trimmed.length() <= 30 && charCounts[','] == 1;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		if (isJapanese()) {
			final int headerConfidence = getHeaderConfidence(context);
			if (headerConfidence <= 0 && cardinality.size() < 5)
				return new PluginAnalysis(BACKOUT);
			if (getHeaderConfidence(context) <= 0 &&
					((lastNames.size() < MAX_LAST_NAMES && (double)lastNames.size()/matchCount < .2) ||
					 (firstNames.size() < MAX_FIRST_NAMES && (double)firstNames.size()/matchCount < .2)))
				return new PluginAnalysis(BACKOUT);
			if (getConfidence(matchCount, realSamples, context) >= getThreshold() / 100.0)
				return PluginAnalysis.OK;
			return new PluginAnalysis(BACKOUT);
		}

		int minCardinality = 8;
		int minSamples = 10;
		if (getHeaderConfidence(context) > 0) {
			minCardinality = 3;
			minSamples = 3;
		}

		if (cardinality.size() < minCardinality)
			return new PluginAnalysis(BACKOUT);

		if (realSamples < minSamples)
			return new PluginAnalysis(BACKOUT);

		// Reject if there is not a reasonable spread of last or first names
		if (getHeaderConfidence(context) <= 0 &&
				((lastNames.size() < MAX_LAST_NAMES && (double)lastNames.size()/matchCount < .2) ||
				(firstNames.size() < MAX_FIRST_NAMES && (double)firstNames.size()/matchCount < .2)))
			return new PluginAnalysis(BACKOUT);

		if (getConfidence(matchCount, realSamples, context) >= getThreshold()/100.0)
			return PluginAnalysis.OK;

		return new PluginAnalysis(BACKOUT);
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		if (isJapanese()) {
			double confidence = (double)matchCount/realSamples;
			if (getHeaderConfidence(context) > 0)
				confidence = Math.min(confidence * 1.2, 1.0);
			return confidence;
		}
		final double is = (double)matchCount/realSamples;
		if (matchCount == realSamples || getHeaderConfidence(context) <= 0)
			return is;

		return is + (1.0 - is)/2;
	}
}
