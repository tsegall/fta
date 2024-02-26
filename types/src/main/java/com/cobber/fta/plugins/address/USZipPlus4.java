/*
 * Copyright 2017-2024 Tim Segall
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

import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Content;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.KnownTypes;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.SingletonSet;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.Utils;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect valid US Zip plus 4 codes.
 * Note: we used an Infinite :-) Semantic Type since the domains is so large.
 */
public class USZipPlus4 extends LogicalTypeInfinite {
	public static final String REGEXP_ZIP_PLUS4_HYPHEN = "\\d{5}-\\d{4}";
	public static final String REGEXP_ZIP_PLUS4 = "\\d{9}";
	public static final String REGEXP_VARIABLE_HYPHEN = "\\d{5}(-\\d{4})?";
	public static final String REGEXP_VARIABLE = "\\d{5}|\\d{9}";
	private int minLength = Integer.MAX_VALUE;
	private int maxLength = Integer.MIN_VALUE;
	private boolean allDigits = true;
	private SingletonSet zipsRef;
	private Set<String> zips;

	/**
	 * Construct a ZIP+4 (See also @link USZip) plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public USZipPlus4(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		final int len = trimmed.length();
		if (len != 12 && len != 10 && len != 9 && len != 8 && len != 5 && len != 4 && len != 3)
			return false;

		final int digits = charCounts['0'] + charCounts['1'] + charCounts['2'] + charCounts['3'] + charCounts['4'] +
				charCounts['5'] + charCounts['6'] + charCounts['7'] + charCounts['8'] + charCounts['9'];
		if (allDigits)
			allDigits = digits == len;

		switch (len) {
		case 3:
			return digits == 3 && zips.contains("00" + trimmed);
		case 4:
			return digits == 4 && zips.contains("0" + trimmed);
		case 5:
			return digits == 5;
		case 8:
			return digits == 8;
		case 9:
			return digits == 9;
		case 10:
			return digits == 9 && (trimmed.charAt(5) == '-' || trimmed.charAt(5) == ' ');
		case 12:
			return digits == 9 && trimmed.charAt(5) == ' ' && trimmed.charAt(6) == '-' && trimmed.charAt(7) == ' ';
		}

		return false;
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		zipsRef = new SingletonSet(new Content("resource", "/reference/us_zips.csv"));
		zips = zipsRef.getMembers();

		return true;
	}

	@Override
	public String nextRandom() {
		return getRandom().nextInt(10) >= 8 ? zipsRef.getRandom(getRandom()) : zipsRef.getRandom(getRandom()) + "-" + getRandom().nextInt(10) + getRandom().nextInt(10) + getRandom().nextInt(10) + getRandom().nextInt(10);
	}

	@Override
	public String getRegExp() {
		if (minLength == 5)
			return maxLength == 10 ? REGEXP_VARIABLE_HYPHEN : REGEXP_VARIABLE;

		return minLength == 9 ? REGEXP_ZIP_PLUS4 : REGEXP_ZIP_PLUS4_HYPHEN;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public boolean acceptsBaseType(final FTAType type) {
		return type == FTAType.STRING || type == FTAType.LONG;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, long count) {
		String trimmed = input.trim();
		final int len = trimmed.length();

		if (len != 12 && len != 10 && len != 9 && len != 8 && len != 5 && len != 4 && len != 3)
			return false;

		if (len == 10 && trimmed.charAt(5) != '-' && trimmed.charAt(5) != ' ')
			return false;
		if (len == 12 && trimmed.charAt(6) != '-')
			return false;

		if (allDigits)
			allDigits = Utils.isNumeric(input);

		if (len < 5)
			trimmed = (len == 3 ? "00" : "0") + trimmed;
		else if (len == 8)
			trimmed = "0" + input.substring(0, 4);
		else if (len >= 9)
			trimmed = input.substring(0, 5);

		if (len < minLength)
			minLength = len;
		if (len > maxLength)
			maxLength = len;

		return zips.contains(trimmed);
	}

	private String backout() {
		return KnownTypes.PATTERN_ANY_VARIABLE;
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
		final String dataStreamName = context.getStreamName();
		double confidence = (double)matchCount/realSamples;

		// If we do not have an embedded '-' then insist that the header is good
		if (allDigits && getHeaderConfidence(dataStreamName) <= 0)
			return 0;

		// Boost by up to 20% if we like the header
		if (getHeaderConfidence(dataStreamName) > 0)
			confidence = Math.min(confidence + Math.min((1.0 - confidence)/2, 0.20), 1.0);

		return confidence;
	}
}
