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
package serialize;

import java.util.Locale;
import java.util.Random;

import com.cobber.fta.LogicalType;
import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;

public abstract class Serialize {
	public static void main(final String[] args) throws FTAException {

		final TextAnalyzer analysis = new TextAnalyzer("Gender");
		analysis.setLocale(Locale.US);

		final String[] options = { "Male", "Female", "Unknown" };
		final Random r = new Random();

		String serialized = analysis.serialize();
		for (int i = 0; i < 10000; i++) {
			final TextAnalyzer t = TextAnalyzer.deserialize(serialized);
			t.train(options[r.nextInt(options.length)]);
			serialized = t.serialize();
		}

		final TextAnalyzer finalAnalysis = TextAnalyzer.deserialize(serialized);
		final TextAnalysisResult result = finalAnalysis.getResult();

		System.err.printf("Semantic Type: %s (%s)%n", result.getSemanticType(), result.getType());
		System.err.println("Detail: " + result.asJSON(true, 1));

		// Given the Semantic Type we retrieve the associated plugin
		final LogicalType semanticType = finalAnalysis.getPlugins().getRegistered(result.getSemanticType());

		// Use the plugin to get the non-localized description
		System.err.printf("Description: %s%n", semanticType.getDescription());

		// Use the plugin to generate a new random sample
		System.err.printf("Sample: %s%n", semanticType.nextRandom());

		// Use the plugin to validate a value
		System.err.println("Is 'BLUE' valid? " + semanticType.isValid("BLUE"));
	}
}
