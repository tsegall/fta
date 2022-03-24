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
import com.cobber.fta.LogicalTypeCode;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.LogicalTypeFiniteSimple;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect '&lt;Last Name&gt;, &lt;First Name&gt;'.
 */
public class NameLastFirst extends LogicalTypeInfinite {
	/** The Semantic type for this Plugin. */
	public static final String SEMANTIC_TYPE = "NAME.LAST_FIRST";

	/** The Regular Express for this Semantic type. */
	private static final String REGEXP = "\\p{IsAlphabetic}[- \\p{IsAlphabetic}]*, ?[- \\p{IsAlphabetic}]+";
	private static final String BACKOUT = ".+";
	private LogicalTypeCode logicalFirst;
	private LogicalTypeCode logicalLast;

	/**
	 * Construct a plugin to detect Last name followed by First name based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public NameLastFirst(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final Locale locale) throws FTAPluginException {
		super.initialize(locale);

		logicalFirst = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByQualifier("NAME.FIRST"), locale);
		logicalLast = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByQualifier("NAME.LAST"), locale);

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
		final int len = trimmed.length();
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
			final char ch = trimmed.charAt(i);
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
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		int minCardinality = 8;
		int minSamples = 10;
		if (getHeaderConfidence(context.getStreamName()) != 0) {
			minCardinality = 3;
			minSamples = 3;
		}

		if (cardinality.size() < minCardinality)
			return new PluginAnalysis(BACKOUT);

		if (realSamples < minSamples)
			return new PluginAnalysis(BACKOUT);

		if (getConfidence(matchCount, realSamples, context.getStreamName()) >= getThreshold()/100.0)
			return PluginAnalysis.OK;

		return new PluginAnalysis(BACKOUT);
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final String dataStreamName) {
		final double is = (double)matchCount/realSamples;
		if (matchCount == realSamples || getHeaderConfidence(dataStreamName) == 0)
			return is;

		return is + (1.0 - is)/2;
	}
}

