/*
 * Copyright 2017-2019 Tim Segall
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

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PatternInfo;
import com.cobber.fta.PatternInfo.Type;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.RegExpGenerator;
import com.cobber.fta.StringFacts;

/**
 * Plugin to detect valid UK Postal codes.
 * Note: Neither the validator nor the random are true reflections of UK Postal Codes.
 * Note: we used an Infinite :-) Logical Type since the domain is so large.
 */
public class LogicalTypeUKPostalCode extends LogicalTypeInfinite {
	public final static String SEMANTIC_TYPE = "POSTAL_CODE.POSTAL_CODE_UK";
	public final static String REGEXP = "([A-Za-z][A-Ha-hK-Yk-y]?[0-9][A-Za-z0-9]? ?[0-9][A-Za-z]{2}|[Gg][Ii][Rr] ?0[Aa]{2})";
	private static String[] validPostalCodes = { "XX9X 9XX", "X9X 9XX", "X9 9XX", "X99 9XX", "XX9 9XX", "XX99 9XX" };
	private static Set<String> validShapes = new HashSet<String>();

	static {
		for (String s : validPostalCodes)
			validShapes.add(s);
	}

	public LogicalTypeUKPostalCode(PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(String trimmed, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		String c = compressed.toString();
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
	public boolean initialize(Locale locale) {
		super.initialize(locale);

		threshold = 95;

		return true;
	}

	@Override
	public String nextRandom() {
		String format = validPostalCodes[random.nextInt(validPostalCodes.length)];
		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String restrictedAlphabet = "ABCDEFGHKLMNOPQRSTUVWXY";
		String result = "";
		for (int i = 0; i < format.length(); i++) {
			switch (format.charAt(i)) {
			case ' ':
				result += ' ';
				break;
			case 'X':
				if (i == 1)
					result += restrictedAlphabet.charAt(random.nextInt(23));
				else
					result += alphabet.charAt(random.nextInt(26));
				break;
			case '9':
				result += (char)('0' + random.nextInt(10));
				break;
			}
		}

		return result;
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public Type getBaseType() {
		return PatternInfo.Type.STRING;
	}

	@Override
	public boolean isValid(String input) {
		String shape = RegExpGenerator.smash(input);
		return validShapes.contains(shape);
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, StringFacts stringFacts, Map<String, Long> cardinality, Map<String, Long> outliers) {
		String upperDataStreamName = dataStreamName.toUpperCase();
		boolean postalName = dataStreamName != null && upperDataStreamName.contains("POST");
		return (cardinality.size() < 5 && !postalName) || (double)matchCount/realSamples < getThreshold()/100.0 ? REGEXP : null;
	}
}
