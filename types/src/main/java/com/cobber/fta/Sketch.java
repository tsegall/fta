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

import java.util.Map;
import java.util.NavigableMap;

import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.InternalErrorException;
import com.datadoghq.sketch.ddsketch.DDSketch;
import com.datadoghq.sketch.ddsketch.DDSketches;

/**
 * This class is used to encapsulate a Sketch to provide Quantile data.  If the data fits in the cardinality set
 * then it simply uses a map to generate the quantiles.  Once the cardinality exceeds maxCardinality then the
 * data is tracked via DDSketch which provides for a specifiable relative-accuracy quantile sketch algorithms.
 */
public class Sketch {
	private final NavigableMap<String, Long> typedMap;
	protected long totalMapEntries;
	protected long totalSketchEntries;
	private DDSketch ddSketch;
	protected FTAType type;
	protected StringConverter stringConverter;
	protected double relativeAccuracy;
	private boolean isComplete = false;
	private final int debug;

	Sketch(final FTAType type, final NavigableMap<String, Long> typedMap, final StringConverter stringConverter, final double relativeAccuracy, final int debug) {
		this.type = type;
		this.stringConverter = stringConverter;
		this.relativeAccuracy = relativeAccuracy;
		this.typedMap = typedMap;
		this.debug = debug;

		ddSketch = DDSketches.unboundedDense(relativeAccuracy);
	}

	public void accept(final String key, final Long count) {
		ddSketch.accept(stringConverter.toDouble(key.trim()), count);
		totalSketchEntries += count;
	}

	public boolean isCardinalityExceeded() {
		return totalSketchEntries != 0;
	}

	public boolean isComplete() {
		return isComplete;
	}

	public void complete(final Map<String, Long> map) {
		for (final Map.Entry<String, Long> entry : map.entrySet()) {
			if (isCardinalityExceeded()) {
				ddSketch.accept(stringConverter.toDouble(entry.getKey().trim()), entry.getValue());
				totalSketchEntries += entry.getValue();
			}
			else {
				// Cardinality map - has entries that differ only based on whitespace, so for example it may include
				// "47" 10 times and " 47" 20 times, for the purposes of calculating quantiles these are coalesced
				// Similarly 47.0 and 47.000 will be collapsed since the typedMap is type aware and will consider these equal
				// This next try/catch is unnecessary in theory, if there are zero bugs then it will never trip,
				// if there happens to be an issue then we will lose this entry in the Cardinality map.
				try {
					typedMap.merge(entry.getKey().trim(), entry.getValue(), Long::sum);
					totalMapEntries += entry.getValue();
				}
				catch (RuntimeException e) {
					if (debug != 0)
						throw new InternalErrorException(e.getMessage(), e);
				}
			}
        }
		isComplete = true;
	}

	/**
	 * Get the value at the requested quantile.
	 * @param quantile a number between 0.0 and 1.0 (both included)
	 * @return the value at the specified quantile
	 */
	public String getValueAtQuantile(final double quantile) {
		// Check to see if the cardinality cache has been blown.
		if (isCardinalityExceeded())
			return stringConverter.formatted(stringConverter.fromDouble(ddSketch.getValueAtQuantile(quantile)));

		final long quest = Math.round(quantile * totalMapEntries);

		long upto = 0;
		for (final Map.Entry<String, Long> e : typedMap.entrySet()) {
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
