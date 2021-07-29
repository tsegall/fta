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

import java.util.Locale;
import java.util.Map;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.FactsTypeBased;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.Shapes;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;

/**
 * Plugin to detect Vehicle Identification Numbers (VINs).
 */
public class LogicalTypeVIN extends LogicalTypeInfinite {
	public static final String SEMANTIC_TYPE = "VIN";
	public static final String REGEXP = "[A-HJ-NPR-Z0-9]{17}";

	private static final boolean[] validCharacter = new boolean[] {
			true, true, true, true, true, true, true, true, false, true, true, true, true,
			true, false, true, false, true, true, true, true, true, true, true, true, true
	};
	private static String[] wmiNA = new String[] { "1VW", "1ZV", "19U", "2G2", "2HG", "2HK" };
	private static String yearCode = "ABCDEFGHJKLMNPRSTVWXY123456789";
	private static int[] letterValue = new int[] {
			1, 2, 3, 4, 5, 6, 7, 8, 0, 1, 2, 3, 4, 5, 0, 7, 0, 9, 2, 3, 4, 5, 6, 7, 8, 9
	};
	private static int[] weights = new int[] { 8, 7, 6, 5, 4, 3, 2, 10, 0, 9, 8, 7, 6, 5, 4, 3, 2 };

	public LogicalTypeVIN(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final Locale locale) throws FTAPluginException {
		super.initialize(locale);

		threshold = 99;

		return true;
	}

	@Override
	public String nextRandom() {
		final char[] ret = new char[17];

		// World manufacturer identifier (1-3)
		String wmi = wmiNA[random.nextInt(wmiNA.length)];
		ret[0] = wmi.charAt(0);
		ret[1] = wmi.charAt(1);
		ret[2] = wmi.charAt(2);

		// Vehicle descriptor section (4-8)
		for (int i = 3; i < 8; i++)
			ret[i] = (char)('0' + random.nextInt(10));

		// Check Digit (9)
		ret[8] = '0';

		// Model Year (10)
		ret[9] = yearCode.charAt(random.nextInt(yearCode.length()));

		// Plant Code (11)
		ret[10] = (char)('0' + random.nextInt(10));

		// Sequential number (12-17)
		for (int i = 11; i < 17; i++)
			ret[i] = (char)('0' + random.nextInt(10));

		// Patch the check digit
		ret[8] = generateCheckDigit(ret);

		return String.valueOf(ret);
	}

	private char generateCheckDigit(char[] VIN) {
		int sum = 0;
		for (int i = 0; i < 17; i++) {
			int weight = Character.isDigit(VIN[i]) ? VIN[i] - '0' : letterValue[VIN[i] - 'A'];
			sum += weight * weights[i];
		}

		int remainder = sum % 11;

		return remainder < 10 ? (char)('0' + remainder) : 'X';
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public boolean isRegExpComplete() {
		return false;
	}

	@Override
	public boolean isValid(final String input) {
		String cleaned = input.trim().replaceAll("-", "").toUpperCase(Locale.ROOT);
		final int len = cleaned.length();
		if (len != 17)
			return false;

		char[] asArray = cleaned.toCharArray();

		boolean northAmerican = false;
		for (int i = 0; i < len; i++) {
			final char ch = asArray[i];
			if (i == 0 && ch >= '0' && ch <= '5')
				northAmerican = true;

			if (!(ch >= '0' && ch <= '9') && !(ch >= 'A' && ch <= 'Z' && validCharacter[ch - 'A']))
				return false;
		}

		if (northAmerican)
			return generateCheckDigit(asArray) == asArray[8];

		return true;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return isValid(trimmed);
	}

	@Override
	public String isValidSet(final String dataStreamName, final long matchCount, final long realSamples, String currentRegExp,
			final FactsTypeBased facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final Shapes shapes, AnalysisConfig analysisConfig) {
		return (double) matchCount / realSamples >= getThreshold() / 100.0 ? null : ".+";
	}
}