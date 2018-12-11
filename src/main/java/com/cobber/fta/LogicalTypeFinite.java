package com.cobber.fta;

import java.util.Set;

import com.cobber.fta.PatternInfo.Type;

/**
 * All Logical Types that consist of a constrained domain, e.g. a finite (small) number of elements should
 * subclass this abstract class.
 */
public abstract class LogicalTypeFinite extends LogicalType {
	protected int minLength = Integer.MAX_VALUE;
	protected int maxLength = Integer.MIN_VALUE;

	public abstract Set<String> getMembers();

	@Override
	public Type getBaseType() {
		return PatternInfo.Type.STRING;
	}
	
	@Override
	public boolean isValid(String input) {
		return input.length() >= minLength && input.length() <= maxLength && getMembers().contains(input);
	}
	
	@Override
	public boolean initialize() {
		for (String member : getMembers()) {
			int len = member.length();
			if (len < minLength)
				minLength = len;
			if (len > maxLength)
				maxLength = len;
		}
		return true;
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
