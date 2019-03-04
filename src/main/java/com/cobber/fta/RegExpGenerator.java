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

	private static StringBuilder segment(int alphas, int digits) {
		StringBuilder ret = new StringBuilder();

		if (alphas != 0) {
			ret.append("\\p{IsAlphabetic}");
			if (alphas > 1)
				ret.append('{').append(alphas).append('}');
		}

		if (digits != 0) {
			ret.append("\\p{IsDigit}");
			if (digits > 1)
				ret.append('{').append(digits).append('}');
		}

		return ret;
	}


	/**
	 * Fast method to simplify a string so that we can determine if all inputs are of the same form.
	 * Smashed strings follow the following rules:
	 *  - Strings of length greater than 30 are replaced with .+
	 *  - any digit is replaced with '1'
	 *  - any alpha is replaced with 'a'
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
			if (Character.isDigit(ch))
				b.append('1');
			else if (Character.isAlphabetic(ch))
				b.append('a');
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
		int digits = 0;
		int alphas = 0;

		for (int i = 0; i < smashed.length(); i++) {
			char ch = smashed.charAt(i);

			switch (ch) {
			case '1':
				if (alphas != 0)
					ret.append(segment(alphas, digits));
				alphas = 0;
				digits++;
				break;

			case 'a':
				if (digits != 0)
					ret.append(segment(alphas, digits));
				digits = 0;
				alphas++;
				break;

			default:
				if (alphas != 0 || digits != 0)
					ret.append(segment(alphas, digits));
				alphas = digits = 0;
				ret.append(slosh(ch));
				break;
			}
		}

		if (alphas != 0 || digits != 0)
			ret.append(segment(alphas, digits));

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

		return result.append(Utils.regExpLength(shortest, longest)).toString();
	}
}
