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

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;
import com.cobber.fta.plugins.Gender;

public class TestBulk {
	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void basicBulk() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicBulk");
		analysis.setTrace("enabled=true");

		final HashMap<String, Long> basic = new HashMap<>();
		basic.put("Male", 2000000L);
		basic.put("Female", 1000000L);
		basic.put("", 1000000L);
		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), 4000000);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(),  Gender.SEMANTIC_TYPE + "EN");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 1000000L);
		assertEquals(result.getRegExp(), "(?i)(FEMALE|MALE)");
		assertEquals(result.getMatchCount(), 3000000);
		assertEquals(result.getConfidence(), 1.0);
		final Map<String, Long> details = result.getCardinalityDetails();
		assertEquals(details.get("MALE"), Long.valueOf(2000000));
		assertEquals(details.get("FEMALE"), Long.valueOf(1000000));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void basicBulkFromDB() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicBulk");
		analysis.setKeyConfidence(1.0);
		analysis.setTotalCount(4_000_000L);

		final HashMap<String, Long> basic = new HashMap<>();
		basic.put("Male", 2_000_000L);
		basic.put("Female", 1_000_000L);
		basic.put("", 1_000_000L);
		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), 4000000);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(),  Gender.SEMANTIC_TYPE + "EN");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 1000000L);
		assertEquals(result.getRegExp(), "(?i)(FEMALE|MALE)");
		assertEquals(result.getMatchCount(), 3000000);
		assertEquals(result.getConfidence(), 1.0);
		final Map<String, Long> details = result.getCardinalityDetails();
		assertEquals(details.get("MALE"), Long.valueOf(2000000));
		assertEquals(details.get("FEMALE"), Long.valueOf(1000000));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void bulkLong() throws IOException, FTAException {
		final TextAnalyzer analysisBulk = new TextAnalyzer("baseline");
		final long SAMPLE_COUNT = 100L;

		Map<String, Long> simple = new HashMap<>();
		simple.put("100", SAMPLE_COUNT);
		simple.put("200", SAMPLE_COUNT);
		analysisBulk.trainBulk(simple);

		final TextAnalysisResult resultBulk = analysisBulk.getResult();
		TestUtils.checkSerialization(analysisBulk);

		assertEquals(resultBulk.getSampleCount(), 2 * SAMPLE_COUNT);
		assertEquals(resultBulk.getType(), FTAType.LONG);
		assertNull(resultBulk.getTypeQualifier());
		assertEquals(resultBulk.getNullCount(), 0);
		assertEquals(resultBulk.getBlankCount(), 0);
		assertEquals(resultBulk.getRegExp(), "\\d{3}");
		assertEquals(resultBulk.getMatchCount(), 2 * SAMPLE_COUNT);
		assertEquals(resultBulk.getConfidence(), 1.0);
		final Map<String, Long> details = resultBulk.getCardinalityDetails();
		assertEquals(details.get("100"), SAMPLE_COUNT);
		assertEquals(details.get("200"), SAMPLE_COUNT);

		final TextAnalyzer analysis = new TextAnalyzer("reference");
		for (int i = 0; i < SAMPLE_COUNT; i++) {
			analysis.train("100");
			analysis.train("200");
		}
		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		// There will be very small errors due to the random nature of the data stream
		assertEquals(resultBulk.getMean(),result.getMean(), TestUtils.EPSILON);
		assertEquals(resultBulk.getStandardDeviation(),result.getStandardDeviation(), TestUtils.EPSILON);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void basicBulkSignature() throws IOException, FTAException {
		final TextAnalyzer analysisBulk = new TextAnalyzer("basicBulkSignature_bulk");
		final TextAnalyzer analysis = new TextAnalyzer("basicBulkSignature");
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
		TestUtils.checkSerialization(analysis);

		assertEquals(resultBulk.getSampleCount(), 3 * ITERATIONS);
		assertEquals(resultBulk.getType(), FTAType.STRING);
		assertEquals(resultBulk.getTypeQualifier(),  Gender.SEMANTIC_TYPE + "EN");
		assertEquals(resultBulk.getNullCount(), 0);
		assertEquals(resultBulk.getRegExp(), "(?i)(FEMALE|MALE)");
		assertEquals(resultBulk.getMatchCount(), 3 * ITERATIONS);
		assertEquals(resultBulk.getConfidence(), 1.0);
		final Map<String, Long> details = resultBulk.getCardinalityDetails();
		assertEquals(details.get("MALE"), Long.valueOf(2 * ITERATIONS));
		assertEquals(details.get("FEMALE"), Long.valueOf(ITERATIONS));

		assertEquals(resultBulk.getStructureSignature(), result.getStructureSignature());
		assertEquals(resultBulk.getDataSignature(), result.getDataSignature());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void basicDistance() throws IOException, FTAException {
		final TextAnalyzer analysisBulk = new TextAnalyzer("basicDistance");
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

		assertEquals(resultBulk.getSampleCount(), SAMPLES);
		assertEquals(resultBulk.getType(), FTAType.STRING);
		assertNull(resultBulk.getTypeQualifier());
		assertEquals(resultBulk.getNullCount(), 0);
		assertEquals(resultBulk.getRegExp(), "(?i)(DISCONNECT|DISCONNECT FRACTIONAL|DISCONNECT OTHER|DISCONNECT STILL BILLING|INSTALL FRACTIONAL|INSTALL FRACTIONAL RERATE|RE-RATES|RUN RATE)");
		assertEquals(resultBulk.getMatchCount(), SAMPLES);
		assertEquals(resultBulk.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void basicDate() throws IOException, FTAException {
		final TextAnalyzer analysisBulk = new TextAnalyzer("ModifiedDate", DateResolutionMode.Auto);

		final HashMap<String, Long> basic = new HashMap<>();
		basic.put("2002-06-01 00:00:00", 10L);
		basic.put("2008-03-11 10:17:21", 99L);
		analysisBulk.trainBulk(basic);
		final TextAnalysisResult resultBulk = analysisBulk.getResult();

		assertEquals(resultBulk.getSampleCount(), 109);
		assertEquals(resultBulk.getType(), FTAType.LOCALDATETIME);
		assertEquals(resultBulk.getTypeQualifier(), "yyyy-MM-dd HH:mm:ss");
		assertEquals(resultBulk.getNullCount(), 0);
		assertEquals(resultBulk.getRegExp(), "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
		assertEquals(resultBulk.getMatchCount(), 109);
		assertEquals(resultBulk.getConfidence(), 1.0);
		assertEquals(resultBulk.getName(), "ModifiedDate");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void justBlanks() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("justBlanks");

		final HashMap<String, Long> basic = new HashMap<>();
		basic.put("", 1000000L);
		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), 1000000);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "BLANK");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 1000000);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_BLANK));
		assertEquals(result.getMatchCount(), 0);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getCardinality(), 0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void dateBug() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("dateBug");
		final int SAMPLES = 26;

		final HashMap<String, Long> basic = new HashMap<>();
		basic.put("2016-10-10 17:12:06.263", 1L);
		basic.put("2016-11-18 12:42:45.98", 2L);
		basic.put("2016-10-10 17:12:06.267", 3L);
		basic.put("2016-10-10 17:11:58.61", 2L);
		basic.put("2018-04-03 00:00:00.0", 8L);
		basic.put("2017-11-16 13:03:00.0", 4L);
		basic.put("2016-11-18 12:42:45.977",2L);
		basic.put("2017-08-09 15:29:22.647", 1L);
		basic.put("2016-10-10 17:11:58.613", 3L);

		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), SAMPLES);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeQualifier(), "yyyy-MM-dd HH:mm:ss.S{1,3}");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{1,3}");
		assertEquals(result.getMatchCount(), SAMPLES);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getCardinality(), 9);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void dateFieldDot6() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("dateFieldDot6", DateResolutionMode.Auto);
		final int SAMPLES = 40;

		final HashMap<String, Long> basic = new HashMap<>();
		basic.put("1970-01-01 10:22:45.000000", 10L);
		basic.put("1970-01-01 04:10:32.000000", 10L);
		basic.put("1970-01-01 05:28:44.000000", 10L);
		basic.put("1970-01-01 05:26:45.000000", 10L);

		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), SAMPLES);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeQualifier(), "yyyy-MM-dd HH:mm:ss.SSSSSS");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{6}");
		assertEquals(result.getMatchCount(), SAMPLES);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getCardinality(), 4);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void dateFieldDot7() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("dateFieldDot7", DateResolutionMode.Auto);
		analysis.setTrace("enabled=true");
		final int SAMPLES = 50;

		final HashMap<String, Long> basic = new HashMap<>();
		basic.put("2021-03-16 00:00:00.0000000", 10L);
		basic.put("2021-07-26 00:00:00.0000000", 10L);
		basic.put("2020-05-10 00:00:00.0000000", 10L);
		basic.put("2021-02-08 00:00:00.0000000", 10L);
		basic.put(null, 10L);

		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), SAMPLES);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeQualifier(), "yyyy-MM-dd HH:mm:ss.SSSSSSS");
		assertEquals(result.getNullCount(), 10);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{7}");
		assertEquals(result.getMatchCount(), SAMPLES - result.getNullCount());
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getCardinality(), 4);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void industrySemantic() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("industrySemantic", DateResolutionMode.Auto);
		analysis.setTrace("enabled=true,samples=1000");
		analysis.setDebug(2);
		final int SAMPLES = 355;

		final HashMap<String, Long> basic = new HashMap<>();
		basic.put("General Business", 200L);
		basic.put(null, 68L);
		basic.put("Financial Services & Insurance", 40L);
		basic.put("Healthcare Insurance", 31L);
		basic.put("Media & Communication", 7L);
		basic.put("Electricity, Oil & Gas", 6L);
		basic.put("Insurance", 3L);

		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), SAMPLES);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getMatchCount(), SAMPLES - result.getNullCount());
		assertEquals(result.getNullCount(), 68);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getCardinality(), 6);
		assertEquals(result.getTypeQualifier(), "INDUSTRY_EN");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void countryBaseLine() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("country", DateResolutionMode.Auto);
		analysis.setTrace("enabled=true,samples=1000");
		analysis.setDebug(2);
		final int GOOD_SAMPLES = 400;
		final int BAD_SAMPLES = 6;

		final HashMap<String, Long> basic = new HashMap<>();
		basic.put("AMERICAN SAMOA", 20L);
		basic.put("BRITISH VIRGIN ISLANDS", 20L);
		basic.put("FALKLAND ISLANDS", 20L);
		basic.put("HONG KONG", 20L);
		basic.put("NEW ZEALAND", 20L);
		basic.put("NORTH KOREA", 20L);
		basic.put("PAPUA NEW GUINEA", 20L);
		basic.put("PUERTO RICO", 20L);
		basic.put("SAINT LUCIA", 20L);
		basic.put("SOUTH AFRICA", 20L);
		basic.put("SOUTH KOREA", 20L);
		basic.put("UNITED KINGDOM", 20L);
		basic.put("UNITED STATES OF AMERICA", 20L);
		basic.put("UNITED ARAB EMIRATES", 20L);
		basic.put("TRINIDAD AND TOBAGO", 20L);
		basic.put("VATICAN CITY", 20L);
		basic.put("VIET NAM", 20L);
		basic.put("WEST BANK", 20L);
		basic.put("WESTERN SAHARA", 20L);
		basic.put("VIRGIN ISLANDS", 20L);

		basic.put("Rubbish that looks like text.", 2L);
		basic.put("Garbage, and other recyclables.", 2L);
		basic.put("Trash, not to be recycled.", 2L);

		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), GOOD_SAMPLES + BAD_SAMPLES);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "COUNTRY.TEXT_EN");
		assertEquals(result.getMatchCount(), GOOD_SAMPLES);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getCardinality(), 20);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void countryOutliers() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("country", DateResolutionMode.Auto);
		analysis.setTrace("enabled=true,samples=1000");
		analysis.setDebug(2);
		final int GOOD_SAMPLES = 400;
		final int BAD_SAMPLES = 90;

		final HashMap<String, Long> basic = new HashMap<>();
		basic.put("AMERICAN SAMOA", 20L);
		basic.put("BRITISH VIRGIN ISLANDS", 20L);
		basic.put("FALKLAND ISLANDS", 20L);
		basic.put("HONG KONG", 20L);
		basic.put("NEW ZEALAND", 20L);
		basic.put("NORTH KOREA", 20L);
		basic.put("PAPUA NEW GUINEA", 20L);
		basic.put("PUERTO RICO", 20L);
		basic.put("SAINT LUCIA", 20L);
		basic.put("SOUTH AFRICA", 20L);
		basic.put("SOUTH KOREA", 20L);
		basic.put("UNITED KINGDOM", 20L);
		basic.put("UNITED STATES OF AMERICA", 20L);
		basic.put("UNITED ARAB EMIRATES", 20L);
		basic.put("TRINIDAD AND TOBAGO", 20L);
		basic.put("VATICAN CITY", 20L);
		basic.put("VIET NAM", 20L);
		basic.put("WEST BANK", 20L);
		basic.put("WESTERN SAHARA", 20L);
		basic.put("VIRGIN ISLANDS", 20L);

		basic.put("Rubbish that looks like text.", 30L);
		basic.put("Garbage, and other recyclables.", 30L);
		basic.put("Trash, not to be recycled.", 30L);

		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), GOOD_SAMPLES + BAD_SAMPLES);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getMatchCount(), GOOD_SAMPLES + BAD_SAMPLES);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getCardinality(), 23);
		assertNull(result.getTypeQualifier());
	}
}
