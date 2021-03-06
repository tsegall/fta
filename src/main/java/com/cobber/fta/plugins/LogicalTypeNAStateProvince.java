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

import java.io.FileNotFoundException;

import com.cobber.fta.LogicalTypeFiniteSimple;
import com.cobber.fta.PluginDefinition;

/**
 * Plugin to detect NA States/Provinces
 */
public class LogicalTypeNAStateProvince extends LogicalTypeFiniteSimple {
	public static final String SEMANTIC_TYPE = "STATE_PROVINCE.STATE_PROVINCE_NA";
	public static final String REGEXP = "\\p{Alpha}{2}";

	public LogicalTypeNAStateProvince(final PluginDefinition plugin) throws FileNotFoundException {
		super(plugin, REGEXP,
				"\\p{IsAlphabetic}{2}", 95);
		setContent("resource", "/reference/na_states_provinces.csv");
	}
}
