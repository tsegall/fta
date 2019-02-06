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
	private final boolean multiline;
	private final double confidence;
	private final String minValue;
	private final String maxValue;
	private final int minLength;
	private final int maxLength;
	private final char decimalSeparator;
	private final String sum;
	private final Map<String, Integer> cardinality;
	private final Map<String, Integer> outliers;
	private final boolean key;

	/**
	 * @param name The name of the data stream being analyzed.
	 * @param matchCount The number of samples that match the patternInfo.
	 * @param patternInfo The PatternInfo associated with this matchCount.
	 * @param leadingWhiteSpace Do any elements have leading White Space?
	 * @param trailingWhiteSpace Do any elements have trailing White Space?
	 * @param multiline Are any elements multi-line?
	 * @param sampleCount The total number of samples seen.
	 * @param nullCount The number of nulls seen in the sample set.
	 * @param blankCount The number of blanks seen in the sample set.
	 * @param leadingZeroCount The number of leading zeros seen in sample set.  Only relevant for type Long.
	 * @param confidence The percentage confidence in the analysis.  The matchCount divided by the sampleCount.
	 * @param minValue A String representation of the minimum value.  Only relevant for Numeric/String types.
	 * @param maxValue A String representation of the maximum value.  Only relevant for Numeric/String types.
	 * @param minLength Get the minimum length. Only relevant for Numeric, Boolean and String. Note: For String and Boolean types this length includes any whitespace.
	 * @param maxLength Get the maximum length. Only relevant for Numeric, Boolean and String. Note: For String and Boolean types this length includes any whitespace.
	 * @param decimalSeparator Get the Decimal separator used to interpret this field (only relevant for type double.
	 * @param sum A String representation of the sum of all values seen.  Only relevant for numeric types.
	 * @param cardinality A map of valid (matching) input values and the count of occurrences of the those input values.
	 * @param outliers A map of invalid input values and the count of occurrences of the those input values.
	 * @param key Do we think this field is a key.
	 */
	TextAnalysisResult(final String name, final long matchCount, final PatternInfo patternInfo, final boolean leadingWhiteSpace, boolean trailingWhiteSpace,
			boolean multiline, final long sampleCount, final long nullCount, final long blankCount, final long leadingZeroCount,
			final double confidence, final String minValue, final String maxValue, final int minLength, final int maxLength,
			char decimalSeparator, final String sum, final Map<String, Integer> cardinality, final Map<String, Integer> outliers, final boolean key) {
		this.name = name;
		this.matchCount = matchCount;
		this.patternInfo = patternInfo;
		this.leadingWhiteSpace = leadingWhiteSpace;
		this.trailingWhiteSpace = trailingWhiteSpace;
		this.multiline = multiline;
		this.sampleCount = sampleCount;
		this.nullCount = nullCount;
		this.blankCount = blankCount;
		this.leadingZeroCount = leadingZeroCount;
		this.confidence = confidence;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.minLength = minLength;
		this.maxLength = maxLength;
		this.decimalSeparator = decimalSeparator;
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
	 * Typically this will be the number of matches divided by the number of real samples.
	 * Where a real sample does not include either nulls or blanks.
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
	 * Get the optional Type Qualifier.  Predefined qualifiers are:
	 * <ul>
	 *  <li>Type: BOOLEAN - "TRUE_FALSE", "YES_NO", "ONE_ZERO"</li>
	 *  <li>Type: STRING - "BLANK", "BLANKORNULL", "NULL"</li>
	 *  <li>Type: LONG - "GROUPING", "SIGNED".  Note: "GROUPING" and "SIGNED" are independent and can both be present.</li>
	 * 	<li>Type: DOUBLE - "GROUPING", "SIGNED".  Note: "GROUPING" and "SIGNED" are independent and can both be present.</li>
	 * 	<li>Type: DATE, TIME, DATETIME, ZONEDDATETIME, OFFSETDATETIME - The qualifier is the detailed date format string</li>
	 * </ul>
	 *
	 * Note: Boolean TRUE_FALSE and YES_NO are not localized, i.e. these will only be detected if the field contains
	 * yes/no and true/false respectively.
	 * Note: Additional Type Qualifiers may be returned if any Logical Type plugins are installed.
	 * For example: If the Month Abbreviation plugin installed, the Base Type will be STRING, and the Qualifier will be "MONTHABBR".
	 * @return The Type Qualifier for the Type.
	 */
	public String getTypeQualifier() {
		return patternInfo.typeQualifier;
	}

	/**
	 * Is this a Logical Type?
	 *
	 * @return True if this is a Logical Type.
	 */
	public boolean isLogicalType() {
		return patternInfo.isLogicalType();
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
	 * Get the Decimal Separator used to interpret Doubles.
	 * Note: This will either be the Decimal Separator as per the locale or possibly a period.
	 * @return The Decimal Separator.
	 */
	public char getDecimalSeparator() {
		return decimalSeparator;
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
		if (patternInfo.isLogicalType || (!leadingWhiteSpace && !trailingWhiteSpace))
			return patternInfo.regexp;

		// We need to add whitespace to the pattern but if there is alternation in the RE we need to be careful
		String answer = "";
		if (leadingWhiteSpace)
			answer = "\\p{javaWhitespace}*";
		boolean optional = patternInfo.regexp.indexOf('|') != -1;
		if (optional)
			answer += "(";
		answer += patternInfo.regexp;
		if (optional)
			answer += ")";
		if (trailingWhiteSpace)
			answer += "\\p{javaWhitespace}*";

		return answer;
	}

	/**
	 * Get the count of all (non-blank/non-null) samples that matched the determined type.
	 * More formally the SampleCount is equal to the MatchCount + BlankCount + NullCount.
	 * @return Count of all matches.
	 */
	public long getMatchCount() {
		return matchCount;
	}

	/**
	 * Does the set of elements contain any elements with leading White Space?
	 * @return True if any elements matched have leading White Space.
	 */
	public boolean getLeadingWhiteSpace() {
		return leadingWhiteSpace;
	}

	/**
	 * Does the set of elements contain any elements with trailing White Space?
	 * @return True if any elements matched have trailing White Space.
	 */
	public boolean getTrailingWhiteSpace() {
		return trailingWhiteSpace;
	}

	/**
	 * Does the set of elements contain any multi-line elements?
	 * @return True if any elements matched are multi-line.
	 */
	public boolean getMultiline() {
		return multiline;
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
	 * Note: The cardinality returned is the cardinality of the valid samples.  For example, if a date is invalid it will not
	 * be included in the cardinality.
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

	private static <K,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entriesSortedByValues(final Map<K,V> map) {
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
	 * A String representation of the Analysis.
	 * @return A String representation of the analysis to date.
	 */
	@Override
	public String toString() {
		return asJSON(false, false);
	}

	private String Q(String input) {
		StringBuilder ret = new StringBuilder();
		ret.append('"');
		for (int i = 0; i < input.length(); i++) {
			char ch = input.charAt(i);
			if (ch == '\\' || ch == '"') {
				ret.append('\\');
				ret.append(ch);
			}
			else if (ch == '\t')
				ret.append("\\t");
			else if (ch == '\n')
				ret.append("\\n");
			else if (ch == '\r')
				ret.append("\\r");
			else
				ret.append(ch);
		}
		return ret.append('"').toString();
	}

	private String K(String input) {
		return Q(input) + ": ";
	}

	/**
	 * A JSON representation of the Analysis.
	 * @param pretty If set, add minimal whitespace formatting.
	 * @param verbose If set, return additional information related to cardinality and outliers.
	 * @return A JSON representation of the analysis.
	 */
	public String asJSON(boolean pretty, boolean verbose) {
		StringBuilder ret = new StringBuilder();
		String eol = pretty ? "\n" : " ";
		String indent = pretty ? "\t" : "";
		String newField = pretty ? ",\n\t" : ", ";

		ret.append("{").append(eol).append(indent);

		ret.append(K("fieldName")).append(Q(name)).append(newField);
		ret.append(K("sampleCount")).append(sampleCount).append(newField);
		ret.append(K("matchCount")).append(matchCount).append(newField);
		ret.append(K("nullCount")).append(nullCount).append(newField);
		ret.append(K("blankCount")).append(blankCount).append(newField);
		ret.append(K("regExp")).append(Q(getRegExp())).append(newField);
		ret.append(K("confidence")).append(confidence).append(newField);
		ret.append(K("type")).append(Q(patternInfo.type.toString())).append(newField);
		if (patternInfo.typeQualifier != null)
			ret.append(K("typeQualifier")).append(Q(patternInfo.typeQualifier)).append(newField);
		if (PatternInfo.Type.DOUBLE == patternInfo.type)
			ret.append(K("decimalSeparator")).append(Q(String.valueOf(decimalSeparator))).append(newField);
		if (minValue != null)
			ret.append(K("min")).append(Q(minValue)).append(newField);
		if (maxValue != null)
			ret.append(K("max")).append(Q(maxValue)).append(newField);
		ret.append(K("minLength")).append(minLength).append(newField);
		ret.append(K("maxLength")).append(maxLength).append(newField);
		if (sum != null)
			ret.append(K("sum")).append(Q(sum)).append(newField);

		ret.append(K("cardinality")).append(Q(cardinality.size() < TextAnalyzer.MAX_CARDINALITY_DEFAULT ? String.valueOf(cardinality.size()) : "MAX")).append(newField);
		if (verbose && cardinality.size() != 0 && cardinality.size() < .2 * sampleCount && cardinality.size() < TextAnalyzer.MAX_CARDINALITY_DEFAULT) {
			ret.append(K("cardinalityDetail")).append('[').append(eol).append(indent).append(indent);
			final SortedSet<Map.Entry<String, Integer>> ordered = entriesSortedByValues(cardinality);
			int i = ordered.size();
			for (final Map.Entry<String,Integer> entry : ordered) {
				ret.append("{ ").append(K("key")).append(Q(entry.getKey())).append(", ").append(K("count")).append(entry.getValue()).append(" }");
				if (i-- > 1)
					ret.append(newField).append(indent);
				else
					ret.append(eol);
			}
			ret.append(indent).append(']').append(newField);
		}

		ret.append(K("outliers")).append(Q(outliers.size() < TextAnalyzer.MAX_OUTLIERS_DEFAULT ? String.valueOf(outliers.size()) : "MAX")).append(newField);
		if (verbose && !outliers.isEmpty() && outliers.size() < .2 * sampleCount) {
			ret.append(K("outlierDetail")).append('[').append(eol).append(indent).append(indent);
			final SortedSet<Map.Entry<String, Integer>> ordered = entriesSortedByValues(outliers);
			int i = ordered.size();
			for (final Map.Entry<String,Integer> entry : ordered) {
				ret.append("{ ").append(K("key")).append(Q(entry.getKey())).append(", ").append(K("count")).append(entry.getValue()).append(" }");
				if (i-- > 1)
					ret.append(newField).append(indent);
				else
					ret.append(eol);
			}
			ret.append(indent).append(']').append(newField);
		}

		ret.append(K("leadingWhiteSpace")).append(getLeadingWhiteSpace()).append(newField);
		ret.append(K("trailingWhiteSpace")).append(getTrailingWhiteSpace()).append(newField);
		ret.append(K("multiline")).append(getMultiline()).append(newField);

		ret.append(K("logicalType")).append(isLogicalType()).append(newField);
		ret.append(K("possibleKey")).append(key).append(eol);

		ret.append("}");

		return ret.toString();
	}
}
