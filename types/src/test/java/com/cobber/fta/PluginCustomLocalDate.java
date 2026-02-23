package com.cobber.fta;

import java.time.LocalDate;
import java.util.regex.Pattern;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.token.TokenStreams;

public class PluginCustomLocalDate extends LogicalTypeInfinite {
	Pattern rePattern;
	Pattern invalidPattern;
	private String regExp;
	private String dateFormat;

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

		rePattern = Pattern.compile((String) defn.getOptions().get("regExp"));
		invalidPattern = Pattern.compile((String) defn.getOptions().get("invalid"));
		dateFormat = (String) defn.getOptions().get("dateFormat");

		return true;
	}

	@Override
	public String nextRandom() {
		return String.valueOf(LocalDate.now().getYear() - getRandom().nextInt(99) + 1);
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
		return rePattern.matcher(trimmed).matches() && !invalidPattern.matcher(trimmed).matches();
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
