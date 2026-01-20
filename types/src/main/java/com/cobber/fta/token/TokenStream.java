/*
 * Copyright 2017-2025 Tim Segall
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cobber.fta.CacheLRU;
import com.cobber.fta.core.InternalErrorException;
import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.core.Utils;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.DatatypesAutomatonProvider;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;

/**
 * A TokenStream (a set of Tokens) captures the Shapes and counts of the incoming data stream.
 *
 * So with data that looks like:
 *	A09123, ICD9-90871, B00023, A12348, C89023, ICD9-90322, ICD9-44233, Z90908, Q23235
 *
 * You will end up with two TokenStream instances:
 *	X99999 - count 6
 *	XXX9-99999 - count 3
 *
 * Each TokenStream preserves information about the the incoming data so in the case of the the second Stream above (XXX9-99999)
 * the first Token X retains the fact that it has seen only the letter 'I'.  So that when asked for the associated Regular Expression
 * the knowledge exists to return ICD9-\d{4} and NOT simply \p{isAlphabetic}{3}\d-\d{4}.
 */
public class TokenStream {
	/* The set of uncompressed tokens. */
	private final Token[] tokens;
	private final String key;

	/* The set of compressed tokens - compression typically coalesces adjacent similar token, but may also do float identification. */
	private Token[] compressedTokens;
	private String compressedKey;

	/* Does this TokenStream consist entirely of Alpha characters. */
	private final boolean isAllAlpha;
	/* Does this TokenStream consist entirely of AlphaNumeric characters. */
	private final boolean isAllAlphaNumeric;
	/* Does this TokenStream consist entirely of Numeric characters. */
	private final boolean isAllNumeric;
	/* Have we created the compressed token stream. */
	private boolean isCompressed;
	/* The number of occurrences of this 'Pattern'. */
	private long occurrences;

	private static final CacheLRU<String, Automaton> cache = new CacheLRU<>(10);

	/** The TokenStream that represents any input that is too long. */
	public static final TokenStream ANYSHAPE = new TokenStream(Utils.repeat('x', Token.MAX_LENGTH + 1), 1);

	/**
	 * Construct a new TokenStream based on the input.
	 * @param trimmed The trimmed input.
	 * @param occurrences The number of occurrences of this input.
	 */
	public TokenStream(final String trimmed, final long occurrences) {
		final int len = trimmed.length();

		this.occurrences = occurrences;

		if (len > Token.MAX_LENGTH) {
			key = "ANY";
			tokens = new Token[1];
			tokens[0] = new AnyInputToken();
			isAllNumeric = isAllAlpha = isAllAlphaNumeric = false;
			return;
		}

		final StringBuilder b = new StringBuilder(trimmed);
		int alphas = 0;
		int digits = 0;

		tokens = new Token[len];

		for (int i = 0; i < len; i++) {
			final char ch = trimmed.charAt(i);
			if (Character.isAlphabetic(ch)) {
				tokens[i] = new CharClassToken(Token.Type.ALPHA_CLASS, ch);
				b.setCharAt(i, Token.Type.ALPHA_CLASS.getEncoded());
				alphas++;
			}
			else if (Character.isDigit(ch)) {
				tokens[i] = new CharClassToken(Token.Type.DIGIT_CLASS, ch);
				b.setCharAt(i, Token.Type.DIGIT_CLASS.getEncoded());
				digits++;
			}
			else
				tokens[i] = new SimpleToken(ch);
		}

		key = b.toString();

		isAllNumeric = digits == len;
		isAllAlpha = alphas == len;
		isAllAlphaNumeric = digits != 0 && alphas != 0 && digits + alphas == len;
	}

	public static String getKey(final String trimmed) {
		final int len = trimmed.length();

		if (len > Token.MAX_LENGTH)
			return "ANY";

		final StringBuilder b = new StringBuilder(trimmed);

		for (int i = 0; i < len; i++) {
			final char ch = trimmed.charAt(i);
			if (Character.isAlphabetic(ch)) {
				b.setCharAt(i, Token.Type.ALPHA_CLASS.getEncoded());
			}
			else if (Character.isDigit(ch)) {
				b.setCharAt(i, Token.Type.DIGIT_CLASS.getEncoded());
			}
		}

		return b.toString();
	}

	public boolean isComplete() {
		for (int i = 0; i < tokens.length;i++)
			if (!tokens[i].isComplete())
				return false;

		return true;
	}

