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

public class LogicalTypeIPV4Address extends LogicalTypeInfinite {
	public static final String SEMANTIC_TYPE = "IPADDRESS.IPV4";
	public static final String REGEXP = "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
	static InetAddressValidator validator = null;
	static {
		validator = InetAddressValidator.getInstance();
	}

	public LogicalTypeIPV4Address(final PluginDefinition plugin) {
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
		StringBuilder ret = new StringBuilder(36);

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
		return input.length() <= 15 && validator.isValidInet4Address(input);
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return trimmed.length() <= 15 && charCounts['.'] == 3;
	}

	@Override
	public String isValidSet(final String dataStreamName, final long matchCount, final long realSamples, final TypeFacts facts,
			final Map<String, Long> cardinality, final Map<String, Long> outliers) {
		return (double) matchCount / realSamples >= getThreshold() / 100.0 ? null : ".+";
	}

}
