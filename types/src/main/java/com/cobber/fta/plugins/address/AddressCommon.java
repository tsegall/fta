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
package com.cobber.fta.plugins.address;

import java.util.HashSet;
import java.util.Set;

import com.cobber.fta.core.Utils;

public class AddressCommon {
	public static final String POBOX = "P.? ?O.?.?BOX.|POST OFFICE BOX";

	public static final String[] SAMPLE_STREETS = {
			"Main",  "Lakeside", "Pennsylvania", "Penaton", "Croydon", "Buchanan", "Riverside", "Flushing",
			"Jefferson", "Randolph", "North Lakeside", "Massachusetts", "Central", "Lincoln", "Final Mile",
			"4th", "Flower", "High", "3rd", "12th", "D", "Piedmont", "Chaton", "Kenwood", "Sycamore Lake",
			"Euclid", "Cedarstone", "Carriage", "Major Grey", "Armory", "Bryan", "Charack",
			"Atha", "Bassel", "Overlook", "Chatham", "Melville", "Stone", "Dawson", "Pringle", "Federation",
			"Winifred", "Pratt", "Hillview", "Rosemont", "Romines Mill", "School House", "Candlelight"
	};

	private static final Set<String> DIRECTIONS = new HashSet<>();
	private static final Set<String> modifiersWithArgument = new HashSet<>();
	private static final Set<String> MODIFIERS_ANY = new HashSet<>();
	private static final Set<String> TEXT_DIGIT = new HashSet<>();
	private static final Set<String> ORDINAL = new HashSet<>();
	private static final Set<String> ORDINAL_INDICATOR = new HashSet<>();
	private static final Set<String> INITIAL_MARKER = new HashSet<>();

	static {
		DIRECTIONS.add("N");
		DIRECTIONS.add("NE");
		DIRECTIONS.add("NW");
		DIRECTIONS.add("S");
		DIRECTIONS.add("SE");
		DIRECTIONS.add("SW");
		DIRECTIONS.add("E");
		DIRECTIONS.add("W");
		DIRECTIONS.add("NORTH");
		DIRECTIONS.add("NORTHEAST");
		DIRECTIONS.add("NORTHWEST");
		DIRECTIONS.add("SOUTH");
		DIRECTIONS.add("SOUTHEAST");
		DIRECTIONS.add("SOUTHWEST");
		DIRECTIONS.add("EAST");
		DIRECTIONS.add("WEST");
		DIRECTIONS.add("NB");
		DIRECTIONS.add("SB");
		DIRECTIONS.add("EB");
		DIRECTIONS.add("WB");

		modifiersWithArgument.add("APARTMENT");
		modifiersWithArgument.add("APT");
		modifiersWithArgument.add("BLDG");
		modifiersWithArgument.add("BOX");
		modifiersWithArgument.add("LEVEL");
		modifiersWithArgument.add("OFFICE");
		modifiersWithArgument.add("ROOM");
		modifiersWithArgument.add("STE");
		modifiersWithArgument.add("SUITE");
		modifiersWithArgument.add("UNIT");
		MODIFIERS_ANY.add("FL");
		MODIFIERS_ANY.add("FLOOR");
		MODIFIERS_ANY.add("LBBY");
		MODIFIERS_ANY.add("LOBBY");

		TEXT_DIGIT.add("ONE");
		TEXT_DIGIT.add("TWO");
		TEXT_DIGIT.add("THREE");
		TEXT_DIGIT.add("FOUR");
		TEXT_DIGIT.add("FIVE");
		TEXT_DIGIT.add("SIX");
		TEXT_DIGIT.add("SEVEN");
		TEXT_DIGIT.add("EIGHT");
		TEXT_DIGIT.add("NINE");
		TEXT_DIGIT.add("TEN");

		ORDINAL.add("FIRST");
		ORDINAL.add("SECOND");
		ORDINAL.add("THIRD");
		ORDINAL.add("FOURTH");
		ORDINAL.add("FIFTH");
		ORDINAL.add("SIXTH");
		ORDINAL.add("SEVENTH");
		ORDINAL.add("EIGHT");
		ORDINAL.add("NINTH");
		ORDINAL.add("TENTH");

		ORDINAL_INDICATOR.add("ST");
		ORDINAL_INDICATOR.add("ND");
		ORDINAL_INDICATOR.add("RD");
		ORDINAL_INDICATOR.add("TH");

		INITIAL_MARKER.add("AVENUE");
		INITIAL_MARKER.add("AVE");
		INITIAL_MARKER.add("CR");
		INITIAL_MARKER.add("HIGHWAY");
		INITIAL_MARKER.add("HWY");
		INITIAL_MARKER.add("INTERSTATE");
		INITIAL_MARKER.add("I");
		INITIAL_MARKER.add("ROUTE");
		INITIAL_MARKER.add("SR");
	}

