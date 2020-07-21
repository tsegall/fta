package com.cobber.fta.examples;

import java.util.Locale;

import com.cobber.fta.DateTimeParser;
import com.cobber.fta.DateTimeParser.DateResolutionMode;

public class DetermineDateFormat {

	public static void main(String[] args) {
		DateTimeParser dtp = new DateTimeParser(DateResolutionMode.MonthFirst, Locale.ENGLISH);

		System.err.println(dtp.determineFormatString("16 July 2012"));
		System.err.println(dtp.determineFormatString("March 20 2012"));
		System.err.println(dtp.determineFormatString("2012 March 20"));
	}
}
