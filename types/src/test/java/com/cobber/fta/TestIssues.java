package com.cobber.fta;

import static org.testng.Assert.assertEquals;
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
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.Utils;
import com.cobber.fta.dates.DateTimeParser;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

public class TestIssues {

	private List<String[]> asRecords(String[] fieldValues) {
		ArrayList<String[]> ret = new ArrayList<>();

		for (String fieldValue : fieldValues)
			ret.add(new String[] { fieldValue });

		return ret;
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

		AnalyzerContext context = new AnalyzerContext(null, DateTimeParser.DateResolutionMode.Auto, "withBlanks",
				new String[] { "email" });
		TextAnalyzer template = new TextAnalyzer(context);
		template.setLocale(Locale.getDefault());
		RecordAnalyzer analyzer = new RecordAnalyzer(template);

		for (String[] value : asRecords(values))
			analyzer.train(value);

		for (TextAnalysisResult result : analyzer.getResult().getStreamResults()) {
			assertEquals(result.getSampleCount(), values.length);
			assertEquals(result.getMaxLength(), LONGEST);
			assertEquals(result.getMinLength(), SHORTEST);
			assertTrue(result.checkCounts());
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

			String[] header = parser.getRecordMetadata().headers();
			AnalyzerContext context = new AnalyzerContext(null, DateTimeParser.DateResolutionMode.Auto, "pickup", header);
			TextAnalyzer template = new TextAnalyzer(context);
			analyzer = new RecordAnalyzer(template);

			String[] row;
			while ((row = parser.parseNext()) != null) {
					analyzer.train(row);
					rows++;
			}
		}

		TextAnalysisResult streetName = analyzer.getResult().getStreamResults()[0];
		assertEquals(streetName.getSampleCount(), rows);
		assertEquals(streetName.getType(), FTAType.STRING);
		assertEquals(streetName.getSemanticType(), "STREET_ADDRESS_EN");
		assertTrue(streetName.checkCounts());
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

			String[] header = parser.getRecordMetadata().headers();
			AnalyzerContext context = new AnalyzerContext(null, DateTimeParser.DateResolutionMode.Auto, "pickup", header);
			TextAnalyzer template = new TextAnalyzer(context);
			analyzer = new RecordAnalyzer(template);

			String[] row;
			while ((row = parser.parseNext()) != null) {
					analyzer.train(row);
					rows++;
			}
		}

		TextAnalysisResult streetName = analyzer.getResult().getStreamResults()[1];
		assertEquals(streetName.getSampleCount(), rows);
		assertEquals(streetName.getType(), FTAType.STRING);
		assertEquals(streetName.getSemanticType(), "STREET_NAME_EN");
		assertTrue(streetName.checkCounts());

		TextAnalysisResult streetNumber = analyzer.getResult().getStreamResults()[0];
		assertEquals(streetNumber.getSampleCount(), rows);
		assertEquals(streetNumber.getType(), FTAType.LONG);
		assertEquals(streetNumber.getSemanticType(), "STREET_NUMBER");
		assertTrue(streetNumber.checkCounts());
	}
}
