/*
 * Copyright 2017-2019 Tim Segall
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
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.cobber.fta.DateTimeParser.DateResolutionMode;

public class DetermineDateTimeFormatTests {
	@Test
	public void allOptions() {
		// "H_mm","MM/dd/yy","dd/MM/yyyy","yyyy/MM/dd","yyyy-MM-dd'T'HH_mm_ssx",
		// "yyyy-MM-dd'T'HH_mm_ssxxx","yyyy-MM-dd'T'HH_mm_ssxxxxx","dd MMMM yyyy",
		// "dd-MMMM-yyyy","d MMMM yyyy","d-MMMM-yyyy","MMMM dd',' yyyy",
		// "MMMM d',' yyyy","MMMM dd yyyy","MMMM d yyyy","MMMM-dd-yyyy","MMMM-d-yyyy"
		// "2/13/98 9:57"
		final String input =
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

		DateTimeParser dtp = new DateTimeParser(DateResolutionMode.None, Locale.getDefault());
		Assert.assertEquals(dtp.determineFormatString("9:57"), "H:mm");
		final String inputs[] = input.split("\\|");
		final String fmts[] = new String[inputs.length];
		String text;
		final StringBuilder answer = new StringBuilder();
		LocalTime localTime = LocalTime.now();
		LocalDate localDate = LocalDate.now();
		LocalDateTime localDateTime = LocalDateTime.now();
		ZonedDateTime zonedDateTime = ZonedDateTime.now();
		OffsetDateTime offsetDateTime = OffsetDateTime.now();
		final Set<String> seen = new HashSet<String>();
		final Set<Integer> ignore = new HashSet<Integer>();

		final StringBuilder headerLine = new StringBuilder("RowID,");

		// Work out headers and which columns we want.
		for (int i = 0; i < inputs.length; i++) {
			fmts[i] = dtp.determineFormatString(inputs[i]);
				Assert.assertNotNull(fmts[i], inputs[i]);

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
				final DateTimeParser det = new DateTimeParser();
				det.train(inputs[i]);
				final DateTimeParserResult result = det.getResult();
				final PatternInfo.Type type = result.getType();
				try {
					final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(fmts[i]);
					if (PatternInfo.Type.LOCALTIME.equals(type)) {
						text = localTime.format(formatter);
						localTime = localTime.minusHours(209);
					}
					else if (PatternInfo.Type.LOCALDATE.equals(type)) {
						text = localDate.format(formatter);
						localDate = localDate.minusDays(29);
					}
					else if (PatternInfo.Type.LOCALDATETIME.equals(type)) {
						text = localDateTime.format(formatter);
						localDateTime = localDateTime.minusHours(209);
					}
					else if (PatternInfo.Type.ZONEDDATETIME.equals(type)) {
						text = zonedDateTime.format(formatter);
						zonedDateTime = zonedDateTime.minusHours(209);
					}
					else {
						text = offsetDateTime.format(formatter);
						offsetDateTime = offsetDateTime.minusHours(209);
					}
					answer.append("\"" + text + "\"");
					if (i + 1 < inputs.length)
						answer.append(',');
					else
						answer.append('\n');
				}
				catch (DateTimeParseException exc) {
					Assert.fail("Failure");
				}
			}
		}
		Assert.assertEquals(dtp.determineFormatString("9:57"), "H:mm");
	}

	@Test
	public void intuitTimeOnly() {
		DateTimeParser dtp = new DateTimeParser(DateResolutionMode.None, Locale.getDefault());

		Assert.assertNull(dtp.determineFormatString("12:02:1"));
		Assert.assertEquals(dtp.determineFormatString("9:57"), "H:mm");
		Assert.assertEquals(dtp.determineFormatString("12:57"), "HH:mm");
		Assert.assertEquals(dtp.determineFormatString("8:57:02"), "H:mm:ss");
		Assert.assertEquals(dtp.determineFormatString("12:57:02"), "HH:mm:ss");
		Assert.assertNull(dtp.determineFormatString(":57:02"));
		Assert.assertNull(dtp.determineFormatString("123:02"));
		Assert.assertNull(dtp.determineFormatString("12:023"));
		Assert.assertNull(dtp.determineFormatString("12:023:12"));
		Assert.assertNull(dtp.determineFormatString("12:0"));
		Assert.assertNull(dtp.determineFormatString("12:02:12:14"));
		Assert.assertNull(dtp.determineFormatString("12:02:124"));
		Assert.assertNull(dtp.determineFormatString("12:02:"));
		Assert.assertNull(dtp.determineFormatString("12::02"));
	}

	@Test
	public void intuitDateOnlySlash() {
		DateTimeParser dtp = new DateTimeParser(DateResolutionMode.None, Locale.getDefault());

		Assert.assertEquals(dtp.determineFormatString("2/12/98"), "?/??/yy");
		Assert.assertEquals(dtp.determineFormatString("2/2/02"), "?/?/yy");
		Assert.assertNull(dtp.determineFormatString("2/31/02"));
		Assert.assertEquals(dtp.determineFormatString("31/02/02"), "??/??/??");
		Assert.assertEquals(dtp.determineFormatString("12/12/98"), "??/??/yy");
		Assert.assertEquals(dtp.determineFormatString("14/12/98"), "dd/MM/yy");
		Assert.assertEquals(dtp.determineFormatString("12/14/98"), "MM/dd/yy");
		Assert.assertEquals(dtp.determineFormatString("12/12/2012"), "??/??/yyyy");
		Assert.assertEquals(dtp.determineFormatString("20/12/2012"), "dd/MM/yyyy");
		Assert.assertEquals(dtp.determineFormatString("11/15/2012"), "MM/dd/yyyy");
		Assert.assertEquals(dtp.determineFormatString("2012/12/13"), "yyyy/MM/dd");
		Assert.assertNull(dtp.determineFormatString("/57/02"));
		Assert.assertNull(dtp.determineFormatString("123/02"));
		Assert.assertNull(dtp.determineFormatString("12/023"));
		Assert.assertNull(dtp.determineFormatString("12/0"));
		Assert.assertNull(dtp.determineFormatString("12/02/1"));
		Assert.assertNull(dtp.determineFormatString("12/023/12"));
		Assert.assertNull(dtp.determineFormatString("12/02/"));
		Assert.assertNull(dtp.determineFormatString("12/02-99"));
	}

	/*
	@Test
	public void testSpaces() {
		Assert.assertEquals(DateTimeParser.parse("2018 12 24"), "yyyy MM dd");
	}
	*/

	@Test
	public void intuitDateOnlyDash() {
		DateTimeParser dtp = new DateTimeParser(DateResolutionMode.None, Locale.getDefault());

		Assert.assertEquals(dtp.determineFormatString("2-12-98"), "?-??-yy");
		Assert.assertEquals(dtp.determineFormatString("12-12-98"), "??-??-yy");
		Assert.assertEquals(dtp.determineFormatString("14-12-98"), "dd-MM-yy");
		Assert.assertEquals(dtp.determineFormatString("12-14-98"), "MM-dd-yy");
		Assert.assertEquals(dtp.determineFormatString("12-12-2012"), "??-??-yyyy");
		Assert.assertEquals(dtp.determineFormatString("2012-12-13"), "yyyy-MM-dd");
		Assert.assertEquals(dtp.determineFormatString("2012-13-12"), "yyyy-dd-MM");
		Assert.assertNull(dtp.determineFormatString("20120-12-12"));
	}

	@Test
	public void intuit8601DD() {
		DateTimeParser dtp = new DateTimeParser(DateResolutionMode.None, Locale.getDefault());

		Assert.assertEquals(dtp.determineFormatString("2004-01-01T00:00:00+05"), "yyyy-MM-dd'T'HH:mm:ssx");

		final DateTimeParser det = new DateTimeParser();
		final String sample = "2004-01-01T00:00:00+05";
		det.train(sample);

		final DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyy-MM-dd'T'HH:mm:ssx");

		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[-+][0-9]{2}([0-9]{2})?");
		Assert.assertTrue(sample.matches(regExp));

		Assert.assertTrue(result.isValid("2004-01-01T00:00:00+05"));
		Assert.assertTrue(result.isValid("2012-03-04T19:22:10+08"));
		Assert.assertTrue(result.isValid("2012-03-04T19:22:10+0830"));
		Assert.assertFalse(result.isValid("2012-03-04T19:22:10+?08"));

		Assert.assertTrue(result.isValid8("2004-01-01T00:00:00+05"));
		Assert.assertTrue(result.isValid8("2012-03-04T19:22:10+08"));
		Assert.assertTrue(result.isValid8("2012-03-04T19:22:10+0830"));
		Assert.assertFalse(result.isValid8("2012-03-04T19:22:10+?08"));
	}

	@Test
	public void intuitHHmmddMyy() {
		final String input = "00:53 15/2/17|17:53 29/7/16|10:53 11/1/16|03:53 25/6/15|20:53 06/12/14|13:53 20/5/14|06:53 01/11/13|23:53 14/4/13|" +
				"16:53 26/9/12|09:53 10/3/12|02:53 23/8/11|19:53 03/2/11|12:53 18/7/10|05:53 30/12/09|22:53 12/6/09|15:53 24/11/08|" +
				"08:53 08/5/08|01:53 21/10/07|18:53 03/4/07|11:53 15/9/06|04:53 27/2/06|21:53 10/8/05|14:53 22/1/05|07:53 06/7/04|" +
				"00:53 19/12/03|17:53 01/6/03|10:53 13/11/02|03:53 27/4/02|20:53 08/10/01|13:53 22/3/01|";
		final String inputs[] = input.split("\\|");
		final DateTimeParser det = new DateTimeParser(DateResolutionMode.DayFirst);

		for (int i = 0; i < inputs.length; i++) {
			det.train(inputs[i]);
		}

		final DateTimeParserResult result = det.getResult();
		final String formatString = result.getFormatString();

		Assert.assertEquals(formatString, "HH:mm dd/M/yy");

		final String regExp = result.getRegExp();

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(regExp));
		}
	}

	@Test
	public void twentyRecords() throws IOException {
		final String input = "1/3/11|1/3/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|7/25/11|9/17/08|1-15-2011|";
		final String inputs[] = input.split("\\|");
		final DateTimeParser det = new DateTimeParser(DateResolutionMode.MonthFirst);

		for (int i = 0; i < inputs.length; i++) {
			det.train(inputs[i]);
		}

		final DateTimeParserResult result = det.getResult();
		final String formatString = result.getFormatString();

		Assert.assertEquals(formatString, "M/dd/yy");
	}

	@Test
	public void intuit8601DDDDDD() {
		DateTimeParser dtp = new DateTimeParser(DateResolutionMode.None, Locale.getDefault());

		Assert.assertEquals(dtp.determineFormatString("2004-01-01T00:00:00+05:00:00"), "yyyy-MM-dd'T'HH:mm:ssxxxxx");

		final DateTimeParser det = new DateTimeParser();
		final String sample = "2004-01-01T00:00:00+05:00:00";
		det.train(sample);

		final DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyy-MM-dd'T'HH:mm:ssxxxxx");

		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[-+][0-9]{2}:[0-9]{2}(:[0-9]{2})?");
		Assert.assertTrue(sample.matches(regExp));

		Assert.assertTrue(result.isValid("2004-01-01T00:00:00+05:00:00"));
		Assert.assertTrue(result.isValid("2012-03-04T19:22:10+08:00:00"));
		Assert.assertFalse(result.isValid("2012-03-04T19:22:10+08:00:0"));
		Assert.assertFalse(result.isValid("2012-03-04T19:22:10+O8:00:00"));
		Assert.assertFalse(result.isValid("2012-03-04T19:22:10+08:60:00"));
		Assert.assertFalse(result.isValid("2012-03-04T19:22:10+08:00:60"));

		Assert.assertTrue(result.isValid8("2004-01-01T00:00:00+05:00:00"));
		Assert.assertTrue(result.isValid8("2012-03-04T19:22:10+08:00:00"));
		Assert.assertFalse(result.isValid8("2012-03-04T19:22:10+08:00:0"));
		Assert.assertFalse(result.isValid8("2012-03-04T19:22:10+O8:00:00"));
		Assert.assertFalse(result.isValid8("2012-03-04T19:22:10+08:00:60"));
	}

	@Test
	public void fullMonths() {
		DateTimeParser dtp = new DateTimeParser(DateResolutionMode.None, Locale.getDefault());

		Assert.assertEquals(dtp.determineFormatString("25 July 2018"), "dd MMMM yyyy");
		Assert.assertEquals(dtp.determineFormatString("25-July-2018"), "dd-MMMM-yyyy");
		Assert.assertEquals(dtp.determineFormatString("7 June 2017"), "d MMMM yyyy");
		Assert.assertEquals(dtp.determineFormatString("7-June-2017"), "d-MMMM-yyyy");

		Assert.assertEquals(dtp.determineFormatString("June 23, 2017"), "MMMM dd',' yyyy");
		Assert.assertEquals(dtp.determineFormatString("August 8, 2017"), "MMMM d',' yyyy");
		Assert.assertEquals(dtp.determineFormatString("August 18 2017"), "MMMM dd yyyy");
		Assert.assertEquals(dtp.determineFormatString("December 9 2017"), "MMMM d yyyy");
		Assert.assertEquals(dtp.determineFormatString("January-14-2017"), "MMMM-dd-yyyy");
		Assert.assertEquals(dtp.determineFormatString("February-4-2017"), "MMMM-d-yyyy");
	}

	@Test
	public void yyyyMMddHHmmz() {
		final String input = "2017-08-24 12:10 EDT|2017-07-03 06:10 EDT|2017-05-12 00:10 EDT|2017-03-20 18:10 EDT|2016-07-02 12:10 EDT|" +
				"2017-01-27 11:10 EST|2016-12-06 05:10 EST|2016-10-15 00:10 EDT|2016-08-23 18:10 EDT|2016-05-11 06:10 EDT|";
		final String inputs[] = input.split("\\|");
		final DateTimeParser det = new DateTimeParser();

		for (int i = 0; i < inputs.length; i++) {
			det.train(inputs[i]);
		}

		final DateTimeParserResult result = det.getResult();

		final String formatString = result.getFormatString();

		Assert.assertEquals(formatString, "yyyy-MM-dd HH:mm z");

		final String regExp = result.getRegExp();

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(regExp));
		}
	}

	@Test
	public void ddMyy() {
		final String input = "02/2/17|27/1/14|21/1/11|15/1/08|08/1/05|02/1/02|27/12/98|21/12/95|14/12/92|08/12/89|";
		final String inputs[] = input.split("\\|");
		final DateTimeParser det = new DateTimeParser(DateResolutionMode.DayFirst);

		for (int i = 0; i < inputs.length; i++) {
			det.train(inputs[i]);
		}

		final DateTimeParserResult result = det.getResult();

		final String formatString = result.getFormatString();

		Assert.assertEquals(formatString, "dd/M/yy");

		final String regExp = result.getRegExp();

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(regExp));
		}
	}

	@Test
	public void basicHMM() {
		final String input = "3:16|3:16|10:16|3:16|10:16|17:16|3:16|10:16|17:16|0:16|3:16|10:16|17:16|0:16|7:16|3:16|10:16|" +
		"17:16|0:16|7:16|14:16|3:16|10:16|17:16|0:16|7:16|14:16|21:16|3:16|10:16|17:16|0:16|7:16|14:16|" +
		"21:16|4:16|3:16|10:16|17:16|0:16|7:16|14:16|21:16|4:16|11:16|3:16|10:16|17:16|0:16|7:16|14:16|";
		final String inputs[] = input.split("\\|");
		final DateTimeParser det = new DateTimeParser();

		for (int i = 0; i < inputs.length; i++) {
			det.train(inputs[i]);
		}

		final DateTimeParserResult result = det.getResult();

		final String formatString = result.getFormatString();

		Assert.assertEquals(formatString, "H:mm");

		final String regExp = result.getRegExp();

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(regExp));
		}
	}

	@Test
	public void basicDTPDDMMMMYYYY() {
		final String input = "25 July 2018|12 August 1984|10 January 2000|1 January 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|12 August 1984|10 January 2000|1 January 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|12 August 1984|10 May 2000|1 April 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|13 August 1984|10 January 2000|1 May 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|12 November 1984|10 October 2000|1 January 1970|16 June 1934|06 July 1961|";
		final String inputs[] = input.split("\\|");
		final DateTimeParser det = new DateTimeParser();

		for (int i = 0; i < inputs.length; i++) {
			det.train(inputs[i]);
		}

		final DateTimeParserResult result = det.getResult();

		final String formatString = result.getFormatString();

		Assert.assertEquals(formatString, "d MMMM yyyy");

		final String regExp = result.getRegExp();

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(regExp));
		}
	}

	@Test
	public void basicAMPM() {
		DateTimeParser dtp = new DateTimeParser(DateResolutionMode.None, Locale.getDefault());

		Assert.assertEquals(dtp.determineFormatString("09/Mar/17 3:14 PM"), "dd/MMM/yy h:mm a");

		final String input = "09/Mar/17 3:14 PM|09/Mar/17 11:36 AM|09/Mar/17 9:12 AM|09/Mar/17 9:12 AM|09/Mar/17 9:12 AM|09/Mar/17 8:14 AM|" +
				"09/Mar/17 7:02 AM|09/Mar/17 6:59 AM|09/Mar/17 6:59 AM|09/Mar/17 6:59 AM|09/Mar/17 6:59 AM|09/Mar/17 6:59 AM|" +
				"09/Mar/17 6:59 AM|09/Mar/17 6:57 AM|08/Mar/17 8:12 AM|07/Mar/17 9:27 PM|07/Mar/17 3:34 PM|07/Mar/17 3:01 PM|" +
				"07/Mar/17 3:00 PM|07/Mar/17 2:51 PM|07/Mar/17 2:46 PM|07/Mar/17 2:40 PM|07/Mar/17 2:23 PM|07/Mar/17 11:04 AM|" +
				"02/Mar/17 10:57 AM|01/Mar/17 11:56 AM|01/Mar/17 6:14 AM|28/Feb/17 4:56 AM|27/Feb/17 5:58 AM|27/Feb/17 5:58 AM|" +
				"22/Feb/17 6:48 AM|18/Jan/17 8:29 AM|04/Jan/17 7:37 AM|10/Nov/16 10:42 AM|";
		final String inputs[] = input.split("\\|");
		final DateTimeParser det = new DateTimeParser();

		for (int i = 0; i < inputs.length; i++) {
			det.train(inputs[i]);
		}

		final DateTimeParserResult result = det.getResult();

		final String formatString = result.getFormatString();

		Assert.assertEquals(formatString, "dd/MMM/yy h:mm a");

		Assert.assertTrue(result.isValid("09/Mar/17 3:14 AM"));
		Assert.assertTrue(result.isValid8("09/Mar/17 3:14 AM"));
		Assert.assertTrue(result.isValid("09/Mar/17 3:14 PM"));
		Assert.assertTrue(result.isValid8("09/Mar/17 3:14 PM"));

		final String regExp = result.getRegExp();

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(regExp));
		}
	}

	//@Test FIX
	public void basicAMPM_viVN() {
		final Locale locale = Locale.forLanguageTag("vi-VN");
		DateTimeParser dtp = new DateTimeParser(DateResolutionMode.None, Locale.getDefault());

		Assert.assertEquals(dtp.determineFormatString("09/thg 3/17 3:14 SA"), "dd/MMM/yy h:mm a");

		final String input = "09/thg 3/17 3:14 CH|09/thg 3/17 11:36 SA|09/thg 3/17 9:12 SA|09/thg 3/17 9:12 SA|09/thg 3/17 9:12 SA|09/thg 3/17 8:14 SA|" +
				"09/thg 3/17 7:02 SA|09/thg 3/17 6:59 SA|09/thg 3/17 6:59 SA|09/thg 3/17 6:59 SA|09/thg 3/17 6:59 SA|09/thg 3/17 6:59 SA|" +
				"09/thg 3/17 6:59 SA|09/thg 3/17 6:57 SA|08/thg 3/17 8:12 SA|07/thg 3/17 9:27 CH|07/thg 3/17 3:34 CH|07/thg 3/17 3:01 CH|" +
				"07/thg 3/17 3:00 CH|07/thg 3/17 2:51 CH|07/thg 3/17 2:46 CH|07/thg 3/17 2:40 CH|07/thg 3/17 2:23 CH|07/thg 3/17 11:04 SA|" +
				"02/thg 3/17 10:57 SA|01/thg 3/17 11:56 SA|01/thg 3/17 6:14 SA|28/thg 2/17 4:56 SA|27/thg 2/17 5:58 SA|27/thg 2/17 5:58 SA|" +
				"22/thg 2/17 6:48 SA|18/thg 1/17 8:29 SA|04/thg 1/17 7:37 SA|10/thg 11/16 10:42 SA|";
		final String inputs[] = input.split("\\|");
		final DateTimeParser det = new DateTimeParser(DateResolutionMode.None, locale);

		for (int i = 0; i < inputs.length; i++) {
			det.train(inputs[i]);
		}

		final DateTimeParserResult result = det.getResult();

		final String formatString = result.getFormatString();

		Assert.assertEquals(formatString, "dd/MMM/yy h:mm a");

		Assert.assertTrue(result.isValid("09/thg 3/17 3:14 SA"));
		Assert.assertTrue(result.isValid8("09/Mar/17 3:14 CH"));
		Assert.assertTrue(result.isValid("09/Mar/17 3:14 SA"));
		Assert.assertTrue(result.isValid8("09/Mar/17 3:14 SA"));

		final String regExp = result.getRegExp();

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(regExp));
		}
	}

	@Test
	public void basic_hhmmss_AMPM() {
		final String input = "06:58:20 AM|07:25:18 PM|01:47:06 AM|05:32:48 AM|11:29:53 PM|02:21:10 PM|04:55:48 AM|" +
				"03:39:14 PM|09:43:02 PM|10:43:15 AM|05:46:07 AM|05:09:34 PM|06:03:58 AM|10:59:15 AM|10:13:28 AM|" +
				"10:25:10 AM|10:33:03 PM|04:24:44 AM|04:19:29 PM|01:08:48 AM|11:11:47 AM|10:11:06 PM|12:27:06 AM|" +
				"12:35:14 AM|01:30:55 PM|12:11:05 AM|09:23:51 PM|10:23:20 PM|01:29:57 PM|04:35:17 PM|06:53:23 PM|" +
				"09:23:13 PM|11:40:44 PM|01:01:02 AM|04:24:19 AM|08:51:48 AM|02:29:26 AM|08:48:32 AM|11:03:13 PM|" +
				"07:52:27 PM|04:51:40 PM|08:31:11 AM|07:53:57 AM|07:04:03 PM|12:05:00 AM|01:50:13 AM|";
		final String inputs[] = input.split("\\|");
		final DateTimeParser det = new DateTimeParser();

		for (int i = 0; i < inputs.length; i++) {
			det.train(inputs[i]);
		}

		final DateTimeParserResult result = det.getResult();

		final String formatString = result.getFormatString();

		Assert.assertEquals(formatString, "hh:mm:ss a");

		Assert.assertTrue(result.isValid("03:14:12 AM"));
		Assert.assertTrue(result.isValid8("03:14:12 AM"));
		Assert.assertTrue(result.isValid("03:14:12 PM"));
		Assert.assertTrue(result.isValid8("03:14:12 PM"));

		final String regExp = result.getRegExp();

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(regExp), inputs[i]);
		}
	}

	@Test
	public void basic_hhmmss_AMPM_2() {
		final String input = "06:58:20am|07:25:18pm|01:47:06am|05:32:48am|11:29:53pm|02:21:10pm|04:55:48am|" +
				"03:39:14pm|09:43:02pm|10:43:15am|05:46:07am|05:09:34pm|06:03:58am|10:59:15am|10:13:28am|" +
				"10:25:10am|10:33:03pm|04:24:44am|04:19:29pm|01:08:48am|11:11:47am|10:11:06pm|12:27:06am|" +
				"12:35:14am|01:30:55pm|12:11:05am|09:23:51pm|10:23:20pm|01:29:57pm|04:35:17pm|06:53:23pm|" +
				"09:23:13pm|11:40:44pm|01:01:02am|04:24:19am|08:51:48am|02:29:26am|08:48:32am|11:03:13pm|" +
				"07:52:27pm|04:51:40pm|08:31:11am|07:53:57am|07:04:03pm|12:05:00am|01:50:13am|";
		final String inputs[] = input.split("\\|");
		final DateTimeParser det = new DateTimeParser();

		for (int i = 0; i < inputs.length; i++) {
			det.train(inputs[i]);
		}

		final DateTimeParserResult result = det.getResult();

		final String formatString = result.getFormatString();

		Assert.assertEquals(formatString, "hh:mm:ssa");

		Assert.assertTrue(result.isValid("03:14:12am"));
		Assert.assertTrue(result.isValid("03:14:12pm"));
		// It hurts but there is no way to specify am/pm (lowercase) as valid without constructing your own formatter
		// Assert.assertTrue(result.isValid8("03:14:12am"));
		// Assert.assertTrue(result.isValid8("03:14:12pm"));

		final String regExp = result.getRegExp();

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(regExp), inputs[i]);
		}
	}

	@Test
	public void basicMMMdcommayyyy() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "August 20, 2017|August 20, 2017|July 22, 2017|August 5, 2017|July 22, 2017|June 23, 2017|August 20, 2017|July 22, 2017|June 23, 2017|" +
				"May 25, 2017|August 20, 2017|July 22, 2017|June 23, 2017|May 25, 2017|April 26, 2017|August 20, 2017|July 22, 2017|June 23, 2017|" +
				"May 25, 2017|April 26, 2017|March 28, 2017|August 20, 2017|July 22, 2017|June 23, 2017|May 25, 2017|April 26, 2017|March 28, 2017|" +
				"February 27, 2017|August 20, 2017|July 22, 2017|June 23, 2017|May 15, 2017|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "MMMM d',' yyyy");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{3,9} \\d{1,2}, \\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicyyyyddMM() throws IOException {
		final String input = "2011/28/02|2017/31/12|2016/20/10|1999/15/07|2017/31/12|2016/20/10|1999/15/07|2017/31/12|2017/31/12|2016/20/10|1999/15/07|2017/30/12|2017/21/12|2016/20/10|1999/15/07|2017/11/12|2012/31/12|2010/31/12|2016/20/10|1999/15/07|";
		final String inputs[] = input.split("\\|");
		final DateTimeParser det = new DateTimeParser();

		for (int i = 0; i < inputs.length; i++) {
			det.train(inputs[i]);
		}

		final DateTimeParserResult result = det.getResult();

		final String formatString = result.getFormatString();

		Assert.assertEquals(formatString, "yyyy/dd/MM");
	}

	@Test
	public void mixed_yyyyddMM() throws IOException {
		final String input = "1970/06/06|1971/01/06|1972/07/07|1973/03/03|1974/04/04|1970/05/05|1970/06/06|1970/08/08|1970/09/09|1970/10/10|1970/06/06|1971/01/06|1972/07/07|1973/03/03|1974/04/04|1970/05/05|1970/06/06|1970/08/08|1970/09/09|1970/10/10|2011/31/02|2017/31/12|2016/20/10|1999/15/07|";
		final String inputs[] = input.split("\\|");
		final DateTimeParser det = new DateTimeParser();

		for (int i = 0; i < inputs.length; i++) {
			det.train(inputs[i]);
		}

		final DateTimeParserResult result = det.getResult();

		final String formatString = result.getFormatString();

		Assert.assertEquals(formatString, "yyyy/dd/MM");
	}

	@Test
	public void basicDDMMMMYYYY() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "25 July 2018|12 August 1984|10 January 2000|1 January 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|12 August 1984|10 January 2000|1 January 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|12 August 1984|10 May 2000|1 April 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|13 August 1984|10 January 2000|1 May 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|12 November 1984|10 October 2000|1 January 1970|16 June 1934|06 July 1961|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "d MMMM yyyy");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{1,2} " + KnownPatterns.PATTERN_ALPHA + "{3,9} \\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void intuitMMMDDYYYY() {
		final String trimmed = "May 1, 2018";
		final DateTimeParser det = new DateTimeParser();
		det.train(trimmed);

		final DateTimeParserResult result = det.getResult();

		final String formatString = result.getFormatString();
		final PatternInfo.Type type = result.getType();

		Assert.assertEquals(formatString, "MMM d',' yyyy");

		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, KnownPatterns.PATTERN_ALPHA + "{3} \\d{1,2}, \\d{4}");
		Assert.assertTrue(trimmed.matches(regExp));

		try {
			final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatString);
			if (PatternInfo.Type.LOCALTIME.equals(type))
				LocalTime.parse(trimmed, formatter);
			else if (PatternInfo.Type.LOCALDATE.equals(type))
				LocalDate.parse(trimmed, formatter);
			else if (PatternInfo.Type.LOCALDATETIME.equals(type))
				LocalDateTime.parse(trimmed, formatter);
			else if (PatternInfo.Type.ZONEDDATETIME.equals(type))
				ZonedDateTime.parse(trimmed, formatter);
			else
				OffsetDateTime.parse(trimmed, formatter);
		}
		catch (DateTimeParseException exc) {
			Assert.fail("Java Should have successfully parsed: " + trimmed);
		}

		try {
			result.parse(trimmed);
		}
		catch (DateTimeParseException e) {
			Assert.fail("Should have successfully parsed: " + trimmed);
		}

		Assert.assertTrue(result.isValid8("Jun 30, 2023"));
		Assert.assertTrue(result.isValid("Jun 30, 2023"));
		Assert.assertFalse(result.isValid8("Jun 30 2023"));
		Assert.assertFalse(result.isValid("Jun 30 2023"));
		Assert.assertFalse(result.isValid("Jun 32, 2023"));
		Assert.assertFalse(result.isValid8("Jun 32, 2023"));
		Assert.assertFalse(result.isValid("Jun 30; 2023"));
		Assert.assertFalse(result.isValid8("Jun 30; 2023"));
		// Invalid but not detected!!
		Assert.assertTrue(result.isValid("Jun 31, 2023"));
		Assert.assertTrue(result.isValid8("Jun 31, 2023"));
	}

	@Test
	public void testAsResult() {
		Assert.assertNull(DateTimeParserResult.asResult("yyyy-MM-ddTHH:m:ssx", DateResolutionMode.None, Locale.getDefault()));
		Assert.assertNull(DateTimeParserResult.asResult("yyyy-MM-ddTHH:mm:sx", DateResolutionMode.None, Locale.getDefault()));
	}

	@Test
	public void testParse() {
		final DateTimeParserResult result = DateTimeParserResult.asResult("yyyy/MM/dd HH:mm", DateResolutionMode.None, Locale.getDefault());

		try {
			result.parse("2018/01/31 05:O5");
		}
		catch (DateTimeParseException e) {
			Assert.assertEquals(e.getMessage(), "Expecting digit");
			Assert.assertEquals(e.getErrorIndex(), 14);
		}

		try {
			result.parse("2018/01/31 05:5");
		}
		catch (DateTimeParseException e) {
			Assert.assertEquals(e.getMessage(), "Expecting digit, end of input");
			Assert.assertEquals(e.getErrorIndex(), 15);
		}

		try {
			result.parse("2018/12/24 09:");
		}
		catch (DateTimeParseException e) {
			Assert.assertEquals(e.getMessage(), "Expecting digit, end of input");
			Assert.assertEquals(e.getErrorIndex(), 14);
		}

		try {
			result.parse("2018/1/24 09:00");
		}
		catch (DateTimeParseException e) {
			Assert.assertEquals(e.getMessage(), "Insufficient digits in input (M)");
			Assert.assertEquals(e.getErrorIndex(), 6);
		}

		try {
			result.parse("2018/11/4 09:00");
		}
		catch (DateTimeParseException e) {
			Assert.assertEquals(e.getMessage(), "Expecting digit");
			Assert.assertEquals(e.getErrorIndex(), 9);
		}

		try {
			result.parse("2018/11/O4 09:00");
		}
		catch (DateTimeParseException e) {
			Assert.assertEquals(e.getMessage(), "Expecting digit");
			Assert.assertEquals(e.getErrorIndex(), 8);
		}

		try {
			result.parse("2018/00/24 05:59");
		}
		catch (DateTimeParseException e) {
			Assert.assertEquals(e.getMessage(), "0 value illegal for day/month");
			Assert.assertEquals(e.getErrorIndex(), 7);
		}

		try {
			result.parse("2018/13/24 05:59");
		}
		catch (DateTimeParseException e) {
			Assert.assertEquals(e.getMessage(), "Value too large for day/month");
			Assert.assertEquals(e.getErrorIndex(), 6);
		}

		try {
			result.parse("2018/01/00 05:59");
		}
		catch (DateTimeParseException e) {
			Assert.assertEquals(e.getMessage(), "0 value illegal for day/month");
			Assert.assertEquals(e.getErrorIndex(), 10);
		}

		try {
			result.parse("2018/01/32 05:59");
		}
		catch (DateTimeParseException e) {
			Assert.assertEquals(e.getMessage(), "Value too large for day/month");
			Assert.assertEquals(e.getErrorIndex(), 9);
		}
	}

	@Test
	public void intuitDateTime() {
		DateTimeParser dtp = new DateTimeParser(DateResolutionMode.None, Locale.getDefault());

		Assert.assertEquals(dtp.determineFormatString("  2/12/98 9:57    "), "?/??/yy H:mm");
		Assert.assertNull(dtp.determineFormatString("0¶¶¶¶¶"));
		Assert.assertNull(dtp.determineFormatString("2/12/98 :57"));
		Assert.assertNull(dtp.determineFormatString("2/12/98 9:5"));
		Assert.assertNull(dtp.determineFormatString("2/12/98 9:55:5"));
		Assert.assertEquals(dtp.determineFormatString("2/13/98 9:57"), "M/dd/yy H:mm");
		Assert.assertEquals(dtp.determineFormatString("13/12/98 12:57"), "dd/MM/yy HH:mm");
		Assert.assertEquals(dtp.determineFormatString("12/12/2012 8:57:02"), "??/??/yyyy H:mm:ss");
		Assert.assertEquals(dtp.determineFormatString("12/12/2012 8:57:02 GMT"), "??/??/yyyy H:mm:ss z");
		Assert.assertEquals(dtp.determineFormatString("13/12/2012 8:57:02"), "dd/MM/yyyy H:mm:ss");
		Assert.assertEquals(dtp.determineFormatString("2012/12/13 12:57:02"), "yyyy/MM/dd HH:mm:ss");

		DateTimeParserResult result;
		final DateTimeParser detUnspecified = new DateTimeParser();
		detUnspecified.train("12/12/2012 8:57:02 GMT");

		result = detUnspecified.getResult();
		Assert.assertEquals(result.getFormatString(), "??/??/yyyy H:mm:ss z");

		final DateTimeParser detDayFirst = new DateTimeParser(DateResolutionMode.DayFirst);
		detDayFirst.train("12/12/2012 8:57:02 GMT");

		result = detDayFirst.getResult();
		Assert.assertEquals(result.getFormatString(), "dd/MM/yyyy H:mm:ss z");

		final DateTimeParser detMonthFirst = new DateTimeParser(DateResolutionMode.MonthFirst);
		final String sample = "12/12/2012 8:57:02 GMT";
		detMonthFirst.train(sample);

		result = detMonthFirst.getResult();
		Assert.assertEquals(result.getFormatString(), "MM/dd/yyyy H:mm:ss z");

		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{2}/\\d{2}/\\d{4} \\d{1,2}:\\d{2}:\\d{2} .*");
		Assert.assertTrue(sample.matches(regExp));

		Assert.assertTrue(result.isValid("12/12/2012 8:57:02 GMT"));
		Assert.assertFalse(result.isValid("12/12/2012 8:57:02 GM"));
		Assert.assertFalse(result.isValid("12/12/2012 8:57:02 GMZ"));
		Assert.assertFalse(result.isValid("1O/12/2012 8:57:02 GMT"));
		Assert.assertFalse(result.isValid("10/1O/2012 8:57:02 GMT"));
		Assert.assertFalse(result.isValid("1/0/2012 8:57:02 GMT"));
		Assert.assertFalse(result.isValid("1/O/2012 8:57:02 GMT"));
		Assert.assertFalse(result.isValid("2/12/1998 :57"));
		Assert.assertFalse(result.isValid("2/12/1998 9:5"));
		Assert.assertFalse(result.isValid("2/12/1998 9:"));
		Assert.assertFalse(result.isValid("2/12/1998 9:55:5"));

		Assert.assertTrue(result.isValid8("12/12/2012 8:57:02 GMT"));
		Assert.assertFalse(result.isValid8("12/12/2012 8:57:02 GM"));
		Assert.assertFalse(result.isValid8("12/12/2012 8:57:02 GMZ"));
		Assert.assertFalse(result.isValid8("1O/12/2012 8:57:02 GMT"));
		Assert.assertFalse(result.isValid8("10/1O/2012 8:57:02 GMT"));
		Assert.assertFalse(result.isValid8("1/0/2012 8:57:02 GMT"));
		Assert.assertFalse(result.isValid8("1/O/2012 8:57:02 GMT"));
		Assert.assertFalse(result.isValid8("2/12/1998 :57"));
		Assert.assertFalse(result.isValid8("2/12/1998 9:5"));
		Assert.assertFalse(result.isValid8("2/12/1998 9:"));
		Assert.assertFalse(result.isValid8("2/12/1998 9:55:5"));
	}

	@Test
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
		Assert.assertEquals(result.getFormatString(), "yyyyMMdd'T'HHmmss'Z'");

		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{8}T\\d{6}Z");
		Assert.assertTrue(sample.trim().matches(regExp));

		Assert.assertTrue(result.isValid("20000610T020000Z"));
		Assert.assertFalse(result.isValid("20000610T020060Z"));
		Assert.assertFalse(result.isValid("20000610T026000Z"));
		Assert.assertFalse(result.isValid("20000610T250000Z"));

		Assert.assertTrue(result.isValid8("20000610T020000Z"));
		Assert.assertFalse(result.isValid8("20000610T020060Z"));
		Assert.assertFalse(result.isValid8("20000610T026000Z"));
		Assert.assertFalse(result.isValid8("20000610T250000Z"));
	}

	@Test
	public void intuitAlmostISONoSeps() {
		final DateTimeParser det = new DateTimeParser();
		final String sample = "20040101T123541";

		det.train(sample);
		det.train("20040101T020000");
		det.train("20060101T052321");
		det.train("20141121T151221");
		det.train("20080517T181221");
		det.train("20040101T002332");
		det.train("20060101T130012");
		det.train("20060101T000000");
		det.train("20040101T181632");
		det.train("20041008T221001");
		det.train("20040101T000000");
		det.train("20140101T221011");
		det.train("20041022T000000");
		det.train("19980905T130112");
		det.train("20080301T130632");
		det.train("20111007T000000");
		det.train(null);
		det.train("20000610T020000");
		det.train(null);
		det.train("20180211T192111");

		final DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyyMMdd'T'HHmmss");

		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{8}T\\d{6}");
		Assert.assertTrue(sample.trim().matches(regExp));

		Assert.assertTrue(result.isValid("20000610T020000"));
		Assert.assertFalse(result.isValid("20000610T020060"));
		Assert.assertFalse(result.isValid("20000610T026000"));
		Assert.assertFalse(result.isValid("20000610T250000"));

		Assert.assertTrue(result.isValid8("20000610T020000"));
		Assert.assertFalse(result.isValid8("20000610T020060"));
		Assert.assertFalse(result.isValid8("20000610T026000"));
		Assert.assertFalse(result.isValid8("20000610T250000"));
	}

	@Test
	public void intuitMMMM_d_yyyy() {
		final DateTimeParser det = new DateTimeParser();
		final String sample = "September-17-2014";

		det.train(sample);
		det.train("September-11-2011");
		det.train("September-4-2008");
		det.train("August-29-2005");
		det.train("August-23-2002");
		det.train("August-17-1999");
		det.train("August-10-1996");
		det.train("August-4-1993");
		det.train("July-29-1990");
		det.train("July-23-1987");
		det.train("July-16-1984");
		det.train("July-10-1981");
		det.train("July-4-1978");
		det.train("June-28-1975");
		det.train("June-21-1972");
		det.train("June-15-1969");
		det.train("June-9-1966");
		det.train("June-3-1963");
		det.train("May-27-1960");
		det.train("May-21-1957");
		det.train("May-15-1954");
		det.train("May-9-1951");
		det.train("May-2-1948");
		det.train("April-26-1945");
		det.train("April-20-1942");
		det.train("April-14-1939");
		det.train("April-7-1936");
		det.train("April-1-1933");
		det.train("March-26-1930");
		det.train("March-20-1927");

		final DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "MMMM-d-yyyy");

		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, KnownPatterns.PATTERN_ALPHA + "{3,9}-\\d{1,2}-\\d{4}");
		Assert.assertTrue(sample.trim().matches(regExp));

		Assert.assertTrue(result.isValid("April-1-1939"));
		Assert.assertFalse(result.isValid("April-32-1940"));
		Assert.assertFalse(result.isValid("Sep-12-1959"));
		Assert.assertFalse(result.isValid("May-12-69"));

		Assert.assertTrue(result.isValid8("April-1-1939"));
		Assert.assertFalse(result.isValid8("April-32-1940"));
		Assert.assertFalse(result.isValid8("Sep-12-1959"));
		Assert.assertFalse(result.isValid8("May-12-69"));
	}

	@Test
	public void intuitAlmostISO() {
		final DateTimeParser det = new DateTimeParser();
		final String sample = "2004-01-01 12:35:41-0500";

		det.train(sample);
		det.train("2004-01-01 02:00:00-0500");
		det.train("2006-01-01 05:23:21-0500");
		det.train("2014-11-21 15:12:21-0500");
		det.train("2008-05-17 18:12:21-0400");
		det.train("2004-01-01 00:23:32-0500");
		det.train("2006-01-01 13:00:12-0500");
		det.train("2006-01-01 00:00:00-0500");
		det.train("2004-01-01 18:16:32-0500");
		det.train("2004-10-08 22:10:01-0400");
		det.train("2004-01-01 00:00:00-0500");
		det.train("2014-01-01 22:10:11-0500");
		det.train("2004-10-22 00:00:00-0400");
		det.train("1998-09-05 13:01:12-0400");
		det.train("2008-03-01 13:06:32-0500");
		det.train("2011-10-07 00:00:00-0400");
		det.train(null);
		det.train("2000-06-10 02:00:00-0400");
		det.train(null);
		det.train("2018-02-11 19:21:11-0500");

		final DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyy-MM-dd HH:mm:ssxx");

		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}[-+][0-9]{4}");
		Assert.assertTrue(sample.trim().matches(regExp));

		Assert.assertTrue(result.isValid("2000-06-10 02:00:00-0400"));
		Assert.assertFalse(result.isValid("2000-06-10 02:00:60-0400"));
		Assert.assertFalse(result.isValid("2000-06-10 02:60:00-0400"));
		Assert.assertFalse(result.isValid("2000-06-10 25:00:00-0400"));

		Assert.assertTrue(result.isValid8("2000-06-10 02:00:00-0400"));
		Assert.assertFalse(result.isValid8("2000-06-10 02:00:60-0400"));
		Assert.assertFalse(result.isValid8("2000-06-10 02:60:00-0400"));
		Assert.assertFalse(result.isValid8("2000-06-10 25:00:00-0400"));
	}

	@Test
	public void intuitAlmostISO2() {
		final DateTimeParser det = new DateTimeParser();
		final String sample = "2004-01-01 12:35:41";

		det.train(sample);
		det.train("2004-01-01 02:00:00");
		det.train("2006-01-01 05:23:21");
		det.train("2014-11-21 15:12:21");
		det.train("2008-05-17 18:12:21");
		det.train("2004-01-01 00:23:32");
		det.train("2006-01-01 13:00:12");
		det.train("2006-01-01 00:00:00");
		det.train("2004-01-01 18:16:32");
		det.train("2004-10-08 22:10:01");
		det.train("2004-01-01 00:00:00");
		det.train("2014-01-01 22:10:11");
		det.train("2004-10-22 00:00:00");
		det.train("1998-09-05 13:01:12");
		det.train("2008-03-01 13:06:32");
		det.train("2011-10-07 00:00:00");
		det.train(null);
		det.train("2000-06-10 02:00:00");
		det.train(null);
		det.train("2018-02-11 19:21:11");

		final DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyy-MM-dd HH:mm:ss");

		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
		Assert.assertTrue(sample.trim().matches(regExp));

		Assert.assertTrue(result.isValid("2000-06-10 02:00:00"));
		Assert.assertFalse(result.isValid("2000-06-10 02:00:60"));
		Assert.assertFalse(result.isValid("2000-06-10 02:60:00"));
		Assert.assertFalse(result.isValid("2000-06-10 25:00:00"));

		Assert.assertTrue(result.isValid8("2000-06-10 02:00:00"));
		Assert.assertFalse(result.isValid8("2000-06-10 02:00:60"));
		Assert.assertFalse(result.isValid8("2000-06-10 02:60:00"));
		Assert.assertFalse(result.isValid8("2000-06-10 25:00:00"));
	}

	@Test
	public void intuitAlmostISO3() {
		final DateTimeParser det = new DateTimeParser(DateResolutionMode.MonthFirst);
		final String sample = "2004-01-01 12:35:41.0";
		Assert.assertEquals(det.determineFormatString(sample), "yyyy-MM-dd HH:mm:ss.S");

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

		final DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyy-MM-dd HH:mm:ss.S");

		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{1}");
		Assert.assertTrue(sample.trim().matches(regExp));

		Assert.assertTrue(result.isValid("2000-06-10 02:00:00.0"));
		Assert.assertFalse(result.isValid("2000-06-10 02:00:60.0"));
		Assert.assertFalse(result.isValid("2000-06-10 02:60:00.0"));
		Assert.assertFalse(result.isValid("2000-06-10 25:00:00.0"));

		Assert.assertTrue(result.isValid8("2000-06-10 02:00:00.0"));
		Assert.assertFalse(result.isValid8("2000-06-10 02:00:60.0"));
		Assert.assertFalse(result.isValid8("2000-06-10 02:60:00.0"));
		Assert.assertFalse(result.isValid8("2000-06-10 25:00:00.0"));
	}

	@Test
	public void testDTPResult_Date() {
		String[] tests = new String[] { "MM", "MMM", "MMMM", "dd", "yy", "yyyy", "xxx", "x", "EEE", "z", "a" };

		for (String test : tests) {
			DateTimeParserResult det = DateTimeParserResult.asResult(test, DateResolutionMode.Auto, Locale.getDefault());
			Assert.assertEquals(det.getType(), PatternInfo.Type.LOCALDATE);
		}
	}

	@Test
	public void testDTPResult_Time() {
		String[] tests = new String[] { "HH", "mm", "ss", "SSS" };

		for (String test : tests) {
			DateTimeParserResult det = DateTimeParserResult.asResult(test, DateResolutionMode.Auto, Locale.getDefault());
			Assert.assertEquals(det.getType(), PatternInfo.Type.LOCALTIME);
		}
	}

	@Test
	public void testDTPResult_DateTime() {
		String[] tests = new String[] { "MM/dd/yyyy HH:mm:ss" };

		for (String test : tests) {
			DateTimeParserResult det = DateTimeParserResult.asResult(test, DateResolutionMode.Auto, Locale.getDefault());
			Assert.assertEquals(det.getType(), PatternInfo.Type.LOCALDATETIME);
		}
	}

	@Test
	public void testDTPResult_OffsetDateTime() {
		String[] tests = new String[] { "yyyyMMdd'T'HHmmssxx" };

		for (String test : tests) {
			DateTimeParserResult det = DateTimeParserResult.asResult(test, DateResolutionMode.Auto, Locale.getDefault());
			Assert.assertEquals(det.getType(), PatternInfo.Type.OFFSETDATETIME);
		}
	}

	@Test
	public void testDTPResult_ZonedDateTime() {
		String[] tests = new String[] { "EEE MMM dd HH:mm:ss z yyyy" };

		for (String test : tests) {
			DateTimeParserResult det = DateTimeParserResult.asResult(test, DateResolutionMode.Auto, Locale.getDefault());
			Assert.assertEquals(det.getType(), PatternInfo.Type.ZONEDDATETIME);
		}
	}

	@Test
	public void testDTPResult_Unknown() {
		String[] tests = new String[] { "W", "V", "G", "u", "L", "Q", "e", "c", "K", "k", "n", "N", "O" };

		for (String test : tests) {
			DateTimeParserResult det = DateTimeParserResult.asResult(test, DateResolutionMode.Auto, Locale.getDefault());
			Assert.assertEquals(det.getType(), PatternInfo.Type.LOCALDATE);
		}
	}

	@Test
	public void intuitAlmostISO4() {
		final DateTimeParser det = new DateTimeParser(DateResolutionMode.MonthFirst);
		final String sample = "2004-01-01 12:35:41.999";
		Assert.assertEquals(det.determineFormatString(sample), "yyyy-MM-dd HH:mm:ss.SSS");

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

		final DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyy-MM-dd HH:mm:ss.SSS");

		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}");
		Assert.assertTrue(sample.trim().matches(regExp));

		Assert.assertTrue(result.isValid("2000-06-10 02:00:00.000"));
		Assert.assertFalse(result.isValid("2000-06-10 02:00:60.990"));
		Assert.assertFalse(result.isValid("2000-06-10 02:60:00.009"));
		Assert.assertFalse(result.isValid("2000-06-10 02:60:00.00"));
		Assert.assertFalse(result.isValid("2000-06-10 25:00:00.008"));

		Assert.assertTrue(result.isValid8("2000-06-10 02:00:00.000"));
		Assert.assertFalse(result.isValid8("2000-06-10 02:00:60.990"));
		Assert.assertFalse(result.isValid8("2000-06-10 02:60:00.009"));
		Assert.assertFalse(result.isValid8("2000-06-10 02:60:00.00"));
		Assert.assertFalse(result.isValid8("2000-06-10 25:00:00.008"));
	}

	@Test
	public void intuitTimeDate() {
		DateTimeParser dtp = new DateTimeParser(DateResolutionMode.None, Locale.getDefault());

		Assert.assertEquals(dtp.determineFormatString("9:57 2/13/98"), "H:mm M/dd/yy");
		Assert.assertEquals(dtp.determineFormatString("9:57 2/12/98"), "H:mm ?/??/yy");
		Assert.assertEquals(dtp.determineFormatString("12:57 13/12/98"), "HH:mm dd/MM/yy");
		Assert.assertEquals(dtp.determineFormatString("8:57:02 12/12/2012"), "H:mm:ss ??/??/yyyy");
		Assert.assertEquals(dtp.determineFormatString("12:57:02 2012/12/18"), "HH:mm:ss yyyy/MM/dd");
	}

	@Test
	public void parseddMMMyyyy() {
		DateTimeParser dtp = new DateTimeParser(DateResolutionMode.None, Locale.getDefault());

		Assert.assertEquals(dtp.determineFormatString("12-May-14"), "dd-MMM-yy");
		Assert.assertEquals(dtp.determineFormatString("2-Jan-2017"), "d-MMM-yyyy");
		Assert.assertEquals(dtp.determineFormatString("21 Jan 2017"), "dd MMM yyyy");
		Assert.assertEquals(dtp.determineFormatString("8 Dec 1993"), "d MMM yyyy");
		Assert.assertEquals(dtp.determineFormatString("25-Dec-2017"), "dd-MMM-yyyy");
		Assert.assertNull(dtp.determineFormatString("21-Jam-2017"));

		final DateTimeParser det = new DateTimeParser();
		final String sample = "2 Jan 2017";
		det.train(sample);

		final DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "d MMM yyyy");
		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{1,2} " + KnownPatterns.PATTERN_ALPHA + "{3} \\d{4}");
		Assert.assertTrue(sample.matches(regExp));

		Assert.assertTrue(result.isValid("20 Jun 2017"));
		Assert.assertTrue(result.isValid("1 Jun 2017"));
		Assert.assertFalse(result.isValid("20 0c"));
		Assert.assertFalse(result.isValid(""));
		Assert.assertFalse(result.isValid("1"));
		Assert.assertFalse(result.isValid("20 0ct 2018"));
		Assert.assertFalse(result.isValid("32 Oct 2018"));
		Assert.assertFalse(result.isValid("32 Och 2018"));
		Assert.assertFalse(result.isValid("31 Oct 201"));

		Assert.assertTrue(result.isValid8("20 Jun 2017"));
		Assert.assertTrue(result.isValid8("1 Jun 2017"));
		Assert.assertFalse(result.isValid8("20 0c"));
		Assert.assertFalse(result.isValid8(""));
		Assert.assertFalse(result.isValid8("1"));
		Assert.assertFalse(result.isValid8("20 0ct 2018"));
		Assert.assertFalse(result.isValid8("32 Oct 2018"));
		Assert.assertFalse(result.isValid8("32 Och 2018"));
		Assert.assertFalse(result.isValid8("31 Oct 201"));
	}

	@Test
	public void intuitTimeDateWithTimeZone() {
		DateTimeParser dtp = new DateTimeParser(null, Locale.getDefault());

		Assert.assertEquals(dtp.determineFormatString("01/30/2012 10:59:48 GMT"), "MM/dd/yyyy HH:mm:ss z");
	}

	@Test
	public void intuitDateTrainSlash() {
		final DateTimeParser det = new DateTimeParser();
		final String sample = "12/12/12";
		det.train("12/12/12");
		det.train("12/12/32");
		det.train("02/22/02");
		for (int i = 0; i < 20; i++)
			det.train("02/02/99");
		det.train("02/O2/99");

		final DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "MM/dd/yy");
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{2}/\\d{2}/\\d{2}");
		Assert.assertTrue(sample.matches(regExp));
	}

	@Test
	public void intuitDateTrainYYYYSlash() {
		final DateTimeParser det = new DateTimeParser(DateResolutionMode.MonthFirst);
		final String sample = "2012/12/12";
		det.train(sample);
		det.train("2012/11/11");
		det.train("2012/10/32");

		final DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyy/MM/dd");
		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{4}/\\d{2}/\\d{2}");
		Assert.assertTrue(sample.matches(regExp));

		Assert.assertTrue(result.isValid("2012/12/12"));
		Assert.assertFalse(result.isValid("2012/10/32"));
		Assert.assertFalse(result.isValid("20121/10/32"));
		Assert.assertFalse(result.isValid("201/10/32"));

		Assert.assertTrue(result.isValid8("2012/12/12"));
		Assert.assertFalse(result.isValid8("2012/10/32"));
		Assert.assertFalse(result.isValid8("20121/10/32"));
		Assert.assertFalse(result.isValid8("201/10/32"));
	}

	@Test
	public void yyyyMd() {
		final DateTimeParser det = new DateTimeParser();
		final String sample = "8547 8 6";
		det.train(sample);

		final DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyy M d");
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{4} \\d{1,2} \\d{1,2}");
		Assert.assertTrue(sample.matches(regExp));
	}

	@Test
	public void timeFirst() {
		final DateTimeParser det = new DateTimeParser();
		final String sample = "7:05 5/4/38";
		det.train(sample);

		final DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "H:mm ?/?/yy");
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATETIME);
		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{1,2}:\\d{2} \\d{1,2}/\\d{1,2}/\\d{2}");
		Assert.assertTrue(sample.matches(regExp));
	}

	@Test
	public void bogusInput() {
		final String inputs = "12:45:64.|21/12/99:|21/12/99:|18:46:|4:38  39|3124/08/|890/65 1/|7/87/33| 89:50|18:52 56:|18/94/06|0463 5 71|50 9:22|" +
				"95/06/88|0-27-98|08/56 22/|31-0-99|0/7:6/11 //61|8:73/4/13 15|14/23/3367| 00/21/79|22-23-00|0/20/2361|0/2/52 9:50 4 |" +
				"1:57:11  1/4/98|2015-8-17T|4/01/41 3:43 T450|37/8/005 5:05|0/6/95|0000 7 1|2000-12-12T12:45-72|2000-12-12T12:45-112|" +
				"12:45:64.|84:12:45.5712:45| 12:45:63.3 |";
		final String[] input = inputs.split("\\|");

		for (final String testCase : input) {
			final DateTimeParser det = new DateTimeParser();
			det.train(testCase);
			final DateTimeParserResult result = det.getResult();
			Assert.assertNull(result, testCase);
		}
	}

	@Test
	public void intuitHHMMTrain() {
		final DateTimeParser det = new DateTimeParser();
		final String sampleOne = "12:57";
		final String sampleThree = "8:03";
		det.train(sampleOne);
		det.train("13:45");
		det.train(sampleThree);

		final DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "H:mm");
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALTIME);
		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{1,2}:\\d{2}");
		Assert.assertTrue(sampleOne.matches(regExp));
		Assert.assertTrue(sampleThree.matches(regExp));
	}

	private void add(final Map<String, Integer> counter, final String key) {
		final Integer seen = counter.get(key);
		if (seen == null)
			counter.put(key, 1);
		else
			counter.put(key, seen + 1);
	}

	private void dump(final Map<String, Integer> counter) {
		final Map<String, Integer> byValue = DateTimeParser.sortByValue(counter);
		for (final Map.Entry<String, Integer> entry : byValue.entrySet()) {
			System.err.printf("'%s' : %d\n", entry.getKey(), entry.getValue());
		}
	}

	@Test
	public void intuit30523() {
		final DateTimeParser det = new DateTimeParser();
		final String input = "9:12:45 30/5/23";
		det.train(input);

		final DateTimeParserResult result = det.getResult();
		final String formatString = result.getFormatString();
		Assert.assertEquals(formatString, "H:mm:ss ??/M/??");

		final String regExp = result.getRegExp();
		Assert.assertTrue(input.matches(regExp), "input: '" + input + "', RE: '" + regExp + "'");
	}

	//@Test
	public void fuzz() {
		final Random randomGenerator = new Random(12);
		final Map<String, Integer> formatStrings = new HashMap<String, Integer>();
		final Map<String, Integer> types = new HashMap<String, Integer>();
		int good = 0;
		final int iterations = 100000000;
		final String[] timeZones = TimeZone.getAvailableIDs();
		final PrintStream logger = System.err;

		for (int iters = 0; iters < iterations; iters++) {
			final int len = 5 + randomGenerator.nextInt(15);
			final StringBuilder s = new StringBuilder(len);
			int digits = 0;
		    for (int i = 0; s.length() <= len; ++i) {
		    	final int randomInt = randomGenerator.nextInt(100);
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
		    	  final int idx = randomGenerator.nextInt(timeZones.length - 1);
		    	  s.append(timeZones[idx]);
		    	  continue;
		      }
		      s.append(",.;':\"[]{}\\|=!@#$%^&*<>".charAt(randomGenerator.nextInt(100) % 23));
		    }

		    final int amPmSwitch = randomGenerator.nextInt(100);
		    if (amPmSwitch == 98)
		    	s.append("am");
		    else if (amPmSwitch == 99)
		    	s.append("pm");

		    final DateTimeParser det = new DateTimeParser();
		    final String input = s.toString();
			//System.err.printf("Input ... '%s'\n", input);
		    final String trimmed = input.trim();
			try {
				det.train(input);

				final DateTimeParserResult result = det.getResult();
				if (result != null) {
					good++;
					final String formatString = result.getFormatString();

					final String regExp = result.getRegExp();
					Assert.assertTrue(trimmed.matches(regExp), "input: '" + trimmed + "', RE: '" + regExp + "'");

					final PatternInfo.Type type = result.getType();
					add(formatStrings, formatString);
					add(types, type.toString());
					result.parse(trimmed);
					if (formatString.indexOf('?') != -1)
						continue;

					try {
						final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatString);
						if (PatternInfo.Type.LOCALTIME.equals(type))
							LocalTime.parse(trimmed, formatter);
						else if (PatternInfo.Type.LOCALDATE.equals(type))
							LocalDate.parse(trimmed, formatter);
						else if (PatternInfo.Type.LOCALDATETIME.equals(type))
							LocalDateTime.parse(trimmed, formatter);
						else if (PatternInfo.Type.ZONEDDATETIME.equals(type))
							ZonedDateTime.parse(trimmed, formatter);
						else
							OffsetDateTime.parse(trimmed, formatter);
					}
					catch (DateTimeParseException exc) {
						logger.printf("Java: Struggled with input of the form: '%s'\n", input);
					}

				}
			}
			catch (Exception e) {
				logger.printf("Struggled with input of the form: '%s'\n", input);
			}
		}

		dump(formatStrings);
		dump(types);

		logger.printf("Good %d out of %d (%%%f)\n", good, iterations, 100*((float)good/iterations));
	}

	@Test
	public void intuitMMDDYYYYHHMMSSTrain() {
		final DateTimeParser det = new DateTimeParser();
		final String sample = "01/26/2012 10:42:23 GMT";
		det.train(sample);
		det.train("01/30/2012 10:59:48 GMT");
		det.train("01/25/2012 16:46:43 GMT");
		det.train("01/25/2012 16:28:42 GMT");
		det.train("01/24/2012 16:53:04 GMT");

		final DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "MM/dd/yyyy HH:mm:ss z");
		Assert.assertEquals(result.getType(), PatternInfo.Type.ZONEDDATETIME);

		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2} .*");
		Assert.assertTrue(sample.matches(regExp));

		Assert.assertTrue(result.isValid("01/26/2012 10:42:23 GMT"));
		Assert.assertTrue(result.isValid8("01/26/2012 10:42:23 GMT"));
	}

	@Test
	public void testPerf() {
		final DateTimeParser det = new DateTimeParser();
		final String sample = "01/26/2012 10:42:23 GMT";
		final PrintStream logger = System.err;
		det.train(sample);
		det.train("01/30/2012 10:59:48 GMT");
		det.train("01/25/2012 16:46:43 GMT");
		det.train("01/25/2012 16:28:42 GMT");
		det.train("01/24/2012 16:53:04 GMT");

		final DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "MM/dd/yyyy HH:mm:ss z");
		Assert.assertEquals(result.getType(), PatternInfo.Type.ZONEDDATETIME);

		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2} .*");
		Assert.assertTrue(sample.matches(regExp));

		final int iterations = 10000000;
		final long start = System.currentTimeMillis();
		for (int i = 0; i < iterations; i++) {
			Assert.assertTrue(result.isValid("01/26/2012 10:42:23 GMT"));
		}
		final long doneCustom = System.currentTimeMillis();
		for (int i = 0; i < iterations; i++) {
			Assert.assertTrue(result.isValid8("01/26/2012 10:42:23 GMT"));
		}
		final long done = System.currentTimeMillis();
		logger.printf("Custom = %dms, Java = %dms\n", doneCustom - start, done - doneCustom);
	}

	@Test
	public void intuitInsufficientFactsTrain() {
		final DateTimeParser detPrime = new DateTimeParser();
		String sample = "12/30/99";

		detPrime.train(sample);

		final DateTimeParserResult resultPrime = detPrime.getResult();
		Assert.assertEquals(resultPrime.getFormatString(), "MM/dd/yy");

		String regExp = resultPrime.getRegExp();
		Assert.assertEquals(regExp, "\\d{2}/\\d{2}/\\d{2}");
		Assert.assertTrue(sample.matches(regExp));

		final DateTimeParser det = new DateTimeParser();
		sample = " 04/03/13";

		det.train(sample);
		det.train(" 05/03/13");
		det.train(" 06/03/13");
		det.train(" 07/03/13");
		det.train(" 08/03/13");
		det.train(" 09/03/13");
		det.train(" 10/03/13");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "??/??/??");

		regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{2}/\\d{2}/\\d{2}");
		Assert.assertTrue(sample.trim().matches(regExp));

		// Force to be day first
		final DateTimeParser detDayFirst = new DateTimeParser(DateResolutionMode.DayFirst);
		sample = " 04/03/13";

		detDayFirst.train(sample);
		detDayFirst.train(" 05/03/13");
		detDayFirst.train(" 06/03/13");
		detDayFirst.train(" 07/03/13");
		detDayFirst.train(" 08/03/13");
		detDayFirst.train(" 09/03/13");
		detDayFirst.train(" 10/03/13");

		result = detDayFirst.getResult();
		Assert.assertEquals(result.getFormatString(), "dd/MM/yy");

		regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{2}/\\d{2}/\\d{2}");
		Assert.assertTrue(sample.trim().matches(regExp));

		Assert.assertTrue(result.isValid("31/12/12"));
		Assert.assertFalse(result.isValid("12/31/12"));
		Assert.assertFalse(result.isValid("2012/12/12"));
		Assert.assertTrue(result.isValid8("31/12/12"));
		Assert.assertFalse(result.isValid8("12/31/12"));
		Assert.assertFalse(result.isValid8("2012/12/12"));

		// Force to be month first
		final DateTimeParser detMonthFirst = new DateTimeParser(DateResolutionMode.MonthFirst);
		sample = " 04/03/13";

		detMonthFirst.train(sample);
		detMonthFirst.train(" 05/03/13");
		detMonthFirst.train(" 06/03/13");
		detMonthFirst.train(" 07/03/13");
		detMonthFirst.train(" 08/03/13");
		detMonthFirst.train(" 09/03/13");
		detMonthFirst.train(" 10/03/13");

		result = detMonthFirst.getResult();
		Assert.assertEquals(result.getFormatString(), "MM/dd/yy");

		regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{2}/\\d{2}/\\d{2}");
		Assert.assertTrue(sample.trim().matches(regExp));

		Assert.assertFalse(result.isValid("31/12/12"));
		Assert.assertTrue(result.isValid("12/31/12"));
		Assert.assertFalse(result.isValid("2012/12/12"));
		Assert.assertFalse(result.isValid8("31/12/12"));
		Assert.assertTrue(result.isValid8("12/31/12"));
		Assert.assertFalse(result.isValid8("2012/12/12"));
	}

	@Test
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

		final DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "MM/dd/yy");

		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{2}/\\d{2}/\\d{2}");
		Assert.assertTrue(sample.trim().matches(regExp));

		Assert.assertTrue(result.isValid("12/12/12"));
		Assert.assertFalse(result.isValid("1/1/1"));
		Assert.assertFalse(result.isValid("123/1/1"));
		Assert.assertFalse(result.isValid("1/123/1"));
		Assert.assertFalse(result.isValid("1/1/123"));

		Assert.assertTrue(result.isValid8("12/12/12"));
		Assert.assertFalse(result.isValid8("1/1/1"));
		Assert.assertFalse(result.isValid8("123/1/1"));
		Assert.assertFalse(result.isValid8("1/123/1"));
		Assert.assertFalse(result.isValid8("1/1/123"));
	}

	@Test
	public void intuitDateddMMyyyyHHmmss() {
		final DateTimeParser det = new DateTimeParser(DateResolutionMode.MonthFirst);
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

		final DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "d/M/yyyy HH:mm:ss");

		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{1,2}/\\d{1,2}/\\d{4} \\d{2}:\\d{2}:\\d{2}");
		Assert.assertTrue(sample.trim().matches(regExp));
	}

	@Test
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

		final DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yy/MM/dd");

		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{2}/\\d{2}/\\d{2}");
		Assert.assertTrue(sample.trim().matches(regExp));

		Assert.assertTrue(result.isValid("12/12/12"));
		Assert.assertFalse(result.isValid("12/13/12"));
		Assert.assertFalse(result.isValid("1/1/1"));
		Assert.assertFalse(result.isValid("123/1/1"));
		Assert.assertFalse(result.isValid("1/123/1"));
		Assert.assertFalse(result.isValid("1/1/123"));

		Assert.assertTrue(result.isValid8("12/12/12"));
		Assert.assertFalse(result.isValid8("12/13/12"));
		Assert.assertFalse(result.isValid8("1/1/1"));
		Assert.assertFalse(result.isValid8("123/1/1"));
		Assert.assertFalse(result.isValid8("1/123/1"));
		Assert.assertFalse(result.isValid8("1/1/123"));
	}

	@Test
	public void intuitDatedMMMyy() {
		final DateTimeParser det = new DateTimeParser();
		final String sample = "1-Jan-14";
		det.train(sample);
		det.train("10-Jan-14");

		final DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "d-MMM-yy");

		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{1,2}-" + KnownPatterns.PATTERN_ALPHA + "{3}-\\d{2}");
		Assert.assertTrue(sample.trim().matches(regExp));

		Assert.assertTrue(result.isValid("1-Jan-14"));
		Assert.assertTrue(result.isValid("10-Jan-14"));

		Assert.assertTrue(result.isValid8("1-Jan-14"));
		Assert.assertTrue(result.isValid8("10-Jan-14"));
	}

	@Test
	public void javaSimple() {
		final DateTimeParser det = new DateTimeParser();
		long millis = System.currentTimeMillis();
		Date d = new Date();
		Set<String> samples = new HashSet<String>();

		for (int i = 0; i < 100; i++) {
			d = new Date(millis);
			samples.add(d.toString());
			det.train(d.toString());
			millis -= 186400000;
		}

		final DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "EEE MMM dd HH:mm:ss z yyyy");

		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, KnownPatterns.PATTERN_ALPHA + "{3} " + KnownPatterns.PATTERN_ALPHA + "{3} \\d{2} \\d{2}:\\d{2}:\\d{2} .* \\d{4}");
		Assert.assertTrue(d.toString().matches(regExp));
		Assert.assertTrue(result.isValid8(d.toString()));
		Assert.assertTrue(result.isValid(d.toString()));
	}

	@Test
	public void intuitHHMMSSTrain() {
		final DateTimeParser det = new DateTimeParser();
		final String sample = "12:57:03";
		det.train(sample);
		det.train("13:45:00");
		det.train("8:03:59");

		final DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "H:mm:ss");

		final String regExp = result.getRegExp();
		Assert.assertEquals(regExp, "\\d{1,2}:\\d{2}:\\d{2}");
		Assert.assertTrue(sample.trim().matches(regExp));

		Assert.assertTrue(result.isValid("12:57:03"));
		Assert.assertTrue(result.isValid("8:03:59"));
		Assert.assertFalse(result.isValid("8:03:599"));
		Assert.assertFalse(result.isValid("118:03:59"));
		Assert.assertFalse(result.isValid("118:3:59"));
		Assert.assertFalse(result.isValid("118:333:59"));

		Assert.assertTrue(result.isValid8("12:57:03"));
		Assert.assertTrue(result.isValid8("8:03:59"));
		Assert.assertFalse(result.isValid8("8:03:599"));
		Assert.assertFalse(result.isValid8("118:03:59"));
		Assert.assertFalse(result.isValid8("118:3:59"));
		Assert.assertFalse(result.isValid8("118:333:59"));
	}
}
