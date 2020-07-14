/*
 * Copyright 2017-2020 Tim Segall
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

import com.cobber.fta.DateTimeParser.DateResolutionMode;

class DriverOptions {
	String charset = "UTF-8";
	boolean bulk = false;
	int col = -1;
	int debug = -1;
	String logicalTypes = null;
	boolean noAnalysis = false;
	boolean noLogicalTypes = false;
	boolean noStatistics = false;
	long recordsToAnalyze = -1;
	int detectWindow = -1;
	Locale locale = null;
	int maxCardinality = -1;
	int maxOutlierCardinality = -1;
	int pluginThreshold = -1;
	boolean pretty = false;
	boolean pluginDefinition = false;
	DateResolutionMode resolutionMode = DateResolutionMode.Auto;
	int threshold = -1;
	boolean validate = false;
	int verbose = 0;
	int xMaxCharsPerColumn = -1;
}