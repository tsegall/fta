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

import org.apache.commons.validator.routines.InetAddressValidator;

import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PatternInfo;
import com.cobber.fta.PatternInfo.Type;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.StringFacts;

public class LogicalTypeIPV4Address extends LogicalTypeInfinite {
	public final static String SEMANTIC_TYPE = "IPADDRESS.IPV4";
	public final static String REGEXP = "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
	static InetAddressValidator validator = null;
	static {
		validator = InetAddressValidator.getInstance();
	}

	public LogicalTypeIPV4Address(PluginDefinition plugin) {
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
