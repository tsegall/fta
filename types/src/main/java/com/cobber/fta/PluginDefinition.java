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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
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
	/** Externally registered plugins can have a priority no lower than this. */
	public final static int PRIORITY_EXTERNAL = 2000;

	private static List<PluginDefinition> builtinPlugins;

	/** Semantic Type of Plugin (Qualifier). */
	public String qualifier;
	/** English language description of the Semantic Type. */
	public String description;
	/** Signature (structure) - the MD5 Hash of the Qualifier and the Base Type. */
	public String signature;
	/** locales this plugin applies to - empty set, implies all locales.  Can use just language instead of tag, e.g. "en" rather than "en_US". */
	public PluginLocaleEntry[] validLocales;

	/** Is this plugin sensitive to the input locale? */
	public boolean localeSensitive;
	/** The relative priority of this plugin. */
	public int priority = PRIORITY_EXTERNAL;

	/** Infinite/Finite plugins: this is the class used to implement. */
	public String clazz;
	/** RegExp plugins: a set of strings that match the regExp but are known to be invalid. */
	public Set<String> invalidList;
	/** Simple finite plugins: the content with the set of valid elements. */
	public String content;
	/** ContentType describes the supplied content and must be one of 'inline', 'file' or 'resource'. */
	public String contentType;
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

	public PluginDefinition() {
	}

	// Only use this for internal testing
	protected PluginDefinition(final String qualifier, final String clazz) {
		this.qualifier = qualifier;
		this.clazz = clazz;
		this.validLocales = PluginLocaleEntry.simple(new String[] { "en" });
	}

	public PluginDefinition(final String qualifier, final String description, final String[] invalidList,
			final String content, final String contentType, final String backout, final PluginLocaleEntry[] validLocales, final boolean localeSensitive,
			final int threshold, final FTAType  baseType) {
		this.qualifier = qualifier;
		this.description = description;
		this.invalidList = invalidList == null ? null : new HashSet<>(Arrays.asList(invalidList));
		this.content = content;
		this.contentType = contentType;
		this.backout = backout;
		this.validLocales = validLocales;
		this.localeSensitive = localeSensitive;
		this.threshold = threshold;
		this.baseType = baseType;
	}

	/**
	 * Retrieve the Plugin Definition associated with this Qualifier.
	 *
	 * @param qualifier The Qualifier for this Logical Type
	 * @return The Plugin Definition associated with the supplied Qualifier.
	 */
	public static PluginDefinition findByQualifier(final String qualifier) {
		synchronized (PluginDefinition.class) {
			if (builtinPlugins == null) {
				try (BufferedReader JSON = new BufferedReader(new InputStreamReader(PluginDefinition.class.getResourceAsStream("/reference/plugins.json"), StandardCharsets.UTF_8))) {
					builtinPlugins = new ObjectMapper().readValue(JSON, new TypeReference<List<PluginDefinition>>(){});
				} catch (Exception e) {
					throw new InternalErrorException("Issues with reference plugins file", e);
				}
			}
		}

		for (final PluginDefinition pluginDefinition : builtinPlugins)
			if (pluginDefinition.qualifier.equals(qualifier))
				return pluginDefinition;

		return null;
	}

	public boolean isRequiredHeaderMissing(final Locale locale, final String streamName) {
		PluginLocaleEntry localeEntry;
		try {
			localeEntry = getLocaleEntry(locale);
		} catch (FTAPluginException e) {
			// Should never happen - since we should have bailed earlier
			return false;
		}

		if (localeEntry.headerRegExps == null)
			return false;

		for (PluginLocaleHeaderEntry entry : localeEntry.headerRegExps)
			if (entry.confidence == 100 && !entry.matches(streamName))
					return true;

		return false;

	}

	public PluginLocaleEntry getLocaleEntry(final Locale locale) throws FTAPluginException {
		final String languageTag = locale.toLanguageTag();
		final String language = locale.getLanguage();

		PluginLocaleEntry wildcard = null;

		if (validLocales == null)
			throw new FTAPluginException("plugin: " + qualifier + " - validLocales cannot be null");

		for (final PluginLocaleEntry validLocale : validLocales) {
			String[] localeTags = validLocale.localeTag.split(",");
			for (final String localeTag : localeTags) {
				if ("*".equals(localeTag))
					wildcard = validLocale;
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
		StringBuilder ret = new StringBuilder();
		for (int i = 0; i < validLocales.length; i++)
			ret.append(validLocales[i].toString());

		return ret.toString();
	}
}
