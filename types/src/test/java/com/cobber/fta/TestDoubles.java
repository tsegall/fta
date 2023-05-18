/*
 * Copyright 2017-2023 Tim Segall
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
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.FTAUnsupportedLocaleException;
import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;

public class TestDoubles {
	private static final SecureRandom random = new SecureRandom();
	private final Logger logger = LoggerFactory.getLogger("com.cobber.fta");

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void positiveDouble() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("positiveDouble");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final String[] inputs = "43.80|1.1|0.1|2.03|.1|99.23|14.08976|14.085576|3.141592654|2.7818|1.414|2.713".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, -1);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_DOUBLE));
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertNull(result.getTypeModifier());
		assertEquals(result.getMinValue(), "0.1");
		assertEquals(result.getMaxValue(), "99.23");
		assertEquals(result.getMinLength(), 2);
		assertEquals(result.getMaxLength(), 11);

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		assertNull(result.checkCounts());

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void tinyDouble() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("tinyDouble");
		final String input = "0e-17";

		analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_DOUBLE_WITH_EXPONENT));
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getMinValue(), "0E0");
		assertEquals(result.getMaxValue(), "0E0");
		assertEquals(result.getMinLength(), 5);
		assertEquals(result.getMaxLength(), 5);
		assertTrue(input.matches(result.getRegExp()));
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void positiveDouble2() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("positiveDouble2");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final String[] inputs = "43.80|1.1|0.1|2.03|0.1|99.23|14.08976|14.085576|3.141592654|2.7818|1.414|2.713".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, -1);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_DOUBLE));
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getMinValue(), "0.1");
		assertEquals(result.getMaxValue(), "99.23");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void negativeDouble() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("negativeDouble");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final String[] inputs = "43.80|-1.1|-.1|2.03|.1|-99.23|14.08976|-14.085576|3.141592654|2.7818|1.414|2.713".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, -1);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_SIGNED_DOUBLE));
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), TypeInfo.TypeModifier.SIGNED.toString());
		assertNull(result.getSemanticType());
		assertEquals(result.getMinValue(), "-99.23");
		assertEquals(result.getMaxValue(), "43.8");
		assertNull(result.checkCounts());

		assertTrue("0".matches(result.getRegExp()));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void basicPromoteToDouble() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicPromoteToDouble");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final String pipedInut =
					"8|172.67|22.73|150|30.26|54.55|45.45|433.22|172.73|7.73|" +
						"218.18|47.27|31.81|22.73|21.43|7.27|26.25|7.27|45.45|80.91|" +
						"63.64|13.64|45.45|15|425.45|95.25|60.15|100|80|72.73|" +
						"0.9|181.81|90|545.45|33.68|13.68|12.12|15|615.42|";
		final String inputs[] = pipedInut.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_DOUBLE));
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void trailingMinus() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("trailingMinus");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final String[] inputs = "458.00-|123.00|901.21|404.064|209.01-|12.0|0.0|0|676.00|1894.80-|2903.22-|111.14-|5234.00".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, -1);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_DOUBLE) + "-?");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), "SIGNED_TRAILING");
		assertEquals(result.getMinValue(), "-2903.22");
		assertEquals(result.getMaxValue(), "5234.0");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void floatBug() throws IOException, FTAException {
		final String[] samples = {
				"352115", "277303", "324576", "818328", "698915", "438223", "104583", "355898", "387829", "130771",
				"246823", "833969", "380632", "467021", "869890", "15191", "463747", "847192", "706545", "895018",
				"311867",
				"0.4704197792748601", "0.8005132170623999", "0.015796397806505325", "0.9830897509338489", "0.669847612276236",
				"0.9325644671976738", "0.5373506913452817", "0.21823369307871965", "0.1699104680573703", "0.18275707526552865",
				"0.24983460286935077", "0.772409965970719", "1.1388812589363528E-4", "0.78120115126727", "0.6386556468768979",
				"0.8730028156182696", "0.8296568674820993", "0.3250682023283127", "0.7261517112855164", "0.09470135380197953" };
		final TextAnalyzer analysis = new TextAnalyzer("floatBug");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		int locked = -1;
		int index = 0;

		analysis.setDetectWindow(2* AnalysisConfig.DETECT_WINDOW_DEFAULT);
		for (int i = 0; i <= AnalysisConfig.DETECT_WINDOW_DEFAULT; i++) {
			if (analysis.train(samples[index++]) && locked == -1)
				locked = index;
		}
		for (int i = 0; i < AnalysisConfig.DETECT_WINDOW_DEFAULT; i++) {
			if (analysis.train(samples[index++]) && locked == -1)
				locked = index;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, 2 * AnalysisConfig.DETECT_WINDOW_DEFAULT + 1);
		assertEquals(result.getSampleCount(), 2 * AnalysisConfig.DETECT_WINDOW_DEFAULT + 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_DOUBLE_WITH_EXPONENT));
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()), sample);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void verySmall() throws IOException, FTAException {
		final String smallest = "1.1388812589363528E-69";
		final String largest = "1.1388812589363528E-40";
		final String[] samples = {
				largest, "1.1388812589363528E-41", "1.1388812589363528E-42",
				"1.1388812589363528E-43", "1.1388812589363528E-44", "1.1388812589363528E-45",
				"1.1388812589363528E-46", "1.1388812589363528E-47", "1.1388812589363528E-48",
				"1.1388812589363528E-49", "1.1388812589363528E-50", "1.1388812589363528E-51",
				"1.1388812589363528E-52", "1.1388812589363528E-53", "1.1388812589363528E-54",
				"1.1388812589363528E-55", "1.1388812589363528E-56", "1.1388812589363528E-57",
				"1.1388812589363528E-58", "1.1388812589363528E-59", "1.1388812589363528E-60",
				"1.1388812589363528E-61", "1.1388812589363528E-62", "1.1388812589363528E-63",
				"1.1388812589363528E-64", "1.1388812589363528E-65", "1.1388812589363528E-66",
				"1.1388812589363528E-67", "1.1388812589363528E-68", smallest
		};

		final TextAnalyzer analysis = new TextAnalyzer("verySmall");
		analysis.setLocale(Locale.forLanguageTag("en-US"));

		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_DOUBLE_WITH_EXPONENT));
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(Double.parseDouble(result.getMinValue()), Double.parseDouble(smallest), TestUtils.EPSILON);
		assertEquals(Double.parseDouble(result.getMaxValue()), Double.parseDouble(largest), TestUtils.EPSILON);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()), sample);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void signedFloatBug() throws IOException, FTAException {
		final String[] samples = {
				"352115", "277303", "324576", "818328", "698915", "438223", "104583", "355898", "387829", "130771",
				"246823", "833969", "380632", "467021", "869890", "15191", "463747", "847192", "706545", "895018",
				"311867",
				"0.4704197792748601", "-0.8005132170623999", "0.015796397806505325", "0.9830897509338489", "0.669847612276236",
				"0.9325644671976738", "0.5373506913452817", "0.21823369307871965", "0.1699104680573703", "0.18275707526552865",
				"0.24983460286935077", "0.772409965970719", "1.1388812589363528E+4", "0.78120115126727", "0.6386556468768979",
				"0.8730028156182696", "0.8296568674820993", "0.3250682023283127", "0.7261517112855164", "0.09470135380197953" };
		final TextAnalyzer analysis = new TextAnalyzer("signedFloatBug");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		int locked = -1;
		int index = 0;

		analysis.setDetectWindow(2* AnalysisConfig.DETECT_WINDOW_DEFAULT);
		for (int i = 0; i <= AnalysisConfig.DETECT_WINDOW_DEFAULT; i++) {
			if (analysis.train(samples[index++]) && locked == -1)
				locked = index;
		}
		for (int i = 0; i < AnalysisConfig.DETECT_WINDOW_DEFAULT; i++) {
			if (analysis.train(samples[index++]) && locked == -1)
				locked = index;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, 2 * AnalysisConfig.DETECT_WINDOW_DEFAULT + 1);
		assertEquals(result.getSampleCount(), 2 * AnalysisConfig.DETECT_WINDOW_DEFAULT + 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), "SIGNED");
		assertNull(result.getSemanticType());
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_SIGNED_DOUBLE_WITH_EXPONENT));
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void mixedInput() throws IOException, FTAException {
		final String[] samples = {
				"8", "172.67", "22.73", "150", "30.26", "54.55", "45.45", "433.22", "172.73", "7.73",
				"218.18", "47.27", "31.81", "22.73", "21.43", "7.27", "26.25", "7.27", "45.45" };

		final TextAnalyzer analysis = new TextAnalyzer("mixedInput");
		analysis.setLocale(Locale.forLanguageTag("en-US"));

		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getRegExp(), "\\d*\\.?\\d+");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMean(), 80.26315789473685);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void impossibleExponent() throws IOException, FTAException {
		final String[] samples = {
				"1001E803", "3232E103", "1333E303", "1444E773", "8888E603", "1099E503", "1000E401", "1000E404", "1220E533", "1103E402",
				"1001E803", "3232E103", "1333E303", "1444E773", "8888E603", "1099E503", "1000E401", "1000E404", "1220E503", "1103E402"
		};
		final TextAnalyzer analysis = new TextAnalyzer("impossibleExponent");

		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getRegExp(), "\\d{4}\\p{IsAlphabetic}\\d{3}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void impossibleExponentNullHeader() throws IOException, FTAException {
		final String[] samples = {
				"1001E803", "3232E103", "1333E303", "1444E773", "8888E603", "1099E503", "1000E401", "1000E404", "1220E533", "1103E402",
				"1001E803", "3232E103", "1333E303", "1444E773", "8888E603", "1099E503", "1000E401", "1000E404", "1220E503", "1103E402"
		};
		final TextAnalyzer analysis = new TextAnalyzer("impossibleExponentNullHeader");

		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getRegExp(), "\\d{4}\\p{IsAlphabetic}\\d{3}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void singleDoubleWithWidening() throws IOException, FTAException {
		final String[] samples = {
				"0", "", "0", "0", "0", "", "0", "0", "0", "",
				"0", "", "0", "0", "0", "", "0", "0", "0", "",
				"0", "", "0", "0", "0", "", "0", "0", "0", "",
				"0", "", "0", "0", "0", "", "0", "0", "0", "",
				"0", "", "0", "0", "0", "", "0", "0", "0", "",
				"0", "", "0", "0", "0", "", "0", "0", "0", "",
				"0", "", "0", "0", "0", "", "0", "0", "0", "",
				"0", "", "0", "0", "0", "", "0", "0", "0", "",
				"0", "", "0", "0", "0", "", "0", "0", "0", "",
				"0", "", "0", "0", "0", "", "0", "0", "0", "",
				"-16.71",
				"0", "", "0", "0", "0", "", "0", "0", "0", ""

		};
		final TextAnalyzer analysis = new TextAnalyzer("EURO_OUTSTANDING");
		analysis.setLocale(Locale.forLanguageTag("en-US"));

		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), "SIGNED");
		assertEquals(result.getRegExp(), "[+-]?\\d*\\.?\\d+");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

//		for (int i = 0; i < samples.length; i++) {
//			assertTrue(samples[i].matches(result.getRegExp()));
//		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void simpleIntegerWithWidening() throws IOException, FTAException {
		final String[] samples = {
				"0", "", "0", "0", "0", "", "0", "0", "0", "",
				"0", "", "0", "0", "0", "", "0", "0", "0", "",
				"0", "", "0", "0", "0", "", "0", "0", "0", "",
				"0", "", "0", "0", "0", "", "0", "0", "0", "",
				"0", "", "0", "0", "0", "", "0", "0", "0", "",
				"0", "", "0", "0", "0", "", "0", "0", "0", "",
				"0", "", "0", "0", "0", "", "0", "0", "0", "",
				"0", "", "0", "0", "0", "", "0", "0", "0", "",
				"0", "", "0", "0", "0", "", "0", "0", "0", "",
				"0", "", "0", "0", "0", "", "0", "0", "0", "",
				"Hello",
				"0", "", "0", "0", "0", "", "0", "0", "0", ""

		};
		final TextAnalyzer analysis = new TextAnalyzer("EURO_OUTSTANDING");

		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getBlankCount(), 33);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getRegExp(), "\\d");
		assertEquals(result.getConfidence(), 1 - (double)1/(result.getSampleCount() - result.getBlankCount()));
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void lateDouble() throws IOException, FTAException {
		final String[] samples = {
				"-10000", "-1000", "-340", "-2500", "-1000", "-2062", "-2500", "-1927", "-2500", "-1927",
				"-1000", "-2062", "-2000", "-8000", "-8000", "-15000", "-2500", "-15000", "-5000", "-1000",
				"-1393.26"
		};
		final TextAnalyzer analysis = new TextAnalyzer("CDS Notional:unicode");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		assertEquals(analysis.getThreshold(), AnalysisConfig.DETECTION_THRESHOLD_DEFAULT);
		analysis.setThreshold(96);
		assertEquals(analysis.getThreshold(), 96);

		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), "SIGNED");
		assertNull(result.getSemanticType());
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_SIGNED_DOUBLE));
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void manyRandomDoubles() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("manyRandomDoubles");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final int nullIterations = 50;
		final int iterations = 2 * AnalysisConfig.MAX_CARDINALITY_DEFAULT;
		int locked = -1;

		for (int i = 0; i < nullIterations; i++) {
			analysis.train(null);
		}
		for (int i = 0; i < iterations; i++) {
			if (analysis.train(String.valueOf(random.nextDouble())) && locked == -1)
				locked = i;
		}
		// This is an invalid value
		analysis.train("Zoomer");

		// These are valid doubles
		analysis.train("NaN");
		analysis.train("Infinity");

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), iterations + nullIterations + 3);
		assertEquals(result.getCardinality(), AnalysisConfig.MAX_CARDINALITY_DEFAULT);
		assertEquals(result.getNullCount(), nullIterations);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getInvalidCount(), 3);
		final Map<String, Long> invalids = result.getInvalidDetails();
		assertEquals(invalids.size(), 3);
		assertEquals(invalids.get("Zoomer"), 1L);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void manyConstantLengthDoublesI18N_1() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("manyConstantLengthDoublesI18N_1");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final int nullIterations = 50;
		final int iterations = 2 * AnalysisConfig.MAX_CARDINALITY_DEFAULT;
		int locked = -1;
		final Locale locale = Locale.forLanguageTag("de-AT");
		analysis.setLocale(locale);
		final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);
		final Set<String> samples = new HashSet<>();

		for (int i = 0; i < nullIterations; i++) {
			analysis.train(null);
		}
		int cnt = 0;
		while (cnt < iterations) {
			final long randomLong = random.nextInt(Integer.MAX_VALUE) + 1000000000L;
			if (randomLong >  9999999999L)
				continue;
			final String sample = String.valueOf(randomLong) + formatSymbols.getDecimalSeparator() + random.nextInt(10);
			samples.add(sample);
			if (analysis.train(sample) && locked == -1)
				locked = cnt;
			cnt++;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), iterations + nullIterations);
		assertEquals(result.getCardinality(), AnalysisConfig.MAX_CARDINALITY_DEFAULT);
		assertEquals(result.getNullCount(), nullIterations);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_DOUBLE));
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void manyFrenchDoubles() throws IOException, FTAException {
		final String[] samplesUS = {
				"54.00", "12719300.00", "4819.00", "262612.00", "141300.00",
				"44876900.00", "681023.00", "460198.00", "1123220.00", "18300.00",
				"166658.00", "114656.00", "61461.00", "263058.00", "23747.00",
				"44539.00", "70836.00", "351498.00", "669803.00", "116655.00",
				"542.00", "12719300.00", "4819.00", "262612.00", "141300.00",
				"44876900.00", "681023.00", "460198.00", "1123220.00", "18300.00"

		};
		final String[] samples = new String[samplesUS.length];
		final TextAnalyzer analysis = new TextAnalyzer("manyFrenchDoubles");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final Locale locale = Locale.forLanguageTag("fr-FR");
		analysis.setLocale(locale);

		final DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(locale);
		formatter.setMinimumFractionDigits(2);
		formatter.setMaximumFractionDigits(2);

		for (int i = 0; i < samples.length; i++) {
			final double d = Double.parseDouble(samplesUS[i]);
			samples[i] = formatter.format(d);
		}

		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getDecimalSeparator(), ',');
		assertEquals(result.getTypeModifier(), "GROUPING");
		assertNull(result.getSemanticType());
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_DOUBLE_GROUPING));
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void doublesWithNan() throws IOException, FTAException {
		final String[] samples = {
				"", "9.04", "nan", "0.92", "0.89", "9.02", "-1.88", "4.84", "", "", "1.24", "1.83", "-0.23", "1.35", "nan",
				"1.4", "1.34", "-0.48", "0.6", "1.23", "", "9.04", "nan", "0.92", "0.89", "9.02", "-1.88", "4.84", "",
				"", "1.24", "1.83", "-0.23", "1.35", "nan", "1.4", "1.34", "-0.48", "0.6", "1.23",
				"22.24", "1202.43", "-0.234", "71.45", "3.411", "234.321", "-0.4848", "0.66666", "1.23", "10.0"
		};
		final TextAnalyzer analysis = new TextAnalyzer("doublesWithNan");
		analysis.setLocale(Locale.forLanguageTag("en-US"));

		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getDecimalSeparator(), '.');
		assertEquals(result.getTypeModifier(), "SIGNED");
		assertNull(result.getSemanticType());
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_SIGNED_DOUBLE));
		assertEquals(result.getConfidence(), 1 - (double)4/(result.getSampleCount() - result.getBlankCount()));
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples) {
			if (!sample.isEmpty() && !"nan".equals(sample))
				assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void dangerousDouble() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;
		final Set<String> samples = new HashSet<>();
		final TextAnalyzer analysis = new TextAnalyzer("Simple");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		analysis.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);

		analysis.train("1010e:");
		for (int i = 0; i < SAMPLE_COUNT; i++) {
			final String sample = String.format("%04d.0e+%d",
					random.nextInt(10000), random.nextInt(10));
			samples.add(sample);
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), SAMPLE_COUNT + 1);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_DOUBLE_WITH_EXPONENT));
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void lowerE() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;
		final Set<String> samples = new HashSet<>();
		final TextAnalyzer analysis = new TextAnalyzer("Simple");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		analysis.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);

		double min = Double.MAX_VALUE;
		analysis.train("1010e:");
		for (int i = 0; i < SAMPLE_COUNT; i++) {
			final String sample = String.format("%04d.0e%d",
					random.nextInt(10000), random.nextInt(10));
			samples.add(sample);
			final double d = Double.parseDouble(sample.toUpperCase(Locale.ROOT));
			if (d < min)
				min = d;
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), SAMPLE_COUNT + 1);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_DOUBLE_WITH_EXPONENT));
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
		assertEquals(Double.valueOf(result.getMinValue()), min);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void exponentWithPlus() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;
		final Set<String> samples = new HashSet<>();
		final TextAnalyzer analysis = new TextAnalyzer("Simple");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		analysis.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);

		analysis.train("1010e:");
		for (int i = 0; i < SAMPLE_COUNT; i++) {
			final String sample = String.format("%04d.0E+%d",
					random.nextInt(10000), random.nextInt(10));
			samples.add(sample);
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), SAMPLE_COUNT + 1);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_DOUBLE_WITH_EXPONENT));
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void longWithOutlierPositiveDouble() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;
		final Set<String> samples = new HashSet<>();
		final TextAnalyzer analysis = new TextAnalyzer("Simple");
		analysis.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);

		for (int i = 0; i < SAMPLE_COUNT; i++) {
			final String sample = String.format("%04d", i);
			samples.add(sample);
			analysis.train(sample);
		}
		analysis.train("10000e+13");

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), SAMPLE_COUNT + 1);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_DOUBLE_WITH_EXPONENT));
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void longWithOutlierNegativeDouble() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;
		final Set<String> samples = new HashSet<>();
		final TextAnalyzer analysis = new TextAnalyzer("Simple");
		analysis.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);

		for (int i = 0; i < SAMPLE_COUNT; i++) {
			final String sample = String.format("%04d", i);
			samples.add(sample);
			analysis.train(sample);
		}
		analysis.train("10000e+13");
		analysis.train("-1000e+13");

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), SAMPLE_COUNT + 2);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_SIGNED_DOUBLE_WITH_EXPONENT));
		assertEquals(result.getTypeModifier(), "SIGNED");
		assertNull(result.getSemanticType());
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void manyConstantLengthDoublesI18N_2() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("manyConstantLengthDoublesI18N_2");
		final int nullIterations = 50;
		final int iterations = 2 * AnalysisConfig.MAX_CARDINALITY_DEFAULT;
		int locked = -1;
		final Locale locale = Locale.forLanguageTag("de-DE");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		analysis.setLocale(locale);
		final Set<String> samples = new HashSet<>();

		for (int i = 0; i < nullIterations; i++) {
			analysis.train(null);
		}
		int cnt = 0;
		while (cnt < iterations) {
			final long randomLong = random.nextInt(Integer.MAX_VALUE) + 1000000000L;
			if (randomLong >  9999999999L)
				continue;
			final String sample = String.valueOf(randomLong) + "." + random.nextInt(10);
			samples.add(sample);
			if (analysis.train(sample) && locked == -1)
				locked = cnt;
			cnt++;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), iterations + nullIterations);
		assertEquals(result.getCardinality(), AnalysisConfig.MAX_CARDINALITY_DEFAULT);
		assertEquals(result.getNullCount(), nullIterations);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), "NON_LOCALIZED");
		assertEquals(result.getRegExp(), "\\d*\\.?\\d+");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void testFromFuzz1() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("testFromFuzz1");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final String input =
				"100|-101.0|102|103|104|105|106|107|hello|109|110|111|112|113|114|115|116|117|118|119|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_SIGNED_DOUBLE));
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
		assertEquals(result.getMinValue(), "-101.0");
		assertEquals(result.getMaxValue(), "119.0");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void backoutToDouble() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("backoutToDouble");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final String pipedInput =
				"0|0|0|0|0|0|0|0|0|1|2|3|0|0|0|0|0|" +
						"0|0|0|0|0.25|0|0|0|0|0|0|0|0|0|0|0|0|" +
						"0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|" +
						"0|0|0|0|0|0|0|0.02|0.06|0|0|0|0|0|0.02|0|0|" +
						"0.02|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|" +
						"0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|" +
						"0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|" +
						"0|0|0|0|0|0|0|0|0|0|0.02|0.02|0|0|0|0|0|" +
						"0.14|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|" +
						"0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|" +
						"0|0|0.25|0|0|0|0|0|0|0|0|0|0|0|0|0|0|" +
						"0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|" +
						"0|0|0|0|0|0.05|0|0|0|0|0|0|0.02|0|0.02|0|0|" +
						"0|0.01|0|0|0|0|0.02|0|0|0|0|0.01|0|0|0|||";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount() + result.getBlankCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_DOUBLE));
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "0.0");
		assertEquals(result.getMaxValue(), "3.0");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		final String regExp = result.getRegExp();
		for (final String input : inputs) {
			if (input.length() == 0)
				continue;
			assertTrue(input.matches(regExp), input);
		}
	}

	boolean isTooHard(final Locale locale) {
		final boolean simple = NumberFormat.getNumberInstance(locale).format(0).matches("\\d");

		if (!simple) {
//			logger.debug("Skipping locale '{}' as it does not use Arabic numerals.", locale);
			return true;
		}

		final Calendar cal = GregorianCalendar.getInstance(locale);
		if (!(cal instanceof GregorianCalendar)) {
//			logger.debug("Skipping locale '{}' as it does not use the Gregorian calendar.", locale);
			return true;
		}

		final String negPrefix = TestUtils.getNegativePrefix(locale);
		final String negSuffix = TestUtils.getNegativeSuffix(locale);

		if (negPrefix.isEmpty() && negSuffix.isEmpty()) {
//			logger.debug("Skipping locale '{}' as it has empty negPrefix and negSuffix.", locale);
			return true;
		}

		final String variant = locale.getDisplayVariant();
		if (variant != null && !variant.isEmpty()) {
//			logger.debug("Skipping locale '{}' as it has a Variant: '{}'.", locale, variant);
			return true;
		}

		final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);
		final String getExponentSeparator = formatSymbols.getExponentSeparator();
		if (getExponentSeparator.length() != 1 || (getExponentSeparator.charAt(0) != 'e' &&  getExponentSeparator.charAt(0) != 'E')) {
//			logger.debug("Skipping locale '{}' as it uses a non-standard exponentiaion character ({}).", locale, getExponentSeparator);
			return true;
		}

		return false;
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void localeNegativeDoubleTest() throws IOException, FTAException {
		final int SAMPLE_SIZE = 1000;
		final Locale[] locales = DateFormat.getAvailableLocales();
//		Locale[] locales = new Locale[] { Locale.forLanguageTag("en-US"), Locale.forLanguageTag("hr-HR") };
//		Locale[] locales = new Locale[] { Locale.forLanguageTag("he") };

		for (final Locale locale : locales) {
			final TextAnalyzer analysis = new TextAnalyzer("Separator");
			analysis.setLocale(locale);

			if (isTooHard(locale))
				continue;

			final NumberFormat nf = NumberFormat.getNumberInstance(locale);

			final Set<String> samples = new HashSet<>();
			nf.setMinimumFractionDigits(2);

			try {
				for (int i = 0; i < SAMPLE_SIZE; i++) {
					final double d = random.nextDouble() * random.nextInt();
					String sample = nf.format(d);
					if (KnownTypes.LEFT_TO_RIGHT_MARK == sample.charAt(0))
						throw new FTAUnsupportedLocaleException("Locale uses Left-to-right Mark");
					samples.add(sample);
					analysis.train(sample);
				}
			}
			catch (FTAUnsupportedLocaleException e) {
				logger.debug("Skipping locale '{}' = reason: '{}'.", locale, e.getMessage());
				continue;
			}

			final TextAnalysisResult result = analysis.getResult();
			TestUtils.checkSerialization(analysis);

			if (result.getType() != FTAType.DOUBLE) {
				System.err.println("locale: " + locale);
				for (final String sample : samples)
					System.err.println(sample);
			}
			assertEquals(result.getType(), FTAType.DOUBLE);
			assertEquals(result.getTypeModifier(), "SIGNED,GROUPING");
			assertNull(result.getSemanticType());
			assertEquals(result.getSampleCount(), SAMPLE_SIZE);
			assertEquals(result.getMatchCount(), SAMPLE_SIZE);
			assertEquals(result.getNullCount(), 0);
			assertEquals(result.getLeadingZeroCount(), 0);

			assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_SIGNED_DOUBLE_GROUPING));
			assertEquals(result.getConfidence(), 1.0);
			assertNull(result.checkCounts());

			TestSupport.checkHistogram(result, 10, true);
			TestSupport.checkQuantiles(result);

			for (final String sample : samples)
				assertTrue(sample.matches(result.getRegExp()), sample + " " + result.getRegExp());
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void simpleDoubleExponentTest() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("simpleDoubleExponentTest");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final DecimalFormat df = new DecimalFormat("0.00E00");
		final int SAMPLE_SIZE = 1000;
		String sample;

		final Set<String> samples = new HashSet<>();
		final NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setMinimumFractionDigits(1);
		for (int i = 0; i < 10; i++) {
			final double d = random.nextDouble();
			sample = nf.format(d);
			samples.add(sample);
			analysis.train(sample);
		}

		for (int i = 0; i < SAMPLE_SIZE; i++) {
			final double d = random.nextDouble() * random.nextInt();
			sample = df.format(d);
			samples.add(sample);
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), "SIGNED");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), SAMPLE_SIZE + 10);
		assertEquals(result.getMatchCount(), SAMPLE_SIZE + 10);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);

		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_SIGNED_DOUBLE_WITH_EXPONENT));
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		final String actualRegExp = result.getRegExp();

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String s : samples) {
			assertTrue(s.matches(actualRegExp), s + " " + actualRegExp);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void localeNegativeDoubleExponentTest() throws IOException, FTAException {
		final int SAMPLE_SIZE = 1000;
		final Locale[] locales = DateFormat.getAvailableLocales();
//		final Locale[] locales = { Locale.forLanguageTag("nn") };

		for (final Locale locale : locales) {
			final TextAnalyzer analysis = new TextAnalyzer("localeNegativeDoubleExponentTest");
			analysis.setLocale(locale);

			if ("Arabic".contentEquals(locale.getDisplayLanguage())) {
				logger.debug("Skipping Arabic locale '{}' - broken on Java 8.", locale.toLanguageTag());
				continue;
			}

			if (isTooHard(locale))
				continue;


			if ("mk-MK".contentEquals(locale.toLanguageTag())) {
				logger.debug("Skipping locale '{}' as it has trailing neg suffix.", locale);
				continue;
			}

			final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);
			final DecimalFormat df = new DecimalFormat("#E0", formatSymbols);

//			final String grp = RegExpGenerator.slosh(formatSymbols.getGroupingSeparator());
//			final String dec = RegExpGenerator.slosh(formatSymbols.getDecimalSeparator());
//			logger.debug("Locale '{}', grouping: '{}', decimal: '{}', negPrefix: '{}', negSuffix: '{}'.",
//					locale, grp, dec, TestUtils.getNegativePrefix(locale), TestUtils.getNegativeSuffix(locale));

			final Set<String> samples = new HashSet<>();
			try {
				for (int i = 0; i < SAMPLE_SIZE; i++) {
					double d = random.nextDouble() * random.nextInt();
					final int pow = random.nextInt(10);
					if (pow % 2 == 0)
						d *= Math.pow(10, pow);
					else
						d *= Math.pow(d,  -pow);

					final String sample = df.format(d);
					if (KnownTypes.LEFT_TO_RIGHT_MARK == sample.charAt(0))
						throw new FTAUnsupportedLocaleException("Locale uses Left-to-right Mark");
					samples.add(sample);
					analysis.train(sample);
				}
			}
			catch (FTAUnsupportedLocaleException e) {
				logger.debug("Skipping locale '{}' = reason: '{}'.", locale, e.getMessage());
				continue;
			}

			final TextAnalysisResult result = analysis.getResult();
			TestUtils.checkSerialization(analysis);

			assertEquals(result.getType(), FTAType.DOUBLE);
			assertEquals(result.getTypeModifier(), "SIGNED");
			assertNull(result.getSemanticType());
			assertEquals(result.getSampleCount(), SAMPLE_SIZE);
			assertEquals(result.getMatchCount(), SAMPLE_SIZE);
			assertEquals(result.getNullCount(), 0);
			assertEquals(result.getLeadingZeroCount(), 0);

			assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_SIGNED_DOUBLE_WITH_EXPONENT));
			assertEquals(result.getConfidence(), 1.0);
			assertNull(result.checkCounts());

			final String actualRegExp = result.getRegExp();

			TestSupport.checkHistogram(result, 10, true);
			TestSupport.checkQuantiles(result);

			for (final String sample : samples) {
				if (!sample.matches(actualRegExp))
					logger.debug("Locale: " + locale + " " + sample + " " + actualRegExp);
				assertTrue(sample.matches(actualRegExp), "Locale: " + locale + " " + sample + " " + actualRegExp);
			}
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void spacedDoubles() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("AMT");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String inputs[] = {
				" 000000512.80", "-000000512.80", "-000000006.96", "-000000206.43", "-000000078.40", " 000000000.03", "-000000000.03", "-000000010.60", " 000000244.87", " 000000917.60",
				" 000000150.00", " 000000024.00", " 000000035.00", " 000000150.00", " 000000035.00", " 000000010.00", " 000000035.00", " 000000035.00", " 000000035.00", " 000000002.80",
				" 000000024.00", " 000000008.40", " 000000005.60", " 000000005.60", " 000000005.60", " 000000035.00", " 000000005.60", " 000000005.60", " 000000919.52", "-000000919.52",
				" 000001839.04", "-000001839.04", " 000000033.12", " 000000002.65", " 000000035.77", "-000000035.77", " 000000959.10", " 000001150.92", " 000000294.45", " 000000353.34",
				" 000000001.73", " 000000001.73", "-000001807.82", " 000000170.83", "-000000000.02", "-000000012.21", "-000000011.22", "-000000015.20", " 000006940.81", " 000001520.00",
				" 000023026.50", " 000005090.71", " 000000047.12", "-000000018.40", "-000000015.97", "-000000012.75", "-000000244.87", "-000000917.60", " 000000078.40", " 000001087.06",
				"-000001087.06", "-000000009.62", "-000000006.98", "-000000004.95", "-000000002.42", " 000001669.40", " 000000306.68", " 000000086.00", " 000000340.32", "-000000139.78",
				"-000000138.70", "-000000132.07", "-000000128.53", "-000000147.21", " 000000686.29", " 000000307.40", "-000000086.84", "-000000167.41", "-000000053.15", " 000004700.34",
				" 000000227.82", "-000001302.62", "-000000516.30", "-000000516.30", "-000000520.23", "-000000520.23", "-000000520.23", "-000000516.30", "-000000059.06", "-000000055.26",
				"-000000059.06", "-000000059.06", "-000000059.06", "-000000055.13", "-000000055.13", "-000000055.13", "-000000059.06", " 000000074.50", "-000000060.24", " 000001720.00"
				,
				" 000006018.37", " 000000026.00", " 000000026.00", "-000000013.87", "-000000414.82", "-000000150.61", "-000000009.77"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), "SIGNED");
		assertNull(result.getSemanticType());
		assertEquals(result.getRegExp(), KnownTypes.PATTERN_WHITESPACE + analysis.getRegExp(KnownTypes.ID.ID_SIGNED_DOUBLE));
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void latitude() throws IOException, FTAException {
		final TextAnalyzer analysis1 = new TextAnalyzer("LATITUDE");
		final TextAnalyzer analysis2 = new TextAnalyzer("LATITUDE");
		final TextAnalyzer analysis3 = new TextAnalyzer("LATITUDE");
		analysis1.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		analysis1.setDebug(2);
		analysis2.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		analysis2.setDebug(2);
		analysis3.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		analysis3.setDebug(2);
		final String inputs1[] = {
				"-89.00", "-88.80", "-87.96", "-86.43", "-85.40", "84.03", "-83.03", "-82.60", "-81.87", "-80.60",
				"-69.00", "-88.80", "-87.96", "-86.43", "-85.40", "84.03", "-83.03", "-82.60", "-81.87", "-80.60",
				"-49.00", "-88.80", "-87.96", "-86.43", "-85.40", "84.03", "-83.03", "-82.60", "-81.87", "-80.60",
				"-29.00", "-88.80", "-87.96", "-86.43", "-85.40", "84.03", "-83.03", "-82.60", "-81.87", "-80.60",
				"9.00", "-88.80", "-87.96", "-86.43", "-85.40", "84.03", "-83.03", "-82.60", "-81.87", "-80.60",
		};
		final String inputs2[] = {
				"29.00", "-88.80", "-87.96", "-86.43", "-85.40", "24.03", "-83.03", "-82.60", "-81.87", "-80.60",
				"49.00", "-88.80", "-87.96", "-86.43", "-85.40", "44.03", "-83.03", "-82.60", "-81.87", "-80.60",
				"69.00", "-88.80", "-87.96", "-86.43", "-85.40", "64.03", "-83.03", "-82.60", "-81.87", "-80.60",
				"89.00", "88.80", "87.96", "86.43", "85.40", "4.03", "83.03", "82.60", "81.87", "80.60",
		};
		// Same as inputs1 but sorted - should have same dataSignature
		final String inputs3[] = {
				"-89.00", "-88.80", "-88.80", "-88.80", "-88.80", "-88.80", "-87.96", "-87.96", "-87.96", "-87.96",
				"-87.96", "-86.43", "-86.43", "-86.43", "-86.43", "-86.43", "-85.40", "-85.40", "-85.40", "-85.40",
				"-85.40", "-83.03", "-83.03", "-83.03", "-83.03", "-83.03", "-82.60", "-82.60", "-82.60", "-82.60",
				"-82.60", "-81.87", "-81.87", "-81.87", "-81.87", "-81.87", "-80.60", "-80.60", "-80.60", "-80.60",
				"-80.60", "-69.00", "-49.00", "-29.00", "9.00", "84.03", "84.03", "84.03", "84.03", "84.03",
		};
		int locked = -1;

		for (int i = 0; i < inputs1.length; i++) {
			if (analysis1.train(inputs1[i]) && locked == -1)
				locked = i;
		}
		final TextAnalysisResult result1 = analysis1.getResult();

		for (int i = 0; i < inputs2.length; i++) {
			if (analysis2.train(inputs2[i]) && locked == -1)
				locked = i;
		}
		final TextAnalysisResult result2 = analysis2.getResult();

		for (int i = 0; i < inputs3.length; i++) {
			if (analysis3.train(inputs3[i]) && locked == -1)
				locked = i;
		}
		final TextAnalysisResult result3 = analysis3.getResult();

		assertEquals(result1.getSemanticType(), result2.getSemanticType());
		assertEquals(result1.getStructureSignature(), result2.getStructureSignature());
		assertNotEquals(result1.getDataSignature(), result2.getDataSignature());
		assertEquals(result1.getSemanticType(), "COORDINATE.LATITUDE_DECIMAL");
		assertEquals(result1.getDataSignature(), result3.getDataSignature());
		assertEquals(result1.getNullCount(), 0);
		assertEquals(result1.getSampleCount(), inputs1.length);
		assertEquals(result1.getConfidence(), 1.0);
		assertEquals(result1.getMatchCount(), inputs1.length);
		assertNull(result1.checkCounts());

		TestSupport.checkHistogram(result3, 10, true);
		TestSupport.checkQuantiles(result3);

		for (final String input : inputs1)
			assertTrue(input.matches(result1.getRegExp()), input);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void decimalSeparatorTest_Locale() throws IOException, FTAException {
		final int SAMPLE_SIZE = 1000;
		final Locale[] locales = { Locale.forLanguageTag("de-DE"), Locale.forLanguageTag("en-US") };

		for (final Locale locale : locales) {
			final TextAnalyzer analysis = new TextAnalyzer("DecimalSeparator");
			analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
			analysis.setLocale(locale);

			final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);

			final Set<String> samples = new HashSet<>();
			for (int i = 0; i < SAMPLE_SIZE; i++) {
				long l = random.nextInt(10000000);
				if (l % 2 == 0)
					l = -l;
				final String sample = String.valueOf(l) + formatSymbols.getDecimalSeparator() + random.nextInt(10);
				samples.add(sample);
				analysis.train(sample);
			}

			final TextAnalysisResult result = analysis.getResult();
			TestUtils.checkSerialization(analysis);

			assertEquals(result.getType(), FTAType.DOUBLE);
			assertEquals(result.getTypeModifier(), "SIGNED");
			assertNull(result.getSemanticType());
			assertEquals(result.getSampleCount(), SAMPLE_SIZE);
			assertEquals(result.getMatchCount(), SAMPLE_SIZE);
			assertEquals(result.getNullCount(), 0);
			assertEquals(result.getLeadingZeroCount(), 0);
			assertEquals(result.getDecimalSeparator(), formatSymbols.getDecimalSeparator());

			assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_SIGNED_DOUBLE));
			assertEquals(result.getConfidence(), 1.0);
			assertNull(result.checkCounts());

			for (final String sample : samples) {
				assertTrue(sample.matches(result.getRegExp()), sample + " " + result.getRegExp());
			}
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void monetaryDecimalSeparatorDefault() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Separator");
		final int SAMPLE_SIZE = 10000;
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		String minValue = String.valueOf(min);
		String maxValue = String.valueOf(max);
		final Set<String> samples = new HashSet<>();

		final NumberFormat nf = NumberFormat.getNumberInstance();
		final DecimalFormat decimalFormatter = (DecimalFormat)nf;
		decimalFormatter.applyPattern("#.##################E0");
		decimalFormatter.setMinimumFractionDigits(1);
		for (int i = 0; i < SAMPLE_SIZE; i++) {
			final double d = random.nextDouble();
			final String sample = decimalFormatter.format(d);
			if (d < min) {
				min = d;
				minValue = sample;
			}
			if (d > max) {
				max = d;
				maxValue = sample;
			}
			samples.add(sample);
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), SAMPLE_SIZE);
		assertEquals(result.getMatchCount(), SAMPLE_SIZE);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getMinValue(), minValue);
		assertEquals(result.getMaxValue(), maxValue);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_DOUBLE_WITH_EXPONENT));
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(analysis.getRegExp(KnownTypes.ID.ID_DOUBLE_WITH_EXPONENT)), sample);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void monetaryDecimalSeparatorFrench() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Separator");
		analysis.setLocale(Locale.FRENCH);
		final int SAMPLE_SIZE = 10000;
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		String minValue = String.valueOf(min);
		String maxValue = String.valueOf(max);
		final Set<String> samples = new HashSet<>();

		final NumberFormat doubleFormatter = NumberFormat.getNumberInstance(Locale.FRENCH);
		final DecimalFormat nf = (DecimalFormat)doubleFormatter;
		nf.applyPattern("#.##################E0");
		for (int i = 0; i < SAMPLE_SIZE; i++) {
			final double d = random.nextDouble() * 10000;
			final String sample = nf.format(d);
			if (d < min) {
				min = d;
				minValue = sample;
			}
			if (d > max) {
				max = d;
				maxValue = sample;
			}
			samples.add(sample);
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), SAMPLE_SIZE);
		assertEquals(result.getMatchCount(), SAMPLE_SIZE);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getMinValue(), minValue);
		assertEquals(result.getMaxValue(), maxValue);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_DOUBLE_WITH_EXPONENT));
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()), sample);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void doublesWithGroupingAndExponent() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("doublesWithGroupingAndExponent");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		analysis.setDebug(2);
		final String[] inputs = {
				"5,000,000E12", "2,000,000E11", "6,000,000E21", "1,000,000E12", "2,000,000E11", "6,000,000E4",
				"5,000,000E12", "2,000,000E11", "3,000,000E4", "7,000,000E12", "2,000,000E11", "6,000,000E4",
				"1,000,000E12", "2,000,000E11", "3,000,000E21", "5,000,000E12", "2,000,000E11", "3,000,000E2",
				"1,000,000E12", "2,000,000E11", "3,000,000E4", "7,000,000E12", "2,000,000E11", "6,000,000E2",
				"7,000,000E12", "2,000,000E11", "3,000,000E21", "5,000,000E12", "2,000,000E11", "3,000,000E4",
				"1,000,000E12", "2,000,000E11", "6,000,000E4", "1,000,000E12", "2,000,000E11", "6,000,000E2"
		};

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), "GROUPING");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_DOUBLE_WITH_EXPONENT_GROUPING));
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "3E8");
		assertEquals(result.getMaxValue(), "6E27");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void signedDoublesWithGroupingAndExponent() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("signedDoublesWithGroupingAndExponent");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		analysis.setDebug(2);
		final String[] inputs = {
				"-5,000,000E12", "2,000,000E11", "6,000,000E21", "1,000,000E12", "2,000,000E11", "6,000,000E4",
				"5,000,000E12", "-2,000,000E11", "3,000,000E4", "7,000,000E12", "2,000,000E11", "6,000,000E4",
				"1,000,000E12", "2,000,000E11", "-3,000,000E21", "-5,000,000E12", "-2,000,000E11", "3,000,000E2",
				"1,000,000E12", "2,000,000E11", "3,000,000E4", "7,000,000E12", "2,000,000E11", "6,000,000E2",
				"7,000,000E12", "2,000,000E11", "3,000,000E21", "5,000,000E12", "2,000,000E11", "3,000,000E-4",
				"1,000,000E12", "2,000,000E11", "6,000,000E4", "1,000,000E12", "-2,000,000E11", "-6,000,000E2"
		};

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), "SIGNED,GROUPING");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_SIGNED_DOUBLE_WITH_EXPONENT_GROUPING));
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "-3E27");
		assertEquals(result.getMaxValue(), "6E27");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void groupingWithPeriod() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("groupingWithPeriod");
		final Locale locale = Locale.forLanguageTag("pt-BR");
		analysis.setLocale(locale);

		final String[] inputs = {
				"46.448", "6.341.288", "543.022", "636.666", "61.606.330",
				"64.425", "109.089", "57.995", "4.773.826", "23.498.620",
				"43.391", "1.356.902", "22.039", "2.526.587", "33.113.104",
				"221.887", "6.313.005", "879.865", "84.369.774"
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
		assertEquals(result.getRegExp(), "[\\d\\.]{6,10}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "22.039");
		assertEquals(result.getMaxValue(), "84.369.774");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void longitudeGermanNonLocalized() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("LÄNGENGRAD");
		analysis.setDebug(2);
		// For German, Decimal Sep = ',' and Thousands Sep = '.'
		final Locale locale = Locale.forLanguageTag("de-DE");
		analysis.setLocale(locale);

		final String[] inputs = {
				"13.41036986", "13.41064075", "13.41052643", "13.4109571", "13.41033349",
				"13.410574", "13.4105445", "13.40978114", "13.41017903", "13.40948033",
				"13.41058769", "13.4105362", "13.4103485", "13.41049951", "13.41070876",
				"13.41005569", "13.41025133", "13.40963824", "13.4108598", "13.41032171",
				"13.41036725", "13.4101065", "13.41052288", "13.41310555",
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getSemanticType(), "COORDINATE.LONGITUDE_DECIMAL");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "([+-]?([0-9]|[0-9][0-9]|1[0-7][0-9])\\.\\d+)|[+-]?180\\.0+");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getDecimalSeparator(), '.');
		assertEquals(result.getMinValue(), "13.40948033");
		assertEquals(result.getMaxValue(), "13.41310555");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void longitudeGermanLocalized() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("LÄNGENGRAD");
		// For German, Decimal Sep = ',' and Thousands Sep = '.'
		final Locale locale = Locale.forLanguageTag("de-DE");
		analysis.setLocale(locale);

		final String[] inputs = {
				"13,41036986", "13,41064075", "13,41052643", "13,4109571", "13,41033349",
				"13,410574", "13,4105445", "13,40978114", "13,41017903", "13,40948033",
				"13,41058769", "13,4105362", "13,4103485", "13,41049951", "13,41070876",
				"13,41005569", "13,41025133", "13,40963824", "13,4108598", "13,41032171",
				"13,41036725", "13,4101065", "13,41052288", "13,41310555",
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getSemanticType(), "COORDINATE.LONGITUDE_DECIMAL");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "([+-]?([0-9]|[0-9][0-9]|1[0-7][0-9]),\\d+)|[+-]?180,0+");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "13,40948033");
		assertEquals(result.getMaxValue(), "13,41310555");
		assertEquals(result.getDecimalSeparator(), ',');
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

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
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			if (!nonLocalized.equals(input))
				assertTrue(input.matches(result.getRegExp()));
	}


	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void nonLocalizedPortugueseDouble() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("notLocalizedPortugueseDouble");
		analysis.setDebug(2);
		// For Portuguese, Decimal Sep = ',' and Thousands Sep = '.'
		final Locale locale = Locale.forLanguageTag("pt-BR");
		analysis.setLocale(locale);

		final String[] inputs = {
				"46.448", "6.341288", "543.022", "636.666", "61.606330",
				"64.425", "109.089", "57.995", "4773.826", "23.498620",
				"43.391", "1356.902", "22.039", "2526.587", "33.113104",
				"221.887", "6313.005", "879.865", "84.369774"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), "NON_LOCALIZED");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d*\\.?\\d+");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "6.341288");
		assertEquals(result.getMaxValue(), "6313.005");
		assertEquals(result.getDecimalSeparator(), '.');
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void nonLocalizedPortugueseDoubleSigned() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("notLocalizedPortugueseDoubleSigned");
		// For Portuguese, Decimal Sep = ',' and Thousands Sep = '.'
		final Locale locale = Locale.forLanguageTag("pt-BR");
		analysis.setLocale(locale);

		final String[] inputs = {
				"46.448", "6.341288", "543.022", "636.666", "61.606330",
				"64.425", "109.089", "57.995", "4773.826", "23.498620",
				"43.391", "-1356.902", "22.039", "2526.587", "33.113104",
				"221.887", "6313.005", "879.865", "84.369774"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), "SIGNED,NON_LOCALIZED");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "[+-]?\\d*\\.?\\d+");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "-1356.902");
		assertEquals(result.getMaxValue(), "6313.005");
		assertEquals(result.getDecimalSeparator(), '.');
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void nonLocalizedPortugueseWithDecSep() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("notLocalizedPortugueseWithDecSep");
		// For Portuguese, Decimal Sep = ',' and Thousands Sep = '.'
		final Locale locale = Locale.forLanguageTag("pt-BR");
		analysis.setLocale(locale);

		final String[] inputs = {
				"46.448", "6.341288", "543.022", "636.666", "61.606330",
				"64.425", "109.089", "57.995", "4773.826", "23.498620",
				"43.391", "-1356.902", "22.039", "2526.587", "33.113104",
				"221.887", "6313.005", "879.865", "84.369774", "43.075",
				"64.425", "109.089", "57.995", "4773.826", "23.498620",
				"76,87"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), "SIGNED,NON_LOCALIZED");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length - 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "[+-]?\\d*\\.?\\d+");
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
		assertEquals(result.getMinValue(), "-1356.902");
		assertEquals(result.getMaxValue(), "6313.005");
		assertEquals(result.getDecimalSeparator(), '.');
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void nonLocalizedFrenchDouble() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("notLocalizedFrenchDouble");
		// For French, Decimal Sep = ',' and Thousands Sep = ' '
		final Locale locale = Locale.forLanguageTag("fr-FR");
		analysis.setLocale(locale);

		final String[] inputs = {
				"46.448", "6.341288", "543.022", "636.666", "61.606330",
				"64.425", "109.089", "57.995", "4773.826", "23.498620",
				"43.391", "1356.902", "22.039", "2526.587", "33.113104",
				"221.887", "6313.005", "879.865", "84.369774"
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
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "6.341288");
		assertEquals(result.getMaxValue(), "6313.005");
		assertEquals(result.getDecimalSeparator(), '.');
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void nonLocalizedFrenchDoubleSigned() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("notLocalizedFrenchDoubleSigned");
		// For French, Decimal Sep = ',' and Thousands Sep = ' '
		final Locale locale = Locale.forLanguageTag("fr-FR");
		analysis.setLocale(locale);

		final String[] inputs = {
				"46.448", "6.341288", "543.022", "636.666", "61.606330",
				"64.425", "109.089", "57.995", "4773.826", "23.498620",
				"43.391", "-1356.902", "22.039", "2526.587", "33.113104",
				"221.887", "6313.005", "879.865", "84.369774"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), "SIGNED,NON_LOCALIZED");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "[+-]?\\d*\\.?\\d+");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "-1356.902");
		assertEquals(result.getMaxValue(), "6313.005");
		assertEquals(result.getDecimalSeparator(), '.');
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void nonLocalizedFrenchWithDecSep() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("notLocalizedFrenchWithDecSep");
		// For French, Decimal Sep = ',' and Thousands Sep = ' '
		final Locale locale = Locale.forLanguageTag("fr-FR");
		analysis.setLocale(locale);

		final String[] inputs = {
				"46.448", "6.341288", "543.022", "636.666", "61.606330",
				"64.425", "109.089", "57.995", "4773.826", "23.498620",
				"43.391", "-1356.902", "22.039", "2526.587", "33.113104",
				"221.887", "6313.005", "879.865", "84.369774", "43.075",
				"64.425", "109.089", "57.995", "4773.826", "23.498620",
				"76,87"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), "SIGNED,NON_LOCALIZED");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length - 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "[+-]?\\d*\\.?\\d+");
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
		assertEquals(result.getMinValue(), "-1356.902");
		assertEquals(result.getMaxValue(), "6313.005");
		assertEquals(result.getDecimalSeparator(), '.');
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void nonLocalizedFrenchWithThousandsSep() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("notLocalizedFrenchChange");
		// For French, Decimal Sep = ',' and Thousands Sep = ' '
		final Locale locale = Locale.forLanguageTag("fr-FR");
		analysis.setLocale(locale);

		final String[] inputs = {
				"46.448", "6.341288", "543.022", "636.666", "61.606330",
				"64.425", "109.089", "57.995", "4773.826", "23.498620",
				"43.391", "-1356.902", "22.039", "2526.587", "33.113104",
				"221.887", "6313.005", "879.865", "84.369774", "43.075",
				"64.425", "109.089", "57.995", "4773.826", "23.498620",
				"7 687"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), "SIGNED,NON_LOCALIZED");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length - 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "[+-]?\\d*\\.?\\d+");
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
		assertEquals(result.getMinValue(), "-1356.902");
		assertEquals(result.getMaxValue(), "6313.005");
		assertEquals(result.getDecimalSeparator(), '.');
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void doublesWithSpaces() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("doublesWithSpaces");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final String pipedInput =
				" 0.56000537| 0.4644182| 0.53597438| 0.66897142| 0.58498305| 0.53236401| 0.57459098| 0.66013932| 0.52850509| 0.59274352| 0.63449258|" +
						" 0.53062689| 0.62101597| 0.54467571| 0.55982822| 0.55236143| 0.52536035| -9999| 0.60300124| 0.56447577| 0.52936405| 0.529791|" +
						" 0.66758442| 0.48754925| 0.53641158| 0.51493853| 0.50437933| 0.59817135| 0.5593698| 0.57516289| 0.51756728| 0.55499005| 0.50622934|" +
						" 0.61930555| 0.61162853| 0.62606031| 0.61473697| 0.58204252| 0.54705042| 0.59241235| 0.56348866| 0.63005126| 0.55131644| 0.59083325|" +
						" 0.6401121| 0.56351823| 0.55042273| 0.61309111| 0.50556493| 0.62455976| 0.58180296| 0.65038371| 0.5766986| 0.60995877| 0.57004404|" +
						" 0.68236881| 0.49527445| 0.59514475| 0.56920892| 0.61952943| 0.56751066| 0.56773728| 0.57786775| 0.56001669| 0.49182054| 0.61677784|" +
						" 0.61103219| 0.57788509| 0.5439918| 0.62966329| 0.52707005| 0.69334954| 0.59485507| 0.54186255| 0.56574053| 0.54190195| 0.50607115|" +
						" 0.48150891| 0.50916439| 0.56314766| 0.56764281| 0.51959276| 0.53935015| 0.56686914| 0.57895356| 0.577981| 0.48543748| 0.62048388|" +
						" 0.58613783| 0.52769667| 0.60062158| 0.61381048| 0.53795183| 0.56081986| 0.64281517| 0.53652996| 0.59506911| 0.56369746| 0.58791381|" +
						" 0.5822112| 0.50465667| 0.51010394| 0.54944164| 0.47499654| 0.59955311| 0.60433054| 0.53369552| 0.59793103| -9999| 0.58600241|";

		final String inputs[] = pipedInput.split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), "SIGNED");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), KnownTypes.PATTERN_WHITESPACE + analysis.getRegExp(KnownTypes.ID.ID_SIGNED_DOUBLE));
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "-9999.0");
		assertEquals(result.getMaxValue(), "0.69334954");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void localeDoubleTest() throws IOException, FTAException {
		final int SAMPLE_SIZE = 1000;
		final Locale[] locales = DateFormat.getAvailableLocales();
//		Locale[] locales = new Locale[] { Locale.forLanguageTag("en"), Locale.forLanguageTag("es-CO") };
//		Locale[] locales = new Locale[] { Locale.forLanguageTag("bg") };

		for (final Locale locale : locales) {
			final TextAnalyzer analysis = new TextAnalyzer("Separator");
			analysis.setLocale(locale);

			if (isTooHard(locale))
				continue;

			final Set<String> samples = new LinkedHashSet<>();
			final NumberFormat nf = NumberFormat.getNumberInstance(locale);
			nf.setMinimumFractionDigits(2);

			try {
				for (int i = 0; i < SAMPLE_SIZE; i++) {
					final double d = random.nextDouble() * 100000000;
					final String sample = nf.format(d);
					samples.add(sample);
					analysis.train(sample);
				}
			}
			catch (FTAUnsupportedLocaleException e) {
				logger.debug("Skipping locale '{}' = reason: '{}'.", locale, e.getMessage());
				continue;
			}

			final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);
			final String grp = formatSymbols.getGroupingSeparator() == '.' ? "\\." : "" + formatSymbols.getGroupingSeparator();
			final String dec = formatSymbols.getDecimalSeparator() == '.' ? "\\." : "" + formatSymbols.getDecimalSeparator();
//			logger.debug("Locale: '{}', grp = '{}',  dec = '{}'.", locale, grp, dec);

			final TextAnalysisResult result = analysis.getResult();
			TestUtils.checkSerialization(analysis);

			if (result.getType() == FTAType.STRING) {
				for (final String s : samples)
					System.err.println(s);
			}
			assertEquals(result.getType(), FTAType.DOUBLE, locale.toLanguageTag());
			assertEquals(result.getTypeModifier(), "GROUPING");
			assertNull(result.getSemanticType());
			assertEquals(result.getSampleCount(), SAMPLE_SIZE);
			assertEquals(result.getMatchCount(), SAMPLE_SIZE);
			assertEquals(result.getNullCount(), 0);
			assertEquals(result.getLeadingZeroCount(), 0);

			final String regExp = "[\\d" + grp + "]*" + dec + "?" + "[\\d" + grp +"]+";

			assertEquals(result.getRegExp(), regExp);
			assertEquals(result.getConfidence(), 1.0);
			assertNull(result.checkCounts());

			for (final String sample : samples) {
				assertTrue(sample.matches(regExp), sample + " " + regExp);
			}
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void simpleDoubleDE_DE() throws IOException, FTAException {
		final String[] ugly = {
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "-14.000,00", "10.000,00", "1.000,00", "1.000,00", "1.000,00", "1.000,00", "0", "0",
				"0", "0", "0", "0", "0", "-4.000,00", "2.500,00", "1.500,00", "0", "-1.700,05",
				"1,61", "141,63", "1.556,81", "0", "0", "0", "-263.000,00", "60.000,00", "50.000,00", "8.000,00", "120.000,00",
				"25.000,00",
		};
		final Locale locale = Locale.forLanguageTag("de-DE");
		final TextAnalyzer analysis = new TextAnalyzer("Zugang/Abgang");
		analysis.setLocale(locale);

		for (final String sample : ugly)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();

		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE, locale.toLanguageTag());
		assertEquals(result.getTypeModifier(), "SIGNED,GROUPING");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), ugly.length);
		assertEquals(result.getMatchCount(), ugly.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		result.asJSON(false, 0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void doublePeriodIT() throws IOException, FTAException {
		final String[] ugly = {
				"3219,53", "3528,67", "5342,12", "3891,63",
				"5.373.99",
				"2107,85", "1039.69", "3250,63", "6678,75",
				"2540,35", "2500", "5041,7", "1626,89", "1881", "5427,42",
				"200", "910,45", "1931,32", "5059,16", "47214,8", "2770,97"
		};
		final Locale locale = Locale.forLanguageTag("it-IT");
		final TextAnalyzer analysis = new TextAnalyzer("Numero");
		analysis.setLocale(locale);
		analysis.setDebug(2);

		for (final String sample : ugly)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();

		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE, locale.toLanguageTag());

		result.asJSON(true, 1);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), ugly.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getInvalidCount(), 2);
		assertEquals(result.getMatchCount(), ugly.length - 2);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void doubleTrailingGroupingDE() throws IOException, FTAException {
		final String[] ugly = {
				"8.722,69-", "1.166.158,26 ", "107.283,55-", "6.410.732,62 ", "6.865,73-",
				"3.511.937,53 ", "1.403,91-", "319.310,77 ", "447.795,12-", "5.481.875,53 ",
				"218.302,97-", "3.275.636,11 ", "189.504,92-", "6.159.988,43 ", "1.264.751,91-",
				"7.729.399,20 ", "7.749,40-", "721.450,96 ", "11.482,18-", "690.129,01 ",
				"26.400,67-", "749.507,72 ", "9.731,44-", "572.007,50 ", "9.086,90-",
		};
		final Locale locale = Locale.forLanguageTag("de-DE");
		final TextAnalyzer analysis = new TextAnalyzer("Planung 2015");
		analysis.setLocale(locale);
		analysis.setDebug(2);

		for (final String sample : ugly)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();

		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE, locale.toLanguageTag());

		result.asJSON(true, 1);
		assertEquals(result.getTypeModifier(), "SIGNED_TRAILING,GROUPING");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), ugly.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getOutlierCount(), 0);
//BUG		assertEquals(result.getInvalidCount(), 0);
//BUG		assertEquals(result.getMatchCount(), ugly.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void recordDoubleDE_DE() throws IOException, FTAException {
		final String[] ugly = {
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "-14.000,00", "10.000,00", "1.000,00", "1.000,00", "1.000,00", "1.000,00", "0", "0",
				"0", "0", "0", "0", "0", "-4.000,00", "2.500,00", "1.500,00", "0", "-1.700,05",
				"1,61", "141,63", "1.556,81", "0", "0", "0", "-263.000,00", "60.000,00", "50.000,00", "8.000,00", "120.000,00",
				"25.000,00",
		};
		final Locale locale = Locale.forLanguageTag("de-DE");
		AnalyzerContext context = new AnalyzerContext(null, DateResolutionMode.Auto, "customer", new String[] { "Zugang/Abgang" } );
		TextAnalyzer template = new TextAnalyzer(context);
		template.setLocale(locale);

		RecordAnalyzer analysis = new RecordAnalyzer(template);

		for (final String sample : ugly)
			analysis.train(new String[] { sample });

		RecordAnalysisResult recordResult = analysis.getResult();

		final TextAnalysisResult result = recordResult.getStreamResults()[0];

		assertEquals(result.getType(), FTAType.DOUBLE, locale.toLanguageTag());
		assertEquals(result.getTypeModifier(), "SIGNED,GROUPING");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), ugly.length);
		assertEquals(result.getMatchCount(), ugly.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);

		result.asJSON(false, 0);

	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void negLongPosDoubles() throws IOException, FTAException {
		final String[] ugly = {
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "-14", "10.0", "100.00", "-900",
				"-600", "43.456", "40.301"
		};
		final TextAnalyzer analysis = new TextAnalyzer("negLongPosDoubles");

		for (final String sample : ugly)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();

		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), "SIGNED");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), ugly.length);
		assertEquals(result.getMatchCount(), ugly.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);

		result.asJSON(false, 0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void localeDoubleES_CO() throws IOException, FTAException {
		final String[] ugly = {
				"77.506.942,294", "55.466.183,606", "78.184.714,556", "52.225.004,254",
				"49.728.440,901", "46.654.635,41", "44.855.131,454", "74.523.230,406",
				"49.266.524,337", "21.683.364,918", "50.170.727,311", "45.015.038,753",
				"77.374.136,348", "14.954.505,431", "67.357.001,775", "81.430.862,119",
				"56.012.358,58", "49.427.706,653", "18.983.565,706", "76.804.973,122",
				"82.300.559,154", "40.007.535,851", "48.120.618,984", "25.215.331,00",
				"68.970.889,115", "98.530.458,063", "52.423.892,78", "51.938.286,39"
		};
		final Locale locale = Locale.forLanguageTag("es-CO");

		final TextAnalyzer analysis = new TextAnalyzer("Separator");
		analysis.setLocale(locale);

		for (final String sample : ugly)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE, locale.toLanguageTag());
		assertEquals(result.getTypeModifier(), "GROUPING");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), ugly.length);
		assertEquals(result.getMatchCount(), ugly.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);

		final String grp = formatSymbols.getGroupingSeparator() == '.' ? "\\." : "" + formatSymbols.getGroupingSeparator();
		final String dec = formatSymbols.getDecimalSeparator() == '.' ? "\\." : "" + formatSymbols.getDecimalSeparator();

		final String regExp = "[\\d" + grp + "]*" + dec + "?" + "[\\d" + grp +"]+";

		assertEquals(result.getRegExp(), regExp);
		assertEquals(result.getConfidence(), 1.0);

		for (final String sample : ugly) {
			assertTrue(sample.matches(regExp), sample + " " + regExp);
		}
	}

//	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void decimalSeparatorTest_Period() throws IOException, FTAException {
		final Set<String> failures = new HashSet<>();
		final int SAMPLE_SIZE = 1000;
		final Locale[] locales = DateFormat.getAvailableLocales();
//		Locale[] locales = new Locale[] { Locale.forLanguageTag("de-DE") };

		for (final Locale locale : locales) {
			long min = Long.MAX_VALUE;
			long absMin = Long.MAX_VALUE;
			long max = Long.MIN_VALUE;
			final TextAnalyzer analysis = new TextAnalyzer("DecimalSeparator");
			analysis.setLocale(locale);

			final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);

			final String grp = RegExpGenerator.slosh(formatSymbols.getGroupingSeparator());

			final boolean simple = NumberFormat.getNumberInstance(locale).format(0).matches("\\d");

			if (!simple) {
				logger.debug("Skipping locale '{}' as it does not use Arabic numerals.", locale);
				continue;
			}

			final Calendar cal = GregorianCalendar.getInstance(locale);
			if (!(cal instanceof GregorianCalendar)) {
				logger.debug("Skipping locale '{}' as it does not use the Gregorian calendar.", locale);
				continue;
			}

			logger.debug("Locale '{}', grouping: {}.", locale, grp);

			final Set<String> samples = new HashSet<>();
			for (int i = 0; i < SAMPLE_SIZE; i++) {
				long l = random.nextInt(10000000);
				if (l % 2 == 0)
					l = -l;
				final String sample = String.valueOf(l) + "." + random.nextInt(10);

				if (l < min) {
					min = l;
				}
				if (Math.abs(l) < absMin)
					absMin = Math.abs(l);
				if (l < min)
					min = l;
				if (l > max)
					max = l;

				samples.add(sample);
				analysis.train(sample);
			}

			final TextAnalysisResult result = analysis.getResult();
			TestUtils.checkSerialization(analysis);

			if (!result.getType().equals(FTAType.DOUBLE)) {
				failures.add("Locale: " + locale + ", type: '" + result.getType() + '"');
				continue;
			}
			assertEquals(result.getTypeModifier(), "SIGNED");
			assertNull(result.getSemanticType());
			assertEquals(result.getSampleCount(), SAMPLE_SIZE);
			assertEquals(result.getMatchCount(), SAMPLE_SIZE);
			assertEquals(result.getNullCount(), 0);
			assertEquals(result.getLeadingZeroCount(), 0);
			assertEquals(result.getDecimalSeparator(), '.');
			assertNull(result.checkCounts());

//			String regExp = "-?";
//			regExp += "[\\d" + Utils.slosh(formatSymbols.getGroupingSeparator()) + "]";
//			regExp += Utils.regExpLength(minValue.length(), maxValue.length());

//			System.err.println("Locale: " + locale + ", grp = '" + grp + "', dec = '" + dec + "', re: " + regExp + "'");

			String regExp = result.getRegExp();
			assertEquals(result.getConfidence(), 1.0);
			regExp = "-?(\\d+)?\\.\\d+";

			for (final String sample : samples) {
				if (!sample.matches(regExp)) {
					failures.add("Locale: " + locale + ", grouping: '" + grp + '"');
					break;
				}
			}
		}

		for (final String failure : failures)
			System.err.println(failure);

		assertEquals(failures.size(), 0);
	}

	public void _doublePerf(final boolean statisticsOn) throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("_doublePerfcd ");
		analysis.setLocale(Locale.FRENCH);
		if (!statisticsOn) {
			analysis.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);
			analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		}
		final long sampleCount = 100_000_000_000L;
		boolean saveOutput = false;
		BufferedWriter bw = null;
		final String[] samples = new String[10000];

		if (saveOutput)
			bw = new BufferedWriter(new FileWriter("/tmp/doublePerf.csv"));

		for (int i = 0; i < samples.length; i++) {
			samples[i] = String.valueOf(random.nextDouble() * 1000000);
			samples[i] = samples[i].replace('.', ',');
		}

		final long start = System.currentTimeMillis();

		long iters;
		// Run for about reasonable number of seconds
		final int seconds = 5;
		for (iters = 0; iters < sampleCount; iters++) {
			final String sample = samples[(int)(iters%samples.length)];
			analysis.train(sample);
			if (bw != null)
				bw.write(sample + '\n');
			if (iters%100 == 0 && System.currentTimeMillis()  - start >= seconds * 1_000)
				break;

		}
		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);
		if (bw != null)
			bw.close();

		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), iters + 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getMaxLength(), 18);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_DOUBLE));
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertNull(result.checkCounts());
		logger.info("Count {}, duration: {}ms, ~{} per second\n", iters + 1, System.currentTimeMillis() - start, (iters  + 1)/seconds);

		// With Statistics & LogicalTypes
		//   - Count 16248501, duration: 10008ms, ~1,624,850 per second
		// No Statistics & No LogicalTypes
		//   - Count 44222501, duration: 10003ms, ~4,422,250 per second
	}

	@Test(groups = { TestGroups.PERFORMANCE, TestGroups.DOUBLES })
	public void doublePerf() throws IOException, FTAException {
		_doublePerf(true);
	}

	@Test(groups = { TestGroups.PERFORMANCE, TestGroups.DOUBLES })
	public void doublePerfNoStatistics() throws IOException, FTAException {
		_doublePerf(false);
	}
}
