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
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;

import com.cobber.fta.core.FTAType;
import com.cobber.fta.dates.DateTimeParser;

/**
 * A set of facts for the Analysis in question.
 */
public class Facts {
	/** The minimum length (not trimmed) - Only relevant for Numeric, Boolean and String. Note: For String and Boolean types this length includes any whitespace. */
	public int minRawLength = Integer.MAX_VALUE;
	/** The maximum length (not trimmed) - Only relevant for Numeric, Boolean and String. Note: For String and Boolean types this length includes any whitespace. */
	public int maxRawLength = Integer.MIN_VALUE;
	/** Are any elements multi-line? */
	protected boolean multiline;
	/** Do any elements have leading White Space? */
	protected boolean leadingWhiteSpace;
	/** Do any elements have trailing White Space? */
	protected boolean trailingWhiteSpace;
	/** The percentage confidence (0-1.0) that the observed stream is a Key field. */
	protected Double keyConfidence;
	/** The number of leading zeros seen in sample set.  Only relevant for type Long. */
	protected long leadingZeroCount;
	/** Get the Decimal Separator used to interpret Doubles.  Only relevant for type double. */
	protected char decimalSeparator = '.';
	/** What is the uniqueness percentage of this column. */
	protected Double uniqueness;

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

	protected double currentM2 = 0.0;
	protected double currentMean = 0.0;

	/** The minimum value observed. */
	protected String minValue;
	/** The maximum value observed. */
	protected String maxValue;
	/** The mean of the observed values (Numeric types only). */
	protected Double mean;
	/** The variance of the observed values (Numeric types only). */
	protected Double variance;
	/** The top 10  values. */
	protected Set<String> topK;
	/** The bottom 10  values. */
	protected Set<String> bottomK;

	private PatternInfo currentPatternInfo;

	private Locale locale;
	private boolean collectStatistics;

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public void setCollectStatistics(boolean collectStatistics) {
		this.collectStatistics = collectStatistics;
	}

