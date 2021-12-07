/*
 * Copyright 2017-2021 Tim Segall
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

public class MinMax implements Comparable<MinMax> {
	private int min;
	private int max;

	public MinMax() {
		min = max = -1;
	}

	public MinMax(final MinMax other) {
		this.min = other.min;
		this.max = other.max;
	}

	public MinMax(final String text) {
		final int brace = text.indexOf('{');
		if (brace == -1)
			this.min = this.max = text.length();
		else {
			this.min = text.charAt(brace + 1) - '0';
			this.max = text.charAt(brace + 3) - '0';
		}
	}

	public void set(final int both) {
		min = max = both;
	}

	public void set(final int min, final int max) {
		this.min = min;
		this.max = max;
	}

	public int getMin() {
		return min;
	}

	public void setMin(final int min) {
		this.min = min;
	}

	public int getMax() {
		return max;
	}

	public boolean isSet() {
		return min != -1;
	}

	public void merge(final MinMax other) {
		if (other.min < this.min)
			this.min = other.min;
		if (other.max > this.max)
			this.max = other.max;
	}

	public int getPatternLength() {
		// if min == max then we just repeat the field
		if (min == max)
			return min;
		// if min != max then we have <Field>{min,max} - e.g. S{1,3}
		return 6;
	}

	public String getPattern(final char field) {
		// if min == max then we just repeat the field
		if (min == max)
			return Utils.repeat(field, min);
		// if min != max then we have <Field>{min,max} - e.g. S{1,3}
		return "" + field + '{' + min + ',' + max + '}';
	}

	@Override
	public int compareTo(final MinMax other) {
		if (this.min == other.min && this.max == other.max)
			return 0;
		return this.min + this.max > other.min + other.max ? 1 : -1;
	}
}
