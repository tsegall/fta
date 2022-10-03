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
package com.cobber.fta.plugins.identity;

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
 * Plugin to US Employer Identification Numbers.
 */
public class EIN extends LogicalTypeInfinite {

	private static boolean[] invalid = new boolean[100];
	private boolean stringType = false;

	/**
	 * Construct a plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public EIN(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		invalid[0] = invalid[7] = invalid[8] = invalid[9] = true;
		invalid[17] = invalid[18] = invalid[19] = true;
		invalid[28] = invalid[29] = true;
		invalid[49] = true;
		invalid[69] = true;
		invalid[70] = invalid[78] = invalid[79] = true;
		invalid[89] = true;
		invalid[96] = invalid[97] = true;

		return true;
	}

	@Override
	public String nextRandom() {
		return "43-" + Utils.getRandomDigits(random, 7);
	}

	@Override
	public String getQualifier() {
		return defn.qualifier;
	}

	@Override
	public FTAType getBaseType() {
		return stringType ? FTAType.STRING : FTAType.LONG;
	}

	@Override
	public boolean acceptsBaseType(final FTAType type) {
		return type == FTAType.LONG || type == FTAType.STRING;
	}

	@Override
	public String getRegExp() {
		return stringType ? "\\d{2}-\\d{7}" : "\\d{9}";
	}

	@Override
	public boolean isRegExpComplete() {
		return true;
	}

	@Override
	public boolean isValid(final String input, boolean detectMode) {
		final int originalLen = input.length();
		int len = originalLen;
		if (len != 9 && len != 10)
			return false;

		String nInput = input;
		if (len == 10) {
			char sep = input.charAt(2);
			if ('-' != sep && '‐' != sep)
				return false;
			nInput = input.substring(0, 2) + input.substring(3);
			len = 9;
		}

		if (len == 9) {
			if (!Utils.isNumeric(nInput))
				return false;
		}

		int initialTwo = Integer.parseInt(nInput.subSequence(0, 2).toString());
		if (invalid[initialTwo])
			return false;

		if (originalLen == 10)
			stringType = true;

		return true;
	}


	@Override
	public boolean isCandidate(String trimmed, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		return isValid(trimmed, true);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		if (getHeaderConfidence(context.getStreamName()) == 0)
			return PluginAnalysis.SIMPLE_NOT_OK;

		if (cardinality.size() < 20 && getHeaderConfidence(context.getStreamName()) < 95)
			return PluginAnalysis.SIMPLE_NOT_OK;

		return (double) matchCount / realSamples >= getThreshold() / 100.0 ?  PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}

	@Override
	public boolean isClosed() {
		return true;
	}
}
