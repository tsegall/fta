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
package com.cobber.fta;

import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.cobber.fta.core.FTAType;
import com.cobber.fta.dates.DateTimeParser;

/**
 * Based on the FTA Type support the ability to convert from a String to an instance of the Type, or from an instance of
 * the type to a String (correctly formatted - based on the pattern).
 * In addition provide routines to convert from any given FTAType to a double and back.  We need this conversion in
 * order to support the Sketch which assumes doubles as input where the ordering of the proxy double is equivalent to
 * the ordering of the FTAType instance.
 */
public class StringConverter {
	private NumberFormat longFormatter;
	private NumberFormat doubleFormatter;
	private DateTimeFormatter dateTimeFormatter;
	private FTAType type;

	StringConverter(final PatternInfo matchPatternInfo, final Locale locale, final char decimalSeparator, final char localeDecimalSeparator) {
		this.type = matchPatternInfo.getBaseType();

		switch (type) {
		case LONG:
			longFormatter = NumberFormat.getIntegerInstance(locale);
			longFormatter.setGroupingUsed(KnownPatterns.hasGrouping(matchPatternInfo.id));
			break;
		case DOUBLE:
			doubleFormatter = NumberFormat.getInstance(decimalSeparator == localeDecimalSeparator ? locale : Locale.ROOT);
			doubleFormatter.setGroupingUsed(KnownPatterns.hasGrouping(matchPatternInfo.id));
			break;
		case LOCALDATE:
		case LOCALTIME:
		case LOCALDATETIME:
		case ZONEDDATETIME:
		case OFFSETDATETIME:
			DateTimeParser dateTimeParser = new DateTimeParser().withLocale(locale).withNumericMode(false);
			dateTimeFormatter = dateTimeParser.ofPattern(matchPatternInfo.format);
			break;
		}
	}

	protected Object getValue(final String input) {
		if (input == null)
			return null;

		switch (type) {
		case BOOLEAN:
			return Boolean.valueOf(input);
		case LONG:
			// We cannot use parseLong as it does not cope with localization
	        try {
                return longFormatter.parse(input).longValue();
	        } catch (ParseException e) {
                return null;
	        }
		case DOUBLE:
	        try {
	        	return  doubleFormatter.parse(input.charAt(0) == '+' ? input.substring(1) : input).doubleValue();
	        } catch (ParseException e) {
                return null;
	        }
		case STRING:
			return input;
		case LOCALDATE:
			return LocalDate.parse(input, dateTimeFormatter);
		case LOCALTIME:
			return LocalTime.parse(input, dateTimeFormatter);
		case LOCALDATETIME:
			return LocalDateTime.parse(input, dateTimeFormatter);
		case ZONEDDATETIME:
			return ZonedDateTime.parse(input, dateTimeFormatter);
		case OFFSETDATETIME:
			return OffsetDateTime.parse(input, dateTimeFormatter);
		}

		return null;
	}

	public String formatted(final Object toFix) {
		switch (type) {
		case BOOLEAN:
			return String.valueOf((toFix));
		case LONG:
			return longFormatter.format((long)toFix);
		case DOUBLE:
			return doubleFormatter.format((double)toFix);
		case STRING:
			return (String)toFix;
		case LOCALDATE:
			return ((LocalDate)toFix).format(dateTimeFormatter);
		case LOCALTIME:
			return ((LocalTime)toFix).format(dateTimeFormatter);
		case LOCALDATETIME:
			return ((LocalDateTime)toFix).format(dateTimeFormatter);
		case ZONEDDATETIME:
			return ((ZonedDateTime)toFix).format(dateTimeFormatter);
		case OFFSETDATETIME:
			return ((OffsetDateTime)toFix).format(dateTimeFormatter);
		}

		return null;
	}

	// Convert a String representation of a type to a Double that preserves ordering of the input.
	// Note: No support for type STRING (no mapping available) or BOOLEAN (no need since cardinality limited)
	public double toDouble(final String key) {
		switch (type) {
		case BOOLEAN:
			return (Boolean)(getValue(key)) ? 0.0 : 1.0;
		case DOUBLE:
			return (Double)(getValue(key));
		case LOCALDATE:
			return ((LocalDate)(getValue(key))).toEpochDay();
		case LOCALDATETIME:
			return ((LocalDateTime)(getValue(key))).toEpochSecond(ZoneOffset.UTC);
		case LOCALTIME:
			return ((LocalTime)(getValue(key))).toNanoOfDay();
		case LONG:
			return (Long)(getValue(key));
		case OFFSETDATETIME:
			OffsetDateTime odt = ((OffsetDateTime)(getValue(key)));
			long epochSeconds = odt.toEpochSecond();
			// Zone Offset seconds ranges from -64800 to +64800
			long zoneOffsetSeconds = odt.getOffset().getTotalSeconds();
			return epochSeconds * 1_000_000 + zoneOffsetSeconds + 64800;
		case ZONEDDATETIME:
			// Not correct - since does not account for Zone
//			ZonedDateTime zdt = ((ZonedDateTime)(getValue(key)));
//			zdt.toInstant().getEpochSecond()
			return ((ZonedDateTime)(getValue(key))).toOffsetDateTime().toEpochSecond();
		}

		return 0.0;
	}

	// Convert a double representation of a type to its native type
	// Note: No support for type STRING (no mapping available) or BOOLEAN (no need since cardinality limited)
	public Object fromDouble(final double value) {
		switch (type) {
		case BOOLEAN:
			return value == 0.0 ? Boolean.FALSE : Boolean.TRUE;
		case DOUBLE:
			return value;
		case LOCALDATE:
			return LocalDate.ofEpochDay((long)value);
		case LOCALDATETIME:
			return LocalDateTime.ofEpochSecond((long)value, 0, ZoneOffset.UTC);
		case LOCALTIME:
			return LocalTime.ofNanoOfDay((long)value);
		case LONG:
			return Math.round(value);
		case OFFSETDATETIME:
			long raw = (long)value;
			long epochSeconds = raw / 1_000_000;
			long zoneOffsetSeconds = raw - (epochSeconds * 1_000_000) - 64800;
			ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds((int)zoneOffsetSeconds);
			return OffsetDateTime.of(LocalDateTime.ofEpochSecond(epochSeconds, 0, zoneOffset), zoneOffset);
		case ZONEDDATETIME:
			// Not correct - since ignores Zone
			return OffsetDateTime.of(LocalDateTime.ofEpochSecond((long)value, 0, ZoneOffset.UTC), ZoneOffset.UTC).toZonedDateTime();
		}

		return 0.0;
	}
}
