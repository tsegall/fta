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
 * Plugin to detect US SSN's
 */
public class SSN_US extends LogicalTypeInfinite {
	private static final int SSN_LENGTH = 11;

	/** The Regular Expression for this Semantic type. */
	private static final String REGEXP = "(?!666|000|9\\d{2})\\d{3}-(?!00)\\d{2}-(?!0{4})\\d{4}";

	private static final String BACKOUT_REGEXP = ".*";

	/**
	 * Construct a plugin to detect US SSN's based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public SSN_US(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return "\\d{3}-\\d{2}-\\d{4}".equals(compressed.toString());
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		return true;
	}

	@Override
	public String nextRandom() {
		final StringBuilder b = new StringBuilder();
		b.setLength(0);
		int component = random.nextInt(899) + 1;
		if (component == 666)
			component = 667;
		b.append(String.format("%03d", component)).append('-').append(String.format("%02d", random.nextInt(99) + 1)).append('-').append(String.format("%04d", random.nextInt(9999) + 1));

		return b.toString();
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public boolean acceptsBaseType(final FTAType type) {
		return type == FTAType.STRING || type == FTAType.LONG;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		final String trimmed = input.trim();
		if (trimmed.length() != SSN_LENGTH)
			return false;

		for (int i = 0; i < SSN_LENGTH; i++) {
			final char ch = trimmed.charAt(i);
			if (i == 3 || i == 6) {
				if (ch != '-')
					return false;
			}
			else {
				if (!Character.isDigit(ch))
					return false;
			}
		}

		final int first = Utils.getValue(trimmed, 0, 3, 3);
		if (first == 0 || first == 666 || first >= 900)
			return false;

		final int second = Utils.getValue(trimmed, 4, 2, 2);
		if (second == 0)
			return false;

		final int third = Utils.getValue(trimmed, 7, 4, 4);
		return third != 0;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		if ((double)matchCount/realSamples < getThreshold()/100.0)
			return new PluginAnalysis(BACKOUT_REGEXP);

		return PluginAnalysis.OK;
	}
}
