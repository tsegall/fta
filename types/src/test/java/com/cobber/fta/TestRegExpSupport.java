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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.testng.annotations.Test;

import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.token.TokenStream;

public class TestRegExpSupport {
	@Test(groups = { TestGroups.ALL })
	public void phone() throws IOException {
		final TokenStream ts = new TokenStream("+9 999 999 9999", 1);
		assertEquals(ts.getRegExp(false), "\\+\\d \\d{3} \\d{3} \\d{4}");
	}

	@Test(groups = { TestGroups.ALL })
	public void simple() throws IOException {
		final TokenStream ts = new TokenStream("xxx", 1);
		assertEquals(ts.getRegExp(false), "\\p{IsAlphabetic}{3}");
	}

	@Test(groups = { TestGroups.ALL })
	public void mixedAlpha() throws IOException {
		final TokenStream ts = new TokenStream("xxXX", 1);
		assertEquals(ts.getRegExp(false), "\\p{IsAlphabetic}{4}");
	}

	@Test(groups = { TestGroups.ALL })
	public void mix() throws IOException {
		final TokenStream ts = new TokenStream("99XXXX:99X", 1);
		assertEquals(ts.getRegExp(false), "\\d{2}\\p{IsAlphabetic}{4}:\\d{2}\\p{IsAlphabetic}");
	}

	@Test(groups = { TestGroups.ALL })
	public void onlyAlpha() throws IOException {
		final TokenStream ts = new TokenStream("XXXXX", 1);
		assertEquals(ts.getRegExp(false), "\\p{IsAlphabetic}{5}");
	}

	@Test(groups = { TestGroups.ALL })
	public void mixedZip() throws IOException {
		final RegExpGenerator gen = new RegExpGenerator();

		gen.train("1-1-1-11");
		gen.train("1-1-11-11");
		gen.train("1-1-11-1");
		gen.train("1-1-1-1");
		assertEquals(gen.getResult(), "[\\p{IsDigit}\\-]{7,9}");
	}

	@Test(groups = { TestGroups.ALL })
	public void fromFile15() throws IOException {
		final RegExpGenerator gen = new RegExpGenerator(15, Locale.getDefault());

		try (BufferedReader br = new BufferedReader(new InputStreamReader(LogicalTypeFiniteSimpleExternal.class.getResourceAsStream("/reference/en_world_region.csv"), StandardCharsets.UTF_8))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		       gen.train(line);
		    }
		}

		assertEquals(gen.getResult(), "(?i)(AFRICA|ANTARCTICA|ASIA|ASIA PACIFIC|AUSTRALIA/NZ|CARIBBEAN|CENTRAL AMERICA|EUROPE|MIDDLE EAST|NORTH AMERICA|OCEANIA|SOUTH AMERICA|THE CARIBBEAN)");
	}

	@Test(groups = { TestGroups.ALL })
	public void fromFile5() throws IOException {
		final RegExpGenerator gen = new RegExpGenerator(5, Locale.getDefault());

		try (BufferedReader br = new BufferedReader(new InputStreamReader(LogicalTypeFiniteSimpleExternal.class.getResourceAsStream("/reference/en_world_region.csv"), StandardCharsets.UTF_8))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		       gen.train(line);
		    }
		}

		assertEquals(gen.getResult(), ".+");
	}

	@Test(groups = { TestGroups.ALL })
	public void rangeTestAlpha() throws IOException {
		final RegExpGenerator gen = new RegExpGenerator(30, Locale.getDefault());

		gen.train("A");
		gen.train("B");
		gen.train("C");
		gen.train("D");
		assertEquals(gen.getResult(), "[A-D]");
	}

	@Test(groups = { TestGroups.ALL })
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
		assertEquals(gen.getResult(), "[0-9]");
	}

	@Test(groups = { TestGroups.ALL })
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
		assertEquals(gen.getResult(), "(?i)(AXP000345|AXP093633|AXP098637|AXP109005|AXP109785|AXP111185|AXP166778|AXP223785|AXP343456|AXP347885|AXP356785|AXP371295|AXP734377|AXP990213)");
	}
}
