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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.KnownTypes;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

/**
 * Plugin to detect valid Mexican Postal Codes.
 */
public class PostalCodeMX extends LogicalTypeInfinite {
	/** The Regular Expression for this Semantic type. */
	public static final String REGEXP_POSTAL_CODE_5 = "\\d{5}";
	public static final String REGEXP_POSTAL_CODE_45 = "\\d{4,5}";

	private String regExp = REGEXP_POSTAL_CODE_5;

	private BloomFilter<CharSequence> reference;
	private static final String examples[] = {
			"80279", "80302", "80347", "80377", "80380", "80415", "80419", "80466", "80537", "80553",
			"29957", "30129", "30134", "30136", "30362", "30396", "30409", "30441", "30503", "30533",
			"08500", "08830", "09110", "09360", "09366", "09438", "09620", "09704", "09760", "1048",
			"60095", "60130", "60143", "60215", "60255", "60581", "60597", "60656", "60677", "60786",
			"92550", "92605", "92625", "92656", "92663", "92708", "92740", "92883", "92900", "92913",
			"11100", "1139", "11470", "11529", "11587", "11850", "13129", "14273", "14327", "14388",
			"40314", "40409", "40417", "40429", "40482", "40500", "40509", "40573", "40640", "40678",
			"59697", "59699", "59704", "59770", "59793", "59935", "59950", "59958", "59975", "59991",
			"79913", "79983", "79994", "79995", "80014", "80016", "80093", "80101", "80225", "80249",
			"92920", "92943", "92975", "93068", "93152", "93157", "93182", "93194", "93260", "93310"
	};

	/**
	 * Construct a Mexican Postal code plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public PostalCodeMX(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return REGEXP_POSTAL_CODE_5.equals(compressed.toString()) || REGEXP_POSTAL_CODE_45.equals(compressed.toString());
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		try (InputStream filterStream = PostalCodeMX.class.getResourceAsStream("/reference/mx_postal_code.bf")) {
			reference = BloomFilter.readFrom(filterStream, Funnels.stringFunnel(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new FTAPluginException("Failed to load BloomFilter", e);
		}

		return true;
	}

	@Override
	public String nextRandom() {
		return examples[random.nextInt(examples.length)];
	}

	@Override
	public String getRegExp() {
		return regExp;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.LONG;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		final int len = input.length();

		if (len < 4 || len > 5)
			return false;

		if (len == 4) {
			regExp = REGEXP_POSTAL_CODE_45;
			return reference.mightContain("0" + input);
		}

		return reference.mightContain(input);
	}

	private String backout() {
		return KnownTypes.PATTERN_ANY_VARIABLE;
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
