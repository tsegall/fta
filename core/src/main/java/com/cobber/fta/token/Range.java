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
package com.cobber.fta.token;

import java.util.Objects;

public class Range implements Comparable<Range> {
	private char min;
	private char max;

	public Range(final char ch) {
		this.min = ch;
	}

	public Range(final char min, final char max) {
		this.min = min;
		this.max = max;
	}

	public char getMin() {
		return min;
	}

	public char getMax() {
		return max;
	}

	public char setMax(final char max) {
		return this.max = max;
	}

	@Override
	public String toString() {
		String ret = String.valueOf(min);
		if (min != max)
			ret += "-" + this.max;
		return ret;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Objects.hash(max, min);
		return result;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == this)
			return true;

		if (!(o instanceof Range))
			return false;

		final Range other = (Range)o;

		return other != null && this.min == other.min && this.max == other.max;
	}

	@Override
	public int compareTo(final Range other) {
		return min - other.min;
	}
}

