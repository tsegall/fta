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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.cobber.fta.core.InternalErrorException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides a wrapper for the keywords.json lookup file, this enables us to look up localized versions of
 * key tags that we are using for matching.  For example, we support detecting localized versions of booleans with Yes/No in
 * the current locale.
 */
public class Keywords {
	private static List<Keywords> keywords;

	/** Keyword Tag. */
	public String keytag;
	/** keyType is either REGEX or WORD. */
	public String keyType;
	/** Used if keyType == WORD. */
	public WordLocaleEntry[] wordLocales;
	/** Used if keyType == REGEX. */
	public HeaderLocaleEntry[] validLocales;

	private Locale locale;
	private String languageTag;
	private String language;

	private static Map<String, Keywords> cache = new HashMap<>();

	public static Keywords getInstance(final Locale locale) {
		synchronized(cache) {
			Keywords ret = cache.get(locale.toLanguageTag());
			if (ret == null) {
				ret = new Keywords().initialize(locale);
				cache.put(locale.toLanguageTag(), ret);
			}
			return ret;
		}

	}

	/**
	 * Initialize an instance of the Keyword storage with the locale.
	 * @param locale The Locale we are currently using
	 */
	private Keywords initialize(final Locale locale) {
		try (BufferedReader JSON = new BufferedReader(new InputStreamReader(Keywords.class.getResourceAsStream("/reference/keywords.json"), StandardCharsets.UTF_8))) {
			keywords = new ObjectMapper().readValue(JSON, new TypeReference<List<Keywords>>(){});
		} catch (Exception e) {
			throw new InternalErrorException("Issues with reference keywords file", e);
		}

		this.locale = locale;
		this.languageTag = locale.toLanguageTag();
		this.language = locale.getLanguage();

		return this;
	}

	/**
	 * Retrieve the localized version of the keyTag supplied.
	 * @param keyTag The Keytag we are looking for a localized version of - e.g. YES in fr-FR will return 'oui'.
	 * @return The localized version of the supplied tag (null if no version exists).
	 */
	public String get(final String keyTag) {
		for (final Keywords keyword : keywords) {
			if (!"WORD".equals(keyword.keyType))
				continue;
			if (keyword.keytag.equals(keyTag))
				for (final WordLocaleEntry entry : keyword.wordLocales)
					if (isMatch(entry.localeTag))
						return entry.value;
		}

		return null;
	}

	/**
	 * Does the supplied input 'match' the Keyword we are looking for in the locale provided.
	 *
	 * @param input The input string to match against.
	 * @param keyTag The well-defined Tag - that we are going to search for a locale specific match for
	 * @return An integer (1-100) indicating how well the input 'matches' the supplied tag.
	 */
	public int match(final String input, final String keyTag) {
		if (input == null)
			return 0;

		final String lower = input.trim().toLowerCase(locale);
		if (lower.isEmpty())
			return 0;

		// Find our Keyword
		for (final Keywords keyword : keywords) {
			if (!"REGEX".equals(keyword.keyType))
				continue;
			if (keyword.keytag.equals(keyTag))
				// Find our locale
				for (final HeaderLocaleEntry entry : keyword.validLocales)
					if (isMatch(entry.localeTag))
						return entry.getHeaderConfidence(input);
		}

		return 0;
	}

	private boolean isMatch(final String validLocale) {
		if ("*".equals(validLocale))
			return true;

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
