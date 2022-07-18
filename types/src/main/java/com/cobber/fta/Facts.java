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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;

import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.Utils;
import com.cobber.fta.dates.DateTimeParser;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A set of facts for the Analysis in question.
 */
public class Facts {
	/** The minimum length (not trimmed) - Only relevant for Numeric, Boolean and String. Note: For String and Boolean types this length includes any whitespace. */
	public int minRawLength = Integer.MAX_VALUE;
	/** The maximum length (not trimmed) - Only relevant for Numeric, Boolean and String. Note: For String and Boolean types this length includes any whitespace. */
	public int maxRawLength = Integer.MIN_VALUE;
	/** Are any elements multi-line? */
	public boolean multiline;
	/** Do any elements have leading White Space? */
	public boolean leadingWhiteSpace;
	/** Do any elements have trailing White Space? */
	public boolean trailingWhiteSpace;
	/** The percentage confidence (0-1.0) that the observed stream is a Key field. */
	protected Double keyConfidence;
	/** The number of leading zeros seen in sample set.  Only relevant for type Long. */
	protected long leadingZeroCount;
	/** Get the Decimal Separator used to interpret Doubles.  Only relevant for type double. */
	public char decimalSeparator = '.';
	/** What is the uniqueness percentage of this column. */
	protected Double uniqueness;

	// The Locale default Decimal Separator
	protected char localeDecimalSeparator;

	public String minBoolean;
	public String maxBoolean;

	public long minLong = Long.MAX_VALUE;
	public long maxLong = Long.MIN_VALUE;
	public final TopBottomK<Long, Long> tbLong = new TopBottomK<>();

	public double minDouble = Double.MAX_VALUE;
	public double maxDouble = -Double.MAX_VALUE;
	public final TopBottomK<Double, Double> tbDouble = new TopBottomK<>();

	public String minString;
	public String maxString;
	public final TopBottomK<String, String> tbString = new TopBottomK<>();

	public LocalDate minLocalDate;
	public LocalDate maxLocalDate;
	public final TopBottomK<LocalDate, ChronoLocalDate> tbLocalDate = new TopBottomK<>();

	public LocalTime minLocalTime;
	public LocalTime maxLocalTime;
	public final TopBottomK<LocalTime, LocalTime> tbLocalTime = new TopBottomK<>();

	public LocalDateTime minLocalDateTime;
	public LocalDateTime maxLocalDateTime;
	public final TopBottomK<LocalDateTime, ChronoLocalDateTime<?>> tbLocalDateTime = new TopBottomK<>();

	public OffsetDateTime minOffsetDateTime;
	public OffsetDateTime maxOffsetDateTime;
	public final TopBottomK<OffsetDateTime, OffsetDateTime> tbOffsetDateTime = new TopBottomK<>();

	public ZonedDateTime minZonedDateTime;
	public ZonedDateTime maxZonedDateTime;
	public final TopBottomK<ZonedDateTime, ChronoZonedDateTime<?>> tbZonedDateTime = new TopBottomK<>();

	public long minLongNonZero = Long.MAX_VALUE;
	public boolean monotonicIncreasing = true;
	public boolean monotonicDecreasing = true;

	public String minOutlierString;
	public String maxOutlierString;

	// The minimum length (not trimmed) - but must be non-Blank
	public int minRawNonBlankLength = Integer.MAX_VALUE;
	// The maximum length (not trimmed) - but must be non-Blank
	public int maxRawNonBlankLength = Integer.MIN_VALUE;

	public int minTrimmedLength = Integer.MAX_VALUE;
	public int maxTrimmedLength = Integer.MIN_VALUE;

	public int minTrimmedLengthNumeric = Integer.MAX_VALUE;
	public int maxTrimmedLengthNumeric = Integer.MIN_VALUE;

	public int minTrimmedOutlierLength = Integer.MAX_VALUE;
	public int maxTrimmedOutlierLength = Integer.MIN_VALUE;

	public long groupingSeparators;

	public Map<String, Long> cardinality = new HashMap<>();
	public Map<String, Long> outliers = new HashMap<>();

	public double currentM2 = 0.0;

