package com.cobber.fta.text;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TextProcessor {
	private TextConfig config;
	private static Map<String, TextConfig> allConfigData = new HashMap<>();

	public TextProcessor(Locale locale) {
		if (locale == null)
			locale = Locale.getDefault();
		String language = locale.getLanguage().toUpperCase(Locale.ROOT);
		this.config = allConfigData.get(language);
	}

	public enum Determination {
		OK,
		TOO_LONG,
		PERCENT_TOO_LOW,
		BAD_AVERAGE_LENGTH,
		TOO_SHORT
	}

	public class TextResult {
		private Determination determination;
		private int alphas;
		private int digits;
		private int words;
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
		// English
		allConfigData.put("EN", new TextConfig(
				20,				// antidisestablishmentarianism is 28 (there are longer), so we choose something that is reasonable
				3.0, 9.0,		// Average word length in English is ~5, so choose a reasonable lower and upper bound
				80,				// The percentage of 'reasonable' characters that we expect to be present
				120,			// Only analyze the first <n> characters
				", ./!?;:-",	// Word Break characters
				",.'-()!?;:"	// Punctuation character
				));
	}

	public TextResult analyze(String trimmed) {
		final TextResult ret = new TextResult();
		final int len = trimmed.length();
		int digitsInWord = 0;
		int lastOffset = 0;
		char lastCh = ' ';
		int totalWordLength = 0;
		boolean wordStarted = false;

		int idx;
		for (idx = 0; idx < len && !(idx >= config.getMaxLength() && Character.isWhitespace(lastCh)); idx++) {
			char ch = trimmed.charAt(idx);
			// Reached the end of a 'word'
			if (wordStarted && config.getWordBreak().indexOf(ch) != -1 && config.getWordBreak().indexOf(lastCh) == -1) {
				// Reject if it looks too long to be a word
				if (idx - lastOffset > config.getLongWord()) {
					ret.determination = Determination.TOO_LONG;
					return ret;
				}

				int wordLength = idx - lastOffset;
				totalWordLength += wordLength;
				ret.words++;
				ret.wordBreaks++;
				lastOffset = idx;
				wordStarted = false;
				// If all the characters in the word are digits then call it a word
				if (digitsInWord == wordLength && wordLength <= 4)
					ret.digits += digitsInWord;
				digitsInWord = 0;
			}

			if (config.getWordBreak().indexOf(lastCh) != -1)
				lastOffset = idx;

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

		if (ret.words == 1) {
			ret.determination = Determination.TOO_LONG;
			return ret;
		}

		if ((len < 10 && ret.wordBreaks == 0) || len < 6) {
			ret.determination = Determination.TOO_SHORT;
			return ret;
		}

		// Count the alphas, wordBreaks, punctuation(,.) and any digits in digit only words
		if ((ret.alphas + ret.digits + ret.wordBreaks + ret.spaces + ret.punctuation)*100/idx < config.getCharacterPercentage()) {
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
