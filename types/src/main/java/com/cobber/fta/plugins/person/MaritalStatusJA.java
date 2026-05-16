/*
 * Copyright 2017-2026 Tim Segall
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
package com.cobber.fta.plugins.person;

import com.cobber.fta.PluginDefinition;

/**
 * Plugin to detect Marital Status (Japanese language).
 */
public class MaritalStatusJA extends SimpleWords {
	public MaritalStatusJA(final PluginDefinition plugin) {
		super(plugin);
	}

	private static final String[] MARITAL_WORDS_JA = {
		"既婚", "未婚", "独身", "離婚", "死別", "別居", "婚約中", "事実婚", "再婚", "未亡人",
	};

	@Override
	protected String[] getWords() {
		return MARITAL_WORDS_JA;
	}
}
