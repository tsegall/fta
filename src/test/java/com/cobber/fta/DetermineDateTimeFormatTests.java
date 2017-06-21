package com.cobber.fta;

import java.time.format.DateTimeParseException;

import org.testng.Assert;
import org.testng.annotations.Test;

public class DetermineDateTimeFormatTests {
	@Test
	public void intuitTimeOnly() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("9:57"), "H:mm");
		Assert.assertEquals(DateTimeParser.determineFormatString("12:57"), "HH:mm");
		Assert.assertEquals(DateTimeParser.determineFormatString("8:57:02"), "H:mm:ss");
		Assert.assertEquals(DateTimeParser.determineFormatString("12:57:02"), "HH:mm:ss");
		Assert.assertNull(DateTimeParser.determineFormatString(":57:02"));
		Assert.assertNull(DateTimeParser.determineFormatString("123:02"));
		Assert.assertNull(DateTimeParser.determineFormatString("12:023"));
		Assert.assertNull(DateTimeParser.determineFormatString("12:023:12"));
		Assert.assertNull(DateTimeParser.determineFormatString("12:0"));
		Assert.assertNull(DateTimeParser.determineFormatString("12:02:1"));
		Assert.assertNull(DateTimeParser.determineFormatString("12:02:12:14"));
		Assert.assertNull(DateTimeParser.determineFormatString("12:02:124"));
		Assert.assertNull(DateTimeParser.determineFormatString("12:02:"));
		Assert.assertNull(DateTimeParser.determineFormatString("12::02"));
	}

	@Test
	public void intuitDateOnlySlash() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("2/12/98"), "X/XX/yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("2/2/02"), "X/X/XX");
		Assert.assertEquals(DateTimeParser.determineFormatString("2/31/02"), "M/dd/yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("31/02/02"), "XX/XX/XX");
		Assert.assertEquals(DateTimeParser.determineFormatString("12/12/98"), "XX/XX/yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("14/12/98"), "dd/MM/yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12/14/98"), "MM/dd/yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12/12/2012"), "XX/XX/yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("20/12/2012"), "dd/MM/yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("11/15/2012"), "MM/dd/yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("2012/12/12"), "yyyy/MM/dd");
		Assert.assertNull(DateTimeParser.determineFormatString("/57/02"));
		Assert.assertNull(DateTimeParser.determineFormatString("123/02"));
		Assert.assertNull(DateTimeParser.determineFormatString("12/023"));
		Assert.assertNull(DateTimeParser.determineFormatString("12/0"));
		Assert.assertNull(DateTimeParser.determineFormatString("12/02/1"));
		Assert.assertNull(DateTimeParser.determineFormatString("12/023/12"));
		Assert.assertNull(DateTimeParser.determineFormatString("12/02/"));
		Assert.assertNull(DateTimeParser.determineFormatString("12/02-99"));
	}

	/*
	@Test
	public void testSpaces() throws Exception {
		Assert.assertEquals(DateTimeParser.parse("2018 12 24"), "yyyy MM dd");
	}
	*/

	@Test
	public void intuitDateOnlyDash() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("2-12-98"), "X-XX-yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12-12-98"), "XX-XX-yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("14-12-98"), "dd-MM-yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12-14-98"), "MM-dd-yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12-12-2012"), "XX-XX-yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("2012-12-12"), "yyyy-MM-dd");
		Assert.assertNull(DateTimeParser.determineFormatString("20120-12-12"));
	}

	@Test
	public void intuit8601DD_DD() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("2004-01-01T00:00:00+05:00"), "yyyy-MM-dd'T'HH:mm:ssxxx");

		DateTimeParser det = new DateTimeParser();
		det.train("2004-01-01T00:00:00+05:00");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyy-MM-dd'T'HH:mm:ssxxx");

		Assert.assertTrue(result.isValid("2004-01-01T00:00:00+05:00"));
		Assert.assertTrue(result.isValid("2012-03-04T19:22:10+08:00"));
		Assert.assertFalse(result.isValid("2012-03-04T19:22:10+08:0"));
		Assert.assertFalse(result.isValid("2012-03-04T19:22:10+?08:00"));
		Assert.assertTrue(result.isValid8("2004-01-01T00:00:00+05:00"));
		Assert.assertTrue(result.isValid8("2012-03-04T19:22:10+08:00"));
		Assert.assertFalse(result.isValid8("2012-03-04T19:22:10+08:0"));
		Assert.assertFalse(result.isValid8("2012-03-04T19:22:10+?08:00"));
	}

	@Test
	public void intuit8601DD_DD_DD() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("2004-01-01T00:00:00+05:00:00"), "yyyy-MM-dd'T'HH:mm:ssxxxxx");

		DateTimeParser det = new DateTimeParser();
		det.train("2004-01-01T00:00:00+05:00:00");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyy-MM-dd'T'HH:mm:ssxxxxx");

		Assert.assertTrue(result.isValid("2004-01-01T00:00:00+05:00:00"));
		Assert.assertTrue(result.isValid("2012-03-04T19:22:10+08:00:00"));
		Assert.assertFalse(result.isValid("2012-03-04T19:22:10+08:00:0"));
		Assert.assertFalse(result.isValid("2012-03-04T19:22:10+O8:00:00"));
		Assert.assertTrue(result.isValid8("2004-01-01T00:00:00+05:00:00"));
		Assert.assertTrue(result.isValid8("2012-03-04T19:22:10+08:00:00"));
		Assert.assertFalse(result.isValid8("2012-03-04T19:22:10+08:00:0"));
		Assert.assertFalse(result.isValid8("2012-03-04T19:22:10+O8:00:00"));
	}

	@Test
	public void testAsResult() throws Exception {
		Assert.assertNull(DateTimeParserResult.asResult("yyyy-MM-ddTHH:m:ssx"));
		Assert.assertNull(DateTimeParserResult.asResult("yyyy-MM-ddTHH:mm:sx"));
		Assert.assertNull(DateTimeParserResult.asResult("yyyy-MM-ddTHH:mm:sx"));
	}

	@Test
	public void testParse() throws Exception {
		DateTimeParserResult result = DateTimeParserResult.asResult("yyyy/MM/dd HH:mm");

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
		Assert.assertNull(DateTimeParser.determineFormatString("0þþþþþ"));
		Assert.assertEquals(DateTimeParser.determineFormatString("2/12/98 9:57"), "X/XX/yy H:mm");
		Assert.assertNull(DateTimeParser.determineFormatString("2/12/98 :57"));
		Assert.assertNull(DateTimeParser.determineFormatString("2/12/98 9:5"));
		Assert.assertNull(DateTimeParser.determineFormatString("2/12/98 9:55:5"));
		Assert.assertEquals(DateTimeParser.determineFormatString("2/13/98 9:57"), "M/dd/yy H:mm");
		Assert.assertEquals(DateTimeParser.determineFormatString("13/12/98 12:57"), "dd/MM/yy HH:mm");
		Assert.assertEquals(DateTimeParser.determineFormatString("12/12/2012 8:57:02"), "XX/XX/yyyy H:mm:ss");
		Assert.assertEquals(DateTimeParser.determineFormatString("12/12/2012 8:57:02 GMT"),
				"XX/XX/yyyy H:mm:ss z");
		Assert.assertEquals(DateTimeParser.determineFormatString("13/12/2012 8:57:02"), "dd/MM/yyyy H:mm:ss");
		Assert.assertEquals(DateTimeParser.determineFormatString("2012/12/12 12:57:02"), "yyyy/MM/dd HH:mm:ss");

		DateTimeParser det = new DateTimeParser();
		det.train("12/12/2012 8:57:02 GMT");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "XX/XX/yyyy H:mm:ss z");

		result.forceResolve(true);
		Assert.assertEquals(result.getFormatString(), "dd/MM/yyyy H:mm:ss z");
		result.forceResolve(false);
		Assert.assertEquals(result.getFormatString(), "MM/dd/yyyy H:mm:ss z");

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
	public void intuitTimeDate() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("9:57 2/12/98"), "H:mm X/XX/yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("9:57 2/13/98"), "H:mm M/dd/yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12:57 13/12/98"), "HH:mm dd/MM/yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("8:57:02 12/12/2012"), "H:mm:ss XX/XX/yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12:57:02 2012/12/12"), "HH:mm:ss yyyy/MM/dd");
	}

	@Test
	public void parseddMMMyyyy() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("2-Jan-2017"), "d-MMM-yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12-May-14"), "dd-MMM-yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("21 Jan 2017"), "dd MMM yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("8 Dec 1993"), "d MMM yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("25-Dec-2017"), "dd-MMM-yyyy");
		Assert.assertNull(DateTimeParser.determineFormatString("21-Jam-2017"));

		DateTimeParser det = new DateTimeParser();
		det.train("2 Jan 2017");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "d MMM yyyy");

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
		Assert.assertEquals(DateTimeParser.determineFormatString("01/30/2012 10:59:48 GMT"),
				"MM/dd/yyyy HH:mm:ss z");
	}

	@Test
	public void intuitDateTrainSlash() throws Exception {
		DateTimeParser det = new DateTimeParser();
		det.train("12/12/12");
		det.train("12/12/32");
		det.train("02/22/02");
		for (int i = 0; i < 20; i++)
			det.train("02/02/99");
		det.train("02/O2/99");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "MM/dd/yy");
		Assert.assertEquals(result.getType(), "Date");
	}

	@Test
	public void intuitDateTrainYYYYSlash() throws Exception {
		DateTimeParser det = new DateTimeParser();
		det.train("2012/12/12");
		det.train("2012/11/11");
		det.train("2012/10/32");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyy/MM/dd");

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
	public void intuitHHMMTrain() throws Exception {
		DateTimeParser det = new DateTimeParser();
		det.train("12:57");
		det.train("13:45");
		det.train("8:03");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "H:mm");
		Assert.assertEquals(result.getType(), "Time");
	}

	@Test
	public void intuitMMDDYYYY_HHMMSSTrain() throws Exception {
		DateTimeParser det = new DateTimeParser();
		det.train("01/26/2012 10:42:23 GMT");
		det.train("01/30/2012 10:59:48 GMT");
		det.train("01/25/2012 16:46:43 GMT");
		det.train("01/25/2012 16:28:42 GMT");
		det.train("01/24/2012 16:53:04 GMT");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "MM/dd/yyyy HH:mm:ss z");
		Assert.assertEquals(result.getType(), "ZonedDateTime");

		Assert.assertTrue(result.isValid("01/26/2012 10:42:23 GMT"));
		Assert.assertTrue(result.isValid8("01/26/2012 10:42:23 GMT"));
	}

	//@Test
	public void testPerf() throws Exception {
		DateTimeParser det = new DateTimeParser();
		det.train("01/26/2012 10:42:23 GMT");
		det.train("01/30/2012 10:59:48 GMT");
		det.train("01/25/2012 16:46:43 GMT");
		det.train("01/25/2012 16:28:42 GMT");
		det.train("01/24/2012 16:53:04 GMT");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "MM/dd/yyyy HH:mm:ss z");
		Assert.assertEquals(result.getType(), "DateTime");

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
		DateTimeParser det = new DateTimeParser();

		det.train(" 04/03/13");
		det.train(" 05/03/13");
		det.train(" 06/03/13");
		det.train(" 07/03/13");
		det.train(" 08/03/13");
		det.train(" 09/03/13");
		det.train(" 10/03/13");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "XX/XX/XX");

		// Force to be day first
		result.forceResolve(true);
		Assert.assertEquals(result.getFormatString(), "dd/MM/yy");
		Assert.assertTrue(result.isValid("31/12/12"));
		Assert.assertFalse(result.isValid("12/31/12"));
		Assert.assertFalse(result.isValid("2012/12/12"));
		Assert.assertTrue(result.isValid8("31/12/12"));
		Assert.assertFalse(result.isValid8("12/31/12"));
		Assert.assertFalse(result.isValid8("2012/12/12"));

		// Force to be month first
		result.forceResolve(false);
		Assert.assertEquals(result.getFormatString(), "MM/dd/yy");
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

		det.train(" 04/03/13");
		det.train(" 05/03/13");
		det.train(" 06/03/13");
		det.train(" 07/03/13");
		det.train(" 08/03/13");
		det.train(" 09/31/13");
		det.train(" 10/03/13");
		for (int i = 0; i < 20; i++) {
			det.train("10/10/13");
		}

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "MM/dd/yy");

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
	public void intuitDateyyMMdd() throws Exception {
		DateTimeParser det = new DateTimeParser();

		det.train("98/03/13");
		det.train("03/03/13");
		det.train("34/03/13");
		det.train("46/03/13");
		det.train("59/03/13");
		det.train("09/03/31");
		det.train("10/03/13");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yy/MM/dd");

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
		det.train("1-Jan-14");
		det.train("10-Jan-14");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "d-MMM-yy");

		Assert.assertTrue(result.isValid("1-Jan-14"));
		Assert.assertTrue(result.isValid("10-Jan-14"));

		Assert.assertTrue(result.isValid8("1-Jan-14"));
		Assert.assertTrue(result.isValid8("10-Jan-14"));
	}

	@Test
	public void intuitHHMMSSTrain() throws Exception {
		DateTimeParser det = new DateTimeParser();
		det.train("12:57:03");
		det.train("13:45:00");
		det.train("8:03:59");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "H:mm:ss");

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
