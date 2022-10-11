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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * This class is used to encapsulate a Histogram to provide Histogram data.  If the data fits in the cardinality set
 * then it simply uses a map to generate the histogram values.  Once the cardinality exceeds maxCardinality then the
 * data is tracked using an algorithm based on Yael Ben-Haim and Elad Tom-Tov, "A streaming parallel decision tree algorithm",
 * J. Machine Learning Research 11 (2010), pp. 849--872
 */
public class HistogramSPDT {
	class Bin implements Comparable<Object> {
		double value;
		long count;

		Bin(final double value, final long count) {
			this.value = value;
			this.count = count;
		}

		@Override
		public int compareTo(Object other) {
	    	return Double.compare(value, ((Bin)other).value);
	    }
	}

	// The array of bins we use to track the Histogram
	private ArrayList<Bin> bins;
	// The configured maximum number of bins
	private int maxBins;
	// Minimum value ever observed
	private double minValue = Double.MAX_VALUE;
	// Maximum value ever observed
	private double maxValue = Double.MIN_VALUE;
	private Random random;

	HistogramSPDT(final int maxBins) {
		this.bins = new ArrayList<>();
		this.maxBins = maxBins;
		this.random = new Random(3141592);
	}

	/**
	 * Add a new data point with an associated count to the histogram approximation.
	 *
	 * @param value The data point to add to the histogram approximation.
	 * @param count The count associated with this data point
	 */
	public void accept(double value, long count) {
//		System.err.printf("accept(%4.2f, %d)%n", value, count);

		if (value < minValue)
			minValue = value;
		if (value > maxValue)
			maxValue = value;

		if (bins.size() == 0) {
			bins.add(new Bin(value, count));
			return;
		}

		int index = locateInsertion(value);

		// Check to see if we already have an identical entry
		if (index < bins.size() && bins.get(index).value == value) {
			bins.get(index).count += count;
			return;
		}

		bins.add(index, new Bin(value, count));

		// We may have overflowed so trim if necessary
		trim();
	}

	/**
	 * Accessor for the ArrayList of all bins.
	 * @return The bins that constitute the Histogram.
	 */
	public ArrayList<Bin> getBins() {
		return bins;
	}

	/**
	 * Retrieve the minimum value.
	 * @return The minimum value ever seen by this Histogram
	 */
	public double getMinValue() {
		return minValue;
	}

	/**
	 * Retrieve the maximum value.
	 * @return The maximum value ever seen by this Histogram
	 */
	public double getMaxValue() {
		return maxValue;
	}

	/*
	 * Trim the number of buckets back to the configured maximum. Typically called after add and
	 * hence only removing one entry but also used post merge to collapse two sets of bins into one.
	 */
	private void trim() {
		while (bins.size() > maxBins) {
			double delta = Double.MAX_VALUE;
			int index = -1;
			int matchEqual = 0;

			// Find the two adjacent items with the smallest delta as these are the ones we need to merge
			// If there are multiple candidates with identical deltas then choose one at random
			for (int i = 0; i + 1 < bins.size(); i++) {
				double newDelta = bins.get(i + 1).value - bins.get(i).value;
				if (newDelta < delta) {
					delta = newDelta;
					index = i;
					matchEqual = 1;
				}
				else if (newDelta == delta && random.nextDouble() <= (1.0/++matchEqual))
					index = i;
			}

			// Adjust the current item to reflect the merge of it and its successor, then remove the successor
			long newCount = bins.get(index).count + bins.get(index + 1).count;
			bins.get(index).value = (bins.get(index).value * bins.get(index).count + bins.get(index + 1).value * bins.get(index + 1).count) /
					newCount;
			bins.get(index).count = newCount;
			bins.remove(index + 1);
		}
	}

	public HistogramSPDT merge(HistogramSPDT other) {
		// Smash the two lists together and then sort them
		bins.addAll(other.bins);
		Collections.sort(bins);

		// Update the min/max of the Histogram
		if (other.getMinValue() < getMinValue())
			minValue = other.getMinValue();
		if (other.getMaxValue() > getMinValue())
			maxValue = other.getMaxValue();

		// Chop the merged entity back to our maximum size
		trim();

		return this;
	}

	public void dump() {
		for (int i = 0; i < bins.size(); i++) {
			System.err.printf("%d: %4.2f, %d%n", i, bins.get(i).value, bins.get(i).count);
		}

	}

	// Locate the insertion point in the list where this value belongs.
	private int locateInsertion(final double value) {
		for (int i = 0; i < bins.size(); i++) {
			if (value > bins.get(i).value)
				continue;
			return i;
		}

		return bins.size();
	}
}