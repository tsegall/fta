/*
 * Copyright 2017-2021 Tim Segall
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
import java.util.Collection;
import java.util.Locale;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.cobber.fta.core.FTAPluginException;

public class TestStandalonePlugins {
	@Test(groups = { "all" })
	public void randomIPV4Address() throws IOException, FTAPluginException {
		final PluginDefinition plugin = new PluginDefinition("IPADDRESS.IPV4", "com.cobber.fta.plugins.LogicalTypeIPV4Address");
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test(groups = { "all" })
	public void randomIPV6Address() throws IOException, FTAPluginException {
		final PluginDefinition plugin = new PluginDefinition("IPADDRESS.IPV6", "com.cobber.fta.plugins.LogicalTypeIPV6Address");
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.isValid("::"), "::");
		Assert.assertTrue(logical.isValid("::1"), "::1");
		Assert.assertTrue("::".matches(logical.getRegExp()), "::");
		Assert.assertTrue("::1".matches(logical.getRegExp()), "::1");
		for (int i = 0; i < 100; i++) {
			final String sample = logical.nextRandom();
			Assert.assertTrue(sample.matches(logical.getRegExp()), sample);
		}
	}

	@Test(groups = { "all" })
	public void randomPhoneNumber() throws IOException, FTAPluginException {
		final PluginDefinition plugin = new PluginDefinition("TELEPHONE", "com.cobber.fta.plugins.LogicalTypePhoneNumber");
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++) {
			final String sample = logical.nextRandom();
			Assert.assertTrue(sample.matches(logical.getRegExp()));
			Assert.assertTrue(logical.isValid(sample), sample);
		}
	}

	@Test(groups = { "all" })
	public void randomEmail() throws IOException, FTAPluginException {
		final PluginDefinition plugin = new PluginDefinition("EMAIL", "com.cobber.fta.plugins.LogicalTypeEmail");
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test(groups = { "all" })
	public void randomFirst() throws IOException, FTAPluginException {
		final PluginDefinition plugin = new PluginDefinition("FIRST_NAME", "com.cobber.fta.plugins.LogicalTypeFirstName");
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test(groups = { "all" })
	public void randomLast() throws IOException, FTAPluginException {
		final PluginDefinition plugin = new PluginDefinition("LAST_NAME", "com.cobber.fta.plugins.LogicalTypeLastName");
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++) {
			final String example = logical.nextRandom();
			Assert.assertTrue(example.matches(logical.getRegExp()));
			Assert.assertTrue(logical.isValid(example));
		}
	}

	@Test(groups = { "all" })
	public void randomURL() throws IOException, FTAPluginException {
		final PluginDefinition plugin = new PluginDefinition("URL", "com.cobber.fta.plugins.LogicalTypeURL");
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test(groups = { "all" })
	public void randomZip() throws IOException, FTAPluginException {
		final PluginDefinition plugin = new PluginDefinition("ZIP", "com.cobber.fta.plugins.LogicalTypeUSZip5");
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test(groups = { "all" })
	public void randomGUID() throws IOException, FTAPluginException {
		final PluginDefinition plugin = new PluginDefinition("GUID", "com.cobber.fta.plugins.LogicalTypeGUID");
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test(groups = { "all" })
	public void randomGender() throws IOException, FTAPluginException {
		final PluginDefinition plugin = new PluginDefinition("GENDER", "com.cobber.fta.plugins.LogicalTypeGender");
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test(groups = { "all" })
	public void randomCountry() throws IOException, FTAPluginException {
		final PluginDefinition plugin = new PluginDefinition("COUNTRY.TEXT_EN", "com.cobber.fta.plugins.LogicalTypeCountryEN");
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++) {
			final String example = logical.nextRandom();
			Assert.assertTrue(example.matches(logical.getRegExp()));
			Assert.assertTrue(logical.isValid(example.toLowerCase(Locale.ENGLISH)), example);
		}
	}

	@Test(groups = { "all" })
	public void randomSupport() throws IOException, FTAPluginException {
		final int SAMPLE_SIZE = 100;
		final Locale[] locales = new Locale[] {
				Locale.forLanguageTag("en-US"), Locale.forLanguageTag("en-GB"), Locale.forLanguageTag("en-AU"),
				Locale.forLanguageTag("nl-NL"), Locale.forLanguageTag("pt-BR"), Locale.forLanguageTag("fr-FR"),
				Locale.forLanguageTag("de-DE"), Locale.forLanguageTag("es-ES"), Locale.forLanguageTag("it-IT"),
				Locale.forLanguageTag("jp-JP"),
		};

		for (Locale locale : locales) {
			// Create an Analyzer to retrieve the Logical Types (magically will be all - since passed in '*')
			final TextAnalyzer analyzer = new TextAnalyzer("*");
			// Load the default set of plugins for Logical Type detection (normally done by a call to train())
			analyzer.registerDefaultPlugins(locale);
			final Collection<LogicalType> registered = analyzer.getPlugins().getRegisteredLogicalTypes();

			for (int iters = 0; iters < 100; iters++) {
				for (final LogicalType logicalType : registered) {

					// Does it support the nextRandom() interface
					if (!LTRandom.class.isAssignableFrom(logicalType.getClass()))
						continue;

					final LogicalTypeCode logical = (LogicalTypeCode)logicalType;

					final String[] testCases = new String[SAMPLE_SIZE];
					for (int i = 0; i < SAMPLE_SIZE; i++) {
						testCases[i] = logical.nextRandom();
						Assert.assertTrue(logical.isValid(testCases[i]), logicalType.getQualifier() + ":'" + testCases[i] + "'");
					}
					for (int i = 0; i < SAMPLE_SIZE; i++)
						Assert.assertTrue(testCases[i].matches(logical.getRegExp()), logicalType.getQualifier() + ": '" + testCases[i] + "', RE: " + logical.getRegExp());
				}
			}
		}
	}

	@Test(groups = { "all" })
	public void randomCOORDINATE_LATITUDE_DECIMAL() throws IOException, FTAPluginException {
		final PluginDefinition pluginDefinition = PluginDefinition.findByQualifier("COORDINATE.LATITUDE_DECIMAL");
		final LogicalType logical = LogicalTypeFactory.newInstance(pluginDefinition, Locale.getDefault());

		final String[] validSamples = new String[] { "12.43", "13.49", "90.0", "-69.4", "-90.0" };

		for (final String sample : validSamples)
			Assert.assertTrue(logical.isValid(sample), sample);

		final String[] invalidSamples = new String[] { "91.0", "-90.2" };

		for (final String sample : invalidSamples)
			Assert.assertFalse(logical.isValid(sample), sample);
	}

	@Test(groups = { "all" })
	public void randomCOORDINATE_LONGITUDE_DECIMAL() throws IOException, FTAPluginException {
		final PluginDefinition pluginDefinition = PluginDefinition.findByQualifier("COORDINATE.LONGITUDE_DECIMAL");
		final LogicalType logical = LogicalTypeFactory.newInstance(pluginDefinition, Locale.getDefault());

		final String[] validSamples = new String[] { "12.43", "13.49", "180.0", "90.0", "-69.4", "-90.0", "-170.0",  };

		for (final String sample : validSamples)
			Assert.assertTrue(logical.isValid(sample), sample);

		final String[] invalidSamples = new String[] { "181.0", "-190.2" };

		for (final String sample : invalidSamples)
			Assert.assertFalse(logical.isValid(sample), sample);
	}

	@Test(groups = { "all" })
	public void randomCITY() throws IOException, FTAPluginException {
		final PluginDefinition pluginDefinition = PluginDefinition.findByQualifier("CITY");
		final LogicalType logical = LogicalTypeFactory.newInstance(pluginDefinition, Locale.getDefault());

		final String[] validSamples = new String[] {
				"Milton Keynes", "Tokyo", "Delhi", "Shanghai", "Sao Paulo", "Mexico City", "Cairo", "Dhaka",
				"Mumbai", "Beijing","Osaka", "Karachi", "Chongqing", "Buenos Aires", "Istanbul", "Kolkata",
				"Lagos", "Manila", "Tianjin","Rio De Janeiro", "Malmö", "St. Louis", "Saint-Georges", "Saint-Jean-sur-Richelieu",
				"MARTHA'S VINEYARD", "CLARK'S MOUNTAIN", "Fort McMurray", "Montréal"
		};

		for (final String sample : validSamples)
			Assert.assertTrue(logical.isValid(sample), sample);

		final String[] invalidSamples = new String[] { "2001Olypics" };

		for (final String sample : invalidSamples)
			Assert.assertFalse(logical.isValid(sample), sample);
	}
}
