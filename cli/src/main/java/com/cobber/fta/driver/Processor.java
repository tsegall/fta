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

import java.io.IOException;
import java.io.PrintStream;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.LogicalType;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.RecordAnalyzer;
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

		if (options.pluginName != null && options.validate) {
			if (logicalType == null) {
				final PluginDefinition pluginDefinition = PluginDefinition.findByQualifier(options.pluginName);
				if (pluginDefinition == null) {
					logger.printf("ERROR: Failed to locate plugin named '%s', use --help%n", options.pluginName);
					System.exit(1);
				}

				logicalType = LogicalTypeFactory.newInstance(pluginDefinition, new AnalysisConfig(options.locale));
			}
		}

		if (options.col == -1) {
			TextAnalyzer template = new TextAnalyzer(new AnalyzerContext(null, options.resolutionMode, compositeName, fieldNames));
			options.apply(template);
			recordAnalyzer = new RecordAnalyzer(template);
			return;
		}

		analyzers = new TextAnalyzer[streamCount];
		for (int i = 0; i < fieldNames.length; i++) {
			if (options.col == -1 || options.col == i) {
				analyzers[i] = new TextAnalyzer(new AnalyzerContext(fieldNames[i] == null ? "" : fieldNames[i].trim(), options.resolutionMode, compositeName, fieldNames));
				options.apply(analyzers[i]);
			}
		}
	}

	public void consume(final String[] row) throws FTAPluginException, FTAUnsupportedLocaleException {
		if (options.col == -1) {
			recordAnalyzer.train(row);
			return;
		}

		for (int i = 0; i < streamCount; i++) {
			if (options.col == -1 || options.col == i) {
				if (options.verbose != 0 && options.noAnalysis)
					System.out.printf("\"%s\"%n", row[i]);
				if (options.pluginName != null && options.validate) {
					if (row[i] != null && !row[i].trim().isEmpty())
						System.out.printf("'%s': %b%n", row[i], logicalType.isValid(row[i], false));
				}
				else if (!options.noAnalysis)
					analyzers[i].train(row[i]);
			}
		}
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
