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
import java.text.DateFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.cobber.fta.DateTimeParser.DateResolutionMode;

public class AnalysisResultTests {
	@Test
	public void inadequateData() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "47|89|90|91".split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getMinValue(), "47");
		Assert.assertEquals(result.getMaxValue(), "91");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void noData() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "[NULL]");
		Assert.assertEquals(result.getConfidence(), 0.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "NULL");
		Assert.assertEquals(result.dump(true), "TextAnalysisResult [name=anonymous, matchCount=0, sampleCount=0, nullCount=0, blankCount=0, regexp=\"[NULL]\", confidence=0.0, type=String(NULL), min=null, max=null, minLength=0, maxLength=0, sum=null, cardinality=0]");
	}

	@Test
	public void variableLengthPositiveInteger() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "47|909|809821|34590|2|0|12|390|4083|4499045|90|9003|8972|42987|8901".split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{1,7}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getMinValue(), "0");
		Assert.assertEquals(result.getMaxValue(), "4499045");
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxLength(), 7);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void rubbish() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "47|hello|hello,world|=====47=====|aaaa|0|12|b,b,b,b390|4083|dddddd|90|-------|+++++|42987|8901".split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), ".{1,12}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getMinValue(), "+++++");
		Assert.assertEquals(result.getMaxValue(), "hello,world");
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxLength(), 12);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void variableLengthString() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "Hello World|Hello|H|Z|A".split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), ".{1,11}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getMinValue(), "A");
		Assert.assertEquals(result.getMaxValue(), "Z");
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxLength(), 11);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void variableLengthStringWithOutlier() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "HelloWorld|Hello|H|Z|A|Do|Not|Ask|What|You|Can|Do".split("\\|");
		final int ITERATIONS = 10;

		int locked = -1;

		for (int iters = 0; iters < ITERATIONS; iters++) {
			for (int i = 0; i < inputs.length; i++) {
				if (analysis.train(inputs[i]) && locked != -1)
					locked = i;
			}
		}
		analysis.train(";+");


		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length * ITERATIONS + 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{1,10}");
		Assert.assertEquals(result.getMatchCount(), inputs.length * ITERATIONS);
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getMinValue(), "A");
		Assert.assertEquals(result.getMaxValue(), "Z");
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxLength(), 10);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void variableLengthInteger() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "-100000|-1000|-100|-10|-3|-2|-1|100|200|300|400|500|600|1000|10000|601|602|6033|604|605|606|607|608|609|610|911|912|913|914|915".split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "-?\\d{1,6}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getTypeQualifier(), "SIGNED");
		Assert.assertEquals(result.getMinValue(), "-100000");
		Assert.assertEquals(result.getMaxValue(), "10000");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void doublesWithSpaces() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input =
				" 0.56000537| 0.4644182| 0.53597438| 0.66897142| 0.58498305| 0.53236401| 0.57459098| 0.66013932| 0.52850509| 0.59274352| 0.63449258|" +
						" 0.53062689| 0.62101597| 0.54467571| 0.55982822| 0.55236143| 0.52536035| -9999| 0.60300124| 0.56447577| 0.52936405| 0.529791|" +
						" 0.66758442| 0.48754925| 0.53641158| 0.51493853| 0.50437933| 0.59817135| 0.5593698| 0.57516289| 0.51756728| 0.55499005| 0.50622934|" +
						" 0.61930555| 0.61162853| 0.62606031| 0.61473697| 0.58204252| 0.54705042| 0.59241235| 0.56348866| 0.63005126| 0.55131644| 0.59083325|" +
						" 0.6401121| 0.56351823| 0.55042273| 0.61309111| 0.50556493| 0.62455976| 0.58180296| 0.65038371| 0.5766986| 0.60995877| 0.57004404|" +
						" 0.68236881| 0.49527445| 0.59514475| 0.56920892| 0.61952943| 0.56751066| 0.56773728| 0.57786775| 0.56001669| 0.49182054| 0.61677784|" +
						" 0.61103219| 0.57788509| 0.5439918| 0.62966329| 0.52707005| 0.69334954| 0.59485507| 0.54186255| 0.56574053| 0.54190195| 0.50607115|" +
						" 0.48150891| 0.50916439| 0.56314766| 0.56764281| 0.51959276| 0.53935015| 0.56686914| 0.57895356| 0.577981| 0.48543748| 0.62048388|" +
						" 0.58613783| 0.52769667| 0.60062158| 0.61381048| 0.53795183| 0.56081986| 0.64281517| 0.53652996| 0.59506911| 0.56369746| 0.58791381|" +
						" 0.5822112| 0.50465667| 0.51010394| 0.54944164| 0.47499654| 0.59955311| 0.60433054| 0.53369552| 0.59793103| -9999| 0.58600241|";

		final String inputs[] = input.split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertEquals(result.getTypeQualifier(), "SIGNED");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*(-?\\d+|-?(\\d+)?\\.\\d+)");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getMinValue(), "-9999.0");
		Assert.assertEquals(result.getMaxValue(), "0.69334954");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void constantLengthInteger() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "456789|456089|456700|116789|433339|409187".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, -1);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{6}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getMinValue(), "116789");
		Assert.assertEquals(result.getMaxValue(), "456789");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void sum50() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		int locked = -1;
		final int COUNT = 50;
		int sum = 0;

		for (int i = 10000; i < 10000 + COUNT; i++) {
			sum += i;
			if (analysis.train(String.valueOf(i)) && locked != -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, -1);
		Assert.assertEquals(result.getSampleCount(), COUNT);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{5}");
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getMinValue(), "10000");
		Assert.assertEquals(result.getMaxValue(), "10049");
		Assert.assertEquals(result.getTypeQualifier(), "US_ZIP5");
		Assert.assertEquals(result.getConfidence(), 0.96);
		Assert.assertEquals(result.getSum(), String.valueOf(sum));

		for (int i = 10000; i < 10000 + COUNT; i++) {
			Assert.assertTrue(String.valueOf(i).matches(result.getRegExp()));
		}
	}

	@Test
	public void positiveDouble() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "43.80|1.1|0.1|2.03|.1|99.23|14.08976|14.085576|3.141592654|2.7818|1.414|2.713".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, -1);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_DOUBLE));
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getMinValue(), "0.1");
		Assert.assertEquals(result.getMaxValue(), "99.23");
		Assert.assertEquals(result.getMinLength(), 2);
		Assert.assertEquals(result.getMaxLength(), 11);


		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void positiveDouble2() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "43.80|1.1|0.1|2.03|0.1|99.23|14.08976|14.085576|3.141592654|2.7818|1.414|2.713".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, -1);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_DOUBLE));
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getMinValue(), "0.1");
		Assert.assertEquals(result.getMaxValue(), "99.23");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void dateOutlier() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		final String[] inputs = "12/12/12|12/12/32|02/22/02".split("\\|");
		int locked = -1;
		final int records = 100;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		for (int i = inputs.length; i < records; i++) {
			if (analysis.train("02/02/99") && locked == -1)
				locked = i;
		}

		// This record is bad 'O' not '0'
		analysis.train("02/O2/99");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), records + 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{2}/\\d{2}/\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "MM/dd/yy");
		Assert.assertEquals(result.getMinValue(), "02/22/02");
		Assert.assertEquals(result.getMaxValue(), "02/02/99");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void negativeDouble() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "43.80|-1.1|-.1|2.03|.1|-99.23|14.08976|-14.085576|3.141592654|2.7818|1.414|2.713".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, -1);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_SIGNED_DOUBLE));
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertEquals(result.getTypeQualifier(), "SIGNED");
		Assert.assertEquals(result.getMinValue(), "-99.23");
		Assert.assertEquals(result.getMaxValue(), "43.8");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicAMPMVietname() throws IOException {
		final Locale locale = Locale.forLanguageTag("vi-VN");
		final TextAnalyzer analysis = new TextAnalyzer();
		analysis.setLocale(locale);
		final String dateTimeFormat = "dd/MMM/yy h:mm a";
		final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateTimeFormat, locale);
		final int sampleCount = 100;
		final Set<String> samples = new HashSet<>();
		int locked = -1;

		LocalDateTime localDateTime = LocalDateTime.now();
		for (int i = 0; i < sampleCount; i++) {
			String sample = null;
			sample = localDateTime.format(dtf);
			samples.add(sample);
			if (analysis.train(sample) && locked == -1)
				locked = i;
			localDateTime = localDateTime.minusDays(100).minusHours(1).minusMinutes(1).minusSeconds(1);
		}
		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), sampleCount);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), sampleCount);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{2}/[\\p{IsAlphabetic}\\p{IsDigit} ]{5,6}/\\d{2} \\d{1,2}:\\d{2} (?i)(SA|CH)");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATETIME);
		Assert.assertEquals(result.getTypeQualifier(), "dd/MMM/yy h:mm a");

		for (String sample : samples) {
			Assert.assertTrue(sample.matches(result.getRegExp()), sample);
		}
	}

	@Test
	public void basicAMPM_enUS() throws IOException {
		final Locale locale = Locale.forLanguageTag("en-US");
		final TextAnalyzer analysis = new TextAnalyzer();
		analysis.setLocale(locale);
		final String dateTimeFormat = "dd/MMM/yy h:mm a";
		final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateTimeFormat, locale);
		final int sampleCount = 100;
		final Set<String> samples = new HashSet<>();
		int locked = -1;

		LocalDateTime localDateTime = LocalDateTime.now();
		for (int i = 0; i < sampleCount; i++) {
			String sample = null;
			sample = localDateTime.format(dtf);
			samples.add(sample);
			if (analysis.train(sample) && locked == -1)
				locked = i;
			localDateTime = localDateTime.minusDays(100).minusHours(1).minusMinutes(1).minusSeconds(1);
		}
		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), sampleCount);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), sampleCount);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{2}/\\p{IsAlphabetic}{3}/\\d{2} \\d{1,2}:\\d{2} (?i)(AM|PM)");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATETIME);
		Assert.assertEquals(result.getTypeQualifier(), "dd/MMM/yy h:mm a");

		for (String sample : samples) {
			Assert.assertTrue(sample.matches(result.getRegExp()), sample);
		}
	}

	@Test
	public void basicAMPM_enUSNotSimple() throws IOException {
		final Locale locale = Locale.forLanguageTag("en-US");
		final TextAnalyzer analysis = new TextAnalyzer("h:mm a dd/MM/yy", DateResolutionMode.DayFirst);
		analysis.setLocale(locale);
		final String dateTimeFormat = "h:mm a dd/MM/yy";
		final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateTimeFormat, locale);
		final int sampleCount = 100;
		final Set<String> samples = new HashSet<>();
		int locked = -1;

		LocalDateTime localDateTime = LocalDateTime.now();
		for (int i = 0; i < sampleCount; i++) {
			String sample = null;
			sample = localDateTime.format(dtf);
			samples.add(sample);
			if (analysis.train(sample) && locked == -1)
				locked = i;
			localDateTime = localDateTime.minusDays(100).minusHours(1).minusMinutes(1).minusSeconds(1);
		}
		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), sampleCount);
		Assert.assertEquals(result.getRegExp(), "\\d{1,2}:\\d{2} (?i)(AM|PM) \\d{2}/\\d{2}/\\d{2}");
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATETIME);
		Assert.assertEquals(result.getTypeQualifier(), "h:mm a dd/MM/yy");
		Assert.assertEquals(result.getMatchCount(), sampleCount);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (String sample : samples) {
			Assert.assertTrue(sample.matches(result.getRegExp()), sample);
		}
	}

	@Test
	public void basicAMPM_viVNNotSimple() throws IOException {
		final Locale locale = Locale.forLanguageTag("vi-VN");
		final TextAnalyzer analysis = new TextAnalyzer("h:mm a dd/MM/yy", DateResolutionMode.DayFirst);
		analysis.setLocale(locale);
		final String dateTimeFormat = "h:mm a dd/MM/yy";
		final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateTimeFormat, locale);
		final int sampleCount = 100;
		final Set<String> samples = new HashSet<>();
		int locked = -1;

		LocalDateTime localDateTime = LocalDateTime.now();
		for (int i = 0; i < sampleCount; i++) {
			String sample = null;
			sample = localDateTime.format(dtf);
			samples.add(sample);
			if (analysis.train(sample) && locked == -1)
				locked = i;
			localDateTime = localDateTime.minusDays(100).minusHours(1).minusMinutes(1).minusSeconds(1);
		}
		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), sampleCount);
		Assert.assertEquals(result.getRegExp(), "\\d{1,2}:\\d{2} (?i)(SA|CH) \\d{2}/\\d{2}/\\d{2}");
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATETIME);
		Assert.assertEquals(result.getTypeQualifier(), "h:mm a dd/MM/yy");
		Assert.assertEquals(result.getMatchCount(), sampleCount);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (String sample : samples) {
			Assert.assertTrue(sample.matches(result.getRegExp()), sample);
		}
	}

	@Test
	public void basicAMPMBug() throws IOException {
		final Locale locale = Locale.forLanguageTag("en-US");
		final TextAnalyzer analysis = new TextAnalyzer();
		analysis.setLocale(locale);
		final String dateTimeFormat = "h:mm 'a'";
		final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateTimeFormat, locale);
		final int sampleCount = 100;
		final Set<String> samples = new HashSet<>();
		int locked = -1;

		LocalDateTime localDateTime = LocalDateTime.now();
		for (int i = 0; i < sampleCount; i++) {
			String sample = null;
			sample = localDateTime.format(dtf);
			samples.add(sample);
			if (analysis.train(sample) && locked == -1)
				locked = i;
			localDateTime = localDateTime.minusDays(100).minusHours(1).minusMinutes(1).minusSeconds(1);
		}
		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), sampleCount);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), sampleCount);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), ".{6,7}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());

		for (String sample : samples) {
			Assert.assertTrue(sample.matches(result.getRegExp()), sample);
		}
	}

	@Test
	public void basicHHmmddMyy() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("TransactionDate", DateResolutionMode.DayFirst);
		final String input = "00:53 15/2/17|17:53 29/7/16|10:53 11/1/16|03:53 25/6/15|20:53 06/12/14|13:53 20/5/14|06:53 01/11/13|23:53 14/4/13|" +
				"16:53 26/9/12|09:53 10/3/12|02:53 23/8/11|19:53 03/2/11|12:53 18/7/10|05:53 30/12/09|22:53 12/6/09|15:53 24/11/08|" +
				"08:53 08/5/08|01:53 21/10/07|18:53 03/4/07|11:53 15/9/06|04:53 27/2/06|21:53 10/8/05|14:53 22/1/05|07:53 06/7/04|" +
				"00:53 19/12/03|17:53 01/6/03|10:53 13/11/02|03:53 27/4/02|20:53 08/10/01|13:53 22/3/01|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{2}:\\d{2} \\d{2}/\\d{1,2}/\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATETIME);
		Assert.assertEquals(result.getTypeQualifier(), "HH:mm dd/M/yy");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicHHmmddMyyUnresolved() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("TransactionDate");
		final String input = "00:53 15/2/17|17:53 29/7/16|10:53 11/1/16|03:53 25/6/15|20:53 06/12/14|13:53 20/5/14|06:53 01/11/13|23:53 14/4/13|" +
				"16:53 26/9/12|09:53 10/3/12|02:53 23/8/11|19:53 03/2/11|12:53 18/7/10|05:53 30/12/09|22:53 12/6/09|15:53 24/11/08|" +
				"08:53 08/5/08|01:53 21/10/07|18:53 03/4/07|11:53 15/9/06|04:53 27/2/06|21:53 10/8/05|14:53 22/1/05|07:53 06/7/04|" +
				"00:53 19/12/03|17:53 01/6/03|10:53 13/11/02|03:53 27/4/02|20:53 08/10/01|13:53 22/3/01|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{2}:\\d{2} \\d{2}/\\d{1,2}/\\d{2}");
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATETIME);
		Assert.assertEquals(result.getTypeQualifier(), "HH:mm ??/M/??");
		Assert.assertEquals(result.getMatchCount(), 0);
		Assert.assertEquals(result.getConfidence(), 0.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicHHmmddMyyFalse() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("TransactionDate", DateResolutionMode.MonthFirst);
		final String input = "00:53 15/2/17|17:53 29/7/16|10:53 11/1/16|03:53 25/6/15|20:53 06/12/14|13:53 20/5/14|06:53 01/11/13|23:53 14/4/13|" +
				"16:53 26/9/12|09:53 10/3/12|02:53 23/8/11|19:53 03/2/11|12:53 18/7/10|05:53 30/12/09|22:53 12/6/09|15:53 24/11/08|" +
				"08:53 08/5/08|01:53 21/10/07|18:53 03/4/07|11:53 15/9/06|04:53 27/2/06|21:53 10/8/05|14:53 22/1/05|07:53 06/7/04|" +
				"00:53 19/12/03|17:53 01/6/03|10:53 13/11/02|03:53 27/4/02|20:53 08/10/01|13:53 22/3/01|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getTypeQualifier(), "HH:mm dd/M/yy");
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{2}:\\d{2} \\d{2}/\\d{1,2}/\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATETIME);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void dMyy() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("TransactionDate", DateResolutionMode.DayFirst);
		final String input = "1/2/09 6:17|1/2/09 4:53|1/2/09 13:08|1/3/09 14:44|1/4/09 12:56|1/4/09 13:19|1/4/09 20:11|1/2/09 20:09|1/4/09 13:17|1/4/09 14:11|" +
				"1/5/09 2:42|1/5/09 5:39|1/2/09 9:16|1/5/09 10:08|1/2/09 14:18|1/4/09 1:05|1/5/09 11:37|1/6/09 5:02|1/6/09 7:45|1/2/09 7:35|" +
				"1/6/09 12:56|1/1/09 11:05|1/5/09 4:10|1/6/09 7:18|1/2/09 1:11|1/1/09 2:24|1/7/09 8:08|1/2/09 2:57|1/1/09 20:21|1/8/09 0:42|" +
				"1/8/09 3:56|1/8/09 3:16|1/8/09 1:59|1/3/09 9:03|1/5/09 13:17|1/6/09 7:46|1/5/09 20:00|1/8/09 16:24|1/9/09 6:39|1/6/09 22:19|" +
				"1/6/09 23:00|1/7/09 7:44|1/3/09 13:24|1/7/09 15:12|1/7/09 20:15|1/3/09 10:11|1/9/09 15:58|1/3/09 13:11|1/10/09 12:57|1/10/09 14:43|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{1,2}/\\d{1,2}/\\d{2} \\d{1,2}:\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATETIME);
		Assert.assertEquals(result.getTypeQualifier(), "d/M/yy H:mm");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicMdyy() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Account_Created", DateResolutionMode.DayFirst);
		final String input = "1/2/09 6:00|1/2/09 4:42|1/1/09 16:21|9/25/05 21:13|11/15/08 15:47|9/24/08 15:19|1/3/09 9:38|1/2/09 17:43|1/4/09 13:03|6/3/08 4:22|" +
				"1/5/09 2:23|1/5/09 4:55|1/2/09 8:32|11/11/08 15:53|12/9/08 12:07|1/4/09 0:00|1/5/09 9:35|1/6/09 2:41|1/6/09 7:00|12/30/08 5:44|" +
				"1/6/09 10:58|12/10/07 12:37|1/5/09 2:33|1/6/09 7:07|12/31/08 2:48|1/1/09 1:56|1/7/09 7:39|1/3/08 7:23|10/24/08 6:48|1/8/09 0:28|" +
				"1/8/09 3:33|1/8/09 3:06|11/28/07 11:56|1/3/09 8:47|1/5/09 12:45|1/6/09 7:30|12/10/08 19:53|1/8/09 15:57|1/9/09 5:09|1/6/09 12:00|" +
				"1/6/09 12:00|1/7/09 5:35|1/3/09 12:47|1/5/09 19:55|1/7/09 17:17|1/3/09 9:27|1/9/09 14:53|10/31/08 0:31|2/7/07 20:16|1/9/09 11:38|" +
				"7/1/08 12:53|1/2/09 17:51|1/8/09 3:06|1/7/09 9:04|1/1/09 0:58|1/11/09 0:33|1/11/09 13:39|1/10/09 21:17|2/23/06 11:56|1/3/09 17:17|" +
				"1/4/09 23:45|12/30/08 10:21|1/12/09 3:12|11/29/07 9:23|12/1/08 6:25|1/12/09 4:45|1/8/09 14:56|1/11/09 9:02|11/23/08 19:30|1/2/09 21:04|" +
				"1/5/09 5:55|1/8/09 20:21|1/6/09 14:38|1/11/09 12:38|9/20/08 20:53|1/12/09 13:16|9/4/08 9:26|10/15/08 5:31|1/13/09 5:17|1/8/09 12:47|" +
				"1/1/09 12:00|1/3/09 12:04|1/1/09 20:11|2/20/08 22:45|1/12/09 2:48|9/1/08 3:39|3/13/06 4:56|1/13/09 11:15|12/5/07 11:37|1/14/09 4:34|" +
				"12/24/08 17:31|1/3/09 12:30|10/21/05 21:49|12/10/08 16:41|1/6/09 14:59|1/13/09 23:13|12/16/08 22:19|1/7/08 21:16|1/14/09 10:44|1/6/09 22:00|" +
				"1/25/08 15:46|1/15/09 2:04|1/10/09 12:50|12/26/08 9:01|12/10/08 9:14|1/10/09 0:07|1/15/09 4:02|7/24/08 15:48|8/21/07 21:19|1/14/09 8:25|" +
				"1/14/09 22:21|7/21/08 12:03|12/16/02 4:09|1/14/09 9:16|1/14/09 5:59|1/7/09 8:39|1/15/09 12:19|1/13/09 17:42|1/13/09 19:37|6/5/06 22:42|" +
				"1/12/09 1:26|11/3/08 14:28|1/15/09 12:22|6/30/08 9:33|12/31/08 16:26|1/16/09 17:01|1/5/09 3:19|1/2/09 0:10|1/12/09 1:40|1/13/09 12:30|" +
				"11/9/08 5:53|11/11/05 11:49|1/16/06 14:45|5/1/08 8:26|1/7/09 12:31|10/29/08 13:09|9/13/07 14:20|1/4/09 6:51|1/17/09 15:22|9/14/05 22:53|" +
				"1/18/09 4:28|1/18/09 4:44|12/14/05 15:46|6/22/08 14:10|12/6/08 1:34|12/31/05 0:55|1/16/09 14:26|1/16/09 19:32|10/28/05 14:57|12/28/07 13:39|" +
				"1/18/09 12:20|1/18/09 0:00|1/18/09 0:00|1/3/06 6:09|1/5/09 16:27|4/12/06 6:14|1/4/09 22:41|1/18/09 16:23|1/18/09 16:58|1/18/09 17:16|" +
				"1/18/09 17:34|12/31/08 19:48|1/13/09 16:44|1/4/09 21:33|1/12/09 14:07|1/6/09 12:04|11/25/08 7:56|1/18/09 15:18|1/3/09 11:06|12/29/08 10:38|" +
				"11/7/07 9:49|5/25/07 10:58|1/3/09 12:39|9/25/05 6:25|1/11/09 18:17|1/12/09 13:31|12/13/08 19:35|12/13/08 19:35|12/9/05 17:19|1/19/09 15:31|" +
				"1/19/09 16:18|1/10/09 16:32|1/11/09 12:00|1/15/09 0:21|1/19/09 22:53|8/27/08 8:22|7/15/08 21:04|1/20/09 3:02|4/18/05 9:27|1/20/09 5:13|" +
				"1/20/09 5:13|12/28/08 20:10|1/20/09 4:23|1/19/09 10:24|1/18/09 0:00|1/7/09 5:30|10/10/08 1:26|3/12/06 11:31|1/20/09 12:17|11/12/08 3:34|" +
				"7/11/07 11:18|9/29/03 17:08|12/10/08 21:49|1/4/09 18:38|2/4/07 10:09|1/7/09 18:15|1/15/09 18:01|11/1/08 16:32|8/5/07 18:52|1/5/09 14:06|" +
				"1/9/09 19:15|1/5/09 15:42|1/4/09 22:22|12/23/08 12:02|1/21/09 12:00|5/3/08 4:14|1/19/09 16:52|10/27/05 19:22|11/1/08 23:14|1/2/08 9:52|" +
				"2/28/05 5:40|1/21/09 10:23|1/7/09 13:23|12/30/08 17:48|12/28/07 15:28|1/18/09 16:25|1/21/09 11:39|1/22/09 4:32|1/10/09 14:55|1/22/09 7:55|" +
				"8/7/08 9:24|9/17/05 3:32|1/4/09 11:32|1/8/09 2:50|6/19/08 10:48|1/14/09 4:10|7/23/08 14:59|12/13/08 19:20|1/5/09 15:00|4/18/06 13:26|" +
				"1/23/09 3:00|1/6/09 3:38|1/23/09 3:27|12/20/08 8:41|12/29/08 3:16|1/21/09 13:56|7/30/07 21:10|1/4/09 12:39|1/22/09 9:55|1/22/09 23:23|" +
				"4/25/07 5:08|1/16/09 1:38|8/3/07 2:48|4/7/08 17:15|1/23/09 6:32|1/5/09 15:43|1/24/09 8:02|1/14/09 4:13|11/28/07 10:05|1/23/09 10:42|" +
				"1/19/09 14:43|3/7/06 5:47|11/24/08 15:50|12/17/07 19:55|12/7/05 19:48|6/20/08 22:08|6/14/07 13:14|6/14/07 13:14|1/17/06 8:51|5/14/07 12:48|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATETIME);
		Assert.assertEquals(result.getTypeQualifier(), "M/d/yy H:mm");
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{1,2}/\\d{1,2}/\\d{2} \\d{1,2}:\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicDateNumber() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Trade Date", DateResolutionMode.DayFirst);
		final String input = "20120202|20120607|20120627|20120627|20120627|20120627|20120627|20120628|20120312|20120201|" +
		"20111031|20120229|20120104|20120312|20120312|20120628|20120628|20120628|20120628|20120628|" +
		"20111027|20120213|20120628|20120227|20120313|20120701|20120702|20120702|20120701|20120701|" +
		"20120629|20120629|20120629|20120629|20120629|20120629|20120629|20120629|20120629|20120629|" +
		"20120629|20120702|20120702|20120702|20120702|20120702|20120702|20120702|20120702|20120702|" +
		"20120702|20120702|20120702|20120702|20120713|20120713|20120713|20120713|20120713|20120713|" +
		"20120430|20120523|20120627|20120627|20120606|20120703|20120718|20120718|20120703|20120703|" +
		"20120523|20120627|20120627|20120703|20120503|20120718|20120926|20120523|20120626|20120713|" +
		"20120713|20120626|20120626|20121004|20120702|20120702|20120702|20120702|20120702|20120702|" +
		"20120702|20120702|20120702|20120702|20120702|20120702|19591209|20120702|20120702|20120702|" +
		"20120702|20120702|20120702|20120702|20120702|20120702|20120702|20120516|20120518|20120521|" +
		"20120522|20120523|20120523|20120524|20120524|20120525|20120525|20120525|20120528|20120529|" +
		"20120529|20120531|20120601|20120601|20120605|20120606|20120608|20120611|20120613|20120618|" +
		"20120620|20120625|20120625|20120702|20120702|20120702|20120702|20120629|20120628|20120702|" +
		"20120629|20120629|20120518|20120702|20120702|20120702|20120702|20120702|20120702|20120702|" +
		"20120702|20120702|20120702|20120629|20120629|20120702|20120629|20120629|20120629|20120702|" +
		"20120702|20120702|20120629|20120629|20120702|20120629|20120626|20120702|20120702|20120702|" +
		"20120702|20120702|20120702|20120702|20120702|20120702|20120702|20120702|20120702|20120702|" +
		"20120702|20120702|20120702|20120702|20120702|20120702|20120629|20120702|20180519|20120702|";

		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "yyyyMMdd");
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{8}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getMinValue(), "19591209");
		Assert.assertEquals(result.getMaxValue(), "20180519");

		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(result.getTypeQualifier());
		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()), inputs[i]);
			try {
				LocalDate.parse(inputs[i], formatter);
			}
			catch (DateTimeParseException e) {
				Assert.fail("Parse failed" + e);
			}
		}
	}

	@Test
	public void basicStateSpaces() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("State", DateResolutionMode.DayFirst);
		final String input = " AL| AK| AZ| KY| KS| LA| ME| MD| MI| MA| MN| MS|MO |NE| MT| SD|TN | TX| UT| VT| WI|" +
						" VA| WA| WV| HI| ID| IL| IN| IA| KS| KY| LA| ME| MD| MA| MI| MN| MS| MO| MT| NE| NV|" +
						" NH| NJ| NM| NY| NC| ND| OH| OK| OR| PA| RI| SC| SD| TN| TX| UT| VT| VA| WA|  WV | WI|" +
						" WY| AL| AK| AZ| AR| CA| CO| CT| DC| DE| FL| GA| HI| ID| IL| IN| IA| KS| KY| LA|SA|" +
						" MD| MA| MI| MN| MS| MO| MT| NE| NV| NH| NJ| NM| NY| NC| ND| OH| OK| OR| RI| SC| SD|" +
						" TX| UT| VT| WV| WI| WY| NV| NH| NJ| OR| PA| RI| SC| AR| CA| CO| CT| ID| HI| IL| IN|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "US_STATE");
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\p{Alpha}{2}");
		final Map<String, Integer> outliers = result.getOutlierDetails();
		Assert.assertEquals(outliers.get("SA"), Integer.valueOf(1));
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].trim().matches(result.getRegExp()), inputs[i]);
		}
	}

	@Test
	public void basicGender() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Gender");
		final String input = "Female|MALE|Male|Female|Female|MALE|Female|Female|Unknown|Male|" +
				"Male|Female|Male|Male|Male|Female|Female|Male|Male|Male|" +
				"Female|Male|Female|FEMALE|Male|Female|male|Male|Male|male|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "GENDER_EN");
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\p{Alpha}+");
		final Map<String, Integer> outliers = result.getOutlierDetails();
		int outlierCount = outliers.get("UNKNOWN");
		Assert.assertEquals(result.getMatchCount(), inputs.length - outlierCount);
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()), inputs[i]);
		}
	}

	@Test
	public void basicGenderTriValue() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Gender");
		final String input = "Female|MALE|Male|Female|Female|MALE|Female|Female|Unknown|Male|" +
				"Male|Female|Male|Male|Male|Female|Female|Male|Male|Male|" +
				"Unknown|Female|Unknown|Male|Unknown|Female|Unknown|Male|Unknown|Male|" +
				"Female|Male|Female|FEMALE|Male|Female|male|Male|Male|male|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "GENDER_EN");
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\p{Alpha}+");
		final Map<String, Integer> outliers = result.getOutlierDetails();
		int outlierCount = outliers.get("UNKNOWN");
		Assert.assertEquals(result.getMatchCount(), inputs.length - outlierCount);
		Assert.assertEquals(result.getConfidence(), 1 - (double)outlierCount/result.getSampleCount());

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()), inputs[i]);
		}
	}

	@Test
	public void basicGenderNoDefaults() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Gender");
		analysis.setDefaultLogicalTypes(false);

		final String input = "Female|MALE|Male|Female|Female|MALE|Female|Female|Unknown|Male|" +
				"Male|Female|Male|Male|Male|Female|Female|Male|Male|Male|" +
				"Female|Male|Female|FEMALE|Male|Female|male|Male|Male|male|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{4,7}");
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()), inputs[i]);
		}
	}


	@Test
	public void basicMMddyy() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("DOB");
		final String input = "12/5/59|2/13/48|6/29/62|1/7/66|7/3/84|5/28/74|" +
				"|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{1,2}/\\d{1,2}/\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "M/d/yy");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void yyyyMddHHmm() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "2017-4-15 21:10|2016-9-27 14:10|2016-3-11 07:10|2015-8-24 00:10|2015-2-04 17:10|" +
				"2014-7-19 10:10|2013-12-31 03:10|2013-6-13 20:10|2012-11-25 13:10|2012-5-09 06:10|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{4}-\\d{1,2}-\\d{2} \\d{2}:\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATETIME);
		Assert.assertEquals(result.getTypeQualifier(), "yyyy-M-dd HH:mm");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void Hmmss() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "21:53:12|12:53:12|3:53:12|18:53:12|9:53:12|0:53:12|15:53:12|6:53:12|21:53:12|12:53:12|3:53:12|18:53:12|9:53:12|0:53:12|15:53:12|" +
				"6:53:12|21:53:12|12:53:12|3:53:12|18:53:12|9:53:12|0:53:12|15:53:12|6:53:12|21:53:12|12:53:12|3:53:12|18:53:12|9:53:12|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{1,2}:\\d{2}:\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALTIME);
		Assert.assertEquals(result.getTypeQualifier(), "H:mm:ss");
		Assert.assertEquals(analysis.getTrainingSet(), Arrays.asList(Arrays.copyOfRange(inputs, 0, analysis.getSampleSize())));

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void yyyyMMddHHmmz() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "2017-08-24 12:10 EDT|2017-07-03 06:10 EDT|2017-05-12 00:10 EDT|2017-03-20 18:10 EDT|2016-07-02 12:10 EDT|" +
				"2017-01-27 11:10 EST|2016-12-06 05:10 EST|2016-10-15 00:10 EDT|2016-08-23 18:10 EDT|2016-05-11 06:10 EDT|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2} .*");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.ZONEDDATETIME);
		Assert.assertEquals(result.getTypeQualifier(), "yyyy-MM-dd HH:mm z");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void dateYYYYMMDD() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "2010-01-22|2019-01-12|1996-01-02|1916-01-02|1993-01-02|1998-01-02|2001-01-02|2000-01-14|2008-01-12".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "yyyy-MM-dd");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void dateYYYY() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "2015|2015|2015|2015|2015|2015|2015|2016|2016|2016|2013|1932|1991|1993|2001|1977|2001|1976|1972|" +
				"1982|2005|1950|1961|1967|1997|1967|1996|2014|2002|1953|1980|2010|2010|1979|1980|1983|1974|1970|" +
				"1978|2014|2015|1979|1982|2016|2016|2013|2011|1986|1985|2000|2000|2012|2000|2000|";
		final String[] inputs = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "yyyy");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}

		final TextAnalyzer analysis2 = new TextAnalyzer();

		for (int i = 0; i < inputs.length; i++) {
			if (analysis2.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result2 = analysis2.getResult();

		Assert.assertEquals(result2.getSampleCount(), inputs.length);
		Assert.assertEquals(result2.getMatchCount(), inputs.length);
		Assert.assertEquals(result2.getNullCount(), 0);
		Assert.assertEquals(result2.getRegExp(), "\\d{4}");
		Assert.assertEquals(result2.getConfidence(), 1.0);
		Assert.assertEquals(result2.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result2.getTypeQualifier(), "yyyy");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result2.getRegExp()));
		}

	}

	@Test
	public void basicDateDMMMYY() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "1-Jan-14|2-Jan-14|3-Jan-14|6-Jan-14|7-Jan-14|7-Jan-14|8-Jan-14|9-Jan-14|10-Jan-14|" +
				"13-Jan-14|14-Jan-14|15-Jan-14|16-Jan-14|17-Jan-14|20-Jan-14|21-Jan-14|22-Jan-14|" +
				"23-Jan-14|24-Jan-14|27-Jan-14|28-Jan-14|29-Jan-14|30-Jan-14|31-Jan-14|3-Feb-14|" +
				"4-Feb-14|5-Feb-14|6-Feb-14|7-Feb-14|10-Feb-14|11-Feb-14|12-Feb-14|13-Feb-14|14-Feb-14|" +
				"17-Feb-14|18-Feb-14|19-Feb-14|20-Feb-14|21-Feb-14|24-Feb-14|25-Feb-14|26-Feb-14|27-Feb-14|" +
				"28-Feb-14|3-Mar-14|4-Mar-14|5-Mar-14|";
		final String[] inputs = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "d-MMM-yy");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{1,2}-" + KnownPatterns.PATTERN_ALPHA + "{3}-\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicMMMM_d_yyyy() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "September-17-2014|September-11-2011|September-4-2008|August-29-2005|August-23-2002|August-17-1999|" +
				"August-10-1996|August-4-1993|July-29-1990|July-23-1987|July-16-1984|July-10-1981|July-4-1978|June-28-1975|" +
				"June-21-1972|June-15-1969|June-9-1966|June-3-1963|May-27-1960|May-21-1957|May-15-1954|May-9-1951|May-2-1948|" +
				"April-26-1945|April-20-1942|April-14-1939|April-7-1936|April-1-1933|March-26-1930|March-20-1927";
		final String[] inputs = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "MMMM-d-yyyy");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{3,9}-\\d{1,2}-\\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void startsAsTwoDigitDay() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input =
				"27/6/2012 12:46:03|27/6/2012 15:29:48|27/6/2012 23:32:22|27/6/2012 23:38:51|27/6/2012 23:42:22|" +
						"27/6/2012 23:49:13|27/6/2012 23:56:02|28/6/2012 08:04:51|28/6/2012 15:53:00|28/6/2012 16:46:34|" +
						"28/6/2012 17:01:01|28/6/2012 17:53:52|28/6/2012 18:03:31|28/6/2012 18:31:14|28/6/2012 18:46:12|" +
						"28/6/2012 23:32:08|28/6/2012 23:44:54|28/6/2012 23:47:48|28/6/2012 23:51:32|28/6/2012 23:53:36|" +
						"29/6/2012 08:54:18|29/6/2012 08:56:53|29/6/2012 11:21:56|29/6/2012 16:48:14|29/6/2012 16:56:32|" +
						"1/7/2012 09:15:03|1/7/2012 15:36:44|1/7/2012 18:25:35|1/7/2012 18:31:19|1/7/2012 18:36:04|" +
						"1/7/2012 19:13:17|1/7/2012 19:13:35|1/7/2012 19:13:49|1/7/2012 19:14:07|1/7/2012 19:14:21|" +
						"1/7/2012 19:14:29|1/7/2012 19:16:45|1/7/2012 19:17:48|1/7/2012 19:18:19|1/7/2012 19:19:09|";
		final String[] inputs = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATETIME);
		Assert.assertEquals(result.getTypeQualifier(), "d/M/yyyy HH:mm:ss");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{1,2}/\\d{1,2}/\\d{4} \\d{2}:\\d{2}:\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void startsAsTwoDigitMonth() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input =
				"27/10/2012 12:46:03|27/10/2012 15:29:48|27/10/2012 23:32:22|27/10/2012 23:38:51|27/10/2012 23:42:22|" +
						"27/10/2012 23:49:13|27/10/2012 23:56:02|28/10/2012 08:04:51|28/10/2012 15:53:00|28/10/2012 16:46:34|" +
						"28/10/2012 17:01:01|28/10/2012 17:53:52|28/10/2012 18:03:31|28/10/2012 18:31:14|28/10/2012 18:46:12|" +
						"28/10/2012 23:32:08|28/10/2012 23:44:54|28/10/2012 23:47:48|28/10/2012 23:51:32|28/10/2012 23:53:36|" +
						"29/10/2012 08:54:18|29/10/2012 08:56:53|29/10/2012 11:21:56|29/10/2012 16:48:14|29/10/2012 16:56:32|" +
						"10/7/2012 09:15:03|1/7/2012 15:36:44|1/7/2012 18:25:35|1/7/2012 18:31:19|1/7/2012 18:36:04|" +
						"1/7/2012 19:13:17|1/7/2012 19:13:35|1/7/2012 19:13:49|1/7/2012 19:14:07|1/7/2012 19:14:21|" +
						"1/7/2012 19:14:29|1/7/2012 19:16:45|1/7/2012 19:17:48|1/7/2012 19:18:19|1/7/2012 19:19:09|";
		final String[] inputs = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATETIME);
		Assert.assertEquals(result.getTypeQualifier(), "d/M/yyyy HH:mm:ss");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{1,2}/\\d{1,2}/\\d{4} \\d{2}:\\d{2}:\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicddMMyyHHmm() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("ddMMyy_HHmm", DateResolutionMode.DayFirst);
		final String input =
				"23/08/17 03:49|23/08/17 03:49|14/08/17 10:49|23/08/17 03:49|14/08/17 10:49|05/08/17 17:49|23/08/17 03:49|14/08/17 10:49|05/08/17 17:49|" +
				"28/07/17 00:49|23/08/17 03:49|14/08/17 10:49|05/08/17 17:49|28/07/17 00:49|19/07/17 07:49|23/08/17 03:49|14/08/17 10:49|05/08/17 17:49|" +
				"28/07/17 00:49|19/07/17 07:49|10/07/17 14:49|23/08/17 03:49|14/08/17 10:49|05/08/17 17:49|28/07/17 00:49|19/07/17 07:49|10/07/17 14:49|" +
				"01/07/17 21:49|23/08/17 03:49|14/08/17 10:49|05/08/17 17:49|28/07/17 00:49|19/07/17 07:49|10/07/17 14:49|01/07/17 21:49|23/06/17 04:49|" +
				"23/08/17 03:49|14/08/17 10:49|05/08/17 17:49|28/07/17 00:49|19/07/17 07:49|10/07/17 14:49|01/07/17 21:49|23/06/17 04:49|14/06/17 11:49|" +
				"23/08/17 03:49|14/08/17 10:49|05/08/17 17:49|28/07/17 00:49|19/07/17 07:49|10/07/17 14:49|01/07/17 21:49|23/06/17 04:49|14/06/17 11:49|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATETIME);
		Assert.assertEquals(result.getTypeQualifier(), "dd/MM/yy HH:mm");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{2}/\\d{2}/\\d{2} \\d{2}:\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getMinValue(), "14/06/17 11:49");
		Assert.assertEquals(result.getMaxValue(), "23/08/17 03:49");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicHMM() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("H:mm");
		final String input = "3:16|3:16|10:16|3:16|10:16|17:16|3:16|10:16|17:16|0:16|3:16|10:16|17:16|0:16|7:16|3:16|10:16|" +
		"17:16|0:16|7:16|14:16|3:16|10:16|17:16|0:16|7:16|14:16|21:16|3:16|10:16|17:16|0:16|7:16|14:16|" +
		"21:16|4:16|3:16|10:16|17:16|0:16|7:16|14:16|21:16|4:16|11:16|3:16|10:16|17:16|0:16|7:16|14:16|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALTIME);
		Assert.assertEquals(result.getTypeQualifier(), "H:mm");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{1,2}:\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getMinValue(), "0:16");
		Assert.assertEquals(result.getMaxValue(), "21:16");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicDateDDMMMYYYHHMM() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input =
				"1/30/06 22:01|1/30/06 22:15|1/30/06 22:25|1/30/06 22:35|1/30/06 22:40|1/30/06 22:45|1/30/06 22:47|1/30/06 23:00|1/30/06 23:00|1/30/06 23:11|" +
						"1/30/06 23:15|1/30/06 23:21|1/30/06 23:31|1/30/06 23:52|1/30/06 23:55|1/30/06 23:58|1/31/06 0:00|1/31/06 0:00|1/31/06 0:00|1/31/06 0:01|" +
						"1/31/06 0:01|1/31/06 0:01|1/31/06 0:01|1/31/06 0:01|1/31/06 0:01|1/31/06 0:01|1/31/06 0:01|1/31/06 0:17|1/31/06 0:26|1/31/06 0:30|" +
						"1/31/06 0:30|1/31/06 0:30|1/31/06 0:47|1/31/06 0:56|1/31/06 1:21|1/31/06 1:34|1/31/06 1:49|1/31/06 2:00|1/31/06 2:08|1/31/06 2:11|1/31/06 2:22|" +
						"1/31/06 2:48|1/31/06 3:05|1/31/06 3:05|1/31/06 3:30|";
		final String[] inputs = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATETIME);
		Assert.assertEquals(result.getTypeQualifier(), "M/dd/yy H:mm");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{1,2}/\\d{2}/\\d{2} \\d{1,2}:\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getMinValue(), "1/30/06 22:01");
		Assert.assertEquals(result.getMaxValue(), "1/31/06 3:30");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void slashLoop() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("thin", DateResolutionMode.MonthFirst);
		final String input = "1/1/06 0:00";
		final int iterations = 30;
		int locked = -1;

		for (int iters = 0; iters < iterations; iters++) {
			if (analysis.train(input) && locked == -1)
				locked = iters;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATETIME);
		Assert.assertEquals(result.getTypeQualifier(), "M/d/yy H:mm");
		Assert.assertEquals(result.getSampleCount(), iterations);
		Assert.assertEquals(result.getMatchCount(), iterations);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{1,2}/\\d{1,2}/\\d{2} \\d{1,2}:\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getMinValue(), "1/1/06 0:00");
		Assert.assertEquals(result.getMaxValue(), "1/1/06 0:00");
		Assert.assertTrue(input.matches(result.getRegExp()));
	}

	@Test
	public void basicDateDDMMMYYY() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "22 Jan 1971|12 Mar 2019|02 Jun 1996|11 Dec 1916|19 Apr 1993|26 Sep 1998|09 Dec 1959|14 Jul 2000|18 Aug 2008".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{2} " + KnownPatterns.PATTERN_ALPHA + "{3} \\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "dd MMM yyyy");
		Assert.assertEquals(result.getMinValue(), "11 Dec 1916");
		Assert.assertEquals(result.getMaxValue(), "12 Mar 2019");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}


	//	put(Pattern.compile("^(?i)\\d{1,2}\\s[a-z]{3}\\s\\d{4}$").matcher(""), "dd MMM yyyy");
	//	put(Pattern.compile("^(?i)\\d{1,2}\\s[a-z]{4,}\\s\\d{4}$").matcher(""), "dd MMMM yyyy");

	@Test
	public void slashDateYYYYMMDD() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "2010/01/22|2019/01/12|1996/01/02|1916/01/02|1993/01/02|1998/01/02|2001/01/02|2000/01/14|2008/01/12".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{4}/\\d{2}/\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "yyyy/MM/dd");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicDateDDMMYYYY() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "22-01-2010|12-01-2019|02-01-1996|02-01-1916|02-01-1993|02-01-1998|02-01-2001|14-01-2000|12-01-2008".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{2}-\\d{2}-\\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "dd-MM-yyyy");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void variableDateDDMMYYYY() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "22-1-2010|12-1-2019|2-1-1996|2-1-1916|2-1-1993|2-1-1998|22-11-2001|14-1-2000|12-5-2008".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{1,2}-\\d{1,2}-\\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "d-M-yyyy");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}

	}

	@Test
	public void slashDateDDMMYYYY() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "22/01/2010|12/01/2019|02/01/1996|02/01/1916|02/01/1993|02/01/1998|02/01/2001|14/01/2000|12/01/2008".split("\\|");
		final int iterations = 4;
		int locked = -1;

		for (int iters = 0; iters < iterations; iters++) {
			for (int i = 0; i < inputs.length; i++) {
				if (analysis.train(inputs[i]) && locked == -1)
					locked = i;
			}
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length * iterations);
		Assert.assertEquals(result.getMatchCount(), inputs.length * iterations);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{2}/\\d{2}/\\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "dd/MM/yyyy");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void slashDateDDMMYY() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "22/01/70|12/01/03|02/01/66|02/01/46|02/01/93|02/01/78|02/01/74|14/01/98|12/01/34".split("\\|");
		final int iterations = 4;
		int locked = -1;

		for (int iters = 0; iters < iterations; iters++) {
			for (int i = 0; i < inputs.length; i++) {
				if (analysis.train(inputs[i]) && locked == -1)
					locked = i;
			}
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length * iterations);
		Assert.assertEquals(result.getMatchCount(), inputs.length * iterations);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{2}/\\d{2}/\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "dd/MM/yy");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void slashDateAmbiguousMMDDYY() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("thin", DateResolutionMode.MonthFirst);
		final String input = " 04/03/13";
		final int iterations = 30;
		int locked = -1;

		for (int iters = 0; iters < iterations; iters++) {
			if (analysis.train(input) && locked == -1)
				locked = iters;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), iterations);
		Assert.assertEquals(result.getMatchCount(), iterations);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*\\d{2}/\\d{2}/\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "MM/dd/yy");

		Assert.assertTrue(input.matches(result.getRegExp()));
	}

	@Test
	public void basicTimeHHMMSS() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "00:10:00|00:10:00|23:07:00|06:07:00|16:07:00|06:37:00|06:07:00|06:09:00|06:20:00|06:57:00".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{2}:\\d{2}:\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALTIME);
		Assert.assertEquals(result.getTypeQualifier(), "HH:mm:ss");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicTimeHHMM() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "00:10|00:10|23:07|06:07|16:07|06:37|06:07|06:09|06:20|06:57".split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{2}:\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALTIME);
		Assert.assertEquals(result.getTypeQualifier(), "HH:mm");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void limitedData() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String[] inputs = "12|4|5|".split("\\|");
		final int pre = 3;
		final int post = 10;

		for (int i = 0; i < pre; i++)
			analysis.train("");
		for (int i = 0; i < inputs.length; i++) {
			analysis.train(inputs[i]);
		}
		for (int i = 0; i < post; i++)
			analysis.train("");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), pre + inputs.length + post);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{1,2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

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

	@Test
	public void manyNulls() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int iterations = 50;

		for (int i = 0; i < iterations; i++) {
			analysis.train(null);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), iterations);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), iterations);
		Assert.assertEquals(result.getNullCount(), iterations);
		Assert.assertEquals(result.getMinLength(), 0);
		Assert.assertEquals(result.getMaxLength(), 0);
		Assert.assertEquals(result.getMinValue(), null);
		Assert.assertEquals(result.getMaxValue(), null);
		Assert.assertEquals(result.getNullCount(), iterations);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getRegExp(), "[NULL]");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "NULL");
	}

	@Test
	public void manyBlanks() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int iterations = 50;

		analysis.train("");
		for (int i = 0; i < iterations; i++) {
			analysis.train(" ");
			analysis.train("  ");
			analysis.train("      ");
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), 3 * iterations + 1);
		Assert.assertEquals(result.getMaxLength(), 6);
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxValue(), "      ");
		Assert.assertEquals(result.getMinValue(), " ");
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getMatchCount(), 3 * iterations + 1);
		Assert.assertEquals(result.getBlankCount(), 3 * iterations + 1);
		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "BLANK");

		Assert.assertTrue("".matches(result.getRegExp()));
		Assert.assertTrue(" ".matches(result.getRegExp()));
		Assert.assertTrue("  ".matches(result.getRegExp()));
		Assert.assertTrue("      ".matches(result.getRegExp()));
	}

	@Test
	public void sameBlanks() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int iterations = 50;

		for (int i = 0; i < iterations; i++) {
			analysis.train("      ");
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), iterations);
		Assert.assertEquals(result.getMaxLength(), 6);
		Assert.assertEquals(result.getMinLength(), 6);
		Assert.assertEquals(result.getMaxValue(), "      ");
		Assert.assertEquals(result.getMinValue(), "      ");
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getMatchCount(), iterations);
		Assert.assertEquals(result.getBlankCount(), iterations);
		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "BLANK");

		Assert.assertTrue("      ".matches(result.getRegExp()));
	}

	@Test
	public void justEmpty() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int iterations = 50;

		for (int i = 0; i < iterations; i++) {
			analysis.train("");
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), iterations);
		Assert.assertEquals(result.getMaxLength(), 0);
		Assert.assertEquals(result.getMinLength(), 0);
		Assert.assertEquals(result.getMaxValue(), "");
		Assert.assertEquals(result.getMinValue(), "");
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getMatchCount(), iterations);
		Assert.assertEquals(result.getBlankCount(), iterations);
		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "BLANK");

		Assert.assertTrue("".matches(result.getRegExp()));
	}


	@Test
	public void whiteSpace() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("field,value");
		final String input = "| |  |   |    |     |      |       |        |         |";
		final String inputs[] = input.split("\\|");

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++)
			analysis.train(inputs[i]);

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++)
			analysis.train(inputs[i]);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "BLANKORNULL");
		Assert.assertEquals(result.getSampleCount(), 22);
		Assert.assertEquals(result.getMatchCount(), 22);
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxLength(), 9);
		Assert.assertEquals(result.getBlankCount(), 20);
		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}

	}

	@Test
	public void testRegisterFinite() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("CUSIP");
		analysis.setMaxCardinality(20000);
		Assert.assertTrue(analysis.registerLogicalType("com.cobber.fta.PluginCUSIP"));
		final String input =
				"75605A702|G39637955|029326105|63009R109|04269E957|666666666|00768Y727|23908L306|126349AF6|73937B589|" +
				"516806956|683797104|902973954|600544950|15671L909|00724F951|292104106|00847X904|219350955|67401P958|" +
				"902641752|50218P957|00739L901|06746P903|92189F953|G47567905|06740P650|13123X952|38173M952|29359T102|" +
				"229663959|33734E103|118230951|883556102|689648103|97382A900|808194954|60649T957|13645T900|075896950|" +
				"29266S956|80105N905|032332904|73935X951|73935B955|464288125|87612G901|39945C909|97717X957|14575E105|" +
				"75605A702|G39637955|029326105|63009R109|04269E957|856190953|00768Y727|23908L306|126349AF6|73937B589|" +
				"516806956|683797104|902973954|600544950|15671L909|00724F951|292104106|00847X904|219350955|67401P958|" +
				"902641752|50218P957|00739L901|06746P903|92189F953|G47567905|06740P650|13123X952|38173M952|29359T102|" +
				"229663959|33734E103|118230951|883556102|689648103|97382A900|808194954|60649T957|13645T900|075896950|" +
				"29266S956|80105N905|032332904|73935X951|73935B955|464288125|87612G901|39945C909|97717X957|14575E105|" +
				"75605A702|G39637955|029326105|63009R109|04269E957|856190953|00768Y727|23908L306|126349AF6|73937B589|" +
				"516806956|683797104|902973954|600544950|15671L909|00724F951|292104106|00847X904|219350955|67401P958|" +
				"902641752|50218P957|00739L901|06746P903|92189F953|G47567905|06740P650|13123X952|38173M952|29359T102|" +
				"229663959|33734E103|118230951|883556102|689648103|97382A900|808194954|60649T957|13645T900|075896950|" +
				"29266S956|80105N905|032332904|73935X951|73935B955|464288125|87612G901|39945C909|97717X957|14575E105|";
		final String inputs[] = input.split("\\|");

		for (int i = 0; i < inputs.length; i++)
			analysis.train(inputs[i]);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "CUSIP");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length - 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getMinLength(), 9);
		Assert.assertEquals(result.getMaxLength(), 9);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\p{Alnum}{9}");
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());

		int matchCount = 0;
		for (int i = 0; i < inputs.length; i++) {
			if (inputs[i].matches(result.getRegExp()))
				matchCount++;
		}
		Assert.assertEquals(matchCount, result.getMatchCount() + 1);
	}

	@Test
	public void testRegisterInfinite() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("CC");
		Assert.assertTrue(analysis.registerLogicalType("com.cobber.fta.PluginCreditCard"));
		final String[] input = {
//				"Credit Card Type,Credit Card Number",
				"American Express,378282246310005",
				"American Express,371449635398431",
				"American Express Corporate,378734493671000 ",
				"Australian BankCard,5610591081018250",
				"Diners Club,30569309025904",
				"Diners Club,38520000023237",
				"Discover,6011111111111117",
				"Discover,6011000990139424",
				"JCB,3530111333300000",
				"JCB,3566002020360505",
				"MasterCard,5555555555554444",
				"MasterCard,5105105105105100",
				"Visa,4111111111111111",
				"Visa,4012888888881881",
				"Visa,4222222222222",
//				"Dankort (PBS),76009244561",
				"Dankort (PBS),5019717010103742",
				"Switch/Solo (Paymentech),6331101999990016",
				"MasterCard,5555 5555 5555 4444",
				"MasterCard,5105 1051 0510 5100",
				"Visa,4111 1111 1111 1111",
				"Visa,4012 8888 8888 1881",
				"MasterCard,5555-5555-5555-4444",
				"MasterCard,5105-1051-0510-5100",
				"Visa,4111-1111-1111-1111",
				"Visa,4012-8888-8888-1881"
		};

		Set<String>  samples = new HashSet<String>();

		for (int i = 0; i < input.length; i++) {
			String s = input[i].split(",")[1];
			samples.add(s);
			analysis.train(s);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "CREDITCARD");
		Assert.assertEquals(result.getSampleCount(), input.length);
		Assert.assertEquals(result.getMatchCount(), input.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getMinLength(), 13);
		Assert.assertEquals(result.getMaxLength(), 19);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertTrue(result.isLogicalType());
		Assert.assertEquals(result.getRegExp(), "(?:\\d[ -]*?){13,16}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (String s : samples) {
			Assert.assertTrue(s.matches(result.getRegExp()), s);
		}
	}

	@Test
	public void employeeNumber() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("employeeNumber");
		final String input = "||||||||||||||||||||" +
				"||||||||||||48|72|242|242|242|335|354|355|355|" +
				"397|460|567|616|616|70|70|865|1023|1023|1023|1023|1023|1023|1023|1023|1161|1161|1161|1161|1161|" +
				"1260|1273|1273|1273|136|136|136|136|136|136|136|136|136|1422|1422|1422|1422|1422|1422|1548|1652|" +
				"F9442559|F9442774|F9442774|F9442856|F9442856|F9442856|F9442856|F9442856|F9442965|F9442965|" +
				"F9442965|F9442968|F9442999|F9442999|FN228446|FN241214|FN241227|FN246235|FN246286|FN246288|" +
				"FN270164|FN270168|FN273815|FN273967|FN295633|FN295655|FN295657|FN295659|FN295684|FN295688|" +
				"FN295842|FN9441020|FN9441048|FN9441064|FN9441082|FN9441138|FN9441189|FN9441244|FN9441246|FN9441248|" +
				"FN9441330|FN9441334|FN9441383|FN9441501|FN9441505|FN9441516|FN9441529|FN9441680|FN9441695|FN9441804|";
		final String inputs[] = input.split("\\|");

		for (int i = 0; i < inputs.length; i++)
			analysis.train(inputs[i]);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length - result.getBlankCount());
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getMinLength(), 2);
		Assert.assertEquals(result.getMaxLength(), 9);
		Assert.assertEquals(result.getBlankCount(), 32);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHANUMERIC + "{2,9}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		int matchCount = 0;
		for (int i = 0; i < inputs.length; i++) {
			if (inputs[i].matches(result.getRegExp()))
				matchCount++;
		}
		Assert.assertEquals(matchCount, result.getMatchCount());
	}

	@Test
	public void basicEmail() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "Bachmann@lavastorm.com|Biedermann@lavastorm.com|buchheim@lavastorm.com|" +
				"coleman@lavastorm.com|Drici@lavastorm.com|Garvey@lavastorm.com|jackson@lavastorm.com|" +
				"Jones@lavastorm.com|Marinelli@lavastorm.com|Nason@lavastorm.com|Parker@lavastorm.com|" +
				"Pigneri@lavastorm.com|Rasmussen@lavastorm.com|Regan@lavastorm.com|Segall@Lavastorm.com|" +
				"Pigneri2@lavastorm.com|ahern@lavastorm.com|reginald@lavastorm.com|blumfontaine@Lavastorm.com|" +
				"Smith@lavastorm.com|Song@lavastorm.com|Tolleson@lavastorm.com|wynn@lavastorm.com|" +
				"Ahmed@lavastorm.com|Benoit@lavastorm.com|Keane@lavastorm.com|Kilker@lavastorm.com|" +
				"Waters@lavastorm.com|Meagher@lavastorm.com|Mok@lavastorm.com|Mullin@lavastorm.com|" +
				"Nason@lavastorm.com|reilly@lavastorm.com|Scoble@lavastorm.com|Comerford@lavastorm.com|" +
				"Gallagher@lavastorm.com|Hughes@lavastorm.com|Kelly@lavastorm.com|" +
				"Tuddenham@lavastorm.com|Williams@lavastorm.com|Wilson@lavastorm.com";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train("tim@cobber com");
		analysis.train("tim@cobber com");
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), inputs.length + 2 + result.getNullCount());
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "EMAIL");
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getRegExp(), "[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}");
		Assert.assertEquals(result.getConfidence(), 1 - (double)2/(result.getSampleCount() - result.getNullCount()));

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	private final static String INPUT_URLS = "http://www.lavastorm.com|ftp://ftp.sun.com|https://www.google.com|" +
			"https://www.homedepot.com|http://www.lowes.com|http://www.apple.com|http://www.sgi.com|" +
			"http://www.ibm.com|http://www.snowgum.com|http://www.zaius.com|http://www.cobber.com|" +
			"http://www.ey.com|http://www.zoomer.com|http://www.redshift.com|http://www.segall.net|" +
			"http://www.sgi.com|http://www.united.com|https://www.hp.com/printers/support|http://www.opinist.com|" +
			"http://www.java.com|http://www.slashdot.org|http://theregister.co.uk|";

	@Test
	public void basicURL() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String inputs[] = INPUT_URLS.split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);
		analysis.train("bogus");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), inputs.length + 1 + result.getNullCount());
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length + 1 - result.getOutlierCount());
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getRegExp(), "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/(result.getSampleCount() - result.getNullCount()));
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "URL");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void backoutURL() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String inputs[] = INPUT_URLS.split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final int badURLs = 50;
		for (int i = 0; i < badURLs; i++)
			analysis.train(String.valueOf(i));

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length + badURLs + result.getNullCount());
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getOutlierCount(), 0);
//		Assert.assertEquals(result.getMatchCount(), inputs.length + badURLs + result.getNullCount());
		Assert.assertEquals(result.getRegExp(), ".{1,35}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void notEmail() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "2@3|3@4|b4@5|" +
				"6@7|7@9|12@13|100@2|" +
				"Zoom@4|Marinelli@44|55@90341|Parker@46|" +
				"Pigneri@22|Rasmussen@77|478 @ 1912|88 @ LC|" +
				"Smith@99|Song@88|77@|@lavastorm.com|" +
				"Tuddenham@02421|Williams@uk|Wilson@99";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		analysis.train(null);
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train("tim@cobber com");
		analysis.train("tim@cobber com");
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), inputs.length + 2 + result.getNullCount());
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getRegExp(), ".{3,15}");
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getMatchCount(), inputs.length + 2);
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void testTrim() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = " Hello|  Hello| Hello |  world  |    Hello   |      Hi        |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi        |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi        |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi        |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi          |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi        |" +
				" Hello|  Hello| Hello |  world  |    Hello   |      Hi        ";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		analysis.train(null);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), inputs.length + result.getNullCount());
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 1);
		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*" + KnownPatterns.PATTERN_ALPHA + "{2,5}\\p{javaWhitespace}*");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicEmailList() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "Bachmann@lavastorm.com,Biedermann@lavastorm.com|buchheim@lavastorm.com|" +
				"coleman@lavastorm.com,Drici@lavastorm.com|Garvey@lavastorm.com|jackson@lavastorm.com|" +
				"Jones@lavastorm.com|Marinelli@lavastorm.com,Nason@lavastorm.com,Parker@lavastorm.com|" +
				"Pigneri@lavastorm.com|Rasmussen@lavastorm.com|Regan@lavastorm.com|Segall@Lavastorm.com|" +
				"Smith@lavastorm.com|Song@lavastorm.com|Tolleson@lavastorm.com|wynn@lavastorm.com|" +
				"Ahmed@lavastorm.com|Benoit@lavastorm.com|Keane@lavastorm.com|Kilker@lavastorm.com|" +
				"Waters@lavastorm.com|Meagher@lavastorm.com|Mok@lavastorm.com|Mullin@lavastorm.com|" +
				"Nason@lavastorm.com|reilly@lavastorm.com|Scoble@lavastorm.com|Comerford@lavastorm.com|" +
				"Gallagher@lavastorm.com|Hughes@lavastorm.com|Kelly@lavastorm.com|" +
				"Tuddenham@lavastorm.com,Williams@lavastorm.com,Wilson@lavastorm.com";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "EMAIL");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		// Only simple emails match the regexp, so the count will not the 4 that include email lists :-(
		int matches = 0;
		for (int i = 0; i < inputs.length; i++) {
			if (inputs[i].matches(result.getRegExp()))
				matches++;
		}
		Assert.assertEquals(result.getMatchCount() - 4, matches);
	}

	@Test
	public void basicEmailListSemicolon() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "Bachmann@lavastorm.com;Biedermann@lavastorm.com|buchheim@lavastorm.com|" +
				"coleman@lavastorm.com;Drici@lavastorm.com|Garvey@lavastorm.com|jackson@lavastorm.com|" +
				"Jones@lavastorm.com|Marinelli@lavastorm.com;Nason@lavastorm.com;Parker@lavastorm.com|" +
				"Pigneri@lavastorm.com|Rasmussen@lavastorm.com|Regan@lavastorm.com|Segall@Lavastorm.com|" +
				"Smith@lavastorm.com|Song@lavastorm.com|Tolleson@lavastorm.com|wynn@lavastorm.com|" +
				"Ahmed@lavastorm.com|Benoit@lavastorm.com|Keane@lavastorm.com|Kilker@lavastorm.com|" +
				"Waters@lavastorm.com|Meagher@lavastorm.com|Mok@lavastorm.com|Mullin@lavastorm.com|" +
				"Nason@lavastorm.com|reilly@lavastorm.com|Scoble@lavastorm.com|Comerford@lavastorm.com|" +
				"Gallagher@lavastorm.com|Hughes@lavastorm.com|Kelly@lavastorm.com|" +
				"Tuddenham@lavastorm.com;Williams@lavastorm.com;Wilson@lavastorm.com|bo gus|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "EMAIL");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length - 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}");
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
	}

	final String validZips = "01770|01772|01773|02027|02030|02170|02379|02657|02861|03216|03561|03848|04066|04281|04481|04671|04921|05072|05463|05761|" +
			"06045|06233|06431|06704|06910|07101|07510|07764|08006|08205|08534|08829|10044|10260|10549|10965|11239|11501|11743|11976|" +
			"12138|12260|12503|12746|12878|13040|13166|13418|13641|13801|14068|14276|14548|14731|14865|15077|15261|15430|15613|15741|" +
			"15951|16210|16410|16662|17053|17247|17516|17765|17951|18109|18428|18702|18957|19095|19339|19489|19808|20043|20170|20370|" +
			"20540|20687|20827|21047|21236|21779|22030|22209|22526|22741|23016|23162|23310|23503|23868|24038|24210|24430|24594|24856|" +
			"25030|25186|25389|25638|25841|26059|26524|26525|26763|27199|27395|27587|27832|27954|28119|28280|28397|28543|28668|28774|" +
			"29111|29329|29475|29622|29744|30016|30119|30235|30343|30503|30643|31002|31141|31518|31724|31901|32134|32297|32454|32617|" +
			"32780|32934|33093|33265|33448|33603|33763|33907|34138|34470|34731|35053|35221|35491|35752|36022|36460|36616|36860|37087|";
	@Test
	public void basicZip() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String inputs[] = validZips.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getTypeQualifier(), "US_ZIP5");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 32);
		Assert.assertEquals(result.getRegExp(), "\\d{5}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void zipUnwind() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "02421|02420|02421|02420|02421|02420|02421|02420|02421|02420|" +
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
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length - 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 20);
		Assert.assertEquals(result.getRegExp(), "\\d{5}");
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());

		int matches = 0;
		for (int i = 0; i < inputs.length; i++) {
			if (inputs[i].matches(result.getRegExp()))
					matches++;
		}
		Assert.assertEquals(result.getMatchCount(), matches);
	}

	@Test
	public void zipNotReal() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input =
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
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{5}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void sameZip() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int copies = 100;
		final String sample = "02421";

		int locked = -1;

		for (int i = 0; i < copies; i++) {
			if (analysis.train(sample) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), copies);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), copies);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), copies);
		Assert.assertEquals(result.getRegExp(), "\\d{5}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertTrue(sample.matches(result.getRegExp()));
	}

	@Test
	public void sameZipWithHeader() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("ZipCode");
		final int copies = 100;
		final String sample = "02421";

		int locked = -1;

		for (int i = 0; i < copies; i++) {
			if (analysis.train(sample) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getTypeQualifier(), "US_ZIP5");
		Assert.assertEquals(result.getSampleCount(), copies);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), copies);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), copies);
		Assert.assertEquals(result.getRegExp(), "\\d{5}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertTrue(sample.matches(result.getRegExp()));
	}

	@Test
	public void changeMind() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		int locked = -1;

		for (int i = 0; i < 2 * TextAnalyzer.SAMPLE_DEFAULT; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i;
		}

		for (char ch = 'a'; ch <= 'z'; ch++) {
			analysis.train(String.valueOf(ch));
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), 2 * TextAnalyzer.SAMPLE_DEFAULT + 26);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), 2 * TextAnalyzer.SAMPLE_DEFAULT + 26);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHANUMERIC + "{1,2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void changeMindMinMax() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		analysis.setThreshold(97);
		final String input =
				"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!!Volume!Volume!!!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!!Volume!Volume!!!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!!!!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!!!!Volume!Audio disc ; Volume!Volume!!!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Audio disc ; Volume!!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!!Volume!Volume!!!!!!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!!!Volume!Volume!Volume!Volume!Volume!!!Volume!Volume!Volume!" +
						"Volume!Volume!!!!!!!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"!Volume!Volume!!!Volume!Volume!Volume!!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!!Volume!Volume!Volume!!Volume!Volume!!Volume!Volume!!Volume!Volume!Volume!" +
						"Volume!!Volume!Volume!!Volume!!!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Online resource (ePub ebook)!Volume!Online resource (ePub ebook)!Volume!!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"!Volume!Volume!Volume!Volume!Volume!Volume!Volume!!Volume!Volume!Volume!Volume!Volume!!Volume!Volume!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Volume!Audio disc ; Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Online resource (ePub ebook)!Volume!Volume!Volume!" +
						"Volume!Volume!!!!Volume!Volume!Volume!Volume!Volume!Volume!Computer disc!Volume!Volume!Volume!Volume!Volume!" +
						"!Volume!Online resource!Volume!!Volume!!Volume!!Volume!!Volume!Online resource (PDF ebook ; ePub ebook)!Volume!Volume!Volume!Volume!" +
						"Volume!Volume!Online resource (ePub ebook)!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Audio disc ; Volume!" +
						"Volume!Volume!Volume!Volume!Volume!!Volume!Volume!!Volume!Volume!Volume!!Volume!Volume!!" +
						"Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!Volume!";
		final String inputs[] = input.split("\\!");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount() + result.getBlankCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 0);
		Assert.assertEquals(result.getRegExp(), ".{6,40}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getMinValue(), "Audio disc ; Volume");
		Assert.assertEquals(result.getMaxValue(), "Volume");

		String regExp = result.getRegExp();
		for (int i = 0; i < inputs.length; i++) {
			if (inputs[i].length() == 0)
				continue;
			Assert.assertTrue(inputs[i].matches(regExp), inputs[i]);
		}
	}

	@Test
	public void backoutToDouble() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input =
				"0|0|0|0|0|0|0|0|0|1|2|3|0|0|0|0|0|" +
						"0|0|0|0|0.25|0|0|0|0|0|0|0|0|0|0|0|0|" +
						"0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|" +
						"0|0|0|0|0|0|0|0.02|0.06|0|0|0|0|0|0.02|0|0|" +
						"0.02|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|" +
						"0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|" +
						"0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|" +
						"0|0|0|0|0|0|0|0|0|0|0.02|0.02|0|0|0|0|0|" +
						"0.14|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|" +
						"0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|" +
						"0|0|0.25|0|0|0|0|0|0|0|0|0|0|0|0|0|0|" +
						"0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|" +
						"0|0|0|0|0|0.05|0|0|0|0|0|0|0.02|0|0.02|0|0|" +
						"0|0.01|0|0|0|0|0.02|0|0|0|0|0.01|0|0|0|||";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount() + result.getBlankCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 218);
		Assert.assertEquals(result.getRegExp(), "\\d+|(\\d+)?\\.\\d+");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getMinValue(), "0.0");
		Assert.assertEquals(result.getMaxValue(), "3.0");

		String regExp = result.getRegExp();
		for (int i = 0; i < inputs.length; i++) {
			if (inputs[i].length() == 0)
				continue;
			Assert.assertTrue(inputs[i].matches(regExp), inputs[i]);
		}
	}

	@Test
	public void leadingZeros() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("BL record ID", null);

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

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), 59);
		Assert.assertEquals(result.getMatchCount(), 59);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 59);
		Assert.assertEquals(result.getRegExp(), "\\d{9}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void groupingSeparator() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Separator");
		final String input = "3600|7500|3600|3600|800|3600|1200|1200|600|" +
				"1200|1200|1200|1200|3600|1200|13,000|1200|200|" +
				"1200|1200|1200|1200|1200|1200|1200|1200|200|" +
				"1200|3600|1200|1200|1200|1200|1200|1200|200|" +
				"1200|1200|1200|1200|1200|1200|1200|1200|200|" +
				"1200|3600|1200|1200|1200|1200|3600|1200|600|" +
				"1200|1200|1200|1200|3600|1200|13,000|1200|200|" +
				"1200|1200|1200|1200|1200|1200|1200|1200|200|" +
				"1200|1200|1200|1200|3600|1200|1200|1200|200|" +
				"3600|1200|1200|1200|1200|1200|1200|1200|200|" +
				"1200|1200|1200|3600|3600|1200|1200|1200|200|";

		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getTypeQualifier(), "GROUPING");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 0);
		Assert.assertEquals(result.getRegExp(), "[\\d,]{3,6}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getMinValue(), "200");
		Assert.assertEquals(result.getMaxValue(), "13000");

		String regExp = result.getRegExp();
		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(regExp), inputs[i]);
		}
	}

	@Test
	public void groupingSeparatorLarge() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Separator");
		final Random random = new Random();
		final int SAMPLE_SIZE = 10000;
		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;
		String minValue = String.valueOf(min);
		String maxValue = String.valueOf(max);
		Set<String> samples = new HashSet<String>();

		for (int i = 0; i < SAMPLE_SIZE; i++) {
			long l = random.nextInt(100000000);
			String sample = NumberFormat.getNumberInstance(Locale.US).format(l).toString();
			if (l < min) {
				min = l;
				minValue = sample;
			}
			if ( l > max) {
				max = l;
				maxValue = sample;
			}
			samples.add(sample);
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getTypeQualifier(), "GROUPING");
		Assert.assertEquals(result.getSampleCount(), SAMPLE_SIZE);
		Assert.assertEquals(result.getMatchCount(), SAMPLE_SIZE);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 0);
		Assert.assertEquals(result.getMinValue(), String.valueOf(min));
		Assert.assertEquals(result.getMaxValue(), String.valueOf(max));
		String regExp = "[\\d,]{";
		if (minValue.length() == maxValue.length())
			regExp += minValue.length();
		else {
			regExp += minValue.length() + "," + maxValue.length();
		}
		regExp += "}";
		Assert.assertEquals(result.getRegExp(), regExp);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (String sample : samples) {
			Assert.assertTrue(sample.matches(regExp), sample);
		}
	}

	@Test
	public void groupingSeparatorLargeFRENCH() throws IOException {
		Locale locales[] = new Locale[] { Locale.GERMAN, Locale.FRANCE };
		final Random random = new Random(1);
		final int SAMPLE_SIZE = 1000;
		Set<String> samples = new HashSet<String>();

		for (Locale locale : locales) {
			long min = Long.MAX_VALUE;
			long absMin = Long.MAX_VALUE;
			long max = Long.MIN_VALUE;
			String minValue = String.valueOf(min);
			String maxValue = String.valueOf(max);
			NumberFormat nf = NumberFormat.getNumberInstance(locale);
			final TextAnalyzer analysis = new TextAnalyzer("Separator");
			analysis.setLocale(locale);
			samples.clear();

			for (int i = 0; i < SAMPLE_SIZE; i++) {
				long l = random.nextInt(100000000);
				if (l%2 == 0)
					l = -l;
				String sample = nf.format(l).toString();
				if (l < min) {
					min = l;
				}
				if (Math.abs(l) < absMin) {
					absMin = Math.abs(l);
					minValue = sample;
				}
				if (l < min) {
					min = l;
				}
				if (l > max) {
					max = l;
					maxValue = sample;
				}
				samples.add(sample);
				analysis.train(sample);
			}

			final TextAnalysisResult result = analysis.getResult();

			Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
			Assert.assertEquals(result.getTypeQualifier(), "SIGNED,GROUPING", locale.toString());
			Assert.assertEquals(result.getSampleCount(), SAMPLE_SIZE);
			Assert.assertEquals(result.getMatchCount(), SAMPLE_SIZE);
			Assert.assertEquals(result.getNullCount(), 0);
			Assert.assertEquals(result.getLeadingZeroCount(), 0);
			Assert.assertEquals(result.getMinValue(), String.valueOf(min));
			Assert.assertEquals(result.getMaxValue(), String.valueOf(max));
			DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);

			String regExp = "-?[\\d" + Utils.slosh(formatSymbols.getGroupingSeparator()) + "]";
			regExp += Utils.regExpLength(minValue.length(), maxValue.length());
			Assert.assertEquals(result.getRegExp(), regExp);
			Assert.assertEquals(result.getConfidence(), 1.0);

			for (String sample : samples) {
				Assert.assertTrue(sample.matches(regExp), sample);
			}
		}
	}

	@Test
	public void localeDoubleTest() throws IOException {
		final Random random = new Random(1);
		final int SAMPLE_SIZE = 1000;
		Locale[] locales = DateFormat.getAvailableLocales();
//		Locale[] locales = new Locale[] { Locale.forLanguageTag("en-US"), Locale.forLanguageTag("hr-HR") };
//		Locale[] locales = new Locale[] { Locale.forLanguageTag("hi-IN") };

		for (Locale locale : locales) {
			final TextAnalyzer analysis = new TextAnalyzer("Separator");
			analysis.setLocale(locale);

			boolean simple = NumberFormat.getNumberInstance(locale).format(0).matches("\\d");

			if (!simple) {
				System.err.printf("Skipping locale '%s' as it does not use Arabic numerals.\n", locale);
				continue;
			}

			Calendar cal = GregorianCalendar.getInstance(locale);
			if (!(cal instanceof GregorianCalendar)) {
				System.err.printf("Skipping locale '%s' as it does not use the Gregorian calendar.\n", locale);
				continue;
			}

			Set<String> samples = new HashSet<String>();
			NumberFormat nf = NumberFormat.getNumberInstance(locale);
			nf.setMinimumFractionDigits(2);
			for (int i = 0; i < SAMPLE_SIZE; i++) {
				double d = random.nextDouble() * 100000000;
				String sample = nf.format(d).toString();
				samples.add(sample);
				analysis.train(sample);
			}

			final TextAnalysisResult result = analysis.getResult();

			Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
			Assert.assertEquals(result.getTypeQualifier(), "GROUPING");
			Assert.assertEquals(result.getSampleCount(), SAMPLE_SIZE);
			Assert.assertEquals(result.getMatchCount(), SAMPLE_SIZE);
			Assert.assertEquals(result.getNullCount(), 0);
			Assert.assertEquals(result.getLeadingZeroCount(), 0);
			DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);

			String grp = formatSymbols.getGroupingSeparator() == '.' ? "\\." : "" + formatSymbols.getGroupingSeparator();
			String dec = formatSymbols.getDecimalSeparator() == '.' ? "\\." : "" + formatSymbols.getDecimalSeparator();

			String regExp = "[\\d" + grp + "]+|([\\d" + grp + "]+)?" + dec + "[\\d" + grp +"]+";

//			System.err.println("Locale: " + locale + ", grp = '" + grp + "', dec = '" + dec + "', re: " + regExp + "'");

			Assert.assertEquals(result.getRegExp(), regExp);
			Assert.assertEquals(result.getConfidence(), 1.0);

			for (String sample : samples) {
				Assert.assertTrue(sample.matches(regExp), sample + " " + regExp);
			}
		}
	}

