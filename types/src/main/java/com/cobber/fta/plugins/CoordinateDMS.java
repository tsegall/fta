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

import static com.cobber.fta.core.Utils.isNumeric;

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

public abstract class CoordinateDMS extends LogicalTypeInfinite {
	public CoordinateDMS(final PluginDefinition plugin) {
		super(plugin);
	}

	protected abstract char[] getDirectionChars();

	protected abstract int getMaxDegrees();

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		return true;
	}

	@Override
	public String nextRandom() {
		final StringBuilder ret = new StringBuilder(10);
		final char[] directionChars = getDirectionChars();

		ret.append(random.nextInt(getMaxDegrees()))
		.append(10 + random.nextInt(50))
		.append(10 + random.nextInt(50))
		.append(random.nextInt(2) == 1 ?  directionChars[0] :  directionChars[1]);

		return ret.toString();
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public boolean isRegExpComplete() {
		return true;
	}

	private boolean validDirection(final char ch) {
		final char[] directionChars = getDirectionChars();
		return ch == directionChars[0] || ch == directionChars[1] || ch == directionChars[2] || ch == directionChars[3];
	}

	private boolean checkDMS(final int degrees, final int minutes, final int seconds) {
		return degrees <= getMaxDegrees() && minutes <= 59 && seconds <= 59;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		final int spaceIndex = input.indexOf(' ');
		if (spaceIndex != -1) {
			final String[] components = input.split(" ");
			if (components.length != 3 && components.length != 4)
				return false;

			if (components.length == 3) {
				final char ch = components[2].charAt(components[2].length() - 1);
				if (!validDirection(ch))
					return false;
				components[2] = components[2].substring(0, components[2].length() - 1);
			}
			else {
				if (components[3].length() != 1 || !validDirection(components[3].charAt(0)))
					return false;
			}
			if (!isNumeric(components[0]) || !isNumeric(components[1]) || !isNumeric(components[2]))
				return false;

			return checkDMS(Integer.parseInt(components[0]), Integer.parseInt(components[1]), Integer.parseInt(components[2]));
		}

		int len = input.length();
		if (len < 6 || len > 8)
			return false;
		if (!validDirection(input.charAt(len - 1)))
			return false;
		final String DMS = input.substring(0, len - 1);
		if (!isNumeric(DMS))
			return false;

		len--;

		return checkDMS(Integer.parseInt(DMS.substring(0, len - 4)), Integer.parseInt(DMS.substring(len - 4, len - 2)), Integer.parseInt(DMS.substring(len - 2, len)));
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		final int len = trimmed.length();
		// Longitude of the form '168 59 46 N' (or a compressed version thereof)
		if (len > 11)
			return false;

		final int spaces = charCounts[' '];
		if (spaces != 0 && spaces != 2 && spaces != 3)
			return false;

		final char[] directionChars = getDirectionChars();

		final int direction = charCounts[directionChars[0]] + charCounts[directionChars[1]] + charCounts[directionChars[2]] + charCounts[directionChars[3]];
		if (direction != 1)
			return false;

		final int digits = charCounts['0'] + charCounts['1'] + charCounts['2'] + charCounts['3'] + charCounts['4'] +
				charCounts['5'] + charCounts['6'] + charCounts['7'] + charCounts['8'] + charCounts['9'];

		return digits + spaces + 1 == len;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		return (double) matchCount / realSamples >= getThreshold() / 100.0 ? PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}
}
