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
package com.cobber.fta.plugins.address;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Content;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalType;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.PluginLocaleEntry;
import com.cobber.fta.SingletonSet;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.WordProcessor;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect the second line of an Address. (English-language only).
 */
public abstract class AddressLineNEN extends LogicalTypeInfinite {
	private PluginLocaleEntry previousAddressLineEntry;
	private PluginLocaleEntry cityEntry;
	private String country;
	private LogicalType logicalZip;
	private LogicalType logicalState;
	private Set<String> addressMarkers;
	private final WordProcessor wordProcessor = new WordProcessor().withAdditionalBreakChars("-#").withAdditionalKillChars("'");

	private final String[] previousLine = { "STREET_ADDRESS_EN", "STREET_ADDRESS2_EN", "STREET_ADDRESS3_EN" };

	/**
	 * Construct a plugin to detect the second line of an Address based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public AddressLineNEN(final PluginDefinition plugin) {
		super(plugin);
	}

	protected abstract int getAddressLine();

	private char getAddressLineChar() {
		return (char)(getAddressLine() + '0');
	}

	@Override
	public String nextRandom() {
		switch (getRandom().nextInt(10)) {
		case 0:
			return "APT " + getRandom().nextInt(100);
		case 1:
			return "APARTMENT #" + getRandom().nextInt(100);
		case 2:
			return "PO BOX " + (1000 + getRandom().nextInt(1000));
		default:
			return "";
		}
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		addressMarkers = new SingletonSet(new Content("resource", "/reference/en_street_markers.csv")).getMembers();
		previousAddressLineEntry = PluginDefinition.findByName(previousLine[getAddressLine() - 2]).getLocaleEntry(locale);
		cityEntry = PluginDefinition.findByName("CITY").getLocaleEntry(locale);
		country = locale.getCountry().toUpperCase(Locale.ROOT);


		if ("US".equals(country)) {
			logicalZip = LogicalTypeFactory.newInstance(PluginDefinition.findByName("POSTAL_CODE.ZIP5_US"), analysisConfig);
			logicalState = LogicalTypeFactory.newInstance(PluginDefinition.findByName("STATE_PROVINCE.STATE_US"), analysisConfig);
		}

		return true;
	}

	@Override
	public String getRegExp() {
		return ".*";
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		final int length = input.length();

		// Attempt to fail fast
		if (length > 60)
			return false;

		return validation(input.trim().toUpperCase(Locale.ENGLISH), true);
	}

	private boolean validation(final String trimmedUpper, final boolean detectMode) {
		final List<String> words = wordProcessor.asWords(trimmedUpper);
		boolean postalFound = false;
		boolean stateFound = false;

		final int wordCount = words.size();
		for (int i = 0; i < wordCount; i++) {
			final String word = words.get(i);
			if (i != 0 && addressMarkers.contains(word))
				return true;
			else if (AddressCommon.isModifier(word, i == wordCount - 1) && words.size() != 1)
				return true;
			if (logicalZip != null && !postalFound && logicalZip.isValid(word))
				postalFound = true;
			if (logicalState != null && !stateFound && logicalState.isValid(word))
				stateFound = true;
			if (postalFound && stateFound)
				return true;
		}

		return false;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return validation(trimmed.toUpperCase(Locale.ENGLISH), true);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		return PluginAnalysis.OK;
	}

	private int headerConfidence(final AnalyzerContext context) {
		final String dataStreamName = context.getStreamName();

		if (dataStreamName.length() == 0)
			return 0;

		final int headerConfidence = getHeaderConfidence(dataStreamName);
		if (headerConfidence >= 99)
			return headerConfidence;

		// If we have no context then we are all done
		if (context.getCompositeStreamNames() == null)
			return 0;

		final int current = context.getStreamIndex();
		// Make sure we can access the previous field
		if (current == 0)
			return 0;

		final String previousStreamName = context.getCompositeStreamNames()[current - 1];
		if (previousStreamName == null || previousStreamName.isBlank())
			return 0;

		final char lastChar = dataStreamName.charAt(dataStreamName.length() - 1);
		// If it looks like an address and the last character matches the address line we are looking for we feel really good
		if (headerConfidence >= 90 && getAddressLine() == lastChar)
			return 99;

		// If this header is the same as the previous but with the previous number then we are pretty confident
		final int index = previousStreamName.indexOf(getAddressLineChar() - 1);
		if (index != -1 && dataStreamName.equals(previousStreamName.substring(0, index) + getAddressLineChar() + previousStreamName.substring(index + 1)))
			return headerConfidence >= 90 ? 99 : 85;

		if (headerConfidence == 0 && lastChar != getAddressLineChar())
			return 0;

		// Does the previous field look like an Address Line <N-1> AND not look like an Address Line <N>
		if (previousAddressLineEntry.getHeaderConfidence(previousStreamName) >= 90 && getHeaderConfidence(previousStreamName) < 99) {
			if (current + 1 < context.getCompositeStreamNames().length &&
					cityEntry.getHeaderConfidence(context.getCompositeStreamNames()[current + 1]) > 0)
				return headerConfidence == 0 ? 85 : 95;
			return headerConfidence == 0 ? 85 : 90;
		}

		return 0;
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		final int headerConfidence = headerConfidence(context);

		// If we really don't like the header call it a day
		if (headerConfidence == 0)
			return 0.0;

		final double matchConfidence = (double)matchCount/realSamples;

		// If we are super confident in the header boost if we like the data
		if (headerConfidence == 99)
			return matchConfidence > .25 ? .95 : .9;

		// Header is reasonable - so check we have some data that looks reasonable
		if (headerConfidence >= 90)
			return matchConfidence > .25 ? .9 : 0;

		return matchConfidence > .5 ? .9 : 0;
	}
}
