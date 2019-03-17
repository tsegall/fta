package com.cobber.fta;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestDoubles {

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
	public void tinyDouble() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final String input = "0e-17";
		int locked = -1;

		analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, -1);
		Assert.assertEquals(result.getSampleCount(), 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_DOUBLE_WITH_EXPONENT));
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getMinValue(), "0.0");
		Assert.assertEquals(result.getMaxValue(), "0.0");
		Assert.assertEquals(result.getMinLength(), 5);
		Assert.assertEquals(result.getMaxLength(), 5);
		Assert.assertTrue(input.matches(result.getRegExp()));
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

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
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

		analysis.setDetectWindow(2* TextAnalyzer.DETECT_WINDOW_DEFAULT);
		for (int i = 0; i <= TextAnalyzer.DETECT_WINDOW_DEFAULT; i++) {
			if (analysis.train(samples[sample++]) && locked == -1)
				locked = sample;
		}
		for (int i = 0; i < TextAnalyzer.DETECT_WINDOW_DEFAULT; i++) {
			if (analysis.train(samples[sample++]) && locked == -1)
				locked = sample;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, 2 * TextAnalyzer.DETECT_WINDOW_DEFAULT + 1);
		Assert.assertEquals(result.getSampleCount(), 2 * TextAnalyzer.DETECT_WINDOW_DEFAULT + 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_DOUBLE_WITH_EXPONENT));
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (int i = 0; i < samples.length; i++) {
			Assert.assertTrue(samples[i].matches(result.getRegExp()), samples[i]);
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

		analysis.setDetectWindow(2* TextAnalyzer.DETECT_WINDOW_DEFAULT);
		for (int i = 0; i <= TextAnalyzer.DETECT_WINDOW_DEFAULT; i++) {
			if (analysis.train(samples[sample++]) && locked == -1)
				locked = sample;
		}
		for (int i = 0; i < TextAnalyzer.DETECT_WINDOW_DEFAULT; i++) {
			if (analysis.train(samples[sample++]) && locked == -1)
				locked = sample;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(locked, 2 * TextAnalyzer.DETECT_WINDOW_DEFAULT + 1);
		Assert.assertEquals(result.getSampleCount(), 2 * TextAnalyzer.DETECT_WINDOW_DEFAULT + 1);
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
	public void manyRandomDoubles() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		analysis.setCollectStatistics(false);
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

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
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
	public void manyConstantLengthDoublesI18N_1() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		analysis.setCollectStatistics(false);
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

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), iterations + nullIterations);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), nullIterations);
		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_DOUBLE));
		Assert.assertEquals(result.getConfidence(), 1.0);
	}

	// BUG - In general, even if the locale suggests otherwise we should still cope with 1234.56 as a valid double
	//@Test
	public void manyConstantLengthDoublesI18N_2() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		final int nullIterations = 50;
		final int iterations = 10000;
		final Random random = new Random();
		int locked = -1;
		Locale locale = Locale.forLanguageTag("de-DE");
		analysis.setCollectStatistics(false);
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

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getSampleCount(), iterations + nullIterations);
		Assert.assertEquals(result.getCardinality(), TextAnalyzer.MAX_CARDINALITY_DEFAULT);
		Assert.assertEquals(result.getNullCount(), nullIterations);
		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertEquals(result.getRegExp(), "\\d+|(\\d+)?" + RegExpGenerator.slosh('.') + "\\d+");
		Assert.assertEquals(result.getConfidence(), 1.0);
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
		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_SIGNED_DOUBLE));
		Assert.assertEquals(result.getConfidence(), 1 - (double)1/result.getSampleCount());
		Assert.assertEquals(result.getMinValue(), "-101.0");
		Assert.assertEquals(result.getMaxValue(), "119.0");
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

		Assert.assertEquals(locked, TextAnalyzer.DETECT_WINDOW_DEFAULT);
		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount() + result.getBlankCount(), inputs.length);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 0);
		Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_DOUBLE));
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
	public void localeNegativeDoubleTest() throws IOException {
		final Random random = new Random(1);
		final int SAMPLE_SIZE = 1000;
		Locale[] locales = DateFormat.getAvailableLocales();
//		Locale[] locales = new Locale[] { Locale.forLanguageTag("en-US"), Locale.forLanguageTag("hr-HR") };
//		Locale[] locales = new Locale[] { Locale.forLanguageTag("ar-AE") };

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

			NumberFormat nf = NumberFormat.getIntegerInstance(locale);

			Set<String> samples = new HashSet<String>();
			nf.setMinimumFractionDigits(2);
			for (int i = 0; i < SAMPLE_SIZE; i++) {
				double d = random.nextDouble() * random.nextInt();
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

			Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_SIGNED_DOUBLE_GROUPING));
			Assert.assertEquals(result.getConfidence(), 1.0);

			for (String sample : samples) {
				Assert.assertTrue(sample.matches(result.getRegExp()), sample + " " + result.getRegExp());
			}
		}
	}

	@Test
	public void simpleDoubleExponentTest() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("simpleDoubleExponentTest");
		analysis.setCollectStatistics(false);
		DecimalFormat df = new DecimalFormat("0.00E00");
		final Random random = new Random(1);
		final int SAMPLE_SIZE = 1000;
		String sample;

		Set<String> samples = new HashSet<String>();
		NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setMinimumFractionDigits(1);
		for (int i = 0; i < 10; i++) {
			double d = random.nextDouble();
			sample = nf.format(d);
			samples.add(sample);
			analysis.train(sample);
		}

		for (int i = 0; i < SAMPLE_SIZE; i++) {
			double d = random.nextDouble() * random.nextInt();
			sample = df.format(d).toString();
			samples.add(sample);
			analysis.train(sample);
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertEquals(result.getTypeQualifier(), "SIGNED");
		Assert.assertEquals(result.getSampleCount(), SAMPLE_SIZE + 10);
		Assert.assertEquals(result.getMatchCount(), SAMPLE_SIZE + 10);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getLeadingZeroCount(), 0);

		Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_SIGNED_DOUBLE_WITH_EXPONENT));
		Assert.assertEquals(result.getConfidence(), 1.0);

		String actualRegExp = result.getRegExp();

		for (String s : samples) {
			Assert.assertTrue(s.matches(actualRegExp), s + " " + actualRegExp);
		}
	}

	@Test
	public void localeNegativeDoubleExponentTest() throws IOException {
		final Random random = new Random(1);
		final int SAMPLE_SIZE = 1000;
		Locale[] locales = DateFormat.getAvailableLocales();
//		Locale[] locales = new Locale[] { Locale.forLanguageTag("mk-MK") };

		for (Locale locale : locales) {
			final TextAnalyzer analysis = new TextAnalyzer("localeNegativeDoubleExponentTest");
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

			if ("mk-MK".contentEquals(locale.toLanguageTag())) {
				System.err.printf("Skipping locale '%s' as it has trailing neg suffix.\n", locale);
				continue;
			}

			DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);

			DecimalFormat df = new DecimalFormat("#E0", formatSymbols);

//			String grp = Utils.slosh(formatSymbols.getGroupingSeparator());
//			String dec = Utils.slosh(formatSymbols.getDecimalSeparator());
//			System.err.printf("Locale '%s', grouping: %s, decimal: %s, negPrefix: %s, negSuffix: %s.\n",
//					locale, grp, dec, negPrefix, negSuffix);

			Set<String> samples = new HashSet<String>();
			for (int i = 0; i < SAMPLE_SIZE; i++) {
				double d = random.nextDouble() * random.nextInt();
				int pow = random.nextInt(10);
				if (pow % 2 == 0)
					d *= Math.pow(10, pow);
				else
					d *= Math.pow(d,  -pow);

				String sample = df.format(d).toString();
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

			Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_SIGNED_DOUBLE_WITH_EXPONENT));
			Assert.assertEquals(result.getConfidence(), 1.0);

			String actualRegExp = result.getRegExp();

			for (String sample : samples) {
				Assert.assertTrue(sample.matches(actualRegExp), "Locale: " + locale + " " + sample + " " + actualRegExp);
			}
		}
	}

	@Test
	public void spacedDoubles() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("AMT");
		analysis.setCollectStatistics(false);
		final String inputs[] = new String[] {
				" 000000512.80", "-000000512.80", "-000000006.96", "-000000206.43", "-000000078.40", " 000000000.03", "-000000000.03", "-000000010.60", " 000000244.87", " 000000917.60",
				" 000000150.00", " 000000024.00", " 000000035.00", " 000000150.00", " 000000035.00", " 000000010.00", " 000000035.00", " 000000035.00", " 000000035.00", " 000000002.80",
				" 000000024.00", " 000000008.40", " 000000005.60", " 000000005.60", " 000000005.60", " 000000035.00", " 000000005.60", " 000000005.60", " 000000919.52", "-000000919.52",
				" 000001839.04", "-000001839.04", " 000000033.12", " 000000002.65", " 000000035.77", "-000000035.77", " 000000959.10", " 000001150.92", " 000000294.45", " 000000353.34",
				" 000000001.73", " 000000001.73", "-000001807.82", " 000000170.83", "-000000000.02", "-000000012.21", "-000000011.22", "-000000015.20", " 000006940.81", " 000001520.00",
				" 000023026.50", " 000005090.71", " 000000047.12", "-000000018.40", "-000000015.97", "-000000012.75", "-000000244.87", "-000000917.60", " 000000078.40", " 000001087.06",
				"-000001087.06", "-000000009.62", "-000000006.98", "-000000004.95", "-000000002.42", " 000001669.40", " 000000306.68", " 000000086.00", " 000000340.32", "-000000139.78",
				"-000000138.70", "-000000132.07", "-000000128.53", "-000000147.21", " 000000686.29", " 000000307.40", "-000000086.84", "-000000167.41", "-000000053.15", " 000004700.34",
				" 000000227.82", "-000001302.62", "-000000516.30", "-000000516.30", "-000000520.23", "-000000520.23", "-000000520.23", "-000000516.30", "-000000059.06", "-000000055.26",
				"-000000059.06", "-000000059.06", "-000000059.06", "-000000055.13", "-000000055.13", "-000000055.13", "-000000059.06", " 000000074.50", "-000000060.24", " 000001720.00"
				,
				" 000006018.37", " 000000026.00", " 000000026.00", "-000000013.87", "-000000414.82", "-000000150.61", "-000000009.77"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertEquals(result.getTypeQualifier(), "SIGNED");
		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*" + analysis.getRegExp(KnownPatterns.ID.ID_SIGNED_DOUBLE));
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void decimalSeparatorTest_Locale() throws IOException {
		final Random random = new Random(1);
		final int SAMPLE_SIZE = 1000;
		Locale[] locales = new Locale[] { Locale.forLanguageTag("de-DE"), Locale.forLanguageTag("en-US") };

		for (Locale locale : locales) {
			final TextAnalyzer analysis = new TextAnalyzer("DecimalSeparator");
			analysis.setCollectStatistics(false);
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

			Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_SIGNED_DOUBLE));
			Assert.assertEquals(result.getConfidence(), 1.0);

			for (String sample : samples) {
				Assert.assertTrue(sample.matches(result.getRegExp()), sample + " " + result.getRegExp());
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
		Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_DOUBLE));
		Assert.assertEquals(result.getConfidence(), 1.0);

		for (String sample : samples) {
			Assert.assertTrue(sample.matches(result.getRegExp()), sample);
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
		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*" + analysis.getRegExp(KnownPatterns.ID.ID_SIGNED_DOUBLE));
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getMinValue(), "-9999.0");
		Assert.assertEquals(result.getMaxValue(), "0.69334954");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void localeDoubleTest() throws IOException {
		final Random random = new Random(1);
		final int SAMPLE_SIZE = 1000;
		Locale[] locales = DateFormat.getAvailableLocales();
//		Locale[] locales = new Locale[] { Locale.forLanguageTag("en"), Locale.forLanguageTag("es-CO") };
//		Locale[] locales = new Locale[] { Locale.forLanguageTag("es-CO") };

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

			Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE, locale.toLanguageTag());
			Assert.assertEquals(result.getTypeQualifier(), "GROUPING");
			Assert.assertEquals(result.getSampleCount(), SAMPLE_SIZE);
			Assert.assertEquals(result.getMatchCount(), SAMPLE_SIZE);
			Assert.assertEquals(result.getNullCount(), 0);
			Assert.assertEquals(result.getLeadingZeroCount(), 0);
			DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);

			String grp = formatSymbols.getGroupingSeparator() == '.' ? "\\." : "" + formatSymbols.getGroupingSeparator();
			String dec = formatSymbols.getDecimalSeparator() == '.' ? "\\." : "" + formatSymbols.getDecimalSeparator();

			String regExp = "[\\d" + grp + "]*" + dec + "?" + "[\\d" + grp +"]+";

