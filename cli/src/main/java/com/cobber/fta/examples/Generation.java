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
package com.cobber.fta.examples;

import java.io.IOException;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.LogicalType;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.LogicalTypeRegExp;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAException;

/**
 * Simple class to demonstrate how to generate valid pseudo-random input based on a Semantic Types.
 */
public abstract class Generation {

	public static void main(final String[] args) throws IOException, FTAException {
		final String demos[] = { "EMAIL", "COORDINATE.LATITUDE_DECIMAL", "CITY" };

		for (final String s: demos) {
			final PluginDefinition pluginDefinition = PluginDefinition.findByName(s);
			final LogicalType logical = LogicalTypeFactory.newInstance(pluginDefinition, new AnalysisConfig());

			if (logical instanceof LogicalTypeRegExp && !((LogicalTypeRegExp)logical).isRegExpComplete())
				System.err.printf("Semantic Type (%s) does implement LTRandom interface - however samples may not be valid.", s);

			System.err.printf("%n*** Semantic Type: '%s' ***%n", s);
			for (int i = 0; i < 10; i++) {
				final String value = logical.nextRandom();
				System.err.println(value);
				if (!logical.isValid(value))
					System.err.println("Issue with SemanticType'" + logical.getDescription() + "', value: " + value + "\n");
			}
		}
	}
}
