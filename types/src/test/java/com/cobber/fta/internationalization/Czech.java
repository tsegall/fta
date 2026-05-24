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

import java.io.IOException;
import java.util.Locale;

import org.testng.annotations.Test;

import com.cobber.fta.Sample;
import com.cobber.fta.TestGroups;
import com.cobber.fta.TestUtils;
import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;

public class Czech {

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void colorCS() throws IOException, FTAException {
		final String[] inputs = {
				"ČERVENÁ", "MODRÁ", "ZELENÁ", "ŽLUTÁ", "BÍLÁ", "ČERNÁ", "ORANŽOVÁ", "FIALOVÁ",
				"RŮŽOVÁ", "HNĚDÁ", "ŠEDÁ", "ZLATÁ", "TYRKYSOVÁ", "BÉŽOVÁ",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "barva", Locale.forLanguageTag("cs-CZ"), "COLOR.TEXT_CS", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void continentCS() throws IOException, FTAException {
		final String[] inputs = {
				"EVROPA", "ASIE", "AFRIKA", "SEVERNÍ AMERIKA", "JIŽNÍ AMERIKA", "OCEÁNIE", "ANTARKTIDA",
				"ASIE", "EVROPA", "AFRIKA", "SEVERNÍ AMERIKA", "ASIE", "EVROPA", "AFRIKA", "OCEÁNIE",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "kontinent", Locale.forLanguageTag("cs-CZ"), "CONTINENT.TEXT_CS", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void countryCS() throws IOException, FTAException {
		final String[] inputs = {
				"ČESKO", "NĚMECKO", "FRANCIE", "ŠPANĚLSKO", "ITÁLIE", "POLSKO", "UKRAJINA", "NIZOZEMSKO",
				"ŠVÉDSKO", "NORSKO", "RUSKO", "ČÍNA", "JAPONSKO", "INDIE", "AUSTRÁLIE",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "země", Locale.forLanguageTag("cs-CZ"), "COUNTRY.TEXT_CS", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void languageCS() throws IOException, FTAException {
		final String[] inputs = {
				"ČEŠTINA", "ANGLIČTINA", "NĚMČINA", "FRANCOUZŠTINA", "ŠPANĚLŠTINA", "ITALŠTINA", "POLŠTINA", "RUŠTINA",
				"ČÍNŠTINA", "JAPONŠTINA", "ARABŠTINA", "HINDŠTINA", "KOREJŠTINA", "TUREČTINA", "ŠVÉDŠTINA",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "jazyk", Locale.forLanguageTag("cs-CZ"), "LANGUAGE.TEXT_CS", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}
}
