/*
 * Copyright 2017-2021 Tim Segall
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

import com.cobber.fta.core.RegExpGenerator;

/**
 * Smashed will generate a simplified string, used to determine if all inputs are of the same form.
 *
 * Smashed strings follow the following rules:
 *  - Strings of length greater than SMASHED_MAX are replaced with .+
 *  - any digit is replaced with '9'
 *  - any low alpha (a-f, A-F) is replaced with 'x'
 *  - any high alpha (g-z, G-Z) is replaced with 'X'
 *  - any % is sloshed (i.e. replaced with %%)
 */
public final class Smashed {

	private Smashed() {
		// Never called
	}

	private static final char DIGIT = '9';
	private static final char LOW_ALPHABETIC = 'x';
	private static final char HIGH_ALPHABETIC = 'X';
	private static final char HEX = 'H';

	private static final int SMASHED_MAX = 30;

	/**
	 * Fast method to simplify a string so that we can determine if all inputs are of the same form.
	 *
	 * @param input The input String to be smashed.
	 * @return A 'smashed' String.
	 */
	public static String smash(final String input) {
		final int len = input.length();

		if (len > SMASHED_MAX)
			return ".+";

		// Allocate len + 2 - we need at least len (and maybe more if we see %'s, but this will be enough most of the time)
		final StringBuilder b = new StringBuilder(len + 2);
		for (int i = 0; i < len; i++) {
			final char ch = input.charAt(i);
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
	public static String smashedAsRegExp(final String smashed) {
		if (".+".equals(smashed))
			return smashed;

		final StringBuilder ret = new StringBuilder();
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

	private static StringBuilder segment(final char toRepeat, final int count) {
		final StringBuilder ret = new StringBuilder();
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
