/*
 * Copyright 2017-2018 Tim Segall
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

import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * TextAnalysisResult is the result of a {@link TextAnalyzer} analysis of a data stream.
 */
public class TextAnalysisResult {
	private final String name;
	private final long matchCount;
	private final long sampleCount;
	private final long nullCount;
	private final long blankCount;
	private final long leadingZeroCount;
	private final PatternInfo patternInfo;
	private final boolean leadingWhiteSpace;
	private final boolean trailingWhiteSpace;
	private final double confidence;
	private final String minValue;
	private final String maxValue;
	private final int minLength;
	private final int maxLength;
	private final String sum;
	private final Map<String, Integer> cardinality;
	private final Map<String, Integer> outliers;
	private final boolean key;

	/**
	 * @param name The name of the data stream being analyzed.
	 * @param matchCount The number of samples that match the patternInfo.
	 * @param patternInfo The PatternInfo associated with this matchCount.
	 * @param leadingWhiteSpace Is there leading White Space?
	 * @param trailingWhiteSpace Is there trailing White Space?
	 * @param sampleCount The total number of samples seen.
	 * @param nullCount The number of nulls seen in the sample set.
	 * @param blankCount The number of blanks seen in the sample set.
	 * @param leadingZeroCount The number of leading zeros seen in sample set.  Only relevant for type Long.
	 * @param confidence The percentage confidence in the analysis.  The matchCount divided by the sampleCount.
	 * @param minValue A String representation of the minimum value.  Only relevant for Numeric/String types.
	 * @param maxValue A String representation of the maximum value.  Only relevant for Numeric/String types.
	 * @param minLength Get the minimum length. Only relevant for Numeric, Boolean and String. Note: For String and Boolean types this length includes any whitespace.
	 * @param maxLength Get the maximum length. Only relevant for Numeric, Boolean and String. Note: For String and Boolean types this length includes any whitespace.
	 * @param sum A String representation of the sum of all values seen.  Only relevant for numeric types.
	 * @param cardinality A map of valid (matching) input values and the count of occurrences of the those input values.
	 * @param outliers A map of invalid input values and the count of occurrences of the those input values.
	 * @param key Do we think this field is a key.
	 */
	TextAnalysisResult(final String name, final long matchCount, final PatternInfo patternInfo, final boolean leadingWhiteSpace, boolean trailingWhiteSpace,
			final long sampleCount, final long nullCount, final long blankCount, final long leadingZeroCount, final double confidence,
			final String minValue, final String maxValue, final int minLength, final int maxLength,
			final String sum, final Map<String, Integer> cardinality, final Map<String, Integer> outliers, final boolean key) {
		this.name = name;
		this.matchCount = matchCount;
		this.patternInfo = patternInfo;
		this.leadingWhiteSpace = leadingWhiteSpace;
		this.trailingWhiteSpace = trailingWhiteSpace;
		this.sampleCount = sampleCount;
		this.nullCount = nullCount;
		this.blankCount = blankCount;
		this.leadingZeroCount = leadingZeroCount;
		this.confidence = confidence;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.minLength = minLength;
		this.maxLength = maxLength;
		this.sum = sum;
		this.cardinality = cardinality;
		this.outliers = outliers;
		this.key = key;
	}

	/**
	 * Name of the data stream being analyzed.
	 * @return Name of data stream..
	 */
	public String getName() {
		return name;
	}

	/**
	 * Confidence in the type classification.
	 * @return Confidence as a percentage.
	 */
	public double getConfidence() {
		return confidence;
	}

	/**
	 * Get 'Type' as determined by training to date.
	 * @return The Type of the data stream.
	 */
	public PatternInfo.Type getType() {
		return patternInfo.type;
	}

	/**
	 * Get the optional Type Qualifier.  Possible qualifiers are:
	 * <ul>
	 *  <li>Type: STRING - "BLANK", "BLANKORNULL", "CA_PROVINCE", "COUNTRY", "EMAIL", "MONTHABBR", "ADDRESS", "NA_STATE", "NULL", "US_STATE", "URL", "ZIP"</li>
	 *  <li>Type: LONG - "SIGNED"</li>
	 * 	<li>Type: DOUBLE - "SIGNED"</li>
	 * 	<li>Type: DATE, TIME, DATETIME, ZONEDDATETIME, OFFSETDATETIME - the detailed date format string</li>
	 * </ul>
	 * @return The Type Qualifier for the Type.
	 */
	public String getTypeQualifier() {
		return patternInfo.typeQualifier;
	}

	/**
	 * Get the minimum value for Numeric, Boolean and String types.
	 * @return The minimum value as a String.
	 */
	public String getMinValue() {
		return minValue;
	}

