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

import com.cobber.fta.Content;
import com.cobber.fta.LogicalTypeFiniteSimple;
import com.cobber.fta.PluginDefinition;

/**
 * Plugin to detect NA States/Provinces
 */
public class NAStateProvince extends LogicalTypeFiniteSimple {
	/** The Semantic type for this Plugin. */
	public static final String SEMANTIC_TYPE = "STATE_PROVINCE.STATE_PROVINCE_NA";

	/**
	 * Construct a North American State/Province plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public NAStateProvince(final PluginDefinition plugin) {
		super(plugin, "\\p{IsAlphabetic}{2}", plugin.threshold);
		setContent(new Content("resource", "/reference/na_states_provinces.csv"));
	}
}
