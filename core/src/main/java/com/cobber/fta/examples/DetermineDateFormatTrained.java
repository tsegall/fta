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
package com.cobber.fta.examples;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.cobber.fta.dates.DateTimeParser;

public abstract class DetermineDateFormatTrained {

	public static void main(final String[] args) {
		final DateTimeParser dtp = new DateTimeParser().withLocale(Locale.ENGLISH);

		final List<String> inputs = Arrays.asList( "10/1/2008", "10/2/2008", "10/3/2008", "10/4/2008", "10/5/2008", "10/10/2008" );

		inputs.forEach(dtp::train);

		// At this stage we are not sure of the date format, since with 'DateResolutionMode == None' we make no
		// assumption whether it is MM/DD or DD/MM and the format String is unbound (??/?/yyyy)
		System.err.println(dtp.getResult().getFormatString());

		// Once we train with another value which indicates that the Day must be the second field then the new
		// result is correctly determined to be MM/d/yyyy
		dtp.train("10/15/2008");
		System.err.println(dtp.getResult().getFormatString());

		// Once we train with another value which indicates that the Month is expressed using one or two digits the
		// result is correctly determined to be M/d/yyyy
		dtp.train("3/15/2008");
		System.err.println(dtp.getResult().getFormatString());
	}
}
