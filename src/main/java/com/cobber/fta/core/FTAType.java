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
	}
}