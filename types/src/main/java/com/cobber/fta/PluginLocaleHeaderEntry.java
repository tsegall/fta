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

import java.util.regex.Pattern;

/**
 * The Header entry has a Regular Expression and a Confidence that reflects the confidence that if the regular expression
 * matches the data stream name we are likely to have a field of the semantic type.  A confidence of 100 indicates that
 * the stream name MUST match the regular expression for this Semantic Type to be accepted.
 */
public class PluginLocaleHeaderEntry {
	/** The Regular Expression used to match against the Stream Name. */
	public String regExp;
	/** The confidence that the match is a good predictor of this being the Semantic Type. */
	public int confidence;
	/** The pattern is used to cache the compiled regular expression since it will be executed many times. */
	private Pattern pattern;

	public PluginLocaleHeaderEntry() {
	}

	/**
	 * Constructor for a Header entry.
	 * @param regExp The Regular Expression used to match against the stream name.
	 * @param confidence The confidence in our assessment that this is the Semantic Type if the regular expression matches.
	 */
	public PluginLocaleHeaderEntry(final String regExp, final int confidence) {
		this.regExp = regExp;
		this.confidence = confidence;
		pattern = Pattern.compile(regExp);
	}

	@Override
	public String toString() {
		return (new StringBuilder()).append('[').append(regExp).append(':').append(confidence).append(']').toString();
	}

	public boolean matches(final String streamName) {
		if (pattern == null) {
			synchronized (this) {
				pattern = Pattern.compile(regExp);
			}
		}

		return pattern.matcher(streamName).matches();
	}
}
