/*
 * Copyright 2017-2024 Tim Segall
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.cobber.fta.TextAnalyzer.Feature;
import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.FTAUnsupportedLocaleException;
import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.core.RegExpSplitter;

public class TestLongs {
	private static final SecureRandom random = new SecureRandom();
	private Logger logger = LoggerFactory.getLogger("com.cobber.fta");

	public void _variableLengthPositiveInteger(final boolean collectStatistics) throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("_variableLengthPositiveInteger");
		if (!collectStatistics)
			analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		final String[] inputs = "47|909|809821|34590|2|0|12|390|4083|4499045|90|9003|8972|42987|8901".split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,7}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LONG);
		if (collectStatistics) {
			assertEquals(result.getMinValue(), "0");
			assertEquals(result.getMaxValue(), "4499045");
		}
		assertEquals(result.getMinLength(), 1);
		assertEquals(result.getMaxLength(), 7);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void variableLengthPositiveInteger() throws IOException, FTAException {
		_variableLengthPositiveInteger(true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void variableLengthPositiveInteger_ns() throws IOException, FTAException {
		_variableLengthPositiveInteger(false);
	}

	public void _variableLengthInteger(final boolean collectStatistics) throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("_variableLengthInteger");
		if (!collectStatistics)
			analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String[] inputs = "-100000|-1000|-100|-10|-3|-2|-1|100|200|300|400|500|600|1000|10000|601|602|6033|604|605|606|607|608|609|610|911|912|913|914|915".split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "[+-]?\\d{1,6}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTypeModifier(), "SIGNED");
		assertNull(result.getSemanticType());
		if (collectStatistics) {
			assertEquals(result.getMinValue(), "-100000");
			assertEquals(result.getMaxValue(), "10000");
		}
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void variableLengthInteger() throws IOException, FTAException {
		_variableLengthInteger(true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void variableLengthInteger_ns() throws IOException, FTAException {
		_variableLengthInteger(false);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void constantLengthInteger() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("constantLengthInteger");
		final String[] inputs = "456789|456089|456700|116789|433339|409187".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, -1);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{6}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getMinValue(), "116789");
		assertEquals(result.getMaxValue(), "456789");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void trailingMinus() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("trailingMinus");
		final String[] inputs = "458-|123|901|404|209-|12|0|0|676|1894-|2903-|111-|5234".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, -1);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,4}-?");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getMinValue(), "-2903");
		assertEquals(result.getMaxValue(), "5234");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void leadingMinus() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("trailingMinus");
		final String[] inputs = "-458|123|901|404|-209|12|0|0|676|-1894|-2903|-111|5234".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, -1);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "[+-]?\\d{1,4}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getMinValue(), "-2903");
		assertEquals(result.getMaxValue(), "5234");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void similar() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("trailingMinus");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final String[] inputs = { "47", " 47", " 47 ", "47.0", "47.000" };

		for (final String input: inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "[ 	]*\\d*\\.?\\d+[ 	]*");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getMinValue(), "47.0");
		assertEquals(result.getMaxValue(), "47.0");
		assertEquals(result.getCardinality(), 5);
		for (final Map.Entry<String, Long> entry : result.getCardinalityDetails().entrySet()) {
			System.err.printf("Key: %s, Count: %s\n", entry.getKey(), entry.getValue());
		}
		assertEquals(result.getValueAtQuantile(.5), "47");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void leadingZeros() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("BL record ID", null);

		analysis.train("000019284");
		analysis.train("000058669");
		analysis.train("000093929");
		analysis.train("000154545");
		analysis.train("000190188");
		analysis.train("000370068");
		analysis.train("000370069");
		analysis.train("000370070");
		analysis.train("000440716");
		analysis.train("000617304");
		analysis.train("000617305");
		analysis.train("000617306");
		analysis.train("000617307");
		analysis.train("000617308");
		analysis.train("000617309");
		analysis.train("000617310");
		analysis.train("000617311");
		analysis.train("000617312");
		analysis.train("000617314");
		analysis.train("000617315");
		analysis.train("000617316");
		analysis.train("000617317");
		analysis.train("000617318");
		analysis.train("000617319");
		analysis.train("000617324");
		analysis.train("000617325");
		analysis.train("000617326");
		analysis.train("000617331");
		analysis.train("000617335");
		analysis.train("000617336");
		analysis.train("000617337");
		analysis.train("000617338");
		analysis.train("000617339");
		analysis.train("000617342");
		analysis.train("000617347");
		analysis.train("000617348");
		analysis.train("000617349");
		analysis.train("000617350");
		analysis.train("000617351");
		analysis.train("000617354");
		analysis.train("000617355");
		analysis.train("000617356");
		analysis.train("000617357");
		analysis.train("000617358");
		analysis.train("000617359");
		analysis.train("000617360");
		analysis.train("000617361");
		analysis.train("000617362");
		analysis.train("000617363");
		analysis.train("000617364");
		analysis.train("000617365");
		analysis.train("000617366");
		analysis.train("000617368");
		analysis.train("000617369");
		analysis.train("000617370");
		analysis.train("000617371");
		analysis.train("000617372");
		analysis.train("000617373");
		analysis.train("000617374");
		// This one is a duplicate - so we do not detect field as an IDENTIFIER
		analysis.train("000617331");

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), 60);
		assertEquals(result.getMatchCount(), 60);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 60);
		assertEquals(result.getRegExp(), "\\d{9}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void leadingZerosWith0() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("BL record ID", null);

		analysis.train("0");
		analysis.train("1");
		analysis.train("2");
		analysis.train("3");
		analysis.train("0");
		analysis.train("2");
		analysis.train("10");

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), 7);
		assertEquals(result.getMatchCount(), 7);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getRegExp(), "\\d{1,2}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void testBuggyEAN() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("BL record ID", null);

		for (int i = 0; i < 6; i++)
			analysis.train("31");
		for (int i = 0; i < 24; i++)
			analysis.train("");

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), 30);
		assertEquals(result.getMatchCount(), 6);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void testUniqueness() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("testUniqueness", null);
		final int tooBig = analysis.getMaxCardinality() - 1;

		for (int i = 0; i < tooBig; i++)
			analysis.train(String.valueOf(i));

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeModifier());
		assertEquals(result.getSemanticType(), "IDENTIFIER");
		assertEquals(result.getSampleCount(), tooBig);
		assertEquals(result.getMatchCount(), tooBig);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getUniqueness(), 1.0);
		assertEquals(result.getDistinctCount(), tooBig);
		assertEquals(result.getRegExp(), "\\d{1,5}");
		assertEquals(result.getConfidence(), 0.99);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void testWithNull() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;
		final TextAnalyzer analysis = new TextAnalyzer("testWithNull", null);

		for (int i = 0; i < SAMPLE_COUNT; i++)
			analysis.train(String.valueOf(i));
		analysis.train(null);
		analysis.train(" ");

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), SAMPLE_COUNT + 2);
		assertEquals(result.getMatchCount(), SAMPLE_COUNT);
		assertEquals(result.getNullCount(), 1);
		assertEquals(result.getBlankCount(), 1);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getUniqueness(), 1.0);
		assertEquals(result.getDistinctCount(), SAMPLE_COUNT);
		assertEquals(result.getRegExp(), "\\d{1,2}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void testUniquenessBlown() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("testUniquenessBlown", null);
		final int tooBig = analysis.getMaxCardinality();

		for (int i = 10; i < tooBig + 10; i++)
			analysis.train(String.valueOf(i));
		analysis.train("1");

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), tooBig + 1);
		assertEquals(result.getMatchCount(), tooBig + 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getUniqueness(), -1.0);
		assertEquals(result.getDistinctCount(), -1);
		assertEquals(result.getRegExp(), "\\d{1,5}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void testMonotonicIncreasing() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("testMonotonicIncreasing", null);
		final int tooBig = 2 * analysis.getMaxCardinality();

		for (int i = 0; i < 2 * tooBig; i++)
			analysis.train(String.valueOf(i));

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeModifier());
		assertEquals(result.getSemanticType(), "IDENTIFIER");
		assertEquals(result.getSampleCount(), 2 * tooBig);
		assertEquals(result.getMatchCount(), 2 * tooBig);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getUniqueness(), 1.0);
		assertEquals(result.getDistinctCount(), 2 * tooBig);
		assertEquals(result.getRegExp(), "\\d{1,5}");
		assertEquals(result.getConfidence(), 0.99);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void testMonotonicDecreasing() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("testMonotonicDecreasing", null);
		final int tooBig = 2 * analysis.getMaxCardinality();

		for (int i = 2 * tooBig; i > 0; i--)
			analysis.train(String.valueOf(i));

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeModifier());
		assertEquals(result.getSemanticType(), "IDENTIFIER");
		assertEquals(result.getSampleCount(), 2 * tooBig);
		assertEquals(result.getMatchCount(), 2 * tooBig);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getUniqueness(), 1.0);
		assertEquals(result.getDistinctCount(), 2 * tooBig);
		assertEquals(result.getRegExp(), "\\d{1,5}");
		assertEquals(result.getConfidence(), 0.99);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void testUniquenessNone() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("testUniquenessNone", null);
		final int tooBig = analysis.getMaxCardinality() - 1;

		for (int i = 0; i < tooBig; i++) {
			analysis.train(String.valueOf(i));
			analysis.train(String.valueOf(i));
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), tooBig * 2);
		assertEquals(result.getMatchCount(), tooBig * 2);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getUniqueness(), 0.0);
		assertEquals(result.getDistinctCount(), tooBig);
		assertEquals(result.getRegExp(), "\\d{1,5}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void testUniquenessExternal() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("testUniquenessExternal", null);
		final int tooBig = 2 * analysis.getMaxCardinality();

		analysis.setUniqueness(1.0);
		analysis.setDistinctCount(2 * tooBig + 1);

		for (int i = 0; i < 2 * tooBig; i++)
			analysis.train(String.valueOf(i));
		analysis.train("-1");

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTypeModifier(), "SIGNED");
		assertEquals(result.getSemanticType(), "IDENTIFIER");
		assertEquals(result.getSampleCount(), 2 * tooBig + 1);
		assertEquals(result.getMatchCount(), 2 * tooBig + 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getUniqueness(), 1.0);
		assertEquals(result.getDistinctCount(), 2 * tooBig + 1);
		assertEquals(result.getRegExp(), "[+-]?\\d{1,5}");
		assertEquals(result.getConfidence(), 0.99);
		assertNull(result.checkCounts());

		TestSupport.dumpRaw(result.getHistogram(10));
		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void groupingSeparatorLarge() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Separator");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final int SAMPLE_SIZE = 10000;
		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;
		String minValue = String.valueOf(min);
		String maxValue = String.valueOf(max);
		final Set<String> samples = new HashSet<>();
		final NumberFormat longFormatter = NumberFormat.getNumberInstance(Locale.US);

		for (int i = 0; i < SAMPLE_SIZE; i++) {
			final long l = random.nextInt(100000000);
			final String sample = longFormatter.format(l);
			if (l < min) {
				min = l;
				minValue = sample;
			}
			if ( l > max) {
				max = l;
				maxValue = sample;
			}
			samples.add(sample);
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTypeModifier(), "GROUPING");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), SAMPLE_SIZE);
		assertEquals(result.getMatchCount(), SAMPLE_SIZE);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getMinValue(), longFormatter.format(min));
		assertEquals(result.getMaxValue(), longFormatter.format(max));
		String regExp = "[\\d,]{";
		if (minValue.length() == maxValue.length())
			regExp += minValue.length();
		else {
			regExp += minValue.length() + "," + maxValue.length();
		}
		regExp += "}";
		assertEquals(result.getRegExp(), regExp);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(regExp), sample);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void groupingSeparatorLargeFRENCH() throws IOException, FTAException {
		final Locale locales[] = { Locale.GERMAN, Locale.FRANCE };
		final int SAMPLE_SIZE = 1000;
		final Set<String> samples = new HashSet<>();

		for (final Locale locale : locales) {
			long min = Long.MAX_VALUE;
			long absMin = Long.MAX_VALUE;
			long max = Long.MIN_VALUE;
			String minValue = String.valueOf(min);
			String maxValue = String.valueOf(max);
			final NumberFormat nf = NumberFormat.getNumberInstance(locale);
			final TextAnalyzer analysis = new TextAnalyzer("Separator");
			analysis.setLocale(locale);
			samples.clear();

			for (int i = 0; i < SAMPLE_SIZE; i++) {
				long l = random.nextInt(100000000);
				if (l%2 == 0)
					l = -l;
				final String sample = nf.format(l);
				if (l < min) {
					min = l;
				}
				if (Math.abs(l) < absMin) {
					absMin = Math.abs(l);
					minValue = sample;
				}
				if (l < min) {
					min = l;
				}
				if (l > max) {
					max = l;
					maxValue = sample;
				}
				samples.add(sample);
				analysis.train(sample);
			}

			final TextAnalysisResult result = analysis.getResult();
			TestUtils.checkSerialization(analysis);

			assertEquals(result.getType(), FTAType.LONG);
			assertEquals(result.getTypeModifier(), "SIGNED,GROUPING", locale.toString());
			assertNull(result.getSemanticType());
			assertEquals(result.getSampleCount(), SAMPLE_SIZE);
			assertEquals(result.getMatchCount(), SAMPLE_SIZE);
			assertEquals(result.getNullCount(), 0);
			assertEquals(result.getLeadingZeroCount(), 0);
			assertEquals(result.getMinValue(), nf.format(min));
			assertEquals(result.getMaxValue(), nf.format(max));
			final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);

			String regExp = "[+-]?[\\d" + RegExpGenerator.slosh(formatSymbols.getGroupingSeparator()) + "]";
			final int minLength = minValue.charAt(0) == '-' ? minValue.length() - 1 : minValue.length();
			regExp += RegExpSplitter.qualify(minLength, maxValue.length());
			assertEquals(result.getRegExp(), regExp);
			assertEquals(result.getConfidence(), 1.0);
			assertNull(result.checkCounts());

			TestSupport.checkHistogram(result, 10, true);
			TestSupport.checkQuantiles(result);

			for (final String sample : samples)
				assertTrue(sample.matches(regExp), sample);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void localizedPortugueseLong() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("localizedPortugueseLong");
		// For Portuguese, Decimal Sep = ',' and Thousands Sep = '.'
		final Locale locale = Locale.forLanguageTag("pt-BR");
		analysis.setLocale(locale);

		final String[] inputs = {
				"46.448", "6.341.288", "543.022", "636.666", "61.606.330",
				"64.425", "109.089", "57.995", "4.773.826", "23.498.620",
				"43.391", "1.356.902", "22.039", "2.526.587", "33.113.104",
				"221.887", "6.313.005", "879.865", "84.369.774"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTypeModifier(), "GROUPING");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "[\\d\\.]{6,10}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "22.039");
		assertEquals(result.getMaxValue(), "84.369.774");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void localizedGermanLong() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("localizedGermanLong");
		final Locale locale = Locale.forLanguageTag("de-DE");
		analysis.setLocale(locale);

		final String[] inputs = {
	            "1.234.567.890.123", "-1234567890123", "   51.000", "1.000.000.000.000",
	            "+11.123.000", "     ",  "  ", "  -12000   "
		};

		for (final String input : inputs)
			analysis.train(input);
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTypeModifier(), "SIGNED,GROUPING");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length + 1);
		assertEquals(result.getBlankCount(), 2);
		assertEquals(result.getMatchCount(), inputs.length - result.getBlankCount());
		assertEquals(result.getNullCount(), 1);
		assertEquals(result.getRegExp(), "[ 	]*[+-]?[\\d\\.]{5,17}[ 	]*");
		assertEquals(result.getMinValue(), "-1.234.567.890.123");
		assertEquals(result.getMaxValue(), "1.234.567.890.123");
		assertEquals(result.getCardinality(), 6);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			if (input == null || input.trim().isEmpty())
				continue;
			assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	// BROKEN @Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void trailingMinusArEH() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Separator");
		final Locale locale = Locale.forLanguageTag("ar-EH");
		analysis.setLocale(locale);

		final String[] samples = {
				"1", "2", "3", "4", "5", "6", "7", "8", "9",
				"1000-", "12", "13", "156", "209", "22012-", "40",
				"489", "932", "98", "12", "333304", "2", "12", "178",
				"95","83"
		};

//		final NumberFormat nf = NumberFormat.getNumberInstance(locale);
//		final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);

		for (final String sample : samples) {
			analysis.train(sample);
			System.err.println(String.valueOf(sample));
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTypeModifier(), "SIGNED,GROUPING");
		assertNull(result.getSemanticType());
		assertEquals(result.getRegExp(), "[\\d,]{21,25}[+-]?");
		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getMatchCount(), samples.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void localeLongTest() throws IOException, FTAException {
		final int SAMPLE_SIZE = 1000;
		final Locale[] locales = DateFormat.getAvailableLocales();
//		Locale[] locales = new Locale[] { Locale.forLanguageTag("tok") };

		for (final Locale locale : locales) {
			long min = Long.MAX_VALUE;
			long absMin = Long.MAX_VALUE;
			long max = Long.MIN_VALUE;
			long absMax = Long.MIN_VALUE;
			String absMinValue = String.valueOf(absMin);
			String absMaxValue = String.valueOf(max);
			final TextAnalyzer analysis = new TextAnalyzer("Separator");
			analysis.setLocale(locale);

//			final String languageTag = locale.toLanguageTag();
//			if (languageTag.indexOf('-') != languageTag.lastIndexOf('-')) {
//				logger.debug("Skipping locale '{}' as it has a script.", locale);
//				continue;
//			}

			final boolean simple = NumberFormat.getNumberInstance(locale).format(0).matches("\\d");

			if (!simple) {
//				logger.debug("Skipping locale '{}' as it does not use Arabic numerals.", locale);
				continue;
			}

			final Calendar cal = GregorianCalendar.getInstance(locale);
			if (!(cal instanceof GregorianCalendar)) {
//				logger.debug("Skipping locale '{}' as it does not use the Gregorian calendar.", locale);
				continue;
			}

			final String variant = locale.getDisplayVariant();
			if (variant != null && !variant.isEmpty()) {
//				logger.debug("Skipping locale '{}' as it has a Variant: '{}'.", locale, variant);
				continue;
			}

			final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);

			final String negPrefix = TestUtils.getNegativePrefix(locale);
			final String negSuffix = TestUtils.getNegativeSuffix(locale);

			if (negPrefix.isEmpty() && negSuffix.isEmpty()) {
				logger.debug("Skipping locale '{}' as it has empty negPrefix and negSuffix.", locale);
				continue;
			}

			final int groupingSize = TestUtils.getGroupingSize(locale);
			if (groupingSize != -1 && groupingSize != 3) {
				logger.debug("Skipping locale '{}' as grouping is used and grouping size = {}", locale, groupingSize);
				continue;
			}

			final Set<String> samples = new HashSet<>();
			final NumberFormat nf = NumberFormat.getIntegerInstance(locale);

//			logger.debug("Locale {}, negPrefix: {}, negSuffix: {}, groupingSize: {}, min: {}, max: {}, absMax: {}.",
//					locale.toLanguageTag(), negPrefix, negSuffix, groupingSize, String.valueOf(min), String.valueOf(max), absMinValue);

			try {
				for (int i = 0; i < SAMPLE_SIZE; i++) {
					long l = random.nextLong();
					if (l % 2 == 0)
						l = -l;
					final String sample = nf.format(l);
					if (KnownTypes.LEFT_TO_RIGHT_MARK == sample.charAt(0))
						throw new FTAUnsupportedLocaleException("Locale uses Left-to-right Mark");

					if (l < min) {
						min = l;
					}
					if (l > max) {
						max = l;
					}
					if (Math.abs(l) < absMin) {
						absMin = Math.abs(l);
						absMinValue = nf.format(Math.abs(l));
					}
					if (Math.abs(l) > absMax) {
						absMax = Math.abs(l);
						absMaxValue = nf.format(Math.abs(l));
					}

					samples.add(sample);
					analysis.train(sample);
				}
			}
			catch (FTAUnsupportedLocaleException e) {
				logger.debug("Skipping locale '{}' = reason: '{}'.", locale, e.getMessage());
				continue;
			}

			final TextAnalysisResult result = analysis.getResult();
			TestUtils.checkSerialization(analysis);

			assertEquals(result.getType(), FTAType.LONG);
			assertEquals(result.getTypeModifier(), "SIGNED,GROUPING", locale.toLanguageTag());
			assertNull(result.getSemanticType());
			assertEquals(result.getSampleCount(), SAMPLE_SIZE);
			assertEquals(result.getMatchCount(), SAMPLE_SIZE);
			assertEquals(result.getNullCount(), 0);
			assertEquals(result.getMinValue(), nf.format(min));
			assertEquals(result.getMaxValue(), nf.format(max));
			assertEquals(result.getLeadingZeroCount(), 0);
			assertNull(result.checkCounts());

			String regExp = "";
			if (!negPrefix.isEmpty())
				regExp += negPrefix;
			regExp += "[\\d" + RegExpGenerator.slosh(formatSymbols.getGroupingSeparator()) + "]";
			regExp += RegExpSplitter.qualify(absMinValue.length(), absMaxValue.length());
			if (!negSuffix.isEmpty())
				regExp += negSuffix;
			assertEquals(result.getDecimalSeparator(), '.');

			assertEquals(result.getRegExp(), regExp, locale.toLanguageTag());
			assertEquals(result.getConfidence(), 1.0);

			TestSupport.checkHistogram(result, 10, true);
			TestSupport.checkQuantiles(result);

			for (final String sample : samples)
				assertTrue(sample.matches(regExp), sample + " " + regExp);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void someInts() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("someInts");
		assertTrue(analysis.isEnabled(TextAnalyzer.Feature.NUMERIC_WIDENING));
		analysis.configure(TextAnalyzer.Feature.NUMERIC_WIDENING, false);
		assertFalse(analysis.isEnabled(TextAnalyzer.Feature.NUMERIC_WIDENING));
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		int minLength = Integer.MAX_VALUE;
		int maxLength = Integer.MIN_VALUE;
		int locked = -1;
		int samples;
		int bad = 0;

		for (samples = 0; samples <= AnalysisConfig.DETECT_WINDOW_DEFAULT; samples++) {
			final String input = String.valueOf(random.nextInt(1000000));
			final int len = input.length();
			if (len < minLength)
				minLength = len;
			if (len > maxLength)
				maxLength = len;
			if (analysis.train(input) && locked == -1)
				locked = samples;
		}

		analysis.train(String.valueOf(random.nextDouble()));
		bad++;

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), samples + 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.LONG);
		String pattern = "\\d{" + minLength;
		if (maxLength != minLength) {
			pattern += "," + maxLength;
		}
		pattern += "}";
		assertEquals(result.getRegExp(), pattern);
		assertEquals(result.getConfidence(), 1 - (double)bad/result.getSampleCount());
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void manyConstantLengthLongs() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("manyConstantLengthLongs");
		final int nullIterations = 50;
		final int iterations = 2 * AnalysisConfig.MAX_CARDINALITY_DEFAULT;
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
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), iterations + nullIterations);
		assertEquals(result.getCardinality(), AnalysisConfig.MAX_CARDINALITY_DEFAULT);
		assertEquals(result.getNullCount(), nullIterations);
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getRegExp(), "\\d{10}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void testEpochSeconds() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Timestamp");

		final String[] samples = {
				"1672431100", "1672431101", "1621458902", "1672432103", "1672431104", "1672432105",
				"1672431206", "1672431107", "1672431108", "1672432009", "1670531110", "1672431111",
				"1671533412", "1572431113", "1672431114", "1672431115", "1621458916", "1672432117",
				"1672431118", "1672432119", "1672431220", "1672431121", "1672431122", "1672432023",
				"1670531124", "1672431125", "1671533426", "1572531127"
		};

		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertTrue(result.isSemanticType());
		assertEquals(result.getSemanticType(), "EPOCH.SECONDS");
		assertEquals(result.getRegExp(), "\\d{10}");
		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getMatchCount(), samples.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void testEpochMilliseconds() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Timestamp");

		final String[] samples = {
				"1672431100000", "1672431101000", "1621458902000", "1672432103000", "1672431104000", "1672432105000",
				"1672431206000", "1672431107000", "1672431108000", "1672432009000", "1670531110000", "1672431111000",
				"1671533412000", "1572431113000", "1672431114000", "1672431115000", "1621458916000", "1672432117000",
				"1672431118000", "1672432119000", "1672431220000", "1672431121000", "1672431122000", "1672432023000",
				"1670531124000", "1672431125000", "1671533426000", "1572531127000"
		};

		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertTrue(result.isSemanticType());
		assertEquals(result.getSemanticType(), "EPOCH.MILLISECONDS");
		assertEquals(result.getRegExp(), "\\d{13}");
		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getMatchCount(), samples.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void testLongLogicalType() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("PARTITA IVA");
		analysis.setLocale(Locale.ITALIAN);
		analysis.setPluginThreshold(95);
		analysis.setDebug(2);

		final String[] samples = {
				"01497781003", "01243801006", "01763561006", "02151151004", "01322551001",
				"03919071005", "01587761006", "05497891001", "00985491000", "01146421001",
				"01869671006", "01869671006", "01869671006", "01028501003", "01321971002",
				"01320371006", "02150831002", "04505241002", "01220551004", "01030121006",
				"01054891005", "07543541002", "07451591007", "04212731006", "01174991008",
				"01428411001", "02077861009", "01037841002", "09452921001", "01004641005",
				"1.0752770585E10"
		};

		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertTrue(result.isSemanticType());
		assertEquals(result.getSemanticType(), "CHECKDIGIT.LUHN");
//BUG TODO		assertEquals(result.getRegExp(), "\\d{13}");
		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getMatchCount(), samples.length - 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), samples.length - 1);
		assertEquals(result.getCardinality(), 28);
		assertEquals(result.getInvalidCount(), 1);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMinValue(), "985491000");
		assertEquals(result.getMaxValue(), "9452921001");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void paddedLongs() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("RowID");
		final String inputs[] = {
				"    0", "    1", "    2", "    3", "    4", "    5", "    6", "    7", "    8", "    9",
				"    10", "    11", "    12", "    13", "    14", "    15", "    16", "    17", "    18", "    19",
				"    20", "    21", "    22", "    23", "    24", "    25", "    11", "    27", "    28", "    29"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getMinValue(), "0");
		assertEquals(result.getMaxValue(), "29");
		assertEquals(result.getRegExp(), KnownTypes.PATTERN_WHITESPACE + "\\d{1,2}");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void someLongs() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("stringField");
		analysis.setThreshold(90);
		final String inputs[] = {
				"12", "baz", "boo", "1234", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "10" };
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getRegExp(), "\\d{1,4}");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getConfidence(), 0.9166666666666666);
		assertEquals(result.getMatchCount(), inputs.length - 2);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}


	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void signedLongs() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("SUB_ACTIVE_DATE_ONLY");
		final String inputs[] = {
				"+400089", "2000931", "-3287392873", "-327398267", "-34", "56", "93823908", "34567", "-757363", "0",
				"4345689", "2333931", "4457892873", "+398267", "-3334464", "78912356", "93823908", "34567", "-757363", "0",
				"489", "931", "-3287373", "-398267", "-234534", "565656", "23908", "7734567", "-99757363", "0"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTypeModifier(), "SIGNED");
		assertNull(result.getSemanticType());
		assertEquals(result.getRegExp(), "[+-]?\\d{1,10}");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void manyKnownInts() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("manyKnownInts");
		final int nullIterations = 50;
		final int iterations = 100000;
		int locked = -1;

		for (int i = 0; i < nullIterations; i++) {
			analysis.train(null);
		}
		for (int i = 0; i < iterations; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i;
		}
		analysis.train("  ");
		analysis.train("    ");

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), iterations + nullIterations + 2);
		assertEquals(result.getCardinality(), AnalysisConfig.MAX_CARDINALITY_DEFAULT);
		assertEquals(result.getNullCount(), nullIterations);
		assertEquals(result.getBlankCount(), 2);
		assertEquals(result.getRegExp(), "\\d{1,5}");
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "0");
		assertEquals(result.getMaxValue(), String.valueOf(iterations - 1));
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void groupingSeparator() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Separator");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final String pipedInput = "3600|7500|3600|3600|800|3600|1200|1200|600|" +
				"1200|1200|1200|1200|3600|1200|13,000|1200|200|" +
				"1200|1200|1200|1200|1200|1200|1200|1200|200|" +
				"1200|3600|1200|1200|1200|1200|1200|1200|200|" +
				"1200|1200|1200|1200|1200|1200|1200|1200|200|" +
				"1200|3600|1200|1200|1200|1200|3600|1200|600|" +
				"1200|1200|1200|1200|3600|1200|13,000|1200|200|" +
				"1200|1200|1200|1200|1200|1200|1200|1200|200|" +
				"1200|1200|1200|1200|3600|1200|1200|1200|200|" +
				"3600|1200|1200|1200|1200|1200|1200|1200|200|" +
				"1200|1200|1200|3600|3600|1200|1200|1200|200|";

		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTypeModifier(), "GROUPING");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getRegExp(), "[\\d,]{3,6}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "200");
		assertEquals(result.getMaxValue(), "13,000");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		final String regExp = result.getRegExp();
		for (final String input : inputs)
			assertTrue(input.matches(regExp), input);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void groupingSeparatorSigned() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Separator");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final int SAMPLE_SIZE = 100;
		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;
		long absMin = Long.MAX_VALUE;
		long absMax = 0;
		String minValue = String.valueOf(Long.MAX_VALUE);
		String maxValue = "0";
		final Set<String> samples = new HashSet<>();
		final NumberFormat longFormatter = NumberFormat.getNumberInstance(Locale.US);

		for (int i = 0; i < SAMPLE_SIZE; i++) {
			long l = random.nextInt(100000000);
			if (random.nextBoolean())
				l *= -1;
			if (l < min) {
				min = l;
			}
			if (l > max) {
				max = l;
			}
			final String sample = longFormatter.format(l);
			final long pos = Math.abs(l);
			if (pos < absMin) {
				absMin = pos;
				minValue = longFormatter.format(pos);
			}
			if (pos > absMax) {
				absMax = pos;
				maxValue = longFormatter.format(pos);
			}
			samples.add(sample);
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTypeModifier(), "SIGNED,GROUPING");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), SAMPLE_SIZE);
		assertEquals(result.getMatchCount(), SAMPLE_SIZE);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getMinValue(), longFormatter.format(min));
		assertEquals(result.getMaxValue(), longFormatter.format(max));
		String regExp = "[+-]?[\\d,]{";
		if (minValue.length() == maxValue.length())
			regExp += minValue.length();
		else {
			regExp += minValue.length() + "," + maxValue.length();
		}
		regExp += "}";
		assertEquals(result.getRegExp(), regExp);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(regExp), sample);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void testQualifierNumeric() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Numeric");
		analysis.configure(Feature.LENGTH_QUALIFIER, false);

		final int start = 10000;
		final int end = 99999;

		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getRegExp(), "\\d+");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void justSimple() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("justSimple");

		final int iterations = 100_000_000;

		final Map<String, Long> input = new HashMap<>();
		for (int i = 0; i < 100; i++)
			input.put(String.valueOf(i), 1_000_000L);
		analysis.trainBulk(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), iterations);
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void manyRandomInts() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("manyRandomInts");
		final int nullIterations = 50;
		final int iterations = AnalysisConfig.MAX_CARDINALITY_DEFAULT + 100;
		int locked = -1;

		for (int i = 0; i < nullIterations; i++) {
			analysis.train(null);
		}
		for (int i = 0; i < iterations; i++) {
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), iterations + nullIterations);
		assertEquals(result.getCardinality(), AnalysisConfig.MAX_CARDINALITY_DEFAULT);
		assertEquals(result.getNullCount(), nullIterations);
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void longToSigned() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("LongToSigned");
		final int SAMPLE_SIZE = 100;

		for (int i = 0; i < SAMPLE_SIZE; i++)
			analysis.train(String.valueOf(i));
		analysis.train("-1");

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTypeModifier(), "SIGNED");
		assertEquals(result.getSemanticType(), "IDENTIFIER");
		assertEquals(result.getSampleCount(), SAMPLE_SIZE + 1);
		assertEquals(result.getMatchCount(), SAMPLE_SIZE + 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void dutchLocalizedLong() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("D");
		// For Dutch, Decimal Sep = ',' and Thousands Sep = '.'
		final Locale locale = Locale.forLanguageTag("nl-NL");
		analysis.setLocale(locale);
		analysis.setDebug(2);

		final String[] inputs = {
				"1.234", "8.078", "1.664", "12.902", "122.987",
				"120.809", "12.036", "121.647", "120.904", "120.707",
				"105.841", "1.525", "129.605", "1.895", "12.187",
				"12.845", "1.962", "109.736", "120.509",
				"1.201.685",
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTypeModifier(), "GROUPING");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "[\\d\\.]{5,9}");
		assertEquals(result.getMinValue(), "1.234");
		assertEquals(result.getMaxValue(), "1.201.685");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	// This is questionable ... probably should be a non-localized Long with Grouping
	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void dutchNonLocalizedLongWithGrouping() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("D");
		// For Dutch, Decimal Sep = ',' and Thousands Sep = '.'
		final Locale locale = Locale.forLanguageTag("nl-NL");
		analysis.setLocale(locale);
		analysis.setDebug(2);

		final String[] inputs = {
				"1,234,900", "8,078", "1,664", "12,902", "122,987",
				"120,809", "12,036", "121,647", "120,904", "120,707",
				"105,841", "1,525", "129,605", "1,895", "12,187",
				"12,845", "1,962", "109,736", "120,509",
				"1,201,685",
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length - 2);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d*,?\\d+");
		assertEquals(result.getMinValue(), "1,525");
		assertEquals(result.getMaxValue(), "129,605");
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void meanSD() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("meanSD");
		final int SAMPLE_SIZE = 100;

		for (int i = 0; i < SAMPLE_SIZE; i++) {
			analysis.train("2");
			analysis.train("1");
			analysis.train("3");
			analysis.train("2");
			analysis.train("4");
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getSampleCount(), 5 * SAMPLE_SIZE);
		assertEquals(result.getMatchCount(), 5 * SAMPLE_SIZE);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getMean(), 2.4, TestUtils.EPSILON);
		assertEquals(result.getStandardDeviation(), 1.019803903, TestUtils.EPSILON);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void meanSDBulk() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("meanSDBulk");
		final long SAMPLE_SIZE = 100;

		final Map<String, Long> data = new HashMap<>();
		data.put("2", SAMPLE_SIZE * 2);
		data.put("1", SAMPLE_SIZE);
		data.put("3", SAMPLE_SIZE);
		data.put("4", SAMPLE_SIZE);
		analysis.trainBulk(data);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), 5 * SAMPLE_SIZE);
		assertEquals(result.getMatchCount(), 5 * SAMPLE_SIZE);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getMean(), 2.4, TestUtils.EPSILON);
		assertEquals(result.getStandardDeviation(), 1.019803903, TestUtils.EPSILON);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void meanSDBulkMerge() throws IOException, FTAException {
		final TextAnalyzer shardOne = new TextAnalyzer("meanSDBulkMerge");
		final TextAnalyzer shardTwo = new TextAnalyzer("meanSDBulkMerge");
		final long SAMPLE_SIZE = 100;

		final Map<String, Long> data = new HashMap<>();

		data.put("2", SAMPLE_SIZE * 2);
		data.put("1", SAMPLE_SIZE);
		shardOne.trainBulk(data);

		data.clear();
		data.put("3", SAMPLE_SIZE);
		data.put("4", SAMPLE_SIZE);
		shardTwo.trainBulk(data);

		final TextAnalyzer merged = TextAnalyzer.merge(shardOne, shardTwo);
		final TextAnalysisResult result = merged.getResult();

		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), 5 * SAMPLE_SIZE);
		assertEquals(result.getMatchCount(), 5 * SAMPLE_SIZE);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getMean(), 2.4, TestUtils.EPSILON);
		assertEquals(result.getStandardDeviation(), 1.019803903, TestUtils.EPSILON);
		assertNull(result.checkCounts());
		dump(result);

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void meanSDCardinalityExceeded() throws IOException, FTAException {
		final int MAX_CARDINALITY = 40;
		final TextAnalyzer analysis = new TextAnalyzer("meanSDCardinalityExceeded");
		analysis.setMaxCardinality(MAX_CARDINALITY);
		analysis.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);
		final int SAMPLE_SIZE = 100;

		for (int i = 1; i < SAMPLE_SIZE; i++)
			analysis.train(String.valueOf(i));

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeModifier());
		assertEquals(result.getSemanticType(), "IDENTIFIER");
		assertEquals(result.getSampleCount(), SAMPLE_SIZE - 1);
		assertEquals(result.getMatchCount(), SAMPLE_SIZE - 1);
		assertEquals(result.getMean(), 50.0, TestUtils.EPSILON);
		assertEquals(result.getStandardDeviation(), 28.57738033, TestUtils.EPSILON);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void meanSDBulkCardinalityExceeded() throws IOException, FTAException {
		final int MAX_CARDINALITY = 40;
		final TextAnalyzer analysis = new TextAnalyzer("meanSDBulkCardinalityExceeded");
		analysis.setMaxCardinality(MAX_CARDINALITY);
		analysis.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);
		final long SAMPLE_SIZE = 100;

		final Map<String, Long> data = new HashMap<>();
		for (int i = 1; i < SAMPLE_SIZE; i++)
			data.put(String.valueOf(i), 1L);
		data.put("x", 1L);
		analysis.trainBulk(data);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), SAMPLE_SIZE);
		assertEquals(result.getMatchCount(), SAMPLE_SIZE - 1);
		assertEquals(result.getMean(), 50.0, TestUtils.EPSILON);
		assertEquals(result.getStandardDeviation(), 28.57738033, TestUtils.EPSILON);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void meanSDBulkMergeCardinalityExceeded() throws IOException, FTAException {
		final int MAX_CARDINALITY = 40;
		final TextAnalyzer shardOne = new TextAnalyzer("shardOne");
		shardOne.setMaxCardinality(MAX_CARDINALITY);
		shardOne.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);

		final TextAnalyzer shardTwo = new TextAnalyzer("shardTwo");
		shardTwo.setMaxCardinality(MAX_CARDINALITY);
		shardTwo.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);
		final int SAMPLE_SIZE = 100;

		final TextAnalyzer shardReference = new TextAnalyzer("shardReference");

		final Map<String, Long> data = new TreeMap<>();

		// Add 1-99 + 'x' to shard One which has a Max Cardinality of 40
		for (int i = 1; i < SAMPLE_SIZE; i++) {
			shardOne.train(String.valueOf(i));
			data.put(String.valueOf(i), 1L);
		}
		shardOne.train("x");
		data.put("x", 1L);
		shardOne.setTotalCount(SAMPLE_SIZE);
		shardReference.trainBulk(data);

		// Add 1-99 to shard Two which has a Max Cardinality of 40
		data.clear();
		for (int i = 1; i < SAMPLE_SIZE; i++)
			data.put(String.valueOf(i), 1L);
		shardTwo.trainBulk(data);
		shardTwo.setTotalCount(data.size());
		shardReference.trainBulk(data);

		final TextAnalyzer merged = TextAnalyzer.merge(shardOne, shardTwo);
		final TextAnalysisResult result = merged.getResult();

		final TextAnalysisResult referenceResult = shardReference.getResult();

		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getMean(), 50.0, TestUtils.EPSILON);
		assertEquals(result.getStandardDeviation(), 28.57738033, TestUtils.EPSILON);
		assertEquals(referenceResult.getStandardDeviation(), 28.57738033, TestUtils.EPSILON);
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, false);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void meanSDCardinalityExceeded2() throws IOException, FTAException {
		final TextAnalyzer shardOne = new TextAnalyzer("shardOne");
		final TextAnalyzer shardTwo = new TextAnalyzer("shardTwo");

		for (int i = 1; i < 20000; i++) {
			shardOne.train(String.valueOf(i));
			shardOne.train(null);
		}
		shardOne.setTotalCount(2 * (20000 - 1));
		final TextAnalysisResult shardOneResult = shardOne.getResult();
		dump(shardOneResult);

		for (int i = 20001; i < 40000; i++)
			shardTwo.train(String.valueOf(i));
		shardTwo.setTotalCount(40000 - 20001);
		final TextAnalysisResult shardTwoResult = shardTwo.getResult();
		dump(shardTwoResult);

		final TextAnalyzer merged = TextAnalyzer.merge(shardOne, shardTwo);
		final TextAnalysisResult mergedResult = merged.getResult();
		dump(mergedResult);

		assertEquals((shardOneResult.getMean() + shardTwoResult.getMean())/2, mergedResult.getMean(), .1);
		assertEquals(shardOneResult.getStandardDeviation(), shardTwoResult.getStandardDeviation(), .5);
		assertEquals(shardOneResult.getStandardDeviation(), mergedResult.getStandardDeviation(), .5);

		TestSupport.checkHistogram(mergedResult, 10, false);
		TestSupport.checkQuantiles(mergedResult);
	}

	private void dump(final TextAnalysisResult result) {
		System.err.printf("%s: Total: %d, Real Samples: %d, Matches: %d, Mean: %f, Standard Deviation: %.10f%n",
				result.getName(), result.getTotalCount(),
				result.getSampleCount() - result.getNullCount() - result.getBlankCount(), result.getMatchCount(),
				result.getMean(), result.getStandardDeviation());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void uniqueness() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("ID");
		final int SAMPLE_SIZE = 1000;

		for (int i = 0; i < SAMPLE_SIZE; i++)
			analysis.train(String.valueOf(i));
		analysis.train(null);
		analysis.train(null);
		analysis.train(" ");
		analysis.train("  ");

		analysis.setTotalCount(1000);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), SAMPLE_SIZE + 4);
		assertEquals(result.getMatchCount(), SAMPLE_SIZE);
		assertEquals(result.getNullCount(), 2);
		assertEquals(result.getBlankCount(), 2);
		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getSemanticType());
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getKeyConfidence(), 0.0);
		assertEquals(result.getUniqueness(), 1.0);
	}


	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void noStatistics() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("noStatistics");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String[] samples = new String[10000];

		int iters;
		for (iters = 0; iters < samples.length; iters++) {
			analysis.train(String.valueOf(random.nextInt(100000000)));
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), iters);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getMaxLength(), 8);
		assertEquals(result.getRegExp(), "\\d{" + result.getMinLength() + ",8}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertNull(result.checkCounts());

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	public void _longPerf(final boolean statisticsOn) throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("_longPerf");
		if (!statisticsOn) {
			analysis.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);
			analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		}
		final long sampleCount = 100_000_000_000L;
		boolean saveOutput = false;
		BufferedWriter bw = null;
		final String[] samples = new String[10000];

		if (saveOutput)
			bw = new BufferedWriter(new FileWriter("/tmp/longPerf.csv"));

		for (int i = 0; i < samples.length; i++)
			samples[i] = String.valueOf(random.nextInt(100000000));

		final long start = System.currentTimeMillis();

		// Run for about reasonable number of seconds
		final int seconds = 5;
		long iters;
		for (iters = 0; iters < sampleCount; iters++) {
			final String sample = samples[(int)(iters%samples.length)];
			analysis.train(sample);
			if (bw != null)
				bw.write(sample + '\n');
			if (iters%100 == 0 && System.currentTimeMillis()  - start >= seconds * 1_000)
				break;

		}
		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);
		if (bw != null)
			bw.close();

		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), iters + 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getMaxLength(), 8);
		assertEquals(result.getRegExp(), "\\d{" + result.getMinLength() + ",8}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertNull(result.checkCounts());
		logger.info("Count {}, duration: {}ms, ~{} per second.", iters + 1, System.currentTimeMillis() - start, (iters  + 1)/seconds);

		// With Statistics & LogicalTypes
		//   - Count 109980301, duration: 10003ms, ~10,998,030 per second
		// No Statistics & No LogicalTypes
		//   - Count 15141740, duration: 10002ms, ~15,141,740 per second
	}

	@Test(groups = { TestGroups.PERFORMANCE, TestGroups.LONGS })
	public void longPerf() throws IOException, FTAException {
		_longPerf(true);
	}

	@Test(groups = { TestGroups.PERFORMANCE, TestGroups.LONGS })
	public void longPerfNoStatistics() throws IOException, FTAException {
		_longPerf(false);
	}
}
