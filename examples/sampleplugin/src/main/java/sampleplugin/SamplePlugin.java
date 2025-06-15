/*
 * Copyright 2017-2025 Tim Segall
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
package sampleplugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;

public abstract class SamplePlugin {

	public static void main(final String[] args) throws IOException, FTAException {
		TextAnalysisResult result = null;
		final String[] inputsEnglish = {
				"red",  "red", "blue", "pink", "black", "white", "orange", "purple",
				"grey", "green", "red", "mauve", "red", "brown", "silver", "gold",
				"peach", "olive", "lemon", "lilac", "beige", "red", "burgundy", "aquamarine",
				"red",  "red", "blue", "pink", "black", "white", "orange", "purple",
				"grey", "green", "red", "mauve", "red", "brown", "silver", "gold",
				"peach", "olive", "lemon", "lilac", "beige", "red", "burgundy", "aquamarine"
		};

		final TextAnalyzer analysis = new TextAnalyzer("Colors");
		addPlugin(analysis);

		for (final String input : inputsEnglish)
			analysis.train(input);

		result = analysis.getResult();

		System.err.printf("Result: %s, Semantic Type: %s, Regular Expression: %s, Max: %s, Min: %s.%n", result.getType(), result.getSemanticType(), result.getRegExp(), result.getMaxValue(), result.getMinValue());

		// Now check that it works in French
		final String[] inputsFrench = {
				"rouge",  "rouge", "BLEUE", "ROSE", "NOIRE", "BLANCHE", "orange", "MAUVE",
				"GRISE", "VERTE", "rouge", "mauve", "rouge", "MARRON", "ARGENT", "OR",
				"PÊCHE", "olive", "CITRON", "LILAS", "beige", "rouge", "BOURGOGNE",
				"rouge",  "rouge", "BLEUE", "ROSE", "NOIRE", "BLANCHE", "orange", "MAUVE",
				"GRISE", "VERTE", "rouge", "mauve", "rouge", "MARRON", "ARGENT", "OR",
				"PÊCHE", "olive", "CITRON", "LILAS", "beige", "rouge", "BOURGOGNE"
		};
		final TextAnalyzer analysisFrench = new TextAnalyzer("Colors");
		final Locale localeFR = Locale.forLanguageTag("fr-FR");
		analysisFrench.setLocale(localeFR);
		addPlugin(analysisFrench);

		for (final String input : inputsFrench)
			analysisFrench.train(input);

		result = analysisFrench.getResult();

		System.err.printf("Result: %s, Semantic Type: %s, Regular Expression: %s, Max: %s, Min: %s.%n", result.getType(), result.getSemanticType(), result.getRegExp(), result.getMaxValue(), result.getMinValue());

		final String[] inputsRE = {
				"2345:AQ", "5993:FG", "3898:WW", "5543:NH", "1992:WW", "4002:CS", "5982:KG", "1090:DD", "3030:XX", "1088:TR",
				"2547:DE", "6587:DS", "3215:QQ", "7745:VD", "4562:DD", "4582:SS", "2257:WE", "3578:HT", "4568:FB", "1587:SW",
				"4573:LF", "3574:SS", "8122:GK", "4523:EW", "7128:RT", "2548:RF", "6873:HH", "4837:NR", "2358:EE", "3731:HY"
			};

		final String[] indianStates = {
			"AP", "AR", "AS", "BR", "CG", "GA", "GJ", "HR", "HP", "JK",
			"KA", "KL", "MP", "MH", "MN", "ML", "MZ", "NL", "OR", "PB",
			"RJ", "SK", "TN", "TS", "TR", "UP", "UK", "WB", "AN", "AP",
			"AR", "AS", "BR", "CG", "CH", "DD", "DH", "DL", "GA", "GJ",
			"HP", "HR", "JH", "JK", "KA", "KL", "LD", "MH", "ML", "MN",
			"MP", "MZ", "NL", "OR", "PB", "PY", "RJ", "SK", "TN", "TR",
			"KA", "KL", "MP", "MH", "MN", "ML", "MZ", "NL", "OR", "PB",
			"HP", "HR", "JH", "JK", "KA", "KL", "LD", "MH", "ML", "MN",
			"RJ", "SK", "TN", "TS", "TR", "UP", "UK", "WB", "AN", "AP",
			"AR", "AS", "BR", "CG", "CH", "GA", "GJ", "HR", "HP", "JK",
			"UK", "UP", "WB"
		};

		// Load our new plugins from a file and test the new Regular Expression Semantic Type
		TextAnalyzer analyzerAugmented = new TextAnalyzer("ID");
		analyzerAugmented.setLocale(Locale.forLanguageTag("en-IN"));
		addPlugins(analyzerAugmented, "ID");

		for (final String input : inputsRE)
			analyzerAugmented.train(input);

		result = analyzerAugmented.getResult();

		System.err.printf("Result: %s, Semantic Type: %s, Regular Expression: %s, Max: %s, Min: %s.%n", result.getType(), result.getSemanticType(), result.getRegExp(), result.getMaxValue(), result.getMinValue());

		// Load our new plugins from a file and test the new List-based Semantic Type
		analyzerAugmented = new TextAnalyzer("State");
		analyzerAugmented.setLocale(Locale.forLanguageTag("en-IN"));
		addPlugins(analyzerAugmented, "State");
		for (final String input : indianStates)
			analyzerAugmented.train(input);

		result = analyzerAugmented.getResult();

		System.err.printf("Result: %s, Semantic Type: %s, Regular Expression: %s, Max: %s, Min: %s.%n", result.getType(), result.getSemanticType(), result.getRegExp(), result.getMaxValue(), result.getMinValue());
	}

    static void addPlugin(final TextAnalyzer analysis) {
		// Register our new magic plugin
		final String colorPlugin = "[ { \"semanticType\": \"CUSTOM_COLOR.TEXT_<LANG>\", \"pluginType\": \"java\", \"clazz\": \"sampleplugin.PluginColor\", \"validLocales\": [ { \"localeTag\": \"en,fr-FR\" } ] } ]";
		try {
			analysis.getPlugins().registerPlugins(new StringReader(colorPlugin), "color", analysis.getConfig());
		} catch (InvocationTargetException | ClassNotFoundException | NoSuchMethodException | InstantiationException |
				IllegalAccessException | IOException | FTAException e) {
			if (e.getCause() != null)
				System.err.println("ERROR: Failed to register plugin: " + e.getCause().getMessage());
			else
				System.err.println("ERROR: Failed to register plugin: " + e.getMessage());
			System.exit(1);
		}
    }

    static void addPlugins(final TextAnalyzer analysis, final String dataStreamName) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/CustomPlugins.json"), StandardCharsets.UTF_8))) {
			analysis.getPlugins().registerPlugins(reader, dataStreamName, analysis.getConfig());
		} catch (InvocationTargetException | ClassNotFoundException | NoSuchMethodException | InstantiationException |
				IllegalAccessException | IOException | FTAException e) {
			if (e.getCause() != null)
				System.err.println("ERROR: Failed to register plugin: " + e.getCause().getMessage());
			else
				System.err.println("ERROR: Failed to register plugin: " + e.getMessage());
			System.exit(1);
		}
    }
}
