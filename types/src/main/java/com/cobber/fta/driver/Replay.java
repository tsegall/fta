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
package com.cobber.fta.driver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAUnsupportedLocaleException;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Replay {
	static class AnalysisConfigWrapper {
		public AnalysisConfig analysisConfig;
	}

	static class AnalyzerContextWrapper {
		public AnalyzerContext analyzerContext;
	}

	static class BulkEntry {
		public String value;
		public long count;
	}

	static class SamplesWrapper {
		public BulkEntry[] samplesBulk;
		public String[] samples;
	}

	/**
	 * Replay a previous FTA analysis using the saved Trace file.
	 * The file format consists of multiple JSON fragments.
	 * 1. AnalysisConfig (Always present)
	 * 2. AnalyzerContext (Always present)
	 * 3. The bulk Samples (Present assuming trainBulk was called)
	 * 4. The Samples (Present assuming train and/or trainBulk was called)
	 *
	 * @param replayFile The name of the FTA trace file.
	 * @param options The command line options (e.g. --verbose, --debug, ...)
	 */
	static void replay(String replayFile, DriverOptions options) {
		final ObjectMapper mapper = new ObjectMapper();
		try (final InputStream in = new FileInputStream(replayFile); JsonParser parser = new JsonFactory().createParser(in)) {
			final AnalysisConfigWrapper configWrapper = mapper.readValue(parser, AnalysisConfigWrapper.class);
			final AnalysisConfig analysisConfig = configWrapper.analysisConfig;
			final AnalyzerContextWrapper contextWrapper = mapper.readValue(parser, AnalyzerContextWrapper.class);
			final AnalyzerContext analyzerContext = contextWrapper.analyzerContext;

			// If train is never called then we may have no samples (if so call it a day)
			SamplesWrapper samplesWrapper = null;
			try {
				samplesWrapper = mapper.readValue(parser, SamplesWrapper.class);
			} catch (IOException e) {
				System.err.printf("NO samples present in input file (%s).%n", replayFile);
				System.exit(1);
			}
			final boolean bulkMode = samplesWrapper.samplesBulk != null;

			// Create a TextAnalyzer using the Context retrieved from the Trace file
			TextAnalyzer analyzer = new TextAnalyzer(analyzerContext);

			// Apply the Config we retrieved from the Trace file
			analyzer.setCollectStatistics(analysisConfig.collectStatistics);
			analyzer.setDefaultLogicalTypes(analysisConfig.enableDefaultLogicalTypes);
			analyzer.setDetectWindow(analysisConfig.detectWindow);
			analyzer.setMaxCardinality(analysisConfig.maxCardinality);
			analyzer.setMaxInputLength(analysisConfig.maxInputLength);
			analyzer.setMaxOutliers(analysisConfig.maxOutliers);
			analyzer.setPluginThreshold(analysisConfig.threshold);
			analyzer.setDetectWindow(analysisConfig.detectWindow);
			analyzer.setDebug(options.debug);
			if (analysisConfig.localeTag != null)
				analyzer.setLocale(Locale.forLanguageTag(analysisConfig.localeTag));

			TextAnalysisResult result = bulkMode ?
				processBulk(analyzer, samplesWrapper.samplesBulk, options) :
				process(analyzer, samplesWrapper.samples, options);

			if (result != null)
				System.err.printf("Field '%s' - %s%n", analyzer.getStreamName(), result.asJSON(options.pretty, options.verbose));
		} catch (FileNotFoundException e) {
			System.err.printf("ERROR: File '%s' - Not found.%n", replayFile);
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	static TextAnalysisResult processBulk(TextAnalyzer analyzer, BulkEntry[] samples, DriverOptions options) {
		final Map<String, Long> observed = new HashMap<>();
		for (BulkEntry sample : samples)
			observed.put(sample.value, sample.count);

		try {
			analyzer.trainBulk(observed);
			return analyzer.getResult();
		} catch (FTAPluginException | FTAUnsupportedLocaleException e) {
			e.printStackTrace();
			return null;
		}
	}

	static TextAnalysisResult process(TextAnalyzer analyzer, String[] samples, DriverOptions options) {
		try {
			for (String sample : samples)
				analyzer.train(sample);
			return analyzer.getResult();
		} catch (FTAPluginException | FTAUnsupportedLocaleException e) {
			e.printStackTrace();
			return null;
		}
	}
}
