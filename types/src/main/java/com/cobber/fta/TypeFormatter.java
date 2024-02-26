/*
 * Copyright 2017-2024 Tim Segall
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

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.cobber.fta.TextAnalyzer.Feature;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.dates.DateTimeParser;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class TypeFormatter {
	private NumberFormat longFormatter;
	private NumberFormat doubleFormatter;
	private DateTimeFormatter dateTimeFormatter;
	public FTAType type;
	public boolean hasGrouping;
	public boolean isTrailingMinus;
	public String format;
	public Locale locale;
	public AnalysisConfig analysisConfig;

	TypeFormatter() {
	}

	TypeFormatter(final TypeInfo matchTypeInfo, final AnalysisConfig analysisConfig) {
		this.type = matchTypeInfo.getBaseType();
		this.hasGrouping = matchTypeInfo.hasGrouping();
		this.isTrailingMinus = matchTypeInfo.isTrailingMinus();
		this.format = matchTypeInfo.format;
		this.analysisConfig = analysisConfig;
		if (type == FTAType.DOUBLE)
			this.locale = matchTypeInfo.isNonLocalized() ? Locale.ROOT : analysisConfig.getLocale();
		else
			this.locale = analysisConfig.getLocale();
	}

	@JsonIgnore
	public NumberFormat getNumericalFormatter() {
		if (type == FTAType.LONG) {
			if (longFormatter == null) {
				longFormatter = NumberFormat.getIntegerInstance(locale);
				longFormatter.setGroupingUsed(hasGrouping);
			}
			return longFormatter;
		}

		if (doubleFormatter == null) {
			doubleFormatter = NumberFormat.getInstance(locale);
			doubleFormatter.setGroupingUsed(hasGrouping);
			doubleFormatter.setMinimumFractionDigits(1);
			doubleFormatter.setMaximumFractionDigits(16);
		}
		return doubleFormatter;
	}

	@JsonIgnore
	public DateTimeFormatter getDateFormatter() {
		if (dateTimeFormatter == null) {
			final DateTimeParser dateTimeParser = new DateTimeParser()
					.withLocale(locale)
					.withNumericMode(false)
					.withNoAbbreviationPunctuation(analysisConfig.isEnabled(Feature.NO_ABBREVIATION_PUNCTUATION))
					.withEnglishAMPM(analysisConfig.isEnabled(TextAnalyzer.Feature.ALLOW_ENGLISH_AMPM));
			dateTimeFormatter = dateTimeParser.ofPattern(format);
		}
		return dateTimeFormatter;
	}
}
