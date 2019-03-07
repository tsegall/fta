package com.cobber.fta;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class KnownPatterns {
	public enum ID {
		ID_BOOLEAN_TRUE_FALSE,
		ID_BOOLEAN_YES_NO,
		ID_BOOLEAN_ONE_ZERO,
		ID_ANY_VARIABLE,
		ID_ALPHA_VARIABLE,
		ID_ALPHANUMERIC_VARIABLE,
		ID_LONG,
		ID_LONG_GROUPING,
		ID_SIGNED_LONG,
		ID_SIGNED_LONG_GROUPING,
		ID_DOUBLE,
		ID_DOUBLE_GROUPING,
		ID_SIGNED_DOUBLE,
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
	public static final String PATTERN_ALPHA_VARIABLE = PATTERN_ALPHA + "+";
	public static final String PATTERN_ALPHANUMERIC = "[" + PATTERN_ALPHA + PATTERN_NUMERIC + "]";
	public static final String PATTERN_ALPHANUMERIC_VARIABLE = PATTERN_ALPHANUMERIC + "+";

	public static final String PATTERN_NULL = "[NULL]";
	public static final String PATTERN_WHITESPACE = "\\p{javaWhitespace}*";

	public static final String PATTERN_BOOLEAN_TRUE_FALSE = "(?i)(true|false)";
	public static final String PATTERN_BOOLEAN_YES_NO = "(?i)(yes|no)";
	public static final String PATTERN_BOOLEAN_ONE_ZERO = "[0|1]";

	public String PATTERN_LONG;
	public String PATTERN_LONG_GROUPING;
	public String PATTERN_SIGNED_LONG;
	public String PATTERN_SIGNED_LONG_GROUPING;
	public String PATTERN_DOUBLE;
	public String PATTERN_DOUBLE_GROUPING;
	public String PATTERN_SIGNED_DOUBLE;
	public String PATTERN_SIGNED_DOUBLE_GROUPING;
	public String PATTERN_DOUBLE_WITH_EXPONENT;
	public String PATTERN_SIGNED_DOUBLE_WITH_EXPONENT;

	Map<String, PatternInfo> knownPatterns = new HashMap<>();
	Map<ID, PatternInfo> knownIDs = new HashMap<>();
	Map<String, PatternInfo> promotion = new HashMap<>();
	Map<String, PatternInfo> negation = new HashMap<>();
	Map<String, PatternInfo> grouping = new HashMap<>();

	public static String freezeANY(int minTrimmed, int maxTrimmed, int minRawNonBlankLength, int maxRawNonBlankLength, boolean leadingWhiteSpace, boolean trailingWhiteSpace, boolean multiline) {
		String leadIn = multiline ? "(?s)." : ".";

		// If there is no leading or trailing space then use the simple min & max of non-blank fields
		if (!leadingWhiteSpace && !trailingWhiteSpace)
			return leadIn + Utils.regExpLength(minRawNonBlankLength, maxRawNonBlankLength);

		// If there was either leading or trailing space then cop out and allow white space on either end and go with the min and max of the trimmed values
		return leadIn + Utils.regExpLength(minTrimmed, maxTrimmed);
	}

	static String withGrouping(String regExp, char groupingSeparator) {
		String re = groupingSeparator == '.' ? "\\\\." : String.valueOf(groupingSeparator);
		return regExp.replaceAll("\\\\d", "[\\\\d" + re + "]");
	}

	String getRegExp(KnownPatterns.ID id) {
		switch (id) {
		case ID_LONG:
			return PATTERN_LONG;
		case ID_LONG_GROUPING:
			return PATTERN_LONG_GROUPING;
		case ID_SIGNED_LONG:
			return PATTERN_SIGNED_LONG;
		case ID_SIGNED_LONG_GROUPING:
			return PATTERN_SIGNED_LONG_GROUPING;
		case ID_DOUBLE:
			return PATTERN_DOUBLE;
		case ID_DOUBLE_GROUPING:
			return PATTERN_DOUBLE_GROUPING;
		case ID_SIGNED_DOUBLE:
			return PATTERN_SIGNED_DOUBLE;
		case ID_SIGNED_DOUBLE_GROUPING:
			return PATTERN_SIGNED_DOUBLE_GROUPING;
		case ID_DOUBLE_WITH_EXPONENT:
			return PATTERN_DOUBLE_WITH_EXPONENT;
		case ID_SIGNED_DOUBLE_WITH_EXPONENT:
			return PATTERN_SIGNED_DOUBLE_WITH_EXPONENT;
		}

		return null;
	}

	void initialize(Locale locale) {
		DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);
		char groupingSeparator = formatSymbols.getGroupingSeparator();
		char decimalSeparator = formatSymbols.getDecimalSeparator();
		char minusSign = formatSymbols.getMinusSign();

		String optionalSignPrefix = "";
		String optionalSignSuffix = "";
		NumberFormat simple = NumberFormat.getNumberInstance(locale);
		if (simple instanceof DecimalFormat) {
			String negPrefix = ((DecimalFormat) simple).getNegativePrefix();
			String negSuffix = ((DecimalFormat) simple).getNegativeSuffix();

			if (!negPrefix.isEmpty()) {
				if (negPrefix.charAt(0) == minusSign)
					optionalSignPrefix = "[+-]?";
				else
					optionalSignPrefix = RegExpGenerator.slosh(negPrefix) + "?";
			}
			if (!negSuffix.isEmpty()) {
				if (negSuffix.charAt(0) == minusSign)
					optionalSignSuffix = "[+-]?";
				else
					optionalSignSuffix = RegExpGenerator.slosh(negSuffix) + "?";
			}
		}
		else {
			optionalSignPrefix = "[+-]?";
			optionalSignSuffix = "";
		}

		PATTERN_LONG = "\\d+";
		PATTERN_SIGNED_LONG = optionalSignPrefix + "\\d+" + optionalSignSuffix;
		PATTERN_DOUBLE = PATTERN_LONG + "|" + "(\\d+)?" + RegExpGenerator.slosh(decimalSeparator) + "\\d+";
		PATTERN_SIGNED_DOUBLE = PATTERN_SIGNED_LONG + "|" + optionalSignPrefix + "(\\d+)?" + RegExpGenerator.slosh(decimalSeparator) + "\\d+" + optionalSignSuffix;

		PATTERN_LONG_GROUPING = withGrouping(PATTERN_LONG, groupingSeparator);
		PATTERN_SIGNED_LONG_GROUPING = withGrouping(PATTERN_SIGNED_LONG, groupingSeparator);
		PATTERN_DOUBLE_GROUPING = withGrouping(PATTERN_DOUBLE, groupingSeparator);
		PATTERN_SIGNED_DOUBLE_GROUPING = withGrouping(PATTERN_SIGNED_DOUBLE, groupingSeparator);

		PATTERN_DOUBLE_WITH_EXPONENT = PATTERN_DOUBLE + "(?:[eE]([-+]?\\d+))?";
		// Not localized!!
		PATTERN_SIGNED_DOUBLE_WITH_EXPONENT = "[+-]?\\d+" + "|" + "[+-]?(\\d+)?" + RegExpGenerator.slosh(decimalSeparator) + "\\d+(?:[eE]([-+]?\\d+))?";

		knownPatterns.put(PATTERN_BOOLEAN_TRUE_FALSE,
				new PatternInfo(ID.ID_BOOLEAN_TRUE_FALSE, PATTERN_BOOLEAN_TRUE_FALSE, PatternInfo.Type.BOOLEAN, "TRUE_FALSE", false, 4, 5, null, ""));
		knownPatterns.put(PATTERN_BOOLEAN_YES_NO,
				new PatternInfo(ID.ID_BOOLEAN_YES_NO, PATTERN_BOOLEAN_YES_NO, PatternInfo.Type.BOOLEAN, "YES_NO", false, 2, 3, null, ""));
		knownPatterns.put(PATTERN_BOOLEAN_ONE_ZERO,
				new PatternInfo(ID.ID_BOOLEAN_ONE_ZERO, PATTERN_BOOLEAN_ONE_ZERO, PatternInfo.Type.BOOLEAN, "ONE_ZERO", false, -1, -1, null, null));
		knownPatterns.put(PATTERN_ANY_VARIABLE,
				new PatternInfo(ID.ID_ANY_VARIABLE, PATTERN_ANY_VARIABLE, PatternInfo.Type.STRING, null, false, 1, -1, null, ""));
		knownPatterns.put(PATTERN_ALPHA_VARIABLE,
				new PatternInfo(ID.ID_ALPHA_VARIABLE, PATTERN_ALPHA_VARIABLE, PatternInfo.Type.STRING, null, false, 1, -1, null, ""));
		knownPatterns.put(PATTERN_ALPHANUMERIC_VARIABLE,
				new PatternInfo(ID.ID_ALPHANUMERIC_VARIABLE, PATTERN_ALPHANUMERIC_VARIABLE, PatternInfo.Type.STRING, null, false, 1, -1, null, ""));
		knownPatterns.put(PATTERN_LONG,
				new PatternInfo(ID.ID_LONG, PATTERN_LONG, PatternInfo.Type.LONG, null, false, 1, -1, null, ""));
		knownPatterns.put(PATTERN_SIGNED_LONG,
				new PatternInfo(ID.ID_SIGNED_LONG, PATTERN_SIGNED_LONG, PatternInfo.Type.LONG, "SIGNED", false, 1, -1, null, ""));
		knownPatterns.put(PATTERN_DOUBLE,
				new PatternInfo(ID.ID_DOUBLE, PATTERN_DOUBLE, PatternInfo.Type.DOUBLE, null, false, -1, -1, null, ""));
		knownPatterns.put(PATTERN_SIGNED_DOUBLE,
				new PatternInfo(ID.ID_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE, PatternInfo.Type.DOUBLE, "SIGNED", false, -1, -1, null, ""));
		knownPatterns.put(PATTERN_DOUBLE_WITH_EXPONENT,
				new PatternInfo(ID.ID_DOUBLE_WITH_EXPONENT, PATTERN_DOUBLE_WITH_EXPONENT, PatternInfo.Type.DOUBLE, null, false, -1, -1, null, ""));
		knownPatterns.put(PATTERN_SIGNED_DOUBLE_WITH_EXPONENT,
				new PatternInfo(ID.ID_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PatternInfo.Type.DOUBLE, "SIGNED", false, -1, -1, null, ""));

		knownPatterns.put(PATTERN_LONG_GROUPING,
				new PatternInfo(ID.ID_LONG_GROUPING, PATTERN_LONG_GROUPING, PatternInfo.Type.LONG, "GROUPING", false, 1, -1, null, ""));
		knownPatterns.put(PATTERN_SIGNED_LONG_GROUPING,
				new PatternInfo(ID.ID_SIGNED_LONG_GROUPING, PATTERN_SIGNED_LONG_GROUPING, PatternInfo.Type.LONG, "SIGNED,GROUPING", false, 1, -1, null, ""));

		knownPatterns.put(PATTERN_DOUBLE_GROUPING,
				new PatternInfo(ID.ID_DOUBLE_GROUPING, PATTERN_DOUBLE_GROUPING, PatternInfo.Type.DOUBLE, "GROUPING", false, 1, -1, null, ""));
		knownPatterns.put(PATTERN_SIGNED_DOUBLE_GROUPING,
				new PatternInfo(ID.ID_SIGNED_DOUBLE_GROUPING, PATTERN_SIGNED_DOUBLE_GROUPING, PatternInfo.Type.DOUBLE, "SIGNED,GROUPING", false, 1, -1, null, ""));

		knownPatterns.put(PATTERN_NULL,
				new PatternInfo(ID.ID_NULL, PATTERN_NULL, PatternInfo.Type.STRING, "NULL", false, -1, -1, null, ""));
		knownPatterns.put(PATTERN_WHITESPACE + "BLANKORNULL",
				new PatternInfo(ID.ID_BLANKORNULL, PATTERN_WHITESPACE, PatternInfo.Type.STRING, "BLANKORNULL", false, -1, -1, null, ""));
		knownPatterns.put(PATTERN_WHITESPACE + "BLANK",
				new PatternInfo(ID.ID_BLANK, PATTERN_WHITESPACE, PatternInfo.Type.STRING, "BLANK", false, -1, -1, null, ""));

		// Build the mapping from ID to PatternInfo
		for (PatternInfo patternInfo : knownPatterns.values()) {
			knownIDs.put(patternInfo.id, patternInfo);
		}

		addBinary(promotion, PATTERN_LONG, PATTERN_SIGNED_LONG, PATTERN_SIGNED_LONG);
		addBinary(promotion, PATTERN_LONG, PATTERN_DOUBLE, PATTERN_DOUBLE);
		addBinary(promotion, PATTERN_LONG, PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE);
		addBinary(promotion, PATTERN_LONG, PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_DOUBLE_WITH_EXPONENT);
		addBinary(promotion, PATTERN_LONG, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);

		addBinary(promotion, PATTERN_SIGNED_LONG, PATTERN_LONG, PATTERN_SIGNED_LONG);
		addBinary(promotion, PATTERN_SIGNED_LONG, PATTERN_DOUBLE, PATTERN_SIGNED_DOUBLE);
		addBinary(promotion, PATTERN_SIGNED_LONG, PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE);
		addBinary(promotion, PATTERN_SIGNED_LONG, PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
		addBinary(promotion, PATTERN_SIGNED_LONG, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);

		addBinary(promotion, PATTERN_DOUBLE, PATTERN_LONG, PATTERN_DOUBLE);
		addBinary(promotion, PATTERN_DOUBLE, PATTERN_SIGNED_LONG, PATTERN_SIGNED_DOUBLE);
		addBinary(promotion, PATTERN_DOUBLE, PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE);
		addBinary(promotion, PATTERN_DOUBLE, PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_DOUBLE_WITH_EXPONENT);
		addBinary(promotion, PATTERN_DOUBLE, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);

		addBinary(promotion, PATTERN_SIGNED_DOUBLE, PATTERN_LONG, PATTERN_SIGNED_DOUBLE);
		addBinary(promotion, PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_LONG, PATTERN_SIGNED_DOUBLE);
		addBinary(promotion, PATTERN_SIGNED_DOUBLE, PATTERN_DOUBLE, PATTERN_SIGNED_DOUBLE);
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

	void put(String key, PatternInfo patternInfo) {
		knownPatterns.put(key, patternInfo);
	}

	PatternInfo getByRegExp(String regExp) {
		return knownPatterns.get(regExp);
	}

	PatternInfo getByID(ID id) {
		return knownIDs.get(id);
	}

	PatternInfo numericPromotion(ID left, ID right) {
		return promotion.get(left.toString() + "---" + right.toString());
	}

	String numericPromotion(String leftPattern, String rightPattern) {
		PatternInfo result = promotion.get(leftPattern + "---" + rightPattern);
		return result == null ? null : result.regexp;
	}

	PatternInfo negation(String pattern) {
		return negation.get(pattern);
	}

	PatternInfo grouping(String pattern) {
		return grouping.get(pattern);
	}

	void addUnary(Map<String, PatternInfo> transformation, String input, String result) {
		transformation.put(input.toString(), knownPatterns.get(result));
	}

	void addBinary(Map<String, PatternInfo> transformation, String left, String right, String result) {
		transformation.put(left + "---" + right, knownPatterns.get(result));
	}
}
