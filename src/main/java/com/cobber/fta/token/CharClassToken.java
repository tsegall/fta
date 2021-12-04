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
package com.cobber.fta.token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.cobber.fta.core.RegExpSplitter;

/**
 * A CharClassToken captures a Regular Expression Character Class.
 * The performance of this class and merge() in particular are critical to the overall performance of FTA.
 * We split the tracking into ASCII and non-ASCII characters to improve performance.
 */
public class CharClassToken extends Token {
	private int countASCII = 0;
	private boolean[] seenASCII = new boolean[128];
	private int lowASCII = Integer.MAX_VALUE;
	private int highASCII = Integer.MIN_VALUE;
	private int maxSetASCII = -1;
	private int countNonASCII = 0;
	private Set<Character> seenNonASCII = new TreeSet<>();
	private List<CharClassToken> children = null;

	CharClassToken(Token.Type type, char ch) {
		super(type);

		if (ch < 128) {
			seenASCII[ch] = true;
			lowASCII = highASCII = ch;
			countASCII++;
		}
		else {
			seenNonASCII.add(ch);
			countNonASCII = 1;
		}

		if (type.equals(Token.Type.DIGIT_CLASS))
			maxSetASCII = 10;
		else if (type.equals(Token.Type.ALPHA_CLASS))
			maxSetASCII = 52;
		else if (type.equals(Token.Type.ALPHADIGIT_CLASS))
			maxSetASCII = 62;
	}

	private CharClassToken(Token.Type type) {
		super(type);
	}

	@Override
	CharClassToken newInstance() {
		CharClassToken ret = new CharClassToken(this.type);
		ret.countASCII = this.countASCII;
		ret.lowASCII = this.lowASCII;
		ret.highASCII = this.highASCII;
		System.arraycopy(seenASCII, this.lowASCII, ret.seenASCII, this.lowASCII, (this.highASCII - this.lowASCII) + 1);
		ret.countNonASCII = this.countNonASCII;
		ret.seenNonASCII = new TreeSet<Character>(this.seenNonASCII);

		return ret;
	}

	@Override
	public int charactersUsed() {
		return countASCII + countNonASCII;
	}

	private Set<Character> getFullSet() {
		Set<Character> ret = new TreeSet<>(seenNonASCII);

		if (countASCII != 0)
			for (int i = lowASCII; i <= highASCII; i++)
				if (seenASCII[i])
					ret.add((char)i);

		return ret;
	}

	@Override
	public String getCharacters() {
		Set<Character> fullSet = getFullSet();
		StringBuilder b = new StringBuilder(fullSet.size());

		for (final char c : fullSet)
			b.append(c);

		return b.toString();
	}

	public class Range {
		char min;
		char max;
		Range(char ch) {
			this.min = ch;
		}

		public char getMin() {
			return min;
		}

		public char getMax() {
			return max;
		}
	}

	/**
	 * Get the set of Ranges (contiguous low to high characters) for this Character Class.
	 * For example with inputs 1, 2, 3, 6, 8, 9 - three ranges would be returned 1-3, 6-6, and 8-9.
	 * @return A set of Ranges (low-high characters) that represent all the characters in this Character Class.
	 */
	public Set<Range> getRanges() {
		Set<Range> ranges = new HashSet<>();
		Range range = null;
		char last = '¶';

		for (final char ch : getFullSet()) {
			if (range == null) {
				range = new Range(ch);
				last = ch;
			}
			else if (ch == last + 1)
				last = ch;
			else {
				range.max = last;
				ranges.add(range);
				range = new Range(ch);
				last = ch;
			}
		}
		if (range != null) {
			range.max = last;
			ranges.add(range);
		}

		return ranges;
	}

	@Override
	String getRegExp(final boolean fitted) {
		int nodeCount = children == null ? 1 : children.size();
		if (!fitted || (countASCII + countNonASCII) > nodeCount)
			return type.getRegExp() + RegExpSplitter.qualify(minObserved, maxObserved);

		List<CharClassToken> nodes = nodeCount == 1 ? Collections.singletonList(this) : children;
		StringBuilder b = new StringBuilder(nodeCount);
		for (CharClassToken token : nodes) {
			for (final char c : token.getFullSet())
				b.append(c);
		}

		return b.toString();
	}

	@Override
	public Token merge(Token o) {
		CharClassToken other = (CharClassToken)o;
		mergeObservations(other);

		this.minObserved = Math.min(this.minObserved, other.minObserved);
		this.maxObserved = Math.max(this.maxObserved, other.maxObserved);

		return this;
	}

	/**
	 * Coalesce is used to merge to adjacent tokens into one in a single TokenStream, this is in contrast to merge which is
	 * used to merge two tokens in a similar position in different TokenStreams.
	 * @param other The other Token to be coalesced.
	 * @return The coalesced token.
	 */
	public CharClassToken coalesce(CharClassToken other) {
		if (children == null) {
			children = new ArrayList<CharClassToken>();
			children.add(this.newInstance());
		}
		children.add(other);

		mergeObservations(other);

		this.minObserved += other.minObserved;
		this.maxObserved += other.maxObserved;

		return this;
	}

	private void mergeObservations(CharClassToken other) {
		// The only differing types we are prepared to merge is anything to ALPHADIGIT
		if (!type.equals(other.type) && !type.equals(Token.Type.ALPHADIGIT_CLASS)) {
			type = Token.Type.ALPHADIGIT_CLASS;
			maxSetASCII = 62;
		}

		// No need to merge the ASCII observations if we have seen the entire set or no observations from other set
		if (maxSetASCII != countASCII && other.countASCII != 0) {
			for (int i = other.lowASCII; i <= other.highASCII; i++)
				if (other.seenASCII[i] && !seenASCII[i]) {
					seenASCII[i] = true;
					countASCII++;
				}
			lowASCII = Math.min(lowASCII, other.lowASCII);
			highASCII = Math.max(highASCII, other.highASCII);
		}

		if (other.countNonASCII != 0) {
			seenNonASCII.addAll(other.seenNonASCII);
			countNonASCII =  seenNonASCII.size();
		}
	}
}