//	@Test
	public void localeNegativeDoubleTest() throws IOException {
		final Random random = new Random(1);
		final int SAMPLE_SIZE = 1000;
//		Locale[] locales = DateFormat.getAvailableLocales();
//		Locale[] locales = new Locale[] { Locale.forLanguageTag("en-US"), Locale.forLanguageTag("hr-HR") };
		Locale[] locales = new Locale[] { Locale.forLanguageTag("ar-AE") };

		for (Locale locale : locales) {
			final TextAnalyzer analysis = new TextAnalyzer("Separator");
			analysis.setLocale(locale);

			boolean simple = NumberFormat.getNumberInstance(locale).format(0).matches("\\d");
			boolean signFront = NumberFormat.getNumberInstance(locale).format(-1).charAt(0) == '-';

			if (!simple) {
				System.err.printf("Skipping locale '%s' as it does not use Arabic numerals.\n", locale);
				continue;
			}

			Calendar cal = GregorianCalendar.getInstance(locale);
			if (!(cal instanceof GregorianCalendar)) {
				System.err.printf("Skipping locale '%s' as it does not use the Gregorian calendar.\n", locale);
				continue;
			}

			DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);

			String grp = formatSymbols.getGroupingSeparator() == '.' ? "\\." : "" + formatSymbols.getGroupingSeparator();
			String dec = formatSymbols.getDecimalSeparator() == '.' ? "\\." : "" + formatSymbols.getDecimalSeparator();
			System.err.printf("Locale '%s', grouping: %s, decimal: %s.\n", locale, grp, dec);

			Set<String> samples = new HashSet<String>();
			NumberFormat nf = NumberFormat.getNumberInstance(locale);
			nf.setMinimumFractionDigits(2);
			for (int i = 0; i < SAMPLE_SIZE; i++) {
				double d = random.nextDouble() * -100000000;
				String sample = nf.format(d).toString();
				samples.add(sample);
				analysis.train(sample);
			}

			final TextAnalysisResult result = analysis.getResult();

			Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
			Assert.assertEquals(result.getTypeQualifier(), "SIGNED,GROUPING");
			Assert.assertEquals(result.getSampleCount(), SAMPLE_SIZE);
			Assert.assertEquals(result.getMatchCount(), SAMPLE_SIZE);
			Assert.assertEquals(result.getNullCount(), 0);
			Assert.assertEquals(result.getLeadingZeroCount(), 0);

			String regExp = "-?[\\d" + grp + "]+|-?([\\d" + grp + "]+)?" + dec + "[\\d" + grp +"]+";

//			System.err.println("Locale: " + locale + ", grp = '" + grp + "', dec = '" + dec + "', re: " + regExp + "'");

			Assert.assertEquals(result.getRegExp(), regExp);
			Assert.assertEquals(result.getConfidence(), 1.0);

			for (String sample : samples) {
				Assert.assertTrue(sample.matches(regExp), sample + " " + regExp);
			}
		}
	}

