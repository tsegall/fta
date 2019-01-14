package com.cobber.fta;

import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class PluginCUSIP extends LogicalTypeFiniteSimple {
	private static Set<String> members = new HashSet<String>();

    public PluginCUSIP() {
    	super("CUSIP", "\\p{Alnum}{9}", "\\p{Alnum}{9}",
			new InputStreamReader(PluginCUSIP.class.getResourceAsStream("/CUSIP.txt")), 99);
	}

	@Override
	public Set<String> getMembers() {
		return members;
	}
}
