package com.cobber.fta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class LogicalTypeFiniteSimple extends LogicalTypeFinite {
	protected String qualifier;
	protected Pattern[] headerPatterns;
	protected String regexp;
	protected String backout;
	protected Reader reader;

	public LogicalTypeFiniteSimple(PluginDefinition plugin, String regexp, String backout, Reader reader, int threshold) {
		super(plugin);
		this.qualifier = plugin.qualifier;
		this.regexp = regexp;
		this.backout = backout;
		this.reader = reader;
		this.threshold = threshold;
	}

	@Override
	public String nextRandom() {
		return getMemberArray()[random.nextInt(getMembers().size())];
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

		if (defn.headerRegExps != null) {
			headerPatterns = new Pattern[defn.headerRegExps.length];
			for (int i = 0; i <  defn.headerRegExps.length; i++)
				headerPatterns[i] = Pattern.compile(defn.headerRegExps[i]);
		}

		super.initialize(locale);

		return true;
	}

	public abstract String[] getMemberArray();

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
			StringFacts stringFacts, Map<String, Long> cardinality, Map<String, Long> outliers) {
		int streamNamePositive = 0;

		if (headerPatterns != null)
			for (int i = 0; i < headerPatterns.length; i++) {
				if (headerPatterns[i].matcher(dataStreamName).matches())
					streamNamePositive += defn.headerRegExpConfidence[i];
			}

		int maxOutliers = 1;
		int minCardinality = 4;
		int minSamples = 20;
		if (streamNamePositive >= 50) {
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
