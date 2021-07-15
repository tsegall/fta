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
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.cobber.fta.core.FTAPluginException;

public class TestTopBottomK {
	private static final SecureRandom random = new SecureRandom();

	@Test
	public void basicZips() throws IOException {
		final String[] candidatesZips = TestUtils.validZips.split("\\|");
		final HashSet<String> topK = new HashSet<>(Arrays.asList(new String[] {
				"34731", "35053", "35221", "35491", "35752", "36022", "36460", "36616", "36860", "37087" } ));
		final HashSet<String> bottomK = new HashSet<>(Arrays.asList(new String[] {
				 "01770", "01772", "01773", "02027", "02030", "02170", "02379", "02657", "02861", "03216" } ));

		final TopBottomK<String, String> t = new TopBottomK<>();

		for (final String s : candidatesZips) {
			t.observe(s);
		}

		Assert.assertEquals(t.topK(), topK);
		Assert.assertEquals(t.bottomK(), bottomK);
	}

	// Need to test the following equivalence classes for size n ...
	// 0, n - 1, n, n + 1, 2n - 1, 2n, 2n + 1

	@Test
	public void basicIntegers0() throws IOException {
		final TopBottomK<Integer, Integer> t = new TopBottomK<>();
		final HashSet<Integer> topK = new HashSet<>();
		final HashSet<Integer> bottomK = new HashSet<>();

		Assert.assertEquals(t.topK(), topK);
		Assert.assertEquals(t.bottomK(), bottomK);
	}

	@Test
	public void basicIntegers9() throws IOException {
		final int SIZE = 9;
		final TopBottomK<Integer, Integer> t = new TopBottomK<>();
		final HashSet<Integer> topK = new HashSet<>(Arrays.asList(new Integer[] {0, 1, 2, 3, 4, 5, 6, 7, 8} ));
		final HashSet<Integer> bottomK = new HashSet<>(Arrays.asList(new Integer[] {0, 1, 2, 3, 4, 5, 6, 7, 8} ));

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

		Assert.assertEquals(t.topK(), topK);
		Assert.assertEquals(t.bottomK(), bottomK);
	}

	@Test
	public void basicIntegers10() throws IOException {
		final int SIZE = 10;
		final TopBottomK<Integer, Integer> t = new TopBottomK<>();
		final HashSet<Integer> topK = new HashSet<>(Arrays.asList(new Integer[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9} ));
		final HashSet<Integer> bottomK = new HashSet<>(Arrays.asList(new Integer[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9} ));

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

		Assert.assertEquals(t.topK(), topK);
		Assert.assertEquals(t.bottomK(), bottomK);
	}

	@Test
	public void basicIntegers11() throws IOException {
		final int SIZE = 11;
		final TopBottomK<Integer, Integer> t = new TopBottomK<>();
		final HashSet<Integer> topK = new HashSet<>(Arrays.asList(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10} ));
		final HashSet<Integer> bottomK = new HashSet<>(Arrays.asList(new Integer[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9} ));

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

