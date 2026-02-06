/*
 * Copyright 2023 Tim Segall
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
package com.cobber.fta.core;

public class PatternFG {
	private final String[] words;
	private int minLength = Integer.MAX_VALUE;
	private int maxLength = 0;
	boolean[] candidates = new boolean[128];

	public static PatternFG compile(final String wPattern) {
		return new PatternFG(wPattern);
	}

	private PatternFG(final String wPattern) {
		final String withSentinel = wPattern + '|';

		final long wordCount = withSentinel.codePoints().filter(ch -> ch =='|').count() + 1;
		words = new String[(int)wordCount];

		int start = 0;
		int wordIndex = 0;
		for (int i = start; i < withSentinel.length(); i++) {
			if (withSentinel.charAt(i) == '|') {
				if (i != withSentinel.length() - 1) {
					final char ch = withSentinel.charAt(i + 1);
					candidates[ch] = true;
					if (Character.isLowerCase(ch))
						candidates[Character.toUpperCase(ch)] = true;
					else if (Character.isUpperCase(ch))
						candidates[Character.toLowerCase(ch)] = true;
				}
				words[wordIndex++] = withSentinel.substring(start, i);
				if (i - start < minLength)
					minLength = i - start;
				if (i - start > maxLength)
					maxLength = i - start;
				start = i + 1;
			}
		}
	}

	public boolean matcher(final String input) {
		if (input == null || input.isEmpty())
			return false;

		final char ch = input.charAt(0);
		if (ch >= candidates.length || !candidates[ch])
			return false;

		final int len = input.length();
		if (len == 0 || len < minLength || len > maxLength)
			return false;

		for (final String word : words)
			if (input.equalsIgnoreCase(word))
				return true;

		return false;
	}
}
