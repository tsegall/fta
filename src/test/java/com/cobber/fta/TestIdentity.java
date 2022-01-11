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

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.plugins.IdentityIN_JA;
import com.cobber.fta.plugins.IdentitySSN_CH;
import com.cobber.fta.plugins.IdentitySSN_FR;

public class TestIdentity {
	private static final SecureRandom random = new SecureRandom();

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void possibleSSN() throws IOException, FTAException {
		String[] samples = new String[1000];

		final StringBuilder b = new StringBuilder();
		samples[0] = "000-00-0000";
		int i = 1;
		while (i < samples.length - 1) {
			b.setLength(0);
			int component = random.nextInt(899) + 1;
			if (component == 666)
				continue;
			b.append(String.format("%03d", component));
			b.append('-');
			b.append(String.format("%02d", random.nextInt(99) + 1));
			b.append('-');
			b.append(String.format("%04d", random.nextInt(9999) + 1));
			samples[i] = b.toString();
			i++;
		}
		samples[samples.length - 1] = "943-00-1067";

		final TextAnalyzer analysis = new TextAnalyzer("possibleSSN");
		analysis.setLengthQualifier(false);
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), samples.length);
		Assert.assertEquals(result.getMatchCount(), samples.length - 2);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertEquals(result.getRegExp(), "(?!666|000|9\\d{2})\\d{3}-(?!00)\\d{2}-(?!0{4})\\d{4}");
		Assert.assertTrue(result.isLogicalType());
		Assert.assertEquals(result.getTypeQualifier(), "SSN");
		Assert.assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("SSN").signature);
		Assert.assertEquals(result.getConfidence(), 0.998);

		for (int l = 1; l < samples.length - 1; l++) {
			Assert.assertTrue(samples[l].matches(result.getRegExp()));
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

		final TextAnalyzer analysis = new TextAnalyzer("notSSN");
		analysis.setLengthQualifier(false);
		for (final String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), samples.length);
		Assert.assertEquals(result.getMatchCount(), samples.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertEquals(result.getRegExp(), "\\d{3}-\\d{2}-\\d{4}");
		Assert.assertFalse(result.isLogicalType());
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int l = 0; l < samples.length; l++) {
			Assert.assertTrue(samples[l].matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testRegExpLogicalType_SSN() throws IOException, FTAException {
		String[] samples = new String[1000];

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
		analysis.setDefaultLogicalTypes(false);
		final List<PluginDefinition> plugins = new ArrayList<>();
		plugins.add(new PluginDefinition("SSN", "Social Security Number", "\\d{3}-\\d{2}-\\d{4}",
				new String[] {"\\d{3}-\\d{2}-\\d{4}"}, null, null, null, "\\d{3}-\\d{2}-\\d{4}", new String[] { "en-US" }, true, new String[] { ".*(SSN|social).*" }, new int[] { 100 }, 98, FTAType.STRING));

		try {
			analysis.getPlugins().registerPluginList(plugins, analysis.getStreamName(), null);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		for (final String sample : samples) {
			analysis.train(sample);
		}
		analysis.train("032--45-0981");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertEquals(result.getRegExp(), "\\d{3}-\\d{2}-\\d{4}");
		Assert.assertEquals(result.getTypeQualifier(), "SSN");
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getSampleCount(), samples.length + 1);
		final Map<String, Long> outliers = result.getOutlierDetails();
		Assert.assertEquals(outliers.size(), 1);
		Assert.assertEquals(outliers.get("032--45-0981"), Long.valueOf(1));

		for (final String sample : samples) {
			Assert.assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicSSN_FR() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicSSN_FR");
		analysis.setLocale(Locale.forLanguageTag("fr-FR"));

		final String[] inputs = new String[] {
				"186022A215325 23", "1691099352470 01", "2741147566941 55",
				"1870364431266 17", "1620750699385 24", "1910926856381 09", "2350193443182 66",
				"1021130154849 54", "1060633581206 43", "2790148853457 33", "1910585591722 44",
				"2031245436518 70", "1011076339993 38", "2980845336004 29", "1991181413900 71",
				"1500645426767 03", "1180926187160 15", "2300747704141 68", "1820485399754 86",
				"1870963392946 48", "1510366293364 46", "2800291682045 16", "1660882307695 51",
				"2760672523900 48", "2130327681550 09", "1940965237732 53", "2370790974188 20",
		};

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getCardinality(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertEquals(result.getTypeQualifier(), IdentitySSN_FR.SEMANTIC_TYPE);
		Assert.assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("IDENTITY.SSN_FR").signature);
		Assert.assertEquals(result.getConfidence(), 1.0);


		for (final String input : inputs) {
			Assert.assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicIN_JA() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicIN_JA");
		analysis.setLocale(Locale.forLanguageTag("ja"));

		final String[] inputs = new String[] {
				"182635424142", "159527866110", "468078079802", "466664186321",
				"846926702714", "685980008501", "160213060470", "330630040728",
				"756862498647", "819877682969", "632954948346", "179173299818",
				"157373192780", "190773997172", "207636877550", "315173957620",
				"306078820740", "868285059291", "255412548963", "189995188363",
				"812906182895", "645408744459", "668972804372", "907328637257"
		};

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getCardinality(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertEquals(result.getTypeQualifier(), IdentityIN_JA.SEMANTIC_TYPE);
		Assert.assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("IDENTITY.INDIVIDUAL_NUMBER_JA").signature);
		Assert.assertEquals(result.getConfidence(), 1.0);


		for (final String input : inputs) {
			Assert.assertTrue(input.matches(result.getRegExp()));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void basicSSN_CH() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicSSN_CH");
		analysis.setLocale(Locale.forLanguageTag("de-CH"));

		final String[] inputs = new String[] {
				"756.3830.7985.38", "756.9709.5787.13", "756.7932.0847.28", "756.4391.6683.84",
				"756.8608.5554.50", "756.7755.7020.90", "756.8274.6040.25", "756.4546.3052.49",
				"756.0087.4496.40", "756.8921.5663.62", "756.3643.3750.32", "756.9704.5745.81",
				"756.8231.4185.59", "756.5332.8407.84", "756.3740.5065.68", "756.0897.8077.08",
				"756.9155.5542.00", "756.3226.0924.93", "756.5132.5619.19", "756.1788.1037.25",
				"756.2753.6875.23", "756.7287.4292.98", "756.2605.0921.61", "756.8724.8722.88"
		};

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getCardinality(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getType(), FTAType.STRING);
		Assert.assertEquals(result.getTypeQualifier(), IdentitySSN_CH.SEMANTIC_TYPE);
		Assert.assertEquals(result.getStructureSignature(), PluginDefinition.findByQualifier("IDENTITY.SSN_CH").signature);
		Assert.assertEquals(result.getConfidence(), 1.0);


		for (final String input : inputs) {
			Assert.assertTrue(input.matches(result.getRegExp()));
		}
	}
}
