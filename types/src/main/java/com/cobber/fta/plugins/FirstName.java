/*
 * Copyright 2017-2023 Tim Segall
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

import java.util.HashSet;
import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect an individuals First Name.
 */
public class FirstName extends PersonName {
	// This set covers the first two letters of ~92% of our first name list - assume this is a reasonable proxy for first names more generally
	private final String plausibleStarters[] = {
			"AB", "AD", "AL", "AM", "AN", "AR", "AS", "AU", "AY",
			"BA", "BE", "BI", "BO", "BR",
			"CA", "CE", "CH", "CI", "CL", "CO", "CR",
			"DA", "DE", "DI", "DO", "EA",
			"ED", "EL", "EM", "EN", "ER", "ES", "EU", "EV",
			"FA", "FE", "FI", "FL", "FR",
			"GA", "GE", "GI", "GL", "GR", "GU",
			"HA", "HE", "HI", "HO", "HU",
			"IL", "IN", "IR", "IS",
			"JA", "JE", "JI", "JO", "JU",
			"KA", "KE", "KI", "KR", "KY",
			"LA", "LE", "LI", "LO", "LU", "LY",
			"MA", "ME", "MI", "MO", "MY",
			"NA", "NE", "NI", "NO",
			"OL", "OR",
			"PA", "PE", "PH", "PI",
			"QU",
			"RA", "RE", "RI", "RO", "RU",
			"SA", "SE", "SH", "SI", "SO", "ST", "SU",
			"TA", "TE", "TH", "TI", "TO", "TR", "TY",
			"VA", "VE", "VI",
			"WA", "WE", "WI",
			"YA", "YO",
			"ZA", "ZO",
	};
	private final Set<String> plausibleSet = new HashSet<>();
	private long bad = 0;

	/**
	 * Construct a First Name plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public FirstName(final PluginDefinition plugin) {
		super(plugin, "firstnames.txt");

		for (final String s: plausibleStarters)
			plausibleSet.add(s);
	}

	@Override
	public boolean isPlausible(final String candidate) {
		if (candidate.length() <= 2 || !plausibleSet.contains(candidate.substring(0, 2)))
			return false;

		// Assume 50% of the remaining are good - hopefully this will not bias the determination excessively.
		// Use hashCode as opposed to random() to ensure that a given data set gives the same results from one run to another.
		return candidate.hashCode() % 10 < 5;
	}

	/*
	 * Note: The input String will be both trimmed and converted to upper Case
	 * @see com.cobber.fta.LogicalType#isValid(java.lang.String)
	 */
	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		final String trimmedUpper = input.trim().toUpperCase(locale);

		if (!Character.isLetter(trimmedUpper.charAt(0))) {
			if (detectMode)
				bad += count;
			return false;
		}

		if (trimmedUpper.length() < minLength && trimmedUpper.length() > maxLength) {
			if (detectMode)
				bad += count;
			return false;
		}

		if (getMembers().contains(trimmedUpper))
			return true;

		final int space = trimmedUpper.indexOf(' ');
		if (space != -1 && getMembers().contains(trimmedUpper.substring(0, space)) && Character.isAlphabetic(trimmedUpper.charAt(space + 1))) {
			final int len = trimmedUpper.length();
			if (len == space + 2 ||
					(len == space + 3 && trimmedUpper.charAt(space + 2) == '.') ||
					getMembers().contains(trimmedUpper.substring(space + 1)))
				return true;
		}

		// For the balance of the 'not found' we will say they are invalid if it is not just a single word
		for (int i = 0; i < trimmedUpper.length(); i++) {
			if (!Character.isAlphabetic(trimmedUpper.charAt(i)))
				return false;
		}

		if (detectMode)
			return isPlausible(trimmedUpper);

		return input.matches(getRegExp());
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		double confidence = (double)matchCount/realSamples;

		// Boost by up to 20% if we like the header
		if (getHeaderConfidence(context.getStreamName()) > 0)
			confidence = Math.min(confidence * 1.2, 1.0);

		return confidence;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples,
			final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		// We do not expect to see much true rubbish
		if (getHeaderConfidence(context.getStreamName()) < 90 && realSamples > 10 && (100*bad)/realSamples > 1)
			return PluginAnalysis.SIMPLE_NOT_OK;
		return super.analyzeSet(context, matchCount, realSamples, currentRegExp, facts, cardinality, outliers, tokenStreams, analysisConfig);
	}

}
