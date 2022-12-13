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

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.LogicalType;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;

/**
 * Simple class to demonstrate how to validate input based on the use of Semantic Types.
 *	1.	Example based on an a-priori known Semantic Type
 *	2.	Example based on a Semantic Type from training
 *	3.	Example based on input that is not a Semantic Type - so only have a Regular Expression
 */
public abstract class Validation {

	public static void main(final String[] args) throws IOException, FTAException {
		final PluginDefinition pluginDefinition = PluginDefinition.findByQualifier("EMAIL");
		final LogicalType knownSemanticType = LogicalTypeFactory.newInstance(pluginDefinition, new AnalysisConfig());

		// Make sure we like India and do not like Gondwana
		System.err.println("Is 'elease.campo@gmail.com' valid? " + knownSemanticType.isValid("elease.campo@gmail.com"));
		System.err.println("Is 'double@at@cobber.com' valid? " + knownSemanticType.isValid("double@at@cobber.com"));

		//
		// Example for Semantic Types ...
		//
		final String[] inputsLT = {
				"france",  "germany", "poland", "canada", "australia", "belgium", "china", "turkey",
				"new zealand", "pakistan", "colombia", "portugal", "spain", "estonia", "croatia", "tanzania"
		};

		final TextAnalyzer analysis = new TextAnalyzer("Country");
		analysis.setTrace("enabled=true");

		// Train the input
		for (final String input : inputsLT)
			analysis.train(input);

		// Grab the result of our training?
		final TextAnalysisResult resultCountry = analysis.getResult();

		// We want to do validation so check that we detected a Semantic Type
		if (!resultCountry.isSemanticType()) {
			System.err.println("Semantic Type not detected");
			System.exit(1);
		}

		// Grab the Semantic Type and check it is closed (e.g. a finite set, so for example Countries are good, First names are not)
		final LogicalType semanticType = analysis.getPlugins().getRegistered(resultCountry.getSemanticType());
		if (!semanticType.isClosed()) {
			System.err.println("Semantic Type not closed - hence can only use RegExp to validate");
			System.exit(1);
		}

		// Make sure we like India and do not like Gondwana
		System.err.println("Is 'India' valid? " + semanticType.isValid("India"));
		System.err.println("Is 'Gondwana' valid? " + semanticType.isValid("Gondwana"));

		//
		// Example where no Semantic Type detected
		//
		final String[] inputsRE = {
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

		// We want to do RegExp validation - so better not be a Semantic Type
		if (result.isSemanticType()) {
			System.err.println("Should not have detected as a Semantic Type");
			System.exit(1);
		}

		// Make sure we like India and do not like Gondwana
		System.err.println("Is 'DAILY' valid? " + "DAILY".matches(result.getRegExp()));
		System.err.println("Is 'FORTNIGHTLY' valid? " + "FORTNIGHTLY".matches(result.getRegExp()));
	}
}
