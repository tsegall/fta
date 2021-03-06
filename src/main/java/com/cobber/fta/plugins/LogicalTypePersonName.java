/*
 * Copyright 2017-2021 Tim Segall
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
package com.cobber.fta.plugins;

import java.util.Iterator;
import java.util.Map;

import com.cobber.fta.LogicalTypeFiniteSimple;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.Shapes;
import com.cobber.fta.FactsTypeBased;

public abstract class LogicalTypePersonName extends LogicalTypeFiniteSimple {
	private class Dodge {
		Iterator<String> iter;
	}

	public static final String REGEXP = "[- \\p{IsAlphabetic}]*";
	// The threshold we use if we have a strong signal from the header
	private static final int IDENTIFIED_LOW_THRESHOLD = 40;
	// The threshold we use if we have a moderate signal from the header
	private static final int IDENTIFIED_HIGH_THRESHOLD = 60;
	private static final int ITERS = 5;
	private Dodge[] iterators;

	public LogicalTypePersonName(final PluginDefinition plugin, final String filename) {
		super(plugin, REGEXP, ".*", 95);
		setContent("resource", "/reference/" + filename);
	}

	@Override
	public String nextRandom() {
		// No easy way to get a random element from a set, could convert to array but that uses lots of memory,
		// so simulate randomness by having a number of iterators start at different points in the set
		if (iterators == null) {
			iterators = new Dodge[ITERS];
			for (int i = 0; i < iterators.length; i++) {
				iterators[i] = new Dodge();
				iterators[i].iter = getMembers().iterator();
				final int offset = random.nextInt(getMembers().size() / 2);
				for (int j = 0; j < offset; j++)
					iterators[i].iter.next();
			}
		}
		final Dodge any = iterators[random.nextInt(ITERS)];
		if (!any.iter.hasNext())
			any.iter = getMembers().iterator();
		return any.iter.next();
	}

	/*
	 * Note: The input String will be both trimmed and converted to upper Case
	 * @see com.cobber.fta.LogicalType#isValid(java.lang.String)
	 */
	@Override
	public boolean isValid(final String input) {
		final String trimmedUpper = input.trim().toUpperCase(locale);
		if (trimmedUpper.length() < minLength && trimmedUpper.length() > maxLength)
			return false;
		if (getMembers().contains(trimmedUpper))
			return true;

		// For the balance of the 'not found' we will say they are invalid if it is not just a single word
		for (int i = 0; i < trimmedUpper.length(); i++) {
			if (!Character.isAlphabetic(trimmedUpper.charAt(i)))
				return false;
		}

		// Assume 40% of the remaining are good - hopefully this will not bias the determination excessively.
		// Use hashCode as opposed to random() to ensure that a given data set gives the same results from one run to another.
		return input.hashCode() % 10 < 4;
	}

	@Override
	public String isValidSet(final String dataStreamName, final long matchCount, final long realSamples,
			final FactsTypeBased facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final Shapes shapes) {

		final int headerConfidence = getHeaderConfidence(dataStreamName);

		if (headerConfidence >= 90 && (double)matchCount / realSamples >= (double)IDENTIFIED_LOW_THRESHOLD/100)
			return null;

		int minCardinality = 10;
		int minSamples = 20;
		if (headerConfidence != 0) {
			minCardinality = 5;
			minSamples = 5;
		}

		if (cardinality.size() < minCardinality)
			return backout;

		if (realSamples < minSamples)
			return backout;

		if (headerConfidence >= 50 && (double)matchCount / realSamples >= (double)IDENTIFIED_HIGH_THRESHOLD/100)
			return null;

		return (double)matchCount / realSamples >= getThreshold()/100.0 ? null : backout;
	}

	@Override
	public boolean isClosed() {
		return false;
	}
}
