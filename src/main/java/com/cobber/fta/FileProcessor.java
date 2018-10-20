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

class FileProcessor {
	class RecordReader implements AutoCloseable {
		BufferedReader in = null;
		PrintStream logger;
		CSVFormat csvFormat;
		String filename;
		String charset;
		CSVParser records = null;

		RecordReader(PrintStream logger, CSVFormat csvFormat, String filename, String charset) {
			this.logger = logger;
			this.csvFormat = csvFormat;
			this.filename = filename;
			this.charset = charset;

			try {
				in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename)), charset));
			} catch (UnsupportedEncodingException e1) {
				logger.printf("Charset '%s' not supported\n", charset);
				System.exit(1);
			} catch (FileNotFoundException e1) {
				logger.printf("File '%s' not found\n", filename);
				System.exit(1);
			}
		}

		CSVParser getRecords() {

			// Parse the input using commons-csv
			try {
				records = csvFormat.parse(in);
				return records;
			} catch (IOException e) {
				logger.printf("Failed to parse input file '%s'\n", filename);
				System.exit(1);
			}

			return null;
		}

		@Override
		public void close() {
			try {
				if (records != null)
					records.close();
				if (in != null)
					in.close();
			} catch (IOException e) {
				// Silently eat
			}
		}
	}

	final Options options;
	private PrintStream logger;
	private String filename;

	FileProcessor(PrintStream logger, String filename, Options options) {
		this.logger = logger;
		this.filename = filename;
		this.options = options;
	}

	void process() throws IOException {
		final long start = System.currentTimeMillis();
		TextAnalyzer[] analysis = null;
		CSVParser records = null;
		String[] header = null;
		int numFields = 0;

		try (RecordReader r = new RecordReader(logger, options.csvFormat, filename, options.charset)) {
			records = r.getRecords();

			long thisRecord = -1;
			for (final CSVRecord record : records) {
				thisRecord = record.getRecordNumber();
				// If this is the header we need to build the header
				if (thisRecord == 1) {
					numFields = record.size();
					header = new String[numFields];
					analysis = new TextAnalyzer[numFields];
					if (options.col > numFields) {
						logger.printf("Column %d does not exist.  Only %d field(s) in input.\n", options.col, numFields);
						System.exit(1);
					}
					for (int i = 0; i < numFields; i++) {
						header[i] = record.get(i);
						if ((options.col == -1 || options.col == i) && options.verbose)
							System.out.println(record.get(i));
						analysis[i] = new TextAnalyzer(header[i], options.resolutionMode);
						if (options.noStatistics)
							analysis[i].setCollectStatistics(false);
						if (options.sampleSize != -1)
							analysis[i].setSampleSize(options.sampleSize);
						if (options.maxCardinality != -1)
							analysis[i].setMaxCardinality(options.maxCardinality);
					}
				}
				else {
					if (record.size() != numFields) {
						logger.printf("Record %d has %d fields, expected %d, skipping\n",
								record.getRecordNumber(), record.size(), numFields);
						continue;
					}
					for (int i = 0; i < numFields; i++) {
						if (options.col == -1 || options.col == i) {
							if (options.verbose)
								System.out.printf("\"%s\"\n", record.get(i));
							analysis[i].train(record.get(i));
						}
					}
				}
				if (thisRecord == options.recordsToAnalyze)
					break;
			}
		}

		// Validate the result of the analysis if requested
		int[] matched = new int[numFields];
		if (options.validate) {
			try (RecordReader r = new RecordReader(logger, options.csvFormat, filename, options.charset)) {
				records = r.getRecords();

				long thisRecord = -1;
				for (final CSVRecord record : records) {
					thisRecord = record.getRecordNumber();
					if (thisRecord == 1) {
						numFields = record.size();
					}
					else {
						if (record.size() != numFields) {
							continue;
						}
						for (int i = 0; i < numFields; i++) {
							if (options.col == -1 || options.col == i) {
								if (record.get(i).matches(analysis[i].getResult().getRegExp()))
									matched[i]++;
							}
						}
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
				logger.printf("Field '%s' (%d) - %s\n", header[i], i, result.dump(options.verbose));
				if (result.getType() != null)
					typesDetected++;
				matchCount += result.getMatchCount();
				sampleCount += result.getSampleCount();
				if (options.validate && matched[i] != result.getMatchCount()) {
					logger.printf("\tMatch Count via RegExp (%d) does not match analysis (%d)\n", matched[i], result.getMatchCount());
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