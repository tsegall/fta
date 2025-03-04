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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAMergeException;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAUnsupportedLocaleException;

public abstract class MergeSimple {
	public static void main(final String[] args) throws FTAException, IOException {
		simple();
		hydrationAndTotals();
	}

	public static void simple() throws FTAPluginException, FTAUnsupportedLocaleException, FTAMergeException {
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
		final TextAnalyzer merged = TextAnalyzer.merge(analysisOne, analysisTwo);
		final TextAnalysisResult mergedResult = merged.getResult();
		System.err.printf("Merged - Semantic Type: %s (%s)%n", mergedResult.getSemanticType(), mergedResult.getType());
		System.err.println("Detail: " + mergedResult.asJSON(true, 1));
	}

	public static void hydrationAndTotals() throws IOException, FTAException {
		final int SAMPLE_COUNT = 20000;

		final TextAnalyzer shardOneAnalyzer = new TextAnalyzer("issue124");
		for (int i = 0; i < SAMPLE_COUNT; i++)
			shardOneAnalyzer.train(String.valueOf(i %2 == 0 ? i : -i));
		shardOneAnalyzer.train(null);
		shardOneAnalyzer.train(null);
		final TextAnalyzer hydratedOne = TextAnalyzer.deserialize(shardOneAnalyzer.serialize());
		// Note we must set the totalCount on the analyzer pre-merge
		final TextAnalysisResult hydratedOneResult = hydratedOne.getResult();
		hydratedOne.setTotalCount(SAMPLE_COUNT);
		hydratedOne.setTotalNullCount(hydratedOneResult.getNullCount());

		final TextAnalyzer shardTwoAnalyzer = new TextAnalyzer("issue124");
		for (int i = SAMPLE_COUNT; i < SAMPLE_COUNT  * 2; i++)
			shardTwoAnalyzer.train(String.valueOf(i %2 == 0 ? i : -i));
		final TextAnalyzer hydratedTwo = TextAnalyzer.deserialize(shardTwoAnalyzer.serialize());
		// Note we must set the totalCount on the analyzer pre-merge
		final TextAnalysisResult hydratedTwoResult = hydratedTwo.getResult();
		hydratedTwo.setTotalCount(SAMPLE_COUNT);
		hydratedTwo.setTotalNullCount(hydratedTwoResult.getNullCount());

		// Merge the two hydrated TextAnalyzers
		final TextAnalyzer merged = TextAnalyzer.merge(hydratedOne, hydratedTwo);
		final TextAnalysisResult mergedResult = merged.getResult();

		final long sampleCount = mergedResult.getSampleCount();
		final long totalCount = mergedResult.getTotalCount();
		final long nullCount = mergedResult.getNullCount();
		final long totalNullCount = mergedResult.getTotalNullCount();

		System.err.printf("Merged - Type: %s (samples: %d, total: %d, nulls: %d, total nulls: %d)%n", mergedResult.getType(), sampleCount, totalCount, nullCount, totalNullCount);
		if (sampleCount != 2 * AnalysisConfig.MAX_CARDINALITY_DEFAULT + 4 * 10 + 2 || totalCount != 2 * SAMPLE_COUNT + 2)
			System.err.printf("ERROR - in sampleCount or totalCount");
	}
}
