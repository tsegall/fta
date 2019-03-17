package com.cobber.fta.examples;

import java.io.IOException;
import java.io.StringReader;

import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;

public class SamplePlugin {

	public static void main(String args[]) throws IOException {
		final String inputs[] = new String[] {
				"red",  "red", "blue", "pink", "black", "white", "orange", "purple",
				"grey", "green", "red", "mauve", "red", "brown", "silver", "gold",
				"peach", "olive", "lemon", "lilac", "beige", "red", "burgundy", "aquamarine",
				"red",  "red", "blue", "pink", "black", "white", "orange", "purple",
				"grey", "green", "red", "mauve", "red", "brown", "silver", "gold",
				"peach", "olive", "lemon", "lilac", "beige", "red", "burgundy", "aquamarine"
		};

		final TextAnalyzer analysis = new TextAnalyzer("Colors");

		// Register our new magic plugin
		String colorPlugin = "[ { \"qualifier\": \"COLOR.TEXT_EN\", \"type\": \"finite\", \"clazz\": \"com.cobber.fta.examples.PluginColor\", \"locale\": [ ] } ]";
		analysis.getPlugins().registerPlugins(new StringReader(colorPlugin), "color", null);


		for (int i = 0; i < inputs.length; i++)
			analysis.train(inputs[i]);

		final TextAnalysisResult result = analysis.getResult();

		System.err.printf("Result: %s, Regular Expression: %s, Max: %s, Min: %s.\n", result.getType(), result.getRegExp(), result.getMaxValue(), result.getMinValue());

		System.err.println("Detail: " + result.asJSON(true, true));
	}
}
