package com.cobber.fta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class LogicalTypeFiniteSimple extends LogicalTypeFinite {
	private Set<String> members;
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
	public boolean initialize() {
		members = new HashSet<String>();	
		try (BufferedReader bufferedReader = new BufferedReader(reader)){
			String line = null;

			while ((line = bufferedReader.readLine()) != null) {
				members.add(line);
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("Internal error: Issues with database for: " + qualifier);
		}
		
		super.initialize();

		return true;
	}

	@Override
	public Set<String> getMembers() {
		return members;
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
	public String shouldBackout(long matchCount, long realsamples, Map<String, Integer> cardinality,
			Map<String, Integer> outliers) {
		if (outliers.size() > 1)
			return backout;

		return (double)matchCount / realsamples >= getThreshold()/100.0 ? null : backout;
	}

}
