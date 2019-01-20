package com.cobber.fta;

public class SimpleFacts {
	private final String matcher;
	private final String format;
	public PatternInfo.Type type;

	SimpleFacts(final String matcher, String format, PatternInfo.Type type) {
		this.matcher = matcher;
		this.format = format;
		this.type = type;
	}

	String getMatcher() {
		return matcher;
	}

	String getFormat() {
		return format;
	}
}
