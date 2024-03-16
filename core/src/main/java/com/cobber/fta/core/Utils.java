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
package com.cobber.fta.core;

import java.io.IOException;
import java.io.StringReader;
import java.security.SecureRandom;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.InputSource;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A Utility class with a set of helper functions.
 */
public final class Utils {

	private Utils() {
		// Never called
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(final Map<K, V> map) {
		return map.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	public static String replaceFirst(final String input, final String oldString, final String newString) {
		final int index = input.indexOf(oldString);
		if (index == -1)
			return input;

		return input.substring(0, index)
				.concat(newString)
				.concat(input.substring(index + oldString.length()));
	}

	public static String replaceLast(final String input, final String oldString, final String newString) {
		final int index = input.lastIndexOf(oldString);
		if (index == -1)
			return input;

		return input.substring(0, index)
				.concat(newString)
				.concat(input.substring(index + oldString.length()));
	}

	public static String replaceAt(final String input, final int offset, final int length, final String newString) {
		return input.substring(0, offset)
				.concat(newString)
				.concat(input.substring(offset + length));
	}

	/**
	 * Return a String with the provided character repeated &lt;count&gt; times;.
	 * @param c The Character to repeat
	 * @param count The number of time to repeat the character.
	 * @return The String with &lt;count&gt; occurrences of the supplied character.
	 */
	public static String repeat(final char c, final int count) {
		if (count == 0)
			return "";

		if (count == 1)
			return String.valueOf(c);

		if (count == 2)
			return String.valueOf(new char[] {c, c});

		final StringBuilder s = new StringBuilder(count);
		for (int i = 0; i < count; i++)
			s.append(c);
		return s.toString();
	}

	/**
	 * Given a String as input with an offset and length return the integer at that position.
	 * @param input String to extract integer from
	 * @param offset Integer offset that marks the start
	 * @param minLength minimum length of integer to be extracted.
	 * @param maxLength maximum length of integer to be extracted.
	 * @return An integer value from the supplied String.
	 */
	public static int getValue(final String input, final int offset, final int minLength, final int maxLength) {
		try {
			if (minLength == maxLength || (offset + maxLength > input.length()) || !Character.isDigit(input.charAt(offset + maxLength - 1)))
				return Integer.parseInt(input.substring(offset, offset + minLength));

			return Integer.parseInt(input.substring(offset, offset + maxLength));
		}
		catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * Test if the supplied input is a string of all 0's.
	 * @param input String to test.
	 * @return True if input is a string of 0's.
	 */
	public static boolean allZeroes(final String input) {
		if (input == null || input.isEmpty())
			return false;

		for (int i = 0; i < input.length(); i++)
			if (input.charAt(i) != '0')
				return false;

		return true;
	}

	/**
	 * Test if the supplied input is all numeric.
	 * @param input String to test.
	 * @return True if the string is all Numeric.
	 */
	public static boolean isNumeric(final String input) {
		if (input == null || input.isEmpty())
			return false;

		return input.chars().allMatch(Character::isDigit);
	}

	/**
	 * Test if the supplied input is all alphas.
	 * @param input String to test.
	 * @return True if the string is all alphas.
	 */
	public static boolean isAlphas(final String input) {
		if (input == null || input.isEmpty())
			return false;

		return input.chars().allMatch(Character::isLetter);
	}

	/**
	 * Test if the supplied character is numeric [0-9].
	 * @param ch Character to test.
	 * @return True if the character is in the range [0-9].
	 */
	public static boolean isSimpleNumeric(final char ch) {
		return ch >= '0' && ch <= '9';
	}

	/**
	 * Test if the supplied character is alpha [A-Za-z].
	 * @param ch Character to test.
	 * @return True if the character is in the range [A-Za-z].
	 */
	public static boolean isSimpleAlpha(final char ch) {
		return ch >= 'A' && ch <= 'Z' || ch >= 'a' && ch <= 'z';
	}

	/**
	 * Test if the supplied character is alphaNumeric [A-Za-z0-9].
	 * @param ch Character to test.
	 * @return True if the character is in the range [A-Za-z0-9].
	 */
	public static boolean isSimpleAlphaNumeric(final char ch) {
		return ch >= '0' && ch <= '9' || ch >= 'A' && ch <= 'Z' || ch >= 'a' && ch <= 'z';
	}

	public static String getBaseName(final String fileName) {
		final int index = fileName.lastIndexOf('.');
		return index == -1 ? fileName : fileName.substring(0, index);
	}

	/**
	 * Clean a string.
	 * Replacing evil characters:
	 *  - LEFT and RIGHT SINGLE QUOTATION MARK and backticks - with a standard quote.
	 *  - LEFT and RIGHT DOUBLE QUOTATION MARK
	 *  - en-dash and em-dash with a simple hyphen.
	 * Note: We delay allocating a StringBuilder until we find out it is required.
	 * @param input String to cleanse
	 * @return The original String if no cleansing required - or a cleansed copy if necessary.
	 */
	public static String cleanse(final String input) {
		final int len = input.length();
		StringBuilder b = null;

		for (int i = 0; i < len; i++) {
			final char ch = input.charAt(i);
			// (U+2018) LEFT SINGLE QUOTATION MARK
			// (U+2019) RIGHT SINGLE QUOTATION MARK
			if (ch == '\u2018' || ch == '\u2019' || ch == '`') {
				if (b == null)
					b = new StringBuilder(input.substring(0, i));
				b.append('\'');
			}
			// (U+201C) LEFT DOUBLE QUOTATION MARK
			// (U+201D) RIGHT DOUBLE QUOTATION MARK
			else if (ch == '\u201C' || ch == '\u201D') {
				if (b == null)
					b = new StringBuilder(input.substring(0, i));
				b.append('\"');
			}
			// (U+2013) ENDASH
			// (U+2014) EMDASH
			else if (ch == '\u2013' || ch == '\u2014') {
				if (b == null)
					b = new StringBuilder(input.substring(0, i));
				b.append('-');
			}
			else if (b != null)
				b.append(ch);
		}

		return b != null ? b.toString() : input;
	}

	private static String version = Utils.class.getPackage().getImplementationVersion();

	/**
	 * Get the version of the FTA library.
	 * @return The version of the FTA library.
	 */
	public static String getVersion() {
		return version;
	}

	// Get a random digit string of length len digits, first must not be a zero
	public static String getRandomDigits(final SecureRandom random, final int len) {
		final StringBuilder b = new StringBuilder(len);
		b.append(random.nextInt(9) + 1);
		for (int i = 1; i < len; i++)
			b.append(random.nextInt(10));
		return b.toString();
	}

	public static String determineStreamFormat(final ObjectMapper mapper, final Map<String, Long> cardinality) {
		final int totalSamples = cardinality.size();

		if (totalSamples == 0)
			return null;

		final String HTML_CHECKER = "<!DOCTYPE html>|</?\\s*[a-z-][^>]*\\s*>|(\\&(?:[\\w\\d]+|#\\d+|#x[a-f\\d]+);)";
		final Pattern patternHTML = Pattern.compile(HTML_CHECKER);

		int fmtJSON = 0;
		int fmtHTML = 0;
		int fmtXML = 0;
		int fmtBase64 = 0;
		int fmtRealBase64 = 0;
		int samples = 0;

		for (final String sample : cardinality.keySet()) {
			samples++;
			if (sample.length() < 2)
				continue;
			final char first = sample.charAt(0);
			final char last = sample.charAt(sample.length() - 1);
			if (first == '{' || first == '[' && first == last && samples - fmtJSON < 5) {
				try {
					mapper.readTree(sample);
					fmtJSON++;
					continue;
				} catch (IOException e) {
					// Ignore
				}
			}
			if (first == '<' && last == '>'&& samples - fmtXML < 5) {
				try {
					DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(sample)));
					fmtXML++;
				} catch (Exception e) {
				}
				continue;
			}
			if (first == '<' && samples - (fmtHTML + fmtXML) < 5) {
				if (patternHTML.matcher(sample).groupCount() != 0)
					fmtHTML++;
				continue;
			}
			if (sample.length() % 4 == 0 && samples - fmtBase64 < 5) {
				try {
					Base64.getDecoder().decode(sample);
					fmtBase64++;
					if (last == '=')
						fmtRealBase64++;
					continue;
				}
				catch (IllegalArgumentException e) {
					// Ignore
				}
			}
		}
		if (cardinality.size() == fmtJSON)
			return "JSON";
		else if (cardinality.size() == fmtXML)
			return "XML";
		else if (cardinality.size() == fmtHTML + fmtXML)
			return "HTML";
		else if (cardinality.size() == fmtBase64 && fmtRealBase64 != 0)
			return "Base64";

		return "OTHER";
	}

	public static long parseLong(final String input, final NumberFormat longFormatter) {
		final String trimmed = input.trim();
		final ParsePosition lPos = new ParsePosition(0);
		final String lParse = trimmed.charAt(0) == '+' ? trimmed.substring(1) : trimmed;
		final Number l = longFormatter.parse(lParse, lPos);

		if (l != null && lParse.length() == lPos.getIndex())
			return l.longValue();

		final int digits = lParse.length();
		if (digits >= 2 && lParse.charAt(digits - 1) == '-')
			return -Long.parseLong(lParse.substring(0, digits - 1));

		return Long.parseLong(lParse);
	}

	// NumberFormat.getInstance(locale) returns a parser that cannot cope with a set of sins including:
	// Exponents with a wrong case 'e' (e.g. 1234.0e5) or with a '+ (e.g. or 123E+5) or with a trailing minus.
	public static Double parseDouble(final String rawInput, final NumberFormat doubleFormatter) {
		final String trimmed = rawInput.trim();
		String cleaned = trimmed.charAt(0) == '+' ? trimmed.substring(1) : trimmed;
		final ParsePosition pos = new ParsePosition(0);
		Number n = doubleFormatter.parse(cleaned, pos);
		final int upto = pos.getIndex();
		final int len = cleaned.length();
		if (n != null && upto == len)
			return n.doubleValue();

		if (len >= 2 && cleaned.charAt(len - 1) == '-')
			return -Double.parseDouble(cleaned.substring(0, len - 1));

		if (upto > len - 2)
			return Double.parseDouble(cleaned);

		// Did we trip up on the Exponent?
		final char exp = cleaned.charAt(upto);
		if (exp != 'E' && exp != 'e')
			return Double.parseDouble(cleaned);

		// Handle <Digits>E+<Digits> which is not supported
		if (upto <= len - 3 && cleaned.charAt(upto + 1) == '+' && Character.isDigit(cleaned.charAt(upto + 2))) {
			pos.setIndex(0);
			final String updatedInput = cleaned.substring(0, upto + 1) + cleaned.substring(upto + 2);
			n = doubleFormatter.parse(updatedInput, pos);
			if (pos.getIndex() == updatedInput.length())
				return n.doubleValue();
			cleaned = updatedInput;
		}

		// Handle the wrong case for the Exponentiation character which is not supported
		if (Character.isDigit(cleaned.charAt(upto + 1))) {
			final char newExp = exp == 'E' ? 'e' : 'E';
			final String updatedInput = cleaned.substring(0, upto) + newExp + cleaned.substring(upto + 1);
			pos.setIndex(0);
			n = doubleFormatter.parse(updatedInput, pos);
			if (pos.getIndex() == updatedInput.length())
				return n.doubleValue();
		}

		return Double.parseDouble(cleaned);
	}

	/**
	 * Calculate the probability that the set of size 'samples' is unique give the sampleSample size.
	 * @param sampleSpace Size of the Sample Space
	 * @param samples number of samples observed
	 * @return The probability that the sample set is unique.
	 */
	public static double uniquenessProbability(final int sampleSpace, final int samples) {
		double result = 1.0;
		double sampleSpaceD = sampleSpace;
		double numerator = sampleSpaceD;
		for (int i = sampleSpace; i > sampleSpace - samples; i--) {
			result = result * numerator / sampleSpaceD;
			numerator -= 1.0;
		}

		return 1.0 - result;
	}
}
