package com.cobber.fta;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.cobber.fta.PatternInfo.Type;

public class LogicalTypeRegExp extends LogicalType {
	private String qualifier;
	private String[] hotWords;
	private String regExp;
	private Pattern pattern;
	private Type baseType;

	public LogicalTypeRegExp(String qualifier, String[] hotWords, String regExp, int threshold, Type baseType) {
		this.qualifier = qualifier;
		this.hotWords = hotWords;
		this.regExp = regExp;
		this.threshold = threshold;
		this.baseType = baseType;
	}

	@Override
	public boolean initialize(Locale locale) {
		try {
			pattern = Pattern.compile(regExp);
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}

	@Override
	public String getQualifier() {
		return qualifier;
	}

	@Override
	public String getRegExp() {
		return regExp;
	}

	@Override
	public boolean isRegExpComplete() {
		return true;
	}

	@Override
	public Type getBaseType() {
		return baseType;
	}

	@Override
	public boolean isValid(String input) {
		return pattern.matcher(input).matches();
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, Map<String, Integer> cardinality,
			Map<String, Integer> outliers) {

		return (double)matchCount / realSamples >= getThreshold()/100.0 ? null : regExp;
	}
}
