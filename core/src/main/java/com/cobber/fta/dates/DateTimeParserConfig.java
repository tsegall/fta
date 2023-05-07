/*
 * Copyright 2017-2023 Tim Segall
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
	/** The list of Locales to test. */
	public Locale[] locales;
	/** The locale determined - will be one from the set above. */
	private Locale locale;
	/** If Strict mode is set, any input to train() that would not pass the current 'best' guess will return null (Default: false). */
	public boolean strictMode;
	/** If Numeric mode is set, any numeric-only input to train() will be tested to see if it appears to be a date (Default: true). */
	public boolean numericMode = true;
	/** If noAbbreviationPunctuation is set we should use Month Abbreviations without periods, for example in the
	 * Canadian locale, Java returns 'AUG.', and similarly for the AM/PM string which are defined as A.M and P.M. (Default: true). */
	public boolean noAbbreviationPunctuation = true;
	/** lenient allows dates of the form '00/00/00' etc to be viewed as valid for the purpose of Format detection (Default: true). */
	public boolean lenient = true;

	public DateTimeParserConfig(final Locale... locales) {
		setLocales(locales);
	}

	public DateTimeParserConfig() {
		resolutionMode = DateResolutionMode.None;
	}

	public void setLocales(final Locale... locales) {
		this.locales = locales;
		if (locales.length == 1)
			this.locale = this.locales[0];
	}

	protected void setLocale(final Locale locale) {
		this.locale = locale;
	}

	/**
	 * Get the current active Locale.  If the set of locales has not been initialized then this will
	 * return the current default Java Locale.  If the set of locales has only one member then this will be returned.
	 * In the case where there are multiple locales - then this will return null, until setLocale() is invoked.
	 * @return The current active Locale.
	 */
	public Locale getLocale() {
		if (locale != null)
			return locale;

		if (locales == null)
			locales = new Locale[] { Locale.getDefault() };

		return locales.length == 1 ? locales[0] : null;
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
