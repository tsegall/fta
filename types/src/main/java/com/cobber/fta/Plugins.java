/*
 * Copyright 2017-2025 Tim Segall
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.cobber.fta.core.FTAPluginException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A class used to track the set of plugins.
 */
public class Plugins {
	private final Map<String, LogicalType> registered = new HashMap<>();
	private final ObjectMapper MAPPER;
	private final List<PluginDefinition> userDefinedPlugins = new ArrayList<>();

	Plugins(final ObjectMapper mapper) {
		this.MAPPER = mapper;
	}

	/**
	 * Register a new set of Plugins by providing JSON Plugin definitions.
	 *
	 * @deprecated  Replaced by {@link registerPlugins(Reader, String, AnalysisConfig, boolean)}
	 *
	 * @param JSON The definition of the plugins.
	 * @param dataStreamName The name of the datastream.
	 * @param analysisConfig The Analysis configuration used for this analysis.
	 *
	 * @throws IOException if the JSON cannot be parsed.
	 * @throws FTAPluginException if the plugin definition is invalid or if a plugin with the same semantic type is already registered.
	 */
	 @Deprecated public void registerPlugins(final Reader JSON, final String dataStreamName, final AnalysisConfig analysisConfig) throws IOException, FTAPluginException {
		registerPluginListCore(MAPPER.readValue(JSON, new TypeReference<List<PluginDefinition>>(){}), analysisConfig, false, false);
	}

	/**
	 * Register a new set of Plugins by providing JSON Plugin definitions.
	 *
	 * @deprecated  Replaced by {@link registerPlugins(Reader, AnalysisConfig, boolean)}
	 *
	 * @param JSON The definition of the plugins.
	 * @param dataStreamName The name of the datastream.
	 * @param analysisConfig The Analysis configuration used for this analysis.
	 * @param preBuiltins True if these are to be registered ahead of the pre-builtin plugins.
	 *
	 * @throws IOException if the JSON cannot be parsed.
	 * @throws FTAPluginException if the plugin definition is invalid or if a plugin with the same semantic type is already registered.
	 */
	 @Deprecated public void registerPlugins(final Reader JSON, final String dataStreamName, final AnalysisConfig analysisConfig, final boolean preBuiltins) throws IOException, FTAPluginException {
		registerPluginListCore(MAPPER.readValue(JSON, new TypeReference<List<PluginDefinition>>(){}), analysisConfig, false, preBuiltins);
	}

	/**
	 * Register a new set of Plugins by providing JSON Plugin definitions.
	 *
	 * @param JSON The definition of the plugins.
	 * @param analysisConfig The Analysis configuration used for this analysis.
	 * @param preBuiltins True if these are to be registered ahead of the pre-builtin plugins.
	 *
	 * @throws IOException if the JSON cannot be parsed.
	 * @throws FTAPluginException if the plugin definition is invalid or if a plugin with the same semantic type is already registered.
	 */
	public void registerPlugins(final Reader JSON, final AnalysisConfig analysisConfig, final boolean preBuiltins) throws IOException, FTAPluginException {
		registerPluginListCore(MAPPER.readValue(JSON, new TypeReference<List<PluginDefinition>>(){}), analysisConfig, false, preBuiltins);
	}

	/**
	 * Register a set of Plugins by providing a list of Plugin definitions.
	 *
	 * @deprecated  Replaced by {@link registerPluginList(List<PluginDefinition>, AnalysisConfig, boolean)}
	 *
	 * @param plugins The list of PluginDefinitions.
	 * @param dataStreamName The name of the datastream.
	 * @param analysisConfig The Analysis configuration used for this analysis.
	 *
	 * @throws FTAPluginException if the plugin definitions are invalid or if a plugin with the same semantic type is already registered.
	 */
	@Deprecated public void registerPluginList(final List<PluginDefinition> plugins, final String dataStreamName, final AnalysisConfig analysisConfig) throws FTAPluginException {
		registerPluginListCore(plugins, analysisConfig, false, false);
	}

	/**
	 * Register a set of Plugins by providing a list of existing user-defined Plugin definitions.
	 *
	 * @param plugins The list of PluginDefinitions.
	 * @param analysisConfig The Analysis configuration used for this analysis.
	 *
	 * @throws FTAPluginException if the plugin definitions are invalid or if a plugin with the same semantic type is already registered.
	 */
	protected void registerPluginListWithPrecedence(final List<PluginDefinition> plugins, final AnalysisConfig analysisConfig) throws FTAPluginException {
		for (final PluginDefinition plugin : plugins)
			registerPluginListCore(List.of(plugin), analysisConfig, false, plugin.getPrecedence() == PluginDefinition.Precedence.PRE_BUILTIN);
	}

