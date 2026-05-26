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

import org.testng.annotations.Test;

public class TestPluginLocaleEntry {

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void headerConfidenceNull() {
		final PluginLocaleEntry entry = new PluginLocaleEntry("en", "(?i)color", 90, "\\p{Alpha}+");
		assertEquals(entry.getHeaderConfidence(null, null), 0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void headerConfidenceBlank() {
		final PluginLocaleEntry entry = new PluginLocaleEntry("en", "(?i)color", 90, "\\p{Alpha}+");
		assertEquals(entry.getHeaderConfidence(null, "   "), 0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void headerConfidenceMatch() {
		final PluginLocaleEntry entry = new PluginLocaleEntry("en", "(?i)color", 90, "\\p{Alpha}+");
		assertEquals(entry.getHeaderConfidence(null, "color"), 90);
		assertEquals(entry.getHeaderConfidence(null, "COLOR"), 90);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void headerConfidenceNoMatch() {
		final PluginLocaleEntry entry = new PluginLocaleEntry("en", "(?i)color", 90, "\\p{Alpha}+");
		assertEquals(entry.getHeaderConfidence(null, "name"), 0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void headerConfidenceNoHeaderRegExps() {
		// simple() creates entries with no headerRegExps
		final PluginLocaleEntry entry = new PluginLocaleEntry("en");
		assertEquals(entry.getHeaderConfidence(null, "color"), 0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void regExpReturnedNullMatchEntries() {
		final PluginLocaleEntry entry = new PluginLocaleEntry("en");
		assertNull(entry.getRegExpReturned(-1));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void regExpReturnedDefaultIndex() {
		final PluginLocaleEntry entry = new PluginLocaleEntry("en", null, 0, "\\d{5}");
		assertEquals(entry.getRegExpReturned(-1), "\\d{5}");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void regExpReturnedSpecificIndex() {
		final PluginLocaleEntry entry = new PluginLocaleEntry("en", null, 0, "\\d{5}");
		assertEquals(entry.getRegExpReturned(0), "\\d{5}");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void isRegExpCompleteDefault() {
		final PluginLocaleEntry entry = new PluginLocaleEntry("en", null, 0, "\\d{5}");
		assertFalse(entry.isRegExpComplete(-1));
		assertFalse(entry.isRegExpComplete(0));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void matchEntryIndexSearchFound() {
		final PluginLocaleEntry entry = new PluginLocaleEntry("en", null, 0, "\\d{5}");
		assertEquals(entry.getMatchEntryIndex("\\d{5}", -1), 0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void matchEntryIndexSearchNotFound() {
		final PluginLocaleEntry entry = new PluginLocaleEntry("en", null, 0, "\\d{5}");
		assertEquals(entry.getMatchEntryIndex("\\d{9}", -1), -1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void matchEntryIndexFixedFound() {
		final PluginLocaleEntry entry = new PluginLocaleEntry("en", null, 0, "\\d{5}");
		assertEquals(entry.getMatchEntryIndex("\\d{5}", 0), 0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void matchEntryIndexFixedNotFound() {
		final PluginLocaleEntry entry = new PluginLocaleEntry("en", null, 0, "\\d{5}");
		assertEquals(entry.getMatchEntryIndex("\\d{9}", 0), -1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void copyConstructorFull() {
		final PluginLocaleEntry original = new PluginLocaleEntry("en", "(?i)color", 90, "\\p{Alpha}+");
		final PluginLocaleEntry copy = new PluginLocaleEntry(original);
		assertEquals(copy.localeTag, "en");
		assertEquals(copy.getHeaderConfidence(null, "color"), 90);
		assertEquals(copy.getRegExpReturned(-1), "\\p{Alpha}+");
		// deep copy: modifying original's tag doesn't affect copy
		original.localeTag = "fr";
		assertEquals(copy.localeTag, "en");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void copyConstructorNullHeaders() {
		final PluginLocaleEntry original = new PluginLocaleEntry("en", null, 0, "\\d+");
		final PluginLocaleEntry copy = new PluginLocaleEntry(original);
		assertNull(copy.headerRegExps);
		assertEquals(copy.getRegExpReturned(-1), "\\d+");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void simpleFactory() {
		final PluginLocaleEntry[] entries = PluginLocaleEntry.simple(new String[] { "en", "fr", "*" });
		assertEquals(entries.length, 3);
		assertEquals(entries[0].localeTag, "en");
		assertEquals(entries[1].localeTag, "fr");
		assertEquals(entries[2].localeTag, "*");
		assertNull(entries[0].headerRegExps);
		assertNull(entries[0].matchEntries);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void toStringWithHeaders() {
		final PluginLocaleEntry entry = new PluginLocaleEntry("en", "(?i)color", 90, "\\p{Alpha}+");
		final String s = entry.toString();
		assertTrue(s.startsWith("en:"), "toString must start with localeTag");
		assertTrue(s.contains("(?i)color"), "toString must include the header regexp");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void toStringWithoutHeaders() {
		final PluginLocaleEntry entry = new PluginLocaleEntry("en");
		assertEquals(entry.toString(), "en");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void defaultConstructor() {
		final PluginLocaleEntry entry = new PluginLocaleEntry();
		assertNull(entry.localeTag);
		assertNull(entry.headerRegExps);
		assertNull(entry.matchEntries);
		assertNotNull(entry);
	}
}
