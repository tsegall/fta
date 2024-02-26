/*
 * Copyright 2017-2024 Tim Segall
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
package com.cobber.fta.dates;

import com.cobber.fta.core.Utils;
import com.cobber.fta.dates.DateTimeParserResult.Token;

class FormatterToken {
	private final Token type;
	private final char value;
	private int count;
	private int high;
	private int fieldWidth;
	private int offset;

	FormatterToken(final Token type) {
		this.type = type;
		this.count = 0;
		this.high = 0;
		this.value = '\0';
		this.fieldWidth = -1;
	}

	FormatterToken(final Token type, final int count) {
		this.type = type;
		this.count = count;
		this.high = 0;
		this.value = '\0';
		this.fieldWidth = -1;
	}

	FormatterToken(final Token type, final int low, final int high) {
		this.type = type;
		this.count = low;
		this.high = high;
		this.value = '\0';
		this.fieldWidth = -1;
	}

	FormatterToken(final Token type, final char value) {
		this.type = type;
		this.count = 0;
		this.high = 0;
		this.value = value;
		this.fieldWidth = -1;
	}

	FormatterToken(final FormatterToken other) {
		this.type = other.type;
		this.count = other.count;
		this.high = other.high;
		this.value = other.value;
		this.fieldWidth = other.fieldWidth;
		this.offset = other.offset;
	}

	public FormatterToken withFieldWidth(final int fieldWidth) {
		this.fieldWidth = fieldWidth;
		return this;
	}

	public FormatterToken withOffset(final int offset) {
		this.offset = offset;
		return this;
	}

	public FormatterToken withCount(final int count) {
		this.count = count;
		return this;
	}

	public enum DateField {

		Hour,
		Minute,
		Second,
		Fraction,
		Day,
		Month,
		Year,
		Unbound1,
		Unbound2,
		Unbound3,
		;

		private static final DateField values[] = values();

		public static DateField get(final int ordinal) { return values[ordinal]; }
	}

	public void merge(final FormatterToken other) {
		this.count = Math.min(this.count, other.count);
		this.high = Math.max(this.high, other.high);
		this.fieldWidth = Math.max(this.fieldWidth, other.fieldWidth);
	}

	public Token getType() {
		return type;
	}

	public int getCount() {
		return count;
	}

	public void setCount(final int count) {
		this.count = count;
	}

	public int getHigh() {
		return high;
	}

	public int getOffset() {
		return offset;
	}

	public char getValue() {
		return value;
	}

	public int getFieldWidth() {
		return fieldWidth;
	}

	public void setFieldWidth(final int fieldWidth) {
		this.fieldWidth = fieldWidth;
	}

	public String getRepresentation() {
		final StringBuilder ret = new StringBuilder();
		if (getFieldWidth() != -1)
			ret.append(Utils.repeat('p', getFieldWidth()));
		if (getCount() != 0)
			ret.append(Utils.repeat(type.getRepresentation().charAt(0), getCount()));
		else
			ret.append(type.getRepresentation());

		return ret.toString();
	}
}
