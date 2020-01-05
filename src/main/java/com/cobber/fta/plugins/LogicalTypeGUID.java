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
import com.cobber.fta.PatternInfo;
import com.cobber.fta.PatternInfo.Type;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.StringFacts;

/**
 * Plugin to detect GUIDs.
 */
public class LogicalTypeGUID extends LogicalTypeInfinite {
	public final static String SEMANTIC_TYPE = "GUID";
	public final static String REGEXP = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";
	private final static char[] HEX = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	public LogicalTypeGUID(PluginDefinition plugin) {
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

		for (int i = 0; i < 32; i++) {
			if (i == 8 || i == 12 || i == 16 || i == 20)
				ret.append('-');
			ret.append(HEX[random.nextInt(16)]);
		}

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
		int len = input.length();
		if (len != 36)
			return false;

		if (input.charAt(8) != '-' || input.charAt(13) != '-' || input.charAt(18) != '-' || input.charAt(23) != '-')
			return false;

		for (int i = 0; i < len; i++) {
			if (i == 8 || i == 13 || i == 18 || i == 23)
				continue;
			char ch = input.charAt(i);
			if ((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F'))
				continue;
			return false;
		}

		return true;
	}

	@Override
	public boolean isCandidate(String trimmed, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		return trimmed.length() == 36 && charCounts['-'] == 4;
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, StringFacts stringFacts,
			Map<String, Long> cardinality, Map<String, Long> outliers) {
		return (double) matchCount / realSamples >= getThreshold() / 100.0 ? null : ".+";
	}
}
