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
package com.cobber.fta.examples;

import java.io.IOException;
import java.util.Locale;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.LogicalType;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAUnsupportedLocaleException;

/**
 * Simple class to demonstrate how to validate input based on the use of Semantic Types.
 *	1.	Validate based on an a-priori known Semantic Type
 *	2.	Validate based on a Semantic Type from training
 *	3.	Validate based on input that is not a Semantic Type - so only have a Regular Expression
 *	4.	Validate based on input where the Semantic Type has a Locale component
 */
public abstract class Validation {
	/*
	 * Validation based on an a-priori known Semantic Type
	 */
	public static void validateKnown() throws FTAPluginException {
		final PluginDefinition pluginDefinition = PluginDefinition.findByQualifier("EMAIL");
		final LogicalType knownSemanticType = LogicalTypeFactory.newInstance(pluginDefinition, new AnalysisConfig());

		// Make sure we can successfully validate a good and bad email
		System.err.println("Is 'elease.campo@gmail.com' valid? " + knownSemanticType.isValid("elease.campo@gmail.com"));
		System.err.println("Is 'double@at@cobber.com' valid? " + knownSemanticType.isValid("double@at@cobber.com"));
	}

	/*
	 * Validation based on a Semantic Type from training
	 */
	public static void validateFromTraining() throws FTAPluginException, FTAUnsupportedLocaleException {
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
	}

	public static void validateNoSemanticType() throws FTAPluginException, FTAUnsupportedLocaleException {
		/*
		 * Validation based on input that is not a Semantic Type - so only have a Regular Expression
		 */
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

		// Make sure we like DAILY and do not like FORTNIGHTLY
		System.err.println("Is 'DAILY' valid? " + "DAILY".matches(result.getRegExp()));
		System.err.println("Is 'FORTNIGHTLY' valid? " + "FORTNIGHTLY".matches(result.getRegExp()));
	}

	/*
	 * Validation based on input where the Semantic Type has a Locale component
	 */
	public static void validateSemanticTypeWithLocale() throws FTAPluginException {
		final PluginDefinition pluginDefinition = PluginDefinition.findByQualifier("DAY.ABBR_<LOCALE>");
		final LogicalType knownSemanticType = LogicalTypeFactory.newInstance(pluginDefinition, new AnalysisConfig(Locale.forLanguageTag("en-US")));

		// Make sure we can successfully validate a good and bad Day abbreviation
		System.err.println("Is 'Mon' valid? " + knownSemanticType.isValid("Mon"));
		System.err.println("Is 'Fre' valid? " + knownSemanticType.isValid("Fre"));
	}

	public static void main(final String[] args) throws IOException, FTAException {
		validateKnown();
		validateFromTraining();
		validateNoSemanticType();
		validateSemanticTypeWithLocale();
	}
}
