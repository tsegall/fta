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

import java.util.Map;
import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.KnownPatterns;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.SingletonSet;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect valid Colombian Postal Codes.
 * Note: we used an Infinite :-) Logical Type since the domains is so large.
 */
public class PostalCodeCO extends LogicalTypeInfinite {
	private static final String REGEXP_POSTAL_CODE = "\\d{6}";
	private static final String REGEXP_POSTAL_CODE_56 = "\\d{5,6}";
	private SingletonSet postalRef;
	private Set<String> postals;
	private String regExp = REGEXP_POSTAL_CODE;

	/**
	 * Construct a Colombian Postal code plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public PostalCodeCO(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		final int len = trimmed.length();
		if (len != 5 && len != 6)
			return false;

		final int digits = charCounts['0'] + charCounts['1'] + charCounts['2'] + charCounts['3'] + charCounts['4'] +
				charCounts['5'] + charCounts['6'] + charCounts['7'] + charCounts['8'] + charCounts['9'];

		return digits == len;
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		postalRef = new SingletonSet("resource", "/reference/co_postal_code.csv");
		postals = postalRef.getMembers();

		return true;
	}

	@Override
	public String nextRandom() {
		return postalRef.getRandom(random);
	}

	@Override
	public String getQualifier() {
		return defn.qualifier;
	}

	@Override
	public String getRegExp() {
		return regExp;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.LONG;
	}

	@Override
	public boolean isValid(final String input) {
		final int len = input.length();

		if (len != 5 && len != 6)
			return false;

		boolean ret = false;
		if (len == 5) {
			ret = postals.contains("0" + input);
			if (ret)
				regExp = REGEXP_POSTAL_CODE_56;
		}
		else
			ret = postals.contains(input);

		return ret;
	}

	private String backout() {
		return KnownPatterns.PATTERN_NUMERIC_VARIABLE;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
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
		if (getHeaderConfidence(context.getStreamName()) != 0)
			confidence = Math.min(confidence + Math.min((1.0 - confidence)/2, 0.20), 1.0);

		return confidence;
	}
}
