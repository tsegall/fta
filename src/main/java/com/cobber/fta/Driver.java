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

class Driver {
	public static void main(String[] args) throws IOException {
		BufferedReader in = null;
		CSVParser records = null;
		int numFields = 0;
		CSVFormat.Predefined csvFormat = CSVFormat.Predefined.Default;
		String charset = "UTF-8";
		String filename = null;
		int samples = -1;
		int col = -1;
		boolean verbose = false;
		TextAnalyzer[] analysis = null;
		String[] header = null;

		long start = System.currentTimeMillis();

		int idx = 0;
		while (idx < args.length && args[idx].startsWith("-")) {
			if ("--charset".equals(args[idx]))
				charset = args[++idx];
			else if ("--col".equals(args[idx]))
				col = Integer.valueOf(args[++idx]);
			else if ("--help".equals(args[idx])) {
				System.err.println("Usage: [--charset <charset>] [--col <n>] [--samples <n>] [--help] file");
				System.exit(0);
			}
			else if ("--samples".equals(args[idx]))
				samples = Integer.valueOf(args[++idx]);
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

		filename = args[idx];

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
				try {
					in.close();
				} catch (IOException e1) {
					// Silently eat
				}
				System.exit(1);
			}

			for (CSVRecord record : records) {
				// If this is the header we need to build the header
				if (record.getRecordNumber() == 1) {
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
						analysis[i] = new TextAnalyzer(header[i]);
						if (samples != -1)
							analysis[i].setSampleSize(samples);
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
				System.err.printf("Result for '%s' (%d)\n\t", header[i], i);
				System.err.println(result.dump(verbose));
				if (result.getType() != null)
					typesDetected++;
				matchCount += result.matchCount;
				sampleCount += result.sampleCount;
			}
		}

		long duration = System.currentTimeMillis() - start;
		if (col == -1)
			System.err.printf("Summary: Types detected %d of %d (%.2f%%), Matched %d, Samples %d.\n",
					typesDetected, numFields, ((double)typesDetected*100)/numFields, matchCount, sampleCount);
		else
			System.err.printf("Summary: Type detected: %s, Matched %d, Samples %d (Confidence: %.2f%%).\n",
					(typesDetected == 1 ? "yes" : "no"), matchCount,
					sampleCount, result.getConfidence());
		System.err.printf("Execution time: %dms\n", duration);
	}
}
