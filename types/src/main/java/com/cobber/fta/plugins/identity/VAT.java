/*
 * Copyright 2017-2025 Tim Segall
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

import java.util.Locale;

import org.apache.commons.validator.routines.checkdigit.CheckDigit;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.InternalErrorException;
import com.cobber.fta.core.Utils;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect Value-added Tax (VAT) identification numbers.
 */
public class VAT extends LogicalTypeInfinite {
	private static final String SEMANTIC_TYPE = "IDENTITY.VAT_";

	private static final String BACKOUT_REGEXP = ".*";

	private String country;
	private long prefixPresent;
	private String prefix = "";
	protected CheckDigit validator;

	/**
	 * Construct a plugin to detect Value-added Tax (VAT) identification numbers.
	 * @param plugin The definition of this plugin.
	 */
	public VAT(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		final int len = trimmed.length() - charCounts[' '];
		return len >= 9 && len <= 14 && isValid(trimmed, true, -1);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		country = locale.getCountry().toUpperCase(Locale.ROOT);

		prefix = country;

		if ("IT".equals(country))
			validator = new LuhnCheckDigit();

		return true;
	}

	@Override
	public String nextRandom() {
		final String ret = prefixPresent == 0 ? "" : prefix;

		if ("AT".equals(country)) {
			final String noCheckDigit = Utils.getRandomDigits(getRandom(), 7);
			return ret + "U" + noCheckDigit + getCheckDigitAT(noCheckDigit);
		}

		if ("ES".equals(country)) {
			final String tester = "0123456789KLMNOPQRSWXYZ";
			final String noCheckDigit = tester.charAt(getRandom().nextInt(tester.length())) + Utils.getRandomDigits(getRandom(), 7);
			return ret + noCheckDigit + getCheckDigitES(noCheckDigit);
		}

		if ("FR".equals(country)) {
			final String noCheckDigit = Utils.getRandomDigits(getRandom(), 9);
			return ret + getCheckDigitFR(noCheckDigit) + noCheckDigit;
		}

		if ("GB".equals(country) || "UK".equals(country))
			return ret + Utils.getRandomDigits(getRandom(), 9);

		if ("IT".equals(country)) {
			final String noCheckDigit = Utils.getRandomDigits(getRandom(), 10);
			try {
				return ret + noCheckDigit + validator.calculate(noCheckDigit);
			} catch (CheckDigitException e) {
				throw new InternalErrorException("Should not have happened: " + getRandom(), e);
			}
		}

		if ("NL".equals(country)) {
			final String noCheckDigit = Utils.getRandomDigits(getRandom(), 8);
			return ret + noCheckDigit + getCheckDigitNL(noCheckDigit) + "B00";
		}

		if ("PL".equals(country)) {
			final String noCheckDigit = Utils.getRandomDigits(getRandom(), 9);
			return ret + noCheckDigit + getCheckDigitPL(noCheckDigit);
		}

		return null;
	}

	@Override
	public String getSemanticType() {
		return SEMANTIC_TYPE + country;
	}

	@Override
	public String getRegExp() {
		final String ret = prefixPresent != 0 ? "(" + prefix + " ?)?" : "";

		if ("AT".equals(country))
			return ret + "U\\d{8}";

		if ("ES".equals(country))
			return ret + "[A-Za-z0-9]\\d{7}[A-Za-z0-9]";

		if ("FR".equals(country))
			return ret + "\\d{11}";

		if ("GB".equals(country) || "UK".equals(country))
			return ret + "[ \\d]{9}";

		if ("IT".equals(country))
			return ret + "\\d{11}";

		if ("NL".equals(country))
			return ret + "\\d{9}B\\d{2}";

		if ("PL".equals(country))
			return ret + "\\d{10}";

		return null;
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
		int len = input.length();

		if (len < 8)
			return false;

		String toCheck = input;
		if (input.toUpperCase(Locale.ROOT).startsWith(prefix)) {
			toCheck = input.substring(2).trim();
			prefixPresent++;
			len = toCheck.length();
		}

		if ("AT".equals(country))
			return toCheck.charAt(0) == 'U' && isValidAT(toCheck.substring(1));

		if ("ES".equals(country))
			return isValidES(toCheck);

		if ("GB".equals(country) || "UK".equals(country))
			return isValidGB(toCheck);

		if ("FR".equals(country))
			return isValidFR(toCheck);

		if ("IT".equals(country))
			return len == 11 && validator.isValid(input);

		if ("NL".equals(country))
			return isValidNL(toCheck);

		if ("PL".equals(country))
			return isValidPL(toCheck);

		return false;
	}

	// Validate a Austrian VAT number
	private boolean isValidAT(final String input) {
		if (input.length() != 8 || !Utils.isNumeric(input))
			return false;

		final String checkDigit = getCheckDigitAT(input.substring(0, 7));

		// Check digit should be the same as the last digit
		return checkDigit.charAt(0) == input.charAt(7);
	}

	private String getCheckDigitAT(final String input) {
		final int[] multipliers = { 1, 2, 1, 2, 1, 2, 1 };
		long total = 0;
		long temp;

		// Extract the next digit and multiply by the appropriate multiplier.
		for (int i = 0; i < 7; i++) {
			temp = (input.charAt(i) - '0') * multipliers[i];
			if (temp > 9)
				total += temp/10 + temp%10;
			else
				total += temp;
		}

		// Compute check digit.
		total = 10 - (total + 4) % 10;
		if (total == 10)
			total = 0;

		return String.valueOf((char)('0' + total));
	}

	// Validate a Spanish VAT number
	private boolean isValidES(final String input) {
		if (input.length() != 9)
			return false;


		final String checkDigit = getCheckDigitES(input);

		if (checkDigit == null)
			return false;

		// Check digit should be the same as the last digit
		return checkDigit.charAt(0) == input.charAt(8);
	}

