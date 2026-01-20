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
package com.cobber.fta;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Locale;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.dates.LocaleInfo;
import com.cobber.fta.dates.LocaleInfoConfig;
import com.cobber.fta.token.TokenStreams;

/**
 * All Semantic Types are derived from this abstract class.
 * This LTRandom interface provides a {@link LTRandom#nextRandom} which will create a new valid example of the
 *  Semantic Type.
 */
public abstract class LogicalType implements Comparable<LogicalType>, LTRandom {
	protected PluginDefinition defn;
	protected AnalysisConfig analysisConfig;
	protected Locale locale;
	protected LocaleInfo localeInfo;
	protected int threshold;
	protected PluginLocaleEntry pluginLocaleEntry;


	@Override
	public int compareTo(final LogicalType other) {
	  return Integer.compare(defn.getOrder(), other.defn.getOrder());
	}

	/**
	 * LogicalType constructor.
	 * @param plugin The definition of this plugin.
	 */
	public LogicalType(final PluginDefinition plugin) {
		this.defn = plugin;
		this.threshold = plugin.threshold;
	}

	/**
	 * Called to perform any initialization.
	 * @param analysisConfig The Analysis configuration used for this analysis
	 * @return True if initialization was successful.
	 * @throws FTAPluginException Thrown when the plugin is incorrectly configured.
	 */
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		if (getBaseType() == null)
			throw new FTAPluginException("baseType cannot be null");

		this.analysisConfig = analysisConfig;
		this.locale = analysisConfig.getLocale();
		this.localeInfo = LocaleInfo.getInstance(new LocaleInfoConfig(locale, analysisConfig.isEnabled(TextAnalyzer.Feature.NO_ABBREVIATION_PUNCTUATION), analysisConfig.isEnabled(TextAnalyzer.Feature.ALLOW_ENGLISH_AMPM)));

		pluginLocaleEntry = defn.getLocaleEntry(locale);

		if (pluginLocaleEntry == null)
			throw new FTAPluginException("Plugin: " + defn.semanticType + " has no support for " + locale.toLanguageTag());

