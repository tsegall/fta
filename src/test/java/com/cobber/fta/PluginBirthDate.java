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
package com.cobber.fta;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import com.cobber.fta.core.FTAType;
import com.cobber.fta.dates.DateTimeParser;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;

public class PluginBirthDate extends LogicalTypeInfinite {
	public final static String REGEXP = "\\d{4}/\\d{2}/\\d{2}";

	private static DateTimeParser dtp = new DateTimeParser();
	private LocalDate plausibleBirth = LocalDate.of(1910, 1, 1);

	public PluginBirthDate(PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean isCandidate(String trimmed, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		String format = dtp.determineFormatString(trimmed, DateResolutionMode.MonthFirst);
		DateTimeFormatter formatter = DateTimeParser.ofPattern(format, locale);
		final LocalDate localDate = LocalDate.parse(trimmed, formatter);

		return localDate.isAfter(plausibleBirth);
	}

	@Override
	public boolean initialize(Locale locale) {
		super.initialize(locale);

		return true;
	}

	@Override
	public String nextRandom() {
		return null;
	}

	@Override
	public String getQualifier() {
		return "BIRTHDATE";
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.LOCALDATE;
	}

	@Override
	public boolean isValid(String input) {
		return isCandidate(input.trim(), null, null, null);
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples,
			TypeFacts facts, Map<String, Long> cardinality, Map<String, Long> outliers) {
		return (double)matchCount/realSamples >= getThreshold()/100.0 ? null : ".+";
	}
}
