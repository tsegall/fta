package com.cobber.fta.plugins;

import java.util.Locale;
import java.util.Map;

import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PatternInfo;
import com.cobber.fta.PatternInfo.Type;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.StringFacts;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

/**
 * Plugin to detect Phone Numbers.
 */
public class LogicalTypePhoneNumber extends LogicalTypeInfinite  {
		public final static String SEMANTIC_TYPE = "PHONENUMBER";
		public final static String REGEXP = ".*";
		private PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		private static String[] areaCodes = new String[] { "617", "781", "303", "970", "212" };

		public LogicalTypePhoneNumber(PluginDefinition plugin) {
			super(plugin);
		}

		@Override
		public String nextRandom() {
			StringBuffer ret = new StringBuffer(20);
			ret.append("+1.").append(areaCodes[random.nextInt(areaCodes.length)]).append(".");
			for (int i = 0; i < 7; i++)
				ret.append(random.nextInt(10));

			return ret.toString();
		}

		@Override
		public boolean initialize(Locale locale) {
			super.initialize(locale);

			threshold = 80;

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
			return false;
		}

		@Override
		public boolean isValid(String input) {
			try {
				Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(input, locale.getCountry());
				return phoneUtil.isValidNumber(phoneNumber);
			}
	        catch (NumberParseException e) {
	        	return false;
	        }
		}

		@Override
		public boolean isCandidate(String trimmed, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
			try {
				Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(trimmed, locale.getCountry());
				return phoneUtil.isPossibleNumber(phoneNumber);
			}
	        catch (NumberParseException e) {
	        	return false;
	        }
		}

		@Override
		public String isValidSet(String dataStreamName, long matchCount, long realSamples, StringFacts stringFacts, Map<String, Integer> cardinality, Map<String, Integer> outliers) {
			return (double)matchCount/realSamples >= getThreshold()/100.0 ? null : REGEXP;
		}
}
