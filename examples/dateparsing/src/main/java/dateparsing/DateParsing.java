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
package dateparsing;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;
import com.cobber.fta.dates.DateTimeParser;

public abstract class DateParsing {

	public static void main(final String[] args) throws FTAException {

		final TextAnalyzer analysis = new TextAnalyzer("DateOfBirth");
		final String[] inputs = {
				"11/25/2010 11:12:38 AM",  "9/21/2010 7:31:26 AM", "9/18/2010 2:37:58 PM", "12/14/2010 11:08:14 AM",
				"10/13/2010 1:17:01 PM", "10/13/2010 1:17:02 PM", "10/13/2010 1:17:00 PM", "10/13/2010 1:17:09 PM",
				"10/13/2010 1:16:04 PM","11/25/2010 11:13:38 AM", "11/25/2010 11:13:38 AM", "9/20/2010 7:31:26 AM",
				"9/17/2010 2:37:58 PM", "12/14/2010 11:08:17 AM", "10/13/2010 1:27:04 PM", "10/13/2010 1:13:04 PM",
				"10/13/2009 2:17:04 PM", "10/10/2010 1:17:04 PM", "10/09/2010 1:17:04 PM", "9/20/2010 7:31:26 AM",
				"9/17/2010 2:37:58 PM", "12/14/2010 11:08:17 AM", "10/08/2010 1:17:04 PM", "10/13/2010 1:12:04 PM",
				"10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM", "10/07/2010 1:17:04 PM"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		System.err.printf("Result: %s, Regular Expression: %s, Max: %s, Min: %s.%n", result.getType(), result.getRegExp(), result.getMaxValue(), result.getMinValue());

		System.err.println("Detail: " + result.asJSON(true, 1));

		// Now prove it works!!
		// Grab the DateTimeFormatter from fta as this creates a case-insensitive parser and it supports a slightly wider set set of formats
		// For example, "yyyy" does not work out of the box if you use ofPattern
		final DateTimeFormatter formatter = new DateTimeParser().ofPattern(result.getTypeModifier());

		for (final String input : inputs)
			LocalDateTime.parse(input, formatter);
	}
}
