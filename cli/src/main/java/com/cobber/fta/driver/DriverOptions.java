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
import java.util.ArrayList;
import java.util.List;
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
	protected int validate;
	protected int verbose;
	protected int xMaxCharsPerColumn = -1;
	protected int xMaxColumns = 1024;
	protected String delimiter;
	protected String quoteChar;

	public DriverOptions() {
	}

	public DriverOptions(DriverOptions other) {
		this.abbreviationPunctuation = other.abbreviationPunctuation;
		this.charset = other.charset;
		this.bulk = other.bulk;
		this.col = other.col;
		this.debug = other.debug;
		this.semanticTypes = other.semanticTypes;
		this.json = other.json;
		this.knownTypes = other.knownTypes;
		this.legacyJSON = other.legacyJSON;
		this.noAnalysis = other.noAnalysis;
		this.noSemanticTypes = other.noSemanticTypes;
		this.noDistributions = other.noDistributions;
		this.noStatistics = other.noStatistics;
		this.noNullAsText = other.noNullAsText;
		this.output = other.output;
		this.formatDetection = other.formatDetection;
		this.recordsToProcess = other.recordsToProcess;
		this.detectWindow = other.detectWindow;
		this.faker = other.faker;
		this.locale = other.locale;
		this.maxCardinality = other.maxCardinality;
		this.maxInputLength = other.maxInputLength;
		this.maxOutlierCardinality = other.maxOutlierCardinality;
		this.pluginThreshold = other.pluginThreshold;
		this.pretty = other.pretty;
		this.pluginDefinition = other.pluginDefinition;
		this.pluginName = other.pluginName;
		this.pluginMode = other.pluginMode;
		this.resolutionMode = other.resolutionMode;
		this.samples = other.samples;
		this.signature = other.signature;
		this.skip = other.skip;
		this.threshold = other.threshold;
		this.trace = other.trace;
		this.validate = other.validate;
		this.verbose = other.verbose;
		this.xMaxCharsPerColumn = other.xMaxCharsPerColumn;
		this.xMaxColumns = other.xMaxColumns;
		this.delimiter = other.delimiter;
		this.quoteChar = other.quoteChar;
	}

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

	public void addFromFile(final String filePath) throws IOException {
		addFromStringArray(Files.readString(Paths.get(filePath)).split("[ \n]"));
	}

	public String[] addFromStringArray(String[] args) {
		List<String> unprocessed = new ArrayList<>();
		int idx = 0;
		while (idx < args.length && args[idx].charAt(0) == '-') {
			if ("--abbreviationPunctuation".equals(args[idx]))
				abbreviationPunctuation = true;
			else if ("--bloomfilter".equals(args[idx])) {
				unprocessed.add(args[idx]);
				unprocessed.add(args[++idx]);
				unprocessed.add(args[++idx]);
			}
			else if ("--bulk".equals(args[idx]))
				bulk = true;
			else if ("--charset".equals(args[idx]))
				charset = args[++idx];
			else if ("--col".equals(args[idx]))
				col = Integer.valueOf(args[++idx]);
			else if ("--debug".equals(args[idx]))
				debug = Integer.valueOf(args[++idx]);
			else if ("--delimiter".equals(args[idx]))
				delimiter = args[++idx];
			else if ("--detectWindow".equals(args[idx]))
				detectWindow = Integer.valueOf(args[++idx]);
			else if ("--faker".equals(args[idx]))
				faker = args[++idx];
			else if ("--formatDetection".equals(args[idx]))
				formatDetection = true;
			else if ("--help".equals(args[idx])) {
				unprocessed.add(args[idx]);
			}
			else if ("--locale".equals(args[idx])) {
				String tag = args[++idx];
				locale = Locale.forLanguageTag(tag);
				if (!locale.toLanguageTag().equals(tag)) {
					System.err.printf("ERROR: Language tag '%s' not known - using '%s'?%n", tag, locale.toLanguageTag());
					System.exit(1);
				}
			}
			else if ("--json".equals(args[idx]))
				json = true;
			else if ("--knownTypes".equals(args[idx]))
				knownTypes = args[++idx];
			else if ("--legacyJSON".equals(args[idx]))
				legacyJSON = true;
			else if ("--maxInputLength".equals(args[idx]))
				maxInputLength = Integer.valueOf(args[++idx]);
			else if ("--maxOutlierCardinality".equals(args[idx]))
				maxOutlierCardinality = Integer.valueOf(args[++idx]);
			else if ("--normalize".equals(args[idx])) {
				unprocessed.add(args[idx]);
				unprocessed.add(args[++idx]);
			}
			else if ("--noAnalysis".equals(args[idx]))
				noAnalysis = true;
			else if ("--noPretty".equals(args[idx]))
				pretty = false;
			else if ("--noDistributions".equals(args[idx]))
				noDistributions = true;
			else if ("--noSemanticTypes".equals(args[idx]))
				noSemanticTypes = true;
			else if ("--noStatistics".equals(args[idx]))
				noStatistics = true;
			else if ("--noNullAsText".equals(args[idx]))
				noNullAsText = true;
			else if ("--output".equals(args[idx]))
				output = true;
			else if ("--pluginDefinition".equals(args[idx]))
				pluginDefinition = true;
			else if ("--pluginMode".equals(args[idx]))
				pluginMode = Boolean.valueOf(args[++idx]);
			else if ("--pluginName".equals(args[idx]))
				pluginName = args[++idx];
			else if ("--pluginThreshold".equals(args[idx]))
				pluginThreshold = Integer.valueOf(args[++idx]);
			else if ("--quoteChar".equals(args[idx]))
				quoteChar = args[++idx];
			else if ("--records".equals(args[idx]))
				recordsToProcess = Long.valueOf(args[++idx]);
			else if ("--replay".equals(args[idx])) {
				unprocessed.add(args[idx]);
				unprocessed.add(args[++idx]);
			}
			else if ("--resolutionMode".equals(args[idx])) {
				final String mode = args[++idx];
				if ("DayFirst".equals(mode))
					resolutionMode = DateResolutionMode.DayFirst;
				else if ("MonthFirst".equals(mode))
					resolutionMode = DateResolutionMode.MonthFirst;
				else if ("Auto".equals(mode))
					resolutionMode = DateResolutionMode.Auto;
				else if ("None".equals(mode))
					resolutionMode = DateResolutionMode.None;
				else {
					System.err.printf("ERROR: Unrecognized argument: '%s', expected Dayfirst or MonthFirst or Auto or None%n", mode);
					System.exit(1);
				}
			}
			else if ("--samples".equals(args[idx]))
				samples = true;
			else if ("--semanticType".equals(args[idx]))
				semanticTypes = args[++idx];
			else if ("--signature".equals(args[idx]))
				signature = true;
			else if ("--skip".equals(args[idx]))
				skip = Integer.valueOf(args[++idx]);
			else if ("--threshold".equals(args[idx]))
				threshold = Integer.valueOf(args[++idx]);
			else if ("--trace".equals(args[idx]))
				trace = args[++idx];
			else if ("--validate".equals(args[idx]))
				validate = Integer.valueOf(args[++idx]);
			else if ("--verbose".equals(args[idx]))
				verbose++;
			else if ("--version".equals(args[idx])) {
				unprocessed.add(args[idx]);
			}
			else if ("--xMaxCharsPerColumn".equals(args[idx]))
				xMaxCharsPerColumn = Integer.valueOf(args[++idx]);
			else if ("--xMaxColumns".equals(args[idx]))
				xMaxColumns = Integer.valueOf(args[++idx]);
			else {
				unprocessed.add(args[idx]);
				System.err.printf("ERROR: Unrecognized option: '%s', use --help%n", args[idx]);
				System.exit(1);
			}
			idx++;
		}

		while (idx < args.length)
			unprocessed.add(args[idx++]);

		return unprocessed.size() == 0 ? null : unprocessed.stream().toArray(String[] ::new);
	}

}
