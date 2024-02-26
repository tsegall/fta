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

import java.util.List;
import java.util.Set;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.Utils;
import com.cobber.fta.core.WordProcessor;

/**
 * All Semantic Types that consist of a constrained domain, for example, a finite (small) number of elements should
 * subclass this abstract class.
 */
public abstract class LogicalTypeFinite extends LogicalTypeCode {
	protected int minLength = Integer.MAX_VALUE;
	protected int maxLength = Integer.MIN_VALUE;

	private final WordProcessor wordProcessor = new WordProcessor();

	/**
	 * The set of valid members for this Semantic Type.
	 * @return The valid members for this Semantic Type.
	 */
	public abstract Set<String> getMembers();

	public LogicalTypeFinite(final PluginDefinition plugin) {
		super(plugin);
	}

	/*
	 * Note: The input String will be both trimmed and converted to upper Case
	 * @see com.cobber.fta.LogicalType#isValid(java.lang.String)
	 */
	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		final String trimmedUpper = Utils.cleanse(input.trim()).toUpperCase(locale);

		if (trimmedUpper.length() < minLength)
			return false;

		if (trimmedUpper.length() <= maxLength && getMembers().contains(trimmedUpper))
			return true;

		final String value = defn.getOptions().get("words");
		if (value != null) {
			final boolean any = "any".equalsIgnoreCase(value);
			final boolean all = "all".equalsIgnoreCase(value);
			final boolean first = "first".equalsIgnoreCase(value);

			final List<String> words = wordProcessor.asWords(trimmedUpper);
			boolean found = false;
			for (final String word : words) {
				found = getMembers().contains(Utils.cleanse(word).toUpperCase(locale));
				if (first || (any && found) || (all && !found))
					return found;
			}
			return all && found;
		}

		return false;
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		for (final String member : getMembers()) {
			final int len = member.length();
			if (len < minLength)
				minLength = len;
			if (len > maxLength)
				maxLength = len;
		}

		return true;
	}

	@Override
	public boolean isRegExpComplete() {
		return false;
	}

	@Override
	public boolean isClosed() {
		return true;
	}

	/**
	 * Get the number of members in this Semantic Type.
	 * @return The number of members
	 */
	public int getSize() {
		return getMembers().size();
	}

	public Set<String> getIgnorable() {
		return defn.ignoreList;
	}

	/**
	 * Get the minimum length of instances of this Semantic Type.
	 * @return The minimum length of instances
	 */
	public int getMinLength() {
		return minLength;
	}

	/**
	 * Get the maximum length of instances of this Semantic Type.
	 * @return The maximum length of instances
	 */
	public int getMaxLength() {
		return maxLength;
	}
}
