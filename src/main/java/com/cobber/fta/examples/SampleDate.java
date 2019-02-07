package com.cobber.fta.examples;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.cobber.fta.DateTimeParser;
import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;

public class SampleDate {

	public static void main(String args[]) {

		final TextAnalyzer analysis = new TextAnalyzer("DateOfBirth");
		final String inputs[] = new String[] {
				"11/25/2010 11:13:38 AM",  "9/20/2010 7:31:26 AM", "9/17/2010 2:37:58 PM", "12/14/2010 11:08:17 AM",
				"10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM",
				"10/13/2010 1:17:04 PM","11/25/2010 11:13:38 AM", "11/25/2010 11:13:38 AM", "9/20/2010 7:31:26 AM",
				"9/17/2010 2:37:58 PM", "12/14/2010 11:08:17 AM", "10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM",
				"10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM", "9/20/2010 7:31:26 AM",
				"9/17/2010 2:37:58 PM", "12/14/2010 11:08:17 AM", "10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM",
				"10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM"
		};

		for (int i = 0; i < inputs.length; i++)
			analysis.train(inputs[i]);

		final TextAnalysisResult result = analysis.getResult();

		System.err.printf("Result: %s, Regular Expression: %s, Max: %s, Min: %s.\n", result.getType(), result.getRegExp(), result.getMaxValue(), result.getMinValue());

		System.err.println("Detail: " + result.asJSON(true, true));

		// Now prove it works!!
		// Grab the DateTimeFormatter from fta as this creates a case-insensitive parser and it supports a slightly wider set set of formats
		// For example, "yyyy" does not work out of the box if you use ofPattern
		//		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(result.getTypeQualifier(), Locale.getDefault());
		final DateTimeFormatter formatter = DateTimeParser.ofPattern(result.getTypeQualifier(), Locale.getDefault());

		for (int i = 0; i < inputs.length; i++)
			LocalDateTime.parse(inputs[i], formatter);
	}
}
