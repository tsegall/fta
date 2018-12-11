package com.cobber.fta.plugins;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PatternInfo;
import com.cobber.fta.PatternInfo.Type;

public class LogicalTypeEmail extends LogicalTypeInfinite {

	@Override
	public boolean initialize() {
		return true;
	}

	@Override
	public String getQualifier() {
		return "EMAIL";
	}

	@Override
	public double getSampleThreshold() {
		return 0.95;
	}

	@Override
	public Type getBaseType() {
		return PatternInfo.Type.STRING;
	}

	@Override
	public String getRegexp() {
		return "[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}";
	}

	@Override
	public boolean isValid(String input) {
		// Address lists commonly have ;'s as separators as opposed to the
		// ','
		if (input.indexOf(';') != -1)
			input = input.replaceAll(";", ",");
		try {
			return InternetAddress.parse(input).length != 0;
		} catch (AddressException e) {
			return false;
		}
	}

	@Override
	public boolean isCandidate(String input, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		int atSigns = charCounts['@'];
		boolean result = atSigns - 1 == charCounts[','] || atSigns - 1 == charCounts[';'];
		if (result)
			candidateCount++;
		return result;
	}
}
