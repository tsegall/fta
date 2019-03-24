package com.cobber.fta.examples;

import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;

public class Performance {

	public static void main(String args[]) {

		final TextAnalyzer analysis = new TextAnalyzer("DateOfBirth");

		// To maximize performance - disable default logical types and Statistics
		analysis.setDefaultLogicalTypes(false);
		analysis.setCollectStatistics(false);

		final String inputs[] = new String[] {
				"11/25/2010 11:13:38 AM",  "9/20/2010 7:31:26 AM", "9/17/2010 2:37:58 PM", "12/14/2010 11:08:17 AM",
				"10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM",
				"10/13/2010 1:17:04 PM","11/25/2010 11:13:38 AM", "11/25/2010 11:13:38 AM", "9/20/2010 7:31:26 AM",
				"9/17/2010 2:37:58 PM", "12/14/2010 11:08:17 AM", "10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM",
				"10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM", "9/20/2010 7:31:26 AM",
				"9/17/2010 2:37:58 PM", "12/14/2010 11:08:17 AM", "10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM",
				"10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM"
		};

		long start = System.currentTimeMillis();

		long iters;
		for (iters = 0; iters < 100_000; iters++)
			for (int i = 0; i < inputs.length; i++)
				analysis.train(inputs[i]);

		final TextAnalysisResult result = analysis.getResult();

		System.err.println("Result: " + result.asJSON(true, 1));

		long duration = System.currentTimeMillis() - start;

		System.err.printf("Count %d, duration: %dms, ~%d per second\n", iters * inputs.length, duration, (long)((iters * inputs.length) / ((double)duration/1000)));
	}
}
