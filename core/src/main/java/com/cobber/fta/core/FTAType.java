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
package com.cobber.fta.core;

public enum FTAType {
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
	};

	/**
	 * Is this Type Numeric?
	 *
	 * @return A boolean indicating if the Type for this pattern is numeric.
	 */
	public boolean isNumeric() {
		return FTAType.LONG.equals(this) || FTAType.DOUBLE.equals(this);
	}

	/**
	 * Is this Type a Date Type?
	 *
	 * @return A boolean indicating if the Type for this pattern includes a Date.
	 */
	public boolean isDateType() {
		return FTAType.LOCALDATE.equals(this) || FTAType.LOCALDATETIME.equals(this) ||
				FTAType.OFFSETDATETIME.equals(this) || FTAType.ZONEDDATETIME.equals(this);
	}

	/**
	 * Is this Type a Date or Time Type?
	 *
	 * @return A boolean indicating if the Type for this pattern includes a Date.
	 */
	public boolean isDateOrTimeType() {
		return FTAType.LOCALTIME.equals(this) || this.isDateType();
	}
}
