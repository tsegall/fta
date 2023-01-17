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
package com.cobber.fta.plugins;

import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.KnownTypes;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.SingletonSet;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.dates.DateTimeParser;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect valid US Zip codes.
 * Note: we used an Infinite :-) Semantic Type since the domains is so large.
 */
public class USZip5 extends LogicalTypeInfinite {
	/** The Semantic type for this Plugin. */
	public static final String SEMANTIC_TYPE = "POSTAL_CODE.ZIP5_US";
	public static final String REGEXP_ZIP5 = "\\d{5}";
	public static final String REGEXP_VARIABLE = "\\d{3,5}";
	private int minLength = 5;
	private SingletonSet zipsRef;
	private Set<String> zips;

	/**
	 * Construct a US ZIP (See also @link USZipPlus4) plugin based on the Plugin Definition.{@link DateTimeParser}
	 * @param plugin The definition of this plugin.
	 */
	public USZip5(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		final int len = trimmed.length();
		if (len != 5 && len != 4 && len != 3)
			return false;

		final int digits = charCounts['0'] + charCounts['1'] + charCounts['2'] + charCounts['3'] + charCounts['4'] +
				charCounts['5'] + charCounts['6'] + charCounts['7'] + charCounts['8'] + charCounts['9'];
		if (len == 5)
			return digits == 5;

		return zips.contains(len == 3 ? "00" + trimmed : "0" + trimmed);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		zipsRef = new SingletonSet("resource", "/reference/us_zips.csv");
		zips = zipsRef.getMembers();

		return true;
	}

	@Override
	public String nextRandom() {
		return zipsRef.getRandom(random);
	}

	@Override
	public String getSemanticType() {
		return SEMANTIC_TYPE;
	}

	@Override
	public String getRegExp() {
		return minLength == 3 ? REGEXP_VARIABLE : REGEXP_ZIP5;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.LONG;
	}

	@Override
	public boolean isValid(String input, final boolean detectMode, long count) {
		final int len = input.length();

		if (len != 5 && len != 4 && len != 3)
			return false;

		if (len < 5) {
			input = (len == 3 ? "00" : "0") + input;
			minLength = 3;
		}

		return zips.contains(input);
	}

	private String backout() {
		return KnownTypes.PATTERN_NUMERIC_VARIABLE;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		final int headerConfidence = getHeaderConfidence(context.getStreamName());
		if (headerConfidence == 0 && cardinality.size() < 5)
			return new PluginAnalysis(backout());

		if (getConfidence(matchCount, realSamples, context) >= getThreshold()/100.0)
			return PluginAnalysis.OK;

		return new PluginAnalysis(backout());
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		double confidence = (double)matchCount/realSamples;

		// Boost by up to 20% if we like the header
		if (getHeaderConfidence(context.getStreamName()) > 0)
			confidence = Math.min(confidence + Math.min((1.0 - confidence)/2, 0.20), 1.0);

		return confidence;
	}
}
