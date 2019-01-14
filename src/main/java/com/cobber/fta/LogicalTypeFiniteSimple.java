package com.cobber.fta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Locale;
import java.util.Map;

public abstract class LogicalTypeFiniteSimple extends LogicalTypeFinite {
	private String qualifier;
	private String regexp;
	private String backout;
	private Reader reader;

	public LogicalTypeFiniteSimple(String qualifier, String regexp, String backout, Reader reader, int threshold) {
		this.qualifier = qualifier;
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
	public String getRegexp() {
		return regexp;
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realsamples,
			Map<String, Integer> cardinality, Map<String, Integer> outliers) {
		if (outliers.size() > 1)
			return backout;

		return (double)matchCount / realsamples >= getThreshold()/100.0 ? null : backout;
	}
}
