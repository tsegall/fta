/*
 * Copyright 2017-2022 Tim Segall
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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import com.cobber.fta.core.InternalErrorException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides a wrapper for the semantic-types.json file.
 */
public class SemanticType {
	private static List<SemanticType> allSemanticTypes;
	private static List<SemanticType> activeSemanticTypes;

	/** ID for the Semantic Type. */
	public String id;
	/** Description of the Semantic Type. */
	public String description;
	/** Languages supported by the Semantic Type. */
	public String[] language;
	/** Documentation for the Semantic Type. */
	public String[] documentation;

	public SemanticType() {
	}

	public SemanticType(final PluginDefinition defn) {
		this.id = defn.semanticType;
		this.description = defn.description;
		if (defn.documentation != null) {
			List<String> doco = new ArrayList<>();
			for (final PluginDocumentationEntry entry : defn.documentation)
				doco.add(entry.reference);
			documentation = doco.toArray(new String[doco.size()]);
		}
	}

	/**
	 * Initialize an instance of the set of SemanticType's with the locale.
	 * @param locale The Locale we are currently using
	 */
	public void initialize(final Locale locale) {
		// Populate the full set of Semantic Types
		try (BufferedReader JSON = new BufferedReader(new InputStreamReader(SemanticType.class.getResourceAsStream("/reference/semantic-types.json"), StandardCharsets.UTF_8))) {
			allSemanticTypes = new ObjectMapper().readValue(JSON, new TypeReference<List<SemanticType>>(){});
		} catch (Exception e) {
			throw new InternalErrorException("Issues with reference semantic-types file", e);
		}

		// Populate the active (based on Locale) set of Semantic Types
		final TextAnalyzer analyzer = TextAnalyzer.getDefaultAnalysis(locale);
		final Collection<LogicalType> registered = analyzer.getPlugins().getRegisteredLogicalTypes();
		final Set<String> qualifiers = new TreeSet<>();
		activeSemanticTypes = new ArrayList<>();

		// Sort the registered plugins by Qualifier
		for (final LogicalType logical : registered)
			qualifiers.add(logical.getSemanticType());

		if (!registered.isEmpty()) {
			for (final String qualifier : qualifiers) {
				final LogicalType logical = analyzer.getPlugins().getRegistered(qualifier);
				activeSemanticTypes.add(new SemanticType(logical.getPluginDefinition()));
			}
		}
	}

	public List<SemanticType> getAllSemanticTypes() {
		return allSemanticTypes;
	}

	public List<SemanticType> getActiveSemanticTypes() {
		return activeSemanticTypes;
	}

	public String getId() {
		return id;
	}

	public String getDescription() {
		return description;
	}

	public String[] getDocumentation() {
		return documentation;
	}

	public String[] getLanguage() {
		return language;
	}
}
