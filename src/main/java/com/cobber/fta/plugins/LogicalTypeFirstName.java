package com.cobber.fta.plugins;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import com.cobber.fta.PluginDefinition;

public class LogicalTypeFirstName extends LogicalTypePersonName {
	private static Set<String> members = new HashSet<String>();

	public LogicalTypeFirstName(PluginDefinition plugin) throws FileNotFoundException {
		super(plugin, "firstnames.txt");
	}

	@Override
	public Set<String> getMembers() {
		return members;
	}

	@Override
	public String[] getMemberArray() {
		return null;
	}
}
