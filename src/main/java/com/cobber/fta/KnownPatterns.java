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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.core.RegExpSplitter;

public class KnownPatterns {
	public static final String OPTIONAL_SIGN = "[+-]?";
	public static final String OPTIONAL_UNICODE_SIGN = "[+\u2212-]?";
	public static final char LEFT_TO_RIGHT_MARK = '\u200E';

	public enum ID {
		ID_BOOLEAN_TRUE_FALSE,
		ID_BOOLEAN_YES_NO,
		ID_BOOLEAN_Y_N,
		ID_BOOLEAN_ONE_ZERO,
		ID_ANY_VARIABLE,
		ID_ALPHA_VARIABLE,
		ID_ALPHANUMERIC_VARIABLE,
		ID_LONG,
		ID_LONG_GROUPING,
		ID_SIGNED_LONG,
		ID_SIGNED_LONG_TRAILING,
		ID_SIGNED_LONG_GROUPING,
		ID_DOUBLE,
		ID_DOUBLE_GROUPING,
		ID_SIGNED_DOUBLE,
		ID_SIGNED_DOUBLE_TRAILING,
		ID_SIGNED_DOUBLE_GROUPING,
		ID_DOUBLE_WITH_EXPONENT,
		ID_SIGNED_DOUBLE_WITH_EXPONENT,
		ID_NULL,
		ID_BLANKORNULL,
		ID_BLANK
	}

	public static final String PATTERN_ANY = ".";
	public static final String PATTERN_ANY_VARIABLE = ".+";
	public static final String PATTERN_ALPHA = "\\p{IsAlphabetic}";
	public static final String PATTERN_NUMERIC = "\\d";
	public static final String PATTERN_NUMERIC_VARIABLE = PATTERN_NUMERIC + "+";
	public static final String PATTERN_ALPHA_VARIABLE = PATTERN_ALPHA + "+";
	public static final String PATTERN_ALPHANUMERIC = "[" + PATTERN_ALPHA + PATTERN_NUMERIC + "]";
	public static final String PATTERN_ALPHANUMERIC_VARIABLE = PATTERN_ALPHANUMERIC + "+";

	public static final String PATTERN_NULL = "[NULL]";
	public static final String PATTERN_WHITESPACE = "[ \t]*";

	public static final String PATTERN_BOOLEAN_TRUE_FALSE = "(?i)(FALSE|TRUE)";
	public static final String PATTERN_BOOLEAN_YES_NO = "(?i)(NO|YES)";
	public static final String PATTERN_BOOLEAN_Y_N = "(?i)(N|Y)";
	public static final String PATTERN_BOOLEAN_ONE_ZERO = "[0|1]";

	private String EXPONENT_REGEXP;

	public String PATTERN_LONG;
	public String PATTERN_LONG_GROUPING;
	public String PATTERN_SIGNED_LONG;
	public String PATTERN_SIGNED_LONG_TRAILING;
	public String PATTERN_SIGNED_LONG_GROUPING;
	public String PATTERN_DOUBLE;
	public String PATTERN_DOUBLE_GROUPING;
	public String PATTERN_SIGNED_DOUBLE;
	public String PATTERN_SIGNED_DOUBLE_TRAILING;
	public String PATTERN_SIGNED_DOUBLE_GROUPING;
	public String PATTERN_DOUBLE_WITH_EXPONENT;
	public String PATTERN_SIGNED_DOUBLE_WITH_EXPONENT;

	private final Map<String, PatternInfo> knownPatterns = new HashMap<>();
	private final Map<ID, PatternInfo> knownIDs = new EnumMap<>(ID.class);
	private final Map<String, PatternInfo> promotion = new HashMap<>();
	private final Map<String, PatternInfo> negation = new HashMap<>();
	private final Map<String, PatternInfo> grouping = new HashMap<>();

