/*
 * Copyright 2017 Tim Segall
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

import java.io.IOException;
import java.util.Map;
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
		Assert.assertEquals(result.getPattern(), "\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "Long");
		Assert.assertEquals(result.getMinValue(), "47");
		Assert.assertEquals(result.getMaxValue(), "91");
	}

	@Test
	public void noData() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "^[ ]*$");
		Assert.assertEquals(result.getConfidence(), 0.0);
		Assert.assertEquals(result.getType(), "[BLANK]");
		Assert.assertEquals(result.dump(true), "TextAnalysisResult [matchCount=0, sampleCount=0, nullCount=0, blankCount=0, pattern=\"^[ ]*$\", confidence=0.0, type=[BLANK], min=null, max=null, sum=null, cardinality=0]");
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
		Assert.assertEquals(result.getMinValue(), "0");
		Assert.assertEquals(result.getMaxValue(), "4499045");
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxLength(), 7);
	}

	@Test
	public void variableLengthString() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String[] inputs = "Hello World|Hello|H|Z|A".split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\a{1,11}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "String");
		Assert.assertEquals(result.getMinValue(), "A");
		Assert.assertEquals(result.getMaxValue(), "Z");
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxLength(), 11);
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
		Assert.assertEquals(result.getMinValue(), "-10000");
		Assert.assertEquals(result.getMaxValue(), "10000");
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
		Assert.assertEquals(result.getMinValue(), "116789");
		Assert.assertEquals(result.getMaxValue(), "456789");
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
		Assert.assertEquals(result.getMinValue(), "0.1");
		Assert.assertEquals(result.getMaxValue(), "99.23");
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
		Assert.assertEquals(result.getMinValue(), "0.1");
		Assert.assertEquals(result.getMaxValue(), "99.23");
	}

	@Test
	public void dateOutlier() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String[] inputs = "12/12/12|12/12/32|02/22/02".split("\\|");
		int locked = -1;
		int records = 100;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		for (int i = inputs.length; i < records; i++) {
			if (analysis.train("02/02/99") && locked == -1)
				locked = i;
		}

		analysis.train("02/O2/99");

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), records + 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{2}/\\d{2}/\\d{2}");
		Assert.assertEquals(result.getConfidence(), 0.9900990099009901);
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getType(), "Date");
		Assert.assertEquals(result.getTypeQualifier(), "MM/dd/yy");
		Assert.assertEquals(result.getMinValue(), "02/22/02");
		Assert.assertEquals(result.getMaxValue(), "02/02/99");
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
		Assert.assertEquals(result.getMinValue(), "-99.23");
		Assert.assertEquals(result.getMaxValue(), "43.8");
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
		Assert.assertEquals(result.getTypeQualifier(), "yyyy-MM-dd");
	}

	@Test
	public void basicDateYYYY() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String input = "2015|2015|2015|2015|2015|2015|2015|2016|2016|2016|2013|1932|1991|1993|2001|1977|2001|1976|1972|" +
				"1982|2005|1950|1961|1967|1997|1967|1996|2014|2002|1953|1980|2010|2010|1979|1980|1983|1974|1970|" +
				"1978|2014|2015|1979|1982|2016|2016|2013|2011|1986|1985|2000|2000|2012|2000|2000|";
		String[] inputs = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "Date");
		Assert.assertEquals(result.getTypeQualifier(), "yyyy");
	}

	@Test
	public void basicDateD_MMM_YY() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String input = "1-Jan-14|2-Jan-14|3-Jan-14|6-Jan-14|7-Jan-14|7-Jan-14|8-Jan-14|9-Jan-14|10-Jan-14|" +
				"13-Jan-14|14-Jan-14|15-Jan-14|16-Jan-14|17-Jan-14|20-Jan-14|21-Jan-14|22-Jan-14|" +
				"23-Jan-14|24-Jan-14|27-Jan-14|28-Jan-14|29-Jan-14|30-Jan-14|31-Jan-14|3-Feb-14|" +
				"4-Feb-14|5-Feb-14|6-Feb-14|7-Feb-14|10-Feb-14|11-Feb-14|12-Feb-14|13-Feb-14|14-Feb-14|" +
				"17-Feb-14|18-Feb-14|19-Feb-14|20-Feb-14|21-Feb-14|24-Feb-14|25-Feb-14|26-Feb-14|27-Feb-14|" +
				"28-Feb-14|3-Mar-14|4-Mar-14|5-Mar-14|";
		String[] inputs = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), "Date");
		Assert.assertEquals(result.getTypeQualifier(), "d-MMM-yy");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{1,2}-\\p{Alpha}{3}-\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void startsAsTwoDigitDay() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String input =
				"27/6/2012 12:46:03|27/6/2012 15:29:48|27/6/2012 23:32:22|27/6/2012 23:38:51|27/6/2012 23:42:22|" +
						"27/6/2012 23:49:13|27/6/2012 23:56:02|28/6/2012 08:04:51|28/6/2012 15:53:00|28/6/2012 16:46:34|" +
						"28/6/2012 17:01:01|28/6/2012 17:53:52|28/6/2012 18:03:31|28/6/2012 18:31:14|28/6/2012 18:46:12|" +
						"28/6/2012 23:32:08|28/6/2012 23:44:54|28/6/2012 23:47:48|28/6/2012 23:51:32|28/6/2012 23:53:36|" +
						"29/6/2012 08:54:18|29/6/2012 08:56:53|29/6/2012 11:21:56|29/6/2012 16:48:14|29/6/2012 16:56:32|" +
						"1/7/2012 09:15:03|1/7/2012 15:36:44|1/7/2012 18:25:35|1/7/2012 18:31:19|1/7/2012 18:36:04|" +
						"1/7/2012 19:13:17|1/7/2012 19:13:35|1/7/2012 19:13:49|1/7/2012 19:14:07|1/7/2012 19:14:21|" +
						"1/7/2012 19:14:29|1/7/2012 19:16:45|1/7/2012 19:17:48|1/7/2012 19:18:19|1/7/2012 19:19:09|";

		String[] inputs = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), "DateTime");
		Assert.assertEquals(result.getTypeQualifier(), "d/M/yyyy HH:mm:ss");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\a{+}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void startsAsTwoDigitMonth() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String input =
				"27/10/2012 12:46:03|27/10/2012 15:29:48|27/10/2012 23:32:22|27/10/2012 23:38:51|27/10/2012 23:42:22|" +
						"27/10/2012 23:49:13|27/10/2012 23:56:02|28/10/2012 08:04:51|28/10/2012 15:53:00|28/10/2012 16:46:34|" +
						"28/10/2012 17:01:01|28/10/2012 17:53:52|28/10/2012 18:03:31|28/10/2012 18:31:14|28/10/2012 18:46:12|" +
						"28/10/2012 23:32:08|28/10/2012 23:44:54|28/10/2012 23:47:48|28/10/2012 23:51:32|28/10/2012 23:53:36|" +
						"29/10/2012 08:54:18|29/10/2012 08:56:53|29/10/2012 11:21:56|29/10/2012 16:48:14|29/10/2012 16:56:32|" +
						"10/7/2012 09:15:03|1/7/2012 15:36:44|1/7/2012 18:25:35|1/7/2012 18:31:19|1/7/2012 18:36:04|" +
						"1/7/2012 19:13:17|1/7/2012 19:13:35|1/7/2012 19:13:49|1/7/2012 19:14:07|1/7/2012 19:14:21|" +
						"1/7/2012 19:14:29|1/7/2012 19:16:45|1/7/2012 19:17:48|1/7/2012 19:18:19|1/7/2012 19:19:09|";

		String[] inputs = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), "DateTime");
		Assert.assertEquals(result.getTypeQualifier(), "d/M/yyyy HH:mm:ss");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\a{+}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void basicDateDD_MMM_YYY_HH_MM() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String input =
				"1/30/06 22:01|1/30/06 22:15|1/30/06 22:25|1/30/06 22:35|1/30/06 22:40|1/30/06 22:45|1/30/06 22:47|1/30/06 23:00|1/30/06 23:00|1/30/06 23:11|" +
						"1/30/06 23:15|1/30/06 23:21|1/30/06 23:31|1/30/06 23:52|1/30/06 23:55|1/30/06 23:58|1/31/06 0:00|1/31/06 0:00|1/31/06 0:00|1/31/06 0:01|" +
						"1/31/06 0:01|1/31/06 0:01|1/31/06 0:01|1/31/06 0:01|1/31/06 0:01|1/31/06 0:01|1/31/06 0:01|1/31/06 0:17|1/31/06 0:26|1/31/06 0:30|" +
						"1/31/06 0:30|1/31/06 0:30|1/31/06 0:47|1/31/06 0:56|1/31/06 1:21|1/31/06 1:34|1/31/06 1:49|1/31/06 2:00|1/31/06 2:08|1/31/06 2:11|1/31/06 2:22|" +
						"1/31/06 2:48|1/31/06 3:05|1/31/06 3:05|1/31/06 3:30|";

		String[] inputs = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{1,2}/\\d{2}/\\d{2} \\d{1,2}:\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "DateTime");
		Assert.assertEquals(result.getTypeQualifier(), "M/dd/yy H:mm");
		Assert.assertEquals(result.getMinValue(), "1/30/06 22:01");
		Assert.assertEquals(result.getMaxValue(), "1/31/06 3:30");
	}

	@Test
	public void slashLoop() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer("thin", false);

		String input = "1/1/06 0:00";
		int locked = -1;
		int iterations = 30;

		for (int iters = 0; iters < iterations; iters++) {
			if (analysis.train(input) && locked == -1)
				locked = iters;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), "DateTime");
		Assert.assertEquals(result.getTypeQualifier(), "M/d/yy H:mm");
		Assert.assertEquals(result.getSampleCount(), iterations);
		Assert.assertEquals(result.getMatchCount(), iterations);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{1,2}/\\d{1,2}/\\d{2} \\d{1,2}:\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getMinValue(), "1/1/06 0:00");
		Assert.assertEquals(result.getMaxValue(), "1/1/06 0:00");
	}

	@Test
	public void basicDateDD_MMM_YYY() throws Exception {
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
		Assert.assertEquals(result.getPattern(), "\\d{2} \\p{Alpha}{3} \\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "Date");
		Assert.assertEquals(result.getTypeQualifier(), "dd MMM yyyy");
		Assert.assertEquals(result.getMinValue(), "11 Dec 1916");
		Assert.assertEquals(result.getMaxValue(), "12 Mar 2019");
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
		Assert.assertEquals(result.getTypeQualifier(), "yyyy/MM/dd");
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
		Assert.assertEquals(result.getTypeQualifier(), "dd-MM-yyyy");
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
		Assert.assertEquals(result.getTypeQualifier(), "d-M-yyyy");
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
		Assert.assertEquals(result.getTypeQualifier(), "dd/MM/yyyy");
	}

	@Test
	public void slashDateDD_MM_YY() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String[] inputs = "22/01/70|12/01/03|02/01/66|02/01/46|02/01/93|02/01/78|02/01/74|14/01/98|12/01/34".split("\\|");
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
		Assert.assertEquals(result.getPattern(), "\\d{2}/\\d{2}/\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "Date");
		Assert.assertEquals(result.getTypeQualifier(), "dd/MM/yy");
	}

	@Test
	public void slashDateAmbiguousMM_DD_YY() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer("thin", false);

		String input = " 04/03/13";
		int locked = -1;
		int iterations = 30;

		for (int iters = 0; iters < iterations; iters++) {
			if (analysis.train(input) && locked == -1)
				locked = iters;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), iterations);
		Assert.assertEquals(result.getMatchCount(), iterations);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{2}/\\d{2}/\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "Date");
		Assert.assertEquals(result.getTypeQualifier(), "MM/dd/yy");
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
		Assert.assertEquals(result.getTypeQualifier(), "HH:mm:ss");
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
		Assert.assertEquals(result.getTypeQualifier(), "HH:mm");
	}

	@Test
	public void limitedData() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String[] inputs = "12|4|5|".split("\\|");
		int pre = 3;
		int post = 10;

		for (int i = 0; i < pre; i++)
			analysis.train("");
		for (int i = 0; i < inputs.length; i++) {
			analysis.train(inputs[i]);
		}
		for (int i = 0; i < post; i++)
			analysis.train("");

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), pre + inputs.length + post);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{1,2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "Long");
	}

	@Test
	public void onlyTrue() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		analysis.train("true");

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), 1);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "(?i)true|false");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "Boolean");
		Assert.assertEquals(result.getMinLength(), 4);
		Assert.assertEquals(result.getMaxLength(), 4);
		Assert.assertEquals(result.getMinValue(), "true");
		Assert.assertEquals(result.getMaxValue(), "true");
	}



	@Test
	public void basicBoolean() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String[] inputs = "false|true|TRUE|    false   |FALSE |TRUE|true|false|False|True|false|  FALSE|FALSE|true|TRUE|bogus".split("\\|");
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
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getPattern(), "(?i)true|false");
		Assert.assertEquals(result.getConfidence(), .9375);
		Assert.assertEquals(result.getType(), "Boolean");
		Assert.assertEquals(result.getMinLength(), 4);
		Assert.assertEquals(result.getMaxLength(), 12);
		Assert.assertEquals(result.getMinValue(), "false");
		Assert.assertEquals(result.getMaxValue(), "true");
	}

	@Test
	public void basicPseudoBoolean() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String[] inputs = "0|1|1|0|0|1|1|0|0|1|0|0|0|1|1|0|1|1|1|1|0|0|0|0|1|1|1".split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), inputs.length + 2);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getPattern(), "[0|1]");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "Boolean");
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxLength(), 1);
		Assert.assertEquals(result.getMinValue(), "false");
		Assert.assertEquals(result.getMaxValue(), "true");
	}

	@Test
	public void basicNotPseudoBoolean() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String[] inputs = "0|5|5|0|0|5|5|0|0|5|0|0|0|5|5|0|5|5|5|5|0|0|0|0|5|5|5|A".split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), inputs.length + 2);
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getPattern(), "\\d{1}");
		Assert.assertEquals(result.getConfidence(), 0.9642857142857143);
		Assert.assertEquals(result.getType(), "Long");
		Assert.assertEquals(result.getCardinality(), 2);
		Map<String, Integer> details = result.getCardinalityDetails();
		Assert.assertEquals(details.get("0"), Integer.valueOf(13));
		Assert.assertEquals(details.get("5"), Integer.valueOf(14));
		Assert.assertEquals(result.dump(true), "TextAnalysisResult [matchCount=27, sampleCount=30, nullCount=2, blankCount=0, pattern=\"\\d{1}\", confidence=0.9642857142857143, type=Long, min=\"0\", max=\"5\", sum=\"70\", cardinality=2 {\"5\":14 \"0\":13 }, outliers=1 {\"A\":1 }]");
		Assert.assertEquals(result.dump(false), "TextAnalysisResult [matchCount=27, sampleCount=30, nullCount=2, blankCount=0, pattern=\"\\d{1}\", confidence=0.9642857142857143, type=Long, min=\"0\", max=\"5\", sum=\"70\", cardinality=2, outliers=1]");
	}

	@Test
	public void manyNulls() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		int iterations = 50;

		for (int i = 0; i < iterations; i++) {
			analysis.train(null);
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), iterations);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), iterations);
		Assert.assertEquals(result.getNullCount(), iterations);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getPattern(), "[NULL]");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "[NULL]");
	}

	@Test
	public void manyBlanks() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		int iterations = 50;

		for (int i = 0; i < iterations; i++) {
			analysis.train("");
			analysis.train(" ");
			analysis.train("  ");
			analysis.train("   ");
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), 4 * iterations);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), 4 * iterations);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getBlankCount(), 4 * iterations);
		Assert.assertEquals(result.getPattern(), "^[ ]*$");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), "[BLANK]");
	}

	@Test
	public void basicEmail() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String input = "Bachmann@lavastorm.com|Biedermann@lavastorm.com|buchheim@lavastorm.com|" +
				"coleman@lavastorm.com|Drici@lavastorm.com|Garvey@lavastorm.com|jackson@lavastorm.com|" +
				"Jones@lavastorm.com|Marinelli@lavastorm.com|Nason@lavastorm.com|Parker@lavastorm.com|" +
				"Pigneri@lavastorm.com|Rasmussen@lavastorm.com|Regan@lavastorm.com|Segall@Lavastorm.com|" +
				"Smith@lavastorm.com|Song@lavastorm.com|Tolleson@lavastorm.com|wynn@lavastorm.com|" +
				"Ahmed@lavastorm.com|Benoit@lavastorm.com|Keane@lavastorm.com|Kilker@lavastorm.com|" +
				"Waters@lavastorm.com|Meagher@lavastorm.com|Mok@lavastorm.com|Mullin@lavastorm.com|" +
				"Nason@lavastorm.com|reilly@lavastorm.com|Scoble@lavastorm.com|Comerford@lavastorm.com|" +
				"Gallagher@lavastorm.com|Hughes@lavastorm.com|Kelly@lavastorm.com|" +
				"Tuddenham@lavastorm.com|Williams@lavastorm.com|Wilson@lavastorm.com";
		String inputs[] = input.split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train("tim@cobber com");
		analysis.train("tim@cobber com");
		analysis.train(null);

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), inputs.length + 2 + result.getNullCount());
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getPattern(), "\\a{14,24}");
		Assert.assertEquals(result.getConfidence(), 0.9487179487179487);
		Assert.assertEquals(result.getType(), "String");
		Assert.assertEquals(result.getTypeQualifier(), "Email");
	}

	@Test
	public void basicEmailList() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String input = "Bachmann@lavastorm.com,Biedermann@lavastorm.com|buchheim@lavastorm.com|" +
				"coleman@lavastorm.com,Drici@lavastorm.com|Garvey@lavastorm.com|jackson@lavastorm.com|" +
				"Jones@lavastorm.com|Marinelli@lavastorm.com,Nason@lavastorm.com,Parker@lavastorm.com|" +
				"Pigneri@lavastorm.com|Rasmussen@lavastorm.com|Regan@lavastorm.com|Segall@Lavastorm.com|" +
				"Smith@lavastorm.com|Song@lavastorm.com|Tolleson@lavastorm.com|wynn@lavastorm.com|" +
				"Ahmed@lavastorm.com|Benoit@lavastorm.com|Keane@lavastorm.com|Kilker@lavastorm.com|" +
				"Waters@lavastorm.com|Meagher@lavastorm.com|Mok@lavastorm.com|Mullin@lavastorm.com|" +
				"Nason@lavastorm.com|reilly@lavastorm.com|Scoble@lavastorm.com|Comerford@lavastorm.com|" +
				"Gallagher@lavastorm.com|Hughes@lavastorm.com|Kelly@lavastorm.com|" +
				"Tuddenham@lavastorm.com,Williams@lavastorm.com,Wilson@lavastorm.com";
		String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), "String");
		Assert.assertEquals(result.getTypeQualifier(), "Email");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\a{17,67}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void basicEmailListSemicolon() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String input = "Bachmann@lavastorm.com;Biedermann@lavastorm.com|buchheim@lavastorm.com|" +
				"coleman@lavastorm.com;Drici@lavastorm.com|Garvey@lavastorm.com|jackson@lavastorm.com|" +
				"Jones@lavastorm.com|Marinelli@lavastorm.com;Nason@lavastorm.com;Parker@lavastorm.com|" +
				"Pigneri@lavastorm.com|Rasmussen@lavastorm.com|Regan@lavastorm.com|Segall@Lavastorm.com|" +
				"Smith@lavastorm.com|Song@lavastorm.com|Tolleson@lavastorm.com|wynn@lavastorm.com|" +
				"Ahmed@lavastorm.com|Benoit@lavastorm.com|Keane@lavastorm.com|Kilker@lavastorm.com|" +
				"Waters@lavastorm.com|Meagher@lavastorm.com|Mok@lavastorm.com|Mullin@lavastorm.com|" +
				"Nason@lavastorm.com|reilly@lavastorm.com|Scoble@lavastorm.com|Comerford@lavastorm.com|" +
				"Gallagher@lavastorm.com|Hughes@lavastorm.com|Kelly@lavastorm.com|" +
				"Tuddenham@lavastorm.com;Williams@lavastorm.com;Wilson@lavastorm.com|bo gus|";
		String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), "String");
		Assert.assertEquals(result.getTypeQualifier(), "Email");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length - 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\a{6,67}");
		Assert.assertEquals(result.getConfidence(), 0.96875);
	}

	@Test
	public void basicZip() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String input = "01770|01772|01773|02027|02030|02170|02379|02657|02861|03216|03561|03848|04066|04281|04481|04671|04921|05072|05463|05761|" +
				"06045|06233|06431|06704|06910|07101|07510|07764|08006|08205|08534|08829|10044|10260|10549|10965|11239|11501|11743|11976|" +
				"12138|12260|12503|12746|12878|13040|13166|13418|13641|13801|14068|14276|14548|14731|14865|15077|15261|15430|15613|15741|" +
				"15951|16210|16410|16662|17053|17247|17516|17765|17951|18109|18428|18702|18957|19095|19339|19489|19808|20043|20170|20370|" +
				"20540|20687|20827|21047|21236|21779|22030|22209|22526|22741|23016|23162|23310|23503|23868|24038|24210|24430|24594|24856|" +
				"25030|25186|25389|25638|25841|26059|26524|26525|26763|27199|27395|27587|27832|27954|28119|28280|28397|28543|28668|28774|" +
				"29111|29329|29475|29622|29744|30016|30119|30235|30343|30503|30643|31002|31141|31518|31724|31901|32134|32297|32454|32617|" +
				"32780|32934|33093|33265|33448|33603|33763|33907|34138|34470|34731|35053|35221|35491|35752|36022|36460|36616|36860|37087|";
		String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), "Long");
		Assert.assertEquals(result.getTypeQualifier(), "Zip");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 32);
		Assert.assertEquals(result.getPattern(), "[ZIP]");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void zipUnwind() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String input = "02421|02420|02421|02420|02421|02420|02421|02420|02421|02420|" +
				"02421|02420|02421|02420|02421|02420|02421|02420|02421|02420|" +
				"10248|10249|10250|10251|10252|10253|10254|10255|10256|10257|10258|10259|10260|10261|10262|10263|10264|" +
				"bogus|" +
						"10265|10266|10267|10268|10269|10270|10271|10272|10273|10274|10275|10276|10277|10278|10279|10280|10281|" +
						"10282|10283|10284|10285|10286|10287|10288|10289|10290|10291|10292|10293|10294|10295|10296|10297|10298|" +
						"10299|10300|10301|10302|10303|10304|10305|10306|10307|10308|10309|10310|10311|10312|10313|10314|10315|" +
						"10316|10317|10318|10319|10320|10321|10322|10323|10324|10325|10326|10327|10328|10329|10330|10331|10332|" +
						"10333|10334|10335|10336|10337|10338|10339|10340|10341|10342|10343|10344|10345|10346|10347|10348|10349|" +
						"10350|10351|10352|10353|10354|10355|10356|10357|10358|10359|10360|10361|10362|10363|10364|10365|10366|" +
						"10367|10368|10369|10370|10371|10372|10373|10374|10375|10376|10377|10378|10379|10380|10381|10382|10383|" +
						"10384|10385|10386|10387|10388|10389|10390|10391|10392|10393|10394|10395|10396|10397|10398|10399|10400|" +
						"10401|10402|10403|10404|10405|10406|10407|10408|10409|10410|10411|10412|10413|10414|10415|10416|10417|" +
						"10418|10419|10420|10421|10422|10423|10424|10425|10426|10427|10428|10429|10430|10431|10432|10433|10434|" +
						"10435|10436|10437|10438|10439|10440|10441|10442|10443|10444|10445|10446|10447|10448|10449|10450|10451|" +
						"10452|10453|10454|10455|10456|10457|10458|10459|10460|10461|10462|10463|10464|10465|10466|10467|10468|" +
						"10469|10470|10471|10472|10473|10474|10475|10476|10477|10478|10479|10480|10481|10482|10483|10484|10485|" +
						"10486|10487|10488|10489|10490|10491|10492|10493|10494|10495|10496|10497|10498|10499|10500|10501|10502|" +
						"10503|10504|10505|10506|10507|10508|10509|10510|10511|10512|10513|10514|10515|10516|10517|10518|10519|" +
						"10520|10521|10522|10523|10524|10525|10526|10527|10528|10529|10530|10531|10532|10533|10534|10535|10536|" +
						"10537|10538|10539|10540|10541|10542|10543|10544|10545|10546|10547|10548|10549|10550|10551|10552|10553|" +
						"10554|10555|10556|10557|10558|10559|10560|10561|10562|10563|10564|10565|10566|10567|10568|10569|10570|" +
						"10571|10572|10573|10574|10575|10576|10577|10578|10579|10580|10581|10582|10583|10584|10585|10586|10587|" +
						"10588|10589|10590|10591|10592|10593|10594|10595|10596|10597|10598|10599|10600|10601|10602|10603|10604|" +
						"10605|10606|10607|10608|10609|10610|10611|10612|10613|10614|10615|10616|10617|10618|10619|10620|10621|" +
						"10622|10623|10624|10625|10626|10627|10628|10629|10630|10631|10632|10633|10634|10635|10636|10637|10638|" +
						"10639|10640|10641|10642|10643|10644|10645|10646|10647|10648|10649|10650|10651|10652|10653|10654|10655|" +
						"10656|10657|10658|10659|10660|10661|10662|10663|10664|10665|10666|10667|10668|10669|10670|10671|10672|" +
						"10673|10674|10675|10676|10677|10678|10679|10680|10681|10682|10683|10684|10685|10686|10687|10688|10689|" +
						"10690|10691|10692|10693|10694|10695|10696|10697|10698|10699|10700|10701|10702|10703|10704|10705|10706|" +
						"10707|10708|10709|10710|10711|10712|10713|10714|10715|10716|10717|10718|10719|10720|10721|10722|10723|" +
						"10724|10725|10726|10727|10728|10729|10730|10731|10732|10733|10734|10735|10736|10737|10738|10739|10740|" +
						"10741|10742|10743|10744|10745|10746|10747|10748|10749|10750|10751|10752|10753|10754|10755|10756|10757|" +
						"10758|10759|10760|10761|10762|10763|10764|10765|10766|10767|10768|10769|10770|10771|10772|10773|10774|" +
						"10775|10776|10777|10778|10779|10780|10781|10782|10783|10784|10785|10786|10787|10788|10789|10790|10791|" +
						"10792|10793|10794|10795|10796|10797|10798|10799|10800|10801|10802|10803|10804|10805|10806|10807|10808|" +
						"10809|10810|10811|10812|10813|10814|10815|10816|10817|10818|10819|10820|10821|10822|10823|10824|10825|" +
						"10826|10827|10828|10829|10830|10831|10832|10833|10834|10835|10836|10837|10838|10839|10840|10841|10842|" +
						"10843|10844|10845|10846|10847|10848|10849|10850|10851|10852|10853|10854|10855|10856|10857|10858|10859|" +
						"10860|10861|10862|10863|10864|10865|10866|10867|10868|10869|10870|10871|10872|10873|10874|10875|10876|" +
						"10877|10878|10879|10880|10881|10882|10883|10884|10885|10886|10887|10888|10889|10890|10891|10892|10893|" +
						"10894|10895|10896|10897|10898|10899|10900|10901|10902|10903|10904|10905|10906|10907|10908|10909|10910|" +
						"10911|10912|10913|10914|10915|10916|10917|10918|10919|10920|10921|10922|10923|10924|10925|10926|10927|" +
						"10928|10929|10930|10931|10932|10933|10934|10935|10936|10937|10938|10939|10940|10941|10942|10943|10944|" +
						"10945|10946|10947|10948|10949|10950|10951|10952|10953|10954|10955|10956|10957|10958|10959|10960|10961|" +
						"10962|10963|10964|10965|10966|10967|10968|10969|10970|10971|10972|10973|10974|10975|10976|10977|10978|" +
						"10979|10980|10981|10982|10983|10984|10985|10986|10987|10988|10989|10990|10991|10992|10993|10994|10995|" +
						"10996|10997|10998|10999|11000|11001|11002|11003|11004|11005|11006|11007|11008|11009|11010|11011|11012|" +
						"11013|11014|11015|11016|11017|11018|11019|11020|11021|11022|11023|11024|11025|11026|11027|11028|11029|" +
						"11030|11031|11032|11033|11034|11035|11036|11037|11038|11039|11040|11041|11042|11043|11044|11045|11046|" +
						"11047|11048|11049|11050|11051|11052|11053|11054|11055|11056|11057|11058|11059|11060|11061|11062|11063|";
		String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), "Long");
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length - 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 20);
		Assert.assertEquals(result.getPattern(), "\\d{5}");
		Assert.assertEquals(result.getConfidence(), 0.998805256869773);
	}

	@Test
	public void zipNotReal() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String input =
				"10248|10249|10250|10251|10252|10253|10254|10255|10256|10257|10258|10259|10260|10261|10262|10263|10264|" +
						"10265|10266|10267|10268|10269|10270|10271|10272|10273|10274|10275|10276|10277|10278|10279|10280|10281|" +
						"10282|10283|10284|10285|10286|10287|10288|10289|10290|10291|10292|10293|10294|10295|10296|10297|10298|" +
						"10299|10300|10301|10302|10303|10304|10305|10306|10307|10308|10309|10310|10311|10312|10313|10314|10315|" +
						"10316|10317|10318|10319|10320|10321|10322|10323|10324|10325|10326|10327|10328|10329|10330|10331|10332|" +
						"10333|10334|10335|10336|10337|10338|10339|10340|10341|10342|10343|10344|10345|10346|10347|10348|10349|" +
						"10350|10351|10352|10353|10354|10355|10356|10357|10358|10359|10360|10361|10362|10363|10364|10365|10366|" +
						"10367|10368|10369|10370|10371|10372|10373|10374|10375|10376|10377|10378|10379|10380|10381|10382|10383|" +
						"10384|10385|10386|10387|10388|10389|10390|10391|10392|10393|10394|10395|10396|10397|10398|10399|10400|" +
						"10401|10402|10403|10404|10405|10406|10407|10408|10409|10410|10411|10412|10413|10414|10415|10416|10417|" +
						"10418|10419|10420|10421|10422|10423|10424|10425|10426|10427|10428|10429|10430|10431|10432|10433|10434|" +
						"10435|10436|10437|10438|10439|10440|10441|10442|10443|10444|10445|10446|10447|10448|10449|10450|10451|" +
						"10452|10453|10454|10455|10456|10457|10458|10459|10460|10461|10462|10463|10464|10465|10466|10467|10468|" +
						"10469|10470|10471|10472|10473|10474|10475|10476|10477|10478|10479|10480|10481|10482|10483|10484|10485|" +
						"10486|10487|10488|10489|10490|10491|10492|10493|10494|10495|10496|10497|10498|10499|10500|10501|10502|" +
						"10503|10504|10505|10506|10507|10508|10509|10510|10511|10512|10513|10514|10515|10516|10517|10518|10519|" +
						"10520|10521|10522|10523|10524|10525|10526|10527|10528|10529|10530|10531|10532|10533|10534|10535|10536|" +
						"10537|10538|10539|10540|10541|10542|10543|10544|10545|10546|10547|10548|10549|10550|10551|10552|10553|" +
						"10554|10555|10556|10557|10558|10559|10560|10561|10562|10563|10564|10565|10566|10567|10568|10569|10570|" +
						"10571|10572|10573|10574|10575|10576|10577|10578|10579|10580|10581|10582|10583|10584|10585|10586|10587|" +
						"10588|10589|10590|10591|10592|10593|10594|10595|10596|10597|10598|10599|10600|10601|10602|10603|10604|" +
						"10605|10606|10607|10608|10609|10610|10611|10612|10613|10614|10615|10616|10617|10618|10619|10620|10621|" +
						"10622|10623|10624|10625|10626|10627|10628|10629|10630|10631|10632|10633|10634|10635|10636|10637|10638|" +
						"10639|10640|10641|10642|10643|10644|10645|10646|10647|10648|10649|10650|10651|10652|10653|10654|10655|" +
						"10656|10657|10658|10659|10660|10661|10662|10663|10664|10665|10666|10667|10668|10669|10670|10671|10672|" +
						"10673|10674|10675|10676|10677|10678|10679|10680|10681|10682|10683|10684|10685|10686|10687|10688|10689|" +
						"10690|10691|10692|10693|10694|10695|10696|10697|10698|10699|10700|10701|10702|10703|10704|10705|10706|" +
						"10707|10708|10709|10710|10711|10712|10713|10714|10715|10716|10717|10718|10719|10720|10721|10722|10723|" +
						"10724|10725|10726|10727|10728|10729|10730|10731|10732|10733|10734|10735|10736|10737|10738|10739|10740|" +
						"10741|10742|10743|10744|10745|10746|10747|10748|10749|10750|10751|10752|10753|10754|10755|10756|10757|" +
						"10758|10759|10760|10761|10762|10763|10764|10765|10766|10767|10768|10769|10770|10771|10772|10773|10774|" +
						"10775|10776|10777|10778|10779|10780|10781|10782|10783|10784|10785|10786|10787|10788|10789|10790|10791|" +
						"10792|10793|10794|10795|10796|10797|10798|10799|10800|10801|10802|10803|10804|10805|10806|10807|10808|" +
						"10809|10810|10811|10812|10813|10814|10815|10816|10817|10818|10819|10820|10821|10822|10823|10824|10825|" +
						"10826|10827|10828|10829|10830|10831|10832|10833|10834|10835|10836|10837|10838|10839|10840|10841|10842|" +
						"10843|10844|10845|10846|10847|10848|10849|10850|10851|10852|10853|10854|10855|10856|10857|10858|10859|" +
						"10860|10861|10862|10863|10864|10865|10866|10867|10868|10869|10870|10871|10872|10873|10874|10875|10876|" +
						"10877|10878|10879|10880|10881|10882|10883|10884|10885|10886|10887|10888|10889|10890|10891|10892|10893|" +
						"10894|10895|10896|10897|10898|10899|10900|10901|10902|10903|10904|10905|10906|10907|10908|10909|10910|" +
						"10911|10912|10913|10914|10915|10916|10917|10918|10919|10920|10921|10922|10923|10924|10925|10926|10927|" +
						"10928|10929|10930|10931|10932|10933|10934|10935|10936|10937|10938|10939|10940|10941|10942|10943|10944|" +
						"10945|10946|10947|10948|10949|10950|10951|10952|10953|10954|10955|10956|10957|10958|10959|10960|10961|" +
						"10962|10963|10964|10965|10966|10967|10968|10969|10970|10971|10972|10973|10974|10975|10976|10977|10978|" +
						"10979|10980|10981|10982|10983|10984|10985|10986|10987|10988|10989|10990|10991|10992|10993|10994|10995|" +
						"10996|10997|10998|10999|11000|11001|11002|11003|11004|11005|11006|11007|11008|11009|11010|11011|11012|" +
						"11013|11014|11015|11016|11017|11018|11019|11020|11021|11022|11023|11024|11025|11026|11027|11028|11029|" +
						"11030|11031|11032|11033|11034|11035|11036|11037|11038|11039|11040|11041|11042|11043|11044|11045|11046|" +
						"11047|11048|11049|11050|11051|11052|11053|11054|11055|11056|11057|11058|11059|11060|11061|11062|11063|";
		String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), "Long");
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{5}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void sameZip() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		int locked = -1;
		int copies = 100;

		for (int i = 0; i < copies; i++) {
			if (analysis.train("02421") && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), "Long");
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), copies);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), copies);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), copies);
		Assert.assertEquals(result.getPattern(), "\\d{5}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void changeMind() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();
		int locked = -1;

		for (int i = 0; i < 2 * TextAnalyzer.SAMPLE_DEFAULT; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i;
		}

		for (char ch = 'a'; ch <= 'z'; ch++) {
			analysis.train(String.valueOf(ch));
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), "String");
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), 2 * TextAnalyzer.SAMPLE_DEFAULT + 26);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), 2 * TextAnalyzer.SAMPLE_DEFAULT + 26);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\a{1,2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void leadingZeros() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer("BL record ID", null);

		analysis.train("000019284");
		analysis.train("000058669");
		analysis.train("000093929");
		analysis.train("000154545");
		analysis.train("000190188");
		analysis.train("000370068");
		analysis.train("000370069");
		analysis.train("000370070");
		analysis.train("000440716");
		analysis.train("000617304");
		analysis.train("000617305");
		analysis.train("000617306");
		analysis.train("000617307");
		analysis.train("000617308");
		analysis.train("000617309");
		analysis.train("000617310");
		analysis.train("000617311");
		analysis.train("000617312");
		analysis.train("000617314");
		analysis.train("000617315");
		analysis.train("000617316");
		analysis.train("000617317");
		analysis.train("000617318");
		analysis.train("000617319");
		analysis.train("000617324");
		analysis.train("000617325");
		analysis.train("000617326");
		analysis.train("000617331");
		analysis.train("000617335");
		analysis.train("000617336");
		analysis.train("000617337");
		analysis.train("000617338");
		analysis.train("000617339");
		analysis.train("000617342");
		analysis.train("000617347");
		analysis.train("000617348");
		analysis.train("000617349");
		analysis.train("000617350");
		analysis.train("000617351");
		analysis.train("000617354");
		analysis.train("000617355");
		analysis.train("000617356");
		analysis.train("000617357");
		analysis.train("000617358");
		analysis.train("000617359");
		analysis.train("000617360");
		analysis.train("000617361");
		analysis.train("000617362");
		analysis.train("000617363");
		analysis.train("000617364");
		analysis.train("000617365");
		analysis.train("000617366");
		analysis.train("000617368");
		analysis.train("000617369");
		analysis.train("000617370");
		analysis.train("000617371");
		analysis.train("000617372");
		analysis.train("000617373");
		analysis.train("000617374");

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), "Long");
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), 59);
		Assert.assertEquals(result.getMatchCount(), 59);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 59);
		Assert.assertEquals(result.getPattern(), "\\d{9}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void notZipButNumeric() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		int locked = -1;
		int start = 10000;
		int end = 99999;

		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i;
		}
		analysis.train("No Zip provided");

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, start + TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), "Long");
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), end + 1 - start);
		Assert.assertEquals(result.getMatchCount(), end - start);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{5}");
		Assert.assertEquals(result.getConfidence(), 0.9999888888888889);
	}

	@Test
	public void notZips() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		int locked = -1;
		int start = 10000;
		int end = 99999;

		for (int i = start; i < end; i++) {
			if (analysis.train(i < 80000 ? String.valueOf(i) : "A" + String.valueOf(i)) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, start + TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), "String");
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), end - start);
		Assert.assertEquals(result.getMatchCount(), end - start);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\a{5,6}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void basicStates() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String input = "AL|AK|AZ|KY|KS|LA|ME|MD|MI|MA|MN|MS|MO|NE|MT|SD|TN|TX|UT|VT|WI|" +
				"VA|WA|WV|HI|ID|IL|IN|IA|KS|KY|LA|ME|MD|MA|MI|MN|MS|MO|MT|NE|NV|" +
				"NH|NJ|NM|NY|NC|ND|OH|OK|OR|PA|RI|SC|SD|TN|TX|UT|VT|VA|WA|WV|WI|" +
				"WY|AL|AK|AZ|AR|CA|CO|CT|DC|DE|FL|GA|HI|ID|IL|IN|IA|KS|KY|LA|ME|" +
				"MD|MA|MI|MN|MS|MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|OH|OK|OR|RI|SC|SD|" +
				"TX|UT|VT|WV|WI|WY|NV|NH|NJ|OR|PA|RI|SC|AR|CA|CO|CT|ID|HI|IL|IN|";
		String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), "String");
		Assert.assertEquals(result.getTypeQualifier(), "US_STATE");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "[US_STATE]");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void basicStatesLower() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String input = "al|ak|az|ky|ks|la|me|md|mi|ma|mn|ms|mo|ne|mt|sd|tn|tx|ut|vt|wi|" +
				"va|wa|wv|hi|id|il|in|ia|ks|ky|la|me|md|ma|mi|mn|ms|mo|mt|ne|nv|" +
				"nh|nj|nm|ny|nc|nd|oh|ok|or|pa|ri|sc|sd|tn|tx|ut|vt|va|wa|wv|wi|" +
				"wy|al|ak|az|ar|ca|co|ct|dc|de|fl|ga|hi|id|il|in|ia|ks|ky|la|me|" +
				"md|ma|mi|mn|ms|mo|mt|ne|nv|nh|nj|nm|ny|nc|nd|oh|ok|or|ri|sc|sd|" +
				"tx|ut|vt|wv|wi|wy|nv|nh|nj|or|pa|ri|sc|ar|ca|co|ct|id|hi|il|in|";
		String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), "String");
		Assert.assertEquals(result.getTypeQualifier(), "US_STATE");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "[US_STATE]");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void basicNA() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String input = "AL|AK|AZ|KY|KS|LA|ME|MD|MI|MA|AB|AB|MN|MS|MO|NE|MT|SD|TN|TX|UT|VT|WI|" +
				"VA|WA|WV|HI|ID|IL|IN|IA|KS|KY|LA|ME|MD|MA|MI|YT|MN|MS|MO|MT|NE|NV|XX|" +
				"NH|NJ|NM|NY|NC|ND|OH|OK|OR|PA|RI|SC|SD|TN|TX|UT|VT|VA|WA|WV|WI|NF|NB|" +
				"WY|AL|AK|AZ|AR|CA|CO|CT|DC|DE|FL|GA|HI|ID|IL|IN|IA|KS|KY|LA|ME|PE|BC|" +
				"MD|MA|MI|MN|MS|MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|OH|OK|OR|RI|SC|SD|ON|LB|" +
				"TX|UT|VT|WV|WI|WY|NV|NH|NJ|OR|PA|RI|SC|AR|CA|CO|CT|ID|HI|IL|IN|YT|LB|";
		String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), "String");
		Assert.assertEquals(result.getTypeQualifier(), "NA_STATE");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length - 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "[NA_STATE]");
		Assert.assertEquals(result.getConfidence(), 0.9927536231884058);
	}

	@Test
	public void basicCA() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String input = "AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|";
		String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), "String");
		Assert.assertEquals(result.getTypeQualifier(), "CA_PROVINCE");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "[CA_PROVINCE]");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void change2() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String input = "AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"Jan|Mar|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
				"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|Sep|Jan|Oct|Oct|Oct|" +
				"Jan|Mar|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
				"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|NA|Sep|Jan|Oct|Oct|Oct|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|";
		String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), "String");
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\a{2,3}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void basicCountry() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String input = "Venezuela|USA|Finland|USA|USA|Germany|France|Italy|Mexico|Germany|" +
				"Sweden|Germany|Sweden|Spain|Spain|Venezuela|Germany|Germany|Germany|Brazil|" +
				"Italy|UK|Brazil|Brazil|Brazil|Mexico|USA|France|Venezuela|France|" +
				"Ireland|Brazil|Italy|Germany|Belgium|Spain|Mexico|USA|Spain|USA|" +
				"Mexico|Ireland|USA|France|Germany|Germany|USA|UK|USA|USA|" +
				"UK|Mexico|Finland|UK|Mexico|Germany|USA|Germany|Spain|Sweden|" +
				"Portugal|USA|Venezuela|France|Canada|Finland|France|Ireland|Portugal|Germany|" +
				"USA|Canada|France|Denmark|Germany|Germany|USA|Germany|USA|Brazil|" +
				"Germany|USA|France|Austria|Portugal|Austria|Mexico|UK|Germany|Venezuela|" +
				"France|UK|France|Germany|France|Germany|UK|Mexico|Spain|Denmark|" +
				"Austria|USA|Switzerland|France|Brazil|Ireland|Poland|USA|Canada|UK|" +
				"Sweden|Brazil|Ireland|Venezuela|Austria|UK|Sweden|USA|Brazil|Norway|" +
				"UK|Canada|Austria|Germany|Austria|USA|USA|Venezuela|Germany|Portugal|" +
				"USA|Denmark|UK|USA|Austria|Austria|Italy|Venezuela|Brazil|Germany|" +
				"France|Argentina|Canada|Canada|Finland|France|Brazil|USA|Finland|Denmark|" +
				"Germany|Switzerland|Brazil|Brazil|Italy|Brazil|Canada|France|Spain|Austria|" +
				"Italy|Ireland|Austria|Canada|USA|Portugal|Sweden|UK|France|Finland|" +
				"Germany|Canada|USA|USA|Austria|Italy|Sweden|Sweden|Germany|Brazil|" +
				"Argentina|France|France|Germany|USA|UK|France|Finland|Germany|Germany|" +
				"Belgium|France|Sweden|Venezuela|UK|Belgium|Portugal|Denmark|Brazil|Italy|" +
				"Germany|USA|France|UK|UK|UK|Mexico|Belgium|Venezuela|Portugal|" +
				"France|USA|France|Brazil|USA|USA|UK|Venezuela|Venezuela|Brazil|" +
				"Germany|Austria|Venezuela|Portugal|Canada|France|Brazil|Canada|Brazil|Germany|" +
				"Venezuela|Venezuela|France|Germany|Mexico|Ireland|USA|Canada|Germany|Mexico|" +
				"Germany|Germany|USA|France|Brazil|Germany|Austria|Germany|Ireland|UK|";
		String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), "String");
		Assert.assertEquals(result.getTypeQualifier(), "COUNTRY");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "[COUNTRY]");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	// Set of valid months + 2 x "NA", 1 x "bogus", 1 x "Bad"
	String monthTest = "Jan|Mar|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
			"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|Sep|Jan|Oct|Oct|Oct|" +
			"Jan|Mar|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
			"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|Sep|Jan|Oct|Oct|Oct|" +
			"Jan|Mar|Jun|Jul|Feb|Dec|Bad|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
			"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|NA|Sep|Jan|Oct|Oct|Oct|" +
			"Jan|Bogus|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
			"Dec|Oct|Apr|May|May|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|Sep|Jan|Oct|Oct|Oct|" +
			"Jan|Mar|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
			"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|NA|Sep|Jan|Oct|Oct|Oct|";

	@Test
	public void basicMonthAbbr() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		int badCount = 4;
		String inputs[] = monthTest.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getPattern(), "[MONTHABBR]");
		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), "String");
		Assert.assertEquals(result.getTypeQualifier(), "MONTHABBR");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 3);
		Map<String, Integer> outliers = result.getOutlierDetails();
		Assert.assertEquals(outliers.size(), 3);
		Assert.assertEquals(outliers.get("Bogus"), new Integer(1));
		Assert.assertEquals(outliers.get("NA"), new Integer(2));
		Assert.assertEquals(outliers.get("Bad"), new Integer(1));
		Assert.assertEquals(result.getMatchCount(), inputs.length - badCount);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 0.9835164835164835);

		/* BUG cannot train once we have determined a logical type
		analysis.train("Another bad element");
		result = analysis.getResult();

		Assert.assertEquals(result.getPattern(), "[MONTHABBR]");
		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), "String");
		Assert.assertEquals(result.getTypeQualifier(), "MONTHABBR");
		Assert.assertEquals(result.getSampleCount(), inputs.length + 1);
		Assert.assertEquals(result.getOutlierCount(), 4);
		Map<String, Integer> updatedOutliers = result.getOutlierDetails();
		Assert.assertEquals(updatedOutliers.size(), 4);
		Assert.assertEquals(updatedOutliers.get("Bogus"), new Integer(1));
		Assert.assertEquals(updatedOutliers.get("NA"), new Integer(2));
		Assert.assertEquals(updatedOutliers.get("Bad"), new Integer(1));
		Assert.assertEquals(updatedOutliers.get("Another bad element"), new Integer(1));
		Assert.assertEquals(result.getMatchCount(), inputs.length - badCount);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);
	*/
	}

	@Test
	public void basicMonthAbbrExcessiveBad() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String inputs[] = monthTest.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train("Another bad element");

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getPattern(), "\\a{3}");
		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), "String");
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length + 1);
		Assert.assertEquals(result.getOutlierCount(), 3);
		Map<String, Integer> outliers = result.getOutlierDetails();
		Assert.assertEquals(outliers.size(), 3);
		Assert.assertEquals(outliers.get("Bogus"), new Integer(1));
		Assert.assertEquals(outliers.get("NA"), new Integer(2));
		Assert.assertEquals(outliers.get("Another bad element"), new Integer(1));
		Assert.assertEquals(result.getMatchCount(), inputs.length + 1 - 4);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 0.9781420765027322);
	}

	@Test
	public void basicPromoteToDouble() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String input =
					"8|172.67|22.73|150|30.26|54.55|45.45|433.22|172.73|7.73|" +
						"218.18|47.27|31.81|22.73|21.43|7.27|26.25|7.27|45.45|80.91|" +
						"63.64|13.64|45.45|15|425.45|95.25|60.15|100|80|72.73|" +
						"0.9|181.81|90|545.45|33.68|13.68|12.12|15|615.42|";
		String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), "Double");
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{*}D\\d{+}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void basicPromote() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		String input =
				"01000053218|0100BRP90233|0100BRP90237|0180BAA01319|0180BAC30834|0190NSC30194|0190NSC30195|0190NSC30652|0190NSC30653|0190NSC30784|" +
		"0190NSC30785|0190NSY28569|0190NSZ01245|020035037|02900033|02900033|02900039|02901210|02903036|02903037|" +
		"030051210001|030051210002|030054160002|030055200003|03700325|03700325|0380F968G059|040000002968|049000000804|049002399361|" +
		"049002399861|0500CCITY084|0500CCITY248|0500CCITY476|0500FWISH002|0500HHUNT027|0500HSTNS060|0500HSTNS062|0500SHARS006|0500SHARS016|" +
		"0590PET621|0590PET622|0590PQG571|0600CR087|0600CR290|0610CH19130|0610CH548|0610EP19031|068000000461|068000000462|" +
		"068000000502|069000024300|0690WNA02867|0690WNA02867|075071047A|075071047B|07605752|077072401A|077072401A|077072572A|" +
		"077072583A|079073001K|0800COA10071|0800COA10194|0800COA10196|0800COA10196|0800COA10204|0800COA10207|0800COA10267|0800COA10268|" +
		"0800COA10268|0800COA10268|0800COA10386|0800COA10469|0800COA10470|0800COA10490|0800COB20133|0800COB20134|0800COB20138|0800COB20139|" +
		"0800COC30257|0800COC30258|0800COC30488|0800COC30504|0800COC30505|0800COC30649|0800COC30815|0800COC30873|0800COC31003|0800COC31004|" +
		"0800COC31093|0800COC31215|0800COC31216|0800COC31221|0800COC31222|0800COC31229|0800COC31231|0800COC31306|0800COC31307|";
		String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), "String");
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\a{8,12}");
		Assert.assertEquals(result.getConfidence(), 1.0);
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
	public void someInts() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();
		Random random = new Random();
		int locked = -1;

		for (int i = 0; i <= TextAnalyzer.SAMPLE_DEFAULT; i++) {
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = i;
		}
		for (int i = 0; i <= TextAnalyzer.SAMPLE_DEFAULT; i++) {
			analysis.train(String.valueOf(random.nextDouble()));
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), 2 * (TextAnalyzer.SAMPLE_DEFAULT + 1));
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), "Long");
		Assert.assertEquals(result.getConfidence(), .5);
	}

	@Test
	public void setSampleSize() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();
		Random random = new Random();
		int locked = -1;
		int sample = 0;

		analysis.setSampleSize(2* TextAnalyzer.SAMPLE_DEFAULT);
		for (int i = 0; i <= TextAnalyzer.SAMPLE_DEFAULT; i++) {
			sample++;
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = sample;
		}
		for (int i = 0; i <= TextAnalyzer.SAMPLE_DEFAULT; i++) {
			sample++;
			if (analysis.train(String.valueOf(random.nextDouble())) && locked == -1)
				locked = sample;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, 2 * TextAnalyzer.SAMPLE_DEFAULT + 1);
		Assert.assertEquals(result.getSampleCount(), 2 * (TextAnalyzer.SAMPLE_DEFAULT + 1));
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), "Double");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void getSampleSize()  throws IOException {
		TextAnalyzer analysis = new TextAnalyzer();

		Assert.assertEquals(analysis.getSampleSize(), TextAnalyzer.SAMPLE_DEFAULT);
	}

	@Test
	public void setSampleSizeTooSmall() throws IOException {
		TextAnalyzer analysis = new TextAnalyzer();

		try {
			analysis.setSampleSize(TextAnalyzer.SAMPLE_DEFAULT - 1);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals(e.getMessage(), "Cannot set sample size below " + TextAnalyzer.SAMPLE_DEFAULT);
			return;
		}
		Assert.fail("Exception should have been thrown");
	}

	@Test
	public void setSampleSizeTooLate() throws IOException {
		TextAnalyzer analysis = new TextAnalyzer();
		Random random = new Random();
		int locked = -1;
		int i = 0;

		for (; i <= TextAnalyzer.SAMPLE_DEFAULT; i++) {
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = i;
		}

		try {
			analysis.setSampleSize(2* TextAnalyzer.SAMPLE_DEFAULT);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals(e.getMessage(), "Cannot change sample size once training has started");
			return;
		}
		Assert.fail("Exception should have been thrown");
	}

	@Test
	public void getMaxCardinality() throws IOException {
		TextAnalyzer analysis = new TextAnalyzer();

		Assert.assertEquals(analysis.getMaxCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
	}

	@Test
	public void setMaxCardinalityTooSmall() throws IOException {
		TextAnalyzer analysis = new TextAnalyzer();

		try {
			analysis.setMaxCardinality(-1);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals(e.getMessage(), "Invalid value for maxCardinality -1");
			return;
		}
		Assert.fail("Exception should have been thrown");
	}

	@Test
	public void setMaxCardinalityTooLate() throws IOException {
		TextAnalyzer analysis = new TextAnalyzer();
		Random random = new Random();
		int locked = -1;
		int i = 0;

		for (; i <= TextAnalyzer.SAMPLE_DEFAULT; i++) {
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = i;
		}

		try {
			analysis.setMaxCardinality(2* TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals(e.getMessage(), "Cannot change maxCardinality once training has started");
			return;
		}
		Assert.fail("Exception should have been thrown");
	}

	@Test
	public void getOutlierCount() throws IOException {
		TextAnalyzer analysis = new TextAnalyzer();

		Assert.assertEquals(analysis.getMaxOutliers(), TextAnalyzer.MAX_OUTLIERS_DEFAULT);
	}

	@Test
	public void setMaxOutliersTooSmall() throws IOException {
		TextAnalyzer analysis = new TextAnalyzer();

		try {
			analysis.setMaxOutliers(-1);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals(e.getMessage(), "Invalid value for outlier count -1");
			return;
		}
		Assert.fail("Exception should have been thrown");
	}

	@Test
	public void setMaxOutliersTooLate() throws IOException {
		TextAnalyzer analysis = new TextAnalyzer();
		Random random = new Random();
		int locked = -1;
		int i = 0;

		for (; i <= TextAnalyzer.SAMPLE_DEFAULT; i++) {
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = i;
		}

		try {
			analysis.setMaxOutliers(2* TextAnalyzer.MAX_OUTLIERS_DEFAULT);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals(e.getMessage(), "Cannot change outlier count once training has started");
			return;
		}
		Assert.fail("Exception should have been thrown");
	}

	@Test
	public void manyRandomDoubles() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();
		Random random = new Random();
		int locked = -1;
		int nullIterations = 50;
		int iterations = 10000;

		for (int i = 0; i < nullIterations; i++) {
			analysis.train(null);
		}
		for (int i = 0; i < iterations; i++) {
			if (analysis.train(String.valueOf(random.nextDouble())) && locked == -1)
				locked = i;
		}
		// This is an outlier
		analysis.train("Zoomer");

		// These are valid doubles
		analysis.train("NaN");
		analysis.train("Infinity");

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), iterations + nullIterations + 3);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), nullIterations);
		Assert.assertEquals(result.getType(), "Double");
		Assert.assertEquals(result.getOutlierCount(), 1);
		Map<String, Integer> outliers = result.getOutlierDetails();
		Assert.assertEquals(outliers.size(), 1);
		Assert.assertEquals(outliers.get("Zoomer"), new Integer(1));
	}

	@Test
	public void manyConstantLengthStrings() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();
		Random random = new Random();
		int locked = -1;
		int nullIterations = 50;
		int iterations = 10000;
		int length = 12;
		StringBuilder b = new StringBuilder(length);
		String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

		for (int i = 0; i < nullIterations; i++) {
			analysis.train(null);
		}
		for (int i = 0; i < iterations; i++) {
			b.setLength(0);
			for (int j = 0; j < length; j++) {
				b.append(alphabet.charAt(Math.abs(random.nextInt()%alphabet.length())));
			}
			if (analysis.train(b.toString()) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), iterations + nullIterations);
		//		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), nullIterations);
		Assert.assertEquals(result.getType(), "String");
		Assert.assertEquals(result.getPattern(), "\\a{12}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void manyConstantLengthLongs() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();
		Random random = new Random();
		int locked = -1;
		int nullIterations = 50;
		int iterations = 10000;

		for (int i = 0; i < nullIterations; i++) {
			analysis.train(null);
		}
		int cnt = 0;
		while (cnt < iterations) {
			long l = random.nextInt(Integer.MAX_VALUE) + 1000000000L;
			if (l >  9999999999L)
				continue;
			if (analysis.train(String.valueOf(l)) && locked == -1)
				locked = cnt;
			cnt++;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), iterations + nullIterations);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), nullIterations);
		Assert.assertEquals(result.getType(), "Long");
		Assert.assertEquals(result.getPattern(), "\\d{10}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void bumpMaxCardinality() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		analysis.setMaxCardinality(2 * TextAnalyzer.MAX_CARDINALITY_DEFAULT);

		Random random = new Random();
		int locked = -1;
		int nullIterations = 50;
		int iterations = 10000;

		for (int i = 0; i < nullIterations; i++) {
			analysis.train(null);
		}
		int cnt = 0;
		while (cnt < iterations) {
			long l = random.nextInt(Integer.MAX_VALUE) + 1000000000L;
			if (l >  9999999999L)
				continue;
			if (analysis.train(String.valueOf(l)) && locked == -1)
				locked = cnt;
			cnt++;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), iterations + nullIterations);
		Assert.assertEquals(result.getCardinality(), 2 * TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), nullIterations);
		Assert.assertEquals(result.getType(), "Long");
		Assert.assertEquals(result.getPattern(), "\\d{10}");
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
		analysis.train("  ");
		analysis.train("    ");

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), iterations + nullIterations + 2);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), nullIterations);
		Assert.assertEquals(result.getBlankCount(), 2);
		Assert.assertEquals(result.getPattern(), "\\d{1,5}");
		Assert.assertEquals(result.getType(), "Long");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getMinValue(), "0");
		Assert.assertEquals(result.getMaxValue(), String.valueOf(iterations - 1));
	}

	@Test
	public void keyFieldLong() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();
		int locked = -1;
		int start = 10000;
		int end = 12000;

		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i - start;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), end - start);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getPattern(), "\\d{5}");
		Assert.assertEquals(result.getType(), "Long");
		Assert.assertTrue(result.isKey());
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void setMaxOutliers() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();
		int locked = -1;
		int start = 10000;
		int end = 12000;
		int newMaxOutliers = 12;

		analysis.setMaxOutliers(newMaxOutliers);

		analysis.train("A");
		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i - start;
		}
		analysis.train("B");
		analysis.train("C");
		analysis.train("D");
		analysis.train("E");
		analysis.train("F");
		analysis.train("G");
		analysis.train("H");
		analysis.train("I");
		analysis.train("J");
		analysis.train("K");
		analysis.train("L");
		analysis.train("M");
		analysis.train("N");
		analysis.train("O");

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(analysis.getMaxOutliers(), newMaxOutliers);
		Assert.assertEquals(result.getOutlierCount(), newMaxOutliers);
		Assert.assertEquals(result.getSampleCount(), newMaxOutliers + 3 + end - start);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getPattern(), "\\d{5}");
		Assert.assertEquals(result.getType(), "Long");
		Assert.assertTrue(result.isKey());
		Assert.assertEquals(result.getConfidence(), 0.9925558312655087);
		Assert.assertEquals(result.dump(true), "TextAnalysisResult [matchCount=2000, sampleCount=2015, nullCount=0, blankCount=0, pattern=\"\\d{5}\", confidence=0.9925558312655087, type=Long, min=\"10000\", max=\"11999\", sum=\"21999000\", cardinality=MAX, outliers=12 {\"A\":1 \"B\":1 \"C\":1 \"D\":1 \"E\":1 \"F\":1 \"G\":1 \"H\":1 \"I\":1 \"J\":1 ...}, PossibleKey]");
	}

	@Test
	public void keyFieldString() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();
		int locked = -1;
		int start = 100000;
		int end = 120000;

		for (int i = start; i < end; i++) {
			if (analysis.train("A" + String.valueOf(i)) && locked == -1)
				locked = i - start;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), end - start);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getPattern(), "\\a{7}");
		Assert.assertEquals(result.getType(), "String");
		Assert.assertTrue(result.isKey());
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void notKeyField() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();
		int locked = -1;
		int start = 10000;
		int end = 12000;

		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i - start;
		}

		analysis.train(String.valueOf(start));

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), 1 + end - start);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getPattern(), "\\d{5}");
		Assert.assertEquals(result.getType(), "Long");
		Assert.assertFalse(result.isKey());
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void DateTimeYYYY_MM_DDTHH_MM_SS() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		analysis.train("2004-01-01T00:00:00");
		analysis.train("2004-01-01T02:00:00");
		analysis.train("2006-01-01T00:00:00");
		analysis.train("2004-01-01T02:00:00");
		analysis.train("2006-01-01T13:00:00");
		analysis.train("2004-01-01T00:00:00");
		analysis.train("2006-01-01T13:00:00");
		analysis.train("2006-01-01T00:00:00");
		analysis.train("2004-01-01T00:00:00");
		analysis.train("2004-01-01T00:00:00");
		analysis.train("2004-01-01T00:00:00");
		analysis.train("2004-01-01T00:00:00");
		analysis.train("2004-01-01T00:00:00");
		analysis.train("2008-01-01T13:00:00");
		analysis.train("2008-01-01T13:00:00");
		analysis.train("2010-01-01T00:00:00");
		analysis.train("2004-01-01T02:00:00");
		analysis.train(null);
		analysis.train("2008-01-01T00:00:00");
		analysis.train(null);

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), 20);
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getType(), "DateTime");
		Assert.assertEquals(result.getPattern(), "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getTypeQualifier(), "yyyy-MM-dd'T'HH:mm:ss");
	}

	@Test
	public void DateTimeYYYY_MM_DDTHH_MM_SS_NNNN() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		analysis.train("2004-01-01T00:00:00-05:00");
		analysis.train("2004-01-01T02:00:00-05:00");
		analysis.train("2006-01-01T00:00:00-05:00");
		analysis.train("2006-01-01T00:00:00-05:00");
		analysis.train("2004-01-01T02:00:00-05:00");
		analysis.train("2006-01-01T13:00:00-05:00");
		analysis.train("2004-01-01T00:00:00-05:00");
		analysis.train("2006-01-01T13:00:00-05:00");
		analysis.train("2006-01-01T00:00:00-05:00");
		analysis.train("2004-01-01T00:00:00-05:00");
		analysis.train("2004-01-01T00:00:00-05:00");
		analysis.train("2004-01-01T00:00:00-05:00");
		analysis.train("2004-01-01T00:00:00-05:00");
		analysis.train("2004-01-01T00:00:00-05:00");
		analysis.train("2008-01-01T13:00:00-05:00");
		analysis.train("2008-01-01T13:00:00-05:00");
		analysis.train("2010-01-01T00:00:00-05:00");
		analysis.train(null);
		analysis.train("2008-01-01T00:00:00-05:00");
		analysis.train(null);

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), "OffsetDateTime");
		Assert.assertEquals(result.getSampleCount(), 20);
		Assert.assertEquals(result.getMatchCount(), 18);
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getPattern(), "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[-+][0-9]{2}:[0-9]{2}");
		Assert.assertEquals(result.getTypeQualifier(), "yyyy-MM-dd'T'HH:mm:ssxxx");
		Assert.assertEquals(result.getConfidence(), 1.0);

		analysis.train("2008-01-01T00:00:00-05:00");
		result = analysis.getResult();
		Assert.assertEquals(result.getSampleCount(), 21);
	}

	@Test
	public void DateTimeYYYY_MM_DDTHH_MM_SS_Z() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		analysis.train("01/26/2012 10:42:23 GMT");
		analysis.train("01/26/2012 10:42:23 GMT");
		analysis.train("01/30/2012 10:59:48 GMT");
		analysis.train("01/25/2012 16:46:43 GMT");
		analysis.train("01/25/2012 16:28:42 GMT");
		analysis.train("01/24/2012 16:53:04 GMT");

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), "ZonedDateTime");
		Assert.assertEquals(result.getSampleCount(), 6);
		Assert.assertEquals(result.getMatchCount(), 6);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2} .*");
		Assert.assertEquals(result.getTypeQualifier(), "MM/dd/yyyy HH:mm:ss z");
		Assert.assertEquals(result.getConfidence(), 1.0);

		analysis.train("01/25/2012 16:28:42 GMT");
		result = analysis.getResult();
		Assert.assertEquals(result.getSampleCount(), 7);
	}

	@Test
	public void intuitDateddMMyyyy_HHmmss() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer("Settlement_Errors", false);

		analysis.train("2/7/2012 06:24:47");
		analysis.train("2/7/2012 09:44:04");
		analysis.train("2/7/2012 06:21:26");
		analysis.train("2/7/2012 06:21:30");
		analysis.train("2/7/2012 06:21:31");
		analysis.train("2/7/2012 06:21:34");
		analysis.train("2/7/2012 06:21:38");
		analysis.train("1/7/2012 23:16:14");
		analysis.train("19/7/2012 17:49:53");
		analysis.train("19/7/2012 17:49:54");
		analysis.train("18/7/2012 09:57:17");
		analysis.train("19/7/2012 17:48:37");
		analysis.train("19/7/2012 17:49:54");
		analysis.train("19/7/2012 17:46:22");
		analysis.train("19/7/2012 17:49:05");
		analysis.train("2/7/2012 06:21:43");
		analysis.train("2/7/2012 06:21:50");
		analysis.train("2/7/2012 06:21:52");
		analysis.train("2/7/2012 06:21:55");
		analysis.train("2/7/2012 06:21:56");
		analysis.train("20/7/2012 17:30:45");
		analysis.train("19/7/2012 17:46:22");
		analysis.train("2/7/2012 05:57:32");
		analysis.train("19/7/2012 17:45:55");
		analysis.train("20/7/2012 17:30:48");
		analysis.train("1/7/2012 18:33:18");
		analysis.train("1/7/2012 18:27:15");
		analysis.train("1/7/2012 18:25:35");
		analysis.train("1/7/2012 18:31:19");
		analysis.train("1/7/2012 18:36:04");
		analysis.train("1/7/2012 19:20:45");
		analysis.train("1/7/2012 19:20:54");
		analysis.train("1/7/2012 19:19:59");
		analysis.train("1/7/2012 19:17:56");
		analysis.train("1/7/2012 19:19:09");
		analysis.train("1/7/2012 19:20:17");
		analysis.train("2/7/2012 06:22:29");
		analysis.train("2/7/2012 06:22:31");
		analysis.train("2/7/2012 06:22:34");

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), "DateTime");
		Assert.assertEquals(result.getTypeQualifier(), "d/M/yyyy HH:mm:ss");
		Assert.assertEquals(result.getSampleCount(), 39);
		Assert.assertEquals(result.getMatchCount(), 39);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getPattern(), "\\d{1,2}/\\d{1,2}/\\d{4} \\d{2}:\\d{2}:\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}
}
