package com.cobber.fta.plugins;

import java.io.FileNotFoundException;

import com.cobber.fta.LogicalTypeFiniteSimple;
import com.cobber.fta.PluginDefinition;

/**
 * Plugin to detect NA States/Provinces
 */
public class LogicalTypeNAStateProvince extends LogicalTypeFiniteSimple {
	public final static String SEMANTIC_TYPE = "STATE_PROVINCE.STATE_PROVINCE_NA";
	public final static String REGEXP = "\\p{Alpha}{2}";

	public LogicalTypeNAStateProvince(PluginDefinition plugin) throws FileNotFoundException {
		super(plugin, REGEXP,
				"\\p{IsAlphabetic}{2}", 95);
		setContent("resource", "/reference/na_states_provinces.csv");
	}
}
