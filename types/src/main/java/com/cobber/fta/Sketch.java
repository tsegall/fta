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

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.Map;
import java.util.TreeMap;

import com.cobber.fta.core.FTAType;
import com.datadoghq.sketch.ddsketch.DDSketch;
import com.datadoghq.sketch.ddsketch.DDSketches;

/*
 * This class is used encapsulate a Sketch to provide Quantile data.  If the data fits in the cardinality set
 * then it simply uses a map to generate the cardinality.  Once the cardinality exceeds maxCardinality then the
 * data is tracked via DDSketch which provides for a specifiable relative-accuracy quantile sketch algorithms.
 */
public class Sketch {
	private TreeMap<String, Long> basis;
	protected long totalMapEntries;
	protected long totalSketchEntries;
	private DDSketch ddSketch;
	protected FTAType type;
	protected StringConverter stringConverter;
	protected double relativeAccuracy;
	private boolean isComplete = false;

	Sketch(final FTAType type, final StringConverter stringConverter, final double relativeAccuracy) {
		this.type = type;
		this.stringConverter = stringConverter;
		this.relativeAccuracy = relativeAccuracy;

		switch (type) {
		case BOOLEAN:
			basis = new TreeMap<>();
			break;
		case DOUBLE:
			basis = new TreeMap<>(new CommonComparator<Double>(stringConverter));
			break;
		case LOCALDATE:
			basis = new TreeMap<>(new CommonComparator<ChronoLocalDate>(stringConverter));
			break;
		case LOCALDATETIME:
			basis = new TreeMap<>(new CommonComparator<ChronoLocalDateTime<?>>(stringConverter));
			break;
		case LOCALTIME:
			basis = new TreeMap<>(new CommonComparator<LocalTime>(stringConverter));
			break;
		case LONG:
			basis = new TreeMap<>(new CommonComparator<Long>(stringConverter));
			break;
		case OFFSETDATETIME:
			basis = new TreeMap<>(new CommonComparator<OffsetDateTime>(stringConverter));
			break;
		case STRING:
			basis = new TreeMap<>();
			break;
		case ZONEDDATETIME:
			basis = new TreeMap<>(new CommonComparator<ChronoZonedDateTime<?>>(stringConverter));
			break;
		default:
			break;
		}

		ddSketch = DDSketches.unboundedDense(relativeAccuracy);
	}

	public void accept(String key, Long count) {
		ddSketch.accept(stringConverter.toDouble(key.trim()), count);
		totalSketchEntries += count;
	}

	public boolean isCardinalityExceeded() {
		return totalSketchEntries != 0;
	}

	public boolean isComplete() {
		return isComplete;
	}

	public void complete(Map<String, Long> map) {
		for (Map.Entry<String, Long> e : map.entrySet()) {
			if (isCardinalityExceeded()) {
				ddSketch.accept(stringConverter.toDouble(e.getKey().trim()), e.getValue());
				totalSketchEntries += e.getValue();
			}
			else {
				// Cardinality map - has entries that differ only based on whitespace, so for example it may include
				// "47" 10 times and " 47" 20 times, for the purposes of calculating quantiles we want these to be coalesced
				String key = e.getKey().trim();
				final Long seen = basis.get(key);
				if (seen == null)
					basis.put(key, e.getValue());
				else
					basis.put(key, seen + e.getValue());
				totalMapEntries += e.getValue();
			}
        }
		isComplete = true;
	}

	/**
	 * Get the value at the requested quantile.
	 * @param quantile a number between 0 and 1 (both included)
	 * @return the value at the specified quantile
	 */
	public String getValueAtQuantile(final double quantile) {
		// Check to see if the cardinality cache has been blown.
		if (isCardinalityExceeded())
			return stringConverter.formatted(stringConverter.fromDouble(ddSketch.getValueAtQuantile(quantile)));

		long quest = Math.round(quantile * totalMapEntries);

		long upto = 0;
		for (Map.Entry<String, Long> e : basis.entrySet()) {
			if (quest >= upto && quest <= upto + e.getValue())
				return e.getKey();
			upto += e.getValue();
        }

		return null;
	}

    protected void setDdSketch(final DDSketch ddSketch) {
		this.ddSketch = ddSketch;
	}

	protected DDSketch getDdSketch() {
		return ddSketch;
	}
}
