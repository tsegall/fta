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
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;

public class TestBooleans {
	@Test(groups = { TestGroups.ALL, TestGroups.BOOLEANS })
	public void onlyTrue() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("onlyTrue");

		analysis.train("true");

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), 1);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_BOOLEAN_TRUE_FALSE));
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.BOOLEAN);
		assertEquals(result.getMinLength(), 4);
		assertEquals(result.getMaxLength(), 4);
		assertEquals(result.getMinValue(), "true");
		assertEquals(result.getMaxValue(), "true");
		assertTrue("true".matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BOOLEANS })
	public void basicBoolean() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicBoolean");
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
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, -1);
		assertEquals(result.getSampleCount(), inputs.length + NULL_COUNT);
		assertEquals(result.getOutlierCount(), 1);
		assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		assertEquals(result.getNullCount(), NULL_COUNT);
		assertEquals(result.getRegExp(), KnownPatterns.PATTERN_WHITESPACE +
				"(" + analysis.getRegExp(KnownPatterns.ID.ID_BOOLEAN_TRUE_FALSE) + ")" +
				KnownPatterns.PATTERN_WHITESPACE);
		assertEquals(result.getConfidence(), 1 - (double)1/(result.getSampleCount() - NULL_COUNT));
		assertEquals(result.getType(), FTAType.BOOLEAN);
		assertEquals(result.getTypeQualifier(), "TRUE_FALSE");
		assertEquals(result.getMinLength(), 4);
		assertEquals(result.getMaxLength(), 12);
		assertEquals(result.getMinValue(), "false");
		assertEquals(result.getMaxValue(), "true");
		assertTrue(inputs[0].matches(result.getRegExp()));

		int matches = 0;

		for (final String input : inputs) {
			if (input.matches(result.getRegExp()))
				matches++;
		}
		assertEquals(result.getMatchCount(), matches);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BOOLEANS })
	public void basicBooleanYesNo() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicBooleanYesNo");
		final String[] inputs = "no|yes|YES|    no   |NO |YES|yes|no|No|Yes|no|  NO|NO|yes|YES|bogus".split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, -1);
		assertEquals(result.getSampleCount(), inputs.length + 2);
		assertEquals(result.getOutlierCount(), 1);
		assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		assertEquals(result.getNullCount(), 2);
		assertEquals(result.getRegExp(), KnownPatterns.PATTERN_WHITESPACE +
				"(" + analysis.getRegExp(KnownPatterns.ID.ID_BOOLEAN_YES_NO) + ")" +
				KnownPatterns.PATTERN_WHITESPACE);
		assertEquals(result.getConfidence(), .9375);
		assertEquals(result.getType(), FTAType.BOOLEAN);
		assertEquals(result.getTypeQualifier(), "YES_NO");
		assertEquals(result.getMinLength(), 2);
		assertEquals(result.getMaxLength(), 9);
		assertEquals(result.getMinValue(), "no");
		assertEquals(result.getMaxValue(), "yes");
		assertTrue(inputs[0].matches(result.getRegExp()));

		int matches = 0;
		for (final String input : inputs) {
			if (input.trim().matches(result.getRegExp()))
					matches++;
		}
		assertEquals(result.getMatchCount(), matches);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BOOLEANS })
	public void basicBooleanYesNoManySamples() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicBooleanYesNoManySamples");
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
		TestUtils.checkSerialization(analysis);

		assertNotEquals(locked, -1);
		assertEquals(result.getSampleCount(), inputs.length + NULL_COUNT);
		assertEquals(result.getOutlierCount(), 1);
		assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		assertEquals(result.getNullCount(), 2);
		assertEquals(result.getRegExp(), KnownPatterns.PATTERN_WHITESPACE +
				"(" + analysis.getRegExp(KnownPatterns.ID.ID_BOOLEAN_YES_NO) + ")" +
				KnownPatterns.PATTERN_WHITESPACE);
		assertEquals(result.getConfidence(), 1 - (double)1/(result.getSampleCount() - NULL_COUNT));
		assertEquals(result.getType(), FTAType.BOOLEAN);
		assertEquals(result.getTypeQualifier(), "YES_NO");
		assertEquals(result.getMinLength(), 2);
		assertEquals(result.getMaxLength(), 9);
		assertEquals(result.getMinValue(), "no");
		assertEquals(result.getMaxValue(), "yes");
		assertTrue(inputs[0].matches(result.getRegExp()));

		int matches = 0;
		for (final String input : inputs) {
			if (input.matches(result.getRegExp()))
				matches++;
		}
		assertEquals(result.getMatchCount(), matches);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BOOLEANS })
	public void basicBooleanYN() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicBooleanYN");
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
		TestUtils.checkSerialization(analysis);

		assertNotEquals(locked, -1);
		assertEquals(result.getSampleCount(), inputs.length + 2);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount() - result.getBlankCount());
		assertEquals(result.getNullCount(), 2);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_BOOLEAN_Y_N));
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.BOOLEAN);
		assertEquals(result.getTypeQualifier(), "Y_N");
		assertEquals(result.getMinLength(), 1);
		assertEquals(result.getMaxLength(), 1);
		assertEquals(result.getMinValue(), "n");
		assertEquals(result.getMaxValue(), "y");
		assertTrue(inputs[0].matches(result.getRegExp()));

		int matches = 0;
		for (final String input : inputs) {
			if (input.trim().matches(result.getRegExp()))
					matches++;
		}
		assertEquals(result.getMatchCount(), matches);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BOOLEANS })
	public void basicBooleanN_Bad() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicBooleanN_Bad");
		final int countN = 30;
		final int countC = 10;

		for (int i = 0; i < countN; i++)
			analysis.train("N");
		for (int i = 0; i < countC; i++)
			analysis.train("C");

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), countN + countC);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), countN + countC);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeQualifier());
		assertEquals(result.getMinLength(), 1);
		assertEquals(result.getMaxLength(), 1);
		assertEquals(result.getMinValue(), "C");
		assertEquals(result.getMaxValue(), "N");
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BOOLEANS })
	public void justY() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("justY");
		final int COUNT = 50;

		for (int i = 0; i < 50; i++)
			analysis.train("y");

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), COUNT);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), COUNT);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeQualifier());
		assertEquals(result.getMinLength(), 1);
		assertEquals(result.getMaxLength(), 1);
		assertEquals(result.getMinValue(), "y");
		assertEquals(result.getMaxValue(), "y");
		assertTrue("y".matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BOOLEANS })
	public void basicPseudoBoolean() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicPseudoBoolean");
		final String[] inputs = "0|1|1|0|0|1|1|0|0|1|0|0|0|1|1|0|1|1|1|1|0|0|0|0|1|1|1".split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), inputs.length + 2);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		assertEquals(result.getNullCount(), 2);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_BOOLEAN_ONE_ZERO));
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.BOOLEAN);
		assertEquals(result.getMinLength(), 1);
		assertEquals(result.getMaxLength(), 1);
		assertEquals(result.getMinValue(), "0");
		assertEquals(result.getMaxValue(), "1");
		assertTrue(inputs[0].matches(result.getRegExp()));

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BOOLEANS })
	public void notPseudoBoolean() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("notPseudoBoolean");
		final String[] inputs = "7|1|1|7|7|1|1|7|7|1|7|7|7|1|1|7|1|1|1|1|7|7|7|7|1|1|1".split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), inputs.length + 2);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		assertEquals(result.getNullCount(), 2);
		assertEquals(result.getRegExp(), "\\d");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getMinLength(), 1);
		assertEquals(result.getMaxLength(), 1);
		assertEquals(result.getMinValue(), "1");
		assertEquals(result.getMaxValue(), "7");
		assertTrue(inputs[0].matches(result.getRegExp()));

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.BOOLEANS })
	public void basicNotPseudoBoolean() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicNotPseudoBoolean");
		final String[] inputs = "0|5|5|0|0|5|5|0|0|5|0|0|0|5|5|0|5|5|5|5|0|0|0|0|5|5|5|A".split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), inputs.length + 2);
		assertEquals(result.getOutlierCount(), 1);
		assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		assertEquals(result.getNullCount(), 2);
		assertEquals(result.getRegExp(), "\\d");
		assertEquals(result.getConfidence(), 1 - (double)1/(result.getSampleCount() - result.getNullCount()));
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getCardinality(), 2);
		final Map<String, Long> details = result.getCardinalityDetails();
		assertEquals(details.get("0"), Long.valueOf(13));
		assertEquals(details.get("5"), Long.valueOf(14));
		assertTrue(inputs[0].matches(result.getRegExp()));

		int matches = 0;
		for (final String input : inputs) {
			if (input.matches(result.getRegExp()))
					matches++;
		}
		assertEquals(result.getMatchCount(), matches);
	}
}
