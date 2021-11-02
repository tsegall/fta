package com.cobber.fta.driver;

import java.io.IOException;

import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.TextAnalysisResult;
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

	public void consume(String[] row) throws FTAPluginException, FTAUnsupportedLocaleException {
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

	public TextAnalysisResult getResult(int stream) throws FTAPluginException, FTAUnsupportedLocaleException {
		return analyzers[stream].getResult();
	}
}
