package com.cobber.fta;

public class PluginDefinition {
	/** Name of Plugin - Qualifier. */
	public String qualifier;
	/** Type of plugin - infinite/finite/regexp. */
	public String type;
	/** locales this plugin applies to - empty set, implies all locales.  Can use just language instead of tag, e.g. "en" rather than "en_US". */
	public String[] locale;

	/* Infinite/Finite - class used to implement. */
	public String clazz;

	public String[] hotWords;
	public String regExp;
	public String maximumValue;
	public String minimumValue;
	public int threshold;
	public PatternInfo.Type baseType;
}
