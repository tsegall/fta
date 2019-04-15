package com.cobber.fta;

/**
 * Definition of a Plugin.
 * There are three distinct types of plugins supported:
 * 	- Finite - implementation is via providing a subclass of LogicalTypeFinite (or a child thereof, e.g. LogicalTypeFiniteSimple)
 *  - Infinite - implementation is via providing a subclass of LogicalTypeInfinite (or a child thereof)
 *  - RegExp - implementation is based on the detection of the supplied RegExp with a provided set of constraints.
 */
public class PluginDefinition {
	/** Semantic Type of Plugin (Qualifier). */
	public String qualifier;
	/** locales this plugin applies to - empty set, implies all locales.  Can use just language instead of tag, e.g. "en" rather than "en_US". */
	public String[] validLocales;
	/** The relative priority of this plugin. */
	public int priority = 1000;

	/* For plugins of type Infinite/Finite this is the class used to implement. */
	public String clazz;

	/** The RegExp to be matched to qualify as this Logical Type. */
	public String regExp;
	/** hotWords An array of Strings that will be compared with the datastream name to boost confidence. */
	public String[] hotWords;
	/** Must one of the HotWords be present? */
	public boolean hotWordMandatory;
	/** The required threshold to be matched (can be adjusted by presence of Hot Words. */
	public int threshold;
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

	public PluginDefinition(String qualifier, String regExp, String[] validLocales, String[] hotWords, boolean hotWordMandatory, int threshold, PatternInfo.Type  baseType) {
		this.qualifier = qualifier;
		this.regExp = regExp;
		this.validLocales = validLocales;
		this.hotWords = hotWords;
		this.hotWordMandatory = hotWordMandatory;
		this.threshold = threshold;
		this.baseType = baseType;
	}
}
