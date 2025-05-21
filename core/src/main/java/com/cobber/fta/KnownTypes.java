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

/**
 * A set of predefined types, for simple numerics, strings and boolean values.
 */
public class KnownTypes {
	public static final String OPTIONAL_SIGN = "[+-]?";
	public static final String OPTIONAL_UNICODE_SIGN = "[+\u2212-]?";
	public static final char LEFT_TO_RIGHT_MARK = '\u200E';

	public enum ID {
		ID_BOOLEAN_TRUE_FALSE,
		ID_BOOLEAN_YES_NO,
		ID_BOOLEAN_YES_NO_LOCALIZED,
		ID_BOOLEAN_Y_N,
		ID_BOOLEAN_ONE_ZERO,
		ID_ANY_VARIABLE,
		ID_ALPHA_VARIABLE,
		ID_ALPHANUMERIC_VARIABLE,
		ID_LONG,
		ID_LONG_GROUPING,
		ID_SIGNED_LONG,
		ID_SIGNED_LONG_GROUPING,
		ID_SIGNED_LONG_TRAILING,
		ID_SIGNED_LONG_TRAILING_GROUPING,
		ID_DOUBLE,
		ID_DOUBLE_GROUPING,
		ID_SIGNED_DOUBLE,
		ID_SIGNED_DOUBLE_GROUPING,
		ID_SIGNED_DOUBLE_TRAILING,
		ID_SIGNED_DOUBLE_TRAILING_GROUPING,
		ID_DOUBLE_WITH_EXPONENT,
		ID_DOUBLE_WITH_EXPONENT_GROUPING,
		ID_SIGNED_DOUBLE_WITH_EXPONENT,
		ID_SIGNED_DOUBLE_WITH_EXPONENT_GROUPING,
		ID_DOUBLE_NL,
		ID_SIGNED_DOUBLE_NL,
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
	public static String PATTERN_BOOLEAN_YES_NO_LOCALIZED;
	public static final String PATTERN_BOOLEAN_Y_N = "(?i)(N|Y)";
	public static final String PATTERN_BOOLEAN_ONE_ZERO = "(0|1)";

	public String PATTERN_LONG;
	public String PATTERN_LONG_GROUPING;
	public String PATTERN_SIGNED_LONG;
	public String PATTERN_SIGNED_LONG_GROUPING;
	public String PATTERN_SIGNED_LONG_TRAILING;
	public String PATTERN_SIGNED_LONG_TRAILING_GROUPING;
	public String PATTERN_DOUBLE;
	public String PATTERN_DOUBLE_GROUPING;
	public String PATTERN_SIGNED_DOUBLE_GROUPING;
	public String PATTERN_SIGNED_DOUBLE;
	public String PATTERN_SIGNED_DOUBLE_TRAILING;
	public String PATTERN_SIGNED_DOUBLE_TRAILING_GROUPING;
	public String PATTERN_DOUBLE_WITH_EXPONENT;
	public String PATTERN_DOUBLE_WITH_EXPONENT_GROUPING;
	public String PATTERN_SIGNED_DOUBLE_WITH_EXPONENT;
	public String PATTERN_SIGNED_DOUBLE_WITH_EXPONENT_GROUPING;
	// These two are for Non-localized doubles, i.e. if we have a country that uses ',' for decimal separator
	// and '.' for thousands separator then that is reflected in PATTERN_DOUBLE, but PATTERN_DOUBLE_NL will use a '.'
	public String PATTERN_DOUBLE_NL;
	public String PATTERN_SIGNED_DOUBLE_NL;

	private final Map<String, TypeInfo> knownTypes = new HashMap<>();
	private final Map<ID, TypeInfo> knownIDs = new EnumMap<>(ID.class);
	private final Map<String, TypeInfo> promotion = new HashMap<>();
	private final Map<String, TypeInfo> negation = new HashMap<>();
	private final Map<String, TypeInfo> grouping = new HashMap<>();

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

	protected void initialize(final Locale locale) {
		final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);
		final char groupingSeparator = formatSymbols.getGroupingSeparator();
		final char decimalSeparator = formatSymbols.getDecimalSeparator();
		final char minusSign = formatSymbols.getMinusSign();
		final Keywords keywords = Keywords.getInstance(locale);

