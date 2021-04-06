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
package com.cobber.fta;

import java.util.Locale;
import java.util.Map;

import com.cobber.fta.core.FTAType;

public class PluginPercent extends LogicalTypeInfinite {
	public final static String REGEXP = "\\d*\\.?\\d+";

	public PluginPercent(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		try {
			return Double.valueOf(trimmed) < 1.0;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public boolean initialize(final Locale locale) {
		super.initialize(locale);

		return true;
	}

	@Override
	public String nextRandom() {
		return null;
	}

	@Override
	public String getQualifier() {
		return "PERCENT";
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.DOUBLE;
	}

	@Override
	public boolean isValid(final String input) {
		try {
			return Double.valueOf(input) < 1.0;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public String isValidSet(final String dataStreamName, final long matchCount, final long realSamples,
			final TypeFacts facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, Shapes shapes) {
		return (double)matchCount/realSamples >= getThreshold()/100.0 ? null : ".+";
	}
}
