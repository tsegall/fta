package com.cobber.fta;

/**
 * All Logical Types that consist of a unconstrained domain, e.g. an infinite (or large) number of elements should
 * subclass this abstract class.
 */
public abstract class LogicalTypeInfinite extends LogicalTypeCode {
	public LogicalTypeInfinite(PluginDefinition plugin) {
		super(plugin);
	}

	/**
	 * A fast check to see if the supplied String might be an instance of this logical type?
	 *
	 * @param trimmed String to check
	 * @param compressed A compressed representation of the input string (e.g. \d{5} for 20351).
	 * @param charCounts An array of occurrence counts for characters in the input (ASCII-only).
	 * @param lastIndex An array of the last index where character is located (ASCII-only).
	 * @return true iff the supplied String is a possible instance of this Logical type.
	 */
	public abstract boolean isCandidate(String trimmed, StringBuilder compressed, int[] charCounts, int[] lastIndex);

	@Override
	public boolean isRegExpComplete() {
		return false;
	}
}