	/**
	 * Register a set of Plugins by providing a list of Plugin definitions.
	 *
	 * @deprecated  Replaced by {@link registerPluginList(List<PluginDefinition>, AnalysisConfig, boolean)}
	 *
	 * @param plugins The list of PluginDefinitions.
	 * @param dataStreamName The name of the datastream.
	 * @param analysisConfig The Analysis configuration used for this analysis.
	 * @param preBuiltins True if these are to be registered ahead of the pre-builtin plugins.
	 *
	 * @throws FTAPluginException if the plugin definitions are invalid or if a plugin with the same semantic type is already registered.
	 */
	@Deprecated
	public void registerPluginList(final List<PluginDefinition> plugins, final String dataStreamName, final AnalysisConfig analysisConfig, final boolean preBuiltins) throws FTAPluginException {
		registerPluginListCore(plugins, analysisConfig, false, preBuiltins);
	}

	/**
	 * Register a set of Plugins by providing a list of Plugin definitions.
	 *
	 * @param plugins The list of PluginDefinitions.
	 * @param analysisConfig The Analysis configuration used for this analysis.
	 * @param preBuiltins True if these are to be registered ahead of the pre-builtin plugins.
	 *
	 * @throws FTAPluginException if the plugin definitions are invalid or if a plugin with the same semantic type is already registered.
	 */
	public void registerPluginList(final List<PluginDefinition> plugins, final AnalysisConfig analysisConfig, final boolean preBuiltins) throws FTAPluginException {
		registerPluginListCore(plugins, analysisConfig, false, preBuiltins);
	}

	protected void registerPluginsInternal(final List<PluginDefinition> plugins, final String dataStreamName, final AnalysisConfig analysisConfig) throws FTAPluginException {
		registerPluginListCore(plugins, analysisConfig, true, false);
	}

	protected void registerPluginListCore(final List<PluginDefinition> plugins, final AnalysisConfig analysisConfig, final boolean internal, final boolean preBuiltins) throws FTAPluginException {
		// Only register plugins that are valid for this locale
		for (final PluginDefinition plugin : plugins) {
			if (plugin.priority > PluginDefinition.PRIORITY_MAX)
				throw new FTAPluginException("Semantic type: '" + plugin.semanticType + "' has invalid priority, priority must be <= " + PluginDefinition.PRIORITY_MAX);

			if (internal)
				plugin.setPrecedence(PluginDefinition.Precedence.BUILTIN);
			else
				if (preBuiltins)
					plugin.setPrecedence(PluginDefinition.Precedence.PRE_BUILTIN);
				else
					plugin.setPrecedence(PluginDefinition.Precedence.POST_BUILTIN);

			if (plugin.isLocaleSupported(analysisConfig.getLocale()))
				if ("java".equals(plugin.pluginType))
					registerLogicalTypeClass(plugin, analysisConfig);
				else if ("list".equals(plugin.pluginType))
					registerLogicalTypeFiniteSet(plugin, analysisConfig);
				else if ("regex".equals(plugin.pluginType))
					registerLogicalTypeRegExp(plugin, analysisConfig);
				else
					throw new FTAPluginException("Semantic type: '" + plugin.semanticType + "' unknown type.");

			if (!internal)
				userDefinedPlugins.add(plugin);
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
	private void registerLogicalTypeClass(final PluginDefinition plugin, final AnalysisConfig analysisConfig) throws FTAPluginException {
		final Class<?> newLogicalType;
		final Constructor<?> ctor;
		final Object logical;

		try {
			newLogicalType = Class.forName(plugin.clazz);
			ctor = newLogicalType.getConstructor(PluginDefinition.class);
			logical = ctor.newInstance(plugin);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | ClassNotFoundException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new FTAPluginException("Semantic type: " + plugin.clazz + " failure to instantiate/contstruct.", e);
		}

		if (!(logical instanceof LogicalType))
			throw new FTAPluginException("Semantic type: " + plugin.clazz + " does not appear to be a Semantic Type.");

		registerLogicalType((LogicalType)logical, analysisConfig);
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
	public Collection<LogicalType> getRegisteredSemanticTypes() {
		return new HashSet<>(registered.values());
	}

	/**
	 * Return the plugin associated with this named Semantic Type.
	 * @param semanticTypeName Name of this Semantic Type.
	 * @return A Collection of the currently registered Semantic Types.
	 */
	public LogicalType getRegistered(final String semanticTypeName) {
		return registered.get(semanticTypeName);
	}

	protected List<PluginDefinition> getUserDefinedPlugins() {
		return userDefinedPlugins;
	}
}
