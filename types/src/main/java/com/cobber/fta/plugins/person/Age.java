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
package com.cobber.fta.plugins.person;

import java.util.Locale;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeCode;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.LogicalTypeFinite;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.Utils;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect Age (Person).
 */
public class Age extends LogicalTypeInfinite {
	private static final String LONG_REGEXP = "\\d{1,3}";
	private static final String DOUBLE_REGEXP = "\\d{1,3}\\.\\d+";

	private static final int MAX_AGE = 120;

	private boolean longType = true;

	private LogicalTypeFinite logicalGender;
	private LogicalTypeCode logicalFirst;
	private LogicalTypeCode logicalRace;

	private String raceSemanticType;

	/**
	 * Construct an Age plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public Age(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		logicalGender = (LogicalTypeFinite) LogicalTypeFactory.newInstance(PluginDefinition.findByName("GENDER.TEXT_<LANGUAGE>"), analysisConfig);

		final PluginDefinition pluginFirst = PluginDefinition.findByName("NAME.FIRST");
		final AnalysisConfig pluginConfig = pluginFirst.isLocaleSupported(locale) ? analysisConfig : new AnalysisConfig(analysisConfig).withLocale(Locale.ENGLISH);
		logicalFirst = (LogicalTypeCode) LogicalTypeFactory.newInstance(pluginFirst, pluginConfig);

		raceSemanticType = analysisConfig.bindSemanticType("PERSON.RACE_<LANGUAGE>");
		final PluginDefinition pluginRace = PluginDefinition.findByName(raceSemanticType);
		if (pluginRace != null)
			logicalRace = (LogicalTypeCode) LogicalTypeFactory.newInstance(pluginRace, pluginConfig);

		return true;
	}

	@Override
	public String nextRandom() {
		return String.valueOf(getRandom().nextInt(99) + 1);
	}

	@Override
	public FTAType getBaseType() {
		return longType ? FTAType.LONG : FTAType.DOUBLE;
	}

	@Override
	public boolean acceptsBaseType(final FTAType type) {
		return type == FTAType.LONG || type == FTAType.DOUBLE;
	}

	@Override
	public String getRegExp() {
		return longType ? LONG_REGEXP : DOUBLE_REGEXP;
	}

	@Override
	public boolean isRegExpComplete() {
		return false;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		final int periodOffset = input.indexOf('.');
		String integerPart;

		if (periodOffset != -1) {
			integerPart = input.substring(0, periodOffset);
			longType = false;
		}
		else
			integerPart = input;

		if (integerPart.length() > 3 || !Utils.isNumeric(integerPart))
			return false;
		final int age = Integer.parseInt(integerPart);
		return age >= 0 && age < MAX_AGE;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return isValid(trimmed, true, -1);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		if (((Number)(facts.getMax())).longValue() > MAX_AGE || ((Number)(facts.getMin())).longValue() < 0 || facts.mean < 5.0 || getHeaderConfidence(context.getStreamName()) == 0)
			return PluginAnalysis.SIMPLE_NOT_OK;

		// Age covers many concepts, we are hunting for a person's age so insist on another highly correlated field
		boolean signalFound = false;
		for (int i = 0; i < context.getCompositeStreamNames().length; i++) {
			if (logicalGender != null && logicalGender.getHeaderConfidence(context.getCompositeStreamNames()[i]) >= 90) {
				signalFound = true;
				break;
			}
			if (logicalFirst != null && logicalFirst.getHeaderConfidence(context.getCompositeStreamNames()[i]) >= 90) {
				signalFound = true;
				break;
			}
			if (logicalRace != null && logicalRace.getHeaderConfidence(context.getCompositeStreamNames()[i]) >= 90) {
				signalFound = true;
				break;
			}
		}

		// This check is similar to the one above but will fire if a suitable Semantic Type was identified on a previous pass
		if (!signalFound &&
				context.existsSemanticType("NAME.FIRST", analysisConfig.bindSemanticType("GENDER.TEXT_<LANGUAGE>"), raceSemanticType))
			signalFound = true;

		return signalFound && (double) matchCount / realSamples >= getThreshold() / 100.0 ?  PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}
}
