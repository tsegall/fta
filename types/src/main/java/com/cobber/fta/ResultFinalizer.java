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

import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.cobber.fta.TextAnalyzer.Feature;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.FTAUnsupportedLocaleException;
import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.core.RegExpSplitter;
import com.cobber.fta.core.Utils;
import com.cobber.fta.dates.DateTimeParser;
import com.cobber.fta.token.TokenStream;

/**
 * Handles the finalization phase of type determination: backout helpers,
 * finite/regexp/datetime type checking, and per-type finalizers.
 * Extracted from {@link TextAnalyzer} as part of the God-Class decomposition.
 */
class ResultFinalizer {

	/** We are prepared to recognize any set of this size as an enum (and give a suitable regular expression). */
	private static final int MAX_ENUM_SIZE = 40;
	/** We need to see at least this many samples (all unique) before we will claim this is a possible key. */
	private static final int MIN_SAMPLES_FOR_KEY = 1000;
	private static final int EARLY_LONG_YYYYMMDD = 19000101;
	private static final int LATE_LONG_YYYYMMDD = 20510101;

	private final AnalysisContext ac;
	private final TypeTracker typeTracker;

	ResultFinalizer(final AnalysisContext ac, final TypeTracker typeTracker) {
		this.ac = ac;
		this.typeTracker = typeTracker;
	}

	class OutlierAnalysis {
		long alphas;
		long digits;
		long spaces;
		long other;
		int doubles;
		long nonAlphaNumeric;
		boolean negative;
		boolean exponent;
		boolean grouping;

		private boolean isDouble(final String input) {
			try {
				Double.parseDouble(input.trim());
			} catch (NumberFormatException e) {
				final ParsePosition pos = new ParsePosition(0);
				final Number n = ac.doubleFormatter.parse(input, pos);
				if (n == null || input.length() != pos.getIndex())
					return false;
				if (typeTracker.hasGroupingSeparator(input, ac.ni.groupingSeparator, ac.ni.decimalSeparator))
					grouping = true;
			}
			return true;
		}

		OutlierAnalysis(final Map<String, Long> outliers, final TypeInfo current) {
			// Sweep the current outliers
			for (final Map.Entry<String, Long> entry : outliers.entrySet()) {
				final String key = entry.getKey();
				final Long value = entry.getValue();
				if (FTAType.LONG.equals(current.getBaseType()) && isDouble(key)) {
					doubles++;
					if (!negative)
						negative = key.charAt(0) == '-';
					if (!exponent)
						exponent = key.indexOf('e') != -1 || key.indexOf('E') != -1;
				}
				boolean foundAlpha = false;
				boolean foundDigit = false;
				boolean foundSpace = false;
				boolean foundOther = false;
				final int len = key.length();
				for (int i = 0; i < len; i++) {
					final Character c = key.charAt(i);
					if (Character.isAlphabetic(c))
						foundAlpha = true;
				    else if (Character.isDigit(c))
						foundDigit = true;
				    else if (Character.isWhitespace(c))
						foundSpace = true;
				    else
						foundOther = true;
				}
				if (foundAlpha)
					alphas += value;
				if (foundDigit)
					digits += value;
				if (foundSpace)
					spaces += value;
				if (foundOther)
					other += value;
				if (foundSpace || foundOther)
					nonAlphaNumeric += value;
			}
		}
	}

	boolean conditionalBackoutToPattern(final long realSamples, final TypeInfo current) {
		final OutlierAnalysis analysis = new OutlierAnalysis(ac.facts.outliers, current);

		final long badCharacters = current.isAlphabetic() ? analysis.digits : analysis.alphas;
		// If we are currently Alphabetic and the only errors are digits then convert to AlphaNumeric
		if (badCharacters != 0 && analysis.spaces == 0 && analysis.other == 0 && current.isAlphabetic()) {
			if (ac.facts.outliers.size() == ac.analysisConfig.getMaxOutliers() || analysis.digits > .01 * realSamples) {
				backoutToPatternID(realSamples, KnownTypes.ID.ID_ALPHANUMERIC_VARIABLE);
				return true;
			}
		}
		// If we are currently Numeric and the only errors are alpha then convert to AlphaNumeric
		else if (badCharacters != 0 && analysis.spaces == 0 && analysis.other == 0 && FTAType.LONG.equals(current.getBaseType())) {
			if (ac.facts.outliers.size() == ac.analysisConfig.getMaxOutliers() || analysis.alphas > .01 * realSamples) {
				backoutToPattern(realSamples, KnownTypes.PATTERN_ALPHANUMERIC_VARIABLE);
				return true;
			}
		}
		// If we are currently Numeric and the only errors are doubles then convert to double
		else if (!ac.facts.getMatchTypeInfo().isSemanticType() && ac.facts.outliers.size() == analysis.doubles && FTAType.LONG.equals(current.getBaseType())) {
			KnownTypes.ID id;
			if (analysis.exponent)
				id = (current.isSigned() || analysis.negative) ? KnownTypes.ID.ID_SIGNED_DOUBLE_WITH_EXPONENT : KnownTypes.ID.ID_DOUBLE_WITH_EXPONENT;
			else
				id = (current.isSigned() || analysis.negative) ? KnownTypes.ID.ID_SIGNED_DOUBLE : KnownTypes.ID.ID_DOUBLE;
			if (current.hasGrouping() || analysis.grouping)
				id = ac.knownTypes.grouping(ac.knownTypes.getByID(id).getRegExp()).id;
			backoutToPatternID(realSamples, id);
			return true;
		}
		else if ((realSamples > ac.reflectionSamples && ac.facts.outliers.size() == ac.analysisConfig.getMaxOutliers())
					|| (badCharacters + analysis.nonAlphaNumeric) > .01 * realSamples) {
				backoutToPattern(realSamples, KnownTypes.PATTERN_ANY_VARIABLE);
				return true;
		}

		return false;
	}

	private void backoutToPattern(final long realSamples, final String newPattern) {
		TypeInfo newTypeInfo = ac.knownTypes.getByRegExp(newPattern);

		// If it is not one of our known types then construct a suitable TypeInfo
		if (newTypeInfo == null)
			newTypeInfo = new TypeInfo(null, newPattern, FTAType.STRING, null, 0);

		backoutToTypeInfo(realSamples, newTypeInfo);
	}

	private void backoutToPatternID(final long realSamples, final KnownTypes.ID patternID) {
		backoutToTypeInfo(realSamples, ac.knownTypes.getByID(patternID));
	}

	private void backoutToString(final long realSamples) {
		ac.facts.matchCount = realSamples;

		// All outliers are now part of the cardinality set and there are now no outliers
		ac.facts.getCardinality().putAll(ac.facts.outliers);

		final RegExpGenerator gen = new RegExpGenerator(MAX_ENUM_SIZE, ac.locale);
		for (final String s : ac.facts.getCardinality().keySet())
			gen.train(s);

		final String newPattern = gen.getResult();
		TypeInfo newTypeInfo = ac.knownTypes.getByRegExp(newPattern);

		// If it is not one of our known types then construct a suitable TypeInfo
		if (newTypeInfo == null)
			newTypeInfo = new TypeInfo(null, newPattern, FTAType.STRING, null, 0);

		ac.facts.setMatchTypeInfo(newTypeInfo);

		for (final Entry<String, Long> entry : ac.facts.getCardinality().entrySet())
			typeTracker.trackString(entry.getKey(), entry.getKey().trim(), newTypeInfo, false, entry.getValue());

		ac.facts.outliers.clear();
		ac.outliersSmashed.clear();
		ac.ctxdebug("Type determination", "backing out string, matchTypeInfo - {}", ac.facts.getMatchTypeInfo());
	}

	private void backoutToTypeInfo(final long realSamples, final TypeInfo newTypeInfo) {
		ac.facts.matchCount = realSamples;
		ac.facts.setMatchTypeInfo(newTypeInfo);

		// All outliers are now part of the cardinality set and there are now no outliers
		for (final Map.Entry<String, Long> entry : ac.facts.outliers.entrySet())
			ac.facts.getCardinality().merge(entry.getKey(), entry.getValue(), Long::sum);

		// Need to update stats to reflect any outliers we previously ignored
		if (ac.facts.getMatchTypeInfo().getBaseType().equals(FTAType.STRING)) {
			for (final Map.Entry<String, Long> entry : ac.facts.getCardinality().entrySet())
				typeTracker.trackString(entry.getKey(), entry.getKey().trim(), newTypeInfo, false, entry.getValue());
		}
		else if (ac.facts.getMatchTypeInfo().getBaseType().equals(FTAType.DOUBLE)) {
			ac.facts.minDouble = ac.facts.getMinLong();
			ac.facts.maxDouble = ac.facts.getMaxLong();
			for (final Map.Entry<String, Long> entry : ac.facts.outliers.entrySet())
				typeTracker.trackDouble(entry.getKey(), ac.facts.getMatchTypeInfo(), true, entry.getValue());
		}

		ac.facts.outliers.clear();
		ac.outliersSmashed.clear();
		ac.ctxdebug("Type determination", "backing out, matchTypeInfo - {}", ac.facts.getMatchTypeInfo());
	}

