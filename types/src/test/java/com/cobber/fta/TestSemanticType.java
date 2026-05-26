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
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.Locale;

import org.testng.annotations.Test;

public class TestSemanticType {

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void getAllSemanticTypesNonEmpty() {
		final List<SemanticType> all = SemanticType.getAllSemanticTypes();
		assertNotNull(all);
		assertFalse(all.isEmpty());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void getAllSemanticTypesCached() {
		// second call must return the exact same cached list
		final List<SemanticType> first = SemanticType.getAllSemanticTypes();
		final List<SemanticType> second = SemanticType.getAllSemanticTypes();
		assertSame(first, second);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void getActiveSemanticTypesNonEmpty() {
		final List<SemanticType> active = SemanticType.getActiveSemanticTypes(Locale.forLanguageTag("en-US"));
		assertNotNull(active);
		assertFalse(active.isEmpty());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void gettersOnKnownType() {
		// EMAIL is always present in plugins.json
		SemanticType email = null;
		for (final SemanticType st : SemanticType.getAllSemanticTypes())
			if ("EMAIL".equals(st.getId())) {
				email = st;
				break;
			}
		assertNotNull(email, "EMAIL semantic type must be registered");
		assertEquals(email.getId(), "EMAIL");
		assertNotNull(email.getDescription());
		assertFalse(email.getDescription().isEmpty());
		assertNotNull(email.getLanguages());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void toJSONStringContainsId() {
		SemanticType email = null;
		for (final SemanticType st : SemanticType.getAllSemanticTypes())
			if ("EMAIL".equals(st.getId())) {
				email = st;
				break;
			}
		assertNotNull(email);
		final String json = email.toJSONString();
		assertNotNull(json);
		assertTrue(json.contains("EMAIL"), "toJSONString must include the semantic type ID");
		assertTrue(json.contains("Id:"), "toJSONString must include 'Id:' label");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void getDocumentationNullWhenNotSet() {
		// Most types don't have documentation — verify getDocumentation() handles null safely
		for (final SemanticType st : SemanticType.getAllSemanticTypes()) {
			// just accessing it must not throw; it may be null or non-null
			final String[] docs = st.getDocumentation();
			if (docs != null)
				assertTrue(docs.length > 0);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void allTypesHaveNonNullId() {
		for (final SemanticType st : SemanticType.getAllSemanticTypes())
			assertNotNull(st.getId());
	}
}
