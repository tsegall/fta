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
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Locale;

import org.testng.annotations.Test;

import com.cobber.fta.KnownTypes;
import com.cobber.fta.Sample;
import com.cobber.fta.TestGroups;
import com.cobber.fta.TestSupport;
import com.cobber.fta.TestUtils;
import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;

public class Italian {

	@Test(groups = { TestGroups.ALL, TestGroups.BOOLEANS })
	public void basicBooleanItalian() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicBooleanItalian");
		analysis.setLocale(Locale.ITALIAN);
		final String[] inputs = "no|si|Si|    no   |NO |SI|si|no|No|Si|no|  NO|NO|si|SI|bogus".split("\\|");
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
		assertEquals(result.getMinLength(), 2);
		assertEquals(result.getMaxLength(), 9);
		assertEquals(result.getMinValue(), "no");
		assertEquals(result.getMaxValue(), "si");
		assertTrue(inputs[0].matches(result.getRegExp()));
		int matches = 0;
		for (final String input : inputs) {
			if (input.trim().matches(result.getRegExp()))
					matches++;
		}
		assertEquals(result.getMatchCount(), matches);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void doublePeriodIT() throws IOException, FTAException {
		final String[] ugly = {
				"3219,53", "3528,67", "5342,12", "3891,63",
				"5.373.99",
				"2107,85", "1039.69", "3250,63", "6678,75",
				"2540,35", "2500", "5041,7", "1626,89", "1881", "5427,42",
				"200", "910,45", "1931,32", "5059,16", "47214,8", "2770,97",
				"219,54", "38,607", "53452,1", "1,6356", "12,995",
				"45,67", "12,34", "14,098", "12,4790", "1,2",
				"34789,0", "2,3", "3,4", "9,0", "14,41",
				"12,23", "3,14", "15,92654", "43,809", "203,01"
		};
		final Locale locale = Locale.forLanguageTag("it-IT");
		final TextAnalyzer analysis = new TextAnalyzer("Numero");
		analysis.setLocale(locale);
		analysis.setDebug(2);
		for (final String sample : ugly)
			analysis.train(sample);
		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);
		assertEquals(result.getType(), FTAType.DOUBLE, locale.toLanguageTag());
		result.asJSON(true, 1);
		assertNull(result.getTypeModifier());
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), ugly.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getInvalidCount(), 2);
		assertEquals(result.getMatchCount(), ugly.length - 2);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
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
	//BUG TODO     assertEquals(result.getRegExp(), "\\d{13}");
		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getMatchCount(), samples.length - 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), samples.length - 1);
		assertEquals(result.getCardinality(), 28);
		assertEquals(result.getInvalidCount(), 1);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMinValue(), "985491000");
		assertEquals(result.getMaxValue(), "9452921001");
		assertNull(result.checkCounts(false));
		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void VAT_IT() throws IOException, FTAException {
		final String[] inputs = {
				"00673580221", "02400290223", "02209460225", "02018300224", "00106510225",
				"02271060226", "02167060223", "00227460227", "01867580225", "01981650227",
				"02046850224", "02459690224", "02141050225", "00051370229", "00983840224",
				"01989590227", "00075750224", "00337140222", "01855780225", "02099830222",
				"00142960228", "01384990220", "02345010223", "01720000221", "01947280226",
				"00166280222", "01889730220", "02046780223", "00921280244", "01648950226",
				"01856020225", "00828140228", "02030200220", "00814060224", "00971660220",
				"00401660220", "02304350222", "02787520168", "01718290222", "01731500227",
				"02331550224", "01743260224", "01887120226", "01226750220", "01323250223",
				"01813150222", "01783350224", "01273520229", "01594610220", "01611170224"
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "IVA", Locale.forLanguageTag("it-IT"), "IDENTITY.VAT_<COUNTRY>", FTAType.STRING, 1.0);
		assertEquals(result.getRegExp(), "\\d{11}");
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void genderIT() throws IOException, FTAException {
		final String[] inputs = {
				"FEMMINA", "MASCHIO", "FEMMINA", "MASCHIO", "FEMMINA", "MASCHIO", "FEMMINA", "MASCHIO", "FEMMINA", "MASCHIO",
				"MASCHIO", "FEMMINA", "MASCHIO", "FEMMINA", "MASCHIO", "FEMMINA", "MASCHIO", "FEMMINA", "MASCHIO", "FEMMINA",
				"FEMMINA", "MASCHIO", "FEMMINA", "MASCHIO", "FEMMINA", "MASCHIO", "FEMMINA", "MASCHIO", "FEMMINA", "MASCHIO",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "sesso", Locale.forLanguageTag("it-IT"), "GENDER.TEXT_<LANGUAGE>", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void postalCodeIT() throws IOException, FTAException {
		final String[] inputs = {
				"00100", "20121", "40121", "50123", "80121", "10121", "30121", "70121", "90121", "16121",
				"37121", "95121", "34121", "06121", "65121", "85100", "88100", "98100", "74121", "89100",
				"24121", "25121", "13900", "12100", "15100", "14100", "15121", "27100", "26100", "23100",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "CAP", Locale.forLanguageTag("it-IT"), "POSTAL_CODE.POSTAL_CODE_IT", FTAType.LONG, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getRegExp(), "\\d{5}");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void colorIT() throws IOException, FTAException {
		final String[] inputs = {
				"ROSSO", "BLU", "VERDE", "GIALLO", "ARANCIONE", "VIOLA", "ROSA", "NERO", "BIANCO", "GRIGIO",
				"MARRONE", "AZZURRO", "TURCHESE", "BEIGE", "CREMA", "ORO", "ARGENTO", "SALMONE", "PESCA", "INDACO",
				"LAVANDA", "OLIVA", "LIME", "CORALLO", "CIANO", "MAGENTA", "BORDEAUX", "BRONZO", "AVORIO", "BLU NAVY",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "colore", Locale.forLanguageTag("it-IT"), "COLOR.TEXT_IT", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void countryIT() throws IOException, FTAException {
		final String[] inputs = {
				"ITALIA", "GERMANIA", "FRANCIA", "SPAGNA", "PORTOGALLO", "GRECIA", "BELGIO", "PAESI BASSI", "SVEZIA", "NORVEGIA",
				"DANIMARCA", "FINLANDIA", "POLONIA", "UNGHERIA", "ROMANIA", "BULGARIA", "CROAZIA", "SLOVACCHIA", "SLOVENIA", "ESTONIA",
				"LETTONIA", "LITUANIA", "CIPRO", "MALTA", "STATI UNITI", "CANADA", "BRASILE", "ARGENTINA", "MESSICO", "CILE",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "paese", Locale.forLanguageTag("it-IT"), "COUNTRY.TEXT_IT", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void continentIT() throws IOException, FTAException {
		final String[] inputs = {
				"AFRICA", "ASIA", "EUROPA", "AMERICA DEL NORD", "AMERICA DEL SUD", "OCEANIA", "ANTARTIDE",
				"EUROPA", "ASIA", "AFRICA", "AMERICA DEL NORD", "ASIA", "EUROPA", "AFRICA", "OCEANIA",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "continente", Locale.forLanguageTag("it-IT"), "CONTINENT.TEXT_IT", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void languageIT() throws IOException, FTAException {
		final String[] inputs = {
				"ITALIANO", "INGLESE", "FRANCESE", "TEDESCO", "SPAGNOLO", "PORTOGHESE", "RUSSO", "CINESE",
				"GIAPPONESE", "ARABO", "HINDI", "COREANO", "TURCO", "POLACCO", "SVEDESE",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "lingua", Locale.forLanguageTag("it-IT"), "LANGUAGE.TEXT_IT", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}
}
