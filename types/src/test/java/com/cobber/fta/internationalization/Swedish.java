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

public class Swedish {

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void colorSV() throws IOException, FTAException {
		final String[] inputs = {
				"RÖD", "BLÅ", "GRÖN", "GUL", "ORANGE", "LILA", "ROSA", "SVART", "VIT", "GRÅ",
				"BRUN", "TURKOS", "BEIGE", "KRÄM", "GULD", "SILVER", "LAX", "INDIGO", "LAVENDEL", "OLIV",
				"BORDEAUX", "BRONS", "ELFENBEN", "MARINBLÅ", "KORALL", "CYAN", "MAGENTA", "VIOLETT", "LIME", "KASTANJ",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "färg", Locale.forLanguageTag("sv-SE"), "COLOR.TEXT_SV", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void continentSV() throws IOException, FTAException {
		final String[] inputs = {
				"AFRIKA", "ASIEN", "EUROPA", "NORDAMERIKA", "SYDAMERIKA", "OCEANIEN", "ANTARKTIS",
				"EUROPA", "ASIEN", "AFRIKA", "NORDAMERIKA", "ASIEN", "EUROPA", "AFRIKA", "OCEANIEN",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "kontinent", Locale.forLanguageTag("sv-SE"), "CONTINENT.TEXT_SV", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void countrySV() throws IOException, FTAException {
		final String[] inputs = {
				"SVERIGE", "TYSKLAND", "FRANKRIKE", "SPANIEN", "ITALIEN", "POLEN", "UKRAINA", "NEDERLÄNDERNA", "NORGE", "FINLAND",
				"RYSSLAND", "KINA", "JAPAN", "INDIEN", "AUSTRALIEN", "BRASILIEN", "ARGENTINA", "MEXIKO", "EGYPTEN", "TURKIET",
				"USA", "KANADA", "SYDAFRIKA", "STORBRITANNIEN", "GREKLAND", "PORTUGAL", "BELGIEN", "SCHWEIZ", "ÖSTERRIKE", "DANMARK",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "land", Locale.forLanguageTag("sv-SE"), "COUNTRY.TEXT_SV", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void languageSV() throws IOException, FTAException {
		final String[] inputs = {
				"SVENSKA", "ENGELSKA", "TYSKA", "FRANSKA", "SPANSKA", "ITALIENSKA", "PORTUGISISKA", "RYSKA",
				"KINESISKA", "JAPANSKA", "ARABISKA", "HINDI", "KOREANSKA", "TURKISKA", "POLSKA",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "språk", Locale.forLanguageTag("sv-SE"), "LANGUAGE.TEXT_SV", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}
}