	/** The total number of samples seen. */
	public long sampleCount;
	/** The number of samples that match the patternInfo. */
	public long matchCount;
	/** The total number of samples in the stream (typically -1 to indicate unknown). */
	public long totalCount = -1;
	/** The number of nulls seen in the sample set. */
	public long nullCount;
	/** The number of blanks seen in the sample set. */
	public long blankCount;
	/** The number of distinct valid values seen in the sample set. */
	public Long distinctCount;

	/** The percentage confidence in the analysis. Typically the matchCount divided by the realSamples (facts.sampleCount - (facts.nullCount + facts.blankCount)). */
	public double confidence;
	/** The PatternInfo associated with this matchCount. */
	public PatternInfo matchPatternInfo;

	/** The minimum value observed. */
	private String minValue;
	/** The maximum value observed. */
	private String maxValue;
	/** The mean of the observed values (Numeric types only). */
	public Double mean = 0.0;
	/** The variance of the observed values (Numeric types only). */
	public Double variance;
	/** The top 10  values. */
	public Set<String> topK;
	/** The bottom 10  values. */
	public Set<String> bottomK;

	public String streamFormat;

	private AnalysisConfig analysisConfig;
	private Locale locale;
	private DateTimeParser dateTimeParser;

	public Locale getLocale() {
		return locale;
	}

	public void setConfig(final AnalysisConfig analysisConfig) {
		this.analysisConfig = analysisConfig;

		this.locale = analysisConfig.getLocaleTag() == null ? Locale.getDefault() : Locale.forLanguageTag(analysisConfig.getLocaleTag());
		final DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(locale);
		final DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
		localeDecimalSeparator = symbols.getDecimalSeparator();

		dateTimeParser = new DateTimeParser().withLocale(locale).withNumericMode(false);
	}

	@JsonIgnore
	public Object getMin() {
		if (matchPatternInfo == null)
			return null;

		switch (matchPatternInfo.getBaseType()) {
		case BOOLEAN:
			return minBoolean;

		case LONG:
			return minLong;

		case DOUBLE:
			return minDouble;

		case STRING:
			return minString;

		case LOCALDATE:
			return minLocalDate;

		case LOCALTIME:
			return minLocalTime;

		case LOCALDATETIME:
			return minLocalDateTime;

		case ZONEDDATETIME:
			return minZonedDateTime;

		case OFFSETDATETIME:
			return minOffsetDateTime;
		}
		return null;
	}

	@JsonIgnore
	public Object getMax() {
		if (matchPatternInfo == null)
			return null;

		switch (matchPatternInfo.getBaseType()) {
		case BOOLEAN:
			return maxBoolean;

		case LONG:
			return maxLong;

		case DOUBLE:
			return maxDouble;

		case STRING:
			return maxString;

		case LOCALDATE:
			return maxLocalDate;

		case LOCALTIME:
			return maxLocalTime;

		case LOCALDATETIME:
			return maxLocalDateTime;

		case ZONEDDATETIME:
			return maxZonedDateTime;

		case OFFSETDATETIME:
			return maxOffsetDateTime;
		}
		return null;
	}

	public String getMinValue() {
		return minValue;
	}

	public void setMinValue(final String minValue) {
		this.minValue = minValue;
	}

	public String getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(final String maxValue) {
		this.maxValue = maxValue;
	}

