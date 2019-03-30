package com.cobber.fta.examples;

import java.util.HashMap;

import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;

public class SampleBulk {
	public static void main(String args[]) {

		final TextAnalyzer analysis = new TextAnalyzer("Gender");
		HashMap<String, Long> basic = new HashMap<>();

		basic.put("Male", 2000000L);
		basic.put("Female", 1000000L);
		basic.put("Unknown", 10000L);

		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();

		System.err.printf("Result: %s, Regular Expression: %s, Max: %s, Min: %s.\n", result.getType(), result.getRegExp(), result.getMaxValue(), result.getMinValue());

		System.err.println("Detail: " + result.asJSON(true, 1));

	}
}
