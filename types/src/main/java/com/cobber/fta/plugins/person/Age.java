/* Copyright 2017-2022 Tim Segall
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


import java.util.Map;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
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
	/** The Semantic type for this Plugin. */
	public static final String SEMANTIC_TYPE = "PERSON.AGE";

	/** The Regular Express for this Semantic type. */
	public static final String REGEXP = "\\d{1,3}";

	private LogicalTypeFinite logicalGender;

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

		logicalGender = (LogicalTypeFinite) LogicalTypeFactory.newInstance(PluginDefinition.findByQualifier("GENDER.TEXT_<LOCALE>"), analysisConfig);

		return true;
	}

	@Override
	public String nextRandom() {
		return String.valueOf(random.nextInt(99) + 1);
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.LONG;
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public boolean isRegExpComplete() {
		return false;
	}

	@Override
	public boolean isValid(final String input) {
		if (input.length() > 3 || !Utils.isNumeric(input))
			return false;
		int age = Integer.valueOf(input);
		return age >= 1 && age < 120;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return isValid(trimmed);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		if (getHeaderConfidence(context.getStreamName()) == 0)
			return PluginAnalysis.SIMPLE_NOT_OK;

		// Find a gender field (highly correlated with the presence of age)
		int gender = -1;
		for (int i = 0; i < context.getCompositeStreamNames().length; i++) {
			if (logicalGender.getHeaderConfidence(context.getCompositeStreamNames()[i]) >= 99) {
				gender = i;
				break;
			}
		}

		return gender != -1 && (double) matchCount / realSamples >= getThreshold() / 100.0 ?  PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}
}
