package com.cobber.fta.examples;

import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;

public class Trivial {

	public static void main(String args[]) {

		final TextAnalyzer analysis = new TextAnalyzer("Age");

		analysis.train("12");
		analysis.train("62");
		analysis.train("21");
		analysis.train("37");

		final TextAnalysisResult result = analysis.getResult();

		System.err.printf("Result: %s, Regular Expression: %s, Max: %s, Min: %s.\n", result.getType(), result.getRegExp(), result.getMaxValue(), result.getMinValue());

		System.err.println("Detail: " + result.asJSON(true, 1));
	}
}
