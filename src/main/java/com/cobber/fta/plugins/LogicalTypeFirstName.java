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

import com.cobber.fta.PluginDefinition;

public class LogicalTypeFirstName extends LogicalTypePersonName {
	public static final String SEMANTIC_TYPE = "NAME.FIRST";

	public LogicalTypeFirstName(final PluginDefinition plugin) throws FileNotFoundException {
		super(plugin, "firstnames.txt");
	}
}
