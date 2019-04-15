package com.cobber.fta.plugins;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import com.cobber.fta.LogicalTypeFiniteSimple;
import com.cobber.fta.PluginDefinition;

/**
 * Plugin to detect Canadian Provinces.
 */
public class LogicalTypeCAProvince extends LogicalTypeFiniteSimple {
	public final static String SEMANTIC_TYPE = "STATE_PROVINCE.PROVINCE_CA";
	public final static String REGEXP = "\\p{Alpha}{2}";
	private static Set<String> members = new HashSet<String>();
	private static String[] membersArray = null;

	public LogicalTypeCAProvince(PluginDefinition plugin) throws FileNotFoundException {
		super(plugin, REGEXP,
				"\\p{IsAlphabetic}{2}", new InputStreamReader(LogicalTypeCAProvince.class.getResourceAsStream("/reference/ca_provinces.csv")), 95);
	}

	@Override
	public Set<String> getMembers() {
		return members;
	}

	@Override
	public String[] getMemberArray() {
		if (membersArray == null)
			membersArray = members.toArray(new String[members.size()]);
		return membersArray;
	}
}