	/**
	 * Backout from a mistaken Semantic type whose base type was DOUBLE
	 * @param logical The Semantic type we are backing out from
	 * @param realSamples The number of real samples we have seen.
	 */
	private void backoutLogicalDoubleType(final LogicalType logical, final long realSamples) {
		long otherDoubles = 0;

		final Map<String, Long> doubleOutliers = new HashMap<>();

		// Back out to a Double with the same Modifiers we already have (e.g., SIGNED, NON_LOCALIZED, ...
		ac.facts.setMatchTypeInfo(ac.knownTypes.getByTypeAndModifier(FTAType.DOUBLE, ac.facts.getMatchTypeInfo().typeModifierFlags));

		// Sweep the current outliers and check they are part of the set
		for (final Map.Entry<String, Long> entry : ac.facts.outliers.entrySet()) {
			try {
				Double.parseDouble(entry.getKey());
				otherDoubles += entry.getValue();
				doubleOutliers.put(entry.getKey(), entry.getValue());
			} catch (NumberFormatException el) {
				// Swallow
			}
		}

		// Move the doubles from the outlier set to the cardinality set
		ac.facts.matchCount += otherDoubles;
		ac.facts.outliers.entrySet().removeAll(doubleOutliers.entrySet());
		for (final Map.Entry<String, Long> entry : doubleOutliers.entrySet())
			typeTracker.addValid(entry.getKey(), entry.getValue());

		ac.ctxdebug("Type determination", "backing out double, matchTypeInfo - {}", ac.facts.getMatchTypeInfo());
	}

	/**
	 * Backout from a mistaken Semantic type whose base type was LONG
	 * @param logical The Semantic type we are backing out from
	 * @param realSamples The number of real samples we have seen.
	 */
	private void backoutLogicalLongType(final LogicalType logical, final long realSamples) {
		long otherLongs = 0;
		long otherDoubles = 0;

		final Map<String, Long> longOutliers = new HashMap<>();
		final Map<String, Long> doubleOutliers = new HashMap<>();

		// Sweep the current outliers and check they are part of the set
		for (final Map.Entry<String, Long> entry : ac.facts.outliers.entrySet()) {
			try {
				Long.parseLong(entry.getKey());
				otherLongs += entry.getValue();
				longOutliers.put(entry.getKey(), entry.getValue());
			} catch (NumberFormatException el) {
				try {
					Double.parseDouble(entry.getKey());
					otherDoubles += entry.getValue();
					doubleOutliers.put(entry.getKey(), entry.getValue());
				}
				catch (NumberFormatException ed) {
					// Swallow
				}
			}
		}

		// Move the longs from the outlier set to the cardinality set
		ac.facts.matchCount += otherLongs;
		ac.facts.outliers.entrySet().removeAll(longOutliers.entrySet());

		// So if all the values observed to date have been monotonic increasing (or decreasing) then we should preserve this fact
		final boolean saveMonitonicIncreasing = ac.facts.monotonicIncreasing;
		final boolean saveMonitonicDecreasing = ac.facts.monotonicDecreasing;
		for (final Map.Entry<String, Long> entry : longOutliers.entrySet()) {
			typeTracker.trackLong(entry.getKey().trim(), ac.knownTypes.getByID(KnownTypes.ID.ID_LONG), true, entry.getValue());
			typeTracker.addValid(entry.getKey(), entry.getValue());
		}
		ac.facts.monotonicIncreasing = saveMonitonicIncreasing;
		ac.facts.monotonicDecreasing = saveMonitonicDecreasing;

		if ((double) ac.facts.matchCount / realSamples > ac.analysisConfig.getThreshold()/100.0) {
			ac.facts.setMatchTypeInfo(ac.knownTypes.getByID(KnownTypes.ID.ID_LONG));
			ac.ctxdebug("Type determination", "backing out long, matchTypeInfo - {}", ac.facts.getMatchTypeInfo());
		}
		else if ((double)(ac.facts.matchCount + otherDoubles) / realSamples > ac.analysisConfig.getThreshold()/100.0) {
			ac.facts.setMatchTypeInfo(ac.knownTypes.getByID(KnownTypes.ID.ID_DOUBLE));
			ac.facts.outliers.entrySet().removeAll(doubleOutliers.entrySet());
			for (final Map.Entry<String, Long> entry : doubleOutliers.entrySet())
				typeTracker.addValid(entry.getKey(), entry.getValue());
			ac.facts.matchCount += otherDoubles;

			// Recalculate the world since we now 'know' it is a double
	        ac.facts.mean = 0.0;
	        ac.facts.variance = 0.0;
	        ac.facts.currentM2 = 0.0;

	        ac.facts.getCardinality().forEach((k, v) -> typeTracker.trackDouble(k, ac.facts.getMatchTypeInfo(), true, v));
		}
		else
			backoutToPatternID(realSamples, KnownTypes.ID.ID_ANY_VARIABLE);
	}

	void backout(final LogicalType logical, final long realSamples, final PluginAnalysis pluginAnalysis) {
		if (FTAType.STRING.equals(ac.facts.getMatchTypeInfo().getBaseType()))
			backoutToPattern(realSamples, pluginAnalysis.getNewPattern());
		else if (FTAType.LONG.equals(ac.facts.getMatchTypeInfo().getBaseType()))
			backoutLogicalLongType(logical, realSamples);
		else
			backoutLogicalDoubleType(logical, realSamples);

		ac.facts.confidence = (double) ac.facts.matchCount / realSamples;
	}

	/**
	 * Determine if the current dataset reflects a Semantic type.
	 * @param logical The Semantic type we are testing
	 * @return A MatchResult that indicates the quality of the match against the provided data
	 */
	private FiniteMatchResult checkFiniteSet(final FiniteMap cardinalityUpper, final FiniteMap outliers, final LogicalTypeFinite logical) {
		long realSamples = ac.facts.sampleCount - (ac.facts.nullCount + ac.facts.blankCount);
		long missCount = 0;				// count of number of misses

		final FiniteMap newOutliers = new FiniteMap(outliers);
		final Map<String, Long> addMatches = new HashMap<>();
		final double missThreshold = 1.0 - logical.getThreshold()/100.0;
		long validCount = 0;

		for (final Map.Entry<String, Long> entry : outliers.entrySet()) {
			final String upper = entry.getKey().toUpperCase(java.util.Locale.ENGLISH);
			if (logical.isValid(upper, true, entry.getValue())) {
				validCount += entry.getValue();
				addMatches.merge(upper, entry.getValue(), Long::sum);
			}
			else {
				missCount += entry.getValue();
				newOutliers.merge(entry.getKey(), entry.getValue(), Long::sum);
			}
		}

		// Sweep the balance and check they are part of the set
		if ((double) missCount / realSamples > missThreshold)
			return new FiniteMatchResult();

		final Map<String, Long> minusMatches = new HashMap<>();

		final Set<String> ignorable = logical.getIgnorable();

		long missEntries = 0;
		Map.Entry<String, Long> missEntry = null;

		for (final Map.Entry<String, Long> entry : cardinalityUpper.entrySet()) {
			if (ignorable != null && ignorable.contains(entry.getKey())) {
				realSamples -= entry.getValue();
				minusMatches.put(entry.getKey(), entry.getValue());
				newOutliers.put(entry.getKey(), entry.getValue());
			}
			else if (logical.isValid(entry.getKey(), true, entry.getValue()))
				validCount += entry.getValue();
			else {
				missEntries++;
				if (missEntry == null || entry.getValue() > missEntry.getValue())
					missEntry = entry;
				minusMatches.put(entry.getKey(), entry.getValue());
				newOutliers.put(entry.getKey(), entry.getValue());
			}
		}

		final FiniteMap newCardinality = new FiniteMap(cardinalityUpper);
		newCardinality.putAll(cardinalityUpper);
		newCardinality.putAll(addMatches);
		for (final String elt : minusMatches.keySet())
			newCardinality.remove(elt);

		final long outlierCount = newOutliers.values().stream().mapToLong(l-> l).sum();
		if (logical.analyzeSet(ac.analyzerContext, validCount, realSamples, ac.facts.getMatchTypeInfo().getRegExp(), ac.facts.calculateFacts(), newCardinality, newOutliers, ac.tokenStreams, ac.analysisConfig).isValid()) {
			validCount += outlierCount - newOutliers.values().stream().mapToLong(l-> l).sum();
			return new FiniteMatchResult(logical, logical.getConfidence(validCount, realSamples, ac.analyzerContext), validCount, newOutliers, newCardinality);
		}

		// If the number of misses is less than 10% then remove the worst offender since it will often be something
		// silly like All, Other, N/A, ...
		if (missEntries != 0 && (double)missEntries/cardinalityUpper.size() < .1 && logical.getHeaderConfidence(ac.analyzerContext) >= 90) {
			realSamples -= missEntry.getValue();
			if (logical.analyzeSet(ac.analyzerContext, validCount, realSamples, ac.facts.getMatchTypeInfo().getRegExp(), ac.facts.calculateFacts(), newCardinality, newOutliers, ac.tokenStreams, ac.analysisConfig).isValid())
				return new FiniteMatchResult(logical, logical.getConfidence(validCount, realSamples, ac.analyzerContext), validCount, newOutliers, newCardinality);
		}

		return new FiniteMatchResult();
	}

	String lengthQualifier(final int min, final int max) {
		if (!ac.analysisConfig.isEnabled(Feature.LENGTH_QUALIFIER))
			return min > 0 ? "+" : ".";

		return RegExpSplitter.qualify(min, max);
	}

