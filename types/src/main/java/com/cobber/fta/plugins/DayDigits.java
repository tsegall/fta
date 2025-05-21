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
import com.cobber.fta.PluginLocaleEntry;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.Utils;
import com.cobber.fta.token.TokenStreams;

/**
* Plugin to detect a Day as digits.
*/
public class DayDigits extends LogicalTypeInfinite {
	/** The Regular Expression for this Semantic type. */
	public static final String REGEXP = "[1-9]|0[1-9]|[12][0-9]|3[01]";

	private Keywords keywords;

	private PluginLocaleEntry monthEntry;

	/**
	 * Construct plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public DayDigits(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		monthEntry = PluginDefinition.findByName("MONTH.DIGITS").getLocaleEntry(locale);
		keywords = Keywords.getInstance(analysisConfig.getLocale());

		return true;
	}

	@Override
	public String nextRandom() {
		return String.valueOf(getRandom().nextInt(31) + 1);
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
		if (input.length() >= 3 || !Utils.isNumeric(input))
			return false;
		final int day = Integer.parseInt(input);
		return day >= 1 && day <= 31;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return isValid(trimmed, true, 0);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		final String streamName = context.getStreamName();
		final int headerConfidence = getHeaderConfidence(streamName);
		if (headerConfidence == 0 || (double) matchCount / realSamples < getThreshold() / 100.0)
			return PluginAnalysis.SIMPLE_NOT_OK;

		if (facts.getMinLong() < 1 || facts.getMaxLong() > 31)
			return PluginAnalysis.SIMPLE_NOT_OK;

		// If we have seen both the expected minimum and expected maximum we feel excellent
		if (headerConfidence == 99 || (facts.getMinLong() == 1 && facts.getMaxLong() == 31))
			return PluginAnalysis.OK;

		// Confidence in header is not great, so could easily be "day of week" unless we have seen a large number
		if (facts.getMaxLong() <= 7)
			return PluginAnalysis.SIMPLE_NOT_OK;

		// Locate the current column
		final int current = context.getStreamIndex();
		// If we have no real context - then nothing we can really do
		if (current == -1)
			return PluginAnalysis.SIMPLE_NOT_OK;

		// Check the previous column for either a day, month, or year to boost our confidence
		// We check for day as some times month as a number is adjacent to day as a string
		if (current >= 1) {
			final String previousStreamName = context.getCompositeStreamNames()[current - 1];
			if (monthEntry.getHeaderConfidence(previousStreamName) >= 99)
				return PluginAnalysis.OK;
			if (context.isPreviousSemanticType("MONTH.DIGITS"))
				return PluginAnalysis.OK;
			if (keywords.match(previousStreamName, "YEAR") >= 90)
				return PluginAnalysis.OK;
			if (getHeaderConfidence(previousStreamName) >= 99)
				return PluginAnalysis.OK;
		}

		// Check the next column for either a day, month, or year to boost our confidence
		if (current < context.getCompositeStreamNames().length - 1) {
			final String nextStreamName = context.getCompositeStreamNames()[current + 1];
			if (monthEntry.getHeaderConfidence(nextStreamName) >= 99)
				return PluginAnalysis.OK;
			if (context.isNextSemanticType("MONTH.DIGITS"))
				return PluginAnalysis.OK;
			if (keywords.match(nextStreamName, "YEAR") >= 90)
				return PluginAnalysis.OK;
			if (getHeaderConfidence(nextStreamName) >= 99)
				return PluginAnalysis.OK;
		}

		return PluginAnalysis.SIMPLE_NOT_OK;
	}
}
