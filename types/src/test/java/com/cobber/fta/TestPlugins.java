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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;
import com.cobber.fta.plugins.AddressEN;
import com.cobber.fta.plugins.CheckDigitABA;
import com.cobber.fta.plugins.CheckDigitCUSIP;
import com.cobber.fta.plugins.CheckDigitEAN13;
import com.cobber.fta.plugins.CheckDigitIBAN;
import com.cobber.fta.plugins.CheckDigitISBN;
import com.cobber.fta.plugins.CheckDigitISIN;
import com.cobber.fta.plugins.CheckDigitLuhn;
import com.cobber.fta.plugins.CheckDigitSEDOL;
import com.cobber.fta.plugins.CountryEN;
import com.cobber.fta.plugins.EmailLT;
import com.cobber.fta.plugins.FirstName;
import com.cobber.fta.plugins.GUID;
import com.cobber.fta.plugins.Gender;
import com.cobber.fta.plugins.IPV4Address;
import com.cobber.fta.plugins.PhoneNumberLT;
import com.cobber.fta.plugins.URLLT;
import com.cobber.fta.plugins.USZip5;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

public class TestPlugins {
	private static final SecureRandom random = new SecureRandom();

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicGenderTwoValues() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Gender");
		final String pipedInput = "Female|MALE|Male|Female|Female|MALE|Female|Female|Male|" +
				"Male|Female|Male|Male|Male|Female|Female|Male|Male|Male|" +
				"MALE|FEMALE|MALE|FEMALE|FEMALE|MALE|FEMALE|MALE|" +
				"Female|Male|Female|FEMALE|Male|Female|male|Male|Male|male|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), Gender.SEMANTIC_TYPE + "EN");
		assertEquals(result.getStructureSignature(), analysis.getPlugins().getRegistered(Gender.SEMANTIC_TYPE + "EN").getSignature());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "(?i)(FEMALE|MALE)");
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicGenderTwoValuesMF() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Gender");
		final String pipedInput = "F|M|M|F|F|M|F|F|M|" +
				"M|F|M|M|M|F|F|M|M|M|" +
				"M|F|M|F|F|M|F|M|" +
				"F|M|F|F|M|F|M|M|M|M|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(),  Gender.SEMANTIC_TYPE + "EN");
		assertEquals(result.getStructureSignature(), analysis.getPlugins().getRegistered(Gender.SEMANTIC_TYPE + "EN").getSignature());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "(?i)(F|M)");
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicGenderThreeValues() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Gender");
		final int UNKNOWNS = 3;
		final String pipedInput = "F|M|M|F|F|M|F|F|M|U|" +
				"M|F|M|M|M|F|F|M|M|M|U|" +
				"M|F|M|F|F|M|F|M|U|" +
				"F|M|F|F|M|F|M|M|M|M|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(),  Gender.SEMANTIC_TYPE + "EN");
		assertEquals(result.getStructureSignature(), analysis.getPlugins().getRegistered(Gender.SEMANTIC_TYPE + "EN").getSignature());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "(?i)(F|M|U)");
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length - UNKNOWNS);
		assertEquals(result.getConfidence(), 1 - (double)UNKNOWNS/result.getSampleCount());

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicGender() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Gender");
		final String pipedInput = "Female|MALE|Male|Female|Female|MALE|Female|Female|Unknown|Male|" +
				"Male|Female|Male|Male|Male|Female|Female|Male|Male|Male|" +
				"Female|Male|Female|FEMALE|Male|Female|male|Male|Male|male|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), Gender.SEMANTIC_TYPE + "EN");
		assertEquals(result.getStructureSignature(), analysis.getPlugins().getRegistered(Gender.SEMANTIC_TYPE + "EN").getSignature());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "(?i)(FEMALE|MALE|UNKNOWN)");
		assertEquals(result.getMatchCount(), inputs.length - 1);
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());

		final LogicalType logicalGender = analysis.getPlugins().getRegistered(Gender.SEMANTIC_TYPE + "EN");
		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()), input);
			final boolean expected = "male".equalsIgnoreCase(input.trim()) || "female".equalsIgnoreCase(input.trim());
			assertEquals(logicalGender.isValid(input, true), expected);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicGenderNL() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("GESLACH");
		final int UNKNOWNS = 3;
		analysis.setLocale(Locale.forLanguageTag("nl-NL"));

		final String pipedInput = "M|V|M|M|O|V|M|O|M|M|M|V|M|V|V|M|M|V|M|V|M|M|M|M|O|M|V|M|V|M|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "GENDER.TEXT_NL");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "(?i)(M|O|V)");
		assertEquals(result.getMatchCount(), inputs.length - UNKNOWNS);
		assertEquals(result.getConfidence(), 1 - (double)UNKNOWNS/result.getSampleCount());

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicGenderDE() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Gender");
		analysis.setLocale(Locale.forLanguageTag("de-AT"));

		final String pipedInput = "Female|MALE|Male|Female|Female|MALE|Female|Female|Unknown|Male|" +
				"Male|Female|Male|Male|Male|Female|Female|Male|Male|Male|" +
				"Female|Male|Female|FEMALE|Male|Female|male|Male|Male|male|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeQualifier());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "(?i)(FEMALE|MALE|UNKNOWN)");
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void trickLatitude() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("latitude");
		analysis.setLocale(Locale.forLanguageTag("de-DE"));

		final String[] inputs = {
				"54.176658700787", "54.1523286823181", "54.1507845291159", "54.1444646959388", "54.0948626983874",
				"54.099612908786", "54.0928342952505", "54.1492128935414", "54.0996275412016", "54.1767338631483",
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeQualifier(), "COORDINATE.LATITUDE_DECIMAL");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "(-?([0-9]|[0-8][0-9])\\.\\d+)|-?90\\.0+");
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()), input);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void latitudeDMS() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("PRIMARY_LAT_DMS");

		final String[] inputs = {
				"402628N", "402427N", "402402N", "402743N", "402543N", "402948N", "402311N", "402621N", "403308N", "403306N",
				"402536N", "402319N", "402808N", "402846N", "402940N", "402659N", "402326N", "401814N", "374458N", "385855N",
				"393617N", "395150N", "405638N", "384141N", "400938N", "392534N", "385957N", "395325N", "392609N", "372359N",
				"401324N", "374640N", "394807N", "390734N", "401008N", "374419N", "410949N", "405543N", "401205N", "392837N",
				"390016N", "412252N", "420158N", "414315N", "414941N", "393903N", "404816N", "410717N", "381018N", "374655N",
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "COORDINATE.LATITUDE_DMS");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "(\\d{5,6}|\\d{1,3} \\d{1,2} \\d{1,2} ?)[NnSs]");
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()), input);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void longitudeDMS() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("PRIM_LONG_DMS");

		final String[] inputs = {
				"0894332W", "0893834W", "0893832W", "0894457W", "0894454W", "0894429W", "0894259W", "0894414W", "0894038W", "0894356W",
				"0894408W", "0894635W", "0893809W", "0894024W", "0893906W", "0894429W", "0894300W", "0894358W", "0884311W", "0890508W",
				"0872305W", "0912038W", "0900136W", "0891621W", "0884901W", "0905221W", "0903358W", "0881050W", "0874208W", "0892343W",
				"0904357W", "0891239W", "0894633W", "0904053W", "0881823W", "0884309W", "0880635W", "0901110W", "0902733W", "0891553W",
				"0890942W", "0894706W", "0881808W", "0873718W", "0873800W", "0905342W", "0902406W", "0905001W", "0885837W", "0883739W",
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "COORDINATE.LONGITUDE_DMS");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "(\\d{5,7}|\\d{1,2} \\d{1,2} \\d{1,2} ?)[EeWw]");
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()), input);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void VAT_AT() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("UID");
		analysis.setLocale(Locale.forLanguageTag("de-AT"));

		final String[] inputs = {
				"ATU11111116", "ATU22222226", "ATU33333336", "ATU44444446", "ATU55555553",
				"ATU66666663", "ATU77777773", "ATU88888883", "ATU99999993", "ATU12345675",
				"ATU00000024", "ATU00000033", "ATU00000042", "ATU00000060", "ATU00000079",
				"ATU00000088", "ATU00000104", "ATU00000113", "ATU00000122", "ATU00000140",
				"ATU00000159", "ATU00000168", "ATU00000186", "ATU00000195", "ATU00000202",
				"ATU00000202", "ATU10223006", "ATU12011204", "ATU15110001",
				"ATU15394605", "ATU15416707", "ATU15662209", "ATU16370905", "ATU23224909",
				"ATU25775505", "ATU28560205", "ATU28609707", "ATU28617100", "ATU29288909",
				"ATU37675002", "ATU37785508", "ATU37830200", "ATU38420507", "ATU38516405",
				"ATU39364503", "ATU42527002", "ATU43666001", "ATU43716207", "ATU45766309",
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "IDENTITY.VAT_AT");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "(AT)?U\\d{8}");
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()), input);
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

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "IDENTITY.VAT_FR");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "(FR)?\\d{11}");
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()), input);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void VAT_UK1() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("VAT Registration Number");
		analysis.setLocale(Locale.forLanguageTag("en-UK"));

		final String[] inputs = {
				"654434043", "654961603", "902905932", "902905932", "902905932",
				"902905932", "902905932", "483797387", "654434043", "654434043",
				"654434729", "654434043", "654434043", "654434729", "673426621",
				"768362492", "654434729", "902905932", "902905932", "902905932",
				"902905932", "902905932", "896107201", "669318888", "654434729",
				"654943996", "873342418", "768362492", "768362492", "768362492",
				"654943996", "654943996", "654434729", "654434043", "654434043",
				"902905932", "902905932", "902905932", "902905932", "902905932",
				"781498779", "781498779", "654434043", "654434043", "654434729",
				"902905932", "902905932", "902905932", "902905932", "902905932",
				"902905932", "902905932", "902905932", "902905932", "902905932",
				"902905932", "902905932", "902905932", "902905932", "902905932",
				"902905932", "902905932", "902905932", "902905932", "902905932",
				"902905932", "902905932", "902905932", "313651680", "654943996",
				"902905932", "902905932", "902905932", "902905932", "902905932",
				"902905932", "654961603", "673426621", "125483810"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "IDENTITY.VAT_GB");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "[ \\d]{9}");
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()), input);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void VAT_UK2() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("VAT Registration Number");
		analysis.setLocale(Locale.forLanguageTag("en-GB"));

		final String[] inputs = {
				"GB654430839", "GB654430839", "GB654430839", "GB654430839", "GB654430839",
				"GB654430839", "GB654430839", "GB654430839", "GB654430839", "GB654430839",
				"GB654430839", "GB654430839", "GB654430839", "GB654430839", "GB654430839",
				"GB654430839", "GB654430839", "GB654430839", "GB654430839", "GB654430839",
				"GB654430839", "GB654430839", "GB654430839", "GB654430839", "GB654430839",
				"GB654430839", "GB654430839", "654966000", "915970991", "GB 654442045",
				"GB 654442045", "GB 654442045", "GB 654442045", "GB 654442045", "GB 654442045",
				"218180129", "654435138", "654435138", "654435138", "654435138",
				"654435138", "218788662", "218788662", "218788662", "GB654430839",
				"906917408", "218180129", "GB654951509", "654923319", "GB654951509",
				"915970991", "GB654430839", "GB654430839", "GB654430839"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "IDENTITY.VAT_GB");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "(GB)?[ \\d]{9}");
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void VAT_PL() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("NIP");
		analysis.setLocale(Locale.forLanguageTag("pl-PL"));

		final String[] inputs = {
				"5250008318", "5260250995", "5861014302", "6430000299", "6310200736",
				"6351011280", "6370102776", "6442211079", "5261040567", "9441251003",
				"6431720328", "6342840172", "6442995827", "6291201739", "5272647089",
				"9570968370", "6340014168", "5221005607", "6340253861", "6340253861",
				"6340125382", "6272473827", "5272706082", "9542732017", "5250007313",
				"5210088682", "6110202860", "5420304637", "6262098291", "5272647089",
				"5861014302", "6340253861", "6310200736", "6340014168", "5221005607",
				"1132779593", "9491924705", "9570968370", "5422737719", "6370102776",
				"5422758029", "5250008318", "5470049288", "7773206954", "7773156972",
				"5272521678", "7272746817", "5210527710", "9542388146", "9691297176",
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "IDENTITY.VAT_PL");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{10}");
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void VAT_IT() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("IVA");
		analysis.setLocale(Locale.forLanguageTag("it-IT"));

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

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "IDENTITY.VAT_IT");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{11}");
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()), input);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void VAT_ES() throws IOException, FTAException {
		final CsvParserSettings settings = new CsvParserSettings();
		settings.setHeaderExtractionEnabled(true);
		String[] header;
		String[] row;
		TextAnalyzer[] analysis;

		try (BufferedReader in = new BufferedReader(new InputStreamReader(TestPlugins.class.getResourceAsStream("/VAT_ES.csv"), StandardCharsets.UTF_8))) {

			final CsvParser parser = new CsvParser(settings);
			parser.beginParsing(in);

			header = parser.getRecordMetadata().headers();
			analysis = new TextAnalyzer[header.length];
			for (int i = 0; i < header.length; i++) {
				analysis[i] = new TextAnalyzer(new AnalyzerContext(header[i], DateResolutionMode.Auto, "VAT_ES.csv", header));
				analysis[i].setLocale(Locale.forLanguageTag("es-ES"));
			}
			while ((row = parser.parseNext()) != null) {
				for (int i = 0; i < row.length; i++) {
					analysis[i].train(row[i]);
				}
			}
		}

		final TextAnalysisResult result = analysis[0].getResult();
		assertEquals(result.getTypeQualifier(), "IDENTITY.VAT_ES");
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getSampleCount(), 1194);
		assertEquals(result.getMatchCount(), 1193);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getInvalidCount(), 1);
		Entry<String, Long> only = result.getInvalidDetails().entrySet().iterator().next();
		assertEquals(only.getKey(), "X02469358");
		assertEquals(only.getValue(), 1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicRace() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicRace");
		final String[] inputs = {
				"UNK", "BLACK", "WHITE", "EAST INDIAN", "METIS", "BLACK",
				"NORTH AMERICAN", "CHINESE", "FILIPINO", "LATIN AMERICAN", "S. E. ASIAN",
				"ASIAN-SOUTH", "MULTIRAC/ETHNIC", "BLACK", "WHITE", "NORTH AMERICAN",
				"SOUTH ASIAN", "S. E. ASIAN", "WHITE", "METIS", "NORTH AMERICAN",
				"S. E. ASIAN", "WHITE", "BLACK", "WHITE", "LATIN AMERICAN",
				"UNK", "BLACK", "WHITE", "EAST INDIAN", "METIS",
				"NORTH AMERICAN", "OTHER", "CHINESE", "LATIN AMERICAN", "S. E. ASIAN",
				"ASI-E/SOUTHEAST", "ASIAN-SOUTH", "EURO.-SOUTHERN", "MULTIRAC/ETHNIC"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "PERSON.RACE_EN");
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("PERSON.RACE_EN").signature);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getOutlierCount(), 0);

		for (final String input : inputs)
			assertTrue(input.trim().matches(result.getRegExp()), input);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicPostalCodeNL() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("P_PCODE");
		analysis.setLocale(Locale.forLanguageTag("nl-NL"));

		final String pipedInput = "2345AQ|5993FG|3898WW|5543NH|1992WW|4002CS|5982KG|1090DD|3030XX|2547DE|6587DS|3215QQ|7745VD|4562DD|4582SS|2257WE|3578HT|4568FB|4573LF|3574SS|8122GK|4523EW|7128RT|2548RF|6873HH|4837NR|2358EE|3731HY|1587SW|1088TR|";		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "POSTAL_CODE.POSTAL_CODE_NL");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{4}\\p{IsAlphabetic}{2}");
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicPhoneNumber() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Phone");
		final String[] inputs = {
				"+1 339 223 3709", "(650) 867-3450", "+44 191 4956203", "(650) 450-8810", "(512) 757-6000", "(336) 222-7000", "(014) 427-4427",
				"(785) 241-6200", "(312) 596-1000", "(503) 421-7800", "(520) 773-9050", "+1 617 875 9183", "(212) 842-5500", "(415) 901-7000",
				"+1 781 820 1290", "508.822.8383", "617-426-1400", "+1 781-219-3635"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "TELEPHONE");
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("TELEPHONE").signature);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), PhoneNumberLT.REGEXP);
		assertEquals(result.getOutlierCount(), 0);
		final Map<String, Long> invalids = result.getInvalidDetails();
		final long invalidCount = invalids.get("(014) 427-4427");
		assertEquals(invalidCount, 1);
		assertEquals(result.getMatchCount(), inputs.length - result.getInvalidCount());
		// Confidence is 1.0 because we got a boost from seeing the valid header - i.e. 'phone'
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs)
			assertTrue(input.trim().matches(result.getRegExp()), input);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicPhoneNumberUnrecognizedHeader() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("BTN");
		final String[] inputs = {
				"+1 339 223 3709", "(650) 867-3450", "+44 191 4956203", "(650) 450-8810", "(512) 757-6000", "(336) 222-7000", "(014) 427-4427",
				"(785) 241-6200", "(312) 596-1000", "(503) 421-7800", "(520) 773-9050", "+1 617 875 9183", "(212) 842-5500", "(415) 901-7000",
				"+1 781 820 1290", "508.822.8383", "617-426-1400", "+1 781-219-3635", "339.201.9591", "1-800-873-4779",
				"5102687777", "5108232370", "8446422227", "7192572713", "7192572713", "9259541178", "5106583226", "5105255950",  "9252441222"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "TELEPHONE");
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("TELEPHONE").signature);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), PhoneNumberLT.REGEXP);
		assertEquals(result.getOutlierCount(), 0);
		final Map<String, Long> invalids = result.getInvalidDetails();
		final long outlierCount = invalids.get("(014) 427-4427");
		assertEquals(outlierCount, 1);
		assertEquals(result.getMatchCount(), inputs.length - result.getInvalidCount());
		assertEquals(result.getConfidence(), 1.0 - (double)1/result.getSampleCount());

		for (final String input : inputs) {
			assertTrue(input.trim().matches(result.getRegExp()), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void trickyPhoneNumber() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Phone");
		final String[] inputs = {
				"617.875.9182", "7818609182", "+13392237279", "+13392237280", "7818201295", "617.875.9183",
				"7818609182", "+13392237271", "+13392237281", "7818201295", "617.875.9184", "7818609182",
				"+13392237272", "+13392237283", "7818201295", "617.875.9185", "7818609182", "+13392237278",
				"+13392237289", "7818201295", "617.875.9188", "7818609182", "+13392237277", "+13392237287",
				"7818201295", "617.875.9189", "7818609182", "+13392237279", "+13392237280", "7818201295",
				"617.875.9182", "7818609182", "+13392237279", "+13392237280", "7818201295"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "TELEPHONE");
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("TELEPHONE").signature);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), PhoneNumberLT.REGEXP);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.trim().matches(result.getRegExp()), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicGenderWithSpaces() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Gender");
		final String pipedInput = " Female| MALE|Male| Female|Female|MALE |Female |Female |Unknown |Male |" +
				" Male|Female |Male|Male|Male|Female | Female|Male |Male |Male |" +
				" Female|Male |Female|FEMALE|Male| Female| male| Male| Male|  male |";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), Gender.SEMANTIC_TYPE + "EN");
		assertEquals(result.getStructureSignature(), analysis.getPlugins().getRegistered(Gender.SEMANTIC_TYPE + "EN").getSignature());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "(?i)(FEMALE|MALE|UNKNOWN)");
		assertEquals(result.getMatchCount(), inputs.length - 1);
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());

		final LogicalType logicalGender = analysis.getPlugins().getRegistered(Gender.SEMANTIC_TYPE + "EN");
		for (final String input : inputs) {
			assertTrue(input.trim().matches(result.getRegExp()), input);
			final boolean expected = "male".equalsIgnoreCase(input.trim()) || "female".equalsIgnoreCase(input.trim());
			assertEquals(logicalGender.isValid(input, true), expected);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicGenderTriValue() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Gender");
		final String pipedInput = "Female|MALE|Male|Female|Female|MALE|Female|Female|Unknown|Male|" +
				"Male|Female|Male|Male|Male|Female|Female|Male|Male|Male|" +
				"Unknown|Female|Unknown|Male|Unknown|Female|Unknown|Male|Unknown|Male|" +
				"Female|Male|Female|FEMALE|Male|Female|male|Male|Male|male|";
		final int UNKNOWN_COUNT = 6;
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();


		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), Gender.SEMANTIC_TYPE + "EN");
		assertEquals(result.getStructureSignature(), analysis.getPlugins().getRegistered(Gender.SEMANTIC_TYPE + "EN").getSignature());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "(?i)(FEMALE|MALE|UNKNOWN)");
		assertEquals(result.getMatchCount(), inputs.length - UNKNOWN_COUNT);
		assertEquals(result.getConfidence(), 1 - (double)UNKNOWN_COUNT/result.getSampleCount());

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicGenderNoDefaults() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Gender");
		analysis.configure(TextAnalyzer.Feature.DEFAULT_LOGICAL_TYPES, false);

		final String pipedInput = "Female|MALE|Male|Female|Female|MALE|Female|Female|Unknown|Male|" +
				"Male|Female|Male|Male|Male|Female|Female|Male|Male|Male|" +
				"Female|Male|Female|FEMALE|Male|Female|male|Male|Male|male|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeQualifier());
		assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "(?i)(FEMALE|MALE|UNKNOWN)");
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	private final String[] validCUSIPs = {
			"000307108", "000307908", "000307958", "000360206", "000360906", "000360956", "000361105", "000361905", "000361955", "000375204", "000375904",
			"000375954", "00081T108", "00081T908", "00081T958", "000868109", "000899104", "00090Q103", "00090Q903", "00090Q953", "000957100", "000957900",
			"000957950", "001084902", "020002101", "020002901", "020002951", "03842B901", "095229100", "171484908", "238661902", "260003108", "260003908",
			"260003958", "29275Y952", "34959E959", "38000Q102", "38000Q902", "38000Q952", "42226A907", "46138E677", "47023A309", "47023A909", "47023A959",
			"470299108", "47030M106", "47102XAH8", "47103U100", "47103U209", "47103U407", "47103U506", "564563104", "659310906", "67000B104", "67000B904",
			"67000B954", "670002AB0", "670002104", "670002904", "670002954", "670008AD3", "684000102", "684000902", "684000952", "72201R403", "74640Y114",
			"800013104", "800013904", "800013954", "80004CAF8", "80007A102", "80007A902", "80007A952", "80007P869", "80007P909", "80007P959", "80007T101",
			"000957950", "001084902", "020002101", "020002901", "020002951", "03842B901", "095229100", "171484908", "238661902", "260003108", "260003908",
			"260003958", "29275Y952", "34959E959", "38000Q102", "38000Q902", "38000Q952", "42226A907", "46138E677", "47023A309", "47023A909", "47023A959",
			"470299108", "47030M106", "47102XAH8", "47103U100", "47103U209", "47103U407", "47103U506", "564563104", "659310906", "67000B104", "67000B904",
			"67000B954", "670002AB0", "670002104", "670002904", "670002954", "670008AD3", "684000102", "684000902", "684000952", "72201R403", "74640Y114",
			"80007T901", "80007T951", "80007V106", "80007V906", "80007V956", "80283M901", "87236Y908", "91705J204", "97717W904"

	};

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testRegisterFinite() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("CUSIP");
		analysis.setMaxCardinality(20000);
		final List<PluginDefinition> plugins = new ArrayList<>();
		final PluginDefinition plugin = new PluginDefinition("CUSIP", "com.cobber.fta.PluginCUSIP");
		final String CUSIP_REGEXP = "[\\p{IsAlphabetic}\\p{IsDigit}]{9}";
		plugin.validLocales = new PluginLocaleEntry[] { new PluginLocaleEntry("en", ".*(?i)(cusip).*", 90, CUSIP_REGEXP) };
		plugins.add(plugin);

		try {
			analysis.getPlugins().registerPluginList(plugins, "C U S I P", analysis.getConfig());
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		for (final String sample : validCUSIPs)
			analysis.train(sample);

		analysis.train("666666666");

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), CUSIP_REGEXP);
		assertEquals(result.getTypeQualifier(), "CUSIP");
		assertEquals(result.getSampleCount(), validCUSIPs.length + 1);
		assertEquals(result.getMatchCount(), validCUSIPs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getMinLength(), 9);
		assertEquals(result.getMaxLength(), 9);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());

		int matchCount = 1;
		for (final String validCUSIP : validCUSIPs) {
			if (validCUSIP.matches(result.getRegExp()))
				matchCount++;
		}
		assertEquals(matchCount, result.getMatchCount() + 1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testRegisterInfinite() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("CC");
		analysis.configure(TextAnalyzer.Feature.DEFAULT_LOGICAL_TYPES, false);
		final List<PluginDefinition> plugins = new ArrayList<>();
		final PluginDefinition pluginDefinition = new PluginDefinition("CC", "com.cobber.fta.PluginCreditCard");
		pluginDefinition.validLocales = new PluginLocaleEntry[] { new PluginLocaleEntry("en") };
		plugins.add(pluginDefinition);

		try {
			analysis.getPlugins().registerPluginList(plugins, "Ignore", analysis.getConfig());
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		final String[] inputs = {
//				"Credit Card Type,Credit Card Number",
				"American Express,378282246310005",
				"American Express,371449635398431",
				"American Express Corporate,378734493671000 ",
				"Australian BankCard,5610591081018250",
				"Diners Club,30569309025904",
				"Diners Club,38520000023237",
				"Discover,6011111111111117",
				"Discover,6011000990139424",
				"JCB,3530111333300000",
				"JCB,3566002020360505",
				"MasterCard,5555555555554444",
				"MasterCard,5105105105105100",
				"MasterCard (2 series),2223003122003222",
				"MasterCard (debit),5200828282828210",
				"MasterCard (prepaid),5105105105105100",
				"Visa,4111111111111111",
				"Visa,4012888888881881",
				"Visa,4222222222222",
				"Visa,4242424242424242",
				"Visa (debit),4000056655665556",
				"Dankort (PBS),5019717010103742",
				"Switch/Solo (Paymentech),6331101999990016"
		};

		final Set<String>  samples = new HashSet<>();

		for (final String input : inputs) {
			final String s = input.split(",")[1];
			samples.add(s);
			analysis.train(s);
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTypeQualifier(), "CREDITCARD");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getMinLength(), 13);
		assertEquals(result.getMaxLength(), 16);
		assertEquals(result.getBlankCount(), 0);
		assertTrue(result.isLogicalType());
		assertEquals(result.getRegExp(), PluginCreditCard.REGEXP);
		assertEquals(result.getConfidence(), 1.0);

		for (final String s : samples) {
			assertTrue(s.matches(result.getRegExp()), s);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicGUID() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("GUID");
		final String[] inputs = {
				"DAA3EDDE-5BCF-4D2A-8FB0-E120089343AF",
				"B0613BE8-88AF-4591-A9A0-059F80413212",
				"063BB913-7287-4A8A-B3DF-41EAA0EABF49",
				"B6011DC1-C4A3-4130-AD42-C3EA2BA35F8B",
				"327B2624-2467-4461-8CA3-2DCB30D06683",
				"BDC94786-4016-4C7A-85F7-A7558425FA26",
				"0525CA73-9A48-497A-AC2D-2596BFE66FF7",
				"88BD42BA-B4F2-4E9E-8BD3-6846F6692E44",
				"1456E784-D404-4864-BBD3-691988220732",
				"FF2B0C44-2277-4EB1-BB25-32CF23181672",
				"929945CC-E4AA-4FEA-BFD6-43B774C9FB05",
				"BC2D3965-24A5-4CC7-986A-99B869925ACD",
				"7C9C9A6C-0A38-41B6-A999-A9A4218D43FA",
				"3324F2BF-9CC6-446A-A02D-DDE2F2ECF31F",
				"F17AA339-5DCE-4318-9B1C-C95255D4C5CC",
				"D67F9D81-DBE7-4214-849F-41B937C628AB",
				"9892D51B-C490-4B6E-8DF0-B032BAAB0476",
				"6CBD3302-F067-4378-8955-CD57EA5E83EB",
				"BEDFFAF8-9E35-4155-A337-7981BA349E7B",
				"37285247-D431-4381-AC5F-7C3136E276C2",
				"6D490537-AA7B-45C5-BEDB-8572EBDEFD15",
				"51e55fd6-74ca-4b1d-b5fd-d210209e3fc4"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "GUID");
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("GUID").signature);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getRegExp(), GUID.REGEXP);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	private final String validEmails = "Bachmann@lavastorm.com|Biedermann@lavastorm.com|buchheim@lavastorm.com|" +
			"coleman@lavastorm.com|Drici@lavastorm.com|Garvey@lavastorm.com|jackson@lavastorm.com|" +
			"Jones@lavastorm.com|Marinelli@lavastorm.com|Nason@lavastorm.com|Parker@lavastorm.com|" +
			"Pigneri@lavastorm.com|Rasmussen@lavastorm.com|Regan@lavastorm.com|Segall@Lavastorm.com|" +
			"Pigneri2@lavastorm.com|ahern@lavastorm.com|reginald@lavastorm.com|blumfontaine@Lavastorm.com|" +
			"Smith@lavastorm.com|Song@lavastorm.com|Tolleson@lavastorm.com|wynn@lavastorm.com|" +
			"Ahmed@lavastorm.com|Benoit@lavastorm.com|Keane@lavastorm.com|Kilker@lavastorm.com|" +
			"Waters@lavastorm.com|Meagher@lavastorm.com|Mok@lavastorm.com|Mullin@lavastorm.com|" +
			"Nason@lavastorm.com|reilly@lavastorm.com|Scoble@lavastorm.com|Comerford@lavastorm.com|" +
			"Gallagher@lavastorm.com|Hughes@lavastorm.com|Kelly@lavastorm.com|" +
			"Tuddenham@lavastorm.com|Williams@lavastorm.com|Wilson@lavastorm.com|";

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicEmail() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicE_mail");
		analysis.setTrace("samples=10");
		final String inputs[] = validEmails.split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train("tim@cobber com");
		analysis.train("tim@cobber com");
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), inputs.length + 2 + result.getNullCount());
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "EMAIL");
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("EMAIL").signature);
		assertEquals(result.getInvalidCount(), 1);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 2);
		assertEquals(result.getRegExp(), EmailLT.REGEXP);
		assertEquals(result.getConfidence(), 1 - (double)2/(result.getSampleCount() - result.getNullCount()));

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void degenerativeEmail() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("degenerativeE_mail");
		final String pipedInput = validEmails + validEmails + validEmails + validEmails + "ask|not|what|your|country|can|";
		final String inputs[] = pipedInput.split("\\|");
		final int ERRORS = 6;
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), inputs.length + result.getNullCount());
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "EMAIL");
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("EMAIL").signature);
		assertEquals(result.getMatchCount(), inputs.length - ERRORS);
		assertEquals(result.getNullCount(), 2);
		assertEquals(result.getRegExp(), EmailLT.REGEXP);
		assertEquals(result.getConfidence(), 1 - (double)ERRORS/(result.getSampleCount() - result.getNullCount()));

		int matches = 0;
		for (final String input : inputs)
			if (input.matches(result.getRegExp()))
				matches++;

		assertEquals(matches, result.getMatchCount());
	}

	private final static String INPUT_URLS = "http://www.lavastorm.com|ftp://ftp.sun.com|https://www.google.com|" +
			"https://www.homedepot.com|http://www.lowes.com|http://www.apple.com|http://www.sgi.com|" +
			"http://www.ibm.com|http://www.snowgum.com|http://www.zaius.com|http://www.cobber.com|" +
			"http://www.ey.com|http://www.zoomer.com|http://www.redshift.com|http://www.segall.net|" +
			"http://www.sgi.com|http://www.united.com|https://www.hp.com/printers/support|http://www.opinist.com|" +
			"http://www.java.com|http://www.slashdot.org|http://theregister.co.uk|";

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicURL() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicU_RL");
		final String inputs[] = INPUT_URLS.split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);
		analysis.train("bogus");

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), inputs.length + 1 + result.getNullCount());
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getInvalidCount(), 1);
		assertEquals(result.getMatchCount(), inputs.length + 1 - result.getInvalidCount());
		assertEquals(result.getNullCount(), 2);
		assertEquals(result.getRegExp(), URLLT.REGEXP_PROTOCOL + URLLT.REGEXP_RESOURCE);
		assertEquals(result.getConfidence(), 1 - (double)1/(result.getSampleCount() - result.getNullCount()));
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), URLLT.SEMANTIC_TYPE);
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("URI.URL").signature);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicURLResource() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicU_RLResource");
		final String inputs[] = INPUT_URLS.split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i].substring(inputs[i].indexOf("://") + 3)) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), inputs.length + result.getNullCount());
		assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		assertEquals(result.getNullCount(), 2);
		assertEquals(result.getRegExp(), URLLT.REGEXP_RESOURCE);
		assertEquals(result.getConfidence(), 0.95);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), URLLT.SEMANTIC_TYPE);
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("URI.URL").signature);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicURLMixed() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicURLMixed");
		final String inputs[] = INPUT_URLS.split("\\|");

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (Math.abs(i % 2) == 1)
				analysis.train(inputs[i]);
			else analysis.train(inputs[i].substring(inputs[i].indexOf("://") + 3));
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length + result.getNullCount());
		assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		assertEquals(result.getNullCount(), 2);
		assertEquals(result.getRegExp(), URLLT.REGEXP_PROTOCOL + "?" + URLLT.REGEXP_RESOURCE);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), URLLT.SEMANTIC_TYPE);
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("URI.URL").signature);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void backoutURL() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("backoutURL");
		final String inputs[] = INPUT_URLS.split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final int badURLs = 50;
		for (int i = 0; i < badURLs; i++)
			analysis.train(String.valueOf(i));

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeQualifier());
		assertEquals(result.getSampleCount(), inputs.length + badURLs + result.getNullCount());
		assertEquals(result.getNullCount(), 2);
		assertEquals(result.getOutlierCount(), 0);
