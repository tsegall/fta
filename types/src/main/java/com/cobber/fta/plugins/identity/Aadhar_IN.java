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

import java.util.Locale;
import java.util.Map;

import org.apache.commons.validator.routines.checkdigit.CheckDigit;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.commons.validator.routines.checkdigit.VerhoeffCheckDigit;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect Indian Aadhar.
 */
public class Aadhar_IN extends LogicalTypeInfinite {
	public static final String SEMANTIC_TYPE = "IDENTITY.AADHAR_IN";
	private static final int ID_LENGTH = 12;
	public static final String BACKOUT_REGEXP = ".*";
	private String regExp = BACKOUT_REGEXP;
	private CheckDigit validator;

	public Aadhar_IN(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		if (trimmed.length() - (charCounts[' ']) != ID_LENGTH)
			return false;

		// Currently numbers starting with 0 & 1 are reserved for future use
		if (trimmed.charAt(0) == '0' || trimmed.charAt(0) == '1')
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
	public boolean initialize(final Locale locale) throws FTAPluginException {
		super.initialize(locale);

		validator = new VerhoeffCheckDigit();

		return true;
	}

	@Override
	public String nextRandom() {
		final String aadhar = String.format("%04d%04d%03d",
				2000 + random.nextInt(8000),
				random.nextInt(10_000),
				random.nextInt(1000));

		String check = null;
		try {
			check = validator.calculate(aadhar);
		} catch (CheckDigitException e) {
			return null;
		}

		return aadhar.substring(0, 4) + " " + aadhar.substring(4, 8) + " " +
			aadhar.substring(8, 11) + check;
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
	public boolean acceptsBaseType(final FTAType type) {
		return type == FTAType.STRING || type == FTAType.LONG;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public boolean isValid(final String input) {
		final String trimmed = input.trim();

		// Currently numbers starting with 0 & 1 are reserved for future use
		if (trimmed.charAt(0) == '0' || trimmed.charAt(0) == '1')
			return false;

		final StringBuilder b = new StringBuilder(ID_LENGTH);

		for (int i = 0; i < trimmed.length(); i++) {
			final char ch = input.charAt(i);
			if (ch == ' ')
				continue;
			if (!Character.isDigit(ch))
				return false;
			b.append(ch);
		}

		if (b.length() != ID_LENGTH)
			return false;

		return validator.isValid(b.toString());
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		if (cardinality.size() < 20 || (double)matchCount/realSamples < getThreshold()/100.0)
			return new PluginAnalysis(BACKOUT_REGEXP);

		regExp = tokenStreams.getRegExp(false);

		return PluginAnalysis.OK;
	}
}
