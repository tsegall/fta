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

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.KnownTypes;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.Utils;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect valid Gemeente Codes (NL).
 */
public class MunicipalityCodeNL extends LogicalTypeInfinite {
	public static final String REGEXP_GM = "GM\\d{4}";
	public static final String REGEXP_G = "G\\d{4}";
	public static final String REGEXP_DIGITS = "\\d{2.4}";
	private int minLength = Integer.MAX_VALUE;
	private int maxLength = Integer.MIN_VALUE;

	/**
	 * Construct a plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public MunicipalityCodeNL(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		final int len = trimmed.length();
		if (len > 6 || len < 2)
			return false;

		final int digits = charCounts['0'] + charCounts['1'] + charCounts['2'] + charCounts['3'] + charCounts['4'] +
				charCounts['5'] + charCounts['6'] + charCounts['7'] + charCounts['8'] + charCounts['9'];

		return validate(trimmed, len, digits);
	}

	private boolean validate(final String trimmed, final int len, final long digits) {
		boolean ret = false;
		if (len == 6)
			ret = trimmed.charAt(0) == 'G' && trimmed.charAt(1) == 'M' && digits == 4;
		else if (len == 5)
			ret = trimmed.charAt(0) == 'G' && digits == 4;
		else
			ret = len == digits;

		if (!ret)
			return false;

		if (len < minLength)
			minLength = len;
		if (len > maxLength)
			maxLength = len;

		return true;
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		return true;
	}

	@Override
	public String nextRandom() {
		return "GM" + Utils.getRandomDigits(random, 4);
	}

	@Override
	public String getRegExp() {
		if (minLength == 6)
			return REGEXP_GM;
		else if (minLength == 5)
			return REGEXP_G;

		return REGEXP_DIGITS;
	}

	@Override
	public FTAType getBaseType() {
		return minLength == 5 || minLength == 6 ? FTAType.STRING : FTAType.LONG;
	}

	@Override
	public boolean acceptsBaseType(final FTAType type) {
		return type == FTAType.STRING || type == FTAType.LONG;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		final String trimmed = input.trim();
		return validate(trimmed, trimmed.length(), input.chars().filter(Character::isDigit).count());
	}

	private String backout() {
		return KnownTypes.PATTERN_ANY_VARIABLE;
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
}
