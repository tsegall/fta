/*
 * Copyright 2017-2022 Tim Segall
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.Base64;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.Utils;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;
import com.cobber.fta.token.TokenStreams;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * TextAnalysisResult is the result of a {@link TextAnalyzer} analysis of a data stream.
 */
public class TextAnalysisResult {
	private enum SignatureTarget {
		/** Output only the fields we use to construct the Data Signature. */
		DATA_SIGNATURE,
		/** Output only the fields we use to construct the Structure Signature. */
		STRUCTURE_SIGNATURE,
		/** Output the fields we use to construct a user friendly description. */
		CONSUMER
	}

	private final static ObjectMapper MAPPER = new ObjectMapper();

	private static final String NOT_ENABLED = "Statistics not enabled.";

	private final String name;
	private final Facts facts;
	private final DateResolutionMode resolutionMode;
	private final AnalysisConfig analysisConfig;
	private final TokenStreams shape;

	/**
	 * @param name The name of the data stream being analyzed.
	 * @param facts Most of the relevant metrics for the current analysis.
	 * @param resolutionMode Determines what to do when the Date field is ambiguous (i.e. we cannot determine which
	 *   of the fields is the day or the month.  If resolutionMode is DayFirst, then assume day is first, if resolutionMode is
	 *   MonthFirst then assume month is first, if it is Auto then choose either DayFirst or MonthFirst based on the locale, if it
	 *   is None then the pattern returned will have '?' in to represent any ambiguity present.
	 * @param analysisConfig The Configuration of the current analysis.
	 * @param tokenStreams A shape analysis of the input stream.
	 */
	TextAnalysisResult(final String name, final Facts facts, final DateResolutionMode resolutionMode,
			final AnalysisConfig analysisConfig, final TokenStreams shape) {
		this.name = name;
		this.facts = facts;
		this.resolutionMode = resolutionMode;
		this.analysisConfig = analysisConfig;
		this.shape = shape;
	}

	/**
	 * Name of the data stream being analyzed.
	 * @return Name of data stream.
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
		return facts.confidence;
	}

	/**
	 * Get 'Type' as determined by training to date.
	 * @return The Type of the data stream.
	 */
	public FTAType getType() {
		return facts.matchPatternInfo.getBaseType();
	}

	/**
	 * Get the optional Type Qualifier.  Predefined qualifiers are:
	 * <ul>
	 *  <li>Type: BOOLEAN - "TRUE_FALSE", "YES_NO", "ONE_ZERO"</li>
	 *  <li>Type: STRING - "BLANK", "BLANKORNULL", "NULL"</li>
	 *  <li>Type: LONG - "GROUPING", "SIGNED", "SIGNED_TRAILING".  Note: "GROUPING" and "SIGNED" are independent and can both be present.</li>
	 * 	<li>Type: DOUBLE - "GROUPING", "SIGNED", "SIGNED_TRAILING", "NON_LOCALIZED".  Note: "GROUPING" and "SIGNED" are independent and can both be present.</li>
	 * 	<li>Type: DATE, TIME, DATETIME, ZONEDDATETIME, OFFSETDATETIME - The qualifier is the detailed date format string</li>
	 * </ul>
	 *
	 * Note: Boolean TRUE_FALSE is not localized, i.e. it will only be detected if the field contains true/false respectively.
	 * Note: Additional Type Qualifiers may be returned if any Logical Type plugins are installed.
	 * For example: If the Month Abbreviation plugin installed, the Base Type will be STRING, and the Qualifier will be "MONTHABBR".
	 * @return The Type Qualifier for the Type.
	 */
	public String getTypeQualifier() {
		return facts.matchPatternInfo.typeQualifier;
	}

	/**
	 * Is this a Logical Type?
	 *
	 * @return True if this is a Logical Type.
	 */
	public boolean isLogicalType() {
		return facts.matchPatternInfo.isLogicalType();
	}

