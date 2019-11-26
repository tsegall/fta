package com.cobber.fta;

public class Smashed {
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
				ret.append(RegExpGenerator.slosh(ch));
				break;
			}
			last = ch;
		}

		if (count != 0)
			ret.append(segment(ch, count));

		return ret.toString();
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
}
