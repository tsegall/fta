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

import java.io.IOException;
import java.util.Locale;

import org.testng.annotations.Test;

import com.cobber.fta.Sample;
import com.cobber.fta.TestGroups;
import com.cobber.fta.TestUtils;
import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;

public class Finnish {

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void colorFI() throws IOException, FTAException {
		final String[] inputs = {
				"PUNAINEN", "SININEN", "VIHREÄ", "KELTAINEN", "ORANSSI", "VIOLETTI", "VAALEANPUNAINEN", "MUSTA", "VALKOINEN", "HARMAA",
				"RUSKEA", "TURKOOSI", "BEIGE", "KERMA", "KULTA", "HOPEA", "LOHI", "INDIGO", "LAVENTELI", "OLIIVI",
				"BORDEAUX", "PRONSSI", "NORSUNLUU", "LAIVASTONSININEN", "KORALLI", "SYAANI", "MAGENTA",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "väri", Locale.forLanguageTag("fi-FI"), "COLOR.TEXT_FI", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void continentFI() throws IOException, FTAException {
		final String[] inputs = {
				"AFRIKKA", "AASIA", "EUROOPPA", "POHJOIS-AMERIKKA", "ETELÄ-AMERIKKA", "OSEANIA", "ETELÄMANNER",
				"EUROOPPA", "AASIA", "AFRIKKA", "POHJOIS-AMERIKKA", "AASIA", "EUROOPPA", "AFRIKKA", "OSEANIA",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "maanosa", Locale.forLanguageTag("fi-FI"), "CONTINENT.TEXT_FI", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void postalCodeFI() throws IOException, FTAException {
		final String[] inputs = {
				"00100", "00200", "00500", "01300", "02100", "02700", "04200", "06100", "07900", "08100",
				"10600", "11100", "13100", "15100", "17200", "20100", "21200", "23500", "26100", "28100",
				"33100", "37600", "40100", "45100", "50100", "55100", "60100", "65100", "70100", "90100",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "postinumero", Locale.forLanguageTag("fi-FI"), "POSTAL_CODE.POSTAL_CODE_FI", FTAType.LONG, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void countryFI() throws IOException, FTAException {
		final String[] inputs = {
				"SUOMI", "SAKSA", "RANSKA", "ESPANJA", "ITALIA", "PUOLA", "UKRAINA", "ALANKOMAAT", "RUOTSI", "NORJA",
				"VENÄJÄ", "KIINA", "JAPANI", "INTIA", "AUSTRALIA", "BRASILIA", "ARGENTIINA", "MEKSIKO", "EGYPTI", "TURKKI",
				"YHDYSVALLAT", "KANADA", "ETELÄ-AFRIKKA", "YHDISTYNYT KUNINGASKUNTA", "KREIKKA", "PORTUGALI", "BELGIA", "SVEITSI", "ITÄVALTA", "TANSKA",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "maa", Locale.forLanguageTag("fi-FI"), "COUNTRY.TEXT_FI", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void languageFI() throws IOException, FTAException {
		final String[] inputs = {
				"SUOMI", "ENGLANTI", "SAKSA", "RANSKA", "ESPANJA", "ITALIA", "PORTUGALI", "VENÄJÄ",
				"KIINA", "JAPANI", "ARABIA", "HINDI", "KOREA", "TURKKI", "PUOLA",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "kieli", Locale.forLanguageTag("fi-FI"), "LANGUAGE.TEXT_FI", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}
}
