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
		Assert.assertEquals(result.getMin(), "47");
		Assert.assertEquals(result.getMax(), "91");
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
		TextAnalyzer analysis = new TextAnalyzer("BL record ID");

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
			int next = Math.abs(random.nextInt());
			for (int j = 0; j < length; j++) {
				b.append(alphabet.charAt(next%alphabet.length()));
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
			long l = Math.abs(random.nextInt()) + 1000000000L;
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
			long l = Math.abs(random.nextInt()) + 1000000000L;
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
		Assert.assertEquals(result.getMin(), "0");
		Assert.assertEquals(result.getMax(), String.valueOf(iterations - 1));
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

		analysis.setMaxOutliers(2);

		analysis.train("A");
		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i - start;
		}
		analysis.train("B");
		analysis.train("C");

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(analysis.getMaxOutliers(), 2);
		Assert.assertEquals(result.getOutlierCount(), 2);
		Assert.assertEquals(result.getSampleCount(), 3 + end - start);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getPattern(), "\\d{5}");
		Assert.assertEquals(result.getType(), "Long");
		Assert.assertTrue(result.isKey());
		Assert.assertEquals(result.getConfidence(), 0.9985022466300549);
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
		analysis.train("2004-01-  01T02:00:00");
		analysis.train(null);
		analysis.train("2008-01-01T00:00:00");
		analysis.train(null);

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), 20);
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getPattern(), "\\d{4}-\\d{2}-\\d{2}\\a{1}\\d{2}:\\d{2}:\\d{2}");
		Assert.assertEquals(result.getConfidence(), 0.9444444444444444);
		Assert.assertEquals(result.getType(), "DateTime");
		Assert.assertEquals(result.getTypeQualifier(), "ISO8601 (NoTZ)");
	}

	@Test
	public void DateTimeYYYY_MM_DDTHH_MM_SS_NNNN() throws Exception {
		TextAnalyzer analysis = new TextAnalyzer();

		analysis.train("2004-01-01T00:00:00-05:00");
		analysis.train("2004-01-01T02:00:00-05:00");
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
		analysis.train("2004-01-   01T02:00:00-05:00");
		analysis.train(null);
		analysis.train("2008-01-01T00:00:00-05:00");
		analysis.train(null);

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), 20);
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getPattern(), "\\d{4}-\\d{2}-\\d{2}\\a{1}\\d{2}:\\d{2}:\\d{2}-\\d{2}:\\d{2}");
		Assert.assertEquals(result.getType(), "DateTime");
		Assert.assertEquals(result.getTypeQualifier(), "ISO8601 (Offset)");
		Assert.assertEquals(result.getConfidence(), 0.9444444444444444);
	}
}
