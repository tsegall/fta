/*
 * Copyright 2017-2023 Tim Segall
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

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import com.cobber.fta.core.InternalErrorException;

/**
 * This class is used to encapsulate a Histogram to provide Histogram data.  If the data fits in the cardinality set
 * then it simply uses a map to generate the histogram values.  Once the cardinality exceeds maxCardinality then the
 * data is tracked using an algorithm based on Yael Ben-Haim and Elad Tom-Tov, "A streaming parallel decision tree algorithm",
 * J. Machine Learning Research 11 (2010), pp. 849--872
 *
 * All data is stored in the Cardinality Map until this is exhausted at this point we start to populate (via accept) the underlying Histogram Sketch with all values
 * not captured in the Cardinality Map.  Once we need to generate a Histogram we either just generate it from the Cardinality Map or if the
 * MaxCardinality has been exceeded we add all the entries captured in the  Cardinality Map to the Sketch.
 */
public class Histogram {
	/**
	 * A Histogram Entry captures the low and high bounds for each bucket along with the number of entries in the bucket.
	 * NOTE: All the buckets except for the last are [low, high) the last bucket is [low,high].
	 */
	public class Entry {
		private String low;
		private String high;
		private double lowCut;
		private double highCut;
		private long count;
		private long clusterCount;
		private double clusterPercent;

		public Entry(final double lowCut, final double highCut) {
			this.lowCut = lowCut;
			this.highCut = highCut;
			this.low = stringConverter.formatted(stringConverter.fromDouble(lowCut));
			this.high = stringConverter.formatted(stringConverter.fromDouble(highCut));
		}

		public String getLow() {
			return low;
		}

		public String getHigh() {
			return high;
		}

		public double getLowCut() {
			return lowCut;
		}

		public double getHighCut() {
			return highCut;
		}

		public long getCount() {
			return count;
		}

		public void setCount(final long count) {
			this.count = count;
		}

		public long getClusterCount() {
			return clusterCount;
		}

		public double getClusterPercent() {
			return clusterPercent;
		}
	}

	private NavigableMap<String, Long> typedMap;
	private HistogramSPDT histogramOverflow;
	private StringConverter stringConverter;
	private int debug;

	Histogram() {
	}

	Histogram(final NavigableMap<String, Long> typedMap, final StringConverter stringConverter, final int bins, final int debug) {
		this.stringConverter = stringConverter;
		this.typedMap = typedMap;
		this.debug = debug;
	}

	public void setCardinality(final Map<String, Long> map) {
		typedMap.clear();
		for (final Map.Entry<String, Long> entry : map.entrySet()) {
			// Cardinality map - has entries that differ only based on whitespace, so for example it may include
			// "47" 10 times and " 47" 20 times, for the purposes of calculating histograms these are coalesced
			// Similarly 47.0 and 47.000 will be collapsed since the typedMap is type aware and will consider these equal
			// This next try/catch is unnecessary in theory, if there are zero bugs then it will never trip,
			// if there happens to be an issue then we will lose this entry in the Cardinality map.
			try {
				typedMap.merge(entry.getKey().trim(), entry.getValue(), Long::sum);
			}
			catch (RuntimeException e) {
				if (debug != 0)
					throw new InternalErrorException(e.getMessage(), e);
			}
		}
	}

	public void setCardinalityOverflow(final HistogramSPDT histogramSPDT) {
		this.histogramOverflow = histogramSPDT;
	}

