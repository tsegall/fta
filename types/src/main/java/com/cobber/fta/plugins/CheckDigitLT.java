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
package com.cobber.fta.plugins;

import org.apache.commons.validator.routines.checkdigit.CheckDigit;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect valid check digits.
 */
public abstract class CheckDigitLT extends LogicalTypeInfinite {
	public static final String BACKOUT_REGEXP = ".*";
	private String regExp = BACKOUT_REGEXP;
	protected CheckDigit validator;
	protected final int length;				/* Length of check digit - -1 indicates variable length */

	public CheckDigitLT(final PluginDefinition plugin, final int length) {
		super(plugin);
		this.length = length;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return isValid(trimmed, true, 0);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		threshold = 98;

		return true;
	}

	@Override
	public String nextRandom() {
		return null;
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
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		return (length == -1 || input.length() == length) && validator.isValid(input);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		if (cardinality.size() < 20 || (double)matchCount/realSamples < getThreshold()/100.0)
			return new PluginAnalysis(BACKOUT_REGEXP);

		regExp = tokenStreams.getRegExp(false);

		return PluginAnalysis.OK;
	}
}
