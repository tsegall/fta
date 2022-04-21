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
package com.cobber.fta.plugins;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.LogicalTypeFinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect Gender.
 */
public class Gender extends LogicalTypeFinite {
	/** The Semantic type for this Plugin. */
	public static final String SEMANTIC_TYPE = "GENDER.TEXT_";

	private static final String BACKOUT_REGEX = "\\p{IsAlphabetic}+";

	// Map from ISO language to Gender Data for the language
	private static Map<String, GenderData> allGenderData = new HashMap<>();

	private static Map<String, Set<String>> allMembers = new HashMap<>();
	private static Map<String, Map<String, String>> allOpposites = new HashMap<>();

	private Map<String, String> opposites = null;

	private String happyRegex = BACKOUT_REGEX;
	private String language = null;
	private GenderData genderData = null;

	private static class GenderData {
		private final String header;
		private final String feminine;
		private final String masculine;
		private final String feminineShort;
		private final String masculineShort;
		private final String woman;
		private final String man;
		private final String womanShort;
		private final String manShort;

		GenderData(final String header, final String feminine, final String masculine, final String feminineShort, final String masculineShort,
				final String woman, final String man, final String womanShort, final String manShort) {
			this.header = header;
			this.feminine = feminine;
			this.masculine = masculine;
			this.feminineShort = feminineShort;
			this.masculineShort = masculineShort;
			this.woman = woman;
			this.womanShort = womanShort;
			this.man = man;
			this.manShort = manShort;
		}
	}

	static {
		// German
		allGenderData.put("DE", new GenderData(".*(?i)(Gender|Geschlecht)", "WEIBLICH", "MÄNNLICH", "W", "M",
				"FRAU", "MANN", "F", "M"));
		// English
		allGenderData.put("EN", new GenderData(".*(?i)(Gender|sex)", "FEMALE", "MALE", "F", "M",
				null, null, null, null));
		// Spanish
		allGenderData.put("ES", new GenderData(".*(?i)(Gender|Sexo)", "FEMENINO", "MASCULINO", "F", "M",
				"MUJERES", "HOMBRES", "M", "H"));
		// French
		allGenderData.put("FR", new GenderData(".*(?i)(Gender|Genre|Sexe)", "FEMME", "HOMME", "F", "H",
				null, null, null, null));
		// Italian
		allGenderData.put("IT", new GenderData(".*(?i)(Gender|genere|sesso)", "FEMMINA", "MASCHIO", "F", "M",
				null, null, null, null));
		// Malaysian
		allGenderData.put("MS", new GenderData(".*(?i)(Gender|jantina)", "PEREMPUAN", "LELAKI", "P", "L",
				null, null, null, null));
		// Dutch
		allGenderData.put("NL", new GenderData(".*(?i)(Gender|Geslach|Geslacht)", "VROUWELIJK", "MANNELIJK", "V", "M",
				null, null, null, null));
		// Portuguese
		allGenderData.put("PT", new GenderData(".*(?i)(Gender|Gênero|Sexo)", "FEMININA", "MASCULINO", "F", "M",
				"DONA", "HOME", "D", "H"));
		// Turkish
		allGenderData.put("TR", new GenderData(".*(?i)(Gender)", "KADIN", "ERKEK", "K", "E",
				null, null, null, null));

		// Belgium covered by DE, FR, and NL
		// Switzerland covered DE, FR, IT
		// Austria covered by DE
	}

