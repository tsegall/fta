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
package com.cobber.fta.examples;

import java.time.LocalDateTime;

import com.cobber.fta.dates.DateTimeParser;
import com.cobber.fta.dates.DateTimeParserResult;

public abstract class Speed {

	public static void main(final String[] args) {
		final int ITERATIONS = 10_000_000;
		LocalDateTime localDateTime = LocalDateTime.now();

		String value = localDateTime.toString();

		final DateTimeParser dtp = new DateTimeParser();

		dtp.train(value);

		DateTimeParserResult result = dtp.getResult();

		// Use the FTA date parser to validate dates
		// Note: this is ~10x faster than the Java parser, however you do not get any object back
		long start = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; i++)
			result.parse(value);
		long timeFTA = System.currentTimeMillis() - start;
		System.err.printf("Elapsed (FTA): %dms, type: %s%n", timeFTA, result.getType());

		// Use the Java date parser to validate dates
		start = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; i++)
			LocalDateTime.parse(value);
		long timeLDT = System.currentTimeMillis() - start;
		System.err.printf("Elapsed (LDT): %dms%n", timeLDT);
	}
}
