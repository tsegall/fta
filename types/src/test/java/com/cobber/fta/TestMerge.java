package com.cobber.fta;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAMergeException;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.FTAUnsupportedLocaleException;
import com.cobber.fta.core.Utils;
import com.cobber.fta.plugins.Gender;

public class TestMerge {
	private static final SecureRandom random = new SecureRandom();

	private final List<String> samplesBLANK = new ArrayList<>();
	private final List<String> samplesNULL = new ArrayList<>();
	private final List<String> samplesBLANKORNULL = new ArrayList<>();
	private final List<String> samplesAlphaData = new ArrayList<>();

	private final List<String> samplesAlphaSimple = new ArrayList<>();

	private final static String[] shortStrings = {
			"baby", "back", "bad", "bag", "ball", "bank", "base", "bath", "be", "bean",
			"bear", "bed", "beer", "bell", "best", "big", "bird", "bit", "bite", "black",
			"bleed", "block", "blood", "blow", "blue", "board", "boat", "body", "boil",
			"bone", "book", "born", "both", "bowl", "box", "boy", "brave", "bread", "break",
			"bring", "brown", "brush", "build", "burn", "bus", "busy", "but", "buy",
			"cake", "call", "can", "cap", "car", "card", "care", "carry", "case", "cat", "catch", "chair",
			"chance", "change", "chase", "cheap", "cheese", "child", "choose", "city", "class", "clever",
			"clean", "clear", "climb", "clock", "cloth", "cloud", "close", "coat", "coin", "cold", "comb",
			"come", "cook", "cool", "corn", "cost", "count", "cover", "crash", "cross", "cry", "cup", "cut"
	};

	private final static String[] longStrings = {
			"across", "active", "activity", "afraid", "already", "always", "amount", "another", "answer", "anyone",
			"anything", "anytime", "appear", "around", "arrive", "attack", "autumn", "basket", "beautiful", "bedroom",
			"behave", "before", "behind", "besides", "better", "between", "birthday", "border", "borrow", "bottle",
			"bottom", "branch", "breakfast", "breathe", "bridge", "bright", "brother", "business", "candle", "careful",
			"careless", "central", "century", "certain", "chance", "change", "cheese", "chicken", "children", "chocolate",
			"choice", "choose", "circle", "clever", "clothes", "cloudy", "coffee", "collect", "colour", "comfortable",
			"common", "compare", "complete", "computer", "condition", "continue", "control", "copper", "corner", "correct",
			"contain", "country", "course", "cupboard", "dangerous", "daughter", "decide", "decrease", "depend", "destroy",
			"develop", "different", "difficult", "dinner", "direction", "discover", "double", "education", "effect", "either",
			"electric", "elephant", "enough", "entrance", "escape", "evening", "everyone", "everybody", "examination", "example",
			"except", "excited", "exercise", "expect", "expensive", "explain", "extremely"
	};

