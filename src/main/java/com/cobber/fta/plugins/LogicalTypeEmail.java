package com.cobber.fta.plugins;

import java.util.Locale;
import java.util.Map;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PatternInfo;
import com.cobber.fta.PatternInfo.Type;

/**
 * Plugin to detect Email Addresses.
 */
public class LogicalTypeEmail extends LogicalTypeInfinite {
//	public final static String EMAIL_REGEXP = "[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?";
	public final static String REGEXP = "[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}";

	@Override
	public boolean initialize(Locale locale) {
		threshold = 95;

		return true;
	}

	@Override
	public String getQualifier() {
		return "EMAIL";
	}

	@Override
	public Type getBaseType() {
		return PatternInfo.Type.STRING;
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public boolean isRegExpComplete() {
		return true;
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
		return atSigns - 1 == charCounts[','] || atSigns - 1 == charCounts[';'];
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, Map<String, Integer> cardinality, Map<String, Integer> outliers) {
		return (double)matchCount/realSamples >= getThreshold()/100.0 ? null : ".+";
	}
}
