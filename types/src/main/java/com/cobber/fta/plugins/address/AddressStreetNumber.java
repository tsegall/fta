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
package com.cobber.fta.plugins.address;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.KnownTypes;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.Utils;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect a Street Name (no number!). (English-language only).
 * Note: This will also detect a Street Intersection.
 */
public class AddressStreetNumber extends LogicalTypeInfinite {
	private boolean onlyNumeric = true;

	/**
	 * Construct a plugin to detect a Street Number based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public AddressStreetNumber(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		return String.valueOf(random.nextInt(1000) + 1);
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
		return onlyNumeric ? FTAType.LONG : FTAType.STRING;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		return validation(input.trim(), detectMode);
	}

	private boolean validation(final String trimmed, final boolean detectMode) {
		if (!AddressCommon.isAddressNumber(trimmed))
			return false;

		if (onlyNumeric)
			onlyNumeric = Utils.isNumeric(trimmed);

		return true;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return validation(trimmed, true);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		return getConfidence(matchCount, realSamples, context) >= getThreshold()/100.0 ? PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		final double confidence = (double)matchCount/realSamples;
		if (getHeaderConfidence(context.getStreamName()) >= 99)
			return confidence;

		// A close field must have a Semantic Type that indicates it is a Street name (with or without the marker)
		final Integer closest = context.indexOfSemanticType(analysisConfig.bindSemanticType("STREET_NAME_<LANGUAGE>"), analysisConfig.bindSemanticType("STREET_NAME_BARE_<LANGUAGE>"));
		if (closest == null || Math.abs(closest) > 2)
			return 0.0;

		return confidence;
	}

	@Override
	public String getRegExp() {
		return onlyNumeric ? KnownTypes.PATTERN_NUMERIC_VARIABLE : KnownTypes.PATTERN_ANY_VARIABLE;
	}
}