	/**
	 * Construct a new TokenStream from an existing TokenStream.
	 * @param other The template for the new TokenStream.
	 */
	public TokenStream(final TokenStream other) {
		tokens = new Token[other.tokens.length];
		for (int i = 0; i < tokens.length;i++)
			tokens[i] = other.tokens[i].newCopy();

		this.isCompressed = other.isCompressed;
		if (isCompressed) {
			compressedTokens = new Token[other.compressedTokens.length];
			for (int i = 0; i < compressedTokens.length;i++)
				compressedTokens[i] = other.compressedTokens[i].newCopy();
			compressedKey = other.compressedKey;
		}

		this.key = other.key;
		this.isAllAlpha = other.isAllAlpha;
		this.isAllAlphaNumeric = other.isAllAlphaNumeric;
		this.isAllNumeric = other.isAllNumeric;
		this.occurrences = other.occurrences;
	}

	/**
	 * Is the input too long?
	 * @param trimmed The trimmed input.
	 * @return True if the input is too long.
	 */
	public static boolean tooLong(final String trimmed) {
		return trimmed.length() > Token.MAX_LENGTH;
	}

	/**
	 * Get the Regular Expression for this TokenStream.
	 * @param fitted If true the Regular Expression should be a 'more closely fitted' Regular Expression.
	 * @return The Java Regular Expression for this TokenStream.
	 */
	public String getRegExp(final boolean fitted) {
		if (!isCompressed)
			compress();

		final StringBuilder ret = new StringBuilder();

		for (final Token token : compressedTokens)
			ret.append(token.getRegExp(fitted));

		return ret.toString();
	}

	/**
	 * Is this TokenStream mergeable with the other.
	 * @param other The other TokenStream
	 * @return True if the this TokenStream can be merged with the other TokenStream.
	 */
	private boolean mergeable(final TokenStream other) {
		if (getKey().length() != other.getKey().length())
			return false;

		// If the keys are identical we can merge
		if (getKey().equals(other.getKey()))
			return true;

		for (int i = 0; i < getKey().length() ;i++) {
			final char ch1 = getKey().charAt(i);
			final char ch2 = other.getKey().charAt(i);
			// You can still merge if one TokenStream has a numeric and the other has an Alpha (the resultant stream will have an AlphaNumeric)
			if (ch1 != ch2 && !((ch1 == '9' && ch2 == 'X') || (ch1 == 'X' && ch2 == '9')))
				return false;
		}

		return true;
	}

	/**
	 * Merge the supplied TokenStream into this one - both TokenStreams must have the same uncompressed representation.
	 * @param other The other TokenStream
	 * @return The updated TokenStream or null if Streams are not mergeable.
	 */
	public TokenStream merge(final TokenStream other) {
		if (!mergeable(other))
			return null;

		this.occurrences += other.occurrences;
		for (int i = 0; i < tokens.length; i++)
			tokens[i].merge(other.tokens[i]);

		return this;
	}

	public TokenStream mergeCount(final long occurrences) {
		this.occurrences += occurrences;
		return this;
	}

	/**
	 * Simplify the Compressed TokenStream to improve the Regular Expression returned.
	 *
	 * We have something that works but it is possibly really ugly, for example, we may have:
	 *	\d{2}\p{IsAlphabetic}\d{8}[\d\p{IsAlphabetic}]{4}\p{IsAlphabetic}{2}[\d\p{IsAlphabetic}]
	 * The objective is to reduce the number of transitions to something reasonable.
	 *
	 * @return A Simplified compressed representation of the current TokenStream.
	 */
	public TokenStream simplify() {
		int transitions = 0;
		int start = -1;

		final List<Token> updated = new ArrayList<>(Arrays.asList(getCompressedTokens()));
		Token lastToken = null;

		for (int i = 0; i < updated.size(); i++) {
			final Token token = updated.get(i);
			if (lastToken == null) {
				lastToken = token;
				continue;
			}
			if (token instanceof CharClassToken) {
				if (lastToken instanceof CharClassToken) {
					if (start == -1)
						start = i - 1;
					transitions++;
				}
				else
					transitions = 0;
			}
			else {
				if (transitions >= 2)
					coalesceCharClasses(updated, start, i - 1);
				start = -1;
				transitions = 0;
			}
			lastToken = token;
		}

		if (transitions >= 2)
			coalesceCharClasses(updated, start, updated.size() - 1);

		compressedTokens = updated.toArray(new Token[0]);

		return this;
	}

	private void coalesceCharClasses(final List<Token> toFix, final int from, final int to) {
		final CharClassToken mergedToken = (CharClassToken)toFix.get(from);

		// Coalesce after from+1 ... to into from
		for (int i = from + 1; i <= to; i++)
			mergedToken.coalesce((CharClassToken)toFix.get(i));
		// Remove from to ... from+1
		for (int i = to; i > from; i--)
			toFix.remove(i);
	}

