package com.cobber.fta;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestRegExpSupport {
	@Test
	public void phone() throws IOException {
		Assert.assertEquals(RegExpGenerator.smashedAsRegExp("+1 111 111 1111"), "\\+\\p{IsDigit} \\p{IsDigit}{3} \\p{IsDigit}{3} \\p{IsDigit}{4}");
	}

	@Test
	public void onlyAlpha() throws IOException {
		Assert.assertEquals(RegExpGenerator.smashedAsRegExp("aaaaa"), "\\p{IsAlphabetic}{5}");
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
