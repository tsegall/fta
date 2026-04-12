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

import java.time.format.DateTimeParseException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.LoggerFactory;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.Utils;
import com.cobber.fta.dates.DateTimeParserResult;

/**
 * TypeDeterminer collects per-sample escalation data during the detect window and then
 * decides which base/semantic type best describes the stream.
 * Extracted from {@link TextAnalyzer} as part of the God-Class decomposition.
 */
class TypeDeterminer {

	/**
	 * An Escalation contains three regExps in order of increasing genericity. So for example the following 3 regExps:
	 * [\d{5}, \d+, .*]
	 * represent a 5-digit integer field escalating up to any string.
	 */
	static class Escalation {
		StringBuilder[] level;
		TypeInfo typeInfo;

		@Override
		public int hashCode() {
			return level[0].toString().hashCode() + 7 * level[1].toString().hashCode() + 11 * level[2].toString().hashCode();
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final Escalation other = (Escalation) obj;
			return Arrays.equals(level, other.level);
		}

		Escalation() {
			level = new StringBuilder[3];
		}
	}

	private final AnalysisContext ac;
	private final TypeTracker typeTracker;

	/** Per-sample escalation triplets accumulated during the detect window. */
	private List<Escalation> detectWindowEscalations;

	/** Frequency maps (one per level 0/1/2) built by collapse() and consumed by getBest(). */
	private List<Map<String, Integer>> frequencies = new ArrayList<>();

	/** Count of samples that look like a date/time format. */
	private int possibleDateTime;

	TypeDeterminer(final AnalysisContext ac, final TypeTracker typeTracker) {
		this.ac = ac;
		this.typeTracker = typeTracker;
		this.detectWindowEscalations = new ArrayList<>(ac.analysisConfig.getDetectWindow());
	}

	private void debug(final String format, final Object... arguments) {
		if (ac.analysisConfig.getDebug() >= 2)
			LoggerFactory.getLogger("com.cobber.fta").debug(format, arguments);
	}

	private boolean isInteresting(final String input) {
		return input != null && !input.isBlank();
	}

	private void updateNumericPattern(final Escalation escalation, final SignStatus signStatus, final int numericDecimalSeparators, final int possibleExponentSeen, final boolean nonLocalizedDouble) {
		if (signStatus == SignStatus.TRAILING_MINUS)
			escalation.typeInfo = ac.knownTypes.getByID(numericDecimalSeparators == 1 ? KnownTypes.ID.ID_SIGNED_DOUBLE_TRAILING : KnownTypes.ID.ID_SIGNED_LONG_TRAILING);
		else {
			final boolean numericSigned = signStatus == SignStatus.LOCALE_STANDARD || signStatus == SignStatus.LEADING_SIGN;

			if (numericDecimalSeparators == 1) {
				if (possibleExponentSeen == -1) {
					if (nonLocalizedDouble)
						escalation.typeInfo = ac.knownTypes.getByID(numericSigned ? KnownTypes.ID.ID_SIGNED_DOUBLE_NL : KnownTypes.ID.ID_DOUBLE_NL);
					else
						escalation.typeInfo = ac.knownTypes.getByID(numericSigned ? KnownTypes.ID.ID_SIGNED_DOUBLE : KnownTypes.ID.ID_DOUBLE);
				} else {
					escalation.typeInfo = ac.knownTypes.getByID(numericSigned ? KnownTypes.ID.ID_SIGNED_DOUBLE_WITH_EXPONENT : KnownTypes.ID.ID_DOUBLE_WITH_EXPONENT);
				}
			} else {
				if (possibleExponentSeen == -1)
					escalation.typeInfo = ac.knownTypes.getByID(numericSigned ? KnownTypes.ID.ID_SIGNED_LONG : KnownTypes.ID.ID_LONG);
				else
					escalation.typeInfo = ac.knownTypes.getByID(numericSigned ? KnownTypes.ID.ID_SIGNED_DOUBLE_WITH_EXPONENT : KnownTypes.ID.ID_DOUBLE_WITH_EXPONENT);
			}
		}

		escalation.level[1] = new StringBuilder(escalation.typeInfo.getRegExp());
		escalation.level[2] = new StringBuilder(ac.knownTypes.negation(escalation.typeInfo.getRegExp()).getRegExp());
	}

