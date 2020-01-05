/*
 * Copyright 2017-2020 Tim Segall
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

import java.io.FileNotFoundException;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Plugins {
	private Map<String, LogicalType> registered = new HashMap<String, LogicalType>();

	public void registerPlugins(Reader JSON, String dataStreamName, Locale locale) throws IOException, ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		ObjectMapper mapper = new ObjectMapper();
		registerPluginList(mapper.readValue(JSON, new TypeReference<List<PluginDefinition>>(){}), dataStreamName, locale);
	}

	public void registerPlugins(String JSON, String dataStreamName, Locale locale) throws IOException, ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		ObjectMapper mapper = new ObjectMapper();
		registerPluginList(mapper.readValue(JSON, new TypeReference<List<PluginDefinition>>(){}), dataStreamName, locale);
	}

	public void registerPluginList(List<PluginDefinition> plugins, String dataStreamName, Locale locale) throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, FileNotFoundException {
		if (locale == null)
			locale = Locale.getDefault();
		String languageTag = locale.toLanguageTag();
		String language = locale.getLanguage();

		// Only register plugins that are valid for this locale
		for (PluginDefinition plugin : plugins) {
			boolean register = false;

			// Check to see if this plugin is valid for this locale
			if (plugin.validLocales != null && plugin.validLocales.length != 0) {
				for (String validLocale : plugin.validLocales) {
					if (validLocale.indexOf('-') != -1) {
						if (validLocale.equals(languageTag)) {
							register = true;
							break;
						}
					}
					else if (validLocale.equals(language)) {
						register = true;
						break;
					}
				}
			}
			else
				register = true;

			// Check to see if this plugin requires a mandatory hotword (and it is present)
			if (register) {
				if (!"*".equals(dataStreamName) && plugin.headerRegExps != null) {
					for (int i = 0; i < plugin.headerRegExps.length && register; i++) {
						if (plugin.headerRegExpConfidence[i] == 100 && !dataStreamName.matches(plugin.headerRegExps[i]))
							register = false;
					}
				}
			}

			if (register)
				if (plugin.clazz != null)
					registerLogicalTypeClass(plugin, locale);
				else if (plugin.content != null)
					registerLogicalTypeFiniteSet(plugin, locale);
				else if (plugin.regExpReturned != null)
					registerLogicalTypeRegExp(plugin, locale);
				else
					throw new IllegalArgumentException("Logical type: '" + plugin.qualifier + "' unknown type.");
		}
	}

	private void registerLogicalType(LogicalType logical, Locale locale) {
		logical.initialize(locale);

		if (registered.containsKey(logical.getQualifier()))
			throw new IllegalArgumentException("Logical type: " + logical.getQualifier() + " already registered.");

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
	 */
	private void registerLogicalTypeClass(PluginDefinition plugin, Locale locale) throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Class<?> newLogicalType;
		Constructor<?> ctor;
		LogicalType logical;

		newLogicalType = Class.forName(plugin.clazz);
		ctor = newLogicalType.getConstructor(PluginDefinition.class);
		logical = (LogicalType)ctor.newInstance(plugin);

		if (!(logical instanceof LogicalType))
			throw new IllegalArgumentException("Logical type: " + plugin.clazz + " does not appear to be a Logical Type.");

		registerLogicalType(logical, locale);
	}

	/**
	 * Register a new Logical Type processor of type LogicalTypeRegExp. See {@link LogicalTypeRegExp}
	 *
	 * @param plugin The Plugin Definition for a RegExp Logical Type
	 * @param locale The current Locale
	 */
	private void registerLogicalTypeRegExp(PluginDefinition plugin, Locale locale) {
		registerLogicalType(new LogicalTypeRegExp(plugin), locale);
	}

	/**
	 * Register a new Logical Type processor of type LogicalTypeFiniteSimpleExternal. See {@link LogicalTypeFiniteSimpleExternal}
	 *
	 * @param plugin The Plugin Definition for a simple file-based Logical Type
	 * @param locale The current Locale
	 * @throws FileNotFoundException
	 */
	private void registerLogicalTypeFiniteSet(PluginDefinition plugin, Locale locale) throws FileNotFoundException {
		registerLogicalType(new LogicalTypeFiniteSimpleExternal(plugin), locale);
	}

	/**
	 * Return the set of registered Logical Types.
	 * @return A Collection of the currently registered Logical Types.
	 */
	public Collection<LogicalType> getRegisteredLogicalTypes() {
		return new HashSet<LogicalType>(registered.values());
	}

	/**
	 * Return the plugin associated with this named Logical Type.
	 * @param qualifier Name of this Logical Type.
	 * @return A Collection of the currently registered Logical Types.
	 */
	public LogicalType getRegistered(String qualifier) {
		return registered.get(qualifier);
	}
}
