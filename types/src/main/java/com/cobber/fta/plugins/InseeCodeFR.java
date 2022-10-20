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
import com.cobber.fta.core.Utils;
import com.cobber.fta.token.TokenStreams;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

/**
 * Plugin to detect valid 5-digit Insee Codes.
 */
public class InseeCodeFR extends LogicalTypeInfinite {
	/** The Regular Expression for this Semantic type. */
	public static final String REGEXP_INSEE_CODE = "\\d{5}";

	private BloomFilter<Integer> reference;
	private static final String examples[] = {
			"01199", "01404", "02138", "02325", "02514", "02702", "03052", "03237", "04118", "05081",
			"06088", "07110", "07298", "08142", "08338", "09025", "09212", "10061", "10248", "10437",
			"11176", "11361", "12107", "12294", "14043", "14244", "14441", "14647", "15073", "15258",
			"16183", "16388", "17151", "17338", "18044", "18230", "19126", "21024", "21211", "21396",
			"21583", "22056", "22247", "23049", "23238", "24164", "24361", "24551", "25166", "25359",
			"25549", "26110", "26303", "27111", "27307", "27501", "28006", "28209", "28409", "29177",
			"30068", "30256", "31084", "31270", "31455", "32047", "32232", "32417", "33137", "33325",
			"33517", "34146", "34331", "35177", "36003", "36187", "37125", "38033", "38235", "38426",
			"39056", "39265", "39468", "40084", "40269", "41121", "42009", "42197", "43047", "43237",
			"44158", "45122", "45310", "46146", "46333", "47178", "48041", "49035", "49227", "50037",
			"50238", "50439", "50635", "51182", "51376", "51573", "52105", "52295", "52488", "53127",
			"54044", "54228", "54417", "55002", "55195", "55384", "55573", "56175", "57101", "57290",
			"57482", "57671", "58096", "58280", "59155", "59341", "59531", "60047", "60232", "60420",
			"60609", "61095", "61287", "61474", "62149", "62338", "62525", "62711", "62897", "63173",
			"63359", "64071", "64256", "64440", "65065", "65250", "65437", "66145", "67098", "67283",
			"67470", "68097", "68283", "69086", "69275", "70154", "70343", "70534", "71142", "71330",
			"71518", "72121", "72311", "73116", "73310", "74176", "76037", "76225", "76412", "76598",
			"77029", "77220", "77405", "78162", "78623", "79179", "80026", "80215", "80407", "80598",
			"80790", "81142", "82003", "82187", "84023", "85059", "85257", "86138", "87032", "88011",
			"88198", "88390", "89049", "89240", "89430", "91103", "92040", "95280", "97313"
	};

	/**
	 * Construct a Insee  code plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public InseeCodeFR(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return REGEXP_INSEE_CODE.equals(compressed.toString());
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		try (InputStream filterStream = InseeCodeFR.class.getResourceAsStream("/reference/fr_insee_code.bf")) {
			reference = BloomFilter.readFrom(filterStream, Funnels.integerFunnel());
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
	public String getSemanticType() {
		return defn.semanticType;
	}

	@Override
	public String getRegExp() {
		return REGEXP_INSEE_CODE;
	}

	@Override
	public FTAType getBaseType() {
		return defn.baseType;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode) {
		final int len = input.length();

		if (len != 5 || !Utils.isNumeric(input))
			return false;

		return reference.mightContain(Integer.valueOf(input));
	}

	private String backout() {
		return KnownTypes.PATTERN_ANY_VARIABLE;
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