	/**
	 * Build an escalation entry for the given input during the detect window.
	 * Called from TextAnalyzer.trainCore() once the type is not yet determined.
	 */
	void buildEscalation(final String rawInput, final String trimmed, final long count) {
		for (int i = 0; i < count; i++)
			ac.raw.add(rawInput);

		final int length = trimmed.length();

		// Analyze the input to determine a set of attributes including whether it is numeric
		final NumericResult nr = Numeric.analyze(trimmed, length, ac.ni);

		final StringBuilder compressedl0 = new StringBuilder(length);
		if (nr.alphasSeen != 0 && nr.digitsSeen != 0 && nr.alphasSeen + nr.digitsSeen == length) {
			compressedl0.append(KnownTypes.PATTERN_ALPHANUMERIC).append('{').append(String.valueOf(length)).append('}');
		} else if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
			compressedl0.append(KnownTypes.PATTERN_BOOLEAN_TRUE_FALSE);
		} else if (ac.localizedYes != null && (ac.collator.compare(trimmed, ac.localizedYes) == 0 || ac.collator.compare(trimmed, ac.localizedNo) == 0)) {
			compressedl0.append(KnownTypes.PATTERN_BOOLEAN_YES_NO_LOCALIZED);
		} else if ("yes".equalsIgnoreCase(trimmed) || "no".equalsIgnoreCase(trimmed)) {
			compressedl0.append(KnownTypes.PATTERN_BOOLEAN_YES_NO);
		} else if ("y".equalsIgnoreCase(trimmed) || "n".equalsIgnoreCase(trimmed)) {
			compressedl0.append(KnownTypes.PATTERN_BOOLEAN_Y_N);
		} else {
			String l0withSentinel = nr.l0.toString() + "|";
			// Walk the new level0 to create the new level1
			if (nr.couldBeNumeric && nr.numericGroupingSeparators > 0)
				l0withSentinel = l0withSentinel.replace("G", "");
			char last = l0withSentinel.charAt(0);
			int repetitions = 1;
			for (int i = 1; i < l0withSentinel.length(); i++) {
				final char ch = l0withSentinel.charAt(i);
				if (ch == last && i + 1 != l0withSentinel.length()) {
					repetitions++;
				} else {
					if (last == 'd' || last == 'a') {
						compressedl0.append(last == 'd' ? KnownTypes.PATTERN_NUMERIC : KnownTypes.PATTERN_ALPHA);
						compressedl0.append('{').append(String.valueOf(repetitions)).append('}');
					} else {
						for (int j = 0; j < repetitions; j++) {
							compressedl0.append(last);
						}
					}
					last = ch;
					repetitions = 1;
				}
			}
		}
		final Escalation escalation = new Escalation();
		escalation.level[0] = compressedl0;

		if (ac.dateTimeParser.determineFormatString(trimmed) != null)
			possibleDateTime++;

		// Check to see if this input is one of our registered Infinite Semantic Types
		int c = 0;
		for (final LogicalTypeInfinite logical : ac.infiniteTypes) {
			try {
				if ((ac.facts.getMatchTypeInfo() == null || logical.acceptsBaseType(ac.facts.getMatchTypeInfo().getBaseType())) && logical.isCandidate(trimmed, compressedl0, nr.charCounts, nr.lastIndex))
					ac.candidateCounts[c]++;
			}
			catch (Exception e) {
				LoggerFactory.getLogger("com.cobber.fta").error("Plugin: {}, issue: {}.", logical.getSemanticType(), e.getMessage());
			}
			c++;
		}

		// Check to see if this input is one of our registered RegExp Semantic Types
		c = 0;
		for (final LogicalTypeRegExp logical : ac.regExpTypes) {
			try {
				if ((ac.facts.getMatchTypeInfo() == null || logical.acceptsBaseType(ac.facts.getMatchTypeInfo().getBaseType())) && logical.isValid(trimmed))
					ac.candidateCountsRE[c]++;
			}
			catch (Exception e) {
				LoggerFactory.getLogger("com.cobber.fta").error("Plugin: {}, issue: {}.", logical.getSemanticType(), e.getMessage());
			}
			c++;
		}

