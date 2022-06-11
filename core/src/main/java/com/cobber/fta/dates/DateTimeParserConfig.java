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
package com.cobber.fta.dates;

import java.util.Locale;
import java.util.Objects;

import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;

/**
 * Capture the Configuration of the DateTimeParser - this and the State are all we need to serialize()/deserialize()/merge the DateTimeParser.
 */
public class DateTimeParserConfig {
	/** When we have ambiguity - should we prefer to conclude day first, month first or unspecified. */
	public DateResolutionMode resolutionMode;
	/** The Locale the input is in. */
	public Locale locale;
	/** If Strict mode is set, any input to train() that would not pass the current 'best' guess will return null. */
	public boolean strictMode;
	/** If noAbbreviationPunctuation is set we should use Month Abbreviations without periods, for example for the
	 * Canadian locale, Java returns 'AUG.', and similarly for the AM/PM string which are defined in as A.M and P.M. */
	public boolean noAbbreviationPunctuation = true;
	/** lenient allows dates of the form 00/00/00 etc to be viewed as valid for the purpose of Format detection. */
	public boolean lenient = true;

	public DateTimeParserConfig(final Locale locale) {
		this.locale = locale;
	}

	public DateTimeParserConfig() {
		this.locale = Locale.getDefault();
		resolutionMode = DateResolutionMode.None;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final DateTimeParserConfig other = (DateTimeParserConfig) obj;
		return Objects.equals(locale, other.locale) && resolutionMode == other.resolutionMode
				&& strictMode == other.strictMode;
	}
}

