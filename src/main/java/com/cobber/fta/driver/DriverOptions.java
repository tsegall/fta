/*
 * Copyright 2017-2018 Tim Segall
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
	int col = -1;
	int debug = -1;
	DateResolutionMode resolutionMode = DateResolutionMode.None;
	int threshold = -1;
	boolean noAnalysis = false;
	boolean noLogicalTypes = false;
	boolean noStatistics = false;
	long recordsToAnalyze = -1;
	String logicalTypes = null;
	int detectWindow = -1;
	Locale locale = null;
	int maxCardinality = -1;
	int maxOutlierCardinality = -1;
	int verbose = 0;
	String charset = "UTF-8";
	boolean validate = false;
	boolean pretty = false;
	int xMaxCharsPerColumn = -1;
}