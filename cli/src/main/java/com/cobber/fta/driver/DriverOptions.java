/*
 * Copyright 2017-2024 Tim Segall
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
	protected int trailer;
	protected int validate;
	protected int verbose;
	protected int xMaxCharsPerColumn = -1;
	protected int xMaxColumns = 1024;
	protected String delimiter;
	protected String quoteChar;

	public DriverOptions() {
	}

	public DriverOptions(final DriverOptions other) {
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
		this.trailer = other.trailer;
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
		final String args = Files.readString(Paths.get(filePath)).trim();
		if (args.isEmpty())
			return;
		addFromStringArray(args.split("[ \n]+"));
	}

	private int nextIntegerArg(final String[] args, final int index) {
		if (index + 1 >= args.length)
			throw new IllegalArgumentException("Missing mandatory argument for '" + args[index] + "' option");

		int ret;

		try {
			ret = Integer.parseInt(args[index + 1]);
		}
		catch (NumberFormatException e) {
			throw new IllegalArgumentException("Expecting integer argument for '" + args[index] + "' option", e);
		}

		return ret;
	}

	private String nextStringArg(final String[] args, final int index) {
		if (index + 1 >= args.length)
			throw new IllegalArgumentException("Missing mandatory argument for '" + args[index] + "' option");
		return args[index + 1];
	}

	public String[] addFromStringArray(final String[] args) {
		final List<String> unprocessed = new ArrayList<>();
		int idx = 0;

		try {
			while (idx < args.length && args[idx].charAt(0) == '-') {
				if ("--abbreviationPunctuation".equals(args[idx]))
					abbreviationPunctuation = true;
				else if ("--bulk".equals(args[idx]))
					bulk = true;
				else if ("--charset".equals(args[idx]))
					charset = nextStringArg(args, idx++);
				else if ("--col".equals(args[idx]))
					col = nextIntegerArg(args, idx++);
				else if ("--createBloomfilter".equals(args[idx])) {
					unprocessed.add(args[idx]);
					unprocessed.add(nextStringArg(args, idx++));
					unprocessed.add(nextStringArg(args, idx++));
				}
				else if ("--createNormalized".equals(args[idx])) {
					unprocessed.add(args[idx]);
					unprocessed.add(nextStringArg(args, idx++));
				}
				else if ("--createSemanticTypesMarkdown".equals(args[idx])) {
					unprocessed.add(args[idx]);
				}
				else if ("--debug".equals(args[idx]))
					debug = nextIntegerArg(args, idx++);
				else if ("--delimiter".equals(args[idx]))
					delimiter = nextStringArg(args, idx++);
				else if ("--detectWindow".equals(args[idx]))
					detectWindow = nextIntegerArg(args, idx++);
				else if ("--faker".equals(args[idx]))
					faker = nextStringArg(args, idx++);
				else if ("--formatDetection".equals(args[idx]))
					formatDetection = true;
				else if ("--help".equals(args[idx])) {
					unprocessed.add(args[idx]);
				}
				else if ("--locale".equals(args[idx])) {
					final String tag = nextStringArg(args, idx++);
					locale = Locale.forLanguageTag(tag);
					if (!locale.toLanguageTag().equals(tag)) {
						System.err.printf("ERROR: Language tag '%s' not known - using '%s'?%n", tag, locale.toLanguageTag());
						System.exit(1);
					}
				}
				else if ("--json".equals(args[idx]))
					json = true;
				else if ("--knownTypes".equals(args[idx]))
					knownTypes = nextStringArg(args, idx++);
				else if ("--legacyJSON".equals(args[idx]))
					legacyJSON = true;
				else if ("--maxInputLength".equals(args[idx]))
					maxInputLength = nextIntegerArg(args, idx++);
				else if ("--maxOutlierCardinality".equals(args[idx]))
					maxOutlierCardinality = nextIntegerArg(args, idx++);
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
					pluginName = nextStringArg(args, idx++);
				else if ("--pluginThreshold".equals(args[idx]))
					pluginThreshold = nextIntegerArg(args, idx++);
				else if ("--quoteChar".equals(args[idx]))
					quoteChar = nextStringArg(args, idx++);
				else if ("--records".equals(args[idx]))
					recordsToProcess = Long.parseLong(args[++idx]);
				else if ("--replay".equals(args[idx])) {
					unprocessed.add(args[idx]);
					unprocessed.add(nextStringArg(args, idx++));
				}
				else if ("--resolutionMode".equals(args[idx])) {
					final String mode = nextStringArg(args, idx++);
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
					semanticTypes = nextStringArg(args, idx++);
				else if ("--signature".equals(args[idx]))
					signature = true;
				else if ("--skip".equals(args[idx]))
					skip = nextIntegerArg(args, idx++);
				else if ("--threshold".equals(args[idx]))
					threshold = nextIntegerArg(args, idx++);
				else if ("--trace".equals(args[idx]))
					trace = nextStringArg(args, idx++);
				else if ("--trailer".equals(args[idx]))
					trailer = nextIntegerArg(args, idx++);
				else if ("--validate".equals(args[idx]))
					validate = nextIntegerArg(args, idx++);
				else if ("--verbose".equals(args[idx]))
					verbose++;
				else if ("--version".equals(args[idx])) {
					unprocessed.add(args[idx]);
				}
				else if ("--xMaxCharsPerColumn".equals(args[idx]))
					xMaxCharsPerColumn = nextIntegerArg(args, idx++);
				else if ("--xMaxColumns".equals(args[idx]))
					xMaxColumns = nextIntegerArg(args, idx++);
				else {
					unprocessed.add(args[idx]);
					System.err.printf("ERROR: Unrecognized option: '%s', use --help%n", args[idx]);
					System.exit(1);
				}
				idx++;
			}
		}
		catch (IllegalArgumentException e) {
			System.err.printf("ERROR: %s%n", e.getMessage());
			System.exit(1);
		}

		while (idx < args.length)
			unprocessed.add(args[idx++]);

		return unprocessed.isEmpty() ? null : unprocessed.stream().toArray(String[] ::new);
	}

}
