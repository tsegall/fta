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
 * Plugin to detect a US National Provider Identifier (NPI).
 *
 */
public class NPI_US extends LogicalTypeInfinite {
	private CheckDigit validator;
	private static final String NPI_PREFIX = "80840";

	/**
	 * Construct a plugin to detect a US NPI based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public NPI_US(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return validate(trimmed);
	}

	private boolean validate(final String trimmed) {

		if (trimmed.length() != 10 || !Utils.isNumeric(trimmed))
			return false;

		try {
			// Validate the Luhn check digit
			final String checkDigit = validator.calculate(NPI_PREFIX + trimmed.substring(0, 9));
			return checkDigit.charAt(0) == trimmed.charAt(9);
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
			final String base = Utils.getRandomDigits(getRandom(), 9);
			return base + validator.calculate(NPI_PREFIX + base);
		} catch (CheckDigitException e) {
			return null;
		}
	}

	@Override
	public String getRegExp() {
		return "\\d{10}";
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
