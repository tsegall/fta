/*
 * Copyright 2017-2023 Tim Segall
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
package modestreaming;

import java.util.Locale;

import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;

public abstract class ModeStreaming {

	public static void main(final String[] args) throws FTAException {
		final String[] inputs = {
				"Anaïs Nin", "Gertrude Stein", "Paul Cézanne", "Pablo Picasso", "Theodore Roosevelt",
				"Henri Matisse", "Georges Braque", "Henri de Toulouse-Lautrec", "Ernest Hemingway",
				"Alice B. Toklas", "Eleanor Roosevelt", "Edgar Degas", "Pierre-Auguste Renoir",
				"Claude Monet", "Édouard Manet", "Mary Cassatt", "Alfred Sisley",
				"Camille Pissarro", "Franklin Delano Roosevelt", "Winston Churchill" };

		// Use simple constructor - for improved detection provide an AnalyzerContext (see Contextual example).
		final TextAnalyzer analysis = new TextAnalyzer("Famous");

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		if (result.getSemanticType() == null) {
			System.err.printf("Current locale is '%s' - which does not support Semantic Type: NAME.FIRST_LAST\n", Locale.getDefault());
		}
		else {
			System.err.printf("Semantic Type: %s (%s)%n", result.getSemanticType(), result.getType());
			System.err.println("Detail: " + result.asJSON(true, 1));
		}
	}
}
