/*
 * Copyright 2017-2025 Tim Segall
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
 * Plugin to detect '&lt;First Name&gt; &lt;Last Name&gt;'.
 */
public class NameFirstLast extends LogicalTypeInfinite {
	/** The Regular Expression for this Semantic type. */
	private static final String REGEXP = "\\p{IsAlphabetic}[- \\p{IsAlphabetic}]* \\p{IsAlphabetic}[- \\.\\p{IsAlphabetic}]*";
	private static final String BACKOUT = ".+";
	private LogicalTypeFiniteSimple logicalFirst;
	private LogicalTypeFiniteSimple logicalLast;
	private static final int MAX_FIRST_NAMES = 1000;
	private static final int MAX_LAST_NAMES = 1000;
	private FiniteMap lastNames;
	private FiniteMap firstNames;
	private int maxExpectedNames;
	private static Set<String> excludes;

	static {
		excludes = new HashSet<>();
		excludes.add("COMPANY");
		excludes.add("CORP");
		excludes.add("CORP.");
		excludes.add("CORPORATION");
		excludes.add("INC");
		excludes.add("INC.");
		excludes.add("INCORPORATED");
		excludes.add("LIMITED");
		excludes.add("LLC");
		excludes.add("SERVICES");
	}

	/**
	 * Construct a plugin to detect First name followed by Last name based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public NameFirstLast(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		logicalFirst = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByName("NAME.FIRST"), analysisConfig);
		logicalLast = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByName("NAME.LAST"), analysisConfig);

		firstNames = new FiniteMap(MAX_FIRST_NAMES);
		lastNames =  new FiniteMap(MAX_LAST_NAMES);

		// Set the number of reasonable parts of a name - Spanish commonly has more than English
		maxExpectedNames = 4;
		if ("es".equals(locale.getLanguage()))
			maxExpectedNames = 5;

		return true;
	}

	@Override
	public String nextRandom() {
		return logicalFirst.nextRandom() + " " + logicalLast.nextRandom();
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
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		final String trimmed = Utils.cleanse(input.trim());

		final int lastSpace = trimmed.lastIndexOf(' ');
		if (lastSpace == -1)
			return false;

		boolean processingLast = false;
		final int len = trimmed.length();
		int dashes = 0;
		int periods = 0;
		int spaces = 0;
		int apostrophe = 0;
		boolean ampersandSeen = false;
		int alphas = 0;
		for (int i = 0; i < len; i++) {
			if (i == lastSpace) {
				processingLast = true;
				alphas = 0;
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
			if (ch == '-') {
				dashes++;
				if (dashes == 2)
					return false;
				continue;
			}
			if (ch == '.') {
				periods++;
				if (periods == alphas)
					continue;
			}

			// Strictly speaking this is not correct since it rejects 'Oscar de la Renta'
			// OTOH if we lose one or two it should not matter and it dramatically helps with the false positives
			if (ch == ' ') {
				spaces++;
				if (spaces == maxExpectedNames)
					return false;
				alphas = 0;
				continue;
			}

			if (ch == '&' && !ampersandSeen && i < lastSpace && spaces <= 1) {
				ampersandSeen = true;
				continue;
			}

			return false;
		}

		final int firstSpace = trimmed.indexOf(' ');
		final String firstName = trimmed.substring(0, firstSpace);
		final String lastName = trimmed.substring(lastSpace + 1);

		if (excludes.contains(lastName.toUpperCase(locale)))
			return false;

		firstNames.mergeIfSpace(firstName, count, Long::sum);
		lastNames.mergeIfSpace(lastName, count, Long::sum);

		// So if we only have a few names insist it is found, otherwise use the isValid() test
		if (firstNames.size() < 10 ? logicalFirst.isMember(firstName) : logicalFirst.isValid(firstName, detectMode, count))
			return true;

		return lastNames.size() < 10 ? logicalLast.isMember(lastName) : logicalLast.isValid(lastName, detectMode, count);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return trimmed.length() >= 5 && trimmed.length() <= 32 && trimmed.split("\\s+").length <= maxExpectedNames;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		int minCardinality = 10;
		int minSamples = 20;
		if (getHeaderConfidence(context.getStreamName()) > 0) {
			minCardinality = 5;
			minSamples = 5;
		}

		// We expect a decent spread of last names relative to first names - stops us tripping on names of things
		if (matchCount > 50 || firstNames.size() > 10) {
			final double spread = (double)lastNames.size()/firstNames.size();
			if (spread < 0.60 || spread > 5)
				return new PluginAnalysis(BACKOUT);
		}

		// Reject if there is not a reasonable spread of values
		if (getHeaderConfidence(context.getStreamName()) <= 0 && cardinality.size() < analysisConfig.getMaxCardinality() && (double)cardinality.size()/matchCount < .2)
			return new PluginAnalysis(BACKOUT);

		// Reject if there is not a reasonable spread of last or first names
		if (getHeaderConfidence(context.getStreamName()) <= 0 &&
				((lastNames.size() < MAX_LAST_NAMES && (double)lastNames.size()/cardinality.size() < .2) ||
				(firstNames.size() < MAX_FIRST_NAMES && (double)firstNames.size()/cardinality.size() < .2)))
			return new PluginAnalysis(BACKOUT);

		if (cardinality.size() < minCardinality)
			return new PluginAnalysis(BACKOUT);

		if (realSamples < minSamples)
			return new PluginAnalysis(BACKOUT);

		if (getConfidence(matchCount, realSamples, context) >= getThreshold()/100.0)
			return PluginAnalysis.OK;

		return new PluginAnalysis(BACKOUT);
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		final double confidence = (double)matchCount/realSamples;
		if (matchCount == realSamples || getHeaderConfidence(context.getStreamName()) <= 0)
			return confidence;

		return Math.min(confidence + 0.15, 1.0);
	}
}

