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
import java.util.Random;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestShapes {
	private static String ALPHA = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static String NUMERIC = "0123456789";
	private static int MAX_SHAPES = 1000;

	@Test
	public void singleAlphaConstantLength() throws IOException {
		Shapes shapes = new Shapes(MAX_SHAPES);
		Random r = new Random(2089);

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 9; j++)
				b.append(ALPHA.charAt(r.nextInt(ALPHA.length())));
			shapes.track(b.toString(), 1);
			b.setLength(0);
		}

		Assert.assertEquals(shapes.getRegExp(), "\\p{IsAlphabetic}{9}");
	}

	@Test
	public void doubleAlphaConstantLength() throws IOException {
		Shapes shapes = new Shapes(MAX_SHAPES);
		Random r = new Random(2089);

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 9; j++)
				b.append(ALPHA.charAt(r.nextInt(ALPHA.length())));
			shapes.track(b.toString(), 1);
			b.setLength(0);
		}

		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 7; j++)
				b.append(ALPHA.charAt(r.nextInt(ALPHA.length())));
			shapes.track(b.toString(), 1);
			b.setLength(0);
		}

		Assert.assertEquals(shapes.getRegExp(), "\\p{IsAlphabetic}{7}|\\p{IsAlphabetic}{9}");
	}

	@Test
	public void multiAlphaConstantLength() throws IOException {
		Shapes shapes = new Shapes(MAX_SHAPES);
		Random r = new Random(2089);

		StringBuilder b = new StringBuilder();
		for (int j = 4; j < 9; j++) {
			for (int i = 0; i < 100; i++) {
				for (int l = 0; l < j; l++)
					b.append(ALPHA.charAt(r.nextInt(ALPHA.length())));
				shapes.track(b.toString(), 1);
				b.setLength(0);
			}
		}

		Assert.assertEquals(shapes.getRegExp(), "\\p{IsAlphabetic}+");
	}

	@Test
	public void singleNumericConstantLength() throws IOException {
		Shapes shapes = new Shapes(MAX_SHAPES);
		Random r = new Random(2089);

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 9; j++)
				b.append(NUMERIC.charAt(r.nextInt(NUMERIC.length())));
			shapes.track(b.toString(), 1);
			b.setLength(0);
		}

		Assert.assertEquals(shapes.getRegExp(), "\\d{9}");
	}

	@Test
	public void doubleNumericConstantLength() throws IOException {
		Shapes shapes = new Shapes(MAX_SHAPES);
		Random r = new Random(2089);

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 9; j++)
				b.append(NUMERIC.charAt(r.nextInt(NUMERIC.length())));
			shapes.track(b.toString(), 1);
			b.setLength(0);
		}

		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 7; j++)
				b.append(NUMERIC.charAt(r.nextInt(NUMERIC.length())));
			shapes.track(b.toString(), 1);
			b.setLength(0);
		}

		Assert.assertEquals(shapes.getRegExp(), "\\d{7}|\\d{9}");
	}

}
