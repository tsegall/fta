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

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.cobber.fta.LogicalTypeCode;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.Shapes;
import com.cobber.fta.FactsTypeBased;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;

/**
 * Plugin to detect Email Addresses.
 */
public class LogicalTypeEmail extends LogicalTypeInfinite {
	public static final String SEMANTIC_TYPE = "EMAIL";
	public static final String REGEXP = "[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}";
	private LogicalTypeCode logicalFirst;
	private LogicalTypeCode logicalLast;
	private static String[] mailDomains = new String[] {
			"gmail.com", "hotmail.com", "yahoo.com"
	};

	public LogicalTypeEmail(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		return logicalFirst.nextRandom().toLowerCase(Locale.ROOT) + "." + logicalLast.nextRandom().toLowerCase(Locale.ROOT) + "@" + mailDomains[random.nextInt(mailDomains.length)];
	}

	@Override
	public boolean initialize(final Locale locale) throws FTAPluginException {
		super.initialize(locale);

		PluginDefinition pluginFirst = new PluginDefinition("NAME.FIRST", "com.cobber.fta.plugins.LogicalTypeFirstName");
		logicalFirst = (LogicalTypeCode) LogicalTypeFactory.newInstance(pluginFirst, Locale.getDefault());
		final PluginDefinition pluginLast = new PluginDefinition("NAME.LAST", "com.cobber.fta.plugins.LogicalTypeLastName");
		logicalLast = (LogicalTypeCode) LogicalTypeFactory.newInstance(pluginLast, Locale.getDefault());

		threshold = 95;

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
		return true;
	}

	@Override
	public boolean isValid(String input) {
		// This is not strictly correct since RFC 822 does not mandate an '@' but this is what mortals expect
		if (input.indexOf('@') == -1)
			return false;

		// Address lists commonly have ;'s as separators as opposed to the ','
		if (input.indexOf(';') != -1)
			input = input.replace(';', ',');
		try {
			return InternetAddress.parse(input).length != 0;
		} catch (AddressException e) {
			return false;
		}
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		final int atSigns = charCounts['@'];
		return atSigns - 1 == charCounts[','] || atSigns - 1 == charCounts[';'];
	}

	@Override
	public String isValidSet(final String dataStreamName, final long matchCount, final long realSamples, final FactsTypeBased facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final Shapes shapes) {
		return getConfidence(matchCount, realSamples, dataStreamName) >= getThreshold()/100.0 ? null : ".+";
	}

	@Override
	public double getConfidence(final long matchCount, final long realSamples, final String dataStreamName) {
		final double is = (double)matchCount/realSamples;
		if (matchCount != realSamples && getHeaderConfidence(dataStreamName) != 0)
			return is + (1.0 - is)/2;
		else
			return is;
	}
}
