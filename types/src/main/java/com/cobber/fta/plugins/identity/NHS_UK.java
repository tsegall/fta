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
 * Plugin to detect UK NHS numbers.
 */
public class NHS_UK extends LogicalTypeInfinite {
	private static final int LENGTH = 10;
	private static final String BACKOUT_REGEXP = ".*";
	private String regExp = BACKOUT_REGEXP;
	private CheckDigit validator;

	/**
	 * Construct a plugin to detect UK NHS numbers based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public NHS_UK(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		if (trimmed.length() - (charCounts[' ']) != LENGTH)
			return false;

		for (int i = 0; i < trimmed.length(); i++) {
			final char ch = trimmed.charAt(i);
			if (ch == ' ')
				continue;
			if (!Character.isDigit(ch))
				return false;
		}

		return true;
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		validator = new NHSCheckDigit();

		return true;
	}

	@Override
	public String nextRandom() {
		// Currently issued numbers for England, Wales and the Isle of Man are from 400 000 000 to 499 999 999 and 600 000 000 upwards.
		String nhs = String.format("%c%02d%03d%03d",
				"46789".charAt(random.nextInt(5)),
				random.nextInt(100),
				random.nextInt(1000),
				random.nextInt(1000));

		String check = null;
		try {
			check = validator.calculate(nhs);
		} catch (CheckDigitException e) {
			// We might have generated a number that is invalid - i.e. generates a mod of 10
			// in that case fix it by flipping the last digit to its 10's complement which must be
			// number that will no longer generate a modulus of 10.
 			nhs = nhs.substring(0,8) + "9876543210".charAt(nhs.charAt(8) - '0');
 			try {
 				check = validator.calculate(nhs);
 			} catch (CheckDigitException ne) {
 				// Should never happen
 				return null;
 			}
		}

		return nhs.substring(0, 3) + " " + nhs.substring(3, 6) + " " + nhs.substring(6, 9) + check;
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

		final StringBuilder b = new StringBuilder(LENGTH);

		for (int i = 0; i < trimmed.length(); i++) {
			final char ch = input.charAt(i);
			if (ch == ' ' || ch == '.')
				continue;
			if (!Character.isDigit(ch))
				return false;
			b.append(ch);
		}

		if (b.length() != LENGTH)
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
