/*
 * Copyright 2017-2025 Tim Segall
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.CircularBuffer;
import com.cobber.fta.core.FTAMergeException;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAUnsupportedLocaleException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

class FileProcessor {
	private final DriverOptions options;
	private final PrintStream error;
	private final String filename;
	private PrintStream output;
	private final ObjectMapper mapper = new ObjectMapper();

	FileProcessor(final PrintStream error, final String filename, final DriverOptions cmdLineOptions) {
		this.error = error;
		this.filename = filename;
		this.options = new DriverOptions(cmdLineOptions);
	}

	static class ParserSettings {
		char delimiter;
		char quoteCharacter;
		boolean withBOM;
	}

	protected void process() throws IOException, FTAPluginException, FTAUnsupportedLocaleException, FTAProcessingException, FTAMergeException {
		if (Files.exists(Paths.get(filename + ".options")))
			options.addFromFile(filename + ".options");

		output = options.output ? new PrintStream(filename + ".out", StandardCharsets.UTF_8) : System.out;

		final ParserSettings settings = new ParserSettings();
		if (options.delimiter != null) {
			settings.delimiter = "\\t".equals(options.delimiter) ? '\t' : options.delimiter.charAt(0);
		}
		else
			settings.delimiter = ',';
		if (options.quoteChar != null)
			settings.quoteCharacter = options.quoteChar.charAt(0);
		else
			settings.quoteCharacter = '"';
		settings.withBOM = options.withBOM;

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

	static class RowCount {
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

	private void processBulk(final ParserSettings settings) throws IOException, FTAPluginException, FTAUnsupportedLocaleException, FTAProcessingException {
		final int FIELD_COUNT = 4;
		TextAnalyzer analyzer;
		TextAnalysisResult result;
        String previousKey = null;
        String key;
        String previousName = null;
        String name = null;
		final Map<String, Long> bulkMap = new HashMap<>();

		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename)), options.charset))) {
			final CsvReader<NamedCsvRecord> csv = CsvReader.builder().ofNamedCsvRecord(in);

			long thisRecord = 0;
			long totalCount = 0;
			NamedCsvRecord row;
			final Map<Integer, RowCount> errors = new HashMap<>();

			for (final CloseableIterator<NamedCsvRecord> iter = csv.iterator(); iter.hasNext();) {
				row = iter.next();
				if (thisRecord == 0 && row.getHeader().size() != FIELD_COUNT)
					throw new FTAProcessingException(filename,
							MessageFormat.format("Expected input with four columns (key,fieldName,fieldValue,fieldCount). {0} field(s) in input",
									row.getHeader().size()));
				thisRecord++;
				final int rowLength = row.getFieldCount();
				if (rowLength != FIELD_COUNT) {
					final RowCount existing = errors.get(rowLength);
					if (existing == null)
						errors.put(rowLength, new RowCount(rowLength, thisRecord));
					else
						errors.put(rowLength, existing.inc());
					continue;
				}
				key = row.getField(0);
				name = row.getField(1);
				final String fieldValue = row.getField(2);
				final Long fieldCount = Long.valueOf(row.getField(3).trim());
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
							filename, recordError.count, recordError.numFields, recordError.firstRow, FIELD_COUNT);
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

	private void processAllFields(final ParserSettings settings) throws IOException, FTAPluginException, FTAUnsupportedLocaleException, FTAProcessingException, FTAMergeException {
		final long startTime = System.currentTimeMillis();
		long initializedTime = -1;
		long consumedTime = -1;
		long resultsTime = -1;
		Processor processor = null;
		Processor altProcessor = null;
		String[] header = null;
		int numFields = 0;
		long rawRecordIndex = 0;
		final Map<Integer, RowCount> errors = new HashMap<>();

		int processedRecords = 0;
		try {
			CsvReader<NamedCsvRecord> csv;

			if (settings.withBOM)
				csv = CsvReader.builder()
					.fieldSeparator(settings.delimiter)
					.quoteCharacter(settings.quoteCharacter)
					.detectBomHeader(true)
					.skipEmptyLines(false)
					.ofNamedCsvRecord(Files.newInputStream(Path.of(filename)), Charset.forName(options.charset));
			else
				csv = CsvReader.builder()
						.fieldSeparator(settings.delimiter)
						.quoteCharacter(settings.quoteCharacter)
						.skipEmptyLines(false)
						.ofNamedCsvRecord(new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename)), options.charset)));

			// Skip the first <n> lines if requested
			if (options.skip != 0) {
				csv.skipLines(options.skip);
				rawRecordIndex += options.skip;
			}

			final String compositeName = com.cobber.fta.core.Utils.getBaseName(Paths.get(filename).getFileName().toString());
			final CircularBuffer buffer = new CircularBuffer(options.trailer + 1);

			int rowLength;

			for (final CloseableIterator<NamedCsvRecord> iter = csv.iterator(); iter.hasNext();) {
				final NamedCsvRecord rowRaw = iter.next();
				String[] row = rowRaw.getFields().toArray(new String[0]);
				rowLength = rowRaw.getFieldCount();
				// Are we looking at the header row?
				if (rawRecordIndex == options.skip) {
					numFields = rowRaw.getHeader().size();
					if (options.col > numFields)
						throw new FTAProcessingException(filename, MessageFormat.format("Column {0} does not exist.  Only {1} field(s) in input.", options.col, numFields));

					header = rowRaw.getHeader().toArray(new String[0]);
					for (int i = 0; i < numFields; i++) {
						if ((options.col == -1 || options.col == i) && options.verbose != 0 && options.noAnalysis)
							System.out.println(header[i]);
					}
					processor = new Processor(compositeName, header, options);
					if (options.testmerge != 0)
						altProcessor = new Processor(compositeName, header, options);
					initializedTime = System.currentTimeMillis();
				}
				rawRecordIndex++;
				// Skip blank lines
				if (row.length == 1 && row[0] == null)
					continue;
				if (rowLength != numFields) {
					final RowCount existing = errors.get(rowLength);
					if (existing == null)
						errors.put(rowLength, new RowCount(rowLength, rawRecordIndex));
					else
						errors.put(rowLength, existing.inc());
					continue;
				}
				buffer.add(row);
				if (!buffer.isFull())
					continue;
				row = buffer.get();
				processedRecords++;

				if (options.testmerge != 0) {
					if (processedRecords % 2 == 0)
						processor.consume(row);
					else
						altProcessor.consume(row);

					if (processedRecords % options.testmerge == 0) {
						processor = Processor.merge(processor, altProcessor);
						altProcessor = new Processor(compositeName, header, options);
					}
				}
				else
					processor.consume(row);

				if (processedRecords == options.getRecordsToProcess()) {
					break;
				}
			}
			consumedTime = System.currentTimeMillis();
		}
		catch (FileNotFoundException e) {
			throw new FTAProcessingException(filename, e.getMessage(), e);
		}

		if (options.testmerge != 0)
			processor = Processor.merge(processor, altProcessor);

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

		final TextAnalysisResult[] results = processor.getResult();

		// Check the RegExp at level 2 validation
		if (options.validate == 2) {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename)), options.charset))) {
				final CsvReader<NamedCsvRecord> csv = CsvReader.builder()
						.fieldSeparator(settings.delimiter)
						.quoteCharacter(settings.quoteCharacter)
						.detectBomHeader(settings.withBOM)
						.skipEmptyLines(false)
						.ofNamedCsvRecord(in);
				Pattern[] patterns = null;
				rawRecordIndex = 0;

				TextAnalyzer[] analyzers = null;
				for (final CloseableIterator<NamedCsvRecord> iter = csv.iterator(); iter.hasNext();) {
					final NamedCsvRecord rowRaw = iter.next();
					final String[] row = rowRaw.getFields().toArray(new String[0]);

					// Are we looking at the header row?
					if (rawRecordIndex == 0) {
						numFields = rowRaw.getFieldCount();
						analyzers = processor.getAnalyzers();
						patterns = new Pattern[numFields];

						for (int i = 0; i < numFields; i++)
							if (options.col == -1 || options.col == i)
								patterns[i] = Pattern.compile(results[i].getRegExp());
					}

					rawRecordIndex++;
					if (row.length != numFields)
						continue;

					for (int i = 0; i < numFields; i++) {
						if (options.col == -1 || options.col == i) {
							final String value = row[i];
							if (analyzers[i].isNullEquivalent(value))
								nulls[i]++;
							else if (value.trim().isEmpty())
								blanks[i]++;
							else if (patterns[i].matcher(value.trim()).matches())
								matched[i]++;
							else if (options.verbose != 0)
								failures.add(value);
						}
					}
					if (rawRecordIndex == options.getRecordsToProcess()) {
						break;
					}
				}
			}
		}

		int typesDetected = 0;
		long matchCount = 0;
		long sampleCount = 0;
		TextAnalysisResult result = null;
		final boolean outputJSON = "json".equalsIgnoreCase(options.outputFormat);
		final boolean outputFaker = "faker".equalsIgnoreCase(options.outputFormat);

		if (outputJSON)
			output.printf("[%n");

		final ArrayNode fieldsArray = mapper.createArrayNode();

		for (int i = 0; i < numFields; i++) {
			if (options.col == -1 || options.col == i) {
				final TextAnalyzer analyzer = processor.getAnalyzer(i);
				if (rawRecordIndex != options.getRecordsToProcess())
					analyzer.setTotalCount(processedRecords);

				result = results[i];
				if (outputJSON) {
					if (i != 0 && options.col == -1)
						output.printf(",");
					output.printf("%s%n", result.asJSON(options.pretty, options.verbose));
				}
				else if (outputFaker) {
					final ObjectNode fieldNode = mapper.createObjectNode();
					if (result.isSemanticType()) {
						fieldNode.put("fieldName", sanitize(analyzer.getStreamName()));
						fieldNode.put("index", i);
						fieldNode.put("type", result.getSemanticType());
						fieldsArray.add(fieldNode);
					}
					else {
						final double nullPercent = (double)result.getNullCount()/result.getSampleCount();
						final double blankPercent = (double)result.getBlankCount()/result.getSampleCount();
						fieldNode.put("fieldName", sanitize(analyzer.getStreamName()));
						fieldNode.put("index", i);
						fieldNode.put("type", result.getType().name());
						switch (result.getType()) {
						case LOCALDATE:
						case LOCALDATETIME:
						case LOCALTIME:
						case OFFSETDATETIME:
						case ZONEDDATETIME:
							fieldNode.put("low", result.getMinValue());
							fieldNode.put("high", result.getMaxValue());
							fieldNode.put("format", result.getTypeModifier());
							break;
						case DOUBLE:
							fieldNode.put("low", result.getMinValue());
							fieldNode.put("high", result.getMaxValue());
							fieldNode.put("format", "%.2f");
							break;
						case LONG:
							fieldNode.put("low", result.getMinValue());
							fieldNode.put("high", result.getMaxValue());
							break;
						case STRING:
							if (result.getCardinality() == 0) {
							}
							else if (result.getCardinality() > 20) {
								fieldNode.put("minLength", result.getMinLength());
								fieldNode.put("maxLength", result.getMaxLength());
								fieldNode.put("format", result.getRegExp());
							}
							else {
								final ArrayNode enumArray = mapper.createArrayNode();

								for (final String key : result.getCardinalityDetails().keySet())
									enumArray.add(key);
								fieldNode.set("values", enumArray);

								final Map<String, Long> map = result.getCardinalityDetails();
								map.keySet().stream().sorted().collect(Collectors.joining(","));
							}
							break;
						case BOOLEAN:
							fieldNode.put("format", result.getTypeModifier());
							break;
						}
						if (nullPercent != 0.0) {
							fieldNode.put("nullPercent", String.format("%.02f", nullPercent));
						}
						if (blankPercent != 0.0) {
							fieldNode.put("blankPercent", String.format("%.02f", blankPercent));
						}
						fieldsArray.add(fieldNode);
					}
				}

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
					String ret = result.checkCounts(options.testmerge != 0);

					if (ret == null && options.testmerge == 0 && result.getSampleCount() != processedRecords)
						ret = "Samples != # of records processed";

					if (ret != null)
						throw new FTAProcessingException(filename,
								MessageFormat.format("Composite: {0}, field: {1} ({2}), failed count validation - {3}",
								analyzer.getContext().getCompositeName(), analyzer.getContext().getStreamName(),
								analyzer.getContext().getStreamIndex(), ret));
				}

				if (options.validate == 2 && matched[i] != result.getMatchCount()) {
					final String logicalType = result.isSemanticType() ? "Logical Type " : "";

					if (matched[i] > result.getMatchCount())
						error.printf("\t*** NOTE: Composite: %s, field: %s (%d), match Count via RegExp (%d) > %smatch analysis (%d) ***%n",
								analyzer.getContext().getCompositeName(), analyzer.getContext().getStreamName(),
								analyzer.getContext().getStreamIndex(), matched[i], logicalType, result.getMatchCount());
					else
						error.printf("\t*** ERROR: Composite: %s, field: %s (%d), match Count via RegExp (%d) < %smatch analysis (%d) ***%n",
								analyzer.getContext().getCompositeName(), analyzer.getContext().getStreamName(),
								analyzer.getContext().getStreamIndex(), matched[i], logicalType, result.getMatchCount());

					if (options.verbose != 0) {
						error.println("Failed to match:");
						for (final String failure : failures)
							error.println("\t" + failure);
					}
				}
			}
		}


		if (outputJSON)
			output.printf("]%n");
		else if (outputFaker) {
			final ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
			try {
				output.printf("%s%n", writer.writeValueAsString(fieldsArray));
			} catch (JsonProcessingException e) {
				throw new FTAProcessingException(filename, "JsonProcessing exception", e);
			}

			output.printf("%n");
		}
		resultsTime = System.currentTimeMillis();

	    final Runtime instance = Runtime.getRuntime();
		final double usedMemory = (instance.totalMemory() - instance.freeMemory()) / (1024 * 1024);
		final long durationTime = System.currentTimeMillis() - startTime;

		if (options.verbose != 0) {
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
