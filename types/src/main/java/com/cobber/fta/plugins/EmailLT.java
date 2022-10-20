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

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeCode;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

/**
 * Plugin to detect Email Addresses.
 */
public class EmailLT extends LogicalTypeInfinite {
	/** The Semantic type for this Plugin. */
	public static final String SEMANTIC_TYPE = "EMAIL";

	/** The Regular Expression for this Semantic type. */
	public static final String REGEXP = "[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}";

	private LogicalTypeCode logicalFirst;
	private LogicalTypeCode logicalLast;
	private static String[] mailDomains = {
			"gmail.com", "hotmail.com", "yahoo.com", "hotmail.com", "aol.com", "msn.com", "comcast.net", "live.com"
	};

	/**
	 * Construct a plugin to detect Email addresses based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public EmailLT(final PluginDefinition plugin) {
		super(plugin);
	}

	private boolean isAscii(final String input) {
		return StandardCharsets.US_ASCII.newEncoder().canEncode(input);
	}

	@Override
	public String nextRandom() {
		String firstName;
		do {
			firstName = logicalFirst.nextRandom().toLowerCase(Locale.ROOT);
		}
		while (!isAscii(firstName));
		String lastName;
		do {
			lastName = logicalLast.nextRandom().toLowerCase(Locale.ROOT);
		}
		while (!isAscii(lastName));

		return firstName + "." + lastName + "@" + mailDomains[random.nextInt(mailDomains.length)];
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		// The Email Plugin is happily supported by any locale, however, if we are generating
		// random entries we use the first and last plugins (which may not be supported by the current locale)
		final PluginDefinition pluginFirst = PluginDefinition.findByQualifier("NAME.FIRST");
		final AnalysisConfig pluginConfig = pluginFirst.isLocaleSupported(locale) ? analysisConfig : new AnalysisConfig(analysisConfig).withLocale(Locale.ENGLISH);
		logicalFirst = (LogicalTypeCode) LogicalTypeFactory.newInstance(pluginFirst, pluginConfig);
		final PluginDefinition pluginLast = PluginDefinition.findByQualifier("NAME.LAST");
		logicalLast = (LogicalTypeCode) LogicalTypeFactory.newInstance(pluginLast, pluginConfig);

		return true;
	}

	@Override
	public String getSemanticType() {
		return SEMANTIC_TYPE;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public boolean isRegExpComplete() {
		return true;
	}

	@Override
	public boolean isValid(String input, final boolean detectMode) {
		// This is not strictly correct since RFC 822 does not mandate an '@' but this is what mortals expect
		if (input.indexOf('@') == -1)
			return false;

		// Address lists commonly have ;'s as separators as opposed to the ','
		if (input.indexOf(';') != -1)
			input = input.replace(';', ',');
		try {
			return InternetAddress.parse(input).length != 0;
		} catch (AddressException e) {
			return false;
		}
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		final int atSigns = charCounts['@'];
		return atSigns - 1 == charCounts[','] || atSigns - 1 == charCounts[';'];
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		return getConfidence(matchCount, realSamples, context) >= getThreshold()/100.0 ? PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		final double is = (double)matchCount/realSamples;
		if (matchCount == realSamples || getHeaderConfidence(context.getStreamName()) == 0)
			return is;

		return is + (1.0 - is)/2;
	}
}
