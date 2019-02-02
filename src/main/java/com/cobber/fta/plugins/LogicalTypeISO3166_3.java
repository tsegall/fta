package com.cobber.fta.plugins;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import com.cobber.fta.LogicalTypeFiniteSimple;

public class LogicalTypeISO3166_3 extends LogicalTypeFiniteSimple {
	public final static String REGEXP = "\\p{Alpha}{3}";
	private static Set<String> members = new HashSet<String>();

	public LogicalTypeISO3166_3() throws FileNotFoundException {
		super("ISO-3166-3", REGEXP, REGEXP,
				new InputStreamReader(LogicalTypeCAProvince.class.getResourceAsStream("/reference/ISO-3166-3.csv")), 95);
	}

	@Override
	public Set<String> getMembers() {
		return members;
	}
}