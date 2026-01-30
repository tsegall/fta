/*
 * Copyright 2017-2025 Tim Segall
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
import com.cobber.fta.Content;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.KnownTypes;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.SingletonSet;
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

	private boolean randomInitialized;
	private SingletonSet samples;
	private TextProcessor processor;

	/**
	 * Construct a plugin to detect free-form text based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public FreeText(final PluginDefinition plugin) {
		super(plugin);
	}

	public Content bindLanguage(final Content unbound) {
		if (locale == null)
			return unbound;

		final int languageOffset = unbound.reference.indexOf("<LANGUAGE>");
		if (languageOffset != -1)
			return new Content(unbound.type, unbound.reference.replace("<LANGUAGE>", locale.getLanguage().toLowerCase(Locale.ROOT)));

		return unbound;
	}

	@Override
	public String nextRandom() {
		if (!randomInitialized) {
			// Check to see if we have been provided with a set of samples
			if (defn.content != null && samples == null)
				samples = new SingletonSet(bindLanguage(defn.content));
			randomInitialized = true;
		}

		return samples != null ? samples.getRandom(getRandom()) : null;
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		processor = new TextProcessor(locale);

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
			regExp = (facts.multiline ? "(?s)" : "") +  KnownTypes.PATTERN_ANY + RegExpSplitter.qualify(facts.minRawLength, facts.maxRawLength);

		return PluginAnalysis.OK;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		final double confidence = (double)matchCount/realSamples;

		if (getHeaderConfidence(context) > 0)
			return Math.min(1.2 * confidence, 1.0);

		// Header is not recognized so return the lowest threshold we would accept
		return Math.min((double)defn.threshold/100, confidence);
	}
}
