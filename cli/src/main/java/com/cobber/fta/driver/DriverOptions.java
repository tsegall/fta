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
package com.cobber.fta.driver;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;

public class DriverOptions {
	protected boolean abbreviationPunctuation;
	protected String charset = "UTF-8";
	protected boolean bulk;
	protected int col = -1;
	protected int debug = -1;
	protected String semanticTypes;
	protected boolean json;
	// Used to pass in a list of known Semantic Types - e.g. User-stated, or previously identified in some manner
	protected String knownTypes;
	protected boolean legacyJSON;
	protected boolean noAnalysis;
	protected boolean noSemanticTypes;
	protected boolean noDistributions;
	protected boolean noStatistics;
	protected boolean noNullAsText;
	protected boolean output;
	protected boolean formatDetection;
	public long recordsToProcess = -1;
	protected int detectWindow = -1;
	public String faker;
	public Locale locale;
	protected int maxCardinality = -1;
	protected int maxInputLength = -1;
	protected int maxOutlierCardinality = -1;
	protected int pluginThreshold = -1;
	protected boolean pretty = true;
	protected boolean pluginDefinition;
	protected String pluginName;
	protected Boolean pluginMode;
	protected DateResolutionMode resolutionMode = DateResolutionMode.Auto;
	protected boolean samples;
	protected boolean signature;
	protected int skip;
	protected int threshold = -1;
	protected String trace;
	protected boolean validate;
	protected int verbose;
	protected int xMaxCharsPerColumn = -1;
	protected int xMaxColumns = 1024;
	protected String delimiter;

	public void apply(final TextAnalyzer analyzer) throws IOException {
		if (this.debug != -1)
			analyzer.setDebug(this.debug);
		if (this.detectWindow != -1)
			analyzer.setDetectWindow(this.detectWindow);
		if (this.maxCardinality != -1)
			analyzer.setMaxCardinality(this.maxCardinality);
		if (this.maxInputLength != -1)
			analyzer.setMaxInputLength(this.maxInputLength);
		if (this.maxOutlierCardinality != -1)
			analyzer.setMaxOutliers(this.maxOutlierCardinality);
		if (this.pluginThreshold != -1)
			analyzer.setPluginThreshold(this.pluginThreshold);
		if (this.locale != null)
			analyzer.setLocale(this.locale);
		if (this.legacyJSON)
			analyzer.configure(TextAnalyzer.Feature.LEGACY_JSON, true);
		if (this.noDistributions)
			analyzer.configure(TextAnalyzer.Feature.DISTRIBUTIONS, false);
		if (this.noStatistics)
			analyzer.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		if (!this.noNullAsText)
			analyzer.configure(TextAnalyzer.Feature.NULL_AS_TEXT, true);
		if (this.noSemanticTypes)
			analyzer.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);
		if (this.formatDetection)
			analyzer.configure(TextAnalyzer.Feature.FORMAT_DETECTION, true);
		if (this.abbreviationPunctuation)
			analyzer.configure(TextAnalyzer.Feature.NO_ABBREVIATION_PUNCTUATION, false);
		if (this.trace != null)
			analyzer.setTrace(trace);

		if (this.semanticTypes != null)
			try {
				// If the argument starts with a '[' assume it is an inline definition, if not assume it is a file
				if (this.semanticTypes.charAt(0) == '[')
					analyzer.getPlugins().registerPlugins(new StringReader(this.semanticTypes),
							analyzer.getStreamName(), analyzer.getConfig());
				else {
					if(!Files.isRegularFile(Paths.get(this.semanticTypes))) {
						System.err.println("ERROR: Failed to read Semantic Types file: " + this.semanticTypes);
						System.exit(1);
					}
					try (FileReader logicalTypes = new FileReader(this.semanticTypes)) {
						analyzer.getPlugins().registerPlugins(logicalTypes, analyzer.getStreamName(), analyzer.getConfig());
					}
				}
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
					| IllegalAccessException | IllegalArgumentException | InvocationTargetException | FTAPluginException e) {
				System.err.println("ERROR: Failed to register plugin: " + e.getMessage());
				System.exit(1);
			}
		if (this.threshold != -1)
			analyzer.setThreshold(this.threshold);
	}
}
