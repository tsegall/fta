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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.KnownPatterns;
import com.cobber.fta.LogicalTypeCode;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.RegExpSplitter;
import com.cobber.fta.text.TextProcessor;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect free text - for example, Comments, Descriptions, Notes, ....
 */
public class FreeText extends LogicalTypeInfinite {
	/** The Semantic type for this Plugin. */
	public static final String SEMANTIC_TYPE = "FREE_TEXT";

	/** The Regular Expression for this Semantic type. */
	private static final String REGEXP = ".+";

	private String regExp = REGEXP;

	private int SAMPLE_COUNT = 100;
	private LogicalTypeCode logicalFirst;
	private String[] samples;
	private TextProcessor processor;
	private Map<String, SimpleSamples> simpleSamples = new HashMap<>();

	private static String[] de_verbs = { "betrachtet", "gemalt", "sah", "beobachtet", "studiert" };
	private static String[] de_base_pronouns = { "sie", "er" };
	private static String[] de_nouns = { "Banane", "Wand", "Kirche", "Kathedrale", "Erdbeere", "mango",
			"Fahrrad", "Motorrad", "Auto", "Himbeere", "Zug", "Flugzeug" };

	private static String[] en_verbs = { "contemplated", "painted", "spotted", "observed", "studied" };
	private static String[] en_base_pronouns = { "she", "he", "they" };
	private static String[] en_nouns = { "banana", "wall", "church", "cathederal", "strawberry", "mango",
			"bicycle", "motorbike", "car", "raspberry", "train", "plane" };

	class SimpleSamples {
		String language;
		List<String> verbs;
		List<String> pronouns;
		List<String> nouns;
		String definiteArticle;
		String indefiniteArticle;
		String conjunction;

		SimpleSamples(final String language, final List<String> verbs, final List<String> pronouns, final List<String> nouns) {
			this.language = language;
			this.verbs = verbs;
			this.pronouns = pronouns;
			this.nouns = nouns;

			if ("DE".equals(language)) {
				definiteArticle = " der ";
				indefiniteArticle = " ein ";
				conjunction = " und ";
			}
			else {
				definiteArticle = " the ";
				indefiniteArticle = " a ";
				conjunction = " and ";
			}
		}

		String getSample() {
			return pronouns.get(random.nextInt(pronouns.size())) + " " +
					verbs.get(random.nextInt(verbs.size())) +
					definiteArticle +
					nouns.get(random.nextInt(nouns.size())) +
					conjunction +
					verbs.get(random.nextInt(verbs.size())) +
					indefiniteArticle +
					nouns.get(random.nextInt(nouns.size())) +
					".";
		}
	}

	/**
	 * Construct a plugin to detect free-form text based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public FreeText(final PluginDefinition plugin) {
		super(plugin);
	}

	private synchronized void constructSamples() {
		if (samples != null)
			return;

		if (locale == null)
			locale = Locale.getDefault();
		String language = this.locale.getLanguage().toUpperCase(Locale.ROOT);

		List<String> pronouns = new ArrayList<>(Arrays.asList("DE".equals(language) ? de_base_pronouns : en_base_pronouns));

		for (int i = 3; i < 100; i++) {
			final String name = logicalFirst.nextRandom();
			String initialCap = String.valueOf(name.charAt(0));
			if (name.length() > 1)
				initialCap += name.substring(1).toLowerCase(Locale.ROOT);
			pronouns.add(initialCap);
		}

		SimpleSamples simpleSample = new SimpleSamples(language, new ArrayList<>(Arrays.asList("DE".equals(language) ? de_verbs : en_verbs)), pronouns,
				new ArrayList<>(Arrays.asList("DE".equals(language) ? de_nouns : en_nouns)));
		simpleSamples.put(language, simpleSample);

		samples = new String[SAMPLE_COUNT];
		for (int i = 0; i < SAMPLE_COUNT; i++) {
			samples[i] = simpleSample.getSample();
		}
	}

	@Override
	public String nextRandom() {
		if (samples == null)
			constructSamples();
		return samples[random.nextInt(samples.length)];
	}

	@Override
	public boolean initialize(final Locale locale) throws FTAPluginException {
		super.initialize(locale);

		processor = new TextProcessor(locale);

		final PluginDefinition pluginFirst = PluginDefinition.findByQualifier("NAME.FIRST");
		logicalFirst = (LogicalTypeCode) LogicalTypeFactory.newInstance(pluginFirst,
				pluginFirst.isLocaleSupported(locale) ? locale : Locale.ENGLISH);

		return true;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return isText(trimmed);
	}

	private boolean isText(final String trimmed) {
		return processor.analyze(trimmed).getDetermination() == TextProcessor.Determination.OK;
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public String getRegExp() {
		return regExp;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public boolean isValid(final String input) {
		return input != null && isText(input.trim());
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final TokenStreams tokenStreams,
			final AnalysisConfig analysisConfig) {
		// If we are below the threshold or the cardinality (and uniqueness are low) reject
		if ((double)matchCount/realSamples < getThreshold()/100.0 ||
				(cardinality.size() < 20 && cardinality.size()*3 < realSamples))
			return new PluginAnalysis(REGEXP);

		if (facts != null)
			regExp = KnownPatterns.PATTERN_ANY + RegExpSplitter.qualify(facts.minRawLength, facts.maxRawLength);

		return PluginAnalysis.OK;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final String dataStreamName) {
		double confidence = (double)matchCount/realSamples;

		if (getHeaderConfidence(dataStreamName) != 0)
			return Math.min(1.2 * confidence, 1.0);

		// Header is not recognized so return the lowest threshold we would accept
		return Math.min((double)defn.threshold/100, confidence);
	}
}