	public static String freezeANY(final int minTrimmed, final int maxTrimmed, final int minRawNonBlankLength, final int maxRawNonBlankLength, final boolean leadingWhiteSpace, final boolean trailingWhiteSpace, final boolean multiline) {
		final String leadIn = multiline ? "(?s)." : ".";

		// If there is no leading or trailing space then use the simple min & max of non-blank fields
		if (!leadingWhiteSpace && !trailingWhiteSpace)
			return leadIn + RegExpSplitter.qualify(minRawNonBlankLength, maxRawNonBlankLength);

		// If there was either leading or trailing space then cop out and allow white space on either end and go with the min and max of the trimmed values
		return leadIn + RegExpSplitter.qualify(minTrimmed, maxTrimmed);
	}

	private static String withGrouping(final String regExp, final char groupingSeparator) {
		final String re = groupingSeparator == '.' ? "\\\\." : String.valueOf(groupingSeparator);
		return regExp.replaceAll("\\\\d", "[\\\\d" + re + "]");
	}

	protected String getRegExp(final KnownPatterns.ID id) {
		switch (id) {
		case ID_LONG:
			return PATTERN_LONG;
		case ID_LONG_GROUPING:
			return PATTERN_LONG_GROUPING;
		case ID_SIGNED_LONG:
			return PATTERN_SIGNED_LONG;
		case ID_SIGNED_LONG_TRAILING:
			return PATTERN_SIGNED_LONG_TRAILING;
		case ID_SIGNED_LONG_GROUPING:
			return PATTERN_SIGNED_LONG_GROUPING;
		case ID_DOUBLE:
			return PATTERN_DOUBLE;
		case ID_DOUBLE_GROUPING:
			return PATTERN_DOUBLE_GROUPING;
		case ID_SIGNED_DOUBLE:
			return PATTERN_SIGNED_DOUBLE;
		case ID_SIGNED_DOUBLE_TRAILING:
			return PATTERN_SIGNED_DOUBLE_TRAILING;
		case ID_SIGNED_DOUBLE_GROUPING:
			return PATTERN_SIGNED_DOUBLE_GROUPING;
		case ID_DOUBLE_WITH_EXPONENT:
			return PATTERN_DOUBLE_WITH_EXPONENT;
		case ID_SIGNED_DOUBLE_WITH_EXPONENT:
			return PATTERN_SIGNED_DOUBLE_WITH_EXPONENT;
		case ID_BLANK:
			return PATTERN_WHITESPACE;
		case ID_BOOLEAN_ONE_ZERO:
			return PATTERN_BOOLEAN_ONE_ZERO;
		case ID_BOOLEAN_TRUE_FALSE:
			return PATTERN_BOOLEAN_TRUE_FALSE;
		case ID_BOOLEAN_YES_NO:
			return PATTERN_BOOLEAN_YES_NO;
		case ID_BOOLEAN_Y_N:
			return PATTERN_BOOLEAN_Y_N;
		case ID_NULL:
			return PATTERN_NULL;
		default:
			break;
		}

		return null;
	}

	protected void initialize(final Locale locale) {
		final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);
		final char groupingSeparator = formatSymbols.getGroupingSeparator();
		final char decimalSeparator = formatSymbols.getDecimalSeparator();
		final char minusSign = formatSymbols.getMinusSign();

