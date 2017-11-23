/*
 * Copyright 2017 Tim Segall
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
import java.io.UnsupportedEncodingException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.cobber.fta.DateTimeParser.DateResolutionMode;

class Driver {
	public static void main(String[] args) throws IOException {
		BufferedReader in = null;
		CSVParser records = null;
		int numFields = 0;
		CSVFormat.Predefined csvFormat = CSVFormat.Predefined.Default;
		String charset = "UTF-8";
		String filename = null;
		int sampleSize = -1;
		long recordsToAnalyze = -1;
		int col = -1;
		boolean verbose = false;
		TextAnalyzer[] analysis = null;
		String[] header = null;
		DateResolutionMode resolutionMode = DateResolutionMode.None;

		long start = System.currentTimeMillis();

		int idx = 0;
		while (idx < args.length && args[idx].startsWith("-")) {
			if ("--charset".equals(args[idx]))
				charset = args[++idx];
			else if ("--col".equals(args[idx]))
				col = Integer.valueOf(args[++idx]);
			else if ("--dayFirst".equals(args[idx]))
				resolutionMode = DateResolutionMode.DayFirst;
			else if ("--help".equals(args[idx])) {
				System.err.println("Usage: [--charset <charset>] [--col <n>] [--dayFirst] [--help] [--monthFirst] [--records <n>] [--samples <n>] file ...");
				System.err.println(" --charset <charset> - Use the supplied <charset> to read the input files");
				System.err.println(" --col <n> - Only analyze column <n>");
				System.err.println(" --dayFirst - If dates are ambigous assume Day precedes Month");
				System.err.println(" --monthFirst - If dates are ambigous assume Month precedes Day");
				System.err.println(" --records <n> - The number of records to analyze");
				System.err.println(" --samples <n> - Set the size of the sample window");
				System.exit(0);
			}
			else if ("--monthFirst".equals(args[idx]))
				resolutionMode = DateResolutionMode.MonthFirst;
			else if ("--records".equals(args[idx]))
				recordsToAnalyze = Long.valueOf(args[++idx]);
			else if ("--samples".equals(args[idx]))
				sampleSize = Integer.valueOf(args[++idx]);
			else if ("--verbose".equals(args[idx])) {
				verbose = true;
			}
			else {
				System.err.printf("Unrecognized option: '%s', use --help\n", args[idx]);
				System.exit(1);
			}
			idx++;
		}

		if (idx == args.length) {
			System.err.printf("No file to process supplied, use --help\n");
			System.exit(1);
		}

		// Loop over all the files arguments
		while (idx < args.length) {
			filename = args[idx++];

			try {
				try {
					in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename)), charset));
				} catch (UnsupportedEncodingException e1) {
					System.err.printf("Charset '%s' not supported\n", charset);
					System.exit(1);
				} catch (FileNotFoundException e1) {
					System.err.printf("File '%s' not found\n", filename);
					System.exit(1);
				}

				// Parse the input using commons-csv
				try {
					records = csvFormat.getFormat().parse(in);
				} catch (IOException e) {
					System.err.printf("Failed to parse input file '%s'\n", filename);
					System.exit(1);
				}

				long thisRecord = -1;
				for (CSVRecord record : records) {
					thisRecord = record.getRecordNumber();
					// If this is the header we need to build the header
					if (thisRecord == 1) {
						numFields = record.size();
						header = new String[numFields];
						analysis = new TextAnalyzer[numFields];
						if (col > numFields) {
							System.err.printf("Column %d does not exist.  Only %d field(s) in input.\n", col, numFields);
							System.exit(1);
						}
						for (int i = 0; i < numFields; i++) {
							header[i] = record.get(i);
							if ((col == -1 || col == i) && verbose)
								System.out.println(record.get(i));
							analysis[i] = new TextAnalyzer(header[i], resolutionMode);
							if (sampleSize != -1)
								analysis[i].setSampleSize(sampleSize);
						}
					}
					else {
						if (record.size() != numFields) {
							System.err.printf("Record %d has %d fields, expected %d, skipping\n",
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
					System.err.printf("Field '%s' (%d) - %s\n", header[i], i, result.dump(verbose));
					if (result.getType() != null)
						typesDetected++;
					matchCount += result.matchCount;
					sampleCount += result.sampleCount;
				}
			}

			long duration = System.currentTimeMillis() - start;
			if (col == -1) {
				double percentage = numFields == 0 ? 0 : ((double)typesDetected*100)/numFields;
				System.err.printf("Summary: File: %s, Types detected %d of %d (%.2f%%), Matched %d, Samples %d.\n",
						filename, typesDetected, numFields, percentage, matchCount, sampleCount);
			}
			else {
				double confidence = result != null ? result.getConfidence() : 0;
				System.err.printf("Summary: Type detected: %s, Matched %d, Samples %d (Confidence: %.2f%%).\n",
						(typesDetected == 1 ? "yes" : "no"), matchCount,
						sampleCount, confidence*100);
			}
			System.err.printf("Execution time: %dms\n", duration);
		}
	}
}
