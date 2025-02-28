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
package com.cobber.fta.token;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.cobber.fta.KnownTypes;
import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.core.RegExpSplitter;

/**
 * This Singleton class captures the sum of all input observed as a set of instances of the TokenStream Class.
 */
public class TokenStreams {
	private final Map<String, TokenStream> tokenStreams = new HashMap<>();
	private final int maxStreams;
	private boolean anyShape;
	private long samples;

	/**
	 * Construct a TokenStreams object with a maximum number of TokenStream instances.
	 * @param maxStreams The maximum number of TokenStream instances.
	 */
	public TokenStreams(final int maxStreams) {
		this.maxStreams = maxStreams;
	}

	/**
	 * Track the supplied input.
	 *
	 * @param trimmed The trimmed input.
	 * @param count The number of occurrences of this input.
	 */
	public void track(final String trimmed, final long count) {
		samples += count;
		if (anyShape)
			return;

		// If the incoming entry is too long - then ditch what we have and call it a day
		if (TokenStream.tooLong(trimmed)) {
			tokenStreams.clear();
			tokenStreams.put(TokenStream.ANYSHAPE.getKey(), TokenStream.ANYSHAPE);
			anyShape = true;
			return;
		}

		final String key = TokenStream.getKey(trimmed);
		final TokenStream current = tokenStreams.get(key);
		if (current == null)
            // New Stream found - add it if there is room, otherwise call it a day
			if (tokenStreams.size() < maxStreams)
				tokenStreams.put(key, new TokenStream(trimmed, count));
			else {
                tokenStreams.clear();
                tokenStreams.put(TokenStream.ANYSHAPE.getKey(), TokenStream.ANYSHAPE);
                anyShape = true;
			}
		else {
			if (current.isComplete())
				current.mergeCount(count);
			else
				current.merge(new TokenStream(trimmed, count));
		}
	}

	public boolean isAnyShape() {
		return anyShape;
	}

	public boolean isFull() {
		return tokenStreams.size() + 1 == maxStreams;
	}

	/**
	 * @return The number of inputs this TokenStream has captured.
	 */
	public long getSamples() {
		return samples;
	}

	/**
	 * Get the 'best' Regular Expression we can based on the set of TokenStreams.
	 * @param fitted If true the Regular Expression should be a 'more closely fitted' Regular Expression.
	 * @return The 'best' Regular Expression we can based on the set of TokenStreams, or null if nothing clever can be discerned.
	 */
	public String getRegExp(final boolean fitted) {
		if (tokenStreams.isEmpty())
			return null;

		if (tokenStreams.size() == 1)
			return tokenStreams.values().iterator().next().getRegExp(fitted);

		if (tokenStreams.size() == 2 && samples >= 100) {
			final Iterator<Map.Entry<String, TokenStream>> iter = tokenStreams.entrySet().iterator();
			final TokenStream firstTokenStream = iter.next().getValue();
			final TokenStream secondTokenStream = iter.next().getValue();

			if (firstTokenStream.getOccurrences() > samples * 15/100 && secondTokenStream.getOccurrences() > samples * 15/100) {
				return RegExpGenerator.merge(firstTokenStream.getRegExp(fitted), secondTokenStream.getRegExp(fitted));
			}
		}

		// Look for multiple constant length strings that are able to be merged (typically by promoting a digit or alpha to an alphadigit
		TokenStream newStream = null;
		for (final TokenStream tokenStream : tokenStreams.values()) {
			if (newStream == null)
				newStream = new TokenStream(tokenStream);
			else
				if ((newStream = newStream.merge(tokenStream)) == null)
					break;
		}

		if (newStream != null)
			return newStream.simplify().getRegExp(fitted);

		// Check to see if the compressed TokenStreams are all similar
		TokenStream compressedResult = null;
		boolean compressedEqual = true;
		for (final TokenStream tokenStream : tokenStreams.values()) {
			if (compressedResult == null)
				compressedResult = new TokenStream(tokenStream);
			else if (compressedResult.getCompressedKey().equals(tokenStream.getCompressedKey()))
				compressedResult.mergeCompressed(tokenStream);
			else
				compressedEqual = false;
		}
		if (compressedEqual)
			return compressedResult.getRegExp(fitted);

		// Check to see if we have a simple Alpha, Numeric or AlphaNumeric
		boolean isAlpha = true;
		boolean isNumeric = true;
		boolean isAlphaNumeric = true;
		int minLength = Integer.MAX_VALUE;
		int maxLength = Integer.MIN_VALUE;
		for (final TokenStream tokenStream : tokenStreams.values()) {
			isAlpha &= tokenStream.isAlpha();
			isNumeric &= tokenStream.isNumeric();
			isAlphaNumeric &= (tokenStream.isAlphaNumeric() || tokenStream.isAlpha() || tokenStream.isNumeric());
			minLength = Math.min(minLength, tokenStream.getKey().length());
			maxLength = Math.max(maxLength, tokenStream.getKey().length());
		}

		if (isAlpha || isNumeric || isAlphaNumeric) {
			String pattern = "";
			if (isAlpha) {
				pattern = KnownTypes.PATTERN_ALPHA;
			}
			else if (isNumeric) {
				pattern = KnownTypes.PATTERN_NUMERIC;
			}
			else if (isAlphaNumeric) {
				pattern = KnownTypes.PATTERN_ALPHANUMERIC;
			}
			if (tokenStreams.size() == maxLength - minLength + 1)
				pattern += RegExpSplitter.qualify(minLength, maxLength);
			else
				pattern += "+";
			return pattern;
		}

		return null;
	}

