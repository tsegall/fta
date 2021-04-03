/*
 * Copyright 2017-2021 Tim Segall
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cobber.fta.examples;

import java.io.IOException;
import java.io.StringReader;

import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;

public class SamplePlugin {

	public static void main(final String[] args) throws IOException {
		final String[] inputs = new String[] {
				"red",  "red", "blue", "pink", "black", "white", "orange", "purple",
				"grey", "green", "red", "mauve", "red", "brown", "silver", "gold",
				"peach", "olive", "lemon", "lilac", "beige", "red", "burgundy", "aquamarine",
				"red",  "red", "blue", "pink", "black", "white", "orange", "purple",
				"grey", "green", "red", "mauve", "red", "brown", "silver", "gold",
				"peach", "olive", "lemon", "lilac", "beige", "red", "burgundy", "aquamarine"
		};

		final TextAnalyzer analysis = new TextAnalyzer("Colors");

		// Register our new magic plugin
		final String colorPlugin = "[ { \"qualifier\": \"COLOR.TEXT_EN\", \"type\": \"finite\", \"clazz\": \"com.cobber.fta.examples.PluginColor\", \"locale\": [ ] } ]";
		try {
			analysis.getPlugins().registerPlugins(new StringReader(colorPlugin), "color", null);
		} catch (Exception e) {
			System.err.println("Failed to register plugin: " + e.getMessage());
			System.exit(1);
		}

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		System.err.printf("Result: %s, Regular Expression: %s, Max: %s, Min: %s.%n", result.getType(), result.getRegExp(), result.getMaxValue(), result.getMinValue());

		System.err.println("Detail: " + result.asJSON(true, 1));
	}
}