	@BeforeClass(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void classSetup() throws FTAPluginException, FTAUnsupportedLocaleException {
		String sample;
		for (int i = 60; i <= 90; i++) {
			sample = Utils.repeat(' ', i);
			samplesBLANK.add(sample);
		}

		for (int i = 1; i < 30; i++)
			samplesNULL.add(null);

		for (int i = 1; i < 30; i++) {
			sample = Utils.repeat(' ', i);
			samplesBLANKORNULL.add(sample);
			samplesBLANKORNULL.add(null);
		}

		for (int i = 0; i < 26; i++) {
			sample = Utils.repeat((char)('A' + i), i + 1);
			samplesAlphaData.add(sample);
			samplesAlphaData.add(null);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void splitLogical() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;

		List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			shardOne.add("MALE");

		List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			shardTwo.add("FEMALE");

		final TextAnalyzer merged = checkTextAnalyzerMerge(shardOne, shardTwo, "MALE_FEMALE");
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.STRING);
		assertTrue(mergedResult.isLogicalType());
		assertEquals(mergedResult.getTypeQualifier(), Gender.SEMANTIC_TYPE + "EN");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void testBulkLong() throws IOException, FTAException {
		final long SAMPLE_COUNT = 100L;

		List<String> samplesLong100 = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesLong100.add("100");
		samplesLong100.add(null);
		samplesLong100.add(" ");

		List<String> samplesLong200 = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesLong200.add("200");
		samplesLong200.add(null);
		samplesLong200.add(null);
		samplesLong200.add("  ");
		samplesLong200.add("  ");
		samplesLong200.add("x");

		final TextAnalyzer merged = checkTextAnalyzerMerge(samplesLong100, samplesLong200, "long_long");
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.LONG);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void cardinalityExceededLong() throws IOException, FTAException {

		List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardOne.add(String.valueOf(i));

		List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardTwo.add(String.valueOf(100000 + i));

		checkTextAnalyzerMerge(shardOne, shardTwo, "cardinalityExceededLong");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void cardinalityExceededDouble() throws IOException, FTAException {

		List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardOne.add(String.valueOf(i) + ".0");

		List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardTwo.add(String.valueOf(100000 + i) + ".0");

		checkTextAnalyzerMerge(shardOne, shardTwo, "cardinalityExceededDouble");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void cardinalityNotExceededString() throws IOException, FTAException {
		List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardOne.add(shortStrings[random.nextInt(shortStrings.length)]);

		List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardTwo.add(longStrings[random.nextInt(longStrings.length)]);

		final TextAnalyzer merged = checkTextAnalyzerMerge(shardOne, shardTwo, "cardinalityNotExceededString");
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.STRING);

	}

	private static String randomString(int length) {
		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		StringBuilder b = new StringBuilder(length);
		for (int i = 0; i < length; i++)
			b.append(alphabet.charAt(random.nextInt(alphabet.length())));

		return b.toString();
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void cardinalityExceededString() throws IOException, FTAException {
		List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardOne.add(randomString(5));

		List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardTwo.add(randomString(9));

		final TextAnalyzer merged = checkTextAnalyzerMerge(shardOne, shardTwo, "cardinalityExceededString");
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.STRING);
		assertEquals(mergedResult.getLeadingWhiteSpace(), false);
		assertEquals(mergedResult.getTrailingWhiteSpace(), false);
		assertEquals(mergedResult.getMultiline(), false);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void cardinalityExceededStringTrailingSpace() throws IOException, FTAException {
		List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardOne.add(randomString(5));
		shardOne.add("SPACE     ");

		List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardTwo.add(randomString(9));

		final TextAnalyzer merged = checkTextAnalyzerMerge(shardOne, shardTwo, "cardinalityExceededStringTrailingSpace");
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.STRING);
		assertEquals(mergedResult.getLeadingWhiteSpace(), false);
		assertEquals(mergedResult.getTrailingWhiteSpace(), true);
		assertEquals(mergedResult.getMultiline(), false);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void cardinalityExceededStringLeadingSpace() throws IOException, FTAException {
		List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardOne.add(randomString(5));
		shardOne.add("    SPACE");

		List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardTwo.add(randomString(9));

		final TextAnalyzer merged = checkTextAnalyzerMerge(shardOne, shardTwo, "cardinalityExceededStringLeadingSpace");
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.STRING);
		assertEquals(mergedResult.getLeadingWhiteSpace(), true);
		assertEquals(mergedResult.getTrailingWhiteSpace(), false);
		assertEquals(mergedResult.getMultiline(), false);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void cardinalityExceededStringMultiline() throws IOException, FTAException {
		List<String> shardOne = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardOne.add(randomString(5));
		shardOne.add("SPACE\n\n");

		List<String> shardTwo = new ArrayList<>();
		for (int i = 0; i < 20000; i++)
			shardTwo.add(randomString(9));

		final TextAnalyzer merged = checkTextAnalyzerMerge(shardOne, shardTwo, "cardinalityExceededStringMultiline");
		final TextAnalysisResult mergedResult = merged.getResult();

		assertEquals(mergedResult.getType(), FTAType.STRING);
		assertEquals(mergedResult.getLeadingWhiteSpace(), false);
		assertEquals(mergedResult.getTrailingWhiteSpace(), false);
		assertEquals(mergedResult.getMultiline(), true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void checkSerialization() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;

		final TextAnalyzer shard = new TextAnalyzer("checkSerialization");
		for (int i = 0; i < SAMPLE_COUNT; i++)
			shard.train(String.valueOf(i));

		// Test pre getResult()
		String serialized = shard.serialize();
		final TextAnalyzer hydrated = TextAnalyzer.deserialize(serialized);
		assertEquals(serialized, hydrated.serialize());

		// Test a hydrated object
		serialized = hydrated.serialize();
		final TextAnalyzer rehydrated = TextAnalyzer.deserialize(serialized);
		assertEquals(serialized, rehydrated.serialize());

		TextAnalysisResult result = rehydrated.getResult();
		assertEquals(result.getType(), FTAType.LONG);

		// Test post getResult()
		serialized = rehydrated.serialize();
		assertEquals(serialized, TextAnalyzer.deserialize(serialized).serialize());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void simpleLongSerialize() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;

		List<String> samplesLong100 = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesLong100.add("100");
		samplesLong100.add(null);
		samplesLong100.add(" ");

		List<String> samplesLong200 = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesLong200.add("200");
		samplesLong200.add(null);
		samplesLong200.add(null);
		samplesLong200.add("  ");
		samplesLong200.add("  ");
		samplesLong200.add("x");

		checkTextAnalyzerMerge(samplesLong100, samplesLong200, "long_long");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void simpleDoubleMerge() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;

		List<String> samplesDouble100 = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesDouble100.add("100.0");
		samplesDouble100.add(null);
		samplesDouble100.add(" ");

		List<String> samplesDouble200 = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesDouble200.add("100.0");
		samplesDouble200.add(null);
		samplesDouble200.add(null);
		samplesDouble200.add(" ");
		samplesDouble200.add(" ");

		checkTextAnalyzerMerge(samplesDouble100, samplesDouble200, "double_double");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void simpleStringMerge() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;

		final List<String> samplesOne = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesOne.add("a");
		samplesOne.add("z");
		samplesOne.add(" ");
		samplesOne.add(null);

		final List<String> samplesTwo = new ArrayList<>();
		for (int i = 0; i < SAMPLE_COUNT; i++)
			samplesTwo.add("bb");
		samplesTwo.add(null);
		samplesTwo.add(null);

		checkTextAnalyzerMerge(samplesOne, samplesTwo, "string_string");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void NullNullStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesNULL, samplesNULL, "NULL_NULL");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void NullBlankStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesNULL, samplesBLANK, "NULL_BLANK");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void NullBlankOrNullStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesNULL, samplesBLANKORNULL, "NULL_BLANKORNULL");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void NullAlphaDataStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesNULL, samplesBLANKORNULL, "NULL_BLANKORNULL");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void blankNullStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesBLANK, samplesNULL, "BLANK_NULL");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void blankBlankStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesBLANK, samplesBLANK, "BLANK_BLANK");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void blankBlankOrNullStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesBLANK, samplesBLANKORNULL, "BLANK_BLANKORNULL");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void blankAlphaDataStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesBLANK, samplesAlphaData, "BLANK_ALPHADATA");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void blankOrNullNullStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesBLANKORNULL, samplesNULL, "BLANKORNULL_NULL");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void blankOrNullBlankStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesBLANKORNULL, samplesBLANK, "BLANKORNULL_BLANK");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void blankOrNullBlankOrNullStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesBLANKORNULL, samplesBLANKORNULL, "BLANKORNULL_BLANKORNULL");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void blankOrNullAlphaDataStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesBLANKORNULL, samplesAlphaData, "BLANKORNULL_ALPHADATA");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void AlphaDataNullStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesAlphaData, samplesNULL, "ALPHADATA_NULL");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void AlphaDataBlankStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesAlphaData, samplesBLANK, "ALPHADATA_BLANK");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void AlphaDataBlankOrNullStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesAlphaData, samplesBLANKORNULL, "ALPHADATA_BLANKORNULL");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.MERGE })
	public void AlphaDataAlphaDataStringTest() throws IOException, FTAException {
		checkTextAnalyzerMerge(samplesAlphaData, samplesAlphaData, "ALPHADATA_ALPHADATA");
	}

	private TextAnalyzer checkTextAnalyzerMerge(List<String> samplesOne, List<String> samplesTwo, String streamName) throws FTAPluginException, FTAUnsupportedLocaleException, FTAMergeException {
		final TextAnalyzer shardOne = new TextAnalyzer(streamName);
		final TextAnalyzer reference = new TextAnalyzer(streamName);

		long countOne = 0;
		long countTwo = 0;
		long countReference = 0;
		// Train the first shard (and the reference set)
		for (final String sample : samplesOne) {
			shardOne.train(sample);
			countOne++;
			reference.train(sample);
			countReference++;
		}
		shardOne.setTotalCount(countOne);
		// serialize and de-serialize it to check this process
		final TextAnalyzer hydratedOne = TextAnalyzer.deserialize(shardOne.serialize());

		// Train the second shard (and the reference set)
		final TextAnalyzer shardTwo = new TextAnalyzer(streamName);
		for (final String sample : samplesTwo) {
			shardTwo.train(sample);
			countTwo++;
			reference.train(sample);
			countReference++;
		}
		shardTwo.setTotalCount(countTwo);
		reference.setTotalCount(countReference);
		final TextAnalyzer hydratedTwo = TextAnalyzer.deserialize(shardTwo.serialize());
		assertEquals(hydratedTwo.getContext().getStreamName(), streamName);

		// Merge the two hydrated TextAnalyzers
		final TextAnalyzer merged = TextAnalyzer.merge(hydratedOne, hydratedTwo);

		final TextAnalysisResult mergedResult = merged.getResult();
		final String mergedJSON = mergedResult.asJSON(false, 1);
		final TextAnalysisResult referenceResult = reference.getResult();
		final String referenceJSON = referenceResult.asJSON(false, 1);

		boolean failed = false;
		// If we captured all the observation then the merge should be roughly perfect :-)
		if (referenceResult.getCardinality() < reference.getMaxCardinality()) {
			if (FTAType.isNumeric(mergedResult.getType())) {
				if (!merged.equals(reference, TestUtils.EPSILON))
					failed = true;
			}
			else {
				 if (!merged.equals(reference) || !mergedJSON.equals(referenceJSON))
					 failed = true;
			}
		}
		else {
			// We lost some results in the merge so compare what we can
			if (
					// Type and TypeQualifier (if it exists)
					!mergedResult.getType().equals(referenceResult.getType()) ||
					mergedResult.isLogicalType() != referenceResult.isLogicalType() ||
					(mergedResult.getTypeQualifier() != null && !mergedResult.getTypeQualifier().equals(referenceResult.getTypeQualifier())) ||
					// White-space
					mergedResult.getLeadingWhiteSpace() != referenceResult.getLeadingWhiteSpace() ||
					mergedResult.getTrailingWhiteSpace() != referenceResult.getTrailingWhiteSpace() ||
					mergedResult.getMultiline() != referenceResult.getMultiline() ||
					// Maximum/Minimum
					!mergedResult.getMaxValue().equals(referenceResult.getMaxValue()) ||
					!mergedResult.getMinValue().equals(referenceResult.getMinValue()) ||
					// Counts - totalCount, nullCount, blankCount
					mergedResult.getTotalCount() != referenceResult.getTotalCount() ||
					mergedResult.getNullCount() != referenceResult.getNullCount() ||
					mergedResult.getBlankCount() != referenceResult.getBlankCount() ||
					// TopK/BottomK
					!Objects.equals(mergedResult.getBottomK(), referenceResult.getBottomK()) ||
					!Objects.equals(mergedResult.getTopK(), referenceResult.getTopK()) ||
					// Structure
					!mergedResult.getStructureSignature().equals(referenceResult.getStructureSignature()) ||
					!mergedResult.getRegExp().equals(referenceResult.getRegExp())
			)
				failed = true;
			if (FTAType.isNumeric(mergedResult.getType())) {
				if (
						Math.abs(mergedResult.getMean() - referenceResult.getMean()) > TestUtils.EPSILON
//						||
//						Math.abs(mergedResult.getStandardDeviation() - referenceResult.getStandardDeviation()) > EPSILON
						)
					failed = true;
			}
		}

		if (failed) {
			System.err.println("Merged:\n" + mergedJSON);
			System.err.println("Reference:\n" + referenceJSON);
			fail();
		}

		return merged;
	}
}