		String optionalSignPrefix = "";
		String optionalSignSuffix = "";
		final NumberFormat simple = NumberFormat.getNumberInstance(locale);
		if (simple instanceof DecimalFormat) {
			String negPrefix = ((DecimalFormat) simple).getNegativePrefix();
			String negSuffix = ((DecimalFormat) simple).getNegativeSuffix();

			// Ignore the LEFT_TO_RIGHT_MARK if it exists
			if (!negPrefix.isEmpty() && negPrefix.charAt(0) == LEFT_TO_RIGHT_MARK)
				negPrefix = negPrefix.substring(1);
			if (!negPrefix.isEmpty()) {
				if (negPrefix.charAt(0) == minusSign && minusSign == '-')
					optionalSignPrefix = OPTIONAL_SIGN;
				else if (negPrefix.charAt(0) == minusSign && minusSign == '\u2212')  // Unicode minus
					optionalSignPrefix = OPTIONAL_UNICODE_SIGN;
				else
					optionalSignPrefix = RegExpGenerator.slosh(negPrefix) + "?";
			}

			if (!negSuffix.isEmpty()) {
				if (negSuffix.charAt(0) == minusSign && minusSign == '-')
					optionalSignSuffix = OPTIONAL_SIGN;
				else if (negSuffix.charAt(0) == minusSign && minusSign == '\u2212')  // Unicode minus
					optionalSignSuffix = OPTIONAL_UNICODE_SIGN;
				else
					optionalSignSuffix = RegExpGenerator.slosh(negSuffix) + "?";
			}
		}
		else {
			optionalSignPrefix = OPTIONAL_SIGN;
			optionalSignSuffix = "";
		}

		EXPONENT_REGEXP = "(?:[eE](" + optionalSignPrefix + "\\d+))?";
		PATTERN_LONG = "\\d+";
		PATTERN_SIGNED_LONG = optionalSignPrefix + "\\d+" + optionalSignSuffix;
		PATTERN_SIGNED_LONG_TRAILING = PATTERN_LONG + "-?";
		PATTERN_DOUBLE = "\\d*" + RegExpGenerator.slosh(decimalSeparator) + "?" + "\\d+";
		PATTERN_SIGNED_DOUBLE = optionalSignPrefix + PATTERN_DOUBLE + optionalSignSuffix;
		PATTERN_SIGNED_DOUBLE_TRAILING = PATTERN_DOUBLE + "-?";

		PATTERN_LONG_GROUPING = withGrouping(PATTERN_LONG, groupingSeparator);
		PATTERN_SIGNED_LONG_GROUPING = withGrouping(PATTERN_SIGNED_LONG, groupingSeparator);
		PATTERN_DOUBLE_GROUPING = withGrouping(PATTERN_DOUBLE, groupingSeparator);
		PATTERN_SIGNED_DOUBLE_GROUPING = withGrouping(PATTERN_SIGNED_DOUBLE, groupingSeparator);

		PATTERN_DOUBLE_WITH_EXPONENT = PATTERN_DOUBLE + EXPONENT_REGEXP;
		// Not quite what you would expect, always use +- if you have an exponent (locale ar_AE for
		PATTERN_SIGNED_DOUBLE_WITH_EXPONENT = optionalSignPrefix + PATTERN_DOUBLE + EXPONENT_REGEXP;

