/*
 * Copyright 2017-2023 Tim Segall
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
 * Plugin to detect Quarters (e.g. Q1).
 */
public class PeriodQuarter extends LogicalTypeInfinite {

	enum Form {
		Long ("(?i)(Quarter[1234])", "(?i)(Quarter ?[1234])"),		// Quarter 1
		Medium("(?i)(QTR1|QTR2|QTR3|QTR4)", "(?i)(QTR ?[1234])"),	// QTR 1
		Short("(?i)(Q1|Q2|Q3|Q4)", "(?i)(Q ?[1234])"),				// Q1
		Digit("[1234]"),											// 1
		Nth("(?i)(1st|2nd|3rd|4th)"),								// 1st
		Unknown("Unknown");

		/** The RegExp that represents this token. */
		private final String regExp;
		/** The RegExp that represents this token. */
		private String regExpWithSpace;

		Form(final String regExp, final String regExpWithSpace) {
			this.regExp = regExp;
			this.regExpWithSpace = regExpWithSpace;
		}

		Form(final String regExp) {
			this.regExp = regExp;
		}

		String getRegExp(final boolean spaceSeen) {
			return spaceSeen ? regExpWithSpace : regExp;
		}
	}

	private Form form = Form.Unknown;

	private boolean spaceSeen;

	/**
	 * Construct a plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public PeriodQuarter(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		return true;
	}

	@Override
	public String nextRandom() {
		return "Q" + (1 + getRandom().nextInt(4));
	}

	@Override
	public FTAType getBaseType() {
		return form == Form.Digit ? FTAType.LONG : FTAType.STRING;
	}

	@Override
	public boolean acceptsBaseType(final FTAType type) {
		return type == FTAType.LONG || type == FTAType.STRING;
	}

	@Override
	public String getRegExp() {
		return form.getRegExp(spaceSeen);
	}

	@Override
	public boolean isRegExpComplete() {
		return true;
	}

	private boolean validQuarter(final String input, final int initialOffset) {
		final int len = input.length();
		if (initialOffset == len)
			return false;

		int offset = initialOffset;
		if (offset < len + 1 && input.charAt(offset) == ' ') {
			spaceSeen = true;
			offset++;
		}

		final char q = input.charAt(offset);

		return q == '1' || q == '2' || q == '3' || q == '4';
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		final int len = input.length();
		if (len == 0)
			return false;
		final String upper = input.toUpperCase(locale);
		final char first = input.charAt(0);

		switch (form) {
		case Digit:
			return validQuarter(input, 0);

		case Long:
			return upper.startsWith("QUARTER") && validQuarter(upper, "QUARTER".length());

		case Medium:
			return upper.startsWith("QTR") && validQuarter(upper, "QTR".length());

		case Nth:
			return "1ST".equals(upper) || "2ND".equals(upper) || "3RD".equals(upper) || "4TH".equals(upper);

		case Short:
			return upper.startsWith("Q") && validQuarter(upper, "Q".length());

		case Unknown:
			if (first == '1' || first == '2' || first == '3' || first == '4')
				form = len == 1 ? Form.Digit : Form.Nth;
			else if (upper.startsWith("QUARTER"))
				form = Form.Long;
			else if (upper.startsWith("QTR"))
				form = Form.Medium;
			else if (upper.startsWith("Q"))
				form = Form.Short;
			else
				return false;

			return isValid(input, detectMode, count);
		}

		return false;
	}


	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return isValid(trimmed, true, -1);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {
		if ((form == Form.Digit || form == Form.Nth) && getHeaderConfidence(context.getStreamName()) < 99)
			return PluginAnalysis.SIMPLE_NOT_OK;

		return (double) matchCount / realSamples >= getThreshold() / 100.0 ?  PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}

	@Override
	public boolean isClosed() {
		return true;
	}
}
