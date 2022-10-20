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
import java.util.regex.Pattern;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.InternalErrorException;
import com.cobber.fta.core.RegExpGenerator;
import com.cobber.fta.core.Utils;
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
	private boolean xegerCompatible = true;
	private NumberFormat longFormatter;
	private NumberFormat doubleFormatter;
	private PluginMatchEntry matchEntry = null;
	private SingletonSet samples;
	private boolean randomInitialized;

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
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		random = new SecureRandom(new byte[] { 3, 1, 4, 1, 5, 9, 2 });

		longFormatter = NumberFormat.getIntegerInstance(locale);
		doubleFormatter = NumberFormat.getInstance(locale);

		return true;
	}

	@Override
	public String getSemanticType() {
		return defn.semanticType;
	}

	@Override
	public boolean isRegExpComplete() {
		if (matchEntry != null)
			return matchEntry.isRegExpComplete();
		return pluginLocaleEntry.matchEntries[0].isRegExpComplete();
	}

	@Override
	public String getRegExp() {
		if (matchEntry != null)
			return matchEntry.getRegExpReturned();
		return pluginLocaleEntry.matchEntries[0].getRegExpReturned();
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
				ret = doubleFormatter.parse(input.charAt(0) == '+' ? input.substring(1) : input).doubleValue();
			} catch (ParseException pe) {
				throw new NumberFormatException(pe.getMessage());
			}
		}

		return ret;
	}

	private Pattern getPattern() {
		if (pattern != null)
			return pattern;

		final String toCompile = getRegExp();
		if (toCompile == null)
			throw new InternalErrorException("Failed to locate pattern, matchEntry = " + matchEntry);

		try {
			pattern = Pattern.compile(toCompile);
		}
		catch (Exception e) {
			throw new InternalErrorException("Failed to compile pattern, RegExpReturned = " + matchEntry, e);
		}

		return pattern;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode) {
		if (!getPattern().matcher(Utils.cleanse(input.trim())).matches())
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
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig) {

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
					return new PluginAnalysis(backout);
					break;

			default:
				throw new InternalErrorException(WRONG_TYPE + defn.baseType);
			}
		}

		if ((double)matchCount / realSamples >= getThreshold()/100.0)
			return PluginAnalysis.OK;

		return new PluginAnalysis(backout);
	}

	/**
	 * Test if the Regular Expression is in the current MatchEntry if we have already decided
	 * which one we match or in all the entries if no decision made yet.  Note: if we had not already
	 * decided which entry - then it will be set as a side-effect if the Regular Expression is found.
	 * @param regExp The Regular expression we are searching for
	 * @return True if the Regular Expression matches the current Match Entry
	 */
	public boolean isMatch(final String regExp) {
		if (regExp == null)
			return false;

		if (matchEntry != null)
			return matchEntry.matches(regExp);

		for (final PluginMatchEntry entry : pluginLocaleEntry.matchEntries)
			if (entry.matches(regExp)) {
				matchEntry = entry;
				return true;
			}

		return false;
	}

	public PluginMatchEntry[] getMatchEntries() {
		return pluginLocaleEntry.matchEntries;
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

	/*
	 * There are two options for generating random samples for 'regex' plugins.  Either you provide a list of samples via the content and then
	 * these are selected at from random or you use Xeger to generate samples based on the regular expression returned.
	 * We use samples for things like 'CITY' where we prefer to see 'Paris', 'London' etc as opposed to 'aqrzx' which is what would be generated
	 * if we simply use the regular expression.
	 */
	@Override
	public String nextRandom() {
		if (!randomInitialized) {
			// Check to see if we have been provided with a set of samples
			if (defn.content != null && samples == null)
				samples = new SingletonSet(defn.contentType, defn.content);
			else {
				final String regExp = getRegExp();
				// Xeger cannot cope with things like 'zero-width negative lookahead' - '(?!X' - so give up in this case
				if (regExp.matches(".*[^\\\\]\\(\\?.*")) {
					xegerCompatible = false;
					return null;
				}

				generator = new Xeger(RegExpGenerator.toAutomatonRE(regExp, true));
			}
			randomInitialized = true;
		}

		// We have samples so use them
		if (samples != null)
			return samples.getRandom(random);

		if (!xegerCompatible)
			return null;

		// If the Regular Expression is complete - then anything generated is valid and so we are done
		if (isRegExpComplete())
			return generator.generate();

		// If the Regular Expression is not complete - then the sample Xeger generates may not be valid,
		// attempt to generate multiple samples hoping that we will get one that works, but give up if we
		// cannot succeed.
		String ret;
		long retries = 0;
		do {
			ret = generator.generate();
			retries++;
			// If we have tried too many time then give up and return an invalid entry.
			if (retries%10 == 0) {
				return ret;
			}
		} while (!isValid(ret, true));

		return ret;
	}

	@Override
	public void seed(final byte[] seed) {

		random = new SecureRandom(seed);
	}

	public PluginMatchEntry getMatchEntry() {
		return matchEntry;
	}

	public void setMatchEntry(PluginMatchEntry matchEntry) {
		this.matchEntry = matchEntry;
	}
}