		knownPatterns.put(PATTERN_BOOLEAN_TRUE_FALSE,
				new PatternInfo(ID.ID_BOOLEAN_TRUE_FALSE, PATTERN_BOOLEAN_TRUE_FALSE, FTAType.BOOLEAN, "TRUE_FALSE", false, false, 4, 5, null, ""));
		knownPatterns.put(PATTERN_BOOLEAN_YES_NO,
				new PatternInfo(ID.ID_BOOLEAN_YES_NO, PATTERN_BOOLEAN_YES_NO, FTAType.BOOLEAN, "YES_NO", false, false, 2, 3, null, ""));
		knownPatterns.put(PATTERN_BOOLEAN_Y_N,
				new PatternInfo(ID.ID_BOOLEAN_Y_N, PATTERN_BOOLEAN_Y_N, FTAType.BOOLEAN, "Y_N", false, false, 2, 3, null, ""));
		knownPatterns.put(PATTERN_BOOLEAN_ONE_ZERO,
				new PatternInfo(ID.ID_BOOLEAN_ONE_ZERO, PATTERN_BOOLEAN_ONE_ZERO, FTAType.BOOLEAN, "ONE_ZERO", false, false, -1, -1, null, null));
		knownPatterns.put(PATTERN_ANY_VARIABLE,
				new PatternInfo(ID.ID_ANY_VARIABLE, PATTERN_ANY_VARIABLE, FTAType.STRING, null, false, false, 1, -1, null, ""));
		knownPatterns.put(PATTERN_ALPHA_VARIABLE,
				new PatternInfo(ID.ID_ALPHA_VARIABLE, PATTERN_ALPHA_VARIABLE, FTAType.STRING, null, false, false, 1, -1, null, ""));
		knownPatterns.put(PATTERN_ALPHANUMERIC_VARIABLE,
				new PatternInfo(ID.ID_ALPHANUMERIC_VARIABLE, PATTERN_ALPHANUMERIC_VARIABLE, FTAType.STRING, null, false, false, 1, -1, null, ""));
		knownPatterns.put(PATTERN_LONG,
				new PatternInfo(ID.ID_LONG, PATTERN_LONG, FTAType.LONG, null, false, false, 1, -1, null, ""));
		knownPatterns.put(PATTERN_SIGNED_LONG,
				new PatternInfo(ID.ID_SIGNED_LONG, PATTERN_SIGNED_LONG, FTAType.LONG, "SIGNED", false, false, 1, -1, null, ""));
		knownPatterns.put(PATTERN_SIGNED_LONG_TRAILING,
				new PatternInfo(ID.ID_SIGNED_LONG_TRAILING, PATTERN_SIGNED_LONG_TRAILING, FTAType.LONG, "SIGNED_TRAILING", false, false, 1, -1, null, ""));
		knownPatterns.put(PATTERN_DOUBLE,
				new PatternInfo(ID.ID_DOUBLE, PATTERN_DOUBLE, FTAType.DOUBLE, null, false, false, -1, -1, null, ""));
		knownPatterns.put(PATTERN_SIGNED_DOUBLE,
				new PatternInfo(ID.ID_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE, FTAType.DOUBLE, "SIGNED", false, false, -1, -1, null, ""));
		knownPatterns.put(PATTERN_SIGNED_DOUBLE_TRAILING,
				new PatternInfo(ID.ID_SIGNED_DOUBLE_TRAILING, PATTERN_SIGNED_DOUBLE_TRAILING, FTAType.DOUBLE, "SIGNED_TRAILING", false, false, -1, -1, null, ""));
		knownPatterns.put(PATTERN_DOUBLE_WITH_EXPONENT,
				new PatternInfo(ID.ID_DOUBLE_WITH_EXPONENT, PATTERN_DOUBLE_WITH_EXPONENT, FTAType.DOUBLE, null, false, false, -1, -1, null, ""));
		knownPatterns.put(PATTERN_SIGNED_DOUBLE_WITH_EXPONENT,
				new PatternInfo(ID.ID_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, FTAType.DOUBLE, "SIGNED", false, false, -1, -1, null, ""));

		knownPatterns.put(PATTERN_LONG_GROUPING,
				new PatternInfo(ID.ID_LONG_GROUPING, PATTERN_LONG_GROUPING, FTAType.LONG, "GROUPING", false, false, 1, -1, null, ""));
		knownPatterns.put(PATTERN_SIGNED_LONG_GROUPING,
				new PatternInfo(ID.ID_SIGNED_LONG_GROUPING, PATTERN_SIGNED_LONG_GROUPING, FTAType.LONG, "SIGNED,GROUPING", false, false, 1, -1, null, ""));

		knownPatterns.put(PATTERN_DOUBLE_GROUPING,
				new PatternInfo(ID.ID_DOUBLE_GROUPING, PATTERN_DOUBLE_GROUPING, FTAType.DOUBLE, "GROUPING", false, false, 1, -1, null, ""));
		knownPatterns.put(PATTERN_SIGNED_DOUBLE_GROUPING,
				new PatternInfo(ID.ID_SIGNED_DOUBLE_GROUPING, PATTERN_SIGNED_DOUBLE_GROUPING, FTAType.DOUBLE, "SIGNED,GROUPING", false, false, 1, -1, null, ""));

