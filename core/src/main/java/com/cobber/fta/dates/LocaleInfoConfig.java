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
package com.cobber.fta.dates;

import java.util.Locale;
import java.util.Objects;

public class LocaleInfoConfig {
	private final Locale locale;
	private final boolean noAbbreviationPunctuation;
	// Allow "AM" and "PM" since these are commonly seen even in locales where they are not the AM/PM strings
	private final boolean allowEnglishAMPM;

	public LocaleInfoConfig(final Locale locale, final boolean noAbbreviationPunctuation, final boolean allowEnglishAMPM) {
		this.locale = locale;
		this.noAbbreviationPunctuation = noAbbreviationPunctuation;
		this.allowEnglishAMPM = allowEnglishAMPM;
	}

	public Locale getLocale() {
		return locale;
	}

	public boolean isNoAbbreviationPunctuation() {
		return noAbbreviationPunctuation;
	}

	public boolean isEnglishAMPMAllowed() {
		return allowEnglishAMPM;
	}

	public String getCacheKey() {
		return String.valueOf(Objects.hash(locale, noAbbreviationPunctuation, allowEnglishAMPM));
	}
}
