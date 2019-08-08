/*
 * Copyright 2017-2019 Tim Segall
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
import com.cobber.fta.PatternInfo;
import com.cobber.fta.PatternInfo.Type;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.SingletonSet;
import com.cobber.fta.StringFacts;

/**
 * Plugin to detect Addresses. (English-language only).
 */
public class LogicalTypeAddressEN extends LogicalTypeInfinite {
	public final static String SEMANTIC_TYPE = "STREET_ADDRESS_EN";
	private boolean multiline = false;
	private SingletonSet addressMarkersRef = null;
	private Set<String> addressMarkers = null;

	public LogicalTypeAddressEN(PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		String[] streets = new String[] {
				"RED",  "GREEN", "BLUE", "PINK", "BLACK", "WHITE", "ORANGE", "PURPLE",
				"GREY", "GREEN", "YELLOW", "MAUVE", "CREAM", "BROWN", "SILVER", "GOLD",
				"PEACH", "OLIVE", "LEMON", "LILAC", "BEIGE", "AMBER", "BURGUNDY"
		};

		return String.valueOf(random.nextInt(1024)) + ' ' + streets[random.nextInt(streets.length)] + ' ' + addressMarkersRef.getAt(random.nextInt(addressMarkers.size()));
	}

	@Override
	public boolean initialize(Locale locale) {
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
	public Type getBaseType() {
		return PatternInfo.Type.STRING;
	}

	@Override
	public boolean isValid(String input) {
		String inputUpper = input.trim().toUpperCase(Locale.ENGLISH);
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
	public boolean isCandidate(String trimmed, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
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
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, StringFacts stringFacts, Map<String, Long> cardinality, Map<String, Long> outliers) {
		return (double)matchCount/realSamples >= getThreshold()/100.0 ? null : ".+";
	}
}
