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
package com.cobber.fta.dates;

import java.util.HashMap;
import java.util.Map;

/**
 * Capture the State of the DateTimeParser - this and the Configuration are all we need to serialize()/deserialize()/merge the DateTimeParser.
 */
public class DateTimeParserState {
	public int sampleCount;
	public int nullCount;
	public int blankCount;
	public int invalidCount;
	public final Map<String, Integer> results;

	DateTimeParserState() {
		results = new HashMap<>();
	}

	/**
	 * Merge a DateTimeParserState from another DateTimeParser with this one.
	 * Note: You cannot merge states unless the DateTimeParserConfigs are equal.
	 * @param other The other DateTimeParserState to be merged
	 * @return A merged DateTimeParserState.
	 */
	public DateTimeParserState merge(final DateTimeParserState other) {
		sampleCount += other.sampleCount;
		nullCount += other.nullCount;
		blankCount += other.blankCount;
		invalidCount += other.invalidCount;

		for (final Map.Entry<String, Integer> entry : other.results.entrySet()) {
			final Integer myCount = results.get(entry.getKey());
			if (myCount == null)
				results.put(entry.getKey(), entry.getValue());
			else
				results.put(entry.getKey(), entry.getValue() + myCount);
		}

		return this;
	}
}

