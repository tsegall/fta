/*
 * Copyright 2017-2021 Tim Segall
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
public class LogicalTypeCheckDigitABA extends LogicalTypeCheckDigit {
	public static final String SEMANTIC_TYPE = "CHECKDIGIT.ABA";
	public static final String REGEXP = "\\d{9}";

	public LogicalTypeCheckDigitABA(final PluginDefinition plugin) {
		super(plugin, 9);
		validator = new ABANumberCheckDigit();
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
	public FTAType getBaseType() {
		return FTAType.LONG;
	}
}
