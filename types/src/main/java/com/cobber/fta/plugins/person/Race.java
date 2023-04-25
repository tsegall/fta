/* Copyright 2017-2023 Tim Segall
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
package com.cobber.fta.plugins.person;

import com.cobber.fta.PluginDefinition;

/**
* Plugin to detect Race (Person).
*/
public class Race extends SimpleWords {
	public Race(final PluginDefinition plugin) {
		super(plugin);
	}

	private static final String[] raceWordsEN = {
			"ABORIGINAL", "AFRICAN", "AIAN", "ALLRACE", "AMERICAN", "ANGLO", "ARAB", "ASIAN", "ASIATIC", "ASN", "BAME", "BLACK", "BLK", "CARIBBEAN", "CAUCASIAN", "CHINESE",
			"FILIPINO", "HAWAIIAN", "HISP", "HISPANIC", "INDIAN", "INUIT", "ISLANDER", "JAPANESE", "KOREAN", "LATINA", "LATINO", "LATINX", "METIS", "MIDDLE EASTERN",
			"MULTIRACIAL", "MULTI-RACIAL", "NATIVE", "NON-HISPANIC", "OCEANIA", "OCEANIC", "PACIFIC", "PAKISTANI", "RACES", "VIETNAMESE", "WHI", "WHITE", "WHT",

			"N/A", "NA", "NO DATA", "NONE", "OTHER", "OTH", "UNK", "UNANSWERED", "UNKNOWN"
	};

	@Override
	protected String[] getWords() {
		return raceWordsEN;
	}

	@Override
	protected int getMaxWords() {
		return 8;
	}
}
