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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.cobber.fta.DateTimeParser.DateResolutionMode;

class Driver {
	public static void main(final String[] args) throws IOException {
		final PrintStream logger = System.err;
		BufferedReader in = null;
		CSVParser records = null;
		int numFields = 0;
		CSVFormat csvFormat = CSVFormat.DEFAULT;
		String charset = "UTF-8";
		int sampleSize = -1;
		long recordsToAnalyze = -1;
		int col = -1;
		boolean verbose = false;
		boolean noStatistics = false;
		TextAnalyzer[] analysis = null;
		String[] header = null;
		DateResolutionMode resolutionMode = DateResolutionMode.None;

		final long start = System.currentTimeMillis();

		int idx = 0;
		while (idx < args.length && args[idx].charAt(0) == '-') {
			if ("--charset".equals(args[idx]))
				charset = args[++idx];
			else if ("--col".equals(args[idx]))
				col = Integer.valueOf(args[++idx]);
			else if ("--dayFirst".equals(args[idx]))
				resolutionMode = DateResolutionMode.DayFirst;
			else if ("--delimiter".equals(args[idx])) {
				String delim = args[++idx];
				csvFormat = csvFormat.withDelimiter("\\t".equals(delim) ? '\t' : delim.charAt(0));
			}
			else if ("--help".equals(args[idx])) {
				logger.println("Usage: [--charset <charset>] [--col <n>] [--dayFirst] [--help] [--monthFirst] [--records <n>] [--samples <n>] [--verbose] file ...");
				logger.println(" --charset <charset> - Use the supplied <charset> to read the input files");
				logger.println(" --col <n> - Only analyze column <n>");
				logger.println(" --dayFirst - If dates are ambigous assume Day precedes Month");
				logger.println(" --delimiter - Delimiter to use - must be a single character");
				logger.println(" --monthFirst - If dates are ambigous assume Month precedes Day");
				logger.println(" --records <n> - The number of records to analyze");
				logger.println(" --samples <n> - Set the size of the sample window");
				logger.println(" --verbose - Output each record as it is processed");
				System.exit(0);
			}
			else if ("--monthFirst".equals(args[idx]))
				resolutionMode = DateResolutionMode.MonthFirst;
			else if ("--noStatistics".equals(args[idx]))
				noStatistics = true;
			else if ("--records".equals(args[idx]))
				recordsToAnalyze = Long.valueOf(args[++idx]);
			else if ("--samples".equals(args[idx]))
				sampleSize = Integer.valueOf(args[++idx]);
			else if ("--verbose".equals(args[idx])) {
				verbose = true;
			}
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

		// Loop over all the files arguments
		while (idx < args.length) {
			String filename = args[idx++];

			try {
				try {
					in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename)), charset));
				} catch (UnsupportedEncodingException e1) {
					logger.printf("Charset '%s' not supported\n", charset);
					System.exit(1);
				} catch (FileNotFoundException e1) {
					logger.printf("File '%s' not found\n", filename);
					System.exit(1);
				}

				// Parse the input using commons-csv
				try {
					records = csvFormat.parse(in);
				} catch (IOException e) {
					logger.printf("Failed to parse input file '%s'\n", filename);
					System.exit(1);
				}

				long thisRecord = -1;
				for (final CSVRecord record : records) {
					thisRecord = record.getRecordNumber();
					// If this is the header we need to build the header
					if (thisRecord == 1) {
						numFields = record.size();
						header = new String[numFields];
						analysis = new TextAnalyzer[numFields];
						if (col > numFields) {
							logger.printf("Column %d does not exist.  Only %d field(s) in input.\n", col, numFields);
							System.exit(1);
						}
						for (int i = 0; i < numFields; i++) {
							header[i] = record.get(i);
							if ((col == -1 || col == i) && verbose)
								System.out.println(record.get(i));
							analysis[i] = new TextAnalyzer(header[i], resolutionMode);
							if (noStatistics)
								analysis[i].setCollectStatistics(false);
							if (sampleSize != -1)
								analysis[i].setSampleSize(sampleSize);
						}
					}
					else {
						if (record.size() != numFields) {
							logger.printf("Record %d has %d fields, expected %d, skipping\n",
									record.getRecordNumber(), record.size(), numFields);
							continue;
						}
						for (int i = 0; i < numFields; i++) {
							if (col == -1 || col == i) {
								if (verbose)
									System.out.printf("\"%s\"\n", record.get(i));
								analysis[i].train(record.get(i));
							}
						}
					}
					if (thisRecord == recordsToAnalyze)
						break;
				}
			}
			finally {
				try {
					if (records != null)
						records.close();
					if (in != null)
						in.close();
				} catch (IOException e) {
					// Silently eat
				}
			}

			int typesDetected = 0;
			int matchCount = 0;
			int sampleCount = 0;
			TextAnalysisResult result = null;
			for (int i = 0; i < numFields; i++) {
				if (col == -1 || col == i) {
					result = analysis[i].getResult();
					logger.printf("Field '%s' (%d) - %s\n", header[i], i, result.dump(verbose));
					if (result.getType() != null)
						typesDetected++;
					matchCount += result.getMatchCount();
					sampleCount += result.getSampleCount();
				}
			}

			final long duration = System.currentTimeMillis() - start;
			if (col == -1) {
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
}
