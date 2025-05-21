/*
 * Copyright 2017-2025 Tim Segall
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

import com.cobber.fta.core.HeaderEntry;

/**
 * Each HeaderLocaleEntry provides a pair with the locale and the value of the tag in that locale.
 * See keywords.json for the structure of the JSON file.
 */
public class HeaderLocaleEntry {
	public String localeTag;
	public HeaderEntry[] headerRegExps;

	/**
	 * Determine the confidence that the input is valid
	 * @param input The input to be validated
	 * @return An integer between -100 and 100 reflecting the confidence that the input is valid.  O indicates no data.
	 */
	public int getHeaderConfidence(final String input) {
		if (headerRegExps != null)
			for (final HeaderEntry headerEntry : headerRegExps) {
				if (headerEntry.matches(input))
					return headerEntry.confidence;
			}

		return 0;
	}
}
