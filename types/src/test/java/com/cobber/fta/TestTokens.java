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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.Random;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.token.CharClassToken;
import com.cobber.fta.token.Range;
import com.cobber.fta.token.Token;
import com.cobber.fta.token.TokenStream;
import com.cobber.fta.token.TokenStreams;

public class TestTokens {
	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void tooLong() throws IOException, FTAException {
		final String input = "012345678901234567890123456789001234567890123456789012345678901234";

		final TokenStream ts = new TokenStream(input, 1);
		assertEquals(ts.getKey(), "ANY");
		assertEquals(ts.getOccurrences(), 1);
		assertEquals(ts.getRegExp(false), ".+");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void oneDigit() throws IOException, FTAException {
		final String input = "0";

		final TokenStream ts = new TokenStream(input, 1);
		assertEquals(ts.getKey(), "9");
		assertEquals(ts.getCompressedKey(), "9");
		assertEquals(ts.getRegExp(false), "\\d");
		assertEquals(ts.getRegExp(true), "0");
		assertEquals(ts.getOccurrences(), 1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void twoDigits() throws IOException, FTAException {
		final String input = "01";

		final TokenStream ts = new TokenStream(input, 1);
		assertEquals(ts.getKey(), "99");
		assertEquals(ts.getCompressedKey(), "9");
		assertEquals(ts.getRegExp(false), "\\d{2}");
		assertEquals(ts.getRegExp(true), "01");
		assertEquals(ts.getOccurrences(), 1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void justDigits() throws IOException, FTAException {
		final String input = "0123456789";

		final TokenStream ts = new TokenStream(input, 1);
		assertEquals(ts.getKey(), "9999999999");
		assertEquals(ts.getCompressedKey(), "9");
		assertEquals(ts.getRegExp(false), "\\d{10}");
		assertEquals(ts.getRegExp(true), "0123456789");
		assertEquals(ts.getOccurrences(), 1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void simpleUpper() throws IOException, FTAException {
		final String input = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

		final TokenStream ts = new TokenStream(input, 1);
		assertEquals(ts.getKey(), "XXXXXXXXXXXXXXXXXXXXXXXXXX");
		assertEquals(ts.getCompressedKey(), "X");

		assertEquals(ts.getRegExp(false), "\\p{IsAlphabetic}{26}");
		assertEquals(ts.getRegExp(true), "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void simpleLower() throws IOException, FTAException {
		final String input = "abcdefghijklmnopqrstuvwxyz";

		final TokenStream ts = new TokenStream(input, 1);
		assertEquals(ts.getKey(), "XXXXXXXXXXXXXXXXXXXXXXXXXX");
		assertEquals(ts.getRegExp(false), "\\p{IsAlphabetic}{26}");
		assertEquals(ts.getRegExp(true), "abcdefghijklmnopqrstuvwxyz");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void nonASCII() throws IOException, FTAException {
		final String input = "München";

		final TokenStream ts = new TokenStream(input, 1);
		assertEquals(ts.getKey(), "XXXXXXX");
		assertEquals(ts.getCompressedKey(), "X");
		assertEquals(ts.getRegExp(false), "\\p{IsAlphabetic}{7}");
		assertEquals(ts.getRegExp(true), "München");
		for (final Token token : ts.getTokens()) {
			if (token instanceof CharClassToken) {
				final CharClassToken ccToken = (CharClassToken)token;
				assertEquals(ccToken.getRanges().size(), 1);
			}
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void randomStuff() throws IOException, FTAException {
		final String input = "+-=?#$";

		final TokenStream ts = new TokenStream(input, 1);
		assertEquals(ts.getKey(), "+-=?#$");
		assertEquals(ts.getCompressedKey(), "+-=?#$");
		assertEquals(ts.getRegExp(false), "\\+-=?#\\$");
		assertEquals(ts.getRegExp(true), "\\+-=?#\\$");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void testMerge() throws IOException, FTAException {
		final TokenStreams ts = new TokenStreams(10);
		ts.track("16789", 1);
		ts.track("01338", 2);
		ts.track("22457", 3);

		assertEquals(ts.getSamples(), 6);
		assertEquals(ts.getRegExp(false), "\\d{5}");
		final String fittedAnswer = "[0-2][1-26][3-47][358][7-9]";
		assertEquals(ts.getRegExp(true), fittedAnswer);

		assertTrue("16789".matches(fittedAnswer));
		assertTrue("01338".matches(fittedAnswer));
		assertTrue("22457".matches(fittedAnswer));

		final Token[] tokens = ts.getBest().getTokens();
		for (int i = 0; i < tokens.length; i++) {
			final CharClassToken ccToken = (CharClassToken)tokens[i];
			for (final Range range : ccToken.getRanges()) {
				if (i == 0) {
					assertEquals(ccToken.getRanges().size(), 1);
					assertEquals(range.getMin(), '0');
					assertEquals(range.getMax(), '2');
				}
				else if (i == 1)
					assertEquals(ccToken.getRanges().size(), 2);
				else if (i == 2)
					assertEquals(ccToken.getRanges().size(), 2);
				else if (i == 3)
					assertEquals(ccToken.getRanges().size(), 3);
				else if (i == 4) {
					assertEquals(ccToken.getRanges().size(), 1);
					assertEquals(range.getMin(), '7');
					assertEquals(range.getMax(), '9');
				}
			}
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void testWords() throws IOException, FTAException {
		final TokenStreams ts = new TokenStreams(10);
		ts.track("uvuck qdtxnz pkaow nu q fdvhuykn okb ar kjdw upfl cxchmna nilb fvtuoqhy", 1);
		ts.track("rtifwhajt ozcjr qmje x mrtklqjhn pfqzdcosa xbhqerf odl xwcxsqua scyptz qv otvif v h hlqfbjl", 2);
		ts.track("mxewys ramyc mhfxuwnt i ypesjme zoxayy", 3);
		ts.track("Hello", 3);

		assertEquals(ts.getRegExp(false), ".+");
		assertEquals(ts.getRegExp(true), ".+");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void testNumeric() throws IOException, FTAException {
		final TokenStreams ts = new TokenStreams(10);
		ts.track("09:09:08:07:04:05", 1);
		ts.track("09:09:08:07:04:60", 1);
		ts.track("89:39:33:17:04:44", 1);
		ts.track("09:19:88:07:04:00", 1);
		ts.track("09:04:08:11:01:22", 1);

		assertEquals(ts.size(), 1);
		final TokenStream tokenStream = ts.getStreams().values().iterator().next();
		assertTrue(tokenStream.matches("[0-9][0-9]:[0-9][0-9]:[0-9][0-9]:[0-9][0-9]:[0-9][0-9]:[0-9][0-9]"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void testNotAllNumeric() throws IOException, FTAException {
		final TokenStreams ts = new TokenStreams(10);
		ts.track("09:09:08:07:04:05", 1);
		ts.track("09:09:08:07:04:60", 1);
		ts.track("89:39:3A:17:04:44", 1);
		ts.track("09:19:88:07:04:00", 1);
		ts.track("09:04:08:11:01:22", 1);

		assertEquals(ts.size(), 2);

		int matches = 0;

		for (final Map.Entry<String, TokenStream> entry : ts.getStreams().entrySet()) {
			if (entry.getValue().matches("[0-9][0-9]:[0-9][0-9]:[0-9][0-9]:[0-9][0-9]:[0-9][0-9]:[0-9][0-9]"))
				matches++;
		}
		assertEquals(matches, 1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void testHex() throws IOException, FTAException {
		final TokenStreams ts = new TokenStreams(10);
		ts.track("09:09:08:07:04:05", 1);
		ts.track("00:a9:b8:07:04:60", 1);
		ts.track("00:39:33:17:04:44", 1);
		ts.track("00:19:8c:0F:04:00", 1);
		ts.track("00:04:08:1E:01:DD", 1);

		for (final Map.Entry<String, TokenStream> entry : ts.getStreams().entrySet())
			assertTrue(entry.getValue().matches("[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}"));

		assertEquals(ts.matches("[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}", 100), 5);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void testNonHex() throws IOException, FTAException {
		final TokenStreams ts = new TokenStreams(10);
		ts.track("09:09:08:07:04:05", 1);
		ts.track("00:a9:b8:07:04:60", 1);
		ts.track("00:39:33:17:04:44", 1);
		ts.track("00:19:8c:0F:04:00", 1);
		ts.track("00:04:08:1E:01:GG", 1);

		assertEquals(ts.matches("[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}", 100), 0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void testShapes() throws IOException, FTAException {
		final String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		final String[] prefixes = { "INCA-", "ICD9-", "ICD10-" };
		final Random r = new Random();
		final TokenStreams tokenStreams = new TokenStreams(10);

		for (final String prefix : prefixes) {
			for (int i = 1000; i < 2000; i++) {
				final String sample = i%2 == 0 ? prefix + String.valueOf(i) : String.valueOf(alpha.charAt(r.nextInt(alpha.length())));

				tokenStreams.track(sample, 1);
			}
		}

		assertEquals(tokenStreams.size(), 4);

		for (final Map.Entry<String, TokenStream> entry : tokenStreams.getStreams().entrySet()) {
			if ("XXX9-9999".equals(entry.getKey())) {
				assertEquals(entry.getValue().getRegExp(false), "\\p{IsAlphabetic}{3}\\d-\\d{4}");
				assertEquals(entry.getValue().getRegExp(true), "ICD9-1[0-9]{2}[02468]");
				assertEquals(entry.getValue().getOccurrences(), 500);
			}
			else if ("XXX99-9999".equals(entry.getKey())) {
				assertEquals(entry.getValue().getRegExp(false), "\\p{IsAlphabetic}{3}\\d{2}-\\d{4}");
				assertEquals(entry.getValue().getRegExp(true), "ICD10-1[0-9]{2}[02468]");
				assertEquals(entry.getValue().getOccurrences(), 500);
			}
			else if ("XXXX-9999".equals(entry.getKey())) {
				assertEquals(entry.getValue().getRegExp(false), "\\p{IsAlphabetic}{4}-\\d{4}");
				assertEquals(entry.getValue().getRegExp(true), "INCA-1[0-9]{2}[02468]");
				assertEquals(entry.getValue().getOccurrences(), 500);
			}
			else if ("X".equals(entry.getKey())) {
				assertEquals(entry.getValue().getRegExp(false), "\\p{IsAlphabetic}");
				assertEquals(entry.getValue().getRegExp(true), "\\p{IsAlphabetic}");
				assertEquals(entry.getValue().getOccurrences(), 1500);
			}
		}
	}
}
