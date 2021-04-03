/*
 * Copyright 2017-2021 Tim Segall
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
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.TypeFacts;
import com.cobber.fta.core.FTAType;

public class LogicalTypeIPV6Address extends LogicalTypeInfinite {
	public static final String SEMANTIC_TYPE = "IPADDRESS.IPV6";
	public static final String REGEXP = "(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))";
	static InetAddressValidator validator = null;
	static {
		validator = InetAddressValidator.getInstance();
	}

	public LogicalTypeIPV6Address(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final Locale locale) {
		super.initialize(locale);

		threshold = 99;

		return true;
	}

	@Override
	public String nextRandom() {
		final StringBuilder ret = new StringBuilder(40);

		for (int i = 0; i < 7; i++) {
			ret.append(String.format("%x", random.nextInt(0xFFFF)));
			ret.append(':');
		}
		ret.append(String.format("%x", random.nextInt(0xFFFF)));

		return ret.toString();
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
		return true;
	}

	@Override
	public boolean isValid(final String input) {
		return validator.isValidInet6Address(input);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return validator.isValidInet6Address(trimmed);
	}

	@Override
	public String isValidSet(final String dataStreamName, final long matchCount, final long realSamples, final TypeFacts facts,
			final Map<String, Long> cardinality, final Map<String, Long> outliers) {
		return (double) matchCount / realSamples >= getThreshold() / 100.0 ? null : ".+";
	}

}
