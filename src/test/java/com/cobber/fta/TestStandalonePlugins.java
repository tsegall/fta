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
import java.util.Locale;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestStandalonePlugins {
	@Test
	public void randomIPV4Address() throws IOException {
		final PluginDefinition plugin = new PluginDefinition("IPADDRESS.IPV4", "com.cobber.fta.plugins.LogicalTypeIPV4Address");
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test
	public void randomIPV6Address() throws IOException {
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

	@Test
	public void randomPhoneNumber() throws IOException {
		final PluginDefinition plugin = new PluginDefinition("TELEPHONE", "com.cobber.fta.plugins.LogicalTypePhoneNumber");
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++) {
			final String sample = logical.nextRandom();
			Assert.assertTrue(sample.matches(logical.getRegExp()));
			Assert.assertTrue(logical.isValid(sample), sample);
		}
	}

	@Test
	public void randomEmail() throws IOException {
		final PluginDefinition plugin = new PluginDefinition("EMAIL", "com.cobber.fta.plugins.LogicalTypeEmail");
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test
	public void randomFirst() throws IOException {
		final PluginDefinition plugin = new PluginDefinition("FIRST_NAME", "com.cobber.fta.plugins.LogicalTypeFirstName");
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test
	public void randomLast() throws IOException {
		final PluginDefinition plugin = new PluginDefinition("LAST_NAME", "com.cobber.fta.plugins.LogicalTypeLastName");
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++) {
			final String example = logical.nextRandom();
			Assert.assertTrue(example.matches(logical.getRegExp()));
			Assert.assertTrue(logical.isValid(example));
		}
	}

	@Test
	public void randomURL() throws IOException {
		final PluginDefinition plugin = new PluginDefinition("URL", "com.cobber.fta.plugins.LogicalTypeURL");
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test
	public void randomZip() throws IOException {
		final PluginDefinition plugin = new PluginDefinition("ZIP", "com.cobber.fta.plugins.LogicalTypeUSZip5");
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test
	public void randomGUID() throws IOException {
		final PluginDefinition plugin = new PluginDefinition("GUID", "com.cobber.fta.plugins.LogicalTypeGUID");
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test
	public void randomGender() throws IOException {
		final PluginDefinition plugin = new PluginDefinition("GENDER", "com.cobber.fta.plugins.LogicalTypeGenderEN");
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test
	public void randomCountry() throws IOException {
		final PluginDefinition plugin = new PluginDefinition("COUNTRY.TEXT_EN", "com.cobber.fta.plugins.LogicalTypeCountryEN");
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(plugin, Locale.getDefault());

		Assert.assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++) {
			final String example = logical.nextRandom();
			Assert.assertTrue(example.matches(logical.getRegExp()));
			Assert.assertTrue(logical.isValid(example.toLowerCase()), example);
		}
	}

	public static String allSemanticTypes[] = new String[] {
			"EMAIL", "URI.URL", "IPADDRESS.IPV4", "IPADDRESS.IPV6", "TELEPHONE", "GUID",
			"POSTAL_CODE.ZIP5_US", "POSTAL_CODE.POSTAL_CODE_UK", "POSTAL_CODE.POSTAL_CODE_CA", "POSTAL_CODE.POSTAL_CODE_AU",
			"STREET_ADDRESS_EN", "GENDER.TEXT_EN", "COUNTRY.TEXT_EN",
			"STATE_PROVINCE.PROVINCE_CA", "STATE_PROVINCE.STATE_US", "STATE_PROVINCE.STATE_PROVINCE_NA", "STATE_PROVINCE.STATE_AU",
			"CURRENCY_CODE.ISO-4217", "COUNTRY.ISO-3166-3", "COUNTRY.ISO-3166-2",
			"AIRPORT_CODE.IATA", "CITY", "SSN",
			"NAME.FIRST", "NAME.LAST", "NAME.LAST_FIRST", "NAME.FIRST_LAST",
			"CREDIT_CARD_TYPE", "LANGUAGE.ISO-639-2", "LANGUAGE.TEXT_EN",
			"MONTH.ABBR_<LOCALE>", "MONTH.FULL_<LOCALE>", "COORDINATE.LATITUDE_DECIMAL", "COORDINATE.LONGITUDE_DECIMAL", "COORDINATE_PAIR.DECIMAL"
	};

	@Test
	public void randomSupport() throws IOException {
		final int SAMPLE_SIZE = 100;

		for (int iters = 0; iters < 100; iters++) {
			for (final String qualifier : allSemanticTypes) {
				final PluginDefinition pluginDefinition = PluginDefinition.findByQualifier(qualifier);
				final LogicalType logicalType = LogicalTypeFactory.newInstance(pluginDefinition, Locale.getDefault());

				// Does it support the nextRandom() interface
				if (!LTRandom.class.isAssignableFrom(logicalType.getClass()))
					continue;

				final LogicalTypeCode logical = (LogicalTypeCode)logicalType;

				final String[] testCases = new String[SAMPLE_SIZE];
				for (int i = 0; i < SAMPLE_SIZE; i++) {
					testCases[i] = logical.nextRandom();
					Assert.assertTrue(logical.isValid(testCases[i]), qualifier + ":" + testCases[i]);
				}
				for (int i = 0; i < SAMPLE_SIZE; i++)
					Assert.assertTrue(testCases[i].matches(logical.getRegExp()), qualifier + ": '" + testCases[i] + "', RE: " + logical.getRegExp());
			}
		}
	}

	@Test
	public void randomSupportIntuitLocale() throws IOException {
		final int SAMPLE_SIZE = 100;

		for (int iters = 0; iters < 100; iters++) {
			for (final String qualifier : allSemanticTypes) {
				final PluginDefinition pluginDefinition = PluginDefinition.findByQualifier(qualifier);
				final LogicalType logicalType = LogicalTypeFactory.newInstance(pluginDefinition);

				// Does it support the nextRandom() interface
				if (!LTRandom.class.isAssignableFrom(logicalType.getClass()))
					continue;

				final LogicalTypeCode logical = (LogicalTypeCode)logicalType;

				String[] testCases = new String[SAMPLE_SIZE];
				for (int i = 0; i < SAMPLE_SIZE; i++) {
					testCases[i] = logical.nextRandom();
					Assert.assertTrue(logical.isValid(testCases[i]), qualifier + ":" + testCases[i]);
				}
				for (int i = 0; i < SAMPLE_SIZE; i++)
					Assert.assertTrue(testCases[i].matches(logical.getRegExp()), qualifier + ": '" + testCases[i] + "', RE: " + logical.getRegExp());
			}
		}
	}

	@Test
	public void randomSupportByQualifier() throws IOException {
		final int SAMPLE_SIZE = 100;

		for (int iters = 0; iters < 100; iters++) {
			for (final String qualifier : allSemanticTypes) {
				final LogicalType logicalType = LogicalTypeFactory.newInstance(qualifier);

				// Does it support the nextRandom() interface
				if (!LTRandom.class.isAssignableFrom(logicalType.getClass()))
					continue;

				final LogicalTypeCode logical = (LogicalTypeCode)logicalType;

				final String[] testCases = new String[SAMPLE_SIZE];
				for (int i = 0; i < SAMPLE_SIZE; i++) {
					testCases[i] = logical.nextRandom();
					Assert.assertTrue(logical.isValid(testCases[i]), qualifier + ":" + testCases[i]);
				}
				for (int i = 0; i < SAMPLE_SIZE; i++)
					Assert.assertTrue(testCases[i].matches(logical.getRegExp()), qualifier + ": '" + testCases[i] + "', RE: " + logical.getRegExp());
			}
		}
	}

	@Test
	public void randomCOORDINATE_LATITUDE_DECIMAL() throws IOException {
		final PluginDefinition pluginDefinition = PluginDefinition.findByQualifier("COORDINATE.LATITUDE_DECIMAL");
		final LogicalType logical = LogicalTypeFactory.newInstance(pluginDefinition, Locale.getDefault());

		final String[] validSamples = new String[] { "12.43", "13.49", "90.0", "-69.4", "-90.0" };

		for (final String sample : validSamples)
			Assert.assertTrue(logical.isValid(sample), sample);

		final String[] invalidSamples = new String[] { "91.0", "-90.2" };

		for (final String sample : invalidSamples)
			Assert.assertFalse(logical.isValid(sample), sample);
	}

	@Test
	public void randomCOORDINATE_LONGITUDE_DECIMAL() throws IOException {
		final PluginDefinition pluginDefinition = PluginDefinition.findByQualifier("COORDINATE.LONGITUDE_DECIMAL");
		final LogicalType logical = LogicalTypeFactory.newInstance(pluginDefinition, Locale.getDefault());

		final String[] validSamples = new String[] { "12.43", "13.49", "180.0", "90.0", "-69.4", "-90.0", "-170.0",  };

		for (final String sample : validSamples)
			Assert.assertTrue(logical.isValid(sample), sample);

		final String[] invalidSamples = new String[] { "181.0", "-190.2" };

		for (final String sample : invalidSamples)
			Assert.assertFalse(logical.isValid(sample), sample);
	}

	@Test
	public void randomCITY() throws IOException {
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
