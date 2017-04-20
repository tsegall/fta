package com.cobber.fta;

/**
 * The PatternInfo class maintains a set of information about a simple pattern.  This is used
 * to derive a Type from a pattern.  For example,
 * 		new PatternInfo("(?i)true|false", null, "", "Boolean", null)
 * indicates that a case insensitive match for true or false indicates a boolean type ("Boolean").
 */
public class PatternInfo {
	String pattern;
	String generalPattern;
	String format;
	String type;
	String typeQualifier;
	
	/**
	 * Construct a new information block for the supplied pattern.
	 * @param pattern The pattern of interest.
	 * @param generalPattern The general case of this pattern (optional).
	 * @param format The Java format specified for a date pattern (optional). 
	 * @param type The type of the pattern.
	 * @param typeQualifier The type qualifier of the pattern (optional).
	 */
	public PatternInfo(String pattern, String generalPattern, String format, String type, String typeQualifier) {
		this.pattern = pattern;
		this.generalPattern = generalPattern;
		this.format = format;
		this.type = type;
		this.typeQualifier = typeQualifier;
	}
	
	
	/**
	 * Is this pattern Numeric?
	 * @return A boolean indicating if the Type for this pattern is numeric.
	 */
	public boolean isNumeric() {
		return "Long".equals(this.type) || "Double".equals(this.type);
	}
}
