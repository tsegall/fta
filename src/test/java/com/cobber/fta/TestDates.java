package com.cobber.fta;

import java.io.IOException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.cobber.fta.DateTimeParser.DateResolutionMode;

public class TestDates {
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
		DateResolutionMode[] cases = new DateResolutionMode[] { DateResolutionMode.DayFirst, DateResolutionMode.Auto };

		for (DateResolutionMode resolutionMode : cases) {
			final TextAnalyzer analysis = new TextAnalyzer("h:mm a dd/MM/yy", resolutionMode);
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
		DateResolutionMode[] cases = new DateResolutionMode[] { DateResolutionMode.DayFirst, DateResolutionMode.Auto };

		Locale[] locales = new Locale[] { Locale.forLanguageTag("en-US"), Locale.forLanguageTag("en-GB") };
		for (Locale locale : locales) {
			for (DateResolutionMode resolutionMode : cases) {
				final TextAnalyzer analysis = new TextAnalyzer("TransactionDate", resolutionMode);
				analysis.setLocale(locale);
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

				Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATETIME);
				Assert.assertEquals(result.getTypeQualifier(), "HH:mm dd/M/yy");
				Assert.assertEquals(result.getSampleCount(), inputs.length);
				Assert.assertEquals(result.getMatchCount(), inputs.length);
				Assert.assertEquals(result.getNullCount(), 0);
				Assert.assertEquals(result.getRegExp(), "\\d{2}:\\d{2} \\d{2}/\\d{1,2}/\\d{2}");
				Assert.assertEquals(result.getConfidence(), 1.0);

				for (int i = 0; i < inputs.length; i++) {
					Assert.assertTrue(inputs[i].matches(result.getRegExp()));
				}
			}
		}
	}

	@Test
	public void basicResolutionMode() throws IOException {
		DateResolutionMode[] cases = new DateResolutionMode[] { DateResolutionMode.DayFirst, DateResolutionMode.MonthFirst, DateResolutionMode.Auto };

		Locale[] locales = new Locale[] { Locale.forLanguageTag("en-US"), Locale.forLanguageTag("en-GB") };
		for (Locale locale : locales) {
			for (DateResolutionMode resolutionMode : cases) {
				final TextAnalyzer analysis = new TextAnalyzer("TransactionDate", resolutionMode);
				analysis.setLocale(locale);
				final String input = "2/2/34|3/3/19|4/4/48|5/5/55|6/6/66|7/7/77|8/8/88|9/9/99|" +
							"12/12/34|4/5/19|6/4/48|7/5/55|12/6/66|12/7/77|3/8/88|2/9/99|" +
							"1/1/26|4/5/33|6/9/48|9/5/55|12/2/66|11/11/78|3/4/98|2/3/39|";
				final String inputs[] = input.split("\\|");
				int locked = -1;

				for (int i = 0; i < inputs.length; i++) {
					if (analysis.train(inputs[i]) && locked == -1)
						locked = i;
				}

				final TextAnalysisResult result = analysis.getResult();

				Assert.assertEquals(result.getType(), PatternInfo.Type.LOCALDATE);
				String expected = "";
				switch (resolutionMode) {
				case DayFirst:
					expected = "d/M/yy";
					break;
				case MonthFirst:
					expected = "M/d/yy";
					break;
				case Auto:
					expected = locale.toLanguageTag().equals("en-GB") ? "d/M/yy" : "M/d/yy";
					break;
				}
				Assert.assertEquals(result.getTypeQualifier(), expected);
				Assert.assertEquals(result.getSampleCount(), inputs.length);
				Assert.assertEquals(result.getMatchCount(), inputs.length);
				Assert.assertEquals(result.getNullCount(), 0);
				Assert.assertEquals(result.getRegExp(), "\\d{1,2}/\\d{1,2}/\\d{2}");
				Assert.assertEquals(result.getConfidence(), 1.0);

				for (int i = 0; i < inputs.length; i++) {
					Assert.assertTrue(inputs[i].matches(result.getRegExp()));
				}
			}
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

	@Test
	public void basicBulgarianDate() throws IOException {

		Set<String> samples = new HashSet<String>();
		LocalDate localDate = LocalDate.now();

		if (!TestUtils.isValidLocale("bg_BG"))
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

		if (!TestUtils.isValidLocale("ca_ES"))
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

		if (!TestUtils.isValidLocale("de_AT"))
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
}
