/* Copyright 2017-2023 Tim Segall
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

import java.time.LocalDate;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.dates.DateTimeParserConfig;
import com.cobber.fta.dates.DateTimeParserResult;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect Date of Birth (Person).
 */
public class DOB extends LogicalTypeInfinite {
	// nextRandom() will generate dates of the form 'yyyy/MM/dd' - this is the associated RE
	private final String REGEXP_DEFAULT = "\\d{4}/\\d{1,2}/\\d{1,2}";

	private String regExp;

	/**
	 * Construct a plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public DOB(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		return true;
	}

	@Override
	public String nextRandom() {
		StringBuffer b = new StringBuffer();
		return b.append(LocalDate.now().getYear() - random.nextInt(99) + 1)
				.append('/')
				.append(random.nextInt(11) + 1)
				.append('/')
				.append(random.nextInt(27) + 1).toString();
	}

	@Override
	public boolean isRegExpComplete() {
		return false;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		return true;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return true;
	}

	@Override
	public String getRegExp() {
		// If regExp is null then analyzeSet has never been called - so we must be generating samples
		return regExp == null ? REGEXP_DEFAULT : regExp;
	}

	@Override
	public PluginAnalysis analyzeSet(AnalyzerContext context, long matchCount, long realSamples, String currentRegExp,
			Facts facts, FiniteMap cardinality, FiniteMap outliers, TokenStreams tokenStreams,
			AnalysisConfig analysisConfig) {
		// Set the regExp based on the Date Format we detected
		regExp = facts.getMatchTypeInfo().regexp;

		// Need to check that the Date Format we detected has at least Year, Month and Day fields
		final DateTimeParserResult result = DateTimeParserResult.asResult(facts.getMatchTypeInfo().format, context.getDateResolutionMode(), new DateTimeParserConfig());
		return result.isDateBound() && getConfidence(matchCount, realSamples, context) >= getThreshold() / 100.0 ?  PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}
}
