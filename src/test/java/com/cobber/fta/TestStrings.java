package com.cobber.fta;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestStrings {
	@Test
	public void variableLengthString() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "Hello World|Hello|H|Z|A".split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.freezeANY(1, 11, 1, 11, result.getLeadingWhiteSpace(), result.getTrailingWhiteSpace(), result.getMultiline()));
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getMinValue(), "A");
		Assert.assertEquals(result.getMaxValue(), "Z");
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxLength(), 11);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()), inputs[i]);
		}
	}

	@Test
	public void variableLengthStringWithOutlier() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "HelloWorld|Hello|H|Z|A|Do|Not|Ask|What|You|Can|Do".split("\\|");
		final int ITERATIONS = 10;

		int locked = -1;

		for (int iters = 0; iters < ITERATIONS; iters++) {
			for (int i = 0; i < inputs.length; i++) {
				if (analysis.train(inputs[i]) && locked != -1)
					locked = i;
			}
		}
		analysis.train(";+");


		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length * ITERATIONS + 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "(?i)(A|ASK|CAN|DO|H|HELLO|HELLOWORLD|NOT|WHAT|YOU|Z)");
		Assert.assertEquals(result.getMatchCount(), inputs.length * ITERATIONS);
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getMinValue(), "A");
		Assert.assertEquals(result.getMaxValue(), "Z");
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxLength(), 10);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void testEnumsWithSpecialChars() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("ShipRegion");
		final String[] inputs = new String[] {
			"RJ", "SP", "Táchira", "RJ", "NM", "DF", "WA", "WY", "NM", "Lara",
			"RJ", "SP", "RJ", "SP", "NM", "Lara", "Co. Cork", "RJ", "AK", "OR",
			"Co. Cork", "OR", "NM", "Isle of Wight", "NM", "OR", "Isle of Wight", "ID", "WY", "Lara",
			"Québec", "Co. Cork", "AK", "Québec", "WA", "NM", "SP", "WY", "Essex", "Lara",
			"WY", "SP", "Co. Cork", "OR", "Québec", "RJ", "Co. Cork", "Lara", "Essex", "WY",
			"SP", "BC", "ID", "OR", "Táchira", "ID", "NM", "Nueva Esparta", "SP", "BC",
			"SP", "OR", "SP", "RJ", "SP", "Québec", "Co. Cork", "BC", "WY", "Québec",
			"ID", "AK", "RJ", "ID", "Essex", "Lara", "SP", "WA", "Isle of Wight", "Táchira",
			"NM", "RJ", "WA", "Nueva Esparta", "Táchira", "SP", "Táchira", "BC", "SP", "BC",
			"SP", "Táchira", "Lara", "Co. Cork", "WA", "Québec", "ID", "SP", "Co. Cork"
		};

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "(?i)(AK|BC|CO\\. CORK|DF|ESSEX|ID|ISLE OF WIGHT|LARA|NM|NUEVA ESPARTA|OR|QUÉBEC|RJ|SP|TÁCHIRA|WA|WY)");
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getMinValue(), "AK");
		Assert.assertEquals(result.getMaxValue(), "WY");
		Assert.assertEquals(result.getMinLength(), 2);
		Assert.assertEquals(result.getMaxLength(), 13);

// BUG/TODO
//		for (int i = 0; i < inputs.length; i++) {
//			Assert.assertTrue(inputs[i].matches(result.getRegExp()), inputs[i]);
//		}
	}



	@Test
	public void manyConstantLengthStrings() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		analysis.setCollectStatistics(false);
		final int nullIterations = 50;
		final int iterations = 10000;
		final Random random = new Random();
		final int length = 12;
		final String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		final StringBuilder b = new StringBuilder(length);
		int locked = -1;

		for (int i = 0; i < nullIterations; i++) {
			analysis.train(null);
		}
		for (int i = 0; i < iterations; i++) {
			b.setLength(0);
			for (int j = 0; j < length; j++) {
				b.append(alphabet.charAt(Math.abs(random.nextInt()%alphabet.length())));
			}
			if (analysis.train(b.toString()) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), iterations + nullIterations);
		//		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), nullIterations);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{12}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void niceEnum() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		analysis.setCollectStatistics(false);
		final String[] inputs = new String[] { "Corrective", "Discretionary", "Marketing/Retention", "Reactivation(FS Only)", "Undefined" };
		final int iterations = 10;

		for (int i = 0; i < iterations; i++) {
			for (String sample : inputs)
				analysis.train(sample);
		}

		TextAnalysisResult result = analysis.getResult();
		result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length * iterations);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getRegExp(), "(?i)(CORRECTIVE|DISCRETIONARY|MARKETING/RETENTION|\\QREACTIVATION(FS ONLY)\\E|UNDEFINED)");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	public void _stringPerf(boolean statisticsOn) throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		if (!statisticsOn) {
			analysis.setDefaultLogicalTypes(false);
			analysis.setCollectStatistics(false);
		}
		final Random random = new Random(314);
		final long sampleCount = 100_000_000_000L;
		boolean saveOutput = false;
		BufferedWriter bw = null;
		String[] samples = new String[10000];

		if (saveOutput)
			bw = new BufferedWriter(new FileWriter("/tmp/stringPerf.csv"));

		final String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		final StringBuilder b = new StringBuilder(alphabet.length());
		for (int i = 0; i < samples.length; i++) {
			int length = random.nextInt(alphabet.length()) + 1;

			b.setLength(0);
			for (int j = 0; j < length; j++)
				b.append(alphabet.charAt(Math.abs(random.nextInt()%alphabet.length())));
			samples[i] = b.toString();
		}

		long start = System.currentTimeMillis();

		long iters = 0;
		// Run for about reasonable number of seconds
		int seconds = 5;
		for (iters = 0; iters < sampleCount; iters++) {
			String sample = samples[(int)(iters%samples.length)];
			analysis.train(sample);
			if (bw != null)
				bw.write(sample + '\n');
			if (iters%100 == 0 && System.currentTimeMillis()  - start >= seconds * 1_000)
				break;

		}
		final TextAnalysisResult result = analysis.getResult();
		if (bw != null)
			bw.close();

		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), iters + 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxLength(), 52);
		Assert.assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{1,52}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		System.err.printf("Count %d, duration: %dms, ~%d per second\n", iters + 1, System.currentTimeMillis() - start, (iters  + 1)/seconds);

		// With Statistics & LogicalTypes
		//   - Count 27154301, duration: 10003ms, ~2,715,430 per second
		// No Statistics & No LogicalTypes
		//   - Count 27322001, duration: 10003ms, ~2,732,200 per second
	}

	@Test
	public void stringPerf() throws IOException {
		_stringPerf(true);
	}

	@Test
	public void stringPerfNoStatistics() throws IOException {
		_stringPerf(false);
	}
}
