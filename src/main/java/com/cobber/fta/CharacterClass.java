package com.cobber.fta;

/**
 * Analyze a set of strings and return a suitable Regular Expression.  Will not ever be a optimal Regular Expression.
 *
 * <p>
 * Typical usage is:
 * </p>
 *
 * <pre>
 * {@code
 * 		CharacterClass characterClass = new CharacterClass();
 *
 * 		characterClass.train("janv.");
 * 		characterClass.train("oct");
 * 		characterClass.train("dec.");
 * 		...
 *
 * 		String result = characterClass.getResult();
 * }
 * </pre>
 */
public class CharacterClass {
	int shortest = Integer.MAX_VALUE;
	int longest = Integer.MIN_VALUE;
	boolean isAlphabetic = false;
	boolean isDigit = false;
	boolean isPeriod = false;
	boolean isOther = false;
	int maxClasses = 0;

	void train(String input) {
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
			else
				isOther = true;
		}

		if (isAlphabetic)
			classes++;
		if (isDigit)
			classes++;
		if (isPeriod)
			classes++;
		if (classes > maxClasses)
			maxClasses = classes;

		if (len < shortest)
			shortest = len;
		if (len > longest)
			longest = len;
	}

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
				ret += "\\p{isDigit}";
			if (isPeriod)
				ret += "\\.";
			if (maxClasses > 1)
				ret += "]";
		}

		return ret + Utils.regExpLength(shortest, longest);
	}
}
