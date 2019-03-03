package com.cobber.fta.plugins;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import com.cobber.fta.LogicalTypeFiniteSimple;

/**
 * Plugin to detect ISO 4127 - Currency codes.
 */
public class LogicalTypeISO4217 extends LogicalTypeFiniteSimple {
	public final static String REGEXP = "\\p{Alpha}{3}";
	private static Set<String> members = new HashSet<String>();

	public LogicalTypeISO4217() throws FileNotFoundException {
		super("ISO-4217", new String[] { "4127", "currency" }, REGEXP,
				REGEXP, new InputStreamReader(LogicalTypeCAProvince.class.getResourceAsStream("/reference/ISO-4217.csv")), 95);
	}

	@Override
	public Set<String> getMembers() {
		return members;
	}
}
