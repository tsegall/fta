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

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.cobber.fta.core.FTAPluginException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A singleton (per thread) used to track the set of plugins.
 */
public class Plugins {
	private final Map<String, LogicalType> registered = new HashMap<>();
	private ObjectMapper MAPPER;

	Plugins(final ObjectMapper mapper) {
		this.MAPPER = mapper;
	}

	public void registerPlugins(final Reader JSON, final String dataStreamName, final Locale locale) throws IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, FTAPluginException {
		registerPluginListCore(MAPPER.readValue(JSON, new TypeReference<List<PluginDefinition>>(){}), dataStreamName, locale, false);
	}

	public void registerPlugins(final String JSON, final String dataStreamName, final Locale locale) throws IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, FTAPluginException {
		registerPluginListCore(MAPPER.readValue(JSON, new TypeReference<List<PluginDefinition>>(){}), dataStreamName, locale, false);
	}

	public void registerPluginList(final List<PluginDefinition> plugins, final String dataStreamName, final Locale locale) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, FTAPluginException {
		registerPluginListCore(plugins, dataStreamName, locale, false);
	}

	protected void registerPluginsInternal(final Reader JSON, final String dataStreamName, final Locale locale) throws IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, FTAPluginException {
		registerPluginListCore(MAPPER.readValue(JSON, new TypeReference<List<PluginDefinition>>(){}), dataStreamName, locale, true);
	}

	protected void registerPluginListCore(final List<PluginDefinition> plugins, final String dataStreamName, Locale locale, final boolean internal) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, FTAPluginException {
		if (locale == null)
			locale = Locale.getDefault();

		// Only register plugins that are valid for this locale
		for (final PluginDefinition plugin : plugins) {
			if (!internal)
				if (plugin.priority < PluginDefinition.PRIORITY_EXTERNAL)
					throw new FTAPluginException("Logical type: '" + plugin.qualifier + "' has invalid priority, priority must be >= " + PluginDefinition.PRIORITY_EXTERNAL);

			boolean register = plugin.isLocaleSupported(locale);

			// Check to see if this plugin requires a mandatory hotword (and it is present)
			if (register && !"*".equals(dataStreamName) && plugin.isRequiredHeaderMissing(locale, dataStreamName))
				register = false;

			if (register)
				if ("java".equals(plugin.pluginType))
					registerLogicalTypeClass(plugin, locale);
				else if ("list".equals(plugin.pluginType))
					registerLogicalTypeFiniteSet(plugin, locale);
				else if ("regex".equals(plugin.pluginType))
					registerLogicalTypeRegExp(plugin, locale);
				else
					throw new FTAPluginException("Logical type: '" + plugin.qualifier + "' unknown type.");
		}
	}

	private void registerLogicalType(final LogicalType logical, final Locale locale) throws FTAPluginException {
		logical.initialize(locale);

		if (registered.containsKey(logical.getQualifier()))
			throw new FTAPluginException("Logical type: " + logical.getQualifier() + " already registered.");

		registered.put(logical.getQualifier(), logical);

	}

	/**
	 * Register a new Logical Type processor.
	 * See {@link LogicalTypeFinite} or {@link LogicalTypeInfinite}
	 *
	 * @param plugin The Plugin Definition for a Finite or Infinite Logical Type
	 * @param locale The current Locale
	 * @return Success if the new Logical type was successfully registered.
	 * @throws ClassNotFoundException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws FTAPluginException
	 */
	private void registerLogicalTypeClass(final PluginDefinition plugin, final Locale locale) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, FTAPluginException {
		Class<?> newLogicalType;
		Constructor<?> ctor;
		LogicalType logical;

		newLogicalType = Class.forName(plugin.clazz);
		ctor = newLogicalType.getConstructor(PluginDefinition.class);
		logical = (LogicalType)ctor.newInstance(plugin);

		if (!(logical instanceof LogicalType))
			throw new FTAPluginException("Logical type: " + plugin.clazz + " does not appear to be a Logical Type.");

		registerLogicalType(logical, locale);
	}

	/**
	 * Register a new Logical Type processor of type LogicalTypeRegExp. See {@link LogicalTypeRegExp}
	 *
	 * @param plugin The Plugin Definition for a RegExp Logical Type
	 * @param locale The current Locale
	 * @throws FTAPluginException
	 */
	private void registerLogicalTypeRegExp(final PluginDefinition plugin, final Locale locale) throws FTAPluginException {
		registerLogicalType(new LogicalTypeRegExp(plugin), locale);
	}

	/**
	 * Register a new Logical Type processor of type LogicalTypeFiniteSimpleExternal. See {@link LogicalTypeFiniteSimpleExternal}
	 *
	 * @param plugin The Plugin Definition for a simple file-based Logical Type
	 * @param locale The current Locale
	 * @throws FTAPluginException
	 */
	private void registerLogicalTypeFiniteSet(final PluginDefinition plugin, final Locale locale) throws FTAPluginException {
		registerLogicalType(new LogicalTypeFiniteSimpleExternal(plugin), locale);
	}

	/**
	 * Return the set of registered Logical Types.
	 * @return A Collection of the currently registered Logical Types.
	 */
	public Collection<LogicalType> getRegisteredLogicalTypes() {
		return new HashSet<>(registered.values());
	}

	/**
	 * Return the plugin associated with this named Logical Type.
	 * @param qualifier Name of this Logical Type.
	 * @return A Collection of the currently registered Logical Types.
	 */
	public LogicalType getRegistered(final String qualifier) {
		return registered.get(qualifier);
	}
}
