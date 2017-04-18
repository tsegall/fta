package com.cobber.fta;

public class PatternInfo {
	String pattern;
	String generalPattern;
	String format;
	String type;
	String typeQualifier;
	
	PatternInfo(String pattern, String generalPattern, String format, String type, String typeQualifier) {
		this.pattern = pattern;
		this.generalPattern = generalPattern;
		this.format = format;
		this.type = type;
		this.typeQualifier = typeQualifier;
	}
	
	boolean isNumeric() {
		return "Long".equals(this.type) || "Double".equals(this.type);
	}
}
