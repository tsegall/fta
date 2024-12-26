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
package com.cobber.fta;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import com.cobber.fta.core.FTAUnsupportedLocaleException;

public class NumericInfo {
	char decimalSeparator;
	char groupingSeparator;
	char minusSign;
	char negativePrefix;
	boolean hasNegativePrefix;
	char negativeSuffix;
	boolean hasNegativeSuffix;

	private NumericInfo nonLocalized;

	private NumericInfo(final char decimalSeparator, final char groupingSeparator, final char minusSign,
			final boolean hasNegativePrefix, final char negativePrefix, final boolean hasNegativeSuffix, final char negativeSuffix) {
		this.decimalSeparator = decimalSeparator;
		this.groupingSeparator = groupingSeparator;
		this.minusSign = minusSign;
		this.hasNegativePrefix = hasNegativePrefix;
		this.negativePrefix = negativePrefix;
		this.hasNegativeSuffix = hasNegativeSuffix;
	}

	public NumericInfo getNonLocalized() {
		return nonLocalized;
	}

	NumericInfo(final Locale locale) throws FTAUnsupportedLocaleException {
		final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);
		decimalSeparator = formatSymbols.getDecimalSeparator();
		groupingSeparator = formatSymbols.getGroupingSeparator();
		minusSign = formatSymbols.getMinusSign();
		final NumberFormat simple = NumberFormat.getNumberInstance(locale);
		if (simple instanceof DecimalFormat) {
			final DecimalFormat simpleDF = (DecimalFormat) simple;
			String signFacts = simpleDF.getNegativePrefix();
			// Ignore the LEFT_TO_RIGHT_MARK if it exists
			if (!signFacts.isEmpty() && signFacts.charAt(0) == KnownTypes.LEFT_TO_RIGHT_MARK)
				signFacts = signFacts.substring(1);
			if (signFacts.length() > 1)
				throw new FTAUnsupportedLocaleException("No support for locales with multi-character sign prefixes");
			hasNegativePrefix = !signFacts.isEmpty();
			if (hasNegativePrefix)
				negativePrefix = signFacts.charAt(0);
			signFacts = simpleDF.getNegativeSuffix();
			if (signFacts.length() > 1)
				throw new FTAUnsupportedLocaleException("No support for locales with multi-character sign suffixes");
			hasNegativeSuffix = !signFacts.isEmpty();
			if (hasNegativeSuffix)
				negativeSuffix = signFacts.charAt(0);
			if (simple.isGroupingUsed() && simpleDF.getGroupingSize() != 3)
				throw new FTAUnsupportedLocaleException("No support for locales with grouping sizes other than 3");
		} else {
			final String signFacts = String.valueOf(formatSymbols.getMinusSign());
			hasNegativePrefix = true;
			negativePrefix = signFacts.charAt(0);
			hasNegativeSuffix = false;
		}

		// Create the non-localized version iff it would generate different results
		if (decimalSeparator != '.' || groupingSeparator != ',')
			nonLocalized = new NumericInfo('.', ',', '-', true, '-', false, ' ');
	}
}
