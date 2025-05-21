/*
 * Copyright 2017-2025 Tim Segall
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

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;

import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.Utils;

/**
 * Plugin to detect valid Luhn check digits (commonly Credit Cards or IMEI Numbers).
 */
public class CheckDigitLuhn extends CheckDigitLT {
	/**
	 * Construct a plugin to detect Luhn Check Digits based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public CheckDigitLuhn(final PluginDefinition plugin) {
		super(plugin, -1);
		validator = new LuhnCheckDigit();
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		return input.length() >= 8 && input.length() < 30 && validator.isValid(input);
	}

	@Override
	public String nextRandom() {
		final String base = Utils.getRandomDigits(getRandom(), 14 + getRandom().nextInt(3));
		try {
			return base + validator.calculate(base);
		} catch (CheckDigitException e) {
			return null;
		}
	}
}
