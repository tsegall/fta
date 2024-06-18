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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.Utils;

/**
 * Based on the FTA Type, support the ability to convert from a String to an
 * instance of the Type, or from an instance of the type to a String (correctly
 * formatted - based on the pattern). In addition provide routines to convert
 * from any given FTAType to a double and back. We need this conversion in order
 * to support the Sketch which assumes doubles as input where the ordering of
 * the proxy double is equivalent to the ordering of the FTAType instance.
 */
public class StringConverter {
	public TypeFormatter typeFormatter;
	public FTAType type;

	StringConverter() {
	}

	StringConverter(final FTAType type, final TypeFormatter typeFormatter) {
		this.type = type;
		this.typeFormatter = typeFormatter;
	}

	protected Object getValue(final String input) {
		if (input == null)
			return null;

		final String trimmed = input.trim();

		try {
			switch (type) {
				case BOOLEAN:
					return Boolean.valueOf(trimmed);
				case LONG:
					return Utils.parseLong(input, typeFormatter.getNumericalFormatter());
				case DOUBLE:
					return Utils.parseDouble(input, typeFormatter.getNumericalFormatter());
				case STRING:
					return input;
				case LOCALDATE:
					return LocalDate.parse(trimmed, typeFormatter.getDateFormatter());
				case LOCALTIME:
					return LocalTime.parse(trimmed, typeFormatter.getDateFormatter());
				case LOCALDATETIME:
					return LocalDateTime.parse(trimmed, typeFormatter.getDateFormatter());
				case ZONEDDATETIME:
					return ZonedDateTime.parse(trimmed, typeFormatter.getDateFormatter());
				case OFFSETDATETIME:
					return OffsetDateTime.parse(trimmed, typeFormatter.getDateFormatter());
				}
		}
		catch (NumberFormatException | DateTimeParseException e) {
			// Fall through
		}

		return null;
	}

	public String formatted(final Object toFix) {
		switch (type) {
		case BOOLEAN:
			return String.valueOf((toFix));
		case LONG:
			return typeFormatter.getNumericalFormatter().format((long) toFix);
		case DOUBLE:
			return typeFormatter.getNumericalFormatter().format((double) toFix);
		case STRING:
			return (String) toFix;
		case LOCALDATE:
			return ((LocalDate) toFix).format(typeFormatter.getDateFormatter());
		case LOCALTIME:
			return ((LocalTime) toFix).format(typeFormatter.getDateFormatter());
		case LOCALDATETIME:
			return ((LocalDateTime) toFix).format(typeFormatter.getDateFormatter());
		case ZONEDDATETIME:
			return ((ZonedDateTime) toFix).format(typeFormatter.getDateFormatter());
		case OFFSETDATETIME:
			return ((OffsetDateTime) toFix).format(typeFormatter.getDateFormatter());
		}

		return null;
	}

	// Convert a String representation of a type to a Double that preserves ordering
	// of the input.
	// Note: No support for type STRING (no mapping available) or BOOLEAN (no need
	// since cardinality limited)
	public double toDouble(final String key) {
		switch (type) {
		case BOOLEAN:
			return (Boolean) (getValue(key)) ? 0.0 : 1.0;
		case DOUBLE:
			return (Double) (getValue(key));
		case LOCALDATE:
			return ((LocalDate) (getValue(key))).toEpochDay();
		case LOCALDATETIME:
			return ((LocalDateTime) (getValue(key))).toEpochSecond(ZoneOffset.UTC);
		case LOCALTIME:
			return ((LocalTime) (getValue(key))).toNanoOfDay();
		case LONG:
			return (Long) (getValue(key));
		case OFFSETDATETIME:
			final OffsetDateTime odt = ((OffsetDateTime) (getValue(key)));
			final long epochSeconds = odt.toEpochSecond();
			// Zone Offset seconds ranges from -64800 to +64800
			final long zoneOffsetSeconds = odt.getOffset().getTotalSeconds();
			return epochSeconds * 1_000_000 + zoneOffsetSeconds + 64800;
		case ZONEDDATETIME:
			// Not correct - since does not account for Zone
//			ZonedDateTime zdt = ((ZonedDateTime)(getValue(key)));
//			zdt.toInstant().getEpochSecond()
			return ((ZonedDateTime) (getValue(key))).toOffsetDateTime().toEpochSecond();
		}

		return 0.0;
	}

	private long clampLong(final long input, final long low, final long high) {
		if (input < low)
			return low;
		if (input > high)
			return high;
		return input;
	}

	private int clampInt(final int input, final int low, final int high) {
		if (input < low)
			return low;
		if (input > high)
			return high;
		return input;
	}

	// Convert a double representation of a type to its native type
	// Note: No support for type STRING (no mapping available) or BOOLEAN (no need
	// since cardinality limited)
	public Object fromDouble(final double value) {
		switch (type) {
		case BOOLEAN:
			return value == 0.0 ? Boolean.FALSE : Boolean.TRUE;
		case DOUBLE:
			return value;
		case LOCALDATE:
			return LocalDate.ofEpochDay((long) value);
		case LOCALDATETIME:
			return LocalDateTime.ofEpochSecond((long) value, 0, ZoneOffset.UTC);
		case LOCALTIME:
			// The Sketch values are only accurate within the relative-error guarantee, so
			// they may be outside the valid
			// range in the case of a LocalTime, so clamp it to something that is plausible.
			return LocalTime.ofNanoOfDay(clampLong((long) value, 0, 86399999999999L));
		case LONG:
			return Math.round(value);
		case OFFSETDATETIME:
			final long raw = (long) value;
			final long epochSeconds = raw / 1_000_000;
			final long zoneOffsetSeconds = raw - (epochSeconds * 1_000_000) - 64800;
			final ZoneOffset zoneOffset = ZoneOffset
					.ofTotalSeconds(clampInt((int) zoneOffsetSeconds, -18 * 60 * 60, 18 * 60 * 60));
			return OffsetDateTime.of(LocalDateTime.ofEpochSecond(epochSeconds, 0, zoneOffset), zoneOffset);
		case ZONEDDATETIME:
			// Not correct - since ignores Zone
			return OffsetDateTime.of(LocalDateTime.ofEpochSecond((long) value, 0, ZoneOffset.UTC), ZoneOffset.UTC)
					.toZonedDateTime();
		}

		return 0.0;
	}
}