		Assert.assertEquals(t.topK(), topK);
		Assert.assertEquals(t.bottomK(), bottomK);
	}

	@Test
	public void basicIntegers19() throws IOException {
		final int SIZE = 19;
		final TopBottomK<Integer, Integer> t = new TopBottomK<>();
		final HashSet<Integer> topK = new HashSet<>(Arrays.asList(new Integer[] {9, 10, 11, 12, 13, 14, 15, 16, 17, 18} ));
		final HashSet<Integer> bottomK = new HashSet<>(Arrays.asList(new Integer[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9} ));

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

		Assert.assertEquals(t.bottomK(), bottomK);
		Assert.assertEquals(t.topK(), topK);
	}

	@Test
	public void basicIntegers20() throws IOException {
		final int SIZE = 20;
		final TopBottomK<Integer, Integer> t = new TopBottomK<>();
		final HashSet<Integer> topK = new HashSet<>(Arrays.asList(new Integer[] {10, 11, 12, 13, 14, 15, 16, 17, 18, 19} ));
		final HashSet<Integer> bottomK = new HashSet<>(Arrays.asList(new Integer[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9} ));

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

		Assert.assertEquals(t.bottomK(), bottomK);
		Assert.assertEquals(t.topK(), topK);
	}

	@Test
	public void basicIntegers21() throws IOException {
		final int SIZE = 21;
		final TopBottomK<Integer, Integer> t = new TopBottomK<>();
		final HashSet<Integer> topK = new HashSet<>(Arrays.asList(new Integer[] {11, 12, 13, 14, 15, 16, 17, 18, 19, 20} ));
		final HashSet<Integer> bottomK = new HashSet<>(Arrays.asList(new Integer[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9} ));

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

		Assert.assertEquals(t.bottomK(), bottomK);
		Assert.assertEquals(t.topK(), topK);
	}

	@Test
	public void basicIntegers100() throws IOException {
		final int SIZE = 100;
		final TopBottomK<Integer, Integer> t = new TopBottomK<>();
		final HashSet<Integer> topK = new HashSet<>(Arrays.asList(new Integer[] {99, 98, 97, 96, 95, 94, 93, 92, 91, 90} ));
		final HashSet<Integer> bottomK = new HashSet<>(Arrays.asList(new Integer[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9} ));

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

		Assert.assertEquals(t.topK(), topK);
		Assert.assertEquals(t.bottomK(), bottomK);
	}

	@Test
	public void basicIntegers11_size5() throws IOException {
		final int SIZE = 11;
		final TopBottomK<Integer, Integer> t = new TopBottomK<>(5);
		final HashSet<Integer> topK = new HashSet<>(Arrays.asList(new Integer[] { 6, 7, 8, 9, 10} ));
		final HashSet<Integer> bottomK = new HashSet<>(Arrays.asList(new Integer[] {0, 1, 2, 3, 4} ));

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

		Assert.assertEquals(t.topK(), topK);
		Assert.assertEquals(t.bottomK(), bottomK);
	}

	@Test
	public void basicLongs100() throws IOException {
		final TopBottomK<Long, Long> t = new TopBottomK<>();
		final HashSet<Long> topK = new HashSet<>(Arrays.asList(new Long[] {99L, 98L, 97L, 96L, 95L, 94L, 93L, 92L, 91L, 90L} ));
		final HashSet<Long> bottomK = new HashSet<>(Arrays.asList(new Long[] {0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L} ));

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

		Assert.assertEquals(t.topK(), topK);
		Assert.assertEquals(t.bottomK(), bottomK);
	}

	@Test
	public void basicDoubles100() throws IOException {
		final TopBottomK<Double, Double> t = new TopBottomK<>();
		final HashSet<Double> topK = new HashSet<>(Arrays.asList(new Double[] {99.0, 98.0, 97.0, 96.0, 95.0, 94.0, 93.0, 92.0, 91.0, 90.0} ));
		final HashSet<Double> bottomK = new HashSet<>(Arrays.asList(new Double[] {0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0} ));

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

		Assert.assertEquals(t.topK(), topK);
		Assert.assertEquals(t.bottomK(), bottomK);
	}

	@Test
	public void basicFirstNames() throws IOException, FTAPluginException {
		final TopBottomK<String, String> t = new TopBottomK<>();
		final HashSet<String> bottomK = new HashSet<>(Arrays.asList(new String[] {
				"AARON", "ABBEY", "ABBIE", "ABBY", "ABDUL", "ABE", "ABEL", "ABIGAIL", "ABRAHAM", "ABRAM" } ));
		final HashSet<String> topK = new HashSet<>(Arrays.asList(new String[] {
				"ZOE", "ZOFIA", "ZOILA", "ZOLA", "ZONA", "ZORA", "ZORAIDA", "ZULA", "ZULEMA", "ZULMA" } ));

		final PluginDefinition pluginFirst = new PluginDefinition("NAME.FIRST", "com.cobber.fta.plugins.LogicalTypeFirstName");
		final LogicalTypeCode logicalFirst = (LogicalTypeCode) LogicalTypeFactory.newInstance(pluginFirst, Locale.getDefault());

		for (int i = 0; i < 100000; i++)
			t.observe(logicalFirst.nextRandom());

		Assert.assertEquals(t.topK(), topK);
		Assert.assertEquals(t.bottomK(), bottomK);
	}
}
