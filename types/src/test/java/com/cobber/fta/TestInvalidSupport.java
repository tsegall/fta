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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.plugins.address.USZip5;

/**
 * Test support for the invalid concept new with 11.0.
 * We classify input into three classes: valid, outliers, and invalid.
 */
public class TestInvalidSupport {
	@Test(groups = { TestGroups.ALL, TestGroups.INVALID_SUPPORT })
	public void longTest() throws IOException, FTAException {
		final TextAnalyzer analyzer = new TextAnalyzer("longTest");
		final int SIZE = 1000;

		for (long i = 0; i < SIZE; i++)
			analyzer.train(String.valueOf(i));

		// Train a second instance of SIZE - 1 so that this set does not look like an IDENTIFIER
		analyzer.train(String.valueOf(SIZE - 1));
		analyzer.train(null);

		// The letter 'O' not the number '0'
		analyzer.train("O");

		final TextAnalysisResult result = analyzer.getResult();

		assertEquals(result.getSampleCount(), SIZE + 3);
		assertEquals(result.getMatchCount(), SIZE + 1);
		assertNull(result.getSemanticType());
		assertEquals(result.getNullCount(), 1);
		assertEquals(result.getConfidence(), 1 - (double)1/(result.getSampleCount() - result.getNullCount()));
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getMinValue(), "0");
		assertEquals(result.getMaxValue(), "999");
		assertNull(result.getTypeModifier());
		assertNull(result.getTypeModifier());

		// Old behavior
		// assertEquals(result.getOutlierCount(), 1);

