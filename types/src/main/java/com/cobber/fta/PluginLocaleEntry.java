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

/**
 * The Locale entry on the plugin has a localeTag (e.g. en, en-US, or *) and an associated set of Header Entries.
 * These Header entries are used to match against the stream name and indicate a confidence in the value of the match.
 */
public class PluginLocaleEntry {
	public String localeTag;
	public PluginLocaleHeaderEntry[] headerRegExps;

	public PluginLocaleEntry() {
	}

	/**
	 * Simple Constructor for use when have a single Language tag and a single header (regular expression, confidence pair).
	 * @param localeTag The localeTag we are using to construct the Locale Entry.
	 * @param regExp The Regular Expression used to match against the stream name.
	 * @param confidence The confidence in our assessment that this is the Semantic Type if the regular expression matches.
	 */
	public PluginLocaleEntry(String localeTag, final String regExp, final int confidence) {
		this.localeTag = localeTag;
		this.headerRegExps = new PluginLocaleHeaderEntry[] { new PluginLocaleHeaderEntry(regExp, confidence) };
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
	public static PluginLocaleEntry[] simple(String[] localeTags) {
		PluginLocaleEntry[] ret = new PluginLocaleEntry[localeTags.length];

		for (int i = 0; i < ret.length; i++) {
			ret[i] = new PluginLocaleEntry(localeTags[i]);
		}

		return ret;
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append(localeTag);

		if (headerRegExps != null) {
			ret.append(':');
			for (PluginLocaleHeaderEntry entry : headerRegExps)
				ret.append(entry);
		}

		return ret.toString();
	}
}
