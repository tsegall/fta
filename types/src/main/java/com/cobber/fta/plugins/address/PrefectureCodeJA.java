/*
 * Copyright 2017-2026 Tim Segall
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
package com.cobber.fta.plugins.address;

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
 * Plugin to detect Japanese Prefecture JIS X 0401 numeric codes (01–47).
 *
 * These two-digit codes are highly ambiguous (they overlap with age buckets,
 * region codes, months, etc.), so detection requires a matching column header.
 * Without a header confidence > 0 the plugin always declines.
 */
public class PrefectureCodeJA extends LogicalTypeInfinite {

	private static final int MIN_CODE = 1;
	private static final int MAX_CODE = 47;
	private static final String REGEXP = "\\d{1,2}";

	public PrefectureCodeJA(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);
		return true;
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
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		final int len = trimmed.length();
		if (len < 1 || len > 2)
			return false;
		for (int i = 0; i < len; i++)
			if (!Character.isDigit(trimmed.charAt(i)))
				return false;
		final int val = Integer.parseInt(trimmed);
		return val >= MIN_CODE && val <= MAX_CODE;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		final String trimmed = input.trim();
		final int len = trimmed.length();
		if (len < 1 || len > 2)
			return false;
		for (int i = 0; i < len; i++)
			if (!Character.isDigit(trimmed.charAt(i)))
				return false;
		final int val = Integer.parseInt(trimmed);
		return val >= MIN_CODE && val <= MAX_CODE;
	}

	@Override
	public String nextRandom() {
		return String.format("%02d", getRandom().nextInt(MAX_CODE) + MIN_CODE);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples,
			final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers,
			final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		if (getHeaderConfidence(context) <= 0)
			return PluginAnalysis.SIMPLE_NOT_OK;

		return (double) matchCount / realSamples >= getThreshold() / 100.0
				? PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}
}
