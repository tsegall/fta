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
import com.cobber.fta.core.Utils;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect Spatial data in Well-Known text format.
 * See https://en.wikipedia.org/wiki/Well-known_text_representation_of_geometry
 */
public class SpatialWKT extends LogicalTypeInfinite {
	/** The Regular Expression for this Semantic type. */
	public static final String REGEXP = "[-+\\p{IsAlphabetic}\\d (),\\.]+";

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
	public SpatialWKT(final PluginDefinition plugin) {
		super(plugin);

		for (final String keyword : keywords) {
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
		int points;
		String ret;
		switch (getRandom().nextInt(3)) {
		case 0:
//			POINT (0 0)
			final int x = getRandom().nextInt(100);
			final int y = getRandom().nextInt(100);
			if (x == 0 || y == 0)
				return "POINT EMPTY";
			return "POINT (" + x + " " + y + ")";
		case 1:
			// LINESTRING (0 0, 0 1, 1 2)
			points = getRandom().nextInt(8) + 1;
			if (points == 1)
				return "LINESTRING EMPTY";
			ret = "LINESTRING (";
			for (int i = 0; i < points; i++) {
				ret += getRandom().nextInt(100) + " " + getRandom().nextInt(100);
				if (i != points - 1)
					ret += ", ";
			}
			return ret + ")";
		case 2:
			// POLYGON ((0 0, 1 0, 1 1, 0 1, 0 0))
			points = getRandom().nextInt(8) + 1;
			if (points == 1)
				return "POLYGON EMPTY";
			ret = "POLYGON ((";
			for (int i = 0; i < points; i++) {
				ret += getRandom().nextInt(100) + " " + getRandom().nextInt(100);
				if (i != points - 1)
					ret += ", ";
			}
			return ret + "))";
		}

		return null;
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
		final int len = trimmed.length();
		if (len < minKeywordLength)
			return false;

		int breakIndex = -1;
		for (int i = 0; i < len && i < maxKeywordLength + 1; i++) {
			final int ch = trimmed.charAt(i);
			if (Character.isAlphabetic(ch))
				continue;
			else if (ch == ' ' || ch == '(') {
				breakIndex = i;
				break;
			}
			else
				break;
		}

		if (breakIndex == -1)
			return false;

		final String word = trimmed.substring(0, breakIndex);

		return keywordSet.contains(word.toUpperCase(Locale.ROOT));
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		return check(input.trim());
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		if (charCounts['('] != charCounts[')'])
			return false;

		if (charCounts['('] == 0 && !Utils.containsIgnoreCase(trimmed, "EMPTY"))
			return false;

		return check(trimmed);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		return (double) matchCount / realSamples >= getThreshold() / 100.0 ?  PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}
}

