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
import java.util.Map;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/**
 * Plugin to detect Phone Numbers.
 */
public class PhoneNumberLT extends LogicalTypeInfinite  {
	/** The Semantic type for this Plugin. */
	public static final String SEMANTIC_TYPE = "TELEPHONE";

	/** The Regular Express for this Semantic type. */
	public static final String REGEXP = ".*";

	/** The Regular Express for this Semantic type. */
	private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

	/**
	 * Construct a Phone Number plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public PhoneNumberLT(final PluginDefinition plugin) {
		super(plugin);
	}

	// Get a random digit string of length len digits, first must not be a zero
	private String getRandomDigits(final int len) {
		final StringBuilder b = new StringBuilder(len);
		b.append(random.nextInt(9) + 1);
		for (int i = 1; i < len; i++)
			b.append(random.nextInt(10));
		return b.toString();
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
			attempt = getRandomDigits(nationalSignificantNumber.length());
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
	public boolean initialize(final Locale locale) throws FTAPluginException {
		super.initialize(locale);
		return true;
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
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
	public boolean isValid(final String input) {
		try {
			// The Google library is very permissive and generally strips punctuation, we want to be
			// a little more discerning so that we don't treat ordinary numbers as phone numbers
			if (input.indexOf(',') != -1 || input.chars().filter(ch -> ch == '.').count() == 1)
				return false;
			final PhoneNumber phoneNumber = phoneUtil.parse(input, locale.getCountry());
			return phoneUtil.isValidNumber(phoneNumber);
		}
		catch (NumberParseException e) {
			return false;
		}
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return trimmed.length() > 5 && isValid(trimmed);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		if (getHeaderConfidence(context.getStreamName()) == 0 && cardinality.size() <= 20 || getConfidence(matchCount, realSamples, context.getStreamName()) < getThreshold()/100.0)
			return new PluginAnalysis(REGEXP);

		return PluginAnalysis.OK;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final String dataStreamName) {
		double is = (double)matchCount/realSamples;
		// Boost by up to 20% if we like the header
		if (getHeaderConfidence(dataStreamName) != 0)
			is = Math.min(is * 1.2, 1.0);

		return is;
	}
}
