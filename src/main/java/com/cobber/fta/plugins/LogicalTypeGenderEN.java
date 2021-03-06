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

import com.cobber.fta.LogicalTypeFinite;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.Shapes;
import com.cobber.fta.FactsTypeBased;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.RegExpGenerator;

/**
 * Plugin to detect Gender. (English-language only).
 */
public class LogicalTypeGenderEN extends LogicalTypeFinite {
	public static final String SEMANTIC_TYPE = "GENDER.TEXT_EN";
	private static Set<String> members = new HashSet<>();
	private static final String BACKOUT_REGEX = "\\p{IsAlphabetic}+";
	private String happyRegex = "\\p{Alpha}+";
	private static Map<String, String> opposites = new HashMap<>();

	static {
		members.add("F");
		members.add("M");
		members.add("FEMALE");
		members.add("MALE");

		opposites.put("F", "M");
		opposites.put("M", "F");
		opposites.put("FEMALE", "MALE");
		opposites.put("MALE", "FEMALE");
	}

	public LogicalTypeGenderEN(final PluginDefinition plugin) throws FileNotFoundException {
		super(plugin);
	}

	@Override
	public String nextRandom() {
		return random.nextInt(2) != 0 ? "FEMALE" : "MALE";
	}

	@Override
	public boolean initialize(final Locale locale) throws FTAPluginException {
		super.initialize(locale);

		threshold = 95;

		return true;
	}

	@Override
	public Set<String> getMembers() {
		return members;
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public String getRegExp() {
		return 	happyRegex;
	}

	@Override
	public String isValidSet(final String dataStreamName, final long matchCount, final long realSamples, final FactsTypeBased facts, Map<String, Long> cardinality, final Map<String, Long> outliers, final Shapes shapes) {

		// Feel like this should be a little more inclusive in this day and age but not sure what set to use!!
		if (outliers.size() > 1)
			return BACKOUT_REGEX;

		final boolean positiveStreamName = dataStreamName.toLowerCase(locale).contains("gender");
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
				if (!positiveStreamName && (first.equals("M") || first.equals("F")))
					return BACKOUT_REGEX;
				re.train(first);
				re.train(opposites.get(first));
			} else if (count == 2) {
				final String second = iter.next();
				if (!positiveStreamName && (first.equals("M") || first.equals("F")) && (second.equals("M") || second.equals("F")))
					return BACKOUT_REGEX;
				if (opposites.get(first).equals(second)) {
					re.train(first);
					re.train(second);
				}
				else
					for (final String element : members)
						re.train(element);
			} else {
				for (final String element : members)
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
