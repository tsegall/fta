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

/**
 * Capture how the Analysis is configured.  Attributes on the analysis are typically frozen once training has started.
 */
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
	protected int maxShapes = MAX_SHAPES_DEFAULT;

	private int threshold = DETECTION_THRESHOLD_DEFAULT;

	private int detectWindow = DETECT_WINDOW_DEFAULT;

	/** The maximum input length. */
	private int maxInputLength = MAX_INPUT_LENGTH_DEFAULT;

	/** The current Locale tag - null if not set. */
	private String localeTag;

	/** The current tracing options. */
	protected String traceOptions;

	/** Should we collect statistics (min, max, sum) as we parse the data stream. */
	private boolean collectStatistics = true;

	/** Internal-only debugging flag. */
	protected int debug;

	/** Should we enable Default Logical Type detection. */
	private boolean enableDefaultLogicalTypes = true;

	AnalysisConfig() {
	}

	public boolean isCollectStatistics() {
		return collectStatistics;
	}

	public void setCollectStatistics(boolean collectStatistics) {
		this.collectStatistics = collectStatistics;
	}

	public boolean isEnableDefaultLogicalTypes() {
		return enableDefaultLogicalTypes;
	}

	public void setEnableDefaultLogicalTypes(boolean enableDefaultLogicalTypes) {
		this.enableDefaultLogicalTypes = enableDefaultLogicalTypes;
	}

	public int getDetectWindow() {
		return detectWindow;
	}

	public int setDetectWindow(int detectWindow) {
		final int ret = this.detectWindow;
		this.detectWindow = detectWindow;
		return ret;
	}

	public int getMaxCardinality() {
		return maxCardinality;
	}

	public int setMaxCardinality(int maxCardinality) {
		final int ret = this.maxCardinality;
		this.maxCardinality = maxCardinality;
		return ret;
	}

	public int getMaxInputLength() {
		return maxInputLength;
	}

	public int setMaxInputLength(int maxInputLength) {
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

	public int setThreshold(int threshold) {
		final int ret = this.threshold;
		this.threshold = threshold;
		return ret;
	}

	public String getLocaleTag() {
		return localeTag;
	}

	public void setLocaleTag(String localeTag) {
		this.localeTag = localeTag;
	}
}
