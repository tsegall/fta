package com.cobber.fta.examples;

import java.util.Locale;

import com.cobber.fta.dates.DateTimeParser;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;

public class DetermineDateFormat {

	public static void main(String[] args) {
		DateTimeParser dtp = new DateTimeParser(DateResolutionMode.MonthFirst, Locale.ENGLISH);

		System.err.println(dtp.determineFormatString("26 July 2012"));
		System.err.println(dtp.determineFormatString("March 9 2012"));
		System.err.println(dtp.determineFormatString("2012 March 20"));
		System.err.println(dtp.determineFormatString("2012/04/09 18:24:12"));
	}
}
