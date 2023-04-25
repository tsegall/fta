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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.SingletonSet;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.Utils;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect an individuals Last Name.
 */
public class LastName extends PersonName {
	// This set covers the first two letters of ~95% of our last name list - assume this is a reasonable proxy for last names more generally
	private final String plausibleStarters[] = {
		"AB", "AC", "AD", "AG", "AL", "AM", "AN", "AP", "AR", "AS", "AT", "AU", "AV", "AY",
		"BA", "BE", "BI", "BL", "BO", "BR", "BU", "BY",
		"CA", "CE", "CH", "CL", "CO", "CR", "CU",
		"DA", "DE", "DI", "DO", "DR", "DU",
		"EA", "EB", "EC", "ED", "EI", "EL", "EM", "EN", "ER", "ES", "EV",
		"FA", "FE", "FI", "FL", "FO", "FR", "FU",
		"GA", "GE", "GI", "GL", "GO", "GR", "GU",
		"HA", "HE", "HI", "HO", "HU",
		"IN", "IS",
		"JA", "JE", "JO", "JU",
		"KA", "KE", "KI", "KL", "KN", "KO", "KR", "KU",
		"LA", "LE", "LI", "LO", "LU", "LY",
		"MA", "MC", "ME", "MI", "MO", "MU",
		"NA", "NE", "NI", "NO", "NU",
		"OL", "OR", "OS", "PA",
		"PE", "PH", "PI", "PL", "PO", "PR", "PU",
		"QU",
		"RA", "RE", "RH", "RI", "RO", "RU",
		"SA", "SC", "SE", "SH", "SI", "SL", "SM", "SN", "SO", "SP", "ST", "SU", "SW",
		"TA", "TE", "TH", "TI", "TO", "TR", "TU",
		"UR",
		"VA", "VE", "VI", "VO",
		"WA", "WE", "WH", "WI", "WO", "WY",
		"YA", "YO", "ZA",
		"ZE",
	};
	private final String badFirstWords[] = {
			"NORTH",  "SOUTH", "EAST", "WEST", "NEW", "OLD", "MOUNT", "LAKE"
	};
	private final String badSecondWords[] = {
			"HILL",  "HILLS", "PARK", "SPRING", "SPRINGS", "RIDGE", "PARK", "VALLEY", "LAKE", "CREEK"
	};

	private final Set<String> plausibleSet;
	private final Set<String> badFirstSet;
	private final Set<String> badSecondSet;
	private String language;
	private Set<String> suffixes;
	private long lengthSum;
	private long sampleCount;
	private long bad;

	/**
	 * Construct a Last Name plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public LastName(final PluginDefinition plugin) {
		super(plugin, "lastnames.txt");

		plausibleSet = new HashSet<>(Arrays.asList(plausibleStarters));
		badFirstSet = new HashSet<>(Arrays.asList(badFirstWords));
		badSecondSet = new HashSet<>(Arrays.asList(badSecondWords));
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		language = locale.getLanguage();
		suffixes = new SingletonSet("resource", "/reference/en_name_suffix.csv").getMembers();

		return true;
	}

	@Override
	protected boolean isPlausible(final String candidate) {
		if (candidate.length() <= 2 || !plausibleSet.contains(candidate.substring(0, 2)))
			return false;

		// Assume 50% of the remaining are good - hopefully this will not bias the determination excessively.
		// Use hashCode as opposed to random() to ensure that a given data set gives the same results from one run to another.
		return candidate.hashCode() % 10 < 5;
	}

	/*
	 * Note: The input String will be both trimmed and converted to upper Case
	 * @see com.cobber.fta.LogicalType#isValid(java.lang.String)
	 */
	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		final int ret = isValidCore(input, detectMode, count);

		if (detectMode && count != 0) {
			if (ret >= 1) {
				sampleCount += count;
				lengthSum += ret * count;
			}
			else if (ret == -1)
				bad += count;
		}

		return ret > 0;
	}

	private boolean hasValidSuffix(final String input) {
		final String trimmed = input.trim();
		for (final String suffix : suffixes)
			if (trimmed.endsWith(suffix))
				return true;
		return false;
	}

	private boolean averageLengthOK() {
		if ("es".equals(language))
			return (double)lengthSum/sampleCount < 2.5;

		return (double)lengthSum/sampleCount < 1.5;
	}

	/*
	 * Handle the following cases:
	 *  - Simple name, e.g. JEFFERSON
	 *  - Space separate last names (max 3), e.g. BARON COHEN, DE LA RENTA
	 *  - Hyphen separated name (max 2), e.g. DAY-LEWIS
	 *  - Name followed by suffix, e.g. BUSH SR (or BUSH, SR)
	 *
	 * Return value:
	 *  -1 = rubbish
	 *   0 = looks OK but does not pass validity test
	 *   >1 = Number of words in a valid name
	 */
	private int isValidCore(final String input, final boolean detectMode, final long count) {
		final String trimmedUpper = input.trim().toUpperCase(locale);
		final int len = trimmedUpper.length();

		int separatorOffset = -1;
		char separator = ' ';
		int spaces = 0;
		for (int i = 0; i < len; i++) {
			final char ch = trimmedUpper.charAt(i);
			if (Character.isLetter(ch))
				continue;
			if (separatorOffset == -1 && (ch == '-' || ch == ' ' || ch == ',')) {
				separatorOffset = i;
				separator = ch;
				continue;
			}
			if (separatorOffset != -1 && ch == '.')
				continue;
			if (ch != ' ')
				return -1;

			spaces++;
		}

		if (separatorOffset == -1)
			return super.isValid(input, detectMode, count) ? 1 : 0;

		if (separatorOffset < 2 || separatorOffset >= trimmedUpper.length() - 2)
			return -1;

		final String first = trimmedUpper.substring(0, separatorOffset);
		if (!Utils.isAlphas(first))
			return 0;

		int wordCount = 2;
		String second = trimmedUpper.substring(separatorOffset + 1).trim();
		if (separator == ' ' && spaces != 0) {
			final String[] words = second.split(" ");
			second = words[0];
			wordCount = words.length + 1;
			if (wordCount > 3)
				return -1;
		}

		if (separator == ',')
			return hasValidSuffix(second) ? 1 : -1;

		final boolean firstMatch = getMembers().contains(first);

		// Reject a set of unlikely names (typically these are Cities)
		if (separator == ' ') {
			if (badFirstSet.contains(first) || badSecondSet.contains(second))
				return 0;
			if (firstMatch && hasValidSuffix(second))
				return 1;
		}

		if (!Utils.isAlphas(second))
			return 0;

		final boolean secondMatch = getMembers().contains(second);

		// Declares success if
		//  - both components of last name are good
		//  - either component of the hyphenated name is good
		//  - second component of the space separated name is good
		if ((firstMatch && secondMatch) || ((firstMatch || secondMatch) && (separator == '-' || "es".equals(language))) || (secondMatch && separator == ' '))
			return wordCount;

		if (!detectMode)
			return wordCount;

		return separator == '-' && isPlausible(first) && isPlausible(second) ? wordCount : 0;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples,
			final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		if (realSamples > 10 && !averageLengthOK())
			return PluginAnalysis.SIMPLE_NOT_OK;
		if (getHeaderConfidence(context.getStreamName()) < 90 && realSamples > 10 && (100*bad)/realSamples > 1)
			return PluginAnalysis.SIMPLE_NOT_OK;
		return super.analyzeSet(context, matchCount, realSamples, currentRegExp, facts, cardinality, outliers, tokenStreams, analysisConfig);
	}
}
