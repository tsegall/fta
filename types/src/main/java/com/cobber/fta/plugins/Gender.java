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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeFinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect Gender.
 */
public class Gender extends LogicalTypeFinite {
	public static final String SEMANTIC_TYPE = "GENDER.TEXT_";

	private static final String BACKOUT_REGEX = "\\p{IsAlphabetic}+";

	private Map<String, String> opposites;

	private String happyRegex = BACKOUT_REGEX;
	private String language;
	private GenderData genderData;
	private Set<String> languageMembers;


	class GenderPair {
		String feminine;
		String masculine;

		GenderPair(final String feminine, final String masculine) {
			this.feminine = feminine;
			this.masculine = masculine;
		}
	}

	class GenderData {
		private GenderPair[] words;
		private GenderPair[] abbreviations;
		private GenderPair[] all;

		GenderData(final String[][] words, final String[][] abbreviations) {
			this.words = new GenderPair[words.length];
			this.abbreviations = new GenderPair[abbreviations.length];
			this.all = new GenderPair[words.length + abbreviations.length];

			for (int i = 0; i < words.length; i++) {
				this.words[i] = new GenderPair(words[i][0], words[i][1]);
				this.all[i] = new GenderPair(words[i][0], words[i][1]);
			}

			for (int i = 0; i < abbreviations.length; i++) {
				this.abbreviations[i] = new GenderPair(abbreviations[i][0], abbreviations[i][1]);
				this.all[words.length + i] = new GenderPair(abbreviations[i][0], abbreviations[i][1]);
			}
		}

		public GenderPair[] getWords() {
			return words;
		}

		public GenderPair[] getAbbreviations() {
			return abbreviations;
		}

		public GenderPair[] getAll() {
			return all;
		}
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
		return random.nextInt(2) != 0 ? genderData.getWords()[0].feminine : genderData.getWords()[0].masculine;
	}

	public boolean initializeDelayed() {
		language = locale.getLanguage().toUpperCase(Locale.ROOT);

		// Belgium covered by DE, FR, and NL
		// Switzerland covered DE, FR, IT
		// Austria covered by DE
		switch(language) {
		case "BG":
			// Bulgarian
			genderData = new GenderData(
					new String[][] { new String[] { "ЖЕНСКИ", "МЪЖКИ" } },
					new String[][] { new String[] { "Ж", "M" } } );
			break;
		case "CA":
			// Catalan
			genderData = new GenderData(
					new String[][] { new String[] { "DONA", "HOME" }, new String[] { "FEMELLA", "MASCLE" } },
					new String[][] { new String[] { "D", "H" }, new String[] { "F", "M" } });
			break;
		case "DE":
			// German
			genderData = new GenderData(
					new String[][] { new String[] { "WEIBLICH", "MÄNNLICH" }, new String[] { "FRAU", "MANN" } },
					new String[][] { new String[] { "W", "M" }, new String[] { "F", "M" } });
			break;
		case "ES":
			// Spanish
			genderData = new GenderData(
					new String[][] { new String[] { "FEMENINO", "MASCULINO" }, new String[] { "MUJERES", "HOMBRES" }, new String[] { "MUJER", "HOMBRE" } },
					new String[][] { new String[] { "F", "M" }, new String[] { "M", "H" } } );
			break;

		case "FI":
			// Finnish
			genderData = new GenderData(
					new String[][] { new String[] { "NAISET", "MIEHET" } },
					new String[][] { new String[] { "N", "M" } } );
			break;
		case "FR":
			// French
			genderData = new GenderData(
					new String[][] { new String[] { "FEMME", "HOMME" }, new String[] { "FEMMES", "HOMMES" }, new String[] { "FÉMININ", "MASCULIN" } },
					new String[][] { new String[] { "F", "H" }, new String[] { "F", "M" } } );
			break;
		case "HR":
			// Croatian
			genderData = new GenderData(
					new String[][] { new String[] { "MUŠKARCI", "ŽENE" } },
					new String[][] { new String[] { "M", "Z" } } );
			break;
		case "IT":
			// Italian
			genderData = new GenderData(
					new String[][] { new String[] { "FEMMINA", "MASCHIO" }, new String[] { "FEMMINE", "MASCHI" } },
					new String[][] { new String[] { "F", "M" } } );
			break;
		case "MS":
			// Malaysian
			genderData = new GenderData(
					new String[][] { new String[] { "PEREMPUAN", "LELAKI" } },
					new String[][] { new String[] { "P", "L" } } );
			break;
		case "NL":
			// Dutch
			genderData = new GenderData(
					new String[][] { new String[] { "VROUWELIJK", "MANNELIJK" }, new String[] { "VROUW", "MAN" } },
					new String[][] { new String[] { "V", "M" } } );
			break;
		case "PL":
			// Polish
			genderData = new GenderData(
					new String[][] { new String[] { "KOBIETY", "MÊ¿CZYŸNI" } },
					new String[][] { new String[] { "K", "M" } } );
			break;
		case "PT":
			// Portuguese
			genderData = new GenderData(
					new String[][] { new String[] { "FEMININA", "MASCULINO" } },
					new String[][] { new String[] { "F", "M" } } );
			break;
		case "RO":
			// Romanian
			genderData = new GenderData(
					new String[][] { new String[] { "FEMEIE", "MASCULIN" } },
					new String[][] { new String[] { "F", "M" } } );
			break;
		case "RU":
			// Russian
			genderData = new GenderData(
					new String[][] { new String[] { "ЖЕНЩИНА", "МУЖЧИНА" } },
					new String[][] { new String[] { "Ж", "M" } } );
			break;
		case "SV":
			// Swedish
			genderData = new GenderData(
					new String[][] { new String[] { "KVINNA", "MANLIG" } },
					new String[][] { new String[] { "K", "M" } } );
			break;
		case "TR":
			// Turkish
			genderData = new GenderData(
					new String[][] { new String[] { "KADIN", "ERKEK" } },
					new String[][] { new String[] { "K", "E" } } );
			break;
		default:
			// FALL THROUGH TO ENGLISH!!!!
		case "EN":
			// English
			genderData = new GenderData(
					new String[][] { new String[] { "FEMALE", "MALE" }, new String[] { "FEMALES", "MALES" }, new String[] { "WOMEN", "MEN" },
						new String[] { "GF", "GM" } // CDC Gender Categories
					},
					new String[][] { new String[] { "F", "M" } } );
			break;
		}

		languageMembers = new HashSet<>();
		opposites = new HashMap<>();

		// Add the Gender Words (e.g. MALE, FEMALE) and setup opposites
		for (final GenderPair candidate : genderData.getWords()) {
			languageMembers.add(candidate.feminine);
			languageMembers.add(candidate.masculine);
			opposites.put(candidate.feminine, candidate.masculine);
			opposites.put(candidate.masculine, candidate.feminine);
		}
		// Add the Gender Abbreviations (e.g. M, F) and setup opposites
		for (final GenderPair candidate : genderData.getAbbreviations()) {
			languageMembers.add(candidate.feminine);
			languageMembers.add(candidate.masculine);
			opposites.put(candidate.feminine, candidate.masculine);
			opposites.put(candidate.masculine, candidate.feminine);
		}

		return true;
	}

