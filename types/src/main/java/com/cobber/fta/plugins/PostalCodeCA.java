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

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.KnownPatterns;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.core.Utils;
import com.cobber.fta.token.TokenStreams;

import nl.flotsam.xeger.Xeger;

/**
 * Plugin to detect valid Canadian Postal Codes.
 */
public class PostalCodeCA extends LogicalTypeInfinite {
	private static final String REGEXP_POSTAL_CODE = "[A-CEGHJ-NPR-TVXY][0-9][A-CEGHJ-NPR-TV-Z] ?[0-9][A-CEGHJ-NPR-TV-Z][0-9]";

	private boolean randomInitialized;

	private Xeger generator;

	/**
	 * Construct a plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public PostalCodeCA(final PluginDefinition plugin) {
		super(plugin);
	}

	private boolean validLetter(char ch) {
		if (!Utils.isSimpleAlpha(ch))
			return false;
		return ch != 'D' && ch != 'F' && ch != 'I' && ch != 'O' && ch != 'Q' && ch != 'U';
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return isValid(trimmed, true);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		return true;
	}

	@Override
	public String nextRandom() {
		if (!randomInitialized) {
			generator = new Xeger(RegExpGenerator.toAutomatonRE(REGEXP_POSTAL_CODE, true));
			randomInitialized = true;
		}

		return generator.generate();
	}

	@Override
	public String getQualifier() {
		return defn.qualifier;
	}

	@Override
	public String getRegExp() {
		return REGEXP_POSTAL_CODE;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode) {
		final String trimmed = input.trim();
		final int len = trimmed.length();

		if (len != 6 && len != 7)
			return false;

		String merged = null;
		if (len == 7) {
			if (trimmed.charAt(3) != ' ')
				return false;
			merged = trimmed.substring(0, 3) + trimmed.substring(4);
		}
		else
			merged = trimmed;
		merged = merged.toUpperCase(Locale.ROOT);

		char first = merged.charAt(0);

		// Validity
		//  - Format A9A 9A9, where A is a [A-Za-z] and 9 is [0-9]
		//  - Supposed to have a space separating the third and fourth characters (often does not)
		//  - Does not include the letters D, F, I, O, Q or U.
		//  - Initial position does not allow  W or Z.
		if (!validLetter(first) || !Utils.isSimpleNumeric(merged.charAt(1)) ||
				!validLetter(merged.charAt(2)) || !Utils.isSimpleNumeric(merged.charAt(3)) ||
				!validLetter(merged.charAt(4)) || !Utils.isSimpleNumeric(merged.charAt(5)))
			return false;

		if (first == 'W' || first == 'Z')
			return false;

		return true;
	}

	private String backout() {
		return KnownPatterns.PATTERN_ANY_VARIABLE;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		final int headerConfidence = getHeaderConfidence(context.getStreamName());
		if (headerConfidence == 0 && cardinality.size() < 5)
			return new PluginAnalysis(backout());

		if (getConfidence(matchCount, realSamples, context) >= getThreshold()/100.0)
			return PluginAnalysis.OK;

		return new PluginAnalysis(backout());
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		double confidence = (double)matchCount/realSamples;

		// Boost by up to 20% if we like the header
		if (getHeaderConfidence(context.getStreamName()) != 0)
			confidence = Math.min(confidence + Math.min((1.0 - confidence)/2, 0.20), 1.0);

		return confidence;
	}
}
