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
import static org.testng.Assert.fail;

import java.io.IOException;
import java.security.SecureRandom;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.core.RegExpSplitter;
import com.cobber.fta.core.Utils;
import com.cobber.fta.core.WordOffset;
import com.cobber.fta.core.WordProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestUtils {
	private final ObjectMapper mapper = new ObjectMapper();

	@Test(groups = { TestGroups.ALL })
	public void base64() {
		final Map<String, Long> cardinality = new HashMap<>();
		cardinality.put("SGVsbG8=", 2000000L);
		cardinality.put("V29ybGQ=", 2000000L);

		final String result = Utils.determineStreamFormat(mapper, cardinality);
		assertEquals(result, "Base64");
	}

	@Test(groups = { TestGroups.ALL })
	public void JSON() {
		final Map<String, Long> cardinality = new HashMap<>();
		cardinality.put("{ \"name\": \"Tim\" }", 2000000L);
		cardinality.put("{ \"name\": \"Anna\" }", 2000000L);
		cardinality.put("{ \"name\": \"Bill\" }", 2000000L);

		final String result = Utils.determineStreamFormat(mapper, cardinality);
		assertEquals(result, "JSON");
	}

	@Test(groups = { TestGroups.ALL })
	public void wellFormedHTML() {
		final Map<String, Long> cardinality = new HashMap<>();
		cardinality.put("<!DOCTYPE html><html></html>", 2000000L);
		cardinality.put("<head><title>My fabulous blog</title></head>", 2000000L);

		final String result = Utils.determineStreamFormat(mapper, cardinality);
		assertEquals(result, "XML");
	}

	@Test(groups = { TestGroups.ALL })
	public void HTML() {
		final Map<String, Long> cardinality = new HashMap<>();
		cardinality.put("<p>Hello", 2000000L);
		cardinality.put("<head><title>My fabulous blog</title></head>", 2000000L);

		final String result = Utils.determineStreamFormat(mapper, cardinality);
		assertEquals(result, "HTML");
	}

	@Test(groups = { TestGroups.ALL })
	public void noSamples() {
		final Map<String, Long> cardinality = new HashMap<>();

		final String result = Utils.determineStreamFormat(mapper, cardinality);
		assertNull(result);
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
	public void wordWithHyphen() {
		final List<String> words = (new WordProcessor()).withAdditionalWordChars("-#").asWords("84097-4146");
		assertEquals(words.get(0), "84097-4146");
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
	public void wordsQuoted() {
		final List<String> words = (new WordProcessor()).withAdditionalWordChars("'").asWords("'RED' 'GREEN'");
		assertEquals(words.get(0), "'RED'");
		assertEquals(words.get(1), "'GREEN'");
	}

	@Test(groups = { TestGroups.ALL })
	public void wordsWithPeriods() {
		final List<String> words = (new WordProcessor()).asWords("sanne.stienstra@state.or.us");
		assertEquals(words.get(0), "sanne");
		assertEquals(words.get(1), "stienstra");
		assertEquals(words.get(2), "@");
		assertEquals(words.get(3), "state");
		assertEquals(words.get(4), "or");
		assertEquals(words.get(5), "us");
		assertEquals(words.size(), 6);
	}

	@Test(groups = { TestGroups.ALL })
	public void wordsWithSlashes() {
		final List<String> words = (new WordProcessor()).asWords("WHITE/RED/STRIPES");
		assertEquals(words.get(0), "WHITE");
		assertEquals(words.get(1), "RED");
		assertEquals(words.get(2), "STRIPES");
		assertEquals(words.size(), 3);
	}

	@Test(groups = { TestGroups.ALL })
	public void wordsNoBreaks() {
		final List<String> words = (new WordProcessor()).asWords("< 18yrs");
		assertEquals(words.get(0), "<");
		assertEquals(words.get(1), "18yrs");
		assertEquals(words.size(), 2);
	}

	@Test(groups = { TestGroups.ALL })
	public void wordsNoBreaksAlphaTransition() {
		final List<String> words = (new WordProcessor()).withAlphaNumberTransition(true).asWords("< 18yrs");
		assertEquals(words.get(0), "<");
		assertEquals(words.get(1), "18");
		assertEquals(words.get(2), "yrs");
		assertEquals(words.size(), 3);
	}


	@Test(groups = { TestGroups.ALL })
	public void wordsWithNewLines() {
		final List<String> words = (new WordProcessor()).withAdditionalWordChars("-#").asWords("100 Aldrich Street, Bldg 15A\nBronx, NY 10475\n(40.87007051900008, -73.83225591699994)");
		assertEquals(words.get(0), "100");
		assertEquals(words.get(1), "Aldrich");
		assertEquals(words.get(2), "Street");
		assertEquals(words.get(3), "Bldg");
		assertEquals(words.get(4), "15A");
		assertEquals(words.get(5), "Bronx");
		assertEquals(words.get(6), "NY");
		assertEquals(words.get(7), "10475");
		//(40.87007051900008, -73.83225591699994)"
	}

	@Test(groups = { TestGroups.ALL })
	public void wordsKillChars() {
		final List<String> words = (new WordProcessor()).withAdditionalWordChars("-#").asWords("\"7469 GRACELY DR, CINC - \"");
		assertEquals(words.get(0), "7469");
		assertEquals(words.get(1), "GRACELY");
		assertEquals(words.get(2), "DR");
		assertEquals(words.get(3), "CINC");
		assertEquals(words.get(4), "-");
		assertEquals(words.size(), 5);
	}

	@Test(groups = { TestGroups.ALL })
	public void wordsRedGreenDefault() {
		final List<String> words = (new WordProcessor()).asWords("RED-GREEN");
		assertEquals(words.get(0), "RED");
		assertEquals(words.get(1), "GREEN");
		assertEquals(words.size(), 2);
	}

	@Test(groups = { TestGroups.ALL })
	public void wordsRedGreenNonDefault() {
		final List<String> words = (new WordProcessor()).withAdditionalWordChars("-#").asWords("RED-GREEN");
		assertEquals(words.get(0), "RED-GREEN");
		assertEquals(words.size(), 1);
	}

	@Test(groups = { TestGroups.ALL })
	public void wordsLanguages() {
		final List<String> words = (new WordProcessor()).asWords("french;german;italian");
		assertEquals(words.get(0), "french");
		assertEquals(words.get(1), "german");
		assertEquals(words.get(2), "italian");
		assertEquals(words.size(), 3);
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

	@Test(groups = { TestGroups.ALL })
	public void wordRace() {
		final List<String> words = (new WordProcessor()).asWords("White or Caucasian (e.g. Anglo, European, etc.)");
		assertEquals(words.get(0), "White");
		assertEquals(words.get(1), "or");
		assertEquals(words.get(2), "Caucasian");
		assertEquals(words.get(3), "e");
		assertEquals(words.get(4), "g");
		assertEquals(words.get(5), "Anglo");
		assertEquals(words.get(6), "European");
	}

	@Test(groups = { TestGroups.ALL })
	public void wordLanguage() {
		final List<String> words = (new WordProcessor()).asWords("Tagalog (Pilipino_ Filipino)");
		assertEquals(words.get(0), "Tagalog");
		assertEquals(words.get(1), "Pilipino");
		assertEquals(words.get(2), "Filipino");
	}

	private void badDouble(final String input, final NumberFormat doubleFormatter) {
		try {
			Utils.parseDouble(input, doubleFormatter);
			fail();
		}
		catch (NumberFormatException e) {
			// Do NOTHING
		}
	}

	@Test(groups = { TestGroups.ALL })
	public void exponentUS() {
		// US Must be E not e and no +
		final NumberFormat nf = NumberFormat.getInstance(Locale.forLanguageTag("en-US"));
		nf.setParseIntegerOnly(false);

		assertEquals(Utils.parseDouble("3.331E14", nf), 3.331E14);
		assertEquals(Utils.parseDouble("3.331E-14", nf), 3.331E-14);
		assertEquals(Utils.parseDouble("3.331E+14", nf), 3.331E14);
		assertEquals(Utils.parseDouble("3.331e14", nf), 3.331E14);
		assertEquals(Utils.parseDouble("3.331e+14", nf), 3.331E14);

		assertEquals(Utils.parseDouble("3.331E9", nf), 3.331E9);
		assertEquals(Utils.parseDouble("3.331E-9", nf), 3.331E-9);
		assertEquals(Utils.parseDouble("3.331E+9", nf), 3.331E9);
		assertEquals(Utils.parseDouble("3.331e9", nf), 3.331E9);

		badDouble("3.331E", nf);
		badDouble("3.331E+", nf);
		badDouble("3.331e", nf);
	}

	@Test(groups = { TestGroups.ALL })
	public void ExponentNL() {
		// NL Must be E not e and no +
		final NumberFormat nf = NumberFormat.getInstance(Locale.forLanguageTag("nl-NL"));
		nf.setParseIntegerOnly(false);

		assertEquals(Utils.parseDouble("3,331E14", nf), 3.331E14);
		assertEquals(Utils.parseDouble("3,331E-14", nf), 3.331E-14);
		assertEquals(Utils.parseDouble("3,331E+14", nf), 3.331E14);
		assertEquals(Utils.parseDouble("3,331e14", nf), 3.331E14);

		assertEquals(Utils.parseDouble("3,331E9", nf), 3.331E9);
		assertEquals(Utils.parseDouble("3,331E-9", nf), 3.331E-9);
		assertEquals(Utils.parseDouble("3,331E+9", nf), 3.331E9);
		assertEquals(Utils.parseDouble("3,331e9", nf), 3.331E9);

		badDouble("3.331E", nf);
		badDouble("3.331E+", nf);
		badDouble("3.331e", nf);
	}

	@Test(groups = { TestGroups.ALL })
	public void testRegExpSplitter_1_1() {
		RegExpSplitter  splitter = RegExpSplitter.newInstance("{3,9}");

		assertEquals(splitter.getMin(), 3);
		assertEquals(splitter.getMax(), 9);
	}

	@Test(groups = { TestGroups.ALL })
	public void testRegExpSplitter_1_2() {
		RegExpSplitter  splitter = RegExpSplitter.newInstance("{3,10}");

		assertEquals(splitter.getMin(), 3);
		assertEquals(splitter.getMax(), 10);
	}

	@Test(groups = { TestGroups.ALL })
	public void testRegExpSplitter_2_2() {
		RegExpSplitter  splitter = RegExpSplitter.newInstance("{10,12}");

		assertEquals(splitter.getMin(), 10);
		assertEquals(splitter.getMax(), 12);
	}

	@Test(groups = { TestGroups.ALL })
	public void testUniqueness() throws IOException {
		double d = 0.0;

		// Birthday paradox - with 23 people in a room, 2 should have the same birthday
		d = Utils.uniquenessProbability(365, 23);
		assertTrue(d > .50 && d < .51);

		d = Utils.uniquenessProbability(99999, 1000);
		assertTrue(d > .99);
	}

	@Test(groups = { TestGroups.ALL })
	public void randomDigits() {
		final byte[] seed = { 3, 1, 4, 1, 5, 9, 2 };
		final SecureRandom random = new SecureRandom(seed);

		for (int i = 0; i < 100; i++) {
			final String random9 = Utils.getRandomDigits(random, 1 + i%10);
			assertEquals(random9.length(), 1 + i%10);
			assertTrue(Utils.isNumeric(random9));
			assertTrue(random9.charAt(0) != '0');
		}

		assertFalse(Utils.isNumeric(null));
		assertFalse(Utils.isNumeric(""));
	}

	@Test(groups = { TestGroups.ALL })
	public void testAllZeroes() {
		assertTrue(Utils.allZeroes("0"));
		assertTrue(Utils.allZeroes("00000000"));
		assertFalse(Utils.allZeroes(""));
		assertFalse(Utils.allZeroes(null));
		assertFalse(Utils.allZeroes("."));
		assertFalse(Utils.allZeroes("10000000"));
		assertFalse(Utils.allZeroes("00000001"));
	}

	@Test(groups = { TestGroups.ALL })
	public void testIsSimpleAlphaNumeric() {
		assertTrue(Utils.isSimpleAlphaNumeric('0'));
		assertTrue(Utils.isSimpleAlphaNumeric('9'));
		assertTrue(Utils.isSimpleAlphaNumeric('a'));
		assertTrue(Utils.isSimpleAlphaNumeric('z'));
		assertTrue(Utils.isSimpleAlphaNumeric('A'));
		assertTrue(Utils.isSimpleAlphaNumeric('Z'));
		assertFalse(Utils.isSimpleAlphaNumeric('å'));
	}

	@Test(groups = { TestGroups.ALL })
	public void testIsSimpleAlpha() {
		assertTrue(Utils.isSimpleAlpha('a'));
		assertTrue(Utils.isSimpleAlpha('z'));
		assertTrue(Utils.isSimpleAlpha('A'));
		assertTrue(Utils.isSimpleAlpha('Z'));
		assertFalse(Utils.isSimpleAlpha('0'));
		assertFalse(Utils.isSimpleAlpha('9'));
		assertFalse(Utils.isSimpleAlpha('å'));
	}

	@Test(groups = { TestGroups.ALL })
	public void testIsSimpleNumeric() {
		assertTrue(Utils.isSimpleNumeric('0'));
		assertTrue(Utils.isSimpleNumeric('9'));
		assertFalse(Utils.isSimpleNumeric('０'));
		assertFalse(Utils.isSimpleNumeric('a'));
		assertFalse(Utils.isSimpleNumeric('z'));
		assertFalse(Utils.isSimpleNumeric('A'));
		assertFalse(Utils.isSimpleNumeric('Z'));
	}

	@Test(groups = { TestGroups.ALL })
	public void testIsAlphas() {
		assertTrue(Utils.isAlphas("Hello"));
		assertFalse(Utils.isAlphas("Number9"));
		assertFalse(Utils.isAlphas("Period."));
		assertFalse(Utils.isAlphas(""));
		assertFalse(Utils.isAlphas(null));
	}

	@Test(groups = { TestGroups.ALL })
	public void testCleanse() {
		assertEquals(Utils.cleanse("“–“ U+2013 En Dash Unicode Character"), "\"-\" U+2013 En Dash Unicode Character");
		assertEquals(Utils.cleanse("“—” U+2014 Em Dash Unicode Character"), "\"-\" U+2014 Em Dash Unicode Character");
		assertEquals(Utils.cleanse("““” U+201C Left Double Quotation Mark Unicode Character"), "\"\"\" U+201C Left Double Quotation Mark Unicode Character");
		assertEquals(Utils.cleanse("Unicode Character “”” (U+201D)"), "Unicode Character \"\"\" (U+201D)");
		assertEquals(Utils.cleanse("“`” U+0060 Grave Accent Unicode Character"), "\"'\" U+0060 Grave Accent Unicode Character");
		assertEquals(Utils.cleanse("‘ U+2018 Left Single Quotation Mark Unicode Character"), "' U+2018 Left Single Quotation Mark Unicode Character");
		assertEquals(Utils.cleanse("nothing to do"), "nothing to do");
	}

	@Test(groups = { TestGroups.ALL })
	public void testreplaceFirst() {
		assertEquals(Utils.replaceFirst("one three three four", "three", "two"), "one two three four");
		assertEquals(Utils.replaceFirst("one three three four", "thrre", "two"), "one three three four");

		assertEquals(Utils.replaceLast("one two two four", "two", "three"), "one two three four");
		assertEquals(Utils.replaceLast("one three three four", "thrre", "two"), "one three three four");
	}

	@Test(groups = { TestGroups.ALL })
	public void testgetBaseName() {
		assertEquals(Utils.getBaseName("zoom.pdf"), "zoom");
		assertEquals(Utils.getBaseName("zoompdf"), "zoompdf");
		assertEquals(Utils.getBaseName("zoom.pdf.pdf"), "zoom.pdf");
	}


	@Test(groups = { TestGroups.ALL })
	public void testRegExpGenerator() {
		 RegExpGenerator generator = new RegExpGenerator();
		 for (int i = 0; i < 100; i++) {
			 generator.train("Red");
			 generator.train("Green");
			 generator.train("Blue");
		 }
		 assertEquals(generator.getResult(), "\\p{IsAlphabetic}{3,5}");

		 generator = new RegExpGenerator(20, Locale.getDefault());
		 for (int i = 0; i < 100; i++) {
			 generator.train("Red");
			 generator.train("Green");
			 generator.train("Blue");
		 }
		 assertEquals(generator.getResult(), "(?i)(BLUE|GREEN|RED)");

		 RegExpGenerator zip5 = new RegExpGenerator(20, Locale.getDefault());
		 final SecureRandom random = new SecureRandom();
		 for (int i = 0; i < 100; i++)
			 zip5.train(Utils.getRandomDigits(random, 5));
		 assertEquals(zip5.getResult(), "\\p{IsDigit}{5}");

		 final String zipPlus4 = "\\p{IsDigit}{5}-\\\\p{IsDigit}{4}";
		 assertEquals(RegExpGenerator.merge(zip5.getResult(), zipPlus4), "\\p{IsDigit}{5}(-\\\\p{IsDigit}{4})?");
		 assertEquals(RegExpGenerator.merge(zipPlus4, zip5.getResult()), "\\p{IsDigit}{5}(-\\\\p{IsDigit}{4})?");

		 assertEquals(RegExpGenerator.merge("\\p{IsDigit}{5}", "(?i)(BLUE|GREEN|RED)"), "(?i)(BLUE|GREEN|RED)|\\p{IsDigit}{5}");

		 generator = new RegExpGenerator();
		 for (int i = 0; i < 100; i++) {
			 generator.train(" ");
			 generator.train("  ");
			 generator.train("   ");
		 }
		 assertEquals(generator.getResult(), " {1,3}");

		 generator = new RegExpGenerator();
		 for (int i = 0; i < 100; i++) {
			 generator.train(" ");
			 generator.train("  ");
			 generator.train("            ");
		 }
		 assertEquals(generator.getResult(), " +");

		 generator = new RegExpGenerator(20, Locale.getDefault());
		 for (int i = 0; i < 100; i++) {
			 generator.train("A");
			 generator.train("B");
			 generator.train("C");
			 generator.train("D");
			 generator.train("E");
		 }
		 assertEquals(generator.getResult(), "[A-E]");

		 generator = new RegExpGenerator();
		 for (int i = 0; i < 100; i++) {
			 generator.train("0-9");
		 }
		 assertEquals(generator.getResult(), "[\\p{IsDigit}\\-]{3}");

		 generator = new RegExpGenerator();
		 for (int i = 0; i < 100; i++) {
			 generator.train("0_9");
		 }
		 assertEquals(generator.getResult(), "[\\p{IsDigit}_]{3}");

	}

	@Test(groups = { TestGroups.ALL })
	public void testSlosh() {
		assertEquals(RegExpGenerator.slosh("Hello"), "Hello");
		assertEquals(RegExpGenerator.slosh("0.9"), "0\\.9");
		assertEquals(RegExpGenerator.slosh("0..9"), "\\Q0..9\\E");
		assertEquals(RegExpGenerator.slosh("."), "\\.");
		assertEquals(RegExpGenerator.slosh("0"), "0");
	}

	@Test(groups = { TestGroups.ALL })
	public void testKnownTypesUS() {
		final KnownTypes knownTypes = new KnownTypes();
		knownTypes.initialize(Locale.US);
		assertEquals(knownTypes.PATTERN_LONG, knownTypes.getByID(KnownTypes.ID.ID_LONG).regexp);
		assertEquals(knownTypes.PATTERN_LONG_GROUPING, "[\\d,]+");
		assertEquals(knownTypes.PATTERN_LONG_GROUPING, knownTypes.getByID(KnownTypes.ID.ID_LONG_GROUPING).regexp);
		assertEquals(knownTypes.PATTERN_LONG_GROUPING, knownTypes.grouping(KnownTypes.ID.ID_LONG).regexp);
		assertEquals(knownTypes.PATTERN_DOUBLE, "\\d*\\.?\\d+");
		assertEquals(KnownTypes.PATTERN_ALPHA, "\\p{IsAlphabetic}");
	}

	@Test(groups = { TestGroups.ALL })
	public void testKnownTypesDE() {
		final KnownTypes knownTypes = new KnownTypes();
		knownTypes.initialize(Locale.GERMAN);
		assertEquals(knownTypes.PATTERN_LONG, knownTypes.getByID(KnownTypes.ID.ID_LONG).regexp);
		assertEquals(knownTypes.PATTERN_LONG_GROUPING, "[\\d\\.]+");
		assertEquals(knownTypes.PATTERN_LONG_GROUPING, knownTypes.getByID(KnownTypes.ID.ID_LONG_GROUPING).regexp);
		assertEquals(knownTypes.PATTERN_LONG_GROUPING, knownTypes.grouping(KnownTypes.ID.ID_LONG).regexp);
		assertEquals(knownTypes.PATTERN_SIGNED_LONG, knownTypes.negation(knownTypes.PATTERN_LONG).regexp);
		assertEquals(knownTypes.PATTERN_DOUBLE, "\\d*,?\\d+");
		assertEquals(KnownTypes.PATTERN_ALPHA, "\\p{IsAlphabetic}");

		final TypeInfo typeInfoNL = knownTypes.getByID(KnownTypes.ID.ID_DOUBLE_NL);
		assertEquals(knownTypes.PATTERN_DOUBLE_NL, typeInfoNL.regexp);
		assertTrue(typeInfoNL.isNonLocalized());
	}

	@Test(groups = { TestGroups.ALL })
	public void testKnownTypesSV() {
		final KnownTypes knownTypes = new KnownTypes();
		knownTypes.initialize(Locale.forLanguageTag("se-SV"));
		assertEquals(knownTypes.PATTERN_LONG, knownTypes.getByID(KnownTypes.ID.ID_LONG).regexp);
		assertEquals(knownTypes.PATTERN_LONG_GROUPING, "[\\d ]+");
		assertEquals(knownTypes.PATTERN_LONG_GROUPING, knownTypes.getByID(KnownTypes.ID.ID_LONG_GROUPING).regexp);
		assertEquals(knownTypes.PATTERN_LONG_GROUPING, knownTypes.grouping(KnownTypes.ID.ID_LONG).regexp);
		assertEquals(knownTypes.PATTERN_SIGNED_LONG, knownTypes.negation(knownTypes.PATTERN_LONG).regexp);
		assertEquals(knownTypes.PATTERN_DOUBLE, "\\d*,?\\d+");
		assertEquals(KnownTypes.PATTERN_ALPHA, "\\p{IsAlphabetic}");
	}

	@Test(groups = { TestGroups.ALL })
	public void typeInfo() {
		final KnownTypes knownTypes = new KnownTypes();
		knownTypes.initialize(Locale.US);
		assertTrue(knownTypes.getByID(KnownTypes.ID.ID_LONG).equals(knownTypes.getByID(KnownTypes.ID.ID_LONG)));
		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_LONG).equals(knownTypes.getByID(KnownTypes.ID.ID_LONG_GROUPING)));
		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_LONG).equals(null));

		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_LONG).hasGrouping());
		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_LONG).isSigned());
		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_LONG).hasExponent());
		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_LONG).isTrailingMinus());
		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_LONG).isNull());
		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_LONG).isBlank());
		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_LONG).isBlankOrNull());
		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_LONG).isSemanticType());
		assertTrue(knownTypes.getByID(KnownTypes.ID.ID_LONG).isNumeric());
		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_LONG).isAlphabetic());
		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_LONG).isAlphanumeric());
		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_LONG).isDateType());
		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_LONG).isForce());
		assertEquals(knownTypes.getByID(KnownTypes.ID.ID_LONG).getBaseType(), FTAType.LONG);
		assertNull(knownTypes.getByID(KnownTypes.ID.ID_LONG).getSemanticType());

		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_BLANK).isNull());
		assertTrue(knownTypes.getByID(KnownTypes.ID.ID_BLANK).isBlank());
		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_BLANK).isBlankOrNull());

		assertTrue(knownTypes.getByID(KnownTypes.ID.ID_NULL).isNull());
		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_NULL).isBlank());
		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_NULL).isBlankOrNull());

		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_BLANKORNULL).isNull());
		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_BLANKORNULL).isBlank());
		assertTrue(knownTypes.getByID(KnownTypes.ID.ID_BLANKORNULL).isBlankOrNull());

		assertTrue(knownTypes.getByID(KnownTypes.ID.ID_LONG_GROUPING).hasGrouping());
		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_LONG_GROUPING).isSigned());
		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_LONG_GROUPING).hasExponent());

		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_SIGNED_LONG).hasGrouping());
		assertTrue(knownTypes.getByID(KnownTypes.ID.ID_SIGNED_LONG).isSigned());
		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_SIGNED_LONG).hasExponent());

		assertTrue(knownTypes.getByID(KnownTypes.ID.ID_SIGNED_LONG_GROUPING).hasGrouping());
		assertTrue(knownTypes.getByID(KnownTypes.ID.ID_SIGNED_LONG_GROUPING).isSigned());
		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_SIGNED_LONG_GROUPING).hasExponent());

		assertTrue(knownTypes.getByID(KnownTypes.ID.ID_SIGNED_DOUBLE_WITH_EXPONENT_GROUPING).hasGrouping());
		assertTrue(knownTypes.getByID(KnownTypes.ID.ID_SIGNED_DOUBLE_WITH_EXPONENT_GROUPING).isSigned());
		assertTrue(knownTypes.getByID(KnownTypes.ID.ID_SIGNED_DOUBLE_WITH_EXPONENT_GROUPING).hasExponent());

		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_ALPHA_VARIABLE).isNumeric());
		assertTrue(knownTypes.getByID(KnownTypes.ID.ID_ALPHA_VARIABLE).isAlphabetic());
		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_ALPHA_VARIABLE).isAlphanumeric());

		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_ALPHANUMERIC_VARIABLE).isNumeric());
		assertFalse(knownTypes.getByID(KnownTypes.ID.ID_ALPHANUMERIC_VARIABLE).isAlphabetic());
		assertTrue(knownTypes.getByID(KnownTypes.ID.ID_ALPHANUMERIC_VARIABLE).isAlphanumeric());

		TypeInfo clone = new TypeInfo(knownTypes.getByID(KnownTypes.ID.ID_SIGNED_LONG_GROUPING));
		assertTrue(clone.equals(knownTypes.getByID(KnownTypes.ID.ID_SIGNED_LONG_GROUPING)));

		TypeInfo semantic1000 = new TypeInfo("[0-9][0-9][0-9][0-9]", FTAType.LONG, "PLUS4", knownTypes.getByID(KnownTypes.ID.ID_LONG));
		assertFalse(semantic1000.equals(knownTypes.getByID(KnownTypes.ID.ID_LONG)));
	}

	@Test(groups = { TestGroups.ALL })
	public void testKeyWords() {
		final Keywords keywordsUS = Keywords.getInstance(Locale.US);
		assertEquals(keywordsUS.get("YES"), "yes");
		assertNull(keywordsUS.get("YODEL"));
		assertEquals(keywordsUS.match("year", "YEAR"), 90);
		assertEquals(keywordsUS.match("yearly", "YEAR"), 90);
		assertEquals(keywordsUS.match(null, "YEAR"), 0);
		assertEquals(keywordsUS.match("", "YEAR"), 0);
		assertEquals(keywordsUS.match("rubbish", "YEAR"), 0);
		assertEquals(keywordsUS.match("yodel", "YODEL"), 0);
		assertEquals(keywordsUS.match("yodel", "YES"), 0);

		final Keywords keywordsFR = Keywords.getInstance(Locale.FRANCE);
		assertEquals(keywordsFR.get("YES"), "oui");
		assertEquals(keywordsFR.match("année", "YEAR"), 90);
		assertEquals(keywordsFR.match("year", "YEAR"), 90);
	}
}
