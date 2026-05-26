/*
 * Copyright 2017-2026 Tim Segall
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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.cobber.fta.core.PatternFG;

public class TestPatternFG {

	/*
	 * PatternFG candidates[] is populated from the first character of each word that *follows* a '|'.
	 * Consequence: the very first word in the pattern is only matchable via the fast-reject check when
	 * some later word starts with the same letter (making that first char a candidate).
	 * When all words begin with the same letter the first word is always reachable.
	 */

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void matcherAllSameLetter() {
		// All three words start with 'a'; the candidate 'a'/'A' is set by "avocado" and "apricot"
		final PatternFG pf = PatternFG.compile("apple|avocado|apricot");
		assertTrue(pf.matcher("apple"));
		assertTrue(pf.matcher("avocado"));
		assertTrue(pf.matcher("apricot"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void matcherCaseInsensitive() {
		final PatternFG pf = PatternFG.compile("apple|avocado|apricot");
		assertTrue(pf.matcher("APPLE"));
		assertTrue(pf.matcher("Apple"));
		assertTrue(pf.matcher("AVOCADO"));
		assertTrue(pf.matcher("APRICOT"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void matcherFirstWordNotDetectableWhenUniqueFirstChar() {
		// 'a' (first char of "apple") is NOT added as a candidate because no subsequent word starts with 'a'
		final PatternFG pf = PatternFG.compile("apple|banana|cherry");
		assertFalse(pf.matcher("apple"));    // fast-rejected: 'a' is not a candidate
		assertFalse(pf.matcher("APPLE"));
		assertTrue(pf.matcher("banana"));    // 'b' is a candidate
		assertTrue(pf.matcher("cherry"));    // 'c' is a candidate
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void matcherNotInSet() {
		final PatternFG pf = PatternFG.compile("apple|avocado|apricot");
		assertFalse(pf.matcher("grape"));
		// "almond" starts with 'a' (a candidate) and has valid length, but is not in the word list
		assertFalse(pf.matcher("almond"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void matcherNull() {
		final PatternFG pf = PatternFG.compile("apple|avocado");
		assertFalse(pf.matcher(null));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void matcherEmpty() {
		final PatternFG pf = PatternFG.compile("apple|avocado");
		assertFalse(pf.matcher(""));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void matcherTooShort() {
		// "banana" and "cherry" both have length 6; "che" (3 chars) is below minLength
		final PatternFG pf = PatternFG.compile("banana|cherry");
		assertFalse(pf.matcher("che"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void matcherTooLong() {
		// max length here is 6; "bananaberry" (11 chars) exceeds maxLength
		final PatternFG pf = PatternFG.compile("banana|cherry");
		assertFalse(pf.matcher("bananaberry"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void matcherWrongFirstChar() {
		// "apple" starts with 'a'/'A'; "maple" starts with 'm', which is never a candidate
		final PatternFG pf = PatternFG.compile("apple|avocado");
		assertFalse(pf.matcher("maple"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void singleWordNoMatchesEver() {
		// A single-word pattern sets no candidates (the final '|' is the sentinel and skipped),
		// so the fast-reject always fires and nothing matches.
		final PatternFG pf = PatternFG.compile("yes");
		assertFalse(pf.matcher("yes"));
		assertFalse(pf.matcher("YES"));
		assertFalse(pf.matcher("no"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void realWorldNoDataPattern() {
		// The actual NO_DATA keyword pattern; "null" starts with 'n', and "no data" also starts
		// with 'n' — so 'n'/'N' is added as a candidate when processing the '|' after "null".
		final PatternFG pf = PatternFG.compile("null|no data|sin dato");
		assertTrue(pf.matcher("null"));
		assertTrue(pf.matcher("NULL"));
		assertTrue(pf.matcher("no data"));
		assertTrue(pf.matcher("sin dato"));
		assertFalse(pf.matcher("n/a"));     // not in list
	}
}
