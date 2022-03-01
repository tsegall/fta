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

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.KnownPatterns;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.SingletonSet;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect valid US Zip plus 4 codes.
 * Note: we used an Infinite :-) Logical Type since the domains is so large.
 */
public class USZipPlus4 extends LogicalTypeInfinite {
	public static final String SEMANTIC_TYPE = "POSTAL_CODE.ZIP5_PLUS4_US";
	public static final String REGEXP_ZIP_PLUS4 = "\\d{5}-\\d{4}";
	public static final String REGEXP_VARIABLE = "\\d{5}(-\\d{4})?";
	private int minLength = 10;
	private SingletonSet zipsRef;
	private Set<String> zips;

	public USZipPlus4(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		final int len = trimmed.length();
		if (len != 10 && len != 5)
			return false;

		final int digits = charCounts['0'] + charCounts['1'] + charCounts['2'] + charCounts['3'] + charCounts['4'] +
				charCounts['5'] + charCounts['6'] + charCounts['7'] + charCounts['8'] + charCounts['9'];
		if (len == 5)
			return digits == 5;

		return len == 10 && trimmed.charAt(5) == '-' && digits == 9;
	}

	@Override
	public boolean initialize(final Locale locale) throws FTAPluginException {
		super.initialize(locale);

		zipsRef = new SingletonSet("resource", "/reference/us_zips.csv");
		zips = zipsRef.getMembers();

		return true;
	}

	@Override
	public String nextRandom() {
		return zipsRef.getAt(random.nextInt(zips.size()));
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public String getRegExp() {
		return minLength == 5 ? REGEXP_VARIABLE : REGEXP_ZIP_PLUS4;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public boolean isValid(String input) {
		final int len = input.length();

		if (len != 10 && len != 5)
			return false;

		if (len == 10)
			input = input.substring(0, 5);
		else
			minLength = 5;

		return zips.contains(input);
	}

	private String backout() {
		return KnownPatterns.PATTERN_ANY_VARIABLE;
	}

	@Override
	public String isValidSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		final int headerConfidence = getHeaderConfidence(context.getStreamName());
		if (headerConfidence == 0 && cardinality.size() < 5)
			return backout();

		return getConfidence(matchCount, realSamples, context.getStreamName()) >= getThreshold()/100.0 ? null : backout();
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final String dataStreamName) {
		double confidence = (double)matchCount/realSamples;

		// Boost by up to 20% if we like the header
		if (getHeaderConfidence(dataStreamName) != 0)
			confidence = Math.min(confidence + Math.min((1.0 - confidence)/2, 0.20), 1.0);

		return confidence;
	}
}
