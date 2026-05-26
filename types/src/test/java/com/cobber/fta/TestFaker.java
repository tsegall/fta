/*
 * Copyright 2017-2026 Tim Segall
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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.Locale;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.faker.FakerLT;
import com.cobber.fta.faker.FakerParameters;

public class TestFaker {

	private FakerLT buildFaker(final FakerParameters p) throws FTAPluginException {
		return buildFaker(p, Locale.getDefault());
	}

	private FakerLT buildFaker(final FakerParameters p, final Locale locale) throws FTAPluginException {
		final PluginDefinition plugin = new PluginDefinition(p.type, p.getClazz());
		// Allow any locale so locale-sensitive faker types (e.g. BOOLEAN YES_NO) work beyond "en"
		plugin.validLocales = PluginLocaleEntry.simple(new String[] { "*" });
		final FakerLT lt = (FakerLT) LogicalTypeFactory.newInstance(plugin, new AnalysisConfig(locale));
		lt.setControl(p);
		return lt;
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void bindAllTypes() {
		final String[][] cases = {
			{ "DOUBLE",          "FakerDoubleLT" },
			{ "LONG",            "FakerLongLT" },
			{ "BOOLEAN",         "FakerBooleanLT" },
			{ "ENUM",            "FakerEnumLT" },
			{ "STRING",          "FakerStringLT" },
			{ "LOCALDATE",       "FakerLocalDateLT" },
			{ "LOCALDATETIME",   "FakerLocalDateTimeLT" },
			{ "OFFSETDATETIME",  "FakerOffsetDateTimeLT" },
			{ "LOCALTIME",       "FakerLocalTimeLT" },
		};

		for (final String[] c : cases) {
			final FakerParameters p = new FakerParameters();
			p.type = c[0];
			p.bind();
			assertNotNull(p.getClazz(), c[0] + " should bind to a class");
			assertTrue(p.getClazz().endsWith(c[1]), c[0] + " expected " + c[1] + " got " + p.getClazz());
		}

		final FakerParameters unknown = new FakerParameters();
		unknown.type = "UNKNOWN";
		unknown.bind();
		assertNull(unknown.getClazz(), "Unknown type should not bind");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void stringEnumMode() throws FTAPluginException {
		final FakerParameters p = new FakerParameters();
		p.type = "STRING";
		p.values = new String[] { "alpha", "beta", "gamma" };
		p.bind();

		final FakerLT lt = buildFaker(p);
		final Set<String> allowed = Set.of("alpha", "beta", "gamma");
		for (int i = 0; i < 50; i++)
			assertTrue(allowed.contains(lt.nextRandom()), "Value must be one of the configured enum values");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void stringRegexMode() throws FTAPluginException {
		final FakerParameters p = new FakerParameters();
		p.type = "STRING";
		p.format = "\\d{5}";
		p.bind();

		final FakerLT lt = buildFaker(p);
		for (int i = 0; i < 50; i++) {
			final String v = lt.nextRandom();
			assertNotNull(v);
			assertTrue(v.matches("\\d{5}"), "Value '" + v + "' must match \\d{5}");
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void longRandomBounds() throws FTAPluginException {
		final FakerParameters p = new FakerParameters();
		p.type = "LONG";
		p.low = "10";
		p.high = "100";
		p.bind();

		final FakerLT lt = buildFaker(p);
		for (int i = 0; i < 200; i++) {
			final long v = Long.parseLong(lt.nextRandom());
			assertTrue(v >= 10 && v <= 100, "Value " + v + " must be in [10, 100]");
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void longMonotonicIncreasing() throws FTAPluginException {
		final FakerParameters p = new FakerParameters();
		p.type = "LONG";
		p.low = "1";
		p.high = "1000";
		p.distribution = "monotonic_increasing";
		p.bind();

		final FakerLT lt = buildFaker(p);
		long prev = Long.MIN_VALUE;
		for (int i = 0; i < 60; i++) {
			final long v = Long.parseLong(lt.nextRandom());
			assertTrue(v >= prev, "Value " + v + " must be >= previous " + prev);
			prev = v;
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void longMonotonicDecreasing() throws FTAPluginException {
		final FakerParameters p = new FakerParameters();
		p.type = "LONG";
		p.low = "1";
		p.high = "1000";
		p.distribution = "monotonic_decreasing";
		p.bind();

		final FakerLT lt = buildFaker(p);
		long prev = Long.MAX_VALUE;
		for (int i = 0; i < 60; i++) {
			final long v = Long.parseLong(lt.nextRandom());
			assertTrue(v <= prev, "Value " + v + " must be <= previous " + prev);
			prev = v;
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void doubleRandomBounds() throws FTAPluginException {
		final FakerParameters p = new FakerParameters();
		p.type = "DOUBLE";
		p.low = "1.0";
		p.high = "10.0";
		p.format = "%f";
		p.bind();

		final FakerLT lt = buildFaker(p);
		for (int i = 0; i < 200; i++) {
			final double v = Double.parseDouble(lt.nextRandom());
			assertTrue(v >= 1.0 && v <= 10.0, "Value " + v + " must be in [1.0, 10.0]");
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void localDateInRange() throws FTAPluginException {
		final FakerParameters p = new FakerParameters();
		p.type = "LOCALDATE";
		p.format = "yyyy-MM-dd";
		p.low = "2000-01-01";
		p.high = "2010-12-31";
		p.bind();

		final FakerLT lt = buildFaker(p);
		final LocalDate low = LocalDate.of(2000, 1, 1);
		final LocalDate high = LocalDate.of(2010, 12, 31);
		final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		for (int i = 0; i < 100; i++) {
			final LocalDate d = LocalDate.parse(lt.nextRandom(), dtf);
			assertTrue(!d.isBefore(low) && !d.isAfter(high), "Date " + d + " must be in [2000-01-01, 2010-12-31]");
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void endToEndLong() throws FTAException {
		final FakerParameters p = new FakerParameters();
		p.type = "LONG";
		p.low = "1000";
		p.high = "9999";
		p.bind();

		final FakerLT lt = buildFaker(p);
		final TextAnalyzer analyzer = new TextAnalyzer("score");
		for (int i = 0; i < 100; i++)
			analyzer.train(lt.nextRandom());

		final TextAnalysisResult result = analyzer.getResult();
		assertEquals(result.getType(), FTAType.LONG);
	}

	// ---- Double distributions ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void doubleMonotonicIncreasing() throws FTAPluginException {
		final FakerParameters p = new FakerParameters();
		p.type = "DOUBLE";
		p.low = "0.0";
		p.high = "1000.0";
		p.format = "%f";
		p.distribution = "monotonic_increasing";
		p.bind();

		final FakerLT lt = buildFaker(p);
		double prev = -Double.MAX_VALUE;
		for (int i = 0; i < 60; i++) {
			final double v = Double.parseDouble(lt.nextRandom());
			assertTrue(v >= prev, "Value " + v + " must be >= previous " + prev);
			prev = v;
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void doubleMonotonicDecreasing() throws FTAPluginException {
		final FakerParameters p = new FakerParameters();
		p.type = "DOUBLE";
		p.low = "0.0";
		p.high = "1000.0";
		p.format = "%f";
		p.distribution = "monotonic_decreasing";
		p.bind();

		final FakerLT lt = buildFaker(p);
		double prev = Double.MAX_VALUE;
		for (int i = 0; i < 60; i++) {
			final double v = Double.parseDouble(lt.nextRandom());
			assertTrue(v <= prev, "Value " + v + " must be <= previous " + prev);
			prev = v;
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void doubleGaussian() throws FTAPluginException {
		final FakerParameters p = new FakerParameters();
		p.type = "DOUBLE";
		p.low = "0.0";
		p.high = "100.0";
		p.format = "%f";
		p.distribution = "gaussian";
		p.bind();

		final FakerLT lt = buildFaker(p);
		for (int i = 0; i < 100; i++) {
			final String v = lt.nextRandom();
			assertNotNull(v);
			Double.parseDouble(v);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void longGaussian() throws FTAPluginException {
		final FakerParameters p = new FakerParameters();
		p.type = "LONG";
		p.low = "0";
		p.high = "100";
		p.distribution = "gaussian";
		p.bind();

		final FakerLT lt = buildFaker(p);
		for (int i = 0; i < 100; i++) {
			final String v = lt.nextRandom();
			assertNotNull(v);
			Long.parseLong(v);
		}
	}

	// ---- Boolean ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void booleanYesNo() throws FTAPluginException {
		final FakerParameters p = new FakerParameters();
		p.type = "BOOLEAN";
		p.format = "YES_NO";
		p.bind();

		// YES_NO uses locale-specific keywords; German has "ja"/"nein" — English has no entry
		final FakerLT lt = buildFaker(p, Locale.GERMAN);
		final Set<String> allowed = Set.of("ja", "nein");
		for (int i = 0; i < 50; i++)
			assertTrue(allowed.contains(lt.nextRandom()), "Value must be ja or nein (de locale)");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void booleanYN() throws FTAPluginException {
		final FakerParameters p = new FakerParameters();
		p.type = "BOOLEAN";
		p.format = "Y_N";
		p.bind();

		final FakerLT lt = buildFaker(p);
		final Set<String> allowed = Set.of("Y", "N");
		for (int i = 0; i < 50; i++)
			assertTrue(allowed.contains(lt.nextRandom()), "Value must be Y or N");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void booleanOneZero() throws FTAPluginException {
		final FakerParameters p = new FakerParameters();
		p.type = "BOOLEAN";
		p.format = "ONE_ZERO";
		p.bind();

		final FakerLT lt = buildFaker(p);
		final Set<String> allowed = Set.of("1", "0");
		for (int i = 0; i < 50; i++)
			assertTrue(allowed.contains(lt.nextRandom()), "Value must be 1 or 0");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void booleanTrueFalse() throws FTAPluginException {
		final FakerParameters p = new FakerParameters();
		p.type = "BOOLEAN";
		p.format = "TRUE_FALSE";
		p.bind();

		final FakerLT lt = buildFaker(p);
		final Set<String> allowed = Set.of("TRUE", "FALSE");
		for (int i = 0; i < 50; i++)
			assertTrue(allowed.contains(lt.nextRandom()), "Value must be TRUE or FALSE");
	}

	// ---- Enum ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void enumRandom() throws FTAPluginException {
		final FakerParameters p = new FakerParameters();
		p.type = "ENUM";
		p.values = new String[] { "RED", "GREEN", "BLUE" };
		p.bind();

		final FakerLT lt = buildFaker(p);
		final Set<String> allowed = Set.of("RED", "GREEN", "BLUE");
		for (int i = 0; i < 50; i++)
			assertTrue(allowed.contains(lt.nextRandom()), "Value must be one of RED, GREEN, BLUE");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void enumMonotonicIncreasing() throws FTAPluginException {
		final FakerParameters p = new FakerParameters();
		p.type = "ENUM";
		p.values = new String[] { "A", "B", "C", "D", "E" };
		p.distribution = "monotonic_increasing";
		p.bind();

		final FakerLT lt = buildFaker(p);
		// Cycles A→B→C→D→E→A... — verify no ArrayIndexOutOfBoundsException across a full cycle
		for (int i = 0; i < 15; i++)
			assertNotNull(lt.nextRandom());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void enumMonotonicDecreasing() throws FTAPluginException {
		final FakerParameters p = new FakerParameters();
		p.type = "ENUM";
		p.values = new String[] { "A", "B", "C", "D", "E" };
		p.distribution = "monotonic_decreasing";
		p.bind();

		final FakerLT lt = buildFaker(p);
		for (int i = 0; i < 15; i++)
			assertNotNull(lt.nextRandom());
	}

	// ---- String free-text fallback ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void stringFreeText() throws FTAPluginException {
		final FakerParameters p = new FakerParameters();
		p.type = "STRING";
		p.bind();

		final FakerLT lt = buildFaker(p);
		for (int i = 0; i < 20; i++)
			assertNotNull(lt.nextRandom(), "Free-text fallback must return a non-null value");
	}

	// ---- LocalDateTime ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void localDateTimeInRange() throws FTAPluginException {
		final FakerParameters p = new FakerParameters();
		p.type = "LOCALDATETIME";
		p.format = "yyyy-MM-dd HH:mm:ss";
		p.low = "2000-01-01 00:00:00";
		p.high = "2010-12-31 23:59:59";
		p.bind();

		final FakerLT lt = buildFaker(p);
		final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		final LocalDateTime low = LocalDateTime.of(2000, 1, 1, 0, 0, 0);
		final LocalDateTime high = LocalDateTime.of(2010, 12, 31, 23, 59, 59);

		for (int i = 0; i < 100; i++) {
			final LocalDateTime v = LocalDateTime.parse(lt.nextRandom(), dtf);
			assertTrue(!v.isBefore(low) && !v.isAfter(high), "DateTime " + v + " must be in [2000-01-01, 2010-12-31]");
		}
	}

	// ---- LocalTime ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void localTimeFormat() throws FTAPluginException {
		final FakerParameters p = new FakerParameters();
		p.type = "LOCALTIME";
		p.format = "HH:mm:ss";
		p.low = "08:00:00";
		p.high = "18:00:00";
		p.bind();

		final FakerLT lt = buildFaker(p);
		final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
		for (int i = 0; i < 100; i++)
			assertNotNull(LocalTime.parse(lt.nextRandom(), dtf), "Must parse as a valid LocalTime");
	}

	// ---- OffsetDateTime ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void offsetDateTimeDefault() throws FTAPluginException {
		final FakerParameters p = new FakerParameters();
		p.type = "OFFSETDATETIME";
		p.format = "yyyy-MM-dd'T'HH:mm:ssXXX";
		p.low = "2000-01-01T00:00:00+00:00";
		p.high = "2020-12-31T23:59:59+00:00";
		p.bind();

		final FakerLT lt = buildFaker(p);
		final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
		final OffsetDateTime low = OffsetDateTime.parse("2000-01-01T00:00:00+00:00", dtf);
		final OffsetDateTime high = OffsetDateTime.parse("2020-12-31T23:59:59+00:00", dtf);

		for (int i = 0; i < 100; i++) {
			final OffsetDateTime v = OffsetDateTime.parse(lt.nextRandom(), dtf);
			assertTrue(!v.isBefore(low) && !v.isAfter(high), "OffsetDateTime " + v + " must be in range");
		}
	}

	// ---- End-to-end for new types ----

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void endToEndBoolean() throws FTAException {
		final FakerParameters p = new FakerParameters();
		p.type = "BOOLEAN";
		p.format = "TRUE_FALSE";
		p.bind();

		final FakerLT lt = buildFaker(p);
		final TextAnalyzer analyzer = new TextAnalyzer("active");
		for (int i = 0; i < 100; i++)
			analyzer.train(lt.nextRandom());

		final TextAnalysisResult result = analyzer.getResult();
		assertEquals(result.getType(), FTAType.BOOLEAN);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void endToEndDateTime() throws FTAException {
		final FakerParameters p = new FakerParameters();
		p.type = "LOCALDATETIME";
		p.format = "yyyy-MM-dd HH:mm:ss";
		p.low = "2000-01-01 00:00:00";
		p.high = "2020-12-31 23:59:59";
		p.bind();

		final FakerLT lt = buildFaker(p);
		final TextAnalyzer analyzer = new TextAnalyzer("timestamp");
		for (int i = 0; i < 100; i++)
			analyzer.train(lt.nextRandom());

		final TextAnalysisResult result = analyzer.getResult();
		assertEquals(result.getType(), FTAType.LOCALDATETIME);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void endToEndDate() throws FTAException {
		final FakerParameters p = new FakerParameters();
		p.type = "LOCALDATE";
		p.format = "yyyy-MM-dd";
		p.low = "1990-01-01";
		p.high = "2020-12-31";
		p.bind();

		final FakerLT lt = buildFaker(p);
		final TextAnalyzer analyzer = new TextAnalyzer("birthdate");
		for (int i = 0; i < 100; i++)
			analyzer.train(lt.nextRandom());

		final TextAnalysisResult result = analyzer.getResult();
		assertEquals(result.getType(), FTAType.LOCALDATE);
	}
}