		knownPatterns.put(PATTERN_NULL,
				new PatternInfo(ID.ID_NULL, PATTERN_NULL, FTAType.STRING, "NULL", false, false, -1, -1, null, ""));
		knownPatterns.put(PATTERN_WHITESPACE + "BLANKORNULL",
				new PatternInfo(ID.ID_BLANKORNULL, PATTERN_WHITESPACE, FTAType.STRING, "BLANKORNULL", false, false, -1, -1, null, ""));
		knownPatterns.put(PATTERN_WHITESPACE + "BLANK",
				new PatternInfo(ID.ID_BLANK, PATTERN_WHITESPACE, FTAType.STRING, "BLANK", false, false, -1, -1, null, ""));

		// Build the mapping from ID to PatternInfo
		for (final PatternInfo patternInfo : knownPatterns.values()) {
			knownIDs.put(patternInfo.id, patternInfo);
		}

		addBinary(promotion, PATTERN_LONG, PATTERN_SIGNED_LONG, PATTERN_SIGNED_LONG);
		addBinary(promotion, PATTERN_LONG, PATTERN_SIGNED_LONG_TRAILING, PATTERN_SIGNED_LONG_TRAILING);
		addBinary(promotion, PATTERN_LONG, PATTERN_DOUBLE, PATTERN_DOUBLE);
		addBinary(promotion, PATTERN_LONG, PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE);
		addBinary(promotion, PATTERN_LONG, PATTERN_SIGNED_DOUBLE_TRAILING, PATTERN_SIGNED_DOUBLE_TRAILING);
		addBinary(promotion, PATTERN_LONG, PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_DOUBLE_WITH_EXPONENT);
		addBinary(promotion, PATTERN_LONG, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);

