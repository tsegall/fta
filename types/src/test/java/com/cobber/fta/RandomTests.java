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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.cobber.fta.TextAnalyzer.Feature;
import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.InternalErrorException;
import com.cobber.fta.core.Utils;
import com.cobber.fta.plugins.address.USZip5;
import com.cobber.fta.plugins.address.USZipPlus4;

public class RandomTests {
	private static final SecureRandom random = new SecureRandom();
	private final Logger logger = LoggerFactory.getLogger("com.cobber.fta");

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void getReflectionSampleSize() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("getReflectionSampleSize");

		assertEquals(analysis.getReflectionSampleSize(), TextAnalyzer.REFLECTION_SAMPLES);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void getDefaultSemanticTypesDefault() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("getDefaultLogicalTypesDefault");

		assertTrue(analysis.isEnabled(Feature.DEFAULT_SEMANTIC_TYPES));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setDefaultSemanticTypesTooLate() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("setDefaultLogicalTypesTooLate");

		analysis.train("Hello, World");

		try {
			analysis.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Cannot adjust feature 'DEFAULT_SEMANTIC_TYPES' once training has started");
			return;
		}
		fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void getAttributesNoStatistics() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("getMinValueNoStatistics");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		analysis.train("0");
		analysis.train("1");
		analysis.train("2");
		analysis.train("3");
		analysis.train("4");

		final TextAnalysisResult result = analysis.getResult();

		int failures = 0;
		try {
			result.getMinValue();
			failures++;
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Statistics not enabled.");
			return;
		}
		try {
			result.getMaxValue();
			failures++;
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Statistics not enabled.");
			return;
		}
		try {
			result.getTopK();
			failures++;
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Statistics not enabled.");
			return;
		}
		try {
			result.getBottomK();
			failures++;
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Statistics not enabled.");
			return;
		}
		try {
			result.getMean();
			failures++;
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Statistics not enabled.");
			return;
		}
		try {
			result.getStandardDeviation();
			failures++;
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Statistics not enabled.");
			return;
		}

