/*
 * Copyright 2017-2023 Tim Segall
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
package com.cobber.fta.core;

public class RegExpSplitter {
	private final int min;
	private final int max;
	private int length;
	private String regExp;

	RegExpSplitter(final int min, final int max) {
		this.min = min;
		this.max = max;
	}

	public static RegExpSplitter newInstance(final String input) {
		if (input == null || input.length() == 0 || input.charAt(0) != '{')
			return null;

		final int open = input.indexOf('{');
		final int close = input.indexOf('}');
		if (open == -1 || close == -1)
			return null;

		final int comma = input.indexOf(',');
		RegExpSplitter facts;
		if (comma != -1)
			facts = new RegExpSplitter(Utils.getValue(input, 1, 1, comma - 1), Utils.getValue(input, comma + 1, 1, close - (comma + 1)));
		else {
			final int len = Utils.getValue(input, 1, 1, close - 1);
			facts = new RegExpSplitter(len, len);
		}

		facts.length = close + 1;
		facts.regExp = input.substring(0, open);

		return facts;
	}

	public static String qualify(final int min, final int max) {
		if (max == 1 && max == min)
			return "";

		String ret = "{" + min;
		if (min != max)
			ret += "," + max;
		ret += "}";

		return ret;
	}

	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
	}

	public int getLength() {
		return length;
	}
}
