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

import com.cobber.fta.DateTimeParser.DateResolutionMode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
	private final String sum;
	private final char decimalSeparator;
	private final DateResolutionMode resolutionMode;
	private final Map<String, Long> cardinality;
	private final int maxCardinality;
	private final Map<String, Long> outliers;
	private final int maxOutliers;
	private final Map<String, Long> shapes;
	private final int maxShapes;
	private final boolean key;
	private final boolean collectStatistics;

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
	 * @param sum A String representation of the sum of all values seen.  Only relevant for numeric types.
	 * @param decimalSeparator Get the Decimal separator used to interpret this field (only relevant for type double.
	 * @param resolutionMode Determines what to do when the Date field is ambiguous (i.e. we cannot determine which
	 *   of the fields is the day or the month.  If resolutionMode is DayFirst, then assume day is first, if resolutionMode is
	 *   MonthFirst then assume month is first, if it is Auto then choose either DayFirst or MonthFirst based on the locale, if it
	 *   is None then the pattern returned will have '?' in to represent any ambiguity present.
	 * @param cardinality A map of valid (matching) input values and the count of occurrences of the those input values.
	 * @param outliers A map of invalid input values and the count of occurrences of the those input values.
	 * @param shapes A map of input shapes and the count of occurrences of the these shapes.
	 * @param key Do we think this field is a key.
	 * @param collectStatistics Were statistics collected during this analysis.
	 */
	TextAnalysisResult(final String name, final long matchCount, final PatternInfo patternInfo, final boolean leadingWhiteSpace, boolean trailingWhiteSpace,
			boolean multiline, final long sampleCount, final long nullCount, final long blankCount, final long leadingZeroCount,
			final double confidence, final String minValue, final String maxValue, final int minLength, final int maxLength,
			final String sum, char decimalSeparator, DateResolutionMode resolutionMode,
			final Map<String, Long> cardinality, int maxCardinality,
			final Map<String, Long> outliers, int maxOutliers,
			final Map<String, Long> shapes, int maxShapes,
			final boolean key, boolean collectStatistics) {
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
		this.sum = sum;
		this.decimalSeparator = decimalSeparator;
		this.resolutionMode = resolutionMode;
		this.cardinality = cardinality;
		this.maxCardinality = maxCardinality;
		this.outliers = outliers;
		this.maxOutliers = maxOutliers;
		this.shapes = shapes;
		this.maxShapes = maxShapes;
		this.key = key;
		this.collectStatistics = collectStatistics;
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
		if (!collectStatistics)
			throw new IllegalArgumentException("Statistics not enabled.");
		return minValue;
	}

	/**
	 * Get the maximum value for Numeric, Boolean and String.
	 * @return The maximum value as a String.
	 */
	public String getMaxValue() {
		if (!collectStatistics)
			throw new IllegalArgumentException("Statistics not enabled.");
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
	 * Get the DateResolutionMode actually used to process Dates.
	 * @return The DateResolution mode used to process Dates.
	 */
	public DateResolutionMode getDateResolutionMode() {
		return resolutionMode;
	}

	/**
	 * Get the sum for Numeric types (Long, Double).
	 * @return The sum.
	 */
	public String getSum() {
		if (!collectStatistics)
			throw new IllegalArgumentException("Statistics not enabled.");
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
	 * Get the count of all samples with leading zeros (Type long only).
	 * Note: a single '0' does not constitute a sample with a leading zero.
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
	public Map<String, Long> getCardinalityDetails() {
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
	public Map<String, Long> getOutlierDetails() {
		return outliers;
	}

	/**
	 * Get the number of distinct shapes for the current data stream.
	 * Note: This is not a complete shape analysis unless the shape count of the
	 * data stream is less than the maximum shape count (Default: {@value com.cobber.fta.TextAnalyzer#MAX_SHAPES_DEFAULT}).
	 * @return Count of the distinct shapes.
	 */
	public int getShapeCount() {
		return shapes.size();
	}

	/**
	 * Get the shape details for the current data stream.  This is a Map of Strings and the count
	 * of occurrences.
	 * @return A Map of shapes and their occurrence frequency of the data stream to date.
	 */
	public Map<String, Long> getShapeDetails() {
		return shapes;
	}

	/**
	 * Is this field a possible key?
	 * @return True if the field could be a key field.
	 */
	public boolean isKey() {
		return key;
	}

	/**
	 * Was statistics collection enabled for this analysis.
	 * @return True if statistics were collected.
	 */
	public boolean statisticsEnabled() {
		return collectStatistics;
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
		return asJSON(false, 0);
	}

	/**
	 * A JSON representation of the Analysis.
	 * @param pretty If set, add minimal whitespace formatting.
	 * @param verbose If set provides additional details on the core and Outlier sets.
	 * @return A JSON representation of the analysis.
	 */
	public String asJSON(boolean pretty, int verbose) {

		ObjectMapper mapper = new ObjectMapper();

		ObjectWriter writer = pretty ? mapper.writerWithDefaultPrettyPrinter() : mapper.writer();

		ObjectNode analysis = mapper.createObjectNode();
		analysis.put("fieldName", name);
		analysis.put("sampleCount", sampleCount);
		analysis.put("matchCount", matchCount);
		analysis.put("nullCount", nullCount);
		analysis.put("blankCount", blankCount);
		analysis.put("regExp", getRegExp());
		analysis.put("confidence", confidence);
		analysis.put("type", patternInfo.type.toString());

		if (patternInfo.typeQualifier != null)
			analysis.put("typeQualifier", patternInfo.typeQualifier);
		if (PatternInfo.Type.DOUBLE == patternInfo.type)
			analysis.put("decimalSeparator", String.valueOf(decimalSeparator));
		if (statisticsEnabled() && minValue != null)
			analysis.put("min", minValue);
		if (statisticsEnabled() && maxValue != null)
			analysis.put("max", maxValue);
			analysis.put("minLength", minLength);
			analysis.put("maxLength", maxLength);
		if (statisticsEnabled() && sum != null)
			analysis.put("sum", sum);
		if (patternInfo.isNumeric())
			analysis.put("leadingZeroCount", getLeadingZeroCount());

		analysis.put("cardinality", cardinality.size() < maxCardinality ? String.valueOf(cardinality.size()) : "MAX");

		if (!cardinality.isEmpty() && (verbose > 1 ||
				(verbose == 1 && cardinality.size() < .2 * sampleCount && cardinality.size() < TextAnalyzer.MAX_CARDINALITY_DEFAULT))) {
			ArrayNode detail = analysis.putArray("cardinalityDetail");
			for (final Map.Entry<String,Long> entry : entriesSortedByValues(cardinality)) {
				ObjectNode elt = mapper.createObjectNode();
				elt.put("key", entry.getKey());
				elt.put("count", entry.getValue());
				detail.add(elt);
			}
		}

		analysis.put("outlierCardinality", outliers.size() < maxOutliers ? String.valueOf(outliers.size()) : "MAX");
 		if (!outliers.isEmpty()  && (verbose > 1 || (verbose == 1 && outliers.size() < .2 * sampleCount))) {
			ArrayNode detail = analysis.putArray("outlierDetail");
			for (final Map.Entry<String, Long> entry : entriesSortedByValues(outliers)) {
				ObjectNode elt = mapper.createObjectNode();
				elt.put("key", entry.getKey());
				elt.put("count", entry.getValue());
				detail.add(elt);
			}
		}

		analysis.put("shapesCardinality", shapes.size() < maxShapes ? String.valueOf(shapes.size()) : "MAX");
		if (!shapes.isEmpty() && verbose >= 1) {
			ArrayNode detail = analysis.putArray("shapesDetail");
			for (final Map.Entry<String, Long> entry : entriesSortedByValues(shapes)) {
				ObjectNode elt = mapper.createObjectNode();
				elt.put("key", entry.getKey());
				elt.put("count", entry.getValue());
				detail.add(elt);
			}
		}

		analysis.put("leadingWhiteSpace", getLeadingWhiteSpace());
		analysis.put("trailingWhiteSpace", getTrailingWhiteSpace());
		analysis.put("multiline", getMultiline());

		if (patternInfo.isDateType())
			analysis.put("dateResolutionMode", getDateResolutionMode().toString());
		analysis.put("logicalType", isLogicalType());
		analysis.put("possibleKey", key);

		try {
			return writer.writeValueAsString(analysis);
		} catch (JsonProcessingException e) {
			return "Internal Error";
		}
	}
}
