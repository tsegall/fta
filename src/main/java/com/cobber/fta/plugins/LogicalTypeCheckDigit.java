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

import org.apache.commons.validator.routines.checkdigit.ModulusCheckDigit;

import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.Shapes;
import com.cobber.fta.FactsTypeBased;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;

/**
 * Plugin to detect valid Luhn check digits (typically Credit Cards or IMEI Numbers).
 */
public abstract class LogicalTypeCheckDigit extends LogicalTypeInfinite {
	public static final String BACKOUT_REGEXP = ".*";
	private String regExp = BACKOUT_REGEXP;
	protected ModulusCheckDigit validator;

	public LogicalTypeCheckDigit(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return isValid(trimmed);
	}

	@Override
	public boolean initialize(final Locale locale) throws FTAPluginException {
		super.initialize(locale);

		threshold = 98;

		return true;
	}

	@Override
	public String nextRandom() {
		return null;
	}

	@Override
	public String getRegExp() {
		return regExp;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public boolean isValid(final String input) {
		return validator.isValid(input);
	}

	@Override
	public String isValidSet(final String dataStreamName, final long matchCount, final long realSamples, final FactsTypeBased facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final Shapes shapes) {

		if (cardinality.size() < 20 || (double)matchCount/realSamples < getThreshold()/100.0)
			return BACKOUT_REGEXP;

		regExp = shapes.getRegExp();
		return null;
	}
}
