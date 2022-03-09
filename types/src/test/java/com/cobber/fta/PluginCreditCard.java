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
package com.cobber.fta;

import java.util.Locale;
import java.util.Map;

import org.apache.commons.validator.routines.CreditCardValidator;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;

public class PluginCreditCard extends LogicalTypeInfinite {
	public final static String REGEXP = "(?:\\d[ -]*?){13,16}";
	private static CreditCardValidator validator;

	static {
		validator = CreditCardValidator.genericCreditCardValidator();
	}

	public PluginCreditCard(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return validator.isValid(trimmed.replaceAll("[\\s\\-]", ""));
	}

	@Override
	public boolean initialize(final Locale locale) throws FTAPluginException {
		super.initialize(locale);

		return true;
	}

	@Override
	public String nextRandom() {
		return null;
	}

	@Override
	public String getQualifier() {
		return "CREDITCARD";
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.LONG;
	}

	@Override
	public boolean isValid(final String input) {
		return validator.isValid(input.replaceAll("[\\s\\-]", ""));
	}

	@Override
	public String isValidSet(final AnalyzerContext context, final long matchCount, final long realSamples,
			String currentRegExp, final Facts facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, TokenStreams tokenStreams, AnalysisConfig analysisConfig) {
		return (double)matchCount/realSamples >= getThreshold()/100.0 ? null : ".+";
	}
}
