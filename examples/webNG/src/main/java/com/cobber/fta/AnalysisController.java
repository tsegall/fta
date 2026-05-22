package com.cobber.fta;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cobber.fta.core.Utils;
import com.cobber.fta.dto.AnalysisResponse;
import com.cobber.fta.dto.GenerateRequest;
import com.cobber.fta.dto.SemanticTypeInfo;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class AnalysisController {

	private final AnalysisService analysisService;
	private final GenerationService generationService;

	public AnalysisController(final AnalysisService analysisService, final GenerationService generationService) {
		this.analysisService = analysisService;
		this.generationService = generationService;
	}

	@PostMapping("/analyze")
	public ResponseEntity<?> analyze(
			@RequestParam("file") final MultipartFile file,
			@RequestParam(value = "locale", required = false) final String locale,
			@RequestParam(value = "recordCount", defaultValue = "100") final int recordCount) {
		try {
			final AnalysisResponse response = analysisService.analyze(file, locale, recordCount);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@PostMapping(value = "/generate", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<?> generate(@RequestBody final GenerateRequest request) {
		try {
			final String csv = generationService.generate(request.spec(), request.count(), request.locale());
			return ResponseEntity.ok(csv);
		} catch (Exception e) {
			return ResponseEntity.badRequest()
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of("error", e.getMessage()));
		}
	}

	@GetMapping("/types")
	public ResponseEntity<List<SemanticTypeInfo>> types(
			@RequestParam(value = "locale", defaultValue = "en") final String locale) {
		return ResponseEntity.ok(analysisService.getSemanticTypes(Locale.forLanguageTag(locale)));
	}

	@GetMapping("/version")
	public ResponseEntity<Map<String, String>> version() {
		return ResponseEntity.ok(Map.of("version", Utils.getVersion()));
	}
}