	/**
	 * Get the minimum value for Numeric, Boolean and String types.
	 * @return The minimum value as a String.
	 */
	public String getMinValue() {
		if (!analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS))
			throw new IllegalArgumentException(NOT_ENABLED);
		return facts.getMinValue();
	}

	/**
	 * Get the maximum value for Numeric, Boolean and String.
	 * @return The maximum value as a String.
	 */
	public String getMaxValue() {
		if (!analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS))
			throw new IllegalArgumentException(NOT_ENABLED);
		return facts.getMaxValue();
	}

	/**
	 * Get the minimum length for Numeric, Boolean and String.
	 * Note: For String and Boolean types this length includes any whitespace.
	 * @return The minimum length.
	 */
	public int getMinLength() {
		return facts.minRawLength;
	}

	/**
	 * Get the maximum length for Numeric, Boolean and String.
	 * Note: For String and Boolean types this length includes any whitespace.
	 * @return The maximum length.
	 */
	public int getMaxLength() {
		return facts.maxRawLength;
	}

	/**
	 * Get the Decimal Separator used to interpret Doubles.
	 * Note: This will either be the Decimal Separator as per the locale or possibly a period.
	 * @return The Decimal Separator.
	 */
	public char getDecimalSeparator() {
		return facts.decimalSeparator;
	}

	/**
	 * Get the DateResolutionMode actually used to process Dates.
	 * @return The DateResolution mode used to process Dates.
	 */
	public DateResolutionMode getDateResolutionMode() {
		return resolutionMode;
	}

	/**
	 * Get the mean for Numeric types (Long, Double).
	 * @return The mean.
	 */
	public Double getMean() {
		if (!analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS))
			throw new IllegalArgumentException(NOT_ENABLED);

		return facts.matchPatternInfo.isNumeric() ? facts.mean : null;
	}

	/**
	 * Get the Standard Deviation for Numeric types (Long, Double).
	 * @return The Standard Deviation.
	 */
	public Double getStandardDeviation() {
		if (!analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS))
			throw new IllegalArgumentException(NOT_ENABLED);

		return facts.matchPatternInfo.isNumeric() ? Math.sqrt(facts.variance) : null;
	}

	/**
	 * Get the topK values.
	 * @return The top K values (default: 10).
	 */
	public Set<String> getTopK() {
		if (!analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS))
			throw new IllegalArgumentException(NOT_ENABLED);
		return facts.topK;
	}

	/**
	 * Get the bottomK values.
	 * @return The bottom K values (default: 10).
	 */
	public Set<String> getBottomK() {
		if (!analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS))
			throw new IllegalArgumentException(NOT_ENABLED);
		return facts.bottomK;
	}

	/**
	 * Get the Regular Expression that reflects the data stream.  All valid inputs should match this Regular Expression,
	 * however in some instances, not all inputs that match this RE are necessarily valid.  For example,
	 * 28/13/2017 will match the RE (\d{2}/\d{2}/\d{4}) however this is not a valid date with pattern dd/MM/yyyy (there
	 * is no 13th month).
	 * @return The Regular Expression.
	 */
	public String getRegExp() {
		if (facts.matchPatternInfo.isLogicalType() || (!facts.leadingWhiteSpace && !facts.trailingWhiteSpace))
			return facts.matchPatternInfo.regexp;

		// We need to add whitespace to the pattern but if there is alternation in the RE we need to be careful
		String answer = "";
		if (facts.leadingWhiteSpace)
			answer = KnownPatterns.PATTERN_WHITESPACE;
		final boolean optional = facts.matchPatternInfo.regexp.indexOf('|') != -1;
		if (optional)
			answer += "(";
		answer += facts.matchPatternInfo.regexp;
		if (optional)
			answer += ")";
		if (facts.trailingWhiteSpace)
			answer += KnownPatterns.PATTERN_WHITESPACE;

		return answer;
	}

	/**
	 * Get the Regular Expression that reflects the non-white space element in the data stream.
	 * For example, if a stream contains '  hello' and 'world  ' this would return '(?i)(HELLO|WORLD)'.
	 * @return The Regular Expression reflecting the non-white space data.
	 */
	public String getDataRegExp() {
		return facts.matchPatternInfo.regexp;
	}

	/**
	 * Get the count of all (non-blank/non-null) samples that matched the determined type.
	 * More formally the SampleCount is equal to the MatchCount + BlankCount + NullCount.
	 * @return Count of all matches.
	 */
	public long getMatchCount() {
		return facts.matchCount;
	}

	/**
	 * Does the set of elements contain any elements with leading White Space?
	 * @return True if any elements matched have leading White Space.
	 */
	public boolean getLeadingWhiteSpace() {
		return facts.leadingWhiteSpace;
	}

	/**
	 * Does the set of elements contain any elements with trailing White Space?
	 * @return True if any elements matched have trailing White Space.
	 */
	public boolean getTrailingWhiteSpace() {
		return facts.trailingWhiteSpace;
	}

	/**
	 * Does the set of elements contain any multi-line elements?
	 * @return True if any elements matched are multi-line.
	 */
	public boolean getMultiline() {
		return facts.multiline;
	}

	/**
	 * Get the total number of elements in the entire data stream (if known).
	 * @return total number of elements in the entire data stream (-1 if not known).
	 */
	public long getTotalCount() {
		return facts.totalCount;
	}

	/**
	 * Get the count of all null elements in the entire data stream (if known).
	 * Use {@link #getNullCount() getNullCount()} for the equivalent on the sample set.
	 * @return Count of all null elements in the entire data stream (-1 if not known).
	 */
	public long getTotalNullCount() {
		return facts.totalNullCount;
	}

	/**
	 * Get the count of all blank elements in the entire data stream (if known).
	 * Note: any number (including zero) of spaces are Blank.
	 * Use {@link #getBlankCount() getBlankCount()} for the equivalent on the sample set.
	 * @return Count of all blank samples in the entire data stream (-1 if not known).
	 */
	public long getTotalBlankCount() {
		return facts.totalBlankCount;
	}

	/**
	 * Get the mean for Numeric types (Long, Double) across the entire data stream (if known).
	 * Use {@link #getMean() getMean()} for the equivalent on the sample set.
	 * @return The mean across the entire data stream (null if not known).
	 */
	public Double getTotalMean() {
		return facts.totalMean;
	}

	/**
	 * Get the standard deviation for Numeric types (Long, Double) across the entire data stream (if known).
	 * Use {@link #getStandardDeviation() getStandardDeviation()} for the equivalent on the sample set.
	 * @return The Standard Deviation across the entire data stream (null if not known).
	 */
	public Double getTotalStandardDeviation() {
		return facts.totalStandardDeviation;
	}

	/**
	 * Get the minimum value for Numeric, Boolean and String types across the entire data stream (if known).
	 * Use {@link #getMinValue() getMinValue()} for the equivalent on the sample set.
	 * @return The minimum value as a String (null if not known).
	 */
	public String getTotalMinValue() {
		return facts.totalMinValue;
	}

	/**
	 * Get the maximum value for Numeric, Boolean and String across the entire data stream (if known).
	 * Use {@link #getMaxValue() getMaxValue()} for the equivalent on the sample set.
	 * @return The maximum value as a String (null if not known).
	 */
	public String getTotalMaxValue() {
		return facts.totalMaxValue;
	}

	/**
	 * Get the minimum length for Numeric, Boolean and String across the entire data stream (if known).
	 * Note: For String and Boolean types this length includes any whitespace.
	 * Use {@link #getMinLength() getMinLength()} for the equivalent on the sample set.
	 * @return The minimum length in the entire Data Stream (-1 if not known).
	 */
	public int getTotalMinLength() {
		return facts.totalMinLength;
	}

	/**
	 * Get the maximum length for Numeric, Boolean and String across the entire data stream (if known).
	 * Note: For String and Boolean types this length includes any whitespace.
	 * Use {@link #getMaxLength() getMaxLength()} for the equivalent on the sample set.
	 * @return The maximum length in the entire Data Stream (-1 if not known).
	 */
	public int getTotalMaxLength() {
		return facts.totalMaxLength;
	}

	/**
	 * Get the count of all samples observed.
	 * @return Count of all samples observed.
	 */
	public long getSampleCount() {
		return facts.sampleCount;
	}

	/**
	 * Get the count of all null samples.
	 * @return Count of all null samples.
	 */
	public long getNullCount() {
		return facts.nullCount;
	}

	/**
	 * Get the count of all blank samples.
	 * Note: any number (including zero) of spaces are Blank.
	 * @return Count of all blank samples.
	 */
	public long getBlankCount() {
		return facts.blankCount;
	}

	/**
	 * Get the count of all samples with leading zeros (Type long only).
	 * Note: a single '0' does not constitute a sample with a leading zero.
	 * @return Count of all leading zero samples.
	 */
	public long getLeadingZeroCount() {
		return facts.leadingZeroCount;
	}

	/**
	 * Get the cardinality for the current data stream.
	 * See {@link com.cobber.fta.TextAnalyzer#setMaxCardinality(int) setMaxCardinality()} method in TextAnalyzer.
	 * Note: The cardinality returned is the cardinality of the valid samples.  For example, if a date is invalid it will not
	 * be included in the cardinality.
	 * Note: This is not a complete cardinality analysis unless the cardinality of the
	 * data stream is less than the maximum cardinality (Default: {@value com.cobber.fta.AnalysisConfig#MAX_CARDINALITY_DEFAULT}).
	 * See also {@link com.cobber.fta.TextAnalyzer#setMaxCardinality(int) setMaxCardinality()} method in TextAnalyzer.
	 * @return Count of all blank samples.
	 */
	public int getCardinality() {
		return facts.cardinality.size();
	}

	/**
	 * Get the cardinality details for the current data stream.  This is a Map of Strings and the count
	 * of occurrences.
	 * @return A Map of values and their occurrence frequency of the data stream to date.
	 */
	public Map<String, Long> getCardinalityDetails() {
		return facts.cardinality;
	}

	/**
	 * Get the number of distinct outliers for the current data stream.
	 * See {@link com.cobber.fta.TextAnalyzer#setMaxOutliers(int) setMaxOutliers()} method in TextAnalyzer.
	 * Note: This is not a complete outlier analysis unless the outlier count of the
	 * data stream is less than the maximum outlier count (Default: {@value com.cobber.fta.AnalysisConfig#MAX_OUTLIERS_DEFAULT}).
	 * See also {@link com.cobber.fta.TextAnalyzer#setMaxOutliers(int) setMaxOutliers()} method in TextAnalyzer.
	 * @return Count of the distinct outliers.
	 */
	public int getOutlierCount() {
		return facts.outliers.size();
	}

	/**
	 * Get the outlier details for the current data stream.  This is a Map of Strings and the count
	 * of occurrences.
	 * @return A Map of values and their occurrence frequency of the data stream to date.
	 */
	public Map<String, Long> getOutlierDetails() {
		return facts.outliers;
	}

	/**
	 * Get the number of distinct shapes for the current data stream.
	 * Note: This is not a complete shape analysis unless the shape count of the
	 * data stream is less than the maximum shape count (Default: {@value com.cobber.fta.AnalysisConfig#MAX_SHAPES_DEFAULT}).
	 * @return Count of the distinct shapes.
	 */
	public int getShapeCount() {
		return shape.getShapes().size();
	}

	/**
	 * Get the shape details for the current data stream.  This is a Map of Strings and the count
	 * of occurrences.
	 * @return A Map of shapes and their occurrence frequency of the data stream to date.
	 */
	public Map<String, Long> getShapeDetails() {
		return shape.getShapes();
	}

	/**
	 * Is this field a key?
	 * @return A Double (0.0 ... 1.0) representing our confidence that this field is a key.
	 */
	public double getKeyConfidence() {
		return facts.keyConfidence;
	}

	/**
	 * How unique is this field, i.e. the number of elements in the set with a cardinality of one / cardinality.
	 * Note: Only supported if the cardinality presented is less than Max Cardinality.
	 * @return A Double (0.0 ... 1.0) representing the uniqueness of this field.
	 */
	public double getUniqueness() {
		return facts.uniqueness;
	}

	/**
	 * Return the distinct number of valid values in this stream.
	 * Note: Typically only supported if the cardinality presented is less than Max Cardinality.
	 * May be set by an external source.
	 * @return A long with the number of distinct values in this stream or -1 if unknown.
	 */
	public long getDistinctCount() {
		return facts.distinctCount;
	}

	/**
	 * Was statistics collection enabled for this analysis.
	 * @return True if statistics were collected.
	 */
	public boolean statisticsEnabled() {
		return analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS);
	}

	private static <K extends Comparable,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entriesSortedByValues(final Map<K,V> map) {
		final SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<>(
				new Comparator<Map.Entry<K,V>>() {
					@Override public int compare(final Map.Entry<K,V> e1, final Map.Entry<K,V> e2) {
						final int res = e2.getValue().compareTo(e1.getValue());
						if (e1.getKey().equals(e2.getKey()))
							return res;

						final int keyRes = e1.getKey().compareTo(e2.getKey());
						return res != 0 ? res : keyRes;
					}
				}
				);

		for (final Map.Entry<K, V> entry : map.entrySet())
			sortedEntries.add(new AbstractMap.SimpleImmutableEntry<K, V>(entry.getKey(), entry.getValue()));

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
	 * A SHA-1 hash that reflects the data stream structure.
	 * Note: If a Semantic type is detected then the SHA-1 hash will reflect this.
	 * @return A String SHA-1 hash that reflects the structure of the data stream.
	 */
	public String getStructureSignature() {
		String structureSignature = facts.matchPatternInfo.getBaseType().toString() + ":";

		if (isLogicalType())
			structureSignature += getTypeQualifier();
		else if (!shape.getShapes().isEmpty())
			structureSignature += getRegExp() + shape.getShapes().keySet().toString();

		MessageDigest md;
		try {

			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}

		final byte[] signature = structureSignature.getBytes(StandardCharsets.UTF_8);
		return Base64.getEncoder().encodeToString(md.digest(signature));
	}

	/**
	 * A SHA-1 hash that reflects the data stream contents.
	 * Note: The order of the data stream is not considered.
	 * @return A String SHA-1 hash that reflects the data stream contents.
	 */
	public String getDataSignature() {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}

		// Grab a JSON representation of the information required for the Data Signature
		final String dataSignature = internalAsJSON(false, 1, SignatureTarget.DATA_SIGNATURE);

		final byte[] signature = dataSignature.getBytes(StandardCharsets.UTF_8);
		return Base64.getEncoder().encodeToString(md.digest(signature));
	}

	private void outputArray(final ArrayNode detail, final Set<String> set) {
		for (final String s : set) {
			detail.add(s);
		}
	}

	private void outputDetails(final ObjectMapper mapper, final ArrayNode detail, final Map<String, Long> details, final int verbose) {
		int records = 0;
		for (final Map.Entry<String,Long> entry : entriesSortedByValues(details)) {
			records++;
			if (verbose == 1 && records > 100)
				break;
			final ObjectNode elt = mapper.createObjectNode();
			elt.put("key", entry.getKey());
			elt.put("count", entry.getValue());
			detail.add(elt);
		}
	}

	private String jsonError(final String message) {
		return "{ \"Error\": \"" + message + "\" }";
	}

	/**
	 * A plugin definition to use to match this type.
	 * @return A JSON representation of the analysis.
	 */
	public String asPlugin() {
		// Already a logical type or date type - so nothing interesting to report as a Plugin
		if (isLogicalType() || facts.matchPatternInfo.isDateType())
			return null;

		// Check to see if the Regular Expression is too boring to report - e.g. '.*' or '.{3,31}'
		final String regExp = getRegExp();
		if (regExp.charAt(0) == '.' && (regExp.length() == 2 || (regExp.length() > 1 && regExp.charAt(1) == '{')))
			return null;

		final ObjectWriter writer = MAPPER.writerWithDefaultPrettyPrinter();
		final ObjectNode plugin = MAPPER.createObjectNode();
		plugin.put("qualifier", name);
		ArrayNode arrayNode = MAPPER.createArrayNode();
		arrayNode.add(".*(?i)" + name);
		plugin.set("headerRegExps", arrayNode);
		arrayNode = MAPPER.createArrayNode();
		arrayNode.add(70);
		plugin.set("headerRegExpConfidence", arrayNode);
		arrayNode = MAPPER.createArrayNode();
		arrayNode.add(getRegExp());
		plugin.set("regExpsToMatch", arrayNode);
		plugin.put("regExpReturned", regExp);

		if (statisticsEnabled() && facts.matchCount > 100 && (facts.cardinality.size()*100)/facts.matchCount < 20 && !regExp.startsWith("(?i)(")) {
			if (facts.getMinValue() != null)
				plugin.put("minimum", facts.getMinValue());
			if (facts.getMaxValue() != null)
				plugin.put("maximum", facts.getMaxValue());
		}

		plugin.put("baseType", facts.matchPatternInfo.getBaseType().toString());

		try {
			return writer.writeValueAsString(plugin);
		} catch (JsonProcessingException e) {
			return jsonError(e.getMessage());
		}
	}

	/**
	 * A JSON representation of the Analysis.
	 * @param pretty If set, add minimal whitespace formatting.
	 * @param verbose If &gt; 0 provides additional details on the core, Outlier, and Shapes sets.  A value of 1
	 * will output the first 100 elements, a value &gt; 1 will output the full set.
	 * @return A JSON representation of the analysis.
	 */
	public String asJSON(final boolean pretty, final int verbose) {
		return internalAsJSON(pretty, verbose, SignatureTarget.CONSUMER);
	}

	private String internalAsJSON(final boolean pretty, final int verbose, final SignatureTarget target) {
		final ObjectWriter writer = pretty ? MAPPER.writerWithDefaultPrettyPrinter() : MAPPER.writer();

		final ObjectNode analysis = MAPPER.createObjectNode();
		if (target != SignatureTarget.STRUCTURE_SIGNATURE && target != SignatureTarget.DATA_SIGNATURE)
			analysis.put("fieldName", name);
		analysis.put("totalCount", facts.totalCount);
		analysis.put("sampleCount", facts.sampleCount);
		analysis.put("matchCount", facts.matchCount);
		analysis.put("nullCount", facts.nullCount);
		analysis.put("blankCount", facts.blankCount);
		analysis.put("distinctCount", facts.distinctCount);
		if (target != SignatureTarget.DATA_SIGNATURE) {
			analysis.put("regExp", getRegExp());
			//			final ArrayNode regExpStream = analysis.putArray("regExpStream");
			//			final Set<String> streamRegExps = new HashSet<>();
			//			streamRegExps.add(shape.getRegExp(false));
			//			// Only bother to return a fitted result of we have a reasonable number of samples
			//			if (shape.getSamples() > 100)
			//				streamRegExps.add(shape.getRegExp(true));
			//			streamRegExps.remove(getRegExp());
			//			if (!streamRegExps.isEmpty())
			//				outputArray(regExpStream, streamRegExps);
			analysis.put("confidence", facts.confidence);
			analysis.put("type", facts.matchPatternInfo.getBaseType().toString());

			if (facts.matchPatternInfo.typeQualifier != null)
				analysis.put("typeQualifier", facts.matchPatternInfo.typeQualifier);
			if (analysisConfig.isEnabled(TextAnalyzer.Feature.FORMAT_DETECTION))
				analysis.put("contentFormat", facts.streamFormat);
		}

		if (FTAType.DOUBLE == facts.matchPatternInfo.getBaseType())
			analysis.put("decimalSeparator", String.valueOf(facts.decimalSeparator));

		if (statisticsEnabled()) {
			if (facts.getMinValue() != null)
				analysis.put("min", facts.getMinValue());
			if (facts.getMaxValue() != null)
				analysis.put("max", facts.getMaxValue());
		}

		analysis.put("minLength", facts.minRawLength);
		analysis.put("maxLength", facts.maxRawLength);

		if (statisticsEnabled()) {
			if (facts.matchPatternInfo.isNumeric())
				analysis.put("mean", facts.mean);
			if (facts.matchPatternInfo.isNumeric())
				analysis.put("standardDeviation", getStandardDeviation());
			if (facts.topK != null) {
				final ArrayNode detail = analysis.putArray("topK");
				outputArray(detail, facts.topK);
			}
			if (facts.bottomK != null) {
				final ArrayNode detail = analysis.putArray("bottomK");
				outputArray(detail, facts.bottomK);
			}
		}

		if (facts.matchPatternInfo.isNumeric())
			analysis.put("leadingZeroCount", getLeadingZeroCount());

		analysis.put("cardinality", facts.cardinality.size() < analysisConfig.getMaxCardinality() ? facts.cardinality.size() : -1);

		if (!facts.cardinality.isEmpty() && verbose > 0) {
			final ArrayNode detail = analysis.putArray("cardinalityDetail");
			outputDetails(MAPPER, detail, facts.cardinality, verbose);
		}

		analysis.put("outlierCardinality", facts.outliers.size() < analysisConfig.getMaxOutliers() ? facts.outliers.size() : -1);
		if (!facts.outliers.isEmpty() && verbose > 0) {
			final ArrayNode detail = analysis.putArray("outlierDetail");
			outputDetails(MAPPER, detail, facts.outliers, verbose);
		}

		analysis.put("shapesCardinality", (shape.getShapes().size() > 0 && shape.getShapes().size() < analysisConfig.getMaxShapes()) ? shape.getShapes().size() : -1);
		if (!shape.getShapes().isEmpty() && verbose > 0) {
			final ArrayNode detail = analysis.putArray("shapesDetail");
			outputDetails(MAPPER, detail, shape.getShapes(), verbose);
		}

		analysis.put("leadingWhiteSpace", getLeadingWhiteSpace());
		analysis.put("trailingWhiteSpace", getTrailingWhiteSpace());
		analysis.put("multiline", getMultiline());

		if (facts.totalCount != -1 && facts.totalCount != facts.sampleCount) {
			analysis.put("totalNullCount", facts.totalNullCount);
			analysis.put("totalBlankCount", facts.totalBlankCount);
			if (facts.totalMean != null)
				analysis.put("toalMean", facts.totalMean);
			if (facts.totalStandardDeviation != null)
				analysis.put("totalStandardDeviation", facts.totalStandardDeviation);
			if (facts.totalMinValue != null)
				analysis.put("totalMin", facts.totalMinValue);
			if (facts.totalMaxValue != null)
				analysis.put("totalMax", facts.totalMaxValue);
			analysis.put("totalMinLength", facts.totalMinLength);
			analysis.put("totalMaxLength", facts.totalMaxLength);
		}

		if (facts.matchPatternInfo.isDateType())
			analysis.put("dateResolutionMode", getDateResolutionMode().toString());

		if (target != SignatureTarget.DATA_SIGNATURE) {
			analysis.put("logicalType", isLogicalType());
			analysis.put("keyConfidence", facts.keyConfidence);
			analysis.put("uniqueness", facts.uniqueness);
			analysis.put("detectionLocale", facts.getLocale().toLanguageTag());
			analysis.put("ftaVersion", Utils.getVersion());

			String signature = getStructureSignature();
			if (signature != null)
				analysis.put("structureSignature", signature);

			signature = getDataSignature();
			if (signature != null)
				analysis.put("dataSignature", signature);
		}

		try {
			return writer.writeValueAsString(analysis);
		} catch (JsonProcessingException e) {
			return jsonError(e.getMessage());
		}
	}
}
