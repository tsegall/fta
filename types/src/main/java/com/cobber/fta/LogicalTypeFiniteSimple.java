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

import java.io.Reader;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.token.TokenStreams;

/**
 * All Logical Types that are backed by a simple list typically subclass this abstract class.
 */
public abstract class LogicalTypeFiniteSimple extends LogicalTypeFinite {
	protected String qualifier;
	protected String regExp;
	protected String backout;
	protected Reader reader;
	protected SingletonSet memberSet;

	public LogicalTypeFiniteSimple(final PluginDefinition plugin, final String backout, final int threshold) {
		super(plugin);
		this.qualifier = plugin.qualifier;
		this.backout = backout;
		this.threshold = threshold;
	}

	public void setContent(final String contentType, final String content) {
		this.memberSet = new SingletonSet(contentType, content);
	}

	@Override
	public Set<String> getMembers() {
		return memberSet.getMembers();
	}

	public boolean isMember(final String input) {
		return getMembers().contains(input.trim().toUpperCase(locale));
	}

	@Override
	public String nextRandom() {
		String result;
		do {
			result = memberSet.getAt(random.nextInt(getMembers().size()));
		} while ("FTAFTAFTA".equals(result));
		return result;
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		if (this.backout == null)
			throw new FTAPluginException("Internal error: Finite Simple types require backout.");

		super.initialize(analysisConfig);

		regExp = pluginLocaleEntry.getRegExpReturned(-1);

		// If the Regular Expression has not been set then generate one based on the content
		if (regExp == null) {
			final RegExpGenerator gen = new RegExpGenerator(15, Locale.getDefault());

			for (final String elt : getMembers())
			       gen.train(elt);

			regExp = gen.getResult();
		}

		return true;
	}

	@Override
	public String getQualifier() {
		return qualifier;
	}

	@Override
	public String getRegExp() {
		return regExp;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		final int headerConfidence = getHeaderConfidence(context.getStreamName());
		final int baseOutliers = ((100 - getThreshold()) * getSize())/100;

		int maxOutliers = Math.max(1, baseOutliers / 2);
		int minCardinality = Math.min(getSize(), 4);
		int minSamples = 20;
		if (headerConfidence != 0) {
			minCardinality = 1;
			minSamples = headerConfidence < 99 ? 4 : 1;
			maxOutliers =  headerConfidence < 90 ? Math.max(4, baseOutliers) : getSize() / 2;
		}

		if (outliers.size() > maxOutliers)
			return new PluginAnalysis(backout);
		if (cardinality.size() < minCardinality)
			return new PluginAnalysis(backout);
		if (realSamples < minSamples)
			return new PluginAnalysis(backout);

		if ((double)matchCount / realSamples >= getThreshold()/100.0)
			return PluginAnalysis.OK;

		return new PluginAnalysis(backout);
	}
}
