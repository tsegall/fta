package com.cobber.fta.plugins;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import com.cobber.fta.LogicalTypeFiniteSimple;

public class LogicalTypeCAProvince extends LogicalTypeFiniteSimple {
	private static Set<String> members = new HashSet<String>();

	public LogicalTypeCAProvince() throws FileNotFoundException {
		super("CA_PROVINCE", "\\p{Alpha}{2}", "\\p{Alpha}{2}",
				new InputStreamReader(LogicalTypeCAProvince.class.getResourceAsStream("/reference/ca_provinces.csv")), 95);
	}

	@Override
	public Set<String> getMembers() {
		return members;
	}
}
