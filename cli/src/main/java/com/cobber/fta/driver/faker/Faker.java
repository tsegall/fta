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
package com.cobber.fta.driver.faker;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Random;

import com.cobber.fta.LogicalType;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.LogicalTypeRegExp;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.driver.DriverOptions;
import com.cobber.fta.driver.DriverUtils;

public class Faker {
	private DriverOptions options;
	private PrintStream output;
	private PrintStream error;

	public Faker(final DriverOptions options, final PrintStream output, final PrintStream error) {
		this.options = options;
		this.output = output;
		this.error = error;
	}

	public void fake() {
		final TextAnalyzer analyzer = DriverUtils.getDefaultAnalysis(options.locale);
		final Random random = new Random(31415926);
		final long outputRecords = options.recordsToProcess == -1 ? 20 : options.recordsToProcess;
		final Collection<LogicalType> registered = analyzer.getPlugins().getRegisteredLogicalTypes();
		String[] pluginDefinitions = options.faker.split(",");
		LogicalType[] logicals = new LogicalType[pluginDefinitions.length];
		FakerParameters[] controls = new FakerParameters[pluginDefinitions.length];
		String[] pluginNames = new String[pluginDefinitions.length];

		for (int i = 0; i < pluginDefinitions.length; i++) {
			int controlIndex = pluginDefinitions[i].indexOf('[');
			if (controlIndex == -1) {
				error.printf("ERROR: Failed to retrieve control for '%s', use --help%n", pluginDefinitions[i]);
				System.exit(1);
			}
			controls[i] = new FakerParameters(pluginDefinitions[i].substring(controlIndex));
			pluginNames[i] = pluginDefinitions[i].substring(0, controlIndex);

			for (final LogicalType logical : registered)
				if (logical.getQualifier().equals(controls[i].type)) {
					logicals[i] = logical;
					break;
				}

			// If we did not find the Semantic Type it must be one of the Base Types
			if (logicals[i] == null) {
				String baseType = controls[i].type;
				if (!"DOUBLE".equals(baseType) && !"LONG".equals(baseType) && !"LOCALDATE".equals(baseType) && !"LOCALDATETIME".equals(baseType) && !"ENUM".equals(baseType)) {
					error.printf("ERROR: Unknown type '%s', use --help%n", baseType);
					System.exit(1);
				}
				final PluginDefinition plugin = new PluginDefinition(controls[i].type, controls[i].clazz);
				try {
					logicals[i] = LogicalTypeFactory.newInstance(plugin, analyzer.getConfig());
					((FakerLT)logicals[i]).setControl(controls[i]);
				} catch (FTAPluginException e) {
					error.printf("ERROR: Failed to locate plugin named '%s', use --help%n", pluginDefinitions[i]);
					System.exit(1);
				}
			}
		}

		StringBuilder line = new StringBuilder();

		// Build the Header
		for (int i = 0; i < logicals.length; i++) {
			if (i != 0)
				line.append(',');
			line.append(quoteIfNeeded(pluginNames[i]));
			if (logicals[i] instanceof LogicalTypeRegExp && !((LogicalTypeRegExp)logicals[i]).isRegExpComplete())
				error.printf("ERROR: Logical Type (%s) does implement LTRandom interface - however samples may not be useful.%n", logicals[i].getQualifier());
		}
		output.println(line);

		for (long l = 0; l < outputRecords; l++) {
			line.setLength(0);
			for (int i = 0; i < logicals.length; i++) {
				if (i != 0)
					line.append(',');
				if (controls[i].nullPercent != 0 && random.nextDouble() <= controls[i].nullPercent)
					;		// Don't output anything which generates a null
				else if (controls[i].blankPercent != 0 && random.nextDouble() <= controls[i].blankPercent) {
					StringBuilder blank = new StringBuilder();
					blank.append('"');
					int blankLength = controls[i].blankLength == -1 ? random.nextInt(6) : controls[i].blankLength;
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
