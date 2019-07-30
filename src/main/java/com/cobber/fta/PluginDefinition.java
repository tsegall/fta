package com.cobber.fta;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

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
	private static List<PluginDefinition> builtinPlugins = null;

	/** Semantic Type of Plugin (Qualifier). */
	public String qualifier;
	/** locales this plugin applies to - empty set, implies all locales.  Can use just language instead of tag, e.g. "en" rather than "en_US". */
	public String[] validLocales;
	/** The relative priority of this plugin. */
	public int priority = 1000;

	/** Infinite/Finite plugins: this is the class used to implement. */
	public String clazz;
	/** RegExp plugins: the RegExps to be matched to qualify as this Logical Type. */
	public String[] regExpsToMatch;
	/** RegExp plugins: the RegExp to be returned for this Logical Type. */
	public String regExpReturned;
	/** Simple finite plugins: the content with the set of valid elements. */
	public String content;
	public String contentType;
	public String backout;

	/** headerRegExps An array of RegExps that will be compared with the datastream name to boost confidence. */
	public String[] headerRegExps;
	public int[] headerRegExpConfidence;

	/** The required threshold to be matched (can be adjusted by presence of Hot Words. */
	public int threshold = 95;
	/** The underlying base type (e.g. STRING, DOUBLE, LONG, DATE, ... */
	public PatternInfo.Type baseType;
	/** Minimum value to be considered as a valid instance of this type, e.g. 1 if the Semantic type is Financial Quarter. */
	public String minimum;
	/** Maximum value to be considered as a valid instance of this type, e.g. 4 if the Semantic type is Financial Quarter. */
	public String maximum;

	public PluginDefinition() {
	}

	public PluginDefinition(String qualifier, String clazz) {
		this.qualifier = qualifier;
		this.clazz = clazz;
	}

	public PluginDefinition(String qualifier, String regExpReturned, String[] regExpsToMatch, String content, String contentType, String backout, String[] validLocales, String[] headerRegExps, int[] headerRegExpConfidence, int threshold, PatternInfo.Type  baseType) {
		this.qualifier = qualifier;
		this.regExpReturned = regExpReturned;
		this.regExpsToMatch = regExpsToMatch == null ? new String[] { regExpReturned } : regExpsToMatch;
		this.content = content;
		this.contentType = contentType;
		this.backout = backout;
		this.validLocales = validLocales;
		this.headerRegExps = headerRegExps;
		this.headerRegExpConfidence = headerRegExpConfidence;
		this.threshold = threshold;
		this.baseType = baseType;
	}

	static PluginDefinition findByQualifier(String qualifier) {
		synchronized (PluginDefinition.class) {
			if (builtinPlugins == null) {
				try (BufferedReader JSON = new BufferedReader(new InputStreamReader(PluginDefinition.class.getResourceAsStream("/reference/plugins.json")))) {
					builtinPlugins = (new ObjectMapper()).readValue(JSON, new TypeReference<List<PluginDefinition>>(){});
				} catch (Exception e) {
					throw new IllegalArgumentException("Internal error: Issues with plugins file: " + e.getMessage(), e);
				}
			}
		}

		for (PluginDefinition pluginDefinition : builtinPlugins)
			if (pluginDefinition.qualifier.equals(qualifier))
				return pluginDefinition;

		return null;
	}
}
