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
 */
package com.cobber.fta.driver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.cobber.fta.PatternInfo;
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

	void process() throws IOException {
		final long start = System.currentTimeMillis();
		TextAnalyzer[] analysis = null;
		String[] header = null;
		int numFields = 0;

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


		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename)), options.charset))) {

			CsvParser parser = new CsvParser(settings);
			parser.beginParsing(in);

			header = parser.getRecordMetadata().headers();
			numFields = header.length;
			analysis = new TextAnalyzer[numFields];
			if (options.col > numFields) {
				logger.printf("Column %d does not exist.  Only %d field(s) in input.\n", options.col, numFields);
				System.exit(1);
			}
			for (int i = 0; i < numFields; i++) {
				if ((options.col == -1 || options.col == i) && options.verbose)
					System.out.println(header[i]);
				analysis[i] = new TextAnalyzer(header[i], options.resolutionMode);
				if (options.noStatistics)
					analysis[i].setCollectStatistics(false);
				if (options.noLogicalTypes)
					analysis[i].setDefaultLogicalTypes(false);
				if (options.detectWindow != -1)
					analysis[i].setDetectWindow(options.detectWindow);
				if (options.maxCardinality != -1)
					analysis[i].setMaxCardinality(options.maxCardinality);
				if (options.locale != null)
					analysis[i].setLocale(options.locale);
				if (options.debug != -1)
					analysis[i].setDebug(options.debug);
				for (String logicalDefinition : options.logicalTypes) {
					String[] components = logicalDefinition.split(",");
					analysis[i].registerLogicalTypeRegExp(components[0], components[1].split("\\|"), components[2],
							Integer.valueOf(components[3]), PatternInfo.Type.valueOf(components[4]));
				}
			}

			long thisRecord = 0;
			String[] row;

			while ((row = parser.parseNext()) != null) {
				thisRecord++;
				if (row.length != numFields) {
					logger.printf("Record %d has %d fields, expected %d, skipping\n",
							thisRecord, row.length, numFields);
					continue;
				}
				for (int i = 0; i < numFields; i++) {
					if (options.col == -1 || options.col == i) {
						if (options.verbose)
							System.out.printf("\"%s\"\n", row[i]);
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
			logger.printf("Filename '%s' not found.\n", filename);
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
							else if (options.verbose)
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
				logger.printf("Field '%s' (%d) - %s\n", header[i], i, result.asJSON(options.pretty, options.verbose));
				if (result.getType() != null)
					typesDetected++;
				matchCount += result.getMatchCount();
				sampleCount += result.getSampleCount();
				if (options.validate && matched[i] != result.getMatchCount()) {
					if (result.isLogicalType())
						if (matched[i] > result.getMatchCount())
							logger.printf("\t*** Warning: Match Count via RegExp (%d) > LogicalType match analysis (%d) ***\n", matched[i], result.getMatchCount());
						else
							logger.printf("\t*** Error: Match Count via RegExp (%d) < LogicalType match analysis (%d) ***\n", matched[i], result.getMatchCount());
					else
						logger.printf("\t*** Error: Match Count via RegExp (%d) does not match analysis (%d) ***\n", matched[i], result.getMatchCount());
					if (options.verbose) {
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
			logger.printf("Summary: File: %s, Types detected %d of %d (%.2f%%), Matched %d, Samples %d.\n",
					filename, typesDetected, numFields, percentage, matchCount, sampleCount);
		}
		else {
			final double confidence = result == null ? 0 : result.getConfidence();
			logger.printf("Summary: Type detected: %s, Matched %d, Samples %d (Confidence: %.2f%%).\n",
					(typesDetected == 1 ? "yes" : "no"), matchCount,
					sampleCount, confidence*100);
		}
		logger.printf("Execution time: %dms\n", duration);
	}
}