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

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.cobber.fta.TestGroups;

public class TestTokens {
	@Test(groups = { TestGroups.ALL })
	public void testCharClassAlpha() {
		final CharClassToken a = new CharClassToken(Token.Type.ALPHA_CLASS, 'a');
		assertEquals(a.charactersUsed(), 1);
		assertEquals(a.getCh(), 'X');
		assertEquals(a.getType(), Token.Type.ALPHA_CLASS);
		assertEquals(a.getRegExp(false), "\\p{IsAlphabetic}");
		assertEquals(a.getRegExp(true), "a");
		assertEquals(a.getRanges().size(), 1);

		final CharClassToken aClone = a.newCopy();
		assertEquals(a.getType(), aClone.getType());
		assertEquals(a.getRegExp(true), aClone.getRegExp(true));

		final CharClassToken b = new CharClassToken(Token.Type.ALPHA_CLASS, 'b');
		assertEquals(b.charactersUsed(), 1);
		assertEquals(b.getType(), Token.Type.ALPHA_CLASS);
		assertEquals(b.getRegExp(false), "\\p{IsAlphabetic}");
		assertEquals(b.getRegExp(true), "b");

		a.merge(b);
		assertEquals(a.charactersUsed(), 2);
		assertEquals(a.getType(), Token.Type.ALPHA_CLASS);
		assertEquals(a.getRegExp(false), "\\p{IsAlphabetic}");
		assertEquals(a.getRanges().size(), 1);
//		assertEquals(a.getRegExp(true), "[ab]");
	}

	@Test(groups = { TestGroups.ALL })
	public void testCharClassDigit() {
		final CharClassToken one = new CharClassToken(Token.Type.DIGIT_CLASS, '1');
		assertEquals(one.charactersUsed(), 1);
		assertEquals(one.getCh(), '9');
		assertEquals(one.getType(), Token.Type.DIGIT_CLASS);
		assertEquals(one.getRegExp(false), "\\d");
		assertEquals(one.getRegExp(true), "1");
		assertEquals(one.getRanges().size(), 1);

		final CharClassToken two = new CharClassToken(Token.Type.DIGIT_CLASS, '2');
		assertEquals(two.charactersUsed(), 1);
		assertEquals(two.getType(), Token.Type.DIGIT_CLASS);
		assertEquals(two.getRegExp(false), "\\d");
		assertEquals(two.getRegExp(true), "2");

		one.merge(two);
		assertEquals(one.charactersUsed(), 2);
		assertEquals(one.getType(), Token.Type.DIGIT_CLASS);
		assertEquals(one.getRegExp(false), "\\d");
		assertEquals(one.getRegExp(true), "[1-2]");
		assertEquals(one.getRanges().size(), 1);

		one.merge(new CharClassToken(Token.Type.DIGIT_CLASS, '3'));
		assertEquals(one.charactersUsed(), 3);
		assertEquals(one.getType(), Token.Type.DIGIT_CLASS);
		assertEquals(one.getRegExp(false), "\\d");
		assertEquals(one.getRegExp(true), "[1-3]");
		assertEquals(one.getRanges().size(), 1);

		one.merge(new CharClassToken(Token.Type.DIGIT_CLASS, '6'));
		one.merge(new CharClassToken(Token.Type.DIGIT_CLASS, '7'));
		one.merge(new CharClassToken(Token.Type.DIGIT_CLASS, '8'));
		assertEquals(one.charactersUsed(), 6);
		assertEquals(one.getType(), Token.Type.DIGIT_CLASS);
		assertEquals(one.getRegExp(false), "\\d");
		assertEquals(one.getRegExp(true), "[1-36-8]");
		assertEquals(one.getRanges().size(), 2);
	}

