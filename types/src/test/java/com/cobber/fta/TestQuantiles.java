/*
 * Copyright 2017-2022 Tim Segall
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.dates.DateTimeParser;

/**
 * Quantile Tests for the following Data Types
 *
 * BOOLEAN (DONE)
 * DOUBLE (DONE)
 * LONG (DONE)
 *
 * LOCALDATE
 * LOCALDATETIME
 * LOCALTIME
 * OFFSETDATETIME
 * ZONEDDATETIME (pretends to be an OFFSETDATETIME)
 *
 * STRING  (Note: no support for Quantile determination once the Cardinality Cache is exceeded)
 */
public class TestQuantiles {
	public void baseLong(final long size, final double relativeAccuracy) throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("baseLong" + size);
		analysis.setQuantileRelativeAccuracy(relativeAccuracy);

		for (long i = 0; i < size; i++)
			analysis.train(String.valueOf(i));

		// Test pre getResult()
		String serialized = analysis.serialize();
		final TextAnalyzer hydrated = TextAnalyzer.deserialize(serialized);
		assertEquals(serialized, hydrated.serialize());

		// Test a hydrated object
		serialized = hydrated.serialize();
		final TextAnalyzer rehydrated = TextAnalyzer.deserialize(serialized);
		assertEquals(serialized, rehydrated.serialize());

		TextAnalysisResult result = rehydrated.getResult();

		// Test post getResult()
		serialized = rehydrated.serialize();
		assertEquals(serialized, TextAnalyzer.deserialize(serialized).serialize());

