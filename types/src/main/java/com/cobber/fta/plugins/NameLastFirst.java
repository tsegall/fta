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

import java.util.HashSet;
import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.LogicalTypeFiniteSimple;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.Utils;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect '&lt;Last Name&gt;, &lt;First Name&gt;'.
 */
public class NameLastFirst extends LogicalTypeInfinite {
	/** The Semantic type for this Plugin. */
	public static final String SEMANTIC_TYPE = "NAME.LAST_FIRST";

	/** The Regular Expression for this Semantic type. */
	private static final String REGEXP = "\\p{IsAlphabetic}[- \\p{IsAlphabetic}]*, ?[- \\p{IsAlphabetic}]+";
	private static final String BACKOUT = ".+";
	private LogicalTypeFiniteSimple logicalFirst;
	private LogicalTypeFiniteSimple logicalLast;
	private static final int MAX_FIRST_NAMES = 100;
	private static final int MAX_LAST_NAMES = 100;
	private Set<String> lastNames;
	private Set<String> firstNames;

	/**
	 * Construct a plugin to detect Last name followed by First name based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public NameLastFirst(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		logicalFirst = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByQualifier("NAME.FIRST"), analysisConfig);
		logicalLast = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByQualifier("NAME.LAST"), analysisConfig);

		firstNames = new HashSet<>();
		lastNames = new HashSet<>();

		return true;
	}

	@Override
	public String nextRandom() {
		return logicalLast.nextRandom() + ", " + logicalFirst.nextRandom();
	}

	@Override
	public String getSemanticType() {
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
	public boolean isValid(final String input, final boolean detectMode) {
		final String trimmed = Utils.cleanse(input.trim());
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
				if (i != comma + 1 && !(i == comma + 2 && trimmed.charAt(i - 1) == ' '))
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

		String firstName = trimmed.substring(comma + 1).trim();
		final int middleName = firstName.indexOf(' ');
		if (middleName != -1)
			firstName = firstName.substring(0, middleName);
		final String lastName = trimmed.substring(0, comma).trim();

		if (firstNames.size() < MAX_FIRST_NAMES)
			firstNames.add(firstName);
		if (lastNames.size() < MAX_LAST_NAMES)
			lastNames.add(lastName);

		// So if we only have a few names insist it is found, otherwise use the isValid() test
		if (firstNames.size() < 10 ? logicalFirst.isMember(firstName) : logicalFirst.isValid(firstName, detectMode))
			return true;

		return lastNames.size() < 10 ? logicalLast.isMember(lastName) : logicalLast.isValid(lastName, detectMode);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return trimmed.length() >= 5 && trimmed.length() <= 30 && charCounts[','] == 1;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		int minCardinality = 8;
		int minSamples = 10;
		if (getHeaderConfidence(context.getStreamName()) > 0) {
			minCardinality = 3;
			minSamples = 3;
		}

		if (cardinality.size() < minCardinality)
			return new PluginAnalysis(BACKOUT);

		if (realSamples < minSamples)
			return new PluginAnalysis(BACKOUT);

		// Reject if there is not a reasonable spread of last or first names
		if (getHeaderConfidence(context.getStreamName()) <= 0 &&
				((lastNames.size() < MAX_LAST_NAMES && (double)lastNames.size()/matchCount < .2) ||
				(firstNames.size() < MAX_FIRST_NAMES && (double)firstNames.size()/matchCount < .2)))
			return new PluginAnalysis(BACKOUT);

		if (getConfidence(matchCount, realSamples, context) >= getThreshold()/100.0)
			return PluginAnalysis.OK;

		return new PluginAnalysis(BACKOUT);
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		final double is = (double)matchCount/realSamples;
		if (matchCount == realSamples || getHeaderConfidence(context.getStreamName()) <= 0)
			return is;

		return is + (1.0 - is)/2;
	}
}

