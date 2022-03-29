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
package com.cobber.fta.core;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A Utility class with a set of helper functions.
 */
public final class Utils {

	private Utils() {
		// Never called
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(final Map<K, V> map) {
		return map.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	public static String replaceFirst(final String input, final String oldString, final String newString) {
		final int index = input.indexOf(oldString);
		if (index == -1)
			return input;

		return input.substring(0, index)
				.concat(newString)
				.concat(input.substring(index + oldString.length()));
	}

	public static String replaceAll(final String input, final String oldString, final String newString) {
		int index = input.indexOf(oldString);
		String ret = input;
		do {
			if (index == -1)
				return input;

			ret = ret.substring(0, index)
					.concat(newString)
					.concat(ret.substring(index + oldString.length()));
		} while ((index = ret.indexOf(oldString)) != -1);

		return ret;
	}

	public static String replaceAt(final String input, final int offset, final int length, final String newString) {
		return input.substring(0, offset)
				.concat(newString)
				.concat(input.substring(offset + length));
	}

	/**
	 * Return a String with the provided character repeated &lt;count&gt; times;.
	 * @param c The Character to repeat
	 * @param count The number of time to repeat the character.
	 * @return The String with &lt;count&gt; occurrences of the supplied character.
	 */
	public static String repeat(final char c, final int count) {
		if (count == 1)
			return String.valueOf(c);

		if (count == 2)
			return String.valueOf(new char[] {c, c});

		final StringBuilder s = new StringBuilder(count);
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
	public static int getValue(final String input, final int offset, final int minLength, final int maxLength) {
		try {
			if (minLength == maxLength || (offset + maxLength > input.length()) || !Character.isDigit(input.charAt(offset + maxLength - 1)))
				return Integer.valueOf(input.substring(offset, offset + minLength));

			return Integer.valueOf(input.substring(offset, offset + maxLength));
		}
		catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * Test if the supplied input is a string of all 0's.
	 * @param input String to test.
	 * @return True if input is a string of 0's.
	 */
	public static boolean allZeroes(final String input) {
		if (input == null)
			return false;

		for (int i = 0; i < input.length(); i++)
			if (input.charAt(i) != '0')
				return false;

		return true;
	}

	/**
	 * Test if the supplied input is all numeric.
	 * @param input String to test.
	 * @return True if the string is all Numeric.
	 */
	public static boolean isNumeric(final String input) {
		if (input == null || input.isEmpty())
			return false;

		for (int i = 0; i < input.length(); i++)
			if (!Character.isDigit(input.charAt(i)))
				return false;

		return true;
	}

	public static String getBaseName(final String fileName) {
	    final int index = fileName.lastIndexOf('.');
        return index == -1 ? fileName : fileName.substring(0, index);
	}

	private static String version = Utils.class.getPackage().getImplementationVersion();

	/**
	 * Get the version of the FTA library.
	 * @return The version of the FTA library.
	 */
	public static String getVersion() {
		return version;
	}
}
