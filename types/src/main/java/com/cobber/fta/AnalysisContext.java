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

import java.text.Collator;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import com.cobber.fta.dates.DateTimeParser;
import com.cobber.fta.token.TokenStreams;

/**
 * Shared mutable state for the analysis pipeline. Populated by {@link TextAnalyzer#initialize()}
 * and passed to helper classes ({@link TypeTracker}, {@link TypeDeterminer}, {@link ResultFinalizer},
 * {@link AnalyzerSerializer}) so they do not need to reach back into {@link TextAnalyzer}.
 */
class AnalysisContext {
	/** The accumulated profiling data for the stream being analyzed. */
	Facts facts;

	/** Configuration for this analysis (cardinality limit, thresholds, feature flags, etc.). */
	AnalysisConfig analysisConfig;

	/** Stream metadata: name, DateResolutionMode, composite name, header. */
	AnalyzerContext analyzerContext;

	/** Registry of known base-type patterns (long, double, date, etc.). */
	KnownTypes knownTypes;

	/** Locale-specific keywords (boolean representations, etc.). */
	Keywords keywords;

	/** Manager for all registered semantic-type plugins. */
	Plugins plugins;

	/** Date/time format detector and parser. */
	DateTimeParser dateTimeParser;

	/** Token-stream tracker for shape/pattern analysis. */
	TokenStreams tokenStreams;

	/** Locale-specific numeric metadata (grouping/decimal separators, sign conventions). */
	NumericInfo ni;

	/** Locale-aware formatter for long values. */
	NumberFormat longFormatter;

	/** Locale-aware formatter for double values. */
	NumberFormat doubleFormatter;

	/** Outliers accumulated during the detect window, collapsed at type-determination time. */
	FiniteMap outliersSmashed;

	/** Raw sample strings from the detect window. */
	List<String> raw;

	/** The active locale for this analysis. */
	Locale locale;

	/** Locale-aware string comparator. */
	Collator collator;

	/** Locale-specific string representing {@code true}. */
	String localizedYes;

	/** Locale-specific string representing {@code false}. */
	String localizedNo;

	/** Number of samples used for reflection/re-analysis passes. */
	int reflectionSamples;

	/** Override threshold for plugin (semantic type) detection; -1 means use plugin defaults. */
	int pluginThreshold;

	/** Active infinite-set semantic type plugins (e.g. Email, URL, GUID). */
	List<LogicalTypeInfinite> infiniteTypes;

	/** Active finite-set semantic type plugins (e.g. Gender, Country, State). */
	List<LogicalTypeFinite> finiteTypes;

	/** Active regex-based semantic type plugins. */
	List<LogicalTypeRegExp> regExpTypes;

	/** Per-plugin candidate match counts for infinite-type plugins during the detect window. */
	int[] candidateCounts;

	/** Per-plugin candidate match counts for regex-type plugins during the detect window. */
	int[] candidateCountsRE;

	/** Whether to treat "NULL" (and similar strings) as null values. */
	boolean nullTextAsNull;

	/** Count of internal errors swallowed during training; surfaced in the result. */
	int internalErrors;

	/** Log a contextual debug message if debug level is &gt;= 2. */
	void ctxdebug(final String area, final String format, final Object... arguments) {
		if (analysisConfig.getDebug() >= 2) {
			final StringBuilder formatWithContext = new StringBuilder().append(area).append(" ({}, {}, {}) -- ").append(format);
			final Object[] newArguments = new Object[3 + arguments.length];
			int newIndex = 0;
			newArguments[newIndex++] = analyzerContext.getStreamName();
			newArguments[newIndex++] = analysisConfig.getTrainingMode();
			newArguments[newIndex++] = analyzerContext.isNested();
			for (final Object arg : arguments)
				newArguments[newIndex++] = arg;
			LoggerFactory.getLogger("com.cobber.fta").debug(formatWithContext.toString(), newArguments);
		}
	}
}
