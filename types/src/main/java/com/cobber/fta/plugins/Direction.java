/*
 * Copyright 2017-2025 Tim Segall
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

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;

/**
 * Plugin to detect Cardinal Directions.
 */
public class Direction extends LogicalTypeInfinite {
	enum Form {
		CARDINAL("(?i)(E|N|S|W)", new String[] { "E", "N", "S", "W" }),
		INTERCARDINAL("(?i)(NE|NW|SE|SW)", new String[] { "NE", "NW", "SE", "SW" }),
		CARDINAL_BOTH("(?i)(E|N|NE|NW|S|SE|SW|W)", new String[] { "E", "N", "NE", "NW", "S", "SE", "SW", "W" }),

		CARDINAL_FULL("(?i)(EAST|NORTH|SOUTH|WEST)", new String[] { "EAST", "NORTH", "SOUTH", "WEST" }),
		INTERCARDINAL_FULL("(?i)(NORTHEAST|NORTHWEST|SOUTHEAST|SOUTHWEST)",
				new String[] { "NORTHEAST", "NORTHWEST", "SOUTHEAST", "SOUTHWEST" }),
		CARDINAL_FULL_BOTH("(?i)(EAST|NORTH|NORTHEAST|NORTHWEST|SOUTH|SOUTHEAST|SOUTHWEST|WEST)",
				new String[] { "EAST", "NORTH", "NORTHEAST", "NORTHWEST", "SOUTH", "SOUTHEAST", "SOUTHWEST", "WEST" }),

		BOUND_SHORT("(?i)(EB|NB|SB|WB)", new String[] { "EB", "NB", "SB", "WB" }),
		BOUND_LONG("(?i)(EASTBOUND|NORTHBOUND|SOUTHBOUND|WESTBOUND)",
				new String[] { "EASTBOUND", "NORTHBOUND", "SOUTHBOUND", "WESTBOUND" }),

		UNKNOWN("(?i)(E|N|S|W)", new String[] { "E", "N", "S", "W" });

		/** The RegExp that represents this Form */
		private final String regExp;
		/** The set of members valid for this Form. */
		private final String[] members;

		Form(final String regExp, final String[] members) {
			this.regExp = regExp;
			this.members = members;
		}

		String getRegExp() {
			return regExp;
		}

		String[] getMembers() {
			return members;
		}
	}

	private final int[] boundShortCounts = new int[4];
	private final int[] boundLongCounts = new int[4];
	private final int[] cardinalCounts = new int[4];
	private final int[] intercardinalCounts = new int[4];
	private final int[] cardinalFullCounts = new int[4];
	private final int[] intercardinalFullCounts = new int[4];

	private Form currentForm = Form.UNKNOWN;

