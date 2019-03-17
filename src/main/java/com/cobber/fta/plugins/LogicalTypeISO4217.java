package com.cobber.fta.plugins;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import com.cobber.fta.LogicalTypeFiniteSimple;
import com.cobber.fta.PluginDefinition;

/**
 * Plugin to detect ISO 4127 - Currency codes.
 */
public class LogicalTypeISO4217 extends LogicalTypeFiniteSimple {
	public final static String SEMANTIC_TYPE = "CURRENCY_CODE.ISO-4217";
	public final static String REGEXP = "\\p{Alpha}{3}";
	private static Set<String> members = new HashSet<String>();
	private static String[] membersArray = null;

	public LogicalTypeISO4217(PluginDefinition plugin) throws FileNotFoundException {
		super(plugin.qualifier, plugin.hotWords, plugin.regExp != null ? plugin.regExp : REGEXP,
				"\\p{IsAlphabetic}{3}", new InputStreamReader(LogicalTypeCAProvince.class.getResourceAsStream("/reference/ISO-4217.csv")), 95);
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
