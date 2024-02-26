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

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.cobber.fta.dates.DateTimeParser;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;

public abstract class DetermineDateFormat {

	public static void main(final String[] args) {
		final DateTimeParser dtp = new DateTimeParser().withDateResolutionMode(DateResolutionMode.MonthFirst).withLocale(Locale.ENGLISH);

		// Determine the DataTimeFormatter for the following examples
		System.err.printf("Format is: '%s'%n", dtp.determineFormatString("26 July 2012"));
		System.err.printf("Format is: '%s'%n", dtp.determineFormatString("March 9 2012"));
		// Note: Detected as MM/dd/yyyy despite being ambiguous as we indicated MonthFirst above when insufficient data
		System.err.printf("Format is: '%s'%n", dtp.determineFormatString("07/04/2012"));
		System.err.printf("Format is: '%s'%n", dtp.determineFormatString("2012 March 20"));
		System.err.printf("Format is: '%s'%n", dtp.determineFormatString("2012/04/09 18:24:12"));
		System.err.printf("Format is: '%s'%n", dtp.determineFormatString("02-01-2014 12:00 AM"));

		// Determine format of the input below and then parse it
		final String input = "Wed Mar 04 05:09:06 GMT-06:00 2009";

		final String formatString = dtp.determineFormatString(input);

		// Grab the DateTimeFormatter from fta as this creates a case-insensitive parser and it supports a slightly wider set set of formats
		// For example, "yyyy" does not work out of the box if you use DateTimeFormatter.ofPattern
		final DateTimeFormatter formatter = dtp.ofPattern(formatString);

		final OffsetDateTime parsedDate = OffsetDateTime.parse(input, formatter);

		System.err.printf("Format is: '%s', Date is: '%s'%n", formatString, parsedDate.toString());
	}
}
