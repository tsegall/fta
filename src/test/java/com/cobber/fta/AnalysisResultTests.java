package com.cobber.fta;

import java.util.Random;

import org.testng.Assert;
import org.testng.annotations.Test;

public class AnalysisResultTests {
	@Test
	public void inadequateData() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String[] inputs = "47|89|90|91".split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\a{2}");
		Assert.assertEquals(result.getConfidence(), 0.0);
		Assert.assertEquals(result.getType(), "String");
		Assert.assertEquals(result.getMin(), null);
		Assert.assertEquals(result.getMax(), null);
	}

	@Test
	public void variableLengthPositiveInteger() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String[] inputs = "47|909|809821|34590|2|0|12|390|4083|4499045|90|9003|8972|42987|8901".split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{1,7}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "Long");
		Assert.assertEquals(result.getMin(), "0");
		Assert.assertEquals(result.getMax(), "4499045");
	}

	@Test
	public void variableLengthInteger() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String[] inputs = "-10000|-1000|-100|-10|-3|-2|-1|0|1|2|3|10|100|1000|10000|1|2|3|4|5|6|7|8|9|10|11|12|13|14|15".split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "[-]\\d{+}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "Long");
		Assert.assertEquals(result.getTypeQualifier(), "Signed");
		Assert.assertEquals(result.getMin(), "-10000");
		Assert.assertEquals(result.getMax(), "10000");
	}

	@Test
	public void constantLengthInteger() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();
		String[] inputs = "456789|456089|456700|116789|433339|409187".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, -1);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{6}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "Long");
		Assert.assertEquals(result.getMin(), "116789");
		Assert.assertEquals(result.getMax(), "456789");
	}

	@Test
	public void positiveDouble() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String[] inputs = "43.80|1.1|0.1|2.03|.1|99.23|14.08976|14.085576|3.141592654|2.7818|1.414|2.713".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, -1);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{*}D\\d{+}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "Double");
		Assert.assertEquals(result.getMin(), "0.1");
		Assert.assertEquals(result.getMax(), "99.23");
	}

	@Test
	public void positiveDouble2() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String[] inputs = "43.80|1.1|0.1|2.03|0.1|99.23|14.08976|14.085576|3.141592654|2.7818|1.414|2.713".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, -1);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{*}D\\d{+}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "Double");
		Assert.assertEquals(result.getMin(), "0.1");
		Assert.assertEquals(result.getMax(), "99.23");
	}

	@Test
	public void negativeDouble() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String[] inputs = "43.80|-1.1|-.1|2.03|.1|-99.23|14.08976|-14.085576|3.141592654|2.7818|1.414|2.713".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, -1);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "[-]\\d{*}D\\d{+}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "Double");
		Assert.assertEquals(result.getTypeQualifier(), "Signed");
		Assert.assertEquals(result.getMin(), "-99.23");
		Assert.assertEquals(result.getMax(), "43.8");
	}
	//			"\\d{4}/\\d{2}/\\d{2}".equals(pattern) ||
	//			"\\d{2}-\\d{2}-\\d{4}".equals(pattern) ||
	//			"\\d{4}/\\d{2}/\\d{4}".equals(pattern))

	@Test
	public void basicDateYYYY_MM_DD() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String[] inputs = "2010-01-22|2019-01-12|1996-01-02|1916-01-02|1993-01-02|1998-01-02|2001-01-02|2000-01-14|2008-01-12".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{4}-\\d{2}-\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "Date");
	}

	@Test
	public void basicDateDD_MMMM_YYY() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String[] inputs = "22 Jan 1971|12 Mar 2019|02 Jun 1996|11 Dec 1916|19 Apr 1993|26 Sep 1998|09 Dec 1959|14 Jul 2000|18 Aug 2008".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{2} \\a{3} \\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "Date");
	}


	//	put(Pattern.compile("^(?i)\\d{1,2}\\s[a-z]{3}\\s\\d{4}$").matcher(""), "dd MMM yyyy");
	//	put(Pattern.compile("^(?i)\\d{1,2}\\s[a-z]{4,}\\s\\d{4}$").matcher(""), "dd MMMM yyyy");

	@Test
	public void slashDateYYYY_MM_DD() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String[] inputs = "2010/01/22|2019/01/12|1996/01/02|1916/01/02|1993/01/02|1998/01/02|2001/01/02|2000/01/14|2008/01/12".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{4}/\\d{2}/\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "Date");
	}

	@Test
	public void basicDateDD_MM_YYYY() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String[] inputs = "22-01-2010|12-01-2019|02-01-1996|02-01-1916|02-01-1993|02-01-1998|02-01-2001|14-01-2000|12-01-2008".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{2}-\\d{2}-\\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "Date");
	}

	@Test
	public void variableDateDD_MM_YYYY() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String[] inputs = "22-1-2010|12-1-2019|2-1-1996|2-1-1916|2-1-1993|2-1-1998|22-11-2001|14-1-2000|12-5-2008".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{1,2}-\\d{1,2}-\\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "Date");
	}

	@Test
	public void slashDateDD_MM_YYYY() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String[] inputs = "22/01/2010|12/01/2019|02/01/1996|02/01/1916|02/01/1993|02/01/1998|02/01/2001|14/01/2000|12/01/2008".split("\\|");
		int locked = -1;
		int iterations = 4;

		for (int iters = 0; iters < iterations; iters++) {
			for (int i = 0; i < inputs.length; i++) {
				if (analysis.train(inputs[i]) && locked == -1)
					locked = i;
			}
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length * iterations);
		Assert.assertEquals(result.getMatchCount(), inputs.length * iterations);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{2}/\\d{2}/\\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "Date");
	}

	@Test
	public void basicTimeHH_MM_SS() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String[] inputs = "00:10:00|00:10:00|23:07:00|06:07:00|16:07:00|06:37:00|06:07:00|06:09:00|06:20:00|06:57:00".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{2}:\\d{2}:\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "Time");
	}

	@Test
	public void basicTimeHH_MM() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String[] inputs = "00:10|00:10|23:07|06:07|16:07|06:37|06:07|06:09|06:20|06:57".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{2}:\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "Time");
	}

	@Test
	public void basicBoolean() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String[] inputs = "false|true|TRUE|    false   |FALSE |TRUE|true|false|False|True|false".split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, -1);
		Assert.assertEquals(result.getSampleCount(), inputs.length + 2);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getPattern(), "(?i)true|false");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "Boolean");
	}

	@Test
	public void basicText() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();
		int locked = -1;
		int iterations = 1000;

		for (int i = 0; i < iterations; i++) {
			if (analysis.train("primary") && locked == -1)
				locked = i;
			if (analysis.train("secondary") && locked == -1)
				locked = i;
			if (analysis.train("tertiary") && locked == -1)
				locked = i;
			if (analysis.train("fictional") && locked == -1)
				locked = i;
			if (analysis.train(null) && locked == -1)
				locked = i;
		}
		analysis.train("secondory");

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT/4);
		Assert.assertEquals(result.getSampleCount(), 5 * iterations + 1);
		Assert.assertEquals(result.getNullCount(), iterations);
		Assert.assertEquals(result.getCardinality(), 5);
		Assert.assertEquals(result.getPattern(), "\\a{7,9}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void manyRandomInts() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();
		Random random = new Random();
		int locked = -1;
		int nullIterations = 50;
		int iterations = 10000;
				

		for (int i = 0; i < nullIterations; i++) {
			analysis.train(null);
		}
		for (int i = 0; i < iterations; i++) {
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), iterations + nullIterations);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), nullIterations);
		Assert.assertEquals(result.getType(), "Long");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void manyKnownInts() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();
		int locked = -1;
		int nullIterations = 50;
		int iterations = 100000;

		for (int i = 0; i < nullIterations; i++) {
			analysis.train(null);
		}
		for (int i = 0; i < iterations; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, 20);
		Assert.assertEquals(result.getSampleCount(), iterations + nullIterations);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), nullIterations);
		Assert.assertEquals(result.getPattern(), "\\d{1,5}");
		Assert.assertEquals(result.getType(), "Long");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getMin(), "0");
		Assert.assertEquals(result.getMax(), String.valueOf(iterations - 1));
	}

	@Test
	public void DateTimeTime() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		analysis.train("2004-01-01T00:00:00-0500");
		analysis.train("2004-01-01T02:00:00-0500");
		analysis.train("2006-01-01T00:00:00-0500");
		analysis.train("2004-01-01T02:00:00-0500");
		analysis.train("2006-01-01T13:00:00-0500");
		analysis.train("2004-01-01T00:00:00-0500");
		analysis.train("2006-01-01T13:00:00-0500");
		analysis.train("2006-01-01T00:00:00-0500");
		analysis.train("2004-01-01T00:00:00-0500");
		analysis.train("2004-01-01T00:00:00-0500");
		analysis.train("2004-01-01T00:00:00-0500");
		analysis.train("2004-01-01T00:00:00-0500");
		analysis.train("2004-01-01T00:00:00-0500");
		analysis.train("2008-01-01T13:00:00-0500");
		analysis.train("2008-01-01T13:00:00-0500");
		analysis.train("2010-01-01T00:00:00-0500");
		analysis.train("2004-01-01T02:00:00-0500");
		analysis.train(null);
		analysis.train("2008-01-01T00:00:00-0500");
		analysis.train(null);

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), 20);
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getPattern(), "\\d{4}-\\d{2}-\\d{2}\\a{1}\\d{2}:\\d{2}:\\d{2}-\\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "DateTime");
	}
}
