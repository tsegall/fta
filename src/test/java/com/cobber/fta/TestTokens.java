package com.cobber.fta;

import java.io.IOException;
import java.util.Map;
import java.util.Random;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.token.CharClassToken;
import com.cobber.fta.token.Token;
import com.cobber.fta.token.TokenStream;
import com.cobber.fta.token.TokenStreams;

public class TestTokens {
	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void tooLong() throws IOException, FTAException {
		final String input = "0123456789012345678901234567890";

		TokenStream ts = new TokenStream(input, 1);
		Assert.assertEquals(ts.getKey(), "ANY");
		Assert.assertEquals(ts.getOccurrences(), 1);
		Assert.assertEquals(ts.getRegExp(false), ".+");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void oneDigit() throws IOException, FTAException {
		final String input = "0";

		TokenStream ts = new TokenStream(input, 1);
		Assert.assertEquals(ts.getKey(), "9");
		Assert.assertEquals(ts.getCompressedKey(), "9");
		Assert.assertEquals(ts.getRegExp(false), "\\d");
		Assert.assertEquals(ts.getRegExp(true), "0");
		Assert.assertEquals(ts.getOccurrences(), 1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void twoDigits() throws IOException, FTAException {
		final String input = "01";

		TokenStream ts = new TokenStream(input, 1);
		Assert.assertEquals(ts.getKey(), "99");
		Assert.assertEquals(ts.getCompressedKey(), "9");
		Assert.assertEquals(ts.getRegExp(false), "\\d{2}");
		Assert.assertEquals(ts.getRegExp(true), "01");
		Assert.assertEquals(ts.getOccurrences(), 1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void justDigits() throws IOException, FTAException {
		final String input = "0123456789";

		TokenStream ts = new TokenStream(input, 1);
		Assert.assertEquals(ts.getKey(), "9999999999");
		Assert.assertEquals(ts.getCompressedKey(), "9");
		Assert.assertEquals(ts.getRegExp(false), "\\d{10}");
		Assert.assertEquals(ts.getRegExp(true), "0123456789");
		Assert.assertEquals(ts.getOccurrences(), 1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void simpleUpper() throws IOException, FTAException {
		final String input = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

		TokenStream ts = new TokenStream(input, 1);
		Assert.assertEquals(ts.getKey(), "XXXXXXXXXXXXXXXXXXXXXXXXXX");
		Assert.assertEquals(ts.getCompressedKey(), "X");

		Assert.assertEquals(ts.getRegExp(false), "\\p{IsAlphabetic}{26}");
		Assert.assertEquals(ts.getRegExp(true), "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void simpleLower() throws IOException, FTAException {
		final String input = "abcdefghijklmnopqrstuvwxyz";

		TokenStream ts = new TokenStream(input, 1);
		Assert.assertEquals(ts.getKey(), "XXXXXXXXXXXXXXXXXXXXXXXXXX");
		Assert.assertEquals(ts.getRegExp(false), "\\p{IsAlphabetic}{26}");
		Assert.assertEquals(ts.getRegExp(true), "abcdefghijklmnopqrstuvwxyz");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void nonASCII() throws IOException, FTAException {
		final String input = "München";

		TokenStream ts = new TokenStream(input, 1);
		Assert.assertEquals(ts.getKey(), "XXXXXXX");
		Assert.assertEquals(ts.getCompressedKey(), "X");
		Assert.assertEquals(ts.getRegExp(false), "\\p{IsAlphabetic}{7}");
		Assert.assertEquals(ts.getRegExp(true), "München");
		for (Token token : ts.getTokens()) {
			if (token instanceof CharClassToken) {
				CharClassToken ccToken = (CharClassToken)token;
				Assert.assertEquals(ccToken.getRanges().size(), 1);
			}
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void randomStuff() throws IOException, FTAException {
		final String input = "+-=?#$";

		TokenStream ts = new TokenStream(input, 1);
		Assert.assertEquals(ts.getKey(), "+-=?#$");
		Assert.assertEquals(ts.getCompressedKey(), "+-=?#$");
		Assert.assertEquals(ts.getRegExp(false), "\\+-=?#\\$");
		Assert.assertEquals(ts.getRegExp(true), "\\+-=?#\\$");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void testMerge() throws IOException, FTAException {
		TokenStream ts1 = new TokenStream("16789", 1);
		TokenStream ts2 = new TokenStream("01338", 2);
		TokenStream ts3 = new TokenStream("22457", 3);

		ts1.merge(ts2).merge(ts3);
		Assert.assertEquals(ts1.getOccurrences(), 6);
		Assert.assertEquals(ts1.getRegExp(false), "\\d{5}");
		Assert.assertEquals(ts1.getRegExp(true), "\\d{5}");

		Token[] tokens = ts1.getTokens();
		for (int i = 0; i < tokens.length; i++) {
			CharClassToken ccToken = (CharClassToken)tokens[i];
			for (CharClassToken.Range range : ccToken.getRanges()) {
				if (i == 0) {
					Assert.assertEquals(ccToken.getRanges().size(), 1);
					Assert.assertEquals(range.getMin(), '0');
					Assert.assertEquals(range.getMax(), '2');
				}
				else if (i == 1)
					Assert.assertEquals(ccToken.getRanges().size(), 2);
				else if (i == 2)
					Assert.assertEquals(ccToken.getRanges().size(), 2);
				else if (i == 3)
					Assert.assertEquals(ccToken.getRanges().size(), 3);
				else if (i == 4) {
					Assert.assertEquals(ccToken.getRanges().size(), 1);
					Assert.assertEquals(range.getMin(), '7');
					Assert.assertEquals(range.getMax(), '9');
				}
			}
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void testWords() throws IOException, FTAException {
		TokenStreams ts = new TokenStreams(10);
		ts.track("uvuck qdtxnz pkaow nu q fdvhuykn okb ar kjdw upfl cxchmna nilb fvtuoqhy", 1);
		ts.track("rtifwhajt ozcjr qmje x mrtklqjhn pfqzdcosa xbhqerf odl xwcxsqua scyptz qv otvif v h hlqfbjl", 2);
		ts.track("mxewys ramyc mhfxuwnt i ypesjme zoxayy", 3);
		ts.track("Hello", 3);

		Assert.assertEquals(ts.getRegExp(false), ".+");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void testNumeric() throws IOException, FTAException {
		TokenStreams ts = new TokenStreams(10);
		ts.track("09:09:08:07:04:05", 1);
		ts.track("09:09:08:07:04:60", 1);
		ts.track("89:39:33:17:04:44", 1);
		ts.track("09:19:88:07:04:00", 1);
		ts.track("09:04:08:11:01:22", 1);

		Assert.assertEquals(ts.size(), 1);
		TokenStream tokenStream = ts.getStreams().values().iterator().next();
		Assert.assertTrue(tokenStream.matches("[0-9][0-9]:[0-9][0-9]:[0-9][0-9]:[0-9][0-9]:[0-9][0-9]:[0-9][0-9]"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void testNotAllNumeric() throws IOException, FTAException {
		TokenStreams ts = new TokenStreams(10);
		ts.track("09:09:08:07:04:05", 1);
		ts.track("09:09:08:07:04:60", 1);
		ts.track("89:39:3A:17:04:44", 1);
		ts.track("09:19:88:07:04:00", 1);
		ts.track("09:04:08:11:01:22", 1);

		Assert.assertEquals(ts.size(), 2);

		int matches = 0;

		for (Map.Entry<String, TokenStream> entry : ts.getStreams().entrySet()) {
			if (entry.getValue().matches("[0-9][0-9]:[0-9][0-9]:[0-9][0-9]:[0-9][0-9]:[0-9][0-9]:[0-9][0-9]"))
				matches++;
		}
		Assert.assertEquals(matches, 1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void testHex() throws IOException, FTAException {
		TokenStreams ts = new TokenStreams(10);
		ts.track("09:09:08:07:04:05", 1);
		ts.track("00:a9:b8:07:04:60", 1);
		ts.track("00:39:33:17:04:44", 1);
		ts.track("00:19:8c:0F:04:00", 1);
		ts.track("00:04:08:1E:01:DD", 1);

		for (Map.Entry<String, TokenStream> entry : ts.getStreams().entrySet())
			Assert.assertTrue(entry.getValue().matches("[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}"));

		Assert.assertTrue(ts.matches("[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void testNonHex() throws IOException, FTAException {
		TokenStreams ts = new TokenStreams(10);
		ts.track("09:09:08:07:04:05", 1);
		ts.track("00:a9:b8:07:04:60", 1);
		ts.track("00:39:33:17:04:44", 1);
		ts.track("00:19:8c:0F:04:00", 1);
		ts.track("00:04:08:1E:01:GG", 1);

		Assert.assertFalse(ts.matches("[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void testShapes() throws IOException, FTAException {
		final String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String[] prefixes = new String[] { "INCA-", "ICD9-", "ICD10-" };
		Random r = new Random();
		TokenStreams tokenStreams = new TokenStreams(10);

		for (String prefix : prefixes) {
			for (int i = 1000; i < 2000; i++) {
				String sample = i%2 == 0 ? prefix + String.valueOf(i) : String.valueOf(alpha.charAt(r.nextInt(alpha.length())));

				tokenStreams.track(sample, 1);
			}
		}

		Assert.assertEquals(tokenStreams.size(), 4);

		for (Map.Entry<String, TokenStream> entry : tokenStreams.getStreams().entrySet()) {
			if ("XXX9-9999".equals(entry.getKey())) {
				Assert.assertEquals(entry.getValue().getRegExp(false), "\\p{IsAlphabetic}{3}\\d-\\d{4}");
				Assert.assertEquals(entry.getValue().getRegExp(true), "ICD9-\\d{4}");
				Assert.assertEquals(entry.getValue().getOccurrences(), 500);
			}
			else if ("XXX99-9999".equals(entry.getKey())) {
				Assert.assertEquals(entry.getValue().getRegExp(false), "\\p{IsAlphabetic}{3}\\d{2}-\\d{4}");
				Assert.assertEquals(entry.getValue().getRegExp(true), "ICD10-\\d{4}");
				Assert.assertEquals(entry.getValue().getOccurrences(), 500);
			}
			else if ("XXXX-9999".equals(entry.getKey())) {
				Assert.assertEquals(entry.getValue().getRegExp(false), "\\p{IsAlphabetic}{4}-\\d{4}");
				Assert.assertEquals(entry.getValue().getRegExp(true), "INCA-\\d{4}");
				Assert.assertEquals(entry.getValue().getOccurrences(), 500);
			}
			else if ("X".equals(entry.getKey())) {
				Assert.assertEquals(entry.getValue().getRegExp(false), "\\p{IsAlphabetic}");
				Assert.assertEquals(entry.getValue().getRegExp(true), "\\p{IsAlphabetic}");
				Assert.assertEquals(entry.getValue().getOccurrences(), 1500);
			}
		}
	}
}
