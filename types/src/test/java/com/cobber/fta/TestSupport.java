package com.cobber.fta;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.text.NumberFormat;

import com.cobber.fta.core.FTAType;
import com.cobber.fta.core.Utils;

public class TestSupport {

	public static long countHistogram(final Histogram.Entry[] histogram) {
		long count = 0;
		for (final Histogram.Entry entry : histogram)
			count += entry.getCount();

		return count;
	}

	public static void checkHistogram(final TextAnalysisResult result, final int width, final boolean checkCounts) {
		// If no Statistics enabled then not much checking we can do
		if (!result.getConfig().isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS))
			return;

		// No Histogram support for STRING or BOOLEAN types
		if (FTAType.STRING.equals(result.getType()) || FTAType.BOOLEAN.equals(result.getType()))
			return;

		final Histogram.Entry[] histogram = result.getHistogram(width);

		if (histogram == null)
			return;

		if (checkCounts)
			assertEquals(countHistogram(histogram), result.getMatchCount());

		// The following is not equivalent to result.getDistinctCount() != 1 because a data set which has
		// 47.0, 47, 47.00 will have a distinctCount() of 3!!!
		if (!result.getMinValue().equals(result.getMaxValue()))
			assertTrue(histogram[0].getCount() != 0);
		assertTrue(histogram[width -1].getCount() != 0);
	}

	public static void checkQuantiles(final TextAnalysisResult result){
		// If no Statistics enabled then not much checking we can do
		if (!result.getConfig().isEnabled(TextAnalyzer.Feature.COLLECT_STATISTICS))
			return;

		if (result.getCardinality() == 0)
			return;

		if (result.getCardinality() < result.getConfig().getMaxCardinality()) {
			if (FTAType.LONG.equals(result.getType())) {
				final NumberFormat longFormatter = NumberFormat.getIntegerInstance(result.getConfig().getLocale());

				final long min = Utils.parseLong(result.getMinValue(), longFormatter);
				final long max = Utils.parseLong(result.getMaxValue(), longFormatter);

				assertEquals(Utils.parseLong(result.getValueAtQuantile(0.0), longFormatter), min);
				assertEquals(Utils.parseLong(result.getValueAtQuantile(1.0), longFormatter), max);
				assertEquals(Utils.parseLong(result.getCardinalityDetails().firstKey(), longFormatter), min);
				assertEquals(Utils.parseLong(result.getCardinalityDetails().lastKey(), longFormatter), max);
			}
			else if (FTAType.DOUBLE.equals(result.getType())) {
				final NumberFormat doubleFormatter = NumberFormat.getInstance(result.getConfig().getLocale());

				final double min = Utils.parseDouble(result.getMinValue(), doubleFormatter);
				final double max = Utils.parseDouble(result.getMaxValue(), doubleFormatter);

				assertEquals(Utils.parseDouble(result.getValueAtQuantile(0.0), doubleFormatter), min);
				assertEquals(Utils.parseDouble(result.getValueAtQuantile(1.0), doubleFormatter), max);
				assertEquals(Utils.parseDouble(result.getCardinalityDetails().firstKey(), doubleFormatter), min);
				assertEquals(Utils.parseDouble(result.getCardinalityDetails().lastKey(), doubleFormatter), max);
			}
			else {
				assertTrue(result.getValueAtQuantile(0.0).equalsIgnoreCase(result.getMinValue()));
				assertTrue(result.getValueAtQuantile(1.0).equalsIgnoreCase(result.getMaxValue()));
				assertTrue(result.getCardinalityDetails().firstKey().equalsIgnoreCase(result.getMinValue()));
				assertTrue(result.getCardinalityDetails().lastKey().equalsIgnoreCase(result.getMaxValue()));
			}
		}
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
		System.err.println("Entries: " + countHistogram(histogram));
		final long max = getMaxCount(histogram);
		long sizeX = max / 100;
		if (sizeX == 0)
			sizeX = max / 10;
		if (sizeX == 0)
			sizeX = max / 1;

		for (int i = 0; i < histogram.length; i++) {
			final long xCount = histogram[i].getCount()/sizeX;
			String output;
			if (histogram[i].getCount() == 0)
				output = "";
			else
				output = xCount == 0 ? "." : Utils.repeat('X', (int)xCount);
			System.err.printf("%d: %4.2f: %s%n", i, Double.valueOf(histogram[i].getHigh()), output);
		}
	}


}
