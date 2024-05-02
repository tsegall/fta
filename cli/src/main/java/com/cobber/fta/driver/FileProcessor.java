/*
 * Copyright 2017-2024 Tim Segall
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAUnsupportedLocaleException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.univocity.parsers.common.TextParsingException;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.UnescapedQuoteHandling;

class FileProcessor {
	private final DriverOptions options;
	private final PrintStream error;
	private final String filename;
	private PrintStream output;

	FileProcessor(final PrintStream error, final String filename, final DriverOptions cmdLineOptions) {
		this.error = error;
		this.filename = filename;
		this.options = new DriverOptions(cmdLineOptions);
	}

	protected void process() throws IOException, FTAPluginException, FTAUnsupportedLocaleException, FTAProcessingException {
		if (Files.exists(Paths.get(filename + ".options"))) {
			options.addFromFile(filename + ".options");
		}

		output = options.output ? new PrintStream(filename + ".out") : System.out;

		final CsvParserSettings settings = new CsvParserSettings();
		settings.setHeaderExtractionEnabled(true);
		settings.detectFormatAutomatically();
		settings.setLineSeparatorDetectionEnabled(true);
		settings.setIgnoreLeadingWhitespaces(false);
		settings.setIgnoreTrailingWhitespaces(false);
		settings.setUnescapedQuoteHandling(UnescapedQuoteHandling.STOP_AT_DELIMITER);
//		settings.setNullValue("");
		settings.setEmptyValue("");
		settings.setSkipEmptyLines(false);
		if (options.delimiter != null) {
			settings.getFormat().setDelimiter(options.delimiter.charAt(0));
			settings.setDelimiterDetectionEnabled(false);
		}
		else
			settings.setDelimiterDetectionEnabled(true, ',', '\t', '|', ';');
		if (options.quoteChar != null) {
			settings.getFormat().setQuote(options.quoteChar.charAt(0));
			settings.setQuoteDetectionEnabled(false);
		}
		if (options.xMaxCharsPerColumn != -1)
			settings.setMaxCharsPerColumn(options.xMaxCharsPerColumn);

		settings.setMaxColumns(options.xMaxColumns);

		try {
			if (options.bulk)
				processBulk(settings);
			else
				processAllFields(settings);
		}
		catch (Exception e) {
			if (options.output)
				output.close();
			throw e;
		}
	}

	class RowCount {
		int numFields;
		long firstRow;
		long count;

		RowCount(final int numFields, final long firstRow) {
			this.numFields = numFields;
			this.firstRow = firstRow;
			this.count = 1;
		}

		RowCount inc() {
			this.count++;
			return this;
		}
	}

	private void processBulk(final CsvParserSettings settings) throws IOException, FTAPluginException, FTAUnsupportedLocaleException, FTAProcessingException {
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

			if (header.length != 4)
				throw new FTAProcessingException(filename,
						MessageFormat.format("Expected input with four columns (key,fieldName,fieldValue,fieldCount). {0} field(s) in input",
								 header.length));

			numFields = header.length;

			long thisRecord = 0;
			long totalCount = 0;
			String[] row;
			final Map<Integer, RowCount> errors = new HashMap<>();

			while ((row = parser.parseNext()) != null) {
				thisRecord++;
				if (row.length != numFields) {
					final RowCount existing = errors.get(row.length);
					if (existing == null)
						errors.put(row.length, new RowCount(row.length, thisRecord));
					else
						errors.put(row.length, existing.inc());
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
						output.printf("Field '%s' - %s%n", sanitize(analyzer.getStreamName()), result.asJSON(options.pretty, options.verbose));
						totalCount = 0;
					}
					bulkMap.clear();
					previousKey = key;
					previousName = name;
                }
                bulkMap.put(fieldValue, fieldCount);
			}

			if (!errors.isEmpty()) {
				for (final RowCount recordError : errors.values())
					error.printf("ERROR: File: '%s', %d records skipped with %d fields, first occurrence %d, expected %d%n",
							filename, recordError.count, recordError.numFields, recordError.firstRow, numFields);
			}

			if (!bulkMap.isEmpty()) {
				analyzer = new TextAnalyzer(name);
				options.apply(analyzer);
				analyzer.trainBulk(bulkMap);
				analyzer.setTotalCount(totalCount);
				result = analyzer.getResult();
				output.printf("Field '%s' - %s%n", sanitize(analyzer.getStreamName()), result.asJSON(options.pretty, options.verbose));
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

	class CircularBuffer {
		final int depth;
		final String[][] buffer;
		int current = 0;
		int records = 0;

		CircularBuffer(final int depth) {
			this.depth = depth;
			this.buffer = new String[depth][];
		}

		boolean add(final String[] record) {
			if (records == depth)
				return false;
			this.buffer[current] = record;
			current = current == depth - 1 ? 0 : current + 1;
			records++;
			return true;
		}

		String[] get() {
			if (records == 0)
				return null;
			records--;
			current = current == 0 ? depth - 1 : current - 1;
			return this.buffer[current];
		}

		boolean isFull() {
			return records == depth;
		}
	}

	private void processAllFields(final CsvParserSettings settings) throws IOException, FTAPluginException, FTAUnsupportedLocaleException, FTAProcessingException {
		final long startTime = System.currentTimeMillis();
		long initializedTime = -1;
		long consumedTime = -1;
		long resultsTime = -1;
		Processor processor = null;
		String[] header = null;
		int numFields = 0;
		long rawRecordIndex = 0;
		final Map<Integer, RowCount> errors = new HashMap<>();

		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename)), options.charset))) {

			// Skip the first <n> lines if requested
			if (options.skip != 0) {
				for (int i = 0; i < options.skip; i++)
					in.readLine();
				rawRecordIndex += options.skip;
			}

			final CsvParser parser = new CsvParser(settings);
			parser.beginParsing(in);

			header = parser.getRecordMetadata().headers();
			if (header == null)
				throw new FTAProcessingException(filename, "Cannot parse header");

			numFields = header.length;
			if (options.col > numFields)
				throw new FTAProcessingException(filename, MessageFormat.format("Column {0} does not exist.  Only {1} field(s) in input.", options.col, numFields));

			for (int i = 0; i < numFields; i++) {
				if ((options.col == -1 || options.col == i) && options.verbose != 0 && options.noAnalysis)
					System.out.println(header[i]);
			}

			processor = new Processor(com.cobber.fta.core.Utils.getBaseName(Paths.get(filename).getFileName().toString()), header, options);
			initializedTime = System.currentTimeMillis();

			final CircularBuffer buffer = new CircularBuffer(options.trailer + 1);

			String[] row;
			int processedRecords = 0;

			while ((row = parser.parseNext()) != null) {
				rawRecordIndex++;
				// Skip blank lines
				if (row.length == 1 && row[0] == null)
					continue;
				if (row.length != numFields) {
					final RowCount existing = errors.get(row.length);
					if (existing == null)
						errors.put(row.length, new RowCount(row.length, rawRecordIndex));
					else
						errors.put(row.length, existing.inc());
					continue;
				}
				buffer.add(row);
				if (!buffer.isFull())
					continue;
				row = buffer.get();
				processedRecords++;
				processor.consume(row);
				if (processedRecords == options.recordsToProcess) {
					parser.stopParsing();
					break;
				}
			}
			consumedTime = System.currentTimeMillis();
		}
		catch (FileNotFoundException e) {
			throw new FTAProcessingException(filename, "File not found", e);
		}
		catch (TextParsingException|ArrayIndexOutOfBoundsException e) {
			throw new FTAProcessingException(filename, "Univocity exception", e);
		}

		if (!errors.isEmpty()) {
			long toSkip = -1;
			for (final RowCount recordError : errors.values()) {
				error.printf("ERROR: File: '%s', %d records skipped with %d fields, first occurrence %d, expected %d%n",
						filename, recordError.count, recordError.numFields, recordError.firstRow, numFields);
				if (rawRecordIndex > 20 && recordError.count > .8 * rawRecordIndex)
					toSkip = recordError.firstRow;
			}
			if (toSkip != -1)
				error.printf("ERROR: File: '%s', retry with --skip %d%n", filename, toSkip);
		}

		if (options.noAnalysis)
			System.exit(0);

		// Validate the result of the analysis if requested
		final int[] matched = new int[numFields];
		final int[] nulls = new int[numFields];
		final int[] blanks = new int[numFields];
		final Set<String> failures = new HashSet<>();

		// Check the RegExp at level 2 validation
		if (options.validate == 2) {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename)), options.charset))) {

				final CsvParser parser = new CsvParser(settings);
				parser.beginParsing(in);
				numFields = parser.getRecordMetadata().headers().length;

				final TextAnalysisResult[] results = processor.getResult();
				final Pattern[] patterns = new Pattern[numFields];

				for (int i = 0; i < numFields; i++)
					if (options.col == -1 || options.col == i)
						patterns[i] = Pattern.compile(results[i].getRegExp());

				rawRecordIndex = 0;
				String[] row;

				while ((row = parser.parseNext()) != null) {
					rawRecordIndex++;
					if (row.length != numFields)
						continue;

					for (int i = 0; i < numFields; i++) {
						if (options.col == -1 || options.col == i) {
							final String value = row[i];
							if (value == null)
								nulls[i]++;
							else if (value.trim().isEmpty())
								blanks[i]++;
							else if (patterns[i].matcher(value).matches())
								matched[i]++;
							else if (options.verbose != 0)
								failures.add(value);
						}
					}
					if (rawRecordIndex == options.recordsToProcess) {
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
		if (options.json)
			output.printf("[%n");
		final TextAnalysisResult[] results = processor.getResult();
		for (int i = 0; i < numFields; i++) {
			if (options.col == -1 || options.col == i) {
				final TextAnalyzer analyzer = processor.getAnalyzer(i);
				if (rawRecordIndex != options.recordsToProcess)
					analyzer.setTotalCount(rawRecordIndex);

				result = results[i];
				if (options.json) {
					if (i != 0 && options.col == -1)
						output.printf(",");
				}
				else
					output.printf("Field '%s' (%d) - ", sanitize(analyzer.getStreamName()), i);
				output.printf("%s%n", result.asJSON(options.pretty, options.verbose));

				if (options.pluginDefinition) {
					final ObjectNode pluginDefinition = result.asPlugin(analyzer);
					if (pluginDefinition != null) {
						final ObjectMapper mapper = new ObjectMapper();

						final ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
						try {
							output.printf("%s%n", writer.writeValueAsString(pluginDefinition));
						} catch (JsonProcessingException e) {
							throw new FTAProcessingException(filename, "JsonProcessing exception", e);
						}
					}
				}
				if (result.getType() != null)
					typesDetected++;
				matchCount += result.getMatchCount();
				sampleCount += result.getSampleCount();

				// Check the counts if we are validating
				if (options.validate >= 1) {
					final String ret = result.checkCounts();
					if (ret != null)
						throw new FTAProcessingException(filename,
								MessageFormat.format("Composite: {0}, field: {1} ({2}), failed count validation - {3}",
								analyzer.getContext().getCompositeName(), analyzer.getContext().getStreamName(),
								analyzer.getContext().getStreamIndex(), ret));
				}

				if (options.validate == 2 && matched[i] != result.getMatchCount()) {
					if (result.isSemanticType())
						if (matched[i] > result.getMatchCount())
							error.printf("\t*** Warning: Match Count via RegExp (%d) > LogicalType match analysis (%d) ***%n", matched[i], result.getMatchCount());
						else
							error.printf("\t*** Error: Match Count via RegExp (%d) < LogicalType match analysis (%d) ***%n", matched[i], result.getMatchCount());
					else
						error.printf("\t*** Error: Match Count via RegExp (%d) does not match analysis (%d) ***%n", matched[i], result.getMatchCount());
					if (options.verbose != 0) {
						error.println("Failed to match:");
						for (final String failure : failures)
							error.println("\t" + failure);
					}
				}
			}
		}
		if (options.json)
			output.printf("]%n");
		resultsTime = System.currentTimeMillis();

	    final Runtime instance = Runtime.getRuntime();
		final double usedMemory = (instance.totalMemory() - instance.freeMemory()) / (1024 * 1024);
		final long durationTime = System.currentTimeMillis() - startTime;

		if (!options.json) {
			if (options.col == -1) {
				final double percentage = numFields == 0 ? 0 : ((double)typesDetected*100)/numFields;
				error.printf("Summary: File: %s, Types detected %d of %d (%.2f%%), Matched %d, Samples %d, Used Memory: %.2fMB.%n",
						filename, typesDetected, numFields, percentage, matchCount, sampleCount, usedMemory);
			}
			else {
				final double confidence = result == null ? 0 : result.getConfidence();
				error.printf("Summary: Type detected: %s, Matched %d, Samples %d (Confidence: %.2f%%), Used Memory: %.2fMB.%n",
						(typesDetected == 1 ? "yes" : "no"), matchCount,
						sampleCount, confidence*100, usedMemory);
			}
			error.printf("Execution time (#fields: %d, #records: %d): initialization: %dms, consumption: %dms, results: %dms, total: %dms%n",
					numFields, rawRecordIndex, initializedTime - startTime, consumedTime - initializedTime, resultsTime - consumedTime, durationTime);
		}
	}
}
