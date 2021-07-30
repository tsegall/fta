/*
 * Copyright 2017-2021 Tim Segall
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.cobber.fta.core.RegExpGenerator;

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
		final RegExpGenerator gen = new RegExpGenerator();

		gen.train("1-1-1-11");
		gen.train("1-1-11-11");
		gen.train("1-1-11-1");
		gen.train("1-1-1-1");
		Assert.assertEquals(gen.getResult(), "[\\p{IsDigit}\\-]{7,9}");
	}

	@Test
	public void fromFile15() throws IOException {
		final RegExpGenerator gen = new RegExpGenerator(15, Locale.getDefault());

		try (BufferedReader br = new BufferedReader(new InputStreamReader(LogicalTypeFiniteSimpleExternal.class.getResourceAsStream("/reference/en_world_region.csv")))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		       gen.train(line);
		    }
		}

		Assert.assertEquals(gen.getResult(), "(?i)(AFRICA|ANTARCTICA|ASIA|ASIA PACIFIC|AUSTRALIA/NZ|CARIBBEAN|CENTRAL AMERICA|EUROPE|MIDDLE EAST|NORTH AMERICA|OCEANIA|SOUTH AMERICA|THE CARIBBEAN)");
	}

	@Test
	public void fromFile5() throws IOException {
		final RegExpGenerator gen = new RegExpGenerator(5, Locale.getDefault());

		try (BufferedReader br = new BufferedReader(new InputStreamReader(LogicalTypeFiniteSimpleExternal.class.getResourceAsStream("/reference/en_world_region.csv")))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		       gen.train(line);
		    }
		}

		Assert.assertEquals(gen.getResult(), ".+");
	}

	@Test
	public void rangeTestAlpha() throws IOException {
		final RegExpGenerator gen = new RegExpGenerator(30, Locale.getDefault());

		gen.train("A");
		gen.train("B");
		gen.train("C");
		gen.train("D");
		Assert.assertEquals(gen.getResult(), "[A-D]");
	}

	@Test
	public void rangeTestNumbers() throws IOException {
		final RegExpGenerator gen = new RegExpGenerator(30, Locale.getDefault());

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

	@Test
	public void rangeConstantComponent() throws IOException {
		final RegExpGenerator gen = new RegExpGenerator(30, Locale.getDefault());

		gen.train("AXP109785");
		gen.train("AXP356785");
		gen.train("AXP109005");
		gen.train("AXP223785");
		gen.train("AXP347885");
		gen.train("AXP111185");
		gen.train("AXP166778");
		gen.train("AXP734377");
		gen.train("AXP093633");
		gen.train("AXP098637");
		gen.train("AXP371295");
		gen.train("AXP343456");
		gen.train("AXP000345");
		gen.train("AXP990213");
		Assert.assertEquals(gen.getResult(), "(?i)(AXP000345|AXP093633|AXP098637|AXP109005|AXP109785|AXP111185|AXP166778|AXP223785|AXP343456|AXP347885|AXP356785|AXP371295|AXP734377|AXP990213)");
	}
}
