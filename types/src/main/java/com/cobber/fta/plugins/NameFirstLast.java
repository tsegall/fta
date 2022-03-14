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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.LogicalTypeFiniteSimple;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect '&lt;First Name&gt; &lt;Last Name&gt;'.
 */
public class NameFirstLast extends LogicalTypeInfinite {
	public static final String SEMANTIC_TYPE = "NAME.FIRST_LAST";
	public static final String REGEXP = "\\p{IsAlphabetic}[- \\p{IsAlphabetic}]* \\p{IsAlphabetic}[- \\p{IsAlphabetic}]*";
	private static final String BACKOUT = ".+";
	private LogicalTypeFiniteSimple logicalFirst;
	private LogicalTypeFiniteSimple logicalLast;
	private static final int MAX_FIRST_NAMES = 100;
	private static final int MAX_LAST_NAMES = 100;
	private Set<String> lastNames;
	private Set<String> firstNames;

	public NameFirstLast(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final Locale locale) throws FTAPluginException {
		super.initialize(locale);

		logicalFirst = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByQualifier("NAME.FIRST"), locale);
		logicalLast = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByQualifier("NAME.LAST"), locale);

		lastNames = new HashSet<>();
		firstNames = new HashSet<>();

		return true;
	}

	@Override
	public String nextRandom() {
		return logicalFirst.nextRandom() + " " + logicalLast.nextRandom();
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
		final int lastSpace = trimmed.lastIndexOf(' ');
		if (lastSpace == -1)
			return false;

		boolean processingLast = false;
		final int len = trimmed.length();
		int dashes = 0;
		int spaces = 0;
		int apostrophe = 0;
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
			if (ch == '.' && alphas == 1)
				continue;

			// Strictly speaking this is not correct since it rejects 'Oscar de la Renta'
			// OTOH if we lose one or two it should not matter and it dramatically helps with the false positives
			if (ch == ' ') {
				spaces++;
				if (spaces == 2)
					return false;
				alphas = 0;
				continue;
			}

			return false;
		}

		final int firstSpace = trimmed.indexOf(' ');
		final String firstName = trimmed.substring(0, firstSpace);
		final String lastName = trimmed.substring(lastSpace + 1);

		if (firstNames.size() < MAX_FIRST_NAMES)
			firstNames.add(firstName);
		if (lastNames.size() < MAX_LAST_NAMES)
			lastNames.add(lastName);

		// So if we only have a few names insist it is found, otherwise use the isValid() test
		if (firstNames.size() < 10 ? logicalFirst.isMember(firstName) : logicalFirst.isValid(firstName))
			return true;

		return lastNames.size() < 10 ? logicalLast.isMember(lastName) : logicalLast.isValid(lastName);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return trimmed.length() >= 5 && trimmed.length() <= 30 && (charCounts[' '] == 1 || charCounts[' '] == 2);
	}

	@Override
	public String isValidSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		int minCardinality = 10;
		int minSamples = 20;
		if (getHeaderConfidence(context.getStreamName()) != 0) {
			minCardinality = 5;
			minSamples = 5;
		}

		// Reject if there is not a reasonable spread of values
		if (getHeaderConfidence(context.getStreamName()) == 0 && cardinality.size() < analysisConfig.getMaxCardinality() && (double)cardinality.size()/matchCount < .2)
			return BACKOUT;

		// Reject if there is not a reasonable spread of last or first names
		if (getHeaderConfidence(context.getStreamName()) == 0 &&
				((lastNames.size() < MAX_LAST_NAMES && (double)lastNames.size()/matchCount < .2) ||
				(firstNames.size() < MAX_FIRST_NAMES && (double)firstNames.size()/matchCount < .2)))
			return BACKOUT;

		if (cardinality.size() < minCardinality)
			return BACKOUT;

		if (realSamples < minSamples)
			return BACKOUT;

		return getConfidence(matchCount, realSamples, context.getStreamName()) >= getThreshold()/100.0 ? null : BACKOUT;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final String dataStreamName) {
		final double is = (double)matchCount/realSamples;
		if (matchCount == realSamples || getHeaderConfidence(dataStreamName) == 0)
			return is;

		return is + (1.0 - is)/2;
	}
}

