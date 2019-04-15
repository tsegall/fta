package com.cobber.fta.plugins;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import com.cobber.fta.LogicalTypeFiniteSimple;
import com.cobber.fta.PluginDefinition;

/**
 * Plugin to detect ISO 3166-3 - three letter country codes.
 */
public class LogicalTypeISO3166_3 extends LogicalTypeFiniteSimple {
	public final static String SEMANTIC_TYPE = "COUNTRY.ISO-3166-3";
	public final static String REGEXP = "\\p{Alpha}{3}";
	private static Set<String> members = new HashSet<String>();
	private static String[] membersArray = null;

	public LogicalTypeISO3166_3(PluginDefinition plugin) throws FileNotFoundException {
		super(plugin, REGEXP,
				"\\p{IsAlphabetic}{3}", new InputStreamReader(LogicalTypeCAProvince.class.getResourceAsStream("/reference/ISO-3166-3.csv")), 95);
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