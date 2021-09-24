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
package com.cobber.fta.examples;

import java.io.IOException;
import java.util.Locale;

import com.cobber.fta.LTRandom;
import com.cobber.fta.LogicalType;
import com.cobber.fta.LogicalTypeCode;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAException;

/**
 * Simple class to demonstrate how to generate valid pseudo-random input based on a Semantic Types.
 */
public abstract class Generation {

	public static void main(final String[] args) throws IOException, FTAException {
		final PluginDefinition pluginDefinition = PluginDefinition.findByQualifier("EMAIL");
		final LogicalType logicalType = LogicalTypeFactory.newInstance(pluginDefinition, Locale.getDefault());

		if (!LTRandom.class.isAssignableFrom(logicalType.getClass())) {
			System.err.println("Logical Type must implement LTRandom interface");
			System.exit(1);
		}

		final LogicalTypeCode logicalTypeCode = (LogicalTypeCode)logicalType;

		for (int i = 0; i < 10; i++) {
			final String value = logicalTypeCode.nextRandom();
			System.err.println(value);
			if (!logicalTypeCode.isValid(value))
				System.err.println("Issue with LogicalType'" + logicalTypeCode.getDescription() + "', value: " + value + "\n");
		}
	}
}
