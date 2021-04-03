/*
 * Copyright 2017-2021 Tim Segall
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

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.SingletonSet;
import com.cobber.fta.TypeFacts;
import com.cobber.fta.core.FTAType;

/**
 * Plugin to detect Addresses. (English-language only).
 */
public class LogicalTypeAddressEN extends LogicalTypeInfinite {
	public static final String SEMANTIC_TYPE = "STREET_ADDRESS_EN";
	private boolean multiline = false;
	private SingletonSet addressMarkersRef = null;
	private Set<String> addressMarkers = null;

	public LogicalTypeAddressEN(PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		final String[] streets = new String[] {
				"RED",  "GREEN", "BLUE", "PINK", "BLACK", "WHITE", "ORANGE", "PURPLE",
				"GREY", "GREEN", "YELLOW", "MAUVE", "CREAM", "BROWN", "SILVER", "GOLD",
				"PEACH", "OLIVE", "LEMON", "LILAC", "BEIGE", "AMBER", "BURGUNDY"
		};

		return String.valueOf(random.nextInt(1024)) + ' ' + streets[random.nextInt(streets.length)] + ' ' + addressMarkersRef.getAt(random.nextInt(addressMarkers.size()));
	}

	@Override
	public boolean initialize(final Locale locale) {
		super.initialize(locale);

		threshold = 90;

		addressMarkersRef = new SingletonSet("resource", "/reference/address_markers.csv");
		addressMarkers = addressMarkersRef.getMembers();

		return true;
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public String getRegExp() {
		return multiline ? "(?s).+" : ".+";
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public boolean isValid(final String input) {
		final String inputUpper = input.trim().toUpperCase(Locale.ENGLISH);
		int length = input.length();

		// Attempt to fail fast
		if (length > 60)
			return false;

		// Simple case first - last 'word is something we recognize
		int spaceIndex = inputUpper.trim().lastIndexOf(' ');
		if (spaceIndex != -1 && addressMarkers.contains(inputUpper.substring(spaceIndex + 1)))
			return true;

		if (inputUpper.startsWith("PO BOX"))
			return true;

		// Accept something of the form, initial digit followed by an address marker word (e.g. Road, Street, etc).
		if (!Character.isDigit(inputUpper.charAt(0)))
			return false;

		String[] words = inputUpper.replace(",", "").split(" ");
		if (words.length < 3)
			return false;

		for (int i = 1; i < words.length  - 1; i++) {
			if (addressMarkers.contains(words[i]))
				return true;
		}

		return false;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		String inputUpper = trimmed.toUpperCase(Locale.ENGLISH);
		int spaceIndex = lastIndex[' '];

		// Track whether this is a multi-line field or not
		if (!multiline)
			multiline = trimmed.indexOf('\n') != -1 || trimmed.indexOf('\r') != -1;


		if (spaceIndex != -1 && addressMarkers.contains(inputUpper.substring(spaceIndex + 1, inputUpper.length())))
			return true;

		if (inputUpper.startsWith("PO BOX"))
			return true;

		if (!Character.isDigit(inputUpper.charAt(0)) || charCounts[' '] < 3)
			return false;

		String[] words = inputUpper.replace(",", "").split(" ");
		for (int i = 1; i < words.length  - 1; i++) {
			if (addressMarkers.contains(words[i]))
				return true;
		}

		return false;
	}

	@Override
	public String isValidSet(final String dataStreamName, final long matchCount, final long realSamples, final TypeFacts facts, final Map<String, Long> cardinality, final Map<String, Long> outliers) {
		return (double)matchCount/realSamples >= getThreshold()/100.0 ? null : ".+";
	}
}
