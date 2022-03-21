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

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.KnownPatterns;
import com.cobber.fta.LogicalTypeCode;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.RegExpSplitter;
import com.cobber.fta.text.TextProcessor;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect free text - for example, Comments, Descriptions, Notes, ....
 */
public class FreeText extends LogicalTypeInfinite{
	public static final String SEMANTIC_TYPE = "FREE_TEXT";
	public static final String REGEXP = ".+";

	private String regExp = REGEXP;

	private int SAMPLE_COUNT = 100;
	private LogicalTypeCode logicalFirst;
	private static String[] samples;
	private static String[] verbs = { "contemplated", "painted", "saw", "observed", "studied" };
	private static String[] pronouns;
	private static String[] nouns = { "banana", "wall", "church", "cathederal", "strawberry", "mango",
			"bicycle", "motorbike", "car", "raspberry", "train", "plane" };
	private TextProcessor processor;

	public FreeText(final PluginDefinition plugin) {
		super(plugin);
	}

	private synchronized void constructSamples() {
		if (samples != null)
			return;

		pronouns = new String[100];
		pronouns[0] = "She";
		pronouns[1] = "He";
		pronouns[2] = "They";

		for (int i = 3; i < 100; i++) {
			String name = logicalFirst.nextRandom();
			pronouns[i] = String.valueOf(name.charAt(0));
			if (name.length() > 1)
				pronouns[i] += name.substring(1).toLowerCase(Locale.ROOT);
		}

		samples = new String[SAMPLE_COUNT];
		for (int i = 0; i < SAMPLE_COUNT; i++) {
			samples[i] = pronouns[random.nextInt(pronouns.length)] + " " +
					verbs[random.nextInt(verbs.length)] +
					" the " +
					nouns[random.nextInt(nouns.length)] +
					" and " +
					verbs[random.nextInt(verbs.length)] +
					" a " +
					nouns[random.nextInt(nouns.length)] +
					".";
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
		logicalFirst = (LogicalTypeCode) LogicalTypeFactory.newInstance(pluginFirst, locale);

		return true;
	}

	@Override
	public boolean isCandidate(String trimmed, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		return isText(trimmed);
	}

	private boolean isText(String trimmed) {
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
	public boolean isValid(String input) {
		return input == null ? false : isText(input.trim());
	}

	@Override
	public String isValidSet(AnalyzerContext context, long matchCount, long realSamples, String currentRegExp,
			Facts facts, Map<String, Long> cardinality, Map<String, Long> outliers, TokenStreams tokenStreams,
			AnalysisConfig analysisConfig) {
		// If we are below the threshold or the cardinality (and uniqueness are low) reject
		if ((double)matchCount/realSamples < getThreshold()/100.0 ||
				(cardinality.size() < 20 && cardinality.size()*3 < realSamples))
			return REGEXP;

		if (facts != null)
			regExp = KnownPatterns.PATTERN_ANY + RegExpSplitter.qualify(facts.minRawLength, facts.maxRawLength);

		return null;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final String dataStreamName) {
		double confidence = (double)matchCount/realSamples;

		return confidence;
	}
}