		if (failures != 0)
			fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void dataRegExp() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("getMinValueNoStatistics");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		analysis.train("hello  ");
		analysis.train("  world");
		analysis.train("magic");
		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getRegExp(), "[ 	]*\\p{IsAlphabetic}{5}[ 	]*");
		assertEquals(result.getDataRegExp(), "\\p{IsAlphabetic}{5}");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void constantLongZeroes() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("constantZeroes");

		final long start = System.currentTimeMillis();
		for (int i = 0; i < 20_000_000; i++)
			analysis.train("0");

		final TextAnalysisResult result = analysis.getResult();
		System.err.printf("Duration(ms): %d%n", System.currentTimeMillis() - start);
		assertEquals(result.getRegExp(), "\\d");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void constantLongZeroesBulk() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("constantZeroes");
		final Map<String, Long> universe = new HashMap<>();
		universe.put("0", 200_000_000L);

		final long start = System.currentTimeMillis();
		analysis.trainBulk(universe);
		final TextAnalysisResult result = analysis.getResult();
		System.err.printf("Duration(ms): %d%n", System.currentTimeMillis() - start);
		assertEquals(result.getRegExp(), "\\d");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void constantDoubleZeroes() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("constantZeroes");

		final long start = System.currentTimeMillis();
		for (int i = 0; i < 20_000_000; i++)
			analysis.train("0.0");

		final TextAnalysisResult result = analysis.getResult();
		System.err.printf("Duration(ms): %d%n", System.currentTimeMillis() - start);
		assertEquals(result.getRegExp(), "\\d*\\.?\\d+");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void constantDoubleZeroesBulk() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("constantZeroes");
		final Map<String, Long> universe = new HashMap<>();
		universe.put("1.0", 200_000_000L);

		final long start = System.currentTimeMillis();
		analysis.trainBulk(universe);
		final TextAnalysisResult result = analysis.getResult();
		System.err.printf("Duration(ms): %d%n", System.currentTimeMillis() - start);
		assertEquals(result.getRegExp(), "\\d*\\.?\\d+");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setDefaultLogicalTypes() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("setDefaultLogicalTypes");
		analysis.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);
		assertFalse(analysis.isEnabled(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void inadequateData() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("inadequateData");
		final String[] inputs = "47|89|90|91".split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getMinValue(), "47");
		assertEquals(result.getMaxValue(), "91");

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void noData() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("noData");
		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_NULL));
		assertEquals(result.getConfidence(), 0.0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeModifier(), "NULL");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void rubbish() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("rubbish");
		final String[] inputs = "47|hello|hello,world|=====47=====|aaaa|0|12|b,b,b,b390|4083|ddd ddd|90|-------|+++++|42987|8901".split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), KnownTypes.freezeANY(1, 12, 1, 12, result.getLeadingWhiteSpace(), result.getTrailingWhiteSpace(), result.getMultiline()));
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getMinValue(), "+++++");
		assertEquals(result.getMaxValue(), "hello,world");
		assertEquals(result.getMinLength(), 1);
		assertEquals(result.getMaxLength(), 12);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void zip50() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("zip50");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		analysis.setPluginThreshold(90);
		int locked = -1;
		final int COUNT = 46;
		final int INVALID = 2;			// 10000, 10042 are invalid

		for (int i = 10000; i < 10000 + COUNT; i++) {
			if (analysis.train(String.valueOf(i)) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		final PluginDefinition defn = PluginDefinition.findByName("POSTAL_CODE.ZIP5_US");
		assertEquals(result.getSemanticType(), defn.semanticType);
		assertEquals(locked, -1);
		assertEquals(result.getSampleCount(), COUNT);
		assertEquals(result.getMatchCount(), COUNT - INVALID);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{5}");
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getMinValue(), "10001");
		assertEquals(result.getMaxValue(), "10045");

		for (int i = 10000; i < 10000 + COUNT; i++) {
			assertTrue(String.valueOf(i).matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void mean100() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("mean100");
		int locked = -1;
		final int COUNT = 100;
		int sum = 0;

		for (int i = 0; i < COUNT; i++) {
			sum += i;
			if (analysis.train(String.valueOf(i)) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, -1);
		assertEquals(result.getSampleCount(), COUNT);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2}");
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getSemanticType(), "IDENTIFIER");
		assertEquals(result.getMinValue(), "0");
		assertEquals(result.getMaxValue(), "99");
		assertNull(result.getTypeModifier());
		assertEquals(result.getConfidence(), 0.95);
		assertEquals(result.getMean(), (double)sum/COUNT);
		assertEquals(result.getStandardDeviation(), 28.86607004772212);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void limitedData() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("limitedData");
		final String[] inputs = "12|4|5|".split("\\|");
		final int pre = 3;
		final int post = 10;

		for (int i = 0; i < pre; i++)
			analysis.train("");
		for (final String input : inputs) {
			analysis.train(input);
		}
		for (int i = 0; i < post; i++)
			analysis.train("");

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), pre + inputs.length + post);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LONG);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void debugging() throws FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("employeeNumber");
		analysis.setTrace("enabled=true");
		analysis.setDebug(2);
		final String pipedInput =
				"F944255990|F944277490|F944277490|F944285690|F944285690|F944285690|F944285690|F944285690|F944296590|F944296590|" +
				"F944296590|F944296890|F944299990|F944299990|FN22844690|FN24121490|FN24122790|FN24623590|FN24628690|FN24628890|" +
				"FN27016490|FN27016890|FN27381590|FN27396790|FN29563390|FN29565590|FN29565790|FN29565990|FN29568490|FN29568890|" +
				"FN29584290|FN944102090|FN944104890|FN944106490|FN944108290|FN944113890|FN944118990|FN944124490|FN944124690|FN944124890|" +
				"¶ xyzzy ¶|" +     // MAGIC
				"FN944133090";
		final String inputs[] = pipedInput.split("\\|");

		assertNull(analysis.getTraceFilePath());

		try {
			for (final String input : inputs)
				analysis.train(input);
		}
		catch (InternalErrorException e) {
			// We expect this to happen ...
		}

		assertNotNull(analysis.getTraceFilePath());
		assertNotEquals(analysis.getTraceFilePath().indexOf("employeeNumber.fta"), -1);

		final TextAnalysisResult result = analysis.getResult();

		final int samplesProcessed = inputs.length - 1;

		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertEquals(result.getSampleCount(), samplesProcessed);
		assertEquals(result.getMatchCount(), samplesProcessed - result.getBlankCount() - 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getMinLength(), 9);
		assertEquals(result.getMaxLength(), 11);
		assertEquals(result.getRegExp(), KnownTypes.PATTERN_ALPHANUMERIC + "{10,11}");
		assertEquals(result.getConfidence(), 0.975609756097561);

		int matchCount = 0;
		for (int i = 0; i < samplesProcessed - 1; i++) {
			final String input = inputs[i];
			if (!input.trim().isEmpty() && input.matches(result.getRegExp()))
				matchCount++;
		}
		assertEquals(matchCount, result.getMatchCount());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testTrim() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("testTrim");
		final String pipedInput = " Hello|  Hello| Hello |  world  |    Hello   |      Hi        |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi        |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi        |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi        |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi          |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi        |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi        ";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), inputs.length + result.getNullCount());
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 1);
		assertEquals(result.getRegExp(), KnownTypes.PATTERN_WHITESPACE + "((?i)(HELLO|HI|WORLD))" + KnownTypes.PATTERN_WHITESPACE);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void wide() throws IOException, FTAException, FTAException, InterruptedException {
		final int COPIES = 100;
		final String demos[] = { "EMAIL", "COORDINATE.LATITUDE_DECIMAL", "CITY", "NAME.FIRST", "NAME.LAST" };
		final LogicalType[] logicals = new LogicalType[COPIES * demos.length];
		final TextAnalyzer[] analyzers = new TextAnalyzer[COPIES * demos.length];

		for (int copy = 0; copy < COPIES; copy++)
			for (int i = 0; i < demos.length; i++) {
				final PluginDefinition pluginDefinition = PluginDefinition.findByName(demos[i]);
				final int index = copy * demos.length + i;
				logicals[index] = LogicalTypeFactory.newInstance(pluginDefinition, new AnalysisConfig());
				analyzers[index] = new TextAnalyzer(demos[i]);
				analyzers[index].setLocale(Locale.forLanguageTag("en-US"));
			}

		for (int rows = 0; rows < 1000; rows++) {
			for (int field = 0; field < analyzers.length; field++) {
				analyzers[field].train(logicals[field].nextRandom());
			}
		}

		for (int field = 0; field < analyzers.length; field++) {
			final TextAnalysisResult result = analyzers[field].getResult();
			assertNotNull(result);
			assertEquals(result.getSemanticType(), demos[field%demos.length]);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void changeMind() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("changeMind");
		int locked = -1;

		for (int i = 0; i < 2 * AnalysisConfig.DETECT_WINDOW_DEFAULT; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i;
		}

		for (char ch = 'a'; ch <= 'z'; ch++) {
			analysis.train(String.valueOf(ch));
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertEquals(result.getSampleCount(), 2 * AnalysisConfig.DETECT_WINDOW_DEFAULT + 26);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), 2 * AnalysisConfig.DETECT_WINDOW_DEFAULT + 26);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), KnownTypes.PATTERN_ALPHANUMERIC + "{1,2}");
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void changeMindMinMax() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("changeMindMinMax");
		analysis.setThreshold(97);
		final String pipedInput =
				"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!!Volume!Volume!!!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!!Volume!Volume!!!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!!!!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!!!!Volume!Audio disc ; Volume!Volume!!!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Audio disc ; Volume!!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!!Volume!Volume!!!!!!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!!!Volume!Volume!Volume!Volume!Volume!!!Volume!Volume!Volume!" +
						"Volume!Volume!!!!!!!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"!Volume!Volume!!!Volume!Volume!Volume!!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!!Volume!Volume!Volume!!Volume!Volume!!Volume!Volume!!Volume!Volume!Volume!" +
						"Volume!!Volume!Volume!!Volume!!!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Online resource (ePub ebook)!Volume!Online resource (ePub ebook)!Volume!!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"!Volume!Volume!Volume!Volume!Volume!Volume!Volume!!Volume!Volume!Volume!Volume!Volume!!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Audio disc ; Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Online resource (ePub ebook)!Volume!Volume!Volume!" +
						"Volume!Volume!!!!Volume!Volume!Volume!Volume!Volume!Volume!Computer disc!Volume!Volume!Volume!Volume!Volume!" +
						"!Volume!Online resource!Volume!!Volume!!Volume!!Volume!!Volume!Online resource (PDF ebook ; ePub ebook)!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Online resource (ePub ebook)!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Audio disc ; Volume!" +
						"Volume!Volume!Volume!Volume!Volume!!Volume!Volume!!Volume!Volume!Volume!!Volume!Volume!!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!";
		final String inputs[] = pipedInput.split("\\!");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount() + result.getBlankCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getRegExp(), "(?i)(AUDIO DISC ; VOLUME|COMPUTER DISC|ONLINE RESOURCE|\\QONLINE RESOURCE (EPUB EBOOK)\\E|\\QONLINE RESOURCE (PDF EBOOK ; EPUB EBOOK)\\E|VOLUME)");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "Audio disc ; Volume");
		assertEquals(result.getMaxValue(), "Volume");

		final String regExp = result.getRegExp();
		for (final String input : inputs) {
			if (input.length() == 0)
				continue;
			assertTrue(input.matches(regExp), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testQualifierAlpha() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Alpha");
		final int STRING_LENGTH = 5;
		assertTrue(analysis.isEnabled(Feature.LENGTH_QUALIFIER));
		analysis.configure(Feature.LENGTH_QUALIFIER, false);
		assertFalse(analysis.isEnabled(Feature.LENGTH_QUALIFIER));
		final String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

		final int start = 10000;
		final int end = 99999;

		int locked = -1;

		for (int i = start; i < end; i++) {
			final StringBuilder sample = new StringBuilder(STRING_LENGTH);
			for (int j = 0; j < STRING_LENGTH; j++)
				sample.append(alphabet.charAt(random.nextInt(52)));
			if (analysis.train(sample.toString()) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{5}");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testQualifierAlphaNumeric() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("AlphaNumeric");
		analysis.configure(Feature.LENGTH_QUALIFIER, false);

		final int start = 10000;
		final int end = 99999;

		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train('A' + String.valueOf(i)) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}\\d{5}");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void change2() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("change2");
		final String pipedInput = "AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"Jan|Mar|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
				"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|Sep|Jan|Oct|Oct|Oct|" +
				"Jan|Mar|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
				"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|NA|Sep|Jan|Oct|Oct|Oct|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "(?i)(AB|APR|AUG|BC|DEC|FEB|JAN|JUL|JUN|MAR|MAY|MB|NA|NB|NL|NOV|NS|NT|NU|OCT|ON|PE|QC|SEP|SK|YT)");
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void mixedZipHypen() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("mixedZip");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final String[] inputs = {
				"98115-2654", "98007", "98042-8501", "98311-3239", "98074-3322", "98039", "98466-2041", "98136-2633", "98166-3212", "98042-8213",
				"98121", "98038-8314", "98112-4739", "98059-7315", "20017-4261", "21204-2055", "21158-3604", "21784", "21776-9719", "20854",
				"22201-2618", "20017-1513", "20016-8001", "20008-5941", "20904-1209", "20901-1040", "20901-3105", "20817-6330", "20164", "20008-2522",
				"20109-3364", "20112-2759", "20708-1401", "20169-2703", "20155-1824", "20854-5497", "20169-1224", "20194-4323", "20190-4969", "20783-3052",
				"20716-1843", "20772-3222", "20882-1614", "20007-4104", "20112-3041", "20902", "20874-2915", "22305", "20165-2810", "20110-5357",
				"21078", "20770-3514", "20032-4801", "20220-0001", "22304-2552", "20772-4505", "20747-5101", "20769-9031", "20715", "20785-4618",
				"20746-3425", "21030-2210", "21078-1828", "20708-9758", "21228", "20754-9574", "21157-7720", "21048", "22192", "22205-3163",
				"21122-5702", "21220-1613", "21228", "21102-2059", "21221-3530", "21210-1556", "21040-1054", "21202-3504", "21043-6929", "21224-2141",
				"21042", "21093-7547", "21001", "21087", "20772-4137", "21111-1120", "21228-5317", "20678-3443", "20639", "20772-8378",
				"20772", "20735-4560", "21220", "21060-7241", "21220", "21009", "21108", "21201-5097", "22202", "22202", "20036", "20024", "20566",
				"21771", "21117", "20005", "21770", "20613", "20009","21229", "21791", "", "22134", "", "", "21225", "20850-3164", "21230", "21236",
				"20190", "20910", "21225", "21409-6107", "20782-3952", "22201-5798", "21205", "22202", "21250-1000", "20015-2770",
				"21209-2101", "21227-4817", "21009", "21204-4310", "22205-3163", "20015-1009", "21029", "21228", "20855-1555", "21227-1056",
				"21157-6530", "21042-3629", "21044-1211", "21794-9604", "20007-4373", "21009", "20903-2019", "20906-5271", "22206", "20769-9161",
				"20019-6732", "20737-1046", "20872-1867", "21074", "20854-6209", "20818-1328", "20906", "20876", "20740-3170", "20112-4735",
				"21201", "22202", "20782-2335", "20166-7547", "20019-1501", "20743", "22046-4235", "21218", "20770-1410", "20817-5700",
				"20905-5003", "20833-1711", "20008-4701", "22201-4502", "20842-9062", "20639-3035", "20166-2117", "20169-1932", "20782-3952", "22203-2054",
				"20854-2983", "21222", "20772-4237", "20878", "20879", "20874-1517", "20879", "20705", "20165-2496", "20772-5035",
				"21001", "20878", "21161", "20170-3241", "22201-5798", "20015-2770", "20882-1266", "20854-3916", "20715-3102", "20747"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		final PluginDefinition defn = PluginDefinition.findByName("POSTAL_CODE.ZIP5_PLUS4_US");
		assertEquals(result.getSemanticType(), defn.semanticType);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length - result.getBlankCount());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), USZipPlus4.REGEXP_VARIABLE_HYPHEN);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			if (!input.isEmpty())
				assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void mixedZip() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("mixedZip");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final String[] inputs = {
				"981152654", "98007", "980428501", "983113239", "980743322", "98039", "984662041", "981362633", "981663212", "980428213",
				"98121", "980388314", "981124739", "980597315", "200174261", "212042055", "211583604", "21784", "217769719", "20854",
				"222012618", "200171513", "200168001", "200085941", "209041209", "209011040", "209013105", "208176330", "20164", "200082522",
				"201093364", "201122759", "207081401", "201692703", "201551824", "208545497", "201691224", "201944323", "201904969", "207833052",
				"207161843", "207723222", "208821614", "200074104", "201123041", "20902", "208742915", "22305", "201652810", "201105357",
				"21078", "207703514", "200324801", "202200001", "223042552", "207724505", "207475101", "207699031", "20715", "207854618",
				"207463425", "210302210", "210781828", "207089758", "21228", "207549574", "211577720", "21048", "22192", "222053163",
				"211225702", "212201613", "21228", "211022059", "212213530", "212101556", "210401054", "212023504", "210436929", "212242141",
				"21042", "210937547", "21001", "21087", "207724137", "211111120", "212285317", "206783443", "20639", "207728378",
				"20772", "207354560", "21220", "210607241", "21220", "21009", "21108", "212015097", "22202", "22202", "20036", "20024", "20566",
				"21771", "21117", "20005", "21770", "20613", "20009","21229", "21791", "", "22134", "", "", "21225", "208503164", "21230", "21236",
				"20190", "20910", "21225", "214096107", "207823952", "222015798", "21205", "22202", "212501000", "200152770",
				"212092101", "212274817", "21009", "212044310", "222053163", "200151009", "21029", "21228", "208551555", "212271056",
				"211576530", "210423629", "210441211", "217949604", "200074373", "21009", "209032019", "209065271", "22206", "207699161",
				"200196732", "207371046", "208721867", "21074", "208546209", "208181328", "20906", "20876", "207403170", "201124735",
				"21201", "22202", "207822335", "201667547", "200191501", "20743", "220464235", "21218", "207701410", "208175700",
				"209055003", "208331711", "200084701", "222014502", "208429062", "206393035", "201662117", "201691932", "207823952", "222032054",
				"208542983", "21222", "207724237", "20878", "20879", "208741517", "20879", "20705", "201652496", "207725035",
				"21001", "20878", "21161", "201703241", "222015798", "200152770", "208821266", "208543916", "207153102", "20747"

		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		final PluginDefinition defn = PluginDefinition.findByName("POSTAL_CODE.ZIP5_PLUS4_US");
		assertEquals(result.getSemanticType(), defn.semanticType);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length - result.getBlankCount());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), USZipPlus4.REGEXP_VARIABLE);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			if (!input.isEmpty())
				assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void stringZip() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("zip_code");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final String[] inputs = {
				"54814", "71644", "66016", /** leading I **/ "I2538", "78705", "37909", "39603", "32612", "65779", "49072",
				"85013", "07452", "43828", "90714", /** leading I **/ "I7375", "45787", "12305", "23328", "65108", "09468",
				"10028", "80621", "56382", /** leading I **/ "I0950", "24562", "57242", "35956", "96830", "19041", "75431",
				"02121", "30711", "58771", "63783", "18966", "27629", "19367", "06840", "54519", "47226",
				"57756", "56325", "05737", "07752", "72442", "98385", "45858", "27401", "69346", "17113",
				"29031", "30124", "30527", "33322", "80219", "75503", "92273", "33514", "27858", "61737",
				"18970", "80226", "76902", "98443", "71060", "50528", "76890", "94659", "25024", "93604",
				"30537", "54655", "94624", "26323", "13603", "84171", "95003", "02493", "70660", "95052",
				"66119", "65257", "62330", "55106", "08353", "63957", "12834", "49249", "12983", "92195",
				"34205", "23694", "09316", "67550", "97127", "54467", "22195", "39108", "62926", "14785",
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.LONG);
		final PluginDefinition defn = PluginDefinition.findByName("POSTAL_CODE.ZIP5_US");
		assertEquals(result.getSemanticType(), defn.semanticType);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length - result.getBlankCount() - 3);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), USZip5.REGEXP_ZIP5);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void trailingAM() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("trailingAM");
		final String inputs[] = {
				"02s500000023SQ3AAM", "02s5000000233ThAAI", "02s5000000238JRAAY", "02s500000023QCEAA2",
				"02s500000023QCFAA2", "02s500000023SKAAA2", "02s5000000233TgAAI", "02s500000023Sw9AAE",
				"02s500000023T0pAAE", "02s500000023U6FAAU", "02s500000023qQVAAY", "02s500000023qQWAAY",
				"02s500000023r2FAAQ", "02s500000023rFiAAI", "02s500000023x3qAAA", "02s50000002GgdtAAC",
				"02s50000002GgduAAC", "02s50000002GkKXAA0", "02s50000002GrukAAC", "02s50000002GrulAAC",
				"02s50000002GsLCAA0", "02s50000002HCnGAAW", "02s50000002HUaFAAW", "02s50000002HUaGAAW",
				"02s50000002HV82AAG", "02s50000002HjVvAAK", "02s50000002Hl4NAAS", "02s50000002HnXRAA0",
				"02s50000002Hq1sAAC", "02s50000002HrQPAA0", "02s50000002HrraAAC", "02s50000002HxoKAAS",
				"02s50000002I6lQAAS", "02s50000002I90MAAS", "02s50000002I93BAAS", "02s50000002I9CSAA0"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), KnownTypes.PATTERN_ALPHANUMERIC + "{18}");
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void frenchName() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("frenchName");
		final String pipedInput = "Adrien|Alain|Albert|Alexandre|Alexis|André|Antoine|Arnaud|Arthur|Aurélien|" +
				"Baptiste|Benjamin|Benoît|Bernard|Bertrand|Bruno|Cédric|Charles|Christian|Christophe|" +
				"Claude|Clément|Cyril|Damien|Daniel|David|Denis|Didier|Dominique|Dylan|" +
				"Emmanuel|Éric|Étienne|Enzo|Fabien|Fabrice|Florent|Florian|Francis|Franck|" +
				"François|Frédéric|Gabriel|Gaétan|Georges|Gérard|Gilbert|Gilles|Grégory|Guillaume|" +
				"Guy|Henri|Hervé|Hugo|Jacques|Jean|";
		//Jean-Claude|Jean-François|Jean-Louis|Jean-Luc|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), KnownTypes.PATTERN_ALPHA + "{3,10}");
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void basicLengthValidationBlanks() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Spaces");
		final int iters = 30;
		final Set<String> samples = new HashSet<>();

		int locked = -1;

		final StringBuilder sb = new StringBuilder("  ");

		for (int i = 0; i < iters; i++) {
			final String s = sb.toString();
			samples.add(s);
			if (analysis.train(s) && locked == -1)
				locked = i;
			sb.append(' ');
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_BLANK));
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeModifier(), "BLANK");
		assertEquals(result.getSampleCount(), iters);
		assertEquals(result.getBlankCount(), iters);
		assertEquals(result.getCardinality(), 0);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), iters - result.getBlankCount());
		assertEquals(result.getMinLength(), 2);
		assertEquals(result.getMaxLength(), 2 + iters - 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples) {
			if (sample.trim().length() > 0)
				assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void basicLengthValidationString() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Spaces");
		final int iters = 30;
		final Set<String> samples = new HashSet<>();

		int locked = -1;

		final StringBuilder sb = new StringBuilder("  ");

		for (int i = 0; i < iters; i++) {
			final String s = sb.toString();
			samples.add(s);
			if (analysis.train(s) && locked == -1)
				locked = i;
			sb.append(' ');
		}
		analysis.train("          abc          ");

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getRegExp(), KnownTypes.PATTERN_WHITESPACE + KnownTypes.PATTERN_ALPHA + "{3}" + KnownTypes.PATTERN_WHITESPACE);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertEquals(result.getSampleCount(), iters + 1);
		assertEquals(result.getBlankCount(), iters);
		assertEquals(result.getCardinality(), 1);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), 1);
		assertEquals(result.getMinLength(), 2);
		assertEquals(result.getMaxLength(), 2 + iters - 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples) {
			if (sample.trim().length() > 0)
				assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void variableSpacesFixedLength() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("variableSpacesFixedLength");
		final String pipedInput = "JMD     |JOD     |JPYP    |KESQ    |KGS     |KHR     |" +
				" AXN    | AOAZ   | B1D    | BIFD   | BSD    | BZD    | CZE    | CHF    |" +
				"  MzR   |  NIO   |  P2N   |  PLN   |  RWF   |  SDG   |  SHP   |  SLL   |" +
				"   SVQ  |   SYP  |   S33Z |   THB  |   TOP  |   TZS  |   UYE  |   VND  |" +
				"    CQP |    COU |    CRC |    CUC |    DJF |    EGP |    GLP |    GMD |" +
				"     APT|     CSU|     44S|    LFUA|XXXXXXXX|     PER|     NAR|     ZMW|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getRegExp(), "[ 	]*[\\p{IsAlphabetic}\\d]{3,8}[ 	]*");
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getCardinality(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getMinLength(), 8);
		assertEquals(result.getMaxLength(), 8);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	private static final String alpha3 = "aaa|bbb|ccc|ddd|eee|fff|ggg|hhh|iii|jjj|" +
			"aaa|iii|sss|sss|sss|vvv|jjj|jjj|jjj|bbb|iii|uuu|bbb|bbb|vvv|mmm|uuu|fff|vvv|fff|" +
			"iii|ééé|iii|ggg|bbb|sss|mmm|uuu|sss|uuu|aaa|iii|sss|sss|sss|vvv|jjj|jjj|jjj|bbb|" +
			"iii|uuu|bbb|bbb|vvv|mmm|uuu|fff|vvv|fff|iii|bbb|iii|ggg|bbb|sss|mmm|uuu|sss|uuu|" +
			"aaa|iii|sss|sss|sss|vvv|jjj|jjj|jjj|ççç|iii|uuu|bbb|bbb|vvv|mmm|uuu|fff|vvv|fff|" +
			"iii|bbb|iii|ggg|bbb|sss|mmm|uuu|sss|uuu|aaa|iii|sss|sss|sss|vvv|jjj|jjj|jjj|bbb|" +
			"iii|uuu|bbb|bbb|vvv|mmm|uuu|fff|vvv|fff|iii|bbb|iii|ggg|bbb|sss|mmm|uuu|sss|uuu|" +
			"kkk|lll|nnn|ooo|qqq|ppp|rrr|ttt|www|zzz|mmm|iii|uuu|fff|ggg|ggg|uuu|uuu|uuu|uuu|";
	private static final String number3 = "111|123|707|902|104|223|537|902|111|443|" +
			"121|234|738|902|002|431|679|093|124|557|886|631|235|569|002|149|963|271|905|501|" +
			"171|734|038|002|882|215|875|193|214|997|126|361|098|888|314|111|222|341|458|082|" +
			"371|334|438|442|782|715|775|893|314|337|326|781|984|349|534|888|654|841|158|182|" +
			"098|123|435|000|312|223|343|563|123|";

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void constantLength3_alpha() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("constantLength3_alpha");
		final String inputs[] = alpha3.split("\\|");

		for (final String input : inputs) {
			analysis.train("a" + input);
			analysis.train("b" + input);
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getRegExp(), KnownTypes.PATTERN_ALPHA + "{4}");
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertEquals(result.getSampleCount(), inputs.length * 2);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length * 2);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(("a" + input).matches(result.getRegExp()));
			assertTrue(("b" + input).matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void constantLength3_alnum() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("constantLength3_alnum");
		final String inputs[] = (alpha3 + number3).split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getRegExp(), KnownTypes.PATTERN_NUMERIC + "{3}" + '|' + KnownTypes.PATTERN_ALPHA + "{3}");
		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void constantLength3_numal() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("constantLength3_numal");
		final String inputs[] = (number3 + alpha3).split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getRegExp(), KnownTypes.PATTERN_NUMERIC + "{3}" + '|' + KnownTypes.PATTERN_ALPHA + "{3}");
		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}


	private void simpleStringTest(final String name, final String input) throws FTAException {
		simpleArrayTest(name, input.split("\\|"));
	}

	private void simpleArrayTest(final String name, final String[] inputs) throws FTAException {
		final TextAnalyzer analysis = new TextAnalyzer(name);
		int locked = -1;
		int realSamples = 0;
		int empty = 0;
		int minTrimmedLength = Integer.MAX_VALUE;
		int maxTrimmedLength = Integer.MIN_VALUE;
		int minLength = Integer.MAX_VALUE;
		int maxLength = Integer.MIN_VALUE;

		for (final String input : inputs) {
			if (analysis.train(input) && locked == -1)
				locked = realSamples;
			if (input.trim().isEmpty())
				empty++;
			else
				realSamples++;

			int len = input.trim().length();
			if (len != 0) {
				if (len > maxTrimmedLength)
					maxTrimmedLength = len;
				if (len < minTrimmedLength)
					minTrimmedLength = len;
				len = input.length();
				if (len > maxLength)
					maxLength = len;
				if (len != 0 && len < minLength)
					minLength = len;
			}
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, realSamples >= AnalysisConfig.DETECT_WINDOW_DEFAULT ? AnalysisConfig.DETECT_WINDOW_DEFAULT : -1);
		assertEquals(result.getType(), FTAType.STRING);
		if (inputs.length == empty)
			assertEquals(result.getTypeModifier(), "BLANK");
		else
			assertNull(result.getTypeModifier());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getBlankCount(), empty);
		assertEquals(result.getMatchCount(), inputs.length - result.getBlankCount());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1.0);

		int matches = 0;
		for (final String input : inputs) {
			if (!input.trim().isEmpty() && input.matches(result.getRegExp()))
				matches++;
		}
		assertEquals(matches, result.getMatchCount());

		if (result.getMatchCount() != 0 && !result.getRegExp().startsWith("(?i)")) {
			String re = "";
			if (result.getLeadingWhiteSpace())
				re += KnownTypes.PATTERN_WHITESPACE;
			re += KnownTypes.freezeANY(minTrimmedLength, maxTrimmedLength, minLength, maxLength, result.getLeadingWhiteSpace(), result.getTrailingWhiteSpace(), result.getMultiline());
			if (result.getTrailingWhiteSpace())
				re += KnownTypes.PATTERN_WHITESPACE;
			assertEquals(result.getRegExp(), re);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void basicEnum() throws IOException, FTAException {
		final String input = "APARTMENT|APARTMENT|DUPLEX|APARTMENT|DUPLEX|CONDO|DUPLEX|CONDO|" +
				"DUPLEX|DUPLEX|CONDO|CONDO|DUPLEX|DUPLEX|CONDO|APARTMENT|" +
				"DUPLEX|CONDO|CONDO|CONDO|DUPLEX|DUPLEX|DUPLEX|DUPLEX|CONDO|" +
				"DUPLEX|DUPLEX|APARTMENT|CONDO|DUPLEX|CONDO|APARTMENT|APARTMENT|DUPLEX|" +
				"DUPLEX|APARTMENT|APARTMENT|APARTMENT|CONDO|CONDO|APARTMENT|CONDO|DUPLEX|" +
				"DUPLEX|CONDO|APARTMENT|DUPLEX|CONDO|DUPLEX|APARTMENT|CONDO|APARTMENT|" +
				"CONDO|CONDO|CONDO|CONDO|MULTI-FAMILY|DUPLEX|APARTMENT|MULTI-FAMILY|DUPLEX|" +
				"CONDO|APARTMENT|APARTMENT|CONDO|CONDO|MULTI-FAMILY|DUPLEX|CONDO|APARTMENT|" +
				"CONDO|DUPLEX|APARTMENT|CONDO|DUPLEX|DUPLEX|APARTMENT|APARTMENT|APARTMENT|" +
				"APARTMENT|APARTMENT|APARTMENT|CONDO|CONDO|APARTMENT|APARTMENT|CONDO|APARTMENT|" +
				"CONDO|APARTMENT|CONDO|APARTMENT|DUPLEX|CONDO|APARTMENT|APARTMENT|DUPLEX|" +
				"CONDO|APARTMENT|APARTMENT|DUPLEX|DUPLEX|CONDO|APARTMENT|CONDO|APARTMENT|" +
				"APARTMENT|CONDO|APARTMENT|CONDO|DUPLEX|MULTI-FAMILY|DUPLEX|CONDO|DUPLEX|" +
				"CONDO|APARTMENT|CONDO|DUPLEX|MULTI-FAMILY|APARTMENT|CONDO|DUPLEX|DUPLEX|" +
				"MULTI-FAMILY|APARTMENT|APARTMENT|APARTMENT|DUPLEX|APARTMENT|CONDO|CONDO|DUPLEX|" +
				"DUPLEX|DUPLEX|APARTMENT|DUPLEX|APARTMENT|DUPLEX|DUPLEX|DUPLEX|CONDO|" +
				"CONDO|APARTMENT|APARTMENT|APARTMENT|DUPLEX|APARTMENT|CONDO|MULTI-FAMILY|CONDO|" +
				"APARTMENT|DUPLEX|DUPLEX|MULTI-FAMILY|MULTI-FAMILY|DUPLEX|DUPLEX|DUPLEX|APARTMENT|" +
				"APARTMENT|DUPLEX|APARTMENT|DUPLEX|APARTMENT|APARTMENT|CONDO|CONDO|CONDO|" +
				"CONDO|DUPLEX|CONDO|MULTI-FAMILY|CONDO|CONDO|APARTMENT|CONDO|APARTMENT|" +
				"CONDO|APARTMENT|APARTMENT|CONDO|DUPLEX|APARTMENT|APARTMENT|APARTMENT|CONDO|" +
				"CONDO|CONDO|DUPLEX|DUPLEX|APARTMENT|CONDO|DUPLEX|DUPLEX|APARTMENT|" +
				"APARTMENT|CONDO|DUPLEX|APARTMENT|CONDO|CONDO|DUPLEX|CONDO|CONDO|" +
				"DUPLEX|CONDO|APARTMENT|DUPLEX|CONDO|CONDO|APARTMENT|DUPLEX|DUPLEX|" +
				"CONDO|APARTMENT|APARTMENT|CONDO|APARTMENT|DUPLEX|CONDO|APARTMENT|MULTI-FAMILY|" +
				"DUPLEX|CONDO|APARTMENT|APARTMENT|CONDO|APARTMENT|MULTI-FAMILY|CONDO|DUPLEX|" +
				"DUPLEX|CONDO|DUPLEX|DUPLEX|DUPLEX|DUPLEX|CONDO|CONDO|CONDO|" +
				"APARTMENT|CONDO|APARTMENT|DUPLEX|APARTMENT|APARTMENT|APARTMENT|DUPLEX|APARTMENT|" +
				"DUPLEX|APARTMENT|APARTMENT|APARTMENT|DUPLEX|DUPLEX|DUPLEX|CONDO|CONDO|" +
				"DUPLEX|CONDO|CONDO|APARTMENT|CONDO|APARTMENT|APARTMENT|APARTMENT|CONDO|" +
				"CONDO|CONDO|DUPLEX|CONDO|APARTMENT|CONDO|DUPLEX|DUPLEX|APARTMENT|" +
				"CONDO|APARTMENT|DUPLEX|DUPLEX|MULTI-FAMILY|DUPLEX|DUPLEX|DUPLEX|DUPLEX|" +
				"DUPLEX|DUPLEX|APARTMENT|DUPLEX|CONDO|APARTMENT|APARTMENT|MULTI-FAMILY|DUPLEX|" +
				"APARTMENT|APARTMENT|CONDO|CONDO|DUPLEX|CONDO|DUPLEX|DUPLEX|DUPLEX|" +
				"APARTMENT|CONDO|CONDO|CONDO|APARTMENT|";
		simpleStringTest("basicEnum", input);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void blanksLeft() throws IOException, FTAException {
		final String[] inputs = {
				" D12345",
				"A123 56", "A1234567", "A12345678", "A123456789", "A123456", "A1234567", "A12345678", "A123456789",
				"B123 56", "B1234567", "B12345678", "B123456789", "B123456", "B1234567", "B12345678", "B123456789",
				"C123 56", "C1234567", "C12345678", "C123456789", "C123456", "C1234567", "C12345678", "C123456789"
		};

		simpleArrayTest("blanksLeft", inputs);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void blanksInInput() throws IOException, FTAException {
		final String[] inputs = {
				" D12345", "  C123456789",
				"A123 56", "A1234567", "A12345678", "        ", "A123456", "A1234567", "A12345678", "A123456789",
				"B123 56", "B1234567", "B12345678", "B123456789", "B123456", "B1234567", "B12345678", "B123456789",
				"C123 56", "C1234567", "C12345678", "C123456789", "    ", "C1234567", "C12345678", "C123456789"
		};

		simpleArrayTest("blanksLeft", inputs);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void allEmpty() throws IOException, FTAException {
		final String[] inputs = {
				"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
				"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
		};

		simpleArrayTest("allEmpty", inputs);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void blanksInField() throws IOException, FTAException {
		final String[] inputs = {
				"-", "-", "", "", "", "", "^^^", "", "", "", "-", "", "", "", "", "", "", "", "",
				"", "-", "", "", "", "", "", "-", "", "", "-", "", "-", "-", "", "", "", "-", "", "",
				"-", "", "", "", "-", "", "-", "-", "", "", "", "-", "", "-", "", "", "", "****", "****", "",
				"", "", "", "", "", "****", "****", "", "****", "****", "****", "****", "", "", "", "", "", "", "****", "****",
				"", "", "", "****", "****", "****", "****", "", "", "", "", "****", "****", "", "", "****", "", "", "", "", " "
		};

		simpleArrayTest("DataValueFootnoteSymbol", inputs);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void blanksAtEndOfField() throws IOException, FTAException {
		final String[] inputs = {
				"", "Foster Road", "", "Grove Road", "", "Library", "", "Bradgers Hill Road", "", "Tomlinson Avenue", "Wheatfield Road", "Tomlinson Avenue", "",
				"Bradgers Hill Road", "", "Nixon Street", "", "Moor Lane", "", "West Hanningfield Road", "Fambridge Road", "Victoria Drive", "Maypole Road",
				"Station Road", "Roundbush Road", "Harborough Hall Lane", "Colchester Road", "Church Road", "Roundbush Road", "Harborough Hall Lane", "Colchester Road", "The Folly",
				"Little Horkesley Road", "London Road", "Home Farm Lane", "Damants Farm Lane", "Hospital Lane", "Clarendon Way", "North Station Rbt", "New Writtle Street", "Oxford Road", "School Lane",
				"Tog Lane", "Station Road", "Colchester Road", "Cooks Hill", "Clarendon Way", "North Station Rbt", "Kelvedon Road", "Latchingdon Road", "Barnhall Road", "Trusses Road",
				"", "School Lane", "Castle Drive", "The Street", "Fairstead Hall Road", "Pepples Lane", "", "Eastern Avenue", "", "Red Lane",
				"", "Granville Street", "", "yes ", "Yes Tactile", "", "Wilmot Road", "Wilmot Lane", "", "Victoria Street",
				"", "Kirk Gate", "", "Gables Lea", "", "Village Hall", "", "Morley Road", "", "Beach Road",
				"Marine Parade", "", "Mount Pleasant", "", "Heol Camlan", "", "Golwg y Mynydd", "", "Sraid na h-Eaglaise", "",
				"Geilear", "", "Tom na Ba", "", "Rathad Ur", "", "Geilear", "Rathad Ur", "", "Struan Ruadh",
				"", "Struan Ruadh", "", "Rubhachlachainn", "", "Sraid a' Chaisteil", "Sraid a' Bhanca", "Ionad Casimir", "Ionad Mhicceallaig", "", "Slighe Ruairidh", ""
		};

		simpleArrayTest("blanksAtEndOfField", inputs);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void basicPromote() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicPromote");
		final String pipedInput =
				"01000053218|0100BRP90233|0100BRP90237|0180BAA01319|0180BAC30834|0190NSC30194|0190NSC30195|0190NSC30652|0190NSC30653|0190NSC30784|" +
						"0190NSC30785|0190NSY28569|0190NSZ01245|020035037|02900033|02900033|02900039|02901210|02903036|02903037|" +
						"030051210001|030051210002|030054160002|030055200003|03700325|03700325|0380F968G059|040000002968|049000000804|049002399361|" +
						"049002399861|0500CCITY084|0500CCITY248|0500CCITY476|0500FWISH002|0500HHUNT027|0500HSTNS060|0500HSTNS062|0500SHARS006|0500SHARS016|" +
						"0590PET621|0590PET622|0590PQG571|0600CR087|0600CR290|0610CH19130|0610CH548|0610EP19031|068000000461|068000000462|" +
						"068000000502|069000024300|0690WNA02867|0690WNA02867|075071047A|075071047B|07605752|077072401A|077072401A|077072572A|" +
						"077072583A|079073001K|0800COA10071|0800COA10194|0800COA10196|0800COA10196|0800COA10204|0800COA10207|0800COA10267|0800COA10268|" +
						"0800COA10268|0800COA10268|0800COA10386|0800COA10469|0800COA10470|0800COA10490|0800COB20133|0800COB20134|0800COB20138|0800COB20139|" +
						"0800COC30257|0800COC30258|0800COC30488|0800COC30504|0800COC30505|0800COC30649|0800COC30815|0800COC30873|0800COC31003|0800COC31004|" +
						 "0800COC31093|0800COC31215|0800COC31216|0800COC31221|0800COC31222|0800COC31229|0800COC31231|0800COC31306|0800COC31307|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), KnownTypes.PATTERN_ALPHANUMERIC + "{8,12}");
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void belowDetectWindow() throws IOException, FTAException {
		final String BAD = "hello";
		final String[] samples = {
				"1234567", "403901",  "6200243690", "6200243691", "6200243692", "6200243693", "6200243694", "5", "8", "9",
				BAD, "020035031", "6200243635", "6200243635", "6200206290", "6200206290",
		};

		final TextAnalyzer analysis = new TextAnalyzer("belowDetectWindow");
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getRegExp(), "\\d{1,10}");
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
		assertEquals(result.getShapeCount(), 6);

		for (final String sample : samples) {
			if (!BAD.contentEquals(sample))
				assertTrue(sample.matches(result.getRegExp()), sample);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void regExpIssue() throws IOException, FTAException {
		final String[] samples = {
				"51100", "44087", "05454-876", "69004", "B-6000", "05454-876", "3012", "1204", "08737-363", "5022", "8010",
				"05022", "50739", "02389-673", "87110", "8010", "S-844 67", "67000", "90110", "80805", "1081", "98124",
				"90110", "82520", "87110", "01307", "51100", "24100", "05033", "04179", "S-958 22", "60528", "S-958 22",
				"28001", "28001", "3508", "60528", "01307", "01307", "02389-890", "42100", "EC2 5NT", "05432-043",
				 "02389-673", "05634-030", "05033", "87110", "51100", "3508"
		};
		final TextAnalyzer analysis = new TextAnalyzer("ShipPostalCode");
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getShapeCount(), 6);

		for (final String sample : samples) {
			assertTrue(sample.matches(result.getRegExp()), sample);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void basicText() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicText");
		final int iterations = 10000;
		int locked = -1;

		for (int i = 0; i < iterations; i++) {
			if (analysis.train("primary") && locked == -1)
				locked = i;
			if (analysis.train("secondary") && locked == -1)
				locked = i;
			if (analysis.train("tertiary") && locked == -1)
				locked = i;
			if (analysis.train("fictional") && locked == -1)
				locked = i;
			if (analysis.train(null) && locked == -1)
				locked = i;
		}
		analysis.train("secondory");

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT/4);
		assertEquals(result.getSampleCount(), 5 * iterations + 1);
		assertEquals(result.getMatchCount(), 4 * iterations);
		assertEquals(result.getNullCount(), iterations);
		assertEquals(result.getCardinality(), 4);
		assertEquals(result.getRegExp(), "(?i)(FICTIONAL|PRIMARY|SECONDARY|TERTIARY)");
		assertEquals(result.getConfidence(), 1 - (double)1/(result.getMatchCount() + 1));
		assertEquals(result.getOutlierCount(), 1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void textBlocks() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("textBlocks");
		final int iterations = 100000;
		int locked = -1;
		final StringBuilder line = new StringBuilder();
		int minTrimmedLength = Integer.MAX_VALUE;
		int maxTrimmedLength = Integer.MIN_VALUE;
		int minLength = Integer.MAX_VALUE;
		int maxLength = Integer.MIN_VALUE;
		final String alphabet = "abcdefhijklmnopqrstuvwxyz";

		for (int i = 0; i < iterations; i++) {
			line.setLength(0);
			final int wordCount = random.nextInt(20);
			for (int words = 0; words < wordCount; words++) {
				final int charCount = random.nextInt(10);
				for (int chars = 0; chars < charCount; chars++) {
					line.append(alphabet.charAt(random.nextInt(25)));
				}
				line.append(' ');
			}
			final String sample = line.toString().trim();
			int len = sample.length();
			if (len > maxLength)
				maxLength = len;
			if (len < minLength && len != 0)
				minLength = len;
			len = sample.trim().length();
			if (len > maxTrimmedLength)
				maxTrimmedLength = len;
			if (len < minTrimmedLength && len != 0)
				minTrimmedLength = len;
			if (analysis.train(sample) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getSampleCount(), iterations);
		assertEquals(result.getCardinality(), AnalysisConfig.MAX_CARDINALITY_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), KnownTypes.freezeANY(minTrimmedLength, maxTrimmedLength, minLength, maxLength, result.getLeadingWhiteSpace(), result.getTrailingWhiteSpace(), result.getMultiline()));
		assertEquals(result.getConfidence(), 1.0);
	}

//	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void leakInt() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("leakInt");
		analysis.setDebug(3);
		final int iterations = 1_000_000;

		for (int i = 0; i < iterations; i++)
			analysis.train(String.valueOf(i));

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getSampleCount(), iterations);
		assertEquals(result.getType(), FTAType.LONG);
	}

//	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void leakString() throws IOException, FTAException {
		final StringBuilder line = new StringBuilder();
		final String alphabet = "abcdefhijklmnopqrstuvwxyz";
		final TextAnalyzer analysis = new TextAnalyzer("leakString");
		analysis.setDebug(3);
		final int iterations = 1_000_000;

		for (int i = 0; i < iterations; i++) {
			line.setLength(0);
			final int wordCount = random.nextInt(20);
			for (int words = 0; words < wordCount; words++) {
				final int charCount = random.nextInt(10);
				for (int chars = 0; chars < charCount; chars++) {
					line.append(alphabet.charAt(random.nextInt(25)));
				}
				line.append(' ');
			}
			final String sample = line.toString().trim();
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getSampleCount(), iterations);
		assertEquals(result.getType(), FTAType.STRING);
	}

//	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void leakLocalDateTime() throws IOException, FTAException {
		LocalDateTime start = LocalDateTime.of(1960, 1, 1, 1, 0, 0);
		final TextAnalyzer analysis = new TextAnalyzer("leakLocalDateTime");
		analysis.setDebug(3);
		final int iterations = 1_000_000;

		for (int i = 0; i < iterations; i++) {
			start = start.plusSeconds(random.nextInt(100));
			analysis.train(String.valueOf(start));
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getSampleCount(), iterations);
		assertEquals(result.getType(), FTAType.STRING);

	}

//	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void leakAnalyzer() throws IOException, FTAException {
		final StringBuilder line = new StringBuilder();
		final String alphabet = "abcdefhijklmnopqrstuvwxyz";
		final int iterations = 10;
		final int samples = 100_000;
		final TextAnalyzer[] analyzers = new TextAnalyzer[iterations];

		for (int i = 0; i < iterations; i++) {
			analyzers[i] = new TextAnalyzer("leakAnalyzer-" + i);
			analyzers[i].setDebug(3);
			int j = samples;
			while (j-- > 0) {
				line.setLength(0);
				final int wordCount = random.nextInt(20);
				for (int words = 0; words < wordCount; words++) {
					final int charCount = random.nextInt(10);
					for (int chars = 0; chars < charCount; chars++) {
						line.append(alphabet.charAt(random.nextInt(25)));
					}
					line.append(' ');
				}
				analyzers[i].train(line.toString().trim());
			}
		}

		for (int i = 0; i < iterations; i++) {
			final TextAnalysisResult result = analyzers[i].getResult();
			assertEquals(result.getNullCount(), 0);
			assertEquals(result.getSampleCount(), samples);
			assertEquals(result.getType(), FTAType.STRING);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setDetectWindow() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("setDetectWindow");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		int locked = -1;
		int sample = 0;

		analysis.setDetectWindow(2* AnalysisConfig.DETECT_WINDOW_DEFAULT);
		for (int i = 0; i <= AnalysisConfig.DETECT_WINDOW_DEFAULT; i++) {
			sample++;
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = sample;
		}
		for (int i = 0; i <= AnalysisConfig.DETECT_WINDOW_DEFAULT; i++) {
			sample++;
			if (analysis.train(String.valueOf(random.nextDouble())) && locked == -1)
				locked = sample;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, 2 * AnalysisConfig.DETECT_WINDOW_DEFAULT + 1);
		assertEquals(result.getSampleCount(), 2 * (AnalysisConfig.DETECT_WINDOW_DEFAULT + 1));
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertNull(result.getTypeModifier());
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void getDetectWindowSize()  throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("getDetectWindowSize");

		assertEquals(analysis.getDetectWindow(), AnalysisConfig.DETECT_WINDOW_DEFAULT);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setDetectWindowTooSmall() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("setDetectWindowTooSmall");

		try {
			analysis.setDetectWindow(AnalysisConfig.DETECT_WINDOW_DEFAULT - 1);
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Cannot set detect window size below " + AnalysisConfig.DETECT_WINDOW_DEFAULT);
			return;
		}
		fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setDetectWindowTooLate() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("setDetectWindowTooLate");
		int locked = -1;
		int i = 0;

		for (; i <= AnalysisConfig.DETECT_WINDOW_DEFAULT; i++) {
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = i;
		}

		try {
			analysis.setDetectWindow(2* AnalysisConfig.DETECT_WINDOW_DEFAULT);
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Cannot change size of detect window once training has started");
			return;
		}
		fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setLocaleTooLate() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("setLocaleTooLate");
		analysis.setTrace("enabled=true");
		final Locale locale = Locale.forLanguageTag("en-US");
		int locked = -1;
		int i = 0;

		for (; i <= AnalysisConfig.DETECT_WINDOW_DEFAULT; i++) {
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = i;
		}

		try {
			analysis.setLocale(locale);
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Cannot adjust Locale once training has started");
			return;
		}
		fail("Exception should have been thrown");
	}


	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void getMaxCardinality() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("getMaxCardinality");

		assertEquals(analysis.getMaxCardinality(), AnalysisConfig.MAX_CARDINALITY_DEFAULT);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setMaxCardinalityNegative() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("setMaxCardinalityNegative");

		try {
			analysis.setMaxCardinality(-1);
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Invalid value for maxCardinality -1");
			return;
		}
		fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setMaxCardinalityTooSmall() throws IOException, FTAException {
		final String inputs[] = alpha3.split("\\|");
		final TextAnalyzer analysis = new TextAnalyzer("setMaxCardinalityTooSmall");
		analysis.setMaxCardinality(100);

		try {
			for (final String input : inputs)
				analysis.train(input);

			analysis.getResult();
		}
		catch (FTAPluginException e) {
			final String message = e.getMessage();
			assertTrue(message.startsWith("Internal error: Max Cardinality: 100 is insufficient to support plugin:"));
			return;
		}
		fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setMaxCardinalitySmall() throws IOException, FTAException {
		final String inputs[] = alpha3.split("\\|");
		final TextAnalyzer analysis = new TextAnalyzer("setMaxCardinalitySmall");
		analysis.setMaxCardinality(100);
		analysis.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);

		try {
			for (final String input : inputs) {
				analysis.train("A" + input);
				analysis.train("B" + input);
			}

			final TextAnalysisResult result = analysis.getResult();

			assertEquals(result.getRegExp(), KnownTypes.PATTERN_ALPHA + "{4}");
			assertEquals(result.getType(), FTAType.STRING);
			assertNull(result.getTypeModifier());
			assertEquals(result.getSampleCount(), inputs.length * 2);
			assertEquals(result.getOutlierCount(), 0);
			assertEquals(result.getMatchCount(), inputs.length * 2);
			assertEquals(result.getNullCount(), 0);
			assertEquals(result.getConfidence(), 1.0);

		}
		catch (FTAPluginException e) {
			fail("Exception should have been thrown");
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setMaxCardinalityTooLate() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("setMaxCardinalityTooLate");
		int locked = -1;
		int i = 0;

		for (; i <= AnalysisConfig.DETECT_WINDOW_DEFAULT; i++) {
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = i;
		}

		try {
			analysis.setMaxCardinality(2* AnalysisConfig.MAX_CARDINALITY_DEFAULT);
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Cannot change maxCardinality once training has started");
			return;
		}
		fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void getOutlierCount() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("getOutlierCount");

		assertEquals(analysis.getMaxOutliers(), AnalysisConfig.MAX_OUTLIERS_DEFAULT);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setMaxOutliersTooSmall() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("setMaxOutliersTooSmall");

		try {
			analysis.setMaxOutliers(-1);
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Invalid value for outlier count -1");
			return;
		}
		fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setMaxOutliersTooLate() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("setMaxOutliersTooLate");
		int locked = -1;
		int i = 0;

		for (; i <= AnalysisConfig.DETECT_WINDOW_DEFAULT; i++) {
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = i;
		}

		try {
			analysis.setMaxOutliers(2* AnalysisConfig.MAX_OUTLIERS_DEFAULT);
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Cannot change outlier count once training has started");
			return;
		}
		fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setMaxInputLengthTooLate() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("setMaxInputLengthTooLate");
		int locked = -1;
		int i = 0;

		for (; i <= AnalysisConfig.DETECT_WINDOW_DEFAULT; i++) {
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = i;
		}

		try {
			analysis.setMaxInputLength(8000);
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Cannot change maxInputLength once training has started");
			return;
		}
		fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void USPhone() throws IOException, FTAException {
		final String[] samples = new String[1000];

		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < samples.length; i++) {
			b.setLength(0);
			b.append("+1 ")
				.append(String.format("%03d", random.nextInt(1000)))
				.append(' ')
				.append(String.format("%03d", random.nextInt(1000)))
				.append(' ')
				.append(String.format("%04d", random.nextInt(10000)));
			samples[i] = b.toString();
		}


		final TextAnalyzer analysis = new TextAnalyzer("USPhone");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertEquals(result.getRegExp(), "\\+\\d \\d{3} \\d{3} \\d{4}");
		assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples) {
			assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void USPhone2() throws IOException, FTAException {
		final String[] samples = new String[1000];

		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < samples.length; i++) {
			b.setLength(0);
			b.append("1.")
				.append(String.format("%03d", random.nextInt(1000)))
				.append('.')
				.append(String.format("%03d", random.nextInt(1000)))
				.append('.')
				.append(String.format("%04d", random.nextInt(10000)));
			samples[i] = b.toString();
		}


		final TextAnalyzer analysis = new TextAnalyzer("USPhone2");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertEquals(result.getRegExp(), "\\d\\.\\d{3}\\.\\d{3}\\.\\d{4}");
		assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void USPhone3() throws IOException, FTAException {
		final String[] samples = new String[1000];

		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < samples.length; i++) {
			b.setLength(0);
			b.append('(')
				.append(String.format("%03d", random.nextInt(1000)))
				.append(") ")
				.append(String.format("%03d", random.nextInt(1000)))
				.append(' ')
				.append(String.format("%04d", random.nextInt(10000)));
			samples[i] = b.toString();
		}

		final TextAnalyzer analysis = new TextAnalyzer("USPhone3");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertEquals(result.getRegExp(), "\\(\\d{3}\\) \\d{3} \\d{4}");
		assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples) {
			assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void bigExponents() throws IOException, FTAException {
		final String[] samples = {
				"5230CGX16431", "3590E094000", "3590E092401", "3590E012300", "66004890064", "020035020", "270000009882", "020035256", "5520WDB48305", "6200600740",
				"6200243690", "6200243690", "6200243690", "6200243690", "6200243690", "6200243690", "6200243690", "6200243690", "66004589900", "66004589900",
				"020035300", "020035300", "020035347", "6020710337", "6020710337", "020035257", "020035053", "020035030", "020035030", "020035031",
				"020035031", "6200243635", "6200243635", "6200206290", "6200206290", "3590E049400", "3590E094300", "3590E094300", "3590E094300"
		};

		final TextAnalyzer analysis = new TextAnalyzer("bigExponents");
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), "[\\p{IsAlphabetic}\\d]{9,12}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getShapeCount(), 6);
		final Map<String, Long> shapes = result.getShapeDetails();
		assertEquals(shapes.size(), result.getShapeCount());
		assertEquals(shapes.get("999999999999"), 1L);
		assertEquals(shapes.get("9999XXX99999"), 2L);

		for (final String sample : samples) {
			assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void difficultRegExp() throws IOException, FTAException {
		final String[] samples = new String[1000];

		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < samples.length; i++) {
			b.setLength(0);
			b.append('[')
				.append(String.format("%03d", random.nextInt(1000)))
				.append("){[0-9] ^")
				.append(String.format("%03d", random.nextInt(1000)))
				.append('$')
				.append(String.format("%04d", random.nextInt(10000)));
			samples[i] = b.toString();
		}


		final TextAnalyzer analysis = new TextAnalyzer("difficultRegExp");
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), "\\[\\d{3}\\)\\{\\[\\d-\\d\\] \\^\\d{3}\\$\\d{4}");
		assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples) {
			assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void bumpMaxCardinality() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("bumpMaxCardinality");

		analysis.setMaxCardinality(2 * AnalysisConfig.MAX_CARDINALITY_DEFAULT);

		final int nullIterations = 50;
		final int iterations = 2 * AnalysisConfig.MAX_CARDINALITY_DEFAULT + 100;
		int locked = -1;

		for (int i = 0; i < nullIterations; i++) {
			analysis.train(null);
		}
		int cnt = 0;
		while (cnt < iterations) {
			final long randomLong = random.nextInt(Integer.MAX_VALUE) + 1000000000L;
			if (randomLong >  9999999999L)
				continue;
			if (analysis.train(String.valueOf(randomLong)) && locked == -1)
				locked = cnt;
			cnt++;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), iterations + nullIterations);
		assertEquals(result.getCardinality(), 2 * AnalysisConfig.MAX_CARDINALITY_DEFAULT);
		assertEquals(result.getNullCount(), nullIterations);
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getRegExp(), "\\d{10}");
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void keyFieldLong() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("keyFieldLong");
		final int start = 10000;
		final int end = start + AnalysisConfig.MAX_CARDINALITY_DEFAULT + 100;
		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i - start;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), end - start);
		assertEquals(result.getCardinality(), AnalysisConfig.MAX_CARDINALITY_DEFAULT);
		assertEquals(result.getRegExp(), "\\d{5}");
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getSemanticType(), "IDENTIFIER");
		assertEquals(result.getKeyConfidence(), 0.99);
		assertEquals(result.getConfidence(), 0.99);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void keyFieldLongNoDefaultTypes() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("keyFieldLong");
		analysis.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);
		final int start = 10000;
		final int end = start + AnalysisConfig.MAX_CARDINALITY_DEFAULT + 100;
		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i - start;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), end - start);
		assertEquals(result.getCardinality(), AnalysisConfig.MAX_CARDINALITY_DEFAULT);
		assertEquals(result.getRegExp(), "\\d{5}");
		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getSemanticType());
		assertEquals(result.getKeyConfidence(), 0.90);
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void defaultMaxOutliers() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Alphabet");
		final int start = 10000;
		final int end = start + AnalysisConfig.MAX_CARDINALITY_DEFAULT + 100;
		final int invalids = 15;
		int locked = -1;

		analysis.train("A");
		for (int i = start; i < end - 1; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i - start;
		}
		// Two copies of end - 2 so the field does not look like an IDENTIFIER
		analysis.train(String.valueOf(end - 2));
		analysis.train("B");
		analysis.train("C");
		analysis.train("D");
		analysis.train("E");
		analysis.train("F");
		analysis.train("G");
		analysis.train("H");
		analysis.train("I");
		analysis.train("J");
		analysis.train("K");
		analysis.train("L");
		analysis.train("M");
		analysis.train("N");
		analysis.train("O");

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getRegExp(), "\\d{5}");
		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getSemanticType());
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getInvalidCount(), invalids);
		assertEquals(result.getSampleCount(), invalids + end - start);
		assertEquals(result.getCardinality(), AnalysisConfig.MAX_CARDINALITY_DEFAULT);
		assertEquals(result.getKeyConfidence(), 0.9);
		assertEquals(result.getConfidence(), 1 - (double)15/result.getSampleCount());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testDataSignature() throws IOException, FTAException {
		final TextAnalyzer analysis1 = new TextAnalyzer("Analysis1");
		final TextAnalyzer analysis2 = new TextAnalyzer("Analysis2");
		analysis2.setTotalCount(1000000);

		final int start = 10000;

		for (int i = start; i < start + 1000; i++) {
			analysis1.train(String.valueOf(i));
		}
		final TextAnalysisResult result1 = analysis1.getResult();
		assertEquals(result1.getTotalCount(), -1);

		for (int i = start; i < start + 1000; i++) {
			analysis2.train(String.valueOf(i));
		}
		final TextAnalysisResult result2 = analysis2.getResult();
		assertEquals(result2.getTotalCount(), 1000000);

		assertEquals(result1.getStructureSignature(), result2.getStructureSignature());
		assertNotEquals(result1.getDataSignature(), result2.getDataSignature());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testMaxInputLengthNoOverflow() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("maxLength");
		final int LONG_TEST = 4100;
		assertTrue(LONG_TEST > analysis.getMaxInputLength());

		analysis.setMaxInputLength(5000);
		assertTrue(LONG_TEST < analysis.getMaxInputLength());

		final String hello = "Hello";
		analysis.train(hello);
		for (int i = 'a'; i < 'a' + 26; i++) {
			final String longString = Utils.repeat((char)i, LONG_TEST);
			analysis.train(longString);
		}

		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{" + hello.length() + "," + LONG_TEST + "}");
		assertEquals(result.getMinLength(), hello.length());
		assertEquals(result.getMaxLength(), LONG_TEST);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testMaxInputLengthOverflow() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("maxLength");
		final int LONG_TEST = 5000;
		assertTrue(LONG_TEST > analysis.getMaxInputLength());
		final String hello = "Hello";

		analysis.train(hello);
		for (int i = 'a'; i < 'a' + 26; i++) {
			final String longString = Utils.repeat((char)i, LONG_TEST);
			analysis.train(longString);
		}

		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{" + hello.length() + "," + LONG_TEST + "}");
		assertEquals(result.getMinLength(), hello.length());
		assertEquals(result.getMaxLength(), LONG_TEST);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setMaxOutliers() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Alphabet");
		final int start = 10000;
		final int end = 12000;
		final int outliers = 15;
		final int newMaxOutliers = 12;

		int locked = -1;

		analysis.setMaxOutliers(newMaxOutliers);

		analysis.train("A");
		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i - start;
		}
		analysis.train("B");
		analysis.train("C");
		analysis.train("D");
		analysis.train("E");
		analysis.train("F");
		analysis.train("G");
		analysis.train("H");
		analysis.train("I");
		analysis.train("J");
		analysis.train("K");
		analysis.train("L");
		analysis.train("M");
		analysis.train("N");
		analysis.train("O");

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getRegExp(), "[\\p{IsAlphabetic}\\d]{1,5}");
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(analysis.getMaxOutliers(), newMaxOutliers);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getSampleCount(), outliers + end - start);
		//BUG		assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void setMaxInvalids() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Alphabet");
		final int start = 10000;
		final int end = 12000;
		final int outliers = 15;
		final int newMaxInvalids = 12;

		int locked = -1;

		analysis.setMaxInvalids(newMaxInvalids);

		analysis.train("A");
		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i - start;
		}
		analysis.train("B");
		analysis.train("C");
		analysis.train("D");
		analysis.train("E");
		analysis.train("F");
		analysis.train("G");
		analysis.train("H");
		analysis.train("I");
		analysis.train("J");
		analysis.train("K");
		analysis.train("L");
		analysis.train("M");
		analysis.train("N");
		analysis.train("O");

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getRegExp(), "\\d{5}");
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(analysis.getMaxInvalids(), newMaxInvalids);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getSampleCount(), outliers + end - start);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void defaultMaxShapes() throws IOException, FTAException {
		TextAnalyzer analysis = new TextAnalyzer("setMaxShapes");

		// We should be able to track shapes up to the MAX_SHAPES_DEFAULT
		for (int i = 0; i < analysis.getMaxShapes(); i++)
			analysis.train(Integer.toBinaryString(i).replace('1', 'A'));

		TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getShapeCount(), AnalysisConfig.MAX_SHAPES_DEFAULT);

		analysis = new TextAnalyzer("setMaxShapes");

		// We supply one more than MAX_SHAPES_DEFAULT - so now we will fail to track (and return 0)
		for (int i = 0; i <= analysis.getMaxShapes(); i++)
			analysis.train(Integer.toBinaryString(i).replace('1', 'A'));

		result = analysis.getResult();

		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getShapeCount(), 0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void adjustMaxShapes() throws IOException, FTAException {
		TextAnalyzer analysis = new TextAnalyzer("setMaxShapes");
		final int MAX_SHAPES = 200;
		analysis.setMaxShapes(MAX_SHAPES);

		// We should be able to track shpaes up to the MAX_SHAPES_DEFAULT
		for (int i = 0; i < MAX_SHAPES; i++)
			analysis.train(Integer.toBinaryString(i).replace('1', 'A'));

		TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getShapeCount(), MAX_SHAPES);

		analysis = new TextAnalyzer("setMaxShapes");
		analysis.setMaxShapes(MAX_SHAPES);

		// We supply one more than MAX_SHAPES_DEFAULT - so now we will fail to track (and return 0)
		for (int i = 0; i <= MAX_SHAPES; i++)
			analysis.train(Integer.toBinaryString(i).replace('1', 'A'));

		result = analysis.getResult();

		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getShapeCount(), 0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void adjustMaxShapesTooLate() throws IOException, FTAException {
		TextAnalyzer analysis = new TextAnalyzer("setMaxShapes");
		analysis.train("Hello, World");

		try {
			analysis.setMaxShapes(200);
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Cannot change maximum shapes once training has started");
			return;
		}
		fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void defaultTopBottomK() throws IOException, FTAException {
		TextAnalyzer analysis = new TextAnalyzer("TopBottomK");

		for (int i = 0; i < 100; i++)
			analysis.train(String.valueOf(i));

		TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTopK().size(), AnalysisConfig.TOP_BOTTOM_K);
		assertEquals(analysis.getTopBottomK(), AnalysisConfig.TOP_BOTTOM_K);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void adjustTopBottomK() throws IOException, FTAException {
		TextAnalyzer analysis = new TextAnalyzer("TopBottomK");
		final int TRACKING = 20;
		analysis.setTopBottomK(TRACKING);

		for (int i = 0; i < 100; i++)
			analysis.train(String.valueOf(i));

		TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTopK().size(), TRACKING);
		assertEquals(analysis.getTopBottomK(), TRACKING);


		analysis = new TextAnalyzer("TopBottomK");
		analysis.setTopBottomK(1);

		for (int i = 0; i < 100; i++)
			analysis.train(String.valueOf(i));

		result = analysis.getResult();

		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTopK().size(), 1);
		assertEquals(analysis.getTopBottomK(), 1);


	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void adjustTopBottomKTooLate() throws IOException, FTAException {
		TextAnalyzer analysis = new TextAnalyzer("setTopBottomK");
		analysis.train("Hello, World");

		try {
			analysis.setTopBottomK(20);
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Cannot change the number of top/bottom values tracked once training has started");
			return;
		}
		fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void totalTestSet() throws IOException, FTAException {
		final int ENTIRE_SET = 1000;
		final int SAMPLES = 100;

		final TextAnalyzer analysis = new TextAnalyzer("totalTest");
		long sum = 0;
		for (int i = 0; i < ENTIRE_SET; i++) {
			sum += i;
			if (i < SAMPLES)
				analysis.train(String.valueOf(i));
		}
		analysis.train(" ");
		analysis.train(null);
		analysis.train(null);

		analysis.setTotalCount(ENTIRE_SET + 3);
		analysis.setTotalNullCount(2);
		analysis.setTotalBlankCount(1);
		analysis.setTotalMinValue("0");
		analysis.setTotalMaxValue("999");

		analysis.setTotalMinLength(1);
		analysis.setTotalMaxLength(3);

		analysis.setTotalMean((double)sum/ENTIRE_SET);
		analysis.setTotalStandardDeviation(288.8194361);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), SAMPLES + 3);
		assertEquals(result.getTotalCount(), ENTIRE_SET + 3);

		assertEquals(result.getNullCount(), 2);
		assertEquals(result.getTotalNullCount(), 2);

		assertEquals(result.getBlankCount(), 1);
		assertEquals(result.getTotalBlankCount(), 1);

		assertEquals(result.getMinValue(), "0");
		assertEquals(result.getTotalMinValue(), "0");
		assertEquals(result.getMaxValue(), "99");
		assertEquals(result.getTotalMaxValue(), "999");

		assertEquals(result.getMinLength(), 1);
		assertEquals(result.getTotalMinLength(), 1);
		assertEquals(result.getMaxLength(), 2);
		assertEquals(result.getTotalMaxLength(), 3);

		assertEquals(result.getMean(), 49.5);
		assertEquals(result.getTotalMean(), 499.5);

		assertEquals(result.getStandardDeviation(), 28.86607004772212);
		assertEquals(result.getTotalStandardDeviation(), 288.8194361);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void totalTestUnset() throws IOException, FTAException {
		final int ENTIRE_SET = 1000;
		final int SAMPLES = 100;

		final TextAnalyzer analysis = new TextAnalyzer("totalTest");
		for (int i = 0; i < ENTIRE_SET; i++) {
			if (i < SAMPLES)
				analysis.train(String.valueOf(i));
		}
		analysis.train(" ");
		analysis.train(null);
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), SAMPLES + 3);
		assertEquals(result.getTotalCount(), -1);

		assertEquals(result.getNullCount(), 2);
		assertEquals(result.getTotalNullCount(), -1);

		assertEquals(result.getBlankCount(), 1);
		assertEquals(result.getTotalBlankCount(), -1);


		assertEquals(result.getMinValue(), "0");
		assertEquals(result.getTotalMinValue(), null);
		assertEquals(result.getMaxValue(), "99");
		assertEquals(result.getTotalMaxValue(), null);

		assertEquals(result.getMinLength(), 1);
		assertEquals(result.getTotalMinLength(), -1);
		assertEquals(result.getMaxLength(), 2);
		assertEquals(result.getTotalMaxLength(), -1);

		assertEquals(result.getMean(), 49.5);
		assertEquals(result.getTotalMean(), null);
		assertEquals(result.getTotalStandardDeviation(), null);

	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testCardinalitySortedLong() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("testCardinalitySortedLong");
		analysis.setDebug(2);
		final int start = 1000;
		final int end = 100;

		for (int i = start; i > end; i--) {
			analysis.train(String.valueOf(i));
		}
		analysis.train("-2");
		analysis.train("10000");
		analysis.train("10000");

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), start - end + 3);
		assertEquals(result.getCardinality(), start - end + 2);
		assertEquals(result.getRegExp(), "[+-]?\\d{1,5}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "-2");
		assertEquals(result.getMaxValue(), "10000");
		final NavigableMap<String, Long> cardinalityDetails = result.getCardinalityDetails();
		assertEquals(cardinalityDetails.firstKey(), "-2");
		assertEquals(cardinalityDetails.lastKey(), "10000");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testCardinalitySortedDouble() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("testCardinalitySortedDouble");
		final int start = 1000;
		final int end = 100;

		for (int i = start; i >= end; i--) {
			analysis.train(String.valueOf(i) + "." + String.valueOf(i%10));
		}
		analysis.train("-2.0");
		analysis.train("10000.45");

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), start - end + 3);
		assertEquals(result.getCardinality(), start - end + 3);
		assertEquals(result.getRegExp(), "[+-]?\\d*\\.?\\d+");
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "-2.0");
		assertEquals(result.getMaxValue(), "10000.45");
		final NavigableMap<String, Long> cardinalityDetails = result.getCardinalityDetails();
		assertEquals(cardinalityDetails.firstKey(), "-2.0");
		assertEquals(cardinalityDetails.lastKey(), "10000.45");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testExternalUniqueness10K() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("testCardinalitySorted");
		final int SIZE = 10_000;

		for (int i = 0; i < SIZE; i++) {
			analysis.train(String.valueOf(i));
			if (i < SIZE/2)
				analysis.train(String.valueOf(i));
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), SIZE + SIZE/2);
		assertEquals(result.getCardinality(), SIZE);
		assertEquals(result.getDistinctCount(), SIZE);
		assertEquals(result.getUniqueness(), 0.5);
		assertEquals(result.getRegExp(), "\\d{1,4}");
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "0");
		assertEquals(result.getMaxValue(), "9999");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testExternalUniqueness100K() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("testCardinalitySorted");
		final int SIZE = 100_000;

		// External Knowledge applied
		analysis.setDistinctCount(SIZE);
		analysis.setUniqueness(0.5);

		for (int i = 0; i < SIZE; i++) {
			analysis.train(String.valueOf(i));
			if (i < SIZE/2)
				analysis.train(String.valueOf(i));
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), SIZE + SIZE/2);
		assertEquals(result.getCardinality(), analysis.getMaxCardinality());
		assertEquals(result.getDistinctCount(), SIZE);
		assertEquals(result.getUniqueness(), 0.5);
		assertEquals(result.getRegExp(), "\\d{1,5}");
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "0");
		assertEquals(result.getMaxValue(), "99999");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void keyFieldString() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("keyFieldString");
		final int start = 100000;
		final int end = 120000;

		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train("A" + i) && locked == -1)
				locked = i - start;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), end - start);
		assertEquals(result.getCardinality(), AnalysisConfig.MAX_CARDINALITY_DEFAULT);
		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}\\d{6}");
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getKeyConfidence(), 0.9);
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void notKeyField() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("notKeyField");
		final int start = 10000;
		final int end = start + AnalysisConfig.MAX_CARDINALITY_DEFAULT + 100;

		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i - start;
		}

		analysis.train(String.valueOf(start));

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), 1 + end - start);
		assertEquals(result.getCardinality(), AnalysisConfig.MAX_CARDINALITY_DEFAULT);
		assertEquals(result.getRegExp(), "\\d{5}");
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getKeyConfidence(), 0.0);
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void freeText() throws IOException, FTAException {
		final String[] samples = {
				"The main issue I have is that it did not arrive correctly calibrated.",
				"This was the most reasonably priced 4’ digital level I could find.",
				"I have been using a couple of this product for about two years and am glad I purchased it. I don't know how accurate the other brands are but this one is accurate enough for my purpose.",
				"This level arrived well packaged and quickly, and seems like it would last just fine on a normal job site.",
				"Light didn’t and still doesn’t work.",
				"Great little level. I love how it has its own carrying case that’s actually useful.",
				"I bought this level for the sole purpose of helping me to determine track grades on my HO scale model train layout.",
				"It is definitely worth every penny as far as I am concerned & certainly is a great product for its cost.",
				"I appreciate the batteries as an addition but if you don’t get a decent commercial battery you will run into what some of have run into.",
				"This is the first digital level I have ever bought, and I look forward to using it on a number of projects where eyeballing just isn’t quite good enough."
		};

		final TextAnalyzer analysis = new TextAnalyzer("Description");
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		final PluginDefinition defn = PluginDefinition.findByName("FREE_TEXT");
		assertEquals(result.getSemanticType(), defn.semanticType);
		assertEquals(result.getRegExp(), ".{36,185}");
		assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples) {
			assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void freeTextLowCardinality() throws IOException, FTAException {
		final String[] samples = {
				"Divorced - separated",
				"Divorced - not separated",
				"Married - sharing a house",
				"Single - living alone",
				"Surviving spouse",
				"Widower - husband died",
				"Widower - wife died"
		};

		final TextAnalyzer analysis = new TextAnalyzer("Marital Description");
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		final PluginDefinition defn = PluginDefinition.findByName("FREE_TEXT");
		assertEquals(result.getSemanticType(), defn.semanticType);
		assertEquals(result.getRegExp(), ".{16,25}");
		assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples) {
			assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void strange() throws IOException, FTAException {
		final String INTERESTING = "88-0828S7";
		final String[] samples = {
				"", "", "", "", "", "", "", "", "", "",
				"", "", "", "", "", "", "", "", "", "",
				"", "", "", "", "", "", "", "", "", "",
				INTERESTING, "", "", "", "", "", "", ""
		};

		final TextAnalyzer analysis = new TextAnalyzer("Fund Origin Council File_3");
		analysis.setDebug(2);
		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getBlankCount(), 37);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertEquals(result.getRegExp(), "\\d{2}-\\d{4}\\p{IsAlphabetic}\\d");
		assertEquals(result.getConfidence(), 1.0);

		assertTrue(INTERESTING.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void viznet3() throws IOException, FTAException {
		final String[] samples = {
				"688 8271 68", "", "876 2105 21", "654 9262 12", "", "299 8420 96", "",  "974 9680 53",
				"106 9951 52", "106 9951 52", "551 2387 53", "104 6247 01", "104 6247 01", "654 4365 27",
				"654 4365 27", "654 4264 33", "654 4264 33", "974 9680 53", "654 9105 28"
		};

		final TextAnalyzer analysis = new TextAnalyzer("VAT Registration Number");
		analysis.setDebug(2);
		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getBlankCount(), 3);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertEquals(result.getRegExp(), "\\d{3} \\d{4} \\d{2}");
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void viznet4() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Garbage");
		analysis.setDebug(2);
		analysis.train("        jQuery(function($){");

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void viznet5A() throws IOException, FTAException {
		final String[] samples = {
				"2010-01-13 09:58:25.443", "2010-02-02 12:13:36.132", "2010-06-22 18:55:04",
				"2010-06-22 18:54:46", "2010-02-03 13:00:49.746", "2010-01-14 10:15:08.388",
				"2010-01-14 10:15:16.041", "2010-01-14 10:14:59.964", "2010-02-02 12:02:38.444",
				"2010-02-02 12:03:35.649", "2010-01-04 09:42:56.82", "2010-02-04 18:23:49.946"
		};

		final TextAnalyzer analysis = new TextAnalyzer("data_devolucao");
		analysis.setDebug(2);
		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getMatchCount(), 10);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "yyyy-MM-dd HH:mm:ss.S{2,3}");
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{2,3}");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void viznet6() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("");
		analysis.setDebug(1);

		final long start = System.currentTimeMillis();
		for (int i = 0; i < 20_000_000; i++)
			analysis.train("0");

		final TextAnalysisResult result = analysis.getResult();
		System.err.printf("Duration(ms): %d%n", System.currentTimeMillis() - start);
		assertEquals(result.getRegExp(), "\\d");
	}



	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void embeddedNumerics() throws IOException, FTAException {
		final String[] samples = {
				"hello 45.6", "world 89.9", "5.89 world 87.33", "9.89 world 87.33", "4.89 world 87.33",
				"4.891 world 87.33", "4.89 world 87.33", "41.89 world 87.33", "4.89 world 87.33", "4.89 world 817.33",
				"41.89 world 87.133", "4.89 world 87.33", "hello 45.6", "world 89.9", "xoom 2456.890",
				"hello 45.6", "world 89.9", "5.89 world 87.33", "9.89 world 87.33", "4.89 world 87.33",

				"4.891 world 87.33", "4.89 world 87.33", "41.89 world 87.33",
				"4.89 world 87.33", "4.89 world 817.33", "41.89 world 87.33",
				"4.89 world 87.33", "4.89 world 817.33", "41.89 world 87.133",
				"4.89 world 87.33", "hello 45.6", "world 89.9", "xoom 2456.890",
		};

		final TextAnalyzer analysis = new TextAnalyzer("embeddedNumerics");
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertEquals(result.getRegExp(), ".{10,18}");
		assertEquals(result.getConfidence(), 1.0);

		int matched = 0;
		for (final String sample : samples)
			if (sample.matches(result.getRegExp()))
				matched++;

		assertEquals(result.getSampleCount(), matched);
	}



	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void viznet5B() throws IOException, FTAException {
		final String[] samples = {
				"2010-01-05 16:26:12.662", "2010-01-12 07:34:13.934", "2010-02-25 07:36:25.8",
				"2010-02-03 08:58:45.692", "2010-02-03 13:06:30.662", "2010-02-03 13:06:43.125",
				"2010-01-13 09:58:25.443", "2010-02-02 12:13:36.132", "2010-06-22 18:55:04",
				"2010-06-22 18:54:46", "2010-02-03 13:00:49.746", "2010-01-14 10:15:08.388"
		};

		final TextAnalyzer analysis = new TextAnalyzer("data_devolucao");
		analysis.setDebug(2);
		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getMatchCount(), 10);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "yyyy-MM-dd HH:mm:ss.S{1,3}");
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{1,3}");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void manyEyes1() throws IOException, FTAException {
		final String[] samples = {
				"New York, NY", "Davidson, TN", "Ocean, NJ", "Stark, OH", "Forsyth, NC", "Cumberland, NC",
				"Whatcom, WA", "Muscogee, GA", "Vanderburgh, IN", "Tolland, CT", "Fairfield, OH", "Frederick, VA",
				"Laurens, SC", "Huron, OH", "Sullivan, NH", "Silver Bow, MT", "Gunnison, CO", "Custer, MT",
				"Carbon, MT", "Lyman, SD", "Los Angeles, CA", "Cook, IL", "Wayne, MI", "Middlesex, MA",
				"St. Louis, MO", "Hartford, CT", "Philadelphia, PA", "Fulton, GA", "New Haven, CT",
		};

		final TextAnalyzer analysis = new TextAnalyzer("County, State");
		analysis.setDebug(2);
		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void pluginDefinitionRegex() throws IOException, FTAException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
		final String[] samples = {
				"Animal", "Vegetable", "Mineral", "Animal", "Mineral", "Mineral",
				"Mineral", "Vegetable", "Vegetable", "Vegetable", "Mineral", "Mineral",
				"Mineral", "Mineral", "Animal", "Vegetable", "Animal", "Vegetable",
				"Vegetable", "Mineral", "Animal", "Vegetable", "Mineral", "Mineral",
				"Mineral", "Mineral", "Mineral", "Animal", "Vegetable",
		};

		final TextAnalyzer analyzer = new TextAnalyzer("Guess");
		for (final String sample : samples)
			analyzer.train(sample);

		final TextAnalysisResult result = analyzer.getResult();

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), "(?i)(ANIMAL|MINERAL|VEGETABLE)");
		assertNull(result.getSemanticType());

		// Construct a new TextAnalyzer and add the Plugin Definition from the previous invocation
		// This time we should detect the new Semantic Type: 'Guess'
		final TextAnalyzer analyzerWithDefinition = new TextAnalyzer("Guess");
		analyzerWithDefinition.getPlugins().registerPlugins(new StringReader("[" + result.asPlugin(analyzer).toString() + "]"),
				analyzerWithDefinition.getStreamName(), analyzerWithDefinition.getConfig());

		for (final String sample : samples)
			analyzerWithDefinition.train(sample);

		final TextAnalysisResult resultWithDefinition = analyzerWithDefinition.getResult();

		assertEquals(resultWithDefinition.getSampleCount(), samples.length);
		assertEquals(resultWithDefinition.getBlankCount(), 0);
		assertEquals(resultWithDefinition.getNullCount(), 0);
		assertEquals(resultWithDefinition.getType(), FTAType.STRING);
		assertEquals(resultWithDefinition.getRegExp(), "(?i)(ANIMAL|MINERAL|VEGETABLE)");
		assertEquals(resultWithDefinition.getSemanticType(), "Guess");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void freeTextLowCardinalityMultiples() throws IOException, FTAException {
		final String[] samples = {
				"Divorced - separated",
				"Divorced - not separated",
				"Married - sharing a house",
				"Single - living alone",
				"Surviving spouse",
				"Widower - husband died",
				"Widower - wife died"
		};

		final TextAnalyzer analysis = new TextAnalyzer("Marital Description");
		for (int i = 0; i < 100; i++)
			for (final String sample : samples) {
				analysis.train(sample);
			}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), samples.length * 100L);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertEquals(result.getRegExp(), "(?i)(DIVORCED - NOT SEPARATED|DIVORCED - SEPARATED|MARRIED - SHARING A HOUSE|SINGLE - LIVING ALONE|SURVIVING SPOUSE|WIDOWER - HUSBAND DIED|WIDOWER - WIFE DIED)");
		assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples) {
			assertTrue(sample.matches(result.getRegExp()));
		}
	}

	class AnalysisThread implements Runnable {
		private final String id;
		private final int streamType;
		private final String[] stream;
		private final TextAnalysisResult answer;
		private final TextAnalyzer analysis;

		AnalysisThread(final String id, final int streamType, final String[] stream, final TextAnalysisResult answer) throws IOException, FTAException {
			this.id = id;
			this.streamType = streamType;
			this.stream = stream;
			this.answer = answer;
			analysis = new TextAnalyzer("AnalysisThread-<" + id + ">");
			//			logger.debug("Thread %s: created, Stream: type: %s, length: %d.",
			//					this.id, decoder[this.streamType], this.stream.length);
		}

		@Override
		public void run() {
			//			long start = System.currentTimeMillis();
			try {
				for (final String input : stream)
					analysis.train(input);

				final TextAnalysisResult result = analysis.getResult();

				assertEquals(result.getSampleCount(), answer.getSampleCount());
				assertEquals(result.getNullCount(), answer.getNullCount());
				assertEquals(result.getBlankCount(), answer.getBlankCount());
				if (!result.getRegExp().equals(answer.getRegExp()))
					assertEquals(result.getRegExp(), answer.getRegExp());
				assertEquals(result.getConfidence(), answer.getConfidence());
				assertEquals(result.getType(), answer.getType());
				assertEquals(result.getMinValue(), answer.getMinValue());
				assertEquals(result.getMaxValue(), answer.getMaxValue());
			} catch (FTAException e) {
				e.printStackTrace();
			}

			// logger.debug("Thread %s: exiting, duration %d.", id, System.currentTimeMillis() - start);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testSemanticTypeAccess() throws IOException, FTAException, FTAException, InterruptedException {
		boolean telephoneFound = false;
		boolean emailFound = false;

		for (final SemanticType semanticType : SemanticType.getAllSemanticTypes()) {
			if ("TELEPHONE".equals(semanticType.getId()))
				telephoneFound = true;
			if ("EMAIL".equals(semanticType.getId()))
				emailFound = true;
		}

		assertTrue(telephoneFound);
		assertTrue(emailFound);

		telephoneFound = false;
		emailFound = false;

		for (final SemanticType semanticType : SemanticType.getActiveSemanticTypes(Locale.US)) {
			if ("TELEPHONE".equals(semanticType.getId()))
				telephoneFound = true;
			if ("EMAIL".equals(semanticType.getId()))
				emailFound = true;
		}

		assertTrue(telephoneFound);
		assertTrue(emailFound);
	}


	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testTypes() throws IOException, FTAException, FTAException, InterruptedException {
		for (int i = 0; i < 100; i++) {
			final int index = random.nextInt(someSemanticTypes.length);
			final String semanticType = someSemanticTypes[index];
			final String heading = someSemanticTypesHeader[index];
			if (
					"COORDINATE_PAIR.DECIMAL".equals(semanticType) ||
					"COORDINATE.LONGITUDE_DECIMAL".equals(semanticType) ||
					"SSN".equals(semanticType)
					) {
				continue;
			}
			System.err.printf("%s: %d%n", semanticType, index);
			final TextAnalyzer analysis = new TextAnalyzer(heading);
			analysis.setLocale(Locale.US);
			final PluginDefinition pluginDefinition = PluginDefinition.findByName(semanticType);
			final LogicalType logicalType = LogicalTypeFactory.newInstance(pluginDefinition, analysis.getConfig());

			final int length = 30 + random.nextInt(10000);
			final String[] stream = new String[length];
			for (int j = 0; j < length; j++)
				stream[j] = logicalType.nextRandom();
			if (semanticType.endsWith("_CA"))
				analysis.setLocale(Locale.CANADA);

			for (final String input : stream)
				analysis.train(input);

			final TextAnalysisResult result = analysis.getResult();
			assertEquals(result.getSampleCount(), stream.length);
			if (result.getOutlierCount() != 0) {
				for (final Map.Entry<String, Long> outlier : result.getOutlierDetails().entrySet())
					System.err.println("'" + outlier.getKey() + "': " + outlier.getValue());
			}
			assertEquals(result.getConfidence(), 1.0, semanticType);
			final String semanticTypeDetected = result.getSemanticType();
			assertNotNull(semanticTypeDetected, semanticType);
			if (!semanticTypeDetected.equals(semanticType) &&
					!semanticTypeDetected.equals(semanticType.replaceAll("<LANGUAGE>", "EN")) &&
					!semanticTypeDetected.equals(semanticType.replaceAll("<LOCALE>", "en-US"))) {
				for (final String input : stream)
					System.err.println(input);
				fail("Input: " + semanticType + ", Result: " + semanticTypeDetected);
			}
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testThreading() throws IOException, FTAException, FTAException, InterruptedException {
		final int THREADS = 500;
		final Thread[] threads = new Thread[THREADS];

		for (int t = 0; t < THREADS; t++) {
			final int type = random.nextInt(TestUtils.testCaseOptions.length);
			final int length = 30 + random.nextInt(10000);
			final String[] stream = TestUtils.generateTestStream(type, length);

			final TextAnalyzer analysis = new TextAnalyzer("testThreading");
			for (final String input : stream)
				analysis.train(input);

			threads[t] = new Thread(new AnalysisThread(String.valueOf(t), type, stream, analysis.getResult()));
		}

		for (int t = 0; t < THREADS; t++)
			threads[t].start();

		for (int t = 0; t < THREADS; t++)
			if (threads[t].isAlive())
				threads[t].join();
	}

	class MemThread implements Runnable {
		private final String id;
		private final int samples;
		private final TextAnalyzer analysis;

		MemThread(final String id, final int samples) throws IOException, FTAException {
			this.id = id;
			this.samples = samples;
			analysis = new TextAnalyzer("AnalysisThread-<" + id + ">");
			//			logger.debug("Thread %s: created, Stream: type: %s, length: %d.",
			//					this.id, decoder[this.streamType], this.stream.length);
		}

		@Override
		public void run() {
			//			long start = System.currentTimeMillis();
			try {

				final TextAnalyzer analysis = new TextAnalyzer("testThreading");
				for (int i = 0; i < samples; i++) {
					final int type = random.nextInt(TestUtils.testCaseOptions.length);
					final int length = 30 + random.nextInt(10000);
					analysis.train(TestUtils.generateTestStream(type, 1)[0]);
				}

				final TextAnalysisResult result = analysis.getResult();

			} catch (FTAException e) {
				e.printStackTrace();
			}

			// logger.debug("Thread %s: exiting, duration %d.", id, System.currentTimeMillis() - start);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testMemThreading() throws IOException, FTAException, FTAException, InterruptedException {
		final int THREADS = 50;
		final Thread[] threads = new Thread[THREADS];
		final AllocationTracker tracker = new AllocationTracker();

		System.err.printf("Initialization - Allocated: %,d, Free memory: %,d%n", tracker.getAllocated(), Runtime.getRuntime().freeMemory());

		for (int t = 0; t < THREADS; t++) {
			threads[t] = new Thread(new MemThread(String.valueOf(t), 100000));
			System.err.printf("Thread creation - Allocated: %,d, Free memory: %,d%n", tracker.getAllocated(), Runtime.getRuntime().freeMemory());
		}

		for (int t = 0; t < THREADS; t++) {
			threads[t].start();
			System.err.printf("Thread start - Allocated: %,d, Free memory: %,d%n", tracker.getAllocated(), Runtime.getRuntime().freeMemory());
		}
		System.err.printf("Thread all running - Allocated: %,d, Free memory: %,d%n", tracker.getAllocated(), Runtime.getRuntime().freeMemory());

		for (int t = 0; t < THREADS; t++)
			if (threads[t].isAlive())
				threads[t].join();
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void checkLengths_1() throws IOException, FTAException, FTAException {
		final TextAnalyzer model = new TextAnalyzer("vehicle_model_year");

		for (int year = 1980; year <= 2024; year++) {
			model.train(String.valueOf(year));
			model.train(String.valueOf(year));
		}

		// Add an outlier
		model.train("1196");

		// Add an invalid entry
		model.train("ABCD");

		final TextAnalysisResult result = model.getResult();
		assertEquals(result.getSampleCount(), 92L);
		assertEquals(result.getMatchCount(), 90L);
		assertEquals(result.getMinLength(), 4);
		assertEquals(result.getMaxLength(), 4);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getRegExp(), "\\d{4}");
		assertEquals(result.getOutlierCount(), 1L);
		assertEquals(result.getOutlierDetails().size(), 1);
		assertTrue(result.getOutlierDetails().containsKey("1196"));
		assertEquals(result.getInvalidCount(), 1L);
		assertEquals(result.getInvalidDetails().size(), 1);
		assertTrue(result.getInvalidDetails().containsKey("ABCD"));

		long[] lengths = result.getLengthFrequencies();
		assertEquals(lengths[4], 92L);
		assertEquals(Arrays.stream(result.getLengthFrequencies()).sum(), result.getSampleCount());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void checkLengths_2() throws IOException, FTAException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("county");
		analysis.setLocale(Locale.US);
		final String[] samples = {
			"Lane", "Lane", "Curry", "Lincoln", "Lane", "Hood River",
			"Baker, Crook, Gilliam, Grant, Harney, Hood River, Lake, Malheur, Morrow, Sherman, Umatilla, Union, Wallowa, Wasco, Wheeler",
			"Multnomah", "Linn; Benton", "Washington", "Lane", "Marion", "Douglas", "Harney", "Marion", "Lincoln", "Multnomah",
			"multiple", "Multnomah", "Malheur", "Lane", "Marion", "Umatilla", "Coos", "Umatilla", "Malheur", "Multnomah",
			"Multnomah", "Deschutes", "Multnomah", "Columbia", "Clatsop", "Umatilla", "Washington", "Malheur", "Polk", "Lincoln",
			"Lane", "statewide", "statewide", "Jackson", "Lane", "Washington", "Coos", "Clackamas", "statewide", "Lane",
			"Washington", "statewide", "statewide", "Washington", "Multnomah", "Washington", "Clackamas", "Curry", "Yamhill", "Polk",
			"Multnomah", "Lane", "statewide", "Multnomah", "Multnomah", "Lane", "Linn", "multiple", "Multnomah", "Baker",
			"Hood River", "multiple", "Josephine", "statewide", "Washington", "Benton", "Linn", "multiple", "Hood River", "multiple",
			"Coos", "Clatsop", "Josephine", "Columbia", "Multnomah", "Lane", "Washington", "Josephine", "Multnomah", "Clackamas",
			"multiple", "Clatsop", "Columbia", "Multnomah", "Benton", "Washington", "Lane", "Lane", "Lane", "Polk",
			"Washington", "Multnomah", "Multnomah", "Marion", "Washington", "Clatsop", "Linn", "Columbia", "Multnomah", "Multnomah",
			"statewide", "Washington", "Lane", "Polk", "statewide", "statewide", "Clackamas", "Lane", "Multnomah", "Marion",
			"Washington", "Umatilla", "Clatsop", "Linn", "statewide", "Multnomah", "Multnomah", "Klamath", "statewide", "Multnomah",
			"Jackson", "Lane", "Polk", "statewide", "Lane", "statewide", "multiple", "Deschutes", "Multnomah", "Multnomah",
			"Washington", "Washington", "Hood River", "Clatsop", "Linn", "statewide", "Clatsop", "Lane", "Multnomah", "Multnomah",
			"Multnomah", "Washington", "Washington"
		};

		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getSampleCount(), 150L);
		assertEquals(result.getNullCount(), 0L);
		assertEquals(result.getMatchCount(), 126L);
		assertEquals(result.getMinLength(), 4);
		assertEquals(result.getMaxLength(), 122);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getSemanticType(), "STATE_PROVINCE.COUNTY_US");
		assertEquals(result.getOutlierCount(), 0L);
		assertEquals(result.getInvalidCount(), 4L);
		assertEquals(Arrays.stream(result.getLengthFrequencies()).sum(), result.getSampleCount());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void checkLengths_3() throws IOException, FTAException, FTAException {
		final TextAnalyzer model = new TextAnalyzer("Type");

		for (int i = 1; i <= 100; i++) {
			model.train("City");
			model.train("Town");
			if (i%10 == 0)
				model.train("NULL");
		}

		final TextAnalysisResult result = model.getResult();
		assertEquals(result.getSampleCount(), 210L);
		assertEquals(result.getNullCount(), 10L);
		assertEquals(result.getMatchCount(), 200L);
		assertEquals(result.getMinLength(), 4);
		assertEquals(result.getMaxLength(), 4);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), "(?i)(CITY|TOWN)");
		assertEquals(result.getOutlierCount(), 0L);

		long[] lengths = result.getLengthFrequencies();
		assertEquals(lengths[4], 200L);
		assertEquals(Arrays.stream(result.getLengthFrequencies()).sum(), result.getSampleCount() - result.getNullCount());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void issue130() throws IOException, FTAException, FTAException {
		final String[] inputs = {
				"Guinea-Bissau", "Sri Lanka", "United Arab Emirates", "Congo, Republic of the", "Poland",
				"Philippines", "Panama", "Croatia", "Papua New Guinea", "Sierra Leone",
				"Sweden", "Central African Republic", "Nigeria", "Italy", "Poland",
				"China", "Bolivia", "Sierra Leone", "Luxembourg", "Luxembourg",
				"Morocco", "Israel", "New Zealand", "United Kingdom", "Haiti",
				"Liberia", "Belarus", "Luxembourg", "Poland", "Niger",
				"Saint Kitts and Nevis", "Syria", "Tuvalu", "Singapore", "Algeria",
				"Costa Rica", "Korea, North", "Swaziland", "Congo", "Lithuania",
				"Monaco", "Kiribati", "Israel", "Nepal", "Angola",
				"Nepal", "Nigeria", "Honduras", "Togo", "Liberia",
				"Japan", "Greece", "Philippines", "Belize", "Palau",
				"Brazil", "Bangladesh", "Bahrain", "Marshall Islands", "Romania",
				"San Marino", "Nigeria", "Vanuatu", "Syria", "Denmark",
				"Paraguay", "Malawi", "Papua New Guinea", "Trinidad and Tobago", "Solomon Islands",
				"San Marino", "Eritrea", "Eritrea", "Ecuador", "Guatemala",
				"Iraq", "Cameroon", "Latvia", "Honduras", "Germany",
				"Congo, Republic of the", "Madagascar", "Equatorial Guinea", "Norway", "Bhutan",
				"Sao Tome and Principe", "Bhutan", "Portugal", "Honduras", "Korea, South",
				"El Salvador", "Suriname", "Honduras", "Andorra", "Romania",
				"Estonia", "Samoa", "Costa Rica", "Kuwait", "Turkmenistan"
		};

		final TextAnalyzer RHS = new TextAnalyzer("Right");
		RHS.setLocale(Locale.forLanguageTag("en_IN"));
		RHS.setTrace("enabled=true,directory=/tmp");
		for (final String input : inputs)
			RHS.train(input);
		String jsonRHS = RHS.serialize();
		final TextAnalyzer hydratedRHS = TextAnalyzer.deserialize(jsonRHS);
		final TextAnalysisResult resultRHS_WHY = hydratedRHS.getResult();
		final TextAnalysisResult resultRHS_WHY_WHY = hydratedRHS.getResult();
		final TextAnalysisResult resultRHS_WHY_WHY_WHY = hydratedRHS.getResult();

		final TextAnalyzer LHS = new TextAnalyzer("Left");
		LHS.setLocale(Locale.forLanguageTag("en_IN"));
		LHS.setTrace("enabled=true,directory=/tmp");
		final TextAnalysisResult resultLHS_WHY = LHS.getResult();

		final TextAnalyzer merged = TextAnalyzer.merge(LHS, hydratedRHS);

		final TextAnalysisResult result = merged.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
	}

	class GetPlugin {
		// one instance of plugins per thread
		private final ThreadLocal<TextAnalyzer> textAnalyzer = new ThreadLocal<>();

		public Plugins getPlugins() {
			TextAnalyzer textAnalyzer = this.textAnalyzer.get();
			if (textAnalyzer == null) {
				// initialize textAnalyzer
				textAnalyzer = new TextAnalyzer("getPlugins");
				textAnalyzer.registerDefaultPlugins(textAnalyzer.getConfig());
				this.textAnalyzer.set(textAnalyzer);
			}
			return textAnalyzer.getPlugins();
		}
	}

	public static void main(final String[] args) throws FTAException {
		final String[] inputs = {
				"Anaïs Nin", "Gertrude Stein", "Paul Cézanne", "Pablo Picasso", "Theodore Roosevelt",
				"Henri Matisse", "Georges Braque", "Henri de Toulouse-Lautrec", "Ernest Hemingway",
				"Alice B. Toklas", "Eleanor Roosevelt", "Edgar Degas", "Pierre-Auguste Renoir",
				"Claude Monet", "Édouard Manet", "Mary Cassatt", "Alfred Sisley",
				"Camille Pissarro", "Franklin Delano Roosevelt", "Winston Churchill"
		};

		// Use simple constructor - for improved detection provide an AnalyzerContext (see Contextual example).
		final TextAnalyzer analysis = new TextAnalyzer("Famous");
		analysis.setLocale(Locale.US);
		analysis.setTrace("enabled=true,directory=/tmp");

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		if (result.getSemanticType() == null) {
			System.err.printf("Current locale is '%s' - which does not support Semantic Type: NAME.FIRST_LAST", Locale.getDefault());
		}
		else {
			System.err.printf("Semantic Type: %s (%s)%n", result.getSemanticType(), result.getType());
			System.err.println("Detail: " + result.asJSON(true, 1));
		}
	}

	class PluginThread implements Runnable {
		private final String id;

		PluginThread(final String id) throws IOException, FTAException {
			this.id = id;
		}

		@Override
		public void run() {
			final GetPlugin pluginGetter = new GetPlugin();

			for (int i = 0; i < 1000; i++)
				pluginGetter.getPlugins();
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testThreadingIssue() throws IOException, FTAException, InterruptedException {
		final int THREADS = 1000;
		final Thread[] threads = new Thread[THREADS];

		for (int t = 0; t < THREADS; t++)
			threads[t] = new Thread(new PluginThread(String.valueOf(t)));

		for (int t = 0; t < THREADS; t++)
			threads[t].start();

		for (int t = 0; t < THREADS; t++)
			if (threads[t].isAlive())
				threads[t].join();
	}

	private static String someSemanticTypes[] = {
			"EMAIL", "URI.URL", "IPADDRESS.IPV4", "IPADDRESS.IPV6", "TELEPHONE", "GUID",
			"POSTAL_CODE.ZIP5_US", "POSTAL_CODE.POSTAL_CODE_CA",
			"STREET_ADDRESS_EN", "GENDER.TEXT_<LANGUAGE>", "COUNTRY.TEXT_EN",
			"STATE_PROVINCE.PROVINCE_CA", "STATE_PROVINCE.STATE_US", "STATE_PROVINCE.STATE_PROVINCE_NA",
			"CURRENCY_CODE.ISO-4217", "COUNTRY.ISO-3166-3", "COUNTRY.ISO-3166-2",
			"AIRPORT_CODE.IATA", "CITY", "SSN",
			"NAME.FIRST", "NAME.LAST", "NAME.LAST_FIRST", "NAME.FIRST_LAST",
			"CREDIT_CARD_TYPE", "LANGUAGE.ISO-639-2", "LANGUAGE.TEXT_EN",
			"MONTH.ABBR_<LOCALE>", "MONTH.FULL_<LOCALE>", "COORDINATE.LATITUDE_DECIMAL", "COORDINATE.LONGITUDE_DECIMAL", "COORDINATE_PAIR.DECIMAL"
	};

	private static String someSemanticTypesHeader[] = {
			"EMAIL", "URI.URL", "IPADDRESS", "IPADDRESS", "TELEPHONE", "GUID",
			"POSTAL_CODE", "POSTAL_CODE",
			"ADDRESS", "GENDER", "COUNTRY",
			"PROVINCE", "STATE", "STATE",
			"CURRENCY", "COUNTRY", "COUNTRY",
			"AIRPORT_CODE", "CITY", "SSN",
			"NAME", "NAME", "NAME", "NAME",
			"CREDIT_CARD_TYPE", "Language", "Language",
			"Month", "Month", "Latitude", "Longitude", "COORDINATES"
	};

	class LogicalTypeThread implements Runnable {
		private final String id;

		LogicalTypeThread(final String id) throws IOException, FTAException {
			this.id = id;
		}

		@Override
		public void run() {
			LogicalType logical = null;
			final Locale locale = Locale.forLanguageTag("en-US");
			do {
				final String semanticType = someSemanticTypes[random.nextInt(someSemanticTypes.length)];
				try {
					final PluginDefinition defn = PluginDefinition.findByName(semanticType);
					if (!defn.isLocaleSupported(locale))
						System.err.println("Attempting to create an intance of LogicalType: " + defn.semanticType + " in an unsupported Locale");
					logical = LogicalTypeFactory.newInstance(defn, new AnalysisConfig(locale));
				} catch (FTAException e) {
					e.printStackTrace();
				}
			} while (!(logical instanceof LogicalTypeRegExp) || ((LogicalTypeRegExp)logical).isRegExpComplete());

			for (int i = 0; i < 1000; i++) {
				final String value = logical.nextRandom();
				if (logical.isRegExpComplete() && !logical.isValid(value)) {
					System.err.println("Issue with LogicalType'" + logical.getDescription() + "', value: " + value + "\n");
					assertTrue(logical.isValid(value), value);
				}
			}
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testBulkWithNull() throws IOException, FTAException {
		final long SIZE = 1000L;
		TextAnalysisResult result;

		// 1 - BULK make sure "NULL" is recognized as NULL
		final Map<String, Long> basic = new HashMap<>();
		basic.put("0", SIZE);
		basic.put("1", SIZE);
		basic.put("2", SIZE);
		basic.put("3", SIZE);
		basic.put("NULL", SIZE);

		TextAnalyzer analysisBulk = new TextAnalyzer("testBulkWithNull");
		assertTrue(analysisBulk.isEnabled(Feature.NULL_TEXT_AS_NULL));
		analysisBulk.trainBulk(basic);

		result = analysisBulk.getResult();
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getSampleCount(), SIZE * 5);
		assertEquals(result.getNullCount(), SIZE);

		// 2 - BULK make sure "NULL" is NOT recognized as NULL
		analysisBulk = new TextAnalyzer("testBulkWithNull");
		analysisBulk.configure(TextAnalyzer.Feature.NULL_TEXT_AS_NULL, false);
		analysisBulk.trainBulk(basic);

		result = analysisBulk.getResult();
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getSampleCount(), SIZE * 5);
		assertEquals(result.getNullCount(), 0);

		// 3 - Non BULK make sure "NULL" is recognized as NULL
		TextAnalyzer analysis = new TextAnalyzer("testBulkWithNull");

		for (long i = 0; i < SIZE; i++) {
			analysis.train("0");
			analysis.train("1");
			analysis.train("2");
			analysis.train("3");
			analysis.train("NULL");
		}

		result = analysis.getResult();

		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getSampleCount(), SIZE * 5);
		assertEquals(result.getNullCount(), SIZE);

		// 3 - Non BULK make sure "NULL" is NOT recognized as NULL
		// Non BULK - Third test make sure "NULL" is recognized as NULL
		analysis = new TextAnalyzer("testBulkWithNull");
		analysis.configure(TextAnalyzer.Feature.NULL_TEXT_AS_NULL, false);

		for (long i = 0; i < SIZE; i++) {
			analysis.train("0");
			analysis.train("1");
			analysis.train("2");
			analysis.train("3");
			analysis.train("NULL");
		}

		result = analysis.getResult();

		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getSampleCount(), SIZE * 5);
		assertEquals(result.getNullCount(), 0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testIntegerCache() throws IOException, FTAException {
		final long SIZE = 20_000_000L;

		final TextAnalyzer analysis = new TextAnalyzer("integerCacheOff");
		long start = System.currentTimeMillis();
		for (long i = 0; i < SIZE/4; i++) {
			analysis.train("0");
			analysis.train("1");
			analysis.train("2");
			analysis.train("3");
		}
		analysis.train("4");
		analysis.train("5");
		analysis.train("6");
		analysis.train("7");
		analysis.train("8");
		analysis.train("9");
		analysis.train("10");
		analysis.train("11");
		analysis.train(null);

		TextAnalysisResult result = analysis.getResult();
		System.err.printf("Duration(ms): %d%n", System.currentTimeMillis() - start);

		assertEquals(result.getSampleCount(), SIZE + 9);
		assertEquals(result.getMatchCount(), SIZE + 8);
		assertEquals(result.getNullCount(), 1);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getMinValue(), "0");
		assertEquals(result.getMaxValue(), "11");
		assertEquals(result.getMinLength(), 1);
		assertEquals(result.getMaxLength(), 2);

		final TextAnalyzer analysisCache = new TextAnalyzer("integerCacheOff");

		final Map<String, Long> basic = new HashMap<>();
		basic.put("0", SIZE/4);
		basic.put("1", SIZE/4);
		basic.put("2", SIZE/4);
		basic.put("3", SIZE/4);
		basic.put("4",  1L);
		basic.put("5",  1L);
		basic.put("6",  1L);
		basic.put("7",  1L);
		basic.put("8",  1L);
		basic.put("9",  1L);
		basic.put("10",  1L);
		basic.put("11",  1L);
		basic.put(null,  1L);
		start = System.currentTimeMillis();
		analysisCache.trainBulk(basic);

		result = analysisCache.getResult();
		System.err.printf("Duration(ms): %d%n", System.currentTimeMillis() - start);

		assertEquals(result.getSampleCount(), SIZE + 9);
		assertEquals(result.getMatchCount(), SIZE + 8);
		assertEquals(result.getNullCount(), 1);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getMinValue(), "0");
		assertEquals(result.getMaxValue(), "11");
		assertEquals(result.getMinLength(), 1);
		assertEquals(result.getMaxLength(), 2);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void testLogicalTypeThreading() throws IOException, FTAException, InterruptedException {
		final int THREADS = 1000;
		final Thread[] threads = new Thread[THREADS];

		for (int t = 0; t < THREADS; t++)
			threads[t] = new Thread(new LogicalTypeThread(String.valueOf(t)));

		for (int t = 0; t < THREADS; t++)
			threads[t].start();

		for (int t = 0; t < THREADS; t++)
			if (threads[t].isAlive())
				threads[t].join();
	}

	//@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void fuzzInt() throws IOException, FTAException {
		final int SAMPLES = 1000;
		final int errorRate = 1;

		for (int iter = 0; iter < 100; iter++) {
			final TextAnalyzer analysis = new TextAnalyzer("fuzzInt");
			analysis.setThreshold(100 - errorRate);
			assertEquals(analysis.getThreshold(), 100 - errorRate);
			final int length = random.nextInt(9);
			final long low = (long)Math.pow(10, length);
			long lowest = low;
			long lowestFloat = low;
			final int lowLength = String.valueOf(low).length();
			final long high = low + SAMPLES - 1;
			final int highLength = String.valueOf(high).length();
			int misses = 0;
			final boolean sticky = random.nextBoolean();
			boolean isNegative = false;
			int floats = 0;
			int strings = 0;
			int nulls = 0;
			int blanks = 0;
			int errorCase = -1;
			long firstFloat = -1;
			final String[] errorCaseDecode = { "String", "NegativeInt", "Double", "negativeDouble", "null", "blank" };

			for (long i = low; i <= high; i++) {
				if (i != low && i != high && random.nextInt(99) < 2) {
					if (errorCase == -1 || !sticky)
						errorCase = random.nextInt(6);
					switch (errorCase) {
					case 0:
						// String
						analysis.train("hello");
						strings++;
						misses++;
						break;
					case 1:
						// NegativeInt
						analysis.train(String.valueOf(-i));
						isNegative = true;
						lowest = -i;
						break;
					case 2:
						// Double
						analysis.train(String.valueOf((double)i));
						floats++;
						if (firstFloat == -1)
							firstFloat = i - low - nulls - blanks;
						misses++;
						break;
					case 3:
						// Negative Double
						analysis.train(String.valueOf((double)-i));
						isNegative = true;
						floats++;
						lowestFloat = -i;
						if (firstFloat == -1)
							firstFloat = i - low - nulls - blanks;
						misses++;
						break;
					case 4:
						// Null
						analysis.train(null);
						nulls++;
						misses++;
						break;
					case 5:
						// Blank
						analysis.train("");
						blanks++;
						misses++;
						break;
					}
				}
				else
					analysis.train(String.valueOf(i));
			}

			FTAType answer;
			String re = "";
			String min;
			String max;
			if (firstFloat != -1 && firstFloat < analysis.getDetectWindow() || floats >= (errorRate * SAMPLES)/100) {
				misses -= floats;
				answer = FTAType.DOUBLE;
				min = String.valueOf((double)Math.min(lowest, lowestFloat));
				max = String.valueOf((double)high);
				re += min.charAt(0) == '-' ? "-?\\d+|-?(\\d+)?\\.\\d+" : "\\d+|(\\d+)?\\.\\d+";
			}
			else {
				if (isNegative)
					re += "-?";
				re += "\\d{" + lowLength;
				if (lowLength != highLength)
					re += "," + highLength;
				re += "}";
				answer = FTAType.LONG;
				min = String.valueOf(lowest);
				max = String.valueOf(high);
			}

			logger.debug("Iter: %d, length: %d, start: %d, sticky: %b, re: %s, floats: %d, firstFloat: %d, strings: %d, errorCase: %s\n",
					iter, length, low, sticky, re, floats, firstFloat, strings, sticky ? errorCaseDecode[errorCase] : "Variable");

			final TextAnalysisResult result = analysis.getResult();

			assertEquals(result.getSampleCount(), SAMPLES);
			assertEquals(result.getNullCount(), nulls);
			assertEquals(result.getBlankCount(), blanks);
			assertEquals(result.getRegExp(), re);
			assertEquals(result.getConfidence(), (double)(SAMPLES-misses)/(SAMPLES - blanks - nulls));
			assertEquals(result.getType(), answer);
			assertEquals(result.getMinValue(), min);
			assertEquals(result.getMaxValue(), max);
		}
	}
}
