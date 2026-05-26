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
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Locale;

import org.testng.annotations.Test;

import com.cobber.fta.PluginDefinition.Precedence;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;

public class TestPluginDefinition {

	// ---- getOrder / precedence ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void getOrderBuiltin() {
		final PluginDefinition defn = new PluginDefinition();
		defn.priority = 100;
		assertEquals(defn.getPrecedence(), Precedence.BUILTIN);
		assertEquals(defn.getOrder(), 100 + PluginDefinition.PRIORITY_MAX);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void getOrderPreBuiltin() {
		final PluginDefinition defn = new PluginDefinition();
		defn.priority = 100;
		defn.setPrecedence(Precedence.PRE_BUILTIN);
		assertEquals(defn.getPrecedence(), Precedence.PRE_BUILTIN);
		assertEquals(defn.getOrder(), 100);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void getOrderPostBuiltin() {
		final PluginDefinition defn = new PluginDefinition();
		defn.priority = 100;
		defn.setPrecedence(Precedence.POST_BUILTIN);
		assertEquals(defn.getPrecedence(), Precedence.POST_BUILTIN);
		assertEquals(defn.getOrder(), 100 + 2 * PluginDefinition.PRIORITY_MAX);
	}

	// ---- findByName ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void findByNameFound() {
		final PluginDefinition email = PluginDefinition.findByName("EMAIL");
		assertNotNull(email);
		assertEquals(email.semanticType, "EMAIL");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void findByNameNotFound() {
		assertNull(PluginDefinition.findByName("UNKNOWN_TYPE_XYZ_NOT_REAL"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void findByNameCaseInsensitive() {
		assertNotNull(PluginDefinition.findByName("email"));
		assertNotNull(PluginDefinition.findByName("Email"));
	}

	// ---- copy constructor ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void copyConstructorCopiesFields() {
		final PluginDefinition orig = new PluginDefinition();
		orig.semanticType = "COPY_TEST";
		orig.description = "Test Description";
		orig.priority = 42;
		orig.minimum = "1";
		orig.maximum = "100";
		orig.validLocales = PluginLocaleEntry.simple(new String[] { "en", "fr" });

		final PluginDefinition copy = new PluginDefinition(orig);
		assertEquals(copy.semanticType, "COPY_TEST");
		assertEquals(copy.description, "Test Description");
		assertEquals(copy.priority, 42);
		assertEquals(copy.minimum, "1");
		assertEquals(copy.maximum, "100");
		assertEquals(copy.validLocales.length, 2);

		// deep copy: mutating original's locale tag must not affect copy
		orig.validLocales[0].localeTag = "de";
		assertEquals(copy.validLocales[0].localeTag, "en");
	}

	// ---- getLocaleEntry ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void getLocaleEntryLanguageMatch() throws FTAPluginException {
		final PluginDefinition defn = new PluginDefinition();
		defn.semanticType = "TEST";
		defn.validLocales = new PluginLocaleEntry[] {
			new PluginLocaleEntry("en", null, 0, "\\d{5}")
		};
		assertNotNull(defn.getLocaleEntry(Locale.ENGLISH));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void getLocaleEntryWildcardFallback() throws FTAPluginException {
		final PluginDefinition defn = new PluginDefinition();
		defn.semanticType = "TEST";
		defn.validLocales = new PluginLocaleEntry[] {
			new PluginLocaleEntry("*", null, 0, "\\d{5}")
		};
		// French is not "en" but the wildcard covers everything
		assertNotNull(defn.getLocaleEntry(Locale.FRENCH));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void getLocaleEntryNullValidLocalesThrows() {
		final PluginDefinition defn = new PluginDefinition();
		defn.semanticType = "TEST";
		try {
			defn.getLocaleEntry(Locale.ENGLISH);
			fail("Expected FTAPluginException when validLocales is null");
		} catch (FTAPluginException e) {
			assertTrue(e.getMessage().contains("TEST"));
		}
	}

	// ---- isLocaleSupported ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void isLocaleSupportedTrue() throws FTAPluginException {
		final PluginDefinition defn = new PluginDefinition();
		defn.semanticType = "TEST";
		defn.validLocales = new PluginLocaleEntry[] {
			new PluginLocaleEntry("en", null, 0, "\\d{5}")
		};
		assertTrue(defn.isLocaleSupported(Locale.ENGLISH));
		assertFalse(defn.isLocaleSupported(Locale.FRENCH));
	}

	// ---- isMandatoryHeaderUnsatisfied ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void mandatoryHeaderNotSatisfied() {
		final PluginDefinition defn = new PluginDefinition();
		defn.semanticType = "TEST";
		final PluginLocaleEntry localeEntry = new PluginLocaleEntry("en", "(?i)email", 85, "\\S+");
		localeEntry.headerRegExps[0].mandatory = true;
		defn.validLocales = new PluginLocaleEntry[] { localeEntry };

		// "phone" stream name doesn't match "(?i)email" → mandatory header unsatisfied
		final AnalyzerContext ctx = new AnalyzerContext("phone", DateResolutionMode.None, null, new String[] { "phone" });
		assertTrue(defn.isMandatoryHeaderUnsatisfied(Locale.ENGLISH, ctx));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void mandatoryHeaderSatisfied() {
		final PluginDefinition defn = new PluginDefinition();
		defn.semanticType = "TEST";
		final PluginLocaleEntry localeEntry = new PluginLocaleEntry("en", "(?i)email", 85, "\\S+");
		localeEntry.headerRegExps[0].mandatory = true;
		defn.validLocales = new PluginLocaleEntry[] { localeEntry };

		// "email" matches "(?i)email" and confidence=85 (not < 0) → returns false (satisfied)
		final AnalyzerContext ctx = new AnalyzerContext("email", DateResolutionMode.None, null, new String[] { "email" });
		assertFalse(defn.isMandatoryHeaderUnsatisfied(Locale.ENGLISH, ctx));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void nullHeaderRegExpsNotMandatory() {
		final PluginDefinition defn = new PluginDefinition();
		defn.semanticType = "TEST";
		// PluginLocaleEntry with null headerRegExp → headerRegExps is null
		defn.validLocales = new PluginLocaleEntry[] {
			new PluginLocaleEntry("en", null, 0, "\\S+")
		};
		final AnalyzerContext ctx = new AnalyzerContext("anything", DateResolutionMode.None, null, new String[] { "anything" });
		assertFalse(defn.isMandatoryHeaderUnsatisfied(Locale.ENGLISH, ctx));
	}

	// ---- getLocaleDescription ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void getLocaleDescriptionNonEmpty() {
		final PluginDefinition defn = new PluginDefinition();
		defn.semanticType = "TEST";
		defn.validLocales = new PluginLocaleEntry[] {
			new PluginLocaleEntry("en", null, 0, "\\d{5}")
		};
		final String desc = defn.getLocaleDescription();
		assertNotNull(desc);
		assertTrue(desc.contains("en"));
	}

	// ---- getOptions ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void getOptionsNullByDefault() {
		final PluginDefinition defn = new PluginDefinition();
		assertNull(defn.getOptions());
	}
}
