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
package com.cobber.fta;

import java.util.Locale;
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
	private boolean isAlphabetic = false;
	private boolean isDigit = false;
	private boolean isPeriod = false;
	private boolean isSpace = false;
	private boolean isUnderscore = false;
	private boolean isMinus = false;
	private boolean isOther = false;
	private int maxClasses = 0;
	private boolean asSet = false;
	private int maxSetSize = -1;
	private Locale locale = null;
	private Set<String> memory = new TreeSet<>();

	public RegExpGenerator() {
		this.asSet = false;
	}

	public RegExpGenerator(boolean asSet, int maxSetSize, Locale locale) {
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
	public static boolean isSpecial(char ch) {
		return ch == '.' ||  ch == '+' || ch == '*' || ch == '^' || ch == '$' ||
				ch == '[' || ch == ']' || ch == '(' || ch == ')' || ch == '{' || ch == '}' || ch == '|';

	}

	public static String slosh(char ch) {
		return isSpecial(ch) ? "\\" + ch : String.valueOf(ch);
	}

	public static String merge(String firstRE, String secondRE) {
		if (!firstRE.contains(secondRE) && !secondRE.contains(firstRE))
			return firstRE + '|' + secondRE;

		if (secondRE.contains(firstRE)) {
			String save = firstRE;
			firstRE = secondRE;
			secondRE = save;
		}

		// Now we know that the first RE contains the second
		if (firstRE.startsWith(secondRE))
			return secondRE + '(' + firstRE.substring(secondRE.length()) + ")?";
		if (firstRE.endsWith(secondRE))
			return '(' + firstRE.substring(0, firstRE.length() - secondRE.length()) + ")?" + secondRE;

		int start = firstRE.indexOf(secondRE);
		return firstRE.substring(0, start) + '(' + secondRE + ")?" + firstRE.substring(start + secondRE.length());
	}

	/**
	 * Return an escaped String (similar to Pattern.quote but not unconditional).
	 * @param input The String to be protected.
	 * @return An escaped String/
	 */
	public static String slosh(String input) {
		int len = input.length();

		if (len == 1)
			return slosh(input.charAt(0));

		int specials = 0;
		StringBuilder ret = new StringBuilder();

		// Build up the answer and count the number of special characters
		for (int i = 0; i < input.length(); i++) {
			char ch = input.charAt(i);
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

	private static StringBuilder segment(char toRepeat, int count) {
		StringBuilder ret = new StringBuilder();
		String repeater = null;
		switch (toRepeat) {
			case HEX:
				repeater = "\\p{XDigit}";
				break;
			case DIGIT:
				repeater = "\\d";
				break;
			case LOW_ALPHABETIC:
			case HIGH_ALPHABETIC:
				repeater = "\\p{IsAlphabetic}";
				break;
		}

		ret.append(repeater);
		if (count > 1)
			ret.append('{').append(count).append('}');

		return ret;
	}

	final static char DIGIT = '9';
	final static char LOW_ALPHABETIC = 'x';
	final static char HIGH_ALPHABETIC = 'X';
	final static char HEX = 'H';

	/**
	 * Fast method to simplify a string so that we can determine if all inputs are of the same form.
	 * Smashed strings follow the following rules:
	 *  - Strings of length greater than 30 are replaced with .+
	 *  - any digit is replaced with '9'
	 *  - any alpha is replaced with 'X'
	 *  - any % is sloshed (i.e. replaced with %%)
	 *
	 * @param input The input String to be smashed.
	 * @return A 'smashed' String.
	 */
	public static String smash(final String input) {
		StringBuilder b = new StringBuilder();

		int len = input.length();
		if (len > 30)
			return ".+";
		for (int i = 0; i < len; i++) {
			char ch = input.charAt(i);
			// Note: we are using 0-9 not isDigit
			if (ch >= '0' && ch <= '9')
				b.append(DIGIT);
			else if (Character.isAlphabetic(ch))
				b.append((ch >= 'a' && ch <= 'f' || ch >= 'A' && ch <= 'F') ? LOW_ALPHABETIC : HIGH_ALPHABETIC);
			else if (ch == '%') {
				b.append("%%");
			}
			else
				b.append(ch);
		}

		return b.toString();
	}

	/**
	 * Generate a Regular Expression from the 'smashed' input.
	 * @param smashed The smashed input
	 * @return A Regular Expression that captures the 'smashed' input.
	 */
	public static String smashedAsRegExp(String smashed) {
		if (".+".equals(smashed))
			return smashed;

		StringBuilder ret = new StringBuilder();
		char last = '¶';
		char ch = '¶';
		char count = 0;

		for (int i = 0; i < smashed.length(); i++) {
			ch = smashed.charAt(i);
			if (ch == LOW_ALPHABETIC)
				ch = HIGH_ALPHABETIC;

			switch (ch) {
			case DIGIT:
			case HEX:
			case LOW_ALPHABETIC:
			case HIGH_ALPHABETIC:
				if (count != 0 && ch != last) {
					ret.append(segment(last, count));
					count = 0;
				}
				count++;
				break;

			case '%':
				if (count != 0 && ch != last) {
					ret.append(segment(last, count));
					count = 0;
				}
				ch = smashed.charAt(++i);
				if (ch == '%')
					ret.append('%');
				else if (ch == 'f')
					ret.append("[+-]?\\d+\\.\\d+");
				break;

			default:
				if (count != 0 && ch != last) {
					ret.append(segment(last, count));
					count = 0;
				}
				ret.append(slosh(ch));
				break;
			}
			last = ch;
		}

		if (count != 0)
			ret.append(segment(ch, count));

		return ret.toString();
	}

	/**
	 * This method should be called for each string in the set.
	 * @param input The String to be used as part of the set.
	 */
	public void train(String input) {
		if (asSet)
			memory.add(input.toUpperCase(locale));

		final int len = input.length();
		int classes = 0;

		for (int i = 0; i < len; i++) {
			char ch = input.charAt(i);
			if (Character.isAlphabetic(ch))
				isAlphabetic = true;
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

		if (isAlphabetic)
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
	 * Given the set of Strings trained() return a Regular Expression which will accept any of the training set.
	 * @return A regular expression matching the training set.
	 */
	public String getResult() {
		StringBuilder result = new StringBuilder();

		if (asSet && memory.size() <= maxSetSize) {
			if (isAlphabetic)
				result.append("(?i)");
			if (memory.size() != 1)
				result.append("(");
			for (String element : memory) {
				result.append(RegExpGenerator.slosh(element)).append("|");
			}
			result.deleteCharAt(result.length() - 1);
			if (memory.size() != 1)
				result.append(")");
			return result.toString();
		}

		if (isOther)
			result.append(".");
		else {
			if (maxClasses > 1)
				result.append("[");
			if (isAlphabetic)
				result.append("\\p{IsAlphabetic}");
			if (isDigit)
				result.append("\\p{IsDigit}");
			if (isPeriod)
				result.append("\\.");
			if (isUnderscore)
				result.append("_");
			if (isMinus)
				result.append("\\-");
			if (isSpace)
				result.append(" ");
			if (maxClasses > 1)
				result.append("]");
		}

		return result.append(RegExpSplitter.qualify(shortest, longest)).toString();
	}
}
