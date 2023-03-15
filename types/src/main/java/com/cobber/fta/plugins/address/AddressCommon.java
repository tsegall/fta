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
package com.cobber.fta.plugins.address;

import java.util.HashSet;

import com.cobber.fta.core.Utils;

public class AddressCommon {
	public static final String POBOX = "P.? ?O.?.?BOX.|POST OFFICE BOX";

	public static final String[] sampleStreets = {
			"Main",  "Lakeside", "Pennsylvania", "Penaton", "Croydon", "Buchanan", "Riverside", "Flushing",
			"Jefferson", "Randolph", "North Lakeside", "Massachusetts", "Central", "Lincoln", "Final Mile",
			"4th", "Flower", "High", "3rd", "12th", "D", "Piedmont", "Chaton", "Kenwood", "Sycamore Lake",
			"Euclid", "Cedarstone", "Carriage", "Major Grey", "Armory", "Bryan", "Charack",
			"Atha", "Bassel", "Overlook", "Chatham", "Melville", "Stone", "Dawson", "Pringle", "Federation",
			"Winifred", "Pratt", "Hillview", "Rosemont", "Romines Mill", "School House", "Candlelight"
	};

	private static HashSet<String> directions = new HashSet<>();
	private static HashSet<String> modifiersWithArgument = new HashSet<>();
	private static HashSet<String> modifiersAny = new HashSet<>();
	private static HashSet<String> textDigit = new HashSet<>();
	private static HashSet<String> ordinal = new HashSet<>();
	private static HashSet<String> ordinalIndicator = new HashSet<>();
	private static HashSet<String> initialMarker = new HashSet<>();

	static {
		directions.add("N");
		directions.add("NE");
		directions.add("NW");
		directions.add("S");
		directions.add("SE");
		directions.add("SW");
		directions.add("E");
		directions.add("W");
		directions.add("NORTH");
		directions.add("NORTHEAST");
		directions.add("NORTHWEST");
		directions.add("SOUTH");
		directions.add("SOUTHEAST");
		directions.add("SOUTHWEST");
		directions.add("EAST");
		directions.add("WEST");
		directions.add("NB");
		directions.add("SB");
		directions.add("EB");
		directions.add("WB");

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
		modifiersAny.add("FL");
		modifiersAny.add("FLOOR");
		modifiersAny.add("LBBY");
		modifiersAny.add("LOBBY");

		textDigit.add("ONE");
		textDigit.add("TWO");
		textDigit.add("THREE");
		textDigit.add("FOUR");
		textDigit.add("FIVE");
		textDigit.add("SIX");
		textDigit.add("SEVEN");
		textDigit.add("EIGHT");
		textDigit.add("NINE");
		textDigit.add("TEN");

		ordinal.add("FIRST");
		ordinal.add("SECOND");
		ordinal.add("THIRD");
		ordinal.add("FOURTH");
		ordinal.add("FIFTH");
		ordinal.add("SIXTH");
		ordinal.add("SEVENTH");
		ordinal.add("EIGHT");
		ordinal.add("NINTH");
		ordinal.add("TENTH");

		ordinalIndicator.add("ST");
		ordinalIndicator.add("ND");
		ordinalIndicator.add("RD");
		ordinalIndicator.add("TH");

		initialMarker.add("AVENUE");
		initialMarker.add("AVE");
		initialMarker.add("CR");
		initialMarker.add("HIGHWAY");
		initialMarker.add("HWY");
		initialMarker.add("INTERSTATE");
		initialMarker.add("I");
		initialMarker.add("ROUTE");
		initialMarker.add("SR");
	}

	public static boolean isDirection(final String input) {
		return directions.contains(input);
	}

	public static boolean isModifier(final String input, final boolean isLastWord) {
		if (isLastWord)
			return modifiersAny.contains(input);
		return modifiersWithArgument.contains(input);
	}

	public static boolean isInitialMarker(final String input) {
		return initialMarker.contains(input);
	}

	/**
	 * Attempt to determine if the input is 'numeric' in the context of an address.
	 * For example, in '110A Eton Drive', 'One Broadway', '45 Penaton St', '312-314 Kings Rd.', '5/2 Hanlon Crescent' are all valid
	 * addresses and should return true.
	 * @param input The input to test.
	 * @return True if the input looks plausible as a 'numeric'.
	 */
	public static boolean isAddressNumber(final String input) {
		// Check for a simple text digit - e.g. 'One', 'Two' etc.
		final char firstCh = input.charAt(0);
		if (Character.isAlphabetic(firstCh) && AddressCommon.isTextDigit(input))
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
			return ordinal.contains(input);
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

		return ordinalIndicator.contains(input.substring(i).trim());
	}

	private static boolean isTextDigit(final String input) {
		return textDigit.contains(input);
	}
}
