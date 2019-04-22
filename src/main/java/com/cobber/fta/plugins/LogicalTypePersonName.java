package com.cobber.fta.plugins;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;

import com.cobber.fta.LogicalTypeFiniteSimple;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.StringFacts;

public abstract class LogicalTypePersonName extends LogicalTypeFiniteSimple {
	private class Dodge {
		Iterator<String> iter;
	}

	public final static String REGEXP = "[- \\p{IsAlphabetic}]*";
	private final int ITERS = 5;
	private Dodge[] iterators = null;

	public LogicalTypePersonName(PluginDefinition plugin, String filename) throws FileNotFoundException {
		super(plugin, REGEXP, ".*",
				new InputStreamReader(LogicalTypeCAProvince.class.getResourceAsStream("/reference/" + filename)),
				95);
	}

	@Override
	public String nextRandom() {
		// No easy way to get a random element from a set, could convert to array but that uses lots of memory,
		// so simulate randomness by having a number of iterators start at different points in the set
		if (iterators == null) {
			iterators = new Dodge[ITERS];
			for (int i = 0; i < iterators.length; i++) {
				iterators[i] = new Dodge();
				iterators[i].iter = getMembers().iterator();
				int offset = random.nextInt(getMembers().size() / 2);
				for (int j = 0; j < offset; j++)
					iterators[i].iter.next();
			}
		}
		Dodge any = iterators[random.nextInt(ITERS)];
		if (!any.iter.hasNext())
			any.iter = getMembers().iterator();
		return any.iter.next();
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples,
			StringFacts stringFacts, Map<String, Long> cardinality, Map<String, Long> outliers) {
		boolean streamNamePositive = false;
		if (hotWords != null)
			for (int i = 0; i < hotWords.length; i++)
				if (dataStreamName.toLowerCase(locale).contains(hotWords[i])) {
					streamNamePositive = true;
					break;
				}

		int maxOutliers = getMembers().size() / 10;
		int minCardinality = 10;
		int minSamples = 20;
		if (streamNamePositive) {
			maxOutliers = getMembers().size() / 20;
			minCardinality = 5;
			minSamples = 5;
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
