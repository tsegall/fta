package com.cobber.fta.text;

public class TextConfig {
	private int longWord;
	private double averageLow;
	private double averageHigh;
	private int characterPercentage;
	private int maxLength;
	private String wordBreak;
	private String punctuation;

	public TextConfig(final int longWord, final double averageLow, double averageHigh, final int characterPercentage, final int maxLength, final String wordBreak, final String punctuation) {
		this.longWord = longWord;
		this.averageLow = averageLow;
		this.averageHigh = averageHigh;
		this.characterPercentage = characterPercentage;
		this.maxLength = maxLength;
		this.wordBreak = wordBreak;
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

	public int getCharacterPercentage() {
		return characterPercentage;
	}

	public int getMaxLength() {
		return maxLength;
	}

	public String getWordBreak() {
		return wordBreak;
	}

	public String getPunctuation() {
		return punctuation;
	}
}