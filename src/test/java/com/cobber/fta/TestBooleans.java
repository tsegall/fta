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

import java.io.IOException;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;

public class TestBooleans {
	@Test
	public void onlyTrue() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer();

		analysis.train("true");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), 1);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_BOOLEAN_TRUE_FALSE));
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), FTAType.BOOLEAN);
		Assert.assertEquals(result.getMinLength(), 4);
		Assert.assertEquals(result.getMaxLength(), 4);
		Assert.assertEquals(result.getMinValue(), "true");
		Assert.assertEquals(result.getMaxValue(), "true");
		Assert.assertTrue("true".matches(result.getRegExp()));
	}

	@Test
	public void basicBoolean() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "false|true|TRUE|    false   |FALSE |TRUE|true|false|False|True|false|  FALSE|FALSE|true|TRUE|bogus".split("\\|");
		final int NULL_COUNT = 2;
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, -1);
		Assert.assertEquals(result.getSampleCount(), inputs.length + NULL_COUNT);
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), NULL_COUNT);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_WHITESPACE +
				"(" + analysis.getRegExp(KnownPatterns.ID.ID_BOOLEAN_TRUE_FALSE) + ")" +
				KnownPatterns.PATTERN_WHITESPACE);
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/(result.getSampleCount() - NULL_COUNT));
		Assert.assertEquals(result.getType(), FTAType.BOOLEAN);
		Assert.assertEquals(result.getTypeQualifier(), "TRUE_FALSE");
		Assert.assertEquals(result.getMinLength(), 4);
		Assert.assertEquals(result.getMaxLength(), 12);
		Assert.assertEquals(result.getMinValue(), "false");
		Assert.assertEquals(result.getMaxValue(), "true");
		Assert.assertTrue(inputs[0].matches(result.getRegExp()));

		int matches = 0;

		for (final String input : inputs) {
			if (input.matches(result.getRegExp()))
				matches++;
		}
		Assert.assertEquals(result.getMatchCount(), matches);
	}

	@Test
	public void basicBooleanYesNo() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "no|yes|YES|    no   |NO |YES|yes|no|No|Yes|no|  NO|NO|yes|YES|bogus".split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, -1);
		Assert.assertEquals(result.getSampleCount(), inputs.length + 2);
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_WHITESPACE +
				"(" + analysis.getRegExp(KnownPatterns.ID.ID_BOOLEAN_YES_NO) + ")" +
				KnownPatterns.PATTERN_WHITESPACE);
		Assert.assertEquals(result.getConfidence(), .9375);
		Assert.assertEquals(result.getType(), FTAType.BOOLEAN);
		Assert.assertEquals(result.getTypeQualifier(), "YES_NO");
		Assert.assertEquals(result.getMinLength(), 2);
		Assert.assertEquals(result.getMaxLength(), 9);
		Assert.assertEquals(result.getMinValue(), "no");
		Assert.assertEquals(result.getMaxValue(), "yes");
		Assert.assertTrue(inputs[0].matches(result.getRegExp()));

		int matches = 0;
		for (final String input : inputs) {
			if (input.trim().matches(result.getRegExp()))
					matches++;
		}
		Assert.assertEquals(result.getMatchCount(), matches);
	}

	@Test
	public void basicBooleanYesNoManySamples() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "no|yes|YES|    no   |NO |YES|yes|no|No|Yes|no|  NO|NO|yes|YES|no|yes|YES|    no   |NO |YES|yes|no|No|Yes|no|  NO|NO|yes|YES|bogus".split("\\|");
		final int NULL_COUNT = 2;
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertNotEquals(locked, -1);
		Assert.assertEquals(result.getSampleCount(), inputs.length + NULL_COUNT);
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_WHITESPACE +
				"(" + analysis.getRegExp(KnownPatterns.ID.ID_BOOLEAN_YES_NO) + ")" +
				KnownPatterns.PATTERN_WHITESPACE);
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/(result.getSampleCount() - NULL_COUNT));
		Assert.assertEquals(result.getType(), FTAType.BOOLEAN);
		Assert.assertEquals(result.getTypeQualifier(), "YES_NO");
		Assert.assertEquals(result.getMinLength(), 2);
		Assert.assertEquals(result.getMaxLength(), 9);
		Assert.assertEquals(result.getMinValue(), "no");
		Assert.assertEquals(result.getMaxValue(), "yes");
		Assert.assertTrue(inputs[0].matches(result.getRegExp()));

		int matches = 0;
		for (final String input : inputs) {
			if (input.matches(result.getRegExp()))
				matches++;
		}
		Assert.assertEquals(result.getMatchCount(), matches);
	}

	@Test
	public void basicBooleanYN() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = new String[] {
				"Y", "Y", "Y", "Y", "Y", "", "Y", "", "", "Y",
				"N", "Y", "Y", "Y", "Y", "", "", "", "Y", "",
				"Y", "N", "N", "N", "", "Y", "Y", "Y", "Y", "N",
				"", "", "", "", "", "", "Y", "", "Y", "Y",
				"Y", "N", "N", "N", "", "Y", "Y", "Y", "Y", "N",
				"Y", "", "Y", "Y", "", "Y", "Y", "Y", "Y", "",
				"Y", "N", "N", "N", "", "Y", "Y", "Y", "Y", "N",
				"Y"
		};
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertNotEquals(locked, -1);
		Assert.assertEquals(result.getSampleCount(), inputs.length + 2);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount() - result.getBlankCount());
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_BOOLEAN_Y_N));
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), FTAType.BOOLEAN);
		Assert.assertEquals(result.getTypeQualifier(), "Y_N");
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxLength(), 1);
		Assert.assertEquals(result.getMinValue(), "n");
		Assert.assertEquals(result.getMaxValue(), "y");
		Assert.assertTrue(inputs[0].matches(result.getRegExp()));

		int matches = 0;
		for (final String input : inputs) {
			if (input.trim().matches(result.getRegExp()))
					matches++;
		}
		Assert.assertEquals(result.getMatchCount(), matches);
	}

	@Test
	public void basicBooleanN_Bad() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int countN = 30;
		final int countC = 10;

		for (int i = 0; i < countN; i++)
			analysis.train("N");
		for (int i = 0; i < countC; i++)
			analysis.train("C");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), countN + countC);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), countN + countC);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxLength(), 1);
		Assert.assertEquals(result.getMinValue(), "C");
		Assert.assertEquals(result.getMaxValue(), "N");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void justY() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int COUNT = 50;

		for (int i = 0; i < 50; i++)
			analysis.train("y");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), COUNT);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), COUNT);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA);
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxLength(), 1);
		Assert.assertEquals(result.getMinValue(), "y");
		Assert.assertEquals(result.getMaxValue(), "y");
		Assert.assertTrue("y".matches(result.getRegExp()));
	}

	@Test
	public void basicPseudoBoolean() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "0|1|1|0|0|1|1|0|0|1|0|0|0|1|1|0|1|1|1|1|0|0|0|0|1|1|1".split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), inputs.length + 2);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_BOOLEAN_ONE_ZERO));
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), FTAType.BOOLEAN);
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxLength(), 1);
		Assert.assertEquals(result.getMinValue(), "0");
		Assert.assertEquals(result.getMaxValue(), "1");
		Assert.assertTrue(inputs[0].matches(result.getRegExp()));

		for (final String input : inputs) {
			Assert.assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test
	public void notPseudoBoolean() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "7|1|1|7|7|1|1|7|7|1|7|7|7|1|1|7|1|1|1|1|7|7|7|7|1|1|1".split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), inputs.length + 2);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getRegExp(), "\\d");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), FTAType.LONG);
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxLength(), 1);
		Assert.assertEquals(result.getMinValue(), "1");
		Assert.assertEquals(result.getMaxValue(), "7");
		Assert.assertTrue(inputs[0].matches(result.getRegExp()));

		for (final String input : inputs) {
			Assert.assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test
	public void basicNotPseudoBoolean() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "0|5|5|0|0|5|5|0|0|5|0|0|0|5|5|0|5|5|5|5|0|0|0|0|5|5|5|A".split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), inputs.length + 2);
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getRegExp(), "\\d");
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/(result.getSampleCount() - result.getNullCount()));
		Assert.assertEquals(result.getType(), FTAType.LONG);
		Assert.assertEquals(result.getCardinality(), 2);
		final Map<String, Long> details = result.getCardinalityDetails();
		Assert.assertEquals(details.get("0"), Long.valueOf(13));
		Assert.assertEquals(details.get("5"), Long.valueOf(14));
		Assert.assertTrue(inputs[0].matches(result.getRegExp()));

		int matches = 0;
		for (final String input : inputs) {
			if (input.matches(result.getRegExp()))
					matches++;
		}
		Assert.assertEquals(result.getMatchCount(), matches);
	}
}
