/*
 * Copyright 2017-2022 Tim Segall
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
import java.security.SecureRandom;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.cobber.fta.core.Utils;
import com.cobber.fta.token.TokenStreams;

public class TestTokenStreams {
	private static final SecureRandom random = new SecureRandom();
	private static String ALPHA = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static String NUMERIC = "0123456789";
	private static int MAX_STREAMS = 1000;

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void singleAlphaConstantLength() throws IOException {
		final TokenStreams tokenStreams = new TokenStreams(MAX_STREAMS);

		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 9; j++)
				b.append(ALPHA.charAt(random.nextInt(ALPHA.length())));
			tokenStreams.track(b.toString(), 1);
			b.setLength(0);
		}

		Assert.assertEquals(tokenStreams.getRegExp(false), "\\p{IsAlphabetic}{9}");
		Assert.assertEquals(tokenStreams.getRegExp(true), "\\p{IsAlphabetic}{9}");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void zip() throws IOException {
		final TokenStreams tokenStreams = new TokenStreams(MAX_STREAMS);

		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < 1000; i++) {
			for (int j = 0; j < 5; j++)
				b.append(NUMERIC.charAt(random.nextInt(NUMERIC.length())));
			if (i % 2 == 0) {
				b.append('-');
				for (int j = 0; j < 4; j++)
					b.append(NUMERIC.charAt(random.nextInt(NUMERIC.length())));
			}
			tokenStreams.track(b.toString(), 1);
			b.setLength(0);
		}

		Assert.assertEquals(tokenStreams.getRegExp(false), "\\d{5}(-\\d{4})?");
		Assert.assertEquals(tokenStreams.getRegExp(true), "\\d{5}(-\\d{4})?");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void doubleAlphaConstantLength() throws IOException {
		final TokenStreams tokenStreams = new TokenStreams(MAX_STREAMS);

		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 9; j++)
				b.append(ALPHA.charAt(random.nextInt(ALPHA.length())));
			tokenStreams.track(b.toString(), 1);
			b.setLength(0);
		}

		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 7; j++)
				b.append(ALPHA.charAt(random.nextInt(ALPHA.length())));
			tokenStreams.track(b.toString(), 1);
			b.setLength(0);
		}

		Assert.assertEquals(tokenStreams.getRegExp(false), "\\p{IsAlphabetic}{7}|\\p{IsAlphabetic}{9}");
		Assert.assertEquals(tokenStreams.getRegExp(true), "\\p{IsAlphabetic}{7}|\\p{IsAlphabetic}{9}");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void multiAlphaConstantLength() throws IOException {
		final TokenStreams tokenStreams = new TokenStreams(MAX_STREAMS);

		final StringBuilder b = new StringBuilder();
		for (int j = 4; j < 9; j++) {
			for (int i = 0; i < 100; i++) {
				for (int l = 0; l < j; l++)
					b.append(ALPHA.charAt(random.nextInt(ALPHA.length())));
				tokenStreams.track(b.toString(), 1);
				b.setLength(0);
			}
		}

		Assert.assertEquals(tokenStreams.getRegExp(false), "\\p{IsAlphabetic}{4,8}");
		Assert.assertEquals(tokenStreams.getRegExp(true), "\\p{IsAlphabetic}{4,8}");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void singleNumericConstantLength() throws IOException {
		final TokenStreams tokenStreams = new TokenStreams(MAX_STREAMS);

		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 9; j++)
				b.append(NUMERIC.charAt(random.nextInt(NUMERIC.length())));
			tokenStreams.track(b.toString(), 1);
			b.setLength(0);
		}

		Assert.assertEquals(tokenStreams.getRegExp(false), "\\d{9}");
		Assert.assertEquals(tokenStreams.getRegExp(true), "\\d{9}");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void doubleNumericConstantLength() throws IOException {
		final TokenStreams tokenStreams = new TokenStreams(MAX_STREAMS);

		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 9; j++)
				b.append(NUMERIC.charAt(random.nextInt(NUMERIC.length())));
			tokenStreams.track(b.toString(), 1);
			b.setLength(0);
		}

		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 7; j++)
				b.append(NUMERIC.charAt(random.nextInt(NUMERIC.length())));
			tokenStreams.track(b.toString(), 1);
			b.setLength(0);
		}

		Assert.assertEquals(tokenStreams.getRegExp(false), "\\d{7}|\\d{9}");
		Assert.assertEquals(tokenStreams.getRegExp(true), "\\d{7}|\\d{9}");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void adjacentNumericConstantLength() throws IOException {
		final TokenStreams tokenStreams = new TokenStreams(MAX_STREAMS);

		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 8; j++)
				b.append(NUMERIC.charAt(random.nextInt(NUMERIC.length())));
			tokenStreams.track(b.toString(), 1);
			b.setLength(0);
		}

		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 7; j++)
				b.append(NUMERIC.charAt(random.nextInt(NUMERIC.length())));
			tokenStreams.track(b.toString(), 1);
			b.setLength(0);
		}

		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 6; j++)
				b.append(NUMERIC.charAt(random.nextInt(NUMERIC.length())));
			tokenStreams.track(b.toString(), 1);
			b.setLength(0);
		}

		Assert.assertEquals(tokenStreams.getRegExp(false), "\\d{6,8}");
		Assert.assertEquals(tokenStreams.getRegExp(true), "\\d{6,8}");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void testGUIDs() throws IOException {
		final TokenStreams tokenStreams = new TokenStreams(MAX_STREAMS);
		final String[] inputs = new String[] {
				"DAA3EDDE-5BCF-4D2A-8FB0-E120089343AF",
				"B0613BE8-88AF-4591-A9A0-059F80413212",
				"063BB913-7287-4A8A-B3DF-41EAA0EABF49",
				"B6011DC1-C4A3-4130-AD42-C3EA2BA35F8B",
				"327B2624-2467-4461-8CA3-2DCB30D06683",
				"BDC94786-4016-4C7A-85F7-A7558425FA26",
				"0525CA73-9A48-497A-AC2D-2596BFE66FF7",
				"88BD42BA-B4F2-4E9E-8BD3-6846F6692E44",
				"1456E784-D404-4864-BBD3-691988220732",
				"FF2B0C44-2277-4EB1-BB25-32CF23181672",
				"929945CC-E4AA-4FEA-BFD6-43B774C9FB05",
				"BC2D3965-24A5-4CC7-986A-99B869925ACD",
				"7C9C9A6C-0A38-41B6-A999-A9A4218D43FA",
				"3324F2BF-9CC6-446A-A02D-DDE2F2ECF31F",
				"F17AA339-5DCE-4318-9B1C-C95255D4C5CC",
				"D67F9D81-DBE7-4214-849F-41B937C628AB",
				"9892D51B-C490-4B6E-8DF0-B032BAAB0476",
				"6CBD3302-F067-4378-8955-CD57EA5E83EB",
				"BEDFFAF8-9E35-4155-A337-7981BA349E7B",
				"37285247-D431-4381-AC5F-7C3136E276C2",
				"6D490537-AA7B-45C5-BEDB-8572EBDEFD15",
				"51e55fd6-74ca-4b1d-b5fd-d210209e3fc4"
		};

		for (String input : inputs)
			tokenStreams.track(input, 1);

		Assert.assertEquals(tokenStreams.getRegExp(false), ".+");
		Assert.assertEquals(tokenStreams.getRegExp(true), ".+");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void testBlank() throws IOException {
		final TokenStreams tokenStreams = new TokenStreams(MAX_STREAMS);

		for (int i = 0; i < 100; i++)
			tokenStreams.track("", 1);

		Assert.assertEquals(tokenStreams.getRegExp(false), "");
		Assert.assertEquals(tokenStreams.getRegExp(true), "");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void testFitted() throws IOException {
		final TokenStreams tokenStreams = new TokenStreams(MAX_STREAMS);
		final String[] inputs = new String[] {
				"ICD9-059F80413212",
				"ICD9-41EAA0EABF49",
				"ICD9-C3EA2BA35F8B",
				"ICD9-2DCB30D06683",
				"ICD9-A7558425FA26",
				"ICD9-2596BFE66FF7",
				"ICD9-6846F6692E44",
				"ICD9-691988220732",
				"ICD9-32CF23181672",
				"ICD9-43B774C9FB05",
				"ICD9-99B869925ACD",
				"ICD9-A9A4218D43FA",
				"ICD9-DDE2F2ECF31F",
				"ICD9-C95255D4C5CC",
				"ICD9-41B937C628AB",
				"ICD9-B032BAAB0476",
				"ICD9-CD57EA5E83EB",
				"ICD9-7981BA349E7B",
				"ICD9-7C3136E276C2",
				"ICD9-8572EBDEFD15",
				"ICD9-d210209e3fc4"
		};

		for (String input : inputs)
			tokenStreams.track(input, 1);

		Assert.assertEquals(tokenStreams.getRegExp(false), "\\p{IsAlphabetic}{3}\\d-[\\p{IsAlphabetic}\\d]{12}");
		Assert.assertEquals(tokenStreams.getRegExp(true), "ICD9-[\\p{IsAlphabetic}\\d]{12}");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void testOverflow() throws IOException {
		final TokenStreams tokenStreams = new TokenStreams(20);

		for (int i = 0; i <= 30; i++)
			tokenStreams.track(Utils.repeat('a', i), 1);

		Assert.assertEquals(tokenStreams.getRegExp(false), ".+");
		Assert.assertEquals(tokenStreams.getRegExp(true), ".+");
		Assert.assertEquals(tokenStreams.getShapes().size(), 0);
	}
}
