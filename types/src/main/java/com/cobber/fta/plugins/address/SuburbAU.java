package com.cobber.fta.plugins.address;

import com.cobber.fta.LogicalTypeBloomFilter;
import com.cobber.fta.PluginDefinition;

/**
 * Plugin to detect Australian Suburb names.
 */
public class SuburbAU extends LogicalTypeBloomFilter {

	public SuburbAU(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String getRegExp() {
		return "[' \\p{IsAlphabetic}]+";
	}
}