		return true;
	}

	/**
	 * Determine the confidence that the name of the data stream is likely a valid header for this Semantic Type.
	 * Positive Numbers indicate it could be this Semantic Type, negative numbers indicate is it unlikely to be this Semantic Type, 0 indicates no opinion.
	 * @param dataStreamName The name of this data stream
	 * @return An integer between -100 and 100 reflecting the confidence that this stream name is a valid header.
	 */
	public int getHeaderConfidence(final String dataStreamName) {
		return pluginLocaleEntry.getHeaderConfidence(dataStreamName);
	}

	/**
	 *  The name of the Semantic Type.  For example, EMAIL for an email address.
	 *  @return The name of the Semantic Type.
	 */
	public String getSemanticType() {
		return defn.semanticType;
	}

	/**
	 *  The user-friendly description of the Semantic Type.  For example, 'Australian State' for the Semantic Type "STATE_PROVINCE.STATE_AU".
	 *  @return The user-friendly description of the Semantic Type.
	 */
	public String getDescription() {
		return defn.description;
	}

	/**
	 *  The relative priority of this plugin.
	 *  @return The relative priority of this plugin.
	 */
	public int getPriority() {
		return defn.priority;
	}

	/**
	 *  Is this plugin sensitive to the input locale.
	 *  @return True if the plugin is sensitive to the input locale.
	 */
	public boolean isLocaleSensitive() {
		return defn.localeSensitive;
	}

	/**
	 * The Regular Expression that most closely matches (See {@link #isRegExpComplete()}) this Semantic Type.
	 * Note: All valid matches will match this RE, but the inverse is not necessarily true.
	 * @return The Java Regular Expression that most closely matches this Semantic Type.
	 */
	public abstract String getRegExp();

	/**
	 * Is the returned Regular Expression a true and complete representation of the Semantic Type.
	 * For example, \\d{5} is not for US ZIP codes (e.g. 00000 is not a valid Zip), whereas (?i)(male|female) could be valid for a Gender.
	 * @return The Java Regular Expression that most closely matches this Semantic Type.
	 */
	public boolean isRegExpComplete() {
		return pluginLocaleEntry.isRegExpComplete(-1);
	}

	/**
	 * The percentage when we declare success 0 - 100.
	 * We use this percentage in the determination of the Semantic Type.  When and how it is used varies based on the plugin.
	 * @return The threshold percentage.
	 */
	public int getThreshold() {
		return threshold;
	}

	/**
	 * The percentage when we declare success 0 - 100.
	 * We use this percentage in the determination of the Semantic Type.  When and how it is used varies based on the plugin.
	 * @param threshold the new threshold.
	 */
	public void setThreshold(final int threshold) {
		this.threshold = threshold;
	}

	/**
	 * Confidence in the type classification.
	 * Typically this will be the number of matches divided by the number of real samples.
	 * @param matchCount Number of matches (as determined by isValid())
	 * @param realSamples Number of samples observed - does not include either nulls or blanks
	 * @param context Context we are operating under (includes data stream name(s))
	 * @return Confidence as a percentage.
	 */
	public double getConfidence(final long matchCount, final long realSamples, final AnalyzerContext context) {
		return (double)matchCount/realSamples;
	}

	/**
	 * The underlying type we are qualifying.
	 * @return The underlying type - e.g. STRING, LONG, etc.
	 */
	public FTAType getBaseType() {
		return defn.baseType;
	}

	/**
	 * Indicate if this Semantic Type will accept the supplied Base Type, defaults to the Base Type from the plugin definition.
	 * @param type The underlying Base Type
	 * @return A boolean indicating if this Semantic Type will accept the supplied Base Type
	 */
	public boolean acceptsBaseType(final FTAType type) {
		return type == getBaseType();
	}

	/**
	 * A SHA-1 hash that reflects the data stream structure.
	 * @return A String SHA-1 hash that reflects the structure of the data stream.
	 */
	public String getSignature() {
		String structureSignature = getSignatureBaseType() + ":";

		structureSignature += getSemanticType();

		final MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}

		final byte[] signature = structureSignature.getBytes(StandardCharsets.UTF_8);
		return Base64.getEncoder().encodeToString(md.digest(signature));
	}

	private FTAType getSignatureBaseType() {
		return defn.baseType;
	}

	/**
	 * Is the supplied String an instance of this Semantic type?
	 * Note: this invokes {@link #isValid(String, boolean, long)} with false so using validate mode not detect mode.
	 * @param input String to check (trimmed for Numeric base Types, un-trimmed for String base Type)
	 * @return true iff the supplied String is an instance of this Semantic type.
	 */
	public boolean isValid(final String input) {
		return isValid(input, false, 0);
	}

	/**
	 * Is the supplied String an instance of this Semantic type?
	 * @param input String to check (trimmed for Numeric base Types, un-trimmed for String base Type)
	 * @param detectMode If true then we are in the process of detection, otherwise it is a simple validity check.
	 * @param count The number of instance of this sample.
	 * @return true iff the supplied String is an instance of this Semantic type.
	 */
	public abstract boolean isValid(final String input, final boolean detectMode, final long count);

	/**
	 * Given the data to date as embodied by the arguments return an analysis. If we think this is an instance
	 * of this Semantic type then valid will be true, if invalid then valid will be false and a new Pattern will be returned.
	 *
	 * @param context The context used to interpret the Data Stream (for example, stream name, date resolution mode, etc)
	 * @param matchCount Number of samples that match so far (as determined by isValid()
	 * @param realSamples Number of real (i.e. non-blank and non-null) samples that we have processed so far.
	 * @param currentRegExp The current Regular Expression that we matched against
	 * @param facts Facts (min, max, sum) for the analysis to date (optional - i.e. maybe null)
	 * @param cardinality Cardinality set, up to the maximum maintained
	 * @param outliers Outlier set, up to the maximum maintained
	 * @param tokenStreams Shapes observed
	 * @param analysisConfig The Configuration of the current analysis
	 * @return Null if we think this is an instance of this Semantic type (backout pattern otherwise)
	 */
	public abstract PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp, final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams, final AnalysisConfig analysisConfig);

	/**
	 * Does the set of members enumerated reflect the entire set.  For example any of the ISO sets are reference sets and
	 * hence complete, compared to FirstName and LastName where the set provided is of the common names.
	 * If isClosed() is true then isValid() false does not imply that the input is not valid just that it is not in the set of
	 * 'known' members.
	 * @return A boolean indicating if the set is closed.
	 */
	public abstract boolean isClosed();

	/**
	 * Accessor for the Plugin Definition for this Semantic Type.
	 * @return The Plugin Definition.
	 */
	public PluginDefinition getPluginDefinition() {
		return defn;
	}
}
