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
package com.cobber.fta.plugins;

import java.util.Map;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.LogicalType;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect the second line of an Address. (English-language only).
 */
public class Address2EN extends LogicalTypeInfinite {
	/** The Semantic type for this Plugin. */
	public static final String SEMANTIC_TYPE = "STREET_ADDRESS2_EN";

	private LogicalType logicalAddressLine1;

	/**
	 * Construct a plugin to detect the second line of an Address based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public Address2EN(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		switch (random.nextInt(10)) {
		case 0:
			return "APT " + random.nextInt(100);
		case 1:
			return "APARTMENT #" + random.nextInt(100);
		case 2:
			return "PO BOX " + (1000 + random.nextInt(1000));
		default:
			return "";
		}
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		final PluginDefinition pluginAddress = PluginDefinition.findByQualifier("STREET_ADDRESS_EN");

		logicalAddressLine1 = LogicalTypeFactory.newInstance(pluginAddress, analysisConfig);

		return true;
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public String getRegExp() {
		return ".*";
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public boolean isValid(final String input) {
		final int length = input.length();

		// Attempt to fail fast
		return length <= 60;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return isValid(trimmed);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		if (context.getCompositeStreamNames() == null)
			return PluginAnalysis.SIMPLE_NOT_OK;

		// Find the index of the of the current field
		int current = -1;
		for (int i = 0; i < context.getCompositeStreamNames().length; i++) {
			if (context.getStreamName().equals(context.getCompositeStreamNames()[i])) {
				current = i;
				break;
			}
		}

		// Does the previous field look like an Address Line 1?
		if (current >= 1 && logicalAddressLine1.getHeaderConfidence(context.getCompositeStreamNames()[current - 1]) > 90)
			return PluginAnalysis.OK;

		return PluginAnalysis.SIMPLE_NOT_OK;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final String dataStreamName) {
		// If all the samples match and the header looks perfect then we are in great shape
		if (matchCount == realSamples && getHeaderConfidence(dataStreamName) >= 95)
			return 1.0;

		return (double)getHeaderConfidence(dataStreamName) / 100;
	}
}
