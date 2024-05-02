/*
 * Copyright 2017-2024 Tim Segall
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

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.validator.routines.UrlValidator;

import com.cobber.fta.AnalysisConfig;
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
 * Plugin to detect URLs.
 */
public class URLLT extends LogicalTypeInfinite {
	public static final String REGEXP_PROTOCOL = "(https?|ftp|file)";
	public static final String REGEXP_RESOURCE = "[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
	private static final int MAX_AUTHORITY_LENGTH = 255;
	private static final int MAX_URL_LENGTH = 2000;
	private static UrlValidator validator;
	private static String[] sitesList = {
			"www.jnj.com", "http://www.medifast1.com/index.jsp", "graybar.com", "johnsoncontrols.com", "commscope.com", "www.energizer.com", "ashland.com", "hersheys.com", "www.flowserve.com", "www.exxonmobil.com",
			"pattersoncompanies.com", "www.campbellsoup.com", "mars.com", "www.bjs.com", "conagra.com", "www.lilly.com", "sap.com", "jci.com", "www.dtcc.com", "bakerhughes.com", "www.microsoft.com", "www.jackson.com",
			"www.chs.net", "www.gallo.com", "www.hormelfoods.com", "perrigo.com", "www.cognizant.com", "www.generalmills.com", "hrblock.com", "www.abbott.com", "www.acuity.com", "www.corning.com", "www.lear.com",
			"bunge.com", "fultonschools.org", "www.agcocorp.com", "www.tcfbank.com", "bamfunds.com", "starbucks.com", "www.heicocompanies.com", "www.hertz.com", "canpack.eu", "dell.com", "disney.com", "gm.com",
			"www.mccormick.com", "www.merck.com", "www.versummaterials.com", "www.baxter.com", "www.chevron.com", "www.raytheon.com", "www.valvoline.com", "http://www.pcg-usa.com/", "www.aam.com", "https://techwave.net/",
			"www.bridgestone-firestone.com/", "www.goodyear.com", "www.rockwellcollins.com", "www.rpminc.com", "www.albemarle.com", "www.dow.com", "www.firstmidwest.com", "http://whirlpoolcorp.com", "www.akorn.com",
			"www.apple.com", "www.cardinalhealth.com", "www.fairwayne.com", "www.horacemann.com", "www.kohler.com", "www.richs.com", "www.amgen.com", "www.bms.com", "www.cargill.com",
			"www.cisco.com", "www.exeloncorp.com", "www.farmers.com", "www.firstenergycorp.com", "www.marathonpetroleum.com", "www.usfoods.com", "www.weber.com", "aig.com", "caterpillar.com"
	};

	static {
		validator = UrlValidator.getInstance();
	}

	// Count of items with protocol specified (e.g. http://) at index 0 and no protocol index 1
	private final int[] protocol = new int[2];

	/**
	 * Construct a URL plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public URLLT(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		return true;
	}

	@Override
	public String nextRandom() {
		return sitesList[getRandom().nextInt(sitesList.length)];
	}

	@Override
	public String getRegExp() {
		// Check to see if any of the matches had a protocol specified?
		if (protocol[0] != 0)
			return protocol[1] != 0 ? REGEXP_PROTOCOL + "?" + REGEXP_RESOURCE : REGEXP_PROTOCOL + REGEXP_RESOURCE;

		return REGEXP_RESOURCE;
	}

	@Override
	public boolean isRegExpComplete() {
		return true;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	private static String convertUnicodeURLToAscii(String url) {
		if (url == null || url.length() > MAX_URL_LENGTH)
			return null;

		try {
			url = url.trim();
			URI uri = new URI(url);
			boolean includeScheme = true;

			// URI needs a scheme to work properly with authority parsing
			if (uri.getScheme() == null) {
				uri = new URI("http://" + url);
				includeScheme = false;
			}

			final String scheme = uri.getScheme() != null ? uri.getScheme() + "://" : null;
			final String authority = uri.getRawAuthority() != null ? uri.getRawAuthority() : ""; // includes domain and port
			if (authority.length() > MAX_AUTHORITY_LENGTH)
				return null;
			final String path = uri.getRawPath() != null ? uri.getRawPath() : "";
			final String queryString = uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "";
			final String fragment = uri.getRawFragment() != null ? "#" + uri.getRawFragment() : "";

			// Must convert domain to punycode separately from the path
			url = (includeScheme ? scheme : "") + IDN.toASCII(authority) + path + queryString + fragment;

			// Convert path from unicode to ascii encoding
			return new URI(url).normalize().toASCIIString();
		} catch (URISyntaxException|IllegalArgumentException e) {
			return null;
		}
	}

	@Override
	public boolean isValid(String input, final boolean detectMode, long count) {
		int index = 0;
		if (input.indexOf("://") == -1) {
			input = "http://" + input;
			index = 1;
		}

		final boolean ret = validator.isValid(convertUnicodeURLToAscii(input));
		if (ret)
			protocol[index]++;

		return ret;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		// Does it have a protocol?
		if (charCounts[':'] != 0 && compressed.indexOf("://") != -1)
			return true;

		// Quickly rule out rubbish
		if (charCounts[' '] != 0)
			return false;

		final int length = trimmed.length();
		if (length > MAX_URL_LENGTH)
			return false;

		// Quick rule out a simple long or double
		final int digits = charCounts['0'] + charCounts['1'] + charCounts['2'] + charCounts['3'] + charCounts['4'] +
				+ charCounts['5'] + charCounts['6'] + charCounts['7'] + charCounts['8'] + charCounts['9'];
		if (digits == length || (digits == length - 1 && charCounts['.'] == 1))
			return false;

		final String cleaned = convertUnicodeURLToAscii(trimmed);

		if (cleaned == null)
			return false;

		return validator.isValid("http://" + cleaned);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		return getConfidence(matchCount, realSamples, context) >= getThreshold()/100.0 ? PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		double confidence = (double)matchCount/realSamples;
		// Boost by 10% if we like the header, drop by 5% if we have only seen items with no protocol
		if (getHeaderConfidence(context.getStreamName()) > 0)
			confidence = Math.min(confidence + 0.1, 1.0);
		else if (protocol[0] == 0)
			confidence = Math.max(confidence - 0.05, 0.0);

		return confidence;
	}
}
