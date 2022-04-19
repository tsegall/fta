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

import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAUnsupportedLocaleException;

public class Processor {
	private final TextAnalyzer[] analyzers;
	private final DriverOptions options;
	private final int streamCount;

	Processor(final String compositeName, final String[] fieldNames, final DriverOptions options) throws IOException {
		this.options = options;
		this.streamCount = fieldNames.length;

		analyzers = new TextAnalyzer[streamCount];

		for (int i = 0; i < fieldNames.length; i++) {
			if (options.col == -1 || options.col == i) {
				analyzers[i] = new TextAnalyzer(new AnalyzerContext(fieldNames[i], options.resolutionMode, compositeName, fieldNames));
				options.apply(analyzers[i]);
			}
		}
	}

	public void consume(final String[] row) throws FTAPluginException, FTAUnsupportedLocaleException {
		for (int i = 0; i < streamCount; i++) {
			if (options.col == -1 || options.col == i) {
				if (options.verbose != 0)
					System.out.printf("\"%s\"%n", row[i]);
				if (!options.noAnalysis) {
					analyzers[i].train(row[i]);
				}
			}
		}
	}

	public TextAnalyzer getAnalyzer(final int stream) {
		return analyzers[stream];
	}

	public TextAnalyzer[] getAnalyzers() {
		return analyzers;
	}
}
