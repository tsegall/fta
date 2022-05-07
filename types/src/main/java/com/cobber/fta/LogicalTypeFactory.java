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
package com.cobber.fta;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import com.cobber.fta.core.FTAPluginException;

/**
 * Construct a LogicalType from PluginDefinition or a Semantic Type name.
 */
public abstract class LogicalTypeFactory {
	/**
	 * Return a Logical Type based on a Plugin Definition in a particular locale.
	 *
	 * @param plugin The Definition for this plugin
	 * @param locale The locale used for this LogicalType
	 * @return The LogicalType The Logical Type associated with the definition (if it exists), null if non-existent.
	 * @throws FTAPluginException Thrown when the plugin is incorrectly configured.
	 */
	public static LogicalType newInstance(final PluginDefinition plugin, final Locale locale) throws FTAPluginException {
		LogicalType logical = null;

		if (plugin.clazz != null) {
			Class<?> newLogicalType;
			Constructor<?> ctor;

			try {
				newLogicalType = Class.forName(plugin.clazz);
				ctor = newLogicalType.getConstructor(PluginDefinition.class);
				logical = (LogicalTypeCode)ctor.newInstance(plugin);

			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				throw new FTAPluginException("Logical type: " + plugin.qualifier + " of class " + plugin.clazz + " does not appear to be a Logical Type.", e);
			}
		}
		else if (plugin.content != null)
			logical = new LogicalTypeFiniteSimpleExternal(plugin);
		else
			logical = new LogicalTypeRegExp(plugin);

		if (!(logical instanceof LogicalType))
			throw new FTAPluginException("Failed to instantiate a new Logical Type.");

		logical.initialize(locale);

		return logical;
	}
}
