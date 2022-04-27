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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import com.cobber.fta.core.InternalErrorException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides a wrapper for the keywords.json lookup file, this enables us to look up localized versions of
 * key tags that we are using for matching.  For example, we support detecting localized versions of booleans with Yes/No in
 * the current locale.
 */
public class Keywords {
	enum MatchStyle {
		CONTAINS,
		EQUALS
	}

	private static List<Keywords> keywords;

	/** Keyword Tag. */
	public String keytag;
	public KeywordLocaleEntry[] validLocales;

	private Locale locale;

	public Keywords() {
	}

	/**
	 * Initialize an instance of the Keyword storage with the locale.
	 * @param locale The Locale we are currently using
	 */
	public void initialize(final Locale locale) {
		try (BufferedReader JSON = new BufferedReader(new InputStreamReader(Keywords.class.getResourceAsStream("/reference/keywords.json"), StandardCharsets.UTF_8))) {
			keywords = new ObjectMapper().readValue(JSON, new TypeReference<List<Keywords>>(){});
		} catch (Exception e) {
			throw new InternalErrorException("Issues with reference keywords file", e);
		}

		this.locale = locale;
	}

	/**
	 * Retrieve a localized version of the keyTag supplied.
	 * @param keyTag The Keytag we are looking for a localized version of - e.g. YES in fr-FR will return 'oui'.
	 * @return The localized version of the supplied tag.
	 */
	public String get(final String keyTag) {
		for (final Keywords keyword : keywords) {
			if (keyword.keytag.equals(keyTag))
				for (final KeywordLocaleEntry entry : keyword.validLocales)
					if (isMatch(entry.localeTag))
						return entry.value;
		}

		return null;
	}

	/**
	 * Does the supplied input match the Keyword we are looking for in the locale provided.
	 *
	 * @param input The input string to match against.
	 * @param keyTag The well-defined Tag - that we are going to search for a locale specific match for
	 * @param matchStyle Either CONTAINS or EQUALS
	 * @return A boolean indicating if the input 'matches' the supplied tag.
	 */
	public boolean match(final String input, final String keyTag, MatchStyle matchStyle) {
		String lower = input.trim().toLowerCase(locale);
		for (final Keywords keyword : keywords)
			if (keyword.keytag.equals(keyTag))
				for (final KeywordLocaleEntry entry : keyword.validLocales)
					if (isMatch(entry.localeTag))
							return lower.contains(entry.value);

		return false;
	}

	private boolean isMatch(final String validLocale) {
		final String languageTag = locale.toLanguageTag();
		final String language = locale.getLanguage();

		// Check to see if this keyword is valid for this locale
		if (validLocale.indexOf('-') != -1) {
			if (validLocale.equals(languageTag))
				return true;
		}
		else if (validLocale.equals(language))
				return true;

		return false;
	}
}
