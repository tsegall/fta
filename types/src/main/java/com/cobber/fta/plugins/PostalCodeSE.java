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
import com.cobber.fta.KnownPatterns;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

/**
 * Plugin to detect valid Swedish Postal Codes.
 */
public class PostalCodeSE extends LogicalTypeInfinite {
	/** The Semantic type for this Plugin. */
	public static final String SEMANTIC_TYPE = "POSTAL_CODE.POSTAL_CODE_SE";

	/** The Regular Expression for this Semantic type. */
	public static final String REGEXP_POSTAL_CODE = "\\d{3} \\d{2}";

	private BloomFilter<CharSequence> reference;
	private static final String examples[] = {
			"186 00", "186 01", "186 03", "186 21", "186 22", "186 23", "186 24", "186 25", "186 26", "186 30",
			"186 31", "186 32", "186 33", "186 34", "186 35", "186 36", "186 37", "186 38", "186 39", "186 40",
			"186 41", "186 42", "186 43", "186 44", "186 45", "186 46", "186 49", "186 50", "186 51", "186 52",
			"186 53", "186 54", "186 55", "186 60", "186 70", "186 86", "186 91", "186 92", "186 93", "186 94",
			"186 95", "186 96", "186 97", "184 86", "103 86", "107 86", "111 86", "112 86", "113 86", "114 86",
			"117 86", "118 86", "121 86", "122 86", "123 86", "125 86", "126 86", "127 86", "141 86", "161 86",
			"164 86", "169 86", "152 86", "131 86", "171 86", "109 86", "110 86", "173 86", "181 86", "186 20",
			"186 47", "186 56", "191 86", "194 86", "901 86", "905 86", "931 86", "930 86", "971 86", "941 86",
			"961 86", "981 86", "751 86", "745 86", "611 86", "631 86", "581 86", "586 00", "586 43", "586 44",
			"586 46", "586 47", "586 48", "586 62", "586 63", "586 65", "586 66", "601 86", "603 86", "591 86",
			"551 86", "351 86", "386 01", "386 21", "386 22", "386 23", "386 30", "386 31", "386 32", "386 33",
			"386 34", "386 35", "386 50", "386 63", "386 64", "386 90", "386 92", "386 93", "386 96", "391 86",
			"598 86", "386 94", "386 95", "386 20", "593 86", "621 86", "371 86", "375 86", "286 01", "286 21",
			"686 25", "686 26", "686 28", "686 29", "686 30", "686 31", "686 33", "686 35", "686 80", "686 91",
			"686 92", "686 93", "686 94", "686 95", "686 96", "686 98", "651 86", "686 97", "686 20", "686 34",
			"701 86", "691 86", "702 86", "721 86", "786 02", "786 21", "786 31", "786 32", "786 33", "786 71"
	};

	/**
	 * Construct a Swedish Postal code plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public PostalCodeSE(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return REGEXP_POSTAL_CODE.equals(compressed.toString()) || "\\d{5}".equals(compressed.toString());
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		try (InputStream filterStream = PostalCodeSE.class.getResourceAsStream("/reference/se_postal_code.bf")) {
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
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public String getRegExp() {
		return REGEXP_POSTAL_CODE;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode) {
		final int len = input.length();

		if (len < 5 || len > 6)
			return false;

		return len == 5 ? reference.mightContain(input.substring(0, 3) + " " + input.substring(3)) : reference.mightContain(input);
	}

	private String backout() {
		return KnownPatterns.PATTERN_ANY_VARIABLE;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		final int headerConfidence = getHeaderConfidence(context.getStreamName());
		if (headerConfidence == 0 && cardinality.size() < 5)
			return new PluginAnalysis(backout());

		if (getConfidence(matchCount, realSamples, context) >= getThreshold()/100.0)
			return PluginAnalysis.OK;

		return new PluginAnalysis(backout());
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		double confidence = (double)matchCount/realSamples;

		// Boost by up to 20% if we like the header
		if (getHeaderConfidence(context.getStreamName()) != 0)
			confidence = Math.min(confidence * 1.2, 1.0);

		return confidence;
	}
}
