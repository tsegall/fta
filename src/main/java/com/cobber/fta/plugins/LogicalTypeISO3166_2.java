package com.cobber.fta.plugins;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import com.cobber.fta.LogicalTypeFiniteSimple;

/**
 * Plugin to detect ISO 3166-2 - two letter country codes.
 */
public class LogicalTypeISO3166_2 extends LogicalTypeFiniteSimple {
	public final static String REGEXP = "\\p{Alpha}{2}";
	private static Set<String> members = new HashSet<String>();

	public LogicalTypeISO3166_2() throws FileNotFoundException {
		super("ISO-3166-2", REGEXP, REGEXP,
				new InputStreamReader(LogicalTypeCAProvince.class.getResourceAsStream("/reference/ISO-3166-2.csv")), 95);
	}

	@Override
	public Set<String> getMembers() {
		return members;
	}
}