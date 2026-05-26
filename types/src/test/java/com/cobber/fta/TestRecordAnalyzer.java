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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAMergeException;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAUnsupportedLocaleException;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;

public class TestRecordAnalyzer {

	private RecordAnalyzer buildAnalyzer(final String... fieldNames) {
		final AnalyzerContext context = new AnalyzerContext(null, DateResolutionMode.Auto,
				"test", fieldNames);
		final TextAnalyzer template = new TextAnalyzer(context);
		return new RecordAnalyzer(template);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void trainWrongLengthThrows() throws FTAPluginException, FTAUnsupportedLocaleException {
		final RecordAnalyzer ra = buildAnalyzer("col1", "col2");
		try {
			ra.train(new String[] { "only-one-value" });
			fail("Expected IllegalArgumentException for wrong array length");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void getAnalyzerByIndex() {
		final RecordAnalyzer ra = buildAnalyzer("col1", "col2");
		assertNotNull(ra.getAnalyzer(0));
		assertNotNull(ra.getAnalyzer(1));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void getAnalyzersLength() {
		final RecordAnalyzer ra = buildAnalyzer("col1", "col2", "col3");
		final TextAnalyzer[] analyzers = ra.getAnalyzers();
		assertNotNull(analyzers);
		assertEquals(analyzers.length, 3);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void mergeProducesResult() throws FTAException, FTAMergeException {
		final RecordAnalyzer ra1 = buildAnalyzer("id", "name");
		final RecordAnalyzer ra2 = buildAnalyzer("id", "name");

		for (int i = 1; i <= 20; i++) {
			ra1.train(new String[] { String.valueOf(i),       "Alice" });
			ra2.train(new String[] { String.valueOf(i + 20),  "Bob"   });
		}

		final RecordAnalyzer merged = RecordAnalyzer.merge(ra1, ra2);
		final RecordAnalysisResult result = merged.getResult();
		assertNotNull(result);
		assertEquals(result.getStreamResults().length, 2);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void trainAndGetResult() throws FTAException {
		final RecordAnalyzer ra = buildAnalyzer("age", "city");
		for (int i = 1; i <= 30; i++)
			ra.train(new String[] { String.valueOf(i), "London" });
		final RecordAnalysisResult result = ra.getResult();
		assertNotNull(result);
		assertEquals(result.getStreamResults().length, 2);
	}
}
