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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAUnsupportedLocaleException;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

class FileProcessor {
	private final DriverOptions options;
	private final PrintStream logger;
	private final String filename;

	FileProcessor(final PrintStream logger, final String filename, final DriverOptions options) {
		this.logger = logger;
		this.filename = filename;
		this.options = options;
	}

	protected void process() throws IOException, FTAPluginException, FTAUnsupportedLocaleException {
		final CsvParserSettings settings = new CsvParserSettings();
		settings.setHeaderExtractionEnabled(true);
		settings.detectFormatAutomatically();
		settings.setLineSeparatorDetectionEnabled(true);
		settings.setIgnoreLeadingWhitespaces(false);
		settings.setIgnoreTrailingWhitespaces(false);
		settings.setNullValue("");
		settings.setEmptyValue("");
		if (options.delimiter != null)
			settings.getFormat().setDelimiter(options.delimiter.charAt(0));
		else
			settings.setDelimiterDetectionEnabled(true, ',', '\t', '|', ';');
		if (options.xMaxCharsPerColumn != -1)
			settings.setMaxCharsPerColumn(options.xMaxCharsPerColumn);

		settings.setMaxColumns(options.xMaxColumns);

		if (options.bulk)
			processBulk(settings);
		else
			processAllFields(settings);
	}

