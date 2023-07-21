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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides a wrapper for the semantic-types.json file.
 */
public class SemanticType {
	private static List<SemanticType> allSemanticTypes = new ArrayList<>();
	private static Boolean allSemanticTypesInitialized = false;
	private static final ObjectMapper MAPPER = new ObjectMapper();

	/** ID for the Semantic Type. */
	private String id;
	/** Description of the Semantic Type. */
	private String description;
	/** Languages supported by the Semantic Type. */
	private String[] languages;
	/** Documentation for the Semantic Type. */
	private String[] documentation;

	private SemanticType(final PluginDefinition defn) {
		id = defn.semanticType;
		description = defn.description;

		List<String> languagesSupported = new ArrayList<>();
		if (defn.validLocales != null) {
			for (final PluginLocaleEntry validLocale : defn.validLocales)
				languagesSupported.add(validLocale.localeTag);
		}
		languages = languagesSupported.toArray(new String[0]);

		if (defn.documentation != null) {
			final List<String> doco = new ArrayList<>();
			for (final PluginDocumentationEntry entry : defn.documentation)
				doco.add(entry.reference);
			documentation = doco.toArray(new String[0]);
		}
	}

	public static List<SemanticType> getAllSemanticTypes() {
		if (allSemanticTypesInitialized)
			return allSemanticTypes;

		synchronized (allSemanticTypes) {
			if (allSemanticTypesInitialized)
				return allSemanticTypes;

			// Populate the full set of Semantic Types
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/plugins.json"), StandardCharsets.UTF_8))) {
				final Map<String, PluginDefinition> semanticTypes = new TreeMap<>();
				final List<PluginDefinition> plugins = MAPPER.readValue(reader, new TypeReference<List<PluginDefinition>>(){});
				// Sort the registered plugins by Qualifier
				for (final PluginDefinition pluginDefn : plugins)
					semanticTypes.put(pluginDefn.semanticType, pluginDefn);

				allSemanticTypes = new ArrayList<>();
				for (final Map.Entry<String, PluginDefinition> entry : semanticTypes.entrySet())
					allSemanticTypes.add(new SemanticType(entry.getValue()));
			} catch (Exception e) {
				throw new IllegalArgumentException("Internal error: Issues with plugins file: " + e.getMessage(), e);
			}

			allSemanticTypesInitialized = true;
			return allSemanticTypes;
		}
	}

	public static List<SemanticType> getActiveSemanticTypes(final Locale locale) {
		final List<SemanticType> activeSemanticTypes = new ArrayList<>();

		// Populate the active (based on Locale) set of Semantic Types
		final TextAnalyzer analyzer = TextAnalyzer.getDefaultAnalysis(locale);
		final Collection<LogicalType> registered = analyzer.getPlugins().getRegisteredLogicalTypes();
		final Set<String> semanticTypesNames = new TreeSet<>();

		// Sort the registered plugins by Qualifier
		for (final LogicalType logical : registered)
			semanticTypesNames.add(logical.getSemanticType());

		if (!registered.isEmpty()) {
			for (final String semanticTypeName : semanticTypesNames) {
				final LogicalType logical = analyzer.getPlugins().getRegistered(semanticTypeName);
				activeSemanticTypes.add(new SemanticType(logical.getPluginDefinition()));
			}
		}
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

	public String[] getLanguages() {
		return languages;
	}

	public String toJSONString() {
		final StringBuilder b = new StringBuilder();
		b.append("Id: ").append(id).append(", Description: ").append(description).append(", Languages: ");
		for (String language : languages)
			b.append(language).append(" ");

		return b.toString();
	}
}
