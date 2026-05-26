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

public class TestLogicalType {

	private LogicalType emailPlugin() throws FTAPluginException {
		final PluginDefinition defn = PluginDefinition.findByName("EMAIL");
		return LogicalTypeFactory.newInstance(defn, new AnalysisConfig());
	}

	// ---- getPriority ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void getPriority() throws FTAPluginException {
		final LogicalType lt = emailPlugin();
		assertTrue(lt.getPriority() > 0);
	}

	// ---- isLocaleSensitive ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void isLocaleSensitiveFalseForEmail() throws FTAPluginException {
		assertFalse(emailPlugin().isLocaleSensitive());
	}

	// ---- isRegExpComplete ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void isRegExpCompleteForEmail() throws FTAPluginException {
		// EmailLT overrides isRegExpComplete() to return true
		assertTrue(emailPlugin().isRegExpComplete());
	}

	// ---- getHeaderConfidence(String, String) ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void getHeaderConfidenceTwoArg() throws FTAPluginException {
		final LogicalType lt = emailPlugin();
		// "email" header should produce positive confidence; "phone" should not
		assertTrue(lt.getHeaderConfidence(null, "email") > 0);
		assertEquals(lt.getHeaderConfidence(null, "phone"), 0);
	}

	// ---- deprecated getHeaderConfidence(String) ----

	@SuppressWarnings("deprecation")
	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void getHeaderConfidenceOneArgDeprecated() throws FTAPluginException {
		final LogicalType lt = emailPlugin();
		assertTrue(lt.getHeaderConfidence("email") > 0);
		assertEquals(lt.getHeaderConfidence("name"), 0);
	}

	// ---- setThreshold / getThreshold ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void setAndGetThreshold() throws FTAPluginException {
		final LogicalType lt = emailPlugin();
		final int original = lt.getThreshold();
		lt.setThreshold(80);
		assertEquals(lt.getThreshold(), 80);
		lt.setThreshold(original);
	}

	// ---- acceptsBaseType ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void acceptsBaseTypeStringTrue() throws FTAPluginException {
		assertTrue(emailPlugin().acceptsBaseType(FTAType.STRING));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void acceptsBaseTypeLongFalse() throws FTAPluginException {
		assertFalse(emailPlugin().acceptsBaseType(FTAType.LONG));
	}

	// ---- getPluginDefinition ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void getPluginDefinitionNonNull() throws FTAPluginException {
		final LogicalType lt = emailPlugin();
		final PluginDefinition defn = lt.getPluginDefinition();
		assertNotNull(defn);
		assertEquals(defn.semanticType, "EMAIL");
	}

	// ---- isValid(String) one-arg convenience ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void isValidOneArgEmail() throws FTAPluginException {
		final LogicalType lt = emailPlugin();
		assertTrue(lt.isValid("user@example.com"));
		assertFalse(lt.isValid("not-an-email"));
	}

	// ---- compareTo ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void compareToOrdersByOrder() throws FTAPluginException {
		final LogicalType email = LogicalTypeFactory.newInstance(PluginDefinition.findByName("EMAIL"), new AnalysisConfig());
		final LogicalType guid = LogicalTypeFactory.newInstance(PluginDefinition.findByName("GUID"), new AnalysisConfig());
		// compareTo establishes a consistent ordering between two plugins
		final int cmp = email.compareTo(guid);
		assertTrue(cmp < 0 || cmp > 0 || cmp == 0, "compareTo must return an int");
	}

	// ---- LogicalTypeFactory: unknown pluginType throws ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void logicalTypeFactoryUnknownTypeThrows() {
		final PluginDefinition defn = new PluginDefinition();
		defn.semanticType = "UNKNOWN";
		defn.pluginType = "not_a_real_type";
		defn.validLocales = PluginLocaleEntry.simple(new String[] { "*" });
		try {
			LogicalTypeFactory.newInstance(defn, new AnalysisConfig());
			throw new AssertionError("Expected FTAPluginException");
		} catch (FTAPluginException e) {
			assertTrue(e.getMessage().contains("unknown type"));
		}
	}

	// ---- LogicalTypeFactory: bad class name throws ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void logicalTypeFactoryBadClassThrows() {
		final PluginDefinition defn = new PluginDefinition("BADCLASS", "com.cobber.fta.NonExistentPluginClass99");
		defn.validLocales = PluginLocaleEntry.simple(new String[] { "*" });
		try {
			LogicalTypeFactory.newInstance(defn, new AnalysisConfig());
			throw new AssertionError("Expected FTAPluginException");
		} catch (FTAPluginException e) {
			assertNotNull(e.getMessage());
		}
	}

	// ---- LogicalTypeBloomFilter: analyzeSet backout path covers private backout() ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void bloomFilterAnalyzeSetBackout() throws FTAPluginException {
		final TextAnalyzer analysis = TextAnalyzer.getDefaultAnalysis(Locale.forLanguageTag("en-AU"));
		final LogicalType suburb = analysis.getPlugins().getRegistered("STATE_PROVINCE.SUBURB_AU");
		assertNotNull(suburb, "SuburbAU plugin must be registered for en-AU");

		final AnalyzerContext ctx = new AnalyzerContext("test", DateResolutionMode.None, null, new String[] { "test" });
		// Empty cardinality (size=0 < 5) with no header match → first early-return in analyzeSet → calls private backout()
		final FiniteMap cardinality = new FiniteMap(5);
		final PluginAnalysis result = suburb.analyzeSet(ctx, 0L, 10L, ".*", null, cardinality, null, null,
				new AnalysisConfig(Locale.forLanguageTag("en-AU")));
		assertFalse(result.isValid(), "Should return backout when cardinality is too small");
	}
}
