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
package com.cobber.fta.driver.faker;

import java.io.PrintStream;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Random;

import com.cobber.fta.LogicalType;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.LogicalTypeRegExp;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.driver.DriverOptions;

public class Faker {
	private final DriverOptions options;
	private final PrintStream output;
	private final PrintStream error;

	public Faker(final DriverOptions options, final PrintStream output, final PrintStream error) {
		this.options = options;
		this.output = output;
		this.error = error;
	}

	private String[] quoteAwareSplit(final String input, final char splitOn) {
		final LinkedHashSet<String> result = new LinkedHashSet<>();
		boolean inQuotes = false;
		int start = 0;

		for (int i = 0; i < input.length(); i++) {
			if (input.charAt(i) == '"')
				inQuotes = !inQuotes;
			else if (!inQuotes && input.charAt(i) == splitOn) {
				result.add(input.substring(start, i));
				start = i + 1;
			}
		}

		result.add(input.substring(start, input.length()));

		return result.toArray(String[]::new);
	}

	public void fake() {
		final TextAnalyzer analyzer = TextAnalyzer.getDefaultAnalysis(options.locale);
		final Random random = new Random(31415926);
		final long outputRecords = options.recordsToProcess == -1 ? 20 : options.recordsToProcess;
		final Collection<LogicalType> registered = analyzer.getPlugins().getRegisteredSemanticTypes();
		final String[] pluginDefinitions = quoteAwareSplit(options.faker, ',');
		final LogicalType[] logicals = new LogicalType[pluginDefinitions.length];
		final FakerParameters[] parameters = new FakerParameters[pluginDefinitions.length];
		final String[] pluginNames = new String[pluginDefinitions.length];

		for (int i = 0; i < pluginDefinitions.length; i++) {
			final int parameterIndex = pluginDefinitions[i].indexOf('[');
			if (parameterIndex == -1) {
				error.printf("ERROR: Failed to retrieve parameters for '%s', missing open ('[')?, use --help%n", pluginDefinitions[i]);
				System.exit(1);
			}
			if (pluginDefinitions[i].charAt(pluginDefinitions[i].length() - 1) != ']') {
				error.printf("ERROR: Missing close (']') for parameter definition for '%s', use --help%n", pluginDefinitions[i]);
				System.exit(1);
			}
			parameters[i] = new FakerParameters(pluginDefinitions[i].substring(parameterIndex + 1, pluginDefinitions[i].length() - 1));
			pluginNames[i] = pluginDefinitions[i].substring(0, parameterIndex);

			for (final LogicalType logical : registered)
				if (logical.getSemanticType().equals(parameters[i].type)) {
					logicals[i] = logical;
					break;
				}

			// If we did not find the Semantic Type it must be one of the Base Types
			if (logicals[i] == null) {
				final String baseType = parameters[i].type;
				if (!"DOUBLE".equals(baseType) && !"LONG".equals(baseType) && !"LOCALDATE".equals(baseType) && !"LOCALTIME".equals(baseType) &&
						!"LOCALDATETIME".equals(baseType) && !"OFFSETDATETIME".equals(baseType) && !"BOOLEAN".equals(baseType) && !"ENUM".equals(baseType)) {
					error.printf("ERROR: Unknown type '%s', use --help%n", baseType);
					System.exit(1);
				}
				final PluginDefinition plugin = new PluginDefinition(parameters[i].type, parameters[i].clazz);
				try {
					logicals[i] = LogicalTypeFactory.newInstance(plugin, analyzer.getConfig());
					((FakerLT)logicals[i]).setControl(parameters[i]);
				} catch (FTAPluginException e) {
					error.printf("ERROR: Failed to locate plugin named '%s', use --help%n", pluginDefinitions[i]);
					System.exit(1);
				}
			}
		}

		final StringBuilder line = new StringBuilder();

		// Build the Header
		for (int i = 0; i < logicals.length; i++) {
			if (i != 0)
				line.append(',');
			line.append(quoteIfNeeded(pluginNames[i]));
			if (logicals[i] instanceof LogicalTypeRegExp && !((LogicalTypeRegExp)logicals[i]).isRegExpComplete())
				error.printf("ERROR: Semantic Type (%s) does implement LTRandom interface - however samples may not be useful.%n", logicals[i].getSemanticType());
		}
		output.println(line);

		for (long l = 0; l < outputRecords; l++) {
			line.setLength(0);
			for (int i = 0; i < logicals.length; i++) {
				if (i != 0)
					line.append(',');
				if (parameters[i].nullPercent != 0 && random.nextDouble() <= parameters[i].nullPercent)
					;		// Don't output anything which generates a null
				else if (parameters[i].blankPercent != 0 && random.nextDouble() <= parameters[i].blankPercent) {
					final StringBuilder blank = new StringBuilder();
					blank.append('"');
					final int blankLength = parameters[i].blankLength == -1 ? random.nextInt(6) : parameters[i].blankLength;
					for (int b = 0; b < blankLength; b++)
						blank.append(' ');
					blank.append('"');
					line.append(blank.toString());
				}
				else
					line.append(quoteIfNeeded(logicals[i].nextRandom()));
			}
			output.println(line);
		}
	}

	private String quoteIfNeeded(final String input) {
		if (input.indexOf(',') == -1 && input.indexOf(' ') == -1)
			return input;

		return "\"" + input + "\"";
	}
}
