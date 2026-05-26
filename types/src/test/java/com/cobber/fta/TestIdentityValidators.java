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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Locale;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;
import com.cobber.fta.plugins.identity.BSN_NL;
import com.cobber.fta.plugins.identity.NPI_US;

public class TestIdentityValidators {

	private NPI_US buildNPI() throws FTAPluginException {
		final PluginDefinition defn = PluginDefinition.findByName("IDENTITY.NPI_US");
		return (NPI_US) LogicalTypeFactory.newInstance(defn, new AnalysisConfig(Locale.forLanguageTag("en-US")));
	}

	private BSN_NL buildBSN() throws FTAPluginException {
		final PluginDefinition defn = PluginDefinition.findByName("IDENTITY.BSN_NL");
		return (BSN_NL) LogicalTypeFactory.newInstance(defn, new AnalysisConfig(Locale.forLanguageTag("nl-NL")));
	}

	// ---- NPI_US ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void npiAnalyzeSetOK() throws FTAPluginException {
		final NPI_US plugin = buildNPI();
		final AnalyzerContext ctx = new AnalyzerContext("npi", DateResolutionMode.None, null, new String[] { "npi" });
		// 100% match rate → OK
		final PluginAnalysis result = plugin.analyzeSet(ctx, 100L, 100L, "\\d{10}", null, null, null, null, new AnalysisConfig());
		assertEquals(result, PluginAnalysis.OK);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void npiAnalyzeSetBelowThreshold() throws FTAPluginException {
		final NPI_US plugin = buildNPI();
		final AnalyzerContext ctx = new AnalyzerContext("npi", DateResolutionMode.None, null, new String[] { "npi" });
		// 0% match rate → not OK
		final PluginAnalysis result = plugin.analyzeSet(ctx, 0L, 100L, "\\d{10}", null, null, null, null, new AnalysisConfig());
		assertFalse(result.isValid());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void npiIsValidKnownGood() throws FTAPluginException {
		final NPI_US plugin = buildNPI();
		// A known valid NPI (Luhn-checked with 80840 prefix)
		final String valid = plugin.nextRandom();
		assertNotNull(valid);
		assertTrue(plugin.isValid(valid));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void npiGetRegExp() throws FTAPluginException {
		assertEquals(buildNPI().getRegExp(), "\\d{10}");
	}

	// ---- BSN_NL ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void bsnAnalyzeSetBackout() throws FTAPluginException {
		final BSN_NL plugin = buildBSN();
		final AnalyzerContext ctx = new AnalyzerContext("bsn", DateResolutionMode.None, null, new String[] { "bsn" });
		// Cardinality < 20 → always returns backout
		final FiniteMap cardinality = new FiniteMap(10);
		final PluginAnalysis result = plugin.analyzeSet(ctx, 100L, 100L, ".*", null, cardinality, null, null,
				new AnalysisConfig(Locale.forLanguageTag("nl")));
		assertFalse(result.isValid());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void bsnAcceptsBaseType() throws FTAPluginException {
		final BSN_NL plugin = buildBSN();
		assertTrue(plugin.acceptsBaseType(FTAType.STRING));
		assertTrue(plugin.acceptsBaseType(FTAType.LONG));
		assertFalse(plugin.acceptsBaseType(FTAType.DOUBLE));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void bsnIsValidKnownGood() throws FTAPluginException {
		final BSN_NL plugin = buildBSN();
		final String valid = plugin.nextRandom();
		assertNotNull(valid);
		assertTrue(plugin.isValid(valid));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void bsnIsValidInvalid() throws FTAPluginException {
		assertFalse(buildBSN().isValid("123456789"));   // wrong check digit (almost always)
		assertFalse(buildBSN().isValid("abcdefghi"));   // non-numeric
	}
}
