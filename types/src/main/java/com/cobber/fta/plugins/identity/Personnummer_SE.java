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

import java.time.LocalDate;

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
import com.cobber.fta.dates.DateTimeParser;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect a Swedish Personnummber.
 *
 * Format is:
 * 	1. Optional Century (2 digits)
 *  2. yyMMdd
 *  3. '-' or '+'
 *  4. 3 digit sequence number
 *  5. Luhn checkdigit
 */
public class Personnummer_SE extends LogicalTypeInfinite {
	private CheckDigit validator;
	private static final int LENGTH_NO_CC = 11;
	private static final int LENGTH_CC = 13;

	/**
	 * Construct a plugin to detect a Swedish Personnummber based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public Personnummer_SE(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return validate(trimmed);
	}

	private boolean validate(final String trimmed) {
		final int len = trimmed.length();
		if (len != LENGTH_NO_CC & len != LENGTH_CC)
			return false;

		// Check all the characters look plausible
		for (int i = 0; i < len; i++) {
			final char ch = trimmed.charAt(i);
			if (i == len - 4 - 1) {
				if (ch != '+' && ch != '-')
					return false;
				continue;
			}

			if (!Utils.isSimpleNumeric(ch))
				return false;
		}

		int century = -1;
		int offset = 0;
		int checkDigitOffset = 0;
		if (len == LENGTH_CC) {
			century = Utils.getValue(trimmed, 0, 2, 2);
			offset += 2;
			checkDigitOffset = 2;
		}

		final int year = Utils.getValue(trimmed, offset, 2, 2);
		offset += 2;

		final int month = Utils.getValue(trimmed, offset, 2, 2);
		offset += 2;
		final int day = Utils.getValue(trimmed, offset, 2, 2);
		offset += 2;

		// '+' is used instead of a '-' to indicate the individual is > 100
		if (century == -1) {
			int bornYear = LocalDate.now().getYear() - year;
			if (trimmed.charAt(offset) == '+')
				bornYear -= 100;
			century = bornYear < 2000 ? 19 : 20;
		}

		offset++;

		// Check that is looks like a reasonable date
		if (!DateTimeParser.isValidDate(century * 100 + year, month, day))
			return false;

		try {
			// Validate the Luhn check digit
			final String checkDigit = validator.calculate(trimmed.substring(checkDigitOffset, checkDigitOffset + 6) + trimmed.substring(offset, offset + 3));
			return checkDigit.charAt(0) == trimmed.charAt(len - 1);
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
		final int year = LocalDate.now().getYear() - getRandom().nextInt(100);
		final String ymd = String.format("%02d%02d%02d", year%100, getRandom().nextInt(12) + 1, getRandom().nextInt(28) + 1);
		final String control = String.format("%03d", getRandom().nextInt(999) + 1);

		try {
			return ymd + "-" + control + validator.calculate(ymd + control);
		} catch (CheckDigitException e) {
			return null;
		}
	}

	@Override
	public String getRegExp() {
		return "(\\d{2})?\\d{6}[-+](?!000)\\d{3}\\d";
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
