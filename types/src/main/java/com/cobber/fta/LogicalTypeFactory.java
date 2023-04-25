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
package com.cobber.fta;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.cobber.fta.core.FTAPluginException;

/**
 * Construct a LogicalType from PluginDefinition or a Semantic Type name.
 */
public abstract class LogicalTypeFactory {
	/**
	 * Return a Semantic Type based on a Plugin Definition with an associated AnalysisConfig.
	 *
	 * @param plugin The Definition for this plugin
	 * @param analysisConfig The Analysis configuration used for this analysis
	 * @return The LogicalType The Semantic Type associated with the definition (if it exists), null if non-existent.
	 * @throws FTAPluginException Thrown when the plugin is incorrectly configured.
	 */
	public static LogicalType newInstance(final PluginDefinition plugin, final AnalysisConfig analysisConfig) throws FTAPluginException {
		LogicalType logical = null;

		if ("java".equals(plugin.pluginType)) {
			Class<?> newLogicalType;
			Constructor<?> ctor;

			try {
				newLogicalType = Class.forName(plugin.clazz);
				ctor = newLogicalType.getConstructor(PluginDefinition.class);
				logical = (LogicalTypeCode)ctor.newInstance(plugin);

			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				throw new FTAPluginException("Semantic type: " + plugin.semanticType + " of class " + plugin.clazz + " does not appear to be a Semantic Type.", e);
			}
		}
		else if ("list".equals(plugin.pluginType))
			logical = new LogicalTypeFiniteSimpleExternal(plugin);
		else if ("regex".equals(plugin.pluginType))
			logical = new LogicalTypeRegExp(plugin);

		if (!(logical instanceof LogicalType))
			throw new FTAPluginException("Semantic type: '" + plugin.semanticType + "' unknown type.");

		logical.initialize(analysisConfig);

		return logical;
	}
}
