/*
 * Copyright 2017-2018 Tim Segall
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

/**
 * The PatternInfo class maintains a set of information about a simple pattern.
 * This is used to derive a Type from a pattern. For example, new
 * PatternInfo("(?i)true|false", null, "", "Boolean", null) indicates that a
 * case insensitive match for true or false indicates a boolean type
 * ("Boolean").
 */
public class PatternInfo {

	public enum Type {
		/** A Boolean type - e.g. True/False, Yes/No, 1/0 */
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
		/** A simple Date value - i.e. A calendar value, no time, no time-zone. */
		LOCALDATE {
			@Override
			public String toString() {
				return "LocalDate";
			}
		},
		/** A date and time - i.e. both Calendar and a wall clock. */
		LOCALDATETIME {
			@Override
			public String toString() {
				return "LocalDateTime";
			}
		},
		/** Any Time value - i.e. a wall Time. */
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
		/** A date-time with an time-zone. */
		ZONEDDATETIME {
			@Override
			public String toString() {
				return "ZonedDateTime";
			}
		}
	}

	public String regexp;
	public String generalPattern;
	public int minLength;
	public int maxLength;
	public String format;
	public Type type;
	public String typeQualifier;

	/**
	 * Construct a new information block for the supplied pattern.
	 *
	 * @param regexp
	 *            The pattern of interest.
	 * @param type
	 *            The type of the pattern.
	 * @param typeQualifier
	 *            The type qualifier of the pattern (optional).
	 * @param minLength
	 *            The minimum length of this pattern (-1 implies undefined)
	 * @param maxLength
	 *            The maximum length of this pattern (-1 implies undefined)
	 * @param generalPattern
	 *            The general case of this pattern (optional).
	 * @param format
	 *            The Java format specified for a date pattern (optional).
	 */
	public PatternInfo(final String regexp, final Type type, final String typeQualifier, final int minLength,
			final int maxLength, final String generalPattern, final String format) {
		this.regexp = regexp;
		this.type = type;
		this.typeQualifier = typeQualifier;
		this.minLength = minLength;
		this.maxLength = maxLength;
		this.generalPattern = generalPattern;
		this.format = format;
	}

	/**
	 * Is this pattern Numeric?
	 *
	 * @return A boolean indicating if the Type for this pattern is numeric.
	 */
	public boolean isNumeric() {
		return PatternInfo.Type.LONG.equals(this.type) || PatternInfo.Type.DOUBLE.equals(this.type);
	}
}
