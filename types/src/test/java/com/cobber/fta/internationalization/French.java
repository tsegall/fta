/*
 * Copyright 2017-2026 Tim Segall
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
package com.cobber.fta.internationalization;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.KnownTypes;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.Sample;
import com.cobber.fta.TestGroups;
import com.cobber.fta.TestSupport;
import com.cobber.fta.TestUtils;
import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.core.RegExpSplitter;

public class French {
	private static final SecureRandom RANDOM = new SecureRandom();

	@Test(groups = { TestGroups.ALL, TestGroups.BOOLEANS })
	public void basicBooleanFrench() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicBooleanFrench");
		analysis.setLocale(Locale.FRENCH);
		final String[] inputs = "non|oui|Oui|    non   |NON |OUI|oui|non|NON|Oui|non|  NON|NON|oui|OUI|bogus".split("\\|");
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
		assertEquals(result.getRegExp(), KnownTypes.PATTERN_WHITESPACE +
				"(" + analysis.getRegExp(KnownTypes.ID.ID_BOOLEAN_YES_NO_LOCALIZED) + ")" +
				KnownTypes.PATTERN_WHITESPACE);
		assertEquals(result.getConfidence(), .9375);
		assertEquals(result.getType(), FTAType.BOOLEAN);
		assertEquals(result.getTypeModifier(), "YES_NO");
		assertEquals(result.getMinLength(), 3);
		assertEquals(result.getMaxLength(), 10);
		assertEquals(result.getMinValue(), "non");
		assertEquals(result.getMaxValue(), "oui");
		assertTrue(inputs[0].matches(result.getRegExp()));

		int matches = 0;
		for (final String input : inputs) {
			if (input.trim().matches(result.getRegExp()))
					matches++;
		}
		assertEquals(result.getMatchCount(), matches);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicFrenchDate() throws IOException, FTAException {

		final Set<String> samples = new HashSet<>();
		LocalDate localDate = LocalDate.now();

		final TextAnalyzer analysis = new TextAnalyzer("basicFrenchDate");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		analysis.configure(TextAnalyzer.Feature.NO_ABBREVIATION_PUNCTUATION, false);

		analysis.setLocale(Locale.FRANCE);

		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRANCE);

		int locked = -1;

		for (int i = 0; i < 100; i++) {
			final String sample = localDate.format(formatter);
			samples.add(sample);
			if (analysis.train(sample) && locked == -1)
				locked = i;
			localDate = localDate.minusDays(100);
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getRegExp(), "\\d{1,2} [\\p{IsAlphabetic}\\.]{3,5} \\d{4}");
		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "d MMM yyyy");
		assertEquals(result.getSampleCount(), samples.size());
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), samples.size());
		assertEquals(result.getNullCount(), 0);
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		// Even the UNK match the RE
		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()), sample);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void manyFrenchDoubles() throws IOException, FTAException {
		final String[] samplesUS = {
				"54.00", "12719300.00", "4819.00", "262612.00", "141300.00",
				"44876900.00", "681023.00", "460198.00", "1123220.00", "18300.00",
				"166658.00", "114656.00", "61461.00", "263058.00", "23747.00",
				"44539.00", "70836.00", "351498.00", "669803.00", "116655.00",
				"542.00", "12719300.00", "4819.00", "262612.00", "141300.00",
				"44876900.00", "681023.00", "460198.00", "1123220.00", "18300.00"

		};
		final String[] samples = new String[samplesUS.length];
		final TextAnalyzer analysis = new TextAnalyzer("manyFrenchDoubles");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final Locale locale = Locale.forLanguageTag("fr-FR");
		analysis.setLocale(locale);

		final DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(locale);
		formatter.setMinimumFractionDigits(2);
		formatter.setMaximumFractionDigits(2);

		for (int i = 0; i < samples.length; i++) {
			final double d = Double.parseDouble(samplesUS[i]);
			samples[i] = formatter.format(d);
		}

		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getDecimalSeparator(), ',');
		assertEquals(result.getTypeModifier(), "GROUPING");
		assertNull(result.getSemanticType());
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_DOUBLE_GROUPING));
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void monetaryDecimalSeparatorFrench() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Separator");
		analysis.setLocale(Locale.FRENCH);
		final int SAMPLE_SIZE = 10000;
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		String minValue = String.valueOf(min);
		String maxValue = String.valueOf(max);
		final Set<String> samples = new HashSet<>();

		final NumberFormat doubleFormatter = NumberFormat.getNumberInstance(Locale.FRENCH);
		final DecimalFormat nf = (DecimalFormat)doubleFormatter;
		nf.applyPattern("#.##################E0");
		for (int i = 0; i < SAMPLE_SIZE; i++) {
			final double d = RANDOM.nextDouble() * 10000;
			final String sample = nf.format(d);
			if (d < min) {
				min = d;
				minValue = sample;
			}
			if (d > max) {
				max = d;
				maxValue = sample;
			}
			samples.add(sample);
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), SAMPLE_SIZE);
		assertEquals(result.getMatchCount(), SAMPLE_SIZE);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getMinValue(), minValue);
		assertEquals(result.getMaxValue(), maxValue);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_DOUBLE_WITH_EXPONENT));
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()), sample);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void nonLocalizedFrenchDouble() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("notLocalizedFrenchDouble");
		// For French, Decimal Sep = ',' and Thousands Sep = ' '
		final Locale locale = Locale.forLanguageTag("fr-FR");
		analysis.setLocale(locale);

		final String[] inputs = {
				"46.448", "6.341288", "543.022", "636.666", "61.606330",
				"64.425", "109.089", "57.995", "4773.826", "23.498620",
				"43.391", "1356.902", "22.039", "2526.587", "33.113104",
				"221.887", "6313.005", "879.865", "84.369774"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), "NON_LOCALIZED");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d*\\.?\\d+");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "6.341288");
		assertEquals(result.getMaxValue(), "6313.005");
		assertEquals(result.getDecimalSeparator(), '.');
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void nonLocalizedFrenchDoubleSigned() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("notLocalizedFrenchDoubleSigned");
		// For French, Decimal Sep = ',' and Thousands Sep = ' '
		final Locale locale = Locale.forLanguageTag("fr-FR");
		analysis.setLocale(locale);

		final String[] inputs = {
				"46.448", "6.341288", "543.022", "636.666", "61.606330",
				"64.425", "109.089", "57.995", "4773.826", "23.498620",
				"43.391", "-1356.902", "22.039", "2526.587", "33.113104",
				"221.887", "6313.005", "879.865", "84.369774"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), "SIGNED,NON_LOCALIZED");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "[+-]?\\d*\\.?\\d+");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "-1356.902");
		assertEquals(result.getMaxValue(), "6313.005");
		assertEquals(result.getDecimalSeparator(), '.');
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void nonLocalizedFrenchWithDecSep() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("notLocalizedFrenchWithDecSep");
		// For French, Decimal Sep = ',' and Thousands Sep = ' '
		final Locale locale = Locale.forLanguageTag("fr-FR");
		analysis.setLocale(locale);

		final String[] inputs = {
				"46.448", "6.341288", "543.022", "636.666", "61.606330",
				"64.425", "109.089", "57.995", "4773.826", "23.498620",
				"43.391", "-1356.902", "22.039", "2526.587", "33.113104",
				"221.887", "6313.005", "879.865", "84.369774", "43.075",
				"64.425", "109.089", "57.995", "4773.826", "23.498620",
				"76,87"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), "SIGNED,NON_LOCALIZED");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length - 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "[+-]?\\d*\\.?\\d+");
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
		assertEquals(result.getMinValue(), "-1356.902");
		assertEquals(result.getMaxValue(), "6313.005");
		assertEquals(result.getDecimalSeparator(), '.');
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void nonLocalizedFrenchWithThousandsSep() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("notLocalizedFrenchChange");
		// For French, Decimal Sep = ',' and Thousands Sep = ' '
		final Locale locale = Locale.forLanguageTag("fr-FR");
		analysis.setLocale(locale);

		final String[] inputs = {
				"46.448", "6.341288", "543.022", "636.666", "61.606330",
				"64.425", "109.089", "57.995", "4773.826", "23.498620",
				"43.391", "-1356.902", "22.039", "2526.587", "33.113104",
				"221.887", "6313.005", "879.865", "84.369774", "43.075",
				"64.425", "109.089", "57.995", "4773.826", "23.498620",
				"7 687"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeModifier(), "SIGNED,NON_LOCALIZED");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length - 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "[+-]?\\d*\\.?\\d+");
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
		assertEquals(result.getMinValue(), "-1356.902");
		assertEquals(result.getMaxValue(), "6313.005");
		assertEquals(result.getDecimalSeparator(), '.');
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
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
				long l = RANDOM.nextInt(100000000);
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
			assertNull(result.checkCounts(false));

			TestSupport.checkHistogram(result, 10, true);
			TestSupport.checkQuantiles(result);

			for (final String sample : samples)
				assertTrue(sample.matches(regExp), sample);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void testBulkLongFrench() throws IOException, FTAException {
		final long SAMPLE_COUNT = 100L;

		final List<String> samplesLong100 = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesLong100.add("100");
		samplesLong100.add(null);
		samplesLong100.add(" ");

		final List<String> samplesLong200 = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesLong200.add("200");
		samplesLong200.add(null);
		samplesLong200.add(null);
		samplesLong200.add("  ");
		samplesLong200.add("  ");
		samplesLong200.add("x");

		final TextAnalyzer merged = TestUtils.checkTextAnalyzerMerge(samplesLong100, samplesLong200, "long_long", Locale.FRANCE, true);
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.LONG);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void cardinalityExceededLongFrench() throws IOException, FTAException {

		final List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardOne.add(String.valueOf(i));

		final List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardTwo.add(String.valueOf(100000 + i));

		TestUtils.checkTextAnalyzerMerge(shardOne, shardTwo, "cardinalityExceededLong", Locale.FRANCE, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void cardinalityExceededDoubleFrench0() throws IOException, FTAException {

		final List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardOne.add(Integer.toString(i) + ",0");

		final List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardTwo.add(Integer.toString(100000 + i) + ",0");

		final TextAnalyzer merged = TestUtils.checkTextAnalyzerMerge(shardOne, shardTwo, "cardinalityExceededDoubleFrench", Locale.FRANCE, true);
		final TextAnalysisResult mergedResult = merged.getResult();
		assertNull(mergedResult.getTypeModifier());
	}

	// Test is broken due to the fact that the bottomk/topK values are stored in a reasonable localized format (reasonable means
	// that it mirrors the format w.r.t. to the presence of the exponent and the presence of the thousands separator) but NOT
	// the number of decimal places.
	// The cardinality set is the input as received, hence if you need to add topK/bottomK as the cardinality has been blown
	// then it has the potential to change the shapes detected.
	// Solution is probably to keep the input as received for the values associated with the topK/bottomK.
	// @Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void cardinalityExceededDoubleFrench00() throws IOException, FTAException {

		final List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardOne.add(Integer.toString(i) + ",00");

		final List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardTwo.add(Integer.toString(100000 + i) + ",00");

		final TextAnalyzer merged = TestUtils.checkTextAnalyzerMerge(shardOne, shardTwo, "cardinalityExceededDoubleFrench", Locale.FRANCE, true);
		final TextAnalysisResult mergedResult = merged.getResult();
		System.err.println(mergedResult.asJSON(true, 0));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void doubleFrench() throws IOException, FTAException {

		final List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20; i++)
			shardOne.add(Integer.toString(i) + ",0");

		final List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < 20; i++)
			shardTwo.add(Integer.toString(100000 + i) + ",0");

		TestUtils.checkTextAnalyzerMerge(shardOne, shardTwo, "doubleFrench", Locale.FRANCE, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void VAT_FR() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("TVA");
		analysis.setLocale(Locale.forLanguageTag("fr-FR"));

		final String[] inputs = {
				"FR00000000190", "FR00300076965", "FR00303656847", "FR19000000067", "FR20562016774",
				"FR01000000158", "FR03512803495", "FR03552081317", "FR03784359069", "FR04494487341",
				"FR05442977302", "FR13393892815", "FR14722057460", "FR17000000034", "FR22528117732",
				"FR25000000166", "FR25432701258", "FR27514868827", "FR29312010820", "FR31387589179",
				"FR38438710865", "FR39412658767", "FR40303265045", "FR40391895109", "FR40402628838",
				"FR41000000042", "FR41343848552", "FR42403335904", "FR42504207853", "FR90524670213",
				"FR43000000075", "FR44527865992", "FR45395080138", "FR45542065305", "FR46400477089",
				"FR47000000141", "FR47323875187", "FR47323875187", "FR48000000109", "FR53418304010",
				"FR54000000208", "FR55338966385", "FR55440243988", "FR55480081306", "FR56439795816",
				"FR57609803416", "FR58399360817", "FR58499528255", "FR61300986619", "FR61954506077",
				"FR64518539093", "FR65489465542", "FR67000000083", "FR71383076817", "FR72000000117",
				"FR73000000182", "FR74532287844", "FR82494628696", "FR82542065479", "FR83404833048",
				"FR85418228102", "FR88414997130", "FR89540090917", "FR90000000026", "FR96000000125"
		};

		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "TVA", Locale.forLanguageTag("fr-FR"), "IDENTITY.VAT_<COUNTRY>", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicMonthAbbrFrench() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicMonthAbbrFrench");
		analysis.setLocale(Locale.FRENCH);
		analysis.configure(TextAnalyzer.Feature.NO_ABBREVIATION_PUNCTUATION, false);
		final int badCount = 4;
		final String inputs[] = TestUtils.MONTHS_FRENCH.split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getRegExp(), "[\\p{IsAlphabetic}\\.]{3,5}");
		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getSemanticType(), "MONTH.ABBR_fr");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		final Map<String, Long> invalids = result.getInvalidDetails();
		assertEquals(invalids.size(), 1);
		assertEquals(invalids.get("UNK"), 4L);
		assertEquals(result.getMatchCount(), inputs.length - badCount);
		assertEquals(result.getNullCount(), 0);
		assertTrue((double)analysis.getPluginThreshold()/100 < result.getConfidence());
		assertEquals(result.getConfidence(), 1 - (double)badCount/result.getSampleCount());

		assertNull(result.checkCounts(false));

		// Even the UNK match the RE
		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()), input);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void inseeCode() throws IOException, FTAException {
		final String[] samples = {
				"99999", "XXXXX", "01001", "01002", "01004", "01005", "01006", "01007", "01008", "01009",
				"01010", "01011", "01012", "01013", "01014", "01015", "01016", "01017", "01019", "01021",
				"01022", "01023", "01024", "01025", "01026", "01027", "01028", "01029", "01030", "01031",
				"01032", "01033", "01034", "01035", "01036", "01037", "01038", "01039", "01040",
		};

		TestUtils.simpleCore(Sample.setInvalid(Sample.allValid(samples), 0, 1), "Codes_Insee", Locale.FRANCE, "STATE_PROVINCE.INSEE_CODE_FR", FTAType.LONG, 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicSSN_FR() throws IOException, FTAException {
		final String[] inputs = {
				"186022A215325 23", "1691099352470 01", "2741147566941 55",
				"1870364431266 17", "1620750699385 24", "1910926856381 09", "2350193443182 66",
				"1021130154849 54", "1060633581206 43", "2790148853457 33", "1910585591722 44",
				"2031245436518 70", "1011076339993 38", "2980845336004 29", "1991181413900 71",
				"1500645426767 03", "1180926187160 15", "2300747704141 68", "1820485399754 86",
				"1870963392946 48", "1510366293364 46", "2800291682045 16", "1660882307695 51",
				"2760672523900 48", "2130327681550 09", "1940965237732 53", "2370790974188 20",
		};

		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "basicSSN_FR", Locale.forLanguageTag("fr-FR"), "IDENTITY.SSN_FR", FTAType.STRING, 1.0);

		assertEquals(result.getCardinality(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void genderFR() throws IOException, FTAException {
		final String[] inputs = {
				"FEMME", "HOMME", "FEMME", "HOMME", "FEMME", "HOMME", "FEMME", "HOMME", "FEMME", "HOMME",
				"HOMME", "FEMME", "HOMME", "FEMME", "HOMME", "FEMME", "HOMME", "FEMME", "HOMME", "FEMME",
				"FEMME", "HOMME", "FEMME", "HOMME", "FEMME", "HOMME", "FEMME", "HOMME", "FEMME", "HOMME",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "Genre", Locale.forLanguageTag("fr-FR"), "GENDER.TEXT_<LANGUAGE>", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void postalCodeFR_validInvalid() throws IOException, FTAPluginException {
		final com.cobber.fta.LogicalType logical = LogicalTypeFactory.newInstance(PluginDefinition.findByName("POSTAL_CODE.POSTAL_CODE_FR"), new AnalysisConfig(Locale.forLanguageTag("fr-FR")));

		final String[] valid = { "01000", "01001", "01002", "75001", "75008", "13001", "69001", "33000", "06000", "59000" };
		for (final String v : valid)
			assertTrue(logical.isValid(v), v);

		final String[] invalid = { "1000", "123456", "ABCDE", "7500A", "" };
		for (final String iv : invalid)
			assertFalse(logical.isValid(iv), iv);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void postalCodeFR_endToEnd() throws IOException, FTAException {
		final String[] inputs = {
			"75001", "75002", "75003", "75004", "75005",
			"13001", "13002", "13003", "69001", "69002",
			"33000", "06000", "59000", "31000", "67000",
			"76000", "44000", "35000", "57000", "29200"
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "code_postal", Locale.forLanguageTag("fr-FR"), "POSTAL_CODE.POSTAL_CODE_FR", FTAType.LONG, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void postalCodeFR_lowCardinalityNoHeader() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("field1");
		analysis.setLocale(Locale.forLanguageTag("fr-FR"));

		// Only 3 distinct values — below the cardinality threshold — and no postal header, so should back out
		final String[] inputs = { "75001", "75002", "75003" };
		for (final String s : inputs)
			for (int i = 0; i < 5; i++)
				analysis.train(s);

		final TextAnalysisResult result = analysis.getResult();
		assertFalse("POSTAL_CODE.POSTAL_CODE_FR".equals(result.getSemanticType()));
	}
}
