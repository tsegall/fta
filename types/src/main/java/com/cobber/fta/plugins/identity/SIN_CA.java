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
package com.cobber.fta.plugins.identity;

import org.apache.commons.validator.routines.checkdigit.CheckDigit;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.Utils;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect a CA Social Insurance Number (SIN).
 *
 */
public class SIN_CA extends LogicalTypeInfinite {
	private CheckDigit validator;
	private static final String NPI_PREFIX = "80840";

	/**
	 * Construct a plugin to detect a CA SIN based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public SIN_CA(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return validate(trimmed);
	}

	private boolean validate(final String trimmed) {
		final String compressed = trimmed.replaceAll(" ", "");
		if (compressed.length() != 9 || !Utils.isNumeric(compressed))
			return false;

		if (compressed.charAt(0) == '0' || compressed.charAt(0) == '8')
			return false;

		try {
			// Validate the Luhn check digit
			final String checkDigit = validator.calculate(compressed.substring(0, 8));
			return checkDigit.charAt(0) == compressed.charAt(8);
		} catch (CheckDigitException e) {
			return false;
		}
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		validator = new LuhnCheckDigit();

		return true;
	}

	@Override
	public String nextRandom() {
		try {
			char first = (char)('1' + getRandom().nextInt(8));
			if (first == '8')
				first = '9';

			final String base = first + Utils.getRandomDigits(getRandom(), 7);
			return base.substring(0, 3) + " " + base.substring(3, 6) + " " + base.substring(6, 8) + validator.calculate(base);
		} catch (CheckDigitException e) {
			return null;
		}
	}

	@Override
	public String getRegExp() {
		return "\\d{3} \\d{3} \\d{3}";
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		return validate(input.trim());
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams,
			AnalysisConfig analysisConfig) {
		return getConfidence(matchCount, realSamples, context) >= getThreshold()/100.0 ? PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}
}
