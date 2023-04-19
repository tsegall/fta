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

import org.apache.commons.validator.routines.checkdigit.CUSIPCheckDigit;

import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAType;

/**
 * Plugin to detect valid CUSIPs .
 */
public class CheckDigitCUSIP extends CheckDigitLT {
	/** The Regular Expression for this Semantic type. */
	private static final String REGEXP = "[\\p{IsAlphabetic}\\d]{9}";

	private final static String[] SAMPLES = {
			"000307108", "000307959", "000360206", "000360909", "000360958", "000361105", "000361956", "000375204", "000375907",
			"000375956", "00081T108", "00081T900", "00081T959", "000868109", "000899104", "00090Q103", "00090Q905", "00090Q954", "000957100", "000957902",
			"000957951", "001084904", "020002101", "020002903", "020002952", "03842B903", "095229100", "171484900", "238661904", "260003108", "260003900",
			"260003959", "29275Y953", "34959E950", "38000Q102", "38000Q904", "38000Q953", "42226A909", "46138E677", "47023A309", "47023A903", "47023A952",
			"470299108", "47030M106", "47102XAH8", "47103U100", "47103U209", "47103U407", "47103U506", "564563104", "659310908", "67000B104", "67000B906",
			"67000B955", "670002AB0", "670002104", "670002906", "670002955", "670008AD3", "684000102", "684000904", "72201R403", "74640Y114",
			"800013104", "800013906", "800013955", "80004CAF8", "80007A102", "80007A904", "80007A953", "80007P869", "80007P901", "80007P950", "80007T101"
	};

	/**
	 * Construct a plugin to detect CUSIPs based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public CheckDigitCUSIP(final PluginDefinition plugin) {
		super(plugin, 9);
		validator = new CUSIPCheckDigit();
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
