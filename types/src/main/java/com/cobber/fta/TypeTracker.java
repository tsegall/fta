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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.cobber.fta.TextAnalyzer.Feature;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.InternalErrorException;
import com.cobber.fta.core.Utils;
import com.cobber.fta.dates.DateTimeParserResult;
import com.cobber.fta.token.Token;

/**
 * Handles per-type value validation and statistics tracking on every training sample.
 * Extracted from {@link TextAnalyzer} to separate the tracking concern from the orchestration concern.
 */
class TypeTracker {
	private final AnalysisContext ac;

	TypeTracker(final AnalysisContext ac) {
		this.ac = ac;
	}

	// Track basic facts for the field - called for all input
	void trackLengthAndShape(final String input, final String trimmed, final long count) {
		// We always want to track basic facts for the field
		final int length = input.length();

		if (length < ac.facts.minRawLength)
			ac.facts.minRawLength = length;
		if (length > ac.facts.maxRawLength)
			ac.facts.maxRawLength = length;

		final int trimmedLength = trimmed.length();
		ac.facts.lengths[Math.min(trimmedLength, ac.facts.lengths.length - 1)] += count;

		if (trimmedLength != 0) {
			if (length != 0 && length < ac.facts.minRawNonBlankLength)
				ac.facts.minRawNonBlankLength = length;
			if (length > ac.facts.maxRawNonBlankLength)
				ac.facts.maxRawNonBlankLength = length;

			ac.tokenStreams.track(trimmed, count);
		}
	}

	boolean trackLong(final String trimmed, final TypeInfo typeInfo, final boolean register, final long count) {
		// Track String facts - just in case we end up backing out.
		if (ac.facts.getMinString() == null || ac.facts.getMinString().compareTo(trimmed) > 0)
			ac.facts.setMinString(trimmed);

		if (ac.facts.getMaxString() == null || ac.facts.getMaxString().compareTo(trimmed) < 0)
			ac.facts.setMaxString(trimmed);

		long l;

		// Interpret the String as a long, first attempt uses parseLong which is fast (although not localized), if that fails,
		// then try using a NumberFormatter which will cope with grouping separators (e.g. 1,000).
		int digits = trimmed.length();
		final char firstCh = trimmed.charAt(0);

		try {
			if (typeInfo.isTrailingMinus()) {
				if (digits >= 2 && trimmed.charAt(digits - 1) == '-') {
					l = -Long.parseLong(trimmed.substring(0, digits - 1));
					digits--;
				}
				else
					l = Long.parseLong(trimmed);
			}
			else {
				l = Long.parseLong(trimmed);
				if (firstCh == '-' || firstCh == '+')
					digits--;
			}
		} catch (NumberFormatException e) {
			final ParsePosition pos = new ParsePosition(0);
			final String cleaned = firstCh == '+' ? trimmed.substring(1) : trimmed;
			final Number n = ac.longFormatter.parse(cleaned, pos);
			if (n == null || cleaned.length() != pos.getIndex())
				return false;
			l = n.longValue();
			if (trimmed.indexOf(ac.ni.groupingSeparator) != -1) {
				ac.facts.groupingSeparators++;
				if (!ac.facts.getMatchTypeInfo().isSemanticType() && !ac.facts.getMatchTypeInfo().hasGrouping()) {
					ac.facts.setMatchTypeInfo(ac.knownTypes.grouping(ac.facts.getMatchTypeInfo().getRegExp()));
					ac.ctxdebug("Type determination", "now with grouping {}", ac.facts.getMatchTypeInfo());
				}
			}
			digits = trimmed.length();
			if (ac.ni.hasNegativePrefix && (firstCh == '-' || firstCh == '+' || firstCh == ac.ni.negativePrefix))
				digits--;
			if (l < 0 && ac.ni.hasNegativeSuffix)
				digits--;
		}

		if (typeInfo.isSemanticType()) {
			// If it is a registered Infinite Semantic Type then validate it
			final LogicalType logical = ac.plugins.getRegistered(typeInfo.getSemanticType());
			if (logical.acceptsBaseType(FTAType.LONG) && !logical.isValid(trimmed, false, count))
				return false;
		}

		if (register) {
			if (firstCh == '0' && digits != 1)
				ac.facts.leadingZeroCount++;

			if (digits < ac.facts.minTrimmedLengthNumeric)
				ac.facts.minTrimmedLengthNumeric = digits;
			if (digits > ac.facts.maxTrimmedLengthNumeric)
				ac.facts.maxTrimmedLengthNumeric = digits;

			if (l != 0 && l < ac.facts.getMinLongNonZero())
				ac.facts.setMinLongNonZero(l);

			if (l < ac.facts.getMinLong())
				ac.facts.setMinLong(l);
			else
				ac.facts.monotonicDecreasing = false;

			if (l > ac.facts.getMaxLong())
				ac.facts.setMaxLong(l);
			else
				ac.facts.monotonicIncreasing = false;

			if (ac.analysisConfig.isEnabled(Feature.COLLECT_STATISTICS)) {
				// Avoids any work if the existing mean is the same as the input
				if (l != ac.facts.mean) {
					// Calculate the mean & standard deviation using Welford's algorithm (weighted)
					final double oldMean = ac.facts.mean;
					final long newCount = ac.facts.matchCount + count;
					ac.facts.mean += (count * (l - oldMean)) / newCount;
					ac.facts.currentM2 += count * ((l - ac.facts.mean) * (l - oldMean));
				}

				ac.facts.tbLong.observe(l);
			}
		}

		return true;
	}

