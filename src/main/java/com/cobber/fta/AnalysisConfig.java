/*
 * Copyright 2017-2021 Tim Segall
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

public class AnalysisConfig {
	/** The maximum Cardinality tracked. */
	public int maxCardinality;
	/** The maximum # of outliers tracked. */
	public int maxOutliers;
	/** The maximum number of shapes tracked. */
	public int maxShapes;
	/** Should we collect statistics (min, max, sum) as we parse the data stream. */
	public boolean collectStatistics;
	/** Internal-only debugging flag. */
	public int debug;

	AnalysisConfig(int maxCardinality, int maxOutliers, int maxShapes, boolean collectStatistics, int debug) {
		this.maxCardinality = maxCardinality;
		this.maxOutliers = maxOutliers;
		this.maxShapes = maxShapes;
		this.collectStatistics = collectStatistics;
		this.debug = debug;
	}
}
