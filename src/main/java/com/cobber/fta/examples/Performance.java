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

import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;

public abstract class Performance {

	public static void main(final String[] args) throws FTAException {

		final TextAnalyzer analysis = new TextAnalyzer("DateOfBirth");

		// To maximize performance - disable default logical types and Statistics
		analysis.setDefaultLogicalTypes(false);
		analysis.setCollectStatistics(false);

		final String[] inputs = {
				"11/25/2010 11:13:48 AM",  "9/20/2010 7:30:26 AM", "9/17/2010 2:27:58 PM", "12/14/2010 11:07:17 AM",
				"10/13/2010 1:17:24 PM", "10/13/2010 1:15:04 PM", "10/13/2010 1:14:04 PM", "10/13/2010 1:13:04 PM",
				"10/13/2010 1:12:04 PM","11/25/2010 11:13:38 AM", "11/25/2010 11:13:38 AM", "9/20/2010 7:31:26 AM",
				"9/17/2010 2:37:58 PM", "12/14/2010 11:08:17 AM", "10/13/2010 2:17:04 PM", "10/13/2010 3:17:04 PM",
				"10/13/2010 4:17:04 PM", "10/13/2010 5:17:04 PM", "10/13/2010 6:17:04 PM", "9/20/2010 7:31:26 AM",
				"9/17/2010 2:37:58 PM", "12/14/2010 11:08:17 AM", "10/13/2010 7:17:04 PM", "10/13/2010 8:17:04 PM",
				"10/13/2010 1:17:01 PM", "10/13/2010 1:17:04 PM", "10/13/2010 1:17:04 PM"
		};

		final long start = System.currentTimeMillis();

		long iters;
		for (iters = 0; iters < 100_000; iters++)
			for (final String input : inputs)
				analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		System.err.println("Result: " + result.asJSON(true, 1));

		final long duration = System.currentTimeMillis() - start;

		System.err.printf("Count %d, duration: %dms, ~%d per second%n", iters * inputs.length, duration, (long)((iters * inputs.length) / ((double)duration/1000)));
	}
}
