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
package com.cobber.fta.driver;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import com.cobber.fta.DateTimeParser.DateResolutionMode;
import com.cobber.fta.LogicalType;
import com.cobber.fta.LogicalTypeFinite;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.LogicalTypeRegExp;
import com.cobber.fta.TextAnalyzer;

class Driver {

	static DriverOptions options;

	public static void main(final String[] args) throws IOException {
		final PrintStream logger = System.err;
		boolean helpRequested = false;

		options = new DriverOptions();
		int idx = 0;
		while (idx < args.length && args[idx].charAt(0) == '-') {
			if ("--bulk".equals(args[idx]))
				options.bulk = true;
			else if ("--charset".equals(args[idx]))
				options.charset = args[++idx];
			else if ("--col".equals(args[idx]))
				options.col = Integer.valueOf(args[++idx]);
			else if ("--debug".equals(args[idx]))
				options.debug = Integer.valueOf(args[++idx]);
			else if ("--detectWindow".equals(args[idx]))
				options.detectWindow = Integer.valueOf(args[++idx]);
			else if ("--help".equals(args[idx])) {
				logger.println("Usage: [--charset <charset>] [--col <n>] [--help] [--locale <LocaleIdentifier>] [--maxCardinality <n>] [--noAnalysis] [--noLogicalTypes] [--noStatistics] [--pretty] [--records <n>] [--resolutionMode <DayFirst|MonthFirst|Auto|None>] [--samples <n>] [--validate] [--verbose] [--xMaxCharPerColumn <n>] file ...");
				logger.println(" --charset <charset> - Use the supplied <charset> to read the input files");
				logger.println(" --col <n> - Only analyze column <n>");
				logger.println(" --help - Print this help");
				logger.println(" --locale <LocaleIdentifier> - Locale to use as opposed to default");
				logger.println(" --logicalType <JSON representation of Logical Types> - Can be inline or as a File");
				logger.println(" --maxCardinality <n> - Set the size of the Maximum Cardinality set supported");
				logger.println(" --maxOutlierCardinality <n> - Set the size of the Maximum Outlier Cardinality set supported");
				logger.println(" --noAnalysis - Do not do analysis");
				logger.println(" --noLogicalTypes - Do not register any Logical Types");
				logger.println(" --noStatistics - Do not track statistics");
				logger.println(" --pluginThreshold <n> - Set the plugin threshold percentage (0-100) for detection");
				logger.println(" --pretty - Pretty print analysis");
				logger.println(" --records <n> - The number of records to analyze");
				logger.println(" --resolutionMode <DayFirst|MonthFirst|Auto|None> - Auto DayFirst or MonthFirst is determined from Locale");
				logger.println(" --samples <n> - Set the size of the sample window");
				logger.println(" --threshold <n> - Set the threshold percentage (0-100) for detection");
				logger.println(" --validate - Validate the result of the analysis by reprocessing file against results");
				logger.println(" --verbose - Output each record as it is processed");
				logger.println(" --xMaxCharsPerColumn <n> - Set the maximum column width (CSV parsing option)");
				helpRequested = true;

			}
			else if ("--locale".equals(args[idx]))
				options.locale = Locale.forLanguageTag(args[++idx]);
			else if ("--maxCardinality".equals(args[idx]))
				options.maxCardinality = Integer.valueOf(args[++idx]);
			else if ("--maxOutlierCardinality".equals(args[idx]))
				options.maxOutlierCardinality = Integer.valueOf(args[++idx]);
			else if ("--noAnalysis".equals(args[idx]))
				options.noAnalysis = true;
			else if ("--noLogicalTypes".equals(args[idx]))
				options.noLogicalTypes = true;
			else if ("--noStatistics".equals(args[idx]))
				options.noStatistics = true;
			else if ("--pluginThreshold".equals(args[idx]))
				options.pluginThreshold = Integer.valueOf(args[++idx]);
			else if ("--pretty".equals(args[idx]))
				options.pretty = true;
			else if ("--records".equals(args[idx]))
				options.recordsToAnalyze = Long.valueOf(args[++idx]);
			else if ("--logicalType".equals(args[idx]))
				options.logicalTypes = args[++idx];
			else if ("--resolutionMode".equals(args[idx])) {
				String mode = args[++idx];
				if (mode.equals("DayFirst"))
					options.resolutionMode = DateResolutionMode.DayFirst;
				else if (mode.equals("MonthFirst"))
					options.resolutionMode = DateResolutionMode.MonthFirst;
				else if (mode.equals("Auto"))
					options.resolutionMode = DateResolutionMode.Auto;
				else if (mode.equals("None"))
					options.resolutionMode = DateResolutionMode.None;
				else {
					logger.printf("Unrecognized argument: '%s', expected Dayfirst or MonthFirst or Auto or None\n", mode);
					System.exit(1);
				}
			}
			else if ("--threshold".equals(args[idx]))
				options.threshold = Integer.valueOf(args[++idx]);
			else if ("--validate".equals(args[idx]))
				options.validate = true;
			else if ("--verbose".equals(args[idx]))
				options.verbose++;
			else if ("--xMaxCharsPerColumn".equals(args[idx]))
				options.xMaxCharsPerColumn = Integer.valueOf(args[++idx]);
			else {
				logger.printf("Unrecognized option: '%s', use --help\n", args[idx]);
				System.exit(1);
			}
			idx++;
		}

		if (helpRequested) {
			// Create a dummy Analyzer to retrieve the Logical Types
			TextAnalyzer analysis = new TextAnalyzer("*");
			if (options.locale != null)
				analysis.setLocale(options.locale);

			// Need to start training to force registration of Logical Types
			analysis.train("Hello");

			// Grab the registered plugins and sort by Qualifier (magically will be all - since passed in '*')
			Collection<LogicalType> registered = analysis.getPlugins().getRegisteredLogicalTypes();
			Set<String> qualifiers = new TreeSet<>();
			for (LogicalType logical : registered)
				qualifiers.add(logical.getQualifier());

			if (registered.size() != 0) {
				logger.println("\nRegistered Logical Types:");
				for (String qualifier : qualifiers) {
					LogicalType logical = analysis.getPlugins().getRegistered(qualifier);
					if (logical instanceof LogicalTypeFinite) {
						LogicalTypeFinite finite = (LogicalTypeFinite)logical;
						logger.printf("\t%s (Finite): Priority: %d, Cardinality: %d, MaxLength: %d, MinLength: %d\n",
								logical.getQualifier(), logical.getPriority(), finite.getSize(), finite.getMaxLength(), finite.getMinLength());
					}
					else if (logical instanceof LogicalTypeInfinite)
						logger.printf("\t%s (Infinite): Priority: %d\n", logical.getQualifier(), logical.getPriority());
					else {
						LogicalTypeRegExp logicalRegExp = (LogicalTypeRegExp)logical;
						logger.printf("\t%s (RegExp): Priority: %d, RegExp: '%s', HotWords: '%s'\n",
								logical.getQualifier(), logical.getPriority(), logical.getRegExp(), String.join("|", logicalRegExp.getHotWords()));
					}
				}
			}
			System.exit(0);
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