	public static boolean isDirection(final String input) {
		return DIRECTIONS.contains(input);
	}

	public static boolean isModifier(final String input, final boolean isLastWord) {
		if (isLastWord)
			return MODIFIERS_ANY.contains(input);
		return modifiersWithArgument.contains(input);
	}

	public static boolean isInitialMarker(final String input) {
		return INITIAL_MARKER.contains(input);
	}

	/**
	 * Attempt to determine if the input is 'numeric' in the context of an address.
	 * For example, in '110A Eton Drive', 'One Broadway', '45 Penaton St', '312-314 Kings Rd.', '5/2 Hanlon Crescent' are all valid
	 * addresses and should return true.
	 * @param input The input to test.
	 * @return True if the input looks plausible as a 'numeric'.
	 */
	public static boolean isAddressNumber(final String input) {
		if (input == null || input.length() == 0)
			return false;

		// Check for a simple text digit - e.g. 'One', 'Two' etc.
		final char firstCh = input.charAt(0);
		if (Character.isAlphabetic(firstCh) && isTextDigit(input))
			return true;

		final int len = input.length();
		final char last = input.charAt(len - 1);
		String toTest = input;

		if (len > 1 && Character.isAlphabetic(last)) {
			final String rest = input.substring(0, len - 1);
			if (!Utils.isNumeric(rest))
				return false;
			toTest = rest;
		}

		if (!Character.isDigit(firstCh))
			return false;

		boolean firstNumber = false;
		boolean secondNumber = false;
		boolean separator = false;
		for (int i = 0; i < toTest.length(); i++) {
			final char ch = toTest.charAt(i);
			if (!Character.isDigit(ch)) {
				if (ch != '-' && ch != '/')
					return false;
				if (!firstNumber || secondNumber)
					return false;
				separator = true;
			}
			else {
				if (!firstNumber)
					firstNumber = true;
				else if (separator && !secondNumber)
					secondNumber = true;
			}
		}

		return true;
	}

	/**
	 * Attempt to determine if the input is a 'numeric' street name in the context of an address.
	 * For example, 110th, 2nd, Fourth.
	 * @param input The input to test.
	 * @return True if the input looks plausible as a 'numeric' street name.
	 */
	public static boolean numericStreetName(final String input) {
		if (input == null)
			return false;
		final int len = input.length();
		if (len == 0)
			return false;
		final char firstCh = input.charAt(0);
		if (Character.isLetter(firstCh))
			return ORDINAL.contains(input);
		if (!Character.isDigit(firstCh))
			return false;

		int digits = 0;
		int i = 0;

		while (i < len) {
			if (!Character.isDigit(input.charAt(i)))
				break;

			digits++;
			i++;
		}

		if (digits == 0 || i == len)
			return false;

		return ORDINAL_INDICATOR.contains(input.substring(i).trim());
	}

	/*
	 * Is the input string a single digit representation (e.g. ONE, TWO etc).
	 * @param input The input to test.
	 * @return True if the input is a representation of a single digit.
	 */
	private static boolean isTextDigit(final String input) {
		return TEXT_DIGIT.contains(input);
	}
}
