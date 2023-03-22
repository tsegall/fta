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
import com.cobber.fta.driver.faker.Faker;

public class Driver {
	private static DriverOptions cmdLineOptions;

	public static void main(final String[] args) throws IOException {
		PrintStream output = System.out;
		PrintStream error = System.err;
		boolean helpRequested = false;
		String replayFile = null;

		cmdLineOptions = new DriverOptions();
		final String[] unprocessed = cmdLineOptions.addFromStringArray(args);
		int idx = 0;
		if (unprocessed != null) {
			while (idx < unprocessed.length && unprocessed[idx].charAt(0) == '-') {
				if ("--createBloomfilter".equals(unprocessed[idx])) {
					DriverUtils.createBloomOutput(unprocessed[idx + 1], unprocessed[idx + 2]);
					System.exit(0);
				}
				else if ("--createNormalized".equals(unprocessed[idx])) {
					DriverUtils.createNormalizedOutput(unprocessed[idx + 1]);
					System.exit(0);
				}
				else if ("--createSemanticHTML".equals(unprocessed[idx])) {
					DriverUtils.createSemanticHTML(unprocessed[idx + 1]);
					System.exit(0);
				}
				else if ("--help".equals(unprocessed[idx])) {
					error.println("Usage: fta [OPTIONS] file ...");
					error.println("Valid OPTIONS are:");
					error.println(" --abbreviationPunctuation - Disable NO_ABBREVIATION_PUNCTUATION mode");
					error.println(" --createBloomfilter <input> <type> - Create Bloom Filter from CSV input, type: 'integer'|'string'");
					error.println(" --createNormalized <input> - Create Normalized output from CSV input");
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
					error.println(" --validate <n> - Set the validations level to <n>, 1 == counts, 2 == regExp");
					error.println(" --verbose - Output each record as it is processed");
					error.println(" --xMaxCharsPerColumn <n> - Set the maximum column width (CSV parsing option)");
					error.println(" --xMaxColumns <n> - Set the maximum number of columns (CSV parsing option - default 1024)");
					helpRequested = true;

				}
				else if ("--replay".equals(unprocessed[idx]))
					replayFile = unprocessed[++idx];
				else if ("--version".equals(unprocessed[idx])) {
					error.printf("%s%n", Utils.getVersion());
					System.exit(0);
				}
				else {
					error.printf("ERROR: Unrecognized option: '%s', use --help%n", unprocessed[idx]);
					System.exit(1);
				}
				idx++;
			}
		}

		// Are we are replaying a trace file?
		if (replayFile != null) {
			Replay.replay(replayFile, cmdLineOptions);
			System.exit(0);
		}

		// Are we generating a signature?
		if (cmdLineOptions.signature) {
			final TextAnalyzer analyzer = TextAnalyzer.getDefaultAnalysis(cmdLineOptions.locale);

			LogicalType logical = DriverUtils.getLogicalType(analyzer, cmdLineOptions.pluginName);
			error.println(logical.getSignature());
			System.exit(0);
		}

		// Are we generating synthetic data?
		if (cmdLineOptions.faker != null) {
			Faker faker = new Faker(cmdLineOptions, output, error);
			faker.fake();
			System.exit(0);
		}

		// Are we generating all samples?
		if (cmdLineOptions.samples) {
			final long ouputRecords = cmdLineOptions.recordsToProcess == -1 ? 20 : cmdLineOptions.recordsToProcess;
			final TextAnalyzer analyzer = TextAnalyzer.getDefaultAnalysis(cmdLineOptions.locale);
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

		if (helpRequested && cmdLineOptions.verbose != 0) {
			final TextAnalyzer analyzer = TextAnalyzer.getDefaultAnalysis(cmdLineOptions.locale);
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

			Locale locale = cmdLineOptions.locale == null ? Locale.getDefault() : cmdLineOptions.locale;
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
			error.printf("\tMinus Sign: '%c'%n", symbols.getMinusSign());
			final NumberFormat simple = NumberFormat.getNumberInstance(locale);
			if (simple instanceof DecimalFormat)
				error.printf("\tNegative Prefix: '%s'%n", ((DecimalFormat) simple).getNegativePrefix());

			System.exit(0);
		}

		if (unprocessed == null || idx == unprocessed.length) {
			error.printf("ERROR: No file to process supplied, use --help%n");
			System.exit(1);
		}

		// Loop over all the file arguments
		while (idx < unprocessed.length) {
			final String filename = unprocessed[idx++];

			final FileProcessor fileProcessor = new FileProcessor(System.err, filename, cmdLineOptions);

			try {
				fileProcessor.process();
			} catch (FTAPluginException e) {
				error.printf("ERROR: Plugin Exception: %s%n", e.getMessage());
				System.exit(1);
			} catch (FTAUnsupportedLocaleException e) {
				final Locale activeLocale = cmdLineOptions.locale != null ? cmdLineOptions.locale : Locale.getDefault();
				error.printf("ERROR: Unsupported Locale: %s, error: %s%n", activeLocale.toLanguageTag(), e.getMessage());
				System.exit(1);
			}
		}
	}

}