	/**
	 * Given a Regular Expression with an unbound Integer freeze it with the low and high size.
	 * For example, given something like \d+, convert to \d{4,9}.
	 * @return If possible an updated String, if not found then the original string.
	 */
	private String freezeNumeric(final String input) {
		final StringBuilder result = new StringBuilder(input);
		boolean characterClass = false;
		boolean numericStarted = false;
		int idx = 0;

		while (idx < result.length()) {
			char ch = result.charAt(idx);
			if (ch == '\\') {
				ch = result.charAt(++idx);
				if (ch == 'd')
					numericStarted = true;
			}
			else if (ch == '[')
				characterClass = true;
			else if (ch == ']')
				characterClass = false;
			else if (ch == '+') {
				if (numericStarted && !characterClass) {
					break;
				}
			}
			idx++;
		}

		return idx == result.length() ? input :
			result.replace(idx, idx + 1, lengthQualifier(ac.facts.minTrimmedLengthNumeric, ac.facts.maxTrimmedLengthNumeric)).toString();
	}

	private class FiniteMatchResult {
		LogicalTypeFinite logical;
		final double score;
		FiniteMap newOutliers;
		FiniteMap newCardinality;
		long validCount;
		final boolean isMatch;

		boolean matched() {
			return isMatch;
		}

		FiniteMatchResult(final LogicalTypeFinite logical, final double score, final long validCount, final FiniteMap newOutliers, final FiniteMap newCardinality) {
			this.logical = logical;
			this.score = score;
			this.validCount = validCount;
			this.newOutliers = newOutliers;
			this.newCardinality = newCardinality;
			this.isMatch = true;
		}

		FiniteMatchResult() {
			score = Double.MIN_VALUE;
			this.isMatch = false;
		}
	}

	private LogicalTypeFinite matchFiniteTypes(final FTAType type, final FiniteMap cardinalityUpper) {
		double originalScore;

		LogicalType priorLogical = null;
		// We may have a Semantic Type already identified but see if there is a better Finite Semantic type
		if (ac.facts.getMatchTypeInfo().isSemanticType()) {
			priorLogical = ac.plugins.getRegistered(ac.facts.getMatchTypeInfo().getSemanticType());
			originalScore = ac.facts.confidence;
		}
		else
			originalScore = -1.0;
		double bestScore = originalScore;

		FiniteMatchResult bestResult = null;

		for (final LogicalTypeFinite logical : ac.finiteTypes) {
			if (!logical.acceptsBaseType(type))
				continue;

			// Either we need to be an open set or the cardinality should be reasonable (relative to the size of the set)
			if ((!logical.isClosed() || cardinalityUpper.size() <= logical.getSize() + 2 + logical.getSize()/20)) {
				final FiniteMatchResult result = checkFiniteSet(cardinalityUpper, ac.facts.outliers, logical);

				if (!result.matched() || result.score < bestScore)
					continue;

				// We prefer finite matches to infinite matches only if the score is equal
				// - header is better or
				// - header is equal and priority is better
				if (bestResult == null && priorLogical != null && result.score == bestScore) {
					if (logical.getHeaderConfidence(ac.analyzerContext) < priorLogical.getHeaderConfidence(ac.analyzerContext))
						continue;
					if (logical.getHeaderConfidence(ac.analyzerContext) == priorLogical.getHeaderConfidence(ac.analyzerContext) &&
							logical.getPluginDefinition().getOrder() > priorLogical.getPluginDefinition().getOrder())
						continue;
				}

				// Choose the best score
				if (result.score > bestScore ||
						// If bestResult is null then this finite match has matched an incoming score to beat
						bestResult == null ||
						// If two scores the same then prefer the one with the higher header confidence
						logical.getHeaderConfidence(ac.analyzerContext) > bestResult.logical.getHeaderConfidence(ac.analyzerContext) ||
						// If two scores the same then prefer the logical with the highest priority
						(logical.getHeaderConfidence(ac.analyzerContext) == bestResult.logical.getHeaderConfidence(ac.analyzerContext) &&
						logical.getPluginDefinition().getOrder() < bestResult.logical.getPluginDefinition().getOrder())) {
					bestResult = result;
					bestScore = result.score;
				}
			}
		}

		if (bestResult == null)
			return null;

		ac.facts.outliers = bestResult.newOutliers;
		ac.facts.setCardinality(bestResult.newCardinality);
		ac.facts.matchCount = bestResult.validCount;
		ac.facts.setMatchTypeInfo(new TypeInfo(bestResult.logical.getRegExp(), bestResult.logical.getBaseType(), bestResult.logical.getSemanticType(), ac.facts.getMatchTypeInfo()));

		ac.ctxdebug("Type determination", "new matchTypeInfo - {}, original score: {}, new score: {}",
				ac.facts.getMatchTypeInfo(), originalScore, bestScore);
		if (priorLogical != null) {
			ac.ctxdebug("Type determination", "header confidence (current, prior): {},{}, priority (current, prior): {}, {}",
					bestResult.logical.getHeaderConfidence(ac.analyzerContext), priorLogical.getHeaderConfidence(ac.analyzerContext),
					bestResult.logical.getPluginDefinition().getOrder(), priorLogical.getPluginDefinition().getOrder());
		}

		return bestResult.logical;
	}

	boolean checkDateTimeTypes(final FTAType type) {
		final long realSamples = ac.facts.sampleCount - (ac.facts.nullCount + ac.facts.blankCount);
		boolean updated = false;
		double bestScore;
		LogicalType priorLogical = null;

		// We may have a Semantic Type already identified but see if there is a better option
		if (ac.facts.getMatchTypeInfo().isSemanticType()) {
			priorLogical = ac.plugins.getRegistered(ac.facts.getMatchTypeInfo().getSemanticType());
			bestScore = ac.facts.confidence;
		}
		else
			bestScore = -1.0;

		if (ac.facts.getMatchTypeInfo().getBaseType().isDateOrTimeType() && !ac.facts.getMatchTypeInfo().isSemanticType()) {
			for (final LogicalTypeInfinite logical : ac.infiniteTypes) {
				if (!logical.acceptsBaseType(type) || logical == priorLogical)
					continue;

				long newMatchCount = ac.facts.matchCount;
				final FiniteMap newCardinality = new FiniteMap(ac.facts.getCardinality());
				final FiniteMap newInvalids = new FiniteMap(ac.facts.outliers);
				for (final Map.Entry<String, Long> current : ac.facts.getCardinality().entrySet()) {
					if (logical.isValid(current.getKey().trim()))
						newCardinality.put(current.getKey(), current.getValue());
					else {
						newMatchCount -= current.getValue();
						newInvalids.put(current.getKey(), current.getValue());
					}
				}

				double newScore = 0.0;
				// Based on the new Cardinality/Outliers do we think this is a match?
				if (logical.analyzeSet(ac.analyzerContext, newMatchCount, realSamples, ac.facts.getMatchTypeInfo().getRegExp(), ac.facts.calculateFacts(), newCardinality, newInvalids, ac.tokenStreams, ac.analysisConfig).isValid()) {
						// Skip if the new score is worse than the current
						if ((newScore = logical.getConfidence(newMatchCount, realSamples, ac.analyzerContext)) < bestScore)
							continue;
						if (newScore == bestScore) {
							// Skip if the scores are the same but we like the header less
							if (logical.getHeaderConfidence(ac.analyzerContext) < priorLogical.getHeaderConfidence(ac.analyzerContext))
								continue;
							// Skip if the scores are the same but the Order is higher
							if (logical.getHeaderConfidence(ac.analyzerContext) == priorLogical.getHeaderConfidence(ac.analyzerContext) && logical.getPluginDefinition().getOrder() > priorLogical.getPluginDefinition().getOrder())
								continue;
						}

					ac.facts.setMatchTypeInfo(new TypeInfo(null, ac.facts.getMatchTypeInfo().getRegExp(), ac.facts.getMatchTypeInfo().getBaseType(), logical.getSemanticType(), true, ac.facts.getMatchTypeInfo().format));
					ac.facts.matchCount = newMatchCount;
					ac.facts.setCardinality(newCardinality);
					ac.facts.invalid = newInvalids;
					ac.ctxdebug("Type determination", "infinite type, matchTypeInfo - {}", ac.facts.getMatchTypeInfo());
					ac.facts.confidence = bestScore = newScore;
					priorLogical = logical;
					updated = true;
				}
			}
		}

		return updated;
	}

