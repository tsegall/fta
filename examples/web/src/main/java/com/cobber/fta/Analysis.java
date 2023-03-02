package com.cobber.fta;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.web.multipart.MultipartFile;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAUnsupportedLocaleException;
import com.cobber.fta.core.Utils;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.RecordAnalyzer;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.TextAnalysisResult;

public class Analysis {

	public class FTAInfo {
		private String fieldName;
		private boolean isSemanticType;
		private String type;
		private String typeModifier;
		private String semanticType;
		private String maxValue;
		private String minValue;
		private String json;

		FTAInfo(TextAnalysisResult result) {
			this.fieldName = result.getName();
			this.isSemanticType = result.isSemanticType();
			this.type = result.getType().toString();
			this.typeModifier = result.getTypeModifier();
			this.semanticType = result.getSemanticType();
			this.maxValue = result.getMaxValue();
			this.minValue = result.getMinValue();
			this.json = result.asJSON(false, 0);
		}

		public String getFieldName() {
			return fieldName;
		}

		public String getIsSemanticType() {
			return isSemanticType ? "YES" : "NO";
		}

		public String getType() {
			return type;
		}

		public String getTypeModifier() {
			return typeModifier;
		}

		public String getSemanticType() {
			return isSemanticType ? semanticType : "";
		}

		public String getMaxValue() {
			return maxValue;
		}

		public String getMinValue() {
			return minValue;
		}

		public String getJSON() {
			return json;
		}
	}
	
	public Analysis(Locale locale) {
		if (locale != null)
			this.locale = locale.toLanguageTag();
	}

	private static String ftaVersion = Utils.getVersion();
	private MultipartFile file;
	private String locale;
	private int recordCount = 100;
	private List<FTAInfo> analysis = new ArrayList<>();

	public String getFile() {
		if (file == null)
			return null;
		return file.getOriginalFilename();
	}

	public void setFile(MultipartFile file) {
		try {
			this.file = file;

			try (BufferedReader in = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
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
				final AnalyzerContext context = new AnalyzerContext(null, DateResolutionMode.Auto, file.getOriginalFilename(), header);
				final TextAnalyzer template = new TextAnalyzer(context);
				if (locale != null)
					template.setLocale(Locale.forLanguageTag(locale));
				final RecordAnalyzer recordAnalyzer = new RecordAnalyzer(template);

				String[] row;
				int thisRecord = 0;

				while ((row = parser.parseNext()) != null && thisRecord < recordCount) {
					thisRecord++;
					if (row.length != header.length) {
						System.err.printf("ERROR: Record %d has %d fields, expected %d, skipping%n",
								thisRecord, row.length, header.length);
						continue;
					}
					recordAnalyzer.train(row);
				}

				final TextAnalysisResult[] results = recordAnalyzer.getResult().getStreamResults();
				for (TextAnalysisResult result : results)
					analysis.add(new FTAInfo(result));
			}
			catch (FTAPluginException e) {
				System.err.println("ERROR: FTAPluginException - " + e.getMessage());
			}
			catch (FTAUnsupportedLocaleException e) {
				System.err.println("ERROR: FTAUnsupportedLocaleException - " + e.getMessage());
			}
		}
		catch (IOException e) {
			System.err.println("ERROR: IOException - " + e.getMessage());
		}
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(final String locale) {
		this.locale = locale;
	}

	public int getRecordCount() {
		return recordCount;
	}

	public void setRecordCount(final int recordCount) {
		this.recordCount = recordCount;
	}

	public List<FTAInfo> getAnalysis() {
		return analysis;
	}

	public String getFtaVersion() {
		return ftaVersion;
	}
}
