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
package com.cobber.fta.plugins;

import java.io.FileNotFoundException;
import java.util.Map;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.FactsTypeBased;
import com.cobber.fta.LogicalTypeFiniteSimple;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.Shapes;

/**
 * Plugin to detect Country names. (English-language only).
 */
public class LogicalTypeCountryEN extends LogicalTypeFiniteSimple {
	public static final String SEMANTIC_TYPE = "COUNTRY.TEXT_EN";
	public static final String REGEXP = ".+";

	public LogicalTypeCountryEN(final PluginDefinition plugin) throws FileNotFoundException {
		super(plugin, REGEXP, "\\p{IsAlphabetic}{2}", 95);
		setContent("resource", "/reference/en_countries.csv");
	}

	@Override
	public String isValidSet(final String dataStreamName, final long matchCount, final long realSamples, String currentRegExp, final FactsTypeBased facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final Shapes shapes, AnalysisConfig analysisConfig) {
		if (matchCount < 50 && outliers.size() > Math.sqrt(getMembers().size()))
			return REGEXP;

		final int headerConfidence = getHeaderConfidence(dataStreamName);

		if (headerConfidence == 0 && (realSamples < 10 || cardinality.size() == 1))
			return REGEXP;

		return (double)matchCount / realSamples >= getThreshold()/100.0 ? null : REGEXP;
	}
}
