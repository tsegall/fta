package com.cobber.fta;

import org.testng.Assert;
import org.testng.annotations.Test;

public class DetermineDateTimeFormatTests {
	@Test
	public void intuitTimeOnly() throws Exception {
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("9:57"), "HH:MM");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("12:57"), "HH:MM");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("8:57:02"), "HH:MM:SS");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("12:57:02"), "HH:MM:SS");
		Assert.assertNull(DetermineDateTimeFormat.intuitDateTimeFormat(":57:02"));
		Assert.assertNull(DetermineDateTimeFormat.intuitDateTimeFormat("123:02"));
		Assert.assertNull(DetermineDateTimeFormat.intuitDateTimeFormat("12:023"));
		Assert.assertNull(DetermineDateTimeFormat.intuitDateTimeFormat("12:0"));
		Assert.assertNull(DetermineDateTimeFormat.intuitDateTimeFormat("12:02:1"));
		Assert.assertNull(DetermineDateTimeFormat.intuitDateTimeFormat("12:02:"));
		Assert.assertNull(DetermineDateTimeFormat.intuitDateTimeFormat("12::02"));
	}
	
	@Test
	public void intuitDateOnlySlash() throws Exception {
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("2/12/98"), "XX/XX/YY");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("2/2/02"), "XX/XX/XX");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("2/31/02"), "XX/DD/XX");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("31/02/02"), "XX/XX/XX");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("12/12/98"), "XX/XX/YY");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("14/12/98"), "DD/MM/YY");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("12/14/98"), "MM/DD/YY");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("12/12/2012"), "XX/XX/YYYY");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("20/12/2012"), "DD/MM/YYYY");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("11/15/2012"), "MM/DD/YYYY");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("2012/12/12"), "YYYY/MM/DD");
		Assert.assertNull(DetermineDateTimeFormat.intuitDateTimeFormat("/57/02"));
		Assert.assertNull(DetermineDateTimeFormat.intuitDateTimeFormat("123/02"));
		Assert.assertNull(DetermineDateTimeFormat.intuitDateTimeFormat("12/023"));
		Assert.assertNull(DetermineDateTimeFormat.intuitDateTimeFormat("12/0"));
		Assert.assertNull(DetermineDateTimeFormat.intuitDateTimeFormat("12/02/1"));
		Assert.assertNull(DetermineDateTimeFormat.intuitDateTimeFormat("12/02/"));
		Assert.assertNull(DetermineDateTimeFormat.intuitDateTimeFormat("12/02-99"));
	}
	
	@Test
	public void intuitDateOnlyDash() throws Exception {
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("2-12-98"), "XX-XX-YY");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("12-12-98"), "XX-XX-YY");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("14-12-98"), "DD-MM-YY");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("12-14-98"), "MM-DD-YY");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("12-12-2012"), "XX-XX-YYYY");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("2012-12-12"), "YYYY-MM-DD");
	}
	
	@Test
	public void intuitDateTime() throws Exception {
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("2/12/98 9:57"), "XX/XX/YY HH:MM");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("2/13/98 9:57"), "MM/DD/YY HH:MM");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("13/12/98 12:57"), "DD/MM/YY HH:MM");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("12/12/2012 8:57:02"), "XX/XX/YYYY HH:MM:SS");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("13/12/2012 8:57:02"), "DD/MM/YYYY HH:MM:SS");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("2012/12/12 12:57:02"), "YYYY/MM/DD HH:MM:SS");
	}
	
	@Test
	public void intuitTimeDate() throws Exception {
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("9:57 2/12/98"), "HH:MM XX/XX/YY");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("9:57 2/13/98"), "HH:MM MM/DD/YY");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("12:57 13/12/98"), "HH:MM DD/MM/YY");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("8:57:02 12/12/2012"), "HH:MM:SS XX/XX/YYYY");
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("12:57:02 2012/12/12"), "HH:MM:SS YYYY/MM/DD");
	}
	
	@Test
	public void intuitTimeDateWithTimeZone() throws Exception {
		Assert.assertEquals(DetermineDateTimeFormat.intuitDateTimeFormat("01/30/2012 10:59:48 GMT"), "MM/DD/YYYY HH:MM:SS z");
	}

	@Test
	public void intuitDateTrainSlash() throws Exception {
		DetermineDateTimeFormat det = new DetermineDateTimeFormat();
		det.train("12/12/12");
		det.train("12/12/32");
		det.train("02/22/02");
		
		Assert.assertEquals(det.getResult(), "MM/DD/YY");
	}
	
	@Test
	public void intuitHHMMTrain() throws Exception {
		DetermineDateTimeFormat det = new DetermineDateTimeFormat();
		det.train("12:57");
		det.train("13:45");
		det.train("8:03");
		
		Assert.assertEquals(det.getResult(), "HH:MM");
	}

	@Test
	public void intuitHHMMSSTrain() throws Exception {
		DetermineDateTimeFormat det = new DetermineDateTimeFormat();
		det.train("12:57:03");
		det.train("13:45:00");
		det.train("8:03:59");
		
		Assert.assertEquals(det.getResult(), "HH:MM:SS");
	}
}
