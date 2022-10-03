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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;

import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.Utils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

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
	public Double keyConfidence;
	/** The number of leading zeros seen in sample set.  Only relevant for type Long. */
	public long leadingZeroCount;
	/** Get the Decimal Separator used to interpret Doubles.  Only relevant for type double. */
	public char decimalSeparator = '.';
	/** What is the uniqueness percentage of this column. */
	public Double uniqueness;

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

	public FiniteMap cardinality = new FiniteMap();
	public FiniteMap outliers = new FiniteMap();
	public FiniteMap invalid = new FiniteMap();

	public double currentM2 = 0.0;

	/** The total number of samples seen. */
	public long sampleCount;
	/** The number of samples that match the patternInfo. */
	public long matchCount;
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

	/** The total number of samples in the stream (typically -1 to indicate unknown). */
	public long totalCount = -1;
	/** The number of null elements in the entire data stream (-1 unless set explicitly). */
	public long totalNullCount = -1;
	/** totalBlankCount - The number of blank elements in the entire data stream (-1 unless set explicitly). */
	public long totalBlankCount = -1;
	/** totalMean - The mean for Numeric types (Long, Double) across the entire data stream (null unless set explicitly). */
	public Double totalMean;
	/** totalStandardDeviation - The standard deviation for Numeric types (Long, Double) across the entire data stream (null unless set explicitly). */
	public Double totalStandardDeviation;
	/** totalMinValue - The minimum value for Numeric, Boolean, and String types across the entire data stream (null unless set explicitly). */
	public String totalMinValue;
	/** totalMaxValue - The manimum value for Numeric, Boolean, and String types across the entire data stream (null unless set explicitly). */
	public String totalMaxValue;
	/** totalMinLength - The minimum length for Numeric, Boolean, and String types across the entire data stream (-1 unless set explicitly). */
	public int totalMinLength = -1;
	/** totalMaxLength - The maximum length for Numeric, Boolean, and String types across the entire data stream (-1 unless set explicitly). */
	public int totalMaxLength = -1;

	public String streamFormat;

	private AnalysisConfig analysisConfig;
	private Locale locale;
	@JsonSerialize(using = SketchSerializer.class)
	@JsonDeserialize(using = SketchDeserializer.class)
	private Sketch sketch;
	private StringConverter stringConverter;
	private TypeFormatter typeFormatter;

	public Locale getLocale() {
		return locale;
	}

	public void setConfig(final AnalysisConfig analysisConfig) {
		this.analysisConfig = analysisConfig;

		this.locale = analysisConfig.getLocaleTag() == null ? Locale.getDefault() : Locale.forLanguageTag(analysisConfig.getLocaleTag());
		final DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(locale);
		final DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
		localeDecimalSeparator = symbols.getDecimalSeparator();

		this.cardinality.setMaxCapacity(analysisConfig.getMaxCardinality());
		this.outliers.setMaxCapacity(analysisConfig.getMaxOutliers());
		this.invalid.setMaxCapacity(analysisConfig.getMaxOutliers());
	}

	public PatternInfo getMatchPatternInfo() {
		return matchPatternInfo;
	}

	public void setMatchPatternInfo(final PatternInfo matchPatternInfo) {
		this.matchPatternInfo = matchPatternInfo;
		typeFormatter = null;
		stringConverter = null;
	}

	@JsonIgnore
	public StringConverter getStringConverter() {
		if (stringConverter == null)
			stringConverter = new StringConverter(matchPatternInfo.getBaseType(), getTypeFormatter());
		return stringConverter;
	}

	@JsonIgnore
	public TypeFormatter getTypeFormatter() {
		if (typeFormatter == null)
			typeFormatter = new TypeFormatter(matchPatternInfo, locale, decimalSeparator, localeDecimalSeparator);
		return typeFormatter;
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

	public boolean sketchExists() {
		return sketch != null;
	}

	@JsonIgnore
	public Sketch getSketch() {
		if (sketch == null)
			sketch = new Sketch(matchPatternInfo.getBaseType(), getStringConverter(), analysisConfig.getQuantileRelativeAccuracy());
		return sketch;
	}

	// Track basic facts for the field - called for any Valid input
	public void trackTrimmedLengthAndWhiteSpace(final String input, final String trimmed) {
		final int trimmedLength = trimmed.length();

		// Determine if there is leading or trailing White space (if not done previously)
		if (trimmedLength != 0) {
			if (!leadingWhiteSpace)
				leadingWhiteSpace = input.charAt(0) == ' ' || input.charAt(0) == '\t';
			if (!trailingWhiteSpace) {
				final int length = input.length();
				trailingWhiteSpace = input.charAt(length - 1) == ' ' || input.charAt(length - 1) == '\t';
			}
		}

		if (trimmedLength < minTrimmedLength)
			minTrimmedLength = trimmedLength;
		if (trimmedLength > maxTrimmedLength)
			maxTrimmedLength = trimmedLength;

		// Determine if this is a multi-line field (if not already decided)
		if (!multiline)
			multiline = input.indexOf('\n') != -1 || input.indexOf('\r') != -1;
	}

	public void killZeroes() {
		if (minLongNonZero != minLong) {
			// Need to remove '0' (and similar friends) from cardinality map and put in outlier map
			Iterator<Entry<String, Long>> it = cardinality.entrySet().iterator();

			while (it.hasNext()) {
				Entry<String, Long> entry = it.next();
				if (Long.parseLong(entry.getKey()) == 0) {
					outliers.put(entry.getKey(), entry.getValue());
					it.remove();
				}
			 }
		}
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
			minValue = getTypeFormatter().getNumericalFormatter().format(minLong);
			maxValue = getTypeFormatter().getNumericalFormatter().format(maxLong);
			variance = currentM2/matchCount;
			bottomK = alignFormat(tbLong.bottomKasString(), FTAType.LONG, getTypeFormatter().getNumericalFormatter());
			topK = alignFormat(tbLong.topKasString(), FTAType.LONG, getTypeFormatter().getNumericalFormatter());
			break;

		case DOUBLE:
			if (getTypeFormatter().getNumericalFormatter() instanceof DecimalFormat && KnownPatterns.hasExponent(matchPatternInfo.id)) {
				final DecimalFormat decimalFormatter = (DecimalFormat)getTypeFormatter().getNumericalFormatter();
				decimalFormatter.applyPattern("#.##################E0");
				minValue = decimalFormatter.format(minDouble);
				maxValue = decimalFormatter.format(maxDouble);
			}
			else {
				minValue = getTypeFormatter().getNumericalFormatter().format(minDouble);
				maxValue = getTypeFormatter().getNumericalFormatter().format(maxDouble);
			}
			variance = currentM2/matchCount;
			bottomK = alignFormat(tbDouble.bottomKasString(), FTAType.DOUBLE, getTypeFormatter().getNumericalFormatter());
			topK = alignFormat(tbDouble.topKasString(), FTAType.DOUBLE, getTypeFormatter().getNumericalFormatter());
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
				minValue = minLocalDate == null ? null : minLocalDate.format(getTypeFormatter().getDateFormatter());
				maxValue = maxLocalDate == null ? null : maxLocalDate.format(getTypeFormatter().getDateFormatter());
				bottomK = alignFormat(tbLocalDate.bottomKasString(), FTAType.LOCALDATE, getTypeFormatter().getDateFormatter());
				topK = alignFormat(tbLocalDate.topKasString(), FTAType.LOCALDATE, getTypeFormatter().getDateFormatter());
			}
			break;

		case LOCALTIME:
			if (analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS)) {
				minValue = minLocalTime == null ? null : minLocalTime.format(getTypeFormatter().getDateFormatter());
				maxValue = maxLocalTime == null ? null : maxLocalTime.format(getTypeFormatter().getDateFormatter());
				bottomK = alignFormat(tbLocalTime.bottomKasString(), FTAType.LOCALTIME, getTypeFormatter().getDateFormatter());
				topK = alignFormat(tbLocalTime.topKasString(), FTAType.LOCALTIME, getTypeFormatter().getDateFormatter());
			}
			break;

		case LOCALDATETIME:
			if (analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS)) {
				minValue = minLocalDateTime == null ? null : minLocalDateTime.format(getTypeFormatter().getDateFormatter());
				maxValue = maxLocalDateTime == null ? null : maxLocalDateTime.format(getTypeFormatter().getDateFormatter());
				bottomK = alignFormat(tbLocalDateTime.bottomKasString(), FTAType.LOCALDATETIME, getTypeFormatter().getDateFormatter());
				topK = alignFormat(tbLocalDateTime.topKasString(), FTAType.LOCALDATETIME, getTypeFormatter().getDateFormatter());
			}
			break;

		case ZONEDDATETIME:
			if (analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS)) {
				minValue = minZonedDateTime == null ? null : minZonedDateTime.format(getTypeFormatter().getDateFormatter());
				maxValue = maxZonedDateTime == null ? null : maxZonedDateTime.format(getTypeFormatter().getDateFormatter());
				bottomK = alignFormat(tbZonedDateTime.bottomKasString(), FTAType.ZONEDDATETIME, getTypeFormatter().getDateFormatter());
				topK = alignFormat(tbZonedDateTime.topKasString(), FTAType.ZONEDDATETIME, getTypeFormatter().getDateFormatter());
			}
			break;

		case OFFSETDATETIME:
			if (analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS)) {
				minValue = minOffsetDateTime == null ? null : minOffsetDateTime.format(getTypeFormatter().getDateFormatter());
				maxValue = maxOffsetDateTime == null ? null : maxOffsetDateTime.format(getTypeFormatter().getDateFormatter());
				bottomK = alignFormat(tbOffsetDateTime.bottomKasString(), FTAType.OFFSETDATETIME, getTypeFormatter().getDateFormatter());
				topK = alignFormat(tbOffsetDateTime.topKasString(), FTAType.OFFSETDATETIME, getTypeFormatter().getDateFormatter());
			}
			break;
		}

		return this;
	}

	public void hydrate() {
		if (!analysisConfig.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS) || matchPatternInfo == null)
			return;

		switch (matchPatternInfo.getBaseType()) {
		case BOOLEAN:
			break;
		case LONG:
			minLong = (Long)getStringConverter().getValue(minValue);
			maxLong = (Long)getStringConverter().getValue(maxValue);

			if (topK != null) {
				topK.forEach(item -> tbLong.observe((Long)getStringConverter().getValue(item)));
				bottomK.forEach(item -> tbLong.observe((Long)getStringConverter().getValue(item)));
			}
			break;

		case DOUBLE:
			minDouble = (Double)getStringConverter().getValue(minValue);
			maxDouble = (Double)getStringConverter().getValue(maxValue);

			if (topK != null) {
				topK.forEach(item -> tbDouble.observe((Double)getStringConverter().getValue(item)));
				bottomK.forEach(item -> tbDouble.observe((Double)getStringConverter().getValue(item)));
			}
			break;

		case STRING:
			if (topK != null) {
				topK.forEach(item -> tbString.observe(item));
				bottomK.forEach(item -> tbString.observe(item));
			}
			break;

		case LOCALDATE:
			minLocalDate = (LocalDate)getStringConverter().getValue(minValue);
			maxLocalDate = (LocalDate)getStringConverter().getValue(maxValue);

			if (topK != null) {
				topK.forEach(item -> tbLocalDate.observe((LocalDate)getStringConverter().getValue(item)));
				bottomK.forEach(item -> tbLocalDate.observe((LocalDate)getStringConverter().getValue(item)));
			}
			break;

		case LOCALTIME:
			minLocalTime = (LocalTime)getStringConverter().getValue(minValue);
			maxLocalTime = (LocalTime)getStringConverter().getValue(maxValue);

			if (topK != null) {
				topK.forEach(item -> tbLocalTime.observe((LocalTime)getStringConverter().getValue(item)));
				bottomK.forEach(item -> tbLocalTime.observe((LocalTime)getStringConverter().getValue(item)));
			}
			break;

		case LOCALDATETIME:
			minLocalDateTime = (LocalDateTime)getStringConverter().getValue(minValue);
			maxLocalDateTime = (LocalDateTime)getStringConverter().getValue(maxValue);

			if (topK != null) {
				topK.forEach(item -> tbLocalDateTime.observe((LocalDateTime)getStringConverter().getValue(item)));
				bottomK.forEach(item -> tbLocalDateTime.observe((LocalDateTime)getStringConverter().getValue(item)));
			}
			break;

		case ZONEDDATETIME:
			minZonedDateTime = (ZonedDateTime)getStringConverter().getValue(minValue);
			maxZonedDateTime = (ZonedDateTime)getStringConverter().getValue(maxValue);

			if (topK != null) {
				topK.forEach(item -> tbZonedDateTime.observe((ZonedDateTime)getStringConverter().getValue(item)));
				bottomK.forEach(item -> tbZonedDateTime.observe((ZonedDateTime)getStringConverter().getValue(item)));
			}
			break;

		case OFFSETDATETIME:
			minOffsetDateTime = (OffsetDateTime)getStringConverter().getValue(minValue);
			maxOffsetDateTime = (OffsetDateTime)getStringConverter().getValue(maxValue);

			if (topK != null) {
				topK.forEach(item -> tbOffsetDateTime.observe((OffsetDateTime)getStringConverter().getValue(item)));
				bottomK.forEach(item -> tbOffsetDateTime.observe((OffsetDateTime)getStringConverter().getValue(item)));
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
		return blankCount == other.blankCount && cardinality.equals(other.cardinality)
				&& Double.doubleToLongBits(confidence) == Double.doubleToLongBits(other.confidence)
				&& decimalSeparator == other.decimalSeparator && groupingSeparators == other.groupingSeparators
				&& Objects.equals(keyConfidence, other.keyConfidence) && leadingWhiteSpace == other.leadingWhiteSpace
				&& leadingZeroCount == other.leadingZeroCount && Objects.equals(locale, other.locale)
				&& matchCount == other.matchCount && Objects.equals(matchPatternInfo, other.matchPatternInfo)
				&& maxRawLength == other.maxRawLength
				&& monotonicDecreasing == other.monotonicDecreasing && monotonicIncreasing == other.monotonicIncreasing
				&& multiline == other.multiline && nullCount == other.nullCount
				&& outliers.equals(other.outliers) && sampleCount == other.sampleCount
				&& trailingWhiteSpace == other.trailingWhiteSpace
				&& totalCount == other.totalCount
				&& totalNullCount == other.totalNullCount && totalBlankCount == other.totalBlankCount
				&& totalMean == other.totalMean && totalStandardDeviation == other.totalStandardDeviation
				&& totalMinValue == other.totalMinValue && totalMaxValue == other.totalMaxValue
				&& totalMinLength == other.totalMinLength && totalMaxLength == other.totalMaxLength
				&& Objects.equals(uniqueness, other.uniqueness)
				&& Objects.equals(distinctCount, other.distinctCount)
				&& Objects.equals(streamFormat, other.streamFormat)
				&& ((mean == 0.0 && other.mean == 0.0) || Math.abs(mean - other.mean) < epsilon)
				&& ((variance == null && other.variance == null) || (variance == 0.0 && other.variance == 0.0) || Math.abs(variance - other.variance) < epsilon);
	}
}
