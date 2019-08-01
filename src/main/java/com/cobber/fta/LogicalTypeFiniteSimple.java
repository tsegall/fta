package com.cobber.fta;

import java.io.Reader;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public abstract class LogicalTypeFiniteSimple extends LogicalTypeFinite {
	protected String qualifier;
	protected String regexp;
	protected String backout;
	protected Reader reader;
	protected SingletonSet memberSet;

	public LogicalTypeFiniteSimple(PluginDefinition plugin, String regexp, String backout, int threshold) {
		super(plugin);
		this.qualifier = plugin.qualifier;
		this.regexp = regexp;
		this.backout = backout;
		this.threshold = threshold;
	}

	public void setContent(String contentType, String content) {
		this.memberSet = new SingletonSet(contentType, content);
	}

	@Override
	public Set<String> getMembers() {
		return memberSet.getMembers();
	}

	@Override
	public String nextRandom() {
		return memberSet.getAt(random.nextInt(getMembers().size()));
	}

	@Override
	public boolean initialize(Locale locale) {
		if (this.backout == null)
			throw new IllegalArgumentException("Internal error: Finite Simple types require backout.");

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
			StringFacts stringFacts, Map<String, Long> cardinality, Map<String, Long> outliers) {
		int headerConfidence = getHeaderConfidence(dataStreamName);

		int maxOutliers = 1;
		int minCardinality = 4;
		int minSamples = 20;
		if (headerConfidence != 0) {
			minCardinality = 1;
			minSamples = 4;
			maxOutliers =  headerConfidence < 90 ? 4 : getSize() / 2;
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
