/*
 * Copyright 2017-2022 Tim Segall
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
package com.cobber.fta.text;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Core class for processing free form text.
 */
public class TextProcessor {
	private TextConfig config;
	private static Map<String, TextConfig> allConfigData = new HashMap<>();

	public TextProcessor(Locale locale) {
		if (locale == null)
			locale = Locale.getDefault();
		this.config = allConfigData.get(locale.getLanguage().toUpperCase(Locale.ROOT));
	}

	public enum Determination {
		/** Input looks like free text. */
		OK,
		/** Average length of words does not look reasonable. */
		BAD_AVERAGE_LENGTH,
		/** Detected word that is unreasonably long. */
		TOO_LONG,
		/** The percentage of alphas, wordBreaks, punctuation(,.) and any digits in digit only words is too low. */
		PERCENT_TOO_LOW,
		/** Sentence is too short. */
		SENTENCE_TOO_SHORT,
		/** Input is too short. */
		TOO_SHORT,
		/** Input just has a single word. */
		SINGLE_WORD
	}

	public class TextResult {
		private Determination determination;
		private int alphas;
		private int digits;
		private int words;
		private int sentenceBreaks;
		private int wordBreaks;
		private int punctuation;
		private int spaces;

		public TextResult() {
			determination = Determination.OK;
		}

		public int getAlphas() {
			return alphas;
		}

		public int getDigits() {
			return digits;
		}

		public int getWords() {
			return words;
		}

		public int getSentenceBreaks() {
			return sentenceBreaks;
		}

		public int getWordBreaks() {
			return wordBreaks;
		}

		public int getPunctuation() {
			return punctuation;
		}

		public int getSpaces() {
			return spaces;
		}

		public Determination getDetermination() {
			return determination;
		}

	}

	static {
		// English configuration
		allConfigData.put("EN", new TextConfig(
				20,				// antidisestablishmentarianism is 28 (there are longer), so we choose something that is reasonable
				3.0, 9.0,		// Average word length in English is ~5, so choose a reasonable lower and upper bound
				30,				// The percentage of 'alpha' characters that we expect to be present
				80,				// The percentage of 'reasonable' characters that we expect to be present
				120,			// Only analyze the first <n> characters
				".!?",			// Sentence Break characters
				", /;:-.!?",	// Word Break characters
				",\"'-();:.!?"	// Punctuation character
				));
	}

	public TextResult analyze(final String trimmed) {
		final TextResult ret = new TextResult();
		final int len = trimmed.length();
		int digitsInWord = 0;
		int lastOffset = 0;
		char lastCh = ' ';
		int totalWordLength = 0;
		boolean wordStarted = false;
		int wordsInSentence = 0;
		int lastWordLength = 0;

		int idx;
		for (idx = 0; idx < len && !(idx >= config.getMaxLength() && Character.isWhitespace(lastCh)); idx++) {
			final char ch = trimmed.charAt(idx);

			// Reached the end of a 'word'
			if (wordStarted && config.getWordBreak().indexOf(ch) != -1 && config.getWordBreak().indexOf(lastCh) == -1) {
				// Reject if it looks too long to be a word
				if (idx - lastOffset > config.getLongWord()) {
					ret.determination = Determination.TOO_LONG;
					return ret;
				}

				lastWordLength = idx - lastOffset;
				totalWordLength += lastWordLength;
				ret.words++;
				wordsInSentence++;
				ret.wordBreaks++;
				lastOffset = idx;
				wordStarted = false;
				// If all the characters in the word are digits then call it a word
				if (digitsInWord == lastWordLength && lastWordLength <= 4)
					ret.digits += digitsInWord;
				digitsInWord = 0;
			}

			if (config.getWordBreak().indexOf(lastCh) != -1)
				lastOffset = idx;

			if (config.getSentenceBreak().indexOf(ch) != -1) {
				// We don't like short sentences on the other hand e.g., i.e., et al., etc. should not be rejected
				if (wordsInSentence == 1 && lastWordLength > 3) {
					ret.determination = Determination.SENTENCE_TOO_SHORT;
					return ret;
				}
				wordsInSentence = 0;
			}

			if (Character.isWhitespace(ch))
				ret.spaces++;
			else if (Character.isAlphabetic(ch)) {
				ret.alphas++;
				wordStarted = true;
			}
			else if (Character.isDigit(ch)) {
				digitsInWord++;
				wordStarted = true;
			}
			else if (config.getPunctuation().indexOf(ch) != -1)
				ret.punctuation++;
			else if (config.getWordBreak().indexOf(ch) != -1 && config.getWordBreak().indexOf(lastCh) != -1)
				ret.wordBreaks++;
			lastCh = ch;
		}
		// Reject if it looks unlikely to be a word
		if (idx - lastOffset > config.getLongWord()) {
			ret.determination = Determination.TOO_LONG;
			return ret;
		}

		if (wordStarted) {
			totalWordLength += idx - lastOffset;
			ret.words++;
		}

		// Only one word so reject
		if (ret.words == 1) {
			ret.determination = Determination.SINGLE_WORD;
			return ret;
		}

		if ((len < 10 && ret.wordBreaks == 0) || len < 6) {
			ret.determination = Determination.TOO_SHORT;
			return ret;
		}

		// Count the alphas, wordBreaks, punctuation(,.) and any digits in digit only words
		if ((ret.alphas + ret.digits + ret.wordBreaks + ret.spaces + ret.punctuation)*100/idx < config.getSimplePercentage()) {
			ret.determination = Determination.PERCENT_TOO_LOW;
			return ret;
		}

		if (ret.alphas * 100/idx < config.getAlphaPercentage()) {
			ret.determination = Determination.PERCENT_TOO_LOW;
			return ret;
		}

		// Calculate the average word length
		double avgWordLength = (double)totalWordLength/ret.words;

		if ((len > 10 && avgWordLength < config.getAverageLow()) || avgWordLength > config.getAverageHigh())
			ret.determination = Determination.BAD_AVERAGE_LENGTH;

		return ret;
	}
}
