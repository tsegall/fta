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
package com.cobber.fta.plugins.identity;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect Japanese Individual Number (個人番号, kojin bangō), also known as My Number (マイナンバー, mai nambā).
 */
public class IN_JA extends LogicalTypeInfinite {
	private static final int IN_LENGTH = 12;
	private static final String BACKOUT_REGEXP = ".*";
	private String regExp = BACKOUT_REGEXP;

	/**
	 * Construct a plugin to detect a Japanese Individual Number based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public IN_JA(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		if (trimmed.length() - charCounts[' '] != IN_LENGTH)
			return false;

		for (int i = 0; i < trimmed.length(); i++) {
			final char ch = trimmed.charAt(i);
			if (ch != ' ' && !Character.isDigit(ch))
				return false;
		}

		return true;
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		return true;
	}

	private int calculateCheckDigit(final String in) {
		long mySum = 0;
		for (int i = 0; i < 5; i++)
			mySum += (11 - i - 5) * (in.charAt(i) - '0');
		for (int i = 5; i < in.length(); i++)
			mySum += (11 - i + 1) * (in.charAt(i) - '0');

		long check = 11 - mySum % 11;
		if (check > 9)
			check = 0;

		return (int)check;
	}

	@Override
	public String nextRandom() {
		final String in = String.valueOf(getRandom().nextInt(90000) + 10000) + String.format("%06d", getRandom().nextInt(1000000));
		return in + calculateCheckDigit(in);
	}

	@Override
	public String getRegExp() {
		return regExp;
	}

	@Override
	public boolean acceptsBaseType(final FTAType type) {
		return type == FTAType.STRING || type == FTAType.LONG;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		final StringBuilder b = new StringBuilder(IN_LENGTH);

		for (int i = 0; i < input.length(); i++) {
			final char ch = input.charAt(i);
			if (ch == ' ')
				continue;
			if (!Character.isDigit(ch))
				return false;
			b.append(ch);
		}

		final int check = calculateCheckDigit(b.substring(0, IN_LENGTH - 1));

		return '0' + check == b.charAt(11);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		if (cardinality.size() < 20 || (double)matchCount/realSamples < getThreshold()/100.0)
			return new PluginAnalysis(BACKOUT_REGEXP);

		regExp = tokenStreams.getRegExp(false);

		return PluginAnalysis.OK;
	}
}
