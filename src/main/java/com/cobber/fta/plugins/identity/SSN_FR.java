/*
 * Copyright 2017-2022 Tim Segall
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
package com.cobber.fta.plugins.identity;

import java.util.Locale;
import java.util.Map;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect French SSN's.
 */
public class SSN_FR extends LogicalTypeInfinite {
	public static final String SEMANTIC_TYPE = "IDENTITY.SSN_FR";
	private static final int SSN_LENGTH = 15;
	public static final String BACKOUT_REGEXP = ".*";
	private String regExp = BACKOUT_REGEXP;

	public SSN_FR(final PluginDefinition plugin) {
		super(plugin);
	}

	private boolean isValidChar(final char ch, final int offset) {
		if (Character.isDigit(ch))
			return true;
		if (offset != 6)
			return false;

		return ch == 'A' || ch == 'a' || ch == 'B' || ch == 'b';
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		if (trimmed.length() - charCounts[' '] != SSN_LENGTH)
			return false;

		int offset = 0;
		for (int i = 0; i < trimmed.length(); i++) {
			char ch = trimmed.charAt(i);
			if (ch == ' ')
				continue;
			if (!isValidChar(ch, offset))
				return false;
			offset++;
		}

		return true;
	}

	@Override
	public boolean initialize(final Locale locale) throws FTAPluginException {
		super.initialize(locale);

		return true;
	}

	@Override
	public String nextRandom() {
		final String inseeStr = String.format("%d%02d%02d%02d%03d%03d",
				random.nextInt(2) + 1,			// Gender
				random.nextInt(100),			// Birth Year
				random.nextInt(12) + 1,			// Birth Month
				21 + random.nextInt(75),		// Department (only use 21-75)
				random.nextInt(990) + 1,		// City (001 - 990)
				random.nextInt(999) + 1);		// Certificate (001 - 999)

		final long insee = Long.parseLong(inseeStr);

		final long check = 97 - insee % 97;

		return inseeStr + String.format(" %02d", check);
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public String getRegExp() {
		return regExp;
	}

	@Override
	public boolean acceptsBaseType(final FTAType type) {
		return type == FTAType.STRING || type == FTAType.LONG;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public boolean isValid(final String input) {
		final StringBuilder b = new StringBuilder(SSN_LENGTH);

		int offset = 0;
		for (int i = 0; i < input.length(); i++) {
			char ch = input.charAt(i);
			if (ch == ' ')
				continue;
			if (!isValidChar(ch, offset))
				return false;
			b.append(ch);
			offset++;
		}

		if (b.length() != SSN_LENGTH)
			return false;

		final char gender = b.charAt(0);
		if (gender != '1' && gender != '7' && gender != '2' && gender != '8')
			return false;

		// year = b.substring(1, 3);

		final long month = Long.parseLong(b.substring(3, 5));
		// Birth month (-12) or 20 if not known
		if ((month < 1 || month > 12) && month != 20)
			return false;

		// place = b.substring(5, 10);

		final char corsica = b.charAt(6);
		final boolean corsicaA = corsica == 'A' || corsica == 'a';
		final boolean corsicaB = corsica == 'B' || corsica == 'b';
		final String departmentStr = b.substring(5,7);
		// Department is 1-95 (except 20), + 2A, 2B (Corsica) + 971-976 (Overseas)
		if (corsicaA || corsicaB) {
			b.setCharAt(6, '0');
			if (b.charAt(5) != '2')
				return false;
		}
		else {
			long department = Long.parseLong(departmentStr);
			if (department == 0 || department == 20 || department == 96 || department == 98)
				return false;
			if (department == 97) {
				department = department * 10 + (b.charAt(7) - '0');
				if (department < 971 || department > 976)
					return false;
			}
			// Department of 99 - means born overseas, the next three digits are the code for the birth country
		}

		final String certificate = b.substring(10, 13);
		if ("000".equals(certificate))
			return false;

		final long key = Long.parseLong(b.substring(13, 15));
		if (key > 97)
			return false;

		long insee = Long.parseLong(b.substring(0, 13));
		if (corsicaA)
			insee -= 1000000;
		else if (corsicaB)
			insee -= 2000000;

		return (97 - insee % 97) == key;
	}

	@Override
	public String isValidSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		if (cardinality.size() < 20 || (double)matchCount/realSamples < getThreshold()/100.0)
			return BACKOUT_REGEXP;

		regExp = tokenStreams.getRegExp(false);
		return null;
	}
}
