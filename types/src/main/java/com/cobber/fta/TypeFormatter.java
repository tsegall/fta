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
			final DateTimeParser dateTimeParser = new DateTimeParser().withLocale(locale).withNumericMode(false).withNoAbbreviationPunctuation(analysisConfig.isEnabled(Feature.NO_ABBREVIATION_PUNCTUATION));
			dateTimeFormatter = dateTimeParser.ofPattern(format);
		}
		return dateTimeFormatter;
	}
}
