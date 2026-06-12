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
import java.util.Map;
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
		textAnalyzer.setLocale(Locale.US);
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

	final String[] inputsRE = {
            "2345:AQ", "5993:FG", "3898:WW", "5543:NH", "1992:WW", "4002:CS", "5982:KG", "1090:DD", "3030:XX", "1088:TR",
            "2547:DE", "6587:DS", "3215:QQ", "7745:VD", "4562:DD", "4582:SS", "2257:WE", "3578:HT", "4568:FB", "1587:SW",
            "4573:LF", "3574:SS", "8122:GK", "4523:EW", "7128:RT", "2548:RF", "6873:HH", "4837:NR", "2358:EE", "3731:HY",
            "0010:AA", "0011:DB", "0012:FT", "0013:KG", "0014:EM", "0015:RP", "0016:TA", "0017:AP", "0018:AA", "0019:AA",
            "0020:QA", "0021:UT", "0022:AA", "0023:AA", "0024:AQ", "0025:PA", "0026:AA", "0027:AA", "0028:AQ", "0029:AG",
            "0030:OA", "0031:AA", "0032:AA", "0033:AA", "0034:AA", "0035:NA", "0036:AI", "0037:AI", "0038:AA", "0039:FA",
            "0040:AH", "0041:AL", "0042:AL", "0043:EA", "0044:AA", "0045:WA", "0046:AA", "0047:QA", "0048:AS", "0049:XA",
            "0050:AS", "0051:ZA", "0052:AA", "0053:BS", "0054:JA", "0055:MA", "0056:AA", "0057:AV", "0058:AC", "0059:AA",
            "0060:AA", "0061:AC", "0062:AA", "0063:AD", "0064:AF", "0065:AA", "0066:AZ", "0067:AA", "0068:AK", "0069:AI",
            "0070:AA", "0071:AA", "0072:AA", "0073:AA", "0074:AF", "0075:AG", "0076:AY", "0077:AI", "0078:AU", "0079:AA"
    };

	final String[] primaryColors = {
		"Red", "Green", "Green", "Red", "Red", "Blue", "Green", "Green", "Green", "Blue",
		"Green", "Blue", "Blue", "Red", "Red", "Blue", "Green", "Blue", "Blue", "Red",
		"Blue", "Green", "Blue"
	};

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void issue155_serialize_pre() throws FTAPluginException, FTAUnsupportedLocaleException, FTAMergeException {
		// Load our new plugins from a file and test the new Regular Expression Semantic Type
		TextAnalyzer analysis = new TextAnalyzer("ID");
		analysis.setLocale(Locale.forLanguageTag("en-US"));

		// Register our sample list and regex plugins from a JSON definition file (before the built-in plugins have been registered)
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/custom_id.json"), StandardCharsets.UTF_8))) {
				analysis.getPlugins().registerPlugins(reader, analysis.getConfig(), true);
		} catch (FTAPluginException e) {
			System.err.println("ERROR: Failed to register plugin: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
		} catch (IOException e) {
			System.err.println("ERROR: Failed to register plugin: " + e.getMessage());
		}

		TextAnalyzer hydrated = TextAnalyzer.deserialize(analysis.serialize());

		for (final String input : inputsRE)
			hydrated.train(input);

		TextAnalysisResult result = hydrated.getResult();

		assertEquals(result.getSemanticType(), "CUSTOM.DIGIT_ALPHA_ID");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void issue155_serialize_color_pre() throws FTAPluginException, FTAUnsupportedLocaleException, FTAMergeException {
		// Load our new plugins from a file and test the new Regular Expression Semantic Type
		TextAnalyzer analysis = new TextAnalyzer("Color");
		analysis.setLocale(Locale.forLanguageTag("en-US"));

		// Register our sample list and regex plugins from a JSON definition file (before the built-in plugins have been registered)
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/custom_id.json"), StandardCharsets.UTF_8))) {
				analysis.getPlugins().registerPlugins(reader, analysis.getConfig(), true);
		} catch (FTAPluginException e) {
			System.err.println("ERROR: Failed to register plugin: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
		} catch (IOException e) {
			System.err.println("ERROR: Failed to register plugin: " + e.getMessage());
		}

		TextAnalyzer hydrated = TextAnalyzer.deserialize(analysis.serialize());

		for (final String input : primaryColors)
			hydrated.train(input);

		TextAnalysisResult result = hydrated.getResult();

		assertEquals(result.getSemanticType(), "CUSTOM.PRIMARY_COLOR");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void issue155_serialize_color_post() throws FTAPluginException, FTAUnsupportedLocaleException, FTAMergeException {
		// Load our new plugins from a file and test the new Regular Expression Semantic Type
		TextAnalyzer analysis = new TextAnalyzer("Color");
		analysis.setLocale(Locale.forLanguageTag("en-US"));

		// Register our sample list and regex plugins from a JSON definition file (before the built-in plugins have been registered)
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/custom_id.json"), StandardCharsets.UTF_8))) {
				analysis.getPlugins().registerPlugins(reader, analysis.getConfig(), false);
		} catch (FTAPluginException e) {
			System.err.println("ERROR: Failed to register plugin: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
		} catch (IOException e) {
			System.err.println("ERROR: Failed to register plugin: " + e.getMessage());
		}

		TextAnalyzer hydrated = TextAnalyzer.deserialize(analysis.serialize());

		for (final String input : primaryColors)
			hydrated.train(input);

		TextAnalysisResult result = hydrated.getResult();

		assertEquals(result.getSemanticType(), "COLOR.TEXT_EN");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void issue155_merge() throws FTAPluginException, FTAUnsupportedLocaleException, FTAMergeException {
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

		TextAnalyzer merged = TextAnalyzer.merge(TextAnalyzer.deserialize(shardOne.serialize()), TextAnalyzer.deserialize(shardTwo.serialize()));
		TextAnalysisResult result = merged.getResult();

		assertEquals(result.getSemanticType(), "CUSTOM.DIGIT_ALPHA_ID");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void issue161() throws FTAPluginException, FTAUnsupportedLocaleException, FTAMergeException {
		final String[] inputs = {
				"00", "01", "02", "03", "04", "05", "16", "17", "18", "19",
				"00", "01", "02", "03", "04", "05", "16", "17", "18", "19",
				"00", "01", "02", "03", "04", "05", "16", "17", "18", "19",
				"00", "01", "02", "03", "04", "05", "16", "17", "18", "19",
				"10"
	    };

		// Load our new plugins from a file and test the new Regular Expression Semantic Type
		TextAnalyzer analysis = new TextAnalyzer("ID");
		analysis.setDebug(2);
		analysis.setLocale(Locale.forLanguageTag("en-US"));

		// Register our sample list and regex plugins from a JSON definition file (before the built-in plugins have been registered)
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/problem.json"), StandardCharsets.UTF_8))) {
				analysis.getPlugins().registerPlugins(reader, analysis.getConfig(), true);
		} catch (FTAPluginException e) {
			System.err.println("ERROR: Failed to register plugin: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
		} catch (IOException e) {
			System.err.println("ERROR: Failed to register plugin: " + e.getMessage());
		}

		TextAnalyzer hydrated = TextAnalyzer.deserialize(analysis.serialize());

		for (final String input : inputs)
			hydrated.train(input);

		TextAnalysisResult result = hydrated.getResult();

		assertEquals(result.getSemanticType(), "PROBLEM");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void issue164() throws FTAException, IOException {
		// Custom Java plugin with baseType LOCALDATETIME registered as pre-builtin should
		// win over the built-in PERSON.DATE_OF_BIRTH plugin when the header matches.
		// Bug: the built-in gets detected instead of the custom plugin.
		final String fieldName = "BIRTHDAY";
		final TextAnalyzer analysis = new TextAnalyzer(fieldName, DateResolutionMode.Auto);
		analysis.setLocale(Locale.forLanguageTag("en-US"));

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				TestIssues.class.getResourceAsStream("/issue164.json"), StandardCharsets.UTF_8))) {
			analysis.getPlugins().registerPlugins(reader, analysis.getConfig(), true);
		}

		final String[] inputs = {
			"1990-05-15 08:23:42.123", "1985-11-03 14:07:55.456", "2000-01-30 23:59:00.000",
			"1972-07-19 06:45:11.789", "1968-03-28 12:30:00.001", "1995-09-01 00:00:00.000",
			"1980-12-25 18:15:33.321", "2003-06-14 09:08:07.654", "1977-02-09 21:44:59.999",
			"1993-08-22 16:00:00.500", "1988-04-17 07:30:45.100", "2001-10-31 11:11:11.111",
			"1965-01-01 03:03:03.003", "1970-07-04 22:22:22.222", "1998-11-11 15:55:55.555",
			"1983-03-03 04:04:04.444", "1975-09-09 13:13:13.130", "2005-05-05 05:05:05.050",
			"1992-02-29 10:10:10.010", "1987-06-30 19:59:59.959", "1963-12-12 08:08:08.080",
			"1978-08-08 17:17:17.170", "2002-04-01 06:06:06.060", "1969-10-10 20:20:20.200",
			"1994-01-15 23:45:00.000", "1982-07-07 12:00:00.120", "1974-03-21 09:09:09.009",
			"1999-09-19 18:18:18.018", "1966-11-30 07:07:07.007", "2004-06-06 16:16:16.016",
			"1991-02-14 14:14:14.140", "1979-08-24 11:11:11.010", "1984-04-04 03:33:33.033",
			"1973-10-20 22:00:00.002", "2006-12-01 05:55:05.500", "1997-01-09 08:48:48.480",
			"1986-07-16 17:37:37.370", "1971-05-25 13:53:53.530", "2007-03-13 06:26:26.260",
			"1996-11-22 20:02:02.020", "1989-09-29 09:39:39.390", "1964-06-18 15:15:15.150",
			"1976-04-11 21:21:21.210", "2008-02-20 04:44:44.440", "1981-10-03 10:10:00.100",
			"1967-08-27 19:49:49.490", "2009-05-08 07:27:27.270", "1985-01-23 16:56:56.560",
			"1990-03-16 23:33:33.330", "1961-07-31 12:42:42.420"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		// With preBuiltIns=true the custom plugin should win over PERSON.DATE_OF_BIRTH
		assertEquals(result.getSemanticType(), "BIRTHNEW.BIRTHDATE_TIME");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void issue166_reproduction() throws FTAException {
		final TextAnalyzer analyzer1 = new TextAnalyzer("test");
		final TextAnalyzer analyzer2 = new TextAnalyzer("test");

		for (int i = 0; i < 1000; i++) {
			analyzer1.train("Hello123");
			analyzer2.train("World456");
		}

		assertFalse(analyzer1.getResult().getShapeDetails().isEmpty(),
				"Shapes should be non-empty before merge");

		final TextAnalyzer merged = TextAnalyzer.merge(analyzer1, analyzer2);
		assertFalse(merged.getResult().getShapeDetails().isEmpty(),
				"Shapes should be non-empty after merge");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void issue166_shapeDetailsLostAfterSerialize() throws FTAException, IOException {
		// Bug: tokenStreams is not included in serialize(), so shapeDetails is empty after a
		// serialize/deserialize round-trip even though the same analyzer had non-empty shapes before.
		final int NAME_COL = 2;

		final List<String> allValues = new ArrayList<>();
		try (BufferedReader in = new BufferedReader(new InputStreamReader(
				TestIssues.class.getResourceAsStream("/testtable2.csv"), StandardCharsets.UTF_8))) {
			final CsvReader<NamedCsvRecord> csv = CsvReader.builder().ofNamedCsvRecord(in);
			for (final CloseableIterator<NamedCsvRecord> iter = csv.iterator(); iter.hasNext();)
				allValues.add(iter.next().getFields().get(NAME_COL));
		}

		final TextAnalyzer analyzer = new TextAnalyzer("NAME", DateResolutionMode.Auto);
		analyzer.setLocale(Locale.forLanguageTag("en-US"));
		for (final String value : allValues)
			analyzer.train(value);

		final Map<String, Long> shapesBeforeSerialize = analyzer.getResult().getShapeDetails();
		assertFalse(shapesBeforeSerialize.isEmpty(),
				"Shapes should be non-empty before serialize");

		// After a serialize/deserialize round-trip, tokenStreams is not restored
		final TextAnalyzer restored = TextAnalyzer.deserialize(analyzer.serialize());
		final Map<String, Long> shapesAfterSerialize = restored.getResult().getShapeDetails();
		// Bug: shapesAfterSerialize is empty because tokenStreams is absent from the serialized form
		assertEquals(shapesAfterSerialize, shapesBeforeSerialize,
				"Shapes should survive a serialize/deserialize round-trip");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.RANDOM })
	public void issue166_serializeDeserializeMergeShapes() throws FTAException, IOException {
		// Simulate a distributed workflow: each shard serializes its analyzer, ships it to the
		// driver, deserializes, and merges. Verify that shape counts survive the serde round-trip
		// on each shard and that the merged shapes match a direct (no-serde) merge.
		final String STREAM_NAME = "Confusing";

		final TextAnalyzer direct1 = new TextAnalyzer(STREAM_NAME);
		final TextAnalyzer direct2 = new TextAnalyzer(STREAM_NAME);
		final TextAnalyzer serde1  = new TextAnalyzer(STREAM_NAME);
		final TextAnalyzer serde2  = new TextAnalyzer(STREAM_NAME);

		for (final String resource : new String[] { "/p1.txt", "/p2.txt" }) {
			final boolean isP1 = resource.equals("/p1.txt");
			try (BufferedReader in = new BufferedReader(new InputStreamReader(
					TestIssues.class.getResourceAsStream(resource), StandardCharsets.UTF_8))) {
				String line;
				boolean header = true;
				while ((line = in.readLine()) != null) {
					if (header) { header = false; continue; }
					if (isP1) { direct1.train(line); serde1.train(line); }
					else       { direct2.train(line); serde2.train(line); }
				}
			}
		}

		// Serde each shard and verify per-shard shape counts survive the round-trip
		final TextAnalyzer restored1 = TextAnalyzer.deserialize(serde1.serialize());
		final TextAnalyzer restored2 = TextAnalyzer.deserialize(serde2.serialize());

		final Map<String, Long> direct1Shapes  = direct1.getResult().getShapeDetails();
		final Map<String, Long> restored1Shapes = restored1.getResult().getShapeDetails();
		final Map<String, Long> direct2Shapes  = direct2.getResult().getShapeDetails();
		final Map<String, Long> restored2Shapes = restored2.getResult().getShapeDetails();

		assertEquals(restored1Shapes.size(), direct1Shapes.size(),
				"p1 shape count should survive serialize/deserialize");
		assertEquals(restored2Shapes.size(), direct2Shapes.size(),
				"p2 shape count should survive serialize/deserialize");

		// Merge and compare distinct shapes against the direct (no-serde) merge
		final TextAnalyzer mergedDirect = TextAnalyzer.merge(direct1, direct2);
		final TextAnalyzer mergedSerde  = TextAnalyzer.merge(restored1, restored2);

		assertEquals(mergedSerde.getResult().getShapeDetails(), mergedDirect.getResult().getShapeDetails(),
				"Merged shapes after serde should match direct merge");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void issue159() throws IOException, FTAException {
		RecordAnalyzer analyzer = null;
		int rows = 0;

		try (BufferedReader in = new BufferedReader(new InputStreamReader(TestPlugins.class.getResourceAsStream("/lat.csv"), StandardCharsets.UTF_8))) {
			final CsvReader<NamedCsvRecord> csv = CsvReader.builder().ofNamedCsvRecord(in);

			for (final CloseableIterator<NamedCsvRecord> iter = csv.iterator(); iter.hasNext();) {
				final NamedCsvRecord rowRaw = iter.next();
				final String[] row = rowRaw.getFields().toArray(new String[0]);
				if (rows == 0) {
					final String[] header = rowRaw.getHeader().toArray(new String[0]);
					final AnalyzerContext context = new AnalyzerContext(null, DateTimeParser.DateResolutionMode.Auto, "profile", header);
					final TextAnalyzer template = new TextAnalyzer(context);
					template.setLocale(Locale.forLanguageTag("es-CO"));
					template.setDebug(2);
					analyzer = new RecordAnalyzer(template);
				}
				analyzer.train(row);
				rows++;
			}
		}

		final TextAnalysisResult coordinate = analyzer.getResult().getStreamResults()[0];
		assertEquals(coordinate.getSampleCount(), rows);
		assertEquals(coordinate.getType(), FTAType.STRING);
		assertEquals(coordinate.getSemanticType(), "COORDINATE.LONGITUDE_DECIMAL");
		assertNull(coordinate.checkCounts(false));
	}
}