	boolean trackBoolean(final String trimmed) {
		final String trimmedLower = trimmed.toLowerCase(ac.locale);

		final boolean isTrue = "true".equals(trimmedLower) || "yes".equals(trimmedLower) || "y".equals(trimmedLower) ||
				(ac.localizedYes != null && ac.collator.compare(trimmed, ac.localizedYes) == 0);
		final boolean isFalse = !isTrue && ("false".equals(trimmedLower) || "no".equals(trimmedLower) || "n".equals(trimmedLower)) ||
				(ac.localizedNo != null && ac.collator.compare(trimmed, ac.localizedNo) == 0);

		if (isTrue) {
			if (ac.facts.minBoolean == null)
				ac.facts.minBoolean = trimmedLower;
			if (ac.facts.maxBoolean == null || "false".equals(ac.facts.maxBoolean) || "no".equals(ac.facts.maxBoolean) || "n".equals(ac.facts.maxBoolean) || (ac.localizedNo != null && ac.localizedNo.equals(ac.facts.maxBoolean)))
				ac.facts.maxBoolean = trimmedLower;
		} else if (isFalse) {
			if (ac.facts.maxBoolean == null)
				ac.facts.maxBoolean = trimmedLower;
			if (ac.facts.minBoolean == null || "true".equals(ac.facts.minBoolean) || "yes".equals(ac.facts.maxBoolean) || "y".equals(ac.facts.maxBoolean) || (ac.localizedYes != null && ac.localizedYes.equals(ac.facts.maxBoolean)))
				ac.facts.minBoolean = trimmedLower;
		}

		return isTrue || isFalse;
	}

	boolean trackString(final String rawInput, final String trimmed, final TypeInfo typeInfo, final boolean register, final long count) {
		if (register && ac.analysisConfig.getDebug() >= 2 && rawInput.length() > 0 && rawInput.charAt(0) == '¶' && "¶ xyzzy ¶".equals(rawInput))
			throw new NullPointerException("¶ xyzzy ¶");

		if (typeInfo.isSemanticType()) {
			// If it is a registered Infinite Semantic Type then validate it
			final LogicalType logical = ac.plugins.getRegistered(typeInfo.getSemanticType());
			if (logical.acceptsBaseType(FTAType.STRING) && !logical.isValid(rawInput, false, count))
				return false;
		}
		else {
			for (int i = 0; i < trimmed.length(); i++) {
				if (typeInfo.isAlphabetic() && !Character.isAlphabetic(trimmed.charAt(i)))
					return false;
				if (typeInfo.isAlphanumeric() && !Character.isLetterOrDigit((trimmed.charAt(i))))
					return false;
			}
		}

		updateStats(rawInput);

		return true;
	}

