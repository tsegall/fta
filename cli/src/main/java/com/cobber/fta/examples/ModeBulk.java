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

import java.util.HashMap;

import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;

public abstract class ModeBulk {
	public static void main(final String[] args) throws FTAException {

		final TextAnalyzer analysis = new TextAnalyzer("Gender");
		final HashMap<String, Long> basic = new HashMap<>();

		basic.put("Male", 2_000_000L);
		basic.put("Female", 1_000_000L);
		basic.put("Unknown", 10_000L);

		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();

		System.err.printf("Semantic Type: %s (%s)%n", result.getSemanticType(), result.getType());

		System.err.println("Detail: " + result.asJSON(true, 1));
	}
}