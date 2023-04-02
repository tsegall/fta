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
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.FTAUnsupportedLocaleException;
import com.cobber.fta.dates.DateTimeParser;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;
import com.cobber.fta.dates.LocaleInfo;
import com.cobber.fta.dates.SimpleDateMatcher;

public class TestDates {
	private Logger logger = LoggerFactory.getLogger("com.cobber.fta");

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void dateOutlier() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("dateOutlier");

		final String[] inputs = "12/12/12|12/12/32|02/22/02".split("\\|");
		int locked = -1;
		final int records = 100;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		for (int i = inputs.length; i < records; i++) {
			if (analysis.train("02/02/99") && locked == -1)
				locked = i;
		}

		// This record is bad 'O' not '0'
		analysis.train("02/O2/99");

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), records + 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}/\\d{2}/\\d{2}");
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
		assertEquals(result.getOutlierCount(), 1);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "MM/dd/yy");
		assertEquals(result.getMinValue(), "02/22/02");
		assertEquals(result.getMaxValue(), "02/02/99");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicAMPMVietname() throws IOException, FTAException {
		final Locale locale = Locale.forLanguageTag("vi-VN");
		final TextAnalyzer analysis = new TextAnalyzer("basicAMPMVietname");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		analysis.setLocale(locale);
		final String dateTimeFormat = "dd/MMM/yy h:mm a";
		final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateTimeFormat, locale);
		final int sampleCount = 100;
		final Set<String> samples = new HashSet<>();
		int locked = -1;

		LocalDateTime localDateTime = LocalDateTime.now();
		for (int i = 0; i < sampleCount; i++) {
			final String sample = localDateTime.format(dtf);
			samples.add(sample);
			if (analysis.train(sample) && locked == -1)
				locked = i;
			localDateTime = localDateTime.minusDays(100).minusHours(1).minusMinutes(1).minusSeconds(1);
		}
		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), sampleCount);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), sampleCount);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}/[\\p{IsAlphabetic}\\p{IsDigit} ]{5,6}/\\d{2} \\d{1,2}:\\d{2} (?i)(SA|CH)");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "dd/MMM/yy h:mm a");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()), sample);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void simpleAMPM() throws IOException, FTAException {
		final Locale locale = Locale.forLanguageTag("en-US");
		final TextAnalyzer analysis = new TextAnalyzer("simpleAMPM");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		analysis.setLocale(locale);
		final String dateTimeFormat = "MM/dd/yy h:mm:ss aaa";
		final SimpleDateFormat sdf = new SimpleDateFormat(dateTimeFormat, locale);
		final int sampleCount = 100;
		final Set<String> samples = new HashSet<>();
		int locked = -1;

		final Calendar calendar = Calendar.getInstance();
		for (int i = 0; i < sampleCount; i++) {
			final String sample = sdf.format(calendar.getTime());
			samples.add(sample);
			if (analysis.train(sample) && locked == -1)
				locked = i;
			calendar.add(Calendar.HOUR, -1000);
			calendar.add(Calendar.MINUTE, 1);
			calendar.add(Calendar.SECOND, 1);
		}
		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), sampleCount);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), sampleCount);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}/\\d{2}/\\d{2} \\d{1,2}:\\d{2}:\\d{2} (?i)(AM|PM)");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "MM/dd/yy h:mm:ss a");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()), sample);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicAMPM_enUS() throws IOException, FTAException {
		final Locale locale = Locale.forLanguageTag("en-US");
		final TextAnalyzer analysis = new TextAnalyzer("funnychars___<>:\"/\\|?*___");

		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		analysis.setLocale(locale);
		analysis.setTrace("enabled=true");
		final String dateTimeFormat = "dd/MMM/yy h:mm a";
		final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateTimeFormat, locale);
		final int sampleCount = 100;
		final Set<String> samples = new HashSet<>();
		int locked = -1;

		LocalDateTime localDateTime = LocalDateTime.now();
		for (int i = 0; i < sampleCount; i++) {
			final String sample = localDateTime.format(dtf);
			samples.add(sample);
			if (analysis.train(sample) && locked == -1)
				locked = i;
			localDateTime = localDateTime.minusDays(100).minusHours(1).minusMinutes(1).minusSeconds(1);
		}
		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), sampleCount);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), sampleCount);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}/\\p{IsAlphabetic}{3}/\\d{2} \\d{1,2}:\\d{2} (?i)(AM|PM)");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "dd/MMM/yy h:mm a");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()), sample);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicAMPM_enUSNotSimple() throws IOException, FTAException {
		final Locale locale = Locale.forLanguageTag("en-US");
		final DateResolutionMode[] cases = { DateResolutionMode.DayFirst, DateResolutionMode.Auto };

		for (final DateResolutionMode resolutionMode : cases) {
			final TextAnalyzer analysis = new TextAnalyzer("h:mm a dd/MM/yy", resolutionMode);
			analysis.setTrace("enabled=true");
			analysis.setLocale(locale);
			final String dateTimeFormat = "h:mm a dd/MM/yy";
			final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateTimeFormat, locale);
			final int sampleCount = 100;
			final Set<String> samples = new HashSet<>();
			int locked = -1;

			LocalDateTime localDateTime = LocalDateTime.now();
			for (int i = 0; i < sampleCount; i++) {
				final String sample = localDateTime.format(dtf);
				samples.add(sample);
				if (analysis.train(sample) && locked == -1)
					locked = i;
				localDateTime = localDateTime.minusDays(100).minusHours(1).minusMinutes(1).minusSeconds(1);
			}
			final TextAnalysisResult result = analysis.getResult();
			TestUtils.checkSerialization(analysis);

			assertEquals(result.getSampleCount(), sampleCount);
			assertEquals(result.getRegExp(), "\\d{1,2}:\\d{2} (?i)(AM|PM) \\d{2}/\\d{2}/\\d{2}");
			assertEquals(result.getType(), FTAType.LOCALDATETIME);
			assertEquals(result.getTypeModifier(), "h:mm a dd/MM/yy");
			assertEquals(result.getMatchCount(), sampleCount);
			assertEquals(result.getOutlierCount(), 0);
			assertEquals(result.getNullCount(), 0);
			assertEquals(result.getConfidence(), 1.0);
			assertNull(result.checkCounts());

			TestSupport.checkHistogram(result, 10, true);
			TestSupport.checkQuantiles(result);

			for (final String sample : samples)
				assertTrue(sample.matches(result.getRegExp()), sample);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicAMPM_viVNNotSimple() throws IOException, FTAException {
		final Locale locale = Locale.forLanguageTag("vi-VN");
		final TextAnalyzer analysis = new TextAnalyzer("h:mm a dd/MM/yy", DateResolutionMode.DayFirst);
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		analysis.setLocale(locale);
		final String dateTimeFormat = "h:mm a dd/MM/yy";
		final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateTimeFormat, locale);
		final int sampleCount = 100;
		final Set<String> samples = new HashSet<>();
		int locked = -1;

		LocalDateTime localDateTime = LocalDateTime.now();
		for (int i = 0; i < sampleCount; i++) {
			final String sample = localDateTime.format(dtf);
			samples.add(sample);
			if (analysis.train(sample) && locked == -1)
				locked = i;
			localDateTime = localDateTime.minusDays(100).minusHours(1).minusMinutes(1).minusSeconds(1);
		}
		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), sampleCount);
		assertEquals(result.getRegExp(), "\\d{1,2}:\\d{2} (?i)(SA|CH) \\d{2}/\\d{2}/\\d{2}");
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "h:mm a dd/MM/yy");
		assertEquals(result.getMatchCount(), sampleCount);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()), sample);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicAMPMBug() throws IOException, FTAException {
		final Locale locale = Locale.forLanguageTag("en-US");
		final TextAnalyzer analysis = new TextAnalyzer("basicAMPMBug");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		analysis.setLocale(locale);
		final String dateTimeFormat = "h:mm 'a'";
		final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateTimeFormat, locale);
		final int sampleCount = 100;
		final Set<String> samples = new HashSet<>();
		int locked = -1;

		LocalDateTime localDateTime = LocalDateTime.now();
		for (int i = 0; i < sampleCount; i++) {
			final String sample = localDateTime.format(dtf);
			samples.add(sample);
			if (analysis.train(sample) && locked == -1)
				locked = i;
			localDateTime = localDateTime.minusDays(100).minusHours(1).minusMinutes(1).minusSeconds(1);
		}
		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertEquals(result.getSampleCount(), sampleCount);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), sampleCount);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d:\\d{2} \\p{IsAlphabetic}|\\d{2}:\\d{2} \\p{IsAlphabetic}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()), sample);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicHHmmddMyy() throws IOException, FTAException {
		final DateResolutionMode[] cases = { DateResolutionMode.DayFirst, DateResolutionMode.Auto };

		final Locale[] locales = { Locale.forLanguageTag("en-US"), Locale.forLanguageTag("en-GB") };
		for (final Locale locale : locales) {
			for (final DateResolutionMode resolutionMode : cases) {
				final TextAnalyzer analysis = new TextAnalyzer("TransactionDate", resolutionMode);
				analysis.setLocale(locale);
				final String pipedInput = "00:53 15/2/17|17:53 29/7/16|10:53 11/1/16|03:53 25/6/15|20:53 06/12/14|13:53 20/5/14|06:53 01/11/13|23:53 14/4/13|" +
						"16:53 26/9/12|09:53 10/3/12|02:53 23/8/11|19:53 03/2/11|12:53 18/7/10|05:53 30/12/09|22:53 12/6/09|15:53 24/11/08|" +
						"08:53 08/5/08|01:53 21/10/07|18:53 03/4/07|11:53 15/9/06|04:53 27/2/06|21:53 10/8/05|14:53 22/1/05|07:53 06/7/04|" +
						"00:53 19/12/03|17:53 01/6/03|10:53 13/11/02|03:53 27/4/02|20:53 08/10/01|13:53 22/3/01|";
				final String inputs[] = pipedInput.split("\\|");
				int locked = -1;

				for (int i = 0; i < inputs.length; i++) {
					if (analysis.train(inputs[i]) && locked == -1)
						locked = i;
				}

				final TextAnalysisResult result = analysis.getResult();
				TestUtils.checkSerialization(analysis);

				assertEquals(result.getType(), FTAType.LOCALDATETIME);
				assertEquals(result.getTypeModifier(), "HH:mm dd/M/yy", resolutionMode.toString() + "+" + locale);
				assertEquals(result.getSampleCount(), inputs.length);
				assertEquals(result.getMatchCount(), inputs.length);
				assertEquals(result.getNullCount(), 0);
				assertEquals(result.getRegExp(), "\\d{2}:\\d{2} \\d{2}/\\d{1,2}/\\d{2}");
				assertEquals(result.getConfidence(), 1.0);
				assertNull(result.checkCounts());

				TestSupport.checkHistogram(result, 10, true);
				TestSupport.checkQuantiles(result);

				for (final String input : inputs)
					assertTrue(input.matches(result.getRegExp()));
			}
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicResolutionMode() throws IOException, FTAException {
		final DateResolutionMode[] cases = { DateResolutionMode.DayFirst, DateResolutionMode.MonthFirst, DateResolutionMode.Auto };

		final Locale[] locales = { Locale.forLanguageTag("en-US"), Locale.forLanguageTag("en-GB") };
		for (final Locale locale : locales) {
			for (final DateResolutionMode resolutionMode : cases) {
				final TextAnalyzer analysis = new TextAnalyzer("TransactionDate", resolutionMode);
				analysis.setLocale(locale);
				final String pipedInput = "2/2/34|3/3/19|4/4/48|5/5/55|6/6/66|7/7/77|8/8/88|9/9/99|" +
							"12/12/34|4/5/19|6/4/48|7/5/55|12/6/66|12/7/77|3/8/88|2/9/99|" +
							"1/1/26|4/5/33|6/9/48|9/5/55|12/2/66|11/11/78|3/4/98|2/3/39|";
				final String inputs[] = pipedInput.split("\\|");
				int locked = -1;

				for (int i = 0; i < inputs.length; i++) {
					if (analysis.train(inputs[i]) && locked == -1)
						locked = i;
				}

				final TextAnalysisResult result = analysis.getResult();
				TestUtils.checkSerialization(analysis);

				assertEquals(result.getType(), FTAType.LOCALDATE);
				String expected;
				if (resolutionMode == DateResolutionMode.DayFirst)
					expected = "d/M/yy";
				else if (resolutionMode == DateResolutionMode.MonthFirst)
					expected = "M/d/yy";
				else
					expected = locale.toLanguageTag().equals("en-GB") ? "d/M/yy" : "M/d/yy";
				assertEquals(result.getTypeModifier(), expected);
				assertEquals(result.getSampleCount(), inputs.length);
				assertEquals(result.getMatchCount(), inputs.length);
				assertEquals(result.getNullCount(), 0);
				assertEquals(result.getRegExp(), "\\d{1,2}/\\d{1,2}/\\d{2}");
				assertEquals(result.getConfidence(), 1.0);

				for (final String input : inputs) {
					assertTrue(input.matches(result.getRegExp()));
				}
			}
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicHHmmddMyyUnresolved() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("TransactionDate");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String pipedInput = "00:53 15/2/17|17:53 29/7/16|10:53 11/1/16|03:53 25/6/15|20:53 06/12/14|13:53 20/5/14|06:53 01/11/13|23:53 14/4/13|" +
				"16:53 26/9/12|09:53 10/3/12|02:53 23/8/11|19:53 03/2/11|12:53 18/7/10|05:53 30/12/09|22:53 12/6/09|15:53 24/11/08|" +
				"08:53 08/5/08|01:53 21/10/07|18:53 03/4/07|11:53 15/9/06|04:53 27/2/06|21:53 10/8/05|14:53 22/1/05|07:53 06/7/04|" +
				"00:53 19/12/03|17:53 01/6/03|10:53 13/11/02|03:53 27/4/02|20:53 08/10/01|13:53 22/3/01|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}:\\d{2} \\d{2}/\\d{1,2}/\\d{2}");
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "HH:mm ??/M/??");

		// Since we do not have a valid Type Qualifier - we cannot assert anything about types matches or confidence
		//		assertEquals(result.getMatchCount(), 0);
		//		assertEquals(result.getConfidence(), 0.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicHHmmddMyyFalse() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("TransactionDate", DateResolutionMode.MonthFirst);
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String pipedInput = "00:53 15/2/17|17:53 29/7/16|10:53 11/1/16|03:53 25/6/15|20:53 06/12/14|13:53 20/5/14|06:53 01/11/13|23:53 14/4/13|" +
				"16:53 26/9/12|09:53 10/3/12|02:53 23/8/11|19:53 03/2/11|12:53 18/7/10|05:53 30/12/09|22:53 12/6/09|15:53 24/11/08|" +
				"08:53 08/5/08|01:53 21/10/07|18:53 03/4/07|11:53 15/9/06|04:53 27/2/06|21:53 10/8/05|14:53 22/1/05|07:53 06/7/04|" +
				"00:53 19/12/03|17:53 01/6/03|10:53 13/11/02|03:53 27/4/02|20:53 08/10/01|13:53 22/3/01|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getTypeModifier(), "HH:mm dd/M/yy");
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}:\\d{2} \\d{2}/\\d{1,2}/\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void fiscalYear() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Fiscal Year");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final String[] inputs = {
				"2014", "2004", "2005", " 2006", " 2008", " 2009", " 2010", " 2012", " 2013", " 2011", " 2007" };

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getTypeModifier(), "yyyy");
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "[ 	]*\\d{4}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertNull(result.checkCounts());

		final int width = 10;
		TestSupport.dumpRaw(result.getHistogram(width));
		TestSupport.checkHistogram(result, width, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicMYYYY() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("CCEXPIRES", DateResolutionMode.Auto);
		analysis.setDebug(1);
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String pipedInput = "12/2025|6/2026|9/2026|9/2023|4/2023|2/2023|3/2023|3/2023|3/2026|7/2025|12/2024|4/2026|12/2023|2/2023|9/2024|8/2026|2/2025|2/2022|9/2024|7/2022|" +
				"11/2025|11/2022|2/2023|9/2024|10/2026|3/2022|8/2023|10/2026|3/2023|8/2026|9/2022|3/2024|10/2023|7/2025|5/2022|11/2026|12/2024|8/2022|10/2023|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getTypeModifier(), "M/yyyy");
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2}/\\d{4}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void fixedWidthDay() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("TransactionDate", DateResolutionMode.MonthFirst);
		analysis.setLocale(Locale.US);
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String pipedInput = "Oct  1 2019 12:14AM|Jan  5 2020 12:31AM|Feb  2 2020 11:20AM|Mar  8 2020 10:20AM|Mar 15 2020 10:20AM|Mar 22 2020 10:20AM|Mar 29 2020 10:20AM|Jan 12 2020 12:12AM|Jan 19 2020 12:12AM|Jan 26 2020 12:12AM|Jun  1 2020 11:11AM|Jun  8 2020 11:11AM|Jun 15 2020 11:11AM|Jun 22 2020 11:11AM|Jun 29 2020 11:11AM|Oct  1 2019 12:14AM|Jan  5 2020 12:31AM|Feb  2 2020 11:20AM|Mar  8 2020 10:20AM|Mar 15 2020 10:20AM|Mar 22 2020 10:20AM|Mar 29 2020 10:20AM|Jan 12 2020 12:12AM|Jan 19 2020 12:12AM|Jan 26 2020 12:12AM|Jun  1 2020 11:11AM|Jun  8 2020 11:11AM|Jun 15 2020 11:11AM|Jun 22 2020 11:11AM|Jun 29 2020 11:11AM|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getTypeModifier(), "MMM ppd yyyy hh:mma");
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{3} [ \\d]\\d \\d{4} \\d{2}:\\d{2}(?i)(AM|PM)");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		final DateTimeFormatter validator = DateTimeFormatter.ofPattern(result.getTypeModifier(), Locale.US);
		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
			validator.parse(input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void withComma() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("TransactionDate", DateResolutionMode.MonthFirst);
		analysis.setLocale(Locale.US);
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String pipedInput = "Jul 21, 2020 9:00 AM|Jul 21, 2020 9:00 AM|Jul 23, 2020 5:00 PM|Jun 03, 2020 3:00 PM|Jun 09, 2020 10:30 AM|Jun 17, 2020 4:00 PM|Jun 23, 2020 3:00 PM|Jun 25, 2020 2:00 PM|Jun 30, 2020 2:30 PM|Jun 30, 2020 8:00 AM|May 19, 2020 9:00 AM|May 19, 2020 9:00 AM|May 28, 2020 2:00 PM|Jul 23, 2020 5:00 PM|Jun 03, 2020 3:00 PM|Jun 09, 2020 10:30 AM|Jun 17, 2020 4:00 PM|Jun 23, 2020 3:00 PM|Jun 25, 2020 2:00 PM|Jun 30, 2020 2:30 PM|Jun 30, 2020 8:00 AM|May 19, 2020 9:00 AM|May 19, 2020 9:00 AM|May 28, 2020 2:00 PM|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getTypeModifier(), "MMM dd, yyyy h:mm a");
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{3} \\d{2}, \\d{4} \\d{1,2}:\\d{2} (?i)(AM|PM)");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		final DateTimeFormatter validator = DateTimeFormatter.ofPattern(result.getTypeModifier(), Locale.US);
		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
			validator.parse(input);
		}
	}


	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void fixedWidthHour() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("TransactionDate", DateResolutionMode.MonthFirst);
		analysis.setLocale(Locale.US);
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String pipedInput =
		"Oct 1 2019  2:14AM|Oct 1 2019 10:08AM|Oct 9 2019 12:49PM|Oct 9 2019  2:52PM|Oct 9 2019  6:10PM|" +
		"Oct 1 2019 12:53PM|Oct 1 2019  1:53PM|Oct 1 2019  6:00PM|Oct 1 2019  6:56PM|Oct 2 2019  9:02PM|" +
		"Oct 2 2019 12:13AM|Oct 2 2019 12:45PM|Oct 2 2019  4:23PM|Oct 2 2019  4:51PM|Oct 2 2019  5:11PM|" +
		"Oct 1 2019  2:14AM|Oct 1 2019 10:08AM|Oct 9 2019 12:49PM|Oct 9 2019  2:52PM|Oct 9 2019  6:10PM|" +
		"Oct 19 2019  6:16PM|Oct 10 2019  4:40AM|Oct 10 2019  9:35AM|Oct 10 2019 11:08AM|Oct 10 2019 12:24PM|" +
		"Oct 10 2019  1:45PM|Oct 11 2019  9:57AM|Oct 11 2019 12:42PM|Oct 14 2019 10:41AM|Oct 14 2019  1:43PM|" +
		"Oct 24 2019 10:47AM|Oct 24 2019 10:59AM|Oct 31 2019  6:02PM|Oct 1 2019 10:10AM|Oct 11 2019 11:02AM|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getTypeModifier(), "MMM d yyyy pph:mma");
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{3} \\d{1,2} \\d{4} [ \\d]\\d:\\d{2}(?i)(AM|PM)");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		final DateTimeFormatter validator = DateTimeFormatter.ofPattern(result.getTypeModifier(), Locale.US);
		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
			validator.parse(input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void fixedWidthDayHour() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("TimeSent", DateResolutionMode.MonthFirst);
		analysis.setLocale(Locale.US);
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String pipedInput =
				"Oct  1 2019  2:14AM|Oct  1 2019 10:08AM|Oct  9 2019 12:49PM|Oct  9 2019  2:52PM|Oct  9 2019  6:10PM|" +
				"Oct  1 2019 12:53PM|Oct  1 2019  1:53PM|Oct  1 2019  6:00PM|Oct  1 2019  6:56PM|Oct  2 2019  9:02PM|" +
				"Oct  2 2019 12:13AM|Oct  2 2019 12:45PM|Oct  2 2019  4:23PM|Oct  2 2019  4:51PM|Oct  2 2019  5:11PM|" +
				"Oct  1 2019  2:14AM|Oct  1 2019 10:08AM|Oct  9 2019 12:49PM|Oct  9 2019  2:52PM|Oct  9 2019  6:10PM|" +
				"Oct 19 2019  6:16PM|Oct 10 2019  4:40AM|Oct 10 2019  9:35AM|Oct 10 2019 11:08AM|Oct 10 2019 12:24PM|" +
				"Oct 10 2019  1:45PM|Oct 11 2019  9:57AM|Oct 11 2019 12:42PM|Oct 14 2019 10:41AM|Oct 14 2019  1:43PM|" +
				"Oct 24 2019 10:47AM|Oct 24 2019 10:59AM|Oct 31 2019  6:02PM|Oct  1 2019 10:10AM|Oct 11 2019 11:02AM|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getTypeModifier(), "MMM ppd yyyy pph:mma");
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{3} [ \\d]\\d \\d{4} [ \\d]\\d:\\d{2}(?i)(AM|PM)");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		final DateTimeFormatter validator = DateTimeFormatter.ofPattern(result.getTypeModifier(), Locale.US);
		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
			validator.parse(input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void variableHour() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("TimeSent", DateResolutionMode.MonthFirst);
		analysis.setLocale(Locale.US);
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String pipedInput =
				"Oct 1 2019 2:14AM|Oct 1 2019 10:08AM|Oct 9 2019 12:49PM|Oct 9 2019 2:52PM|Oct 9 2019 6:10PM|" +
				"Oct 1 2019 12:53PM|Oct 1 2019 1:53PM|Oct 1 2019 6:00PM|Oct 1 2019 6:56PM|Oct 2 2019 9:02PM|" +
				"Oct 2 2019 12:13AM|Oct 2 2019 12:45PM|Oct 2 2019 4:23PM|Oct 2 2019 4:51PM|Oct 2 2019 5:11PM|" +
				"Oct 1 2019 2:14AM|Oct 1 2019 10:08AM|Oct 9 2019 12:49PM|Oct 9 2019 2:52PM|Oct 9 2019 6:10PM|" +
				"Oct 19 2019 6:16PM|Oct 10 2019 4:40AM|Oct 10 2019 9:35AM|Oct 10 2019 11:08AM|Oct 10 2019 12:24PM|" +
				"Oct 10 2019 1:45PM|Oct 11 2019 9:57AM|Oct 11 2019 12:42PM|Oct 14 2019 10:41AM|Oct 14 2019 1:43PM|" +
				"Oct 24 2019 10:47AM|Oct 24 2019 10:59AM|Oct 31 2019 6:02PM|Oct 1 2019 10:10AM|Oct 11 2019 11:02AM|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getTypeModifier(), "MMM d yyyy h:mma");
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{3} \\d{1,2} \\d{4} \\d{1,2}:\\d{2}(?i)(AM|PM)");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		final DateTimeFormatter validator = DateTimeFormatter.ofPattern(result.getTypeModifier(), Locale.US);
		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
			validator.parse(input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void mixedDateandDateTime() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("TimeSent", DateResolutionMode.MonthFirst);
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String pipedInput =
				"3/11/2020|12/4/2020|6/14/2020|4/21/2020|3/6/2020|5/8/2020|4/7/2020|12/8/2020|8/12/2020|9/12/2020|" +
				"3/13/2020|12/11/2020|7/24/2020|6/4/2020|6/29/2020|7/5/2020|4/30/2020|" +
				"8/10/2020 0:00|9/13/2020 0:00|" +
				"10/6/2020|3/1/2020|12/14/2020|4/29/2020|11/24/2020|7/21/2020|1/18/2020|6/7/2020|1/25/2020|2/12/2020|10/31/2020|" +
				"1/20/2021|2/3/2020|12/15/2020|3/3/2020|4/4/2020|5/25/2020|7/20/2020|11/21/2020|9/1/2020|3/18/2020|6/17/2020|12/26/2020|" +
				"5/27/2020 0:00|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getTypeModifier(), "M/d/yyyy");
		assertEquals(result.getMatchCount(), inputs.length - 3);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2}/\\d{1,2}/\\d{4}");
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void fixedSSS() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("TransactionDate", DateResolutionMode.MonthFirst);
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String pipedInput = "2019-10-01 07:26:47.617|2019-10-01 12:12:31.587|2019-10-01 12:12:32.827|2019-10-01 12:12:33.840|2019-10-01 12:12:34.770|2019-10-01 12:12:35.830|2019-10-01 12:12:36.690|2019-10-01 12:12:37.590|2019-10-01 12:12:38.357|2019-10-01 12:12:40.117|2019-10-01 12:12:41.087|2019-10-02 07:34:35.987|2019-10-02 07:34:40.680|2019-10-02 07:34:41.293|2019-10-02 07:34:43.640|2019-10-02 07:34:44.910|2019-10-02 07:34:45.907|2019-10-02 07:34:46.987|2019-10-02 07:34:47.867|2019-10-02 07:34:48.820|2019-10-02 07:34:49.930|2019-10-02 07:34:50.747|2019-10-02 07:34:51.923|2019-10-02 07:34:53.763|2019-10-02 07:34:54.090|2019-10-02 07:34:55.340|2019-10-02 07:34:56.500|2019-10-02 07:34:58.330|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getTypeModifier(), "yyyy-MM-dd HH:mm:ss.SSS");
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void twentyRecords() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("FROM_DATE", DateResolutionMode.MonthFirst);
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String pipedInput = "1/3/11|1/3/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|9/17/08|1-15-2011|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getTypeModifier(), "M/d/yy");
		assertEquals(result.getMatchCount(), inputs.length - 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2}/\\d{1,2}/\\d{2}");
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertNull(result.checkCounts());

		int matches = 0;
		for (final String input : inputs) {
			if (input.matches(result.getRegExp()))
				matches++;
		}
		assertEquals(matches, result.getMatchCount());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void dMyy() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("TransactionDate", DateResolutionMode.DayFirst);
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String pipedInput = "1/2/09 6:17|1/2/09 4:53|1/2/09 13:08|1/3/09 14:44|1/4/09 12:56|1/4/09 13:19|1/4/09 20:11|1/2/09 20:09|1/4/09 13:17|1/4/09 14:11|" +
				"1/5/09 2:42|1/5/09 5:39|1/2/09 9:16|1/5/09 10:08|1/2/09 14:18|1/4/09 1:05|1/5/09 11:37|1/6/09 5:02|1/6/09 7:45|1/2/09 7:35|" +
				"1/6/09 12:56|1/1/09 11:05|1/5/09 4:10|1/6/09 7:18|1/2/09 1:11|1/1/09 2:24|1/7/09 8:08|1/2/09 2:57|1/1/09 20:21|1/8/09 0:42|" +
				"1/8/09 3:56|1/8/09 3:16|1/8/09 1:59|1/3/09 9:03|1/5/09 13:17|1/6/09 7:46|1/5/09 20:00|1/8/09 16:24|1/9/09 6:39|1/6/09 22:19|" +
				"1/6/09 23:00|1/7/09 7:44|1/3/09 13:24|1/7/09 15:12|1/7/09 20:15|1/3/09 10:11|1/9/09 15:58|1/3/09 13:11|1/10/09 12:57|1/10/09 14:43|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2}/\\d{1,2}/\\d{2} \\d{1,2}:\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "d/M/yy H:mm");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicMdyy() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Account_Created", DateResolutionMode.DayFirst);
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String pipedInput = "1/2/09 6:00|1/2/09 4:42|1/1/09 16:21|9/25/05 21:13|11/15/08 15:47|9/24/08 15:19|1/3/09 9:38|1/2/09 17:43|1/4/09 13:03|6/3/08 4:22|" +
				"1/5/09 2:23|1/5/09 4:55|1/2/09 8:32|11/11/08 15:53|12/9/08 12:07|1/4/09 0:00|1/5/09 9:35|1/6/09 2:41|1/6/09 7:00|12/30/08 5:44|" +
				"1/6/09 10:58|12/10/07 12:37|1/5/09 2:33|1/6/09 7:07|12/31/08 2:48|1/1/09 1:56|1/7/09 7:39|1/3/08 7:23|10/24/08 6:48|1/8/09 0:28|" +
				"1/8/09 3:33|1/8/09 3:06|11/28/07 11:56|1/3/09 8:47|1/5/09 12:45|1/6/09 7:30|12/10/08 19:53|1/8/09 15:57|1/9/09 5:09|1/6/09 12:00|" +
				"1/6/09 12:00|1/7/09 5:35|1/3/09 12:47|1/5/09 19:55|1/7/09 17:17|1/3/09 9:27|1/9/09 14:53|10/31/08 0:31|2/7/07 20:16|1/9/09 11:38|" +
				"7/1/08 12:53|1/2/09 17:51|1/8/09 3:06|1/7/09 9:04|1/1/09 0:58|1/11/09 0:33|1/11/09 13:39|1/10/09 21:17|2/23/06 11:56|1/3/09 17:17|" +
				"1/4/09 23:45|12/30/08 10:21|1/12/09 3:12|11/29/07 9:23|12/1/08 6:25|1/12/09 4:45|1/8/09 14:56|1/11/09 9:02|11/23/08 19:30|1/2/09 21:04|" +
				"1/5/09 5:55|1/8/09 20:21|1/6/09 14:38|1/11/09 12:38|9/20/08 20:53|1/12/09 13:16|9/4/08 9:26|10/15/08 5:31|1/13/09 5:17|1/8/09 12:47|" +
				"1/1/09 12:00|1/3/09 12:04|1/1/09 20:11|2/20/08 22:45|1/12/09 2:48|9/1/08 3:39|3/13/06 4:56|1/13/09 11:15|12/5/07 11:37|1/14/09 4:34|" +
				"12/24/08 17:31|1/3/09 12:30|10/21/05 21:49|12/10/08 16:41|1/6/09 14:59|1/13/09 23:13|12/16/08 22:19|1/7/08 21:16|1/14/09 10:44|1/6/09 22:00|" +
				"1/25/08 15:46|1/15/09 2:04|1/10/09 12:50|12/26/08 9:01|12/10/08 9:14|1/10/09 0:07|1/15/09 4:02|7/24/08 15:48|8/21/07 21:19|1/14/09 8:25|" +
				"1/14/09 22:21|7/21/08 12:03|12/16/02 4:09|1/14/09 9:16|1/14/09 5:59|1/7/09 8:39|1/15/09 12:19|1/13/09 17:42|1/13/09 19:37|6/5/06 22:42|" +
				"1/12/09 1:26|11/3/08 14:28|1/15/09 12:22|6/30/08 9:33|12/31/08 16:26|1/16/09 17:01|1/5/09 3:19|1/2/09 0:10|1/12/09 1:40|1/13/09 12:30|" +
				"11/9/08 5:53|11/11/05 11:49|1/16/06 14:45|5/1/08 8:26|1/7/09 12:31|10/29/08 13:09|9/13/07 14:20|1/4/09 6:51|1/17/09 15:22|9/14/05 22:53|" +
				"1/18/09 4:28|1/18/09 4:44|12/14/05 15:46|6/22/08 14:10|12/6/08 1:34|12/31/05 0:55|1/16/09 14:26|1/16/09 19:32|10/28/05 14:57|12/28/07 13:39|" +
				"1/18/09 12:20|1/18/09 0:00|1/18/09 0:00|1/3/06 6:09|1/5/09 16:27|4/12/06 6:14|1/4/09 22:41|1/18/09 16:23|1/18/09 16:58|1/18/09 17:16|" +
				"1/18/09 17:34|12/31/08 19:48|1/13/09 16:44|1/4/09 21:33|1/12/09 14:07|1/6/09 12:04|11/25/08 7:56|1/18/09 15:18|1/3/09 11:06|12/29/08 10:38|" +
				"11/7/07 9:49|5/25/07 10:58|1/3/09 12:39|9/25/05 6:25|1/11/09 18:17|1/12/09 13:31|12/13/08 19:35|12/13/08 19:35|12/9/05 17:19|1/19/09 15:31|" +
				"1/19/09 16:18|1/10/09 16:32|1/11/09 12:00|1/15/09 0:21|1/19/09 22:53|8/27/08 8:22|7/15/08 21:04|1/20/09 3:02|4/18/05 9:27|1/20/09 5:13|" +
				"1/20/09 5:13|12/28/08 20:10|1/20/09 4:23|1/19/09 10:24|1/18/09 0:00|1/7/09 5:30|10/10/08 1:26|3/12/06 11:31|1/20/09 12:17|11/12/08 3:34|" +
				"7/11/07 11:18|9/29/03 17:08|12/10/08 21:49|1/4/09 18:38|2/4/07 10:09|1/7/09 18:15|1/15/09 18:01|11/1/08 16:32|8/5/07 18:52|1/5/09 14:06|" +
				"1/9/09 19:15|1/5/09 15:42|1/4/09 22:22|12/23/08 12:02|1/21/09 12:00|5/3/08 4:14|1/19/09 16:52|10/27/05 19:22|11/1/08 23:14|1/2/08 9:52|" +
				"2/28/05 5:40|1/21/09 10:23|1/7/09 13:23|12/30/08 17:48|12/28/07 15:28|1/18/09 16:25|1/21/09 11:39|1/22/09 4:32|1/10/09 14:55|1/22/09 7:55|" +
				"8/7/08 9:24|9/17/05 3:32|1/4/09 11:32|1/8/09 2:50|6/19/08 10:48|1/14/09 4:10|7/23/08 14:59|12/13/08 19:20|1/5/09 15:00|4/18/06 13:26|" +
				"1/23/09 3:00|1/6/09 3:38|1/23/09 3:27|12/20/08 8:41|12/29/08 3:16|1/21/09 13:56|7/30/07 21:10|1/4/09 12:39|1/22/09 9:55|1/22/09 23:23|" +
				"4/25/07 5:08|1/16/09 1:38|8/3/07 2:48|4/7/08 17:15|1/23/09 6:32|1/5/09 15:43|1/24/09 8:02|1/14/09 4:13|11/28/07 10:05|1/23/09 10:42|" +
				"1/19/09 14:43|3/7/06 5:47|11/24/08 15:50|12/17/07 19:55|12/7/05 19:48|6/20/08 22:08|6/14/07 13:14|6/14/07 13:14|1/17/06 8:51|5/14/07 12:48|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "M/d/yy H:mm");
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2}/\\d{1,2}/\\d{2} \\d{1,2}:\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicDateNumber() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Trade Date", DateResolutionMode.DayFirst);
		final String pipedInput = "20120202|20120607|20120627|20120627|20120627|20120627|20120627|20120628|20120312|20120201|" +
		"20111031|20120229|20120104|20120312|20120312|20120628|20120628|20120628|20120628|20120628|" +
		"20111027|20120213|20120628|20120227|20120313|20120701|20120702|20120702|20120701|20120701|" +
		"20120629|20120629|20120629|20120629|20120629|20120629|20120629|20120629|20120629|20120629|" +
		"20120629|20120702|20120702|20120702|20120702|20120702|20120702|20120702|20120702|20120702|" +
		"20120702|20120702|20120702|20120702|20120713|20120713|20120713|20120713|20120713|20120713|" +
		"20120430|20120523|20120627|20120627|20120606|20120703|20120718|20120718|20120703|20120703|" +
		"20120523|20120627|20120627|20120703|20120503|20120718|20120926|20120523|20120626|20120713|" +
		"20120713|20120626|20120626|20121004|20120702|20120702|20120702|20120702|20120702|20120702|" +
		"20120702|20120702|20120702|20120702|20120702|20120702|19591209|20120702|20120702|20120702|" +
		"20120702|20120702|20120702|20120702|20120702|20120702|20120702|20120516|20120518|20120521|" +
		"20120522|20120523|20120523|20120524|20120524|20120525|20120525|20120525|20120528|20120529|" +
		"20120529|20120531|20120601|20120601|20120605|20120606|20120608|20120611|20120613|20120618|" +
		"20120620|20120625|20120625|20120702|20120702|20120702|20120702|20120629|20120628|20120702|" +
		"20120629|20120629|20120518|20120702|20120702|20120702|20120702|20120702|20120702|20120702|" +
		"20120702|20120702|20120702|20120629|20120629|20120702|20120629|20120629|20120629|20120702|" +
		"20120702|20120702|20120629|20120629|20120702|20120629|20120626|20120702|20120702|20120702|" +
		"20120702|20120702|20120702|20120702|20120702|20120702|20120702|20120702|20120702|20120702|" +
		"20120702|20120702|20120702|20120702|20120702|20120702|20120629|20120702|20180519|20120702|";

		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "yyyyMMdd");
		assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{8}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "19591209");
		assertEquals(result.getMaxValue(), "20180519");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(result.getTypeModifier());
		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()), input);
			try {
				LocalDate.parse(input, formatter);
			}
			catch (DateTimeParseException e) {
				fail("Parse failed" + e);
			}
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicddMMyyHHmm() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("ddMMyy_HHmm", DateResolutionMode.DayFirst);
		final String pipedInput =
				"23/08/17 03:49|23/08/17 03:49|14/08/17 10:49|23/08/17 03:49|14/08/17 10:49|05/08/17 17:49|23/08/17 03:49|14/08/17 10:49|05/08/17 17:49|" +
				"28/07/17 00:49|23/08/17 03:49|14/08/17 10:49|05/08/17 17:49|28/07/17 00:49|19/07/17 07:49|23/08/17 03:49|14/08/17 10:49|05/08/17 17:49|" +
				"28/07/17 00:49|19/07/17 07:49|10/07/17 14:49|23/08/17 03:49|14/08/17 10:49|05/08/17 17:49|28/07/17 00:49|19/07/17 07:49|10/07/17 14:49|" +
				"01/07/17 21:49|23/08/17 03:49|14/08/17 10:49|05/08/17 17:49|28/07/17 00:49|19/07/17 07:49|10/07/17 14:49|01/07/17 21:49|23/06/17 04:49|" +
				"23/08/17 03:49|14/08/17 10:49|05/08/17 17:49|28/07/17 00:49|19/07/17 07:49|10/07/17 14:49|01/07/17 21:49|23/06/17 04:49|14/06/17 11:49|" +
				"23/08/17 03:49|14/08/17 10:49|05/08/17 17:49|28/07/17 00:49|19/07/17 07:49|10/07/17 14:49|01/07/17 21:49|23/06/17 04:49|14/06/17 11:49|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "dd/MM/yy HH:mm");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}/\\d{2}/\\d{2} \\d{2}:\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "14/06/17 11:49");
		assertEquals(result.getMaxValue(), "23/08/17 03:49");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void mixed_yyyyddMM() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("yyyyddMM", DateResolutionMode.DayFirst);
		final String pipedInput = "1970/06/06|1971/01/06|1972/07/07|1973/03/03|1974/04/04|1970/05/05|1970/06/06|1970/08/08|1970/09/09|1970/10/10|1970/06/06|1971/01/06|1972/07/07|1973/03/03|1974/04/04|1970/05/05|1970/06/06|1970/08/08|1970/09/09|1970/10/10|2011/31/02|2017/31/12|2016/20/10|1999/15/07|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "yyyy/dd/MM");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{4}/\\d{2}/\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "1970/05/05");
		assertEquals(result.getMaxValue(), "2017/31/12");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void mixed_ddMMyyyy() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("ddMMyyyy", DateResolutionMode.DayFirst);
		final String pipedInput = "06/06/1970|01/06/1971|07/07/1972|03/03/1973|04/04/1974|05/05/1970|06/06/1970|08/08/1970|09/09/1970|10/10/1970|06/06/1970|01/06/1971|07/07/1972|03/03/1973|04/04/1974|05/05/1970|06/06/1970|08/08/1970|09/09/1970|10/10/1970|31/02/2011|31/12/2017|20/10/2016|15/07/1999|";

		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "dd/MM/yyyy");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}/\\d{2}/\\d{4}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "05/05/1970");
		assertEquals(result.getMaxValue(), "31/12/2017");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicHMM() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("H:mm");
		final String pipedInput = "3:16|3:16|10:16|3:16|10:16|17:16|3:16|10:16|17:16|0:16|3:16|10:16|17:16|0:16|7:16|3:16|10:16|" +
		"17:16|0:16|7:16|14:16|3:16|10:16|17:16|0:16|7:16|14:16|21:16|3:16|10:16|17:16|0:16|7:16|14:16|" +
		"21:16|4:16|3:16|10:16|17:16|0:16|7:16|14:16|21:16|4:16|11:16|3:16|10:16|17:16|0:16|7:16|14:16|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALTIME);
		assertEquals(result.getTypeModifier(), "H:mm");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2}:\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "0:16");
		assertEquals(result.getMaxValue(), "21:16");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicDateDDMMMYYYHHMM() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicDateDDMMMYYYHHMM");
		final String pipedInput =
				"1/30/06 22:01|1/30/06 22:15|1/30/06 22:25|1/30/06 22:35|1/30/06 22:40|1/30/06 22:45|1/30/06 22:47|1/30/06 23:00|1/30/06 23:00|1/30/06 23:11|" +
						"1/30/06 23:15|1/30/06 23:21|1/30/06 23:31|1/30/06 23:52|1/30/06 23:55|1/30/06 23:58|1/31/06 0:00|1/31/06 0:00|1/31/06 0:00|1/31/06 0:01|" +
						"1/31/06 0:01|1/31/06 0:01|1/31/06 0:01|1/31/06 0:01|1/31/06 0:01|1/31/06 0:01|1/31/06 0:01|1/31/06 0:17|1/31/06 0:26|1/31/06 0:30|" +
						"1/31/06 0:30|1/31/06 0:30|1/31/06 0:47|1/31/06 0:56|1/31/06 1:21|1/31/06 1:34|1/31/06 1:49|1/31/06 2:00|1/31/06 2:08|1/31/06 2:11|1/31/06 2:22|" +
						"1/31/06 2:48|1/31/06 3:05|1/31/06 3:05|1/31/06 3:30|";
		final String[] inputs = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "M/dd/yy H:mm");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2}/\\d{2}/\\d{2} \\d{1,2}:\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "1/30/06 22:01");
		assertEquals(result.getMaxValue(), "1/31/06 3:30");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void slashLoop() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("thin", DateResolutionMode.MonthFirst);
		final String input = "1/1/06 0:00";
		final int iterations = 30;
		int locked = -1;

		for (int iters = 0; iters < iterations; iters++) {
			if (analysis.train(input) && locked == -1)
				locked = iters;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "M/d/yy H:mm");
		assertEquals(result.getSampleCount(), iterations);
		assertEquals(result.getDistinctCount(), 1);
		assertEquals(result.getMatchCount(), iterations);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2}/\\d{1,2}/\\d{2} \\d{1,2}:\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "1/1/06 0:00");
		assertEquals(result.getMaxValue(), "1/1/06 0:00");
		assertTrue(input.matches(result.getRegExp()));
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicDateDDMMMYYY() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicDateDDMMMYYY");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final String[] inputs = "22 Jan 1971|12 Mar 2019|02 Jun 1996|11 Dec 1916|19 Apr 1993|26 Sep 1998|09 Dec 1959|14 Jul 2000|18 Aug 2008".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2} " + KnownTypes.PATTERN_ALPHA + "{3} \\d{4}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "dd MMM yyyy");
		assertEquals(result.getMinValue(), "11 Dec 1916");
		assertEquals(result.getMaxValue(), "12 Mar 2019");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void slashDateDDMMYY() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("slashDateDDMMYY");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String[] inputs = "22/01/70|12/01/03|02/01/66|02/01/46|02/01/93|02/01/78|02/01/74|14/01/98|12/01/34".split("\\|");
		final int iterations = 4;
		int locked = -1;

		for (int iters = 0; iters < iterations; iters++) {
			for (int i = 0; i < inputs.length; i++) {
				if (analysis.train(inputs[i]) && locked == -1)
					locked = i;
			}
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length * iterations);
		assertEquals(result.getMatchCount(), inputs.length * iterations);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}/\\d{2}/\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "dd/MM/yy");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void slashDateAmbiguousMMDDYY() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("thin", DateResolutionMode.MonthFirst);
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String input = " 04/03/13";
		final int iterations = 30;
		int locked = -1;

		for (int iters = 0; iters < iterations; iters++) {
			if (analysis.train(input) && locked == -1)
				locked = iters;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), iterations);
		assertEquals(result.getMatchCount(), iterations);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), KnownTypes.PATTERN_WHITESPACE + "\\d{2}/\\d{2}/\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "MM/dd/yy");
		assertNull(result.checkCounts());

		assertTrue(input.matches(result.getRegExp()));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicTimeHHMMSS() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicTimeHHMMSSbasicTimeHHMMSS");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String[] inputs = "00:10:00|00:10:00|23:07:00|06:07:00|16:07:00|06:37:00|06:07:00|06:09:00|06:20:00|06:57:00".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}:\\d{2}:\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALTIME);
		assertEquals(result.getTypeModifier(), "HH:mm:ss");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicTimeHHMM() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicTimeHHMM");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String[] inputs = "00:10|00:10|23:07|06:07|16:07|06:37|06:07|06:09|06:20|06:57".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}:\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALTIME);
		assertEquals(result.getTypeModifier(), "HH:mm");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicFrenchDate() throws IOException, FTAException {

		final Set<String> samples = new HashSet<>();
		LocalDate localDate = LocalDate.now();

		final TextAnalyzer analysis = new TextAnalyzer("basicFrenchDate");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		analysis.configure(TextAnalyzer.Feature.NO_ABBREVIATION_PUNCTUATION, false);

		analysis.setLocale(Locale.FRANCE);

		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRANCE);

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

		assertEquals(result.getRegExp(), "\\d{1,2} [\\p{IsAlphabetic}\\.]{3,5} \\d{4}");
		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "d MMM yyyy");
		assertEquals(result.getSampleCount(), samples.size());
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), samples.size());
		assertEquals(result.getNullCount(), 0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		// Even the UNK match the RE
		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()), sample);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicBulgarianDate() throws IOException, FTAException {

		final Set<String> samples = new HashSet<>();
		LocalDate localDate = LocalDate.now();

		if (!TestUtils.isValidLocale("bg_BG"))
			return;

		final Locale bulgarian = Locale.forLanguageTag("bg-BG");

		final TextAnalyzer analysis = new TextAnalyzer("basicBulgarianDate");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		analysis.setLocale(bulgarian);

		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy", bulgarian);

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

		// Post Java 8 the month abbreviations have changed
		assertEquals(result.getRegExp(), TestUtils.getJavaVersion() == 8 ? "\\d{1,2} \\p{IsAlphabetic}{1,4} \\d{4}" : "\\d{1,2} \\p{IsAlphabetic}{3,4} \\d{4}");
		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "d MMM yyyy");
		assertEquals(result.getSampleCount(), samples.size());
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), samples.size());
		assertEquals(result.getNullCount(), 0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		// Even the UNK match the RE
		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()), sample);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicCatalanDate() throws IOException, FTAException {

		final Set<String> samples = new HashSet<>();
		LocalDate localDate = LocalDate.now();

		if (!TestUtils.isValidLocale("ca_ES"))
			return;

		final Locale catalan = Locale.forLanguageTag("ca-ES");

		final TextAnalyzer analysis = new TextAnalyzer("basicCatalanDate");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		analysis.configure(TextAnalyzer.Feature.NO_ABBREVIATION_PUNCTUATION, false);
		analysis.setLocale(catalan);

		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy", catalan);

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

		assertEquals(result.getRegExp(), "\\d{1,2} .{5,8} \\d{4}");
		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "d MMM yyyy");
		assertEquals(result.getSampleCount(), samples.size());
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), samples.size());
		assertEquals(result.getNullCount(), 0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		// Even the UNK match the RE
		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()), sample);
	}

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
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		// Even the UNK match the RE
		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()), sample);
	}


	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void silly() throws IOException, FTAException {
		//	Output as "EEE MMM dd HH:mm:ss z yyyy"
		final String sample = "Fri Nov 19 13:25:20 GMT-08:00 2021";
		final DateTimeParser dtp = new DateTimeParser();
		String format = dtp.determineFormatString(sample);
		System.err.println("format: " + format);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicGermanDatePassThree() throws IOException, FTAException {

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
		if (!TestUtils.isValidLocale("de_AT"))
			return;

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
		assertNull(result.checkCounts());

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

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicLengthValidationDate() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Spaces");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final int iters = 30;
		final Set<String> samples = new HashSet<>();

		int locked = -1;

		for (int i = 0; i < iters; i++) {
			final String s = String.format(" %02d/03/93", i);
			samples.add(s);
			if (analysis.train(s) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getRegExp(), KnownTypes.PATTERN_WHITESPACE + "\\d{2}/\\d{2}/\\d{2}");
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "dd/MM/yy");
		assertEquals(result.getSampleCount(), iters);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getCardinality(), iters - 1);
		assertEquals(result.getOutlierCount(), 1);
		assertEquals(result.getMatchCount(), iters - 1);
		assertEquals(result.getMinLength(), 9);
		assertEquals(result.getMaxLength(), 9);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples) {
			assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void dateTimeYYYYMMDDTHHMMSS() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("dateTimeYYYYMMDDTHHMMSS");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		analysis.train("2004-01-01T00:00:00");
		analysis.train("2004-01-01T02:00:00");
		analysis.train("2006-01-01T00:00:00");
		analysis.train("2004-01-01T02:00:00");
		analysis.train("2006-01-01T13:00:00");
		analysis.train("2004-01-01T00:00:00");
		analysis.train("2006-01-01T13:00:00");
		analysis.train("2006-01-01T00:00:00");
		analysis.train("2004-01-01T00:00:00");
		analysis.train("2004-01-01T00:00:00");
		analysis.train("2004-01-01T00:00:00");
		analysis.train("2004-01-01T00:00:00");
		analysis.train("2004-01-01T00:00:00");
		analysis.train("2008-01-01T13:00:00");
		analysis.train("2008-01-01T13:00:00");
		analysis.train("2010-01-01T00:00:00");
		analysis.train("2004-01-01T02:00:00");
		analysis.train(null);
		analysis.train("2008-01-01T00:00:00");
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), 20);
		assertEquals(result.getNullCount(), 2);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getTypeModifier(), "yyyy-MM-dd'T'HH:mm:ss");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}


	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void dateTimeYYYYMMDDTHHMMssSSSZ() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("dateTimeYYYYMMDDTHHMMssSSSZ");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		analysis.train("2010-07-01T22:20:22.400Z");
		analysis.train("2015-03-01T22:20:21.000Z");
		analysis.train("2015-07-01T22:20:22.300Z");
		analysis.train("2015-07-01T12:20:32.000Z");
		analysis.train("2015-07-01T22:20:22.100Z");
		analysis.train("2011-02-01T02:20:22.000Z");
		analysis.train("2015-07-01T22:30:22.000Z");
		analysis.train("2015-07-01T22:20:22.010Z");
		analysis.train("2012-01-01T22:20:22.000Z");
		analysis.train("2015-07-01T22:40:22.000Z");
		analysis.train("2015-07-01T22:20:22.000Z");
		analysis.train("2015-07-01T22:20:22.200Z");
		analysis.train("2015-07-01T22:20:22.000Z");
		analysis.train("2014-08-01T12:10:22.000Z");
		analysis.train("2015-06-01T22:20:22.010Z");
		analysis.train("2015-07-01T22:20:22.000Z");
		analysis.train("2017-07-01T02:20:22.000Z");
		analysis.train(null);
		analysis.train("2018-06-01T08:20:22.000Z");
		analysis.train(null);

		TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.OFFSETDATETIME);
		assertEquals(result.getTypeModifier(), "yyyy-MM-dd'T'HH:mm:ss.SSSX");
		assertEquals(result.getSampleCount(), 20);
		assertEquals(result.getNullCount(), 2);
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}([-+][0-9]{2}([0-9]{2})?|Z)");
		assertEquals(result.getMatchCount(), 18);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		analysis.train("2008-01-01T00:00:00-05:00");
		result = analysis.getResult();
		assertEquals(result.getSampleCount(), 21);

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void variableFractionalSeconds() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("variableFractionalSeconds");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		analysis.train("2021-08-23T18:49:50.991-04:00");
		analysis.train("2021-08-23T19:03:44.449-04:00");
		analysis.train("2021-08-23T19:03:44.5-04:00");
		analysis.train("2021-08-23T19:03:44.757-04:00");
		analysis.train("2021-08-23T19:03:45.046-04:00");
		analysis.train("2021-08-23T19:03:45.338-04:00");
		analysis.train("2021-08-23T19:03:45.63-04:00");
		analysis.train("2021-08-23T19:03:45.922-04:00");
		analysis.train("2021-08-23T18:51:56.043-04:00");
		analysis.train("2021-08-23T18:51:57.189-04:00");
		analysis.train("2021-08-23T18:52:01.167-04:00");
		analysis.train("2021-08-23T18:52:03.302-04:00");
		analysis.train("2021-08-23T18:52:03.596-04:00");
		analysis.train("2021-08-23T18:52:04.234-04:00");
		analysis.train("2021-08-23T18:52:06.261-04:00");
		analysis.train("2021-08-23T18:52:06.608-04:00");
		analysis.train("2021-08-23T18:52:07.666-04:00");
		analysis.train("2021-08-23T18:52:08.023-04:00");
		analysis.train("2021-08-23T18:52:09.286-04:00");
		analysis.train("2021-08-23T18:53:39.559-04:00");

		TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.OFFSETDATETIME);
		assertEquals(result.getTypeModifier(), "yyyy-MM-dd'T'HH:mm:ss.S{1,3}xxx");
		assertEquals(result.getSampleCount(), 20);
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{1,3}[-+][0-9]{2}:[0-9]{2}");
		assertEquals(result.getMatchCount(), 20);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		analysis.train("2008-01-01T00:00:00-05:00");
		result = analysis.getResult();
		assertEquals(result.getSampleCount(), 21);

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void variableFractionalSecondsWithStats() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("variableFractionalSecondsWithStats");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		analysis.train("2021-08-23T18:49:50.991-04:00");
		analysis.train("2021-08-23T19:03:44.449-04:00");
		analysis.train("2021-08-23T19:03:44.5-04:00");
		analysis.train("2021-08-23T19:03:44.757-04:00");
		analysis.train("2021-08-23T19:03:45.046-04:00");
		analysis.train("2021-08-23T19:03:45.338-04:00");
		analysis.train("2021-08-23T19:03:45.63-04:00");
		analysis.train("2021-08-23T19:03:45.922-04:00");
		analysis.train("2021-08-23T18:51:56.043-04:00");
		analysis.train("2021-08-23T18:51:57.189-04:00");
		analysis.train("2021-08-23T18:52:01.167-04:00");
		analysis.train("2021-08-23T18:52:03.302-04:00");
		analysis.train("2021-08-23T18:52:03.596-04:00");
		analysis.train("2021-08-23T18:52:04.234-04:00");
		analysis.train("2021-08-23T18:52:06.261-04:00");
		analysis.train("2021-08-23T18:52:06.608-04:00");
		analysis.train("2021-08-23T18:52:07.666-04:00");
		analysis.train("2021-08-23T18:52:08.023-04:00");
		analysis.train("2021-08-23T18:52:09.286-04:00");
		analysis.train("2021-08-23T18:53:39.559-04:00");

		TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.OFFSETDATETIME);
		assertEquals(result.getTypeModifier(), "yyyy-MM-dd'T'HH:mm:ss.S{1,3}xxx");
		assertEquals(result.getSampleCount(), 20);
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{1,3}[-+][0-9]{2}:[0-9]{2}");
		assertEquals(result.getMatchCount(), 20);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		analysis.train("2008-01-01T00:00:00-05:00");
		result = analysis.getResult();
		assertEquals(result.getSampleCount(), 21);

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void dateTimeYYYYMMDDTHHMMSSNNNN() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("ISODate", DateResolutionMode.Auto);

		analysis.train("2004-01-01T00:00:00-05:00");
		analysis.train("2004-01-01T02:00:00-05:00");
		analysis.train("2006-01-01T00:00:00-05:00");
		analysis.train("2006-01-01T00:00:00-05:00");
		analysis.train("2004-01-01T02:00:00-05:00");
		analysis.train("2006-01-01T13:00:00-05:00");
		analysis.train("2004-01-01T00:00:00-05:00");
		analysis.train("2006-01-01T13:00:00-05:00");
		analysis.train("2006-01-01T00:00:00-05:00");
		analysis.train("2004-01-01T00:00:00-05:00");
		analysis.train("2004-01-01T00:00:00-05:00");
		analysis.train("2004-01-01T00:00:00-05:00");
		analysis.train("2004-01-01T00:00:00-05:00");
		analysis.train("2004-01-01T00:00:00-05:00");
		analysis.train("2008-01-01T13:00:00-05:00");
		analysis.train("2008-01-01T13:00:00-05:00");
		analysis.train("2010-01-01T00:00:00-05:00");
		analysis.train(null);
		analysis.train("2008-01-01T00:00:00-05:00");
		analysis.train(null);

		TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.OFFSETDATETIME);
		assertEquals(result.getSampleCount(), 20);
		assertEquals(result.getMatchCount(), 18);
		assertEquals(result.getNullCount(), 2);
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[-+][0-9]{2}:[0-9]{2}");
		assertEquals(result.getTypeModifier(), "yyyy-MM-dd'T'HH:mm:ssxxx");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "2004-01-01T00:00:00-05:00");
		assertEquals(result.getMaxValue(), "2010-01-01T00:00:00-05:00");
		assertNull(result.checkCounts());

		analysis.train("2008-01-01T00:00:00-05:00");
		result = analysis.getResult();
		assertEquals(result.getSampleCount(), 21);

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void dateTimeYYYYMMDDTHHMMSSZ() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("dateTimeYYYYMMDDTHHMMSSZ");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		analysis.train("01/26/2012 10:42:23 GMT");
		analysis.train("01/26/2012 10:42:23 GMT");
		analysis.train("01/30/2012 10:59:48 GMT");
		analysis.train("01/25/2012 16:46:43 GMT");
		analysis.train("01/25/2012 16:28:42 GMT");
		analysis.train("01/24/2012 16:53:04 GMT");

		TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.ZONEDDATETIME);
		assertEquals(result.getSampleCount(), 6);
		assertEquals(result.getMatchCount(), 6);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2} .*");
		assertEquals(result.getTypeModifier(), "MM/dd/yyyy HH:mm:ss z");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		analysis.train("01/25/2012 16:28:42 GMT");
		result = analysis.getResult();
		assertEquals(result.getSampleCount(), 7);

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void dateTimeYYYYMMDDTHHMMSSZwithStatistics() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("dateTimeYYYYMMDDTHHMMSSZwithStatistics");

		analysis.train("01/26/2012 10:42:23 GMT");
		analysis.train("01/26/2012 10:42:23 GMT");
		analysis.train("01/30/2012 10:59:48 GMT");
		analysis.train("01/25/2012 16:46:43 GMT");
		analysis.train("01/25/2012 16:28:42 GMT");
		analysis.train("01/24/2012 16:53:04 GMT");

		TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.ZONEDDATETIME);
		assertEquals(result.getSampleCount(), 6);
		assertEquals(result.getMatchCount(), 6);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2} .*");
		assertEquals(result.getTypeModifier(), "MM/dd/yyyy HH:mm:ss z");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "01/24/2012 16:53:04 GMT");
		assertEquals(result.getMaxValue(), "01/30/2012 10:59:48 GMT");
		assertNull(result.checkCounts());

		analysis.train("01/25/2012 16:28:42 GMT");
		result = analysis.getResult();
		assertEquals(result.getSampleCount(), 7);

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicMMddyy() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("DOB");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String pipedInput = "12/5/59|2/13/48|6/29/62|1/7/66|7/3/84|5/28/74|" +
				"|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2}/\\d{1,2}/\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "M/d/yy");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void yyyyMddHHmm() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("yyyyMddHHmm");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String pipedInput = "2017-4-15 21:10|2016-9-27 14:10|2016-3-11 07:10|2015-8-24 00:10|2015-2-04 17:10|" +
				"2014-7-19 10:10|2013-12-31 03:10|2013-6-13 20:10|2012-11-25 13:10|2012-5-09 06:10|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{4}-\\d{1,2}-\\d{2} \\d{2}:\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "yyyy-M-dd HH:mm");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void H_mm_ss_S_false() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("H_mm_ss_S_false");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String pipedInput = "1:01:50.00|2:01:16.00|2:01:30.00|2:01:55.00|5:01:49.00|9:01:51.00|11:01:20.0|11:01:47.0|12:01:16.0|12:01:55.0|14:01:21.0|14:01:25.0|14:01:43.0|15:01:03.0|15:01:39.0|15:01:48.0|15:01:51.0|19:01:47.0|20:01:34.0|21:01:03.0|21:01:27.0|22:01:15.0|22:01:32.0|11:01:58.0|13:01:31.0|16:01:24.0|16:01:58.0|17:01:05.0|11:01:38.0|11:01:44.0|13:01:41.0|14:01:14.0|14:01:59.0|14:01:59.0|14:01:59.0|15:01:04.0|15:01:11.0|15:01:54.0|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2}:\\d{2}:\\d{2}\\.\\d{1,2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALTIME);
		assertEquals(result.getTypeModifier(), "H:mm:ss.S{1,2}");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void H_mm_ss_S_true() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("H_mm_ss_S_true");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String pipedInput = "1:01:50.00|2:01:16.00|2:01:30.00|2:01:55.00|5:01:49.00|9:01:51.00|11:01:20.0|11:01:47.0|12:01:16.0|12:01:55.0|14:01:21.0|14:01:25.0|14:01:43.0|15:01:03.0|15:01:39.0|15:01:48.0|15:01:51.0|19:01:47.0|20:01:34.0|21:01:03.0|21:01:27.0|22:01:15.0|22:01:32.0|11:01:58.0|13:01:31.0|16:01:24.0|16:01:58.0|17:01:05.0|11:01:38.0|11:01:44.0|13:01:41.0|14:01:14.0|14:01:59.0|14:01:59.0|14:01:59.0|15:01:04.0|15:01:11.0|15:01:54.0|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2}:\\d{2}:\\d{2}\\.\\d{1,2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALTIME);
		assertEquals(result.getTypeModifier(), "H:mm:ss.S{1,2}");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void k_mm_ss_S_false() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("k_mm_ss_S_false");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String pipedInput = "1:01:50.00|2:01:16.00|2:01:30.00|2:01:55.00|5:01:49.00|9:01:51.00|11:01:20.0|11:01:47.0|12:01:16.0|12:01:55.0|14:01:21.0|14:01:25.0|14:01:43.0|15:01:03.0|15:01:39.0|15:01:48.0|15:01:51.0|19:01:47.0|20:01:34.0|21:01:03.0|21:01:27.0|22:01:15.0|22:01:32.0|24:01:29.0|11:01:58.0|13:01:31.0|16:01:24.0|16:01:58.0|17:01:05.0|11:01:38.0|11:01:44.0|13:01:41.0|14:01:14.0|14:01:59.0|14:01:59.0|14:01:59.0|15:01:04.0|15:01:11.0|15:01:54.0|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getRegExp(), "\\d{1,2}:\\d{2}:\\d{2}\\.\\d{1,2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALTIME);
		assertEquals(result.getTypeModifier(), "k:mm:ss.S{1,2}");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void k_mm_ss_S_true() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("k_mm_ss_S_true");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String pipedInput = "1:01:50.00|2:01:16.00|2:01:30.00|2:01:55.00|5:01:49.00|9:01:51.00|11:01:20.0|11:01:47.0|12:01:16.0|12:01:55.0|14:01:21.0|14:01:25.0|14:01:43.0|15:01:03.0|15:01:39.0|15:01:48.0|15:01:51.0|19:01:47.0|20:01:34.0|21:01:03.0|21:01:27.0|22:01:15.0|22:01:32.0|24:01:29.0|11:01:58.0|13:01:31.0|16:01:24.0|16:01:58.0|17:01:05.0|11:01:38.0|11:01:44.0|13:01:41.0|14:01:14.0|14:01:59.0|14:01:59.0|14:01:59.0|15:01:04.0|15:01:11.0|15:01:54.0|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getRegExp(), "\\d{1,2}:\\d{2}:\\d{2}\\.\\d{1,2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALTIME);
		assertEquals(result.getTypeModifier(), "k:mm:ss.S{1,2}");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void Hmmss() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Hmmss");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String pipedInput = "21:53:12|12:53:12|3:53:12|18:53:12|9:53:12|0:53:12|15:53:12|6:53:12|21:53:12|12:53:12|3:53:12|18:53:12|9:53:12|0:53:12|15:53:12|" +
				"6:53:12|21:53:12|12:53:12|3:53:12|18:53:12|9:53:12|0:53:12|15:53:12|6:53:12|21:53:12|12:53:12|3:53:12|18:53:12|9:53:12|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2}:\\d{2}:\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALTIME);
		assertEquals(result.getTypeModifier(), "H:mm:ss");
		assertEquals(analysis.getTrainingSet(), Arrays.asList(Arrays.copyOfRange(inputs, 0, analysis.getDetectWindow())));
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void yyyyMMddHHmmz() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("yyyyMMddHHmmz");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String pipedInput = "2017-08-24 12:10 EDT|2017-07-03 06:10 EDT|2017-05-12 00:10 EDT|2017-03-20 18:10 EDT|2016-07-02 12:10 EDT|" +
				"2017-01-27 11:10 EST|2016-12-06 05:10 EST|2016-10-15 00:10 EDT|2016-08-23 18:10 EDT|2016-05-11 06:10 EDT|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2} .*");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.ZONEDDATETIME);
		assertEquals(result.getTypeModifier(), "yyyy-MM-dd HH:mm z");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void dateYYYYMMDD() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("dateYYYYMMDD");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String[] inputs = "2010-01-22|2019-01-12|1996-01-02|1916-01-02|1993-01-02|1998-01-02|2001-01-02|2000-01-14|2008-01-12".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "yyyy-MM-dd");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void dateYYYY() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("dateYYYY");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String pipedInput = "2015|2015|2015|2015|2015|2015|2015|2016|2016|2016|2013|1932|1991|1993|2001|1977|2001|1976|1972|" +
				"1982|2005|1950|1961|1967|1997|1967|1996|2014|2002|1953|1980|2010|2010|1979|1980|1983|1974|1970|" +
				"1978|2014|2015|1979|1982|2016|2016|2013|2011|1986|1985|2000|2000|2012|2000|2000|";
		final String[] inputs = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{4}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "yyyy");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));

		final TextAnalyzer analysis2 = new TextAnalyzer("dateYYYY_2");

		for (int i = 0; i < inputs.length; i++) {
			if (analysis2.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result2 = analysis2.getResult();

		assertEquals(result2.getSampleCount(), inputs.length);
		assertEquals(result2.getMatchCount(), inputs.length);
		assertEquals(result2.getNullCount(), 0);
		assertEquals(result2.getRegExp(), "\\d{4}");
		assertEquals(result2.getConfidence(), 1.0);
		assertEquals(result2.getType(), FTAType.LOCALDATE);
		assertEquals(result2.getTypeModifier(), "yyyy");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result2, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result2.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void dateYYYY_with_zeroes() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Date");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String pipedInput =
				"1801|1802|1900|1901|1902|1903|1904|1801|1802|1900|1901|1902|1903|" +
				"1904|1801|1802|1900|1901|1902|1903|1904|1801|1802|1900|1901|1902|" +
				"1904|1801|1802|1900|1901|1902|1903|1904|1801|1802|1900|1901|1902|" +
				"2013|2014|2020|2009|2008|2007|2006|2005|2004|2003|2002|2000|2001|" +
				"1904|1801|1802|1900|1901|1902|1903|1904|1801|1802|1900|1901|1902|" +
				"1902|1903|1904|00|";
		final String[] inputs = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length - 1);
		assertEquals(result.getInvalidCount(), 1);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "yyyy");
		assertEquals(result.getRegExp(), "\\d{4}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final Map.Entry<String, Long> entry : result.getCardinalityDetails().entrySet())
			assertTrue(entry.getKey().matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void randomFormats() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("randomFormats");

		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String[] inputs = {
				"2019-01-01",
				"3/3/2013",
				"2018-12-31",
				"7/4/2014",
				"2017-11-30",
				"9/15/2012",
				"2019-01-01",
				"2016-10-29",
				"12/25/2011",
				"2015-09-28" };
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "yyyy-MM-dd");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length - 4);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2}");
		assertEquals(result.getConfidence(), 0.6);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void mixedDates() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("mixedDates");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String[] inputs = {
				"04/07/2017", "13/07/2017", "15/08/2017", "29/08/2017", "09/10/2017", "14/09/2017", "25/09/2017",
				" 2017-07-11", " 2017-07-16", " 2017-08-06", " 2017-08-09", " 2017-09-04", " 2017-09-06", " 2017-09-08",
				" 2017-09-11", " 2017-09-14", " 2017-09-17", " 2017-07-07", "", "",
				" 2017-07-12", " 2017-07-17", " 2017-07-19", "", "", " 2017-08-22", ""
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "yyyy-MM-dd");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getBlankCount(), 5);
		// 20 total - 7 bad, 5 blank
		assertEquals(result.getMatchCount(), inputs.length - 12);
		assertEquals(result.getRegExp(), "[ 	]*\\d{4}-\\d{2}-\\d{2}");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicDateDMMMYY() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicDateDMMMYY");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String pipedInput = "1-Jan-14|2-Jan-14|3-Jan-14|6-Jan-14|7-Jan-14|7-Jan-14|8-Jan-14|9-Jan-14|10-Jan-14|" +
				"13-Jan-14|14-Jan-14|15-Jan-14|16-Jan-14|17-Jan-14|20-Jan-14|21-Jan-14|22-Jan-14|" +
				"23-Jan-14|24-Jan-14|27-Jan-14|28-Jan-14|29-Jan-14|30-Jan-14|31-Jan-14|3-Feb-14|" +
				"4-Feb-14|5-Feb-14|6-Feb-14|7-Feb-14|10-Feb-14|11-Feb-14|12-Feb-14|13-Feb-14|14-Feb-14|" +
				"17-Feb-14|18-Feb-14|19-Feb-14|20-Feb-14|21-Feb-14|24-Feb-14|25-Feb-14|26-Feb-14|27-Feb-14|" +
				"28-Feb-14|3-Mar-14|4-Mar-14|5-Mar-14|";
		final String[] inputs = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "d-MMM-yy");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2}-" + KnownTypes.PATTERN_ALPHA + "{3}-\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicUnixDateCommand() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicUnixDateCommand");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		analysis.setDebug(1);
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, true);
		final String pipedInput =
				"Thu Jul  2 09:48:00 PDT 2020|Thu Jul  1 10:00:56 PDT 2020|Thu Jul  2 04:56:56 PDT 2020|Thu Jul  2 09:48:56 PDT 2020|" +
				"Thu Jul  2 09:56:14 PDT 2020|Thu Jul  4 04:00:48 PDT 2020|Thu Jul  2 09:56:48 PDT 2020|Thu Jul 23 04:48:48 PDT 2020|" +
				"Thu Jul  2 09:00:56 PDT 2020|Thu Jul  2 03:00:56 PDT 2020|Thu Jul  9 09:14:00 PDT 2020|Thu Jul  2 09:56:56 PDT 2020|" +
				"Thu Jul  4 08:14:56 PDT 2020|Thu Jul  2 09:14:56 PDT 2020|Thu Jul 12 09:23:56 PDT 2020|Thu Jul 23 09:56:23 PDT 2020|" +
				"Thu Jul  2 09:56:56 PDT 2020|Thu Jul 23 08:56:56 PDT 2020|Thu Jul  2 09:56:23 PDT 2020|Thu Jul  5 03:14:56 PDT 2020|";
		final String[] inputs = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.ZONEDDATETIME);
		assertEquals(result.getTypeModifier(), "EEE MMM ppd HH:mm:ss z yyyy");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length - 5);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{3} \\p{IsAlphabetic}{3} [ \\d]\\d \\d{2}:\\d{2}:\\d{2} .* \\d{4}");
		assertEquals(result.getConfidence(), .75);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void badDatesGoodFormat() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicUnixDateCommand");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		analysis.setDebug(1);
		// ** Note: June 16 was NOT a Sunday! We still return the 'correct' format.
		final String[] inputs = {
				"Sunday, June 16, 2010 23:02:18 UTC", "Sunday, June 16, 2010 22:58:45 UTC", "Sunday, June 16, 2010 22:58:20 UTC",
				"Sunday, June 16, 2010 22:35:06 UTC", "Sunday, June 16, 2010 22:21:39 UTC", "Sunday, June 16, 2010 22:07:59 UTC",
				"Sunday, June 16, 2010 22:07:52 UTC", "Sunday, June 16, 2010 21:46:07 UTC", "Sunday, June 16, 2010 21:44:19 UTC",
				"Sunday, June 16, 2010 21:23:41 UTC", "Sunday, June 16, 2010 21:21:48 UTC", "Sunday, June 16, 2010 21:20:31 UTC",
				"Sunday, June 16, 2010 21:15:45 UTC", "Sunday, June 16, 2010 21:08:57 UTC", "Sunday, June 16, 2010 20:30:08 UTC",
				"Sunday, June 16, 2010 20:26:52 UTC", "Sunday, June 16, 2010 20:17:12 UTC", "Sunday, June 16, 2010 20:04:35 UTC",
				"Sunday, June 16, 2010 19:34:07 UTC", "Sunday, June 16, 2010 19:07:12 UTC", "Sunday, June 16, 2010 18:58:43 UTC",
				"Sunday, June 16, 2010 18:54:22 UTC", "Sunday, June 16, 2010 18:38:08 UTC", "Sunday, June 16, 2010 17:50:25 UTC",
				"Sunday, June 16, 2010 17:44:22 UTC", "Sunday, June 16, 2010 17:11:25 UTC", "Sunday, June 16, 2010 16:59:38 UTC"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.ZONEDDATETIME);
		assertEquals(result.getTypeModifier(), "EEEE, MMMM dd, yyyy HH:mm:ss z");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{6,9}, \\p{IsAlphabetic}{3,9} \\d{2}, \\d{4} \\d{2}:\\d{2}:\\d{2} .*");
		assertEquals(result.getConfidence(), 0.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		// The dates are all invalid BUT they should still match the RE
		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void datesMonthAbbr_enUS() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Date");
		final String[] inputs = {
				"1999-Aug-11", "1999-Aug-11", "1999-Aug-17", "1999-Aug-17", "1999-Aug-26",
				"1999-Aug-11", "1999-Aug-25", "1999-Aug-23", "1999-Aug-16", "1999-Aug-17",
				"1999-Aug-18", "1999-Aug-26", "1999-Aug-20", "1999-Aug-26", "1999-Aug-23",
				"1999-Aug-24", "1999-Aug-11", "1999-Aug-11", "1999-Aug-18", "1999-Aug-24",
				"1999-Aug-24", "1999-Aug-11", "1999-Aug-17", "1999-Aug-26", "1999-Aug-23",
				"1999-Aug-17", "1999-Aug-26", "1999-Aug-26", "1999-Aug-24", "1999-Aug-24",
				"1999-Aug-24", "1999-Aug-11", "1999-Aug-25", "1999-Aug-16", "1999-Aug-17"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "yyyy-MMM-dd");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{4}-\\p{IsAlphabetic}{3}-\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void datesMonthAbbr_enCA() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Date");
		analysis.setLocale(Locale.CANADA);
		final String[] inputs = {
				"1999-Aug-11", "1999-Aug-11", "1999-Aug-17", "1999-Aug-17", "1999-Aug-26",
				"1999-Aug-11", "1999-Aug-25", "1999-Aug-23", "1999-Aug-16", "1999-Aug-17",
				"1999-Aug-18", "1999-Aug-26", "1999-Aug-20", "1999-Aug-26", "1999-Aug-23",
				"1999-Aug-24", "1999-Aug-11", "1999-Aug-11", "1999-Aug-18", "1999-Aug-24",
				"1999-Aug-24", "1999-Aug-11", "1999-Aug-17", "1999-Aug-26", "1999-Aug-23",
				"1999-Aug-17", "1999-Aug-26", "1999-Aug-26", "1999-Aug-24", "1999-Aug-24",
				"1999-Aug-24", "1999-Aug-11", "1999-Aug-25", "1999-Aug-16", "1999-Aug-17"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "yyyy-MMM-dd");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{4}-\\p{IsAlphabetic}{3}-\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void datesMonthAbbr_ptBR() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("VIGÊNCIA FINAL");
		analysis.setLocale(Locale.forLanguageTag("pt-BR"));
		final String[] inputs = {
				"09/mai/2017", "07/dez/2017", "21/jul/2017", "16/nov/2017", "24/mar/2017", "30/mar/2017",
				"30/mar/2018", "30/mai/2017", "24/ago/2017", "16/out/2017", "06/dez/2017", "18/dez/2017",
				"27/jul/2017", "30/set/2017", "21/ago/2017", "12/jul/2017", "21/mai/2017", "09/jun/2017",
				"27/jul/2017", "21/ago/2017", "16/nov/2017", "30/dez/2017", "30/dez/2017", "INDETERMINADO",
				"15/dez/2017", "11/mar/2017", "01/jan/2018", "29/jan/2018", "31/out/2017", "17/dez/2017",
				"", "", ""
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "dd/MMM/yyyy");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length - 1 - result.getBlankCount());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}/\\p{IsAlphabetic}{3}/\\d{4}");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicMMMM_d_yyyy() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicMMMM_d_yyyy");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String pipedInput = "September-17-2014|September-11-2011|September-4-2008|August-29-2005|August-23-2002|August-17-1999|" +
				"August-10-1996|August-4-1993|July-29-1990|July-23-1987|July-16-1984|July-10-1981|July-4-1978|June-28-1975|" +
				"June-21-1972|June-15-1969|June-9-1966|June-3-1963|May-27-1960|May-21-1957|May-15-1954|May-9-1951|May-2-1948|" +
				"April-26-1945|April-20-1942|April-14-1939|April-7-1936|April-1-1933|March-26-1930|March-20-1927";
		final String[] inputs = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "MMMM-d-yyyy");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), KnownTypes.PATTERN_ALPHA + "{3,9}-\\d{1,2}-\\d{4}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void startsAsTwoDigitDay() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("startsAsTwoDigitDay");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String pipedInput =
				"27/6/2012 12:46:03|27/6/2012 15:29:48|27/6/2012 23:32:22|27/6/2012 23:38:51|27/6/2012 23:42:22|" +
						"27/6/2012 23:49:13|27/6/2012 23:56:02|28/6/2012 08:04:51|28/6/2012 15:53:00|28/6/2012 16:46:34|" +
						"28/6/2012 17:01:01|28/6/2012 17:53:52|28/6/2012 18:03:31|28/6/2012 18:31:14|28/6/2012 18:46:12|" +
						"28/6/2012 23:32:08|28/6/2012 23:44:54|28/6/2012 23:47:48|28/6/2012 23:51:32|28/6/2012 23:53:36|" +
						"29/6/2012 08:54:18|29/6/2012 08:56:53|29/6/2012 11:21:56|29/6/2012 16:48:14|29/6/2012 16:56:32|" +
						"1/7/2012 09:15:03|1/7/2012 15:36:44|1/7/2012 18:25:35|1/7/2012 18:31:19|1/7/2012 18:36:04|" +
						"1/7/2012 19:13:17|1/7/2012 19:13:35|1/7/2012 19:13:49|1/7/2012 19:14:07|1/7/2012 19:14:21|" +
						"1/7/2012 19:14:29|1/7/2012 19:16:45|1/7/2012 19:17:48|1/7/2012 19:18:19|1/7/2012 19:19:09|";
		final String[] inputs = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "d/M/yyyy HH:mm:ss");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2}/\\d{1,2}/\\d{4} \\d{2}:\\d{2}:\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void startsAsTwoDigitMonth() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("startsAsTwoDigitMonth");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String pipedInput =
				"27/10/2012 12:46:03|27/10/2012 15:29:48|27/10/2012 23:32:22|27/10/2012 23:38:51|27/10/2012 23:42:22|" +
						"27/10/2012 23:49:13|27/10/2012 23:56:02|28/10/2012 08:04:51|28/10/2012 15:53:00|28/10/2012 16:46:34|" +
						"28/10/2012 17:01:01|28/10/2012 17:53:52|28/10/2012 18:03:31|28/10/2012 18:31:14|28/10/2012 18:46:12|" +
						"28/10/2012 23:32:08|28/10/2012 23:44:54|28/10/2012 23:47:48|28/10/2012 23:51:32|28/10/2012 23:53:36|" +
						"29/10/2012 08:54:18|29/10/2012 08:56:53|29/10/2012 11:21:56|29/10/2012 16:48:14|29/10/2012 16:56:32|" +
						"10/7/2012 09:15:03|1/7/2012 15:36:44|1/7/2012 18:25:35|1/7/2012 18:31:19|1/7/2012 18:36:04|" +
						"1/7/2012 19:13:17|1/7/2012 19:13:35|1/7/2012 19:13:49|1/7/2012 19:14:07|1/7/2012 19:14:21|" +
						"1/7/2012 19:14:29|1/7/2012 19:16:45|1/7/2012 19:17:48|1/7/2012 19:18:19|1/7/2012 19:19:09|";
		final String[] inputs = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "d/M/yyyy HH:mm:ss");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2}/\\d{1,2}/\\d{4} \\d{2}:\\d{2}:\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void slashDateYYYYMMDD() throws IOException, FTAException {
		final TextAnalyzer analysis1 = new TextAnalyzer("slashDateYYYYMMDD");
		analysis1.setTrace("samples=10");
		analysis1.setTrace("samples=10");
		analysis1.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String[] inputs1 = "2010/01/22|2019/01/12|1996/01/02|1916/01/02|1993/01/02|1998/01/02|2001/01/02|2000/01/14|2008/01/12".split("\\|");
		final String[] inputs2 = "2007/01/22|2019/01/12|1996/03/02|1916/06/02|1993/09/02|1998/01/02|2001/01/02|2000/01/14|2018/01/12".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs1.length; i++) {
			if (analysis1.train(inputs1[i]) && locked == -1)
				locked = i;
		}
		final TextAnalysisResult result1 = analysis1.getResult();
		String structureSignature1 = result1.getStructureSignature();
		String dateSignature1 = result1.getDataSignature();

		assertEquals(result1.getSampleCount(), inputs1.length);
		assertEquals(result1.getMatchCount(), inputs1.length);
		assertEquals(result1.getNullCount(), 0);
		assertEquals(result1.getRegExp(), "\\d{4}/\\d{2}/\\d{2}");
		assertEquals(result1.getConfidence(), 1.0);
		assertEquals(result1.getType(), FTAType.LOCALDATE);
		assertEquals(result1.getTypeModifier(), "yyyy/MM/dd");

		TestSupport.checkHistogram(result1, 10, true);
		TestSupport.checkQuantiles(result1);

		for (final String input2 : inputs2)
			analysis1.train(input2);
		final TextAnalysisResult result2 = analysis1.getResult();

		assertEquals(result2.getSampleCount(), inputs1.length + inputs2.length);
		assertEquals(result2.getMatchCount(), inputs1.length + inputs2.length);
		assertEquals(result2.getNullCount(), 0);
		assertEquals(result2.getRegExp(), "\\d{4}/\\d{2}/\\d{2}");
		assertEquals(result2.getConfidence(), 1.0);
		assertEquals(result2.getType(), FTAType.LOCALDATE);
		assertEquals(result2.getTypeModifier(), "yyyy/MM/dd");

		assertEquals(structureSignature1, result2.getStructureSignature());
		assertNotEquals(dateSignature1, result2.getDataSignature());

		TestSupport.checkHistogram(result2, 10, true);
		TestSupport.checkQuantiles(result2);

		for (final String input1 : inputs1)
			assertTrue(input1.matches(result1.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicDateDDMMYYYY() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicDateDDMMYYYY");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String[] inputs = "22-01-2010|12-01-2019|02-01-1996|02-01-1916|02-01-1993|02-01-1998|02-01-2001|14-01-2000|12-01-2008".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}-\\d{2}-\\d{4}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "dd-MM-yyyy");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void variableDateDDMMYYYY() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("variableDateDDMMYYYY");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String[] inputs = "22-1-2010|12-1-2019|2-1-1996|2-1-1916|2-1-1993|2-1-1998|22-11-2001|14-1-2000|12-5-2008".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2}-\\d{1,2}-\\d{4}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "d-M-yyyy");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void slashDateDDMMYYYY() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("slashDateDDMMYYYY");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String[] inputs = "22/01/2010|12/01/2019|02/01/1996|02/01/1916|02/01/1993|02/01/1998|02/01/2001|14/01/2000|12/01/2008".split("\\|");
		final int iterations = 4;
		int locked = -1;

		for (int iters = 0; iters < iterations; iters++) {
			for (int i = 0; i < inputs.length; i++) {
				if (analysis.train(inputs[i]) && locked == -1)
					locked = i;
			}
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length * iterations);
		assertEquals(result.getMatchCount(), inputs.length * iterations);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}/\\d{2}/\\d{4}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "dd/MM/yyyy");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void lenientDates() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("INACTIVE DATE");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String inputs[] = {
				"26/01/2018", "06/01/2018", "00/00/0000", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "00/00/0000", "00/00/0000", "06/01/2018",
				"06/01/2018", "06/01/2018", "00/00/0000", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "00/00/0000",
				"06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018",
				"06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "00/00/0000",
				"06/01/2018", "06/01/2018", "00/00/0000", "06/01/2018", "00/00/0000", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "00/00/0000",
				"26/01/2018", "06/01/2018", "00/00/0000", "06/01/2018", "06/01/2018", "09/01/2018", "09/01/2018", "00/00/0000", "09/01/2018", "09/01/2018",
				"00/00/0000", "29/01/2018", "00/00/0000", "00/00/0000", "09/01/2018", "00/00/0000", "09/01/2018", "09/01/2018", "00/00/0000", "00/00/0000",
				"00/00/0000", "00/00/0000", "09/01/2018", "00/00/0000", "00/00/0000", "00/00/0000", "09/01/2018", "09/01/2018", "00/00/0000", "00/00/0000",
				"00/00/0000", "00/00/0000", "00/00/0000", "00/00/0000", "00/00/0000", "09/01/2018", "00/00/0000", "00/00/0000", "09/01/2018", "09/01/2018",
				"09/01/2018", "09/01/2018", "09/01/2018", "09/01/2018", "00/00/0000", "00/00/0000", "09/01/2018", "09/01/2018", "09/01/2018", "09/01/2018",
				"09/01/2018", "00/00/0000", "00/00/0000", "09/01/2018", "00/00/0000", "00/00/0000", "00/00/0000", "00/00/0000", "00/00/0000", "00/00/0000",
				"09/01/2018", "00/00/0000", "09/01/2018", "09/01/2018", "09/01/2018", "09/01/2018", "00/00/0000", "09/01/2018", "00/00/0000", "00/00/0000",
				"00/00/0000", "00/00/0000", "00/00/0000", "09/01/2018", "06/01/2018", "00/00/0000", "00/00/0000", "09/01/2018", "09/01/2018"
		};
		int locked = -1;
		final int zeroes = 50;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "dd/MM/yyyy");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 1);
		final Map<String, Long> outliers = result.getOutlierDetails();
		assertEquals(outliers.size(), 1);
		assertEquals(outliers.get("00/00/0000"), Long.valueOf(zeroes));
		assertEquals(result.getMatchCount(), inputs.length - zeroes);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}/\\d{2}/\\d{4}");
		assertEquals(result.getConfidence(), 1 - (double)zeroes/result.getSampleCount());
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void longAsDatewithErrors() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Date");
		analysis.setDebug(1);

		final String inputs[] = {
				"20050915", "20050915", "20051020", "20060112", "20060201",
				"20060201", "20060223", "20060700", "20060908", "20061100",
				"20061000", "20061115", "20061200", "20070110", "20070305",
				"20070424", "20070717", "20070919", "20070920", "20070921",
				"20080124", "20080827", "20081007", "20081021", "20081100",
				"20081203", "20081215", "20090115", "20090115", "20090128",
				"20090909", "20091109", "20100205", "20100911", "20100922",
				"201207XX", "20121101"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "yyyyMMdd");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 1);
		assertEquals(result.getInvalidCount(), 5);
		assertEquals(result.getMatchCount(), inputs.length - 6);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{8}");
		assertNull(result.checkCounts());

		// TODO TestSupport.checkHistogram(result, 10);
		// TODO TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void anotherDateSwitcher() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("INACTIVE DATE");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String inputs[] = {
				"7:11:41.00", "7:11:47.00", "7:11:51.00", "7:11:58.00", "8:11:07.00", "8:11:10.00", "8:11:11.00", "8:11:16.00",
				"8:11:22.00", "8:11:25.00", "8:11:27.00", "8:11:32.00", "8:11:32.00", "8:11:39.00", "8:11:41.00", "8:11:50.00",
				"8:11:51.00", "8:11:55.00", "8:11:55.00", "8:11:59.00", "9:11:01.00", "9:11:07.00", "9:11:13.00", "9:11:40.00",
				"9:11:48.00", "10:11:09.0", "10:11:17.0", "10:11:48.0", "11:11:22.0", "13:11:30.0", "13:11:42.0", "14:11:00.0",
				"14:11:30.0", "14:11:45.0", "15:11:46.0", "19:11:15.0", "20:11:58.0", "21:11:28.0", "21:11:47.0", "22:11:01.0",
				"22:11:03.0", "22:11:09.0", "22:11:12.0", "22:11:13.0", "22:11:34.0", "22:11:37.0"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.LOCALTIME);
		assertEquals(result.getTypeModifier(), "H:mm:ss.S{1,2}");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2}:\\d{2}:\\d{2}\\.\\d{1,2}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void localeDateTest() throws IOException, FTAException {
		final int testSet = new SecureRandom().nextInt(10);
		final Locale[] locales = DateFormat.getAvailableLocales();
//		Locale[] locales = new Locale[] {Locale.forLanguageTag("nn")};

		final String testCases[] = {
				"yyyy MM dd", "yyyy M dd", "yyyy MM d", "yyyy M d",
				"dd MMM yyyy", "d MMM yyyy", "dd-MMM-yyyy", "d-MMM-yyyy", "dd/MMM/yyyy", "d/MMM/yyyy",
				"yyyyMMdd'T'HHmmss'Z'", "yyyyMMdd'T'HHmmss", "yyyyMMdd'T'HHmmssxx", "yyyyMMdd'T'HHmmssxx",
				"dd MMMM yyyy", "d MMMM yyyy", "dd-MMMM-yyyy", "d-MMMM-yyyy",
				"dd/MMMM/yyyy", "d/MMMM/yyyy", "dd MMM yy", "d MMM yy", "dd-MMM-yy",
				"d-MMM-yy", "dd/MMM/yy", "d/MMM/yy",
				"yyyyMMdd'T'HHmmss.SSSxx", "yyyyMMdd'T'HHmmss.SSSxx", "dd/MMM/yy h:mm a",
				"dd/MMM/yy hh:mm a",
				"MMM dd, yyyy", "MMM d, yyyy", "MMM dd yyyy", "MMM d yyyy", "MMM-dd-yyyy", "MMM-d-yyyy",
				"MMMM dd, yyyy", "MMMM d, yyyy", "MMMM dd yyyy", "MMMM d yyyy", "MMMM-dd-yyyy", "MMMM-d-yyyy",
//				"EEE MMM dd HH:mm:ss OOOO yyyy"
//				"EEE MMM dd HH:mm:ss z yyyy"
		};

		final Set <String> problems = new HashSet<>();
		int countTests = 0;
		Set<String> localesTested = new HashSet<>();
		int countNotGregorian = 0;
		int countNotArabicNumerals = 0;
		int	countNoMonthAbbreviations = 0;
		int countNotSupported = 0;
		int countProblems = 0;

		Set<String> unsupportedLocales = new HashSet<>();

		for (final String testCase : testCases) {
			System.err.println(testCase + " ");

			nextLocale:
			for (final Locale locale : locales) {
				if (unsupportedLocales.contains(locale.toLanguageTag()))
					continue;
				// Only test one in 10 locales each run to speed up test!
				if (locale.hashCode() % 10 != testSet)
					continue;
				final Set<String> samples = new HashSet<>();
				LocalDate localDate = null;
				LocalDateTime localDateTime = null;
				OffsetDateTime offsetDateTime = null;
				ZonedDateTime zonedDateTime = null;
				final String testID = locale.toLanguageTag() + " (" + testCase + ") ...";
				final TextAnalyzer analysis = new TextAnalyzer("localeDateTest" + testID);
				analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
				analysis.configure(TextAnalyzer.Feature.NO_ABBREVIATION_PUNCTUATION, false);
				analysis.setLocale(locale);

				if (locale.toLanguageTag().equals("mgh"))
					continue;

				if (LocaleInfo.isSupported(locale) != null) {
					String reason = LocaleInfo.isSupported(locale);
					if (reason.contains("No support for locales that do not use Arabic numerals"))
						countNotArabicNumerals++;
					else if (reason.contains("No support for locales that do not use the Gregorian Calendar"))
						countNotGregorian++;
					else {
						System.err.printf("SKIPPING locale: %s, reason: %s%n", locale, LocaleInfo.isSupported(locale));
						countNotSupported++;
					}
					unsupportedLocales.add(locale.toLanguageTag());
					continue;
				}
				LocaleInfo localeInfo = LocaleInfo.getInstance(locale, analysis.getConfig().isEnabled(TextAnalyzer.Feature.NO_ABBREVIATION_PUNCTUATION));

				if (localeInfo.getShortMonths() == null || localeInfo.getShortMonths().size() == 0) {
						countNoMonthAbbreviations++;
						continue;
				}
				if (localeInfo.getShortMonths().keySet().equals(localeInfo.getMonths().keySet()) &&
					testCase.contains("MMMM")) {
					countNoMonthAbbreviations++;
					continue;
				}

//				System.err.println("TestCase: " + testCase + ", Locale: " + locale + ", country: " + locale.getCountry() +
//						", language: " + locale.getDisplayLanguage() + ", name: " + locale.toLanguageTag());

				countTests++;
				localesTested.add(locale.toLanguageTag());

				DateTimeFormatter formatter = DateTimeFormatter.ofPattern(testCase, locale);
				final FTAType type = SimpleDateMatcher.getType(testCase);

				if (type == null)
					System.err.println("TestCase: " + testCase + ", Locale: " + locale + ", country: " + locale.getCountry() +
							", language: " + locale.getDisplayLanguage() + ", name: " + locale.toLanguageTag());
				assertNotNull(type);

				if (type.equals(FTAType.LOCALDATE))
					localDate = LocalDate.now();
				else if (type.equals(FTAType.LOCALDATETIME))
					localDateTime = LocalDateTime.now();
				else if (type.equals(FTAType.OFFSETDATETIME))
					offsetDateTime = OffsetDateTime.now();
				else if (type.equals(FTAType.ZONEDDATETIME))
					zonedDateTime = ZonedDateTime.now();

				String sample = null;
				int locked = -1;

				try {
					for (int i = 0; i < 100; i++) {
						if (type.equals(FTAType.LOCALDATE))
							sample = localDate.format(formatter);
						else if (type.equals(FTAType.LOCALDATETIME))
							sample = localDateTime.format(formatter);
						else if (type.equals(FTAType.OFFSETDATETIME))
							sample = offsetDateTime.format(formatter);
						else if (type.equals(FTAType.ZONEDDATETIME))
							sample = zonedDateTime.format(formatter);

						samples.add(sample);
//						System.err.println(sample);
						if (analysis.train(sample) && locked == -1)
							locked = i;

						if (type.equals(FTAType.LOCALDATE)) {
							localDate = localDate.minusDays(100);
						}
						else if (type.equals(FTAType.LOCALDATETIME)) {
							localDateTime = localDateTime.minusDays(100).minusHours(1).minusMinutes(1).minusSeconds(1);
						}
						else if (type.equals(FTAType.OFFSETDATETIME)) {
							offsetDateTime = offsetDateTime.minusDays(100).minusHours(1).minusMinutes(1).minusSeconds(1);
						}
						else if (type.equals(FTAType.ZONEDDATETIME)) {
							zonedDateTime = zonedDateTime.minusDays(100).minusHours(1).minusMinutes(1).minusSeconds(1);
						}
					}
				}
				catch (FTAUnsupportedLocaleException e) {
					problems.add(testID + "Locale: '" + locale.toLanguageTag() + "' not supported: '" + e.getMessage() + "'");
					countNotSupported++;
					continue;
				}

				final TextAnalysisResult result = analysis.getResult();
				TestUtils.checkSerialization(analysis);

				if (locked != AnalysisConfig.DETECT_WINDOW_DEFAULT) {
					problems.add(testID + "Locked incorrect: " + locked);
					countProblems++;
					continue;
				}
				if (!result.getType().equals(type)) {
					problems.add(testID + "Type incorrect: '" + result.getType() + "' '" + type + "'");
					countProblems++;
					continue;
				}
				if (!result.getTypeModifier().equals(testCase)) {
					problems.add(testID + "TypeQualifer incorrect: '" + result.getTypeModifier() + "'");
					countProblems++;
					continue;
				}
				if (result.getSampleCount() != samples.size()) {
					problems.add(testID + "Samples != Samples " + result.getSampleCount() + " " + samples.size());
					countProblems++;
					continue;
				}
				if (result.getOutlierCount() != 0) {
					problems.add(testID + "Outliers: " + result.getOutlierCount());
					for (final Map.Entry<String, Long> outlier : result.getOutlierDetails().entrySet())
						System.err.println("'" + outlier.getKey() + "': " + outlier.getValue());
					countProblems++;
					continue;
				}
				if (result.getMatchCount() != samples.size()) {
					problems.add(testID + "Matches: " + result.getMatchCount());
					countProblems++;
					continue;
				}
				if (result.getNullCount() != 0) {
					problems.add(testID + "Nulls: " + result.getNullCount());
					countProblems++;
					continue;
				}
				assertEquals(result.getNullCount(), 0);

				// Even the UNK match the RE
				for (final String s : samples)
					if (!s.matches(result.getRegExp())) {
						problems.add(testID + "RE: " + result.getRegExp() + ", !match: " + s);
						countProblems++;
						continue nextLocale;
					}

			}
		}

		for (final String problem : problems) {
			System.err.println(problem);
		}

		logger.debug("{} locales tested, {} not Gregorian (skipped), {} not Arabic numerals, {} no Month abbr. (skipped), {} locales not supported, {} locales, {} failures (of {} tests).",
				localesTested.size(), countNotGregorian, countNotArabicNumerals, countNoMonthAbbreviations, countNotSupported, locales.length, countProblems, countTests);

		assertEquals(countProblems, 0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void novelApproachDateTime() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("SUB_ACTIVE_DATE");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String inputs[] = {
				"04OCT2012:16:27:03", "04OCT2012:16:27:03", "04OCT2012:16:27:03", "04OCT2012:16:27:03", "04OCT2012:16:27:03", "04OCT2012:16:27:03", "03DEC2012:16:23:30", "03DEC2012:16:23:30", "03DEC2012:16:33:46", "03DEC2012:16:57:14",
				"05DEC2012:11:31:06", "03DEC2012:16:23:30", "03DEC2012:16:23:30", "03DEC2012:16:33:46", "03DEC2012:16:57:14", "05DEC2012:11:31:06", "03DEC2012:16:23:30", "03DEC2012:16:23:30", "03DEC2012:16:23:30", "03DEC2012:16:23:30",
				"03DEC2012:16:23:30", "03DEC2012:16:23:30", "03DEC2012:16:33:46", "03DEC2012:16:33:46", "03DEC2012:16:33:46", "03DEC2012:16:57:14", "03DEC2012:16:57:14", "03DEC2012:16:57:14", "05DEC2012:11:31:06", "05DEC2012:11:31:06",
				"05DEC2012:11:31:06", "15DEC2012:21:02:35", "15DEC2012:21:02:35", "15DEC2012:21:02:35", "15DEC2012:21:02:35", "20FEB2013:22:25:28", "20FEB2013:22:25:28", "20FEB2013:22:25:28", "21FEB2013:16:55:29", "21FEB2013:16:55:29",
				"21FEB2013:16:55:29", "22FEB2013:14:10:14", "22FEB2013:14:10:14", "22FEB2013:14:10:14", "22FEB2013:14:10:14", "25FEB2013:10:16:21", "25FEB2013:10:16:21", "25FEB2013:10:16:21", "25FEB2013:10:16:21", "25FEB2013:10:30:27",
				"25FEB2013:10:30:27", "25FEB2013:10:30:27", "25FEB2013:10:30:27", "25FEB2013:13:13:29", "25FEB2013:13:13:29", "25FEB2013:13:13:29", "26FEB2013:08:10:28", "26FEB2013:08:10:28", "26FEB2013:08:10:28",
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "ddMMMyyyy:HH:mm:ss");
		assertEquals(result.getRegExp(), "\\d{2}\\p{IsAlphabetic}{3}\\d{4}:\\d{2}:\\d{2}:\\d{2}");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })

	public void novelApproachDate() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("SUB_ACTIVE_DATE_ONLY");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String inputs[] = {
				"04OCT2012", "04OCT2012", "04OCT2012", "04OCT2012", "04OCT2012", "04OCT2012", "03DEC2012", "03DEC2012", "03DEC2012", "03DEC2012",
				"05DEC2012", "03DEC2012", "03DEC2012", "03DEC2012", "03DEC2012", "05DEC2012", "03DEC2012", "03DEC2012", "03DEC2012", "03DEC2012",
				"03DEC2012", "03DEC2012", "03DEC2012", "03DEC2012", "03DEC2012", "03DEC2012", "03DEC2012", "03DEC2012", "05DEC2012", "05DEC2012",
				"05DEC2012", "15DEC2012", "15DEC2012", "15DEC2012", "15DEC2012", "20FEB2013", "20FEB2013", "20FEB2013", "21FEB2013", "21FEB2013",
				"21FEB2013", "22FEB2013", "22FEB2013", "22FEB2013", "22FEB2013", "25FEB2013", "25FEB2013", "25FEB2013", "25FEB2013", "25FEB2013",
				"25FEB2013", "25FEB2013", "25FEB2013", "25FEB2013", "25FEB2013", "25FEB2013", "26FEB2013", "26FEB2013", "26FEB2013",
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "ddMMMyyyy");
		assertEquals(result.getRegExp(), "\\d{2}\\p{IsAlphabetic}{3}\\d{4}");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicHours24() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("RETURN_TIME");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String inputs[] = {
				"19:40:03", "21:30:30", "18:00:00", "17:54:00", "10:03:03", "09:09:30", "07:00:00", "21:30:30", "22:30:05", "20:08:08",
				"17:40:23", "20:10:30", "18:00:00", "11:24:00", "11:04:03", "08:09:30", "07:00:05", "13:30:30", "22:30:05", "20:08:08",
				"15:40:23", "19:10:30", "14:00:00", "13:14:00", "12:03:03", "09:09:30", "07:00:00", "21:30:30", "22:30:05", "20:08:08",
				"13:40:13", "20:30:30", "17:00:00", "16:34:00", "11:13:03", "19:09:30", "07:11:11", "11:30:30", "22:30:05", "20:08:08",
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALTIME);
		assertEquals(result.getTypeModifier(), "HH:mm:ss");
		assertEquals(result.getRegExp(), "\\d{2}:\\d{2}:\\d{2}");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicHours24Change() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("RETURN_TIME");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String inputs[] = {
				"19:40:03", "21:30:30", "18:00:00", "17:54:00", "10:03:03", "19:09:30", "17:00:00", "21:30:30", "22:30:05", "20:08:08",
				"17:40:23", "20:10:30", "18:00:00", "11:24:00", "11:04:03", "18:09:30", "17:00:05", "13:30:30", "22:30:05", "20:08:08",
				"15:40:23", "19:10:30", "14:00:00", "13:14:00", "12:03:03", "19:09:30", "17:00:00", "21:30:30", "22:30:05", "20:08:08",
				"13:40:13", "20:30:30", "17:00:00", "16:34:00", "11:13:03", "19:09:30", "17:11:11", "11:30:30", "22:30:05", "20:08:08",
				"9:15:12"

		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALTIME);
		assertEquals(result.getTypeModifier(), "H:mm:ss");
		assertEquals(result.getRegExp(), "\\d{1,2}:\\d{2}:\\d{2}");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void timMBug1() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("TimMeagherBug1");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		analysis.train("01/12/2018 12:34:44+0000");
		analysis.train("12/01/2017 11:23:21-0100");
		analysis.train("06/05/1998 18:19:21+0100");
		analysis.train("  ");
		analysis.train(null);
		analysis.train("31/12/2015 08:05:55-0500");
		analysis.train("15/06/2019 23:15:31-0500");
		analysis.train("00/00/0000 00:00:00+0000");

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.OFFSETDATETIME);
		assertEquals(result.getTypeModifier(), "dd/MM/yyyy HH:mm:ssxx");
		assertEquals(result.getRegExp(), "\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}[-+][0-9]{4}");
		assertEquals(result.getBlankCount(), 1);
		assertEquals(result.getNullCount(), 1);
		assertEquals(result.getSampleCount(), 8);
		assertEquals(result.getMatchCount(), 5);
		assertEquals(result.getConfidence(), 1 - (double)1/(result.getSampleCount() - 2));
		assertNull(result.checkCounts());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void longAsDateWith0() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("stringField:string");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String inputs[] = {
				"20180112", "20171201", "19980605", "19990605", "20000605", "20010605", "20020605", "20030605", "20040605", "20050605",
				"20060605", "20070605", "20080605", "20090605", "20100605", "20110605", "20120605", "20130605", "20140605", "20150605",
				"20160605", "20170605", "20180605", "20190605", "20200605", "20210605", "20220605", "20230605", "20240605", "20250605",
				"20260605", "20270605", "", "19990101", "0", "20151231", "20190615",
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "yyyyMMdd");
		assertEquals(result.getRegExp(), "\\d{8}");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 1);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length - 2);
		assertEquals(result.getInvalidCount(), 1);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			if (!input.isEmpty() && !"0".equals(input))
				assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void dottedDates() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("dottedDate");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String inputs[] = {
				"1.1.2011", "1.1.2012", "1.1.2013", "1.1.2014", "1.1.2015", "1.1.2016", "1.10.2011", "1.10.2012", "1.10.2013", "1.10.2014",
				"1.10.2015", "1.11.2011", "1.11.2012", "1.15.2013", "1.11.2014", "1.11.2015", "1.12.2011", "1.12.2012", "1.12.2013", "1.12.2014",
				"9.6.2014", "9.6.2015", "9.6.2016", "9.7.2011", "9.7.2012", "9.7.2013", "9.7.2014", "9.7.2015", "9.7.2016", "9.8.2011",
				"9.8.2012", "9.8.2013", "9.8.2014", "9.8.2015", "9.9.2011", "9.9.2012", "9.9.2013", "9.9.2014", "9.9.2015",
				"1.1.2011.12.12.2012",
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "M.d.yyyy");
		assertEquals(result.getRegExp(), "\\d{1,2}\\.\\d{1,2}\\.\\d{4}");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length - 1);
		assertEquals(result.getConfidence(), 1.0 - (double)1/result.getSampleCount());
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (int i = 0; i < inputs.length - 1; i++)
			if (!inputs[i].isEmpty())
				assertTrue(inputs[i].matches(result.getRegExp()), inputs[i]);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void colonDates_ddMMyyyy() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("colonDate");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String inputs[] = {
				"16:08:2014", "16:05:2018", "15:08:2013", "16:08:2043", "15:08:2043",
				"16:08:2013", "15:08:2043", "16:08:2003", "14:08:2043", "16:08:2013",
				"14:08:2043", "16:08:2003", "14:05:2043", "16:05:2013", "14:05:2003",
				"16:05:2043", "14:05:2043", "16:05:2013", "14:05:2003", "13:05:2043",
				"13:05:2013", "16:05:2003",
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "dd:MM:yyyy");
		assertEquals(result.getRegExp(), "\\d{2}:\\d{2}:\\d{4}");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			if (!input.isEmpty())
				assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void colonDates_MMddyyyy() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("colonDates_MMddyyyy");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String inputs[] = {
				"08:16:2014", "05:16:2018", "08:15:2013", "08:16:2043", "08:15:2043", "08:16:2013",
				"08:15:2043", "08:16:2003", "08:14:2043", "08:16:2013", "08:14:2043", "08:16:2003",
				"05:14:2043", "05:16:2013", "05:14:2003", "05:16:2043", "05:14:2043", "05:16:2013",
				"05:14:2003", "05:13:2043", "05:13:2013", "05:16:2003",
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "MM:dd:yyyy");
		assertEquals(result.getRegExp(), "\\d{2}:\\d{2}:\\d{4}");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			if (!input.isEmpty())
				assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void shortDates_MM_slash_yyyy() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("shortDates_MM_yyyy");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String inputs[] = {
				"08/2014", "05/2018", "08/2013", "08/2043", "08/2043", "08/2013",
				"08/2043", "08/2003", "08/2043", "08/2013", "08/2043", "08/2003",
				"05/2043", "05/2013", "05/2003", "05/2043", "05/2043", "05/2013",
				"05/2003", "05/2043", "05/2013", "05/2003",
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "MM/yyyy");
		assertEquals(result.getRegExp(), "\\d{2}/\\d{4}");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		final DateTimeFormatter formatter = new DateTimeParser().ofPattern(result.getTypeModifier());
		for (final String input : inputs) {
			if (!input.isEmpty()) {
				assertTrue(input.matches(result.getRegExp()), input);
				try {
					LocalDate.parse(input, formatter);
				}
				catch (DateTimeParseException e) {
					fail("Parse failed" + e);
				}
			}
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void shortDates_MM_dash_yyyy() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("shortDates_MM_yyyy");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String inputs[] = {
				"08-2014", "05-2018", "08-2013", "08-2043", "08-2043", "08-2013",
				"08-2043", "08-2003", "08-2043", "08-2013", "08-2043", "08-2003",
				"05-2043", "05-2013", "05-2003", "05-2043", "05-2043", "05-2013",
				"05-2003", "05-2043", "05-2013", "05-2003",
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "MM-yyyy");
		assertEquals(result.getRegExp(), "\\d{2}-\\d{4}");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		final DateTimeFormatter formatter = new DateTimeParser().ofPattern(result.getTypeModifier());
		for (final String input : inputs) {
			if (!input.isEmpty()) {
				assertTrue(input.matches(result.getRegExp()), input);
				try {
					LocalDate.parse(input, formatter);
				}
				catch (DateTimeParseException e) {
					fail("Parse failed" + e);
				}
			}
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void shortDates_yyyy_slash_MM() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("shortDates_yyyy_MM");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String inputs[] = {
				"2014/08", "2018/11", "2013/11", "2043/08", "2043/01", "2013/03",
				"2043/03", "2003/11", "2043/03", "2013/04", "2043/03", "2003/01",
				"2043/08", "2013/08", "2003/03", "2043/04", "2043/03", "2013/01",
				"2003/03", "2043/11", "2013/08", "2003/04",
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "yyyy/MM");
		assertEquals(result.getRegExp(), "\\d{4}/\\d{2}");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		final DateTimeFormatter formatter = new DateTimeParser().ofPattern(result.getTypeModifier());
		for (final String input : inputs) {
			if (!input.isEmpty()) {
				assertTrue(input.matches(result.getRegExp()), input);
				try {
					LocalDate.parse(input, formatter);
				}
				catch (DateTimeParseException e) {
					fail("Parse failed" + e);
				}
			}
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void shortDates_yyyy_dash_MM() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("shortDates_yyyy_MM");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String inputs[] = {
				"2014-08", "2018-05", "2013-07", "2043-09", "2043-11", "2013-09",
				"2043-09", "2003-01", "2043-09", "2013-11", "2043-11", "2003-02",
				"2043-01", "2013-02", "2003-02", "2043-05", "2043-02", "2013-11",
				"2003-02", "2043-01", "2013-02", "2003-09",
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "yyyy-MM");
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		final DateTimeFormatter formatter = new DateTimeParser().ofPattern(result.getTypeModifier());
		for (final String input : inputs) {
			if (!input.isEmpty()) {
				assertTrue(input.matches(result.getRegExp()), input);
				try {
					LocalDate.parse(input, formatter);
				}
				catch (DateTimeParseException e) {
					fail("Parse failed" + e);
				}
			}
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void doubleStep() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("doubleStep");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String inputs[] = {
				"30.12.2011", "28.11.2012", "21.12.2013", "22.10.2014", "23.11.2015", "24.12.2016", "25.10.2011", "26.10.2012", "27.10.2013", "28.10.2014",
				"20.12.2011", "18.11.2012", "11.12.2013", "12.10.2014", "13.11.2015", "14.12.2016", "15.10.2011", "16.10.2012", "17.10.2013", "18.10.2014",
				"1.1.2015"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "d.M.yyyy");
		assertEquals(result.getRegExp(), "\\d{1,2}\\.\\d{1,2}\\.\\d{4}");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			if (!input.isEmpty())
				assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void quadrupleStep() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("quadrupleStep");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String inputs[] = {
				"30.12.2011 16:40:00.100", "28.11.2012 16:40:00.101", "21.12.2013 16:40:00.102", "22.10.2014 16:40:00.103", "23.11.2015 16:40:00.104",
				"24.12.2016 16:40:00.105", "25.10.2011 16:40:00.106", "26.10.2012 16:40:00.107", "27.10.2013 16:40:00.108", "28.10.2014 16:40:00.109",
				"20.12.2011 16:40:00.110", "18.11.2012 16:40:00.111", "11.12.2013 16:40:00.112", "12.10.2014 16:40:00.113", "13.11.2015 16:40:00.114",
				"14.12.2016 16:40:00.117", "15.10.2011 16:40:00.118", "16.10.2012 16:40:00.119", "17.10.2013 16:40:00.120", "18.10.2014 16:40:00.121",
				"1.1.2015 6:40:00.12"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "d.M.yyyy H:mm:ss.S{1,3}");
		assertEquals(result.getRegExp(), "\\d{1,2}\\.\\d{1,2}\\.\\d{4} \\d{1,2}:\\d{2}:\\d{2}\\.\\d{1,3}");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			if (!input.isEmpty())
				assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void intuitDateddMMyyyyHHmmss() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Settlement_Errors", DateResolutionMode.MonthFirst);
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		analysis.train("2/7/2012 06:24:47");
		analysis.train("2/7/2012 09:44:04");
		analysis.train("2/7/2012 06:21:26");
		analysis.train("2/7/2012 06:21:30");
		analysis.train("2/7/2012 06:21:31");
		analysis.train("2/7/2012 06:21:34");
		analysis.train("2/7/2012 06:21:38");
		analysis.train("1/7/2012 23:16:14");
		analysis.train("19/7/2012 17:49:53");
		analysis.train("19/7/2012 17:49:54");
		analysis.train("18/7/2012 09:57:17");
		analysis.train("19/7/2012 17:48:37");
		analysis.train("19/7/2012 17:49:54");
		analysis.train("19/7/2012 17:46:22");
		analysis.train("19/7/2012 17:49:05");
		analysis.train("2/7/2012 06:21:43");
		analysis.train("2/7/2012 06:21:50");
		analysis.train("2/7/2012 06:21:52");
		analysis.train("2/7/2012 06:21:55");
		analysis.train("2/7/2012 06:21:56");
		analysis.train("20/7/2012 17:30:45");
		analysis.train("19/7/2012 17:46:22");
		analysis.train("2/7/2012 05:57:32");
		analysis.train("19/7/2012 17:45:55");
		analysis.train("20/7/2012 17:30:48");
		analysis.train("1/7/2012 18:33:18");
		analysis.train("1/7/2012 18:27:15");
		analysis.train("1/7/2012 18:25:35");
		analysis.train("1/7/2012 18:31:19");
		analysis.train("1/7/2012 18:36:04");
		analysis.train("1/7/2012 19:20:45");
		analysis.train("1/7/2012 19:20:54");
		analysis.train("1/7/2012 19:19:59");
		analysis.train("1/7/2012 19:17:56");
		analysis.train("1/7/2012 19:19:09");
		analysis.train("1/7/2012 19:20:17");
		analysis.train("2/7/2012 06:22:29");
		analysis.train("2/7/2012 06:22:31");
		analysis.train("2/7/2012 06:22:34");

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "d/M/yyyy HH:mm:ss");
		assertEquals(result.getSampleCount(), 39);
		assertEquals(result.getMatchCount(), 39);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2}/\\d{1,2}/\\d{4} \\d{2}:\\d{2}:\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void commaFractions() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Settlement_Errors", DateResolutionMode.MonthFirst);
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String inputs[] = {
			"2017-10-01 00:00:01,189", "2017-10-01 00:00:02,926", "2017-10-01 00:00:03,285", "", "2017-10-01 00:00:26,263", "2017-10-01 00:00:26,427", "2017-10-01 00:00:26,430", "2017-10-01 00:00:26,659",
			"2017-10-01 00:00:26,742", "2017-10-01 00:00:27,001", "2017-10-01 00:00:27,004", "2017-10-01 00:00:27,376", "2017-10-01 00:00:28,194", "2017-10-01 00:00:28,218", "2017-10-01 00:00:28,799",
			"2017-10-01 00:00:29,771", "2017-10-01 00:00:30,015", "2017-10-01 00:00:30,586", "2017-10-01 00:00:30,875", "2017-10-01 00:00:31,042", "2017-10-01 00:00:31,138", "2017-10-01 00:00:31,428",
			"2017-10-01 00:00:31,433", "2017-10-01 00:00:31,660", "2017-10-01 00:00:32,525", "2017-10-01 00:00:32,785", "2017-10-01 00:00:32,788", "2017-10-01 00:00:33,054", "2017-10-01 00:00:33,067",
			"2017-10-01 00:00:33,275", "2017-10-01 00:00:33,733", "2017-10-01 00:00:33,820", "2017-10-01 00:00:33,924", "2017-10-01 00:00:33,937", "2017-10-01 00:00:34,690", "2017-10-01 00:00:35,068",
			"2017-10-01 00:00:35,108", "2017-10-01 00:00:35,170", "2017-10-01 00:00:35,174", "2017-10-01 00:00:35,177", "2017-10-01 00:00:35,177", "2017-10-01 00:00:35,178", "2017-10-01 00:00:35,178",
			"2017-10-01 00:00:35,179", "2017-10-01 00:00:35,179", "2017-10-01 00:00:35,179", "2017-10-01 00:00:35,180", "2017-10-01 00:00:35,180", "2017-10-01 00:00:35,180", "2017-10-01 00:00:35,181",
			"2017-10-01 00:00:35,181", "2017-10-01 00:00:35,813", "2017-10-01 00:00:36,131", "2017-10-01 00:00:36,619", "2017-10-01 00:00:37,221", "2017-10-01 00:00:37,921", "2017-10-01 00:00:38,256"
		};

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "yyyy-MM-dd HH:mm:ss,SSS");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 1);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length - 1);
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			if (!input.isEmpty())
				assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void variableLengthFractions() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Settlement_Errors", DateResolutionMode.MonthFirst);
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String inputs[] = {
				"2017-10-01 00:00:00.913", "2017-10-01 00:00:01.862", "2017-10-01 00:00:01.666", "2017-10-01 00:00:03.286", "2017-10-01 00:00:26.079", "2017-10-01 00:00:26.165",
				"2017-10-01 00:00:26.429", "2017-10-01 00:00:26.265", "2017-10-01 00:00:26.461", "2017-10-01 00:00:26.743", "2017-10-01 00:00:27.003", "2017-10-01 00:00:27.005",
				"2017-10-01 00:00:27.687", "2017-10-01 00:00:28.217", "2017-10-01 00:00:28.641", "2017-10-01 00:00:29.68", "2017-10-01 00:00:29.778", "2017-10-01 00:00:30.415",
				"2017-10-01 00:00:30.555", "2017-10-01 00:00:30.59", "2017-10-01 00:00:30.875", "2017-10-01 00:00:31.29", "2017-10-01 00:00:31.139", "2017-10-01 00:00:31.433",
				"2017-10-01 00:00:32.247", "2017-10-01 00:00:32.525", "2017-10-01 00:00:32.787", "2017-10-01 00:00:31.993", "2017-10-01 00:00:33.066", "2017-10-01 00:00:32.789",
				"2017-10-01 00:00:33.5", "2017-10-01 00:00:33.734", "2017-10-01 00:00:33.594", "2017-10-01 00:00:33.936", "2017-10-01 00:00:34.576", "2017-10-01 00:00:34.697",
				"2017-10-01 00:00:34.911", "2017-10-01 00:00:32.124", "2017-10-01 00:00:35.173", "2017-10-01 00:00:35.177", "2017-10-01 00:00:35.177", "2017-10-01 00:00:35.178",
				"2017-10-01 00:00:35.178", "2017-10-01 00:00:35.178", "2017-10-01 00:00:35.179", "2017-10-01 00:00:35.179", "2017-10-01 00:00:35.18", "2017-10-01 00:00:35.18",
				"2017-10-01 00:00:35.18", "2017-10-01 00:00:35.181", "2017-10-01 00:00:35.181", "2017-10-01 00:00:35.685", "2017-10-01 00:00:35.816", "2017-10-01 00:00:36.461",
				"2017-10-01 00:00:36.648", "2017-10-01 00:00:37.706", "2017-10-01 00:00:37.969", "2017-10-01 00:00:37.924", "2017-10-01 00:00:38.257", "2017-10-01 00:00:38.539",
				"2017-10-01 00:00:39.574", "2017-10-01 00:00:39.849", "2017-10-01 00:00:40.113", "2017-10-01 00:00:40.373", "2017-10-01 00:00:41.415", "2017-10-01 00:00:42.281",
				"2017-10-01 00:00:42.801", "2017-10-01 00:00:44.176", "2017-10-01 00:00:44.505", "2017-10-01 00:00:44.812", "2017-10-01 00:00:45.799", "2017-10-01 00:00:46.148",
				"2017-10-01 00:00:46.077", "2017-10-01 00:00:46.344", "2017-10-01 00:00:46.307", "2017-10-01 00:00:46.6", "2017-10-01 00:00:46.719", "2017-10-01 00:00:47.212",
				"2017-10-01 00:00:47.669", "2017-10-01 00:00:48.168", "2017-10-01 00:00:50.276", "2017-10-01 00:00:50.398", "2017-10-01 00:00:50.491", "2017-10-01 00:00:51.845",
				"2017-10-01 00:00:51.968", "2017-10-01 00:00:52.465", "2017-10-01 00:00:52.652", "2017-10-01 00:00:52.722", "2017-10-01 00:00:52.873", "2017-10-01 00:00:52.939",
				"2017-10-01 00:00:52.857", "2017-10-01 00:00:53.446", "2017-10-01 00:00:53.777", "2017-10-01 00:00:53.754", "2017-10-01 00:00:54.023", "2017-10-01 00:00:54.025"
		};

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "yyyy-MM-dd HH:mm:ss.S{1,3}");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{1,3}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			if (!input.isEmpty())
				assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void dateIssueAMPM() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("CREATED_ON");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String inputs[] = {
				"11/25/2010 11:13:38 AM",  "9/20/2010 7:31:26 AM", "9/17/2010 2:37:58 PM", "12/14/2010 11:08:17 AM",
				"10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM",
				"10/13/2010 1:17:04 PM","11/25/2010 11:13:38 AM", "11/25/2010 11:13:38 AM", "9/20/2010 7:31:26 AM",
				"9/17/2010 2:37:58 PM", "12/14/2010 11:08:17 AM", "10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM",
				"10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM", "9/20/2010 7:31:26 AM",
				"9/17/2010 2:37:58 PM", "12/14/2010 11:08:17 AM", "10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM",
				"10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2}/\\d{2}/\\d{4} \\d{1,2}:\\d{2}:\\d{2} (?i)(AM|PM)");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "M/dd/yyyy h:mm:ss a");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })

	public void dateSwitcher() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Contract Signed Date");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String inputs[] = {
				"3/14/2012", "3/14/2012", "6/30/2011", "3/14/2012", "3/14/2012", "3/14/2012",
				"3/14/2012", "3/14/2012", "3/14/2012", "3/14/2012", "3/14/2012", "3/14/2012",
				"3/14/2012", "6/30/2011", "6/30/2011", "6/30/2011", "6/30/2011", "6/30/2011",
				"6/30/2011", "9/11/2013", "10/2/2013", "10/2/2013", "10/2/2013", "10/2/2013",
				"10/2/2013", "10/2/2013", "2/18/2014", "2/18/2014", "2/18/2014", "6/2/2014",
				"6/2/2014", "6/2/2014", "6/2/2014", "6/2/2014", "10/17/2012"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2}/\\d{1,2}/\\d{4}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "M/d/yyyy");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	class SimpleResult {
		String regExp;
		String typeModifier;
		FTAType type;


		SimpleResult(final String regExp, final String typeModifier, final String typeString) {
			this.regExp = regExp;
			this.typeModifier = typeModifier;
			this.type = FTAType.valueOf(typeString.toUpperCase());
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void dqplus() throws IOException, FTAException {

		final String[] tests = {
			"MM/dd/yyyy", "MMM d yyyy", "M/dd/yyyy", "MM/dd/yy",
			"dd-MMM-yy", "dd-MMM-yyyy",
		    "MMMM dd, yyyy", "yyyy-MM-dd", "EEEE, MMMM, dd, yyyy",
		    "yyyy MMM dd", "yyyy/MM/dd", "dd/MM/yyyy HH:mm:ss", "dd-MM-yyyy HH:mm:ss",
		    "MMMM d yyyy hh:mm:ss aaa", "yyyy-MM-dd'T'HH:mm:ss",
		    "yyyy-MM-dd'T'HH:mm:ss.S", "yyyy/MM/dd HH:mm:ss.S",
		    "yyyy-MM-dd HH:mm:ss.S",
		    "MM/dd/yy h:mm:ss aaa",
		    "MM-dd-yy h:mm:ss aaa", "M/dd/yy HH:mm", "M-dd-yy HH:mm", "yyyy-MM-dd'T'HH:mm",
		    "yyyy-MM-dd'T'HH", "yyyyMMdd'T'HHmmss", "yyyyMMdd'T'HHmm", "yyyyMMdd'T'HH",
			"yyyyMMdd'T'HHmmssS",
			"M/d", "dd-MMM", "MM-yy", "MMMM-yy"
		};

		final Map<String, SimpleResult> results = new HashMap<>();
		results.put("MM/dd/yyyy", new SimpleResult("\\d{2}/\\d{2}/\\d{4}", "MM/dd/yyyy", "LocalDate"));
		results.put("MMM d yyyy", new SimpleResult("\\p{IsAlphabetic}{3} \\d{1,2} \\d{4}", "MMM d yyyy", "LocalDate"));
		results.put("M/dd/yyyy", new SimpleResult("\\d{1,2}/\\d{2}/\\d{4}", "M/dd/yyyy", "LocalDate"));
		results.put("MM/dd/yy", new SimpleResult("\\d{2}/\\d{2}/\\d{2}", "MM/dd/yy", "LocalDate"));
		results.put("dd-MMM-yy", new SimpleResult("\\d{2}-\\p{IsAlphabetic}{3}-\\d{2}", "dd-MMM-yy", "LocalDate"));
		results.put("dd-MMM-yyyy", new SimpleResult("\\d{2}-\\p{IsAlphabetic}{3}-\\d{4}", "dd-MMM-yyyy", "LocalDate"));
		results.put("MMMM dd, yyyy", new SimpleResult("\\p{IsAlphabetic}{3,9} \\d{2}, \\d{4}", "MMMM dd, yyyy", "LocalDate"));
		results.put("yyyy-MM-dd", new SimpleResult("\\d{4}-\\d{2}-\\d{2}", "yyyy-MM-dd", "LocalDate"));
		results.put("EEEE, MMMM, dd, yyyy", new SimpleResult(", \\p{IsAlphabetic}{3,9}, \\d{2}, \\d{4}", "EEEE, MMMM, dd, yyyy", "LocalDate"));
		results.put("yyyy MMM dd", new SimpleResult("\\d{4} \\p{IsAlphabetic}{3} \\d{2}", "yyyy MMM dd", "LocalDate"));
		results.put("yyyy/MM/dd", new SimpleResult("\\d{4}/\\d{2}/\\d{2}", "yyyy/MM/dd", "LocalDate"));
		results.put("dd/MM/yyyy HH:mm:ss", new SimpleResult("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}", "dd/MM/yyyy HH:mm:ss", "LocalDateTime"));
		results.put("dd-MM-yyyy HH:mm:ss", new SimpleResult("\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2}:\\d{2}", "dd-MM-yyyy HH:mm:ss", "LocalDateTime"));
		results.put("MMMM d yyyy hh:mm:ss aaa", new SimpleResult("\\p{IsAlphabetic}{3,9} \\d{1,2} \\d{4} \\d{2}:\\d{2}:\\d{2} (?i)(AM|PM)", "MMMM d yyyy hh:mm:ss a", "LocalDateTime"));
		results.put("yyyy-MM-dd'T'HH:mm:ss", new SimpleResult("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}", "yyyy-MM-dd'T'HH:mm:ss", "LocalDateTime"));
		results.put("yyyy-MM-dd'T'HH:mm:ss.S", new SimpleResult("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{1,3}", "yyyy-MM-dd'T'HH:mm:ss.S{1,3}", "LocalDateTime"));
		results.put("yyyy/MM/dd HH:mm:ss.S", new SimpleResult("\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{1,3}", "yyyy/MM/dd HH:mm:ss.S{1,3}", "LocalDateTime"));
		results.put("yyyy-MM-dd HH:mm:ss.S", new SimpleResult("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{1,3}", "yyyy-MM-dd HH:mm:ss.S{1,3}", "LocalDateTime"));
		results.put("MM/dd/yy h:mm:ss aaa", new SimpleResult("\\d{2}/\\d{2}/\\d{2} \\d{1,2}:\\d{2}:\\d{2} (?i)(AM|PM)", "MM/dd/yy h:mm:ss a", "LocalDateTime"));
		results.put("MM-dd-yy h:mm:ss aaa", new SimpleResult("\\d{2}-\\d{2}-\\d{2} \\d{1,2}:\\d{2}:\\d{2} (?i)(AM|PM)", "MM-dd-yy h:mm:ss a", "LocalDateTime"));
		results.put("M/dd/yy HH:mm", new SimpleResult("\\d{1,2}/\\d{2}/\\d{2} \\d{2}:\\d{2}", "M/dd/yy HH:mm", "LocalDateTime"));
		results.put("M-dd-yy HH:mm", new SimpleResult("\\d{1,2}-\\d{2}-\\d{2} \\d{2}:\\d{2}", "M-dd-yy HH:mm", "LocalDateTime"));
		results.put("yyyy-MM-dd'T'HH:mm", new SimpleResult("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}", "yyyy-MM-dd'T'HH:mm", "LocalDateTime"));
		results.put("yyyy-MM-dd'T'HH", new SimpleResult("\\d{4}-\\d{2}-\\d{2}T\\d{2}", "yyyy-MM-dd'T'HH", "LocalDateTime"));
		results.put("yyyyMMdd'T'HHmmss", new SimpleResult("\\d{8}T\\d{6}", "yyyyMMdd'T'HHmmss", "LocalDateTime"));
		results.put("yyyyMMdd'T'HHmm", new SimpleResult("\\d{8}T\\d{4}", "yyyyMMdd'T'HHmm", "LocalDateTime"));
		results.put("yyyyMMdd'T'HH", new SimpleResult("\\d{8}T\\d{2}", "yyyyMMdd'T'HH", "LocalDateTime"));

		for (final String test : tests) {
			final TextAnalyzer analysis = new TextAnalyzer("dqplus" + test);
			analysis.setLocale(Locale.forLanguageTag("en-US"));
			final String dateTimeFormat = test;
			final SimpleDateFormat sdf = new SimpleDateFormat(dateTimeFormat);
			final int sampleCount = 1000;
			String[] samples = new String[sampleCount];

			final Calendar calendar = Calendar.getInstance();

			for (int iters = 0; iters < samples.length; iters++) {
				samples[iters] = sdf.format(calendar.getTime());
				calendar.add(Calendar.HOUR, -1000);
				calendar.add(Calendar.MINUTE, 1);
				calendar.add(Calendar.SECOND, 1);
				calendar.add(Calendar.MILLISECOND, 17);
				analysis.train(samples[iters]);
			}
			final TextAnalysisResult result = analysis.getResult();
			TestUtils.checkSerialization(analysis);

			if (result.getTypeModifier() != null) {
				final SimpleResult expected = results.get(dateTimeFormat);
				if (expected == null)
					fail("expected is null");
				final String actual = result.getRegExp();
				if (actual == null)
					fail("actual is null");
				if (!actual.equals(expected.regExp))
					logger.debug("Format: '{}', expected: '{}', actual '{}'.", dateTimeFormat, expected.regExp, actual);
				assertEquals(result.getConfidence(), 1.0);
				assertEquals(result.getType(), expected.type);
				assertEquals(result.getTypeModifier(), expected.typeModifier);
				assertEquals(result.getSampleCount(), samples.length);
				assertEquals(result.getMatchCount(), samples.length);
				assertEquals(result.getNullCount(), 0);
			}
		}
	}

	public void _dateTimePerf(final boolean statisticsOn) throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("_dateTimePerf");
		if (!statisticsOn) {
			analysis.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);
			analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		}
		final String dateTimeFormat = "yyyy-MM-dd'T'HH:mm:ss";
		final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateTimeFormat, Locale.getDefault());
		final int sampleCount = 100_000_000;
		boolean saveOutput = false;
		BufferedWriter bw = null;
		String[] samples = new String[10000];

		if (saveOutput)
			bw = new BufferedWriter(new FileWriter("/tmp/dateTimePerf.csv"));

		LocalDateTime localDateTime = LocalDateTime.now();

		int iters;
		for (iters = 0; iters < samples.length; iters++) {
			samples[iters] = localDateTime.format(dtf);
			localDateTime = localDateTime.minusMinutes(1).minusSeconds(1);
		}

		long start = System.currentTimeMillis();

		// Run for about reasonable number of seconds
		final int seconds = 5;
		for (iters = 0; iters < sampleCount; iters++) {
			final String sample = samples[iters%samples.length];
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
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "yyyy-MM-dd'T'HH:mm:ss");
		assertNull(result.checkCounts());
		logger.info("Count {}, duration: {}ms, ~{} per second.", iters + 1, System.currentTimeMillis() - start, (iters  + 1)/seconds);

		// With Statistics & LogicalTypes
		//   - Count 9684201, duration: 10004ms, ~968,400 per second
		// No Statistics & No LogicalTypes
		//   - Count 18672901, duration: 10003ms, ~1,867,200 per second
		// No Parsing!
		//   - Count 24523501, duration: 10002ms, ~2,452,300 per second
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void basicMMMdcommayyyy() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicMMMdcommayyyy");
		Locale locale = Locale.forLanguageTag("en-US");
		analysis.setLocale(locale);
		final String pipedInput = "August 20, 2017|August 20, 2017|July 22, 2017|August 5, 2017|July 22, 2017|June 23, 2017|August 20, 2017|July 22, 2017|June 23, 2017|" +
				"May 25, 2017|August 20, 2017|July 22, 2017|June 23, 2017|May 25, 2017|April 26, 2017|August 20, 2017|July 22, 2017|June 23, 2017|" +
				"May 25, 2017|April 26, 2017|March 28, 2017|August 20, 2017|July 22, 2017|June 23, 2017|May 25, 2017|April 26, 2017|March 28, 2017|" +
				"February 27, 2017|August 20, 2017|July 22, 2017|June 23, 2017|May 15, 2017|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "MMMM d, yyyy");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), KnownTypes.PATTERN_ALPHA + "{3,9} \\d{1,2}, \\d{4}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
			assertNull(checkParseable(result, input, locale));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void inputZ() throws FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("inputZ");
		Locale locale = Locale.forLanguageTag("en-US");
		analysis.setLocale(locale);
		final String[] inputs = {
				"1995-02-28Z", "1994-02-28Z", "2003-02-28Z", "2004-02-29Z", "1991-02-28Z",
				"2008-05-31Z", "2002-02-28Z", "2008-05-31Z", "2003-02-28Z", "1993-02-28Z",
				"2001-02-28Z", "1993-02-28Z", "1995-02-28Z", "1996-02-29Z", "1995-02-28Z",
				"1993-02-28Z", "1998-02-28Z", "2004-02-29Z", "2007-02-28Z", "1990-02-28Z",
				"2008-05-31Z", "1996-02-29Z", "1990-02-28Z", "2006-02-28Z", "2010-12-31Z",
				"2006-02-28Z", "1998-02-28Z", "2001-02-28Z", "1965-02-28Z", "1995-02-28Z",
				"2006-02-28Z", "1990-02-28Z", "2007-10-31Z", "1969-12-31Z", "2009-12-31Z",
				"1985-02-28Z", "1986-02-28Z", "1987-02-28Z", "1988-02-29Z", "2001-02-28Z",
				"1988-02-29Z", "1965-02-28Z", "2001-02-28Z", "2003-02-28Z", "2009-02-28Z",
				"1978-02-28Z", "2008-12-31Z", "1994-02-28Z", "1995-02-28Z", "1996-02-29Z"
		};

		for (String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(),  "yyyy-MM-dd'Z'");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2}Z");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
			assertNull(checkParseable(result, input, locale));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void basicJapanese() throws FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicJapanese");
		Locale locale = Locale.forLanguageTag("ja-JP");
		analysis.setLocale(locale);
		final String[] inputs = {
				"2018年10月6日", "2019年4月30日", "2017年3月12日", "2008年4月30日", "2087年5月20日",
				"2019年6月10日", "2019年7月30日", "2019年4月30日", "2017年8月12日", "2019年4月30日",
				"2017年3月12日", "2008年4月30日", "2087年5月20日", "2019年6月10日", "2019年7月30日",
				"2019年4月30日", "2017年8月12日", "2019年4月30日", "2017年3月12日", "2008年4月30日",
				"2087年5月20日", "2019年6月10日", "2019年7月30日", "2019年4月30日", "2017年8月12日",
				"2019年4月30日", "2017年3月12日", "2008年4月30日", "2087年5月20日", "2019年6月10日",
				"2019年7月30日", "2019年4月30日", "2017年8月12日"
		};

		for (String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "yyyy年M月d日");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{4}年\\d{1,2}月\\d{1,2}日");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
			assertNull(checkParseable(result, input, locale));
		}
	}

