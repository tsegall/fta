/*
 * Copyright 2017-2026 Tim Segall
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.cobber.fta.core.FTAMergeException;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.FTAUnsupportedLocaleException;
import com.cobber.fta.core.InternalErrorException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Handles serialization, deserialization, and merging of {@link TextAnalyzer} instances.
 * Extracted from {@link TextAnalyzer} as part of the God-Class decomposition.
 */
class AnalyzerSerializer {

	static final ObjectMapper serializationMapper = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

	/**
	 * Serialize a TextAnalyzer.
	 * @see TextAnalyzer#serialize()
	 */
	static String serialize(final TextAnalyzer ta) throws FTAPluginException, FTAUnsupportedLocaleException {
		if (ta.getConfig().getTraceOptions() != null && ta.traceConfig == null)
			ta.initializeTrace();

		ta.emptyCache();

		// If we have not already determined the type - we need to force the issue
		if (ta.facts.getMatchTypeInfo() == null)
			ta.determineType();

		final TextAnalyzerWrapper wrapper = new TextAnalyzerWrapper(ta.getConfig(), ta.getContext(), ta.getPlugins().getUserDefinedPlugins(), ta.facts.calculateFacts());

		// We are serializing the analyzer (assume it will not be used again - so persist the samples)
		if (ta.traceConfig != null) {
			ta.traceConfig.persistSamples();
			ta.traceConfig.tag("serialize", ta.facts.sampleCount);
		}

		try {
			return serializationMapper.writeValueAsString(serializationMapper.convertValue(wrapper, ObjectNode.class));
		} catch (IOException e) {
			throw new InternalErrorException("Cannot output JSON for the Analysis", e);
		}
	}

	/**
	 * Create a new TextAnalyzer from a serialized representation.
	 * @see TextAnalyzer#deserialize(String)
	 */
	static TextAnalyzer deserialize(final String serialized) throws FTAMergeException, FTAPluginException, FTAUnsupportedLocaleException {
		TextAnalyzer ret = null;

		try {
			final TextAnalyzerWrapper wrapper = serializationMapper.readValue(serialized, TextAnalyzerWrapper.class);
			ret = new TextAnalyzer(wrapper.analyzerContext);
			ret.setConfig(wrapper.analysisConfig);

			ret.facts = wrapper.facts;
			ret.facts.setConfig(wrapper.analysisConfig);
			ret.getPlugins().registerPluginListWithPrecedence(wrapper.userDefinedPlugins, wrapper.analysisConfig);
			ret.initializeTrace();
			ret.initialize();
			ret.facts.hydrate();

			if (ret.traceConfig != null)
				ret.traceConfig.tag("deserialize", ret.facts.sampleCount);

			return ret;
		} catch (JsonProcessingException e) {
			throw new FTAMergeException("Issue deserializing supplied JSON.", e);
		}
	}

