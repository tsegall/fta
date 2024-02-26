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
package com.cobber.fta;

/**
 * All Semantic Types that consist of a unconstrained domain, for example, an infinite (or large) number of elements should
 * subclass this abstract class.
 */
public abstract class LogicalTypeInfinite extends LogicalTypeCode {
	public LogicalTypeInfinite(final PluginDefinition plugin) {
		super(plugin);
	}

	/**
	 * A fast check to see if the supplied String might be an instance of this Semantic type?
	 *
	 * @param trimmed String to check
	 * @param compressed A compressed representation of the input string (e.g. \d{5} for 20351).
	 * @param charCounts An array of occurrence counts for characters in the input (ASCII-only).
	 * @param lastIndex An array of the last index where character is located (ASCII-only).
	 * @return true iff the supplied String is a possible instance of this Semantic type.
	 */
	public abstract boolean isCandidate(String trimmed, StringBuilder compressed, int[] charCounts, int[] lastIndex);

	@Override
	public boolean isRegExpComplete() {
		return false;
	}

	@Override
	public boolean isClosed() {
		return false;
	}
}
