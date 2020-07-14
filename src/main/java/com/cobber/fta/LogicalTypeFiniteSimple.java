/*
 * Copyright 2017-2020 Tim Segall
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

public abstract class LogicalTypeFiniteSimple extends LogicalTypeFinite {
	protected String qualifier;
	protected String regexp;
	protected String backout;
	protected Reader reader;
	protected SingletonSet memberSet;

	public LogicalTypeFiniteSimple(PluginDefinition plugin, String regexp, String backout, int threshold) {
		super(plugin);
		this.qualifier = plugin.qualifier;
		this.regexp = regexp;
		this.backout = backout;
		this.threshold = threshold;
	}

	public void setContent(String contentType, String content) {
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
	public boolean initialize(Locale locale) {
		if (this.backout == null)
			throw new IllegalArgumentException("Internal error: Finite Simple types require backout.");

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
	public String isValidSet(String dataStreamName, long matchCount, long realSamples,
			TypeFacts facts, Map<String, Long> cardinality, Map<String, Long> outliers) {
		int headerConfidence = getHeaderConfidence(dataStreamName);

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
