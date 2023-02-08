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
		Locale locale = null;
		boolean verbose = false;
		int idx = 0;

		while (idx < args.length && args[idx].charAt(0) == '-') {
			if ("--help".equals(args[idx])) {
				System.err.println("Usage: cli [OPTIONS] file ...");
				System.err.println("Valid OPTIONS are:");
				System.err.println(" --help - Print this help");
				System.err.println(" --locale <LocaleIdentifier> - Locale to use as opposed to default");
				System.err.println(" --verbose - Dump JSON details");
			}
			else if ("--locale".equals(args[idx])) {
				String tag = args[++idx];
				locale = Locale.forLanguageTag(tag);
				if (!locale.toLanguageTag().equals(tag)) {
					System.err.printf("ERROR: Language tag '%s' not known - using '%s'?%n", tag, locale.toLanguageTag());
					System.exit(1);
				}
			}
			else if ("--verbose".equals(args[idx])) {
				verbose = true;
			}
			idx++;
		}

		if (idx == args.length) {
			System.err.println("ERROR: No file to process supplied, use --help");
			System.exit(1);
		}

		String source = args[idx];

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
				template.setLocale(locale);
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
				if (verbose)
					System.err.printf("Field: '%s':\n%s\n", result.getName(), result.asJSON(true, 0));
				else
					System.err.printf("Field: '%s', IsSemanticType?: %b, Type: '%s', TypeModifier: '%s', SemanticType: '%s', Confidence: %.2f, Max: '%s', Min: '%s'\n",
						result.getName(), result.isSemanticType(), result.getType().toString(), result.getTypeModifier(), result.getSemanticType(), result.getConfidence(), result.getMaxValue(), result.getMinValue());
			}
		}
	}
}