//		assertEquals(result.getMatchCount(), inputs.length + badURLs + result.getNullCount());
		assertEquals(result.getRegExp(), KnownPatterns.freezeANY(1, 35, 1, 35, result.getLeadingWhiteSpace(), result.getTrailingWhiteSpace(), result.getMultiline()));
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void notEmail() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("notEmail");
		final String pipedInput = "2@3|3@4|b4@5|" +
				"6@7|7@9|12@13|100@2|" +
				"Zoom@4|Marinelli@44|55@90341|Parker@46|" +
				"Pigneri@22|Rasmussen@77|478 @ 1912|88 @ LC|" +
				"Smith@99|Song@88|77@|@lavastorm.com|" +
				"Tuddenham@02421|Williams@uk|Wilson@99";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train("tim@cobber com");
		analysis.train("tim@cobber com");
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), inputs.length + 2 + result.getNullCount());
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getRegExp(), KnownPatterns.freezeANY(3, 15, 3, 15, result.getLeadingWhiteSpace(), result.getTrailingWhiteSpace(), result.getMultiline()));
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeQualifier());
		assertEquals(result.getMatchCount(), inputs.length + 2);
		assertEquals(result.getNullCount(), 2);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicZip() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicZ_ip");
		final String inputs[] = TestUtils.validZips.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTypeQualifier(), USZip5.SEMANTIC_TYPE);
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("POSTAL_CODE.ZIP5_US").signature);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 32);
		assertEquals(result.getRegExp(), USZip5.REGEXP_ZIP5);
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicZipVariable() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicZ_ipVariable");
		final String inputs[] = TestUtils.validZips.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			final String input = inputs[i].charAt(0) == '0' ? inputs[i].substring(1) : inputs[i];
			if (analysis.train(input) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTypeQualifier(), USZip5.SEMANTIC_TYPE);
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("POSTAL_CODE.ZIP5_US").signature);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getRegExp(), USZip5.REGEXP_VARIABLE);
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void random3166_2() throws IOException, FTAException {
		final TextAnalyzer analyzer = new TextAnalyzer("country");
		analyzer.registerDefaultPlugins(analyzer.getConfig());
		final LogicalTypeCode logical = (LogicalTypeCode)analyzer.getPlugins().getRegistered("COUNTRY.ISO-3166-2");

		assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void random3166_2_noHeader() throws IOException, FTAException {
		final TextAnalyzer analyzer = new TextAnalyzer("random3166_2_noHeader");
		analyzer.registerDefaultPlugins(analyzer.getConfig());
		final LogicalTypeCode logical = (LogicalTypeCode)analyzer.getPlugins().getRegistered("COUNTRY.ISO-3166-2");

		assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void random3166_3() throws IOException, FTAException {
		final TextAnalyzer analyzer = new TextAnalyzer("country");
		analyzer.registerDefaultPlugins(analyzer.getConfig());
		final LogicalTypeCode logical = (LogicalTypeCode)analyzer.getPlugins().getRegistered("COUNTRY.ISO-3166-3");

		assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void random4217() throws IOException, FTAException {
		final TextAnalyzer analyzer = new TextAnalyzer("currency");
		analyzer.registerDefaultPlugins(analyzer.getConfig());
		final LogicalTypeCode logical = (LogicalTypeCode)analyzer.getPlugins().getRegistered("CURRENCY_CODE.ISO-4217");

		assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void randomIATA() throws IOException, FTAException {
		final TextAnalyzer analyzer = new TextAnalyzer("IATA");
		analyzer.registerDefaultPlugins(analyzer.getConfig());
		final LogicalTypeCode logical = (LogicalTypeCode)analyzer.getPlugins().getRegistered("AIRPORT_CODE.IATA");

		assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++) {
			assertTrue(logical.nextRandom().matches(logical.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testRegister() throws IOException, FTAException {
		final TextAnalyzer analyzer = new TextAnalyzer("testRegister");
		analyzer.registerDefaultPlugins(analyzer.getConfig());

		LogicalType logical = analyzer.getPlugins().getRegistered(URLLT.SEMANTIC_TYPE);

		final String valid = "http://www.infogix.com";
		final String invalid = "www infogix.com";

		assertTrue(logical.isValid(valid, true));
		assertFalse(logical.isValid(invalid, true));

		logical = analyzer.getPlugins().getRegistered(CountryEN.SEMANTIC_TYPE);

		final String ChinaUpper = "CHINA";
		assertTrue(logical.isValid(ChinaUpper, true));

		final String ChinaWithSpaces = "  CHINA  ";
		assertTrue(logical.isValid(ChinaWithSpaces, true));

		final String ChinaCamel = "China";
		assertTrue(logical.isValid(ChinaCamel, true));

		final String Lemuria = "Lemuria";
		assertFalse(logical.isValid(Lemuria, true));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicZipHeader() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("BillingPostalCode");
		final String inputs[] = {
			"", "", "", "", "", "", "", "", "", "27215", "75251", "66045", "", "",
			"", "", "", "", "94087", "", "", "", "", "", "", "", "", "", "", ""
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTypeQualifier(), USZip5.SEMANTIC_TYPE);
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("POSTAL_CODE.ZIP5_US").signature);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), 4);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getRegExp(), USZip5.REGEXP_ZIP5);
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicLuhn() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("IMEI");
		final String inputs[] = {
				"518328079297586", "494238109049246", "497201528898871", "916790719773953", "991640416096547", "517179040180885", "496180503928286",
				"512689883394604", "496164591404293", "545307005094090", "359268076686757", "451386551229690", "010396357738673", "541276254953906",
				"447222295078647", "357859215957307", "867490245465674", "537397059073660", "301312941350311", "861927543064002", "446773324240112",
				"444379739779116", "986670865359218", "306552935575523", "918077793540088", "523155687421636"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTypeQualifier(), CheckDigitLuhn.SEMANTIC_TYPE);
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("CHECKDIGIT.LUHN").signature);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 1);
		assertEquals(result.getRegExp(), "\\d{15}");
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicCUSIP() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("CUSIP");
		final String inputs[] = {
				"B38564108", "B38564900", "B38564959", "C15396AB7", "D18190898", "D18190906", "D18190955", "F21107101", "F21107903", "F21107952",
				"G0083D112", "G00748122", "G0083D104", "G0083D112", "G0083D120", "G0084W101", "G0084W903", "G0084W952", "G01125106", "G01125908",
				"G01125957", "G0120M109", "G0120M117", "G0120M125", "G0120M133", "G0132V105", "G0176J109", "G01767105", "G0232J101", "G0232J119", "G0232J127"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), CheckDigitCUSIP.SEMANTIC_TYPE);
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("CHECKDIGIT.CUSIP").signature);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getRegExp(), "[\\p{IsAlphabetic}\\d]{9}");
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicSEDOL() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("SEDOL");
		final String inputs[] = {
				"0078416", "B63H849", "BJVNSS4", "B5M6XQ7", "B082RF1", "B0SWJX3", "3319521", "BLDYK61", "BD6K457", "B19NLV4", "B1XZS82",
				"B1KJJ40", "3174300", "0673123", "BHJYC05", "BH4HKS3", "0263494", "3091357", "B7T7721", "0870612", "B03MM40",
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), CheckDigitSEDOL.SEMANTIC_TYPE);
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("CHECKDIGIT.SEDOL").signature);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getRegExp(), CheckDigitSEDOL.REGEXP);
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicUPC() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("UPC");
		final String inputs[] = {
				"9780444505156", "4605664000050", "3014260115531", "8020187300016", "8076809513456", "3155250001387",
				"2151191106847", "1626093139220", "8556467100101", "0922077722381", "3064298186966", "1068035884902",
				"4709099997098", "2460125680880", "9686595482097", "2455962755150", "1883097580551", "9664864959587",
				"4632812983156", "8715988259303", "4114932292979", "1635056616685", "1850775082089", "4514120918771"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTypeQualifier(), CheckDigitEAN13.SEMANTIC_TYPE);
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("CHECKDIGIT.EAN13").signature);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 1);
		assertEquals(result.getRegExp(), CheckDigitEAN13.REGEXP);
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicISIN() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("ISIN");
		final String inputs[] = {
				"GB0000784164", "GB00B63H8491", "JE00BJVNSS43", "ES0177542018", "GB00B082RF11", "GB00B0SWJX34", "GB0033195214",
				"GB00BLDYK618", "GB00BD6K4575", "GB00B19NLV48", "GB00B1XZS820", "GB00B1KJJ408", "GB0031743007", "GB0006731235",
				"GB00BHJYC057", "GB00BH4HKS39", "GB0002634946", "GB0030913577", "GB00B7T77214", "GB0008706128", "GB00B03MM408"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), CheckDigitISIN.SEMANTIC_TYPE);
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("CHECKDIGIT.ISIN").signature);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getRegExp(), CheckDigitISIN.REGEXP);
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
 	public void basicISBN() throws IOException, FTAException {
 		final TextAnalyzer analysis = new TextAnalyzer("ISBN");
 		final String inputs[] = {
 				"978-1-83790-353-5", "978-1-921048-91-3", "978-0-315-09943-2", "978-0-535-98831-8", "978-0-451-05990-1", "978-1-58120-222-9",
 				"978-1-64072-519-5", "978-1-218-87051-7", "978-1-05-073878-5", "978-0-06-877239-2", "978-0-7528-6694-9", "978-1-247-67895-5",
 				"978-0-348-29489-7", "978-0-11-949459-4", "978-1-80795-326-3", "978-0-355-05307-4", "978-1-249-01060-9", "978-1-74928-734-1",
 				"978-1-351-75568-9", "978-1-68506-159-3", "978-1-260-50436-1", "978-0-680-48813-8", "978-0-948544-89-7", "978-0-7703-9196-6",
 				"978-0-9993630-2-7", "978-1-203-00498-9", "978-1-72090-440-3", "978-0-10-695287-8", "978-1-56707-945-6", "978-1-376-32192-0"
 		};

 		for (final String input : inputs)
 			analysis.train(input);

 		final TextAnalysisResult result = analysis.getResult();

 		assertEquals(result.getType(), FTAType.STRING);
 		assertEquals(result.getTypeQualifier(), CheckDigitISBN.SEMANTIC_TYPE);
 		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("CHECKDIGIT.ISBN").signature);
 		assertEquals(result.getSampleCount(), inputs.length);
 		assertEquals(result.getOutlierCount(), 0);
 		assertEquals(result.getMatchCount(), inputs.length);
 		assertEquals(result.getNullCount(), 0);
 		assertEquals(result.getLeadingZeroCount(), 0);
 		assertEquals(result.getRegExp(), CheckDigitISBN.REGEXP);
		assertEquals(result.getConfidence(), 1.0);
	}


	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicIPAddress() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("BillingPostalCode");
		final String inputs[] = {
			"8.8.8.8", "4.4.4.4", "1.1.1.1", "172.217.4.196", "192.168.86.1", "64.68.200.46", "23.45.133.21",
			"15.73.4.77"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), IPV4Address.SEMANTIC_TYPE);
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("IPADDRESS.IPV4").signature);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getRegExp(), IPV4Address.REGEXP);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicZipHeaderDE() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Postleitzahl");
		analysis.setLocale(Locale.forLanguageTag("de-AT"));
		final String inputs[] = {
			"", "", "", "", "", "", "", "", "", "27215", "75251", "66045", "", "",
			"", "", "", "", "94087", "", "", "", "", "", "", "", "", "", "", ""
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.LONG);
//		assertEquals(result.getTypeQualifier(), "POSTAL_CODE.POSTAL_CODE_DE");
		assertNull(result.getTypeQualifier());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), 4);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getRegExp(), "\\d{5}");
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void zipUnwind() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("zipUnwind");
		final String pipedInput = "02421|02420|02421|02420|02421|02420|02421|02420|02421|02420|" +
				"02421|02420|02421|02420|02421|02420|02421|02420|02421|02420|" +
				"10248|10249|10250|10251|10252|10253|10254|10255|10256|10257|10258|10259|10260|10261|10262|10263|10264|" +
				"bogus|" +
						"10265|10266|10267|10268|10269|10270|10271|10272|10273|10274|10275|10276|10277|10278|10279|10280|10281|" +
						"10282|10283|10284|10285|10286|10287|10288|10289|10290|10291|10292|10293|10294|10295|10296|10297|10298|" +
						"10299|10300|10301|10302|10303|10304|10305|10306|10307|10308|10309|10310|10311|10312|10313|10314|10315|" +
						"10316|10317|10318|10319|10320|10321|10322|10323|10324|10325|10326|10327|10328|10329|10330|10331|10332|" +
						"10333|10334|10335|10336|10337|10338|10339|10340|10341|10342|10343|10344|10345|10346|10347|10348|10349|" +
						"10350|10351|10352|10353|10354|10355|10356|10357|10358|10359|10360|10361|10362|10363|10364|10365|10366|" +
						"10367|10368|10369|10370|10371|10372|10373|10374|10375|10376|10377|10378|10379|10380|10381|10382|10383|" +
						"10384|10385|10386|10387|10388|10389|10390|10391|10392|10393|10394|10395|10396|10397|10398|10399|10400|" +
						"10401|10402|10403|10404|10405|10406|10407|10408|10409|10410|10411|10412|10413|10414|10415|10416|10417|" +
						"10418|10419|10420|10421|10422|10423|10424|10425|10426|10427|10428|10429|10430|10431|10432|10433|10434|" +
						"10435|10436|10437|10438|10439|10440|10441|10442|10443|10444|10445|10446|10447|10448|10449|10450|10451|" +
						"10452|10453|10454|10455|10456|10457|10458|10459|10460|10461|10462|10463|10464|10465|10466|10467|10468|" +
						"10469|10470|10471|10472|10473|10474|10475|10476|10477|10478|10479|10480|10481|10482|10483|10484|10485|" +
						"10486|10487|10488|10489|10490|10491|10492|10493|10494|10495|10496|10497|10498|10499|10500|10501|10502|" +
						"10503|10504|10505|10506|10507|10508|10509|10510|10511|10512|10513|10514|10515|10516|10517|10518|10519|" +
						"10520|10521|10522|10523|10524|10525|10526|10527|10528|10529|10530|10531|10532|10533|10534|10535|10536|" +
						"10537|10538|10539|10540|10541|10542|10543|10544|10545|10546|10547|10548|10549|10550|10551|10552|10553|" +
						"10554|10555|10556|10557|10558|10559|10560|10561|10562|10563|10564|10565|10566|10567|10568|10569|10570|" +
						"10571|10572|10573|10574|10575|10576|10577|10578|10579|10580|10581|10582|10583|10584|10585|10586|10587|" +
						"10588|10589|10590|10591|10592|10593|10594|10595|10596|10597|10598|10599|10600|10601|10602|10603|10604|" +
						"10605|10606|10607|10608|10609|10610|10611|10612|10613|10614|10615|10616|10617|10618|10619|10620|10621|" +
						"10622|10623|10624|10625|10626|10627|10628|10629|10630|10631|10632|10633|10634|10635|10636|10637|10638|" +
						"10639|10640|10641|10642|10643|10644|10645|10646|10647|10648|10649|10650|10651|10652|10653|10654|10655|" +
						"10656|10657|10658|10659|10660|10661|10662|10663|10664|10665|10666|10667|10668|10669|10670|10671|10672|" +
						"10673|10674|10675|10676|10677|10678|10679|10680|10681|10682|10683|10684|10685|10686|10687|10688|10689|" +
						"10690|10691|10692|10693|10694|10695|10696|10697|10698|10699|10700|10701|10702|10703|10704|10705|10706|" +
						"10707|10708|10709|10710|10711|10712|10713|10714|10715|10716|10717|10718|10719|10720|10721|10722|10723|" +
						"10724|10725|10726|10727|10728|10729|10730|10731|10732|10733|10734|10735|10736|10737|10738|10739|10740|" +
						"10741|10742|10743|10744|10745|10746|10747|10748|10749|10750|10751|10752|10753|10754|10755|10756|10757|" +
						"10758|10759|10760|10761|10762|10763|10764|10765|10766|10767|10768|10769|10770|10771|10772|10773|10774|" +
						"10775|10776|10777|10778|10779|10780|10781|10782|10783|10784|10785|10786|10787|10788|10789|10790|10791|" +
						"10792|10793|10794|10795|10796|10797|10798|10799|10800|10801|10802|10803|10804|10805|10806|10807|10808|" +
						"10809|10810|10811|10812|10813|10814|10815|10816|10817|10818|10819|10820|10821|10822|10823|10824|10825|" +
						"10826|10827|10828|10829|10830|10831|10832|10833|10834|10835|10836|10837|10838|10839|10840|10841|10842|" +
						"10843|10844|10845|10846|10847|10848|10849|10850|10851|10852|10853|10854|10855|10856|10857|10858|10859|" +
						"10860|10861|10862|10863|10864|10865|10866|10867|10868|10869|10870|10871|10872|10873|10874|10875|10876|" +
						"10877|10878|10879|10880|10881|10882|10883|10884|10885|10886|10887|10888|10889|10890|10891|10892|10893|" +
						"10894|10895|10896|10897|10898|10899|10900|10901|10902|10903|10904|10905|10906|10907|10908|10909|10910|" +
						"10911|10912|10913|10914|10915|10916|10917|10918|10919|10920|10921|10922|10923|10924|10925|10926|10927|" +
						"10928|10929|10930|10931|10932|10933|10934|10935|10936|10937|10938|10939|10940|10941|10942|10943|10944|" +
						"10945|10946|10947|10948|10949|10950|10951|10952|10953|10954|10955|10956|10957|10958|10959|10960|10961|" +
						"10962|10963|10964|10965|10966|10967|10968|10969|10970|10971|10972|10973|10974|10975|10976|10977|10978|" +
						"10979|10980|10981|10982|10983|10984|10985|10986|10987|10988|10989|10990|10991|10992|10993|10994|10995|" +
						"10996|10997|10998|10999|11000|11001|11002|11003|11004|11005|11006|11007|11008|11009|11010|11011|11012|" +
						"11013|11014|11015|11016|11017|11018|11019|11020|11021|11022|11023|11024|11025|11026|11027|11028|11029|" +
						"11030|11031|11032|11033|11034|11035|11036|11037|11038|11039|11040|11041|11042|11043|11044|11045|11046|" +
						"11047|11048|11049|11050|11051|11052|11053|11054|11055|11056|11057|11058|11059|11060|11061|11062|11063|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeQualifier());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getInvalidCount(), 1);
		assertEquals(result.getMatchCount(), inputs.length - 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 20);
		assertEquals(result.getRegExp(), "\\d{5}");
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());

		int matches = 0;
		for (final String input : inputs) {
			if (input.matches(result.getRegExp()))
					matches++;
		}
		assertEquals(result.getMatchCount(), matches);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void zipNotReal() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("zipNotReal");
		final String pipedInput =
				"10248|10249|10250|10251|10252|10253|10254|10255|10256|10257|10258|10259|10260|10261|10262|10263|10264|" +
						"10265|10266|10267|10268|10269|10270|10271|10272|10273|10274|10275|10276|10277|10278|10279|10280|10281|" +
						"10282|10283|10284|10285|10286|10287|10288|10289|10290|10291|10292|10293|10294|10295|10296|10297|10298|" +
						"10299|10300|10301|10302|10303|10304|10305|10306|10307|10308|10309|10310|10311|10312|10313|10314|10315|" +
						"10316|10317|10318|10319|10320|10321|10322|10323|10324|10325|10326|10327|10328|10329|10330|10331|10332|" +
						"10333|10334|10335|10336|10337|10338|10339|10340|10341|10342|10343|10344|10345|10346|10347|10348|10349|" +
						"10350|10351|10352|10353|10354|10355|10356|10357|10358|10359|10360|10361|10362|10363|10364|10365|10366|" +
						"10367|10368|10369|10370|10371|10372|10373|10374|10375|10376|10377|10378|10379|10380|10381|10382|10383|" +
						"10384|10385|10386|10387|10388|10389|10390|10391|10392|10393|10394|10395|10396|10397|10398|10399|10400|" +
						"10401|10402|10403|10404|10405|10406|10407|10408|10409|10410|10411|10412|10413|10414|10415|10416|10417|" +
						"10418|10419|10420|10421|10422|10423|10424|10425|10426|10427|10428|10429|10430|10431|10432|10433|10434|" +
						"10435|10436|10437|10438|10439|10440|10441|10442|10443|10444|10445|10446|10447|10448|10449|10450|10451|" +
						"10452|10453|10454|10455|10456|10457|10458|10459|10460|10461|10462|10463|10464|10465|10466|10467|10468|" +
						"10469|10470|10471|10472|10473|10474|10475|10476|10477|10478|10479|10480|10481|10482|10483|10484|10485|" +
						"10486|10487|10488|10489|10490|10491|10492|10493|10494|10495|10496|10497|10498|10499|10500|10501|10502|" +
						"10503|10504|10505|10506|10507|10508|10509|10510|10511|10512|10513|10514|10515|10516|10517|10518|10519|" +
						"10520|10521|10522|10523|10524|10525|10526|10527|10528|10529|10530|10531|10532|10533|10534|10535|10536|" +
						"10537|10538|10539|10540|10541|10542|10543|10544|10545|10546|10547|10548|10549|10550|10551|10552|10553|" +
						"10554|10555|10556|10557|10558|10559|10560|10561|10562|10563|10564|10565|10566|10567|10568|10569|10570|" +
						"10571|10572|10573|10574|10575|10576|10577|10578|10579|10580|10581|10582|10583|10584|10585|10586|10587|" +
						"10588|10589|10590|10591|10592|10593|10594|10595|10596|10597|10598|10599|10600|10601|10602|10603|10604|" +
						"10605|10606|10607|10608|10609|10610|10611|10612|10613|10614|10615|10616|10617|10618|10619|10620|10621|" +
						"10622|10623|10624|10625|10626|10627|10628|10629|10630|10631|10632|10633|10634|10635|10636|10637|10638|" +
						"10639|10640|10641|10642|10643|10644|10645|10646|10647|10648|10649|10650|10651|10652|10653|10654|10655|" +
						"10656|10657|10658|10659|10660|10661|10662|10663|10664|10665|10666|10667|10668|10669|10670|10671|10672|" +
						"10673|10674|10675|10676|10677|10678|10679|10680|10681|10682|10683|10684|10685|10686|10687|10688|10689|" +
						"10690|10691|10692|10693|10694|10695|10696|10697|10698|10699|10700|10701|10702|10703|10704|10705|10706|" +
						"10707|10708|10709|10710|10711|10712|10713|10714|10715|10716|10717|10718|10719|10720|10721|10722|10723|" +
						"10724|10725|10726|10727|10728|10729|10730|10731|10732|10733|10734|10735|10736|10737|10738|10739|10740|" +
						"10741|10742|10743|10744|10745|10746|10747|10748|10749|10750|10751|10752|10753|10754|10755|10756|10757|" +
						"10758|10759|10760|10761|10762|10763|10764|10765|10766|10767|10768|10769|10770|10771|10772|10773|10774|" +
						"10775|10776|10777|10778|10779|10780|10781|10782|10783|10784|10785|10786|10787|10788|10789|10790|10791|" +
						"10792|10793|10794|10795|10796|10797|10798|10799|10800|10801|10802|10803|10804|10805|10806|10807|10808|" +
						"10809|10810|10811|10812|10813|10814|10815|10816|10817|10818|10819|10820|10821|10822|10823|10824|10825|" +
						"10826|10827|10828|10829|10830|10831|10832|10833|10834|10835|10836|10837|10838|10839|10840|10841|10842|" +
						"10843|10844|10845|10846|10847|10848|10849|10850|10851|10852|10853|10854|10855|10856|10857|10858|10859|" +
						"10860|10861|10862|10863|10864|10865|10866|10867|10868|10869|10870|10871|10872|10873|10874|10875|10876|" +
						"10877|10878|10879|10880|10881|10882|10883|10884|10885|10886|10887|10888|10889|10890|10891|10892|10893|" +
						"10894|10895|10896|10897|10898|10899|10900|10901|10902|10903|10904|10905|10906|10907|10908|10909|10910|" +
						"10911|10912|10913|10914|10915|10916|10917|10918|10919|10920|10921|10922|10923|10924|10925|10926|10927|" +
						"10928|10929|10930|10931|10932|10933|10934|10935|10936|10937|10938|10939|10940|10941|10942|10943|10944|" +
						"10945|10946|10947|10948|10949|10950|10951|10952|10953|10954|10955|10956|10957|10958|10959|10960|10961|" +
						"10962|10963|10964|10965|10966|10967|10968|10969|10970|10971|10972|10973|10974|10975|10976|10977|10978|" +
						"10979|10980|10981|10982|10983|10984|10985|10986|10987|10988|10989|10990|10991|10992|10993|10994|10995|" +
						"10996|10997|10998|10999|11000|11001|11002|11003|11004|11005|11006|11007|11008|11009|11010|11011|11012|" +
						"11013|11014|11015|11016|11017|11018|11019|11020|11021|11022|11023|11024|11025|11026|11027|11028|11029|" +
						"11030|11031|11032|11033|11034|11035|11036|11037|11038|11039|11040|11041|11042|11043|11044|11045|11046|" +
						"11047|11048|11049|11050|11051|11052|11053|11054|11055|11056|11057|11058|11059|11060|11061|11062|11063|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeQualifier());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		assertEquals(result.getRegExp(), "\\d{5}");
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void sameZip() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("sameZZiiPP");
		final int copies = 100;
		final String sample = "02421";

		int locked = -1;

		for (int i = 0; i < copies; i++) {
			if (analysis.train(sample) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeQualifier());
		assertEquals(result.getSampleCount(), copies);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), copies);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), copies);
		assertEquals(result.getRegExp(), "\\d{5}");
		assertEquals(result.getConfidence(), 1.0);
		assertTrue(sample.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void sameZipWithHeader() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("ZipCode");
		final int copies = 100;
		final String sample = "02421";

		int locked = -1;

		for (int i = 0; i < copies; i++) {
			if (analysis.train(sample) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTypeQualifier(), USZip5.SEMANTIC_TYPE);
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("POSTAL_CODE.ZIP5_US").signature);
		assertEquals(result.getSampleCount(), copies);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), copies);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), copies);
		assertEquals(result.getRegExp(), USZip5.REGEXP_ZIP5);
		assertEquals(result.getConfidence(), 1.0);
		assertTrue(sample.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicStateHeader() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("BillingState");

		final String[] inputs = {
				"NY", "CA", "CA", "", "", "CA", "UK", "TX", "NC", "", "", "", "", "", "MA",
				"", "KS", "IL", "OR", "AZ", "NY", "CA", "CA", "MA", "MI", "ME", "", "", "", "",
				"", "KS", "IL", "OR", "AZ", "NY", "CA", "CA", "MA", "MI", "ME", "", "", "", ""
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "STATE_PROVINCE.STATE_US");
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("STATE_PROVINCE.STATE_US").signature);
		assertEquals(result.getMatchCount(), inputs.length - result.getBlankCount() - result.getInvalidCount());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{2}");
		final Map<String, Long> invalids = result.getInvalidDetails();
		assertEquals(invalids.get("UK"), Long.valueOf(1));
		assertEquals(result.getConfidence(), 1 - (double)1/(result.getSampleCount() - result.getBlankCount()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicStateSpaces() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("State", DateResolutionMode.DayFirst);
		final String pipedInput = " AL| AK| AZ| KY| KS| LA| ME| MD| MI| MA| MN| MS|MO |NE| MT| SD|TN | TX| UT| VT| WI|" +
						" VA| WA| WV| HI| ID| IL| IN| IA| KS| ky| LA| ME| MD| MA| MI| MN| MS| MO| MT| NE| NV|" +
						" NH| NJ| NM| NY| NC| ND| OH| OK| OR| PA| RI| SC| SD| TN| TX| UT| VT| VA| WA|  WV | WI|" +
						" WY| AL| AK| AZ| AR| CA| CO| CT| DC| de| FL| GA| HI| ID| IL| IN| IA| KS| KY| LA|SA|" +
						" MD| MA| MI| MN| MS| MO| MT| NE| NV| NH| NJ| NM| NY| NC| ND| OH| OK| OR| RI| SC| SD|" +
						" TX| UT| VT| WV| WI| WY| NV| NH| NJ| or| PA| RI| SC| AR| CA| CO| CT| ID| HI| IL| IN|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "STATE_PROVINCE.STATE_US");
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("STATE_PROVINCE.STATE_US").signature);
		assertEquals(result.getMatchCount(), inputs.length - result.getInvalidCount());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{2}");
		final Map<String, Long> invalids = result.getInvalidDetails();
		assertEquals(invalids.get("SA"), Long.valueOf(1));
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());

		final LogicalType logical = analysis.getPlugins().getRegistered("STATE_PROVINCE.STATE_US");
		for (final String input : inputs) {
			final String trimmed = input.trim();
			assertTrue(trimmed.matches(result.getRegExp()), input);
			final boolean expected = !invalids.containsKey(trimmed);
			assertEquals(logical.isValid(input, true), expected);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicStateMX() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Estado");

		final String[] inputs = {
				"SLP", "SON", "TAB", "TAM", "TLA", "VER", "YUC", "ZAC",
				"AGU", "BCN", "BCS", "CAM", "CHH", "CHP", "CMX", "COA",
				"COL", "DUR", "GRO", "GUA", "HID", "JAL", "MEX", "MIC",
				"MOR", "NAY", "NLE", "OAX", "PUE", "QUE", "ROO", "SIN",
				"SLP", "SON", "TAB", "TAM", "TLA", "UK", "YUC", "ZAC"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "STATE_PROVINCE.STATE_MX");
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("STATE_PROVINCE.STATE_MX").signature);
		assertEquals(result.getMatchCount(), inputs.length - result.getBlankCount() - result.getInvalidCount());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{3}");
		final Map<String, Long> invalids = result.getInvalidDetails();
		assertEquals(invalids.get("UK"), Long.valueOf(1));
		assertEquals(result.getConfidence(), 1 - (double)1/(result.getSampleCount() - result.getBlankCount()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicAUStateName() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("State");
		analysis.setLocale(Locale.forLanguageTag("en-AU"));

		final String[] inputs = {
				"Victoria", "New South Wales", "South Australia", "Tasmania", "Western Australia", "New South Wales", "Northern Territory", "Tasmania",
				"Northern Territory", "Australian Capital Territory", "Victoria", "Western Australia", "Western Australia", "Northern Territory", "Australian Capital Territory", "New South Wales",
				"Northern Territory", "South Australia", "Western Australia", "Queensland", "Northern Territory", "South Australia", "Australian Capital Territory", "Tasmania",
				"South Australia", "Victoria", "Western Australia", "Australian Capital Territory", "Victoria", "Australian Capital Territory", "Tasmania", "Northern Territory",
				"South Australia", "Victoria", "Victoria", "New South Wales", "New South Wales", "UK", "Tasmania", "Western Australia"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "STATE_PROVINCE.STATE_NAME_AU");
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("STATE_PROVINCE.STATE_NAME_AU").signature);
		assertEquals(result.getMatchCount(), inputs.length - result.getBlankCount() - result.getInvalidCount());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "[\\p{IsAlphabetic} ]+");
		final Map<String, Long> invalids = result.getInvalidDetails();
		assertEquals(invalids.get("UK"), Long.valueOf(1));
		assertEquals(result.getConfidence(), 1 - (double)1/(result.getSampleCount() - result.getBlankCount()));

		for (final String input : inputs) {
			if (!"UK".equals(input))
				assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicNAStateName() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Billing State/Province");

		final String[] inputs = {
				"ST", "ST", "NY", "IN", "IN", "Georgia", "WI", "WI", "WI", "WI", "WI", "WI", "WI",
				"WI", "WI", "WI", "WI", "WI", "WI", "WI", "WI", "WI", "WI", "WI", "Ohio", "Ohio", "Ohio",
				"Ohio", "Ohio", "Ohio", "WI", "Ohio", "Ohio", "Ohio", "Ohio", "Ohio", "Massachusetts",
				"CA", "MN", "MN", "MN", "MN", "MN", "Ny", "Ny", "MN", "IL", "IL", "IL", "IL", "NJ", "NJ",
				"NJ", "NJ", "NJ", "NJ", "IL", "IL", "NY", "MN", "MN", "MN", "MN", "MN", "MN", "MN", "MN",
				"MN", "MN", "MN", "MN", "MN", "MN", "MN", "MN", "Utrecht", "MN", "IN", "CO", "CO", "CO",
				"CO", "CO", "IL", "IL", "IL", "IL", "IL", "OK", "IA", "IA", "IA", "IL", "IL", "IL", "IL",
				"IL", "MI", "MI", "WI", "WI", "Brussels", "MI", "MI", "MI", "MI", "MI", "MI", "IL", "CA",
				"IL", "IL", "IL", "IL", "IL", "IL", "IL", "IL", "IL", "ON", "IL", "IL", "IL", "IL", "IL",
				"IL", "NY", "Michigan", "Michigan", "Michigan", "Michigan", "Michigan", "Michigan", "Michigan",
				"Michigan", "Michigan", "Michigan", "Michigan", "Michigan", "Michigan", "Michigan", "Michigan",
				"Michigan", "Michigan", "Michigan", "CA", "CA", "CA", "CA", "CT", "CT", "CT", "CT", "CT", "CT",
				"CT", "CA", "CA", "CA", "CA", "CA", "Michigan", "NY", "WI", "IL", "NC", "OR", "IL", "MO",
				"MO", "MO", "IL", "IL", "IL", "IL", "IL", "IL", "AL", "IL", "IL", "IL", "IL", "IL", "Comunidad de Madrid",
				"IL", "CA", "IDF", "Ohio", "Ohio", "Ohio", "Ohio", "Ohio", "Ohio", "OH", "CT", "", "IL", "IL", "IL"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "STATE_PROVINCE.STATE_PROVINCE_NAME_NA");
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("STATE_PROVINCE.STATE_PROVINCE_NAME_NA").signature);
		assertEquals(result.getMatchCount(),
				inputs.length - result.getBlankCount() - result.getInvalidCount() - result.getBlankCount());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "[\\p{IsAlphabetic} ]+");
		final Map<String, Long> invalids = result.getInvalidDetails();
		assertEquals(invalids.size(), 5);
		assertEquals(result.getConfidence(), 0.9696969696969697);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicEmailList() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicE_mailList");
		final String pipedInput = "Bachmann@lavastorm.com,Biedermann@lavastorm.com|buchheim@lavastorm.com|" +
				"coleman@lavastorm.com,Drici@lavastorm.com|Garvey@lavastorm.com|jackson@lavastorm.com|" +
				"Jones@lavastorm.com|Marinelli@lavastorm.com,Nason@lavastorm.com,Parker@lavastorm.com|" +
				"Pigneri@lavastorm.com|Rasmussen@lavastorm.com|Regan@lavastorm.com|Segall@Lavastorm.com|" +
				"Smith@lavastorm.com|Song@lavastorm.com|Tolleson@lavastorm.com|wynn@lavastorm.com|" +
				"Ahmed@lavastorm.com|Benoit@lavastorm.com|Keane@lavastorm.com|Kilker@lavastorm.com|" +
				"Waters@lavastorm.com|Meagher@lavastorm.com|Mok@lavastorm.com|Mullin@lavastorm.com|" +
				"Nason@lavastorm.com|reilly@lavastorm.com|Scoble@lavastorm.com|Comerford@lavastorm.com|" +
				"Gallagher@lavastorm.com|Hughes@lavastorm.com|Kelly@lavastorm.com|" +
				"Tuddenham@lavastorm.com,Williams@lavastorm.com,Wilson@lavastorm.com";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), EmailLT.SEMANTIC_TYPE);
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("EMAIL").signature);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), EmailLT.REGEXP);
		assertEquals(result.getConfidence(), 1.0);

		// Only simple emails match the regexp, so the count will not the 4 that include email lists :-(
		int matches = 0;
		for (final String input : inputs) {
			if (input.matches(result.getRegExp()))
				matches++;
		}
		assertEquals(result.getMatchCount() - 4, matches);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicEmailListSemicolon() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicE_mailListSemicolon");
		final String pipedInput = "Bachmann@lavastorm.com;Biedermann@lavastorm.com|buchheim@lavastorm.com|" +
				"coleman@lavastorm.com;Drici@lavastorm.com|Garvey@lavastorm.com|jackson@lavastorm.com|" +
				"Jones@lavastorm.com|Marinelli@lavastorm.com;Nason@lavastorm.com;Parker@lavastorm.com|" +
				"Pigneri@lavastorm.com|Rasmussen@lavastorm.com|Regan@lavastorm.com|Segall@Lavastorm.com|" +
				"Smith@lavastorm.com|Song@lavastorm.com|Tolleson@lavastorm.com|wynn@lavastorm.com|" +
				"Ahmed@lavastorm.com|Benoit@lavastorm.com|Keane@lavastorm.com|Kilker@lavastorm.com|" +
				"Waters@lavastorm.com|Meagher@lavastorm.com|Mok@lavastorm.com|Mullin@lavastorm.com|" +
				"Nason@lavastorm.com|reilly@lavastorm.com|Scoble@lavastorm.com|Comerford@lavastorm.com|" +
				"Gallagher@lavastorm.com|Hughes@lavastorm.com|Kelly@lavastorm.com|" +
				"Tuddenham@lavastorm.com;Williams@lavastorm.com;Wilson@lavastorm.com|bo gus|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), EmailLT.SEMANTIC_TYPE);
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("EMAIL").signature);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getInvalidCount(), 1);
		assertEquals(result.getMatchCount(), inputs.length - 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), EmailLT.REGEXP);
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicStates() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicS_tates");

		final String inputs[] = TestUtils.validUSStates.split("\\|");
		int locked = -1;

		analysis.train("XX");
		analysis.train("XX");
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i + 2;
		}
		analysis.train("XX");
		analysis.train("XX");
		analysis.train("XX");

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "STATE_PROVINCE.STATE_US");
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("STATE_PROVINCE.STATE_US").signature);
		assertEquals(result.getSampleCount(), inputs.length + 5);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getInvalidCount(), 1);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{2}");
		assertEquals(result.getConfidence(), 1 - (double)5/result.getSampleCount());

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicStatesBelowThreshold() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicS_tatesBelowThreshold");

		final String inputs[] = TestUtils.validUSStates.split("\\|");
		int locked = -1;
		final int BAD = 10;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		for (int i = 0; i < BAD; i++)
			analysis.train("XX");

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeQualifier());
		assertEquals(result.getSampleCount(), inputs.length + BAD);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length + BAD);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{2}");
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicStatesWithDash() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicS_tatesWithDash");

		final String pipedInput = "AL|AK|AZ|KY|KS|LA|ME|MD|MI|MA|MN|MS|MO|NE|MT|SD|TN|TX|UT|VT|WI|" +
				"VA|WA|WV|HI|ID|IL|IN|IA|KS|KY|LA|ME|MD|MA|MI|MN|MS|MO|MT|NE|NV|-|" +
				"NH|NJ|NM|NY|NC|ND|OH|OK|OR|PA|RI|SC|SD|TN|TX|UT|VT|VA|WA|WV|WI|-|" +
				"WY|AL|AK|AZ|AR|CA|CO|CT|DC|DE|FL|GA|HI|ID|IL|IN|IA|KS|KY|LA|ME|-|" +
				"MD|MA|MI|MN|MS|MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|OH|OK|OR|RI|SC|SD|-|" +
				"TX|UT|VT|WV|WI|WY|NV|NH|NJ|OR|PA|RI|SC|AR|CA|CO|CT|ID|HI|IL|IN|-|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "STATE_PROVINCE.STATE_US");
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("STATE_PROVINCE.STATE_US").signature);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getInvalidCount(), 1);
		assertEquals(result.getMatchCount(), inputs.length - 5);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{2}");
		assertEquals(result.getConfidence(), 1 - (double)5/result.getSampleCount());

		for (final String input : inputs) {
			if (!"-".equals(input))
				assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicStates100Percent() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicS_tates100Percent");

		analysis.setPluginThreshold(100);

		final String pipedInput = "AL|AK|AZ|KY|KS|LA|ME|MD|MI|MA|MN|MS|MO|NE|MT|SD|TN|TX|UT|VT|WI|" +
				"VA|WA|WV|HI|ID|IL|IN|IA|KS|KY|LA|ME|MD|MA|MI|MN|MS|MO|MT|NE|NV|XX|" +
				"NH|NJ|NM|NY|NC|ND|OH|OK|OR|PA|RI|SC|SD|TN|TX|UT|VT|VA|WA|WV|WI|XX|" +
				"WY|AL|AK|AZ|AR|CA|CO|CT|DC|DE|FL|GA|HI|ID|IL|IN|IA|KS|KY|LA|ME|XX|" +
				"MD|MA|MI|MN|MS|MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|OH|OK|OR|RI|SC|SD|XX|" +
				"TX|UT|VT|WV|WI|WY|NV|NH|NJ|OR|PA|RI|SC|AR|CA|CO|CT|ID|HI|IL|IN|XX|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeQualifier());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{2}");
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void thresholdTooLow() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("thresholdTooLow");

		try {
			analysis.setThreshold(0);
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Threshold must be between 0 and 100");
			return;
		}
		fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void thresholdTooHigh() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("thresholdTooHigh");

		try {
			analysis.setThreshold(101);
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Threshold must be between 0 and 100");
			return;
		}
		fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void thresholdPostStart() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("thresholdPostStart");

		analysis.setThreshold(100);

		final String pipedInput = "AL|AK|AZ|KY|KS|LA|ME|MD|MI|MA|MN|MS|MO|NE|MT|SD|TN|TX|UT|VT|WI|" +
				"VA|WA|WV|HI|ID|IL|IN|IA|KS|KY|LA|ME|MD|MA|MI|MN|MS|MO|MT|NE|NV|XX|" +
				"NH|NJ|NM|NY|NC|ND|OH|OK|OR|PA|RI|SC|SD|TN|TX|UT|VT|VA|WA|WV|WI|XX|" +
				"WY|AL|AK|AZ|AR|CA|CO|CT|DC|DE|FL|GA|HI|ID|IL|IN|IA|KS|KY|LA|ME|XX|" +
				"MD|MA|MI|MN|MS|MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|OH|OK|OR|RI|SC|SD|XX|" +
				"TX|UT|VT|WV|WI|WY|NV|NH|NJ|OR|PA|RI|SC|AR|CA|CO|CT|ID|HI|IL|IN|XX|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		try {
			analysis.setThreshold(80);
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Cannot adjust Threshold once training has started");
			return;
		}
		fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void pluginThresholdTooLow() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("pluginThresholdTooLow");

		try {
			analysis.setPluginThreshold(0);
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Plugin Threshold must be between 0 and 100");
			return;
		}
		fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void pluginThresholdTooHigh() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("pluginThresholdTooHigh");

		try {
			analysis.setPluginThreshold(101);
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Plugin Threshold must be between 0 and 100");
			return;
		}
		fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void pluginThresholdPostStart() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("pluginThresholdPostStart");

		analysis.setPluginThreshold(100);

		final String pipedInput = "AL|AK|AZ|KY|KS|LA|ME|MD|MI|MA|MN|MS|MO|NE|MT|SD|TN|TX|UT|VT|WI|" +
				"VA|WA|WV|HI|ID|IL|IN|IA|KS|KY|LA|ME|MD|MA|MI|MN|MS|MO|MT|NE|NV|XX|" +
				"NH|NJ|NM|NY|NC|ND|OH|OK|OR|PA|RI|SC|SD|TN|TX|UT|VT|VA|WA|WV|WI|XX|" +
				"WY|AL|AK|AZ|AR|CA|CO|CT|DC|DE|FL|GA|HI|ID|IL|IN|IA|KS|KY|LA|ME|XX|" +
				"MD|MA|MI|MN|MS|MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|OH|OK|OR|RI|SC|SD|XX|" +
				"TX|UT|VT|WV|WI|WY|NV|NH|NJ|OR|PA|RI|SC|AR|CA|CO|CT|ID|HI|IL|IN|XX|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		try {
			analysis.setPluginThreshold(80);
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Cannot adjust Plugin Threshold once training has started");
			return;
		}
		fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void collectStatisticsPostStart() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("collectStatisticsPostStart");

		final String pipedInput = "AL|AK|AZ|KY|KS|LA|ME|MD|MI|MA|MN|MS|MO|NE|MT|SD|TN|TX|UT|VT|WI|" +
				"VA|WA|WV|HI|ID|IL|IN|IA|KS|KY|LA|ME|MD|MA|MI|MN|MS|MO|MT|NE|NV|XX|" +
				"NH|NJ|NM|NY|NC|ND|OH|OK|OR|PA|RI|SC|SD|TN|TX|UT|VT|VA|WA|WV|WI|XX|" +
				"WY|AL|AK|AZ|AR|CA|CO|CT|DC|DE|FL|GA|HI|ID|IL|IN|IA|KS|KY|LA|ME|XX|" +
				"MD|MA|MI|MN|MS|MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|OH|OK|OR|RI|SC|SD|XX|" +
				"TX|UT|VT|WV|WI|WY|NV|NH|NJ|OR|PA|RI|SC|AR|CA|CO|CT|ID|HI|IL|IN|XX|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		try {
			analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "Cannot adjust feature 'COLLECT_STATISTICS' once training has started");
			return;
		}
		fail("Exception should have been thrown");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicStatesLower() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicS_tatesLower");
		final String pipedInput = "al|ak|az|ky|ks|la|me|md|mi|ma|mn|ms|mo|ne|mt|sd|tn|tx|ut|vt|wi|" +
				"va|wa|wv|hi|id|il|in|ia|ks|ky|la|me|md|ma|mi|mn|ms|mo|mt|ne|nv|" +
				"nh|nj|nm|ny|nc|nd|oh|ok|or|pa|ri|sc|sd|tn|tx|ut|vt|va|wa|wv|wi|" +
				"wy|al|ak|az|ar|ca|co|ct|dc|de|fl|ga|hi|id|il|in|ia|ks|ky|la|me|" +
				"md|ma|mi|mn|ms|mo|mt|ne|nv|nh|nj|nm|ny|nc|nd|oh|ok|or|ri|sc|sd|" +
				"tx|ut|vt|wv|wi|wy|nv|nh|nj|or|pa|ri|sc|ar|ca|co|ct|id|hi|il|in|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "STATE_PROVINCE.STATE_US");
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("STATE_PROVINCE.STATE_US").signature);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{2}");
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicCA() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicCA");
		final String inputs[] = TestUtils.validCAProvinces.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "STATE_PROVINCE.PROVINCE_CA");
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("STATE_PROVINCE.PROVINCE_CA").signature);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{2}");
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicAU() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicAU");
		analysis.setDebug(2);
		analysis.setLocale(Locale.forLanguageTag("en-AU"));
		final String inputs[] = TestUtils.validAUStates.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length * 10; i++) {
			if (analysis.train(inputs[i % inputs.length]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "STATE_PROVINCE.STATE_AU");
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("STATE_PROVINCE.STATE_AU").signature);
		assertEquals(result.getSampleCount(), inputs.length * 10);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length * 10);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "(?i)(ACT|NSW|NT|QLD|SA|TAS|VIC|WA)");
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void notZipButNumeric() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("notZipButNumeric");
		final int start = 10000;
		final int end = 99999;

		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i;
		}
		analysis.train("No Zip provided");

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, start + AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeQualifier());
		assertEquals(result.getSampleCount(), end + 1 - start);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), USZip5.REGEXP_ZIP5);
		assertEquals(result.getMatchCount(), end - start);
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void notZips() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("notZips");
		final int start = 10000;
		final int end = 99999;

		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train(i < 80000 ? String.valueOf(i) : "A" + String.valueOf(i)) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, start + AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeQualifier());
		assertEquals(result.getSampleCount(), end - start);
		assertEquals(result.getMatchCount(), end - start);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "(\\p{IsAlphabetic})?\\d{5}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getMinValue(), "10000");
		assertEquals(result.getMaxValue(), "A99998");

		assertTrue("10000".matches(result.getRegExp()));
		assertTrue("A99998".matches(result.getRegExp()));
	}

	// Set of valid months + 4 x "UNK"
	private final static String MONTH_TEST_GERMAN =
			"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|UNK|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|UNK|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|UNK|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|UNK|Nov|Dez|";

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicMonthAbbrGerman() throws IOException, FTAException {

		if (!TestUtils.isValidLocale("de"))
			return;

		final Locale german = Locale.forLanguageTag("de");

		final DateFormatSymbols dfs = new DateFormatSymbols(german);
		final String[] m = dfs.getShortMonths();
		final GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(german);
		final int actualMonths = cal.getActualMaximum(Calendar.MONTH);

		final TextAnalyzer analysis = new TextAnalyzer("basicMonthAbbrGerman");
		analysis.setLocale(german);
		analysis.configure(TextAnalyzer.Feature.NO_ABBREVIATION_PUNCTUATION, false);

		final int badCount = 4;
		final int iterations = 10;
		int bads = 0;

		for (int i = 0; i < iterations; i++) {
			for (int j = 0; j < actualMonths; j++)
				analysis.train(m[j]);
			if (bads < badCount) {
				analysis.train("UNKN");
				bads++;
			}
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getRegExp(), TestUtils.getJavaVersion() == 8 ? "\\p{IsAlphabetic}{3}" : "[\\p{IsAlphabetic}\\.]{3,4}");
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "MONTH.ABBR_de");
		assertEquals(result.getSampleCount(), iterations * actualMonths + badCount);
		assertEquals(result.getOutlierCount(), 0);
		final Map<String, Long> invalids = result.getInvalidDetails();
		assertEquals(invalids.size(), 1);
		assertEquals(invalids.get("UNKN"), Long.valueOf(4));
		assertEquals(result.getMatchCount(), iterations * actualMonths);
		assertEquals(result.getNullCount(), 0);
		assertTrue((double)analysis.getPluginThreshold()/100 < result.getConfidence());
		assertEquals(result.getConfidence(), 1 - (double)badCount/result.getSampleCount());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicMonthAbbrBackout() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicMonthAbbrBackout");
		final String inputs[] = TestUtils.months.split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		final int unknownCount = 10;
		for (int i = 0; i < unknownCount; i++)
			analysis.train("UNK");

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getRegExp(), "(?i)(APR|AUG|DEC|FEB|JAN|JUL|JUN|MAR|MAY|NOV|OCT|SEP|UNK)");
		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeQualifier());
		assertEquals(result.getSampleCount(), result.getMatchCount());
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1.0);
		assertTrue(inputs[0].matches(result.getRegExp()));

		int matches = 0;
		for (final String input : inputs) {
			if (input.matches(result.getRegExp()))
					matches++;
		}
		assertEquals(result.getMatchCount() - unknownCount, matches);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicMonthAbbrExcessiveBad() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicMonthAbbrExcessiveBad");
		final String inputs[] = TestUtils.months.split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		final int unknownCount = 10;
		for (int i = 0; i < unknownCount; i++)
			analysis.train("Bad");
		analysis.train("NA");

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getRegExp(), "(?i)(APR|AUG|BAD|DEC|FEB|JAN|JUL|JUN|MAR|MAY|NA|NOV|OCT|SEP|UNK)");
		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeQualifier());
		assertEquals(result.getSampleCount(), inputs.length + unknownCount + 1);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length + unknownCount + 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1.0);
		assertTrue(inputs[0].matches(result.getRegExp()));

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicMonthAbbr() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicMonthAbbr");
		final int badCount = 4;
		final String inputs[] = TestUtils.months.split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getTypeQualifier(), "MONTH.ABBR_en-US");
		assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{3}");
		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		final Map<String, Long> invalids = result.getInvalidDetails();
		assertEquals(invalids.size(), 1);
		assertEquals(invalids.get("UNK"), Long.valueOf(4));
		assertEquals(result.getMatchCount(), inputs.length - badCount);
		assertEquals(result.getNullCount(), 0);
		assertTrue((double)analysis.getPluginThreshold()/100 < result.getConfidence());
		assertEquals(result.getConfidence(), 1 - (double)badCount/result.getSampleCount());

		// Even the UNK match the RE
		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));

