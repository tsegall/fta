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
		BOUND_SHORT("(?i)(EB|NB|SB|WB)", new String[] { "EB", "NB", "SB", "WB" }),
		BOUND_LONG("(?i)(EASTBOUND|NORTHBOUND|SOUTHBOUND|WESTBOUND)", new String[] { "EASTBOUND", "NORTHBOUND", "SOUTHBOUND", "WESTBOUND" }),
		INTERCARDINAL("(?i)(NE|NW|SE|SW)", new String[] { "NE", "NW", "SE", "SW" }),
		CARDINAL_BOTH("(?i)(E|N|NE|NW|S|SE|SW|W)", new String[] { "E", "N", "NE", "NW", "S", "SE", "SW", "W" }),
		FULL("(?i)(EAST|NORTH|SOUTH|WEST)", new String[] { "EAST", "NORTH", "SOUTH", "WEST" }),
		UNKNOWN("(?i)(E|N|S|W)", new String[] { "E", "N", "S", "W" });

		/** The RegExp that represents this Form */
		private String regExp;
		/** The set of members valid for this Form. */
		private String[] members;

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
	};

	private int[] boundShortCounts = new int[4];
	private int[] boundLongCounts = new int[4];
	private int[] cardinalCounts = new int[4];
	private int[] intercardinalCounts = new int[4];
	private int[] fullCounts = new int[4];

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
		return Form.CARDINAL.getMembers()[random.nextInt(4)];
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

		index = indexOf(upper, Form.FULL.getMembers());
		if (index != -1) {
			fullCounts[index]++;
			if (currentForm == Form.UNKNOWN)
				currentForm = Form.FULL;
			return true;
		}

		index = indexOf(upper, Form.BOUND_LONG.getMembers());
		if (index == -1)
			return false;

		boundLongCounts[index]++;
		if (currentForm == Form.UNKNOWN)
			currentForm = Form.BOUND_LONG;

		return true;
	}

	@Override
	public boolean isCandidate(String trimmed, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		return isValid(trimmed, true, 0);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		if (currentForm == Form.UNKNOWN)
			return PluginAnalysis.SIMPLE_NOT_OK;
		else if (currentForm == Form.BOUND_SHORT) {
			int numberSeen = seenCount(boundShortCounts);
			if (numberSeen != 4 && getHeaderConfidence(context.getStreamName()) < 90)
				return PluginAnalysis.SIMPLE_NOT_OK;
		}
		else if (currentForm == Form.BOUND_LONG) {
			int numberSeen = seenCount(boundLongCounts);
			if (numberSeen != 4 && getHeaderConfidence(context.getStreamName()) < 90)
				return PluginAnalysis.SIMPLE_NOT_OK;
		}
		else if (currentForm == Form.CARDINAL) {
			int numberSeen = seenCount(cardinalCounts);
			if (numberSeen != 4 && getHeaderConfidence(context.getStreamName()) < 90)
				return PluginAnalysis.SIMPLE_NOT_OK;
		}
		else if (currentForm == Form.INTERCARDINAL) {
			int numberSeen = seenCount(intercardinalCounts);
			if (numberSeen != 4 && getHeaderConfidence(context.getStreamName()) < 90)
				return PluginAnalysis.SIMPLE_NOT_OK;
		}
		else if (currentForm == Form.FULL) {
			int numberSeen = seenCount(intercardinalCounts);
			if (numberSeen != 4 && getHeaderConfidence(context.getStreamName()) < 90)
				return PluginAnalysis.SIMPLE_NOT_OK;
		}
		else if (currentForm == Form.CARDINAL_BOTH) {
			int numberSeen = seenCount(cardinalCounts) + seenCount(intercardinalCounts);
			if (numberSeen < 5 && getHeaderConfidence(context.getStreamName()) < 90)
				return PluginAnalysis.SIMPLE_NOT_OK;
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
