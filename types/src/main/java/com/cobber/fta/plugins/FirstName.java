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

import com.cobber.fta.PluginDefinition;

/**
 * Plugin to detect an individuals First Name.
 */
public class FirstName extends PersonName {
	/** The Semantic type for this Plugin. */
	public static final String SEMANTIC_TYPE = "NAME.FIRST";

	/**
	 * Construct a First Name plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public FirstName(final PluginDefinition plugin) {
		super(plugin, "firstnames.txt");
	}

	/*
	 * Note: The input String will be both trimmed and converted to upper Case
	 * @see com.cobber.fta.LogicalType#isValid(java.lang.String)
	 */
	@Override
	public boolean isValid(final String input) {
		final String trimmedUpper = input.trim().toUpperCase(locale);
		if (trimmedUpper.length() < minLength && trimmedUpper.length() > maxLength)
			return false;
		if (getMembers().contains(trimmedUpper))
			return true;

		int space = trimmedUpper.indexOf(' ');
		if (space != -1 && getMembers().contains(trimmedUpper.substring(0, space)) && Character.isAlphabetic(trimmedUpper.charAt(space + 1))) {
			int len = trimmedUpper.length();
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

		// Assume 40% of the remaining are good - hopefully this will not bias the determination excessively.
		// Use hashCode as opposed to random() to ensure that a given data set gives the same results from one run to another.
		return input.hashCode() % 10 < 4;
	}

}
