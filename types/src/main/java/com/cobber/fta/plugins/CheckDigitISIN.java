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

import org.apache.commons.validator.routines.checkdigit.ISINCheckDigit;

import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAType;

/**
 * Plugin to detect valid International Securities Identification Number.
 */
public class CheckDigitISIN extends CheckDigitLT {
	/** The Semantic type for this Plugin. */
	public static final String SEMANTIC_TYPE = "CHECKDIGIT.ISIN";

	/** The Regular Expression for this Semantic type. */
	public static final String REGEXP = "\\p{IsAlphabetic}\\p{IsAlphabetic}[\\p{IsAlphabetic}\\d]{9}\\d";

	private final static String[] SAMPLES = {
			"GB0000784164", "GB00B63H8491", "JE00BJVNSS43", "ES0177542018", "GB00B082RF11", "GB00B0SWJX34", "GB0033195214",
			"GB00BLDYK618", "GB00BD6K4575", "GB00B19NLV48", "GB00B1XZS820", "GB00B1KJJ408", "GB0031743007", "GB0006731235",
			"GB00BHJYC057", "GB00BH4HKS39", "GB0002634946", "GB0030913577", "GB00B7T77214", "GB0008706128", "GB00B03MM408"
	};

	/**
	 * Construct a plugin to detect ISINs (International Securities Identification Number) based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public CheckDigitISIN(final PluginDefinition plugin) {
		super(plugin, 12);
		validator = new ISINCheckDigit();
	}

	@Override
	public String getRegExp() {
		return REGEXP;
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
	public String nextRandom() {
		return SAMPLES[random.nextInt(SAMPLES.length)];
	}
}
