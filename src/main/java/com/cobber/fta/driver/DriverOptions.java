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
	protected String charset = "UTF-8";
	protected boolean bulk;
	protected int col = -1;
	protected int debug = -1;
	protected String logicalTypes;
	protected boolean noAnalysis;
	protected boolean noLogicalTypes;
	protected boolean noStatistics;
	protected long recordsToProcess = -1;
	protected int detectWindow = -1;
	protected Locale locale;
	protected int maxCardinality = -1;
	protected int maxOutlierCardinality = -1;
	protected int pluginThreshold = -1;
	protected boolean pretty;
	protected boolean pluginDefinition;
	protected String pluginSamples;
	protected DateResolutionMode resolutionMode = DateResolutionMode.Auto;
	protected int threshold = -1;
	protected boolean validate;
	protected int verbose;
	protected int xMaxCharsPerColumn = -1;
	protected String delimiter;
}
