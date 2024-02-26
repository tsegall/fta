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
package com.cobber.fta.plugins.identity;

import java.util.HashSet;
import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.Utils;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect UK National Insurance numbers.
 * Format: Two prefix letters, six digits and one suffix letter.
 */
public class NI_UK extends LogicalTypeInfinite {
	private static final String[] validPrefixesList = {
			"AA", "AB", "AE", "AH", "AK", "AL", "AM", "AP", "AR", "AS", "AT", "AW", "AX", "AY", "AZ",
			"BA", "BB", "BE", "BH", "BK", "BL", "BM", "BT",
			"CA", "CB", "CE", "CH", "CK", "CL", "CR",
			"EA", "EB", "EE", "EH", "EK", "EL", "EM", "EP", "ER", "ES", "ET", "EW", "EX", "EY", "EZ",
			"GY",
			"HA", "HB", "HE", "HH", "HK", "HL", "HM", "HP", "HR", "HS", "HT", "HW", "HX", "HY", "HZ",
			"JA", "JB", "JC", "JE", "JG", "JH", "JJ", "JK", "JL", "JM", "JN", "JP", "JR", "JS", "JT", "JW", "JX", "JY", "JZ",
			"KA", "KB", "KE", "KH", "KK", "KL", "KM", "KP", "KR", "KS", "KT", "KW", "KX", "KY", "KZ",
			"LA", "LB", "LE", "LH", "LK", "LL", "LM", "LP", "LR", "LS", "LT", "LW", "LX", "LY", "LZ",
			"MA", "MW", "MX",
			"NA", "NB", "NE", "NH", "NL", "NM", "NP", "NR", "NS", "NW", "NX", "NY", "NZ",
			"OA", "OB", "OE", "OH", "OK", "OL", "OM", "OP", "OR", "OS", "OX",
			"PA", "PB", "PC", "PE", "PG", "PH", "PJ", "PK", "PL", "PM", "PN", "PP", "PR", "PS", "PT", "PW", "PX", "PY",
			"RA", "RB", "RE", "RH", "RK", "RM", "RP", "RR", "RS", "RT", "RW", "RX", "RY", "RZ",
			"SA", "SB", "SC", "SE", "SG", "SH", "SJ", "SK", "SL", "SM", "SN", "SP", "SR", "SS", "ST", "SW", "SX", "SY", "SZ",
			"TA", "TB", "TE", "TH", "TK", "TL", "TM", "TP", "TR", "TS", "TT", "TW", "TX", "TY", "TZ",
			"WA", "WB", "WE", "WK", "WL", "WM", "WP",
			"YA", "YB", "YE", "YH", "YK", "YL", "YM", "YP", "YR", "YS", "YT", "YW", "YX", "YY", "YZ",
			"ZA", "ZB", "ZE", "ZH", "ZK", "ZL", "ZM", "ZP", "ZR", "ZS", "ZT", "ZW", "ZX", "ZY"
		};
	private static final Set<String> validPrefixes = new HashSet<>();

	static {
		for (final String prefix : validPrefixesList)
			validPrefixes.add(prefix);
	}

	private static final int LENGTH = 9;
	private static final String BACKOUT_REGEXP = ".*";
	private String regExp = BACKOUT_REGEXP;

	/**
	 * Construct a plugin to detect UK National Insurance numbers based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public NI_UK(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return validate(trimmed);
	}

	private boolean validate(final String trimmed) {
		if (trimmed.length() != LENGTH)
			return false;

		if (!validPrefixes.contains(trimmed.substring(0, 2)))
			return false;

		for (int i = 2; i < LENGTH - 1; i++) {
			final char ch = trimmed.charAt(i);
			if (!Character.isDigit(ch))
				return false;
		}

		final char lastCh = trimmed.charAt(LENGTH - 1);
		return lastCh >= 'A' && lastCh <= 'D';
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		return true;
	}

	@Override
	public String nextRandom() {
		return validPrefixesList[getRandom().nextInt(validPrefixesList.length)] + Utils.getRandomDigits(getRandom(), 6) + "ABCD".charAt(getRandom().nextInt(4));
	}

	@Override
	public String getRegExp() {
		return regExp;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		return validate(input.trim());
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		if (cardinality.size() < 20 || (double)matchCount/realSamples < getThreshold()/100.0)
			return new PluginAnalysis(BACKOUT_REGEXP);

		regExp = tokenStreams.getRegExp(false);

		return PluginAnalysis.OK;
	}
}