//			System.err.println("Locale: " + locale + ", grp = '" + grp + "', dec = '" + dec + "', re: " + regExp + "'");

			Assert.assertEquals(result.getRegExp(), regExp);
			Assert.assertEquals(result.getConfidence(), 1.0);

			for (String sample : samples) {
				Assert.assertTrue(sample.matches(regExp), sample + " " + regExp);
			}
		}
	}

//	@Test
	public void decimalSeparatorTest_Period() throws IOException {
		final Random random = new Random(1);
		Set<String> failures = new HashSet<>();
		final int SAMPLE_SIZE = 1000;
		Locale[] locales = DateFormat.getAvailableLocales();
//		Locale[] locales = new Locale[] { Locale.forLanguageTag("de-DE") };

		for (Locale locale : locales) {
			long min = Long.MAX_VALUE;
			long absMin = Long.MAX_VALUE;
			long max = Long.MIN_VALUE;
			final TextAnalyzer analysis = new TextAnalyzer("DecimalSeparator");
			analysis.setLocale(locale);

			DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);

			String grp = RegExpGenerator.slosh(formatSymbols.getGroupingSeparator());

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

			System.err.printf("Locale '%s', grouping: %s.\n", locale, grp);

			Set<String> samples = new HashSet<String>();
			for (int i = 0; i < SAMPLE_SIZE; i++) {
				long l = random.nextInt(10000000);
				if (l % 2 == 0)
					l = -l;
				String sample = String.valueOf(l) + "." + random.nextInt(10);

				if (l < min) {
					min = l;
				}
				if (Math.abs(l) < absMin)
					absMin = Math.abs(l);
				if (l < min)
					min = l;
				if (l > max)
					max = l;

				samples.add(sample);
				analysis.train(sample);
			}

			final TextAnalysisResult result = analysis.getResult();

			if (!result.getType().equals(PatternInfo.Type.DOUBLE)) {
				failures.add("Locale: " + locale + ", type: '" + result.getType() + '"');
				continue;
			}
			Assert.assertEquals(result.getTypeQualifier(), "SIGNED");
			Assert.assertEquals(result.getSampleCount(), SAMPLE_SIZE);
			Assert.assertEquals(result.getMatchCount(), SAMPLE_SIZE);
			Assert.assertEquals(result.getNullCount(), 0);
			Assert.assertEquals(result.getLeadingZeroCount(), 0);
			Assert.assertEquals(result.getDecimalSeparator(), '.');

//			String regExp = "-?";
//			regExp += "[\\d" + Utils.slosh(formatSymbols.getGroupingSeparator()) + "]";
//			regExp += Utils.regExpLength(minValue.length(), maxValue.length());

//			System.err.println("Locale: " + locale + ", grp = '" + grp + "', dec = '" + dec + "', re: " + regExp + "'");

			String regExp = result.getRegExp();
			Assert.assertEquals(result.getConfidence(), 1.0);
			regExp = "-?(\\d+)?\\.\\d+";

			for (String sample : samples) {
				if (!sample.matches(regExp)) {
					failures.add("Locale: " + locale + ", grouping: '" + grp + '"');
					break;
				}
			}
		}

		for (String failure : failures)
			System.err.println(failure);

		Assert.assertEquals(failures.size(), 0);
	}

	public void _doublePerf(boolean statisticsOn) throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		if (!statisticsOn) {
			analysis.setDefaultLogicalTypes(false);
			analysis.setCollectStatistics(false);
		}
		final Random random = new Random(314);
		final long sampleCount = 100_000_000_000L;
		boolean saveOutput = false;
		BufferedWriter bw = null;
		String[] samples = new String[10000];

		if (saveOutput)
			bw = new BufferedWriter(new FileWriter("/tmp/doublePerf.csv"));

		for (int i = 0; i < samples.length; i++)
			samples[i] = String.valueOf(random.nextDouble() * 1000000);

		long start = System.currentTimeMillis();

		long iters = 0;
		// Run for about reasonable number of seconds
		int seconds = 5;
		for (iters = 0; iters < sampleCount; iters++) {
			String sample = samples[(int)(iters%samples.length)];
			analysis.train(sample);
			if (bw != null)
				bw.write(sample + '\n');
			if (iters%100 == 0 && System.currentTimeMillis()  - start >= seconds * 1_000)
				break;

		}
		final TextAnalysisResult result = analysis.getResult();
		if (bw != null)
			bw.close();

		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), iters + 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getMinLength(), 12);
		Assert.assertEquals(result.getMaxLength(), 18);
		Assert.assertEquals(result.getRegExp(), analysis.getRegExp(KnownPatterns.ID.ID_DOUBLE));
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.DOUBLE);
		Assert.assertNull(result.getTypeQualifier());
		System.err.printf("Count %d, duration: %dms, ~%d per second\n", iters + 1, System.currentTimeMillis() - start, (iters  + 1)/seconds);

		// With Statistics & LogicalTypes
		//   - Count 16248501, duration: 10008ms, ~1,624,850 per second
		// No Statistics & No LogicalTypes
		//   - Count 44222501, duration: 10003ms, ~4,422,250 per second
	}

	@Test
	public void doublePerf() throws IOException {
		_doublePerf(true);
	}

	@Test
	public void doublePerfNoStatistics() throws IOException {
		_doublePerf(false);
	}
}
