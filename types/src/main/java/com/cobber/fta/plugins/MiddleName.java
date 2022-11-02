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
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.LogicalTypeFiniteSimple;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect a middle name.
 */
public class MiddleName extends FirstName {
	/** The Semantic type for this Plugin. */
	public static final String SEMANTIC_TYPE = "NAME.MIDDLE";

	private LogicalTypeFiniteSimple logicalFirst;
	private LogicalTypeFiniteSimple logicalLast;

	/**
	 * Construct a Middle Name plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public MiddleName(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);
		logicalFirst = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByQualifier("NAME.FIRST"), analysisConfig);
		logicalLast = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByQualifier("NAME.LAST"), analysisConfig);

		return true;
	}

	@Override
	public String getSemanticType() {
		return SEMANTIC_TYPE;
	}

	/*
	 * Note: The input String will be both trimmed and converted to upper Case
	 * @see com.cobber.fta.LogicalType#isValid(java.lang.String)
	 */
	@Override
	public boolean isValid(final String input, final boolean detectMode) {
		final String trimmedUpper = input.trim().toUpperCase(locale);
		final int length = trimmedUpper.length();

		if (getMembers().contains(trimmedUpper))
			return true;

		// We are prepared to accepted X or X. as a middle name
		if (length == 1 || length == 2) {
			if (!Character.isAlphabetic(trimmedUpper.charAt(0)))
				return false;
			return length == 1 || trimmedUpper.charAt(1) == '.';
		}

		if (length < minLength || length > maxLength)
			return false;

		// For the balance of the 'not found' we will say they are invalid if it is not just a single word
		for (int i = 0; i < length; i++) {
			if (!Character.isAlphabetic(trimmedUpper.charAt(i)))
				return false;
		}

		if (detectMode) {
			// Assume 40% of the remaining are good - hopefully this will not bias the determination excessively.
			// Use hashCode as opposed to random() to ensure that a given data set gives the same results from one run to another.
			return input.hashCode() % 10 < 4;
		}
		else
			return input.matches(getRegExp());
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		if (context.getCompositeStreamNames() == null)
			return PluginAnalysis.SIMPLE_NOT_OK;

		// If it is consistently short - then reject and detect as MIDDLE_INITIAL
		if (facts.maxTrimmedLength <= 2)
			return PluginAnalysis.SIMPLE_NOT_OK;

		// We have 'Middle Name' or MiddleName'
		if (getHeaderConfidence(context.getStreamName()) >= 99) {
			long newMatchCount = matchCount;
			for (final Map.Entry<String, Long> outlier : outliers.entrySet())
				if (outlier.getKey().length() == 1 && Character.isAlphabetic(outlier.getKey().charAt(0)))
					newMatchCount += outlier.getValue();
			if (super.analyzeSet(context, newMatchCount, realSamples, currentRegExp, facts, cardinality, outliers, tokenStreams, analysisConfig).isValid())
				return PluginAnalysis.OK;
		}

		// Find the index of the First & Last Name fields and of the current field
		int first = -1;
		int last = -1;
		int current = -1;
		for (int i = 0; i < context.getCompositeStreamNames().length; i++) {
			if (first == -1 && logicalFirst.getHeaderConfidence(context.getCompositeStreamNames()[i]) >= 90)
				first = i;
			if (last == -1 && logicalLast.getHeaderConfidence(context.getCompositeStreamNames()[i]) >= 90)
				last = i;
			if (context.getStreamName().equals(context.getCompositeStreamNames()[i]))
				current = i;
		}

		// We want to see fields we recognize as first and last names and a header that looks reasonable and a set of names that look good
		if (first != -1 && last != -1 && current > first &&
				getHeaderConfidence(context.getStreamName()) >= 90 &&
				super.analyzeSet(context, matchCount, realSamples, currentRegExp, facts, cardinality, outliers, tokenStreams, analysisConfig).isValid())
			return PluginAnalysis.OK;

		return PluginAnalysis.SIMPLE_NOT_OK;
	}
}
