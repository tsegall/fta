/*
 * Copyright 2017-2020 Tim Segall
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
import java.util.Locale;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestRegExpSupport {
	@Test
	public void phone() throws IOException {
		Assert.assertEquals(Smashed.smashedAsRegExp("+9 999 999 9999")
				, "\\+\\d \\d{3} \\d{3} \\d{4}");
	}

	@Test
	public void simple() throws IOException {
		Assert.assertEquals(Smashed.smashedAsRegExp("xxx"),
				"\\p{IsAlphabetic}{3}");
	}

	@Test
	public void mixedAlpha() throws IOException {
		Assert.assertEquals(Smashed.smashedAsRegExp("xxXX"),
				"\\p{IsAlphabetic}{4}");
	}

	@Test
	public void mac() throws IOException {
		Assert.assertEquals(Smashed.smashedAsRegExp("HH:HH:HH:HH:HH:HH"),
				"\\p{XDigit}{2}:\\p{XDigit}{2}:\\p{XDigit}{2}:\\p{XDigit}{2}:\\p{XDigit}{2}:\\p{XDigit}{2}");
	}

	@Test
	public void mix() throws IOException {
		Assert.assertEquals(Smashed.smashedAsRegExp("99XXXX:99X"),
				"\\d{2}\\p{IsAlphabetic}{4}:\\d{2}\\p{IsAlphabetic}");
	}

	@Test
	public void onlyAlpha() throws IOException {
		Assert.assertEquals(Smashed.smashedAsRegExp("XXXXX"), "\\p{IsAlphabetic}{5}");
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

	@Test
	public void rangeTestAlpha() throws IOException {
		RegExpGenerator gen = new RegExpGenerator(true, 30, Locale.getDefault());

		gen.train("A");
		gen.train("B");
		gen.train("C");
		gen.train("D");
		Assert.assertEquals(gen.getResult(), "[A-D]");
	}

	@Test
	public void rangeTestNumbers() throws IOException {
		RegExpGenerator gen = new RegExpGenerator(true, 30, Locale.getDefault());

		gen.train("0");
		gen.train("1");
		gen.train("2");
		gen.train("3");
		gen.train("4");
		gen.train("5");
		gen.train("6");
		gen.train("7");
		gen.train("8");
		gen.train("9");
		gen.train("2");
		gen.train("3");
		gen.train("4");
		gen.train("5");
		Assert.assertEquals(gen.getResult(), "[0-9]");
	}
}
