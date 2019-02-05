/*
 * Copyright 2017 Tim Segall
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
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

public class RandomTests {
	@Test
	public void inadequateData() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "47|89|90|91".split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getMinValue(), "47");
		Assert.assertEquals(result.getMaxValue(), "91");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void noData() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "[NULL]");
		Assert.assertEquals(result.getConfidence(), 0.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "NULL");
		Assert.assertEquals(result.dump(true), "TextAnalysisResult [name=anonymous, matchCount=0, sampleCount=0, nullCount=0, blankCount=0, regexp=\"[NULL]\", confidence=0.0, type=String(NULL), min=null, max=null, minLength=0, maxLength=0, sum=null, cardinality=0]");
	}

	@Test
	public void rubbish() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "47|hello|hello,world|=====47=====|aaaa|0|12|b,b,b,b390|4083|ddd ddd|90|-------|+++++|42987|8901".split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.freezeANY(1, 12, 1, 12, result.getLeadingWhiteSpace(), result.getTrailingWhiteSpace(), result.getMultiline()));
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getMinValue(), "+++++");
		Assert.assertEquals(result.getMaxValue(), "hello,world");
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxLength(), 12);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void sum50() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		int locked = -1;
		final int COUNT = 50;
		int sum = 0;

		for (int i = 10000; i < 10000 + COUNT; i++) {
			sum += i;
			if (analysis.train(String.valueOf(i)) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, -1);
		Assert.assertEquals(result.getSampleCount(), COUNT);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{5}");
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getMinValue(), "10000");
		Assert.assertEquals(result.getMaxValue(), "10049");
		Assert.assertEquals(result.getTypeQualifier(), "US_ZIP5");
		Assert.assertEquals(result.getConfidence(), 0.96);
		Assert.assertEquals(result.getSum(), String.valueOf(sum));

		for (int i = 10000; i < 10000 + COUNT; i++) {
			Assert.assertTrue(String.valueOf(i).matches(result.getRegExp()));
		}
	}

	@Test
	public void limitedData() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "12|4|5|".split("\\|");
		final int pre = 3;
		final int post = 10;

		for (int i = 0; i < pre; i++)
			analysis.train("");
		for (int i = 0; i < inputs.length; i++) {
			analysis.train(inputs[i]);
		}
		for (int i = 0; i < post; i++)
			analysis.train("");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), pre + inputs.length + post);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{1,2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void manyNulls() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int iterations = 50;

		for (int i = 0; i < iterations; i++) {
			analysis.train(null);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), iterations);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), 0);
		Assert.assertEquals(result.getNullCount(), iterations);
		Assert.assertEquals(result.getMinLength(), 0);
		Assert.assertEquals(result.getMaxLength(), 0);
		Assert.assertEquals(result.getMinValue(), null);
		Assert.assertEquals(result.getMaxValue(), null);
		Assert.assertEquals(result.getNullCount(), iterations);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getRegExp(), "[NULL]");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "NULL");
	}

	@Test
	public void manyBlanks() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int iterations = 50;

		analysis.train("");
		for (int i = 0; i < iterations; i++) {
			analysis.train(" ");
			analysis.train("  ");
			analysis.train("      ");
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), 3 * iterations + 1);
		Assert.assertEquals(result.getMaxLength(), 6);
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxValue(), "      ");
		Assert.assertEquals(result.getMinValue(), " ");
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getMatchCount(), 0);
		Assert.assertEquals(result.getBlankCount(), 3 * iterations + 1);
		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "BLANK");

		Assert.assertTrue("".matches(result.getRegExp()));
		Assert.assertTrue(" ".matches(result.getRegExp()));
		Assert.assertTrue("  ".matches(result.getRegExp()));
		Assert.assertTrue("      ".matches(result.getRegExp()));
	}

	@Test
	public void sameBlanks() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int iterations = 50;

		for (int i = 0; i < iterations; i++) {
			analysis.train("      ");
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), iterations);
		Assert.assertEquals(result.getMaxLength(), 6);
		Assert.assertEquals(result.getMinLength(), 6);
		Assert.assertEquals(result.getMaxValue(), "      ");
		Assert.assertEquals(result.getMinValue(), "      ");
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getMatchCount(), 0);
		Assert.assertEquals(result.getBlankCount(), iterations);
		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "BLANK");

		Assert.assertTrue("      ".matches(result.getRegExp()));
	}

	@Test
	public void justEmpty() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int iterations = 50;

		for (int i = 0; i < iterations; i++) {
			analysis.train("");
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), iterations);
		Assert.assertEquals(result.getMaxLength(), 0);
		Assert.assertEquals(result.getMinLength(), 0);
		Assert.assertEquals(result.getMaxValue(), "");
		Assert.assertEquals(result.getMinValue(), "");
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getMatchCount(), 0);
		Assert.assertEquals(result.getBlankCount(), iterations);
		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "BLANK");

		Assert.assertTrue("".matches(result.getRegExp()));
	}


	@Test
	public void whiteSpace() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("field,value");
		final String input = "| |  |   |    |     |      |       |        |         |";
		final String inputs[] = input.split("\\|");

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++)
			analysis.train(inputs[i]);

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++)
			analysis.train(inputs[i]);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "BLANKORNULL");
		Assert.assertEquals(result.getSampleCount(), 22);
		Assert.assertEquals(result.getMatchCount(), 0);
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxLength(), 9);
		Assert.assertEquals(result.getBlankCount(), 20);
		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}

	}

	@Test
	public void employeeNumber() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("employeeNumber");
		final String input = "||||||||||||||||||||" +
				"||||||||||||48|72|242|242|242|335|354|355|355|" +
				"397|460|567|616|616|70|70|865|1023|1023|1023|1023|1023|1023|1023|1023|1161|1161|1161|1161|1161|" +
				"1260|1273|1273|1273|136|136|136|136|136|136|136|136|136|1422|1422|1422|1422|1422|1422|1548|1652|" +
				"F9442559|F9442774|F9442774|F9442856|F9442856|F9442856|F9442856|F9442856|F9442965|F9442965|" +
				"F9442965|F9442968|F9442999|F9442999|FN228446|FN241214|FN241227|FN246235|FN246286|FN246288|" +
				"FN270164|FN270168|FN273815|FN273967|FN295633|FN295655|FN295657|FN295659|FN295684|FN295688|" +
				"FN295842|FN9441020|FN9441048|FN9441064|FN9441082|FN9441138|FN9441189|FN9441244|FN9441246|FN9441248|" +
				"FN9441330|FN9441334|FN9441383|FN9441501|FN9441505|FN9441516|FN9441529|FN9441680|FN9441695|FN9441804|";
		final String inputs[] = input.split("\\|");

		for (int i = 0; i < inputs.length; i++)
			analysis.train(inputs[i]);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getBlankCount());
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getMinLength(), 2);
		Assert.assertEquals(result.getMaxLength(), 9);
		Assert.assertEquals(result.getBlankCount(), 32);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHANUMERIC + "{2,9}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		int matchCount = 0;
		for (int i = 0; i < inputs.length; i++) {
			if (inputs[i].matches(result.getRegExp()))
				matchCount++;
		}
		Assert.assertEquals(matchCount, result.getMatchCount());
	}

	@Test
	public void testTrim() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = " Hello|  Hello| Hello |  world  |    Hello   |      Hi        |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi        |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi        |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi        |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi          |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi        |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi        ";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), inputs.length + result.getNullCount());
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 1);
		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*" + KnownPatterns.PATTERN_ALPHA + "{2,5}\\p{javaWhitespace}*");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void changeMind() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		int locked = -1;

		for (int i = 0; i < 2 * TextAnalyzer.SAMPLE_DEFAULT; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i;
		}

		for (char ch = 'a'; ch <= 'z'; ch++) {
			analysis.train(String.valueOf(ch));
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), 2 * TextAnalyzer.SAMPLE_DEFAULT + 26);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), 2 * TextAnalyzer.SAMPLE_DEFAULT + 26);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHANUMERIC + "{1,2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void changeMindMinMax() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		analysis.setThreshold(97);
		final String input =
				"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!!Volume!Volume!!!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!!Volume!Volume!!!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!!!!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!!!!Volume!Audio disc ; Volume!Volume!!!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Audio disc ; Volume!!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!!Volume!Volume!!!!!!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!!!Volume!Volume!Volume!Volume!Volume!!!Volume!Volume!Volume!" +
						"Volume!Volume!!!!!!!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"!Volume!Volume!!!Volume!Volume!Volume!!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!!Volume!Volume!Volume!!Volume!Volume!!Volume!Volume!!Volume!Volume!Volume!" +
						"Volume!!Volume!Volume!!Volume!!!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Online resource (ePub ebook)!Volume!Online resource (ePub ebook)!Volume!!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"!Volume!Volume!Volume!Volume!Volume!Volume!Volume!!Volume!Volume!Volume!Volume!Volume!!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Audio disc ; Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Online resource (ePub ebook)!Volume!Volume!Volume!" +
						"Volume!Volume!!!!Volume!Volume!Volume!Volume!Volume!Volume!Computer disc!Volume!Volume!Volume!Volume!Volume!" +
						"!Volume!Online resource!Volume!!Volume!!Volume!!Volume!!Volume!Online resource (PDF ebook ; ePub ebook)!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Online resource (ePub ebook)!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Audio disc ; Volume!" +
						"Volume!Volume!Volume!Volume!Volume!!Volume!Volume!!Volume!Volume!Volume!!Volume!Volume!!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!";
		final String inputs[] = input.split("\\!");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount() + result.getBlankCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.freezeANY(6, 40, 6, 40, result.getLeadingWhiteSpace(), result.getTrailingWhiteSpace(), result.getMultiline()));
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getMinValue(), "Audio disc ; Volume");
		Assert.assertEquals(result.getMaxValue(), "Volume");

		String regExp = result.getRegExp();
		for (int i = 0; i < inputs.length; i++) {
			if (inputs[i].length() == 0)
				continue;
			Assert.assertTrue(inputs[i].matches(regExp), inputs[i]);
		}
	}

	@Test
	public void testQualifierAlpha() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Alpha");
		final Random random = new Random(21456);
		final int STRING_LENGTH = 5;
		analysis.setLengthQualifier(false);
		String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

		final int start = 10000;
		final int end = 99999;

		int locked = -1;

		for (int i = start; i < end; i++) {
			StringBuilder sample = new StringBuilder(STRING_LENGTH);
			for (int j = 0; j < STRING_LENGTH; j++)
				sample.append(alphabet.charAt(random.nextInt(52)));
			if (analysis.train(sample.toString()) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "+");
	}

	@Test
	public void testQualifierAlphaNumeric() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("AlphaNumeric");
		analysis.setLengthQualifier(false);

		final int start = 10000;
		final int end = 99999;

		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train('A' + String.valueOf(i)) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHANUMERIC + "+");
	}

	@Test
	public void change2() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"Jan|Mar|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
				"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|Sep|Jan|Oct|Oct|Oct|" +
				"Jan|Mar|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
				"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|NA|Sep|Jan|Oct|Oct|Oct|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{2,3}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void trailingAM() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String inputs[] = new String[] {
				"02s500000023SQ3AAM", "02s5000000233ThAAI", "02s5000000238JRAAY", "02s500000023QCEAA2",
				"02s500000023QCFAA2", "02s500000023SKAAA2", "02s5000000233TgAAI", "02s500000023Sw9AAE",
				"02s500000023T0pAAE", "02s500000023U6FAAU", "02s500000023qQVAAY", "02s500000023qQWAAY",
				"02s500000023r2FAAQ", "02s500000023rFiAAI", "02s500000023x3qAAA", "02s50000002GgdtAAC",
				"02s50000002GgduAAC", "02s50000002GkKXAA0", "02s50000002GrukAAC", "02s50000002GrulAAC",
				"02s50000002GsLCAA0", "02s50000002HCnGAAW", "02s50000002HUaFAAW", "02s50000002HUaGAAW",
				"02s50000002HV82AAG", "02s50000002HjVvAAK", "02s50000002Hl4NAAS", "02s50000002HnXRAA0",
				"02s50000002Hq1sAAC", "02s50000002HrQPAA0", "02s50000002HrraAAC", "02s50000002HxoKAAS",
				"02s50000002I6lQAAS", "02s50000002I90MAAS", "02s50000002I93BAAS", "02s50000002I9CSAA0"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHANUMERIC + "{18}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}

	}

	@Test
	public void frenchName() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "Adrien|Alain|Albert|Alexandre|Alexis|André|Antoine|Arnaud|Arthur|Aurélien|" +
				"Baptiste|Benjamin|Benoît|Bernard|Bertrand|Bruno|Cédric|Charles|Christian|Christophe|" +
				"Claude|Clément|Cyril|Damien|Daniel|David|Denis|Didier|Dominique|Dylan|" +
				"Emmanuel|Éric|Étienne|Enzo|Fabien|Fabrice|Florent|Florian|Francis|Franck|" +
				"François|Frédéric|Gabriel|Gaétan|Georges|Gérard|Gilbert|Gilles|Grégory|Guillaume|" +
				"Guy|Henri|Hervé|Hugo|Jacques|Jean|";
		//Jean-Claude|Jean-François|Jean-Louis|Jean-Luc|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{3,10}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()), inputs[i]);
		}
	}

	@Test
	public void basicLengthValidationBlanks() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Spaces");
		final int iters = 30;
		Set<String> samples = new HashSet<String>();

		int locked = -1;

		StringBuilder sb = new StringBuilder("  ");

		for (int i = 0; i < iters; i++) {
			String s = sb.toString();
			samples.add(s);
			if (analysis.train(s) && locked == -1)
				locked = i;
			sb.append(' ');
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*");
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "BLANK");
		Assert.assertEquals(result.getSampleCount(), iters);
		Assert.assertEquals(result.getBlankCount(), iters);
		Assert.assertEquals(result.getCardinality(), 0);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), iters - result.getBlankCount());
		Assert.assertEquals(result.getMinLength(), 2);
		Assert.assertEquals(result.getMaxLength(), 2 + iters - 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (String sample : samples) {
			if (sample.trim().length() > 0)
				Assert.assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test
	public void basicLengthValidationString() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Spaces");
		final int iters = 30;
		Set<String> samples = new HashSet<String>();

		int locked = -1;

		StringBuilder sb = new StringBuilder("  ");

		for (int i = 0; i < iters; i++) {
			String s = sb.toString();
			samples.add(s);
			if (analysis.train(s) && locked == -1)
				locked = i;
			sb.append(' ');
		}
		analysis.train("          abc          ");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*" + KnownPatterns.PATTERN_ALPHA + "{3}\\p{javaWhitespace}*");
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), iters + 1);
		Assert.assertEquals(result.getBlankCount(), iters);
		Assert.assertEquals(result.getCardinality(), 1);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), 1);
		Assert.assertEquals(result.getMinLength(), 2);
		Assert.assertEquals(result.getMaxLength(), 2 + iters - 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (String sample : samples) {
			if (sample.trim().length() > 0)
				Assert.assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test
	public void variableSpacesFixedLength() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("variableSpacesFixedLength");
		final String input = "JMD     |JOD     |JPYP    |KESQ    |KGS     |KHR     |" +
				" AXN    | AOAZ   | B1D    | BIFD   | BSD    | BZD    | CZE    | CHF    |" +
				"  MzR   |  NIO   |  P2N   |  PLN   |  RWF   |  SDG   |  SHP   |  SLL   |" +
				"   SVQ  |   SYP  |   S33Z |   THB  |   TOP  |   TZS  |   UYE  |   VND  |" +
				"    CQP |    COU |    CRC |    CUC |    DJF |    EGP |    GLP |    GMD |" +
				"     APT|     CSU|     44S|    LFUA|XXXXXXXX|     PER|     NAR|     ZMW|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*" + KnownPatterns.freezeANY(3, 8, 3, 8, result.getLeadingWhiteSpace(), result.getTrailingWhiteSpace(), result.getMultiline()) + "\\p{javaWhitespace}*");
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getCardinality(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getMinLength(), 8);
		Assert.assertEquals(result.getMaxLength(), 8);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	final String alpha3 = "aaa|bbb|ccc|ddd|eee|fff|ggg|hhh|iii|jjj|" +
			"aaa|iii|sss|sss|sss|vvv|jjj|jjj|jjj|bbb|iii|uuu|bbb|bbb|vvv|mmm|uuu|fff|vvv|fff|" +
			"iii|ééé|iii|ggg|bbb|sss|mmm|uuu|sss|uuu|aaa|iii|sss|sss|sss|vvv|jjj|jjj|jjj|bbb|" +
			"iii|uuu|bbb|bbb|vvv|mmm|uuu|fff|vvv|fff|iii|bbb|iii|ggg|bbb|sss|mmm|uuu|sss|uuu|" +
			"aaa|iii|sss|sss|sss|vvv|jjj|jjj|jjj|ççç|iii|uuu|bbb|bbb|vvv|mmm|uuu|fff|vvv|fff|" +
			"iii|bbb|iii|ggg|bbb|sss|mmm|uuu|sss|uuu|aaa|iii|sss|sss|sss|vvv|jjj|jjj|jjj|bbb|" +
			"iii|uuu|bbb|bbb|vvv|mmm|uuu|fff|vvv|fff|iii|bbb|iii|ggg|bbb|sss|mmm|uuu|sss|uuu|" +
			"mmm|iii|uuu|fff|ggg|ggg|uuu|uuu|uuu|uuu|";
	final String number3 = "111|123|707|902|104|223|537|902|111|443|" +
			"121|234|738|902|002|431|679|093|124|557|886|631|235|569|002|149|963|271|905|501|" +
			"171|734|038|002|882|215|875|193|214|997|126|361|098|888|314|111|222|341|458|082|" +
			"371|334|438|442|782|715|775|893|314|337|326|781|984|349|534|888|654|841|158|182|" +
			"098|123|435|000|312|223|343|563|123|";

	@Test
	public void constantLength3_alpha() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String inputs[] = alpha3.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{3}");
		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void constantLength3_alnum() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String inputs[] = (alpha3 + number3).split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHANUMERIC + "{3}");
		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void constantLength3_numal() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String inputs[] = (number3 + alpha3).split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHANUMERIC + "{3}");
		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}


	void simpleStringTest(final String name, final String input) {
		simpleArrayTest(name, input.split("\\|"));
	}

	void simpleArrayTest(final String name, final String[] inputs) {
		final TextAnalyzer analysis = new TextAnalyzer("DataValueFootnoteSymbol");
		int locked = -1;
		int realSamples = 0;
		int empty = 0;
		int minTrimmedLength = Integer.MAX_VALUE;
		int maxTrimmedLength = Integer.MIN_VALUE;
		int minLength = Integer.MAX_VALUE;
		int maxLength = Integer.MIN_VALUE;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = realSamples;
			if (inputs[i].trim().isEmpty())
				empty++;
			else
				realSamples++;

			int len = inputs[i].trim().length();
			if (len != 0) {
				if (len > maxTrimmedLength)
					maxTrimmedLength = len;
				if (len != 0 && len < minTrimmedLength)
					minTrimmedLength = len;
				len = inputs[i].length();
				if (len > maxLength)
					maxLength = len;
				if (len != 0 && len < minLength)
					minLength = len;
			}
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, realSamples >= TextAnalyzer.SAMPLE_DEFAULT ? TextAnalyzer.SAMPLE_DEFAULT : -1);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		if (inputs.length == empty)
			Assert.assertEquals(result.getTypeQualifier(), "BLANK");
		else
			Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getBlankCount(), empty);
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getBlankCount());
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		int matches = 0;
		for (int i = 0; i < inputs.length; i++) {
			if (!inputs[i].trim().isEmpty() && inputs[i].matches(result.getRegExp()))
				matches++;
		}
		Assert.assertEquals(matches, result.getMatchCount());

		if (result.getMatchCount() != 0) {
			String re = "";
			if (result.getLeadingWhiteSpace())
				re += "\\p{javaWhitespace}*";
			re += KnownPatterns.freezeANY(minTrimmedLength, maxTrimmedLength, minLength, maxLength, result.getLeadingWhiteSpace(), result.getTrailingWhiteSpace(), result.getMultiline());
			if (result.getTrailingWhiteSpace())
				re += "\\p{javaWhitespace}*";
			Assert.assertEquals(result.getRegExp(), re);
		}
	}

	@Test
	public void basicEnum() throws IOException {
		final String input = "APARTMENT|APARTMENT|DUPLEX|APARTMENT|DUPLEX|CONDO|DUPLEX|CONDO|" +
				"DUPLEX|DUPLEX|CONDO|CONDODUPLEX|DUPLEX|CONDO|APARTMENT|" +
				"DUPLEX|CONDO|CONDO|CONDO|DUPLEX|DUPLEX|DUPLEX|DUPLEX|CONDO|" +
				"DUPLEX|DUPLEX|APARTMENT|CONDO|DUPLEX|CONDO|APARTMENT|APARTMENT|DUPLEX|" +
				"DUPLEX|APARTMENT|APARTMENT|APARTMENT|CONDO|CONDO|APARTMENT|CONDO|DUPLEX|" +
				"DUPLEX|CONDO|APARTMENT|DUPLEX|CONDO|DUPLEX|APARTMENT|CONDO|APARTMENT|" +
				"CONDO|CONDO|CONDO|CONDO|MULTI-FAMILY|DUPLEX|APARTMENT|MULTI-FAMILY|DUPLEX|" +
				"CONDO|APARTMENT|APARTMENT|CONDO|CONDO|MULTI-FAMILY|DUPLEX|CONDO|APARTMENT|" +
				"CONDO|DUPLEX|APARTMENT|CONDO|DUPLEX|DUPLEX|APARTMENT|APARTMENT|APARTMENT|" +
				"APARTMENT|APARTMENT|APARTMENT|CONDO|CONDO|APARTMENT|APARTMENT|CONDO|APARTMENT|" +
				"CONDO|APARTMENT|CONDO|APARTMENT|DUPLEX|CONDO|APARTMENT|APARTMENT|DUPLEX|" +
				"CONDO|APARTMENT|APARTMENT|DUPLEX|DUPLEX|CONDO|APARTMENT|CONDO|APARTMENT|" +
				"APARTMENT|CONDO|APARTMENT|CONDO|DUPLEX|MULTI-FAMILY|DUPLEX|CONDO|DUPLEX|" +
				"CONDO|APARTMENT|CONDO|DUPLEX|MULTI-FAMILY|APARTMENT|CONDO|DUPLEX|DUPLEX|" +
				"MULTI-FAMILY|APARTMENT|APARTMENT|APARTMENT|DUPLEX|APARTMENT|CONDO|CONDO|DUPLEX|" +
				"DUPLEX|DUPLEX|APARTMENT|DUPLEX|APARTMENT|DUPLEX|DUPLEX|DUPLEX|CONDO|" +
				"CONDO|APARTMENT|APARTMENT|APARTMENT|DUPLEX|APARTMENT|CONDO|MULTI-FAMILY|CONDO|" +
				"APARTMENT|DUPLEX|DUPLEX|MULTI-FAMILY|MULTI-FAMILY|DUPLEX|DUPLEX|DUPLEX|APARTMENT|" +
				"APARTMENT|DUPLEX|APARTMENT|DUPLEX|APARTMENT|APARTMENT|CONDO|CONDO|CONDO|" +
				"CONDO|DUPLEX|CONDO|MULTI-FAMILY|CONDO|CONDO|APARTMENT|CONDO|APARTMENT|" +
				"CONDO|APARTMENT|APARTMENT|CONDO|DUPLEX|APARTMENT|APARTMENT|APARTMENT|CONDO|" +
				"CONDO|CONDO|DUPLEX|DUPLEX|APARTMENT|CONDO|DUPLEX|DUPLEX|APARTMENT|" +
				"APARTMENT|CONDO|DUPLEX|APARTMENT|CONDO|CONDO|DUPLEX|CONDO|CONDO|" +
				"DUPLEX|CONDO|APARTMENT|DUPLEX|CONDO|CONDO|APARTMENT|DUPLEX|DUPLEX|" +
				"CONDO|APARTMENT|APARTMENT|CONDO|APARTMENT|DUPLEX|CONDO|APARTMENT|MULTI-FAMILY|" +
				"DUPLEX|CONDO|APARTMENT|APARTMENT|CONDO|APARTMENT|MULTI-FAMILY|CONDO|DUPLEX|" +
				"DUPLEX|CONDO|DUPLEX|DUPLEX|DUPLEX|DUPLEX|CONDO|CONDO|CONDO|" +
				"APARTMENT|CONDO|APARTMENT|DUPLEX|APARTMENT|APARTMENT|APARTMENT|DUPLEX|APARTMENT|" +
				"DUPLEX|APARTMENT|APARTMENT|APARTMENT|DUPLEX|DUPLEX|DUPLEX|CONDO|CONDO|" +
				"DUPLEX|CONDO|CONDO|APARTMENT|CONDO|APARTMENT|APARTMENT|APARTMENT|CONDO|" +
				"CONDO|CONDO|DUPLEX|CONDO|APARTMENT|CONDO|DUPLEX|DUPLEX|APARTMENT|" +
				"CONDO|APARTMENT|DUPLEX|DUPLEX|MULTI-FAMILY|DUPLEX|DUPLEX|DUPLEX|DUPLEX|" +
				"DUPLEX|DUPLEX|APARTMENT|DUPLEX|CONDO|APARTMENT|APARTMENT|MULTI-FAMILY|DUPLEX|" +
				"APARTMENT|APARTMENT|CONDO|CONDO|DUPLEX|CONDO|DUPLEX|DUPLEX|DUPLEX|" +
				"APARTMENT|CONDO|CONDO|CONDO|APARTMENT|";
		simpleStringTest("basicEnum", input);
	}

	@Test
	public void blanksLeft() throws IOException {
		final String[] inputs = new String[] {
				" D12345",
				"A123 56", "A1234567", "A12345678", "A123456789", "A123456", "A1234567", "A12345678", "A123456789",
				"B123 56", "B1234567", "B12345678", "B123456789", "B123456", "B1234567", "B12345678", "B123456789",
				"C123 56", "C1234567", "C12345678", "C123456789", "C123456", "C1234567", "C12345678", "C123456789"
		};

		simpleArrayTest("blanksLeft", inputs);
	}

	@Test
	public void blanksInInput() throws IOException {
		final String[] inputs = new String[] {
				" D12345", "  C123456789",
				"A123 56", "A1234567", "A12345678", "        ", "A123456", "A1234567", "A12345678", "A123456789",
				"B123 56", "B1234567", "B12345678", "B123456789", "B123456", "B1234567", "B12345678", "B123456789",
				"C123 56", "C1234567", "C12345678", "C123456789", "    ", "C1234567", "C12345678", "C123456789"
		};

		simpleArrayTest("blanksLeft", inputs);
	}

	@Test
	public void allEmpty() throws IOException {
		final String[] inputs = new String[] {
				"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
				"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
		};

		simpleArrayTest("allEmpty", inputs);
	}

	@Test
	public void blanksInField() throws IOException {
		final String[] inputs = new String[] {
				"-", "-", "", "", "", "", "^^^", "", "", "", "-", "", "", "", "", "", "", "", "",
				"", "-", "", "", "", "", "", "-", "", "", "-", "", "-", "-", "", "", "", "-", "", "",
				"-", "", "", "", "-", "", "-", "-", "", "", "", "-", "", "-", "", "", "", "****", "****", "",
				"", "", "", "", "", "****", "****", "", "****", "****", "****", "****", "", "", "", "", "", "", "****", "****",
				"", "", "", "****", "****", "****", "****", "", "", "", "", "****", "****", "", "", "****", "", "", "", "", " "
		};

		simpleArrayTest("DataValueFootnoteSymbol", inputs);
	}

	@Test
	public void blanksAtEndOfField() throws IOException {
		final String[] inputs = new String[] {
				"", "Foster Road", "", "Grove Road", "", "Library", "", "Bradgers Hill Road", "", "Tomlinson Avenue", "Wheatfield Road", "Tomlinson Avenue", "",
				"Bradgers Hill Road", "", "Nixon Street", "", "Moor Lane", "", "West Hanningfield Road", "Fambridge Road", "Victoria Drive", "Maypole Road",
				"Station Road", "Roundbush Road", "Harborough Hall Lane", "Colchester Road", "Church Road", "Roundbush Road", "Harborough Hall Lane", "Colchester Road", "The Folly",
				"Little Horkesley Road", "London Road", "Home Farm Lane", "Damants Farm Lane", "Hospital Lane", "Clarendon Way", "North Station Rbt", "New Writtle Street", "Oxford Road", "School Lane",
				"Tog Lane", "Station Road", "Colchester Road", "Cooks Hill", "Clarendon Way", "North Station Rbt", "Kelvedon Road", "Latchingdon Road", "Barnhall Road", "Trusses Road",
				"", "School Lane", "Castle Drive", "The Street", "Fairstead Hall Road", "Pepples Lane", "", "Eastern Avenue", "", "Red Lane",
				"", "Granville Street", "", "yes ", "Yes Tactile", "", "Wilmot Road", "Wilmot Lane", "", "Victoria Street",
				"", "Kirk Gate", "", "Gables Lea", "", "Village Hall", "", "Morley Road", "", "Beach Road",
				"Marine Parade", "", "Mount Pleasant", "", "Heol Camlan", "", "Golwg y Mynydd", "", "Sraid na h-Eaglaise", "",
				"Geilear", "", "Tom na Ba", "", "Rathad Ur", "", "Geilear", "Rathad Ur", "", "Struan Ruadh",
				"", "Struan Ruadh", "", "Rubhachlachainn", "", "Sraid a' Chaisteil", "Sraid a' Bhanca", "Ionad Casimir", "Ionad Mhicceallaig", "", "Slighe Ruairidh", ""
		};
		simpleArrayTest("blanksAtEndOfField", inputs);
	}

	@Test
	public void basicPromote() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input =
				"01000053218|0100BRP90233|0100BRP90237|0180BAA01319|0180BAC30834|0190NSC30194|0190NSC30195|0190NSC30652|0190NSC30653|0190NSC30784|" +
		"0190NSC30785|0190NSY28569|0190NSZ01245|020035037|02900033|02900033|02900039|02901210|02903036|02903037|" +
		"030051210001|030051210002|030054160002|030055200003|03700325|03700325|0380F968G059|040000002968|049000000804|049002399361|" +
		"049002399861|0500CCITY084|0500CCITY248|0500CCITY476|0500FWISH002|0500HHUNT027|0500HSTNS060|0500HSTNS062|0500SHARS006|0500SHARS016|" +
		"0590PET621|0590PET622|0590PQG571|0600CR087|0600CR290|0610CH19130|0610CH548|0610EP19031|068000000461|068000000462|" +
		"068000000502|069000024300|0690WNA02867|0690WNA02867|075071047A|075071047B|07605752|077072401A|077072401A|077072572A|" +
		"077072583A|079073001K|0800COA10071|0800COA10194|0800COA10196|0800COA10196|0800COA10204|0800COA10207|0800COA10267|0800COA10268|" +
		"0800COA10268|0800COA10268|0800COA10386|0800COA10469|0800COA10470|0800COA10490|0800COB20133|0800COB20134|0800COB20138|0800COB20139|" +
		"0800COC30257|0800COC30258|0800COC30488|0800COC30504|0800COC30505|0800COC30649|0800COC30815|0800COC30873|0800COC31003|0800COC31004|" +
		"0800COC31093|0800COC31215|0800COC31216|0800COC31221|0800COC31222|0800COC31229|0800COC31231|0800COC31306|0800COC31307|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHANUMERIC + "{8,12}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicText() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int iterations = 1000;
		int locked = -1;

		for (int i = 0; i < iterations; i++) {
			if (analysis.train("primary") && locked == -1)
				locked = i;
			if (analysis.train("secondary") && locked == -1)
				locked = i;
			if (analysis.train("tertiary") && locked == -1)
				locked = i;
			if (analysis.train("fictional") && locked == -1)
				locked = i;
			if (analysis.train(null) && locked == -1)
				locked = i;
		}
		analysis.train("secondory");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT/4);
		Assert.assertEquals(result.getSampleCount(), 5 * iterations + 1);
		Assert.assertEquals(result.getNullCount(), iterations);
		Assert.assertEquals(result.getCardinality(), 5);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{7,9}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void textBlocks() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int iterations = 100000;
		final Random random = new Random(478031);
		int locked = -1;
		StringBuilder line = new StringBuilder();
		int minTrimmedLength = Integer.MAX_VALUE;
		int maxTrimmedLength = Integer.MIN_VALUE;
		int minLength = Integer.MAX_VALUE;
		int maxLength = Integer.MIN_VALUE;
		String alphabet = "abcdefhijklmnopqrstuvwxyz";

		for (int i = 0; i < iterations; i++) {
			line.setLength(0);
			int wordCount = random.nextInt(20);
			for (int words = 0; words < wordCount; words++) {
				int charCount = random.nextInt(10);
				for (int chars = 0; chars < charCount; chars++) {
					line.append(alphabet.charAt(random.nextInt(25)));
				}
				line.append(' ');
			}
			String sample = line.toString().trim();
			int len = sample.length();
			if (len > maxLength)
				maxLength = len;
			if (len < minLength && len != 0)
				minLength = len;
			len = sample.trim().length();
			if (len > maxTrimmedLength)
				maxTrimmedLength = len;
			if (len < minTrimmedLength && len != 0)
				minTrimmedLength = len;
			if (analysis.train(sample) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), iterations);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.freezeANY(minTrimmedLength, maxTrimmedLength, minLength, maxLength, result.getLeadingWhiteSpace(), result.getTrailingWhiteSpace(), result.getMultiline()));
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void setSampleSize() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final Random random = new Random();
		int locked = -1;
		int sample = 0;

		analysis.setDetectWindow(2* TextAnalyzer.SAMPLE_DEFAULT);
		for (int i = 0; i <= TextAnalyzer.SAMPLE_DEFAULT; i++) {
			sample++;
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = sample;
		}
		for (int i = 0; i <= TextAnalyzer.SAMPLE_DEFAULT; i++) {
			sample++;
			if (analysis.train(String.valueOf(random.nextDouble())) && locked == -1)
				locked = sample;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, 2 * TextAnalyzer.SAMPLE_DEFAULT + 1);
		Assert.assertEquals(result.getSampleCount(), 2 * (TextAnalyzer.SAMPLE_DEFAULT + 1));
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void getSampleSize()  throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		Assert.assertEquals(analysis.getSampleSize(), TextAnalyzer.SAMPLE_DEFAULT);
	}

	@Test
	public void setSampleSizeTooSmall() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		try {
			analysis.setDetectWindow(TextAnalyzer.SAMPLE_DEFAULT - 1);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals(e.getMessage(), "Cannot set sample size below " + TextAnalyzer.SAMPLE_DEFAULT);
			return;
		}
		Assert.fail("Exception should have been thrown");
	}

	@Test
	public void setSampleSizeTooLate() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final Random random = new Random();
		int locked = -1;
		int i = 0;

		for (; i <= TextAnalyzer.SAMPLE_DEFAULT; i++) {
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = i;
		}

		try {
			analysis.setDetectWindow(2* TextAnalyzer.SAMPLE_DEFAULT);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals(e.getMessage(), "Cannot change sample size once training has started");
			return;
		}
		Assert.fail("Exception should have been thrown");
	}

	@Test
	public void getMaxCardinality() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		Assert.assertEquals(analysis.getMaxCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
	}

	@Test
	public void setMaxCardinalityTooSmall() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		try {
			analysis.setMaxCardinality(-1);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals(e.getMessage(), "Invalid value for maxCardinality -1");
			return;
		}
		Assert.fail("Exception should have been thrown");
	}

	@Test
	public void setMaxCardinalityTooLate() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final Random random = new Random();
		int locked = -1;
		int i = 0;

		for (; i <= TextAnalyzer.SAMPLE_DEFAULT; i++) {
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = i;
		}

		try {
			analysis.setMaxCardinality(2* TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals(e.getMessage(), "Cannot change maxCardinality once training has started");
			return;
		}
		Assert.fail("Exception should have been thrown");
	}

	@Test
	public void getOutlierCount() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		Assert.assertEquals(analysis.getMaxOutliers(), TextAnalyzer.MAX_OUTLIERS_DEFAULT);
	}

	@Test
	public void setMaxOutliersTooSmall() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		try {
			analysis.setMaxOutliers(-1);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals(e.getMessage(), "Invalid value for outlier count -1");
			return;
		}
		Assert.fail("Exception should have been thrown");
	}

	@Test
	public void setMaxOutliersTooLate() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final Random random = new Random();
		int locked = -1;
		int i = 0;

		for (; i <= TextAnalyzer.SAMPLE_DEFAULT; i++) {
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = i;
		}

		try {
			analysis.setMaxOutliers(2* TextAnalyzer.MAX_OUTLIERS_DEFAULT);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals(e.getMessage(), "Cannot change outlier count once training has started");
			return;
		}
		Assert.fail("Exception should have been thrown");
	}

	@Test
	public void possibleSSN() throws IOException {
		final Random random = new Random(314159265);
		String[] samples = new String[1000];

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < samples.length; i++) {
			b.setLength(0);
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append('-');
			b.append(String.format("%02d", random.nextInt(100)));
			b.append('-');
			b.append(String.format("%04d", random.nextInt(10000)));
			samples[i] = b.toString();
		}


		final TextAnalyzer analysis = new TextAnalyzer();
		analysis.setLengthQualifier(false);
		for (String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), samples.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getRegExp(), "\\d{3}-\\d{2}-\\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < samples.length; i++) {
			Assert.assertTrue(samples[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void USPhone() throws IOException {
		final Random random = new Random(314159265);
		String[] samples = new String[1000];

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < samples.length; i++) {
			b.setLength(0);
			b.append("+1 ");
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append(' ');
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append(' ');
			b.append(String.format("%04d", random.nextInt(10000)));
			samples[i] = b.toString();
		}


		final TextAnalyzer analysis = new TextAnalyzer();
		for (String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), samples.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getRegExp(), "\\+\\d{1} \\d{3} \\d{3} \\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < samples.length; i++) {
			Assert.assertTrue(samples[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void USPhone2() throws IOException {
		final Random random = new Random(314159265);
		String[] samples = new String[1000];

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < samples.length; i++) {
			b.setLength(0);
			b.append("1.");
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append('.');
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append('.');
			b.append(String.format("%04d", random.nextInt(10000)));
			samples[i] = b.toString();
		}


		final TextAnalyzer analysis = new TextAnalyzer();
		for (String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), samples.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getRegExp(), "\\d{1}\\.\\d{3}\\.\\d{3}\\.\\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < samples.length; i++) {
			Assert.assertTrue(samples[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void USPhone3() throws IOException {
		final Random random = new Random(314159265);
		String[] samples = new String[1000];

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < samples.length; i++) {
			b.setLength(0);
			b.append("(");
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append(") ");
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append(' ');
			b.append(String.format("%04d", random.nextInt(10000)));
			samples[i] = b.toString();
		}


		final TextAnalyzer analysis = new TextAnalyzer();
		for (String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), samples.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getRegExp(), "\\(\\d{3}\\) \\d{3} \\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < samples.length; i++) {
			Assert.assertTrue(samples[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void difficultRegExp() throws IOException {
		final Random random = new Random(314159265);
		String[] samples = new String[1000];

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < samples.length; i++) {
			b.setLength(0);
			b.append("[");
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append("){[0-9] ^");
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append('$');
			b.append(String.format("%04d", random.nextInt(10000)));
			samples[i] = b.toString();
		}


		final TextAnalyzer analysis = new TextAnalyzer();
		for (String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), samples.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getRegExp(), "\\[\\d{3}\\)\\{\\[\\d{1}-\\d{1}\\] \\^\\d{3}\\$\\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < samples.length; i++) {
			Assert.assertTrue(samples[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void bumpMaxCardinality() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		analysis.setMaxCardinality(2 * TextAnalyzer.MAX_CARDINALITY_DEFAULT);

		final Random random = new Random();
		final int nullIterations = 50;
		final int iterations = 10000;
		int locked = -1;

		for (int i = 0; i < nullIterations; i++) {
			analysis.train(null);
		}
		int cnt = 0;
		while (cnt < iterations) {
			final long randomLong = random.nextInt(Integer.MAX_VALUE) + 1000000000L;
			if (randomLong >  9999999999L)
				continue;
			if (analysis.train(String.valueOf(randomLong)) && locked == -1)
				locked = cnt;
			cnt++;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), iterations + nullIterations);
		Assert.assertEquals(result.getCardinality(), 2 * TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), nullIterations);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getRegExp(), "\\d{10}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void keyFieldLong() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int start = 10000;
		final int end = 12000;
		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i - start;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), end - start);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getRegExp(), "\\d{5}");
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertTrue(result.isKey());
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void setMaxOutliers() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Alphabet");
		final int start = 10000;
		final int end = 12000;
		final int newMaxOutliers = 12;

		int locked = -1;

		analysis.setMaxOutliers(newMaxOutliers);

		analysis.train("A");
		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i - start;
		}
		analysis.train("B");
		analysis.train("C");
		analysis.train("D");
		analysis.train("E");
		analysis.train("F");
		analysis.train("G");
		analysis.train("H");
		analysis.train("I");
		analysis.train("J");
		analysis.train("K");
		analysis.train("L");
		analysis.train("M");
		analysis.train("N");
		analysis.train("O");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(analysis.getMaxOutliers(), newMaxOutliers);
		Assert.assertEquals(result.getOutlierCount(), newMaxOutliers);
		Assert.assertEquals(result.getSampleCount(), newMaxOutliers + 3 + end - start);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getRegExp(), "\\d{5}");
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertTrue(result.isKey());
		Assert.assertEquals(result.getConfidence(), 1 - (double)15/result.getSampleCount());
		Assert.assertEquals(result.dump(true), "TextAnalysisResult [name=Alphabet, matchCount=2000, sampleCount=2015, nullCount=0, blankCount=0, regexp=\"\\d{5}\", confidence=0.9925558312655087, type=Long, min=\"10000\", max=\"11999\", minLength=1, maxLength=5, sum=\"21999000\", cardinality=MAX, outliers=12 {\"A\":1 \"B\":1 \"C\":1 \"D\":1 \"E\":1 \"F\":1 \"G\":1 \"H\":1 \"I\":1 \"J\":1 ...}, PossibleKey]");
	}

	@Test
	public void keyFieldString() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int start = 100000;
		final int end = 120000;

		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train("A" + String.valueOf(i)) && locked == -1)
				locked = i - start;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), end - start);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHANUMERIC + "{7}");
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertTrue(result.isKey());
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void notKeyField() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int start = 10000;
		final int end = 12000;

		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i - start;
		}

		analysis.train(String.valueOf(start));

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), 1 + end - start);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getRegExp(), "\\d{5}");
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertFalse(result.isKey());
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	public String[] decoder = new String[] {
			"Integer", "Boolean", "Long", "Double", "Date",
			"ISO-3166-3", "ISO-3166-2", "ZIP", "US_STATE", "CA_PROVINCE",
			"US_STREET"
	};

	public String[] generateTestData(int type, int length) {
		final Random random = new Random(314159265);
		String[] result = new String[length];
		String[] candidatesISO3166_3 = TestUtils.valid3166_3.split("\\|");
		String[] candidatesISO3166_2 = TestUtils.valid3166_2.split("\\|");
		String[] candidatesZips = TestUtils.validZips.split("\\|");
		String[] candidatesUSStates = TestUtils.validUSStates.split("\\|");
		String[] candidatesCAProvinces = TestUtils.validCAProvinces.split("\\|");

		for (int i = 0; i < length; i++)
		switch (type) {
		case 0:
			// Integer
			result[i] = String.valueOf(random.nextInt());
			break;
		case 1:
			// Boolean
			result[i] = String.valueOf(random.nextBoolean());
			break;
		case 2:
			// Long
			result[i] = String.valueOf(random.nextLong());
			break;
		case 3:
			// Double
			result[i] = String.valueOf(random.nextDouble());
			break;
		case 4:
			// Date
			Date d = new Date(random.nextLong());
			result[i] = d.toString();
			break;
		case 5:
			// ISO 3166-3
			result[i] = candidatesISO3166_3[random.nextInt(candidatesISO3166_3.length)];
			break;
		case 6:
			// ISO 3166-2
			result[i] = candidatesISO3166_2[random.nextInt(candidatesISO3166_2.length)];
			break;
		case 7:
			// Zip Cpde
			result[i] = candidatesZips[random.nextInt(candidatesZips.length)];
			break;
		case 8:
			// US State
			result[i] = candidatesUSStates[random.nextInt(candidatesUSStates.length)];
			break;
		case 9:
			// CA Provinces
			result[i] = candidatesCAProvinces[random.nextInt(candidatesCAProvinces.length)];
			break;
		case 10:
			// US Street
			result[i] = TestUtils.validUSStreets[random.nextInt(TestUtils.validUSStreets.length)];
			break;
		}

		return result;
	}

	class AnalysisThread implements Runnable {
		private String id;
		private int streamType;
		private String[] stream;
		private TextAnalysisResult answer;
		private TextAnalyzer analysis;

		AnalysisThread(String id, int streamType, String[] stream, TextAnalysisResult answer) throws IOException {
			this.id = id;
			this.streamType = streamType;
			this.stream = stream;
			this.answer = answer;
			analysis = new TextAnalyzer();
//			System.out.printf("Thread %s: created, Stream: type: %s, length: %d\n",
//					this.id, decoder[this.streamType], this.stream.length);
		}

		@Override
		public void run() {
//			long start = System.currentTimeMillis();
			for (int i = 0; i < stream.length; i++)
				analysis.train(stream[i]);

			final TextAnalysisResult result = analysis.getResult();

			Assert.assertEquals(result.getSampleCount(), answer.getSampleCount());
			Assert.assertEquals(result.getNullCount(), answer.getNullCount());
			Assert.assertEquals(result.getBlankCount(), answer.getBlankCount());
			Assert.assertEquals(result.getRegExp(), answer.getRegExp());
			Assert.assertEquals(result.getConfidence(), answer.getConfidence());
			Assert.assertEquals(result.getType(), answer.getType());
			Assert.assertEquals(result.getMinValue(), answer.getMinValue());
			Assert.assertEquals(result.getMaxValue(), answer.getMaxValue());

//			System.out.printf("Thread %s: exiting, duration %d\n", id, System.currentTimeMillis() - start);
		}
	}

	@Test
	public void testThreading() throws IOException, InterruptedException {
		final Random random = new Random(271828);
		final int THREADS = 1000;
		Thread[] threads = new Thread[THREADS];

		for (int t = 0; t < THREADS; t++) {
			int type = random.nextInt(decoder.length);
			int length = 30 + random.nextInt(10000);
			String[] stream = generateTestData(type, length);

			TextAnalyzer analysis = new TextAnalyzer();
			for (int i = 0; i < stream.length; i++)
				analysis.train(stream[i]);

			threads[t] = new Thread(new AnalysisThread(String.valueOf(t), type, stream, analysis.getResult()));
		}

		for (int t = 0; t < THREADS; t++)
			threads[t].start();

		for (int t = 0; t < THREADS; t++)
			if (threads[t].isAlive())
				threads[t].join();
	}

	//@Test
	public void fuzzInt() throws IOException {
		final Random random = new Random(3141562);
		final int SAMPLES = 1000;
		final int errorRate = 1;

		for (int iter = 0; iter < 100; iter++) {
			final TextAnalyzer analysis = new TextAnalyzer();
			analysis.setThreshold(100 - errorRate);
			int length = random.nextInt(9);
			long low = (long)Math.pow(10, length);
			long lowest = low;
			long lowestFloat = low;
			int lowLength = String.valueOf(low).length();
			long high = low + SAMPLES - 1;
			int highLength = String.valueOf(high).length();
			int misses = 0;
			boolean sticky = random.nextBoolean();
			boolean isNegative = false;
			int floats = 0;
			int strings = 0;
			int nulls = 0;
			int blanks = 0;
			int errorCase = -1;
			long firstFloat = -1;
			String[] errorCaseDecode = new String[] { "String", "NegativeInt", "Double", "negativeDouble", "null", "blank" };

			for (long i = low; i <= high; i++) {
				if (i != low && i != high && random.nextInt(99) < 2) {
					if (errorCase == -1 || !sticky)
						errorCase = random.nextInt(6);
					switch (errorCase) {
					case 0:
						// String
						analysis.train("hello");
						strings++;
						misses++;
						break;
					case 1:
						// NegativeInt
						analysis.train(String.valueOf(-i));
						isNegative = true;
						lowest = -i;
						break;
					case 2:
						// Double
						analysis.train(String.valueOf((double)i));
						floats++;
						if (firstFloat == -1)
							firstFloat = i - low - nulls - blanks;
						misses++;
						break;
					case 3:
						// Negative Double
						analysis.train(String.valueOf((double)-i));
						isNegative = true;
						floats++;
						lowestFloat = -i;
						if (firstFloat == -1)
							firstFloat = i - low - nulls - blanks;
						misses++;
						break;
					case 4:
						// Null
						analysis.train(null);
						nulls++;
						misses++;
						break;
					case 5:
						// Blank
						analysis.train("");
						blanks++;
						misses++;
						break;
					}
				}
				else
					analysis.train(String.valueOf(i));
			}

			PatternInfo.Type answer;
			String re = "";
			String min;
			String max;
			if (firstFloat != -1 && firstFloat < analysis.getSampleSize() || floats >= (errorRate * SAMPLES)/100) {
				misses -= floats;
				answer = PatternInfo.Type.DOUBLE;
				min = String.valueOf((double)Math.min(lowest, lowestFloat));
				max = String.valueOf((double)high);
				re += min.charAt(0) == '-' ? "-?\\d+|-?(\\d+)?\\.\\d+" : "\\d+|(\\d+)?\\.\\d+";
			}
			else {
				if (isNegative)
					re += "-?";
				re += "\\d{" + lowLength;
				if (lowLength != highLength)
					re += "," + highLength;
				re += "}";
				answer = PatternInfo.Type.LONG;
				min = String.valueOf(lowest);
				max = String.valueOf(high);
			}

			System.err.printf("Iter: %d, length: %d, start: %d, sticky: %b, re: %s, floats: %d, firstFloat: %d, strings: %d, errorCase: %s\n",
					iter, length, low, sticky, re, floats, firstFloat, strings, sticky ? errorCaseDecode[errorCase] : "Variable");

			final TextAnalysisResult result = analysis.getResult();

			Assert.assertEquals(result.getSampleCount(), SAMPLES);
			Assert.assertEquals(result.getNullCount(), nulls);
			Assert.assertEquals(result.getBlankCount(), blanks);
			Assert.assertEquals(result.getRegExp(), re);
			Assert.assertEquals(result.getConfidence(), (double)(SAMPLES-misses)/(SAMPLES - blanks - nulls));
			Assert.assertEquals(result.getType(), answer);
			Assert.assertEquals(result.getMinValue(), min);
			Assert.assertEquals(result.getMaxValue(), max);
		}
	}
}
