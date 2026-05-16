/*
 * Copyright 2017-2026 Tim Segall
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

import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Content;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.SingletonSet;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect Job Titles (Japanese language).
 *
 * Accepts values that either appear in the known-titles list or end with one of the
 * productive Japanese job-title suffixes: 長(chief/head), 員(member/staff),
 * 士(licensed professional), 師(practitioner), 者(person/practitioner),
 * 役(officer/role), 官(official).
 */
public class JobTitleJA extends LogicalTypeInfinite {
	public static final String REGEXP = ".+";

	// Last-character suffixes that productively form Japanese job titles
	private static final String TITLE_SUFFIXES = "長員士師者役官";

	private Set<String> knownTitles;

	public JobTitleJA(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		final String[] examples = {
			"部長", "課長", "係長", "主任", "担当", "社長", "副社長", "専務", "常務",
			"取締役", "監査役", "エンジニア", "マネージャー", "ディレクター",
			"弁護士", "公認会計士", "看護師", "薬剤師", "技術者", "研究者"
		};
		return examples[getRandom().nextInt(examples.length)];
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);
		knownTitles = new SingletonSet(new Content("resource", "/reference/ja_job_titles.csv")).getMembers();
		return true;
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		final String trimmed = input.trim();
		final int len = trimmed.length();

		if (len < 2 || len > 20)
			return false;

		if (knownTitles.contains(trimmed))
			return true;

		return TITLE_SUFFIXES.indexOf(trimmed.charAt(len - 1)) >= 0;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return isValid(trimmed, true, 0);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples,
			final String currentRegExp, final Facts facts, final FiniteMap cardinality,
			final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		if (getHeaderConfidence(context) >= 99)
			return PluginAnalysis.OK;

		int minCardinality = 10;
		int minSamples = 20;
		if (getHeaderConfidence(context) > 0) {
			minCardinality = 5;
			minSamples = 5;
		}

		if (cardinality.size() < minCardinality)
			return PluginAnalysis.SIMPLE_NOT_OK;

		if (realSamples < minSamples)
			return PluginAnalysis.SIMPLE_NOT_OK;

		if ((double)matchCount/realSamples >= getThreshold()/100.0)
			return PluginAnalysis.OK;

		return PluginAnalysis.SIMPLE_NOT_OK;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		final double confidence = (double)matchCount/realSamples;

		if (getHeaderConfidence(context) >= 99)
			return 1.0;
		else if (getHeaderConfidence(context) >= 90)
			return Math.min(1.2 * confidence, 1.0);

		return confidence;
	}
}