	boolean checkRegExpTypes(final FTAType type) {
		final long realSamples = ac.facts.sampleCount - (ac.facts.nullCount + ac.facts.blankCount);
		boolean updated = false;
		double bestScore;
		LogicalType priorLogical = null;

		// We may have a Semantic Type already identified but see if there is a better option
		if (ac.facts.getMatchTypeInfo().isSemanticType()) {
			priorLogical = ac.plugins.getRegistered(ac.facts.getMatchTypeInfo().getSemanticType());
			bestScore = ac.facts.confidence;
		}
		else
			bestScore = -1.0;

		for (final LogicalTypeRegExp logical : ac.regExpTypes) {
			if (!logical.acceptsBaseType(type) || logical == priorLogical)
				continue;

			// Check to see if either
			// the Regular Expression we have matches the Semantic types, or
			// the Regular Expression for the Semantic types matches all the data we have observed
			for (final PluginMatchEntry entry : logical.getMatchEntries()) {
				long newMatchCount = ac.facts.matchCount;
				final String re = entry.getRegExpReturned();
				if (((newMatchCount = ac.tokenStreams.matches(re, logical.getThreshold())) != 0)) {
					// Build the new Cardinality and Invalid maps - based on the RE
					final FiniteMap newCardinality = new FiniteMap(ac.facts.getCardinality());
					final FiniteMap newInvalids = new FiniteMap(ac.facts.outliers);
					for (final Map.Entry<String, Long> current : ac.facts.getCardinality().entrySet()) {
						if (current.getKey().trim().matches(re))
							newCardinality.put(current.getKey(), current.getValue());
						else
							newInvalids.put(current.getKey(), current.getValue());
					}
					for (final Map.Entry<String, Long> current : ac.facts.outliers.entrySet()) {
						if (current.getKey().trim().matches(re))
							newCardinality.put(current.getKey(), current.getValue());
						else
							newInvalids.put(current.getKey(), current.getValue());
					}
					for (final Map.Entry<String, Long> current : ac.facts.invalid.entrySet()) {
						if (current.getKey().trim().matches(re))
							newCardinality.put(current.getKey(), current.getValue());
						else
							newInvalids.put(current.getKey(), current.getValue());
					}

					double newScore = 0.0;
					// Based on the new Cardinality/Outliers do we think this is a match?
					if (logical.analyzeSet(ac.analyzerContext, ac.facts.matchCount, realSamples, ac.facts.getMatchTypeInfo().getRegExp(), ac.facts.calculateFacts(), newCardinality, newInvalids, ac.tokenStreams, ac.analysisConfig).isValid()) {
							// Skip if the new score is worse than the current
							if ((newScore = logical.getConfidence(newMatchCount, realSamples, ac.analyzerContext)) < bestScore)
								continue;
							if (newScore == bestScore) {
								// Skip if the scores are the same but we like the header less
								if (logical.getHeaderConfidence(ac.analyzerContext) < priorLogical.getHeaderConfidence(ac.analyzerContext))
									continue;
								// Skip if the scores are the same but the Order is higher
								if (logical.getPluginDefinition().getOrder() > priorLogical.getPluginDefinition().getOrder())
									continue;
							}

						logical.setMatchEntry(entry);
						ac.facts.setMatchTypeInfo(new TypeInfo(logical.getRegExp(), logical.getBaseType(), logical.getSemanticType(), ac.facts.getMatchTypeInfo()));
						ac.facts.matchCount = newMatchCount;
						ac.facts.setCardinality(newCardinality);
						ac.facts.invalid = newInvalids;
						ac.facts.outliers.clear();
						ac.ctxdebug("Type determination", "updated to Regular Expression Semantic type {}", ac.facts.getMatchTypeInfo());
						ac.facts.confidence = bestScore = newScore;
						priorLogical = logical;
						updated = true;
					}
				}
			}
		}

		return updated;
	}

	/*
	 * Synthesize the topK/bottomK by running the cardinality set.
	 */
	private void generateTopBottom() {
		for (final String s : ac.facts.getCardinality().keySet())
			try {
				typeTracker.trackDateTime(s, ac.facts.getMatchTypeInfo(), true, 1);
			}
			catch (DateTimeException e) {
				// Swallow - any we lost are no good so will not be in the top/bottom set!
			}
	}

	private boolean plausibleYear(final long realSamples) {
		return (ac.facts.getMinLongNonZero() > DateTimeParser.RECENT_EARLY_LONG_YYYY && ac.facts.getMaxLong() <= DateTimeParser.LATE_LONG_YYYY &&
				realSamples >= ac.reflectionSamples && ac.facts.getCardinality().size() > 10) ||
				(ac.facts.getMinLongNonZero() >= DateTimeParser.EARLY_LONG_YYYY && ac.facts.getMaxLong() <= DateTimeParser.LATE_LONG_YYYY &&
				(ac.keywords.match(ac.analyzerContext.getStreamName(), "YEAR") >= 90 ||
					ac.keywords.match(ac.analyzerContext.getStreamName(), "DATE") >= 90 ||
					ac.keywords.match(ac.analyzerContext.getStreamName(), "PERIOD") >= 90));
	}

	void switchToDate(final TypeInfo newTypeInfo, final LocalDate newMin, final LocalDate newMax) {
		ac.facts.setMatchTypeInfo(newTypeInfo);
		ac.facts.minLocalDate = newMin;
		ac.facts.maxLocalDate = newMax;

		killInvalidDates();

		// If we are collecting statistics - we need to generate the topK and bottomK
		if (ac.analysisConfig.isEnabled(Feature.COLLECT_STATISTICS))
			generateTopBottom();
	}

	private boolean isReallyDate(final long realSamples) {
		if (ac.facts.groupingSeparators != 0 || ac.facts.getMinLongNonZero() == Long.MAX_VALUE)
			return false;

		if (ac.facts.getMinLongNonZero() > EARLY_LONG_YYYYMMDD && ac.facts.getMaxLong() < LATE_LONG_YYYYMMDD &&
				DateTimeParser.plausibleDateLong(ac.facts.getMinLongNonZero(), 4) && DateTimeParser.plausibleDateLong(ac.facts.getMaxLong(), 4) &&
				((realSamples >= ac.reflectionSamples && ac.facts.getCardinality().size() > 10) || ac.keywords.match(ac.analyzerContext.getStreamName(), "DATE") >= 90)) {
			// Sometimes a Long is not a Long but it is really a date (yyyyMMdd)
			final TypeInfo newTypeInfo = new TypeInfo(null, "\\d{8}", FTAType.LOCALDATE, "yyyyMMdd", false, "yyyyMMdd");
			final java.time.format.DateTimeFormatter dtf = ac.dateTimeParser.ofPattern(newTypeInfo.format);
			switchToDate(newTypeInfo, LocalDate.parse(String.valueOf(ac.facts.getMinLongNonZero()), dtf), LocalDate.parse(String.valueOf(ac.facts.getMaxLong()), dtf));
			return true;
		}

		if (ac.facts.getMinLongNonZero() > EARLY_LONG_YYYYMMDD/100 && ac.facts.getMaxLong() < LATE_LONG_YYYYMMDD/100 &&
				DateTimeParser.plausibleDateLong(ac.facts.getMinLongNonZero() * 100 + 1, 4) && DateTimeParser.plausibleDateLong(ac.facts.getMaxLong() * 100 + 1, 4) &&
				((realSamples >= ac.reflectionSamples && ac.facts.getCardinality().size() > 10) || ac.keywords.match(ac.analyzerContext.getStreamName(), "PERIOD") >= 90)) {
			// Sometimes a Long is not a Long but it is really a date (yyyyMM)
			final TypeInfo newTypeInfo = new TypeInfo(null, "\\d{6}", FTAType.LOCALDATE, "yyyyMM", false, "yyyyMM");
			final java.time.format.DateTimeFormatter dtf = ac.dateTimeParser.ofPattern(newTypeInfo.format);
			switchToDate(newTypeInfo, LocalDate.parse(String.valueOf(ac.facts.getMinLongNonZero()), dtf), LocalDate.parse(String.valueOf(ac.facts.getMaxLong()), dtf));
			return true;
		}

		if (ac.facts.groupingSeparators == 0 && ac.facts.getMinLongNonZero() != Long.MAX_VALUE && plausibleYear(realSamples)) {
			// Sometimes a Long is not a Long but it is really a date (yyyy)
			final TypeInfo newTypeInfo = new TypeInfo(null, "\\d{4}", FTAType.LOCALDATE, "yyyy", false, "yyyy");
			switchToDate(newTypeInfo, LocalDate.of((int)ac.facts.getMinLongNonZero(), 1, 1), LocalDate.of((int)ac.facts.getMaxLong(), 1, 1));
			return true;
		}

		return false;
	}

	// Called to finalize a LONG type determination when NOT a Semantic type
	void finalizeLong(final long realSamples) {
		if (KnownTypes.ID.ID_LONG == ac.facts.getMatchTypeInfo().id && ac.facts.getMatchTypeInfo().typeModifier == null && ac.facts.getMinLong() < 0) {
			ac.facts.setMatchTypeInfo(ac.knownTypes.negation(ac.facts.getMatchTypeInfo().getRegExp()));
			ac.ctxdebug("Type determination", "now with sign {}", ac.facts.getMatchTypeInfo());
		}

		if (isReallyDate(realSamples))
			return;

		if (ac.facts.getCardinality().size() == 2 && ac.facts.getMinLong() == 0 && ac.facts.getMaxLong() == 1) {
			// boolean by any other name
			ac.facts.minBoolean = "0";
			ac.facts.maxBoolean = "1";
			ac.facts.setMatchTypeInfo(ac.knownTypes.getByID(KnownTypes.ID.ID_BOOLEAN_ONE_ZERO));
			return;
		}

		if (!ac.facts.getMatchTypeInfo().isSemanticType() && ac.facts.groupingSeparators != 0 && !ac.facts.getMatchTypeInfo().hasGrouping()) {
			ac.facts.setMatchTypeInfo(ac.knownTypes.grouping(ac.facts.getMatchTypeInfo().getRegExp()));
			ac.ctxdebug("Type determination", "now with grouping {}", ac.facts.getMatchTypeInfo());
		}

		if (!ac.facts.getMatchTypeInfo().isSemanticType()) {
			// Create a new TypeInfo - we don't want to change a predefined one!
			ac.facts.setMatchTypeInfo(new TypeInfo(ac.facts.getMatchTypeInfo()));
			ac.facts.getMatchTypeInfo().setRegExp(freezeNumeric(ac.facts.getMatchTypeInfo().getRegExp()));
		}

		// We may have a Semantic Type already identified but see if there is a better Finite Semantic type
		final LogicalTypeFinite logicalFinite = matchFiniteTypes(FTAType.LONG, ac.facts.getCardinality());
		if (logicalFinite != null)
			ac.facts.confidence = logicalFinite.getConfidence(ac.facts.matchCount, realSamples, ac.analyzerContext);

		if (!ac.facts.getMatchTypeInfo().isSemanticType())
			for (final LogicalTypeRegExp logical : ac.regExpTypes) {
				if (logical.acceptsBaseType(FTAType.LONG) &&
						logical.isMatch(ac.facts.getMatchTypeInfo().getRegExp()) &&
						logical.analyzeSet(ac.analyzerContext, ac.facts.matchCount, realSamples, ac.facts.getMatchTypeInfo().getRegExp(), ac.facts.calculateFacts(), ac.facts.getCardinality(), ac.facts.outliers, ac.tokenStreams, ac.analysisConfig).isValid()) {
					ac.facts.setMatchTypeInfo(new TypeInfo(logical.getRegExp(), logical.getBaseType(), logical.getSemanticType(), ac.facts.getMatchTypeInfo()));
					ac.facts.confidence = logical.getConfidence(ac.facts.matchCount, realSamples, ac.analyzerContext);
					ac.ctxdebug("Type determination", "was LONG, matchTypeInfo - {}", ac.facts.getMatchTypeInfo());
					break;
				}
			}

		// Do we want to back out to a DOUBLE?  Only do this if we have seen a reasonable number of samples and have
		// not blown out the maximum cardinality.
		if (!ac.facts.getMatchTypeInfo().isSemanticType() && realSamples >= ac.analysisConfig.getDetectWindow() &&
				ac.facts.getCardinalityOverflow() == null &&
				(ac.facts.confidence < ac.analysisConfig.getThreshold()/100.0 ||
						(ac.analysisConfig.isEnabled(Feature.NUMERIC_WIDENING) && !ac.facts.outliers.isEmpty() && (new OutlierAnalysis(ac.facts.outliers, ac.facts.getMatchTypeInfo())).doubles == ac.facts.outliers.size()))) {
			// We thought it was an integer field, but on reflection it does not feel like it
			conditionalBackoutToPattern(realSamples, ac.facts.getMatchTypeInfo());
			ac.facts.confidence = (double) ac.facts.matchCount / realSamples;
		}

		// If it is a Semantic type then the outliers are invalid, if it is not a Semantic type then it is garbage and so is also invalid
		if (!ac.facts.outliers.isEmpty()) {
			ac.facts.invalid.putAll(ac.facts.outliers);
			ac.facts.outliers.clear();
		}
	}

