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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.HeaderEntry;
import com.cobber.fta.core.InternalErrorException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Definition of a Plugin.
 * There are three distinct types of plugins supported:
 * 	- Finite - implementation is via providing a subclass of LogicalTypeFinite (or a child thereof, e.g. LogicalTypeFiniteSimple)
 *  - Infinite - implementation is via providing a subclass of LogicalTypeInfinite (or a child thereof)
 *  - RegExp - implementation is based on the detection of the supplied RegExp with a provided set of constraints.
 */
public class PluginDefinition {
	/** Priority of plugins must be between 0 and PRIORITY_MAX. */
	public final static int PRIORITY_MAX = 10000;

	private static List<PluginDefinition> builtinPlugins;

	/** Semantic Type name of Plugin. */
	public String semanticType;
	/** English language description of the Semantic Type. */
	public String description;
	/** Type of the plugin - can be 'java', 'list', or 'regex' */
	public String pluginType;
	/** Plugin-specific options - format key1=value1, key2=value2, ... */
	public String pluginOptions;
	/** Signature (structure) - the MD5 Hash of the Semantic Type name and the Base Type. */
	public String signature;
	/** locales this plugin applies to - empty set, implies all locales.  Can use just language instead of tag, e.g. "en" rather than "en_US". */
	public PluginLocaleEntry[] validLocales;

	public PluginDocumentationEntry[] documentation;

	/** Is this plugin sensitive to the input locale? */
	public boolean localeSensitive;
	/** The relative priority of this plugin. */
	public int priority = 0;

	/** Infinite/Finite plugins: this is the class used to implement. */
	public String clazz;
	/** RegExp plugins: a set of strings that match the regExp but are known to be invalid. */
	public Set<String> invalidList;
	/** Simple finite plugins: a set of strings that we should ignore when checking the known good list. */
	public Set<String> ignoreList;
	/** Simple finite plugins: the content with the set of valid elements. */
	public Content content;
	public String backout;

	/** The required threshold to be matched (can be adjusted by presence of Hot Words. */
	public int threshold = 95;
	/** The underlying base type (e.g. STRING, DOUBLE, LONG, DATE, ... */
	public FTAType baseType = FTAType.STRING;
	/** Minimum value to be considered as a valid instance of this type, e.g. 1 if the Semantic type is Financial Quarter. */
	public String minimum;
	/** Maximum value to be considered as a valid instance of this type, e.g. 4 if the Semantic type is Financial Quarter. */
	public String maximum;
	/** Minimum number of samples required to declare success. */
	public int minSamples = -1;
	/** Need to see both the minimum and maximum values to declare success. */
	public boolean minMaxPresent;

	private volatile Map<String, String> options = null;

	public enum Precedence {
	    BUILTIN,
	    PRE_BUILTIN,
	    POST_BUILTIN
	}
	protected Precedence precedence = Precedence.BUILTIN;

	public PluginDefinition() {
	}

	/*
	 * Get the adjusted order value for this plugin.
	 *
	 * The order is based on a combination of the precedence and the priority:
	 *	0 - PRIORITY_MAX : reserved for user-defined plugins (preBuiltins == true)
	 *	PRIORITY_MAX+1 - 2*PRIORITY_MAX : reserved for built-in plugins
	 *	> 2*PRIORITY_MAX : reserved for user-defined plugins (preBuiltins == false)
	 *
	 * @return The order for this plugin.
	 */
	public int getOrder() {
		switch (precedence) {
			case BUILTIN:
				return priority + PluginDefinition.PRIORITY_MAX;
			case PRE_BUILTIN:
				return priority;
			case POST_BUILTIN:
				return priority + 2 * PluginDefinition.PRIORITY_MAX;
			default:
				return -1;
		}
	}

	public void setPrecedence(final Precedence precedence) {
		this.precedence = precedence;
	}

	// *** Only use this for internal testing - be warned only works in English language ***
	public PluginDefinition(final String semanticType, final String clazz) {
		this.semanticType = semanticType;
		this.clazz = clazz;
		this.pluginType = "java";
		this.validLocales = PluginLocaleEntry.simple(new String[] { "en" });
	}

	// *** Only use this for internal testing ***
	public PluginDefinition(final String semanticType, final String description, final String[] invalidList,
			final Content content, final String backout, final PluginLocaleEntry[] validLocales, final boolean localeSensitive,
			final int threshold, final FTAType  baseType) {
		this.semanticType = semanticType;
		this.description = description;
		this.pluginType = content != null ? "list" : "regex";
		this.invalidList = invalidList == null ? null : new HashSet<>(Arrays.asList(invalidList));
		this.content = content;
		this.backout = backout;
		this.validLocales = validLocales;
		this.localeSensitive = localeSensitive;
		this.threshold = threshold;
		this.baseType = baseType;
	}