	/**
	 * Check if the TokenStreams (i.e. if all member TokenStream's) match the supplied Regular Expression
	 * at the supplied confidence level.
	 * @param regExp The Regular Expression to match.
	 * @param confidence The confidence we require to determine if this is a match.
	 * @return The number of matches if the TokenStreams matches the supplied Regular Expression at the supplied confidence level or 0 otherwise.
	 */
	public long matches(final String regExp, final int confidence) {
		if (anyShape)
			return 0;

		if (tokenStreams.isEmpty())
			return 0;

		// First calculate the total number of entries
		long total = 0;
		for (final TokenStream tokenStream : tokenStreams.values())
			total += tokenStream.getOccurrences();

		long misses = 0;
		for (final TokenStream tokenStream : tokenStreams.values()) {
			if (!tokenStream.matches(regExp)) {
				misses += tokenStream.getOccurrences();
				// We can bail as soon as the misses exceeds the threshold since it cannot get better
				if ((double)misses/total > (double)(100 - confidence)/100)
					return 0;
			}
		}

		return total - misses;
	}

	/**
	 * Get the 'best' TokenStream - where 'best' is the one with the highest count.
	 * @return The 'best' TokenStream entry.
	 */
	public TokenStream getBest() {
		TokenStream ret = null;
		long best = -1;
		for (final TokenStream tokenStream : tokenStreams.values())
			if (tokenStream.getOccurrences() > best) {
				ret = tokenStream;
				best = tokenStream.getOccurrences();
			}

		return ret;
	}

	/**
	 * Get the Map of shapes.
	 * @return The ordered (by shape) Map of all shapes.
	 */
	public Map<String, Long> getShapes() {
		final Map<String, Long> shapes = new TreeMap<>();

		if (!anyShape)
			for (final Map.Entry<String, TokenStream> entry : tokenStreams.entrySet())
				shapes.put(entry.getKey(), entry.getValue().getOccurrences());

		return shapes;
	}

	/**
	 * Get the Map of streams.
	 * @return The Map of all streams.
	 */
	public Map<String, TokenStream> getStreams() {
		return tokenStreams;
	}

	/**
	 * Get the size of the Map.
	 * @return The Map size.
	 */
	public int size() {
		return tokenStreams.size();
	}
}
