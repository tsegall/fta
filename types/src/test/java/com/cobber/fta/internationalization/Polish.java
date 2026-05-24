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

public class Polish {

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void colorPL() throws IOException, FTAException {
		final String[] inputs = {
				"CZERWONY", "NIEBIESKI", "ZIELONY", "ŻÓŁTY", "POMARAŃCZOWY", "FIOLETOWY", "RÓŻOWY", "CZARNY", "BIAŁY", "SZARY",
				"BRĄZOWY", "TURKUSOWY", "BEŻOWY", "KREMOWY", "ZŁOTY", "SREBRNY", "ŁOSOSIOWY", "INDYGO", "LAWENDOWY", "OLIWKOWY",
				"BORDOWY", "GRANATOWY", "KORALOWY", "CYJAN", "MAGENTA", "LIMONKOWY", "MALINOWY",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "kolor", Locale.forLanguageTag("pl-PL"), "COLOR.TEXT_PL", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void continentPL() throws IOException, FTAException {
		final String[] inputs = {
				"AFRYKA", "AZJA", "EUROPA", "AMERYKA PÓŁNOCNA", "AMERYKA POŁUDNIOWA", "OCEANIA", "ANTARKTYDA",
				"EUROPA", "AZJA", "AFRYKA", "AMERYKA PÓŁNOCNA", "AZJA", "EUROPA", "AFRYKA", "OCEANIA",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "kontynent", Locale.forLanguageTag("pl-PL"), "CONTINENT.TEXT_PL", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void postalCodePL() throws IOException, FTAException {
		final String[] inputs = {
				"00-001", "01-100", "02-200", "03-300", "04-400", "05-500", "10-001", "15-001", "20-001", "25-001",
				"30-001", "31-001", "40-001", "41-001", "50-001", "51-001", "60-001", "61-001", "70-001", "71-001",
				"80-001", "81-001", "85-001", "87-001", "90-001", "91-001", "93-001", "95-001", "97-001", "99-001",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "kod pocztowy", Locale.forLanguageTag("pl-PL"), "POSTAL_CODE.POSTAL_CODE_PL", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getRegExp(), "\\d{2}-\\d{3}");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void countryPL() throws IOException, FTAException {
		final String[] inputs = {
				"POLSKA", "NIEMCY", "FRANCJA", "HISZPANIA", "WŁOCHY", "UKRAINA", "HOLANDIA", "SZWECJA", "NORWEGIA", "FINLANDIA",
				"ROSJA", "CHINY", "JAPONIA", "INDIE", "AUSTRALIA", "BRAZYLIA", "ARGENTYNA", "MEKSYK", "EGIPT", "TURCJA",
				"STANY ZJEDNOCZONE", "KANADA", "REPUBLIKA POŁUDNIOWEJ AFRYKI", "WIELKA BRYTANIA", "GRECJA", "PORTUGALIA", "BELGIA", "SZWAJCARIA", "AUSTRIA", "DANIA",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "kraj", Locale.forLanguageTag("pl-PL"), "COUNTRY.TEXT_PL", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void languagePL() throws IOException, FTAException {
		final String[] inputs = {
				"POLSKI", "ANGIELSKI", "NIEMIECKI", "FRANCUSKI", "HISZPAŃSKI", "WŁOSKI", "PORTUGALSKI", "ROSYJSKI",
				"CHIŃSKI", "JAPOŃSKI", "ARABSKI", "HINDI", "KOREAŃSKI", "TURECKI", "SZWEDZKI",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "język", Locale.forLanguageTag("pl-PL"), "LANGUAGE.TEXT_PL", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}
}