	/**
	 * Merge the supplied TokenStream into this one - both TokenStreams must have the same uncompressed representation.
	 * @param other The other TokenStream
	 * @return The updated TokenStream.
	 */
	public TokenStream mergeCompressed(final TokenStream other) {
		if (!compressedKey.equals(other.compressedKey))
			throw new InternalErrorException("Impossible merge: '" + key + "' and '" + other.key + "'");

		this.occurrences += other.occurrences;
		for (int i = 0; i < compressedTokens.length; i++) {
			compressedTokens[i].merge(other.compressedTokens[i]);
		}

		return this;
	}

	enum FloatState {
		NONE,
		SIGN,
		FIRST_COMPONENT,
		PERIOD,
		SECOND_COMPONENT
	}

	private void compress() {
		if (isCompressed)
			return;

		final List<Token> newTokens = new ArrayList<>();
		Token lastToken = null;

		// Coalesce multiple numerics or alphas into one
		for (final Token token : tokens) {
			if (lastToken == null) {
				lastToken = token.newCopy();
				newTokens.add(lastToken);
				continue;
			}

			if (token instanceof CharClassToken && lastToken instanceof CharClassToken && token.type == lastToken.type)
				((CharClassToken)lastToken).coalesce((CharClassToken)token);
			else {
				lastToken = token.newCopy();
				newTokens.add(lastToken);
			}
		}

		// Collapse floats into a Float Token
		FloatState state = FloatState.NONE;
		boolean signed = false;
		int start = -1;
		for (int i = 0; i < newTokens.size(); i++) {
			final Token token = newTokens.get(i);
			switch (state) {
			case NONE:
				if (token.type == Token.Type.SIMPLE && (token.ch == '+' || token.ch == '-')) {
					state = FloatState.SIGN;
					signed = true;
					start = i;
				}
				else if (token.type == Token.Type.DIGIT_CLASS) {
					state = FloatState.FIRST_COMPONENT;
					signed = false;
					start = i;
				}
				break;
			case SIGN:
				if (token.type == Token.Type.DIGIT_CLASS)
					state = FloatState.FIRST_COMPONENT;
				else
					state = FloatState.NONE;
				break;
			case FIRST_COMPONENT:
				if (token.type == Token.Type.SIMPLE && token.ch == '.')
					state = FloatState.PERIOD;
				else
					state = FloatState.NONE;
				break;
			case PERIOD:
				if (token.type == Token.Type.DIGIT_CLASS)
					state = FloatState.SECOND_COMPONENT;
				else
					state = FloatState.NONE;
				break;
			case SECOND_COMPONENT:
				if (token.type == Token.Type.SIMPLE && token.ch != '.') {
					replaceWithFloat(newTokens, start, i - 1, signed);
					i = start;
				}
				else {
					// Give up!
					i = newTokens.size();
				}
				state = FloatState.NONE;
				break;
			}
		}

		if (state == FloatState.SECOND_COMPONENT)
			replaceWithFloat(newTokens, start, newTokens.size() - 1, signed);

		compressedTokens = newTokens.toArray(new Token[0]);
		final StringBuilder b = new StringBuilder(key.length());
		for (final Token token : compressedTokens)
			b.append(token.getCh());
		compressedKey = b.toString();
		isCompressed = true;
	}

	private void replaceWithFloat(final List<Token> newTokens, final int start, final int end, final boolean signed) {
		for (int j = end; j >= start; j--)
			newTokens.remove(j);
		newTokens.add(start, new FloatToken(signed));
	}

	public String getKey() {
		return key;
	}

	public Token[] getTokens() {
		return tokens;
	}

	public String getCompressedKey() {
		if (!isCompressed)
			compress();
		return compressedKey;
	}

	protected Token[] getCompressedTokens() {
		if (!isCompressed)
			compress();
		return compressedTokens;
	}

	/**
	 * @return True is TokenStream is exclusively Alpha's.
	 */
	public boolean isAlpha() {
		return isAllAlpha;
	}

	/**
	 * @return True is TokenStream is a mix of Alpha's and Numeric's.
	 */
	public boolean isAlphaNumeric() {
		return isAllAlphaNumeric;
	}

	/**
	 * @return True is TokenStream is exclusively Numeric's.
	 */
	public boolean isNumeric() {
		return isAllNumeric;
	}

	/**
	 * @return The number of inputs this TokenStream has captured.
	 */
	public long getOccurrences() {
		return occurrences;
	}

	/**
	 * Check if this TokenStream matches the supplied Regular Expression.
	 * We use the Automaton to do all the heavy lifting.
	 * @param regExp The Regular Expression to match.
	 * @return True if the TokenStream matches the supplied Regular Expression.
	 */
	public boolean matches(final String regExp) {
		Automaton automaton = cache.get(regExp);
		if (automaton == null) {
			automaton = new RegExp(RegExpGenerator.toAutomatonRE(regExp, false), RegExp.AUTOMATON).toAutomaton(new DatatypesAutomatonProvider());
			cache.put(regExp, automaton);
		}

		return matches(automaton.getInitialState(), 0);
	}