	/**
	 * Create a new TextAnalyzer which is the result of merging two separate TextAnalyzers.
	 * @see TextAnalyzer#merge(TextAnalyzer, TextAnalyzer)
	 */
	static TextAnalyzer merge(final TextAnalyzer first, final TextAnalyzer second) throws FTAMergeException, FTAPluginException, FTAUnsupportedLocaleException {
		first.emptyCache();
		second.emptyCache();
		final TextAnalyzer ret = new TextAnalyzer(first.getContext());

		// We are merging two analyzers (assume they will not be used again - so persist the samples)
		if (first.traceConfig != null) {
			first.traceConfig.persistSamples();
			first.traceConfig.tag("merge.left", first.getFacts().getSampleCount());
		}
		if (second.traceConfig != null) {
			second.traceConfig.persistSamples();
			second.traceConfig.tag("merge.right", first.getFacts().getSampleCount());
		}

		if (!first.getConfig().equals(second.getConfig()))
			throw new FTAMergeException("The AnalysisConfig for both TextAnalyzers must be identical.");

		// If we have not already determined the type - we need to force the issue
		if (first.facts.getMatchTypeInfo() == null)
			first.determineType();
		if (second.facts.getMatchTypeInfo() == null)
			second.determineType();

		ret.setConfig(first.getConfig());

		// An inadequate check that the two Analyzers being merged have the same user-defined plugins registered
		if (first.getPlugins().getUserDefinedPlugins().size() != second.getPlugins().getUserDefinedPlugins().size())
			throw new FTAMergeException("The user-defined plugins for both TextAnalyzers must be identical.");

		// Register the user-defined plugins on the merged TextAnalyzer
		ret.getPlugins().registerPluginListWithPrecedence(first.getPlugins().getUserDefinedPlugins(), first.getConfig());

		// Train using all the non-null/non-blank elements
		final Map<String, Long> merged = new HashMap<>();

		// Prime the merged set with the first set (real, outliers, and invalid which are non-overlapping)
		final Facts firstFacts = first.facts.calculateFacts();
		merged.putAll(firstFacts.cardinality);
		merged.putAll(firstFacts.outliers);
		merged.putAll(firstFacts.invalid);
		// Preserve the top and bottom values - even if they were not captured in the cardinality set
		if (firstFacts.cardinality.size() >= first.getMaxCardinality()) {
			addToMap(merged, firstFacts.topK, first);
			addToMap(merged, firstFacts.bottomK, first);
		}

		// Merge in the second set
		final Facts secondFacts = second.facts.calculateFacts();
		for (final Map.Entry<String, Long> entry : secondFacts.cardinality.entrySet()) {
			final Long seen = merged.get(entry.getKey());
			if (seen == null) {
				merged.put(entry.getKey(), entry.getValue());
			}
			else
				merged.put(entry.getKey(), seen + entry.getValue());
		}
		for (final Map.Entry<String, Long> entry : secondFacts.outliers.entrySet()) {
			final Long seen = merged.get(entry.getKey());
			if (seen == null) {
				merged.put(entry.getKey(), entry.getValue());
			}
			else
				merged.put(entry.getKey(), seen + entry.getValue());
		}
		for (final Map.Entry<String, Long> entry : secondFacts.invalid.entrySet()) {
			final Long seen = merged.get(entry.getKey());
			if (seen == null) {
				merged.put(entry.getKey(), entry.getValue());
			}
			else
				merged.put(entry.getKey(), seen + entry.getValue());
		}
		// Preserve the top and bottom values - even if they were not captured in the cardinality set
		if (secondFacts.cardinality.size() >= second.getMaxCardinality()) {
			addToMap(merged, secondFacts.topK, second);
			addToMap(merged, secondFacts.bottomK, second);
		}
		ret.trainBulk(merged);

		ret.facts.nullCount = firstFacts.nullCount + secondFacts.nullCount;
		ret.facts.blankCount = firstFacts.blankCount + secondFacts.blankCount;
		ret.facts.sampleCount += ret.facts.nullCount + ret.facts.blankCount;

		if (firstFacts.external.totalCount != -1 && secondFacts.external.totalCount != -1)
			ret.facts.external.totalCount = firstFacts.external.totalCount + secondFacts.external.totalCount;
		if (firstFacts.external.totalNullCount != -1 && secondFacts.external.totalNullCount != -1)
			ret.facts.external.totalNullCount = firstFacts.external.totalNullCount + secondFacts.external.totalNullCount;
		if (firstFacts.external.totalBlankCount != -1 && secondFacts.external.totalBlankCount != -1)
			ret.facts.external.totalBlankCount = firstFacts.external.totalBlankCount + secondFacts.external.totalBlankCount;
		if (firstFacts.external.totalInvalidCount != -1 && secondFacts.external.totalInvalidCount != -1)
			ret.facts.external.totalInvalidCount = firstFacts.external.totalInvalidCount + secondFacts.external.totalInvalidCount;
		if (firstFacts.external.totalMatchCount != -1 && secondFacts.external.totalMatchCount != -1)
			ret.facts.external.totalMatchCount = firstFacts.external.totalMatchCount + secondFacts.external.totalMatchCount;
		if (firstFacts.external.totalMinLength != -1 && secondFacts.external.totalMinLength != -1)
			ret.facts.external.totalMinLength = Math.min(firstFacts.external.totalMinLength, secondFacts.external.totalMinLength);
		if (firstFacts.external.totalMaxLength != -1 && secondFacts.external.totalMaxLength != -1)
			ret.facts.external.totalMaxLength = Math.max(firstFacts.external.totalMaxLength, secondFacts.external.totalMaxLength);
		if (firstFacts.external.totalMinValue != null && secondFacts.external.totalMinValue != null) {
			final CommonComparator<?> comparator = new CommonComparator<>(firstFacts.getStringConverter());
			if (comparator.compare(firstFacts.external.totalMinValue, secondFacts.external.totalMinValue) < 0)
				ret.facts.external.totalMinValue = firstFacts.external.totalMinValue;
			else
				ret.facts.external.totalMinValue = secondFacts.external.totalMinValue;
		}
		if (firstFacts.external.totalMaxValue != null && secondFacts.external.totalMaxValue != null) {
			final CommonComparator<?> comparator = new CommonComparator<>(firstFacts.getStringConverter());
			if (comparator.compare(firstFacts.external.totalMaxValue, secondFacts.external.totalMaxValue) > 0)
				ret.facts.external.totalMaxValue = firstFacts.external.totalMaxValue;
			else
				ret.facts.external.totalMaxValue = secondFacts.external.totalMaxValue;
		}
		// Unfortunately nothing we can do for totalMean/totalStandardDeviation when we are merging
		// as we do not have the requisite data.

		// Set the min/maxRawLength just in case a blank field is the longest/shortest
		ret.facts.minRawLength = Math.min(first.facts.minRawLength, second.facts.minRawLength);
		ret.facts.maxRawLength = Math.max(first.facts.maxRawLength, second.facts.maxRawLength);

		// Lengths are true representations - so just overwrite with truth
		System.arraycopy(firstFacts.lengths, 0, ret.facts.lengths, 0, firstFacts.lengths.length);
		for (int i = 0; i < ret.facts.lengths.length; i++)
			ret.facts.lengths[i] += secondFacts.lengths[i];

		// So if both sets are unique in their own right and the sets are non-overlapping then the merged set is unique
		if (firstFacts.getMatchTypeInfo() != null && nonOverlappingRegions(firstFacts, secondFacts, ret.getConfig())) {
			if (firstFacts.uniqueness != null && firstFacts.uniqueness == 1.0 && secondFacts.uniqueness != null && secondFacts.uniqueness == 1.0)
				ret.facts.uniqueness = 1.0;
			if (firstFacts.monotonicIncreasing && secondFacts.monotonicIncreasing)
				ret.facts.monotonicIncreasing = true;
			else if (firstFacts.monotonicDecreasing && secondFacts.monotonicDecreasing)
				ret.facts.monotonicDecreasing = true;
		}

		boolean cardinalityBlown = false;
		// Check to see if we have exceeded the cardinality on the the first, second, or the merge.
		// If so the samples we have seen do not reflect the entirety of the input so we need to
		// calculate a set of attributes.
		if (ret.facts.cardinality.size() == ret.getConfig().getMaxCardinality() ||
				firstFacts.cardinality.size() == first.getConfig().getMaxCardinality() ||
				secondFacts.cardinality.size() == second.getConfig().getMaxCardinality()) {
			cardinalityBlown = true;

			ret.facts.minRawNonBlankLength = Math.min(first.facts.minRawNonBlankLength, second.facts.minRawNonBlankLength);
			ret.facts.maxRawNonBlankLength = Math.max(first.facts.maxRawNonBlankLength, second.facts.maxRawNonBlankLength);

			ret.facts.minTrimmedLength = Math.min(first.facts.minTrimmedLength, second.facts.minTrimmedLength);
			ret.facts.maxTrimmedLength = Math.max(first.facts.maxTrimmedLength, second.facts.maxTrimmedLength);

			ret.facts.minTrimmedLengthNumeric = Math.min(first.facts.minTrimmedLengthNumeric, second.facts.minTrimmedLengthNumeric);
			ret.facts.maxTrimmedLengthNumeric = Math.max(first.facts.maxTrimmedLengthNumeric, second.facts.maxTrimmedLengthNumeric);

			ret.facts.minTrimmedOutlierLength = Math.min(first.facts.minTrimmedOutlierLength, second.facts.minTrimmedOutlierLength);
			ret.facts.maxTrimmedOutlierLength = Math.max(first.facts.maxTrimmedOutlierLength, second.facts.maxTrimmedOutlierLength);

			ret.facts.leadingWhiteSpace = first.facts.leadingWhiteSpace || second.facts.leadingWhiteSpace;
			ret.facts.trailingWhiteSpace = first.facts.trailingWhiteSpace || second.facts.trailingWhiteSpace;
			ret.facts.multiline = first.facts.multiline || second.facts.multiline;

			// When we did the trainBulk above with the new set, max cardinality entries landed in the Cardinality set and potentially
			// some overflow was captured in the cardinalityOverflow - merge in the overflow from the first and second set.
			if (ret.facts.cardinalityOverflow != null || firstFacts.cardinalityOverflow != null || secondFacts.cardinalityOverflow != null) {
				if (firstFacts.cardinalityOverflow != null)
					ret.facts.cardinalityOverflow = ret.facts.cardinalityOverflow == null ? firstFacts.cardinalityOverflow : ret.facts.cardinalityOverflow.merge(firstFacts.cardinalityOverflow);
				if (secondFacts.cardinalityOverflow != null)
					ret.facts.cardinalityOverflow = ret.facts.cardinalityOverflow == null ? secondFacts.cardinalityOverflow : ret.facts.cardinalityOverflow.merge(secondFacts.cardinalityOverflow);
			}

			// If we are numeric then we need to synthesize the mean and variance
			if (ret.facts.getMatchTypeInfo() != null && ret.facts.getMatchTypeInfo().isNumeric()) {
				ret.facts.mean = (first.facts.mean*first.facts.matchCount + second.facts.mean*second.facts.matchCount)/(first.facts.matchCount + second.facts.matchCount);
				if (first.facts.variance == null)
					ret.facts.variance = second.facts.variance;
				else if (second.facts.variance == null)
					ret.facts.variance = first.facts.variance;
				else
					ret.facts.variance = ((first.facts.matchCount - 1)*first.facts.variance + (second.facts.matchCount - 1)*second.facts.variance)/(first.facts.matchCount+second.facts.matchCount-2);
				ret.facts.currentM2 = ret.facts.variance * ret.facts.matchCount;
			}
		}

		if (cardinalityBlown && ret.facts.getMatchTypeInfo() != null)
			ret.checkRegExpTypes(ret.facts.getMatchTypeInfo().getBaseType());

		// Do some basic sanity checks
		if (first.facts.getMatchTypeInfo() != null || second.facts.getMatchTypeInfo() != null) {
			if (ret.facts.getMatchTypeInfo() == null)
				ret.ctxdebug("Type determination", "WARNING - had a type pre merge but no longer does?");
			else
				if (!ret.facts.getMatchTypeInfo().isSemanticType() &&
						((first.facts.getMatchTypeInfo() != null && first.facts.getMatchTypeInfo().isSemanticType()) ||
								(second.facts.getMatchTypeInfo() != null && second.facts.getMatchTypeInfo().isSemanticType()))) {
					ret.ctxdebug("Type determination", "WARNING - result of merge not a Semantic Type but one of the inputs was?");
				}
		}

		return ret;
	}

