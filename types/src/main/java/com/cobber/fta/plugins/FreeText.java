/*
 * Copyright 2017-2023 Tim Segall
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
import com.cobber.fta.FiniteMap;
import com.cobber.fta.KnownTypes;
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
	/** The Regular Expression for this Semantic type. */
	private static final String REGEXP = ".+";

	private String regExp = REGEXP;

	private final int SAMPLE_COUNT = 100;
	private LogicalTypeCode logicalFirst;
	private String[] samples;
	private TextProcessor processor;
	private final Map<String, SimpleSamples> simpleSamples = new HashMap<>();

	private static String[] de_verbs = { "betrachtet", "gemalt", "sah", "beobachtet", "studiert" };
	private static String[] de_base_pronouns = { "sie", "er" };
	private static String[] de_nouns = { "Banane", "Wand", "Kirche", "Kathedrale", "Erdbeere", "mango",
			"Fahrrad", "Motorrad", "Auto", "Himbeere", "Zug", "Flugzeug" };

	private static String[] en_verbs = { "contemplated", "painted", "spotted", "observed", "studied" };
	private static String[] en_base_pronouns = { "She", "He", "They" };
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
		final String language = this.locale.getLanguage().toUpperCase(Locale.ROOT);

		final List<String> pronouns = new ArrayList<>(Arrays.asList("DE".equals(language) ? de_base_pronouns : en_base_pronouns));

		for (int i = 3; i < 100; i++) {
			final String name = logicalFirst.nextRandom();
			String initialCap = String.valueOf(name.charAt(0));
			if (name.length() > 1)
				initialCap += name.substring(1).toLowerCase(Locale.ROOT);
			pronouns.add(initialCap);
		}

		final SimpleSamples simpleSample = new SimpleSamples(language, new ArrayList<>(Arrays.asList("DE".equals(language) ? de_verbs : en_verbs)), pronouns,
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

		final StringBuilder result = new StringBuilder(samples[random.nextInt(samples.length)]);
		for (int i = 0; i < random.nextInt(4); i++)
			result.append("  ").append(samples[random.nextInt(samples.length)]);

		return result.toString();
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		processor = new TextProcessor(locale);

		final PluginDefinition pluginFirst = PluginDefinition.findByQualifier("NAME.FIRST");
		// The FreeText Plugin is pseudo supported by any locale, however, if we are generating
		// random entries we use the first name plugins (which may not be supported by the current locale)
		final AnalysisConfig pluginConfig = pluginFirst.isLocaleSupported(locale) ? analysisConfig : new AnalysisConfig(analysisConfig).withLocale(Locale.ENGLISH);
		logicalFirst = (LogicalTypeCode) LogicalTypeFactory.newInstance(pluginFirst, pluginConfig);

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
	public String getRegExp() {
		return regExp;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		return input != null && isText(input.trim());
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams,
			final AnalysisConfig analysisConfig) {

		// If we are below the threshold reject
		if ((double)matchCount/realSamples < getThreshold()/100.0)
			return new PluginAnalysis(REGEXP);

		// No business claiming this is free text if the cardinality is 1!
		if (cardinality.size() == 1)
			return new PluginAnalysis(REGEXP);

		double uniqueness = 1.0;
		// Calculate the uniqueness if it is possible
		if (realSamples != 0 && cardinality.size() != analysisConfig.getMaxCardinality()) {
			// We want to discard the most common entry just in case it is an 'Not Applicable' or something similar
			Map.Entry<String, Long> largest = null;
			for (final Map.Entry<String, Long> entry : cardinality.entrySet())
				if (largest == null || entry.getValue() > largest.getValue())
					largest = entry;

			uniqueness = (double)(cardinality.size() - 1)/(realSamples - largest.getValue());
		}

		// If the uniqueness is low reject
		if (uniqueness < .1)
			return new PluginAnalysis(REGEXP);

		if (facts != null)
			regExp = KnownTypes.PATTERN_ANY + RegExpSplitter.qualify(facts.minRawLength, facts.maxRawLength);

		return PluginAnalysis.OK;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		final double confidence = (double)matchCount/realSamples;

		if (getHeaderConfidence(context.getStreamName()) > 0)
			return Math.min(1.2 * confidence, 1.0);

		// Header is not recognized so return the lowest threshold we would accept
		return Math.min((double)defn.threshold/100, confidence);
	}
}
