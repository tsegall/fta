/*
 * Copyright 2017-2023 Tim Segall
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
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect GUIDs (with hyphens).
 */
public class GUID extends LogicalTypeInfinite {
	private static final String REGEXP_32 = "[a-fA-F0-9]{32}";
	private static final String REGEXP_36 = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";
	private static final char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	private Character left;
	private Character right;
	private int len38;
	private int len36;
	private int len32;

	/**
	 * Construct a plugin to detect GUIDs (Globally Unique Identifiers) based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public GUID(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		return true;
	}

	@Override
	public String nextRandom() {
		final StringBuilder ret = new StringBuilder(36);

		for (int i = 0; i < 32; i++) {
			if (i == 8 || i == 12 || i == 16 || i == 20)
				ret.append('-');
			ret.append(HEX[random.nextInt(16)]);
		}

		return ret.toString();
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public String getRegExp() {
		if (len38 != 0)
			return RegExpGenerator.slosh(left) + REGEXP_36 + RegExpGenerator.slosh(right);
		if (len32 != 0)
			return REGEXP_32;

		return REGEXP_36;
	}

	@Override
	public boolean isRegExpComplete() {
		return true;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		int len = input.length();
		String check = input;
		if (len == 38) {
			if (left == null) {
				if (Character.isLetterOrDigit(0) && !Character.isLetterOrDigit(37))
					return false;
				left = input.charAt(0);
				right = input.charAt(37);
			}
			if (left != input.charAt(0) || right != input.charAt(37))
				return false;
			check = input.substring(1, 37);
			len = 36;
			len38++;
		}
		else if (len == 36) {
			len36++;
			if (len32 != 0)
				return false;
			if (check.charAt(8) != '-' || check.charAt(13) != '-' || check.charAt(18) != '-' || check.charAt(23) != '-')
				return false;
		}
		else if (len == 32) {
			len32++;
			if (len36 != 0)
				return false;
		}
		else
			return false;

		for (int i = 0; i < len; i++) {
			if (len == 36 && (i == 8 || i == 13 || i == 18 || i == 23))
				continue;
			final char ch = check.charAt(i);
			if ((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F'))
				continue;
			return false;
		}

		return true;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return trimmed.chars().filter(Character::isLetterOrDigit).count() == 32;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		int typesSeen = len32 != 0 ? 1 : 0;
		if (len36 != 0)
			typesSeen++;
		if (len38 != 0)
			typesSeen++;
		if (typesSeen != 1)
			return PluginAnalysis.SIMPLE_NOT_OK;

		// Insist on a plausible header if it is just 32 hex digits
		if (len32 != 0 && getHeaderConfidence(context.getStreamName()) <= 0)
			return PluginAnalysis.SIMPLE_NOT_OK;

		return (double) matchCount / realSamples >= getThreshold() / 100.0 ? PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}
}
