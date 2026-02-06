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
package com.cobber.fta;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAMergeException;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.FTAUnsupportedLocaleException;
import com.cobber.fta.core.Utils;
import com.cobber.fta.dates.DateTimeParser;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

public class TestIssues {

	private List<String[]> asRecords(final String[] fieldValues) {
		final List<String[]> ret = new ArrayList<>();

		for (final String fieldValue : fieldValues)
			ret.add(new String[] { fieldValue });

		return ret;
	}

	@Test(groups = { TestGroups.ALL })
	public void issue48() throws FTAPluginException, FTAUnsupportedLocaleException {
		final String[] fieldnames = { "AddressLine2" };
		final String[][] values = {
				{ "MIDDLEBURY, CT 06762" }, { "DANVERS, MA 01923-3782" },
				{ "SAN JOSE, CA 95123-3696" }, { "JACKSONVILLE, FL 32202-1031" },
				{ "MORIARTY, NM 87035" }, { "ALEXANDRIA, MO 63430-9801" },
				{ "BROOKSHIRE, TX 77423-9440" }, { "CARROLL, IA 51401-9167" },
				{ "BUFFALO, NY 14223" }, { "HOUSTON, TX 77002-2526" } };

		final AnalyzerContext context = new AnalyzerContext(null, DateTimeParser.DateResolutionMode.Auto, "issue48",
				fieldnames);
		final TextAnalyzer textAnalyzer = new TextAnalyzer(context);
		textAnalyzer.setLocale(Locale.getDefault());
		final RecordAnalyzer analyzer = new RecordAnalyzer(textAnalyzer);

		for (final String[] value : values)
			analyzer.train(value);

		final TextAnalysisResult result = analyzer.getResult().getStreamResults()[0];
		assertEquals(result.getSemanticType(), "STREET_ADDRESS2_EN");
		// Header asserts with high confidence that this is a STREET ADDRESS 2, it is clearly not!
		// First entry is detected as a STREET ADDRESS 2 because CT looks like Court

		// TODO - this is broken
		//assertEquals(result.getInvalidCount(), 9);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void issue24() throws IOException, FTAException {
		final int LONGEST = 40;
		final String longBlank = Utils.repeat(' ', LONGEST);
		final int SHORTEST = 1;
		final String shortBlank = Utils.repeat(' ', SHORTEST);
		final String[] values = { "cmcfarlan13@aol.com", "cgorton14@dell.com", "kkorneichike@marriott.com",
				"alovattj@qq.com", "wwinterscalek@weibo.com", "cfugglel@pen.io.co.uk", "bsel&%odp@bloglovin.com",
				"gjoplingq@guardian.co.uk", "cvall$&owr@vkontakte.ru", "fpenas@bandcamp.com", "''", "NULL",
				"kkirsteiny@icio.us", "jgeistbeckz@shutterfly.com", "achansonne10@mac.com",
				"bpiotrkowski11#barnesandnoble.com", "jaikett15@netlog.com", "dattril17@phoca.cz",
				"abranchet18@psu.edu", "ddisley19@alexa.com", "vspriddle1a@japanpost.jp", "fdurbin1b@intel.com",
				"yedelheit1c@usda.gov", "msimacek1d@wikia.com", "rmessage1e@bizjournals.com",
				"hallenson1f@linkedin.com", "hrutley1g@phoca.cz", "kroakes1h@issuu.com", "msign1i@ocn.ne.jp",
				"hsiderfin1j@qq.com", "civakhin1k@sphinn.com", "abetty1l@yolasite.com", "lgussin1m@ft.com",
				"kfairleigh1n@ftc.gov", "kbrocklesby1o@tumblr.com", "nrands1p@google.com.br",
				"thattoe1q@washingtonpost.com", "vmadle1r@soup.io", "twhordley2c@addtoany.com",
				shortBlank, longBlank
		};

		final AnalyzerContext context = new AnalyzerContext(null, DateTimeParser.DateResolutionMode.Auto, "withBlanks",
				new String[] { "email" });
		final TextAnalyzer template = new TextAnalyzer(context);
		template.setLocale(Locale.getDefault());
		final RecordAnalyzer analyzer = new RecordAnalyzer(template);

		for (final String[] value : asRecords(values))
			analyzer.train(value);

		for (final TextAnalysisResult result : analyzer.getResult().getStreamResults()) {
			assertEquals(result.getSampleCount(), values.length);
			assertEquals(result.getMaxLength(), LONGEST);
			assertEquals(result.getMinLength(), SHORTEST);
			assertNull(result.checkCounts(false));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void issue25() throws IOException, FTAException {
		RecordAnalyzer analyzer = null;
		int rows = 0;

		try (BufferedReader in = new BufferedReader(new InputStreamReader(TestPlugins.class.getResourceAsStream("/addresses.csv"), StandardCharsets.UTF_8))) {
			final CsvReader<NamedCsvRecord> csv = CsvReader.builder().ofNamedCsvRecord(in);

			for (final CloseableIterator<NamedCsvRecord> iter = csv.iterator(); iter.hasNext();) {
				final NamedCsvRecord rowRaw = iter.next();
				final String[] row = rowRaw.getFields().toArray(new String[0]);
				if (rows == 0) {
					final String[] header = rowRaw.getHeader().toArray(new String[0]);
					final AnalyzerContext context = new AnalyzerContext(null, DateTimeParser.DateResolutionMode.Auto, "profile", header);
					final TextAnalyzer template = new TextAnalyzer(context);
					template.setLocale(Locale.forLanguageTag("en-US"));
					analyzer = new RecordAnalyzer(template);
				}
				analyzer.train(row);
				rows++;
			}
		}

		final TextAnalysisResult streetName = analyzer.getResult().getStreamResults()[0];
		assertEquals(streetName.getSampleCount(), rows);
		assertEquals(streetName.getType(), FTAType.STRING);
		assertEquals(streetName.getSemanticType(), "STREET_ADDRESS_EN");
		assertNull(streetName.checkCounts(false));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void enumCountMismatch() throws IOException, FTAException {
		final String[] values = {
				"Tungsten", "Silver", "Silver", "Silver", "Silver", "Silver", "Silver", "Silver", "Silver", "Tungsten",
				"Tungsten", "Tungsten", "Tungsten", "Silver", "Silver", "Tungsten", "Tungsten", "Tungsten", "Tungsten",
				"Tungsten", "Tungsten", "Gold", "Gold", "Gold", "Gold", "Gold", "Gold", "Gold", "Gold",
				"Gold", "Gold", "Gold", "Multi Mineral", "Multi Mineral", "Multi Mineral", "Multi Mineral", "Multi Mineral", "Multi Mineral", "Multi Mineral",
				"Multi Mineral", "Multi Mineral", "Multi Mineral", "Copper", "Copper", "Copper", "Copper", "Lithium", "Lithium", "Lithium",
				"Lithium", "Lithium", "Lithium", "Lead", "Lead", "Lead", "Aluminum", "Aluminum", "Aluminum", "Aluminum",
				"Aluminum", "Aluminum", "Aluminum", "Aluminum", "Aluminum", "Aluminum", "Lithium", "Lithium", "Lithium", "Lithium",
				"Lithium", "Lithium", "Lithium", "Lithium", "Bad Egg."
		};

		final AnalyzerContext context = new AnalyzerContext(null, DateTimeParser.DateResolutionMode.Auto, "enumCountMismatch",
				new String[] { "Mineral" });
		final TextAnalyzer template = new TextAnalyzer(context);
		template.setLocale(Locale.getDefault());
		final RecordAnalyzer analyzer = new RecordAnalyzer(template);

		for (final String[] value : asRecords(values))
			analyzer.train(value);

		for (final TextAnalysisResult result : analyzer.getResult().getStreamResults()) {
			assertEquals(result.getSampleCount(), values.length);
			assertEquals(result.getMatchCount(), values.length - 1);
			assertNull(result.checkCounts(false));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void miCountMismatch() throws IOException, FTAException {
		final String[] values = {
				"M", "H", "L", "M", "L", "E", "J", "R", "M", "C", "H", "N", "N", "S", "J", "J",
				"M", "M", "R", "T", "O", "R", "M", "D", "K", "JB", "G", "L", "D", "K", "W", "B",
				"C", "D", "K", "A", "S", "B", "J", "J", "R", "L", "D", "L", "T", "M", "R", "K",
				"R", "L", "R", "S", "L", "E", "G", "T", "S", "J", "J", "L", "Y", "L", "D", "R",
				"D", "L", "F", "F", "F", "T", "L", "A", "A", "R", "G", "T", "W", "J", "L", "S",
				"S", "R", "L", "M", "F", "M", "A", "G", "S", "M", "J", "A", "O", "R", "A", "E",
				"D", "W", "J", "A", "M", "T", "E", "O", "M", "M", "J", "C", "K", "J", "M", "N",
				"A", "C", "E", "L", "L", "A", "R", "L", "C", "H", "A", "L", "D", "W", "M", "A",
				"K", "J", "J", "T", "E", "D", "C", "J", "W", "A", "M", "P", "B", "J", "B", "D",
				"J", "C", "J", "L", "A", "W", "V", "G", "O", "R", "P", "K", "E", "T", "M", "D",
				"L", "D", "R", "A", "D", "R", "L", "D", "L", "C", "M", "N", "S", "W", "S", "C"
		};

		final AnalyzerContext context = new AnalyzerContext(null, DateTimeParser.DateResolutionMode.Auto, "miCountMismatch",
				new String[] { "MI" });
		final TextAnalyzer template = new TextAnalyzer(context);
		template.setLocale(Locale.forLanguageTag("en-US"));
		final RecordAnalyzer analyzer = new RecordAnalyzer(template);

		for (final String[] value : asRecords(values))
			analyzer.train(value);

		for (final TextAnalysisResult result : analyzer.getResult().getStreamResults()) {
			assertEquals(result.getSampleCount(), values.length);
			assertEquals(result.getMatchCount(), values.length - 1);
			assertNull(result.checkCounts(false));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void middleInitialCountMismatch() throws IOException, FTAException {
		final String[] values = {
				"J", "M", "L", "M", "M", "M", "M", "T", "R", "R", "P", "F", "A", "E", "T", "N", "A",
				"E", "A", "A", "R", "N", "F", "D", "L", "S", "E", "W", "E", "J", "V", "A", "A", "J",
				"M", "J", "S", "E", "G.", "T", "B", "M", "J", "K", "H", "B", "M", "T", "N", "G", "S",
				"J", "R", "D", "J", "H", "M", "M", "P", "A", "C", "T", "A", "E", "M", "Z", "D", "P",
				"M", "M", "C", "KEVIN", "A", "K", "H", "J.", "A", "R", "V", "M", "V", "L", "R", "MICHAEL",
				"N", "E", "N", "E", "J", "J", "L", "P", "J", "J", "R", "L", "L", "J", "A", "T", "P",
				"A", "A", "W", "M", "A", "D", "C", "V", "V", "E", "A", "A", "A", "P", "S", "E", "H",
				"M", "A", "A", "W", "M", "L", "A", "R", "M", "A", "R", "E", "J", "J", "M", "E", "S",
				"J", "M", "R", "J", "A", "D", "L", "T", "E", "W", "J", "J", "H", "DAVID", "W", "P",
				"L", "R", "C", "K", "M", "D", "A", "E", "A", "P", "D", "L", "H", "N", "J", "L", "A",
				"A", "T", "S", "C", "R", "LEE", "T", "M", "J", "C", "L", "L", "E", "P", "M", "J", "J",
				"E", "L", "V", "A", "P", "L", "W", "P", "J", "L", "H", "H", "F", "L", "J", "F", "J",
				"W", "J", "L", "M", "S", "T", "I", "A", "G", "A", "W", "C", "K", "C", "A", "N", "B",
				"L", "T", "T", "J", "M", "L", "P", "A", "B", "SZETO", "D", "M", "Q", "D", "M", "M",
				"M", "E", "E", "H", "C", "L", "D", "L", "L", "L", "R", "J", "A", "L", "A", "I", "M",
				"I", "R", "A", "J", "D", "L", "H", "J", "J", "P", "M", "M", "W", "J", "P", "J", "M",
				"L", "A", "M", "R", "A", "J", "L", "G", "J", "C.", "M", "A", "L", "D", "J", "E", "N",
				"F", "E", "L", "JUDAS", "J", "E", "C", "D", "M", "N", "A", "A", "W", "J", "T", "L",
				"R", "W", "R", "F", "C", "R", "N", "L", "L"
		};

		final AnalyzerContext context = new AnalyzerContext(null, DateTimeParser.DateResolutionMode.Auto, "middleInitialCountMismatch",
				new String[] { "middle_init" });
		final TextAnalyzer template = new TextAnalyzer(context);
		template.setDebug(2);
		template.setLocale(Locale.forLanguageTag("en-US"));
		final RecordAnalyzer analyzer = new RecordAnalyzer(template);

		for (final String[] value : asRecords(values))
			analyzer.train(value);

		for (final TextAnalysisResult result : analyzer.getResult().getStreamResults()) {
			assertEquals(result.getSampleCount(), values.length);
			assertNull(result.checkCounts(false));
			assertEquals(result.getMatchCount(), values.length - 6);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void numericalMismatch() throws IOException, FTAException {
		final String[] values = {
				"4", "4", "4", "4", "4", "4", "4", "4", "4", "4", "1", "1", "1", "1", "1", "1", "1",
				"1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1",
				"1", "1.1", "1.1", "1.1", "1.1", "1.1", "1.1", "1.1", "1.1", "1.1", "1.1", "1.1", "1.1",
				"1.1", "1.1", "1.1", "1.1", "1.1", "1.1", "1.1", "1.1", "1.1", "1.1", "1.1", "1.1", "1.1",
				"1.2", "1.2", "1.2", "1.2", "1.2", "1.2", "1.2", "1.2", "1.2", "1.2", "1.2", "1.2", "1.2",
				"1.2", "1.2", "1.2", "1.2", "1.2", "1.2", "1.2", "1.2", "1.2", "1.2", "1.2", "1.2", "2",
				"2", "2", "2", "2", "2", "2", "2", "2", "2", "2", "2", "2", "2", "2", "2", "2", "2", "2",
				"2", "2", "2", "2", "2", "2", "2.1", "2.1", "2.1", "2.1", "2.1", "2.1", "2.1", "2.1", "2.1",
				"2.1", "2.1", "2.1", "2.1", "2.1", "2.1", "2.1", "2.1", "2.1", "2.1", "2.1", "2.1", "2.1",
				"2.1", "2.1", "2.1", "3", "3", "3", "3", "3", "3", "3", "3", "3", "3", "3", "3", "3", "3",
				"3", "3", "3", "3", "3", "3", "3", "3", "3", "3", "3", "3.1", "3.1", "3.1", "3.1", "3.1",
				"3.1", "3.1", "3.1", "3.1", "3.1", "3.1", "3.1", "3.1", "3.1", "4", "4", "4", "4", "4",
				"4", "4", "4", "4", "4", "4", "4", "4", "4", "4", "4", "2.2"
		};

		final AnalyzerContext context = new AnalyzerContext(null, DateTimeParser.DateResolutionMode.Auto, "numericalMismatch",
				new String[] { "age_num" });
		final TextAnalyzer template = new TextAnalyzer(context);
		template.setLocale(Locale.getDefault());
		final RecordAnalyzer analyzer = new RecordAnalyzer(template);

		for (final String[] value : asRecords(values))
			analyzer.train(value);

		for (final TextAnalysisResult result : analyzer.getResult().getStreamResults()) {
			assertEquals(result.getSampleCount(), values.length);
			assertEquals(result.getMatchCount(), values.length);
			assertNull(result.checkCounts(false));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void pickup() throws IOException, FTAException {
		RecordAnalyzer analyzer = null;
		int rows = 0;

		try (BufferedReader in = new BufferedReader(new InputStreamReader(TestPlugins.class.getResourceAsStream("/pickup.csv"), StandardCharsets.UTF_8))) {
			final CsvReader<NamedCsvRecord> csv = CsvReader.builder().ofNamedCsvRecord(in);

			for (final CloseableIterator<NamedCsvRecord> iter = csv.iterator(); iter.hasNext();) {
				final NamedCsvRecord rowRaw = iter.next();
				final String[] row = rowRaw.getFields().toArray(new String[0]);
				if (rows == 0) {
					final String[] header = rowRaw.getHeader().toArray(new String[0]);
					final AnalyzerContext context = new AnalyzerContext(null, DateTimeParser.DateResolutionMode.Auto, "profile", header);
					final TextAnalyzer template = new TextAnalyzer(context);
					template.setLocale(Locale.forLanguageTag("en-US"));
					analyzer = new RecordAnalyzer(template);
				}
				analyzer.train(row);
				rows++;
			}
		}

		final TextAnalysisResult streetName = analyzer.getResult().getStreamResults()[1];
		assertEquals(streetName.getSampleCount(), rows);
		assertEquals(streetName.getType(), FTAType.STRING);
		assertEquals(streetName.getSemanticType(), "STREET_NAME_EN");
		assertNull(streetName.checkCounts(false));

		final TextAnalysisResult streetNumber = analyzer.getResult().getStreamResults()[0];
		assertEquals(streetNumber.getSampleCount(), rows);
		assertEquals(streetNumber.getType(), FTAType.LONG);
		assertEquals(streetNumber.getSemanticType(), "STREET_NUMBER");
		assertNull(streetNumber.checkCounts(false));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void issue70() throws FTAPluginException, FTAUnsupportedLocaleException {

		final TextAnalyzer analyzer = new TextAnalyzer("foo", DateResolutionMode.Auto);

		analyzer.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);
		analyzer.train("");
		analyzer.train("");

		final TextAnalysisResult result = analyzer.getResult();
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getBlankCount(), 2);
		assertEquals(result.getMinLength(), 0);
		assertEquals(result.getMaxLength(), 0);
		assertEquals(result.getTypeModifier(), "BLANK");
	 }

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void issue71() throws FTAPluginException, FTAUnsupportedLocaleException {
		final String[] headers = { "First", "Last", "MI" };
		final String[][] names = { { "Anaïs", "Nin", "9,876.54" }, { "Gertrude", "Stein", "3,876.2" },
				{ "Paul", "Campbell", "76.54" }, { "Pablo", "Picasso", "123.45" } };

		final AnalyzerContext context = new AnalyzerContext(null, DateResolutionMode.Auto, "customer", headers);
		final TextAnalyzer template = new TextAnalyzer(context);
		template.setDebug(2);

		template.setLocale(Locale.GERMAN);

		final RecordAnalyzer analysis = new RecordAnalyzer(template);

		for (final String[] name : names)
			analysis.train(name);

		final RecordAnalysisResult recordResult = analysis.getResult();

		final TextAnalysisResult[] results = recordResult.getStreamResults();
		assertEquals(results[0].getSemanticType(), "NAME.FIRST");
		assertNull(results[2].getSemanticType());
		assertEquals(results[2].getType(), FTAType.DOUBLE);
		assertEquals(results[2].getTypeModifier(), "NON_LOCALIZED");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void checkJSON() throws IOException, FTAException {
		// Issue #120
		final String[] headers = { "First", "Last", "MI" };
		final String[][] names = { { "Anaïs", "Nin", "9,876.54" }, { "Gertrude", "Stein", "3,876.2" },
				{ "Paul", "Campbell", "76.54" }, { "Pablo", "Picasso", "123.45" } };

		final AnalyzerContext context = new AnalyzerContext(null, DateResolutionMode.Auto, "customer", headers);
		final TextAnalyzer template = new TextAnalyzer(context);

		template.setLocale(Locale.GERMAN);

		final RecordAnalyzer analysis = new RecordAnalyzer(template);

		for (final String[] name : names)
			analysis.train(name);

		final RecordAnalysisResult recordResult = analysis.getResult();
		final TextAnalysisResult[] results = recordResult.getStreamResults();

		final String jsonRepresentation = results[0].asJSON(false, 0);

		final ObjectMapper objectMapper = new ObjectMapper();
		final JsonNode jsonNode = objectMapper.readTree(jsonRepresentation);

		// Validate that the JSON representation has all the fields we expect - with the values we expect
		assertTrue(jsonNode.get("isSemanticType").asBoolean());
		assertEquals(jsonNode.get("semanticType").asText(), "NAME.FIRST");

		assertEquals(jsonNode.get("fieldName").asText(), "First");
		assertEquals(jsonNode.get("sampleCount").asInt(), 4);
		assertEquals(jsonNode.get("matchCount").asInt(), 4);
		assertEquals(jsonNode.get("nullCount").asInt(), 0);
		assertEquals(jsonNode.get("blankCount").asInt(), 0);
		assertEquals(jsonNode.get("distinctCount").asInt(), 4);
		assertEquals(jsonNode.get("confidence").asDouble(), 1.0);
		assertEquals(jsonNode.get("type").asText(), "String");
		assertEquals(jsonNode.get("min").asText(), "Anaïs");
		assertEquals(jsonNode.get("max").asText(), "Paul");
		assertEquals(jsonNode.get("minLength").asInt(), 4);
		assertEquals(jsonNode.get("maxLength").asInt(), 8);
		assertEquals(jsonNode.get("min").asText(), "Anaïs");
		assertEquals(jsonNode.get("cardinality").asInt(), 4);
		assertEquals(jsonNode.get("outlierCardinality").asInt(), 0);
		assertEquals(jsonNode.get("invalidCardinality").asInt(), 0);
		assertEquals(jsonNode.get("shapesCardinality").asInt(), 3);
		assertFalse(jsonNode.get("leadingWhiteSpace").asBoolean());
		assertFalse(jsonNode.get("trailingWhiteSpace").asBoolean());
		assertFalse(jsonNode.get("multiline").asBoolean());
		assertEquals(jsonNode.get("keyConfidence").asDouble(), 0.0);
		assertEquals(jsonNode.get("uniqueness").asDouble(), 1.0);
		assertEquals(jsonNode.get("detectionLocale").asText(), "de");
		assertEquals(jsonNode.get("structureSignature").asText(), "slggsAEDZ26rz9dqs15eNF23j2w=");
		assertEquals(jsonNode.get("dataSignature").asText(), "hOm2Ez8xHWr6iDeQ1j/A3hBtz0Y=");
	}

	final String[] inputsRE = {
            "2345:AQ", "5993:FG", "3898:WW", "5543:NH", "1992:WW", "4002:CS", "5982:KG", "1090:DD", "3030:XX", "1088:TR",
            "2547:DE", "6587:DS", "3215:QQ", "7745:VD", "4562:DD", "4582:SS", "2257:WE", "3578:HT", "4568:FB", "1587:SW",
            "4573:LF", "3574:SS", "8122:GK", "4523:EW", "7128:RT", "2548:RF", "6873:HH", "4837:NR", "2358:EE", "3731:HY"
    };

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void issue155() throws FTAPluginException, FTAUnsupportedLocaleException, FTAMergeException {
		// Load our new plugins from a file and test the new Regular Expression Semantic Type
		TextAnalyzer shardOne = new TextAnalyzer("ID");
		shardOne.setLocale(Locale.forLanguageTag("en-US"));
		TextAnalyzer shardTwo = new TextAnalyzer("ID");
		shardTwo.setLocale(Locale.forLanguageTag("en-US"));

		// Register our sample list and regex plugins from a JSON definition file (before the built-in plugins have been registered)
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/custom_id.json"), StandardCharsets.UTF_8))) {
				shardOne.getPlugins().registerPlugins(reader, shardOne.getConfig(), true);
		} catch (FTAPluginException e) {
			System.err.println("ERROR: Failed to register plugin: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
		} catch (IOException e) {
			System.err.println("ERROR: Failed to register plugin: " + e.getMessage());
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/custom_id.json"), StandardCharsets.UTF_8))) {
			shardTwo.getPlugins().registerPlugins(reader, shardTwo.getConfig(), true);
		} catch (FTAPluginException e) {
			System.err.println("ERROR: Failed to register plugin: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
		} catch (IOException e) {
			System.err.println("ERROR: Failed to register plugin: " + e.getMessage());
		}

		for (final String input : inputsRE) {
			shardOne.train(input);
			shardTwo.train(input);
		}

		TextAnalyzer merged = TextAnalyzer.merge(shardOne, shardTwo);
		TextAnalysisResult result = merged.getResult();

		assertEquals(result.getSemanticType(), "CUSTOM.DIGIT_ALPHA_ID");
	}
}