	public Facts calculateFacts() {
		if (matchPatternInfo == null)
			return this;

		// We know the type - so calculate a minimum and maximum value
		switch (matchPatternInfo.getBaseType()) {
		case BOOLEAN:
			minValue = String.valueOf(minBoolean);
			maxValue = String.valueOf(maxBoolean);
			break;

		case LONG:
			final NumberFormat longFormatter = NumberFormat.getIntegerInstance(locale);
			longFormatter.setGroupingUsed(KnownPatterns.hasGrouping(matchPatternInfo.id));
			minValue = longFormatter.format(minLong);
			maxValue = longFormatter.format(maxLong);
			variance = currentM2/matchCount;
			bottomK = alignFormat(tbLong.bottomKasString(), FTAType.LONG, longFormatter);
			topK = alignFormat(tbLong.topKasString(), FTAType.LONG, longFormatter);
			break;

		case DOUBLE:
			final NumberFormat doubleFormatter = NumberFormat.getNumberInstance(decimalSeparator == localeDecimalSeparator ? locale : Locale.ROOT);
			doubleFormatter.setMinimumFractionDigits(1);
			doubleFormatter.setMaximumFractionDigits(16);
			if (doubleFormatter instanceof DecimalFormat && KnownPatterns.hasExponent(matchPatternInfo.id)) {
				final DecimalFormat decimalFormatter = (DecimalFormat)doubleFormatter;
				decimalFormatter.applyPattern("#.##################E0");
				minValue = decimalFormatter.format(minDouble);
				maxValue = decimalFormatter.format(maxDouble);
			}
			else {
				doubleFormatter.setGroupingUsed(KnownPatterns.hasGrouping(matchPatternInfo.id));
				minValue = doubleFormatter.format(minDouble);
				maxValue = doubleFormatter.format(maxDouble);
			}
			variance = currentM2/matchCount;
			bottomK = alignFormat(tbDouble.bottomKasString(), FTAType.DOUBLE, doubleFormatter);
			topK = alignFormat(tbDouble.topKasString(), FTAType.DOUBLE, doubleFormatter);
			break;

		case STRING:
			if ("NULL".equals(matchPatternInfo.typeQualifier)) {
				minRawLength = maxRawLength = 0;
			} else if ("BLANK".equals(matchPatternInfo.typeQualifier)) {
				// If all the fields are blank (i.e. a variable number of spaces) - then we have not saved any of the raw input, so we
				// need to synthesize the min and max value, as well as the minRawlength if not set.
				if (minRawLength == Integer.MAX_VALUE)
					minRawLength = 0;
				minValue = Utils.repeat(' ', minRawLength);
				maxValue = Utils.repeat(' ', maxRawLength);
			}
			else {
				minValue = minString;
				maxValue = maxString;
				bottomK = tbString.bottomKasString();
				topK = tbString.topKasString();
			}
			break;

		case LOCALDATE:
			if (analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS)) {
				final DateTimeFormatter dtf = dateTimeParser.ofPattern(matchPatternInfo.format);

				minValue = minLocalDate == null ? null : minLocalDate.format(dtf);
				maxValue = maxLocalDate == null ? null : maxLocalDate.format(dtf);
				bottomK = alignFormat(tbLocalDate.bottomKasString(), FTAType.LOCALDATE, dtf);
				topK = alignFormat(tbLocalDate.topKasString(), FTAType.LOCALDATE, dtf);
			}
			break;

		case LOCALTIME:
			if (analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS)) {
				final DateTimeFormatter dtf = dateTimeParser.ofPattern(matchPatternInfo.format);

				minValue = minLocalTime == null ? null : minLocalTime.format(dtf);
				maxValue = maxLocalTime == null ? null : maxLocalTime.format(dtf);
				bottomK = alignFormat(tbLocalTime.bottomKasString(), FTAType.LOCALTIME, dtf);
				topK = alignFormat(tbLocalTime.topKasString(), FTAType.LOCALTIME, dtf);
			}
			break;

