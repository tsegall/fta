package com.cobber.fta.plugins;

import java.io.FileNotFoundException;

import com.cobber.fta.PluginDefinition;

public class LogicalTypeLastName extends LogicalTypePersonName {
	public LogicalTypeLastName(PluginDefinition plugin) throws FileNotFoundException {
		super(plugin, "lastnames.txt");
	}
}
