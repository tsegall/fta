package com.cobber.fta;

import java.util.Locale;
import java.util.Map;

import org.apache.commons.validator.routines.CreditCardValidator;

import com.cobber.fta.PatternInfo.Type;

public class PluginCreditCard extends LogicalTypeInfinite {
	public final static String REGEXP = "(?:\\d[ -]*?){13,16}";
	static CreditCardValidator validator = null;
	static {
		validator = CreditCardValidator.genericCreditCardValidator();
	}

	public PluginCreditCard(PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(String trimmed, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		return validator.isValid(trimmed.replaceAll("[\\s\\-]", ""));
	}

	@Override
	public boolean initialize(Locale locale) {
		super.initialize(locale);

		return true;
	}

	@Override
	public String nextRandom() {
		return null;
	}

	@Override
	public String getQualifier() {
		return "CREDITCARD";
	}

	@Override
	public String getRegExp() {
		return REGEXP;
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
			StringFacts stringFacts, Map<String, Integer> cardinality, Map<String, Integer> outliers) {
		return (double)matchCount/realSamples >= getThreshold()/100.0 ? null : ".+";
	}
}
