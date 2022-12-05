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

import java.util.HashSet;
import java.util.Set;

import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.Utils;

/**
 * Plugin to detect an individuals Last Name.
 */
public class LastName extends PersonName {
	// This set covers the first two letters of ~95% of our last name list - assume this is a reasonable proxy for last names more generally
	private String plausibleStarters[] = {
		"AB", "AC", "AD", "AG", "AL", "AM", "AN", "AP", "AR", "AS", "AT", "AU", "AV", "AY",
		"BA", "BE", "BI", "BL", "BO", "BR", "BU", "BY",
		"CA", "CE", "CH", "CL", "CO", "CR", "CU",
		"DA", "DE", "DI", "DO", "DR", "DU",
		"EA", "EB", "EC", "ED", "EI", "EL", "EM", "EN", "ER", "ES", "EV",
		"FA", "FE", "FI", "FL", "FO", "FR", "FU",
		"GA", "GE", "GI", "GL", "GO", "GR", "GU",
		"HA", "HE", "HI", "HO", "HU",
		"IN", "IS",
		"JA", "JE", "JO", "JU",
		"KA", "KE", "KI", "KL", "KN", "KO", "KR", "KU",
		"LA", "LE", "LI", "LO", "LU", "LY",
		"MA", "MC", "ME", "MI", "MO", "MU",
		"NA", "NE", "NI", "NO", "NU",
		"OL", "OR", "OS", "PA",
		"PE", "PH", "PI", "PL", "PO", "PR", "PU",
		"QU",
		"RA", "RE", "RH", "RI", "RO", "RU",
		"SA", "SC", "SE", "SH", "SI", "SL", "SM", "SN", "SO", "SP", "ST", "SU", "SW",
		"TA", "TE", "TH", "TI", "TO", "TR", "TU",
		"UR",
		"VA", "VE", "VI", "VO",
		"WA", "WE", "WH", "WI", "WO", "WY",
		"YA", "YO", "ZA",
		"ZE",
	};
	private Set<String> plausibleSet = new HashSet<>();

	/**
	 * Construct a Last Name plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public LastName(final PluginDefinition plugin) {
		super(plugin, "lastnames.txt");

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
	public boolean isValid(final String input, final boolean detectMode) {
		final String trimmedUpper = input.trim().toUpperCase(locale);
		final int len = trimmedUpper.length();

		if (!Character.isLetter(trimmedUpper.charAt(0)) || !Character.isLetter(trimmedUpper.charAt(len - 1)))
			return false;

		int separatorOffset = -1;
		for (int i = 0; i < len; i++) {
			final char ch = trimmedUpper.charAt(i);
			if (Character.isLetter(ch))
				continue;
			if (separatorOffset == -1 && (ch == '-' || ch == ' '))
				separatorOffset = i;
			else
				return super.isValid(input, detectMode);
		}

		if (separatorOffset == -1)
			return super.isValid(input, detectMode);

		if (separatorOffset < 2 || separatorOffset >= trimmedUpper.length() - 2)
			return false;

		final String first = trimmedUpper.substring(0, separatorOffset);
		final String second = trimmedUpper.substring(separatorOffset + 1);

		if (!Utils.isAlphas(first) || !Utils.isAlphas(second))
			return false;

		int matches = 0;
		// If the either component of the hyphenated name is good then declare success
		if (getMembers().contains(first))
			matches++;
		if (getMembers().contains(second))
			matches++;

		final char separator = trimmedUpper.charAt(separatorOffset);
		if (matches == 2 || (matches == 1 && separator == '-'))
			return true;

		if (!detectMode)
			return true;

		if (separator == ' ')
			return false;

		return isPlausible(trimmedUpper);
	}
}
