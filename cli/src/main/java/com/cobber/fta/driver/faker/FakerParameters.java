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

public class FakerParameters {
	public String fieldName;
	public int index;
	public String type;
	public String format;
	public String low;
	public String high;
	public int minLength;
	public int maxLength;
	public double nullPercent = 0.0;
	public double blankPercent = 0.0;
	public int blankLength = -1;
	public String clazz;
	public String distribution;
	public String[] values;

	public FakerParameters() {
	}

	public void bind() {
		if ("LOCALDATE".equals(type))
			clazz = "com.cobber.fta.driver.faker.FakerLocalDateLT";
		else if ("LOCALDATETIME".equals(type))
			clazz = "com.cobber.fta.driver.faker.FakerLocalDateTimeLT";
		else if ("OFFSETDATETIME".equals(type))
			clazz = "com.cobber.fta.driver.faker.FakerOffsetDateTimeLT";
		else if ("LOCALTIME".equals(type))
			clazz = "com.cobber.fta.driver.faker.FakerLocalTimeLT";
		else if ("DOUBLE".equals(type))
			clazz = "com.cobber.fta.driver.faker.FakerDoubleLT";
		else if ("LONG".equals(type))
			clazz = "com.cobber.fta.driver.faker.FakerLongLT";
		else if ("BOOLEAN".equals(type))
			clazz = "com.cobber.fta.driver.faker.FakerBooleanLT";
		else if ("ENUM".equals(type))
			clazz = "com.cobber.fta.driver.faker.FakerEnumLT";
		else if ("STRING".equals(type))
			clazz = "com.cobber.fta.driver.faker.FakerStringLT";
	}
}
