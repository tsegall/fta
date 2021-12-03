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

/**
 * A Token is the basic building block for a TokenStream.  There are a set of different Tokens to capture the key elements of
 * Regular Expressions.
 */
public abstract class Token {
	protected Type type;
	protected char ch;
	protected int minObserved = 1;
	protected int maxObserved = 1;

	public enum Type {
		/** Simple Character. */
		SIMPLE('S', "."),
		/** Digit Character Class. */
		DIGIT_CLASS('9', "\\d"),
		/** Alpha Character Class. */
		ALPHA_CLASS('X', "\\p{IsAlphabetic}"),
		/** AlphaNumeric Character Class. */
		ALPHADIGIT_CLASS('A', "[\\p{IsAlphabetic}\\d]"),
		/** Signed Float - only in Compressed form. */
		SIGNED_FLOAT('F', "[+-]?\\d+\\.\\d+"),
		/** Unsigned Float - only in Compressed form. */
		UNSIGNED_FLOAT('G', "\\d+\\.\\d+"),
		/** Wildcard - basically we have no idea :-) */
		ANY_INPUT('W', ".+");

		/** Each input character is encoded as one of the above possible tokens. */
		private char encoded;
		/** The RegExp that represents this token. */
		private String regExp;

		public char getEncoded() {
			return encoded;
		}

		public String getRegExp() {
			return regExp;
		}

		Type(final char encoded, final String regExp) {
			this.encoded = encoded;
			this.regExp = regExp;
		}
	}

	Token(Type type) {
		this.type = type;
		this.ch = type.encoded;
	}

	Token(Type type, char ch) {
		this.type = type;
		this.ch = ch;
	}

	abstract Token merge(Token o);
	abstract int charactersUsed();
	abstract String getCharacters();
	abstract Token newInstance();

	/**
	 * Get the Regular Expression for this Token.
	 * @param fitted If true the Regular Expression should be a 'more closely fitted' Regular Expression.
	 * @return The Java Regular Expression for this Token.
	 */
	String getRegExp(final boolean fitted) {
		return type.getRegExp();
	}

	/**
	 * Get the Token Type.
	 * @return The Token Type.
	 */
	public Type getType() {
		return type;
	}

	public char getCh() {
		return ch;
	}
}
