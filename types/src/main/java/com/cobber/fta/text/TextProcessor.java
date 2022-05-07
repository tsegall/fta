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
	private Locale locale;
	private static Map<String, TextConfig> allConfigData = new HashMap<>();
	private int lastOffset;
	private int totalAlphaWordLength;

	/** Number of words in this sentence. */
	private int wordsInSentence;
	/** Number of words consisting of only alphas. */
	private int alphaWords;
	/** Number of alphaWords of length at least 3. */
	private int longWords;
	/** Number of longWords with a plausible stem (defined by TextConfig.getStarts(). */
	private int realWords;

	public enum Determination {
		/** Input looks like free text. */
		OK,
		/** Average length of words does not look reasonable. */
		BAD_AVERAGE_LENGTH,
		/** We need some real looking words. */
		NOT_ENOUGH_REAL_WORDS,
		/** The percentage of alphas, wordBreaks, punctuation(,.) and any digits in digit only words is too low. */
		PERCENT_TOO_LOW,
		/** Sentence is too short. */
		SENTENCE_TOO_SHORT,
		/** Input just has a single word. */
		SINGLE_WORD,
		/** Detected word that is unreasonably long. */
		TOO_LONG
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

		/**
		 * Return the number of alpha (isAlphabetic()) characters detected.
		 * Note: This may not be complete if the length of the input is greater than the analysis maximum.
		 * @return The number of alpha characters detected.
		 */
		public int getAlphas() {
			return alphas;
		}

		/**
		 * Return the number of digit (isDigit()) characters detected in digit-only words.
		 * Note: This may not be complete if the length of the input is greater than the analysis maximum.
		 * @return The number of digit characters detected.
		 */
		public int getDigits() {
			return digits;
		}

		/**
		 * Return the number of words detected.
		 * Note: This may not be complete if the length of the input is greater than the analysis maximum.
		 * @return The number of words detected.
		 */
		public int getWords() {
			return words;
		}

		/**
		 * Return the number of sentence break characters detected.
		 * Note: This may not be complete if the length of the input is greater than the analysis maximum.
		 * @return The number of sentence break characters detected.
		 */
		public int getSentenceBreaks() {
			return sentenceBreaks;
		}

		/**
		 * Return the number of word break characters detected.
		 * Note: This may not be complete if the length of the input is greater than the analysis maximum.
		 * @return The number of word break characters detected.
		 */
		public int getWordBreaks() {
			return wordBreaks;
		}

		/**
		 * Return the number of punctuation characters detected.
		 * Note: This may not be complete if the length of the input is greater than the analysis maximum.
		 * @return The number of punctuation characters detected.
		 */
		public int getPunctuation() {
			return punctuation;
		}

		/**
		 * Return the number of space characters (isWhiteSpace()) detected.
		 * Note: This may not be complete if the length of the input is greater than the analysis maximum.
		 * @return The number of space characters detected.
		 */
		public int getSpaces() {
			return spaces;
		}

		public Determination getDetermination() {
			return determination;
		}
	}

	private class WordState {
		int charsInWord = 0;
		int digitsInWord = 0;
		int lastWordLength = 0;
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
				", /();:.!?",	// Word Break characters
				",\"'-();:.!?",	// Punctuation character
				new String[] {
					"ab", "ac", "ad", "af", "ag", "ah", "ai", "al", "am", "an", "ap", "ar", "as", "at", "au", "av", "aw",
					"ba", "be", "bi", "bl", "bo", "br", "bu", "ca",
					"ce", "ch", "ci", "cl", "co", "cr", "cu", "cy",
					"da", "de", "di", "do", "dr", "du",
					"ea", "ec", "ed", "ef", "ei", "el", "em", "en", "ep", "eq", "er", "es", "et", "ev", "ex",
					"fa", "fe", "fi", "fl", "fo", "fr", "fu",
					"ga", "ge", "gh", "gi", "gl", "go", "gr", "gu",
					"ha", "he", "hi", "ho", "hu", "hy",
					"id", "ig", "il", "im", "in", "ir", "is", "it",
					"ja", "jo", "ju",
					"ke", "ki", "kn",
					"la", "le", "li", "lo", "lu",
					"ma", "me", "mi", "mo", "mu", "my",
					"na", "ne", "ni", "no", "nu",
					"ob", "oc", "od", "of", "ok", "on", "op", "or", "ot", "ou", "ov", "ow",
					"pa", "pe", "ph", "pi", "pl", "po", "pr", "ps", "pu",
					"qu", "ra",
					"re", "rh", "ri", "ro", "ru",
					"sa", "sc", "se", "sh", "si", "sk", "sl", "sm", "sn", "so", "sp", "sq", "st", "su", "sw", "sy",
					"ta", "te", "th", "ti", "to", "tr", "tu", "tw", "ty",
					"ug", "ul", "un", "up", "ur", "us", "ut",
					"va", "ve", "vi", "vo", "vu",
					"wa", "we", "wh", "wi", "wo", "wr",
					"ya", "ye", "yi", "yo",
					"xx",						// xx is really a no-no but it seems to be commonly used to redact
					"zo" }
				));
	}

	public TextProcessor(final Locale locale) {
		this.locale = locale;
		if (this.locale == null)
			this.locale = Locale.getDefault();
		this.config = allConfigData.get(this.locale.getLanguage().toUpperCase(Locale.ROOT));

		// If we don't support the requested locale - just default to English
		if (this.config == null)
			this.config = allConfigData.get("EN");
	}

	/**
	 * Process the newly found word.
	 * @param current The current TextResult
	 * @param wordState The WordState used to track our current state
	 * @param trimmed The source input to analyze
	 * @param idx The location of the cursor in the source input (trimmed)
	 */
	private void endOfWord(final TextResult current, final WordState wordState, final String trimmed, final int idx) {
		// Reject if it looks too long to be a word
		if (idx - lastOffset > config.getLongWord())
			current.determination = Determination.TOO_LONG;

		wordState.lastWordLength = idx - lastOffset;
		current.words++;
		wordsInSentence++;

		// If all the characters in the word are digits then could those digits
		if (wordState.digitsInWord == wordState.lastWordLength && wordState.lastWordLength <= 4)
			current.digits += wordState.digitsInWord;

		// If all the characters in the word are alphas then we have a 'real' word
		if (wordState.charsInWord == wordState.lastWordLength) {
			totalAlphaWordLength += wordState.lastWordLength;
			alphaWords++;
			if (wordState.lastWordLength > 2) {
				longWords++;
				if (config.getStarts().contains(trimmed.substring(lastOffset, lastOffset+2).toLowerCase(locale)))
					realWords++;
			}
		}

		wordState.digitsInWord = 0;
		wordState.charsInWord = 0;
	}

	/**
	 * Analyze the given input string to determine if it looks like free-form text in this locale.
	 * @param trimmed The input string
	 * @return A TextResult with both a determination of the input as well as a set of statistics.
	 */
	public TextResult analyze(final String trimmed) {
		final TextResult ret = new TextResult();
		final int len = trimmed.length();
		char lastCh = ' ';
		boolean wordStarted = false;
		WordState wordState = new WordState();

		wordsInSentence = 0;
		lastOffset = 0;
		alphaWords = 0;
		realWords = 0;
		longWords = 0;
		totalAlphaWordLength = 0;

		int idx;
		for (idx = 0; idx < len && !(idx >= config.getMaxLength() && Character.isWhitespace(lastCh)); idx++) {
			final char ch = trimmed.charAt(idx);

			// Reached the end of a 'word'
			if (wordStarted && config.getWordBreak().indexOf(ch) != -1 && config.getWordBreak().indexOf(lastCh) == -1) {
				endOfWord(ret, wordState, trimmed, idx);
				if (ret.determination != Determination.OK)
					return ret;

				ret.wordBreaks++;
				lastOffset = idx;
				wordStarted = false;
			}

			if (config.getWordBreak().indexOf(lastCh) != -1)
				lastOffset = idx;

			if (config.getSentenceBreak().indexOf(ch) != -1) {
				// We don't like short sentences on the other hand e.g., i.e., et al., etc. should not be rejected
				if (wordsInSentence == 1 && wordState.lastWordLength > 3) {
					ret.determination = Determination.SENTENCE_TOO_SHORT;
					return ret;
				}
				wordsInSentence = 0;
			}

			if (Character.isWhitespace(ch))
				ret.spaces++;
			else if (Character.isAlphabetic(ch)) {
				ret.alphas++;
				wordState.charsInWord++;
				wordStarted = true;
			}
			else if (Character.isDigit(ch)) {
				wordState.digitsInWord++;
				wordStarted = true;
			}
			else if (config.getPunctuation().indexOf(ch) != -1)
				ret.punctuation++;
			else if (config.getWordBreak().indexOf(ch) != -1 && config.getWordBreak().indexOf(lastCh) != -1)
				ret.wordBreaks++;
			lastCh = ch;
		}

		if (wordStarted) {
			endOfWord(ret, wordState, trimmed, idx);
			if (ret.determination != Determination.OK)
				return ret;
		}

		// Only one word so reject
		if (ret.words == 1) {
			ret.determination = Determination.SINGLE_WORD;
			return ret;
		}

		// Need some real words and a reasonable percentage of real words
		if (realWords == 0 || (longWords != 0 && (longWords - realWords)*100/longWords > 25)) {
			ret.determination = Determination.NOT_ENOUGH_REAL_WORDS;
			return ret;
		}

		// Count the alphas, wordBreaks, punctuation(,.) and any digits in digit only words
		if ((ret.alphas + ret.digits + ret.wordBreaks + ret.spaces + ret.punctuation)*100/idx < config.getSimplePercentage()) {
			ret.determination = Determination.PERCENT_TOO_LOW;
			return ret;
		}

		if ((ret.alphas + ret.spaces) * 100/idx < config.getAlphaSpacePercentage()) {
			ret.determination = Determination.PERCENT_TOO_LOW;
			return ret;
		}

		// Calculate the average word length
		double avgWordLength = (double)totalAlphaWordLength/alphaWords;

		// Average length of words need to look reasonable for this language
		if (alphaWords > 3 && ((len > 10 && avgWordLength < config.getAverageLow()) || avgWordLength > config.getAverageHigh())) {
			ret.determination = Determination.BAD_AVERAGE_LENGTH;
			return ret;
		}

		return ret;
	}
}
