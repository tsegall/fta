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

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.cobber.fta.core.FTAPluginException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A singleton (per thread) used to track the set of plugins.
 */
public class Plugins {
	private final Map<String, LogicalType> registered = new HashMap<>();
	private final ObjectMapper MAPPER;

	Plugins(final ObjectMapper mapper) {
		this.MAPPER = mapper;
	}

	public void registerPlugins(final Reader JSON, final String dataStreamName, final AnalysisConfig analysisConfig) throws IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, FTAPluginException {
		registerPluginListCore(MAPPER.readValue(JSON, new TypeReference<List<PluginDefinition>>(){}), dataStreamName, analysisConfig, false);
	}

	public void registerPlugins(final String JSON, final String dataStreamName, final AnalysisConfig analysisConfig) throws IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, FTAPluginException {
		registerPluginListCore(MAPPER.readValue(JSON, new TypeReference<List<PluginDefinition>>(){}), dataStreamName, analysisConfig, false);
	}

	public void registerPluginList(final List<PluginDefinition> plugins, final String dataStreamName, final AnalysisConfig analysisConfig) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, FTAPluginException {
		registerPluginListCore(plugins, dataStreamName, analysisConfig, false);
	}

	protected void registerPluginsInternal(final Reader JSON, final String dataStreamName, final AnalysisConfig analysisConfig) throws IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, FTAPluginException {
		registerPluginListCore(MAPPER.readValue(JSON, new TypeReference<List<PluginDefinition>>(){}), dataStreamName, analysisConfig, true);
	}

	protected void registerPluginListCore(final List<PluginDefinition> plugins, final String dataStreamName, final AnalysisConfig analysisConfig, final boolean internal) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, FTAPluginException {
		// Only register plugins that are valid for this locale
		for (final PluginDefinition plugin : plugins) {
			if (!internal)
				if (plugin.priority < PluginDefinition.PRIORITY_EXTERNAL)
					throw new FTAPluginException("Semantic type: '" + plugin.semanticType + "' has invalid priority, priority must be >= " + PluginDefinition.PRIORITY_EXTERNAL);

			boolean register = plugin.isLocaleSupported(analysisConfig.getLocale());

			// Check to see if this plugin requires a mandatory hotword (and it is present)
			if (register && !"*".equals(dataStreamName) && plugin.isMandatoryHeaderUnsatisfied(analysisConfig.getLocale(), dataStreamName))
				register = false;

			if (register)
				if ("java".equals(plugin.pluginType))
					registerLogicalTypeClass(plugin, analysisConfig);
				else if ("list".equals(plugin.pluginType))
					registerLogicalTypeFiniteSet(plugin, analysisConfig);
				else if ("regex".equals(plugin.pluginType))
					registerLogicalTypeRegExp(plugin, analysisConfig);
				else
					throw new FTAPluginException("Semantic type: '" + plugin.semanticType + "' unknown type.");
		}
	}

	private void registerLogicalType(final LogicalType logical, final AnalysisConfig analysisConfig) throws FTAPluginException {
		logical.initialize(analysisConfig);

		if (registered.containsKey(logical.getSemanticType()))
			throw new FTAPluginException("Semantic type: " + logical.getSemanticType() + " already registered.");

		registered.put(logical.getSemanticType(), logical);
	}

	/**
	 * Register a new Semantic Type processor.
	 * See {@link LogicalTypeFinite} or {@link LogicalTypeInfinite}
	 *
	 * @param plugin The Plugin Definition for a Finite or Infinite Semantic Type
	 * @param locale The current Locale
	 * @return Success if the new Semantic type was successfully registered.
	 * @throws ClassNotFoundException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws FTAPluginException
	 */
	private void registerLogicalTypeClass(final PluginDefinition plugin, final AnalysisConfig analysisConfig) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, FTAPluginException {
		Class<?> newLogicalType;
		Constructor<?> ctor;
		LogicalType logical;

		newLogicalType = Class.forName(plugin.clazz);
		ctor = newLogicalType.getConstructor(PluginDefinition.class);
		logical = (LogicalType)ctor.newInstance(plugin);

		if (!(logical instanceof LogicalType))
			throw new FTAPluginException("Semantic type: " + plugin.clazz + " does not appear to be a Semantic Type.");

		registerLogicalType(logical, analysisConfig);
	}

	/**
	 * Register a new Semantic Type processor of type LogicalTypeRegExp. See {@link LogicalTypeRegExp}
	 *
	 * @param plugin The Plugin Definition for a RegExp Semantic Type
	 * @param locale The current Locale
	 * @throws FTAPluginException
	 */
	private void registerLogicalTypeRegExp(final PluginDefinition plugin, final AnalysisConfig analysisConfig) throws FTAPluginException {
		registerLogicalType(new LogicalTypeRegExp(plugin), analysisConfig);
	}

	/**
	 * Register a new Semantic Type processor of type LogicalTypeFiniteSimpleExternal. See {@link LogicalTypeFiniteSimpleExternal}
	 *
	 * @param plugin The Plugin Definition for a simple file-based Semantic Type
	 * @param locale The current Locale
	 * @throws FTAPluginException
	 */
	private void registerLogicalTypeFiniteSet(final PluginDefinition plugin, final AnalysisConfig analysisConfig) throws FTAPluginException {
		registerLogicalType(new LogicalTypeFiniteSimpleExternal(plugin), analysisConfig);
	}

	/**
	 * Return the set of registered Semantic Types.
	 * @return A Collection of the currently registered Semantic Types.
	 */
	public Collection<LogicalType> getRegisteredLogicalTypes() {
		return new HashSet<>(registered.values());
	}

	/**
	 * Return the plugin associated with this named Semantic Type.
	 * @param qualifier Name of this Semantic Type.
	 * @return A Collection of the currently registered Semantic Types.
	 */
	public LogicalType getRegistered(final String qualifier) {
		return registered.get(qualifier);
	}
}
