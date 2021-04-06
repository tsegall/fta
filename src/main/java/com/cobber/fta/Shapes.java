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
package com.cobber.fta;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.core.RegExpSplitter;

/**
 * Class used to manage Shapes.
 */
public class Shapes {
	private final Map<String, Long> shapes = new HashMap<>();
	private final Map<String, Long> compressed = new TreeMap<>();
	private boolean anyShape;
	private boolean isCompressed;
	private int maxShapes;
	private long samples;
	private boolean isAlpha = true;
	private boolean isNumeric = true;
	private boolean isAlphaNumeric = true;
	private int minLength = Integer.MAX_VALUE;
	private int maxLength = Integer.MIN_VALUE;

	public Shapes(final int maxShapes) {
		this.maxShapes = maxShapes;
	}

	/**
	 * Get the 'best' Regular Expression we can based on the set of shapes.
	 * @return The 'best' Regular Expression we can based on the set of shapes, or null if nothing clever can be discerned.
	 */
	public String getRegExp() {
		compressShapes();

		if (anyShape)
			return null;

		if (compressed.size() == 1)
			return Smashed.smashedAsRegExp(compressed.keySet().iterator().next().trim());

		if (compressed.size() == 2 && samples > 100) {
			final Iterator<Map.Entry<String, Long>> iter = compressed.entrySet().iterator();
			final Map.Entry<String, Long> firstShape = iter.next();
			final Map.Entry<String, Long> secondShape = iter.next();

			if (firstShape.getValue() > samples * 15/100 && secondShape.getValue() > samples * 15/100)
				return RegExpGenerator.merge(Smashed.smashedAsRegExp(firstShape.getKey()), Smashed.smashedAsRegExp(secondShape.getKey()));
		}

		Map<String, Long> updatedShapes = new HashMap<>();

		final Pattern decimalNumberPattern = Pattern.compile("(?:[^-+0-9\\.]|^)([+-]?[0-9]+\\.[0-9]+)(?:[^0-9\\.]|$)");

		// Shrink the shapes map to collapse multiple instances of a float (no exponent) with one.
	    // e.g. we might have in the map $111.11, $11.11, and $-1111.11 and we will replace this with '$%f'
		for (final Map.Entry<String, Long> shape : shapes.entrySet()) {

			final String original = shape.getKey();
			final Matcher matcher = decimalNumberPattern.matcher(original);
			final StringBuilder updatedSB = new StringBuilder(original.length());

			int offset = 0;
			while (matcher.find(offset)) {
				updatedSB.append(original.substring(offset, matcher.start(1))).append("%f");
				offset = matcher.end(1);
		    }
		    final String updated = updatedSB.length() == 0 ? original : updatedSB.toString();

		    final Long seen = updatedShapes.get(updated);
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
		for (final Map.Entry<String, Long> shape : shapes.entrySet()) {

			final String original = shape.getKey();
			if (original.indexOf('X') != -1) {
				isHex = false;
				break;
			}
			final String updated = original.replace('9', 'H').replace('x', 'H');

		    final Long seen = updatedShapes.get(updated);
			if (seen == null)
				updatedShapes.put(updated, shape.getValue());
			else
				updatedShapes.put(updated, seen + shape.getValue());
			if (updatedShapes.size() > 1)
				break;
		}
		if (isHex && updatedShapes.size() == 1)
			return Smashed.smashedAsRegExp(updatedShapes.entrySet().iterator().next().getKey());

		// We had nothing clever to say - so return a simple classification if possible
		String pattern = null;
		if (isAlpha || isNumeric) {
			pattern = isAlpha ? KnownPatterns.PATTERN_ALPHA : KnownPatterns.PATTERN_NUMERIC;
			if (compressed.size() == maxLength - minLength + 1)
				pattern += RegExpSplitter.qualify(minLength, maxLength);
			else
				pattern += "+";
		}
		else if (isAlphaNumeric) {
			pattern = KnownPatterns.PATTERN_ALPHANUMERIC;
			pattern += minLength == maxLength ? RegExpSplitter.qualify(minLength, maxLength) : "+";
		}

		return pattern;
	}

	/**
	 * Track the supplied shape.
	 *
	 * @param trimmed Track the supplied shape.
	 * @param count The count of the supplied shape.
	 */
	public void track(final String trimmed, final long count) {
		samples += count;
		if (!anyShape) {
			final String inputShape = Smashed.smash(trimmed);
			if (".+".equals(inputShape))
				anyShape = true;
			else {
				final Long seen = shapes.get(inputShape);
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
				shapes.clear();
		}
	}

	/**
	 * Get the 'best' shape - where 'best' is the one with the highest count.
	 * @return The 'best' shape entry (shape and count).
	 */
	public Map.Entry<String, Long> getBest() {
		compressShapes();
		return Collections.max(compressed.entrySet(), Map.Entry.comparingByValue());
	}

	/**
	 * Get the Map of shapes.
	 * @return The ordered (by shape) Map of all shapes.
	 */
	public Map<String, Long> getShapes() {
		compressShapes();
		return compressed;
	}

	/**
	 * Get the size of the Map.
	 * @return The Map size.
	 */
	public int size() {
		compressShapes();
		return compressed.size();
	}

	private void compressShapes() {
		if (isCompressed)
			return;

		for (final Map.Entry<String, Long> shape : shapes.entrySet()) {
			final String upperKey = shape.getKey().toUpperCase(Locale.ROOT);
			final Long seen = compressed.get(upperKey);
			isAlpha = isAlpha && isAlpha(upperKey);
			isNumeric = isNumeric && isNumeric(upperKey);
			isAlphaNumeric = isAlphaNumeric && isAlphaNumeric(upperKey);
			final int len = upperKey.length();
			if (len < minLength)
				minLength = len;
			if (len > maxLength)
				maxLength = len;
			if (seen == null)
				compressed.put(upperKey, shape.getValue());
			else
				compressed.put(upperKey, seen + shape.getValue());
		}
		isCompressed = true;
	}

	private boolean isAlpha(final String input) {
		for (int i = 0; i < input.length(); i++)
			if (input.charAt(i) != 'X')
				return false;
		return true;
	}

	private boolean isNumeric(final String input) {
		for (int i = 0; i < input.length(); i++)
			if (input.charAt(i) != '9')
				return false;
		return true;
	}

	private boolean isAlphaNumeric(final String input) {
		for (int i = 0; i < input.length(); i++)
			if (input.charAt(i) != 'X' && input.charAt(i) != '9')
				return false;
		return true;
	}
}