	void killInvalidDates() {
		final Iterator<Entry<String, Long>> it = ac.facts.getCardinality().entrySet().iterator();

		while (it.hasNext()) {
			final Entry<String, Long> entry = it.next();
			boolean kill = false;
			try {
				kill = !typeTracker.trackDateTime(entry.getKey().trim(), ac.facts.getMatchTypeInfo(), false, 1);
			}
			catch (DateTimeException e) {
				kill = true;
			}

			if (kill) {
				ac.facts.invalid.put(entry.getKey(), entry.getValue());
				ac.facts.matchCount -= entry.getValue();
				it.remove();
			}
		 }
	}

	void finalizeBoolean(final long realSamples) {
		if ((ac.facts.getCardinality().size() == 1 && ac.facts.getMatchTypeInfo().id == KnownTypes.ID.ID_BOOLEAN_Y_N)
				|| (ac.facts.confidence < .98 && ac.facts.outliers.size() >= 2)) {
			backoutToString(realSamples);
			ac.facts.confidence = (double) ac.facts.matchCount / realSamples;
		}
	}

	// Called to finalize a DOUBLE type determination when NOT a Semantic type
	void finalizeDouble(final long realSamples) {
		if (ac.facts.minDouble < 0.0) {
			ac.facts.setMatchTypeInfo(ac.knownTypes.negation(ac.facts.getMatchTypeInfo().getRegExp()));
			ac.ctxdebug("Type determination", "now with sign {}", ac.facts.getMatchTypeInfo());
		}

		if (ac.facts.groupingSeparators != 0 && !ac.facts.getMatchTypeInfo().hasGrouping()) {
			ac.facts.setMatchTypeInfo(ac.knownTypes.grouping(ac.facts.getMatchTypeInfo().getRegExp()));
			ac.ctxdebug("Type determination", "now with grouping {}", ac.facts.getMatchTypeInfo());
		}

		for (final LogicalTypeRegExp logical : ac.regExpTypes) {
			if (logical.acceptsBaseType(FTAType.DOUBLE) &&
					logical.isMatch(ac.facts.getMatchTypeInfo().getRegExp()) &&
					logical.analyzeSet(ac.analyzerContext, ac.facts.matchCount, realSamples, ac.facts.getMatchTypeInfo().getRegExp(), ac.facts.calculateFacts(), ac.facts.getCardinality(), ac.facts.outliers, ac.tokenStreams, ac.analysisConfig).isValid()) {
				ac.facts.setMatchTypeInfo(new TypeInfo(logical.getRegExp(), logical.getBaseType(), logical.getSemanticType(), ac.facts.getMatchTypeInfo()));
				ac.facts.confidence = logical.getConfidence(ac.facts.matchCount, realSamples, ac.analyzerContext);
				break;
			}
		}

		if (!ac.facts.getMatchTypeInfo().isSemanticType() && realSamples >= ac.analysisConfig.getDetectWindow() &&
				ac.facts.outliers.size() > (realSamples >= 100 ? 2 : 1) &&
				(ac.facts.confidence < ac.analysisConfig.getThreshold()/100.0 ||
						(ac.analysisConfig.isEnabled(Feature.NUMERIC_WIDENING) && !ac.facts.outliers.isEmpty() && (new OutlierAnalysis(ac.facts.outliers, ac.facts.getMatchTypeInfo())).doubles == ac.facts.outliers.size()))) {
			// We thought it was an double field, but on reflection it does not feel like it
			conditionalBackoutToPattern(realSamples, ac.facts.getMatchTypeInfo());
			ac.facts.confidence = (double) ac.facts.matchCount / realSamples;
		}

		// All outliers are actually invalid
		if (!ac.facts.outliers.isEmpty()) {
			ac.facts.invalid.putAll(ac.facts.outliers);
			ac.facts.outliers.clear();
		}
	}

	private boolean isInteresting(final String input) {
		return input != null && !input.isBlank();
	}