	/**
	 * Construct a Gender plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public Gender(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		return random.nextInt(2) != 0 ? genderData.feminine : genderData.masculine;
	}

	@Override
	public boolean initialize(final Locale locale) throws FTAPluginException {
		super.initialize(locale);

		language = locale.getLanguage().toUpperCase(Locale.ROOT);
		genderData = allGenderData.get(language);

		opposites = allOpposites.get(language);
		if (opposites == null) {
			opposites = new HashMap<>();
			opposites.put(genderData.feminineShort, genderData.masculineShort);
			opposites.put(genderData.masculineShort, genderData.feminineShort);
			opposites.put(genderData.feminine, genderData.masculine);
			opposites.put(genderData.masculine, genderData.feminine);

			if (genderData.woman != null) {
				opposites.put(genderData.womanShort, genderData.manShort);
				opposites.put(genderData.manShort, genderData.manShort);
				opposites.put(genderData.woman, genderData.man);
				opposites.put(genderData.man, genderData.woman);
			}

			allOpposites.put(language, opposites);
		}

		return true;
	}

	@Override
	public Set<String> getMembers() {
		final String setupLanguage = locale.getLanguage().toUpperCase(Locale.ROOT);
		Set<String> languageMembers = allMembers.get(setupLanguage);

		if (languageMembers == null) {
			final GenderData setup = allGenderData.get(setupLanguage);

			languageMembers = new HashSet<>();
			languageMembers.add(setup.feminineShort);
			languageMembers.add(setup.masculineShort);
			languageMembers.add(setup.feminine);
			languageMembers.add(setup.masculine);

			if (setup.woman != null) {
				languageMembers.add(setup.womanShort);
				languageMembers.add(setup.manShort);
				languageMembers.add(setup.woman);
				languageMembers.add(setup.man);
			}

			allMembers.put(setupLanguage, languageMembers);
		}

		return languageMembers;
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE + language;
	}

	@Override
	public String getRegExp() {
		return 	happyRegex;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, Map<String, Long> cardinality, final Map<String, Long> outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		final boolean positiveStreamName = context.getStreamName().matches(genderData.header);

		if (cardinality.isEmpty())
			return new PluginAnalysis(BACKOUT_REGEX);

		// If we have a happy header and a good percentage of FEMALE/MALE or WOMAN/MAN (localized) - then assume all is good
		Long feminine = cardinality.get(genderData.feminine);
		Long masculine = cardinality.get(genderData.masculine);
		Long woman = cardinality.get(genderData.woman);
		Long man = cardinality.get(genderData.man);
		if (positiveStreamName && (
				(feminine != null && masculine != null && feminine + masculine > matchCount/2) ||
				(woman != null && man != null && woman + man > matchCount/2)
			))
		{
			final RegExpGenerator re = new RegExpGenerator(8, locale);
			cardinality.putAll(outliers);
			outliers.clear();
			for (String item : cardinality.keySet())
				re.train(item);
			happyRegex = re.getResult();
			return PluginAnalysis.OK;
		}

		// We made a good faith effort above to be inclusive - so at this stage call it a day if there are too many outliers
		if (outliers.size() > 1 || (!positiveStreamName && cardinality.size() - outliers.size() <= 1))
			return new PluginAnalysis(BACKOUT_REGEX);

		String outlier;
		if (!outliers.isEmpty()) {
			outlier = outliers.keySet().iterator().next();
			cardinality = new HashMap<>(cardinality);
			cardinality.remove(outlier);
		}

		final int count = cardinality.size();
		Iterator<String> iter = null;
		String first = null;
		if (count != 0) {
			iter = cardinality.keySet().iterator();
			first = iter.next();
		}

		// If we have seen no more than one outlier then we are feeling pretty good unless we are in Strict mode (e.g. 100%)
		if ((threshold != 100 && outliers.size() <= 1) || (double)matchCount / realSamples >= getThreshold()/100.0) {
			final RegExpGenerator re = new RegExpGenerator(5, locale);
			// There is some complexity here due to the facts that 'Male' & 'Female' are good predictors of Gender.
			// However a field with only 'M' and 'F' in it is not, so in this case we would like an extra hint.
			if (count == 1) {
				if (!positiveStreamName && (first.equals(genderData.feminineShort) || first.equals(genderData.masculineShort)))
					return new PluginAnalysis(BACKOUT_REGEX);
				re.train(first);
				re.train(opposites.get(first));
			} else if (count == 2) {
				final String second = iter.next();
				if (!positiveStreamName && (first.equals(genderData.feminineShort) || first.equals(genderData.masculineShort)) && (second.equals(genderData.feminineShort) || second.equals(genderData.masculineShort)))
					return new PluginAnalysis(BACKOUT_REGEX);
				if (opposites.get(first).equals(second)) {
					re.train(first);
					re.train(second);
				}
				else
					for (final String element : getMembers())
						re.train(element);
			} else {
				for (final String element : getMembers())
					re.train(element);
			}
			if (!outliers.isEmpty())
				re.train(outliers.keySet().iterator().next());
			happyRegex = re.getResult();
			return PluginAnalysis.OK;
		}

		return new PluginAnalysis(BACKOUT_REGEX);
	}
}
