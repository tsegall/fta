package com.cobber.fta.plugins;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;

import com.cobber.fta.LogicalTypeFiniteSimple;

public class LogicalTypeISO4217 extends LogicalTypeFiniteSimple {
	public LogicalTypeISO4217() throws FileNotFoundException {
		super("ISO-4217", "\\p{Alpha}{3}", "\\p{Alpha}{3}",
				new InputStreamReader(LogicalTypeCAProvince.class.getResourceAsStream("/reference/ISO-4217.csv")), 95);
	}
}
