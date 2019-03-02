package com.cobber.fta;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestLongs {
	public void _variableLengthPositiveInteger(boolean collectStatistics) throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		if (!collectStatistics)
			analysis.setCollectStatistics(false);

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
		if (collectStatistics) {
			Assert.assertEquals(result.getMinValue(), "0");
			Assert.assertEquals(result.getMaxValue(), "4499045");
		}
		Assert.assertEquals(result.getMinLength(), 1);
		Assert.assertEquals(result.getMaxLength(), 7);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void variableLengthPositiveInteger() throws IOException {
		_variableLengthPositiveInteger(true);
	}

	@Test
	public void variableLengthPositiveInteger_ns() throws IOException {
		_variableLengthPositiveInteger(false);
	}

	public void _variableLengthInteger(boolean collectStatistics) throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		if (!collectStatistics)
			analysis.setCollectStatistics(false);
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
		Assert.assertEquals(result.getRegExp(), "[+-]?\\d{1,6}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getTypeQualifier(), "SIGNED");
		if (collectStatistics) {
			Assert.assertEquals(result.getMinValue(), "-100000");
			Assert.assertEquals(result.getMaxValue(), "10000");
		}

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void variableLengthInteger() throws IOException {
		_variableLengthInteger(true);
	}

	@Test
	public void variableLengthInteger_ns() throws IOException {
		_variableLengthInteger(false);
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

			String regExp = "[+-]?[\\d" + RegExpGenerator.slosh(formatSymbols.getGroupingSeparator()) + "]";
			regExp += Utils.regExpLength(minValue.length(), maxValue.length());
			Assert.assertEquals(result.getRegExp(), regExp);
			Assert.assertEquals(result.getConfidence(), 1.0);

			for (String sample : samples) {
				Assert.assertTrue(sample.matches(regExp), sample);
			}
		}
	}

	@Test
	public void localeLongTest() throws IOException {
		final Random random = new Random(1);
		final int SAMPLE_SIZE = 1000;
		Locale[] locales = DateFormat.getAvailableLocales();
//		Locale[] locales = new Locale[] { Locale.forLanguageTag("mk-MK") };
//		Locale[] locales = new Locale[] { Locale.forLanguageTag("ar-JO") };

		for (Locale locale : locales) {
			long min = Long.MAX_VALUE;
			long absMin = Long.MAX_VALUE;
			long max = Long.MIN_VALUE;
			long absMax = Long.MIN_VALUE;
			String absMinValue = String.valueOf(absMin);
			String absMaxValue = String.valueOf(max);
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

			String negPrefix = TestUtils.getNegativePrefix(locale);
			String negSuffix = TestUtils.getNegativeSuffix(locale);

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
				if (l > max) {
					max = l;
				}
				if (Math.abs(l) < absMin) {
					absMin = Math.abs(l);
					absMinValue = nf.format(Math.abs(l)).toString();
				}
				if (Math.abs(l) > absMax) {
					absMax = Math.abs(l);
					absMaxValue = nf.format(Math.abs(l)).toString();
				}

				samples.add(sample);
				analysis.train(sample);
			}

			final TextAnalysisResult result = analysis.getResult();

//			System.err.printf("Locale '%s', re: '%s', grouping: %s, negPrefix: %s, negSuffix: %s, min: %s, max: %s, absMax:%s.\n",
//					locale, result.getRegExp(), grp, negPrefix, negSuffix, String.valueOf(min), String.valueOf(max), absMinValue);

			Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
			Assert.assertEquals(result.getTypeQualifier(), "SIGNED,GROUPING");
			Assert.assertEquals(result.getSampleCount(), SAMPLE_SIZE);
			Assert.assertEquals(result.getMatchCount(), SAMPLE_SIZE);
			Assert.assertEquals(result.getNullCount(), 0);
			Assert.assertEquals(result.getMinValue(), String.valueOf(min));
			Assert.assertEquals(result.getMaxValue(), String.valueOf(max));
			Assert.assertEquals(result.getLeadingZeroCount(), 0);

			String regExp = "";
			if (!negPrefix.isEmpty())
				regExp += negPrefix;
			regExp += "[\\d" + RegExpGenerator.slosh(formatSymbols.getGroupingSeparator()) + "]";
			regExp += Utils.regExpLength(absMinValue.length(), absMaxValue.length());
			if (!negSuffix.isEmpty())
				regExp += negSuffix;
			Assert.assertEquals(result.getDecimalSeparator(), '.');

			Assert.assertEquals(result.getRegExp(), regExp);
			Assert.assertEquals(result.getConfidence(), 1.0);

			for (String sample : samples) {
				Assert.assertTrue(sample.matches(regExp), sample + " " + regExp);
			}
		}
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
	public void paddedLongs() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("RowID");
		final String inputs[] = new String[] {
			    "    0", "    1", "    2", "    3", "    4", "    5", "    6", "    7", "    8", "    9",
			    "    10", "    11", "    12", "    13", "    14", "    15", "    16", "    17", "    18", "    19",
			    "    20", "    21", "    22", "    23", "    24", "    25", "    26", "    27", "    28", "    29"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertNull(result.getTypeQualifier());
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);
		Assert.assertEquals(result.getMinValue(), "0");
		Assert.assertEquals(result.getMaxValue(), "29");
		Assert.assertEquals(result.getRegExp(), "\\p{javaWhitespace}*\\d{1,2}");

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
	}

	@Test
	public void signedLongs() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer("SUB_ACTIVE_DATE_ONLY");
		final String inputs[] = new String[] {
				"+400089", "2000931", "-3287392873", "-327398267", "-34", "56", "93823908", "34567", "-757363", "0",
				"4345689", "2333931", "4457892873", "+398267", "-3334464", "78912356", "93823908", "34567", "-757363", "0",
				"489", "931", "-3287373", "-398267", "-234534", "565656", "23908", "7734567", "-99757363", "0"
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertEquals(result.getTypeQualifier(), "SIGNED");
		Assert.assertEquals(result.getRegExp(), "[+-]?\\d{1,10}");
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getSampleCount(), inputs.length);
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getMatchCount(), inputs.length);

		for (int i = 0; i < inputs.length; i++) {
			Assert.assertTrue(inputs[i].matches(result.getRegExp()));
		}
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
		String regExp = "[+-]?[\\d,]{";
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
	public void noStatistics() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		analysis.setCollectStatistics(false);
		final Random random = new Random(314);
		String[] samples = new String[10000];

		int iters = 0;
		for (iters = 0; iters < samples.length; iters++) {
			analysis.train(String.valueOf(random.nextInt(100000000)));
		}

		final TextAnalysisResult result = analysis.getResult();

		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), iters);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getMinLength(), 5);
		Assert.assertEquals(result.getMaxLength(), 8);
		Assert.assertEquals(result.getRegExp(), "\\d{5,8}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertNull(result.getTypeQualifier());
	}

	@Test
	public void longPerf() throws IOException {
		final TextAnalyzer analysis = new TextAnalyzer();
		analysis.setDefaultLogicalTypes(false);
		analysis.setCollectStatistics(false);
		final Random random = new Random(314);
		final long sampleCount = 100_000_000_000L;
		boolean saveOutput = false;
		BufferedWriter bw = null;
		String[] samples = new String[10000];

		if (saveOutput)
			bw = new BufferedWriter(new FileWriter("/tmp/longPerf.csv"));

		for (int i = 0; i < samples.length; i++)
			samples[i] = String.valueOf(random.nextInt(100000000));

		long start = System.currentTimeMillis();

		long iters = 0;
		// Run for about 10 seconds
		for (iters = 0; iters < sampleCount; iters++) {
			String sample = samples[(int)(iters%samples.length)];
			analysis.train(sample);
			if (bw != null)
				bw.write(sample + '\n');
			if (iters%100 == 0 && System.currentTimeMillis()  - start >= 10_000)
				break;

		}
		final TextAnalysisResult result = analysis.getResult();
		if (bw != null)
			bw.close();

		Assert.assertEquals(result.getOutlierCount(), 0);
		Assert.assertEquals(result.getMatchCount(), iters + 1);
		Assert.assertEquals(result.getNullCount(), 0);
		Assert.assertEquals(result.getMinLength(), 5);
		Assert.assertEquals(result.getMaxLength(), 8);
		Assert.assertEquals(result.getRegExp(), "\\d{5,8}");
		Assert.assertEquals(result.getConfidence(), 1.0);
		Assert.assertEquals(result.getType(), PatternInfo.Type.LONG);
		Assert.assertNull(result.getTypeQualifier());
		System.err.printf("Count %d, duration: %dms, ~%d per second\n", iters + 1, System.currentTimeMillis() - start, (iters  + 1)/10);

		// With Statistics & LogicalTypes
		//   - Count 109980301, duration: 10003ms, ~10,998,030 per second
		// No Statistics & No LogicalTypes
		//   - Count 15141740, duration: 10002ms, ~15,141,740 per second
	}
}
