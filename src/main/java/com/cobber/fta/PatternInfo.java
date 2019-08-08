/*
 * Copyright 2017-2019 Tim Segall
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

import com.cobber.fta.KnownPatterns.ID;

/**
 * The PatternInfo class maintains a set of information about a simple pattern.
 * This is used to derive a Type from a pattern. For example, new
 * PatternInfo("(?i)true|false", null, "", "Boolean", null) indicates that a
 * case insensitive match for true or false indicates a boolean type
 * ("BOOLEAN").
 */
public class PatternInfo {

	public enum Type {
		/** A Boolean type - for example, True/False, Yes/No, 1/0. */
		BOOLEAN {
			@Override
			public String toString() {
				return "Boolean";
			}
		},
		/** Any Floating point type - refer to min/max to determine range. */
		DOUBLE {
			@Override
			public String toString() {
				return "Double";
			}
		},
		/** A simple Date value - a calendar value with no time or no time-zone. */
		LOCALDATE {
			@Override
			public String toString() {
				return "LocalDate";
			}
		},
		/** A date and time - both a calendar and a wall clock. */
		LOCALDATETIME {
			@Override
			public String toString() {
				return "LocalDateTime";
			}
		},
		/** Any Time value - a wall Time. */
		LOCALTIME {
			@Override
			public String toString() {
				return "LocalTime";
			}
		},
		/** Any Integral type - refer to min/max to determine range. */
		LONG {
			@Override
			public String toString() {
				return "Long";
			}
		},
		/** A date-time with an offset from UTC. */
		OFFSETDATETIME {
			@Override
			public String toString() {
				return "OffsetDateTime";
			}
		},
		/** Any String value. */
		STRING {
			@Override
			public String toString() {
				return "String";
			}
		},
		/** A date-time with a time-zone. */
		ZONEDDATETIME {
			@Override
			public String toString() {
				return "ZonedDateTime";
			}
		}
	}

	public enum TypeQualifier {
		/* String */
		BLANK,
		BLANKORNULL,
		NULL,
		/* Numerics */
		SIGNED,
		SIGNED_TRAILING,
		GROUPING,
		/* Boolean */
		TRUE_FALSE,
		YES_NO,
		Y_N,
		ONE_ZERO
	}

	public KnownPatterns.ID id;
	public String regexp;
	public String generalPattern;
	public int minLength;
	public int maxLength;
	public String format;
	public Type type;
	public String typeQualifier;
	public boolean isLogicalType;

	/**
	 * Construct a new information block for the supplied pattern.
	 * @param id
	 *            The ID of interest.
	 * @param regexp
	 *            The pattern of interest.
	 * @param type
	 *            The type of the pattern.
	 * @param typeQualifier
	 *            The type qualifier of the pattern (optional).
	 * @param isLogicalType
	 *			  A boolean indicating if this is a Logical Type.
	 * @param minLength
	 *            The minimum length of this pattern (-1 implies undefined)
	 * @param maxLength
	 *            The maximum length of this pattern (-1 implies undefined)
	 * @param generalPattern
	 *            The general case of this pattern (optional).
	 * @param format
	 *            The Java format specified for a date pattern (optional).
	 */
	public PatternInfo(ID id, final String regexp, final Type type, final String typeQualifier,
			boolean isLogicalType, final int minLength, final int maxLength, final String generalPattern, final String format) {
		this.id = id;
		this.regexp = regexp;
		this.type = type;
		this.typeQualifier = typeQualifier;
		this.isLogicalType = isLogicalType;
		this.minLength = minLength;
		this.maxLength = maxLength;
		this.generalPattern = generalPattern;
		this.format = format;
	}

	public PatternInfo(PatternInfo that) {
		this.id = that.id;
		this.regexp = that.regexp;
		this.type = that.type;
		this.typeQualifier = that.typeQualifier;
		this.isLogicalType = that.isLogicalType;
		this.minLength = that.minLength;
		this.maxLength = that.maxLength;
		this.generalPattern = that.generalPattern;
		this.format = that.format;
	}

	/**
	 * Is this pattern Numeric?
	 *
	 * @return A boolean indicating if the Type for this pattern is numeric.
	 */
	public boolean isNumeric() {
		return PatternInfo.Type.LONG.equals(this.type) || PatternInfo.Type.DOUBLE.equals(this.type);
	}

	/**
	 * Is this pattern a Date Type?
	 *
	 * @return A boolean indicating if the Type for this pattern includes a Date.
	 */
	public boolean isDateType() {
		return PatternInfo.Type.LOCALDATE.equals(this.type) || PatternInfo.Type.LOCALDATETIME.equals(this.type) ||
				PatternInfo.Type.OFFSETDATETIME.equals(this.type) || PatternInfo.Type.ZONEDDATETIME.equals(this.type);
	}

	/**
	 * Is this pattern Alphabetic?
	 *
	 * @return A boolean indicating if the Type for this pattern is Alphabetic.
	 */
	public boolean isAlphabetic() {
		return this.regexp.startsWith(KnownPatterns.PATTERN_ALPHA);
	}

	/**
	 * Is this pattern Alphanumeric?
	 *
	 * @return A boolean indicating if the Type for this pattern is Alphanumeric.
	 */
	public boolean isAlphanumeric() {
		return this.regexp.startsWith(KnownPatterns.PATTERN_ALPHANUMERIC);
	}

	/**
	 * Is this PatternInfo a Logical Type?
	 *
	 * @return A boolean indicating if this is a Logical Type.
	 */
	public boolean isLogicalType() {
		return isLogicalType;
	}
}