	/*
	 * Used when merging to preserve uniqueness/monotonicIncreasing/monotonicDecreasing.  We can preserve these facts
	 * iff they have the same FTAType (e.g. Long/Double/Date) and they are comparable using a double as a proxy (see StringConverter).
	 */
	private static boolean nonOverlappingRegions(final Facts firstFacts, final Facts secondFacts, final AnalysisConfig analysisConfig) {
		final TypeInfo firstInfo = firstFacts.getMatchTypeInfo();
		final TypeInfo secondInfo = secondFacts.getMatchTypeInfo();

		if (firstInfo == null || secondInfo == null)
			return false;

		final FTAType firstBaseType = firstInfo.getBaseType();
		final FTAType secondBaseType = secondInfo.getBaseType();

		if (firstBaseType != secondBaseType || !Objects.equals(firstInfo.typeModifier, secondInfo.typeModifier))
			return false;

		if (!firstBaseType.isNumeric() && !firstBaseType.isDateOrTimeType())
			return false;

		if (firstBaseType.isDateOrTimeType() && !firstInfo.format.equals(secondInfo.format))
			return false;

		final String firstMin = firstFacts.getMinValue();
		final String secondMin = secondFacts.getMinValue();
		if (firstMin == null || secondMin == null)
			return false;

		final StringConverter stringConverter = new StringConverter(firstFacts.getMatchTypeInfo().getBaseType(), new TypeFormatter(firstFacts.getMatchTypeInfo(), analysisConfig));
		if (stringConverter.toDouble(firstMin) == stringConverter.toDouble(secondMin))
			return false;

		if (stringConverter.toDouble(firstMin) < stringConverter.toDouble(secondMin))
			return stringConverter.toDouble(firstFacts.getMaxValue()) < stringConverter.toDouble(secondMin);

		return stringConverter.toDouble(secondFacts.getMaxValue()) < stringConverter.toDouble(firstMin);
	}