	@Override
	public Set<String> getMembers() {
		if (genderData == null)
			initializeDelayed();
		return languageMembers;
	}

	@Override
	public String getSemanticType() {
		return SEMANTIC_TYPE + language;
	}

	@Override
	public String getRegExp() {
		return 	happyRegex;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		if (cardinality.isEmpty())
			return new PluginAnalysis(BACKOUT_REGEX);

		final boolean positiveStreamName = getHeaderConfidence(context.getStreamName()) > 0;

		if (positiveStreamName) {
			for (final GenderPair candidate : genderData.getAll()) {
				final Long feminine = cardinality.get(candidate.feminine);
				final Long masculine = cardinality.get(candidate.masculine);
				long thisPairCount = 0;
				if (feminine != null)
					thisPairCount += feminine;
				if (masculine != null)
					thisPairCount += masculine;

				// If we have a happy header and a good percentage of FEMALE/MALE F/M (localized) - then assume all is good
				if (thisPairCount > matchCount/2)
				{
					final RegExpGenerator re = new RegExpGenerator(8, locale);
					cardinality.putAll(outliers);
					outliers.clear();
					for (final String item : cardinality.keySet())
						re.train(item);

					// We might have a great header and a set of one or other of the pair - in this case add the opposite
					if (feminine == null)
						re.train(opposites.get(candidate.masculine));
					if (masculine == null)
						re.train(opposites.get(candidate.feminine));
					happyRegex = re.getResult();
					return PluginAnalysis.OK;
				}
			}
		}

		// We made a good faith effort above to be inclusive - so at this stage call it a day if there are too many outliers
		if (outliers.size() > 1 || (!positiveStreamName && cardinality.size() - outliers.size() <= 1))
			return new PluginAnalysis(BACKOUT_REGEX);

		// We have at most one outlier and if the sum of one of the word pairs == matchCount then declare success
		for (final GenderPair candidate : genderData.getWords()) {
			final Long feminine = cardinality.get(candidate.feminine);
			final Long masculine = cardinality.get(candidate.masculine);
			if (feminine == null || masculine == null)
				continue;
			final long thisPairCount = feminine + masculine;
			if (thisPairCount == matchCount) {
				final RegExpGenerator re = new RegExpGenerator(8, locale);
				cardinality.putAll(outliers);
				outliers.clear();
				for (final String item : cardinality.keySet())
					re.train(item);
				happyRegex = re.getResult();
				return PluginAnalysis.OK;
			}
		}

		return new PluginAnalysis(BACKOUT_REGEX);
	}
}
