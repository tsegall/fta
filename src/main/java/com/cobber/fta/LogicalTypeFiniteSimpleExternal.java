package com.cobber.fta;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class LogicalTypeFiniteSimpleExternal extends LogicalTypeFiniteSimple {
	private static Set<String> members = new HashSet<String>();
	private static String[] membersArray = null;

	public LogicalTypeFiniteSimpleExternal(PluginDefinition plugin) throws FileNotFoundException {
		super(plugin, plugin.regExpReturned,
				"\\p{IsAlphabetic}{2}", new InputStreamReader(new FileInputStream(plugin.filename)), 95);
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
