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

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.cobber.fta.core.FTAType;

public class CommonComparator<T extends Comparable<? super T>> implements Comparator<String> {
	private final StringConverter stringConverter;

	public CommonComparator(final StringConverter stringConverter) {
		this.stringConverter = stringConverter;
	}

	@Override
	public int compare(final String input1, final String input2) {
		final T val1 = (T)stringConverter.getValue(input1);
		if (val1 == null)
			System.err.println("*** Failed to convert ... " + input1);
		final T val2 = (T)stringConverter.getValue(input2);
		final int value = val1.compareTo(val2);

        if (value < 0)
            return -1;

        if (value > 0)
            return 1;

        return 0;
    }

	public static NavigableMap<String, Long> getTypedMap(final FTAType type, final StringConverter stringConverter) {
		switch (type) {
		case BOOLEAN:
			return new TreeMap<>();
		case DOUBLE:
			return new TreeMap<>(new CommonComparator<Double>(stringConverter));
		case LOCALDATE:
			return new TreeMap<>(new CommonComparator<ChronoLocalDate>(stringConverter));
		case LOCALDATETIME:
			return new TreeMap<>(new CommonComparator<ChronoLocalDateTime<?>>(stringConverter));
		case LOCALTIME:
			return new TreeMap<>(new CommonComparator<LocalTime>(stringConverter));
		case LONG:
			return new TreeMap<>(new CommonComparator<Long>(stringConverter));
		case OFFSETDATETIME:
			return new TreeMap<>(new CommonComparator<OffsetDateTime>(stringConverter));
		case STRING:
			return new TreeMap<>();
		case ZONEDDATETIME:
			return new TreeMap<>(new CommonComparator<ChronoZonedDateTime<?>>(stringConverter));
		default:
			return null;
		}
	}
}
