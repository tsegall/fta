/*
 * Copyright 2017-2026 Tim Segall
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

import java.io.IOException;
import java.util.Locale;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;

public class TestInternationalization {

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void genderCA() throws IOException, FTAException {
		final String[] inputs = {
				"DONA", "HOME", "DONA", "HOME", "HOME", "DONA", "HOME", "DONA", "DONA", "HOME",
				"DONA", "HOME", "HOME", "DONA", "HOME", "DONA", "HOME", "DONA", "HOME", "HOME",
				"DONA", "HOME", "DONA", "HOME", "DONA", "HOME", "HOME", "DONA", "HOME", "DONA",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "sexe", Locale.forLanguageTag("ca-ES"), "GENDER.TEXT_<LANGUAGE>", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void genderES() throws IOException, FTAException {
		final String[] inputs = {
				"FEMENINO", "MASCULINO", "FEMENINO", "MASCULINO", "FEMENINO", "MASCULINO", "FEMENINO", "MASCULINO", "FEMENINO", "MASCULINO",
				"MASCULINO", "FEMENINO", "MASCULINO", "FEMENINO", "MASCULINO", "FEMENINO", "MASCULINO", "FEMENINO", "MASCULINO", "FEMENINO",
				"FEMENINO", "MASCULINO", "FEMENINO", "MASCULINO", "FEMENINO", "MASCULINO", "FEMENINO", "MASCULINO", "FEMENINO", "MASCULINO",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "Sexo", Locale.forLanguageTag("es-ES"), "GENDER.TEXT_<LANGUAGE>", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void genderFI() throws IOException, FTAException {
		final String[] inputs = {
				"NAISET", "MIEHET", "NAISET", "NAISET", "MIEHET", "MIEHET", "NAISET", "MIEHET", "NAISET", "MIEHET",
				"NAISET", "MIEHET", "NAISET", "MIEHET", "NAISET", "MIEHET", "NAISET", "NAISET", "MIEHET", "MIEHET",
				"NAISET", "MIEHET", "NAISET", "MIEHET", "NAISET", "MIEHET", "NAISET", "MIEHET", "NAISET", "MIEHET",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "Sukupuoli", Locale.forLanguageTag("fi-FI"), "GENDER.TEXT_<LANGUAGE>", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void genderHR() throws IOException, FTAException {
		final String[] inputs = {
				"ŽENE", "MUŠKARCI", "ŽENE", "MUŠKARCI", "ŽENE", "MUŠKARCI", "ŽENE", "MUŠKARCI", "ŽENE", "MUŠKARCI",
				"MUŠKARCI", "ŽENE", "MUŠKARCI", "ŽENE", "MUŠKARCI", "ŽENE", "MUŠKARCI", "ŽENE", "MUŠKARCI", "ŽENE",
				"ŽENE", "MUŠKARCI", "ŽENE", "MUŠKARCI", "ŽENE", "MUŠKARCI", "ŽENE", "MUŠKARCI", "ŽENE", "MUŠKARCI",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "Gender", Locale.forLanguageTag("hr-HR"), "GENDER.TEXT_<LANGUAGE>", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void genderIS() throws IOException, FTAException {
		final String[] inputs = {
				"KONA", "KARL", "KONA", "KARL", "KONA", "KARL", "KONA", "KARL", "KONA", "KARL",
				"KARL", "KONA", "KARL", "KONA", "KARL", "KONA", "KARL", "KONA", "KARL", "KONA",
				"KONA", "KARL", "KONA", "KARL", "KONA", "KARL", "KONA", "KARL", "KONA", "KARL",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "kyn", Locale.forLanguageTag("is-IS"), "GENDER.TEXT_<LANGUAGE>", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void genderMS() throws IOException, FTAException {
		final String[] inputs = {
				"PEREMPUAN", "LELAKI", "PEREMPUAN", "LELAKI", "PEREMPUAN", "LELAKI", "PEREMPUAN", "LELAKI", "PEREMPUAN", "LELAKI",
				"LELAKI", "PEREMPUAN", "LELAKI", "PEREMPUAN", "LELAKI", "PEREMPUAN", "LELAKI", "PEREMPUAN", "LELAKI", "PEREMPUAN",
				"PEREMPUAN", "LELAKI", "PEREMPUAN", "LELAKI", "PEREMPUAN", "LELAKI", "PEREMPUAN", "LELAKI", "PEREMPUAN", "LELAKI",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "jantina", Locale.forLanguageTag("ms-MY"), "GENDER.TEXT_<LANGUAGE>", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void genderPL() throws IOException, FTAException {
		final String[] inputs = {
				"KOBIETY", "MĘŻCZYŹNI", "KOBIETY", "MĘŻCZYŹNI", "KOBIETY", "MĘŻCZYŹNI", "KOBIETY", "MĘŻCZYŹNI", "KOBIETY", "MĘŻCZYŹNI",
				"MĘŻCZYŹNI", "KOBIETY", "MĘŻCZYŹNI", "KOBIETY", "MĘŻCZYŹNI", "KOBIETY", "MĘŻCZYŹNI", "KOBIETY", "MĘŻCZYŹNI", "KOBIETY",
				"KOBIETY", "MĘŻCZYŹNI", "KOBIETY", "MĘŻCZYŹNI", "KOBIETY", "MĘŻCZYŹNI", "KOBIETY", "MĘŻCZYŹNI", "KOBIETY", "MĘŻCZYŹNI",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "Płeć", Locale.forLanguageTag("pl-PL"), "GENDER.TEXT_<LANGUAGE>", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void genderRO() throws IOException, FTAException {
		final String[] inputs = {
				"FEMEIE", "BĂRBAT", "FEMEIE", "BĂRBAT", "FEMEIE", "BĂRBAT", "FEMEIE", "BĂRBAT", "FEMEIE", "BĂRBAT",
				"BĂRBAT", "FEMEIE", "BĂRBAT", "FEMEIE", "BĂRBAT", "FEMEIE", "BĂRBAT", "FEMEIE", "BĂRBAT", "FEMEIE",
				"FEMEIE", "BĂRBAT", "FEMEIE", "BĂRBAT", "FEMEIE", "BĂRBAT", "FEMEIE", "BĂRBAT", "FEMEIE", "BĂRBAT",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "sex", Locale.forLanguageTag("ro-RO"), "GENDER.TEXT_<LANGUAGE>", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void genderSV() throws IOException, FTAException {
		final String[] inputs = {
				"KVINNA", "MAN", "KVINNA", "MAN", "KVINNA", "MAN", "KVINNA", "MAN", "KVINNA", "MAN",
				"MAN", "KVINNA", "MAN", "KVINNA", "MAN", "KVINNA", "MAN", "KVINNA", "MAN", "KVINNA",
				"KVINNA", "MAN", "KVINNA", "MAN", "KVINNA", "MAN", "KVINNA", "MAN", "KVINNA", "MAN",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "kön", Locale.forLanguageTag("sv-SE"), "GENDER.TEXT_<LANGUAGE>", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}
}
