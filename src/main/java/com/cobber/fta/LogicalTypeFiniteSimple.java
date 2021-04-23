/*
 * Copyright 2017-2021 Tim Segall
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

public abstract class LogicalTypeFiniteSimple extends LogicalTypeFinite {
	protected String qualifier;
	protected String regexp;
	protected String backout;
	protected Reader reader;
	protected SingletonSet memberSet;

	public LogicalTypeFiniteSimple(final PluginDefinition plugin, final String regexp, final String backout, final int threshold) {
		super(plugin);
		this.qualifier = plugin.qualifier;
		this.regexp = regexp;
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

	@Override
	public String nextRandom() {
		return memberSet.getAt(random.nextInt(getMembers().size()));
	}

	@Override
	public boolean initialize(final Locale locale) throws FTAPluginException {
		if (this.backout == null)
			throw new FTAPluginException("Internal error: Finite Simple types require backout.");

		super.initialize(locale);

		return true;
	}

	@Override
	public String getQualifier() {
		return qualifier;
	}

	@Override
	public String getRegExp() {
		return regexp;
	}

	@Override
	public String isValidSet(final String dataStreamName, final long matchCount, final long realSamples,
			final FactsTypeBased facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final Shapes shapes) {
		final int headerConfidence = getHeaderConfidence(dataStreamName);

		int maxOutliers = 1;
		int minCardinality = 4;
		int minSamples = 20;
		if (headerConfidence != 0) {
			minCardinality = 1;
			minSamples = 4;
			maxOutliers =  headerConfidence < 90 ? 4 : getSize() / 2;
		}

		if (outliers.size() > maxOutliers)
			return backout;
		if (cardinality.size() < minCardinality)
			return backout;
		if (realSamples < minSamples)
			return backout;

		return (double)matchCount / realSamples >= getThreshold()/100.0 ? null : backout;
	}
}