	void updateStats(final String cleaned) {
		final int len = cleaned.trim().length();

		if (ac.facts.getMinString() == null || ac.facts.getMinString().compareTo(cleaned) > 0)
			ac.facts.setMinString(cleaned);

		if (ac.facts.getMaxString() == null || ac.facts.getMaxString().compareTo(cleaned) < 0)
			ac.facts.setMaxString(cleaned);

		if (len < ac.facts.minTrimmedLength)
			ac.facts.minTrimmedLength = len;
		if (len > ac.facts.maxTrimmedLength)
			ac.facts.maxTrimmedLength = len;

		if (ac.analysisConfig.isEnabled(Feature.COLLECT_STATISTICS))
			ac.facts.tbString.observe(cleaned);
	}

	boolean trackDouble(final String rawInput, final TypeInfo typeInfo, final boolean register, final long count) {
		final String input = rawInput.trim();
		double d = 0.0;
		boolean converted = false;

		if (ac.facts.decimalSeparator == '.')
			try {
				// parseDouble is not locale sensitive, but it is fast!
				if (typeInfo.isTrailingMinus()) {
					final int digits = input.length();
					if (digits >= 2 && input.charAt(digits - 1) == '-')
						d = -Double.parseDouble(input.substring(0, digits - 1));
					else
						d = Double.parseDouble(input);
				}
				else
					d = Double.parseDouble(input);
				converted = true;
			} catch (NumberFormatException | StringIndexOutOfBoundsException e) {
				// IGNORE - note converted will be false
			}

		if (!converted) {
			// If we think we are a Non Localized number then no point in using the locale-aware parsing
			if (typeInfo.isNonLocalized())
				return false;
			final Double dd;
			try {
				// Failed to parse using the naive parseDouble, so use the locale-sensitive Numberformat.parse
				dd = Utils.parseDouble(input, ac.doubleFormatter);
			} catch (NumberFormatException e) {
				return false;
			}
			if (dd == null)
				return false;
			d = dd;
			if (input.indexOf(ac.ni.groupingSeparator) != -1) {
				if (!hasGroupingSeparator(input, ac.ni.groupingSeparator, ac.ni.decimalSeparator))
					return false;
				ac.facts.groupingSeparators++;
			}
			// Make sure to track the decimal separator being used for doubles
			if (ac.ni.decimalSeparator != '.' && ac.facts.decimalSeparator != ac.ni.decimalSeparator && input.indexOf(ac.ni.decimalSeparator) != -1)
				ac.facts.decimalSeparator = ac.ni.decimalSeparator;
		}

		// If it is NaN/Infinity then we are all done
		if (Double.isNaN(d) || Double.isInfinite(d))
			return false;

		if (register && ac.analysisConfig.isEnabled(Feature.COLLECT_STATISTICS)) {
			if (d < ac.facts.minDouble)
				ac.facts.minDouble = d;

			if (d > ac.facts.maxDouble)
				ac.facts.maxDouble = d;

			if (d != 0.0 && d < ac.facts.minDoubleNonZero)
				ac.facts.minDoubleNonZero = d;

			// This test avoids the loop if the existing mean is the same as the input
			if (d != ac.facts.mean)
				for (int i = 0; i < count; i++) {
					// Calculate the mean & standard deviation using Welford's algorithm
					final double delta = d - ac.facts.mean;
					// matchCount is one low - because we do not 'count' the record until we return from this routine indicating valid
					ac.facts.mean += delta / (ac.facts.matchCount + i + 1);
					ac.facts.currentM2 += delta * (d - ac.facts.mean);
				}

			ac.facts.tbDouble.observe(d);

			// Track whether we have ever seen a double with a non-zero fractional component
			if (ac.facts.allZeroes) {
				final int separatorIndex = input.indexOf(ac.facts.decimalSeparator);
				if (separatorIndex != -1 && !Utils.allZeroes(input.substring(separatorIndex + 1)))
					ac.facts.allZeroes = false;
				else {
					if (ac.facts.zeroesLength == -1)
						ac.facts.zeroesLength = input.length() - separatorIndex - 1;
					else if (ac.facts.zeroesLength != input.length() - separatorIndex - 1)
						ac.facts.allZeroes = false;
				}
			}
		}

		if (typeInfo.isSemanticType()) {
			// If it is a registered Infinite Semantic Type then validate it
			final LogicalType logical = ac.plugins.getRegistered(typeInfo.getSemanticType());
			if (logical.acceptsBaseType(FTAType.DOUBLE) && !logical.isValid(input, false, count))
				return false;
		}

		return true;
	}