//	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void japaneseEra() throws FTAException {

		final TextAnalyzer analysis = new TextAnalyzer("japaneseEra");
		Locale locale = Locale.forLanguageTag("ja-JP");
		analysis.setLocale(locale);
		final String[] inputs = {
				"平成12年", "平成13年", "平成14年",
				"平成15年", "平成16年", "明治23年",
				"昭和22年", "令和5年"
		};

		for (String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "yyyy年M月d日");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{4}年\\d{1,2}月\\d{1,2}日");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
			assertNull(checkParseable(result, input, locale));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void chineseAM() throws FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("japaneseEra");
		Locale locale = Locale.forLanguageTag("ja-JP");
		analysis.setLocale(locale);
		final String[] inputs = {
				"2015/1/5 上午 12:00:00", "2015/1/30 上午 12:00:00", "2014/12/30 上午 12:00:00",
				"2015/1/13 上午 12:00:00", "2015/1/20 上午 12:00:00", "2014/12/24 上午 12:00:00",
				"2014/12/29 上午 12:00:00", "2015/2/11 上午 12:00:00", "2015/1/6 上午 12:00:00",
				"2015/1/23 上午 12:00:00", "2015/1/28 上午 12:00:00", "2015/2/4 上午 12:00:00",
				"2015/1/28 上午 12:00:00", "2015/1/5 上午 12:00:00", "2015/1/15 上午 12:00:00",
				"2015/2/26 上午 12:00:00", "2014/12/24 上午 12:00:00", "2013/11/14 上午 12:00:00",
				"2015/2/24 上午 12:00:00"
		};

		for (String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "yyyy/M/d 上午 HH:mm:ss");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{4}/\\d{1,2}/\\d{1,2} 上午 \\d{2}:\\d{2}:\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
			assertNull(checkParseable(result, input, locale));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void bulgarianddMMyyyy() throws FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("bulgarian");
		Locale locale = Locale.forLanguageTag("bg-BG");
		analysis.setLocale(locale);
		final String[] inputs = {
				"14.02.2017г.", "10.01.2017г.", "02.02.2017г.", "07.02.2017г.", "16.02.2017г.",
				"28.02.2017г.", "01.03.2017г.", "23.03.2017г.", "28.03.2017г.", "30.03.2017г.",
				"04.04.2017г.", "06.04.2017г.", "11.04.2017г.", "18.04.2017г.", "20.04.2017г.",
				"20.04.2047г.", "20.04.2017г.", "25.04.2017г.", "02.05.2017г.", "03.05.2017г.",
				"04.05.2017г.", "22.05.2017г.", "05.06.2017г.", "06.06.2017г.", "08.06.2017г.",
				"15.06.2017г.", "18.07.2017г.", "25.07.2017г.", "03.08.2017г.", "10.08.2017г.",
				"18.08.2017г.", "21.08.2017г.", "04.09.2017г.", "07.09.2017г.", "21.09.2017г.",
				"26.09.2017г.", "28.09.2017г.", "12.10.2017г.", "17.10.2017г.", "24.10.2017г.",
				"26.10.2017г."
				//, "31.10.2017", "13.11.2017", "14.11.2017", "21.11.2017", "28.11.2017",
		};

		for (String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "dd.MM.yyyy'г.'");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}\\.\\d{2}\\.\\d{4}г\\.");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
			assertNull(checkParseable(result, input, locale));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void bulgariandNonDate() throws FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("bulgarian");
		analysis.setLocale(Locale.forLanguageTag("bg-BG"));
		final String[] inputs = {
				"A12/1174/16", "A12/1175/16", "A12/1176/16", "A12/1177/16", "A12/1178/16",
				"A12/1179/16", "A12/1180/16", "A12/1181/16", "A12/1182/16", "A12/1183/16",
				"A12/1184/16", "A12/1185/16", "A12/1186/16", "A12/1187/16", "A11/0086/16",
				"INFO/0139/16", "A12/0745/16", "A12/1120/16", "A12/1117/16", "A12/1115/16",
				"A12/1112/16", "A12/1111/16", "A12/1116/16", "A12/1110/16", "A11/0085/16",
				"INFO/0135/16", "INFO/0136/16", "INFO/0137/16", "A12/1137/16", "A12/1138/16"
		};

		for (String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.STRING);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void bulgarianddMMyy() throws FTAException {
		Locale bulgarian = Locale.forLanguageTag("bg-BG");
		final TextAnalyzer analysis = new TextAnalyzer("bulgarian", DateResolutionMode.DayFirst);
		analysis.setLocale(bulgarian);
		final String[] inputs = {
				"26.01.12г.", "05.04.05г.", "10.05.10г.", "05.04.05г.", "17.06.10г.",
				"05.04.05г.", "10.06.08г.", "05.04.05г.", "26.04.07г.", "05.04.05г.",
				"09.06.06г.", "05.04.05г.", "25.10.11г.", "26.09.06г.", "06.04.05г.",
				"26.06.12г.", "17.03.09г.", "06.04.05г.", "07.04.05г.", "27.10.09г.",
				"07.04.05г.", "26.02.08г.", "07.04.05г.", "06.07.10г.", "07.04.05г.",
				"15.06.11г.", "03.02.17г.", "12.08.16г.", "07.04.05г.", "08.04.05г.",
		};

		final DateTimeParser dtp = new DateTimeParser().withLocale(bulgarian).withDateResolutionMode(DateResolutionMode.DayFirst);
		for (String input : inputs) {
			assertEquals(dtp.determineFormatString(input), "dd.MM.yy'г.'");
			analysis.train(input);
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "dd.MM.yy'г.'");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}\\.\\d{2}\\.\\d{2}г\\.");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()), input);
			assertNull(checkParseable(result, input, bulgarian));
		}
	}


	protected static String checkParseable(final TextAnalysisResult result, final String input, final Locale locale) {
		final String formatString = result.getTypeModifier();
		final FTAType type = result.getType();
		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatString, locale);

		try {
			if (FTAType.LOCALTIME.equals(type))
				LocalTime.parse(input, formatter);
			else if (FTAType.LOCALDATE.equals(type))
				LocalDate.parse(input, formatter);
			else if (FTAType.LOCALDATETIME.equals(type))
				LocalDateTime.parse(input, formatter);
			else if (FTAType.ZONEDDATETIME.equals(type))
				ZonedDateTime.parse(input, formatter);
			else
				OffsetDateTime.parse(input, formatter);
		}
		catch (DateTimeParseException exc) {
			return "Java Should have successfully parsed: " + input;
		}

		return null;
	}



	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void basicDDMMMMYYYY() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicDDMMMMYYYY");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final String pipedInput = "25 July 2018|12 August 1984|10 January 2000|1 January 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|12 August 1984|10 January 2000|1 January 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|12 August 1984|10 May 2000|1 April 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|13 August 1984|10 January 2000|1 May 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|12 November 1984|10 October 2000|1 January 1970|16 June 1934|06 July 1961|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "d MMMM yyyy");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2} " + KnownTypes.PATTERN_ALPHA + "{3,9} \\d{4}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.PERFORMANCE, TestGroups.DATES })
	public void dateTimePerf() throws IOException, FTAException {
		_dateTimePerf(true);
	}

	@Test(groups = { TestGroups.PERFORMANCE, TestGroups.DATES })
	public void dateTimePerfNoStatistics() throws IOException, FTAException {
		_dateTimePerf(false);
	}
}
