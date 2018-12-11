package com.cobber.fta;

public abstract class LogicalType {
	/**
	 * Called to perform any initialization.
	 * @return True if initialization was successful.
	 */
	public abstract boolean initialize();

	/**
	 *  The user-friendly name of the Qualifier.  For example, EMAIL for an email address
	 *  @return The user-friendly name of the type-qualifier.
	 */
	public abstract String getQualifier();

	/**
	 * The RE that most closely matches this Logical Type.
	 * Note: All valid matches will match this RE, but the inverse is not necessarily true.
	 * @return The Java RE that most closely matches this Logical Type.
	 */
	public abstract String getRegexp();

	/**
	 * The percentage when we declare success 0.0 - 1.0.
	 * We need this percentage in the initial sample set to conclude it is of this type.
	 * @return The threshold percentage.
	 */
	public abstract double getSampleThreshold();

	/**
	 * The underlying type we are qualifying.
	 * @return The underlying type - e.g. STRING, INT, etc.
	 */
	public abstract PatternInfo.Type getBaseType();

	/** 
	 * Is the supplied String an instance of this logical type?
	 * @param input String to check
	 * @return true iff the supplied String is an instance of this Logical type.
	 */
	public abstract boolean isValid(String input);
}
