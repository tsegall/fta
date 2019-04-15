package com.cobber.fta;

import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class PluginCUSIP extends LogicalTypeFiniteSimple {
	public final static String REGEXP = "[\\p{IsAlphabetic}\\d]{9}";
	private static Set<String> members = new HashSet<String>();
	private static String[] membersArray = null;

	public PluginCUSIP(PluginDefinition plugin) {
		super(plugin, REGEXP, "\\p{Alnum}{9}",
				new InputStreamReader(PluginCUSIP.class.getResourceAsStream("/CUSIP.txt")), 99);
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
