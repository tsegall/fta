package com.cobber.fta;

public class PluginDefinition {
	/** Semantic Type of Plugin (Qualifier). */
	public String qualifier;
	/** Type of plugin - infinite/finite/regexp. */
	public String type;
	/** locales this plugin applies to - empty set, implies all locales.  Can use just language instead of tag, e.g. "en" rather than "en_US". */
	public String[] locale;

	/* For plugins of type Infinite/Finite this is the class used to implement. */
	public String clazz;

	/** hotWords An array of Strings that will be compared with the datastream name to boost confidence. */
	public String[] hotWords;
	public boolean hotWordMandatory;
	/** The RegExp to be matched to qualify as this Logical Type. */
	public String regExp;
	/** The required threshold to be matched (can be adjusted by presence of Hot Words. */
	public int threshold;
	/** The underlying base type (e.g. STRING, DOUBLE, LONG, DATE, ... */
	public PatternInfo.Type baseType;
	public String minimum;
	public String maximum;
}
