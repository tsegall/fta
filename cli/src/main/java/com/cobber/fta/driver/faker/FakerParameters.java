/*
 * Copyright 2017-2023 Tim Segall
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

public class FakerParameters {
	String format;
	String low;
	String high;
	public double nullPercent = 0.0;
	public double blankPercent = 0.0;
	public int blankLength = -1;
	public String type;
	public String clazz;
	public String distribution;
	public String values;

	public FakerParameters(final String config) {
		final String[] attributes = config.split(";");

		for (final String attribute : attributes) {
			final String[] components = attribute.split("=");
			if ("format".equals(components[0]))
				format = components[1];
			else if ("low".equals(components[0]))
				low = components[1];
			else if ("high".equals(components[0]))
				high = components[1];
			else if ("type".equals(components[0]))
				type = components[1];
			else if ("nulls".equals(components[0]))
				nullPercent = Double.parseDouble(components[1]);
			else if ("blanks".equals(components[0]))
				blankPercent = Double.parseDouble(components[1]);
			else if ("distribution".equals(components[0]))
				distribution = components[1];
			else if ("blankLength".equals(components[0]))
				blankLength = Integer.parseInt(components[1]);
			else if ("values".equals(components[0]))
				values = components[1];

			if ("LOCALDATE".equals(type))
				clazz = "com.cobber.fta.driver.faker.FakerLocalDateLT";
			else if ("LOCALDATETIME".equals(type))
				clazz = "com.cobber.fta.driver.faker.FakerLocalDateTimeLT";
			else if ("DOUBLE".equals(type))
				clazz = "com.cobber.fta.driver.faker.FakerDoubleLT";
			else if ("LONG".equals(type))
				clazz = "com.cobber.fta.driver.faker.FakerLongLT";
			else if ("ENUM".equals(type))
				clazz = "com.cobber.fta.driver.faker.FakerEnumLT";
		}
	}
}
