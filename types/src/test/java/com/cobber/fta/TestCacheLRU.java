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
import static org.testng.Assert.assertNull;

import org.testng.annotations.Test;

public class TestCacheLRU {

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void invalidateRemovesEntry() {
		final CacheLRU<String, String> cache = new CacheLRU<>(10);
		cache.put("key1", "value1");
		cache.invalidate("key1");
		assertNull(cache.get("key1"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void sizeReflectsContent() {
		final CacheLRU<String, String> cache = new CacheLRU<>(10);
		cache.put("key1", "value1");
		cache.put("key2", "value2");
		cache.put("key3", "value3");
		assertEquals(cache.size(), 3L);
		cache.invalidate("key2");
		assertEquals(cache.size(), 2L);
	}
}
