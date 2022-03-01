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

import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Map;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.LogicalTypeFiniteSimple;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect a middle name.
 */
public class MiddleName extends FirstName {
	public static final String SEMANTIC_TYPE = "NAME.MIDDLE";
	private LogicalTypeFiniteSimple logicalFirst;
	private LogicalTypeFiniteSimple logicalLast;

	public MiddleName(final PluginDefinition plugin) throws FileNotFoundException {
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
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public String isValidSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		if (context.getCompositeStreamNames() == null)
			return ".+";

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
		return first != -1 && last != -1 && current > first &&
				getHeaderConfidence(context.getStreamName()) >= 90 &&
				super.isValidSet(context, matchCount, realSamples, currentRegExp, facts, cardinality, outliers, tokenStreams, analysisConfig) == null ? null : ".+";
	}
}