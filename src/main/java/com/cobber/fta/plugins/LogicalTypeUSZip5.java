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

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.FactsTypeBased;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.Shapes;
import com.cobber.fta.SingletonSet;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;

/**
 * Plugin to detect valid US Zip codes.
 * Note: we used an Infinite :-) Logical Type since the domains is so large.
 */
public class LogicalTypeUSZip5 extends LogicalTypeInfinite {
	public static final String SEMANTIC_TYPE = "POSTAL_CODE.ZIP5_US";
	public static final String REGEXP_CONSTANT = "\\d{5}";
	public static final String REGEXP_VARIABLE = "\\d{3,5}";
	private int minLength = 5;
	private SingletonSet zipsRef;
	private Set<String> zips;

	public LogicalTypeUSZip5(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return compressed.length() == 5 && compressed.toString().equals(REGEXP_CONSTANT);
	}

	@Override
	public boolean initialize(final Locale locale) throws FTAPluginException {
		super.initialize(locale);

		threshold = 95;

		zipsRef = new SingletonSet("resource", "/reference/us_zips.csv");
		zips = zipsRef.getMembers();

		return true;
	}

	@Override
	public String nextRandom() {
		return zipsRef.getAt(random.nextInt(zips.size()));
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public String getRegExp() {
		return minLength == 5 ? REGEXP_CONSTANT : REGEXP_VARIABLE;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.LONG;
	}

	@Override
	public boolean isValid(String input) {
		final int len = input.length();

		if (len > 5 || len < 3)
			return false;

		if (len < 5) {
			input = (len == 3 ? "00" : "0") + input;
			minLength = 3;
		}

		return zips.contains(input);
	}

	@Override
	public String isValidSet(final AnalyzerContext context, final long matchCount, final long realSamples, String currentRegExp, final FactsTypeBased facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final Shapes shapes, AnalysisConfig analysisConfig) {
		final String upperDataStreamName = context.getStreamName().toUpperCase();
		final boolean zipName = (upperDataStreamName.contains("ZIP") || upperDataStreamName.contains("POSTALCODE") || upperDataStreamName.contains("POSTCODE"));
		return (cardinality.size() < 5 && !zipName) || (double)matchCount/realSamples < getThreshold()/100.0 ? getRegExp() : null;
	}
}
