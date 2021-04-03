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
package com.cobber.fta.driver;

import java.util.Locale;

import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;

class DriverOptions {
	String charset = "UTF-8";
	boolean bulk;
	int col = -1;
	int debug = -1;
	String logicalTypes;
	boolean noAnalysis;
	boolean noLogicalTypes;
	boolean noStatistics;
	long recordsToAnalyze = -1;
	int detectWindow = -1;
	Locale locale;
	int maxCardinality = -1;
	int maxOutlierCardinality = -1;
	int pluginThreshold = -1;
	boolean pretty;
	boolean pluginDefinition;
	DateResolutionMode resolutionMode = DateResolutionMode.Auto;
	int threshold = -1;
	boolean validate;
	int verbose = 0;
	int xMaxCharsPerColumn = -1;
	String delimiter;
}