	private void processBulk(final CsvParserSettings settings) throws IOException, FTAPluginException, FTAUnsupportedLocaleException {
		String[] header;
		int numFields;
		TextAnalyzer analyzer;
		TextAnalysisResult result;
        String previousKey = null;
        String key;
        String previousName = null;
        String name = null;
		final Map<String, Long> bulkMap = new HashMap<>();

		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename)), options.charset))) {
			final CsvParser parser = new CsvParser(settings);
			parser.beginParsing(in);

			header = parser.getRecordMetadata().headers();

			if (header.length != 4) {
				logger.printf("ERROR: Expected input with four columns (key,fieldName,fieldValue,fieldCount).  %d field(s) in input.%n", header.length);
				System.exit(1);
			}

			numFields = header.length;

			long thisRecord = 0;
			long totalCount = 0;
			String[] row;
			while ((row = parser.parseNext()) != null) {
				thisRecord++;
				if (row.length != numFields) {
					logger.printf("Record %d has %d fields, expected %d, skipping%n",
							thisRecord, row.length, numFields);
					continue;
				}
				key = row[0];
				name = row[1];
				final String fieldValue = row[2];
				final Long fieldCount = Long.valueOf(row[3].trim());
				totalCount += fieldCount;
				if (previousKey == null || !key.equals(previousKey)) {
					if (!bulkMap.isEmpty()) {
						analyzer = new TextAnalyzer(previousName);
						options.apply(analyzer);
						analyzer.trainBulk(bulkMap);
						analyzer.setTotalCount(totalCount);
						result = analyzer.getResult();
						logger.printf("Field '%s' - %s%n", sanitize(analyzer.getStreamName()), result.asJSON(options.pretty, options.verbose));
						totalCount = 0;
					}
					bulkMap.clear();
					previousKey = key;
					previousName = name;
                }
                bulkMap.put(fieldValue, fieldCount);
			}

			if (!bulkMap.isEmpty()) {
				analyzer = new TextAnalyzer(name);
				options.apply(analyzer);
				analyzer.trainBulk(bulkMap);
				analyzer.setTotalCount(totalCount);
				result = analyzer.getResult();
				logger.printf("Field '%s' - %s%n", sanitize(analyzer.getStreamName()), result.asJSON(options.pretty, options.verbose));
			}
		}
	}

	private String sanitize(final String input) {
		if (input == null || input.isEmpty())
			return input;

		final StringBuilder b = new StringBuilder();

		for (int i = 0; i < input.length(); i++) {
			if (input.charAt(i) == '\n')
				b.append("%0A");
			else if (input.charAt(i) == '\r')
				b.append("%0D");
			else
				b.append(input.charAt(i));
		}

		return b.toString();
	}

	private void processAllFields(final CsvParserSettings settings) throws IOException, FTAPluginException, FTAUnsupportedLocaleException {
		final long start = System.currentTimeMillis();
		Processor processor = null;
		String[] header = null;
		int numFields = 0;
		long thisRecord = 0;

		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename)), options.charset))) {

			final CsvParser parser = new CsvParser(settings);
			parser.beginParsing(in);

			header = parser.getRecordMetadata().headers();
			if (header == null) {
				logger.printf("ERROR: Cannot parse header for file '%s'%n", filename);
				System.exit(1);
			}
			numFields = header.length;
			if (options.col > numFields) {
				logger.printf("ERROR: Column %d does not exist.  Only %d field(s) in input.%n", options.col, numFields);
				System.exit(1);
			}
			for (int i = 0; i < numFields; i++) {
				if ((options.col == -1 || options.col == i) && options.verbose != 0)
					System.out.println(header[i]);
			}

			processor = new Processor(com.cobber.fta.core.Utils.getBaseName(Paths.get(filename).getFileName().toString()), header, options);

			String[] row;

			while ((row = parser.parseNext()) != null) {
				thisRecord++;
				if (row.length != numFields) {
					logger.printf("Record %d has %d fields, expected %d, skipping%n",
							thisRecord, row.length, numFields);
					continue;
				}
				processor.consume(row);
				if (thisRecord == options.recordsToProcess) {
					parser.stopParsing();
					break;
				}
			}
		}
		catch (FileNotFoundException e) {
			logger.printf("ERROR: Filename '%s' not found.%n", filename);
			System.exit(1);
		}

		if (options.noAnalysis)
			System.exit(0);

		// Validate the result of the analysis if requested
		int[] matched = new int[numFields];
		int[] blanks = new int[numFields];
		final Set<String> failures = new HashSet<>();
		if (options.validate) {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename)), options.charset))) {

				final CsvParser parser = new CsvParser(settings);
				parser.beginParsing(in);
				numFields = parser.getRecordMetadata().headers().length;

				TextAnalysisResult[] results = new TextAnalysisResult[numFields];
				Pattern[] patterns = new Pattern[numFields];

				for (int i = 0; i < numFields; i++)
					if (options.col == -1 || options.col == i) {
						results[i] = processor.getAnalyzer(i).getResult();
						patterns[i] = Pattern.compile(results[i].getRegExp());
					}

				thisRecord = 0;
				String[] row;

				while ((row = parser.parseNext()) != null) {
					thisRecord++;
					if (row.length != numFields)
						continue;

					for (int i = 0; i < numFields; i++) {
						if (options.col == -1 || options.col == i) {
							final String value = row[i];
							if (value.trim().isEmpty())
								blanks[i]++;
							else if (patterns[i].matcher(value).matches())
								matched[i]++;
							else if (options.verbose != 0)
								failures.add(value);
						}
					}
					if (thisRecord == options.recordsToProcess) {
						parser.stopParsing();
						break;
					}
				}
			}
		}

		int typesDetected = 0;
		long matchCount = 0;
		long sampleCount = 0;
		TextAnalysisResult result = null;
		for (int i = 0; i < numFields; i++) {
			if (options.col == -1 || options.col == i) {
				final TextAnalyzer analyzer = processor.getAnalyzer(i);
				if (thisRecord != options.recordsToProcess)
					analyzer.setTotalCount(thisRecord);

				result = analyzer.getResult();
				logger.printf("Field '%s' (%d) - %s%n", sanitize(header[i]), i, result.asJSON(options.pretty, options.verbose));
				if (options.pluginDefinition) {
					final String pluginDefinition = result.asPlugin();
					if (pluginDefinition != null)
						logger.printf("Plugin Definition - %s%n", pluginDefinition);
				}
				if (result.getType() != null)
					typesDetected++;
				matchCount += result.getMatchCount();
				sampleCount += result.getSampleCount();
				if (options.validate && matched[i] != result.getMatchCount()) {
					if (result.isLogicalType())
						if (matched[i] > result.getMatchCount())
							logger.printf("\t*** Warning: Match Count via RegExp (%d) > LogicalType match analysis (%d) ***%n", matched[i], result.getMatchCount());
						else
							logger.printf("\t*** Error: Match Count via RegExp (%d) < LogicalType match analysis (%d) ***%n", matched[i], result.getMatchCount());
					else
						logger.printf("\t*** Error: Match Count via RegExp (%d) does not match analysis (%d) ***%n", matched[i], result.getMatchCount());
					if (options.verbose != 0) {
						logger.println("Failed to match:");
						for (final String failure : failures)
							logger.println("\t" + failure);
					}
				}
			}
		}

		analyzeRecord(processor.getAnalyzers());

	    final Runtime instance = Runtime.getRuntime();
		final double usedMemory = (instance.totalMemory() - instance.freeMemory()) / (1024 * 1024);
		final long duration = System.currentTimeMillis() - start;
		if (options.col == -1) {
			final double percentage = numFields == 0 ? 0 : ((double)typesDetected*100)/numFields;
			logger.printf("Summary: File: %s, Types detected %d of %d (%.2f%%), Matched %d, Samples %d, Used Memory: %.2f.%n",
					filename, typesDetected, numFields, percentage, matchCount, sampleCount, usedMemory);
		}
		else {
			final double confidence = result == null ? 0 : result.getConfidence();
			logger.printf("Summary: Type detected: %s, Matched %d, Samples %d (Confidence: %.2f%%), Used Memory: %.2f.%n",
					(typesDetected == 1 ? "yes" : "no"), matchCount,
					sampleCount, confidence*100, usedMemory);
		}
		logger.printf("Execution time: %dms%n", duration);
	}

	// HONORIFIC_EN, NAME.FIRST, NAME.LAST, NAME.MIDDLE -> FULL_NAME [PERSON]
	// HONORIFIC_EN, NAME.FIRST, NAME.LAST, NAME.MIDDLE_INITIAL -> FULL_NAME  [PERSON]
	// NAME.FIRST, NAME.LAST, NAME.MIDDLE -> FULL_NAME (PERSON)
	// NAME.FIRST, NAME.LAST, NAME.MIDDLE_INITIAL -> FULL_NAME [PERSON]
	// NAME.FIRST, NAME.LAST -> FULL_NAME [PERSON]
	// POSTAL_CODE.ZIP5_US, ADDRESS, CITY, STATE/PROVICE, COUNTRY, COUNTY [ADDRESS]
	// [PERSON] + DOB/Birth*Date -> PERSON.DOB
	// [PERSON] + GENDER -> PERSON.GENDER
	// [PERSON] + JOB_TITLE -> PERSON.JOB_TITLE

	private void analyzeRecord(final TextAnalyzer[] analyzers) {
	}

}
