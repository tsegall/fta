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
package com.cobber.fta.internationalization;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.testng.annotations.Test;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.PluginLocaleEntry;
import com.cobber.fta.Sample;
import com.cobber.fta.TestGroups;
import com.cobber.fta.TestSupport;
import com.cobber.fta.TestUtils;
import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;

public class Portuguese {

	private static final SecureRandom RANDOM = new SecureRandom();

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void datesMonthAbbr_ptBR() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("VIGÊNCIA FINAL");
		analysis.setLocale(Locale.forLanguageTag("pt-BR"));
		final String[] inputs = {
				"09/mai/2017", "07/dez/2017", "21/jul/2017", "16/nov/2017", "24/mar/2017", "30/mar/2017",
				"30/mar/2018", "30/mai/2017", "24/ago/2017", "16/out/2017", "06/dez/2017", "18/dez/2017",
				"27/jul/2017", "30/set/2017", "21/ago/2017", "12/jul/2017", "21/mai/2017", "09/jun/2017",
				"27/jul/2017", "21/ago/2017", "16/nov/2017", "30/dez/2017", "30/dez/2017", "INDETERMINADO",
				"15/dez/2017", "11/mar/2017", "01/jan/2018", "29/jan/2018", "31/out/2017", "17/dez/2017",
				"", "", ""
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "dd/MMM/yyyy");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length - 1 - result.getBlankCount());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}/\\p{IsAlphabetic}{3}/\\d{4}");
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void groupingWithPeriod() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("groupingWithPeriod");
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
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void nonLocalizedPortugueseDouble() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("notLocalizedPortugueseDouble");
		analysis.setDebug(2);
		// For Portuguese, Decimal Sep = ',' and Thousands Sep = '.'
		final Locale locale = Locale.forLanguageTag("pt-BR");
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
	public void nonLocalizedPortugueseDoubleSigned() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("notLocalizedPortugueseDoubleSigned");
		// For Portuguese, Decimal Sep = ',' and Thousands Sep = '.'
		final Locale locale = Locale.forLanguageTag("pt-BR");
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
	public void nonLocalizedPortugueseWithDecSep() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("notLocalizedPortugueseWithDecSep");
		// For Portuguese, Decimal Sep = ',' and Thousands Sep = '.'
		final Locale locale = Locale.forLanguageTag("pt-BR");
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
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testGenderPT() throws IOException, FTAException {
		final String[] samples = new String[1000];

		for (int i = 0; i < samples.length; i++) {
			samples[i] = RANDOM.nextInt(2) == 1 ? "femenino" : "masculino";
		}

		final TextAnalyzer analysis = new TextAnalyzer("genero");
		analysis.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);
		final Locale portuguese = Locale.forLanguageTag("pt-BR");
		analysis.setLocale(portuguese);
		final List<PluginDefinition> plugins = new ArrayList<>();
		plugins.add(new PluginDefinition("GENDER_PT", "Gender (Portuguese Language)", null, null, "\\d{3}-\\d{2}-\\d{4}",
				new PluginLocaleEntry[] { new PluginLocaleEntry("pt", null, 90, "(?i)(FEMENINO|MASCULINO)") },
				true, 98, FTAType.STRING));

		try {
			analysis.getPlugins().registerPluginList(plugins, analysis.getConfig(), false);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), "(?i)(FEMENINO|MASCULINO)");
		assertEquals(result.getSemanticType(), "GENDER_PT");
		assertEquals(result.getConfidence(), 1);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getSampleCount(), samples.length);

		assertNull(result.checkCounts(false));

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void postalCodeBR_validInvalid() throws IOException, FTAPluginException {
		final com.cobber.fta.LogicalType logical = LogicalTypeFactory.newInstance(PluginDefinition.findByName("POSTAL_CODE.POSTAL_CODE_BR"), new AnalysisConfig(Locale.forLanguageTag("pt-BR")));

		final String[] valid = {
			"01310-100", "20040-020", "22250-145", "70040-010", "90010-150",
			"30140-071", "40020-010", "60140-120", "50010-010", "69010-050"
		};
		for (final String v : valid)
			assertTrue(logical.isValid(v), v);

		final String[] invalid = { "1310-100", "013100100", "01310-10", "ABCDE-123", "" };
		for (final String iv : invalid)
			assertFalse(logical.isValid(iv), iv);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void postalCodeBR_endToEnd() throws IOException, FTAException {
		final String[] inputs = {
			"01310-100", "20040-020", "22250-145", "70040-010", "90010-150",
			"30140-071", "40020-010", "60140-120", "50010-010", "69010-050",
			"04038-001", "80010-010", "74010-010", "66010-090", "58010-000",
			"49010-100", "64010-010", "65010-000", "77010-010", "79010-010"
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "cep", Locale.forLanguageTag("pt-BR"), "POSTAL_CODE.POSTAL_CODE_BR", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getRegExp(), "\\d{5}-\\d{3}");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void colorPT() throws IOException, FTAException {
		final String[] inputs = {
				"VERMELHO", "AZUL", "VERDE", "AMARELO", "LARANJA", "ROSA", "PRETO", "BRANCO", "CINZA", "MARROM",
				"ROXO", "TURQUESA", "BEGE", "CREME", "OURO", "PRATA", "SALMÃO", "ÍNDIGO", "LAVANDA", "OLIVA",
				"BORDEAUX", "BRONZE", "MARFIM", "AZUL MARINHO", "CORAL", "CIANO", "MAGENTA", "VIOLETA", "CASTANHO", "LIMÃO",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "cor", Locale.forLanguageTag("pt-BR"), "COLOR.TEXT_PT", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void postalCodeBR_lowCardinalityNoHeader() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("field1");
		analysis.setLocale(Locale.forLanguageTag("pt-BR"));

		// Only 3 distinct values — below the cardinality threshold — and no postal header, so should back out
		final String[] inputs = { "01310-100", "20040-020", "70040-010" };
		for (final String s : inputs)
			for (int i = 0; i < 5; i++)
				analysis.train(s);

		final TextAnalysisResult result = analysis.getResult();
		assertFalse("POSTAL_CODE.POSTAL_CODE_BR".equals(result.getSemanticType()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void countryPT() throws IOException, FTAException {
		final String[] inputs = {
				"BRASIL", "PORTUGAL", "ALEMANHA", "FRANÇA", "ESPANHA", "ITÁLIA", "ARGENTINA", "CHILE", "COLÔMBIA", "PERU",
				"MÉXICO", "CANADÁ", "ESTADOS UNIDOS", "AUSTRÁLIA", "JAPÃO", "CHINA", "ÍNDIA", "RÚSSIA", "ANGOLA", "MOÇAMBIQUE",
				"CABO VERDE", "GUINÉ-BISSAU", "SÃO TOMÉ E PRÍNCIPE", "TIMOR-LESTE", "POLÔNIA", "HUNGRIA", "ROMÊNIA", "BULGÁRIA", "CROÁCIA", "GRÉCIA",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "país", Locale.forLanguageTag("pt-BR"), "COUNTRY.TEXT_PT", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void continentPT() throws IOException, FTAException {
		final String[] inputs = {
				"ÁFRICA", "ÁSIA", "EUROPA", "AMERICA DO NORTE", "AMERICA DO SUL", "OCEANIA", "ANTÁRTICA",
				"EUROPA", "ÁSIA", "ÁFRICA", "AMERICA DO NORTE", "ÁSIA", "EUROPA", "ÁFRICA", "OCEANIA",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "continente", Locale.forLanguageTag("pt-BR"), "CONTINENT.TEXT_PT", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void languagePT() throws IOException, FTAException {
		final String[] inputs = {
				"PORTUGUÊS", "INGLÊS", "FRANCÊS", "ALEMÃO", "ESPANHOL", "ITALIANO", "RUSSO", "CHINÊS",
				"JAPONÊS", "ÁRABE", "HINDI", "COREANO", "TURCO", "POLONÊS", "SUECO",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "língua", Locale.forLanguageTag("pt-BR"), "LANGUAGE.TEXT_PT", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}
}
