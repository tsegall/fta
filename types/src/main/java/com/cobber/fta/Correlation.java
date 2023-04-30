/*
 * Copyright 2017-2023 Tim Segall
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.cobber.fta.core.InternalErrorException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Correlation {
	private final Locale locale;
	private final String tag;
	private List<SemanticTypeInfo> semanticTypesList;
	private Map<String, SemanticTypeInfo> semanticTypes;

	Correlation(final Locale locale) {
		this.locale = locale;
		this.tag = locale.toLanguageTag().replaceAll("-", "_");
	}

	void initialize() {
		final InputStream is = Correlation.class.getResourceAsStream("/data/SemanticTypes__" + tag + ".json");
		if (is != null) {
			try (BufferedReader JSON = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
				semanticTypesList = new ObjectMapper().readValue(JSON, new TypeReference<List<SemanticTypeInfo>>(){});
			} catch (Exception e) {
				throw new InternalErrorException("Issues with reference plugins file", e);
			}

			semanticTypes = new TreeMap<>();
			for (final SemanticTypeInfo semanticType : semanticTypesList)
				semanticTypes.put(semanticType.semanticType, semanticType);
		}
	}

	List<SemanticTypeInfo> getCorrelatedTypes(final String semanticType, final float threshold) {
		return null;
	}
}
