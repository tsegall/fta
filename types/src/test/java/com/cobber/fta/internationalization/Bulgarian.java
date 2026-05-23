/*
 * Copyright 2017-2025 Tim Segall
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
package com.cobber.fta.internationalization;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.testng.annotations.Test;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.Sample;
import com.cobber.fta.TestGroups;
import com.cobber.fta.TestSupport;
import com.cobber.fta.TestUtils;
import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.dates.DateTimeParser;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;

public class Bulgarian {

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void basicBulgarianDate() throws IOException, FTAException {

		final Set<String> samples = new HashSet<>();
		LocalDate localDate = LocalDate.now();

		if (!TestUtils.isValidLocale("bg_BG"))
			return;

		final Locale bulgarian = Locale.forLanguageTag("bg-BG");

		final TextAnalyzer analysis = new TextAnalyzer("basicBulgarianDate");
		analysis.configure(TextAnalyzer.Feature.COLLECT_STATISTICS, false);
		analysis.setLocale(bulgarian);

		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy", bulgarian);

		int locked = -1;

		for (int i = 0; i < 100; i++) {
			final String sample = localDate.format(formatter);
			samples.add(sample);
			if (analysis.train(sample) && locked == -1)
				locked = i;
			localDate = localDate.minusDays(100);
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		// Post Java 8 the month abbreviations have changed
		assertEquals(result.getRegExp(), TestUtils.getJavaVersion() == 8 ? "\\d{1,2} \\p{IsAlphabetic}{1,4} \\d{4}" : "\\d{1,2} \\p{IsAlphabetic}{3,4} \\d{4}");
		assertEquals(locked, AnalysisConfig.DETECT_WINDOW_DEFAULT);
		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "d MMM yyyy");
		assertEquals(result.getSampleCount(), samples.size());
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), samples.size());
		assertEquals(result.getNullCount(), 0);
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		// Even the UNK match the RE
		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()), sample);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void bulgarianddMMyyyy() throws FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("bulgarian");
		final Locale locale = Locale.forLanguageTag("bg-BG");
		analysis.setLocale(locale);
		final String[] inputs = {
				"14.02.2017г.", "10.01.2017г.", "02.02.2017г.", "07.02.2017г.", "16.02.2017г.",
				"28.02.2017г.", "01.03.2017г.", "23.03.2017г.", "28.03.2017г.", "30.03.2017г.",
				"04.04.2017г.", "06.04.2017г.", "11.04.2017г.", "18.04.2017г.", "20.04.2017г.",
				"20.04.2047г.", "20.04.2017г.", "25.04.2017г.", "02.05.2017г.", "03.05.2017г.",
				"04.05.2017г.", "22.05.2017г.", "05.06.2017г.", "06.06.2017г.", "08.06.2017г.",
				"15.06.2017г.", "18.07.2017г.", "25.07.2017г.", "03.08.2017г.", "10.08.2017г.",
				"18.08.2017г.", "21.08.2017г.", "04.09.2017г.", "07.09.2017г.", "21.09.2017г.",
				"26.09.2017г.", "28.09.2017г.", "12.10.2017г.", "17.10.2017г.", "24.10.2017г.",
				"26.10.2017г."
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "dd.MM.yyyy'г.'");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}\\.\\d{2}\\.\\d{4}г\\.");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
			assertNull(TestUtils.checkParseable(result, input, locale));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void bulgariandNonDate() throws FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("bulgarian");
		analysis.setLocale(Locale.forLanguageTag("bg-BG"));
		final String[] inputs = {
				"A12/1174/16", "A12/1175/16", "A12/1176/16", "A12/1177/16", "A12/1178/16",
				"A12/1179/16", "A12/1180/16", "A12/1181/16", "A12/1182/16", "A12/1183/16",
				"A12/1184/16", "A12/1185/16", "A12/1186/16", "A12/1187/16", "A11/0086/16",
				"INFO/0139/16", "A12/0745/16", "A12/1120/16", "A12/1117/16", "A12/1115/16",
				"A12/1112/16", "A12/1111/16", "A12/1116/16", "A12/1110/16", "A11/0085/16",
				"INFO/0135/16", "INFO/0136/16", "INFO/0137/16", "A12/1137/16", "A12/1138/16"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.STRING);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void bulgarianddMMyy() throws FTAException {
		final Locale bulgarian = Locale.forLanguageTag("bg-BG");
		final TextAnalyzer analysis = new TextAnalyzer("bulgarian", DateResolutionMode.DayFirst);
		analysis.setLocale(bulgarian);
		final String[] inputs = {
				"26.01.12г.", "05.04.05г.", "10.05.10г.", "05.04.05г.", "17.06.10г.",
				"05.04.05г.", "10.06.08г.", "05.04.05г.", "26.04.07г.", "05.04.05г.",
				"09.06.06г.", "05.04.05г.", "25.10.11г.", "26.09.06г.", "06.04.05г.",
				"26.06.12г.", "17.03.09г.", "06.04.05г.", "07.04.05г.", "27.10.09г.",
				"07.04.05г.", "26.02.08г.", "07.04.05г.", "06.07.10г.", "07.04.05г.",
				"15.06.11г.", "03.02.17г.", "12.08.16г.", "07.04.05г.", "08.04.05г.",
		};

		final DateTimeParser dtp = new DateTimeParser().withLocale(bulgarian).withDateResolutionMode(DateResolutionMode.DayFirst);
		for (final String input : inputs) {
			assertEquals(dtp.determineFormatString(input), "dd.MM.yy'г.'");
			analysis.train(input);
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "dd.MM.yy'г.'");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}\\.\\d{2}\\.\\d{2}г\\.");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()), input);
			assertNull(TestUtils.checkParseable(result, input, bulgarian));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void genderBG() throws IOException, FTAException {
		final String[] inputs = {
				"ЖЕНСКИ", "МЪЖКИ", "ЖЕНСКИ", "ЖЕНСКИ", "МЪЖКИ", "МЪЖКИ", "ЖЕНСКИ", "МЪЖКИ", "ЖЕНСКИ", "МЪЖКИ",
				"ЖЕНСКИ", "МЪЖКИ", "ЖЕНСКИ", "МЪЖКИ", "ЖЕНСКИ", "МЪЖКИ", "ЖЕНСКИ", "ЖЕНСКИ", "МЪЖКИ", "МЪЖКИ",
				"ЖЕНСКИ", "МЪЖКИ", "ЖЕНСКИ", "МЪЖКИ", "ЖЕНСКИ", "МЪЖКИ", "ЖЕНСКИ", "МЪЖКИ", "ЖЕНСКИ", "МЪЖКИ",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "пол", Locale.forLanguageTag("bg-BG"), "GENDER.TEXT_<LANGUAGE>", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}
}
