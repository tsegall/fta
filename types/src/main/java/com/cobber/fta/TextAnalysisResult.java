/*
 * Copyright 2017-2023 Tim Segall
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
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
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
	 * Get the configuration associated with this TextAnalysisResult.
	 *
	 * @return The AnalysisConfig  of the TextAnalysisResult.
	 */
	public AnalysisConfig getConfig() {
		return analysisConfig;
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
		return facts.getMatchTypeInfo().getBaseType();
	}

	/**
	 * Get the optional Type Modifier (which modifies the Base Type - see {@link#getType}  Predefined qualifiers are:
	 * <ul>
	 *  <li>Type: BOOLEAN - "TRUE_FALSE", "YES_NO", "Y_N", "ONE_ZERO"</li>
	 *  <li>Type: STRING - "BLANK", "BLANKORNULL", "NULL"</li>
	 *  <li>Type: LONG - "GROUPING", "SIGNED", "SIGNED_TRAILING".  Note: "GROUPING" and "SIGNED" are independent and can both be present.</li>
	 * 	<li>Type: DOUBLE - "GROUPING", "SIGNED", "SIGNED_TRAILING", "NON_LOCALIZED".  Note: "GROUPING" and "SIGNED" are independent and can both be present.</li>
	 * 	<li>Type: DATE, TIME, DATETIME, ZONEDDATETIME, OFFSETDATETIME - The qualifier is the detailed date format string</li>
	 * </ul>
	 *
	 * Note: Boolean TRUE_FALSE is not localized, i.e. it will only be detected if the field contains true/false respectively.
	 * @return The Type Modifier for the Type.
	 */
	public String getTypeModifier() {
		return facts.getMatchTypeInfo().typeModifier;
	}

	/**
	 * Is this a Semantic Type?
	 *
	 * @return True if this is a Semantic Type.
	 */
	public boolean isSemanticType() {
		return facts.getMatchTypeInfo().isSemanticType();
	}

	/**
	 * The Semantic Type detected.
	 *
	 * Note: The Semantic Types detected are based on the set of plugins are installed.
	 * For example: If the Month Abbreviation plugin installed, the Base Type will be STRING, and the Semantic Type will be "MONTHABBR".
	 *
	 * @return The Semantic Type detected - only valid if isSemanticType() is true.
	 */
	public String getSemanticType() {
		return facts.getMatchTypeInfo().getSemanticType();
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

		return facts.getMatchTypeInfo().isNumeric() ? facts.mean : null;
	}

	/**
	 * Get the Standard Deviation for Numeric types (Long, Double).
	 * @return The Standard Deviation.
	 */
	public Double getStandardDeviation() {
		if (!analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS))
			throw new IllegalArgumentException(NOT_ENABLED);

		return facts.getMatchTypeInfo().isNumeric() ? Math.sqrt(facts.variance) : null;
	}

	/**
	 * Get the value at the requested quantile.
	 * @param quantile a double between 0.0 and 1.0 (both included)
	 * @return the value at the specified quantile
	 */
	public String getValueAtQuantile(final double quantile) {
		if (!analysisConfig.isEnabled(TextAnalyzer.Feature.DISTRIBUTIONS))
			throw new IllegalArgumentException(NOT_ENABLED);

		if (!facts.getSketch().isComplete())
			facts.getSketch().complete(facts.cardinality);

		return facts.getSketch().getValueAtQuantile(quantile);
	}

	/**
	 * Get the values at the requested quantiles.
	 * Note: The input array must be ordered.
	 * @param quantiles a array of doubles between 0.0 and 1.0 (both included)
	 * @return the values at the specified quantiles
	 */
	public String[] getValuesAtQuantiles(final double[] quantiles) {
		if (quantiles == null || quantiles.length == 0)
			throw new IllegalArgumentException("Quantiles array must be non-null and of non-zero length.");

		// Check we have an ordered list
		Double last = null;
		for (final double d : quantiles) {
			if (last == null) {
				last = d;
				continue;
			}
			if (d < last)
				throw new IllegalArgumentException("Quantiles array must be ordered.");
		}

		final String[] ret = new String[quantiles.length];

		for (int i = 0; i < quantiles.length; i++)
			ret[i] = getValueAtQuantile(quantiles[i]);

		return ret;
	}

	/**
	 * Get the histogram with the supplied number of buckets.
	 * @param buckets the number of buckets in the Histogram
	 * @return An array of length 'buckets' that constitutes the Histogram
	 */
	public Histogram.Entry[] getHistogram(final int buckets) {
		if (!analysisConfig.isEnabled(TextAnalyzer.Feature.DISTRIBUTIONS))
			throw new IllegalArgumentException(NOT_ENABLED);

		if (buckets <= 0)
			throw new IllegalArgumentException("Number of buckets must be > 0.");

		final FTAType baseType = facts.getMatchTypeInfo().getBaseType();
		if (FTAType.STRING.equals(baseType) || FTAType.BOOLEAN.equals(baseType))
			throw new IllegalArgumentException("No Histogram support for either STRING or BOOLEAN types.");

		return facts.getHistogram().getHistogram(buckets);
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
		if (facts.getMatchTypeInfo().isSemanticType() || (!facts.leadingWhiteSpace && !facts.trailingWhiteSpace))
			return facts.getMatchTypeInfo().regexp;

		// We need to add whitespace to the pattern but if there is alternation in the RE we need to be careful
		StringBuilder answer = new StringBuilder();
		if (facts.leadingWhiteSpace)
			answer.append(KnownTypes.PATTERN_WHITESPACE);
		final boolean optional = facts.getMatchTypeInfo().regexp.indexOf('|') != -1;
		if (optional)
			answer.append('(');
		answer.append(facts.getMatchTypeInfo().regexp);
		if (optional)
			answer.append(')');
		if (facts.trailingWhiteSpace)
			answer.append(KnownTypes.PATTERN_WHITESPACE);

		return answer.toString();
	}

	/**
	 * Get the Regular Expression that reflects the non-white space element in the data stream.
	 * For example, if a stream contains '  hello' and 'world  ' this would return '(?i)(HELLO|WORLD)'.
	 * @return The Regular Expression reflecting the non-white space data.
	 */
	public String getDataRegExp() {
		return facts.getMatchTypeInfo().regexp;
	}

	/**
	 * Get the count of all (non-blank/non-null/non-outlier/non-invalid) samples that matched the determined type.
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
		return facts.external.totalCount;
	}

	/**
	 * Get the count of all null elements in the entire data stream (if known).
	 * Use {@link #getNullCount() getNullCount()} for the equivalent on the sample set.
	 * @return Count of all null elements in the entire data stream (-1 if not known).
	 */
	public long getTotalNullCount() {
		return facts.external.totalNullCount;
	}

	/**
	 * Get the count of all blank elements in the entire data stream (if known).
	 * Note: any number (including zero) of spaces are Blank.
	 * Use {@link #getBlankCount() getBlankCount()} for the equivalent on the sample set.
	 * @return Count of all blank samples in the entire data stream (-1 if not known).
	 */
	public long getTotalBlankCount() {
		return facts.external.totalBlankCount;
	}

	/**
	 * Get the mean for Numeric types (Long, Double) across the entire data stream (if known).
	 * Use {@link #getMean() getMean()} for the equivalent on the sample set.
	 * @return The mean across the entire data stream (null if not known).
	 */
	public Double getTotalMean() {
		return facts.external.totalMean;
	}

	/**
	 * Get the standard deviation for Numeric types (Long, Double) across the entire data stream (if known).
	 * Use {@link #getStandardDeviation() getStandardDeviation()} for the equivalent on the sample set.
	 * @return The Standard Deviation across the entire data stream (null if not known).
	 */
	public Double getTotalStandardDeviation() {
		return facts.external.totalStandardDeviation;
	}

	/**
	 * Get the minimum value for Numeric, Boolean and String types across the entire data stream (if known).
	 * Use {@link #getMinValue() getMinValue()} for the equivalent on the sample set.
	 * @return The minimum value as a String (null if not known).
	 */
	public String getTotalMinValue() {
		return facts.external.totalMinValue;
	}

	/**
	 * Get the maximum value for Numeric, Boolean and String across the entire data stream (if known).
	 * Use {@link #getMaxValue() getMaxValue()} for the equivalent on the sample set.
	 * @return The maximum value as a String (null if not known).
	 */
	public String getTotalMaxValue() {
		return facts.external.totalMaxValue;
	}

	/**
	 * Get the minimum length for Numeric, Boolean and String across the entire data stream (if known).
	 * Note: For String and Boolean types this length includes any whitespace.
	 * Use {@link #getMinLength() getMinLength()} for the equivalent on the sample set.
	 * @return The minimum length in the entire Data Stream (-1 if not known).
	 */
	public int getTotalMinLength() {
		return facts.external.totalMinLength;
	}

	/**
	 * Get the maximum length for Numeric, Boolean and String across the entire data stream (if known).
	 * Note: For String and Boolean types this length includes any whitespace.
	 * Use {@link #getMaxLength() getMaxLength()} for the equivalent on the sample set.
	 * @return The maximum length in the entire Data Stream (-1 if not known).
	 */
	public int getTotalMaxLength() {
		return facts.external.totalMaxLength;
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
	public NavigableMap<String, Long> getCardinalityDetails() {
		return facts.getCardinalitySorted();
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
	 * Get the number of distinct invalid entries for the current data stream.
	 * See {@link com.cobber.fta.TextAnalyzer#setMaxOutliers(int) setMaxOutliers()} method in TextAnalyzer.
	 * Note: This is not a complete invalid analysis unless the invalid count of the
	 * data stream is less than the maximum invalid count (Default: {@value com.cobber.fta.AnalysisConfig#MAX_INVALID_DEFAULT}).
	 * @return Count of the distinct invalid entries.
	 */
	public int getInvalidCount() {
		return facts.invalid.size();
	}

	/**
	 * Get the invalid entry details for the current data stream.  This is a Map of Strings and the count
	 * of occurrences.
	 * @return A Map of values and their occurrence frequency of the data stream to date.
	 */
	public Map<String, Long> getInvalidDetails() {
		return facts.invalid;
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

	protected Facts getFacts() {
		return facts;
	}

	public String checkCounts() {
		if (getOutlierCount() == getConfig().getMaxOutliers())
			return null;
		if (getInvalidCount() == getConfig().getMaxInvalids())
			return null;

		final long outlierCount = getOutlierDetails().values().stream().mapToLong(l-> l).sum();
		final long invalidCount = getInvalidDetails().values().stream().mapToLong(l-> l).sum();

		// Check that the sum of the Cardinality set is equal to the matchCount
		if (getCardinality() < getConfig().getMaxCardinality() && getCardinalityDetails().values().stream().mapToLong(l-> l).sum() != getMatchCount())
			return "Cardinality sum incorrect";

		if (getSampleCount() != getMatchCount() + getBlankCount() + getNullCount() + outlierCount + invalidCount)
			return "Samples != match + blank + null + outlier count + invalid count";

		return null;
	}


	/**
	 * A SHA-1 hash that reflects the data stream structure.
	 * Note: If a Semantic type is detected then the SHA-1 hash will reflect this.
	 * @return A String SHA-1 hash that reflects the structure of the data stream.
	 */
	public String getStructureSignature() {
		String structureSignature = getSignatureBaseType().toString() + ":";

		if (isSemanticType())
			structureSignature += getSemanticType();
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

	private FTAType getSignatureBaseType() {
		if (!isSemanticType())
			return facts.getMatchTypeInfo().getBaseType();

		final PluginDefinition pluginDefinition = PluginDefinition.findByQualifier(getTypeModifier());
		if (pluginDefinition != null && pluginDefinition.baseType != null)
			return pluginDefinition.baseType;

		return facts.getMatchTypeInfo().getBaseType();
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

	private void outputArray(final ArrayNode detail, final Collection<String> collection) {
		for (final String s : collection)
			detail.add(s);
	}

	private void outputArray(final ArrayNode detail, final String[] array) {
		for (final String s : array)
			detail.add(s);
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
	public ObjectNode asPlugin() {
		// Already a Semantic type or date type - so nothing interesting to report as a Plugin
		if (isSemanticType() || facts.getMatchTypeInfo().isDateType())
			return null;

		final ObjectNode plugin = MAPPER.createObjectNode();
		ArrayNode arrayNode;

		// Check to see if the Regular Expression is too boring to report - e.g. '.*' or '.{3,31}'
		final String regExp = getRegExp();
		final boolean boringRegExp = regExp.charAt(0) == '.' && (regExp.length() == 2 || (regExp.length() > 1 && regExp.charAt(1) == '{'));

		if (boringRegExp && facts.cardinality.size() > 100)
			return null;

		plugin.put("semanticType", name);

		final ArrayNode localeArray = MAPPER.createArrayNode();

		final ObjectNode localeNode = MAPPER.createObjectNode();
		localeNode.put("localeTag", facts.getLocale().toLanguageTag());

		final ArrayNode headerArray = MAPPER.createArrayNode();
		final ObjectNode headerNode = MAPPER.createObjectNode();
		headerNode.put("regExp", ".*(?i)" + name);
		headerNode.put("confidence", 90);
		headerArray.add(headerNode);
		localeNode.set("headerRegExps", headerArray);

		if (!boringRegExp) {
			plugin.put("pluginType", "regex");
			final ArrayNode matchArray = MAPPER.createArrayNode();
			final ObjectNode matchNode = MAPPER.createObjectNode();
			matchNode.put("regExpReturned", regExp);
			matchArray.add(matchNode);
			localeNode.set("matchEntries", matchArray);
		}

		localeArray.add(localeNode);

		plugin.set("validLocales", localeArray);

		if (boringRegExp) {
			plugin.put("pluginType", "list");
			final ObjectNode content = MAPPER.createObjectNode();
			arrayNode = MAPPER.createArrayNode();
			for (String element : facts.cardinality.keySet())
				arrayNode.add(element.toUpperCase(facts.getLocale()));
			content.put("type", "inline");
			content.set("members", arrayNode);
			plugin.set("content", content);
			plugin.put("backout", ".*");
		}
		else {
			if (statisticsEnabled() && facts.matchCount > 100 && (facts.cardinality.size()*100)/facts.matchCount < 20 && !regExp.startsWith("(?i)(")) {
				if (facts.getMinValue() != null)
					plugin.put("minimum", facts.getMinValue());
				if (facts.getMaxValue() != null)
					plugin.put("maximum", facts.getMaxValue());
			}
		}

		plugin.put("threshold", 95);
		plugin.put("baseType", facts.getMatchTypeInfo().getBaseType().name());

		return plugin;
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
		final boolean legacyJSON = analysisConfig.isEnabled(TextAnalyzer.Feature.LEGACY_JSON);

		final ObjectNode analysis = MAPPER.createObjectNode();
		if (target != SignatureTarget.STRUCTURE_SIGNATURE && target != SignatureTarget.DATA_SIGNATURE)
			analysis.put("fieldName", name);
		analysis.put("totalCount", facts.external.totalCount);
		analysis.put("sampleCount", facts.sampleCount);
		analysis.put("matchCount", facts.matchCount);
		analysis.put("nullCount", facts.nullCount);
		analysis.put("blankCount", facts.blankCount);
		analysis.put("distinctCount", facts.distinctCount);
		if (target != SignatureTarget.DATA_SIGNATURE) {
			analysis.put("regExp", getRegExp());
			/*
						final ArrayNode regExpStream = analysis.putArray("regExpStream");
						final Set<String> streamRegExps = new HashSet<>();
						streamRegExps.add(shape.getRegExp(false));
						// Only bother to return a fitted result of we have a reasonable number of samples
						if (shape.getSamples() > 100)
							streamRegExps.add(shape.getRegExp(true));
						streamRegExps.remove(getRegExp());
						if (!streamRegExps.isEmpty())
							outputArray(regExpStream, streamRegExps);
			*/

			analysis.put("confidence", facts.confidence);
			analysis.put("type", facts.getMatchTypeInfo().getBaseType().toString());

			analysis.put(legacyJSON ? "logicalType" : "isSemanticType", isSemanticType());
			if (legacyJSON) {
				if (isSemanticType())
					analysis.put("typeQualifier", facts.getMatchTypeInfo().getSemanticType());
				else if (facts.getMatchTypeInfo().typeModifier != null)
					analysis.put("typeQualifier", facts.getMatchTypeInfo().typeModifier);
			}
			else {
				if (facts.getMatchTypeInfo().typeModifier != null)
					analysis.put("typeModifier", facts.getMatchTypeInfo().typeModifier);
				if (isSemanticType())
					analysis.put("semanticType", facts.getMatchTypeInfo().getSemanticType());
			}
			if (analysisConfig.isEnabled(TextAnalyzer.Feature.FORMAT_DETECTION))
				analysis.put("contentFormat", facts.streamFormat);
		}

		if (FTAType.DOUBLE == facts.getMatchTypeInfo().getBaseType())
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
			if (facts.getMatchTypeInfo().isNumeric())
				analysis.put("mean", facts.mean);
			if (facts.getMatchTypeInfo().isNumeric())
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

		if (facts.getMatchTypeInfo().isNumeric())
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

		analysis.put("invalidCardinality", facts.invalid.size() < analysisConfig.getMaxOutliers() ? facts.invalid.size() : -1);
		if (!facts.invalid.isEmpty() && verbose > 0) {
			final ArrayNode detail = analysis.putArray("invalidDetail");
			outputDetails(MAPPER, detail, facts.invalid, verbose);
		}

		analysis.put("shapesCardinality", (shape.getShapes().size() > 0 && shape.getShapes().size() < analysisConfig.getMaxShapes()) ? shape.getShapes().size() : -1);
		if (!shape.getShapes().isEmpty() && verbose > 0) {
			final ArrayNode detail = analysis.putArray("shapesDetail");
			outputDetails(MAPPER, detail, shape.getShapes(), verbose);
		}

		// Output any quantile/histogram data
		if (analysisConfig.isEnabled(TextAnalyzer.Feature.DISTRIBUTIONS) && facts.matchCount != 0 &&
				!FTAType.STRING.equals(facts.getMatchTypeInfo().getBaseType()) && !FTAType.BOOLEAN.equals(facts.getMatchTypeInfo().getBaseType())) {
			// We have support for arbitrary quantiles - but output percentiles in the JSON
			final ArrayNode detailQ = analysis.putArray("percentiles");
			// 101 because we want 0.0 and 1.0 plus everything in between
			final double[] percentiles = new double[101];
			double value = 0.0;
			for (int i = 0; i < 100; i++) {
				percentiles[i] = value;
				value += .01;
			}
			// Make sure the last one is precisely 1.0
			percentiles[100] = 1.0;
			outputArray(detailQ, getValuesAtQuantiles(percentiles));

			final Histogram.Entry[] histogram = getHistogram(10);
			if (histogram != null) {
				final ArrayNode detailH = analysis.putArray("histogram");
				for (final Histogram.Entry e : histogram)
					detailH.add(e.getCount());
			}
		}

		analysis.put("leadingWhiteSpace", getLeadingWhiteSpace());
		analysis.put("trailingWhiteSpace", getTrailingWhiteSpace());
		analysis.put("multiline", getMultiline());

		// If an external source has set totalCount then output all the total* attributes (which
		// will presumably also have been set by the external source).
		if (facts.external.totalCount != -1) {
			if (facts.external.totalNullCount != -1)
				analysis.put("totalNullCount", facts.external.totalNullCount);
			if (facts.external.totalBlankCount != -1)
				analysis.put("totalBlankCount", facts.external.totalBlankCount);
			if (facts.external.totalMean != null)
				analysis.put("toalMean", facts.external.totalMean);
			if (facts.external.totalStandardDeviation != null)
				analysis.put("totalStandardDeviation", facts.external.totalStandardDeviation);
			if (facts.external.totalMinValue != null)
				analysis.put("totalMin", facts.external.totalMinValue);
			if (facts.external.totalMaxValue != null)
				analysis.put("totalMax", facts.external.totalMaxValue);
			if (facts.external.totalMinLength != -1)
				analysis.put("totalMinLength", facts.external.totalMinLength);
			if (facts.external.totalMaxLength != -1)
				analysis.put("totalMaxLength", facts.external.totalMaxLength);
		}

		if (facts.getMatchTypeInfo().isDateType())
			analysis.put("dateResolutionMode", getDateResolutionMode().toString());

		if (target != SignatureTarget.DATA_SIGNATURE) {
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
