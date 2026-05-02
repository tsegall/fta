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
package com.cobber.fta.plugins;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.Utils;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to support custom LOCALDATETIME semantic types via a user-supplied regex.
 * Configure via pluginOptions: { "regex": "..." }.
 */
public class PluginCustomLocalDateTime extends LogicalTypeInfinite {
	private Pattern rePattern;
	private String regExp;

	public PluginCustomLocalDateTime(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		if (defn.getOptions() == null || defn.getOptions().get("regex") == null)
			throw new FTAPluginException("Misconfigured plugin - pluginOptions must include 'regex'");

		regExp = (String) defn.getOptions().get("regex");
		if (!Utils.isSafeRegExp(regExp))
			throw new FTAPluginException("Unsafe regex in pluginOptions.regex (nested open-ended quantifiers): " + regExp);
		rePattern = Pattern.compile(regExp);

		return true;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.LOCALDATETIME;
	}

	@Override
	public boolean acceptsBaseType(final FTAType type) {
		return type == FTAType.LOCALDATETIME;
	}

	@Override
	public String nextRandom() {
		final LocalDateTime dt = LocalDateTime.now().minusDays(getRandom().nextInt(3000));
		return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
	}

	@Override
	public boolean isRegExpComplete() {
		return true;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		return rePattern.matcher(input.trim()).matches();
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return rePattern.matcher(trimmed).matches();
	}

	@Override
	public String getRegExp() {
		return regExp;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples,
			final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers,
			final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		return getConfidence(matchCount, realSamples, context) >= getThreshold() / 100.0
				? PluginAnalysis.OK
				: PluginAnalysis.SIMPLE_NOT_OK;
	}
}
