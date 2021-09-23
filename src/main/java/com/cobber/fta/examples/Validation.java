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

import com.cobber.fta.LogicalType;
import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;

public abstract class Validation {

	public static void main(final String[] args) throws IOException, FTAException {
		//
		// Example for Logical Types ...
		//
		final String[] inputsLT = new String[] {
				"france",  "germany", "poland", "canada", "australia", "belgium", "china", "turkey",
				"new zealand", "pakistan", "colombia", "portugal", "spain", "estonia", "croatia", "tanzania"
		};

		final TextAnalyzer analysisLT = new TextAnalyzer("Country");

		// Train the input
		for (final String input : inputsLT)
			analysisLT.train(input);

		// Grab the result of our training?
		final TextAnalysisResult resultLT = analysisLT.getResult();

		// We want to do validation so check that this we detected a Logical Type
		if (!resultLT.isLogicalType()) {
			System.err.println("Logical Type not detected");
			System.exit(1);
		}

		// Grab the Logical Type and check it is closed (e.g. a finite set, so for example Countries are good, First names are not)
		LogicalType logicalType = analysisLT.getPlugins().getRegistered(resultLT.getTypeQualifier());
		if (!logicalType.isClosed()) {
			System.err.println("Logical Type not closed - hence can only use RegExp to validate");
			System.exit(1);
		}

		// Make sure we like India and do not like Gondwana
		System.err.println("Is 'India' valid? " + logicalType.isValid("India"));
		System.err.println("Is 'Gondwana' valid? " + logicalType.isValid("Gondwana"));

		//
		// Example where no Logical Type detected
		//
		final String[] inputsRE = new String[] {
				"DAILY",  "WEEKLY", "MONTHLY", "ANUALLY", "BIANNNUALLY", "QUARTERLY", "DAILY", "MONTHLY",
				"MONTHLY", "WEEKLY", "WEEKLY", "QUARTERLY", "WEEKLY", "DAILY", "croatia", "QUARTERLY",
				"DAILY",  "WEEKLY", "MONTHLY", "ANUALLY", "BIANNNUALLY", "QUARTERLY", "DAILY", "MONTHLY",
				"MONTHLY", "WEEKLY", "WEEKLY", "QUARTERLY", "WEEKLY", "DAILY", "croatia", "QUARTERLY"
		};

		final TextAnalyzer analysisRE = new TextAnalyzer("Frequency");

		// Train the input
		for (final String input : inputsRE)
			analysisRE.train(input);

		// Grab the result of our training?
		final TextAnalysisResult result = analysisRE.getResult();

		// We want to do RegExp validation - so better not be a Logical Type
		if (result.isLogicalType()) {
			System.err.println("Should not have detected as a Logical Type");
			System.exit(1);
		}

		// Make sure we like India and do not like Gondwana
		System.err.println("Is 'DAILY' valid? " + "DAILY".matches(result.getRegExp()));
		System.err.println("Is 'FORTNIGHTLY' valid? " + "FORTNIGHTLY".matches(result.getRegExp()));
	}
}
