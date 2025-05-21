/*
 * Copyright 2017-2025 Tim Segall
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
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAPluginException;

public class TestTopBottomK {
	private static final SecureRandom random = new SecureRandom();

	@Test(groups = { TestGroups.ALL })
	public void basicZips() throws IOException {
		final String[] candidatesZips = TestUtils.validZips.split("\\|");
		final Set<String> topK = new HashSet<>(Arrays.asList(
				"34731", "35053", "35221", "35491", "35752", "36022", "36460", "36616", "36860", "37087" ));
		final Set<String> bottomK = new HashSet<>(Arrays.asList(
				 "01770", "01772", "01773", "02027", "02030", "02170", "02379", "02657", "02861", "03216" ));

		final TopBottomK<String, String> t = new TopBottomK<>(10);

		for (final String s : candidatesZips) {
			t.observe(s);
		}

		assertEquals(t.topK(), topK);
		assertEquals(t.bottomK(), bottomK);
	}

	// Need to test the following equivalence classes for size n ...
	// 0, n - 1, n, n + 1, 2n - 1, 2n, 2n + 1

	@Test(groups = { TestGroups.ALL })
	public void basicIntegers0() throws IOException {
		final TopBottomK<Integer, Integer> t = new TopBottomK<>(10);
		final Set<Integer> topK = new HashSet<>();
		final Set<Integer> bottomK = new HashSet<>();

		assertEquals(t.topK(), topK);
		assertEquals(t.bottomK(), bottomK);
	}

	@Test(groups = { TestGroups.ALL })
	public void basicIntegers9() throws IOException {
		final int SIZE = 9;
		final TopBottomK<Integer, Integer> t = new TopBottomK<>(10);
		final Set<Integer> topK = new HashSet<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8));
		final Set<Integer> bottomK = new HashSet<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8));

		boolean[] used;
		int usedCount = 0;

		used = new boolean[SIZE];
		while (usedCount < SIZE) {
			final int r = random.nextInt(SIZE);
			if (!used[r]) {
				usedCount++;
				used[r] = true;
			}
			t.observe(r);
		}

		assertEquals(t.topK(), topK);
		assertEquals(t.bottomK(), bottomK);
	}

	@Test(groups = { TestGroups.ALL })
	public void basicIntegers10() throws IOException {
		final int SIZE = 10;
		final TopBottomK<Integer, Integer> t = new TopBottomK<>(10);
		final Set<Integer> topK = new HashSet<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
		final Set<Integer> bottomK = new HashSet<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

		boolean[] used;
		int usedCount = 0;

		used = new boolean[SIZE];
		while (usedCount < SIZE) {
			final int r = random.nextInt(SIZE);
			if (!used[r]) {
				usedCount++;
				used[r] = true;
			}
			t.observe(r);
		}

		assertEquals(t.topK(), topK);
		assertEquals(t.bottomK(), bottomK);
	}

	@Test(groups = { TestGroups.ALL })
	public void basicIntegers11() throws IOException {
		final int SIZE = 11;
		final TopBottomK<Integer, Integer> t = new TopBottomK<>(10);
		final Set<Integer> topK = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
		final Set<Integer> bottomK = new HashSet<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

		boolean[] used;
		int usedCount = 0;

		used = new boolean[SIZE];
		while (usedCount < SIZE) {
			final int r = random.nextInt(SIZE);
			if (!used[r]) {
				usedCount++;
				used[r] = true;
			}
			t.observe(r);
		}

		assertEquals(t.topK(), topK);
		assertEquals(t.bottomK(), bottomK);
	}

	@Test(groups = { TestGroups.ALL })
	public void basicIntegers19() throws IOException {
		final int SIZE = 19;
		final TopBottomK<Integer, Integer> t = new TopBottomK<>(10);
		final Set<Integer> topK = new HashSet<>(Arrays.asList(9, 10, 11, 12, 13, 14, 15, 16, 17, 18));
		final Set<Integer> bottomK = new HashSet<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

		boolean[] used;
		int usedCount = 0;

		used = new boolean[SIZE];
		while (usedCount < SIZE) {
			final int r = random.nextInt(SIZE);
			if (!used[r]) {
				usedCount++;
				used[r] = true;
			}
			t.observe(r);
		}

		assertEquals(t.bottomK(), bottomK);
		assertEquals(t.topK(), topK);
	}

	@Test(groups = { TestGroups.ALL })
	public void basicIntegers20() throws IOException {
		final int SIZE = 20;
		final TopBottomK<Integer, Integer> t = new TopBottomK<>(10);
		final Set<Integer> topK = new HashSet<>(Arrays.asList(10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
		final Set<Integer> bottomK = new HashSet<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

		boolean[] used;
		int usedCount = 0;

		used = new boolean[SIZE];
		while (usedCount < SIZE) {
			final int r = random.nextInt(SIZE);
			if (!used[r]) {
				usedCount++;
				used[r] = true;
			}
			t.observe(r);
		}

		assertEquals(t.bottomK(), bottomK);
		assertEquals(t.topK(), topK);
	}

	@Test(groups = { TestGroups.ALL })
	public void basicBug() throws IOException {
		final int SIZE = 20;
		final TopBottomK<Integer, Integer> t = new TopBottomK<>(10);
		final Set<Integer> topK = new HashSet<>(Arrays.asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
		final Set<Integer> bottomK = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

		for (int i = 1; i <= SIZE; i++)
			t.observe(i);

		assertEquals(t.bottomK(), bottomK);
		assertEquals(t.topK(), topK);

		// So sets are equal - but we need also need to check that they are correctly ordered
		// Should be lowest to highest
		int current = Integer.MIN_VALUE;
		for (final Integer i : t.bottomK()) {
			assertTrue(i >= current);
			current = i;
		}
		current = Integer.MIN_VALUE;
		// Should be lowest to highest
		for (final Integer i : t.topK()) {
			assertTrue(i >= current);
			current = i;
		}

		assertEquals(t.bottomKasString(), new TreeSet<>(t.bottomK().stream().map(x->x.toString()).collect(Collectors.toSet())));
		assertEquals(t.topKasString(), new TreeSet<>(t.topK().stream().map(x->x.toString()).collect(Collectors.toSet())));

		// So sets are equal - but we need also need to check that they are correctly ordered
		// Should be lowest to highest
		current = Integer.MIN_VALUE;
		for (final String i : t.bottomKasString()) {
			assertTrue(Integer.parseInt(i) >= current);
			current = Integer.parseInt(i);
		}
		current = Integer.MAX_VALUE;
		// Should be highest to lowest
		for (final String i : t.topKasString()) {
			assertTrue(Integer.parseInt(i) <= current);
			current = Integer.parseInt(i);
		}
	}

	@Test(groups = { TestGroups.ALL })
	public void basicIntegers21() throws IOException {
		final int SIZE = 21;
		final TopBottomK<Integer, Integer> t = new TopBottomK<>(10);
		final Set<Integer> topK = new HashSet<>(Arrays.asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
		final Set<Integer> bottomK = new HashSet<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

		boolean[] used;
		int usedCount = 0;

		used = new boolean[SIZE];
		while (usedCount < SIZE) {
			final int r = random.nextInt(SIZE);
			if (!used[r]) {
				usedCount++;
				used[r] = true;
			}
			t.observe(r);
		}

		assertEquals(t.bottomK(), bottomK);
		assertEquals(t.topK(), topK);
	}

	@Test(groups = { TestGroups.ALL })
	public void basicIntegers100() throws IOException {
		final int SIZE = 100;
		final TopBottomK<Integer, Integer> t = new TopBottomK<>(10);
		final Set<Integer> topK = new HashSet<>(Arrays.asList(99, 98, 97, 96, 95, 94, 93, 92, 91, 90));
		final Set<Integer> bottomK = new HashSet<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

		boolean[] used;
		int usedCount = 0;

		used = new boolean[SIZE];
		while (usedCount < SIZE) {
			final int r = random.nextInt(SIZE);
			if (!used[r]) {
				usedCount++;
				used[r] = true;
			}
			t.observe(r);
		}

		assertEquals(t.topK(), topK);
		assertEquals(t.bottomK(), bottomK);
	}

	@Test(groups = { TestGroups.ALL })
	public void basicIntegers11_size5() throws IOException {
		final int SIZE = 11;
		final TopBottomK<Integer, Integer> t = new TopBottomK<>(5);
		final Set<Integer> topK = new HashSet<>(Arrays.asList(6, 7, 8, 9, 10));
		final Set<Integer> bottomK = new HashSet<>(Arrays.asList(0, 1, 2, 3, 4));

		boolean[] used;
		int usedCount = 0;

		used = new boolean[SIZE];
		while (usedCount < SIZE) {
			final int r = random.nextInt(SIZE);
			if (!used[r]) {
				usedCount++;
				used[r] = true;
			}
			t.observe(r);
		}

		assertEquals(t.topK(), topK);
		assertEquals(t.bottomK(), bottomK);
	}

	@Test(groups = { TestGroups.ALL })
	public void basicLongs100() throws IOException {
		final TopBottomK<Long, Long> t = new TopBottomK<>(10);
		final Set<Long> topK = new HashSet<>(Arrays.asList(99L, 98L, 97L, 96L, 95L, 94L, 93L, 92L, 91L, 90L));
		final Set<Long> bottomK = new HashSet<>(Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L));

		boolean[] used;
		int usedCount = 0;

		used = new boolean[100];
		while (usedCount < 100) {
			final int r = random.nextInt(100);
			if (!used[r]) {
				usedCount++;
				used[r] = true;
			}
			t.observe((long)r);
		}

		assertEquals(t.topK(), topK);
		assertEquals(t.bottomK(), bottomK);
	}

	@Test(groups = { TestGroups.ALL })
	public void basicDoubles100() throws IOException {
		final TopBottomK<Double, Double> t = new TopBottomK<>(10);
		final Set<Double> topK = new HashSet<>(Arrays.asList(99.0, 98.0, 97.0, 96.0, 95.0, 94.0, 93.0, 92.0, 91.0, 90.0));
		final Set<Double> bottomK = new HashSet<>(Arrays.asList(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0));

		boolean[] used;
		int usedCount = 0;

		used = new boolean[100];
		while (usedCount < 100) {
			final int r = random.nextInt(100);
			if (!used[r]) {
				usedCount++;
				used[r] = true;
			}
			t.observe((double)r);
		}

		assertEquals(t.topK(), topK);
		assertEquals(t.bottomK(), bottomK);
	}

	@Test(groups = { TestGroups.ALL })
	public void basicFirstNames() throws IOException, FTAPluginException {
		final TopBottomK<String, String> t = new TopBottomK<>(10);
		final Set<String> bottomK = new HashSet<>(Arrays.asList(
				"AALIYAH", "AARON", "AARYA", "AARÓN", "ABBEY", "ABBIE", "ABBY", "ABDALLAH", "ABDUL", "ABDULLAH" ));
		final Set<String> topK = new HashSet<>(Arrays.asList(
				"ÉLIA", "ÉLINA", "ÉLYAS", "ÉLÉA", "ÉLÉNA", "ÉMILIA", "ÉRICA", "ÍRIS", "ÍSIS", "ÖMER" ));

		final LogicalTypeCode logicalFirst = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByName("NAME.FIRST"), new AnalysisConfig());

		for (int i = 0; i < 100000; i++)
			t.observe(logicalFirst.nextRandom());

		assertEquals(t.topK(), topK);
		assertEquals(t.bottomK(), bottomK);
	}
}
