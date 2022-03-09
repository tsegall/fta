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
package com.cobber.fta.plugins.identity;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.commons.validator.routines.checkdigit.ModulusCheckDigit;

/**
 * NHS Check Digit calculation/validation.
 * This implementation validates/calculates NHS check digits.
 */
public final class NHSCheckDigit extends ModulusCheckDigit  {
	private static final long serialVersionUID = 7892962486766619960L;

	/**
	 * Construct an NHS Identifier Check Digit routine.
	 */
	public NHSCheckDigit() {
		super(11);
	}

	/**
	 * Calculate the modulus for an NHS code.
	 *
	 * @param code The code to calculate the modulus for.
	 * @param includesCheckDigit Whether the code includes the Check Digit or not.
	 * @return The modulus value
	 * @throws CheckDigitException if an error occurs calculating the modulus
	 * for the specified code
	 */
	@Override
	protected int calculateModulus(final String code, final boolean includesCheckDigit) throws CheckDigitException {
		if (includesCheckDigit) {
			final char checkDigit = code.charAt(code.length()-1); // fetch the last character
			if (!Character.isDigit(checkDigit)){
				throw new CheckDigitException("Invalid checkdigit["+ checkDigit+ "] in " + code);
			}
		}
		for (int i = 0; i < code.length(); i++) {
			final char ch = code.charAt(i);
			if (ch < '0' || ch > '9')
				throw new CheckDigitException("Invalid Character[" +
						(i + 1) + "] = '" + ch + "'");
		}
		return super.calculateModulus(code, includesCheckDigit);
	}

	/**
	 * <p>Calculates the <i>weighted</i> value of a character in the
	 * code at a specified position.</p>
	 *
	 * <p>For NHS digits are weighted starting with 10 at the left-most digit
	 * and descending by one for each position.
	 *
	 * @param charValue The numeric value of the character.
	 * @param leftPos  The position of the character in the code, counting from left to right
	 * @param rightPos The position of the character in the code, counting from right to left
	 * @return The weighted value of the character.
	 */
	@Override
	protected int weightedValue(final int charValue, final int leftPos, final int rightPos) {
		final int weight = 11 - leftPos;
		return charValue * weight;
	}
}
