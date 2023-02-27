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
package com.cobber.fta;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAUnsupportedLocaleException;

public class RecordAnalyzer {
	private TextAnalyzer[] analyzers;
	private int streamCount;

	/**
	 * Construct a Record Analyzer using the supplied template.
	 *
	 * @param template The TextAnalyzer to be used as a template.
	 */
	public RecordAnalyzer(final TextAnalyzer template) {
		streamCount = template.getContext().getCompositeStreamNames().length;
		analyzers = new TextAnalyzer[streamCount];

		for (int i = 0; i < streamCount; i++) {
			analyzers[i] = new TextAnalyzer(getStreamContext(template.getContext(), i));
			analyzers[i].setConfig(template.getConfig());
		}
	}

	private AnalyzerContext getStreamContext(final AnalyzerContext templateContext, final int streamIndex) {
		final String fieldName = templateContext.getCompositeStreamNames()[streamIndex];
		return  new AnalyzerContext(fieldName == null ? "" : fieldName.trim(), templateContext.getDateResolutionMode(), templateContext.getCompositeName(), templateContext.getCompositeStreamNames());
	}

	/**
	 * Train is the streaming entry point used to supply input to the Record Analyzer.
	 *
	 * @param rawInput
	 *            The raw input as a String array
	 * @return A boolean indicating if the resultant type is currently known for all Analyzers.
	 * @throws FTAPluginException Thrown when a registered plugin has detected an issue
	 * @throws FTAUnsupportedLocaleException Thrown when a requested locale is not supported
	 */
	public boolean train(final String[] rawInput) throws FTAPluginException, FTAUnsupportedLocaleException {
		if (rawInput.length != streamCount)
			throw new IllegalArgumentException("Size of training input must match number of stream names");
		boolean allTrained = true;
		for (int i = 0; i < rawInput.length; i++) {
			final boolean trained = analyzers[i].train(rawInput[i]);
			if (!trained)
				allTrained = false;
		}

		return allTrained;
	}

	/**
	 * Determine the result of the training complete to date. Typically invoked
	 * after all training is complete, but may be invoked at any stage.
	 *
	 * @return A RecordAnalysisResult with the analysis of any training completed.
	 * @throws FTAPluginException Thrown when a registered plugin has detected an issue
	 * @throws FTAUnsupportedLocaleException Thrown when a requested locale is not supported
	 */
	public RecordAnalysisResult getResult() throws FTAPluginException, FTAUnsupportedLocaleException {
		TextAnalysisResult[] results = new TextAnalysisResult[streamCount];

		// Build an array of the Semantic Types detected as a result of the analysis so far
		final String[] semanticTypes = new String[streamCount];
		for (int i = 0; i < streamCount; i++) {
			results[i] = analyzers[i].getResult();
			if (results[i].isSemanticType())
				semanticTypes[i] = results[i].getSemanticType();
		}

		// For any stream where we have not already determined a Semantic type - try again providing the overall Semantic Type context
		int pickups;
		do {
			pickups = 0;
			for (int i = 0; i < streamCount; i++) {
				if (!results[i].isSemanticType()) {
					// Update the Context with all the Semantic Type information we have calculated
					analyzers[i].setContext(analyzers[i].getContext().withSemanticTypes(semanticTypes));
					results[i] = reAnalyze(analyzers[i], results[i]);
					semanticTypes[i] = results[i].getSemanticType();
					if (results[i].isSemanticType())
						pickups++;
				}
			}
		} while (pickups != 0);

		// Now do Entity detection based on the Semantic Type analysis

		/*
		{
			"entity": "PERSON.FULL_NAME",
			"description": "Person's Full Name",
			"mandatoryTypes": { "NAME.FIRST", "NAME.LAST" },
			"optionalTypes": { "HONORIFIC_EN", "NAME.MIDDLE", "NAME.MIDDLE_INITIAL", "NAME.SUFFIX" },
		},
		{
			"entity": "PERSON.FULL_NAME",
			"description": "Person's Full Name",
			"mandatoryTypes": { "NAME.FIRST_LAST" },
			"optionalTypes": { "HONORIFIC_EN", "NAME.MIDDLE", "NAME.MIDDLE_INITIAL", "NAME.SUFFIX" },
		},
		{
			"entity": "PERSON.FULL_NAME",
			"description": "Person's Full Name",
			"mandatoryTypes": { "NAME.LAST_FIRST" },
			"optionalTypes": { "HONORIFIC_EN", "NAME.MIDDLE", "NAME.MIDDLE_INITIAL", "NAME.SUFFIX" },
		},
		*/

		// POSTAL_CODE.ZIP5_US, ADDRESS, CITY, STATE/PROVICE, COUNTRY, COUNTY [ADDRESS]
		// [PERSON] + DOB/Birth*Date -> PERSON.DOB
		// [PERSON] + GENDER -> PERSON.GENDER
		// [PERSON] + JOB_TITLE -> PERSON.JOB_TITLE
		// LocalDate(yyyy), MONTH.DIGITS, DAY.DIGITS -> Date


		return new RecordAnalysisResult(results);
	}

	private TextAnalysisResult reAnalyze(final TextAnalyzer analyzer, final TextAnalysisResult result) throws FTAPluginException, FTAUnsupportedLocaleException {
		// Now do the analysis using the bulk data with the addition of the Semantic Type information
		final TextAnalysisResult newResult = analyzer.reAnalyze((result.getFacts().synthesizeBulk()));

		return newResult.isSemanticType() ? newResult : result;
	}

	/**
	 * Get the TextAnalyzer associated with a particular stream.
	 *
	 * @param stream The index of the stream requested.
	 * @return The TextAnalyzer used to process this particular stream.
	 */
	public TextAnalyzer getAnalyzer(final int stream) {
		return analyzers[stream];
	}

	/**
	 * Get all the TextAnalyzers associated with this record.
	 *
	 * @return The array of TextAnalyzer's used to process the records.
	 */
	public TextAnalyzer[] getAnalyzers() {
		return analyzers;
	}
}
