package com.cobber.fta;

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
	int shortest = Integer.MAX_VALUE;
	int longest = Integer.MIN_VALUE;
	boolean isAlphabetic = false;
	boolean isDigit = false;
	boolean isPeriod = false;
	boolean isSpace = false;
	boolean isOther = false;
	int maxClasses = 0;

	/**
	 * This method should be called for each string in the set.
	 * @param input The String to be used as part of the set.
	 */
	public void train(String input) {
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
	String getResult() {
		String ret = "";

		if (isOther)
			ret = ".";
		else {
			if (maxClasses > 1)
				ret += "[";
			if (isAlphabetic)
				ret += "\\p{IsAlphabetic}";
			if (isDigit)
				ret += "\\p{IsDigit}";
			if (isPeriod)
				ret += "\\.";
			if (isSpace)
				ret += " ";
			if (maxClasses > 1)
				ret += "]";
		}

		return ret + Utils.regExpLength(shortest, longest);
	}
}
