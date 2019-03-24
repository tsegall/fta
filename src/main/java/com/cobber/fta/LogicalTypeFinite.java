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

	@Override
	public boolean isValid(String input) {
		String trimmed = input.trim();
		return trimmed.length() >= minLength && trimmed.length() <= maxLength && getMembers().contains(trimmed);
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
}
