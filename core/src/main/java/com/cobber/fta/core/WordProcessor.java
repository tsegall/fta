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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Split an input string into words.
 *  - Break chars are used to split words and are not returned.
 *  - AlphaNumeric transition (if true then this is treated as a word break)
 */
public class WordProcessor {
	private final StringBuilder defaultBreakChars = new StringBuilder(" \u00A0\n\t,/-_;!.|&");
	private StringBuilder breakChars;
	private String killChars = "\"()[]";
	private String breakCharsString;
	private String additionalWordChars = null;
	private boolean alphaNumberTransition = false;
	private boolean lastAlpha = false;
	private boolean lastNumeric = false;
	private boolean lastSimpleWordChar = false;

	public WordProcessor() {
		breakChars = defaultBreakChars;
		breakCharsString = breakChars.toString();
	}

	/**
	 * Modify the WordProcessor() appending a set of additional characters to the valid word characters.
	 * @param additionalWordChars The set of additional characters to treat as part of the word.
	 * @return The WordProcessor
	 */
	public WordProcessor withAdditionalWordChars(final String additionalWordChars) {
		this.additionalWordChars = additionalWordChars;
		// If it is a Word character it cannot also be a Break character
		for (int i = 0; i < additionalWordChars.length(); i++) {
			final char ch = additionalWordChars.charAt(i);
			final int breakIndex = breakCharsString.indexOf(ch);
			if (breakIndex != -1) {
				breakChars.deleteCharAt(breakIndex);
				breakCharsString = breakChars.toString();
			}
		}
		return this;
	}

	/**
	 * Modify the WordProcessor() setting the set of characters to use as break characters.
	 * @param breakChars The set of characters to treat as break characters.
	 * @return The WordProcessor
	 */
	public WordProcessor withBreakChars(final String breakChars) {
		this.breakChars = new StringBuilder(breakChars);
		return this;
	}

	/**
	 * Modify the WordProcessor() appending a set of additional characters to the valid break characters.
	 * @param additionalBreakChars The set of additional characters to treat as break characters.
	 * @return The WordProcessor
	 */
	public WordProcessor withAdditionalBreakChars(final String additionalBreakChars) {
		breakChars.append(additionalBreakChars);
		return this;
	}

	public WordProcessor withAdditionalKillChars(final String additionalKillChars) {
		killChars += additionalKillChars;
		return this;
	}

	public WordProcessor withAlphaNumberTransition(final boolean alphaNumberTransition) {
		this.alphaNumberTransition = alphaNumberTransition;
		return this;
	}

	/**
	 * Split the input String into 'words' based on the break characters.
	 * @param input String to break into words.
	 * @return A list of words based on the input string.
	 */
	public List<String> asWords(final String input) {
		return asWordOffsets(input).stream().map(x -> x.word).collect(Collectors.toList());
	}

	private boolean isSimpleWordChar(final char ch) {
		return Character.isAlphabetic(ch) || Character.isDigit(ch) || (additionalWordChars != null && additionalWordChars.indexOf(ch) != -1);
	}

	private void append(final StringBuilder b, final char ch) {
		b.append(ch);
		lastAlpha = Character.isAlphabetic(ch);
		lastNumeric = Character.isDigit(ch);
		lastSimpleWordChar = isSimpleWordChar(ch);
	}

	/**
	 * Split the input String into 'words' based on the break characters.
	 * @param input String to break into words.
	 * @return A list of words based on the input string.
	 */
	public List<WordOffset> asWordOffsets(final String input) {
		final List<WordOffset> ret = new ArrayList<>();
		boolean midWord = false;
		int start = -1;
		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < input.length(); i++) {
			final char ch = input.charAt(i);
			if (killChars.indexOf(ch) != -1)
				continue;
			if (!midWord) {
				if (breakCharsString.indexOf(ch) != -1)
					continue;
				midWord = true;
				append(b, ch);
				start = i;
				continue;
			}

			// If we have <DIGIT>.<DIGIT> then it does not count as a break!
			if (ch == '.' && lastNumeric && i + 1 < input.length() && Character.isDigit(input.charAt(i + 1))) {
				append(b, ch);
				continue;
			}

			// At this point we are in the middle of a word
			final boolean isBreak = breakCharsString.indexOf(ch) != -1;
			if (isBreak ||
					(alphaNumberTransition && (lastAlpha && Character.isDigit(ch) || lastNumeric && Character.isAlphabetic(ch))) ||
					lastSimpleWordChar ^ isSimpleWordChar(ch)) {
				// End of word reached
				ret.add(new WordOffset(b.toString(), start));
				b.setLength(0);
				midWord = !isBreak;
				// If it was a valid word character as opposed to a break character that caused us to detect the end of the
				// previous word then start a new word with this character.
				if (midWord) {
					append(b, ch);
					start = i;
				}
				continue;
			}

			append(b, ch);
		}

		if (start != -1 && b.length() != 0)
			ret.add(new WordOffset(b.toString(), start));

		return ret;
	}
}
