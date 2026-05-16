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
package com.cobber.fta.plugins.address;

import java.util.regex.Pattern;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.plugins.Japanese;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect a Japanese street address (住所).
 *
 * Recognises addresses with or without a leading 〒XXX-XXXX postal-code prefix,
 * requiring a city-level administrative unit (市/区/町/村) and a block/lot
 * indicator in either abbreviated numeric form (1-2-3) or explicit kanji form
 * (丁目/番地/条/号).  Prefecture names (都/道/府/県) are common but not required,
 * since many database fields omit them.
 */
public class AddressJA extends LogicalTypeInfinite {
	// Optional leading 〒XXX-XXXX postal-code prefix
	private static final Pattern POSTAL_PREFIX = Pattern.compile("^〒\\d{3}-\\d{4}\\s*");
	// Abbreviated block notation: 1-1, 2-8-1, 1-2-2-100
	private static final Pattern BLOCK_NUMERIC = Pattern.compile("\\d+(-\\d+)+");

	// City-level administrative unit markers (one char each)
	private static final String CITY_MARKERS = "市区町村";
	// Block/lot-level markers including Sapporo's 条 (jō)
	private static final String BLOCK_MARKERS = "丁番号条";

	private static final String[] SAMPLE_ADDRESSES = {
		"東京都千代田区千代田1-1",
		"東京都新宿区西新宿2-8-1",
		"大阪府大阪市北区梅田1-2-2-100",
		"愛知県名古屋市中区三の丸3-1-2",
		"北海道札幌市中央区北1条西5-2",
		"神奈川県横浜市中区山下町1",
		"福岡県福岡市博多区博多駅前1-1-1",
		"京都府京都市中京区烏丸通三条上ル2-1"
	};

	/**
	 * Construct a plugin to detect a Japanese Address based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public AddressJA(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);
		return true;
	}

	@Override
	public String nextRandom() {
		return SAMPLE_ADDRESSES[getRandom().nextInt(SAMPLE_ADDRESSES.length)];
	}

	@Override
	public String getRegExp() {
		return ".+";
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	private static boolean hasJapaneseChar(final String s) {
		for (int i = 0; i < s.length(); i++)
			if (Japanese.isJapaneseChar(s.charAt(i)))
				return true;
		return false;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		if (trimmed.isEmpty())
			return false;
		// Postal-code prefix is an immediate candidate signal
		if (trimmed.charAt(0) == '〒')
			return true;
		// City-level marker is a strong signal
		for (int i = 0; i < CITY_MARKERS.length(); i++)
			if (trimmed.indexOf(CITY_MARKERS.charAt(i)) != -1)
				return true;
		// Japanese text + block pattern (neighborhood+lot form common in database address fields)
		return hasJapaneseChar(trimmed) && BLOCK_NUMERIC.matcher(trimmed).find();
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		String addr = input.trim();

		final int len = addr.codePointCount(0, addr.length());
		if (len < 4 || len > 100)
			return false;

		// Strip optional 〒XXX-XXXX prefix before structural checks
		addr = POSTAL_PREFIX.matcher(addr).replaceFirst("");

		// Must have at least one Japanese character
		if (!hasJapaneseChar(addr))
			return false;

		// Require a block/lot indicator — abbreviated numeric form is dominant in databases
		if (BLOCK_NUMERIC.matcher(addr).find())
			return true;

		// Also accept explicit kanji block markers (丁目, 番地, 番, 号, 条)
		for (int i = 0; i < BLOCK_MARKERS.length(); i++)
			if (addr.indexOf(BLOCK_MARKERS.charAt(i)) != -1)
				return true;

		// Accept city/ward/town marker followed by a trailing number (e.g. 山下町1)
		if (Character.isDigit(addr.charAt(addr.length() - 1)))
			for (int i = 0; i < CITY_MARKERS.length(); i++)
				if (addr.indexOf(CITY_MARKERS.charAt(i)) != -1)
					return true;

		return false;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples,
			final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers,
			final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		return getConfidence(matchCount, realSamples, context) >= getThreshold() / 100.0
				? PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		final int headerConfidence = getHeaderConfidence(context);
		double confidence = (double) matchCount / realSamples;

		if (headerConfidence >= 99)
			confidence = Math.min(confidence + 0.20, 1.0);
		else if (headerConfidence >= 90)
			confidence = Math.min(confidence + 0.10, 1.0);
		else if (headerConfidence > 0)
			confidence = Math.min(confidence + 0.05, 1.0);

		return confidence;
	}
}
