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
import java.util.Locale;
import java.util.Map;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
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
 * Plugin to detect valid Japanese Postal Codes.
 */
public class PostalCodeJA extends LogicalTypeInfinite {
	public static final String SEMANTIC_TYPE = "POSTAL_CODE.POSTAL_CODE_JA";
	public static final String REGEXP_POSTAL_CODE = "\\d{3}-\\d{4}";
	private BloomFilter<Integer> zipsRef;
	private static final String examples[] = {
			"004-0063", "004-0064", "004-0065", "004-0068", "004-0069", "004-0071", "004-0072", "004-0073", "014-0051", "014-0052", "018-4271",
			"018-4272", "024-0051", "024-0052", "028-8351", "028-8352", "036-0162", "036-0162", "038-3525", "038-3531", "041-0407", "041-0408",
			"049-5414", "049-5415", "062-0033", "062-0034", "070-8061", "070-8071", "078-3166", "078-3167", "085-1145", "089-2261", "098-3221",
			"100-6930", "107-6006", "158-0096", "183-0003", "220-6110", "243-0415", "260-0814", "277-0087", "289-1537", "298-0217", "300-1634",
			"308-0111", "315-0037", "321-4200", "329-1316", "343-0823", "355-0009", "367-0244", "373-0038", "381-3203", "395-0301", "407-0311",
			"415-0302", "425-0072", "437-1214", "441-3608", "445-0077", "453-0833", "464-0037", "471-0878", "483-8204", "490-1145", "498-0808",
			"501-3227", "503-0936", "509-2204", "511-0946", "517-0701", "520-0853", "526-0103", "537-0014", "566-0034", "576-0054", "591-8013",
			"600-8468", "602-8175", "604-8242", "607-8187", "614-8247", "620-0004", "629-1108", "633-2224", "639-2318", "647-0054", "651-2226",
			"661-0022", "669-1101", "670-0914", "675-1306", "680-0134", "689-1124", "697-0001", "703-8228", "710-0064", "722-0211", "732-0811",
			"741-0072", "752-0931", "762-0025", "771-5409", "780-8088", "786-0061", "792-0002", "799-1108", "808-0014", "817-0321", "830-1127",
			"842-0052", "852-8053", "859-3600", "861-4736", "869-3172", "873-0222", "879-6911", "890-0067", "898-0043", "905-0219", "912-0071",
			"918-8161", "920-2347", "925-0303", "929-2218", "932-0311", "939-0119", "939-2624", "942-0536", "948-0304", "950-0872", "954-0111",
			"959-1717", "960-0808", "963-7716", "968-0412", "969-6506", "981-2116", "986-1335", "989-6436", "997-0055", "999-8211"
	};

	public PostalCodeJA(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return REGEXP_POSTAL_CODE.equals(compressed.toString());
	}

	@Override
	public boolean initialize(final Locale locale) throws FTAPluginException {
		super.initialize(locale);

		try (InputStream filterStream = PostalCodeJA.class.getResourceAsStream("/reference/ja_postal_code.bf")) {
			zipsRef = BloomFilter.readFrom(filterStream, Funnels.integerFunnel());
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
	public boolean isValid(final String input) {
		final int len = input.length();

		if (len != 8)
			return false;

		return zipsRef.mightContain(Integer.valueOf(input.substring(0, 3) + input.substring(4)));
	}

	private String backout() {
		return KnownPatterns.PATTERN_ANY_VARIABLE;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		final int headerConfidence = getHeaderConfidence(context.getStreamName());
		if (headerConfidence == 0 && cardinality.size() < 5)
			return new PluginAnalysis(backout());

		if (getConfidence(matchCount, realSamples, context.getStreamName()) >= getThreshold()/100.0)
			return PluginAnalysis.OK;

		return new PluginAnalysis(backout());
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final String dataStreamName) {
		double confidence = (double)matchCount/realSamples;

		// Boost by up to 20% if we like the header
		if (getHeaderConfidence(dataStreamName) != 0)
			confidence = Math.min(confidence + Math.min((1.0 - confidence)/2, 0.20), 1.0);

		return confidence;
	}
}
