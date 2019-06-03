package com.cobber.fta;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestRegExpSupport {
	@Test
	public void phone() throws IOException {
		Assert.assertEquals(RegExpGenerator.smashedAsRegExp("+9 999 999 9999"), "\\+\\d \\d{3} \\d{3} \\d{4}");
	}

	@Test
	public void onlyAlpha() throws IOException {
		Assert.assertEquals(RegExpGenerator.smashedAsRegExp("XXXXX"), "\\p{IsAlphabetic}{5}");
	}

	@Test
	public void mixedZip() throws IOException {
		RegExpGenerator gen = new RegExpGenerator();

		gen.train("1-1-1-11");
		gen.train("1-1-11-11");
		gen.train("1-1-11-1");
		gen.train("1-1-1-1");
		Assert.assertEquals(gen.getResult(), "[\\p{IsDigit}\\-]{7,9}");
	}
}