	/**
	 * Construct a plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public Direction(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		return true;
	}

	@Override
	public String nextRandom() {
		return Form.CARDINAL.getMembers()[getRandom().nextInt(4)];
	}

	@Override
	public boolean acceptsBaseType(final FTAType type) {
		return type == FTAType.LONG || type == FTAType.STRING;
	}

	@Override
	public String getRegExp() {
		return currentForm.getRegExp();
	}

	@Override
	public boolean isRegExpComplete() {
		return true;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		final int len = input.length();
		final String upper = input.toUpperCase(locale);

		int index = indexOf(upper, Form.CARDINAL.getMembers());
		if (len == 1 && index != -1) {
			cardinalCounts[index]++;
			if (currentForm == Form.UNKNOWN)
				currentForm = Form.CARDINAL;
			else if (currentForm == Form.INTERCARDINAL)
				currentForm = Form.CARDINAL_BOTH;
			return true;
		}

		if (len == 2) {
			index = indexOf(upper, Form.INTERCARDINAL.getMembers());
			if (index != -1) {
				intercardinalCounts[index]++;
				if (currentForm == Form.UNKNOWN)
					currentForm = Form.INTERCARDINAL;
				else if (currentForm == Form.CARDINAL)
					currentForm = Form.CARDINAL_BOTH;
				return true;
			}
			index = indexOf(upper, Form.BOUND_SHORT.getMembers());
			if (index == -1)
				return false;
			boundShortCounts[index]++;
			if (currentForm == Form.UNKNOWN)
				currentForm = Form.BOUND_SHORT;
			return true;
		}

		index = indexOf(upper, Form.CARDINAL_FULL.getMembers());
		if (index != -1) {
			cardinalFullCounts[index]++;
			if (currentForm == Form.UNKNOWN)
				currentForm = Form.CARDINAL_FULL;
			else if (currentForm == Form.INTERCARDINAL_FULL)
				currentForm = Form.CARDINAL_FULL_BOTH;
			return true;
		}

		index = indexOf(upper, Form.BOUND_LONG.getMembers());
		if (index != -1) {
			boundLongCounts[index]++;
			if (currentForm == Form.UNKNOWN)
				currentForm = Form.BOUND_LONG;
			return true;
		}

		index = indexOf(upper, Form.INTERCARDINAL_FULL.getMembers());
		if (index != -1) {
			intercardinalFullCounts[index]++;
			if (currentForm == Form.UNKNOWN)
				currentForm = Form.INTERCARDINAL_FULL;
			else if (currentForm == Form.CARDINAL_FULL)
				currentForm = Form.CARDINAL_FULL_BOTH;
			return true;
		}

		return false;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return isValid(trimmed, true, 0);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		long numberSeen = 0;
		switch (currentForm) {
		case UNKNOWN:
			return PluginAnalysis.SIMPLE_NOT_OK;
		case BOUND_SHORT:
			numberSeen = seenCount(boundShortCounts);
			if (numberSeen != 4 && getHeaderConfidence(context) < 90)
				return PluginAnalysis.SIMPLE_NOT_OK;
			break;
		case BOUND_LONG:
			numberSeen = seenCount(boundLongCounts);
			if (numberSeen != 4 && getHeaderConfidence(context) < 90)
				return PluginAnalysis.SIMPLE_NOT_OK;
			break;
		case CARDINAL:
			numberSeen = seenCount(cardinalCounts);
			if (numberSeen != 4 && getHeaderConfidence(context) < 90)
				return PluginAnalysis.SIMPLE_NOT_OK;
			break;
		case INTERCARDINAL:
			numberSeen = seenCount(intercardinalCounts);
			if (numberSeen != 4 && getHeaderConfidence(context) < 90)
				return PluginAnalysis.SIMPLE_NOT_OK;
			break;
		case INTERCARDINAL_FULL:
			numberSeen = seenCount(intercardinalFullCounts);
			if (numberSeen != 4 && getHeaderConfidence(context) < 90)
				return PluginAnalysis.SIMPLE_NOT_OK;
			break;
		case CARDINAL_FULL:
			numberSeen = seenCount(intercardinalCounts);
			if (numberSeen != 4 && getHeaderConfidence(context) < 90)
				return PluginAnalysis.SIMPLE_NOT_OK;
			break;
		case CARDINAL_BOTH:
			numberSeen = seenCount(cardinalCounts) + seenCount(intercardinalCounts);
			if (numberSeen < 5 && getHeaderConfidence(context) < 90)
				return PluginAnalysis.SIMPLE_NOT_OK;
			break;
		case CARDINAL_FULL_BOTH:
			numberSeen = seenCount(cardinalFullCounts) + seenCount(intercardinalFullCounts);
			if (numberSeen < 5 && getHeaderConfidence(context) < 90)
				return PluginAnalysis.SIMPLE_NOT_OK;
			break;
		}

		return 	(double) matchCount / realSamples >= getThreshold() / 100.0 ?  PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}

	private int seenCount(final int[] counts) {
		int ret = 0;
		for (final int seen : counts)
			if (seen != 0)
				ret++;
		return ret;
	}

	private int indexOf(final String input, final String[] toSearch) {
		for (int i = 0; i < toSearch.length; i++)
			if (input.equals(toSearch[i]))
				return i;
		return -1;
	}

	@Override
	public boolean isClosed() {
		return true;
	}
}
