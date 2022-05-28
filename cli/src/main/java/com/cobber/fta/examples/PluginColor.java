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
package com.cobber.fta.examples;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.LogicalTypeFinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.token.TokenStreams;

public class PluginColor extends LogicalTypeFinite {
	public static final String SEMANTIC_TYPE_BASE = "COLOR.TEXT_";
	public static final Map<String, Set<String>> MEMBERS = new HashMap<>();
	private static String[] colorsEN = {
			"RED",  "GREEN", "BLUE", "PINK", "BLACK", "WHITE", "ORANGE", "PURPLE",
			"GREY", "YELLOW", "MAUVE", "CREAM", "BROWN", "SILVER", "GOLD",
			"PEACH", "OLIVE", "LEMON", "LILAC", "BEIGE", "AMBER", "BURGUNDY"
	};
	private static String[] colorsFR = {
			"ROUGE", "VERTE", "BLEUE", "ROSE", "NOIRE", "BLANCHE", "ORANGE", "MAUVE",
			"GRISE", "JAUNE", "MAUVE", "CRÈME", "MARRON", "ARGENT", "OR",
			"PÊCHE", "OLIVE", "CITRON", "LILAS", "BEIGE", "AMBRE", "BOURGOGNE"
	};

	private String language;

	static {
		MEMBERS.put("EN", new HashSet<String>(Arrays.asList(colorsEN)));
		MEMBERS.put("FR", new HashSet<String>(Arrays.asList(colorsFR)));
	}

	public PluginColor(final PluginDefinition plugin) throws FTAPluginException {
		super(plugin);
	}

	@Override
	public boolean initialize(final Locale locale) throws FTAPluginException {
		if (!defn.isLocaleSupported(locale))
			throw new FTAPluginException("Locale '" + language + "' is not supported");

		language = locale.toLanguageTag().split("[-_]+")[0].toUpperCase(Locale.ROOT);

		return super.initialize(locale);
	}

	@Override
	public Set<String> getMembers() {
		return MEMBERS.get(language);
	}

	@Override
	public String nextRandom() {
		if ("EN".equals(language))
			return colorsEN[random.nextInt(colorsEN.length)];
		if ("FR".equals(language))
			return colorsFR[random.nextInt(colorsFR.length)];

		return null;
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE_BASE + language;
	}

	@Override
	public String getRegExp() {
		return ".+";
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		if (outliers.size() > 3)
			return PluginAnalysis.SIMPLE_NOT_OK;

		return (double)matchCount / realSamples >= getThreshold()/100.0 ? PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}
}
