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

public class WordProcessor {
	private final String defaultBreakChars = " \u00A0\n\t,/-;";
	private String breakChars = null;
	private String additionalWordChars = null;

	public WordProcessor() {
		breakChars = defaultBreakChars;
	}

	public WordProcessor(final String additionalWordChars) {
		breakChars = defaultBreakChars;
		this.additionalWordChars = additionalWordChars;
	}

	public WordProcessor withAdditionalWordChars(final String additionalWordChars) {
		this.additionalWordChars = additionalWordChars;
		return this;
	}

	public WordProcessor withAdditionalBreakChars(final String additionalBreakChars) {
		breakChars += additionalBreakChars;
		return this;
	}

	/**
	 * Split the input String into 'words' based on the break characters.  By default anything that is
	 * not an alpha or numeric is not returned as part of the word array.  Additional characters can be included
	 * in the words by adding them to the additionalWordCards.
	 * @param input String to break into words.
	 * @return A list of words based on the input string.
	 */
	public List<String> asWords(final String input) {
		return asWordOffsets(input).stream().map(x -> x.word).collect(Collectors.toList());
	}

	/**
	 * Split the input String into 'words' based on the break characters.  By default anything that is
	 * not an alpha or numeric is not returned as part of the word array.  Additional characters can be included
	 * in the words by adding them to the additionalWordCards.
	 * @param input String to break into words.
	 * @return A list of words based on the input string.
	 */
	public List<WordOffset> asWordOffsets(final String input) {
		final ArrayList<WordOffset> ret = new ArrayList<>();

		int start = -1;
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < input.length(); i++) {
			char ch = input.charAt(i);
			if (breakChars.indexOf(ch) != -1 && (additionalWordChars == null || additionalWordChars.indexOf(ch) == -1)) {
				if (start != -1) {
					ret.add(new WordOffset(b.toString(), start));
					b.setLength(0);
					start = -1;
				}
			}
			else if (Character.isAlphabetic(ch) || Character.isDigit(ch) ||
					(additionalWordChars != null && additionalWordChars.indexOf(ch) != -1)) {
				if (start == -1)
					start = i;
				b.append(ch);
			}
		}

		if (start != -1)
			ret.add(new WordOffset(b.toString(), start));

		return ret;
	}
}