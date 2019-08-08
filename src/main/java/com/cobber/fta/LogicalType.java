/*
 * Copyright 2017-2019 Tim Segall
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cobber.fta;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class LogicalType implements Comparable<LogicalType> {
	protected PluginDefinition defn;
	protected Locale locale = null;
	protected int priority;
	protected Pattern[] headerPatterns;

	@Override
	public int compareTo(LogicalType other) {
	  return Integer.compare(priority, other.priority);
	}

	LogicalType(PluginDefinition plugin) {
		this.defn = plugin;
		this.priority = plugin.priority;
	}

	protected int threshold;

	/**
	 * Called to perform any initialization.
	 * @param locale The locale used for this analysis
	 * @return True if initialization was successful.
	 */
	public boolean initialize(Locale locale) {
		if (getBaseType() == null)
			throw new IllegalArgumentException("baseType cannot be null");

		this.locale = locale;

		if (defn != null && defn.headerRegExps != null) {
			headerPatterns = new Pattern[defn.headerRegExps.length];
			for (int i = 0; i <  defn.headerRegExps.length; i++)
				headerPatterns[i] = Pattern.compile(defn.headerRegExps[i]);
		}

		return true;
	}

	protected int getHeaderConfidence(String dataStreamName) {
		if (headerPatterns != null)
			for (int i = 0; i < headerPatterns.length; i++) {
				if (headerPatterns[i].matcher(dataStreamName).matches())
					return defn.headerRegExpConfidence[i];
			}

		return 0;
	}

	/**
	 *  The user-friendly name of the Qualifier.  For example, EMAIL for an email address
	 *  @return The user-friendly name of the type-qualifier.
	 */
	public abstract String getQualifier();

	/**
	 *  The relative priority of this plugin.
	 *  @return The relative priority of this plugin.
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * The Regular Expression that most closely matches (See {@link #isRegExpComplete()}) this Logical Type.
	 * Note: All valid matches will match this RE, but the inverse is not necessarily true.
	 * @return The Java Regular Expression that most closely matches this Logical Type.
	 */
	public abstract String getRegExp();

	/**
	 * Is the returned Regular Expression a true representation of the Logical Type.
	 * For example, \\d{5} is not for US ZIP codes, whereas (?i)(male|female) could be valid for a Gender.
	 * @return The Java Regular Expression that most closely matches this Logical Type.
	 */
	public abstract boolean isRegExpComplete();

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
	 * Confidence in the type classification.
	 * Typically this will be the number of matches divided by the number of real samples.
	 * @param matchCount Number of matches (as determined by isValid())
	 * @param realSamples Number of samples observed - does not include either nulls or blanks
	 * @param dataStreamName Name of the Data Stream
	 * @return Confidence as a percentage.
	 */
	public double getConfidence(long matchCount, long realSamples, String dataStreamName) {
		return (double)matchCount/realSamples;
	}

	/**
	 * The underlying type we are qualifying.
	 * @return The underlying type - e.g. STRING, INT, etc.
	 */
	public abstract PatternInfo.Type getBaseType();

	/**
	 * Is the supplied String an instance of this logical type?
	 * @param input String to check (trimmed for Numeric base Types, un-trimmed for String base Type)
	 * @return true iff the supplied String is an instance of this Logical type.
	 */
	public abstract boolean isValid(String input);

	/**
	 * Given the data to date as embodied by the arguments return null if we think this is an instance
	 * of this logical type, if not return a new suitable pattern.
	 * instance of this logical type.
	 * @param dataStreamName The name of the Data Stream
	 * @param matchCount Number of samples that match so far (as determined by isValid()
	 * @param realSamples Number of real (i.e. non-blank and non-null) samples that we have processed so far.
	 * @param stringFacts Facts (min, max, sum) for the analysis to date (optional - i.e. maybe null)
	 * @param cardinality Cardinality set, up to the maximum maintained
	 * @param outliers Outlier set, up to the maximum maintained
	 * @return Null if we think this is an instance of this logical type (backout pattern otherwise)
	 */
	public abstract String isValidSet(String dataStreamName, long matchCount, long realSamples, StringFacts stringFacts, Map<String, Long> cardinality, Map<String, Long> outliers);

	/**
	 * Does the set of members enumerated reflect the entire set.  For example any of the ISO sets are reference sets and
	 * hence complete, compared to FirstName and LastName where the set provided is of the common names.
	 * If isClosed() is true then isValid() false does not imply that the input is not valid just that it is not in the set of
	 * 'known' members.
	 * @return A boolean indicating if the set is closed.
	 */
	public abstract boolean isClosed();
}
