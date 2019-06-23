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
	private final int IDENTIFIED_THRESHOLD = 40;
	private final int NON_IDENTIFIED_THRESHOLD = 60;
	private final int ITERS = 5;
	private Dodge[] iterators = null;

	public LogicalTypePersonName(PluginDefinition plugin, String filename) throws FileNotFoundException {
		super(plugin, REGEXP, ".*", 95);
		setReader(new InputStreamReader(LogicalTypePersonName.class.getResourceAsStream("/reference/" + filename)));
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

	/*
	 * Note: The input String will be both trimmed and converted to upper Case
	 * @see com.cobber.fta.LogicalType#isValid(java.lang.String)
	 */
	@Override
	public boolean isValid(String input) {
		String trimmedUpper = input.trim().toUpperCase(locale);
		if (trimmedUpper.length() < minLength && trimmedUpper.length() > maxLength)
			return false;
		if (getMembers().contains(trimmedUpper))
			return true;

		// Throw 40% away
		if (random.nextInt(100) >= IDENTIFIED_THRESHOLD)
			return false;

		// For the balance of the failures we will say they are valid if it is just a single word
		for (int i = 0; i < trimmedUpper.length(); i++) {
			if (!Character.isAlphabetic(trimmedUpper.charAt(i)))
				return false;
		}

		return true;
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples,
			StringFacts stringFacts, Map<String, Long> cardinality, Map<String, Long> outliers) {

		int headerConfidence = getHeaderConfidence(dataStreamName);

		if (headerConfidence >= 90 && (double)matchCount / realSamples >= (double)IDENTIFIED_THRESHOLD/100)
			return null;

		int minCardinality = 10;
		int minSamples = 20;
		if (headerConfidence != 0) {
			minCardinality = 5;
			minSamples = 5;
		}

		if (cardinality.size() < minCardinality)
			return backout;
		if (realSamples < minSamples)
			return backout;

		if (headerConfidence >= 50 && (double)matchCount / realSamples >= (double)NON_IDENTIFIED_THRESHOLD/100)
			return null;

		return (double)matchCount / realSamples >= getThreshold()/100.0 ? null : backout;
	}

	@Override
	public boolean isClosed() {
		return false;
	}
}
