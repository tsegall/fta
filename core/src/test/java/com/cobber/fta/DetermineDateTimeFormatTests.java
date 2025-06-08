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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.security.SecureRandom;
import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.cobber.fta.core.FTAMergeException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.Utils;
import com.cobber.fta.dates.DateTimeParser;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;
import com.cobber.fta.dates.DateTimeParserConfig;
import com.cobber.fta.dates.DateTimeParserResult;
import com.cobber.fta.dates.SimpleDateMatcher;

public class DetermineDateTimeFormatTests {
	private static final SecureRandom random = new SecureRandom();
	private final Logger logger = LoggerFactory.getLogger("com.cobber.fta");

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void allOptions() {
		// "H_mm","MM/dd/yy","dd/MM/yyyy","yyyy/MM/dd","yyyy-MM-dd'T'HH_mm_ssx",
		// "yyyy-MM-dd'T'HH_mm_ssxxx","yyyy-MM-dd'T'HH_mm_ssxxxxx","dd MMMM yyyy",
		// "dd-MMMM-yyyy","d MMMM yyyy","d-MMMM-yyyy","MMMM dd',' yyyy",
		// "MMMM d',' yyyy","MMMM dd yyyy","MMMM d yyyy","MMMM-dd-yyyy","MMMM-d-yyyy"
		// "2/13/98 9:57"
		final String pipedInput =
				"2017-12-28|18:59|9:59:59|18:59 2017-12-28|2017-12-28 18:59|18:59:59|9:59 2017-12-28|9:59|2017-12-28|" +
						"9:59:59 2017-12-28|2017 7 8|2017-12-28 9:59:59|18:59:59 2017-12-28|2017-12-28 9:59|7/28/39|39-7-28|" +
						"39/7/28|2017 7 28|" +
						"7-28-39|28/7/39|28-7-39|2017-12-28 18:59:59|2017-12-28|2017 12 8|2017-7-28|" +
						"2017/7/28|2017-12-28|2017-12-28 18:59 GMT|18:59 2017-12-28 GMT|7/28/2017|7-28-2017|28-7-2017|" +
						"28/7/2017|2017 12 28|39/12/28|39-12-28|2017-12-28|7-28-39 18:59|12/28/39|39-7-28 18:59|28/12/39|" +
						"7/28/39 18:59|28/7/39 18:59|28-12-39|39/7/28 18:59|2017-12-28|2017/12/28|12-28-39|9:59:59.3|" +
						"28-7-2017 18:59|18:59 7/28/39|2017-7-28 18:59|2017-12-28|18:59:59.3|2017-12-28 9:59:59 GMT|" +
						"2017-12-28|12/28/2017|2017-12-28 9:59 GMT|9:59 2017-12-28 GMT|12-28-2017|18:59:59.5959|" +
						"7-28-39 18:59:59|18:59:59.593|28-12-2017|2017-12-28|9:59:59 2017-12-28 GMT|39/7/28 18:59:59|" +
						"7-28-2017 18:59|18:59 7-28-39|39/12/28 18:59|2017/7/28 18:59|9:59:59.59|18:59 28/7/39|" +
						"9:57|12/14/98|20/12/2012|2012/12/12|2004-01-01T00:00:00+05|2004-01-01T00:00:00+05:00|" +
						"25 July 2018|25-July-2018|7 June 2017|7-June-2017|June 23, 2017|" +
						"August 8, 2017|August 18 2017|December 9 2017|January-14-2017|February-4-2017|";

		final DateTimeParser dtp = new DateTimeParser().withLocale(Locale.forLanguageTag("en-US"));
		assertEquals(dtp.determineFormatString("9:57"), "H:mm");
		final String inputs[] = pipedInput.split("\\|");
		final String fmts[] = new String[inputs.length];
		String text;
		final StringBuilder answer = new StringBuilder();
		LocalTime localTime = LocalTime.now();
		LocalDate localDate = LocalDate.now();
		LocalDateTime localDateTime = LocalDateTime.now();
		ZonedDateTime zonedDateTime = ZonedDateTime.now();
		OffsetDateTime offsetDateTime = OffsetDateTime.now();
		final Set<String> seen = new HashSet<>();
		final Set<Integer> ignore = new HashSet<>();

		final StringBuilder headerLine = new StringBuilder("RowID,");

		// Work out headers and which columns we want.
		for (int i = 0; i < inputs.length; i++) {
			fmts[i] = dtp.determineFormatString(inputs[i]);
			assertNotNull(fmts[i], inputs[i]);

			final String header = fmts[i].replace(':', '_').replace('\'', '_');

			if (!seen.add(header)) {
				ignore.add(i);
				continue;
			}
			headerLine.append('"').append(header).append('"');
			if (i + 1 < inputs.length)
				headerLine.append(',');
		}


		for (int rows = 0; rows < 30; rows++) {
			answer.append(rows).append(',');
			for (int i = 0; i < inputs.length; i++) {
				if (ignore.contains(i)) {
					continue;
				}
				final DateTimeParser det = new DateTimeParser().withLocale(Locale.forLanguageTag("en-US"));
				det.train(inputs[i]);
				final DateTimeParserResult result = det.getResult();
				final FTAType type = result.getType();
				try {
					final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(fmts[i]);
					if (FTAType.LOCALTIME.equals(type)) {
						text = localTime.format(formatter);
						localTime = localTime.minusHours(209);
					}
					else if (FTAType.LOCALDATE.equals(type)) {
						text = localDate.format(formatter);
						localDate = localDate.minusDays(29);
					}
					else if (FTAType.LOCALDATETIME.equals(type)) {
						text = localDateTime.format(formatter);
						localDateTime = localDateTime.minusHours(209);
					}
					else if (FTAType.ZONEDDATETIME.equals(type)) {
						text = zonedDateTime.format(formatter);
						zonedDateTime = zonedDateTime.minusHours(209);
					}
					else {
						text = offsetDateTime.format(formatter);
						offsetDateTime = offsetDateTime.minusHours(209);
					}
					answer.append('"').append(text).append('"');
					if (i + 1 < inputs.length)
						answer.append(',');
					else
						answer.append('\n');
				}
				catch (DateTimeParseException exc) {
					fail("Failure");
				}
			}
		}
		assertEquals(dtp.determineFormatString("9:57"), "H:mm");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitTimeOnly() {
		final DateTimeParser dtp = new DateTimeParser();

		assertNull(dtp.determineFormatString("12:02:1"));
		assertEquals(dtp.determineFormatString("9:57"), "H:mm");
		assertEquals(dtp.determineFormatString("12:57"), "HH:mm");
		assertEquals(dtp.determineFormatString("8:57:02"), "H:mm:ss");
		assertEquals(dtp.determineFormatString("12:57:02"), "HH:mm:ss");
		assertNull(dtp.determineFormatString(":57:02"));
		assertNull(dtp.determineFormatString("123:02"));
		assertNull(dtp.determineFormatString("12:023"));
		assertNull(dtp.determineFormatString("12:023:12"));
		assertNull(dtp.determineFormatString("12:0"));
		assertNull(dtp.determineFormatString("12:02:12:14"));
		assertNull(dtp.determineFormatString("12:02:124"));
		assertNull(dtp.determineFormatString("12:02:"));
		assertNull(dtp.determineFormatString("12::02"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void japanese() {
		final DateTimeParser dtp = new DateTimeParser().withLocale(Locale.JAPAN);

		assertNull(dtp.determineFormatString("資料：農林水産省「食育推進計画調査報告書」（平成29（2017）年3月公表）"));
		// No ERA support - so this should fail
		assertNull(dtp.determineFormatString("平成26（2014）年３月"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void dutch() {
		final DateTimeParser dtpMonthFirst = new DateTimeParser().withLocale(Locale.forLanguageTag("nl-NL")).withDateResolutionMode(DateResolutionMode.MonthFirst);

		assertEquals(dtpMonthFirst.determineFormatString("2015-08-01"), "yyyy-MM-dd");
		assertEquals(dtpMonthFirst.determineFormatString("2023-02-05 11:18:22.539921"), "yyyy-MM-dd HH:mm:ss.SSSSSS");


		final DateTimeParser dtpDayFirst = new DateTimeParser().withLocale(Locale.forLanguageTag("nl-NL")).withDateResolutionMode(DateResolutionMode.MonthFirst);

		assertEquals(dtpDayFirst.determineFormatString("2015-08-01"), "yyyy-MM-dd");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void dutchNonLocalized() {
		final DateTimeParser dtpMonthFirst = new DateTimeParser().withLocale(Locale.forLanguageTag("nl-NL"));

		assertEquals(dtpMonthFirst.determineFormatString("03/28/2013 12:00:00 AM"), "MM/dd/yyyy hh:mm:ss P");
	}


	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void chinese() {
		final DateTimeParser dtp = new DateTimeParser().withLocale(Locale.CHINA);

		// This is really incorrect - since '上午' is AM and so we really should have "yyyy/M/d a HH:mm:ss"
		assertEquals(dtp.determineFormatString("2015/1/5 上午 12:00:00"), "yyyy/M/d 上午 HH:mm:ss");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitDateOnlySlash() {
		final DateTimeParser dtp = new DateTimeParser();

		assertEquals(dtp.determineFormatString("2015/9/9"), "yyyy/?/?");
		assertEquals(dtp.determineFormatString("2015/9/10"), "yyyy/?/??");
		assertEquals(dtp.determineFormatString("2/12/98"), "?/??/yy");
		assertEquals(dtp.determineFormatString("2/2/02"), "?/?/yy");
		assertNull(dtp.determineFormatString("2/31/02"));
		assertEquals(dtp.determineFormatString("31/02/02"), "??/??/??");
		assertEquals(dtp.determineFormatString("12/12/98"), "??/??/yy");
		assertEquals(dtp.determineFormatString("14/12/98"), "dd/MM/yy");
		assertEquals(dtp.determineFormatString("12/14/98"), "MM/dd/yy");
		assertEquals(dtp.determineFormatString("12/12/2012"), "??/??/yyyy");
		assertEquals(dtp.determineFormatString("20/12/2012"), "dd/MM/yyyy");
		assertEquals(dtp.determineFormatString("11/15/2012"), "MM/dd/yyyy");
		assertEquals(dtp.determineFormatString("2012/12/13"), "yyyy/MM/dd");
		assertNull(dtp.determineFormatString("/57/02"));
		assertNull(dtp.determineFormatString("123/02"));
		assertNull(dtp.determineFormatString("12/023"));
		assertNull(dtp.determineFormatString("12/0"));
		assertNull(dtp.determineFormatString("12/02/1"));
		assertNull(dtp.determineFormatString("12/023/12"));
		assertNull(dtp.determineFormatString("12/02/"));
		assertNull(dtp.determineFormatString("12/02-99"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void stackOverflow_69360498() {
		final DateTimeParser dtp = new DateTimeParser().withDateResolutionMode(DateResolutionMode.DayFirst);

		assertEquals(dtp.determineFormatString("1970-01-01T00:00:00.00Z"), "yyyy-MM-dd'T'HH:mm:ss.SSX");
		assertEquals(dtp.determineFormatString("2021-09-20T17:27:00.000Z+02:00"), "yyyy-MM-dd'T'HH:mm:ss.SSSX");
		assertEquals(dtp.determineFormatString("08/07/2021"), "dd/MM/yyyy");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void testRubbish() {
		assertNull(new DateTimeParser().determineFormatString("hello44world{"));
		assertNull(new DateTimeParser().determineFormatString("02-0828S15"));
		assertNull(new DateTimeParser().determineFormatString("88-0828S7"));
		assertNull(new DateTimeParser().determineFormatString("01-0828S7"));
		assertNull(new DateTimeParser().determineFormatString("28-0828S7"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void unusualT() {
		final String formatString = (new DateTimeParser().determineFormatString("2018-06-26T15:27:50."));
		new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(formatString).toFormatter(Locale.getDefault());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void unusualDate() {
		final String formatString = (new DateTimeParser().determineFormatString("2009-12-01: WKV"));
		assertNull(formatString);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitDateOnlyDash() {
		final DateTimeParser dtp = new DateTimeParser();

		assertEquals(dtp.determineFormatString("2-12-98"), "?-??-yy");
		assertEquals(dtp.determineFormatString("12-12-98"), "??-??-yy");
		assertEquals(dtp.determineFormatString("14-12-98"), "dd-MM-yy");
		assertEquals(dtp.determineFormatString("12-14-98"), "MM-dd-yy");
		assertEquals(dtp.determineFormatString("12-12-2012"), "??-??-yyyy");
		assertEquals(dtp.determineFormatString("2012-12-13"), "yyyy-MM-dd");
		assertEquals(dtp.determineFormatString("2012-13-12"), "yyyy-dd-MM");
		assertNull(dtp.determineFormatString("20120-12-12"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void badTXOffsets() {
		final DateTimeParser dtp = new DateTimeParser();
		assertEquals(dtp.determineFormatString("2012-08-29 10:17:30 +12"), "yyyy-MM-dd HH:mm:ss x");
		assertNull(dtp.determineFormatString("2012-08-29 10:17:30 +19"));

		assertEquals(dtp.determineFormatString("2012-08-29 10:17:30 +00:30"), "yyyy-MM-dd HH:mm:ss xxx");
		assertNull(dtp.determineFormatString("2012-08-29 10:17:30 +00:63"));

		assertEquals(dtp.determineFormatString("2012-08-29 10:17:30 +00:30:30"), "yyyy-MM-dd HH:mm:ss xxxxx");
		assertNull(dtp.determineFormatString("2012-08-29 10:17:30 +00:30:63"));

		assertEquals(dtp.determineFormatString("2012-08-29 10:17:30 +003030"), "yyyy-MM-dd HH:mm:ss xxxx");
		assertNull(dtp.determineFormatString("2012-08-29 10:17:30 +003063"));

		assertNull(dtp.determineFormatString("2012-08-29 10:17:30 +0030300"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void goodTZOffsets() {
		DateTimeParser dtp = new DateTimeParser();
		final String xTest = "2012-08-29 10:17:30 +12";
		dtp.train(xTest);
		assertEquals(dtp.getResult().getFormatString(), "yyyy-MM-dd HH:mm:ss x");
		assertEquals(dtp.getResult().getRegExp(), "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} [-+][0-9]{2}([0-9]{2})?");
		assertTrue(xTest.matches(dtp.getResult().getRegExp()));

		dtp = new DateTimeParser();
		final String xxxTest = "2012-08-29 10:17:30 +00:30";
		dtp.train(xxxTest);
		assertEquals(dtp.getResult().getFormatString(), "yyyy-MM-dd HH:mm:ss xxx");
		assertEquals(dtp.getResult().getRegExp(), "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} [-+][0-9]{2}:[0-9]{2}");
		assertTrue(xxxTest.matches(dtp.getResult().getRegExp()));

		dtp = new DateTimeParser();
		final String xxxxxTest = "2012-08-29 10:17:30 +00:30:30";
		dtp.train(xxxxxTest);
		assertEquals(dtp.getResult().getFormatString(), "yyyy-MM-dd HH:mm:ss xxxxx");
		assertEquals(dtp.getResult().getRegExp(), "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} [-+][0-9]{2}:[0-9]{2}(:[0-9]{2})?");
		assertTrue(xxxxxTest.matches(dtp.getResult().getRegExp()));

		dtp = new DateTimeParser();
		final String xxxxTest = "2012-08-29 10:17:30 +003030";
		dtp.train(xxxxTest);
		assertEquals(dtp.getResult().getFormatString(), "yyyy-MM-dd HH:mm:ss xxxx");
		assertEquals(dtp.getResult().getRegExp(), "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} [-+][0-9]{4}([0-9]{2})?");
		assertTrue(xxxxTest.matches(dtp.getResult().getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void digits4() {
		final DateTimeParser dtp = new DateTimeParser();

		assertEquals(dtp.determineFormatString("2013"), "yyyy");
		assertEquals(dtp.determineFormatString("1913"), "yyyy");
		assertEquals(dtp.determineFormatString("1813"), "yyyy");
		assertNull(dtp.determineFormatString("1499"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void lineNoise() {
		final DateTimeParser dtp = new DateTimeParser();

		assertNull(dtp.determineFormatString("a quick Internet also reveals:<p>life:{3=a-x*4=a:2{+(0+&"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void digits6() {
		final DateTimeParser dtpNumericDayFirst = new DateTimeParser().withDateResolutionMode(DateResolutionMode.DayFirst);

		assertEquals(dtpNumericDayFirst.determineFormatString("201407"), "yyyyMM");
		assertNull(dtpNumericDayFirst.determineFormatString("201213"));


		final DateTimeParser dtpNonNumeric = new DateTimeParser().withNumericMode(false);

		assertNull(dtpNonNumeric.determineFormatString("201407"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void digits8() {
		final DateTimeParser dtpNumericDayFirst = new DateTimeParser().withDateResolutionMode(DateResolutionMode.DayFirst);

		assertEquals(dtpNumericDayFirst.determineFormatString("12122013"), "ddMMyyyy");
		assertEquals(dtpNumericDayFirst.determineFormatString("12182013"), "MMddyyyy");
		assertNull(dtpNumericDayFirst.determineFormatString("31112013"));
		assertNull(dtpNumericDayFirst.determineFormatString("11312013"));
		assertNull(dtpNumericDayFirst.determineFormatString("13282013"));
		assertNull(dtpNumericDayFirst.determineFormatString("18121812"));
		assertEquals(dtpNumericDayFirst.determineFormatString("18122013"), "ddMMyyyy");
		assertEquals(dtpNumericDayFirst.determineFormatString("20121213"), "yyyyMMdd");
		assertEquals(dtpNumericDayFirst.determineFormatString("19121213"), "yyyyMMdd");
		assertEquals(dtpNumericDayFirst.determineFormatString("20140722105203"), "yyyyMMddHHmmss");
		assertNull(dtpNumericDayFirst.determineFormatString("20121312"));

		final DateTimeParser dtpNumericMonthFirst = new DateTimeParser().withDateResolutionMode(DateResolutionMode.MonthFirst);

		assertEquals(dtpNumericMonthFirst.determineFormatString("12122013"), "MMddyyyy");
		assertEquals(dtpNumericMonthFirst.determineFormatString("12182013"), "MMddyyyy");
		assertEquals(dtpNumericMonthFirst.determineFormatString("18122013"), "ddMMyyyy");
		assertEquals(dtpNumericMonthFirst.determineFormatString("20121213"), "yyyyMMdd");
		assertEquals(dtpNumericMonthFirst.determineFormatString("20140722105203"), "yyyyMMddHHmmss");
		assertNull(dtpNumericMonthFirst.determineFormatString("20121312"));

		final DateTimeParser dtpNumericNone = new DateTimeParser().withDateResolutionMode(DateResolutionMode.None);

		assertEquals(dtpNumericNone.determineFormatString("12122013"), "????yyyy");
		assertEquals(dtpNumericNone.determineFormatString("12182013"), "MMddyyyy");
		assertEquals(dtpNumericNone.determineFormatString("18122013"), "ddMMyyyy");
		assertEquals(dtpNumericNone.determineFormatString("20121213"), "yyyyMMdd");
		assertEquals(dtpNumericDayFirst.determineFormatString("20140722105203"), "yyyyMMddHHmmss");
		assertNull(dtpNumericDayFirst.determineFormatString("20121312"));

		final DateTimeParser dtpNonNumeric = new DateTimeParser().withNumericMode(false);

		assertNull(dtpNonNumeric.determineFormatString("20121213"));
		assertNull(dtpNonNumeric.determineFormatString("20140722105203"), "yyyyMMddHHmmss");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void digits10() {
		final DateTimeParser dtpNumericDayFirst = new DateTimeParser().withDateResolutionMode(DateResolutionMode.DayFirst);

		assertEquals(dtpNumericDayFirst.determineFormatString("2014072210"), "yyyyMMddHH");
		assertNull(dtpNumericDayFirst.determineFormatString("2012131232"));


		final DateTimeParser dtpNonNumeric = new DateTimeParser().withNumericMode(false);

		assertNull(dtpNonNumeric.determineFormatString("2014072210"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void digits8Harder() {
		final DateTimeParser dtpNumericDayFirst = new DateTimeParser().withDateResolutionMode(DateResolutionMode.DayFirst);

		final String[] bad = { "13132000", "20000230", "20000230", "20010229", "20000431", "20000631", "20000931", "20001131" };
		for (final String test : bad)
			assertNull(dtpNumericDayFirst.determineFormatString(test));

		final String[] good = { "20001213", "20000229", "20010228", "20000430", "20000430", "20000630", "20000930", "20001130" };
		for (final String test : good)
			assertEquals(dtpNumericDayFirst.determineFormatString(test), "yyyyMMdd");

		assertEquals(dtpNumericDayFirst.determineFormatString("12302013"), "MMddyyyy");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void digits12Harder() {
		final DateTimeParser dtpNumericDayFirst = new DateTimeParser().withDateResolutionMode(DateResolutionMode.DayFirst);

		final String[] good = { "200012131212", "200002291212", "200102281212", "200004301212", "200004301212",
				"200006301212", "200009301212", "200011301212" };

		for (final String test : good)
			assertEquals(dtpNumericDayFirst.determineFormatString(test), "yyyyMMddHHmm");

		final String[] bad = { "200002301212", "200002301212", "200002301212", "200102291212", "200004311212",
				"200006311212", "200009311212", "200011311212", "200012132412", "200012132360", "189912132312", "210112132312" };

		for (final String test : bad)
			assertNull(dtpNumericDayFirst.determineFormatString(test));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void digits14Harder() {
		final DateTimeParser dtpNumericDayFirst = new DateTimeParser().withDateResolutionMode(DateResolutionMode.DayFirst);

		final String[] good = { "20001213121200", "20000229121200", "20010228121200",
				"20000430121200", "20000430121200", "20000630121200", "20000930121200", "20001130121200" };

		for (final String test : good)
			assertEquals(dtpNumericDayFirst.determineFormatString(test), "yyyyMMddHHmmss");

		final String[] bad = { "20000230121200", "20000230121200", "20000230121200", "20010229121200", "20000431121200",
				"20000631121200", "20000931121200", "20001131121200", "20001213241200", "20001213235960" };

		for (final String test : bad)
			assertNull(dtpNumericDayFirst.determineFormatString(test));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuit8601DD() {
		final DateTimeParser dtp = new DateTimeParser();

		assertEquals(dtp.determineFormatString("2004-01-01T00:00:00+05"), "yyyy-MM-dd'T'HH:mm:ssx");

		final DateTimeParser det = new DateTimeParser();
		final String sample = "2004-01-01T00:00:00+05";
		det.train(sample);
		det.train("      ");

		final DateTimeParserResult result = det.getResult();
		assertEquals(result.getFormatString(), "yyyy-MM-dd'T'HH:mm:ssx");

		final String regExp = result.getRegExp();
		assertEquals(regExp, "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[-+][0-9]{2}([0-9]{2})?");
		assertTrue(sample.matches(regExp));

		assertTrue(result.isValid("2004-01-01T00:00:00+05"));
		assertTrue(result.isValid("2012-03-04T19:22:10+08"));
		assertTrue(result.isValid("2012-03-04T19:22:10+0830"));
		assertFalse(result.isValid("2012-03-04T19:22:10+?08"));

		assertTrue(result.isValid8("2004-01-01T00:00:00+05"));
		assertTrue(result.isValid8("2012-03-04T19:22:10+08"));
		assertTrue(result.isValid8("2012-03-04T19:22:10+0830"));
		assertFalse(result.isValid8("2012-03-04T19:22:10+?08"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitMyyyy() {
		final DateTimeParser dtp = new DateTimeParser().withLocale(Locale.US);
		final String fmt = dtp.determineFormatString("5 2023");

		assertEquals(fmt, "M yyyy");
		final DateTimeParserResult result = DateTimeParserResult.asResult(fmt, DateResolutionMode.None, dtp.getConfig());

		assertTrue(result.isValid("2 1999"));
		assertTrue(result.isValid("2 3345"));
		assertFalse(result.isValid("3 19X2"));

		assertTrue(result.isValid8("2 1999"));
		assertTrue(result.isValid8("2 3345"));
		assertFalse(result.isValid8("3 19X2"));

		final DateTimeFormatter dtf = dtp.ofPattern(fmt);
		assertTrue(result.isPlausible("2 1999", dtf));
		assertFalse(result.isPlausible("2 3345", dtf));
		assertFalse(result.isPlausible("3 19X2", dtf));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void unsetFraction() {
		final DateTimeParser det = new DateTimeParser();
		det.train("12:45");
		det.train("12:38.444");

		final DateTimeParserResult result = det.getResult();
		assertEquals(result.getFormatString(), "HH:mm");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitHHmmddMyy() {
		final String pipedInput = "00:53 15/2/17|17:53 29/7/16|10:53 11/1/16|03:53 25/6/15|20:53 06/12/14|13:53 20/5/14|06:53 01/11/13|23:53 14/4/13|" +
				"16:53 26/9/12|09:53 10/3/12|02:53 23/8/11|19:53 03/2/11|12:53 18/7/10|05:53 30/12/09|22:53 12/6/09|15:53 24/11/08|" +
				"08:53 08/5/08|01:53 21/10/07|18:53 03/4/07|11:53 15/9/06|04:53 27/2/06|21:53 10/8/05|14:53 22/1/05|07:53 06/7/04|" +
				"00:53 19/12/03|17:53 01/6/03|10:53 13/11/02|03:53 27/4/02|20:53 08/10/01|13:53 22/3/01|";
		final String inputs[] = pipedInput.split("\\|");
		final DateTimeParser det = new DateTimeParser().withDateResolutionMode(DateResolutionMode.DayFirst);

		for (final String input : inputs)
			det.train(input);

		final DateTimeParserResult result = det.getResult();
		final String formatString = result.getFormatString();

		assertEquals(formatString, "HH:mm dd/M/yy");

		final String regExp = result.getRegExp();

		for (final String input : inputs) {
			assertTrue(input.matches(regExp));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void manyEyes1() {
		final DateTimeParser det = new DateTimeParser().withDateResolutionMode(DateResolutionMode.DayFirst).withLocale(Locale.forLanguageTag("en-US"));
		final String[] inputs = {
				"Fri 08 Jan 2010 14:34:29 +0000", "Fri 08 Jan 2010 14:34:29 +0000", "Fri 08 Jan 2010 14:42:51 +0000", "Fri 08 Jan 2010 14:44:15 +0000",
				"Fri 08 Jan 2010 14:44:26 +0000", "Fri 08 Jan 2010 14:44:51 +0000", "Fri 08 Jan 2010 14:46:13 +0000", "Fri 08 Jan 2010 14:48:16 +0000",
				"Fri 08 Jan 2010 14:48:29 +0000", "Fri 08 Jan 2010 14:49:58 +0000", "Fri 08 Jan 2010 14:49:59 +0000", "Fri 08 Jan 2010 14:51:13 +0000",
				"Fri 08 Jan 2010 14:51:15 +0000", "Fri 08 Jan 2010 14:51:57 +0000", "Fri 08 Jan 2010 14:52:37 +0000", "Fri 08 Jan 2010 14:56:53 +0000",
				"Fri 08 Jan 2010 14:57:57 +0000", "Fri 08 Jan 2010 15:02:11 +0000", "Fri 08 Jan 2010 15:03:17 +0000", "Fri 08 Jan 2010 15:06:53 +0000",
				"Fri 08 Jan 2010 15:08:08 +0000", "Fri 08 Jan 2010 15:08:17 +0000", "Fri 08 Jan 2010 15:09:00 +0000", "Fri 08 Jan 2010 15:10:09 +0000",
				"Fri 08 Jan 2010 15:10:12 +0000", "Fri 08 Jan 2010 15:10:37 +0000", "Fri 08 Jan 2010 15:10:52 +0000", "Fri 08 Jan 2010 15:10:56 +0000"
		};

		for (final String input : inputs)
			det.train(input);

		final DateTimeParserResult result = det.getResult();
		final String formatString = result.getFormatString();

		assertEquals(formatString, "EEE dd MMM yyyy HH:mm:ss x");

		final String regExp = result.getRegExp();

		for (final String input : inputs)
			assertTrue(input.matches(regExp));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void githubIssue10() {
		final Locale english = Locale.forLanguageTag("en-GB");

		final String value = "02.02.2020";
		DateTimeParser dateTimeParser = new DateTimeParser()
				.withLocale(english)
				.withDateResolutionMode(DateTimeParser.DateResolutionMode.Auto);
        dateTimeParser.train(value);
        DateTimeParserResult result = dateTimeParser.getResult();
        assertEquals(result.getFormatString(), "dd.MM.yyyy");

        final Locale us = Locale.forLanguageTag("en-US");

        dateTimeParser = new DateTimeParser()
                .withLocale(us)
                .withDateResolutionMode(DateTimeParser.DateResolutionMode.Auto);
        dateTimeParser.train(value);
        result = dateTimeParser.getResult();
        assertEquals(result.getFormatString(), "MM.dd.yyyy");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void manyEyes2() {
		final DateTimeParser det = new DateTimeParser().withDateResolutionMode(DateResolutionMode.DayFirst).withLocale(Locale.forLanguageTag("en-US"));
		final String[] inputs = {
				"Sunday, June  6, 2010 23:02:18 UTC", "Sunday, June  6, 2010 22:58:45 UTC", "Sunday, June  6, 2010 22:58:20 UTC",
				"Sunday, June  6, 2010 22:35:06 UTC", "Sunday, June  6, 2010 22:21:39 UTC", "Sunday, June  6, 2010 22:07:59 UTC",
				"Sunday, June  6, 2010 22:07:52 UTC", "Sunday, June  6, 2010 21:46:07 UTC", "Sunday, June  6, 2010 21:44:19 UTC",
				"Sunday, June  6, 2010 21:23:41 UTC", "Sunday, June  6, 2010 21:21:48 UTC", "Sunday, June  6, 2010 21:20:31 UTC",
				"Sunday, June  6, 2010 21:15:45 UTC", "Sunday, June  6, 2010 21:08:57 UTC", "Sunday, June  6, 2010 20:30:08 UTC",
				"Sunday, June  6, 2010 20:26:52 UTC", "Sunday, June  6, 2010 20:17:12 UTC", "Sunday, June  6, 2010 20:04:35 UTC",
				"Sunday, June  6, 2010 19:34:07 UTC", "Sunday, June  6, 2010 19:07:12 UTC", "Sunday, June  6, 2010 18:58:43 UTC",
				"Sunday, June  6, 2010 18:54:22 UTC", "Sunday, June  6, 2010 18:38:08 UTC", "Sunday, June  6, 2010 17:50:25 UTC",
				"Sunday, June  6, 2010 17:44:22 UTC", "Sunday, June  6, 2010 17:11:25 UTC", "Sunday, June  6, 2010 16:59:38 UTC",
				"Sunday, June  6, 2010 16:57:26 UTC", "Sunday, June  6, 2010 16:42:49 UTC", "Sunday, June  6, 2010 16:38:56 UTC"
		};

		for (final String input : inputs)
			det.train(input);

		final DateTimeParserResult result = det.getResult();
		final String formatString = result.getFormatString();

		assertEquals(formatString, "EEEE, MMMM ppd, yyyy HH:mm:ss z");

		final String regExp = result.getRegExp();

		for (final String input : inputs)
			assertTrue(input.matches(regExp), regExp);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void manyEyes3() {
		final DateTimeParser det = new DateTimeParser().withDateResolutionMode(DateResolutionMode.DayFirst).withLocale(Locale.forLanguageTag("en-US"));
		final String[] inputs = {
				"Sunday, June 6, 2010 23:02:18 UTC", "Sunday, June 6, 2010 22:58:45 UTC", "Sunday, June 6, 2010 22:58:20 UTC",
				"Sunday, June 6, 2010 22:35:06 UTC", "Sunday, June 6, 2010 22:21:39 UTC", "Sunday, June 6, 2010 22:07:59 UTC",
				"Sunday, June 6, 2010 22:07:52 UTC", "Sunday, June 6, 2010 21:46:07 UTC", "Sunday, June 6, 2010 21:44:19 UTC",
				"Sunday, June 6, 2010 21:23:41 UTC", "Sunday, June 6, 2010 21:21:48 UTC", "Sunday, June 6, 2010 21:20:31 UTC",
				"Sunday, June 6, 2010 21:15:45 UTC", "Sunday, June 6, 2010 21:08:57 UTC", "Sunday, June 6, 2010 20:30:08 UTC",
				"Sunday, June 6, 2010 20:26:52 UTC", "Sunday, June 6, 2010 20:17:12 UTC", "Sunday, June 6, 2010 20:04:35 UTC",
				"Sunday, June 6, 2010 19:34:07 UTC", "Sunday, June 6, 2010 19:07:12 UTC", "Sunday, June 6, 2010 18:58:43 UTC",
				"Sunday, June 6, 2010 18:54:22 UTC", "Sunday, June 6, 2010 18:38:08 UTC", "Sunday, June 6, 2010 17:50:25 UTC",
				"Sunday, June 6, 2010 17:44:22 UTC", "Sunday, June 6, 2010 17:11:25 UTC", "Sunday, June 6, 2010 16:59:38 UTC",
		};

		for (final String input : inputs)
			det.train(input);

		final DateTimeParserResult result = det.getResult();
		final String formatString = result.getFormatString();

		assertEquals(formatString, "EEEE, MMMM d, yyyy HH:mm:ss z");

		final String regExp = result.getRegExp();

		for (final String input : inputs)
			assertTrue(input.matches(regExp), regExp);
	}

	@Test(enabled=false, groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void manyEyes4() {
		final DateTimeParser det = new DateTimeParser().withDateResolutionMode(DateResolutionMode.DayFirst).withLocale(Locale.forLanguageTag("en-US"));
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

		for (final String input : inputs)
			det.train(input);

		final DateTimeParserResult result = det.getResult();
		final String formatString = result.getFormatString();

		assertEquals(formatString, "EEEE, MMMM dd, yyyy HH:mm:ss z");

		final String regExp = result.getRegExp();

		for (final String input : inputs)
			assertTrue(input.matches(regExp), regExp);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	/*
	 * So you would have thought that 'yyyy-MM-ddX' would have successfully round-tripped.  But it appears that it is happy to output
	 * a 'Z' when the h/m/s is zero but it is not happy to roundtrip if you use X and provide it as input a date like '1995-02-28Z'.
	 * This 'hack' returns a quoted Z which works on both the input and the output - with the consequence that you are told it is
	 * a LocalDate as opposed to a ZonedDateTime.
	 */
	public void inputZ() {
		final String DATE_FORMAT = "yyyy-MM-dd'Z'";
		final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
		final ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"));
		final String formattedString = zonedDateTime.format(dateFormatter);
		assertEquals(formattedString.charAt(formattedString.length()-1), 'Z');

		final LocalDate roundTrip = LocalDate.parse("2022-07-06Z", dateFormatter);
		assertEquals(roundTrip.getDayOfMonth(), 6);

		final DateTimeParser det = new DateTimeParser().withLocale(Locale.forLanguageTag("nl-NL"));
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

		for (final String input : inputs)
			det.train(input);

		final DateTimeParserResult result = det.getResult();
		final String formatString = result.getFormatString();

		assertEquals(formatString, "yyyy-MM-dd'Z'");
		assertEquals(result.getType(), FTAType.LOCALDATE);

		final String regExp = result.getRegExp();

		for (final String input : inputs) {
			assertTrue(input.matches(regExp), regExp);
			assertNull(checkParseable(result, input, Locale.forLanguageTag("nl-NL")));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void githubIssue7A() {
		final String[] french = { "Mars 25, 2021", "Févr 5, 2020", "Avr 1, 1999" };
		final String[] english = { "Mar 25, 2021", "Feb 5, 2020", "Apr 1, 1999" };

		// Simple French test
		final DateTimeParser dtpFrench = new DateTimeParser()
		    .withDateResolutionMode(DateTimeParser.DateResolutionMode.Auto)
		    .withLocale(Locale.FRENCH)
		    .withStrictMode(true);

		for (final String input : french)
			dtpFrench.train(input);

		final DateTimeParserResult frenchResult = dtpFrench.getResult();
		assertEquals(frenchResult.getFormatString(), "MMM d, yyyy");
		assertEquals(frenchResult.getLocale().toLanguageTag(), "fr");

		// Simple English test
		final DateTimeParser dtpEnglish = new DateTimeParser()
			    .withDateResolutionMode(DateTimeParser.DateResolutionMode.Auto)
			    .withLocale(Locale.ENGLISH)
			    .withStrictMode(true);

		for (final String input : english)
			dtpEnglish.train(input);

		final DateTimeParserResult englishResult = dtpEnglish.getResult();
		assertEquals(englishResult.getFormatString(), "MMM d, yyyy");
		assertEquals(englishResult.getLocale().toLanguageTag(), "en");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void githubIssue7B() {
		final String[] french = { "Mars 25, 2021", "Févr 5, 2020", "Avr 1, 1999" };

		// French input with French, then English as the locale
		final DateTimeParser dtpFrenchEnglish = new DateTimeParser()
			    .withDateResolutionMode(DateTimeParser.DateResolutionMode.Auto)
			    .withLocale(Locale.FRENCH, Locale.ENGLISH)
			    .withStrictMode(true);

		for (final String input : french)
			dtpFrenchEnglish.train(input);

		final DateTimeParserResult frenchEnglishResult = dtpFrenchEnglish.getResult();
		assertEquals(frenchEnglishResult.getFormatString(), "MMM d, yyyy");
		assertEquals(frenchEnglishResult.getLocale().toLanguageTag(), "fr");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void githubIssue7B_Det() {
		// French input with French, then English as the locale
		final DateTimeParser dtpFrenchEnglish = new DateTimeParser()
			    .withDateResolutionMode(DateTimeParser.DateResolutionMode.Auto)
			    .withLocale(Locale.FRENCH, Locale.ENGLISH)
			    .withStrictMode(true);

		assertEquals(dtpFrenchEnglish.determineFormatString("Mars 25, 2021"), "MMM dd, yyyy");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void githubIssue7C() {
		final String[] english = { "Mar 25, 2021", "Feb 5, 2020", "Apr 1, 1999" };

		// English input with French, then English as the locale
		final DateTimeParser dtpFrenchEnglish = new DateTimeParser()
			    .withDateResolutionMode(DateTimeParser.DateResolutionMode.Auto)
			    .withLocale(Locale.FRENCH, Locale.ENGLISH)
			    .withStrictMode(true);

		for (final String input : english)
			dtpFrenchEnglish.train(input);

		final DateTimeParserResult frenchEnglishResult = dtpFrenchEnglish.getResult();
		assertEquals(frenchEnglishResult.getFormatString(), "MMM d, yyyy");
		assertEquals(frenchEnglishResult.getLocale().toLanguageTag(), "en");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void githubIssue7C_Det() {
		// English input with French, then English as the locale
		final DateTimeParser dtpFrenchEnglish = new DateTimeParser()
			    .withDateResolutionMode(DateTimeParser.DateResolutionMode.Auto)
			    .withLocale(Locale.FRENCH, Locale.ENGLISH)
			    .withStrictMode(true);

		assertEquals(dtpFrenchEnglish.determineFormatString("Mar 25, 2021"), "MMM dd, yyyy");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void githubIssue7D() {
		final String[] french = { "Mars 25, 2021", "Févr 5, 2020", "Avr 1, 1999" };

		// French input with English, then French as the locale
		final DateTimeParser dtpEnglishFrench = new DateTimeParser()
			    .withDateResolutionMode(DateTimeParser.DateResolutionMode.Auto)
			    .withLocale(Locale.ENGLISH, Locale.FRENCH)
			    .withStrictMode(true);

		for (final String input : french)
			dtpEnglishFrench.train(input);

		final DateTimeParserResult englishFrenchResult = dtpEnglishFrench.getResult();
		assertEquals(englishFrenchResult.getFormatString(), "MMM d, yyyy");
		assertEquals(englishFrenchResult.getLocale().toLanguageTag(), "fr");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void githubIssue7E() {
		final String[] english = { "Mar 25, 2021", "Feb 5, 2020", "Apr 1, 1999" };

		// English input with English, then French as the locale
		final DateTimeParser dtpEnglishFrench = new DateTimeParser()
			    .withDateResolutionMode(DateTimeParser.DateResolutionMode.Auto)
			    .withLocale(Locale.ENGLISH, Locale.FRENCH)
			    .withStrictMode(true);

		for (final String input : english)
			dtpEnglishFrench.train(input);

		final DateTimeParserResult englishFrenchResult = dtpEnglishFrench.getResult();
		assertEquals(englishFrenchResult.getFormatString(), "MMM d, yyyy");
		assertEquals(englishFrenchResult.getLocale().toLanguageTag(), "en");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void githubIssue7F() {
		final String[] english = { "Mar 25, 2021", "Feb 5, 2020", "Apr 1, 1999" };

		// English input with German, then French as the locale
		final DateTimeParser dtpGermanFrench = new DateTimeParser()
			    .withDateResolutionMode(DateTimeParser.DateResolutionMode.Auto)
			    .withLocale(Locale.GERMAN, Locale.FRENCH)
			    .withStrictMode(true);

		for (final String input : english)
			dtpGermanFrench.train(input);

		final DateTimeParserResult englishFrenchResult = dtpGermanFrench.getResult();
		assertNull(englishFrenchResult);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void githubIssue7F_Det() {
		// English input with German, then French as the locale
		final DateTimeParser dtpGermanFrench = new DateTimeParser()
			    .withDateResolutionMode(DateTimeParser.DateResolutionMode.Auto)
			    .withLocale(Locale.GERMAN, Locale.FRENCH)
			    .withStrictMode(true);

		assertNull(dtpGermanFrench.determineFormatString("Mar 25, 2021"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void githubIssue7G() {
		final String[] english = { "Mar 25, 2021", "Feb 5, 2020", "Apr 1, 1999" };

		// English input with German, then French as the locale
		final DateTimeParser dtpGermanFrench = new DateTimeParser()
			    .withDateResolutionMode(DateTimeParser.DateResolutionMode.Auto)
			    .withLocale(Locale.GERMAN, Locale.FRENCH, Locale.ENGLISH)
			    .withStrictMode(true);

		for (final String input : english)
			dtpGermanFrench.train(input);

		final DateTimeParserResult englishFrenchResult = dtpGermanFrench.getResult();
		assertEquals(englishFrenchResult.getFormatString(), "MMM d, yyyy");
		assertEquals(englishFrenchResult.getLocale().toLanguageTag(), "en");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void xx() {
		final String pipedInput = "2016-10-10T17:11:58+0000|2016-10-10T17:11:58+0000|2016-10-10T17:11:58+0000|2016-10-10T17:11:58+0000|2016-10-10T17:11:58+0000|2016-10-10T17:12:06+0000|2016-10-10T17:12:06+0000|2016-10-10T17:12:06+0000|2016-10-10T17:12:06+0000|2016-11-18T12:42:45+0000|2016-11-18T12:42:45+0000|2016-11-18T12:42:45+0000|2016-11-18T12:42:45+0000|2017-08-09T15:29:22+0000|2017-11-16T13:03:00+0000|2017-11-16T13:03:00+0000|2017-11-16T13:03:00+0000|2017-11-16T13:03:00+0000|2018-04-03T00:00:00+0000|2018-04-03T00:00:00+0000|2018-04-03T00:00:00+0000|2018-04-03T00:00:00+0000|2018-04-03T00:00:00+0000|2018-04-03T00:00:00+0000|2018-04-03T00:00:00+0000|2018-04-03T00:00:00+0000|";
		final String inputs[] = pipedInput.split("\\|");
		final DateTimeParser det = new DateTimeParser().withDateResolutionMode(DateResolutionMode.DayFirst);

		for (final String input : inputs)
			det.train(input);

		final DateTimeParserResult result = det.getResult();
		final String formatString = result.getFormatString();

		assertEquals(formatString, "yyyy-MM-dd'T'HH:mm:ssxx");

		final String regExp = result.getRegExp();

		for (final String input : inputs) {
			assertTrue(input.matches(regExp));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void twentyRecords() throws IOException {
		final List<String> inputs = Arrays.asList(
				"1/3/11", "1/3/11", "7/25/11", "7/25/11", "7/25/11", "7/25/11", "7/25/11", "7/25/11",
				"7/25/11", "7/25/11", "7/25/11", "7/25/11", "7/25/11", "7/25/11", "7/25/11", "7/25/11",
				"7/25/11", "7/25/11", "9/17/08", "1-15-2011");
		final DateTimeParser dtp = new DateTimeParser().withDateResolutionMode(DateResolutionMode.MonthFirst);

		inputs.forEach(dtp::train);

		final DateTimeParserResult result = dtp.getResult();
		final String formatString = result.getFormatString();

		assertEquals(formatString, "M/d/yy");

		final String regExp = result.getRegExp();

		int matches = 0;
		for (final String input : inputs)
			if (input.matches(regExp))
				matches++;

		assertEquals(matches, inputs.size() - 1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuit8601DDDDDD() {
		final DateTimeParser dtp = new DateTimeParser();

		assertEquals(dtp.determineFormatString("2004-01-01T00:00:00+05:00:00"), "yyyy-MM-dd'T'HH:mm:ssxxxxx");

		final DateTimeParser det = new DateTimeParser();
		final String sample = "2004-01-01T00:00:00+05:00:00";
		det.train(sample);

		final DateTimeParserResult result = det.getResult();
		assertEquals(result.getFormatString(), "yyyy-MM-dd'T'HH:mm:ssxxxxx");

		final String regExp = result.getRegExp();
		assertEquals(regExp, "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[-+][0-9]{2}:[0-9]{2}(:[0-9]{2})?");
		assertTrue(sample.matches(regExp));

		assertTrue(result.isValid("2004-01-01T00:00:00+05:00:00"));
		assertTrue(result.isValid("2012-03-04T19:22:10+08:00:00"));
		assertFalse(result.isValid("2012-03-04T19:22:10+08:00:0"));
		assertFalse(result.isValid("2012-03-04T19:22:10+O8:00:00"));
		assertFalse(result.isValid("2012-03-04T19:22:10+08:60:00"));
		assertFalse(result.isValid("2012-03-04T19:22:10+08:00:60"));

		assertTrue(result.isValid8("2004-01-01T00:00:00+05:00:00"));
		assertTrue(result.isValid8("2012-03-04T19:22:10+08:00:00"));
		assertFalse(result.isValid8("2012-03-04T19:22:10+08:00:0"));
		assertFalse(result.isValid8("2012-03-04T19:22:10+O8:00:00"));
		assertFalse(result.isValid8("2012-03-04T19:22:10+08:00:60"));

		assertFalse(result.isValid("2004-01-01T00:00:00+19:00:00"));
		assertFalse(result.isValid8("2004-01-01T00:00:00+19:00:00"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void fullMonths() {
		final DateTimeParser dtp = new DateTimeParser().withLocale(Locale.forLanguageTag("en-US"));

		assertEquals(dtp.determineFormatString("25 July 2018"), "dd MMMM yyyy");
		assertEquals(dtp.determineFormatString("25-July-2018"), "dd-MMMM-yyyy");
		assertEquals(dtp.determineFormatString("7 June 2017"), "d MMMM yyyy");
		assertEquals(dtp.determineFormatString("7-June-2017"), "d-MMMM-yyyy");

		assertEquals(dtp.determineFormatString("June 23, 2017"), "MMMM dd, yyyy");
		assertEquals(dtp.determineFormatString("August 8, 2017"), "MMMM d, yyyy");
		assertEquals(dtp.determineFormatString("August 18 2017"), "MMMM dd yyyy");
		assertEquals(dtp.determineFormatString("December 9 2017"), "MMMM d yyyy");
		assertEquals(dtp.determineFormatString("January-14-2017"), "MMMM-dd-yyyy");
		assertEquals(dtp.determineFormatString("February-4-2017"), "MMMM-d-yyyy");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void yyyyMMddHHmmz() {
		final String pipedInput = "2017-08-24 12:10 EDT|2017-07-03 06:10 EDT|2017-05-12 00:10 EDT|2017-03-20 18:10 EDT|2016-07-02 12:10 EDT|" +
				"2017-01-27 11:10 EST|2016-12-06 05:10 EST|2016-10-15 00:10 EDT|2016-08-23 18:10 EDT|2016-05-11 06:10 EDT|";
		final String inputs[] = pipedInput.split("\\|");
		final DateTimeParser det = new DateTimeParser();

		for (final String input : inputs) {
			det.train(input);
		}

		final DateTimeParserResult result = det.getResult();

		final String formatString = result.getFormatString();

		assertEquals(formatString, "yyyy-MM-dd HH:mm z");

		final String regExp = result.getRegExp();

		for (final String input : inputs)
			assertTrue(input.matches(regExp));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void ddMyy() {
		final List<String> inputs = Arrays.asList(
				"02/2/17", "27/1/14", "21/1/11", "15/1/08", "08/1/05", "02/1/02", "27/12/98", "21/12/95", "14/12/92", "08/12/89", "00/00/00");
		final DateTimeParser dtp = new DateTimeParser().withDateResolutionMode(DateResolutionMode.DayFirst);

		inputs.forEach(dtp::train);

		final DateTimeParserResult result = dtp.getResult();

		final String formatString = result.getFormatString();

		assertEquals(formatString, "dd/M/yy");

		final String regExp = result.getRegExp();

		for (final String input : inputs) {
			assertTrue(input.matches(regExp));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void sillySep() {
		final String pipedInput = "02_02_2017|27_01_2014|21_01_2011|15_01_2008|08_01_2005|02_01_2002|27_12_1998|21_12_1995|14_12_1992|08_12_2009|";
		final String inputs[] = pipedInput.split("\\|");
		final DateTimeParser det = new DateTimeParser().withDateResolutionMode(DateResolutionMode.DayFirst);

		for (final String input : inputs) {
			det.train(input);
		}

		final DateTimeParserResult result = det.getResult();

		final String formatString = result.getFormatString();

		assertEquals(formatString, "dd_MM_yyyy");

		final String regExp = result.getRegExp();

		for (final String input : inputs) {
			assertTrue(input.matches(regExp));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void unqualifiedPM() {
		final DateTimeParser det = new DateTimeParser().withDateResolutionMode(DateResolutionMode.DayFirst);
		final String AMBIGUOUS = "08/01/2017 10:35:00.1234 AM +0000";

		for (int i = 0; i < 25; i++)
			det.train(AMBIGUOUS);

		final DateTimeParserResult result = det.getResult();

		final String formatString = result.getFormatString();

		assertEquals(formatString, "dd/MM/yyyy hh:mm:ss.SSSS a xx");

		final String regExp = result.getRegExp();

		assertTrue(AMBIGUOUS.matches(regExp));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void unixDateCommand(){
		final String pipedInput = "Thu Jul  2 09:48:00 PDT 2020|Wed Jul  1 10:00:56 PDT 2020|Thu Jul  2 04:56:56 PDT 2020|Wed Jul 22 09:48:56 PDT 2020|";
		final String inputs[] = pipedInput.split("\\|");

		final DateTimeParser dtp = new DateTimeParser().withDateResolutionMode(DateResolutionMode.DayFirst).withLocale(Locale.forLanguageTag("en-US"));

		for (final String input : inputs) {
			dtp.train(input);
		}

		final DateTimeParserResult result = dtp.getResult();

		final String formatString = result.getFormatString();

		assertEquals(formatString, "EEE MMM ppd HH:mm:ss z yyyy");

		final DateTimeFormatter formatter = dtp.ofPattern(result.getFormatString());

		final String regExp = result.getRegExp();

		for (final String input : inputs) {
			assertTrue(input.matches(regExp));
			ZonedDateTime.parse(input, formatter);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void EEE_With_Offset() {
		final DateTimeParser det = new DateTimeParser().withLocale(Locale.forLanguageTag("en-US"));
		det.train("Wed Apr 21 08:10:38 GMT+8 2021");
		final DateTimeParserResult result = det.getResult();
		assertEquals(result.getFormatString(), "EEE MMM dd HH:mm:ss O yyyy");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void fixedWidthDay() {
		final DateTimeParser dtp = new DateTimeParser().withLocale(Locale.forLanguageTag("en-US"));
		assertEquals(dtp.determineFormatString("Oct  1 2019 12:14AM"), "MMM ppd yyyy hh:mma");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void fixedWidthHour() {
		final DateTimeParser dtp = new DateTimeParser().withLocale(Locale.forLanguageTag("en-US"));
		assertEquals(dtp.determineFormatString("Oct 1 2019  1:53PM"), "MMM d yyyy pph:mma");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void EEE_With_Offset_mm() {
		final String sample = "Wed Apr 21 08:10:38 GMT-07:12 2021";
		final String format = "EEE MMM dd HH:mm:ss OOOO yyyy";
		final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format, Locale.US);
		OffsetDateTime.parse(sample, dtf);

		final DateTimeParser det = new DateTimeParser().withLocale(Locale.forLanguageTag("en-US"));
		det.train(sample);
		final DateTimeParserResult result = det.getResult();
		assertEquals(result.getFormatString(), format);

		assertTrue(result.isValid8(sample));
		assertTrue(result.isValid(sample));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void EEE_With_Offset_mmss() {
		final String sample = "Wed Apr 21 08:10:38 GMT-07:12:34 2021";
		final DateTimeParser dtp = new DateTimeParser().withLocale(Locale.forLanguageTag("en-US"));
		final DateTimeFormatter dtf = dtp.ofPattern("EEE MMM dd HH:mm:ss OOOO yyyy");

		try {
			ZonedDateTime.parse("Wed Apr 21 08:10:38 GMT-07:12:34 2021", dtf);
		}
		catch (DateTimeParseException e) {
			fail(e.getMessage());
		}

		dtp.train(sample);
		final DateTimeParserResult result = dtp.getResult();
		assertEquals(result.getFormatString(), "EEE MMM dd HH:mm:ss OOOO yyyy");

		assertTrue(result.isValid8(sample));
		assertTrue(result.isValid(sample));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void basicHMM() {
		final String pipedInput = "3:16|3:16|10:16|3:16|10:16|17:16|3:16|10:16|17:16|0:16|3:16|10:16|17:16|0:16|7:16|3:16|10:16|" +
				"17:16|0:16|7:16|14:16|3:16|10:16|17:16|0:16|7:16|14:16|21:16|3:16|10:16|17:16|0:16|7:16|14:16|" +
				"21:16|4:16|3:16|10:16|17:16|0:16|7:16|14:16|21:16|4:16|11:16|3:16|10:16|17:16|0:16|7:16|14:16|";
		final String inputs[] = pipedInput.split("\\|");
		final DateTimeParser det = new DateTimeParser();

		for (final String input : inputs) {
			det.train(input);
		}

		final DateTimeParserResult result = det.getResult();

		final String formatString = result.getFormatString();

		assertEquals(formatString, "H:mm");

		final String regExp = result.getRegExp();

		for (final String input : inputs) {
			assertTrue(input.matches(regExp));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void basicDTPDDMMMMYYYY() {
		final String pipedInput = "25 July 2018|12 August 1984|10 January 2000|1 January 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|12 August 1984|10 January 2000|1 January 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|12 August 1984|10 May 2000|1 April 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|13 August 1984|10 January 2000|1 May 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|12 November 1984|10 October 2000|1 January 1970|16 June 1934|06 July 1961|";
		final String inputs[] = pipedInput.split("\\|");
		final DateTimeParser det = new DateTimeParser().withLocale(Locale.forLanguageTag("en-US"));

		for (final String input : inputs)
			det.train(input);

		final DateTimeParserResult result = det.getResult();

		final String formatString = result.getFormatString();

		assertEquals(formatString, "d MMMM yyyy");

		final String regExp = result.getRegExp();

		for (final String input : inputs) {
			assertTrue(input.matches(regExp));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void basicAMPM() {
		final DateTimeParser dtp = new DateTimeParser().withLocale(Locale.forLanguageTag("en-US"));

		assertEquals(dtp.determineFormatString("09/Mar/17 3:14 PM"), "dd/MMM/yy h:mm a");

		final String pipedInput = "09/Mar/17 3:14 PM|09/Mar/17 11:36 AM|09/Mar/17 9:12 AM|09/Mar/17 9:12 AM|09/Mar/17 9:12 AM|09/Mar/17 8:14 AM|" +
				"09/Mar/17 7:02 AM|09/Mar/17 6:59 AM|09/Mar/17 6:59 AM|09/Mar/17 6:59 AM|09/Mar/17 6:59 AM|09/Mar/17 6:59 AM|" +
				"09/Mar/17 6:59 AM|09/Mar/17 6:57 AM|08/Mar/17 8:12 AM|07/Mar/17 9:27 PM|07/Mar/17 3:34 PM|07/Mar/17 3:01 PM|" +
				"07/Mar/17 3:00 PM|07/Mar/17 2:51 PM|07/Mar/17 2:46 PM|07/Mar/17 2:40 PM|07/Mar/17 2:23 PM|07/Mar/17 11:04 AM|" +
				"02/Mar/17 10:57 AM|01/Mar/17 11:56 AM|01/Mar/17 6:14 AM|28/Feb/17 4:56 AM|27/Feb/17 5:58 AM|27/Feb/17 5:58 AM|" +
				"22/Feb/17 6:48 AM|18/Jan/17 8:29 AM|04/Jan/17 7:37 AM|10/Nov/16 10:42 AM|";
		final String inputs[] = pipedInput.split("\\|");

		for (final String input : inputs)
			dtp.train(input);

		final DateTimeParserResult result = dtp.getResult();

		final String formatString = result.getFormatString();

		assertEquals(formatString, "dd/MMM/yy h:mm a");

		assertTrue(result.isValid("09/Mar/17 3:14 AM"));
		assertTrue(result.isValid8("09/Mar/17 3:14 AM"));
		assertTrue(result.isValid("09/Mar/17 3:14 PM"));
		assertTrue(result.isValid8("09/Mar/17 3:14 PM"));
		assertFalse(result.isValid("09/Mar/17 3:14"));

		final String regExp = result.getRegExp();

		for (final String input : inputs) {
			assertTrue(input.matches(regExp));
		}
	}

//	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void basicAMPM_viVN() {
		final Locale locale = Locale.forLanguageTag("vi-VN");
		final DateTimeParser dtp = new DateTimeParser();
		dtp.withLocale(locale);

		assertEquals(dtp.determineFormatString("09/thg 3/17 3:14 SA"), "dd/MMM/yy h:mm a");

		final String pipedInput = "09/thg 3/17 3:14 CH|09/thg 3/17 11:36 SA|09/thg 3/17 9:12 SA|09/thg 3/17 9:12 SA|09/thg 3/17 9:12 SA|09/thg 3/17 8:14 SA|" +
				"09/thg 3/17 7:02 SA|09/thg 3/17 6:59 SA|09/thg 3/17 6:59 SA|09/thg 3/17 6:59 SA|09/thg 3/17 6:59 SA|09/thg 3/17 6:59 SA|" +
				"09/thg 3/17 6:59 SA|09/thg 3/17 6:57 SA|08/thg 3/17 8:12 SA|07/thg 3/17 9:27 CH|07/thg 3/17 3:34 CH|07/thg 3/17 3:01 CH|" +
				"07/thg 3/17 3:00 CH|07/thg 3/17 2:51 CH|07/thg 3/17 2:46 CH|07/thg 3/17 2:40 CH|07/thg 3/17 2:23 CH|07/thg 3/17 11:04 SA|" +
				"02/thg 3/17 10:57 SA|01/thg 3/17 11:56 SA|01/thg 3/17 6:14 SA|28/thg 2/17 4:56 SA|27/thg 2/17 5:58 SA|27/thg 2/17 5:58 SA|" +
				"22/thg 2/17 6:48 SA|18/thg 1/17 8:29 SA|04/thg 1/17 7:37 SA|10/thg 11/16 10:42 SA|";
		final String inputs[] = pipedInput.split("\\|");
		final DateTimeParser det = new DateTimeParser();

		for (final String input : inputs)
			det.train(input);

		final DateTimeParserResult result = det.getResult();

		final String formatString = result.getFormatString();

		assertEquals(formatString, "dd/MMM/yy h:mm a");

		assertTrue(result.isValid("09/thg 3/17 3:14 SA"));
		assertTrue(result.isValid8("09/Mar/17 3:14 CH"));
		assertTrue(result.isValid("09/Mar/17 3:14 SA"));
		assertTrue(result.isValid8("09/Mar/17 3:14 SA"));

		final String regExp = result.getRegExp();

		for (final String input : inputs)
			assertTrue(input.matches(regExp));
	}

	private DateTimeParser checkSerialization(final DateTimeParser input) {
		try {
			return DateTimeParser.deserialize(input.serialize());
		} catch (FTAMergeException e) {
			fail(e.getMessage());
			return null;
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void basic_hhmmss_AMPM() {
		final String pipedInput = "06:58:20 AM|07:25:18 PM|01:47:06 AM|05:32:48 AM|11:29:53 PM|02:21:10 PM|04:55:48 AM|" +
				"03:39:14 PM|09:43:02 PM|10:43:15 AM|05:46:07 AM|05:09:34 PM|06:03:58 AM|10:59:15 AM|10:13:28 AM|" +
				"10:25:10 AM|10:33:03 PM|04:24:44 AM|04:19:29 PM|01:08:48 AM|11:11:47 AM|10:11:06 PM|12:27:06 AM|" +
				"12:35:14 AM|01:30:55 PM|12:11:05 AM|09:23:51 PM|10:23:20 PM|01:29:57 PM|04:35:17 PM|06:53:23 PM|" +
				"09:23:13 PM|11:40:44 PM|01:01:02 AM|04:24:19 AM|08:51:48 AM|02:29:26 AM|08:48:32 AM|11:03:13 PM|" +
				"07:52:27 PM|04:51:40 PM|08:31:11 AM|07:53:57 AM|07:04:03 PM|12:05:00 AM|01:50:13 AM|";
		final String inputs[] = pipedInput.split("\\|");
		final DateTimeParser dtp = new DateTimeParser().withLocale(Locale.forLanguageTag("en-US"));

		for (final String input : inputs)
			dtp.train(input);

		final DateTimeParser hydrated = checkSerialization(dtp);
		final DateTimeParserResult result = hydrated.getResult();

		final String formatString = result.getFormatString();

		assertEquals(formatString, "hh:mm:ss a");

		assertTrue(result.isValid("03:14:12 AM"));
		assertTrue(result.isValid8("03:14:12 AM"));
		assertTrue(result.isValid("03:14:12 PM"));
		assertTrue(result.isValid8("03:14:12 PM"));

		final String regExp = result.getRegExp();

		for (final String input : inputs)
			assertTrue(input.matches(regExp), input);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void basic_hhmmss_AMPM_2() {
		final String pipedInput = "06:58:20am|07:25:18pm|01:47:06am|05:32:48am|11:29:53pm|02:21:10pm|04:55:48am|" +
				"03:39:14pm|09:43:02pm|10:43:15am|05:46:07am|05:09:34pm|06:03:58am|10:59:15am|10:13:28am|" +
				"10:25:10am|10:33:03pm|04:24:44am|04:19:29pm|01:08:48am|11:11:47am|10:11:06pm|12:27:06am|" +
				"12:35:14am|01:30:55pm|12:11:05am|09:23:51pm|10:23:20pm|01:29:57pm|04:35:17pm|06:53:23pm|" +
				"09:23:13pm|11:40:44pm|01:01:02am|04:24:19am|08:51:48am|02:29:26am|08:48:32am|11:03:13pm|" +
				"07:52:27pm|04:51:40pm|08:31:11am|07:53:57am|07:04:03pm|12:05:00am|01:50:13am|";
		final String inputs[] = pipedInput.split("\\|");
		final DateTimeParser dtp = new DateTimeParser();

		dtp.train("");
		for (final String input : inputs)
			dtp.train(input);

		final DateTimeParser hydrated = checkSerialization(dtp);
		final DateTimeParserResult result = hydrated.getResult();

		final String formatString = result.getFormatString();

		assertEquals(formatString, "hh:mm:ssa");

		assertTrue(result.isValid("03:14:12am"));
		assertTrue(result.isValid("03:14:12pm"));
		// It hurts but there is no way to specify am/pm (lowercase) as valid without constructing your own formatter
		// assertTrue(result.isValid8("03:14:12am"));
		// assertTrue(result.isValid8("03:14:12pm"));

		final String regExp = result.getRegExp();

		for (final String input : inputs)
			assertTrue(input.matches(regExp), input);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void serializationPerformance() throws FTAMergeException {
		final String pipedInput = "06:58:20am|07:25:18pm|01:47:06am|05:32:48am|11:29:53pm|02:21:10pm|04:55:48am|" +
				"03:39:14pm|09:43:02pm|10:43:15am|05:46:07am|05:09:34pm|06:03:58am|10:59:15am|10:13:28am|" +
				"10:25:10am|10:33:03pm|04:24:44am|04:19:29pm|01:08:48am|11:11:47am|10:11:06pm|12:27:06am|" +
				"12:35:14am|01:30:55pm|12:11:05am|09:23:51pm|10:23:20pm|01:29:57pm|04:35:17pm|06:53:23pm|" +
				"09:23:13pm|11:40:44pm|01:01:02am|04:24:19am|08:51:48am|02:29:26am|08:48:32am|11:03:13pm|" +
				"07:52:27pm|04:51:40pm|08:31:11am|07:53:57am|07:04:03pm|12:05:00am|01:50:13am|";
		final String inputs[] = pipedInput.split("\\|");
		final DateTimeParser dtp = new DateTimeParser();
		final int ITERATIONS = 10000;

		dtp.train("");
		for (final String input : inputs)
			dtp.train(input);

		String s = dtp.serialize();
		long start = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; i++)
			s = dtp.serialize();
		final long serializeTime = System.currentTimeMillis() - start;
		System.err.printf("Serialization: %dms (%dμs per)%n", serializeTime, (serializeTime * 1000) / ITERATIONS);

		start = System.currentTimeMillis();
		DateTimeParser hydrated = DateTimeParser.deserialize(s);
		for (int i = 0; i < ITERATIONS; i++)
			hydrated = DateTimeParser.deserialize(s);
		final long deserializeTime = System.currentTimeMillis() - start;
		System.err.printf("Deserialization: %dms (%dμs per)%n", deserializeTime, (deserializeTime * 1000) / ITERATIONS);

		final DateTimeParserResult result = hydrated.getResult();

		final String formatString = result.getFormatString();

		assertEquals(formatString, "hh:mm:ssa");

		assertTrue(result.isValid("03:14:12am"));
		assertTrue(result.isValid("03:14:12pm"));

		final String regExp = result.getRegExp();

		for (final String input : inputs)
			assertTrue(input.matches(regExp), input);
	}


	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void basicyyyyddMM() throws IOException {
		final String pipedInput = "2011/28/02|2017/31/12|2016/20/10|1999/15/07|2017/31/12|2016/20/10|1999/15/07|2017/31/12|2017/31/12|2016/20/10|1999/15/07|2017/30/12|2017/21/12|2016/20/10|1999/15/07|2017/11/12|2012/31/12|2010/31/12|2016/20/10|1999/15/07|";
		final String inputs[] = pipedInput.split("\\|");
		final DateTimeParser dtp = new DateTimeParser();

		for (final String input : inputs)
			dtp.train(input);

		final DateTimeParserResult result = checkSerialization(dtp).getResult();

		final String formatString = result.getFormatString();

		assertEquals(formatString, "yyyy/dd/MM");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void mixed_yyyyddMM() throws IOException {
		final String pipedInput = "1970/06/06|1971/01/06|1972/07/07|1973/03/03|1974/04/04|1970/05/05|1970/06/06|1970/08/08|1970/09/09|1970/10/10|1970/06/06|1971/01/06|1972/07/07|1973/03/03|1974/04/04|1970/05/05|1970/06/06|1970/08/08|1970/09/09|1970/10/10|2011/31/02|2017/31/12|2016/20/10|1999/15/07|";
		final String inputs[] = pipedInput.split("\\|");
		final DateTimeParser dtp = new DateTimeParser();

		for (final String input : inputs)
			dtp.train(input);

		final DateTimeParserResult result = checkSerialization(dtp).getResult();

		final String formatString = result.getFormatString();

		assertEquals(formatString, "yyyy/dd/MM");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void testMMM_enCA() throws IOException {
		final String input = "1999-Aug-11";
		final DateTimeParser dtp = new DateTimeParser().withLocale(Locale.CANADA);
		final String format = dtp.determineFormatString(input);

		assertEquals(format, "yyyy-MMM-dd");

		final DateTimeFormatter dtf = dtp.ofPattern(format);

		try {
			LocalDate.parse(input, dtf);
		}
		catch (DateTimeParseException e) {
			fail(e.getMessage());
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void testMMM_enCA_false() throws IOException {
		final String[] shortMonths = new DateFormatSymbols(Locale.CANADA).getShortMonths();

		// Java 8 has the Short Month as 'Aug', Java 11 & 18 has 'Aug.'
		// This test is to check the behavior when the period is present - so skip if it is not
		if (shortMonths[7].length() == 4) {
			final String input = "1999-Aug.-11";
			final DateTimeParser dtp = new DateTimeParser().withLocale(Locale.CANADA).withNoAbbreviationPunctuation(false);
			final String format = dtp.determineFormatString(input);

			if (!"yyyy-MMM-dd".equals(format))
				System.err.println("format = " + format);
			assertEquals(format, "yyyy-MMM-dd", format);

			final DateTimeFormatter dtf = dtp.ofPattern(format);

			try {
				LocalDate.parse(input, dtf);
			}
			catch (DateTimeParseException e) {
				System.err.println(e.getMessage());
				fail(e.getMessage());
			}
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void testMMM_enAU() throws IOException {
		final String input = "1999-Aug-11";
		final Locale australia = Locale.forLanguageTag("en-AU");
		final DateTimeParser dtp = new DateTimeParser().withLocale(australia);
		final String format = dtp.determineFormatString(input);

		assertEquals(format, "yyyy-MMM-dd");

		final DateTimeFormatter dtf = dtp.ofPattern(format);

		try {
			LocalDate.parse(input, dtf);
		}
		catch (DateTimeParseException e) {
			fail(e.getMessage());
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitMMMDDYYYY() {
		final String trimmed = "May 1, 2018";
		final DateTimeParser dtp = new DateTimeParser().withLocale(Locale.US);
		dtp.train(trimmed);

		final DateTimeParserResult result = checkSerialization(dtp).getResult();

		final String formatString = result.getFormatString();

		assertEquals(formatString, "MMM d, yyyy");

		final String regExp = result.getRegExp();
		assertEquals(regExp, KnownTypes.PATTERN_ALPHA + "{3} \\d{1,2}, \\d{4}");
		assertTrue(trimmed.matches(regExp));

		assertNull(checkParseable(result, trimmed, Locale.US));

		assertTrue(result.isValid8("Jun 30, 2023"));
		assertTrue(result.isValid("Jun 30, 2023"));
		assertFalse(result.isValid8("Jun 30 2023"));
		assertFalse(result.isValid("Jun 30 2023"));
		assertFalse(result.isValid("Jun 32, 2023"));
		assertFalse(result.isValid8("Jun 32, 2023"));
		assertFalse(result.isValid("Jun 30; 2023"));
		assertFalse(result.isValid8("Jun 30; 2023"));
		// Invalid but not detected!!
		assertTrue(result.isValid("Jun 31, 2023"));
		assertTrue(result.isValid8("Jun 31, 2023"));
	}

	private static String checkParseable(final DateTimeParserResult result, final String input, final Locale locale) {
		final String formatString = result.getFormatString();
		final FTAType type = result.getType();

		try {
			final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatString, locale);
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
			formatter.parse(input);
		}
		catch (DateTimeParseException exc) {
			return "Java Should have successfully parsed: " + input;
		}

		try {
			result.parse(input);
		}
		catch (DateTimeParseException e) {
			return "Should have successfully parsed: " + input;
		}

		return null;
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void testAsResult() {
		assertNull(DateTimeParserResult.asResult("yyyy-MM-ddTHH:m:ssx", DateResolutionMode.None, new DateTimeParserConfig()));
		assertNull(DateTimeParserResult.asResult("yyyy-MM-ddTHH:mm:sx", DateResolutionMode.None, new DateTimeParserConfig()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void ambiguous_yyyy_dM_dM() {
		final DateTimeParser dtp = new DateTimeParser();

		final String fmt = dtp.determineFormatString("2022/01/01");
		DateTimeParserResult result = DateTimeParserResult.asResult(fmt, DateResolutionMode.None, dtp.getConfig());
		assertEquals(result.getFormatString(), "yyyy/??/??");
		result = DateTimeParserResult.asResult(fmt, DateResolutionMode.DayFirst, dtp.getConfig());
		assertEquals(result.getFormatString(), "yyyy/??/??");
		result = DateTimeParserResult.asResult(fmt, DateResolutionMode.MonthFirst, dtp.getConfig());
		assertEquals(result.getFormatString(), "yyyy/??/??");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void ambiguous_dM_dM_yyyy() {
		final String sample = "01/01/2018";

		DateTimeParser dtp = new DateTimeParser().withDateResolutionMode(DateResolutionMode.None);
		dtp.train(sample);
		assertEquals(dtp.getResult().getFormatString(), "??/??/yyyy");

		dtp = new DateTimeParser().withDateResolutionMode(DateResolutionMode.DayFirst);
		dtp.train(sample);
		assertEquals(dtp.getResult().getFormatString(), "dd/MM/yyyy");

		dtp = new DateTimeParser().withDateResolutionMode(DateResolutionMode.MonthFirst);
		dtp.train(sample);
		assertEquals(dtp.getResult().getFormatString(), "MM/dd/yyyy");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void ambiguous_dMy_dMy_dMy() {
		final String sample = "01/01/01";

		DateTimeParser dtp = new DateTimeParser().withDateResolutionMode(DateResolutionMode.None);
		dtp.train(sample);
		assertEquals(dtp.getResult().getFormatString(), "??/??/??");

		dtp = new DateTimeParser().withDateResolutionMode(DateResolutionMode.DayFirst);
		dtp.train(sample);
		assertEquals(dtp.getResult().getFormatString(), "dd/MM/yy");

		dtp = new DateTimeParser().withDateResolutionMode(DateResolutionMode.MonthFirst);
		dtp.train(sample);
		assertEquals(dtp.getResult().getFormatString(), "MM/dd/yy");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void padding() {
		final DateTimeParser dtp = new DateTimeParser().withLocale(Locale.US);
		final String PADDED_INPUT = "23/01/88  3:14:16";

		final String fmt = dtp.determineFormatString(PADDED_INPUT);
		final DateTimeParserResult result = DateTimeParserResult.asResult(fmt, DateResolutionMode.None, dtp.getConfig());
		assertEquals(fmt, result.getFormatString());
		assertEquals(result.getFormatString(), "dd/MM/yy ppH:mm:ss");
		assertNull(checkParseable(result, PADDED_INPUT, Locale.US));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void padding2() {
		final String PADDED_FORMAT = "dd/MM/yy ppH:mm:ss";

		final DateTimeParserResult result = DateTimeParserResult.asResult(PADDED_FORMAT, DateResolutionMode.None, new DateTimeParserConfig());
		assertEquals(PADDED_FORMAT, result.getFormatString());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void testParse() {
		final DateTimeParserResult result = DateTimeParserResult.asResult("yyyy/MM/dd HH:mm", DateResolutionMode.None, new DateTimeParserConfig());

		try {
			result.parse("2018/01/31 05:O5");
		}
		catch (DateTimeParseException e) {
			assertEquals(e.getMessage(), "Expecting digit");
			assertEquals(e.getErrorIndex(), 14);
		}

		try {
			result.parse("2018/01/31 05:5");
		}
		catch (DateTimeParseException e) {
			assertEquals(e.getMessage(), "Expecting digit, end of input");
			assertEquals(e.getErrorIndex(), 15);
		}

		try {
			result.parse("2018/12/24 09:");
		}
		catch (DateTimeParseException e) {
			assertEquals(e.getMessage(), "Expecting digit, end of input");
			assertEquals(e.getErrorIndex(), 14);
		}

		try {
			result.parse("2018/1/24 09:00");
		}
		catch (DateTimeParseException e) {
			assertEquals(e.getMessage(), "Insufficient digits in input (M)");
			assertEquals(e.getErrorIndex(), 6);
		}

		try {
			result.parse("2018/11/4 09:00");
		}
		catch (DateTimeParseException e) {
			assertEquals(e.getMessage(), "Expecting digit");
			assertEquals(e.getErrorIndex(), 9);
		}

		try {
			result.parse("2018/11/O4 09:00");
		}
		catch (DateTimeParseException e) {
			assertEquals(e.getMessage(), "Expecting digit");
			assertEquals(e.getErrorIndex(), 8);
		}

		try {
			result.parse("2018/00/24 05:59");
		}
		catch (DateTimeParseException e) {
			assertEquals(e.getMessage(), "0 value illegal for day/month");
			assertEquals(e.getErrorIndex(), 7);
		}

		try {
			result.parse("2018/13/24 05:59");
		}
		catch (DateTimeParseException e) {
			assertEquals(e.getMessage(), "Value too large for day/month");
			assertEquals(e.getErrorIndex(), 6);
		}

		try {
			result.parse("2018/01/00 05:59");
		}
		catch (DateTimeParseException e) {
			assertEquals(e.getMessage(), "0 value illegal for day/month");
			assertEquals(e.getErrorIndex(), 10);
		}

		try {
			result.parse("2018/01/32 05:59");
		}
		catch (DateTimeParseException e) {
			assertEquals(e.getMessage(), "Value too large for day/month");
			assertEquals(e.getErrorIndex(), 9);
		}

		try {
			result.parse("2018/01/22 05:59 pm");
		}
		catch (DateTimeParseException e) {
			assertEquals(e.getMessage(), "Expecting end of input, extraneous input found, last token (MINS)");
			assertEquals(e.getErrorIndex(), 16);
		}

		try {
			result.parse("201/01/22 05:59 pm");
		}
		catch (DateTimeParseException e) {
			assertEquals(e.getMessage(), "Expecting digit");
			assertEquals(e.getErrorIndex(), 3);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitDateTime() {
		final DateTimeParser dtp = new DateTimeParser();

		assertEquals(dtp.determineFormatString("  2/12/98 9:57    "), "?/??/yy H:mm");
		assertNull(dtp.determineFormatString("0¶¶¶¶¶"));
		assertNull(dtp.determineFormatString("2/12/98 :57"));
		assertNull(dtp.determineFormatString("2/12/98 9:5"));
		assertNull(dtp.determineFormatString("2/12/98 9:55:5"));
		assertEquals(dtp.determineFormatString("2/13/98 9:57"), "M/dd/yy H:mm");
		assertEquals(dtp.determineFormatString("13/12/98 12:57"), "dd/MM/yy HH:mm");
		assertEquals(dtp.determineFormatString("12/12/2012 8:57:02"), "??/??/yyyy H:mm:ss");
		assertEquals(dtp.determineFormatString("12/12/2012 8:57:02 GMT"), "??/??/yyyy H:mm:ss z");
		assertEquals(dtp.determineFormatString("13/12/2012 8:57:02"), "dd/MM/yyyy H:mm:ss");
		assertEquals(dtp.determineFormatString("2012/12/13 12:57:02"), "yyyy/MM/dd HH:mm:ss");

		DateTimeParserResult result;
		final DateTimeParser detUnspecified = new DateTimeParser();
		detUnspecified.train("12/12/2012 8:57:02 GMT");

		result = detUnspecified.getResult();
		assertEquals(result.getFormatString(), "??/??/yyyy H:mm:ss z");

		final DateTimeParser detDayFirst = new DateTimeParser().withDateResolutionMode(DateResolutionMode.DayFirst);
		detDayFirst.train("12/12/2012 8:57:02 GMT");

		result = detDayFirst.getResult();
		assertEquals(result.getFormatString(), "dd/MM/yyyy H:mm:ss z");

		final DateTimeParser detMonthFirst = new DateTimeParser().withDateResolutionMode(DateResolutionMode.MonthFirst);
		final String sample = "12/12/2012 8:57:02 GMT";
		detMonthFirst.train(sample);

		result = detMonthFirst.getResult();
		assertEquals(result.getFormatString(), "MM/dd/yyyy H:mm:ss z");

		final String regExp = result.getRegExp();
		assertEquals(regExp, "\\d{2}/\\d{2}/\\d{4} \\d{1,2}:\\d{2}:\\d{2} .*");
		assertTrue(sample.matches(regExp));

		assertTrue(result.isValid("12/12/2012 8:57:02 GMT"));
		assertFalse(result.isValid("12/12/2012 8:57:02 GM"));
		assertFalse(result.isValid("12/12/2012 8:57:02 GMZ"));
		assertFalse(result.isValid("1O/12/2012 8:57:02 GMT"));
		assertFalse(result.isValid("10/1O/2012 8:57:02 GMT"));
		assertFalse(result.isValid("1/0/2012 8:57:02 GMT"));
		assertFalse(result.isValid("1/O/2012 8:57:02 GMT"));
		assertFalse(result.isValid("2/12/1998 :57"));
		assertFalse(result.isValid("2/12/1998 9:5"));
		assertFalse(result.isValid("2/12/1998 9:"));
		assertFalse(result.isValid("2/12/1998 9:55:5"));

		assertTrue(result.isValid8("12/12/2012 8:57:02 GMT"));
		assertFalse(result.isValid8("12/12/2012 8:57:02 GM"));
		assertFalse(result.isValid8("12/12/2012 8:57:02 GMZ"));
		assertFalse(result.isValid8("1O/12/2012 8:57:02 GMT"));
		assertFalse(result.isValid8("10/1O/2012 8:57:02 GMT"));
		assertFalse(result.isValid8("1/0/2012 8:57:02 GMT"));
		assertFalse(result.isValid8("1/O/2012 8:57:02 GMT"));
		assertFalse(result.isValid8("2/12/1998 :57"));
		assertFalse(result.isValid8("2/12/1998 9:5"));
		assertFalse(result.isValid8("2/12/1998 9:"));
		assertFalse(result.isValid8("2/12/1998 9:55:5"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitISONoSeps() {
		final DateTimeParser det = new DateTimeParser();
		final String sample = "20040101T123541Z";

		det.train(sample);
		det.train("20040101T020000Z");
		det.train("20060101T052321Z");
		det.train("20141121T151221Z");
		det.train("20080517T181221Z");
		det.train("20040101T002332Z");
		det.train("20060101T130012Z");
		det.train("20060101T000000Z");
		det.train("20040101T181632Z");
		det.train("20041008T221001Z");
		det.train("20040101T000000Z");
		det.train("20140101T221011Z");
		det.train("20041022T000000Z");
		det.train("19980905T130112Z");
		det.train("20080301T130632Z");
		det.train("20111007T000000Z");
		det.train(null);
		det.train("20000610T020000Z");
		det.train(null);
		det.train("20180211T192111Z");

		final DateTimeParserResult result = det.getResult();
		assertEquals(result.getFormatString(), "yyyyMMdd'T'HHmmss'Z'");

		final String regExp = result.getRegExp();
		assertEquals(regExp, "\\d{8}T\\d{6}Z");
		assertTrue(sample.trim().matches(regExp));

		assertTrue(result.isValid("20000610T020000Z"));
		assertFalse(result.isValid("20000610T020060Z"));
		assertFalse(result.isValid("20000610T026000Z"));
		assertFalse(result.isValid("20000610T250000Z"));

		assertTrue(result.isValid8("20000610T020000Z"));
		assertFalse(result.isValid8("20000610T020060Z"));
		assertFalse(result.isValid8("20000610T026000Z"));
		assertFalse(result.isValid8("20000610T250000Z"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitAlmostISONoSeps() {
		final DateTimeParser dtp = new DateTimeParser();
		final String sample = "20040101T123541";

		dtp.train(sample);
		dtp.train("20040101T020000");
		dtp.train("20060101T052321");
		dtp.train("20141121T151221");
		dtp.train("20080517T181221");
		dtp.train("20040101T002332");
		dtp.train("20060101T130012");
		dtp.train("20060101T000000");
		dtp.train("20040101T181632");
		dtp.train("20041008T221001");
		dtp.train("20040101T000000");
		dtp.train("20140101T221011");
		dtp.train("20041022T000000");
		dtp.train("19980905T130112");
		dtp.train("20080301T130632");
		dtp.train("20111007T000000");
		dtp.train(null);
		dtp.train("20000610T020000");
		dtp.train(null);
		dtp.train("20180211T192111");

		final DateTimeParserResult result = checkSerialization(dtp).getResult();
		assertEquals(result.getFormatString(), "yyyyMMdd'T'HHmmss");

		final String regExp = result.getRegExp();
		assertEquals(regExp, "\\d{8}T\\d{6}");
		assertTrue(sample.trim().matches(regExp));

		assertTrue(result.isValid("20000610T020000"));
		assertFalse(result.isValid("20000610T020060"));
		assertFalse(result.isValid("20000610T026000"));
		assertFalse(result.isValid("20000610T250000"));

		assertTrue(result.isValid8("20000610T020000"));
		assertFalse(result.isValid8("20000610T020060"));
		assertFalse(result.isValid8("20000610T026000"));
		assertFalse(result.isValid8("20000610T250000"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitMMMM_d_yyyy() {
		final DateTimeParser dtp = new DateTimeParser().withLocale(Locale.forLanguageTag("en-US"));
		final String sample = "September-17-2014";

		dtp.train(sample);
		dtp.train("September-11-2011");
		dtp.train("September-4-2008");
		dtp.train("August-29-2005");
		dtp.train("August-23-2002");
		dtp.train("August-17-1999");
		dtp.train("August-10-1996");
		dtp.train("August-4-1993");
		dtp.train("July-29-1990");
		dtp.train("July-23-1987");
		dtp.train("July-16-1984");
		dtp.train("July-10-1981");
		dtp.train("July-4-1978");
		dtp.train("June-28-1975");
		dtp.train("June-21-1972");
		dtp.train("June-15-1969");
		dtp.train("June-9-1966");
		dtp.train("June-3-1963");
		dtp.train("May-27-1960");
		dtp.train("May-21-1957");
		dtp.train("May-15-1954");
		dtp.train("May-9-1951");
		dtp.train("May-2-1948");
		dtp.train("April-26-1945");
		dtp.train("April-20-1942");
		dtp.train("April-14-1939");
		dtp.train("April-7-1936");
		dtp.train("April-1-1933");
		dtp.train("March-26-1930");
		dtp.train("March-20-1927");

		final DateTimeParserResult result = checkSerialization(dtp).getResult();
		assertEquals(result.getFormatString(), "MMMM-d-yyyy");

		final String regExp = result.getRegExp();
		assertEquals(regExp, KnownTypes.PATTERN_ALPHA + "{3,9}-\\d{1,2}-\\d{4}");
		assertTrue(sample.trim().matches(regExp));

		assertTrue(result.isValid("April-1-1939"));
		assertFalse(result.isValid("April-32-1940"));
		assertFalse(result.isValid("Sep-12-1959"));
		assertFalse(result.isValid("May-12-69"));

		assertTrue(result.isValid8("April-1-1939"));
		assertFalse(result.isValid8("April-32-1940"));
		assertFalse(result.isValid8("Sep-12-1959"));
		assertFalse(result.isValid8("May-12-69"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitAlmostISO() {
		final DateTimeParser dtp = new DateTimeParser();
		final String sample = "2004-01-01 12:35:41-0500";

		dtp.train(sample);
		dtp.train("2004-01-01 02:00:00-0500");
		dtp.train("2006-01-01 05:23:21-0500");
		dtp.train("2014-11-21 15:12:21-0500");
		dtp.train("2008-05-17 18:12:21-0400");
		dtp.train("2004-01-01 00:23:32-0500");
		dtp.train("2006-01-01 13:00:12-0500");
		dtp.train("2006-01-01 00:00:00-0500");
		dtp.train("2004-01-01 18:16:32-0500");
		dtp.train("2004-10-08 22:10:01-0400");
		dtp.train("2004-01-01 00:00:00-0500");
		dtp.train("2014-01-01 22:10:11-0500");
		dtp.train("2004-10-22 00:00:00-0400");
		dtp.train("1998-09-05 13:01:12-0400");
		dtp.train("2008-03-01 13:06:32-0500");
		dtp.train("2011-10-07 00:00:00-0400");
		dtp.train(null);
		dtp.train("2000-06-10 02:00:00-0400");
		dtp.train(null);
		dtp.train("2018-02-11 19:21:11-0500");

		final DateTimeParserResult result = checkSerialization(dtp).getResult();
		assertEquals(result.getFormatString(), "yyyy-MM-dd HH:mm:ssxx");

		final String regExp = result.getRegExp();
		assertEquals(regExp, "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}[-+][0-9]{4}");
		assertTrue(sample.trim().matches(regExp));

		assertTrue(result.isValid("2000-06-10 02:00:00-0400"));
		assertFalse(result.isValid("2000-06-10 02:00:60-0400"));
		assertFalse(result.isValid("2000-06-10 02:60:00-0400"));
		assertFalse(result.isValid("2000-06-10 25:00:00-0400"));

		assertTrue(result.isValid8("2000-06-10 02:00:00-0400"));
		assertFalse(result.isValid8("2000-06-10 02:00:60-0400"));
		assertFalse(result.isValid8("2000-06-10 02:60:00-0400"));
		assertFalse(result.isValid8("2000-06-10 25:00:00-0400"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitAlmostISO2() {
		final DateTimeParser dtp = new DateTimeParser();
		final String sample = "2004-01-01 12:35:41";

		dtp.train(sample);
		dtp.train("2004-01-01 02:00:00");
		dtp.train("2006-01-01 05:23:21");
		dtp.train("2014-11-21 15:12:21");
		dtp.train("2008-05-17 18:12:21");
		dtp.train("2004-01-01 00:23:32");
		dtp.train("2006-01-01 13:00:12");
		dtp.train("2006-01-01 00:00:00");
		dtp.train("2004-01-01 18:16:32");
		dtp.train("2004-10-08 22:10:01");
		dtp.train("2004-01-01 00:00:00");
		dtp.train("2014-01-01 22:10:11");
		dtp.train("2004-10-22 00:00:00");
		dtp.train("1998-09-05 13:01:12");
		dtp.train("2008-03-01 13:06:32");
		dtp.train("2011-10-07 00:00:00");
		dtp.train(null);
		dtp.train("2000-06-10 02:00:00");
		dtp.train(null);
		dtp.train("2018-02-11 19:21:11");

		final DateTimeParserResult result = checkSerialization(dtp).getResult();
		assertEquals(result.getFormatString(), "yyyy-MM-dd HH:mm:ss");

		final String regExp = result.getRegExp();
		assertEquals(regExp, "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
		assertTrue(sample.trim().matches(regExp));

		assertTrue(result.isValid("2000-06-10 02:00:00"));
		assertFalse(result.isValid("2000-06-10 02:00:60"));
		assertFalse(result.isValid("2000-06-10 02:60:00"));
		assertFalse(result.isValid("2000-06-10 25:00:00"));

		assertTrue(result.isValid8("2000-06-10 02:00:00"));
		assertFalse(result.isValid8("2000-06-10 02:00:60"));
		assertFalse(result.isValid8("2000-06-10 02:60:00"));
		assertFalse(result.isValid8("2000-06-10 25:00:00"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitAlmostISO3() {
		final DateTimeParser det = new DateTimeParser().withDateResolutionMode(DateResolutionMode.MonthFirst);
		final String sample = "2004-01-01 12:35:41.0";
		assertEquals(det.determineFormatString(sample), "yyyy-MM-dd HH:mm:ss.S");

		det.train(sample);
		det.train("2004-01-01 02:00:00.0");
		det.train("2006-01-01 05:23:21.999");
		det.train("2014-11-21 15:12:21.0");
		det.train("2008-05-17 18:12:21.0");
		det.train("2004-01-01 00:23:32.0");
		det.train("2006-01-01 13:00:12.0");
		det.train("2006-01-01 00:00:00.0");
		det.train("2004-01-01 18:16:32.0");
		det.train("2004-10-08 22:10:01.0");
		det.train("2004-01-01 00:00:00.0");
		det.train("2014-01-01 22:10:11.0");
		det.train("2004-10-22 00:00:00.0");
		det.train("1998-09-05 13:01:12.0");
		det.train("2008-03-01 13:06:32.0");
		det.train("2011-10-07 00:00:00.0");
		det.train(null);
		det.train("2000-06-10 02:00:00.0");
		det.train(null);
		det.train("2018-02-11 19:21:11.0");

		DateTimeParserResult result = det.getResult();
		assertEquals(result.getFormatString(), "yyyy-MM-dd HH:mm:ss.S{1,3}");
		result = checkSerialization(det).getResult();
		assertEquals(result.getFormatString(), "yyyy-MM-dd HH:mm:ss.S{1,3}");

		final String regExp = result.getRegExp();
		assertEquals(regExp, "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{1,3}");
		assertTrue(sample.trim().matches(regExp));

		assertTrue(result.isValid("2000-06-10 02:00:00.0"));
		assertFalse(result.isValid("2000-06-10 02:00:60.0"));
		assertFalse(result.isValid("2000-06-10 02:60:00.0"));
		assertFalse(result.isValid("2000-06-10 25:00:00.0"));

		assertTrue(result.isValid8("2000-06-10 02:00:00.0"));
		assertFalse(result.isValid8("2000-06-10 02:00:60.0"));
		assertFalse(result.isValid8("2000-06-10 02:60:00.0"));
		assertFalse(result.isValid8("2000-06-10 25:00:00.0"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void testDTPResult_Date() {
		final String[] tests = { "MM", "MMM", "MMMM", "dd", "yy", "yyyy", "EEE", "a" };

		for (final String test : tests) {
			final DateTimeParserResult det = DateTimeParserResult.asResult(test, DateResolutionMode.Auto, new DateTimeParserConfig());
			assertEquals(det.getType(), FTAType.LOCALDATE);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void testDTPResult_Time() {
		final String[] tests = { "HH", "mm", "ss", "SSS" };

		for (final String test : tests) {
			final DateTimeParserResult det = DateTimeParserResult.asResult(test, DateResolutionMode.Auto, new DateTimeParserConfig());
			assertEquals(det.getType(), FTAType.LOCALTIME);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void testDTPResult_DateTime() {
		final String[] tests = { "MM/dd/yyyy HH:mm:ss" };

		for (final String test : tests) {
			final DateTimeParserResult det = DateTimeParserResult.asResult(test, DateResolutionMode.Auto, new DateTimeParserConfig());
			assertEquals(det.getType(), FTAType.LOCALDATETIME);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void testDTPResult_OffsetDateTime() {
		final String[] tests = { "yyyyMMdd'T'HHmmssxx" };

		for (final String test : tests) {
			final DateTimeParserResult det = DateTimeParserResult.asResult(test, DateResolutionMode.Auto, new DateTimeParserConfig());
			assertEquals(det.getType(), FTAType.OFFSETDATETIME);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void testDTPResult_ZonedDateTime() {
		final String[] tests = { "EEE MMM dd HH:mm:ss z yyyy" };

		for (final String test : tests) {
			final DateTimeParserResult det = DateTimeParserResult.asResult(test, DateResolutionMode.Auto, new DateTimeParserConfig());
			assertEquals(det.getType(), FTAType.ZONEDDATETIME);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void testDTPResult_Unknown() {
		final String[] tests = { "W", "V", "G", "u", "L", "Q", "e", "c", "K", "n", "N" };

		for (final String test : tests) {
			final DateTimeParserResult det = DateTimeParserResult.asResult(test, DateResolutionMode.Auto, new DateTimeParserConfig());
			assertEquals(det.getType(), FTAType.LOCALDATE, test);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitAlmostISO4() {
		final DateTimeParser det = new DateTimeParser().withDateResolutionMode(DateResolutionMode.MonthFirst);
		final String sample = "2004-01-01 12:35:41.999";
		assertEquals(det.determineFormatString(sample), "yyyy-MM-dd HH:mm:ss.SSS");

		det.train(sample);
		det.train("2004-01-01 02:00:00.000");
		det.train("2006-01-01 05:23:21.999");
		det.train("2014-11-21 15:12:21.123");
		det.train("2008-05-17 18:12:21.456");
		det.train("2004-01-01 00:23:32.789");
		det.train("2006-01-01 13:00:12.000");
		det.train("2006-01-01 00:00:00.001");
		det.train("2004-01-01 18:16:32.010");
		det.train("2004-10-08 22:10:01.500");
		det.train("2004-01-01 00:00:00.600");
		det.train("2014-01-01 22:10:11.000");
		det.train("2004-10-22 00:00:00.090");
		det.train("1998-09-05 13:01:12.010");
		det.train("2008-03-01 13:06:32.890");
		det.train("2011-10-07 00:00:00.880");
		det.train(null);
		det.train("2000-06-10 02:00:00.000");
		det.train(null);
		det.train("2018-02-11 19:21:11.000");

		final DateTimeParserResult result = checkSerialization(det).getResult();
		assertEquals(result.getFormatString(), "yyyy-MM-dd HH:mm:ss.SSS");

		final String regExp = result.getRegExp();
		assertEquals(regExp, "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}");
		assertTrue(sample.trim().matches(regExp));

		assertTrue(result.isValid("2000-06-10 02:00:00.000"));
		assertFalse(result.isValid("2000-06-10 02:00:60.990"));
		assertFalse(result.isValid("2000-06-10 02:60:00.009"));
		assertFalse(result.isValid("2000-06-10 02:60:00.00"));
		assertFalse(result.isValid("2000-06-10 25:00:00.008"));

		assertTrue(result.isValid8("2000-06-10 02:00:00.000"));
		assertFalse(result.isValid8("2000-06-10 02:00:60.990"));
		assertFalse(result.isValid8("2000-06-10 02:60:00.009"));
		assertFalse(result.isValid8("2000-06-10 02:60:00.00"));
		assertFalse(result.isValid8("2000-06-10 25:00:00.008"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitTimeDate() {
		final DateTimeParser dtp = new DateTimeParser();

		assertEquals(dtp.determineFormatString("9:57 2/13/98"), "H:mm M/dd/yy");
		assertEquals(dtp.determineFormatString("9:57 2/12/98"), "H:mm ?/??/yy");
		assertEquals(dtp.determineFormatString("12:57 13/12/98"), "HH:mm dd/MM/yy");
		assertEquals(dtp.determineFormatString("8:57:02 12/12/2012"), "H:mm:ss ??/??/yyyy");
		assertEquals(dtp.determineFormatString("12:57:02 2012/12/18"), "HH:mm:ss yyyy/MM/dd");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void parsedMMMyyyy() {
		final DateTimeParser dtp = new DateTimeParser().withLocale(Locale.forLanguageTag("en-US"));

		assertEquals(dtp.determineFormatString("12-May-14"), "dd-MMM-yy");
		assertEquals(dtp.determineFormatString("2-Jan-2017"), "d-MMM-yyyy");
		assertEquals(dtp.determineFormatString("21 Jan 2017"), "dd MMM yyyy");
		assertEquals(dtp.determineFormatString("8 Dec 1993"), "d MMM yyyy");
		assertEquals(dtp.determineFormatString("25-Dec-2017"), "dd-MMM-yyyy");
		assertNull(dtp.determineFormatString("21-Jam-2017"));

		final DateTimeParser det = new DateTimeParser();
		final String sample = "2 Jan 2017";
		det.train(sample);

		final DateTimeParserResult result = checkSerialization(det).getResult();
		assertEquals(result.getFormatString(), "d MMM yyyy");
		final String regExp = result.getRegExp();
		assertEquals(regExp, "\\d{1,2} " + KnownTypes.PATTERN_ALPHA + "{3} \\d{4}");
		assertTrue(sample.matches(regExp));

		assertTrue(result.isValid("20 Jun 2017"));
		assertTrue(result.isValid("1 Jun 2017"));
		assertFalse(result.isValid("20 0c"));
		assertFalse(result.isValid(""));
		assertFalse(result.isValid("1"));
		assertFalse(result.isValid("20 0ct 2018"));
		assertFalse(result.isValid("32 Oct 2018"));
		assertFalse(result.isValid("32 Och 2018"));
		assertFalse(result.isValid("31 Oct 201"));

		assertTrue(result.isValid8("20 Jun 2017"));
		assertTrue(result.isValid8("1 Jun 2017"));
		assertFalse(result.isValid8("20 0c"));
		assertFalse(result.isValid8(""));
		assertFalse(result.isValid8("1"));
		assertFalse(result.isValid8("20 0ct 2018"));
		assertFalse(result.isValid8("32 Oct 2018"));
		assertFalse(result.isValid8("32 Och 2018"));
		assertFalse(result.isValid8("31 Oct 201"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitTimeDateWithTimeZone() {
		final DateTimeParser dtp = new DateTimeParser();

		assertEquals(dtp.determineFormatString("01/30/2012 10:59:48 GMT"), "MM/dd/yyyy HH:mm:ss z");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitDateTrainSlash() {
		final DateTimeParser det = new DateTimeParser();
		final String sample = "12/12/12";
		det.train("12/12/12");
		det.train("12/12/32");
		det.train("02/22/02");
		for (int i = 0; i < 20; i++)
			det.train("02/02/99");
		det.train("02/O2/99");

		final DateTimeParserResult result = checkSerialization(det).getResult();
		assertEquals(result.getFormatString(), "MM/dd/yy");
		assertEquals(result.getType(), FTAType.LOCALDATE);
		final String regExp = result.getRegExp();
		assertEquals(regExp, "\\d{2}/\\d{2}/\\d{2}");
		assertTrue(sample.matches(regExp));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitDateTrainYYYYSlash() {
		final DateTimeParser det = new DateTimeParser().withDateResolutionMode(DateResolutionMode.MonthFirst);
		final String sample = "2012/12/12";
		det.train(sample);
		det.train("2012/11/11");
		det.train("2012/10/32");

		final DateTimeParserResult result = det.getResult();
		assertEquals(result.getFormatString(), "yyyy/MM/dd");
		final String regExp = result.getRegExp();
		assertEquals(regExp, "\\d{4}/\\d{2}/\\d{2}");
		assertTrue(sample.matches(regExp));

		assertTrue(result.isValid("2012/12/12"));
		assertFalse(result.isValid("2012/10/32"));
		assertFalse(result.isValid("20121/10/32"));
		assertFalse(result.isValid("201/10/32"));

		assertTrue(result.isValid8("2012/12/12"));
		assertFalse(result.isValid8("2012/10/32"));
		assertFalse(result.isValid8("20121/10/32"));
		assertFalse(result.isValid8("201/10/32"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitDateTrainYYYYWithTime() {
		final DateTimeParser det = new DateTimeParser().withDateResolutionMode(DateResolutionMode.MonthFirst);
		final String sample = "2012/12/12 12:12:12.4";
		det.train(sample);
		det.train("2012/11/11 11:11:11.4");
		det.train("2012/10/32 10:10:10.2");

		final DateTimeParserResult result = checkSerialization(det).getResult();
		assertEquals(result.getFormatString(), "yyyy/MM/dd HH:mm:ss.S");
		final String regExp = result.getRegExp();
		assertEquals(regExp, "\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d");
		assertTrue(sample.matches(regExp));

		assertTrue(result.isValid(sample));

		assertTrue(result.isValid8(sample));
	}

	// If we were H (0-23) and we have a k (1-24) then assume k
	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void flipToK() {
		final DateTimeParser det = new DateTimeParser();
		det.train("11:30");
		det.train("9:30");
		det.train("24:30");

		final DateTimeParserResult result = checkSerialization(det).getResult();
		assertEquals(result.getFormatString(), "k:mm");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void dayDetermined() {
		final DateTimeParser det = new DateTimeParser();
		det.train("01/01/2000");
		det.train("02/02/2002");
		det.train("02/31/2002");

		final DateTimeParserResult result = checkSerialization(det).getResult();
		assertEquals(result.getFormatString(), "MM/dd/yyyy");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void unknownShrunk() {
		final DateTimeParser det = new DateTimeParser();
		det.train("01/10/2000");
		det.train("02/11/2002");
		det.train("02/9/2002");

		final DateTimeParserResult result = checkSerialization(det).getResult();
		assertEquals(result.getFormatString(), "??/?/yyyy");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void dd_to_ppD() {
		final DateTimeParser det = new DateTimeParser();
		det.train("Oct 21 2002");
		det.train("Dec 22 2003");
		det.train("Jan  1 2024");

		final DateTimeParserResult result = checkSerialization(det).getResult();
		assertEquals(result.getFormatString(), "MMM ppd yyyy");

		final DateTimeParser det2 = new DateTimeParser();
		det2.train("May 21 2002");
		det2.train("May 22 2003");
		det2.train("June 1 2024");

		final DateTimeParserResult result2 = checkSerialization(det2).getResult();
		assertEquals(result2.getFormatString(), "MMMM d yyyy");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void switch_MMM_to_MMMM() {
		final DateTimeParser det = new DateTimeParser();
		det.train("May 21 2002");
		det.train("May 22 2003");
		det.train("June 1 2024");

		final DateTimeParserResult result = checkSerialization(det).getResult();
		assertEquals(result.getFormatString(), "MMMM d yyyy");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void yyyyMd() {
		final DateTimeParser det = new DateTimeParser().withLocale(Locale.US);
		final String sample = "8547 8 6";
		det.train(sample);

		final DateTimeParserResult result = det.getResult();
		assertEquals(result.getFormatString(), "yyyy M d");
		assertEquals(result.getType(), FTAType.LOCALDATE);
		final String regExp = result.getRegExp();
		assertEquals(regExp, "\\d{4} \\d{1,2} \\d{1,2}");
		assertTrue(sample.matches(regExp));
		assertNull(checkParseable(result, sample, Locale.US));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void timeFirst() {
		final DateTimeParser det = new DateTimeParser();
		final String sample = "7:05 5/4/38";
		det.train(sample);

		final DateTimeParserResult result = det.getResult();
		assertEquals(result.getFormatString(), "H:mm ?/?/yy");
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		final String regExp = result.getRegExp();
		assertEquals(regExp, "\\d{1,2}:\\d{2} \\d{1,2}/\\d{1,2}/\\d{2}");
		assertTrue(sample.matches(regExp));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void githubIssue5() {
		final String[] months = { "March 26, 2015", "August 5, 2006", "May 10, 2014" };

		DateTimeParser dtp = new DateTimeParser()
		   .withDateResolutionMode(DateTimeParser.DateResolutionMode.Auto)
		    .withStrictMode(true).withLocale(Locale.US);

		for (final String s : months)
			dtp.train(s);

		DateTimeParserResult result = dtp.getResult();
		assertEquals(result.getFormatString(), "MMMM d, yyyy");

		for (final String s : months)
			assertNull(checkParseable(result, s, Locale.US));

		final String[] monthAbbr = { "25 Sep, 2018", "21 Mar, 2015", "2 Sep, 2010" };

		dtp = new DateTimeParser()
		    .withDateResolutionMode(DateTimeParser.DateResolutionMode.Auto)
		    .withStrictMode(true).withLocale(Locale.forLanguageTag("en-US"));

		for (final String s : monthAbbr)
			dtp.train(s);

		result = dtp.getResult();
		assertEquals(result.getFormatString(), "d MMM, yyyy");

		for (final String s : monthAbbr)
			assertNull(checkParseable(result, s, Locale.US));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void githubIssue6() {
		final String[] inputs = {
	            "6/30/2008 17:30", "6/30/2008 17:34", "7/31/2008 17:35", "12/15/2007 10:52",
	            "7/31/2008 17:40", "7/31/2008 17:36", "12/15/2008 3:14", "12/15/2008 13:05",
	            "8/31/2008 17:47", "7/31/2008 17:37", "7/31/2008 17:38", "7/31/2008 17:39",
	            "8/31/2008 17:43", "8/31/2008 17:44", "12/15/2008 3:15", "12/15/2008 18:18",
	            "8/31/2008 17:45", "8/31/2008 17:46", "12/15/2008 18:17", "9/30/2008 17:48",
	            "9/30/2008 17:53"
		};

		final DateTimeParser dtp = new DateTimeParser()
			    .withDateResolutionMode(DateTimeParser.DateResolutionMode.Auto)
			    .withLocale(Locale.ENGLISH)
			    .withStrictMode(true);

		for (final String s : inputs)
			dtp.train(s);

		DateTimeParserResult result = dtp.getResult();
		assertEquals(result.getFormatString(), "M/dd/yyyy H:mm");

		for (final String s : inputs)
			assertNull(checkParseable(result, s, Locale.ENGLISH));

		final String[] fails = {
			"6/30/2008 17:30", "6/30/2008 17:34", "7/31/2008 17:35", "12/15/2007 10:52",
			"7/31/2008 17:40", "7/31/2008 17:36", "12/15/2008 3:14", "12/15/2008 13:05",
			"8/31/2008 17:47", "7/31/2008 17:37", "7/31/2008 17:38", "7/31/2008 17:39",
			"8/31/2008 17:43", "8/31/2008 17:44", "12/15/2008 3:15", "12/15/2008 18:18",
			"8/31/2008 17:45", "8/31/2008 17:46", "12/15/2008 18:17", "9/30/2008 17:48",
			"9/30/2008 17:53", "12/15/2008 3:16", "9/30/2008 17:50", "9/30/2008 17:51",
			"10/31/2008 17:55", "12/15/2008 3:11", "10/31/2008 18:03", "10/31/2008 18:04",
			"12/15/2007 10:53", "10/31/2008 17:57", "10/31/2008 17:58", "10/31/2008 17:59",
			"10/31/2008 18:00", "10/31/2008 18:01", "10/31/2008 18:02", "11/30/2008 18:05",
			"11/30/2008 18:10", "11/30/2008 18:07", "11/30/2008 18:08", "11/30/2008 18:09",
			"11/30/2008 18:11", "11/30/2008 18:12", "12/15/2008 18:13", "12/15/2008 18:16",
			"12/15/2008 18:20", "12/15/2008 18:25", "12/15/2008 18:26", "12/15/2008 17:20",
			"12/15/2008 17:22", "12/15/2008 3:08", "12/15/2008 18:12", "12/15/2008 3:22",
			"12/15/2008 3:23", "12/15/2008 3:24"
		};

		for (final String s : fails)
			dtp.train(s);

		result = dtp.getResult();
		assertEquals(result.getFormatString(), "M/dd/yyyy H:mm");

		for (final String s : fails)
			assertNull(checkParseable(result, s, Locale.ENGLISH));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void bogusInput() {
		final String pipedInput = "12:45:64.|21/12/99:|21/12/99:|18:46:|4:38  39|3124/08/|890/65 1/|7/87/33| 89:50|18:52 56:|18/94/06|0463 5 71|50 9:22|" +
				"95/06/88|0-27-98|08/56 22/|31-0-99|0/7:6/11 //61|8:73/4/13 15|14/23/3367| 00/21/79|22-23-00|0/20/2361|0/2/52 9:50 4 |" +
				"4/01/41 3:43 T450|37/8/005 5:05|0/6/95|2000-12-12T12:45-72|2000-12-12T12:45-112|" +
				"12:45:64.|84:12:45.5712:45| 12:45:63.3 |";
		final String[] inputs = pipedInput.split("\\|");

		for (final String testCase : inputs) {
			final DateTimeParser det = new DateTimeParser();
			det.train(testCase);
			final DateTimeParserResult result = checkSerialization(det).getResult();
			if (result != null)
				System.err.println(result.getFormatString());
			assertNull(result, testCase);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitHHMMTrain() {
		final DateTimeParser det = new DateTimeParser();
		final String sampleOne = "12:57";
		final String sampleThree = "8:03";
		det.train(sampleOne);
		det.train("13:45");
		det.train(sampleThree);

		final DateTimeParserResult result = checkSerialization(det).getResult();
		assertEquals(result.getFormatString(), "H:mm");
		assertEquals(result.getType(), FTAType.LOCALTIME);
		final String regExp = result.getRegExp();
		assertEquals(regExp, "\\d{1,2}:\\d{2}");
		assertTrue(sampleOne.matches(regExp));
		assertTrue(sampleThree.matches(regExp));
	}

	private void add(final Map<String, Integer> counter, final String key) {
		final Integer seen = counter.get(key);
		if (seen == null)
			counter.put(key, 1);
		else
			counter.put(key, seen + 1);
	}

	private void dump(final Map<String, Integer> counter) {
		final Map<String, Integer> byValue = Utils.sortByValue(counter);
		for (final Map.Entry<String, Integer> entry : byValue.entrySet()) {
			logger.debug("'%s' : %d", entry.getKey(), entry.getValue());
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuit30523() {
		final DateTimeParser det = new DateTimeParser();
		final String input = "9:12:45 30/5/23";
		det.train(input);

		final DateTimeParserResult result = checkSerialization(det).getResult();
		final String formatString = result.getFormatString();
		assertEquals(formatString, "H:mm:ss ??/M/??");

		final String regExp = result.getRegExp();
		assertTrue(input.matches(regExp), "input: '" + input + "', RE: '" + regExp + "'");
	}

//	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void fuzz() {
		final Map<String, Integer> formatStrings = new HashMap<>();
		final Map<String, Integer> types = new HashMap<>();
		int good = 0;
		final int iterations = 100000000;
		final String[] timeZones = TimeZone.getAvailableIDs();

		for (int iters = 0; iters < iterations; iters++) {
			final int len = 5 + random.nextInt(15);
			final StringBuilder s = new StringBuilder(len);
			int digits = 0;
			for (int i = 0; s.length() <= len; ++i) {
				final int randomInt = random.nextInt(100);
				if (randomInt < 10) {
					if (Math.abs(randomInt % 2) == 1)
						s.append("2000-12-12");
					else
						s.append("12:45");
					continue;
				}
				if (randomInt < 50) {
					if (i == 10 && randomInt % 10 == 1) {
						s.append('T');
						continue;
					}
					if (digits == 4) {
						i--;
						continue;
					}
					s.append("0123456789".charAt(randomInt % 10));
					digits++;
					continue;
				}
				digits = 0;
				if (randomInt < 60) {
					s.append(':');
					continue;
				}
				if (randomInt < 70) {
					s.append('/');
					continue;
				}
				if (randomInt < 80) {
					if (i < 10)
						s.append('-');
					else
						s.append(Math.abs(randomInt % 2)  == 1 ? '+' : '-');
					continue;
				}
				if (randomInt < 95) {
					s.append(' ');
					continue;
				}
				if (randomInt < 97) {
					s.append('T');
					continue;
				}
				if (randomInt < 99) {
					final int idx = random.nextInt(timeZones.length - 1);
					s.append(timeZones[idx]);
					continue;
				}
				s.append(",.;':\"[]{}\\|=!@#$%^&*<>".charAt(random.nextInt(100) % 23));
			}

			final int amPmSwitch = random.nextInt(100);
			if (amPmSwitch == 98)
				s.append("am");
			else if (amPmSwitch == 99)
				s.append("pm");

			final DateTimeParser det = new DateTimeParser();
			final String input = s.toString();
			//logger.debug("Input ... '%s'", input);
			final String trimmed = input.trim();
			try {
				det.train(input);

				final DateTimeParserResult result = det.getResult();
				if (result != null) {
					good++;
					final String formatString = result.getFormatString();

					final String regExp = result.getRegExp();
					assertTrue(trimmed.matches(regExp), "input: '" + trimmed + "', RE: '" + regExp + "'");

					final FTAType type = result.getType();
					add(formatStrings, formatString);
					add(types, type.toString());
					result.parse(trimmed);
					if (formatString.indexOf('?') != -1)
						continue;

					try {
						final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatString);
						if (FTAType.LOCALTIME.equals(type))
							LocalTime.parse(trimmed, formatter);
						else if (FTAType.LOCALDATE.equals(type))
							LocalDate.parse(trimmed, formatter);
						else if (FTAType.LOCALDATETIME.equals(type))
							LocalDateTime.parse(trimmed, formatter);
						else if (FTAType.ZONEDDATETIME.equals(type))
							ZonedDateTime.parse(trimmed, formatter);
						else
							OffsetDateTime.parse(trimmed, formatter);
					}
					catch (DateTimeParseException exc) {
						logger.error("Java: Struggled with input of the form: '{}'", input);
					}

				}
			}
			catch (Throwable e) {
				logger.error("Struggled with input of the form: '{}'", input);
			}
		}

		dump(formatStrings);
		dump(types);

		logger.error("Good %d out of %d (%%%f)", good, iterations, 100*((float)good/iterations));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitMMDDYYYYHHMMSSTrain() {
		final DateTimeParser det = new DateTimeParser();
		final String sample = "01/26/2012 10:42:23 GMT";
		det.train(sample);
		det.train("01/30/2012 10:59:48 GMT");
		det.train("01/25/2012 16:46:43 GMT");
		det.train("01/25/2012 16:28:42 GMT");
		det.train("01/24/2012 16:53:04 GMT");

		final DateTimeParserResult result = checkSerialization(det).getResult();
		assertEquals(result.getFormatString(), "MM/dd/yyyy HH:mm:ss z");
		assertEquals(result.getType(), FTAType.ZONEDDATETIME);

		final String regExp = result.getRegExp();
		assertEquals(regExp, "\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2} .*");
		assertTrue(sample.matches(regExp));

		assertTrue(result.isValid("01/26/2012 10:42:23 GMT"));
		assertTrue(result.isValid8("01/26/2012 10:42:23 GMT"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void testPerf() {
		final DateTimeParser det = new DateTimeParser();
		final String sample = "01/26/2012 10:42:23 GMT";
		det.train(sample);
		det.train("01/30/2012 10:59:48 GMT");
		det.train("01/25/2012 16:46:43 GMT");
		det.train("01/25/2012 16:28:42 GMT");
		det.train("01/24/2012 16:53:04 GMT");

		final DateTimeParserResult result = checkSerialization(det).getResult();
		assertEquals(result.getFormatString(), "MM/dd/yyyy HH:mm:ss z");
		assertEquals(result.getType(), FTAType.ZONEDDATETIME);

		final String regExp = result.getRegExp();
		assertEquals(regExp, "\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2} .*");
		assertTrue(sample.matches(regExp));

		final int iterations = 10000000;
		final long start = System.currentTimeMillis();
		for (int i = 0; i < iterations; i++) {
			assertTrue(result.isValid("01/26/2012 10:42:23 GMT"));
		}
		final long doneCustom = System.currentTimeMillis();
		for (int i = 0; i < iterations; i++) {
			assertTrue(result.isValid8("01/26/2012 10:42:23 GMT"));
		}
		final long done = System.currentTimeMillis();
		logger.debug("Custom = {}ms, Java = {}ms.", doneCustom - start, done - doneCustom);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitInsufficientFactsTrain() {
		final DateTimeParser detPrime = new DateTimeParser();
		String sample = "12/30/99";

		detPrime.train(sample);

		final DateTimeParserResult resultPrime = detPrime.getResult();
		assertEquals(resultPrime.getFormatString(), "MM/dd/yy");

		String regExp = resultPrime.getRegExp();
		assertEquals(regExp, "\\d{2}/\\d{2}/\\d{2}");
		assertTrue(sample.matches(regExp));

		final DateTimeParser det = new DateTimeParser();
		sample = " 04/03/13";

		det.train(sample);
		det.train(" 05/03/13");
		det.train(" 06/03/13");
		det.train(" 07/03/13");
		det.train(" 08/03/13");
		det.train(" 09/03/13");
		det.train(" 10/03/13");

		DateTimeParserResult result = checkSerialization(det).getResult();
		assertEquals(result.getFormatString(), "??/??/??");

		regExp = result.getRegExp();
		assertEquals(regExp, "\\d{2}/\\d{2}/\\d{2}");
		assertTrue(sample.trim().matches(regExp));

		// Force to be day first
		final DateTimeParser detDayFirst = new DateTimeParser().withDateResolutionMode(DateResolutionMode.DayFirst);
		sample = " 04/03/13";

		detDayFirst.train(sample);
		detDayFirst.train(" 05/03/13");
		detDayFirst.train(" 06/03/13");
		detDayFirst.train(" 07/03/13");
		detDayFirst.train(" 08/03/13");
		detDayFirst.train(" 09/03/13");
		detDayFirst.train(" 10/03/13");

		result = detDayFirst.getResult();
		assertEquals(result.getFormatString(), "dd/MM/yy");

		regExp = result.getRegExp();
		assertEquals(regExp, "\\d{2}/\\d{2}/\\d{2}");
		assertTrue(sample.trim().matches(regExp));

		assertTrue(result.isValid("31/12/12"));
		assertFalse(result.isValid("12/31/12"));
		assertFalse(result.isValid("2012/12/12"));
		assertTrue(result.isValid8("31/12/12"));
		assertFalse(result.isValid8("12/31/12"));
		assertFalse(result.isValid8("2012/12/12"));

		// Force to be month first
		final DateTimeParser detMonthFirst = new DateTimeParser().withDateResolutionMode(DateResolutionMode.MonthFirst);
		sample = " 04/03/13";

		detMonthFirst.train(sample);
		detMonthFirst.train(" 05/03/13");
		detMonthFirst.train(" 06/03/13");
		detMonthFirst.train(" 07/03/13");
		detMonthFirst.train(" 08/03/13");
		detMonthFirst.train(" 09/03/13");
		detMonthFirst.train(" 10/03/13");

		result = detMonthFirst.getResult();
		assertEquals(result.getFormatString(), "MM/dd/yy");

		regExp = result.getRegExp();
		assertEquals(regExp, "\\d{2}/\\d{2}/\\d{2}");
		assertTrue(sample.trim().matches(regExp));

		assertFalse(result.isValid("31/12/12"));
		assertTrue(result.isValid("12/31/12"));
		assertFalse(result.isValid("2012/12/12"));
		assertFalse(result.isValid8("31/12/12"));
		assertTrue(result.isValid8("12/31/12"));
		assertFalse(result.isValid8("2012/12/12"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitDateMMddyy() {
		final DateTimeParser det = new DateTimeParser();
		final String sample = " 04/03/13";

		det.train(sample);
		det.train(" 05/03/13");
		det.train(" 06/03/13");
		det.train(" 07/03/13");
		det.train(" 08/03/13");
		det.train(" 09/30/13");
		det.train(" 10/03/13");
		for (int i = 0; i < 20; i++) {
			det.train("10/10/13");
		}

		final DateTimeParserResult result = checkSerialization(det).getResult();
		assertEquals(result.getFormatString(), "MM/dd/yy");

		final String regExp = result.getRegExp();
		assertEquals(regExp, "\\d{2}/\\d{2}/\\d{2}");
		assertTrue(sample.trim().matches(regExp));

		assertTrue(result.isValid("12/12/12"));
		assertFalse(result.isValid("1/1/1"));
		assertFalse(result.isValid("123/1/1"));
		assertFalse(result.isValid("1/123/1"));
		assertFalse(result.isValid("1/1/123"));

		assertTrue(result.isValid8("12/12/12"));
		assertFalse(result.isValid8("1/1/1"));
		assertFalse(result.isValid8("123/1/1"));
		assertFalse(result.isValid8("1/123/1"));
		assertFalse(result.isValid8("1/1/123"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitDateddMMyyyyHHmmss() {
		final DateTimeParser det = new DateTimeParser().withDateResolutionMode(DateResolutionMode.MonthFirst);
		final String sample = "2/7/2012 06:24:47";

		det.train(sample);
		det.train("2/7/2012 09:44:04");
		det.train("2/7/2012 06:21:26");
		det.train("2/7/2012 06:21:30");
		det.train("2/7/2012 06:21:31");
		det.train("2/7/2012 06:21:34");
		det.train("2/7/2012 06:21:38");
		det.train("1/7/2012 23:16:14");
		det.train("19/7/2012 17:49:53");

		DateTimeParserResult result = det.getResult();
		assertEquals(result.getFormatString(), "d/M/yyyy HH:mm:ss");
		result = checkSerialization(det).getResult();
		assertEquals(result.getFormatString(), "d/M/yyyy HH:mm:ss");

		final String regExp = result.getRegExp();
		assertEquals(regExp, "\\d{1,2}/\\d{1,2}/\\d{4} \\d{2}:\\d{2}:\\d{2}");
		assertTrue(sample.trim().matches(regExp));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitDateyyMMdd() {
		final DateTimeParser det = new DateTimeParser();
		final String sample = "98/03/13";

		det.train(sample);
		det.train("03/03/13");
		det.train("34/03/13");
		det.train("46/03/13");
		det.train("59/03/13");
		det.train("09/03/31");
		det.train("10/03/13");

		final DateTimeParserResult result = checkSerialization(det).getResult();
		assertEquals(result.getFormatString(), "yy/MM/dd");

		final String regExp = result.getRegExp();
		assertEquals(regExp, "\\d{2}/\\d{2}/\\d{2}");
		assertTrue(sample.trim().matches(regExp));

		assertTrue(result.isValid("12/12/12"));
		assertFalse(result.isValid("12/13/12"));
		assertFalse(result.isValid("1/1/1"));
		assertFalse(result.isValid("123/1/1"));
		assertFalse(result.isValid("1/123/1"));
		assertFalse(result.isValid("1/1/123"));

		assertTrue(result.isValid8("12/12/12"));
		assertFalse(result.isValid8("12/13/12"));
		assertFalse(result.isValid8("1/1/1"));
		assertFalse(result.isValid8("123/1/1"));
		assertFalse(result.isValid8("1/123/1"));
		assertFalse(result.isValid8("1/1/123"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitDatedMMMyy() {
		final DateTimeParser det = new DateTimeParser();
		final String sample = "1-Jan-14";
		det.train(sample);
		det.train("10-Jan-14");

		final DateTimeParserResult result = checkSerialization(det).getResult();
		assertEquals(result.getFormatString(), "d-MMM-yy");

		final String regExp = result.getRegExp();
		assertEquals(regExp, "\\d{1,2}-" + KnownTypes.PATTERN_ALPHA + "{3}-\\d{2}");
		assertTrue(sample.trim().matches(regExp));

		assertTrue(result.isValid("1-Jan-14"));
		assertTrue(result.isValid("10-Jan-14"));

		assertTrue(result.isValid8("1-Jan-14"));
		assertTrue(result.isValid8("10-Jan-14"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void javaSimple() {
		final DateTimeParser det = new DateTimeParser().withLocale(Locale.forLanguageTag("en-US"));
		long millis = System.currentTimeMillis();
		Date d = new Date();

		for (int i = 0; i < 100; i++) {
			d = new Date(millis);
			det.train(d.toString());
			millis -= 186400000;
		}

		final DateTimeParserResult result = checkSerialization(det).getResult();
		assertEquals(result.getFormatString(), "EEE MMM dd HH:mm:ss z yyyy");

		final String regExp = result.getRegExp();
		assertEquals(regExp, KnownTypes.PATTERN_ALPHA + "{3} " + KnownTypes.PATTERN_ALPHA + "{3} \\d{2} \\d{2}:\\d{2}:\\d{2} .* \\d{4}");
		assertTrue(d.toString().matches(regExp));
		assertTrue(result.isValid8(d.toString()));
		assertTrue(result.isValid(d.toString()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitHHMMSSTrain() {
		final DateTimeParser det = new DateTimeParser();
		final String sample = "12:57:03";
		det.train(sample);
		det.train("13:45:00");
		det.train("8:03:59");

		final DateTimeParserResult result = checkSerialization(det).getResult();
		assertEquals(result.getFormatString(), "H:mm:ss");

		final String regExp = result.getRegExp();
		assertEquals(regExp, "\\d{1,2}:\\d{2}:\\d{2}");
		assertTrue(sample.trim().matches(regExp));

		assertTrue(result.isValid("12:57:03"));
		assertTrue(result.isValid("8:03:59"));
		assertFalse(result.isValid("8:03:599"));
		assertFalse(result.isValid("118:03:59"));
		assertFalse(result.isValid("118:3:59"));
		assertFalse(result.isValid("118:333:59"));

		assertTrue(result.isValid8("12:57:03"));
		assertTrue(result.isValid8("8:03:59"));
		assertFalse(result.isValid8("8:03:599"));
		assertFalse(result.isValid8("118:03:59"));
		assertFalse(result.isValid8("118:3:59"));
		assertFalse(result.isValid8("118:333:59"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitMMMHHmmssa() {
		checker("May 8, 2009 5:57:51 PM", "MMM d, yyyy h:mm:ss a", FTAType.LOCALDATETIME, DateResolutionMode.None, Locale.ENGLISH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuityyyyMMddHmm() {
		checker("2014/4/8 22:05", "yyyy/M/d HH:mm", FTAType.LOCALDATETIME, DateResolutionMode.None, Locale.ENGLISH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void embeddedQuote() {
		checker("oct 7, '20", "MMM d, ''yy", FTAType.LOCALDATE, DateResolutionMode.None, Locale.ENGLISH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitMMMdyy() {
		checker("oct. 7, 20", "MMM. d, yy", FTAType.LOCALDATE, DateResolutionMode.None, Locale.ENGLISH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuityyyyMMdHHmmss() {
		checker("2014/04/2 03:00:51", "yyyy/MM/d HH:mm:ss", FTAType.LOCALDATETIME, DateResolutionMode.None, Locale.ENGLISH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuityyyyMdHHmm() {
		checker("2014:4:8 22:05", "yyyy:M:d HH:mm", FTAType.LOCALDATETIME, DateResolutionMode.None, Locale.ENGLISH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuityyyyMMdHHmmssColons() {
		checker("2014:04:2 03:00:51", "yyyy:MM:d HH:mm:ss", FTAType.LOCALDATETIME, DateResolutionMode.None, Locale.ENGLISH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuityyyyMddHHmmssColons() {
		checker("2014:4:02 03:00:51", "yyyy:M:dd HH:mm:ss", FTAType.LOCALDATETIME, DateResolutionMode.None, Locale.ENGLISH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuityyyyMddColons() {
		checker("2014:03:31", "yyyy:MM:dd", FTAType.LOCALDATE, DateResolutionMode.None, Locale.ENGLISH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitHHmmColons() {
		checker("03:31", "HH:mm", FTAType.LOCALTIME, DateResolutionMode.None, Locale.ENGLISH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitwithTimeZone() {
		checker("06/Jan/2008 15:04:05 -0700", "dd/MMM/yyyy HH:mm:ss xx", FTAType.OFFSETDATETIME, DateResolutionMode.None, Locale.ENGLISH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitSpaceAMSpacewithTZ() {
		checker("11/15/2014 02:00:00 AM +0000", "MM/dd/yyyy hh:mm:ss a xx", FTAType.OFFSETDATETIME, DateResolutionMode.None, Locale.ENGLISH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitSpaceAMwithTZ() {
		checker("11/15/2014 02:00:00 AM+0000", "MM/dd/yyyy hh:mm:ss axx", FTAType.OFFSETDATETIME, DateResolutionMode.None, Locale.ENGLISH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitAMwithTZ() {
		checker("11/15/2014 02:00:00AM+0000", "MM/dd/yyyy hh:mm:ssaxx", FTAType.OFFSETDATETIME, DateResolutionMode.None, Locale.ENGLISH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitTimeAM() {
		checker("2:23 PM 29/04/77", "h:mm a dd/MM/yy", FTAType.LOCALDATETIME, DateResolutionMode.None, Locale.ENGLISH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitMddyyyyHHmmss() {
		checker("2/19/2012 09:44:04", "M/dd/yyyy HH:mm:ss", FTAType.LOCALDATETIME, DateResolutionMode.None, Locale.ENGLISH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitMMddyyyyHHmmssz() {
		checker("01/30/2012 10:59:48 GMT", "MM/dd/yyyy HH:mm:ss z", FTAType.ZONEDDATETIME, DateResolutionMode.None, Locale.ENGLISH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuityyyyMMddTHHmmssx() {
		checker("2004-01-01T00:00:00+05", "yyyy-MM-dd'T'HH:mm:ssx", FTAType.OFFSETDATETIME, DateResolutionMode.None, Locale.ENGLISH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuityyyyMMddTHHmmSSSX() {
		checker("2010-07-01T22:20:22.400Z", "yyyy-MM-dd'T'HH:mm:ss.SSSX", FTAType.OFFSETDATETIME, DateResolutionMode.None, Locale.ENGLISH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuityyyyMMddTHHmmssSSSxxx() {
		checker("2021-08-23T19:03:44.449-04:00", "yyyy-MM-dd'T'HH:mm:ss.SSSxxx", FTAType.OFFSETDATETIME, DateResolutionMode.None, Locale.ENGLISH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuityyyyMMddTHHmmssSxxx() {
		checker("2021-08-23T19:03:44.5-04:00", "yyyy-MM-dd'T'HH:mm:ss.Sxxx", FTAType.OFFSETDATETIME, DateResolutionMode.None, Locale.ENGLISH);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuityyyyMMddTHHmmz() {
		checker("2017-08-24 12:10 EDT", "yyyy-MM-dd HH:mm z", FTAType.ZONEDDATETIME, DateResolutionMode.None, Locale.ENGLISH);
	}

//	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
//	public void intuitZZZ() {
//		checker("01/31/2017 10:35:00 AM +0000", "MM/dd/yyyy hh:mm:ss a xx", FTAType.OFFSETDATETIME, DateResolutionMode.None, Locale.ENGLISH);
//	}
//
//	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
//	public void intuitZZZ2() {
//		checker("01/01/2017 10:35:00 AM +0000", "MM/dd/yyyy hh:mm:ss a xx", FTAType.OFFSETDATETIME, DateResolutionMode.None, Locale.ENGLISH);
//	}

	public void checker(final String testInput, final String expectedFormat, final FTAType type,
			final DateResolutionMode resolutionMode, final Locale locale) {
		final DateTimeParser dtp = new DateTimeParser().withDateResolutionMode(resolutionMode).withLocale(locale);

		// Check we can determine the format
		assertEquals(dtp.determineFormatString(testInput), expectedFormat);

		final DateTimeParserResult result = DateTimeParserResult.asResult(expectedFormat, resolutionMode, dtp.getConfig());

		// Check it is of the expected FTA type
		assertEquals(result.getType(), type);

		// Grab our slightly modified DateTimeFormatter (since it copes with case insensitivity)
		final DateTimeFormatter dtf = dtp.ofPattern(expectedFormat);

		String formatted = null;
		switch (type) {
		case LOCALDATE:
			final LocalDate ld = LocalDate.parse(testInput, dtf);
			formatted = ld.format(dtf);
			break;
		case LOCALTIME:
			final LocalTime lt = LocalTime.parse(testInput, dtf);
			formatted = lt.format(dtf);
			break;
		case LOCALDATETIME:
			final LocalDateTime ldt = LocalDateTime.parse(testInput, dtf);
			formatted = ldt.format(dtf);
			break;
		case OFFSETDATETIME:
			final OffsetDateTime odt = OffsetDateTime.parse(testInput, dtf);
			formatted = odt.format(dtf);
			break;
		case ZONEDDATETIME:
			final ZonedDateTime zdt = ZonedDateTime.parse(testInput, dtf);
			formatted = zdt.format(dtf);
			break;
		default:
			fail("Unexpected type");
		}

		// Check the result by parsing the original string and printing it
		assertTrue(formatted.equalsIgnoreCase(testInput));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitZZ() {
		final DateTimeParser dtp = new DateTimeParser().withDateResolutionMode(DateResolutionMode.DayFirst);

		assertEquals(dtp.determineFormatString("06/Jan/2008 15:04:05"), "dd/MMM/yyyy HH:mm:ss");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void intuitKKMMSSTrain() {
		for (int i = 0; i < 100; i++) {
			final DateTimeParser det = new DateTimeParser();
			final String sample = "24:57:03";
			det.train(sample);
			det.train("13:45:00");
			det.train("8:03:59");

			final DateTimeParserResult result = det.getResult();
			if (!"k:mm:ss".equals(result.getFormatString()))
				System.err.println("i = " + i);
			assertEquals(result.getFormatString(), "k:mm:ss", result.getFormatString());

			final String regExp = result.getRegExp();
			assertEquals(regExp, "\\d{1,2}:\\d{2}:\\d{2}");
			assertTrue(sample.trim().matches(regExp));

			assertTrue(result.isValid("12:57:03"));
			assertTrue(result.isValid("8:03:59"));
			assertFalse(result.isValid("8:03:599"));
			assertFalse(result.isValid("118:03:59"));
			assertFalse(result.isValid("118:3:59"));
			assertFalse(result.isValid("118:333:59"));

			assertTrue(result.isValid8("12:57:03"));
			assertTrue(result.isValid8("8:03:59"));
			assertFalse(result.isValid8("8:03:599"));
			assertFalse(result.isValid8("118:03:59"));
			assertFalse(result.isValid8("118:3:59"));
			assertFalse(result.isValid8("118:333:59"));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void check23() {
		final DateTimeParserResult result = DateTimeParserResult.asResult("yyyy-MM-dd HH:mm:ss.S{2,3}", DateResolutionMode.MonthFirst, new DateTimeParserConfig(Locale.US));
		assertEquals(result.timeFieldLengths[3].getMin(), 2);
		assertEquals(result.timeFieldLengths[3].getMax(), 3);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void check11() {
		final DateTimeParserResult result = DateTimeParserResult.asResult("H:mm:ss.S", DateResolutionMode.MonthFirst, new DateTimeParserConfig(Locale.US));
		assertEquals(result.timeFieldLengths[3].getMin(), 1);
		assertEquals(result.timeFieldLengths[3].getMax(), 1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void checkembedded11() {
		final DateTimeParserResult result = DateTimeParserResult.asResult("yyyy-MM-dd'T'HH:mm:ss.Sxxx", DateResolutionMode.MonthFirst, new DateTimeParserConfig(Locale.US));
		assertEquals(result.timeFieldLengths[3].getMin(), 1);
		assertEquals(result.timeFieldLengths[3].getMax(), 1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void strange() {
		final DateTimeParser dtp = new DateTimeParser().withLocale(Locale.US);
		assertEquals(dtp.determineFormatString("2023-02-27  09:56:22"), "yyyy-MM-dd  HH:mm:ss");
		assertEquals(dtp.determineFormatString("2023-02-27  9:56:22"), "yyyy-MM-dd ppH:mm:ss");
		assertEquals(dtp.determineFormatString("21_12_1959 04:12:30.123"), "dd_MM_yyyy HH:mm:ss.SSS");

		final DateTimeParser dtpBG = new DateTimeParser().withLocale(Locale.forLanguageTag("bg-BG"));
		// Bulgarian years are often written with a trailing 'г.', for example "18/03/2018г."
		assertEquals(dtpBG.determineFormatString("18/03/2018г."), "dd/MM/yyyy'г.'");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void badFormats() {
		final DateTimeParser dtp = new DateTimeParser().withLocale(Locale.US);
		assertNull(dtp.determineFormatString("9.33.2020"));
		assertNull(dtp.determineFormatString("33.9.2020"));
		assertNull(dtp.determineFormatString("13.13.2020"));

		// yyyy tests
		assertNull(dtp.determineFormatString("1020"));
		assertNull(dtp.determineFormatString("3020"));

		// yyyyMM tests
		assertNull(dtp.determineFormatString("202013"));
		assertNull(dtp.determineFormatString("102012"));
		assertNull(dtp.determineFormatString("302012"));

		// yyyyMMddHH tests
		assertNull(dtp.determineFormatString("2020023104"));
		assertNull(dtp.determineFormatString("2020123134"));
		assertNull(dtp.determineFormatString("2020123214"));
		assertNull(dtp.determineFormatString("1020120914"));
		assertNull(dtp.determineFormatString("3020120914"));

		assertNull(dtp.determineFormatString("2023-01-45 234:12"));
		assertNull(dtp.determineFormatString("1970-01-01T"));

		assertNull(dtp.determineFormatString("1970010112 11"));

		assertEquals(SimpleDateMatcher.getType("dd MMMM yyyy"), FTAType.LOCALDATE);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void testPlausibleDateLong() {
		assertTrue(DateTimeParser.plausibleDateLong(20201216, 4));
		assertTrue(DateTimeParser.plausibleDateLong(201216, 2));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void testTooLong() {
		assertTrue(DateTimeParser.plausibleDateLong(20201216, 4));
		assertTrue(DateTimeParser.plausibleDateLong(201216, 2));
	}

}
