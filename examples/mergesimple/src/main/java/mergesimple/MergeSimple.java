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
package mergesimple;

import java.util.HashMap;
import java.util.Map;

import com.cobber.fta.LogicalType;
import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;

public abstract class MergeSimple {
	public static void main(final String[] args) throws FTAException {

		// Analyze the data in the first shard - no Semantic Type detected
		final TextAnalyzer analysisOne = new TextAnalyzer("Col1");
		final Map<String, Long> onlyMale = new HashMap<>();

		onlyMale.put("Male", 2_000_000L);
		analysisOne.trainBulk(onlyMale);
		final TextAnalysisResult resultOne = analysisOne.getResult();
		System.err.printf("Shard1 - Semantic Type: %s (%s)%n", resultOne.getSemanticType(), resultOne.getType());

		// Analyze the data in the second shard - no Semantic Type detected
		final TextAnalyzer analysisTwo = new TextAnalyzer("Col1");
		final Map<String, Long> onlyFemale = new HashMap<>();

		onlyFemale.put("Female", 2_000_000L);
		analysisTwo.trainBulk(onlyFemale);
		final TextAnalysisResult resultTwo = analysisTwo.getResult();
		System.err.printf("Shard2 - Semantic Type: %s (%s)%n", resultTwo.getSemanticType(), resultTwo.getType());

		// Now merge the two shards - Semantic Type correctly identified
		final TextAnalyzer analysisMerged = TextAnalyzer.merge(analysisOne, analysisTwo);
		final TextAnalysisResult resultMerged = analysisMerged.getResult();
		System.err.printf("Merged - Semantic Type: %s (%s)%n", resultMerged.getSemanticType(), resultMerged.getType());
		System.err.println("Detail: " + resultMerged.asJSON(true, 1));
	}
}
