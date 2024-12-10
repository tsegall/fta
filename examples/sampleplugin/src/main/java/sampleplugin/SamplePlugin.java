/*
 * Copyright 2017-2024 Tim Segall
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

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;

public abstract class SamplePlugin {

	public static void main(final String[] args) throws IOException, FTAException {
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

		TextAnalysisResult result = analysis.getResult();

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
		final Locale locale = Locale.forLanguageTag("fr-FR");
		analysisFrench.setLocale(locale);
		addPlugin(analysisFrench);

		for (final String input : inputsFrench)
			analysisFrench.train(input);

		result = analysisFrench.getResult();

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

}