	/*
	 * AddToMap is used to add the bottomK and topK to the Map we are going to use to train.  Doing this ensures that
	 * the merged result will at least have the same bottomK/topK as it should have even if these were not captured in the
	 * cardinality set.
	 * The challenge here is that the bottomK/topK values are normalized e.g. if the user supplies 00, 2, 4, 6, ...
	 * then the bottomK will be 0,2,4,6 so 0 will not appear in the cardinality set but will appear in the bottomK set.
	 * Note: this routine is not fast if the extremes are not in the cardinality set, but it is only used when we are merging two analyses.
	 */
	private static void addToMap(final Map<String, Long> merged, final Set<String> extremes, final TextAnalyzer analyzer) {
		if (extremes == null)
			return;

		final Map<Object, String> missing = new HashMap<>();
		for (final String e : extremes) {
			if (e == null)
				return;
			// If we already have it in the merged set then we are done
			if (merged.get(e.toUpperCase(analyzer.getConfig().getLocale())) != null)
				continue;
			final Object extreme = analyzer.facts.getStringConverter().getValue(e);
			if (extreme == null)
				continue;
			missing.put(extreme, e);
		}

		// If we failed to find any of the extreme values then do a single pass through the existing set to see if any
		// are present in their normalized form, if so remove them, if not then add them to the set.
		if (missing.size() != 0) {
			for (final String m : merged.keySet()) {
				// Check for equality of value not of format - e.g. "00" will equal "0" once both are converted to Longs
				final Object mValue = analyzer.facts.getStringConverter().getValue(m);
				if (mValue != null && missing.keySet().contains(mValue))
					missing.remove(mValue);
			}
			for (final String missed : missing.values())
				merged.put(missed, 1L);
		}
	}
}
