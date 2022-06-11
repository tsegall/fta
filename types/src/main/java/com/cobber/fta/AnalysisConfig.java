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

import java.util.Locale;
import java.util.Objects;

import com.cobber.fta.TextAnalyzer.Feature;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * Capture how the Analysis is configured.  Attributes on the analysis are typically frozen once training has started.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AnalysisConfig {
	/** The default value for the maximum Cardinality tracked. */
	public static final int MAX_CARDINALITY_DEFAULT = 12_000;

	/** The default value for the maximum # of outliers tracked. */
	public static final int MAX_OUTLIERS_DEFAULT = 50;

	/** The default value for the maximum number of shapes tracked. */
	public static final int MAX_SHAPES_DEFAULT = 400;

	/** The default value for the detection threshold. */
	public static final int DETECTION_THRESHOLD_DEFAULT = 95;

	/** The default value for the number of samples to collect before making a type determination. */
	public static final int DETECT_WINDOW_DEFAULT = 20;

	/** The default value for the maximum length of input to process. */
	public static final int MAX_INPUT_LENGTH_DEFAULT = 4096;

	/** The minimum value for the maximum length of input to process. */
	public static final int MAX_INPUT_LENGTH_MINIMUM = 64;

	/** The maximum Cardinality tracked. */
	private int maxCardinality = MAX_CARDINALITY_DEFAULT;

	/** The maximum number of outliers tracked. */
	private int maxOutliers = MAX_OUTLIERS_DEFAULT;

	/** The maximum number of shapes tracked. */
	private int maxShapes = MAX_SHAPES_DEFAULT;

	private int threshold = DETECTION_THRESHOLD_DEFAULT;

	private int detectWindow = DETECT_WINDOW_DEFAULT;

	/** The maximum input length. */
	private int maxInputLength = MAX_INPUT_LENGTH_DEFAULT;

	/** The current Locale tag. */
	private String localeTag;

	/** The current tracing options. */
	private String traceOptions;

	/** Should we collect statistics (min, max, sum) as we parse the data stream. */
	private boolean collectStatistics = true;

	/** Internal-only debugging flag. */
	private int debug;

	/** Should we enable Default Logical Type detection. */
	private boolean enableDefaultLogicalTypes = true;

	/** Enable Numeric widening - i.e. if we see lots of integers then some doubles call it a double. */
	private boolean numericWidening = true;

	/** Should we attempt to do Stream format detection.  For example, HTML, XML, JSON, BASE64, ... */
	private boolean formatDetection = false;

	/** Should we attempt to qualify the size of the returned RexExp. */
	private boolean lengthQualifier = true;

	/** Should we use simple Month Abbreviations with no punctuation for example some locales have periods, e.g. Canada uses 'AUG.',
	 * similarly for the AM/PM string which are defined in Canada as A.M and P.M. */
	private boolean noAbbreviationPunctuation = true;

	public AnalysisConfig() {
		this(Locale.getDefault());
	}

	public AnalysisConfig(final Locale locale) {
		localeTag = locale == null ? Locale.getDefault().toLanguageTag() : locale.toLanguageTag();
	}

	public AnalysisConfig(final AnalysisConfig other) {
		this.maxCardinality = other.maxCardinality;
		this.maxOutliers = other.maxOutliers;
		this.maxShapes = other.maxShapes;
		this.threshold = other.threshold;
		this.detectWindow = other.detectWindow;
		this.maxInputLength = other.maxInputLength;
		this.localeTag = other.localeTag;
		this.traceOptions = other.traceOptions;
		this.collectStatistics = other.collectStatistics;
		this.debug = other.debug;
		this.enableDefaultLogicalTypes = other.enableDefaultLogicalTypes;
		this.numericWidening = other.numericWidening;
		this.formatDetection = other.formatDetection;
		this.lengthQualifier = other.lengthQualifier;
		this.noAbbreviationPunctuation = other.noAbbreviationPunctuation;
	}

	/**
	 * Method for changing state of an on/off feature for this TextAnalyzer.
	 * @param feature The feature to be set.
	 * @param state The new state of the feature.
	 */
	public void configure(final Feature feature, final boolean state) {
		switch (feature) {
		case COLLECT_STATISTICS:
			collectStatistics = state;
			break;
		case DEFAULT_LOGICAL_TYPES:
			enableDefaultLogicalTypes = state;
			break;
		case NUMERIC_WIDENING:
			numericWidening = state;
			break;
		case FORMAT_DETECTION:
			formatDetection = state;
			break;
		case LENGTH_QUALIFIER:
			lengthQualifier = state;
			break;
		case NO_ABBREVIATION_PUNCTUATION:
			noAbbreviationPunctuation = state;
			break;
		}
	}

	/**
	 * Method for checking whether given TextAnalyzer feature is enabled.
	 * @param feature The feature to be tested.
	 * @return Whether the identified feature is enabled.
	 */
	public boolean isEnabled(final Feature feature) {
		switch (feature) {
		case COLLECT_STATISTICS:
			return collectStatistics;
		case DEFAULT_LOGICAL_TYPES:
			return enableDefaultLogicalTypes;
		case NUMERIC_WIDENING:
			return numericWidening;
		case FORMAT_DETECTION:
			return formatDetection;
		case LENGTH_QUALIFIER:
			return lengthQualifier;
		case NO_ABBREVIATION_PUNCTUATION:
			return noAbbreviationPunctuation;
		}
		return false;
	}

	public int getDetectWindow() {
		return detectWindow;
	}

	public int setDetectWindow(final int detectWindow) {
		final int ret = this.detectWindow;
		this.detectWindow = detectWindow;
		return ret;
	}

	public int getMaxCardinality() {
		return maxCardinality;
	}

	public int setMaxCardinality(final int maxCardinality) {
		final int ret = this.maxCardinality;
		this.maxCardinality = maxCardinality;
		return ret;
	}

	public int getMaxInputLength() {
		return maxInputLength;
	}

	public int setMaxInputLength(final int maxInputLength) {
		final int ret = this.maxInputLength;
		this.maxInputLength = maxInputLength;
		return ret;
	}

	public int getMaxOutliers() {
		return maxOutliers;
	}

	public int setMaxOutliers(int maxOutliers) {
		final int ret = this.maxOutliers;
		this.maxOutliers = maxOutliers;
		return ret;
	}

	public int getThreshold() {
		return threshold;
	}

	public int setThreshold(final int threshold) {
		final int ret = this.threshold;
		this.threshold = threshold;
		return ret;
	}

	public String getLocaleTag() {
		return localeTag;
	}

	public void setLocaleTag(final String localeTag) {
		this.localeTag = localeTag;
	}

	public Locale getLocale() {
		return localeTag == null ? null : Locale.forLanguageTag(localeTag);
	}

	public void setLocale(final Locale locale) {
		localeTag = locale == null ? null : locale.toLanguageTag();
	}

	public AnalysisConfig withLocale(final Locale locale) {
		setLocale(locale);
		return this;
	}

	public int getMaxShapes() {
		return maxShapes;
	}

	public int setMaxShapes(final int maxShapes) {
		final int ret = this.maxShapes;
		this.maxShapes = maxShapes;
		return ret;
	}

	public String getTraceOptions() {
		return traceOptions;
	}

	public void setTraceOptions(final String traceOptions) {
		this.traceOptions = traceOptions;
	}

	public int getDebug() {
		return debug;
	}

	public void setDebug(final int debug) {
		this.debug = debug;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final AnalysisConfig other = (AnalysisConfig) obj;
		return detectWindow == other.detectWindow && enableDefaultLogicalTypes == other.enableDefaultLogicalTypes
				&& Objects.equals(localeTag, other.localeTag) && maxCardinality == other.maxCardinality
				&& maxInputLength == other.maxInputLength && maxOutliers == other.maxOutliers
				&& maxShapes == other.maxShapes && threshold == other.threshold && numericWidening == other.numericWidening;
	}
}
