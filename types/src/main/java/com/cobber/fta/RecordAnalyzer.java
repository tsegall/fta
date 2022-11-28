package com.cobber.fta;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAUnsupportedLocaleException;

public class RecordAnalyzer {
	private TextAnalyzer[] analyzers = null;
	private int streamCount = -1;

	/**
	 * Construct a Record Analyzer using the supplied template.
	 *
	 * @param template The TextAnalyzer to be used as a template.
	 */
	public RecordAnalyzer(final TextAnalyzer template) {
		streamCount = template.getContext().getCompositeStreamNames().length;
		analyzers = new TextAnalyzer[streamCount];
		AnalyzerContext templateContext = template.getContext();

		for (int i = 0; i < streamCount; i++) {
			final AnalyzerContext context = new AnalyzerContext(templateContext.getCompositeStreamNames()[i], templateContext.getDateResolutionMode(), templateContext.getCompositeName(), templateContext.getCompositeStreamNames());
			analyzers[i] = new TextAnalyzer(context);
		}
	}

	/**
	 * Train is the streaming entry point used to supply input to the Record Analyzer.
	 *
	 * @param rawInput
	 *            The raw input as a String array
	 * @return A boolean indicating if the resultant type is currently known for all Analyzers.
	 * @throws FTAPluginException Thrown when a registered plugin has detected an issue
	 * @throws FTAUnsupportedLocaleException Thrown when a requested locale is not supported
	 */
	public boolean train(final String[] rawInput) throws FTAPluginException, FTAUnsupportedLocaleException {
		if (rawInput.length != streamCount)
			throw new IllegalArgumentException("Size of training input must match number of stream names");
		boolean allTrained = true;
		for (int i = 0; i < rawInput.length; i++)
			allTrained = allTrained && analyzers[i].train(rawInput[i]);

		return allTrained;
	}

	/**
	 * Determine the result of the training complete to date. Typically invoked
	 * after all training is complete, but may be invoked at any stage.
	 *
	 * @return A TextAnalysisResult[] with the analysis of any training completed.
	 * @throws FTAPluginException Thrown when a registered plugin has detected an issue
	 * @throws FTAUnsupportedLocaleException Thrown when a requested locale is not supported
	 */
	public TextAnalysisResult[] getResult() throws FTAPluginException, FTAUnsupportedLocaleException {
		TextAnalysisResult[] ret = new TextAnalysisResult[streamCount];

		for (int i = 0; i < streamCount; i++)
			ret[i] = analyzers[i].getResult();

		return ret;
	}
}
