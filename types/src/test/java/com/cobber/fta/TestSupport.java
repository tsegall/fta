package com.cobber.fta;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.text.NumberFormat;
import java.text.ParseException;

import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.Utils;

public class TestSupport {
	public static void checkHistogram(final TextAnalysisResult result, int width) {
		// No Histogram support for STRING or BOOLEAN types
		if (FTAType.STRING.equals(result.getType()) || FTAType.BOOLEAN.equals(result.getType()))
			return;

		Histogram.Entry[] histogram = result.getHistogram(10);
		long histogramCount = 0;
		for (Histogram.Entry entry : histogram)
			histogramCount += entry.getCount();

		assertEquals(histogramCount, result.getMatchCount());
	}

	public static void checkQuantiles(final TextAnalysisResult result){
		// If no Statistics enabled then not much checking we can do
		if (!result.getConfig().isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS))
			return;

		if (result.getCardinality() < result.getConfig().getMaxCardinality()) {
			if (FTAType.LONG.equals(result.getType())) {
				NumberFormat longFormatter = NumberFormat.getIntegerInstance(result.getConfig().getLocale());

				long min = safeParseLong(result.getMinValue(), longFormatter);
				long max = safeParseLong(result.getMaxValue(), longFormatter);

				assertEquals(safeParseLong(result.getValueAtQuantile(0.0), longFormatter), min);
				assertEquals(safeParseLong(result.getValueAtQuantile(1.0), longFormatter), max);
				assertEquals(safeParseLong(result.getCardinalityDetails().firstKey(), longFormatter), min);
				assertEquals(safeParseLong(result.getCardinalityDetails().lastKey(), longFormatter), max);
			}
			else if (FTAType.DOUBLE.equals(result.getType())) {
				NumberFormat doubleFormatter = NumberFormat.getInstance(result.getConfig().getLocale());

				double min = safeParseDouble(result.getMinValue(), doubleFormatter);
				double max = safeParseDouble(result.getMaxValue(), doubleFormatter);

				assertEquals(safeParseDouble(result.getValueAtQuantile(0.0), doubleFormatter), min);
				assertEquals(safeParseDouble(result.getValueAtQuantile(1.0), doubleFormatter), max);
				assertEquals(safeParseDouble(result.getCardinalityDetails().firstKey(), doubleFormatter), min);
				assertEquals(safeParseDouble(result.getCardinalityDetails().lastKey(), doubleFormatter), max);
			}
			else {
				assertTrue(result.getValueAtQuantile(0.0).equalsIgnoreCase(result.getMinValue()));
				assertTrue(result.getValueAtQuantile(1.0).equalsIgnoreCase(result.getMaxValue()));
				assertTrue(result.getCardinalityDetails().firstKey().equalsIgnoreCase(result.getMinValue()));
				assertTrue(result.getCardinalityDetails().lastKey().equalsIgnoreCase(result.getMaxValue()));
			}
		}
	}

	private static long safeParseLong(final String input, final NumberFormat longFormatter) {
		final String trimmed = input.trim();
		long ret;

		try {
			ret = Long.parseLong(trimmed);
		}
		catch (NumberFormatException e) {
			try {
				ret = longFormatter.parse(trimmed).longValue();
			} catch (ParseException pe) {
				throw new NumberFormatException(pe.getMessage());
			}
		}

		return ret;
	}

	private static double safeParseDouble(final String input, final NumberFormat doubleFormatter) {
		final String trimmed = input.trim();
		double ret;

		try {
			ret = Double.parseDouble(trimmed);
		}
		catch (NumberFormatException e) {
			try {
				ret = doubleFormatter.parse(trimmed.charAt(0) == '+' ? trimmed.substring(1) : trimmed).doubleValue();
			} catch (ParseException pe) {
				throw new NumberFormatException(pe.getMessage());
			}
		}

		return ret;
	}

	public static void dumpRaw(final Histogram.Entry[] histogram) {
		for (int i = 0; i < histogram.length; i++)
			System.err.printf("%d: %s(%4.2f)-%s(%4.2f): %d%n",
					i, histogram[i].getLow(), histogram[i].getLowCut(),
					histogram[i].getHigh(), histogram[i].getHighCut(), histogram[i].getCount());
	}

	public static void dumpRawDouble(final Histogram.Entry[] histogram) {
		for (int i = 0; i < histogram.length; i++)
			System.err.printf("%d: %4.2f-%4.2f: %d%n", i, Double.valueOf(histogram[i].getLow()), Double.valueOf(histogram[i].getHigh()), histogram[i].getCount());
	}

	private static long getMaxCount(final Histogram.Entry[] histogram) {
		long max = Long.MIN_VALUE;

		for (int i = 0; i < histogram.length; i++)
			if (histogram[i].getCount() > max)
				max = histogram[i].getCount();

		return max;
	}

	public static void dumpPicture(final Histogram.Entry[] histogram) {
		long max = getMaxCount(histogram);
		long sizeX = max / 100;
		if (sizeX == 0)
			sizeX = max / 10;
		if (sizeX == 0)
			sizeX = max / 1;

		for (int i = 0; i < histogram.length; i++) {
			long xCount = histogram[i].getCount()/sizeX;
			String output = xCount == 0 ? "." : Utils.repeat('X', (int)xCount);
			System.err.printf("%d: %4.2f: %s%n", i, Double.valueOf(histogram[i].getHigh()), output);
		}
	}


}
