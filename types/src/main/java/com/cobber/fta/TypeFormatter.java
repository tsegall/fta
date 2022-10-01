package com.cobber.fta;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.cobber.fta.core.FTAType;
import com.cobber.fta.dates.DateTimeParser;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class TypeFormatter {
	private NumberFormat longFormatter;
	private NumberFormat doubleFormatter;
	private DateTimeFormatter dateTimeFormatter;
	public FTAType type;
	public boolean hasGrouping;
	public String format;
	public Locale locale;

	TypeFormatter() {
	}

	TypeFormatter(final PatternInfo matchPatternInfo, final Locale locale, final char decimalSeparator, final char localeDecimalSeparator) {
		this.type = matchPatternInfo.getBaseType();
		this.hasGrouping = KnownPatterns.hasGrouping(matchPatternInfo.id);
		this.format = matchPatternInfo.format;
		if (type == FTAType.DOUBLE) {
			this.locale = decimalSeparator == localeDecimalSeparator ? locale : Locale.ROOT;
		}
		else
			this.locale = locale;
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
			DateTimeParser dateTimeParser = new DateTimeParser().withLocale(locale).withNumericMode(false);
			dateTimeFormatter = dateTimeParser.ofPattern(format);
		}
		return dateTimeFormatter;
	}
}
