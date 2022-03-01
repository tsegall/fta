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

import java.util.Locale;
import java.util.Map;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.LogicalType;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect the second line of an Address. (English-language only).
 */
public class Address2EN extends LogicalTypeInfinite {
	public static final String SEMANTIC_TYPE = "STREET_ADDRESS2_EN";

	private LogicalType logicalAddressLine1;

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
	public boolean initialize(final Locale locale) throws FTAPluginException {
		super.initialize(locale);

		PluginDefinition pluginAddress = PluginDefinition.findByQualifier("STREET_ADDRESS_EN");

		logicalAddressLine1 = LogicalTypeFactory.newInstance(pluginAddress, locale);

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
	public String isValidSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		if (context.getCompositeStreamNames() == null)
			return ".+";

		// Find the index of the Address Line 1 field and of the current field
		int line1 = -1;
		int current = -1;
		for (int i = 0; i < context.getCompositeStreamNames().length; i++) {
			if (line1 == -1 && logicalAddressLine1.getHeaderConfidence(context.getCompositeStreamNames()[i]) > 90)
				line1 = i;
			if (context.getStreamName().equals(context.getCompositeStreamNames()[i]))
				current = i;
		}

		return (line1 != -1 && current == line1 + 1 && getHeaderConfidence(context.getStreamName()) > 90) ? null : ".+";
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final String dataStreamName) {
		return (double)getHeaderConfidence(dataStreamName) / 100;
	}
}
