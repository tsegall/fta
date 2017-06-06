package com.cobber.fta;

import java.util.HashMap;

public class DateTimeAnalysisResult {
	String format = null;
	int timeElements = -1;
	int dateElements = -1;
	Boolean timeFirst = null;
	Character dateTimeSeparator = null;
	int yearOffset = -1;
	int yearLength = -1;
	int monthOffset = -1;
	int dayOffset = -1;
	Character dateSeparator = null;

	static HashMap<String, DateTimeAnalysisResult> options = new HashMap<String, DateTimeAnalysisResult>();
	
	static {
		options.put("HH:MM", new DateTimeAnalysisResult(2));  
		options.put("HH:MM:SS", new DateTimeAnalysisResult(3));
		options.put("XX/XX/YY", new DateTimeAnalysisResult(3, '/', 2, 2, -1, -1));
		options.put("XX/XX/XX", new DateTimeAnalysisResult(3, '/', 2, -1, -1, -1));
		options.put("XX/DD/XX", new DateTimeAnalysisResult(3, '/', 2, -1, -1, 1));
		options.put("DD/MM/YY", new DateTimeAnalysisResult(3, '/', 2, 2, 1, 0));
		options.put("MM/DD/YY", new DateTimeAnalysisResult(3, '/', 2, 2, 0, 1));
		options.put("XX/XX/YYYY", new DateTimeAnalysisResult(3, '/', 4, 2, -1, -1));
		options.put("DD/MM/YYYY", new DateTimeAnalysisResult(3, '/', 4, 2, 1, 0));
		options.put("MM/DD/YYYY", new DateTimeAnalysisResult(3, '/', 4, 2, 0, 1));
		options.put("YYYY/MM/DD", new DateTimeAnalysisResult(3, '/', 4, 0, 1, 2));
	}
	
	DateTimeAnalysisResult(int timeElements) {
		this.timeElements = timeElements;
	}

	DateTimeAnalysisResult(int dateElements, Character dateSeparator, int yearLength, int yearOffset, int monthOffset, int dayOffset) {
		this.dateElements = dateElements;
		this.yearLength = yearLength;
		this.yearOffset = yearOffset;
		this.monthOffset = monthOffset;
		this.dayOffset = dayOffset;
		this.dateSeparator = dateSeparator;
	}

	DateTimeAnalysisResult(int timeElements, int dateElements, Boolean timeFirst, Character dateTimeSeparator, 
			int yearLength, int yearOffset, int monthOffset, int dayOffset, Character dateSeparator) {
		this.timeElements = timeElements;
		this.dateElements = dateElements;
		this.timeFirst = timeFirst;
		this.dateTimeSeparator = dateTimeSeparator;
		this.yearLength = yearLength;
		this.yearOffset = yearOffset;
		this.monthOffset = monthOffset;
		this.dayOffset = dayOffset;
		this.dateSeparator = dateSeparator;
	}
	
	 public static DateTimeAnalysisResult toResult(String dateTimeString) {
		return options.get(dateTimeString);
	}

	public String toString() {
		String timeAnswer = timeElements == 0 ? "" : (timeElements == 2 ? "HH:MM" : "HH:MM:SS");
		String year = yearLength == 4 ? "YYYY" : "YY";
		String dateAnswer = "";
		if (dateElements != 0) {
			if (yearOffset == 0) {
				if (dayOffset != -1) {
					if (dayOffset == 1)
						dateAnswer = year + "DD" + dateSeparator + "MM" + dateSeparator + year;
					else
						dateAnswer = year + "MM" + dateSeparator + "DD" + dateSeparator + year;
				}
				else
					dateAnswer += year + dateSeparator + "XX" + "XX";
			}
			if (yearOffset == 2) {
				if (dayOffset != -1) {
					if (dayOffset == 0)
						dateAnswer = "DD" + dateSeparator + "MM" + dateSeparator + year;
					else
						dateAnswer = "MM" + dateSeparator + "DD" + dateSeparator + year;
				}
				else
					dateAnswer = "XX" + dateSeparator + "XX" + dateSeparator + year;
			}
		}
		
		if (timeElements == -1)
			return dateAnswer;
		if (dateElements == -1)
			return timeAnswer;
		return timeFirst ? timeAnswer + " " + dateAnswer : dateAnswer + " " + timeAnswer; 
	}
}
