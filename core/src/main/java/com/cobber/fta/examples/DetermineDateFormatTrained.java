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

import java.util.Locale;

import com.cobber.fta.dates.DateTimeParser;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;

public abstract class DetermineDateFormatTrained {

	public static void main(final String[] args) {
		final DateTimeParser dtp = new DateTimeParser(DateResolutionMode.None, Locale.ENGLISH);

		String inputs[] = { "10/1/2008", "10/2/2008", "10/3/2008", "10/4/2008", "10/5/2008", "10/10/2008" };

		for (final String input : inputs)
			dtp.train(input);

		// At this stage we are not sure of the date format, since with DateResolutionMode == None we make no
		// assumption whether it is MM/DD or DD/MM	and the format String is unbound (??/?/yyyy)
		System.err.println(dtp.getResult().getFormatString());

		dtp.train("10/15/2008");

		// Once we train with another value which indicates that the Day must be the second field then the new
		// result is correctly determined to be MM/d/yyyy
		System.err.println(dtp.getResult().getFormatString());
	}
}
