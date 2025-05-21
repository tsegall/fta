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
package com.cobber.fta;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
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
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		analysis.setTrace("enabled=true");

		final Map<String, Long> basic = new HashMap<>();
		basic.put("Male", 2000000L);
		basic.put("Female", 1000000L);
		basic.put("", 1000000L);
		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), 4000000);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getSemanticType(),  Gender.SEMANTIC_TYPE + "EN");
		assertNull(result.getTypeModifier());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 1000000L);
		assertEquals(result.getRegExp(), "(?i)(FEMALE|MALE)");
		assertEquals(result.getMatchCount(), 3000000);
		assertEquals(result.getConfidence(), 1.0);
		final Map<String, Long> details = result.getCardinalityDetails();
		assertEquals(details.get("MALE"), 2000000L);
		assertEquals(details.get("FEMALE"), 1000000L);

		assertNull(result.checkCounts());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void bulkManyBlanks() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicBulk");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		analysis.setTrace("enabled=true");

		final Map<String, Long> basic = new HashMap<>();
		basic.put("Male", 200L);
		basic.put("Female", 100L);
		basic.put("", 1000000L);
		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), 1000300);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getSemanticType(),  Gender.SEMANTIC_TYPE + "EN");
		assertNull(result.getTypeModifier());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 1000000L);
		assertEquals(result.getRegExp(), "(?i)(FEMALE|MALE)");
		assertEquals(result.getMatchCount(), 300);
		assertEquals(result.getConfidence(), 1.0);
		final Map<String, Long> details = result.getCardinalityDetails();
		assertEquals(details.get("MALE"), 200L);
		assertEquals(details.get("FEMALE"), 100L);

		assertNull(result.checkCounts());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void bulkManyNulls() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicBulk");
		analysis.setLocale(Locale.forLanguageTag("en-US"));

		final Map<String, Long> basic = new HashMap<>();
		basic.put("Male", 200L);
		basic.put("Female", 100L);
		basic.put(null, 1000000L);
		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), 1000300);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getSemanticType(),  Gender.SEMANTIC_TYPE + "EN");
		assertNull(result.getTypeModifier());
		assertEquals(result.getNullCount(), 1000000L);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getRegExp(), "(?i)(FEMALE|MALE)");
		assertEquals(result.getMatchCount(), 300);
		assertEquals(result.getConfidence(), 1.0);
		final Map<String, Long> details = result.getCardinalityDetails();
		assertEquals(details.get("MALE"), 200L);
		assertEquals(details.get("FEMALE"), 100L);

		assertNull(result.checkCounts());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void bulkManyNullsSSN() throws IOException, FTAException {
		final long NULL_COUNT = 100_000_000L;
		final TextAnalyzer analysis = new TextAnalyzer("SSN");
		analysis.setLocale(Locale.forLanguageTag("en-US"));

		final Map<String, Long> basic = new HashMap<>();
		basic.put("837-22-8866", 1L);
		basic.put("726-68-8208", 1L);
		basic.put("341-91-4353", 1L);
		basic.put("338-14-8478", 1L);
		basic.put("120-71-7416", 1L);
		basic.put("663-16-9399", 1L);
		basic.put("596-15-0770", 1L);
		basic.put("346-62-7346", 1L);
		basic.put("158-56-9208", 1L);
		basic.put("483-22-6143", 1L);
		basic.put("608-02-5884", 1L);
		basic.put("852-93-0688", 1L);
		basic.put("038-88-1305", 1L);
		basic.put("842-75-7993", 1L);
		basic.put("241-69-5880", 1L);
		basic.put("530-34-1458", 1L);
		basic.put("069-68-8247", 1L);
		basic.put("821-56-1917", 1L);
		basic.put("554-70-1868", 1L);
		basic.put("636-15-6961", 1L);
		basic.put("328-55-2766", 1L);
		basic.put("186-37-4768", 1L);
		basic.put("417-98-8932", 1L);
		basic.put("447-14-1684", 1L);
		basic.put("157-46-2170", 1L);
		basic.put("267-28-3243", 1L);
		basic.put("112-12-8417", 1L);
		basic.put("026-01-6415", 1L);
		basic.put("347-13-9354", 1L);
		basic.put("059-82-2748", 1L);
		basic.put("193-21-2069", 1L);
		basic.put("820-73-9678", 1L);
		basic.put("507-50-4273", 1L);
		basic.put("426-81-8913", 1L);
		basic.put("543-27-0475", 1L);
		basic.put("725-62-8302", 1L);
		basic.put("575-20-5392", 1L);
		basic.put("786-51-1458", 1L);
		basic.put("334-51-4993", 1L);
		basic.put("694-47-6446", 1L);
		basic.put("266-88-7436", 1L);
		basic.put("321-51-1869", 1L);
		basic.put("326-32-6241", 1L);
		basic.put("282-37-2687", 1L);
		basic.put("751-83-4244", 1L);
		basic.put("575-20-8106", 1L);
		basic.put("686-35-9862", 1L);
		basic.put("676-92-3550", 1L);
		basic.put("366-15-1519", 1L);
		basic.put("792-03-1953", 1L);
		basic.put("379-77-1256", 1L);
		basic.put("480-71-4983", 1L);
		basic.put("692-33-7392", 1L);
		basic.put("800-22-4980", 1L);
		basic.put("170-87-4882", 1L);
		basic.put("367-57-8210", 1L);
		basic.put("634-63-7330", 1L);
		basic.put("222-73-2050", 1L);
		basic.put("847-68-4263", 1L);
		basic.put("041-06-6064", 1L);
		basic.put("334-92-8621", 1L);
		basic.put("082-71-0028", 1L);
		basic.put("314-45-5336", 1L);
		basic.put("132-09-9594", 1L);
		basic.put("133-71-3755", 1L);
		basic.put("139-62-8452", 1L);
		basic.put("192-86-7714", 1L);
		basic.put("376-04-9564", 1L);
		basic.put("551-83-6248", 1L);
		basic.put("199-13-9342", 1L);
		basic.put("221-28-9098", 1L);
		basic.put("430-41-9936", 1L);
		basic.put("239-83-5244", 1L);
		basic.put("545-80-2393", 1L);
		basic.put("412-67-7415", 1L);
		basic.put("188-44-2713", 1L);
		basic.put("465-97-7962", 1L);
		basic.put("157-95-5709", 1L);
		basic.put("664-37-0469", 1L);
		basic.put("826-83-0393", 1L);
		basic.put("866-15-9267", 1L);
		basic.put("365-05-2553", 1L);
		basic.put("386-38-4281", 1L);
		basic.put("020-22-1853", 1L);
		basic.put("620-99-3627", 1L);
		basic.put("766-68-2853", 1L);
		basic.put("879-43-9995", 1L);
		basic.put("386-43-0888", 1L);
		basic.put("217-68-1307", 1L);
		basic.put("512-90-3996", 1L);
		basic.put("566-86-9759", 1L);
		basic.put("760-28-4598", 1L);
		basic.put("485-25-7799", 1L);
		basic.put("346-11-9548", 1L);
		basic.put("667-79-9556", 1L);
		basic.put("702-90-3201", 1L);
		basic.put("662-74-0586", 1L);
		basic.put("695-29-3735", 1L);
		basic.put("066-40-1743", 1L);
		basic.put("719-91-3237", 1L);
		basic.put(null, NULL_COUNT);

		final long start = System.currentTimeMillis();
		analysis.trainBulk(basic);
		final TextAnalysisResult result = analysis.getResult();
		System.err.printf("Duration(ms): %d%n", System.currentTimeMillis() - start);
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), NULL_COUNT + 100);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getSemanticType(), "SSN");
		assertNull(result.getTypeModifier());
		assertEquals(result.getNullCount(), NULL_COUNT);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getMatchCount(), 100);
		assertEquals(result.getConfidence(), 1.0);

		assertNull(result.checkCounts());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void bulkManyNA_SSN() throws IOException, FTAException {
		final long BAD_COUNT = 100_000_000L;
		final TextAnalyzer analysis = new TextAnalyzer("SSN");
		analysis.setLocale(Locale.forLanguageTag("en-US"));

		final Map<String, Long> basic = new HashMap<>();
		basic.put("837-22-8866", 1L);
		basic.put("726-68-8208", 1L);
		basic.put("341-91-4353", 1L);
		basic.put("338-14-8478", 1L);
		basic.put("120-71-7416", 1L);
		basic.put("663-16-9399", 1L);
		basic.put("596-15-0770", 1L);
		basic.put("346-62-7346", 1L);
		basic.put("158-56-9208", 1L);
		basic.put("483-22-6143", 1L);
		basic.put("608-02-5884", 1L);
		basic.put("852-93-0688", 1L);
		basic.put("038-88-1305", 1L);
		basic.put("842-75-7993", 1L);
		basic.put("241-69-5880", 1L);
		basic.put("530-34-1458", 1L);
		basic.put("069-68-8247", 1L);
		basic.put("821-56-1917", 1L);
		basic.put("554-70-1868", 1L);
		basic.put("636-15-6961", 1L);
		basic.put("328-55-2766", 1L);
		basic.put("186-37-4768", 1L);
		basic.put("417-98-8932", 1L);
		basic.put("447-14-1684", 1L);
		basic.put("157-46-2170", 1L);
		basic.put("267-28-3243", 1L);
		basic.put("112-12-8417", 1L);
		basic.put("026-01-6415", 1L);
		basic.put("347-13-9354", 1L);
		basic.put("059-82-2748", 1L);
		basic.put("193-21-2069", 1L);
		basic.put("820-73-9678", 1L);
		basic.put("507-50-4273", 1L);
		basic.put("426-81-8913", 1L);
		basic.put("543-27-0475", 1L);
		basic.put("725-62-8302", 1L);
		basic.put("575-20-5392", 1L);
		basic.put("786-51-1458", 1L);
		basic.put("334-51-4993", 1L);
		basic.put("694-47-6446", 1L);
		basic.put("266-88-7436", 1L);
		basic.put("321-51-1869", 1L);
		basic.put("326-32-6241", 1L);
		basic.put("282-37-2687", 1L);
		basic.put("751-83-4244", 1L);
		basic.put("575-20-8106", 1L);
		basic.put("686-35-9862", 1L);
		basic.put("676-92-3550", 1L);
		basic.put("366-15-1519", 1L);
		basic.put("792-03-1953", 1L);
		basic.put("379-77-1256", 1L);
		basic.put("480-71-4983", 1L);
		basic.put("692-33-7392", 1L);
		basic.put("800-22-4980", 1L);
		basic.put("170-87-4882", 1L);
		basic.put("367-57-8210", 1L);
		basic.put("634-63-7330", 1L);
		basic.put("222-73-2050", 1L);
		basic.put("847-68-4263", 1L);
		basic.put("041-06-6064", 1L);
		basic.put("334-92-8621", 1L);
		basic.put("082-71-0028", 1L);
		basic.put("314-45-5336", 1L);
		basic.put("132-09-9594", 1L);
		basic.put("133-71-3755", 1L);
		basic.put("139-62-8452", 1L);
		basic.put("192-86-7714", 1L);
		basic.put("376-04-9564", 1L);
		basic.put("551-83-6248", 1L);
		basic.put("199-13-9342", 1L);
		basic.put("221-28-9098", 1L);
		basic.put("430-41-9936", 1L);
		basic.put("239-83-5244", 1L);
		basic.put("545-80-2393", 1L);
		basic.put("412-67-7415", 1L);
		basic.put("188-44-2713", 1L);
		basic.put("465-97-7962", 1L);
		basic.put("157-95-5709", 1L);
		basic.put("664-37-0469", 1L);
		basic.put("826-83-0393", 1L);
		basic.put("866-15-9267", 1L);
		basic.put("365-05-2553", 1L);
		basic.put("386-38-4281", 1L);
		basic.put("020-22-1853", 1L);
		basic.put("620-99-3627", 1L);
		basic.put("766-68-2853", 1L);
		basic.put("879-43-9995", 1L);
		basic.put("386-43-0888", 1L);
		basic.put("217-68-1307", 1L);
		basic.put("512-90-3996", 1L);
		basic.put("566-86-9759", 1L);
		basic.put("760-28-4598", 1L);
		basic.put("485-25-7799", 1L);
		basic.put("346-11-9548", 1L);
		basic.put("667-79-9556", 1L);
		basic.put("702-90-3201", 1L);
		basic.put("662-74-0586", 1L);
		basic.put("695-29-3735", 1L);
		basic.put("066-40-1743", 1L);
		basic.put("719-91-3237", 1L);
		basic.put("Not Available", BAD_COUNT);

		analysis.trainBulk(basic);
		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), BAD_COUNT + 100);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getSemanticType(), "SSN");
		assertNull(result.getTypeModifier());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getMatchCount(), 100);
		assertEquals(result.getConfidence(), 0.95);

		assertNull(result.checkCounts());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void basicBulkFromDB() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicBulk");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		analysis.setKeyConfidence(1.0);
		analysis.setTotalCount(4_000_000L);

		final Map<String, Long> basic = new HashMap<>();
		basic.put("Male", 2_000_000L);
		basic.put("Female", 1_000_000L);
		basic.put("", 1_000_000L);
		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), 4000000);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getSemanticType(),  Gender.SEMANTIC_TYPE + "EN");
		assertNull(result.getTypeModifier());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 1000000L);
		assertEquals(result.getRegExp(), "(?i)(FEMALE|MALE)");
		assertEquals(result.getMatchCount(), 3000000);
		assertEquals(result.getConfidence(), 1.0);
		final Map<String, Long> details = result.getCardinalityDetails();
		assertEquals(details.get("MALE"), 2000000L);
		assertEquals(details.get("FEMALE"), 1000000L);

		assertNull(result.checkCounts());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void bulkLong() throws IOException, FTAException {
		final TextAnalyzer analysisBulk = new TextAnalyzer("baseline");
		final long SAMPLE_COUNT = 100L;

		final Map<String, Long> simple = new HashMap<>();
		simple.put("100", SAMPLE_COUNT);
		simple.put("200", SAMPLE_COUNT);
		analysisBulk.trainBulk(simple);

		final TextAnalysisResult resultBulk = analysisBulk.getResult();
		TestUtils.checkSerialization(analysisBulk);

		assertEquals(resultBulk.getSampleCount(), 2 * SAMPLE_COUNT);
		assertEquals(resultBulk.getType(), FTAType.LONG);
		assertNull(resultBulk.getTypeModifier());
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

		assertNull(result.checkCounts());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void basicBulkSignature() throws IOException, FTAException {
		final TextAnalyzer analysisBulk = new TextAnalyzer("basicBulkSignature_bulk");
		analysisBulk.setLocale(Locale.forLanguageTag("en-US"));
		final TextAnalyzer analysis = new TextAnalyzer("basicBulkSignature");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final long ITERATIONS = 10000;

		final Map<String, Long> basic = new HashMap<>();
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
		assertEquals(resultBulk.getSemanticType(),  Gender.SEMANTIC_TYPE + "EN");
		assertNull(result.getTypeModifier());
		assertEquals(resultBulk.getNullCount(), 0);
		assertEquals(resultBulk.getRegExp(), "(?i)(FEMALE|MALE)");
		assertEquals(resultBulk.getMatchCount(), 3 * ITERATIONS);
		assertEquals(resultBulk.getConfidence(), 1.0);
		final Map<String, Long> details = resultBulk.getCardinalityDetails();
		assertEquals(details.get("MALE"), 2 * ITERATIONS);
		assertEquals(details.get("FEMALE"), ITERATIONS);

		assertEquals(resultBulk.getStructureSignature(), result.getStructureSignature());
		assertEquals(resultBulk.getDataSignature(), result.getDataSignature());

		assertNull(result.checkCounts());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void basicDistance() throws IOException, FTAException {
		final TextAnalyzer analysisBulk = new TextAnalyzer("basicDistance");
		final long SAMPLES = 3622;

		final Map<String, Long> basic = new HashMap<>();
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
		assertNull(resultBulk.getTypeModifier());
		assertNull(resultBulk.getSemanticType());
		assertEquals(resultBulk.getNullCount(), 0);
		assertEquals(resultBulk.getRegExp(), "(?i)(DISCONNECT|DISCONNECT FRACTIONAL|DISCONNECT OTHER|DISCONNECT STILL BILLING|INSTALL FRACTIONAL|INSTALL FRACTIONAL RERATE|RE-RATES|RUN RATE)");
		assertEquals(resultBulk.getMatchCount(), SAMPLES);
		assertEquals(resultBulk.getConfidence(), 1.0);

		assertNull(resultBulk.checkCounts());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void basicDate() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("ModifiedDate", DateResolutionMode.Auto);
		analysis.setLocale(Locale.forLanguageTag("en-US"));

		final Map<String, Long> basic = new HashMap<>();
		basic.put("2002-06-01 00:00:00", 10L);
		basic.put("2008-03-11 10:17:21", 99L);
		analysis.trainBulk(basic);
		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), 109);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "yyyy-MM-dd HH:mm:ss");
		assertNull(result.getSemanticType());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
		assertEquals(result.getMatchCount(), 109);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getName(), "ModifiedDate");

		assertNull(result.checkCounts());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void justBlanks() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("justBlanks");

		final Map<String, Long> basic = new HashMap<>();
		basic.put("", 1000000L);
		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), 1000000);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeModifier(), "BLANK");
		assertNull(result.getSemanticType());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 1000000);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_BLANK));
		assertEquals(result.getMatchCount(), 0);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getCardinality(), 0);

		assertNull(result.checkCounts());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void dateBug() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("dateBug");
		final int SAMPLES = 26;

		final Map<String, Long> basic = new HashMap<>();
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
		assertEquals(result.getTypeModifier(), "yyyy-MM-dd HH:mm:ss.S{1,3}");
		assertNull(result.getSemanticType());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{1,3}");
		assertEquals(result.getMatchCount(), SAMPLES);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getCardinality(), 9);

		assertNull(result.checkCounts());
}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void dateFieldDot6() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("dateFieldDot6", DateResolutionMode.Auto);
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final int SAMPLES = 40;

		final Map<String, Long> basic = new HashMap<>();
		basic.put("1970-01-01 10:22:45.000000", 10L);
		basic.put("1970-01-01 04:10:32.000000", 10L);
		basic.put("1970-01-01 05:28:44.000000", 10L);
		basic.put("1970-01-01 05:26:45.000000", 10L);

		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), SAMPLES);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "yyyy-MM-dd HH:mm:ss.SSSSSS");
		assertNull(result.getSemanticType());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{6}");
		assertEquals(result.getMatchCount(), SAMPLES);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getCardinality(), 4);

		assertNull(result.checkCounts());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void dateFieldDot7() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("dateFieldDot7", DateResolutionMode.Auto);
		analysis.setTrace("enabled=true");
		final int SAMPLES = 50;

		final Map<String, Long> basic = new HashMap<>();
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
		assertEquals(result.getTypeModifier(), "yyyy-MM-dd HH:mm:ss.SSSSSSS");
		assertNull(result.getSemanticType());
		assertEquals(result.getNullCount(), 10);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{7}");
		assertEquals(result.getMatchCount(), SAMPLES - result.getNullCount());
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getCardinality(), 4);

		assertNull(result.checkCounts());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void industrySemantic() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("industrySemantic", DateResolutionMode.Auto);
		analysis.setTrace("enabled=true,samples=1000");
		analysis.setDebug(2);
		final int SAMPLES = 355;

		final Map<String, Long> basic = new HashMap<>();
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
		assertEquals(result.getSemanticType(), "INDUSTRY_EN");
		assertNull(result.getTypeModifier());

		assertNull(result.checkCounts());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void countryBaseLine() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("country", DateResolutionMode.Auto);
		analysis.setTrace("enabled=true,samples=1000");
		analysis.setDebug(2);
		final int GOOD_SAMPLES = 400;
		final int BAD_SAMPLES = 6;

		final Map<String, Long> basic = new HashMap<>();
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
		assertEquals(result.getSemanticType(), "COUNTRY.TEXT_EN");
		assertNull(result.getTypeModifier());
		assertEquals(result.getMatchCount(), GOOD_SAMPLES);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getCardinality(), 20);

		assertNull(result.checkCounts());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BULK })
	public void countryOutliers() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("country", DateResolutionMode.Auto);
		analysis.setTrace("enabled=true,samples=1000");
		analysis.setDebug(2);
		final int GOOD_SAMPLES = 400;
		final int BAD_SAMPLES = 120;

		final Map<String, Long> basic = new HashMap<>();
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
		basic.put("Detritus, that should be cleaned up.", 30L);

		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), GOOD_SAMPLES + BAD_SAMPLES);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getMatchCount(), GOOD_SAMPLES + BAD_SAMPLES);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getCardinality(), 24);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());

		assertNull(result.checkCounts());
	}
}
