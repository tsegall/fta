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
 *
 * Simple Driver to utilize the FTA framework.
 */
package com.cobber.fta.driver;

import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
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
import com.cobber.fta.driver.faker.Faker;

public class Driver {
	private static DriverOptions options;

	public static void main(final String[] args) throws IOException {
		PrintStream output = System.out;
		PrintStream error = System.err;
		boolean helpRequested = false;
		String replayFile = null;

		options = new DriverOptions();
		int idx = 0;
		while (idx < args.length && args[idx].charAt(0) == '-') {
			if ("--abbreviationPunctuation".equals(args[idx]))
				options.abbreviationPunctuation = true;
			else if ("--bloomfilter".equals(args[idx])) {
				DriverUtils.createBloomOutput(args[idx + 1], args[idx + 2]);
				System.exit(0);
			}
			else if ("--bulk".equals(args[idx]))
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
			else if ("--faker".equals(args[idx]))
				options.faker = args[++idx];
			else if ("--formatDetection".equals(args[idx]))
				options.formatDetection = true;
			else if ("--help".equals(args[idx])) {
				error.println("Usage: fta [OPTIONS] file ...");
				error.println("Valid OPTIONS are:");
				error.println(" --abbreviationPunctuation - Disable NO_ABBREVIATION_PUNCTUATION mode");
				error.println(" --bloomfilter <input> <type> - Create Bloom Filter from CSV input, type: 'integer'|'string'");
				error.println(" --bulk - Enable bulk mode");
				error.println(" --charset <charset> - Use the supplied <charset> to read the input files");
				error.println(" --col <n> - Only analyze column <n>");
				error.println(" --debug <n> - Set the debug level to <n>");
				error.println(" --delimiter <ch> - Set the delimiter to the character <ch>");
				error.println(" --detectWindow <n> - Set the size of the detect window to <n>");
				error.println(" --faker <header> - Header is a comma separated list of Semantic Types");
				error.println(" --formatDetection - Enable Format Detection");
				error.println(" --help - Print this help");
				error.println(" --json - Output as JSON");
				error.println(" --knownTypes <SemanticTypes> - Comma separated list of Semantic Types");
				error.println(" --legacyJSON - Output legacy JSON - compatible with FTA 11.X or lower");
				error.println(" --locale <LocaleIdentifier> - Locale to use as opposed to default");
				error.println(" --maxCardinality <n> - Set the size of the Maximum Cardinality set supported");
				error.println(" --maxInputLength <n> - Set the Maximum Input length supported");
				error.println(" --maxOutlierCardinality <n> - Set the size of the Maximum Outlier Cardinality set supported");
				error.println(" --normalize <input> - Create Normalized output from CSV input");
				error.println(" --noAnalysis - Do not do analysis");
				error.println(" --noPretty - Do not pretty print analysis");
				error.println(" --noQuantiles - Do not track quantiles");
				error.println(" --noSemanticTypes - Do not register any Semantic Types");
				error.println(" --noStatistics - Do not track statistics");
				error.println(" --pluginDefinition - Output the plugin definitions from the training data set");
				error.println(" --pluginMode true|false - Set the detect mode when running Plugin validate");
				error.println(" --pluginName <PluginName> - Use supplied Plugin to generate samples or a signature (record count based on --records)");
				error.println(" --pluginThreshold <n> - Set the plugin threshold percentage (0-100) for detection");
				error.println(" --quoteChar <ch> - Set the quote character to  <ch>");
				error.println(" --records <n> - The number of records to analyze");
				error.println(" --replay <file>.fta - Replay the FTA trace file");
				error.println(" --resolutionMode <DayFirst|MonthFirst|Auto|None> - Auto DayFirst or MonthFirst is determined from Locale");
				error.println(" --samples - If set then generate samples");
				error.println(" --semanticType <JSON representation of Semantic Types> - Can be inline or as a File");
				error.println(" --signature - Output the Signature for the supplied pluginName");
				error.println(" --skip <n> - Skip the initial <n> rows of the input");
				error.println(" --threshold <n> - Set the threshold percentage (0-100) for detection");
				error.println(" --trace <trace_options> - Set trace options");
				error.println(" --validate - Validate the result of the analysis by reprocessing file against results");
				error.println(" --verbose - Output each record as it is processed");
				error.println(" --xMaxCharsPerColumn <n> - Set the maximum column width (CSV parsing option)");
				error.println(" --xMaxColumns <n> - Set the maximum number of columns (CSV parsing option - default 1024)");
				helpRequested = true;

			}
			else if ("--locale".equals(args[idx])) {
				String tag = args[++idx];
				options.locale = Locale.forLanguageTag(tag);
				if (!options.locale.toLanguageTag().equals(tag)) {
					error.printf("ERROR: Language tag '%s' not known - using '%s'?%n", tag, options.locale.toLanguageTag());
					System.exit(1);
				}
			}
			else if ("--json".equals(args[idx]))
				options.json = true;
			else if ("--knownTypes".equals(args[idx]))
				options.knownTypes = args[++idx];
			else if ("--legacyJSON".equals(args[idx]))
				options.legacyJSON = true;
			else if ("--maxInputLength".equals(args[idx]))
				options.maxInputLength = Integer.valueOf(args[++idx]);
			else if ("--maxOutlierCardinality".equals(args[idx]))
				options.maxOutlierCardinality = Integer.valueOf(args[++idx]);
			else if ("--normalize".equals(args[idx])) {
				DriverUtils.createNormalizedOutput(args[idx + 1]);
				System.exit(0);
			}
			else if ("--noAnalysis".equals(args[idx]))
				options.noAnalysis = true;
			else if ("--noPretty".equals(args[idx]))
				options.pretty = false;
			else if ("--noDistributions".equals(args[idx]))
				options.noDistributions = true;
			else if ("--noSemanticTypes".equals(args[idx]))
				options.noSemanticTypes = true;
			else if ("--noStatistics".equals(args[idx]))
				options.noStatistics = true;
			else if ("--noNullAsText".equals(args[idx]))
				options.noNullAsText = true;
			else if ("--output".equals(args[idx]))
				options.output = true;
			else if ("--pluginDefinition".equals(args[idx]))
				options.pluginDefinition = true;
			else if ("--pluginMode".equals(args[idx]))
				options.pluginMode = Boolean.valueOf(args[++idx]);
			else if ("--pluginName".equals(args[idx]))
				options.pluginName = args[++idx];
			else if ("--pluginThreshold".equals(args[idx]))
				options.pluginThreshold = Integer.valueOf(args[++idx]);
			else if ("--quoteChar".equals(args[idx]))
				options.quoteChar = args[++idx];
			else if ("--records".equals(args[idx]))
				options.recordsToProcess = Long.valueOf(args[++idx]);
			else if ("--replay".equals(args[idx]))
				replayFile = args[++idx];
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
					error.printf("ERROR: Unrecognized argument: '%s', expected Dayfirst or MonthFirst or Auto or None%n", mode);
					System.exit(1);
				}
			}
			else if ("--samples".equals(args[idx]))
				options.samples = true;
			else if ("--semanticType".equals(args[idx]))
				options.semanticTypes = args[++idx];
			else if ("--signature".equals(args[idx]))
				options.signature = true;
			else if ("--skip".equals(args[idx]))
				options.skip = Integer.valueOf(args[++idx]);
			else if ("--threshold".equals(args[idx]))
				options.threshold = Integer.valueOf(args[++idx]);
			else if ("--trace".equals(args[idx]))
				options.trace = args[++idx];
			else if ("--validate".equals(args[idx]))
				options.validate = true;
			else if ("--verbose".equals(args[idx]))
				options.verbose++;
			else if ("--version".equals(args[idx])) {
				error.printf("%s%n", Utils.getVersion());
				System.exit(0);
			}
			else if ("--xMaxCharsPerColumn".equals(args[idx]))
				options.xMaxCharsPerColumn = Integer.valueOf(args[++idx]);
			else if ("--xMaxColumns".equals(args[idx]))
				options.xMaxColumns = Integer.valueOf(args[++idx]);
			else {
				error.printf("ERROR: Unrecognized option: '%s', use --help%n", args[idx]);
				System.exit(1);
			}
			idx++;
		}

		// Are we are replaying a trace file?
		if (replayFile != null) {
			Replay.replay(replayFile, options);
			System.exit(0);
		}

		// Are we generating a signature?
		if (options.signature) {
			final TextAnalyzer analyzer = DriverUtils.getDefaultAnalysis(options.locale);

			LogicalType logical = DriverUtils.getLogicalType(analyzer, options.pluginName);
			error.println(logical.getSignature());
			System.exit(0);
		}

		// Are we generating synthetic data?
		if (options.faker != null) {
			Faker faker = new Faker(options, output, error);
			faker.fake();
			System.exit(0);
		}

		// Are we generating all samples?
		if (options.samples) {
			final long ouputRecords = options.recordsToProcess == -1 ? 20 : options.recordsToProcess;
			final TextAnalyzer analyzer = DriverUtils.getDefaultAnalysis(options.locale);
			final Collection<LogicalType> registered = analyzer.getPlugins().getRegisteredLogicalTypes();

			for (final LogicalType logical : registered) {
				try (PrintStream results = new PrintStream(logical.getSemanticType() + ".csv")) {
					if (logical instanceof LogicalTypeRegExp && !((LogicalTypeRegExp)logical).isRegExpComplete())
						error.printf("ERROR: Semantic Type (%s) does implement LTRandom interface - however samples may not be useful.%n", logical.getSemanticType());

					// Use the Semantic Type as a header!
					results.println(logical.getSemanticType());
					for (long l = 0; l < ouputRecords; l++)
						results.printf("\"%s\"%n", logical.nextRandom());
				}
			}
			System.exit(0);
		}

		if (helpRequested && options.verbose != 0) {
			final TextAnalyzer analyzer = DriverUtils.getDefaultAnalysis(options.locale);
			final Collection<LogicalType> registered = analyzer.getPlugins().getRegisteredLogicalTypes();
			final Set<String> qualifiers = new TreeSet<>();

			// Sort the registered plugins by Qualifier
			for (final LogicalType logical : registered)
				qualifiers.add(logical.getSemanticType());

			if (!registered.isEmpty()) {
				error.println("\nRegistered Semantic Types:");
				for (final String qualifier : qualifiers) {
					final LogicalType logical = analyzer.getPlugins().getRegistered(qualifier);
					if (logical instanceof LogicalTypeFinite) {
						final LogicalTypeFinite finite = (LogicalTypeFinite)logical;
						error.printf("\t%s (Finite): Priority: %d, Cardinality: %d, MaxLength: %d, MinLength: %d",
								logical.getSemanticType(), logical.getPriority(), finite.getSize(), finite.getMaxLength(), finite.getMinLength());
					}
					else if (logical instanceof LogicalTypeInfinite)
						error.printf("\t%s (Infinite): Priority: %d", logical.getSemanticType(), logical.getPriority());
					else {
						error.printf("\t%s (RegExp): Priority: %d, RegExp: '%s'",
								logical.getSemanticType(), logical.getPriority(), logical.getRegExp());
					}
					error.printf(", Locales: '%s'%n\t\t%s%n", logical.getPluginDefinition().getLocaleDescription(), logical.getDescription());
				}
			}

			Locale locale = options.locale == null ? Locale.getDefault() : options.locale;
			final DateFormatSymbols dfs = DateFormatSymbols.getInstance(locale);
			final GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(locale);

			final int actualMonths = cal.getActualMaximum(Calendar.MONTH);

			error.printf("%nLocale: '%s'%n", locale.toLanguageTag());

			error.printf("\tMonths: ");
			final String[] months = dfs.getMonths();
			for (int i = 0; i <= actualMonths; i++) {
				error.printf("%s", months[i]);
				if (i != actualMonths)
					error.printf(", ");
			}

			error.printf("%n\tShort Months: ");
			final String[] shortMonths = dfs.getShortMonths();
			for (int i = 0; i <= actualMonths; i++) {
				error.printf("%s", shortMonths[i]);
				if (i != actualMonths)
					error.printf(", ");
			}

			final String[] amPmStrings = dfs.getAmPmStrings();
			error.printf("%n\tAM/PM: %s, %s%n", amPmStrings[0], amPmStrings[1]);

			DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(locale);
			DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
			error.printf("\tDecimal Separator: '%c'%n", symbols.getDecimalSeparator());
			error.printf("\tGrouping Separator: '%c'%n", symbols.getGroupingSeparator());

			System.exit(0);
		}

		if (idx == args.length) {
			error.printf("ERROR: No file to process supplied, use --help%n");
			System.exit(1);
		}

		// Loop over all the file arguments
		while (idx < args.length) {
			final String filename = args[idx++];

			if (options.output)
				output = new PrintStream(filename + ".out");
			final FileProcessor fileProcessor = new FileProcessor(output, System.err, filename, options);

			try {
				fileProcessor.process();
				if (options.output)
					output.close();
			} catch (FTAPluginException e) {
				error.printf("ERROR: Plugin Exception: %s%n", e.getMessage());
				System.exit(1);
			} catch (FTAUnsupportedLocaleException e) {
				final Locale activeLocale = options.locale != null ? options.locale : Locale.getDefault();
				error.printf("ERROR: Unsupported Locale: %s, error: %s%n", activeLocale.toLanguageTag(), e.getMessage());
				System.exit(1);
			}
		}
	}

}
