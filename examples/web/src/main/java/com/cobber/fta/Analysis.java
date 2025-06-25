package com.cobber.fta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.web.multipart.MultipartFile;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAUnsupportedLocaleException;
import com.cobber.fta.core.Utils;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

public class Analysis {

	public class FTAInfo {
		private final String fieldName;
		private final boolean isSemanticType;
		private final String type;
		private final String typeModifier;
		private final String semanticType;
		private final String maxValue;
		private final String minValue;
		private final String json;

		FTAInfo(final TextAnalysisResult result) {
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

	public Analysis(final Locale locale) {
		if (locale != null)
			this.locale = locale.toLanguageTag();

		allTypes = SemanticType.getActiveSemanticTypes(locale);
	}

	private static String ftaVersion = Utils.getVersion();
	private MultipartFile file;
	private String locale;
	private int recordCount = 100;
	private final List<FTAInfo> analysisResult = new ArrayList<>();
	private final List<SemanticType> allTypes;

	public String getFile() {
		if (file == null)
			return null;
		return file.getOriginalFilename();
	}

	public void setFile(final MultipartFile file) {
		try {
			this.file = file;

			try (BufferedReader in = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
				final CsvReader<NamedCsvRecord> csv = CsvReader.builder().ofNamedCsvRecord(in);
				RecordAnalyzer recordAnalyzer = null;
				String[] header = null;
				int thisRecord = 0;

				for (final CloseableIterator<NamedCsvRecord> iter = csv.iterator(); thisRecord < recordCount && iter.hasNext();) {
					final NamedCsvRecord rowRaw = iter.next();
					if (thisRecord == 0) {
						header = rowRaw.getHeader().toArray(new String[0]);
						final AnalyzerContext context = new AnalyzerContext(null, DateResolutionMode.Auto, file.getOriginalFilename(), header);
						final TextAnalyzer template = new TextAnalyzer(context);
						if (locale != null)
							template.setLocale(Locale.forLanguageTag(locale));
						recordAnalyzer = new RecordAnalyzer(template);
					}
					thisRecord++;
					if (rowRaw.getFieldCount() != header.length) {
						System.err.printf("ERROR: Record %d has %d fields, expected %d, skipping%n",
								thisRecord, rowRaw.getFieldCount(), header.length);
						continue;
					}
					recordAnalyzer.train(rowRaw.getFields().toArray(new String[0]));
				}

				final TextAnalysisResult[] results = recordAnalyzer.getResult().getStreamResults();
				for (final TextAnalysisResult result : results)
					analysisResult.add(new FTAInfo(result));
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

	public List<FTAInfo> getAnalysisResult() {
		return analysisResult;
	}

	public String getFtaVersion() {
		return ftaVersion;
	}

	public List<SemanticType> getAllTypes() {
		return allTypes;
	}
}
