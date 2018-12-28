package com.cobber.fta.plugins;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;

import com.cobber.fta.LogicalTypeFiniteSimple;

public class LogicalTypeISO3166_2 extends LogicalTypeFiniteSimple {
	public LogicalTypeISO3166_2() throws FileNotFoundException {
		super("ISO-3166-2", "\\p{Alpha}{2}", "\\p{Alpha}{2}",
				new InputStreamReader(LogicalTypeCAProvince.class.getResourceAsStream("/reference/ISO-3166-2.csv")), 95);
	}
}