		addBinary(promotion, PATTERN_SIGNED_LONG, PATTERN_LONG, PATTERN_SIGNED_LONG);
		addBinary(promotion, PATTERN_SIGNED_LONG, PATTERN_DOUBLE, PATTERN_SIGNED_DOUBLE);
		addBinary(promotion, PATTERN_SIGNED_LONG, PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE);
		addBinary(promotion, PATTERN_SIGNED_LONG, PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
		addBinary(promotion, PATTERN_SIGNED_LONG, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);

		addBinary(promotion, PATTERN_DOUBLE, PATTERN_LONG, PATTERN_DOUBLE);
		addBinary(promotion, PATTERN_DOUBLE, PATTERN_SIGNED_DOUBLE_TRAILING, PATTERN_SIGNED_DOUBLE_TRAILING);
		addBinary(promotion, PATTERN_DOUBLE, PATTERN_SIGNED_LONG, PATTERN_SIGNED_DOUBLE);
		addBinary(promotion, PATTERN_DOUBLE, PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE);
		addBinary(promotion, PATTERN_DOUBLE, PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_DOUBLE_WITH_EXPONENT);
		addBinary(promotion, PATTERN_DOUBLE, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);

		addBinary(promotion, PATTERN_SIGNED_LONG_TRAILING, PATTERN_LONG, PATTERN_SIGNED_LONG_TRAILING);
		addBinary(promotion, PATTERN_SIGNED_DOUBLE_TRAILING, PATTERN_LONG, PATTERN_SIGNED_DOUBLE_TRAILING);
		addBinary(promotion, PATTERN_SIGNED_DOUBLE_TRAILING, PATTERN_DOUBLE, PATTERN_SIGNED_DOUBLE_TRAILING);

		addBinary(promotion, PATTERN_SIGNED_DOUBLE, PATTERN_LONG, PATTERN_SIGNED_DOUBLE);
		addBinary(promotion, PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_LONG, PATTERN_SIGNED_DOUBLE);
		addBinary(promotion, PATTERN_SIGNED_DOUBLE, PATTERN_DOUBLE, PATTERN_SIGNED_DOUBLE);
		addBinary(promotion, PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE_TRAILING, PATTERN_SIGNED_DOUBLE_TRAILING);
		addBinary(promotion, PATTERN_SIGNED_DOUBLE, PATTERN_DOUBLE_WITH_EXPONENT,PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
		addBinary(promotion, PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);

		addBinary(promotion, PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_LONG, PATTERN_DOUBLE_WITH_EXPONENT);
		addBinary(promotion, PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_LONG, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
		addBinary(promotion, PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_DOUBLE, PATTERN_DOUBLE_WITH_EXPONENT);
		addBinary(promotion, PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
		addBinary(promotion, PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);

		addBinary(promotion, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_LONG, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
		addBinary(promotion, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_LONG, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
		addBinary(promotion, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_DOUBLE, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
		addBinary(promotion, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
		addBinary(promotion, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);

		addUnary(negation, PATTERN_LONG, PATTERN_SIGNED_LONG);
		addUnary(negation, PATTERN_LONG_GROUPING, PATTERN_SIGNED_LONG_GROUPING);
		addUnary(negation, PATTERN_SIGNED_LONG, PATTERN_SIGNED_LONG);
		addUnary(negation, PATTERN_SIGNED_LONG_GROUPING, PATTERN_SIGNED_LONG_GROUPING);
		addUnary(negation, PATTERN_DOUBLE, PATTERN_SIGNED_DOUBLE);
		addUnary(negation, PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE);
		addUnary(negation, PATTERN_SIGNED_DOUBLE_TRAILING, PATTERN_SIGNED_DOUBLE_TRAILING);
		addUnary(negation, PATTERN_SIGNED_DOUBLE_GROUPING, PATTERN_SIGNED_DOUBLE_GROUPING);
		addUnary(negation, PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
		addUnary(negation, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);

		addUnary(grouping, PATTERN_LONG, PATTERN_LONG_GROUPING);
		addUnary(grouping, PATTERN_LONG_GROUPING, PATTERN_LONG_GROUPING);
		addUnary(grouping, PATTERN_SIGNED_LONG, PATTERN_SIGNED_LONG_GROUPING);
		addUnary(grouping, PATTERN_SIGNED_LONG_GROUPING, PATTERN_SIGNED_LONG_GROUPING);
		addUnary(grouping, PATTERN_DOUBLE, PATTERN_DOUBLE_GROUPING);
		addUnary(grouping, PATTERN_DOUBLE_GROUPING, PATTERN_DOUBLE_GROUPING);
		addUnary(grouping, PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE_GROUPING);
		addUnary(grouping, PATTERN_SIGNED_DOUBLE_GROUPING, PATTERN_SIGNED_DOUBLE_GROUPING);
	}

	protected void put(final String key, final PatternInfo patternInfo) {
		knownPatterns.put(key, patternInfo);
	}

	protected PatternInfo getByRegExp(final String regExp) {
		return knownPatterns.get(regExp);
	}

	protected PatternInfo getByID(final ID id) {
		return knownIDs.get(id);
	}

	protected PatternInfo numericPromotion(final ID left, final ID right) {
		return promotion.get(left.toString() + "---" + right.toString());
	}

	protected String numericPromotion(final String leftPattern, final String rightPattern) {
		final PatternInfo result = promotion.get(leftPattern + "---" + rightPattern);
		return result == null ? null : result.regexp;
	}

	protected PatternInfo negation(final String pattern) {
		return negation.get(pattern);
	}

	protected PatternInfo grouping(final String pattern) {
		return grouping.get(pattern);
	}

	protected void addUnary(final Map<String, PatternInfo> transformation, final String input, final String result) {
		transformation.put(input, knownPatterns.get(result));
	}

	protected void addBinary(final Map<String, PatternInfo> transformation, final String left, final String right, final String result) {
		transformation.put(left + "---" + right, knownPatterns.get(result));
	}
}
