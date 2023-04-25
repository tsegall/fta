/*
 * Copyright 2017-2023 Tim Segall
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Capture a set of key metrics for any given language.
 */
public final class TextConfig {
	/** Any word longer than this will cause rejection, so set it to something long but not silly (rejecting a couple of should not effect the analysis). */
	private final int longWord;
	/** A reasonable lower bound for the average word length in the language. */
	private final double averageLow;
	/** A reasonable upper bound for the average word length in the language. */
	private final double averageHigh;
	/** The percentage of 'alpha' pr 'space' characters that we expect to be present. */
	private final int alphaSpacePercentage;
	/** The percentage of 'reasonable' characters that we expect to be present. */
	private final int simplePercentage;
	/** Only analyze the first <n> characters. */
	private final int maxLength;
	/** Sentence Break characters. */
	private final String sentenceBreak;
	/** Word Break characters. */
	private final String wordBreak;
	/** Punctuation character. */
	private final String punctuation;
	/** Valid starts for common words. */
	private final Set<String> validStarts;

	public TextConfig(final int longWord, final double averageLow, final double averageHigh, final int alphaSpacePercentage,
			final int simplePercentage, final int maxLength, final String sentenceBreak, final String wordBreak,
			final String punctuation,
			final String[] starts) {
		this.longWord = longWord;
		this.averageLow = averageLow;
		this.averageHigh = averageHigh;
		this.alphaSpacePercentage = alphaSpacePercentage;
		this.simplePercentage = simplePercentage;
		this.maxLength = maxLength;
		this.wordBreak = wordBreak;
		this.sentenceBreak = sentenceBreak;
		this.punctuation = punctuation;
		this.validStarts = new HashSet<>(Arrays.stream(starts).collect(Collectors.toSet()));
	}

	/**
	 * The maximum length we expect any likely word to be in the target language.
	 * @return The maximum length we expect any likely word to be in the target language.
	 */
	public int getLongWord() {
		return longWord;
	}

	/**
	 * A reasonable lower bound for the average word length in the language.
	 * @return A reasonable lower bound for the average word length in the language.
	 */
	public double getAverageLow() {
		return averageLow;
	}

	/**
	 * A reasonable upper bound for the average word length in the language.
	 * @return A reasonable upper bound for the average word length in the language.
	 */
	public double getAverageHigh() {
		return averageHigh;
	}

	/**
	 * An estimate of the percentage of 'alpha' or space (isWhiteSpace()) characters that we expect to be present.
	 * @return An estimate of the percentage of 'alpha' or space (isWhiteSpace()) characters that we expect to be present.
	 */
	public int getAlphaSpacePercentage() {
		return alphaSpacePercentage;
	}

	/**
	 * An estimate of the percentage of 'reasonable' characters that we expect to be present.
	 * Note: The reasonable characters are defined as the sum of:
	 *  - alphas, digits (in digit only words), wordBreaks, spaces, and punctuation
	 *  @return An estimate of the percentage of 'reasonable' characters that we expect to be present.
	 */
	public int getSimplePercentage() {
		return simplePercentage;
	}

	/**
	 *  The maximum number of character to analyze in the input.
	 *  @return The maximum number of character to analyze in the input.
	 */
	public int getMaxLength() {
		return maxLength;
	}

	/**
	 * The Sentence Break characters.
	 * @return The set of characters used to break paragraphs into sentences.
	 */
	public String getSentenceBreak() {
		return sentenceBreak;
	}

	/**
	 * The Word Break characters.
	 * @return The set of characters used to break sentences into words.
	 */
	public String getWordBreak() {
		return wordBreak;
	}

	/**
	 * The Punctuation characters.
	 * @return The set of characters recognized as punctuation.
	 */
	public String getPunctuation() {
		return punctuation;
	}

	/**
	 * The 'likely' set of two character initial stems.
	 * For example, in English 'fo' is reasonable (for, form, foot, ...) whereas 'xz' is not,
	 * as no words start with xz.
	 * @return The 'likely' set of two character initial stems.
	 */
	public Set<String> getStarts() {
		return validStarts;
	}

}