/*
 * Copyright 2017-2020 Tim Segall
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

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class Utils {

	private Utils() {
		// Never called
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(final Map<K, V> map) {
		return map.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	public static String replaceFirst(String input, String oldString, String newString) {
		int index = input.indexOf(oldString);
		if (index == -1)
			return input;

		return input.substring(0, index)
		        .concat(newString)
		        .concat(input.substring(index + oldString.length()));
	}

	public static String replaceAt(String input, int offset, int length, String newString) {
		return input.substring(0, offset)
		        .concat(newString)
		        .concat(input.substring(offset + length));
	}

	public static String repeat(final char c, final int count) {
		StringBuilder s = new StringBuilder(c);
		for (int i = 0; i < count; i++)
			s.append(c);
		return s.toString();
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

	public static boolean allZeroes(String input) {
		if (input == null)
			return false;

		for (int i = 0; i < input.length(); i++)
			if (input.charAt(i) != '0')
				return false;

		return true;
	}
}