	/**
	 * Get the maximum value for Numeric, Boolean and String.
	 * @return The maximum value as a String.
	 */
	public String getMaxValue() {
		return maxValue;
	}

	/**
	 * Get the minimum length for Numeric, Boolean and String.
	 * Note: For String and Boolean types this length includes any whitespace.
	 * @return The minimum length.
	 */
	public int getMinLength() {
		return minLength;
	}

	/**
	 * Get the maximum length for Numeric, Boolean and String.
	 * Note: For String and Boolean types this length includes any whitespace.
	 * @return The maximum length.
	 */
	public int getMaxLength() {
		return maxLength;
	}

	/**
	 * Get the sum for Numeric types (Long, Double).
	 * @return The sum.
	 */
	public String getSum() {
		return sum;
	}

	/**
	 * Get Regular Expression that reflects the data stream.  All valid inputs should match this Regular Expression,
	 * however in some instances, not all inputs that match this RE are necessarily valid.  For example,
	 * 28/13/2017 will match the RE (\d{2}/\d{2}/\d{4}) however this is not a valid date with pattern dd/MM/yyyy (there
	 * is no 13th month).
	 * @return The Regular Expression.
	 */
	public String getRegExp() {
		String answer = patternInfo.regexp;

		if (!leadingWhiteSpace && !trailingWhiteSpace)
			return answer;

		if (TextAnalyzer.PATTERN_BOOLEAN.equals(answer) || TextAnalyzer.PATTERN_YESNO.equals(answer)) {
			if (leadingWhiteSpace)
				answer = "\\p{javaWhitespace}*" + answer;
			if (trailingWhiteSpace)
				answer += "\\p{javaWhitespace}*";
			return answer;
		}

		if (TextAnalyzer.PATTERN_ANY_VARIABLE.equals(answer))
			return answer;

		String pattern1 = null;
		String pattern2 = null;
		int orIndex = answer.indexOf('|');
		if (orIndex == -1) {
			pattern1 = answer.substring(0);
			pattern2 = null;
		}
		else {
			pattern1 = answer.substring(0, orIndex);
			pattern2 = answer.substring(orIndex + 1);
		}

		if (leadingWhiteSpace)
			pattern1 = "\\p{javaWhitespace}*" + pattern1;
		if (trailingWhiteSpace)
			pattern1 += "\\p{javaWhitespace}*";

		answer = pattern1;

		if (pattern2 != null) {
			if (leadingWhiteSpace)
				pattern2 = "\\p{javaWhitespace}*" + pattern2;
			if (trailingWhiteSpace)
				pattern2 += "\\p{javaWhitespace}*";

			answer += '|' + pattern2;
		}

		return answer;
	}

	/**
	 * Get the count of all samples that matched the determined type.
	 * @return Count of all matches.
	 */
	public long getMatchCount() {
		return matchCount;
	}

	/**
	 * Get the count of all samples seen.
	 * @return Count of all samples.
	 */
	public long getSampleCount() {
		return sampleCount;
	}

	/**
	 * Get the count of all null samples.
	 * @return Count of all null samples.
	 */
	public long getNullCount() {
		return nullCount;
	}

	/**
	 * Get the count of all blank samples.
	 * Note: any number (including zero) of spaces are Blank.
	 * @return Count of all blank samples.
	 */
	public long getBlankCount() {
		return blankCount;
	}

	/**
	 * Get the count of all samples with leading zeros (Type long only)
	 * @return Count of all leading zero samples.
	 */
	public long getLeadingZeroCount() {
		return leadingZeroCount;
	}

	/**
	 * Get the cardinality for the current data stream.
	 * See {@link com.cobber.fta.TextAnalyzer#setMaxCardinality(int) setMaxCardinality()} method in TextAnalyzer.
	 * Note: The cardinality returned is the cardinality of the 'matched' samples.
	 * Note: This is not a complete cardinality analysis unless the cardinality of the
	 * data stream is less than the maximum cardinality (Default: {@value com.cobber.fta.TextAnalyzer#MAX_CARDINALITY_DEFAULT}).
	 * See also {@link com.cobber.fta.TextAnalyzer#setMaxCardinality(int) setMaxCardinality()} method in TextAnalyzer.
	 * @return Count of all blank samples.
	 */
	public int getCardinality() {
		return cardinality.size();
	}

	/**
	 * Get the cardinality details for the current data stream.  This is a Map of Strings and the count
	 * of occurrences.
	 * @return A Map of values and their occurrence frequency of the data stream to date.
	 */
	public Map<String, Integer> getCardinalityDetails() {
		return cardinality;
	}

