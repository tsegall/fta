package com.cobber.fta;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Analyze a set of strings and return a suitable Regular Expression.
 * Not likely to be an optimal Regular Expression!!
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
	private boolean isOther = false;
	private int maxClasses = 0;
	private boolean asSet = false;
	private Locale locale = null;
	private Set<String> memory = new HashSet<>();

	public RegExpGenerator() {
		this.asSet = false;
	}

	public RegExpGenerator(boolean asSet, Locale locale) {
		this.asSet = true;
		this.locale = locale;
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

		if (asSet && memory.size() < 10) {
			result.append("(?i)");
			if (memory.size() != 1)
				result.append("(");
			for (String element : memory) {
				result.append(element).append("|");
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
			if (isSpace)
				result.append(" ");
			if (maxClasses > 1)
				result.append("]");
		}

		return result.append(Utils.regExpLength(shortest, longest)).toString();
	}
}
