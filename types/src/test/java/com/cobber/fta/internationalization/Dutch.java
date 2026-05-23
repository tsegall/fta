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
package com.cobber.fta.internationalization;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Locale;

import org.testng.annotations.Test;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.LogicalType;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.Sample;
import com.cobber.fta.TestGroups;
import com.cobber.fta.TestSupport;
import com.cobber.fta.TestUtils;
import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;

public class Dutch {

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void dutchLocalized() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("X_COORD");
		// For Dutch, Decimal Sep = ',' and Thousands Sep = '.'
		final Locale locale = Locale.forLanguageTag("nl-NL");
		analysis.setLocale(locale);
		final String nonLocalized = "120114.963735722";

		final String[] inputs = {
				"121725,84489347", "121258,298875078", "120898,328669664", "120718,710287902", "122615,928882987",
				"120793,655243809", "121115,972636036", "121718,898590647", "120724,550131904", "120886,176838707",
				"120513,472283841", "122103,160984525", "121695,764769605", "121748,427187895", "121701,209663187",
				"122711,434683845", "123071,319201962", "120933,985450736", "120628,627456509", "120717,547663685",
				"121553,945999326", "122309,278902156", "121421,043304069", "121310,146841934", "121075,795301121",
				"121027,7347339", "122158,600785584", "121671,473438156", "121142,977894114", "121107,856228947",
				"120256,857625212", "122556,814330651", "121866,52727883", nonLocalized, "120479,513662353"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length - 1);
		assertEquals(result.getNullCount(), 0);
		assertNull(result.getTypeModifier());
		assertEquals(result.getRegExp(), "\\d*,?\\d+");
		assertEquals(result.getMinValue(), "120256,857625212");
		assertEquals(result.getMaxValue(), "123071,319201962");
		assertEquals(result.getDecimalSeparator(), ',');
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			if (!nonLocalized.equals(input))
				assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void dutchNonLocalizedLong() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("L");
		// For Dutch, Decimal Sep = ',' and Thousands Sep = '.'
		final Locale locale = Locale.forLanguageTag("nl-NL");
		analysis.setLocale(locale);

