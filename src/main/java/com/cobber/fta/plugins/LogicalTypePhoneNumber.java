/*
 * Copyright 2017-2020 Tim Segall
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cobber.fta.plugins;

import java.util.Locale;
import java.util.Map;

import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.TypeFacts;
import com.cobber.fta.core.FTAType;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

/**
 * Plugin to detect Phone Numbers.
 */
public class LogicalTypePhoneNumber extends LogicalTypeInfinite  {
		public static final String SEMANTIC_TYPE = "TELEPHONE";
		public static final String REGEXP = ".*";
		private PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		private static String[] areaCodes = new String[] { "617", "781", "303", "970", "212" };

		public LogicalTypePhoneNumber(PluginDefinition plugin) {
			super(plugin);
		}

		@Override
		public String nextRandom() {
			String base = "+1" + areaCodes[random.nextInt(areaCodes.length)];
			while (true) {
				StringBuilder result = new StringBuilder(base);
				for (int i = 0; i < 7; i++)
					result.append(random.nextInt(10));
				String attempt = result.toString();
				if (isValid(attempt))
					return attempt;
			}
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
		public FTAType getBaseType() {
			return FTAType.STRING;
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
		public String isValidSet(String dataStreamName, long matchCount, long realSamples, TypeFacts facts, Map<String, Long> cardinality, Map<String, Long> outliers) {
			return (double)matchCount/realSamples >= getThreshold()/100.0 ? null : REGEXP;
		}
}
