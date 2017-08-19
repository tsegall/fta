package com.cobber.fta;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

import org.testng.Assert;
import org.testng.annotations.Test;

public class DetermineDateTimeFormatTests {
	@Test
	public void intuitTimeOnly() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("9:57", null), "H:mm");
		Assert.assertEquals(DateTimeParser.determineFormatString("12:57", null), "HH:mm");
		Assert.assertEquals(DateTimeParser.determineFormatString("8:57:02", null), "H:mm:ss");
		Assert.assertEquals(DateTimeParser.determineFormatString("12:57:02", null), "HH:mm:ss");
		Assert.assertNull(DateTimeParser.determineFormatString(":57:02", null));
		Assert.assertNull(DateTimeParser.determineFormatString("123:02", null));
		Assert.assertNull(DateTimeParser.determineFormatString("12:023", null));
		Assert.assertNull(DateTimeParser.determineFormatString("12:023:12", null));
		Assert.assertNull(DateTimeParser.determineFormatString("12:0", null));
		Assert.assertNull(DateTimeParser.determineFormatString("12:02:1", null));
		Assert.assertNull(DateTimeParser.determineFormatString("12:02:12:14", null));
		Assert.assertNull(DateTimeParser.determineFormatString("12:02:124", null));
		Assert.assertNull(DateTimeParser.determineFormatString("12:02:", null));
		Assert.assertNull(DateTimeParser.determineFormatString("12::02", null));
	}

	@Test
	public void intuitDateOnlySlash() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("2/12/98", null), "?/??/yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("2/2/02", null), "?/?/yy");
		Assert.assertNull(DateTimeParser.determineFormatString("2/31/02", null));
		Assert.assertEquals(DateTimeParser.determineFormatString("31/02/02", null), "??/??/??");
		Assert.assertEquals(DateTimeParser.determineFormatString("12/12/98", null), "??/??/yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("14/12/98", null), "dd/MM/yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12/14/98", null), "MM/dd/yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12/12/2012", null), "??/??/yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("20/12/2012", null), "dd/MM/yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("11/15/2012", null), "MM/dd/yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("2012/12/12", null), "yyyy/MM/dd");
		Assert.assertNull(DateTimeParser.determineFormatString("/57/02", null));
		Assert.assertNull(DateTimeParser.determineFormatString("123/02", null));
		Assert.assertNull(DateTimeParser.determineFormatString("12/023", null));
		Assert.assertNull(DateTimeParser.determineFormatString("12/0", null));
		Assert.assertNull(DateTimeParser.determineFormatString("12/02/1", null));
		Assert.assertNull(DateTimeParser.determineFormatString("12/023/12", null));
		Assert.assertNull(DateTimeParser.determineFormatString("12/02/", null));
		Assert.assertNull(DateTimeParser.determineFormatString("12/02-99", null));
	}

	/*
	@Test
	public void testSpaces() throws Exception {
		Assert.assertEquals(DateTimeParser.parse("2018 12 24"), "yyyy MM dd");
	}
	*/

	@Test
	public void intuitDateOnlyDash() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("2-12-98", null), "?-??-yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12-12-98", null), "??-??-yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("14-12-98", null), "dd-MM-yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12-14-98", null), "MM-dd-yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12-12-2012", null), "??-??-yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("2012-12-12", null), "yyyy-MM-dd");
		Assert.assertNull(DateTimeParser.determineFormatString("20120-12-12", null));
	}

	@Test
	public void intuit8601DD() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("2004-01-01T00:00:00+05", null), "yyyy-MM-dd'T'HH:mm:ssx");

		DateTimeParser det = new DateTimeParser();
		String sample = "2004-01-01T00:00:00+05";
		det.train(sample);

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyy-MM-dd'T'HH:mm:ssx");

		String re = result.getRegExp();
		Assert.assertEquals(re, "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[-+][0-9]{2}([0-9]{2})?");
		Assert.assertTrue(sample.matches(re));

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
	public void intuit8601DD_DD() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("2004-01-01T00:00:00+05:00", null), "yyyy-MM-dd'T'HH:mm:ssxxx");

		DateTimeParser det = new DateTimeParser();
		String sample = "2004-01-01T00:00:00+05:00";
		det.train(sample);

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyy-MM-dd'T'HH:mm:ssxxx");

		String re = result.getRegExp();
		Assert.assertEquals(re, "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[-+][0-9]{2}:[0-9]{2}");
		Assert.assertTrue(sample.matches(re));

		Assert.assertTrue(result.isValid("2004-01-01T00:00:00+05:00"));
		Assert.assertTrue(result.isValid("2012-03-04T19:22:10+08:00"));
		Assert.assertFalse(result.isValid("2012-03-04T19:22:10+08:0"));
		Assert.assertFalse(result.isValid("2012-03-04T19:22:10+?08:00"));
		Assert.assertFalse(result.isValid("2012-03-04T19:22:10+19:00"));

		Assert.assertTrue(result.isValid8("2004-01-01T00:00:00+05:00"));
		Assert.assertTrue(result.isValid8("2012-03-04T19:22:10+08:00"));
		Assert.assertFalse(result.isValid8("2012-03-04T19:22:10+08:0"));
		Assert.assertFalse(result.isValid8("2012-03-04T19:22:10+?08:00"));
		Assert.assertFalse(result.isValid8("2012-03-04T19:22:10+19:00"));
	}

	@Test
	public void intuit8601DD_DD_DD() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("2004-01-01T00:00:00+05:00:00", null), "yyyy-MM-dd'T'HH:mm:ssxxxxx");

		DateTimeParser det = new DateTimeParser();
		String sample = "2004-01-01T00:00:00+05:00:00";
		det.train(sample);

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyy-MM-dd'T'HH:mm:ssxxxxx");

		String re = result.getRegExp();
		Assert.assertEquals(re, "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[-+][0-9]{2}:[0-9]{2}(:[0-9]{2})?");
		Assert.assertTrue(sample.matches(re));

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
	public void fullMonths() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("25 July 2018", null), "dd MMMM yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("25-July-2018", null), "dd-MMMM-yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("7 June 2017", null), "d MMMM yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("7-June-2017", null), "d-MMMM-yyyy");

		Assert.assertEquals(DateTimeParser.determineFormatString("June 23, 2017", null), "MMMM dd',' yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("August 8, 2017", null), "MMMM d',' yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("August 18 2017", null), "MMMM dd yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("December 9 2017", null), "MMMM d yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("January-14-2017", null), "MMMM-dd-yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("February-4-2017", null), "MMMM-d-yyyy");
	}

	@Test
	public void basic_DTP_DD_MMMM_YYYY() throws Exception {
		String input = "25 July 2018|12 August 1984|10 January 2000|1 January 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|12 August 1984|10 January 2000|1 January 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|12 August 1984|10 May 2000|1 April 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|13 August 1984|10 January 2000|1 May 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|12 November 1984|10 October 2000|1 January 1970|16 June 1934|06 July 1961|";
		String inputs[] = input.split("\\|");
		DateTimeParser det = new DateTimeParser();

		for (int i = 0; i < inputs.length; i++) {
			det.train(inputs[i]);
		}

		DateTimeParserResult result = det.getResult();

		String formatString = result.getFormatString();

		Assert.assertEquals(formatString, "d MMMM yyyy");

		String re = result.getRegExp();

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(re));
		}
	}

	@Test
	public void basic_DD_MMMM_YYYY() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String input = "25 July 2018|12 August 1984|10 January 2000|1 January 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|12 August 1984|10 January 2000|1 January 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|12 August 1984|10 May 2000|1 April 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|13 August 1984|10 January 2000|1 May 1970|16 July 1934|06 July 1961|" +
				"25 July 2018|12 November 1984|10 October 2000|1 January 1970|16 June 1934|06 July 1961|";
		String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.DATE);
		Assert.assertEquals(result.getTypeQualifier(), "d MMMM yyyy");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{1,2} \\p{Alpha}{3,9} \\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getPattern()));
		}
	}

	@Test
	public void intuitMMM_DD_YYYY() throws Exception {
		String trimmed = "May 1, 2018";
		DateTimeParser det = new DateTimeParser();
		det.train(trimmed);

		DateTimeParserResult result = det.getResult();

		String formatString = result.getFormatString();
		PatternInfo.Type type = result.getType();

		Assert.assertEquals(formatString, "MMM d',' yyyy");

		String re = result.getRegExp();
		Assert.assertEquals(re, "\\p{Alpha}{3} \\d{1,2}, \\d{4}");
		Assert.assertTrue(trimmed.matches(re));

		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatString);
			if (PatternInfo.Type.TIME.equals(type))
				LocalTime.parse(trimmed, formatter);
			else if (PatternInfo.Type.DATE.equals(type))
				LocalDate.parse(trimmed, formatter);
			else if (PatternInfo.Type.DATETIME.equals(type))
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
	public void testAsResult() throws Exception {
		Assert.assertNull(DateTimeParserResult.asResult("yyyy-MM-ddTHH:m:ssx", null));
		Assert.assertNull(DateTimeParserResult.asResult("yyyy-MM-ddTHH:mm:sx", null));
	}

	@Test
	public void testParse() throws Exception {
		DateTimeParserResult result = DateTimeParserResult.asResult("yyyy/MM/dd HH:mm", null);

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
	public void intuitDateTime() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("  2/12/98 9:57    ", null), "?/??/yy H:mm");
		Assert.assertNull(DateTimeParser.determineFormatString("0þþþþþ", null));
		Assert.assertNull(DateTimeParser.determineFormatString("2/12/98 :57", null));
		Assert.assertNull(DateTimeParser.determineFormatString("2/12/98 9:5", null));
		Assert.assertNull(DateTimeParser.determineFormatString("2/12/98 9:55:5", null));
		Assert.assertEquals(DateTimeParser.determineFormatString("2/13/98 9:57", null), "M/dd/yy H:mm");
		Assert.assertEquals(DateTimeParser.determineFormatString("13/12/98 12:57", null), "dd/MM/yy HH:mm");
		Assert.assertEquals(DateTimeParser.determineFormatString("12/12/2012 8:57:02", null), "??/??/yyyy H:mm:ss");
		Assert.assertEquals(DateTimeParser.determineFormatString("12/12/2012 8:57:02 GMT", null),
				"??/??/yyyy H:mm:ss z");
		Assert.assertEquals(DateTimeParser.determineFormatString("13/12/2012 8:57:02", null), "dd/MM/yyyy H:mm:ss");
		Assert.assertEquals(DateTimeParser.determineFormatString("2012/12/12 12:57:02", null), "yyyy/MM/dd HH:mm:ss");

		DateTimeParserResult result = null;
		DateTimeParser detUnspecified = new DateTimeParser();
		detUnspecified.train("12/12/2012 8:57:02 GMT");

		result = detUnspecified.getResult();
		Assert.assertEquals(result.getFormatString(), "??/??/yyyy H:mm:ss z");

		DateTimeParser detDayFirst = new DateTimeParser(true);
		detDayFirst.train("12/12/2012 8:57:02 GMT");

		result = detDayFirst.getResult();
		Assert.assertEquals(result.getFormatString(), "dd/MM/yyyy H:mm:ss z");

		DateTimeParser detMonthFirst = new DateTimeParser(false);
		String sample = "12/12/2012 8:57:02 GMT";
		detMonthFirst.train(sample);

		result = detMonthFirst.getResult();
		Assert.assertEquals(result.getFormatString(), "MM/dd/yyyy H:mm:ss z");

		String re = result.getRegExp();
		Assert.assertEquals(re, "\\d{2}/\\d{2}/\\d{4} \\d{1,2}:\\d{2}:\\d{2} .*");
		Assert.assertTrue(sample.matches(re));

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
	public void intuitISONoSeps() throws Exception {
		DateTimeParser det = new DateTimeParser();
		String sample = "20040101T123541Z";

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

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyyMMdd'T'HHmmss'Z'");

		String re = result.getRegExp();
		Assert.assertEquals(re, "\\d{8}T\\d{6}Z");
		Assert.assertTrue(sample.trim().matches(re));

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
	public void intuitAlmostISONoSeps() throws Exception {
		DateTimeParser det = new DateTimeParser();
		String sample = "20040101T123541";

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

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyyMMdd'T'HHmmss");

		String re = result.getRegExp();
		Assert.assertEquals(re, "\\d{8}T\\d{6}");
		Assert.assertTrue(sample.trim().matches(re));

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
	public void intuitAlmostISO() throws Exception {
		DateTimeParser det = new DateTimeParser();
		String sample = "2004-01-01 12:35:41-0500";

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

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyy-MM-dd HH:mm:ssxx");

		String re = result.getRegExp();
		Assert.assertEquals(re, "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}[-+][0-9]{4}");
		Assert.assertTrue(sample.trim().matches(re));

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
	public void intuitAlmostISO_2() throws Exception {
		DateTimeParser det = new DateTimeParser();
		String sample = "2004-01-01 12:35:41";

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

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyy-MM-dd HH:mm:ss");

		String re = result.getRegExp();
		Assert.assertEquals(re, "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
		Assert.assertTrue(sample.trim().matches(re));

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
	public void intuitAlmostISO_3() throws Exception {
		DateTimeParser det = new DateTimeParser();
		String sample = "2004-01-01 12:35:41.0";
		Assert.assertEquals(DateTimeParser.determineFormatString(sample, null), "yyyy-MM-dd HH:mm:ss.S");

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
		Assert.assertEquals(result.getFormatString(), "yyyy-MM-dd HH:mm:ss.S");

		String re = result.getRegExp();
		Assert.assertEquals(re, "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{1}");
		Assert.assertTrue(sample.trim().matches(re));

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
	public void intuitAlmostISO_4() throws Exception {
		DateTimeParser det = new DateTimeParser();
		String sample = "2004-01-01 12:35:41.999";
		Assert.assertEquals(DateTimeParser.determineFormatString(sample, null), "yyyy-MM-dd HH:mm:ss.SSS");

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

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyy-MM-dd HH:mm:ss.SSS");

		String re = result.getRegExp();
		Assert.assertEquals(re, "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}");
		Assert.assertTrue(sample.trim().matches(re));

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
	public void intuitTimeDate() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("9:57 2/13/98", null), "H:mm M/dd/yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("9:57 2/12/98", null), "H:mm ?/??/yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12:57 13/12/98", null), "HH:mm dd/MM/yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("8:57:02 12/12/2012", null), "H:mm:ss ??/??/yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12:57:02 2012/12/12", null), "HH:mm:ss yyyy/MM/dd");
	}

	@Test
	public void parseddMMMyyyy() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("2-Jan-2017", null), "d-MMM-yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12-May-14", null), "dd-MMM-yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("21 Jan 2017", null), "dd MMM yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("8 Dec 1993", null), "d MMM yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("25-Dec-2017", null), "dd-MMM-yyyy");
		Assert.assertNull(DateTimeParser.determineFormatString("21-Jam-2017", null));

		DateTimeParser det = new DateTimeParser();
		String sample = "2 Jan 2017";
		det.train(sample);

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "d MMM yyyy");
		String re = result.getRegExp();
		Assert.assertEquals(re, "\\d{1,2} \\p{Alpha}{3} \\d{4}");
		Assert.assertTrue(sample.matches(re));

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
	public void intuitTimeDateWithTimeZone() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("01/30/2012 10:59:48 GMT", null),
				"MM/dd/yyyy HH:mm:ss z");
	}

	@Test
	public void intuitDateTrainSlash() throws Exception {
		DateTimeParser det = new DateTimeParser();
		String sample = "12/12/12";
		det.train("12/12/12");
		det.train("12/12/32");
		det.train("02/22/02");
		for (int i = 0; i < 20; i++)
			det.train("02/02/99");
		det.train("02/O2/99");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "MM/dd/yy");
		Assert.assertEquals(result.getType(), PatternInfo.Type.DATE);
		String re = result.getRegExp();
		Assert.assertEquals(re, "\\d{2}/\\d{2}/\\d{2}");
		Assert.assertTrue(sample.matches(re));
	}

	@Test
	public void intuitDateTrainYYYYSlash() throws Exception {
		DateTimeParser det = new DateTimeParser();
		String sample = "2012/12/12";
		det.train(sample);
		det.train("2012/11/11");
		det.train("2012/10/32");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyy/MM/dd");
		String re = result.getRegExp();
		Assert.assertEquals(re, "\\d{4}/\\d{2}/\\d{2}");
		Assert.assertTrue(sample.matches(re));

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
	public void yyyyMd() throws Exception {
		DateTimeParser det = new DateTimeParser();
		String sample = "8547 8 6";
		det.train(sample);

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyy M d");
		Assert.assertEquals(result.getType(), PatternInfo.Type.DATE);
		String re = result.getRegExp();
		Assert.assertEquals(re, "\\d{4} \\d{1,2} \\d{1,2}");
		Assert.assertTrue(sample.matches(re));
	}

	@Test
	public void timeFirst() throws Exception {
		DateTimeParser det = new DateTimeParser();
		String sample = "7:05 5/4/38";
		det.train(sample);

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "H:mm ?/?/yy");
		Assert.assertEquals(result.getType(), PatternInfo.Type.DATETIME);
		String re = result.getRegExp();
		Assert.assertEquals(re, "\\d{1,2}:\\d{2} \\d{1,2}/\\d{1,2}/\\d{2}");
		Assert.assertTrue(sample.matches(re));
	}

	@Test
	public void bogusInput() throws Exception {
		String inputs = "21/12/99:|21/12/99:|18:46:|4:38  39|3124/08/|890/65 1/|7/87/33| 89:50|18:52 56:|18/94/06|0463 5 71|50 9:22|" +
				"95/06/88|0-27-98|08/56 22/|31-0-99|0/7:6/11 //61|8:73/4/13 15|14/23/3367| 00/21/79|22-23-00|0/20/2361|0/2/52 9:50 4 |" +
				"1:57:11  1/4/98|2015-8-17T|4/01/41 3:43 T450|37/8/005 5:05|0/6/95|0000 7 1|2000-12-12T12:45-72|2000-12-12T12:45-112|" +
				"12:45:64.|84:12:45.5712:45| 12:45:63.3 |";
		String[] input = inputs.split("\\|");

		for (String testCase : input) {
			DateTimeParser det = new DateTimeParser();
			det.train(testCase);
			DateTimeParserResult result = det.getResult();
			Assert.assertNull(result, testCase);
		}
	}

	//@Test
	public void bogusInput2() throws Exception {
		String testInput = "12:45:.085";
		DateTimeParser det = new DateTimeParser();
		det.train(testInput);
		DateTimeParserResult result = det.getResult();
		String formatString = result.getFormatString();
		PatternInfo.Type type = result.getType();

		System.err.printf("getFormatString(): '%s', getType(): '%s'\n", formatString, type);

		String trimmed = testInput.trim();

		try {
			result.parse(trimmed);
		}
		catch (DateTimeParseException e) {
			System.err.printf("Message: '%s', at '%s', offset %d\n", e.getMessage(), e.getParsedString(), e.getErrorIndex());
		}
		if (testInput.indexOf('?') != -1)
			return;
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatString);
			if (PatternInfo.Type.TIME.equals(type))
				LocalTime.parse(trimmed, formatter);
			else if (PatternInfo.Type.DATE.equals(type))
				LocalDate.parse(trimmed, formatter);
			else if (PatternInfo.Type.DATETIME.equals(type))
				LocalDateTime.parse(trimmed, formatter);
			else if (PatternInfo.Type.ZONEDDATETIME.equals(type))
				ZonedDateTime.parse(trimmed, formatter);
			else
				OffsetDateTime.parse(trimmed, formatter);
		}
		catch (DateTimeParseException e) {
			System.err.printf("Java Message: '%s', at '%s', offset %d\n", e.getMessage(), e.getParsedString(), e.getErrorIndex());
		}
		Assert.assertNull(result);
	}

	@Test
	public void intuitHHMMTrain() throws Exception {
		DateTimeParser det = new DateTimeParser();
		String sampleOne = "12:57";
		String sampleThree = "8:03";
		det.train(sampleOne);
		det.train("13:45");
		det.train(sampleThree);

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "H:mm");
		Assert.assertEquals(result.getType(), PatternInfo.Type.TIME);
		String re = result.getRegExp();
		Assert.assertEquals(re, "\\d{1,2}:\\d{2}");
		Assert.assertTrue(sampleOne.matches(re));
		Assert.assertTrue(sampleThree.matches(re));
	}

	private void add(Map<String, Integer> counter, String key) {
		Integer seen = counter.get(key);
		if (seen == null)
			counter.put(key, 1);
		else
			counter.put(key, seen + 1);
	}

	private void dump(Map<String, Integer> counter) {
		Map<String, Integer> byValue = DateTimeParser.sortByValue(counter);
		for (Map.Entry<String, Integer> entry : byValue.entrySet()) {
			System.err.printf("'%s' : %d\n", entry.getKey(), entry.getValue());
		}
	}

	//@Test
	public void fuzz() throws Exception {
		Random randomGenerator = new Random(12);
		Map<String, Integer> formatStrings = new HashMap<String, Integer>();
		Map<String, Integer> types = new HashMap<String, Integer>();
		int good = 0;
		int iterations = 1000000000;
		String[] timeZones = TimeZone.getAvailableIDs();

		for (int iters = 0; iters < iterations; iters++) {
			int len = 5 + randomGenerator.nextInt(15);
			StringBuilder s = new StringBuilder(len);
			int digits = 0;
		    for (int i = 0; s.length() <= len; ++i) {
		      int randomInt = randomGenerator.nextInt(100);
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
		    	  int idx = randomGenerator.nextInt(timeZones.length - 1);
		    	  s.append(timeZones[idx]);
		    	  continue;
		      }
		      s.append(",.;':\"[]{}\\|=!@#$%^&*<>".charAt(randomGenerator.nextInt(100) % 23));
		    }
			DateTimeParser det = new DateTimeParser();
			String input = s.toString();
			//System.err.printf("Input ... '%s'\n", input);
			String trimmed = input.trim();
			try {
				det.train(input);

				DateTimeParserResult result = det.getResult();
				if (result != null) {
					good++;
					String formatString = result.getFormatString();

					String re = result.getRegExp();
					Assert.assertTrue(trimmed.matches(re), "input: '" + trimmed + "', RE: '" + re + "'");

					PatternInfo.Type type = result.getType();
					add(formatStrings, formatString);
					add(types, type.toString());
					result.parse(trimmed);
					if (formatString.indexOf('?') != -1)
						continue;

					try {
						DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatString);
						if (PatternInfo.Type.TIME.equals(type))
							LocalTime.parse(trimmed, formatter);
						else if (PatternInfo.Type.DATE.equals(type))
							LocalDate.parse(trimmed, formatter);
						else if (PatternInfo.Type.DATETIME.equals(type))
							LocalDateTime.parse(trimmed, formatter);
						else if (PatternInfo.Type.ZONEDDATETIME.equals(type))
							ZonedDateTime.parse(trimmed, formatter);
						else
							OffsetDateTime.parse(trimmed, formatter);
					}
					catch (DateTimeParseException exc) {
						System.err.printf("Java: Struggled with input of the form: '%s'\n", input);
					}

				}
			}
			catch (Exception e) {
				System.err.printf("Struggled with input of the form: '%s'\n", input);
			}
		}

		dump(formatStrings);
		dump(types);

		System.err.printf("Good %d out of %d (%%%f)\n", good, iterations, 100*((float)good/iterations));
	}

	@Test
	public void intuitMMDDYYYY_HHMMSSTrain() throws Exception {
		DateTimeParser det = new DateTimeParser();
		String sample = "01/26/2012 10:42:23 GMT";
		det.train(sample);
		det.train("01/30/2012 10:59:48 GMT");
		det.train("01/25/2012 16:46:43 GMT");
		det.train("01/25/2012 16:28:42 GMT");
		det.train("01/24/2012 16:53:04 GMT");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "MM/dd/yyyy HH:mm:ss z");
		Assert.assertEquals(result.getType(), PatternInfo.Type.ZONEDDATETIME);

		String re = result.getRegExp();
		Assert.assertEquals(re, "\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2} .*");
		Assert.assertTrue(sample.matches(re));

		Assert.assertTrue(result.isValid("01/26/2012 10:42:23 GMT"));
		Assert.assertTrue(result.isValid8("01/26/2012 10:42:23 GMT"));
	}

	//@Test
	public void testPerf() throws Exception {
		DateTimeParser det = new DateTimeParser();
		String sample = "01/26/2012 10:42:23 GMT";
		det.train(sample);
		det.train("01/30/2012 10:59:48 GMT");
		det.train("01/25/2012 16:46:43 GMT");
		det.train("01/25/2012 16:28:42 GMT");
		det.train("01/24/2012 16:53:04 GMT");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "MM/dd/yyyy HH:mm:ss z");
		Assert.assertEquals(result.getType(), PatternInfo.Type.DATETIME);

		String re = result.getRegExp();
		Assert.assertEquals(re, "\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2} .*");
		Assert.assertTrue(sample.matches(re));

		int iterations = 10000000;
		long start = System.currentTimeMillis();
		for (int i = 0; i < iterations; i++) {
			Assert.assertTrue(result.isValid("01/26/2012 10:42:23 GMT"));
		}
		long doneCustom = System.currentTimeMillis();
		for (int i = 0; i < iterations; i++) {
			Assert.assertTrue(result.isValid8("01/26/2012 10:42:23 GMT"));
		}
		long done = System.currentTimeMillis();
		System.err.printf("Custom = %dms, Java 8 = %dms\n", doneCustom - start, done - doneCustom);
	}

	@Test
	public void intuitInsufficientFactsTrain() throws Exception {
		DateTimeParser detPrime = new DateTimeParser();
		String sample = "12/30/99";

		detPrime.train(sample);

		DateTimeParserResult resultPrime = detPrime.getResult();
		Assert.assertEquals(resultPrime.getFormatString(), "MM/dd/yy");

		String re = resultPrime.getRegExp();
		Assert.assertEquals(re, "\\d{2}/\\d{2}/\\d{2}");
		Assert.assertTrue(sample.matches(re));

		DateTimeParser det = new DateTimeParser();
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

		re = result.getRegExp();
		Assert.assertEquals(re, "\\d{2}/\\d{2}/\\d{2}");
		Assert.assertTrue(sample.trim().matches(re));

		// Force to be day first
		DateTimeParser detDayFirst = new DateTimeParser(true);
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

		re = result.getRegExp();
		Assert.assertEquals(re, "\\d{2}/\\d{2}/\\d{2}");
		Assert.assertTrue(sample.trim().matches(re));

		Assert.assertTrue(result.isValid("31/12/12"));
		Assert.assertFalse(result.isValid("12/31/12"));
		Assert.assertFalse(result.isValid("2012/12/12"));
		Assert.assertTrue(result.isValid8("31/12/12"));
		Assert.assertFalse(result.isValid8("12/31/12"));
		Assert.assertFalse(result.isValid8("2012/12/12"));

		// Force to be month first
		DateTimeParser detMonthFirst = new DateTimeParser(false);
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

		re = result.getRegExp();
		Assert.assertEquals(re, "\\d{2}/\\d{2}/\\d{2}");
		Assert.assertTrue(sample.trim().matches(re));

		Assert.assertFalse(result.isValid("31/12/12"));
		Assert.assertTrue(result.isValid("12/31/12"));
		Assert.assertFalse(result.isValid("2012/12/12"));
		Assert.assertFalse(result.isValid8("31/12/12"));
		Assert.assertTrue(result.isValid8("12/31/12"));
		Assert.assertFalse(result.isValid8("2012/12/12"));
	}

	@Test
	public void intuitDateMMddyy() throws Exception {
		DateTimeParser det = new DateTimeParser();
		String sample = " 04/03/13";

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

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "MM/dd/yy");

		String re = result.getRegExp();
		Assert.assertEquals(re, "\\d{2}/\\d{2}/\\d{2}");
		Assert.assertTrue(sample.trim().matches(re));

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
	public void intuitDateddMMyyyy_HHmmss() throws Exception {
		DateTimeParser det = new DateTimeParser(false);
		String sample = "2/7/2012 06:24:47";

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
		Assert.assertEquals(result.getFormatString(), "d/M/yyyy HH:mm:ss");

		String re = result.getRegExp();
		Assert.assertEquals(re, "\\d{1,2}/\\d{1,2}/\\d{4} \\d{2}:\\d{2}:\\d{2}");
		Assert.assertTrue(sample.trim().matches(re));
	}

	@Test
	public void intuitDateyyMMdd() throws Exception {
		DateTimeParser det = new DateTimeParser();
		String sample = "98/03/13";

		det.train(sample);
		det.train("03/03/13");
		det.train("34/03/13");
		det.train("46/03/13");
		det.train("59/03/13");
		det.train("09/03/31");
		det.train("10/03/13");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yy/MM/dd");

		String re = result.getRegExp();
		Assert.assertEquals(re, "\\d{2}/\\d{2}/\\d{2}");
		Assert.assertTrue(sample.trim().matches(re));

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
	public void intuitDatedMMMyy() throws Exception {
		DateTimeParser det = new DateTimeParser();
		String sample = "1-Jan-14";
		det.train(sample);
		det.train("10-Jan-14");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "d-MMM-yy");

		String re = result.getRegExp();
		Assert.assertEquals(re, "\\d{1,2}-\\p{Alpha}{3}-\\d{2}");
		Assert.assertTrue(sample.trim().matches(re));

		Assert.assertTrue(result.isValid("1-Jan-14"));
		Assert.assertTrue(result.isValid("10-Jan-14"));

		Assert.assertTrue(result.isValid8("1-Jan-14"));
		Assert.assertTrue(result.isValid8("10-Jan-14"));
	}

	@Test
	public void intuitHHMMSSTrain() throws Exception {
		DateTimeParser det = new DateTimeParser();
		String sample = "12:57:03";
		det.train(sample);
		det.train("13:45:00");
		det.train("8:03:59");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "H:mm:ss");

		String re = result.getRegExp();
		Assert.assertEquals(re, "\\d{1,2}:\\d{2}:\\d{2}");
		Assert.assertTrue(sample.trim().matches(re));

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