		final String[] inputs = {
				"1.234", "8.078", "1.664", "12.902", "122.987",
				"120.809", "12.036", "121.647", "120.904", "120.707",
				"105.841", "1.525", "129.605", "1.895", "12.187",
				"12.845", "1.962", "109.736", "120.509", "120.685",
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTypeModifier(), "GROUPING");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "[\\d\\.]{5,7}");
		assertEquals(result.getMinValue(), "1.234");
		assertEquals(result.getMaxValue(), "129.605");
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void dutchNonLocalizedDouble19() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("D");
		// For Dutch, Decimal Sep = ',' and Thousands Sep = '.'
		final Locale locale = Locale.forLanguageTag("nl-NL");
		analysis.setLocale(locale);
		analysis.setDebug(2);

		final String[] inputs = {
				"1.234", "8.078", "1.664", "12.902", "122.987",
				"120.809", "12.036", "121.647", "120.904", "120.707",
				"105.841", "1.525", "129.605", "1.895", "12.187",
				"12.845", "1.962", "109.736", "120.509",
				// This one must be a double - hence we are all in on the Doubles
				"1201.685"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), "NON_LOCALIZED");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d*\\.?\\d+");
		assertEquals(result.getMinValue(), "1.234");
		assertEquals(result.getMaxValue(), "1201.685");
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void dutchNonLocalizedConfused() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("bed_zosmar");
		// For Dutch, Decimal Sep = ',' and Thousands Sep = '.'
		final Locale locale = Locale.forLanguageTag("nl-NL");
		analysis.setLocale(locale);
		analysis.setDebug(2);

		final String[] inputs = {
				"0.75", "3.125", "0.125", "1.875", "1.875",
				"1.875", "0.75", "0.75", "0.125", "0.125",
				"1.875", "0.125", "0.75", "1.875", "0.125",
				"0.125", "0.75", "1.875", "0.75", "3.125",
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), "NON_LOCALIZED");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d*\\.?\\d+");
		assertEquals(result.getMinValue(), "0.125");
		assertEquals(result.getMaxValue(), "3.125");
		assertEquals(result.getDecimalSeparator(), '.');
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void issue81_NL() throws IOException, FTAException {

		final String[] doubleList = {
				"648,152", "396,533", "12,2642", "0,28616046", "1,36448",
				"0,0070301776", "0,03352375", "0,058304594", "0,27803", "0,22545675",
				"0,58353168", "0,02087185", "0,0150214", "0,0040381682", "0,06775475",
				"3,0292296", "0,122049178", "0,7679275", "27,2085962391327", "15,773",
				"1,3704275e-5"
		};

		final TextAnalyzer textAnalyzer = new TextAnalyzer("Nothing");
		textAnalyzer.setLocale(Locale.forLanguageTag("nl-NL"));
		textAnalyzer.setDebug(2);

		for (final String input : doubleList)
			textAnalyzer.train(input);
		final TextAnalysisResult result = textAnalyzer.getResult();

		assertEquals(result.getType().name(), "DOUBLE");
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void dutchLocalizedLong() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("D");
		// For Dutch, Decimal Sep = ',' and Thousands Sep = '.'
		final Locale locale = Locale.forLanguageTag("nl-NL");
		analysis.setLocale(locale);
		analysis.setDebug(2);

		final String[] inputs = {
				"1.234", "8.078", "1.664", "12.902", "122.987",
				"120.809", "12.036", "121.647", "120.904", "120.707",
				"105.841", "1.525", "129.605", "1.895", "12.187",
				"12.845", "1.962", "109.736", "120.509",
				"1.201.685",
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTypeModifier(), "GROUPING");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "[\\d\\.]{5,9}");
		assertEquals(result.getMinValue(), "1.234");
		assertEquals(result.getMaxValue(), "1.201.685");
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	// This is questionable ... probably should be a non-localized Long with Grouping
	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void dutchNonLocalizedLongWithGrouping() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("D");
		// For Dutch, Decimal Sep = ',' and Thousands Sep = '.'
		final Locale locale = Locale.forLanguageTag("nl-NL");
		analysis.setLocale(locale);
		analysis.setDebug(2);

		final String[] inputs = {
				"1,234,900", // Bad
				"8,078", "1,664", "12,902", "122,987",
				"120,809", "12,036", "121,647", "120,904", "120,707",
				"105,841", "1,525", "129,605", "1,895", "12,187",
				"12,845", "1,962", "109,736", "120,509",
				"1,201,685", // Bad
				"254,035", "2,500", "504,117", "162,689", "1,881", "542,742",
				"200", "91,045", "193,132", "505,916", "472,138", "277,097",
				"21,954", "38,607", "534,231", "1,356", "12,995",
				"45,670", "12,340", "14,098", "12,479", "1,200",

		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length - 2);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d*,?\\d+");
//BUG		assertEquals(result.getMinValue(), "1,525");
		assertEquals(result.getMaxValue(), "542,742");
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
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

		final TextAnalyzer t1 = new TextAnalyzer("Subsidie beschikt");
		t1.setLocale(Locale.forLanguageTag("nl-NL"));
		for (final String sample : s1)
			t1.train(sample);

		final TextAnalyzer t2 = new TextAnalyzer("Subsidie beschikt");
		t2.setLocale(Locale.forLanguageTag("nl-NL"));
		for (final String sample : s2)
			t2.train(sample);

		final TextAnalyzer merged = TextAnalyzer.merge(t2, t1);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.DOUBLE);
		assertEquals(mergedResult.getTypeModifier(), "GROUPING");
