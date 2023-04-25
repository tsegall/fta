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

import org.apache.commons.validator.routines.checkdigit.ABANumberCheckDigit;

import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAType;

/**
 * Plugin to detect valid ABA Number (or Routing Transit Number (RTN)).
 */
public class CheckDigitABA extends CheckDigitLT {
	/** The Regular Expression for this Semantic type. */
	private static final String REGEXP = "\\d{9}";

	private final static String[] SAMPLES = {
			"981140283", "989853459", "892328657", "781258896", "112551654",
			"438364101", "806651255", "095050162", "505993780", "827776957",
			"086820709", "609581894", "463724075", "167622596", "355856417",
			"138265568", "479756862", "779880373", "750997751", "053438344",
			"199436608", "391657007", "033359472", "465043929", "977684902",
			"373527896"
	};

	/**
	 * Construct a plugin to detect ABA numbers based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public CheckDigitABA(final PluginDefinition plugin) {
		super(plugin, 9);
		validator = new ABANumberCheckDigit();
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.LONG;
	}

	@Override
	public String nextRandom() {
		return SAMPLES[random.nextInt(SAMPLES.length)];
	}
}
