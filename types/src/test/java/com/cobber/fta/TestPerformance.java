/*
 * Copyright 2017-2023 Tim Segall
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.dates.DateTimeParser;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

/**
 */
public class TestPerformance {
	private final Logger logger = LoggerFactory.getLogger("com.cobber.fta");

	@Test(groups = { TestGroups.ALL, TestGroups.PERFORMANCE })
	public void basePerformanceBulkString() throws IOException, FTAException {
		final TextAnalyzer analyzer = new TextAnalyzer("basePerformanceBulkString");
		final Map<String, Long> testCase = new HashMap<>();
		final long SIZE = 1_000_000_000L;

		testCase.put("RED", SIZE/4);
		testCase.put("BLUE", SIZE/4);
		testCase.put("GREEN", SIZE/4);
		testCase.put("BLACK", SIZE/4);

		analyzer.trainBulk(testCase);

		final TextAnalysisResult result = analyzer.getResult();

		assertEquals(result.getSampleCount(), SIZE);
		assertEquals(result.getMatchCount(), SIZE);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getInvalidCount(), 0);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), "(?i)(BLACK|BLUE|GREEN|RED)");
		assertNull(result.getTypeModifier());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PERFORMANCE })
	public void basePerformanceBulkDate() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basePerformanceBulkDate");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final Map<String, Long> testCase = new HashMap<>();
		final long SIZE = 1_000_000_000L;

		testCase.put("Tue Oct  4 16:04:19 PDT 2022", SIZE/4);
		testCase.put("Mon Oct 11 17:01:16 PDT 2021", SIZE/4);
		testCase.put("Mon May 18 21:01:27 PDT 1970", SIZE/4);
		testCase.put("Wed Dec  9 12:44:29 PDT 1959", SIZE/4);

		analysis.trainBulk(testCase);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), SIZE);
		assertEquals(result.getMatchCount(), SIZE);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getInvalidCount(), 0);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.ZONEDDATETIME);
		assertEquals(result.getTypeModifier(), "EEE MMM ppd HH:mm:ss z yyyy");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PERFORMANCE })
	public void basePerformanceLong() throws IOException, FTAException {
		final TextAnalyzer analyzer = new TextAnalyzer("basePerformanceLong");
		analyzer.setMaxCardinality(1_000_000);

		final Map<String, Long> testCase = new HashMap<>();
		final long SIZE = 1_000_000L;

		for (long l = 0; l < SIZE; l++)
			testCase.put(String.valueOf(l), l);

		final long start = System.currentTimeMillis();

		analyzer.trainBulk(testCase);

		final long trained = System.currentTimeMillis();

		final TextAnalysisResult result = analyzer.getResult();

		final long completed = System.currentTimeMillis();

		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getInvalidCount(), 0);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeModifier());
		assertEquals(result.getMean(), 666666.3333333203, .00001);
		assertEquals(result.getStandardDeviation(), 235702.14254410806, 0.00001);

		logger.info("Count {}, training: {}ms, result calc: {}ms, ~{} per second.",
				SIZE, trained - start, completed - trained, Math.round(SIZE/((double)(completed - start)/1000)));

	}

	@Test(groups = { TestGroups.ALL, TestGroups.PERFORMANCE })
	public void wideRecord() throws IOException, FTAException {
		final int iterations = 5;
		final CsvParserSettings settings = new CsvParserSettings();
		settings.setHeaderExtractionEnabled(true);

		for (int i = 0; i < iterations; i++) {
			long start = System.currentTimeMillis();
			try (BufferedReader in = new BufferedReader(new InputStreamReader(TestPlugins.class.getResourceAsStream("/enriched.csv"), StandardCharsets.UTF_8))) {

				final CsvParser parser = new CsvParser(settings);
				parser.beginParsing(in);

				final String[] header = parser.getRecordMetadata().headers();

				AnalyzerContext context = new AnalyzerContext(null, DateTimeParser.DateResolutionMode.Auto, "profile", header);
				TextAnalyzer textAnalyzer = new TextAnalyzer(context);
				textAnalyzer.setLocale(Locale.getDefault());
				RecordAnalyzer analyzer = new RecordAnalyzer(textAnalyzer);

				String[] row;
				int rows = 0;
				while ((row = parser.parseNext()) != null) {
					analyzer.train(row);
					rows++;
				}

				RecordAnalysisResult result = analyzer.getResult();

				System.out.printf("durations: %d ms, columns: %d, rows: %d%n",
						System.currentTimeMillis() - start, header.length, rows);
			}
		}
	}
}