	private String getCheckDigitES(final String input) {
		// There are four separate cases
		final String nationalJuridical = "ABCDEFGHJUV";
		final String otherJuridical = "ABCDEFGHNOPQRSW";
		final String personalOne = "0123456789YZ";
		final String personalTwo = "KLMX";

		final int[] multipliers = { 2, 1, 2, 1, 2, 1, 2 };
		long total = 0;
		long temp;
		final char first = input.charAt(0);

		if (nationalJuridical.indexOf(first) != -1 && Utils.isSimpleAlphaNumeric(input.charAt(8))) {
			// National juridical entities
			if (!Utils.isNumeric(input.substring(1)))
				return null;

			// Extract the next digit and multiply by the counter.
			for (int i = 0; i < 7; i++) {
				temp = (input.charAt(i+1) - '0') * multipliers[i];
				if (temp > 9)
					total += temp/10 + temp%10;
				else
					total += temp;
			}

			// Now calculate the check digit itself.
			total = 10 - total % 10;
			if (total == 10)
				total = 0;

			return String.valueOf((char)('0' + total));
		}

		if (otherJuridical.indexOf(first) != -1) {
			// Juridical entities other than national ones

			// Extract the next digit and multiply by the counter.
			for (int i = 0; i < 7; i++) {
				temp = (input.charAt(i+1) - '0') * multipliers[i];
				if (temp > 9)
					total += temp/10 + temp%10;
				else
					total += temp;
			}

			total = 10 - total % 10;

			return String.valueOf((char)('@' + total));
		}

		final String personalCheckDigit = "TRWAGMYFPDXBNJZSQVHLCKE";

		if (personalOne.indexOf(first) != -1) {
			// Personal number (NIF) (starting with numeric of Y or Z)
			String newNumber;

			if (first == 'Y')
				newNumber = "1" + input.substring(1, 8);
			else if (first == 'Z')
				newNumber = "2" + input.substring(1, 8);
			else
				newNumber = input.substring(0, 8);

			return String.valueOf(personalCheckDigit.charAt(Integer.parseInt(newNumber) % personalCheckDigit.length()));
		}

		if (personalTwo.indexOf(first) != -1) {
			// Personal number (NIF) (starting with K, L, M, or X)
			return String.valueOf(personalCheckDigit.charAt(Integer.parseInt(input.substring(1, 8)) % personalCheckDigit.length()));
		}

		return null;
	}

	// Validate a French VAT number
	private boolean isValidFR(final String input) {
		if (input.length() != 11 || !Utils.isNumeric(input))
			return false;

		// Calculate the CheckSum based on the last 9 digits
		final String checkDigit = getCheckDigitFR(input.substring(2));

		// Compare the calculated check digits with the first two digits of the input
		return checkDigit.equals(input.substring(0, 2));
	}

	private String getCheckDigitFR(final String input) {

		long total = Integer.parseInt(input);

		// Calculate check digit
		total = (12 + 3 * (total % 97)) % 97;

		return total < 10 ? "0" + total : String.valueOf(total);
	}

	// Validate a UK VAT number
	private boolean isValidGB(final String input) {
		String toCheck = input;

		// Commonly formatted as 999 9999 99 (or 999-9999-99)
		if (toCheck.length() == 11) {
			toCheck = toCheck.replace(" ", "");
			if (toCheck.length() == 11)
				toCheck = toCheck.replace("-", "");
		}

		return toCheck.length() == 9 && Utils.isNumeric(toCheck);
	}

	// Validate a Dutch VAT number - NL
	private boolean isValidNL(final String input) {
		if (input.length() != 12 || input.charAt(9) != 'B' || !Character.isDigit(input.charAt(10)) || !Character.isDigit(input.charAt(11)))
			return false;

		final char checkDigit = getCheckDigitNL(input.substring(0, 8));

		// Check digit should be the same as the last digit
		return checkDigit == input.charAt(8);
	}

	public static char getCheckDigitNL(final String input) {

		final int[] multipliers = { 9, 8, 7, 6, 5, 4, 3, 2 };

		long total = 0;

		// Calculate the total
		for (int i = 0; i < 8; i++)
			total += (input.charAt(i) - '0') * multipliers[i];

		// Check digit is mod 11
		total = total % 11;
		if (total > 9)
			total = 0;

		return (char)(total + '0');
	}

	// Validate a Polish VAT number
	private boolean isValidPL(final String input) {
		if (input.length() != 10 || !Character.isDigit(input.charAt(9)))
			return false;

		final char checkDigit = getCheckDigitPL(input.substring(0, 9));

		// Check digit should be the same as the last digit
		return checkDigit == input.charAt(9);
	}

	private static char getCheckDigitPL(final String input) {

		final int[] multipliers = { 6, 5, 7, 2, 3, 4, 5, 6, 7 };

		long total = 0;

		// Calculate the total
		for (int i = 0; i < 9; i++)
			total += (input.charAt(i) - '0') * multipliers[i];

		// Check digit is mod 11
		total = total % 11;
		if (total > 9)
			total = 0;

		return (char)(total + '0');
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		double confidence = (double)matchCount/realSamples;

		// Boost based on how much we like the header
		if (getHeaderConfidence(context.getStreamName()) >= 90)
			confidence = Math.min(confidence + Math.min((1.0 - confidence)/2, 0.30), 1.0);

		return confidence;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		if (getConfidence(matchCount, realSamples, context) < getThreshold()/100.0)
			return new PluginAnalysis(BACKOUT_REGEXP);

		return PluginAnalysis.OK;
	}
}
