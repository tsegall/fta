/*
 * Copyright 2017-2025 Tim Segall
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
import java.nio.charset.StandardCharsets;
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
	protected boolean semanticTypesPre;
	// Used to pass in a list of known Semantic Types - e.g. User-stated, or previously identified in some manner
	protected String knownTypes;
	protected boolean noAnalysis;
	protected boolean noSemanticTypes;
	protected boolean noDistributions;
	protected boolean noQuantiles;
	protected boolean noStatistics;
	protected boolean noNullTextAsNull;
	protected String outputFormat = "json";
	protected boolean output;
	protected boolean formatDetection;
	private long recordsToProcess = -1;
	protected int detectWindow = -1;
	private String faker;
	private Locale locale;
	protected int maxCardinality = -1;
	protected int maxInputLength = -1;
	protected int maxOutlierCardinality = -1;
	protected int maxShapes = -1;
	protected int topBottomK = -1;
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
	protected int testmerge;
	protected String trace;
	protected int trailer;
	protected int validate;
	protected int verbose;
	protected boolean withBOM;
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
		this.knownTypes = other.knownTypes;
		this.noAnalysis = other.noAnalysis;
		this.noSemanticTypes = other.noSemanticTypes;
		this.noDistributions = other.noDistributions;
		this.noStatistics = other.noStatistics;
		this.noNullTextAsNull = other.noNullTextAsNull;
		this.output = other.output;
		this.outputFormat = other.outputFormat;
		this.formatDetection = other.formatDetection;
		this.recordsToProcess = other.recordsToProcess;
		this.detectWindow = other.detectWindow;
		this.faker = other.faker;
		this.locale = other.locale;
		this.maxCardinality = other.maxCardinality;
		this.maxInputLength = other.maxInputLength;
		this.maxOutlierCardinality = other.maxOutlierCardinality;
		this.maxShapes = other.maxShapes;
		this.topBottomK = other.topBottomK;
		this.pluginThreshold = other.pluginThreshold;
		this.pretty = other.pretty;
		this.pluginDefinition = other.pluginDefinition;
		this.pluginName = other.pluginName;
		this.pluginMode = other.pluginMode;
		this.resolutionMode = other.resolutionMode;
		this.samples = other.samples;
		this.semanticTypesPre = other.semanticTypesPre;
		this.signature = other.signature;
		this.skip = other.skip;
		this.threshold = other.threshold;
		this.testmerge = other.testmerge;
		this.trace = other.trace;
		this.trailer = other.trailer;
		this.validate = other.validate;
		this.verbose = other.verbose;
		this.withBOM = other.withBOM;
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
		if (this.maxShapes != -1)
			analyzer.setMaxShapes(this.maxShapes);
		if (this.topBottomK != -1)
			analyzer.setTopBottomK(this.topBottomK);
		if (this.pluginThreshold != -1)
			analyzer.setPluginThreshold(this.pluginThreshold);
		if (this.locale != null)
			analyzer.setLocale(this.locale);
		if (this.noDistributions)
			analyzer.configure(TextAnalyzer.Feature.DISTRIBUTIONS, false);
		if (this.noQuantiles)
			analyzer.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		if (this.noStatistics)
			analyzer.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		if (this.noNullTextAsNull)
			analyzer.configure(TextAnalyzer.Feature.NULL_TEXT_AS_NULL, false);
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
					analyzer.getPlugins().registerPlugins(new StringReader(this.semanticTypes), analyzer.getConfig(), semanticTypesPre);
				else {
					if(!Files.isRegularFile(Paths.get(this.semanticTypes))) {
						System.err.println("ERROR: Failed to read Semantic Types file: " + this.semanticTypes);
						System.exit(1);
					}
					try (FileReader logicalTypes = new FileReader(this.semanticTypes, StandardCharsets.UTF_8)) {
						analyzer.getPlugins().registerPlugins(logicalTypes, analyzer.getConfig(), semanticTypesPre);
					}
				}
			} catch (SecurityException | FTAPluginException e) {
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
				if (!locale.toLanguageTag().equals(tag))
					throw new IllegalArgumentException(String.format("Language tag '%s' not known - using '%s'?", tag, locale.toLanguageTag()));
			}
			else if ("--format".equals(args[idx]))
				outputFormat = args[++idx];
			else if ("--knownTypes".equals(args[idx]))
				knownTypes = nextStringArg(args, idx++);
			else if ("--maxInputLength".equals(args[idx]))
				maxInputLength = nextIntegerArg(args, idx++);
			else if ("--maxOutlierCardinality".equals(args[idx]))
				maxOutlierCardinality = nextIntegerArg(args, idx++);
			else if ("--maxShapes".equals(args[idx]))
				maxShapes = nextIntegerArg(args, idx++);
			else if ("--noAnalysis".equals(args[idx]))
				noAnalysis = true;
			else if ("--noPretty".equals(args[idx]))
				pretty = false;
			else if ("--noDistributions".equals(args[idx]))
				noDistributions = true;
			else if ("--noQuantiles".equals(args[idx]))
				noQuantiles = true;
			else if ("--noSemanticTypes".equals(args[idx]))
				noSemanticTypes = true;
			else if ("--noStatistics".equals(args[idx]))
				noStatistics = true;
			else if ("--noNullTextAsNull".equals(args[idx]))
				noNullTextAsNull = true;
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
				else
					throw new IllegalArgumentException(String.format("Unrecognized argument: '%s', expected Dayfirst or MonthFirst or Auto or None", mode));
			}
			else if ("--samples".equals(args[idx]))
				samples = true;
			else if ("--semanticType".equals(args[idx]))
				semanticTypes = nextStringArg(args, idx++);
			else if ("--semanticTypesPre".equals(args[idx]))
				semanticTypesPre = true;
			else if ("--signature".equals(args[idx]))
				signature = true;
			else if ("--skip".equals(args[idx]))
				skip = nextIntegerArg(args, idx++);
			else if ("--testMerge".equals(args[idx]))
				testmerge = nextIntegerArg(args, idx++);
			else if ("--threshold".equals(args[idx]))
				threshold = nextIntegerArg(args, idx++);
			else if ("--topBottomK".equals(args[idx]))
				topBottomK = nextIntegerArg(args, idx++);
			else if ("--trace".equals(args[idx]))
				trace = nextStringArg(args, idx++);
			else if ("--trailer".equals(args[idx]))
				trailer = nextIntegerArg(args, idx++);
			else if ("--validate".equals(args[idx]))
				validate = nextIntegerArg(args, idx++);
			else if ("--verbose".equals(args[idx]))
				verbose++;
			else if ("--version".equals(args[idx]))
				unprocessed.add(args[idx]);
			else if ("--withBOM".equals(args[idx]))
				withBOM = true;
			else {
				unprocessed.add(args[idx]);
				throw new IllegalArgumentException(String.format("Unrecognized option: '%s', use --help", args[idx]));
			}
			idx++;
		}

		while (idx < args.length)
			unprocessed.add(args[idx++]);

		return unprocessed.isEmpty() ? null : unprocessed.stream().toArray(String[] ::new);
	}

	public String getFaker() {
		return faker;
	}

	public Locale getLocale() {
		return locale;
	}

	public long getRecordsToProcess() {
		return recordsToProcess;
	}
}
