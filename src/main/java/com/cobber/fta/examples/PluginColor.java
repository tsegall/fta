/*
 * Copyright 2017-2021 Tim Segall
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
import com.cobber.fta.FactsTypeBased;
import com.cobber.fta.LogicalTypeFinite;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.Shapes;
import com.cobber.fta.core.FTAPluginException;

public class PluginColor extends LogicalTypeFinite {
	public static final String SEMANTIC_TYPE_BASE = "COLOR.TEXT_";
	public static final Map<String, Set<String>> members = new HashMap<>();
	private static String[] colorsEN = new String[] {
			"RED",  "GREEN", "BLUE", "PINK", "BLACK", "WHITE", "ORANGE", "PURPLE",
			"GREY", "YELLOW", "MAUVE", "CREAM", "BROWN", "SILVER", "GOLD",
			"PEACH", "OLIVE", "LEMON", "LILAC", "BEIGE", "AMBER", "BURGUNDY"
	};
	private static String[] colorsFR = new String[] {
			"ROUGE", "VERTE", "BLEUE", "ROSE", "NOIRE", "BLANCHE", "ORANGE", "MAUVE",
			"GRISE", "JAUNE", "MAUVE", "CRÈME", "MARRON", "ARGENT", "OR",
			"PÊCHE", "OLIVE", "CITRON", "LILAS", "BEIGE", "AMBRE", "BOURGOGNE"
	};

	private String language;

	static {
		members.put("EN", new HashSet<String>(Arrays.asList(colorsEN)));
		members.put("FR", new HashSet<String>(Arrays.asList(colorsFR)));
	}

	public PluginColor(final PluginDefinition plugin) throws FTAPluginException {
		super(plugin);
	}

	@Override
	public boolean initialize(final Locale locale) throws FTAPluginException {
		if (!defn.isSupported(locale))
			throw new FTAPluginException("Locale '" + language + "' is not supported");

		language = locale.toLanguageTag().split("[-_]+")[0].toUpperCase(Locale.ROOT);

		return super.initialize(locale);
	}

	@Override
	public Set<String> getMembers() {
		return members.get(language);
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
	public String isValidSet(final String dataStreamName, final long matchCount, final long realSamples, String currentRegExp,
			final FactsTypeBased facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final Shapes shapes, AnalysisConfig analysisConfig) {
		if (outliers.size() > 3)
			return ".+";

		if ((double)matchCount / realSamples >= getThreshold()/100.0)
			return null;

		return ".+";
	}
}
