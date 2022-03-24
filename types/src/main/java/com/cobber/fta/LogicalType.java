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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.token.TokenStreams;

/**
 * All Logical Types are derived from this abstract class.
 * This LTRandom interface provides a {@link LTRandom#nextRandom} which will create a new valid example of the
 *  Semantic Type.
 */
public abstract class LogicalType implements Comparable<LogicalType>, LTRandom {
	protected PluginDefinition defn;
	protected Locale locale;
	protected int priority;
	protected int threshold;
	protected Pattern[] headerPatterns;

	@Override
	public int compareTo(final LogicalType other) {
	  return Integer.compare(priority, other.priority);
	}

	/**
	 * LogicalType constructor.
	 * @param plugin The definition of this plugin.
	 */
	public LogicalType(final PluginDefinition plugin) {
		this.defn = plugin;
		this.priority = plugin.priority;
		this.threshold = plugin.threshold;
	}

	/**
	 * Called to perform any initialization.
	 * @param locale The locale used for this analysis
	 * @return True if initialization was successful.
	 * @throws FTAPluginException Thrown when the plugin is incorrectly configured.
	 */
	public boolean initialize(final Locale locale) throws FTAPluginException {
		if (getBaseType() == null)
			throw new FTAPluginException("baseType cannot be null");

		this.locale = locale;

		if (defn != null && defn.headerRegExps != null) {
			headerPatterns = new Pattern[defn.headerRegExps.length];
			for (int i = 0; i <  defn.headerRegExps.length; i++)
				headerPatterns[i] = Pattern.compile(defn.headerRegExps[i]);
		}

		return true;
	}

	/**
	 * Determine the confidence that the name of the data stream is likely a valid header for this Semantic Type.
	 * @param dataStreamName The name of this data stream
	 * @return An integer between 0 and 100 reflecting the confidence that this stream name is a valid header.
	 */
	public int getHeaderConfidence(final String dataStreamName) {
		if (headerPatterns != null)
			for (int i = 0; i < headerPatterns.length; i++) {
				if (headerPatterns[i].matcher(dataStreamName).matches())
					return defn.headerRegExpConfidence[i];
			}

		return 0;
	}

	/**
	 *  The user-friendly name of the Qualifier.  For example, EMAIL for an email address
	 *  @return The user-friendly name of the type-qualifier.
	 */
	public abstract String getQualifier();

	/**
	 *  The user-friendly description of the Qualifier.  For example, 'Australian State' for the qualifier "STATE_PROVINCE.STATE_AU".
	 *  @return The user-friendly description of the type-qualifier.
	 */
	public String getDescription() {
		return defn.description;
	}

	/**
	 *  The relative priority of this plugin.
	 *  @return The relative priority of this plugin.
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 *  Is this plugin sensitive to the input locale.
	 *  @return True if the plugin is sensitive to the input locale.
	 */
	public boolean isLocaleSensitive() {
		return defn.localeSensitive;
	}

	/**
	 * The Regular Expression that most closely matches (See {@link #isRegExpComplete()}) this Logical Type.
	 * Note: All valid matches will match this RE, but the inverse is not necessarily true.
	 * @return The Java Regular Expression that most closely matches this Logical Type.
	 */
	public abstract String getRegExp();

	/**
	 * Is the returned Regular Expression a true and complete representation of the Logical Type.
	 * For example, \\d{5} is not for US ZIP codes (e.g. 00000 is not a valid Zip), whereas (?i)(male|female) could be valid for a Gender.
	 * @return The Java Regular Expression that most closely matches this Logical Type.
	 */
	public boolean isRegExpComplete() {
		return defn.isRegExpComplete;
	}

	/**
	 * The percentage when we declare success 0 - 100.
	 * We use this percentage in the determination of the Logical Type.  When and how it is used varies based on the plugin.
	 * @return The threshold percentage.
	 */
	public int getThreshold() {
		return threshold;
	}

	/**
	 * The percentage when we declare success 0 - 100.
	 * We use this percentage in the determination of the Logical Type.  When and how it is used varies based on the plugin.
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
	 * @param dataStreamName Name of the Data Stream
	 * @return Confidence as a percentage.
	 */
	public double getConfidence(final long matchCount, final long realSamples, final String dataStreamName) {
		return (double)matchCount/realSamples;
	}

	/**
	 * The underlying type we are qualifying.
	 * @return The underlying type - e.g. STRING, LONG, etc.
	 */
	public abstract FTAType getBaseType();

	public boolean acceptsBaseType(final FTAType type) {
		return type == getBaseType();
	}

	/**
	 * A SHA-1 hash that reflects the data stream structure.
	 * @return A String SHA-1 hash that reflects the structure of the data stream.
	 */
	public String getSignature() {
		String structureSignature = getBaseType() + ":";

		structureSignature += getQualifier();

		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}

		final byte[] signature = structureSignature.getBytes(StandardCharsets.UTF_8);
		return Base64.getEncoder().encodeToString(md.digest(signature));
	}

	/**
	 * Is the supplied String an instance of this logical type?
	 * @param input String to check (trimmed for Numeric base Types, un-trimmed for String base Type)
	 * @return true iff the supplied String is an instance of this Logical type.
	 */
	public abstract boolean isValid(final String input);

	/**
	 * Given the data to date as embodied by the arguments return an analysis. If we think this is an instance
	 * of this logical type then valid will be true , if invalid then valid will be false and a new Pattern will be returned.
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
	 * @return Null if we think this is an instance of this logical type (backout pattern otherwise)
	 */
	public abstract PluginAnalysis analyzeSet(AnalyzerContext context, long matchCount, long realSamples, String currentRegExp, Facts facts, Map<String, Long> cardinality, Map<String, Long> outliers, TokenStreams tokenStreams, AnalysisConfig analysisConfig);

	/**
	 * Does the set of members enumerated reflect the entire set.  For example any of the ISO sets are reference sets and
	 * hence complete, compared to FirstName and LastName where the set provided is of the common names.
	 * If isClosed() is true then isValid() false does not imply that the input is not valid just that it is not in the set of
	 * 'known' members.
	 * @return A boolean indicating if the set is closed.
	 */
	public abstract boolean isClosed();

	/**
	 * Accessor for the Plugin Definition for this Logical Type.
	 * @return The Plugin Definition.
	 */
	public PluginDefinition getPluginDefinition() {
		return defn;
	}
}
