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

import org.apache.commons.validator.routines.checkdigit.EAN13CheckDigit;

import com.cobber.fta.PluginDefinition;

/**
 * Plugin to detect valid UPC identifiers.
 */
public class CheckDigitUPC extends CheckDigitLT {
	/** The Semantic type for this Plugin. */
	public static final String SEMANTIC_TYPE = "CHECKDIGIT.UPC";

	/** The Regular Expression for this Semantic type. */
	public static final String REGEXP = "\\d{12}";

	private final static String[] SAMPLES = {
			"844802112772", "844802212625", "844802212649", "844802193801", "844802029629", "844802037815",
			"844802059671", "844802059824", "844802037884", "844802037891", "844802029681", "844802037822",
			"751906002314", "751906002321", "029757360076", "029757360052", "855468003052", "855468003052",
	};

	/**
	 * Construct a plugin to detect UPCs (Universal Product Code) based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public CheckDigitUPC(final PluginDefinition plugin) {
		super(plugin, 12);
		validator = new EAN13CheckDigit();
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public String nextRandom() {
		return SAMPLES[random.nextInt(SAMPLES.length)];
	}
}
