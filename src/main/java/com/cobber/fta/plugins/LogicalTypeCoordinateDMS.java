/*
 * Copyright 2017-2021 Tim Segall
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

import java.util.Locale;
import java.util.Map;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.FactsTypeBased;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;

public abstract class LogicalTypeCoordinateDMS extends LogicalTypeInfinite {
	public LogicalTypeCoordinateDMS(final PluginDefinition plugin) {
		super(plugin);
	}

	abstract char[] getDirectionChars();

	abstract int getMaxDegrees();

	@Override
	public boolean initialize(final Locale locale) throws FTAPluginException {
		super.initialize(locale);

		threshold = 99;

		return true;
	}

	@Override
	public String nextRandom() {
		final StringBuilder ret = new StringBuilder(10);
		final char[] directionChars = getDirectionChars();

		ret.append(random.nextInt(getMaxDegrees()));
		ret.append(10 + random.nextInt(50));
		ret.append(10 + random.nextInt(50));
		ret.append(random.nextInt(2) == 1 ?  directionChars[0] :  directionChars[1]);

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

	private boolean validDirection(char ch) {
		char[] directionChars = getDirectionChars();
		return ch == directionChars[0] || ch == directionChars[1] || ch == directionChars[2] || ch == directionChars[3];
	}

	private boolean CheckDMS(final int degrees, final int minutes, final int seconds) {
		return degrees <= getMaxDegrees() && minutes <= 59 && seconds <= 59;
	}

	@Override
	public boolean isValid(final String input) {
		int len = input.length();
		final int spaceIndex = input.indexOf(' ');
		if (spaceIndex != -1) {
			String[] components = input.split(" ");
			if (components.length != 3 && components.length != 4)
				return false;

			if (components.length == 3) {
				char ch = components[2].charAt(components[2].length() - 1);
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

			return CheckDMS(Integer.valueOf(components[0]), Integer.valueOf(components[1]), Integer.valueOf(components[2]));
		}

		if (len < 6 || len > 8)
			return false;
		if (!validDirection(input.charAt(len - 1)))
			return false;
		String DMS = input.substring(0, len - 1);
		if (!isNumeric(DMS))
			return false;

		len--;

		return CheckDMS(Integer.valueOf(DMS.substring(0, len - 4)), Integer.valueOf(DMS.substring(len - 4, len - 2)), Integer.valueOf(DMS.substring(len - 2, len)));
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

		char[] directionChars = getDirectionChars();

		final int direction = charCounts[directionChars[0]] + charCounts[directionChars[1]] + charCounts[directionChars[2]] + charCounts[directionChars[3]];
		if (direction != 1)
			return false;

		int digits = charCounts['0'] + charCounts['1'] + charCounts['2'] + charCounts['3'] + charCounts['4'] +
				charCounts['5'] + charCounts['6'] + charCounts['7'] + charCounts['8'] + charCounts['9'];

		return digits + spaces + 1 == len;
	}

	@Override
	public String isValidSet(final AnalyzerContext context, final long matchCount, final long realSamples, String currentRegExp,
			final FactsTypeBased facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final TokenStreams tokenStreams, AnalysisConfig analysisConfig) {
		return (double) matchCount / realSamples >= getThreshold() / 100.0 ? null : ".+";
	}

}
