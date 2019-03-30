package com.cobber.fta;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

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


		TextAnalysisResult result = analysis.getResult();

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

		for (int i = 0; i < inputs.length; i++)
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));

		Map<String,Long> details = result.getCardinalityDetails();
		details.putAll(result.getOutlierDetails());
		final TextAnalyzer analysisBulk = new TextAnalyzer();
		analysisBulk.trainBulk(details);
		result = analysisBulk.getResult();

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

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), iterations + nullIterations);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), result.getNullCount());
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{12}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		Map<String,Long> details = result.getCardinalityDetails();
		details.put(null, result.getNullCount());
		long sum = details.values().stream().collect(Collectors.summingLong(Long::longValue));
		final TextAnalyzer analysisBulk = new TextAnalyzer();
		analysisBulk.trainBulk(details);
		result = analysisBulk.getResult();

		Assert.assertEquals(result.getSampleCount(), sum);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), nullIterations);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{12}");
		Assert.assertEquals(result.getConfidence(), 1.0);

	}

	@Test
	public void constantNoise() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("NOISE");
		analysis.setCollectStatistics(false);
		final String[] inputs = new String[] {
				"3D4657263441283442A1202020202020", "2832412833553D365529202020202020", "2F36423D334229464326202020202020",
				"2832432834432F3359A1202020202020", "2832432834432F335A24202020202020", "28324328344325304325202020202020",
				"28324328344325304328202020202020", "3F3056254644A146423F202020202020", "3D46572534563D344424202020202020",
				"2832412946432F324329202020202020", "283243283443253044A1202020202020", "3F365624464128335624202020202020",
				"263958243455293442A1202020202020", "2639562936563D335829202020202020", "3D36563D344325454424202020202020",
				"28324328344325465525202020202020", "A145442639432645572F202020202020", "2832562345432932433F202020202020",
				"A1394324345825324325202020202020", "A1454426394326455823202020202020", "28324328344324345825202020202020",
				"3F3056263658A130563D202020202020", "2634562F394328454426202020202020", "2832562F36562F45553F202020202020",
				"283256A1335A24345929202020202020", "2832432834432434443F202020202020", "25394326455923455A25202020202020",
				"2832432834432430553D202020202020", "283256A132553D34553D202020202020", "2832562346563D4656A1202020202020",
				"2832562346563D465723202020202020", "2832562346563D465726202020202020", "263056254655A1335728202020202020",
				"2F36413D34422845573D202020202020", "28324328344323465825202020202020", "3D465924305725305928202020202020",
				"2546572545443F3044A1202020202020", "2546573D335925464123202020202020", "3D34592F344329324224202020202020"
				};
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
		Assert.assertEquals(result.getRegExp(), "[\\p{IsAlphabetic}\\d]{32}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void testMix() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("CUSIP");
		final String input =
				"756059702|G39637955|029326105|63009R109|04269E957|666666666|00768Y727|23908L306|126349AF6|73937B589|" +
				"516806956|683797104|902973954|600544950|156719909|00724F951|292104106|008479904|219350955|67401P958|" +
				"902641752|50218P957|00739L901|06746P903|92189F953|G47567905|06740P650|13123X952|38173M952|29359T102|" +
				"229663959|33734E103|118230951|883556102|689648103|97382A900|808194954|60649T957|13645T900|075896950|" +
				"29266S956|80105N905|032332904|73935X951|73935B955|464288125|87612G901|39945C909|97717X957|14575E105|" +
				"75605A702|G39637955|029326105|63009R109|04269E957|856190953|00768Y727|23908L306|126349AF6|73937B589|" +
				"516806956|683797104|902973954|600544950|15671L909|00724F951|292104106|00847X904|219350955|67401P958|" +
				"902641752|50218P957|00739L901|06746P903|92189F953|G47567905|06740P650|13123X952|38173M952|29359T102|" +
				"229663959|33734E103|118230951|883556102|689648103|97382A900|808194954|60649T957|13645T900|075896950|" +
				"29266S956|80105N905|032332904|73935X951|73935B955|464288125|87612G901|39945C909|97717X957|14575E105|" +
				"75605A702|G39637955|029326105|63009R109|04269E957|856190953|00768Y727|23908L306|126349AF6|73937B589|" +
				"516806956|683797104|902973954|600544950|15671L909|00724F951|292104106|00847X904|219350955|67401P958|" +
				"902641752|50218P957|00739L901|06746P903|92189F953|G47567905|06740P650|13123X952|38173M952|29359T102|" +
				"229663959|33734E103|118230951|883556102|689648103|97382A900|808194954|60649T957|13645T900|075896950|" +
				"29266S956|80105N905|032332904|73935X951|73935B955|464288125|87612G901|39945C909|97717X957|14575E105|";
		final String inputs[] = input.split("\\|");

		for (int i = 0; i < inputs.length; i++)
			analysis.train(inputs[i]);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getRegExp(), "[\\p{IsAlphabetic}\\d]{9}");
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getMinLength(), 9);
		Assert.assertEquals(result.getMaxLength(), 9);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++)
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
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
