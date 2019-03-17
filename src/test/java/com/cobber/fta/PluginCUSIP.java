package com.cobber.fta;

import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class PluginCUSIP extends LogicalTypeFiniteSimple {
	private static Set<String> members = new HashSet<String>();
	private static String[] membersArray = null;

	public PluginCUSIP(PluginDefinition plugin) {
		super(plugin.qualifier, plugin.hotWords, plugin.regExp, "\\p{Alnum}{9}",
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
