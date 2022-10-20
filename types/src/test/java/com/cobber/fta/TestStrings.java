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
package com.cobber.fta;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.plugins.CountryEN;

public class TestStrings {
	private static final SecureRandom random = new SecureRandom();
	private Logger logger = LoggerFactory.getLogger("com.cobber.fta");

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void manyNulls() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("manyNulls");
		final int iterations = 50;

		for (int i = 0; i < iterations; i++) {
			analysis.train(null);
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), iterations);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), 0);
		assertEquals(result.getNullCount(), iterations);
		assertEquals(result.getMinLength(), 0);
		assertEquals(result.getMaxLength(), 0);
		assertEquals(result.getMinValue(), null);
		assertEquals(result.getMaxValue(), null);
		assertEquals(result.getNullCount(), iterations);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_NULL));
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeModifier(), "NULL");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void manyBlanks() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("manyBlanks");
		final int iterations = 50;

		analysis.train("");
		for (int i = 0; i < iterations; i++) {
			analysis.train(" ");
			analysis.train("  ");
			analysis.train("      ");
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), 3 * iterations + 1);
		assertEquals(result.getMaxLength(), 6);
		assertEquals(result.getMinLength(), 1);
		assertEquals(result.getMaxValue(), "      ");
		assertEquals(result.getMinValue(), " ");
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getMatchCount(), 0);
		assertEquals(result.getBlankCount(), 3 * iterations + 1);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_BLANK));
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeModifier(), "BLANK");

		assertTrue("".matches(result.getRegExp()));
		assertTrue(" ".matches(result.getRegExp()));
		assertTrue("  ".matches(result.getRegExp()));
		assertTrue("      ".matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void sameBlanks() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("sameBlanks");
		final int iterations = 50;

		for (int i = 0; i < iterations; i++) {
			analysis.train("      ");
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), iterations);
		assertEquals(result.getMaxLength(), 6);
		assertEquals(result.getMinLength(), 6);
		assertEquals(result.getMaxValue(), "      ");
		assertEquals(result.getMinValue(), "      ");
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getMatchCount(), 0);
		assertEquals(result.getBlankCount(), iterations);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_BLANK));
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeModifier(), "BLANK");

		assertTrue("      ".matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void justEmpty() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("justEmpty");
		final int iterations = 50;

		for (int i = 0; i < iterations; i++) {
			analysis.train("");
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), iterations);
		assertEquals(result.getTypeModifier(), "BLANK");
		assertEquals(result.getMaxLength(), 0);
		assertEquals(result.getMinLength(), 0);
		assertEquals(result.getMaxValue(), "");
		assertEquals(result.getMinValue(), "");
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getMatchCount(), 0);
		assertEquals(result.getBlankCount(), iterations);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_BLANK));
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.STRING);

		assertTrue("".matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void whiteSpace() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("field,value");
		final String pipedInput = "| |  |   |    |     |      |       |        |         |";
		final String inputs[] = pipedInput.split("\\|");

		analysis.train(null);
		for (final String input : inputs)
			analysis.train(input);

		analysis.train(null);
		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getTypeModifier(), "BLANKORNULL");
		assertEquals(result.getSampleCount(), 22);
		assertEquals(result.getMatchCount(), 0);
		assertEquals(result.getNullCount(), 2);
		assertEquals(result.getMinLength(), 1);
		assertEquals(result.getMaxLength(), 9);
		assertEquals(result.getBlankCount(), 20);
		assertEquals(result.getRegExp(), analysis.getRegExp(KnownTypes.ID.ID_BLANKORNULL));
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void employeeNumber() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("employeeNumber");
		final String pipedInput = "||||||||||||||||||||" +
				"||||||||||||48|72|242|242|242|335|354|355|355|" +
				"397|460|567|616|616|70|70|865|1023|1023|1023|1023|1023|1023|1023|1023|1161|1161|1161|1161|1161|" +
				"1260|1273|1273|1273|136|136|136|136|136|136|136|136|136|1422|1422|1422|1422|1422|1422|1548|1652|" +
				"F9442559|F9442774|F9442774|F9442856|F9442856|F9442856|F9442856|F9442856|F9442965|F9442965|" +
				"F9442965|F9442968|F9442999|F9442999|FN228446|FN241214|FN241227|FN246235|FN246286|FN246288|" +
				"FN270164|FN270168|FN273815|FN273967|FN295633|FN295655|FN295657|FN295659|FN295684|FN295688|" +
				"FN295842|FN9441020|FN9441048|FN9441064|FN9441082|FN9441138|FN9441189|FN9441244|FN9441246|FN9441248|" +
				"FN9441330|FN9441334|FN9441383|FN9441501|FN9441505|FN9441516|FN9441529|FN9441680|FN9441695|FN9441804|";
		final String inputs[] = pipedInput.split("\\|");

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length - result.getBlankCount());
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getMinLength(), 2);
		assertEquals(result.getMaxLength(), 9);
		assertEquals(result.getBlankCount(), 32);
		assertEquals(result.getRegExp(), KnownTypes.PATTERN_ALPHANUMERIC + "{2,9}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getName(), "employeeNumber");

		int matchCount = 0;
		for (final String input : inputs) {
			if (input.matches(result.getRegExp()))
				matchCount++;
		}
		assertEquals(matchCount, result.getMatchCount());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void variableLengthString() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("variableLengthString");
		final String[] inputs = "Hello World|Hello|H|Z|A".split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), KnownTypes.freezeANY(1, 11, 1, 11, result.getLeadingWhiteSpace(), result.getTrailingWhiteSpace(), result.getMultiline()));
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getMinValue(), "A");
		assertEquals(result.getMaxValue(), "Z");
		assertEquals(result.getMinLength(), 1);
		assertEquals(result.getMaxLength(), 11);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()), input);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void variableLengthStringSimpleVsBulk() throws IOException, FTAException {
		for (int r = 0; r < 100; r++) {
			final TextAnalyzer analysis = new TextAnalyzer("variableLengthStringSimpleVsBulk_1");
			final String[] inputs = "HelloWorld|Hello|H|Z|A|Do|Not|Ask|What|You|Can|Do".split("\\|");

			final int ITERATIONS = 100;

			int locked = -1;

			for (int iters = 0; iters < ITERATIONS; iters++) {
				for (int i = 0; i < inputs.length; i++) {
					if (analysis.train(inputs[i]) && locked != -1)
						locked = i;
				}
			}

			TextAnalysisResult result = analysis.getResult();

			assertEquals(result.getSampleCount(), inputs.length * ITERATIONS);
			assertEquals(result.getNullCount(), 0);
			assertEquals(result.getRegExp(), "(?i)(A|ASK|CAN|DO|H|HELLO|HELLOWORLD|NOT|WHAT|YOU|Z)");
			assertEquals(result.getMatchCount(), inputs.length * ITERATIONS);
			assertEquals(result.getConfidence(), 1.0);
			assertEquals(result.getType(), FTAType.STRING);
			assertEquals(result.getMinValue(), "A");
			assertEquals(result.getMaxValue(), "Z");
			assertEquals(result.getMinLength(), 1);
			assertEquals(result.getMaxLength(), 10);

			for (final String input : inputs)
				assertTrue(input.matches(result.getRegExp()));

			final Map<String,Long> details = result.getCardinalityDetails();
			details.putAll(result.getOutlierDetails());
			final TextAnalyzer analysisBulk = new TextAnalyzer("variableLengthStringSimpleVsBulk_2");
			analysisBulk.trainBulk(details);
			result = analysisBulk.getResult();

			assertEquals(result.getSampleCount(), inputs.length * ITERATIONS);
			assertEquals(result.getNullCount(), 0);
			if (!result.getRegExp().equals("(?i)(A|ASK|CAN|DO|H|HELLO|HELLOWORLD|NOT|WHAT|YOU|Z)")) {
					for (final Map.Entry<String, Long> entry : result.getShapeDetails().entrySet())
						logger.debug("%s: %d", entry.getKey(), entry.getValue());
					for (final Map.Entry<String, Long> entry : result.getCardinalityDetails().entrySet())
						logger.debug("%s: %d", entry.getKey(), entry.getValue());
			}
			assertEquals(result.getMatchCount(), inputs.length * ITERATIONS);
			assertEquals(result.getConfidence(), 1.0);
			assertEquals(result.getType(), FTAType.STRING);
			assertEquals(result.getMinValue(), "A");
			assertEquals(result.getMaxValue(), "Z");
			assertEquals(result.getMinLength(), 1);
			assertEquals(result.getMaxLength(), 10);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void testEnumsWithSpecialChars() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("ShipRegion");
		final String[] inputs = {
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

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "(?i)(AK|BC|CO\\. CORK|DF|ESSEX|ID|ISLE OF WIGHT|LARA|NM|NUEVA ESPARTA|OR|QUÉBEC|RJ|SP|TÁCHIRA|WA|WY)");
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getMinValue(), "AK");
		assertEquals(result.getMaxValue(), "WY");
		assertEquals(result.getMinLength(), 2);
		assertEquals(result.getMaxLength(), 13);

// BUG/TODO
//		for (int i = 0; i < inputs.length; i++) {
//			assertTrue(inputs[i].matches(result.getRegExp()), inputs[i]);
//		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void manyConstantLengthStrings() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("manyConstantLengthStrings");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final int nullIterations = 50;
		final int iterations = 2 * AnalysisConfig.MAX_CARDINALITY_DEFAULT;;
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

		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getSampleCount(), iterations + nullIterations);
		assertEquals(result.getCardinality(), AnalysisConfig.MAX_CARDINALITY_DEFAULT);
		assertEquals(result.getNullCount(), nullIterations);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), KnownTypes.PATTERN_ALPHA + "{12}");
		assertEquals(result.getConfidence(), 1.0);

		// Now check that we achieve the same outcome but using trainBulk() instead of train()
		final Map<String, Long> details = new HashMap<>(result.getCardinalityDetails());
		details.put(null, result.getNullCount());
		final long sum = details.values().stream().collect(Collectors.summingLong(Long::longValue));
		final TextAnalyzer analysisBulk = new TextAnalyzer("manyConstantLengthStrings_bulk");
		analysisBulk.trainBulk(details);
		result = analysisBulk.getResult();

		assertEquals(result.getSampleCount(), sum);
		assertEquals(result.getCardinality(), AnalysisConfig.MAX_CARDINALITY_DEFAULT);
		assertEquals(result.getNullCount(), nullIterations);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), KnownTypes.PATTERN_ALPHA + "{12}");
		assertEquals(result.getConfidence(), 1.0);

	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void constantNoise() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("NOISE");
		analysis.setTrace("enabled=true");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String[] inputs = {
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
			for (final String sample : inputs)
				analysis.train(sample);
		}

		TextAnalysisResult result = analysis.getResult();
		result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length * iterations);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), "[\\p{IsAlphabetic}\\d]{32}");
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void testMix() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("CUSIP");
		final String pipedInput =
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
		final String inputs[] = pipedInput.split("\\|");

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), "[\\p{IsAlphabetic}\\d]{9}");
		assertNull(result.getTypeModifier());
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getMinLength(), 9);
		assertEquals(result.getMaxLength(), 9);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.getMean());
		assertNull(result.getStandardDeviation());

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void niceEnum() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("niceEnum");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String[] inputs = { "Corrective", "Discretionary", "Marketing/Retention", "Reactivation(FS Only)", "Undefined" };
		final int iterations = 10;

		for (int i = 0; i < iterations; i++) {
			for (final String sample : inputs)
				analysis.train(sample);
		}

		TextAnalysisResult result = analysis.getResult();
		result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length * iterations);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), "(?i)(CORRECTIVE|DISCRETIONARY|MARKETING/RETENTION|\\QREACTIVATION(FS ONLY)\\E|UNDEFINED)");
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void nastyEnum() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("nastyEnum");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String[] inputs = {
				"Skipping locale 'hi_IN' as it does not use Arabic numerals.",
				"Skipping locale 'th_TH_TH_#u-nu-thai' as it does not use Arabic numerals.",
				"Skipping locale 'ja_JP_JP_#u-ca-japanese' as it does not use the Gregorian calendar.",
				"Skipping locale 'mk_MK' as it has trailing neg suffix." };
		final int iterations = 10;

		for (int i = 0; i < iterations; i++) {
			for (final String sample : inputs)
				analysis.train(sample);
		}

		TextAnalysisResult result = analysis.getResult();
		result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length * iterations);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), ".{54,84}");
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void nastyEnumsTwo() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("nastyEnumsTwo");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String[] inputs = {
				"Fighter",
				"Not a Fighter",
				"Would like to be a Fighter",
				"Hates Fighting",
				"Fighter; WANNABE"};
		final int iterations = 10;

		for (int i = 0; i < iterations; i++) {
			for (final String sample : inputs)
				analysis.train(sample);
		}

		TextAnalysisResult result = analysis.getResult();
		result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length * iterations);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), "(?i)(FIGHTER|FIGHTER; WANNABE|HATES FIGHTING|NOT A FIGHTER|WOULD LIKE TO BE A FIGHTER)");
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void testCompressUSD() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("testCompressUSD");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String[] inputs = {
				"$411.00", "$420.00", "$407.00", "$80.00", "$453.00", "$401.00", "$490.00", "$430.00", "$4.00", "$40.00",
				"$830.00", "$411.00", "$420.00", "$407.00", "$80.00", "$453.00", "$401.00", "$490.00", "$430.00", "$4.00"
		};
		final int iterations = 10;

		for (int i = 0; i < iterations; i++) {
			for (final String sample : inputs)
				analysis.train(sample);
		}

		TextAnalysisResult result = analysis.getResult();
		result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length * iterations);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), "\\$\\d+\\.\\d+");
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs)
			assertTrue(input.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void testCompressGBP() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("testCompressGBP");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String[] inputs = {
				"£41.99", "£51.99", "£28.56", "£7.82", "£9.78", ""
		};
		final int iterations = 1;

		for (int i = 0; i < iterations; i++) {
			for (final String sample : inputs)
				analysis.train(sample);
		}

		TextAnalysisResult result = analysis.getResult();
		result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length * iterations);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 1);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), "£\\d+\\.\\d+");
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			if (input.length() != 0)
				assertTrue(input.matches(result.getRegExp()), result.getRegExp());
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void testPercent() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("testPercent");
		assertTrue(analysis.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS));
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		assertFalse(analysis.isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS));
		final String pipedInput =
				"14%|27%|11%|26%|29%|25%|21%|25%|21%|0%|4%|14%|25%|17%|26%|20%|20%|6%|25%|10%|1%|25%|21%|22%|9%|6%|17%|15%|2%|25%|5%|15%|22%|5%|10%|14%|24%|9%|13%|6%|22%|7%|14%|25%|1%|6%|3%|10%|11%|23%|3%|18%|4%|5%|29%|5%|9%|4%|22%|26%|10%|27%|8%|6%|15%|3%|19%|7%|11%|22%|16%|23%|18%|8%|13%|11%|27%|5%|20%|10%|11%|20%|19%|9%|30%|12%|22%|10%|11%|5%|30%|8%|24%|22%|10%|9%|11%|2%|2%|26%|23%|0%|8%|18%|23%|18%|27%|23%|12%|11%|24%|25%|6%|9%|5%|6%|20%|15%|8%|12%|1%|20%|2%|10%|12%|7%|20%|29%|25%|15%|13%|28%|30%|2%|29%|27%|24%|18%|18%|21%|20%|20%|25%|28%|13%|2%|17%|27%|5%|8%|27%|15%|26%|0%|24%|23%|8%|9%|14%|9%|13%|23%|16%|4%|1%|25%|26%|25%|7%|20%|9%|24%|19%|0%|30%|25%|16%|13%|16%|0%|17%|22%|26%|24%|9%|19%|29%|5%|11%|27%|1%|15%|21%|8%|2%|3%|30%|5%|7%|16%|18%|6%|25%|21%|11%|9%|18%|19%|2%|22%|27%|8%|26%|21%|14%|13%|29%|29%|9%|8%|23%|26%|23%|22%|4%|13%|28%|17%|27%|30%|11%|4%|12%|20%|29%|2%|28%|12%|28%|17%|18%|11%|12%|9%|2%|2%|30%|18%|9%|10%|19%|20%|14%|3%|24%|4%|10%|21%|25%|2%|21%|6%|18%|27%|6%|11%|13%|19%|9%|9%|7%|23%|18%|24%|26%|23%|8%|26%|21%|13%|28%|7%|22%|30%|16%|17%|14%|27%|24%|12%|16%|11%|15%|23%|4%|26%|15%|24%|2%|29%|16%|25%|26%|10%|18%|12%|1%|9%|13%|27%|27%|2%|9%|1%|11%|30%|30%|4%|2%|3%|0%|30%|26%|2%|10%|15%|9%|6%|26%|10%|20%|23%|21%|28%|0%|14%|5%|18%|22%|17%|20%|2%|26%|21%|0%|5%|9%|7%|25%|22%|27%|29%|17%|26%|17%|26%|6%|10%|10%|4%|25%|11%|29%|19%|23%|14%|1%|10%|26%|23%|20%|7%|28%|10%|25%|14%|17%|30%|11%|13%|3%|7%|5%|19%|30%|13%|14%|11%|4%|7%|18%|24%|26%|10%|22%|9%|24%|12%|5%|2%|4%|10%|21%|30%|9%|17%|4%|15%|15%|29%|10%|6%|27%|18%|7%|7%|24%|3%|10%|7%|5%|1%|8%|8%|17%|19%|24%|11%|24%|19%|22%|26%|17%|21%|17%|13%|24%|20%|1%|0%|10%|12%|29%|7%|4%|20%|10%|30%|15%|8%|28%|4%|11%|21%|22%|25%|12%|19%|27%|16%|25%|1%|29%|26%|21%|8%|2%|7%|13%|18%|25%|3%|18%|25%|0%|5%|29%|11%|23%|30%|19%|10%|0%|23%|22%|2%|5%|16%|30%|24%|26%|12%|28%|16%|4%|4%|0%|14%|30%|1%|7%|5%|24%|30%|26%|17%|9%|20%|10%|19%|24%|12%|24%|23%|13%|22%|8%|3%|29%|18%|24%|15%|13%|21%|3%|1%|18%|30%|2%|20%|3%|17%|9%|12%|5%|23%|18%|2%|18%|12%|9%|10%|22%|12%|13%|27%|28%|21%|1%|22%|9%|4%|11%|4%|12%|0%|11%|13%|17%|13%|13%|9%|5%|12%|18%|3%|9%|18%|11%|22%|21%|30%|28%|13%|25%|4%|3%|3%|8%|9%|3%|24%|15%|10%|7%|4%|17%|0%|25%|20%|11%|22%|18%|23%|0%|7%|4%|7%|13%|21%|22%|28%|11%|2%|5%|23%|11%|23%|11%|28%|8%|7%|14%|6%|27%|10%|28%|27%|21%|12%|13%|18%|7%|0%|2%|14%|11%|22%|0%|20%|26%|4%|5%|3%|12%|12%|4%|8%|20%|14%|22%|19%|21%|5%|3%|27%|20%|9%|30%|25%|17%|24%|7%|5%|15%|12%|13%|25%|1%|15%|7%|10%|26%|5%|7%|19%|17%|16%|25%|26%|29%|10%|21%|21%|10%|22%|4%|24%|5%|12%|29%|10%|29%|26%|9%|8%|12%|2%|6%|6%|25%|7%|26%|4%|21%|27%|13%|29%|19%|5%|8%|13%|16%|18%|26%|16%|29%|17%|17%|22%|20%|23%|9%|25%|20%|7%|19%|24%|8%|5%|27%|2%|14%|18%|25%|0%|20%|13%|11%|14%|12%|30%|15%|28%|22%|10%|22%|16%|10%|10%|12%|21%|13%|17%|20%|21%|14%|19%|8%|20%|10%|27%|1%|24%|24%|6%|8%|24%|1%|0%|15%|17%|20%|18%|16%|5%|23%|24%|24%|2%|8%|22%|21%|17%|23%|28%|14%|11%|3%|19%|6%|24%|27%|10%|5%|24%|16%|23%|4%|13%|3%|22%|11%|20%|5%|20%|13%|4%|28%|1%|19%|18%|16%|11%|13%|6%|28%|1%|9%|7%|13%|28%|17%|2%|7%|22%|26%|14%|15%|27%|17%|26%|5%|24%|16%|4%|22%|26%|6%|22%|15%|9%|23%|16%|28%|28%|22%|17%|5%|2%|20%|21%|5%|5%|8%|13%|20%|4%|17%|6%|15%|8%|14%|28%|14%|10%|15%|13%|3%|26%|27%|25%|25%|22%|22%|6%|6%|1%|17%|17%|26%|5%|4%|5%|4%|1%|13%|22%|12%|24%|27%|13%|7%|17%|29%|16%|10%|15%|25%|29%|3%|8%|16%|10%|2%|12%|0%|3%|4%|12%|29%|2%|6%|14%|6%|2%|3%|5%|1%|28%|24%|25%|9%|18%|18%|11%|0%|20%|30%|14%|1%|3%|15%|23%|3%|5%|19%|17%|6%|18%|16%|19%|29%|0%|15%|4%|12%|25%|26%|7%|4%|28%|1%|12%|12%|0%|8%|12%|22%|4%|23%|11%|19%|8%|0%|0%|22%|7%|22%|10%|25%|2%|5%|11%|18%|9%|1%|10%|9%|24%|4%|26%|23%|17%|16%|20%|20%|4%|1%|9%|2%|29%|28%|12%|5%|27%|15%|21%|11%|14%|19%|27%|11%|29%|26%|10%|28%|25%|4%|6%|11%|11%|23%|21%|28%|25%|14%|23%|19%|12%|24%|23%|3%|28%|22%|20%|6%|9%|22%|12%|2%|22%|20%|10%|12%|21%|9%|4%|28%|15%|0%|28%|6%|0%|16%|14%|21%|17%|4%|26%|26%|5%|5%|10%|26%|0%|15%|10%|6%|0%|14%|25%|27%|29%|2%|26%|13%|19%|10%|14%|20%|6%|30%|19%|9%|2%|5%|23%|1%|22%|26%|3%|25%|27%|1%|24%|10%|2%|14%|19%|23%|10%|15%|20%|25%|0%|5%|19%|0%|5%|23%|28%|5%|27%|9%|14%|5%|1%|15%|";
		final String inputs[] = pipedInput.split("\\|");
		final int iterations = 1;

		for (int i = 0; i < iterations; i++) {
			for (final String sample : inputs)
				analysis.train(sample);
		}

		TextAnalysisResult result = analysis.getResult();
		result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length * iterations);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), "\\d%|\\d{2}%");
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			if (input.length() != 0)
				assertTrue(input.matches(result.getRegExp()), result.getRegExp());
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void testTwoContainedStrings() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("testTwoContainedStrings");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String pipedInput = "99501|45011|60632-4069|95111|57105|11953|90034|44023-1023|85013|53207|48180|61109-4069|19014|95111|66218-4069|10011|88011|10025|10011|93012-1023|78204|67410|97754|66204-4069|99708|33196|99712|53711|19132|" +
				"99501|45011|60632-4069|95111|57105|11953|90034|44023-1023|85013|53207|48180|61109-4069|19014|95111|66218-4069|10011|88011|10025|10011|93012-1023|78204|67410|97754|66204-4069|99708|33196|99712|53711|19132|" +
				"99501|45011|60632-4069|95111|57105|11953|90034|44023-1023|85013|53207|48180|61109-4069|19014|95111|66218-4069|10011|88011|10025|10011|93012-1023|78204|67410|97754|66204-4069|99708|33196|99712|53711|19132|" +
				"99501|45011|60632-4069|95111|57105|11953|90034|44023-1023|85013|53207|48180|61109-4069|19014|95111|66218-4069|10011|88011|10025|10011|93012-1023|78204|67410|97754|66204-4069|99708|33196|99712|53711|19132|";
		final int iterations = 1;
		final String inputs[] = pipedInput.split("\\|");
		for (int i = 0; i < iterations; i++) {
			for (final String sample : inputs)
				analysis.train(sample);
		}

		TextAnalysisResult result = analysis.getResult();
		result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length * iterations);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getShapeCount(), 2);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), "\\d{5}(-\\d{4})?");
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			if (input.length() != 0)
				assertTrue(input.matches(result.getRegExp()), result.getRegExp());
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void testNonLogicalStructureSignatures() throws IOException, FTAException {
		final TextAnalyzer analysis1 = new TextAnalyzer("Disposition");
		final TextAnalyzer analysis2 = new TextAnalyzer("Interaction");
		final String input1 = "HANDLED|HANDLED|HANDLED|HANDLED|INVALID_PRODUCT|HANDLED|HANDLED|HANDLED|DEFERRED|DEFERRED|HANDLED|DEFERRED|HANDLED_WITH_ISSUES|DEFERRED|INVALID_PRODUCT|HANDLED_WITH_ISSUES|HANDLED|HANDLED_WITH_ISSUES|HANDLED_WITH_ISSUES|FOLLOW_UP_REQUIRED|ESCALATED|HANDLED|HANDLED|HANDLED|HANDLED|HANDLED|DEFERRED|HANDLED|HANDLED|HANDLED|DEFERRED|HANDLED|DEFERRED|HANDLED|DEFERRED|INVALID_PRODUCT|HANDLED|HANDLED|INVALID_PRODUCT|ESCALATED|";
		final String input2 = "HANDLED|INVALID_PRODUCT|ESCALATED|HANDLED|HANDLED|HANDLED|HANDLED|ESCALATED|ESCALATED|ESCALATED|HANDLED|HANDLED|DEFERRED|HANDLED|HANDLED|HANDLED_WITH_ISSUES|DEFERRED|INVALID_PRODUCT|HANDLED_WITH_ISSUES|HANDLED|HANDLED_WITH_ISSUES|FOLLOW_UP_REQUIRED|ESCALATED|HANDLED|HANDLED|HANDLED|HANDLED|ESCALATED|HANDLED|DEFERRED|HANDLED|HANDLED|ESCALATED|ESCALATED|HANDLED|DEFERRED|HANDLED|DEFERRED|HANDLED|DEFERRED|INVALID_PRODUCT|HANDLED|HANDLED|INVALID_PRODUCT|HANDLED_WITH_ISSUES|";

		final String inputs1[] = input1.split("\\|");
		final String inputs2[] = input2.split("\\|");
		for (final String sample : inputs1)
			analysis1.train(sample);

		for (final String sample : inputs2)
			analysis2.train(sample);

		final TextAnalysisResult result1 = analysis1.getResult();
		final TextAnalysisResult result2 = analysis2.getResult();
		assertEquals(result1.getStructureSignature(), result2.getStructureSignature());
		assertNotEquals(result1.getDataSignature(), result2.getDataSignature());
		assertEquals(result1.getRegExp(), result2.getRegExp());
		assertEquals(result1.getRegExp(), "(?i)(DEFERRED|ESCALATED|FOLLOW_UP_REQUIRED|HANDLED|HANDLED_WITH_ISSUES|INVALID_PRODUCT)");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void testLogicalStructureSignatures() throws IOException, FTAException {
		final TextAnalyzer analysis1 = new TextAnalyzer("Disposition");
		final TextAnalyzer analysis2 = new TextAnalyzer("Interaction");
		final String input1 = "England|Australia|New Zealand|India|Pakistan|China|Brazil|France|Germany|Israel|Jordan|Lebanon|Malawi|";
		final String input2 = "Belgium|UK|Ireland|Canada|France|Germany|Austria|Hungary|Poland|Russia|Ukraine|Croatia|Norway|Sweden|";

		final String inputs1[] = input1.split("\\|");
		final String inputs2[] = input2.split("\\|");
		for (final String sample : inputs1)
			analysis1.train(sample);

		for (final String sample : inputs2)
			analysis2.train(sample);

		final TextAnalysisResult result1 = analysis1.getResult();
		final TextAnalysisResult result2 = analysis2.getResult();

		assertEquals(result1.getStructureSignature(), PluginDefinition.findByQualifier("COUNTRY.TEXT_EN").signature);
		assertEquals(result1.getStructureSignature(), result2.getStructureSignature());
		assertNotEquals(result1.getDataSignature(), result2.getDataSignature());
		assertEquals(result1.getSemanticType(), result2.getSemanticType());
		assertEquals(result1.getSemanticType(), CountryEN.SEMANTIC_TYPE);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void testCompressCoordinates() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("testCompressCoordinates");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String[] inputs = {
				"-69.97345,12.51678", "66.00845,33.83627", "17.53646,12.29118",
				"-63.06082,18.22560", "20.05399,41.14258", "20.03715,60.20733"
		};
		final int iterations = 3;

		for (int i = 0; i < iterations; i++) {
			for (final String sample : inputs)
				analysis.train(sample);
		}

		TextAnalysisResult result = analysis.getResult();
		result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length * iterations);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getSemanticType(), "COORDINATE_PAIR.DECIMAL");
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			if (input.length() != 0)
				assertTrue(input.matches(result.getRegExp()), result.getRegExp());
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void testCompressCoordinates2() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("testCompressCoordinates2");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String[] inputs = {
				"-38.3,142.4", "33.749,-84.4124",
				"36.6666,-119.834", "42.0834,-71.0184",
				"3.4195,99.1654", "38.4199,-117.122",
				"59.6167,16.55", "42.8198,-83.2366",
				"4.2884,98.0429", "39.0667,-83.0666",
				"28.95,77.2167", "7.16667,126.333",
				"28.0222,-81.7329", "19.4,-101.267",
				"29.2947,117.208", "6.32649,99.8432",
				"36.4643,-82.586", "-33.9622,18.4135",
				"40.754,-79.8101", "56.1,34.1",
				"-20.0637,30.8277"
		};
		final int iterations = 3;

		for (int i = 0; i < iterations; i++) {
			for (final String sample : inputs)
				analysis.train(sample);
		}

		TextAnalysisResult result = analysis.getResult();
		result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length * iterations);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getSemanticType(), "COORDINATE_PAIR.DECIMAL");
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			if (input.length() != 0)
				assertTrue(input.matches(result.getRegExp()), result.getRegExp());
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void testCompressCoordinates3() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("lat-lon");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String[] inputs = {
				"20.627527,-87.076686", "20.627567,-87.076718", "20.6267,-87.075866", "20.635482,-87.070345",
				"20.629092,-87.074658", "20.62158,-87.092398", "20.630723,-87.073113", "20.669436,-87.041784",
				"20.636356,-87.064015", "20.616003,-87.08558", "21.026917,-89.57469", "20.698689,-88.589284",
				"20.927007,-89.562144", "20.666667,-89.25", "20.997851,-89.564806", "21.036225,-89.622806",
				"20.666667,-89.25", "20.96681,-89.664374", "20.97,-89.62", "20.993882,-89.654744",
				"21.022035,-89.569943", "21.017202,-89.576877", "20.968765,-89.575109", "21.010511,-89.562531",
				"20.989657,-89.586893", "20.968422,-89.67354", "", "", "19.54761,-96.928985"
		};

		for (final String sample : inputs)
			analysis.train(sample);

		TextAnalysisResult result = analysis.getResult();
		result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 2);
		assertEquals(result.getSemanticType(), "COORDINATE_PAIR.DECIMAL");
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			if (input.length() != 0)
				assertTrue(input.matches(result.getRegExp()), result.getRegExp());
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void testCompressCoordinates4() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("loc");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String[] inputs = {
				"(51.426177000000003, -2.58955)", "(51.411724999999997, -2.6098059999999998)", "(51.473087, -2.6035400000000002)",
				"(51.407119999999999, -2.5915240000000002)", "(51.414721999999998, -2.6210499999999999)",
				"(51.492756999999997, -2.6825899999999998)", "(51.460093999999998, -2.5694659999999998)",
				"(51.495963000000003, -2.649632)", "(51.484152999999999, -2.566719)", "(51.462553999999997, -2.5361630000000002)",
				"(51.444048000000002, -2.5403690000000001)", "(51.416970999999997, -2.566805)", "(51.416007, -2.5475789999999998)",
				"(51.434202999999997, -2.6147840000000002)", "(51.472231999999998, -2.52)", "(51.508037999999999, -2.621737)",
				"(51.456617999999999, -2.5249999999999999)", "(51.434257000000002, -2.5544449999999999)",
				"(51.432758999999997, -2.5686930000000001)", "(51.440463000000001, -2.5828549999999999)",
				"(51.463408999999999, -2.5583079999999998)", "(51.485061000000002, -2.5265499999999999)",
				"(51.494734000000001, -2.5876619999999999)", "(51.473087, -2.6035400000000002)", "(51.466189999999997, -2.583456)",
				"(51.474530999999999, -2.5471499999999998)", "(51.480358000000003, -2.5889489999999999)",
				"(51.490031999999999, -2.6219079999999999)", "(51.452018000000002, -2.5990769999999999)",
				"(51.485970000000002, -2.6072310000000001)", "(51.466884999999998, -2.6031970000000002)",
				"(51.473087, -2.6035400000000002)", "(51.479823000000003, -2.6368429999999998)",
				"(51.462446999999997, -2.6123810000000001)", "(51.455067, -2.6207919999999998)",
		};

		for (final String sample : inputs)
			analysis.train(sample);

		TextAnalysisResult result = analysis.getResult();
		result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getSemanticType(), "COORDINATE_PAIR.DECIMAL_PAREN");
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			if (input.length() != 0)
				assertTrue(input.matches(result.getRegExp()), result.getRegExp());
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void testCompressCoordinates5() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Location");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String[] inputs = {
				"(42.678066, -73.814233)", "(42.678066, -73.814233)", "(42.678066, -73.814233)", "(42.226801, -78.020567)",
				"(42.226801, -78.020567)", "(42.226801, -78.020567)", "(40.85589, -73.868294)", "(40.85589, -73.868294)",
				"(40.85589, -73.868294)", "(42.122015, -75.933191)", "(42.122015, -75.933191)", "(42.122015, -75.933191)",
				"(42.122015, -75.933191)", "(42.122015, -75.933191)", "(42.224267, -78.606673)", "(42.224267, -78.606673)",
				"(42.224267, -78.606673)", "(42.940095, -76.560755)", "(42.940095, -76.560755)", "(42.940095, -76.560755)",
				"(42.940095, -76.560755)", "(42.246904, -79.315313)", "(42.246904, -79.315313)"
		};

		for (final String sample : inputs)
			analysis.train(sample);

		TextAnalysisResult result = analysis.getResult();
		result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getSemanticType(), "COORDINATE_PAIR.DECIMAL_PAREN");
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			if (input.length() != 0)
				assertTrue(input.matches(result.getRegExp()), result.getRegExp());
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void testCompressCoordinates6() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Location");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String[] inputs = {
				"(28.0, 3.0)", "(28.0, 3.0)", "(28.0, 3.0)", "(28.0, 3.0)", "(28.0, 3.0)", "(28.0, 3.0)", "(28.0, 3.0)", "(28.0, 3.0)", "(28.0, 3.0)", "(28.0, 3.0)",
				"(28.0, 3.0)", "(28.0, 3.0)", "(28.0, 3.0)", "(28.0, 3.0)", "(42.5, 1.6)", "(42.5, 1.6)", "(42.5, 1.6)", "(42.5, 1.6)", "(42.5, 1.6)", "(42.5, 1.6)",
				"(42.5, 1.6)", "(42.5, 1.6)", "(42.5, 1.6)", "(42.5, 1.6)", "(42.5, 1.6)", "(42.5, 1.6)", "(42.5, 1.6)", "(42.5, 1.6)", "(46.0, 2.0)", "(46.0, 2.0)",
				"(46.0, 2.0)", "(46.0, 2.0)", "(46.0, 2.0)", "(46.0, 2.0)", "(46.0, 2.0)", "(46.0, 2.0)", "(46.0, 2.0)", "(46.0, 2.0)", "(46.0, 2.0)", "(46.0, 2.0)",
				"(46.0, 2.0)", "(46.0, 2.0)", "(51.0, 9.0)", "(51.0, 9.0)", "(51.0, 9.0)", "(51.0, 9.0)", "(51.0, 9.0)", "(51.0, 9.0)", "(51.0, 9.0)", "(51.0, 9.0)",
				"(51.0, 9.0)", "(51.0, 9.0)", "(51.0, 9.0)", "(51.0, 9.0)", "(51.0, 9.0)", "(51.0, 9.0)", "(16.0, 8.0)", "(16.0, 8.0)", "(16.0, 8.0)", "(16.0, 8.0)",
				"(16.0, 8.0)", "(16.0, 8.0)", "(16.0, 8.0)", "(16.0, 8.0)", "(16.0, 8.0)", "(16.0, 8.0)", "(16.0, 8.0)", "(16.0, 8.0)", "(16.0, 8.0)", "(16.0, 8.0)",
				"(10.0, 8.0)", "(10.0, 8.0)", "(10.0, 8.0)", "(10.0, 8.0)", "(10.0, 8.0)", "(10.0, 8.0)", "(10.0, 8.0)", "(10.0, 8.0)", "(10.0, 8.0)", "(10.0, 8.0)",
				"(10.0, 8.0)", "(10.0, 8.0)", "(10.0, 8.0)", "(10.0, 8.0)", "(47.0, 8.0)", "(47.0, 8.0)", "(47.0, 8.0)", "(47.0, 8.0)", "(47.0, 8.0)", "(47.0, 8.0)",
				"(47.0, 8.0)", "(47.0, 8.0)", "(47.0, 8.0)", "(47.0, 8.0)", "(47.0, 8.0)", "(47.0, 8.0)", "(47.0, 8.0)", "(47.0, 8.0)", "(34.0, 9.0)", "(34.0, 9.0)",
				"(34.0, 9.0)", "(34.0, 9.0)", "(34.0, 9.0)", "(34.0, 9.0)", "(34.0, 9.0)", "(34.0, 9.0)", "(34.0, 9.0)", "(34.0, 9.0)", "(34.0, 9.0)", "(34.0, 9.0)"
		};

		for (final String sample : inputs)
			analysis.train(sample);

		TextAnalysisResult result = analysis.getResult();
		result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getSemanticType(), "COORDINATE_PAIR.DECIMAL_PAREN");
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getConfidence(), 1.0);

		for (final String input : inputs) {
			if (input.length() != 0)
				assertTrue(input.matches(result.getRegExp()), result.getRegExp());
		}
	}



	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void testBugPipe() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("testBugPipe");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		analysis.train("10010|");

		TextAnalysisResult result = analysis.getResult();
		result = analysis.getResult();

		assertEquals(result.getSampleCount(), 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), "\\d{5}\\|");
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void testTrimmedLength() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("testTrimmedLength");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String[] inputs = {
				"abc", "abcd ", "abcde  ",
				"abcdef   ", "abcdefg    ", "abcdefgh     ",
				"mno", "mnop ", "mnopq  ",
				"abcdef   ", "abcdefg    ", "abcdeifgh     ",
				"rst", "rstu ", "tstuv  ",
				"aaaaaaaaaa   ", "bbbbbbbbbbb    ", "ccccccccccccccccc     ",
		};

		for (final String sample : inputs)
			analysis.train(sample);

		TextAnalysisResult result = analysis.getResult();
		result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{3,17}[ 	]*");
		assertEquals(result.getMinLength(), 3);
		assertEquals(result.getMaxLength(), 22);
		assertEquals(result.getConfidence(), 1.0);

		for (final String sample : inputs)
			assertTrue(sample.matches(result.getRegExp()), result.getRegExp());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void testAlphaNumeric() throws IOException, FTAException {
		final int SAMPLE_COUNT = 100;
		final Set<String> samples = new HashSet<>();
		final TextAnalyzer analysis = new TextAnalyzer("SSN");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);

		for (int i = 0; i < SAMPLE_COUNT/2; i++) {
			String sample = String.format("%c%02d%c",
					'a' + random.nextInt(26), random.nextInt(100), 'a' + random.nextInt(26));
			samples.add(sample);
			analysis.train(sample);
			sample = String.format("%02d%c%c",
					random.nextInt(100), 'a' + random.nextInt(26), 'a' + random.nextInt(26));
			samples.add(sample);
			analysis.train(sample);
		}

		analysis.train("N/A");
		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), SAMPLE_COUNT + 1);
		assertEquals(result.getRegExp(), "[\\p{IsAlphabetic}\\d]{4}");
		assertNull(result.getTypeModifier());
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());

		for (final String sample : samples) {
			assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.STRINGS })
	public void testStrange() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("testStrange");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		final String[] inputs = {
				"PK__3214EC273319DEC5", "PK__3214EC273319DEC5 ", "PK__3214EC273319DEC5",
		};

		for (final String sample : inputs)
			analysis.train(sample);

		TextAnalysisResult result = analysis.getResult();
		result = analysis.getResult();

		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{2}__\\d{4}\\p{IsAlphabetic}{2}\\d{6}\\p{IsAlphabetic}{3}\\d[ 	]*");
		assertEquals(result.getMinLength(), 20);
		assertEquals(result.getMaxLength(), 21);
		assertEquals(result.getConfidence(), 1.0);

		for (final String sample : inputs)
			assertTrue(sample.matches(result.getRegExp()), result.getRegExp());
	}

	public void _stringPerf(final boolean statisticsOn) throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("testCompressUSD");
		if (!statisticsOn) {
			analysis.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);
			analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		}
		final long sampleCount = 100_000_000_000L;
		boolean saveOutput = false;
		BufferedWriter bw = null;
		String[] samples = new String[10000];

		if (saveOutput)
			bw = new BufferedWriter(new FileWriter("/tmp/stringPerf.csv"));

		final String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		final StringBuilder b = new StringBuilder(alphabet.length());
		for (int i = 0; i < samples.length; i++) {
			final int length = random.nextInt(alphabet.length()) + 1;

			b.setLength(0);
			for (int j = 0; j < length; j++)
				b.append(alphabet.charAt(Math.abs(random.nextInt()%alphabet.length())));
			samples[i] = b.toString();
		}

		final long start = System.currentTimeMillis();

		long iters = 0;
		// Run for about reasonable number of seconds
		final int seconds = 5;
		for (iters = 0; iters < sampleCount; iters++) {
			final String sample = samples[(int)(iters%samples.length)];
			analysis.train(sample);
			if (bw != null)
				bw.write(sample + '\n');
			if (iters%100 == 0 && System.currentTimeMillis()  - start >= seconds * 1_000)
				break;

		}
		final TextAnalysisResult result = analysis.getResult();
		if (bw != null)
			bw.close();

		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), iters + 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getMinLength(), 1);
		assertEquals(result.getMaxLength(), 52);
		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{1,52}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeModifier());
		logger.info("Count {}, duration: {}ms, ~{} per second.", iters + 1, System.currentTimeMillis() - start, (iters  + 1)/seconds);

		// With Statistics & LogicalTypes
		//   - Count 27154301, duration: 10003ms, ~2,715,430 per second
		// No Statistics & No LogicalTypes
		//   - Count 27322001, duration: 10003ms, ~2,732,200 per second
	}

	@Test(groups = { TestGroups.PERFORMANCE, TestGroups.STRINGS })
	public void stringPerf() throws IOException, FTAException {
		_stringPerf(true);
	}

	@Test(groups = { TestGroups.PERFORMANCE, TestGroups.STRINGS })
	public void stringPerfNoStatistics() throws IOException, FTAException {
		_stringPerf(false);
	}
}
