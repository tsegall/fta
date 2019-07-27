package com.cobber.fta;

import java.io.FileNotFoundException;

public class PluginCUSIP extends LogicalTypeFiniteSimple {
	public final static String REGEXP = "[\\p{IsAlphabetic}\\d]{9}";

	public PluginCUSIP(PluginDefinition plugin) throws FileNotFoundException {
		super(plugin, REGEXP, "\\p{Alnum}{9}", 99);
		setContent("resource", "/CUSIP.txt");
	}
}
