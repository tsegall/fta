/*
 * Copyright 2017-2025 Tim Segall
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
package com.cobber.fta.plugins;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.token.TokenStreams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Plugin to detect Spatial data in GeoJSON format.
 * See https://en.wikipedia.org/wiki/Well-known_text_representation_of_geometry
 */
public class SpatialGeoJSON extends LogicalTypeInfinite {
	/** The Regular Expression for this Semantic type. */
	public static final String REGEXP = ".*";

	private final ObjectMapper mapper = new ObjectMapper();

	private static final String[] keywords = {
			"POINT",
			"MULTIPOINT",
			"LINESTRING",
			"MULTILINESTRING",
			"POLYGON",
			"MULTIPOLYGON"
	};
	private Set<String> keywordSet;
	private int minKeywordLength = Integer.MAX_VALUE;
	private int maxKeywordLength = 0;

	/**
	 * Construct a Spatial plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public SpatialGeoJSON(final PluginDefinition plugin) {
		super(plugin);

		for (String keyword : keywords) {
			final int len = keyword.length();
			if (len < minKeywordLength)
				minKeywordLength = len;
			if (len > maxKeywordLength)
				maxKeywordLength = len;
		}

		keywordSet = new HashSet<>(Arrays.asList(keywords));
	}

	@Override
	public String nextRandom() {
		StringBuilder s = new StringBuilder();
		s.append("{ \"coordinates\": [");
		final int points = getRandom().nextInt(8) + 2;
		for (int i = 0; i < points; i++) {
			s.append('[').
			append(getRandom().nextDouble() * 100).
			append(", ").
			append(getRandom().nextDouble() * 100).
			append(']');
			if (i != points - 1)
				s.append(", ");
		}

		s.append("], \"type\": \"LineString\" }");

		return s.toString();
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public boolean isRegExpComplete() {
		return false;
	}

	private boolean check(final String trimmed) {
		String input = trimmed;
		final int len = input.length();
		if (len < minKeywordLength)
			return false;

		char first = input.charAt(0);
		char last = input.charAt(input.length() - 1);

		// If the whole field is quoted - then inspect it without the quotes
		if (first == '"' && last == '"') {
			input = input.substring(1, input.length() - 1);
			first = input.charAt(0);
			last = input.charAt(input.length() - 1);
		}

		// Quick test to see if it might be a JSON structure (note: must see entire JSON field, i.e. not truncated)
		if (first == '{' && last == '}') {
			try {
				JsonNode root = mapper.readTree(input);
				JsonNode typeNode = root.path("type");
				if (!typeNode.isMissingNode()) {
					JsonNode coordinatesNode = root.path("coordinates");
					if (coordinatesNode.isMissingNode())
						return false;
					String shapeType = root.path("type").asText();
					return keywordSet.contains(shapeType.toUpperCase(Locale.ROOT));
				}
			} catch (IOException e) {
				return false;
			}
		}

		return false;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		return check(input.trim());
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		if (charCounts['{'] == 0)
			return false;

		return check(trimmed);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		return (double) matchCount / realSamples >= getThreshold() / 100.0 ?  PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}
}

