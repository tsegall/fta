/*
 * Copyright 2017-2022 Tim Segall
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
import java.lang.reflect.InvocationTargetException;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;

public abstract class SamplePlugin {

	public static void main(final String[] args) throws IOException, FTAException {
		final String[] inputs = {
				"red",  "red", "blue", "pink", "black", "white", "orange", "purple",
				"grey", "green", "red", "mauve", "red", "brown", "silver", "gold",
				"peach", "olive", "lemon", "lilac", "beige", "red", "burgundy", "aquamarine",
				"red",  "red", "blue", "pink", "black", "white", "orange", "purple",
				"grey", "green", "red", "mauve", "red", "brown", "silver", "gold",
				"peach", "olive", "lemon", "lilac", "beige", "red", "burgundy", "aquamarine"
		};

		final TextAnalyzer analysis = new TextAnalyzer("Colors");

		// Register our new magic plugin
		final String colorPlugin = "[ { \"semanticType\": \"CUSTOM_COLOR.TEXT_<LANG>\", \"pluginType\": \"java\", \"clazz\": \"com.cobber.fta.examples.PluginColor\", \"validLocales\": [ { \"localeTag\": \"en,fr-FR\" } ] } ]";
		try {
			analysis.getPlugins().registerPlugins(new StringReader(colorPlugin), "color", new AnalysisConfig());
		} catch (InvocationTargetException e) {
			System.err.println("ERROR: Failed to register plugin: " + e.getCause().getMessage());
			System.exit(1);
		} catch (Exception e) {
			System.err.println("ERROR: Failed to register plugin: " + e.getMessage());
			System.exit(1);
		}

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		System.err.printf("Result: %s, Regular Expression: %s, Max: %s, Min: %s.%n", result.getType(), result.getRegExp(), result.getMaxValue(), result.getMinValue());

		System.err.println("Detail: " + result.asJSON(true, 1));
	}
}
