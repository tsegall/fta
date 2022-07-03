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
package com.cobber.fta.dates;

import static com.cobber.fta.dates.DateTimeParserResult.FRACTION_INDEX;
import static com.cobber.fta.dates.DateTimeParserResult.HOUR_INDEX;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import com.cobber.fta.core.FTAMergeException;
import com.cobber.fta.core.InternalErrorException;
import com.cobber.fta.core.MinMax;
import com.cobber.fta.core.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Analyze String data to determine whether input represents a date or datetime.
 *
 * <p>
 * Typical usage is:
 * </p>
 * <pre>
 * {@code
 *		DateTimeParser dtp = new DateTimeParser(false);
 *
 *		dtp.train("2/7/2012 06:24:47");
 *		dtp.train("2/7/2012 09:44:04");
 *		dtp.train("2/7/2012 06:21:38");
 *		dtp.train("1/7/2012 23:16:14");
 *		dtp.train("19/7/2012 17:49:53");
 *
 *		DateTimeParserResult result = dtp.getResult();
 *      // Expect "d/M/yyyy HH:mm:ss");
 *		System.err.println(result.getFormatString());
 * }
 * </pre>
 */
public class DateTimeParser {
	private static final String TIME_ONLY_HHMMSS = "d{2}:d{2}:d{2}";
	private static final String TIME_ONLY_HMMSS = "d:d{2}:d{2}";
	private static final String TIME_ONLY_HHMM = "d{2}:d{2}";
	private static final String TIME_ONLY_HMM = "d:d{2}";
	private static final String TIME_ONLY_PPHMM = "  d:d{2}";

	/** When we have ambiguity - should we prefer to conclude day first, month first, auto (based on locale) or unspecified. */
	public enum DateResolutionMode {
		/** Result returned may have unbound elements, for example ??/??/yyyy. */
		None,
		/** In order to resolve ambiguity the day will be assumed to be first. */
		DayFirst,
		/** In order to resolve ambiguity the month will be assumed to be first. */
		MonthFirst,
		/** Auto will choose DayFirst or MonthFirst based on the Locale. */
		Auto
	}

	enum TimeDateElement {
		Time,
		Date,
		AMPM,
		WhiteSpace,
		TimeZone,
		Indicator_8601
	}

