package com.cobber.fta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Locale;
import java.util.Map;

public abstract class LogicalTypeFiniteSimple extends LogicalTypeFinite {
	private String qualifier;
	private String[] hotWords;
	private String regexp;
	private String backout;
	private Reader reader;

	public LogicalTypeFiniteSimple(String qualifier, String[] hotWords, String regexp, String backout, Reader reader, int threshold) {
		this.qualifier = qualifier;
		this.hotWords = hotWords;
		this.regexp = regexp;
		this.backout = backout;
		this.reader = reader;
		this.threshold = threshold;
	}

	@Override
	public synchronized boolean initialize(Locale locale) {
		// Only set up the Static Data once
		if (getMembers().isEmpty()) {
			try (BufferedReader bufferedReader = new BufferedReader(reader)){
				String line = null;

				while ((line = bufferedReader.readLine()) != null) {
					getMembers().add(line);
				}
			} catch (IOException e) {
				throw new IllegalArgumentException("Internal error: Issues with database for: " + qualifier);
			}
		}

		super.initialize(locale);

		return true;
	}

	@Override
	public String getQualifier() {
		return qualifier;
	}

	@Override
	public String getRegExp() {
		return regexp;
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples,
			StringFacts stringFacts, Map<String, Integer> cardinality, Map<String, Integer> outliers) {
		boolean streamNamePositive = false;
		for (int i = 0; i < hotWords.length; i++)
			if (dataStreamName.toLowerCase(locale).contains(hotWords[i])) {
				streamNamePositive = true;
				break;
			}

		int maxOutliers = 1;
		int minCardinality = 4;
		int minSamples = 20;
		if (streamNamePositive) {
			maxOutliers = 4;
			minCardinality = 1;
			minSamples = 4;
		}

		if (outliers.size() > maxOutliers)
			return backout;
		if (cardinality.size() < minCardinality)
			return backout;
		if (realSamples < minSamples)
			return backout;

		return (double)matchCount / realSamples >= getThreshold()/100.0 ? null : backout;
	}
}