/*
		analysis.train("Another bad element");
		result = analysis.getResult();

		assertEquals(result.getRegExp(), "\\p{Alpha}{3}");
		assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		assertEquals(result.getType(), PatternInfo.Type.STRING);
		assertTrue((double)analysis.getPluginThreshold()/100 < result.getConfidence());
		assertNull(result.getTypeQualifier());
		assertEquals(result.getSampleCount(), inputs.length + 1);
		assertEquals(result.getOutlierCount(), 0);
		Map<String, Integer> updatedOutliers = result.getOutlierDetails();
		assertEquals(updatedOutliers.size(), 2);
		assertEquals(updatedOutliers.get("UNK"), Integer.valueOf(4));
		assertEquals(updatedOutliers.get("Another bad element"), Integer.valueOf(1));
		assertEquals(result.getMatchCount(), inputs.length - badCount);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1 - (double)(badCount + 1)/result.getSampleCount());
		*/
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicMonthAbbr_enCA() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicMonthAbbr");
		analysis.setLocale(Locale.CANADA);
		final int badCount = 4;
		final String inputs[] = TestUtils.months.split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getTypeQualifier(), "MONTH.ABBR_en-CA");
		assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{3}");
		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		final Map<String, Long> invalids = result.getInvalidDetails();
		assertEquals(invalids.size(), 1);
		assertEquals(invalids.get("UNK"), Long.valueOf(4));
		assertEquals(result.getMatchCount(), inputs.length - badCount);
		assertEquals(result.getNullCount(), 0);
		assertTrue((double)analysis.getPluginThreshold()/100 < result.getConfidence());
		assertEquals(result.getConfidence(), 1 - (double)badCount/result.getSampleCount());

		// Even the UNK match the RE
		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicMonthAbbrFrench() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicMonthAbbrFrench");
		analysis.setLocale(Locale.FRENCH);
		analysis.configure(TextAnalyzer.Feature.NO_ABBREVIATION_PUNCTUATION, false);
		final int badCount = 4;
		final String inputs[] = TestUtils.monthsFrench.split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getRegExp(), "[\\p{IsAlphabetic}\\.]{3,5}");
		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "MONTH.ABBR_fr");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		final Map<String, Long> invalids = result.getInvalidDetails();
		assertEquals(invalids.size(), 1);
		assertEquals(invalids.get("UNK"), Long.valueOf(4));
		assertEquals(result.getMatchCount(), inputs.length - badCount);
		assertEquals(result.getNullCount(), 0);
		assertTrue((double)analysis.getPluginThreshold()/100 < result.getConfidence());
		assertEquals(result.getConfidence(), 1 - (double)badCount/result.getSampleCount());

		// Even the UNK match the RE
		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()), input);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicStateLowCard() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("State", DateResolutionMode.DayFirst);
		final String input = "MA|MI|ME|MO|MS|";
		final String inputs[] = input.split("\\|");
		final int iters = 20;

		int locked = -1;

		for (int j = 0; j < iters; j++) {
			for (int i = 0; i < inputs.length; i++) {
				if (analysis.train(inputs[i]) && locked == -1)
					locked = i;
			}
		}
		final int UNKNOWN = 4;
		for (int k = 0; k < UNKNOWN; k++)
			analysis.train("NA");

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{2}");
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "STATE_PROVINCE.STATE_US");
		assertEquals(result.getSampleCount(), inputs.length * iters + UNKNOWN);
		assertEquals(result.getCardinality(), 5);
		assertEquals(result.getOutlierCount(), 0);
		final Map<String, Long> invalids = result.getInvalidDetails();
		assertEquals(invalids.size(), 1);
		assertEquals(invalids.get("NA"), Long.valueOf(UNKNOWN));
		assertEquals(result.getMatchCount(), inputs.length * iters);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1 - (double)UNKNOWN/result.getSampleCount());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicISO4127() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("CurrencyCode");
		final String pipedInput = "JMD|JOD|JPY|KES|KGS|KHR|KMF|KPW|KZT|LRD|MKD|MRU|" +
				"AFN|AOA|BBD|BIF|BSD|BZD|CHE|CHF|CHW|CLF|CLP|CNY|" +
				"MYR|NIO|PEN|PLN|RWF|SDG|SHP|SLL|SOS|SRD|SSP|STN|" +
				"SVC|SYP|SZL|THB|TOP|TZS|UYU|VND|XBA|XCD|XPD|XPF|" +
				"COP|COU|CRC|CUC|DJF|EGP|GBP|GMD|HRK|ILS|IRR|ISK|" +
				"XPT|XSU|XTS|XUA|XXX|YER|ZAR|ZMW|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{3}");
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "CURRENCY_CODE.ISO-4217");
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("CURRENCY_CODE.ISO-4217").signature);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getCardinality(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getMinLength(), 3);
		assertEquals(result.getMaxLength(), 3);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicISO3166_3() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("3166 Alpha-3");
		final String inputs[] = TestUtils.valid3166_3.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{3}");
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "COUNTRY.ISO-3166-3");
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("COUNTRY.ISO-3166-3").signature);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getCardinality(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getMinLength(), 3);
		assertEquals(result.getMaxLength(), 3);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testGenderPT() throws IOException, FTAException {
		String[] samples = new String[1000];

		for (int i = 0; i < samples.length; i++) {
			samples[i] = random.nextInt(2) == 1 ? "femenino" : "masculino";
		}

		final TextAnalyzer analysis = new TextAnalyzer("genero");
		analysis.configure(TextAnalyzer.Feature.DEFAULT_LOGICAL_TYPES, false);
		final Locale portuguese = Locale.forLanguageTag("pt-BR");
		analysis.setLocale(portuguese);
		final List<PluginDefinition> plugins = new ArrayList<>();
		plugins.add(new PluginDefinition("GENDER_PT", "Gender (Portuguese Language)", null, null, null, "\\d{3}-\\d{2}-\\d{4}",
				new PluginLocaleEntry[] { new PluginLocaleEntry("pt", null, 90, "(?i)(FEMENINO|MASCULINO)") },
				true, 98, FTAType.STRING));

		try {
			analysis.getPlugins().registerPluginList(plugins, analysis.getStreamName(), analysis.getConfig());
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
		assertEquals(result.getTypeQualifier(), "GENDER_PT");
		assertEquals(result.getConfidence(), 1);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getSampleCount(), samples.length);

		for (final String sample : samples) {
			assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testFinitePlugin() throws IOException, FTAException {
		final InlineContent planets = new InlineContent(new String[] { "MERCURY", "VENUS", "EARTH", "MARS", "JUPITER", "SATURN", "URANUS", "NEPTUNE", "PLUTO", "" });
		final int SAMPLES = 100;

		final PluginDefinition pluginDefinition = new PluginDefinition("PLANET", "One of the planets orbiting our Solar System",
				null, (new ObjectMapper()).writeValueAsString(planets), "inline", "\\p{Alpha}*",
				new PluginLocaleEntry[] { new PluginLocaleEntry("en", null, 90, null) }, true,  98, FTAType.STRING);


		final TextAnalyzer analysis = new TextAnalyzer("Planets");
		final List<PluginDefinition> plugins = new ArrayList<>();
		plugins.add(pluginDefinition);

		try {
			analysis.getPlugins().registerPluginList(plugins, "Planets", analysis.getConfig());
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		for (int i = 0; i < SAMPLES; i++) {
			analysis.train(planets.members[random.nextInt(planets.members.length)]);
		}
		analysis.train("032--45-0981");

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		final String re = result.getRegExp();
		assertEquals(re, "(?i)(|EARTH|JUPITER|MARS|MERCURY|NEPTUNE|PLUTO|SATURN|URANUS|VENUS)");
		assertEquals(result.getTypeQualifier(), "PLANET");
		assertEquals(result.getConfidence(), 1 - (double)1/(result.getSampleCount() - result.getBlankCount()));
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getInvalidCount(), 1);
		assertEquals(result.getSampleCount(), SAMPLES + 1);
		final Map<String, Long> invalids = result.getInvalidDetails();
		assertEquals(invalids.size(), 1);
		assertEquals(invalids.get("032--45-0981"), Long.valueOf(1));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testFinitePluginBackout() throws IOException, FTAException {
		final InlineContent planets = new InlineContent(new String[] { "MERCURY", "VENUS", "EARTH", "MARS", "JUPITER", "SATURN", "URANUS", "NEPTUNE", "PLUTO", "" });
		final int SAMPLES = 100;

		final PluginDefinition pluginDefinition = new PluginDefinition(
				"PLANET", "One of the planets orbiting our Solar System", null, (new ObjectMapper()).writeValueAsString(planets), "inline", "\\p{Alpha}*",
				new PluginLocaleEntry[] { new PluginLocaleEntry("en", null, 90, "\\p{Alpha}*") }, true, 98, FTAType.STRING);

		final TextAnalyzer analysis = new TextAnalyzer("Planets");
		final List<PluginDefinition> plugins = new ArrayList<>();
		plugins.add(pluginDefinition);

		try {
			analysis.getPlugins().registerPluginList(plugins, "Planets", analysis.getConfig());
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		analysis.train("io");
		for (int i = 0; i < SAMPLES; i++) {
			analysis.train(planets.members[random.nextInt(planets.members.length)]);
		}
		analysis.train("europa");

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), "(?i)(EARTH|EUROPA|IO|JUPITER|MARS|MERCURY|NEPTUNE|PLUTO|SATURN|URANUS|VENUS)");
		assertNull(result.getTypeQualifier());
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getSampleCount(), SAMPLES + 2);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testInfiniteDoublePlugin() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("PERCENTAGES");
		final int COUNT = 1000;
		final List<PluginDefinition> plugins = new ArrayList<>();
		final PluginDefinition plugin = new PluginDefinition("PERCENT", "com.cobber.fta.PluginPercent");

		plugin.validLocales = new PluginLocaleEntry[] { new PluginLocaleEntry("en", ".*(?i)(percent).*", 90, "\\d*\\.?\\d+") };
		plugins.add(plugin);

		try {
			analysis.getPlugins().registerPluginList(plugins, "Percentages", analysis.getConfig());
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		for (int i = 0; i < COUNT; i++)
			analysis.train(String.valueOf(random.nextDouble()));

		analysis.train("A");
		analysis.train("BBBBBBBBBBBBBBBBBBBBBBBBB");

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getRegExp(), PluginPercent.REGEXP);
		assertEquals(result.getTypeQualifier(), "PERCENT");
		assertEquals(result.getSampleCount(), COUNT + 2);
		assertEquals(result.getMatchCount(), COUNT);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getMinLength(), 1);
		assertEquals(result.getMaxLength(), 25);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getConfidence(), 1 - (double)2/result.getSampleCount());
	}

	/*@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS }) - TODO dates do not support Semantic Types :-( */
	public void testInfiniteDateTimePlugin() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("DOB");
		final int COUNT = 1000;

		final List<PluginDefinition> plugins = new ArrayList<>();
		final PluginDefinition plugin = new PluginDefinition("BIRTHDATE", "com.cobber.fta.PluginBirthDate");
		plugin.validLocales = new PluginLocaleEntry[] { new PluginLocaleEntry("en", ".*(?i)(DOB).*", 90, "\\d{4}/\\d{2}/\\d{2}") };
		plugins.add(plugin);

		try {
			analysis.getPlugins().registerPluginList(plugins, "BirthDate", analysis.getConfig());
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		LocalDate localDate = LocalDate.now();

		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

		for (int i = 0; i < COUNT; i++) {
			final String sample = localDate.format(formatter);
			analysis.train(sample);
			localDate = localDate.minusDays(10);
		}

		analysis.train("A");
		analysis.train("BBBBBBBBBBBBBBBBBBBBBBBBB");

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getRegExp(), PluginBirthDate.REGEXP);
		assertEquals(result.getTypeQualifier(), "PERCENT");
		assertEquals(result.getSampleCount(), COUNT + 2);
		assertEquals(result.getMatchCount(), COUNT);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getMinLength(), 1);
		assertEquals(result.getMaxLength(), 25);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getConfidence(), 1 - (double)2/result.getSampleCount());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testNames() throws IOException, FTAException {
		final CsvParserSettings settings = new CsvParserSettings();
		settings.setHeaderExtractionEnabled(true);
		String[] header;
		String[] row;
		TextAnalyzer[] analysis;

		try (BufferedReader in = new BufferedReader(new InputStreamReader(TestPlugins.class.getResourceAsStream("/Names.txt"), StandardCharsets.UTF_8))) {

			final CsvParser parser = new CsvParser(settings);
			parser.beginParsing(in);

			header = parser.getRecordMetadata().headers();
			analysis = new TextAnalyzer[header.length];
			for (int i = 0; i < header.length; i++) {
				analysis[i] = new TextAnalyzer(new AnalyzerContext(header[i], DateResolutionMode.Auto, "Names.txt", header));
			}
			while ((row = parser.parseNext()) != null) {
				for (int i = 0; i < row.length; i++) {
					analysis[i].train(row[i]);
				}
			}
		}

		final TextAnalysisResult first = analysis[0].getResult();
		assertEquals(first.getTypeQualifier(), "NAME.FIRST");
		assertEquals(first.getStructureSignature(), PluginDefinition.findByQualifier("NAME.FIRST").signature);

		final TextAnalysisResult last = analysis[1].getResult();
		assertEquals(last.getTypeQualifier(), "NAME.LAST");
		assertEquals(last.getStructureSignature(), PluginDefinition.findByQualifier("NAME.LAST").signature);

		final TextAnalysisResult middle = analysis[2].getResult();
		assertEquals(middle.getTypeQualifier(), "NAME.MIDDLE");
		assertEquals(middle.getStructureSignature(), PluginDefinition.findByQualifier("NAME.MIDDLE").signature);

		final TextAnalysisResult middleInitial = analysis[3].getResult();
		assertEquals(middleInitial.getTypeQualifier(), "NAME.MIDDLE_INITIAL");
		assertEquals(middleInitial.getStructureSignature(), PluginDefinition.findByQualifier("NAME.MIDDLE_INITIAL").signature);

		final LogicalType logicalFirst = analysis[0].getPlugins().getRegistered(FirstName.SEMANTIC_TYPE);
		assertTrue(logicalFirst.isValid("Harry", true));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testNameHeader() throws IOException, FTAException {
		final String headersGood[] = {
				"FirstName", "First Name", "First_Name", "FIRSTNAME", "FNAME", "FIRST.NAME", "FIRST-NAME", "GIVEN NAME"
		};
		final String headersMaybe[] = {
				"NAME"
		};
		final String headersBad[] = {
				null, ""
		};
		final String[] samplesGoodFirstOnly = {
				"Elizabeth", "Sarah", "Tim", "Paula", "Bethany", "Reginald", "Amanda", "Nancy", "Margaret", "Leila"
		};
		final String[] samplesUnknown = {
				"Mary Beth", "Mary Joe"
		};

		// So you need 40% or better success rate if you have 'good' headers
		for (int i = 0; i < 100; i++) {
			for (final String header : headersGood) {
				final TextAnalyzer analysis = new TextAnalyzer(header);
				for (int s = 0; s < 2; s++)
					analysis.train(samplesGoodFirstOnly[s]);

				for (final String sample : samplesUnknown)
					analysis.train(sample);

				assertEquals(analysis.getResult().getTypeQualifier(), "NAME.FIRST", header);
				assertEquals(analysis.getResult().getStructureSignature(), PluginDefinition.findByQualifier("NAME.FIRST").signature);
			}
		}

		// So you need 60% or better success rate if you have 'maybe' headers
		for (int i = 0; i < 100; i++) {
			for (final String header : headersMaybe) {
				final TextAnalyzer analysis = new TextAnalyzer(header);
				for (int s = 0; s < 5; s++)
					analysis.train(samplesGoodFirstOnly[s]);

				for (final String sample : samplesUnknown)
					analysis.train(sample);

				assertEquals(analysis.getResult().getTypeQualifier(), "NAME.FIRST", header);
				assertEquals(analysis.getResult().getStructureSignature(), PluginDefinition.findByQualifier("NAME.FIRST").signature);
			}
		}

		// With 'bad' headers you really need good data, and more of it!
		for (final String header : headersBad) {
			final TextAnalyzer analysis = new TextAnalyzer(header);
			for (int i = 0; i < 4; i++)
				for (final String sample : samplesGoodFirstOnly)
					analysis.train(sample);

			assertEquals(analysis.getResult().getTypeQualifier(), "NAME.FIRST", header);
			assertEquals(analysis.getResult().getStructureSignature(), PluginDefinition.findByQualifier("NAME.FIRST").signature);
		}

		// With 'bad' headers you really need good data - this is an example of not enough data!
		for (final String header : headersBad) {
			final TextAnalyzer analysis = new TextAnalyzer(header);
			for (final String sample : samplesGoodFirstOnly) {
				analysis.train(sample);
			}

			assertNull(analysis.getResult().getTypeQualifier(), header);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testRegExpLogicalType_CUSIP() throws IOException, FTAException {
		final String CUSIP_REGEXP = "[\\p{IsAlphabetic}\\d]{9}";
		final String[] samples = {
				"B38564108", "B38564908", "B38564958", "C10268AC1", "C35329AA6", "D18190898", "D18190908", "D18190958", "G0084W101", "G0084W901",
				"G0084W951", "G0129K104", "G0129K904", "G0129K954", "G0132V105", "G0176J109", "G0176J909", "G0176J959", "G01767105", "G01767905",
				"G01767955", "G0177J108", "G0177J908", "G0177J958", "G02602103", "G02602903", "G02602953", "G0335L102", "G0335L902", "G0335L952"

		};

		final TextAnalyzer analysis = new TextAnalyzer("CUSIP");

		final List<PluginDefinition> plugins = new ArrayList<>();
		plugins.add(new PluginDefinition("CUSIP", "Another sort of CUSIP",
				null, null, null, "[\\p{IsAlphabetic}\\d]{9}",
				new PluginLocaleEntry[] { new PluginLocaleEntry("*", ".*CUSIP.*", 100, "[\\p{IsAlphabetic}\\d]{9}") },
				false, 98, FTAType.STRING));

		try {
			analysis.getPlugins().registerPluginList(plugins, "CUSIP", analysis.getConfig());
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getRegExp(), CUSIP_REGEXP);
		assertEquals(result.getTypeQualifier(), "CUSIP");
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples) {
			assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testRegExpLogicalType_CUSIP_bulk() throws IOException, FTAException {
		final String CUSIP_REGEXP = "[\\p{IsAlphabetic}\\d]{9}";
		final String[] samples = {
				"B38564108", "B38564908", "B38564958", "C10268AC1", "C35329AA6", "D18190898", "D18190908", "D18190958", "G0084W101", "G0084W901",
				"G0084W951", "G0129K104", "G0129K904", "G0129K954", "G0132V105", "G0176J109", "G0176J909", "G0176J959", "G01767105", "G01767905",
				"G01767955", "G0177J108", "G0177J908", "G0177J958", "G02602103", "G02602903", "G02602953", "G0335L102", "G0335L902", "G0335L952"

		};

		final TextAnalyzer analysis = new TextAnalyzer("CUSIP");

		final List<PluginDefinition> plugins = new ArrayList<>();
		plugins.add(new PluginDefinition("CUSIP", "Another sort of CUSIP",
				null, null, null, "[\\p{IsAlphabetic}\\d]{9}",
				new PluginLocaleEntry[] { new PluginLocaleEntry("*", ".*CUSIP.*", 100, "[\\p{IsAlphabetic}\\d]{9}") }, false, 98, FTAType.STRING));

		try {
			analysis.getPlugins().registerPluginList(plugins, "CUSIP", analysis.getConfig());
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		final Map<String, Long> observed = new HashMap<>();
		for (int i = 0; i < samples.length; i++) {
			observed.put(samples[i], Long.valueOf(i + 1));
		}
		analysis.trainBulk(observed);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), (samples.length * (samples.length + 1)) / 2);
		assertEquals(result.getRegExp(), CUSIP_REGEXP);
		assertEquals(result.getTypeQualifier(), "CUSIP");
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples) {
			assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testSignatures() throws IOException, FTAException {
		final String CUSIP_REGEXP = "[\\p{IsAlphabetic}\\d]{9}";
		final String[] samples = {
				"B38564108", "B38564908", "B38564958", "C10268AC1", "C35329AA6", "D18190898", "D18190908", "D18190958", "G0084W101", "G0084W901",
				"G0084W951", "G0129K104", "G0129K904", "G0129K954", "G0132V105", "G0176J109", "G0176J909", "G0176J959", "G01767105", "G01767905",
				"G01767955", "G0177J108", "G0177J908", "G0177J958", "G02602103", "G02602903", "G02602953", "G0335L102", "G0335L902", "G0335L952"
		};

		final TextAnalyzer preAnalysis = new TextAnalyzer("CUSIP");

		for (final String sample : samples)
			preAnalysis.train(sample);

		final TextAnalysisResult preResult = preAnalysis.getResult();

		assertEquals(preResult.getSampleCount(), samples.length);
		assertEquals(preResult.getRegExp(), CUSIP_REGEXP);
		assertNull(preResult.getTypeQualifier());
		assertEquals(preResult.getBlankCount(), 0);
		assertEquals(preResult.getNullCount(), 0);
		assertEquals(preResult.getType(), FTAType.STRING);
		assertEquals(preResult.getConfidence(), 1.0);
		assertEquals(preResult.getStructureSignature(), "yW7lIrjlrjF/WZwIInoH/TrmhCw=");
		assertEquals(preResult.getShapeCount(), 3);
		final Iterator<Entry<String, Long>> shapes =  preResult.getShapeDetails().entrySet().iterator();

		final Map.Entry<String, Long> first = shapes.next();
		assertEquals(first.getKey(), "X99999999");
		assertEquals(first.getValue().longValue(), 12);
		final Map.Entry<String, Long> second = shapes.next();
		assertEquals(second.getKey(), "X99999XX9");
		assertEquals(second.getValue().longValue(), 2);
		final Map.Entry<String, Long> third = shapes.next();
		assertEquals(third.getKey(), "X9999X999");
		assertEquals(third.getValue().longValue(), 16);

		for (final String sample : samples) {
			assertTrue(sample.matches(preResult.getRegExp()));
		}

		final TextAnalyzer analysis = new TextAnalyzer("CUSIP");

		final List<PluginDefinition> plugins = new ArrayList<>();
		plugins.add(new PluginDefinition("CUSIP", "Another sort of CUSIP",
				null, null, null, "[\\p{IsAlphabetic}\\d]{9}",
				new PluginLocaleEntry[] { new PluginLocaleEntry("*", ".*(?i)(cusip).*", 100, "[\\p{IsAlphabetic}\\d]{9}") }, false, 98, FTAType.STRING));

		try {
			analysis.getPlugins().registerPluginList(plugins, "CUSIP", analysis.getConfig());
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getRegExp(), CUSIP_REGEXP);
		assertEquals(result.getTypeQualifier(), "CUSIP");
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getStructureSignature(), "Frd1mNXRneO3yWDzQa4eEdRgtJs=");

		for (final String sample : samples) {
			assertTrue(sample.matches(result.getRegExp()));
		}

		// Data Signature is independent of Structure
		assertEquals(preResult.getDataSignature(), result.getDataSignature());
		assertEquals(preResult.getDataSignature(), "hiU3XR5WlhejAaWRPuDh0/i2A48=");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testRegExpLogicalType_FUND() throws IOException, FTAException {
		final String FUND_REGEXP = "[\\p{IsAlphabetic}\\d]{10}";
		final String[] samples = {
				"BFLGXF682X", "BFLGXF682W", "PNUJWNGCFQ", "SVAXRA5JCJ", "Y7OBPIXWM9", "G8K8TRB34J", "Y3EB59C7IS", "SJBDBG2P2M", "4XC8B8ZE2Y", "JN7BXG4Z6B",
				"VMTM4FS09S", "INX2TS29XH", "HCUBHG6SL8", "KTCZJMS3C2", "YOUOL9IN2K", "NK8YM32JKG", "J8608B2931", "YFJBB0HOUS", "HKPS0A7DGO", "6USXWQTEK1",
				"4BTJHZ1I2V", "PDGWWLKDY9", "OQ1KEBQD85", "HS8JYUVVCO", "WZYULQRKW2", "SDLJVXYOUW", "2DBSKJEJMK", "J0DN2PR11M", "DDIKS9IUVJ", "WRUDP8V53N",
				"TTP0QM9LBB", "YOBTOPQ7SS", "FEIQJXA9QS", "YFX7A29YP7", "SHMP1HGGJP", "3SSITU608H", "UPGYH13E22", "LD7QN90UL9", "2RHOBF94OY", "QM8EUAU5Y8",
				"WX9A0C9BX2", "GYAVONF05B", "3EMMIG52FC", "389OCJM16S", "ELKZOOXWQQ", "G1QQO07DVX", "B9KAVG2XIO", "Z7PMK6HZIT", "30997SFT8G", "445X9OVQ8I",
				"7DJLANTCAM", "3LUZTHNKYQ", "Y75AWAD2J7", "43BWNCE0IO", "WETOQEXVMK", "I9QJC1Y362", "BZPIBC32J8", "QUGEIX28PQ", "803ZIHG8TB", "M27W6A2OWF",
				"FMQ9O6NXTP", "X15CFBQCEN", "7G2FOQTA9G", "3SSZJ0HFAI", "I7ONRG4LL9", "QIRLXTQ67R", "ULBT4MG4I4", "2NYTJ3SU91", "6U3CFJCLRB", "IRODHFP3WZ",
				"RBXXUTBHE9", "3XEZPGG3HY", "AX4ZKUSXIN", "SO6NPS35C8", "09SPAMYVBM", "9UEPW1GV3B", "QKONJM4PL7", "S7QY4O08GH", "4372MH2Q6H", "6UJROS7NZI",
				"HV95MAJQH5", "D0VQHHJZTG", "9Y4HY3JG6F", "OTYHGPG5AL", "ND1CE5NHI1", "J3U18BFLEQ", "BCZA5IYEU2", "SN9WQMMFYH", "HMRLQUSGYG", "PHMEA59YCI",
				"X5Q7VGKBSA", "BG5G1NPDV0", "83C87F75FN", "L76A3ARGHL", "89VOPGUFK0", "8TJCZGI05B", "VLEPQIKH22", "0FB3TX3VLX", "CFDVZLQZVM", "1CDYRDTTV3",
				"7M5ABGF3V8"
		};

		final TextAnalyzer analysis = new TextAnalyzer("FUND_ID");
		analysis.configure(TextAnalyzer.Feature.DEFAULT_LOGICAL_TYPES, false);
		final List<PluginDefinition> plugins = new ArrayList<>();
		plugins.add(new PluginDefinition("FUND_ID", "Fund Identifier", null, null, null, FUND_REGEXP,
				new PluginLocaleEntry[] { new PluginLocaleEntry("*", ".*(?i)(cusip).*", 90, FUND_REGEXP) }, false, 98, FTAType.STRING));

		try {
			analysis.getPlugins().registerPluginList(plugins, "FUND_ID", analysis.getConfig());
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getRegExp(), FUND_REGEXP);
		assertEquals(result.getTypeQualifier(), "FUND_ID");
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples) {
			assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testRegExpLogicalType_Bug() throws FTAException {
		final String EXPECTED_REGEXP = "\\p{IsAlphabetic}\\d{10}";
		final String[] samples = {
				"a1234567890", "a1234567890", "a1234567890", "a1234567890", "a1234567890", "a1234567890", "a1234567890", "a1234567890", "a1234567890", "a1234567890",
				"a1234567890", "a1234567890", "a1234567890", "a1234567890", "a1234567890", "a1234567890", "a1234567890", "a1234567890", "a1234567890", "a1234567890",
				"a1234567890", "a1234567890", "a1234567890", "a1234567890", "a1234567890", "a1234567890", "a1234567890", "a1234567890", "a1234567890", "a1234567890",
		};

		final TextAnalyzer analysis = new TextAnalyzer("NotMuch");
		final String pluginDefinitions =
				"[ {" +
				"  \"qualifier\": \"BUG\"," +
				"  \"pluginType\": \"regex\"," +
				"  \"validLocales\": [ { \"localeTag\":  \"*\", \"matchEntries\": [ { \"regExpReturned\": \"\\\\d{11}\" } ] } ]," +
				"  \"threshold\": 98," +
				"  \"baseType\": \"STRING\"" +
				"  } ]";


		try {
			analysis.getPlugins().registerPlugins(new StringReader(pluginDefinitions),
					analysis.getStreamName(), analysis.getConfig());
		} catch (Exception e) {
			System.err.println(e.getMessage());
			fail();
		}
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getRegExp(), EXPECTED_REGEXP);
		assertNull(result.getTypeQualifier());
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getConfidence(), 1.0);

		for (final String sample : samples) {
			assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicISO3166_2() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("3166 Alpha-2");
		final String inputs[] = TestUtils.valid3166_2.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{2}");
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "COUNTRY.ISO-3166-2");
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("COUNTRY.ISO-3166-2").signature);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getCardinality(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getMinLength(), 2);
		assertEquals(result.getMaxLength(), 2);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicCountry() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicCountry");
		final String pipedInput = "Venezuela|USA|Finland|USA|USA|Germany|France|Italy|Mexico|Germany|" +
				"Sweden|Germany|Sweden|Spain|Spain|Venezuela|Germany|Germany|Germany|Brazil|" +
				"Italy|UK|Brazil|Brazil|Brazil|Mexico|USA|France|Venezuela|France|" +
				"Ireland|Brazil|Italy|Germany|Belgium|Spain|Mexico|USA|Spain|USA|" +
				"Mexico|Ireland|USA|France|Germany|Germany|USA|UK|USA|USA|" +
				"UK|Mexico|Finland|UK|Mexico|Germany|USA|Germany|Spain|Sweden|" +
				"Portugal|USA|Venezuela|France|Canada|Finland|France|Ireland|Portugal|Germany|" +
				"USA|Canada|France|Denmark|Germany|Germany|USA|Germany|USA|Brazil|" +
				"Germany|USA|France|Austria|Portugal|Austria|Mexico|UK|Germany|Venezuela|" +
				"France|UK|France|Germany|France|Germany|UK|Mexico|Spain|Denmark|" +
				"Austria|USA|Switzerland|France|Brazil|Ireland|Poland|USA|Canada|UK|" +
				"Sweden|Brazil|Ireland|Venezuela|Austria|UK|Sweden|USA|Brazil|Norway|" +
				"UK|Canada|Austria|Germany|Austria|USA|USA|Venezuela|Germany|Portugal|" +
				"USA|Denmark|UK|USA|Austria|Austria|Italy|Venezuela|Brazil|Germany|" +
				"France|Argentina|Canada|Canada|Finland|France|Brazil|USA|Finland|Denmark|" +
				"Germany|Switzerland|Brazil|Brazil|Italy|Brazil|Canada|France|Spain|Austria|" +
				"Italy|Ireland|Austria|Canada|USA|Portugal|Sweden|UK|France|Finland|" +
				"Germany|Canada|USA|USA|Austria|Italy|Sweden|Sweden|Germany|Brazil|" +
				"Argentina|France|France|Germany|USA|UK|France|Finland|Germany|Germany|" +
				"Belgium|France|Sweden|Venezuela|UK|Belgium|Portugal|Denmark|Brazil|Italy|" +
				"Germany|USA|France|UK|UK|UK|Mexico|  Belgium  |Venezuela|Portugal|" +
				"France|USA|France|Brazil|USA|USA|UK|Venezuela|Venezuela|Brazil|" +
				"Germany|Austria|Venezuela|Portugal|Canada|France|Brazil|Canada|Brazil|Germany|" +
				"Venezuela|Venezuela|France|Germany|Mexico|Ireland|USA|Canada|Germany|Mexico|" +
				"Germany|Germany|USA|France|Brazil|Germany|Austria|Germany|Ireland|UK|Gondwanaland|";
		final String inputs[] = pipedInput.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), CountryEN.SEMANTIC_TYPE);
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("COUNTRY.TEXT_EN").signature);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getInvalidCount(), 1);
		final Map<String, Long> invalids = result.getInvalidDetails();
		assertEquals(invalids.size(), 1);
		final long invalidCount = invalids.get("GONDWANALAND");
		assertEquals(result.getMatchCount(), inputs.length - invalidCount);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "[\\p{IsAlphabetic}][-\\p{IsAlphabetic} '\\.(),]+");
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void constantLengthCountry() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("constantLengthCountry");
		final String[] inputs = {
				"ARUBA", "BENIN", "BURMA", "CHILE", "CHINA", "CONGO", "EGYPT", "FYROM", "GABON", "GHANA", "HAITI", "INDIA",
				"ITALY", "JAPAN", "KENYA", "KOREA", "LIBYA", "MACAU", "MALTA", "NAURU", "NEPAL", "NIGER", "PALAU", "QATAR",
				"SAMOA", "SPAIN", "SUDAN", "SYRIA", "TONGA", "WALES", "YEMEN"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), CountryEN.SEMANTIC_TYPE);
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("COUNTRY.TEXT_EN").signature);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "[\\p{IsAlphabetic}][-\\p{IsAlphabetic} '\\.(),]+");
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void thinAddress() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Example_Address");
		final String[] inputs = {
				"123 Test St",
				"124 Test St",
				"125 Test St",
				"126 Test St",
				"127 Test St",
				"128 Test St",
				"129 Test St",
				"130 Test St",
				"131 Test St"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), AddressEN.SEMANTIC_TYPE);
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("STREET_ADDRESS_EN").signature);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), ".+");
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void addressNonEnglish() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("адрес");
		analysis.setLocale(Locale.forLanguageTag("ru-RU"));
		final String[] inputs = {
				"432071, г.Ульяновск, ул.Гагарина,20",
				"432063, г.Ульяновск, ул.Гончарова, 15/27,17",
				"432034, г.Ульяновск, ул.Терешковой, д. 2",
				"432045, г. Ульяновск, ул. Промышленная, д.34",
				"432030, г.Ульяновск, пр-т Нариманова, д.99",
				"432071, г. Ульяновск ул. Белинского. 13/58",
				"432011, г.Ульяновск, ул.Рылеева, д.30/30",
				"432071, г.Ульяновск, ул.Рылеева, д.27",
				"432011, г.Ульяновск, ул.Радищева, д.42",
				"432011, г.Ульяновск, ул.Радищева, д.97",
				"432071, г.Ульяновск, ул.Орлова,21",
				"432002, г.Ульяновск, ул.Орлова д. 17",
				"432017, г.Ульяновск, ул.Кирова, д.4",
				"432044, г.Ульяновск, ул.Варейкиса, д.31",
				"432054, г.Ульяновск, ул.Камышинская,41",
				"432054, г.Ульяновск, ул.Камышинская,39",
				"432031, г.Ульяновск, пр-д Заводской, 30а",
				"432063, г.Ульяновск, ул.Карла Либкнехта,17",
				"432063, г.Ульяновск, ул.Гончарова, д.8/1",
				"433300, Ульяновская обл., Ульяновский район, г. Новоульяновск, ул. Ремесленная д.2",
				"433910, Ульяновская обл., Радищевский район, р.п.Радищево, улица Свердлова, д 24",
				"433360, Ульяновская обл., Тереньгульский район, р/п Тереньга ул. Степная, д.16",
				"432059, г. Ульяновск, б-р. Киевский, д.6-а",
				"432072, г.Ульяновск, пр-т Созидателей,11",
				"432064, г.Ульяновск, пр-т Врача Сурова,4",
				"432059, г.Ульяновск, пр-т Генерала Тюленева,6",
				"432072, г. Ульяновск, ул. Карбышева, д.6",
				"432067, г.Ульяновск, пр-т Генерала Тюленева,7",
				"432064, г.Ульяновск, пр-т Авиастроителей,5",
				"432072, г.Ульяновск, пр-т Авиастроителей, 31",
				"433321, г. Ульяновск, ул. Центральная д.13",
				"432010, г. Ульяновск, ул. Оренбургская, д.7А",
				"432057, г. Ульяновск, ул. Оренбургская, 27",
				"433795, г. Ульяновск ул. Кузнецова, д.26",
				"432071, г.Ульяновск, ул. Можайского 8/8",
				"432017 г. Ульяновск, ул. 3 Интернационала,1",
				"432063, г.Ульяновск, ул.Льва Толстого,28",
				"432063, г.Ульяновск,ул.III Интернационала д.7",
				"432017, г. Ульяновск, ул. 3 Интернационала, д.13",
				"г.Ульяновск, проспект Нариманова, д. 11",
				"432068, г.Ульяновск, ул. 12 Сентября, 90",
				"432017, г.Ульяновск, ул. 12 Сентября, 83",
				"432025 г.Ульяновск ул.Маяковского д.13",
				"432030, г Ульяновск, пр-кт Нариманова, 102",
				"432006, г.Ульяновск, ул.Локомотивная,13",
				"432049 г. Ульяновск ул. Пушкарева д.52А",
				"432049 г. Ульяновск, ул. Пушкарева, д. 29",
				"432005, г.Ульяновск ул.Пушкарева, д.6а",
				"432032, г. Ульяновск, ул. Полбина, д.34",
				"432980, г.Ульяновск, ул.Хрустальная 3а",
				"432012, г. Ульяновск, ул. Хрустальная, д.3",
				"432044, г.Ульяновск, ул.Хрустальная,3б",
				"432008, г.Ульяновск, пл.Горького,11а",
				"432026, г.Ульяновск, ул.Лихачева, д.12",
				"432042, г. Ульяновск, ул. Б. Хмельницкого, д.30",
				"432029, г. Ульяновск, ул. Корунковой, д. 21",
				"432064, г. Ульяновск, пр-т Врача Сурова, д. 4",
				"433223, Ульяновская область, Карсунский район, с. Сосновка, ул. Кооперативная, д. 38",
				"433610, Ульяновская обл., Цильнинский р- н, с.Большое Нагаткино, территория больницы, 11",
				"433700, Ульяновская обл., Базарносызганский район, р.п. Базарный Сызган, ул.Ульяновская, 2",
				"433752, Ульяновская обл., Барышский район, г. Барыш, ул. Аптечная 7",
				"433377, Ульяновская обл, Тереньгульский р-н, с.Солдатская Ташла",
				"433100, Ульяновская обл., Вешкаймский район. р.п.Вешкайма ул Больничная д 1",
				"432010, г.Ульяновск ул.Врача Михайлова д.35",
				"433512, Ульяновская обл., г.Димитровград, пр.Ленина, 30Б",
				"433400, Ульяновская обл., Чердаклинский район, р.п. Чердаклы, ул. Врача Попова, 1",
				"432071, г.Ульяновск, ул.Маяковского, д.13",
				"433016, Ульяновская область, Инзенский район, с.Юлово",
				"433970, Ульяновская обл., Павловский район, р.п. Павловка, ул. Калинина, 144",
				"433520, Ульяновская обл., Мелекесский район, с. Тиинск, ул. Больничная, д 10",
				"433720, Ульяновская обл., Барышский р-н, Приозерный пос.",
				"433310, Ульяновская обл., Ульяновский район, р.п. Ишеевка, ул. Больничная д.24",
				"433529, Ульяновская обл., Мелекесский район, п. Новоселки, ул. Гагарина, д 24",
				"433031, Ульяновская обл., г. Инза, ул. Пирогова, д.1",
				"433210, Ульяновская обл., Карсунский р-он, р.п. Карсун, ул. Саратовская, д 77",
				"433760, Ульяновская обл., Кузоватовский район, р.п. Кузоватово, ул. Гвардейская, д. 21",
				"433130, Ульяновская обл., р.п. Майна, ул. Зеленая д.1",
				"433551, Ульяновская обл., Мелекесский район, р.п. Мулловка, ул. Некрасова, 10",
				"433810, Ульяновская обл., Николаевский район, р.п. Николаевка, ул. Ульянова, д.21",
				"433534, Ульяновская обл., Мелекесский район, с. Никольское, ул.Мира, дом 101",
				"433555, Ульяновская обл., Мелекесский район, р.п. Новая Майна, ул. Комсомольская, 36",
				"433560, Ульяновская обл., Новомалыклинский район, с.Новая Малыкла, ул Кооперативная, д. 114",
				"433870, Ульяновская обл., Новоспасский район, пгт Новоспасское-пл.Семашко-10",
				"433545, Ульяновская обл., Мелекесский район, с. Рязаново, ул. Школьная, д. 15",
				"433380, Ульяновская обл., Сенгилеевский район г.Сенгилей ул.Нижневыборная 8",
				"433940, Ульяновская обл., Старокулаткинский район, р.п. Старая Кулатка, ул Больничная 21",
				"433460, Ульяновская обл., Старомайнский район, р.п. Старая Майна, ул. Сидорова, д 1",
				"433524, Ульяновская обл., Мелекесский район, с. Старая Сахча, ул. Кооперативная,д.28",
				"433240, Ульяновская обл., Сурский район, р.п.Сурское, ул. Октябрьская, 82"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), "STREET_ADDRESS_RU");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), ".+");
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicCountryHeader() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("BillingCountry");
		final String[] inputs = {
				"", "", "", "", "", "", "", "", "", "USA", "France", "USA",
				"", "", "", "", "US", "", "", "", "", "", "", "", "", "", "", "", "", ""
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), CountryEN.SEMANTIC_TYPE);
		assertEquals(result.getStructureSignature(),  PluginDefinition.findByQualifier("COUNTRY.TEXT_EN").signature);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), 4);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "[\\p{IsAlphabetic}][-\\p{IsAlphabetic} '\\.(),]+");
		assertEquals(result.getConfidence(), 1.0);
	}

	private final String validUSStreets2[] = {
			"6649 N Blue Gum St",
			"4 B Blue Ridge Blvd",
			"8 W Cerritos Ave #54",
			"639 Main St",
			"34 Center St",
			"3 Mcauley Dr",
			"7 Eads St",
			"7 W Jackson Blvd",
			"5 Boston Ave #88",
			"228 Runamuck Pl #2808",
			"2371 Jerrold Ave",
			"37275 St  Rt 17m M",
			"25 E 75th St #69",
			"98 Connecticut Ave Nw",
			"56 E Morehead St",
			"73 State Road 434 E",
			"69734 E Carrillo St",
			"322 New Horizon Blvd",
			"1 State Route 27",
			"394 Manchester Blvd",
			"6 S 33rd St",
			"6 Greenleaf Ave",
			"618 W Yakima Ave",
			"74 S Westgate St",
			"3273 State St",
			"1 Central Ave",
			"86 Nw 66th St #8673",
			"2 Cedar Ave #84",
			"90991 Thorburn Ave",
			"386 9th Ave N",
			"74874 Atlantic Ave",
			"366 South Dr",
			"45 E Liberty St",
			"4 Ralph Ct",
			"2742 Distribution Way",
			"426 Wolf St",
			"128 Bransten Rd",
			"17 Morena Blvd",
			"775 W 17th St",
			"6980 Dorsett Rd",
			"2881 Lewis Rd",
			"7219 Woodfield Rd",
			"1048 Main St",
			"678 3rd Ave",
			"20 S Babcock St",
			"2 Lighthouse Ave",
			"38938 Park Blvd",
			"5 Tomahawk Dr",
			"762 S Main St",
	};

	private final String validUSAddresses[] = {
			"9885 Princeton Court Shakopee, MN 55379",
			"11 San Pablo Rd.  Nottingham, MD 21236",
			"",
			"365 3rd St.  Woodhaven, NY 11421",
			"426 Brewery Street Horn Lake, MS 38637",
			"676 Thatcher St.  Hagerstown, MD 21740",
			"848 Hawthorne St.  Rockaway, NJ 07866",
			"788 West Coffee St.  Abingdon, MD 21009",
			"240 Arnold Avenue Yorktown Heights, NY 10598",
			"25 S. Hawthorne St.  Elizabeth City, NC 27909",
			"9314 Rose Street Holyoke, MA 01040",
			"32 West Bellevue St.  Holly Springs, NC 27540",
			"8168 Thomas Road El Dorado, AR 71730",
			"353 Homewood Ave.  Poughkeepsie, NY 12601",
			"14 North Cambridge Street Anchorage, AK 99504",
			"30 Leeton Ridge Drive Bristol, CT 06010",
			"8412 North Mulberry Dr.  Tiffin, OH 44883",
			"7691 Beacon Street Marysville, OH 43040",
			"187 Lake View Drive Redford, MI 48239",
			"318 Summerhouse Road Lenoir, NC 28645",
			"",
			"609 Taylor Ave.  Fort Myers, FL 33905",
			"47 Broad St.  Baldwin, NY 11510",
			"525 Valley View St.  Natick, MA 01760",
			"8 Greenview Ave.  Lithonia, GA 30038",
			"86 North Helen St.  Clermont, FL 34711",
			"8763 Virginia Street Hyattsville, MD 20782",
			"10 Front Avenue Brookline, MA 02446",
			"141 Blue Spring Street Ocoee, FL 34761",
			"99 W. Airport Ave.  Eau Claire, WI 54701",
			"32 NW. Rocky River Ave.  Raeford, NC 28376",
			"324 North Lancaster Dr.  Wyoming, MI 49509"
	};

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicUSStreet() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicUSStreet");

		for (final String s : TestUtils.validUSStreets) {
			analysis.train(s);
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), TestUtils.validUSStreets.length);
		assertEquals(result.getCardinality(), TestUtils.validUSStreets.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), AddressEN.SEMANTIC_TYPE);
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("STREET_ADDRESS_EN").signature);
		assertEquals(result.getRegExp(), ".+");
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicUSStreetTwo() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicUSStreetTwo");

		for (final String s : validUSStreets2) {
			analysis.train(s);
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), validUSStreets2.length);
		assertEquals(result.getCardinality(), validUSStreets2.length);
		assertEquals(result.getMatchCount(), validUSStreets2.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), AddressEN.SEMANTIC_TYPE);
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("STREET_ADDRESS_EN").signature);
		assertEquals(result.getRegExp(), ".+");
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicIBAN() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicIBAN");
		final String[] inputs = {
				"AD1400080001001234567890", "AT483200000012345864", "AZ96AZEJ00000000001234567890",
				"BH02CITI00001077181611", "BY86AKBB10100000002966000000", "BE71096123456769",
				"BA393385804800211234", "BR1500000000000010932840814P2", "BG18RZBB91550123456789",
				"CR23015108410026012345", "HR1723600001101234565", "CY21002001950000357001234567",
				"CZ5508000000001234567899", "DK9520000123456789", "DO22ACAU00000000000123456789",
				"EG800002000156789012345180002", "SV43ACAT00000000000000123123", "EE471000001020145685",
				"FO9264600123456789", "FI1410093000123458", "FR7630006000011234567890189",
				"GE60NB0000000123456789", "DE75512108001245126199", "GI04BARC000001234567890",
				"GR9608100010000001234567890", "GL8964710123456789", "GT20AGRO00000000001234567890",
				"VA59001123000012345678", "HU93116000060000000012345676", "IS750001121234563108962099",
				"IQ20CBIQ861800101010500", "IE64IRCE92050112345678", "IL170108000000012612345",
				"IT60X0542811101000000123456", "JO71CBJO0000000000001234567890", "KZ563190000012344567",
				"XK051212012345678906", "KW81CBKU0000000000001234560101", "LV97HABA0012345678910",
				"LB92000700000000123123456123", "LY38021001000000123456789", "LI7408806123456789012",
				"LT601010012345678901", "LU120010001234567891", "MT31MALT01100000000000000000123",
				"MR1300020001010000123456753", "MU43BOMM0101123456789101000MUR", "MD21EX000000000001234567",
				"MC5810096180790123456789085", "ME25505000012345678951", "NL02ABNA0123456789",
				"MK07200002785123453", "NO8330001234567", "PK36SCBL0000001123456702",
				"PS92PALS000000000400123456702", "PL10105000997603123456789123", "PT50002700000001234567833",
				"QA54QNBA000000000000693123456", "RO09BCYP0000001234567890", "LC14BOSL123456789012345678901234",
				"SM76P0854009812123456789123", "ST23000200000289355710148", "SA4420000001234567891234",
				"RS35105008123123123173", "SC52BAHL01031234567890123456USD", "SK8975000000000012345671",
				"SI56192001234567892", "ES7921000813610123456789", "SD8811123456789012",
				"SE7280000810340009783242", "CH5604835012345678009", "TL380010012345678910106",
				"TN5904018104004942712345", "TR320010009999901234567890", "UA903052992990004149123456789",
				"AE460090000000123456789", "GB33BUKB20201555555555", "VG21PACG0000000123456789" };

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getCardinality(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeQualifier(), CheckDigitIBAN.SEMANTIC_TYPE);
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("CHECKDIGIT.IBAN").signature);
		assertEquals(result.getConfidence(), 1.0);


		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicABA() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicABA");
		final String[] inputs = {
				"981140283", "989853459", "892328657", "781258896", "112551654", "438364101", "806651255", "095050162", "505993780", "827776957", "086820709", "609581894", "463724075",
				 "167622596", "355856417", "138265568", "479756862", "779880373", "750997751", "053438344", "199436608", "391657007", "033359472", "465043929", "977684902", "373527896"
		};

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getCardinality(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getTypeQualifier(), CheckDigitABA.SEMANTIC_TYPE);
		assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("CHECKDIGIT.ABA").signature);
		assertEquals(result.getConfidence(), 1.0);


		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}
}
