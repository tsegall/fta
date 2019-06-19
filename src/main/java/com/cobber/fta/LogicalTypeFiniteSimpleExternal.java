package com.cobber.fta;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

public class LogicalTypeFiniteSimpleExternal extends LogicalTypeFiniteSimple {
	private Set<String> members = new HashSet<String>();
	private String[] membersArray = null;

	public LogicalTypeFiniteSimpleExternal(PluginDefinition plugin) throws FileNotFoundException {
		super(plugin, plugin.regExpReturned, plugin.backout, plugin.threshold);
		if (plugin.contentType.equals("inline"))
			setReader(new StringReader(plugin.content.replace('|', '\n')));
		else if (plugin.contentType.equals("file"))
			setReader(new InputStreamReader(new FileInputStream(plugin.content)));
		else if (plugin.contentType.equals("resource"))
			setReader(new InputStreamReader(LogicalTypeFiniteSimpleExternal.class.getResourceAsStream(plugin.content)));
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
