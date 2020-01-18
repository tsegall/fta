/*
 * Copyright 2017-2020 Tim Segall
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class used to manage Shapes.
 */
public class Shapes {
	private Map<String, Long> shapes = new HashMap<>();
	private Map<String, Long> compressed = new TreeMap<>();
	private boolean anyShape = false;
	private boolean isCompressed = false;
	private int maxShapes = -1;

	Shapes(int maxShapes) {
		this.maxShapes = maxShapes;
	}

	/**
	 * Get the 'best' Regular Expression we can based on the set of shapes.
	 * Note: this will commonly return null, unless we can do something clever.
	 *
	 * @param realSamples The number of real samples that we have observed.
	 */
	String getRegExp(long realSamples) {
		compressShapes();

		if (anyShape)
			return null;

		if (compressed.size() == 1)
			return Smashed.smashedAsRegExp(compressed.keySet().iterator().next().trim());

		if (compressed.size() == 2 && realSamples > 100) {
			Iterator<Map.Entry<String, Long>> iter = compressed.entrySet().iterator();
			Map.Entry<String, Long> firstShape = iter.next();
			Map.Entry<String, Long> secondShape = iter.next();

			if (firstShape.getValue() > realSamples * 15/100 && secondShape.getValue() > realSamples * 15/100) {
				String firstRE = Smashed.smashedAsRegExp(firstShape.getKey());
				String secondRE = Smashed.smashedAsRegExp(secondShape.getKey());
				return RegExpGenerator.merge(firstRE, secondRE);
			}
		}

		Map<String, Long> updatedShapes = new HashMap<>();

		Pattern decimalNumberPattern = Pattern.compile("(?:[^-+0-9\\.]|^)([+-]?[0-9]+\\.[0-9]+)(?:[^0-9\\.]|$)");

		// Shrink the shapes map to collapse multiple instances of a float (no exponent) with one.
	    // e.g. we might have in the map $111.11, $11.11, and $-1111.11 and we will replace this with '$%f'
		for (Map.Entry<String, Long> shape : shapes.entrySet()) {

			String original = shape.getKey();
			Matcher matcher = decimalNumberPattern.matcher(original);
			StringBuilder updatedSB = new StringBuilder(original.length());

			int offset = 0;
			while (matcher.find(offset)) {
				updatedSB.append(original.substring(offset, matcher.start(1))).append("%f");
				offset = matcher.end(1);
		    }
		    String updated = updatedSB.length() == 0 ? original : updatedSB.toString();

		    Long seen = updatedShapes.get(updated);
			if (seen == null)
				updatedShapes.put(updated, shape.getValue());
			else
				updatedShapes.put(updated, seen + shape.getValue());
			if (updatedShapes.size() > 1)
				break;
		}
		if (updatedShapes.size() == 1)
			return Smashed.smashedAsRegExp(updatedShapes.entrySet().iterator().next().getKey());

		updatedShapes = new HashMap<>();
		boolean isHex = true;
		for (Map.Entry<String, Long> shape : shapes.entrySet()) {

			String original = shape.getKey();
			if (original.indexOf('X') != -1) {
				isHex = false;
				break;
			}
			String updated = original.replace('9', 'H').replace('x', 'H');

		    Long seen = updatedShapes.get(updated);
			if (seen == null)
				updatedShapes.put(updated, shape.getValue());
			else
				updatedShapes.put(updated, seen + shape.getValue());
			if (updatedShapes.size() > 1)
				break;
		}
		if (isHex && updatedShapes.size() == 1)
			return Smashed.smashedAsRegExp(updatedShapes.entrySet().iterator().next().getKey());


		return null;
	}

	/**
	 * Track the supplied shape.
	 *
	 * @param trimmed Track the supplied shape.
	 * @param count The count of the supplied shape.
	 */
	void track(final String trimmed, long count) {
		if (!anyShape) {
			String inputShape = Smashed.smash(trimmed);
			if (inputShape.equals(".+"))
				anyShape = true;
			else {
				Long seen = shapes.get(inputShape);
				isCompressed = false;
				if (seen == null)
					if (shapes.size() < maxShapes)
						shapes.put(inputShape, count);
					else
						anyShape = true;
				else
					shapes.put(inputShape, seen + count);
			}

			// If we overflow or we decide it is not meaningful - then just throw the analysis away
			if (anyShape)
				shapes = new HashMap<>();
		}
	}

	/**
	 * Get the 'best' shape - where 'best' is the one with the highest count.
	 * @return The 'best' shape entry (shape and count).
	 */
	Map.Entry<String, Long> getBest() {
		compressShapes();
		return Collections.max(compressed.entrySet(), Map.Entry.comparingByValue());
	}

	/**
	 * Get the Map of shapes.
	 * @return The ordered (by shape) Map of all shapes.
	 */
	Map<String, Long> getShapes() {
		compressShapes();
		return compressed;
	}

	/**
	 * Get the size of the Map.
	 * @return The Map size.
	 */
	int size() {
		compressShapes();
		return compressed.size();
	}

	private void compressShapes() {
		if (isCompressed)
			return;

		for (Map.Entry<String, Long> shape : shapes.entrySet()) {
			String upperKey = shape.getKey().toUpperCase();
		    Long seen = compressed.get(upperKey);
			if (seen == null)
				compressed.put(upperKey, shape.getValue());
			else
				compressed.put(upperKey, seen + shape.getValue());
		}
		isCompressed = true;
	}
}
