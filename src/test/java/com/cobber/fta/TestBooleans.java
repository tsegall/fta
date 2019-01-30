package com.cobber.fta;

import java.io.IOException;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestBooleans {
	@Test
	public void onlyTrue() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		analysis.train("true");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), 1);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "(?i)(true|false)");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.BOOLEAN);
		Assert.assertEquals(result.getMinLength(), 4);
		Assert.assertEquals(result.getMaxLength(), 4);
		Assert.assertEquals(result.getMinValue(), "true");
		Assert.assertEquals(result.getMaxValue(), "true");
		Assert.assertTrue("true".matches(result.getRegExp()));
	}

	@Test
	public void basicBoolean() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "false|true|TRUE|    false   |FALSE |TRUE|true|false|False|True|false|  FALSE|FALSE|true|TRUE|bogus".split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, -1);
		Assert.assertEquals(result.getSampleCount(), inputs.length + 2);
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*((?i)(true|false))\\p{javaWhitespace}*");
		Assert.assertEquals(result.getConfidence(), .9375);
		Assert.assertEquals(result.getType(), PatternInfo.Type.BOOLEAN);
		Assert.assertEquals(result.getTypeQualifier(), "TRUE_FALSE");
		Assert.assertEquals(result.getMinLength(), 4);
		Assert.assertEquals(result.getMaxLength(), 12);
		Assert.assertEquals(result.getMinValue(), "false");
		Assert.assertEquals(result.getMaxValue(), "true");
		Assert.assertTrue(inputs[0].matches(result.getRegExp()));

		int matches = 0;
		for (int i = 0; i < inputs.length; i++) {
			if (inputs[i].matches(result.getRegExp()))
				matches++;
		}
		Assert.assertEquals(result.getMatchCount(), matches);
	}

	@Test
	public void basicBooleanYN() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "no|yes|YES|    no   |NO |YES|yes|no|No|Yes|no|  NO|NO|yes|YES|bogus".split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, -1);
		Assert.assertEquals(result.getSampleCount(), inputs.length + 2);
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*((?i)(yes|no))\\p{javaWhitespace}*");
		Assert.assertEquals(result.getConfidence(), .9375);
		Assert.assertEquals(result.getType(), PatternInfo.Type.BOOLEAN);
		Assert.assertEquals(result.getTypeQualifier(), "YES_NO");
		Assert.assertEquals(result.getMinLength(), 2);
		Assert.assertEquals(result.getMaxLength(), 9);
		Assert.assertEquals(result.getMinValue(), "no");
		Assert.assertEquals(result.getMaxValue(), "yes");
		Assert.assertTrue(inputs[0].matches(result.getRegExp()));

		int matches = 0;
		for (int i = 0; i < inputs.length; i++) {
			if (inputs[i].trim().matches(result.getRegExp()))
					matches++;
		}
		Assert.assertEquals(result.getMatchCount(), matches);
	}

	@Test
	public void basicPseudoBoolean() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "0|1|1|0|0|1|1|0|0|1|0|0|0|1|1|0|1|1|1|1|0|0|0|0|1|1|1".split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), inputs.length + 2);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getRegExp(), "[0|1]");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.BOOLEAN);
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxLength(), 1);
		Assert.assertEquals(result.getMinValue(), "0");
		Assert.assertEquals(result.getMaxValue(), "1");
		Assert.assertTrue(inputs[0].matches(result.getRegExp()));

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void notPseudoBoolean() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "7|1|1|7|7|1|1|7|7|1|7|7|7|1|1|7|1|1|1|1|7|7|7|7|1|1|1".split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), inputs.length + 2);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getRegExp(), "\\d{1}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxLength(), 1);
		Assert.assertEquals(result.getMinValue(), "1");
		Assert.assertEquals(result.getMaxValue(), "7");
		Assert.assertTrue(inputs[0].matches(result.getRegExp()));

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicNotPseudoBoolean() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "0|5|5|0|0|5|5|0|0|5|0|0|0|5|5|0|5|5|5|5|0|0|0|0|5|5|5|A".split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), inputs.length + 2);
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getRegExp(), "\\d{1}");
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/(result.getSampleCount() - result.getNullCount()));
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getCardinality(), 2);
		final Map<String, Integer> details = result.getCardinalityDetails();
		Assert.assertEquals(details.get("0"), Integer.valueOf(13));
		Assert.assertEquals(details.get("5"), Integer.valueOf(14));
		Assert.assertEquals(result.dump(true), "TextAnalysisResult [name=anonymous, matchCount=27, sampleCount=30, nullCount=2, blankCount=0, regexp=\"\\d{1}\", confidence=0.9642857142857143, type=Long, min=\"0\", max=\"5\", minLength=1, maxLength=1, sum=\"70\", cardinality=2 {\"5\":14 \"0\":13 }, outliers=1 {\"A\":1 }]");
		Assert.assertEquals(result.dump(false), "TextAnalysisResult [name=anonymous, matchCount=27, sampleCount=30, nullCount=2, blankCount=0, regexp=\"\\d{1}\", confidence=0.9642857142857143, type=Long, min=\"0\", max=\"5\", minLength=1, maxLength=1, sum=\"70\", cardinality=2, outliers=1]");
		Assert.assertTrue(inputs[0].matches(result.getRegExp()));

		int matches = 0;
		for (int i = 0; i < inputs.length; i++) {
			if (inputs[i].matches(result.getRegExp()))
					matches++;
		}
		Assert.assertEquals(result.getMatchCount(), matches);
	}


}
