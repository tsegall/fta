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

import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;

public abstract class Merge {
	public static void main(final String[] args) throws FTAException {

		final int SAMPLE_COUNT = 100;

		// Create the first Analyzer
		final TextAnalyzer shardOne = new TextAnalyzer("Merge");
		for (int i = 0; i < SAMPLE_COUNT; i++)
			shardOne.train("FEMALE");
		final String serializedOne = shardOne.serialize();

		// Create the second Analyzer
		final TextAnalyzer shardTwo = new TextAnalyzer("Merge");
		for (int i = 0; i < SAMPLE_COUNT; i++)
			shardTwo.train("MALE");
		final String serializedTwo = shardTwo.serialize();

		final TextAnalyzer hydratedOne = TextAnalyzer.deserialize(serializedOne);
		System.err.println(hydratedOne.getResult().getTypeQualifier());
		final TextAnalyzer hydratedTwo = TextAnalyzer.deserialize(serializedTwo);
		System.err.println(hydratedTwo.getResult().getTypeQualifier());

		final TextAnalyzer merged = TextAnalyzer.merge(hydratedOne, hydratedTwo);

		System.err.println(merged.getResult().getTypeQualifier());
	}
}