	TextAnalysisResult buildResult() throws FTAPluginException, FTAUnsupportedLocaleException {
		// Compute our confidence
		final long realSamples = ac.facts.sampleCount - (ac.facts.nullCount + ac.facts.blankCount);

		// Check to see if we are all blanks or all nulls
		if (ac.facts.blankCount == ac.facts.sampleCount || ac.facts.nullCount == ac.facts.sampleCount || ac.facts.blankCount + ac.facts.nullCount == ac.facts.sampleCount) {
			if (ac.facts.nullCount == ac.facts.sampleCount)
				ac.facts.setMatchTypeInfo(ac.knownTypes.getByID(KnownTypes.ID.ID_NULL));
			else if (ac.facts.blankCount == ac.facts.sampleCount)
				ac.facts.setMatchTypeInfo(ac.knownTypes.getByID(KnownTypes.ID.ID_BLANK));
			else
				ac.facts.setMatchTypeInfo(ac.knownTypes.getByID(KnownTypes.ID.ID_BLANKORNULL));
			ac.facts.confidence = ac.facts.sampleCount >= 10 ? 1.0 : 0.0;
		}
		else {
			ac.facts.confidence = (double) ac.facts.matchCount / realSamples;
		}

		boolean backedOutRegExp = false;

		// Do we need to back out from any of our Semantic type determinations.  Most of the time this backs out of
		// Infinite type determinations (since we have not yet declared it to be a Finite type).  However it is possible
		// that this is a subsequent call to getResult()!!
		final long outlierCount = ac.facts.outliers.values().stream().mapToLong(l -> l).sum();
		if (ac.facts.getMatchTypeInfo().isSemanticType() && !ac.facts.getMatchTypeInfo().isForce()) {
			final LogicalType logical = ac.plugins.getRegistered(ac.facts.getMatchTypeInfo().getSemanticType());

			final PluginAnalysis pluginAnalysis = logical.analyzeSet(ac.analyzerContext, ac.facts.matchCount, realSamples, ac.facts.getMatchTypeInfo().getRegExp(), ac.facts.calculateFacts(), ac.facts.getCardinality(), ac.facts.outliers, ac.tokenStreams, ac.analysisConfig);
			if (!pluginAnalysis.isValid()) {
				if (logical.acceptsBaseType(FTAType.STRING) || logical.acceptsBaseType(FTAType.LONG) || logical.acceptsBaseType(FTAType.DOUBLE)) {
					backout(logical, realSamples, pluginAnalysis);
					if (logical instanceof LogicalTypeRegExp)
						backedOutRegExp = true;
				}
			}
			else {
				// Update our Regular Expression - since it may have changed based on all the data observed
				ac.facts.getMatchTypeInfo().setRegExp(logical.getRegExp());
				ac.facts.matchCount += outlierCount - ac.facts.outliers.values().stream().mapToLong(l -> l).sum();
				ac.facts.confidence = logical.getConfidence(ac.facts.matchCount, realSamples, ac.analyzerContext);
			}
		}

		final FiniteMap cardinalityUpper = new FiniteMap(ac.facts.getCardinality());
		final FTAType currentType = ac.facts.getMatchTypeInfo().getBaseType();

		if (FTAType.LONG.equals(currentType))
			finalizeLong(realSamples);
		else if (FTAType.BOOLEAN.equals(currentType))
			finalizeBoolean(realSamples);
		else if (FTAType.DOUBLE.equals(currentType) && !ac.facts.getMatchTypeInfo().isSemanticType())
			finalizeDouble(realSamples);
		else if (FTAType.STRING.equals(currentType))
			finalizeString(realSamples, cardinalityUpper);

		if (FTAType.STRING.equals(ac.facts.getMatchTypeInfo().getBaseType())) {
			if (ac.facts.getMatchTypeInfo().isSemanticType()) {
				final LogicalType logical = ac.plugins.getRegistered(ac.facts.getMatchTypeInfo().getSemanticType());
				boolean recalcConfidence = false;

				// Sweep the outliers - flipping them to invalid if they do not pass the relaxed isValid definition
				for (final Map.Entry<String, Long> entry : ac.facts.outliers.entrySet()) {
					// Split the outliers to either invalid entries or valid entries
					if (logical.isValid(entry.getKey(), false, entry.getValue())) {
						typeTracker.addValid(entry.getKey(), entry.getValue());
						ac.facts.matchCount += entry.getValue();
						recalcConfidence = true;
					}
					else
						typeTracker.addInvalid(entry);
				}

				if (recalcConfidence)
					ac.facts.confidence = logical.getConfidence(ac.facts.matchCount, realSamples, ac.analyzerContext);
				ac.facts.outliers.clear();
			}
			else {
				// We would really like to say something better than it is a String!
				boolean updated = false;

				// If we are currently matching everything then flip to a better Regular Expression based on Stream analysis if possible
				if (ac.facts.matchCount == realSamples && !ac.tokenStreams.isAnyShape()) {
					final String newRegExp = ac.tokenStreams.getRegExp(false);
					if (newRegExp != null) {
						ac.facts.setMatchTypeInfo(new TypeInfo(null, newRegExp, FTAType.STRING, null, false, null));
						ac.ctxdebug("Type determination", "updated based on Stream analysis {}", ac.facts.getMatchTypeInfo());
					}
				}

				if (!backedOutRegExp)
					updated = checkRegExpTypes(FTAType.STRING);

				final long interestingSamples = ac.facts.sampleCount - (ac.facts.nullCount + ac.facts.blankCount);

				// Try a nice discrete enum
				if (!updated && cardinalityUpper.size() > 1 && cardinalityUpper.size() <= MAX_ENUM_SIZE && (interestingSamples > ac.reflectionSamples || interestingSamples / cardinalityUpper.size() >= 3)) {
					// Rip through the enum doing some basic sanity checks
					RegExpGenerator gen = new RegExpGenerator(MAX_ENUM_SIZE, ac.locale);
					boolean fail = false;
					int excessiveDigits = 0;

					for (final String elt : cardinalityUpper.keySet()) {
						final int length = elt.length();
						// Give up if any one of the strings is too long
						if (length > 40) {
							fail = true;
							break;
						}
						int digits = 0;
						for (int i = 0; i < length; i++) {
							final char ch = elt.charAt(i);
							// Give up if we have some non-expected character
							if (!Character.isAlphabetic(ch) && !Character.isDigit(ch) &&
									ch != '-' && ch != '_' && ch != ' ' && ch != ';' && ch != '.' && ch != ',' && ch != '/' && ch != '(' && ch != ')') {
								fail = true;
								break;
							}

							// Record how many of the elements have 3 or more digits
							if (Character.isDigit(ch)) {
								digits++;
								if (digits == 3)
									excessiveDigits++;
							}
						}

						if (fail)
							break;
						gen.train(elt);
					}

					// If we did not find any reason to reject, output it as an enum
					if (excessiveDigits != cardinalityUpper.size() && !fail) {
						// If we have a significant # of samples and a small number of non-numerics with distinct values attempt to remove outliers
						if (interestingSamples > 1000 && cardinalityUpper.size() < 20 && !gen.isDigit()) {
							final Map<String, Long> sorted = Utils.sortByValue(cardinalityUpper);
							for (final Map.Entry<String, Long> elt : sorted.entrySet()) {
								if (elt.getValue() < 3 && elt.getKey().length() > 3 && TextAnalyzer.distanceLevenshtein(elt.getKey(), cardinalityUpper.keySet()) <= 1) {
									cardinalityUpper.remove(elt.getKey());
									ac.facts.getCardinality().entrySet()
									  .removeIf(entry -> entry.getKey().equalsIgnoreCase(elt.getKey()));
									ac.facts.outliers.put(elt.getKey(), elt.getValue());
									ac.facts.matchCount -= elt.getValue();
									ac.facts.confidence = (double) ac.facts.matchCount / realSamples;
								}
							}

							// Regenerate the enum without the outliers removed
							gen = new RegExpGenerator(MAX_ENUM_SIZE, ac.locale);
							for (final String elt : cardinalityUpper.keySet())
								gen.train(elt);
						}

						ac.facts.setMatchTypeInfo(new TypeInfo(null, gen.getResult(), FTAType.STRING, ac.facts.getMatchTypeInfo().typeModifier, false, null));
						updated = true;

						// Now we have mapped to an enum we need to check again if this should be matched to a Semantic type
						for (final LogicalTypeRegExp logical : ac.regExpTypes) {
							if (logical.acceptsBaseType(FTAType.STRING) &&
									logical.isMatch(ac.facts.getMatchTypeInfo().getRegExp()) &&
									logical.analyzeSet(ac.analyzerContext, ac.facts.matchCount, realSamples, ac.facts.getMatchTypeInfo().getRegExp(), ac.facts.calculateFacts(), ac.facts.getCardinality(), ac.facts.outliers, ac.tokenStreams, ac.analysisConfig).isValid()) {
								ac.facts.setMatchTypeInfo(new TypeInfo(logical.getRegExp(), logical.getBaseType(), logical.getSemanticType(), ac.facts.getMatchTypeInfo()));
								ac.facts.confidence = logical.getConfidence(ac.facts.matchCount, realSamples, ac.analyzerContext);
								break;
							}
						}
					}
				}

				// Check to see whether the most common shape matches our regExp and test to see if this valid
				if (!updated && ac.tokenStreams.size() > 1) {
					final TokenStream best = ac.tokenStreams.getBest();
					final String regExp = best.getRegExp(false);
					for (final LogicalTypeRegExp logical : ac.regExpTypes) {
						if (logical.acceptsBaseType(FTAType.STRING) &&
								logical.isMatch(regExp) &&
								logical.analyzeSet(ac.analyzerContext, best.getOccurrences(), realSamples, ac.facts.getMatchTypeInfo().getRegExp(), ac.facts.calculateFacts(), ac.facts.getCardinality(), ac.facts.outliers, ac.tokenStreams, ac.analysisConfig).isValid()) {
							ac.facts.setMatchTypeInfo(new TypeInfo(regExp, logical.getBaseType(), logical.getSemanticType(), ac.facts.getMatchTypeInfo()));
							ac.facts.matchCount = best.getOccurrences();
							ac.facts.confidence = logical.getConfidence(ac.facts.matchCount, realSamples, ac.analyzerContext);
							updated = true;
							break;
						}
					}
				}

				// Qualify Alpha or Alnum with a min and max length
				if (!updated && (KnownTypes.PATTERN_ALPHA_VARIABLE.equals(ac.facts.getMatchTypeInfo().getRegExp()) || KnownTypes.PATTERN_ALPHANUMERIC_VARIABLE.equals(ac.facts.getMatchTypeInfo().getRegExp()))) {
					String newPattern = ac.facts.getMatchTypeInfo().getRegExp();
					newPattern = newPattern.substring(0, newPattern.length() - 1) + lengthQualifier(ac.facts.minTrimmedLength, ac.facts.maxTrimmedLength);
					ac.facts.setMatchTypeInfo(new TypeInfo(null, newPattern, FTAType.STRING, ac.facts.getMatchTypeInfo().typeModifier, false, null));
					updated = true;
				}

				// Qualify random string with a min and max length
				if (!updated && KnownTypes.PATTERN_ANY_VARIABLE.equals(ac.facts.getMatchTypeInfo().getRegExp())) {
					final String newPattern = KnownTypes.freezeANY(ac.facts.minTrimmedLength, ac.facts.maxTrimmedLength, ac.facts.minRawNonBlankLength, ac.facts.maxRawNonBlankLength, ac.facts.leadingWhiteSpace, ac.facts.trailingWhiteSpace, ac.facts.multiline);
					ac.facts.setMatchTypeInfo(new TypeInfo(null, newPattern, FTAType.STRING, ac.facts.getMatchTypeInfo().typeModifier, false, null));
					updated = true;
				}
			}
		}

		checkDateTimeTypes(ac.facts.getMatchTypeInfo().getBaseType());
		checkRegExpTypes(ac.facts.getMatchTypeInfo().getBaseType());

		// Only attempt to do key identification if we have not already been told the answer
		if (ac.facts.keyConfidence == null) {
			ac.facts.keyConfidence = 0.0;
			if (ac.facts.sampleCount > MIN_SAMPLES_FOR_KEY && ac.analysisConfig.getMaxCardinality() >= MIN_SAMPLES_FOR_KEY / 2 &&
					(ac.facts.getCardinality().size() == ac.analysisConfig.getMaxCardinality() || ac.facts.getCardinality().size() == ac.facts.sampleCount) &&
					ac.facts.blankCount == 0 && ac.facts.nullCount == 0 &&
					((ac.facts.getMatchTypeInfo().isSemanticType() && "GUID".equals(ac.facts.getMatchTypeInfo().getSemanticType())) ||
					(ac.facts.getMatchTypeInfo().typeModifier == null &&
					((FTAType.STRING.equals(ac.facts.getMatchTypeInfo().getBaseType()) && ac.facts.minRawLength == ac.facts.maxRawLength && ac.facts.minRawLength < 32)
							|| FTAType.LONG.equals(ac.facts.getMatchTypeInfo().getBaseType()))))) {
				ac.facts.keyConfidence = 0.9;

				if (ac.facts.getCardinality().size() == ac.analysisConfig.getMaxCardinality())
					for (final Map.Entry<String, Long> entry : ac.facts.getCardinality().entrySet()) {
						if (entry.getValue() != 1) {
							ac.facts.keyConfidence = 0.0;
							break;
						}
					}
			}
		}

		// Only attempt to set uniqueness if we have not already been told the answer
		if (ac.facts.uniqueness == null) {
			if (ac.facts.getCardinality().isEmpty())
				ac.facts.uniqueness = 0.0;
			else if (ac.facts.getCardinality().size() < ac.analysisConfig.getMaxCardinality()) {
				int uniques = 0;
				for (final Map.Entry<String, Long> entry : ac.facts.getCardinality().entrySet()) {
					if (entry.getValue() == 1)
						uniques++;
				}
				ac.facts.uniqueness = (double) uniques / ac.facts.getCardinality().size();
			}
			else if (FTAType.LONG.equals(ac.facts.getMatchTypeInfo().getBaseType()) && (ac.facts.monotonicIncreasing || ac.facts.monotonicDecreasing)) {
				ac.facts.uniqueness = 1.0;
			}
			else
				ac.facts.uniqueness = -1.0;
		}

		// Only attempt to set distinct count if we have not already been told the answer
		if (ac.facts.distinctCount == null) {
			if (ac.facts.getCardinality().size() < ac.analysisConfig.getMaxCardinality())
				ac.facts.distinctCount = (long) ac.facts.getCardinality().size();
			else if (FTAType.LONG.equals(ac.facts.getMatchTypeInfo().getBaseType()) && (ac.facts.monotonicIncreasing || ac.facts.monotonicDecreasing))
				ac.facts.distinctCount = ac.facts.matchCount;
			else
				ac.facts.distinctCount = -1L;
		}

		if (ac.analysisConfig.isEnabled(Feature.APPROX_DISTINCT_COUNT) && ac.facts.approxDistinctCount == null) {
			if (!ac.facts.getCardinality().hasOverflowed())
				ac.facts.approxDistinctCount = (long) ac.facts.getCardinality().size();
			else
				ac.facts.approxDistinctCount = Math.round(ac.facts.getHllSketch().getEstimate());
		}

		if (ac.analysisConfig.isEnabled(Feature.FORMAT_DETECTION))
			ac.facts.streamFormat = Utils.determineStreamFormat(ac.mapper, ac.facts.getCardinality());

		TextAnalysisResult result = null;
		// If we have not detected a Semantic Type but the header looks really good, then try excluding the
		// most popular non-valid entry in the hope that it is something like 'NA', 'XX', etc.
		if (FTAType.STRING.equals(ac.facts.getMatchTypeInfo().getBaseType()) && !ac.facts.getMatchTypeInfo().isSemanticType() && !ac.analyzerContext.isNested() && ac.pluginThreshold != 100 && ac.facts.getCardinality().size() >= 4) {
			for (final LogicalType logical : ac.plugins.getRegisteredSemanticTypes()) {
				final Map<String, Long> details = ac.facts.synthesizeBulk();
				long worst = (ac.facts.sampleCount - (ac.facts.nullCount + ac.facts.blankCount)) / 20;
				Map.Entry<String, Long> worstEntry = null;
				if (logical.getHeaderConfidence(ac.analyzerContext) >= 90) {
					for (final Map.Entry<String, Long> entry : details.entrySet()) {
						if (isInteresting(entry.getKey()) && !logical.isValid(entry.getKey()) && entry.getValue() > worst) {
							worstEntry = entry;
							worst = entry.getValue();
						}
					}
					if (worstEntry != null) {
						details.remove(worstEntry.getKey());
						final TextAnalysisResult newResult = ac.reAnalyzer.apply(details);
						if (newResult.isSemanticType() && newResult.getSemanticType().equals(logical.getSemanticType())) {
							newResult.getFacts().invalid.put(worstEntry.getKey(), worstEntry.getValue());
							if (!ac.facts.outliers.isEmpty()) {
								newResult.getFacts().invalid.putAll(ac.facts.outliers);
								newResult.getFacts().sampleCount += ac.facts.outliers.values().stream().mapToLong(l -> l).sum();
								for (final Map.Entry<String, Long> entry : ac.facts.outliers.entrySet())
									newResult.getFacts().lengths[Math.min(entry.getKey().length(), ac.facts.lengths.length - 1)] += entry.getValue();
							}
							newResult.getFacts().sampleCount += worstEntry.getValue();
							newResult.getFacts().confidence -= 0.05;
							newResult.getFacts().getMatchTypeInfo().setBaseType(FTAType.STRING);
							newResult.getFacts().lengths[Math.min(worstEntry.getKey().length(), ac.facts.lengths.length - 1)] += worstEntry.getValue();
							result = newResult;
							ac.ctxdebug("Type determination", "was STRING, post exclusion analyis ({}, {}), matchTypeInfo {} -> {} ",
									worstEntry.getKey(), worstEntry.getValue(), ac.facts.matchTypeInfo, newResult.getFacts().getMatchTypeInfo());
							break;
						}
					}
				}
			}
		}

		// If we have not detected a Semantic Type - then attempt to exclude outliers and re-analyze
		if (ac.analysisConfig.isEnabled(Feature.COLLECT_STATISTICS) &&
				ac.analysisConfig.isEnabled(Feature.DISTRIBUTIONS) &&
				FTAType.LONG.equals(ac.facts.getMatchTypeInfo().getBaseType()) &&
				!ac.facts.getMatchTypeInfo().isSemanticType() &&
				ac.facts.getCardinality().size() != ac.analysisConfig.getMaxCardinality() &&
				!ac.analyzerContext.isNested() && ac.pluginThreshold != 100 && ac.facts.matchCount >= 20) {
			final Map<String, Long> details = ac.facts.synthesizeBulk();
			final Map<String, Long> outliers = new HashMap<>();
			final Histogram.Entry[] buckets = ac.facts.calculateFacts().getHistogram().getHistogram(10);
			final StringConverter stringConverter = ac.facts.getStringConverter();

			// Associate each bucket with a cluster - so that we can do the outlier detection
			Histogram.tagClusters(buckets);

			// Identify any outliers based on 'density-based clustering' (using the Histograms to generate the clusters)
			for (final Map.Entry<String, Long> entry : details.entrySet()) {
				if (Utils.isNumeric(entry.getKey())) {
					final double value = stringConverter.toDouble(entry.getKey());
					final Histogram.Entry bucket = Histogram.getBucket(buckets, value);
					// Scratch any cluster with less than 2% of the samples
					if (bucket.getClusterPercent() < .02)
						outliers.put(entry.getKey(), entry.getValue());
				}
			}

			// If we identified any outliers then re-analyze using the 'cleaned' set and see if we have success
			if (!outliers.isEmpty()) {
				details.keySet().removeAll(outliers.keySet());
				final TextAnalysisResult newResult = ac.reAnalyzer.apply(details);

				final FTAType newType = newResult.getFacts().getMatchTypeInfo().getBaseType();

				if (newResult.isSemanticType() || newType.isDateOrTimeType()) {
					// We found a new Semantic Type so add the old invalids & outliers to the current invalids and update the sample count
					for (final Map.Entry<String, Long> entry : outliers.entrySet()) {
						newResult.getFacts().outliers.mergeIfSpace(entry.getKey(), entry.getValue(), Long::sum);
						newResult.getFacts().lengths[Math.min(entry.getKey().length(), ac.facts.lengths.length - 1)] += entry.getValue();
					}
					newResult.getFacts().sampleCount += outliers.values().stream().mapToLong(l -> l).sum();
					for (final Map.Entry<String, Long> entry : ac.facts.invalid.entrySet()) {
						newResult.getFacts().invalid.mergeIfSpace(entry.getKey(), entry.getValue(), Long::sum);
						newResult.getFacts().lengths[Math.min(entry.getKey().length(), ac.facts.lengths.length - 1)] += entry.getValue();
					}
					newResult.getFacts().sampleCount += ac.facts.invalid.values().stream().mapToLong(l -> l).sum();
					newResult.getFacts().confidence -= 0.05;
					result = newResult;
					ac.ctxdebug("Type determination", "was LONG, post outlier analyis, matchTypeInfo - {}", newResult.getFacts().getMatchTypeInfo());
				}
			}
		}

		// If we have not detected a Semantic Type and we have a double masquerading as a Long then re-analyze with a long set
		if (ac.analysisConfig.isEnabled(Feature.COLLECT_STATISTICS) &&
				FTAType.DOUBLE.equals(ac.facts.getMatchTypeInfo().getBaseType()) &&
				!ac.facts.getMatchTypeInfo().isSemanticType() &&
				ac.facts.allZeroes &&
				!ac.analyzerContext.isNested() && ac.pluginThreshold != 100 && ac.facts.matchCount >= 20) {
			final Map<String, Long> doubleDetails = ac.facts.synthesizeBulk();
			final Map<String, Long> details = new HashMap<>();
			final StringConverter stringConverter = ac.facts.getStringConverter();

			for (final Map.Entry<String, Long> entry : doubleDetails.entrySet())
				if (entry.getKey() == null || entry.getKey().isBlank())
					details.put(entry.getKey(), entry.getValue());
				else
					details.put(String.valueOf(((Double) stringConverter.getValue(entry.getKey())).longValue()), entry.getValue());

			final TextAnalysisResult newResult = ac.reAnalyzer.apply(details);

			final FTAType newType = newResult.getFacts().getMatchTypeInfo().getBaseType();

			if (newResult.isSemanticType()) {
				ac.facts.getMatchTypeInfo().setSemanticType(newResult.getSemanticType());
				ac.ctxdebug("Type determination", "was DOUBLE, post LONG conversion, matchTypeInfo - {}", newResult.getFacts().getMatchTypeInfo());
			}
			else if (FTAType.LOCALDATE.equals(newType)) {
				final TypeInfo interimTypeInfo = newResult.getFacts().getMatchTypeInfo();
				final String trailingZeroes = "." + Utils.repeat('0', ac.facts.zeroesLength);
				final String updatedModifier = interimTypeInfo.typeModifier + "'" + trailingZeroes + "'";
				final TypeInfo newTypeInfo = new TypeInfo(null, newResult.getFacts().getMatchTypeInfo().getRegExp() + "\\Q" + trailingZeroes + "\\E", FTAType.LOCALDATE, updatedModifier, false, updatedModifier);
				final DateTimeFormatter interimFormatter = ac.dateTimeParser.ofPattern(interimTypeInfo.format);
				switchToDate(newTypeInfo,
						LocalDate.parse(String.valueOf(Double.valueOf(ac.facts.minDoubleNonZero).longValue()), interimFormatter),
						LocalDate.parse(String.valueOf(Double.valueOf(ac.facts.maxDouble).longValue()), interimFormatter));
				ac.ctxdebug("Type determination", "was DOUBLE, post LONG conversion, matchTypeInfo - {}", newResult.getFacts().getMatchTypeInfo());
			}
		}

		if (ac.analysisConfig.isEnabled(Feature.DEFAULT_SEMANTIC_TYPES)) {
			final PluginDefinition pluginDefinition = PluginDefinition.findByName("IDENTIFIER");
			final LogicalType identifier = LogicalTypeFactory.newInstance(pluginDefinition, ac.analysisConfig);

			if (!ac.facts.getMatchTypeInfo().isSemanticType() && !ac.analyzerContext.isNested() &&
					((ac.facts.external.getKeyConfidence() != null && ac.facts.external.getKeyConfidence() == 1.0) ||
					(ac.facts.uniqueness == 1.0 && ac.facts.matchCount >= 20 &&
						identifier.analyzeSet(ac.analyzerContext, ac.facts.matchCount, realSamples, ac.facts.getMatchTypeInfo().getRegExp(), ac.facts.calculateFacts(), ac.facts.getCardinality(), ac.facts.outliers, ac.tokenStreams, ac.analysisConfig).isValid()))) {
				ac.facts.getMatchTypeInfo().setRegExp(ac.facts.getRegExp());
				ac.facts.getMatchTypeInfo().setSemanticType(identifier.getSemanticType());
				if (ac.facts.external.getKeyConfidence() == null) {
					ac.facts.confidence = ac.facts.external.getTotalCount() == ac.facts.sampleCount ? 1.0 : identifier.getConfidence(ac.facts.matchCount, realSamples, ac.analyzerContext);
					ac.facts.keyConfidence = ac.facts.confidence;
				}
				else
					ac.facts.confidence = 1.0;

				ac.ctxdebug("Type determination", "post Uniqueness analyis, matchTypeInfo - {}", ac.facts.getMatchTypeInfo());
			}
		}

		// If we are in SIMPLE mode (i.e. not Bulk) and we have not detected a Semantic Type - try replaying accumulated set in Bulk mode,
		// this has the potential to pick up entries where the first <n> (by default 20 are misleading).
		if (FTAType.STRING.equals(ac.facts.getMatchTypeInfo().getBaseType()) && !ac.facts.getMatchTypeInfo().isSemanticType() && ac.analysisConfig.getTrainingMode() == AnalysisConfig.TrainingMode.SIMPLE && ac.pluginThreshold != 100) {
			final TextAnalysisResult bulkResult = ac.reAnalyzer.apply(ac.facts.synthesizeBulk());
			if (bulkResult.isSemanticType() || bulkResult.getType() != ac.facts.getMatchTypeInfo().getBaseType()) {
				if (!ac.facts.outliers.isEmpty()) {
					bulkResult.getFacts().invalid.putAll(ac.facts.outliers);
					bulkResult.getFacts().sampleCount += ac.facts.outliers.values().stream().mapToLong(l -> l).sum();
				}
				bulkResult.getFacts().confidence -= 0.05;
				result = bulkResult;
				ac.ctxdebug("Type determination", "was STRING, post Bulk analyis, matchTypeInfo - {}", bulkResult.getFacts().getMatchTypeInfo());
			}
		}

		if (result == null)
			result = new TextAnalysisResult(ac.analyzerContext.getStreamName(), ac.facts.calculateFacts(), ac.analyzerContext.getDateResolutionMode(), ac.analysisConfig, ac.tokenStreams);

		return result;
	}

