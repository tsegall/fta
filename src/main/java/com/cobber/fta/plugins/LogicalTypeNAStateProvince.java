package com.cobber.fta.plugins;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import com.cobber.fta.LogicalTypeFiniteSimple;
import com.cobber.fta.PluginDefinition;

/**
 * Plugin to detect NA States/Provinces
 */
public class LogicalTypeNAStateProvince extends LogicalTypeFiniteSimple {
	public final static String SEMANTIC_TYPE = "STATE_PROVINCE.STATE_PROVINCE_NA";
	public final static String REGEXP = "\\p{Alpha}{2}";
	private static Set<String> members = new HashSet<String>();
	private static String[] membersArray = null;

	public LogicalTypeNAStateProvince(PluginDefinition plugin) throws FileNotFoundException {
		super(plugin, REGEXP,
				"\\p{IsAlphabetic}{2}", new InputStreamReader(LogicalTypeCAProvince.class.getResourceAsStream("/reference/na_states_provinces.csv")), 95);
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