	protected static final Set<String> timeZones = new HashSet<>();
	private static final int[] monthDays = {-1, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

	static {
		// Cache the set of available Time Zones
		Collections.addAll(timeZones, TimeZone.getAvailableIDs());
		// Add the non-real Time Zones (that people use)
		timeZones.addAll(Arrays.asList(
				"ACDT", "ACST", "ACT", "ACWDT", "ACWST", "ADS", "ADST", "ADT", "AEDT", "AEST", "AET", "AFST", "AFT", "AKDT", "AKST", "ALMST",
				"ALMT", "AMDT", "AMST", "AMT", "ANAST", "ANAT", "AQTST", "AQTT", "ARST", "ART", "AST", "AT", "AWDT", "AWST", "AZODT", "AZOST",
				"AZOT", "AZST", "AZT", "BDST", "BDT", "BNST", "BNT", "BOST", "BOT", "BRST", "BST", "BT", "BTST", "BTT", "CAST", "CAT", "CCST",
				"CCT", "CDST", "CDT", "CEDT", "CEST", "CET", "CHADT", "CHAST", "CHODST", "CHODT", "CHOST", "CHOT", "CHUST", "CHUT", "CIDST",
				"CIST", "CIT", "CKHST", "CKT", "CLDT", "CLST", "CLT", "COST", "COT", "CST", "CT", "CVST", "CVT", "CXST", "CXT", "ChDT", "ChST",
				"DAVST", "DAVT", "DDUST", "DDUT", "EADT", "EASST", "EAST", "EAT", "ECST", "ECT", "EDST", "EDT", "EEDT", "EEST", "EET", "EFATE",
				"EGST", "EGT", "EIST", "EST", "ET", "FET", "FJDT", "FJST", "FJT", "FKDT", "FKST", "FKT", "FNST", "FNT", "GALST", "GALT", "GAMST",
				"GAMT", "GDT", "GEST", "GET", "GFST", "GFT", "GHST", "GILST", "GILT", "GMT", "GST", "GT", "GYST", "GYT", "HAA", "HAC", "HADT",
				"HAE", "HAP", "HAR", "HAST", "HAT", "HDT", "HKST", "HKT", "HLV", "HNA", "HNC", "HNE", "HNP", "HNR", "HNT", "HOVDST", "HOVDT",
				"HOVST", "HOVT", "HST", "ICST", "ICT", "IDT", "IOST", "IOT", "IRDT", "IRKST", "IRKT", "IRST", "IST", "IT", "JDT", "JST", "KDT",
				"KGST", "KGT", "KIT", "KOSST", "KOST", "KRAST", "KRAT", "KST", "KT", "KUYT", "LHDT", "LHST", "LINST", "LINT", "MAGST", "MAGT",
				"MARST", "MART", "MAWST", "MAWT", "MCK", "MDST", "MDT", "MEST", "MESZ", "MEZ", "MHST", "MHT", "MIDT", "MMST", "MMT", "MSD", "MSK",
				"MST", "MT", "MUST", "MUT", "MVST", "MVT", "MYST", "MYT", "NACDT", "NACST", "NAEDT", "NAEST", "NAMDT", "NAMST", "NAPDT", "NAPST",
				"NCST", "NCT", "NDT", "NFST", "NFT", "NOVST", "NOVT", "NPST", "NPT", "NRST", "NRT", "NST", "NUST", "NUT", "NZDT", "NZST", "OESZ",
				"OEZ", "OMSST", "OMST", "ORAST", "ORAT", "PDST", "PDT", "PEST", "PET", "PETST", "PETT", "PGST", "PGT", "PHOST", "PHOT", "PHST",
				"PHT", "PKST", "PKT", "PMDT", "PMST", "PONST", "PONT", "PST", "PT", "PWST", "PWT", "PYST", "PYT", "Pacific", "QYZST", "QYZT",
				"REST", "RET", "ROTST", "ROTT", "SAKST", "SAKT", "SAMST", "SAMT", "SAST", "SBST", "SBT", "SCST", "SCT", "SDT", "SGST", "SGT",
				"SREDT", "SRET", "SRST", "SRT", "SST", "ST", "SYOST", "SYOT", "TAHST", "TAHT", "TFST", "TFT", "TJST", "TJT", "TKST", "TKT", "TLST",
				"TLT", "TMST", "TMT", "TOST", "TOT", "TRT", "TVST", "TVT", "ULAST", "ULAT", "UTC", "UYST", "UYT", "UZST", "UZT", "VEST", "VET",
				"VLAST", "VLAT", "VOSST", "VOST", "VUST", "VUT", "WAKST", "WAKT", "WARST", "WAST", "WAT", "WDT", "WEDT", "WEST", "WESZ", "WET",
				"WEZ", "WFST", "WFT", "WGST", "WGT", "WIB", "WIST", "WIT", "WITA", "WSDT", "WST", "WT", "XJDT", "YAKST", "YAKT", "YAPT", "YEKST",
				"YEKT" ));
	}

	private DateTimeParserState state = new DateTimeParserState();
	private DateTimeParserConfig config = new DateTimeParserConfig();

	private static final Map<String, DateTimeFormatter> formatterCache = new HashMap<>();

	private LocaleInfo localeInfo;

	/**
	 * Construct a DateTimeParser with DateResolutionMode = None, and in the default Locale.
	 */
	public DateTimeParser() {
		config.resolutionMode = DateResolutionMode.None;
		config.locale = Locale.getDefault();
	}

	/**
	 * Construct a DateTimeParse with the specified DateResolutionMode.
	 * @param resolutionMode When we have ambiguity - should we prefer to conclude day first, month first, auto (based on locale) or unspecified.
	 * @deprecated Since 8.X use the fluent API instead
	 */
	@Deprecated
	public DateTimeParser(final DateResolutionMode resolutionMode) {
		config.resolutionMode = resolutionMode;
		config.locale = Locale.getDefault();
	}

	/**
	 * Construct a DateTimeParse with the specified DateResolutionMode and Locale.
	 * @param resolutionMode When we have ambiguity - should we prefer to conclude day first, month first, auto (based on locale) or unspecified.
	 * @param locale Locale the input string is in
	 * @deprecated Since 8.X use the fluent API instead
	 */
	@Deprecated
	public DateTimeParser(final DateResolutionMode resolutionMode, final Locale locale) {
		config.resolutionMode = resolutionMode;
		config.locale = locale;
	}

	/**
	 * Set the DateResolutionMode on the Parser.
	 * @param resolutionMode When we have ambiguity - should we prefer to conclude day first, month first, auto (based on locale) or unspecified.
	 * @return The DateTimeParser
	 */
	public DateTimeParser withDateResolutionMode(final DateResolutionMode resolutionMode) {
		config.resolutionMode = resolutionMode;
		return this;
	}

	/**
	 * Set the Locale on the Parser.
	 * @param locale Locale the input string is in
	 * @return The DateTimeParser
	 */
	public DateTimeParser withLocale(final Locale locale) {
		config.locale = locale;
		return this;
	}

	/**
	 * Set Strict mode on the Parser - if set, any input to train() that would not pass the current 'best' guess will return null.
	 * @param strict The new value for Strict mode
	 * @return The DateTimeParser
	 */
	public DateTimeParser withStrictMode(final boolean strict) {
		config.strictMode = strict;
		return this;
	}

	/**
	 * Set NoAbbreviationPunctuation mode on the Parser - if set then use month abbreviations without periods.
	 * This attempts to workaround the fact that Java will return things like Aug. for month abbreviation in Canada
	 * (and other locales) whereas many dates are simply of the form '02-Aug-2013' (i.e. without the period).
	 * Similarly for the AM/PM string which are defined in Canada as A.M and P.M.
	 *
	 * @param noAbbreviationPunctuation The new value for noAbbreviationPunctuation mode.
	 * @return The DateTimeParser
	 */
	public DateTimeParser withNoAbbreviationPunctuation(final boolean noAbbreviationPunctuation) {
		config.noAbbreviationPunctuation = noAbbreviationPunctuation;
		return this;
	}

	/**
	 * Retrieve the Configuration for this DateTimeParser.
	 * @return The current Configuration.
	 */
	public DateTimeParserConfig getConfig() {
		return config;
	}

	/**
	 * Given an input string with a DateTimeFormatter pattern return a suitable DateTimeFormatter.
	 * This is very similar to DateTimeFormatter.ofPattern(), however, there are a set of key differences:
	 *  - This will cache the Formatters
	 *  - It supports a slightly extended syntax, the following are supported:
	 *    - Year only - "yyyy"
	 *    - S{min,max} to reflect a variable number of digits in a fractional seconds component
	 *    - Year month only - "MM/YYYY" or "MM-YYYY" or "YYYY/MM" or "YYYY-MM"
	 *  - The formatter returned is always case-insensitive
	 *
	 * @param formatString A DateTimeString using DateTimeFormatter patterns
	 * @return The corresponding DateTimeFormatter (note - this will be a case-insensitive parser).
	 */
	public DateTimeFormatter ofPattern(final String formatString) {
		final String cacheKey = config.locale.toLanguageTag() + "---" + formatString + "---" + config.noAbbreviationPunctuation;
		DateTimeFormatter formatter = formatterCache.get(cacheKey);

		if (formatter != null)
			return formatter;

		int offset = formatString.indexOf("S{");
		if (offset != -1) {
			final MinMax minMax = new MinMax(formatString.substring(offset, formatString.indexOf('}', offset)));
			final DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
					.appendPattern(formatString.substring(0, offset))
					.appendFraction(ChronoField.MICRO_OF_SECOND, minMax.getMin(), minMax.getMax(), false);
			final int upto = offset + minMax.getPatternLength();
			if (upto < formatString.length())
				builder.appendPattern(formatString.substring(upto));
			formatter = builder.toFormatter(config.locale);
		}
		else if ("yyyy".equals(formatString))
            // The default formatter with "yyyy" will not default the month/day, make it so!
            formatter = new DateTimeFormatterBuilder()
            .appendPattern("yyyy")
            .parseCaseInsensitive()
            .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
            .toFormatter(config.locale);
		else if ("MM/yyyy".equals(formatString) || "MM-yyyy".equals(formatString) || "yyyy/MM".equals(formatString) || "yyyy-MM".equals(formatString))
			formatter = new DateTimeFormatterBuilder()
			.parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
			.append(DateTimeFormatter.ofPattern(formatString))
			.toFormatter(config.locale);
		else if (config.noAbbreviationPunctuation && (offset = formatString.indexOf("MMM")) != -1 && offset != formatString.indexOf("MMMM")) {
			if (localeInfo == null)
				localeInfo = LocaleInfo.getInstance(config.locale, config.noAbbreviationPunctuation);

			// Setup the Monthly abbreviations, in Java some countries (e.g. Canada) have the short months defined with a
			// period after them, for example 'AUG.' - we compensate by removing the punctuation
			Map<Long, String> lookup = new HashMap<>();
			long index = 1;
			for (String month : localeInfo.getShortMonthsArray())
				lookup.put(index++, month);

			final DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
		            .parseCaseInsensitive()
					.appendPattern(formatString.substring(0, offset))
					.appendText(ChronoField.MONTH_OF_YEAR, lookup);
			final int upto = offset + "MMM".length();
			if (upto < formatString.length())
				builder.appendPattern(formatString.substring(upto));
			formatter = builder.toFormatter(config.locale);
		}
		else
			formatter = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(formatString).toFormatter(config.locale);

		formatterCache.put(cacheKey, formatter);

		return formatter;
	}

	/**
	 * train() is the core entry point used to supply input to the DateTimeParser.  The returned value from this method is the
	 * DateTimeFormatter Pattern for this input.  For the consolidated result of all the training see {@link #getResult()}.
	 * Note: If {@link #withStrictMode(boolean)} is set then train will return null if the input does not match the current consolidated training result.
	 * @param input The String representing a date with possible surrounding whitespace.
	 * @return A String representing the DateTime detected for this input (Using DateTimeFormatter Patterns) or null if no match.
	 */
	public String train(final String input) {
		if (localeInfo == null)
			localeInfo = LocaleInfo.getInstance(config.locale, config.noAbbreviationPunctuation);

		if (config.strictMode && state.invalidCount != 0)
			return null;

		state.sampleCount++;

		if (input == null) {
			state.nullCount++;
			return null;
		}

		final String trimmed = input.trim();

		if (trimmed.length() == 0) {
			state.blankCount++;
			return null;
		}

		// We determine the format using no resolution mode - so as not to bias the training
		final String ret = determineFormatString(trimmed, DateResolutionMode.None);
		if (ret == null) {
			state.invalidCount++;
			return null;
		}
		final Integer seen = state.results.get(ret);
		if (seen == null)
			state.results.put(ret, 1);
		else
			state.results.put(ret, seen + 1);

		if (config.strictMode) {
			// If we are insisting that all records are valid - then we need to check as we go that
			// each input matches the current result.
			try {
				getResult().parse(input);
			}
			catch (DateTimeParseException e) {
				// If we cannot parse this input - then we are done!
				state.invalidCount++;
				return null;
			}
		}

		return ret;
	}

	/**
	 * Determine the result of the training complete to date. Typically invoked
	 * after all training is complete, but may be invoked at any stage.
	 * @return A DateTimeParserResult with the analysis of any training completed, or null if no answer.
	 */
	public DateTimeParserResult getResult() {
		// If we have no good samples or we are in Strict mode and have seen an error, call it a day
		if (state.sampleCount == state.nullCount + state.blankCount + state.invalidCount || (config.strictMode && state.invalidCount != 0))
			return null;

		DateTimeParserResult answerResult = null;
		StringBuilder answerBuffer = null;

		// If there is only one result then it must be correct :-)
		if (state.results.size() == 1) {
			answerResult = DateTimeParserResult.asResult(state.results.keySet().iterator().next(), config.resolutionMode, config);
			// If we are fully bound then we are done!
			if (!answerResult.isDateUnbound() || config.resolutionMode == DateResolutionMode.None)
				return answerResult;
			answerResult = DateTimeParserResult.newInstance(answerResult);
			answerBuffer = new StringBuilder(answerResult.getFormatString());
		}
		else {
			// Sort the results of our training by value so that we consider the most frequent first
			final Map<String, Integer> byValue = Utils.sortByValue(state.results);

			// Iterate through all the results of our training, merging them to produce our best guess
			for (final Map.Entry<String, Integer> entry : byValue.entrySet()) {
				final String key = entry.getKey();
				final DateTimeParserResult result = DateTimeParserResult.asResult(key, config.resolutionMode, config);

				// First entry
				if (answerBuffer == null) {
					answerBuffer = new StringBuilder(key);
					answerResult = DateTimeParserResult.newInstance(result);
					continue;
				}

				// Process any time-related information (can only merge if they both had a time component)
				if (result.timeElements != -1 && answerResult.timeElements != -1) {
					if (answerResult.timeFirst == null)
						answerResult.timeFirst = result.timeFirst;
					if (answerResult.timeZone == null)
						answerResult.timeZone = result.timeZone;
					if (answerResult.amPmIndicator == null)
						answerResult.amPmIndicator = result.amPmIndicator;

					// If we were H (0-23) and we have a k (1-24) then assume k
					final char was = answerBuffer.charAt(answerResult.timeFieldOffsets[HOUR_INDEX]);
					final char is = result.getFormatString().charAt(result.timeFieldOffsets[HOUR_INDEX]);
					if (was == 'H' && is == 'k') {
						final int start = answerResult.timeFieldOffsets[HOUR_INDEX];
						answerResult.timeFieldLengths[0].merge(result.timeFieldLengths[HOUR_INDEX]);
						final int len = answerResult.timeFieldLengths[HOUR_INDEX].getMin();
						answerBuffer.replace(start, start + len, Utils.repeat('k', len));
					}

					if (result.timeFieldLengths != null) {
						// Shrink the Hours, Minutes, or Seconds fields if the length is shorter
						// Expand the fractions of Seconds if the length is longer
						for (int i = 0; i < result.timeFieldLengths.length; i++) {
							if (!answerResult.timeFieldLengths[i].isSet())
								answerResult.timeFieldLengths[i] = result.timeFieldLengths[i];
							else {
								// Hours, Minutes, or Seconds
								if (i < result.timeFieldLengths.length - 1 && result.timeFieldLengths[i].getMin() < answerResult.timeFieldLengths[i].getMin()) {
									final int start = answerResult.timeFieldOffsets[i];
									final int len = answerResult.timeFieldLengths[i].getMin();
									answerResult.timeFieldLengths[i].setMin(result.timeFieldLengths[i].getMin());

									// Need to reset all the offsets in the answerResult
									for (int j = i + 1; j < result.timeFieldLengths.length; j++)
										answerResult.timeFieldOffsets[j]--;
									if (result.dateElements != -1 && answerResult.timeFieldOffsets[0] < answerResult.dateFieldOffsets[0])
										for (int j = 0; j < result.dateFieldLengths.length; j++)
											answerResult.dateFieldOffsets[j]--;

									// Fix up the String
									answerBuffer.replace(start, start + len, Utils.repeat(answerBuffer.charAt(start), result.timeFieldLengths[i].getMin()));
								}
								// Fractions of Seconds
								else if (i == FRACTION_INDEX && result.timeFieldLengths[FRACTION_INDEX].compareTo(answerResult.timeFieldLengths[FRACTION_INDEX]) != 0) {
									final int start = answerResult.timeFieldOffsets[FRACTION_INDEX];
									final int len = answerResult.timeFieldLengths[FRACTION_INDEX].getPatternLength();
									// Fix up the String
									answerResult.timeFieldLengths[i].merge(result.timeFieldLengths[FRACTION_INDEX]);
									answerBuffer.replace(start, start + len, answerResult.timeFieldLengths[FRACTION_INDEX].getPattern('S'));
								}
							}
						}
					}
				}

				// Process any date-related information
				if (result.dateElements != -1) {
					// Ensure that we have compatible date formats - if not skip it
					if (answerResult.yearOffset != result.yearOffset && answerResult.yearOffset != -1 && result.yearOffset != -1 ||
							answerResult.monthOffset != result.monthOffset && answerResult.monthOffset != -1 && result.monthOffset != -1 ||
							answerResult.dayOffset != result.dayOffset && answerResult.dayOffset != -1 && result.dayOffset != -1
							)
						continue;

					if (answerResult.dayOffset == -1 && result.dayOffset != -1) {
						// We did not know where the day was and now do
						answerResult.dayOffset = result.dayOffset;
						final int start = answerResult.dateFieldOffsets[answerResult.dayOffset];
						final int len = answerResult.dateFieldLengths[answerResult.dayOffset];
						answerBuffer.replace(start, start + len, Utils.repeat('d', len));
					}
					if (answerResult.monthOffset == -1 && result.monthOffset != -1) {
						// We did not know where the month was and now do
						answerResult.monthOffset = result.monthOffset;
						final int start = answerResult.dateFieldOffsets[answerResult.monthOffset];
						final int len = answerResult.dateFieldLengths[answerResult.monthOffset];
						answerBuffer.replace(start, start + len, Utils.repeat('M', len));
					}
					if (answerResult.yearOffset == -1 && result.yearOffset != -1) {
						// We did not know where the year was and now do
						answerResult.yearOffset = result.yearOffset;
						final int start = answerResult.dateFieldOffsets[answerResult.yearOffset];
						final int len = answerResult.dateFieldLengths[answerResult.yearOffset];
						answerBuffer.replace(start, start + len, Utils.repeat('y', len));
					}

					if (answerResult.dateElements == -1)
						answerResult.dateElements = result.dateElements;
					if (answerResult.dateSeparator == null)
						answerResult.dateSeparator = result.dateSeparator;
					if (answerResult.dateTimeSeparator == null)
						answerResult.dateTimeSeparator = result.dateTimeSeparator;

					// If the result we are looking at has the same format as the current answer then merge lengths
					if (answerResult.yearOffset == result.yearOffset &&
							(answerResult.monthOffset == result.monthOffset || result.monthOffset == -1))
						for (int i = 0; i < result.dateFieldLengths.length; i++) {
							if (answerResult.dateFieldLengths[i] == -1 && result.dateFieldLengths[i] != -1)
								answerResult.dateFieldLengths[i] = result.dateFieldLengths[i];
							else if (i != answerResult.yearOffset && answerResult.dateFieldLengths[i] != result.dateFieldLengths[i]) {
								// Merge two date lengths:
								//  - dd (and d) -> d
								//  - dd (and ppd) -> ppd
								//  - HH (and ppH) -> ppH
								//  - MM (and M) -> M
								//  - ?? (and ?) -> ?
								//  - MMM (and MMMM) -> MMMM
								final int start = answerResult.dateFieldOffsets[i];
								final int len = answerResult.dateFieldLengths[i];
								final String was = answerBuffer.substring(start, start + len);
								String replacement;
								if ("MMM".equals(was))
									replacement = "MMMM";
								else if ("??".equals(was) || "HH".equals(was) || "hh".equals(was) || "dd".equals(was) || "MM".equals(was)) {
									replacement = result.dateFieldPad[i] != 0 ? "pp" : "";
									replacement += was.charAt(0);
								}
								else
									continue;

								// Need to reset all the offsets in the answerResult
								final int delta = was.length() - replacement.length();
								for (int j = i + 1; j < result.dateFieldLengths.length; j++)
									 answerResult.dateFieldOffsets[j] -= delta;
								if (result.timeFieldLengths != null && answerResult.dateFieldOffsets[0] < answerResult.timeFieldOffsets[0])
									for (int j = 0; j < result.timeFieldLengths.length; j++)
										answerResult.timeFieldOffsets[j] -= delta;

								answerResult.dateFieldLengths[i] = result.dateFieldLengths[i];
								answerBuffer.replace(start, start + len, replacement);
							}
						}
				}
			}
		}

		// If we are supposed to be fully bound and still have some ambiguities then fix them based on the mode
		if (answerResult.isDateUnbound() && config.resolutionMode != DateResolutionMode.None)
			if (answerResult.monthOffset != -1) {
				int start = answerResult.dateFieldOffsets[0];
				answerBuffer.replace(start, start + answerResult.dateFieldLengths[0], Utils.repeat('d', answerResult.dateFieldLengths[0]));
				start = answerResult.dateFieldOffsets[2];
				answerBuffer.replace(start, start + answerResult.dateFieldLengths[2], Utils.repeat('y', answerResult.dateFieldLengths[2]));
			}
			else {
					final char firstField = config.resolutionMode == DateResolutionMode.DayFirst ? 'd' : 'M';
					final char secondField = config.resolutionMode == DateResolutionMode.DayFirst ? 'M' : 'd';
					final int idx = answerResult.yearOffset == 0 ? 1 : 0;
					int start = answerResult.dateFieldOffsets[idx];
					answerBuffer.replace(start, start + answerResult.dateFieldLengths[idx], Utils.repeat(firstField, answerResult.dateFieldLengths[idx]));
					start = answerResult.dateFieldOffsets[idx + 1];
					answerBuffer.replace(start, start + answerResult.dateFieldLengths[idx + 1], Utils.repeat(secondField, answerResult.dateFieldLengths[idx + 1]));
			}

		if (answerResult.timeZone == null)
			answerResult.timeZone = "";

		answerResult.updateFormatString(answerBuffer.toString());
		return answerResult;
	}

	private boolean plausibleDate(final DateTracker tracker, final int[] fieldOffsets) {
		return plausibleDateCore(config.lenient, tracker.getValue(fieldOffsets[0]), tracker.getValue(fieldOffsets[1]),
				tracker.getValue(fieldOffsets[2]), tracker.getDigit(fieldOffsets[2]));
	}

	public static boolean plausibleDateCore(final boolean lenient, final int day, final int month, final int year, final int yearLength) {
		if (lenient && year == 0 && month == 0 && day == 0)
			return true;

		if (year == 0 && yearLength == 4)
			return false;
		if (month == 0 || month > 12)
			return false;

		return day != 0 && day <= monthDays[month];
	}

	private static String dateFormat(final DateTracker tracker, final char separator, final DateResolutionMode resolutionMode, final boolean yearKnown, final boolean yearFirst) {
		if (yearFirst)
			switch (resolutionMode) {
			case None:
				return Utils.repeat(yearKnown ? 'y' : '?', tracker.getDigit(0))  + separator + Utils.repeat('?', tracker.getDigit(1)) + separator + Utils.repeat('?', tracker.getDigit(2));
			case DayFirst:
				return  Utils.repeat('y', tracker.getDigit(0)) + separator + Utils.repeat('d', tracker.getDigit(1)) + separator + Utils.repeat('M', tracker.getDigit(2));
			case MonthFirst:
				return Utils.repeat('y', tracker.getDigit(0)) + separator + Utils.repeat('M', tracker.getDigit(1)) + separator + Utils.repeat('d', tracker.getDigit(2));
			default:
				throw new InternalErrorException("unexpected resolutionMode: " + resolutionMode);
			}
		else
			switch (resolutionMode) {
			case None:
				return Utils.repeat('?', tracker.getDigit(0)) + separator + Utils.repeat('?', tracker.getDigit(1)) + separator + Utils.repeat(yearKnown ? 'y' : '?', tracker.getDigit(2));
			case DayFirst:
				return Utils.repeat('d', tracker.getDigit(0)) + separator + Utils.repeat('M', tracker.getDigit(1)) + separator + Utils.repeat('y', tracker.getDigit(2));
			case MonthFirst:
				return Utils.repeat('M', tracker.getDigit(0)) + separator + Utils.repeat('d', tracker.getDigit(1)) + separator + Utils.repeat('y', tracker.getDigit(2));
			default:
				throw new InternalErrorException("unexpected resolutionMode: " + resolutionMode);
			}
	}

	/**
	 * Determine a FormatString from an input string that may represent a Date, Time,
	 * DateTime, OffsetDateTime or a ZonedDateTime.
	 * @param input The String representing a date with optional leading/trailing whitespace
	 * The resolution mode will default to the mode provided when the DateTimeParser was initially constructed.
	 * @return A String representing the DateTime detected (Using DateTimeFormatter Patterns) or null if no match.
	 */
	public String determineFormatString(final String input) {
		return determineFormatString(input,  config.resolutionMode);
	}

	/**
	 * Determine a FormatString from an input string that may represent a Date, Time,
	 * DateTime, OffsetDateTime or a ZonedDateTime.
	 * @param input The String representing a date with optional leading/trailing whitespace
	 * @param resolutionMode When we have ambiguity - should we prefer to conclude day first, month first, auto (based on locale) or unspecified.
	 * @return A String representing the DateTime detected (Using DateTimeFormatter Patterns) or null if no match.
	 */
	public String determineFormatString(final String input, final DateResolutionMode resolutionMode) {
		if (localeInfo == null)
			localeInfo = LocaleInfo.getInstance(config.locale, config.noAbbreviationPunctuation);

		int len = input.length();

		// Remove leading spaces
		int start = 0;
		while (start < len && input.charAt(start) == ' ')
			start++;

		// Remove trailing spaces
		while (len >= 1 && input.charAt(len - 1) == ' ')
			len--;

		len -= start;

		final String trimmed = input.substring(start, len + start);

		// Fail fast if we can
		if (len < 4 || len > 70 || (!Character.isDigit(trimmed.codePointAt(0)) && !Character.isAlphabetic(trimmed.codePointAt(0))))
			return null;

		if (trimmed.indexOf('¶') != -1)
			return null;

		final SimpleDateMatcher matcher = new SimpleDateMatcher(trimmed, localeInfo);

		// Initial pass is a simple match against a set of known patterns
		final String formatPassOne = passOne(matcher);
		if (formatPassOne != null)
			return formatPassOne;

		// Fail fast if we can
		if (matcher.getComponentCount() < 2 || !Character.isLetterOrDigit(trimmed.charAt(0)))
			return null;

		// Second pass is an attempt to 'parse' the provided input string and derive a format
		final String formatPassTwo = passTwo(trimmed, resolutionMode);

		if (formatPassTwo != null)
			return formatPassTwo;

		if ("ja".equals(config.locale.getLanguage()) || "zh".equals(config.locale.getLanguage()))
			return passJaZh(trimmed, matcher, resolutionMode);

		// Third and final pass is brute force by elimination
		return passThree(trimmed, matcher, resolutionMode);
	}

	private char jaCnDateTimeMapper(final char ch) {
		switch (ch) {
		case '年':
			return 'y';
		case  '月':
			return 'M';
		case '日':
		case '号':
			return 'd';
		case '時':
			return 'H';
		case '分':
			return 'm';
		case '秒':
			return 's';
		default:
			return '¶';
		}
	}

	private String passJaZh(final String trimmed, final SimpleDateMatcher matcher, final DateResolutionMode resolutionMode) {
		final int yearIndex = trimmed.indexOf('年');
		final int hourIndex = trimmed.indexOf('時');
		if (yearIndex == -1 && hourIndex == -1)
			return null;

		String input = trimmed;
		// Only hunt for AM/PM strings if we have seen the Hours character
		if (hourIndex != -1)
			for (final String s : localeInfo.getAMPMStrings()) {
				final int find = trimmed.indexOf(s);
				if (find != -1)
					input = Utils.replaceAt(trimmed, find, s.length(), "a");
			}

		final int len = input.length();
		char workingOn = '¶';
		int digits = 0;

		// If we have a year then try to weed out some rubbish by insisting that the year is up front
		if (yearIndex != -1 && !(yearIndex == 0 || Character.isDigit(trimmed.charAt(0))))
			return null;

		StringBuffer result = new StringBuffer(len);
		for (int i = len - 1; i >= 0; i--) {
			char ch = input.charAt(i);
			if (Character.isDigit(ch))
				ch = 'd';
			switch (ch) {
			// Year
			case '年':
			// Month
			case '月':
			// Day
			case '日':
			case '号':
			// Hour
			case '時':
			// Minute
			case '分':
			// Second
			case '秒':
				if (digits != 0) {
					result.append(Utils.repeat(workingOn, digits));
					digits = 0;
				}
				result.append(ch);
				workingOn = jaCnDateTimeMapper(ch);
				break;
			case 'd':
				digits++;
				break;
			default:
				if (digits != 0) {
					result.append(Utils.repeat(workingOn, digits));
					digits = 0;
				}
				result.append(ch);
				break;
			}
		}

		if (digits != 0)
			result.append(Utils.repeat(workingOn, digits));

		result = result.reverse();

		// So we think we have nailed it - but it only counts if it happily passes a validity check
		final DateTimeParserResult dtp = DateTimeParserResult.asResult(result.toString(), resolutionMode, config);

		return (dtp != null && dtp.isValid(trimmed)) ? result.toString() : null;
	}

	/**
	 * This is the first simple pass where we test against a set of predefined simple options.
	 *
	 * @param matcher The previously computed matcher which provides both the compressed form of the input as well as a component count
	 * @return a DateTimeFormatter pattern.
	 */
	private String passOne(final SimpleDateMatcher matcher) {
		SimpleFacts simpleFacts = SimpleDateMatcher.getSimpleDataFacts().get(matcher.getCompressed());

		if (simpleFacts == null)
			return null;

		if (!matcher.parse(simpleFacts.getFormat()))
			return null;

		final DateTracker dateTracker = new DateTracker();
		dateTracker.setComponent(matcher.getDayOfMonth(), matcher.getDayLength(), -1);
		dateTracker.setComponent(matcher.getMonthValue(), matcher.getMonthLength(), -1);
		dateTracker.setComponent(matcher.getYear(), matcher.getYearLength(), -1);

		if (!plausibleDate(dateTracker, new int[] {0,1,2}))
			return null;

		return simpleFacts.getFormat();
	}

	class DateTimeTracker {
		private int[] valueArray;
		private int[] digitsArray;
		private int[] padArray;
		/** Has we seen the start of a Date/Time. */
		boolean seen;
		/** Has the Date/Time been closed. */
		boolean closed;

		int current;

		DateTimeTracker(final int components) {
			valueArray = new int[components];
			digitsArray = new int[components];
			padArray = new int[components];
		}

		boolean setComponent(final int value, final int digits, final int padding) {
			if (current >= valueArray.length)
				return false;

			// If we have set a component then it has clearly been seen
			seen = true;
			valueArray[current] = value;
			digitsArray[current] = digits;
			padArray[current] = padding;

			current++;

			return true;
		}

		int getValue(final int i) {
			return valueArray[i];
		}

		int getDigit(final int i) {
			return digitsArray[i];
		}

		int getPad(final int i) {
			return padArray[i];
		}

		boolean seen() {
			return seen;
		}

		boolean isClosed() {
			return closed;
		}

		void close() {
			closed = true;
		}

		int lastSet() {
			return current - 1;
		}

		int components() {
			return current;
		}
	}

	class TimeTracker extends DateTimeTracker {
		TimeTracker() {
			super(4);
		}

		@Override
		boolean setComponent(final int value, final int digits, final int padding) {
			super.setComponent(value, digits, padding);

			if (current - 1 == 0)
				return digits == 1 || digits == 2;
			if (current - 1 == 1 || current - 1 == 2)
				return digits == 2;

			// You can have any number of fractional seconds
			return true;
		}
	}

	class DateTracker extends DateTimeTracker {
		DateTracker() {
			super(3);
		}
	}

	/**
	 * This is core 'intuitive' pass where we hunt in some logical fashion for something that looks like a date/time.
	 *
	 * @param trimmed The input we are scouring for a date/datetime/time
	 * @param resolutionMode When we have ambiguity - should we prefer to conclude day first, month first, auto (based on locale) or unspecified.
	 * @return a DateTimeFormatter pattern.
	 */
	private String passTwo(final String trimmed, final DateResolutionMode resolutionMode) {
		final List<TimeDateElement> timeDateElements = new ArrayList<>();
		final int len = trimmed.length();
		int digits = 0;
		int value = 0;
		final DateTracker dateTracker = new DateTracker();
		final TimeTracker timeTracker = new TimeTracker();
		char dateSeparator = '_';
		int hourLength = -1;
		boolean yearInDateFirst = false;
		boolean fourDigitYear = false;
		String timeZone = "";
		boolean ampmDetected = false;
		boolean iso8601 = false;
		int padding = 0;

		int lastCh = '¶';
		for (int i = 0; i < len && timeZone.length() == 0; i++) {
			final char ch = trimmed.charAt(i);

			// Two spaces in a row implies padding
			if (lastCh == ' ' && ch == ' ') {
				padding++;
				continue;
			}
			lastCh = ch;

			switch (ch) {
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				value = value * 10 + ch - '0';
				digits++;
				if (digits > 9)
					return null;
				break;

			case ':':
				if ((dateTracker.seen() && !dateTracker.isClosed()) || (timeTracker.seen() && timeTracker.isClosed()) || timeTracker.components() == 3)
					return null;

				if (!timeTracker.setComponent(value, digits, padding))
					return null;

				if (timeTracker.lastSet() == 0) {
					if (digits != 1 && digits != 2)
						return null;
					hourLength = digits;
				}
				if (timeTracker.lastSet() == 1 && digits != 2)
					return null;
				if (timeTracker.lastSet() == 2)
					return null;
				digits = 0;
				value = 0;
				padding = 0;
				break;

			case '+':
				// FALL THROUGH
			case '-':
				if (dateTracker.seen() && dateTracker.isClosed() && timeTracker.seen() && timeTracker.components() >= 2) {
					int minutesOffset = Integer.MIN_VALUE;
					int secondsOffset = Integer.MIN_VALUE;

					i++;

					final String offset = SimpleDateMatcher.compress(trimmed.substring(i, len), localeInfo);

					// Expecting DD:DD:DD or DDDDDD or DD:DD or DDDD or DD
					if (i + 8 <= len && TIME_ONLY_HHMMSS.equals(offset)) {
						timeZone = "xxxxx";
						minutesOffset = 3;
						secondsOffset = 6;
					}
					else if (i + 6 <= len && "d{6}".equals(offset)) {
						timeZone = "xxxx";
						minutesOffset = 2;
						secondsOffset = 4;
					}
					else if (i + 5 <= len && "d{2}:d{2}".equals(offset)) {
						timeZone = "xxx";
						minutesOffset = 3;
					}
					else if (i + 4 <= len && "d{4}".equals(offset)) {
						timeZone = "xx";
					}
					else if (i + 2 <= len && "d{2}".equals(offset)) {
						timeZone = "x";
					}
					else
						return null;

					if (timeTracker.seen() && !timeTracker.isClosed()) {
						// Need to close out the time before we can add the TimeZone
						if (!timeTracker.setComponent(value, digits, padding))
							return null;
						digits = 0;
						timeTracker.close();
						timeDateElements.add(TimeDateElement.Time);
					}

					timeDateElements.add(TimeDateElement.TimeZone);

					// Validate the hours
					final int hours = Utils.getValue(trimmed, i, 2, 2);
					if (hours != Integer.MIN_VALUE && hours > 18)
						return null;

					// Validate the minutes
					if (minutesOffset != Integer.MIN_VALUE) {
						final int minutes = Utils.getValue(trimmed, i + minutesOffset, 2, 2);
						if (minutes != Integer.MIN_VALUE && minutes > 59)
							return null;
					}

					// Validate the seconds
					if (secondsOffset != Integer.MIN_VALUE) {
						final int seconds = Utils.getValue(trimmed, i + secondsOffset, 2, 2);
						if (seconds != Integer.MIN_VALUE && seconds > 59)
							return null;
					}
					break;
				}
				// FALL THROUGH
			case '/':
				if (timeTracker.seen() && !timeTracker.isClosed())
					return null;

				if (!dateTracker.setComponent(value, digits, padding))
					return null;

				if (dateTracker.lastSet() == 0) {
					dateSeparator = ch;
					fourDigitYear = digits == 4;
					yearInDateFirst = fourDigitYear || (digits == 2 && value > 31);
					if (!yearInDateFirst && digits != 1 && digits != 2)
						return null;
				} else if (dateTracker.lastSet() == 1) {
					if (ch != dateSeparator)
						return null;
					if (digits != 1 && digits != 2)
						return null;
				}

				digits = 0;
				value = 0;
				padding = 0;
				break;

			case '.':
				// If we are not processing the time component
				if ((!timeTracker.seen() || timeTracker.isClosed())) {
					// Expecting a 'dotted' date - e.g. 9.12.2008
					if (!dateTracker.setComponent(value, digits, padding))
						return null;

					if (dateTracker.lastSet() == 0) {
						dateSeparator = ch;
						fourDigitYear = digits == 4;
						yearInDateFirst = fourDigitYear || (digits == 2 && value > 31);
						if (!yearInDateFirst && digits != 1 && digits != 2)
							return null;
					} else if (dateTracker.lastSet() == 1) {
						if (ch != dateSeparator)
							return null;
						if (digits != 1 && digits != 2)
							return null;
					}
				}
				else {
					if ((dateTracker.seen() && !dateTracker.isClosed()) || (timeTracker.seen() && timeTracker.isClosed()) || timeTracker.components() != 2 || digits != 2)
						return null;
					if (!timeTracker.setComponent(value, digits, padding))
						return null;
				}
				digits = 0;
				value = 0;
				padding = 0;
				break;

			case ' ':
				if (!dateTracker.seen() && !timeTracker.seen())
					return null;
				if (timeTracker.seen() && !timeTracker.isClosed()) {
					if (!timeTracker.setComponent(value, digits, padding))
						return null;
					timeTracker.close();
					timeDateElements.add(TimeDateElement.Time);
				}
				else if (dateTracker.seen() && !dateTracker.isClosed()) {
					if (dateTracker.components() != 2)
						return null;
					if (!(digits == 2 || (!yearInDateFirst && digits == 4)))
						return null;
					if (!fourDigitYear)
						fourDigitYear = digits == 4;
					dateTracker.setComponent(value, digits, padding);
					dateTracker.close();
					timeDateElements.add(TimeDateElement.Date);
				}
				timeDateElements.add(TimeDateElement.WhiteSpace);

				digits = 0;
				value = 0;
				padding = 0;
				break;

			default:
				if (!Character.isAlphabetic(ch))
					return null;

				if (timeTracker.seen()) {
					final String rest = trimmed.substring(i).toUpperCase(config.locale);
					for (final String s : localeInfo.getAMPMStrings()) {
						if (rest.startsWith(s)) {
							if (!timeTracker.isClosed()) {
								if (!timeTracker.setComponent(value, digits, padding))
									return null;
								digits = 0;
								timeTracker.close();
								timeDateElements.add(TimeDateElement.Time);
							}
							timeDateElements.add(TimeDateElement.AMPM);
							i += s.length() - 1;
							ampmDetected = true;
							// Eat the space after if it exists
							if (i + 1 < len && trimmed.charAt(i + 1) == ' ') {
								i++;
								timeDateElements.add(TimeDateElement.WhiteSpace);
							}
							break;
						}
					}

					if (!ampmDetected) {
						if (!timeTracker.isClosed()) {
							if (!timeTracker.setComponent(value, digits, padding))
								return null;
							digits = 0;
							timeTracker.close();
							timeDateElements.add(TimeDateElement.Time);
						}
						timeDateElements.add(TimeDateElement.TimeZone);
						if (ch == 'Z')
							timeZone = "X";
						else {
							final String currentTimeZone = trimmed.substring(i, len);
							if (!DateTimeParser.timeZones.contains(currentTimeZone))
								return null;
							timeZone = "z";
						}
					}
				}
				else {
					if (ch == 'T') {
						// ISO 8601
						if (!dateTracker.seen() || dateTracker.isClosed() || digits != 2 || dateSeparator != '-' || !fourDigitYear || !yearInDateFirst)
							return null;
						iso8601 = true;
						dateTracker.setComponent(value, digits, padding);
						dateTracker.close();
						timeDateElements.add(TimeDateElement.Date);
						timeDateElements.add(TimeDateElement.Indicator_8601);
						digits = 0;
						value = 0;
						padding = 0;
					}
					else
						return null;
				}
				break;
			}
		}

		if (!dateTracker.seen() && !timeTracker.seen())
			return null;

		if (dateTracker.seen() && !dateTracker.isClosed()) {
			// Need to close out the date
			if (yearInDateFirst) {
				if (digits != 1 && digits != 2)
					return null;
			}
			else {
				if (digits != 2 && digits != 4)
					return null;
			}
			fourDigitYear = digits == 4;
			if (dateTracker.components() != 2)
				return null;
			dateTracker.setComponent(value, digits, padding);
			digits = 0;
			padding = 0;
			timeDateElements.add(TimeDateElement.Date);
		}
		if (timeTracker.seen() && !timeTracker.isClosed()) {
			// Need to close out the time
			if ((timeTracker.components() != 3 && digits != 2) || (timeTracker.components() == 3 && (digits > 9)))
				return null;
			if (!timeTracker.setComponent(value, digits, padding))
				return null;
			digits = 0;
			timeDateElements.add(TimeDateElement.Time);
		}

		if (digits != 0)
			return null;

		if (iso8601 && timeTracker.components() == 0)
			return null;

		String timeAnswer = null;
		if (timeTracker.components() != 0) {
			if (timeTracker.getValue(1) > 59 || (timeTracker.components() >= 2 && timeTracker.getValue(2) > 59))
				return null;
			String hours = timeTracker.getPad(0) != 0 ? "pp" : "";
			if (ampmDetected) {
				if (timeTracker.getValue(0) > 12)
					return null;
				hours += hourLength == 1 ? "h" : "hh";
			}
			else {
				if (timeTracker.getValue(0) > 24)
					return null;
				if (timeTracker.getValue(0) == 24)
					hours += hourLength == 1 ? "k" : "kk";
				else
					hours += hourLength == 1 ? "H" : "HH";
			}
			timeAnswer = hours + ":mm";
			if (timeTracker.components() > 2)
				timeAnswer += ":ss";
			if (timeTracker.components() == 4)
				timeAnswer += "." + "SSSSSSSSS".substring(0, timeTracker.getDigit(3));
		}

		String dateAnswer = null;

		// Do we have any date components?
		if (dateTracker.components() != 0) {
			// If we don't have two date components then it is invalid
			if (dateTracker.components() == 1)
				return null;
			final boolean freePass = config.lenient && dateTracker.getValue(0) == 0 && dateTracker.getValue(1) == 0 && dateTracker.getValue(2) == 0;
			if ((!freePass && dateTracker.getValue(1) == 0) || dateTracker.getValue(1) > 31)
				return null;
			if (yearInDateFirst) {
				if (iso8601 || dateTracker.getValue(2) > 12) {
					if (!plausibleDate(dateTracker, new int[] {2,1,0}))
						return null;
					dateAnswer = Utils.repeat('y', dateTracker.getDigit(0)) + dateSeparator + Utils.repeat('M', dateTracker.getDigit(1)) + dateSeparator + Utils.repeat('d', dateTracker.getDigit(2));
				}
				else if (dateTracker.getValue(1) > 12) {
					if (!plausibleDate(dateTracker, new int[] {1,2,0}))
						return null;
					dateAnswer = Utils.repeat('y', dateTracker.getDigit(0)) + dateSeparator + Utils.repeat('d', dateTracker.getDigit(1)) + dateSeparator + Utils.repeat('M', dateTracker.getDigit(2));
				}
				else
					dateAnswer = dateFormat(dateTracker, dateSeparator, resolutionMode, true, true);
			}
			else {
				if (fourDigitYear) {
					// Year is the last field - attempt to determine which is the month
					if (dateTracker.getValue(0) > 12) {
						if (!plausibleDate(dateTracker, new int[] {0,1,2}))
							return null;
						dateAnswer = "dd" + dateSeparator + Utils.repeat('M', dateTracker.getDigit(1)) + dateSeparator + "yyyy";
					}
					else if (dateTracker.getValue(1) > 12) {
						if (!plausibleDate(dateTracker, new int[] {1,0,2}))
							return null;
						dateAnswer = Utils.repeat('M', dateTracker.getDigit(0)) + dateSeparator + "dd" + dateSeparator + "yyyy";
					}
					else
						dateAnswer = dateFormat(dateTracker, dateSeparator, resolutionMode, true, false);
				} else {
					// If the first group of digits is of length 1, then it is either d/MM/yy or M/dd/yy
					if (dateTracker.getDigit(0) == 1) {
						if (!freePass && dateTracker.getValue(0) == 0)
							return null;
						if (dateTracker.getValue(1) > 12) {
							if (!plausibleDate(dateTracker, new int[] {1,0,2}))
								return null;
							dateAnswer = Utils.repeat('M', dateTracker.getDigit(0)) + dateSeparator + "dd" + dateSeparator + "yy";
						}
						else
							dateAnswer = dateFormat(dateTracker, dateSeparator, resolutionMode, true, false);
					}
					// If year is the first field - then assume yy/MM/dd
					else if (dateTracker.getValue(0) > 31)
						dateAnswer = "yy" + dateSeparator + Utils.repeat('M', dateTracker.getDigit(1)) + dateSeparator + Utils.repeat('d', dateTracker.getDigit(2));
					else if (dateTracker.getValue(2) > 31) {
						// Year is the last field - attempt to determine which is the month
						if (dateTracker.getValue(0) > 12) {
							if (!plausibleDate(dateTracker, new int[] {0,1,2}))
								return null;
							dateAnswer = "dd" + dateSeparator + Utils.repeat('M', dateTracker.getDigit(1)) + dateSeparator + "yy";
						}
						else if (dateTracker.getValue(1) > 12) {
							if (!plausibleDate(dateTracker, new int[] {1,0,2}))
								return null;
							dateAnswer = Utils.repeat('M', dateTracker.getDigit(0)) + dateSeparator + "dd" + dateSeparator + "yy";
						}
						else
							dateAnswer = dateFormat(dateTracker, dateSeparator, resolutionMode, true, false);
					} else if (dateTracker.getValue(1) > 12) {
						if (!plausibleDate(dateTracker, new int[] {1,0,2}))
							return null;
						dateAnswer = Utils.repeat('M', dateTracker.getDigit(0)) + dateSeparator + "dd" + dateSeparator + "yy";
					} else if (dateTracker.getValue(0) > 12 && dateTracker.getValue(2) > 12) {
						dateAnswer = "??" + dateSeparator + Utils.repeat('M', dateTracker.getDigit(1)) + dateSeparator + "??";
					} else
						dateAnswer = dateFormat(dateTracker, dateSeparator, resolutionMode, false, false);
				}
			}
		}

		final StringBuilder ret = new StringBuilder();
		for (final TimeDateElement elt : timeDateElements) {
			switch (elt) {
			case Time:
				ret.append(timeAnswer);
				break;
			case Date:
				ret.append(dateAnswer);
				break;
			case WhiteSpace:
				ret.append(' ');
				break;
			case Indicator_8601:
				ret.append("'T'");
				break;
			case TimeZone:
				ret.append(timeZone);
				break;
			case AMPM:
				ret.append('a');
				break;
			}
		}

		return ret.toString();
	}

	/**
	 * This is our last attempt to construct a date pattern from the input.
	 *
	 * We use a brute force approach to repeatedly remove 'known' good patterns until we end up with a fully qualified pattern.
	 * This technique has the advantage that it is relatively forgiving of input with strange characters used to separate the components.
	 * For example, this will recognize input like the following:
	 * 	2017-10-12 16:45:30,403 or 2015-12-03:16:03:50 or 01APR2019
	 *
	 * @param trimmed The input we are scouring for a date/datetime/time
	 * @param matcher The previously computed matcher which provides both the compressed form of the input as well as a component count
	 * @param resolutionMode When we have ambiguity - should we prefer to conclude day first, month first, auto (based on locale) or unspecified.
	 * @return a DateTimeFormatter pattern.
	 */
	private String passThree(final String trimmed, final SimpleDateMatcher matcher, final DateResolutionMode resolutionMode) {
		String compressed = matcher.getCompressed();
		int components = matcher.getComponentCount();
		final boolean ampm = compressed.endsWith("P");

		// If there is an AM/PM indicator - then make it so.
		if (ampm)
			compressed = compressed.substring(0, compressed.length() - 1) + 'a';

		if (components > 6) {
			// Do we have a timezone offset?
			boolean positive;
			if ((positive = compressed.endsWith(" d{4}")) || compressed.endsWith(" -d{4}")) {
				final int offset = compressed.lastIndexOf(positive ? " d{4}" : " -d{4}");
				compressed = compressed.substring(0, offset) + " xx";
				components--;
			}

			// Do we have some milliseconds?
			if (components > 6 && compressed.indexOf("d{3}") != -1) {
				compressed = Utils.replaceFirst(compressed, "d{3}", "SSS");
				components--;
			}

			if (components > 6)
				return null;
		}

		boolean timeFound = false;
		if (components >= 3 && compressed.indexOf(TIME_ONLY_HHMMSS) != -1) {
			compressed = Utils.replaceFirst(compressed, TIME_ONLY_HHMMSS, ampm ? "hh:mm:ss" : "HH:mm:ss");
			components -= 3;
			timeFound = true;
		}

		if (!timeFound && components >= 3 && compressed.indexOf(TIME_ONLY_HMMSS) != -1) {
			compressed = Utils.replaceFirst(compressed, TIME_ONLY_HMMSS, ampm ? "h:mm:ss" : "H:mm:ss");
			components -= 3;
			timeFound = true;
		}

		// Happy to strip off a trailing time but only if there is something meaningful to further process
		if (!timeFound && components > 3 && matchAtEnd(compressed, TIME_ONLY_HHMM)) {
			compressed = Utils.replaceFirst(compressed, TIME_ONLY_HHMM, ampm ? "hh:mm" : "HH:mm");
			components -= 2;
			timeFound = true;
		}

		// Happy to strip off a trailing time but only if there is something meaningful to further process
		if (!timeFound && components > 3 && matchAtEnd(compressed, TIME_ONLY_PPHMM)) {
			compressed = Utils.replaceFirst(compressed, TIME_ONLY_PPHMM, ampm ? " pph:mm" : " ppH:mm");
			components -= 2;
			timeFound = true;
		}

		// Happy to strip off a trailing time but only if there is something meaningful to further process
		if (!timeFound && components > 3 && matchAtEnd(compressed, TIME_ONLY_HMM)) {
			compressed = Utils.replaceFirst(compressed, TIME_ONLY_HMM, ampm ? "h:mm" : "H:mm");
			components -= 2;
			timeFound = true;
		}

		if (components > 3)
			return null;

		if (components == 2) {
			// We only support MM/yyyy, MM-yyyy, yyyy/MM, and yyyy-MM (and their corresponding single month variants)
			final int len = compressed.length();
			if (len != 9 && len != 6)
				return null;
			final int year = compressed.indexOf("d{4}");
			if (year == -1)
				return null;

			final char separator = compressed.charAt(year == 0 ? 4 : year - 1);
			if (separator != '/' && separator != '-')
				return null;

			if (year == 0)
				compressed = "yyyy" + separator + (len == 9 ? "MM" : "M");
			else if (year == 2)
				compressed = "M" + separator + "yyyy";
			else if (year == 5)
				compressed = "MM" + separator + "yyyy";
			else
				return null;
		}
		else {
			int alreadyResolved = 0;
			final int yearIndex = compressed.indexOf("d{4}");
			if (yearIndex != -1) {
				compressed = Utils.replaceFirst(compressed, "d{4}", "yyyy");
				components--;
				alreadyResolved++;
			}

			final int monthIndex = compressed.indexOf("MMM");
			if (monthIndex != -1) {
				components--;
				alreadyResolved++;
			}

			// At this point we need at most two unresolved components
			if (alreadyResolved == 0 || components + alreadyResolved != 3)
				return null;

			if (components == 1) {
				if (compressed.indexOf("d{2}") != -1)
					compressed = Utils.replaceFirst(compressed, "d{2}", "dd");
				else if (compressed.indexOf("  d") != -1)
					compressed = Utils.replaceFirst(compressed, "  d", " ppd");
				else if (compressed.indexOf('d') != -1)
					;
				else
					return null;
			}
			else {
				// So we are looking for d{2}/d{2} or d{2}/d or d/{d2} or d/d
				final int firstDigit = compressed.indexOf('d');
				if (firstDigit == -1)
					return null;

				final int firstTwoDigit = compressed.indexOf("d{2}");
				String firstMatch;
				int secondStart;
				int firstLength;
				if (firstDigit == firstTwoDigit) {
					firstMatch = "d{2}";
					firstLength = 2;
					secondStart = firstDigit + 4;
				}
				else {
					firstMatch = "d";
					firstLength = 1;
					secondStart = firstDigit + 1;
					// Check to see we are not looking at d{X} where X is other than 2
					if (compressed.charAt(secondStart) == '{')
						return null;
				}

				final int secondDigit = compressed.indexOf('d', secondStart);
				if (secondDigit == -1)
					return null;

				final int secondTwoDigit = compressed.indexOf("d{2}", secondStart);
				String secondMatch;
				int secondLength;
				if (secondDigit == secondTwoDigit) {
					secondMatch = "d{2}";
					secondLength = 2;
				}
				else {
					secondMatch = "d";
					secondLength = 1;
				}

				if (monthIndex != -1)
					compressed = Utils.replaceFirst(Utils.replaceFirst(compressed, firstMatch, Utils.repeat('d', firstLength)), secondMatch, Utils.repeat('y', secondLength));
				else
					if (firstDigit > yearIndex)
						compressed = Utils.replaceFirst(Utils.replaceFirst(compressed, firstMatch, Utils.repeat('M', firstLength)), secondMatch, Utils.repeat('d', secondLength));
					else {
						final int firstValue = Utils.getValue(trimmed, firstDigit, 2, 2);
						final int secondValue = Utils.getValue(trimmed, firstLength == 2 ? secondDigit - 2 : secondDigit, secondLength, secondLength);

						if (firstValue > 31 || secondValue > 31 || (firstValue > 12 && secondValue > 12))
							return null;

						if (firstValue > 12) {
							// Day then Month
							compressed = Utils.replaceFirst(Utils.replaceLast(compressed, secondMatch, Utils.repeat('M', secondLength)), firstMatch, Utils.repeat('d', firstLength));
						}
						else if (secondValue > 12) {
							// Month then Day
							compressed = Utils.replaceFirst(Utils.replaceFirst(compressed, firstMatch, Utils.repeat('M', firstLength)), secondMatch, Utils.repeat('d', secondLength));
						}
						else
							if (resolutionMode == DateResolutionMode.DayFirst)
								compressed = Utils.replaceFirst(Utils.replaceLast(compressed, secondMatch, Utils.repeat('M', secondLength)), firstMatch, Utils.repeat('d', firstLength));
							else
								compressed = Utils.replaceFirst(Utils.replaceFirst(compressed, firstMatch, Utils.repeat('M', firstLength)), secondMatch, Utils.repeat('d', secondLength));
					}
			}
		}

		// Quotes are special in format strings and need to be doubled to protect them
		if (compressed.indexOf('\'') != -1)
			compressed = compressed.replaceAll("'", "''");

		// So we think we have nailed it - but it only counts if it happily passes a validity check
		final DateTimeParserResult dtpResult = DateTimeParserResult.asResult(compressed, resolutionMode, config);

		if (dtpResult == null || !dtpResult.isValid(trimmed))
			return null;

		// Add a relatively naive check to see if we have any characters that make it look unlikely this is an actual date.
		for (int i = 0; i < compressed.length(); i++) {
			char ch = compressed.charAt(i);
			boolean possiblePatternCharacter = (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z');
			if (possiblePatternCharacter &&
					ch != 'E' && ch != 'H' && ch != 'M' && ch != 'S' &&
					ch != 'a' && ch != 'd' && ch != 'h' && ch != 'm' && ch != 'p' && ch != 's' & ch != 'x' && ch != 'y' && ch != 'z')
				return null;
		}

		// So before we declare ultimate success - check that Java is happy with our conclusion
		try {
			new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(compressed).toFormatter(config.locale);
		}
		catch (IllegalArgumentException e) {
			return null;
		}

		return compressed;
	}

	private boolean matchAtEnd(final String input, final String toMatch) {
		return input.endsWith(toMatch) || input.endsWith(toMatch + 'a') || input.endsWith(toMatch + " a");
	}

	public DateTimeParser apply(final String input) {
	    train(input);
	    return this;
	}

	/**
	 * Merge a DateTimeParser with another DateTimeParser.
	 * Note: You cannot merge unless the Configurations are equal.
	 * @param other The other DateTimeParser to be merged
	 * @return A merged DateTimeParser.
	 * @throws FTAMergeException If the merge is impossible.
	 */
	public DateTimeParser merge(final DateTimeParser other) throws FTAMergeException {
		if (this == other)
			throw new FTAMergeException("Cannot merge with myself!");

		if (!this.config.equals(other.config))
			throw new FTAMergeException("Cannot merge Parsers with differing configurations!");
		state.merge(other.state);
	    return this;
	}

	/**
	 * Serialize a DateTimeParser - commonly used in concert with {@link #deserialize(String)} and {@link #merge(DateTimeParser)}
	 * to merge DateTimeParsers run on separate shards into a single DateTimeParser.
	 * @return A Serialized version of this DateTimeParser which can be hydrated via deserialize().
	 * @throws FTAMergeException When we fail to serialize the parser.
	 */
	public String serialize() throws FTAMergeException {
		final ObjectMapper mapper = new ObjectMapper();

		final DateTimeParserWrapper wrapper = new DateTimeParserWrapper(config, state);
		try {
			return mapper.writeValueAsString(mapper.convertValue(wrapper, JsonNode.class));
		} catch (IOException e) {
			throw new FTAMergeException("Cannot output JSON for the Parser", e);
		}
	}

	/**
	 * Create a new DateTimeParser from a serialized representation - used in concert with {@link #serialize()} and {@link #merge(DateTimeParser)}
	 * to merge DateTimeParsers run on separate shards into a single DateTimeParser.
	 * @param serialized The serialized form of a DateTimeParser.
	 * @return A new DateTimeParser which can be merged with another DateTimeParser to product a single result.
	 * @throws FTAMergeException When we fail to de-serialize the provided serialized form.
	 */
	public static DateTimeParser deserialize(final String serialized) throws FTAMergeException {
		try {
			final ObjectMapper mapper = new ObjectMapper();
			final DateTimeParserWrapper wrapper = mapper.readValue(serialized, DateTimeParserWrapper.class);
			final DateTimeParser ret = new DateTimeParser();

			ret.config = wrapper.config;
			ret.state = wrapper.state;

			return ret;
		} catch (JsonProcessingException e) {
			throw new FTAMergeException("Issue deserializing supplied JSON.", e);
		}
	}
}

