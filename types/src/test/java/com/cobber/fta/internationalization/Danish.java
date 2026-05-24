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

public class Danish {

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void colorDA() throws IOException, FTAException {
		final String[] inputs = {
				"RØD", "BLÅ", "GRØN", "GUL", "ORANGE", "LILLA", "LYSERØD", "SORT", "HVID", "GRÅ",
				"BRUN", "TURKIS", "BEIGE", "CREME", "GULD", "SØLV", "LAKS", "INDIGO", "LAVENDEL", "OLIVEN",
				"BORDEAUX", "BRONZE", "ELFENBEN", "MARINEBLÅ", "KORAL", "CYAN", "MAGENTA", "VIOLET", "LIME",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "farve", Locale.forLanguageTag("da-DK"), "COLOR.TEXT_DA", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void continentDA() throws IOException, FTAException {
		final String[] inputs = {
				"AFRIKA", "ASIEN", "EUROPA", "NORDAMERIKA", "SYDAMERIKA", "OCEANIEN", "ANTARKTIS",
				"EUROPA", "ASIEN", "AFRIKA", "NORDAMERIKA", "ASIEN", "EUROPA", "AFRIKA", "OCEANIEN",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "kontinent", Locale.forLanguageTag("da-DK"), "CONTINENT.TEXT_DA", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void postalCodeDK() throws IOException, FTAException {
		final String[] inputs = {
				"1000", "2100", "2200", "2300", "2400", "2500", "2600", "2700", "2800", "2900",
				"3000", "3200", "3400", "3500", "3600", "3700", "4000", "4100", "4200", "4300",
				"5000", "5200", "5500", "6000", "6200", "6400", "7000", "7400", "7500", "8000",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "postnummer", Locale.forLanguageTag("da-DK"), "POSTAL_CODE.POSTAL_CODE_DK", FTAType.LONG, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void countryDA() throws IOException, FTAException {
		final String[] inputs = {
				"DANMARK", "TYSKLAND", "FRANKRIG", "SPANIEN", "ITALIEN", "POLEN", "UKRAINE", "NEDERLANDENE", "SVERIGE", "NORGE",
				"FINLAND", "RUSLAND", "KINA", "JAPAN", "INDIEN", "AUSTRALIEN", "BRASILIEN", "ARGENTINA", "MEXICO", "EGYPTEN",
				"USA", "CANADA", "SYDAFRIKA", "STORBRITANNIEN", "GRÆKENLAND", "PORTUGAL", "BELGIEN", "SCHWEIZ", "ØSTRIG", "TYRKIET",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "land", Locale.forLanguageTag("da-DK"), "COUNTRY.TEXT_DA", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void languageDA() throws IOException, FTAException {
		final String[] inputs = {
				"DANSK", "ENGELSK", "TYSK", "FRANSK", "SPANSK", "ITALIENSK", "PORTUGISISK", "RUSSISK",
				"KINESISK", "JAPANSK", "ARABISK", "HINDI", "KOREANSK", "TYRKISK", "POLSK",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "sprog", Locale.forLanguageTag("da-DK"), "LANGUAGE.TEXT_DA", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}
}
