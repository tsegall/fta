/*
 * Copyright 2017-2022 Tim Segall
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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Analyze a set of strings and return a suitable Regular Expression.
 * Unlikely to be an optimal Regular Expression!!
 *
 * <p>
 * Typical usage is:
 * </p>
 *
 * <pre>
 * {@code
 * 		RegExpGenerator generator = new RegExpGenerator();
 *
 * 		generator.train("janv.");
 * 		generator.train("oct");
 * 		generator.train("dec.");
 * 		...
 *
 * 		String result = generator.getResult();
 * }
 * </pre>
 */
public class RegExpGenerator {
	private int shortest = Integer.MAX_VALUE;
	private int longest = Integer.MIN_VALUE;
	private boolean isLetter;
	private boolean isDigit;
	private boolean isPeriod;
	private boolean isSpace;
	private boolean isUnderscore;
	private boolean isMinus;
	private boolean isOther;
	private int maxClasses;
	private int maxSetSize = -1;
	private Locale locale;
	private final boolean asSet;
	private final Set<String> memory = new TreeSet<>();

	public RegExpGenerator() {
		this.asSet = false;
	}

	public RegExpGenerator(final int maxSetSize, final Locale locale) {
		this.asSet = true;
		this.maxSetSize = maxSetSize;
		this.locale = locale;
	}

	/**
	 * Is the supplied character reserved a special meaning in Regular Expressions?
	 * Note: We do not declare '-' as a special character, so should not be used in a Character Class
	 * @param ch The character to test.
	 * @return True if the character is reserved.
	 */
	public static boolean isSpecial(final char ch) {
		return ch == '.' ||  ch == '+' || ch == '*' || ch == '^' || ch == '$' ||
				ch == '[' || ch == ']' || ch == '(' || ch == ')' || ch == '{' || ch == '}' || ch == '|';

	}

	/*
	 * Return a sloshed single character - protects any characters that is special in a Regular Expression.
	 * @return A String representation of the input character protected.
	 */
	public static String slosh(final char ch) {
		return isSpecial(ch) ? "\\" + ch : String.valueOf(ch);
	}

	/*
	 * Given two Regular Expressions return a single Regular Expression that captures the sum of the two supplied expressions.
	 * @return The merged expression.
	 */
	public static String merge(String firstRE, String secondRE) {
		if (!firstRE.contains(secondRE) && !secondRE.contains(firstRE))
			return firstRE.compareTo(secondRE) < 0 ? firstRE + '|' + secondRE : secondRE + '|' + firstRE;

		// Switch it so that the firstRE contains the second
		if (secondRE.contains(firstRE)) {
			final String save = firstRE;
			firstRE = secondRE;
			secondRE = save;
		}

		// Case 1: first starts with second
		if (firstRE.startsWith(secondRE))
			return secondRE + '(' + firstRE.substring(secondRE.length()) + ")?";

		// Case 2: first ends with second
		if (firstRE.endsWith(secondRE))
			return '(' + firstRE.substring(0, firstRE.length() - secondRE.length()) + ")?" + secondRE;

		// Case 3: first is embedded in second
		final int start = firstRE.indexOf(secondRE);
		return firstRE.substring(0, start) + '(' + secondRE + ")?" + firstRE.substring(start + secondRE.length());
	}

	/**
	 * Return an escaped String (similar to Pattern.quote but not unconditional).
	 * @param input The String to be protected.
	 * @return An escaped String.
	 */
	public static String slosh(final String input) {
		final int len = input.length();

		if (len == 1)
			return slosh(input.charAt(0));

		int specials = 0;
		final StringBuilder ret = new StringBuilder();

		// Build up the answer and count the number of special characters
		for (int i = 0; i < len; i++) {
			final char ch = input.charAt(i);
			if (isSpecial(ch)) {
				specials++;
				if (specials > 1)
					break;
				ret.append(slosh(ch));
			}
			else
				ret.append(ch);
		}

		// No special characters so just return the input string
		if (specials == 0)
			return input;

		// If there is only one special character then use a simple version else give up and use \Q and \E via Pattern.quote
		return specials == 1 ? ret.toString() : Pattern.quote(input);
	}

	public boolean isOther() {
		return isOther;
	}

	public boolean isDigit() {
		return isDigit;
	}

