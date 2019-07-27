package com.cobber.fta.plugins;

import java.util.Locale;
import java.util.Map;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.cobber.fta.LogicalTypeCode;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PatternInfo;
import com.cobber.fta.PatternInfo.Type;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.StringFacts;

/**
 * Plugin to detect Email Addresses.
 */
public class LogicalTypeEmail extends LogicalTypeInfinite {
	public final static String SEMANTIC_TYPE = "EMAIL";
//	public final static String EMAIL_REGEXP = "[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?";
	public final static String REGEXP = "[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}";
	private LogicalTypeCode logicalFirst = null;
	private LogicalTypeCode logicalLast = null;
	private static String[] mailDomains = new String[] {
			"gmail.com", "hotmail.com", "yahoo.com"
	};

	public LogicalTypeEmail(PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		if (logicalFirst == null) {
			logicalFirst = LogicalTypeCode.newInstance(new PluginDefinition("FIRST_NAME", "com.cobber.fta.plugins.LogicalTypeFirstName"), Locale.getDefault());
			logicalLast = LogicalTypeCode.newInstance(new PluginDefinition("LAST_NAME", "com.cobber.fta.plugins.LogicalTypeLastName"), Locale.getDefault());
		}
		return logicalFirst.nextRandom().toLowerCase() + "." + logicalLast.nextRandom().toLowerCase() + "@" + mailDomains[random.nextInt(mailDomains.length)];
	}

	@Override
	public boolean initialize(Locale locale) {
		super.initialize(locale);

		threshold = 95;

		return true;
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
		// This is not strictly correct since RFC 822 does not mandate an '@' but this is what mortals expect
		if (input.indexOf('@') == -1)
			return false;

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
	public boolean isCandidate(String trimmed, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		int atSigns = charCounts['@'];
		return atSigns - 1 == charCounts[','] || atSigns - 1 == charCounts[';'];
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, StringFacts stringFacts, Map<String, Long> cardinality, Map<String, Long> outliers) {
		return getConfidence(matchCount, realSamples, dataStreamName) >= getThreshold()/100.0 ? null : ".+";
	}

	@Override
	public double getConfidence(long matchCount, long realSamples, String dataStreamName) {
		double is = (double)matchCount/realSamples;
		if (matchCount != realSamples && getHeaderConfidence(dataStreamName) != 0)
			return is + (1.0 - is)/2;
		else
			return is;
	}
}