	/**
	 * Get the number of distinct outliers for the current data stream.
	 * See {@link com.cobber.fta.TextAnalyzer#setMaxOutliers(int) setMaxOutliers()} method in TextAnalyzer.
	 * Note: This is not a complete outlier analysis unless the outlier count of the
	 * data stream is less than the maximum outlier count (Default: {@value com.cobber.fta.TextAnalyzer#MAX_OUTLIERS_DEFAULT}).
	 * See also {@link com.cobber.fta.TextAnalyzer#setMaxOutliers(int) setMaxOutliers()} method in TextAnalyzer.
	 * @return Count of the distinct outliers.
	 */
	public int getOutlierCount() {
		return outliers.size();
	}

	/**
	 * Get the outlier details for the current data stream.  This is a Map of Strings and the count
	 * of occurrences.
	 * @return A Map of values and their occurrence frequency of the data stream to date.
	 */
	public Map<String, Integer> getOutlierDetails() {
		return outliers;
	}

	/**
	 * Is this field a possible key?
	 * @return True if the field could be a key field.
	 */
	public boolean isKey() {
		return key;
	}

	private static <K,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
	    final SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
	        new Comparator<Map.Entry<K,V>>() {
	            @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
	                int res = e2.getValue().compareTo(e1.getValue());
	                if (e1.getKey().equals(e2.getKey())) {
	                    return res;
	                } else {
	                    return res != 0 ? res : 1;
	                }
	            }
	        }
	    );
	    sortedEntries.addAll(map.entrySet());
	    return sortedEntries;
	}

	/**
	 * A String representation of the Analysis.  This is not suitable for anything other than
	 * debug output and is likely to change with no notice!!
	 * @return A String representation of the analysis to date.
	 */
	@Override
	public String toString() {
		return dump(false);
	}

	/**
	 * A String representation of the Analysis.  This is not suitable for anything other than
	 * debug output and is likely to change with no notice!!
	 * @param verbose If set, dump additional information related to cardinality and outliers.
	 * @return A String representation of the analysis to date.
	 */
	public String dump(final boolean verbose) {
		String ret = "TextAnalysisResult [name=" + name + ", matchCount=" + matchCount + ", sampleCount=" + sampleCount + ", nullCount="
				+ nullCount + ", blankCount=" + blankCount+ ", regexp=\"" + getRegExp() + "\", confidence=" + confidence +
				", type=" + patternInfo.type +
				(patternInfo.typeQualifier != null ? "(" + patternInfo.typeQualifier + ")" : "") + ", min=";
		if (minValue != null)
			ret += "\"" + minValue + "\"";
		else
			ret += "null";
		ret += ", max=";
		if (maxValue != null)
			ret += "\"" + maxValue + "\"";
		else
			ret += "null";
		ret += ", minLength=" + minLength;
		ret += ", maxLength=" + maxLength;
		ret += ", sum=";
		if (sum != null)
			ret += "\"" + sum + "\"";
		else
			ret += "null";
		ret += ", cardinality=" + (cardinality.size() < TextAnalyzer.MAX_CARDINALITY_DEFAULT ? String.valueOf(cardinality.size()) : "MAX");
		if (verbose && cardinality.size() != 0 && cardinality.size() < .2 * sampleCount && cardinality.size() < TextAnalyzer.MAX_CARDINALITY_DEFAULT) {
			ret += " {";
			int i = 0;
			final SortedSet<Map.Entry<String, Integer>> ordered = entriesSortedByValues(cardinality);
			for (final Map.Entry<String,Integer> entry : ordered) {
				if (i++ == 10) {
					ret += "...";
					break;
				}
				ret += "\"" + entry.getKey() + "\":" + entry.getValue();
				ret += " ";
			}
			ret += "}";
		}

		if (!outliers.isEmpty()) {
			ret += ", outliers=" + (outliers.size() < TextAnalyzer.MAX_OUTLIERS_DEFAULT ? String.valueOf(outliers.size()) : "MAX");
			if (verbose && !outliers.isEmpty() && outliers.size() < .2 * sampleCount) {
				ret += " {";
				int i = 0;
				final SortedSet<Map.Entry<String, Integer>> ordered = entriesSortedByValues(outliers);
				for (final Map.Entry<String,Integer> entry : ordered) {
					if (i++ == 10) {
						ret += "...";
						break;
					}
					ret += "\"" + entry.getKey() + "\":" + entry.getValue();
					ret += " ";
				}
				ret += "}";
			}
		}

		if (key)
			ret += ", PossibleKey";

		ret += "]";
		return ret;
	}
}
