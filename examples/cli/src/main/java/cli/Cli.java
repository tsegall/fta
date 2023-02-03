package cli;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Locale;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.RecordAnalyzer;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.TextAnalysisResult;

public abstract class Cli {
	public static void main(final String[] args) throws FTAException, IOException {
		if (args.length == 0) {
			System.err.println("Usage: cli <CSV File>");
			System.exit(1);
		}

		String source = args[0];
		String locale = "en-US";

		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(source), "UTF-8"))) {
			final CsvParserSettings settings = new CsvParserSettings();
			settings.setHeaderExtractionEnabled(true);
			settings.detectFormatAutomatically();
			settings.setLineSeparatorDetectionEnabled(true);
			settings.setIgnoreLeadingWhitespaces(false);
			settings.setIgnoreTrailingWhitespaces(false);
			settings.setEmptyValue("");
			final CsvParser parser = new CsvParser(settings);
			parser.beginParsing(in);
			String[] header = parser.getRecordMetadata().headers();
			final AnalyzerContext context = new AnalyzerContext(null, DateResolutionMode.Auto, source, header);
			final TextAnalyzer template = new TextAnalyzer(context);
			if (locale != null)
				template.setLocale(Locale.forLanguageTag(locale));
			final RecordAnalyzer recordAnalyzer = new RecordAnalyzer(template);

			String[] row;
			int thisRecord = 0;

			while ((row = parser.parseNext()) != null) {
				thisRecord++;
				if (row.length != header.length) {
					System.err.printf("ERROR: Record %d has %d fields, expected %d, skipping%n",
							thisRecord, row.length, header.length);
					continue;
				}
				recordAnalyzer.train(row);
			}

			final TextAnalysisResult[] results = recordAnalyzer.getResult().getStreamResults();
			StringBuilder ret = new StringBuilder();
			for (TextAnalysisResult result : results) {
				System.err.printf("Field: '%s', IsSemanticType?: %b, Type: '%s', TypeModifier: '%s', SemanticType: '%s', Max: '%s', Min: '%s'\n",
					result.getName(), result.isSemanticType(), result.getType().toString(), result.getTypeModifier(), result.getSemanticType(), result.getMaxValue(), result.getMinValue());
			}
		}
	}
}
