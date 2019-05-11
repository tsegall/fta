package com.cobber.fta;

import java.util.Locale;
import java.util.Set;

import com.cobber.fta.PatternInfo.Type;

/**
 * All Logical Types that consist of a constrained domain, e.g. a finite (small) number of elements should
 * subclass this abstract class.
 */
public abstract class LogicalTypeFinite extends LogicalTypeCode {
	protected int minLength = Integer.MAX_VALUE;
	protected int maxLength = Integer.MIN_VALUE;

	public abstract Set<String> getMembers();

	public LogicalTypeFinite(PluginDefinition plugin) {
		super(plugin);
	}

	public LogicalTypeFinite() {
		super(null);
	}

	@Override
	public Type getBaseType() {
		return PatternInfo.Type.STRING;
	}

	/*
	 * Note: The input String will be both trimmed and converted to upper Case
	 * @see com.cobber.fta.LogicalType#isValid(java.lang.String)
	 */
	@Override
	public boolean isValid(String input) {
		String trimmedUpper = input.trim().toUpperCase(locale);
		return trimmedUpper.length() >= minLength && trimmedUpper.length() <= maxLength && getMembers().contains(trimmedUpper);
	}

	@Override
	public boolean initialize(Locale locale) {
		super.initialize(locale);

		for (String member : getMembers()) {
			int len = member.length();
			if (len < minLength)
				minLength = len;
			if (len > maxLength)
				maxLength = len;
		}
		return true;
	}

	@Override
	public boolean isRegExpComplete() {
		return false;
	}

	/**
	 * Get the number of members in this Logical Type.
	 * @return The number of members
	 */
	public int getSize() {
		return getMembers().size();
	}

	/**
	 * Get the minimum length of instances of this Logical Type.
	 * @return The minimum length of instances
	 */
	public int getMinLength() {
		return minLength;
	}

	/**
	 * Get the maximum length of instances of this Logical Type.
	 * @return The maximum length of instances
	 */
	public int getMaxLength() {
		return maxLength;
	}

	/**
	 * Does the set of members enumerated reflect the entire set.  For example any of the ISO sets are reference sets and
	 * hence complete, compared to FirstName and LastName where the set provided is of the common names.
	 * @return A boolean indicating if the set is closed.
	 */
	public boolean isClosed() {
		return true;
	}
}
