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

import java.io.IOException;
import java.util.Locale;

import org.testng.annotations.Test;

import com.cobber.fta.Sample;
import com.cobber.fta.TestGroups;
import com.cobber.fta.TestUtils;
import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;

public class Romanian {

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void colorRO() throws IOException, FTAException {
		final String[] inputs = {
				"ROȘU", "ALBASTRU", "VERDE", "GALBEN", "PORTOCALIU", "VIOLET", "ROZ", "NEGRU", "ALB", "GRI",
				"MARO", "TURCOAZ", "BEJ", "CREM", "AUR", "ARGINT", "SOMON", "INDIGO", "LAVANDĂ", "MĂSLINIU",
				"BORDEAUX", "BRONZ", "FILDEȘ", "BLEUMARIN", "CORAL", "CYAN", "MAGENTA", "TEAL",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "culoare", Locale.forLanguageTag("ro-RO"), "COLOR.TEXT_RO", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void continentRO() throws IOException, FTAException {
		final String[] inputs = {
				"AFRICA", "ASIA", "EUROPA", "AMERICA DE NORD", "AMERICA DE SUD", "OCEANIA", "ANTARCTICA",
				"EUROPA", "ASIA", "AFRICA", "AMERICA DE NORD", "ASIA", "EUROPA", "AFRICA", "OCEANIA",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "continent", Locale.forLanguageTag("ro-RO"), "CONTINENT.TEXT_RO", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void postalCodeRO() throws IOException, FTAException {
		final String[] inputs = {
				"010001", "020001", "030001", "040001", "050001", "060001", "070001", "080001", "090001", "100001",
				"110001", "120001", "130001", "140001", "150001", "160001", "170001", "180001", "190001", "200001",
				"300001", "400001", "500001", "600001", "700001", "720001", "740001", "800001", "900001", "905000",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "cod postal", Locale.forLanguageTag("ro-RO"), "POSTAL_CODE.POSTAL_CODE_RO", FTAType.LONG, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void countryRO() throws IOException, FTAException {
		final String[] inputs = {
				"ROMÂNIA", "GERMANIA", "FRANȚA", "SPANIA", "ITALIA", "POLONIA", "UCRAINA", "OLANDA", "SUEDIA", "NORVEGIA",
				"FINLANDA", "RUSIA", "CHINA", "JAPONIA", "INDIA", "AUSTRALIA", "BRAZILIA", "ARGENTINA", "MEXIC", "EGIPT",
				"STATELE UNITE", "CANADA", "AFRICA DE SUD", "REGATUL UNIT", "GRECIA", "PORTUGALIA", "BELGIA", "ELVEȚIA", "AUSTRIA", "DANEMARCA",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "țară", Locale.forLanguageTag("ro-RO"), "COUNTRY.TEXT_RO", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void languageRO() throws IOException, FTAException {
		final String[] inputs = {
				"ROMÂNĂ", "ENGLEZĂ", "GERMANĂ", "FRANCEZĂ", "SPANIOLĂ", "ITALIANĂ", "PORTUGHEZĂ", "RUSĂ",
				"CHINEZĂ", "JAPONEZĂ", "ARABĂ", "HINDI", "COREEANĂ", "TURCĂ", "POLONĂ",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "limbă", Locale.forLanguageTag("ro-RO"), "LANGUAGE.TEXT_RO", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}
}