	/*
	 * Validate (and track) the date/time/datetime input.
	 * This routine is called for every date/time/datetime we see in the input, so performance is critical.
	 */
	boolean trackDateTime(final String input, final TypeInfo typeInfo, final boolean register, final long count) {
		final String dateFormat = typeInfo.format;

		// Retrieve the (likely cached) DateTimeParserResult for the supplied dateFormat
		final DateTimeParserResult result = DateTimeParserResult.asResult(dateFormat, ac.analyzerContext.getDateResolutionMode(), ac.dateTimeParser.getConfig());
		if (result == null)
			throw new InternalErrorException("NULL result for " + dateFormat);

		final DateTimeFormatter formatter = ac.dateTimeParser.ofPattern(result.getFormatString());

		final String trimmed = input.trim();

		// If we are not collecting statistics we can use the parse on DateTimeParserResult which is
		// significantly faster than the parse on LocalTime/LocalDate/LocalDateTime/...
		switch (result.getType()) {
		case LOCALTIME:
			if (register && ac.analysisConfig.isEnabled(Feature.COLLECT_STATISTICS)) {
				final LocalTime localTime = LocalTime.parse(trimmed, formatter);
				if (ac.facts.minLocalTime == null || localTime.compareTo(ac.facts.minLocalTime) < 0)
					ac.facts.minLocalTime = localTime;
				if (ac.facts.maxLocalTime == null || localTime.compareTo(ac.facts.maxLocalTime) > 0)
					ac.facts.maxLocalTime = localTime;
				ac.facts.tbLocalTime.observe(localTime);
			}
			else
				result.parse(trimmed);
			break;

		case LOCALDATE:
			if (register && ac.analysisConfig.isEnabled(Feature.COLLECT_STATISTICS)) {
				final LocalDate localDate = LocalDate.parse(trimmed, formatter);
				if (ac.facts.minLocalDate == null || localDate.compareTo(ac.facts.minLocalDate) < 0)
					ac.facts.minLocalDate = localDate;
				if (ac.facts.maxLocalDate == null || localDate.compareTo(ac.facts.maxLocalDate) > 0)
					ac.facts.maxLocalDate = localDate;
				ac.facts.tbLocalDate.observe(localDate);
			}
			else
				result.parse(trimmed);
			break;

		case LOCALDATETIME:
			if (register && ac.analysisConfig.isEnabled(Feature.COLLECT_STATISTICS)) {
				final LocalDateTime localDateTime = LocalDateTime.parse(trimmed, formatter);
				if (ac.facts.minLocalDateTime == null || localDateTime.compareTo(ac.facts.minLocalDateTime) < 0)
					ac.facts.minLocalDateTime = localDateTime;
				if (ac.facts.maxLocalDateTime == null || localDateTime.compareTo(ac.facts.maxLocalDateTime) > 0)
					ac.facts.maxLocalDateTime = localDateTime;
				ac.facts.tbLocalDateTime.observe(localDateTime);
			}
			else
				result.parse(trimmed);
			break;

		case ZONEDDATETIME:
			if (register && ac.analysisConfig.isEnabled(Feature.COLLECT_STATISTICS)) {
				final ZonedDateTime zonedDateTime = ZonedDateTime.parse(trimmed, formatter);
				if (ac.facts.minZonedDateTime == null || zonedDateTime.compareTo(ac.facts.minZonedDateTime) < 0)
					ac.facts.minZonedDateTime = zonedDateTime;
				if (ac.facts.maxZonedDateTime == null || zonedDateTime.compareTo(ac.facts.maxZonedDateTime) > 0)
					ac.facts.maxZonedDateTime = zonedDateTime;
				ac.facts.tbZonedDateTime.observe(zonedDateTime);
			}
			else
				result.parse(trimmed);
			break;

		case OFFSETDATETIME:
			if (register && ac.analysisConfig.isEnabled(Feature.COLLECT_STATISTICS)) {
				final OffsetDateTime offsetDateTime = OffsetDateTime.parse(trimmed, formatter);
				if (ac.facts.minOffsetDateTime == null || offsetDateTime.compareTo(ac.facts.minOffsetDateTime) < 0)
					ac.facts.minOffsetDateTime = offsetDateTime;
				if (ac.facts.maxOffsetDateTime == null || offsetDateTime.compareTo(ac.facts.maxOffsetDateTime) > 0)
					ac.facts.maxOffsetDateTime = offsetDateTime;
				ac.facts.tbOffsetDateTime.observe(offsetDateTime);
			}
			else
				result.parse(trimmed);
			break;

		default:
			throw new InternalErrorException("Expected Date/Time type.");
		}

		if (typeInfo.isSemanticType()) {
			// If it is a registered Infinite Semantic Type then validate it
			final LogicalType logical = ac.plugins.getRegistered(typeInfo.getSemanticType());
			if (logical.acceptsBaseType(result.getType()) && !logical.isValid(input, false, count))
				return false;
		}

		return true;
	}

