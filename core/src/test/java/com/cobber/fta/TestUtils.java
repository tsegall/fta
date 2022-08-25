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

import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;

import org.testng.annotations.Test;

import com.cobber.fta.core.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestUtils {
	private final ObjectMapper mapper = new ObjectMapper();

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void base64() {
		final HashMap<String, Long> cardinality = new HashMap<>();
		cardinality.put("SGVsbG8=", 2000000L);
		cardinality.put("V29ybGQ=", 2000000L);

		String result = Utils.determineStreamFormat(mapper, cardinality);
		assertEquals(result, "Base64");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void JSON() {
		final HashMap<String, Long> cardinality = new HashMap<>();
		cardinality.put("{ \"name\": \"Tim\" }", 2000000L);
		cardinality.put("{ \"name\": \"Anna\" }", 2000000L);
		cardinality.put("{ \"name\": \"Bill\" }", 2000000L);

		String result = Utils.determineStreamFormat(mapper, cardinality);
		assertEquals(result, "JSON");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void wellFormedHTML() {
		final HashMap<String, Long> cardinality = new HashMap<>();
		cardinality.put("<!DOCTYPE html><html></html>", 2000000L);
		cardinality.put("<head><title>My fabulous blog</title></head>", 2000000L);

		String result = Utils.determineStreamFormat(mapper, cardinality);
		assertEquals(result, "XML");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void HTML() {
		final HashMap<String, Long> cardinality = new HashMap<>();
		cardinality.put("<p>Hello", 2000000L);
		cardinality.put("<head><title>My fabulous blog</title></head>", 2000000L);

		String result = Utils.determineStreamFormat(mapper, cardinality);
		assertEquals(result, "HTML");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void wordsHelloWorld() {
		final List<String> words = Utils.asWords("Hello world!");
		assertEquals(words.get(0), "Hello");
		assertEquals(words.get(1), "world");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void wordsCount() {
		final List<String> words = Utils.asWords("   One, two, three,four!");
		assertEquals(words.get(0), "One");
		assertEquals(words.get(1), "two");
		assertEquals(words.get(2), "three");
		assertEquals(words.get(3), "four");
	}
}
