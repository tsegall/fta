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
package com.cobber.fta.plugins.address;

import com.cobber.fta.PluginDefinition;

/**
 * Plugin to detect the third line of an Address. (English-language only).
 */
public class Address3EN extends AddressLineNEN {
	/**
	 * Construct a plugin to detect the third line of an Address based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public Address3EN(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	protected int getAddressLine() {
		return 3;
	}
}
