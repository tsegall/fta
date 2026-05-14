package com.cobber.fta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAUnsupportedLocaleException;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;
import com.cobber.fta.dto.AnalysisResponse;
import com.cobber.fta.dto.FieldResult;
import com.cobber.fta.dto.SemanticTypeInfo;
import tools.jackson.databind.ObjectMapper;

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import de.siegmar.fastcsv.reader.NamedCsvRecordHandler;

@Service
public class AnalysisService {

	private final ObjectMapper mapper = new ObjectMapper();

	public AnalysisResponse analyze(final MultipartFile file, final String locale, final int recordCount) throws IOException, FTAPluginException, FTAUnsupportedLocaleException {
		if (file == null || file.isEmpty())
			throw new IllegalArgumentException("No file provided");

		final Locale analysisLocale = (locale != null && !locale.isBlank())
				? Locale.forLanguageTag(locale)
				: Locale.getDefault();

		final List<FieldResult> fields = new ArrayList<>();

		try (BufferedReader in = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
			final NamedCsvRecordHandler handler = NamedCsvRecordHandler.builder().allowDuplicateHeaderFields(true).build();
			final CsvReader<NamedCsvRecord> csv = CsvReader.builder().build(handler, in);

			RecordAnalyzer recordAnalyzer = null;
			String[] header = null;
			int thisRecord = 0;

			for (final CloseableIterator<NamedCsvRecord> iter = csv.iterator(); thisRecord < recordCount && iter.hasNext();) {
				final NamedCsvRecord row = iter.next();
				if (thisRecord == 0) {
					header = row.getHeader().toArray(new String[0]);
					final AnalyzerContext context = new AnalyzerContext(null, DateResolutionMode.Auto, file.getOriginalFilename(), header);
					final TextAnalyzer template = new TextAnalyzer(context);
					template.setLocale(analysisLocale);
					recordAnalyzer = new RecordAnalyzer(template);
				}
				thisRecord++;
				if (row.getFields().size() != header.length) {
					System.err.printf("ERROR: Record %d has %d fields, expected %d, skipping%n",
							thisRecord, row.getFields().size(), header.length);
					continue;
				}
				recordAnalyzer.train(row.getFields().toArray(new String[0]));
			}

			if (recordAnalyzer != null) {
				for (final TextAnalysisResult result : recordAnalyzer.getResult().getStreamResults()) {
					fields.add(new FieldResult(
							result.getName(),
							result.isSemanticType(),
							result.getType().toString(),
							result.getTypeModifier(),
							result.isSemanticType() ? result.getSemanticType() : null,
							result.getMinValue(),
							result.getMaxValue(),
							mapper.readTree(result.asJSON(false, 0))));
				}
			}
		}

		final String effectiveLocale = (locale != null && !locale.isBlank()) ? locale : analysisLocale.toLanguageTag();
		return new AnalysisResponse(file.getOriginalFilename(), effectiveLocale, recordCount, fields);
	}

	public List<SemanticTypeInfo> getSemanticTypes(final Locale locale) {
		return SemanticType.getActiveSemanticTypes(locale).stream()
				.map(st -> new SemanticTypeInfo(st.getId(), st.getDescription(),
					st.getDocumentation() != null ? Arrays.asList(st.getDocumentation()) : List.of(),
					Arrays.asList(st.getLanguages())))
				.collect(Collectors.toList());
	}
}