		case LOCALDATETIME:
			if (analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS)) {
				final DateTimeFormatter dtf = dateTimeParser.ofPattern(matchPatternInfo.format);

				minValue = minLocalDateTime == null ? null : minLocalDateTime.format(dtf);
				maxValue = maxLocalDateTime == null ? null : maxLocalDateTime.format(dtf);
				bottomK = alignFormat(tbLocalDateTime.bottomKasString(), FTAType.LOCALDATETIME, dtf);
				topK = alignFormat(tbLocalDateTime.topKasString(), FTAType.LOCALDATETIME, dtf);
			}
			break;

		case ZONEDDATETIME:
			if (analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS)) {
				final DateTimeFormatter dtf = dateTimeParser.ofPattern(matchPatternInfo.format);

				minValue = minZonedDateTime == null ? null : minZonedDateTime.format(dtf);
				maxValue = maxZonedDateTime == null ? null : maxZonedDateTime.format(dtf);
				bottomK = alignFormat(tbZonedDateTime.bottomKasString(), FTAType.ZONEDDATETIME, dtf);
				topK = alignFormat(tbZonedDateTime.topKasString(), FTAType.ZONEDDATETIME, dtf);
			}
			break;

		case OFFSETDATETIME:
			if (analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS)) {
				final DateTimeFormatter dtf = dateTimeParser.ofPattern(matchPatternInfo.format);

				minValue = minOffsetDateTime == null ? null : minOffsetDateTime.format(dtf);
				maxValue = maxOffsetDateTime == null ? null : maxOffsetDateTime.format(dtf);
				bottomK = alignFormat(tbOffsetDateTime.bottomKasString(), FTAType.OFFSETDATETIME, dtf);
				topK = alignFormat(tbOffsetDateTime.topKasString(), FTAType.OFFSETDATETIME, dtf);
			}
			break;
		}

		return this;
	}

	protected Object getValue(final String input) {
		if (matchPatternInfo == null || input == null)
			return null;

		switch (matchPatternInfo.getBaseType()) {
		case LONG:
			// We cannot use parseLong as it does not cope with localization
			final NumberFormat longFormatter = NumberFormat.getIntegerInstance(locale);
	        try {
                return longFormatter.parse(input).longValue();
	        } catch (ParseException e) {
                return null;
	        }
		case DOUBLE:
			final NumberFormat doubleFormatter = NumberFormat.getInstance(decimalSeparator == localeDecimalSeparator ? locale : Locale.ROOT);
	        try {
                return doubleFormatter.parse(input).doubleValue();
	        } catch (ParseException e) {
                return null;
	        }
		case STRING:
			return input;
		case LOCALDATE:
			final DateTimeFormatter dtfLD = dateTimeParser.ofPattern(matchPatternInfo.format);
			return LocalDate.parse(input, dtfLD);
		case LOCALTIME:
			final DateTimeFormatter dtfLT = dateTimeParser.ofPattern(matchPatternInfo.format);
			return LocalTime.parse(input, dtfLT);
		case LOCALDATETIME:
			final DateTimeFormatter dtfLDT = dateTimeParser.ofPattern(matchPatternInfo.format);
			return LocalDateTime.parse(input, dtfLDT);
		case ZONEDDATETIME:
			final DateTimeFormatter dtfZDT = dateTimeParser.ofPattern(matchPatternInfo.format);
			return ZonedDateTime.parse(input, dtfZDT);
		case OFFSETDATETIME:
			final DateTimeFormatter dtfODT = dateTimeParser.ofPattern(matchPatternInfo.format);
			return OffsetDateTime.parse(input, dtfODT);
		}

		return null;
	}

	public void hydrate() {
		if (!analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS) || matchPatternInfo == null)
			return;

		switch (matchPatternInfo.getBaseType()) {
		case BOOLEAN:
			break;
		case LONG:
			minLong = (Long)getValue(minValue);
			maxLong = (Long)getValue(maxValue);

			if (topK != null) {
				topK.forEach(item -> tbLong.observe((Long)getValue(item)));
				bottomK.forEach(item -> tbLong.observe((Long)getValue(item)));
			}
			break;

		case DOUBLE:
			minDouble = (Double)getValue(minValue);
			maxDouble = (Double)getValue(maxValue);

			if (topK != null) {
				topK.forEach(item -> tbDouble.observe((Double)getValue(item)));
				bottomK.forEach(item -> tbDouble.observe((Double)getValue(item)));
			}
			break;

		case STRING:
			if (topK != null) {
				topK.forEach(item -> tbString.observe(item));
				bottomK.forEach(item -> tbString.observe(item));
			}
			break;

		case LOCALDATE:
			minLocalDate = (LocalDate)getValue(minValue);
			maxLocalDate = (LocalDate)getValue(maxValue);

			if (topK != null) {
				topK.forEach(item -> tbLocalDate.observe((LocalDate)getValue(item)));
				bottomK.forEach(item -> tbLocalDate.observe((LocalDate)getValue(item)));
			}
			break;

		case LOCALTIME:
			minLocalTime = (LocalTime)getValue(minValue);
			maxLocalTime = (LocalTime)getValue(maxValue);

			if (topK != null) {
				topK.forEach(item -> tbLocalTime.observe((LocalTime)getValue(item)));
				bottomK.forEach(item -> tbLocalTime.observe((LocalTime)getValue(item)));
			}
			break;

		case LOCALDATETIME:
			minLocalDateTime = (LocalDateTime)getValue(minValue);
			maxLocalDateTime = (LocalDateTime)getValue(maxValue);

			if (topK != null) {
				topK.forEach(item -> tbLocalDateTime.observe((LocalDateTime)getValue(item)));
				bottomK.forEach(item -> tbLocalDateTime.observe((LocalDateTime)getValue(item)));
			}
			break;

		case ZONEDDATETIME:
			minZonedDateTime = (ZonedDateTime)getValue(minValue);
			maxZonedDateTime = (ZonedDateTime)getValue(maxValue);

			if (topK != null) {
				topK.forEach(item -> tbZonedDateTime.observe((ZonedDateTime)getValue(item)));
				bottomK.forEach(item -> tbZonedDateTime.observe((ZonedDateTime)getValue(item)));
			}
			break;

		case OFFSETDATETIME:
			minOffsetDateTime = (OffsetDateTime)getValue(minValue);
			maxOffsetDateTime = (OffsetDateTime)getValue(maxValue);

			if (topK != null) {
				topK.forEach(item -> tbOffsetDateTime.observe((OffsetDateTime)getValue(item)));
				bottomK.forEach(item -> tbOffsetDateTime.observe((OffsetDateTime)getValue(item)));
			}
			break;
		}
	}

	/*
	 * Return a new Set (in the same order) as the input set but formatted according to the supplied Formatter.
	 * The input set is ordered based on the position on a timeline, not lexigraphically.
	 * Note: The input set is formatted according to the default formatter based on the type.
	 */
	private Set<String> alignFormat(final SortedSet<String> toFix, final FTAType type, final Object formatter) {
		final Set<String> ret = new LinkedHashSet<>();
		for (final String s : toFix) {
			switch (type) {
			case LONG:
				ret.add(((NumberFormat)formatter).format(Long.parseLong(s)));
				break;
			case DOUBLE:
				ret.add(((NumberFormat)formatter).format(Double.parseDouble(s)));
				break;
			case LOCALDATE:
				ret.add(LocalDate.parse(s).format((DateTimeFormatter)formatter));
				break;
			case LOCALTIME:
				ret.add(LocalTime.parse(s).format((DateTimeFormatter)formatter));
				break;
			case LOCALDATETIME:
				ret.add(LocalDateTime.parse(s).format((DateTimeFormatter)formatter));
				break;
			case ZONEDDATETIME:
				ret.add(ZonedDateTime.parse(s).format((DateTimeFormatter)formatter));
				break;
			case OFFSETDATETIME:
				ret.add(OffsetDateTime.parse(s).format((DateTimeFormatter)formatter));
				break;
			}
		}

		return ret;
	}

	public boolean equals(final Object obj, final double epsilon) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Facts other = (Facts) obj;

		final Object min = getMin();
		final Object otherMin = other.getMin();
		if (min == null ^ otherMin == null)
			return false;
		if (min != null && !min.equals(otherMin))
			return false;
		final Object max = getMax();
		final Object otherMax = other.getMax();
		if (max == null ^ otherMax == null)
			return false;
		if (max != null && !max.equals(otherMax))
			return false;
		return blankCount == other.blankCount && Objects.equals(cardinality, other.cardinality)
				&& Double.doubleToLongBits(confidence) == Double.doubleToLongBits(other.confidence)
				&& decimalSeparator == other.decimalSeparator && groupingSeparators == other.groupingSeparators
				&& Objects.equals(keyConfidence, other.keyConfidence) && leadingWhiteSpace == other.leadingWhiteSpace
				&& leadingZeroCount == other.leadingZeroCount && Objects.equals(locale, other.locale)
				&& matchCount == other.matchCount && Objects.equals(matchPatternInfo, other.matchPatternInfo)
				&& maxRawLength == other.maxRawLength
				&& monotonicDecreasing == other.monotonicDecreasing && monotonicIncreasing == other.monotonicIncreasing
				&& multiline == other.multiline && nullCount == other.nullCount
				&& Objects.equals(outliers, other.outliers) && sampleCount == other.sampleCount
				&& totalCount == other.totalCount && trailingWhiteSpace == other.trailingWhiteSpace
				&& Objects.equals(uniqueness, other.uniqueness)
				&& Objects.equals(distinctCount, other.distinctCount)
				&& Objects.equals(streamFormat, other.streamFormat)
				&& ((mean == 0.0 && other.mean == 0.0) || Math.abs(mean - other.mean) < epsilon)
				&& ((variance == null && other.variance == null) || (variance == 0.0 && other.variance == 0.0) || Math.abs(variance - other.variance) < epsilon);
	}
}
