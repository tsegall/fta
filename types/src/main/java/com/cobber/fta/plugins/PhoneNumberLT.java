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
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.ValidationResult;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/**
 * Plugin to detect Phone Numbers.
 */
public class PhoneNumberLT extends LogicalTypeInfinite  {
	/** The Regular Expression for this Semantic type. */
	public static final String REGEXP = ".*";

	private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

	private String country;
	private boolean localNumbersValid;
	private int nonLocal;
	private boolean onlyDigits = true;

	/**
	 * Construct a Phone Number plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public PhoneNumberLT(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		final String country = locale.getCountry();
		final PhoneNumber sample = phoneUtil.getExampleNumberForType(country, PhoneNumberUtil.PhoneNumberType.MOBILE);
		if (sample == null)
			return null;

		final String nationalSignificantNumber = phoneUtil.getNationalSignificantNumber(sample);

		String attempt;
		Phonenumber.PhoneNumber phoneNumber;
		do {
			attempt = Utils.getRandomDigits(random, nationalSignificantNumber.length());
			try {
				phoneNumber = phoneUtil.parse(attempt, country);
			} catch (NumberParseException e) {
				return null;
			}
		}
		while (!phoneUtil.isValidNumber(phoneNumber));

		return attempt;
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		country = locale.getCountry();

		localNumbersValid = "CO".equals(country);

		return true;
	}

	@Override
	public String getSemanticType() {
		return defn.semanticType;
	}

	@Override
	public FTAType getBaseType() {
		return onlyDigits ? FTAType.LONG : FTAType.STRING;
	}

	@Override
	public boolean acceptsBaseType(final FTAType type) {
		return type == FTAType.STRING || type == FTAType.LONG;
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public boolean isRegExpComplete() {
		return false;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		try {
			// The Google library is very permissive and generally strips punctuation, we want to be
			// a little more discerning so that we don't treat ordinary numbers as phone numbers
			if (input == null || input.indexOf(',') != -1 || input.chars().filter(ch -> ch == '.').count() == 1)
				return false;

			return validTest(input);
		}
		catch (NumberParseException e) {
			return false;
		}
	}

	private boolean validTest(final String input) throws NumberParseException {
		final PhoneNumber number = phoneUtil.parse(input, country);

		final boolean ret = phoneUtil.isValidNumber(number);
		if (!localNumbersValid || ret) {
			if (ret && onlyDigits)
				onlyDigits = Utils.isNumeric(input);
			return ret;
		}

		// Some countries (e.g. Colombia) often record Phone Numbers in their local-only format, if this
		// is the case check whether the number is valid as a local number.
		ValidationResult result = phoneUtil.isPossibleNumberWithReason(number);
		if (result == ValidationResult.IS_POSSIBLE_LOCAL_ONLY) {
			nonLocal++;
			if (onlyDigits)
				onlyDigits = Utils.isNumeric(input);
			return true;
		}

		return false;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return trimmed.length() > 5 && isValid(trimmed, true, -1);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		// If we are allowing local-only numbers then insist on some signal from the header
		if (localNumbersValid && nonLocal != 0 && getHeaderConfidence(context.getStreamName()) <= 0)
			return new PluginAnalysis(onlyDigits ? KnownTypes.PATTERN_NUMERIC_VARIABLE : REGEXP);

		if (getHeaderConfidence(context.getStreamName()) == 0 && cardinality.size() <= 20 || getConfidence(matchCount, realSamples, context) < getThreshold()/100.0)
			return new PluginAnalysis(onlyDigits ? KnownTypes.PATTERN_NUMERIC_VARIABLE : REGEXP);

		return PluginAnalysis.OK;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		double is = (double)matchCount/realSamples;
		// Boost by up to 20% if we like the header
		if (getHeaderConfidence(context.getStreamName()) > 0)
			is = Math.min(is * 1.2, 1.0);

		return is;
	}
}
