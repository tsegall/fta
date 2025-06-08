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
package com.cobber.fta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

/**
 * Base class for a BloomFilter implementation.
 */
public abstract class LogicalTypeBloomFilter extends LogicalTypeInfinite {
	private BloomFilter<CharSequence> reference;
	private static List<String> examples;

	/**
	 * Construct a BloomFilter plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public LogicalTypeBloomFilter(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return isValid(trimmed, true, 0);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		try (InputStream filterStream = LogicalTypeBloomFilter.class.getResourceAsStream(defn.content.reference + ".bf")) {
			reference = BloomFilter.readFrom(filterStream, Funnels.stringFunnel(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new FTAPluginException("Failed to load BloomFilter", e);
		}

		return true;
	}

	@Override
	public String nextRandom() {
		if (examples == null) {
			synchronized (LogicalTypeBloomFilter.class) {
				if (examples == null) {
					examples = new ArrayList<>();
					final String samplesName = defn.content.reference + "_s.csv";
					final InputStream stream = LogicalTypeFiniteSimpleExternal.class.getResourceAsStream(samplesName);
					if (stream == null)
						throw new IllegalArgumentException("Internal error: Issues with 'resource' content: " + samplesName);

					try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
						String line;

						while ((line = bufferedReader.readLine()) != null) {
							examples.add(line);
						}
					} catch (IOException e) {
						throw new IllegalArgumentException("Internal error: Issues with 'file/resource' content: " + samplesName, e);
					}
				}
			}
		}
		return examples.get(getRandom().nextInt(examples.size()));
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		return reference.mightContain(input.toUpperCase(locale));
	}

	private String backout() {
		return defn.backout;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		final int headerConfidence = getHeaderConfidence(context.getStreamName());
		if (headerConfidence <= 0 && cardinality.size() < 5)
			return new PluginAnalysis(backout());

		if (getConfidence(matchCount, realSamples, context) >= getThreshold()/100.0)
			return PluginAnalysis.OK;

		return new PluginAnalysis(backout());
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		double confidence = (double)matchCount/realSamples;

		// Boost by up to 20% if we like the header
		if (getHeaderConfidence(context.getStreamName()) > 0)
			confidence = Math.min(confidence * 1.2, 1.0);

		return confidence;
	}
}
