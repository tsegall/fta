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
package com.cobber.fta.plugins;

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
	private static HashSet<String> modifiers = new HashSet<>();
	private static HashSet<String> textDigit = new HashSet<>();
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

		modifiers.add("APARTMENT");
		modifiers.add("APT");
		modifiers.add("BLDG");
		modifiers.add("BOX");
		modifiers.add("FL");
		modifiers.add("FLOOR");
		modifiers.add("LBBY");
		modifiers.add("LOBBY");
		modifiers.add("OFFICE");
		modifiers.add("ROOM");
		modifiers.add("STE");
		modifiers.add("SUITE");
		modifiers.add("UNIT");

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

		initialMarker.add("AVENUE");
		initialMarker.add("AVE");
		initialMarker.add("INTERSTATE");
		initialMarker.add("ROUTE");
		initialMarker.add("HWY");
		initialMarker.add("SR");
	}

	public static boolean isDirection(final String input) {
		return directions.contains(input);
	}

	public static boolean isModifier(final String input) {
		return modifiers.contains(input);
	}

	public static boolean isInitialMarker(final String input) {
		return initialMarker.contains(input);
	}

	/**
	 * This routine attempts to determine if the input is 'numeric' in the context of an address.
	 * For example, in '110A Eton Drive', 'One Broadway', '45 Penaton St', '312-314 Kings Rd.', '5/2 Hanlon Crescent' are all valid
	 * addresses and should return true.
	 * @param input The input to test.
	 * @return True if the input looks plausible as the start of an address.
	 */
	public static boolean isAddressNumber(final String input) {
		final int len = input.length();
		final char last = input.charAt(len - 1);
		String toTest = input;

		if (len > 1 && Character.isAlphabetic(last)) {
			String rest = input.substring(0, len - 1);
			if (!Utils.isNumeric(rest))
				return false;
			toTest = rest;
		}

		// Check for a simple text digit - e.g. 'One', 'Two' etc.
		char firstCh = toTest.charAt(0);
		if (Character.isAlphabetic(firstCh) && AddressCommon.isTextDigit(toTest))
			return true;

		if (!Character.isDigit(firstCh))
			return false;

		boolean firstNumber = false;
		boolean secondNumber = false;
		boolean separator = false;
		for (int i = 0; i < toTest.length(); i++) {
			char ch = toTest.charAt(i);
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

	private static boolean isTextDigit(final String input) {
		return textDigit.contains(input);
	}
}
