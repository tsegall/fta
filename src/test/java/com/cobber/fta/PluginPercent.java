/*
 * Copyright 2017-2020 Tim Segall
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

import com.cobber.fta.PatternInfo.Type;

public class PluginPercent extends LogicalTypeInfinite {
	public final static String REGEXP = "\\d*\\.?\\d+";

	public PluginPercent(PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(String trimmed, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		try {
			Double d = Double.valueOf(trimmed);
			return d < 1.0;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public boolean initialize(Locale locale) {
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
	public Type getBaseType() {
		return PatternInfo.Type.DOUBLE;
	}

	@Override
	public boolean isValid(String input) {
		try {
			Double d = Double.valueOf(input);
			return d < 1.0;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples,
			StringFacts stringFacts, Map<String, Long> cardinality, Map<String, Long> outliers) {
		return (double)matchCount/realSamples >= getThreshold()/100.0 ? null : ".+";
	}
}
