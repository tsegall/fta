/*
 * Copyright 2017-2026 Tim Segall
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

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAUnsupportedLocaleException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.dates.DateTimeParser;

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

/**
 */
public class TestPerformance {
	private final Logger logger = LoggerFactory.getLogger("com.cobber.fta");

	/**
	 * Baseline for plugin startup cost: measures the time to initialize 30 TextAnalyzers (one per column)
	 * across representative locales.  Run twice per locale — the first pass pays JSON parsing, CSV loading,
	 * and class loading; the second pass shows the steady-state per-instance cost that optimisations will reduce.
	 *
	 * Interpretation guide:
	 *   Pass 1 (cold): dominated by one-time JVM work — not the target for optimization.
	 *   Pass 2 (warm): dominated by per-instance work (filter scan + reflection + initialize) — this IS the target.
	 *   Per-col warm: warm total / 30 — the marginal cost of adding one more column to an analysis run.
	 *   Plugin count: how many semantic-type plugins were registered for that locale — drives both passes.
	 */
	@Test(groups = { TestGroups.ALL, TestGroups.PERFORMANCE })
	public void pluginStartupCost() throws FTAPluginException, FTAUnsupportedLocaleException {
		// Locales span the range from most plugins (en-US) down to a handful (de-DE), with ja-JP and fr-FR in between.
		final String[][] locales = {
			{ "en-US", "English (US)"  },
			{ "ja-JP", "Japanese"      },
			{ "fr-FR", "French"        },
			{ "de-DE", "German"        },
		};
		final int COLUMNS = 30;
		final int PASSES  = 2;

		System.out.printf("%nPlugin startup cost benchmark — %d columns per run, %d passes (cold then warm)%n", COLUMNS, PASSES);
		System.out.printf("%-22s  %8s  %9s  %9s  %12s%n",
				"Locale", "Plugins", "Pass 1 (ms)", "Pass 2 (ms)", "Per-col warm");
		System.out.printf("%-22s  %8s  %9s  %9s  %12s%n",
				"------", "-------", "-----------", "-----------", "------------");

		for (final String[] localeInfo : locales) {
			final Locale locale = Locale.forLanguageTag(localeInfo[0]);
			final long[] passTimes = new long[PASSES];
			int pluginCount = 0;

			for (int pass = 0; pass < PASSES; pass++) {
				final long start = System.nanoTime();
				for (int col = 0; col < COLUMNS; col++) {
					final TextAnalyzer ta = new TextAnalyzer("col" + col);
					ta.setLocale(locale);
					ta.train("sample");
					ta.getResult();
					// Capture plugin count once — same every column after warm-up.
					if (pass == 0 && col == 0)
						pluginCount = ta.getPlugins().getRegisteredSemanticTypes().size();
				}
				passTimes[pass] = System.nanoTime() - start;
			}

			final long warmMicrosPerCol = passTimes[1] / (COLUMNS * 1_000L);
			System.out.printf("%-22s  %8d  %9d  %9d  %9d µs%n",
					localeInfo[1] + " (" + localeInfo[0] + ")",
					pluginCount,
					passTimes[0] / 1_000_000,
					passTimes[1] / 1_000_000,
					warmMicrosPerCol);
		}
		System.out.println();
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PERFORMANCE })
	public void basePerformanceBulkString() throws IOException, FTAException {
		final TextAnalyzer analyzer = new TextAnalyzer("basePerformanceBulkString");
		final Map<String, Long> testCase = new HashMap<>();
		final long SIZE = 1_000_000_000L;

		testCase.put("RED", SIZE/4);
		testCase.put("BLUE", SIZE/4);
		testCase.put("GREEN", SIZE/4);
		testCase.put("BLAK", SIZE/4);

		analyzer.trainBulk(testCase);

		final TextAnalysisResult result = analyzer.getResult();

		assertEquals(result.getSampleCount(), SIZE);
		assertEquals(result.getMatchCount(), SIZE);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getInvalidCount(), 0);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), "(?i)(BLAK|BLUE|GREEN|RED)");
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
		int headerLength = 0;

		for (int i = 0; i < iterations; i++) {
			final long start = System.currentTimeMillis();
			try (BufferedReader in = new BufferedReader(new InputStreamReader(TestPlugins.class.getResourceAsStream("/enriched.csv"), StandardCharsets.UTF_8))) {
				final CsvReader<NamedCsvRecord> csv = CsvReader.builder().ofNamedCsvRecord(in);
				RecordAnalyzer analyzer = null;
				int rows = 0;
				for (final CloseableIterator<NamedCsvRecord> iter = csv.iterator(); iter.hasNext();) {
					final NamedCsvRecord rowRaw = iter.next();
					final String[] row = rowRaw.getFields().toArray(new String[0]);
					if (rows == 0) {
						final String[] header = rowRaw.getHeader().toArray(new String[0]);
						headerLength = header.length;
						final AnalyzerContext context = new AnalyzerContext(null, DateTimeParser.DateResolutionMode.Auto, "profile", header);
						final TextAnalyzer textAnalyzer = new TextAnalyzer(context);
						textAnalyzer.setLocale(Locale.getDefault());
						analyzer = new RecordAnalyzer(textAnalyzer);
					}
					analyzer.train(row);
					rows++;
				}

				RecordAnalysisResult result = analyzer.getResult();

				System.out.printf("durations: %d ms, columns: %d, rows: %d%n",
						System.currentTimeMillis() - start, headerLength, rows);
			}
		}
	}
}
