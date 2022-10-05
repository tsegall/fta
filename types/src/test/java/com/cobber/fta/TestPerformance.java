/*
 * Copyright 2017-2022 Tim Segall
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;

/**
 */
public class TestPerformance {
	private Logger logger = LoggerFactory.getLogger("com.cobber.fta");

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

		TextAnalysisResult result = analyzer.getResult();

		assertEquals(result.getSampleCount(), SIZE);
		assertEquals(result.getMatchCount(), SIZE);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getInvalidCount(), 0);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), "(?i)(BLACK|BLUE|GREEN|RED)");
		assertNull(result.getTypeQualifier());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PERFORMANCE })
	public void basePerformanceBulkDate() throws IOException, FTAException {
		final TextAnalyzer analyzer = new TextAnalyzer("basePerformanceBulkDate");
		final Map<String, Long> testCase = new HashMap<>();
		final long SIZE = 1_000_000_000L;

		testCase.put("Tue Oct  4 16:04:19 PDT 2022", SIZE/4);
		testCase.put("Mon Oct 11 17:01:16 PDT 2021", SIZE/4);
		testCase.put("Mon May 18 21:01:27 PDT 1970", SIZE/4);
		testCase.put("Wed Dec  9 12:44:29 PDT 1959", SIZE/4);

		analyzer.trainBulk(testCase);

		TextAnalysisResult result = analyzer.getResult();

		assertEquals(result.getSampleCount(), SIZE);
		assertEquals(result.getMatchCount(), SIZE);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getInvalidCount(), 0);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.ZONEDDATETIME);
		assertEquals(result.getTypeQualifier(), "EEE MMM ppd HH:mm:ss z yyyy");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PERFORMANCE })
	public void basePerformanceLong() throws IOException, FTAException {
		final TextAnalyzer analyzer = new TextAnalyzer("basePerformanceLong");
		analyzer.setMaxCardinality(1_000_000);

		final Map<String, Long> testCase = new HashMap<>();
		final long SIZE = 1_000_000L;

		for (long l = 0; l < SIZE; l++)
			testCase.put(String.valueOf(l), l);

		long start = System.currentTimeMillis();

		analyzer.trainBulk(testCase);

		long trained = System.currentTimeMillis();

		TextAnalysisResult result = analyzer.getResult();

		long completed = System.currentTimeMillis();

		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getInvalidCount(), 0);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeQualifier());
		assertEquals(result.getMean(), 666666.3333333203, .00001);
		assertEquals(result.getStandardDeviation(), 235702.14254410806, 0.00001);

		logger.info("Count {}, training: {}ms, result calc: {}ms, ~{} per second.",
				SIZE, trained - start, completed - trained, Math.round(SIZE/((double)(completed - start)/1000)));

	}
}