//	@Test
	public void localeLongTest() throws IOException {
		final Random random = new Random(1);
		final int SAMPLE_SIZE = 1000;
		Locale[] locales = DateFormat.getAvailableLocales();
//		Locale[] locales = new Locale[] { Locale.forLanguageTag("en-US"), Locale.forLanguageTag("hr-HR") };
//		Locale[] locales = new Locale[] { Locale.forLanguageTag("ar-AE") };

		for (Locale locale : locales) {
			long min = Long.MAX_VALUE;
			long absMin = Long.MAX_VALUE;
			long max = Long.MIN_VALUE;
			String minValue = String.valueOf(min);
			String maxValue = String.valueOf(max);
			final TextAnalyzer analysis = new TextAnalyzer("Separator");
			analysis.setLocale(locale);

			boolean simple = NumberFormat.getNumberInstance(locale).format(0).matches("\\d");

			if (!simple) {
				System.err.printf("Skipping locale '%s' as it does not use Arabic numerals.\n", locale);
				continue;
			}

			Calendar cal = GregorianCalendar.getInstance(locale);
			if (!(cal instanceof GregorianCalendar)) {
				System.err.printf("Skipping locale '%s' as it does not use the Gregorian calendar.\n", locale);
				continue;
			}

			DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);

			String grp = formatSymbols.getGroupingSeparator() == '.' ? "\\." : "" + formatSymbols.getGroupingSeparator();
			char minusSign = formatSymbols.getMinusSign();
			boolean signLeading = NumberFormat.getNumberInstance(locale).format(-1).charAt(0) == minusSign;

			System.err.printf("Locale '%s', grouping: %s.\n", locale, grp);

			Set<String> samples = new HashSet<String>();
			NumberFormat nf = NumberFormat.getIntegerInstance(locale);
			for (int i = 0; i < SAMPLE_SIZE; i++) {
				long l = random.nextLong();
				if (l % 2 == 0)
					l = -l;
				String sample = nf.format(l).toString();

				if (l < min) {
					min = l;
				}
				if (Math.abs(l) < absMin) {
					absMin = Math.abs(l);
					minValue = sample;
				}
				if (l < min) {
					min = l;
				}
				if (l > max) {
					max = l;
					maxValue = sample;
				}

				samples.add(sample);
				analysis.train(sample);
			}

			final TextAnalysisResult result = analysis.getResult();

			Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
			Assert.assertEquals(result.getTypeQualifier(), "SIGNED,GROUPING");
			Assert.assertEquals(result.getSampleCount(), SAMPLE_SIZE);
			Assert.assertEquals(result.getMatchCount(), SAMPLE_SIZE);
			Assert.assertEquals(result.getNullCount(), 0);
			Assert.assertEquals(result.getLeadingZeroCount(), 0);
			Assert.assertEquals(result.getDecimalSeparator(), formatSymbols.getDecimalSeparator());

			String regExp = "";
			if (signLeading)
				regExp += Utils.slosh(minusSign) + "?";
			regExp += "[\\d" + Utils.slosh(formatSymbols.getGroupingSeparator()) + "]";
			regExp += Utils.regExpLength(minValue.length(), maxValue.length());
			if (!signLeading)
				regExp += Utils.slosh(minusSign) + "?";

//			System.err.println("Locale: " + locale + ", grp = '" + grp + "', dec = '" + dec + "', re: " + regExp + "'");

//			Assert.assertEquals(result.getRegExp(), regExp);
			Assert.assertEquals(result.getConfidence(), 1.0);

			for (String sample : samples) {
				Assert.assertTrue(sample.matches(regExp), sample + " " + regExp);
			}
		}
	}

	//@Test
	public void decimalSeparatorTest_Period() throws IOException {
		final Random random = new Random(1);
		final int SAMPLE_SIZE = 1000;
		Locale[] locales = new Locale[] { Locale.forLanguageTag("de-DE") };

		for (Locale locale : locales) {
			long min = Long.MAX_VALUE;
			long absMin = Long.MAX_VALUE;
			long max = Long.MIN_VALUE;
			String minValue = String.valueOf(min);
			String maxValue = String.valueOf(max);
			final TextAnalyzer analysis = new TextAnalyzer("DecimalSeparator");
			analysis.setLocale(locale);

			DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);

			String grp = formatSymbols.getGroupingSeparator() == '.' ? "\\." : "" + formatSymbols.getGroupingSeparator();

			System.err.printf("Locale '%s', grouping: %s.\n", locale, grp);

			Set<String> samples = new HashSet<String>();
			NumberFormat nf = NumberFormat.getIntegerInstance(locale);
			for (int i = 0; i < SAMPLE_SIZE; i++) {
				long l = random.nextInt(10000000);
				if (l % 2 == 0)
					l = -l;
				String sample = String.valueOf(l) + "." + random.nextInt(10);

				if (l < min) {
					min = l;
				}
				if (Math.abs(l) < absMin) {
					absMin = Math.abs(l);
					minValue = sample;
				}
				if (l < min) {
					min = l;
				}
				if (l > max) {
					max = l;
					maxValue = sample;
				}

				samples.add(sample);
				analysis.train(sample);
			}

			final TextAnalysisResult result = analysis.getResult();

			Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
			Assert.assertEquals(result.getTypeQualifier(), "SIGNED,GROUPING");
			Assert.assertEquals(result.getSampleCount(), SAMPLE_SIZE);
			Assert.assertEquals(result.getMatchCount(), SAMPLE_SIZE);
			Assert.assertEquals(result.getNullCount(), 0);
			Assert.assertEquals(result.getLeadingZeroCount(), 0);
			Assert.assertEquals(result.getDecimalSeparator(), formatSymbols.getDecimalSeparator());

			String regExp = "-?";
			regExp += "[\\d" + Utils.slosh(formatSymbols.getGroupingSeparator()) + "]";
			regExp += Utils.regExpLength(minValue.length(), maxValue.length());

//			System.err.println("Locale: " + locale + ", grp = '" + grp + "', dec = '" + dec + "', re: " + regExp + "'");

//			Assert.assertEquals(result.getRegExp(), regExp);
			Assert.assertEquals(result.getConfidence(), 1.0);

			for (String sample : samples) {
				Assert.assertTrue(sample.matches(regExp), sample + " " + regExp);
			}
		}
	}

	@Test
	public void decimalSeparatorTest_Locale() throws IOException {
		final Random random = new Random(1);
		final int SAMPLE_SIZE = 1000;
		Locale[] locales = new Locale[] { Locale.forLanguageTag("de-DE"), Locale.forLanguageTag("en-US") };

		for (Locale locale : locales) {
			final TextAnalyzer analysis = new TextAnalyzer("DecimalSeparator");
			analysis.setLocale(locale);

			DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);

			Set<String> samples = new HashSet<String>();
			for (int i = 0; i < SAMPLE_SIZE; i++) {
				long l = random.nextInt(10000000);
				if (l % 2 == 0)
					l = -l;
				String sample = String.valueOf(l) + formatSymbols.getDecimalSeparator() + random.nextInt(10);

				samples.add(sample);
				analysis.train(sample);
			}

			final TextAnalysisResult result = analysis.getResult();

			Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
			Assert.assertEquals(result.getTypeQualifier(), "SIGNED");
			Assert.assertEquals(result.getSampleCount(), SAMPLE_SIZE);
			Assert.assertEquals(result.getMatchCount(), SAMPLE_SIZE);
			Assert.assertEquals(result.getNullCount(), 0);
			Assert.assertEquals(result.getLeadingZeroCount(), 0);
			Assert.assertEquals(result.getDecimalSeparator(), formatSymbols.getDecimalSeparator());

			String regExp = "-?\\d+|-?(\\d+)?" + Utils.slosh(formatSymbols.getDecimalSeparator()) + "\\d+";

			Assert.assertEquals(result.getRegExp(), regExp);
			Assert.assertEquals(result.getConfidence(), 1.0);

			for (String sample : samples) {
				Assert.assertTrue(sample.matches(regExp), sample + " " + regExp);
			}
		}
	}

	@Test
	public void monetaryDecimalSeparatorDefault() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Separator");
		final Random random = new Random();
		final int SAMPLE_SIZE = 10000;
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		String minValue = String.valueOf(min);
		String maxValue = String.valueOf(max);
		Set<String> samples = new HashSet<String>();

		NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setMinimumFractionDigits(1);
		for (int i = 0; i < SAMPLE_SIZE; i++) {
			double d = random.nextDouble();
			String sample = nf.format(d).toString();
			if (d < min) {
				min = d;
				minValue = sample;
			}
			if (d > max) {
				max = d;
				maxValue = sample;
			}
			samples.add(sample);
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), SAMPLE_SIZE);
		Assert.assertEquals(result.getMatchCount(), SAMPLE_SIZE);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 0);
		Assert.assertEquals(result.getMinValue(), minValue);
		Assert.assertEquals(result.getMaxValue(), maxValue);
		Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_DOUBLE));
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (String sample : samples) {
			Assert.assertTrue(sample.matches(analysis.getRegExp(KnownPatterns.ID.ID_DOUBLE)), sample);
		}
	}

	@Test
	public void monetaryDecimalSeparatorFrench() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Separator");
		analysis.setLocale(Locale.FRENCH);
		final Random random = new Random(314159265);
		final int SAMPLE_SIZE = 10000;
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		String minValue = String.valueOf(min);
		String maxValue = String.valueOf(max);
		Set<String> samples = new HashSet<String>();

		NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRENCH);
		nf.setMinimumFractionDigits(1);
		for (int i = 0; i < SAMPLE_SIZE; i++) {
			double d = random.nextDouble();
			String sample = nf.format(d).toString();
			if (d < min) {
				min = d;
				minValue = sample;
			}
			if (d > max) {
				max = d;
				maxValue = sample;
			}
			samples.add(sample);
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), SAMPLE_SIZE);
		Assert.assertEquals(result.getMatchCount(), SAMPLE_SIZE);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 0);
		Assert.assertEquals(result.getMinValue(), minValue);
		Assert.assertEquals(result.getMaxValue(), maxValue);
		Assert.assertEquals(result.getRegExp(), "\\d+|(\\d+)?,\\d+");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (String sample : samples) {
			Assert.assertTrue(sample.matches(result.getRegExp()), sample);
		}
	}

	@Test
	public void groupingSeparatorSigned() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Separator");
		final Random random = new Random(21456);
		final int SAMPLE_SIZE = 100;
		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;
		long absMin = Long.MAX_VALUE;
		long absMax = 0;
		String minValue = String.valueOf(Long.MAX_VALUE);
		String maxValue = "0";
		Set<String> samples = new HashSet<String>();

		for (int i = 0; i < SAMPLE_SIZE; i++) {
			long l = random.nextInt(100000000);
			if (random.nextBoolean())
				l *= -1;
			if (l < min) {
				min = l;
			}
			if (l > max) {
				max = l;
			}
			String sample = NumberFormat.getNumberInstance(Locale.US).format(l).toString();
			long pos = Math.abs(l);
			if (pos < absMin) {
				absMin = pos;
				minValue = NumberFormat.getNumberInstance(Locale.US).format(pos).toString();
			}
			if (pos > absMax) {
				absMax = pos;
				maxValue = NumberFormat.getNumberInstance(Locale.US).format(pos).toString();
			}
			samples.add(sample);
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getTypeQualifier(), "SIGNED,GROUPING");
		Assert.assertEquals(result.getSampleCount(), SAMPLE_SIZE);
		Assert.assertEquals(result.getMatchCount(), SAMPLE_SIZE);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 0);
		Assert.assertEquals(result.getMinValue(), String.valueOf(min));
		Assert.assertEquals(result.getMaxValue(), String.valueOf(max));
		String regExp = "-?[\\d,]{";
		if (minValue.length() == maxValue.length())
			regExp += minValue.length();
		else {
			regExp += minValue.length() + "," + maxValue.length();
		}
		regExp += "}";
		Assert.assertEquals(result.getRegExp(), regExp);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (String sample : samples) {
			Assert.assertTrue(sample.matches(regExp), sample);
		}
	}

	@Test
	public void testQualifierNumeric() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Numeric");
		analysis.setLengthQualifier(false);

		final int start = 10000;
		final int end = 99999;

		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "\\d+");
	}

	@Test
	public void testQualifierAlpha() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Alpha");
		final Random random = new Random(21456);
		final int STRING_LENGTH = 5;
		analysis.setLengthQualifier(false);
		String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

		final int start = 10000;
		final int end = 99999;

		int locked = -1;

		for (int i = start; i < end; i++) {
			StringBuilder sample = new StringBuilder(STRING_LENGTH);
			for (int j = 0; j < STRING_LENGTH; j++)
				sample.append(alphabet.charAt(random.nextInt(52)));
			if (analysis.train(sample.toString()) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "+");
	}

	@Test
	public void testQualifierAlphaNumeric() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("AlphaNumeric");
		analysis.setLengthQualifier(false);

		final int start = 10000;
		final int end = 99999;

		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train('A' + String.valueOf(i)) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHANUMERIC + "+");
	}

	@Test
	public void notZipButNumeric() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int start = 10000;
		final int end = 99999;

		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i;
		}
		analysis.train("No Zip provided");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, start + TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), end + 1 - start);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{5}");
		Assert.assertEquals(result.getMatchCount(), end - start);
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
	}

	@Test
	public void notZips() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int start = 10000;
		final int end = 99999;

		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train(i < 80000 ? String.valueOf(i) : "A" + String.valueOf(i)) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, start + TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), end - start);
		Assert.assertEquals(result.getMatchCount(), end - start);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHANUMERIC + "{5,6}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getMinValue(), "10000");
		Assert.assertEquals(result.getMaxValue(), "A99998");
	}

	final String validUSStates = "AL|AK|AZ|KY|KS|LA|ME|MD|MI|MA|MN|MS|MO|NE|MT|SD|TN|TX|UT|VT|WI|" +
			"VA|WA|WV|HI|ID|IL|IN|IA|KS|KY|LA|ME|MD|MA|MI|MN|MS|MO|MT|NE|NV|" +
			"NH|NJ|NM|NY|NC|ND|OH|OK|OR|PA|RI|SC|SD|TN|TX|UT|VT|VA|WA|WV|WI|" +
			"WY|AL|AK|AZ|AR|CA|CO|CT|DC|DE|FL|GA|HI|ID|IL|IN|IA|KS|KY|LA|ME|" +
			"MD|MA|MI|MN|MS|MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|OH|OK|OR|RI|SC|SD|" +
			"TX|UT|VT|WV|WI|WY|NV|NH|NJ|OR|PA|RI|SC|AR|CA|CO|CT|ID|HI|IL|IN|";
	@Test
	public void basicStates() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		final String inputs[] = validUSStates.split("\\|");
		int locked = -1;

		analysis.train("XX");
		analysis.train("XX");
		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i + 2;
		}
		analysis.train("XX");
		analysis.train("XX");
		analysis.train("XX");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "US_STATE");
		Assert.assertEquals(result.getSampleCount(), inputs.length + 5);
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\p{Alpha}{2}");
		Assert.assertEquals(result.getConfidence(), 1 - (double)5/result.getSampleCount());

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicStatesWithDash() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		final String input = "AL|AK|AZ|KY|KS|LA|ME|MD|MI|MA|MN|MS|MO|NE|MT|SD|TN|TX|UT|VT|WI|" +
				"VA|WA|WV|HI|ID|IL|IN|IA|KS|KY|LA|ME|MD|MA|MI|MN|MS|MO|MT|NE|NV|-|" +
				"NH|NJ|NM|NY|NC|ND|OH|OK|OR|PA|RI|SC|SD|TN|TX|UT|VT|VA|WA|WV|WI|-|" +
				"WY|AL|AK|AZ|AR|CA|CO|CT|DC|DE|FL|GA|HI|ID|IL|IN|IA|KS|KY|LA|ME|-|" +
				"MD|MA|MI|MN|MS|MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|OH|OK|OR|RI|SC|SD|-|" +
				"TX|UT|VT|WV|WI|WY|NV|NH|NJ|OR|PA|RI|SC|AR|CA|CO|CT|ID|HI|IL|IN|-|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "US_STATE");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), inputs.length - 5);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\p{Alpha}{2}");
		Assert.assertEquals(result.getConfidence(), 1 - (double)5/result.getSampleCount());

		for (int i = 0; i < inputs.length; i++) {
			if (!"-".equals(inputs[i]))
				Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicStates100Percent() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		analysis.setPluginThreshold(100);

		final String input = "AL|AK|AZ|KY|KS|LA|ME|MD|MI|MA|MN|MS|MO|NE|MT|SD|TN|TX|UT|VT|WI|" +
				"VA|WA|WV|HI|ID|IL|IN|IA|KS|KY|LA|ME|MD|MA|MI|MN|MS|MO|MT|NE|NV|XX|" +
				"NH|NJ|NM|NY|NC|ND|OH|OK|OR|PA|RI|SC|SD|TN|TX|UT|VT|VA|WA|WV|WI|XX|" +
				"WY|AL|AK|AZ|AR|CA|CO|CT|DC|DE|FL|GA|HI|ID|IL|IN|IA|KS|KY|LA|ME|XX|" +
				"MD|MA|MI|MN|MS|MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|OH|OK|OR|RI|SC|SD|XX|" +
				"TX|UT|VT|WV|WI|WY|NV|NH|NJ|OR|PA|RI|SC|AR|CA|CO|CT|ID|HI|IL|IN|XX|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicStatesLower() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "al|ak|az|ky|ks|la|me|md|mi|ma|mn|ms|mo|ne|mt|sd|tn|tx|ut|vt|wi|" +
				"va|wa|wv|hi|id|il|in|ia|ks|ky|la|me|md|ma|mi|mn|ms|mo|mt|ne|nv|" +
				"nh|nj|nm|ny|nc|nd|oh|ok|or|pa|ri|sc|sd|tn|tx|ut|vt|va|wa|wv|wi|" +
				"wy|al|ak|az|ar|ca|co|ct|dc|de|fl|ga|hi|id|il|in|ia|ks|ky|la|me|" +
				"md|ma|mi|mn|ms|mo|mt|ne|nv|nh|nj|nm|ny|nc|nd|oh|ok|or|ri|sc|sd|" +
				"tx|ut|vt|wv|wi|wy|nv|nh|nj|or|pa|ri|sc|ar|ca|co|ct|id|hi|il|in|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "US_STATE");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\p{Alpha}{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	final String validCAProvinces = "AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
			"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
			"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
			"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
			"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|";
	@Test
	public void basicCA() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String inputs[] = validCAProvinces.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "CA_PROVINCE");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\p{Alpha}{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void change2() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
				"Jan|Mar|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
				"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|Sep|Jan|Oct|Oct|Oct|" +
				"Jan|Mar|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
				"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|NA|Sep|Jan|Oct|Oct|Oct|" +
				"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{2,3}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicCountry() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "Venezuela|USA|Finland|USA|USA|Germany|France|Italy|Mexico|Germany|" +
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
				"Germany|USA|France|UK|UK|UK|Mexico|  Belgium  |Venezuela|Portugal|" +
				"France|USA|France|Brazil|USA|USA|UK|Venezuela|Venezuela|Brazil|" +
				"Germany|Austria|Venezuela|Portugal|Canada|France|Brazil|Canada|Brazil|Germany|" +
				"Venezuela|Venezuela|France|Germany|Mexico|Ireland|USA|Canada|Germany|Mexico|" +
				"Germany|Germany|USA|France|Brazil|Germany|Austria|Germany|Ireland|UK|Gondwanaland|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "COUNTRY_EN");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 1);
		final Map<String, Integer> outliers = result.getOutlierDetails();
		Assert.assertEquals(outliers.size(), 1);
		int outlierCount = outliers.get("GONDWANALAND");
		Assert.assertEquals(result.getMatchCount(), inputs.length - outlierCount);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), ".+");
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
	}

	// Set of valid months + 4 x "UNK"
	private final static String MONTH_TEST = "Jan|Mar|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
			"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|Sep|Jan|Oct|Oct|Oct|" +
			"Jan|Mar|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
			"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|Sep|Jan|Oct|Oct|Oct|" +
			"Jan|Mar|Jun|Jul|Feb|Dec|UNK|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
			"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|UNK|Sep|Jan|Oct|Oct|Oct|" +
			"Jan|UNK|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
			"Dec|Oct|Apr|May|May|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|Sep|Jan|Oct|Oct|Oct|" +
			"Jan|Mar|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
			"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|UNK|Sep|Jan|Oct|Oct|Oct|";

	@Test
	public void basicMonthAbbr() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int badCount = 4;
		final String inputs[] = MONTH_TEST.split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{3}");
		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "MONTHABBR");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 1);
		final Map<String, Integer> outliers = result.getOutlierDetails();
		Assert.assertEquals(outliers.size(), 1);
		Assert.assertEquals(outliers.get("UNK"), Integer.valueOf(4));
		Assert.assertEquals(result.getMatchCount(), inputs.length - badCount);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertTrue((double)analysis.getPluginThreshold()/100 < result.getConfidence());
		Assert.assertEquals(result.getConfidence(), 1 - (double)badCount/result.getSampleCount());

		// Even the UNK match the RE
		for (int i = 0; i < inputs.length; i++)
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));