		// Create the level 1 and 2
		if (nr.digitsSeen > 0 && nr.couldBeNumeric && nr.numericDecimalSeparators <= 1) {
			updateNumericPattern(escalation, nr.numericSigned, nr.numericDecimalSeparators, nr.possibleExponentSeen, nr.nonLocalizedDouble);
		} else {
			// Fast version of replaceAll("\\{\\d*\\}", "+"), e.g. replace \d{5} with \d+
			final StringBuilder collapsed = new StringBuilder(compressedl0);
			for (int i = 0; i < collapsed.length(); i++) {
				if (collapsed.charAt(i) == '{' && i + 1 < collapsed.length() && Character.isDigit(collapsed.charAt(i + 1))) {
					final int start = i++;
					while (collapsed.charAt(++i) != '}')
						/* EMPTY */;
					collapsed.replace(start, i + 1, "+");
				}
			}

			// Level 1 is the collapsed version e.g. convert \d{4}-\d{2}-\d{2] to \d+-\d+-\d+
			escalation.level[1] = new StringBuilder(collapsed);
			escalation.level[2] = new StringBuilder(KnownTypes.PATTERN_ANY_VARIABLE);
		}

		detectWindowEscalations.add(escalation);
	}

	private Map.Entry<String, Integer> getBest(final int levelIndex) {
		final Map<String, Integer> frequency = frequencies.get(levelIndex);

		// Grab the best and the second best based on frequency
		Map.Entry<String, Integer> best = null;
		Map.Entry<String, Integer> secondBest = null;
		Map.Entry<String, Integer> thirdBest = null;
		TypeInfo bestPattern = null;
		TypeInfo secondBestPattern = null;
		TypeInfo thirdBestPattern = null;
		String newKey = null;

		// Handle numeric promotion
		for (final Map.Entry<String, Integer> entry : frequency.entrySet()) {

			if (best == null) {
				best = entry;
				bestPattern = ac.knownTypes.getByRegExp(best.getKey());
			}
			else if (secondBest == null) {
				secondBest = entry;
				secondBestPattern = ac.knownTypes.getByRegExp(secondBest.getKey());
				if (levelIndex != 0 && bestPattern != null && secondBestPattern != null &&
						bestPattern.isNumeric() && secondBestPattern.isNumeric()) {
					newKey = ac.knownTypes.numericPromotion(bestPattern.getRegExp(), secondBestPattern.getRegExp());
					if (newKey != null) {
						best = new AbstractMap.SimpleEntry<>(newKey, best.getValue() + secondBest.getValue());
						bestPattern = ac.knownTypes.getByRegExp(best.getKey());
					}
				}
			}
			else if (thirdBest == null) {
				thirdBest = entry;
				thirdBestPattern = ac.knownTypes.getByRegExp(thirdBest.getKey());
				if (levelIndex != 0 && bestPattern != null && thirdBestPattern != null &&
						bestPattern.isNumeric() && thirdBestPattern.isNumeric()) {
					newKey = ac.knownTypes.numericPromotion(newKey != null ? newKey : bestPattern.getRegExp(), thirdBestPattern.getRegExp());
					if (newKey != null) {
						best = new AbstractMap.SimpleEntry<>(newKey, best.getValue() + thirdBest.getValue());
						bestPattern = ac.knownTypes.getByRegExp(best.getKey());
					}
				}
			}
		}

		// Promote almost anything to STRING, DOUBLES are pretty clear (unlike LONGS) so do not promote these
		if (bestPattern != null && secondBestPattern != null && !FTAType.DOUBLE.equals(bestPattern.getBaseType()))
			if (FTAType.STRING.equals(bestPattern.getBaseType()))
				best = new AbstractMap.SimpleEntry<>(best.getKey(), best.getValue() + secondBest.getValue());
			else if (secondBestPattern.id == KnownTypes.ID.ID_ANY_VARIABLE)
				best = new AbstractMap.SimpleEntry<>(secondBest.getKey(), best.getValue() + secondBest.getValue());

		return best;
	}

	/*
	 * collapse() will attempt to coalesce samples to be more generic so we stay at a lower level rather than
	 * ending up with '.+' which is not very informative.  So for example, with the following pair:
	 * [\p{IsAlphabetic}{10}, \p{IsAlphabetic}+, .+]
	 * [[\p{IsAlphabetic}\d]{10}, [\p{IsAlphabetic}\d]+, .+]
	 * The first element will be 'promoted' to the second.
	 */
	private void collapse() {
		// Map from Escalation hash to count of occurrences
		final Map<Integer, Integer> observedFrequency = new HashMap<>();
		// Map from Escalation hash to Escalation
		final Map<Integer, Escalation> observedSet = new HashMap<>();

		int longCount = 0;
		int doubleCount = 0;
		int totalCount = 0;
		Escalation eDouble = null;
		for (final Escalation e : detectWindowEscalations) {
			totalCount++;
			if (e.typeInfo != null)
				switch (e.typeInfo.getBaseType()) {
					case LONG:
						longCount++;
						break;
					case DOUBLE:
						eDouble = e;
						doubleCount++;
						break;
					default:
						break;
				}
		}

		// If we have a full complement of numerics which is a mix of longs and doubles - then we want to switch to doubles
		final boolean switchToDouble = longCount != 0 && doubleCount != 0 && longCount + doubleCount == totalCount;

		// Calculate the frequency of every element
		for (final Escalation e : detectWindowEscalations) {
			// If we are switching to doubles and this entry is a long then just replace it with a random double entry
			final Escalation eObserved = switchToDouble && e.typeInfo.getBaseType() == FTAType.LONG ? eDouble : e;
			final int hash = eObserved.hashCode();
			final Integer seen = observedFrequency.get(hash);
			if (seen == null) {
				observedFrequency.put(hash, 1);
				observedSet.put(hash, eObserved);
			} else {
				observedFrequency.put(hash, seen + 1);
			}
		}

		for (int i = 0; i < 3; i++) {
			final Map<String, Integer> keyFrequency = new HashMap<>();
			for (final Map.Entry<Integer, Integer> entry : observedFrequency.entrySet()) {
				final Escalation escalation = observedSet.get(entry.getKey());
				keyFrequency.merge(escalation.level[i].toString(), entry.getValue(), Integer::sum);
			}

			// If it makes sense rewrite our sample data switching numeric/alpha matches to alphanumeric matches
			if (keyFrequency.size() > 1) {
				final Set<String> keys = new HashSet<>(keyFrequency.keySet());
				for (final String oldKey : keys) {
					String newKey = oldKey.replace(KnownTypes.PATTERN_NUMERIC, KnownTypes.PATTERN_ALPHANUMERIC);
					if (!newKey.equals(oldKey) && keys.contains(newKey)) {
						final int oldCount = keyFrequency.remove(oldKey);
						final int currentCount = keyFrequency.get(newKey);
						keyFrequency.put(newKey, currentCount + oldCount);
					} else {
						newKey = oldKey.replace(KnownTypes.PATTERN_ALPHA, KnownTypes.PATTERN_ALPHANUMERIC);
						if (!newKey.equals(oldKey) && keys.contains(newKey)) {
							final int oldCount = keyFrequency.remove(oldKey);
							final int currentCount = keyFrequency.get(newKey);
							keyFrequency.put(newKey, currentCount + oldCount);
						}
					}
				}
			}
			frequencies.add(Utils.sortByValue(keyFrequency));
		}
	}

	boolean handleForce() {
		final String[] semanticTypes = ac.analyzerContext.getSemanticTypes();
		if (semanticTypes == null)
			return false;

		final String semanticType = semanticTypes[ac.analyzerContext.getStreamIndex()];

		if (isInteresting(semanticType)) {
			final PluginDefinition pluginDefinition = PluginDefinition.findByName(semanticType);
			if (pluginDefinition == null) {
				debug("ERROR: Failed to locate plugin named '{}'", semanticType);
				return false;
			}

			LogicalType logical = null;
			try {
				logical = LogicalTypeFactory.newInstance(pluginDefinition, ac.analysisConfig);
			} catch (FTAPluginException e) {
				debug("ERROR: Failed to instantiate plugin named '{}', error: {}", semanticType, e.getMessage());
				return false;
			}
			final TypeInfo answer = new TypeInfo(logical.getRegExp(), logical.getBaseType(), logical.getSemanticType(), ac.facts.getMatchTypeInfo());
			answer.setForce(true);

			ac.facts.setMatchTypeInfo(answer);
			ac.ctxdebug("Type determination", "infinite type, confidence: FORCED, matchTypeInfo - {}", ac.facts.getMatchTypeInfo());

			return true;
		}

		return false;
	}

	/**
	 * Core type-determination routine. Processes the accumulated escalations from the detect
	 * window and sets {@code facts.matchTypeInfo}.
	 *
	 * @return {@code true} if the raw-sample replay (trackResult pass) should be performed by the caller.
	 */
	boolean determineType() {
		if (ac.facts.sampleCount == 0) {
			ac.facts.setMatchTypeInfo(ac.knownTypes.getByRegExp(KnownTypes.PATTERN_ANY_VARIABLE));
			return false;
		}

		collapse();

		int level0value = 0;
		int level1value = 0;
		int level2value = 0;
		String level0pattern = null;
		String level1pattern = null;
		String level2pattern = null;
		TypeInfo level0typeInfo = null;
		TypeInfo level1typeInfo = null;
		TypeInfo level2typeInfo = null;
		final Map.Entry<String, Integer> level0 = getBest(0);
		final Map.Entry<String, Integer> level1 = getBest(1);
		final Map.Entry<String, Integer> level2 = getBest(2);
		Map.Entry<String, Integer> best = level0;

		if (level0 != null) {
			level0pattern = level0.getKey();
			level0value = level0.getValue();
			level0typeInfo = ac.knownTypes.getByRegExp(level0pattern);

			if (level0typeInfo == null) {
				for (final LogicalTypeRegExp logical : ac.regExpTypes) {
					// Check that the samples match the pattern we are looking for and the pattern returned.
					// For example to be an IDENTITY.EIN_US you need to match
					//    "regExpsToMatch": [ "\\d{2}-\\d{7}" ],
					// as well as
					//    "regExpReturned": "(0[1-6]|1[0-6]|2[0-7]|3[0-9]|4[0-8]|5[0-9]|6[0-8]|7[1-7]|8[0-8]|9[01234589])-\\d{7}",
					if (logical.isMatch(level0pattern) && ac.tokenStreams.matches(logical.getRegExp(), logical.getThreshold()) != 0) {
						level0typeInfo = new TypeInfo(logical.getRegExp(), logical.getBaseType(), logical.getSemanticType(), ac.facts.getMatchTypeInfo());
						break;
					}
				}
			}
		}
		if (level1 != null) {
			level1pattern = level1.getKey();
			level1value = level1.getValue();
			level1typeInfo = ac.knownTypes.getByRegExp(level1pattern);
		}
		if (level2 != null) {
			level2pattern = level2.getKey();
			level2value = level2.getValue();
			level2typeInfo = ac.knownTypes.getByRegExp(level2pattern);
		}

		if (best != null) {
			ac.facts.setMatchTypeInfo(level0typeInfo);

			// Take any 'reasonable' (80%) level 1 with something we recognize or a better count
			if (level1 != null && (double)level1value/ac.raw.size() >= 0.8 && (level0typeInfo == null || level1value > level0value)) {
				best = level1;
				ac.facts.setMatchTypeInfo(level1typeInfo);
			}

			// Take any level 2 if
			// - we have something we recognize (and we had nothing)
			// - we have the same key but a better count
			// - we have different keys but same type (signed versus not-signed)
			// - we have different keys, two numeric types and an improvement of at least 5%
			// - we have different keys, different types and an improvement of at least 10% and we are below the threshold
			if (level2 != null &&
					((ac.facts.getMatchTypeInfo() == null && level2typeInfo != null)
					|| (best.getKey().equals(level2pattern) && level2value > best.getValue())
					|| (!best.getKey().equals(level2pattern) && level2typeInfo != null
							&& ac.facts.getMatchTypeInfo().getBaseType().equals(level2typeInfo.getBaseType())
							&& level2value > best.getValue())
					|| (!best.getKey().equals(level2pattern) && level2typeInfo != null
							&& ac.facts.getMatchTypeInfo().isNumeric()
							&& level2typeInfo.isNumeric()
							&& level2value >= 1.05 * best.getValue())
					|| (!best.getKey().equals(level2pattern) && (double)best.getValue()/ac.raw.size() < (double)ac.analysisConfig.getThreshold()/100
							&& level2value >= 1.10 * best.getValue()))) {
				ac.facts.setMatchTypeInfo(level2typeInfo);

				// If we are really unhappy with this determination then just back out to String
				if ((double)level2value/ac.raw.size() < 0.8)
					ac.facts.setMatchTypeInfo(ac.knownTypes.getByID(KnownTypes.ID.ID_ANY_VARIABLE));

			}

			if (possibleDateTime != 0 && possibleDateTime + 1 >= ac.raw.size()) {

				// This next try/catch is unnecessary in theory, if there are zero bugs then it will never trip,
				// if there happens to be an issue then we swallow it and will not detect the date/datetime.
				try {
					for (final String sample : ac.raw)
						ac.dateTimeParser.train(sample);

					final DateTimeParserResult result = ac.dateTimeParser.getResult();
					final String formatString = result.getFormatString();
					ac.facts.setMatchTypeInfo(new TypeInfo(null, result.getRegExp(), result.getType(), formatString, false, formatString));
				}
				catch (RuntimeException e) {
					debug("Internal error: {}", e);
				}
			}

			ac.ctxdebug("Type determination", "initial, matchTypeInfo - {}", ac.facts.getMatchTypeInfo());

			// Check to see if it might be one of the known Infinite Semantic Types
			int i = 0;
			double bestConfidence = 0.0;
			for (final LogicalTypeInfinite logical : ac.infiniteTypes) {
				if (logical.acceptsBaseType(ac.facts.getMatchTypeInfo().getBaseType()) && logical.getConfidence(ac.candidateCounts[i], ac.raw.size(), ac.analyzerContext) >= logical.getThreshold()/100.0) {
					int count = 0;
					TypeInfo candidate;
					if (logical.getBaseType().isDateOrTimeType())
						candidate = new TypeInfo(null, ac.facts.getMatchTypeInfo().getRegExp(), ac.facts.getMatchTypeInfo().getBaseType(), logical.getSemanticType(), true, ac.facts.getMatchTypeInfo().format);
					else
						candidate = new TypeInfo(logical.getRegExp(), logical.getBaseType(), logical.getSemanticType(), ac.facts.getMatchTypeInfo());

					for (final String sample : ac.raw) {
						switch (logical.getBaseType()) {
						case STRING:
							if (typeTracker.trackString(sample, sample.trim(), candidate, false, 1))
								count++;
							break;
						case LONG:
							if (typeTracker.trackLong(sample.trim(), candidate, false, 1))
								count++;
							break;
						case DOUBLE:
							if (typeTracker.trackDouble(sample, candidate, false, 1))
								count++;
							break;
						case LOCALDATE:
							try {
								if (typeTracker.trackDateTime(sample, candidate, false, 1))
									count++;
							}
							catch (DateTimeParseException e) {
								// DO NOTHING
							}
							break;
						default:
							break;
						}
					}

					// If a reasonable number look genuine then we are convinced
					final double currentConfidence = logical.getConfidence(count, ac.raw.size(), ac.analyzerContext);
					if (currentConfidence > bestConfidence && currentConfidence >= logical.getThreshold()/100.0) {
						ac.facts.setMatchTypeInfo(candidate);
						bestConfidence = currentConfidence;
						ac.ctxdebug("Type determination", "infinite type, confidence: {}, matchTypeInfo - {}", currentConfidence, ac.facts.getMatchTypeInfo());
					}
				}
				i++;
			}

			// Try a regExp match nice and early - we can always back out
			i = 0;
			for (final LogicalTypeRegExp logical : ac.regExpTypes) {
				if (logical.acceptsBaseType(ac.facts.getMatchTypeInfo().getBaseType()) &&
					logical.getConfidence(ac.candidateCountsRE[i], ac.raw.size(), ac.analyzerContext) >= logical.getThreshold()/100.0) {
					ac.facts.setMatchTypeInfo(new TypeInfo(logical.getRegExp(), logical.getBaseType(), logical.getSemanticType(), ac.facts.getMatchTypeInfo()));
					ac.ctxdebug("Type determination", "was '{}', matchTypeInfo - {}", ac.facts.getMatchTypeInfo().getBaseType(), ac.facts.getMatchTypeInfo());
					break;
				}
				i++;
			}

			return true;
		}

		return false;
	}
}
