/*
 * Copyright 2017-2022 Tim Segall
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

import java.util.HashMap;
import java.util.List;

import org.testng.annotations.Test;

import com.cobber.fta.core.Utils;
import com.cobber.fta.core.WordOffset;
import com.cobber.fta.core.WordProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestUtils {
	private final ObjectMapper mapper = new ObjectMapper();

	@Test(groups = { TestGroups.ALL })
	public void base64() {
		final HashMap<String, Long> cardinality = new HashMap<>();
		cardinality.put("SGVsbG8=", 2000000L);
		cardinality.put("V29ybGQ=", 2000000L);

		String result = Utils.determineStreamFormat(mapper, cardinality);
		assertEquals(result, "Base64");
	}

	@Test(groups = { TestGroups.ALL })
	public void JSON() {
		final HashMap<String, Long> cardinality = new HashMap<>();
		cardinality.put("{ \"name\": \"Tim\" }", 2000000L);
		cardinality.put("{ \"name\": \"Anna\" }", 2000000L);
		cardinality.put("{ \"name\": \"Bill\" }", 2000000L);

		String result = Utils.determineStreamFormat(mapper, cardinality);
		assertEquals(result, "JSON");
	}

	@Test(groups = { TestGroups.ALL })
	public void wellFormedHTML() {
		final HashMap<String, Long> cardinality = new HashMap<>();
		cardinality.put("<!DOCTYPE html><html></html>", 2000000L);
		cardinality.put("<head><title>My fabulous blog</title></head>", 2000000L);

		String result = Utils.determineStreamFormat(mapper, cardinality);
		assertEquals(result, "XML");
	}

	@Test(groups = { TestGroups.ALL })
	public void HTML() {
		final HashMap<String, Long> cardinality = new HashMap<>();
		cardinality.put("<p>Hello", 2000000L);
		cardinality.put("<head><title>My fabulous blog</title></head>", 2000000L);

		String result = Utils.determineStreamFormat(mapper, cardinality);
		assertEquals(result, "HTML");
	}

	@Test(groups = { TestGroups.ALL })
	public void wordsHelloWorld() {
		final List<String> words = (new WordProcessor()).asWords("Hello world!");
		assertEquals(words.get(0), "Hello");
		assertEquals(words.get(1), "world");
	}

	@Test(groups = { TestGroups.ALL })
	public void wordsCount() {
		final List<String> words = (new WordProcessor()).asWords("   One, two, three,four!");
		assertEquals(words.get(0), "One");
		assertEquals(words.get(1), "two");
		assertEquals(words.get(2), "three");
		assertEquals(words.get(3), "four");
	}

	@Test(groups = { TestGroups.ALL })
	public void wordsWithNumbersCount() {
		final List<String> words = (new WordProcessor()).asWords("4814 Hollywood Blvd., Los Angeles, CA 90027");
		assertEquals(words.get(0), "4814");
		assertEquals(words.get(1), "Hollywood");
		assertEquals(words.get(2), "Blvd");
		assertEquals(words.get(3), "Los");
		assertEquals(words.get(4), "Angeles");
		assertEquals(words.get(5), "CA");
		assertEquals(words.get(6), "90027");
	}

	@Test(groups = { TestGroups.ALL })
	public void wordsWithHyphens() {
		final List<String> words = (new WordProcessor()).withAdditionalWordChars("-#").asWords("586 E 800 N         Orem UT 84097-4146");
		assertEquals(words.get(0), "586");
		assertEquals(words.get(1), "E");
		assertEquals(words.get(2), "800");
		assertEquals(words.get(3), "N");
		assertEquals(words.get(4), "Orem");
		assertEquals(words.get(5), "UT");
		assertEquals(words.get(6), "84097-4146");
	}

	@Test(groups = { TestGroups.ALL })
	public void wordsWithHashes() {
		final List<String> words = (new WordProcessor()).withAdditionalWordChars("-#").asWords("9350   WILSHIRE BLVD   SUITE #203");
		assertEquals(words.get(0), "9350");
		assertEquals(words.get(1), "WILSHIRE");
		assertEquals(words.get(2), "BLVD");
		assertEquals(words.get(3), "SUITE");
		assertEquals(words.get(4), "#203");
	}

	@Test(groups = { TestGroups.ALL })
	public void wordsWithPeriods() {
		final List<String> words = (new WordProcessor()).asWords("sanne.stienstra@state.or.us");
		assertEquals(words.get(0), "sannestienstrastateorus");
	}

	@Test(groups = { TestGroups.ALL })
	public void wordsWithSlashes() {
		final List<String> words = (new WordProcessor()).asWords("WHITE/RED/STRIPES");
		assertEquals(words.get(0), "WHITE");
		assertEquals(words.get(1), "RED");
		assertEquals(words.get(2), "STRIPES");
	}

	/*
	@Test(groups = { TestGroups.ALL })
	public void wordsNoBreaks() {
		final List<String> words = (new WordProcessor()).asWords("< 18yrs");
		assertEquals(words.get(0), "WHITE");
		assertEquals(words.get(1), "RED");
		assertEquals(words.get(2), "STRIPES");
	}
	*/

	@Test(groups = { TestGroups.ALL })
	public void wordsWithNewLines() {
		final List<String> words = (new WordProcessor()).withAdditionalWordChars("-#").asWords("100 Aldrich Street, Bldg 15A\nBronx, NY 10475\n(40.87007051900008, -73.83225591699994)");
		assertEquals(words.get(0), "100");
		assertEquals(words.get(1), "Aldrich");
		assertEquals(words.get(2), "Street");
		assertEquals(words.get(3), "Bldg");
		assertEquals(words.get(4), "15A");
		assertEquals(words.get(5), "Bronx");
	}

	@Test(groups = { TestGroups.ALL })
	public void wordsRedGreenDefault() {
		final List<String> words = (new WordProcessor()).asWords("RED-GREEN");
		assertEquals(words.get(0), "RED");
		assertEquals(words.get(1), "GREEN");
	}

	@Test(groups = { TestGroups.ALL })
	public void wordsRedGreenNonDefault() {
		final List<String> words = (new WordProcessor()).withAdditionalWordChars("-#").asWords("RED-GREEN");
		assertEquals(words.get(0), "RED-GREEN");
	}

	@Test(groups = { TestGroups.ALL })
	public void wordsLanguages() {
		final List<String> words = (new WordProcessor()).asWords("french;german;italian");
		assertEquals(words.get(0), "french");
		assertEquals(words.get(1), "german");
		assertEquals(words.get(2), "italian");
	}

	@Test(groups = { TestGroups.ALL })
	public void wordsNBSP() {
		final List<String> words = (new WordProcessor()).asWords("122 Amsterdam Ave  New York, NY 10023");
		assertEquals(words.get(0), "122");
		assertEquals(words.get(1), "Amsterdam");
		assertEquals(words.get(2), "Ave");
		assertEquals(words.get(3), "New");
	}

	@Test(groups = { TestGroups.ALL })
	public void wordsAmpersand() {
		final List<String> words = (new WordProcessor()).asWords("Cnr Gungahlin Drive & The Valley Avenue");
		assertEquals(words.get(0), "Cnr");
		assertEquals(words.get(1), "Gungahlin");
		assertEquals(words.get(2), "Drive");
		assertEquals(words.get(3), "The");
	}

	@Test(groups = { TestGroups.ALL })
	public void wordsSlash() {
		final List<String> words = (new WordProcessor()).asWords("N ANDREWS AV/NE 62ND ST");
		assertEquals(words.get(0), "N");
		assertEquals(words.get(1), "ANDREWS");
		assertEquals(words.get(2), "AV");
		assertEquals(words.get(3), "NE");
		assertEquals(words.get(4), "62ND");
		assertEquals(words.get(5), "ST");
	}

	@Test(groups = { TestGroups.ALL })
	public void wordAgeRange() {
		final List<String> words = new WordProcessor().withBreakChars(" \u00A0-").asWords(Utils.cleanse("00–4"));
		assertEquals(words.get(0), "00");
		assertEquals(words.get(1), "4");
	}

	@Test(groups = { TestGroups.ALL })
	public void wordOffsets() {
		final String testCase = "1025 41st Avenue\nQueens, NEW YORK 11101";
		final List<WordOffset> words = (new WordProcessor()).asWordOffsets(testCase);
		assertEquals(words.get(0).word, "1025");
		assertEquals(words.get(0).offset, testCase.indexOf("1025"));
		assertEquals(words.get(1).word, "41st");
		assertEquals(words.get(1).offset, testCase.indexOf("41st"));
		assertEquals(words.get(2).word, "Avenue");
		assertEquals(words.get(2).offset, testCase.indexOf("Avenue"));
		assertEquals(words.get(3).word, "Queens");
		assertEquals(words.get(3).offset, testCase.indexOf("Queens"));
		assertEquals(words.get(4).word, "NEW");
		assertEquals(words.get(4).offset, testCase.indexOf("NEW"));
		assertEquals(words.get(5).word, "YORK");
		assertEquals(words.get(5).offset, testCase.indexOf("YORK"));
	}
}
