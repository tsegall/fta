package com.cobber.fta.plugins;

import java.util.Locale;
import java.util.Map;

import org.apache.commons.validator.routines.InetAddressValidator;

import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PatternInfo;
import com.cobber.fta.PatternInfo.Type;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.StringFacts;

public class LogicalTypeIPAddress extends LogicalTypeInfinite {
	public final static String SEMANTIC_TYPE = "IPADDRESS.IPV4";
	public final static String REGEXP = "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
	static InetAddressValidator validator = null;
	static {
		validator = InetAddressValidator.getInstance();
	}

	public LogicalTypeIPAddress(PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(Locale locale) {
		super.initialize(locale);

		threshold = 99;

		return true;
	}

	@Override
	public String nextRandom() {
		StringBuffer ret = new StringBuffer(36);

		ret.append(random.nextInt(256));
		ret.append('.');
		ret.append(random.nextInt(256));
		ret.append('.');
		ret.append(random.nextInt(256));
		ret.append('.');
		ret.append(random.nextInt(256));

		return ret.toString();
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
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
		return input.length() <= 15 && validator.isValidInet4Address(input);
	}

	@Override
	public boolean isCandidate(String trimmed, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		return trimmed.length() <= 15 && charCounts['.'] == 3;
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, StringFacts stringFacts,
			Map<String, Long> cardinality, Map<String, Long> outliers) {
		return (double) matchCount / realSamples >= getThreshold() / 100.0 ? null : ".+";
	}

}
