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
package com.cobber.fta;

import com.cobber.fta.TextAnalyzer.SignStatus;
import com.cobber.fta.core.Utils;

public class Numeric {

	private static NumericResult core(final String trimmed, final int length, final NumericInfo ni) {
		final NumericResult ret = new NumericResult();

		ret.l0 = new StringBuilder(length);

		// Walk the string
		boolean leadingZero = false;
		int startLooking = 0;
		int stopLooking = length;

		int matchesRequired = 0;
		int matches = 0;
		if (ni.hasNegativePrefix) {
			matchesRequired++;
			if (ni.negativePrefix == trimmed.charAt(0)) {
				matches++;
				startLooking = 1;
			}
		}
		if (ni.hasNegativeSuffix) {
			matchesRequired++;
			if (ni.negativeSuffix == trimmed.charAt(length - 1)) {
				matches++;
				stopLooking = length - 1;
			}
		}
		if (matches == matchesRequired && matches > 0)
			ret.numericSigned = SignStatus.LOCALE_STANDARD;

		for (int i = startLooking; i < stopLooking; i++) {
			final char ch = trimmed.charAt(i);

			// Track counts and last occurrence for simple characters
			if (ch <= 127) {
				ret.charCounts[ch]++;
				ret.lastIndex[ch] = i;
			}

			if ((ch == ni.minusSign || ch == '+') && i == 0)
				ret.numericSigned = SignStatus.LEADING_SIGN;
			else if (!ni.hasNegativeSuffix && ret.numericSigned == SignStatus.NONE && ch == '-'
					&& i == stopLooking - 1 && ret.possibleExponentSeen == -1) {
				ret.numericSigned = SignStatus.TRAILING_MINUS;
			} else if (Character.isDigit(ch)) {
				ret.l0.append('d');
				if (ret.digitsSeen == 0 && ch == '0')
					leadingZero = true;
				ret.digitsSeen++;
			} else if (ch == ni.decimalSeparator) {
				ret.l0.append('D');
				ret.numericDecimalSeparators++;
				if (ret.numericDecimalSeparators > 1)
					ret.couldBeNumeric = false;
			} else if (ret.digitsSeen >= 0 && leadingZero == false && ch == ni.groupingSeparator
					&& i + 3 < stopLooking && (i + 4 == stopLooking || !Character.isDigit(trimmed.charAt(i + 4)))) {
				ret.l0.append('G');
				ret.numericGroupingSeparators++;
			} else if (Character.isAlphabetic(ch)) {
				ret.l0.append('a');
				ret.alphasSeen++;
				if (ret.couldBeNumeric && (ch == 'e' || ch == 'E')) {
					if (ret.possibleExponentSeen != -1 || i < 1 || i + 1 >= length)
						ret.couldBeNumeric = false;
					else
						ret.possibleExponentSeen = i;
				}
				else
					ret.couldBeNumeric = false;
			} else {
				ret.l0.append(ch);
				// If the last character was an exponentiation symbol then this better be a sign
				// if it is going to be numeric
				if (ret.possibleExponentSeen != -1 && ret.possibleExponentSeen == i - 1) {
					if (ch != ni.minusSign && ch != '+')
						ret.couldBeNumeric = false;
				}
				else
					ret.couldBeNumeric = false;
			}
		}

		if (ret.couldBeNumeric && ret.possibleExponentSeen != -1) {
			final int exponentLength = stopLooking - ret.possibleExponentSeen - 1;
			if (exponentLength >= 5)
				ret.couldBeNumeric = false;
			else {
				int offset = ret.possibleExponentSeen + 1;
				// parseInt cannot cope with UTF-8 minus sign, which is used in some locales, so
				// just skip sign
				char ch = trimmed.charAt(ret.possibleExponentSeen + 1);
				if ((ch == ni.minusSign || ch == '-' || ch == '+') && offset + 1 < stopLooking) {
					offset++;
					ch = trimmed.charAt(offset);
				}
				if (!Utils.isSimpleDigit(ch))
					ret.couldBeNumeric = false;
				else {
					final int exponentSize = Integer.parseInt(trimmed.substring(offset, stopLooking));
					if (exponentSize > 308)
						ret.couldBeNumeric = false;
				}
			}
		}

		if (ret.couldBeNumeric && ret.numericGroupingSeparators == 1 && ret.numericDecimalSeparators == 0 && ni.groupingSeparator == '.' &&
				(ret.digitsSeen - 1) / 3 > ret.numericGroupingSeparators)
			ret.couldBeNumeric = false;

		return ret;
	}

	public static NumericResult analyze(final String trimmed, final int length, final NumericInfo ni) {
		final NumericResult ret = core(trimmed, length, ni);

		// If we did not find a numeric value, then try again to find a non-localized numeric value
		if (ret.couldBeNumeric == false && ret.alphasSeen == 0 && ni.getNonLocalized() != null) {
			final NumericResult retNL = core(trimmed, length, ni.getNonLocalized());
			if (retNL.couldBeNumeric) {
				if (retNL.numericDecimalSeparators == 1)
					retNL.nonLocalizedDouble = true;
				return retNL;
			}
		}

		return ret;
	}
}
