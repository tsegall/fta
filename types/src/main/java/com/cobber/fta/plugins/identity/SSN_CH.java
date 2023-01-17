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
package com.cobber.fta.plugins.identity;

import org.apache.commons.validator.routines.checkdigit.CheckDigit;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.commons.validator.routines.checkdigit.EAN13CheckDigit;

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
 * Plugin to detect Swiss SSN's (AVH Number/Sozialversicherungsnummer).
 */
public class SSN_CH extends LogicalTypeInfinite {
	/** The Semantic type for this Plugin. */
	public static final String SEMANTIC_TYPE = "IDENTITY.SSN_CH";

	private static final int SSN_LENGTH = 13;
	private static final String BACKOUT_REGEXP = ".*";
	private String regExp = BACKOUT_REGEXP;
	private CheckDigit validator;

	/**
	 * Construct a plugin to detect Swiss SSN's based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public SSN_CH(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		if (!trimmed.startsWith("756"))
			return false;

		if (trimmed.length() - (charCounts[' '] + charCounts['.']) != SSN_LENGTH)
			return false;

		for (int i = 0; i < trimmed.length(); i++) {
			final char ch = trimmed.charAt(i);
			if (ch == ' ' || ch == '.')
				continue;
			if (!Character.isDigit(ch))
				return false;
		}

		return true;
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		validator = new EAN13CheckDigit();

		return true;
	}

	@Override
	public String nextRandom() {
		final String avh = String.format("756%04d%04d%d",
				random.nextInt(10000),
				random.nextInt(10000),
				random.nextInt(10));

		String check = null;
		try {
			check = validator.calculate(avh);
		} catch (CheckDigitException e) {
			return null;
		}

		return avh.substring(0, 3) + "." + avh.substring(3, 7) + "." +
			avh.substring(7, 11) + "." + avh.substring(11) + check;
	}

	@Override
	public String getSemanticType() {
		return SEMANTIC_TYPE;
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
		final String trimmed = input.trim();
		if (!trimmed.startsWith("756"))
			return false;

		final StringBuilder b = new StringBuilder(SSN_LENGTH);

		for (int i = 0; i < trimmed.length(); i++) {
			final char ch = input.charAt(i);
			if (ch == ' ' || ch == '.')
				continue;
			if (!Character.isDigit(ch))
				return false;
			b.append(ch);
		}

		if (b.length() != SSN_LENGTH)
			return false;

		return validator.isValid(b.toString());
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		if (cardinality.size() < 20 || (double)matchCount/realSamples < getThreshold()/100.0)
			return new PluginAnalysis(BACKOUT_REGEXP);

		regExp = tokenStreams.getRegExp(false);

		return PluginAnalysis.OK;
	}
}
