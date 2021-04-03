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

import com.cobber.fta.LogicalTypeCode;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.TypeFacts;
import com.cobber.fta.core.FTAType;

/**
 * Plugin to detect '&lt;Last Name&gt;, &lt;First Name&gt;'.
 */
public class LogicalTypeNameLastFirst extends LogicalTypeInfinite {
	public static final String SEMANTIC_TYPE = "NAME.LAST_FIRST";
	public static final String REGEXP = "[- \\p{IsAlphabetic}]+, ?[- \\p{IsAlphabetic}]+";
	private static final String BACKOUT = ".+";
	private LogicalTypeCode logicalFirst;
	private LogicalTypeCode logicalLast;

	public LogicalTypeNameLastFirst(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final Locale locale) {
		super.initialize(locale);

		PluginDefinition pluginFirst = new PluginDefinition("NAME.FIRST", "com.cobber.fta.plugins.LogicalTypeFirstName");
		logicalFirst = (LogicalTypeCode) LogicalTypeFactory.newInstance(pluginFirst, Locale.getDefault());
		PluginDefinition pluginLast = new PluginDefinition("NAME.LAST", "com.cobber.fta.plugins.LogicalTypeLastName");
		logicalLast = (LogicalTypeCode) LogicalTypeFactory.newInstance(pluginLast, Locale.getDefault());

		threshold = 95;

		return true;
	}

	@Override
	public String nextRandom() {
		return logicalLast.nextRandom() + ", " + logicalFirst.nextRandom();
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public boolean isRegExpComplete() {
		return true;
	}

	@Override
	public boolean isValid(final String input) {
		final String trimmed = input.trim();
		final int comma = trimmed.indexOf(',');
		if (comma == -1 || comma == 0 || comma == trimmed.length() - 1)
			return false;

		boolean processingLast = true;
		int len = trimmed.length();
		int spaces = 0;
		int dashes = 0;
		int apostrophe = 0;
		int alphas = 0;
		for (int i = 0; i < len; i++) {
			if (i == comma) {
				processingLast = false;
				alphas = 0;
				spaces = 0;
				dashes = 0;
				continue;
			}
			char ch = trimmed.charAt(i);
			if (Character.isAlphabetic(ch)) {
				alphas++;
				continue;
			}
			if (ch == '\'') {
				apostrophe++;
				if (processingLast && apostrophe == 1)
					continue;
				return false;
			}
			if (ch == ' ') {
				alphas = 0;
				if (i != comma + 1)
					spaces++;
				if (spaces == 2)
					return false;
				continue;
			}
			if (ch == '-') {
				dashes++;
				if (dashes == 2)
					return false;
				continue;
			}
			if (ch == '.' && alphas == 1)
				continue;

			return false;
		}

		return true;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return trimmed.length() >= 5 && trimmed.length() <= 30 && charCounts[','] == 1;
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, TypeFacts facts,
			Map<String, Long> cardinality, Map<String, Long> outliers) {

		int minCardinality = 8;
		int minSamples = 10;
		if (getHeaderConfidence(dataStreamName) != 0) {
			minCardinality = 3;
			minSamples = 3;
		}

		if (cardinality.size() < minCardinality)
			return BACKOUT;

		if (realSamples < minSamples)
			return BACKOUT;

		return getConfidence(matchCount, realSamples, dataStreamName) >= getThreshold()/100.0 ? null : BACKOUT;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final String dataStreamName) {
		final double is = (double)matchCount/realSamples;
		if (matchCount != realSamples && getHeaderConfidence(dataStreamName) != 0)
			return is + (1.0 - is)/2;
		else
			return is;
	}
}

