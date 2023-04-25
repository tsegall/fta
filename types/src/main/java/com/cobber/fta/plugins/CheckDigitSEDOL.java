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

import org.apache.commons.validator.routines.checkdigit.SedolCheckDigit;

import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAType;

/**
 * Plugin to detect valid SEDOL identifiers.
 */
public class CheckDigitSEDOL extends CheckDigitLT {
	/** The Regular Expression for this Semantic type. */
	public static final String REGEXP = "[\\p{IsAlphabetic}\\d]{6}\\d";

	private final static String[] SAMPLES = {
			"0078416", "B63H849", "BJVNSS4", "B5M6XQ7", "B082RF1", "B0SWJX3", "3319521", "BLDYK61", "BD6K457", "B19NLV4", "B1XZS82",
			"B1KJJ40", "3174300", "0673123", "BHJYC05", "BH4HKS3", "0263494", "3091357", "B7T7721", "0870612", "B03MM40",
	};

	/**
	 * Construct a plugin to detect SEDOLs (Stock Exchange Daily Official List) based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public CheckDigitSEDOL(final PluginDefinition plugin) {
		super(plugin, 7);
		validator = new SedolCheckDigit();
	}

	@Override
	public String getRegExp() {
		return REGEXP;
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
