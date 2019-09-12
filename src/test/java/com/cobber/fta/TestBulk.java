/*
 * Copyright 2017-2019 Tim Segall
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.cobber.fta.plugins.LogicalTypeGenderEN;

public class TestBulk {
	@Test
	public void basicBulk() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		HashMap<String, Long> basic = new HashMap<>();
		basic.put("Male", 2000000L);
		basic.put("Female", 1000000L);
		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), 3000000);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(),  LogicalTypeGenderEN.SEMANTIC_TYPE);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "(?i)(FEMALE|MALE)");
		Assert.assertEquals(result.getMatchCount(), 3000000);
		Assert.assertEquals(result.getConfidence(), 1.0);
		final Map<String, Long> details = result.getCardinalityDetails();
		Assert.assertEquals(details.get("MALE"), Long.valueOf(2000000));
		Assert.assertEquals(details.get("FEMALE"), Long.valueOf(1000000));
	}

	@Test
	public void basicBulkSignature() throws IOException {
		final TextAnalyzer analysisBulk = new TextAnalyzer();
		final TextAnalyzer analysis = new TextAnalyzer();
		final long ITERATIONS = 10000;

		HashMap<String, Long> basic = new HashMap<>();
		basic.put("Male", 2 *  ITERATIONS);
		basic.put("Female", ITERATIONS);
		analysisBulk.trainBulk(basic);
		final TextAnalysisResult resultBulk = analysisBulk.getResult();

		for (int i = 0; i < ITERATIONS; i++) {
			analysis.train("Male");
			analysis.train("Male");
			analysis.train("Female");
		}
		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(resultBulk.getSampleCount(), 3 * ITERATIONS);
		Assert.assertEquals(resultBulk.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(resultBulk.getTypeQualifier(),  LogicalTypeGenderEN.SEMANTIC_TYPE);
		Assert.assertEquals(resultBulk.getNullCount(), 0);
		Assert.assertEquals(resultBulk.getRegExp(), "(?i)(FEMALE|MALE)");
		Assert.assertEquals(resultBulk.getMatchCount(), 3 * ITERATIONS);
		Assert.assertEquals(resultBulk.getConfidence(), 1.0);
		final Map<String, Long> details = resultBulk.getCardinalityDetails();
		Assert.assertEquals(details.get("MALE"), Long.valueOf(2 * ITERATIONS));
		Assert.assertEquals(details.get("FEMALE"), Long.valueOf(ITERATIONS));

		Assert.assertEquals(resultBulk.getStructureSignature(), result.getStructureSignature());
		Assert.assertEquals(resultBulk.getDataSignature(), result.getDataSignature());
	}

	@Test
	public void justBlanks() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		HashMap<String, Long> basic = new HashMap<>();
		basic.put("", 1000000L);
		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), 1000000);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "BLANK");
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getBlankCount(), 1000000);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_WHITESPACE);
		Assert.assertEquals(result.getMatchCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getCardinality(), 0);
	}
}
