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

	/** The maximum Cardinality tracked. */
	public int maxCardinality = MAX_CARDINALITY_DEFAULT;

	/** The maximum number of outliers tracked. */
	public int maxOutliers = MAX_OUTLIERS_DEFAULT;

	/** The maximum number of shapes tracked. */
	public int maxShapes = MAX_SHAPES_DEFAULT;

	public int threshold = DETECTION_THRESHOLD_DEFAULT;

	public int detectWindow = DETECT_WINDOW_DEFAULT;

	public int maxInputLength = MAX_INPUT_LENGTH_DEFAULT;

	public String localeTag;

	public String traceOptions;

	/** Should we collect statistics (min, max, sum) as we parse the data stream. */
	public boolean collectStatistics = true;

	/** Internal-only debugging flag. */
	public int debug;

	/** Should we enable Default Logical Type detection. */
	public boolean enableDefaultLogicalTypes = true;

	AnalysisConfig() {
	}
}
