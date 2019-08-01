package com.cobber.fta.plugins;

import java.io.FileNotFoundException;

import com.cobber.fta.PluginDefinition;

public class LogicalTypeFirstName extends LogicalTypePersonName {
	public final static String SEMANTIC_TYPE = "NAME.FIRST";

	public LogicalTypeFirstName(PluginDefinition plugin) throws FileNotFoundException {
		super(plugin, "firstnames.txt");
	}
}
