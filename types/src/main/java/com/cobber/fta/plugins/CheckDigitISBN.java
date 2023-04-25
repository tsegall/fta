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

import org.apache.commons.validator.routines.checkdigit.EAN13CheckDigit;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect ISBN-13 identifiers (with hyphens).
 */
public class CheckDigitISBN extends CheckDigitLT {
	/** The Regular Expression for this Semantic type. */
	public static final String REGEXP = "[-\\d]{17}";

	private static final int DIGIT_LENGTH = 13;

	private final static String[] SAMPLES = {
			"978-1-83790-353-5", "978-1-921048-91-3", "978-0-315-09943-2", "978-0-535-98831-8", "978-0-451-05990-1", "978-1-58120-222-9",
			"978-1-64072-519-5", "978-1-218-87051-7", "978-1-05-073878-5", "978-0-06-877239-2", "978-0-7528-6694-9", "978-1-247-67895-5",
			"978-0-348-29489-7", "978-0-11-949459-4", "978-1-80795-326-3", "978-0-355-05307-4", "978-1-249-01060-9", "978-1-74928-734-1",
			"978-1-351-75568-9", "978-1-68506-159-3", "978-1-260-50436-1", "978-0-680-48813-8", "978-0-948544-89-7", "978-0-7703-9196-6",
			"978-0-9993630-2-7", "978-1-203-00498-9", "978-1-72090-440-3", "978-0-10-695287-8", "978-1-56707-945-6", "978-1-376-32192-0",
			"978-1-167-91312-9", "978-1-148-53486-2", "978-0-491-96453-1", "978-1-275-45223-7", "978-1-901397-91-8", "978-0-222-26407-7",
			"978-1-4954-9629-5", "978-0-900574-63-4", "978-1-59820-250-2", "978-1-249-88828-4", "978-0-928642-59-9", "978-0-9982736-2-4",
			"978-1-82069-542-3", "978-1-916101-14-2", "978-1-61595-537-4", "978-0-326-45273-8", "978-1-65756-876-1", "978-1-370-40160-4",
			"978-1-943295-43-2", "978-1-223-27773-8", "978-0-8443-7911-1", "978-0-567-26806-8", "978-0-632-67115-1", "978-0-11-767044-0"
	};

	/**
	 * Construct a plugin to detect ISBNs (International Standard Book Numbers) with hyphens based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public CheckDigitISBN(final PluginDefinition plugin) {
		super(plugin, DIGIT_LENGTH);
		validator = new EAN13CheckDigit();
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		final int digits = charCounts['0'] + charCounts['1'] + charCounts['2'] + charCounts['3'] + charCounts['4'] +
				charCounts['5'] + charCounts['6'] + charCounts['7'] + charCounts['8'] + charCounts['9'];

		if (digits != DIGIT_LENGTH)
			return false;

		// If we have the right number of digits and there are 4 -'s then it could be valid
		// ISBN: <Prefix element>-<Registration group element>-<Registrant element>-<Publication element>-<Check digit>
		if (charCounts['-'] != 4)
			return false;

		return isValid(trimmed, true, -1);
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		if (input.length() == DIGIT_LENGTH)
			return validator.isValid(input);

		final StringBuilder b = new StringBuilder(input);
		for (int i = input.length() - 1; i >= 0; i--) {
			final char ch = b.charAt(i);
			switch (ch) {
			case '-':
				b.deleteCharAt(i);
				break;
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				break;
			default:
				return false;
			}
		}

		if (b.length() != DIGIT_LENGTH)
			return false;

		return validator.isValid(b.toString());
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		if (cardinality.size() < 20 || (double)matchCount/realSamples < getThreshold()/100.0)
			return new PluginAnalysis(BACKOUT_REGEXP);

		return PluginAnalysis.OK;
	}

	@Override
	public String nextRandom() {
		return SAMPLES[random.nextInt(SAMPLES.length)];
	}
}
