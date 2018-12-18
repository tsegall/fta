package com.cobber.fta;

import java.util.Map;

public abstract class LogicalType {
	protected int threshold;

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
	 * The percentage when we declare success 0 - 100.
	 * We use this percentage in the determination of the Logical Type.  When and how it is used varies based on the plugin. 
	 * @return The threshold percentage.
	 */
	public int getThreshold() {
		return threshold;
	}

	/**
	 * The percentage when we declare success 0 - 100.
	 * We use this percentage in the determination of the Logical Type.  When and how it is used varies based on the plugin. 
	 * @param threshold the new threshold.
	 */
	public void setThreshold(int threshold) {
		this.threshold = threshold;
	}

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

	/**
	 * Given the data to date as embodied by the arguments return null if we think this is an instance
	 * of this logical type, if not return a new suitable pattern.
	 * instance of this logical type.
	 * @param matchCount Number of samples that match so far (as determined by isValid()
	 * @param realsamples Number of real (i.e. non-blank and non-null) samples that we have processed so far.
	 * @param cardinality Cardinality set, up to the maximum maintained
	 * @param outliers Outlier set, up to the maximum maintained
	 * @return Null if we think this is an instance of this logical type (backout pattern otherwise)
	 */
	public abstract String shouldBackout(long matchCount, long realsamples, Map<String, Integer> cardinality, Map<String, Integer> outliers);
}
