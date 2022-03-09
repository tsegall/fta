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
 * Plugin to detect valid EAN-13 (UPC, ISBN-13) identifiers.
 */
public class CheckDigitEAN13 extends CheckDigitLT {
	public static final String SEMANTIC_TYPE = "CHECKDIGIT.EAN13";
	public static final String REGEXP = "\\d{13}";

	private final static String[] SAMPLES = {
			"9780444505156", "4605664000050", "3014260115531", "8020187300016", "8076809513456", "3155250001387",
			"2151191106847", "1626093139220", "8556467100101", "0922077722381", "3064298186966", "1068035884902",
			"4709099997098", "2460125680880", "9686595482097", "2455962755150", "1883097580551", "9664864959587",
			"4632812983156", "8715988259303", "4114932292979", "1635056616685", "1850775082089", "4514120918771"
	};

	public CheckDigitEAN13(final PluginDefinition plugin) {
		super(plugin, 13);
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