/*
		analysis.train("Another bad element");
		result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "\\p{Alpha}{3}");
		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertTrue((double)analysis.getPluginThreshold()/100 < result.getConfidence());
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length + 1);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Map<String, Integer> updatedOutliers = result.getOutlierDetails();
		Assert.assertEquals(updatedOutliers.size(), 2);
		Assert.assertEquals(updatedOutliers.get("UNK"), Integer.valueOf(4));
		Assert.assertEquals(updatedOutliers.get("Another bad element"), Integer.valueOf(1));
		Assert.assertEquals(result.getMatchCount(), inputs.length - badCount);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1 - (double)(badCount + 1)/result.getSampleCount());
		*/
	}

	// Set of valid months + 4 x "UNK"
	private final static String MONTH_TEST_FRENCH =
			"janv.|févr.|mars|avr.|mai|juin|juil.|août|sept.|oct.|nov.|déc.|" +
					"janv.|févr.|mars|avr.|mai|juin|juil.|août|sept.|oct.|nov.|déc.|" +
					"janv.|févr.|mars|avr.|mai|juin|juil.|août|sept.|oct.|nov.|déc.|" +
					"janv.|févr.|mars|avr.|mai|juin|juil.|août|sept.|oct.|nov.|déc.|" +
					"janv.|févr.|mars|UNK|mai|juin|juil.|août|sept.|oct.|nov.|déc.|" +
					"janv.|févr.|mars|avr.|mai|juin|juil.|août|sept.|oct.|nov.|déc.|" +
					"janv.|févr.|mars|avr.|mai|UNK|juil.|août|sept.|oct.|nov.|déc.|" +
					"janv.|févr.|mars|UNK|mai|juin|juil.|août|sept.|UNK|nov.|déc.|";

	@Test
	public void basicMonthAbbrFrench() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		analysis.setLocale(Locale.FRENCH);
		final int badCount = 4;
		final String inputs[] = MONTH_TEST_FRENCH.split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "[\\p{IsAlphabetic}\\.]{3,5}");
		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "MONTHABBR");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 1);
		final Map<String, Integer> outliers = result.getOutlierDetails();
		Assert.assertEquals(outliers.size(), 1);
		Assert.assertEquals(outliers.get("UNK"), Integer.valueOf(4));
		Assert.assertEquals(result.getMatchCount(), inputs.length - badCount);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertTrue((double)analysis.getPluginThreshold()/100 < result.getConfidence());
		Assert.assertEquals(result.getConfidence(), 1 - (double)badCount/result.getSampleCount());

		// Even the UNK match the RE
		for (int i = 0; i < inputs.length; i++)
			Assert.assertTrue(inputs[i].matches(result.getRegExp()), inputs[i]);
	}

	@Test
	public void basicFrenchDate() throws IOException {

		Set<String> samples = new HashSet<String>();
		LocalDate localDate = LocalDate.now();

		final TextAnalyzer analysis = new TextAnalyzer();
		analysis.setLocale(Locale.FRANCE);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRANCE);

		int locked = -1;

		for (int i = 0; i < 100; i++) {
			String sample = localDate.format(formatter);
			samples.add(sample);
			if (analysis.train(sample) && locked == -1)
				locked = i;
			localDate = localDate.minusDays(100);
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "\\d{1,2} [\\p{IsAlphabetic}\\.]{3,5} \\d{4}");
		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "d MMM yyyy");
		Assert.assertEquals(result.getSampleCount(), samples.size());
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), samples.size());
		Assert.assertEquals(result.getNullCount(), 0);

		// Even the UNK match the RE
		for (String sample : samples)
			Assert.assertTrue(sample.matches(result.getRegExp()), sample);
	}

	static boolean isValidLocale(String value) {
		Locale[] locales = Locale.getAvailableLocales();
		for (Locale locale : locales) {
			if (value.equals(locale.toString())) {
				return true;
		    }
		}
		return false;
	}

	@Test
	public void basicBulgarianDate() throws IOException {

		Set<String> samples = new HashSet<String>();
		LocalDate localDate = LocalDate.now();

		if (!isValidLocale("bg_BG"))
			return;

		Locale bulgarian = Locale.forLanguageTag("bg-BG");

		final TextAnalyzer analysis = new TextAnalyzer();
		analysis.setLocale(bulgarian);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy", bulgarian);

		int locked = -1;

		for (int i = 0; i < 100; i++) {
			String sample = localDate.format(formatter);
			samples.add(sample);
			if (analysis.train(sample) && locked == -1)
				locked = i;
			localDate = localDate.minusDays(100);
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "\\d{1,2} \\p{IsAlphabetic}{1,4} \\d{4}");
		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "d MMM yyyy");
		Assert.assertEquals(result.getSampleCount(), samples.size());
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), samples.size());
		Assert.assertEquals(result.getNullCount(), 0);

		// Even the UNK match the RE
		for (String sample : samples)
			Assert.assertTrue(sample.matches(result.getRegExp()), sample);
	}

	@Test
	public void basicCatalanDate() throws IOException {

		Set<String> samples = new HashSet<String>();
		LocalDate localDate = LocalDate.now();

		if (!isValidLocale("ca_ES"))
			return;

		Locale catalan = Locale.forLanguageTag("ca-ES");

		final TextAnalyzer analysis = new TextAnalyzer();
		analysis.setLocale(catalan);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy", catalan);

		int locked = -1;

		for (int i = 0; i < 100; i++) {
			String sample = localDate.format(formatter);
			samples.add(sample);
			if (analysis.train(sample) && locked == -1)
				locked = i;
			localDate = localDate.minusDays(100);
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "\\d{1,2} .{5,8} \\d{4}");
		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "d MMM yyyy");
		Assert.assertEquals(result.getSampleCount(), samples.size());
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), samples.size());
		Assert.assertEquals(result.getNullCount(), 0);

		// Even the UNK match the RE
		for (String sample : samples)
			Assert.assertTrue(sample.matches(result.getRegExp()), sample);
	}

	@Test
	public void basicGermanDate() throws IOException {

		Set<String> samples = new HashSet<String>();
		LocalDate localDate = LocalDate.now();

		if (!isValidLocale("de_AT"))
			return;

		Locale german = Locale.forLanguageTag("de-AT");

		final TextAnalyzer analysis = new TextAnalyzer();
		analysis.setLocale(german);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy", german);

		int locked = -1;

		for (int i = 0; i < 100; i++) {
			String sample = localDate.format(formatter);
			samples.add(sample);
			if (analysis.train(sample) && locked == -1)
				locked = i;
			localDate = localDate.minusDays(100);
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "\\d{1,2} \\p{IsAlphabetic}{3} \\d{4}");
		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "d MMM yyyy");
		Assert.assertEquals(result.getSampleCount(), samples.size());
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), samples.size());
		Assert.assertEquals(result.getNullCount(), 0);

		// Even the UNK match the RE
		for (String sample : samples)
			Assert.assertTrue(sample.matches(result.getRegExp()), sample);
	}

	@Test
	public void trailingAM() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String inputs[] = new String[] {
				"02s500000023SQ3AAM", "02s5000000233ThAAI", "02s5000000238JRAAY", "02s500000023QCEAA2",
				"02s500000023QCFAA2", "02s500000023SKAAA2", "02s5000000233TgAAI", "02s500000023Sw9AAE",
				"02s500000023T0pAAE", "02s500000023U6FAAU", "02s500000023qQVAAY", "02s500000023qQWAAY",
				"02s500000023r2FAAQ", "02s500000023rFiAAI", "02s500000023x3qAAA", "02s50000002GgdtAAC",
				"02s50000002GgduAAC", "02s50000002GkKXAA0", "02s50000002GrukAAC", "02s50000002GrulAAC",
				"02s50000002GsLCAA0", "02s50000002HCnGAAW", "02s50000002HUaFAAW", "02s50000002HUaGAAW",
				"02s50000002HV82AAG", "02s50000002HjVvAAK", "02s50000002Hl4NAAS", "02s50000002HnXRAA0",
				"02s50000002Hq1sAAC", "02s50000002HrQPAA0", "02s50000002HrraAAC", "02s50000002HxoKAAS",
				"02s50000002I6lQAAS", "02s50000002I90MAAS", "02s50000002I93BAAS", "02s50000002I9CSAA0"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHANUMERIC + "{18}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}

	}

	@Test
	public void lenientDates() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("INACTIVE DATE");
		final String inputs[] = new String[] {
				"26/01/2018", "06/01/2018", "00/00/0000", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "00/00/0000", "00/00/0000", "06/01/2018",
				"06/01/2018", "06/01/2018", "00/00/0000", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "00/00/0000",
				"06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018",
				"06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "00/00/0000",
				"06/01/2018", "06/01/2018", "00/00/0000", "06/01/2018", "00/00/0000", "06/01/2018", "06/01/2018", "06/01/2018", "06/01/2018", "00/00/0000",
				"26/01/2018", "06/01/2018", "00/00/0000", "06/01/2018", "06/01/2018", "09/01/2018", "09/01/2018", "00/00/0000", "09/01/2018", "09/01/2018",
				"00/00/0000", "29/01/2018", "00/00/0000", "00/00/0000", "09/01/2018", "00/00/0000", "09/01/2018", "09/01/2018", "00/00/0000", "00/00/0000",
				"00/00/0000", "00/00/0000", "09/01/2018", "00/00/0000", "00/00/0000", "00/00/0000", "09/01/2018", "09/01/2018", "00/00/0000", "00/00/0000",
				"00/00/0000", "00/00/0000", "00/00/0000", "00/00/0000", "00/00/0000", "09/01/2018", "00/00/0000", "00/00/0000", "09/01/2018", "09/01/2018",
				"09/01/2018", "09/01/2018", "09/01/2018", "09/01/2018", "00/00/0000", "00/00/0000", "09/01/2018", "09/01/2018", "09/01/2018", "09/01/2018",
				"09/01/2018", "00/00/0000", "00/00/0000", "09/01/2018", "00/00/0000", "00/00/0000", "00/00/0000", "00/00/0000", "00/00/0000", "00/00/0000",
				"09/01/2018", "00/00/0000", "09/01/2018", "09/01/2018", "09/01/2018", "09/01/2018", "00/00/0000", "09/01/2018", "00/00/0000", "00/00/0000",
				"00/00/0000", "00/00/0000", "00/00/0000", "09/01/2018", "06/01/2018", "00/00/0000", "00/00/0000", "09/01/2018", "09/01/2018"
		};
		int locked = -1;
		int zeroes = 50;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "dd/MM/yyyy");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 1);
		final Map<String, Integer> outliers = result.getOutlierDetails();
		Assert.assertEquals(outliers.size(), 1);
		Assert.assertEquals(outliers.get("00/00/0000"), Integer.valueOf(zeroes));
		Assert.assertEquals(result.getMatchCount(), inputs.length - zeroes);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{2}/\\d{2}/\\d{4}");
		Assert.assertEquals(result.getConfidence(), 1 - (double)zeroes/result.getSampleCount());

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void localeDateTest() throws IOException {

		Locale[] locales = DateFormat.getAvailableLocales();
//		Locale[] locales = new Locale[] {Locale.forLanguageTag("lv-LV")};

		String testCases[] = {
				"yyyy MM dd", "yyyy MM dd", "yyyy M dd", "yyyy MM d", "yyyy M d",
				"dd MMM yyyy", "d MMM yyyy", "dd-MMM-yyyy", "d-MMM-yyyy", "dd/MMM/yyyy",
				"d/MMM/yyyy", "dd MMMM yyyy", "d MMMM yyyy", "dd-MMMM-yyyy", "d-MMMM-yyyy",
				"dd/MMMM/yyyy", "d/MMMM/yyyy", "dd MMM yy", "d MMM yy", "dd-MMM-yy",
				"d-MMM-yy", "dd/MMM/yy", "d/MMM/yy", "MMM dd',' yyyy", "MMM d',' yyyy",
				"MMM dd yyyy", "MMM d yyyy", "MMM-dd-yyyy", "MMM-d-yyyy", "MMMM dd',' yyyy",
				"MMMM d',' yyyy", "MMMM dd yyyy", "MMMM d yyyy", "MMMM-dd-yyyy", "MMMM-d-yyyy",
				"yyyyMMdd'T'HHmmss'Z'", "yyyyMMdd'T'HHmmss", "yyyyMMdd'T'HHmmssxx", "yyyyMMdd'T'HHmmssxx",
				"yyyyMMdd'T'HHmmss.SSSxx", "yyyyMMdd'T'HHmmss.SSSxx",
				"dd/MMM/yy h:mm a",
				"dd/MMM/yy hh:mm a",
				"EEE MMM dd HH:mm:ss z yyyy"
		};

		Set <String> problems = new HashSet<>();
		int countTests = 0;
		int countNotGregorian = 0;
		int countNotArabicNumerals = 0;
		int	countNoMonthAbbreviations = 0;
		int countProblems = 0;

		for (String testCase : testCases) {

			nextLocale:
			for (Locale locale : locales) {
				Set<String> samples = new HashSet<String>();
				LocalDate localDate = null;
				LocalDateTime localDateTime = null;
				OffsetDateTime offsetDateTime = null;
				ZonedDateTime zonedDateTime = null;

				String testID = locale.toLanguageTag() + " (" + testCase + ") ...";

				Calendar cal = GregorianCalendar.getInstance(locale);
				if (!(cal instanceof GregorianCalendar)) {
					countNotGregorian++;
					continue;
				}

				if (!NumberFormat.getNumberInstance(locale).format(0).matches("\\d")) {
					countNotArabicNumerals++;
					continue;
				}

				if (LocaleInfo.getShortMonths(locale).keySet().equals(LocaleInfo.getMonths(locale).keySet()) &&
					testCase.contains("MMMM")) {
					countNoMonthAbbreviations++;
					continue;
				}
				final TextAnalyzer analysis = new TextAnalyzer();

				// We do not like Japanese the Month Abbreviations are 1, 2, 3, 4, 5, ... which causes issues
				if ("Japanese".equals(locale.getDisplayLanguage()))
					continue;

//				System.err.println("TestCas " + testCase + ", Locale: " + locale + ", country: " + locale.getCountry() +
//						", language: " + locale.getDisplayLanguage() + ", name: " + locale.toLanguageTag());

				countTests++;

				analysis.setLocale(locale);

				DateTimeFormatter formatter = DateTimeFormatter.ofPattern(testCase, locale);
				PatternInfo.Type type = SimpleDateMatcher.getType(testCase, locale);

				if (type.equals(PatternInfo.Type.LOCALDATE))
					localDate = LocalDate.now();
				else if (type.equals(PatternInfo.Type.LOCALDATETIME))
					localDateTime = LocalDateTime.now();
				else if (type.equals(PatternInfo.Type.OFFSETDATETIME))
					offsetDateTime = OffsetDateTime.now();
				else if (type.equals(PatternInfo.Type.ZONEDDATETIME))
					zonedDateTime = ZonedDateTime.now();

				String sample = null;
				int locked = -1;

				for (int i = 0; i < 100; i++) {
					if (type.equals(PatternInfo.Type.LOCALDATE))
						sample = localDate.format(formatter);
					else if (type.equals(PatternInfo.Type.LOCALDATETIME))
						sample = localDateTime.format(formatter);
					else if (type.equals(PatternInfo.Type.OFFSETDATETIME))
						sample = offsetDateTime.format(formatter);
					else if (type.equals(PatternInfo.Type.ZONEDDATETIME))
						sample = zonedDateTime.format(formatter);

					samples.add(sample);
					if (analysis.train(sample) && locked == -1)
						locked = i;

					if (type.equals(PatternInfo.Type.LOCALDATE)) {
						localDate = localDate.minusDays(100);
					}
					else if (type.equals(PatternInfo.Type.LOCALDATETIME)) {
						localDateTime = localDateTime.minusDays(100).minusHours(1).minusMinutes(1).minusSeconds(1);
					}
					else if (type.equals(PatternInfo.Type.OFFSETDATETIME)) {
						offsetDateTime = offsetDateTime.minusDays(100).minusHours(1).minusMinutes(1).minusSeconds(1);
					}
					else if (type.equals(PatternInfo.Type.ZONEDDATETIME)) {
						zonedDateTime = zonedDateTime.minusDays(100).minusHours(1).minusMinutes(1).minusSeconds(1);
					}
				}

				TextAnalysisResult result = analysis.getResult();

				if (locked != TextAnalyzer.SAMPLE_DEFAULT) {
					problems.add(testID + "Locked incorrect: " + locked);
					countProblems++;
					continue;
				}
				if (!result.getType().equals(type)) {
					problems.add(testID + "Type incorrect: '" + result.getType() + "' '" + type + "'");
					countProblems++;
					continue;
				}
				if (!result.getTypeQualifier().equals(testCase)) {
					problems.add(testID + "TypeQualifer incorrect: '" + result.getTypeQualifier() + "'");
					countProblems++;
					continue;
				}
				if (result.getSampleCount() != samples.size()) {
					problems.add(testID + "Samples != Samples " + result.getSampleCount() + " " + samples.size());
					countProblems++;
					continue;
				}
				if (result.getOutlierCount() != 0) {
					problems.add(testID + "Outliers: " + result.getOutlierCount());
					countProblems++;
					continue;
				}
				if (result.getMatchCount() != samples.size()) {
					problems.add(testID + "Matches: " + result.getMatchCount());
					countProblems++;
					continue;
				}
				if (result.getNullCount() != 0) {
					problems.add(testID + "Nulls: " + result.getNullCount());
					countProblems++;
					continue;
				}
				Assert.assertEquals(result.getNullCount(), 0);

				// Even the UNK match the RE
				for (String s : samples)
					if (!s.matches(result.getRegExp())) {
						problems.add(testID + "RE: " + result.getRegExp() + ", !match: " + s);
						countProblems++;
						continue nextLocale;
					}
			}
		}

		for (String problem : problems) {
			System.err.println(problem);
		}

		System.err.printf("%d not Gregorian (skipped), %d not Arabic numerals, %d no Month abbr. (skipped), %d locales, %d failures (of %d tests)\n",
				countNotGregorian, countNotArabicNumerals, countNoMonthAbbreviations, locales.length, countProblems, countTests);

		Assert.assertEquals(countProblems, 0);
	}

	// Set of valid months + 4 x "UNK"
	private final static String MONTH_TEST_GERMAN =
			"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|UNK|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|UNK|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|UNK|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|Okt|Nov|Dez|" +
					"Jan|Feb|Mär|Apr|Mai|Jun|Jul|Aug|Sep|UNK|Nov|Dez|";

	@Test
	public void basicMonthAbbrGerman() throws IOException {

		if (!isValidLocale("de"))
			return;

		Locale german = Locale.forLanguageTag("de");

		final TextAnalyzer analysis = new TextAnalyzer();
		analysis.setLocale(german);

		final int badCount = 4;
		final String inputs[] = MONTH_TEST_GERMAN.split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{3}");
		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "MONTHABBR");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 1);
		final Map<String, Integer> outliers = result.getOutlierDetails();
		Assert.assertEquals(outliers.size(), 1);
		Assert.assertEquals(outliers.get("UNK"), Integer.valueOf(4));
		Assert.assertEquals(result.getMatchCount(), inputs.length - badCount);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertTrue((double)analysis.getPluginThreshold()/100 < result.getConfidence());
		Assert.assertEquals(result.getConfidence(), 1 - (double)badCount/result.getSampleCount());

		// Even the UNK match the RE
		for (int i = 0; i < inputs.length; i++)
			Assert.assertTrue(inputs[i].matches(result.getRegExp()), inputs[i]);
	}

	@Test
	public void basicMonthAbbrBackout() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String inputs[] = MONTH_TEST.split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		final int unknownCount = 10;
		for (int i = 0; i < unknownCount; i++)
			analysis.train("UNK");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{3}");
		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), result.getMatchCount());
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertTrue(inputs[0].matches(result.getRegExp()));

		int matches = 0;
		for (int i = 0; i < inputs.length; i++) {
			if (inputs[i].matches(result.getRegExp()))
					matches++;
		}
		Assert.assertEquals(result.getMatchCount() - unknownCount, matches);
	}

	@Test
	public void basicMonthAbbrExcessiveBad() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String inputs[] = MONTH_TEST.split("\\|");

		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}
		final int unknownCount = 10;
		for (int i = 0; i < unknownCount; i++)
			analysis.train("Bad");
		analysis.train("NA");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{2,3}");
		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length + unknownCount + 1);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length + unknownCount + 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertTrue(inputs[0].matches(result.getRegExp()));

		for (int i = 0; i < inputs.length; i++)
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
	}

	@Test
	public void frenchName() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "Adrien|Alain|Albert|Alexandre|Alexis|André|Antoine|Arnaud|Arthur|Aurélien|" +
				"Baptiste|Benjamin|Benoît|Bernard|Bertrand|Bruno|Cédric|Charles|Christian|Christophe|" +
				"Claude|Clément|Cyril|Damien|Daniel|David|Denis|Didier|Dominique|Dylan|" +
				"Emmanuel|Éric|Étienne|Enzo|Fabien|Fabrice|Florent|Florian|Francis|Franck|" +
				"François|Frédéric|Gabriel|Gaétan|Georges|Gérard|Gilbert|Gilles|Grégory|Guillaume|" +
				"Guy|Henri|Hervé|Hugo|Jacques|Jean|";
		//Jean-Claude|Jean-François|Jean-Louis|Jean-Luc|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{3,10}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()), inputs[i]);
		}
	}

	@Test
	public void basicStateLowCard() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("State", DateResolutionMode.DayFirst);
		final String input = "MA|MI|ME|MO|MS|";
		final String inputs[] = input.split("\\|");
		final int iters = 20;

		int locked = -1;

		for (int j = 0; j < iters; j++) {
			for (int i = 0; i < inputs.length; i++) {
				if (analysis.train(inputs[i]) && locked == -1)
					locked = i;
			}
		}
		final int UNKNOWN = 4;
		for (int k = 0; k < UNKNOWN; k++)
			analysis.train("NA");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "\\p{Alpha}{2}");
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "US_STATE");
		Assert.assertEquals(result.getSampleCount(), inputs.length * iters + UNKNOWN);
		Assert.assertEquals(result.getCardinality(), 5);
		Assert.assertEquals(result.getOutlierCount(), 1);
		final Map<String, Integer> outliers = result.getOutlierDetails();
		Assert.assertEquals(outliers.size(), 1);
		Assert.assertEquals(outliers.get("NA"), Integer.valueOf(UNKNOWN));
		Assert.assertEquals(result.getMatchCount(), inputs.length * iters);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1 - (double)UNKNOWN/result.getSampleCount());
	}

	@Test
	public void basicLengthValidationBlanks() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Spaces");
		final int iters = 30;
		Set<String> samples = new HashSet<String>();

		int locked = -1;

		StringBuilder sb = new StringBuilder("  ");

		for (int i = 0; i < iters; i++) {
			String s = sb.toString();
			samples.add(s);
			if (analysis.train(s) && locked == -1)
				locked = i;
			sb.append(' ');
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*");
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "BLANK");
		Assert.assertEquals(result.getSampleCount(), iters);
		Assert.assertEquals(result.getBlankCount(), iters);
		Assert.assertEquals(result.getCardinality(), 0);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), iters);
		Assert.assertEquals(result.getMinLength(), 2);
		Assert.assertEquals(result.getMaxLength(), 2 + iters - 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (String sample : samples) {
			if (sample.trim().length() > 0)
				Assert.assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test
	public void basicLengthValidationString() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Spaces");
		final int iters = 30;
		Set<String> samples = new HashSet<String>();

		int locked = -1;

		StringBuilder sb = new StringBuilder("  ");

		for (int i = 0; i < iters; i++) {
			String s = sb.toString();
			samples.add(s);
			if (analysis.train(s) && locked == -1)
				locked = i;
			sb.append(' ');
		}
		analysis.train("          abc          ");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*" + KnownPatterns.PATTERN_ALPHA + "{3}\\p{javaWhitespace}*");
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), iters + 1);
		Assert.assertEquals(result.getBlankCount(), iters);
		Assert.assertEquals(result.getCardinality(), 1);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), 1);
		Assert.assertEquals(result.getMinLength(), 2);
		Assert.assertEquals(result.getMaxLength(), 2 + iters - 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (String sample : samples) {
			if (sample.trim().length() > 0)
				Assert.assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test
	public void basicLengthValidationDate() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Spaces");
		final int iters = 30;
		Set<String> samples = new HashSet<String>();

		int locked = -1;

		for (int i = 0; i < iters; i++) {
			String s = String.format(" %02d/03/93", i);
			samples.add(s);
			if (analysis.train(s) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*\\d{2}/\\d{2}/\\d{2}");
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "dd/MM/yy");
		Assert.assertEquals(result.getSampleCount(), iters);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getCardinality(), iters - 1);
		Assert.assertEquals(result.getOutlierCount(), 1);
		Assert.assertEquals(result.getMatchCount(), iters - 1);
		Assert.assertEquals(result.getMinLength(), 9);
		Assert.assertEquals(result.getMaxLength(), 9);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());

		for (String sample : samples) {
			Assert.assertTrue(sample.matches(result.getRegExp()));
		}
	}

	@Test
	public void variableSpacesFixedLength() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("variableSpacesFixedLength");
		final String input = "JMD     |JOD     |JPYP    |KESQ    |KGS     |KHR     |" +
				" AXN    | AOAZ   | B1D    | BIFD   | BSD    | BZD    | CZE    | CHF    |" +
				"  MzR   |  NIO   |  P2N   |  PLN   |  RWF   |  SDG   |  SHP   |  SLL   |" +
				"   SVQ  |   SYP  |   S33Z |   THB  |   TOP  |   TZS  |   UYE  |   VND  |" +
				"    CQP |    COU |    CRC |    CUC |    DJF |    EGP |    GLP |    GMD |" +
				"     APT|     CSU|     44S|    LFUA|XXXXXXXX|     PER|     NAR|     ZMW|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*.{3,8}\\p{javaWhitespace}*");
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getCardinality(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getMinLength(), 8);
		Assert.assertEquals(result.getMaxLength(), 8);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicISO4127() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("CurrencyCode");
		final String input = "JMD|JOD|JPY|KES|KGS|KHR|KMF|KPW|KZT|LRD|MKD|MRU|" +
				"AFN|AOA|BBD|BIF|BSD|BZD|CHE|CHF|CHW|CLF|CLP|CNY|" +
				"MYR|NIO|PEN|PLN|RWF|SDG|SHP|SLL|SOS|SRD|SSP|STN|" +
				"SVC|SYP|SZL|THB|TOP|TZS|UYU|VND|XBA|XCD|XPD|XPF|" +
				"COP|COU|CRC|CUC|DJF|EGP|GBP|GMD|HRK|ILS|IRR|ISK|" +
				"XPT|XSU|XTS|XUA|XXX|YER|ZAR|ZMW|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "\\p{Alpha}{3}");
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "ISO-4217");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getCardinality(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getMinLength(), 3);
		Assert.assertEquals(result.getMaxLength(), 3);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	final String valid3166_3 = "ALA|ARM|BEL|BIH|BWA|BVT|BRA|IOT|BRN|BGR|BFA|" +
			"BDI|CPV|CYM|COG|DJI|ETH|GMB|GTM|HUN|JAM|KGZ|" +
			"LIE|LTU|LUX|MAC|MKD|MDG|MWI|MYS|MDV|MLI|MRT|" +
			"MAR|NER|PAN|REU|VCT|SXM|SDN|TLS|TKM|TCA|TUV|" +
			"UGA|UKR|ARE|GBR|UMI|USA|URY|VNM|";
	@Test
	public void basicISO3166_3() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("3166 Alpha-3");
		final String inputs[] = valid3166_3.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "\\p{Alpha}{3}");
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "ISO-3166-3");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getCardinality(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getMinLength(), 3);
		Assert.assertEquals(result.getMaxLength(), 3);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	final String valid3166_2 =  "AL|AW|BZ|BW|BV|BR|IO|BN|BG|" +
			"BF|BI|CV|KH|CF|CK|DM|FK|GE|" +
			"GG|IS|JP|LA|LT|LU|MO|MK|MG|" +
			"MW|MY|MV|ML|MT|MU|MZ|NG|PG|" +
			"RO|WS|SK|SR|TG|TC|TV|UG|UA|" +
			"AE|GB|UM|US|UY|UZ|VG|";
	@Test
	public void basicISO3166_2() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("3166 Alpha-2");
		final String inputs[] = valid3166_2.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), "\\p{Alpha}{2}");
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "ISO-3166-2");
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getCardinality(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getMinLength(), 2);
		Assert.assertEquals(result.getMaxLength(), 2);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	final String alpha3 = "aaa|bbb|ccc|ddd|eee|fff|ggg|hhh|iii|jjj|" +
			"aaa|iii|sss|sss|sss|vvv|jjj|jjj|jjj|bbb|iii|uuu|bbb|bbb|vvv|mmm|uuu|fff|vvv|fff|" +
			"iii|ééé|iii|ggg|bbb|sss|mmm|uuu|sss|uuu|aaa|iii|sss|sss|sss|vvv|jjj|jjj|jjj|bbb|" +
			"iii|uuu|bbb|bbb|vvv|mmm|uuu|fff|vvv|fff|iii|bbb|iii|ggg|bbb|sss|mmm|uuu|sss|uuu|" +
			"aaa|iii|sss|sss|sss|vvv|jjj|jjj|jjj|ççç|iii|uuu|bbb|bbb|vvv|mmm|uuu|fff|vvv|fff|" +
			"iii|bbb|iii|ggg|bbb|sss|mmm|uuu|sss|uuu|aaa|iii|sss|sss|sss|vvv|jjj|jjj|jjj|bbb|" +
			"iii|uuu|bbb|bbb|vvv|mmm|uuu|fff|vvv|fff|iii|bbb|iii|ggg|bbb|sss|mmm|uuu|sss|uuu|" +
			"mmm|iii|uuu|fff|ggg|ggg|uuu|uuu|uuu|uuu|";
	final String number3 = "111|123|707|902|104|223|537|902|111|443|" +
			"121|234|738|902|002|431|679|093|124|557|886|631|235|569|002|149|963|271|905|501|" +
			"171|734|038|002|882|215|875|193|214|997|126|361|098|888|314|111|222|341|458|082|" +
			"371|334|438|442|782|715|775|893|314|337|326|781|984|349|534|888|654|841|158|182|" +
			"098|123|435|000|312|223|343|563|123|";

	@Test
	public void constantLength3_alpha() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String inputs[] = alpha3.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{3}");
		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void constantLength3_alnum() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String inputs[] = (alpha3 + number3).split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHANUMERIC + "{3}");
		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void constantLength3_numal() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String inputs[] = (number3 + alpha3).split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHANUMERIC + "{3}");
		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicPromoteToDouble() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input =
					"8|172.67|22.73|150|30.26|54.55|45.45|433.22|172.73|7.73|" +
						"218.18|47.27|31.81|22.73|21.43|7.27|26.25|7.27|45.45|80.91|" +
						"63.64|13.64|45.45|15|425.45|95.25|60.15|100|80|72.73|" +
						"0.9|181.81|90|545.45|33.68|13.68|12.12|15|615.42|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_DOUBLE));
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicEnum() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "APARTMENT|APARTMENT|DUPLEX|APARTMENT|DUPLEX|CONDO|DUPLEX|CONDO|" +
				"DUPLEX|DUPLEX|CONDO|CONDODUPLEX|DUPLEX|CONDO|APARTMENT|" +
				"DUPLEX|CONDO|CONDO|CONDO|DUPLEX|DUPLEX|DUPLEX|DUPLEX|CONDO|" +
				"DUPLEX|DUPLEX|APARTMENT|CONDO|DUPLEX|CONDO|APARTMENT|APARTMENT|DUPLEX|" +
				"DUPLEX|APARTMENT|APARTMENT|APARTMENT|CONDO|CONDO|APARTMENT|CONDO|DUPLEX|" +
				"DUPLEX|CONDO|APARTMENT|DUPLEX|CONDO|DUPLEX|APARTMENT|CONDO|APARTMENT|" +
				"CONDO|CONDO|CONDO|CONDO|MULTI-FAMILY|DUPLEX|APARTMENT|MULTI-FAMILY|DUPLEX|" +
				"CONDO|APARTMENT|APARTMENT|CONDO|CONDO|MULTI-FAMILY|DUPLEX|CONDO|APARTMENT|" +
				"CONDO|DUPLEX|APARTMENT|CONDO|DUPLEX|DUPLEX|APARTMENT|APARTMENT|APARTMENT|" +
				"APARTMENT|APARTMENT|APARTMENT|CONDO|CONDO|APARTMENT|APARTMENT|CONDO|APARTMENT|" +
				"CONDO|APARTMENT|CONDO|APARTMENT|DUPLEX|CONDO|APARTMENT|APARTMENT|DUPLEX|" +
				"CONDO|APARTMENT|APARTMENT|DUPLEX|DUPLEX|CONDO|APARTMENT|CONDO|APARTMENT|" +
				"APARTMENT|CONDO|APARTMENT|CONDO|DUPLEX|MULTI-FAMILY|DUPLEX|CONDO|DUPLEX|" +
				"CONDO|APARTMENT|CONDO|DUPLEX|MULTI-FAMILY|APARTMENT|CONDO|DUPLEX|DUPLEX|" +
				"MULTI-FAMILY|APARTMENT|APARTMENT|APARTMENT|DUPLEX|APARTMENT|CONDO|CONDO|DUPLEX|" +
				"DUPLEX|DUPLEX|APARTMENT|DUPLEX|APARTMENT|DUPLEX|DUPLEX|DUPLEX|CONDO|" +
				"CONDO|APARTMENT|APARTMENT|APARTMENT|DUPLEX|APARTMENT|CONDO|MULTI-FAMILY|CONDO|" +
				"APARTMENT|DUPLEX|DUPLEX|MULTI-FAMILY|MULTI-FAMILY|DUPLEX|DUPLEX|DUPLEX|APARTMENT|" +
				"APARTMENT|DUPLEX|APARTMENT|DUPLEX|APARTMENT|APARTMENT|CONDO|CONDO|CONDO|" +
				"CONDO|DUPLEX|CONDO|MULTI-FAMILY|CONDO|CONDO|APARTMENT|CONDO|APARTMENT|" +
				"CONDO|APARTMENT|APARTMENT|CONDO|DUPLEX|APARTMENT|APARTMENT|APARTMENT|CONDO|" +
				"CONDO|CONDO|DUPLEX|DUPLEX|APARTMENT|CONDO|DUPLEX|DUPLEX|APARTMENT|" +
				"APARTMENT|CONDO|DUPLEX|APARTMENT|CONDO|CONDO|DUPLEX|CONDO|CONDO|" +
				"DUPLEX|CONDO|APARTMENT|DUPLEX|CONDO|CONDO|APARTMENT|DUPLEX|DUPLEX|" +
				"CONDO|APARTMENT|APARTMENT|CONDO|APARTMENT|DUPLEX|CONDO|APARTMENT|MULTI-FAMILY|" +
				"DUPLEX|CONDO|APARTMENT|APARTMENT|CONDO|APARTMENT|MULTI-FAMILY|CONDO|DUPLEX|" +
				"DUPLEX|CONDO|DUPLEX|DUPLEX|DUPLEX|DUPLEX|CONDO|CONDO|CONDO|" +
				"APARTMENT|CONDO|APARTMENT|DUPLEX|APARTMENT|APARTMENT|APARTMENT|DUPLEX|APARTMENT|" +
				"DUPLEX|APARTMENT|APARTMENT|APARTMENT|DUPLEX|DUPLEX|DUPLEX|CONDO|CONDO|" +
				"DUPLEX|CONDO|CONDO|APARTMENT|CONDO|APARTMENT|APARTMENT|APARTMENT|CONDO|" +
				"CONDO|CONDO|DUPLEX|CONDO|APARTMENT|CONDO|DUPLEX|DUPLEX|APARTMENT|" +
				"CONDO|APARTMENT|DUPLEX|DUPLEX|MULTI-FAMILY|DUPLEX|DUPLEX|DUPLEX|DUPLEX|" +
				"DUPLEX|DUPLEX|APARTMENT|DUPLEX|CONDO|APARTMENT|APARTMENT|MULTI-FAMILY|DUPLEX|" +
				"APARTMENT|APARTMENT|CONDO|CONDO|DUPLEX|CONDO|DUPLEX|DUPLEX|DUPLEX|" +
				"APARTMENT|CONDO|CONDO|CONDO|APARTMENT|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), ".{5,12}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicPromote() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input =
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
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHANUMERIC + "{8,12}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void basicText() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int iterations = 1000;
		int locked = -1;

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

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT/4);
		Assert.assertEquals(result.getSampleCount(), 5 * iterations + 1);
		Assert.assertEquals(result.getNullCount(), iterations);
		Assert.assertEquals(result.getCardinality(), 5);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{7,9}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void manyRandomInts() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int nullIterations = 50;
		final int iterations = 10000;
		final Random random = new Random();
		int locked = -1;

		for (int i = 0; i < nullIterations; i++) {
			analysis.train(null);
		}
		for (int i = 0; i < iterations; i++) {
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), iterations + nullIterations);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), nullIterations);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void textBlocks() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int iterations = 100000;
		final Random random = new Random(478031);
		int locked = -1;
		StringBuilder line = new StringBuilder();
		int minLength = Integer.MAX_VALUE;
		int maxLength = Integer.MIN_VALUE;
		String alphabet = "abcdefhijklmnopqrstuvwxyz";

		for (int i = 0; i < iterations; i++) {
			line.setLength(0);
			int wordCount = random.nextInt(20);
			for (int words = 0; words < wordCount; words++) {
				int charCount = random.nextInt(10);
				for (int chars = 0; chars < charCount; chars++) {
					line.append(alphabet.charAt(random.nextInt(25)));
				}
				line.append(' ');
			}
			String sample = line.toString().trim();
			int len = sample.length();
			if (len > maxLength)
				maxLength = len;
			if (len < minLength && len != 0)
				minLength = len;
			if (analysis.train(sample) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), iterations);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getRegExp(), ".{" + minLength + "," + maxLength + "}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void someInts() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final Random random = new Random();
		int minLength = Integer.MAX_VALUE;
		int maxLength = Integer.MIN_VALUE;
		int locked = -1;
		int samples;
		int bad = 0;

		for (samples = 0; samples <= TextAnalyzer.SAMPLE_DEFAULT; samples++) {
			final String input = String.valueOf(random.nextInt(1000000));
			final int len = input.length();
			if (len < minLength)
				minLength = len;
			if (len > maxLength)
				maxLength = len;
			if (analysis.train(input) && locked == -1)
				locked = samples;
		}
		while (samples++ < analysis.getReflectionSampleSize() - 1) {
			analysis.train(String.valueOf(random.nextDouble()));
			bad++;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), samples - 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		String pattern = "\\d{" + minLength;
		if (maxLength != minLength) {
			pattern += "," + maxLength;
		}
		pattern += "}";
		Assert.assertEquals(result.getRegExp(), pattern);
		Assert.assertEquals(result.getConfidence(), 1 - (double)bad/result.getSampleCount());
	}

	@Test
	public void floatBug() throws IOException {
		String[] samples = new String[] {
				"352115", "277303", "324576", "818328", "698915", "438223", "104583", "355898", "387829", "130771",
				"246823", "833969", "380632", "467021", "869890", "15191", "463747", "847192", "706545", "895018",
				"311867",
				"0.4704197792748601", "0.8005132170623999", "0.015796397806505325", "0.9830897509338489", "0.669847612276236",
				"0.9325644671976738", "0.5373506913452817", "0.21823369307871965", "0.1699104680573703", "0.18275707526552865",
				"0.24983460286935077", "0.772409965970719", "1.1388812589363528E-4", "0.78120115126727", "0.6386556468768979",
				"0.8730028156182696", "0.8296568674820993", "0.3250682023283127", "0.7261517112855164", "0.09470135380197953" };
		final TextAnalyzer analysis = new TextAnalyzer();
		int locked = -1;
		int sample = 0;

		analysis.setDetectWindow(2* TextAnalyzer.SAMPLE_DEFAULT);
		for (int i = 0; i <= TextAnalyzer.SAMPLE_DEFAULT; i++) {
			if (analysis.train(samples[sample++]) && locked == -1)
				locked = sample;
		}
		for (int i = 0; i < TextAnalyzer.SAMPLE_DEFAULT; i++) {
			if (analysis.train(samples[sample++]) && locked == -1)
				locked = sample;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, 2 * TextAnalyzer.SAMPLE_DEFAULT + 1);
		Assert.assertEquals(result.getSampleCount(), 2 * TextAnalyzer.SAMPLE_DEFAULT + 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_DOUBLE_WITH_EXPONENT));
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < samples.length; i++) {
			Assert.assertTrue(samples[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void signedFloatBug() throws IOException {
		String[] samples = new String[] {
				"352115", "277303", "324576", "818328", "698915", "438223", "104583", "355898", "387829", "130771",
				"246823", "833969", "380632", "467021", "869890", "15191", "463747", "847192", "706545", "895018",
				"311867",
				"0.4704197792748601", "-0.8005132170623999", "0.015796397806505325", "0.9830897509338489", "0.669847612276236",
				"0.9325644671976738", "0.5373506913452817", "0.21823369307871965", "0.1699104680573703", "0.18275707526552865",
				"0.24983460286935077", "0.772409965970719", "1.1388812589363528E+4", "0.78120115126727", "0.6386556468768979",
				"0.8730028156182696", "0.8296568674820993", "0.3250682023283127", "0.7261517112855164", "0.09470135380197953" };
		final TextAnalyzer analysis = new TextAnalyzer();
		int locked = -1;
		int sample = 0;

		analysis.setDetectWindow(2* TextAnalyzer.SAMPLE_DEFAULT);
		for (int i = 0; i <= TextAnalyzer.SAMPLE_DEFAULT; i++) {
			if (analysis.train(samples[sample++]) && locked == -1)
				locked = sample;
		}
		for (int i = 0; i < TextAnalyzer.SAMPLE_DEFAULT; i++) {
			if (analysis.train(samples[sample++]) && locked == -1)
				locked = sample;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, 2 * TextAnalyzer.SAMPLE_DEFAULT + 1);
		Assert.assertEquals(result.getSampleCount(), 2 * TextAnalyzer.SAMPLE_DEFAULT + 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertEquals(result.getTypeQualifier(), "SIGNED");
		Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_SIGNED_DOUBLE_WITH_EXPONENT));
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < samples.length; i++) {
			Assert.assertTrue(samples[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void setSampleSize() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final Random random = new Random();
		int locked = -1;
		int sample = 0;

		analysis.setDetectWindow(2* TextAnalyzer.SAMPLE_DEFAULT);
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

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, 2 * TextAnalyzer.SAMPLE_DEFAULT + 1);
		Assert.assertEquals(result.getSampleCount(), 2 * (TextAnalyzer.SAMPLE_DEFAULT + 1));
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void getSampleSize()  throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		Assert.assertEquals(analysis.getSampleSize(), TextAnalyzer.SAMPLE_DEFAULT);
	}

	@Test
	public void setSampleSizeTooSmall() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		try {
			analysis.setDetectWindow(TextAnalyzer.SAMPLE_DEFAULT - 1);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals(e.getMessage(), "Cannot set sample size below " + TextAnalyzer.SAMPLE_DEFAULT);
			return;
		}
		Assert.fail("Exception should have been thrown");
	}

	@Test
	public void setSampleSizeTooLate() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final Random random = new Random();
		int locked = -1;
		int i = 0;

		for (; i <= TextAnalyzer.SAMPLE_DEFAULT; i++) {
			if (analysis.train(String.valueOf(random.nextInt(1000000))) && locked == -1)
				locked = i;
		}

		try {
			analysis.setDetectWindow(2* TextAnalyzer.SAMPLE_DEFAULT);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals(e.getMessage(), "Cannot change sample size once training has started");
			return;
		}
		Assert.fail("Exception should have been thrown");
	}

	@Test
	public void getMaxCardinality() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		Assert.assertEquals(analysis.getMaxCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
	}

	@Test
	public void setMaxCardinalityTooSmall() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

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
		final TextAnalyzer analysis = new TextAnalyzer();
		final Random random = new Random();
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
		final TextAnalyzer analysis = new TextAnalyzer();

		Assert.assertEquals(analysis.getMaxOutliers(), TextAnalyzer.MAX_OUTLIERS_DEFAULT);
	}

	@Test
	public void setMaxOutliersTooSmall() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

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
		final TextAnalyzer analysis = new TextAnalyzer();
		final Random random = new Random();
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
	public void manyRandomDoubles() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int nullIterations = 50;
		final int iterations = 10000;
		final Random random = new Random();
		int locked = -1;

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

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), iterations + nullIterations + 3);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), nullIterations);
		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getOutlierCount(), 1);
		final Map<String, Integer> outliers = result.getOutlierDetails();
		Assert.assertEquals(outliers.size(), 1);
		Assert.assertEquals(outliers.get("Zoomer"), Integer.valueOf(1));
	}

	@Test
	public void manyConstantLengthStrings() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int nullIterations = 50;
		final int iterations = 10000;
		final Random random = new Random();
		final int length = 12;
		final String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		final StringBuilder b = new StringBuilder(length);
		int locked = -1;

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

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), iterations + nullIterations);
		//		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), nullIterations);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHA + "{12}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void manyConstantLengthLongs() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int nullIterations = 50;
		final int iterations = 10000;
		final Random random = new Random();
		int locked = -1;

		for (int i = 0; i < nullIterations; i++) {
			analysis.train(null);
		}
		int cnt = 0;
		while (cnt < iterations) {
			final long randomLong = random.nextInt(Integer.MAX_VALUE) + 1000000000L;
			if (randomLong >  9999999999L)
				continue;
			if (analysis.train(String.valueOf(randomLong)) && locked == -1)
				locked = cnt;
			cnt++;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), iterations + nullIterations);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), nullIterations);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getRegExp(), "\\d{10}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void manyConstantLengthDoublesI18N_1() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int nullIterations = 50;
		final int iterations = 10000;
		final Random random = new Random();
		int locked = -1;
		Locale locale = Locale.forLanguageTag("de-AT");
		analysis.setLocale(locale);
		DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);
		char grpSep = formatSymbols.getDecimalSeparator();
		final Set<String> samples = new HashSet<>();

		for (int i = 0; i < nullIterations; i++) {
			analysis.train(null);
		}
		int cnt = 0;
		while (cnt < iterations) {
			final long randomLong = random.nextInt(Integer.MAX_VALUE) + 1000000000L;
			if (randomLong >  9999999999L)
				continue;
			String sample = String.valueOf(randomLong) + formatSymbols.getDecimalSeparator() + random.nextInt(10);
			samples.add(sample);
			if (analysis.train(sample) && locked == -1)
				locked = cnt;
			cnt++;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), iterations + nullIterations);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), nullIterations);
		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertEquals(result.getRegExp(), "\\d+|(\\d+)?" + Utils.slosh(grpSep) + "\\d+");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	//@Test
	public void manyConstantLengthDoublesI18N_2() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int nullIterations = 50;
		final int iterations = 10000;
		final Random random = new Random();
		int locked = -1;
		Locale locale = Locale.forLanguageTag("de-DE");
		analysis.setLocale(locale);
		final Set<String> samples = new HashSet<>();

		for (int i = 0; i < nullIterations; i++) {
			analysis.train(null);
		}
		int cnt = 0;
		while (cnt < iterations) {
			final long randomLong = random.nextInt(Integer.MAX_VALUE) + 1000000000L;
			if (randomLong >  9999999999L)
				continue;
			String sample = String.valueOf(randomLong) + "." + random.nextInt(10);
			samples.add(sample);
			if (analysis.train(sample) && locked == -1)
				locked = cnt;
			cnt++;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), iterations + nullIterations);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), nullIterations);
		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertEquals(result.getRegExp(), "\\d+|(\\d+)?" + Utils.slosh('.') + "\\d+");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void possibleSSN() throws IOException {
		final Random random = new Random(314159265);
		String[] samples = new String[1000];

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < samples.length; i++) {
			b.setLength(0);
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append('-');
			b.append(String.format("%02d", random.nextInt(100)));
			b.append('-');
			b.append(String.format("%04d", random.nextInt(10000)));
			samples[i] = b.toString();
		}


		final TextAnalyzer analysis = new TextAnalyzer();
		analysis.setLengthQualifier(false);
		for (String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), samples.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getRegExp(), "\\d{3}-\\d{2}-\\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < samples.length; i++) {
			Assert.assertTrue(samples[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void USPhone() throws IOException {
		final Random random = new Random(314159265);
		String[] samples = new String[1000];

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < samples.length; i++) {
			b.setLength(0);
			b.append("+1 ");
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append(' ');
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append(' ');
			b.append(String.format("%04d", random.nextInt(10000)));
			samples[i] = b.toString();
		}


		final TextAnalyzer analysis = new TextAnalyzer();
		for (String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), samples.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getRegExp(), "\\+\\d{1} \\d{3} \\d{3} \\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < samples.length; i++) {
			Assert.assertTrue(samples[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void USPhone2() throws IOException {
		final Random random = new Random(314159265);
		String[] samples = new String[1000];

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < samples.length; i++) {
			b.setLength(0);
			b.append("1.");
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append('.');
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append('.');
			b.append(String.format("%04d", random.nextInt(10000)));
			samples[i] = b.toString();
		}


		final TextAnalyzer analysis = new TextAnalyzer();
		for (String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), samples.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getRegExp(), "\\d{1}\\.\\d{3}\\.\\d{3}\\.\\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < samples.length; i++) {
			Assert.assertTrue(samples[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void USPhone3() throws IOException {
		final Random random = new Random(314159265);
		String[] samples = new String[1000];

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < samples.length; i++) {
			b.setLength(0);
			b.append("(");
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append(") ");
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append(' ');
			b.append(String.format("%04d", random.nextInt(10000)));
			samples[i] = b.toString();
		}


		final TextAnalyzer analysis = new TextAnalyzer();
		for (String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), samples.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getRegExp(), "\\(\\d{3}\\) \\d{3} \\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < samples.length; i++) {
			Assert.assertTrue(samples[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void difficultRegExp() throws IOException {
		final Random random = new Random(314159265);
		String[] samples = new String[1000];

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < samples.length; i++) {
			b.setLength(0);
			b.append("[");
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append("){[0-9] ^");
			b.append(String.format("%03d", random.nextInt(1000)));
			b.append('$');
			b.append(String.format("%04d", random.nextInt(10000)));
			samples[i] = b.toString();
		}


		final TextAnalyzer analysis = new TextAnalyzer();
		for (String sample : samples) {
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), samples.length);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getRegExp(), "\\[\\d{3}\\)\\{\\[\\d{1}-\\d{1}\\] \\^\\d{3}\\$\\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < samples.length; i++) {
			Assert.assertTrue(samples[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void bumpMaxCardinality() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		analysis.setMaxCardinality(2 * TextAnalyzer.MAX_CARDINALITY_DEFAULT);

		final Random random = new Random();
		final int nullIterations = 50;
		final int iterations = 10000;
		int locked = -1;

		for (int i = 0; i < nullIterations; i++) {
			analysis.train(null);
		}
		int cnt = 0;
		while (cnt < iterations) {
			final long randomLong = random.nextInt(Integer.MAX_VALUE) + 1000000000L;
			if (randomLong >  9999999999L)
				continue;
			if (analysis.train(String.valueOf(randomLong)) && locked == -1)
				locked = cnt;
			cnt++;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), iterations + nullIterations);
		Assert.assertEquals(result.getCardinality(), 2 * TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), nullIterations);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getRegExp(), "\\d{10}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void manyKnownInts() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int nullIterations = 50;
		final int iterations = 100000;
		int locked = -1;

		for (int i = 0; i < nullIterations; i++) {
			analysis.train(null);
		}
		for (int i = 0; i < iterations; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i;
		}
		analysis.train("  ");
		analysis.train("    ");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), iterations + nullIterations + 2);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), nullIterations);
		Assert.assertEquals(result.getBlankCount(), 2);
		Assert.assertEquals(result.getRegExp(), "\\d{1,5}");
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getMinValue(), "0");
		Assert.assertEquals(result.getMaxValue(), String.valueOf(iterations - 1));
	}

	String validUSStreets[] = new String[] {
			"9885 Princeton Court",
			"11 San Pablo Rd.",
			"365 3rd St.",
			"426 Brewery Street",
			"676 Thatcher St.",
			"848 Hawthorne St.",
			"788 West Coffee St.",
			"240 Arnold Avenue",
			"25 S. Hawthorne St.",
			"9314 Rose Street",
			"32 West Bellevue St.",
			"8168 Thomas Road",
			"353 Homewood Ave.",
			"14 North Cambridge Street",
			"30 Leeton Ridge Drive",
			"8412 North Mulberry Dr.",
			"7691 Beacon Street",
			"187 Lake View Drive",
			"318 Summerhouse Road",
			"609 Taylor Ave.",
			"47 Broad St.",
			"525 Valley View St.",
			"8 Greenview Ave.",
			"86 North Helen St.",
			"8763 Virginia Street",
			"10 Front Avenue",
			"141 Blue Spring Street",
			"99 W. Airport Ave.",
			"32 NW. Rocky River Ave.",
			"324 North Lancaster Dr."
	};

	String validUSStreets2[] = new String[] {
			"6649 N Blue Gum St",
			"4 B Blue Ridge Blvd",
			"8 W Cerritos Ave #54",
			"639 Main St",
			"34 Center St",
			"3 Mcauley Dr",
			"7 Eads St",
			"7 W Jackson Blvd",
			"5 Boston Ave #88",
			"228 Runamuck Pl #2808",
			"2371 Jerrold Ave",
			"37275 St  Rt 17m M",
			"25 E 75th St #69",
			"98 Connecticut Ave Nw",
			"56 E Morehead St",
			"73 State Road 434 E",
			"69734 E Carrillo St",
			"322 New Horizon Blvd",
			"1 State Route 27",
			"394 Manchester Blvd",
			"6 S 33rd St",
			"6 Greenleaf Ave",
			"618 W Yakima Ave",
			"74 S Westgate St",
			"3273 State St",
			"1 Central Ave",
			"86 Nw 66th St #8673",
			"2 Cedar Ave #84",
			"90991 Thorburn Ave",
			"386 9th Ave N",
			"74874 Atlantic Ave",
			"366 South Dr",
			"45 E Liberty St",
			"4 Ralph Ct",
			"2742 Distribution Way",
			"426 Wolf St",
			"128 Bransten Rd",
			"17 Morena Blvd",
			"775 W 17th St",
			"6980 Dorsett Rd",
			"2881 Lewis Rd",
			"7219 Woodfield Rd",
			"1048 Main St",
			"678 3rd Ave",
			"20 S Babcock St",
			"2 Lighthouse Ave",
			"38938 Park Blvd",
			"5 Tomahawk Dr",
			"762 S Main St",
	};

	String validUSAddresses[] = new String[] {
			"9885 Princeton Court Shakopee, MN 55379",
			"11 San Pablo Rd.  Nottingham, MD 21236",
			"",
			"365 3rd St.  Woodhaven, NY 11421",
			"426 Brewery Street Horn Lake, MS 38637",
			"676 Thatcher St.  Hagerstown, MD 21740",
			"848 Hawthorne St.  Rockaway, NJ 07866",
			"788 West Coffee St.  Abingdon, MD 21009",
			"240 Arnold Avenue Yorktown Heights, NY 10598",
			"25 S. Hawthorne St.  Elizabeth City, NC 27909",
			"9314 Rose Street Holyoke, MA 01040",
			"32 West Bellevue St.  Holly Springs, NC 27540",
			"8168 Thomas Road El Dorado, AR 71730",
			"353 Homewood Ave.  Poughkeepsie, NY 12601",
			"14 North Cambridge Street Anchorage, AK 99504",
			"30 Leeton Ridge Drive Bristol, CT 06010",
			"8412 North Mulberry Dr.  Tiffin, OH 44883",
			"7691 Beacon Street Marysville, OH 43040",
			"187 Lake View Drive Redford, MI 48239",
			"318 Summerhouse Road Lenoir, NC 28645",
			"",
			"609 Taylor Ave.  Fort Myers, FL 33905",
			"47 Broad St.  Baldwin, NY 11510",
			"525 Valley View St.  Natick, MA 01760",
			"8 Greenview Ave.  Lithonia, GA 30038",
			"86 North Helen St.  Clermont, FL 34711",
			"8763 Virginia Street Hyattsville, MD 20782",
			"10 Front Avenue Brookline, MA 02446",
			"141 Blue Spring Street Ocoee, FL 34761",
			"99 W. Airport Ave.  Eau Claire, WI 54701",
			"32 NW. Rocky River Ave.  Raeford, NC 28376",
			"324 North Lancaster Dr.  Wyoming, MI 49509"
	};

	@Test
	public void basicUSStreet() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		for (String s : validUSStreets) {
			analysis.train(s);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), validUSStreets.length);
		Assert.assertEquals(result.getCardinality(), validUSStreets.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "ADDRESS_EN");
		Assert.assertEquals(result.getRegExp(), ".+");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void basicUSStreet2() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		for (String s : validUSStreets2) {
			analysis.train(s);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), validUSStreets2.length);
		Assert.assertEquals(result.getCardinality(), validUSStreets2.length);
		Assert.assertEquals(result.getMatchCount(), validUSStreets2.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertEquals(result.getTypeQualifier(), "ADDRESS_EN");
		Assert.assertEquals(result.getRegExp(), ".+");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void keyFieldLong() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int start = 10000;
		final int end = 12000;
		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i - start;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), end - start);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getRegExp(), "\\d{5}");
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertTrue(result.isKey());
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void setMaxOutliers() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Alphabet");
		final int start = 10000;
		final int end = 12000;
		final int newMaxOutliers = 12;

		int locked = -1;

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

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(analysis.getMaxOutliers(), newMaxOutliers);
		Assert.assertEquals(result.getOutlierCount(), newMaxOutliers);
		Assert.assertEquals(result.getSampleCount(), newMaxOutliers + 3 + end - start);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getRegExp(), "\\d{5}");
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertTrue(result.isKey());
		Assert.assertEquals(result.getConfidence(), 1 - (double)15/result.getSampleCount());
		Assert.assertEquals(result.dump(true), "TextAnalysisResult [name=Alphabet, matchCount=2000, sampleCount=2015, nullCount=0, blankCount=0, regexp=\"\\d{5}\", confidence=0.9925558312655087, type=Long, min=\"10000\", max=\"11999\", minLength=1, maxLength=5, sum=\"21999000\", cardinality=MAX, outliers=12 {\"A\":1 \"B\":1 \"C\":1 \"D\":1 \"E\":1 \"F\":1 \"G\":1 \"H\":1 \"I\":1 \"J\":1 ...}, PossibleKey]");
	}

	@Test
	public void keyFieldString() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int start = 100000;
		final int end = 120000;

		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train("A" + String.valueOf(i)) && locked == -1)
				locked = i - start;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), end - start);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getRegExp(), KnownPatterns.PATTERN_ALPHANUMERIC + "{7}");
		Assert.assertEquals(result.getType(), PatternInfo.Type.STRING);
		Assert.assertTrue(result.isKey());
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void notKeyField() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int start = 10000;
		final int end = 12000;

		int locked = -1;

		for (int i = start; i < end; i++) {
			if (analysis.train(String.valueOf(i)) && locked == -1)
				locked = i - start;
		}

		analysis.train(String.valueOf(start));

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, TextAnalyzer.SAMPLE_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), 1 + end - start);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getRegExp(), "\\d{5}");
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertFalse(result.isKey());
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	@Test
	public void dateTimeYYYYMMDDTHHMMSS() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

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

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), 20);
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATETIME);
		Assert.assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getTypeQualifier(), "yyyy-MM-dd'T'HH:mm:ss");
	}

	@Test
	public void dateTimeYYYYMMDDTHHMMSSNNNN() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

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

		Assert.assertEquals(result.getType(), PatternInfo.Type.OFFSETDATETIME);
		Assert.assertEquals(result.getSampleCount(), 20);
		Assert.assertEquals(result.getMatchCount(), 18);
		Assert.assertEquals(result.getNullCount(), 2);
		Assert.assertEquals(result.getRegExp(), "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[-+][0-9]{2}:[0-9]{2}");
		Assert.assertEquals(result.getTypeQualifier(), "yyyy-MM-dd'T'HH:mm:ssxxx");
		Assert.assertEquals(result.getConfidence(), 1.0);

		analysis.train("2008-01-01T00:00:00-05:00");
		result = analysis.getResult();
		Assert.assertEquals(result.getSampleCount(), 21);
	}

	@Test
	public void dateTimeYYYYMMDDTHHMMSSZ() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();

		analysis.train("01/26/2012 10:42:23 GMT");
		analysis.train("01/26/2012 10:42:23 GMT");
		analysis.train("01/30/2012 10:59:48 GMT");
		analysis.train("01/25/2012 16:46:43 GMT");
		analysis.train("01/25/2012 16:28:42 GMT");
		analysis.train("01/24/2012 16:53:04 GMT");

		TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.ZONEDDATETIME);
		Assert.assertEquals(result.getSampleCount(), 6);
		Assert.assertEquals(result.getMatchCount(), 6);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2} .*");
		Assert.assertEquals(result.getTypeQualifier(), "MM/dd/yyyy HH:mm:ss z");
		Assert.assertEquals(result.getConfidence(), 1.0);

		analysis.train("01/25/2012 16:28:42 GMT");
		result = analysis.getResult();
		Assert.assertEquals(result.getSampleCount(), 7);
	}

	@Test
	public void testFromFuzz1() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input =
				"100|-101.0|102|103|104|105|106|107|hello|109|110|111|112|113|114|115|116|117|118|119|";
		final String inputs[] = input.split("\\|");
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getBlankCount(), 0);
		Assert.assertEquals(result.getRegExp(), "-?\\d+|-?(\\d+)?\\.\\d+");
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertEquals(result.getMinValue(), "-101.0");
		Assert.assertEquals(result.getMaxValue(), "119.0");
	}

	public String[] decoder = new String[] {
			"Integer", "Boolean", "Long", "Double", "Date",
			"ISO-3166-3", "ISO-3166-2", "ZIP", "US_STATE", "CA_PROVINCE",
			"US_STREET"
	};

	public String[] generateTestData(int type, int length) {
		final Random random = new Random(314159265);
		String[] result = new String[length];
		String[] candidatesISO3166_3 = valid3166_3.split("\\|");
		String[] candidatesISO3166_2 = valid3166_2.split("\\|");
		String[] candidatesZips = validZips.split("\\|");
		String[] candidatesUSStates = validUSStates.split("\\|");
		String[] candidatesCAProvinces = validCAProvinces.split("\\|");

		for (int i = 0; i < length; i++)
		switch (type) {
		case 0:
			// Integer
			result[i] = String.valueOf(random.nextInt());
			break;
		case 1:
			// Boolean
			result[i] = String.valueOf(random.nextBoolean());
			break;
		case 2:
			// Long
			result[i] = String.valueOf(random.nextLong());
			break;
		case 3:
			// Double
			result[i] = String.valueOf(random.nextDouble());
			break;
		case 4:
			// Date
			Date d = new Date(random.nextLong());
			result[i] = d.toString();
			break;
		case 5:
			// ISO 3166-3
			result[i] = candidatesISO3166_3[random.nextInt(candidatesISO3166_3.length)];
			break;
		case 6:
			// ISO 3166-2
			result[i] = candidatesISO3166_2[random.nextInt(candidatesISO3166_2.length)];
			break;
		case 7:
			// Zip Cpde
			result[i] = candidatesZips[random.nextInt(candidatesZips.length)];
			break;
		case 8:
			// US State
			result[i] = candidatesUSStates[random.nextInt(candidatesUSStates.length)];
			break;
		case 9:
			// CA Provinces
			result[i] = candidatesCAProvinces[random.nextInt(candidatesCAProvinces.length)];
			break;
		case 10:
			// US Street
			result[i] = validUSStreets[random.nextInt(validUSStreets.length)];
			break;
		}

		return result;
	}

	class AnalysisThread implements Runnable {
		private String id;
		private int streamType;
		private String[] stream;
		private TextAnalysisResult answer;
		private TextAnalyzer analysis;

		AnalysisThread(String id, int streamType, String[] stream, TextAnalysisResult answer) throws IOException {
			this.id = id;
			this.streamType = streamType;
			this.stream = stream;
			this.answer = answer;
			analysis = new TextAnalyzer();
//			System.out.printf("Thread %s: created, Stream: type: %s, length: %d\n",
//					this.id, decoder[this.streamType], this.stream.length);
		}

		@Override
		public void run() {
//			long start = System.currentTimeMillis();
			for (int i = 0; i < stream.length; i++)
				analysis.train(stream[i]);

			final TextAnalysisResult result = analysis.getResult();

			Assert.assertEquals(result.getSampleCount(), answer.getSampleCount());
			Assert.assertEquals(result.getNullCount(), answer.getNullCount());
			Assert.assertEquals(result.getBlankCount(), answer.getBlankCount());
			Assert.assertEquals(result.getRegExp(), answer.getRegExp());
			Assert.assertEquals(result.getConfidence(), answer.getConfidence());
			Assert.assertEquals(result.getType(), answer.getType());
			Assert.assertEquals(result.getMinValue(), answer.getMinValue());
			Assert.assertEquals(result.getMaxValue(), answer.getMaxValue());

//			System.out.printf("Thread %s: exiting, duration %d\n", id, System.currentTimeMillis() - start);
		}
	}

	@Test
	public void testThreading() throws IOException, InterruptedException {
		final Random random = new Random(271828);
		final int THREADS = 1000;
		Thread[] threads = new Thread[THREADS];

		for (int t = 0; t < THREADS; t++) {
			int type = random.nextInt(decoder.length);
			int length = 30 + random.nextInt(10000);
			String[] stream = generateTestData(type, length);

			TextAnalyzer analysis = new TextAnalyzer();
			for (int i = 0; i < stream.length; i++)
				analysis.train(stream[i]);

			threads[t] = new Thread(new AnalysisThread(String.valueOf(t), type, stream, analysis.getResult()));
		}

		for (int t = 0; t < THREADS; t++)
			threads[t].start();

		for (int t = 0; t < THREADS; t++)
			if (threads[t].isAlive())
				threads[t].join();
	}

	//@Test
	public void fuzzInt() throws IOException {
		final Random random = new Random(3141562);
		final int SAMPLES = 1000;
		final int errorRate = 1;

		for (int iter = 0; iter < 100; iter++) {
			final TextAnalyzer analysis = new TextAnalyzer();
			analysis.setThreshold(100 - errorRate);
			int length = random.nextInt(9);
			long low = (long)Math.pow(10, length);
			long lowest = low;
			long lowestFloat = low;
			int lowLength = String.valueOf(low).length();
			long high = low + SAMPLES - 1;
			int highLength = String.valueOf(high).length();
			int misses = 0;
			boolean sticky = random.nextBoolean();
			boolean isNegative = false;
			int floats = 0;
			int strings = 0;
			int nulls = 0;
			int blanks = 0;
			int errorCase = -1;
			long firstFloat = -1;
			String[] errorCaseDecode = new String[] { "String", "NegativeInt", "Double", "negativeDouble", "null", "blank" };

			for (long i = low; i <= high; i++) {
				if (i != low && i != high && random.nextInt(99) < 2) {
					if (errorCase == -1 || !sticky)
						errorCase = random.nextInt(6);
					switch (errorCase) {
					case 0:
						// String
						analysis.train("hello");
						strings++;
						misses++;
						break;
					case 1:
						// NegativeInt
						analysis.train(String.valueOf(-i));
						isNegative = true;
						lowest = -i;
						break;
					case 2:
						// Double
						analysis.train(String.valueOf((double)i));
						floats++;
						if (firstFloat == -1)
							firstFloat = i - low - nulls - blanks;
						misses++;
						break;
					case 3:
						// Negative Double
						analysis.train(String.valueOf((double)-i));
						isNegative = true;
						floats++;
						lowestFloat = -i;
						if (firstFloat == -1)
							firstFloat = i - low - nulls - blanks;
						misses++;
						break;
					case 4:
						// Null
						analysis.train(null);
						nulls++;
						misses++;
						break;
					case 5:
						// Blank
						analysis.train("");
						blanks++;
						misses++;
						break;
					}
				}
				else
					analysis.train(String.valueOf(i));
			}

			PatternInfo.Type answer;
			String re = "";
			String min;
			String max;
			if (firstFloat != -1 && firstFloat < analysis.getSampleSize() || floats >= (errorRate * SAMPLES)/100) {
				misses -= floats;
				answer = PatternInfo.Type.DOUBLE;
				min = String.valueOf((double)Math.min(lowest, lowestFloat));
				max = String.valueOf((double)high);
				re += min.charAt(0) == '-' ? "-?\\d+|-?(\\d+)?\\.\\d+" : "\\d+|(\\d+)?\\.\\d+";
			}
			else {
				if (isNegative)
					re += "-?";
				re += "\\d{" + lowLength;
				if (lowLength != highLength)
					re += "," + highLength;
				re += "}";
				answer = PatternInfo.Type.LONG;
				min = String.valueOf(lowest);
				max = String.valueOf(high);
			}

			System.err.printf("Iter: %d, length: %d, start: %d, sticky: %b, re: %s, floats: %d, firstFloat: %d, strings: %d, errorCase: %s\n",
					iter, length, low, sticky, re, floats, firstFloat, strings, sticky ? errorCaseDecode[errorCase] : "Variable");

			final TextAnalysisResult result = analysis.getResult();

			Assert.assertEquals(result.getSampleCount(), SAMPLES);
			Assert.assertEquals(result.getNullCount(), nulls);
			Assert.assertEquals(result.getBlankCount(), blanks);
			Assert.assertEquals(result.getRegExp(), re);
			Assert.assertEquals(result.getConfidence(), (double)(SAMPLES-misses)/(SAMPLES - blanks - nulls));
			Assert.assertEquals(result.getType(), answer);
			Assert.assertEquals(result.getMinValue(), min);
			Assert.assertEquals(result.getMaxValue(), max);
		}
	}

	@Test
	public void dateSwitcher() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Contract Signed Date");
		final String inputs[] = new String[] {
				"3/14/2012", "3/14/2012", "6/30/2011", "3/14/2012", "3/14/2012", "3/14/2012",
				"3/14/2012", "3/14/2012", "3/14/2012", "3/14/2012", "3/14/2012", "3/14/2012",
				"3/14/2012", "6/30/2011", "6/30/2011", "6/30/2011", "6/30/2011", "6/30/2011",
				"6/30/2011", "9/11/2013", "10/2/2013", "10/2/2013", "10/2/2013", "10/2/2013",
				"10/2/2013", "10/2/2013", "2/18/2014", "2/18/2014", "2/18/2014", "6/2/2014",
				"6/2/2014", "6/2/2014", "6/2/2014", "6/2/2014", "10/17/2012"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{1,2}/\\d{1,2}/\\d{4}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "M/d/yyyy");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}


	}

	@Test
	public void novelApproachDateTime() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("SUB_ACTIVE_DATE");
		final String inputs[] = new String[] {
				"04OCT2012:16:27:03", "04OCT2012:16:27:03", "04OCT2012:16:27:03", "04OCT2012:16:27:03", "04OCT2012:16:27:03", "04OCT2012:16:27:03", "03DEC2012:16:23:30", "03DEC2012:16:23:30", "03DEC2012:16:33:46", "03DEC2012:16:57:14",
				"05DEC2012:11:31:06", "03DEC2012:16:23:30", "03DEC2012:16:23:30", "03DEC2012:16:33:46", "03DEC2012:16:57:14", "05DEC2012:11:31:06", "03DEC2012:16:23:30", "03DEC2012:16:23:30", "03DEC2012:16:23:30", "03DEC2012:16:23:30",
				"03DEC2012:16:23:30", "03DEC2012:16:23:30", "03DEC2012:16:33:46", "03DEC2012:16:33:46", "03DEC2012:16:33:46", "03DEC2012:16:57:14", "03DEC2012:16:57:14", "03DEC2012:16:57:14", "05DEC2012:11:31:06", "05DEC2012:11:31:06",
				"05DEC2012:11:31:06", "15DEC2012:21:02:35", "15DEC2012:21:02:35", "15DEC2012:21:02:35", "15DEC2012:21:02:35", "20FEB2013:22:25:28", "20FEB2013:22:25:28", "20FEB2013:22:25:28", "21FEB2013:16:55:29", "21FEB2013:16:55:29",
				"21FEB2013:16:55:29", "22FEB2013:14:10:14", "22FEB2013:14:10:14", "22FEB2013:14:10:14", "22FEB2013:14:10:14", "25FEB2013:10:16:21", "25FEB2013:10:16:21", "25FEB2013:10:16:21", "25FEB2013:10:16:21", "25FEB2013:10:30:27",
				"25FEB2013:10:30:27", "25FEB2013:10:30:27", "25FEB2013:10:30:27", "25FEB2013:13:13:29", "25FEB2013:13:13:29", "25FEB2013:13:13:29", "26FEB2013:08:10:28", "26FEB2013:08:10:28", "26FEB2013:08:10:28",
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATETIME);
		Assert.assertEquals(result.getTypeQualifier(), "ddMMMyyyy:HH:mm:ss");
		Assert.assertEquals(result.getRegExp(), "\\d{2}\\p{IsAlphabetic}{3}\\d{4}:\\d{2}:\\d{2}:\\d{2}");
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void novelApproachDate() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("SUB_ACTIVE_DATE_ONLY");
		final String inputs[] = new String[] {
				"04OCT2012", "04OCT2012", "04OCT2012", "04OCT2012", "04OCT2012", "04OCT2012", "03DEC2012", "03DEC2012", "03DEC2012", "03DEC2012",
				"05DEC2012", "03DEC2012", "03DEC2012", "03DEC2012", "03DEC2012", "05DEC2012", "03DEC2012", "03DEC2012", "03DEC2012", "03DEC2012",
				"03DEC2012", "03DEC2012", "03DEC2012", "03DEC2012", "03DEC2012", "03DEC2012", "03DEC2012", "03DEC2012", "05DEC2012", "05DEC2012",
				"05DEC2012", "15DEC2012", "15DEC2012", "15DEC2012", "15DEC2012", "20FEB2013", "20FEB2013", "20FEB2013", "21FEB2013", "21FEB2013",
				"21FEB2013", "22FEB2013", "22FEB2013", "22FEB2013", "22FEB2013", "25FEB2013", "25FEB2013", "25FEB2013", "25FEB2013", "25FEB2013",
				"25FEB2013", "25FEB2013", "25FEB2013", "25FEB2013", "25FEB2013", "25FEB2013", "26FEB2013", "26FEB2013", "26FEB2013",
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "ddMMMyyyy");
		Assert.assertEquals(result.getRegExp(), "\\d{2}\\p{IsAlphabetic}{3}\\d{4}");
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void timMBug1() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("TimMeagherBug1");

		analysis.train("01/12/2018 12:34:44+0000");
		analysis.train("12/01/2017 11:23:21-0100");
		analysis.train("06/05/1998 18:19:21+0100");
		analysis.train("  ");
		analysis.train(null);
		analysis.train("31/12/2015 08:05:55-0500");
		analysis.train("15/06/2019 23:15:31-0500");
		analysis.train("00/00/0000 00:00:00+0000");

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.OFFSETDATETIME);
		Assert.assertEquals(result.getTypeQualifier(), "dd/MM/yyyy HH:mm:ssxx");
		Assert.assertEquals(result.getRegExp(), "\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}[-+][0-9]{4}");
		Assert.assertEquals(result.getBlankCount(), 1);
		Assert.assertEquals(result.getNullCount(), 1);
		Assert.assertEquals(result.getSampleCount(), 8);
		Assert.assertEquals(result.getMatchCount(), 5);
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/(result.getSampleCount() - 2));
	}

	@Test
	public void longAsDateWith0() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("stringField:string");

		final String inputs[] = new String[] {
				"20180112", "20171201", "19980605", "19990605", "20000605", "20010605", "20020605", "20030605", "20040605", "20050605",
				"20060605", "20070605", "20080605", "20090605", "20100605", "20110605", "20120605", "20130605", "20140605", "20150605",
				"20160605", "20170605", "20180605", "20190605", "20200605", "20210605", "20220605", "20230605", "20240605", "20250605",
				"20260605", "20270605", "", "19990101", "0", "20151231", "20190615",
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
		Assert.assertEquals(result.getTypeQualifier(), "yyyyMMdd");
		Assert.assertEquals(result.getRegExp(), "\\d{8}");
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getBlankCount(), 1);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getMatchCount(), inputs.length - 1);
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < inputs.length; i++) {
			if (!inputs[i].isEmpty() && !"0".equals(inputs[i]))
				Assert.assertTrue(inputs[i].matches(result.getRegExp()), inputs[i]);
		}
	}

	@Test
	public void intuitDateddMMyyyyHHmmss() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("Settlement_Errors", DateResolutionMode.MonthFirst);

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

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATETIME);
		Assert.assertEquals(result.getTypeQualifier(), "d/M/yyyy HH:mm:ss");
		Assert.assertEquals(result.getSampleCount(), 39);
		Assert.assertEquals(result.getMatchCount(), 39);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), "\\d{1,2}/\\d{1,2}/\\d{4} \\d{2}:\\d{2}:\\d{2}");
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

}
