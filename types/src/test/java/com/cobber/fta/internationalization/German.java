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
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.security.SecureRandom;
import java.text.DateFormatSymbols;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.KnownTypes;
import com.cobber.fta.RecordAnalysisResult;
import com.cobber.fta.RecordAnalyzer;
import com.cobber.fta.Sample;
import com.cobber.fta.TestGroups;
import com.cobber.fta.TestSupport;
import com.cobber.fta.TestUtils;
import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.FTAUnsupportedLocaleException;
import com.cobber.fta.dates.DateTimeParser;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;
import com.cobber.fta.text.TextProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class German {

	private static final SecureRandom RANDOM = new SecureRandom();

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicGermanDate() throws IOException, FTAException {

		final Set<String> samples = new HashSet<>();
		LocalDate localDate = LocalDate.now();

		if (!TestUtils.isValidLocale("de_AT"))
			return;

		final Locale german = Locale.forLanguageTag("de-AT");

		final TextAnalyzer analysis = new TextAnalyzer("basicGermanDate");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		analysis.configure(TextAnalyzer.Feature.NO_ABBREVIATION_PUNCTUATION, false);
		analysis.setLocale(german);

		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy", german);

		int locked = -1;

		for (int i = 0; i < 100; i++) {
			final String sample = localDate.format(formatter);
			samples.add(sample);
			if (analysis.train(sample) && locked == -1)
				locked = i;
			localDate = localDate.minusDays(100);
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		// Post Java 8 the month abbreviations now appear with a period when necessary
		assertEquals(result.getRegExp(), TestUtils.getJavaVersion() == 8 ? "\\d{1,2} \\p{IsAlphabetic}{3} \\d{4}" : "\\d{1,2} [\\p{IsAlphabetic}\\.]{3,4} \\d{4}");
		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "d MMM yyyy");
		assertEquals(result.getSampleCount(), samples.size());
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), samples.size());
		assertEquals(result.getNullCount(), 0);
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		// Even the UNK match the RE
		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()), sample);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicGermanDatePassThree() throws IOException, FTAException {
		if (!TestUtils.isValidLocale("de_AT"))
			return;

		final String[] samplesOld = {
				"17.Jul.2003",	"21.Mai.2010", "03.Jul.2017", "15.Nov.2018",
				"23.Feb.2019", "16.Jun.2005", "07.Okt.2014", "12.Mai.2004",
				"17.Mär.2011",	"12.Aug.1998", "30.Mär.1997", "20.Sep.2002",
				"20.Dez.1996", "03.Mai.2021", "16.Aug.2001", "16.Apr.2009",
				"17.Mai.2007", "28.Feb.1999", "25.Jul.2009", "03.Jun.2019",
				"02.Feb.2004", "04.Mär.2002", "12.Jul.2000", "19.Jän.2018",
				"06.Feb.2007", "25.Dez.1999", "07.Jun.2022", "15.Okt.2020",
				"10.Feb.2010", "28.Sep.2008", "24.Feb.1996"
		};
		final String[] samplesNew = {
				"17.Juli.2003",	"21.Mai.2010", "03.Juli.2017", "15.Nov.2018",
				"23.Feb.2019", "16.Juni.2005", "07.Okt.2014", "12.Mai.2004",
				"17.März.2011",	"12.Aug.1998", "30.März.1997", "20.Sep.2002",
				"20.Dez.1996", "03.Mai.2021", "16.Aug.2001", "16.Apr.2009",
				"17.Mai.2007", "28.Feb.1999", "25.Juli.2009", "03.Juni.2019",
				"02.Feb.2004", "04.März.2002", "12.Juli.2000", "19.Jän.2018",
				"06.Feb.2007", "25.Dez.1999", "07.Juni.2022", "15.Okt.2020",
				"10.Feb.2010", "28.Sep.2008", "24.Feb.1996"
		};

		final String[] samples = TestUtils.getJavaVersion() == 8 ? samplesOld : samplesNew;
		final String expectedRE = TestUtils.getJavaVersion() == 8 ? "\\d{2}\\.\\p{IsAlphabetic}{3}\\.\\d{4}" : "\\d{2}\\.\\p{IsAlphabetic}{3,4}\\.\\d{4}";

		final Locale german = Locale.forLanguageTag("de-AT");

		final TextAnalyzer analysis = new TextAnalyzer("basicGermanDate");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		analysis.setLocale(german);

		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getTypeModifier(), "dd.MMM.yyyy", result.getTypeModifier());
		// Post Java 8 the month abbreviations now appear with a period when necessary
		assertEquals(result.getRegExp(), expectedRE);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), samples.length);
		assertEquals(result.getNullCount(), 0);
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		final DateTimeFormatter formatter = new DateTimeParser().withLocale(german).ofPattern(result.getTypeModifier());
		for (final String sample : samples) {
			assertTrue(sample.matches(result.getRegExp()), sample);
			try {
				LocalDate.parse(sample, formatter);
			}
			catch (DateTimeParseException e) {
				fail("Parse failed" + e);
			}
		}
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
		assertEquals(result.getRegExp(), "([+-]?([0-9]|[0-9][0-9]|1[0-7][0-9])([,\\.]\\d+)?)|[+-]?180[,\\.]0+");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getDecimalSeparator(), '.');
		assertEquals(result.getMinValue(), "13.40948033");
		assertEquals(result.getMaxValue(), "13.41310555");
		assertNull(result.checkCounts(false));

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
		assertEquals(result.getRegExp(), "([+-]?([0-9]|[0-9][0-9]|1[0-7][0-9])([,\\.]\\d+)?)|[+-]?180[,\\.]0+");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "13,40948033");
		assertEquals(result.getMaxValue(), "13,41310555");
		assertEquals(result.getDecimalSeparator(), ',');
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
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
		assertEquals(result.getInvalidCount(), 0);
		assertEquals(result.getMatchCount(), ugly.length);
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
		final AnalyzerContext context = new AnalyzerContext(null, DateResolutionMode.Auto, "customer", new String[] { "Zugang/Abgang" } );
		final TextAnalyzer template = new TextAnalyzer(context);
		template.setLocale(locale);

		final RecordAnalyzer analysis = new RecordAnalyzer(template);

		for (final String sample : ugly)
			analysis.train(new String[] { sample });

		final RecordAnalysisResult recordResult = analysis.getResult();

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

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void localizedGermanLong() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("localizedGermanLong");
		final Locale locale = Locale.forLanguageTag("de-DE");
		analysis.setLocale(locale);

		final String[] inputs = {
	            "1.234.567.890.123", "-1234567890123", "   51.000", "1.000.000.000.000",
	            "+11.123.000", "     ",  "  ", "  -12000   "
		};

		for (final String input : inputs)
			analysis.train(input);
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTypeModifier(), "SIGNED,GROUPING");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length + 1);
		assertEquals(result.getBlankCount(), 2);
		assertEquals(result.getMatchCount(), inputs.length - result.getBlankCount());
		assertEquals(result.getNullCount(), 1);
		assertEquals(result.getRegExp(), "[ 	]*[+-]?[\\d\\.]{5,17}[ 	]*");
		assertEquals(result.getMinValue(), "-1.234.567.890.123");
		assertEquals(result.getMaxValue(), "1.234.567.890.123");
		assertEquals(result.getCardinality(), 6);
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			if (input == null || input.trim().isEmpty())
				continue;
			assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TEXT })
	public void german1() throws IOException, FTAException {
		final TextProcessor processor = new TextProcessor(Locale.GERMAN);

		final TextProcessor.TextResult result = processor.analyze("Adventistische Neue Mittelschule (Grünauerstraße 20)");

		assertEquals(result.getDetermination(), TextProcessor.Determination.OK);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicGenderDE() throws IOException, FTAException {
		final String[] inputs = {
				"Female", "MALE", "Male", "Female", "Female", "MALE", "Female", "Female", "Unknown", "Male",
				"Male", "Female", "Male", "Male", "Male", "Female", "Female", "Male", "Male", "Male",
				"Female", "Male", "Female", "FEMALE", "Male", "Female", "male", "Male", "Male", "male",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "Gender", Locale.forLanguageTag("de-AT"), null, FTAType.STRING, 1.0);
		assertEquals(result.getRegExp(), "(?i)(FEMALE|MALE|UNKNOWN)");
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void VAT_AT() throws IOException, FTAException {
		final String[] inputs = {
				"ATU11111116", "ATU22222226", "ATU33333336", "ATU44444446", "ATU55555553",
				"ATU66666663", "ATU77777773", "ATU88888883", "ATU99999993", "ATU12345675",
				"ATU00000024", "ATU00000033", "ATU00000042", "ATU00000060", "ATU00000079",
				"ATU00000088", "ATU00000104", "ATU00000113", "ATU00000122", "ATU00000140",
				"ATU00000159", "ATU00000168", "ATU00000186", "ATU00000195", "ATU00000202",
				"ATU00000202", "ATU10223006", "ATU12011204", "ATU15110001",
				"ATU15394605", "ATU15416707", "ATU15662209", "ATU16370905", "ATU23224909",
				"ATU25775505", "ATU28560205", "ATU28609707", "ATU28617100", "ATU29288909",
				"ATU37675002", "ATU37785508", "ATU37830200", "ATU38420507", "ATU38516405",
				"ATU39364503", "ATU42527002", "ATU43666001", "ATU43716207", "ATU45766309",
		};

		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "UID", Locale.forLanguageTag("de-AT"), "IDENTITY.VAT_<COUNTRY>", FTAType.STRING, 1.0);

		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicZipHeaderDE() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Postleitzahl");
		analysis.setLocale(Locale.forLanguageTag("de-AT"));
		final String inputs[] = {
			"", "", "", "", "", "", "", "", "", "27215", "75251", "66045", "", "",
			"", "", "", "", "94087", "", "", "", "", "", "", "", "", "", "", ""
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.LONG);
//		assertEquals(result.getTypeQualifier(), "POSTAL_CODE.POSTAL_CODE_DE");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), 4);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getRegExp(), "\\d{5}");
		assertEquals(result.getConfidence(), 1.0);

		assertNull(result.checkCounts(false));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicMonthAbbrGerman() throws IOException, FTAException {

		if (!TestUtils.isValidLocale("de"))
			return;

		final Locale german = Locale.forLanguageTag("de");

		final DateFormatSymbols dfs = new DateFormatSymbols(german);
		final String[] m = dfs.getShortMonths();
		final GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(german);
		final long actualMonths = cal.getActualMaximum(Calendar.MONTH);

		final TextAnalyzer analysis = new TextAnalyzer("basicMonthAbbrGerman");
		analysis.setLocale(german);
		analysis.configure(TextAnalyzer.Feature.NO_ABBREVIATION_PUNCTUATION, false);

		final int badCount = 4;
		final int iterations = 10;
		int bads = 0;

		for (int i = 0; i < iterations; i++) {
			for (int j = 0; j < actualMonths; j++)
				analysis.train(m[j]);
			if (bads < badCount) {
				analysis.train("UNKN");
				bads++;
			}
		}

		final TextAnalysisResult result = analysis.getResult();

		final int javaVersion = TestUtils.getJavaVersion();
		String expected = "untested";
		switch (javaVersion) {
		case 8:
			expected = "\\p{IsAlphabetic}{3}";
			break;
		case 11:
			expected = "[\\p{IsAlphabetic}\\.]{3,4}";
			break;
		default:
			// This is the correct answer for at least 17, 18, 21, 22
			expected = "[\\p{IsAlphabetic}\\.]{3,5}";
			break;
		}
		assertEquals(result.getRegExp(), expected);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getSemanticType(), "MONTH.ABBR_de");
		assertEquals(result.getSampleCount(), iterations * actualMonths + badCount);
		assertEquals(result.getOutlierCount(), 0);
		final Map<String, Long> invalids = result.getInvalidDetails();
		assertEquals(invalids.size(), 1);
		assertEquals(invalids.get("UNKN"), 4L);
		assertEquals(result.getMatchCount(), iterations * actualMonths);
		assertEquals(result.getNullCount(), 0);
		assertTrue((double)analysis.getPluginThreshold()/100 < result.getConfidence());
		assertEquals(result.getConfidence(), 1 - (double)badCount/result.getSampleCount());

		assertNull(result.checkCounts(false));
	}

	@Test(groups = { TestGroups.ALL })
	public void testLatitudeUnsigned_deDE() throws IOException, FTAException {
		final String[] samples = {
				"51,5", "39,195", "46,18806", "36,1333333", "33,52056", "39,79", "40,69361", "36,34333", "32,0666667", "48,8833333", "40,71417",
				"51,45", "29,42389", "43,69556", "40,03222", "53,6772222", "45,4166667", "17,3833333", "51,52721", "40,76083", "53,5", "51,8630556",
				"26,1666667", "32,64", "62,9", "29,61944", "40,71417", "51,52721", "40,61278", "37,22667", "40,71417", "25,77389",
				"46,2333333", "40,65", "52,3333333", "38,96861", "27,1666667", "33,44833", "29,76306", "43,77222", "43,77222", "34,33806",
				"56,0333333", "41,54278", "29,76306", "26,46111", "51,4", "55,6666667", "33,92417", "53,4247222", "26,12194", "37,8166667"
		};

		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(samples), "Latitude", Locale.GERMAN, "COORDINATE.LATITUDE_DECIMAL", FTAType.DOUBLE, 1.0);
		assertEquals(result.getMatchCount(), samples.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void issue71() throws FTAPluginException, FTAUnsupportedLocaleException {
		final String[] headers = { "First", "Last", "MI" };
		final String[][] names = { { "Anaïs", "Nin", "9,876.54" }, { "Gertrude", "Stein", "3,876.2" },
				{ "Paul", "Campbell", "76.54" }, { "Pablo", "Picasso", "123.45" } };

		final AnalyzerContext context = new AnalyzerContext(null, DateResolutionMode.Auto, "customer", headers);
		final TextAnalyzer template = new TextAnalyzer(context);
		template.setDebug(2);

		template.setLocale(Locale.GERMAN);

		final RecordAnalyzer analysis = new RecordAnalyzer(template);

		for (final String[] name : names)
			analysis.train(name);

		final RecordAnalysisResult recordResult = analysis.getResult();

		final TextAnalysisResult[] results = recordResult.getStreamResults();
		assertEquals(results[0].getSemanticType(), "NAME.FIRST");
		assertNull(results[2].getSemanticType());
		assertEquals(results[2].getType(), FTAType.DOUBLE);
		assertEquals(results[2].getTypeModifier(), "NON_LOCALIZED");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void checkJSON() throws IOException, FTAException {
		// Issue #120
		final String[] headers = { "First", "Last", "MI" };
		final String[][] names = { { "Anaïs", "Nin", "9,876.54" }, { "Gertrude", "Stein", "3,876.2" },
				{ "Paul", "Campbell", "76.54" }, { "Pablo", "Picasso", "123.45" } };

		final AnalyzerContext context = new AnalyzerContext(null, DateResolutionMode.Auto, "customer", headers);
		final TextAnalyzer template = new TextAnalyzer(context);

		template.setLocale(Locale.GERMAN);

		final RecordAnalyzer analysis = new RecordAnalyzer(template);

		for (final String[] name : names)
			analysis.train(name);

		final RecordAnalysisResult recordResult = analysis.getResult();
		final TextAnalysisResult[] results = recordResult.getStreamResults();

		final String jsonRepresentation = results[0].asJSON(false, 0);

		final ObjectMapper objectMapper = new ObjectMapper();
		final JsonNode jsonNode = objectMapper.readTree(jsonRepresentation);

		// Validate that the JSON representation has all the fields we expect - with the values we expect
		assertTrue(jsonNode.get("isSemanticType").asBoolean());
		assertEquals(jsonNode.get("semanticType").asText(), "NAME.FIRST");

		assertEquals(jsonNode.get("fieldName").asText(), "First");
		assertEquals(jsonNode.get("sampleCount").asInt(), 4);
		assertEquals(jsonNode.get("matchCount").asInt(), 4);
		assertEquals(jsonNode.get("nullCount").asInt(), 0);
		assertEquals(jsonNode.get("blankCount").asInt(), 0);
		assertEquals(jsonNode.get("distinctCount").asInt(), 4);
		assertEquals(jsonNode.get("confidence").asDouble(), 1.0);
		assertEquals(jsonNode.get("type").asText(), "String");
		assertEquals(jsonNode.get("min").asText(), "Anaïs");
		assertEquals(jsonNode.get("max").asText(), "Paul");
		assertEquals(jsonNode.get("minLength").asInt(), 4);
		assertEquals(jsonNode.get("maxLength").asInt(), 8);
		assertEquals(jsonNode.get("min").asText(), "Anaïs");
		assertEquals(jsonNode.get("cardinality").asInt(), 4);
		assertEquals(jsonNode.get("outlierCardinality").asInt(), 0);
		assertEquals(jsonNode.get("invalidCardinality").asInt(), 0);
		assertEquals(jsonNode.get("shapesCardinality").asInt(), 3);
		assertFalse(jsonNode.get("leadingWhiteSpace").asBoolean());
		assertFalse(jsonNode.get("trailingWhiteSpace").asBoolean());
		assertFalse(jsonNode.get("multiline").asBoolean());
		assertEquals(jsonNode.get("keyConfidence").asDouble(), 0.0);
		assertEquals(jsonNode.get("uniqueness").asDouble(), 1.0);
		assertEquals(jsonNode.get("detectionLocale").asText(), "de");
		assertEquals(jsonNode.get("structureSignature").asText(), "slggsAEDZ26rz9dqs15eNF23j2w=");
		assertEquals(jsonNode.get("dataSignature").asText(), "hOm2Ez8xHWr6iDeQ1j/A3hBtz0Y=");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void deColorDetection() throws IOException, FTAPluginException, FTAException {
		final String[] inputs = {
			"ROT", "BLAU", "GRÜN", "WEISS", "SCHWARZ", "ORANGE", "ROSA", "GRAU",
			"BRAUN", "VIOLETT", "GELB", "BEIGE", "CREME", "MARINEBLAU", "BORDEAUX",
			"TÜRKIS", "INDIGO", "SILBER", "GOLD", "BRONZE", "ROT", "BLAU", "GRÜN"
		};
		for (final String header : new String[] { "farbe", "Farbe", "Haarfarbe" }) {
			final TextAnalyzer analysis = new TextAnalyzer(header);
			analysis.setLocale(Locale.forLanguageTag("de-DE"));
			for (final String s : inputs)
				analysis.train(s);
			assertEquals(analysis.getResult().getSemanticType(), "COLOR.TEXT_DE", "header: " + header);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void deColorNoHeader() throws IOException, FTAPluginException, FTAException {
		final String[] inputs = {
			"ROT", "BLAU", "GRÜN", "WEISS", "SCHWARZ", "ORANGE", "ROSA", "GRAU",
			"BRAUN", "VIOLETT", "GELB", "BEIGE", "CREME", "MARINEBLAU", "BORDEAUX"
		};
		final TextAnalyzer analysis = new TextAnalyzer("col");
		analysis.setLocale(Locale.forLanguageTag("de-DE"));
		for (final String s : inputs)
			analysis.train(s);
		assertNotEquals(analysis.getResult().getSemanticType(), "COLOR.TEXT_DE");
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
			final long randomLong = RANDOM.nextInt(Integer.MAX_VALUE) + 1000000000L;
			if (randomLong >  9999999999L)
				continue;
			final String sample = Long.toString(randomLong) + "." + RANDOM.nextInt(10);
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
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
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
				long l = RANDOM.nextInt(10000000);
				if (l % 2 == 0)
					l = -l;
				final String sample = String.valueOf(l) + formatSymbols.getDecimalSeparator() + RANDOM.nextInt(10);
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
			assertNull(result.checkCounts(false));

			for (final String sample : samples) {
				assertTrue(sample.matches(result.getRegExp()), sample + " " + result.getRegExp());
			}
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void continentDE() throws IOException, FTAException {
		final String[] inputs = {
				"AFRIKA", "ASIEN", "EUROPA", "NORDAMERIKA", "SÜDAMERIKA", "OZEANIEN", "ANTARKTIS",
				"EUROPA", "ASIEN", "AFRIKA", "NORDAMERIKA", "ASIEN", "EUROPA", "AFRIKA", "OZEANIEN",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "kontinent", Locale.forLanguageTag("de-DE"), "CONTINENT.TEXT_DE", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void trickLatitude() throws IOException, FTAException {
		final String[] inputs = {
				"54.176658700787", "54.1523286823181", "54.1507845291159", "54.1444646959388", "54.0948626983874",
				"54.099612908786", "54.0928342952505", "54.1492128935414", "54.0996275412016", "54.1767338631483",
		};

		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "latitude", Locale.forLanguageTag("de-DE"), "COORDINATE.LATITUDE_DECIMAL", FTAType.DOUBLE, 1.0);
		assertEquals(result.getRegExp(), "-?([0-9]|[0-8][0-9])([,\\.]\\d+)?|-?90[,\\.]0+");
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void languageDE() throws IOException, FTAException {
		final String[] inputs = {
				"DEUTSCH", "ENGLISCH", "FRANZÖSISCH", "SPANISCH", "ITALIENISCH", "PORTUGIESISCH", "RUSSISCH",
				"CHINESISCH", "JAPANISCH", "ARABISCH", "HINDI", "KOREANISCH", "TÜRKISCH", "POLNISCH", "SCHWEDISCH",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "sprache", Locale.forLanguageTag("de-DE"), "LANGUAGE.TEXT_DE", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}
}