	@Test(groups = { TestGroups.ALL })
	public void testCharClassAlphaDigit() {
		final CharClassToken one = new CharClassToken(Token.Type.ALPHADIGIT_CLASS, '1');
		assertEquals(one.charactersUsed(), 1);
		assertEquals(one.getCh(), 'A');
		assertEquals(one.getType(), Token.Type.ALPHADIGIT_CLASS);
		assertEquals(one.getRegExp(false), "[\\p{IsAlphabetic}\\d]");
		assertEquals(one.getRegExp(true), "1");

		final CharClassToken two = new CharClassToken(Token.Type.ALPHADIGIT_CLASS, '2');
		assertEquals(two.charactersUsed(), 1);
		assertEquals(two.getType(), Token.Type.ALPHADIGIT_CLASS);
		assertEquals(two.getRegExp(false), "[\\p{IsAlphabetic}\\d]");
		assertEquals(two.getRegExp(true), "2");

		one.merge(two);
		assertEquals(one.charactersUsed(), 2);
		assertEquals(one.getType(), Token.Type.ALPHADIGIT_CLASS);
		assertEquals(one.getRegExp(false), "[\\p{IsAlphabetic}\\d]");
//		assertEquals(one.getRegExp(true), "[1-2]");

		one.merge(new CharClassToken(Token.Type.ALPHADIGIT_CLASS, 'a'));
		assertEquals(one.charactersUsed(), 3);
		assertEquals(one.getType(), Token.Type.ALPHADIGIT_CLASS);
		assertEquals(one.getRegExp(false), "[\\p{IsAlphabetic}\\d]");
//		assertEquals(one.getRegExp(true), "[1-3]");

		one.merge(new CharClassToken(Token.Type.ALPHADIGIT_CLASS, '6'));
		one.merge(new CharClassToken(Token.Type.ALPHADIGIT_CLASS, '7'));
		one.merge(new CharClassToken(Token.Type.ALPHADIGIT_CLASS, '8'));
		assertEquals(one.charactersUsed(), 6);
		assertEquals(one.getType(), Token.Type.ALPHADIGIT_CLASS);
		assertEquals(one.getRegExp(false), "[\\p{IsAlphabetic}\\d]");
//		assertEquals(one.getRegExp(true), "[1-36-8]");
	}

	@Test(groups = { TestGroups.ALL })
	public void testMergeNonAlphaDigit() {
		final CharClassToken a = new CharClassToken(Token.Type.ALPHA_CLASS, 'a');
		assertEquals(a.getType(), Token.Type.ALPHA_CLASS);

		final CharClassToken one = new CharClassToken(Token.Type.DIGIT_CLASS, '1');
		assertEquals(one.getType(), Token.Type.DIGIT_CLASS);

		a.merge(one);
		assertEquals(a.getType(), Token.Type.ALPHADIGIT_CLASS);
		assertEquals(a.getRegExp(false), "[\\p{IsAlphabetic}\\d]");
	}

	@Test(groups = { TestGroups.ALL })
	public void testMergeAlphaDigit() {
		final CharClassToken a = new CharClassToken(Token.Type.ALPHADIGIT_CLASS, 'a');
		assertEquals(a.getType(), Token.Type.ALPHADIGIT_CLASS);

		final CharClassToken one = new CharClassToken(Token.Type.DIGIT_CLASS, '1');
		assertEquals(one.getType(), Token.Type.DIGIT_CLASS);

		a.merge(one);
		assertEquals(a.getType(), Token.Type.ALPHADIGIT_CLASS);
		assertEquals(a.getRegExp(false), "[\\p{IsAlphabetic}\\d]");
	}


	@Test(groups = { TestGroups.ALL })
	public void testNonASCII() {
		final CharClassToken a = new CharClassToken(Token.Type.ALPHA_CLASS, 'a');
		assertEquals(a.getType(), Token.Type.ALPHA_CLASS);

		final CharClassToken å = new CharClassToken(Token.Type.ALPHA_CLASS, 'å');
		assertEquals(å.getType(), Token.Type.ALPHA_CLASS);

		a.merge(å);
		assertEquals(a.getType(), Token.Type.ALPHA_CLASS);
		assertEquals(a.getRegExp(false), "\\p{IsAlphabetic}");
	}

}
