package com.cobber.fta.plugins;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;

import com.cobber.fta.LogicalTypeFiniteSimple;

public class LogicalTypeISO3166_3 extends LogicalTypeFiniteSimple {
	public LogicalTypeISO3166_3() throws FileNotFoundException {
		super("ISO-3166-3", "\\p{Alpha}{3}", "\\p{Alpha}{3}",
				new InputStreamReader(LogicalTypeCAProvince.class.getResourceAsStream("/reference/ISO-3166-3.csv")), 95);
	}
}