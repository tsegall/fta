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
