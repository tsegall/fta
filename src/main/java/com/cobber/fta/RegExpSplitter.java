/*
 * Copyright 2017-2019 Tim Segall
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

public class RegExpSplitter {
	int min;
	int max;
	int length;
	String regExp;

	RegExpSplitter(int min, int max) {
		this.min = min;
		this.max = max;
	}

	public static RegExpSplitter newInstance(String input) {
		if (input == null || input.length() == 0 || input.charAt(0) != '{')
			return null;

		int open = input.indexOf('{');
		int comma = input.indexOf(',');
		int close = input.indexOf('}');
		if (open == -1 || close == -1)
			return null;

		RegExpSplitter facts;
		if (comma != -1)
			facts = new RegExpSplitter(Utils.getValue(input, 1, 1, comma - 1), Utils.getValue(input, comma + 1, 1, close - comma));
		else {
			int len = Utils.getValue(input, 1, 1, close - 1);
			facts = new RegExpSplitter(len, len);
		}

		facts.length = close + 1;
		facts.regExp = input.substring(0, open);

		return facts;
	}

	public static String qualify(int min, int max) {
		String ret = "{" + min;
		if (min != max)
			ret += "," + max;
		ret += "}";

		return ret;
	}
}
