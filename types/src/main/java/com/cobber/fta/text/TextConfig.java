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

/**
 * Capture a set of key metrics for any given language.
 */
public final class TextConfig {
	/** Any word longer than this will cause rejection, so set it to something long but not silly (rejecting a couple of should not effect the analysis). */
	private final int longWord;
	/** Average word length in the language, so choose a reasonable lower bound. */
	private final double averageLow;
	/** Average word length in the language, so choose a reasonable upper bound. */
	private final double averageHigh;
	/** The percentage of 'alpha' characters that we expect to be present. */
	private final int alphaPercentage;
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

	public TextConfig(final int longWord, final double averageLow, final double averageHigh, final int alphaPercentage,
			final int simplePercentage, final int maxLength, final String sentenceBreak, final String wordBreak, final String punctuation) {
		this.longWord = longWord;
		this.averageLow = averageLow;
		this.averageHigh = averageHigh;
		this.alphaPercentage = alphaPercentage;
		this.simplePercentage = simplePercentage;
		this.maxLength = maxLength;
		this.wordBreak = wordBreak;
		this.sentenceBreak = sentenceBreak;
		this.punctuation = punctuation;
	}

	public int getLongWord() {
		return longWord;
	}

	public double getAverageLow() {
		return averageLow;
	}

	public double getAverageHigh() {
		return averageHigh;
	}

	public int getAlphaPercentage() {
		return alphaPercentage;
	}

	public int getSimplePercentage() {
		return simplePercentage;
	}

	public int getMaxLength() {
		return maxLength;
	}

	public String getSentenceBreak() {
		return sentenceBreak;
	}

	public String getWordBreak() {
		return wordBreak;
	}

	public String getPunctuation() {
		return punctuation;
	}
}