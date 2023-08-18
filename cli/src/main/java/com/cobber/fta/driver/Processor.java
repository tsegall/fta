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
package com.cobber.fta.driver;

import java.io.IOException;
import java.io.PrintStream;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.LogicalType;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.RecordAnalyzer;
import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAUnsupportedLocaleException;

public class Processor {
	private TextAnalyzer[] analyzers;
	private RecordAnalyzer recordAnalyzer;
	private final DriverOptions options;
	private final int streamCount;
	private LogicalType logicalType;
	private final PrintStream logger = System.err;

	Processor(final String compositeName, final String[] fieldNames, final DriverOptions options) throws IOException, FTAPluginException {
		this.options = options;
		this.streamCount = fieldNames.length;

		if (options.pluginName != null && options.pluginMode != null) {
			if (logicalType == null) {
				final PluginDefinition pluginDefinition = PluginDefinition.findByName(options.pluginName);
				if (pluginDefinition == null) {
					logger.printf("ERROR: Failed to locate plugin named '%s', use --help%n", options.pluginName);
					System.exit(1);
				}

				logicalType = LogicalTypeFactory.newInstance(pluginDefinition, new AnalysisConfig(options.locale));
			}
		}

		if (options.col == -1) {
			final AnalyzerContext context = new AnalyzerContext(null, options.resolutionMode, compositeName, fieldNames);
			if (options.knownTypes != null)
				context.withSemanticTypes(options.knownTypes.split(","));
			final TextAnalyzer template = new TextAnalyzer(context);
			recordAnalyzer = new RecordAnalyzer(template);
			for (final TextAnalyzer analyzer : recordAnalyzer.getAnalyzers())
				options.apply(analyzer);
			return;
		}

		analyzers = new TextAnalyzer[streamCount];
		for (int i = 0; i < fieldNames.length; i++) {
			if (options.col == -1 || options.col == i) {
				final AnalyzerContext context = new AnalyzerContext(fieldNames[i] == null ? "" : fieldNames[i].trim(), options.resolutionMode, compositeName, fieldNames);
				if (options.knownTypes != null)
					context.withSemanticTypes(options.knownTypes.split(","));
				analyzers[i] = new TextAnalyzer(context);
				options.apply(analyzers[i]);
			}
		}
	}

	public void consume(final String[] row) throws FTAPluginException, FTAUnsupportedLocaleException {
		if (options.col == -1) {
			recordAnalyzer.train(row);
			return;
		}

		if (options.verbose != 0 && options.noAnalysis)
			System.out.printf("\"%s\"%n", row[options.col]);
		if (options.pluginName != null && options.pluginMode != null) {
			if (row[options.col] != null && !row[options.col].trim().isEmpty())
				System.out.printf("'%s': %b%n", row[options.col], logicalType.isValid(row[options.col], options.pluginMode, 0));
		}
		else if (!options.noAnalysis)
			analyzers[options.col].train(row[options.col]);
	}

	public TextAnalysisResult[] getResult() throws FTAPluginException, FTAUnsupportedLocaleException {
		if (options.col == -1)
			return recordAnalyzer.getResult().getStreamResults();

		final TextAnalysisResult[] results = new TextAnalysisResult[streamCount];

		results[options.col] = getAnalyzer(options.col).getResult();

		return results;
	}

	public TextAnalyzer getAnalyzer(final int stream) {
		if (options.col == -1)
			return recordAnalyzer.getAnalyzer(stream);

		return analyzers[stream];
	}

	public TextAnalyzer[] getAnalyzers() {
		if (options.col == -1)
			return recordAnalyzer.getAnalyzers();

		return analyzers;
	}
}
