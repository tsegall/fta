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

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.PluginLocaleEntry;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect the second line of an Address. (English-language only).
 */
public class Address2EN extends LogicalTypeInfinite {
	/** The Semantic type for this Plugin. */
	public static final String SEMANTIC_TYPE = "STREET_ADDRESS2_EN";

	private PluginLocaleEntry addressLine1Entry;

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

		addressLine1Entry = PluginDefinition.findByQualifier("STREET_ADDRESS_EN").getLocaleEntry(locale);

		return true;
	}

	@Override
	public String getSemanticType() {
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
	public boolean isValid(final String input, final boolean detectMode) {
		final int length = input.length();

		// Attempt to fail fast
		return length <= 60;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return isValid(trimmed, true);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		return PluginAnalysis.OK;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		final String dataStreamName = context.getStreamName();
		final int headerConfidence = getHeaderConfidence(dataStreamName);

		// If we don't like the header, or we have no header context or this is better header for an Address Line 1 than an Address Line 2 then bail
		if (headerConfidence < 95 || context.getCompositeStreamNames() == null || context.getCompositeStreamNames().length < 2 ||
				addressLine1Entry.getHeaderConfidence(dataStreamName) > headerConfidence)
			return 0.0;

		// We really don't want to classify Line 1/Line 3 of an address as a Line 2
		if (dataStreamName.length() > 1) {
			final char lastChar = dataStreamName.charAt(dataStreamName.length() - 1);
			if (Character.isDigit(lastChar) && lastChar != '2')
				return 0.0;
		}

		// Get the index of the of the current field
		int current = context.getStreamIndex();

		// Does the previous field exist and look like an Address Line 1?
		if (current < 1 || addressLine1Entry.getHeaderConfidence(context.getCompositeStreamNames()[current - 1]) < 90)
			return 0.0;

		// If all the samples match and the header looks perfect then we are in great shape
		if (matchCount == realSamples && headerConfidence >= 95)
			return 1.0;

		return (double)headerConfidence / 100;
	}
}
