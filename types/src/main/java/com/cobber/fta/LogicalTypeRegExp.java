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
package com.cobber.fta;

import java.security.SecureRandom;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.InternalErrorException;
import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.token.TokenStreams;

import nl.flotsam.xeger.Xeger;

/**
 * This class supports all plugins that are based on a RegExp.
 */
public class LogicalTypeRegExp extends LogicalType {
	private static final String WRONG_TYPE = "LogicalTypeRegExp baseType must be LONG, DOUBLE or STRING, not ";
	private Pattern pattern;
	private Long minLong;
	private Long maxLong;
	private Double minDouble;
	private Double maxDouble;
	private String minString;
	private String maxString;
	protected SecureRandom random;
	private Xeger generator;
	private NumberFormat longFormatter;
	private NumberFormat doubleFormatter;

	public LogicalTypeRegExp(final PluginDefinition plugin) throws FTAPluginException {
		super(plugin);

		if (defn.minimum != null || defn.maximum != null)
			switch (defn.baseType) {
			case LONG:
				minLong = defn.minimum != null ? Long.parseLong(defn.minimum) : null;
				maxLong = defn.maximum != null ? Long.parseLong(defn.maximum) : null;
				break;

			case DOUBLE:
				minDouble = defn.minimum != null ? Double.parseDouble(defn.minimum) : null;
				maxDouble = defn.maximum != null ? Double.parseDouble(defn.maximum) : null;
				break;

			case STRING:
				minString = defn.minimum;
				maxString = defn.minimum;
				break;

			default:
				throw new FTAPluginException(WRONG_TYPE + defn.baseType);
			}
	}

	@Override
	public boolean initialize(final Locale locale) throws FTAPluginException {
		super.initialize(locale);

		try {
			pattern = Pattern.compile(pluginLocaleEntry.regExpReturned);
		}
		catch (Exception e) {
			return false;
		}

		random = new SecureRandom(new byte[] { 3, 1, 4, 1, 5, 9, 2 });

		longFormatter = NumberFormat.getIntegerInstance(locale);
		doubleFormatter = NumberFormat.getInstance(locale);

		return true;
	}

	@Override
	public String getQualifier() {
		return defn.qualifier;
	}

	@Override
	public String getRegExp() {
		return pluginLocaleEntry.regExpReturned;
	}

	public String[] getRegExpToMatch() {
		return pluginLocaleEntry.getRegExpsToMatch();
	}

	@Override
	public FTAType getBaseType() {
		return defn.baseType;
	}

	private long safeParseLong(final String input) {
		long ret;
		try {
			ret = Long.parseLong(input);
		}
		catch (NumberFormatException e) {
			try {
				ret = longFormatter.parse(input).longValue();
			} catch (ParseException pe) {
				throw new NumberFormatException(pe.getMessage());
			}
		}

		return ret;
	}

	private double safeParseDouble(final String input) {
		double ret;
		try {
			ret = Double.parseDouble(input);
		}
		catch (NumberFormatException e) {
			try {
				ret = doubleFormatter.parse(input).doubleValue();
			} catch (ParseException pe) {
				throw new NumberFormatException(pe.getMessage());
			}
		}

		return ret;
	}

	@Override
	public boolean isValid(final String input) {
		if (!pattern.matcher(input).matches())
			return false;

		if (defn.minimum != null || defn.maximum != null)
			switch (defn.baseType) {
			case LONG:
				final long inputLong = safeParseLong(input);
				if (minLong != null && inputLong < minLong)
					return false;
				if (maxLong != null && inputLong > maxLong)
					return false;
				break;

			case DOUBLE:
				final double inputDouble = safeParseDouble(input);
				if (minDouble != null && inputDouble < minDouble)
					return false;
				if (maxDouble != null && inputDouble > maxDouble)
					return false;
				break;

			case STRING:
				if (minString != null && input.compareTo(minString) < 0)
					return false;
				if (maxString != null && input.compareTo(maxString) > 0)
					return false;
				break;

			default:
				throw new InternalErrorException(WRONG_TYPE + defn.baseType);
			}

		return defn.invalidList == null || !defn.invalidList.contains(input);
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final Map<String, Long> cardinality, final Map<String, Long> outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

		final String backout = currentRegExp;

		// If this plugin insists on a minimum number of samples (validate it)
		if (realSamples < getMinSamples())
			return new PluginAnalysis(backout);

		// Plugins can insist that the maximum and minimum values be present in the observed set.
		if (isMinMaxPresent() && (cardinality.get(defn.minimum) == null || cardinality.get(defn.maximum) == null))
			return new PluginAnalysis(backout);

		if (defn.isRequiredHeaderMissing(locale, context.getStreamName()))
			return new PluginAnalysis(backout);

		if (facts != null) {
			switch (defn.baseType) {
			case LONG:
				if ((minLong != null && (Long)facts.getMin() < minLong) ||
						(maxLong != null && (Long)facts.getMax() > maxLong))
					return new PluginAnalysis(backout);;
					break;

			case DOUBLE:
				if ((minDouble != null && (Double)facts.getMin() < minDouble) ||
						(maxDouble != null && (Double)facts.getMax() > maxDouble))
					return new PluginAnalysis(backout);;
					break;

			case STRING:
				if ((minString != null && facts.getMinValue().compareTo(minString) < 0) ||
						(maxString != null && facts.getMaxValue().compareTo(maxString) > 0))
					return new PluginAnalysis(backout);;
					break;

			default:
				throw new InternalErrorException(WRONG_TYPE + defn.baseType);
			}
		}

		if ((double)matchCount / realSamples >= getThreshold()/100.0)
			return PluginAnalysis.OK;

		return new PluginAnalysis(backout);
	}

	public boolean isMatch(final String regExp) {
		// The optional 'regExpsToMatch' tag is an ordered list of Regular Expressions used to match against the Stream Data.
		// If not set then the regExpReturned is used to match.
		if (pluginLocaleEntry.getRegExpsToMatch() == null)
			return regExp.equals(pluginLocaleEntry.regExpReturned);

		for (final String re : pluginLocaleEntry.getRegExpsToMatch()) {
			if (regExp.equals(re))
				return true;
		}

		return false;
	}

	public int getMinSamples() {
		return defn.minSamples;
	}

	public boolean isMinMaxPresent() {
		return defn.minMaxPresent;
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public String nextRandom() {
		if (generator == null)
			generator = new Xeger(RegExpGenerator.toAutomatonRE(pluginLocaleEntry.regExpReturned, true));

		return generator.generate();
	}

	@Override
	public void seed(final byte[] seed) {

		random = new SecureRandom(seed);
	}
}
