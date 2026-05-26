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
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.cobber.fta.core.HeaderEntry;

public class TestHeaderEntry {

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void defaultConstructor() {
		final HeaderEntry he = new HeaderEntry();
		assertNull(he.regExp);
		assertEquals(he.confidence, 0);
		assertFalse(he.mandatory);
		assertFalse(he.compositeKey);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void twoArgConstructorMatchesDataStreamName() {
		final HeaderEntry he = new HeaderEntry("(?i)email", 85);
		assertEquals(he.regExp, "(?i)email");
		assertEquals(he.confidence, 85);
		assertTrue(he.matches(null, "email"));
		assertTrue(he.matches(null, "EMAIL"));
		assertFalse(he.matches(null, "phone"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void copyConstructorLazyPatternInit() {
		// The copy constructor does NOT copy the compiled Pattern — it is re-compiled lazily on first matches() call
		final HeaderEntry original = new HeaderEntry("(?i)email", 85);
		final HeaderEntry copy = new HeaderEntry(original);
		assertEquals(copy.regExp, "(?i)email");
		assertEquals(copy.confidence, 85);
		assertFalse(copy.mandatory);
		// First call triggers lazy pattern compilation
		assertTrue(copy.matches(null, "email"));
		// Second call reuses the compiled pattern
		assertFalse(copy.matches(null, "phone"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void compositeKeyMatchesCombinedName() {
		final HeaderEntry he = new HeaderEntry("(?i)person\\.email", 90);
		he.compositeKey = true;
		// compositeKey=true with non-null compositeName → matches "person.email"
		assertTrue(he.matches("person", "email"));
		assertFalse(he.matches("order", "email"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void compositeKeyNullCompositeFallsBackToDataStream() {
		// When compositeKey=true but compositeName is null, match falls back to dataStreamName
		final HeaderEntry he = new HeaderEntry("(?i)email", 85);
		he.compositeKey = true;
		assertTrue(he.matches(null, "email"));
		assertFalse(he.matches(null, "phone"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void toStringContainsRegExpAndConfidence() {
		final HeaderEntry he = new HeaderEntry("(?i)email", 85);
		final String s = he.toString();
		assertTrue(s.contains("(?i)email"), "toString must include regExp");
		assertTrue(s.contains("85"), "toString must include confidence");
	}
}
