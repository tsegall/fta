/*
 * Copyright 2017-2021 Tim Segall
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

import com.cobber.fta.LogicalType;
import com.cobber.fta.LogicalTypeFinite;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.LogicalTypeRegExp;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAUnsupportedLocaleException;
import com.cobber.fta.core.Utils;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;

public class Driver {

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
			else if ("--delimiter".equals(args[idx]))
				options.delimiter = args[++idx];
			else if ("--detectWindow".equals(args[idx]))
				options.detectWindow = Integer.valueOf(args[++idx]);
			else if ("--help".equals(args[idx])) {
				logger.println("Usage: fta [OPTIONS] file ...");
				logger.println("Valid OPTIONS are:");
				logger.println(" --bulk - Enable bulk mode");
				logger.println(" --charset <charset> - Use the supplied <charset> to read the input files");
				logger.println(" --col <n> - Only analyze column <n>");
				logger.println(" --delimiter <ch> - Set the delimiter to the charactere <ch>");
				logger.println(" --detectWindow <n> - Set the size of the detect window to <n>");
				logger.println(" --help - Print this help");
				logger.println(" --locale <LocaleIdentifier> - Locale to use as opposed to default");
				logger.println(" --logicalType <JSON representation of Logical Types> - Can be inline or as a File");
				logger.println(" --maxCardinality <n> - Set the size of the Maximum Cardinality set supported");
				logger.println(" --maxOutlierCardinality <n> - Set the size of the Maximum Outlier Cardinality set supported");
				logger.println(" --noAnalysis - Do not do analysis");
				logger.println(" --noLogicalTypes - Do not register any Logical Types");
				logger.println(" --noStatistics - Do not track statistics");
				logger.println(" --pluginDefinition - Output the plugin definitions from the training data set");
				logger.println(" --pluginName <PluginName> - Use supplied Plugin to generate samples or a signature (record count based on --records)");
				logger.println(" --pluginThreshold <n> - Set the plugin threshold percentage (0-100) for detection");
				logger.println(" --pretty - Pretty print analysis");
				logger.println(" --records <n> - The number of records to analyze");
				logger.println(" --resolutionMode <DayFirst|MonthFirst|Auto|None> - Auto DayFirst or MonthFirst is determined from Locale");
				logger.println(" --samples <n> - Set the size of the sample window");
				logger.println(" --signature - Output the Signature for the supplied pluginName");
				logger.println(" --threshold <n> - Set the threshold percentage (0-100) for detection");
				logger.println(" --validate - Validate the result of the analysis by reprocessing file against results");
				logger.println(" --verbose - Output each record as it is processed");
				logger.println(" --xMaxCharsPerColumn <n> - Set the maximum column width (CSV parsing option)");
				helpRequested = true;

			}
			else if ("--locale".equals(args[idx]))
				options.locale = Locale.forLanguageTag(args[++idx]);
			else if ("--logicalType".equals(args[idx]))
				options.logicalTypes = args[++idx];
			else if ("--maxCardinality".equals(args[idx]))
				options.maxCardinality = Integer.valueOf(args[++idx]);
			else if ("--maxInputLength".equals(args[idx]))
				options.maxInputLength = Integer.valueOf(args[++idx]);
			else if ("--maxOutlierCardinality".equals(args[idx]))
				options.maxOutlierCardinality = Integer.valueOf(args[++idx]);
			else if ("--noAnalysis".equals(args[idx]))
				options.noAnalysis = true;
			else if ("--noLogicalTypes".equals(args[idx]))
				options.noLogicalTypes = true;
			else if ("--noStatistics".equals(args[idx]))
				options.noStatistics = true;
			else if ("--pluginDefinition".equals(args[idx]))
				options.pluginDefinition = true;
			else if ("--pluginName".equals(args[idx]))
				options.pluginName = args[++idx];
			else if ("--pluginThreshold".equals(args[idx]))
				options.pluginThreshold = Integer.valueOf(args[++idx]);
			else if ("--pretty".equals(args[idx]))
				options.pretty = true;
			else if ("--records".equals(args[idx]))
				options.recordsToProcess = Long.valueOf(args[++idx]);
			else if ("--resolutionMode".equals(args[idx])) {
				final String mode = args[++idx];
				if ("DayFirst".equals(mode))
					options.resolutionMode = DateResolutionMode.DayFirst;
				else if ("MonthFirst".equals(mode))
					options.resolutionMode = DateResolutionMode.MonthFirst;
				else if ("Auto".equals(mode))
					options.resolutionMode = DateResolutionMode.Auto;
				else if ("None".equals(mode))
					options.resolutionMode = DateResolutionMode.None;
				else {
					logger.printf("ERROR: Unrecognized argument: '%s', expected Dayfirst or MonthFirst or Auto or None%n", mode);
					System.exit(1);
				}
			}
			else if ("--signature".equals(args[idx]))
				options.signature = true;
			else if ("--threshold".equals(args[idx]))
				options.threshold = Integer.valueOf(args[++idx]);
			else if ("--validate".equals(args[idx]))
				options.validate = true;
			else if ("--verbose".equals(args[idx]))
				options.verbose++;
			else if ("--version".equals(args[idx])) {
				logger.printf("%s%n", Utils.getVersion());
				System.exit(0);
			}
			else if ("--xMaxCharsPerColumn".equals(args[idx]))
				options.xMaxCharsPerColumn = Integer.valueOf(args[++idx]);
			else {
				logger.printf("ERROR: Unrecognized option: '%s', use --help%n", args[idx]);
				System.exit(1);
			}
			idx++;
		}

		// Are we generating samples or a signature?
		if (options.pluginName != null) {
			long ouputRecords = options.recordsToProcess == -1 ? 20 : options.recordsToProcess;
			final TextAnalyzer analyzer = getDefaultAnalysis();
			Collection<LogicalType> registered = analyzer.getPlugins().getRegisteredLogicalTypes();

			for (LogicalType logical : registered)
				if (logical.getQualifier().equals(options.pluginName)) {
					if (options.signature)
						logger.println(logical.getSignature());
					else {
						if (logical instanceof LogicalTypeRegExp && !((LogicalTypeRegExp)logical).isRegExpComplete())
							System.err.printf("Logical Type (%s) does implement LTRandom interface - however samples may not be useful.%n", logical.getQualifier());

						for (long l = 0; l < ouputRecords; l++)
							logger.println(logical.nextRandom());
					}

					System.exit(0);
				}

			logger.printf("ERROR: Failed to locate plugin named '%s', use --help%n", options.pluginName);
			System.exit(1);
		}

		if (helpRequested) {
			final TextAnalyzer analyzer = getDefaultAnalysis();
			final Collection<LogicalType> registered = analyzer.getPlugins().getRegisteredLogicalTypes();
			final Set<String> qualifiers = new TreeSet<>();

			// Sort the registered plugins by Qualifier
			for (final LogicalType logical : registered)
				qualifiers.add(logical.getQualifier());

			if (!registered.isEmpty()) {
				logger.println("\nRegistered Logical Types:");
				for (final String qualifier : qualifiers) {
					final LogicalType logical = analyzer.getPlugins().getRegistered(qualifier);
					if (options.verbose == 0) {
						if (logical instanceof LogicalTypeFinite) {
							final LogicalTypeFinite finite = (LogicalTypeFinite)logical;
							logger.printf("\t%s (Finite): Priority: %d, Cardinality: %d, MaxLength: %d, MinLength: %d%n",
									logical.getQualifier(), logical.getPriority(), finite.getSize(), finite.getMaxLength(), finite.getMinLength());
						}
						else if (logical instanceof LogicalTypeInfinite)
							logger.printf("\t%s (Infinite): Priority: %d%n", logical.getQualifier(), logical.getPriority());
						else {
							final LogicalTypeRegExp logicalRegExp = (LogicalTypeRegExp)logical;
							logger.printf("\t%s (RegExp): Priority: %d, RegExp: '%s', HeaderRegExps: '%s'%n",
									logical.getQualifier(), logical.getPriority(), logical.getRegExp(),
									logicalRegExp.getHeaderRegExps() != null ? String.join("|", logicalRegExp.getHeaderRegExps()) : "None");
						}
						logger.printf("\t\t%s%n", logical.getDescription());
					}
					else {
						// Used to generate the documentation
						logger.printf("%s|%s%n", logical.getQualifier(), logical.getDescription());
					}
				}
			}
			System.exit(0);
		}

		if (idx == args.length) {
			logger.printf("ERROR: No file to process supplied, use --help%n");
			System.exit(1);
		}

		// Loop over all the file arguments
		while (idx < args.length) {
			final String filename = args[idx++];

			final FileProcessor fileProcessor = new FileProcessor(logger, filename, options);
			try {
				fileProcessor.process();
			} catch (FTAPluginException e) {
				logger.printf("ERROR: Plugin Exception: %s%n", e.getMessage());
				System.exit(1);
			} catch (FTAUnsupportedLocaleException e) {
				Locale activeLocale = options.locale != null ? options.locale : Locale.getDefault();
				logger.printf("ERROR: Unsupported Locale: %s, error: %s%n", activeLocale.toLanguageTag(), e.getMessage());
				System.exit(1);
			}
		}
	}

	private static TextAnalyzer getDefaultAnalysis() {
		// Create an Analyzer to retrieve the Logical Types (magically will be all - since passed in '*')
		final TextAnalyzer analysis = new TextAnalyzer("*");
		if (options.locale != null)
			analysis.setLocale(options.locale);

		// Load the default set of plugins for Logical Type detection (normally done by a call to train())
		analysis.registerDefaultPlugins(options.locale);

		return  analysis;
	}
}
