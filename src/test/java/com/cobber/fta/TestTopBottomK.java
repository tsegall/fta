package com.cobber.fta;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestTopBottomK {
	@Test
	public void basicZips() throws IOException {
		String[] candidatesZips = TestUtils.validZips.split("\\|");
		HashSet<String> topK = new HashSet<>(Arrays.asList(new String[] {
				"34731", "35053", "35221", "35491", "35752", "36022", "36460", "36616", "36860", "37087" } ));
		HashSet<String> bottomK = new HashSet<>(Arrays.asList(new String[] {
				 "01770", "01772", "01773", "02027", "02030", "02170", "02379", "02657", "02861", "03216" } ));

		TopBottomK<String, String> t = new TopBottomK<>();

		for (String s : candidatesZips) {
			t.observe(s);
		}

		Assert.assertEquals(t.topK(), topK);
		Assert.assertEquals(t.bottomK(), bottomK);
	}

	// Need to test the following equivalence classes for size n ...
	// 0, n - 1, n, n + 1, 2n - 1, 2n, 2n + 1

	@Test
	public void basicIntegers0() throws IOException {
		TopBottomK<Integer, Integer> t = new TopBottomK<>();
		HashSet<Integer> topK = new HashSet<>();
		HashSet<Integer> bottomK = new HashSet<>();

		Assert.assertEquals(t.topK(), topK);
		Assert.assertEquals(t.bottomK(), bottomK);
	}

	@Test
	public void basicIntegers9() throws IOException {
		final int SIZE = 9;
		TopBottomK<Integer, Integer> t = new TopBottomK<>();
		Random random = new Random(662607004);
		HashSet<Integer> topK = new HashSet<>(Arrays.asList(new Integer[] {0, 1, 2, 3, 4, 5, 6, 7, 8} ));
		HashSet<Integer> bottomK = new HashSet<>(Arrays.asList(new Integer[] {0, 1, 2, 3, 4, 5, 6, 7, 8} ));

		boolean[] used;
		int usedCount = 0;

		used = new boolean[SIZE];
		while (usedCount < SIZE) {
			int r = random.nextInt(SIZE);
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
		TopBottomK<Integer, Integer> t = new TopBottomK<>();
		Random random = new Random(662607004);
		HashSet<Integer> topK = new HashSet<>(Arrays.asList(new Integer[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9} ));
		HashSet<Integer> bottomK = new HashSet<>(Arrays.asList(new Integer[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9} ));

		boolean[] used;
		int usedCount = 0;

		used = new boolean[SIZE];
		while (usedCount < SIZE) {
			int r = random.nextInt(SIZE);
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
		TopBottomK<Integer, Integer> t = new TopBottomK<>();
		Random random = new Random(662607004);
		HashSet<Integer> topK = new HashSet<>(Arrays.asList(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10} ));
		HashSet<Integer> bottomK = new HashSet<>(Arrays.asList(new Integer[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9} ));

		boolean[] used;
		int usedCount = 0;

		used = new boolean[SIZE];
		while (usedCount < SIZE) {
			int r = random.nextInt(SIZE);
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
		TopBottomK<Integer, Integer> t = new TopBottomK<>();
		Random random = new Random(662607004);
		HashSet<Integer> topK = new HashSet<>(Arrays.asList(new Integer[] {9, 10, 11, 12, 13, 14, 15, 16, 17, 18} ));
		HashSet<Integer> bottomK = new HashSet<>(Arrays.asList(new Integer[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9} ));

		boolean[] used;
		int usedCount = 0;

		used = new boolean[SIZE];
		while (usedCount < SIZE) {
			int r = random.nextInt(SIZE);
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
		TopBottomK<Integer, Integer> t = new TopBottomK<>();
		Random random = new Random(662607004);
		HashSet<Integer> topK = new HashSet<>(Arrays.asList(new Integer[] {10, 11, 12, 13, 14, 15, 16, 17, 18, 19} ));
		HashSet<Integer> bottomK = new HashSet<>(Arrays.asList(new Integer[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9} ));

		boolean[] used;
		int usedCount = 0;

		used = new boolean[SIZE];
		while (usedCount < SIZE) {
			int r = random.nextInt(SIZE);
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
		TopBottomK<Integer, Integer> t = new TopBottomK<>();
		Random random = new Random(662607004);
		HashSet<Integer> topK = new HashSet<>(Arrays.asList(new Integer[] {11, 12, 13, 14, 15, 16, 17, 18, 19, 20} ));
		HashSet<Integer> bottomK = new HashSet<>(Arrays.asList(new Integer[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9} ));

		boolean[] used;
		int usedCount = 0;

		used = new boolean[SIZE];
		while (usedCount < SIZE) {
			int r = random.nextInt(SIZE);
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
		TopBottomK<Integer, Integer> t = new TopBottomK<>();
		Random random = new Random(662607004);
		HashSet<Integer> topK = new HashSet<>(Arrays.asList(new Integer[] {99, 98, 97, 96, 95, 94, 93, 92, 91, 90} ));
		HashSet<Integer> bottomK = new HashSet<>(Arrays.asList(new Integer[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9} ));

		boolean[] used;
		int usedCount = 0;

		used = new boolean[SIZE];
		while (usedCount < SIZE) {
			int r = random.nextInt(SIZE);
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
		TopBottomK<Integer, Integer> t = new TopBottomK<>(5);
		Random random = new Random(662607004);
		HashSet<Integer> topK = new HashSet<>(Arrays.asList(new Integer[] { 6, 7, 8, 9, 10} ));
		HashSet<Integer> bottomK = new HashSet<>(Arrays.asList(new Integer[] {0, 1, 2, 3, 4} ));

		boolean[] used;
		int usedCount = 0;

		used = new boolean[SIZE];
		while (usedCount < SIZE) {
			int r = random.nextInt(SIZE);
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
		TopBottomK<Long, Long> t = new TopBottomK<>();
		Random random = new Random(662607004);
		HashSet<Long> topK = new HashSet<>(Arrays.asList(new Long[] {99L, 98L, 97L, 96L, 95L, 94L, 93L, 92L, 91L, 90L} ));
		HashSet<Long> bottomK = new HashSet<>(Arrays.asList(new Long[] {0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L} ));

		boolean[] used;
		int usedCount = 0;

		used = new boolean[100];
		while (usedCount < 100) {
			int r = random.nextInt(100);
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
		TopBottomK<Double, Double> t = new TopBottomK<>();
		Random random = new Random(662607004);
		HashSet<Double> topK = new HashSet<>(Arrays.asList(new Double[] {99.0, 98.0, 97.0, 96.0, 95.0, 94.0, 93.0, 92.0, 91.0, 90.0} ));
		HashSet<Double> bottomK = new HashSet<>(Arrays.asList(new Double[] {0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0} ));

		boolean[] used;
		int usedCount = 0;

		used = new boolean[100];
		while (usedCount < 100) {
			int r = random.nextInt(100);
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
	public void basicFirstNames() throws IOException {
		TopBottomK<String, String> t = new TopBottomK<>();
		HashSet<String> bottomK = new HashSet<>(Arrays.asList(new String[] {
				"AARON", "ABBEY", "ABBIE", "ABBY", "ABDUL", "ABE", "ABEL", "ABIGAIL", "ABRAHAM", "ABRAM" } ));
		HashSet<String> topK = new HashSet<>(Arrays.asList(new String[] {
				"ZOE", "ZOFIA", "ZOILA", "ZOLA", "ZONA", "ZORA", "ZORAIDA", "ZULA", "ZULEMA", "ZULMA" } ));

		PluginDefinition pluginFirst = new PluginDefinition("NAME.FIRST", "com.cobber.fta.plugins.LogicalTypeFirstName");
		LogicalTypeCode logicalFirst = (LogicalTypeCode) LogicalTypeFactory.newInstance(pluginFirst, Locale.getDefault());

		for (int i = 0; i < 100000; i++)
			t.observe(logicalFirst.nextRandom());

		Assert.assertEquals(t.topK(), topK);
		Assert.assertEquals(t.bottomK(), bottomK);
	}
}