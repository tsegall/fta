/*
 * Copyright 2017-2021 Tim Segall
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
import com.cobber.fta.FactsTypeBased;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.Shapes;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

/**
 * Plugin to detect Phone Numbers.
 */
public class LogicalTypePhoneNumber extends LogicalTypeInfinite  {
	public static final String SEMANTIC_TYPE = "TELEPHONE";
	public static final String REGEXP = ".*";
	private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
	private static String[] areaCodes = new String[] { "617", "781", "303", "970", "212" };

	public LogicalTypePhoneNumber(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		final String base = "+1" + areaCodes[random.nextInt(areaCodes.length)];
		while (true) {
			final StringBuilder result = new StringBuilder(base);
			for (int i = 0; i < 7; i++)
				result.append(random.nextInt(10));
			final String attempt = result.toString();
			if (isValid(attempt))
				return attempt;
		}
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
			final Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(input, locale.getCountry());
			return phoneUtil.isValidNumber(phoneNumber);
		}
		catch (NumberParseException e) {
			return false;
		}
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return isValid(trimmed);
	}

	@Override
	public String isValidSet(final AnalyzerContext context, final long matchCount, final long realSamples, String currentRegExp, final FactsTypeBased facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final Shapes shapes, AnalysisConfig analysisConfig) {
		if (getHeaderConfidence(context.getStreamName()) == 0 && cardinality.size() <= 20 || getConfidence(matchCount, realSamples, context.getStreamName()) < getThreshold()/100.0)
			return REGEXP;
		return null;
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
