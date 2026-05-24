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

public class Norwegian {

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void postalCodeNO() throws IOException, FTAException {
		final String[] inputs = {
				"0001", "0010", "0100", "0150", "0250", "0350", "0450", "0550", "0650", "0750",
				"1001", "1300", "1400", "1500", "2000", "2100", "3000", "3200", "4000", "4600",
				"5000", "5200", "5500", "6000", "6400", "7000", "7400", "7500", "8000", "9000",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "postnummer", Locale.forLanguageTag("no-NO"), "POSTAL_CODE.POSTAL_CODE_NO", FTAType.LONG, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void postalCodeAT() throws IOException, FTAException {
		final String[] inputs = {
				"1010", "1020", "1030", "1040", "1050", "1060", "1070", "1080", "1090", "1100",
				"2100", "2340", "2500", "3100", "4020", "4600", "5020", "6020", "6800", "7000",
				"8010", "8020", "8200", "8600", "9020", "9500", "3400", "4400", "5700", "6900",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "Postleitzahl", Locale.forLanguageTag("de-AT"), "POSTAL_CODE.POSTAL_CODE_AT", FTAType.LONG, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void postalCodeCH() throws IOException, FTAException {
		final String[] inputs = {
				"1000", "1200", "1700", "2000", "2500", "3000", "3600", "3900", "4000", "4500",
				"5000", "5400", "6000", "6300", "6600", "6900", "7000", "7500", "8000", "8200",
				"8400", "8600", "8800", "8900", "9000", "9200", "9400", "9500", "1800", "2300",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "PLZ", Locale.forLanguageTag("de-CH"), "POSTAL_CODE.POSTAL_CODE_CH", FTAType.LONG, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}
}
