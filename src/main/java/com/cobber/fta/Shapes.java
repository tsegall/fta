package com.cobber.fta;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Shapes {
	private Map<String, Long> shapes = new HashMap<>();
	private Map<String, Long> compressed = new TreeMap<>();
	private boolean anyShape = false;
	private boolean isCompressed = false;
	private int maxShapes = -1;

	Shapes(int maxShapes) {
		this.maxShapes = maxShapes;
	}

	String getRegExp(long realSamples) {
		compressShapes();

		if (anyShape)
			return null;

		if (compressed.size() == 1)
			return RegExpGenerator.smashedAsRegExp(compressed.keySet().iterator().next().trim());

		if (compressed.size() == 2 && realSamples > 100) {
			Iterator<Map.Entry<String, Long>> iter = compressed.entrySet().iterator();
			Map.Entry<String, Long> firstShape = iter.next();
			Map.Entry<String, Long> secondShape = iter.next();

			if (firstShape.getValue() > realSamples * 15/100 && secondShape.getValue() > realSamples * 15/100) {
				String firstRE = RegExpGenerator.smashedAsRegExp(firstShape.getKey());
				String secondRE = RegExpGenerator.smashedAsRegExp(secondShape.getKey());
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
			StringBuffer updatedSB = new StringBuffer(original.length());

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
			return RegExpGenerator.smashedAsRegExp(updatedShapes.entrySet().iterator().next().getKey());

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
			return RegExpGenerator.smashedAsRegExp(updatedShapes.entrySet().iterator().next().getKey());


		return null;
	}

	void track(final String trimmed, long count) {
		if (!anyShape) {
			String inputShape = RegExpGenerator.smash(trimmed);
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

	Map.Entry<String, Long> getBest() {
		compressShapes();
		return Collections.max(compressed.entrySet(), Map.Entry.comparingByValue());
	}

	Map<String, Long> getShapes() {
		compressShapes();
		return compressed;
	}

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
