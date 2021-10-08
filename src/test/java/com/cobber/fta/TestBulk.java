/*
 * Copyright 2017-2021 Tim Segall
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

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;
import com.cobber.fta.plugins.LogicalTypeGenderEN;

public class TestBulk {
	@Test
	public void basicBulk() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer();

		final HashMap<String, Long> basic = new HashMap<>();
		basic.put("Male", 2000000L);
		basic.put("Female", 1000000L);
		basic.put("", 1000000L);
		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), 4000000);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertEquals(result.getTypeQualifier(),  LogicalTypeGenderEN.SEMANTIC_TYPE);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getBlankCount(), 1000000L);
		Assert.assertEquals(result.getRegExp(), "(?i)(FEMALE|MALE)");
		Assert.assertEquals(result.getMatchCount(), 3000000);
		Assert.assertEquals(result.getConfidence(), 1.0);
		final Map<String, Long> details = result.getCardinalityDetails();
		Assert.assertEquals(details.get("MALE"), Long.valueOf(2000000));
		Assert.assertEquals(details.get("FEMALE"), Long.valueOf(1000000));
	}

	@Test
	public void basicBulkSignature() throws IOException, FTAException {
		final TextAnalyzer analysisBulk = new TextAnalyzer();
		final TextAnalyzer analysis = new TextAnalyzer();
		final long ITERATIONS = 10000;

		final HashMap<String, Long> basic = new HashMap<>();
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
		Assert.assertEquals(resultBulk.getType(), FTAType.STRING);
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
	public void basicDistance() throws IOException, FTAException {
		final TextAnalyzer analysisBulk = new TextAnalyzer();
		final long SAMPLES = 3622;

		final HashMap<String, Long> basic = new HashMap<>();
		basic.put("Disconnect Fractional", 100L);
		basic.put("Disconnect Other", 137L);
		basic.put("Disconnect Still Billing", 172L);
		basic.put("Disconnect", 217L);
		basic.put("Install Fractional Rerate", 33L);
		basic.put("Install Fractional", 1L);
		basic.put("Re-rates", 223L);
		basic.put("Run Rate", 2739L);
		analysisBulk.trainBulk(basic);
		final TextAnalysisResult resultBulk = analysisBulk.getResult();

		Assert.assertEquals(resultBulk.getSampleCount(), SAMPLES);
		Assert.assertEquals(resultBulk.getType(), FTAType.STRING);
		Assert.assertNull(resultBulk.getTypeQualifier());
		Assert.assertEquals(resultBulk.getNullCount(), 0);
		Assert.assertEquals(resultBulk.getRegExp(), "(?i)(DISCONNECT|DISCONNECT FRACTIONAL|DISCONNECT OTHER|DISCONNECT STILL BILLING|INSTALL FRACTIONAL|INSTALL FRACTIONAL RERATE|RE-RATES|RUN RATE)");
		Assert.assertEquals(resultBulk.getMatchCount(), SAMPLES);
		Assert.assertEquals(resultBulk.getConfidence(), 1.0);
	}

	@Test
	public void basicDate() throws IOException, FTAException {
		final TextAnalyzer analysisBulk = new TextAnalyzer("ModifiedDate", DateResolutionMode.Auto);

		final HashMap<String, Long> basic = new HashMap<>();
		basic.put("2002-06-01 00:00:00", 10L);
		basic.put("2008-03-11 10:17:21", 99L);
		analysisBulk.trainBulk(basic);
		final TextAnalysisResult resultBulk = analysisBulk.getResult();

		Assert.assertEquals(resultBulk.getSampleCount(), 109);
		Assert.assertEquals(resultBulk.getType(), FTAType.LOCALDATETIME);
		Assert.assertEquals(resultBulk.getTypeQualifier(), "yyyy-MM-dd HH:mm:ss");
		Assert.assertEquals(resultBulk.getNullCount(), 0);
		Assert.assertEquals(resultBulk.getRegExp(), "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
		Assert.assertEquals(resultBulk.getMatchCount(), 109);
		Assert.assertEquals(resultBulk.getConfidence(), 1.0);
		Assert.assertEquals(resultBulk.getName(), "ModifiedDate");
	}

	@Test
	public void justBlanks() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer();

		final HashMap<String, Long> basic = new HashMap<>();
		basic.put("", 1000000L);
		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), 1000000);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "BLANK");
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getBlankCount(), 1000000);
		Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_BLANK));
		Assert.assertEquals(result.getMatchCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getCardinality(), 0);
	}
}
