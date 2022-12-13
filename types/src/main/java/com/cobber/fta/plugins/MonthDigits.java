/* Copyright 2017-2022 Tim Segall
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
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.Utils;
import com.cobber.fta.token.TokenStreams;

/**
* Plugin to detect a Month as digits.
*/
public class MonthDigits extends LogicalTypeInfinite {
	/** The Regular Expression for this Semantic type. */
	public static final String REGEXP = "[1-9]|0[1-9]|1[012]";

	private final Keywords keywords = new Keywords();

	private PluginLocaleEntry dayEntry;

	/**
	 * Construct plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public MonthDigits(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		dayEntry = PluginDefinition.findByQualifier("DAY.DIGITS").getLocaleEntry(locale);

		keywords.initialize(locale);

		return true;
	}

	@Override
	public String nextRandom() {
		return String.valueOf(random.nextInt(12) + 1);
	}

	@Override
	public String getSemanticType() {
		return defn.semanticType;
	}

	@Override
	public FTAType getBaseType() {
		return defn.baseType;
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
	public boolean isValid(final String input, final boolean detectMode) {
		if (input.length() >= 3 || !Utils.isNumeric(input))
			return false;
		final int month = Integer.valueOf(input);
		return month >= 1 && month <= 12;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return isValid(trimmed);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		final String streamName = context.getStreamName();
		if (getHeaderConfidence(streamName) == 0 || (double) matchCount / realSamples < getThreshold() / 100.0)
			return PluginAnalysis.SIMPLE_NOT_OK;

		if (facts.minLong < 1 || facts.maxLong > 12)
			return PluginAnalysis.SIMPLE_NOT_OK;

		// If we have seen both the expected minimum and expected maximum we feel excellent
		if (facts.minLong == 1 && facts.maxLong == 12)
			return PluginAnalysis.OK;

		final int columns = context.getCompositeStreamNames().length;
		// If we have no real context - then nothing we can really do
		if (columns == 1)
			return PluginAnalysis.SIMPLE_NOT_OK;

		// Locate the current column
		int myIndex = -1;
		for (int i = 0; i < columns; i++)
			if (streamName.equals(context.getCompositeStreamNames()[i]))
				myIndex = i;

		// Check the previous column for either a day, month, or year to boost our confidence
		// We check for month as some times month as a number is adjacent to month as a string
		if (myIndex >= 1) {
			String previousStreamName = context.getCompositeStreamNames()[myIndex - 1];
			if (dayEntry.getHeaderConfidence(previousStreamName) >= 99)
				return PluginAnalysis.OK;
			if (keywords.match(previousStreamName, "YEAR", Keywords.MatchStyle.EQUALS) >= 90)
				return PluginAnalysis.OK;
			if (getHeaderConfidence(previousStreamName) >= 99)
				return PluginAnalysis.OK;
		}

		// Check the next column for either a day, month, or year to boost our confidence
		if (myIndex < columns - 1) {
			String nextStreamName = context.getCompositeStreamNames()[myIndex + 1];
			if (dayEntry.getHeaderConfidence(nextStreamName) >= 99)
				return PluginAnalysis.OK;
			if (keywords.match(nextStreamName, "YEAR", Keywords.MatchStyle.EQUALS) >= 90)
				return PluginAnalysis.OK;
			if (getHeaderConfidence(nextStreamName) >= 99)
				return PluginAnalysis.OK;
		}

		return PluginAnalysis.SIMPLE_NOT_OK;
	}
}
