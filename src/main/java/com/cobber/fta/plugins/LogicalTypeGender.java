/*
 * Copyright 2017-2021 Tim Segall
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

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.FactsTypeBased;
import com.cobber.fta.LogicalTypeFinite;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.Shapes;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.RegExpGenerator;

/**
 * Plugin to detect Gender.
 */
public class LogicalTypeGender extends LogicalTypeFinite {
	public static final String SEMANTIC_TYPE = "GENDER.TEXT_";
	private static final String BACKOUT_REGEX = "\\p{IsAlphabetic}+";

	private static Map<String, GenderData> allGenderData = new HashMap<>();

	private static Map<String, Set<String>> allMembers = new HashMap<>();
	private static Map<String, Map<String, String>> allOpposites = new HashMap<>();

	private Map<String, String> opposites = null;

	private String happyRegex = BACKOUT_REGEX;
	private String language = null;
	private GenderData genderData = null;

	static class GenderData {
		String header;
		String feminine;
		String masculine;
		String feminine_short;
		String masculine_short;

		GenderData(String header, String feminine, String masculine, String feminine_short, String masculine_short) {
			this.header = header;
			this.feminine = feminine;
			this.masculine = masculine;
			this.feminine_short = feminine_short;
			this.masculine_short = masculine_short;
		}
	}

	static {
		allGenderData.put("DE", new GenderData(".*(?i)(Gender|Geschlecht)", "WEIBLICH", "MÄNNLICH", "W", "M"));
		allGenderData.put("EN", new GenderData(".*(?i)(Gender|sex)", "FEMALE", "MALE", "F", "M"));
		allGenderData.put("ES", new GenderData(".*(?i)(Gender|Sexo)", "FEMENINO", "MASCULINO", "F", "M"));
		allGenderData.put("IT", new GenderData(".*(?i)(Gender|genere)", "FEMMINA", "MASCHIO", "F", "M"));
		allGenderData.put("FR", new GenderData(".*(?i)(Gender|Genre|Sexe)", "FEMME", "HOMME", "F", "H"));
		allGenderData.put("NL", new GenderData(".*(?i)(Gender|Geslach|Geslacht)", "VROUWELIJK", "MANNELIJK", "V", "M"));
		allGenderData.put("PT", new GenderData(".*(?i)(Gender|Gênero)", "FEMININO", "MASCULINO", "F", "M"));
		allGenderData.put("TR", new GenderData(".*(?i)(Gender)", "KADIN", "ERKEK", "K", "E"));

		// Belgium covered by DE, FR, and NL
		// Switzerland covered DE, FR, IT
		// Austria covered by DE
	}

	public LogicalTypeGender(final PluginDefinition plugin) throws FileNotFoundException {
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
			opposites.put(genderData.feminine_short, genderData.masculine_short);
			opposites.put(genderData.masculine_short, genderData.feminine_short);
			opposites.put(genderData.feminine, genderData.masculine);
			opposites.put(genderData.masculine, genderData.feminine);
			allOpposites.put(language, opposites);
		}

		threshold = 95;

		return true;
	}

	@Override
	public Set<String> getMembers() {
		String setupLanguage = locale.getLanguage().toUpperCase(Locale.ROOT);
		Set<String> languageMembers = allMembers.get(setupLanguage);

		if (languageMembers == null) {
			GenderData setup = allGenderData.get(setupLanguage);

			languageMembers = new HashSet<>();
			languageMembers.add(setup.feminine_short);
			languageMembers.add(setup.masculine_short);
			languageMembers.add(setup.feminine);
			languageMembers.add(setup.masculine);

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
	public String isValidSet(final AnalyzerContext context, final long matchCount, final long realSamples, String currentRegExp, final FactsTypeBased facts, Map<String, Long> cardinality, final Map<String, Long> outliers, final Shapes shapes, AnalysisConfig analysisConfig) {

		// Feel like this should be a little more inclusive in this day and age but not sure what set to use!!
		if (outliers.size() > 1)
			return BACKOUT_REGEX;

		final boolean positiveStreamName = context.getStreamName().matches(genderData.header);
		if (!positiveStreamName && cardinality.size() - outliers.size() <= 1)
			return BACKOUT_REGEX;

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
				if (!positiveStreamName && (first.equals(genderData.feminine_short) || first.equals(genderData.masculine_short)))
					return BACKOUT_REGEX;
				re.train(first);
				re.train(opposites.get(first));
			} else if (count == 2) {
				final String second = iter.next();
				if (!positiveStreamName && (first.equals(genderData.feminine_short) || first.equals(genderData.masculine_short)) && (second.equals(genderData.feminine_short) || second.equals(genderData.masculine_short)))
					return BACKOUT_REGEX;
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
			return null;
		}

		return BACKOUT_REGEX;
	}
}
