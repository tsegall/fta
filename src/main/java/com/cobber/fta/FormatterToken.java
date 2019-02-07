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

import com.cobber.fta.DateTimeParserResult.Token;

class FormatterToken {
	private final Token type;
	private final int count;
	private final int high;
	private final char value;

	public Token getType() {
		return type;
	}

	public int getCount() {
		return count;
	}

	public int getHigh() {
		return high;
	}

	public char getValue() {
		return value;
	}

	FormatterToken(final Token type) {
		this.type = type;
		this.count = 0;
		this.high = 0;
		this.value = '\0';
	}

	FormatterToken(final Token type, final int count) {
		this.type = type;
		this.count = count;
		this.high = 0;
		this.value = '\0';
	}

	FormatterToken(final Token type, final int low, int high) {
		this.type = type;
		this.count = low;
		this.high = high;
		this.value = '\0';
	}

	FormatterToken(final Token type, final char value) {
		this.type = type;
		this.count = 0;
		this.high = 0;
		this.value = value;
	}
}
