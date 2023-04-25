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

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

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
 * Plugin to detect valid UK Postal codes.
 * Note: Neither the validator nor the random are true reflections of UK Postal Codes.
 * Note: we used an Infinite :-) Semantic Type since the domain is so large.
 */
public class PostalCodeUK extends LogicalTypeInfinite {
	/** The Regular Expression for this Semantic type. */
	public static final String REGEXP = "([A-Za-z][A-Ha-hK-Yk-y]?[0-9][A-Za-z0-9]? ?[0-9][A-Za-z]{2}|[Gg][Ii][Rr] ?0[Aa]{2})";

	private static String[] validPostalCodes = { "XX9X 9XX", "X9X 9XX", "X9 9XX", "X99 9XX", "XX9 9XX", "XX99 9XX" };
	private static Set<String> validShapes = new HashSet<>();
	private final Pattern validator = Pattern.compile(REGEXP);

	static {
		Collections.addAll(validShapes, validPostalCodes);
	}

	/**
	 * Construct a UK Postal code plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public PostalCodeUK(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		final String c = compressed.toString();
		if (!c.startsWith("\\p{IsAlphabetic}"))
			return false;

		return
				c.equals("\\p{IsAlphabetic}{2}\\d{1}\\p{IsAlphabetic}{1} \\d{1}\\p{IsAlphabetic}{2}") ||
				c.equals("\\p{IsAlphabetic}{1}\\d{1}\\p{IsAlphabetic}{1} \\d{1}\\p{IsAlphabetic}{2}") ||
				c.equals("\\p{IsAlphabetic}{1}\\d{1} \\d{1}\\p{IsAlphabetic}{2}") ||
				c.equals("\\p{IsAlphabetic}{1}\\d{2} \\d{1}\\p{IsAlphabetic}{2}") ||
				c.equals("\\p{IsAlphabetic}{2}\\d{1} \\d{1}\\p{IsAlphabetic}{2}") ||
				c.equals("\\p{IsAlphabetic}{2}\\d{2} \\d{1}\\p{IsAlphabetic}{2}");
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		return true;
	}

	@Override
	public String nextRandom() {
		final String format = validPostalCodes[random.nextInt(validPostalCodes.length)];
		final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		final String restrictedAlphabet = "ABCDEFGHKLMNOPQRSTUVWXY";
		final StringBuilder result = new StringBuilder("");
		for (int i = 0; i < format.length(); i++) {
			switch (format.charAt(i)) {
			case ' ':
				result.append(' ');
				break;
			case 'X':
				if (i == 1)
					result.append(restrictedAlphabet.charAt(random.nextInt(23)));
				else
					result.append(alphabet.charAt(random.nextInt(26)));
				break;
			case '9':
				result.append((char)('0' + random.nextInt(10)));
				break;
			}
		}

		return result.toString();
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
	public boolean isValid(final String input, final boolean detectMode, final long count) {
	    return validator.matcher(input).matches();
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		final String upperDataStreamName = context.getStreamName().toUpperCase(Locale.ROOT);
		if ((cardinality.size() < 5 && !upperDataStreamName.contains("POST")) || (double)matchCount/realSamples < getThreshold()/100.0)
			return new PluginAnalysis(REGEXP);

		return PluginAnalysis.OK;
	}
}
