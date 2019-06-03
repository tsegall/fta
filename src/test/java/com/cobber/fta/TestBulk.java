package com.cobber.fta;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.cobber.fta.plugins.LogicalTypeGenderEN;

public class TestBulk {
	@Test
	public void basicBulk() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		HashMap<String, Long> basic = new HashMap<>();
		basic.put("Male", 2000000L);
		basic.put("Female", 1000000L);
		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), 3000000);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(),  LogicalTypeGenderEN.SEMANTIC_TYPE);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "(?i)(FEMALE|MALE)");
		Assert.assertEquals(result.getMatchCount(), 3000000);
		Assert.assertEquals(result.getConfidence(), 1.0);
		final Map<String, Long> details = result.getCardinalityDetails();
		Assert.assertEquals(details.get("MALE"), Long.valueOf(2000000));
		Assert.assertEquals(details.get("FEMALE"), Long.valueOf(1000000));
	}

	@Test
	public void justBlanks() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		HashMap<String, Long> basic = new HashMap<>();
		basic.put("", 1000000L);
		analysis.trainBulk(basic);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), 1000000);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "BLANK");
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getBlankCount(), 1000000);
		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*");
		Assert.assertEquals(result.getMatchCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getCardinality(), 0);
	}
}