		// New behavior
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getInvalidCount(), 1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.INVALID_SUPPORT })
	public void doubleTest() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("doubleTest");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final int SIZE = 1000;

		for (long i = 0; i < SIZE; i++)
			analysis.train(Long.toString(i) + "." + i%10);

		analysis.train(null);

		// The letter 'O' not the number '0'
		analysis.train("O");

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), SIZE + 2);
		assertEquals(result.getMatchCount(), SIZE);
		assertEquals(result.getNullCount(), 1);
		assertEquals(result.getConfidence(), 1 - (double)1/(result.getSampleCount() - result.getNullCount()));
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getMinValue(), "0.0");
		assertEquals(result.getMaxValue(), "999.9");
		assertNull(result.getTypeModifier());
		assertNull(result.getTypeModifier());

		// Old behavior
		// assertEquals(result.getOutlierCount(), 1);

		// New behavior
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getInvalidCount(), 1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.INVALID_SUPPORT })
	public void closedSemanticTypeString() throws IOException, FTAException {
		final TextAnalyzer analyzer = new TextAnalyzer("closedSemanticTypeString");
		final int GOOD_SAMPLES = 400;
		final int BAD_SAMPLES = 6;

		final Map<String, Long> basic = new HashMap<>();
		basic.put("AMERICAN SAMOA", 20L);
		basic.put("BRITISH VIRGIN ISLANDS", 20L);
		basic.put("FALKLAND ISLANDS", 20L);
		basic.put("HONG KONG", 20L);
		basic.put("NEW ZEALAND", 20L);
		basic.put("NORTH KOREA", 20L);
		basic.put("PAPUA NEW GUINEA", 20L);
		basic.put("PUERTO RICO", 20L);
		basic.put("SAINT LUCIA", 20L);
		basic.put("SOUTH AFRICA", 20L);
		basic.put("SOUTH KOREA", 20L);
		basic.put("UNITED KINGDOM", 20L);
		basic.put("UNITED STATES OF AMERICA", 20L);
		basic.put("UNITED ARAB EMIRATES", 20L);
		basic.put("TRINIDAD AND TOBAGO", 20L);
		basic.put("VATICAN CITY", 20L);
		basic.put("VIET NAM", 20L);
		basic.put("WEST BANK", 20L);
		basic.put("WESTERN SAHARA", 20L);
		basic.put("VIRGIN ISLANDS", 20L);

		basic.put("Rubbish that looks like text.", 2L);
		basic.put("Garbage, and other recyclables.", 2L);
		basic.put("Trash, not to be recycled.", 2L);

		analyzer.trainBulk(basic);

		final TextAnalysisResult result = analyzer.getResult();

		assertEquals(result.getSampleCount(), GOOD_SAMPLES + BAD_SAMPLES);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getSemanticType(), "COUNTRY.TEXT_EN");
		assertNull(result.getTypeModifier());
		assertEquals(result.getMatchCount(), GOOD_SAMPLES);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getCardinality(), 20);

		// Old behavior
		// assertEquals(result.getOutlierCount(), 3);

		// New behavior
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getInvalidCount(), 3);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.INVALID_SUPPORT })
	public void openSemanticTypeStringFinite() throws IOException, FTAException {
		final TextAnalyzer analyzer = new TextAnalyzer("openSemanticTypeStringFinite");
		analyzer.setLocale(Locale.US);
		final LogicalTypeCode logical = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByName("NAME.FIRST"), new AnalysisConfig(Locale.US));
		final int SIZE = 1000;

		for (int i = 0; i < SIZE; i++) {
			final String testCase = logical.nextRandom();
			analyzer.train(testCase);
			assertTrue(logical.isValid(testCase));
		}

		// Good - but now known names
		analyzer.train("Aditya");
		assertTrue(logical.isValid("Aditya", false, -1));
		analyzer.train("Zendaya");
		assertTrue(logical.isValid("Zendaya", false, -1));

		analyzer.train(null);
		analyzer.train("9999");
		assertFalse(logical.isValid("9999", false, -1));
		analyzer.train("     ");

		final TextAnalysisResult result = analyzer.getResult();

		assertEquals(result.getSampleCount(), SIZE + 5);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getSemanticType(), "NAME.FIRST");
		assertNull(result.getTypeModifier());
		assertEquals(result.getMatchCount(), SIZE + 2);
		assertEquals(result.getNullCount(), 1);
		assertEquals(result.getBlankCount(), 1);
		assertEquals(result.getRegExp(), "[\\p{IsAlphabetic}]+[- \\p{IsAlphabetic}]*\\.?");

		// Old behavior
		// assertEquals(result.getOutlierCount(), 2);

		// New behavior
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getInvalidCount(), 1);

	}

	@Test(groups = { TestGroups.ALL, TestGroups.INVALID_SUPPORT })
	public void openSemanticTypeStringInfinite() throws IOException, FTAException {
		final TextAnalyzer analyzer = new TextAnalyzer("openSemanticTypeStringInfinite");
		final LogicalTypeCode logical = (LogicalTypeInfinite) LogicalTypeFactory.newInstance(PluginDefinition.findByName("GUID"), new AnalysisConfig());
		final int SIZE = 1000;

		for (int i = 0; i < SIZE; i++) {
			final String testCase = logical.nextRandom();
			analyzer.train(testCase);
			assertTrue(logical.isValid(testCase));
		}

		// The letter 'O' not the number '0'
		analyzer.train("O929998A-96BD-28CC-833C-F77AAFE49E7A");
		assertFalse(logical.isValid("O929998A-96BD-28CC-833C-F77AAFE49E7A", false, -1));

		analyzer.train(null);
		analyzer.train("9999");
		assertFalse(logical.isValid("9999", false, -1));
		analyzer.train("     ");

		final TextAnalysisResult result = analyzer.getResult();

		assertEquals(result.getSampleCount(), SIZE + 4);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getSemanticType(), "GUID");
		assertNull(result.getTypeModifier());
		assertEquals(result.getMatchCount(), SIZE);
		assertEquals(result.getNullCount(), 1);
		assertEquals(result.getBlankCount(), 1);

		// Old behavior
		// assertEquals(result.getOutlierCount(), 2);

		// New behavior
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getInvalidCount(), 2);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.INVALID_SUPPORT })
	public void closedSemanticTypeLong() throws IOException, FTAException {
		final TextAnalyzer analyzer = new TextAnalyzer("closedSemanticTypeLong");
		analyzer.setLocale(Locale.forLanguageTag("en-US"));
		final String inputs[] = TestUtils.validZips.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analyzer.train(inputs[i]) && locked == -1)
				locked = i;
		}

		// Invalid Zip
		analyzer.train("99999");

		analyzer.train(null);

		final TextAnalysisResult result = analyzer.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.LONG);
		final PluginDefinition defn = PluginDefinition.findByName("POSTAL_CODE.ZIP5_US");
		assertEquals(result.getSemanticType(), defn.semanticType);
		assertNull(result.getTypeModifier());
		assertEquals(result.getStructureSignature(), defn.signature);
		assertEquals(result.getSampleCount(), inputs.length + 2);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 1);
		assertEquals(result.getLeadingZeroCount(), 32);
		assertEquals(result.getRegExp(), USZip5.REGEXP_ZIP5);
		assertEquals(result.getConfidence(), 1 - (double)1/(result.getSampleCount() - result.getNullCount()));

		// Old behavior
		// assertEquals(result.getOutlierCount(), 1);

		// New behavior
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getInvalidCount(), 1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.INVALID_SUPPORT })
	public void outlier() throws IOException, FTAException {
		final TextAnalyzer analyzer = new TextAnalyzer("outlier");
		final long SIZE = 1000;
		final Map<String, Long> colors = new HashMap<>();
		colors.put("RED", SIZE);
		colors.put("BLUE", SIZE);
		colors.put("GREEN", SIZE);
		colors.put("GREEEN", 1L);

		analyzer.trainBulk(colors);


		final TextAnalysisResult result = analyzer.getResult();

		assertEquals(result.getSampleCount(), SIZE * 3 + 1);
		assertEquals(result.getMatchCount(), SIZE * 3);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getInvalidCount(), 0);
		assertEquals(result.getOutlierCount(), 1);
		assertEquals(result.getConfidence(), 1 - (double)1/(result.getSampleCount() - result.getNullCount()));
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), "(?i)(BLUE|GREEN|RED)");
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
	}
}
