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

import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;

import com.cobber.fta.core.FTAType;

/**
 * This class is used to encapsulate a Histogram to provide Histogram data.  If the data fits in the cardinality set
 * then it simply uses a map to generate the histogram values.  Once the cardinality exceeds maxCardinality then the
 * data is tracked using an algorithm based on Yael Ben-Haim and Elad Tom-Tov, "A streaming parallel decision tree algorithm",
 * J. Machine Learning Research 11 (2010), pp. 849--872
 */
public class Histogram {
	/**
	 * A Histogram Entry captures the low and high bounds for each bucket along with the number of entries in the bucket.
	 * NOTE: All the buckets except for the last are [low, high) the last bucket is [low,high].
	 */
	public class Entry {
		private String low;
		private String high;
		private long count;

		public Entry(final String low, final String high) {
			this.low = low;
			this.high = high;
		}

		public String getLow() {
			return low;
		}

		public String getHigh() {
			return high;
		}

		public long getCount() {
			return count;
		}

		public void setCount(long count) {
			this.count = count;
		}
	}

	private NavigableMap<String, Long> typedMap;
	protected long totalMapEntries;
	protected long totalHistogramEntries;
	protected FTAType type;
	protected StringConverter stringConverter;
	private boolean isComplete = false;

	Histogram(final FTAType type, final NavigableMap<String, Long> typedMap, final StringConverter stringConverter) {
		this.type = type;
		this.stringConverter = stringConverter;
		this.typedMap = typedMap;
	}

	public void accept(String key, Long count) {
		// histogram.add(key, count);
		totalHistogramEntries += count;
	}

	public boolean isCardinalityExceeded() {
		return totalHistogramEntries != 0;
	}

	public boolean isComplete() {
		return isComplete;
	}

	public void complete(Map<String, Long> map) {
		for (Map.Entry<String, Long> e : map.entrySet()) {
			if (isCardinalityExceeded()) {
//				histogram.accept(stringConverter.toDouble(e.getKey().trim()), e.getValue());
				totalHistogramEntries += e.getValue();
			}
			else {
				// Cardinality map - has entries that differ only based on whitespace, so for example it may include
				// "47" 10 times and " 47" 20 times, for the purposes of calculating histograms these are coalesced
				// Similarly 47.0 and 47.000 will be collapsed since the typedMap is type aware and will consider these equal
				typedMap.merge(e.getKey().trim(), e.getValue(), Long::sum);
				totalMapEntries += e.getValue();
			}
        }
		isComplete = true;
	}

	/**
	 * Get the histogram with the supplied number of buckets
	 * @param buckets the number of buckets in the Histogram
	 * @return An array of length 'buckets' that constitutes the Histogram
	 */
	public Histogram.Entry[] getHistogram(final int buckets) {
		final Histogram.Entry[] ret = new Entry[buckets];
		final double low = stringConverter.toDouble(typedMap.firstKey());
		final double high = stringConverter.toDouble(typedMap.lastKey());
		final double[] cutPoints = new double[buckets + 1];
		String[] cutPointStrings = new String[buckets + 1];
		final double cutSize = (high - low) / buckets;

		// Set the N+1 cut points
		for (int i = 0; i <= buckets; i++) {
			cutPoints[i] = low + i * cutSize;
			cutPointStrings[i] = stringConverter.formatted(stringConverter.fromDouble(cutPoints[i]));
		}

		// Set the low and high bounds on each bucket
		for (int i = 0; i < buckets; i++)
			ret[i] = new Entry(cutPointStrings[i], cutPointStrings[i + 1]);

		int upto = 0;
		long count = 0;
		Comparator<? super String> c = typedMap.comparator();
		for (Map.Entry<String, Long> e : typedMap.entrySet()) {
			// All the buckets except for the last are [low, high) the last bucket is [low,high]
			if ((upto < buckets - 1 && c.compare(e.getKey(), ret[upto].getHigh()) < 0) ||
					upto == buckets - 1 && c.compare(e.getKey(), ret[upto].getHigh()) <= 0)
				count += e.getValue();
			else {
				ret[upto].setCount(count);
				upto++;
				count = e.getValue();
			}
        }

		if (upto < buckets)
			ret[upto].setCount(count);

		return ret;
	}
}
