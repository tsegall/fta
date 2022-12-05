package com.cobber.fta;

import java.util.HashMap;
import java.util.Map;

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

		for (int i = 0; i < streamCount; i++) {
			analyzers[i] = new TextAnalyzer(getStreamContext(template.getContext(), i));
			analyzers[i].setConfig(template.getConfig());
		}
	}

	private AnalyzerContext getStreamContext(final AnalyzerContext templateContext, final int streamIndex) {
		final String fieldName = templateContext.getCompositeStreamNames()[streamIndex];
		return  new AnalyzerContext(fieldName == null ? "" : fieldName.trim(), templateContext.getDateResolutionMode(), templateContext.getCompositeName(), templateContext.getCompositeStreamNames());
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
		for (int i = 0; i < rawInput.length; i++) {
			boolean trained = analyzers[i].train(rawInput[i]);
			if (!trained)
				allTrained = false;
		}

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

		// Build an array of the Semantic Types detected as a result of the analysis so far
		final String[] semanticTypes = new String[streamCount];
		for (int i = 0; i < streamCount; i++) {
			ret[i] = analyzers[i].getResult();
			semanticTypes[i] = ret[i].isSemanticType() ? ret[i].getSemanticType() : null;
		}

		// For any stream where we have not already determined a Semantic type - try again providing the overall Semantic Type context
		for (int i = 0; i < streamCount; i++) {
			if (!ret[i].isSemanticType()) {
				// Update the Context with all the Semantic Type information we have calculated
				analyzers[i].setContext(analyzers[i].getContext().withSemanticTypes(semanticTypes));
				ret[i] = reAnalyze(analyzers[i], ret[i]);
			}
		}

		return ret;
	}

	private TextAnalysisResult reAnalyze(final TextAnalyzer analyzer, final TextAnalysisResult result) throws FTAPluginException, FTAUnsupportedLocaleException {
		final Map<String, Long> details = new HashMap<>(result.getCardinalityDetails());
		details.put(null, result.getNullCount());

		final TextAnalyzer analysisBulk = new TextAnalyzer(analyzer.getContext());
		analysisBulk.setExternalFacts(analyzer.getFacts().external);
		analysisBulk.setConfig(analyzer.getConfig());

		analysisBulk.trainBulk(details);
		TextAnalysisResult newResult = analysisBulk.getResult();
		return newResult.isSemanticType() ? newResult : result;
	}


	public TextAnalyzer getAnalyzer(final int stream) {
		return analyzers[stream];
	}

	public TextAnalyzer[] getAnalyzers() {
		return analyzers;
	}
}
