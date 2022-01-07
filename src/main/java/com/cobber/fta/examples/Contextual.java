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

import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;

public abstract class Contextual {

	public static void main(final String[] args) throws FTAException {

		// Providing a broader context and not just the column name will improve detection in a set of use cases
		AnalyzerContext context = new AnalyzerContext("MI", DateResolutionMode.None, "ClientDetails", new String[] { "First", "MI", "Last" });

		final TextAnalyzer analysis = new TextAnalyzer(context);

		analysis.train("W");
		analysis.train("B");
		analysis.train("D");
		analysis.train("R");
		analysis.train("E");
		analysis.train("f");

		final TextAnalysisResult result = analysis.getResult();

		System.err.printf("Result: %s, Regular Expression: %s, Max: %s, Min: %s.%n", result.getType(), result.getRegExp(), result.getMaxValue(), result.getMinValue());

		System.err.println("Detail: " + result.asJSON(true, 1));
	}
}
