package com.cobber.fta;

import java.util.Map;

/**
 * All Logical Types that consist of a unconstrained domain, e.g. an infinite (or large) number of elements should
 * subclass this abstract class.
 */
public abstract class LogicalTypeInfinite extends LogicalType {
	protected int candidateCount = 0;

	/**
	 * A fast check to see if the supplied String might be an instance of this logical type?
	 * If successful the number of candidates should be incremented. See {@link #getCandidateCount() getCandidateCount()}
	 * 
	 * @param input String to check
	 * @param compressed A compressed representation of the input string (e.g. \d{5} for 20351).
	 * @param charCounts An array of occurrence counts for characters in the input (ASCII-only).
	 * @param lastIndex An array of the last index where character is located (ASCII-only).
	 * @return true iff the supplied String is a possible instance of this Logical type.
	 */
	public abstract boolean isCandidate(String input, StringBuilder compressed, int[] charCounts, int[] lastIndex);
	
	/**
	 * Get the number of candidates reviewed that were valid. 
	 * @return true iff the supplied String is an instance of this Logical type.
	 */
	public int getCandidateCount() {
		return candidateCount;
	}

	/**
	 * Given the data to date as embodied by the arguments return a boolean indicating whether we think this is an
	 * instance of this logical type.
	 * @param matchount Number of samples that match so far (as determined by isValid()
	 * @param realsamples Number of real (i.e. non-blank and non-null) samples that we have processed so far.
	 * @param cardinality Cardinality set, up to the maximum maintained
	 * @param outliers Outlier set, up to the maximum maintained
	 * @return True if we think this is an instance of this logical type.
	 */
	public abstract boolean shouldBackout(long matchount, long realsamples, Map<String, Integer> cardinality, Map<String, Integer> outliers);
}
