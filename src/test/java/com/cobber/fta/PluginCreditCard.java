package com.cobber.fta;

import java.util.Locale;
import java.util.Map;

import org.apache.commons.validator.routines.CreditCardValidator;

import com.cobber.fta.PatternInfo.Type;

public class PluginCreditCard extends LogicalTypeInfinite {
	static CreditCardValidator validator = null;
	static {
		validator = CreditCardValidator.genericCreditCardValidator();
	}

	@Override
	public boolean isCandidate(String input, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		return validator.isValid(input.replaceAll("[\\s\\-]", ""));
	}

	@Override
	public boolean initialize(Locale locale) {
		return true;
	}

	@Override
	public String getQualifier() {
		return "CREDITCARD";
	}

	@Override
	public String getRegexp() {
		return "(?:\\d[ -]*?){13,16}";
	}

	@Override
	public Type getBaseType() {
		return PatternInfo.Type.STRING;
	}

	@Override
	public boolean isValid(String input) {
		return validator.isValid(input.replaceAll("[\\s\\-]", ""));
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples,
			Map<String, Integer> cardinality, Map<String, Integer> outliers) {
		return (double)matchCount/realSamples >= getThreshold()/100.0 ? null : ".+";
	}
}
