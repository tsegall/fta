package com.cobber.fta.plugins;

import java.util.HashSet;
import java.util.Set;

import com.cobber.fta.LogicalTypeFinite;

public class LogicalTypeGender extends LogicalTypeFinite {
	private static Set<String> members;

	static {
		members = new HashSet<String>();	
		members.add("FEMALE");
		members.add("MALE");
	}

	@Override
	public Set<String> getMembers() {
		return members;
	}

	@Override
	public String getQualifier() {
		return "GENDER_EN";
	}

	@Override
	public String getRegexp() {
		return 	"\\p{Alpha}+";
	}

	@Override
	public double getSampleThreshold() {
		// TODO Auto-generated method stub
		return 0;
	}
}