	public Facts calculateFacts(PatternInfo matchPatternInfo, long matchCount) {
		currentPatternInfo = matchPatternInfo;

		// We know the type - so calculate a minimum and maximum value
		switch (currentPatternInfo.getBaseType()) {
		case BOOLEAN:
			minValue = String.valueOf(minBoolean);
			maxValue = String.valueOf(maxBoolean);
			break;

		case LONG:
			minValue = String.valueOf(minLong);
			maxValue = String.valueOf(maxLong);
			mean = currentMean;
			variance = currentM2/matchCount;
			bottomK = tbLong.bottomKasString();
			topK = tbLong.topKasString();
			break;

		case DOUBLE:
			final NumberFormat formatter = NumberFormat.getInstance(locale);
			formatter.setMaximumFractionDigits(12);
			formatter.setMinimumFractionDigits(1);
			formatter.setGroupingUsed(false);

			minValue = formatter.format(minDouble);
			maxValue = formatter.format(maxDouble);
			mean = currentMean;
			variance = currentM2/matchCount;
			bottomK = tbDouble.bottomKasString();
			topK = tbDouble.topKasString();
			break;

		case STRING:
			if ("NULL".equals(currentPatternInfo.typeQualifier)) {
				minRawLength = maxRawLength = 0;
			} else if ("BLANK".equals(currentPatternInfo.typeQualifier)) {
				// If all the fields are blank (i.e. a variable number of spaces) - then we have not saved any of the raw input, so we
				// need to synthesize the min and max value, as well as the minRawlength if not set.
				if (minRawLength == Integer.MAX_VALUE)
					minRawLength = 0;
				final StringBuilder s = new StringBuilder(maxRawLength);
				for (int i = 0; i < maxRawLength; i++) {
					if (i == minRawLength)
						minValue = s.toString();
					s.append(' ');
				}
				maxValue = s.toString();
				if (minRawLength == maxRawLength)
					minValue = maxValue;
			}
			else {
				minValue = minString;
				maxValue = maxString;
				bottomK = tbString.bottomKasString();
				topK = tbString.topKasString();
			}
			break;

		case LOCALDATE:
			if (collectStatistics) {
				final DateTimeFormatter dtf = DateTimeParser.ofPattern(currentPatternInfo.format, locale);

				minValue = minLocalDate == null ? null : minLocalDate.format(dtf);
				maxValue = maxLocalDate == null ? null : maxLocalDate.format(dtf);
				bottomK = alignFormat(tbLocalDate.bottomKasString(), FTAType.LOCALDATE, dtf);
				topK = alignFormat(tbLocalDate.topKasString(), FTAType.LOCALDATE, dtf);
			}
			break;

		case LOCALTIME:
			if (collectStatistics) {
				final DateTimeFormatter dtf = DateTimeParser.ofPattern(currentPatternInfo.format, locale);

				minValue = minLocalTime == null ? null : minLocalTime.format(dtf);
				maxValue = maxLocalTime == null ? null : maxLocalTime.format(dtf);
				bottomK = alignFormat(tbLocalTime.bottomKasString(), FTAType.LOCALTIME, dtf);
				topK = alignFormat(tbLocalTime.topKasString(), FTAType.LOCALTIME, dtf);
			}
			break;

		case LOCALDATETIME:
			if (collectStatistics) {
				final DateTimeFormatter dtf = DateTimeParser.ofPattern(currentPatternInfo.format, locale);

				minValue = minLocalDateTime == null ? null : minLocalDateTime.format(dtf);
				maxValue = maxLocalDateTime == null ? null : maxLocalDateTime.format(dtf);
				bottomK = alignFormat(tbLocalDateTime.bottomKasString(), FTAType.LOCALDATETIME, dtf);
				topK = alignFormat(tbLocalDateTime.topKasString(), FTAType.LOCALDATETIME, dtf);
			}
			break;

		case ZONEDDATETIME:
			if (collectStatistics) {
				final DateTimeFormatter dtf = DateTimeParser.ofPattern(currentPatternInfo.format, locale);

				minValue = minZonedDateTime.format(dtf);
				maxValue = maxZonedDateTime.format(dtf);
				bottomK = alignFormat(tbZonedDateTime.bottomKasString(), FTAType.ZONEDDATETIME, dtf);
				topK = alignFormat(tbZonedDateTime.topKasString(), FTAType.ZONEDDATETIME, dtf);
			}
			break;

		case OFFSETDATETIME:
			if (collectStatistics) {
				final DateTimeFormatter dtf = DateTimeParser.ofPattern(currentPatternInfo.format, locale);

				minValue = minOffsetDateTime.format(dtf);
				maxValue = maxOffsetDateTime.format(dtf);
				bottomK = alignFormat(tbOffsetDateTime.bottomKasString(), FTAType.OFFSETDATETIME, dtf);
				topK = alignFormat(tbOffsetDateTime.topKasString(), FTAType.OFFSETDATETIME, dtf);
			}
			break;
		}

		return this;
	}

	/*
	 * Return a new Set (in the same order) as the input set but formatted according to the supplied DateTimeFormatter.
	 * The input set is ordered based on the position on a timeline, not lexigraphically.
	 * Note: The input set is formatted according to the default formatter based on the type.
	 */
	private Set<String> alignFormat(final SortedSet<String> toFix, final FTAType type, final DateTimeFormatter dtf) {
		final Set<String> ret = new LinkedHashSet<>();
		for (final String s : toFix) {
			switch (type) {
			case LOCALDATE:
				ret.add(LocalDate.parse(s).format(dtf));
				break;
			case LOCALTIME:
				ret.add(LocalTime.parse(s).format(dtf));
				break;
			case LOCALDATETIME:
				ret.add(LocalDateTime.parse(s).format(dtf));
				break;
			case ZONEDDATETIME:
				ret.add(ZonedDateTime.parse(s).format(dtf));
				break;
			case OFFSETDATETIME:
				ret.add(OffsetDateTime.parse(s).format(dtf));
				break;
			}
		}

		return ret;
	}
}
