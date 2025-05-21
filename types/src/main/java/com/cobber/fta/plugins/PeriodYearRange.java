/*
 * Copyright 2017-2025 Tim Segall
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
import com.cobber.fta.Keywords;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.Utils;
import com.cobber.fta.dates.DateTimeParser;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect Year Ranges, for example 2016-2019.
 */
public class PeriodYearRange extends LogicalTypeInfinite {
	/** The Regular Expression for this Semantic type. */
	public static final String REGEXP = "\\d{4}-\\d{4}";

	private Keywords keywords;

	/**
	 * Construct a plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public PeriodYearRange(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);
		keywords = Keywords.getInstance(analysisConfig.getLocale());

		return true;
	}

	@Override
	public String nextRandom() {
		final StringBuilder ret = new StringBuilder(9);

		final int yearOne = 1980 + getRandom().nextInt(40);
		final int yearTwo = yearOne + getRandom().nextInt(20);
		return ret.append(yearOne).append('-').append(yearTwo).toString();
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
		return true;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		if (input.length() != 9 || input.charAt(4) != '-' || input.chars().filter(Character::isDigit).count() != 8)
			return false;
		final int yearOne = Utils.getValue(input, 0, 4, 4);
		final int yearTwo = Utils.getValue(input, 0, 5, 4);

		return yearOne >= DateTimeParser.EARLY_LONG_YYYY && yearOne <= DateTimeParser.LATE_LONG_YYYY &&
				yearTwo >= DateTimeParser.EARLY_LONG_YYYY && yearTwo <= DateTimeParser.LATE_LONG_YYYY &&
				yearOne <= yearTwo;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return trimmed.length() == 9 && trimmed.charAt(4) == '-' &&  "\\d{4}-\\d{4}".equals(compressed.toString());
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		if (keywords.match(context.getStreamName(), "YEAR") < 90 &&
				keywords.match(context.getStreamName(), "DATE") < 90)
			return PluginAnalysis.SIMPLE_NOT_OK;

		return (double) matchCount / realSamples >= getThreshold() / 100.0 ?  PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}
}
