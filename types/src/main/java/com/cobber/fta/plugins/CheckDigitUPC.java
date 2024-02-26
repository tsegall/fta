/*
 * Copyright 2017-2024 Tim Segall
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
	/** The Regular Expression for this Semantic type. */
	public static final String REGEXP = "\\d{12}";

	private final static String[] SAMPLES = {
			"689228994560", "723660982409", "796941331913", "072774354948", "888571055670", "400310025319",
			"840226095042", "796941332781", "796941332767", "796941332743", "796941332873", "796941332835",
			"796941332828", "796941333108", "796941333115", "796941331678", "796941331654", "796941331456",
			"760343490448", "760343490097", "844802112772", "844802212625", "844802212649", "844802193801",
			"844802029629", "844802037815", "844802059671", "844802059824", "844802037884", "844802037891",
			"844802029681", "844802037822", "751906002314", "751906002321", "029757360076", "029757360052",
			"855468003052", "855468003052"
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
	public String nextRandom() {
		return SAMPLES[getRandom().nextInt(SAMPLES.length)];
	}
}
