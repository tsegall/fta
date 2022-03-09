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
package com.cobber.fta;

import java.util.Locale;
import java.util.Set;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;

/**
 * All Logical Types that consist of a constrained domain, for example, a finite (small) number of elements should
 * subclass this abstract class.
 */
public abstract class LogicalTypeFinite extends LogicalTypeCode {
	protected int minLength = Integer.MAX_VALUE;
	protected int maxLength = Integer.MIN_VALUE;

	public abstract Set<String> getMembers();

	public LogicalTypeFinite(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	/*
	 * Note: The input String will be both trimmed and converted to upper Case
	 * @see com.cobber.fta.LogicalType#isValid(java.lang.String)
	 */
	@Override
	public boolean isValid(final String input) {
		final String trimmedUpper = input.trim().toUpperCase(locale);
		return trimmedUpper.length() >= minLength && trimmedUpper.length() <= maxLength && getMembers().contains(trimmedUpper);
	}

	@Override
	public boolean initialize(final Locale locale) throws FTAPluginException {
		super.initialize(locale);

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
	 * Get the number of members in this Logical Type.
	 * @return The number of members
	 */
	public int getSize() {
		return getMembers().size();
	}

	/**
	 * Get the minimum length of instances of this Logical Type.
	 * @return The minimum length of instances
	 */
	public int getMinLength() {
		return minLength;
	}

	/**
	 * Get the maximum length of instances of this Logical Type.
	 * @return The maximum length of instances
	 */
	public int getMaxLength() {
		return maxLength;
	}
}