//BUG		assertEquals(mergedResult.getSampleCount(), s1.length + s2.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void doubleSpace() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("datum_melding");
		analysis.setLocale(Locale.forLanguageTag("nl-NL"));
		analysis.setDebug(2);

		final String inputs[] = {
				"2023-02-03  09:56:22", "2023-02-02  11:49:12", "2023-02-01  12:59:19", "2023-01-06  11:30:31", "2022-11-14  15:50:01",
				"2022-11-07  07:41:47", "2022-11-04  17:23:14", "2022-10-04  17:40:40", "2022-09-30  16:30:11", "2022-09-27  17:18:15",
				"2022-09-26  07:16:06", "2022-09-14  08:38:55", "2022-09-06  12:27:57", "2022-09-01  13:01:19", "2022-08-30  10:57:05",
				"2022-08-25  16:31:48", "2022-08-19  09:13:11", "2022-08-17  10:59:31", "2022-08-12  13:07:12", "2022-08-11  15:12:41",
				"2022-08-09  06:22:05", "2022-07-25  11:49:33", "2022-07-21  20:08:37", "2022-07-18  11:21:15", "2022-07-13  11:30:48",
				"2022-07-12  08:34:15", "2022-07-09  11:43:37", "2022-07-01  16:13:43", "2022-06-22  10:14:31", "2022-06-16  13:43:24",
				"2022-06-16  10:27:56", "2022-06-01  20:25:06", "2022-05-23  17:09:01", "2022-05-17  11:56:05", "2022-05-12  21:21:28",
				"2022-05-12  10:08:59", "2022-05-07  20:59:54", "2022-05-01  22:49:13", "2022-04-21  11:58:16", "2022-04-19  08:42:47",
				"2022-04-14  09:29:47", "2022-04-13  20:11:24", "2022-04-12  23:24:14", "2022-04-08  11:24:55", "2022-03-25  16:31:20",
				"2022-03-24  14:15:05", "2022-03-17  14:11:51", "2022-03-16  13:51:00", "2022-03-16  06:30:01", "2022-03-15  13:49:20",
				"2022-03-14  12:42:26", "2022-03-14  08:45:46", "2022-03-11  11:35:30", "2022-02-22  15:02:36", "2022-02-03  09:47:22",
				"2022-01-31  13:21:46", "2022-01-12  15:15:36", "2022-01-10  16:59:04", "2022-01-07  12:30:43", "2022-01-06  16:49:37",
				"2022-01-05  08:29:19", "2021-12-06  10:41:51", "2021-11-22  12:29:15", "2021-11-15  10:36:12", "2021-10-25  09:24:04",
				"2021-10-14  09:31:38", "2021-10-11  09:38:34", "2021-10-04  14:03:55", "2021-09-15  16:08:12", "2021-09-10  17:08:55",
				"2021-09-06  13:58:09", "2021-09-01  15:08:11", "2021-08-31  11:45:28", "2021-08-28  23:19:36", "2021-08-27  07:47:13",
				"2021-08-24  10:54:43", "2021-08-23  13:39:28", "2021-08-18  12:11:02", "2021-08-11  23:25:28", "2021-08-10  15:36:51",
				"2021-08-04  10:39:28", "2021-08-04  06:36:22", "2021-07-27  10:41:47", "2021-07-24  11:50:49", "2021-07-20  14:08:51",
				"2021-07-20  11:16:10", "2021-07-13  16:50:29", "2021-07-06  10:14:14", "2021-07-01  09:48:46", "2021-06-30  07:20:15",
				"2021-06-28  10:09:51", "2021-06-24  22:46:28", "2021-06-23  13:22:00", "2021-06-18  10:32:50", "2021-06-07  18:36:04",
				"2021-05-27  18:29:57", "2021-05-27  12:37:25", "2021-05-25  17:53:40", "2021-05-25  15:39:59",
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "yyyy-MM-dd  HH:mm:ss");
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2}  \\d{2}:\\d{2}:\\d{2}");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			if (!input.isEmpty())
				assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void tripleSpace() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("datum_melding");
		analysis.setLocale(Locale.forLanguageTag("nl-NL"));
		analysis.setDebug(2);

		final String inputs[] = {
				"2023-02-03   09:56:22", "2023-02-02   11:49:12", "2023-02-01   12:59:19", "2023-01-06   11:30:31", "2022-11-14   15:50:01",
				"2022-11-07   07:41:47", "2022-11-04   17:23:14", "2022-10-04   17:40:40", "2022-09-30   16:30:11", "2022-09-27   17:18:15",
				"2022-09-26   07:16:06", "2022-09-14   08:38:55", "2022-09-06   12:27:57", "2022-09-01   13:01:19", "2022-08-30   10:57:05",
				"2022-08-25   16:31:48", "2022-08-19   09:13:11", "2022-08-17   10:59:31", "2022-08-12   13:07:12", "2022-08-11   15:12:41",
				"2022-08-09   06:22:05", "2022-07-25   11:49:33", "2022-07-21   20:08:37", "2022-07-18   11:21:15", "2022-07-13   11:30:48",
				"2022-07-12   08:34:15", "2022-07-09   11:43:37", "2022-07-01   16:13:43", "2022-06-22   10:14:31", "2022-06-16   13:43:24",
				"2022-06-16   10:27:56", "2022-06-01   20:25:06", "2022-05-23   17:09:01", "2022-05-17   11:56:05", "2022-05-12   21:21:28",
				"2022-05-12   10:08:59", "2022-05-07   20:59:54", "2022-05-01   22:49:13", "2022-04-21   11:58:16", "2022-04-19   08:42:47",
				"2022-04-14   09:29:47", "2022-04-13   20:11:24", "2022-04-12   23:24:14", "2022-04-08   11:24:55", "2022-03-25   16:31:20",
				"2022-03-24   14:15:05", "2022-03-17   14:11:51", "2022-03-16   13:51:00", "2022-03-16   06:30:01", "2022-03-15   13:49:20",
				"2022-03-14   12:42:26", "2022-03-14   08:45:46", "2022-03-11   11:35:30", "2022-02-22   15:02:36", "2022-02-03   09:47:22",
				"2022-01-31   13:21:46", "2022-01-12   15:15:36", "2022-01-10   16:59:04", "2022-01-07   12:30:43", "2022-01-06   16:49:37",
				"2022-01-05   08:29:19", "2021-12-06   10:41:51", "2021-11-22   12:29:15", "2021-11-15   10:36:12", "2021-10-25   09:24:04",
				"2021-10-14   09:31:38", "2021-10-11   09:38:34", "2021-10-04   14:03:55", "2021-09-15   16:08:12", "2021-09-10   17:08:55",
				"2021-09-06   13:58:09", "2021-09-01   15:08:11", "2021-08-31   11:45:28", "2021-08-28   23:19:36", "2021-08-27   07:47:13",
				"2021-08-24   10:54:43", "2021-08-23   13:39:28", "2021-08-18   12:11:02", "2021-08-11   23:25:28", "2021-08-10   15:36:51",
				"2021-08-04   10:39:28", "2021-08-04   06:36:22", "2021-07-27   10:41:47", "2021-07-24   11:50:49", "2021-07-20   14:08:51",
				"2021-07-20   11:16:10", "2021-07-13   16:50:29", "2021-07-06   10:14:14", "2021-07-01   09:48:46", "2021-06-30   07:20:15",
				"2021-06-28   10:09:51", "2021-06-24   22:46:28", "2021-06-23   13:22:00", "2021-06-18   10:32:50", "2021-06-07   18:36:04",
				"2021-05-27   18:29:57", "2021-05-27   12:37:25", "2021-05-25   17:53:40", "2021-05-25   15:39:59",
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "yyyy-MM-dd   HH:mm:ss");
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2}   \\d{2}:\\d{2}:\\d{2}");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			if (!input.isEmpty())
				assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicGenderNL() throws IOException, FTAException {
		final String[] inputs = {
				"M", "V", "M", "M", "O", "V", "M", "O", "M", "M", "M", "V", "M", "V", "V",
				"M", "M", "V", "M", "V", "M", "M", "M", "M", "O", "M", "V", "M", "V", "M"
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "GESLACH", Locale.forLanguageTag("nl-NL"), "GENDER.TEXT_<LANGUAGE>", FTAType.STRING, 1.0);

		assertEquals(result.getRegExp(), "(?i)(M|O|V)");
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void VAT_NL() throws IOException, FTAException {
		final String[] inputs = {
				"NL000000024B01", "NL813195779B01", "NL814170511B01", "NL815216002B01", "NL815498093B01",
				"NL000000048B01", "NL000000061B01", "NL000000073B01", "NL000000085B01", "NL000000103B01",
				"NL000000115B01", "NL000000127B01", "NL000000140B01", "NL000000152B01", "NL000000164B01",
				"NL000000188B01", "NL000000205B01", "NL000000206B01", "NL000000206B01", "NL001079293B01",
				"NL001368023B01", "NL003156709B01", "NL004909665B07", "NL005033019B01", "NL006292227B01",
				"NL010000445B01", "NL010000446B01", "NL121745417B01", "NL128297906B01", "NL147804668B01",
				"NL173389909B01", "NL208560129B01", "NL800272912B01", "NL805332674B01", "NL805969317B01",
				"NL806825790B01", "NL806925206B01", "NL809442127B01", "NL810195835B01", "NL810876334B01",
		};

		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "BTW-nummer", Locale.forLanguageTag("nl-NL"), "IDENTITY.VAT_<COUNTRY>", FTAType.STRING, 0.975);
		assertEquals(result.getMatchCount(), inputs.length - 2);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicPostalCodeNL() throws IOException, FTAException {
		final String[] inputs = {
			"2345AQ", "5993FG", "3898WW", "5543NH", "1992WW", "4002CS", "5982KG", "1090DD", "3030XX", "1088TR",
			"2547DE", "6587DS", "3215QQ", "7745VD", "4562DD", "4582SS", "2257WE", "3578HT", "4568FB", "1587SW",
			"4573LF", "3574SS", "8122GK", "4523EW", "7128RT", "2548RF", "6873HH", "4837NR", "2358EE", "3731HY"
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "P_PCODE", Locale.forLanguageTag("nl-NL"), "POSTAL_CODE.POSTAL_CODE_NL", FTAType.STRING, 1.0);

		assertEquals(result.getRegExp(), "\\d{4} \\p{IsAlphabetic}{2}|\\d{4}\\p{IsAlphabetic}{2}");
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testMunicipalityCodeNL() throws IOException, FTAException {
		final String[] samples = {
				"0014", "0034", "0037", "0047", "0059", "0060", "0074", "0080", "0085", "0086",
				"0088", "0090", "0106", "0114", "0118", "0193", "0233", "0307", "0363", "0600",
				"0622", "0623", "0624", "0625", "0626", "0627", "0628", "0629", "0630", "0631",
				"0632", "0633", "0634", "0635", "0636", "0637", "0638", "0639", "0640", "0641",
				"0642", "0643", "0644", "0645", "0646", "0647", "0648", "0649", "0650", "0651",
				"0652", "0653", "0654", "0655", "0656", "0657", "0658", "0659", "0660", "0661",
				"0662", "0663", "0664", "0665", "0666", "0667", "0668", "0669", "0670", "0671",
				"0672", "0673", "0674", "0675", "0676", "0677", "0678", "0679", "0680", "0681"
		};

		TestUtils.simpleCore(Sample.allValid(samples), "GEMEENTE_CODE", Locale.forLanguageTag("nl-NL"), "STATE_PROVINCE.MUNICIPALITY_CODE_NL", FTAType.LONG, 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void nameLast() throws IOException, FTAException {
		final String[] inputs = {
				"LEE", "WALKER", "SIMS", "SLAUGHTER", "ACHENBACH", "BARNES", "GALIS", "RAMPAGE", "GUINN", "HALLAT",
				"HEINER", "SMITH", "GOOSE", "MASON", "CANTOR", "SELPH", "SCHERER", "LOWENBRAU", "HAUGEN", "LEONARD",
				"HANNA", "CUSHMAN", "DENNING", "CLYMER", "CUSICK", "EDER", "EDGAR", "HANNAH", "CUSTER", "COAKLEY",
				"HANNAN", "CUSTODIO", "DENNIS", "HANNER", "HANNIGAN", "DENNISON", "EDGE", "EDGERTON", "DENNY", "EDINGER",
				"EDISON", "COATES", "COATS", "HANNINEN", "COBB", "HANNON", "HANNULA", "HANRAHAN", "DENSMORE", "HANS"
		};

		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "naam", Locale.forLanguageTag("nl-NL"), "NAME.LAST", FTAType.STRING, 1.0);

		assertEquals(result.getMatchCount(), inputs.length);

		final PluginDefinition pluginDefinition = PluginDefinition.findByName("NAME.LAST");
		final LogicalType knownSemanticType = LogicalTypeFactory.newInstance(pluginDefinition, new AnalysisConfig(Locale.forLanguageTag("nl-NL")));

		assertTrue(knownSemanticType.isValid("Segall"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void randomGender() throws IOException, FTAPluginException {
		final com.cobber.fta.LogicalTypeCode logical = (com.cobber.fta.LogicalTypeCode) LogicalTypeFactory.newInstance(PluginDefinition.findByName("GENDER.TEXT_<LANGUAGE>"), new AnalysisConfig(Locale.forLanguageTag("nl-NL")));

		assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void streetNameBareNL() throws IOException, FTAPluginException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("straat");
		analysis.setLocale(Locale.forLanguageTag("nl"));

		final String[] inputs = {
			"Kalverstraat", "Damrak", "Leidsestraat", "Herengracht", "Keizersgracht",
			"Prinsengracht", "Nieuwezijds Voorburgwal", "Rokin", "Spui", "Reguliersbreestraat",
			"Amstelstraat", "Utrechtsestraat", "Vijzelstraat", "Overtoom", "Jan Luijkenstraat"
		};
		for (final String s : inputs)
			analysis.train(s);

		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getSemanticType(), "STREET_NAME_BARE_NL");
		assertTrue(result.getConfidence() >= 0.95);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void streetNameBareNL_numericWordRejected() throws IOException, FTAPluginException {
		final LogicalType logical = LogicalTypeFactory.newInstance(PluginDefinition.findByName("STREET_NAME_BARE_<LANGUAGE>"), new AnalysisConfig(Locale.forLanguageTag("nl")));

		assertTrue(logical.isValid("Kalverstraat"));
		// A value that is purely numeric should be rejected
		assertFalse(logical.isValid("42"));
		// Empty input should be rejected
		assertFalse(logical.isValid(""));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void nlColorDetection() throws IOException, FTAException {
		final String[] inputs = {
			"ROOD", "BLAUW", "GROEN", "WIT", "ZWART", "ORANJE", "ROZE", "GRIJS",
			"BRUIN", "PAARS", "GEEL", "BEIGE", "ROOM", "MARINEBLAUW", "BOURGONDIË",
			"TURKOOIS", "INDIGO", "ZILVER", "GOUD", "BRONZEN", "ROOD", "BLAUW", "GROEN"
		};
		for (final String header : new String[] { "kleur", "Kleur", "haarkleur" }) {
			final TextAnalyzer analysis = new TextAnalyzer(header);
			analysis.setLocale(Locale.forLanguageTag("nl-NL"));
			for (final String s : inputs)
				analysis.train(s);
			assertEquals(analysis.getResult().getSemanticType(), "COLOR.TEXT_NL", "header: " + header);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void nlColorNoHeader() throws IOException, FTAException {
		final String[] inputs = {
			"ROOD", "BLAUW", "GROEN", "WIT", "ZWART", "ORANJE", "ROZE", "GRIJS",
			"BRUIN", "PAARS", "GEEL", "BEIGE", "ROOM", "MARINEBLAUW", "BOURGONDIË"
		};
		final TextAnalyzer analysis = new TextAnalyzer("col");
		analysis.setLocale(Locale.forLanguageTag("nl-NL"));
		for (final String s : inputs)
			analysis.train(s);
		assertNotEquals(analysis.getResult().getSemanticType(), "COLOR.TEXT_NL");
	}
}
