/*
 * Copyright 2017-2018 Tim Segall
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
 *
 * Simple Driver to utilize the FTA framework.
 */
package com.cobber.fta;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;

import com.cobber.fta.DateTimeParser.DateResolutionMode;

class Driver {

	static DriverOptions options;

	public static void main(final String[] args) throws IOException {
		final PrintStream logger = System.err;

		options = new DriverOptions();
		int idx = 0;
		while (idx < args.length && args[idx].charAt(0) == '-') {
			if ("--charset".equals(args[idx]))
				options.charset = args[++idx];
			else if ("--col".equals(args[idx]))
				options.col = Integer.valueOf(args[++idx]);
			else if ("--dayFirst".equals(args[idx]))
				options.resolutionMode = DateResolutionMode.DayFirst;
			else if ("--delimiter".equals(args[idx])) {
				String delim = args[++idx];
				options.csvFormat = options.csvFormat.withDelimiter("\\t".equals(delim) ? '\t' : delim.charAt(0));
			}
			else if ("--help".equals(args[idx])) {
				logger.println("Usage: [--charset <charset>] [--col <n>] [--dayFirst] [--help] [--maxCardinality <n>] [--monthFirst] [--records <n>] [--samples <n>] [--verbose] file ...");
				logger.println(" --charset <charset> - Use the supplied <charset> to read the input files");
				logger.println(" --col <n> - Only analyze column <n>");
				logger.println(" --dayFirst - If dates are ambigous assume Day precedes Month");
				logger.println(" --delimiter - Delimiter to use - must be a single character");
				logger.println(" --locale <LocaleIdentifier> - Locale to use as opposed to default");
				logger.println(" --maxCardinality <n> - Set the Maximum Cardinality size");
				logger.println(" --monthFirst - If dates are ambigous assume Month precedes Day");
				logger.println(" --records <n> - The number of records to analyze");
				logger.println(" --samples <n> - Set the size of the sample window");
				logger.println(" --validate - Validate the result of the analysis by reprocessing file against results");
				logger.println(" --verbose - Output each record as it is processed");
				System.exit(0);
			}
			else if ("--locale".equals(args[idx]))
				options.locale = new Locale(args[++idx]);
			else if ("--maxCardinality".equals(args[idx]))
				options.maxCardinality = Integer.valueOf(args[++idx]);
			else if ("--monthFirst".equals(args[idx]))
				options.resolutionMode = DateResolutionMode.MonthFirst;
			else if ("--noStatistics".equals(args[idx]))
				options.noStatistics = true;
			else if ("--records".equals(args[idx]))
				options.recordsToAnalyze = Long.valueOf(args[++idx]);
			else if ("--samples".equals(args[idx]))
				options.sampleSize = Integer.valueOf(args[++idx]);
			else if ("--validate".equals(args[idx]))
				options.validate = true;
			else if ("--verbose".equals(args[idx]))
				options.verbose = true;
			else {
				logger.printf("Unrecognized option: '%s', use --help\n", args[idx]);
				System.exit(1);
			}
			idx++;
		}

		if (idx == args.length) {
			logger.printf("No file to process supplied, use --help\n");
			System.exit(1);
		}

		// Loop over all the file arguments
		while (idx < args.length) {
			String filename = args[idx++];

			FileProcessor fileProcessor = new FileProcessor(logger, filename, options);
			fileProcessor.process();
		}
	}
}
