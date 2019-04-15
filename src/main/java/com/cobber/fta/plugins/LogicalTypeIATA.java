package com.cobber.fta.plugins;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import com.cobber.fta.LogicalTypeFiniteSimple;
import com.cobber.fta.PluginDefinition;

/**
 * Plugin to detect IATA Airport Codes.
 */
public class LogicalTypeIATA  extends LogicalTypeFiniteSimple {
	public final static String SEMANTIC_TYPE = "AIRPORT_CODE.IATA";
	public final static String REGEXP = "\\p{Alpha}{3}";
	private static Set<String> members = new HashSet<String>();
	private static String[] membersArray = null;

	public LogicalTypeIATA(PluginDefinition plugin) throws FileNotFoundException {
		super(plugin, REGEXP,
				"\\p{IsAlphabetic}{3}", new InputStreamReader(LogicalTypeCAProvince.class.getResourceAsStream("/reference/IATA.txt")), 95);
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