	/**
	 * This method should be called for each string in the set.
	 * @param input The String to be used as part of the set.
	 */
	public void train(final String input) {
		if (asSet)
			memory.add(input.toUpperCase(locale));

		final int len = input.length();
		int classes = 0;

		for (int i = 0; i < len; i++) {
			final char ch = input.charAt(i);
			if (Character.isLetter(ch))
				isLetter = true;
			else if (Character.isDigit(ch))
				isDigit = true;
			else if (ch == '.')
				isPeriod = true;
			else if (ch == ' ')
				isSpace = true;
			else if (ch == '_')
				isUnderscore = true;
			else if (ch == '-')
				isMinus = true;
			else
				isOther = true;
		}

		if (isLetter)
			classes++;
		if (isDigit)
			classes++;
		if (isPeriod)
			classes++;
		if (isSpace)
			classes++;
		if (isUnderscore)
			classes++;
		if (isMinus)
			classes++;
		if (classes > maxClasses)
			maxClasses = classes;

		if (len < shortest)
			shortest = len;
		if (len > longest)
			longest = len;
	}

	/**
	 * Given the set of Strings trained (See @link #train(String)) return a Regular Expression which will accept any of the training set.
	 * @return A regular expression matching the training set.
	 */
	public String getResult() {
		final StringBuilder result = new StringBuilder();

		if (asSet) {
			final boolean constantLength = shortest == longest;

			// Generate a Character class if possible - we would rather see [A-G] than A|B|C|D|E|F|G
			if (memory.size() >= 3 && shortest == 1 && constantLength) {
				char first = 0;
				char last = 0;
				char current;
				boolean collapsible = true;
				for (final String element : memory) {
					current = element.charAt(0);
					if (first == 0)
						first = current;
					else
						if (current != last + 1) {
							collapsible = false;
							break;
					}
					last = current;
				}
				if (collapsible) {
					result.append('[').append(first).append('-').append(last).append(']');
					return result.toString();
				}
			}

			// Hoping to output a nice enum like (?i)(SMALL|MEDIUM|LARGE|HUGE), dodge long constant length strings.
			if (memory.size() <= maxSetSize && !(constantLength && !isSpace && !isOther && !isUnderscore && shortest >= 12)) {
				if (isLetter)
					result.append("(?i)");
				if (memory.size() != 1)
					result.append('(');
				for (final String element : memory) {
					result.append(RegExpGenerator.slosh(element)).append('|');
				}
				result.deleteCharAt(result.length() - 1);
				if (memory.size() != 1)
					result.append(')');
				return result.toString();
			}
		}

		if (isOther)
			result.append('.');
		else {
			if (maxClasses > 1)
				result.append('[');
			if (isLetter)
				result.append("\\p{IsAlphabetic}");
			if (isDigit)
				result.append("\\p{IsDigit}");
			if (isPeriod)
				result.append("\\.");
			if (isUnderscore)
				result.append('_');
			if (isMinus)
				result.append("\\-");
			if (isSpace)
				result.append(' ');
			if (maxClasses > 1)
				result.append(']');
		}

		if (longest - shortest <= 6)
			result.append(RegExpSplitter.qualify(shortest, longest));
		else
			result.append('+');

		return result.toString();
	}

	/**
	 * Get the set of Strings (in upper case) used to train the Generator.
	 * @return The set of Strings (in upper case).
	 */
	public Set<String> getValues() {
		return memory;
	}

	private static Map<String, String> toSimplifyFull = new HashMap<>();
	private static Map<String, String> toSimplifyASCII = new HashMap<>();
	static {
		toSimplifyFull.put("\\p{XDigit}", "[0-9A-Fa-f]");
		toSimplifyFull.put("\\p{IsAlphabetic}", "<L>");
		toSimplifyFull.put("\\d", "<Nd>");

		toSimplifyASCII.put("\\p{XDigit}", "[0-9A-Fa-f]");
		toSimplifyASCII.put("\\p{IsAlphabetic}", "[A-Za-z]");
		toSimplifyASCII.put("\\d", "[0-9]");
	}

	/**
	 * Map a set of "well-known" Regexp's to Unicode Character Classes that the Automaton package supports.
	 * @param regExp A String Java Regular Expression.
	 * @param onlyASCII If true then generate simple ASCII only regexps, otherwise utilize Unicode Character Classes.
	 * @return The Automaton friendly RegExp.
	 */
	public static String toAutomatonRE(final String regExp, final boolean onlyASCII) {
		final Map<String, String> mapping = onlyASCII ? toSimplifyASCII : toSimplifyFull;
		String ret = regExp;

		for (final Map.Entry<String, String> s : mapping.entrySet())
			ret = Utils.replaceAll(ret, s.getKey(), s.getValue());

		return ret;
	}
}