	void addValid(final String input, final long count) {
		final boolean added = ac.facts.cardinality.mergeIfSpace(input, count, Long::sum);
		// If Cardinality blown track remaining set in a Sketch
		if (!added && ac.analysisConfig.isEnabled(Feature.DISTRIBUTIONS) && !ac.facts.getMatchTypeInfo().getBaseType().equals(FTAType.STRING)) {
			ac.facts.getSketch().accept(input, count);
			if (ac.facts.getCardinalityOverflow() == null)
				ac.facts.createHistogramOverflow(new StringConverter(ac.facts.getMatchTypeInfo().getBaseType(), new TypeFormatter(ac.facts.getMatchTypeInfo(), ac.analysisConfig)));
			ac.facts.getCardinalityOverflow().accept(input, count);
		}
	}

	void addOutlier(final String input, final long count) {
		final String cleaned = input.trim();
		final int trimmedLength = cleaned.length();

		if (trimmedLength < ac.facts.minTrimmedOutlierLength)
			ac.facts.minTrimmedOutlierLength = trimmedLength;
		if (trimmedLength > ac.facts.maxTrimmedOutlierLength)
			ac.facts.maxTrimmedOutlierLength = trimmedLength;

		if (ac.facts.minOutlierString == null || ac.facts.minOutlierString.compareTo(cleaned) > 0)
			ac.facts.minOutlierString = cleaned;

		if (ac.facts.maxOutlierString == null || ac.facts.maxOutlierString.compareTo(cleaned) < 0)
			ac.facts.maxOutlierString = cleaned;

		ac.outliersSmashed.mergeIfSpace(Token.generateKey(input), count, Long::sum);

		ac.facts.outliers.mergeIfSpace(input, count, Long::sum);
	}

	void addInvalid(final Map.Entry<String, Long> entry) {
		ac.facts.invalid.mergeIfSpace(entry.getKey(), entry.getValue(), Long::sum);
	}

	boolean hasGroupingSeparator(final String input, final char groupingSeparator, final char decimalSeparator) {
		int digitsLength = 0;
		boolean decimalSeparatorSeen = false;
		boolean groupingSeparatorSeen = false;
		boolean exponentSeen = false;

		for (int i = 0; i < input.length(); i++) {
			final char ch = input.charAt(i);
			if (Character.isDigit(ch))
				digitsLength++;
			else if (ch == groupingSeparator) {
				if (decimalSeparatorSeen || digitsLength > 3 || (groupingSeparatorSeen && digitsLength != 3))
					return false;
				digitsLength = 0;
				groupingSeparatorSeen = true;
			}
			else if (ch == decimalSeparator) {
				if (decimalSeparatorSeen || digitsLength > 3 || (groupingSeparatorSeen && digitsLength != 3))
					return false;
				digitsLength = 0;
				decimalSeparatorSeen = true;
			}
			else if (ch == 'e' || ch == 'E')
				exponentSeen = true;
		}

		return groupingSeparatorSeen && (decimalSeparatorSeen || exponentSeen || digitsLength == 0 || digitsLength == 3);
	}
}
