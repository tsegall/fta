package com.cobber.fta.plugins;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import com.cobber.fta.LogicalTypeFiniteSimple;

public class LogicalTypeUSState extends LogicalTypeFiniteSimple {
	public final static String REGEXP = "\\p{Alpha}{2}";
	private static Set<String> members = new HashSet<String>();

	public LogicalTypeUSState() throws FileNotFoundException {
		super("US_STATE", REGEXP, REGEXP,
				new InputStreamReader(LogicalTypeCAProvince.class.getResourceAsStream("/reference/us_states.csv")), 95);
	}

	@Override
	public Set<String> getMembers() {
		return members;
	}
}
