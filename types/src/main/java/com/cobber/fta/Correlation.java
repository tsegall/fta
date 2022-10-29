package com.cobber.fta;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import com.cobber.fta.core.InternalErrorException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Correlation {
	private Locale locale;
	private String tag;
	private List<SemanticTypeInfo> semanticTypesList;
	private TreeMap<String, SemanticTypeInfo>  semanticTypes;

	Correlation(Locale locale) {
		this.locale = locale;
		this.tag = locale.toLanguageTag().replaceAll("-", "_");
	}

	void initialize() {
		InputStream is = Correlation.class.getResourceAsStream("/data/SemanticTypes__" + tag + ".json");
		if (is != null) {
			try (BufferedReader JSON = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
				semanticTypesList = new ObjectMapper().readValue(JSON, new TypeReference<List<SemanticTypeInfo>>(){});
			} catch (Exception e) {
				throw new InternalErrorException("Issues with reference plugins file", e);
			}

			semanticTypes = new TreeMap<>();
			for (SemanticTypeInfo semanticType : semanticTypesList)
				semanticTypes.put(semanticType.semanticType, semanticType);
		}
	}

	List<SemanticTypeInfo> getCorrelatedTypes(String semanticType, float threshold) {
		return null;
	}
}