	void finalizeString(final long realSamples, final FiniteMap cardinalityUpper) {
		// Build Cardinality map ignoring case (and white space)
		for (final Map.Entry<String, Long> entry : ac.facts.getCardinality().entrySet()) {
			final String key = entry.getKey().toUpperCase(ac.locale).trim();
			cardinalityUpper.merge(key, entry.getValue(), Long::sum);
		}
		// Sort the results so that we consider the most frequent first (we will hopefully fail faster)
		cardinalityUpper.sortByValue();

		// We may have a Semantic Type already identified but see if there is a better Finite Semantic type
		final LogicalTypeFinite logical = matchFiniteTypes(FTAType.STRING, cardinalityUpper);
		if (logical != null)
			ac.facts.confidence = logical.getConfidence(ac.facts.matchCount, realSamples, ac.analyzerContext);

		// Fixup any likely enums
		if (!ac.facts.getMatchTypeInfo().isSemanticType() && cardinalityUpper.size() < MAX_ENUM_SIZE && !ac.facts.outliers.isEmpty() && ac.facts.outliers.size() < 10) {
			boolean updated = false;

			final Set<String> killSet = new HashSet<>();

			// Sort the outliers so that we consider the most frequent first
			ac.facts.outliers.sortByValue();

			// Iterate through the outliers adding them to the core cardinality set if we think they are reasonable.
			for (final Map.Entry<String, Long> entry : ac.facts.outliers.entrySet()) {
				final String key = entry.getKey();
				final String keyUpper = key.toUpperCase(ac.locale).trim();
				String validChars = " _-";
				boolean skip = false;

				// We are wary of outliers that only have one instance, do an extra check that the characters in the
				// outlier exist in the real set.
				if (entry.getValue() == 1) {
					boolean onlyAlphas = true;
					boolean onlyNumeric = true;
					// Build the universe of valid characters
					for (final String existing : cardinalityUpper.keySet()) {
						for (int i = 0; i < existing.length(); i++) {
							final char ch = existing.charAt(i);
							if (onlyAlphas && !Character.isAlphabetic(ch))
								onlyAlphas = false;
							if (onlyNumeric && !Character.isDigit(ch))
								onlyNumeric = false;
							if (!Character.isAlphabetic(ch) && !Character.isDigit(ch))
								if (validChars.indexOf(ch) == -1)
									validChars += ch;
						}
					}
					for (int i = 0; i < keyUpper.length(); i++) {
						final char ch = keyUpper.charAt(i);
						if ((onlyAlphas && !Character.isAlphabetic(ch)) || (onlyNumeric && !Character.isDigit(ch)) ||
								(!Character.isAlphabetic(ch) && !Character.isDigit(ch) && validChars.indexOf(ch) == -1)) {
							skip = true;
							break;
						}
					}
				}
				else
					skip = false;

				if (!skip) {
					cardinalityUpper.merge(keyUpper, entry.getValue(), Long::sum);
					killSet.add(key);
					updated = true;
				}
			}

			// If we updated the set then we need to remove the outliers we OK'd and
			// also update the pattern to reflect the looser definition
			if (updated) {
				final FiniteMap remainingOutliers = new FiniteMap(ac.facts.outliers);
				remainingOutliers.putAll(ac.facts.outliers);
				for (final String elt : killSet)
					remainingOutliers.remove(elt);

				// This resets the Cardinality set to include all outliers
				backoutToPattern(realSamples, KnownTypes.PATTERN_ANY_VARIABLE);
				// Fix the outliers
				ac.facts.outliers = remainingOutliers;
				// Fix the cardinality set
				for (final String elt : ac.facts.outliers.keySet())
					ac.facts.getCardinality().remove(elt);
				ac.facts.matchCount -= remainingOutliers.values().stream().mapToLong(l-> l).sum();
				ac.facts.confidence = (double) ac.facts.matchCount / realSamples;
			}
		}

		// Need to evaluate if we got the type wrong
		if (!ac.facts.getMatchTypeInfo().isSemanticType() && !ac.facts.outliers.isEmpty() && ac.facts.getMatchTypeInfo().isAlphabetic() && realSamples >= ac.reflectionSamples) {
			conditionalBackoutToPattern(realSamples, ac.facts.getMatchTypeInfo());
			ac.facts.confidence = (double) ac.facts.matchCount / realSamples;

			// Rebuild the cardinalityUpper Map
			cardinalityUpper.clear();
			for (final Map.Entry<String, Long> entry : ac.facts.getCardinality().entrySet())
				cardinalityUpper.merge(entry.getKey().toUpperCase(ac.locale).trim(), entry.getValue(), Long::sum);
		}
	}
}
