/*
 * Copyright 2017-2024 Tim Segall
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
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.security.SecureRandom;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.FTAUnsupportedLocaleException;
import com.cobber.fta.core.Utils;
import com.cobber.fta.plugins.Gender;

public class TestMerge {
	private static final SecureRandom random = new SecureRandom();

	private final List<String> samplesBLANK = new ArrayList<>();
	private final List<String> samplesNULL = new ArrayList<>();
	private final List<String> samplesBLANKORNULL = new ArrayList<>();
	private final List<String> samplesAlphaData = new ArrayList<>();

	private final static String[] shortStrings = {
			"baby", "back", "bad", "bag", "ball", "bank", "base", "bath", "be", "bean",
			"bear", "bed", "beer", "bell", "best", "big", "bird", "bit", "bite", "black",
			"bleed", "block", "blood", "blow", "blue", "board", "boat", "body", "boil",
			"bone", "book", "born", "both", "bowl", "box", "boy", "brave", "bread", "break",
			"bring", "brown", "brush", "build", "burn", "bus", "busy", "but", "buy",
			"cake", "call", "can", "cap", "car", "card", "care", "carry", "case", "cat", "catch", "chair",
			"chance", "change", "chase", "cheap", "cheese", "child", "choose", "city", "class", "clever",
			"clean", "clear", "climb", "clock", "cloth", "cloud", "close", "coat", "coin", "cold", "comb",
			"come", "cook", "cool", "corn", "cost", "count", "cover", "crash", "cross", "cry", "cup", "cut"
	};

	private final static String[] longStrings = {
			"across", "active", "activity", "afraid", "already", "always", "amount", "another", "answer", "anyone",
			"anything", "anytime", "appear", "around", "arrive", "attack", "autumn", "basket", "beautiful", "bedroom",
			"behave", "before", "behind", "besides", "better", "between", "birthday", "border", "borrow", "bottle",
			"bottom", "branch", "breakfast", "breathe", "bridge", "bright", "brother", "business", "candle", "careful",
			"careless", "central", "century", "certain", "chance", "change", "cheese", "chicken", "children", "chocolate",
			"choice", "choose", "circle", "clever", "clothes", "cloudy", "coffee", "collect", "colour", "comfortable",
			"common", "compare", "complete", "computer", "condition", "continue", "control", "copper", "corner", "correct",
			"contain", "country", "course", "cupboard", "dangerous", "daughter", "decide", "decrease", "depend", "destroy",
			"develop", "different", "difficult", "dinner", "direction", "discover", "double", "education", "effect", "either",
			"electric", "elephant", "enough", "entrance", "escape", "evening", "everyone", "everybody", "examination", "example",
			"except", "excited", "exercise", "expect", "expensive", "explain", "extremely"
	};

	@BeforeClass(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void classSetup() throws FTAPluginException, FTAUnsupportedLocaleException {
		String sample;
		for (int i = 60; i <= 90; i++) {
			sample = Utils.repeat(' ', i);
			samplesBLANK.add(sample);
		}

		for (int i = 1; i < 30; i++)
			samplesNULL.add(null);

		for (int i = 1; i < 30; i++) {
			sample = Utils.repeat(' ', i);
			samplesBLANKORNULL.add(sample);
			samplesBLANKORNULL.add(null);
		}

		for (int i = 0; i < 26; i++) {
			sample = Utils.repeat((char)('A' + i), i + 1);
			samplesAlphaData.add(sample);
			samplesAlphaData.add(null);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void smallSets() throws IOException, FTAException {
		final int SAMPLE_COUNT = 20;

		TextAnalyzer even = new TextAnalyzer("Even");
		for (int i = 0; i < SAMPLE_COUNT; i++)
			even.train(String.valueOf(i * 2));

		TextAnalyzer odd = new TextAnalyzer("Even");
		for (int i = 0; i < SAMPLE_COUNT; i++)
			odd.train(String.valueOf(i * 2 + 1));

		TextAnalyzer merged = TextAnalyzer.merge(even, odd);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getSampleCount(), 2 * SAMPLE_COUNT);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void differentBaseTypes() throws IOException, FTAException {
		String[] zips = {
			"95051", "95037", "95111", "95148", "95112", "95111", "95134", "95033", "93230", "95125",
			"95050", "95112", "95112", "95125", "95127", "95112", "95134", "94040", "95020", "93230",
			"95122", "95123", "95125", "95050", "95124", "95123", "94403", "93230", "94089", "95111",
			"95966", "95050", "95127", "95116", "94306", "95112", "95966", "95129", "95008", "95125",
			"94301", "95116", "95966", "95020", "94578", "95966", "95110", "95966", "94085", "95111",
			"95112", "95127", "93230", "95008", "95020", "94306", "95020", "95110", "95118", "95135",
			"95051", "95127", "95117", "95117", "95351", "95966", "95050", "95116", "95020", "97031",
			"95050", "95127", "94301", "94306", "95051", "95124", "95116", "95134", "95014", "95023",
			"95366", "95051", "95122", "95051", "95008", "95124", "95134", "95133", "95032", "95127",
			"95050", "95051", "95121", "95020", "95134", "93230", "95966", "95128", "95123", "95111",
			"95051", "95124", "94306", "95020", "95014", "93230", "97532", "95139", "95112", "95036",
			"95051", "95116", "94306", "95050", "95112", "95148", "94621", "95129", "95125", "95125",
			"95020", "95139", "95121", "95112", "85206", "95129", "95129", "95129", "95131", "95037",
			"95051", "95122", "95020", "95037", "95051", "95966", "94587", "95121", "95116", "95126",
			"95122", "94085", "95116", "95050", "95112", "93230", "95966", "95051", "93230", "95132",
			"95051", "95111", "95020", "95125", "95110", "95111", "93230", "94024", "94301", "95112",
			"95122", "94089", "94306", "95125", "95111", "95131", "95127", "95127", "95020", "95134",
			"95131", "95121", "95020", "95032", "95112", "95112", "95008", "95124", "95050", "93230",
			"94306", "95112", "95123", "95008", "95128", "85206", "94024", "95118", "95120", "95112",
			"94612", "95116", "95604", "85206", "95020", "95123", "94089", "85206", "95136", "95117"
		};

		String[] hidden = {
			"95118", "01810", "N/A", "95118", "N/A", "95020", "95121", "95148", "N/A", "95112",
			"N/A", "N/A", "95121", "95020", "N/A", "N/A", "N/A", "95128", "95123", "95111",
			"95051", "N/A", "N/A", "N/A", "90272", "95008", "N/A", "95132", "95122", "N/A",
			"95148", "95133", "N/A", "N/A", "N/A", "N/A", "95050", "95122", "N/A", "N/A",
		};

		TextAnalyzer t1 = new TextAnalyzer("resident_zip");
		t1.setLocale(Locale.US);
		for (int i = 0; i < zips.length; i++)
			t1.train(zips[i]);

		TextAnalyzer t2 = new TextAnalyzer("ziip");
		t2.setLocale(Locale.US);
		for (int i = 0; i < hidden.length; i++)
			t2.train(hidden[i]);

		TextAnalyzer merged = TextAnalyzer.merge(t1, t2);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.LONG);
		assertTrue(mergedResult.isSemanticType());
		final PluginDefinition defn = PluginDefinition.findByName("POSTAL_CODE.ZIP5_US");
		assertEquals(mergedResult.getSemanticType(), defn.semanticType);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void splitLogical() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;

		final List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			shardOne.add("MALE");

		final List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			shardTwo.add("FEMALE");
		final TextAnalyzer merged = checkTextAnalyzerMerge(shardOne, shardTwo, "MALE_FEMALE", Locale.forLanguageTag("en-US"), true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.STRING);
		assertTrue(mergedResult.isSemanticType());
		assertEquals(mergedResult.getSemanticType(), Gender.SEMANTIC_TYPE + "EN");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void exerciseSerialization() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Gender");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String[] options = { "Male", "Female", "Unknown" };
		final Random r = new Random();
		final int ITERATIONS = 10000;

		String serialized = analysis.serialize();
		TextAnalyzer t = TextAnalyzer.deserialize(serialized);
		long trainTime = 0;
		long serializeTime = 0;
		long deserializeTime = 0;
		long testStart = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; i++) {
			long start = System.currentTimeMillis();
			t.train(options[r.nextInt(options.length)]);
			long postTrain = System.currentTimeMillis();
			serialized = t.serialize();
			long postSerialize = System.currentTimeMillis();
			t = TextAnalyzer.deserialize(serialized);
			long postDeserialize = System.currentTimeMillis();

			trainTime += postTrain - start;
			serializeTime += postSerialize - postTrain;
			deserializeTime += postDeserialize - postSerialize;
		}

		System.err.printf(
				"Test duration: %dms, total training: %dms (%dμs per), total serialization: %dms (%dμs per), deserialization: %dms (%dμs per)\n",
				System.currentTimeMillis() - testStart, trainTime, (trainTime * 1000) / ITERATIONS, serializeTime,
				(serializeTime * 1000) / ITERATIONS, deserializeTime, (deserializeTime * 1000) / ITERATIONS);
		final TextAnalysisResult result = t.getResult();

		assertEquals(result.getSemanticType(), "GENDER.TEXT_EN");
		assertEquals(result.getType(), FTAType.STRING);

		// Given the Semantic Type we retrieve the associated plugin
		LogicalType semanticType = t.getPlugins().getRegistered(result.getSemanticType());

		// Use the plugin to get the non-localized description
		assertEquals(semanticType.getDescription(), "Gender");
		assertFalse(semanticType.isValid("BLUE"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void testBulkLong() throws IOException, FTAException {
		final long SAMPLE_COUNT = 100L;

		final List<String> samplesLong100 = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesLong100.add("100");
		samplesLong100.add(null);
		samplesLong100.add(" ");

		final List<String> samplesLong200 = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesLong200.add("200");
		samplesLong200.add(null);
		samplesLong200.add(null);
		samplesLong200.add("  ");
		samplesLong200.add("  ");
		samplesLong200.add("x");

		final TextAnalyzer merged = checkTextAnalyzerMerge(samplesLong100, samplesLong200, "long_long", null, true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.LONG);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void issue95_A() throws IOException, FTAException {
		final long SAMPLE_COUNT = 100000L;

		final List<String> samplesLong = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesLong.add(String.valueOf(i));

		final List<String> samplesNull = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesNull.add(null);

		final TextAnalyzer merged = checkTextAnalyzerMerge(samplesLong, samplesNull, "long_null", null, true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.LONG);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void issue95_B() throws IOException, FTAException {
		final long SAMPLE_COUNT = 100000L;

		final List<String> samplesNull = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesNull.add(null);

		final List<String> samplesLong = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesLong.add(String.valueOf(i));


		final TextAnalyzer merged = checkTextAnalyzerMerge(samplesNull, samplesLong, "null_long", null, true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.LONG);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void issue97_A() throws IOException, FTAException {
		final String[] fmtYYYY_MM_DD = {
				"2020-06-29", "2021-07-18", "2022-04-20", "2022-06-19", "2024-06-06"
		};

		final String[] fmtMMM_D_YYYY = {
			"Aug 1, 2019", "July 18, 2019"
		};

		TextAnalyzer t1 = new TextAnalyzer("updated");
		for (int i = 0; i < fmtYYYY_MM_DD.length; i++)
			t1.train(fmtYYYY_MM_DD[i]);

		TextAnalyzer t2 = new TextAnalyzer("updated");
		for (int i = 0; i < fmtMMM_D_YYYY.length; i++)
			t2.train(fmtMMM_D_YYYY[i]);

		TextAnalyzer merged = TextAnalyzer.merge(t1, t2);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.LOCALDATE);
		assertEquals(mergedResult.getTypeModifier(), "yyyy-MM-dd");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void issue97_B() throws IOException, FTAException {
		final String[] fmtYYYY_d_MMM = {
			"2023-1(JUL)", "2023-3(AUG)"
		};

		final String[] fmtYYYY_d_SPACE_MMM = {
			"2023-02 (AUG)"
		};

		TextAnalyzer t1 = new TextAnalyzer("updated");
		for (int i = 0; i < fmtYYYY_d_MMM.length; i++)
			t1.train(fmtYYYY_d_MMM[i]);

		TextAnalyzer t2 = new TextAnalyzer("updated");
		for (int i = 0; i < fmtYYYY_d_SPACE_MMM.length; i++)
			t2.train(fmtYYYY_d_SPACE_MMM[i]);

		TextAnalyzer merged = TextAnalyzer.merge(t2, t1);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.LOCALDATE);
		assertEquals(mergedResult.getTypeModifier(), "yyyy-d(MMM)");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void issue97_C() throws IOException, FTAException {
		final String[] s1 = {
				"1.200,00", "1.900,00", "3.475,00", "14.450,00", "1.300,00",
				"7.500,00", "21.802,50", "24.373,50", "48.171,00", "88.979,00"
		};

		final String[] s2 = {
				"1.500,00", "3.000,00", "13.000,00", "470.000,00", "4.920,00",
				"14.000,00", "21.920,00", "29.672,33", "53.240,00", "379.295,00"
		};

		TextAnalyzer t1 = new TextAnalyzer("Subsidie beschikt");
		t1.setLocale(Locale.forLanguageTag("nl-NL"));
		for (final String sample : s1)
			t1.train(sample);

		TextAnalyzer t2 = new TextAnalyzer("Subsidie beschikt");
		t2.setLocale(Locale.forLanguageTag("nl-NL"));
		for (final String sample : s2)
			t2.train(sample);

		TextAnalyzer merged = TextAnalyzer.merge(t2, t1);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.DOUBLE);
		assertEquals(mergedResult.getTypeModifier(), "GROUPING");
//BUG		assertEquals(mergedResult.getSampleCount(), s1.length + s2.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void testMonotonicIncreasing() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;

		final List<String> samplesLong100 = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesLong100.add(String.valueOf(i));
		samplesLong100.add(null);

		final List<String> samplesLong200 = new ArrayList<>();
		for (int i = SAMPLE_COUNT; i < 2 * SAMPLE_COUNT; i++)
			samplesLong200.add(String.valueOf(i));
		samplesLong200.add(null);

		final TextAnalyzer merged = checkTextAnalyzerMerge(samplesLong100, samplesLong200, "monotonicIncreasing", null, true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.LONG);
		assertNull(mergedResult.getSemanticType());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void testMonotonicIncreasingIdentifier() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;

		final List<String> samplesLong100 = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesLong100.add(String.valueOf(i));

		final List<String> samplesLong200 = new ArrayList<>();
		for (int i = SAMPLE_COUNT; i < 2 * SAMPLE_COUNT; i++)
			samplesLong200.add(String.valueOf(i));

		final TextAnalyzer merged = checkTextAnalyzerMerge(samplesLong100, samplesLong200, "monotonicIncreasing", null, true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.LONG);
		assertEquals(mergedResult.getSemanticType(), "IDENTIFIER");
		assertEquals(mergedResult.getKeyConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void testBulkLongNoStats() throws IOException, FTAException {
		final long SAMPLE_COUNT = 100L;

		final List<String> samplesLong100 = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesLong100.add("100");
		samplesLong100.add(null);
		samplesLong100.add(" ");

		final List<String> samplesLong200 = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesLong200.add("200");
		samplesLong200.add(null);
		samplesLong200.add(null);
		samplesLong200.add("  ");
		samplesLong200.add("  ");
		samplesLong200.add("x");

		final TextAnalyzer merged = checkTextAnalyzerMerge(samplesLong100, samplesLong200, "long_long", null, false);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.LONG);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void testEquals() throws IOException, FTAException {
		final TextAnalyzer shardOne = new TextAnalyzer();
		final TextAnalyzer shardTwo = new TextAnalyzer();

		for (int i = 0; i < 100; i++) {
			shardOne.train(String.valueOf(i));
			shardTwo.train(String.valueOf(i));
		}
		shardOne.train("100");
		shardTwo.train("0100");

		assertFalse(shardOne.equals(shardTwo));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void testBulkLongFrench() throws IOException, FTAException {
		final long SAMPLE_COUNT = 100L;

		final List<String> samplesLong100 = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesLong100.add("100");
		samplesLong100.add(null);
		samplesLong100.add(" ");

		final List<String> samplesLong200 = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesLong200.add("200");
		samplesLong200.add(null);
		samplesLong200.add(null);
		samplesLong200.add("  ");
		samplesLong200.add("  ");
		samplesLong200.add("x");

		final TextAnalyzer merged = checkTextAnalyzerMerge(samplesLong100, samplesLong200, "long_long", Locale.FRANCE, true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.LONG);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void cardinalityExceededLong() throws IOException, FTAException {
		final NumberFormat longFormatter = NumberFormat.getIntegerInstance(Locale.ENGLISH);
		longFormatter.setGroupingUsed(true);

		final List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardOne.add(longFormatter.format(i));

		final List<String> shardTwo = new ArrayList<>();
		for (int i = 40000; i < 100000; i++)
			shardTwo.add(longFormatter.format(i));

		final TextAnalyzer merged = checkTextAnalyzerMerge(shardOne, shardTwo, "cardinalityExceededLong", Locale.ENGLISH, true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.LONG);
		assertEquals(mergedResult.getTypeModifier(), "GROUPING");
		assertEquals(mergedResult.getMaxValue(), "99,999");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void cardinalityExceededLongNoSerialization() throws IOException, FTAException {
		final NumberFormat longFormatter = NumberFormat.getIntegerInstance(Locale.ENGLISH);
		longFormatter.setGroupingUsed(true);

		final TextAnalyzer shardOne = new TextAnalyzer("cardinalityExceededLongNoSerialization");
		shardOne.setLocale(Locale.forLanguageTag("en-US"));
		for (int i = 0; i < 20000; i++)
			shardOne.train(longFormatter.format(i));

		final TextAnalyzer shardTwo = new TextAnalyzer("cardinalityExceededLongNoSerialization");
		shardTwo.setLocale(Locale.forLanguageTag("en-US"));
		for (int i = 40000; i < 100000; i++)
			shardTwo.train(longFormatter.format(i));

		final TextAnalyzer merged = TextAnalyzer.merge(shardOne, shardTwo);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.LONG);
		assertEquals(mergedResult.getTypeModifier(), "GROUPING");
		assertEquals(mergedResult.getMaxValue(), "99,999");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void cardinalityExceededLongNoStats() throws IOException, FTAException {

		final List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardOne.add(String.valueOf(i));

		final List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardTwo.add(String.valueOf(100000 + i));

		checkTextAnalyzerMerge(shardOne, shardTwo, "cardinalityExceededLong", null, false);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void mergeWithInvalid() throws IOException, FTAException {
		final TextAnalyzer shardOne = new TextAnalyzer("shardOne");
		for (int i = 0; i < 1000; i++)
			shardOne.train(String.valueOf(i));
		shardOne.train("x");
		final TextAnalysisResult shardOneResult = shardOne.getResult();

		final TextAnalyzer shardTwo = new TextAnalyzer("shardTwo");
		for (int i = 0; i < 1000; i++)
			shardTwo.train(String.valueOf(100000 + i));
		shardTwo.train("y");
		final TextAnalysisResult shardTwoResult = shardTwo.getResult();

		final TextAnalyzer merged = TextAnalyzer.merge(shardOne, shardTwo);
		final TextAnalysisResult mergedResult = merged.getResult();
		assertEquals(mergedResult.getCardinality(), shardOneResult.getCardinality() + shardTwoResult.getCardinality());
		assertEquals(mergedResult.getInvalidCount(), shardOneResult.getInvalidCount() + shardTwoResult.getInvalidCount());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void testUniquenessTrue() throws IOException, FTAException {
		final TextAnalyzer shardOne = new TextAnalyzer("shardOne");
		for (int i = 0; i < 20000; i++)
			shardOne.train(String.valueOf(i));
		final TextAnalysisResult shardOneResult = shardOne.getResult();

		final TextAnalyzer shardTwo = new TextAnalyzer("shardTwo");
		for (int i = 0; i < 20000; i++)
			shardTwo.train(String.valueOf(100000 + i));
		final TextAnalysisResult shardTwoResult = shardTwo.getResult();

		final TextAnalyzer merged = TextAnalyzer.merge(shardOne, shardTwo);
		final TextAnalysisResult mergedResult = merged.getResult();
		assertEquals(mergedResult.getInvalidCount(), shardOneResult.getInvalidCount() + shardTwoResult.getInvalidCount());
		assertEquals(mergedResult.getMinValue(), shardOneResult.getMinValue());
		assertEquals(mergedResult.getMaxValue(), shardTwoResult.getMaxValue());
		assertEquals(mergedResult.getUniqueness(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void testUniquenessFalse() throws IOException, FTAException {
		final TextAnalyzer shardOne = new TextAnalyzer("shardOne");
		for (int i = 0; i < 20000; i++)
			shardOne.train(String.valueOf(i));
		final TextAnalysisResult shardOneResult = shardOne.getResult();

		final TextAnalyzer shardTwo = new TextAnalyzer("shardTwo");
		for (int i = 0; i < 20000; i++)
			shardTwo.train(String.valueOf(1000 + i));
		final TextAnalysisResult shardTwoResult = shardTwo.getResult();

		final TextAnalyzer merged = TextAnalyzer.merge(shardOne, shardTwo);
		final TextAnalysisResult mergedResult = merged.getResult();
		assertEquals(mergedResult.getInvalidCount(), shardOneResult.getInvalidCount() + shardTwoResult.getInvalidCount());
		assertEquals(mergedResult.getMinValue(), shardOneResult.getMinValue());
		assertEquals(mergedResult.getMaxValue(), shardTwoResult.getMaxValue());
		assertEquals(mergedResult.getUniqueness(), -1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void cardinalityExceededLongFrench() throws IOException, FTAException {

		final List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardOne.add(String.valueOf(i));

		final List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardTwo.add(String.valueOf(100000 + i));

		checkTextAnalyzerMerge(shardOne, shardTwo, "cardinalityExceededLong", Locale.FRANCE, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void cardinalityExceededDouble() throws IOException, FTAException {

		final List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardOne.add(String.valueOf(i) + ".0");

		final List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardTwo.add(String.valueOf(100000 + i) + ".0");

		checkTextAnalyzerMerge(shardOne, shardTwo, "cardinalityExceededDouble", null, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void cardinalityExceededDoubleNoStats() throws IOException, FTAException {

		final List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardOne.add(String.valueOf(i) + ".0");

		final List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardTwo.add(String.valueOf(100000 + i) + ".0");

		checkTextAnalyzerMerge(shardOne, shardTwo, "cardinalityExceededDouble", null, false);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void cardinalityExceededDoubleFrench0() throws IOException, FTAException {

		final List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardOne.add(String.valueOf(i) + ",0");

		final List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardTwo.add(String.valueOf(100000 + i) + ",0");

		final TextAnalyzer merged = checkTextAnalyzerMerge(shardOne, shardTwo, "cardinalityExceededDoubleFrench", Locale.FRANCE, true);
		final TextAnalysisResult mergedResult = merged.getResult();
		assertNull(mergedResult.getTypeModifier());
	}

	// Test is broken due to the fact that the bottomk/topK values are stored in a reasonable localized format (reasonable means
	// that it mirrors the format w.r.t. to the presence of the exponent and the presence of the thousands separator) but NOT
	// the number of decimal places.
	// The cardinality set is the input as received, hence if you need to add topK/bottomK as the cardinality has been blown
	// then it has the potential to change the shapes detected.
	// Solution is probably to keep the input as received for the values associated with the topK/bottomK.
	// @Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void cardinalityExceededDoubleFrench00() throws IOException, FTAException {

		final List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardOne.add(String.valueOf(i) + ",00");

		final List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardTwo.add(String.valueOf(100000 + i) + ",00");

		final TextAnalyzer merged = checkTextAnalyzerMerge(shardOne, shardTwo, "cardinalityExceededDoubleFrench", Locale.FRANCE, true);
		final TextAnalysisResult mergedResult = merged.getResult();
		System.err.println(mergedResult.asJSON(true, 0));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void doubleFrench() throws IOException, FTAException {

		final List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20; i++)
			shardOne.add(String.valueOf(i) + ",0");

		final List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < 20; i++)
			shardTwo.add(String.valueOf(100000 + i) + ",0");

		checkTextAnalyzerMerge(shardOne, shardTwo, "doubleFrench", Locale.FRANCE, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void cardinalityNotExceededString() throws IOException, FTAException {
		final List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardOne.add(shortStrings[random.nextInt(shortStrings.length)]);

		final List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardTwo.add(longStrings[random.nextInt(longStrings.length)]);

		final TextAnalyzer merged = checkTextAnalyzerMerge(shardOne, shardTwo, "cardinalityNotExceededString", null, true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.STRING);

	}

	private static String randomString(final int length) {
		final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		final StringBuilder b = new StringBuilder(length);
		for (int i = 0; i < length; i++)
			b.append(alphabet.charAt(random.nextInt(alphabet.length())));

		return b.toString();
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void cardinalityExceededString() throws IOException, FTAException {
		final List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardOne.add(randomString(5));

		final List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardTwo.add(randomString(9));

		final TextAnalyzer merged = checkTextAnalyzerMerge(shardOne, shardTwo, "cardinalityExceededString", null, true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.STRING);
		assertFalse(mergedResult.getLeadingWhiteSpace());
		assertFalse(mergedResult.getTrailingWhiteSpace());
		assertFalse(mergedResult.getMultiline());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void cardinalityExceededStringTrailingSpace() throws IOException, FTAException {
		final List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardOne.add(randomString(5));
		shardOne.add("SPACE     ");

		final List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardTwo.add(randomString(9));

		final TextAnalyzer merged = checkTextAnalyzerMerge(shardOne, shardTwo, "cardinalityExceededStringTrailingSpace", null, true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.STRING);
		assertFalse(mergedResult.getLeadingWhiteSpace());
		assertTrue(mergedResult.getTrailingWhiteSpace());
		assertFalse(mergedResult.getMultiline());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void cardinalityExceededStringLeadingSpace() throws IOException, FTAException {
		final List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardOne.add(randomString(5));
		shardOne.add("    SPACE");

		final List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardTwo.add(randomString(9));

		final TextAnalyzer merged = checkTextAnalyzerMerge(shardOne, shardTwo, "cardinalityExceededStringLeadingSpace", null, true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.STRING);
		assertTrue(mergedResult.getLeadingWhiteSpace());
		assertFalse(mergedResult.getTrailingWhiteSpace());
		assertFalse(mergedResult.getMultiline());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void cardinalityExceededStringMultiline() throws IOException, FTAException {
		final List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardOne.add(randomString(5));
		shardOne.add("SPACE\n\n");

		final List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardTwo.add(randomString(9));

		final TextAnalyzer merged = checkTextAnalyzerMerge(shardOne, shardTwo, "cardinalityExceededStringMultiline", null, true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.STRING);
		assertFalse(mergedResult.getLeadingWhiteSpace());
		assertFalse(mergedResult.getTrailingWhiteSpace());
		assertTrue(mergedResult.getMultiline());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void checkSerialization() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;

		final TextAnalyzer shard = new TextAnalyzer("checkSerialization");
		for (int i = 0; i < SAMPLE_COUNT; i++)
			shard.train(String.valueOf(i));

		// Test pre getResult()
		String serialized = shard.serialize();
		final TextAnalyzer hydrated = TextAnalyzer.deserialize(serialized);
		assertEquals(serialized, hydrated.serialize());

		// Test a hydrated object
		serialized = hydrated.serialize();
		final TextAnalyzer rehydrated = TextAnalyzer.deserialize(serialized);
		assertEquals(serialized, rehydrated.serialize());

		final TextAnalysisResult result = rehydrated.getResult();
		assertEquals(result.getType(), FTAType.LONG);

		// Test post getResult()
		serialized = rehydrated.serialize();
		assertEquals(serialized, TextAnalyzer.deserialize(serialized).serialize());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void smallSamples() throws IOException, FTAException {
		final TextAnalyzer shard = new TextAnalyzer("smallSamples");
		for (int i = 0; i < 10; i++)
			shard.train(String.valueOf(i));

		// Test pre getResult()
		String serialized = shard.serialize();
		final TextAnalyzer hydrated = TextAnalyzer.deserialize(serialized);
		assertEquals(serialized, hydrated.serialize());
		TextAnalysisResult hydratedResult = hydrated.getResult();

		// Test a hydrated object
		serialized = hydrated.serialize();
		final TextAnalyzer rehydrated = TextAnalyzer.deserialize(serialized);
		assertEquals(serialized, rehydrated.serialize());

		final TextAnalysisResult result = rehydrated.getResult();
		assertEquals(result.getType(), FTAType.LONG);

		// Test post getResult()
		serialized = rehydrated.serialize();
		assertEquals(serialized, TextAnalyzer.deserialize(serialized).serialize());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void simpleLongSerialize() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;

		final List<String> samplesLong100 = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesLong100.add("100");
		samplesLong100.add(null);
		samplesLong100.add(" ");

		final List<String> samplesLong200 = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesLong200.add("200");
		samplesLong200.add(null);
		samplesLong200.add(null);
		samplesLong200.add("  ");
		samplesLong200.add("  ");
		samplesLong200.add("x");

		checkTextAnalyzerMerge(samplesLong100, samplesLong200, "long_long", null, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void simpleDoubleMerge() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;

		final List<String> samplesDouble100 = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesDouble100.add("100.0");
		samplesDouble100.add(null);
		samplesDouble100.add(" ");

		final List<String> samplesDouble200 = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesDouble200.add("100.0");
		samplesDouble200.add(null);
		samplesDouble200.add(null);
		samplesDouble200.add(" ");
		samplesDouble200.add(" ");

		checkTextAnalyzerMerge(samplesDouble100, samplesDouble200, "double_double", null, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void simpleStringMerge() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;

		final List<String> samplesOne = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesOne.add("a");
		samplesOne.add("z");
		samplesOne.add(" ");
		samplesOne.add(null);

		final List<String> samplesTwo = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesTwo.add("bb");
		samplesTwo.add(null);
		samplesTwo.add(null);

		checkTextAnalyzerMerge(samplesOne, samplesTwo, "string_string", null, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void simpleLocalDateMerge() throws IOException, FTAException {
		final String[] inputsOne = "22 Jan 1971|12 Mar 2019|02 Jun 1996|11 Dec 1916|19 Apr 1993|26 Sep 1998|09 Dec 1959|14 Jul 2000|18 Aug 2008".split("\\|");
		final String[] inputsTwo = "12 Dec 1970|19 Jan 2010|09 Jul 1999|22 Dec 1976|15 May 1973|23 Sep 1998|09 Dec 1959|14 Jul 2004|17 Nov 1998".split("\\|");

		final List<String> samplesOne = new ArrayList<>();
		for (final String sample : inputsOne)
			samplesOne.add(sample);
		samplesOne.add(" ");
		samplesOne.add(null);

		final List<String> samplesTwo = new ArrayList<>();
		for (final String sample : inputsTwo)
			samplesTwo.add(sample);
		samplesTwo.add(null);
		samplesTwo.add(null);

		final TextAnalyzer merged = checkTextAnalyzerMerge(samplesOne, samplesTwo, "localdate_localdate", Locale.US, true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getSampleCount(), inputsOne.length + inputsTwo.length + 4);
		assertEquals(mergedResult.getMatchCount(), inputsOne.length + inputsTwo.length);
		assertEquals(mergedResult.getNullCount(), 3);
		assertEquals(mergedResult.getBlankCount(), 1);
		assertEquals(mergedResult.getRegExp(), "\\d{2} " + KnownTypes.PATTERN_ALPHA + "{3} \\d{4}");
		assertEquals(mergedResult.getConfidence(), 1.0);
		assertEquals(mergedResult.getType(), FTAType.LOCALDATE);
		assertEquals(mergedResult.getTypeModifier(), "dd MMM yyyy");
		assertEquals(mergedResult.getMinValue(), "11 Dec 1916");
		assertEquals(mergedResult.getMaxValue(), "12 Mar 2019");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void simpleLocalDateMergeNoStats() throws IOException, FTAException {
		final String[] inputsOne = "22 Jan 1971|12 Mar 2019|02 Jun 1996|11 Dec 1916|19 Apr 1993|26 Sep 1998|09 Dec 1959|14 Jul 2000|18 Aug 2008".split("\\|");
		final String[] inputsTwo = "12 Dec 1970|19 Jan 2010|09 Jul 1999|22 Dec 1976|15 May 1973|23 Sep 1998|09 Dec 1959|14 Jul 2004|17 Nov 1998".split("\\|");

		final List<String> samplesOne = new ArrayList<>();
		for (final String sample : inputsOne)
			samplesOne.add(sample);
		samplesOne.add(" ");
		samplesOne.add(null);

		final List<String> samplesTwo = new ArrayList<>();
		for (final String sample : inputsTwo)
			samplesTwo.add(sample);
		samplesTwo.add(null);
		samplesTwo.add(null);

		final TextAnalyzer merged = checkTextAnalyzerMerge(samplesOne, samplesTwo, "localdate_localdate", Locale.US, true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getSampleCount(), inputsOne.length + inputsTwo.length + 4);
		assertEquals(mergedResult.getMatchCount(), inputsOne.length + inputsTwo.length);
		assertEquals(mergedResult.getNullCount(), 3);
		assertEquals(mergedResult.getBlankCount(), 1);
		assertEquals(mergedResult.getRegExp(), "\\d{2} " + KnownTypes.PATTERN_ALPHA + "{3} \\d{4}");
		assertEquals(mergedResult.getConfidence(), 1.0);
		assertEquals(mergedResult.getType(), FTAType.LOCALDATE);
		assertEquals(mergedResult.getTypeModifier(), "dd MMM yyyy");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void simpleLocalDateTimeMerge() throws IOException, FTAException {
		final List<String> inputsOne = Arrays.asList(
			"2004-01-01T00:00:00", "2004-01-01T02:00:00", "2006-01-01T00:00:00", "2004-01-01T02:00:00",
			"2006-01-01T13:00:00", "2004-01-01T00:00:00", "2006-01-01T13:00:00", "2006-01-01T00:00:00",
			"2004-01-01T00:00:00", "2004-01-01T00:00:00", "2004-01-01T00:00:00", "2004-01-01T00:00:00",
			"2004-01-01T00:00:00", "2008-01-01T13:00:00", "2008-01-01T13:00:00", "2010-01-01T00:00:00",
			"2004-01-01T02:00:00", "2008-01-01T00:00:00"
		);
		final List<String> inputsTwo = Arrays.asList(
				"1994-01-01T00:00:00", "1994-01-01T02:00:00", "1996-01-01T00:00:00", "1994-01-01T02:00:00",
				"1996-01-01T13:00:00", "1994-01-01T00:00:00", "1996-01-01T13:00:00", "1996-01-01T00:00:00",
				"1994-01-01T00:00:00", "1994-01-01T00:00:00", "1994-01-01T00:00:00", "1994-01-01T00:00:00",
				"1994-01-01T00:00:00", "1998-01-01T13:00:00", "1998-01-01T13:00:00", "1990-01-01T00:00:00",
				"1994-01-01T02:00:00", "1998-01-01T00:00:00"
		);

		final List<String> samplesOne = new ArrayList<>();
		samplesOne.addAll(inputsOne);
		samplesOne.add(" ");
		samplesOne.add(null);

		final List<String> samplesTwo = new ArrayList<>();
		samplesTwo.addAll(inputsTwo);
		samplesTwo.add(null);
		samplesTwo.add(null);

		final TextAnalyzer merged = checkTextAnalyzerMerge(samplesOne, samplesTwo, "localdatetime_localdatetime", null, true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getSampleCount(), inputsOne.size() + inputsTwo.size() + 4);
		assertEquals(mergedResult.getNullCount(), 3);
		assertEquals(mergedResult.getBlankCount(), 1);
		assertEquals(mergedResult.getType(), FTAType.LOCALDATETIME);
		assertEquals(mergedResult.getRegExp(), "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
		assertEquals(mergedResult.getConfidence(), 1.0);
		assertEquals(mergedResult.getTypeModifier(), "yyyy-MM-dd'T'HH:mm:ss");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void simpleOffsetDateTimeMerge() throws IOException, FTAException {
		final List<String> inputsOne = Arrays.asList(
				"2010-07-01T22:20:22.400Z", "2015-03-01T22:20:21.000Z", "2015-07-01T22:20:22.300Z", "2015-07-01T12:20:32.000Z",
				"2015-07-01T22:20:22.100Z", "2011-02-01T02:20:22.000Z", "2015-07-01T22:30:22.000Z", "2015-07-01T22:20:22.010Z",
				"2012-01-01T22:20:22.000Z", "2015-07-01T22:40:22.000Z", "2015-07-01T22:20:22.000Z", "2015-07-01T22:20:22.200Z",
				"2015-07-01T22:20:22.000Z", "2014-08-01T12:10:22.000Z", "2015-06-01T22:20:22.010Z", "2015-07-01T22:20:22.000Z",
				"2017-07-01T02:20:22.000Z", "2018-06-01T08:20:22.000Z"
		);
		final List<String> inputsTwo = Arrays.asList(
				"1994-01-01T00:00:00.400Z", "1994-01-01T02:00:00.400Z", "1996-01-01T00:00:00.400Z", "1994-01-01T02:00:00.400Z",
				"1996-01-01T13:00:00.400Z", "1994-01-01T00:00:00.400Z", "1996-01-01T13:00:00.400Z", "1996-01-01T00:00:00.400Z",
				"1994-01-01T00:00:00.400Z", "1994-01-01T00:00:00.400Z", "1994-01-01T00:00:00.400Z", "1994-01-01T00:00:00.400Z",
				"1994-01-01T00:00:00.400Z", "1998-01-01T13:00:00.400Z", "1998-01-01T13:00:00.400Z", "1990-01-01T00:00:00.400Z",
				"1994-01-01T02:00:00.400Z", "1998-01-01T00:00:00.400Z"
		);

		final List<String> samplesOne = new ArrayList<>();
		samplesOne.addAll(inputsOne);
		samplesOne.add(" ");
		samplesOne.add(null);

		final List<String> samplesTwo = new ArrayList<>();
		samplesTwo.addAll(inputsTwo);
		samplesTwo.add(null);
		samplesTwo.add(null);

		final TextAnalyzer merged = checkTextAnalyzerMerge(samplesOne, samplesTwo, "offsetdatetime_offsetdatetime", null, true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.OFFSETDATETIME);
		assertEquals(mergedResult.getTypeModifier(), "yyyy-MM-dd'T'HH:mm:ss.SSSX");
		assertEquals(mergedResult.getSampleCount(), inputsOne.size() + inputsTwo.size() + 4);
		assertEquals(mergedResult.getNullCount(), 3);
		assertEquals(mergedResult.getBlankCount(), 1);
		assertEquals(mergedResult.getRegExp(), "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}([-+][0-9]{2}([0-9]{2})?|Z)");
		assertEquals(mergedResult.getMatchCount(), inputsOne.size() + inputsTwo.size());
		assertEquals(mergedResult.getConfidence(), 1.0);
	}

	/*
	 * The challenge with this test is that "2:01:30.00" is captured in the bottomK as "2:01:30.0" (i.e. normalized
	 * with one trailing 0) but it is in the cardinality set with the input as received, hence if we are not careful
	 * it will be double counted!
	 */
	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void simpleLocalTimeMerge() throws IOException, FTAException {
		final List<String>  inputsOne = Arrays.asList(
				"1:01:50.00", "2:01:16.00", "2:01:30.00", "2:01:55.00", "5:01:49.00",
				"9:01:51.00", "11:01:20.0", "11:01:47.0", "12:01:16.0", "12:01:55.0",
				"14:01:21.0", "14:01:25.0", "14:01:43.0", "15:01:03.0", "15:01:39.0",
				"15:01:48.0", "15:01:51.0", "19:01:47.0", "20:01:34.0", "21:01:03.0",
				"21:01:27.0", "22:01:15.0", "22:01:32.0", "11:01:58.0", "13:01:31.0",
				"16:01:24.0", "16:01:58.0", "17:01:05.0", "11:01:38.0", "11:01:44.0",
				"13:01:41.0", "14:01:14.0", "14:01:59.0", "14:01:59.0", "14:01:59.0",
				"15:01:04.0", "15:01:11.0", "15:01:54.0"
		);

		final List<String> samplesOne = new ArrayList<>();
		samplesOne.addAll(inputsOne);

		final List<String> samplesTwo = new ArrayList<>();
		samplesTwo.add(null);
		samplesTwo.add(null);

		final TextAnalyzer merged = checkTextAnalyzerMerge(samplesOne, samplesTwo, "localtime_localtime", null, true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getSampleCount(), inputsOne.size() + 2);
		assertEquals(mergedResult.getMatchCount(), inputsOne.size());
		assertEquals(mergedResult.getNullCount(), 2);
		assertEquals(mergedResult.getRegExp(), "\\d{1,2}:\\d{2}:\\d{2}\\.\\d{1,2}");
		assertEquals(mergedResult.getConfidence(), 1.0);
		assertEquals(mergedResult.getType(), FTAType.LOCALTIME);
		assertEquals(mergedResult.getTypeModifier(), "H:mm:ss.S{1,2}");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void strangeZeroes() throws IOException, FTAException {
		final List<String> samplesOne = new ArrayList<>();

		// Register 00, 000, 0000, ...
		for (int i = 2; i < 10; i++)
			samplesOne.add(Utils.repeat('0', i));

		final List<String> samplesTwo = new ArrayList<>();
		samplesTwo.add(null);
		samplesTwo.add(null);

		// The challenge is that the topK/bottomK has '0' in it which is not in the set of samples registered above
		final TextAnalyzer merged = checkTextAnalyzerMerge(samplesOne, samplesTwo, "strangeZeroes", null, true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getSampleCount(), 10);
		assertEquals(mergedResult.getMatchCount(), 8);
		assertEquals(mergedResult.getNullCount(), 2);
		assertEquals(mergedResult.getRegExp(), "\\d{2,9}");
		assertEquals(mergedResult.getConfidence(), 1.0);
		assertEquals(mergedResult.getType(), FTAType.LONG);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void simpleZonedDateTimeMerge() throws IOException, FTAException {
		final String[] inputsOne = {
				"01/26/2012 10:42:23 GMT", "01/26/2012 10:42:23 GMT", "01/30/2012 10:59:48 GMT", "01/25/2012 16:46:43 GMT",
				"01/25/2012 16:28:42 GMT", "01/24/2012 16:53:04 GMT",
		};
		final String[] inputsTwo = {
				"01/27/2011 10:42:23 GMT", "01/20/2011 10:42:23 GMT", "01/30/2011 10:59:48 GMT", "01/25/2011 16:46:43 GMT",
				"01/26/2011 16:28:42 GMT", "01/24/2011 16:53:04 GMT",
		};

		final List<String> samplesOne = new ArrayList<>();
		for (final String sample : inputsOne)
			samplesOne.add(sample);

		final List<String> samplesTwo = new ArrayList<>();
		for (final String sample : inputsTwo)
			samplesTwo.add(sample);

		final TextAnalyzer merged = checkTextAnalyzerMerge(samplesOne, samplesTwo, "zoneddatetime_zoneddatetime", null, true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.ZONEDDATETIME);
		assertEquals(mergedResult.getSampleCount(), inputsOne.length + inputsTwo.length);
		assertEquals(mergedResult.getMatchCount(), inputsOne.length + inputsTwo.length);
		assertEquals(mergedResult.getNullCount(), 0);
		assertEquals(mergedResult.getRegExp(), "\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2} .*");
		assertEquals(mergedResult.getTypeModifier(), "MM/dd/yyyy HH:mm:ss z");
		assertEquals(mergedResult.getConfidence(), 1.0);
		assertEquals(mergedResult.getMinValue(), "01/20/2011 10:42:23 GMT");
		assertEquals(mergedResult.getMaxValue(), "01/30/2012 10:59:48 GMT");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void simpleZonedDateTimeMergeNoStats() throws IOException, FTAException {
		final String[] inputsOne = {
				"01/26/2012 10:42:23 GMT", "01/26/2012 10:42:23 GMT", "01/30/2012 10:59:48 GMT", "01/25/2012 16:46:43 GMT",
				"01/25/2012 16:28:42 GMT", "01/24/2012 16:53:04 GMT",
		};
		final String[] inputsTwo = {
				"01/27/2011 10:42:23 GMT", "01/20/2011 10:42:23 GMT", "01/30/2011 10:59:48 GMT", "01/25/2011 16:46:43 GMT",
				"01/26/2011 16:28:42 GMT", "01/24/2011 16:53:04 GMT",
		};

		final List<String> samplesOne = new ArrayList<>();
		for (final String sample : inputsOne)
			samplesOne.add(sample);

		final List<String> samplesTwo = new ArrayList<>();
		for (final String sample : inputsTwo)
			samplesTwo.add(sample);

		final TextAnalyzer merged = checkTextAnalyzerMerge(samplesOne, samplesTwo, "zoneddatetime_zoneddatetime", null, false);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.ZONEDDATETIME);
		assertEquals(mergedResult.getSampleCount(), inputsOne.length + inputsTwo.length);
		assertEquals(mergedResult.getMatchCount(), inputsOne.length + inputsTwo.length);
		assertEquals(mergedResult.getNullCount(), 0);
		assertEquals(mergedResult.getRegExp(), "\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2} .*");
		assertEquals(mergedResult.getTypeModifier(), "MM/dd/yyyy HH:mm:ss z");
		assertEquals(mergedResult.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void simpleBooleanMerge() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;

		final List<String> samplesOne = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesOne.add("true");
		samplesOne.add(null);

		final List<String> samplesTwo = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesTwo.add("true");
		samplesTwo.add(null);
		samplesTwo.add(null);

		final TextAnalyzer merged = checkTextAnalyzerMerge(samplesOne, samplesTwo, "boolean_boolean", null, true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.BOOLEAN);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void simpleBooleanYesNoMerge() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;

		final List<String> samplesOne = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesOne.add("yes");
		samplesOne.add(null);

		final List<String> samplesTwo = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesTwo.add("no");
		samplesTwo.add(null);
		samplesTwo.add(null);

		final TextAnalyzer merged = checkTextAnalyzerMerge(samplesOne, samplesTwo, "boolean_boolean", null, true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.BOOLEAN);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void simpleInvalidMerge() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;

		final List<String> samplesOne = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesOne.add(String.valueOf(i));
		samplesOne.add("a");

		final List<String> samplesTwo = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesTwo.add(String.valueOf(i + 50));
		samplesTwo.add("b");

		final TextAnalyzer merged = checkTextAnalyzerMerge(samplesOne, samplesTwo, "long_long", null, true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getInvalidCount(), 2);
		assertEquals(mergedResult.getMaxValue(), "149");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void NullNullStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesNULL, samplesNULL, "NULL_NULL", null, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void NullBlankStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesNULL, samplesBLANK, "NULL_BLANK", null, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void NullBlankOrNullStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesNULL, samplesBLANKORNULL, "NULL_BLANKORNULL", null, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void NullAlphaDataStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesNULL, samplesBLANKORNULL, "NULL_BLANKORNULL", null, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void blankNullStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesBLANK, samplesNULL, "BLANK_NULL", null, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void blankBlankStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesBLANK, samplesBLANK, "BLANK_BLANK", null, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void blankBlankOrNullStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesBLANK, samplesBLANKORNULL, "BLANK_BLANKORNULL", null, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void blankAlphaDataStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesBLANK, samplesAlphaData, "BLANK_ALPHADATA", null, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void blankOrNullNullStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesBLANKORNULL, samplesNULL, "BLANKORNULL_NULL", null, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void blankOrNullBlankStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesBLANKORNULL, samplesBLANK, "BLANKORNULL_BLANK", null, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void blankOrNullBlankOrNullStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesBLANKORNULL, samplesBLANKORNULL, "BLANKORNULL_BLANKORNULL", null, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void blankOrNullAlphaDataStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesBLANKORNULL, samplesAlphaData, "BLANKORNULL_ALPHADATA", null, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void AlphaDataNullStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesAlphaData, samplesNULL, "ALPHADATA_NULL", null, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void AlphaDataBlankStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesAlphaData, samplesBLANK, "ALPHADATA_BLANK", null, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void AlphaDataBlankOrNullStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesAlphaData, samplesBLANKORNULL, "ALPHADATA_BLANKORNULL", null, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void AlphaDataAlphaDataStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesAlphaData, samplesAlphaData, "ALPHADATA_ALPHADATA", null, true);
	}

	public void testHistogramMerge(final long sizeOne, final long sizeTwo) throws IOException, FTAException {
		final TextAnalyzer shardOne = new TextAnalyzer("shardOne");
		final TextAnalyzer shardTwo = new TextAnalyzer("shardTwo");
		final SecureRandom random = new SecureRandom();

		for (int i = 0; i < sizeOne; i++)
//			shardOne.train(String.valueOf(random.nextGaussian()*5 + 20));
			shardOne.train(String.valueOf(i%100));

		final TextAnalysisResult shardOneResult = shardOne.getResult();
		final Histogram.Entry[] shardOneHistogram = shardOneResult.getHistogram(10);
		final long shardOneHistogramCount = TestSupport.countHistogram(shardOneHistogram);
		assertEquals(shardOneHistogramCount, shardOneResult.getMatchCount());

		final String serializedOne = shardOne.serialize();
		final TextAnalyzer hydratedOne = TextAnalyzer.deserialize(serializedOne);

		final TextAnalysisResult hydratedOneResult = hydratedOne.getResult();
		final Histogram.Entry[] histogramOne = hydratedOneResult.getHistogram(10);
		final long histogramOneCount = TestSupport.countHistogram(histogramOne);
		assertEquals(hydratedOneResult.getMatchCount(), sizeOne);
		assertEquals(histogramOneCount, hydratedOneResult.getMatchCount());

		TestSupport.dumpPicture(histogramOne);
		TestSupport.checkQuantiles(hydratedOneResult);
		TestSupport.checkHistogram(hydratedOneResult, 10, true);

		for (int i = 0; i < sizeTwo; i++)
//			shardTwo.train(String.valueOf(random.nextGaussian()*5 + 70));
			shardTwo.train(String.valueOf(i%100 + 200));

		final String serializedTwo = shardTwo.serialize();
		final TextAnalyzer hydratedTwo = TextAnalyzer.deserialize(serializedTwo);
		final TextAnalysisResult hydratedTwoResult = hydratedTwo.getResult();
		final Histogram.Entry[] histogramTwo = hydratedTwoResult.getHistogram(10);
		final long histogramTwoCount = TestSupport.countHistogram(histogramTwo);
		assertEquals(hydratedTwoResult.getMatchCount(), sizeTwo);
		assertEquals(histogramTwoCount, hydratedTwoResult.getMatchCount());

		TestSupport.dumpPicture(histogramTwo);
		TestSupport.checkQuantiles(hydratedTwoResult);
		TestSupport.checkHistogram(hydratedTwoResult, 10, true);

		final TextAnalyzer merged = TextAnalyzer.merge(shardOne, shardTwo);
		final TextAnalysisResult mergedResult = merged.getResult();
		final Histogram.Entry[] histogramResult = mergedResult.getHistogram(20);
		final long mergedCount = TestSupport.countHistogram(histogramResult);
		assertEquals(mergedCount, sizeOne + sizeTwo);

		TestSupport.dumpPicture(mergedResult.getHistogram(20));
		TestSupport.checkQuantiles(mergedResult);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void histogramSmallSmall() throws IOException, FTAException {
		testHistogramMerge(1000, AnalysisConfig.MAX_CARDINALITY_DEFAULT - 500);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void histogramSmallLarge() throws IOException, FTAException {
		testHistogramMerge(1000, AnalysisConfig.MAX_CARDINALITY_DEFAULT + 1000);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void histogramLargeSmall() throws IOException, FTAException {
		testHistogramMerge(AnalysisConfig.MAX_CARDINALITY_DEFAULT + 1000, 1000);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void histogramLargeLarge() throws IOException, FTAException {
		testHistogramMerge(AnalysisConfig.MAX_CARDINALITY_DEFAULT + 1000, AnalysisConfig.MAX_CARDINALITY_DEFAULT + 1000);
	}

	private TextAnalyzer checkTextAnalyzerMerge(final List<String> samplesOne, final List<String> samplesTwo, final String streamName,
			final Locale locale, final boolean collectStatistics) throws FTAException {
		final TextAnalyzer shardOne = new TextAnalyzer(streamName);
		shardOne.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, collectStatistics);

		if (locale != null)
			shardOne.setLocale(locale);
		final TextAnalyzer reference = new TextAnalyzer(streamName);
		if (locale != null)
			reference.setLocale(locale);
		reference.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, collectStatistics);

		long countOne = 0;
		long countTwo = 0;
		long countReference = 0;
		// Train the first shard (and the reference set)
		for (final String sample : samplesOne) {
			shardOne.train(sample);
			countOne++;
			reference.train(sample);
			countReference++;
		}
		shardOne.setTotalCount(countOne);
		// serialize and de-serialize it to check this process
		final TextAnalyzer hydratedOne = TextAnalyzer.deserialize(shardOne.serialize());

		// Train the second shard (and the reference set)
		final TextAnalyzer shardTwo = new TextAnalyzer(streamName);
		if (locale != null)
			shardTwo.setLocale(locale);
		shardTwo.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, collectStatistics);

		for (final String sample : samplesTwo) {
			shardTwo.train(sample);
			countTwo++;
			reference.train(sample);
			countReference++;
		}
		shardTwo.setTotalCount(countTwo);
		reference.setTotalCount(countReference);
		final TextAnalyzer hydratedTwo = TextAnalyzer.deserialize(shardTwo.serialize());
		assertEquals(hydratedTwo.getContext().getStreamName(), streamName);

		// Merge the two hydrated TextAnalyzers
		final TextAnalyzer merged = TextAnalyzer.merge(hydratedOne, hydratedTwo);

		final TextAnalysisResult mergedResult = merged.getResult();
		final String mergedJSON = mergedResult.asJSON(false, 1);
		final TextAnalysisResult referenceResult = reference.getResult();
		final String referenceJSON = referenceResult.asJSON(false, 1);

		boolean failed = false;
		// If we captured all the observation then the merge should be roughly perfect :-)
		if (referenceResult.getCardinality() < reference.getMaxCardinality()) {
			if (mergedResult.getType().isNumeric()) {
				if (!merged.equals(reference, TestUtils.EPSILON))
					failed = true;
			}
			else {
				 if (!merged.equals(reference) || !mergedJSON.equals(referenceJSON))
					 failed = true;
			}
		}
		else {
			// We lost some results in the merge so compare what we can
			if (
					// Type and TypeQualifier (if it exists)
					!mergedResult.getType().equals(referenceResult.getType()) ||
					mergedResult.isSemanticType() != referenceResult.isSemanticType() ||
					(mergedResult.getTypeModifier() != null && !mergedResult.getTypeModifier().equals(referenceResult.getTypeModifier())) ||
					// White-space
					mergedResult.getLeadingWhiteSpace() != referenceResult.getLeadingWhiteSpace() ||
					mergedResult.getTrailingWhiteSpace() != referenceResult.getTrailingWhiteSpace() ||
					mergedResult.getMultiline() != referenceResult.getMultiline() ||
					// Counts - totalCount, nullCount, blankCount
					mergedResult.getTotalCount() != referenceResult.getTotalCount() ||
					mergedResult.getNullCount() != referenceResult.getNullCount() ||
					mergedResult.getBlankCount() != referenceResult.getBlankCount() ||
					// Structure
					!mergedResult.getStructureSignature().equals(referenceResult.getStructureSignature()) ||
					!mergedResult.getRegExp().equals(referenceResult.getRegExp())
			)
				failed = true;
			if (merged.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS) && (
					// Maximum/Minimum
					!mergedResult.getMaxValue().equals(referenceResult.getMaxValue()) ||
					!mergedResult.getMinValue().equals(referenceResult.getMinValue()) ||
					// TopK/BottomK
					!Objects.equals(mergedResult.getBottomK(), referenceResult.getBottomK()) ||
					!Objects.equals(mergedResult.getTopK(), referenceResult.getTopK())
					))
				failed = true;
			if (merged.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS) && mergedResult.getType().isNumeric()) {
				if (
						Math.abs(mergedResult.getMean() - referenceResult.getMean()) > TestUtils.EPSILON
//						||
//						Math.abs(mergedResult.getStandardDeviation() - referenceResult.getStandardDeviation()) > EPSILON
						)
					failed = true;
			}
		}

		if (failed) {
			System.err.println("Merged:\n" + mergedJSON);
			System.err.println("Reference:\n" + referenceJSON);
			fail();
		}

		return merged;
	}
}