		String optionalSignPrefix = "";
		String optionalSignSuffix = "";
		final NumberFormat simple = NumberFormat.getNumberInstance(locale);
		if (simple instanceof DecimalFormat) {
			String negPrefix = ((DecimalFormat) simple).getNegativePrefix();
			final String negSuffix = ((DecimalFormat) simple).getNegativeSuffix();

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

		final String EXPONENT_REGEXP = "(?:[eE](" + optionalSignPrefix + "\\d+))?";
		PATTERN_LONG = "\\d+";
		PATTERN_SIGNED_LONG = optionalSignPrefix + "\\d+" + optionalSignSuffix;
		PATTERN_SIGNED_LONG_TRAILING = PATTERN_LONG + "-?";
		PATTERN_DOUBLE = "\\d*" + RegExpGenerator.slosh(decimalSeparator) + "?" + "\\d+";
		PATTERN_SIGNED_DOUBLE = optionalSignPrefix + PATTERN_DOUBLE + optionalSignSuffix;
		PATTERN_SIGNED_DOUBLE_TRAILING = PATTERN_DOUBLE + "-?";

		if (decimalSeparator != '.' && !optionalSignPrefix.isEmpty() && optionalSignSuffix.isEmpty()) {
			PATTERN_DOUBLE_NL = "\\d*\\.?\\d+";
			PATTERN_SIGNED_DOUBLE_NL = optionalSignPrefix + PATTERN_DOUBLE_NL;
		}

		PATTERN_DOUBLE_WITH_EXPONENT = PATTERN_DOUBLE + EXPONENT_REGEXP;
		// Not quite what you would expect, always use +- if you have an exponent (locale ar_AE for
		PATTERN_SIGNED_DOUBLE_WITH_EXPONENT = optionalSignPrefix + PATTERN_DOUBLE + EXPONENT_REGEXP;

		PATTERN_LONG_GROUPING = withGrouping(PATTERN_LONG, groupingSeparator);
		PATTERN_SIGNED_LONG_GROUPING = withGrouping(PATTERN_SIGNED_LONG, groupingSeparator);
		PATTERN_SIGNED_LONG_TRAILING_GROUPING = withGrouping(PATTERN_SIGNED_LONG_TRAILING, groupingSeparator);
		PATTERN_DOUBLE_GROUPING = withGrouping(PATTERN_DOUBLE, groupingSeparator);
		PATTERN_SIGNED_DOUBLE_GROUPING = withGrouping(PATTERN_SIGNED_DOUBLE, groupingSeparator);
		PATTERN_SIGNED_DOUBLE_TRAILING_GROUPING = withGrouping(PATTERN_SIGNED_DOUBLE_TRAILING, groupingSeparator);
		PATTERN_DOUBLE_WITH_EXPONENT_GROUPING = withGrouping(PATTERN_DOUBLE_WITH_EXPONENT, groupingSeparator);
		PATTERN_SIGNED_DOUBLE_WITH_EXPONENT_GROUPING = withGrouping(PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, groupingSeparator);

		knownTypes.put(PATTERN_BOOLEAN_TRUE_FALSE,
				new TypeInfo(ID.ID_BOOLEAN_TRUE_FALSE, PATTERN_BOOLEAN_TRUE_FALSE, FTAType.BOOLEAN, "TRUE_FALSE", false, ""));
		knownTypes.put(PATTERN_BOOLEAN_YES_NO,
				new TypeInfo(ID.ID_BOOLEAN_YES_NO, PATTERN_BOOLEAN_YES_NO, FTAType.BOOLEAN, "YES_NO", false, ""));
		// Check to see if we have a localized version of Yes/No, if so add it
		final String localizedYes = keywords.get("YES");
		final String localizedNo = keywords.get("NO");
		if (localizedYes != null && localizedNo != null) {
			if (!"yes".equals(localizedYes) || !"no".equals(localizedNo)) {
				PATTERN_BOOLEAN_YES_NO_LOCALIZED = "(?i)(" + localizedNo + "|" + localizedYes + ")";
				knownTypes.put(PATTERN_BOOLEAN_YES_NO_LOCALIZED,
						new TypeInfo(ID.ID_BOOLEAN_YES_NO_LOCALIZED, PATTERN_BOOLEAN_YES_NO_LOCALIZED, FTAType.BOOLEAN, "YES_NO", false, ""));
			}
		}

		final TypeInfo PI_BOOLEAN_Y_N = new TypeInfo(ID.ID_BOOLEAN_Y_N, PATTERN_BOOLEAN_Y_N, FTAType.BOOLEAN, "Y_N", false, "");
		final TypeInfo PI_BOOLEAN_ONE_ZERO = new TypeInfo(ID.ID_BOOLEAN_ONE_ZERO, PATTERN_BOOLEAN_ONE_ZERO, FTAType.BOOLEAN, "ONE_ZERO", TypeInfo.ONE_ZERO_FLAG);
		final TypeInfo PI_ANY_VARIABLE = new TypeInfo(ID.ID_ANY_VARIABLE, PATTERN_ANY_VARIABLE, FTAType.STRING, null, 0);
		final TypeInfo PI_ALPHA_VARIABLE = new TypeInfo(ID.ID_ALPHA_VARIABLE, PATTERN_ALPHA_VARIABLE, FTAType.STRING, null, 0);
		final TypeInfo PI_ALPHANUMERIC_VARIABLE = new TypeInfo(ID.ID_ALPHANUMERIC_VARIABLE, PATTERN_ALPHANUMERIC_VARIABLE, FTAType.STRING, null, 0);
		final TypeInfo PI_LONG = new TypeInfo(ID.ID_LONG, PATTERN_LONG, FTAType.LONG, null, 0);
		final TypeInfo PI_SIGNED_LONG = new TypeInfo(ID.ID_SIGNED_LONG, PATTERN_SIGNED_LONG, FTAType.LONG, "SIGNED", TypeInfo.SIGNED_FLAG);
		final TypeInfo PI_SIGNED_LONG_TRAILING = new TypeInfo(ID.ID_SIGNED_LONG_TRAILING, PATTERN_SIGNED_LONG_TRAILING, FTAType.LONG, "SIGNED_TRAILING", TypeInfo.SIGNED_TRAILING_FLAG);
		final TypeInfo PI_DOUBLE = new TypeInfo(ID.ID_DOUBLE, PATTERN_DOUBLE, FTAType.DOUBLE, null, 0);
		final TypeInfo PI_SIGNED_DOUBLE = new TypeInfo(ID.ID_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE, FTAType.DOUBLE, "SIGNED", TypeInfo.SIGNED_FLAG);
		if (PATTERN_DOUBLE_NL != null) {
			final TypeInfo PI_DOUBLE_NL = new TypeInfo(ID.ID_DOUBLE_NL, PATTERN_DOUBLE_NL, FTAType.DOUBLE, "NON_LOCALIZED", TypeInfo.NON_LOCALIZED_FLAG);
			final TypeInfo PI_SIGNED_DOUBLE_NL = new TypeInfo(ID.ID_SIGNED_DOUBLE_NL, PATTERN_SIGNED_DOUBLE_NL, FTAType.DOUBLE, "SIGNED,NON_LOCALIZED", TypeInfo.SIGNED_FLAG|TypeInfo.NON_LOCALIZED_FLAG);
			knownTypes.put(PATTERN_DOUBLE_NL, PI_DOUBLE_NL);
			knownTypes.put(PATTERN_SIGNED_DOUBLE_NL,PI_SIGNED_DOUBLE_NL);
		}
		final TypeInfo PI_SIGNED_DOUBLE_TRAILING = new TypeInfo(ID.ID_SIGNED_DOUBLE_TRAILING, PATTERN_SIGNED_DOUBLE_TRAILING, FTAType.DOUBLE, "SIGNED_TRAILING", TypeInfo.SIGNED_TRAILING_FLAG);
		final TypeInfo PI_DOUBLE_WITH_EXPONENT = new TypeInfo(ID.ID_DOUBLE_WITH_EXPONENT, PATTERN_DOUBLE_WITH_EXPONENT, FTAType.DOUBLE, null, TypeInfo.EXPONENT_FLAG);
		final TypeInfo PI_DOUBLE_WITH_EXPONENT_GROUPING = new TypeInfo(ID.ID_DOUBLE_WITH_EXPONENT_GROUPING, PATTERN_DOUBLE_WITH_EXPONENT_GROUPING, FTAType.DOUBLE, "GROUPING", TypeInfo.GROUPING_FLAG|TypeInfo.EXPONENT_FLAG);
		final TypeInfo PI_SIGNED_DOUBLE_WITH_EXPONENT = new TypeInfo(ID.ID_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, FTAType.DOUBLE, "SIGNED", TypeInfo.SIGNED_FLAG|TypeInfo.EXPONENT_FLAG);
		final TypeInfo PI_SIGNED_DOUBLE_WITH_EXPONENT_GROUPING = new TypeInfo(ID.ID_SIGNED_DOUBLE_WITH_EXPONENT_GROUPING, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT_GROUPING, FTAType.DOUBLE, "SIGNED,GROUPING", TypeInfo.SIGNED_FLAG|TypeInfo.GROUPING_FLAG|TypeInfo.EXPONENT_FLAG);
		final TypeInfo PI_LONG_GROUPING = new TypeInfo(ID.ID_LONG_GROUPING, PATTERN_LONG_GROUPING, FTAType.LONG, "GROUPING", TypeInfo.GROUPING_FLAG);
		final TypeInfo PI_SIGNED_LONG_GROUPING = new TypeInfo(ID.ID_SIGNED_LONG_GROUPING, PATTERN_SIGNED_LONG_GROUPING, FTAType.LONG, "SIGNED,GROUPING", TypeInfo.SIGNED_FLAG|TypeInfo.GROUPING_FLAG);
		final TypeInfo PI_SIGNED_LONG_TRAILING_GROUPING = new TypeInfo(ID.ID_SIGNED_LONG_TRAILING_GROUPING, PATTERN_SIGNED_LONG_TRAILING_GROUPING, FTAType.LONG, "SIGNED_TRAILING,GROUPING", TypeInfo.SIGNED_TRAILING_FLAG|TypeInfo.GROUPING_FLAG);
		final TypeInfo PI_DOUBLE_GROUPING = new TypeInfo(ID.ID_DOUBLE_GROUPING, PATTERN_DOUBLE_GROUPING, FTAType.DOUBLE, "GROUPING", TypeInfo.GROUPING_FLAG);
		final TypeInfo PI_SIGNED_DOUBLE_GROUPING = new TypeInfo(ID.ID_SIGNED_DOUBLE_GROUPING, PATTERN_SIGNED_DOUBLE_GROUPING, FTAType.DOUBLE, "SIGNED,GROUPING", TypeInfo.SIGNED_FLAG|TypeInfo.GROUPING_FLAG);
		final TypeInfo PI_SIGNED_DOUBLE_TRAILING_GROUPING = new TypeInfo(ID.ID_SIGNED_DOUBLE_TRAILING_GROUPING, PATTERN_SIGNED_DOUBLE_TRAILING_GROUPING, FTAType.DOUBLE, "SIGNED_TRAILING,GROUPING", TypeInfo.SIGNED_TRAILING_FLAG|TypeInfo.GROUPING_FLAG);
		final TypeInfo PI_NULL = new TypeInfo(ID.ID_NULL, PATTERN_NULL, FTAType.STRING, "NULL", TypeInfo.NULL_FLAG);
		final TypeInfo PI_BLANKORNULL = new TypeInfo(ID.ID_BLANKORNULL, PATTERN_WHITESPACE, FTAType.STRING, "BLANKORNULL", TypeInfo.BLANKORNULL_FLAG);
		final TypeInfo PI_BLANK = new TypeInfo(ID.ID_BLANK, PATTERN_WHITESPACE, FTAType.STRING, "BLANK", TypeInfo.BLANK_FLAG);

		knownTypes.put(PATTERN_BOOLEAN_Y_N, PI_BOOLEAN_Y_N);
		knownTypes.put(PATTERN_BOOLEAN_ONE_ZERO, PI_BOOLEAN_ONE_ZERO);
		knownTypes.put(PATTERN_ANY_VARIABLE, PI_ANY_VARIABLE);
		knownTypes.put(PATTERN_ALPHA_VARIABLE, PI_ALPHA_VARIABLE);
		knownTypes.put(PATTERN_ALPHANUMERIC_VARIABLE, PI_ALPHANUMERIC_VARIABLE);

		knownTypes.put(PATTERN_LONG, PI_LONG);
		knownTypes.put(PATTERN_LONG_GROUPING, PI_LONG_GROUPING);
		knownTypes.put(PATTERN_SIGNED_LONG, PI_SIGNED_LONG);
		knownTypes.put(PATTERN_SIGNED_LONG_GROUPING, PI_SIGNED_LONG_GROUPING);
		knownTypes.put(PATTERN_SIGNED_LONG_TRAILING, PI_SIGNED_LONG_TRAILING);
		knownTypes.put(PATTERN_SIGNED_LONG_TRAILING_GROUPING, PI_SIGNED_LONG_TRAILING_GROUPING);

		knownTypes.put(PATTERN_DOUBLE, PI_DOUBLE);
		knownTypes.put(PATTERN_DOUBLE_GROUPING, PI_DOUBLE_GROUPING);
		knownTypes.put(PATTERN_SIGNED_DOUBLE, PI_SIGNED_DOUBLE);
		knownTypes.put(PATTERN_SIGNED_DOUBLE_GROUPING, PI_SIGNED_DOUBLE_GROUPING);
		knownTypes.put(PATTERN_SIGNED_DOUBLE_TRAILING, PI_SIGNED_DOUBLE_TRAILING);
		knownTypes.put(PATTERN_SIGNED_DOUBLE_TRAILING_GROUPING, PI_SIGNED_DOUBLE_TRAILING_GROUPING);
		knownTypes.put(PATTERN_DOUBLE_WITH_EXPONENT, PI_DOUBLE_WITH_EXPONENT);
		knownTypes.put(PATTERN_DOUBLE_WITH_EXPONENT_GROUPING, PI_DOUBLE_WITH_EXPONENT_GROUPING);
		knownTypes.put(PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PI_SIGNED_DOUBLE_WITH_EXPONENT);
		knownTypes.put(PATTERN_SIGNED_DOUBLE_WITH_EXPONENT_GROUPING, PI_SIGNED_DOUBLE_WITH_EXPONENT_GROUPING);

		knownTypes.put(PATTERN_NULL, PI_NULL);
		knownTypes.put(PATTERN_WHITESPACE + "BLANKORNULL", PI_BLANKORNULL);
		knownTypes.put(PATTERN_WHITESPACE + "BLANK", PI_BLANK);

		// Build the mapping from ID to TypeInfo
		for (final TypeInfo typeInfo : knownTypes.values())
			knownIDs.put(typeInfo.id, typeInfo);

		addBinary(promotion, PATTERN_LONG, PATTERN_SIGNED_LONG, PATTERN_SIGNED_LONG);
		addBinary(promotion, PATTERN_LONG, PATTERN_SIGNED_LONG_TRAILING, PATTERN_SIGNED_LONG_TRAILING);
		addBinary(promotion, PATTERN_LONG, PATTERN_DOUBLE, PATTERN_DOUBLE);
		addBinary(promotion, PATTERN_LONG, PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE);
		addBinary(promotion, PATTERN_LONG, PATTERN_SIGNED_DOUBLE_TRAILING, PATTERN_SIGNED_DOUBLE_TRAILING);
		addBinary(promotion, PATTERN_LONG, PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_DOUBLE_WITH_EXPONENT);
		addBinary(promotion, PATTERN_LONG, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
		addBinary(promotion, PATTERN_LONG, PATTERN_DOUBLE_NL, PATTERN_DOUBLE_NL);
		addBinary(promotion, PATTERN_LONG, PATTERN_SIGNED_DOUBLE_NL, PATTERN_SIGNED_DOUBLE_NL);

		addBinary(promotion, PATTERN_SIGNED_LONG, PATTERN_LONG, PATTERN_SIGNED_LONG);
		addBinary(promotion, PATTERN_SIGNED_LONG, PATTERN_DOUBLE, PATTERN_SIGNED_DOUBLE);
		addBinary(promotion, PATTERN_SIGNED_LONG, PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE);
		addBinary(promotion, PATTERN_SIGNED_LONG, PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
		addBinary(promotion, PATTERN_SIGNED_LONG, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
		addBinary(promotion, PATTERN_SIGNED_LONG, PATTERN_DOUBLE_NL, PATTERN_SIGNED_DOUBLE_NL);
		addBinary(promotion, PATTERN_SIGNED_LONG, PATTERN_SIGNED_DOUBLE_NL, PATTERN_SIGNED_DOUBLE_NL);

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
		addUnary(negation, PATTERN_SIGNED_LONG_TRAILING, PATTERN_SIGNED_LONG_TRAILING);
		addUnary(negation, PATTERN_SIGNED_LONG_TRAILING_GROUPING, PATTERN_SIGNED_LONG_TRAILING_GROUPING);
		addUnary(negation, PATTERN_DOUBLE, PATTERN_SIGNED_DOUBLE);
		addUnary(negation, PATTERN_DOUBLE_GROUPING, PATTERN_SIGNED_DOUBLE_GROUPING);
		addUnary(negation, PATTERN_SIGNED_DOUBLE_GROUPING, PATTERN_SIGNED_DOUBLE_GROUPING);
		addUnary(negation, PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE);
		addUnary(negation, PATTERN_SIGNED_DOUBLE_TRAILING, PATTERN_SIGNED_DOUBLE_TRAILING);
		addUnary(negation, PATTERN_SIGNED_DOUBLE_GROUPING, PATTERN_SIGNED_DOUBLE_GROUPING);
		addUnary(negation, PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
		addUnary(negation, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
		addUnary(negation, PATTERN_DOUBLE_WITH_EXPONENT_GROUPING, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT_GROUPING);
		addUnary(negation, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT_GROUPING, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT_GROUPING);
		addUnary(negation, PATTERN_DOUBLE_NL, PATTERN_SIGNED_DOUBLE_NL);
		addUnary(negation, PATTERN_SIGNED_DOUBLE_NL, PATTERN_SIGNED_DOUBLE_NL);

		addUnary(grouping, PATTERN_LONG, PATTERN_LONG_GROUPING);
		addUnary(grouping, PATTERN_LONG_GROUPING, PATTERN_LONG_GROUPING);
		addUnary(grouping, PATTERN_SIGNED_LONG, PATTERN_SIGNED_LONG_GROUPING);
		addUnary(grouping, PATTERN_SIGNED_LONG_GROUPING, PATTERN_SIGNED_LONG_GROUPING);
		addUnary(grouping, PATTERN_SIGNED_LONG_TRAILING, PATTERN_SIGNED_LONG_TRAILING_GROUPING);
		addUnary(grouping, PATTERN_SIGNED_LONG_TRAILING_GROUPING, PATTERN_SIGNED_LONG_TRAILING_GROUPING);
		addUnary(grouping, PATTERN_DOUBLE, PATTERN_DOUBLE_GROUPING);
		addUnary(grouping, PATTERN_DOUBLE_GROUPING, PATTERN_DOUBLE_GROUPING);
		addUnary(grouping, PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE_GROUPING);
		addUnary(grouping, PATTERN_SIGNED_DOUBLE_GROUPING, PATTERN_SIGNED_DOUBLE_GROUPING);
		addUnary(grouping, PATTERN_SIGNED_DOUBLE_TRAILING, PATTERN_SIGNED_DOUBLE_TRAILING_GROUPING);
		addUnary(grouping, PATTERN_SIGNED_DOUBLE_TRAILING_GROUPING, PATTERN_SIGNED_DOUBLE_TRAILING_GROUPING);
		addUnary(grouping, PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_DOUBLE_WITH_EXPONENT_GROUPING);
		addUnary(grouping, PATTERN_DOUBLE_WITH_EXPONENT_GROUPING, PATTERN_DOUBLE_WITH_EXPONENT_GROUPING);
		addUnary(grouping, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT_GROUPING);
		addUnary(grouping, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT_GROUPING, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT_GROUPING);
	}

	protected void put(final String key, final TypeInfo typeInfo) {
		knownTypes.put(key, typeInfo);
	}

	protected TypeInfo getByRegExp(final String regExp) {
		return knownTypes.get(regExp);
	}

	protected TypeInfo getByID(final ID id) {
		return knownIDs.get(id);
	}

	protected TypeInfo getByTypeAndModifier(final FTAType type, final int typeModifier) {
		for (final TypeInfo typeInfo : knownTypes.values()) {
			if (typeInfo.getBaseType() == type && typeInfo.typeModifierFlags == typeModifier)
				return typeInfo;
		}

		return null;
	}

	protected TypeInfo numericPromotion(final ID left, final ID right) {
		return promotion.get(left.toString() + "---" + right.toString());
	}

	protected String numericPromotion(final String leftPattern, final String rightPattern) {
		final TypeInfo result = promotion.get(leftPattern + "---" + rightPattern);
		return result == null ? null : result.regexp;
	}

	protected TypeInfo negation(final String pattern) {
		return negation.get(pattern);
	}

	protected TypeInfo grouping(final ID id) {
		return grouping.get(knownIDs.get(id).regexp);
	}

	protected TypeInfo grouping(final String pattern) {
		return grouping.get(pattern);
	}

	protected void addUnary(final Map<String, TypeInfo> transformation, final String input, final String result) {
		transformation.put(input, knownTypes.get(result));
	}

	protected void addBinary(final Map<String, TypeInfo> transformation, final String left, final String right, final String result) {
		transformation.put(left + "---" + right, knownTypes.get(result));
	}
}
