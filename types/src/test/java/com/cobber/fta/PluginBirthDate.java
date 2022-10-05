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
package com.cobber.fta;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.dates.DateTimeParser;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;
import com.cobber.fta.token.TokenStreams;

public class PluginBirthDate extends LogicalTypeInfinite {
	public final static String REGEXP = "\\d{4}/\\d{2}/\\d{2}";

	private static DateTimeParser dtp = new DateTimeParser();
	private static LocalDate plausibleBirth = LocalDate.of(1910, 1, 1);

	public PluginBirthDate(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		final String format = dtp.determineFormatString(trimmed, DateResolutionMode.MonthFirst);
		final DateTimeFormatter formatter = dtp.ofPattern(format);
		final LocalDate localDate = LocalDate.parse(trimmed, formatter);

		return localDate.isAfter(plausibleBirth);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		return true;
	}

	@Override
	public String nextRandom() {
		return null;
	}

	@Override
	public String getQualifier() {
		return "BIRTHDATE";
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.LOCALDATE;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode) {
		return isCandidate(input.trim(), null, null, null);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples,
			final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		return (double)matchCount/realSamples >= getThreshold()/100.0 ? PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}
}
