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
package com.cobber.fta;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.testng.annotations.Test;

import com.cobber.fta.TextAnalyzer.Feature;
import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;

public class TestIdentity {
	private static final SecureRandom random = new SecureRandom();

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void possibleSSN() throws IOException, FTAException {
		final String[] samples = new String[1000];

		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < samples.length; i++) {
			if (i == 100)
				samples[i] = "000-00-0000";
			else if (i == 200)
				samples[i] = "943-00-1067";
			else {
				b.setLength(0);
				int component = random.nextInt(899) + 1;
				if (component == 666)
					component = 667;
				b.append(String.format("%03d", component));
				b.append('-');
				b.append(String.format("%02d", random.nextInt(99) + 1));
				b.append('-');
				b.append(String.format("%04d", random.nextInt(9999) + 1));
				samples[i] = b.toString();
			}
		}

		final TextAnalyzer analysis = new TextAnalyzer("possibleSSN");
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		analysis.configure(Feature.LENGTH_QUALIFIER, false);
		for (final String sample : samples)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSemanticType(), "SSN");
		assertEquals(result.getSampleCount(), samples.length);
		assertEquals(result.getMatchCount(), samples.length - 2);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), "(?!666|000|9\\d{2})\\d{3}-(?!00)\\d{2}-(?!0{4})\\d{4}");
		assertTrue(result.isSemanticType());
		assertEquals(result.getStructureSignature(), PluginDefinition.findByName("SSN").signature);
		assertEquals(result.getConfidence(), 0.998);
		assertNull(result.checkCounts());

		for (int l = 0; l < samples.length; l++) {
			if (l != 100 && l != 200)
				assertTrue(samples[l].matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void notSSN() throws IOException, FTAException {
		final String pipedInput = "000-00-0000|063-02-3609|527-99-6328|209-50-0139|239-64-4998|" +
				"210-19-0049|635-31-8665|215-38-8995|209-50-0139|304-88-9478|" +
				"312-35-8549|063-02-3609|927-99-6328|209-00-0139|239-64-4998|" +
				"113-36-8579|363-22-3701|887-88-6124|207-33-4569|211-11-0498|" +
				"532-71-2239|963-02-3609|527-99-6328|909-56-0139|934-66-4597|";
		final String samples[] = pipedInput.split("\\|");

		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(samples), "notSSN", Locale.US, null, FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), samples.length);
		assertEquals(result.getRegExp(), "\\d{3}-\\d{2}-\\d{4}");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testRegExpLogicalType_SSN() throws IOException, FTAException {
		final String[] samples = new String[1000];

		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < samples.length; i++) {
			b.setLength(0);
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append('-');
			b.append(String.format("%02d", random.nextInt(100)));
			b.append('-');
			b.append(String.format("%04d", random.nextInt(10000)));
			samples[i] = b.toString();
		}

		final TextAnalyzer analysis = new TextAnalyzer("Primary SSN");
		analysis.configure(TextAnalyzer.Feature.DEFAULT_SEMANTIC_TYPES, false);
		analysis.setLocale(Locale.forLanguageTag("en-US"));
		final List<PluginDefinition> plugins = new ArrayList<>();
		plugins.add(new PluginDefinition("SSN", "Social Security Number", null, null, "\\d{3}-\\d{2}-\\d{4}",
				new PluginLocaleEntry[] { new PluginLocaleEntry("en-US", ".*(SSN|social).*" , 100, "\\d{3}-\\d{2}-\\d{4}") },
						true, 98, FTAType.STRING));

		try {
			analysis.getPlugins().registerPluginList(plugins, analysis.getStreamName(), analysis.getConfig());
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		for (final String sample : samples) {
			analysis.train(sample);
		}
		analysis.train("032--45-0981");

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getRegExp(), "\\d{3}-\\d{2}-\\d{4}");
		assertEquals(result.getSemanticType(), "SSN");
		assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getInvalidCount(), 1);
		assertEquals(result.getSampleCount(), samples.length + 1);
		final Map<String, Long> invalids = result.getInvalidDetails();
		assertEquals(invalids.size(), 1);
		assertEquals(invalids.get("032--45-0981"), 1L);
		assertNull(result.checkCounts());

		for (final String sample : samples)
			assertTrue(sample.matches(result.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicSSN_FR() throws IOException, FTAException {
		final String[] inputs = {
				"186022A215325 23", "1691099352470 01", "2741147566941 55",
				"1870364431266 17", "1620750699385 24", "1910926856381 09", "2350193443182 66",
				"1021130154849 54", "1060633581206 43", "2790148853457 33", "1910585591722 44",
				"2031245436518 70", "1011076339993 38", "2980845336004 29", "1991181413900 71",
				"1500645426767 03", "1180926187160 15", "2300747704141 68", "1820485399754 86",
				"1870963392946 48", "1510366293364 46", "2800291682045 16", "1660882307695 51",
				"2760672523900 48", "2130327681550 09", "1940965237732 53", "2370790974188 20",
		};

		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "basicSSN_FR", Locale.forLanguageTag("fr-FR"), "IDENTITY.SSN_FR", FTAType.STRING, 1.0);

		assertEquals(result.getCardinality(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicPersonNummer_yyyy_SE() throws IOException, FTAException {
		final String[] inputs = {
				"19781216-2449", "19781211-2444", "19600518-9557", "19570105-5401",
				"19570602-5540", "19431023-5918", "19970113-8183", "19320417-7699",
				"19220110+2924", "19870117-9643", "19680430-7822", "20080413-5689",
				"20200315-9502", "19300728-5699", "19300417-3385", "20020317-4388",
				"19620227-1331", "19331216-9414", "20080102-2823", "20030211-8492",
				"19250925-4658", "19420426-9635", "19720805-1438", "19461011-2924",
				"20140515-9656"
		};

		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "PersonNummer", Locale.forLanguageTag("sv-SE"), "IDENTITY.PERSONNUMMER_SE", FTAType.STRING, 1.0);

		assertEquals(result.getCardinality(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicPersonNummer_yy_SE() throws IOException, FTAException {
		final String[] inputs = {
				"781216-2449", "781211-2444", "600518-9557", "570105-5401",
				"570602-5540", "431023-5918", "970113-8183", "320417-7699",
				"220110+2924", "870117-9643", "680430-7822", "080413-5689",
				"200315-9502", "300728-5699", "300417-3385", "020317-4388",
				"620227-1331", "331216-9414", "080102-2823", "030211-8492",
				"250925-4658", "420426-9635", "720805-1438", "461011-2924",
				"140515-9656"
		};

		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "PersonNummer", Locale.forLanguageTag("sv-SE"), "IDENTITY.PERSONNUMMER_SE", FTAType.STRING, 1.0);

		assertEquals(result.getCardinality(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicIN_JA() throws IOException, FTAException {
		final String[] inputs = {
				"182635424142", "159527866110", "468078079802", "466664186321",
				"846926702714", "685980008501", "160213060470", "330630040728",
				"756862498647", "819877682969", "632954948346", "179173299818",
				"157373192780", "190773997172", "207636877550", "315173957620",
				"306078820740", "868285059291", "255412548963", "189995188363",
				"812906182895", "645408744459", "668972804372", "907328637257"
		};

		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "basicIN_JA", Locale.forLanguageTag("ja"), "IDENTITY.INDIVIDUAL_NUMBER_JA", FTAType.STRING, 1.0);

		assertEquals(result.getCardinality(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicSSN_CH() throws IOException, FTAException {
		final String[] inputs = {
				"756.3830.7985.38", "756.9709.5787.13", "756.7932.0847.28", "756.4391.6683.84",
				"756.8608.5554.50", "756.7755.7020.90", "756.8274.6040.25", "756.4546.3052.49",
				"756.0087.4496.40", "756.8921.5663.62", "756.3643.3750.32", "756.9704.5745.81",
				"756.8231.4185.59", "756.5332.8407.84", "756.3740.5065.68", "756.0897.8077.08",
				"756.9155.5542.00", "756.3226.0924.93", "756.5132.5619.19", "756.1788.1037.25",
				"756.2753.6875.23", "756.7287.4292.98", "756.2605.0921.61", "756.8724.8722.88"
		};

		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "basicSSN_CH", Locale.forLanguageTag("de-CH"), "IDENTITY.SSN_CH", FTAType.STRING, 1.0);
		assertEquals(result.getCardinality(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicNHS_UK() throws IOException, FTAException {
		final String[] inputs = {
				"603 235 8429", "607 639 9864", "663 217 6682", "740 844 8349", "489 161 1189",
				"854 106 0098", "726 516 9476", "957 260 2357", "686 273 2757", "896 329 3181",
				"443 934 8424", "963 033 6693", "805 735 6146", "633 502 1153", "775 663 3911",
				"405 458 8298", "953 622 6391", "627 075 8319", "808 974 4516", "940 604 2495",
				"736 088 6082", "820 530 6265", "692 233 6046", "760 019 4724", "998 607 2263"
		};

		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "basicNHS_UK", Locale.forLanguageTag("en-UK"), "IDENTITY.NHS_UK", FTAType.STRING, 1.0);
		assertEquals(result.getCardinality(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicAadhaar_IN() throws IOException, FTAException {
		final String[] inputs = {
				"6625 7361 2953", "5931 6696 0291", "8248 5984 8175", "3016 4826 5142",
				"4434 7776 8326", "4824 7928 4386", "4685 4991 7577", "3863 3381 0102",
				"8015 6866 2992", "9526 2155 7463", "5019 5961 5939", "6329 6296 9460",
				"2104 0519 0000", "6858 3860 1162", "7401 5528 6218", "6263 2420 8471",
				"8955 0790 8863", "7313 7946 3468", "9806 9428 4326", "9187 7705 5800",
				"6138 7065 3258", "8060 0186 2257", "7658 2380 1240", "2768 4014 3753",
		};

		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "basicAadhaar_IN", Locale.forLanguageTag("en-IN"), "IDENTITY.AADHAAR_IN", FTAType.STRING, 1.0);

		assertEquals(result.getCardinality(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
	}
}
