package com.cobber.fta.plugins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import com.cobber.fta.LogicalTypeFinite;
import com.cobber.fta.TextAnalyzer;

public class LogicalTypeUSState extends LogicalTypeFinite {
	private static Set<String> members;

	static {
		members = new HashSet<String>();	
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/us_states.csv")))){
			String line = null;

			while ((line = reader.readLine()) != null) {
				members.add(line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Set<String> getMembers() {
		return members;
	}

	@Override
	public String getQualifier() {
		return "US_STATE";
	}

	@Override
	public String getRegexp() {
		return "\\p{Alpha}{2}";
	}

	@Override
	public double getSampleThreshold() {
		// TODO Auto-generated method stub
		return 0;
	}

}
