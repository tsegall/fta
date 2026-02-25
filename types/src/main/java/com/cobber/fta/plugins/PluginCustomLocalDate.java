package com.cobber.fta.plugins;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.Facts;
import com.cobber.fta.FiniteMap;
import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PluginAnalysis;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.InternalErrorException;
import com.cobber.fta.dates.DateTimeParser;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;
import com.cobber.fta.dates.DateTimeParserConfig;
import com.cobber.fta.dates.DateTimeParserResult;
import com.cobber.fta.token.TokenStreams;

public class PluginCustomLocalDate extends LogicalTypeInfinite {
	Pattern rePattern;
	Pattern invalidPattern;
	private String regExp;
	private String dateFormat;
	private String minimum;
	private LocalDate minimumDate;
	private DateTimeFormatter dateTimeFormatter;

	/**
	 * Construct a plugin based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public PluginCustomLocalDate(final PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(final AnalysisConfig analysisConfig) throws FTAPluginException {
		super.initialize(analysisConfig);

		if (defn.getOptions() == null)
			throw new FTAPluginException("Misconfigured plugin - need pluginOptions including format, and invalid");

		dateFormat = (String) defn.getOptions().get("dateFormat");
		regExp = DateTimeParserResult.asResult(dateFormat, DateResolutionMode.None, new DateTimeParserConfig()).getRegExp();
		rePattern = Pattern.compile(regExp);

		// Grab the set of invalid regular expressions if they exist
		final Object invalidRegExp = defn.getOptions().get("invalidRegExp");
		if (invalidRegExp != null)
			invalidPattern = Pattern.compile((String) defn.getOptions().get("invalidRegExp"));

		// Grab the minimum value if it exists
		final Object minimumObject = defn.getOptions().get("minimum");
		if (minimumObject != null)
			minimum = (String) minimumObject;
		if (dateFormat != null) {
			final DateTimeParser dateTimeParser = new DateTimeParser()
					.withDateResolutionMode(DateResolutionMode.Auto)
					.withLocale(locale)
					.withNumericMode(false)
					.withNoAbbreviationPunctuation(analysisConfig.isEnabled(TextAnalyzer.Feature.NO_ABBREVIATION_PUNCTUATION))
					.withEnglishAMPM(analysisConfig.isEnabled(TextAnalyzer.Feature.ALLOW_ENGLISH_AMPM));

			final DateTimeParserResult result = DateTimeParserResult.asResult(dateFormat, DateResolutionMode.Auto, new DateTimeParserConfig());
			if (result == null)
				throw new InternalErrorException("NULL result for " + dateFormat);

			dateTimeFormatter = dateTimeParser.ofPattern(result.getFormatString());
		}

		if (minimum != null && dateFormat != null)
			minimumDate = LocalDate.parse(minimum, dateTimeFormatter);

		return true;
	}

	@Override
	public String nextRandom() {
		return LocalDate.now().minusDays(getRandom().nextInt(3000)).format(dateTimeFormatter);
	}

	@Override
	public boolean isRegExpComplete() {
		return false;
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode, final long count) {
		return validate(input.trim());
	}

	private boolean validate(final String trimmed) {
		if (!rePattern.matcher(trimmed).matches() || (invalidPattern != null && invalidPattern.matcher(trimmed).matches()))
			return false;

		if (minimumDate != null && minimumDate.compareTo(LocalDate.parse(trimmed, dateTimeFormatter)) > 0)
			return false;

		return true;
	}

	@Override
	public boolean isCandidate(final String trimmed, final StringBuilder compressed, final int[] charCounts, final int[] lastIndex) {
		return validate(trimmed);
	}

	@Override
	public String getRegExp() {
		return regExp;
	}

	@Override
	public PluginAnalysis analyzeSet(final AnalyzerContext context, final long matchCount, final long realSamples, final String currentRegExp,
			final Facts facts, final FiniteMap cardinality, final FiniteMap outliers, final TokenStreams tokenStreams,
			final AnalysisConfig analysisConfig) {
		return dateFormat.equals(facts.matchTypeInfo.typeModifier) && getConfidence(matchCount, realSamples, context) >= getThreshold() / 100.0 ?  PluginAnalysis.OK : PluginAnalysis.SIMPLE_NOT_OK;
	}
}
