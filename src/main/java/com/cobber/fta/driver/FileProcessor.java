/*
 * Copyright 2017-2020 Tim Segall
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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

class FileProcessor {
	final DriverOptions options;
	private PrintStream logger;
	private String filename;

	FileProcessor(PrintStream logger, String filename, DriverOptions options) {
		this.logger = logger;
		this.filename = filename;
		this.options = options;
	}

	void setOptions(TextAnalyzer analyzer) throws IOException {
		if (options.debug != -1)
			analyzer.setDebug(options.debug);
		if (options.detectWindow != -1)
			analyzer.setDetectWindow(options.detectWindow);
		if (options.maxCardinality != -1)
			analyzer.setMaxCardinality(options.maxCardinality);
		if (options.maxOutlierCardinality != -1)
			analyzer.setMaxOutliers(options.maxOutlierCardinality);
		if (options.pluginThreshold != -1)
			analyzer.setPluginThreshold(options.pluginThreshold);
		if (options.locale != null)
			analyzer.setLocale(options.locale);
		if (options.noStatistics)
			analyzer.setCollectStatistics(false);
		if (options.noLogicalTypes)
			analyzer.setDefaultLogicalTypes(false);

		if (options.logicalTypes != null)
			try {
				if (options.logicalTypes.charAt(0) == '[')
					analyzer.getPlugins().registerPlugins(new StringReader(options.logicalTypes),
							analyzer.getStreamName(), options.locale);
				else
					analyzer.getPlugins().registerPlugins(new FileReader(options.logicalTypes), analyzer.getStreamName(), options.locale);
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
					| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				System.err.println("Failed to register plugin: " + e.getMessage());
				System.exit(1);
			}
		if (options.threshold != -1)
			analyzer.setThreshold(options.threshold);
	}

	void process() throws IOException {
		CsvParserSettings settings = new CsvParserSettings();
		settings.setHeaderExtractionEnabled(true);
		settings.detectFormatAutomatically();
		settings.setLineSeparatorDetectionEnabled(true);
		settings.setDelimiterDetectionEnabled(true, ',', '\t', '|', ';');
		settings.setIgnoreLeadingWhitespaces(false);
		settings.setIgnoreTrailingWhitespaces(false);
		settings.setNullValue("");
		settings.setEmptyValue("");
		if (options.xMaxCharsPerColumn != -1)
			settings.setMaxCharsPerColumn(options.xMaxCharsPerColumn);

		if (options.bulk)
			processBulk(settings);
		else
			processAllFields(settings);
	}

	void processBulk(CsvParserSettings settings) throws IOException {
		String[] header = null;
		int numFields = 0;
		TextAnalyzer analyzer = null;
		TextAnalysisResult result;
        String previousKey = null;
        String key = null;
        String previousName = null;
        String name = null;
		Map<String, Long> bulkMap = new HashMap<>();

		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename)), options.charset))) {
			CsvParser parser = new CsvParser(settings);
			parser.beginParsing(in);

			header = parser.getRecordMetadata().headers();
			numFields = header.length;

			long thisRecord = 0;
			String[] row;
			while ((row = parser.parseNext()) != null) {
				if (row.length != numFields) {
					logger.printf("Record %d has %d fields, expected %d, skipping%n",
							thisRecord, row.length, numFields);
					continue;
				}
				key = row[0];
				name = row[1];
				String fieldValue = row[2];
				Long fieldCount = Long.valueOf(row[3]);
				if (previousKey == null || !key.equals(previousKey)) {
					if (!bulkMap.isEmpty()) {
						analyzer = new TextAnalyzer(previousName);
						analyzer.trainBulk(bulkMap);
						result = analyzer.getResult();
						logger.printf("Field '%s' - %s%n", analyzer.getStreamName(), result.asJSON(options.pretty, options.verbose));
					}
					bulkMap.clear();
					previousKey = key;
					previousName = name;
                }
                bulkMap.put(fieldValue, fieldCount);
			}

			if (!bulkMap.isEmpty()) {
				analyzer = new TextAnalyzer(name);
				analyzer.trainBulk(bulkMap);
				result = analyzer.getResult();
				logger.printf("Field '%s' - %s%n", analyzer.getStreamName(), result.asJSON(options.pretty, options.verbose));
			}
		}
	}

	void processAllFields(CsvParserSettings settings) throws IOException {
		final long start = System.currentTimeMillis();
		TextAnalyzer[] analysis = null;
		String[] header = null;
		int numFields = 0;

		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename)), options.charset))) {

			CsvParser parser = new CsvParser(settings);
			parser.beginParsing(in);

			header = parser.getRecordMetadata().headers();
			numFields = header.length;
			analysis = new TextAnalyzer[numFields];
			if (options.col > numFields) {
				logger.printf("Column %d does not exist.  Only %d field(s) in input.%n", options.col, numFields);
				System.exit(1);
			}
			for (int i = 0; i < numFields; i++) {
				if ((options.col == -1 || options.col == i) && options.verbose != 0)
					System.out.println(header[i]);
				analysis[i] = new TextAnalyzer(header[i], options.resolutionMode);
				setOptions(analysis[i]);
			}

			long thisRecord = 0;
			String[] row;

			while ((row = parser.parseNext()) != null) {
				thisRecord++;
				if (row.length != numFields) {
					logger.printf("Record %d has %d fields, expected %d, skipping%n",
							thisRecord, row.length, numFields);
					continue;
				}
				for (int i = 0; i < numFields; i++) {
					if (options.col == -1 || options.col == i) {
						if (options.verbose != 0)
							System.out.printf("\"%s\"%n", row[i]);
						if (!options.noAnalysis)
							analysis[i].train(row[i]);
					}
				}
				if (thisRecord == options.recordsToAnalyze) {
					parser.stopParsing();
					break;
				}
			}
		}
		catch (FileNotFoundException e) {
			logger.printf("Filename '%s' not found.%n", filename);
			System.exit(1);
		}

		if (options.noAnalysis)
			System.exit(0);

		// Validate the result of the analysis if requested
		int[] matched = new int[numFields];
		int[] blanks = new int[numFields];
		Set<String> failures = new HashSet<>();
		if (options.validate) {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename)), options.charset))) {

				CsvParser parser = new CsvParser(settings);
				parser.beginParsing(in);
				numFields = parser.getRecordMetadata().headers().length;

				TextAnalysisResult[] results = new TextAnalysisResult[numFields];
				Pattern[] patterns = new Pattern[numFields];

				for (int i = 0; i < numFields; i++)
					if (options.col == -1 || options.col == i) {
						results[i] = analysis[i].getResult();
						patterns[i] = Pattern.compile(results[i].getRegExp());
					}

				long thisRecord = 0;
				String[] row;

				while ((row = parser.parseNext()) != null) {
					thisRecord++;
					if (row.length != numFields)
						continue;

					for (int i = 0; i < numFields; i++) {
						if (options.col == -1 || options.col == i) {
							String value = row[i];
							if (value.trim().isEmpty())
								blanks[i]++;
							else if (patterns[i].matcher(value).matches())
								matched[i]++;
							else if (options.verbose != 0)
								failures.add(value);
						}
					}
					if (thisRecord == options.recordsToAnalyze) {
						parser.stopParsing();
						break;
					}
				}
			}
		}

		int typesDetected = 0;
		int matchCount = 0;
		int sampleCount = 0;
		TextAnalysisResult result = null;
		for (int i = 0; i < numFields; i++) {
			if (options.col == -1 || options.col == i) {
				result = analysis[i].getResult();
				logger.printf("Field '%s' (%d) - %s%n", header[i], i, result.asJSON(options.pretty, options.verbose));
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
						for (String failure : failures)
							logger.println("\t" + failure);
					}
				}
			}
		}

		final long duration = System.currentTimeMillis() - start;
		if (options.col == -1) {
			final double percentage = numFields == 0 ? 0 : ((double)typesDetected*100)/numFields;
			logger.printf("Summary: File: %s, Types detected %d of %d (%.2f%%), Matched %d, Samples %d.%n",
					filename, typesDetected, numFields, percentage, matchCount, sampleCount);
		}
		else {
			final double confidence = result == null ? 0 : result.getConfidence();
			logger.printf("Summary: Type detected: %s, Matched %d, Samples %d (Confidence: %.2f%%).%n",
					(typesDetected == 1 ? "yes" : "no"), matchCount,
					sampleCount, confidence*100);
		}
		logger.printf("Execution time: %dms%n", duration);
	}
}