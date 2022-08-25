package com.cobber.fta.plugins;

import com.cobber.fta.LogicalTypeBloomFilter;
import com.cobber.fta.PluginDefinition;

public class SuburbAU extends LogicalTypeBloomFilter {

	public SuburbAU(PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String getRegExp() {
		return "[' \\p{IsAlphabetic}]+";
	}
}