		assertEquals(result.getSampleCount(), size);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LONG);

		assertEquals(result.getMinValue(), "0");
		assertEquals(result.getMaxValue(), String.valueOf(size - 1));
		String q0_0 = result.getValueAtQuantile(0);
		String q0_5 = result.getValueAtQuantile(.5);
		String q1_0 = result.getValueAtQuantile(1.0);

		long actual_0_0 = Long.valueOf(q0_0);
		long actual_0_5 = Long.valueOf(q0_5);
		long actual_1_0 = Long.valueOf(q1_0);

		long expected_0_0 = 0;
		long expected_0_5 = (size - 1) / 2;
		long expected_1_0 = size - 1;

		assertEquals(actual_0_0, expected_0_0, expected_0_0 * relativeAccuracy);
		assertEquals(actual_0_5, expected_0_5, expected_0_5 * relativeAccuracy);
		assertEquals(actual_1_0, expected_1_0, expected_1_0 * relativeAccuracy);

		assertEquals(result.getMinLength(), 1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void long10() throws IOException, FTAException {
		baseLong(10, .01);
		baseLong(10, .001);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void long100() throws IOException, FTAException {
		baseLong(100, .01);
		baseLong(100, .001);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void long1K() throws IOException, FTAException {
		baseLong(1_000, .01);
		baseLong(1_000, .001);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void long10K() throws IOException, FTAException {
		baseLong(10_000, .01);
		baseLong(10_000, .001);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void long15K() throws IOException, FTAException {
		baseLong(15_000, .01);
		baseLong(15_000, .001);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void long100K() throws IOException, FTAException {
		baseLong(100_000, .01);
		baseLong(100_000, .001);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void long1M() throws IOException, FTAException {
		baseLong(1_000_000, .01);
		baseLong(1_000_000, .001);
	}

	public void negativeLong(final long size, final double relativeAccuracy) throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("baseLong" + size);
		analysis.setQuantileRelativeAccuracy(relativeAccuracy);

		for (long i = -size + 1; i < size; i++)
			analysis.train(String.valueOf(i));

		final TextAnalysisResult result = analysis.getResult();
//		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), size * 2 - 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LONG);

		assertEquals(result.getMinValue(), String.valueOf(-size + 1));
		assertEquals(result.getMaxValue(), String.valueOf(size - 1));
		String q0_0 = result.getValueAtQuantile(0);
		String q0_5 = result.getValueAtQuantile(.5);
		String q1_0 = result.getValueAtQuantile(1.0);

		long actual_0_0 = Long.valueOf(q0_0);
		long actual_0_5 = Long.valueOf(q0_5);
		long actual_1_0 = Long.valueOf(q1_0);

		long expected_0_0 = -size + 1;
		long expected_0_5 = 0;
		long expected_1_0 = size - 1;

		assertEquals(actual_0_0, expected_0_0, Math.abs(expected_0_0) * relativeAccuracy);
		assertEquals(actual_0_5, expected_0_5, expected_0_5 * relativeAccuracy);
		assertEquals(actual_1_0, expected_1_0, expected_1_0 * relativeAccuracy);

		assertEquals(result.getMinLength(), 1);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void netgativeLong10() throws IOException, FTAException {
		negativeLong(10, .01);
		negativeLong(10, .001);
	}

	public void baseDouble(final long size, final double relativeAccuracy) throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("baseLong" + size );
		analysis.setQuantileRelativeAccuracy(relativeAccuracy);

		for (int i = 0; i < size; i++)
			analysis.train(String.valueOf(1.0 * i));

		// Test pre getResult()
		String serialized = analysis.serialize();
		final TextAnalyzer hydrated = TextAnalyzer.deserialize(serialized);
		assertEquals(serialized, hydrated.serialize());

		// Test a hydrated object
		serialized = hydrated.serialize();
		final TextAnalyzer rehydrated = TextAnalyzer.deserialize(serialized);
		assertEquals(serialized, rehydrated.serialize());

		TextAnalysisResult result = rehydrated.getResult();

		// Test post getResult()
		serialized = rehydrated.serialize();
		assertEquals(serialized, TextAnalyzer.deserialize(serialized).serialize());

		assertEquals(result.getSampleCount(), size);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.DOUBLE);

		assertEquals(result.getMinValue(), "0.0");
		assertEquals(result.getMaxValue(), String.valueOf(1.0 * (size - 1)));

		String[] quantiles = result.getValuesAtQuantiles(new double[] { 0.0, 0.5, 1.0 });
		String q0_0 = quantiles[0];
		String q0_5 = quantiles[1];
		String q1_0 = quantiles[2];

		double actual_0_0 = Double.valueOf(q0_0);
		double actual_0_5 = Double.valueOf(q0_5);
		double actual_1_0 = Double.valueOf(q1_0);

		double expected_0_0 = 0.0;
		double expected_0_5 = Math.floor(1.0 * (size - 1) / 2);
		double expected_1_0 = 1.0 * (size - 1);

		assertEquals(actual_0_0, expected_0_0, expected_0_0 * relativeAccuracy);
		assertEquals(actual_0_5, expected_0_5, expected_0_5 * relativeAccuracy);
		assertEquals(actual_1_0, expected_1_0, expected_1_0 * relativeAccuracy);

		assertEquals(result.getMinLength(), 3);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void double10() throws IOException, FTAException {
		baseDouble(10, .01);
		baseDouble(10, .001);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void double100() throws IOException, FTAException {
		baseDouble(100, .01);
		baseDouble(100, .001);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void double1K() throws IOException, FTAException {
		baseDouble(1_000, .01);
		baseDouble(1_000, .001);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void double10K() throws IOException, FTAException {
		baseDouble(10_000, .01);
		baseDouble(10_000, .001);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void double100K() throws IOException, FTAException {
		baseDouble(100_000, .01);
		baseDouble(100_000, .001);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void double1M() throws IOException, FTAException {
		baseDouble(1_000_000, .01);
		baseDouble(1_000_000, .001);
	}

	public void baseYYYY(final long size) throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("year");
		final double RELATIVE_ACCURACY = 0.01;

		for (int i = 1900; i < 1900 + size; i++)
			analysis.train(String.valueOf(i));

		analysis.train("0");

		// Test pre getResult()
		String serialized = analysis.serialize();
		final TextAnalyzer hydrated = TextAnalyzer.deserialize(serialized);
		assertEquals(serialized, hydrated.serialize());

		// Test a hydrated object
		serialized = hydrated.serialize();
		final TextAnalyzer rehydrated = TextAnalyzer.deserialize(serialized);
		assertEquals(serialized, rehydrated.serialize());

		TextAnalysisResult result = rehydrated.getResult();

		// Test post getResult()
		serialized = rehydrated.serialize();
		assertEquals(serialized, TextAnalyzer.deserialize(serialized).serialize());

		assertEquals(result.getSampleCount(), size + 1);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATE);

		assertEquals(result.getMinValue(), "1900");
		assertEquals(result.getMaxValue(), String.valueOf(1900 + size - 1));
		String q0_0 = result.getValueAtQuantile(0);
		String q0_5 = result.getValueAtQuantile(.5);
		String q1_0 = result.getValueAtQuantile(1.0);

		long actual_0_0 = Long.valueOf(q0_0);
		long actual_0_5 = Long.valueOf(q0_5);
		long actual_1_0 = Long.valueOf(q1_0);

		long expected_0_0 = 1900;
		long expected_0_5 = 1900 + (size - 1) / 2;
		long expected_1_0 = 1900 + (size - 1);

		assertEquals(actual_0_0, expected_0_0, expected_0_0 * RELATIVE_ACCURACY);
		assertEquals(actual_0_5, expected_0_5, expected_0_5 * RELATIVE_ACCURACY);
		assertEquals(actual_1_0, expected_1_0, expected_1_0 * RELATIVE_ACCURACY);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void year10() throws IOException, FTAException {
		baseYYYY(10);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void year100() throws IOException, FTAException {
		baseYYYY(100);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void baseYYYYMMDD() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("Date");

		final String testCases[] = {
				"20180703", "20170701", "20160701", "20140701", "20130702", "20130101", "20120908", "20120830", "20100701", "19960701",
				"0",
				"19880101", "19960701", "20100701", "20120830", "20120908", "20130101", "20130702", "20140701", "20160701", "20170701"
		};

		for (String testCase : testCases)
			analysis.train(testCase);

		// Test pre getResult()
		String serialized = analysis.serialize();
		final TextAnalyzer hydrated = TextAnalyzer.deserialize(serialized);
		assertEquals(serialized, hydrated.serialize());

		// Test a hydrated object
		serialized = hydrated.serialize();
		final TextAnalyzer rehydrated = TextAnalyzer.deserialize(serialized);
		assertEquals(serialized, rehydrated.serialize());

		TextAnalysisResult result = rehydrated.getResult();

		// Test post getResult()
		serialized = rehydrated.serialize();
		assertEquals(serialized, TextAnalyzer.deserialize(serialized).serialize());

		assertEquals(result.getSampleCount(), testCases.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATE);

		assertEquals(result.getMinValue(), "19880101");
		assertEquals(result.getMaxValue(), "20180703");

		String actual_0_0 = result.getValueAtQuantile(0);
		String actual_0_5 = result.getValueAtQuantile(.5);
		String actual_1_0 = result.getValueAtQuantile(1.0);

		assertEquals(actual_0_0, "19880101");
		assertEquals(actual_0_5, "20130101");
		assertEquals(actual_1_0, "20180703");
	}

	public void baseLocalDateTime(final long size, final double relativeAccuracy) throws IOException, FTAException {
		final Locale locale = Locale.forLanguageTag("en-US");
		final TextAnalyzer analysis = new TextAnalyzer("localeDateTime");
		analysis.setQuantileRelativeAccuracy(relativeAccuracy);
		analysis.setLocale(locale);
		DateTimeParser dateTimeParser = new DateTimeParser().withLocale(locale).withNumericMode(false);
		DateTimeFormatter dtf = dateTimeParser.ofPattern("MM/dd/yyyy h:mm:ss a");
		LocalDateTime start = LocalDateTime.parse("01/01/1990 8:30:00 AM", dtf);

		LocalDateTime ldt = start;
		LocalDateTime mid = null;

		for (int i = 0; i < size; i++) {
			final String sample = dtf.format(ldt);
			analysis.train(sample);
			if (mid == null && i > (size -1) / 2)
				mid = ldt;
			if (i + 1 < size)
				ldt = ldt.minusHours(25).minusMinutes(25).minusSeconds(25);
		}

		// Test pre getResult()
		String serialized = analysis.serialize();
		final TextAnalyzer hydrated = TextAnalyzer.deserialize(serialized);
		assertEquals(serialized, hydrated.serialize());

		// Test a hydrated object
		serialized = hydrated.serialize();
		final TextAnalyzer rehydrated = TextAnalyzer.deserialize(serialized);
		assertEquals(serialized, rehydrated.serialize());

		TextAnalysisResult result = rehydrated.getResult();

		// Test post getResult()
		serialized = rehydrated.serialize();
		assertEquals(serialized, TextAnalyzer.deserialize(serialized).serialize());

		assertEquals(result.getSampleCount(), size);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), size);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}/\\d{2}/\\d{4} \\d{1,2}:\\d{2}:\\d{2} (?i)(AM|PM)");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeQualifier(), "MM/dd/yyyy h:mm:ss a");

		String q0_0 = result.getValueAtQuantile(0);
		String q0_5 = result.getValueAtQuantile(.5);
		String q1_0 = result.getValueAtQuantile(1.0);

		if (size <= analysis.getMaxCardinality()) {
			assertEquals(q0_0, dtf.format(ldt));
			assertEquals(q0_5, dtf.format(mid));
			assertEquals(q1_0, dtf.format(start));
		}
		else {
			System.err.printf("0 - actual %s, expected %s\n", q0_0, dtf.format(ldt));
			System.err.printf("0 - actual %s, expected %s\n", q0_5, dtf.format(mid));
			System.err.printf("0 - actual %s, expected %s\n", q1_0, dtf.format(start));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void localDateTime10() throws IOException, FTAException {
		baseLocalDateTime(10, .01);
		baseLocalDateTime(10, .001);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void localDateTime10K() throws IOException, FTAException {
		baseLocalDateTime(10_000, .01);
		baseLocalDateTime(10_000, .001);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void localDateTime100K() throws IOException, FTAException {
		baseLocalDateTime(100_000, .01);
		baseLocalDateTime(100_000, .001);
	}

	public void baseString(final long size) throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("string");

		String min = null;
		String mid = null;
		String max = null;

		long num = size;
		int digits = 0;
		for(; num != 0; num/=10, ++digits)
			;
		String format = "%0" + digits + "d";
		char[] mapping = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J' };

		StringBuilder s = new StringBuilder();
		String sample = null;

		for (int i = 0; i < size; i++) {
			s.setLength(0);
			String result = String.format(format, i);
			for (int l = 0; l < result.length(); l++) {
				s.append(mapping[result.charAt(l) - '0']);
			}
			sample = s.toString();

			if (i == 0)
				min = sample;

			analysis.train(sample);

			if (i <= (size - 1) / 2)
				mid = sample;
		}
		max = sample;

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), size);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), size);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\p{IsAlphabetic}{" + digits + "}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.STRING);
		assertNull(result.getTypeQualifier());

		String q0_0 = result.getValueAtQuantile(0);
		String q0_5 = result.getValueAtQuantile(.5);
		String q1_0 = result.getValueAtQuantile(1.0);

		assertEquals(q0_0, min);
		assertEquals(q0_5, mid);
		assertEquals(q1_0, max);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void string10() throws IOException, FTAException {
		baseString(10);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void string10K() throws IOException, FTAException {
		baseString(10_000);
	}

	public void boolean100K(final int size, final boolean input) throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("boolean100K");

		for (int i = 0; i < size; i++)
			analysis.train(String.valueOf(input));

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getSampleCount(), size);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), size);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "(?i)(FALSE|TRUE)");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.BOOLEAN);
		assertEquals(result.getTypeQualifier(), "TRUE_FALSE");

		Boolean q0_0 = Boolean.valueOf(result.getValueAtQuantile(0));
		Boolean q0_5 = Boolean.valueOf(result.getValueAtQuantile(.5));
		Boolean q1_0 = Boolean.valueOf(result.getValueAtQuantile(1.0));

		assertEquals(q0_0, input);
		assertEquals(q0_5, input);
		assertEquals(q1_0, input);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void true10K() throws IOException, FTAException {
		boolean100K(10_000, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void false10K() throws IOException, FTAException {
		boolean100K(10_000, false);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void true100K() throws IOException, FTAException {
		boolean100K(100_000, true);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void false100K() throws IOException, FTAException {
		boolean100K(100_000, false);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void booleanMixed100K() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("mixed100K");
		final int size = 100_000;

		for (int i = 0; i < size; i++)
			analysis.train(i < size/10 ? "true" : "false");

		// Test pre getResult()
		String serialized = analysis.serialize();
		final TextAnalyzer hydrated = TextAnalyzer.deserialize(serialized);
		assertEquals(serialized, hydrated.serialize());

		// Test a hydrated object
		serialized = hydrated.serialize();
		final TextAnalyzer rehydrated = TextAnalyzer.deserialize(serialized);
		assertEquals(serialized, rehydrated.serialize());

		TextAnalysisResult result = rehydrated.getResult();

		// Test post getResult()
		serialized = rehydrated.serialize();
		assertEquals(serialized, TextAnalyzer.deserialize(serialized).serialize());

		assertEquals(result.getSampleCount(), size);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), size);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "(?i)(FALSE|TRUE)");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.BOOLEAN);
		assertEquals(result.getTypeQualifier(), "TRUE_FALSE");

		Boolean q0_0 = Boolean.valueOf(result.getValueAtQuantile(0));
		Boolean q0_5 = Boolean.valueOf(result.getValueAtQuantile(.5));
		Boolean q1_0 = Boolean.valueOf(result.getValueAtQuantile(1.0));

		assertEquals(q0_0, false);
		assertEquals(q0_5, false);
		assertEquals(q1_0, true);
	}

	public void baseLocalTime(final long size, final double relativeAccuracy) throws IOException, FTAException {
		final Locale locale = Locale.forLanguageTag("en-US");
		final TextAnalyzer analysis = new TextAnalyzer("localeDateTime");
		analysis.setQuantileRelativeAccuracy(relativeAccuracy);
		analysis.setLocale(locale);

		DateTimeParser dateTimeParser = new DateTimeParser().withLocale(locale).withNumericMode(false);
		DateTimeFormatter dtf = dateTimeParser.ofPattern("HH:mm:ss.SSS");

		LocalTime start = LocalTime.MIN;
		LocalTime mid = null;
		LocalTime lt = start;

		for (int i = 0; i < size; i++) {
			final String sample = dtf.format(lt);
			analysis.train(sample);
			if (i <= (size - 1) / 2)
				mid = lt;
			if (i + 1 < size)
				lt = size > 86400 ? lt.plusNanos(1_000_000) : lt.plusSeconds(1);
		}

		// Test pre getResult()
		String serialized = analysis.serialize();
		final TextAnalyzer hydrated = TextAnalyzer.deserialize(serialized);
		assertEquals(serialized, hydrated.serialize());

		// Test a hydrated object
		serialized = hydrated.serialize();
		final TextAnalyzer rehydrated = TextAnalyzer.deserialize(serialized);
		assertEquals(serialized, rehydrated.serialize());

		TextAnalysisResult result = rehydrated.getResult();

		// Test post getResult()
		serialized = rehydrated.serialize();
		assertEquals(serialized, TextAnalyzer.deserialize(serialized).serialize());

		assertEquals(result.getSampleCount(), size);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), size);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{2}:\\d{2}:\\d{2}\\.\\d{3}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LOCALTIME);
		assertEquals(result.getTypeQualifier(), "HH:mm:ss.SSS");

		String q0_0 = result.getValueAtQuantile(0);
		String q0_5 = result.getValueAtQuantile(.5);
		String q1_0 = result.getValueAtQuantile(1.0);

		if (size <= analysis.getMaxCardinality()) {
			assertEquals(q0_0, dtf.format(start));
			assertEquals(q0_5, dtf.format(mid));
			assertEquals(q1_0, dtf.format(lt));
		}
		else {
			System.err.printf("0 - actual %s, expected %s\n", q0_0, dtf.format(start));
			System.err.printf("0 - actual %s, expected %s\n", q0_5, dtf.format(mid));
			System.err.printf("0 - actual %s, expected %s\n", q1_0, dtf.format(lt));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void localTime10() throws IOException, FTAException {
		baseLocalTime(10, .01);
		baseLocalTime(10, .001);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void localTime10K() throws IOException, FTAException {
		baseLocalTime(10_000, .01);
		baseLocalTime(10_000, .001);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void localTime100K() throws IOException, FTAException {
		baseLocalTime(100_000, .01);
		baseLocalTime(100_000, .001);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void withSpaces() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("withSpaces");
		final int SIZE = 100;

		for (int i = 0; i < SIZE; i++) {
			analysis.train(String.valueOf(i));
			analysis.train(" " + String.valueOf(i));
		}
		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), SIZE * 2);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), SIZE * 2);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "[ 	]*\\d{1,2}");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LONG);
		assertNull(result.getTypeQualifier(), "HH:mm:ss.SSS");

		String q0_0 = result.getValueAtQuantile(0);
		String q0_5 = result.getValueAtQuantile(.5);
		String q1_0 = result.getValueAtQuantile(1.0);

		assertEquals(q0_0, "0");
		assertEquals(q0_5, "49");
		assertEquals(q1_0, "99");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void leadingPlus() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("latitude");
		final String testCases[] = {
				"+37.4417477", "+37.3159742", "+37.3504209", "+37.3515288", "+37.3504209",
				"+37.2303267", "+37.3805678", "+37.4157026", "+37.3511428", "+37.3444821",
				"+37.3871047", "+37.3286828", "+37.3444821", "+37.4182561", "+37.2831109",
				"+37.3159742", "+37.3444821", "+37.2568494", "+37.0185369", "+37.2172621",
				"+37.3805678", "+37.3504209", "+37.0185369", "+37.3041649", "+37.3444821",
				"+37.4140278", "+37.3504209", "+37.3504209", "+37.4124017", "+37.4436377"
		};

		for (String testCase : testCases)
			analysis.train(testCase);

		final TextAnalysisResult result = analysis.getResult();

		assertEquals(result.getSampleCount(), testCases.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), testCases.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "([+-]?([0-9]|[0-8][0-9])\\.\\d+)|[+-]?90\\.0+|0");
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeQualifier(), "COORDINATE.LATITUDE_DECIMAL");

		String q0_0 = result.getValueAtQuantile(0);
		String q0_5 = result.getValueAtQuantile(.5);
		String q1_0 = result.getValueAtQuantile(1.0);

		assertEquals(q0_0, "+37.0185369");
		assertEquals(q0_5, "+37.3504209");
		assertEquals(q1_0, "+37.4436377");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void normalCurve() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("normalCurve");
		final SecureRandom random = new SecureRandom();
		final int SIZE = 100000;

		for (int i = 0; i < SIZE; i++)
			analysis.train(String.valueOf(random.nextGaussian()*100));

		// Test pre getResult()
		String serialized = analysis.serialize();
		final TextAnalyzer hydrated = TextAnalyzer.deserialize(serialized);
		assertEquals(serialized, hydrated.serialize());

		// Test a hydrated object
		serialized = hydrated.serialize();
		final TextAnalyzer rehydrated = TextAnalyzer.deserialize(serialized);
		assertEquals(serialized, rehydrated.serialize());

		TextAnalysisResult result = rehydrated.getResult();

		// Test post getResult()
		serialized = rehydrated.serialize();
		assertEquals(serialized, TextAnalyzer.deserialize(serialized).serialize());

		assertEquals(result.getSampleCount(), SIZE);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), SIZE);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.DOUBLE);
		assertEquals(result.getTypeQualifier(), "SIGNED");

		assertEquals(result.getMean(), 0.0, 1.0);
		assertEquals(result.getStandardDeviation(), 100, 1);

		String q0_0 = result.getValueAtQuantile(0);
		String q0_5 = result.getValueAtQuantile(.5);
		String q1_0 = result.getValueAtQuantile(1.0);

		// Median should be seriously close to 0
		assertEquals(Double.valueOf(q0_5), 0.0, 1);

		// 3.5 Standard Deviations should cover low and high points
		assertTrue(Double.valueOf(q0_0) < -350);
		assertTrue(Double.valueOf(q1_0) > 350);

		// 101 because we want 0.0 and 1.0 plus everything in between
		double[] percentiles = new double[101];
		double value = 0.0;
		for (int i = 0; i < 100; i++) {
			percentiles[i] = value;
			value += .01;
		}
		// Make sure the last one is precisely 1.0
		percentiles[100] = 1.0;

		String[] answers = result.getValuesAtQuantiles(percentiles);
		assertEquals(answers[0], q0_0);
		assertEquals(answers[50], q0_5);
		assertEquals(answers[100], q1_0);

		for (int i = 10; i < 50; i++) {
			double low = Double.parseDouble(answers[i]);
			double high = Double.parseDouble(answers[100 - i]);
//			System.err.printf("low: %f, high: %f\n", low, high);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.QUANTILES })
	public void mergeTest() throws IOException, FTAException {
		final double RELATIVE_ACCURACY = 0.01;
		final TextAnalyzer shardOne = new TextAnalyzer("shardOne");
		final TextAnalyzer shardTwo = new TextAnalyzer("shardTwo");
		final int size = 100000;

		for (long i = 0; i < size; i++)
			shardOne.train(String.valueOf(i));
		shardOne.setTotalCount(size);
		final TextAnalyzer hydratedOne = TextAnalyzer.deserialize(shardOne.serialize());

		for (int i = size; i <= 2 * size; i++)
			shardTwo.train(String.valueOf(i));
		shardTwo.setTotalCount(size);
		final TextAnalyzer hydratedTwo = TextAnalyzer.deserialize(shardTwo.serialize());

		// Merge the two hydrated TextAnalyzers
		final TextAnalyzer merged = TextAnalyzer.merge(hydratedOne, hydratedTwo);

		TextAnalysisResult result = merged.getResult();


		assertEquals(result.getTotalCount(), 2 * size);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getConfidence(), 1.0);
		assertEquals(result.getType(), FTAType.LONG);
		assertEquals(result.getMinValue(), "0");
		assertEquals(result.getMaxValue(), "200000");
		assertNull(result.getTypeQualifier(), "SIGNED");

		assertEquals(result.getMean(), 100000.0);

		String q0_0 = result.getValueAtQuantile(0);
		String q0_5 = result.getValueAtQuantile(.5);
		String q1_0 = result.getValueAtQuantile(1.0);

		assertEquals(Long.valueOf(q0_0), 0);
		assertEquals(Long.valueOf(q1_0), 200000.0, 200000 * RELATIVE_ACCURACY);

		// Median should be seriously close to 0
		assertEquals(Long.valueOf(q0_5), 100000.0, 100000 * RELATIVE_ACCURACY);
	}
}