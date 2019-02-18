/*
 * Copyright 2017-2018 Tim Segall
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

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Utils {

	public static <K, V extends Comparable<? super V>> HashMap<K, V> sortByValue(final Map<K, V> map) {
		return map.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	public static String regExpLength(int min, int max) {
		String ret = "{" + min;
		if (min != max)
			ret += "," + max;
		ret += "}";

		return ret;
	}

	public static boolean getLength(RegExpLength facts, String input) {
		if (input == null || input.length() == 0 || input.charAt(0) != '{')
			return false;

		int comma = input.indexOf(',');
		int close = input.indexOf('}');
		if (close == -1)
			return false;

		if (comma != -1) {
			facts.min = getValue(input, 1, 1, comma - 1);
			facts.max = getValue(input, comma + 1, 1, close - comma);
		}
		else {
			facts.min = getValue(input, 1, 1, close - 1);
			facts.max = facts.min;
		}

		facts.length = close + 1;

		return true;
	}

	public static String replaceFirst(String input, String oldString, String newString) {
		int index = input.indexOf(oldString);
		if (index == -1)
			return input;

		return input.substring(0, index)
		        .concat(newString)
		        .concat(input.substring(index + oldString.length()));
	}

	/**
	 * Give a String as input with an offset and length return the integer at that position.
	 * @param input String to extract integer from
	 * @param offset Integer offset that marks the start
	 * @param minLength minimum length of integer to be extracted.
	 * @param maxLength maximum length of integer to be extracted.
	 * @return An integer value from the supplied String.
	 */
	public static int getValue(final String input, final int offset, final int minLength, int maxLength) {
		try {
			if (minLength == maxLength || (offset + maxLength > input.length()) || !Character.isDigit(input.charAt(offset + maxLength - 1)))
				return Integer.valueOf(input.substring(offset, offset + minLength));

			return Integer.valueOf(input.substring(offset, offset + maxLength));
		}
		catch (NumberFormatException e) {
			return -1;
		}
	}
}
