package com.cobber.fta;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.Utils;
import com.cobber.fta.dates.DateTimeParser;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

public class TestIssues {

	private List<String[]> asRecords(final String[] fieldValues) {
		final List<String[]> ret = new ArrayList<>();

		for (final String fieldValue : fieldValues)
			ret.add(new String[] { fieldValue });

		return ret;
	}

	@Test
	public void issue48() throws Exception {
		final String[] fieldnames = { "AddressLine2" };
		final String[][] values = new String[][] {
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
		assertEquals(result.getInvalidCount(), 9);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void issue24() throws IOException, FTAException {
		final int LONGEST = 40;
		final String longBlank = Utils.repeat(' ', LONGEST);
		final int SHORTEST = 1;
		final String shortBlank = Utils.repeat(' ', SHORTEST);
		final String[] values = { "", "cmcfarlan13@aol.com", "cgorton14@dell.com", "kkorneichike@marriott.com",
				"alovattj@qq.com", "wwinterscalek@weibo.com", "cfugglel@pen.io.co.uk", "bsel&%odp@bloglovin.com",
				"gjoplingq@guardian.co.uk", "cvall$&owr@vkontakte.ru", "fpenas@bandcamp.com", "''", "NULL", "",
				"kkirsteiny@icio.us", "jgeistbeckz@shutterfly.com", "achansonne10@mac.com",
				"bpiotrkowski11#barnesandnoble.com", "jaikett15@netlog.com", "dattril17@phoca.cz",
				"abranchet18@psu.edu", "ddisley19@alexa.com", "vspriddle1a@japanpost.jp", "fdurbin1b@intel.com",
				"yedelheit1c@usda.gov", "msimacek1d@wikia.com", "rmessage1e@bizjournals.com",
				"hallenson1f@linkedin.com", "hrutley1g@phoca.cz", "kroakes1h@issuu.com", "msign1i@ocn.ne.jp",
				"hsiderfin1j@qq.com", "civakhin1k@sphinn.com", "abetty1l@yolasite.com", "lgussin1m@ft.com",
				"kfairleigh1n@ftc.gov", "kbrocklesby1o@tumblr.com", "nrands1p@google.com.br",
				"thattoe1q@washingtonpost.com", "vmadle1r@soup.io", "", "twhordley2c@addtoany.com",
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
			assertNull(result.checkCounts());
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void issue25() throws IOException, FTAException {
		final CsvParserSettings settings = new CsvParserSettings();
		settings.setHeaderExtractionEnabled(true);
		RecordAnalyzer analyzer;
		int rows = 0;

		try (BufferedReader in = new BufferedReader(new InputStreamReader(TestPlugins.class.getResourceAsStream("/addresses.csv"), StandardCharsets.UTF_8))) {

			final CsvParser parser = new CsvParser(settings);
			parser.beginParsing(in);

			final String[] header = parser.getRecordMetadata().headers();
			final AnalyzerContext context = new AnalyzerContext(null, DateTimeParser.DateResolutionMode.Auto, "pickup", header);
			final TextAnalyzer template = new TextAnalyzer(context);
			template.setLocale(Locale.forLanguageTag("en-US"));
			analyzer = new RecordAnalyzer(template);

			String[] row;
			while ((row = parser.parseNext()) != null) {
					analyzer.train(row);
					rows++;
			}
		}

		final TextAnalysisResult streetName = analyzer.getResult().getStreamResults()[0];
		assertEquals(streetName.getSampleCount(), rows);
		assertEquals(streetName.getType(), FTAType.STRING);
		assertEquals(streetName.getSemanticType(), "STREET_ADDRESS_EN");
		assertNull(streetName.checkCounts());
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
			assertNull(result.checkCounts());
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
			assertNull(result.checkCounts());
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
		template.setLocale(Locale.forLanguageTag("en-US"));
		final RecordAnalyzer analyzer = new RecordAnalyzer(template);

		for (final String[] value : asRecords(values))
			analyzer.train(value);

		for (final TextAnalysisResult result : analyzer.getResult().getStreamResults()) {
			assertEquals(result.getSampleCount(), values.length);
			assertNull(result.checkCounts());
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
			assertNull(result.checkCounts());
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void pickup() throws IOException, FTAException {
		final CsvParserSettings settings = new CsvParserSettings();
		settings.setHeaderExtractionEnabled(true);
		RecordAnalyzer analyzer;
		int rows = 0;

		try (BufferedReader in = new BufferedReader(new InputStreamReader(TestPlugins.class.getResourceAsStream("/pickup.csv"), StandardCharsets.UTF_8))) {

			final CsvParser parser = new CsvParser(settings);
			parser.beginParsing(in);

			final String[] header = parser.getRecordMetadata().headers();
			final AnalyzerContext context = new AnalyzerContext(null, DateTimeParser.DateResolutionMode.Auto, "pickup", header);
			final TextAnalyzer template = new TextAnalyzer(context);
			template.setLocale(Locale.forLanguageTag("en-US"));
			analyzer = new RecordAnalyzer(template);

			String[] row;
			while ((row = parser.parseNext()) != null) {
					analyzer.train(row);
					rows++;
			}
		}

		final TextAnalysisResult streetName = analyzer.getResult().getStreamResults()[1];
		assertEquals(streetName.getSampleCount(), rows);
		assertEquals(streetName.getType(), FTAType.STRING);
		assertEquals(streetName.getSemanticType(), "STREET_NAME_EN");
		assertNull(streetName.checkCounts());

		final TextAnalysisResult streetNumber = analyzer.getResult().getStreamResults()[0];
		assertEquals(streetNumber.getSampleCount(), rows);
		assertEquals(streetNumber.getType(), FTAType.LONG);
		assertEquals(streetNumber.getSemanticType(), "STREET_NUMBER");
		assertNull(streetNumber.checkCounts());
	}
}