	/**
	 * Copy constructor - creates a new PluginDefinition based on an existing one.
	 *
	 * @param other The PluginDefinition to copy from
	 */
	public PluginDefinition(final PluginDefinition other) {
		this.semanticType = other.semanticType;
		this.description = other.description;
		this.pluginType = other.pluginType;
		this.pluginOptions = other.pluginOptions;
		this.signature = other.signature;

		// Deep copy validLocales array
		if (other.validLocales != null) {
			this.validLocales = new PluginLocaleEntry[other.validLocales.length];
			for (int i = 0; i < other.validLocales.length; i++)
				this.validLocales[i] = new PluginLocaleEntry(other.validLocales[i]);
		}

		this.documentation = other.documentation;
		this.localeSensitive = other.localeSensitive;
		this.priority = other.priority;
		this.clazz = other.clazz;
		this.invalidList = other.invalidList == null ? null : new HashSet<>(other.invalidList);
		this.ignoreList = other.ignoreList == null ? null : new HashSet<>(other.ignoreList);
		this.content = other.content;
		this.backout = other.backout;
		this.threshold = other.threshold;
		this.baseType = other.baseType;
		this.minimum = other.minimum;
		this.maximum = other.maximum;
		this.minSamples = other.minSamples;
		this.minMaxPresent = other.minMaxPresent;
		this.precedence = other.precedence;
	}

	/**
	 * Retrieve the Plugin Definition associated with this Semantic Type name.
	 *
	 * @param semanticTypeName The name for this Semantic Type
	 * @return The Plugin Definition associated with the supplied name.
	 */
	public static PluginDefinition findByName(final String semanticTypeName) {
		synchronized (PluginDefinition.class) {
			if (builtinPlugins == null)
				try (BufferedReader JSON = new BufferedReader(new InputStreamReader(PluginDefinition.class.getResourceAsStream("/reference/plugins.json"), StandardCharsets.UTF_8))) {
					builtinPlugins = new ObjectMapper().readValue(JSON, new TypeReference<List<PluginDefinition>>(){});
				} catch (Exception e) {
					throw new InternalErrorException("Issues with reference plugins file", e);
				}
		}

		for (final PluginDefinition pluginDefinition : builtinPlugins)
			if (pluginDefinition.semanticType.equalsIgnoreCase(semanticTypeName))
				return pluginDefinition;

		return null;
	}

	public boolean isMandatoryHeaderUnsatisfied(final Locale locale, final String streamName) {
		PluginLocaleEntry localeEntry;
		try {
			localeEntry = getLocaleEntry(locale);
		} catch (FTAPluginException e) {
			// Should never happen - since we should have bailed earlier
			return false;
		}

		if (localeEntry.headerRegExps == null)
			return false;

		// If there are any mandatory entries - then they are effectively all mandatory (and one must be present)
		boolean mandatory = false;
		for (final HeaderEntry entry : localeEntry.headerRegExps) {
			if (entry.mandatory)
				mandatory = true;

			if (entry.matches(streamName))
				return entry.confidence < 0;
		}

		return mandatory;

	}

	public PluginLocaleEntry getLocaleEntry(final Locale locale) throws FTAPluginException {
		if (validLocales == null)
			throw new FTAPluginException("plugin: " + semanticType + " - validLocales cannot be null");

		final String languageTag = locale.toLanguageTag();
		final String language = locale.getLanguage();

		PluginLocaleEntry wildcard = null;

		for (final PluginLocaleEntry validLocale : validLocales) {
			final String[] localeTags = validLocale.localeTag.split(",");
			for (final String localeTag : localeTags) {
				if ("*".equals(localeTag)) {
					wildcard = validLocale;
					continue;
				}
				if (localeTag.indexOf('-') != -1) {
					if (localeTag.equals(languageTag))
						return validLocale;
				}
				else if (localeTag.equals(language))
					return validLocale;
			}
		}

		return wildcard;
	}

	public boolean isLocaleSupported(final Locale locale) throws FTAPluginException {
		return getLocaleEntry(locale) != null;
	}

	public String getLocaleDescription() {
		final StringBuilder ret = new StringBuilder();
		for (final PluginLocaleEntry validLocale : validLocales)
			ret.append(validLocale.toString());

		return ret.toString();
	}

	public Map<String, String> getOptions() {
		if (options == null)
			synchronized(this) {
				if (options == null) {
					options = new HashMap<>();
					if (pluginOptions != null) {
						final String[] entries = pluginOptions.split("\\s*,\\s*");
						for (final String entry : entries) {
							final int separator = entry.indexOf('=');
							options.put(entry.substring(0,separator), entry.substring(separator + 1));
						}
					}
				}
			}

		return options;
	}
}
