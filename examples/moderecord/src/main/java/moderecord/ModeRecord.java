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
package moderecord;

import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.RecordAnalysisResult;
import com.cobber.fta.RecordAnalyzer;
import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;

public abstract class ModeRecord {
	public static void main(final String[] args) throws FTAException {
		final String[] headers = { "First", "Last", "MI" };
		final String[][] names = {
				{ "Anaïs", "Nin", "" }, { "Gertrude", "Stein", "" }, { "Paul", "Campbell", "" },
				{ "Pablo", "Picasso", "" }, { "Theodore", "Camp", "" }, { "Henri", "Matisse", "" },
				{ "Georges", "Braque", "" }, { "Ernest", "Hemingway", "" }, { "Alice", "Toklas", "B." },
				{ "Eleanor", "Roosevelt", "" }, { "Edgar", "Degas", "" }, { "Pierre-Auguste", "Wren", "" },
				{ "Claude", "Monet", "" }, { "Édouard", "Sorenson", "" }, { "Mary", "Dunning", "" },
				{ "Alfred", "Jones", "" }, { "Joseph", "Smith", "" }, { "Camille", "Pissarro", "" },
				{ "Franklin", "Roosevelt", "Delano" }, { "Winston", "Churchill", "" }
		};

		final AnalyzerContext context = new AnalyzerContext(null, DateResolutionMode.Auto, "customer", headers );
		final TextAnalyzer template = new TextAnalyzer(context);

		final RecordAnalyzer analysis = new RecordAnalyzer(template);
		for (final String [] name : names)
			analysis.train(name);

		final RecordAnalysisResult recordResult = analysis.getResult();

		for (final TextAnalysisResult result : recordResult.getStreamResults()) {
			System.err.printf("Semantic Type: %s (%s)%n", result.getSemanticType(), result.getType());
//			System.err.println("Detail: " + result.asJSON(true, 1));
		}
	}
}