	/**
	 * Recursive solution to determining if the current TokenStream matches the Regular Expression.
	 * @param state The current Automaton state.
	 * @param tokenIndex The current index into the token (uncompressed) array.
	 * @return True if we have a match.
	 */
	private boolean matches(final State state, final int tokenIndex) {
		if (tokenIndex == tokens.length)
			return state.isAccept();

		// Get a list of transitions ordered by (min, reverse max, to)
		final List<Transition> transitions = state.getSortedTransitions(false);
		final Token token = tokens[tokenIndex];

		switch (token.type) {
		case SIMPLE:
			if (transitions.isEmpty())
				return false;
			final char ch = token.getCh();
			for (final Transition transition : transitions)
				if (ch >= transition.getMin() && ch <= transition.getMax()) {
					return matches(transition.getDest(), tokenIndex + 1);
				}
			return false;

		case ALPHA_CLASS:
		case DIGIT_CLASS:
		case ALPHADIGIT_CLASS:
			if (transitions.isEmpty())
				return false;

			// We need to make sure that every range has a successful transition, there is no more
			// than one applicable transition per range.
			return checkRanges(((CharClassToken)token).getRanges(), transitions, tokenIndex, 0);

		default:
			// Should never happen since FLOAT is not in an uncompressed form and we should not be invoked with ANY
			return false;

		}
	}
	private boolean checkRanges(final Set<Range> ranges, final List<Transition> transitions, final int tokenIndex, final int level) {
		Boolean satisfied = null;
		Set<Range> splits;
		final Set<Integer> done = new HashSet<>();

		// We have a set of ranges and we need to make sure they are covered by transitions for this Regular Expression
		// There are two cases:
		//  - The range in question is covered by the transition (e.g. we observed the digits 1-3, and the transition covers 0-9)
		//  - The range in question is partly covered by a transition (e.g we observed 1-2, and hopefully there are separate transitions that
		// cover this range for example 1-1, 2-9).
		// In the latter case (partial coverage) - we split the range and then validate that the split ranges are both covered
		for (final Range range : ranges) {
			satisfied = null;
			nextRange: for (final Transition transition : transitions) {
				if (isCoveredBy(range, transition)) {
					if (satisfied == null)
						satisfied = true;
					if (done.contains(transition.hashCode())) {
						continue nextRange;
					}
					if (!matches(transition.getDest(), tokenIndex + 1))
						return false;
					done.add(transition.hashCode());
				} else if ((splits = isPartiallyCoveredBy(range, transition)) != null) {
					for (final Range splitRange : splits)
						if (!checkRanges(Collections.singleton(splitRange), transitions, tokenIndex, level + 1))
							return false;
					if (satisfied == null)
						satisfied = true;
				}
			}
			// We need to see one successful transition for every range
			if (satisfied == null)
				return false;
		}
		 return satisfied != null && satisfied;
	}

	/**
	 * Is this Range completely covered by this Transition.
	 * @param range The Range to test
	 * @param transition The Transition to test
	 * @return
	 */
	private static boolean isCoveredBy(final Range range, final Transition transition) {
		return transition.getMin() <= range.getMin() && range.getMax() <= transition.getMax();
	}

	/* Is this Range partially covered by this Transition.
	 * Note: Assumes isCovered() is false
	 *
	 * @param range The Range to test
	 * @param transition The Transition to test
	 * @return A Set with the input range split into two (one covered by this transition and one not)
	 */
	private static Set<Range> isPartiallyCoveredBy(final Range range, final Transition transition) {
		final Set<Range> ret = new HashSet<>();
		///  R             -----------------------
		///  T       ----------------------
		if (transition.getMin() < range.getMin()) {
			if (transition.getMax() < range.getMax() && transition.getMax() >= range.getMin()) {
				ret.add(new Range(range.getMin(), transition.getMax()));
				ret.add(new Range((char)(transition.getMax() + 1), range.getMax()));
				return ret;
			}
			return null;
		}

		///  R       -----------------------
		///  T       --------------------
		if (transition.getMin() == range.getMin()) {
			if (transition.getMax() < range.getMax()) {
				ret.add(new Range(range.getMin(), transition.getMax()));
				ret.add(new Range((char)(transition.getMax() + 1), range.getMax()));
				return ret;
			}
			return null;
		}

		///  R       ----------------------- (1-2)
		///  T             ---------------------- (2-9)
		// transition.getMin() > range.getMin()
		if (transition.getMin() <= range.getMax()) {
			ret.add(new Range(range.getMin(), (char) (transition.getMin() - 1)));
			ret.add(new Range(transition.getMin(), range.getMax()));
			return ret;
		}

		return null;
	}
}
