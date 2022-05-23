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

import com.cobber.fta.core.LocaleEntry;

/**
 * The Locale entry on the plugin has a localeTag (e.g. en, en-US, or *) and an associated set of Header Entries.
 * These Header entries are used to match against the stream name and indicate a confidence in the value of the match.
 */
public class PluginLocaleEntry {
	/* A comma separated list of locales - either la (language 'la') or la-CT (language 'la' in country 'CT') or '*'. */
	public String localeTag;
	public LocaleEntry[] headerRegExps;
	public PluginMatchEntry[] matchEntries;

	public PluginLocaleEntry() {
	}

	/**
	 * Is the returned Regular Expression a true and complete representation of the Logical Type.
	 * For example, \\d{5} is not for US ZIP codes (e.g. 00000 is not a valid Zip), whereas (?i)(male|female) could be valid for a Gender.
	 * @return The Java Regular Expression that most closely matches this Logical Type.
	 * @param matchEntry The MatchEntry we have concluded is the correct one.
	 */
	public boolean isRegExpComplete(final int matchEntry) {
		// If we have not decided which Match Entry we are after (then default to the first!)
		if (matchEntry == -1) {
			return matchEntries[0].isRegExpComplete();
		}

		return matchEntries[matchEntry].isRegExpComplete();
	}

	public String getRegExpReturned(final int matchEntry) {
		if (matchEntries == null)
			return null;

		// If we have not decided which Match Entry we are after (then default to the first!)
		if (matchEntry == -1) {
			return matchEntries[0].getRegExpReturned();
		}

		return matchEntries[matchEntry].getRegExpReturned();
	}

	public int getMatchEntry(final String regExp, final int matchEntry) {
		if (matchEntry == -1) {
			for (int i = 0; i < matchEntries.length; i++) {
				for (final String re : matchEntries[i].getRegExpsToMatch()) {
					if (regExp.equals(re))
						return i;
				}
			}

			return -1;
		}

		for (final String re : matchEntries[matchEntry].getRegExpsToMatch()) {
			if (regExp.equals(re))
				return matchEntry;
		}

		return -1;
	}

	/**
	 * Simple Constructor for use when have a single Language tag and a single header (regular expression, confidence pair).
	 * @param localeTag The localeTag we are using to construct the Locale Entry.
	 * @param headerRegExp The Regular Expression used to match against the stream name.
	 * @param confidence The confidence in our assessment that this is the Semantic Type if the regular expression matches.
	 * @param regExpReturned The Regular Expressed returned in the case of a successful match.
	 */
	public PluginLocaleEntry(final String localeTag, final String headerRegExp, final int confidence, final String regExpReturned) {
		this.localeTag = localeTag;
		if (headerRegExp != null)
			this.headerRegExps = new LocaleEntry[] { new LocaleEntry(headerRegExp, confidence) };
		matchEntries = new PluginMatchEntry[] { new PluginMatchEntry(regExpReturned) };
	}

	/**
	 * Simple Constructor for use when have a single Language tag and no header information.
	 * @param localeTag The localeTag we are using to construct the Locale Entry.
	 */
	public PluginLocaleEntry(final String localeTag) {
		this.localeTag = localeTag;
	}

	/**
	 * @param localeTags Construct an array of simple Locale Entries based on a set of locale tags.
	 * @return An array of Locale Entries with only the tag set - no header information.
	 */
	public static PluginLocaleEntry[] simple(final String[] localeTags) {
		PluginLocaleEntry[] ret = new PluginLocaleEntry[localeTags.length];

		for (int i = 0; i < ret.length; i++) {
			ret[i] = new PluginLocaleEntry(localeTags[i]);
		}

		return ret;
	}

	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder();
		ret.append(localeTag);

		if (headerRegExps != null) {
			ret.append(':');
			for (final LocaleEntry entry : headerRegExps)
				ret.append(entry);
		}

		return ret.toString();
	}
}