	/**
	 * Get the histogram with the supplied number of buckets
	 * @param buckets the number of buckets in the Histogram
	 * @return An array of length 'buckets' that constitutes the Histogram (or null if cardinality is zero).
	 */
	public Histogram.Entry[] getHistogram(final int buckets) {
		final Histogram.Entry[] ret = new Entry[buckets];
		final double[] cutPoints = new double[buckets + 1];
		HistogramSPDT histogramFull = null;
		double low;
		double high;

		if (typedMap.isEmpty())
			return null;

		if (histogramOverflow != null) {
			histogramFull = new HistogramSPDT(histogramOverflow);
			for (final Map.Entry<String, Long> e : typedMap.entrySet())
				histogramFull.accept(e.getKey(), e.getValue());
			low = histogramFull.getMinValue();
			high = histogramFull.getMaxValue();
		}
		else {
			low = stringConverter.toDouble(typedMap.firstKey());
			high = stringConverter.toDouble(typedMap.lastKey());
		}

		final double cutSize = (high - low) / buckets;

		// Set the N+1 cut points
		for (int i = 0; i <= buckets; i++)
			cutPoints[i] = low + i * cutSize;

		// Set the low and high bounds on each bucket
		for (int i = 0; i < buckets; i++)
			ret[i] = new Entry(cutPoints[i], cutPoints[i + 1]);

		int upto = 0;

		if (histogramFull != null) {
			final List<HistogramSPDT.Bin> bins = histogramFull.getBins();
			for (final HistogramSPDT.Bin bin : bins) {
				// All the buckets except for the last are [low, high) the last bucket is [low,high]
				while (!inThisBucket(ret[upto], bin.value, buckets, upto))
					upto++;

				ret[upto].setCount(ret[upto].getCount() + bin.count);
			}
		}
		else {
			for (final Map.Entry<String, Long> e : typedMap.entrySet()) {
				// All the buckets except for the last are [low, high) the last bucket is [low,high]
				while (!inThisBucket(ret[upto], stringConverter.toDouble(e.getKey()), buckets, upto))
					upto++;

				ret[upto].setCount(ret[upto].getCount() + e.getValue());
			}
		}

		return ret;
	}

	private boolean inThisBucket(final Entry bucket, final double value, final int buckets, final int currentBucket) {
		// By definition any value cannot be lower than the first bucket low bound
		if (currentBucket == 0 && value < bucket.getLowCut())
			return true;

		// If this is the last bucket then any value is by definition < the high bound
		if (currentBucket == buckets - 1)
			return true;

		return value < bucket.getHighCut();
	}

	public Histogram merge(final Histogram other) {
		this.typedMap.putAll(other.typedMap);
		if (this.histogramOverflow != null && other.histogramOverflow != null) {
			// Both sides have overflow so merge
			this.histogramOverflow.merge(other.histogramOverflow);
		}
		else if (other.histogramOverflow != null && this.histogramOverflow == null) {
			// We have no overflow but the other side does
			this.histogramOverflow = other.histogramOverflow;
		}

		return this;
	}

	/**
	 * Given a Histogram analysis mark each bucket as part of a cluster and then attach the count and percent for the cluster
	 * to all buckets in the cluster.  For example, with the following distribution:
	 * 		1, 1, 0, 0, 0, 0, 10, 20, 30, 30, 8
	 * We would declare two clusters - the first one having 2% and the second having 98%, so the percentages would look as follows:
	 * 		2, 2, 0, 0, 0, 0, 98, 98, 98, 98, 98
	 * @param buckets The set of Histogram buckets for this analysis
	 */
	public static void tagClusters(final Histogram.Entry[] buckets) {
		long total = 0;
		for (Entry bucket : buckets)
			total += bucket.count;

		int clusterStart = -1;
		int clusterEnd = -1;
		long clusterTotal = 0;
		for (int i = 0; i < buckets.length; i++) {
			if (buckets[i].count == 0) {
				if (clusterStart == -1)
					continue;
				clusterEnd = i - 1;
				for (int c = clusterStart; c <= clusterEnd; c++) {
					buckets[c].clusterCount = clusterTotal;
					buckets[c].clusterPercent = (double)clusterTotal/total;
				}
				clusterStart = clusterEnd = -1;
				clusterTotal = 0;
				continue;
			}
			if (clusterStart == -1)
				clusterStart = i;
			clusterTotal += buckets[i].count;
		}
		for (int c = clusterStart; c <= buckets.length - 1; c++) {
			buckets[c].clusterCount = clusterTotal;
			buckets[c].clusterPercent = (double)clusterTotal/total;
		}
	}

	/**
	 * Given a value and set of buckets - locate the bucket holding this value.
	 * @param buckets The set of Histogram buckets for this analysis
	 * @param value The value we are searching for
	 * @return The bucket containing the supplied value.
	 */
	public static Histogram.Entry getBucket(final Histogram.Entry[] buckets, final double value) {
		for (int i = 0; i < buckets.length; i++) {
			// By definition any value cannot be lower than the first bucket low bound
			if (i == 0 && value < buckets[i].getLowCut())
				return buckets[0];

			// If this is the last bucket then any value is by definition < the high bound
			if (i == buckets.length - 1)
				return buckets[buckets.length - 1];

			if (value < buckets[i].getHighCut())
				return buckets[i];
		}

		// Should never get here!!
		return null;
	}
}
